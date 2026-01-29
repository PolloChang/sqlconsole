package work.pollochang.sqlconsole.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MavenDriverResolverService {

    @Value("${app.home:./drivers}")
    private String appHome;

    private RepositorySystem repositorySystem;
    private RepositorySystemSession session;
    private Path localRepoPath;

    @PostConstruct
    public void init() throws IOException {
        localRepoPath = Paths.get(appHome, "maven-cache");
        Files.createDirectories(localRepoPath);

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        repositorySystem = locator.getService(RepositorySystem.class);

        DefaultRepositorySystemSession defaultSession = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
        defaultSession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(defaultSession, localRepo));

        this.session = defaultSession;
    }

    public List<Path> resolveArtifacts(String coords) {
        log.info("Resolving Maven artifact: {}", coords);

        Artifact artifact = new DefaultArtifact(coords);

        Dependency dependency = new Dependency(artifact, "compile");
        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(Collections.singletonList(central));

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);

        try {
            List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(session, dependencyRequest).getArtifactResults();

            return artifactResults.stream()
                    .map(result -> result.getArtifact().getFile().toPath())
                    .collect(Collectors.toList());

        } catch (DependencyResolutionException e) {
             throw new RuntimeException("Failed to resolve artifacts for: " + coords, e);
        }
    }
}
