package co.kaiba.gradle.aether;

import java.util.HashMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AetherPlugin implements Plugin<Project> {

    private HashMap<String, HashMap<String, String>> versionMap = new HashMap<>();

    public HashMap<String, HashMap<String, String>> getVersionMap() {
        return versionMap;
    }

    @Override
    public void apply(Project project) {
        project.getConfigurations().all(new VersionResolverConfiguration(project, this));
    }
}
