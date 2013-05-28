package org.tmatesoft.svn.core.internal.io.svn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.trilead.ssh2.crypto.PEMDecoder;

public class SVNSSHPrivateKeyUtil {

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
    
    public static boolean isValidPrivateKey(char[] privateKey, String passphrase) {
        try {
            PEMDecoder.decode(privateKey, passphrase);
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return false;
        }        
        return true;
    }

}
