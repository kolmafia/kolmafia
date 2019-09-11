/**
 * Copyright (c) 2005-2018, KoLmafia development team
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.session.ChoiceManager;

/**
 *	Utilities for extracting data from a choice.php response
 */
public class ChoiceUtilities
{
	private static final Pattern FORM_PATTERN = Pattern.compile( "<form.*?</form>", Pattern.DOTALL );
	private static final Pattern OPTION_PATTERN1 = Pattern.compile( "name=[\"']?option[\"']? value=[\"']?(\\d+)[\"']?" );
	private static final Pattern TEXT_PATTERN1 = Pattern.compile( "class=[\"']?button[\"']?.*?value=(?:\"([^\"]*)\"|'([^']*)'|([^ >]*))" );

	private static final Pattern LINK_PATTERN = Pattern.compile( "<[aA] .*?</[aA]>", Pattern.DOTALL );
	private static final Pattern OPTION_PATTERN2 = Pattern.compile( "&option=(\\d+)" );
	private static final Pattern TEXT_PATTERN2 = Pattern.compile( "title=(?:\"([^\"]*)\"|'([^']*)'|([^ >]*))" );

	public static Map<Integer,String> parseChoices( final String responseText )
	{
		TreeMap<Integer,String> rv = new TreeMap<Integer,String>();
		if ( responseText == null )
		{
			return rv;
		}

		Matcher m = FORM_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			String form = m.group();
			if ( !form.contains( "choice.php" ) )
			{
				continue;
			}
			Matcher optMatcher = OPTION_PATTERN1.matcher( form );
			if ( !optMatcher.find() )
			{
				continue;
			}
			int decision = Integer.parseInt( optMatcher.group( 1 ) );
			Integer key = IntegerPool.get( decision );
			if ( rv.get( key ) != null )
			{
				continue;
			}
			Matcher textMatcher = TEXT_PATTERN1.matcher( form );
			String text =
				!textMatcher.find() ?
				"(secret choice)" :
				textMatcher.group( 1 ) != null ?
				textMatcher.group( 1 ) :
				textMatcher.group( 2 ) != null ?
				textMatcher.group( 2 ) :
				textMatcher.group( 3 ) != null ?
				textMatcher.group( 3 ) :
				"(secret choice)";
			rv.put( key, text );
		}

