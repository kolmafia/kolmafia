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
import java.io.BufferedReader;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * A static class which retrieves all the concoctions available in
 * the Kingdom of Loathing.  This class technically uses up a lot
 * more memory than it needs to because it creates an array storing
 * all possible item combinations, but that's okay!  Because it's
 * only temporary.  Then again, this is supposedly true of all the
 * flow-control using exceptions, but that hasn't been changed.
 */

public class ConcoctionsDatabase extends KoLDatabase
{
	public static final SortedListModel concoctionsList = new SortedListModel();

	private static ConcoctionArray concoctions = new ConcoctionArray();
	private static SortedListModelArray knownUses = new SortedListModelArray();

	public static final int METHOD_COUNT = ItemCreationRequest.METHOD_COUNT;
	private static boolean [] PERMIT_METHOD = new boolean[ METHOD_COUNT ];
	private static int [] ADVENTURE_USAGE = new int[ METHOD_COUNT ];

	private static final int CHEF = 438;
	private static final int CLOCKWORK_CHEF = 1112;
	private static final int BARTENDER = 440;
	private static final int CLOCKWORK_BARTENDER = 1111;

	private static final AdventureResult OVEN = new AdventureResult( 157, 1 );
	private static final AdventureResult KIT = new AdventureResult( 236, 1 );
	public static final AdventureResult HAMMER = new AdventureResult( 338, 1 );
	private static final AdventureResult PLIERS = new AdventureResult( 709, 1 );

	private static final AdventureResult PASTE = new AdventureResult( ItemCreationRequest.MEAT_PASTE, 1 );
	private static final AdventureResult STACK = new AdventureResult( ItemCreationRequest.MEAT_STACK, 1 );
	private static final AdventureResult DENSE = new AdventureResult( ItemCreationRequest.DENSE_STACK, 1 );

	private static final AdventureResult ROLLING_PIN = new AdventureResult( 873, 1 );
	private static final AdventureResult UNROLLING_PIN = new AdventureResult( 873, 1 );

	private static final int TOMATO = 246;
	private static final int DOUGH = 159;
	private static final int FLAT_DOUGH = 301;

