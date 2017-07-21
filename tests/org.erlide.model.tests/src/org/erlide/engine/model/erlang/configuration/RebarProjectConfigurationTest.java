package org.erlide.engine.model.erlang.configuration;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.erlide.engine.model.root.ErlangProjectProperties;
import org.erlide.engine.model.root.FileProjectConfigurator;
import org.erlide.engine.model.root.IProjectConfigurator;
import org.erlide.engine.model.root.ProjectConfigType;
import org.erlide.engine.model.root.ProjectConfigurationSerializer;
import org.erlide.engine.model.root.ProjectConfiguratorFactory;
import org.junit.Test;

public class RebarProjectConfigurationTest extends AbstractProjectConfigurationTest {

    @Test
    public void configCanBeParsed() throws CoreException {
        project.setConfigType(ProjectConfigType.REBAR);
        project.storeAllProperties();
        setFileContent(ProjectConfigType.REBAR.getConfigName(), "");

        final IProjectConfigurator persister = ProjectConfiguratorFactory.getDefault()
                .getConfig(project.getConfigType(), project);
        final ProjectConfigurationSerializer configurator = ((FileProjectConfigurator) persister)
                .getSerializer();

        final ErlangProjectProperties expected = new ErlangProjectProperties();
        expected.setOutputDir(new Path("ebin"));
        final ErlangProjectProperties actual = configurator.decodeConfig("");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void propertiesShouldFollowConfigFileChange() throws CoreException {
        project.setConfigType(ProjectConfigType.REBAR);
        final String cfgFile = ProjectConfigType.REBAR.getConfigName();
        final String config = getFileContent(cfgFile);

        final String config1 = config + "{erl_opts, [{i, \"myinclude\"}, "
                + "{src_dirs, [\"src\", \"src2\"]}]}.";
        setFileContent(cfgFile, config1);

        final ErlangProjectProperties p2 = project.getProperties();

        final Collection<IPath> actualSources = p2.getSourceDirs();
        assertThat(actualSources).hasSize(2);
        assertThat(actualSources).contains(new Path("src2"));

        final Collection<IPath> actualIncludes = p2.getIncludeDirs();
        assertThat(actualIncludes).hasSize(1);
        assertThat(actualIncludes).contains(new Path("myinclude"));
    }
}
