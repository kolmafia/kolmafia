/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNSimpleVersionedPropertiesImpl extends SVNVersionedProperties {

    public SVNSimpleVersionedPropertiesImpl(SVNProperties props) {
        super(props);
    }

    public boolean containsProperty(String name) throws SVNException {
        return getProperties() != null && getProperties().containsName(name);
    }

    public SVNPropertyValue getPropertyValue(String name) throws SVNException {
        return getProperties() != null ? getProperties().getSVNPropertyValue(name) : null;
    }

    protected SVNProperties loadProperties() throws SVNException {
        return getProperties();
    }

    protected SVNVersionedProperties wrap(SVNProperties properties) {
        return new SVNSimpleVersionedPropertiesImpl(properties);
    }

}