	private static final AdventureResult DYSPEPSI = new AdventureResult( 347, 1 );
	private static final AdventureResult CLOACA = new AdventureResult( 1334, 1 );
	private static final AdventureResult SCHLITZ = new AdventureResult( 41, 1 );
	private static final AdventureResult WILLER = new AdventureResult( 81, 1 );
	private static final AdventureResult KETCHUP = new AdventureResult( 106, 1 );
	private static final AdventureResult CATSUP = new AdventureResult( 107, 1 );

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "concoctions.dat" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			try
			{
				if ( data.length > 2 )
				{
					AdventureResult item = AdventureResult.parseResult( data[0] );
					int itemID = item.getItemID();

					if ( itemID != -1 )
					{
						int mixingMethod = StaticEntity.parseInt( data[1] );
						Concoction concoction = new Concoction( item, mixingMethod );

						for ( int i = 2; i < data.length; ++i )
							concoction.addIngredient( parseIngredient( data[i] ) );

						if ( !concoction.isBadRecipe() )
						{
							concoctions.set( itemID, concoction );
							continue;
						}
					}

					// Bad item or bad ingredients
					System.out.println( "Bad recipe: " + data[0] );
				}
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final boolean isKnownCombination( AdventureResult [] ingredients )
	{
		// Known combinations which could not be added because
		// there are limitations in the item manager.

		if ( ingredients.length == 2 )
		{
			// Handle meat stacks, which are created from fairy
			// gravy and meat from yesterday.

			if ( ingredients[0].getItemID() == 80 && ingredients[1].getItemID() == 87 )
				return true;
			if ( ingredients[1].getItemID() == 80 && ingredients[0].getItemID() == 87 )
				return true;

			// Handle plain pizza, which also allows flat dough
			// to be used instead of wads of dough.

			if ( ingredients[0].getItemID() == TOMATO && ingredients[1].getItemID() == FLAT_DOUGH )
				return true;
			if ( ingredients[1].getItemID() == TOMATO && ingredients[0].getItemID() == FLAT_DOUGH )
				return true;

			// Handle catsup recipes, which only exist in the
			// item table as ketchup recipes.

			if ( ingredients[0].getItemID() == CATSUP.getItemID() )
			{
				ingredients[0] = KETCHUP;
				return isKnownCombination( ingredients );
			}
			if ( ingredients[1].getItemID() == CATSUP.getItemID() )
			{
				ingredients[1] = KETCHUP;
				return isKnownCombination( ingredients );
			}

			// Handle ice-cold beer recipes, which only uses the
			// recipe for item #41 at this time.

			if ( ingredients[0].getItemID() == WILLER.getItemID() )
			{
				ingredients[0] = SCHLITZ;
				return isKnownCombination( ingredients );
			}
			if ( ingredients[1].getItemID() == WILLER.getItemID() )
			{
				ingredients[1] = SCHLITZ;
				return isKnownCombination( ingredients );
			}

			// Handle cloaca recipes, which only exist in the
			// item table as dyspepsi cola.

			if ( ingredients[0].getItemID() == CLOACA.getItemID() )
			{
				ingredients[0] = DYSPEPSI;
				return isKnownCombination( ingredients );
			}
			if ( ingredients[1].getItemID() == CLOACA.getItemID() )
			{
				ingredients[1] = DYSPEPSI;
				return isKnownCombination( ingredients );
			}
		}

		int [] ingredientTestIDs;
		AdventureResult [] ingredientTest;

		for ( int i = 0; i < concoctions.size(); ++i )
		{
			ingredientTest = concoctions.get(i).getIngredients();
			if ( ingredientTest.length != ingredients.length )
				continue;

			ingredientTestIDs = new int[ ingredients.length ];
			for ( int j = 0; j < ingredientTestIDs.length; ++j )
				ingredientTestIDs[j] = ingredientTest[j].getItemID();

			boolean foundMatch = true;
			for ( int j = 0; j < ingredients.length && foundMatch; ++j )
			{
				foundMatch = false;
				for ( int k = 0; k < ingredientTestIDs.length && !foundMatch; ++k )
				{
					foundMatch |= ingredients[j].getItemID() == ingredientTestIDs[k];
					if ( foundMatch )  ingredientTestIDs[k] = -1;
				}
			}

			if ( foundMatch )
				return true;
		}

		return false;
	}

	public static final SortedListModel getKnownUses( AdventureResult item )
	{	return knownUses.get( item.getItemID() );
	}

	public static final boolean isPermittedMethod( int method )
	{	return PERMIT_METHOD[ method ];
	}

	private static AdventureResult parseIngredient( String data )
	{
		// If the ingredient is specified inside of brackets,
		// then a specific item ID is being designated.

		if ( data.startsWith( "[" ) )
		{
			int closeBracketIndex = data.indexOf( "]" );
			String itemIDString = data.substring( 0, closeBracketIndex ).replaceAll( "[\\[\\]]", "" ).trim();
			String quantityString = data.substring( closeBracketIndex + 1 ).trim();

			return new AdventureResult( StaticEntity.parseInt( itemIDString ), quantityString.length() == 0 ? 1 :
				StaticEntity.parseInt( quantityString.replaceAll( "[\\(\\)]", "" ) ) );
		}

		// Otherwise, it's a standard ingredient - use
		// the standard adventure result parsing routine.

		return AdventureResult.parseResult( data );
	}

	public static synchronized SortedListModel getConcoctions()
	{	return concoctionsList;
	}

	/**
	 * Returns the concoctions which are available given the list of
	 * ingredients.  The list returned contains formal requests for
	 * item creation.
	 */

	public static synchronized void refreshConcoctions()
	{
		List availableIngredients = new ArrayList();
		availableIngredients.addAll( KoLCharacter.getInventory() );

		boolean showClosetDrivenCreations = getProperty( "showClosetDrivenCreations" ).equals( "true" );

		if ( showClosetDrivenCreations )
		{
			List closetList = (List) KoLCharacter.getCloset();
			for ( int i = 0; i < closetList.size(); ++i )
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) closetList.get(i) );
		}

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		for ( int i = 1; i < concoctions.size(); ++i )
			concoctions.get(i).resetCalculations();

