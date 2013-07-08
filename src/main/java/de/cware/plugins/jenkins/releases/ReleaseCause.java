package de.cware.plugins.jenkins.releases;

import hudson.model.Cause;
import hudson.model.Hudson;

/**
 * Created with IntelliJ IDEA.
 * User: cdutz
 * Date: 26.10.12
 * Time: 09:04
 */
public class ReleaseCause extends Cause.UserCause {

    private String authenticationName;

    public ReleaseCause() {
        this.authenticationName = Hudson.getAuthentication().getName();
    }

    @Override
    public String getUserName() {
        return authenticationName;
    }

    @Override
    public String getShortDescription() {
        return Messages.ReleaseCause_ShortDescription(authenticationName);
    }

}
