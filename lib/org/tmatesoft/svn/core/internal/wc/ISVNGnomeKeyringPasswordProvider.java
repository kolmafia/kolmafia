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
package org.tmatesoft.svn.core.internal.wc;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
import org.tmatesoft.svn.core.SVNException;

public interface ISVNGnomeKeyringPasswordProvider {

    public String getKeyringPassword(String keyringName) throws SVNException;
}
