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

	public static String validateChoiceFields( final String decision, final String extraFields, final String responseText)
	{
		// Given the response text from visiting a choice, determine if
		// a particular decision (option) and set of extra fields are valid.
		//
		// Some decisions are not always available.
		// Some decisions have extra fields from "select" inputs which must be specified.
		// Some select inputs are variable: available options vary.

		// This method checks all of the following:
		//
		// - The decision is currently available
		// - All required select inputs are supplied
		// - No invalid select inputs are supplied
		//
		// If all is well, null is returned, and decision + extraFields
		// will work as a response to the choice as presented
		//
		// If there are errors, returns a string, suitable as an error
		// message, describing all of the issues.

		// Must have a response text to examine
		if ( responseText == null )
		{
			return "No response text.";
		}

		// Figure out which choice we are in from responseText
		int choice = ChoiceManager.extractChoice( responseText );
		if ( choice == 0 )
		{
			return "No choice adventure in response text.";
		}

		String choiceOption = choice + "/" + decision;

		// See if supplied decision is available
		Map<Integer,String> choices = ChoiceUtilities.parseChoices( responseText );
		if ( !choices.containsKey( StringUtilities.parseInt( decision ) ) )
		{
			return "Choice option " + choiceOption + " is not available.";
		}
		
		// Accumulate multiple errors in a buffer
		StringBuilder errors = new StringBuilder();

		// Extract supplied extra fields
		Set<String> extras = new TreeSet<String>();
		for ( String field : extraFields.split( "&" ) )
		{
			if ( field.equals( "" ) )
			{
			}
			else if ( field.indexOf( "=" ) != -1 )
			{
				extras.add( field );
			}
			else
			{
				errors.append( "Invalid extra field: '" + field + "'; no value supplied.\n" );
			}
		}

		// Get a map from CHOICE => map from NAME => set of OPTIONS
		Map<Integer, Map<String, Set<String>>> forms = ChoiceUtilities.parseSelectInputs( responseText );

		// Does the decision have extra selects?
		Map<String, Set<String>> options = forms.get( StringUtilities.parseInt( decision ) );

		if ( options == null )
		{
			// No. If the user supplied no extra fields, all is well
			if ( extras.size() == 0 )
			{
				return ( errors.length() > 0 ) ? errors.toString() : null;
			}
			// Otherwise, list all unexpected extra fields
			for ( String extra : extras )
			{
				errors.append( "Choice option " + choiceOption + "does not require '" + extra + "'.\n" );
			}
			return errors.toString();
		}

		// There are selects available/required for this form.

		// Make a map from supplied select field => value
		Map<String, String> suppliedFields = new TreeMap<String, String>();
		for ( String field : extras )
		{
			// We validated this above; only fields with '=' are included
			int equals = field.indexOf( "=" );
			String name = field.substring( 0, equals );
			String value = field.substring( equals + 1 );
			suppliedFields.put( name, value );
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
				errors.append( "Choice option " + choiceOption + " requires '" + name + "' but not supplied.\n" );
			}
			else if ( !values.contains( supplied) )
			{
				errors.append( "Choice option " + choiceOption + " requires '" + name + "' but '" + supplied + "' is not a valid value.\n" );
			}
		}

		// No invalid selects in the form can be supplied
		for ( Map.Entry<String, String> supplied : suppliedFields.entrySet() )
		{
			String name = supplied.getKey();
			if ( !options.containsKey( name ) )
			{
				errors.append( "Choice option " + choiceOption + "does not require '" + name + "'.\n" );
			}
		}

		return ( errors.length() > 0 ) ? errors.toString() : null;
	}
}
