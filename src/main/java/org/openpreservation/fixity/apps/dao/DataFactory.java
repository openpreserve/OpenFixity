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

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DataFactory {
    private final CollectionDAO collectionDAO;
    private final CollectionPathDAO collectionPathDAO;
    private final PathRegistrationDAO pathRegistrationDAO;
    private final FileScanRecordDAO fileScanRecordDAO;
    private final PathScanDAO pathScanDAO;
    private final FolderScanRecordDAO folderScanRecordDAO;

    public DataFactory(SessionFactory sessionFactory) {
        this.collectionDAO = new CollectionDAO(sessionFactory);
        this.collectionPathDAO = new CollectionPathDAO(sessionFactory);
        this.pathRegistrationDAO = new PathRegistrationDAO(sessionFactory);
        this.fileScanRecordDAO = new FileScanRecordDAO(sessionFactory);
        this.pathScanDAO = new PathScanDAO(sessionFactory);
        this.folderScanRecordDAO = new FolderScanRecordDAO(sessionFactory);
    }
    public CollectionDAO collectionDAO() {
        return this.collectionDAO;
    }
    public CollectionPathDAO collectionPathDAO() {
        return this.collectionPathDAO;
    }
    public PathRegistrationDAO pathRegistrationDAO() {
        return this.pathRegistrationDAO;
    }
    public FileScanRecordDAO fileScanRecordDAO() {
        return this.fileScanRecordDAO;
    }
    public PathScanDAO pathScanDAO() {
        return this.pathScanDAO;
    }
    public FolderScanRecordDAO folderScanRecordDAO() {
        return this.folderScanRecordDAO;
    }
}
