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

public class SorceressLair implements KoLConstants
{
	private static KoLmafia client;
	private static List missingItems;

	public static void setClient( KoLmafia client )
	{
		SorceressLair.client = client;
		missingItems = new ArrayList();
	}

	public static List getMissingItems()
	{	return missingItems;
	}

	private static boolean checkPrerequisites()
	{
		// If the client has not yet been set, then there is
		// no entryway to complete.

		if ( client == null )
			return false;

		// If the player has never ascended, then they're going
		// to have to do it all by hand.

		if ( client.getCharacterData().getSignStat() == KoLCharacter.NONE )
		{
			client.updateDisplay( ERROR_STATE, "Sorry, you've never ascended." );
			client.cancelRequest();
			return false;
		}

		// Otherwise, they've passed all the standard checks
		// on prerequisites.  Return true.

		return true;
	}

	private static boolean checkRequirements( AdventureResult [] requirements )
	{
		missingItems.clear();

		// First, check the standard item requirements
		// for this accomplishment.

		for ( int i = 0; i < requirements.length; ++i )
			if ( requirements[i] == null || requirements[i].getCount( client.getInventory() ) < requirements[i].getCount() )
				missingItems.add( requirements[i] );

		// If there are any missing requirements
		// be sure to return false.

		if ( !missingItems.isEmpty() )
		{
			client.updateDisplay( ERROR_STATE, "Insufficient items to continue." );
			client.cancelRequest();
		}

		return missingItems.isEmpty();
	}

