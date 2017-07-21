package org.erlide.engine.model.root

import org.eclipse.xtend.lib.annotations.Data

@Data
abstract class ProjectConfigurator implements IProjectConfigurator {
    val String name
    val String location
}