		// Make assessment of availability of mixing methods.
		// This method will also calculate the availability of
		// chefs and bartenders automatically so a second call
		// is not needed.

		cachePermitted( availableIngredients );

		// Next, do calculations on all mixing methods which cannot
		// be created at this time.

		for ( int i = 1; i < concoctions.size(); ++i )
		{
			Concoction current = concoctions.get(i);
			if ( !isPermittedMethod( current.getMixingMethod() ) )
			{
				current.initial = current.concoction.getCount( availableIngredients );
				current.creatable = 0;
				current.total = current.initial;
			}
		}

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		concoctions.get(0).total = KoLCharacter.getAdventuresLeft();
		calculateMeatCombines( availableIngredients );

		// Ice-cold beer and ketchup are special instances -- for the
		// purposes of calculation, we assume that they will use the
		// ingredient which is present in the greatest quantity.

		int availableSoda = getBetterIngredient( DYSPEPSI, CLOACA, availableIngredients ).getCount( availableIngredients );

		concoctions.get( DYSPEPSI.getItemID() ).initial = availableSoda;
		concoctions.get( DYSPEPSI.getItemID() ).creatable = 0;
		concoctions.get( DYSPEPSI.getItemID() ).total = availableSoda;

		concoctions.get( CLOACA.getItemID() ).initial = availableSoda;
		concoctions.get( CLOACA.getItemID() ).creatable = 0;
		concoctions.get( CLOACA.getItemID() ).total = availableSoda;

		int availableBeer = getBetterIngredient( SCHLITZ, WILLER, availableIngredients ).getCount( availableIngredients );

		concoctions.get( SCHLITZ.getItemID() ).initial = availableBeer;
		concoctions.get( SCHLITZ.getItemID() ).creatable = 0;
		concoctions.get( SCHLITZ.getItemID() ).total = availableBeer;

		concoctions.get( WILLER.getItemID() ).initial = availableBeer;
		concoctions.get( WILLER.getItemID() ).creatable = 0;
		concoctions.get( WILLER.getItemID() ).total = availableBeer;

		int availableKetchup = getBetterIngredient( KETCHUP, CATSUP, availableIngredients ).getCount( availableIngredients );

		concoctions.get( KETCHUP.getItemID() ).initial = availableKetchup;
		concoctions.get( KETCHUP.getItemID() ).creatable = 0;
		concoctions.get( KETCHUP.getItemID() ).total = availableKetchup;

