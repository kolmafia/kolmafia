/**
 * Copyright (c) 2005-2012, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.utilities;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import net.sourceforge.kolmafia.StaticEntity;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NaiveSecureSocketLayer
{
	public static void initialize()
	{
		try
		{
			SSLContext sslContext = SSLContext.getInstance( "SSL" );
			sslContext.init( null, NaiveSecureSocketLayer.NAIVE_TRUST_MANAGERS, new SecureRandom() );

			HttpsURLConnection.setDefaultSSLSocketFactory( sslContext.getSocketFactory() );
			HttpsURLConnection.setDefaultHostnameVerifier( NaiveSecureSocketLayer.NAIVE_HOSTNAME_VERIFIER );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final TrustManager[] NAIVE_TRUST_MANAGERS = new TrustManager[]
	{
		new NaiveTrustManager()
	};

	private static class NaiveTrustManager
		implements X509TrustManager
	{
		public void checkClientTrusted( X509Certificate[] chain, String authType )
			throws CertificateException
		{
		}

		public void checkServerTrusted( X509Certificate[] chain, String authType )
			throws CertificateException
		{
		}

		public X509Certificate[] getAcceptedIssuers()
		{
			return null;
		}
	}

	private static final HostnameVerifier NAIVE_HOSTNAME_VERIFIER = new NaiveHostnameVerifier();

	private static class NaiveHostnameVerifier
		implements HostnameVerifier
	{
		public boolean verify( String hostname, SSLSession session )
		{
			return true;
		}
	}
}
