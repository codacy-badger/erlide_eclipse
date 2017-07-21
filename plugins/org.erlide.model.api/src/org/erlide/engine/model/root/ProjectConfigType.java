package org.erlide.engine.model.root;

public enum ProjectConfigType {
    // FIXME this is kind of an indirect dep on core plugin (needs to be
    // started)

    // @formatter:off
    INTERNAL("org.erlide.core"),
    REBAR("rebar.config");
    // @formatter:on

    private final String name;

    ProjectConfigType(final String name) {
        this.name = name;
    }

    /**
     * A string that points to where the configuration is stored for this project. The
     * configurator interprets the value as fit, it can be a file name or a preference
     * node name.
     */
    public String getConfigName() {
        return name;
    }

}
