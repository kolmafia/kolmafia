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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.listener.CharacterListener;

public class CharacterListenerRegistry
{
	// Listener-driven container items

	private static final List<CharacterListener> listenerList = new ArrayList<CharacterListener>();

	/**
	 * Adds a new <code>CharacterListener</code> to the list of listeners listening to this
	 * <code>KoLCharacter</code>.
	 *
	 * @param listener The listener to be added to the listener list
	 */

	public static final void addCharacterListener( final CharacterListener listener )
	{
		if ( listener != null && !CharacterListenerRegistry.listenerList.contains( listener ) )
		{
			CharacterListenerRegistry.listenerList.add( listener );
		}
	}

	/**
	 * Removes an existing <code>KoLCharacterListener</code> from the list of listeners listening to this
	 * <code>KoLCharacter</code>.
	 *
	 * @param listener The listener to be removed from the listener list
	 */

	public static final void removeCharacterListener( final CharacterListener listener )
	{
		if ( listener != null )
		{
			CharacterListenerRegistry.listenerList.remove( listener );
		}
	}

	public static final void updateStatus()
	{
		CharacterListener[] listenerArray = new CharacterListener[ CharacterListenerRegistry.listenerList.size() ];
		CharacterListenerRegistry.listenerList.toArray( listenerArray );

		for ( int i = 0; i < listenerArray.length; ++i )
		{
			listenerArray[ i ].update();
		}

	}
}
