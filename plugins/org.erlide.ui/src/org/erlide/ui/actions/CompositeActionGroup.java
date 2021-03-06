/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Eclipse Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class CompositeActionGroup extends ActionGroup {

    private ActionGroup[] fGroups;

    public CompositeActionGroup() {
    }

    public CompositeActionGroup(final ActionGroup[] groups) {
        setGroups(groups);
    }

    protected void setGroups(final ActionGroup[] groups) {
        fGroups = groups;
    }

    public ActionGroup get(final int index) {
        if (fGroups == null) {
            return null;
        }
        return fGroups[index];
    }

    public void addGroup(final ActionGroup group) {
        if (fGroups == null) {
            fGroups = new ActionGroup[] {
                    group
            };
        } else {
            final ActionGroup[] newGroups = new ActionGroup[fGroups.length + 1];
            System.arraycopy(fGroups, 0, newGroups, 0, fGroups.length);
            newGroups[fGroups.length] = group;
            fGroups = newGroups;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fGroups == null) {
            return;
        }
        for (final ActionGroup fGroup : fGroups) {
            fGroup.dispose();
        }
    }

    @Override
    public void fillActionBars(final IActionBars actionBars) {
        super.fillActionBars(actionBars);
        if (fGroups == null) {
            return;
        }
        for (final ActionGroup fGroup : fGroups) {
            fGroup.fillActionBars(actionBars);
        }
    }

    @Override
    public void fillContextMenu(final IMenuManager menu) {
        super.fillContextMenu(menu);
        if (fGroups == null) {
            return;
        }
        for (final ActionGroup fGroup : fGroups) {
            fGroup.fillContextMenu(menu);
        }
    }

    @Override
    public void setContext(final ActionContext context) {
        super.setContext(context);
        if (fGroups == null) {
            return;
        }
        for (final ActionGroup fGroup : fGroups) {
            fGroup.setContext(context);
        }
    }

    @Override
    public void updateActionBars() {
        super.updateActionBars();
        if (fGroups == null) {
            return;
        }
        for (final ActionGroup fGroup : fGroups) {
            fGroup.updateActionBars();
        }
    }
}
