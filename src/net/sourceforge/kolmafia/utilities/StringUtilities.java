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

package net.sourceforge.kolmafia.utilities;

import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.WeakHashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

public class StringUtilities
{
	private static final HashMap<String, String> entityEncodeCache = new HashMap<String, String>();
	private static final HashMap<String, String> entityDecodeCache = new HashMap<String, String>();

	private static final HashMap<String, String> urlEncodeCache = new HashMap<String, String>();
	private static final HashMap<String, String> urlDecodeCache = new HashMap<String, String>();

	private static final HashMap<String, String> displayNameCache = new HashMap<String, String>();
	private static final HashMap<String, String> canonicalNameCache = new HashMap<String, String>();

	private static final HashMap<String, String> prepositionsMap = new HashMap<String, String>();
	private static final WeakHashMap<String[], int[]> hashCache = new WeakHashMap<String[], int[]>();

	private static final Pattern NONINTEGER_PATTERN = Pattern.compile( "[^0-9\\-]+" );

	private static final Pattern PREPOSITIONS_PATTERN =
		Pattern.compile( "\\b(?:about|above|across|after|against|along|among|around|at|before|behind|" + "below|beneath|beside|between|beyond|by|down|during|except|for|from|in|inside|" + "into|like|near|of|off|on|onto|out|outside|over|past|through|throughout|to|" + "under|up|upon|with|within|without)\\b" );

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss zzz" );
	static {
		DATE_FORMAT.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
	};

	public static synchronized final long parseDate( final String dateString )
	{
		if ( dateString != null )
		{
			try
			{
				return StringUtilities.DATE_FORMAT.parse( dateString ).getTime();
			}
			catch ( Exception e )
			{
			}
		}
		return 0;
	}

	public static final String formatDate( final long date )
	{
		return StringUtilities.formatDate( new Date( date ) );
	}

	public static synchronized final String formatDate( final Date date )
	{
		try
		{
			return StringUtilities.DATE_FORMAT.format( date );
		}
		catch ( Exception e )
		{
			return "";
		}
	}

	/**
	 * Returns the encoded-encoded version of the provided UTF-8 string.
	 */

	public static final String getEntityEncode( final String utf8String )
	{
		return StringUtilities.getEntityEncode( utf8String, true );
	}

	public static final String getEntityEncode( String utf8String, final boolean cache )
	{
		if ( utf8String == null )
		{
			return utf8String;
		}

		String entityString = null;

		if ( cache )
		{
			entityString = StringUtilities.entityEncodeCache.get( utf8String );
		}

		if ( entityString == null )
		{
			// If this string is a substring of a longer string, make sure
			// we aren't keeping a reference to the longer string.
			utf8String = new String( utf8String );

			if ( utf8String.indexOf( "&" ) == -1 || utf8String.indexOf( ";" ) == -1 )
			{
				entityString = CharacterEntities.escape( utf8String );
			}
			else
			{
				entityString = CharacterEntities.escape( CharacterEntities.unescape( utf8String ) );
			}

			// The following replacement makes the Hodgman journals (which have
			// a double space after the colon) unsearchable in the Mall.
			//entityString = StringUtilities.globalStringReplace( entityString, "  ", " " );

			if ( cache && utf8String.length() < 100 )
			{
				StringUtilities.entityEncodeCache.put( utf8String, entityString );
			}
		}

		return entityString;
	}

	/**
	 * Returns the UTF-8 version of the provided character entity string.
	 */

	public static final String getEntityDecode( final String entityString )
	{
		return StringUtilities.getEntityDecode( entityString, true );
	}

	public static final String getEntityDecode( String entityString, final boolean cache )
	{
		if ( entityString == null )
		{
			return entityString;
		}

		String utf8String = null;

		if ( cache )
		{
			utf8String = StringUtilities.entityDecodeCache.get( entityString );
		}

		if ( utf8String == null )
		{
			// If this string is a substring of a longer string, make sure
			// we aren't keeping a reference to the longer string.
			entityString = new String( entityString );

			utf8String = CharacterEntities.unescape( entityString );

			if ( cache && entityString.length() < 100 )
			{
				StringUtilities.entityDecodeCache.put( entityString, utf8String );
			}
		}

		return utf8String;
	}

	/**
	 * Returns the URL-encoded version of the provided URL string.
	 */

