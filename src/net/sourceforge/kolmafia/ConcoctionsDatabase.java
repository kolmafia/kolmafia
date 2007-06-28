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
import java.util.Stack;
import net.java.dev.spellcast.utilities.SortedListModel;

public class ConcoctionsDatabase extends KoLDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	public static final SortedListModel creatableList = new SortedListModel();
	public static final SortedListModel usableList = new SortedListModel();

	private static boolean ignoreRefresh = false;

	private static int queuedAdventuresUsed = 0;
	private static int queuedStillsUsed = 0;
	private static Stack queuedChanges = new Stack();
	private static SortedListModel queuedIngredients = new SortedListModel();

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
                                addConcoction( data );
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

	private static final void addConcoction( String [] data )
	{
		// Need at least concoction name and mixing method
		if ( data.length <= 2 )
			return;

		boolean bogus = false;

		String name = data[0];
		AdventureResult item = AdventureResult.parseResult( name );
		int itemId = item.getItemId();

		if ( itemId <= 0 )
		{
			System.out.println( "Unknown concoction: " + name );
			bogus = true;
		}

		int mixingMethod = parseInt( data[1] );
		if ( mixingMethod <= 0 || mixingMethod >= METHOD_COUNT )
		{
			System.out.println( "Unknown mixing method (" + mixingMethod + ") for concoction: " + name );
			bogus = true;
		}

		AdventureResult [] ingredients = new AdventureResult[ data.length - 2 ];
		for ( int i = 2; i < data.length; ++i )
		{
			AdventureResult ingredient = parseIngredient( data[i] );
			if ( ingredient == null || ingredient.getItemId() == -1 || ingredient.getName() == null )
			{
				System.out.println( "Unknown ingredient (" + data[i] + ") for concoction: " + name );
				bogus = true;
				continue;
			}
				
			ingredients[ i - 2 ] = ingredient;
		}

		if ( !bogus )
		{
			Concoction concoction = new Concoction( item, mixingMethod );

			for ( int i = 0; i < ingredients.length; ++i )
				concoction.addIngredient( ingredients[i] );
			concoctions.set( itemId, concoction );
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

	public static SortedListModel getQueue()
	{	return queuedIngredients;
	}

	public static void push( Concoction c, int quantity )
	{
		int adventureChange = queuedAdventuresUsed;
		int stillChange = queuedStillsUsed;

		ArrayList ingredientChange = new ArrayList();
		c.queue( ingredientChange, quantity );

		adventureChange -= queuedAdventuresUsed;
		stillChange -= queuedStillsUsed;

		queuedChanges.push( new Integer( stillChange ) );
		queuedChanges.push( new Integer( adventureChange ) );

		queuedChanges.push( ingredientChange );
		queuedChanges.push( new Integer( quantity ) );
		queuedChanges.push( c );
	}

	public static void pop()
	{
		if ( queuedChanges.isEmpty() )
			return;

		Concoction c = (Concoction) queuedChanges.pop();
		Integer quantity = (Integer) queuedChanges.pop();
		ArrayList ingredientChange = (ArrayList) queuedChanges.pop();

		Integer adventureChange = (Integer) queuedChanges.pop();
		Integer stillChange = (Integer) queuedChanges.pop();

		c.queued -= quantity.intValue();
		for ( int i = 0; i < ingredientChange.size(); ++i )
		{
			AdventureResult.addResultToList( queuedIngredients,
				((AdventureResult)ingredientChange.get(i)).getNegation() );
		}

		queuedAdventuresUsed += adventureChange.intValue();
		queuedStillsUsed += stillChange.intValue();
	}

	public static SortedListModel getUsables()
	{	return usableList;
	}

	public static SortedListModel getCreatables()
	{	return creatableList;
	}

	public static void handleQueue( boolean consume )
	{
		queuedChanges.clear();
		queuedIngredients.clear();

		queuedStillsUsed = 0;
		queuedAdventuresUsed = 0;

		ignoreRefresh = true;

		RequestThread.openRequestSequence();
		SpecialOutfit.createImplicitCheckpoint();

		Concoction c;

		for ( int i = 0; i < usableList.size(); ++i )
		{
			c = (Concoction) usableList.get(i);

			if ( c.getQueued() == 0 )
				continue;

			KoLRequest request = null;

			if ( !consume && c.getItem() != null )
				AdventureDatabase.retrieveItem( c.getItem().getInstance( c.getQueued() ) );
			else if ( c.getItem() == null && c.getFullness() > 0 )
				request = new RestaurantRequest( c.getName() );
			else if ( c.getItem() == null && c.getInebriety() > 0 )
				request = new MicrobreweryRequest( c.getName() );
			else
				request = new ConsumeItemRequest( c.getItem().getInstance( c.getQueued() ) );

			if ( request != null )
			{
				if ( request instanceof ConsumeItemRequest )
					RequestThread.postRequest( request );
				else
				{
					for ( int j = 0; j < c.getQueued(); ++j )
						RequestThread.postRequest( request );
				}
			}

			c.queued = 0;
			usableList.applyListFilters();
		}

		SpecialOutfit.restoreImplicitCheckpoint();
		RequestThread.closeRequestSequence();

		ignoreRefresh = false;
		refreshConcoctions();
	}

	private static List getAvailableIngredients()
	{
		boolean includeCloset = !closet.isEmpty();
		boolean includeStash = getBooleanProperty( "autoSatisfyWithStash" ) && KoLCharacter.canInteract() && !ClanManager.getStash().isEmpty();
		boolean includeQueue = !queuedIngredients.isEmpty();

		if ( !includeCloset && !includeStash && !includeQueue )
			return inventory;

		ArrayList availableIngredients = new ArrayList();
		availableIngredients.addAll( inventory );

		if ( includeCloset )
		{
			AdventureResult [] items = new AdventureResult[ closet.size() ];
			closet.toArray( items );

			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( availableIngredients, items[i] );
		}

		if ( includeStash )
		{
			AdventureResult [] items = new AdventureResult[ ClanManager.getStash().size() ];
			ClanManager.getStash().toArray( items );

			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( availableIngredients, items[i] );
		}

		if ( includeQueue )
		{
			AdventureResult [] items = new AdventureResult[ queuedIngredients.size() ];
			queuedIngredients.toArray( items );

			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( availableIngredients, items[i].getNegation() );
		}

		return availableIngredients;
	}

	private static void setBetterIngredient( AdventureResult item1, AdventureResult item2, List availableIngredients )
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
		List availableIngredients = getAvailableIngredients();

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
				int price = NPCStoreDatabase.price( concoction.getName() );
				item.initial = concoction.getCount( availableIngredients ) + KoLCharacter.getAvailableMeat() / price;
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

		if ( !ignoreRefresh )
		{
			creatableList.applyListFilters();
			usableList.applyListFilters();
		}
	}

	public static int getMeatPasteRequired( int itemId, int creationCount )
	{
		Concoction item = concoctions.get( itemId );
		if ( item == null )
			return 0;

		List availableIngredients = getAvailableIngredients();
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

		adventureLimit.initial = KoLCharacter.getAdventuresLeft() - queuedAdventuresUsed;
		adventureLimit.creatable = 0;
		adventureLimit.total = KoLCharacter.getAdventuresLeft();

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		stillsLimit.initial = KoLCharacter.getStillsAvailable() - queuedStillsUsed;
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

		PERMIT_METHOD[ EXPENSIVE_JEWELRY ] = PERMIT_METHOD[ JEWELRY ] && KoLCharacter.canCraftExpensiveJewelry();
		ADVENTURE_USAGE[ EXPENSIVE_JEWELRY ] = 3;

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		PERMIT_METHOD[ STARCHART ] = true;
		ADVENTURE_USAGE[ STARCHART ] = 0;

		PERMIT_METHOD[ PIXEL ] = true;
		ADVENTURE_USAGE[ PIXEL ] = 0;

		// You don't currently need a Weaving Manual to weave.

		// PERMIT_METHOD[ WEAVE ] = inventory.contains( PalmFrondRequest.MANUAL );
		PERMIT_METHOD[ WEAVE ] = true;
		ADVENTURE_USAGE[ WEAVE ] = 0;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		PERMIT_METHOD[ ROLLING_PIN ] = true;
		ADVENTURE_USAGE[ ROLLING_PIN ] = 0;

		// Rodoric will make chefstaves for mysticality class
		// characters who can get to the guild.

		PERMIT_METHOD[ STAFF ] = KoLCharacter.isMysticalityClass();
		ADVENTURE_USAGE[ STAFF ] = 0;

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

			if ( !PERMIT_METHOD[ COOK ] && !getBooleanProperty( "requireBoxServants" ) )
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

			if ( !PERMIT_METHOD[ MIX ] && !getBooleanProperty( "requireBoxServants" ) )
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

		int mixingMethod = getMixingMethod( itemId );

		switch ( mixingMethod )
		{
		case ROLLING_PIN:
		case CLOVER:
			AdventureResult [] ingredients = getStandardIngredients( itemId );
			return inventory.contains( ingredients[0] ) || closet.contains( ingredients[0] );

		default:
			if ( !isPermittedMethod( mixingMethod ) )
				return false;
		}

		AdventureResult [] ingredients = getStandardIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			// An item is immediately available if it is in your inventory,
			// in your closet, in an NPC store, or you have the ingredients
			// for a substep.  But, be careful of infinite recursion.

			if ( inventory.contains( ingredients[i] ) || closet.contains( ingredients[i] ) )
				return true;

			if ( NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemId ) ) )
				return true;

			if ( hasAnyIngredient( ingredients[i].getItemId() ) )
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
		private int initial, creatable, total, queued;
		private int fullness, inebriety;
		private int visibleTotal;

		public Concoction( String name, int price )
		{
			this.name = name;
			this.concoction = null;

			this.mixingMethod = NOCREATE;
			this.wasPossible = true;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[0];

			this.fullness = TradeableItemDatabase.getFullness( name );
			this.inebriety = TradeableItemDatabase.getInebriety( name );

			int consumeType = fullness == 0 ? CONSUME_DRINK : CONSUME_EAT;

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
			this.resetCalculations();
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

			this.price = -1;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof Concoction) )
				return -1;

			if ( this.name == null )
				return 1;

			if ( ((Concoction)o).name == null )
				return -1;

			if ( this.sortOrder != ((Concoction)o).sortOrder )
				return this.sortOrder - ((Concoction)o).sortOrder;

			int fullness1 = this.fullness;
			int fullness2 = ((Concoction)o).fullness;

			if ( !StaticEntity.getBooleanProperty( "showGainsPerUnit" ) && fullness1 != fullness2 )
				return fullness2 - fullness1;

			int inebriety1 = this.inebriety;
			int inebriety2 = ((Concoction)o).inebriety;

			if ( !StaticEntity.getBooleanProperty( "showGainsPerUnit" ) && inebriety1 != inebriety2 )
				return inebriety2 - inebriety1;

			float adventures1 = parseFloat( TradeableItemDatabase.getAdventureRange( this.name ) );
			float adventures2 = parseFloat( TradeableItemDatabase.getAdventureRange( ((Concoction)o).name ) );

			if ( adventures1 != adventures2 )
				return (int) (adventures2 - adventures1);

			return this.name.compareToIgnoreCase( ((Concoction)o).name );
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof Concoction) )
				return false;

			if ( this.name == null )
				return ((Concoction)o).name == null;

			if ( ((Concoction)o).name == null )
				return false;

			return this.name.equals( ((Concoction)o).name );
		}

		public AdventureResult getItem()
		{	return this.concoction;
		}

		public int getItemId()
		{	return this.concoction == null ? -1 : this.concoction.getItemId();
		}

		public String getName()
		{	return this.name;
		}

		public int getInitial()
		{	return this.initial;
		}

		public int getTotal()
		{	return this.visibleTotal;
		}

		public int getQueued()
		{	return this.queued;
		}

		public int getPrice()
		{	return this.price;
		}

		public int getFullness()
		{	return this.fullness;
		}

		public int getInebriety()
		{	return this.inebriety;
		}

		public void queue( ArrayList ingredientChange, int amount )
		{	queue( ingredientChange, amount, true );
		}

		public void queue( ArrayList ingredientChange, int amount, boolean adjust )
		{
			if ( amount <= 0 )
				return;

			if ( concoction == null )
			{
				if ( adjust )
					this.queued += amount;

				return;
			}

			int decrementAmount = Math.min( this.initial, amount );
			int overAmount = amount - decrementAmount;

			// Tiny plastic swords are special in that they
			// are not used up.

			if ( concoction.getItemId() != 938 )
			{
				AdventureResult ingredient = concoction.getInstance( decrementAmount );
				AdventureResult.addResultToList( ingredientChange, ingredient );
				AdventureResult.addResultToList( queuedIngredients, ingredient );
			}

			queuedAdventuresUsed += ADVENTURE_USAGE[ mixingMethod ] * overAmount;
			if ( mixingMethod == STILL_BOOZE || mixingMethod == STILL_MIXER )
				queuedStillsUsed += overAmount;

			if ( adjust )
				this.queued += amount;

			for ( int i = 0; i < this.ingredientArray.length; ++i )
				concoctions.get( this.ingredientArray[i].getItemId() ).queue( ingredientChange, overAmount, false );
		}

		public void resetCalculations()
		{
			this.initial = -1;
			this.creatable = 0;
			this.total = 0;

			this.modifier = 0;
			this.multiplier = 0;

			if ( this.concoction == null && this.name != null )
			{
				if ( TradeableItemDatabase.getFullness( this.name ) > 0 )
					this.initial = KoLCharacter.getAvailableMeat() / this.price;
				else
					this.initial = KoLCharacter.getAvailableMeat() / this.price;

				this.creatable = -1;
				this.total = this.initial;
			}
		}

		public void setPossible( boolean wasPossible )
		{	this.wasPossible = wasPossible;
		}

		public boolean wasPossible()
		{	return this.wasPossible;
		}

		public void addIngredient( AdventureResult ingredient )
		{
			SortedListModel uses = knownUses.get( ingredient.getItemId() );
			if ( uses == null )
			{
				uses = new SortedListModel();
				knownUses.set( ingredient.getItemId(), uses );
			}

			uses.add( this.concoction );
			this.ingredients.add( ingredient );

			this.ingredientArray = new AdventureResult[ this.ingredients.size() ];
			this.ingredients.toArray( this.ingredientArray );
		}

		public int getMixingMethod()
		{	return this.mixingMethod;
		}

		public AdventureResult [] getIngredients()
		{	return this.ingredientArray;
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

			if ( this.concoction == null || this.name == null )
				return;

			// Determine how many were available initially in the
			// available ingredient list.

			this.initial = this.concoction.getCount( availableIngredients );
			this.total = this.initial;

			if ( !isPermittedMethod( this.mixingMethod ) )
				return;

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < this.ingredientArray.length; ++i )
				concoctions.get( this.ingredientArray[i].getItemId() ).calculate( availableIngredients );

			this.mark( 0, 1 );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			if ( this.mixingMethod == ROLLING_PIN || this.mixingMethod == CLOVER )
			{
				// If there's only one ingredient, then the
				// quantity depends entirely on it.

				this.creatable = concoctions.get( this.ingredientArray[0].getItemId() ).initial;
				this.total = this.initial + this.creatable;
			}
			else
			{
				this.total = MallPurchaseRequest.MAX_QUANTITY;
				for ( int i = 0; i < this.ingredientArray.length; ++i )
					this.total = Math.min( this.total, concoctions.get( this.ingredientArray[i].getItemId() ).quantity() );

				if ( ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
					this.total = Math.min( this.total, adventureLimit.quantity() );

				if ( this.mixingMethod == STILL_MIXER || this.mixingMethod == STILL_BOOZE )
					this.total = Math.min( this.total, stillsLimit.quantity() );

				// The total available for other creations is equal
				// to the total, less the initial.

				this.creatable = this.total - this.initial;
			}

			// Now that all the calculations are complete, unmark
			// the ingredients so that later calculations can make
			// the correct calculations.

			this.visibleTotal = this.total;
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

			if ( this.mixingMethod == ROLLING_PIN || this.mixingMethod == CLOVER || !isPermittedMethod( this.mixingMethod ) )
				return quantity;

			// The true value is affected by the maximum value for
			// the ingredients.  Therefore, calculate the quantity
			// for all other ingredients to complete the solution
			// of the linear inequality.

			for ( int i = 0; quantity > 0 && i < this.ingredientArray.length; ++i )
				quantity = Math.min( quantity, concoctions.get( this.ingredientArray[i].getItemId() ).quantity() );

			// Adventures are also considered an ingredient; if
			// no adventures are necessary, the multiplier should
			// be zero and the infinite number available will have
			// no effect on the calculation.

			if ( ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
				quantity = Math.min( quantity, adventureLimit.quantity() );

			// Still uses are also considered an ingredient.

			if ( this.mixingMethod == STILL_MIXER || this.mixingMethod == STILL_BOOZE )
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

			if ( this.mixingMethod == ROLLING_PIN || this.mixingMethod == CLOVER || !isPermittedMethod( this.mixingMethod ) )
				return;

			// Mark all the ingredients, being sure to multiply
			// by the number of that ingredient needed in this
			// concoction.

			int instanceCount;

			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				boolean shouldMark = true;
				instanceCount = this.ingredientArray[i].getCount();

				for ( int j = 0; j < i; ++j )
					shouldMark &= this.ingredientArray[i].getItemId() != this.ingredientArray[j].getItemId();

				if ( shouldMark )
				{
					for ( int j = i + 1; j < this.ingredientArray.length; ++j )
						if ( this.ingredientArray[i].getItemId() == this.ingredientArray[j].getItemId() )
							instanceCount += this.ingredientArray[j].getCount();

					concoctions.get( this.ingredientArray[i].getItemId() ).mark(
							(this.modifier + this.initial) * instanceCount, this.multiplier * instanceCount );
				}
			}

			// Mark the implicit adventure ingredient, being
			// sure to multiply by the number of adventures
			// which are required for this mixture.

			if ( ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
				adventureLimit.mark( (this.modifier + this.initial) * ADVENTURE_USAGE[ this.mixingMethod ],
					this.multiplier * ADVENTURE_USAGE[ this.mixingMethod ] );

			if ( this.mixingMethod == STILL_MIXER || this.mixingMethod == STILL_BOOZE )
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

			for ( int i = 0; i < this.ingredientArray.length; ++i )
				concoctions.get( this.ingredientArray[i].getItemId() ).unmark();

			if ( ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
				adventureLimit.unmark();

			if ( this.mixingMethod == STILL_MIXER || this.mixingMethod == STILL_BOOZE )
				stillsLimit.unmark();
		}

		private int getMeatPasteNeeded( int quantityNeeded )
		{
			// Avoid mutual recursion.

			if ( this.mixingMethod != COMBINE || KoLCharacter.inMuscleSign() )
				return 0;

			// Count all the meat paste from the different
			// levels in the creation tree.

			int runningTotal = 0;
			for ( int i = 0; i < this.ingredientArray.length; ++i )
				runningTotal += concoctions.get( this.ingredientArray[i].getItemId() ).getMeatPasteNeeded( quantityNeeded );

			return runningTotal + quantityNeeded;
		}

		/**
		 * Returns the string form of this concoction.  This is
		 * basically the display name for the item created.
		 */

		public String toString()
		{	return this.name;
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

			while ( index >= this.internalList.size() )
				this.internalList.add( null );

			return (SortedListModel) this.internalList.get( index );
		}

		public void set( int index, SortedListModel value )
		{
			while ( index >= this.internalList.size() )
				this.internalList.add( null );

			this.internalList.set( index, value );
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

		public ConcoctionArray()
		{
			int maxItemId = TradeableItemDatabase.maxItemId();
			for ( int i = 0; i <= maxItemId; ++i )
			{
				this.internalList.add( new Concoction( TradeableItemDatabase.getItemName( i ) == null ? null : new AdventureResult( i, 0 ),
					NOCREATE ) );
			}
		}

		public Concoction get( int index )
		{
			if ( index < 0 )
				return null;

			return (Concoction) this.internalList.get( index );
		}

		public void set( int index, Concoction value )
		{	this.internalList.set( index, value );
		}

		public int size()
		{	return this.internalList.size();
		}
	}
}
