package org.erlide.test_support.popup.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.erlide.test_support.ui.suites.TestExecutor;
import org.erlide.ui.handlers.ErlangAbstractHandler;

public class RunEunitHandler extends ErlangAbstractHandler {

    final TestExecutor exec = new TestExecutor();

    @Override
    protected void doAction(final ISelection sel, final ITextEditor textEditor) {
        exec.run("run", sel, textEditor);
    }

}
