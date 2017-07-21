package org.erlide.test_support.ui.suites;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.erlide.runtime.events.ErlangEventHandler;
import org.erlide.test_support.EunitEventHandler;
import org.erlide.util.ErlLogger;
import org.erlide.util.erlang.OtpBindings;
import org.erlide.util.erlang.OtpErlang;
import org.erlide.util.erlang.OtpParserException;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangException;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class EunitResultsView extends TestResultsView {

    @Override
    protected ErlangEventHandler createEventHandler() {
        final EunitEventHandler handler = new EunitEventHandler(this);
        // backend.registerEventListener(handler);
        return handler;
    }

    @Override
    protected void handleEvent(final OtpErlangObject msg)
            throws OtpParserException, OtpErlangException {

        ErlLogger.debug("---- %s", OtpErlang.asString(msg));

        final OtpErlangTuple tuple = (OtpErlangTuple) msg;
        final String tag = ((OtpErlangAtom) tuple.elementAt(0)).atomValue();
        final OtpErlangObject value = tuple.elementAt(1);

        TestCaseData test;
        if ("init".equals(tag)) {
            // value = {Dir, Suite, Case}
            label.setText("Started: " + formatTitle(value)
                    + ". Compiling files, please wait...");
            treeViewer.getTree().setCursor(treeViewer.getTree().getShell().getDisplay()
                    .getSystemCursor(SWT.CURSOR_WAIT));
        } else if ("start_failed".equals(tag)) {
            // value = ?
        } else if ("log_started".equals(tag)) {
            // value = Dir
            treeViewer.getTree().setCursor(treeViewer.getTree().getShell().getDisplay()
                    .getSystemCursor(SWT.CURSOR_ARROW));
        } else if ("start".equals(tag)) {
            // value = {Module, Function}
            final OtpBindings bindings = OtpErlang.match("{M:a,F:a}", value);
            final String mod = bindings.getAtom("M");
            final String fun = bindings.getAtom("F");
            test = findCase(mod, fun);
            test.setRunning();
        } else if ("result".equals(tag)) {
            // value = {Module, Function, Result}
            final OtpBindings bindings = OtpErlang.match("{M:a,F:a,R}", value);
            final String mod = bindings.getAtom("M");
            final String fun = bindings.getAtom("F");
            final OtpErlangObject result = bindings.get("R");
            test = findCase(mod, fun);
            if (result instanceof OtpErlangAtom) {
                test.setSuccesful();
                // } else {
                // final BindingsImpl bindings =
                // ErlUtils.match("{failure,{M:a,F:a},L,R}", result);
                // final OtpErlangObject locations = bindings.get("L");
                // final OtpErlangObject reason = bindings.get("R");
                // test.setFailed(reason, locations);
            }
        } else if ("fail".equals(tag)) {
            // value = {{Module, Function}, [Locations], Reason
            final OtpBindings bindings = OtpErlang.match("{{M:a,F:a},L,R}", value);
            final String mod = bindings.getAtom("M");
            final String fun = bindings.getAtom("F");
            final Collection<OtpErlangObject> locations = bindings.getList("L");
            final OtpErlangObject reason = bindings.get("R");
            test = findCase(mod, fun);
            test.setFailed(reason, locations);
        } else if ("skip".equals(tag)) {
            // value = {Module, Function, Comment
            final OtpBindings bindings = OtpErlang.match("{M:a,F:a,C}", value);
            final String mod = bindings.getAtom("M");
            final String fun = bindings.getAtom("F");
            final OtpErlangObject reason = bindings.get("C");
            test = findCase(mod, fun);
            test.setSkipped(reason);
        } else if ("done".equals(tag)) {
            // value = Module, Log, {Successful,Failed,Skipped}, [Results]}
            final OtpBindings bindings = OtpErlang.match("{M,L,{S:i,F:i,K:i},R}", value);
            final int successful = bindings.getInt("S");
            final int failed = bindings.getInt("F");
            final int skipped = bindings.getInt("K");
            label.setText(label.getText() + " -- Done! Successful: " + successful
                    + ", Failed: " + failed + ", Skipped: " + skipped);
        }
        control.redraw();
    }

    private TestCaseData findCase(final String mod, final String fun) {
        for (final TestCaseData data : events) {
            if (data.getModule().equals(mod) && data.getFunction().equals(fun)) {
                return data;
            }
        }
        final TestCaseData data = new TestCaseData(mod, fun);
        events.add(data);
        return data;
    }

    private String formatTitle(final OtpErlangObject value) {
        try {
            final OtpBindings b = OtpErlang.match("{D,S,C}", value);
            final String suite = b.getAtom("S");
            final String tcase = b.getAtom("C");
            if (tcase.length() == 0) {
                return "suite " + suite;
            }
            return "suite " + suite + "; case " + tcase;
        } catch (final OtpParserException e) {
        } catch (final OtpErlangException e) {
        }
        return value.toString();
    }

}
