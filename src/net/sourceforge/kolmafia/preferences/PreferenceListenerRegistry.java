/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.preferences;

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

public class PreferenceListenerRegistry
{
	// The registry of listeners:
	private static final HashMap<String,ArrayList<WeakReference>> listenerMap = new HashMap<String,ArrayList<WeakReference>>();

	// Logging
	private static boolean logging = false;
	public static final void setLogging( final boolean logging )
	{
		PreferenceListenerRegistry.logging = logging;
	}

	// Deferring
	private static int deferring = 0;
	private static final HashSet<String> deferred = new HashSet<String>();

	public static void deferListeners( boolean deferring )
	{
		// If we are deferring, increment defer level
		if ( deferring )
		{
			PreferenceListenerRegistry.deferring += 1;
			return;
		}

		// If we are undeferring but are not deferred, do nothing
		if ( PreferenceListenerRegistry.deferring == 0 )
		{
			return;
		}

		// If we are undeferring and are still deferred, nothing more to do
		if ( --PreferenceListenerRegistry.deferring > 0 )
		{
			return;
		}

		// We were deferred but are no longer deferred. Fire at Will!

		boolean logit = PreferenceListenerRegistry.logging && RequestLogger.isDebugging();

		synchronized( PreferenceListenerRegistry.deferred )
		{
			Iterator<String> it = PreferenceListenerRegistry.deferred.iterator();
			while ( it.hasNext() )
			{
				String name = it.next();
				ArrayList<WeakReference> listenerList = PreferenceListenerRegistry.listenerMap.get( name );
				if ( logit )
				{
					int count = listenerList == null ? 0 : listenerList.size();
					RequestLogger.updateDebugLog( "Firing " + count + " listeners for \"" + name + "\"" );
				}
				PreferenceListenerRegistry.fireListeners( listenerList, null );
			}
			PreferenceListenerRegistry.deferred.clear();
		}

	}

	public static final void registerListener( final String name, final PreferenceListener listener )
	{
		ArrayList<WeakReference> listenerList = null;

		synchronized ( listenerMap )
		{
			listenerList = PreferenceListenerRegistry.listenerMap.get( name );

			if ( listenerList == null )
			{
				listenerList = new ArrayList<WeakReference>();
				PreferenceListenerRegistry.listenerMap.put( name, listenerList );
			}
		}

		WeakReference reference = new WeakReference( listener );

		synchronized ( listenerList )
		{
			listenerList.add( reference );
		}
	}

	public static final void firePreferenceChanged( final String name )
	{
		ArrayList<WeakReference> listenerList = null;

		synchronized ( listenerMap )
		{
			listenerList = PreferenceListenerRegistry.listenerMap.get( name );
		}

		if ( listenerList == null )
		{
			return;
		}

		if ( PreferenceListenerRegistry.deferring > 0 )
		{
			PreferenceListenerRegistry.deferred.add( name );
			return;
		}

		boolean logit = PreferenceListenerRegistry.logging && RequestLogger.isDebugging();
		if ( logit )
		{
			int count = listenerList.size();
			RequestLogger.updateDebugLog( "Firing " + count + " listeners for \"" + name + "\"" );
		}

		PreferenceListenerRegistry.fireListeners( listenerList, null );
	}

	public static final void fireAllPreferencesChanged()
	{
		if ( PreferenceListenerRegistry.deferring > 0 )
		{
			Set<String> keys = null;
			synchronized ( PreferenceListenerRegistry.listenerMap )
			{
				keys = PreferenceListenerRegistry.listenerMap.keySet();
			}
			PreferenceListenerRegistry.deferred.addAll( keys );
			return;
		}

		HashSet<ArrayList<WeakReference>> listeners = new HashSet<ArrayList<WeakReference>>();

		boolean logit = PreferenceListenerRegistry.logging && RequestLogger.isDebugging();
		if ( logit )
		{
			Set<Entry<String,ArrayList<WeakReference>>> entries = null;
			synchronized ( PreferenceListenerRegistry.listenerMap )
			{
				entries = PreferenceListenerRegistry.listenerMap.entrySet();
			}

			Iterator<Entry<String,ArrayList<WeakReference>>> i1 = entries.iterator();
			while ( i1.hasNext() )
			{
				Entry<String,ArrayList<WeakReference>> entry = i1.next();
				ArrayList<WeakReference> listenerList = entry.getValue();
				String name = entry.getKey();
				int count = listenerList == null ? 0 : listenerList.size();
				RequestLogger.updateDebugLog( "Firing " + count + " listeners for \"" + name + "\"" );
				listeners.add( listenerList );
			}
		}
		else
		{
			Collection<ArrayList<WeakReference>> values = null;
			synchronized ( PreferenceListenerRegistry.listenerMap )
			{
				values = PreferenceListenerRegistry.listenerMap.values();
			}
			listeners.addAll( values );
		}

		Iterator<ArrayList<WeakReference>> i2 = listeners.iterator();
		HashSet<PreferenceListener> notified = new HashSet<PreferenceListener>();

		while ( i2.hasNext() )
		{
			PreferenceListenerRegistry.fireListeners( i2.next(), notified );
		}
	}

	private static final void fireListeners( final ArrayList<WeakReference> listenerList, final HashSet<PreferenceListener> notified )
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

				PreferenceListener listener = (PreferenceListener) reference.get();

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
