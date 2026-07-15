/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openpreservation.fixity.apps.schedule;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.context.internal.ManagedSessionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.Collection;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.CollectionPathDAO;
import org.openpreservation.fixity.apps.dao.DigestRecord;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathRegistration;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.dao.PathSummaryRecord;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.quartz.JobExecutionException;

/**
 * Regression tests for the DB deadlock caused by two concurrent scan
 * transactions competing for write locks on the same tables.
 *
 * Root causes addressed:
 *  - N+1 lazy SELECTs for DigestRecord inside an open write transaction
 *    (fixed by @BatchSize on FileScanRecord.digestResults and FolderScanRecord.files)
 *  - Long-running single-transaction cascade inserts (improved by JDBC batch_size)
 */
@SuppressWarnings("null")
public class ScanUpdaterConcurrentTest {

    // Isolated in-memory DB — does not share state with other test classes
    private static SessionFactory sessionFactory;
    private final List<Path> tempDirs = new ArrayList<>();

    @BeforeAll
    static void buildSessionFactory() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url",
                        "jdbc:h2:mem:concurrent_scan_test;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.current_session_context_class", "managed")
                .applySetting("hibernate.show_sql", "false")
                .applySetting("hibernate.jdbc.batch_size", "50")
                .build();
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(Collection.class)
                .addAnnotatedClass(CollectionPath.class)
                .addAnnotatedClass(DigestRecord.class)
                .addAnnotatedClass(FileScanRecord.class)
                .addAnnotatedClass(FolderScanRecord.class)
                .addAnnotatedClass(PathRegistration.class)
                .addAnnotatedClass(PathScan.class)
                .addAnnotatedClass(PathSummaryRecord.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    @AfterAll
    static void closeSessionFactory() {
        if (sessionFactory != null) sessionFactory.close();
    }

    @AfterEach
    void cleanupTempDirs() {
        tempDirs.forEach(d -> Utils.deleteDirectory(d));
        tempDirs.clear();
    }

    // -- helpers --

    private Path createDirWithFiles(int count) throws IOException {
        Path dir = Files.createTempDirectory("concurrent-scan-");
        for (int i = 0; i < count; i++) {
            Files.writeString(dir.resolve("file" + i + ".txt"), "content for file " + i);
        }
        tempDirs.add(dir);
        return dir;
    }

    /**
     * Runs {@code action} inside a dedicated Hibernate session that is committed
     * on success, rolled back on failure. Safe to call from any thread.
     */
    private void inSession(RunnableEx action) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        session.beginTransaction();
        try {
            action.run();
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    private PathScan runScan(Path dir) throws IOException, NoSuchAlgorithmException, JobExecutionException {
        CollectionPathDAO dao = new CollectionPathDAO(sessionFactory);
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(dir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        new ScanUpdater(dao).updateDatabase(scan);
        return scan;
    }

    // -- tests --

    /**
     * Two scans on separate paths are fired simultaneously. Both must complete
     * within 30 seconds. Without the @BatchSize fix, each scan holds an
     * open transaction while executing N+1 lazy SELECTs, and both large
     * write transactions then compete for H2 page locks — causing a deadlock.
     */
    @Test
    void testConcurrentScansOnDifferentPathsDoNotDeadlock() throws Exception {
        Path dir1 = createDirWithFiles(25);
        Path dir2 = createDirWithFiles(25);

        // Register both paths in committed transactions so scan threads can read them
        inSession(() -> new CollectionPathDAO(sessionFactory).create(CollectionPath.of(dir1)));
        inSession(() -> new CollectionPathDAO(sessionFactory).create(CollectionPath.of(dir2)));

        // Latch ensures both threads start their DB writes at the same instant
        CountDownLatch go = new CountDownLatch(1);

        CompletableFuture<Void> scan1 = CompletableFuture.runAsync(() -> {
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            inSession(() -> runScan(dir1));
        });

        CompletableFuture<Void> scan2 = CompletableFuture.runAsync(() -> {
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            inSession(() -> runScan(dir2));
        });

        go.countDown(); // fire

        assertTimeoutPreemptively(Duration.ofSeconds(30),
                () -> CompletableFuture.allOf(scan1, scan2).join(),
                "Concurrent scans deadlocked — both should complete within 30 seconds");
    }

    /**
     * A second scan on the same path triggers updateFromPreviousScan, which
     * compares file digests between the current and previous scan. Without
     * @BatchSize on FileScanRecord.digestResults, this generates one lazy
     * SELECT per file inside the open write transaction — N+1 queries that
     * hold the transaction open long enough to block concurrent readers/writers.
     */
    @Test
    void testSecondScanWithPreviousResultsCompletesWithoutDeadlock() throws Exception {
        Path dir = createDirWithFiles(30);

        inSession(() -> new CollectionPathDAO(sessionFactory).create(CollectionPath.of(dir)));

        // First scan — establishes the baseline in the DB
        inSession(() -> runScan(dir));

        // Second scan — exercises the lazy digest load code path.
        // This is the scenario that generated N+1 queries before the fix.
        assertTimeoutPreemptively(Duration.ofSeconds(30),
                () -> inSession(() -> runScan(dir)),
                "Second scan deadlocked loading previous digest results");
    }

    /**
     * Verify that concurrent scans each produce the correct number of results.
     */
    @Test
    void testConcurrentScansProduceCorrectResults() throws Exception {
        Path dir1 = createDirWithFiles(10);
        Path dir2 = createDirWithFiles(15);

        inSession(() -> new CollectionPathDAO(sessionFactory).create(CollectionPath.of(dir1)));
        inSession(() -> new CollectionPathDAO(sessionFactory).create(CollectionPath.of(dir2)));

        CountDownLatch go = new CountDownLatch(1);
        List<PathScan> results = new ArrayList<>();

        CompletableFuture<Void> scan1 = CompletableFuture.runAsync(() -> {
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            inSession(() -> {
                PathScan s = runScan(dir1);
                synchronized (results) { results.add(s); }
            });
        });

        CompletableFuture<Void> scan2 = CompletableFuture.runAsync(() -> {
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            inSession(() -> {
                PathScan s = runScan(dir2);
                synchronized (results) { results.add(s); }
            });
        });

        go.countDown();
        assertTimeoutPreemptively(Duration.ofSeconds(30),
                () -> CompletableFuture.allOf(scan1, scan2).join());

        assertTrue(results.stream().anyMatch(s -> s.getResultCount() == 10),
                "dir1 scan should have 10 results");
        assertTrue(results.stream().anyMatch(s -> s.getResultCount() == 15),
                "dir2 scan should have 15 results");
    }

    @FunctionalInterface
    private interface RunnableEx {
        void run() throws Exception;
    }
}
