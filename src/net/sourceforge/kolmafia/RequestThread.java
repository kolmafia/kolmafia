/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.InternalMessage;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.ResponseTextParser;

import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

import net.sourceforge.kolmafia.utilities.PauseObject;

public abstract class RequestThread
{
	private static int nextRequestId = 0;
	private static Map<Integer,Thread> threadMap = new HashMap<Integer,Thread>();

	public static final void runInParallel( final Runnable action )
	{
		RequestThread.runInParallel( action, true );
	}

	public static final void runInParallel( final Runnable action, final boolean sequence )
	{
		// Later on, we'll make this more sophisticated and create
		// something similar to the worker thread pool used in the
		// relay browser.  For now, just spawn a new thread.

		new ThreadWrappedRunnable( action, sequence ).start();
	}

	public static final void postRequestAfterInitialization( final GenericRequest request )
	{
		RequestThread.runInParallel( new PostDelayedRequestRunnable( request ) );
	}

	private static class PostDelayedRequestRunnable
		implements Runnable
	{
		private final GenericRequest request;
		private final PauseObject pauser;

		public PostDelayedRequestRunnable( final GenericRequest request )
		{
			this.request = request;
			this.pauser = new PauseObject();
		}

		public void run()
		{
			while ( KoLmafia.isRefreshing() )
			{
				this.pauser.pause( 100 );
			}

			RequestThread.postRequest( this.request );
		}
	}

	public static final void executeMethodAfterInitialization( final Object object, final String method )
	{
		RequestThread.runInParallel( new ExecuteDelayedMethodRunnable( object, method ) );
	}

	private static class ExecuteDelayedMethodRunnable
		implements Runnable
	{
		private Class objectClass;
		private Object object;
		private final String methodName;
		private Method method;
		private final PauseObject pauser;

		public ExecuteDelayedMethodRunnable( final Object object, final String methodName )
		{
			if ( object instanceof Class )
			{
				this.objectClass = (Class) object;
				this.object = null;
			}
			else
			{
				this.objectClass = object.getClass();
				this.object = object;
			}

			this.methodName = methodName;
			try
			{
				Class[] parameters = new Class[ 0 ];
				this.method = this.objectClass.getMethod( methodName, parameters );
			}
			catch ( Exception e )
			{
				this.method = null;
				KoLmafia.updateDisplay(
					MafiaState.ERROR, "Could not invoke " + this.objectClass + "." + this.methodName );
			}

			this.pauser = new PauseObject();
		}

		public void run()
		{
			if ( this.method == null )
			{
				return;
			}

			while ( KoLmafia.isRefreshing() )
			{
				this.pauser.pause( 100 );
			}

			try
			{
				Object[] args = new Object[ 0 ];
				this.method.invoke( this.object, args );
			}
			catch ( Exception e )
			{
			}
		}
	}

	public static final void executeMethod( final Object object, final String method )
	{
		RequestThread.runInParallel( new ExecuteMethodRunnable( object, method ) );
	}

	private static class ExecuteMethodRunnable
		implements Runnable
	{
		private Class objectClass;
		private Object object;
		private final String methodName;
		private Method method;

		public ExecuteMethodRunnable( final Object object, final String methodName )
		{
			if ( object instanceof Class )
			{
				this.objectClass = (Class) object;
				this.object = null;
			}
			else
			{
				this.objectClass = object.getClass();
				this.object = object;
			}

			this.methodName = methodName;
			try
			{
				Class[] parameters = new Class[ 0 ];
				this.method = this.objectClass.getMethod( methodName, parameters );
			}
			catch ( Exception e )
			{
				this.method = null;
				KoLmafia.updateDisplay(
					MafiaState.ERROR, "Could not invoke " + this.objectClass + "." + this.methodName );
			}
		}

		public void run()
		{
			if ( this.method == null )
			{
				return;
			}

			try
			{
				Object[] args = new Object[ 0 ];
				this.method.invoke( this.object, args );
			}
			catch ( Exception e )
			{
			}
		}
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

		boolean force = RequestThread.threadMap.isEmpty() &&
			ResponseTextParser.hasResult( request.getURLString() );

		RequestThread.postRequest( force, request );
	}

	public static final void postRequest( final KoLAdventure request )
	{
		if ( request == null )
		{
			return;
		}

		boolean force = true;
		RequestThread.postRequest( force, request );
	}

	public static final void postRequest( final Runnable request )
	{
		if ( request == null )
		{
			return;
		}

		boolean force = RequestThread.threadMap.isEmpty();
		RequestThread.postRequest( force, request );
	}

	private static final void postRequest( final boolean force, final Runnable request )
	{
		Integer requestId = RequestThread.openRequestSequence( force );

		try
		{
			if ( Preferences.getBoolean( "debugFoxtrotRemoval" ) &&
				SwingUtilities.isEventDispatchThread() )
			{
				StaticEntity.printStackTrace( "Runnable in event dispatch thread" );
			}

			request.run();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		finally
		{
			RequestThread.closeRequestSequence( requestId );
		}
	}

	public static synchronized final void checkOpenRequestSequences( final boolean flush )
	{
		int openSequences = 0;
		Thread currentThread = Thread.currentThread();

		Iterator threadIterator = RequestThread.threadMap.values().iterator();

		while ( threadIterator.hasNext() )
		{
			Thread thread = (Thread) threadIterator.next();

			if ( thread != currentThread )
			{
				++openSequences;
			}
		}

		if ( flush )
		{
			RequestThread.threadMap.clear();

			KoLmafia.updateDisplay( openSequences + " request sequences will be ignored." );
			KoLmafia.enableDisplay();
		}
		else
		{
			KoLmafia.updateDisplay( openSequences + " open request sequences detected." );
		}

		StaticEntity.printThreadDump();
	}

	public static synchronized final boolean hasOpenRequestSequences()
	{
		return !RequestThread.threadMap.isEmpty();
	}

	public static synchronized final Integer openRequestSequence()
	{
		return RequestThread.openRequestSequence( RequestThread.threadMap.isEmpty() );
	}

	public static synchronized final Integer openRequestSequence( final boolean forceContinue )
	{
		if ( forceContinue )
		{
			KoLmafia.forceContinue();
		}

		int requestId = ++RequestThread.nextRequestId;
		Integer requestIdObj = IntegerPool.get( requestId );

		RequestThread.threadMap.put( requestIdObj, Thread.currentThread() );

		return requestIdObj;
	}

	public static synchronized final void closeRequestSequence( final Integer requestIdObj )
	{
		Thread thread = (Thread) RequestThread.threadMap.remove( requestIdObj );

		if ( thread == null || !RequestThread.threadMap.isEmpty() )
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
		KoLmafia.updateDisplay( MafiaState.ABORT, "KoLmafia declares world peace." );
		InternalMessage message = new InternalMessage( "KoLmafia declares world peace.", "red" );
		ChatManager.broadcastEvent( message );
	}

	private static class ThreadWrappedRunnable
		extends Thread
	{
		private final Runnable wrapped;
		private final boolean sequence;

		public ThreadWrappedRunnable( final Runnable wrapped, final boolean sequence )
		{
			this.wrapped = wrapped;
			this.sequence = sequence;
		}

		@Override
		public void run()
		{
			Integer requestId = null;

			try
			{
				if ( this.sequence )
				{
					requestId = RequestThread.openRequestSequence();
				}
				this.wrapped.run();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
			finally
			{
				if ( requestId != null )
				{
					RequestThread.closeRequestSequence( requestId );
				}
			}
		}
	}
}
