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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.DataUtilities;

public class KoLDatabase implements KoLConstants
{
	public static KoLmafia client;

	static { System.setProperty( "SHARED_MODULE_DIRECTORY", "net/sourceforge/kolmafia/" ); };

	protected static BufferedReader getReader( String file )
	{	return DataUtilities.getReaderForSharedDataFile( file );
	}

	protected static String [] readData( BufferedReader reader )
	{
		try
		{
			String line;

			// Read in all of the comment lines, or until
			// the end of file, whichever comes first.

			while ( (line = reader.readLine()) != null && line.startsWith( "#" ) );

			// If you've reached the end of file, then
			// return null.  Otherwise, return the line
			// that's been split on tabs.

			return line == null ? null : line.split( "\t" );
		}
		catch ( Exception e )
		{
			// If an exception is caught while attempting
			// to retrieve the next tokenizer, return null.

			return null;
		}
	}

	/**
	 * Returns the canonicalized name, where all symbols are
	 * replaced with their HTML representations.
	 *
	 * @param	name	The name to be canonicalized
	 * @return	The canonicalized name
	 */

	public static final String getCanonicalName( String name )
	{	return name == null ? null : name.replaceAll( "ñ", "&ntilde;" ).replaceAll( "é", "&eacute;" ).replaceAll( " \\[[tT][mM]\\]", "&trade;" ).toLowerCase();
	}

	/**
	 * Returns the display name name, where all HTML representations
	 * are replaced with their appropriate display symbols.
	 *
	 * @param	name	The name to be transformed to display form
	 * @return	The display form of the given name
	 */

	public static final String getDisplayName( String name )
	{	return name == null ? null : name.replaceAll( "&ntilde;", "ñ" ).replaceAll( "&eacute;", "é" ).replaceAll( "&trade;", " [TM]" );
	}

	/**
	 * Returns a list of all elements which contain the given
	 * substring in their name.
	 *
	 * @param	nameMap	The map in which to search for the string
	 * @param	substring	The substring for which to search
	 */

	public static final List getMatchingNames( Map nameMap, String substring )
	{
		List substringList = new ArrayList();
		String searchString = getCanonicalName( substring.replaceAll( "\"", "" ) );

		if ( substring.indexOf( "\"" ) != -1 )
		{
			if ( nameMap.containsKey( searchString ) )
				substringList.add( searchString );
		}
		else if ( nameMap.containsKey( searchString ) )
		{
			substringList.add( searchString );
		}
		else
		{
			String currentName;

			Iterator nameIterator = nameMap.keySet().iterator();
			while ( nameIterator.hasNext() )
			{
				currentName = (String) nameIterator.next();
				if ( currentName.indexOf( searchString ) != -1 )
					substringList.add( currentName );
			}
		}

		return substringList;
	}

	protected static final void setProperty( String name, String value )
	{
		if ( client != null )
		{
			client.getSettings().setProperty( name, value );
			client.getSettings().saveSettings();
		}
	}

	protected static final String getProperty( String name )
	{	return client == null ? GLOBAL_SETTINGS.getProperty( name ) : client.getSettings().getProperty( name );
	}
}
