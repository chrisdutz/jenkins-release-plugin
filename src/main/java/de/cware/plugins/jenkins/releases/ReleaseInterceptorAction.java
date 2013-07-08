package de.cware.plugins.jenkins.releases;

import hudson.maven.MavenArgumentInterceptorAction;

/**
 * Base interface for all ReleaseInterceptorActions. Having one base interface
 * allows the ReleaseBuildWrapper to generally react on any type of release build
 * triggered by this plugin.
 *
 * User: cdutz
 * Date: 26.10.12
 * Time: 13:57
 */
public interface ReleaseInterceptorAction extends MavenArgumentInterceptorAction {
}
