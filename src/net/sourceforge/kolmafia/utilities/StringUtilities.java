/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.net.URLEncoder;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtilities
{
	private static final HashMap entityEncodeCache = new HashMap();
	private static final HashMap entityDecodeCache = new HashMap();

	private static final HashMap urlEncodeCache = new HashMap();
	private static final HashMap urlDecodeCache = new HashMap();

	private static final HashMap displayNameCache = new HashMap();
	private static final HashMap canonicalNameCache = new HashMap();
	
	private static final HashMap prepositionsMap = new HashMap();
	private static final WeakHashMap hashCache = new WeakHashMap();

	private static final Pattern NONINTEGER_PATTERN = Pattern.compile( "[^\\-0-9]" );
	private static final Pattern NONFLOAT_PATTERN = Pattern.compile( "[^\\-\\.0-9]" );
	private static final Pattern PREPOSITIONS_PATTERN = Pattern.compile(
		"\\b(?:about|above|across|after|against|along|among|around|at|before|behind|" +
		"below|beneath|beside|between|beyond|by|down|during|except|for|from|in|inside|" +
		"into|like|near|of|off|on|onto|out|outside|over|past|through|throughout|to|" +
		"under|up|upon|with|within|without)\\b" );

	/**
	 * Returns the encoded-encoded version of the provided UTF-8 string.
	 */

	public static final String getEntityEncode( final String utf8String )
	{
		return getEntityEncode( utf8String, true );
	}

	public static final String getEntityEncode( final String utf8String, boolean cache )
	{
		if ( utf8String == null )
		{
			return utf8String;
		}

		String entityString = null;

		if ( cache )
		{
			entityString = (String) StringUtilities.entityEncodeCache.get( utf8String );
		}

		if ( entityString == null )
		{
			if ( utf8String.indexOf( "&" ) == -1 || utf8String.indexOf( ";" ) == -1 )
			{
				entityString = CharacterEntities.escape( utf8String );
			}
			else
			{
				entityString = utf8String;
			}

			// The following replacement makes the Hodgman journals (which have
			// a double space after the colon) unsearchable in the Mall.
			//entityString = StringUtilities.globalStringReplace( entityString, "  ", " " );

			if ( cache )
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
		return getEntityDecode( entityString, true );
	}

	public static final String getEntityDecode( final String entityString, boolean cache )
	{
		if ( entityString == null )
		{
			return entityString;
		}

		String utf8String = null;

		if ( cache )
		{
			utf8String = (String) StringUtilities.entityDecodeCache.get( entityString );
		}

		if ( utf8String == null )
		{
			utf8String = CharacterEntities.unescape( entityString );

			if ( cache )
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

		String encodedURL = (String) StringUtilities.urlEncodeCache.get( url );

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

		String encodedURL = (String) StringUtilities.urlDecodeCache.get( url );

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

	public static final String getDisplayName( final String name )
	{
		if ( name == null )
		{
			return name;
		}

		String displayName = (String) StringUtilities.displayNameCache.get( name );

		if ( displayName == null )
		{
			displayName = StringUtilities.getEntityDecode( name );
			StringUtilities.displayNameCache.put( name, displayName );
		}

		return displayName;
	}

	/**
	 * Returns the canonicalized name for the provided display name.
	 */

	public static final String getCanonicalName( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		String canonicalName = (String) StringUtilities.canonicalNameCache.get( name );

		if ( canonicalName == null )
		{
			canonicalName = StringUtilities.getEntityEncode( name ).toLowerCase();
			StringUtilities.canonicalNameCache.put( name, canonicalName );
		}

		return canonicalName;
	}

	/**
	 * Returns a list of all elements which contain the given substring in their name.
	 *
	 * @param nameMap The map in which to search for the string
	 * @param substring The substring for which to search
	 */

	public static final List getMatchingNames( final String [] names, String searchString )
	{
		boolean isExactMatch = searchString.startsWith( "\"" );
		List matchList = new ArrayList();

		if ( isExactMatch )
		{
			searchString = searchString.substring( 1,
				searchString.endsWith( "\"" ) ? searchString.length() - 1 : searchString.length() );
		}

		searchString = StringUtilities.getCanonicalName( searchString.trim() );

		if ( searchString.length() == 0 )
		{
			return matchList;
		}

		if ( isExactMatch )
		{
			if ( Arrays.binarySearch( names, searchString ) >= 0 )
			{
				matchList.add( searchString );
			}

			return matchList;
		}

		if ( Arrays.binarySearch( names, searchString ) >= 0 )
		{
			matchList.add( searchString );
			return matchList;
		}

		int nameCount = names.length;
		int[] hashes = (int[]) StringUtilities.hashCache.get( names );
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
			if ( (hashes[ i ] & hash) == hash &&
				StringUtilities.substringMatches( names[ i ], searchString, true ) )
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
			if ( (hashes[ i ] & hash) == hash &&
				StringUtilities.substringMatches( names[ i ], searchString, false ) )
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
			if ( (hashes[ i ] & hash) == hash &&
				StringUtilities.fuzzyMatches( names[i], searchString ) )
			{
				matchList.add( names[i] );
			}
		}

		return matchList;
	}
	
	private static final int stringHash( String s )
	{
		int hash = 0;
		for ( int i = s.length() - 1; i >= 0; --i )
		{
			hash |= 1 << (s.charAt( i ) & 0x1F);
		}
		return hash;
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

		return StringUtilities.fuzzyMatches( sourceString, searchString, -1, -1 );
	}

	private static final boolean fuzzyMatches( final String sourceString, final String searchString, final int lastSourceIndex, final int lastSearchIndex )
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
			if ( sourceIndex == 0 || sourceIndex == lastSourceIndex + 1 || !Character.isLetterOrDigit( sourceString.charAt( sourceIndex - 1 ) ) )
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

	public static final void insertBefore( final StringBuffer buffer, final String searchString, final String insertString )
	{
		int searchIndex = buffer.indexOf( searchString );
		if ( searchIndex == -1 )
		{
			return;
		}

		buffer.insert( searchIndex, insertString );
	}

	public static final void insertAfter( final StringBuffer buffer, final String searchString, final String insertString )
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
		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finalally-allocated StringBuffers.

		int lastIndex = originalString.indexOf( searchString );
		if ( lastIndex == -1 )
		{
			return originalString;
		}

		StringBuffer buffer = new StringBuffer();
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

		StringBuffer buffer = new StringBuffer( originalString );
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
		// a static finalally-allocated StringBuffers.

		int lastIndex = buffer.indexOf( tag );
		while ( lastIndex != -1 )
		{
			buffer.replace( lastIndex, lastIndex + tag.length(), replaceWith );
			lastIndex = buffer.indexOf( tag, lastIndex + replaceWith.length() );
		}
	}

	public static final int parseInt( final String string )
	{
		if ( string == null )
		{
			return 0;
		}

		float multiplier = 1f;

		if ( string.endsWith( "k" ) || string.endsWith( "K" ) )
		{
			multiplier = 1000f;
		}
		else if ( string.endsWith( "m" ) || string.endsWith( "M" ) )
		{
			multiplier = 1000000f;
		}
		else
		{
			String clean = StringUtilities.NONINTEGER_PATTERN.matcher( string ).replaceAll( "" );
			return clean.equals( "" ) || clean.equals( "-" ) ? 0 : Integer.parseInt( clean );
		}

		String clean = StringUtilities.NONFLOAT_PATTERN.matcher( string ).replaceAll( "" );

		float result = clean.equals( "" ) || clean.equals( "-" ) ? 0 : Float.parseFloat( clean );
		return (int) (result * multiplier);
	}

	public static final float parseFloat( final String string )
	{
		if ( string == null )
		{
			return 0.0f;
		}

		String clean = StringUtilities.NONFLOAT_PATTERN.matcher( string ).replaceAll( "" );
		return clean.equals( "" ) ? 0.0f : Float.parseFloat( clean );
	}

	public static final String basicTextWrap(String text) {

		if (text.length() < 80 || text.startsWith("<html>")) {
			return text;
		}

		StringBuffer result = new StringBuffer();

		while (text.length() > 0) {
			if (text.length() < 80) {
				result.append(text);
				text = "";
			}
			else {
				int spaceIndex = text.lastIndexOf(" ", 80);
				int breakIndex = text.lastIndexOf("\n", spaceIndex);

				if (breakIndex != -1) {
					result.append(text.substring(0, breakIndex));
					result.append("\n");
					text = text.substring(breakIndex).trim();
				}
				else {
					result.append(text.substring(0, spaceIndex).trim());
					result.append("\n");
					text = text.substring(spaceIndex).trim();
				}
			}
		}

		return result.toString();
	}
	
	public static final void registerPrepositions( String text )
	{
		Matcher m = StringUtilities.PREPOSITIONS_PATTERN.matcher( text );
		if ( !m.find() )
		{
			return;
		}
		StringUtilities.prepositionsMap.put( m.replaceAll( "@" ), text );
	}
	
	public static final String lookupPrepositions( String text )
	{
		Matcher m = StringUtilities.PREPOSITIONS_PATTERN.matcher( text );
		if ( !m.find() )
		{
			return text;
		}
		String rv = (String) StringUtilities.prepositionsMap.get( m.replaceAll( "@" ) );
		return rv == null ? text : rv;
	}
}
