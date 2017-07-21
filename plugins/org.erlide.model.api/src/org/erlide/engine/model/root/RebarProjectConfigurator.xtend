package org.erlide.engine.model.root

import org.erlide.engine.model.root.FileProjectConfigurator
import org.erlide.engine.model.root.RebarConfigurationSerializer

class RebarProjectConfigurator extends FileProjectConfigurator {

    new(String path) {
        super("rebar", new RebarConfigurationSerializer(), path)
    }

}
