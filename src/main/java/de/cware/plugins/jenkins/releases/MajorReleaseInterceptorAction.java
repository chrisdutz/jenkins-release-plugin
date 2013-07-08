package de.cware.plugins.jenkins.releases;

import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.util.ArgumentListBuilder;
import hudson.util.RunList;

import java.io.File;
import java.io.IOException;

/**
 * Initially Jenkins only knew Actions as being extensions to the Actions menu
 * on the left hand side. In this case the Action is an invisible Action passed
 * from the ReleaseBuildAction to the ReleaseBuildWrapper. In order to keep it
 * invisible the 3 Action interface methods must all return null.
 *
 * As in a major release all projects should be set to the same version, this
 * action will only have to provide the version string for the major release.
 *
 * User: cdutz
 * Date: 26.10.12
 * Time: 10:42
 */
public class MajorReleaseInterceptorAction implements ReleaseInterceptorAction {

    private final String majorReleaseVersion;

    public MajorReleaseInterceptorAction(String majorReleaseVersion) {
        this.majorReleaseVersion = majorReleaseVersion;
    }

    public String getMajorReleaseVersion() {
        return majorReleaseVersion;
    }

    ////////////////////////////////////////////////////////////////
    // MavenArgumentInterceptorAction methods
    ////////////////////////////////////////////////////////////////

    public String getGoalsAndOptions(MavenModuleSetBuild build) {
        final StringBuilder cmd = new StringBuilder();

        if(majorReleaseVersion != null) {
            // Save the current version as will be passed to the release plugin as development version.
            final String currentVersion = build.getProject().getRootModule().getVersion();

            // Prevent maven from asking questions.
            cmd.append(" --batch-mode");

            // Define the name of he tag the release plugin will use.
            cmd.append(" -Dtag=").append(majorReleaseVersion);
            // Make sure the tag name is just the version.
            cmd.append(" -DtagNameFormat=@{project.version}");

            // Define the version the artifacts will be released as.
            cmd.append(" -DreleaseVersion=").append(majorReleaseVersion);

            // Define the version of the modules will have in their poms after the release is finished.
            cmd.append(" -DdevelopmentVersion=").append(currentVersion);

            // if the user specified additional maven parameters, append them to the command.
            final ReleaseBuildWrapper releaseBuildWrapper = build.getProject().getBuildWrappersList().get(ReleaseBuildWrapper.class);
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

    protected File getProjectWorkDir(MavenModuleSet project) {
        final RunList<MavenModuleSetBuild> builds = project.getBuilds();
        // This code assumes that the builds work dir stays the same during builds.
        final MavenModuleSetBuild build = builds.getLastBuild();

        // Convert the FilePath into a File object that we can actually use.
        if(build != null) {
            try {
                return new File(build.getWorkspace().toURI().getPath());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
