
package com.velocityreviews.forums;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import sun.net.www.http.HttpClient;
import sun.net.www.protocol.http.HttpURLConnection;

/**
 * Need to override any function in HttpURLConnection that create a new HttpClient and create a HttpTimeoutClient
 * instead. This source code was originally found at:
 * http://www.velocityreviews.com/forums/t130657-httpurlconnection-timeout-solution.html It has been modified to have
 * timeout be non-configurable for added encapsulation.
 */

public class HttpTimeoutURLConnection
	extends HttpURLConnection
{
	public HttpTimeoutURLConnection( final URL location )
		throws IOException
	{
		super( location, HttpTimeoutHandler.getInstance() );
	}

	public void connect()
		throws IOException
	{
		if ( this.connected )
		{
			return;
		}

		if ( "http".equals( this.url.getProtocol() ) )
		{
			synchronized ( this.url )
			{
				this.http = HttpTimeoutClient.getInstance( this.url );
			}
		}
		else
		{
			if ( !( this.handler instanceof HttpTimeoutHandler ) )
			{
				throw new IOException(
					"Expected com.velocityreviews.HttpTimeoutConnection$HttpTimeoutHandler, got " + this.handler.getClass() );
			}

			this.http =
				new HttpTimeoutClient(
					super.url, ( (HttpTimeoutHandler) this.handler ).getProxy(),
					( (HttpTimeoutHandler) this.handler ).getProxyPort() );
		}

		this.ps = (PrintStream) this.http.getOutputStream();
		this.connected = true;
	}

	protected HttpClient getNewClient( final URL url )
		throws IOException
	{
		return new HttpTimeoutClient( url, (String) null, -1 );
	}

	protected HttpClient getProxiedClient( final URL url, final String s, final int i )
		throws IOException
	{
		return new HttpTimeoutClient( url, s, i );
	}
}