	public static final String getURLEncode( final String url )
	{
		if ( url == null )
		{
			return url;
		}

		String encodedURL = StringUtilities.urlEncodeCache.get( url );

		if ( encodedURL == null )
		{
			try
			{
				encodedURL = URLEncoder.encode( url, "UTF-8" );
			}
			catch ( UnsupportedEncodingException e )
			{
				encodedURL = url;
			}

			StringUtilities.urlEncodeCache.put( url, encodedURL );
		}

		return encodedURL;
	}

	/**
	 * Returns the URL-decoded version of the provided URL string.
	 */

	public static final String getURLDecode( final String url )
	{
		if ( url == null )
		{
			return url;
		}

		String encodedURL = StringUtilities.urlDecodeCache.get( url );

		if ( encodedURL == null )
		{
			try
			{
				encodedURL = URLDecoder.decode( url, "UTF-8" );
			}
			catch ( UnsupportedEncodingException e )
			{
				encodedURL = url;
			}

			StringUtilities.urlDecodeCache.put( url, encodedURL );
		}

		return encodedURL;
	}

	/**
	 * Returns the display name for the provided canonical name.
	 */

	public static final String getDisplayName( String name )
	{
		if ( name == null )
		{
			return name;
		}

		String displayName = StringUtilities.displayNameCache.get( name );

		if ( displayName == null )
		{
			// If this string is a substring of a longer string, make sure
			// we aren't keeping a reference to the longer string.
			name = new String( name );

			displayName = StringUtilities.getEntityDecode( name );
			StringUtilities.displayNameCache.put( name, displayName );
		}

		return displayName;
	}

	/**
	 * Returns the canonicalized name for the provided display name.
	 */

	public static final String getCanonicalName( String name )
	{
		if ( name == null )
		{
			return null;
		}

		String canonicalName = StringUtilities.canonicalNameCache.get( name );

		if ( canonicalName == null )
		{
			// If this string is a substring of a longer string, make sure
			// we aren't keeping a reference to the longer string.
			name = new String( name );

			canonicalName = StringUtilities.getEntityEncode( name ).toLowerCase();
			if ( name.length() < 100 )
			{
				StringUtilities.canonicalNameCache.put( name, canonicalName );
			}
		}

		return canonicalName;
	}

	/**
	 * Returns a list of all elements which contain the given substring in their name.
	 *
	 * @param nameMap The map in which to search for the string
	 * @param substring The substring for which to search
	 */

	public static final List<String> getMatchingNames( final String[] names, String searchString )
	{
		if ( searchString == null )
		{
			searchString = "";
		}

		searchString = searchString.trim();

		boolean isExactMatch = searchString.startsWith( "\"" );
		List<String> matchList = new ArrayList<String>();

		if ( isExactMatch )
		{
			String fullString = StringUtilities.getCanonicalName( searchString );
			if ( Arrays.binarySearch( names, fullString ) >= 0 )
			{
				matchList.add( fullString );
				return matchList;
			}

			int end = searchString.endsWith( "\"" ) ? searchString.length() - 1 : searchString.length();
			searchString = searchString.substring( 1, end );
		}

		searchString = StringUtilities.getCanonicalName( searchString );

		if ( searchString.length() == 0 )
		{
			return matchList;
		}

		if ( Arrays.binarySearch( names, searchString ) >= 0 )
		{
			matchList.add( searchString );
			return matchList;
		}

		if ( isExactMatch )
		{
			return matchList;
		}

		int nameCount = names.length;
		int[] hashes = StringUtilities.hashCache.get( names );
		if ( hashes == null )
		{
			hashes = new int[ nameCount ];
			for ( int i = 0; i < nameCount; ++i )
			{
				hashes[ i ] = StringUtilities.stringHash( names[ i ] );
			}
			StringUtilities.hashCache.put( names, hashes );
		}
		int hash = StringUtilities.stringHash( searchString );

		for ( int i = 0; i < nameCount; ++i )
		{
			if ( ( hashes[ i ] & hash ) == hash && StringUtilities.substringMatches( names[ i ], searchString, true ) )
			{
				matchList.add( names[ i ] );
			}
		}

		if ( !matchList.isEmpty() )
		{
			return matchList;
		}

		for ( int i = 0; i < nameCount; ++i )
		{
			if ( ( hashes[ i ] & hash ) == hash && StringUtilities.substringMatches( names[ i ], searchString, false ) )
			{
				matchList.add( names[ i ] );
			}
		}

		if ( !matchList.isEmpty() )
		{
			return matchList;
		}
		
		// There is an oddball special case here: a search string containing
		// spaces can successfully fuzzy-match an item name with no spaces, 
		// for example "in the box" will match "chef-in-the-box".  However,
		// the hash check would prevent us from even trying such a match.
		// Therefore, strip out the bit representing a space in the hash:
		hash &= ~StringUtilities.stringHash( " " );

		for ( int i = 0; i < nameCount; ++i )
		{
			if ( ( hashes[ i ] & hash ) == hash && StringUtilities.fuzzyMatches( names[ i ], searchString ) )
			{
				matchList.add( names[ i ] );
			}
		}

		return matchList;
	}

