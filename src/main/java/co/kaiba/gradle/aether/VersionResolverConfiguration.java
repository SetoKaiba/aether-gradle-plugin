package co.kaiba.gradle.aether;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class VersionResolverConfiguration implements Action<Configuration> {

    private Project project;
    private AetherPlugin aetherPlugin;

    public VersionResolverConfiguration(Project project, AetherPlugin aetherPlugin) {
        this.project = project;
        this.aetherPlugin = aetherPlugin;
    }

    @Override
    public void execute(Configuration configuration) {
        configuration.getResolutionStrategy().eachDependency(new VersionResolverDependencyResolveDetails(project, configuration, aetherPlugin));
    }
}
