/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DefaultSVNHostOptionsProvider implements ISVNHostOptionsProvider {

    private final File myConfigDirectory;
    private SVNCompositeConfigFile myServersFile;
    private Map myServersOptions;

    public DefaultSVNHostOptionsProvider() {
        this(null);
    }

    public DefaultSVNHostOptionsProvider(File configDirectory) {
        myConfigDirectory = configDirectory == null ? SVNWCUtil.getDefaultConfigurationDirectory() : configDirectory;
    }

    public void setInMemoryServersOptions(Map serversOptions) {
        myServersOptions = serversOptions;
    }

    protected SVNCompositeConfigFile getServersFile() {
        if (myServersFile == null) {
            SVNConfigFile.createDefaultConfiguration(myConfigDirectory);
            SVNConfigFile userConfig = new SVNConfigFile(new File(myConfigDirectory, "servers"));
            SVNConfigFile systemConfig = new SVNConfigFile(new File(SVNFileUtil.getSystemConfigurationDirectory(), "servers"));
            myServersFile = new SVNCompositeConfigFile(systemConfig, userConfig);
            myServersFile.setGroupsToOptions(myServersOptions);
        }
        return myServersFile;
    }

    public ISVNHostOptions getHostOptions(SVNURL url) {
        return new DefaultSVNHostOptions(getServersFile(), url);
    }
}
