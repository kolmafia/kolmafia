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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.webui.CharacterEntityReference;

public class KoLDatabase
	extends StaticEntity
{
	public static final BufferedReader getReader( final String filename )
	{
		return DataUtilities.getReader( filename );
	}

	public static final BufferedReader getReader( final File file )
	{
		return DataUtilities.getReader( file );
	}

	public static final BufferedReader getReader( final InputStream istream )
	{
		return DataUtilities.getReader( istream );
	}

	public static final BufferedReader getVersionedReader( final String filename, final int version )
	{
		BufferedReader reader = DataUtilities.getReader( UtilityConstants.DATA_DIRECTORY, filename, true );

		// If no file, no reader
		if ( reader == null )
		{
			return null;
		}

		// Read the version number
		String line = KoLDatabase.readLine( reader );

		// Parse the version number and validate
		int fileVersion = StaticEntity.parseInt( line );

		if ( version == fileVersion )
		{
			return reader;
		}

		// We don't understand this file format
		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Override file is wrong version. Get built-in file

		return DataUtilities.getReader( UtilityConstants.DATA_DIRECTORY, filename, false );
	}

	public static final String readLine( final BufferedReader reader )
	{
		if ( reader == null )
		{
			return null;
		}

		try
		{
			String line;

			// Read in all of the comment lines, or until
			// the end of file, whichever comes first.

			while ( ( line = reader.readLine() ) != null && ( line.startsWith( "#" ) || line.length() == 0 ) )
			{
				;
			}

			// If you've reached the end of file, then
			// return null.  Otherwise, return the line
			// that's been split on tabs.

			return line;
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	public static final String[] readData( final BufferedReader reader )
	{
		if ( reader == null )
		{
			return null;
		}

		String line = KoLDatabase.readLine( reader );
		return line == null ? null : line.split( "\t" );
	}

	/**
	 * Returns the canonicalized name, where all symbols are replaced with their HTML representations.
	 *
	 * @param name The name to be canonicalized
	 * @return The canonicalized name
	 */

	public static final String getCanonicalName( final String name )
	{
		return name == null ? null : StaticEntity.globalStringReplace( CharacterEntityReference.escape(
			CharacterEntityReference.unescape( name ) ).toLowerCase(), "  ", " " );
	}

	/**
	 * Returns the display name name, where all HTML representations are replaced with their appropriate display
	 * symbols.
	 *
	 * @param name The name to be transformed to display form
	 * @return The display form of the given name
	 */

	public static final String getDisplayName( final String name )
	{
		return name == null ? null : CharacterEntityReference.unescape( name );
	}

	/**
	 * Returns a list of all elements which contain the given substring in their name.
	 *
	 * @param nameMap The map in which to search for the string
	 * @param substring The substring for which to search
	 */

	public static final List getMatchingNames( final Map nameMap, final String substring )
	{
		List substringList = new ArrayList();
		String searchString =
			KoLDatabase.getCanonicalName(
				substring.startsWith( "\"" ) ? substring.substring( 1, substring.length() - 1 ) : substring ).trim();

		if ( substring.length() == 0 )
		{
			return substringList;
		}

		if ( substring.startsWith( "\"" ) )
		{
			if ( nameMap.containsKey( searchString ) )
			{
				substringList.add( searchString );
			}

			return substringList;
		}

		if ( nameMap.containsKey( searchString ) )
		{
			substringList.add( searchString );
			return substringList;
		}

		String[] names = new String[ nameMap.size() ];
		nameMap.keySet().toArray( names );

		for ( int i = 0; i < names.length; ++i )
		{
			if ( KoLDatabase.substringMatches( names[ i ], searchString ) )
			{
				substringList.add( names[ i ] );
			}
		}

		if ( !substringList.isEmpty() )
		{
			return substringList;
		}

		int spaceIndex = searchString.indexOf( " " );
		if ( spaceIndex != -1 )
		{
			String tempSearchString = StaticEntity.globalStringDelete( searchString, " " );

			for ( int i = 0; i < names.length; ++i )
			{
				if ( KoLDatabase.substringMatches( names[ i ], tempSearchString ) )
				{
					substringList.add( names[ i ] );
				}
			}

			if ( !substringList.isEmpty() )
			{
				return substringList;
			}
		}

		return substringList;
	}

	public static final boolean substringMatches( final String source, final String substring )
	{
		return KoLDatabase.substringMatches( source, substring, false );
	}

	public static final boolean substringMatches( final String source, final String substring, boolean checkBoundaries )
	{
		int index = source.indexOf( substring );
		if ( index == -1 )
		{
			return false;
		}

		if ( !checkBoundaries || index == 0 )
		{
			return true;
		}

		return !Character.isLetterOrDigit( source.charAt( index - 1 ) );
	}

	public static final boolean fuzzyMatches( final String sourceString, final String searchString )
	{
		if ( searchString == null || searchString.length() == 0 )
		{
			return true;
		}

		char sourceChar = '\0';
		int sourceIndex = -1;
		int maxSourceIndex = sourceString.length() - 1;

		char searchChar = Character.toLowerCase( searchString.charAt( 0 ) );

		// First, find the index of the first character
		// to begin the search (this allows searches to
		// start in the middle of the string.

		for ( int i = 0; i <= maxSourceIndex && sourceIndex == -1; ++i )
		{
			sourceChar = Character.toLowerCase( sourceString.charAt(i) );
			if ( searchChar == sourceChar )
			{
				sourceIndex = i;
			}
		}

		if ( sourceIndex == -1 )
		{
			return false;
		}

		++sourceIndex;
		int maxSearchIndex = searchString.length() - 1;

		boolean matchedSpace = false;

		// Next, search the rest of the string.  Make sure
		// that all characters are accounted for in the fuzzy
		// matching sequence.

		for ( int searchIndex = 1; searchIndex <= maxSearchIndex; ++searchIndex )
		{
			if ( sourceIndex > maxSourceIndex )
			{
				return false;
			}

			searchChar = Character.toLowerCase( searchString.charAt( searchIndex ) );
			sourceChar = Character.toLowerCase( sourceString.charAt( sourceIndex ) );

			if ( searchChar == sourceChar )
			{
				++sourceIndex;
				matchedSpace = Character.isWhitespace( searchChar );
				continue;
			}

			// If the last character matched was not whitespace,
			// we need to search for the next whitespace character
			// in the source string.

			if ( !matchedSpace )
			{
				// If the search character isn't a whitespace, we
				// shouldn't bother fuzzy matching.

				if ( !Character.isWhitespace( searchChar ) )
				{
					return false;
				}

				// Begin searching for the next non-whitespace
				// character in the source string -- this is
				// what we will match against next.

				do
				{
					++searchIndex;

					if ( searchIndex > maxSearchIndex )
					{
						return false;
					}

					searchChar = Character.toLowerCase( searchString.charAt( searchIndex ) );
				}
				while ( Character.isWhitespace( searchChar ) );

				// If the current character is not a space,
				// find the next space to begin searching.

				if ( sourceChar != ' ' )
				{
					sourceIndex = sourceString.indexOf( ' ', sourceIndex );

					if ( sourceIndex == -1 )
					{
						return false;
					}
				}
			}

			// Search for the next matching character after
			// the whitespace in the source string.

			do
			{
				if ( ++sourceIndex > maxSourceIndex )
				{
					return false;
				}

				sourceChar = Character.toLowerCase( sourceString.charAt( sourceIndex ) );
			}
			while ( searchChar != sourceChar );

			// Now that we know we've found the character,
			// increment the counter and continue.

			++sourceIndex;
		}

		return true;
	}

	private static class ItemCounter
		implements Comparable
	{
		private final int count;
		private final String name;

		public ItemCounter( final String name, final int count )
		{
			this.name = name;
			this.count = count;
		}

		public int compareTo( final Object o )
		{
			ItemCounter ic = (ItemCounter) o;

			if ( this.count != ic.count )
			{
				return ic.count - this.count;
			}

			return this.name.compareToIgnoreCase( ic.name );
		}

		public String toString()
		{
			return this.name + ": " + this.count;
		}
	}

	public static final String getBreakdown( final List items )
	{
		StringBuffer strbuf = new StringBuffer();
		strbuf.append( KoLConstants.LINE_BREAK );

		Object[] itemArray = new Object[ items.size() ];
		items.toArray( itemArray );

		int currentCount = 1;

		ArrayList itemList = new ArrayList();

		for ( int i = 1; i < itemArray.length; ++i )
		{
			if ( itemArray[ i - 1 ] == null )
			{
				continue;
			}

			if ( itemArray[ i ] != null && !itemArray[ i - 1 ].equals( itemArray[ i ] ) )
			{
				itemList.add( new ItemCounter( itemArray[ i - 1 ].toString(), currentCount ) );
				currentCount = 0;
			}

			++currentCount;
		}

		if ( itemArray[ itemArray.length - 1 ] != null )
		{
			itemList.add( new ItemCounter( itemArray[ itemArray.length - 1 ].toString(), currentCount ) );
		}

		strbuf.append( "<ul>" );
		Collections.sort( itemList );

		for ( int i = 0; i < itemList.size(); ++i )
		{
			strbuf.append( "<li><nobr>" + itemList.get( i ) + "</nobr></li>" );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</ul>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		return strbuf.toString();
	}

	/**
	 * Calculates the sum of all the integers in the given list. Note that the list must consist entirely of Integer
	 * objects.
	 */

	public static final long calculateTotal( final List values )
	{
		long total = 0;
		for ( int i = 0; i < values.size(); ++i )
		{
			if ( values.get( i ) != null )
			{
				total += ( (Integer) values.get( i ) ).intValue();
			}
		}

		return total;
	}

	/**
	 * Calculates the average of all the integers in the given list. Note that the list must consist entirely of Integer
	 * objects.
	 */

	public static final float calculateAverage( final List values )
	{
		return (float) KoLDatabase.calculateTotal( values ) / (float) values.size();
	}

	/**
	 * Internal class which functions exactly an array of boolean, except it uses "sets" and "gets" like a list. This
	 * could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	public static class BooleanArray
	{
		private final ArrayList internalList = new ArrayList();

		public boolean get( final int index )
		{
			return index < 0 || index >= this.internalList.size() ? false : ( (Boolean) this.internalList.get( index ) ).booleanValue();
		}

		public void set( final int index, final boolean value )
		{
			while ( index >= this.internalList.size() )
			{
				this.internalList.add( Boolean.FALSE );
			}

			this.internalList.set( index, value ? Boolean.TRUE : Boolean.FALSE );
		}
	}

	/**
	 * Internal class which functions exactly an array of integers, except it uses "sets" and "gets" like a list. This
	 * could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	public static class IntegerArray
	{
		private final ArrayList internalList = new ArrayList();

		public int get( final int index )
		{
			return index < 0 || index >= this.internalList.size() ? 0 : ( (Integer) this.internalList.get( index ) ).intValue();
		}

		public void set( final int index, final int value )
		{
			while ( index >= this.internalList.size() )
			{
				this.internalList.add( new Integer( 0 ) );
			}

			this.internalList.set( index, new Integer( value ) );
		}
	}

	/**
	 * Internal class which functions exactly an array of strings, except it uses "sets" and "gets" like a list. This
	 * could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	public static class StringArray
	{
		private final ArrayList internalList = new ArrayList();

		public String get( final int index )
		{
			return index < 0 || index >= this.internalList.size() ? "" : (String) this.internalList.get( index );
		}

		public void set( final int index, final String value )
		{
			while ( index >= this.internalList.size() )
			{
				this.internalList.add( "" );
			}

			this.internalList.set( index, value );
		}

		public void add( final String s )
		{
			this.internalList.add( s );
		}

		public void clear()
		{
			this.internalList.clear();
		}

		public String[] toArray()
		{
			String[] array = new String[ this.internalList.size() ];
			this.internalList.toArray( array );
			return array;
		}

		public int size()
		{
			return this.internalList.size();
		}
	}

	public static final LockableListModel createListModel( final Set entries )
	{
		LockableListModel model = new LockableListModel();

		Iterator it = entries.iterator();
		while ( it.hasNext() )
		{
			model.add( new LowerCaseEntry( (Entry) it.next() ) );
		}

		return model;
	}

	public static class LowerCaseEntry
		implements Entry
	{
		private final Entry original;
		private final Object key;
		private Object value;
		private String pairString, lowercase;

		public LowerCaseEntry( final Entry original )
		{
			this.original = original;
			this.key = original.getKey();
			this.value = original.getValue();
			this.pairString = this.value + " (" + this.key + ")";
			this.lowercase = this.value.toString().toLowerCase();
		}

		public boolean equals( final Object o )
		{
			if ( o instanceof LowerCaseEntry )
			{
				return this.original.equals( ( (LowerCaseEntry) o ).original );
			}

			return this.original.equals( o );
		}

		public Object getKey()
		{
			return this.key;
		}

		public Object getValue()
		{
			return this.value;
		}

		public String toString()
		{
			return this.pairString;
		}

		public int hashCode()
		{
			return this.original.hashCode();
		}

		public Object setValue( final Object newValue )
		{
			Object returnValue = this.original.setValue( newValue );

			this.value = newValue;
			this.pairString = this.value + " (" + this.key + ")";
			this.lowercase = this.value.toString().toLowerCase();

			return returnValue;
		}

		public String getLowerCase()
		{
			return this.lowercase;
		}
	}
}
