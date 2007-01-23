/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import javax.swing.SwingUtilities;
import net.sourceforge.foxtrot.ConcurrentWorker;
import net.sourceforge.foxtrot.Job;
import net.sourceforge.foxtrot.Worker;

public abstract class RequestThread implements KoLConstants
{
	private static int sequenceCount = 0;

	/**
	 * Posts a single request one time without forcing concurrency.
	 * The display will be enabled if there is no sequence.
	 */

	public static void postRequest( Runnable request )
	{
		if ( request == null )
			return;

		try
		{
			executeRequest( request, false );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Posts a single request one time possibly forcing concurrency.
	 * The display will be enabled if there is no sequence.
	 */

	public static void postRequest( Runnable request, boolean forceConcurrency )
	{
		if ( request == null )
			return;

		try
		{
			executeRequest( request, forceConcurrency );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Executes a single queued request and also re-enables the display.
	 * This should be executed as a sub-component.
	 */

	private static void executeRequest( Runnable request, boolean forceConcurrency ) throws Exception
	{
		if ( sequenceCount == 0 && (!(request instanceof KoLRequest) || !((KoLRequest)request).isDelayExempt()) )
			KoLmafia.forceContinue();

		// If you're not in the event dispatch thread, you can run
		// without posting to a separate thread.

		if ( !SwingUtilities.isEventDispatchThread() )
			request.run();

		// Now you know you're in the event dispatch thread, either
		// post concurrently or post in the handle thread based on
		// the request type.

		else if ( forceConcurrency || (request.getClass() == KoLRequest.class || request.getClass() == ChatRequest.class) )
			ConcurrentWorker.post( request instanceof Job ? (Job) request : new Request( request ) );

		else
			Worker.post( request instanceof Job ? (Job) request : new Request( request ) );

		if ( request instanceof KoLRequest )
			enableDisplayIfSequenceComplete();
	}

	public static void openRequestSequence()
	{	++sequenceCount;
	}

	public static void closeRequestSequence()
	{
		if ( sequenceCount <= 0 )
			return;

		--sequenceCount;

		if ( enableDisplayIfSequenceComplete() )
			SystemTrayFrame.showBalloon( "Requests complete." );
	}

	public static boolean enableDisplayIfSequenceComplete()
	{
		if ( sequenceCount != 0 )
			return false;

		if ( KoLmafia.getLastMessage().endsWith( "..." ) )
			KoLmafia.updateDisplay( "Request completed." );

		if ( KoLmafia.permitsContinue() || KoLmafia.refusesContinue() )
			KoLmafia.enableDisplay();

		return true;
	}

	/**
	 * Declare world peace.  This causes all pending requests and queued
	 * commands to be cleared, along with all currently running requests
	 * to be notified that they should stop as soon as possible.
	 */

	public static void declareWorldPeace()
	{
		KoLmafia.updateDisplay( ABORT_STATE, "KoLmafia declares world peace." );
	}

	private static class Request extends Job
	{
		private Runnable request;

		public Request( Runnable request )
		{	this.request = request;
		}

		public void run()
		{
			if ( KoLmafia.refusesContinue() )
				return;

			request.run();
		}
	}
}
