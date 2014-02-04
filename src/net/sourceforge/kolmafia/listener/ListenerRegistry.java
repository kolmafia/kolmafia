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

package net.sourceforge.kolmafia.listener;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

public class ListenerRegistry
{
	// A registry of listeners:
	private final HashMap<Object,ArrayList<WeakReference>> listenerMap = new HashMap<Object,ArrayList<WeakReference>>();

	// Logging. For now, this applies to all types of listeners
	private static boolean logging = false;
	public final static void setLogging( final boolean logging )
	{
		ListenerRegistry.logging = logging;
	}

	// Deferring
	private final HashSet<Object> deferred = new HashSet<Object>();
	private int deferring = 0;

	public ListenerRegistry()
	{
	}

	public void deferListeners( boolean deferring )
	{
		// If we are deferring, increment defer level
		if ( deferring )
		{
			this.deferring += 1;
			return;
		}

		// If we are undeferring but are not deferred, do nothing
		if ( this.deferring == 0 )
		{
			return;
		}

		// If we are undeferring and are still deferred, nothing more to do
		if ( --this.deferring > 0 )
		{
			return;
		}

		// We were deferred but are no longer deferred. Fire at Will!

		boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();

		synchronized( this.deferred )
		{
			Iterator<Object> it = this.deferred.iterator();
			while ( it.hasNext() )
			{
				Object key = it.next();
				ArrayList<WeakReference> listenerList = this.listenerMap.get( key );
				if ( logit )
				{
					int count = listenerList == null ? 0 : listenerList.size();
					RequestLogger.updateDebugLog( "Firing " + count + " listeners for \"" + key + "\"" );
				}
				this.fireListeners( listenerList, null );
			}
			this.deferred.clear();
		}

	}

	public final void registerListener( final Object key, final Listener listener )
	{
		ArrayList<WeakReference> listenerList = null;

		synchronized ( this.listenerMap )
		{
			listenerList = this.listenerMap.get( key );

			if ( listenerList == null )
			{
				listenerList = new ArrayList<WeakReference>();
				this.listenerMap.put( key, listenerList );
			}
		}

		WeakReference reference = new WeakReference( listener );

		synchronized ( listenerList )
		{
			listenerList.add( reference );
		}
	}

	public final void fireListener( final Object key )
	{
		ArrayList<WeakReference> listenerList = null;

		synchronized ( this.listenerMap )
		{
			listenerList = this.listenerMap.get( key );
		}

		if ( listenerList == null )
		{
			return;
		}

		if ( this.deferring > 0 )
		{
			this.deferred.add( key );
			return;
		}

		boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();
		if ( logit )
		{
			int count = listenerList.size();
			RequestLogger.updateDebugLog( "Firing " + count + " listeners for \"" + key + "\"" );
		}

		this.fireListeners( listenerList, null );
	}

	public final void fireAllListeners()
	{
		if ( this.deferring > 0 )
		{
			Set<Object> keys = null;
			synchronized ( this.listenerMap )
			{
				keys = this.listenerMap.keySet();
			}
			this.deferred.addAll( keys );
			return;
		}

		HashSet<ArrayList<WeakReference>> listeners = new HashSet<ArrayList<WeakReference>>();

		boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();
		if ( logit )
		{
			Set<Entry<Object,ArrayList<WeakReference>>> entries = null;
			synchronized ( this.listenerMap )
			{
				entries = this.listenerMap.entrySet();
			}

			Iterator<Entry<Object,ArrayList<WeakReference>>> i1 = entries.iterator();
			while ( i1.hasNext() )
			{
				Entry<Object,ArrayList<WeakReference>> entry = i1.next();
				Object key = entry.getKey();
				ArrayList<WeakReference> listenerList = entry.getValue();
				int count = listenerList == null ? 0 : listenerList.size();
				RequestLogger.updateDebugLog( "Firing " + count + " listeners for \"" + key + "\"" );
				listeners.add( listenerList );
			}
		}
		else
		{
			Collection<ArrayList<WeakReference>> values = null;
			synchronized ( this.listenerMap )
			{
				values = this.listenerMap.values();
			}
			listeners.addAll( values );
		}

		Iterator<ArrayList<WeakReference>> i2 = listeners.iterator();
		HashSet<Listener> notified = new HashSet<Listener>();

		while ( i2.hasNext() )
		{
			this.fireListeners( i2.next(), notified );
		}
	}

	private final void fireListeners( final ArrayList<WeakReference> listenerList, final HashSet<Listener> notified )
	{
		if ( listenerList == null )
		{
			return;
		}

		synchronized ( listenerList )
		{
			Iterator<WeakReference> i = listenerList.iterator();

			while ( i.hasNext() )
			{
				WeakReference reference = i.next();

				Listener listener = (Listener) reference.get();

				if ( listener == null )
				{
					i.remove();
					continue;
				}

				if ( notified != null )
				{
					if ( notified.contains( listener ) )
					{
						continue;
					}

					notified.add( listener );
				}

				try
				{
					listener.update();
				}
				catch ( Exception e )
				{
					// Don't let a botched listener interfere with
					// the code that modified the preference.

					StaticEntity.printStackTrace( e );
				}
			}
		}
	}
}
