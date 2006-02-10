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
import java.util.ArrayList;

public class UntinkerRequest extends KoLRequest
{
	private static final AdventureResult SCREWDRIVER = new AdventureResult( 454, -1 );

	private int itemID;

	public UntinkerRequest( KoLmafia client )
	{
		super( client, "town_right.php" );
		addFormField( "place", "untinker" );

		this.itemID = -1;
	}

	public UntinkerRequest( KoLmafia client, int itemID )
	{
		super( client, "town_right.php" );

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action", "untinker" );
		addFormField( "whichitem", String.valueOf( itemID ) );
		addFormField( "untinker", "Untinker!" );

		this.itemID = itemID;
	}

	public void run()
	{
		// If we are not untinkering an item, then the user wants to
		// visit the untinker and get or fulfill the quest

		if ( itemID == -1 )
		{
			updateDisplay( NORMAL_STATE, "Visiting the Untinker..." );
			super.run();
			updateDisplay( NORMAL_STATE, "Back from Untinker." );
			return;
		}

		// Check to see if the item can be constructed using meat
		// paste, and only execute the request if it is known to be
		// creatable through combination.

		if ( ConcoctionsDatabase.getMixingMethod( itemID ) != ItemCreationRequest.COMBINE )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "You cannot untinker that item." );
			return;
		}

		// Check to see if the person has the untinkering accomplishment
		// before starting.

		if ( KoLCharacter.getLevel() < 4 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "You cannot untinker items yet." );
		}

		// If the person does not have the accomplishment, visit
		// the untinker to ensure that they get the quest.

		KoLRequest questCompleter = new UntinkerRequest( client );
		questCompleter.run();

		// If they do not have a screwdriver, tell them they
		// need to complete the untinker quest.

		if ( responseText.indexOf( "action" ) == -1 && !KoLCharacter.getInventory().contains( SCREWDRIVER ) )
		{
			// If the are in a muscle sign, this is a trivial task;
			// just have them visit Innabox.

			if ( KoLCharacter.inMuscleSign() )
			{
				KoLRequest retrieve = new KoLRequest( client, "knoll.php?place=smith", true );
				retrieve.run();
			}
			else
			{
				// Okay, so they don't have one yet. Complete the
				// untinkerer's quest automatically.

				ArrayList temporary = new ArrayList();
				temporary.addAll( client.getConditions() );

				client.getConditions().clear();
				client.getConditions().add( SCREWDRIVER.getNegation() );

				KoLAdventure adventure = AdventureDatabase.getAdventure( "degrassi" );
				client.makeRequest( adventure, KoLCharacter.getAdventuresLeft() );

				if ( !client.getConditions().isEmpty() )
				{
					updateDisplay( ERROR_STATE, "Unable to complete untinkerer's quest." );
					client.getConditions().clear();
					client.getConditions().addAll( temporary );
					return;
				}

				client.getConditions().clear();
				client.getConditions().addAll( temporary );
			}

			// You should now have a screwdriver in your inventory.
			// Go ahead and rerun the untinker request and you will
			// have the needed accomplishment.

			questCompleter.run();
		}

		// Visiting the untinker automatically deducts a
		// screwdriver from the inventory.

		KoLCharacter.processResult( SCREWDRIVER );
		updateDisplay( NORMAL_STATE, "Untinkering an item..." );

		super.run();

		client.processResult( new AdventureResult( itemID, -1 ) );
		updateDisplay( NORMAL_STATE, "Untinkering complete" );
	}

	protected void processResults()
	{
		if ( itemID != AdventureRequest.ABRIDGED.getItemID() )
		{
			super.processResults();
			return;
		}

		KoLCharacter.processResult( AdventureRequest.ABRIDGED );
		KoLCharacter.processResult( FightRequest.DICTIONARY1 );
		KoLCharacter.processResult( AdventureRequest.BRIDGE.getNegation() );
	}
}
