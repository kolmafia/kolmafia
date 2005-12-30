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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public abstract class SorceressLair extends StaticEntity
{
	// Items for the entryway

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

	private static final AdventureResult STONE_BANJO = new AdventureResult( 53, 1 );
	private static final AdventureResult DISCO_BANJO = new AdventureResult( 54, 1 );
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
	private static final AdventureResult BALLOON = new AdventureResult( 436, 1 );

        // Results of key puzzles
	private static final AdventureResult STRUMMING = new AdventureResult( 736, 1 );
	private static final AdventureResult SQUEEZINGS = new AdventureResult( 737, 1 );
	private static final AdventureResult RHYTHM = new AdventureResult( 738, 1 );
	private static final AdventureResult SCUBA = new AdventureResult( 734, 1 );

	// Items for the hedge maze

	private static final AdventureResult PUZZLE_PIECE = new AdventureResult( 727, 1 );
	private static final AdventureResult HEDGE_KEY = new AdventureResult( 728, 1 );

	// Guardians and the items that defeat them

	private static final String [][] GUARDIAN_DATA =
	{
		{ "Beer Batter", "baseball" }, { "Vicious Easel", "disease" }, { "Enraged Cow", "barbed-wire fence" },
		{ "Fickle Finger of F8", "razor-sharp can lid" }, { "Big Meat Golem", "meat vortex" }, { "Flaming Samurai", "frigid ninja stars" },
		{ "Tyrannosaurus Tex", "chaos butterfly" }, { "Giant Desktop Globe", "NG" }, { "Electron Submarine", "photoprotoneutron torpedo" },
		{ "Bowling Cricket", "sonar-in-a-biscuit" }, { "Ice Cube", "can of hair spray" }, { "Pretty Fly", "spider web" }
	};

	// Items for the Sorceress's Chamber
	private static final AdventureResult SHARD = new AdventureResult( 726, 1 );
	private static final AdventureResult RED_PIXEL_POTION = new AdventureResult( 464, 1 );

	// Familiars and the familiars that defeat them

	private static final String [][] FAMILIAR_DATA =
	{
		{ "sabre-toothed lime", "Levitating Potato" },
		{ "mosquito", "Sabre-Toothed Lime" },
		{ "barrrnacle", "Angry Goat" },
		{ "goat", "Mosquito" },
		{ "potato", "Barrrnacle" }
	};

	private static boolean checkPrerequisites( int min, int max )
	{
		KoLRequest request;

		// If the client has not yet been set, then there is
		// no entryway to complete.

		if ( client == null )
			return false;

		client.updateDisplay( DISABLE_STATE, "Checking prerequisites..." );

		// If the player has never ascended, then they're going
		// to have to do it all by hand.

		if ( KoLCharacter.getAscensions() < 1 )
		{
			client.updateDisplay( ERROR_STATE, "Sorry, you've never ascended." );
			client.cancelRequest();
			return false;
		}

		// Make sure he's been given the quest

		request = new KoLRequest( client, "main.php", true );
		request.run();

		if ( request.responseText.indexOf( "lair.php" ) == -1 )
		{
			client.updateDisplay( ERROR_STATE, "You haven't been given the quest to fight the Sorceress!" );
			client.cancelRequest();
			return false;
		}

		// Make sure he can get to the desired area

		// Deduce based on which image map is used:
		//
		// NoMap = lair1
		// Map = lair1, lair3
		// Map2 = lair1, lair3, lair4
		// Map3 = lair1, lair3, lair4, lair5
		// Map4 = lair1, lair3, lair4, lair5, lair6

		request = new KoLRequest( client, "lair.php", true );
		request.run();

		Matcher mapMatcher = Pattern.compile( "usemap=\"#(\\w+)\"" ).matcher( request.responseText );
		if ( mapMatcher.find() )
		{
			String map = mapMatcher.group( 1 );
			int reached;

			if ( map.equals( "NoMap" ) )
				reached = 1;
			else if ( map.equals( "Map" ) )
				reached = 3;
			else if ( map.equals( "Map2" ) )
				reached = 4;
			else if ( map.equals( "Map3" ) )
				reached = 5;
			else if ( map.equals( "Map4" ) )
				reached = 6;
			else
				reached = 0;

			if ( reached < min )
			{
				client.updateDisplay( ERROR_STATE, "You can't use this script yet." );
				client.cancelRequest();
				return false;
			}

			if ( reached > max )
			{
				client.updateDisplay( ERROR_STATE, "You're already past this script." );
				client.cancelRequest();
				return false;
			}
		}

		// Otherwise, they've passed all the standard checks
		// on prerequisites.  Return true.

		return true;
	}

	public static void completeEntryway()
	{
		// Make sure he's ascended at least once

		if ( !checkPrerequisites( 1, 2 ) )
			return;

		List requirements = new ArrayList();

		// Make sure the character has some candy, or at least
		// the appropriate status effect.

		AdventureResult candy = MARZIPAN.getCount( KoLCharacter.getInventory() ) > 0 ? MARZIPAN :
			FARMER_CANDY.getCount( KoLCharacter.getInventory() ) > 0 ? FARMER_CANDY : RICE_CANDY;

		// Check to see if the person has crossed through the
		// gates already.  If they haven't, then that's the
		// only time you need the special effects.

		KoLRequest request = new KoLRequest( client, "lair1.php" );
		request.run();

		if ( request.responseText.indexOf( "gatesdone" ) == -1 )
		{
			if ( !KoLCharacter.getEffects().contains( SUGAR ) )
				requirements.add( candy );

			if ( !KoLCharacter.getEffects().contains( WUSSINESS ) )
				requirements.add( WUSSY_POTION );

			if ( !KoLCharacter.getEffects().contains( MIASMA ) )
				requirements.add( BLACK_CANDLE );
		}

		// Make sure required keys are available

		// Digital key unless you already have the Squeezings of Woe
		if ( SQUEEZINGS.getCount( KoLCharacter.getInventory() ) < 1 )
			requirements.add( DIGITAL );

		// Skeleton key and a clover unless you already have the
		// Really Evil Rhythms
		if ( RHYTHM.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			requirements.add( SKELETON );
			requirements.add( CLOVER );
		}

		// Decide on which star weapon should be available for
		// this whole process.

		String currentWeapon = KoLCharacter.getCurrentEquipmentName( KoLCharacter.WEAPON );
		AdventureResult starWeapon;

		boolean needsWeapon;

		// If he's currently wielding a star weapon, use that one.

		if ( currentWeapon != null && currentWeapon.startsWith( "star" ) )
		{
			starWeapon = new AdventureResult( currentWeapon );
			needsWeapon = false;
		}
		else
		{
			// Otherwise, he'll need to equip one
			needsWeapon = true;

			// See which ones are available
			boolean hasSword = ( STAR_SWORD.getCount( KoLCharacter.getInventory() ) > 0 ) || ( STAR_SWORD.getCount( KoLCharacter.getCloset() ) > 0 );
			boolean hasStaff = ( STAR_STAFF.getCount( KoLCharacter.getInventory() ) > 0 ) || ( STAR_STAFF.getCount( KoLCharacter.getCloset() ) > 0 );
			boolean hasCrossbow = ( STAR_CROSSBOW.getCount( KoLCharacter.getInventory() ) > 0 ) || ( STAR_CROSSBOW.getCount( KoLCharacter.getCloset() ) > 0 );

			// See which ones he can use
			boolean canUseSword = EquipmentDatabase.canEquip( STAR_SWORD.getName() );
			boolean canUseStaff = EquipmentDatabase.canEquip( STAR_STAFF.getName() );
			boolean canUseCrossbow = EquipmentDatabase.canEquip( STAR_CROSSBOW.getName() );

			// Pick one that he has and can use
			if ( hasSword && canUseSword )
				starWeapon = STAR_SWORD;
			else if ( hasStaff && canUseStaff )
				starWeapon = STAR_STAFF;
			else if ( hasCrossbow && canUseCrossbow )
				starWeapon = STAR_CROSSBOW;

			// Otherwise, pick one that he has
			else if ( hasSword )
				starWeapon = STAR_SWORD;
			else if ( hasStaff )
				starWeapon = STAR_STAFF;
			else if ( hasCrossbow )
				starWeapon = STAR_CROSSBOW;

			// At least pick one that he can use
			else if ( canUseSword )
				starWeapon = STAR_SWORD;
			else if ( canUseStaff )
				starWeapon = STAR_STAFF;
			else if ( canUseCrossbow )
				starWeapon = STAR_CROSSBOW;

			// What a wimp!
			else
				starWeapon = STAR_SWORD;
		}

		boolean needsBuckler = STAR_BUCKLER.getCount( KoLCharacter.getInventory() ) == 0 && !KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ).startsWith( "star" ) &&
			!KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ).startsWith( "star" ) && !KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ).startsWith( "star" );

		// Star equipment unless you already have Sinister Strummings

		if ( STRUMMING.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			if ( needsWeapon )
				requirements.add( starWeapon );

			if ( needsBuckler )
				requirements.add( STAR_BUCKLER );

			requirements.add( RICHARD );
		}

		// The three hero keys are needed to get the SCUBA gear

		if ( SCUBA.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			requirements.add( BORIS );
			requirements.add( JARLSBERG );
			requirements.add( SNEAKY_PETE );

			// It's possible that meat paste is also required, if
			// the person is not in a muscle sign.

			if ( !KoLCharacter.inMuscleSign() )
				requirements.add( new AdventureResult( ItemCreationRequest.MEAT_PASTE, 2 ) );
		}

		// Next, figure out which instrument is needed for the final
		// stage of the entryway.

		requirements.add( HEAVY_METAL_GUITAR.getCount( KoLCharacter.getInventory() ) > 0 ? HEAVY_METAL_GUITAR :
			ACOUSTIC_GUITAR.getCount( KoLCharacter.getInventory() ) > 0 ? ACOUSTIC_GUITAR :
			DISCO_BANJO.getCount( KoLCharacter.getInventory() ) > 0 ? DISCO_BANJO : STONE_BANJO );

		requirements.add( TAMBOURINE.getCount( KoLCharacter.getInventory() ) > 0 ? TAMBOURINE : BONE_RATTLE );
		requirements.add( ROCKNROLL_LEGEND.getCount( KoLCharacter.getInventory() ) > 0 ? ROCKNROLL_LEGEND : ACCORDION );

		// Now that the array's initialized, issue the checks
		// on the items needed to finish the entryway.

		if ( !client.checkRequirements( requirements ) )
			return;

		if ( RHYTHM.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			// The character needs to have at least 50 HP, or 25% of
			// maximum HP (whichever is greater) in order to play
			// the skeleton dice game

			int healthNeeded = Math.max( KoLCharacter.getMaximumHP() / 4, 50 );
			client.recoverHP( healthNeeded );

			// Verify that you have enough HP to proceed with the
			// skeleton dice game.

			if ( KoLCharacter.getCurrentHP() < healthNeeded )
			{
				client.updateDisplay( ERROR_STATE, "You must have more than " + healthNeeded + " HP to proceed." );
				return;
			}
		}

		// If you can't equip the appropriate weapon and buckler,
		// then tell the player they lack the required stats.

		if ( STRUMMING.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			if ( !EquipmentDatabase.canEquip( starWeapon.getName() ) )
			{
				client.updateDisplay( ERROR_STATE, "Stats too low to equip a star weapon." );
				return;
			}

			if ( !EquipmentDatabase.canEquip( STAR_BUCKLER.getName() ) )
			{
				client.updateDisplay( ERROR_STATE, "Stats too low to equip a star buckler." );
				return;
			}
		}

		// Use the rice candy, wussiness potion, and black candle
		// and then cross through the first door.

		if ( request.responseText.indexOf( "gatesdone" ) == -1 )
		{
			if ( !KoLCharacter.getEffects().contains( SUGAR ) )
			{
				client.updateDisplay( DISABLE_STATE, "Getting jittery..." );
				(new ConsumeItemRequest( client, candy )).run();
			}

			if ( !KoLCharacter.getEffects().contains( WUSSINESS ) )
			{
				client.updateDisplay( DISABLE_STATE, "Becoming a pansy..." );
				(new ConsumeItemRequest( client, WUSSY_POTION )).run();
			}

			if ( !KoLCharacter.getEffects().contains( MIASMA ) )
			{
				client.updateDisplay( DISABLE_STATE, "Inverting anime smileyness..." );
				(new ConsumeItemRequest( client, BLACK_CANDLE )).run();
			}

			client.updateDisplay( DISABLE_STATE, "Crossing three door puzzle..." );

			request = new KoLRequest( client, "lair1.php" );
			request.addFormField( "action", "gates" );
			request.run();
		}

		// Now, unequip all of your equipment and cross through
		// the mirror. Process the mirror shard that results.

		if ( request.responseText.indexOf( "lair2.php" ) == -1 )
		{
			(new FamiliarRequest( client, FamiliarData.NO_FAMILIAR )).run();
			(new EquipmentRequest( client, SpecialOutfit.BIRTHDAY_SUIT )).run();
			// We will need to re-equip

			needsWeapon = true;
			needsBuckler = true;

			client.updateDisplay( DISABLE_STATE, "Crossing mirror puzzle..." );

			request = new KoLRequest( client, "lair1.php" );
			request.addFormField( "action", "mirror" );
			request.run();

			client.processResult( SHARD );
		}

		// Now handle the form for the digital key to get
		// the Squeezings of Woe.

		if ( SQUEEZINGS.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			client.updateDisplay( DISABLE_STATE, "Inserting digital key..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( DIGITAL.getItemID() ) );
			request.run();

			if ( request.responseText.indexOf( "prepreaction" ) != -1 )
			{
				request = new KoLRequest( client, "lair2.php" );
				request.addFormField( "prepreaction", "sequence" );
				request.addFormField( "seq1", "up" );
				request.addFormField( "seq2", "up" );
				request.addFormField( "seq3", "down" );
				request.addFormField( "seq4", "down" );
				request.addFormField( "seq5", "left" );
				request.addFormField( "seq6", "right" );
				request.addFormField( "seq7", "left" );
				request.addFormField( "seq8", "right" );
				request.addFormField( "seq9", "b" );
				request.addFormField( "seq10", "a" );
				request.run();
			}

			client.processResult( SQUEEZINGS );
		}

		// Now handle the form for the star key to get
		// the Sinister Strumming.  Note that this will
		// require you to re-equip your star weapon and
		// a star buckler and switch to a starfish first.

		if ( STRUMMING.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			if ( needsWeapon )
				(new EquipmentRequest( client, starWeapon.getName() )).run();

			if ( needsBuckler )
				(new EquipmentRequest( client, STAR_BUCKLER.getName() )).run();

			(new FamiliarRequest( client, new FamiliarData( 17 ) )).run();

			client.updateDisplay( DISABLE_STATE, "Inserting Richard's star key..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( RICHARD.getItemID() ) );
			request.run();

			if ( request.responseText.indexOf( "prepreaction" ) != -1 )
			{
				request = new KoLRequest( client, "lair2.php" );
				request.addFormField( "prepreaction", "starcage" );
				request.run();

				// For unknown reasons, this doesn't always work
				// Error check the possibilities

				// "You beat on the cage with your weapon, but
				// to no avail.	 It doesn't appear to be made
				// out of the right stuff."

				if ( request.responseText.indexOf( "right stuff" ) != -1 )
				{
					client.updateDisplay( ERROR_STATE, "Failed to equip a star weapon." );
					return;
				}

				// "A fragment of a line hits you really hard
				// on the arm, and it knocks you back into the
				// main cavern."

				if ( request.responseText.indexOf( "knocks you back" ) != -1 )
				{
					client.updateDisplay( ERROR_STATE, "Failed to equip star buckler." );
					return;
				}

				// "Trog creeps toward the pedestal, but is
				// blown backwards.  You give up, and go back
				// out to the main cavern."

				if ( request.responseText.indexOf( "You give up" ) != -1 )
				{
					client.updateDisplay( ERROR_STATE, "Failed to equip star starfish." );
					return;
				}

				// "Gron crawls toward the pedestal, picks it
				// up, and brings it back to you."

				if ( request.responseText.indexOf( "brings it back to you" ) != -1 )
				{
					// Result "stone tablet (Sinister
					// Strumming)" doesn't parse
					// correctly...

					client.processResult( STRUMMING );
				}
			}
		}

		// Next, handle the form for the skeleton key to
		// get the Really Evil Rhythm. This uses up the
		// clover you had, so process it.

		if ( RHYTHM.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			client.updateDisplay( DISABLE_STATE, "Inserting skeleton key..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( SKELETON.getItemID() ) );
			request.run();

			if ( request.responseText.indexOf( "prepreaction" ) != -1 )
			{
				request = new KoLRequest( client, "lair2.php" );
				request.addFormField( "prepreaction", "skel" );
				request.run();

				client.processResult( RHYTHM );
				client.processResult( CLOVER.getNegation() );
			}
		}

		// Next, handle the three hero keys, which involve
		// answering the riddles with the forms of fish.

		if ( SCUBA.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			client.updateDisplay( DISABLE_STATE, "Inserting Boris's key..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( BORIS.getItemID() ) );
			request.run();

			if ( request.responseText.indexOf( "prepreaction" ) != -1 )
			{
				request = new KoLRequest( client, "lair2.php" );
				request.addFormField( "prepreaction", "sorcriddle1" );
				request.addFormField( "answer", "fish" );
				request.run();
			}

			client.updateDisplay( DISABLE_STATE, "Inserting Jarlsberg's key..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( JARLSBERG.getItemID() ) );
			request.run();

			if ( request.responseText.indexOf( "prepreaction" ) != -1 )
			{
				request = new KoLRequest( client, "lair2.php" );
				request.addFormField( "prepreaction", "sorcriddle2" );
				request.addFormField( "answer", "phish" );
				request.run();
			}

			client.updateDisplay( DISABLE_STATE, "Inserting Sneaky Pete's key..." );

			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( SNEAKY_PETE.getItemID() ) );
			request.run();

			if ( request.responseText.indexOf( "prepreaction" ) != -1 )
			{
				request = new KoLRequest( client, "lair2.php" );
				request.addFormField( "prepreaction", "sorcriddle3" );
				request.addFormField( "answer", "fsh" );
				request.run();
			}

			// Now use the components to make the SCUBA gear

			KoLCharacter.refreshCalculatedLists();
			ItemCreationRequest.getInstance( client, 734, 1 ).run();
		}

		// Equip the SCUBA gear

		(new EquipmentRequest( client, "makeshift SCUBA gear" )).run();

		// If he brought a balloon monkey, get him an easter egg

		if ( BALLOON.getCount( KoLCharacter.getInventory() ) > 0 )
		{
			request = new KoLRequest( client, "lair2.php" );
			request.addFormField( "preaction", "key" );
			request.addFormField( "whichkey", String.valueOf( BALLOON.getItemID() ) );
			request.run();
		}

		// Now, press the switch beyond the odor by
		// visiting the appropriate page.

		client.updateDisplay( DISABLE_STATE, "Pressing switch beyond odor..." );
		(new KoLRequest( client, "lair2.php?action=odor" )).run();

		// Finally, arm the stone mariachis with their
		// appropriate instruments.

		client.updateDisplay( DISABLE_STATE, "Arming stone mariachis..." );

		request = new KoLRequest( client, "lair2.php" );
		request.addFormField( "action", "statues" );
		request.run();

		// "As the mariachis reach a dire crescendo (Hey, have you
		// heard my new band, Dire Crescendo?) the gate behind the
		// statues slowly grinds open, revealing the way to the
		// Sorceress' courtyard."

		// Just check to see if there is a link to lair3.php

		if ( request.responseText.indexOf( "lair3.php" ) == -1 )
		{
			client.updateDisplay( ERROR_STATE, "Failed to complete entryway." );
			return;
		}

		// This consumes the tablets
		client.processResult( RHYTHM.getNegation() );
		client.processResult( STRUMMING.getNegation() );
		client.processResult( SQUEEZINGS.getNegation() );

		client.updateDisplay( ENABLE_STATE, "Sorceress entryway complete." );
	}

	public static void completeHedgeMaze()
	{
		if ( !checkPrerequisites( 3, 3 ) )
			return;

		// Retrieve any puzzle pieces that might be sitting
		// inside of the player's closet.

		int closetCount = PUZZLE_PIECE.getCount( KoLCharacter.getCloset() );
		int inventoryCount = PUZZLE_PIECE.getCount( KoLCharacter.getInventory() );

		if ( closetCount > 0 )
			AdventureDatabase.retrieveItem( PUZZLE_PIECE.getInstance( inventoryCount + closetCount ) );

		// Check to see if you've run out of puzzle pieces.
		// If you have, don't bother running the puzzle.

		if ( inventoryCount + closetCount == 0 )
		{
			client.updateDisplay( ERROR_STATE, "Ran out of puzzle pieces." );
			client.cancelRequest();
			return;
		}

		// Otherwise, check their current state relative
		// to the hedge maze, and begin!

		client.updateDisplay( DISABLE_STATE, "Retrieving maze status..." );
		KoLRequest request = new KoLRequest( client, "hedgepuzzle.php" );
		request.run();

		String responseText = request.responseText;

		// First mission -- retrieve the key from the hedge
		// maze puzzle.

		if ( !KoLCharacter.getInventory().contains( HEDGE_KEY ) )
		{
			client.updateDisplay( DISABLE_STATE, "Retrieving hedge key..." );
			responseText = retrieveHedgeKey( responseText );

			// Retrieving the key after rotating the puzzle pieces
			// uses an adventure. If we ran out, we canceled.

			if ( !client.permitsContinue() )
				return;
		}

		// Second mission -- rotate the hedge maze until
		// the hedge path leads to the hedge door.

		if ( responseText.indexOf( "Click one" ) != -1 )
		{
			client.updateDisplay( DISABLE_STATE, "Executing final rotations..." );
			responseText = finalizeHedgeMaze( responseText );

			// Navigating up to the tower door after rotating the
			// puzzle pieces requires an adventure. If we ran out,
			// we canceled.

			if ( !client.permitsContinue() )
				return;
		}

		// Check to see if you ran out of puzzle pieces
		// in the middle -- if you did, update the user
		// display to say so.

		if ( responseText.indexOf( "Click one" ) == -1 )
		{
			client.updateDisplay( ERROR_STATE, "Ran out of puzzle pieces." );
			client.cancelRequest();
			return;
		}

		client.updateDisplay( ENABLE_STATE, "Hedge maze quest complete." );
	}

	private static String rotateHedgePiece( String responseText, String hedgePiece, String searchText )
	{
		KoLRequest request;

		// Rotate puzzle sections until we reach our goal
		while ( responseText.indexOf( searchText ) == -1 )
		{
			// We're out of puzzles unless the response says:

			// "Click one of the puzzle sections to rotate that
			// section 90 degrees to the right."

			if ( responseText.indexOf( "Click one" ) == -1 )
				return responseText;

			request = new KoLRequest( client, "hedgepuzzle.php" );
			request.addFormField( "action", hedgePiece );
			request.run();

			responseText = request.responseText;

			// If the topiary golem stole one of your hedge
			// pieces, take it away.

			if ( responseText.indexOf( "Topiary Golem" ) != -1 )
				client.processResult( PUZZLE_PIECE.getNegation() );
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

		if ( responseText.indexOf( "Click one" ) != -1 )
		{
			KoLRequest request = new KoLRequest( client, "lair3.php" );
			request.addFormField( "action", "hedge" );
			request.run();

			if ( request.responseText.indexOf( "You're out of adventures." ) != -1 )
			{
				// Cancel and return now
				client.updateDisplay( ERROR_STATE, "Ran out of adventures." );
				client.cancelRequest();
				return responseText;
			}

			// Decrement adventure tally
			client.processResult( new AdventureResult( AdventureResult.ADV, -1 ) );
		}

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

		if ( responseText.indexOf( "Click one" ) != -1 )
		{
			KoLRequest request = new KoLRequest( client, "lair3.php" );
			request.addFormField( "action", "hedge" );
			request.run();

			if ( request.responseText.indexOf( "You're out of adventures." ) != -1 )
			{
				client.updateDisplay( ERROR_STATE, "Ran out of adventures." );
				client.cancelRequest();
				return responseText;
			}

			// Decrement adventure tally
			client.processResult( new AdventureResult( AdventureResult.ADV, -1 ) );
		}

		return responseText;
	}

	public static void fightTowerGuardians()
	{
		if ( !checkPrerequisites( 4, 5 ) )
			return;

		// Determine which level you actually need to start from.

		client.updateDisplay( DISABLE_STATE, "Climbing the tower..." );

		KoLRequest request = new KoLRequest( client, "lair4.php" );
		request.run();

		int currentLevel = 0;

		if ( request.responseText.indexOf( "lair5.php" ) != -1 )
		{
			// There is a link to higher in the tower.

			request = new KoLRequest( client, "lair5.php" );
			request.run();

			currentLevel = 3;
		}

		if ( request.responseText.indexOf( "value=\"level1\"" ) != -1 )
			currentLevel += 1;
		else if ( request.responseText.indexOf( "value=\"level2\"" ) != -1 )
			currentLevel += 2;
		else if ( request.responseText.indexOf( "value=\"level3\"" ) != -1 )
			currentLevel += 3;
		else
			currentLevel += 4;

		for ( int towerLevel = currentLevel; towerLevel <= 6; ++towerLevel )
			if ( !fightGuardian( towerLevel ) )
				return;

		client.updateDisplay( ENABLE_STATE, "Path to Sorceress's chamber cleared." );
	}

	private static boolean fightGuardian( int towerLevel )
	{
		client.updateDisplay( DISABLE_STATE, "Fighting guardian on level " + towerLevel + " of the tower..." );

		// Boldly climb the stairs.

		KoLRequest request = new KoLRequest( client, towerLevel <= 3 ? "lair4.php" : "lair5.php", true );
		request.addFormField( "action", "level" + ((towerLevel - 1) % 3 + 1) );
		request.run();

		if ( request.responseText.indexOf( "You don't have time to mess around in the Tower." ) != -1 )
		{
			client.updateDisplay( ERROR_STATE, "You're out of adventures." );
			client.cancelRequest();
			return false;
		}

		// Decrement adventure tally
		client.processResult( new AdventureResult( AdventureResult.ADV, -1 ) );

		// Parse response to see which item we need.

		AdventureResult guardianItem = getGuardianItem( request.responseText );

		// With the guardian item retrieved, check to see if you have
		// the item, and if so, use it and report success.  Otherwise,
		// run away and report failure.

		request = new KoLRequest( client, "fight.php" );

		if ( KoLCharacter.getInventory().contains( guardianItem ) )
		{
			request.addFormField( "action", "useitem" );
			request.addFormField( "whichitem", String.valueOf( guardianItem.getItemID() ) );
			request.run();

			// Un-cancel
			client.resetContinueState();

			// Use up the item
			client.processResult( guardianItem.getNegation() );

			// Keep on climbing
			return true;
		}

		// Since we don't have the item, run away

		request.addFormField( "action", "runaway" );
		request.run();

		AdventureDatabase.retrieveItem( guardianItem );
		if ( guardianItem.getCount( KoLCharacter.getInventory() ) != 0 )
			return fightGuardian( towerLevel );

		client.updateDisplay( ERROR_STATE, "You need an additional " + guardianItem.getName() + " to continue." );
		return false;
	}

	private static AdventureResult getGuardianItem( String fightText )
	{
		for ( int i = 0; i < GUARDIAN_DATA.length; ++i)
			if ( fightText.indexOf( GUARDIAN_DATA[i][0] ) != -1 )
				return new AdventureResult( GUARDIAN_DATA[i][1], 1 );

		// Shouldn't get here.

		client.updateDisplay( ERROR_STATE, "Unknown guardian!" );
		return new AdventureResult( 666, 1 );
	}

	public static void completeSorceressChamber()
	{
		KoLRequest request;

		// Make sure the player has ascended at least once

		if ( !checkPrerequisites( 6, 6 ) )
			return;

		// You must have at least 70 in all stats before you can enter
		// the chamber.

		if ( KoLCharacter.getBaseMuscle() < 70 || KoLCharacter.getBaseMysticality() < 70 || KoLCharacter.getBaseMoxie() < 70 )
		{
			client.updateDisplay( ERROR_STATE, "You can't enter the chamber unless all base stats are 70 or higher." );
			return;
		}

		// Figure out how far he's gotten into the Sorceress's Chamber
		request = new KoLRequest( client, "lair6.php", true );
		request.run();

		if ( request.responseText.indexOf( "ascend.php" ) != -1 )
		{
			client.updateDisplay( ENABLE_STATE, "You've already beaten Her Naughtiness. Go forth and ascend!" );
			return;
		}

		int n = -1;
		Matcher placeMatcher = Pattern.compile( "lair6.php\\?place=(\\d+)" ).matcher( request.responseText );
		if ( placeMatcher.find() )
		{
			try
			{
				n = df.parse( placeMatcher.group(1) ).intValue();
			}
			catch ( Exception e )
			{
				// Ignore parse error
			}
		}

		if ( n < 0)
		{
			client.updateDisplay( ERROR_STATE, "I can't tell how far you've gotten into the Sorceress's Chamber yet." );
			return;
		}

		if ( n == 0)
		{
			// We know that all base stats are at least 70. But if
			// we attained that goal this session without
			// charpane.php having been invoked, KoL itself
			// sometimes doesn't realize it and will complain
			// "You're not tough enough to fight up here."

			(new CharpaneRequest( client )).run();
		}

		while ( n < 5 )
		{
			switch (n)
			{
				case 0:
					findDoorCode();
					break;
				case 1:
					reflectEnergyBolt();
					break;
				case 2:
					fightShadow();
					break;
				case 3:
					familiarBattle(3);
					break;
				case 4:
					familiarBattle(4);
					break;
				}

			if ( !client.permitsContinue() )
				return;

			n += 1;
		}

		client.updateDisplay( ENABLE_STATE, "Her Naughtiness awaits. Go battle her!" );
	}

	private static void findDoorCode()
	{
		KoLRequest request;

		client.updateDisplay( DISABLE_STATE, "Cracking door code..." );

		// Enter the chamber

		request = new KoLRequest( client, "lair6.php", true );
		request.addFormField( "place", "0" );
		request.run();

		// Talk to the guards

		request = new KoLRequest( client, "lair6.php", true );
		request.addFormField( "place", "0" );
		request.addFormField( "preaction", "lightdoor" );
		request.run();

		// Crack the code

		String code = deduceCode( request.responseText );

		if ( code == null )
		{
			client.updateDisplay( ERROR_STATE, "Couldn't solve door code. Do it yourself and come back!" );
			client.cancelRequest();
			return;
		}

		request = new KoLRequest( client, "lair6.php", true );
		request.addFormField( "place", "0" );
		request.addFormField( "action", "doorcode" );
		request.addFormField( "code", code );
		request.run();

		// Check for success

		if ( request.responseText.indexOf( "the door slides open" ) == -1 )
		{
			// Account for HP loss
			client.updateDisplay( ERROR_STATE, "I used the wrong code. Sorry." );
			client.cancelRequest();
		}
	}

	private static String deduceCode( String text )
	{
		int start = text.indexOf( "<p>The guard playing South" );
		if ( start == -1)
			return null;

		int end = text.indexOf( "<p>You roll your eyes." );
		if (end == -1)
			return null;

		// Pretty up the data
		String dialog = text.substring( start+3, end ).replaceAll( "&quot;", "\"" );

		// Make an array of lines
		String lines[] = dialog.split( " *<p>" );
		if ( lines.length != 16)
			return null;

		// Initialize the three digits of the code
		String digit1 = "0", digit2 = "0", digit3 = "0";
		Matcher matcher;

		// Check for variant, per Visual WIKI
		if (lines[7].indexOf( "You're full of it") != -1 )
		{
			matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[5] );
			if ( !matcher.find() )
				return null;
			digit1 = matcher.group(1);
			matcher = Pattern.compile( "it's (\\d)" ).matcher( lines[11] );
			if ( !matcher.find() )
				return null;
			digit2 = matcher.group(1);
			matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[12] );
			if ( !matcher.find() )
				return null;
			digit3 = matcher.group(1);
		}
		else
		{
			if ( lines[13].indexOf( "South" ) != -1 )
				matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[5] );
			else
				matcher = Pattern.compile( "It's (\\d)" ).matcher( lines[6] );
			if ( !matcher.find() )
				return null;
			digit1 = matcher.group(1);
			matcher = Pattern.compile( "that's (\\d)" ).matcher( lines[8] );
			if ( !matcher.find() )
				return null;
			digit2 = matcher.group(1);
			matcher = Pattern.compile( "It's (\\d)" ).matcher( lines[13] );
			if ( !matcher.find() )
				return null;
			digit3 = matcher.group(1);
		}

		return ( digit1 + digit2 + digit3 );
	}

	private static void reflectEnergyBolt()
	{
		KoLRequest request;
		AdventureDatabase.retrieveItem( SHARD );

		// Equip the huge mirror shard
		(new EquipmentRequest( client, SHARD.getName() )).run();

		client.updateDisplay( DISABLE_STATE, "Reflecting energy bolt..." );

		// Reflect the energy bolt
		request = new KoLRequest( client, "lair6.php", true );
		request.addFormField( "place", "1" );
		request.run();
	}

	private static void fightShadow()
	{
		List requirements = new ArrayList();
		requirements.add( new AdventureResult( "red pixel potion", 4 ) );

		if ( !client.checkRequirements( requirements ) )
			return;

		// Ensure that the player is at full HP since the shadow will
		// probably beat him up if he has less.

		client.recoverHP( KoLCharacter.getMaximumHP() );

		// Need to be at full health.  Abort if this is
		// not the case.

		if ( KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() )
		{
			client.updateDisplay( ERROR_STATE, "You must be fully healed to fight your shadow." );
			client.cancelRequest();
			return;
		}

		client.resetContinueState();
		client.updateDisplay( DISABLE_STATE, "Fighting your shadow..." );

		// Start the battle!

		String action = getProperty( "battleAction" );
		setProperty( "battleAction", "item464" );

		KoLRequest request = new KoLRequest( client, "lair6.php" );
		request.addFormField( "place", "2" );
		request.run();

		setProperty( "battleAction", action );

		if ( request.responseText.indexOf( "You don't have time to mess around up here." ) != -1 )
		{
			client.updateDisplay( ERROR_STATE, "You're out of adventures." );
			client.cancelRequest();
			return;
		}

		client.processResult( new AdventureResult( AdventureResult.ADV, -1 ) );
		client.resetContinueState();
	}

	private static void familiarBattle( int n )
	{
		// Make sure that the familiar is at least twenty pounds.
		// Otherwise, it's a wasted request.

		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
		if ( currentFamiliar == null )
		{
			client.updateDisplay( ERROR_STATE, "You don't have a familiar equipped." );
			client.cancelRequest();
			return;
		}

		// Ensure that the player has more than 50 HP, since
		// you cannot enter the familiar chamber with less.

		client.recoverHP( 50 );

		// Need more than 50 hit points.  Abort if this is
		// not the case.

		if ( KoLCharacter.getCurrentHP() <= 50 )
		{
			client.updateDisplay( ERROR_STATE, "You must have more than 50 HP to proceed." );
			client.cancelRequest();
			return;
		}

		client.updateDisplay( DISABLE_STATE, "Facing giant familiar..." );
		KoLRequest request = new KoLRequest( client, "lair6.php", true );
		request.addFormField( "place", String.valueOf( n ) );
		request.run();

		// If you do not successfully pass the familiar, you
		// will get a "stomp off in a huff" message.

		if ( request.responseText.indexOf( "stomp off in a huff" ) == -1 )
			return;

		// Find the necessary familiar and see if the player has one.

		String race = "";
		FamiliarData familiar = null;
		for ( int i = 0; i < FAMILIAR_DATA.length; ++i )
		{
			if ( request.responseText.indexOf( FAMILIAR_DATA[i][0] ) != -1 )
			{
				race = FAMILIAR_DATA[i][1];
				familiar = KoLCharacter.findFamiliar( race );
				break;
			}
		}

		// If not, tell the player to get one and come back.

		if ( familiar == null)
		{
			client.updateDisplay( ERROR_STATE, "Come back with a 20 pound " + race );
			client.cancelRequest();
			return;
		}

		// Switch to the required familiar

		if ( currentFamiliar != familiar )
			(new FamiliarRequest( client, familiar )).run();

		// If we can buff it to 20 pounds, try again.

		if ( FamiliarTrainingFrame.buffFamiliar( client, 20 ) )
		{
			familiarBattle( n );
			return;
		}

		// We can't buff it high enough. Train it.

		if ( !FamiliarTrainingFrame.levelFamiliar( client, 20, FamiliarTrainingFrame.BUFFED, false, false) )
			return;

		// We trained it. Equip and buff it.

		if ( !FamiliarTrainingFrame.buffFamiliar( client, 20 ) )
			return;

		// We're good to go. Fight!
		familiarBattle( n );
	}
}
