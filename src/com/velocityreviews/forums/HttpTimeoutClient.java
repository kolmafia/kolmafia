package com.velocityreviews.forums;

import java.io.IOException;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.URL;

import sun.net.www.http.HttpClient;

/**
 * Need to override any function in HttpURLConnection that create a new HttpClient
 * and create a HttpTimeoutClient instead.  This source code was originally found at:
 * http://www.velocityreviews.com/forums/t130657-httpurlconnection-timeout-solution.html
 * It has been modified to have timeout be non-configurable for added encapsulation.
 */

public class HttpTimeoutClient extends HttpClient
{
	private static int currentTimeout = 24000;

	public HttpTimeoutClient( URL location ) throws IOException
	{	super( location, (String) null, -1 );
	}

	public HttpTimeoutClient( URL location, String proxy, int proxyPort ) throws IOException
	{	super( location, proxy, proxyPort );
	}

	public static HttpTimeoutClient getInstance( URL location ) throws IOException
	{
		HttpTimeoutClient client = (HttpTimeoutClient) kac.get( location );

		if ( client == null )
		{
			if ( System.getProperty( "http.proxyHost" ) != null && !System.getProperty( "http.proxyHost" ).equals( "" ) )
				client = new HttpTimeoutClient( location, System.getProperty( "http.proxyHost" ), Integer.parseInt( System.getProperty( "http.proxyPort" ) ) );
			else
				client = new HttpTimeoutClient( location ); // CTOR called openServer()
		}
		else
		{
			client.url = location;
		}

		return client;
	}

	public static void setHttpTimeout( int newTimeout )
	{	currentTimeout = newTimeout;
	}

	// Override doConnect in NetworkClient

	protected Socket doConnect( String s, int i ) throws IOException, UnknownHostException, SocketException
	{
		Socket socket = super.doConnect( s, i );

		// This is the important bit

		socket.setSoTimeout( currentTimeout );
		return socket;
	}
}