	public static void completeEntryway()
	{
		if ( !checkPrerequisites() )
			return;

		// Make sure the character has some candy

		AdventureResult rice = new AdventureResult( 540, 1 );
		AdventureResult farmer = new AdventureResult( 617, 1 );
		AdventureResult marzipan = new AdventureResult( 1163, 1 );
		AdventureResult candy = marzipan.getCount( client.getInventory() ) > 0 ? marzipan :
			farmer.getCount( client.getInventory() ) > 0 ? farmer : rice;

		// Decide on which star weapon should be available for
		// this whole process.

		AdventureResult starSword = new AdventureResult( 657, 1 );
		AdventureResult starCrossbow = new AdventureResult( 658, 1 );
		AdventureResult starStaff = new AdventureResult( 659, 1 );

		AdventureResult starWeapon = starSword.getCount( client.getInventory() ) > 0 ? starSword :
			starCrossbow.getCount( client.getInventory() ) > 0 ? starCrossbow : starStaff;

		// Next, figure out which instrument is needed for each
		// step of the process.

		AdventureResult acoustic = new AdventureResult( 404, 1 );
		AdventureResult heavyMetal = new AdventureResult( 507, 1 );
		AdventureResult strummingInstrument = heavyMetal.getCount( client.getInventory() ) > 0 ? heavyMetal : acoustic;

		AdventureResult rattle = new AdventureResult( 168, 1 );
		AdventureResult tambourine = new AdventureResult( 740, 1 );
		AdventureResult percussionInstrument = tambourine.getCount( client.getInventory() ) > 0 ? tambourine : rattle;

		AdventureResult accordion = new AdventureResult( 11, 1 );
		AdventureResult legend = new AdventureResult( 50, 1 );
		AdventureResult squeezingInstrument = legend.getCount( client.getInventory() ) > 0 ? legend : accordion;

		// Now, compile a list of items which need to be checked;
		// if you've already ascended, you're guaranteed to have
		// a starfish available, so no need to check for it.

		AdventureResult [] requirements = new AdventureResult[16];

		requirements[0] = candy;                          // candy
		requirements[1] = new AdventureResult( 469, 1 );  // wussiness potion
		requirements[2] = new AdventureResult( 620, 1 );  // thin black candle

		requirements[3] = new AdventureResult( 24, 1 );   // ten-leaf clover
		requirements[4] = starWeapon;                     // star weapon (previously determined)
		requirements[5] = new AdventureResult( 662, 1 );  // star buckler

		requirements[6] = new AdventureResult( 691, 1 );  // digital key
		requirements[7] = new AdventureResult( 665, 1 );  // Richard's star key
		requirements[8] = new AdventureResult( 642, 1 );  // skeleton key

		requirements[9] = new AdventureResult( 282, 1 );  // Boris's key
		requirements[10] = new AdventureResult( 283, 1 ); // Jarlsberg's key
		requirements[11] = new AdventureResult( 284, 1 ); // Sneaky Pete's key

		requirements[12] = squeezingInstrument;           // squeezing instrument
		requirements[13] = strummingInstrument;           // strumming instrument
		requirements[14] = percussionInstrument;          // percussion instrument

		requirements[15] = new AdventureResult( ItemCreationRequest.MEAT_PASTE,
			client.getCharacterData().inMuscleSign() ? 0 : 2 );

		// Now that the array's initialized, issue the checks
		// on the items needed to finish the entryway.

		if ( !checkRequirements( requirements ) )
			return;

		// Use the rice candy, wussiness potion, and black candle
		// and then cross through the first door.

		for ( int i = 0; i < 3; ++i )
			(new ConsumeItemRequest( client, requirements[i] )).run();

		client.updateDisplay( DISABLED_STATE, "Crossing three door puzzle..." );

		KoLRequest request = new KoLRequest( client, "lair.php" );
		request.addFormField( "action", "gates" );
		request.run();

		// Now, unequip all of your equipment and cross through
		// the mirror.  Process the mirror shard that results.

		(new EquipmentRequest( client, EquipmentRequest.UNEQUIP_ALL )).run();

		client.updateDisplay( DISABLED_STATE, "Crossing mirror puzzle..." );

		request = new KoLRequest( client, "lair.php" );
		request.addFormField( "action", "mirror" );
		request.run();

		AdventureResult mirrorShard = new AdventureResult( 726, 1 );
		client.processResult( mirrorShard );

		// Now handle the form for the digital key to get
		// the Squeezings of Woe.

		client.updateDisplay( DISABLED_STATE, "Inserting digital key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( requirements[6].getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "sequence" );
		request.addFormField( "seq1", "up" );  request.addFormField( "seq2", "up" );
		request.addFormField( "seq3", "down" );  request.addFormField( "seq4", "down" );
		request.addFormField( "seq5", "left" );  request.addFormField( "seq6", "right" );
		request.addFormField( "seq7", "left" );  request.addFormField( "seq8", "right" );
		request.addFormField( "seq9", "b" );  request.addFormField( "seq10", "a" );
		request.run();

		// Now handle the form for the star key to get
		// the Sinister Strumming.  Note that this will
		// require you to re-equip your star weapon and
		// a star buckler and switch to a starfish first.

		(new EquipmentRequest( client, requirements[4].getName() )).run();
		(new EquipmentRequest( client, requirements[5].getName() )).run();
		(new FamiliarRequest( client, new FamiliarData( 17 ) )).run();

		client.updateDisplay( DISABLED_STATE, "Inserting Richard's star key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( requirements[7].getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "starcage" );
		request.run();

		// Next, handle the form for the skeleton key to
		// get the Really Evil Rhythm.  This uses up the
		// clover you had, so process it.

		client.updateDisplay( DISABLED_STATE, "Inserting skeleton key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( requirements[8].getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "skel" );
		request.run();

		client.processResult( requirements[3].getNegation() );

		// Next, handle the three hero keys, which involve
		// answering the riddles with the forms of fish.

		for ( int i = 9; i < 12; ++i )
		{
			client.updateDisplay( DISABLED_STATE, "Inserting " + requirements[i].getName() + "..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( requirements[i].getItemID() ) );
			request.run();

			request = new KoLRequest( client, "lair2.php" );

			if ( i == 9 )
			{
				request.addFormField( "preaction", "sorcriddle1" );
				request.addFormField( "answer", "fish" );
			}
			else if ( i == 10 )
			{
				request.addFormField( "preaction", "sorcriddle1" );
				request.addFormField( "answer", "phish" );
			}
			else
			{
				request.addFormField( "preaction", "sorcriddle1" );
				request.addFormField( "answer", "fsh" );
			}

			request.run();
		}

		// Next, issue combine requests on the makeshift
		// scuba gear components and then equip the gear.

		client.processResult( new AdventureResult( 729, 1 ) );
		client.processResult( new AdventureResult( 730, 1 ) );
		client.processResult( new AdventureResult( 731, 1 ) );

		ItemCreationRequest.getInstance( client, 734, 1 ).run();
		(new EquipmentRequest( client, "makeshift SCUBA gear" )).run();

		// Now, press the switch beyond the odor by
		// visiting the appropriate page.

		client.updateDisplay( DISABLED_STATE, "Pressing switch beyond odor..." );
		(new KoLRequest( client, "lair2.php?action=odor" )).run();

		// Finally, arm the stone mariachis with their
		// appropriate instruments.

		client.updateDisplay( DISABLED_STATE, "Arming stone mariachis..." );
		(new KoLRequest( client, "lair2.php?action=statues" )).run();

		// Because this has never been tested, just
		// enable the display and pretend that the
		// process is now complete.

		client.updateDisplay( ENABLED_STATE, "Sorceress entryway complete.  Maybe." );
	}

	public static void completeHedgeMaze()
	{
		if ( !checkPrerequisites() )
			return;

		// Check to see if you've run out of puzzle pieces.
		// If you have, don't bother running the puzzle.

		AdventureResult [] requirements = new AdventureResult[1];
		requirements[0] = new AdventureResult( 727, 1 );

		if ( !checkRequirements( requirements ) )
			return;

		// Otherwise, check their current state relative
		// to the hedge maze, and begin!

		client.updateDisplay( DISABLED_STATE, "Retrieving maze status..." );
		KoLRequest request = new KoLRequest( client, "hedgepuzzle.php" );
		request.run();

		String responseText = request.responseText;

		// First mission -- retrieve the key from the hedge
		// maze puzzle.

		client.updateDisplay( DISABLED_STATE, "Retrieving hedge key..." );
		responseText = retrieveHedgeKey( responseText );

		// Second mission -- rotate the hedge maze until
		// the hedge path leads to the hedge door.

		client.updateDisplay( DISABLED_STATE, "Executing final rotations..." );
		responseText = finalizeHedgeMaze( responseText );

		// Check to see if you ran out of puzzle pieces
		// in the middle -- if you did, update the user
		// display to say so.

		if ( responseText.indexOf( "Click one" ) == -1 )
		{
			client.updateDisplay( ERROR_STATE, "Ran out of puzzle pieces." );
			missingItems.add( requirements[0] );
			client.cancelRequest();
			return;
		}

		client.updateDisplay( ENABLED_STATE, "Hedge maze quest complete." );
	}

	private static String rotateHedgePiece( String responseText, String hedgePiece, String searchText )
	{
		KoLRequest request;

		while ( responseText.indexOf( searchText ) == -1 )
		{
			// If the topiary golem stole one of your hedge
			// pieces, then make sure you have another before
			// continuing.

			if ( responseText.indexOf( "Click one" ) == -1 )
			{
				AdventureResult puzzlePiece = new AdventureResult( 727, -1 );
				int puzzlePieceCount = puzzlePiece.getCount( client.getInventory() );

				// Reduce your hedge piece count by one; if
				// it turns out that you've run out of puzzle
				// pieces, return the original response text

				if ( puzzlePieceCount > 0 )
					client.processResult( puzzlePiece );

				// If you've run out of hedge puzzle pieces,
				// return the original response text.

				if ( puzzlePieceCount < 2 )
					return responseText;
			}

			request = new KoLRequest( client, "hedgepuzzle.php" );
			request.addFormField( "action", hedgePiece );
			request.run();

			responseText = request.responseText;
		}

		return responseText;
	}

	private static String retrieveHedgeKey( String responseText )
	{
		// Before doing anything, check to see if the hedge
		// maze has already been solved for the key.

		if ( responseText.indexOf( "There is a key here." ) == -1 )
			return responseText;

		responseText = rotateHedgePiece( responseText, "1", "form1.submit();\"><img alt=\"90 degree bend, exits south and east.\"" );
		responseText = rotateHedgePiece( responseText, "2", "form2.submit();\"><img alt=\"Straight east/west passage.\"" );
		responseText = rotateHedgePiece( responseText, "3", "form3.submit();\"><img alt=\"Dead end, exit to the west.  There is a key here.\"" );
		responseText = rotateHedgePiece( responseText, "4", "form4.submit();\"><img alt=\"Straight north/south passage.\"" );
		responseText = rotateHedgePiece( responseText, "7", "form7.submit();\"><img alt=\"90 degree bend, exits north and east.\"" );
		responseText = rotateHedgePiece( responseText, "8", "form8.submit();\"><img alt=\"90 degree bend, exits south and east.\"" );

		// The hedge maze has been properly rotated!  Now go ahead
		// and retrieve the key from the maze.

		KoLRequest request = new KoLRequest( client, "lair3.php" );
		request.addFormField( "action", "hedge" );
		request.run();

		return responseText;
	}

	private static String finalizeHedgeMaze( String responseText )
	{
		responseText = rotateHedgePiece( responseText, "2", "form2.submit();\"><img alt=\"Straight north/south passage.\"" );
		responseText = rotateHedgePiece( responseText, "5", "form5.submit();\"><img alt=\"90 degree bend, exits north and east.\"" );
		responseText = rotateHedgePiece( responseText, "6", "form6.submit();\"><img alt=\"90 degree bend, exits south and west.\"" );
		responseText = rotateHedgePiece( responseText, "9", "form9.submit();\"><img alt=\"90 degree bend, exits north and west.\"" );
		responseText = rotateHedgePiece( responseText, "8", "form8.submit();\"><img alt=\"90 degree bend, exits south and east.\"" );

		// The hedge maze has been properly rotated!  Now go ahead
		// and complete the hedge maze puzzle!

		KoLRequest request = new KoLRequest( client, "lair3.php" );
		request.addFormField( "action", "hedge" );
		request.run();

		return responseText;
	}
}
