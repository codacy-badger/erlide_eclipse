/*******************************************************************************
 * Copyright (c) 2010 György Orosz. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: György Orosz - initial API and implementation
 ******************************************************************************/
package org.erlide.wrangler.refactoring.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.erlide.wrangler.refactoring.util.IErlRange;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

/**
 * Abstract class for integrating Wrangler refactorings which offer a selection list for
 * the user. These selections are tipically code parts.
 *
 * @author Gyorgy Orosz
 * @version %I%, %G%
 */
public abstract class CostumWorkflowRefactoringWithPositionsSelection
        extends CostumWorkflowRefactoring {

    protected Map<IErlRange, OtpErlangTuple> positions;
    protected List<IErlRange> selectedPositions;

    /**
     * Get those exporessions with positions which can be selected by the user.
     *
     * @return List of positions
     */
    public List<IErlRange> getPositions() {
        final List<IErlRange> ret = new ArrayList<>();
        ret.addAll(positions.keySet());

        return ret;
    }

    /**
     * Setter method, which offers the ability to set the selected positions.
     *
     * @param l
     *            selected positions
     */
    public void setSelectedPos(final ArrayList<IErlRange> l) {
        selectedPositions = l;
    }

    protected OtpErlangList getSelectedPos() {
        if (selectedPositions == null) {
            return new OtpErlangList();
        }
        OtpErlangList ret;
        final OtpErlangObject[] selection = new OtpErlangObject[selectedPositions.size()];

        for (int i = 0; i < selectedPositions.size(); ++i) {
            selection[i] = positions.get(selectedPositions.get(i));
        }

        ret = new OtpErlangList(selection);
        return ret;
    }

}
