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
	// Patterns for repeated usage.

	private static final Pattern MAP_PATTERN = Pattern.compile( "usemap=\"#(\\w+)\"" );
	private static final Pattern LAIR6_PATTERN = Pattern.compile( "lair6.php\\?place=(\\d+)" );

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
	private static final AdventureResult STAR_HAT = new AdventureResult( 661, 1 );

	private static final AdventureResult STONE_BANJO = new AdventureResult( 53, 1 );
	private static final AdventureResult DISCO_BANJO = new AdventureResult( 54, 1 );
	private static final AdventureResult ACOUSTIC_GUITAR = new AdventureResult( 404, 1 );
	private static final AdventureResult HEAVY_METAL_GUITAR = new AdventureResult( 507, 1 );

	private static final AdventureResult BROKEN_SKULL = new AdventureResult( 741, 1 );
	private static final AdventureResult BONE_RATTLE = new AdventureResult( 168, 1 );
	private static final AdventureResult TAMBOURINE = new AdventureResult( 740, 1 );

	private static final AdventureResult ACCORDION = new AdventureResult( 11, 1 );
	private static final AdventureResult ROCKNROLL_LEGEND = new AdventureResult( 50, 1 );

	private static final AdventureResult CLOVER = new AdventureResult( 24, 1 );

	private static final AdventureResult DIGITAL = new AdventureResult( 691, 1 );
	private static final AdventureResult RICHARD = new AdventureResult( 665, 1 );
	private static final AdventureResult SKELETON = new AdventureResult( 642, 1 );
	private static final AdventureResult KEY_RING = new AdventureResult( 643, 1 );


	private static final AdventureResult BORIS = new AdventureResult( 282, 1 );
	private static final AdventureResult JARLSBERG = new AdventureResult( 283, 1 );
	private static final AdventureResult SNEAKY_PETE = new AdventureResult( 284, 1 );
	private static final AdventureResult BALLOON = new AdventureResult( 436, 1 );

	// Results of key puzzles

	private static final AdventureResult STRUMMING = new AdventureResult( 736, 1 );
	private static final AdventureResult SQUEEZINGS = new AdventureResult( 737, 1 );
	private static final AdventureResult RHYTHM = new AdventureResult( 738, 1 );

	private static final AdventureResult BOWL = new AdventureResult( 729, 1 );
	private static final AdventureResult TANK = new AdventureResult( 730, 1 );
	private static final AdventureResult HOSE = new AdventureResult( 731, 1 );
	private static final AdventureResult HOSE_TANK = new AdventureResult( 732, 1 );
	private static final AdventureResult HOSE_BOWL = new AdventureResult( 733, 1 );
	private static final AdventureResult SCUBA = new AdventureResult( 734, 1 );

	// Items for the hedge maze

	public static final AdventureResult PUZZLE_PIECE = new AdventureResult( 727, 1 );
	public static final AdventureResult HEDGE_KEY = new AdventureResult( 728, 1 );

	private static final AdventureResult BANJO_STRING = new AdventureResult( 52, 1 );
	private static final AdventureResult [] CLOVER_WEAPONS = { new AdventureResult( 32, 1 ), new AdventureResult( 50, 1 ), new AdventureResult( 57, 1 ), new AdventureResult( 60, 1 ), new AdventureResult( 68, 1 ) };

	private static final AdventureResult HEART_ROCK = new AdventureResult( 48, 1 );

	// Items for the shadow battle

	private static final AdventureResult DOC_ELIXIR = new AdventureResult( "Doc Galaktik's Homeopathic Elixir", 6 );
	private static final AdventureResult RED_POTION = new AdventureResult( "red pixel potion", 4 );
	private static final AdventureResult PLASTIC_EGG = new AdventureResult( "red plastic oyster egg", 3 );

	// Guardians and the items that defeat them

	public static final String [][] GUARDIAN_DATA =
	{
		{ "Beer Batter", "baseball" }, { "Vicious Easel", "disease" }, { "Enraged Cow", "barbed-wire fence" },
		{ "Fickle Finger of F8", "razor-sharp can lid" }, { "Big Meat Golem", "meat vortex" }, { "Flaming Samurai", "frigid ninja stars" },
		{ "Tyrannosaurus Tex", "chaos butterfly" }, { "Giant Desktop Globe", "NG" }, { "Electron Submarine", "photoprotoneutron torpedo" },
		{ "Bowling Cricket", "sonar-in-a-biscuit" }, { "Ice Cube", "can of hair spray" }, { "Pretty Fly", "spider web" }
	};

	// Items for the Sorceress's Chamber
	private static final AdventureResult SHARD = new AdventureResult( 726, 1 );
	private static final AdventureResult RED_PIXEL_POTION = new AdventureResult( 464, 1 );

	private static final FamiliarData STARFISH = new FamiliarData( 17 );
	private static final AdventureResult STARFISH_ITEM = new AdventureResult( 664, 1 );

	// Familiars and the familiars that defeat them
	private static final String [][] FAMILIAR_DATA =
	{
		{ "giant sabre-toothed lime", "Levitating Potato" },
		{ "giant mosquito", "Sabre-Toothed Lime" },
		{ "giant barrrnacle", "Angry Goat" },
		{ "giant goat", "Mosquito" },
		{ "giant potato", "Barrrnacle" }
	};

	private static boolean checkPrerequisites( int min, int max )
	{
		KoLmafia.updateDisplay( "Checking prerequisites..." );

		// Make sure you have a starfish.  If not,
		// acquire the item and use it; use the
		// default acquisition mechanisms.

		if ( !KoLCharacter.getFamiliarList().contains( STARFISH ) )
		{
			(new ConsumeItemRequest( STARFISH_ITEM )).run();
			if ( !KoLmafia.permitsContinue() )
				return false;
		}

		// Make sure he's been given the quest

		REDIRECT_FOLLOWER.constructURLString( "main.php" );
		REDIRECT_FOLLOWER.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "lair.php" ) == -1 )
		{
			// Visit the council to see if the quest can be unlocked,
			// but only if you've reached level 11.

			boolean unlockedQuest = false;
			if ( KoLCharacter.getLevel() >= 11 )
			{
				// We should theoretically be able to figure out
				// whether or not the quest is unlocked from the
				// HTML in the council request, but for now, use
				// this inefficient workaround.

				DEFAULT_SHELL.executeLine( "council" );
				REDIRECT_FOLLOWER.run();
				unlockedQuest = REDIRECT_FOLLOWER.responseText.indexOf( "lair.php" ) != -1;
			}

			if ( !unlockedQuest )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You haven't been given the quest to fight the Sorceress!" );
				return false;
			}
		}

		// Make sure he can get to the desired area

		// Deduce based on which image map is used:
		//
		// NoMap = lair1
		// Map = lair1, lair3
		// Map2 = lair1, lair3, lair4
		// Map3 = lair1, lair3, lair4, lair5
		// Map4 = lair1, lair3, lair4, lair5, lair6

		REDIRECT_FOLLOWER.constructURLString( "lair.php" );
		REDIRECT_FOLLOWER.run();

		Matcher mapMatcher = MAP_PATTERN.matcher( REDIRECT_FOLLOWER.responseText );
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
				KoLmafia.updateDisplay( ERROR_STATE, "You can't use this script yet." );
				return false;
			}

			if ( reached > max )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You're already past this script." );
				return false;
			}
		}

		// Otherwise, they've passed all the standard checks
		// on prerequisites.  Return true.

		return true;
	}

	private static AdventureResult pickOne( AdventureResult [] itemOptions )
	{
		for ( int i = 0; i < itemOptions.length; ++i )
			if ( inventory.contains( itemOptions[i] ) )
				return itemOptions[i];

		for ( int i = 0; i < itemOptions.length; ++i )
			if ( hasItem( itemOptions[i] ) )
				return itemOptions[i];

		return itemOptions[0];
	}

	private static boolean hasItem( AdventureResult item )
	{	return KoLCharacter.hasItem( item, true );
	}

	public static void completeCloveredEntryway()
	{	completeEntryway( true );
	}

	public static void completeCloverlessEntryway()
	{	completeEntryway( false );
	}

	public static void completeEntryway( boolean useCloverForSkeleton )
	{
		if ( !checkPrerequisites( 1, 2 ) )
			return;

		SpecialOutfit.createCheckpoint( true );

		// If you couldn't complete the gateway, then return
		// from this method call.

		FamiliarData originalFamiliar = KoLCharacter.getFamiliar();
		completeGateway();

		if ( !KoLmafia.permitsContinue() )
			return;

		List requirements = new ArrayList();

		// Next, figure out which instruments are needed for the final
		// stage of the entryway. If the person has a clover weapon,
		// but no stringed instrument, but they have a banjo string,
		// then dismantle the legend and construct the stone banjo.

		AdventureResult cloverWeapon = null;
		boolean untinkerCloverWeapon = !hasItem( ACOUSTIC_GUITAR ) && !hasItem( HEAVY_METAL_GUITAR ) && !hasItem( STONE_BANJO ) && !hasItem( DISCO_BANJO );

		if ( untinkerCloverWeapon )
		{
			cloverWeapon = pickOne( CLOVER_WEAPONS );

			if ( hasItem( BANJO_STRING ) && hasItem( cloverWeapon ) )
			{
				UseSkillRequest.untinkerCloverWeapon( cloverWeapon );

				ItemCreationRequest irequest = ItemCreationRequest.getInstance( STONE_BANJO.getItemID() );
				irequest.setQuantityNeeded( 1 );
				irequest.run();
			}
		}

		requirements.add( pickOne( new AdventureResult [] { STONE_BANJO, ACOUSTIC_GUITAR, HEAVY_METAL_GUITAR, DISCO_BANJO } ) );

		AdventureResult percussion = pickOne( new AdventureResult [] { BONE_RATTLE, TAMBOURINE, BROKEN_SKULL } );
		requirements.add( percussion );
		requirements.add( pickOne( new AdventureResult [] { ACCORDION, ROCKNROLL_LEGEND } ) );

		// If he brought a balloon monkey, get him an easter egg

		if ( hasItem( BALLOON ) )
		{
			AdventureDatabase.retrieveItem( BALLOON );
			REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
			REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
			REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( BALLOON.getItemID() ) );
			REDIRECT_FOLLOWER.run();
		}

		// Now, iterate through each of the completion steps;
		// at the end, check to make sure you've completed
		// all the needed requirements.

		requirements.addAll( retrieveRhythm( useCloverForSkeleton ) );
		requirements.addAll( retrieveStrumming() );
		requirements.addAll( retrieveSqueezings() );
		requirements.addAll( retrieveScubaGear() );

		DEFAULT_SHELL.executeLine( "familiar " + originalFamiliar.getRace() );
		SpecialOutfit.restoreCheckpoint( true );

		if ( !getClient().checkRequirements( requirements ) || KoLmafia.refusesContinue() )
			return;

		// If you decided to use a broken skull because
		// you had no other items, untinker the key.

		if ( percussion == BROKEN_SKULL )
		{
			DEFAULT_SHELL.executeLine( "untinker skeleton key" );
			DEFAULT_SHELL.executeLine( "create bone rattle" );
		}

		// Finally, arm the stone mariachis with their
		// appropriate instruments.

		KoLmafia.updateDisplay( "Arming stone mariachis..." );

		AdventureDatabase.retrieveItem( RHYTHM );
		AdventureDatabase.retrieveItem( STRUMMING );
		AdventureDatabase.retrieveItem( SQUEEZINGS );

		REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
		REDIRECT_FOLLOWER.addFormField( "action", "statues" );
		REDIRECT_FOLLOWER.run();


		// "As the mariachis reach a dire crescendo (Hey, have you
		// heard my new band, Dire Crescendo?) the gate behind the
		// statues slowly grinds open, revealing the way to the
		// Sorceress' courtyard."

		// Just check to see if there is a link to lair3.php

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "lair3.php" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to complete entryway." );
			return;
		}

		// This consumes the tablets

		getClient().processResult( RHYTHM.getNegation() );
		getClient().processResult( STRUMMING.getNegation() );
		getClient().processResult( SQUEEZINGS.getNegation() );

		// If you untinkered the rock and roll legend at the very
		// beginning, go ahead and re-create it at the end.

		if ( untinkerCloverWeapon )
		{
			(new UntinkerRequest( STONE_BANJO.getItemID() )).run();

			ItemCreationRequest irequest = ItemCreationRequest.getInstance( cloverWeapon.getItemID() );
			irequest.setQuantityNeeded( 1 );
			irequest.run();
		}

		KoLmafia.updateDisplay( "Sorceress entryway complete." );
	}

	private static void completeGateway()
	{
		// Make sure the character has some candy, or at least
		// the appropriate status effect.

		AdventureResult candy = pickOne( new AdventureResult [] { RICE_CANDY, MARZIPAN, FARMER_CANDY } );

		// Check to see if the person has crossed through the
		// gates already.  If they haven't, then that's the
		// only time you need the special effects.

		REDIRECT_FOLLOWER.constructURLString( "lair1.php" );
		REDIRECT_FOLLOWER.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "gatesdone" ) == -1 )
		{
			if ( !activeEffects.contains( SUGAR ) )
				AdventureDatabase.retrieveItem( candy );

			if ( !activeEffects.contains( WUSSINESS ) )
				AdventureDatabase.retrieveItem( WUSSY_POTION );

			if ( !activeEffects.contains( MIASMA ) )
				AdventureDatabase.retrieveItem( BLACK_CANDLE );
		}

		if ( !KoLmafia.permitsContinue() )
			return;

		// Use the rice candy, wussiness potion, and black candle
		// and then cross through the first door.

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "gatesdone" ) == -1 )
		{
			if ( !activeEffects.contains( SUGAR ) )
				(new ConsumeItemRequest( candy )).run();

			if ( !activeEffects.contains( WUSSINESS ) )
				(new ConsumeItemRequest( WUSSY_POTION )).run();

			if ( !activeEffects.contains( MIASMA ) )
				(new ConsumeItemRequest( BLACK_CANDLE )).run();

			KoLmafia.updateDisplay( "Crossing three door puzzle..." );

			REDIRECT_FOLLOWER.constructURLString( "lair1.php" );
			REDIRECT_FOLLOWER.addFormField( "action", "gates" );
			REDIRECT_FOLLOWER.run();
		}

		// Now, unequip all of your equipment and cross through
		// the mirror. Process the mirror shard that results.

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "lair2.php" ) == -1 )
		{
			DEFAULT_SHELL.executeLine( "familiar none" );
			DEFAULT_SHELL.executeLine( "outfit birthday suit" );

			// We will need to re-equip

			KoLmafia.updateDisplay( "Crossing mirror puzzle..." );

			REDIRECT_FOLLOWER.constructURLString( "lair1.php" );
			REDIRECT_FOLLOWER.addFormField( "action", "mirror" );
			REDIRECT_FOLLOWER.run();
		}
	}

	private static List retrieveRhythm( boolean useCloverForSkeleton )
	{
		// Skeleton key and a clover unless you already have the
		// Really Evil Rhythms

		List requirements = new ArrayList();

		if ( !hasItem( SKELETON ) && hasItem( KEY_RING ) )
			DEFAULT_SHELL.executeLine( "use skeleton key ring" );

		AdventureDatabase.retrieveItem( SKELETON );
		if ( !hasItem( SKELETON ) )
			requirements.add( SKELETON );

		if ( useCloverForSkeleton )
		{
			AdventureDatabase.retrieveItem( CLOVER );
			if ( !hasItem( CLOVER ) )
				requirements.add( CLOVER );
		}

		if ( !requirements.isEmpty() )
			return requirements;

		while ( KoLmafia.permitsContinue() && !hasItem( RHYTHM ) )
		{
			// The character needs to have at least 50 HP, or 25% of
			// maximum HP (whichever is greater) in order to play
			// the skeleton dice game, UNLESS you have a clover.

			if ( !useCloverForSkeleton && !hasItem( CLOVER ) )
			{
				int healthNeeded = Math.max( KoLCharacter.getMaximumHP() / 4, 50 );
				getClient().recoverHP( healthNeeded + 1 );

				// Verify that you have enough HP to proceed with the
				// skeleton dice game.

				if ( KoLCharacter.getCurrentHP() <= healthNeeded )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You must have more than " + healthNeeded + " HP to proceed." );
					return requirements;
				}
			}

			// Next, handle the form for the skeleton key to
			// get the Really Evil Rhythm. This uses up the
			// clover you had, so process it.

			AdventureDatabase.retrieveItem( SKELETON );
			KoLmafia.updateDisplay( "Inserting skeleton key..." );

			REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
			REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
			REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( SKELETON.getItemID() ) );
			REDIRECT_FOLLOWER.run();

			if ( REDIRECT_FOLLOWER.responseText.indexOf( "prepreaction" ) != -1 )
			{
				REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
				REDIRECT_FOLLOWER.addFormField( "prepreaction", "skel" );
				REDIRECT_FOLLOWER.run();

				if ( hasItem( CLOVER ) )
					getClient().processResult( CLOVER.getNegation() );
			}
		}

		return requirements;
	}

	private static List retrieveStrumming()
	{
		// Decide on which star weapon should be available for
		// this whole process.

		List requirements = new ArrayList();
		AdventureResult starWeapon;

		// See which ones are available

		boolean hasSword = KoLCharacter.hasItem( STAR_SWORD, false );
		boolean hasStaff = KoLCharacter.hasItem( STAR_STAFF, false );
		boolean hasCrossbow = KoLCharacter.hasItem( STAR_CROSSBOW, false );

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

		// Otherwise, pick one that he can
		// create and use

		else if ( canUseSword && hasItem( STAR_SWORD ) )
			starWeapon = STAR_SWORD;

		else if ( canUseStaff && hasItem( STAR_SWORD ) )
			starWeapon = STAR_STAFF;
		else if ( canUseCrossbow && hasItem( STAR_SWORD ) )
			starWeapon = STAR_CROSSBOW;

		// At least pick one that he can use

		else if ( canUseSword )
			starWeapon = STAR_SWORD;
		else if ( canUseStaff )
			starWeapon = STAR_STAFF;
		else if ( canUseCrossbow )
			starWeapon = STAR_CROSSBOW;

		// Otherwise, pick one that he has

		else if ( hasSword )
			starWeapon = STAR_SWORD;
		else if ( hasStaff )
			starWeapon = STAR_STAFF;
		else if ( hasCrossbow )
			starWeapon = STAR_CROSSBOW;

		// What a wimp!

		else
			starWeapon = STAR_SWORD;

		// Star equipment unless you already have Sinister Strummings

		if ( !hasItem( STRUMMING ) )
		{
			AdventureDatabase.retrieveItem( starWeapon );
			if ( !hasItem( starWeapon ) )
				requirements.add( starWeapon );

			AdventureDatabase.retrieveItem( STAR_HAT );
			if ( !hasItem( STAR_HAT ) )
				requirements.add( STAR_HAT );

			AdventureDatabase.retrieveItem( RICHARD );
			if ( !hasItem( RICHARD ) )
				requirements.add( RICHARD );
		}

		if ( hasItem( STRUMMING ) || !requirements.isEmpty() )
			return requirements;

		// If you can't equip the appropriate weapon and buckler,
		// then tell the player they lack the required stats.

		if ( !EquipmentDatabase.canEquip( starWeapon.getName() ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Stats too low to equip a star weapon." );
			return requirements;
		}

		if ( !EquipmentDatabase.canEquip( STAR_HAT.getName() ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Stats too low to equip a star hat." );
			return requirements;
		}

		// Now handle the form for the star key to get
		// the Sinister Strumming.  Note that this will
		// require you to re-equip your star weapon and
		// a star buckler and switch to a starfish first.

		DEFAULT_SHELL.executeLine( "equip " + starWeapon.getName() );
		DEFAULT_SHELL.executeLine( "equip star hat" );
		DEFAULT_SHELL.executeLine( "familiar star starfish" );

		KoLmafia.updateDisplay( "Inserting Richard's star key..." );

		REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
		REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
		REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( RICHARD.getItemID() ) );
		REDIRECT_FOLLOWER.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "prepreaction" ) != -1 )
		{
			REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
			REDIRECT_FOLLOWER.addFormField( "prepreaction", "starcage" );
			REDIRECT_FOLLOWER.run();

			// For unknown reasons, this doesn't always work
			// Error check the possibilities

			// "You beat on the cage with your weapon, but
			// to no avail.	 It doesn't appear to be made
			// out of the right stuff."

			if ( REDIRECT_FOLLOWER.responseText.indexOf( "right stuff" ) != -1 )
				KoLmafia.updateDisplay( ERROR_STATE, "Failed to equip a star weapon." );

			// "A fragment of a line hits you really hard
			// on the arm, and it knocks you back into the
			// main cavern."

			if ( REDIRECT_FOLLOWER.responseText.indexOf( "knocks you back" ) != -1 )
				KoLmafia.updateDisplay( ERROR_STATE, "Failed to equip star buckler." );

			// "Trog creeps toward the pedestal, but is
			// blown backwards.  You give up, and go back
			// out to the main cavern."

			if ( REDIRECT_FOLLOWER.responseText.indexOf( "You give up" ) != -1 )
				KoLmafia.updateDisplay( ERROR_STATE, "Failed to equip star starfish." );
		}

		return requirements;
	}

	private static List retrieveSqueezings()
	{
		// Digital key unless you already have the Squeezings of Woe

		List requirements = new ArrayList();

		if ( !hasItem( SQUEEZINGS ) )
		{
			AdventureDatabase.retrieveItem( DIGITAL );
			if ( !hasItem( DIGITAL ) )
				requirements.add( DIGITAL );
		}

		if ( hasItem( SQUEEZINGS ) || !requirements.isEmpty() )
			return requirements;

		// Now handle the form for the digital key to get
		// the Squeezings of Woe.

		KoLmafia.updateDisplay( "Inserting digital key..." );

		REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
		REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
		REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( DIGITAL.getItemID() ) );
		REDIRECT_FOLLOWER.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "prepreaction" ) != -1 )
		{
			REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
			REDIRECT_FOLLOWER.addFormField( "prepreaction", "sequence" );
			REDIRECT_FOLLOWER.addFormField( "seq1", "up" );
			REDIRECT_FOLLOWER.addFormField( "seq2", "up" );
			REDIRECT_FOLLOWER.addFormField( "seq3", "down" );
			REDIRECT_FOLLOWER.addFormField( "seq4", "down" );
			REDIRECT_FOLLOWER.addFormField( "seq5", "left" );
			REDIRECT_FOLLOWER.addFormField( "seq6", "right" );
			REDIRECT_FOLLOWER.addFormField( "seq7", "left" );
			REDIRECT_FOLLOWER.addFormField( "seq8", "right" );
			REDIRECT_FOLLOWER.addFormField( "seq9", "b" );
			REDIRECT_FOLLOWER.addFormField( "seq10", "a" );
			REDIRECT_FOLLOWER.run();
		}

		return requirements;
	}

	private static List retrieveScubaGear()
	{
		List requirements = new ArrayList();

		// The three hero keys are needed to get the SCUBA gear

		if ( hasItem( SCUBA ) )
			return requirements;

		// Next, handle the three hero keys, which involve
		// answering the riddles with the forms of fish.

		if ( !hasItem( BOWL ) && !hasItem( HOSE_BOWL ) )
		{
			AdventureDatabase.retrieveItem( BORIS );
			if ( !hasItem( BORIS ) )
			{
				KoLmafia.forceContinue();
				requirements.add( BORIS );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Boris's key..." );

				REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
				REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
				REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( BORIS.getItemID() ) );
				REDIRECT_FOLLOWER.run();

				if ( REDIRECT_FOLLOWER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
					REDIRECT_FOLLOWER.addFormField( "prepreaction", "sorcriddle1" );
					REDIRECT_FOLLOWER.addFormField( "answer", "fish" );
					REDIRECT_FOLLOWER.run();
				}
			}
		}

		if ( !hasItem( TANK ) && !hasItem( HOSE_TANK ) )
		{
			AdventureDatabase.retrieveItem( JARLSBERG );
			if ( !hasItem( JARLSBERG ) )
			{
				KoLmafia.forceContinue();
				requirements.add( JARLSBERG );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Jarlsberg's key..." );

				REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
				REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
				REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( JARLSBERG.getItemID() ) );
				REDIRECT_FOLLOWER.run();

				if ( REDIRECT_FOLLOWER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
					REDIRECT_FOLLOWER.addFormField( "prepreaction", "sorcriddle2" );
					REDIRECT_FOLLOWER.addFormField( "answer", "phish" );
					REDIRECT_FOLLOWER.run();
				}
			}
		}

		if ( !hasItem( HOSE ) && !hasItem( HOSE_TANK ) && !hasItem( HOSE_BOWL ) )
		{
			AdventureDatabase.retrieveItem( SNEAKY_PETE );
			if ( !hasItem( SNEAKY_PETE ) )
			{
				KoLmafia.forceContinue();
				requirements.add( SNEAKY_PETE );
			}
			else
			{
				AdventureDatabase.retrieveItem( SNEAKY_PETE );
				KoLmafia.updateDisplay( "Inserting Sneaky Pete's key..." );

				REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
				REDIRECT_FOLLOWER.addFormField( "preaction", "key" );
				REDIRECT_FOLLOWER.addFormField( "whichkey", String.valueOf( SNEAKY_PETE.getItemID() ) );
				REDIRECT_FOLLOWER.run();

				if ( REDIRECT_FOLLOWER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					REDIRECT_FOLLOWER.constructURLString( "lair2.php" );
					REDIRECT_FOLLOWER.addFormField( "prepreaction", "sorcriddle3" );
					REDIRECT_FOLLOWER.addFormField( "answer", "fsh" );
					REDIRECT_FOLLOWER.run();
				}
			}
		}

		// Equip the SCUBA gear.  Attempting to retrieve it
		// will automatically create it.

		if ( hasItem( SCUBA ) )
		{
			AdventureDatabase.retrieveItem( SCUBA );
			DEFAULT_SHELL.executeLine( "equip acc1 makeshift SCUBA gear" );
			KoLmafia.updateDisplay( "Pressing switch beyond odor..." );

			REDIRECT_FOLLOWER.constructURLString( "lair2.php?action=odor" );
			REDIRECT_FOLLOWER.run();
		}

		return requirements;
	}

	public static void completeHedgeMaze()
	{
		if ( !checkPrerequisites( 3, 3 ) )
			return;

		// Retrieve any puzzle pieces that might be sitting
		// inside of the player's closet.

		int closetCount = PUZZLE_PIECE.getCount( closet );
		int inventoryCount = PUZZLE_PIECE.getCount( inventory );

		if ( closetCount > 0 )
			AdventureDatabase.retrieveItem( PUZZLE_PIECE.getInstance( inventoryCount + closetCount ) );

		// Check to see if you've run out of puzzle pieces.
		// If you have, don't bother running the puzzle.

		if ( inventoryCount + closetCount == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Ran out of puzzle pieces." );
			return;
		}

		// Otherwise, check their current state relative
		// to the hedge maze, and begin!

		KoLmafia.updateDisplay( "Retrieving maze status..." );
		REDIRECT_FOLLOWER.constructURLString( "hedgepuzzle.php" );
		REDIRECT_FOLLOWER.run();

		String responseText = REDIRECT_FOLLOWER.responseText;

		// First mission -- retrieve the key from the hedge
		// maze puzzle.

		if ( !inventory.contains( HEDGE_KEY ) )
		{
			KoLmafia.updateDisplay( "Retrieving hedge key..." );
			responseText = retrieveHedgeKey( responseText );

			// Retrieving the key after rotating the puzzle pieces
			// uses an adventure. If we ran out, we canceled.

			if ( !KoLmafia.permitsContinue() )
				return;
		}

		// Second mission -- rotate the hedge maze until
		// the hedge path leads to the hedge door.

		if ( responseText.indexOf( "Click one" ) != -1 )
		{
			KoLmafia.updateDisplay( "Executing final rotations..." );
			responseText = finalizeHedgeMaze( responseText );

			// Navigating up to the tower door after rotating the
			// puzzle pieces requires an adventure. If we ran out,
			// we canceled.

			if ( !KoLmafia.permitsContinue() )
				return;
		}

		// Check to see if you ran out of puzzle pieces
		// in the middle -- if you did, update the user
		// display to say so.

		if ( responseText.indexOf( "Click one" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Ran out of puzzle pieces." );
			return;
		}

		KoLmafia.updateDisplay( "Hedge maze quest complete." );
	}

	private static String rotateHedgePiece( String responseText, String hedgePiece, String searchText )
	{
		// Rotate puzzle sections until we reach our goal
		while ( responseText.indexOf( searchText ) == -1 )
		{
			// We're out of puzzles unless the response says:

			// "Click one of the puzzle sections to rotate that
			// section 90 degrees to the right."

			if ( responseText.indexOf( "Click one" ) == -1 )
				return responseText;

			REDIRECT_FOLLOWER.constructURLString( "hedgepuzzle.php" );
			REDIRECT_FOLLOWER.addFormField( "action", hedgePiece );
			REDIRECT_FOLLOWER.run();

			responseText = REDIRECT_FOLLOWER.responseText;

			// If the topiary golem stole one of your hedge
			// pieces, take it away.

			if ( responseText.indexOf( "Topiary Golem" ) != -1 )
				getClient().processResult( PUZZLE_PIECE.getNegation() );
		}

		return responseText;
	}

	private static String retrieveHedgeKey( String responseText )
	{
		// Before doing anything, check to see if the hedge
		// maze has already been solved for the key.

		if ( responseText.indexOf( "There is a key here." ) == -1 )
			return responseText;

		responseText = rotateHedgePiece( responseText, "3", "Upper-Right Tile: Dead end, exit to the west.  There is a key here." );
		responseText = rotateHedgePiece( responseText, "2", "Upper-Middle Tile: Straight east/west passage." );
		responseText = rotateHedgePiece( responseText, "1", "Upper-Left Tile: 90 degree bend, exits south and east." );
		responseText = rotateHedgePiece( responseText, "4", "Middle-Left Tile: Straight north/south passage." );
		responseText = rotateHedgePiece( responseText, "7", "Lower-Left Tile: 90 degree bend, exits north and east." );
		responseText = rotateHedgePiece( responseText, "8", "Lower-Middle Tile: 90 degree bend, exits south and west." );

		// The hedge maze has been properly rotated!  Now go ahead
		// and retrieve the key from the maze.

		if ( responseText.indexOf( "Click one" ) != -1 )
		{
			REDIRECT_FOLLOWER.constructURLString( "lair3.php" );
			REDIRECT_FOLLOWER.addFormField( "action", "hedge" );
			REDIRECT_FOLLOWER.run();

			if ( REDIRECT_FOLLOWER.responseText.indexOf( "You're out of adventures." ) != -1 )
			{
				// Cancel and return now

				KoLmafia.updateDisplay( ERROR_STATE, "Ran out of adventures." );
				return responseText;
			}

			if ( !REDIRECT_FOLLOWER.needsRefresh )
				CharpaneRequest.getInstance().run();
		}

		return responseText;
	}

	private static String finalizeHedgeMaze( String responseText )
	{
		responseText = rotateHedgePiece( responseText, "2", "Upper-Middle Tile: Straight north/south passage." );
		responseText = rotateHedgePiece( responseText, "5", "Center Tile: 90 degree bend, exits north and east." );
		responseText = rotateHedgePiece( responseText, "6", "Middle-Right Tile: 90 degree bend, exits south and west." );
		responseText = rotateHedgePiece( responseText, "9", "Lower-Right Tile: 90 degree bend, exits north and west." );
		responseText = rotateHedgePiece( responseText, "8", "Lower-Middle Tile: 90 degree bend, exits south and east." );

		// The hedge maze has been properly rotated!  Now go ahead
		// and complete the hedge maze puzzle!

		if ( responseText.indexOf( "Click one" ) != -1 )
		{
			REDIRECT_FOLLOWER.constructURLString( "lair3.php" );
			REDIRECT_FOLLOWER.addFormField( "action", "hedge" );
			REDIRECT_FOLLOWER.run();

			if ( REDIRECT_FOLLOWER.responseText.indexOf( "You're out of adventures." ) != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Ran out of adventures." );
				return responseText;
			}

			// Decrement adventure tally
			if ( !REDIRECT_FOLLOWER.needsRefresh )
				CharpaneRequest.getInstance().run();
		}

		return responseText;
	}

	public static int fightAllTowerGuardians()
	{	return fightTowerGuardians( true );
	}

	public static int fightMostTowerGuardians()
	{	return fightTowerGuardians( false );
	}

	public static int fightTowerGuardians( boolean fightFamiliarGuardians )
	{
		if ( !checkPrerequisites( 4, 6 ) )
			return 0;

		// Make sure that auto-attack is deactivated for the
		// shadow fight, otherwise it will fail.

		String previousAutoAttack = StaticEntity.getProperty( "defaultAutoAttack" );

		if ( !previousAutoAttack.equals( "0" ) )
		{
			REDIRECT_FOLLOWER.constructURLString( "account.php?action=autoattack&whichattack=0" );
			REDIRECT_FOLLOWER.run();
		}

		// Determine which level you actually need to start from.

		KoLmafia.updateDisplay( "Climbing the tower..." );

		REDIRECT_FOLLOWER.constructURLString( "lair4.php" );
		REDIRECT_FOLLOWER.run();

		int currentLevel = 0;

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "lair5.php" ) != -1 )
		{
			// There is a link to higher in the tower.

			REDIRECT_FOLLOWER.constructURLString( "lair5.php" );
			REDIRECT_FOLLOWER.run();

			currentLevel = 3;
		}

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "value=\"level1\"" ) != -1 )
			currentLevel += 1;
		else if ( REDIRECT_FOLLOWER.responseText.indexOf( "value=\"level2\"" ) != -1 )
			currentLevel += 2;
		else if ( REDIRECT_FOLLOWER.responseText.indexOf( "value=\"level3\"" ) != -1 )
			currentLevel += 3;
		else
			currentLevel += 4;

		int requiredItemID = -1;
		for ( int towerLevel = currentLevel; KoLCharacter.getAdventuresLeft() > 0 && KoLmafia.permitsContinue() && towerLevel <= 6; ++towerLevel )
		{
			requiredItemID = fightGuardian( towerLevel );
			CharpaneRequest.getInstance().run();

			getClient().runBetweenBattleChecks( false );

			if ( requiredItemID != -1 )
			{
				resetAutoAttack( previousAutoAttack );
				return requiredItemID;
			}
		}

		// You must have at least 70 in all stats before you can enter
		// the chamber.

		if ( KoLCharacter.getBaseMuscle() < 70 || KoLCharacter.getBaseMysticality() < 70 || KoLCharacter.getBaseMoxie() < 70 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't enter the chamber unless all base stats are 70 or higher." );
			resetAutoAttack( previousAutoAttack );
			return -1;
		}

		// Figure out how far he's gotten into the Sorceress's Chamber
		REDIRECT_FOLLOWER.constructURLString( "lair6.php" );
		REDIRECT_FOLLOWER.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "ascend.php" ) != -1 )
		{
			KoLmafia.updateDisplay( "You've already beaten Her Naughtiness." );
			return -1;
		}

		int n = -1;
		FamiliarData originalFamiliar = KoLCharacter.getFamiliar();

		Matcher placeMatcher = LAIR6_PATTERN.matcher( REDIRECT_FOLLOWER.responseText );
		if ( placeMatcher.find() )
			n = parseInt( placeMatcher.group(1) );

		if ( n < 0 )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Server-side change detected.  Script aborted." );
			resetAutoAttack( previousAutoAttack );
			return -1;
		}

		if ( n == 0 )
		{
			// We know that all base stats are at least 70. But if
			// we attained that goal this session without
			// charpane.php having been invoked, KoL itself
			// sometimes doesn't realize it and will complain
			// "You're not tough enough to fight up here."

			CharpaneRequest.getInstance().run();
		}

		for ( ; n < 5 && KoLmafia.permitsContinue(); ++n )
		{
			switch ( n )
			{
				case 0:
					findDoorCode();
					break;
				case 1:
					reflectEnergyBolt();
					break;
				case 2:

					if ( !fightFamiliarGuardians )
					{
						KoLmafia.updateDisplay( "Path to shadow cleared." );
						resetAutoAttack( previousAutoAttack );
						return -1;
					}

					fightShadow();
					break;

				case 3:

					if ( !fightFamiliarGuardians )
					{
						KoLmafia.updateDisplay( "Path to shadow cleared." );
						resetAutoAttack( previousAutoAttack );
						return -1;
					}

					familiarBattle(3);
					break;

				case 4:

					if ( !fightFamiliarGuardians )
					{
						KoLmafia.updateDisplay( "Path to shadow cleared." );
						resetAutoAttack( previousAutoAttack );
						return -1;
					}

					familiarBattle(4);
					break;
			}

			if ( !KoLmafia.permitsContinue() )
			{
				resetAutoAttack( previousAutoAttack );
				return -1;
			}
		}

		DEFAULT_SHELL.executeLine( "familiar " + originalFamiliar.getRace() );
		KoLmafia.updateDisplay( "Her Naughtiness awaits." );
		resetAutoAttack( previousAutoAttack );

		return -1;
	}

	private static void resetAutoAttack( String previousAutoAttack )
	{
		if ( !previousAutoAttack.equals( "0" ) )
			(new KoLRequest( "account.php?action=autoattack&whichattack=" + previousAutoAttack )).run();
	}

	private static int fightGuardian( int towerLevel )
	{
		if ( KoLCharacter.getAdventuresLeft() == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You're out of adventures." );
			return -1;
		}

		KoLmafia.updateDisplay( "Fighting guardian on level " + towerLevel + " of the tower..." );

		// Boldly climb the stairs.

		REDIRECT_FOLLOWER.constructURLString( towerLevel <= 3 ? "lair4.php" : "lair5.php" );
		REDIRECT_FOLLOWER.addFormField( "action", "level" + ((towerLevel - 1) % 3 + 1) );
		REDIRECT_FOLLOWER.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "You don't have time to mess around in the Tower." ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You're out of adventures." );
			return -1;
		}

		// Parse response to see which item we need.
		AdventureResult guardianItem = getGuardianItem( REDIRECT_FOLLOWER.responseText );

		// With the guardian item retrieved, check to see if you have
		// the item, and if so, use it and report success.  Otherwise,
		// run away and report failure.

		REDIRECT_FOLLOWER.constructURLString( "fight.php" );

		if ( inventory.contains( guardianItem ) )
		{
			REDIRECT_FOLLOWER.addFormField( "action", "useitem" );
			REDIRECT_FOLLOWER.addFormField( "whichitem", String.valueOf( guardianItem.getItemID() ) );
			REDIRECT_FOLLOWER.run();

			// Use up the item

			getClient().processResult( guardianItem.getNegation() );
			return -1;
		}

		// Since we don't have the item, run away

		REDIRECT_FOLLOWER.addFormField( "action", "runaway" );
		REDIRECT_FOLLOWER.run();

		AdventureDatabase.retrieveItem( guardianItem );
		if ( guardianItem.getCount( inventory ) != 0 )
			return fightGuardian( towerLevel );

		return guardianItem.getItemID();
	}

	private static AdventureResult getGuardianItem( String fightText )
	{
		for ( int i = 0; i < GUARDIAN_DATA.length; ++i)
			if ( fightText.indexOf( GUARDIAN_DATA[i][0] ) != -1 )
				return new AdventureResult( GUARDIAN_DATA[i][1], 1 );

		// Shouldn't get here.

		KoLmafia.updateDisplay( ABORT_STATE, "Server-side change detected.  Script aborted." );
		return new AdventureResult( 666, 1 );
	}

	private static void findDoorCode()
	{
		KoLmafia.updateDisplay( "Cracking door code..." );

		// Enter the chamber

		REDIRECT_FOLLOWER.constructURLString( "lair6.php" );
		REDIRECT_FOLLOWER.addFormField( "place", "0" );
		REDIRECT_FOLLOWER.run();

		// Talk to the guards

		REDIRECT_FOLLOWER.constructURLString( "lair6.php" );
		REDIRECT_FOLLOWER.addFormField( "place", "0" );
		REDIRECT_FOLLOWER.addFormField( "preaction", "lightdoor" );
		REDIRECT_FOLLOWER.run();

		// Crack the code

		String code = deduceCode( REDIRECT_FOLLOWER.responseText );

		if ( code == null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Couldn't solve door code. Do it yourself and come back!" );
			return;
		}

		REDIRECT_FOLLOWER.constructURLString( "lair6.php" );
		REDIRECT_FOLLOWER.addFormField( "place", "0" );
		REDIRECT_FOLLOWER.addFormField( "action", "doorcode" );
		REDIRECT_FOLLOWER.addFormField( "code", code );
		REDIRECT_FOLLOWER.run();

		// Check for success

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "the door slides open" ) == -1 )
			KoLmafia.updateDisplay( ERROR_STATE, "I used the wrong code. Sorry." );
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
		AdventureDatabase.retrieveItem( SHARD );

		// Get current equipment
		AdventureResult initialWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		AdventureResult initialOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );

		// Unequip a ranged off-hand weapon
		if ( EquipmentDatabase.isRanged( initialOffhand.getName() ) )
			DEFAULT_SHELL.executeLine( "unequip off-hand" );

		// Equip the huge mirror shard
		if ( initialWeapon != null && !initialWeapon.equals( "huge mirror shard" ) )
			DEFAULT_SHELL.executeLine( "equip huge mirror shard" );

		// Reflect the energy bolt
		KoLmafia.updateDisplay( "Reflecting energy bolt..." );
		REDIRECT_FOLLOWER.constructURLString( "lair6.php" );
		REDIRECT_FOLLOWER.addFormField( "place", "1" );
		REDIRECT_FOLLOWER.run();

		// If we unequipped a weapon, equip it again
		if ( initialWeapon != null && !initialWeapon.equals( KoLCharacter.getEquipment( KoLCharacter.WEAPON ) ) )
			DEFAULT_SHELL.executeLine( "equip weapon " + initialWeapon.getName() );

		// If we unequipped an off-hand weapon, equip it again
		if ( initialOffhand != null && !initialOffhand.equals( KoLCharacter.getEquipment( KoLCharacter.OFFHAND ) ) )
			DEFAULT_SHELL.executeLine( "equip off-hand " + initialWeapon.getName() );
	}

	private static int getShadowBattleHealth( int shadowDamage, int healAmount )
	{
		int combatRounds = (int) Math.ceil( 96.0f / ((float) healAmount) ) - 1;
		int neededHealth = shadowDamage + Math.max( shadowDamage - healAmount, 0 ) * combatRounds;
		return neededHealth + 1;
	}

	private static void fightShadow()
	{
		List requirements = new ArrayList();

		// In order to see what happens, we calculate the health needed
		// to survive the shadow fight using red pixel potions.  We use
		// worst-case scenario in all cases (minimum recovery, maximum
		// damage, which may happen).

		int shadowDamage = 22 + ((int)Math.floor( KoLCharacter.getMaximumHP() / 5 )) + 3;

		AdventureResult option = RED_POTION;
		int neededHealth = getShadowBattleHealth( shadowDamage, 25 );

		// If the person has red plastic oyster eggs, then they are an
		// alternative if the person can't survive using red pixel potions.

		if ( neededHealth > KoLCharacter.getMaximumHP() )
		{
			if ( hasItem( PLASTIC_EGG ) )
			{
				option = PLASTIC_EGG;
				neededHealth = getShadowBattleHealth( shadowDamage, 35 );
			}
		}

		// In the event that you have Ambidextrous Funkslinging, then
		// always rely on elixirs.

		if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			option = DOC_ELIXIR;
			neededHealth = getShadowBattleHealth( shadowDamage, 36 );
		}

		// Make sure you can get enough health for the shadow
		// fight.  If not, then abort.

		if ( neededHealth > KoLCharacter.getMaximumHP() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The shadow fight is too dangerous with " + KoLCharacter.getMaximumHP() + " maximum HP." );
			return;
		}

		// Now, we validate against the requirements by seeing
		// if we have the item to use against the shadow.

		requirements.add( option );
		if ( !getClient().checkRequirements( requirements ) )
			return;

		getClient().recoverHP( neededHealth );
		if ( KoLCharacter.getCurrentHP() < neededHealth )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You must have at least " + neededHealth + " HP to fight your shadow." );
			return;
		}

		KoLmafia.updateDisplay( "Fighting your shadow..." );

		// Start the battle!

		String oldAction = getProperty( "battleAction" );
		setProperty( "battleAction", "item " + option.getName().toLowerCase() );

		KoLRequest combat = new KoLRequest( "lair6.php" );
		combat.addFormField( "place", "2" );
		combat.run();

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "You don't have time to mess around up here." ) != -1 )
			KoLmafia.updateDisplay( ERROR_STATE, "You're out of adventures." );

		// Reset all of the old battle action settings, including
		// the original KoL-side auto-attack.

		setProperty( "battleAction", oldAction );
	}

	public static void makeGuardianItems()
	{
		for ( int i = 0; i < GUARDIAN_DATA.length; ++i )
		{
			AdventureResult item = new AdventureResult( GUARDIAN_DATA[i][1], 1, false );
			if ( !inventory.contains( item ) )
			{
				if ( KoLCharacter.hasItem( item, true ) || NPCStoreDatabase.contains( GUARDIAN_DATA[i][1] ) )
					AdventureDatabase.retrieveItem( item );
			}
		}
	}

	private static void familiarBattle( int n )
	{	familiarBattle( n, true );
	}

	private static void familiarBattle( int n, boolean requiresHeal )
	{
		// Make sure that the familiar is at least twenty pounds.
		// Otherwise, it's a wasted REDIRECT_FOLLOWER.

		FamiliarData originalFamiliar = KoLCharacter.getFamiliar();
		if ( originalFamiliar == null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a familiar equipped." );
			return;
		}

		// Ensure that the player has more than 50 HP, since
		// you cannot enter the familiar chamber with less.

		if ( requiresHeal )
		{
			getClient().recoverHP( 51 );

			// Need more than 50 hit points.  Abort if this is
			// not the case.

			if ( KoLCharacter.getCurrentHP() <= 50 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You must have more than 50 HP to proceed." );
				return;
			}
		}

		// Make sure that the current familiar is at least twenty
		// pounds, if it's one of the ones which can be used against
		// the tower familiars; otherwise, it won't survive.

		boolean isPotentialFamiliar = false;

		for ( int i = 0; i < FAMILIAR_DATA.length; ++i )
			if ( originalFamiliar.getRace().equals( FAMILIAR_DATA[i][1] ) )
				isPotentialFamiliar = true;

		boolean shouldFaceFamiliar = !isPotentialFamiliar || FamiliarTrainingFrame.buffFamiliar( 20 );

		KoLmafia.updateDisplay( "Facing giant familiar..." );
		REDIRECT_FOLLOWER.constructURLString( "lair6.php" );
		REDIRECT_FOLLOWER.addFormField( "place", String.valueOf( n ) );
		REDIRECT_FOLLOWER.run();

		// If you do not successfully pass the familiar, you
		// will get a "stomp off in a huff" message.

		if ( REDIRECT_FOLLOWER.responseText.indexOf( "stomp off in a huff" ) == -1 )
			return;

		// Find the necessary familiar and see if the player has one.

		String race = "";
		FamiliarData familiar = null;
		for ( int i = 0; i < FAMILIAR_DATA.length; ++i )
		{
			if ( REDIRECT_FOLLOWER.responseText.indexOf( FAMILIAR_DATA[i][0] ) != -1 )
			{
				race = FAMILIAR_DATA[i][1];
				familiar = KoLCharacter.findFamiliar( race );
				break;
			}
		}

		// If not, tell the player to get one and come back.

		if ( familiar == null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Come back with a 20 pound " + race );
			return;
		}

		// Switch to the required familiar
		if ( originalFamiliar != familiar )
			(new FamiliarRequest( familiar )).run();

		// If we can buff it to 20 pounds, try again.
		if ( !FamiliarTrainingFrame.buffFamiliar( 20 ) )
		{
			// We can't buff it high enough. Train it.
			if ( !FamiliarTrainingFrame.levelFamiliar( 20, FamiliarTrainingFrame.BUFFED, false, false ) )
				return;
		}

		// We're good to go. Fight!
		familiarBattle( n, false );
	}
}
