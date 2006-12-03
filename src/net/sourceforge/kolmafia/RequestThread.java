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

import javax.swing.SwingUtilities;
import net.sourceforge.foxtrot.Job;
import net.sourceforge.foxtrot.Worker;
import net.sourceforge.foxtrot.ConcurrentWorker;

import java.util.Vector;

public abstract class RequestThread implements Runnable, KoLConstants
{
	public static void postRequest( Runnable request )
	{	execute( request, 1 );
	}

	public static void postRequest( Runnable request, int repeatCount )
	{	execute( request, repeatCount );
	}

	public static void postConcurrent( Runnable request )
	{
		try
		{
			Request runner = new Request( request, 1 );

			if ( SwingUtilities.isEventDispatchThread() )
				ConcurrentWorker.post( runner );
			else
				runner.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void execute( Runnable request, int repeatCount )
	{
		if ( request == null )
			return;

		KoLmafia.forceContinue();
		Request runner = new Request( request, repeatCount );

		try
		{
			if ( SwingUtilities.isEventDispatchThread() )
				Worker.post( runner );
			else
				runner.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		KoLmafia.enableDisplay();

		if ( request instanceof KoLAdventure )
			SystemTrayFrame.showBalloon( "Requests complete." );
	}

	private static class Request extends Job
	{
		private Runnable runner;
		private int repeatCount;

		public Request( Runnable runner, int repeatCount )
		{
			this.runner = runner;
			this.repeatCount = repeatCount;
		}

		public Object run()
		{
			if ( (runner instanceof KoLRequest && !((KoLRequest)runner).isDelayExempt()) || runner instanceof KoLAdventure )
			{
				StaticEntity.getClient().makeRequest( runner, repeatCount );
				return null;
			}

			for ( int i = 0; i < repeatCount; ++i )
				runner.run();

			return null;
		}
	}
}
