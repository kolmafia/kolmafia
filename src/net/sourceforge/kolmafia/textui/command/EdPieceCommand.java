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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class EdPieceCommand
	extends AbstractCommand
{
	public static final String[][] ANIMAL =
	{
		{ "bear", "muscle", "1", "Muscle: +20; +2 Muscle Stats Per Fight" },
		{ "owl", "mysticality", "2", "Mysticality: +20; +2 Mysticality Stats Per Fight" },
		{ "puma", "moxie", "3", "Moxie: +20; +2 Moxie Stats Per Fight" },
		{ "hyena", "monster level", "4", "+20 to Monster Level" },
		{ "mouse", "item/meat", "5", "+10% Item Drops From Monsters; +20% Meat from Monsters" },
		{ "weasel", "block/HP regen", "6", "The first attack against you will always miss; Regenerate 10-20 HP per Adventure" },
		{ "fish", "sea", "7", "Lets you breath Adventure" },
	};

	public EdPieceCommand()
	{
		this.usage = "[?] <animal> - place a golden animal on the Crown of Ed (and equip it if unequipped)";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		boolean checking = KoLmafiaCLI.isExecutingCheckOnlyCommand;

		if ( checking )
		{
			StringBuilder output = new StringBuilder();

			output.append( "<table border=2 cols=5>" );
			output.append( "<tr>" );
			output.append( "<th>Decoration</th>" );
			output.append( "<th>Effect</th>" );
			output.append( "</tr>" );
			for ( String[] decoration : ANIMAL )
			{
				output.append( "<tr>" );
				output.append( "<td valign=top>" );
				output.append( "golden " );
				output.append( decoration[ 0 ] );
				output.append( "</td>" );
				output.append( "<td valign=top>" );
				output.append( decoration[ 3 ] );
				output.append( "</td>" );
				output.append( "</tr>" );
			}

			output.append( "</table>" );

			RequestLogger.printLine( output.toString() );
			RequestLogger.printLine();

			parameters = "";
		}

		String currentAnimal = Preferences.getString( "edPiece" );
		if ( parameters.length() == 0 )
		{
			StringBuilder output = new StringBuilder();

			output.append( "The current decoration on The Crown of Ed the Undying is " );
			if ( currentAnimal.equals( "" ) )
			{
				output.append( "&lt;nothing&gt;" );
			}
			else
			{
				output.append( "a golden " );
				output.append( currentAnimal );
			}
			output.append( "." );

			RequestLogger.printLine( output.toString() );
			RequestLogger.printLine();
			return;
		}

		String animal = parameters;
		String choice = "0";

		for ( String[] it : EdPieceCommand.ANIMAL )
		{
			if ( animal.equalsIgnoreCase( it[0] ) || it[1].contains( animal ) )
			{
				choice = it[2];
				animal = it[0];
				break;
			}
		}

		if ( choice.equals( "0" ) )
		{
			KoLmafia.updateDisplay( "Animal " + animal + " not recognised. Valid values are bear, owl, puma, hyena, mouse, weasel and fish." );
			return;
		}

		if ( EquipmentManager.getEquipment( EquipmentManager.HAT ).getItemId() != ItemPool.CROWN_OF_ED )
		{
			AdventureResult edPiece = ItemPool.get( ItemPool.CROWN_OF_ED );
			RequestThread.postRequest( new EquipmentRequest( edPiece, EquipmentManager.HAT ) );
		}

		if ( animal.equalsIgnoreCase( currentAnimal ) )
		{
			KoLmafia.updateDisplay( "Animal " + animal + " already equipped." );
			return;
		}

		if ( KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new GenericRequest( "inventory.php?action=activateedhat" ) );
		}
		if ( KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1063&option=" + choice ) );
		}
		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Crown of Ed decorated with golden " + animal + "." );
		}
	}
}
