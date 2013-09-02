package org.erlide.model.services.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.erlide.runtime.api.IRpcSite;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class ErlideIndent implements IndentService {

    private List<OtpErlangTuple> fixIndentPrefs(final Map<String, String> m) {
        final List<OtpErlangTuple> result = new ArrayList<OtpErlangTuple>(
                m.size());
        for (final Map.Entry<String, String> e : m.entrySet()) {
            final OtpErlangAtom a = new OtpErlangAtom(e.getKey());
            final String s = e.getValue();
            int n;
            if ("false".equals(s)) {
                n = 0;
            } else if ("true".equals(s)) {
                n = 1;
            } else {
                n = Integer.parseInt(s);
            }
            final OtpErlangLong l = new OtpErlangLong(n);
            final OtpErlangTuple t = new OtpErlangTuple(new OtpErlangObject[] {
                    a, l });
            result.add(t);
        }
        return result;
    }

    @Override
    @SuppressWarnings("boxing")
    public IndentResult indentLine(final IRpcSite b, final String oldLine,
            final String txt, final String insertedText, final int tabw,
            final boolean useTabs, final Map<String, String> prefs)
            throws RpcException, OtpErlangRangeException {
        ErlLogger.debug("indentLine '%s'", txt);
        final OtpErlangObject o = b.call("erlide_indent", "indent_line",
                "sssiox", txt, oldLine, insertedText, tabw, useTabs,
                fixIndentPrefs(prefs));
        return new IndentResult(o);
    }

    @Override
    @SuppressWarnings("boxing")
    public OtpErlangObject indentLines(final IRpcSite b, final int offset,
            final int length, final String text, final int tabw,
            final boolean useTabs, final Map<String, String> prefs)
            throws RpcException {
        final OtpErlangObject o = b.call(40000, "erlide_indent",
                "indent_lines", "siiiolx", text, offset, length, tabw, useTabs,
                fixIndentPrefs(prefs));
        return o;
    }

    @Override
    public OtpErlangObject templateIndentLines(final IRpcSite b,
            final String prefix, final String text, final int tabw,
            final boolean useTabs, final Map<String, String> prefs)
            throws RpcException {
        final OtpErlangObject o = b.call(20000, "erlide_indent",
                "template_indent_lines", "ssiolx", prefix, text, tabw, useTabs,
                fixIndentPrefs(prefs));
        return o;
    }

}
