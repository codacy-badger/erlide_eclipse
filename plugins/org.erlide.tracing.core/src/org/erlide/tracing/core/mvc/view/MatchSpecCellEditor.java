package org.erlide.tracing.core.mvc.view;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.erlide.engine.model.OtpRpcFactory;
import org.erlide.runtime.rpc.IOtpRpc;
import org.erlide.tracing.core.Constants;
import org.erlide.tracing.core.mvc.model.MatchSpec;
import org.erlide.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;

/**
 * Cell editor for specifying match specifications.
 *
 * @author Piotr Dorobisz
 *
 */
public class MatchSpecCellEditor extends DialogCellEditor {

    public MatchSpecCellEditor(final Composite parent) {
        super(parent);
    }

    @Override
    protected Object openDialogBox(final Control cellEditorWindow) {
        final MatchSpecInputDialog dialog = new MatchSpecInputDialog(
                cellEditorWindow.getShell(), "Create match spec", "Literal fun:",
                ((MatchSpec) getValue()).getFunctionString(), new MatchSpecValidator());
        dialog.open();
        return getValue();
    }

    private class MatchSpecInputDialog extends InputDialog {

        public MatchSpecInputDialog(final Shell parentShell, final String dialogTitle,
                final String dialogMessage, final String initialValue,
                final IInputValidator validator) {
            super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
        }

        @Override
        protected int getInputTextStyle() {
            return SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL;
        }
    }

    private class MatchSpecValidator implements IInputValidator {

        public IOtpRpc getBackend() {
            return OtpRpcFactory.getOtpRpc();
        }

        @Override
        public String isValid(final String newText) {
            if (newText == null || newText.isEmpty()) {
                // no match spec
                ((MatchSpec) getValue()).setFunctionString("");
                ((MatchSpec) getValue()).setMsObject(null);
                return null;
            } else if ("x".equals(newText)) {
                // shortcut for matching exceptions and return values
                ((MatchSpec) getValue()).setFunctionString("x");
                ((MatchSpec) getValue()).setMsObject(new OtpErlangAtom("x"));
                return null;
            }
            try {

                final IOtpRpc backend = getBackend();
                final OtpErlangTuple tuple = (OtpErlangTuple) backend.call(
                        Constants.ERLANG_HELPER_MODULE, Constants.FUN_STR2MS, "s",
                        new OtpErlangString(newText));
                if ("ok".equals(((OtpErlangAtom) tuple.elementAt(0)).atomValue())) {
                    // correct match spec - update
                    ((MatchSpec) getValue()).setFunctionString(newText);
                    ((MatchSpec) getValue()).setMsObject(tuple.elementAt(1));
                    return null;
                }
                // incorrect match spec
                final OtpErlangAtom errorType = (OtpErlangAtom) tuple.elementAt(1);
                if ("standard_info".equals(errorType.atomValue())) {
                    final OtpErlangTuple errorTuple = (OtpErlangTuple) tuple.elementAt(2);
                    final StringBuilder builder = new StringBuilder("Line ");
                    builder.append(errorTuple.elementAt(0)).append(": ");
                    final OtpErlangList errorList = (OtpErlangList) errorTuple
                            .elementAt(2);
                    builder.append(
                            ((OtpErlangString) errorList.elementAt(0)).stringValue());
                    if (errorList.elementAt(1) instanceof OtpErlangString) {
                        builder.append(
                                ((OtpErlangString) errorList.elementAt(1)).stringValue());
                    }
                    return builder.toString();
                } else if ("not_fun".equals(errorType.atomValue())) {
                    return "Given expression is not a function";
                } else if ("unbound_var".equals(errorType.atomValue())) {
                    return "Unbound variable: " + tuple.elementAt(2);
                } else {
                    return tuple.elementAt(2).toString();
                }
            } catch (final Exception e) {
                ErlLogger.error(e);
                return "Backend problem: " + e.getMessage();
            }
        }
    }
}
