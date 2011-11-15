package com.velocityreviews.forums;

import java.io.IOException;
import java.io.PrintStream;

import java.net.URL;

import sun.net.www.http.HttpClient;

import sun.net.www.protocol.http.HttpURLConnection;

/**
 * Need to override any function in HttpURLConnection that create a new HttpClient
 * and create a HttpTimeoutClient instead.  This source code was originally found at:
 * http://www.velocityreviews.com/forums/t130657-httpurlconnection-timeout-solution.html
 * It has been modified to have timeout be non-configurable for added encapsulation.
 */

public class HttpTimeoutURLConnection extends HttpURLConnection
{
	public HttpTimeoutURLConnection( URL location ) throws IOException
	{	super( location, HttpTimeoutHandler.getInstance() );
	}

	public void connect() throws IOException
	{
		if ( connected )
			return;

		if ( "http".equals( url.getProtocol() ) )
		{
			// For safety's sake, as reported by KLGroup.

			synchronized ( url )
			{
				http = HttpTimeoutClient.getInstance( url );
			}
		}
		else
		{
			if ( !(handler instanceof HttpTimeoutHandler) )
				throw new IOException( "Expected com.velocityreviews.HttpTimeoutConnection$HttpTimeoutHandler, got " + handler.getClass() );

			http = new HttpTimeoutClient( super.url, ((HttpTimeoutHandler)handler).getProxy(), ((HttpTimeoutHandler)handler).getProxyPort() );
		}

		ps = (PrintStream) http.getOutputStream();
		connected = true;
	}


	protected HttpClient getNewClient( URL url ) throws IOException
	{	return new HttpTimeoutClient( url, (String) null, -1 );
	}

	protected HttpClient getProxiedClient( URL url, String s, int i ) throws IOException
	{	return new HttpTimeoutClient( url, s, i );
	}
}