		m = LINK_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			String form = m.group();
			if ( !form.contains( "choice.php" ) )
			{
				continue;
			}
			Matcher optMatcher = OPTION_PATTERN2.matcher( form );
			if ( !optMatcher.find() )
			{
				continue;
			}
			int decision = Integer.parseInt( optMatcher.group( 1 ) );
			Integer key = IntegerPool.get( decision );
			if ( rv.get( key ) != null )
			{
				continue;
			}
			Matcher textMatcher = TEXT_PATTERN2.matcher( form );
			String text =
				!textMatcher.find() ?
				"(secret choice)" :
				textMatcher.group( 1 ) != null ?
				textMatcher.group( 1 ) :
				textMatcher.group( 2 ) != null ?
				textMatcher.group( 2 ) :
				textMatcher.group( 3 ) != null ?
				textMatcher.group( 3 ) :
				"(secret choice)";
			rv.put( key, text );
		}

		return rv;
	}

	public static Map<Integer,String> parseChoicesWithSpoilers()
	{
		Map<Integer,String> rv = ChoiceUtilities.parseChoices( ChoiceManager.lastResponseText );

		if ( !ChoiceManager.handlingChoice || ChoiceManager.lastResponseText == null )
		{
			return rv;
		}

		Object[][] possibleDecisions = ChoiceManager.choiceSpoilers( ChoiceManager.lastChoice );
		if ( possibleDecisions == null )
		{
			return rv;
		}

		Object[] options = possibleDecisions[ 2 ];
		if ( options == null )
		{
			return rv;
		}

		for ( Map.Entry<Integer,String> entry : rv.entrySet() )
		{
			Integer key = entry.getKey();
			Object option = ChoiceManager.findOption( options, key );
			if ( option != null )
			{
				String text = entry.getValue() + " (" + option.toString() + ")";
				rv.put( key, text );
			}
		}

		return rv;
	}

	public static boolean optionAvailable( final String decision, final String responseText)
	{
		Map<Integer,String> choices = ChoiceUtilities.parseChoices( responseText );
		return choices.containsKey( StringUtilities.parseInt( decision ) );
	}

	public static String actionOption( final String action, final String responseText)
	{
		Map<Integer,String> choices = ChoiceUtilities.parseChoices( responseText );
		for ( Map.Entry<Integer,String> entry : choices.entrySet() )
		{
			if ( entry.getValue().equals( action ) )
			{
				return String.valueOf( entry.getKey() );
			}
		}
		return null;
	}

	// Support for extra fields. For example, tossid=10320
	// Assume that they are all options in a "select" (dropdown) input

	// <select name=tossid>><option value=7375>actual tapas  (5 casualties)</option>
	private static final Pattern SELECT_PATTERN = Pattern.compile( "<select name=(.*?)>(.*?)</select>", Pattern.DOTALL );
	private static final Pattern SELECT_OPTION_PATTERN = Pattern.compile( "<option value=(.*?)>(.*?)</option>" );

	public static Map<Integer, Map<String, Set<String>>> parseSelectInputs( final String responseText )
	{
		// Return a map from CHOICE => map from  NAME => set of OPTIONS
		Map<Integer, Map<String, Set<String>>> rv = new TreeMap<Integer, Map<String, Set<String>>>();

		if ( responseText == null )
		{
			return rv;
		}

		// Find all choice forms
		Matcher m = FORM_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			String form = m.group();
			if ( !form.contains( "choice.php" ) )
			{
				continue;
			}
			Matcher optMatcher = OPTION_PATTERN1.matcher( form );
			if ( !optMatcher.find() )
			{
				continue;
			}

			// Collect all the selects from this form
			Map<String, Set<String>> choice = new TreeMap<String, Set<String>>();

			// Find all "select" tags within this form
			Matcher s = SELECT_PATTERN.matcher( form );
			while ( s.find() )
			{
				String name = s.group(1);

				// For each, extract all the options into a set
				Set<String> options = new TreeSet<String>();

				Matcher o = SELECT_OPTION_PATTERN.matcher( s.group(2) );
				while ( o.find() )
				{
					options.add( o.group(1) );
				}

				choice.put( name, options );
			}

			rv.put( Integer.parseInt( optMatcher.group( 1 ) ), choice );
		}

		return rv;
	}

	public static Map<Integer, Map<String, Map<String, String>>> parseSelectInputsWithTags( final String responseText )
	{
		// Return a map from CHOICE => map from  NAME => map from OPTION => SPOILER
		Map<Integer, Map<String, Map<String, String>>> rv = new TreeMap<Integer, Map<String, Map<String, String>>>();

		if ( responseText == null )
		{
			return rv;
		}

		// Find all choice forms
		Matcher m = FORM_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			String form = m.group();
			if ( !form.contains( "choice.php" ) )
			{
				continue;
			}
			Matcher optMatcher = OPTION_PATTERN1.matcher( form );
			if ( !optMatcher.find() )
			{
				continue;
			}

			// Collect all the selects from this form
			Map<String, Map<String, String>> choice = new TreeMap<String, Map<String, String>>();

			// Find all "select" tags within this form
			Matcher s = SELECT_PATTERN.matcher( form );
			while ( s.find() )
			{
				String name = s.group(1);

				// For each, extract all the options into a map
				Map<String, String> options = new TreeMap<String, String>();

				Matcher o = SELECT_OPTION_PATTERN.matcher( s.group(2) );
				while ( o.find() )
				{
					String option = o.group(1);
					String tag = o.group(2);
					options.put( option, tag );
				}
				choice.put( name, options );
			}

			rv.put( Integer.parseInt( optMatcher.group( 1 ) ), choice );
		}

		return rv;
	}

	public static boolean extraFieldsValid( final String decision, final String extraFields, final String responseText)
	{
		// Get a map from CHOICE => map from  NAME => set of OPTIONS
		Map<Integer, Map<String, Set<String>>> forms = ChoiceUtilities.parseSelectInputs( responseText );

		// Make sure that the decision has available selects
		Map<String, Set<String>> options = forms.get( StringUtilities.parseInt( decision ) );
		if ( options == null )
		{
			// If there are no selects in this form, extra fields are not allowed.
			return extraFields.equals( "" );
		}

		// There are selects available/required for this form.
		// Make a map from extra field => value
		Map<String, String> suppliedFields = new HashMap<String, String>();
		for ( String field : extraFields.split( "&" ) )
		{
			int equals = field.indexOf( "=" );
			if ( equals != -1 )
			{
				String name = field.substring( 0, equals );
				String value = field.substring( equals + 1 );
				suppliedFields.put( name, value );
			}
		}

		// All selects in the form must have a value supplied
		for ( Map.Entry<String, Set<String>> select : options.entrySet() )
		{
			String name = select.getKey();
			Set<String> values = select.getValue();
			String supplied = suppliedFields.get( name );
			if ( supplied == null )
			{
				// Did not supply a value for a field
				return false;
			}
			if ( !values.contains( supplied) )
			{
				// Supplied an unavailable value
				return false;
			}
		}

		// All extraFields have been validated
		return true;
	}
}
