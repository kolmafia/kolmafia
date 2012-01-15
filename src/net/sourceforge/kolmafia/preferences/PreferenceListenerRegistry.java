/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JCheckBox;

import net.sourceforge.kolmafia.StaticEntity;

public class PreferenceListenerRegistry
{
	private static final HashMap listenerMap = new HashMap();

	public static final void registerListener( final String name, final PreferenceListener listener )
	{
		ArrayList list = (ArrayList) PreferenceListenerRegistry.listenerMap.get( name );

		if ( list == null )
		{
			list = new ArrayList();
			PreferenceListenerRegistry.listenerMap.put( name, list );
		}

		WeakReference reference = new WeakReference( listener );

		list.add( reference );
	}

	public static final void firePreferenceChanged( final String name )
	{
		ArrayList listenerList = (ArrayList) PreferenceListenerRegistry.listenerMap.get( name );
		fireListeners( listenerList, null );
	}

	public static final void fireAllPreferencesChanged()
	{
		try
		{
			HashSet notified = new HashSet();

			Iterator i = PreferenceListenerRegistry.listenerMap.values().iterator();

			while ( i.hasNext() )
			{
				fireListeners( (ArrayList) i.next(), notified );
			}
		}
		//Instead of requiring synchronous operation, optimistically handle concurrency
		catch ( ConcurrentModificationException e )
		{
			fireAllPreferencesChanged();
		}
	}

	private static final void fireListeners( final ArrayList listenerList, final HashSet notified )
	{
		if ( listenerList == null )
		{
			return;
		}

		Iterator i = listenerList.iterator();

		while ( i.hasNext() )
		{
			WeakReference reference = (WeakReference) i.next();

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
