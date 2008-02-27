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

package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class StringUtilities
{
	private static final Pattern NONDIGIT_PATTERN = Pattern.compile( "[^\\-0-9]" );
	private static final Pattern NONFLOAT_PATTERN = Pattern.compile( "[^\\-\\.0-9]" );

	/**
	 * Returns the display name name, where all HTML representations are replaced with their appropriate display
	 * symbols.
	 *
	 * @param name The name to be transformed to display form
	 * @return The display form of the given name
	 */

	public static final String getDisplayName( final String name )
	{
		return name == null ? null : CharacterEntities.unescape( name );
	}

	/**
	 * Returns the canonicalized name, where all symbols are replaced with their HTML representations.
	 *
	 * @param name The name to be canonicalized
	 * @return The canonicalized name
	 */

	public static final String getCanonicalName( final String name )
	{
		return name == null ? null : StringUtilities.globalStringReplace( CharacterEntities.escape(
			CharacterEntities.unescape( name ) ).toLowerCase(), "  ", " " );
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
			StringUtilities.getCanonicalName(
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
			if ( StringUtilities.substringMatches( names[ i ], searchString ) )
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
			String tempSearchString = StringUtilities.globalStringDelete( searchString, " " );

			for ( int i = 0; i < names.length; ++i )
			{
				if ( StringUtilities.substringMatches( names[ i ], tempSearchString ) )
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
		return StringUtilities.substringMatches( source, substring, false );
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
			matchedSpace = Character.isWhitespace( searchChar );

			if ( searchChar == sourceChar )
			{
				++sourceIndex;
				continue;
			}

			// Search for the next matching character after
			// the whitespace in the source string.

			while ( searchChar != sourceChar )
			{
				if ( ++sourceIndex > maxSourceIndex )
				{
					return false;
				}

				sourceChar = Character.toLowerCase( sourceString.charAt( sourceIndex ) );

				if ( !matchedSpace && Character.isWhitespace( sourceChar ) )
				{
					return false;
				}
			}

			// Now that we know we've found the character,
			// increment the counter and continue.

			++sourceIndex;
		}

		return true;
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
		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finalally-allocated StringBuffers.

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

		String clean = StringUtilities.NONDIGIT_PATTERN.matcher( string ).replaceAll( "" );
		return clean.equals( "" ) || clean.equals( "-" ) ? 0 : Integer.parseInt( clean );
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
}
