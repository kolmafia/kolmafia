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

package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import java.util.LinkedList;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
/**
 * Maintains a most recently used list of scripts
 * @author Fronobulax
 */
public class ScriptMRUList
{
	private int maxMRU = 16;
	private final LinkedList mruList = new LinkedList();
	private boolean isInit = false;

	public ScriptMRUList()
	{
		isInit = false;
	}

	private void init()
	{
		maxMRU = Preferences.getInteger( "scriptMRULength" );
		if ( maxMRU > 0 )
		{
			// Load list from preference - use whatever is there
			String oldValues = Preferences.getString( "scriptMRUList" );
			if ( ( oldValues != null ) && ( !oldValues.equals( "" ) ) )
			{
				// First to last, delimited by semi-colon.  Split and insert.
				String items[] = oldValues.split( ";" );
				for ( int i = ( items.length - 1 ); i >= 0; i-- )
				{
					mruList.addFirst( items[i] );
				}
			}
			while ( mruList.size() > maxMRU )
			{
				mruList.removeLast();
			}
			isInit = true;
		}
	}

	public void addItem(String script)
	{
		// Initialize list, if needed
		if ( !isInit )
		{
			init();
		}
		if ( !isInit )
		{
			return;
		}
		// don't add empty or null names
		if ( ( script != null ) && ( !script.equals( "" ) ) )
		{
			// check for item as an existing file and bail if not
			File fn = new File( script );
			if ( !fn.exists() )
			{
				return;
			}
			// Strip off the path info
			String sscript = fn.getName();
			// delete item if it is currently in list
			// note - as implemented this is a case sensitive compare
			while ( mruList.contains( sscript ) )
			{
				mruList.remove( sscript );
			}
			// add this as the first
			mruList.addFirst( sscript );
			// delete excess
			while ( mruList.size() > maxMRU )
			{
				mruList.removeLast();
			}
			// save the new list as a preference
			Object mruArray [] = mruList.toArray();
			StringBuffer pref = new StringBuffer();
			pref.append( mruArray[0].toString() );
			int count = mruList.size();
			if ( count > 1 )
			{
				for ( int i = 1; i < count; i++ )
				{
					pref.append( ";" );
					pref.append( mruArray[i].toString() );
				}
			}
			// now save it
			Preferences.setString( KoLCharacter.getUserName(), "scriptMRUList", pref.toString() );
		}
	}
	
	public File[] listFiles()
	{
		if ( !isInit )
		{
			init();
		}
		int count = mruList.size();
		if ( count < 1 )
		{
			return new File[ 0 ];
		}
		File [] result = new File [count];
		Object mruArray [] = mruList.toArray();
		for (int i = 0; i < count; i++)
		{
			result[i] = new File( mruArray[i].toString() );
		}
		return result;
	}
}
