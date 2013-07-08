package de.cware.plugins.jenkins.releases;

import hudson.maven.MavenModuleSetBuild;
import hudson.util.ArgumentListBuilder;

/**
 * Initially Jenkins only knew Actions as being extensions to the Actions menu
 * on the left hand side. In this case the Action is an invisible Action passed
 * from the ReleaseBuildAction to the ReleaseBuildWrapper. In order to keep it
 * invisible the 3 Action interface methods must all return null.
 *
 * This interceptor action is used when a previous release-attempt failed and
 * the release has to be rolled-back. Not doing a rollback and trying to re-
 * release would certainly result in failure.
 *
 * User: cdutz
 * Date: 01.11.12
 * Time: 10:35
 */
public class CleanupInterceptorAction implements ReleaseInterceptorAction {

    public CleanupInterceptorAction() {
    }

    ////////////////////////////////////////////////////////////////
    // MavenArgumentInterceptorAction methods
    ////////////////////////////////////////////////////////////////

    public String getGoalsAndOptions(MavenModuleSetBuild build) {
        // TODO: Find out which directories have dirty release data and limit the reactor to these projects.
        return "release:rollback";
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
