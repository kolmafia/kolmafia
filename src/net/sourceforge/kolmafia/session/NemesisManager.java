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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public abstract class NemesisManager
	extends StaticEntity
{
	private static final GenericRequest QUEST_HANDLER = new GenericRequest( "" );

	// Items for the cave

	private static final AdventureResult FLY_SWATTER = new AdventureResult( 123, 1 );
	private static final AdventureResult COG = new AdventureResult( 120, 1 );
	private static final AdventureResult SPROCKET = new AdventureResult( 119, 1 );
	private static final AdventureResult GRAVY = new AdventureResult( 80, 1 );
	private static final AdventureResult TONGS = new AdventureResult( 36, 1 );
	private static final AdventureResult KETCHUP = new AdventureResult( 106, 1 );
	private static final AdventureResult CATSUP = new AdventureResult( 107, 1 );

	private static final boolean checkPrerequisites()
	{
		if ( KoLCharacter.isFallingDown() )
		{
			return false;
		}

		// If thehas not yet been set, then there is no cave

		KoLmafia.updateDisplay( "Checking prerequisites..." );

		// Make sure the player has been given the quest

		NemesisManager.QUEST_HANDLER.constructURLString( "mountains.php" );
		RequestThread.postRequest( NemesisManager.QUEST_HANDLER );

		if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "cave.php" ) == -1 )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "You haven't been given the quest to defeat your NemesisManager!" );
			return false;
		}

		return true;
	}

	public static final void faceNemesisManager()
	{
		// Make sure the player is qualified to use this script

		if ( !NemesisManager.checkPrerequisites() )
		{
			return;
		}

		// See how far the player has gotten in this quest

		NemesisManager.QUEST_HANDLER.clearDataFields();
		RequestThread.postRequest( NemesisManager.QUEST_HANDLER );

		if ( NemesisManager.QUEST_HANDLER.responseText == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find quest." );
			return;
		}

		int region = 0;

		if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "value='flies'" ) != -1 )
		{
			region = 4;
		}
		else if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "value='door1'" ) != -1 )
		{
			region = 5;
		}
		else if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "value='troll1'" ) != -1 )
		{
			region = 6;
		}
		else if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "value='door2'" ) != -1 )
		{
			region = 7;
		}
		else if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "value='troll2'" ) != -1 )
		{
			region = 8;
		}
		else if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "value='end'" ) != -1 )
		{
			region = 9;
		}
		else if ( NemesisManager.QUEST_HANDLER.responseText.indexOf( "cave9done" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You've already defeated your nemesis." );
			return;
		}

		List requirements = new ArrayList();

		// Need a flyswatter to get past the Fly Bend

		if ( region <= 4 )
		{
			if ( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getItemId() != NemesisManager.FLY_SWATTER.getItemId() )
			{
				requirements.add( NemesisManager.FLY_SWATTER );
			}
		}

		// Need a cog and a sprocket to get past the Stone Door

		if ( region <= 5 )
		{
			requirements.add( NemesisManager.COG );
			requirements.add( NemesisManager.SPROCKET );
		}

		// Need fairy gravy to get past the first lavatory troll

		if ( region <= 6 )
		{
			requirements.add( NemesisManager.GRAVY );
		}

		// Need tongs to get past the salad covered door

		if ( region <= 7 )
		{
			if ( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getItemId() != NemesisManager.TONGS.getItemId() )
			{
				requirements.add( NemesisManager.TONGS );
			}
		}

		// Need some kind of ketchup to get past the second lavatory troll

		AdventureResult ketchup =
			NemesisManager.CATSUP.getCount( KoLConstants.inventory ) > 0 ? NemesisManager.CATSUP : NemesisManager.KETCHUP;

		if ( region <= 8 )
		{
			requirements.add( ketchup );
		}

		if ( !KoLmafia.checkRequirements( requirements ) )
		{
			return;
		}

		// Get current equipment
		AdventureResult initialWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		AdventureResult initialOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );

		if ( initialWeapon == null )
		{
			initialWeapon = EquipmentRequest.UNEQUIP;
		}

		if ( initialOffhand == null )
		{
			initialOffhand = EquipmentRequest.UNEQUIP;
		}

		// Pass the obstacles one at a time.

		for ( int i = region; i <= 9; i++ )
		{
			String action = "none";

			switch ( i )
			{
			case 4: // The Fly Bend

				// Equip fly swatter, but only if it's
				// not currently equipped

				RequestThread.postRequest( new EquipmentRequest( NemesisManager.FLY_SWATTER, KoLCharacter.WEAPON ) );
				action = "flies";
				KoLmafia.updateDisplay( "Swatting flies..." );
				break;

			case 5: // A Stone Door

				action = "door1";
				KoLmafia.updateDisplay( "Activating the stone door..." );
				break;

			case 6: // Lavatory Troll 1

				action = "troll1";
				KoLmafia.updateDisplay( "Feeding the first troll..." );
				break;

			case 7: // Salad-Covered Door

				RequestThread.postRequest( new EquipmentRequest( NemesisManager.TONGS, KoLCharacter.WEAPON ) );
				action = "door2";
				KoLmafia.updateDisplay( "Plucking the salad door..." );
				break;

			case 8: // Lavatory Troll 2

				action = "troll2";
				KoLmafia.updateDisplay( "Feeding the second troll..." );
				break;

			case 9: // Chamber of Epic Conflict

				if ( initialWeapon != null )
				{
					RequestThread.postRequest( new EquipmentRequest( initialWeapon, KoLCharacter.WEAPON ) );
				}

				if ( initialOffhand != null )
				{
					RequestThread.postRequest( new EquipmentRequest( initialOffhand, KoLCharacter.OFFHAND ) );
				}

				action = "end";
				KoLmafia.updateDisplay( "Fighting your nemesis..." );
				break;
			}

			// Visit the cave

			NemesisManager.QUEST_HANDLER.clearDataFields();
			NemesisManager.QUEST_HANDLER.addFormField( "action", action );
			RequestThread.postRequest( NemesisManager.QUEST_HANDLER );

			if ( NemesisManager.QUEST_HANDLER.responseText != null && NemesisManager.QUEST_HANDLER.responseText.indexOf( "You must have at least one Adventure left to fight your nemesis." ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're out of adventures." );
				return;
			}

			// Consume items
			switch ( i )
			{
			case 5: // A Stone Door

				// Use up cog & sprocket
				StaticEntity.getClient().processResult( NemesisManager.COG.getNegation() );
				StaticEntity.getClient().processResult( NemesisManager.SPROCKET.getNegation() );
				break;

			case 6: // Lavatory Troll 1

				// Use up fairy gravy
				StaticEntity.getClient().processResult( NemesisManager.GRAVY.getNegation() );
				break;

			case 8: // Lavatory Troll 2

				// Use up ketchup
				StaticEntity.getClient().processResult( ketchup.getNegation() );
				break;
			}
		}
	}
}
