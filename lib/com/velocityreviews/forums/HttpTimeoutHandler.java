package com.velocityreviews.forums;

import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;

import sun.net.www.protocol.http.Handler;

/**
 * Need to override any function in HttpURLConnection that create a new HttpClient
 * and create a HttpTimeoutClient instead.  This source code was originally found at:
 * http://www.velocityreviews.com/forums/t130657-httpurlconnection-timeout-solution.html
 * It has been modified to have timeout be non-configurable for added encapsulation.
 */

public class HttpTimeoutHandler extends Handler
{
	private static final HttpTimeoutHandler INSTANCE = new HttpTimeoutHandler();

	private HttpTimeoutHandler()
	{
	}

	public static final Handler getInstance()
	{	return INSTANCE;
	}

	protected URLConnection openConnection( URL u ) throws IOException
	{	return new HttpTimeoutURLConnection( u );
	}

	protected String getProxy()
	{	return proxy;
	}

	protected int getProxyPort()
	{	return proxyPort;
	}
}
