/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

public class RequestThread extends Thread implements KoLConstants
{
	private KoLmafia client;
	private int [] repeatCount;
	private Runnable [] requests;

	public RequestThread()
	{	this( (Runnable) null, 0 );
	}

	public RequestThread( Runnable request )
	{	this( request, 1 );
	}

	public RequestThread( Runnable request, int repeatCount )
	{
		if ( request == null )
		{
			this.requests = new Runnable[0];
			this.repeatCount = new int[0];
		}
		else
		{
			this.client = request instanceof KoLRequest ? ((KoLRequest)request).client : null;

			this.requests = new Runnable[1];
			this.requests[0] = request;
			this.repeatCount = new int[1];
			this.repeatCount[0] = repeatCount;
		}

		setDaemon( true );
	}

	public RequestThread( Runnable [] requests )
	{	this( requests, 1 );
	}

	public RequestThread( Runnable [] requests, int repeatCount )
	{
		int requestCount = 0;
		for ( int i = 0; i < requests.length; ++i )
			if ( requests[i] != null )
				++requestCount;

		this.requests = new Runnable[ requestCount ];
		this.repeatCount = new int[ requestCount ];

		requestCount = 0;
		this.client = null;

		for ( int i = 0; i < requests.length; ++i )
			if ( requests[i] != null )
			{
				if ( this.client == null )
					this.client = requests[i] instanceof KoLRequest ? ((KoLRequest)requests[i]).client : null;

				this.requests[ requestCount ] = requests[i];
				this.repeatCount[ requestCount++ ] = repeatCount;
			}

		setDaemon( true );
	}

	public RequestThread( Runnable [] requests, int [] repeatCount )
	{
		int requestCount = 0;
		for ( int i = 0; i < requests.length; ++i )
			if ( requests[i] != null )
				++requestCount;

		this.requests = new Runnable[ requestCount ];
		this.repeatCount = new int[ requestCount ];

		requestCount = 0;
		this.client = null;

		for ( int i = 0; i < requests.length; ++i )
			if ( requests[i] != null )
			{
				if ( this.client == null )
					this.client = requests[i] instanceof KoLRequest ? ((KoLRequest)requests[i]).client : null;

				this.requests[ requestCount ] = requests[i];
				this.repeatCount[ requestCount++ ] = repeatCount[i];
			}

		setDaemon( true );
	}

	public void run()
	{
		if ( this.client != null )
			client.resetContinueState();

		for ( int i = 0; i < requests.length; ++i )
		{
			// Chat requests are only run once, no matter what
			// the repeat count is.  This is also to avoid the
			// message prompts you get otherwise.

			if ( requests[i] instanceof ChatRequest )
				requests[i].run();

			// Standard KoL requests are handled through the
			// client.makeRequest() method.

			else if ( requests[i] instanceof KoLRequest && !((KoLRequest)requests[i]).client.inLoginState() )
				client.makeRequest( requests[i], repeatCount[i] );

			// Standard KoL adventures are handled through the
			// client.makeRequest() method.

			else if ( requests[i] instanceof KoLAdventure )
			{
				client = ((KoLAdventure)requests[i]).client;
				client.makeRequest( requests[i], repeatCount[i] );
			}

			// All other runnables are run, as expected, with
			// no updates to the client.

			else
				for ( int j = 0; j < repeatCount[i]; ++j )
					requests[i].run();
		}

		if ( requests[0] instanceof ItemCreationRequest && client.permitsContinue() )
		{
			ItemCreationRequest irequest = (ItemCreationRequest) requests[0];
			client.updateDisplay( NORMAL_STATE, "Successfully created " + irequest.getQuantityNeeded() + " " + irequest.getName() );
		}

		if ( client != null && !(requests[0] instanceof ChatRequest) && !BuffBotHome.isBuffBotActive() )
		{
			client.enableDisplay();
			client.resetContinueState();
		}
	}
}
