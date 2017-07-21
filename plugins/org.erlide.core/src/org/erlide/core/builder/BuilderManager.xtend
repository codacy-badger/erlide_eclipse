package org.erlide.core.builder

import org.erlide.core.internal.builder.InternalBuilder
import org.erlide.core.internal.builder.external.MakeBuilder
import org.erlide.core.internal.builder.external.RebarBuilder
import org.erlide.engine.model.root.ProjectConfigType

class BuilderManager {
    public val configurations = #[ProjectConfigType.INTERNAL, ProjectConfigType.REBAR]
    public val builders = #[new InternalBuilder(), new RebarBuilder(), new MakeBuilder()]
}
