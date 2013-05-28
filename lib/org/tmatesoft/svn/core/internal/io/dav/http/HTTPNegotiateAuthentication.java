package org.tmatesoft.svn.core.internal.io.dav.http;

/**
 * Base class for negotiate authentication support.  May be extended by local implementations using native GSS
 * implementations or delegated credentials.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class HTTPNegotiateAuthentication extends HTTPAuthentication {

    protected HTTPNegotiateAuthentication() {
    }

    public String getAuthenticationScheme() {
        return "Negotiate";
    }

    protected String getServerPrincipalName() {
        return "HTTP@" + getChallengeParameter("host");
    }

    public abstract void respondTo(String challenge);
        
    public abstract boolean isStarted();
    
    public abstract boolean needsLogin();    
}
