/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChoiceCommand
	extends AbstractCommand
{
	public ChoiceCommand()
	{
		this.usage = " [<number> [always]|<text>] - list or choose choice adventure options.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !ChoiceManager.handlingChoice || ChoiceManager.lastResponseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You aren't in a choice adventure." );
			return;
		}
		if ( parameters.equals( "" ) )
		{
			ChoiceCommand.printChoices();
			return;
		}
		boolean always = false;
		if ( parameters.endsWith(" always") )
		{
		    always = true;
		    parameters = parameters.substring( 0, parameters.length() - 7 ).trim();
		}
		int decision = 0;
		TreeMap<Integer,String> choices = ChoiceCommand.parseChoicesWithSpoilers();
		if ( StringUtilities.isNumeric( parameters ) )
		{
			decision = StringUtilities.parseInt( parameters );
		}
		else
		{
			for ( Map.Entry<Integer,String> entry : choices.entrySet() )
			{
				if ( entry.getValue().toLowerCase().indexOf( parameters.toLowerCase() ) != -1 )
				{
					decision = entry.getKey().intValue();
					break;
				}
			}
		}
		
		if ( !choices.containsKey( IntegerPool.get( decision ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That isn't one of your choices." );
			return;
		}

		if (always)
		{
			String pref = "choiceAdventure" + ChoiceManager.currentChoice();
			RequestLogger.printLine( pref + " => " + decision );
			Preferences.setInteger( pref, decision );
		}

		ChoiceManager.processChoiceAdventure( decision, true );
	}

	private static final Pattern FORM_PATTERN = Pattern.compile( "<form.*?</form>", Pattern.DOTALL );
	private static final Pattern OPTION_PATTERN1 = Pattern.compile( "name=[\"']?option[\"']? value=[\"']?(\\d+)[\"']?" );
	private static final Pattern TEXT_PATTERN1 = Pattern.compile( "class=[\"']?button[\"']?.*?value=[\"']?([^\"'>]*)[\"']?" );

	private static final Pattern LINK_PATTERN = Pattern.compile( "<[aA] .*?</[aA]>", Pattern.DOTALL );
	private static final Pattern OPTION_PATTERN2 = Pattern.compile( "&option=(\\d+)" );
	private static final Pattern TEXT_PATTERN2 = Pattern.compile( "title=[\"']?([^\"'>]*)[\"']?" );

	public static TreeMap<Integer,String> parseChoices( final String responseText )
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
			String text = textMatcher.find() ? textMatcher.group( 1 ) : "(secret choice)";
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
			String text = textMatcher.find() ? textMatcher.group( 1 ) : "(secret choice)";
			rv.put( key, text );
		}

		return rv;
	}

	private static TreeMap<Integer,String> parseChoicesWithSpoilers()
	{
		TreeMap<Integer,String> rv = ChoiceCommand.parseChoices( ChoiceManager.lastResponseText );

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
		TreeMap<Integer,String> choices = ChoiceCommand.parseChoices( responseText );
		return choices.containsKey( StringUtilities.parseInt( decision ) );
	}

	public static String actionOption( final String action, final String responseText)
	{
		TreeMap<Integer,String> choices = ChoiceCommand.parseChoices( responseText );
		for ( Map.Entry<Integer,String> entry : choices.entrySet() )
		{
			if ( entry.getValue().equals( action ) )
			{
				return String.valueOf( entry.getKey() );
			}
		}
		return null;
	}

	public static void printChoices()
	{
		TreeMap<Integer,String> choices = ChoiceCommand.parseChoicesWithSpoilers();
		for ( Map.Entry<Integer,String> entry : choices.entrySet() )
		{
			RequestLogger.printLine( "<b>choice " + entry.getKey() + "</b>: " + entry.getValue() );
		}
	}
	
	public static void logChoices()
	{
		TreeMap<Integer,String> choices = ChoiceCommand.parseChoicesWithSpoilers();
		int choice = ChoiceManager.currentChoice();
		for ( Map.Entry<Integer,String> entry : choices.entrySet() )
		{
			RequestLogger.updateSessionLog( "choice " + choice + "/" + entry.getKey() + ": " + entry.getValue() );
			RequestLogger.printLine( "<b>choice " + entry.getKey() + "</b>: " + entry.getValue() );
		}
	}
}
