/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.util;

import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;


/**
 * @version 1.0
 * @author  TMate Software Ltd.
 */
public class SVNSSLUtil {
    
    public static StringBuffer getServerCertificatePrompt(X509Certificate cert, String realm, String hostName) {
        int failures = getServerCertificateFailures(cert, hostName);
        StringBuffer prompt = new StringBuffer();
        prompt.append("Error validating server certificate for '");
        prompt.append(realm);
        prompt.append("':\n");
        if ((failures & 8) != 0) {
            prompt.append(" - The certificate is not issued by a trusted authority. Use the\n" +
                          "   fingerprint to validate the certificate manually!\n");
        }
        if ((failures & 4) != 0) {
            prompt.append(" - The certificate hostname does not match.\n");
        }
        if ((failures & 2) != 0) {
            prompt.append(" - The certificate has expired.\n");
        }
        if ((failures & 1) != 0) {
            prompt.append(" - The certificate is not yet valid.\n");
        }
        getServerCertificateInfo(cert, prompt);
        return prompt;

    }
    
    private static String getFingerprint(X509Certificate cert) {
        try  {
           return getFingerprint(cert.getEncoded(), "SHA1");
        } catch (Exception e)  {
        } 
        return null;
    }

    public static String getFingerprint(byte[] key, String digestAlgorithm) {
        StringBuffer s = new StringBuffer();
        try  {
           MessageDigest md = MessageDigest.getInstance(digestAlgorithm != null ? digestAlgorithm : "SHA1");
           md.update(key);
           byte[] digest = md.digest();
           for (int i= 0; i < digest.length; i++)  {
              if (i != 0) {
                  s.append(':');
              }
              int b = digest[i] & 0xFF;
              String hex = Integer.toHexString(b);
              if (hex.length() == 1) {
                  s.append('0');
              }
              s.append(hex.toLowerCase());
           }
        } catch (Exception e)  {
        } 
        return s.toString();
    }

  private static void getServerCertificateInfo(X509Certificate cert, StringBuffer info) {
      info.append("Certificate information:");
      info.append('\n');
      info.append(" - Subject: ");
      info.append(cert.getSubjectDN().getName());
      info.append('\n');
      info.append(" - Valid: ");
      info.append("from " + cert.getNotBefore() + " until " + cert.getNotAfter());
      info.append('\n');
      info.append(" - Issuer: ");
      info.append(cert.getIssuerDN().getName());
      info.append('\n');
      info.append(" - Fingerprint: ");
      info.append(getFingerprint(cert));
  }

  public static int getServerCertificateFailures(X509Certificate cert, String realHostName) {
      int mask = 8;
      Date time = new Date(System.currentTimeMillis());
      if (time.before(cert.getNotBefore())) {
          mask |= 1;
      }
      if (time.after(cert.getNotAfter())) {
          mask |= 2;
      }
      String certHostName = cert.getSubjectDN().getName();
      int index = certHostName.indexOf("CN=");
      if (index >= 0) {
          index += 3;
          certHostName = certHostName.substring(index);
          if (certHostName.indexOf(' ') >= 0) {
              certHostName = certHostName.substring(0, certHostName.indexOf(' '));
          }
          if (certHostName.indexOf(',') >= 0) {
              certHostName = certHostName.substring(0, certHostName.indexOf(','));
          }
      }
      if (!realHostName.equals(certHostName)) {
          try {
              Collection altNames = cert.getSubjectAlternativeNames();
              if(altNames != null) {
                  for (Iterator names = altNames.iterator(); names.hasNext();) {
                      Object nameList = names.next();
                      if (nameList instanceof Collection && ((Collection) nameList).size() >= 2) {
                          Object[] name = ((Collection) nameList).toArray();
                          Object type = name[0];
                          Object host = name[1];
                          if (type instanceof Integer && host instanceof String) {
                              if (((Integer) type).intValue() == 2 && host.equals(realHostName)) {
                                  return mask;
                              }
                          }
                      }
                  }
              }
          } catch (CertificateParsingException e) {
          }
          mask |= 4;
      }
      return mask;
  }

    public static class CertificateNotTrustedException extends CertificateException {

        private static final long serialVersionUID = 4845L;

        public CertificateNotTrustedException() {
            super();
        }

        public CertificateNotTrustedException(String msg) {
            super(msg);
        }
    }
}
