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
package org.openpreservation.fixity.apps.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.context.internal.ManagedSessionContext;
import org.jspecify.annotations.NullMarked;

@SuppressWarnings("null")
@NullMarked
public class TestSessionFactory {
    private static final SessionFactory SESSION_FACTORY;
    private static final DataFactory DATA_FACTORY;

    static {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.current_session_context_class", "managed")
                .applySetting("hibernate.show_sql", "false")
                .build();
        SESSION_FACTORY = new MetadataSources(registry)
                .addAnnotatedClass(Collection.class)
                .addAnnotatedClass(CollectionPath.class)
                .addAnnotatedClass(DigestRecord.class)
                .addAnnotatedClass(FileScanRecord.class)
                .addAnnotatedClass(FolderScanRecord.class)
                .addAnnotatedClass(PathRegistration.class)
                .addAnnotatedClass(PathScan.class)
                .addAnnotatedClass(PathSummaryRecord.class)
                .addAnnotatedClass(ScanSchedule.class)
                .buildMetadata()
                .buildSessionFactory();
        DATA_FACTORY = new DataFactory(SESSION_FACTORY);
    }

    public static DataFactory dataFactory() {
        return DATA_FACTORY;
    }

    public static Session currentSession() {
        return SESSION_FACTORY.getCurrentSession();
    }

    public static void beginTransaction() {
        Session session = SESSION_FACTORY.openSession();
        ManagedSessionContext.bind(session);
        session.beginTransaction();
    }

    public static void rollback() {
        Session session = SESSION_FACTORY.getCurrentSession();
        Transaction tx = session.getTransaction();
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
        ManagedSessionContext.unbind(SESSION_FACTORY);
        session.close();
    }
}
