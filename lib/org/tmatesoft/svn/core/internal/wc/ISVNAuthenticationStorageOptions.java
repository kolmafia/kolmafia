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

import org.tmatesoft.svn.core.SVNException;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public interface ISVNAuthenticationStorageOptions {

    ISVNAuthenticationStorageOptions DEFAULT = new ISVNAuthenticationStorageOptions() {
        public boolean isNonInteractive() throws SVNException {
            return false;
        }

        public ISVNAuthStoreHandler getAuthStoreHandler() throws SVNException {
            return null;
        }

        public boolean isSSLPassphrasePromptSupported() {
            return false;
        }

        public ISVNGnomeKeyringPasswordProvider getGnomeKeyringPasswordProvider() {
            return null;
        }
    };

    boolean isNonInteractive() throws SVNException;

    ISVNAuthStoreHandler getAuthStoreHandler() throws SVNException;

    boolean isSSLPassphrasePromptSupported();

    ISVNGnomeKeyringPasswordProvider getGnomeKeyringPasswordProvider();
}
