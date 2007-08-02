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

import javax.swing.SwingUtilities;
import net.sourceforge.foxtrot.Job;
import net.sourceforge.foxtrot.Worker;

public abstract class RequestThread implements KoLConstants
{
	private static int sequenceCount = 0;
	private static final OneSecondDelay ONE_SECOND_DELAY = new OneSecondDelay();

	/**
	 * Posts a single request one time without forcing concurrency.
	 * The display will be enabled if there is no sequence.
	 */

	public static final void postRequest( KoLRequest request )
	{
		if ( request == null )
			return;

		try
		{
			executeRequest( request );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static final void postRequest( KoLAdventure request )
	{
		if ( request == null )
			return;

		KoLmafia.forceContinue();
		++sequenceCount;

		try
		{
			if ( !SwingUtilities.isEventDispatchThread() )
				request.run();
			else
				Worker.post( request );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		--sequenceCount;

		if ( enableDisplayIfSequenceComplete() )
			SystemTrayFrame.showBalloon( "Requests complete." );
	}

	/**
	 * Posts a single request one time possibly forcing concurrency.
	 * The display will be enabled if there is no sequence.
	 */

	public static final void executeRequest( KoLRequest request )
	{
		if ( request == null )
			return;

		if ( sequenceCount == 0 && !request.isDelayExempt() )
			KoLmafia.forceContinue();

		++sequenceCount;

		// If you're not in the event dispatch thread, you can run
		// without posting to a separate thread.

		try
		{
			if ( !SwingUtilities.isEventDispatchThread() )
				request.run();
			else
				Worker.post( request );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		--sequenceCount;
		enableDisplayIfSequenceComplete();
	}

	public static final void postRequest( Runnable request )
	{
		if ( sequenceCount == 0 )
			KoLmafia.forceContinue();

		++sequenceCount;

		// If you're not in the event dispatch thread, you can run
		// without posting to a separate thread.

		try
		{
			if ( !SwingUtilities.isEventDispatchThread() )
				request.run();
			else
				Worker.post( new RunnableWrapper( request ) );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		--sequenceCount;
		enableDisplayIfSequenceComplete();
	}

	public static final void openRequestSequence()
	{
		if ( sequenceCount == 0 )
			KoLmafia.forceContinue();

		++sequenceCount;
	}

	public static final void closeRequestSequence()
	{
		if ( sequenceCount <= 0 )
			return;

		--sequenceCount;
		enableDisplayIfSequenceComplete();
	}

	public static final boolean enableDisplayIfSequenceComplete()
	{
		if ( sequenceCount != 0 )
			return false;

		if ( KoLmafia.getLastMessage().endsWith( "..." ) )
		{
			KoLmafia.updateDisplay( "Requests complete." );
			RequestLogger.printLine();
		}

		if ( KoLmafia.permitsContinue() || KoLmafia.refusesContinue() )
			KoLmafia.enableDisplay();

		return true;
	}

	/**
	 * Declare world peace.  This causes all pending requests and queued
	 * commands to be cleared, along with all currently running requests
	 * to be notified that they should stop as soon as possible.
	 */

	public static final void declareWorldPeace()
	{
		KoLmafia.updateDisplay( ABORT_STATE, "KoLmafia declares world peace." );
	}

	public static final void waitOneSecond()
	{
		try
		{
			if ( SwingUtilities.isEventDispatchThread() )
				Worker.post( ONE_SECOND_DELAY );
			else
				ONE_SECOND_DELAY.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static class RunnableWrapper extends Job
	{
		private Runnable wrapped;

		public RunnableWrapper( Runnable wrapped )
		{	this.wrapped = wrapped;
		}

		public void run()
		{	this.wrapped.run();
		}
	}

	private static class OneSecondDelay extends Job
	{
		public void run()
		{
			if ( KoLmafia.refusesContinue() )
				return;

			KoLRequest.delay( 1000 );
		}
	}
}
