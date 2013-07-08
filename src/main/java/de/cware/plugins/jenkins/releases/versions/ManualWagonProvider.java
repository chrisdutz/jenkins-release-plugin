package de.cware.plugins.jenkins.releases.versions;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.sonatype.aether.connector.wagon.WagonProvider;

/**
 * A simplistic provider for wagon instances when no Plexus-compatible IoC container is used.
 */
public class ManualWagonProvider
        implements WagonProvider {

    public Wagon lookup(String roleHint)
            throws Exception {
        if ("http".equals(roleHint)) {
            return new HttpWagon();
        } else if ("https".equals(roleHint)) {
            return new HttpWagon();
        }
        return null;
    }

    public void release(Wagon wagon) {

    }

}
