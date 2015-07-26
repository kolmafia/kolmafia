package org.tmatesoft.svn.core.internal.io.dav.http;

public interface INTLMEngine {

    public String generateType1Msg(String domain, String ws) throws NTLMEngineException;

    public String generateType3Msg(String userName, char[] password, String domain, String ws, String token) throws NTLMEngineException;
}
