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

public enum PathScanStatus {
    DAMAGED("<i class=\"bi bi-file-earmark-x\"></i>"),
    DENIED("<i class=\"bi bi-file-earmark-lock2\"></i>"),
    IGNORED("<i class=\"bi bi-file\"></i>"),
    NOTFOUND("<i class=\"bi bi-file-earmark-minus\"></i>"),
    ADDED("<i class=\"bi bi-file-plus\"></i>"),
    CHANGED("<i class=\"bi bi-file-earmark-diff\"></i>"),
    VERIFIED("<i class=\"bi bi-file-check\"></i>");

    private final String symbol;

    private PathScanStatus(final String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}
