package org.openpreservation.fixity.apps.server.resources.views;

import org.openpreservation.fixity.apps.server.resources.api.FoldersResource.FolderInfo;
import org.openpreservation.fixity.apps.server.views.FoldersView;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@Path("/folders/")
public class FoldersResource {
    private org.openpreservation.fixity.apps.server.resources.api.FoldersResource apiResource;
    public FoldersResource(@Context ResourceContext resourceContext) {
        this.apiResource = resourceContext.getResource(org.openpreservation.fixity.apps.server.resources.api.FoldersResource.class);
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
