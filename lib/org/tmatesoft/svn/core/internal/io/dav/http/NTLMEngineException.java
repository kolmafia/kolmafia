package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.IOException;

/**
* Created by alex on 7/3/14.
*/
@SuppressWarnings("serial")
public class NTLMEngineException extends IOException {
    public NTLMEngineException(String message) {
        super(message);
    }

    public NTLMEngineException(String message, Exception cause) {
        super(message, cause);
    }
}
