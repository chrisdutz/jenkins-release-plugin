package de.cware.plugins.jenkins.releases;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.util.ArgumentListBuilder;

import java.util.Map;

/**
 * Initially Jenkins only knew Actions as being extensions to the Actions menu
 * on the left hand side. In this case the Action is an invisible Action passed
 * from the ReleaseBuildAction to the ReleaseBuildWrapper. In order to keep it
 * invisible the 3 Action interface methods must all return null.
 *
 * As in a minor release only the selected projects will be released and these
 * can all contain different versions. Therefore this action will have to provide
 * both the information about which modules are to be released as well as which
 * versions these should have.
 *
 * User: cdutz
 * Date: 26.10.12
 * Time: 10:42
 */
public class MinorReleaseInterceptorAction implements ReleaseInterceptorAction {

    private final Map<MavenModule, String> releaseVersions;
    private final Map<MavenModule, String> latestVersions;

    public MinorReleaseInterceptorAction(
            Map<MavenModule, String> releaseVersions, Map<MavenModule, String> latestVersions) {
        this.releaseVersions = releaseVersions;
        this.latestVersions = latestVersions;
    }

    ////////////////////////////////////////////////////////////////
    // MavenArgumentInterceptorAction methods
    ////////////////////////////////////////////////////////////////

    public String getGoalsAndOptions(MavenModuleSetBuild build) {
        final StringBuilder cmd = new StringBuilder();

        if((releaseVersions != null) && (releaseVersions.size() > 0)) {
            // Save the current version as will be passed to the release plugin as development version.
            final String currentVersion = build.getProject().getRootModule().getVersion();

            // Define the name of he tag the release plugin will use.
            cmd.append(" -Dtag=").append(releaseVersions.get(build.getProject().getRootModule()));
            // Make sure the tag name is just the version.
            cmd.append(" -DtagNameFormat=@{project.version}");

            final StringBuilder releasePluginVersionsFragment = new StringBuilder();
            final StringBuilder projectsFragment = new StringBuilder();

            // Limit maven to only release the modules that should be released
            // and configure the desired versions of those artifacts.
            for(final MavenModule module : releaseVersions.keySet()) {
                final String moduleName = module.getModuleName().groupId + ":" + module.getModuleName().artifactId;

                final String moduleReleaseVersion = releaseVersions.get(module);
                releasePluginVersionsFragment.append(" -Dproject.rel.").append(moduleName).append("=");
                releasePluginVersionsFragment.append(moduleReleaseVersion);
                releasePluginVersionsFragment.append(" -Dproject.dev.").append(moduleName).append("=");
                releasePluginVersionsFragment.append(currentVersion);

                if(projectsFragment.length() > 0) {
                    projectsFragment.append(",");
                }
                projectsFragment.append(moduleName);
            }

            // Tell the release plugin the latest versions of all of the modules not in the build. This way
            // the release plugin can update the dependencies to non-release modules too (Hopefully).
            for(final MavenModule module : latestVersions.keySet()) {
                final String moduleName = module.getModuleName().groupId + ":" + module.getModuleName().artifactId;
                final String moduleLatestVersion = latestVersions.get(module);

                releasePluginVersionsFragment.append(" -Dproject.rel.").append(moduleName).append("=");
                releasePluginVersionsFragment.append(moduleLatestVersion);
                releasePluginVersionsFragment.append(" -Dproject.dev.").append(moduleName).append("=");
                releasePluginVersionsFragment.append(currentVersion);
            }

            // Restrict the reactor only to use the selected modules.
            //cmd.append(" --projects ").append(projectsFragment.toString());

            // Add the versions of all of the project.
            cmd.append(releasePluginVersionsFragment);

            // As not all projects are in the reactor, the project will contain modules
            // that contain SNAPSHOT versions, so we have to disable this test.
            cmd.append(" -DignoreSnapshots=true");

            // Limit the build to only handle the projects we want to release.
            cmd.append(" --projects ").append(projectsFragment.toString());

            // if the user specified additional maven parameters, append them to the command.
            final ReleaseBuildWrapper releaseBuildWrapper =
                    build.getProject().getBuildWrappersList().get(ReleaseBuildWrapper.class);
            if(releaseBuildWrapper.getMavenArgs() != null) {
                cmd.append(" ").append(releaseBuildWrapper.getMavenArgs().trim());
            }
        }

        cmd.append(" release:prepare release:perform");

        return cmd.toString();
    }

    public ArgumentListBuilder intercept(ArgumentListBuilder mavenArgs, MavenModuleSetBuild build) {
        return null;
    }

    ////////////////////////////////////////////////////////////////
    // Action methods
    ////////////////////////////////////////////////////////////////
    // Note:
    //
    // This Action does not contribute anything to the UI so by
    // returning 'null' for all of the 3 Action interface methods
    // this Action will be ignored by the Stapler framework.
    ////////////////////////////////////////////////////////////////

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

}
