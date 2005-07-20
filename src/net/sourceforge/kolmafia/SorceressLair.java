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

	private static final AdventureResult SUGAR = new AdventureResult( "Sugar Rush", 0 );
	private static final AdventureResult RICE_CANDY = new AdventureResult( 540, 1 );
	private static final AdventureResult FARMER_CANDY = new AdventureResult( 617, 1 );
	private static final AdventureResult MARZIPAN = new AdventureResult( 1163, 1 );

	private static final AdventureResult WUSSINESS = new AdventureResult( "Wussiness", 0 );
	private static final AdventureResult WUSSY_POTION = new AdventureResult( 469, 1 );

	private static final AdventureResult MIASMA = new AdventureResult( "Rainy Soul Miasma", 0 );
	private static final AdventureResult BLACK_CANDLE = new AdventureResult( 620, 1 );

	private static final AdventureResult STAR_SWORD = new AdventureResult( 657, 1 );
	private static final AdventureResult STAR_CROSSBOW = new AdventureResult( 658, 1 );
	private static final AdventureResult STAR_STAFF = new AdventureResult( 659, 1 );
	private static final AdventureResult STAR_BUCKLER = new AdventureResult( 662, 1 );

	private static final AdventureResult ACOUSTIC_GUITAR = new AdventureResult( 404, 1 );
	private static final AdventureResult HEAVY_METAL_GUITAR = new AdventureResult( 507, 1 );

	private static final AdventureResult BONE_RATTLE = new AdventureResult( 168, 1 );
	private static final AdventureResult TAMBOURINE = new AdventureResult( 740, 1 );

	private static final AdventureResult ACCORDION = new AdventureResult( 11, 1 );
	private static final AdventureResult ROCKNROLL_LEGEND = new AdventureResult( 50, 1 );

	private static final AdventureResult CLOVER = new AdventureResult( 24, 1 );

	private static final AdventureResult DIGITAL = new AdventureResult( 691, 1 );
	private static final AdventureResult RICHARD = new AdventureResult( 665, 1 );
	private static final AdventureResult SKELETON = new AdventureResult( 642, 1 );

	private static final AdventureResult BORIS = new AdventureResult( 282, 1 );
	private static final AdventureResult JARLSBERG = new AdventureResult( 283, 1 );
	private static final AdventureResult SNEAKY_PETE = new AdventureResult( 284, 1 );


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

		KoLCharacter data = client.getCharacterData();

		if ( !checkPrerequisites() )
			return;

		List requirements = new ArrayList();

		// Make sure the character has some candy, or at least
		// the appropriate status effect.

		AdventureResult candy = MARZIPAN.getCount( client.getInventory() ) > 0 ? MARZIPAN :
			FARMER_CANDY.getCount( client.getInventory() ) > 0 ? FARMER_CANDY : RICE_CANDY;

		if ( !data.getEffects().contains( SUGAR ) )
			requirements.add( candy );

		// Other effect-gaining items, including the inherent
		// luckiness granted by the clover.

		if ( !data.getEffects().contains( WUSSINESS ) )
			requirements.add( WUSSY_POTION );

		if ( !data.getEffects().contains( MIASMA ) )
			requirements.add( BLACK_CANDLE );

		requirements.add( CLOVER );

		// Decide on which star weapon should be available for
		// this whole process.

		AdventureResult starWeapon = STAR_SWORD.getCount( client.getInventory() ) > 0 ? STAR_SWORD :
			STAR_CROSSBOW.getCount( client.getInventory() ) > 0 ? STAR_CROSSBOW : STAR_STAFF;

		boolean needsWeapon = !data.getEquipment( KoLCharacter.WEAPON ).startsWith( "star" );

		if ( needsWeapon )
			requirements.add( starWeapon );

		boolean needsBuckler = !data.getEquipment( KoLCharacter.ACCESSORY1 ).startsWith( "star" ) &&
			!data.getEquipment( KoLCharacter.ACCESSORY2 ).startsWith( "star" ) && !data.getEquipment( KoLCharacter.ACCESSORY3 ).startsWith( "star" );

		if ( needsBuckler )
			requirements.add( STAR_BUCKLER );

		// Now, add all the keys which are required for the entire
		// entryway quest.

		requirements.add( DIGITAL );
		requirements.add( RICHARD );
		requirements.add( SKELETON );

		requirements.add( BORIS );
		requirements.add( JARLSBERG );
		requirements.add( SNEAKY_PETE );

		// Next, figure out which instrument is needed for the final
		// stage of the entryway.

		requirements.add( HEAVY_METAL_GUITAR.getCount( client.getInventory() ) > 0 ? HEAVY_METAL_GUITAR : ACOUSTIC_GUITAR );
		requirements.add( TAMBOURINE.getCount( client.getInventory() ) > 0 ? TAMBOURINE : BONE_RATTLE );
		requirements.add( ROCKNROLL_LEGEND.getCount( client.getInventory() ) > 0 ? ROCKNROLL_LEGEND : ACCORDION );

		// It's possible that meat paste is also required, if the
		// person is not in a muscle sign.

		if ( !data.inMuscleSign() )
			requirements.add( new AdventureResult( ItemCreationRequest.MEAT_PASTE, 2 ) );

		// Now that the array's initialized, issue the checks
		// on the items needed to finish the entryway.

		AdventureResult [] requirementsArray = new AdventureResult[ requirements.size() ];
		requirements.toArray( requirementsArray );

		if ( !checkRequirements( requirementsArray ) )
			return;

		// Use the rice candy, wussiness potion, and black candle
		// and then cross through the first door.

		if ( !data.getEffects().contains( SUGAR ) )
			(new ConsumeItemRequest( client, candy )).run();

		if ( !data.getEffects().contains( WUSSINESS ) )
			(new ConsumeItemRequest( client, WUSSY_POTION )).run();

		if ( !data.getEffects().contains( MIASMA ) )
			(new ConsumeItemRequest( client, BLACK_CANDLE )).run();

		client.updateDisplay( DISABLED_STATE, "Crossing three door puzzle..." );

		KoLRequest request = new KoLRequest( client, "lair1.php" );
		request.addFormField( "action", "gates" );
		request.run();

		// Now, unequip all of your equipment and cross through
		// the mirror.  Process the mirror shard that results.

		(new EquipmentRequest( client, SpecialOutfit.BIRTHDAY_SUIT )).run();

		client.updateDisplay( DISABLED_STATE, "Crossing mirror puzzle..." );

		request = new KoLRequest( client, "lair1.php" );
		request.addFormField( "action", "mirror" );
		request.run();

		client.processResult( new AdventureResult( 726, 1 ) );

		// Now handle the form for the digital key to get
		// the Squeezings of Woe.

		client.updateDisplay( DISABLED_STATE, "Inserting digital key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( DIGITAL.getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "prepreaction", "sequence" );
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

		if ( needsWeapon )
			(new EquipmentRequest( client, starWeapon.getName() )).run();

		if ( needsBuckler )
			(new EquipmentRequest( client, STAR_BUCKLER.getName() )).run();

		if ( !data.getFamiliars().getSelectedItem().toString().startsWith( "Star" ) )
			(new FamiliarRequest( client, new FamiliarData( 17 ) )).run();

		client.updateDisplay( DISABLED_STATE, "Inserting Richard's star key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( RICHARD.getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "prepreaction", "starcage" );
		request.run();

		// Next, handle the form for the skeleton key to
		// get the Really Evil Rhythm.  This uses up the
		// clover you had, so process it.

		client.updateDisplay( DISABLED_STATE, "Inserting skeleton key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( SKELETON.getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "prepreaction", "skel" );
		request.run();

		client.processResult( CLOVER.getNegation() );

		// Next, handle the three hero keys, which involve
		// answering the riddles with the forms of fish.

		client.updateDisplay( DISABLED_STATE, "Inserting Boris's key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( BORIS.getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "prepreaction", "sorcriddle1" );
		request.addFormField( "answer", "fish" );
		request.run();

		client.updateDisplay( DISABLED_STATE, "Inserting Jarlsberg's key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( JARLSBERG.getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "prepreaction", "sorcriddle2" );
		request.addFormField( "answer", "phish" );
		request.run();

		client.updateDisplay( DISABLED_STATE, "Inserting Sneaky Pete's key..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "preaction", "key" );
		request.addFormField( "whichkey", String.valueOf( SNEAKY_PETE.getItemID() ) );
		request.run();

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "prepreaction", "sorcriddle3" );
		request.addFormField( "answer", "fsh" );
		request.run();

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
		responseText = rotateHedgePiece( responseText, "8", "form8.submit();\"><img alt=\"90 degree bend, exits south and west.\"" );

		// The hedge maze has been properly rotated!  Now go ahead
		// and retrieve the key from the maze.

		KoLRequest request = new KoLRequest( client, "lair3.php" );
		request.addFormField( "action", "hedge" );
		request.run();

		// Add key to inventory
		client.processResult( new AdventureResult( 728, 1 ) );

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
