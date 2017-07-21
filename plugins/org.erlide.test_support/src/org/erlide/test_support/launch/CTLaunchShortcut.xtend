package org.erlide.test_support.launch

import org.eclipse.debug.ui.ILaunchShortcut
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IEditorPart
import org.erlide.test_support.ui.suites.TestExecutor

class CTLaunchShortcut implements ILaunchShortcut {

    TestExecutor exec = new TestExecutor

    override void launch(ISelection selection, String mode) {
        exec.run(mode, "CT", selection)
    }

    override void launch(IEditorPart editor, String mode) {
        exec.run(mode, "CT", editor)
    }
}
