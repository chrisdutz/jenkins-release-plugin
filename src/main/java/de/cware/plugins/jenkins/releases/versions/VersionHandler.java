package de.cware.plugins.jenkins.releases.versions;

import de.cware.plugins.jenkins.releases.ReleaseBuildWrapper;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.ModuleName;

import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.version.Version;

import java.util.HashMap;
import java.util.Map;

/**
 * Little helper class that wraps accessing a nexus server for retrieving
 * the latest versions of artifacts that are part of this build.
 * <p/>
 * User: cdutz
 * Date: 25.10.12
 * Time: 15:20
 */
public class VersionHandler {

    protected MavenModuleSet project;

    protected String majorVersion;

    protected Map<String, String> moduleVersions;

    public VersionHandler(MavenModuleSet project) {
        this.project = project;

        // Die Major Version besteht aus der version des root Projektes,
        // bei dem der Suffix "-SNAPSHOT" abgeschitten wird.
        if ((project.getRootModule() != null) && (project.getRootModule().getModuleName() != null)) {
            final String rootModuleVersion = project.getRootModule().getVersion();
            if(rootModuleVersion.contains("-SNAPSHOT")) {
                majorVersion = rootModuleVersion.substring(0, rootModuleVersion.indexOf("-SNAPSHOT"));
            } else {
                majorVersion = rootModuleVersion;
            }

            moduleVersions = new HashMap<String, String>();
            try {
                final ReleaseBuildWrapper releaseBuildWrapper =
                        project.getBuildWrappersList().get(ReleaseBuildWrapper.class);
                final RemoteRepository repo = Booter.newCentralRepository(releaseBuildWrapper.getMavenRepoUrl());
                final RepositorySystem system = Booter.newRepositorySystem();
                final RepositorySystemSession session = Booter.newRepositorySystemSession(system);

                // If authentication credentials are provided, use them to authenticate.
                if ((releaseBuildWrapper.getMavenRepoUser() != null) &&
                        (releaseBuildWrapper.getMavenRepoPassword() != null)) {
                    repo.setAuthentication(new Authentication(
                            releaseBuildWrapper.getMavenRepoUser(), releaseBuildWrapper.getMavenRepoPassword()));
                }

                populateLatestVersionForMajorReleaseMap(majorVersion, project.getRootModule(), repo, system, session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void populateLatestVersionForMajorReleaseMap(String majorVersion, MavenModule module,
                                                           RemoteRepository repo, RepositorySystem system,
                                                           RepositorySystemSession session) {
        try {
            final String versionRange = "[" + majorVersion + "," + getNextVersion(majorVersion) + ")";
            final Artifact artifact = new DefaultArtifact(module.getModuleName().groupId + ":" +
                    module.getModuleName().artifactId + ":" + versionRange);
            final VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(artifact);
            rangeRequest.addRepository(repo);
            final VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);
            final Version newestVersion = rangeResult.getHighestVersion();
            if (newestVersion != null) {
                final String key = module.getModuleName().groupId + ":" + module.getModuleName().artifactId;
                moduleVersions.put(key, newestVersion.toString());
            }
        } catch (Exception e) {
            // Ignore.
        }

        if (module.getChildren() != null && !module.getChildren().isEmpty()) {
            for (final MavenModule child : module.getChildren()) {
                populateLatestVersionForMajorReleaseMap(majorVersion, child, repo, system, session);
            }
        }
    }

    public String getMajorVersion() {
        return majorVersion + ".0";
    }

    public boolean isNotReleased(ModuleName moduleName) {
        final String key = moduleName.groupId + ":" + moduleName.artifactId;
        return !moduleVersions.containsKey(key);
    }

    public String getCurrentReleaseVersion(ModuleName moduleName) {
        final String key = moduleName.groupId + ":" + moduleName.artifactId;
        if(moduleVersions.containsKey(key)) {
            return moduleVersions.get(key);
        }
        return "- not released -";
    }

    public String getNextReleaseVersion(ModuleName moduleName) {
        final String key = moduleName.groupId + ":" + moduleName.artifactId;
        if(moduleVersions.containsKey(key)) {
            final String latestVersion = moduleVersions.get(key);
            return getNextVersion(latestVersion);
        } else {
            return getMajorVersion();
        }
    }

    protected String getNextVersion(String currentVersion) {
        final int minorVersion = Integer.valueOf(currentVersion.substring(currentVersion.lastIndexOf(".") + 1));
        return currentVersion.substring(0, currentVersion.lastIndexOf(".") + 1) + (minorVersion + 1);
    }

}
