/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;
import java.util.List;
import java.util.ArrayList;

public abstract class Nemesis extends StaticEntity
{
	// Items for the cave

	private static final AdventureResult FLY_SWATTER = new AdventureResult( 123, 1 );
	private static final AdventureResult COG = new AdventureResult( 120, 1 );
	private static final AdventureResult SPROCKET = new AdventureResult( 119, 1 );
	private static final AdventureResult GRAVY = new AdventureResult( 80, 1 );
	private static final AdventureResult TONGS = new AdventureResult( 36, 1 );
	private static final AdventureResult KETCHUP = new AdventureResult( 106, 1 );
	private static final AdventureResult CATSUP = new AdventureResult( 107, 1 );

	private static boolean checkPrerequisites()
	{
		if ( KoLCharacter.isFallingDown() )
			return false;

		KoLRequest request;

		// If the getClient() has not yet been set, then there is no cave

		KoLmafia.updateDisplay( "Checking prerequisites..." );

		// Make sure the player has been given the quest

		request = new KoLRequest( getClient(), "mountains.php", true );
		request.run();

		if ( request.responseText.indexOf( "cave.php" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You haven't been given the quest to defeat your Nemesis!" );
			return false;
		}

		return true;
	}

	public static void faceNemesis()
	{
		// Make sure the player is qualified to use this script

		if ( !checkPrerequisites() )
			return;

		// See how far the player has gotten in this quest

		KoLRequest request = new KoLRequest( getClient(), "cave.php", true );
		request.run();

		if ( request.responseText == null )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Unable to find quest." );
			return;
		}

		int region = 0;

		if ( request.responseText.indexOf( "value='flies'" ) != -1 )
			region = 4;
		else if ( request.responseText.indexOf( "value='door1'" ) != -1 )
			region = 5;
		else if ( request.responseText.indexOf( "value='troll1'" ) != -1 )
			region = 6;
		else if ( request.responseText.indexOf( "value='door2'" ) != -1 )
			region = 7;
		else if ( request.responseText.indexOf( "value='troll2'" ) != -1 )
			region = 8;
		else if ( request.responseText.indexOf( "value='end'" ) != -1 )
			region = 9;
		else if ( request.responseText.indexOf( "cave9done" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You've already defeated your nemesis." );
			return;
		}

		List requirements = new ArrayList();

		// Need a flyswatter to get past the Fly Bend

		if ( region <= 4 )
		{
			if ( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).indexOf( "fly" ) == -1 )
				requirements.add( FLY_SWATTER );
		}

		// Need a cog and a sprocket to get past the Stone Door

		if ( region <= 5 )
		{
			requirements.add( COG );
			requirements.add( SPROCKET );
		}

		// Need fairy gravy to get past the first lavatory troll

		if ( region <= 6 )
		{
			requirements.add( GRAVY );
		}

		// Need tongs to get past the salad covered door

		if ( region <= 7 )
		{
			if ( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).indexOf( "tongs" ) == -1 )
				requirements.add( TONGS );
		}

		// Need some kind of ketchup to get past the second lavatory troll

		AdventureResult ketchup = CATSUP.getCount( KoLCharacter.getInventory() ) > 0 ? CATSUP : KETCHUP;

		if ( region <= 8 )
			requirements.add( ketchup );

		if ( !getClient().checkRequirements( requirements ) )
			return;

		// Get current equipment
		String initialWeapon = KoLCharacter.getCurrentEquipmentName( KoLCharacter.WEAPON );
		String initialOffhand = KoLCharacter.getCurrentEquipmentName( KoLCharacter.OFFHAND );

		if ( initialWeapon == null )
			initialWeapon = "";

		if ( initialOffhand == null )
			initialOffhand = "";

		// Pass the obstacles one at a time.

		for ( int i = region; i <= 9; i++ )
		{
			String action = "none";

			switch (i)
			{
				case 4: // The Fly Bend

					// Equip fly swatter, but only if it's
					// not currently equipped

					DEFAULT_SHELL.executeLine( "equip Gnollish flyswatter" );
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

				case 7:	// Salad-Covered Door

					DEFAULT_SHELL.executeLine( "equip Knob Goblin tongs" );
					action = "door2";
					KoLmafia.updateDisplay( "Plucking the salad door..." );
					break;

				case 8: // Lavatory Troll 2

					action = "troll2";
					KoLmafia.updateDisplay( "Feeding the second troll..." );
					break;

				case 9: // Chamber of Epic Conflict

					if ( initialWeapon != null )
						DEFAULT_SHELL.executeLine( "equip weapon " + initialWeapon );

					if ( initialOffhand != null )
						DEFAULT_SHELL.executeLine( "equip off-hand " + initialOffhand );

					action = "end";
					KoLmafia.updateDisplay( "Fighting your nemesis..." );
					break;
			}

			// Visit the cave
			request = new KoLRequest( getClient(), "cave.php?action=" + action );
			request.run();

			if ( request.responseText != null && request.responseText.indexOf( "You must have at least one Adventure left to fight your nemesis." ) != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You're out of adventures." );
				return;
			}

			// Consume items
			switch (i)
			{
				case 5: // A Stone Door

					// Use up cog & sprocket
					getClient().processResult( COG.getNegation() );
					getClient().processResult( SPROCKET.getNegation() );
					break;

				case 6: // Lavatory Troll 1

					// Use up fairy gravy
					getClient().processResult( GRAVY.getNegation() );
					break;

				case 8: // Lavatory Troll 2

					// Use up ketchup
					getClient().processResult( ketchup.getNegation() );
					break;
			}
		}

		if ( getClient().getCurrentRequest() != null && getClient().getCurrentRequest().responseText != null &&
			getClient().getCurrentRequest().responseText.indexOf( "WINWINWIN" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "KoLmafia was unable to defeat your nemesis." );
			return;
		}

		KoLmafia.updateDisplay( "You defeated your nemesis. Congratulations!" );
	}
}
