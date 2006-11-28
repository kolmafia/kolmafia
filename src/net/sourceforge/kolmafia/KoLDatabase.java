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

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.io.BufferedReader;

import net.java.dev.spellcast.utilities.DataUtilities;

public class KoLDatabase extends StaticEntity
{
	protected static BufferedReader getReader( String filename )
	{	return DataUtilities.getReader( filename );
	}

	protected static BufferedReader getReader( File file )
	{	return DataUtilities.getReader( file );
	}

	protected static BufferedReader getReader( InputStream istream )
	{	return DataUtilities.getReader( istream );
	}

	protected static String [] readData( BufferedReader reader )
	{
		if ( reader == null )
			return null;

		try
		{
			String line;

			// Read in all of the comment lines, or until
			// the end of file, whichever comes first.

			while ( (line = reader.readLine()) != null && (line.startsWith( "#" ) || line.length() == 0) );

			// If you've reached the end of file, then
			// return null.  Otherwise, return the line
			// that's been split on tabs.

			return line == null ? null : line.split( "\t" );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
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
	{
		if ( name == null )
			return null;

		// If the name already contains character entities, getEntities
		// will turn the "&" characters themselves into "&amp;" entities.

		// Therefore, convert the string to unicode before converting
		// it (back into) entities

		return RequestEditorKit.getEntities( RequestEditorKit.getUnicode( name ) ).toLowerCase();
	}

	/**
	 * Returns the display name name, where all HTML representations
	 * are replaced with their appropriate display symbols.
	 *
	 * @param	name	The name to be transformed to display form
	 * @return	The display form of the given name
	 */

	public static final String getDisplayName( String name )
	{	return name == null ? null : RequestEditorKit.getUnicode( name );
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
		String searchString = getCanonicalName( substring.startsWith( "\"" ) ? substring.substring( 1, substring.length() - 1 ) : substring ).trim();

		if ( substring.length() == 0 )
			return substringList;

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
			String [] names = new String[ nameMap.keySet().size() ];
			nameMap.keySet().toArray( names );

			for ( int i = 0; i < names.length; ++i )
				if ( names[i].toLowerCase().indexOf( searchString ) != -1 )
					substringList.add( names[i] );
		}

		if ( substringList.isEmpty() )
			substringList.addAll( getMatchingAbbreviations( nameMap, searchString ) );

		return substringList;
	}

	/**
	 * Returns a list of all elements which contain the given
	 * abbreviation in their name.
	 *
	 * @param	nameMap	The map in which to search for the string
	 * @param	substring	The substring for which to search
	 */

	private static final List getMatchingAbbreviations( Map nameMap, String substring )
	{
		List substringList = new ArrayList();

		if ( substring.length() == 0 )
			return substringList;

		String [] names = new String[ nameMap.keySet().size() ];
		nameMap.keySet().toArray( names );

		substring = substring.toLowerCase();

		for ( int i = 0; i < names.length; ++i )
			if ( fuzzyMatches( names[i], substring ) )
				substringList.add( names[i] );

		return substringList;
	}

	public static boolean fuzzyMatches( String source, String substring )
	{
		if ( substring == null || substring.length() == 0 )
			return true;

		int searchIndex = 0;
		int lowerIndex, upperIndex;

		for ( int j = 0; j < substring.length(); ++j )
		{
			lowerIndex = source.indexOf( Character.toLowerCase( substring.charAt(j) ), searchIndex );
			upperIndex = source.indexOf( Character.toUpperCase( substring.charAt(j) ), searchIndex );
			searchIndex = lowerIndex != -1 && upperIndex != -1 ? Math.min( lowerIndex, upperIndex ) : Math.max( lowerIndex, upperIndex );

			if ( searchIndex == -1 )
				return false;

			++searchIndex;
		}

		return true;
	}

	private static class ItemCounter implements Comparable
	{
		private int count;
		private String name;

		public ItemCounter( String name, int count )
		{
			this.name = name;
			this.count = count;
		}

		public int compareTo( Object o )
		{
			ItemCounter ic = (ItemCounter) o;

			if ( count != ic.count )
				return ic.count - count;

			return name.compareToIgnoreCase( ic.name );
		}

		public String toString()
		{	return name + ": " + count;
		}
	}

	public static String getBreakdown( List items )
	{
		StringBuffer strbuf = new StringBuffer();
		strbuf.append( LINE_BREAK );

		Object [] itemArray = new Object[ items.size() ];
		items.toArray( itemArray );

		int maximumCount = 0;
		int currentCount = 1;

		ArrayList itemList = new ArrayList();

		for ( int i = 1; i < itemArray.length; ++i )
		{
			if ( itemArray[ i - 1 ] == null )
				continue;

			if ( itemArray[i] != null && !itemArray[ i - 1 ].equals( itemArray[i] ) )
			{
				itemList.add( new ItemCounter( itemArray[ i - 1 ].toString(), currentCount ) );
				currentCount = 0;
			}

			++currentCount;
		}

		if ( itemArray[ itemArray.length - 1 ] != null )
			itemList.add( new ItemCounter( itemArray[ itemArray.length - 1 ].toString(), currentCount ) );

		strbuf.append( "<ul>" );
		Collections.sort( itemList );

		for ( int i = 0; i < itemList.size(); ++i )
		{
			strbuf.append( "<li><nobr>" + itemList.get(i) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</ul>" );
		strbuf.append( LINE_BREAK );

		return strbuf.toString();
	}

	/**
	 * Calculates the sum of all the integers in the given list.
	 * Note that the list must consist entirely of Integer objects.
	 */

	public static final long calculateTotal( List values )
	{
		long total = 0;
		String currentValue;

		for ( int i = 0; i < values.size(); ++i )
		{
			if ( values.get(i) != null )
				total += ((Integer)values.get(i)).intValue();
		}

		return total;
	}

	/**
	 * Calculates the average of all the integers in the given list.
	 * Note that the list must consist entirely of Integer objects.
	 */

	public static final float calculateAverage( List values )
	{	return (float)calculateTotal( values ) / (float)values.size();
	}

	/**
	 * Internal class which functions exactly an array of boolean,
	 * except it uses "sets" and "gets" like a list.  This could be
	 * done with generics (Java 1.5) but is done like this so that
	 * we get backwards compatibility.
	 */

	protected static class BooleanArray
	{
		private ArrayList internalList = new ArrayList();

		public boolean get( int index )
		{	return index < 0 || index >= internalList.size() ? false : ((Boolean)internalList.get( index )).booleanValue();
		}

		public void set( int index, boolean value )
		{
			while ( index >= internalList.size() )
				internalList.add( new Boolean( false ) );

			internalList.set( index, new Boolean( value ) );
		}
	}

	/**
	 * Internal class which functions exactly an array of integers,
	 * except it uses "sets" and "gets" like a list.  This could be
	 * done with generics (Java 1.5) but is done like this so that
	 * we get backwards compatibility.
	 */

	protected static class IntegerArray
	{
		private ArrayList internalList = new ArrayList();

		public int get( int index )
		{	return index < 0 || index >= internalList.size() ? 0 : ((Integer)internalList.get( index )).intValue();
		}

		public void set( int index, int value )
		{
			while ( index >= internalList.size() )
				internalList.add( new Integer(0) );

			internalList.set( index, new Integer( value ) );
		}
	}

	/**
	 * Internal class which functions exactly an array of strings,
	 * except it uses "sets" and "gets" like a list.  This could be
	 * done with generics (Java 1.5) but is done like this so that
	 * we get backwards compatibility.
	 */

	protected static class StringArray
	{
		private ArrayList internalList = new ArrayList();

		public String get( int index )
		{	return index < 0 || index >= internalList.size() ? null : (String) internalList.get( index );
		}

		public void set( int index, String value )
		{
			while ( index >= internalList.size() )
				internalList.add( "" );

			internalList.set( index, value );
		}

		public void add( String s )
		{	internalList.add( s );
		}

		public void clear()
		{	internalList.clear();
		}

		public String [] toArray()
		{
			String [] array = new String[ internalList.size() ];
			internalList.toArray( array );
			return array;
		}

		public int size()
		{	return internalList.size();
		}
	}
}
