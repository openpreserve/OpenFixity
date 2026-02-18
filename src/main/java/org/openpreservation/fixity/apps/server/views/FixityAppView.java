package org.openpreservation.fixity.apps.server.views;

import io.dropwizard.views.common.View;

public abstract class FixityAppView extends View {
    protected final String menuId;

    public FixityAppView(final String templateName, final String menuId) {
        super(templateName);
        this.menuId = menuId;
    }

    public String getMenuId() {
        return menuId;
    }
}
