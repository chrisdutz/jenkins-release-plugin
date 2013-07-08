package de.cware.plugins.jenkins.releases;

import hudson.Extension;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapperDescriptor;

/**
 * This Jenkins Extension is the central extension point for including the release plugin
 * into Jenkins. It tells Jenkins which class is the implementation of this BuildWrapper
 * and takes care of loading it's configuration.
 *
 * On the job configuration wiew the getDisplayName returns the text to be
 * used in the "Buildenvironmen" section, to allow enabling of the ReleaseBuildWrapper,
 * which then takes care of wrapping all the fun stuf around a normal Maven build.
 *
 * This is also why isApplicable only returns true, if the project is a Maven project.
 *
 * User: cdutz
 * Date: 26.10.12
 * Time: 11:19
 */
@Extension
public class ReleaseBuildWrapperDescriptor extends BuildWrapperDescriptor {

    public ReleaseBuildWrapperDescriptor() {
        super(ReleaseBuildWrapper.class);

        // Load the persisted properties from file.
        load();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        return (item instanceof AbstractMavenProject);
    }

    @Override
    public String getDisplayName() {
        return Messages.ReleaseBuildWrapperDescriptor_DisplayName();
    }

}
