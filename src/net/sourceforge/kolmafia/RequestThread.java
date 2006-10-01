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
import java.util.Vector;

public class RequestThread extends Thread implements KoLConstants
{
	private static final Vector runningRequests = new Vector();

	private int repeatCount;
	private Runnable [] requests;

	public RequestThread( Runnable request )
	{	this( new Runnable [] { request }, 1 );
	}

	public RequestThread( Runnable request, int repeatCount )
	{	this( new Runnable [] { request }, repeatCount );
	}

	public RequestThread( Runnable [] requests )
	{	this( requests, 1 );
	}

	public RequestThread( Runnable [] requests, int repeatCount )
	{
		this.repeatCount = repeatCount;
		this.requests = requests;
	}

	public void run()
	{
		if ( requests.length == 0 )
			return;

		if ( !runningRequests.isEmpty() )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "World peace declaration is still propogating." );
			return;
		}

		KoLmafia.updateDisplay( "Beginning a series of external requests..." );

		runningRequests.add( this );
		KoLmafia.forceContinue();

		for ( int i = 0; i < requests.length && KoLmafia.permitsContinue(); ++i )
		{
			if ( requests[i] == null )
			{
				// If it's null, then there's nothing that
				// can be done, so skip it.
			}
			else if ( requests[i] instanceof KoLRequest )
			{
				// Setting it up so that derived classes can
				// override the behavior of execution.

				run( (KoLRequest) requests[i], repeatCount );
			}
			else if ( requests[i] instanceof KoLAdventure )
			{
				// Standard KoL adventures are handled through the
				// client.makeRequest() method.

				StaticEntity.getClient().makeRequest( requests[i], repeatCount );
			}
			else
			{
				// All other runnables are run, as expected, with
				// no updates to the client.

				for ( int j = 0; j < repeatCount; ++j )
					requests[i].run();
			}
		}

		runningRequests.remove( this );
		KoLmafia.enableDisplay();

		if ( runningRequests.isEmpty() )
			SystemTrayFrame.showBalloon( "Requests complete." );
	}

	protected void run( KoLRequest request, int repeatCount )
	{	StaticEntity.getClient().makeRequest( request, repeatCount );
	}
}