	private static final int stringHash( final String s )
	{
		int hash = 0;
		for ( int i = s.length() - 1; i >= 0; --i )
		{
			hash |= 1 << ( s.charAt( i ) & 0x1F );
		}
		return hash;
	}

	public static final boolean substringMatches( final String source, final String substring,
		final boolean checkBoundaries )
	{
		if ( source == null )
		{
			return false;
		}

		if ( substring == null || substring.length() == 0 )
		{
			return true;
		}

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
		if ( sourceString == null )
		{
			return false;
		}

		if ( searchString == null || searchString.length() == 0 )
		{
			return true;
		}

		return StringUtilities.fuzzyMatches( sourceString, searchString, -1, -1 );
	}

	private static final boolean fuzzyMatches( final String sourceString, final String searchString,
		final int lastSourceIndex, final int lastSearchIndex )
	{
		int maxSearchIndex = searchString.length() - 1;

		if ( lastSearchIndex == maxSearchIndex )
		{
			return true;
		}

		// Skip over any non alphanumeric characters in the search string
		// since they hold no meaning.

		char searchChar;
		int searchIndex = lastSearchIndex;

		do
		{
			if ( ++searchIndex > maxSearchIndex )
			{
				return true;
			}

			searchChar = searchString.charAt( searchIndex );
		}
		while ( Character.isWhitespace( searchChar ) );

		// If it matched the first character in the source string, the
		// character right after the last search, or the match is on a
		// word boundary, continue searching.

		int sourceIndex = sourceString.indexOf( searchChar, lastSourceIndex + 1 );

		while ( sourceIndex != -1 )
		{
			if ( sourceIndex == 0 || sourceIndex == lastSourceIndex + 1 || isWordBoundary( sourceString.charAt( sourceIndex - 1 ) ) )
			{
				if ( StringUtilities.fuzzyMatches( sourceString, searchString, sourceIndex, searchIndex ) )
				{
					return true;
				}
			}

			sourceIndex = sourceString.indexOf( searchChar, sourceIndex + 1 );
		}

		return false;
	}
	
	private static final boolean isWordBoundary( char ch )
	{
		return ch != '#' && !Character.isLetterOrDigit( ch );
	}

	public static final void insertBefore( final StringBuffer buffer, final String searchString,
		final String insertString )
	{
		int searchIndex = buffer.indexOf( searchString );
		if ( searchIndex == -1 )
		{
			return;
		}

		buffer.insert( searchIndex, insertString );
	}

	public static final void insertAfter( final StringBuffer buffer, final String searchString,
		final String insertString )
	{
		int searchIndex = buffer.indexOf( searchString );
		if ( searchIndex == -1 )
		{
			return;
		}

		buffer.insert( searchIndex + searchString.length(), insertString );
	}

	public static final String singleStringDelete( final String originalString, final String searchString )
	{
		return StringUtilities.singleStringReplace( originalString, searchString, "" );
	}

