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
package org.openpreservation.fixity.apps.server.resources.views;

import org.openpreservation.fixity.apps.server.resources.api.FolderInfoResource.FolderInfo;
import org.openpreservation.fixity.apps.server.views.FoldersView;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@Path("/folders/")
public class FoldersResource {
    private org.openpreservation.fixity.apps.server.resources.api.FolderInfoResource apiResource;
    public FoldersResource(@Context ResourceContext resourceContext) {
        this.apiResource = resourceContext.getResource(org.openpreservation.fixity.apps.server.resources.api.FolderInfoResource.class);
    }

    @GET
    public FoldersView defaultFolder() {
        FolderInfo folderInfo = apiResource.getHomeFolder();
        return new FoldersView(folderInfo,
                               apiResource.getChildFolders(folderInfo.id),
                               apiResource.getParentFolders(folderInfo.id));
    }

    @GET
    @Path("/{folderId}/")
    public FoldersView folders(@PathParam("folderId") Integer folderId) {
        return new FoldersView(apiResource.getFolder(folderId),
                               apiResource.getChildFolders(folderId),
                               apiResource.getParentFolders(folderId));
    }
}
