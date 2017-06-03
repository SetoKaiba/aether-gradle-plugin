package co.kaiba.gradle.aether;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

public class VersionResolverDependencyResolveDetails implements Action<DependencyResolveDetails> {

    private Project project;
    private Configuration configuration;
    private AetherPlugin aetherPlugin;

    public VersionResolverDependencyResolveDetails(Project project, Configuration configuration, AetherPlugin aetherPlugin) {
        this.project = project;
        this.configuration = configuration;
        this.aetherPlugin = aetherPlugin;
    }

    @Override
    public void execute(DependencyResolveDetails dependencyResolveDetails) {
        String group = dependencyResolveDetails.getTarget().getGroup();
        String name = dependencyResolveDetails.getTarget().getName();
        if (aetherPlugin.getVersionMap().containsKey(group) && aetherPlugin.getVersionMap().get(group).containsKey(name)) {
            if (dependencyResolveDetails.getRequested().getVersion().equals(dependencyResolveDetails.getTarget().getVersion())) {
                dependencyResolveDetails.useVersion(aetherPlugin.getVersionMap().get(group).get(name));
            }
        } else {
            RepositorySystem system = setupRepositorySystem();
            RepositorySystemSession session = setupSession(project, system);
            List<RemoteRepository> remoteRepositories = new ArrayList<>();
            for (ArtifactRepository artifactRepository : project.getRepositories()) {
                if (artifactRepository instanceof MavenArtifactRepository) {
                    MavenArtifactRepository mavenArtifactRepository = (MavenArtifactRepository) artifactRepository;
                    remoteRepositories.add(new RemoteRepository.Builder(mavenArtifactRepository.getName(), "default", mavenArtifactRepository.getUrl().toString()).build());
                }
            }
            Artifact artifact = new DefaultArtifact(dependencyResolveDetails.getTarget().getGroup()
                    + ":" + dependencyResolveDetails.getTarget().getName()
                    + ":" + dependencyResolveDetails.getTarget().getVersion());

            CollectRequest collectRequest = new CollectRequest();
            String scope;
            if (configuration.getName().contains("test")) {
                scope = JavaScopes.TEST;
            } else if (configuration.getName().contains("runtime")) {
                scope = JavaScopes.RUNTIME;
            } else if (configuration.getName().equals("providedCompile")
                    || configuration.getName().equals("compileOnly")) {
                scope = JavaScopes.PROVIDED;
            } else
                scope = JavaScopes.COMPILE;
            collectRequest.setRoot(new Dependency(artifact, scope));
            collectRequest.setRepositories(remoteRepositories);

            try {
                CollectResult collectResult = system.collectDependencies(session, collectRequest);
                processDependencyNode(collectResult.getRoot());
            } catch (DependencyCollectionException e) {
                e.printStackTrace();
            }
        }
    }

    public RepositorySystem setupRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    public RepositorySystemSession setupSession(Project project, RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        File dir = new File(project.getProjectDir(), "aether/repository");
        LocalRepository localRepo = new LocalRepository(dir);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    private void processDependencyNode(DependencyNode dependencyNode) {
        Artifact artifact = dependencyNode.getArtifact();
        if (!aetherPlugin.getVersionMap().containsKey(artifact.getGroupId()))
            aetherPlugin.getVersionMap().put(artifact.getGroupId(), new HashMap<>());
        aetherPlugin.getVersionMap().get(artifact.getGroupId()).put(artifact.getArtifactId(), artifact.getVersion());
        for (DependencyNode node : dependencyNode.getChildren()) {
            processDependencyNode(node);
        }
    }
}
