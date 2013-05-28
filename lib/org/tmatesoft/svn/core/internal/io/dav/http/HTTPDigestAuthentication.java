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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class HTTPDigestAuthentication extends HTTPAuthentication {

    private static final char[] HEXADECIMAL = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
        'e', 'f'
    };

    private String myCnonce;
    private String myQop;
    private String myLastNonce;
    private int myNC;

    protected HTTPDigestAuthentication () {
        myNC = 0;
    }

    public void init() throws SVNException {
        String qop = getChallengeParameter("qop");
        String selectedQop = null;

        if (qop != null) {
            for(StringTokenizer tok = new StringTokenizer(qop,","); tok.hasMoreTokens();) {
                selectedQop = tok.nextToken().trim();
                if ("auth".equals(selectedQop)) {
                    break;
                }
            }
        }
        if (selectedQop != null && !"auth".equals(selectedQop)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Digest HTTP auth: ''(0}'' is not supported", selectedQop);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        myQop = selectedQop;
        myCnonce = createCnonce();
    }
    
    public String authenticate() throws SVNException {
        if (getUserName() == null || getPassword() == null) {
            return null;
        }
        
        String uname = getUserName();
        String nonce = getParameter("nonce");
        if (nonce == null || !nonce.equals(myLastNonce)) {
            myNC = 0;
        } 
        myNC++;
        myLastNonce = nonce;
        String digest = createDigest(uname, getPassword(), "US-ASCII");

        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String opaque = getParameter("opaque");
        String algorithm = getParameter("algorithm", "MD5");

        StringBuffer sb = new StringBuffer();

        sb.append("Digest ");

        sb.append("username=\"" + uname + "\"")
          .append(", realm=\"" + realm + "\"")
          .append(", nonce=\"" + nonce + "\"").append(", uri=\"" + uri + "\"")
          .append(", response=\"" + digest + "\"");
        
        String nc = formatNC(myNC);        
        if (myQop != null) {
            sb.append(", qop=\"" + myQop + "\"")
              .append(", nc="+ nc)
              .append(", cnonce=\"" + myCnonce + "\"");
        }
        if (algorithm != null) {
            sb.append(", algorithm=\"" + algorithm + "\"");
        }
        if (opaque != null) {
            sb.append(", opaque=\"" + opaque + "\"");
        }
        return sb.toString();
    }

    public String getAuthenticationScheme(){
        return "Digest";
    }

    private String createDigest(String uname, String pwd, String charset) throws SVNException {
        final String digAlg = "MD5";

        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String nonce = getParameter("nonce");
        String method = getParameter("method");
        String algorithm = getParameter("algorithm", "MD5");

        MessageDigest md5Helper;
        try {
            md5Helper = MessageDigest.getInstance(digAlg);
        } catch (Exception e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Unsupported algorithm in HTTP Digest authentication: ''{0}''", digAlg);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
            return null;
        }
        StringBuffer tmp = new StringBuffer(uname.length() + realm.length() + pwd.length() + 2);
        tmp.append(uname);
        tmp.append(':');
        tmp.append(realm);
        tmp.append(':');
        tmp.append(pwd);
        String a1 = tmp.toString();
        if ("MD5-sess".equals(algorithm)) {
            String tmp2=encode(md5Helper.digest(HTTPAuthentication.getBytes(a1, charset)));
            StringBuffer tmp3 = new StringBuffer(tmp2.length() + nonce.length() + myCnonce.length() + 2);
            tmp3.append(tmp2);
            tmp3.append(':');
            tmp3.append(nonce);
            tmp3.append(':');
            tmp3.append(myCnonce);
            a1 = tmp3.toString();
        }

        String md5a1 = encode(md5Helper.digest(HTTPAuthentication.getBytes(a1, charset)));
        String a2 = method + ":" + uri;
        String md5a2 = encode(md5Helper.digest(HTTPAuthentication.getASCIIBytes(a2)));

        StringBuffer tmp2;
        if (myQop == null) {
            tmp2 = new StringBuffer(md5a1.length() + nonce.length() + md5a2.length());
            tmp2.append(md5a1);
            tmp2.append(':');
            tmp2.append(nonce);
            tmp2.append(':');
            tmp2.append(md5a2);
        } else {
            String qopOption = "auth";
            String nc = formatNC(myNC);
            tmp2 = new StringBuffer(md5a1.length() + nonce.length()
                    + nc.length() + myCnonce.length() + qopOption.length() + md5a2.length() + 5);
            tmp2.append(md5a1);
            tmp2.append(':');
            tmp2.append(nonce);
            tmp2.append(':');
            tmp2.append(nc);
            tmp2.append(':');
            tmp2.append(myCnonce);
            tmp2.append(':');
            tmp2.append(qopOption);
            tmp2.append(':');
            tmp2.append(md5a2);
        }

        return encode(md5Helper.digest(HTTPAuthentication.getASCIIBytes(tmp2.toString())));
    }

    private String getParameter(String name) {
        return getParameter(name, null);
    }

    private String getParameter(String name, String defaultValue) {
        String value = getChallengeParameter(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private static String createCnonce() {
        String cnonce;
        final String digAlg = "MD5";
        MessageDigest md5Helper;

        try {
            md5Helper = MessageDigest.getInstance(digAlg);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        cnonce = Long.toString(System.currentTimeMillis());
        cnonce = encode(md5Helper.digest(HTTPAuthentication.getASCIIBytes(cnonce)));
        return cnonce;
    }

    private static String encode(byte[] binaryData) {
        if (binaryData.length != 16) {
            return null;
        }

        char[] buffer = new char[32];
        for (int i = 0; i < 16; i++) {
            int low = binaryData[i] & 0x0f;
            int high = (binaryData[i] & 0xf0) >> 4;
            buffer[i * 2] = HEXADECIMAL[high];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }

        return new String(buffer);
    }
    
    private static String formatNC(int nc) {
        String value = Integer.toHexString(nc);
        int count = 8 - value.length();
        for(int i = 0; i < count; i++) {
            value = '0' + value;
        }
        return value;
    }

}