		concoctions.get( CATSUP.getItemID() ).initial = availableKetchup;
		concoctions.get( CATSUP.getItemID() ).creatable = 0;
		concoctions.get( CATSUP.getItemID() ).total = availableKetchup;

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < concoctions.size(); ++i )
			if ( concoctions.get(i).toString() != null )
				concoctions.get(i).calculate( availableIngredients );

		// Now, to update the list of creatables without removing
		// all creatable items.  We do this by determining the
		// number of items inside of the old list.

		for ( int i = 1; i < concoctions.size(); ++i )
		{
			// We can't make this concoction now

			if ( concoctions.get(i).creatable <= 0 )
			{
				if ( concoctions.get(i).wasPossible() )
				{
					concoctionsList.remove( ItemCreationRequest.getInstance( client, i, 0, false ) );
					concoctions.get(i).setPossible( false );
				}
			}
			else
			{
				// We can make the concoction now
				ItemCreationRequest currentCreation = ItemCreationRequest.getInstance( client, i, concoctions.get(i).creatable );

				if ( concoctions.get(i).wasPossible() )
				{
					if ( currentCreation.getCount( concoctionsList ) != concoctions.get(i).creatable )
					{
						concoctionsList.remove( currentCreation );
						concoctionsList.add( currentCreation );
					}
				}
				else
				{
					concoctionsList.add( currentCreation );
					concoctions.get(i).setPossible( true );
				}
			}
		}
	}

	private static void calculateMeatCombines( List availableIngredients )
	{
		// Meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.

		concoctions.get( PASTE.getItemID() ).initial = PASTE.getCount( availableIngredients );
		concoctions.get( PASTE.getItemID() ).creatable = KoLCharacter.getAvailableMeat() / 10;
		concoctions.get( PASTE.getItemID() ).total = concoctions.get( PASTE.getItemID() ).initial + concoctions.get( PASTE.getItemID() ).creatable;

		concoctions.get( STACK.getItemID() ).initial = STACK.getCount( availableIngredients );
		concoctions.get( STACK.getItemID() ).creatable = KoLCharacter.getAvailableMeat() / 100;
		concoctions.get( STACK.getItemID() ).total = concoctions.get( STACK.getItemID() ).initial + concoctions.get( STACK.getItemID() ).creatable;

		concoctions.get( DENSE.getItemID() ).initial = DENSE.getCount( availableIngredients );
		concoctions.get( DENSE.getItemID() ).creatable = KoLCharacter.getAvailableMeat() / 1000;
		concoctions.get( DENSE.getItemID() ).total = concoctions.get( DENSE.getItemID() ).initial + concoctions.get( DENSE.getItemID() ).creatable;
	}

	/**
	 * Utility method used to cache the current permissions on
	 * item creation, based on the given client.
	 */

	private static void cachePermitted( List availableIngredients )
	{
		calculateMeatCombines( availableIngredients );

		// It is never possible to create items which are flagged
		// NOCREATE, and it is always possible to create items
		// through meat paste combination.

		PERMIT_METHOD[ ItemCreationRequest.NOCREATE ] = false;
		ADVENTURE_USAGE[ ItemCreationRequest.NOCREATE ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.COMBINE ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.COMBINE ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.CLOVER ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.CLOVER ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.CATALYST ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.CATALYST ] = 0;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		PERMIT_METHOD[ ItemCreationRequest.TINKER ] = KoLCharacter.inMoxieSign();
		ADVENTURE_USAGE[ ItemCreationRequest.TINKER ] = 0;

		// Smithing of items is possible whenever the person
		// has a hammer.

		PERMIT_METHOD[ ItemCreationRequest.SMITH ] = KoLCharacter.getInventory().contains( HAMMER );

		// Advanced smithing is available whenever the person can
		// smith and has access to the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.SMITH_WEAPON ] = PERMIT_METHOD[ ItemCreationRequest.SMITH ] && KoLCharacter.canSmithWeapons();
		ADVENTURE_USAGE[ ItemCreationRequest.SMITH_WEAPON ] = 1;

		PERMIT_METHOD[ ItemCreationRequest.SMITH_ARMOR ] = PERMIT_METHOD[ ItemCreationRequest.SMITH ] && KoLCharacter.canSmithArmor();
		ADVENTURE_USAGE[ ItemCreationRequest.SMITH_ARMOR ] = 1;

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.inMuscleSign() )
		{
			PERMIT_METHOD[ ItemCreationRequest.SMITH ] = true;
			ADVENTURE_USAGE[ ItemCreationRequest.SMITH ] = 0;
		}
		else
			ADVENTURE_USAGE[ ItemCreationRequest.SMITH ] = 1;

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		PERMIT_METHOD[ ItemCreationRequest.JEWELRY ] = KoLCharacter.getInventory().contains( PLIERS );
		ADVENTURE_USAGE[ ItemCreationRequest.JEWELRY ] = 3;

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		PERMIT_METHOD[ ItemCreationRequest.STARCHART ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.STARCHART ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.PIXEL ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.PIXEL ] = 0;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		PERMIT_METHOD[ ItemCreationRequest.ROLLING_PIN ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.ROLLING_PIN ] = 0;

		// It's not possible to ask Uncle Crimbo to make toys

		PERMIT_METHOD[ ItemCreationRequest.TOY ] = false;
		ADVENTURE_USAGE[ ItemCreationRequest.TOY ] = 0;

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		boolean noServantNeeded = getProperty( "createWithoutBoxServants" ).equals( "true" );

		concoctions.get( CHEF ).calculate( availableIngredients );
		concoctions.get( CLOCKWORK_CHEF ).calculate( availableIngredients );
		concoctions.get( BARTENDER ).calculate( availableIngredients );
		concoctions.get( CLOCKWORK_BARTENDER ).calculate( availableIngredients );

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		PERMIT_METHOD[ ItemCreationRequest.COOK ] = isAvailable( CHEF, CLOCKWORK_CHEF );

		if ( !PERMIT_METHOD[ ItemCreationRequest.COOK ] && noServantNeeded && KoLCharacter.getInventory().contains( OVEN ) )
		{
			PERMIT_METHOD[ ItemCreationRequest.COOK ] = true;
			ADVENTURE_USAGE[ ItemCreationRequest.COOK ] = 1;
		}
		else
			ADVENTURE_USAGE[ ItemCreationRequest.COOK ] = 0;

		// Cooking of reagents and noodles is possible whenever
		// the person can cook and has the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.COOK_REAGENT ] = PERMIT_METHOD[ ItemCreationRequest.COOK ] && KoLCharacter.canSummonReagent();
		ADVENTURE_USAGE[ ItemCreationRequest.COOK_REAGENT ] = ADVENTURE_USAGE[ ItemCreationRequest.COOK ];

		PERMIT_METHOD[ ItemCreationRequest.SUPER_REAGENT ] = PERMIT_METHOD[ ItemCreationRequest.COOK ] && KoLCharacter.hasSkill( "The Way of Sauce" );
