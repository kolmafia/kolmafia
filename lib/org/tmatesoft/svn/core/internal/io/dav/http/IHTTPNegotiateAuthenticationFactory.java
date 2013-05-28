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
package org.tmatesoft.svn.core.internal.io.dav.http;


/**
 * Factory interface for Negotiate authentication support.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface IHTTPNegotiateAuthenticationFactory {
    
    /**
     * Create a negotiate authentication handler.  
     * 
     * @param prev The current handler; may not be of the same class
     * @param requestID Identifier of request in a single connection; allows new authenticators
     * to be created for new requests
     * 
     * @return The negotiate handler, or <code>null</code> for the default behaviour
     */
    
    HTTPNegotiateAuthentication createNegotiateAuthentication(HTTPNegotiateAuthentication prev, int requestID);
}
