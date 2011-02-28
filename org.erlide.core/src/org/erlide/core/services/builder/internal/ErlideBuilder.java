package org.erlide.core.services.builder.internal;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.erlide.core.ErlangCore;
import org.erlide.core.backend.BackendException;
import org.erlide.core.backend.BackendManager;
import org.erlide.core.backend.ErlideBackend;
import org.erlide.core.backend.RpcCallSite;
import org.erlide.core.backend.rpc.RpcFuture;
import org.erlide.core.model.erlang.util.CoreUtil;
import org.erlide.jinterface.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.google.common.collect.Lists;

public class ErlideBuilder {

    public static RpcFuture compileErl(final RpcCallSite backend,
            final IPath fn, final String outputdir,
            final Collection<IPath> includedirs,
            final OtpErlangList compilerOptions) {
        final List<String> incs = Lists.newArrayList();
        for (final IPath p : includedirs) {
            incs.add(p.toString());
        }
        try {
            return backend.async_call("erlide_builder", "compile", "sslsx",
                    fn.toString(), outputdir, incs, compilerOptions);
        } catch (final Exception e) {
            ErlLogger.debug(e);
            return null;
        }
    }

    public static OtpErlangList getSourceClashes(final RpcCallSite backend,
            final String[] dirList) throws BackendException {
        final OtpErlangObject res = backend.call("erlide_builder",
                "source_clash", "ls", (Object) dirList);
        if (res instanceof OtpErlangList) {
            return (OtpErlangList) res;
        }
        throw new BackendException(
                "bad result from erlide_builder:source_clash: " + res);
    }

    public static OtpErlangList getCodeClashes(final RpcCallSite b)
            throws BackendException {
        final OtpErlangList res = (OtpErlangList) b.call("erlide_builder",
                "code_clash", null);
        return res;
    }

    public static void loadModule(final IProject project, final String module) {
        try {
            final BackendManager backendManager = ErlangCore
                    .getBackendManager();
            for (final ErlideBackend b : backendManager
                    .getExecutionBackends(project)) {
                ErlLogger.debug(":: loading %s in %s", module, b.getInfo()
                        .toString());
                if (b.isDistributed()) {
                    b.call("erlide_builder", "load", "ao", module,
                            b.doLoadOnAllNodes());
                } else {
                    CoreUtil.loadModuleViaInput(b, project, module);
                }
                backendManager.moduleLoaded(b, project, module);
            }
        } catch (final Exception e) {
            ErlLogger.debug(e);
        }
    }

    public static RpcFuture compileYrl(final RpcCallSite backend,
            final String fn, final String output) {
        try {
            return backend.async_call("erlide_builder", "compile_yrl", "ss",
                    fn, output);
        } catch (final Exception e) {
            ErlLogger.debug(e);
            return null;
        }
    }

}