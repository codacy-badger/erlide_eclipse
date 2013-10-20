package org.erlide.core.internal.builder.external;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.erlide.core.executor.ToolExecutor;
import org.erlide.core.executor.ToolExecutor.ToolResults;
import org.erlide.core.internal.builder.BuilderConfigurator;
import org.erlide.engine.model.root.IErlangProjectProperties;

public class MakeConfigurator implements BuilderConfigurator {

    @Override
    public String encodeConfig(final IProject project, final IErlangProjectProperties info) {
        // do nothing, creating a generic Makefile is too difficult
        // maybe if users demand it
        return null;
    }

    @Override
    public IErlangProjectProperties decodeConfig(final String config) {
        // do nothing at the moment
        return null;
    }

    /*
     * This is usable only for small, "normal" Makefiles.
     */
    Collection<String> getMakefileTargets(final String dir) {
        final ToolExecutor ex = new ToolExecutor();
        final ToolResults make = ex.run("/bin/bash",
                "-c \"make -rpn | sed -n -e '/^$/ { n ; /^[^ ]*:/p }' "
                        + "| grep -v ^.PHONY | cut -d : -f 1\"", dir);
        return make.output;
    }

    @Override
    public String getConfigFile() {
        return "Makefile";
    }
}
