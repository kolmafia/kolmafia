/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.preferences.Preferences;

public class VillainLairDecorator
{
	private static final Pattern LABEL_PATTERN = Pattern.compile( "from top to bottom:<br /><center>\"(.*?)\"<br />\"(.*?)\"<br />\"(.*?)\"</center>" );

	private static final String[] SYMBOLOGY_OPTIONS =
	{
		"Vent Poisonous Gas",
		"Monorail Shutdown",
		"Roaring Fire",
		"Poison Gas"
	};

	private static final String[] GREEN_CLUES =
	{
		"aqua button", // "Maintenance, please ensure that the aqua button is disabled, the pool is moderately active now."
		"disconnect the jade", // "Maintenance, please disconnect the jade button, the lunchroom is constantly full."
		"remove the Sub", // "Maintenance, please remove the Sub recall system, the dome already has enough aquanauts. It's hooked up to burnt umber."
		"press the periwinkle", // "Hello? Can someone press the periwinkle button? We're trapped behind the lavafall."
		"moss", // "Nobody press the moss button, we've got two squads in the barracks."
		"disable the indigo button", // "Can someone disable the indigo button, it's already too cold here."
		"hit the pine button", // "Ok, the charges are in place. You can hit the pine button after today when the patrol is cleared out."
		"tangerine", // "Stop playing with the tangerine button, we don't need to recruit anyone else."
		"bathrooms are full", // "Avoid the orange button folks, we've got a jammed magma chute and the bathrooms are full."
	};

	private static final String[] BLUE_CLUES =
	{
		"press the navy button", // "If anyone needs to send out an exploration team, press the navy button."
		"don't hit the navy", // "Hey folks, don't hit the navy button until my squad is back in the base."
		"green means alert", // "Remember everyone, green means alert."
		"magma heating system", // "Avoid the peach button everyone, the magma heating system isn't quite right."
		"off the gondola", // "Clear to hit the navy button once our squads are off the gondola!"
		"no pay", // "Don't forget, navy button means no pay today. Stupid furnace."
		"avoid pressing the green", // "Let's avoid pressing the green button unless we want to share the barracks with even more recruits!"
		"press the vermilion", // "Clear to press the vermilion button once my squad is off the lift."
		"Jello", // "The mint button is on the fritz and we can't afford to lose more Jello. Be careful."
		"pumpkin-colored", // "A reminder, only press the pumpkin-colored button if you know the team outside the door."
		"engage the flood-wash", // "Please be courteous and engage the flood-wash system if you make a mess in the bathroom. It's the apricot button."
		"seafoam", // "In case of emergency, press the seafoam button."
	};

	private static final String[] ORANGE_CLUES =
	{
		"powder blue", // "Remember, powder blue means gas the cafeteria."
		"pine button sounds", // "Don't forget, the pine button sounds the alarm and alerts the defenses."
		"silo", // "Avoid the peach button while a crew is in the silo."
	};

	public static final String Symbology( final String responseText )
	{
		if ( !responseText.contains( "Symbology" ) )
		{
			return "0";
		}
		Matcher matcher = LABEL_PATTERN.matcher( responseText );
		int index = 0;
		if ( matcher.find() )
		{
			for ( String option : VillainLairDecorator.SYMBOLOGY_OPTIONS )
			{
				for ( int i = 1; i <= 3; i++ )
				{
					if ( option.equals( matcher.group( i ) ) )
					{
						index = i;
						break;
					}
				}
			}
		}
		return String.valueOf( index );
	}

	public static final void parseColorClue( final String text )
	{
		for ( String clue : VillainLairDecorator.GREEN_CLUES )
		{
			if ( text.contains( clue ) )
			{
				Preferences.setString( "_villainLairColor", "green" );
				return;
			}
		}
		for ( String clue : VillainLairDecorator.BLUE_CLUES )
		{
			if ( text.contains( clue ) )
			{
				Preferences.setString( "_villainLairColor", "blue" );
				return;
			}
		}
		for ( String clue : VillainLairDecorator.ORANGE_CLUES )
		{
			if ( text.contains( clue ) )
			{
				Preferences.setString( "_villainLairColor", "orange" );
				return;
			}
		}
	}

	public static final String spoilColorChoice()
	{
		String color = Preferences.getString( "_villainLairColor" );
		if ( color.equals( "blue" ) )
		{
			return "1";
		}
		if ( color.equals( "green" ) )
		{
			return "2";
		}
		if ( color.equals( "orange" ) )
		{
			return "3";
		}
		return "0";
	}

}
