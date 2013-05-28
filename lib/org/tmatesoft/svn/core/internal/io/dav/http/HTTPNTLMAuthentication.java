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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.internal.util.SVNFormatUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class HTTPNTLMAuthentication extends HTTPAuthentication {

    private static final String NTLM_CASE_CONVERTION_PROPERTY = "svnkit.http.ntlm.uppercase";
    private static final String OLD_NTLM_CASE_CONVERTION_PROPERTY = "javasvn.http.ntlm.uppercase";
    
    private static final String DEFAULT_CHARSET = "ASCII";
    private static final String PROTOCOL_NAME = "NTLMSSP";
    private static final int LM_RESPONSE_LENGTH = 24;
    private static final int UNINITIATED = 0;
    protected static final int TYPE1 = 1;
    protected static final int TYPE3 = 3;
    private static byte[] ourMagicBytes = {
        (byte) 0x4B, (byte) 0x47, (byte) 0x53, (byte) 0x21, 
        (byte) 0x40, (byte) 0x23, (byte) 0x24, (byte) 0x25
    };
    
    private static final long NEGOTIATE_UNICODE = 0x00000001L;  
    private static final long NEGOTIATE_OEM = 0x00000002L;  
    private static final long REQUEST_TARGET = 0x00000004L;  
    private static final long NEGOTIATE_SIGN = 0x00000010L;  
    private static final long NEGOTIATE_SEAL = 0x00000020L;  
    private static final long NEGOTIATE_DATAGRAM_STYLE = 0x00000040L;  
    private static final long NEGOTIATE_LAN_MANAGER_KEY = 0x00000080L;  
    private static final long NEGOTIATE_NETWARE = 0x00000100L;  
    private static final long NEGOTIATE_NTLM = 0x00000200L;  
    private static final long NEGOTIATE_DOMAIN_SUPPLIED = 0x00001000L;  
    private static final long NEGOTIATE_WORKSTATION_SUPPLIED = 0x00002000L;  
    private static final long NEGOTIATE_LOCAL_CALL = 0x00004000L;  
    private static final long NEGOTIATE_ALWAYS_SIGN = 0x00008000L;  
    private static final long TARGET_TYPE_DOMAIN = 0x00010000L;  
    private static final long TARGET_TYPE_SERVER = 0x00020000L;  
    private static final long TARGET_TYPE_SHARE = 0x00040000L;  
    private static final long NEGOTIATE_NTLM2_KEY = 0x00080000L;  
    private static final long REQUEST_INIT_RESPONSE = 0x00100000L;  
    private static final long REQUEST_ACCEPT_RESPONSE = 0x00200000L;  
    private static final long REQUEST_NON_NT_SESSION_KEY = 0x00400000L;  
    private static final long NEGOTIATE_TARGET_INFO = 0x00800000L;  
    private static final long NEGOTIATE_128 = 0x20000000L;  
    private static final long NEGOTIATE_KEY_EXCHANGE = 0x40000000L;  
    private static final long NEGOTIATE_56 = 0x80000000L;  
    
    private static Map<Long, String> ourFlags = new TreeMap<Long, String>();
    static {
        ourFlags.put(new Long(NEGOTIATE_UNICODE), "0x00000001 (Negotiate Unicode)");
        ourFlags.put(new Long(NEGOTIATE_OEM), "0x00000002 (Negotiate OEM)");
        ourFlags.put(new Long(REQUEST_TARGET), "0x00000004 (Request Target)");
        ourFlags.put(new Long(0x00000008L), "0x00000008 (Unknown)");
        ourFlags.put(new Long(NEGOTIATE_SIGN), "0x00000010 (Negotiate Sign)");
        ourFlags.put(new Long(NEGOTIATE_SEAL), "0x00000020 (Negotiate Seal)");
        ourFlags.put(new Long(NEGOTIATE_DATAGRAM_STYLE), "0x00000040 (Negotiate Datagram Style)");
        ourFlags.put(new Long(NEGOTIATE_LAN_MANAGER_KEY), "0x00000080 (Negotiate Lan Manager Key)");
        ourFlags.put(new Long(NEGOTIATE_NETWARE), "0x00000100 (Negotiate Netware)");
        ourFlags.put(new Long(NEGOTIATE_NTLM), "0x00000200 (Negotiate NTLM)");
        ourFlags.put(new Long(0x00000400L), "0x00000400 (Unknown)");
        ourFlags.put(new Long(0x00000800L), "0x00000800 (Unknown)");
        ourFlags.put(new Long(NEGOTIATE_DOMAIN_SUPPLIED), "0x00001000 (Negotiate Domain Supplied)");
        ourFlags.put(new Long(NEGOTIATE_WORKSTATION_SUPPLIED), "0x00002000 (Negotiate Workstation Supplied)");
        ourFlags.put(new Long(NEGOTIATE_LOCAL_CALL), "0x00004000 (Negotiate Local Call)");
        ourFlags.put(new Long(NEGOTIATE_ALWAYS_SIGN), "0x00008000 (Negotiate Always Sign)");
        ourFlags.put(new Long(TARGET_TYPE_DOMAIN), "0x00010000 (Target Type Domain)");
        ourFlags.put(new Long(TARGET_TYPE_SERVER), "0x00020000 (Target Type Server)");
        ourFlags.put(new Long(TARGET_TYPE_SHARE), "0x00040000 (Target Type Share)");
        ourFlags.put(new Long(NEGOTIATE_NTLM2_KEY), "0x00080000 (Negotiate NTLM2 Key)");
        ourFlags.put(new Long(REQUEST_INIT_RESPONSE), "0x00100000 (Request Init Response)");
        ourFlags.put(new Long(REQUEST_ACCEPT_RESPONSE), "0x00200000 (Request Accept Response)");
        ourFlags.put(new Long(REQUEST_NON_NT_SESSION_KEY), "0x00400000 (Request Non-NT Session Key)");
        ourFlags.put(new Long(NEGOTIATE_TARGET_INFO), "0x00800000 (Negotiate Target Info)");
        ourFlags.put(new Long(0x01000000L), "0x01000000 (Unknown)");
        ourFlags.put(new Long(0x02000000L), "0x02000000 (Unknown)");
        ourFlags.put(new Long(0x04000000L), "0x04000000 (Unknown)");
        ourFlags.put(new Long(0x08000000L), "0x08000000 (Unknown)");
        ourFlags.put(new Long(0x10000000L), "0x10000000 (Unknown)");
        ourFlags.put(new Long(NEGOTIATE_128), "0x20000000 (Negotiate 128)");
        ourFlags.put(new Long(NEGOTIATE_KEY_EXCHANGE), "0x40000000 (Negotiate Key Exchange)");
        ourFlags.put(new Long(NEGOTIATE_56), "0x80000000 (Negotiate 56)");
    }

    private static Map<Integer, String> ourTargetInfoTypes = new TreeMap<Integer, String>();
    static {
        ourTargetInfoTypes.put(new Integer(1), "Server Name");
        ourTargetInfoTypes.put(new Integer(2), "Domain Name");
        ourTargetInfoTypes.put(new Integer(3), "DNS Host Name");
        ourTargetInfoTypes.put(new Integer(4), "DNS Domain Name");
    }
    
    protected int myState;
    private String myCharset;
    private byte[] myResponse;
    private int myPosition; 
    private byte[] myNonce;
    private boolean myIsNegotiateLocalCall;
    
    protected HTTPNTLMAuthentication (String charset) {
        myState = UNINITIATED;
        myIsNegotiateLocalCall = false;
        myCharset = charset;
        if (myCharset == null) {
            myCharset = "US-ASCII";
        }
    }
    
    public void setType1State(){
        myState = TYPE1;
    }

    public void setType3State(){
        myState = TYPE3;
    }

    public boolean isInType3State(){
        return myState == TYPE3;
    }
    
    private void initResponse(int bufferSize){
        myResponse = new byte[bufferSize];
        myPosition = 0;
    }
    
    private void addByte(byte b){
        myResponse[myPosition++] = b;
    }

    private void addBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            myResponse[myPosition++] = bytes[i];
        }
    }

    private byte[] convertToShortValue(int num){
        byte[] val = new byte[2];
        val[0] = (byte)(num & 0xff);
        val[1] = (byte)((num >> 8) & 0xff);
        return val;
    }
    
    private String getResponse(){
        byte[] response;
        if (myResponse.length > myPosition) {
            response = new byte[myPosition];
            for (int i = 0; i < myPosition; i++) {
                response[i] = myResponse[i];
            }
        } else {
            response = myResponse;
        }
        
        return SVNBase64.byteArrayToBase64(response);
    }
    
    public void parseChallenge(String challenge) throws SVNException {
        if (challenge == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                    "NTLM HTTP auth: expected challenge");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        
        byte[] challengeBase64Bytes = HTTPAuthentication.getBytes(challenge, DEFAULT_CHARSET);
        byte[] resultBuffer = new byte[challengeBase64Bytes.length];
        int resultLength = 0;
        try {
            resultLength = SVNBase64.base64ToByteArray(new StringBuffer(new String(challengeBase64Bytes, myCharset)), 
                    resultBuffer);
        } catch (UnsupportedEncodingException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                    "NTLM HTTP auth: " + e.getMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        
        String proto;
        try {
            proto = new String(resultBuffer, 0, 7, myCharset);
        } catch (UnsupportedEncodingException e) {
            proto = new String(resultBuffer, 0, 7);
        }
        byte[] typeBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            typeBytes[i] = resultBuffer[8 + i];
        }
        long type = toLong(typeBytes); 
        
        if (!PROTOCOL_NAME.equalsIgnoreCase(proto)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                    "NTLM HTTP auth: incorrect signature ''(0}''", proto);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        } else if (type != 2) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                    "NTLM HTTP auth: expected type 2 message instead of ''(0, number, integer}''", 
                    new Long(type));
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        
        myNonce = new byte[8];
        for (int i = 0; i < 8; i++) {
            myNonce[i] = resultBuffer[i + 24];
        }
        
        byte[] flagBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            flagBytes[i] = resultBuffer[i + 20];
        }
        long flags = toLong(flagBytes);
        
        StringBuffer log = new StringBuffer();
        String base64DecodedMessage;
        try {
            base64DecodedMessage = new String(resultBuffer, 0, resultLength, myCharset);
        } catch (UnsupportedEncodingException e) {
            base64DecodedMessage = new String(resultBuffer, 0, resultLength);
        }
        log.append("NTLM auth message: " + base64DecodedMessage);
        log.append('\n');
        log.append("Length: " + base64DecodedMessage.length());
        log.append('\n');
        log.append("Signature: " + proto);
        log.append('\n');
        log.append("Type: " + type);
        log.append('\n');
        log.append("Flags: " + Long.toString(flags, 16));
        log.append('\n');
        for (Iterator<Long> flagsIter = ourFlags.keySet().iterator(); flagsIter.hasNext();) {
            final Long curFlag = flagsIter.next();
            if ((flags & curFlag.longValue()) != 0) {
                log.append(ourFlags.get(curFlag));
                log.append('\n');
            }
        }
        
        byte[] targetNameLengthBytes = new byte[2];
        for (int i = 0; i < 2; i++) {
            targetNameLengthBytes[i] = resultBuffer[12 + i];
        }
        int targetNameLength = toInt(targetNameLengthBytes);

        byte[] targetNameAllocatedBytes = new byte[2];
        for (int i = 0; i < 2; i++) {
            targetNameAllocatedBytes[i] = resultBuffer[14 + i];
        }
        int targetNameAllocated = toInt(targetNameAllocatedBytes);
        
        byte[] targetNameOffsetBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            targetNameOffsetBytes[i] = resultBuffer[16 + i];
        }
        long targetNameOffset = toLong(targetNameOffsetBytes); 
        
        if (targetNameLength > 0) {
            String targetName;
            try {
                targetName = new String(resultBuffer, (int)targetNameOffset, targetNameAllocated, myCharset);
            } catch (UnsupportedEncodingException e) {
                targetName = new String(resultBuffer, (int)targetNameOffset, targetNameAllocated);
            }
            log.append("Target Name: " + targetName);
            log.append('\n');
        }
        log.append("Challenge: ");
        for (int i = 0; i < myNonce.length; i++) {
            log.append(SVNFormatUtil.getHexNumberFromByte(myNonce[i]));
        }
        log.append('\n');
        
        //check for local call
        long contextH = -1;
        long contextL = -1;
        boolean containsContext = false;
        if (targetNameOffset != 32 && resultLength >= 40) {
            byte[] contextHBytes = new byte[4];
            byte[] contextLBytes = new byte[4];
            int i = 0;
            for (i = 0; i < 4; i++) {
                contextHBytes[i] = resultBuffer[i + 32];
            }
            for (;i < 8; i++) {
                contextLBytes[i - 4] = resultBuffer[i + 32];
            }

            contextH = toLong(contextHBytes);
            contextL = toLong(contextLBytes);
            if (contextL == 0) {
                containsContext = true;
                log.append("Context: ");
                log.append(Long.toString(contextH, 16) + " " + Long.toString(contextL, 16));
                log.append('\n');
            }
            
            if (contextH != 0 && (flags & NEGOTIATE_LOCAL_CALL) != 0) {
                myIsNegotiateLocalCall = true;
            } else {
                myIsNegotiateLocalCall = false;
            }
        } else {
            myIsNegotiateLocalCall = false;
        }
        
        if ((flags & NEGOTIATE_TARGET_INFO) != 0) {
            int tgtInfoSecurityBufferOffset = containsContext ? 40: 32;
            
            byte[] targetInfoLengthBytes = new byte[2];
            for (int i = 0; i < 2; i++) {
                targetInfoLengthBytes[i] = resultBuffer[tgtInfoSecurityBufferOffset + i];
            }
            int targetInfoLength = toInt(targetInfoLengthBytes);
            
            byte[] targetInfoAllocatedBytes = new byte[2];
            for (int i = 0; i < 2; i++) {
                targetInfoAllocatedBytes[i] = resultBuffer[tgtInfoSecurityBufferOffset + 2 + i];
            }
            int targetInfoAllocated = toInt(targetInfoAllocatedBytes);
            
            byte[] targetInfoOffsetBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                targetInfoOffsetBytes[i] = resultBuffer[tgtInfoSecurityBufferOffset + 4 + i];
            }
            long targetInfoOffset = toLong(targetInfoOffsetBytes);
            
            byte[] targetInfoTypeBytes = new byte[2];
            byte[] subblockLengthBytes = new byte[2];
            int read = 0;
            while (targetInfoLength > 0 && read <= targetInfoAllocated) {
                for (int i = 0; i < 2; i++) {
                    targetInfoTypeBytes[i] = resultBuffer[(int)targetInfoOffset + i];
                }
                read += 2;
                targetInfoOffset += 2;
                int targetInfoType = toInt(targetInfoTypeBytes);
                if (targetInfoType == 0){
                    break;
                }

                for (int i = 0; i < 2; i++) {
                    subblockLengthBytes[i] = resultBuffer[(int)targetInfoOffset + i];
                }
                read += 2;
                targetInfoOffset += 2;
                int subblockLength = toInt(subblockLengthBytes);

                String typeDescription = (String)ourTargetInfoTypes.get(new Integer(targetInfoType));
                if (typeDescription != null) {
                    String info;
                    try {
                        info = new String(resultBuffer, (int)targetInfoOffset, subblockLength, myCharset);
                    } catch (UnsupportedEncodingException e) {
                        info = new String(resultBuffer, (int)targetInfoOffset, subblockLength);
                    }
                    log.append(typeDescription + ": " + info);
                    log.append('\n');
                }
                read += subblockLength;
                targetInfoOffset += subblockLength;
            }
        }
        log.append('\n');
    }
    
    private static int toInt(byte[] num){
        int l = 0;
        for (int i = 0; i < 2 ; i++) {
            int b = num[i] & 0xff;
            b = b << i*8;
            l |=  b;
        }
        return l;
    }

    public String authenticate() throws SVNException {
        if (myState != TYPE1 && myState != TYPE3) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                    "Unsupported message type in HTTP NTLM authentication");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        
        final String username = getUserName();
        String domain = getDomain();
        if (domain == null) {
            domain = "";
        }
        
        String hostName = null;
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            hostName = localhost.getHostName();
        } catch (UnknownHostException uhe) {
            hostName = "";
        }
        
        if (isUpperCase()) {
            domain = domain.toUpperCase();
            hostName = hostName.toUpperCase();
        }
        
        byte[] protocol = HTTPAuthentication.getBytes(PROTOCOL_NAME, DEFAULT_CHARSET);
        byte[] domainBytes = HTTPAuthentication.getBytes(domain, DEFAULT_CHARSET);
        byte[] hostNameBytes = HTTPAuthentication.getBytes(hostName, DEFAULT_CHARSET);
        byte[] domLen = convertToShortValue(domainBytes.length);
        byte[] hostLen = convertToShortValue(hostNameBytes.length);
        StringBuffer sublog = new StringBuffer();
        sublog.append("Signature: " + PROTOCOL_NAME);
        sublog.append('\n');

        long flags = NEGOTIATE_OEM | REQUEST_TARGET | NEGOTIATE_NTLM | NEGOTIATE_LOCAL_CALL;
        if (domain.length() > 0) {
            flags |= NEGOTIATE_DOMAIN_SUPPLIED;
        }
        
        if (myState == TYPE1) {
            int responseLength = 32 + domainBytes.length + hostNameBytes.length;
            
            initResponse(responseLength);

            //NTLMSSP\0 signature (8 bytes long)
            addBytes(protocol);
            addByte((byte) 0);

            // Type1 - Negotiate (4 bytes long)
            addByte((byte) 1);
            addByte((byte) 0);
            addByte((byte) 0);
            addByte((byte) 0);
            sublog.append("Type: " + 1);
            sublog.append('\n');

            // Flags (4 bytes long): 'Negotiate OEM', 'Request Target', 
            // 'Negotiate NTLM', 'Negotiate Always Sign'
            addByte((byte)(flags & 0xff));
            addByte((byte)((flags >> 8) & 0xff));
            addByte((byte)((flags >> 16) & 0xff));
            addByte((byte)((flags >> 24) & 0xff));
            sublog.append("Flags: " + Long.toString(flags, 16));
            sublog.append('\n');
            
            for (Iterator<Long> flagsIter = ourFlags.keySet().iterator(); flagsIter.hasNext();) {
                final Long curFlag = flagsIter.next();
                if ((flags & curFlag.longValue()) != 0) {
                    sublog.append(ourFlags.get(curFlag));
                    sublog.append('\n');
                }
            }

            // Domain name length (2 bytes short)
            addBytes(domLen);
            // Allocated space for the domain name (2 bytes short)
            addBytes(domLen);

            // Domain name offset (4 bytes long)
            byte[] domainOffset = convertToShortValue(hostNameBytes.length + 32);
            addBytes(domainOffset);
            addByte((byte) 0);
            addByte((byte) 0);

            // Host name length (2 bytes short).
            addBytes(hostLen);
            // Allocated space for the host name (2 bytes short)
            addBytes(hostLen);

            // Host name offset (always 32, 4 bytes long).
            byte[] hostOffset = convertToShortValue(32);
            addBytes(hostOffset);
            addByte((byte) 0);
            addByte((byte) 0);

            // Host name 
            addBytes(hostNameBytes);
            if (hostName.length() > 0) {
                sublog.append("Host Name: " + hostName);
                sublog.append('\n');
            }

            // Domain name
            addBytes(domainBytes);
            if (domain.length() > 0) {
                sublog.append("Domain: " + domain);
                sublog.append('\n');
            }
        } else if (myState == TYPE3) {
            byte[] userBytes = username.getBytes();
            sublog.append("Type: " + 3);
            sublog.append('\n');
            sublog.append("Flags: " + Long.toString(flags, 16));
            sublog.append('\n');
            for (Iterator<Long> flagsIter = ourFlags.keySet().iterator(); flagsIter.hasNext();) {
                final Long curFlag = flagsIter.next();
                if ((flags & curFlag.longValue()) != 0) {
                    sublog.append(ourFlags.get(curFlag));
                    sublog.append('\n');
                }
            }

            if (!myIsNegotiateLocalCall) {
                int responseLength = 64 + LM_RESPONSE_LENGTH + domainBytes.length + hostNameBytes.length + userBytes.length;
                
                initResponse(responseLength);
    
                addBytes(protocol);
                addByte((byte) 0);
    
                //Type3
                addByte((byte) 3);
                addByte((byte) 0);
                addByte((byte) 0);
                addByte((byte) 0);
    
                byte[] lmResponseLength = convertToShortValue(24); 
                // LM Response Length 
                addBytes(lmResponseLength);
                // LM Response allocated space
                addBytes(lmResponseLength);
    
                // LM Response Offset
                addBytes(convertToShortValue(responseLength - 24));
                addByte((byte) 0);
                addByte((byte) 0);
    
                byte[] ntlmResponseLength = convertToShortValue(0); 
                // NTLM Response Length 
                addBytes(ntlmResponseLength);
                // NTLM Response allocated space
                addBytes(ntlmResponseLength);
    
                byte[] responseLengthShortBytes = convertToShortValue(responseLength); 
                // NTLM Response Offset
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);
    
                // Domain length
                addBytes(domLen);
                // Domain allocated space
                addBytes(domLen);
                
                // Domain Offset
                addBytes(convertToShortValue(64));
                addByte((byte) 0);
                addByte((byte) 0);
    
                byte[] usernameLength = convertToShortValue(userBytes.length); 
                // Username Length 
                addBytes(usernameLength);
                // Username allocated space
                addBytes(usernameLength);
    
                // User offset
                addBytes(convertToShortValue(64 + domainBytes.length));
                addByte((byte) 0);
                addByte((byte) 0);
    
                // Host name length
                addBytes(hostLen);
                // Host name allocated space
                addBytes(hostLen);
    
                // Host offset
                addBytes(convertToShortValue(64 + domainBytes.length + userBytes.length));
    
                for (int i = 0; i < 6; i++) {
                    addByte((byte) 0);
                }
    
                // Message length
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);
    
                // Flags
                addByte((byte)(flags & 0xff));
                addByte((byte)((flags >> 8) & 0xff));
                addByte((byte)((flags >> 16) & 0xff));
                addByte((byte)((flags >> 24) & 0xff));

                addBytes(domainBytes);
                if (domain.length() > 0) {
                    sublog.append("Domain: " + domain);
                    sublog.append('\n');
                }

                addBytes(userBytes);
                if (username.length() > 0) {
                    sublog.append("User Name: " + username);
                    sublog.append('\n');
                }

                addBytes(hostNameBytes);
                if (hostName.length() > 0) {
                    sublog.append("Host Name: " + hostName);
                    sublog.append('\n');
                }

                String password = getPassword();
                byte[] hash = hashPassword(password != null ? password : ""); 
                addBytes(hash);

                sublog.append("Hash: " + new String(hash));
                sublog.append('\n');
                
            } else {
                int responseLength = 64;
                byte[] responseLengthShortBytes = convertToShortValue(responseLength); 
                initResponse(responseLength);

                addBytes(protocol);
                addByte((byte) 0);

                //Type3
                addByte((byte) 3);
                addByte((byte) 0);
                addByte((byte) 0);
                addByte((byte) 0);

                // LM Response Length 
                addByte((byte)0);
                addByte((byte)0);
                // LM Response allocated space
                addByte((byte)0);
                addByte((byte)0);

                // LM Response Offset
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);

                // NTLM Response Length 
                addByte((byte)0);
                addByte((byte)0);
                // NTLM Response allocated space
                addByte((byte)0);
                addByte((byte)0);
                // NTLM Response Offset
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);

                // Domain length
                addByte((byte)0);
                addByte((byte)0);
                // Domain allocated space
                addByte((byte)0);
                addByte((byte)0);
                // Domain Offset
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);

                // Username Length 
                addByte((byte)0);
                addByte((byte)0);
                // Username allocated space
                addByte((byte)0);
                addByte((byte)0);
                // User offset
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);

                // Host name length
                addByte((byte)0);
                addByte((byte)0);
                // Host name allocated space
                addByte((byte)0);
                addByte((byte)0);
                // Host offset
                addBytes(responseLengthShortBytes);

                for (int i = 0; i < 6; i++) {
                    addByte((byte) 0);
                }

                // Message length
                addBytes(responseLengthShortBytes);
                addByte((byte) 0);
                addByte((byte) 0);

                // Flags
                addByte((byte)(flags & 0xff));
                addByte((byte)((flags >> 8) & 0xff));
                addByte((byte)((flags >> 16) & 0xff));
                addByte((byte)((flags >> 24) & 0xff));
            }
            setType1State();
        }

        StringBuffer log = new StringBuffer();
        String message = null;
        try {
            message = new String(myResponse, 0, myPosition, myCharset);
        } catch (UnsupportedEncodingException e) {
            message = new String(myResponse, 0, myPosition);
        }
        log.append("NTLM auth message: " + message);
        log.append('\n');
        log.append("Length: " + message.length());
        log.append('\n');
        log.append(sublog);

        return "NTLM " + getResponse();
    }

    public String getAuthenticationScheme(){
        return "NTLM";
    }
    
    public boolean isNative() {
        return false;
    }
    
    public String getUserName() {
        String login = getRawUserName();
        String userName = null;
        int slashInd = login != null ? login.indexOf('\\') : -1; 
        if (slashInd != -1) {
            int lastInd = slashInd + 1;
            while (lastInd < login.length() && login.charAt(lastInd) == '\\') {
                lastInd++;
            }
            userName = login.substring(lastInd);
        } else {
            userName = login == null ? System.getProperty("user.name") : login;
        }
        return userName;
    }

    public String getDomain() {
        String login = getRawUserName();
        String domain = null;
        int slashInd = login != null ? login.indexOf('\\') : -1; 
        if (slashInd != -1) {
            domain = login.substring(0, slashInd);
        } 
        return domain;
    }
    
    private long toLong(byte[] num){
        long l = 0;
        for (int i = 0; i < 4 ; i++) {
            long b = num[i] & 0xff;
            b = b << i*8;
            l |=  b;
        }
        return l;
    }

    private boolean isUpperCase() {
        String upperCase = System.getProperty(NTLM_CASE_CONVERTION_PROPERTY, System.getProperty(OLD_NTLM_CASE_CONVERTION_PROPERTY, "true"));
        return Boolean.valueOf(upperCase).booleanValue();
    }

    private byte[] hashPassword(String password) throws SVNException {
        byte[] passw = isUpperCase() ? password.toUpperCase().getBytes() : password.getBytes();
        byte[] lmPw1 = new byte[7];
        byte[] lmPw2 = new byte[7];

        int len = passw.length;
        if (len > 7) {
            len = 7;
        }

        int idx;
        for (idx = 0; idx < len; idx++) {
            lmPw1[idx] = passw[idx];
        }
        
        for (; idx < 7; idx++) {
            lmPw1[idx] = (byte) 0;
        }

        len = passw.length;
        if (len > 14) {
            len = 14;
        }
        for (idx = 7; idx < len; idx++) {
            lmPw2[idx - 7] = passw[idx];
        }
        for (; idx < 14; idx++) {
            lmPw2[idx - 7] = (byte) 0;
        }

        byte[] lmHpw1;
        lmHpw1 = encrypt(lmPw1, ourMagicBytes);

        byte[] lmHpw2 = encrypt(lmPw2, ourMagicBytes);

        byte[] lmHpw = new byte[21];
        for (int i = 0; i < lmHpw1.length; i++) {
            lmHpw[i] = lmHpw1[i];
        }
        for (int i = 0; i < lmHpw2.length; i++) {
            lmHpw[i + 8] = lmHpw2[i];
        }
        for (int i = 0; i < 5; i++) {
            lmHpw[i + 16] = (byte) 0;
        }

        // Create the responses.
        byte[] lmResp = new byte[24];
        calcResp(lmHpw, lmResp);

        return lmResp;
    }
    
    private void calcResp(byte[] keys, byte[] results) throws SVNException {
        byte[] keys1 = new byte[7];
        byte[] keys2 = new byte[7];
        byte[] keys3 = new byte[7];
        
        for (int i = 0; i < 7; i++) {
            keys1[i] = keys[i];
        }

        for (int i = 0; i < 7; i++) {
            keys2[i] = keys[i + 7];
        }

        for (int i = 0; i < 7; i++) {
            keys3[i] = keys[i + 14];
        }
        
        byte[] results1 = encrypt(keys1, myNonce);

        byte[] results2 = encrypt(keys2, myNonce);

        byte[] results3 = encrypt(keys3, myNonce);

        for (int i = 0; i < 8; i++) {
            results[i] = results1[i];
        }
        
        for (int i = 0; i < 8; i++) {
            results[i + 8] = results2[i];
        }
        
        for (int i = 0; i < 8; i++) {
            results[i + 16] = results3[i];
        }
    }
    
    private byte[] encrypt(byte[] key, byte[] bytes) throws SVNException {
        Cipher ecipher = getCipher(key);
        try {
            byte[] enc = ecipher.doFinal(bytes);
            return enc;
        } catch (IllegalBlockSizeException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Invalid block size for DES encryption: {0}", e.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        } catch (BadPaddingException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Data not padded correctly for DES encryption: {0}", e.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return null;
    }
    
    private Cipher getCipher(byte[] key) throws SVNException {
        try {
            final Cipher ecipher = Cipher.getInstance("DES/ECB/NoPadding");
            key = setupKey(key);
            ecipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
            return ecipher;
        } catch (NoSuchAlgorithmException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "DES encryption is not available: {0}", e.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        } catch (InvalidKeyException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Invalid key for DES encryption: {0}", e.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        } catch (NoSuchPaddingException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "NoPadding option for DES is not available: {0}", e.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return null;
    }
    
    private byte[] setupKey(byte[] key56) {
        byte[] key = new byte[8];
        key[0] = (byte) ((key56[0] >> 1) & 0xff);
        key[1] = (byte) ((((key56[0] & 0x01) << 6) 
            | (((key56[1] & 0xff) >> 2) & 0xff)) & 0xff);
        key[2] = (byte) ((((key56[1] & 0x03) << 5) 
            | (((key56[2] & 0xff) >> 3) & 0xff)) & 0xff);
        key[3] = (byte) ((((key56[2] & 0x07) << 4) 
            | (((key56[3] & 0xff) >> 4) & 0xff)) & 0xff);
        key[4] = (byte) ((((key56[3] & 0x0f) << 3) 
            | (((key56[4] & 0xff) >> 5) & 0xff)) & 0xff);
        key[5] = (byte) ((((key56[4] & 0x1f) << 2) 
            | (((key56[5] & 0xff) >> 6) & 0xff)) & 0xff);
        key[6] = (byte) ((((key56[5] & 0x3f) << 1) 
            | (((key56[6] & 0xff) >> 7) & 0xff)) & 0xff);
        key[7] = (byte) (key56[6] & 0x7f);
        
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (key[i] << 1);
        }
        return key;
    }

    public boolean allowPropmtForCredentials() {
        return true;
    }

}
