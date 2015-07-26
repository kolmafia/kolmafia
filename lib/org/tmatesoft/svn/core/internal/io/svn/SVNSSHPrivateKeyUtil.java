package org.tmatesoft.svn.core.internal.io.svn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.trilead.ssh2.auth.AgentProxy;
import com.trilead.ssh2.crypto.PEMDecoder;

public class SVNSSHPrivateKeyUtil {
    
    private static final String TRILEAD_AGENT_PROXY_CLASS = "com.jcraft.jsch.agentproxy.TrileadAgentProxy";
    private static final String CONNECTOR_FACTORY_CLASS = "com.jcraft.jsch.agentproxy.ConnectorFactory";
    private static final String CONNECTOR_CLASS = "com.jcraft.jsch.agentproxy.Connector";

    public static AgentProxy createOptionalSSHAgentProxy() {
        try {
            final Class<?> connectorClass = Class.forName(CONNECTOR_CLASS);
            final Method connectorFactoryGetDefault = Class.forName(CONNECTOR_FACTORY_CLASS).getMethod("getDefault");
            final Method connectorFactoryCreateConnector = Class.forName(CONNECTOR_FACTORY_CLASS).getMethod("createConnector");
            
            final Object connectorFactory = connectorFactoryGetDefault.invoke(null);
            final Object connector = connectorFactoryCreateConnector.invoke(connectorFactory);
            
            final Class<?> proxyClass = Class.forName(TRILEAD_AGENT_PROXY_CLASS);
            return (AgentProxy) proxyClass.getConstructor(connectorClass).newInstance(connector);
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "Failed to load TrileadAgentProxy");            
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);            
        }
        return null;
    }


    public static char[] readPrivateKey(File privateKey) {
        if (privateKey == null || !privateKey.exists() || !privateKey.isFile() || !privateKey.canRead()) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "Can not read private key from '" + privateKey + "'");
            return null;
        }
        Reader reader = null;
        StringWriter buffer = new StringWriter();
        try {
            reader = new BufferedReader(new FileReader(privateKey));
            int ch;
            while(true) {
                ch = reader.read();
                if (ch < 0) {
                    break;
                }
                buffer.write(ch);
            }
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return null;
        } finally {
            SVNFileUtil.closeFile(reader);
        }
        return buffer.toString().toCharArray();
    }

    /**
     * @deprecated
     */
    public static boolean isValidPrivateKey(char[] privateKey, String passphrase) {
        return isValidPrivateKey(privateKey, passphrase != null ? passphrase.toCharArray() : null);
    }
    
    public static boolean isValidPrivateKey(char[] privateKey, char[] passphrase) {
        try {
            PEMDecoder.decode(privateKey, passphrase != null ? new String(passphrase) : null);
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return false;
        }        
        return true;
    }

}
