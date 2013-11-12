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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

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
		if ( GenericRequest.choiceHandled || ChoiceManager.lastResponseText == null )
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
		TreeMap choices = ChoiceCommand.parseChoices();
		if ( StringUtilities.isNumeric( parameters ) )
		{
			decision = StringUtilities.parseInt( parameters );
		}
		else
		{
			Iterator i = choices.entrySet().iterator();
			while ( i.hasNext() )
			{
				Map.Entry e = (Map.Entry) i.next();
				if ( ((String) e.getValue()).toLowerCase().indexOf( parameters.toLowerCase() ) != -1 )
				{
					decision = ((Integer) e.getKey()).intValue();
					break;
				}
			}
		}
		
		if ( !choices.containsKey( IntegerPool.get( decision ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That isn't one of your choices." );
			return;
		}
		if (always) {
		    String pref = "choiceAdventure" + ChoiceManager.lastChoice;
		    RequestLogger.printLine( pref + " => " + decision );
		    Preferences.setInteger( pref, decision );
		}		
		ChoiceManager.processChoiceAdventure( decision );
	}

	private static final Pattern OPTION_PATTERN = Pattern.compile( "<form(?=.*?name=option value=(\\d+)).*?class=button.*?value=\"([^\"]+)\".*?</form>", Pattern.DOTALL );
	private static final Pattern LINK_PATTERN = Pattern.compile( "href='choice.php\\?.*option=(\\d+)'" );

	public static TreeMap parseChoices()
	{
		TreeMap rv = new TreeMap();
		if ( GenericRequest.choiceHandled || ChoiceManager.lastResponseText == null )
		{
			return rv;
		}

		int choice = ChoiceManager.extractChoice( ChoiceManager.lastResponseText );
		Object[][] possibleDecisions = ChoiceManager.choiceSpoilers( choice );
		if ( possibleDecisions == null )
		{
			possibleDecisions = new Object[][] { null, null, {} };
		}
		Object[] options = possibleDecisions[ 2 ];
		
		Matcher m = OPTION_PATTERN.matcher( ChoiceManager.lastResponseText );
		while ( m.find() )
		{
			int decision = Integer.parseInt( m.group( 1 ) );
			Integer key = IntegerPool.get( decision );
			Object option = ChoiceManager.findOption( options, decision );
			String text = m.group( 2 );
			if ( option != null )
			{
				text = text + " (" + option.toString() + ")";
			}
			rv.put( IntegerPool.get( decision ), text );
		}

		m = LINK_PATTERN.matcher( ChoiceManager.lastResponseText );
		while ( m.find() )
		{
			int decision = Integer.parseInt( m.group( 1 ) );
			Integer key = IntegerPool.get( decision );
			if ( rv.get( key ) != null )
			{
				continue;
			}
			Object option = ChoiceManager.findOption( options, decision );
			String text = "(secret choice)";
			if ( option != null )
			{
				text = text + " (" + option.toString() + ")";
			}
			rv.put( key, text );
		}

		return rv;
	}

	public static boolean optionAvailable( final String decision, final String responseText)
	{
		Matcher m = OPTION_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			if ( m.group( 1 ).equals( decision ) )
			{
				return true;
			}
		}

		m = LINK_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			if ( m.group( 1 ).equals( decision ) )
			{
				return true;
			}
		}

		return false;
	}	

	public static void printChoices()
	{
		TreeMap choices = ChoiceCommand.parseChoices();
		Iterator i = choices.entrySet().iterator();
		while ( i.hasNext() )
		{
			Map.Entry e = (Map.Entry) i.next();
			RequestLogger.printLine( "<b>choice " + e.getKey() + "</b>: " + e.getValue() );
		}
	}
	
	public static void logChoices()
	{
		TreeMap choices = ChoiceCommand.parseChoices();
		Iterator i = choices.entrySet().iterator();
		while ( i.hasNext() )
		{
			Map.Entry e = (Map.Entry) i.next();
			RequestLogger.updateSessionLog( "choice " + ChoiceManager.lastChoice + "/" + e.getKey() + ": " + e.getValue() );
			RequestLogger.printLine( "<b>choice " + e.getKey() + "</b>: " + e.getValue() );
		}
	}
}