	public static final String singleStringReplace( final String originalString, final String searchString,
		final String replaceString )
	{
		if ( originalString == null )
		{
			return null;
		}

		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finally-allocated StringBuffers.

		int lastIndex = originalString.indexOf( searchString );
		if ( lastIndex == -1 )
		{
			return originalString;
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append( originalString.substring( 0, lastIndex ) );
		buffer.append( replaceString );
		buffer.append( originalString.substring( lastIndex + searchString.length() ) );
		return buffer.toString();
	}

	public static final void singleStringDelete( final StringBuffer buffer, final String searchString )
	{
		StringUtilities.singleStringReplace( buffer, searchString, "" );
	}

	public static final void singleStringReplace( final StringBuffer buffer, final String searchString,
		final String replaceString )
	{
		int index = buffer.indexOf( searchString );
		if ( index != -1 )
		{
			buffer.replace( index, index + searchString.length(), replaceString );
		}
	}

	public static final String globalStringDelete( final String originalString, final String searchString )
	{
		return StringUtilities.globalStringReplace( originalString, searchString, "" );
	}

	public static final String globalStringReplace( final String originalString, final String searchString,
		final String replaceString )
	{
		if ( originalString == null )
		{
			return null;
		}

		if ( searchString.equals( "" ) )
		{
			return originalString;
		}

		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finally-allocated StringBuffers.

		int lastIndex = originalString.indexOf( searchString );
		if ( lastIndex == -1 )
		{
			return originalString;
		}

		StringBuilder buffer = new StringBuilder( originalString );
		while ( lastIndex != -1 )
		{
			buffer.replace( lastIndex, lastIndex + searchString.length(), replaceString );
			lastIndex = buffer.indexOf( searchString, lastIndex + replaceString.length() );
		}

		return buffer.toString();
	}

	public static final void globalStringReplace( final StringBuffer buffer, final String tag, final int replaceWith )
	{
		StringUtilities.globalStringReplace( buffer, tag, String.valueOf( replaceWith ) );
	}

	public static final void globalStringDelete( final StringBuffer buffer, final String tag )
	{
		StringUtilities.globalStringReplace( buffer, tag, "" );
	}

	public static final void globalStringReplace( final StringBuffer buffer, final String tag, String replaceWith )
	{
		if ( buffer == null )
		{
			return;
		}

		if ( tag.equals( "" ) )
		{
			return;
		}

		if ( replaceWith == null )
		{
			replaceWith = "";
		}

		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finally-allocated StringBuffers.

		int lastIndex = buffer.indexOf( tag );
		while ( lastIndex != -1 )
		{
			buffer.replace( lastIndex, lastIndex + tag.length(), replaceWith );
			lastIndex = buffer.indexOf( tag, lastIndex + replaceWith.length() );
		}
	}
	
	public static final boolean isNumeric( String string )
	{
		if ( string == null || string.length() == 0 )
		{
			return false;
		}
		
		char ch = string.charAt( 0 );

		if ( ( ch != '-' ) && ( ch != '+' ) && !Character.isDigit( ch ) )
		{
			return false;
		}

		for ( int i = 1; i < string.length(); ++i )
		{
			ch = string.charAt( i );

			if ( ( ch != ',' ) && !Character.isDigit( ch ) )
			{
				return false;
			}
		}
		
		return true;
	}
	
	public static final boolean isFloat( String string )
	{
		if ( string == null || string.length() == 0 )
		{
			return false;
		}
		
		char ch = string.charAt( 0 );

		if ( ( ch != '-' ) && ( ch != '+' ) && ( ch != '.' ) && !Character.isDigit( ch ) )
		{
			return false;
		}

		boolean hasDecimalSeparator = false;

		for ( int i = 1; i < string.length(); ++i )
		{
			ch = string.charAt( i );

			if ( ch == '.' )
			{
				if ( hasDecimalSeparator )
				{
					return false;
				}

				hasDecimalSeparator = true;
			}

			if ( ch != '.' )
			{
				if ( ( ch != ',' ) && !Character.isDigit( ch ) )
				{
					return false;
				}
			}
		}
		
		return true;
	}

	public static final int parseInt( String string )
	{
		return StringUtilities.parseIntInternal1( string, false );
	}

	public static final int parseIntInternal1( String string, boolean throwException )
		throws NumberFormatException
	{
		if ( string == null )
		{
			return 0;
		}

		// Remove commas anywhere in the string
		string = StringUtilities.globalStringDelete( string, "," );

		// Remove whitespace from front and end of string
		string = string.trim();

		// Remove + sign from start of string
		if ( string.startsWith( "+" ) )
		{
			string = string.substring( 1 );
		}
		
		if ( string.length() == 0 )
		{
			return 0;
		}

		if ( StringUtilities.isNumeric( string ) )
		{
			try
			{
				return Integer.parseInt( string );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger.printLine( string + " is out of range, returning 0" );
				return 0;
			}
		}
		
		String fstring = string.substring( 0, string.length() - 1 );
		if ( StringUtilities.isFloat( fstring ) )
		{
			char ch = string.charAt( string.length() - 1 );
			float base = StringUtilities.parseFloat( fstring );
			float multiplier = 1.0f;
			
			switch ( ch )
			{
			case 'k':
			case 'K':
				multiplier = 1000.0f;
				break;
			case 'm':
			case 'M':
				multiplier = 1000000.0f;
				break;
			}
			
			return (int) ( base * multiplier );
		}

		if ( throwException )
		{
			throw new NumberFormatException( string );
		}

		return StringUtilities.parseIntInternal2( string );
	}

	public static final int parseIntInternal2( String string )
		throws NumberFormatException
	{
		string = NONINTEGER_PATTERN.matcher( string ).replaceAll( "" );

		if ( string.length() == 0 )
		{
			return 0;
		}

		try
		{
			return Integer.parseInt( string );
		}
		catch ( NumberFormatException e )
		{
			RequestLogger.printLine( string + " is out of range, returning 0" );
			return 0;
		}
	}

	public static final long parseLong( String string )
	{
		if ( string == null )
		{
			return 0L;
		}

		if ( string.startsWith( "+" ) )
		{
			string = string.substring( 1 );
		}

		string = StringUtilities.globalStringDelete( string, "," );

		if ( string.length() == 0 )
		{
			return 0L;
		}

		try
		{
			return Long.parseLong( string );
		}
		catch ( NumberFormatException e )
		{
			return 0L;
		}
	}

	public static final float parseFloat( String string )
	{
		if ( string == null )
		{
			return 0.0f;
		}

		if ( string.startsWith( "+" ) )
		{
			string = string.substring( 1 );
		}

		string = StringUtilities.globalStringDelete( string, "," );
		string = StringUtilities.globalStringDelete( string, " " );

		if ( string.length() == 0 )
		{
			return 0.0f;
		}

		if ( !StringUtilities.isFloat( string ) )
		{
			return 0.0f;
		}

		return Float.parseFloat( string );
	}

	public static final double parseDouble( String string )
	{
		if ( string == null )
		{
			return 0.0;
		}

		if ( string.startsWith( "+" ) )
		{
			string = string.substring( 1 );
		}

		string = StringUtilities.globalStringDelete( string, "," );
		string = StringUtilities.globalStringDelete( string, " " );

		if ( string.length() == 0 )
		{
			return 0.0;
		}

		if ( !StringUtilities.isFloat( string ) )
		{
			return 0.0;
		}

		return Double.parseDouble( string );
	}

	public static final String basicTextWrap( String text )
	{

		if ( text.length() < 80 || text.startsWith( "<html>" ) )
		{
			return text;
		}

		StringBuilder result = new StringBuilder();

		while ( text.length() > 0 )
		{
			if ( text.length() < 80 )
			{
				result.append( text );
				text = "";
			}
			else
			{
				int spaceIndex = text.lastIndexOf( " ", 80 );
				int breakIndex = text.lastIndexOf( "\n", spaceIndex );

				if ( breakIndex != -1 )
				{
					result.append( text.substring( 0, breakIndex ) );
					result.append( "\n" );
					text = text.substring( breakIndex ).trim();
				}
				else
				{
					result.append( text.substring( 0, spaceIndex ).trim() );
					result.append( "\n" );
					text = text.substring( spaceIndex ).trim();
				}
			}
		}

		return result.toString();
	}

	public static final void registerPrepositions( final String text )
	{
		Matcher m = StringUtilities.PREPOSITIONS_PATTERN.matcher( text );
		if ( !m.find() )
		{
			return;
		}
		StringUtilities.prepositionsMap.put( m.replaceAll( "@" ), text );
	}

	public static final String lookupPrepositions( final String text )
	{
		Matcher m = StringUtilities.PREPOSITIONS_PATTERN.matcher( text );
		if ( !m.find() )
		{
			return text;
		}
		String rv = StringUtilities.prepositionsMap.get( m.replaceAll( "@" ) );
		return rv == null ? text : rv;
	}
}
