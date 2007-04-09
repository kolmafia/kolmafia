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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import net.java.dev.spellcast.utilities.SortedListModel;

public class ConcoctionsDatabase extends KoLDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	public static final SortedListModel creatableList = new SortedListModel();
	public static final SortedListModel usableList = new SortedListModel();

	private static Concoction stillsLimit = new Concoction( (AdventureResult) null, NOCREATE );
	private static Concoction adventureLimit = new Concoction( (AdventureResult) null, NOCREATE );

	private static ConcoctionArray concoctions = new ConcoctionArray();
	private static SortedListModelArray knownUses = new SortedListModelArray();

	private static boolean [] PERMIT_METHOD = new boolean[ METHOD_COUNT ];
	private static int [] ADVENTURE_USAGE = new int[ METHOD_COUNT ];
	private static final AdventureResult [] NO_INGREDIENTS = new AdventureResult[0];

	private static final int CHEF = 438;
	private static final int CLOCKWORK_CHEF = 1112;
	private static final int BARTENDER = 440;
	private static final int CLOCKWORK_BARTENDER = 1111;

	public static final AdventureResult OVEN = new AdventureResult( 157, 1 );
	public static final AdventureResult KIT = new AdventureResult( 236, 1 );
	public static final AdventureResult HAMMER = new AdventureResult( 338, 1 );
	public static final AdventureResult PLIERS = new AdventureResult( 709, 1 );

	private static final AdventureResult PASTE = new AdventureResult( MEAT_PASTE, 1 );
	private static final AdventureResult STACK = new AdventureResult( MEAT_STACK, 1 );
	private static final AdventureResult DENSE = new AdventureResult( DENSE_STACK, 1 );

	private static final int TOMATO = 246;
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
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = getReader( "concoctions.txt" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			try
			{
				if ( data.length > 2 )
				{
					AdventureResult item = AdventureResult.parseResult( data[0] );
					int itemId = item.getItemId();

					if ( itemId > 0 )
					{
						int mixingMethod = parseInt( data[1] );
						Concoction concoction = new Concoction( item, mixingMethod );

						for ( int i = 2; i < data.length; ++i )
							concoction.addIngredient( parseIngredient( data[i] ) );

						if ( !concoction.isBadRecipe() )
						{
							concoctions.set( itemId, concoction );
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

				printStackTrace( e );
				System.out.println( data.length );
				for ( int i = 0; i < data.length; ++i )
					System.out.println( data[i] );
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

			printStackTrace( e );
		}

		for ( int i = 0; i < concoctions.size(); ++i )
			if ( concoctions.get(i) != null )
				usableList.add( concoctions.get(i) );
	}

	public static final boolean isKnownCombination( AdventureResult [] ingredients )
	{
		// Known combinations which could not be added because
		// there are limitations in the item manager.

		if ( ingredients.length == 2 )
		{
			// Handle meat stacks, which are created from fairy
			// gravy and meat from yesterday.

			if ( ingredients[0].getItemId() == 80 && ingredients[1].getItemId() == 87 )
				return true;
			if ( ingredients[1].getItemId() == 80 && ingredients[0].getItemId() == 87 )
				return true;

			// Handle plain pizza, which also allows flat dough
			// to be used instead of wads of dough.

			if ( ingredients[0].getItemId() == TOMATO && ingredients[1].getItemId() == FLAT_DOUGH )
				return true;
			if ( ingredients[1].getItemId() == TOMATO && ingredients[0].getItemId() == FLAT_DOUGH )
				return true;

			// Handle catsup recipes, which only exist in the
			// item table as ketchup recipes.

			if ( ingredients[0].getItemId() == CATSUP.getItemId() )
			{
				ingredients[0] = KETCHUP;
				return isKnownCombination( ingredients );
			}
			if ( ingredients[1].getItemId() == CATSUP.getItemId() )
			{
				ingredients[1] = KETCHUP;
				return isKnownCombination( ingredients );
			}

			// Handle ice-cold beer recipes, which only uses the
			// recipe for item #41 at this time.

			if ( ingredients[0].getItemId() == WILLER.getItemId() )
			{
				ingredients[0] = SCHLITZ;
				return isKnownCombination( ingredients );
			}
			if ( ingredients[1].getItemId() == WILLER.getItemId() )
			{
				ingredients[1] = SCHLITZ;
				return isKnownCombination( ingredients );
			}

			// Handle cloaca recipes, which only exist in the
			// item table as dyspepsi cola.

			if ( ingredients[0].getItemId() == CLOACA.getItemId() )
			{
				ingredients[0] = DYSPEPSI;
				return isKnownCombination( ingredients );
			}
			if ( ingredients[1].getItemId() == CLOACA.getItemId() )
			{
				ingredients[1] = DYSPEPSI;
				return isKnownCombination( ingredients );
			}
		}

		int [] ingredientTestIds;
		AdventureResult [] ingredientTest;

		for ( int i = 0; i < concoctions.size(); ++i )
		{
			ingredientTest = concoctions.get(i).getIngredients();
			if ( ingredientTest.length != ingredients.length )
				continue;

			ingredientTestIds = new int[ ingredients.length ];
			for ( int j = 0; j < ingredientTestIds.length; ++j )
				ingredientTestIds[j] = ingredientTest[j].getItemId();

			boolean foundMatch = true;
			for ( int j = 0; j < ingredients.length && foundMatch; ++j )
			{
				foundMatch = false;
				for ( int k = 0; k < ingredientTestIds.length && !foundMatch; ++k )
				{
					foundMatch |= ingredients[j].getItemId() == ingredientTestIds[k];
					if ( foundMatch )  ingredientTestIds[k] = -1;
				}
			}

			if ( foundMatch )
				return true;
		}

		return false;
	}

	public static final SortedListModel getKnownUses( int itemId )
	{
		SortedListModel uses = knownUses.get( itemId );
		return uses == null ? EMPTY_LIST : uses;
	}

	public static final SortedListModel getKnownUses( AdventureResult item )
	{	return getKnownUses( item.getItemId() );
	}

	public static final boolean isPermittedMethod( int method )
	{	return PERMIT_METHOD[ method ];
	}

	private static AdventureResult parseIngredient( String data )
	{
		// If the ingredient is specified inside of brackets,
		// then a specific item Id is being designated.

		if ( data.startsWith( "[" ) )
		{
			int closeBracketIndex = data.indexOf( "]" );
			String itemIdString = data.substring( 0, closeBracketIndex ).replaceAll( "[\\[\\]]", "" ).trim();
			String quantityString = data.substring( closeBracketIndex + 1 ).trim();

			return new AdventureResult( parseInt( itemIdString ), quantityString.length() == 0 ? 1 :
				parseInt( quantityString.replaceAll( "[\\(\\)]", "" ) ) );
		}

		// Otherwise, it's a standard ingredient - use
		// the standard adventure result parsing routine.

		return AdventureResult.parseResult( data );
	}

	public static SortedListModel getUsables()
	{	return usableList;
	}

	public static SortedListModel getCreatables()
	{	return creatableList;
	}

	private static ArrayList getAvailableIngredients()
	{
		ArrayList availableIngredients = new ArrayList();
		availableIngredients.addAll( inventory );

		if ( getBooleanProperty( "autoSatisfyWithStash" ) && KoLCharacter.canInteract() )
		{
			AdventureResult [] items = new AdventureResult[ ClanManager.getStash().size() ];
			ClanManager.getStash().toArray( items );

			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( availableIngredients, items[i] );
		}

		return availableIngredients;
	}

	private static void setBetterIngredient( AdventureResult item1, AdventureResult item2, ArrayList availableIngredients )
	{
		Concoction item;
		int available = getBetterIngredient( item1, item2, availableIngredients ).getCount( availableIngredients );

		item = concoctions.get( item1.getItemId() );
		item.initial = available;
		item.creatable = 0;
		item.total = available;

		item = concoctions.get( item2.getItemId() );
		item.initial = available;
		item.creatable = 0;
		item.total = available;
	}

	/**
	 * Returns the concoctions which are available given the list of
	 * ingredients.  The list returned contains formal requests for
	 * item creation.
	 */

	public static void refreshConcoctions()
	{
		ArrayList availableIngredients = getAvailableIngredients();

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		Concoction item;
		for ( int i = 1; i < concoctions.size(); ++i )
		{
			item = concoctions.get(i);
			if ( item != null )
				item.resetCalculations();
		}

		// Make assessment of availability of mixing methods.
		// This method will also calculate the availability of
		// chefs and bartenders automatically so a second call
		// is not needed.

		cachePermitted( availableIngredients );
		boolean infiniteNPCStoreItems = getBooleanProperty( "assumeInfiniteNPCItems" );

		// Next, do calculations on all mixing methods which cannot
		// be created at this time.

		for ( int i = 1; i < concoctions.size(); ++i )
		{
			item = concoctions.get(i);
			if ( item == null )
				continue;

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
				continue;

			if ( infiniteNPCStoreItems && NPCStoreDatabase.contains( concoction.getName() ) )
			{
				item.initial = concoction.getCount( availableIngredients ) + KoLCharacter.getAvailableMeat() / (TradeableItemDatabase.getPriceById( concoction.getItemId() ) * 2);
				item.creatable = 0;
				item.total = item.initial;
			}
			else if ( !isPermittedMethod( item.getMixingMethod() ) )
			{
				item.initial = concoction.getCount( availableIngredients );
				item.creatable = 0;
				item.total = item.initial;
			}
		}

		calculateBasicItems( availableIngredients );

		// Ice-cold beer and ketchup are special instances -- for the
		// purposes of calculation, we assume that they will use the
		// ingredient which is present in the greatest quantity.


		setBetterIngredient( DYSPEPSI, CLOACA, availableIngredients );
		setBetterIngredient( SCHLITZ, WILLER, availableIngredients );
		setBetterIngredient( KETCHUP, CATSUP, availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < concoctions.size(); ++i )
		{
			item = concoctions.get(i);
			if ( item != null && item.toString() != null && item.creatable > -1 )
				item.calculate( availableIngredients );
		}

		// Now, to update the list of creatables without removing
		// all creatable items.  We do this by determining the
		// number of items inside of the old list.

		ItemCreationRequest instance;

		for ( int i = 1; i < concoctions.size(); ++i )
		{
			item = concoctions.get(i);
			if ( item == null )
				continue;

			instance = ItemCreationRequest.getInstance( i, false );
			if ( instance == null || item.creatable == instance.getQuantityPossible() )
				continue;

			instance.setQuantityPossible( item.creatable );

			if ( instance.getQuantityPossible() == 0 )
			{
				// We can't make this concoction now

				if ( item.wasPossible() )
				{
					creatableList.remove( instance );
					item.setPossible( false );
				}
			}
			else
			{
				// We can make the concoction now

				if ( !item.wasPossible() )
				{
					creatableList.add( instance );
					item.setPossible( true );
				}
			}
		}
	}

	public static int getMeatPasteRequired( int itemId, int creationCount )
	{
		Concoction item = concoctions.get( itemId );
		if ( item == null )
			return 0;

		ArrayList availableIngredients = getAvailableIngredients();
		item.calculate( availableIngredients );
		return item.getMeatPasteNeeded( creationCount + item.initial ) - item.initial + 1;
	}

	private static void calculateBasicItems( List availableIngredients )
	{
		// Meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.

		setBasicItem( availableIngredients, PASTE, KoLCharacter.getAvailableMeat() / 10 );
		setBasicItem( availableIngredients, STACK, KoLCharacter.getAvailableMeat() / 100 );
		setBasicItem( availableIngredients, DENSE, KoLCharacter.getAvailableMeat() / 1000 );

		AdventureResult item;
		int worthlessItems = Math.min( HermitRequest.getWorthlessItemCount(), KoLCharacter.getAvailableMeat() / 100 );

		for ( int i = 0; i < hermitItems.size(); ++i )
		{
			item = (AdventureResult) hermitItems.get(i);
			if ( !item.equals( SewerRequest.POSITIVE_CLOVER ) )
				setBasicItem( availableIngredients, item, worthlessItems );
		}
	}

	private static void setBasicItem( List availableIngredients, AdventureResult item, int creatable )
	{
		Concoction creation = concoctions.get( item.getItemId() );
		if ( creation == null )
			return;

		creation.initial = item.getCount( availableIngredients );
		creation.creatable = creatable;
		creation.total = creation.initial + creatable;
	}

	/**
	 * Utility method used to cache the current permissions on
	 * item creation.
	 */

	private static void cachePermitted( List availableIngredients )
	{
		boolean willBuyServant = KoLCharacter.canInteract() && getBooleanProperty( "autoSatisfyWithMall" );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		adventureLimit.initial = KoLCharacter.getAdventuresLeft();
		adventureLimit.creatable = 0;
		adventureLimit.total = KoLCharacter.getAdventuresLeft();

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		stillsLimit.initial = KoLCharacter.getStillsAvailable();
		stillsLimit.creatable = 0;
		stillsLimit.total = stillsLimit.initial;

		calculateBasicItems( availableIngredients );

		// It is never possible to create items which are flagged
		// NOCREATE, and it is always possible to create items
		// through meat paste combination.

		PERMIT_METHOD[ NOCREATE ] = false;
		ADVENTURE_USAGE[ NOCREATE ] = 0;

		PERMIT_METHOD[ COMBINE ] = true;
		ADVENTURE_USAGE[ COMBINE ] = 0;

		PERMIT_METHOD[ CLOVER ] = true;
		ADVENTURE_USAGE[ CLOVER ] = 0;

		PERMIT_METHOD[ CATALYST ] = true;
		ADVENTURE_USAGE[ CATALYST ] = 0;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		PERMIT_METHOD[ TINKER ] = KoLCharacter.inMoxieSign();
		ADVENTURE_USAGE[ TINKER ] = 0;

		// Smithing of items is possible whenever the person
		// has a hammer.

		PERMIT_METHOD[ SMITH ] = inventory.contains( HAMMER ) || KoLCharacter.getAvailableMeat() >= 1000;
		ADVENTURE_USAGE[ SMITH ] = 1;

		// Advanced smithing is available whenever the person can
		// smith and has access to the appropriate skill.

		PERMIT_METHOD[ SMITH_WEAPON ] = PERMIT_METHOD[ SMITH ] &&
			KoLCharacter.canSmithWeapons() && KoLCharacter.getAdventuresLeft() > 0;
		ADVENTURE_USAGE[ SMITH_WEAPON ] = 1;

		PERMIT_METHOD[ SMITH_ARMOR ] = PERMIT_METHOD[ SMITH ] &&
			KoLCharacter.canSmithArmor() && KoLCharacter.getAdventuresLeft() > 0;
		ADVENTURE_USAGE[ SMITH_ARMOR ] = 1;

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.inMuscleSign() )
		{
			PERMIT_METHOD[ SMITH ] = true;
			ADVENTURE_USAGE[ SMITH ] = 0;
		}

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		PERMIT_METHOD[ JEWELRY ] = inventory.contains( PLIERS );
		ADVENTURE_USAGE[ JEWELRY ] = 3;

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		PERMIT_METHOD[ STARCHART ] = true;
		ADVENTURE_USAGE[ STARCHART ] = 0;

		PERMIT_METHOD[ PIXEL ] = true;
		ADVENTURE_USAGE[ PIXEL ] = 0;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		PERMIT_METHOD[ ROLLING_PIN ] = true;
		ADVENTURE_USAGE[ ROLLING_PIN ] = 0;

		// It's not possible to ask Uncle Crimbo to make toys

		PERMIT_METHOD[ TOY ] = false;
		ADVENTURE_USAGE[ TOY ] = 0;

		// It's possible to ask Ugh Crimbo to make toys

		PERMIT_METHOD[ UGH ] = false;
		ADVENTURE_USAGE[ UGH ] = 0;

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		concoctions.get( CHEF ).calculate( availableIngredients );
		concoctions.get( CLOCKWORK_CHEF ).calculate( availableIngredients );
		concoctions.get( BARTENDER ).calculate( availableIngredients );
		concoctions.get( CLOCKWORK_BARTENDER ).calculate( availableIngredients );

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		PERMIT_METHOD[ COOK ] = willBuyServant || KoLCharacter.hasChef() || isAvailable( CHEF, CLOCKWORK_CHEF );
		ADVENTURE_USAGE[ COOK ] = 0;

		if ( !PERMIT_METHOD[ COOK ] )
		{
			PERMIT_METHOD[ COOK ] = KoLCharacter.canInteract() && getBooleanProperty( "autoSatisfyWithMall" );

			if ( !PERMIT_METHOD[ COOK ] )
			{
				PERMIT_METHOD[ COOK ] = !KoLCharacter.canInteract() && (inventory.contains( OVEN ) || KoLCharacter.getAvailableMeat() >= 1000);
				ADVENTURE_USAGE[ COOK ] = 1;
			}
		}

		// Cooking of reagents and noodles is possible whenever
		// the person can cook and has the appropriate skill.

		PERMIT_METHOD[ COOK_REAGENT ] = PERMIT_METHOD[ COOK ] && KoLCharacter.canSummonReagent();
		ADVENTURE_USAGE[ COOK_REAGENT ] = ADVENTURE_USAGE[ COOK ];

		PERMIT_METHOD[ SUPER_REAGENT ] = PERMIT_METHOD[ COOK ] && KoLCharacter.hasSkill( "The Way of Sauce" );
		ADVENTURE_USAGE[ SUPER_REAGENT ] = ADVENTURE_USAGE[ COOK ];

		PERMIT_METHOD[ COOK_PASTA ] = PERMIT_METHOD[ COOK ] && KoLCharacter.canSummonNoodles();
		ADVENTURE_USAGE[ COOK_PASTA ] = ADVENTURE_USAGE[ COOK ];

		// Mixing is possible whenever the person has a bartender
		// or they don't need a box servant and have a kit.

		PERMIT_METHOD[ MIX ] = KoLCharacter.hasBartender() || isAvailable( BARTENDER, CLOCKWORK_BARTENDER );
		ADVENTURE_USAGE[ MIX ] = 0;

		if ( !PERMIT_METHOD[ MIX ] )
		{
			PERMIT_METHOD[ MIX ] = KoLCharacter.canInteract() && getBooleanProperty( "autoSatisfyWithMall" );

			if ( !PERMIT_METHOD[ MIX ] )
			{
				PERMIT_METHOD[ MIX ] = !KoLCharacter.canInteract() && (inventory.contains( KIT ) || KoLCharacter.getAvailableMeat() >= 1000);
				ADVENTURE_USAGE[ MIX ] = 1;
			}
		}

		// Mixing of advanced drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		PERMIT_METHOD[ MIX_SPECIAL ] = PERMIT_METHOD[ MIX ] && KoLCharacter.canSummonShore();
		ADVENTURE_USAGE[ MIX_SPECIAL ] = ADVENTURE_USAGE[ MIX ];

		// Mixing of super-special drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		PERMIT_METHOD[ MIX_SUPER ] = PERMIT_METHOD[ MIX ] && KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" );
		ADVENTURE_USAGE[ MIX_SUPER ] = ADVENTURE_USAGE[ MIX ];

		// Using Crosby Nash's Still is possible if the person has
		// Superhuman Cocktailcrafting and is a Moxie class character.

		boolean hasStillsAvailable = stillsLimit.total > 0;
		PERMIT_METHOD[ STILL_MIXER ] = hasStillsAvailable;
		ADVENTURE_USAGE[ STILL_MIXER ] = 0;

		PERMIT_METHOD[ STILL_BOOZE ] = hasStillsAvailable;
		ADVENTURE_USAGE[ STILL_BOOZE ] = 0;

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		PERMIT_METHOD[ WOK ] = KoLCharacter.canUseWok() &&
			KoLCharacter.getAdventuresLeft() > 0;
		ADVENTURE_USAGE[ WOK ] = 1;

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		PERMIT_METHOD[ MALUS ] = KoLCharacter.canUseMalus();
		ADVENTURE_USAGE[ MALUS ] = 0;

		// Now, go through all the cached adventure usage values and if
		// the number of adventures left is zero and the request requires
		// adventures, it is not permitted.

		for ( int i = 0; i < METHOD_COUNT; ++i )
			if ( PERMIT_METHOD[i] && ADVENTURE_USAGE[i] > 0 )
				PERMIT_METHOD[i] = ADVENTURE_USAGE[i] <= KoLCharacter.getAdventuresLeft();
	}

	private static boolean isAvailable( int servantId, int clockworkId )
	{
		// If the user did not wish to repair their boxes
		// on explosion, then the box servant is not available

		if ( !getBooleanProperty( "autoRepairBoxes" ) )
			return false;

		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		return concoctions.get( servantId ).total > 0 || concoctions.get( clockworkId ).total > 0;
	}

	/**
	 * Returns the mixing method for the item with the given Id.
	 */

	public static int getMixingMethod( int itemId )
	{
		Concoction item = concoctions.get( itemId );
		return item == null ? NOCREATE : item.getMixingMethod();
	}

	/**
	 * Returns the item Ids of the ingredients for the given item.
	 * Note that if there are no ingredients, then <code>null</code>
	 * will be returned instead.
	 */

	public static AdventureResult [] getIngredients( int itemId )
	{
		List availableIngredients = getAvailableIngredients();

		// Ensure that you're retrieving the same ingredients that
		// were used in the calculations.  Usually this is the case,
		// but ice-cold beer and ketchup are tricky cases.

		AdventureResult [] ingredients = getStandardIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( ingredients[i].getItemId() == SCHLITZ.getItemId() || ingredients[i].getItemId() == WILLER.getItemId() )
				ingredients[i] = getBetterIngredient( SCHLITZ, WILLER, availableIngredients );
			else if ( ingredients[i].getItemId() == KETCHUP.getItemId() || ingredients[i].getItemId() == CATSUP.getItemId() )
				ingredients[i] = getBetterIngredient( KETCHUP, CATSUP, availableIngredients );
		}

		return ingredients;
	}

	public static AdventureResult [] getStandardIngredients( int itemId )
	{
		Concoction item = concoctions.get( itemId );
		return item == null ? NO_INGREDIENTS : item.getIngredients();
	}

	public static boolean hasAnyIngredient( int itemId )
	{
		if ( itemId < 0 )
			return false;

		switch ( itemId )
		{
		case MEAT_PASTE:
			return KoLCharacter.getAvailableMeat() >= 10;
		case MEAT_STACK:
			return KoLCharacter.getAvailableMeat() >= 100;
		case DENSE_STACK:
			return KoLCharacter.getAvailableMeat() >= 1000;
		}

		if ( !isPermittedMethod( getMixingMethod( itemId ) ) )
			return false;

		AdventureResult [] ingredients = getStandardIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( inventory.contains( ingredients[i] ) || closet.contains( ingredients[i] ) )
				return true;
			if ( NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemId ) ) )
				return true;
			if ( i > 0 && hasAnyIngredient( ingredients[i].getItemId() ) )
				return true;
		}

		return false;
	}

	private static AdventureResult getBetterIngredient( AdventureResult ingredient1, AdventureResult ingredient2, List availableIngredients )
	{	return ingredient1.getCount( availableIngredients ) > ingredient2.getCount( availableIngredients ) ? ingredient1 : ingredient2;
	}

	/**
	 * Internal class used to represent a single concoction.  It
	 * contains all the information needed to actually make the item.
	 */

	public static class Concoction implements Comparable
	{
		private AdventureResult concoction;
		private int mixingMethod, sortOrder;
		private boolean wasPossible;

		private String name;
		private int price;

		private List ingredients;
		private AdventureResult [] ingredientArray;

		private int modifier, multiplier;
		private int initial, creatable, total;

		public Concoction( String name, int price )
		{
			this.name = name;
			this.concoction = null;

			if ( TradeableItemDatabase.contains( name ) )
				this.concoction = new AdventureResult( name, 1, false );

			this.mixingMethod = NOCREATE;
			this.wasPossible = true;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[0];

			int consumeType = TradeableItemDatabase.getFullness( name ) == 0 ? CONSUME_DRINK : CONSUME_EAT;

			switch ( consumeType )
			{
			case CONSUME_EAT:
				this.sortOrder = 1;
				break;
			case CONSUME_DRINK:
				this.sortOrder = 2;
				break;
			default:
				this.sortOrder = 3;
				break;
			}

			this.price = price;
			resetCalculations();
		}

		public Concoction( AdventureResult concoction, int mixingMethod )
		{
			this.concoction = concoction;

			if ( concoction != null )
				this.name = concoction.getName();

			this.mixingMethod = mixingMethod;
			this.wasPossible = false;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[0];

			int consumeType = concoction == null ? 0 : TradeableItemDatabase.getConsumptionType( concoction.getItemId() );

			switch ( consumeType )
			{
			case CONSUME_EAT:
				this.sortOrder = 1;
				break;
			case CONSUME_DRINK:
				this.sortOrder = 2;
				break;
			default:
				this.sortOrder = 3;
				break;
			}

			this.price = price;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof Concoction) )
				return -1;

			if ( name == null )
				return 1;

			if ( ((Concoction)o).name == null )
				return -1;

			if ( sortOrder != ((Concoction)o).sortOrder )
				return sortOrder - ((Concoction)o).sortOrder;

			int fullness1 = TradeableItemDatabase.getFullness( name );
			int fullness2 = TradeableItemDatabase.getFullness( ((Concoction)o).name );

			if ( fullness1 != fullness2 )
				return fullness1 > fullness2 ? -1 : 1;

			int inebriety1 = TradeableItemDatabase.getInebriety( name );
			int inebriety2 = TradeableItemDatabase.getInebriety( ((Concoction)o).name );

			if ( inebriety1 != inebriety2 )
				return inebriety1 > inebriety2 ? -1 : 1;

			float adventures1 = parseFloat( TradeableItemDatabase.getAdventureRange( name ) );
			float adventures2 = parseFloat( TradeableItemDatabase.getAdventureRange( ((Concoction)o).name ) );

			if ( adventures1 != adventures2 )
				return adventures1 > adventures2 ? -1 : 1;

			return name.compareToIgnoreCase( name );
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof Concoction) )
				return false;

			if ( name == null )
				return false;

			if ( ((Concoction)o).name == null )
				return false;

			return name.equals( ((Concoction)o).name );
		}

		public AdventureResult getItem()
		{	return concoction;
		}

		public int getItemId()
		{	return concoction == null ? -1 : concoction.getItemId();
		}

		public String getName()
		{	return name;
		}

		public int getInitial()
		{	return initial;
		}

		public int getTotal()
		{	return total;
		}

		public int getPrice()
		{	return price;
		}

		public void resetCalculations()
		{
			this.initial = -1;
			this.creatable = 0;
			this.total = 0;

			this.modifier = 0;
			this.multiplier = 0;

			if ( concoction == null && name != null )
			{
				int fullness = TradeableItemDatabase.getFullness( name );
				int inebriety = TradeableItemDatabase.getInebriety( name );

				if ( fullness > 0 )
					this.initial = 40 / fullness;
				else
					this.initial = 40 / inebriety;

				this.creatable = 0;
				this.total = this.initial;
			}
		}

		public void setPossible( boolean wasPossible )
		{	this.wasPossible = wasPossible;
		}

		public boolean wasPossible()
		{	return wasPossible;
		}

		public void addIngredient( AdventureResult ingredient )
		{
			SortedListModel uses = knownUses.get( ingredient.getItemId() );
			if ( uses == null )
			{
				uses = new SortedListModel();
				knownUses.set( ingredient.getItemId(), uses );
			}

			uses.add( concoction );
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
				if ( ingredient == null || ingredient.getItemId() == -1 || ingredient.getName() == null )
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

			if ( concoction == null || name == null )
				return;

			// Determine how many were available initially in the
			// available ingredient list.

			this.initial = concoction.getCount( availableIngredients );
			this.total = initial;

			if ( !isPermittedMethod( mixingMethod ) )
				return;

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions.get( ingredientArray[i].getItemId() ).calculate( availableIngredients );

			this.mark( 0, 1 );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			if ( mixingMethod == ROLLING_PIN || mixingMethod == CLOVER )
			{
				// If there's only one ingredient, then the
				// quantity depends entirely on it.

				this.creatable = concoctions.get( ingredientArray[0].getItemId() ).initial;
				this.total = this.initial + this.creatable;
			}
			else
			{
				this.total = MallPurchaseRequest.MAX_QUANTITY;
				for ( int i = 0; i < ingredientArray.length; ++i )
					this.total = Math.min( this.total, concoctions.get( ingredientArray[i].getItemId() ).quantity() );

				if ( ADVENTURE_USAGE[ mixingMethod ] != 0 )
					this.total = Math.min( this.total, adventureLimit.quantity() );

				if ( mixingMethod == STILL_MIXER || mixingMethod == STILL_BOOZE )
					this.total = Math.min( this.total, stillsLimit.quantity() );

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
				return MallPurchaseRequest.MAX_QUANTITY;

			// The maximum value is equivalent to the total, plus
			// the modifier, divided by the multiplier, if the
			// multiplier exists.

			int quantity = (this.total + this.modifier) / this.multiplier;

			// Avoid mutual recursion.

			if ( mixingMethod == ROLLING_PIN || mixingMethod == CLOVER || !isPermittedMethod( mixingMethod ) )
				return quantity;

			// The true value is affected by the maximum value for
			// the ingredients.  Therefore, calculate the quantity
			// for all other ingredients to complete the solution
			// of the linear inequality.

			for ( int i = 0; quantity > 0 && i < ingredientArray.length; ++i )
				quantity = Math.min( quantity, concoctions.get( ingredientArray[i].getItemId() ).quantity() );

			// Adventures are also considered an ingredient; if
			// no adventures are necessary, the multiplier should
			// be zero and the infinite number available will have
			// no effect on the calculation.

			if ( ADVENTURE_USAGE[ mixingMethod ] != 0 )
				quantity = Math.min( quantity, adventureLimit.quantity() );

			// Still uses are also considered an ingredient.

			if ( mixingMethod == STILL_MIXER || mixingMethod == STILL_BOOZE )
				quantity = Math.min( quantity, stillsLimit.quantity() );

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

			if ( mixingMethod == ROLLING_PIN || mixingMethod == CLOVER || !isPermittedMethod( mixingMethod ) )
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
					shouldMark &= ingredientArray[i].getItemId() != ingredientArray[j].getItemId();

				if ( shouldMark )
				{
					for ( int j = i + 1; j < ingredientArray.length; ++j )
						if ( ingredientArray[i].getItemId() == ingredientArray[j].getItemId() )
							instanceCount += ingredientArray[j].getCount();

					concoctions.get( ingredientArray[i].getItemId() ).mark(
							(this.modifier + this.initial) * instanceCount, this.multiplier * instanceCount );
				}
			}

			// Mark the implicit adventure ingredient, being
			// sure to multiply by the number of adventures
			// which are required for this mixture.

			if ( ADVENTURE_USAGE[ mixingMethod ] != 0 )
				adventureLimit.mark( (this.modifier + this.initial) * ADVENTURE_USAGE[ mixingMethod ],
					this.multiplier * ADVENTURE_USAGE[ mixingMethod ] );

			if ( mixingMethod == STILL_MIXER || mixingMethod == STILL_BOOZE )
				stillsLimit.mark( (this.modifier + this.initial), this.multiplier );
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
				concoctions.get( ingredientArray[i].getItemId() ).unmark();

			if ( ADVENTURE_USAGE[ mixingMethod ] != 0 )
				adventureLimit.unmark();

			if ( mixingMethod == STILL_MIXER || mixingMethod == STILL_BOOZE )
				stillsLimit.unmark();
		}

		private int getMeatPasteNeeded( int quantityNeeded )
		{
			// Avoid mutual recursion.

			if ( mixingMethod != COMBINE || KoLCharacter.inMuscleSign() )
				return 0;

			// Count all the meat paste from the different
			// levels in the creation tree.

			int runningTotal = 0;
			for ( int i = 0; i < ingredientArray.length; ++i )
				runningTotal += concoctions.get( ingredientArray[i].getItemId() ).getMeatPasteNeeded( quantityNeeded );

			runningTotal += Math.max( 0, quantityNeeded - this.initial );
			return runningTotal;
		}

		/**
		 * Returns the string form of this concoction.  This is
		 * basically the display name for the item created.
		 */

		public String toString()
		{	return name;
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
				internalList.add( null );

			return (SortedListModel) internalList.get( index );
		}

		public void set( int index, SortedListModel value )
		{
			while ( index >= internalList.size() )
				internalList.add( null );

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
			{
				internalList.add( new Concoction( TradeableItemDatabase.getItemName( i ) == null ? null : new AdventureResult( i, 0 ),
					NOCREATE ) );
			}

			return (Concoction) internalList.get( index );
		}

		public void set( int index, Concoction value )
		{
			for ( int i = internalList.size(); i <= index; ++i )
			{
				internalList.add( new Concoction( TradeableItemDatabase.getItemName( i ) == null ? null : new AdventureResult( i, 0 ),
					NOCREATE ) );
			}

			internalList.set( index, value );
		}

		public int size()
		{	return internalList.size();
		}
	}
}
