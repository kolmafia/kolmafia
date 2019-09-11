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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.Map;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
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
		Map<Integer,String> choices = ChoiceUtilities.parseChoicesWithSpoilers();
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

	public static void printChoices()
	{
		Map<Integer,String> choices = ChoiceUtilities.parseChoicesWithSpoilers();
		Map<Integer, Map<String, Map<String, String>>> selects = ChoiceUtilities.parseSelectInputsWithTags( ChoiceManager.lastResponseText );
		for ( Map.Entry<Integer,String> choice : choices.entrySet() )
		{
			Integer choiceKey = choice.getKey();
			RequestLogger.printLine( "<b>choice " + choiceKey + "</b>: " + choice.getValue() );
			Map<String, Map<String, String>> choiceSelects = selects.get( choiceKey );
			if ( choiceSelects != null )
			{
				for ( Map.Entry<String,Map<String, String>> select : choiceSelects.entrySet() )
				{
					Map<String, String> options = select.getValue();
					RequestLogger.printLine( "&nbsp;&nbsp;select = <b>" + select.getKey() + "</b> (" + options.size() + " options)" );
					for ( Map.Entry<String, String> option : options.entrySet() )
					{
						RequestLogger.printLine( "&nbsp;&nbsp;&nbsp;&nbsp;" + option.getKey() + " => " + option.getValue() );
					}
				}
			}
		}
	}
	
	public static void logChoices()
	{
		Map<Integer,String> choices = ChoiceUtilities.parseChoicesWithSpoilers();
		int choice = ChoiceManager.currentChoice();
		for ( Map.Entry<Integer,String> entry : choices.entrySet() )
		{
			RequestLogger.updateSessionLog( "choice " + choice + "/" + entry.getKey() + ": " + entry.getValue() );
			RequestLogger.printLine( "<b>choice " + entry.getKey() + "</b>: " + entry.getValue() );
		}
	}
}
