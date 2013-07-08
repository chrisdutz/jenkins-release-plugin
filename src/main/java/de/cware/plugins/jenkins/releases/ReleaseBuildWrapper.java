package de.cware.plugins.jenkins.releases;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.tasks.BuildWrapper;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: cdutz
 * Date: 26.10.12
 * Time: 10:41
 */
public class ReleaseBuildWrapper extends BuildWrapper {

    private String mavenArgs;

    private String mavenRepoUrl;
    private String mavenRepoUser;
    private String mavenRepoPassword;

    @DataBoundConstructor
    public ReleaseBuildWrapper(String mavenArgs, String mavenRepoUrl, String mavenRepoUser, String mavenRepoPassword) {
        this.mavenArgs = mavenArgs;
        this.mavenRepoUrl = mavenRepoUrl;
        this.mavenRepoUser = mavenRepoUser;
        this.mavenRepoPassword = mavenRepoPassword;
    }

    ////////////////////////////////////////////////////////////////
    // Action methods
    ////////////////////////////////////////////////////////////////

    /**
     * Each BuildWrapper can add extra Action to the Actions panel on the left hand side.
     * getProjectActions is the callback in which the BuildWrapper has the chance to return
     * the actions it intends to add.
     *
     * In this case we add one Action for performing a release build. Returning an instance
     * of the ReleaseBuildAction will make Jenkins add a "Release Modules" Link in the main
     * actions menu. Clicking on this will allow the user to decide what type of release he
     * wants to perform and initiate the release build.
     *
     * In general this BuildWrapper is called on every build of jobs it is enabled on. The
     * ReleaseBuildAction will add a marker to the job. When executing a job Jenkins will
     * call setUp before the job execution.
     *
     * In the setUp method of this BuildWrapper the isReleaseBuild method will be inspecting
     * the job for the marker and only if this marker is present, the actual Wrapper logic
     * will be executed.
     *
     * @param job Reference to the current project. This is needed in order to correctly
     *            initialize the Action.
     * @return List of one action, used for triggering release builds.
     */
    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject job) {
        final ReleaseBuildAction releaseBuildAction = new ReleaseBuildAction((MavenModuleSet) job);
        return Arrays.asList(releaseBuildAction);
    }

    /**
     * This method is called when setting up the build. Unfortunately we have to implement this
     * as the default implementation would assume we were relying on a deprecated implementation.
     *
     * In case of this plugin the actual contribution to the build is done by the
     * ReleaseInterceptorActions which the ReleaseBuildAction added when scheduling the build.
     * As all work is already done, we simply have to prevent the error that would occur if this
     * method was not implemented.
     */
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        // Return an empty environment as we are not changing anything.
        return new Environment() {};
    }

    public String getMavenArgs() {
        return mavenArgs;
    }

    public String getMavenRepoUrl() {
        return mavenRepoUrl;
    }

    public String getMavenRepoUser() {
        return mavenRepoUser;
    }

    public String getMavenRepoPassword() {
        return mavenRepoPassword;
    }

}