;
		ADVENTURE_USAGE[ ItemCreationRequest.SUPER_REAGENT ] = ADVENTURE_USAGE[ ItemCreationRequest.COOK ];

		PERMIT_METHOD[ ItemCreationRequest.COOK_PASTA ] = PERMIT_METHOD[ ItemCreationRequest.COOK ] && KoLCharacter.canSummonNoodles();
		ADVENTURE_USAGE[ ItemCreationRequest.COOK_PASTA ] = ADVENTURE_USAGE[ ItemCreationRequest.COOK ];

		// Mixing is possible whenever the person has a bartender
		// or they don't need a box servant and have a kit.

		PERMIT_METHOD[ ItemCreationRequest.MIX ] = isAvailable( BARTENDER, CLOCKWORK_BARTENDER );

		if ( !PERMIT_METHOD[ ItemCreationRequest.MIX ] && noServantNeeded && KoLCharacter.getInventory().contains( KIT ) )
		{
			PERMIT_METHOD[ ItemCreationRequest.MIX ] = true;
			ADVENTURE_USAGE[ ItemCreationRequest.MIX ] = 1;
		}
		else
			ADVENTURE_USAGE[ ItemCreationRequest.MIX ] = 0;

		// Mixing of advanced drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.MIX_SPECIAL ] = PERMIT_METHOD[ ItemCreationRequest.MIX ] && KoLCharacter.canSummonShore();
		ADVENTURE_USAGE[ ItemCreationRequest.MIX_SPECIAL ] = ADVENTURE_USAGE[ ItemCreationRequest.MIX ];

		// Mixing of super-special drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.MIX_SUPER ] = PERMIT_METHOD[ ItemCreationRequest.MIX ] && KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" );
		ADVENTURE_USAGE[ ItemCreationRequest.MIX_SUPER ] = ADVENTURE_USAGE[ ItemCreationRequest.MIX ];

		// Using Crosby Nash's Still is possible if the person has
		// Superhuman Cocktailcrafting and is a Moxie class character.

		boolean hasStillsAvailable = KoLCharacter.getStillsAvailable() > 0;
		PERMIT_METHOD[ ItemCreationRequest.STILL_MIXER ] = hasStillsAvailable;
		ADVENTURE_USAGE[ ItemCreationRequest.STILL_MIXER ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.STILL_BOOZE ] = hasStillsAvailable;
		ADVENTURE_USAGE[ ItemCreationRequest.STILL_BOOZE ] = 0;

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		PERMIT_METHOD[ ItemCreationRequest.WOK ] = KoLCharacter.canUseWok();
		ADVENTURE_USAGE[ ItemCreationRequest.WOK ] = 1;

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		PERMIT_METHOD[ ItemCreationRequest.MALUS ] = KoLCharacter.canUseMalus();
		ADVENTURE_USAGE[ ItemCreationRequest.MALUS ] = 0;
	}

	private static boolean isAvailable( int servantID, int clockworkID )
	{
		// If it's a base case, return whether or not the
		// servant is already available at the camp.

		if ( servantID == CHEF && KoLCharacter.hasChef() )
			return true;
		if ( servantID == BARTENDER && KoLCharacter.hasBartender() )
			return true;

		// If the user did not wish to repair their boxes
		// on explosion, then the box servant is not available

		if ( getProperty( "autoRepairBoxes" ).equals( "false" ) )
			return false;

		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		return concoctions.get( servantID ).total > 0 || concoctions.get( clockworkID ).total > 0;
	}

	/**
	 * Returns the mixing method for the item with the given ID.
	 */

	public static int getMixingMethod( int itemID )
	{	return concoctions.get( itemID ).getMixingMethod();
	}

	/**
	 * Returns the item IDs of the ingredients for the given item.
	 * Note that if there are no ingredients, then <code>null</code>
	 * will be returned instead.
	 */

	public static AdventureResult [] getIngredients( int itemID )
	{
		List availableIngredients = new ArrayList();
		availableIngredients.addAll( KoLCharacter.getInventory() );

		boolean showClosetDrivenCreations = getProperty( "showClosetDrivenCreations" ).equals( "true" );

		if ( showClosetDrivenCreations )
		{
			List closetList = (List) KoLCharacter.getCloset();
			for ( int i = 0; i < closetList.size(); ++i )
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) closetList.get(i) );
		}

		// Ensure that you're retrieving the same ingredients that
		// were used in the calculations.  Usually this is the case,
		// but ice-cold beer and ketchup are tricky cases.

		AdventureResult [] ingredients = concoctions.get( itemID ).getIngredients();

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( ingredients[i].getItemID() == SCHLITZ.getItemID() || ingredients[i].getItemID() == WILLER.getItemID() )
				ingredients[i] = getBetterIngredient( SCHLITZ, WILLER, availableIngredients );
			else if ( ingredients[i].getItemID() == KETCHUP.getItemID() || ingredients[i].getItemID() == CATSUP.getItemID() )
				ingredients[i] = getBetterIngredient( KETCHUP, CATSUP, availableIngredients );
		}

		return ingredients;
	}

	public static boolean hasAnyIngredient( int itemID )
	{
		boolean hasOneIngredient = false;
		AdventureResult [] ingredients = concoctions.get( itemID ).getIngredients();

		for ( int i = 0; i < ingredients.length; ++i )
		{
			hasOneIngredient |= KoLCharacter.getInventory().contains( ingredients[i] );
			hasOneIngredient |= KoLCharacter.getCloset().contains( ingredients[i] );
			hasOneIngredient |= ingredients.length > 1 && hasAnyIngredient( ingredients[i].getItemID() );
			hasOneIngredient |= NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemID ) );
		}

		return hasOneIngredient;
	}

	private static AdventureResult getBetterIngredient( AdventureResult ingredient1, AdventureResult ingredient2, List availableIngredients )
	{	return ingredient1.getCount( availableIngredients ) > ingredient2.getCount( availableIngredients ) ? ingredient1 : ingredient2;
	}

	/**
	 * Internal class used to represent a single concoction.  It
	 * contains all the information needed to actually make the item.
	 */

	private static class Concoction
	{
		private AdventureResult concoction;
		private int mixingMethod;
		private boolean wasPossible;

		private List ingredients;
		private AdventureResult [] ingredientArray;

		private int modifier, multiplier;
		private int initial, creatable, total;

		public Concoction( AdventureResult concoction, int mixingMethod )
		{
			this.concoction = concoction;
			this.mixingMethod = mixingMethod;
			this.wasPossible = false;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[0];
		}

		public void resetCalculations()
		{
			this.initial = -1;
			this.creatable = 0;
			this.total = 0;

			this.modifier = 0;
			this.multiplier = 0;
		}

		public void setPossible( boolean wasPossible )
		{	this.wasPossible = wasPossible;
		}

		public boolean wasPossible()
		{	return wasPossible;
		}

		public void addIngredient( AdventureResult ingredient )
		{
			knownUses.get( ingredient.getItemID() ).add( concoction );

			ingredients.add( ingredient );
			ingredientArray = new AdventureResult[ ingredients.size() ];
			ingredients.toArray( ingredientArray );
		}

		public int getMixingMethod()
		{	return mixingMethod;
		}

		public boolean isBadRecipe()
		{
			for ( int i = 0; i < ingredientArray.length; ++i )
			{
				AdventureResult ingredient = ingredientArray[i];
				if ( ingredient == null || ingredient.getItemID() == -1 || ingredient.getName() == null )
					return true;
			}

			return false;
		}

		public AdventureResult [] getIngredients()
		{	return ingredientArray;
		}

		public void calculate( List availableIngredients )
		{
			// If a calculation has already been done for this
			// concoction, no need to calculate again.

			if ( this.initial != -1 )
				return;

			// Initialize creatable item count to 0.  This way,
			// you ensure that you're not always off by one.

			this.creatable = 0;

			// If the item doesn't exist in the item table,
			// then assume it can't be created.

			if ( concoction.getName() == null )
			{
				this.initial = 0;
				return;
			}

			// Determine how many were available initially in the
			// available ingredient list.

			this.initial = concoction.getCount( availableIngredients );
			this.total = initial;

			if ( !isPermittedMethod( mixingMethod ) )
				return;

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions.get( ingredientArray[i].getItemID() ).calculate( availableIngredients );

			this.mark( 0, 1 );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER )
			{
				// If there's only one ingredient, then the
				// quantity depends entirely on it.

				this.creatable = concoctions.get( ingredientArray[0].getItemID() ).initial;
				this.total = this.initial + this.creatable;
			}
			else if ( mixingMethod == ItemCreationRequest.STILL_MIXER || mixingMethod == ItemCreationRequest.STILL_BOOZE )
			{
				// Improving mixers or booze depends on the
				// quantity of the ingredient as well as the
				// number of Still usages remaining
				this.creatable = Math.min( concoctions.get( ingredientArray[0].getItemID() ).initial, KoLCharacter.getStillsAvailable() );
				this.total = this.initial + this.creatable;
			}
			else
			{
				this.total = Integer.MAX_VALUE;
				for ( int i = 0; i < ingredientArray.length; ++i )
					this.total = Math.min( this.total, concoctions.get( ingredientArray[i].getItemID() ).quantity() );

				// The total available for other creations is equal
				// to the total, less the initial.

				this.creatable = this.total - this.initial;
			}

			// Now that all the calculations are complete, unmark
			// the ingredients so that later calculations can make
			// the correct calculations.

			this.unmark();
		}

		/**
		 * Utility method which calculates the quantity available for
		 * a recipe based on the modifier/multiplier of its ingredients
		 */

		private int quantity()
		{
			// If there is no multiplier, assume that an infinite
			// number is available.

			if ( this.multiplier == 0 )
				return Integer.MAX_VALUE;

			// The maximum value is equivalent to the total, plus
			// the modifier, divided by the multiplier, if the
			// multiplier exists.

			int quantity = (this.total + this.modifier) / this.multiplier;

			// Avoid mutual recursion.

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER || !isPermittedMethod( mixingMethod ) )
				return quantity;

			// The true value is affected by the maximum value for
			// the ingredients.  Therefore, calculate the quantity
			// for all other ingredients to complete the solution
			// of the linear inequality.

			for ( int i = 0; quantity > 0 && i < ingredientArray.length; ++i )
				quantity = Math.min( quantity, concoctions.get( ingredientArray[i].getItemID() ).quantity() );

			// Adventures are also considered an ingredient; if
			// no adventures are necessary, the multiplier should
			// be zero and the infinite number available will have
			// no effect on the calculation.

			if ( quantity > 0 && this != concoctions.get(0) )
				quantity = Math.min( quantity, concoctions.get(0).quantity() );

			// The true value is now calculated.  Return this
			// value to the requesting method.

			return quantity;
		}

		/**
		 * Utility method which marks the ingredient for usage with
		 * the given added modifier and the given additional multiplier.
		 */

		private void mark( int modifier, int multiplier )
		{
			this.modifier += modifier;
			this.multiplier += multiplier;

			// Avoid mutual recursion

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER || !isPermittedMethod( mixingMethod ) )
				return;

			// Mark all the ingredients, being sure to multiply
			// by the number of that ingredient needed in this
			// concoction.

			int instanceCount;

			for ( int i = 0; i < ingredientArray.length; ++i )
			{
				boolean shouldMark = true;
				instanceCount = ingredientArray[i].getCount();

				for ( int j = 0; j < i; ++j )
					shouldMark &= ingredientArray[i].getItemID() != ingredientArray[j].getItemID();

				if ( shouldMark )
				{
					for ( int j = i + 1; j < ingredientArray.length; ++j )
						if ( ingredientArray[i].getItemID() == ingredientArray[j].getItemID() )
							instanceCount += ingredientArray[j].getCount();

					concoctions.get( ingredientArray[i].getItemID() ).mark(
							(this.modifier + this.initial) * instanceCount, this.multiplier * instanceCount );
				}
			}

			// Mark the implicit adventure ingredient, being
			// sure to multiply by the number of adventures
			// which are required for this mixture.

			if ( this != concoctions.get(0) && ADVENTURE_USAGE[ mixingMethod ] != 0 )
				concoctions.get(0).mark( (this.modifier + this.initial) * ADVENTURE_USAGE[ mixingMethod ],
					this.multiplier * ADVENTURE_USAGE[ mixingMethod ] );
		}

		/**
		 * Utility method which undoes the yielding process, resetting
		 * the ingredient and current total values to the given number.
		 */

		private void unmark()
		{
			if ( this.modifier == 0 && this.multiplier == 0 )
				return;

			this.modifier = 0;
			this.multiplier = 0;

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions.get( ingredientArray[i].getItemID() ).unmark();

			if ( this != concoctions.get(0) )
				concoctions.get(0).unmark();
		}

		private int getMeatPasteNeeded()
		{
			// Avoid mutual recursion.

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER || !isPermittedMethod( mixingMethod ) )
				return 0;

			// Count all the meat paste from the different
			// levels in the creation tree.

			int runningTotal = 0;
			for ( int i = 0; i < ingredientArray.length; ++i )
				runningTotal += concoctions.get( ingredientArray[i].getItemID() ).getMeatPasteNeeded();

			runningTotal += this.creatable;
			return runningTotal;
		}

		/**
		 * Returns the string form of this concoction.  This is
		 * basically the display name for the item created.
		 */

		public String toString()
		{	return concoction.getName();
		}
	}

	/**
	 * Internal class which functions exactly an array of sorted lists,
	 * except it uses "sets" and "gets" like a list.  This could be
	 * done with generics (Java 1.5) but is done like this so that
	 * we get backwards compatibility.
	 */

	private static class SortedListModelArray
	{
		private ArrayList internalList = new ArrayList();

		public SortedListModel get( int index )
		{
			if ( index < 0 )
				return null;

			while ( index >= internalList.size() )
				internalList.add( new SortedListModel() );

			return (SortedListModel) internalList.get( index );
		}

		public void set( int index, SortedListModel value )
		{
			while ( index >= internalList.size() )
				internalList.add( new SortedListModel() );

			internalList.set( index, value );
		}
	}


	/**
	 * Internal class which functions exactly an array of concoctions,
	 * except it uses "sets" and "gets" like a list.  This could be
	 * done with generics (Java 1.5) but is done like this so that
	 * we get backwards compatibility.
	 */

	private static class ConcoctionArray
	{
		private ArrayList internalList = new ArrayList();

		public Concoction get( int index )
		{
			if ( index < 0 )
				return null;

			for ( int i = internalList.size(); i <= index; ++i )
				internalList.add( new Concoction( new AdventureResult( i, 0 ), ItemCreationRequest.NOCREATE ) );

			return (Concoction) internalList.get( index );
		}

		public void set( int index, Concoction value )
		{
			for ( int i = internalList.size(); i <= index; ++i )
				internalList.add( new Concoction( new AdventureResult( i, 0 ), ItemCreationRequest.NOCREATE ) );

			internalList.set( index, value );
		}

		public int size()
		{	return internalList.size();
		}
	}
}
