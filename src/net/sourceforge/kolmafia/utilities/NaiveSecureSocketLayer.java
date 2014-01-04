/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NaiveSecureSocketLayer
{
	private static boolean UNINSTALL_ENABLED = false;
	private static SSLSocketFactory DEFAULT_SOCKET_FACTORY = null;
	private static HostnameVerifier DEFAULT_HOSTNAME_VERIFIER = null;

	private static boolean INSTALL_ENABLED = false;
	private static SSLSocketFactory NAIVE_SOCKET_FACTORY = null;
	private static HostnameVerifier NAIVE_HOSTNAME_VERIFIER = null;

	static
	{
		try
		{
			NaiveSecureSocketLayer.DEFAULT_SOCKET_FACTORY = HttpsURLConnection.getDefaultSSLSocketFactory();
			NaiveSecureSocketLayer.DEFAULT_HOSTNAME_VERIFIER = HttpsURLConnection.getDefaultHostnameVerifier();
			NaiveSecureSocketLayer.UNINSTALL_ENABLED =
				NaiveSecureSocketLayer.DEFAULT_SOCKET_FACTORY != null &&
				NaiveSecureSocketLayer.DEFAULT_HOSTNAME_VERIFIER != null;
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		try
		{
			TrustManager[] naiveTrustManagers = new TrustManager[]
			{
				new NaiveTrustManager()
			};

			SSLContext sslContext = SSLContext.getInstance( "SSL" );
			sslContext.init( null, naiveTrustManagers, new SecureRandom() );

			NaiveSecureSocketLayer.NAIVE_SOCKET_FACTORY = sslContext.getSocketFactory();
			NaiveSecureSocketLayer.NAIVE_HOSTNAME_VERIFIER = new NaiveHostnameVerifier();
			NaiveSecureSocketLayer.INSTALL_ENABLED =
				NaiveSecureSocketLayer.NAIVE_SOCKET_FACTORY != null &&
				NaiveSecureSocketLayer.NAIVE_HOSTNAME_VERIFIER != null;
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static void install()
	{
		if ( NaiveSecureSocketLayer.INSTALL_ENABLED )
		{
			RequestLogger.printLine( "Installing naive certificate validation..." );

			HttpsURLConnection.setDefaultSSLSocketFactory( NaiveSecureSocketLayer.NAIVE_SOCKET_FACTORY );
			HttpsURLConnection.setDefaultHostnameVerifier( NaiveSecureSocketLayer.NAIVE_HOSTNAME_VERIFIER );
		}
		else
		{
			RequestLogger.printLine( "Skipping naive certificate validation installation..." );
		}
	}

	public static void uninstall()
	{
		if ( NaiveSecureSocketLayer.UNINSTALL_ENABLED )
		{
			RequestLogger.printLine( "Installing default certificate validation..." );

			HttpsURLConnection.setDefaultSSLSocketFactory( NaiveSecureSocketLayer.DEFAULT_SOCKET_FACTORY );
			HttpsURLConnection.setDefaultHostnameVerifier( NaiveSecureSocketLayer.DEFAULT_HOSTNAME_VERIFIER );
		}
		else
		{
			RequestLogger.printLine( "Skipping default certificate validation installation..." );
		}
	}

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

	private static class NaiveHostnameVerifier
		implements HostnameVerifier
	{
		public boolean verify( String hostname, SSLSession session )
		{
			return true;
		}
	}
}
