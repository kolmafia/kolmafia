package org.tmatesoft.svn.core.internal.io.dav.http;

import org.tmatesoft.svn.core.internal.util.SVNBase64;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class NTLMJCIFSEngine implements INTLMEngine {

    private static final int TYPE_1_FLAGS =
                    0x80000000 | // NtlmFlags.NTLMSSP_NEGOTIATE_56
                    0x20000000 | // NtlmFlags.NTLMSSP_NEGOTIATE_128
                    0x00080000 | // NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2
                    0x00008000 | // NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN
                    0x00000004;  // NtlmFlags.NTLMSSP_REQUEST_TARGET;

    public static boolean isAvailable() {
        try {
            final Class<?> clazz = Class.forName("jcifs.ntlmssp.NtlmFlags");
            return clazz != null;
        } catch (ClassNotFoundException e) {
            //
        }
        return false;
    }

    public String generateType1Msg(String domain, String ws) throws NTLMEngineException {
        try {
            final Class<?> type1MessageClass = Class.forName("jcifs.ntlmssp.Type1Message");
            final Constructor<?> constructor = type1MessageClass.getConstructor(Integer.TYPE, String.class, String.class);
            final Object type1MessageObject = constructor.newInstance(TYPE_1_FLAGS, domain, ws);
            final Method toByteArray = type1MessageClass.getMethod("toByteArray");
            final byte[] message = (byte[]) toByteArray.invoke(type1MessageObject);
            return SVNBase64.byteArrayToBase64(message);
        } catch (Exception e) {
            throw new NTLMEngineException(e.getMessage(), e);
        }
    }

    public String generateType3Msg(String userName, char[] password, String domain, String ws, String token) throws NTLMEngineException {
        final byte[] buffer = new byte[token.length()];
        final int length = SVNBase64.base64ToByteArray(new StringBuffer(token), buffer);
        final byte[] tokenBytes = new byte[length];
        System.arraycopy(buffer, 0, tokenBytes, 0, length);
        try {
            final Class<?> type2MessageClass = Class.forName("jcifs.ntlmssp.Type2Message");
            final Class<?> type3MessageClass = Class.forName("jcifs.ntlmssp.Type3Message");

            final Constructor<?> type2Constructor = type2MessageClass.getConstructor(byte[].class);
            final Constructor<?> type3Constructor = type3MessageClass.getConstructor(type2MessageClass,
                    String.class, String.class, String.class, String.class, Integer.TYPE);
            final Object type2MessageObject = type2Constructor.newInstance(tokenBytes);
            final Method getFlags = type2MessageClass.getMethod("getFlags");
            final int type2Flags = (Integer) getFlags.invoke(type2MessageObject);
            final int type3Flags =  type2Flags
                    & (~(0x00010000 /*NtlmFlags.NTLMSSP_TARGET_TYPE_DOMAIN*/ |
                         0x00020000 /*NtlmFlags.NTLMSSP_TARGET_TYPE_SERVER*/));
            final Object type3MessageObject = type3Constructor.newInstance(type2MessageObject,
                    new String(password), domain, userName, ws, type3Flags);

            final Method toByteArray = type3MessageClass.getMethod("toByteArray");
            final byte[] message = (byte[]) toByteArray.invoke(type3MessageObject);
            return SVNBase64.byteArrayToBase64(message);
        } catch (Exception e) {
            throw new NTLMEngineException(e.getMessage(), e);
        }
    }
}
