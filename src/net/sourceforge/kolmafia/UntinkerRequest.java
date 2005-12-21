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
			updateDisplay( DISABLE_STATE, "Visiting the Untinker..." );
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

		if ( !KoLCharacter.hasAccomplishment( KoLCharacter.UNTINKER ) )
		{
			// If the person does not have the accomplishment, visit
			// the untinker to ensure that they get the quest.

			UntinkerRequest request = new UntinkerRequest( client );
			request.run();

			// If they do not have a screwdriver, tell them they
			// need to complete the untinker quest.

			if ( !KoLCharacter.getInventory().contains( SCREWDRIVER ) )
			{
				client.cancelRequest();
				client.updateDisplay( ERROR_STATE, "You have not completed the rusty quest." );
				return;
			}

			// Visiting the untinker automatically deducts a
			// screwdriver from the inventory.

			KoLCharacter.addAccomplishment( KoLCharacter.UNTINKER );
			KoLCharacter.processResult( SCREWDRIVER );
		}

		updateDisplay( DISABLE_STATE, "Untinkering an item..." );

		super.run();

		client.processResult( new AdventureResult( itemID, -1 ) );
		updateDisplay( NORMAL_STATE, "Untinkering complete" );
	}
}
