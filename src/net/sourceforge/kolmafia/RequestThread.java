/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.InternalMessage;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

public abstract class RequestThread
{
	private static int nextRequestId = 0;
	private static Map requestMap = new HashMap();
	private static Map threadMap = new HashMap();

	public static final void runInParallel( final Runnable action )
	{
		// Later on, we'll make this more sophisticated and create
		// something similar to the worker thread pool used in the
		// relay browser.  For now, just spawn a new thread.

		new ThreadWrappedRunnable( action ).start();
	}

	/**
	 * Posts a single request one time without forcing concurrency. The display will be enabled if there is no sequence.
	 */

	public static final void postRequest( final GenericRequest request )
	{
		if ( request == null )
		{
			return;
		}

		// Make sure there is a URL string in the request

		request.reconstructFields();

		int requestId = RequestThread.openRequestSequence( RequestThread.requestMap.isEmpty() && ResponseTextParser.hasResult( request.getURLString() ) );

		try
		{
			if ( Preferences.getBoolean( "debugFoxtrotRemoval" ) && SwingUtilities.isEventDispatchThread() )
			{
				StaticEntity.printStackTrace( "Runnable in event dispatch thread" );
			}

			request.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		RequestThread.closeRequestSequence( requestId );
	}

	public static final void postRequest( final KoLAdventure request )
	{
		if ( request == null )
		{
			return;
		}

		int requestId = RequestThread.openRequestSequence( true );

		try
		{
			if ( Preferences.getBoolean( "debugFoxtrotRemoval" ) && SwingUtilities.isEventDispatchThread() )
			{
				StaticEntity.printStackTrace( "Runnable in event dispatch thread" );
			}

			request.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		RequestThread.closeRequestSequence( requestId );
	}

	public static final void postRequest( final Runnable request )
	{
		if ( request == null )
		{
			return;
		}

		int requestId = RequestThread.openRequestSequence();

		// If you're not in the event dispatch thread, you can run
		// without posting to a separate thread.

		try
		{
			if ( Preferences.getBoolean( "debugFoxtrotRemoval" ) && SwingUtilities.isEventDispatchThread() )
			{
				StaticEntity.printStackTrace( "Runnable in event dispatch thread" );
			}

			request.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		RequestThread.closeRequestSequence( requestId );
	}

	public static synchronized final void checkOpenRequestSequences()
	{
		int openSequenceCount = 0;

		Set threads = threadMap.entrySet();
		Thread currentThread = Thread.currentThread();

		Iterator threadIterator = threads.iterator();

		while ( threadIterator.hasNext() )
		{
			Entry entry = (Entry) threadIterator.next();


			if ( entry.getValue() == currentThread )
			{
				continue;
			}

			Object requestId = entry.getKey();

			Exception e = (Exception) requestMap.get( requestId );

			StaticEntity.printStackTrace( e, "Open request sequence" );
		}

		KoLmafia.updateDisplay( openSequenceCount + " open request sequences" );
	}

	public static synchronized final boolean hasOpenRequestSequences()
	{
		return !threadMap.isEmpty();
	}

	public static synchronized final int openRequestSequence()
	{
		return RequestThread.openRequestSequence( RequestThread.requestMap.isEmpty() );
	}

	private static synchronized final int openRequestSequence( boolean forceContinue )
	{
		if ( forceContinue )
		{
			KoLmafia.forceContinue();
		}

		int requestId = ++RequestThread.nextRequestId;
		Integer requestIdObj = new Integer( requestId );

		RequestThread.requestMap.put( requestIdObj, new Exception( "request sequence " + requestId ) );
		RequestThread.threadMap.put( requestIdObj, Thread.currentThread() );

		return requestId;
	}

	public static synchronized final void closeRequestSequence( int requestId )
	{
		Integer requestIdObj = new Integer( requestId );

		RequestThread.requestMap.remove( requestIdObj );
		RequestThread.threadMap.remove( requestIdObj );

		if ( !RequestThread.requestMap.isEmpty() )
		{
			return;
		}

		if ( KoLmafia.getLastMessage().endsWith( "..." ) )
		{
			KoLmafia.updateDisplay( "Requests complete." );
			SystemTrayFrame.showBalloon( "Requests complete." );
			RequestLogger.printLine();
		}

		if ( KoLmafia.permitsContinue() || KoLmafia.refusesContinue() )
		{
			KoLmafia.enableDisplay();
		}
	}

	/**
	 * Declare world peace. This causes all pending requests and queued commands to be cleared, along with all currently
	 * running requests to be notified that they should stop as soon as possible.
	 */

	public static final void declareWorldPeace()
	{
		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "KoLmafia declares world peace." );
		InternalMessage message = new InternalMessage( "KoLmafia declares world peace.", "red" );
		ChatManager.broadcastEvent( message );
	}

	private static class ThreadWrappedRunnable
		extends Thread
	{
		private final Runnable wrapped;

		public ThreadWrappedRunnable( final Runnable wrapped )
		{
			this.wrapped = wrapped;
		}

		public void run()
		{
			int requestId = RequestThread.openRequestSequence();

			try
			{
				this.wrapped.run();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			RequestThread.closeRequestSequence( requestId );
		}
	}
}
