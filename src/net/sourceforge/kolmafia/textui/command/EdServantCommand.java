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

import java.io.File;

import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.EdBaseRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;

public class EdServantCommand
	extends AbstractCommand
{
	public EdServantCommand()
	{
		this.usage = " - List status of Ed's servants.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		// If User wants to see all possible servants, list them.
		if ( cmd.equals( "servants" ) )
		{
			StringBuilder output = new StringBuilder();
			this.printServants( output );
			if ( KoLCharacter.isEd() )
			{
				this.printCurrentServant( output );
			}
		}

		// If Ed wants to switch his active servant, attempt it.
		if ( cmd.equals( "servant" ) )
		{
			if ( !KoLCharacter.isEd() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Only Ed the Undying has entombed servants!" );
				return;
			}

			String type = parameters.trim();
			if ( !type.equals( "" ) )
			{
				Object [] data = EdServantData.typeToData( type );
				if ( data == null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Ed has no servants of type \"" + type + "\"." );
					return;
				}
				type = EdServantData.dataToType( data );
				// Switch servant
				EdServantData servant = EdServantData.findEdServant( type );
				if ( servant == null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You have not called forth a " + type + " to be your servant." );
					return;
				}

				KoLmafia.updateDisplay( "Putting your " + type + " to work..." );
				RequestThread.postRequest( new EdBaseRequest( "edbase_door", true ) );
				RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1053&option=1&sid=" + servant.getId() ) );
			}

			// Print your current servant
			StringBuilder output = new StringBuilder();
			this.printCurrentServant( output );
			return;
		}
	}

	private void printServants( final StringBuilder output )
	{
		output.setLength( 0 );

		output.append( "<table border=2 cols=4 cellpadding=5>" );
		output.append( "<tr>" );
		output.append( "<th>Type</th>" );
		output.append( "<th>Image</th>" );
		output.append( "<th>Name</th>" );
		output.append( "<th>Abilities</th>" );
		output.append( "</tr>" );

		for ( Object[] data : EdServantData.SERVANTS )
		{
			// Download the image
			String image = EdServantData.dataToImage( data );
			File file = FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + image );

			String type = EdServantData.dataToType( data );
			EdServantData servant = EdServantData.findEdServant( type );

			output.append( "<tr>" );

			output.append( "<td>" );
			output.append( type );
			output.append( "</td>" );

			output.append( "<td>" );
			if ( file != null )
			{
				output.append( "<img src=\"/images/itemimages/" );
				output.append( image );
				output.append( "\" alt = \"" );
				output.append( type );
				output.append( "\">" );
			}
			output.append( "</td>" );

			output.append( "<td>" );
			if ( servant == null )
			{
				output.append( "-" );
			}
			else
			{
				output.append( "<table border=0 cols=1 cellpadding=0>" );
				output.append( "<tr><td></td></tr>" );
				output.append( "<tr><td>" );
				output.append( servant.getName() );
				output.append( "</td></tr>" );
				output.append( "<tr><td>" );
				output.append( "(Level " );
				output.append( String.valueOf( servant.getLevel() ) );
				output.append( ", " );
				output.append( String.valueOf( servant.getExperience() ) );
				output.append( " XP)" );
				output.append( "</td></tr>" );
				output.append( "<tr><td></td></tr>" );
				output.append( "</table>" );
			}
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( "<table border=0 cols=1 cellpadding=0>" );
			output.append( "<tr style=\"text-align:left\"><td>" );
			output.append( "Level 1: " );
			output.append( EdServantData.dataToLevel1Ability( data ) );
			output.append( "</td></tr>" );
			output.append( "<tr style=\"text-align:left\"><td>" );
			output.append( "Level 7: " );
			output.append( EdServantData.dataToLevel7Ability( data ) );
			output.append( "</td></tr>" );
			output.append( "<tr style=\"text-align:left\"><td>" );
			output.append( "Level 14: " );
			output.append( EdServantData.dataToLevel14Ability( data ) );
			output.append( "</td></tr>" );
			output.append( "<tr style=\"text-align:left\"><td>" );
			output.append( "Level 21: " );
			output.append( EdServantData.dataToLevel21Ability( data ) );
			output.append( "</td></tr>" );
			output.append( "</table>" );
			output.append( "</td>" );

			output.append( "</tr>" );
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}

	private void printCurrentServant( final StringBuilder output )
	{
		output.setLength( 0 );

		EdServantData current = EdServantData.currentServant();
		if ( current == EdServantData.NO_SERVANT )
		{
			output.append( "You do not currently have an active servant" );
		}
		else
		{
			output.append( "Your current servant is " );
			output.append( current.toString() );
		}

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
