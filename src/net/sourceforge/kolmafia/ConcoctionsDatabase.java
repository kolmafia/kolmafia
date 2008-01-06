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

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class ConcoctionsDatabase
	extends KoLDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	public static final SortedListModel creatableList = new SortedListModel();
	public static final LockableListModel usableList = new LockableListModel();

	private static boolean tripleReagent = false;
	private static boolean ignoreRefresh = false;

	private static int queuedFullness = 0;
	private static int queuedInebriety = 0;
	private static int queuedAdventuresUsed = 0;
	private static int queuedStillsUsed = 0;

	private static final Stack queuedChanges = new Stack();
	private static final SortedListModel queuedIngredients = new SortedListModel();

	private static final Concoction stillsLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	private static final Concoction adventureLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );

	private static final ConcoctionArray concoctions = new ConcoctionArray();
	private static final SortedListModelArray knownUses = new SortedListModelArray();

	private static final boolean[] PERMIT_METHOD = new boolean[ KoLConstants.METHOD_COUNT ];
	private static final int[] ADVENTURE_USAGE = new int[ KoLConstants.METHOD_COUNT ];
	private static final AdventureResult[] NO_INGREDIENTS = new AdventureResult[ 0 ];

	private static final int CHEF = 438;
	private static final int CLOCKWORK_CHEF = 1112;
	private static final int BARTENDER = 440;
	private static final int CLOCKWORK_BARTENDER = 1111;

	public static final AdventureResult OVEN = new AdventureResult( 157, 1 );
	public static final AdventureResult KIT = new AdventureResult( 236, 1 );
	public static final AdventureResult HAMMER = new AdventureResult( 338, 1 );
	public static final AdventureResult PLIERS = new AdventureResult( 709, 1 );

	private static final AdventureResult PASTE = new AdventureResult( KoLConstants.MEAT_PASTE, 1 );
	private static final AdventureResult STACK = new AdventureResult( KoLConstants.MEAT_STACK, 1 );
	private static final AdventureResult DENSE = new AdventureResult( KoLConstants.DENSE_STACK, 1 );

	private static final int TOMATO = 246;
	public static final int WAD_DOUGH = 159;
	public static final int FLAT_DOUGH = 301;

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

		BufferedReader reader = KoLDatabase.getVersionedReader( "concoctions.txt", KoLConstants.CONCOCTIONS_VERSION );
		String[] data;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			ConcoctionsDatabase.addConcoction( data );
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

		for ( int i = 0; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			if ( ConcoctionsDatabase.concoctions.get( i ) != null )
			{
				ConcoctionsDatabase.usableList.add( ConcoctionsDatabase.concoctions.get( i ) );
			}
		}

		ConcoctionsDatabase.usableList.sort();
	}

	private static final void addConcoction( final String[] data )
	{
		// Need at least concoction name and mixing method
		if ( data.length <= 2 )
		{
			return;
		}

		boolean bogus = false;

		String name = data[ 0 ];
		AdventureResult item = AdventureResult.parseResult( name );
		int itemId = item.getItemId();

		if ( itemId <= 0 )
		{
			RequestLogger.printLine( "Unknown concoction: " + name );
			bogus = true;
		}

		int mixingMethod = StaticEntity.parseInt( data[ 1 ] );
		if ( mixingMethod <= 0 || mixingMethod >= KoLConstants.METHOD_COUNT )
		{
			RequestLogger.printLine( "Unknown mixing method (" + mixingMethod + ") for concoction: " + name );
			bogus = true;
		}

		AdventureResult[] ingredients = new AdventureResult[ data.length - 2 ];
		for ( int i = 2; i < data.length; ++i )
		{
			AdventureResult ingredient = ConcoctionsDatabase.parseIngredient( data[ i ] );
			if ( ingredient == null || ingredient.getItemId() == -1 || ingredient.getName() == null )
			{
				RequestLogger.printLine( "Unknown ingredient (" + data[ i ] + ") for concoction: " + name );
				bogus = true;
				continue;
			}

			ingredients[ i - 2 ] = ingredient;
		}

		if ( !bogus )
		{
			Concoction concoction = new Concoction( item, mixingMethod );

			for ( int i = 0; i < ingredients.length; ++i )
			{
				concoction.addIngredient( ingredients[ i ] );
			}
			ConcoctionsDatabase.concoctions.set( itemId, concoction );
		}
	}

	public static final boolean isKnownCombination( final AdventureResult[] ingredients )
	{
		// Known combinations which could not be added because
		// there are limitations in the item manager.

		if ( ingredients.length == 2 )
		{
			// Handle meat stacks, which are created from fairy
			// gravy and meat from yesterday.

			if ( ingredients[ 0 ].getItemId() == 80 && ingredients[ 1 ].getItemId() == 87 )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == 80 && ingredients[ 0 ].getItemId() == 87 )
			{
				return true;
			}

			// Handle plain pizza, which also allows flat dough
			// to be used instead of wads of dough.

			if ( ingredients[ 0 ].getItemId() == ConcoctionsDatabase.TOMATO && ingredients[ 1 ].getItemId() == ConcoctionsDatabase.FLAT_DOUGH )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionsDatabase.TOMATO && ingredients[ 0 ].getItemId() == ConcoctionsDatabase.FLAT_DOUGH )
			{
				return true;
			}

			// Handle catsup recipes, which only exist in the
			// item table as ketchup recipes.

			if ( ingredients[ 0 ].getItemId() == ConcoctionsDatabase.CATSUP.getItemId() )
			{
				ingredients[ 0 ] = ConcoctionsDatabase.KETCHUP;
				return ConcoctionsDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionsDatabase.CATSUP.getItemId() )
			{
				ingredients[ 1 ] = ConcoctionsDatabase.KETCHUP;
				return ConcoctionsDatabase.isKnownCombination( ingredients );
			}

			// Handle ice-cold beer recipes, which only uses the
			// recipe for item #41 at this time.

			if ( ingredients[ 0 ].getItemId() == ConcoctionsDatabase.WILLER.getItemId() )
			{
				ingredients[ 0 ] = ConcoctionsDatabase.SCHLITZ;
				return ConcoctionsDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionsDatabase.WILLER.getItemId() )
			{
				ingredients[ 1 ] = ConcoctionsDatabase.SCHLITZ;
				return ConcoctionsDatabase.isKnownCombination( ingredients );
			}

			// Handle cloaca recipes, which only exist in the
			// item table as dyspepsi cola.

			if ( ingredients[ 0 ].getItemId() == ConcoctionsDatabase.CLOACA.getItemId() )
			{
				ingredients[ 0 ] = ConcoctionsDatabase.DYSPEPSI;
				return ConcoctionsDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionsDatabase.CLOACA.getItemId() )
			{
				ingredients[ 1 ] = ConcoctionsDatabase.DYSPEPSI;
				return ConcoctionsDatabase.isKnownCombination( ingredients );
			}
		}

		int[] ingredientTestIds;
		AdventureResult[] ingredientTest;

		for ( int i = 0; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			ingredientTest = ConcoctionsDatabase.concoctions.get( i ).getIngredients();
			if ( ingredientTest.length != ingredients.length )
			{
				continue;
			}

			ingredientTestIds = new int[ ingredients.length ];
			for ( int j = 0; j < ingredientTestIds.length; ++j )
			{
				ingredientTestIds[ j ] = ingredientTest[ j ].getItemId();
			}

			boolean foundMatch = true;
			for ( int j = 0; j < ingredients.length && foundMatch; ++j )
			{
				foundMatch = false;
				for ( int k = 0; k < ingredientTestIds.length && !foundMatch; ++k )
				{
					foundMatch |= ingredients[ j ].getItemId() == ingredientTestIds[ k ];
					if ( foundMatch )
					{
						ingredientTestIds[ k ] = -1;
					}
				}
			}

			if ( foundMatch )
			{
				return true;
			}
		}

		return false;
	}

	public static final SortedListModel getKnownUses( final int itemId )
	{
		SortedListModel uses = ConcoctionsDatabase.knownUses.get( itemId );
		return uses == null ? ConcoctionsDatabase.EMPTY_LIST : uses;
	}

	public static final SortedListModel getKnownUses( final AdventureResult item )
	{
		return ConcoctionsDatabase.getKnownUses( item.getItemId() );
	}

	public static final boolean isPermittedMethod( final int method )
	{
		return ConcoctionsDatabase.PERMIT_METHOD[ method ];
	}

	private static final AdventureResult parseIngredient( final String data )
	{
		// If the ingredient is specified inside of brackets,
		// then a specific item Id is being designated.

		if ( data.startsWith( "[" ) )
		{
			int closeBracketIndex = data.indexOf( "]" );
			String itemIdString = data.substring( 0, closeBracketIndex ).replaceAll( "[\\[\\]]", "" ).trim();
			String quantityString = data.substring( closeBracketIndex + 1 ).trim();

			return new AdventureResult(
				StaticEntity.parseInt( itemIdString ),
				quantityString.length() == 0 ? 1 : StaticEntity.parseInt( quantityString.replaceAll( "[\\(\\)]", "" ) ) );
		}

		// Otherwise, it's a standard ingredient - use
		// the standard adventure result parsing routine.

		return AdventureResult.parseResult( data );
	}

	public static final SortedListModel getQueue()
	{
		return ConcoctionsDatabase.queuedIngredients;
	}

	public static final void push( final Concoction c, final int quantity )
	{
		int adventureChange = ConcoctionsDatabase.queuedAdventuresUsed;
		int stillChange = ConcoctionsDatabase.queuedStillsUsed;

		ConcoctionsDatabase.queuedFullness += c.getFullness() * quantity;
		ConcoctionsDatabase.queuedInebriety += c.getInebriety() * quantity;

		ArrayList ingredientChange = new ArrayList();
		c.queue( ingredientChange, quantity );

		adventureChange -= ConcoctionsDatabase.queuedAdventuresUsed;
		stillChange -= ConcoctionsDatabase.queuedStillsUsed;

		ConcoctionsDatabase.queuedChanges.push( new Integer( stillChange ) );
		ConcoctionsDatabase.queuedChanges.push( new Integer( adventureChange ) );

		ConcoctionsDatabase.queuedChanges.push( ingredientChange );
		ConcoctionsDatabase.queuedChanges.push( new Integer( quantity ) );
		ConcoctionsDatabase.queuedChanges.push( c );
	}

	public static final void pop()
	{
		if ( ConcoctionsDatabase.queuedChanges.isEmpty() )
		{
			return;
		}

		Concoction c = (Concoction) ConcoctionsDatabase.queuedChanges.pop();
		Integer quantity = (Integer) ConcoctionsDatabase.queuedChanges.pop();
		ArrayList ingredientChange = (ArrayList) ConcoctionsDatabase.queuedChanges.pop();

		Integer adventureChange = (Integer) ConcoctionsDatabase.queuedChanges.pop();
		Integer stillChange = (Integer) ConcoctionsDatabase.queuedChanges.pop();

		c.queued -= quantity.intValue();
		for ( int i = 0; i < ingredientChange.size(); ++i )
		{
			AdventureResult.addResultToList(
				ConcoctionsDatabase.queuedIngredients, ( (AdventureResult) ingredientChange.get( i ) ).getNegation() );
		}

		int advs = adventureChange.intValue();
		if ( advs != 0 )
		{
			ConcoctionsDatabase.queuedAdventuresUsed += advs;
			AdventureResult.addResultToList(
				ConcoctionsDatabase.queuedIngredients,
				new AdventureResult( AdventureResult.ADV, advs ) );
		}
		ConcoctionsDatabase.queuedStillsUsed += stillChange.intValue();

		ConcoctionsDatabase.queuedFullness -= c.getFullness() * quantity.intValue();
		ConcoctionsDatabase.queuedInebriety -= c.getInebriety() * quantity.intValue();
	}

	public static final LockableListModel getUsables()
	{
		return ConcoctionsDatabase.usableList;
	}

	public static final SortedListModel getCreatables()
	{
		return ConcoctionsDatabase.creatableList;
	}

	public static final void handleQueue( boolean consume )
	{

		ConcoctionsDatabase.queuedChanges.clear();
		ConcoctionsDatabase.queuedIngredients.clear();
		ConcoctionsDatabase.refreshConcoctions();

		ConcoctionsDatabase.queuedStillsUsed = 0;
		ConcoctionsDatabase.queuedAdventuresUsed = 0;
		ConcoctionsDatabase.queuedFullness = 0;
		ConcoctionsDatabase.queuedInebriety = 0;

		ConcoctionsDatabase.ignoreRefresh = true;

		RequestThread.openRequestSequence();
		KoLmafia.updateDisplay( "Processing queued items..." );

		SpecialOutfit.createImplicitCheckpoint();
		Concoction c;

		for ( int i = 0; i < ConcoctionsDatabase.usableList.size(); ++i )
		{
			c = (Concoction) ConcoctionsDatabase.usableList.get( i );

			if ( c.getQueued() == 0 )
			{
				continue;
			}

			KoLRequest request = null;

			if ( !consume && c.getItem() != null )
			{
				AdventureDatabase.retrieveItem( c.getItem().getInstance( c.getQueued() ) );
			}
			else if ( c.getPrice() > 0 )
			{
				String name = c.getName();
				if ( KitchenRequest.onMenu( name ) )
				{
					request = new KitchenRequest( name );
				}
				else if ( RestaurantRequest.onMenu( name ) )
				{
					request = new RestaurantRequest( name );
				}
				else if ( MicrobreweryRequest.onMenu( name ) )
				{
					request = new MicrobreweryRequest( name );
				}
			}
			else
			{
				request = new ConsumeItemRequest( c.getItem().getInstance( c.getQueued() ) );
			}

			if ( request != null )
			{
				if ( request instanceof ConsumeItemRequest )
				{
					RequestThread.postRequest( request );
				}
				else
				{
					for ( int j = 0; j < c.getQueued(); ++j )
					{
						RequestThread.postRequest( request );
					}
				}
			}

			c.queued = 0;
		}

		SpecialOutfit.restoreImplicitCheckpoint();
		RequestThread.closeRequestSequence();

		ConcoctionsDatabase.ignoreRefresh = false;
		ConcoctionsDatabase.refreshConcoctions();
	}

	public static final int getQueuedFullness()
	{
		return ConcoctionsDatabase.queuedFullness;
	}

	public static final int getQueuedInebriety()
	{
		return ConcoctionsDatabase.queuedInebriety;
	}

	private static final List getAvailableIngredients()
	{
		boolean includeCloset = !KoLConstants.closet.isEmpty();
		boolean includeStorage = KoLCharacter.canInteract() && !KoLConstants.storage.isEmpty();
		boolean includeStash =
			KoLCharacter.canInteract() && KoLSettings.getBooleanProperty( "autoSatisfyWithStash" ) && !ClanManager.getStash().isEmpty();
		boolean includeQueue = !ConcoctionsDatabase.queuedIngredients.isEmpty();

		if ( !includeCloset && !includeStash && !includeQueue )
		{
			return KoLConstants.inventory;
		}

		ArrayList availableIngredients = new ArrayList();
		availableIngredients.addAll( KoLConstants.inventory );

		if ( includeCloset )
		{
			for ( int i = 0; i < KoLConstants.closet.size(); ++i )
			{
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) KoLConstants.closet.get( i ) );
			}
		}

		if ( includeStorage )
		{
			for ( int i = 0; i < KoLConstants.storage.size(); ++i )
			{
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) KoLConstants.storage.get( i ) );
			}
		}

		if ( includeStash )
		{
			List stash = ClanManager.getStash();
			for ( int i = 0; i < stash.size(); ++i )
			{
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) stash.get( i ) );
			}
		}

		if ( includeQueue )
		{
			for ( int i = 0; i < ConcoctionsDatabase.queuedIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionsDatabase.queuedIngredients.get( i );
				if ( ingredient.isItem() )
				{
					AdventureResult.addResultToList(
						availableIngredients,
						ingredient.getNegation() );
				}
			}
		}

		return availableIngredients;
	}

	private static final void setBetterIngredient( final AdventureResult item1, final AdventureResult item2,
		final List availableIngredients )
	{
		Concoction item;
		int available =
			ConcoctionsDatabase.getBetterIngredient( item1, item2, availableIngredients ).getCount(
				availableIngredients );

		item = ConcoctionsDatabase.concoctions.get( item1.getItemId() );
		item.initial = available;
		item.creatable = 0;
		item.total = available;
		item.visibleTotal = available;

		item = ConcoctionsDatabase.concoctions.get( item2.getItemId() );
		item.initial = available;
		item.creatable = 0;
		item.total = available;
		item.visibleTotal = available;
	}

	/**
	 * Returns the concoctions which are available given the list of ingredients. The list returned contains formal
	 * requests for item creation.
	 */

	public static final synchronized void refreshConcoctions()
	{
		List availableIngredients = ConcoctionsDatabase.getAvailableIngredients();

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		Concoction item;
		for ( int i = 1; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionsDatabase.concoctions.get( i );
			if ( item != null )
			{
				item.resetCalculations();
			}
		}

		// Make assessment of availability of mixing methods.
		// This method will also calculate the availability of
		// chefs and bartenders automatically so a second call
		// is not needed.

		boolean includeNPCs = KoLSettings.getBooleanProperty( "autoSatisfyWithNPCs" );

		// Next, do calculations on all mixing methods which cannot
		// be created at this time.

		for ( int i = 1; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionsDatabase.concoctions.get( i );
			if ( item == null )
			{
				continue;
			}

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
			{
				continue;
			}

			if ( includeNPCs && NPCStoreDatabase.contains( concoction.getName(), true ) )
			{
				int price = NPCStoreDatabase.price( concoction.getName() );
				item.initial = concoction.getCount( availableIngredients ) + KoLCharacter.getAvailableMeat() / price;
				item.creatable = 0;
				item.total = item.initial;
				item.visibleTotal = item.total;
			}
		}

		ConcoctionsDatabase.cachePermitted( availableIngredients );

		for ( int i = 1; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionsDatabase.concoctions.get( i );
			if ( item == null )
			{
				continue;
			}

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
			{
				continue;
			}

			if ( !ConcoctionsDatabase.isPermittedMethod( item.getMixingMethod() ) && item.initial == -1 )
			{
				item.initial = concoction.getCount( availableIngredients );
				item.creatable = 0;
				item.total = item.initial;
				item.visibleTotal = item.total;
			}
		}

		ConcoctionsDatabase.calculateBasicItems( availableIngredients );

		// Ice-cold beer and ketchup are special instances -- for the
		// purposes of calculation, we assume that they will use the
		// ingredient which is present in the greatest quantity.

		ConcoctionsDatabase.setBetterIngredient(
			ConcoctionsDatabase.DYSPEPSI, ConcoctionsDatabase.CLOACA, availableIngredients );
		ConcoctionsDatabase.setBetterIngredient(
			ConcoctionsDatabase.SCHLITZ, ConcoctionsDatabase.WILLER, availableIngredients );
		ConcoctionsDatabase.setBetterIngredient(
			ConcoctionsDatabase.KETCHUP, ConcoctionsDatabase.CATSUP, availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionsDatabase.concoctions.get( i );
			if ( item != null && item.toString() != null && item.creatable > -1 )
			{
				item.calculate( availableIngredients );
			}
		}

		// Now, to update the list of creatables without removing
		// all creatable items.  We do this by determining the
		// number of items inside of the old list.

		ItemCreationRequest instance;
		boolean changeDetected = false;

		for ( int i = 1; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionsDatabase.concoctions.get( i );
			if ( item == null )
			{
				continue;
			}

			instance = ItemCreationRequest.getInstance( i, false );
			if ( instance == null || item.creatable == instance.getQuantityPossible() )
			{
				continue;
			}

			instance.setQuantityPossible( item.creatable );

			if ( instance.getQuantityPossible() == 0 )
			{
				// We can't make this concoction now

				if ( item.wasPossible() )
				{
					ConcoctionsDatabase.creatableList.remove( instance );
					item.setPossible( false );
				}
			}
			else if ( !item.wasPossible() )
			{
				ConcoctionsDatabase.creatableList.add( instance );
				item.setPossible( true );
			}
			else
			{
				changeDetected = true;
			}
		}

		if ( !ConcoctionsDatabase.ignoreRefresh )
		{
			ConcoctionsDatabase.creatableList.updateFilter( changeDetected );
			ConcoctionsDatabase.usableList.updateFilter( changeDetected );
		}
	}

	public static final int getMeatPasteRequired( final int itemId, final int creationCount )
	{
		Concoction item = ConcoctionsDatabase.concoctions.get( itemId );
		if ( item == null )
		{
			return 0;
		}

		List availableIngredients = ConcoctionsDatabase.getAvailableIngredients();
		item.calculate( availableIngredients );
		return item.getMeatPasteNeeded( creationCount + item.initial ) - item.initial + 1;
	}

	private static final void calculateBasicItems( final List availableIngredients )
	{
		// Meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.

		ConcoctionsDatabase.setBasicItem(
			availableIngredients, ConcoctionsDatabase.PASTE, KoLCharacter.getAvailableMeat() / 10 );
		ConcoctionsDatabase.setBasicItem(
			availableIngredients, ConcoctionsDatabase.STACK, KoLCharacter.getAvailableMeat() / 100 );
		ConcoctionsDatabase.setBasicItem(
			availableIngredients, ConcoctionsDatabase.DENSE, KoLCharacter.getAvailableMeat() / 1000 );

		AdventureResult item;
		int worthlessItems = Math.min( HermitRequest.getWorthlessItemCount(), KoLCharacter.getAvailableMeat() / 100 );

		for ( int i = 0; i < KoLConstants.hermitItems.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.hermitItems.get( i );
			if ( item.getItemId() != SewerRequest.TEN_LEAF_CLOVER )
			{
				ConcoctionsDatabase.setBasicItem( availableIngredients, item, worthlessItems );
			}
		}

		int furCount = CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory );
		for ( int i = 0; i < KoLConstants.trapperItems.size(); ++i )
		{
			ConcoctionsDatabase.setBasicItem(
				availableIngredients, (AdventureResult) KoLConstants.trapperItems.get( i ), furCount );
		}
	}

	private static final void setBasicItem( final List availableIngredients, final AdventureResult item,
		final int creatable )
	{
		Concoction creation = ConcoctionsDatabase.concoctions.get( item.getItemId() );
		if ( creation == null )
		{
			return;
		}

		creation.initial = item.getCount( availableIngredients );
		creation.creatable = creatable;
		creation.total = creation.initial + creatable;
		creation.visibleTotal = creation.total;
	}

	/**
	 * Utility method used to cache the current permissions on item creation.
	 */

	private static final void cachePermitted( final List availableIngredients )
	{
		ConcoctionsDatabase.tripleReagent = KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR );
		boolean willBuyServant = KoLCharacter.canInteract() && KoLSettings.getBooleanProperty( "autoSatisfyWithMall" );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		ConcoctionsDatabase.adventureLimit.initial =
			KoLCharacter.getAdventuresLeft() - ConcoctionsDatabase.queuedAdventuresUsed;
		ConcoctionsDatabase.adventureLimit.creatable = 0;
		ConcoctionsDatabase.adventureLimit.total = KoLCharacter.getAdventuresLeft();
		ConcoctionsDatabase.adventureLimit.visibleTotal = ConcoctionsDatabase.adventureLimit.total;

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		ConcoctionsDatabase.stillsLimit.initial =
			KoLCharacter.getStillsAvailable() - ConcoctionsDatabase.queuedStillsUsed;
		ConcoctionsDatabase.stillsLimit.creatable = 0;
		ConcoctionsDatabase.stillsLimit.total = ConcoctionsDatabase.stillsLimit.initial;
		ConcoctionsDatabase.stillsLimit.visibleTotal = ConcoctionsDatabase.stillsLimit.total;

		ConcoctionsDatabase.calculateBasicItems( availableIngredients );

		// It is never possible to create items which are flagged
		// NOCREATE, and it is always possible to create items
		// through meat paste combination.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.NOCREATE ] = false;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.NOCREATE ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COMBINE ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COMBINE ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.CLOVER ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.CLOVER ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.CATALYST ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.CATALYST ] = 0;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.TINKER ] = KoLCharacter.inMoxieSign();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.TINKER ] = 0;

		// Smithing of items is possible whenever the person
		// has a hammer.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] =
			KoLConstants.inventory.contains( ConcoctionsDatabase.HAMMER ) || KoLCharacter.getAvailableMeat() >= 1000;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 1;

		// Advanced smithing is available whenever the person can
		// smith and has access to the appropriate skill.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SMITH_WEAPON ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] && KoLCharacter.canSmithWeapons() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH_WEAPON ] = 1;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SMITH_ARMOR ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] && KoLCharacter.canSmithArmor() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH_ARMOR ] = 1;

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.inMuscleSign() )
		{
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] = true;
			ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 0;
		}

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.JEWELRY ] =
			KoLConstants.inventory.contains( ConcoctionsDatabase.PLIERS );
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.JEWELRY ] = 3;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.EXPENSIVE_JEWELRY ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.JEWELRY ] && KoLCharacter.canCraftExpensiveJewelry();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.EXPENSIVE_JEWELRY ] = 3;

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.STARCHART ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.STARCHART ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.PIXEL ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.PIXEL ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MULTI_USE ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MULTI_USE ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SINGLE_USE ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.SINGLE_USE ] = 0;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.ROLLING_PIN ] = true;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.ROLLING_PIN ] = 0;

		// Rodoric will make chefstaves for mysticality class
		// characters who can get to the guild.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.STAFF ] = KoLCharacter.isMysticalityClass();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.STAFF ] = 0;

		// It's not possible to ask Uncle Crimbo 2005 to make toys

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.TOY ] = false;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.TOY ] = 0;

		// It's not possible to ask Ugh Crimbo 2006 to make toys

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.UGH ] = false;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.UGH ] = 0;

		// It's not possible to ask Uncle Crimbo 2007 to make toys

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.CRIMBO ] = false;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.CRIMBO ] = 0;

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		ConcoctionsDatabase.concoctions.get( ConcoctionsDatabase.CHEF ).calculate( availableIngredients );
		ConcoctionsDatabase.concoctions.get( ConcoctionsDatabase.CLOCKWORK_CHEF ).calculate( availableIngredients );
		ConcoctionsDatabase.concoctions.get( ConcoctionsDatabase.BARTENDER ).calculate( availableIngredients );
		ConcoctionsDatabase.concoctions.get( ConcoctionsDatabase.CLOCKWORK_BARTENDER ).calculate( availableIngredients );

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
			willBuyServant || KoLCharacter.hasChef() || ConcoctionsDatabase.isAvailable(
				ConcoctionsDatabase.CHEF, ConcoctionsDatabase.CLOCKWORK_CHEF );
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ] = 0;

		boolean useMall = KoLSettings.getBooleanProperty( "autoSatisfyWithMall" );
		boolean useStash = KoLSettings.getBooleanProperty( "autoSatisfyWithStash" );

		if ( !ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] )
		{
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
				KoLCharacter.canInteract() && ( useMall || useStash );

			if ( !ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && !KoLSettings.getBooleanProperty( "requireBoxServants" ) )
			{
				ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
					KoLConstants.inventory.contains( ConcoctionsDatabase.OVEN ) || KoLCharacter.getAvailableMeat() >= 1000;
				ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ] = 1;
			}
		}

		// Since catalysts aren't purchasable, you can only
		// create items based on them if it's already in your
		// inventory.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.CATALYST ] =
			KoLCharacter.isMysticalityClass() || KoLCharacter.getClassType().equals(  KoLCharacter.ACCORDION_THIEF );

		// Cooking of reagents and noodles is possible whenever
		// the person can cook and has the appropriate skill.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK_REAGENT ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && KoLCharacter.canSummonReagent();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_REAGENT ] =
			ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ];

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.SUPER_REAGENT ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && KoLCharacter.hasSkill( "The Way of Sauce" );
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.SUPER_REAGENT ] =
			ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ];

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK_PASTA ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && KoLCharacter.canSummonNoodles();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_PASTA ] =
			ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ];

		// Mixing is possible whenever the person has a bartender
		// or they don't need a box servant and have a kit.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
			KoLCharacter.hasBartender() || ConcoctionsDatabase.isAvailable(
				ConcoctionsDatabase.BARTENDER, ConcoctionsDatabase.CLOCKWORK_BARTENDER );
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 0;

		if ( !ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] )
		{
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
				KoLCharacter.canInteract() && ( useMall || useStash );

			if ( !ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && !KoLSettings.getBooleanProperty( "requireBoxServants" ) )
			{
				ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
					KoLConstants.inventory.contains( ConcoctionsDatabase.KIT ) || KoLCharacter.getAvailableMeat() >= 1000;
				ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 1;
			}
		}

		// Mixing of advanced drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX_SPECIAL ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && KoLCharacter.canSummonShore();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_SPECIAL ] =
			ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ];

		// Mixing of super-special drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX_SUPER ] =
			ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" );
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_SUPER ] =
			ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ];

		// Using Crosby Nash's Still is possible if the person has
		// Superhuman Cocktailcrafting and is a Moxie class character.

		boolean hasStillsAvailable = ConcoctionsDatabase.stillsLimit.total > 0;
		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.STILL_MIXER ] = hasStillsAvailable;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.STILL_MIXER ] = 0;

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.STILL_BOOZE ] = hasStillsAvailable;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.STILL_BOOZE ] = 0;

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.WOK ] =
			KoLCharacter.canUseWok() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.WOK ] = 1;

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		ConcoctionsDatabase.PERMIT_METHOD[ KoLConstants.MALUS ] = KoLCharacter.canUseMalus();
		ConcoctionsDatabase.ADVENTURE_USAGE[ KoLConstants.MALUS ] = 0;

		// Now, go through all the cached adventure usage values and if
		// the number of adventures left is zero and the request requires
		// adventures, it is not permitted.

		for ( int i = 0; i < KoLConstants.METHOD_COUNT; ++i )
		{
			if ( ConcoctionsDatabase.PERMIT_METHOD[ i ] && ConcoctionsDatabase.ADVENTURE_USAGE[ i ] > 0 )
			{
				ConcoctionsDatabase.PERMIT_METHOD[ i ] =
					ConcoctionsDatabase.ADVENTURE_USAGE[ i ] <= KoLCharacter.getAdventuresLeft();
			}
		}
	}

	private static final boolean isAvailable( final int servantId, final int clockworkId )
	{
		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		return KoLSettings.getBooleanProperty( "autoRepairBoxServants" ) && ConcoctionsDatabase.concoctions.get( servantId ).total > 0 || ConcoctionsDatabase.concoctions.get( clockworkId ).total > 0;
	}

	/**
	 * Returns the mixing method for the item with the given Id.
	 */

	public static final int getMixingMethod( final int itemId )
	{
		Concoction item = ConcoctionsDatabase.concoctions.get( itemId );
		return item == null ? KoLConstants.NOCREATE : item.getMixingMethod();
	}

	/**
	 * Returns the item Ids of the ingredients for the given item. Note that if there are no ingredients, then
	 * <code>null</code> will be returned instead.
	 */

	public static final AdventureResult[] getIngredients( final int itemId )
	{
		List availableIngredients = ConcoctionsDatabase.getAvailableIngredients();

		// Ensure that you're retrieving the same ingredients that
		// were used in the calculations.  Usually this is the case,
		// but ice-cold beer and ketchup are tricky cases.

		AdventureResult[] ingredients = ConcoctionsDatabase.getStandardIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( ingredients[ i ].getItemId() == ConcoctionsDatabase.SCHLITZ.getItemId() || ingredients[ i ].getItemId() == ConcoctionsDatabase.WILLER.getItemId() )
			{
				ingredients[ i ] =
					ConcoctionsDatabase.getBetterIngredient(
						ConcoctionsDatabase.SCHLITZ, ConcoctionsDatabase.WILLER, availableIngredients );
			}
			else if ( ingredients[ i ].getItemId() == ConcoctionsDatabase.KETCHUP.getItemId() || ingredients[ i ].getItemId() == ConcoctionsDatabase.CATSUP.getItemId() )
			{
				ingredients[ i ] =
					ConcoctionsDatabase.getBetterIngredient(
						ConcoctionsDatabase.KETCHUP, ConcoctionsDatabase.CATSUP, availableIngredients );
			}
		}

		return ingredients;
	}

	public static final AdventureResult[] getStandardIngredients( final int itemId )
	{
		Concoction item = ConcoctionsDatabase.concoctions.get( itemId );
		return item == null ? ConcoctionsDatabase.NO_INGREDIENTS : item.getIngredients();
	}

	/**
	 * Find a concoction made in a particular way that includes the specified ingredient
	 */

	public static final int findConcoction( final int mixingMethod, final int itemId )
	{
		AdventureResult ingredient = new AdventureResult( itemId, 1 );
		for ( int i = 0; i < ConcoctionsDatabase.concoctions.size(); ++i )
		{
			Concoction concoction = ConcoctionsDatabase.concoctions.get( i );
			if ( concoction == null || concoction.getMixingMethod() != mixingMethod )
			{
				continue;
			}

			AdventureResult[] ingredients = ConcoctionsDatabase.getStandardIngredients( i );
			if ( ingredients == null )
			{
				continue;
			}

			for ( int j = 0; j < ingredients.length; ++j )
			{
				if ( ingredients[ j ].equals( ingredient ) )
				{
					return i;
				}
			}
		}
		return -1;
	}

	private static final AdventureResult getBetterIngredient( final AdventureResult ingredient1,
		final AdventureResult ingredient2, final List availableIngredients )
	{
		return ingredient1.getCount( availableIngredients ) > ingredient2.getCount( availableIngredients ) ? ingredient1 : ingredient2;
	}

	/**
	 * Internal class used to represent a single concoction. It contains all the information needed to actually make the
	 * item.
	 */

	public static class Concoction
		implements Comparable
	{
		private final AdventureResult concoction;
		private final int mixingMethod;
		private int sortOrder;
		private boolean wasPossible;

		private String name;
		private final int price;

		private final List ingredients;
		private AdventureResult[] ingredientArray;

		private int modifier, multiplier;
		private int initial, creatable, total, queued;
		private final int fullness, inebriety;
		private int visibleTotal;

		public Concoction( final String name, final int price )
		{
			this.name = name;
			this.concoction = null;

			this.mixingMethod = KoLConstants.NOCREATE;
			this.wasPossible = true;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[ 0 ];

			this.fullness = TradeableItemDatabase.getFullness( name );
			this.inebriety = TradeableItemDatabase.getInebriety( name );

			int consumeType = this.fullness == 0 ? KoLConstants.CONSUME_DRINK : KoLConstants.CONSUME_EAT;

			switch ( consumeType )
			{
			case CONSUME_EAT:
				this.sortOrder = this.fullness > 0 ? 1 : 3;
				break;
			case CONSUME_DRINK:
				this.sortOrder = this.inebriety > 0 ? 2 : 3;
				break;
			default:
				this.sortOrder = 3;
				break;
			}

			this.price = price;
			this.resetCalculations();
		}

		public Concoction( final AdventureResult concoction, final int mixingMethod )
		{
			this.concoction = concoction;

			if ( concoction != null )
			{
				this.name = concoction.getName();
			}

			this.mixingMethod = mixingMethod;
			this.wasPossible = false;

			this.fullness = TradeableItemDatabase.getFullness( this.name );
			this.inebriety = TradeableItemDatabase.getInebriety( this.name );

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[ 0 ];

			int consumeType =
				concoction == null ? 0 : TradeableItemDatabase.getConsumptionType( concoction.getItemId() );

			switch ( consumeType )
			{
			case CONSUME_EAT:
				this.sortOrder = this.fullness > 0 ? 1 : 3;
				break;
			case CONSUME_DRINK:
				this.sortOrder = this.inebriety > 0 ? 2 : 3;
				break;
			default:
				this.sortOrder = 3;
				break;
			}

			this.price = -1;
		}

		public int compareTo( final Object o )
		{
			if ( o == null || !( o instanceof Concoction ) )
			{
				return -1;
			}

			if ( this.name == null )
			{
				return ( (Concoction) o ).name == null ? 0 : 1;
			}

			if ( ( (Concoction) o ).name == null )
			{
				return -1;
			}

			if ( this.sortOrder != ( (Concoction) o ).sortOrder )
			{
				return this.sortOrder - ( (Concoction) o ).sortOrder;
			}

			if ( this.sortOrder == 3 )
			{
				return this.name.compareToIgnoreCase( ( (Concoction) o ).name );
			}

			if ( !KoLSettings.getBooleanProperty( "showGainsPerUnit" ) )
			{
				int fullness1 = this.fullness;
				int fullness2 = ( (Concoction) o ).fullness;

				if ( fullness1 != fullness2 )
				{
					return fullness2 - fullness1;
				}

				int inebriety1 = this.inebriety;
				int inebriety2 = ( (Concoction) o ).inebriety;

				if ( inebriety1 != inebriety2 )
				{
					return inebriety2 - inebriety1;
				}
			}

			float adventures1 = StaticEntity.parseFloat( TradeableItemDatabase.getAdventureRange( this.name ) );
			float adventures2 =
				StaticEntity.parseFloat( TradeableItemDatabase.getAdventureRange( ( (Concoction) o ).name ) );

			if ( adventures1 != adventures2 )
			{
				return adventures2 - adventures1 > 0.0f ? 1 : -1;
			}

			return this.name.compareToIgnoreCase( ( (Concoction) o ).name );
		}

		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof Concoction ) )
			{
				return false;
			}

			if ( this.name == null )
			{
				return ( (Concoction) o ).name == null;
			}

			if ( ( (Concoction) o ).name == null )
			{
				return false;
			}

			return this.name.equals( ( (Concoction) o ).name );
		}

		public AdventureResult getItem()
		{
			return this.concoction;
		}

		public int getItemId()
		{
			return this.concoction == null ? -1 : this.concoction.getItemId();
		}

		public String getName()
		{
			return this.name;
		}

		public int getInitial()
		{
			return this.initial;
		}

		public int getTotal()
		{
			return this.price > 0 ? KoLCharacter.getAvailableMeat() / this.price : this.visibleTotal;
		}

		public int getQueued()
		{
			return this.queued;
		}

		public int getPrice()
		{
			return this.price;
		}

		public int getFullness()
		{
			return this.fullness;
		}

		public int getInebriety()
		{
			return this.inebriety;
		}

		public void queue( final ArrayList ingredientChange, final int amount )
		{
			this.queue( ingredientChange, amount, true );
		}

		public void queue( final ArrayList ingredientChange, final int amount, final boolean adjust )
		{
			if ( amount <= 0 )
			{
				return;
			}

			if ( this.concoction == null )
			{
				if ( adjust )
				{
					this.queued += amount;
				}

				return;
			}

			int decrementAmount = Math.min( this.initial, amount );
			int overAmount = amount - decrementAmount;

			// Tiny plastic swords are special in that they
			// are not used up.

			if ( this.concoction.getItemId() != 938 )
			{
				AdventureResult ingredient = this.concoction.getInstance( decrementAmount );
				AdventureResult.addResultToList( ingredientChange, ingredient );
				AdventureResult.addResultToList( ConcoctionsDatabase.queuedIngredients, ingredient );
			}

			int advs = ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ] * overAmount;
			if ( advs != 0 )
			{
				ConcoctionsDatabase.queuedAdventuresUsed += advs;
				AdventureResult.addResultToList( ConcoctionsDatabase.queuedIngredients, new AdventureResult( AdventureResult.ADV, advs ) );
			}

			if ( this.mixingMethod == KoLConstants.STILL_BOOZE || this.mixingMethod == KoLConstants.STILL_MIXER )
			{
				ConcoctionsDatabase.queuedStillsUsed += overAmount;
			}

			if ( adjust )
			{
				this.queued += amount;
			}

			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				ConcoctionsDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).queue(
					ingredientChange, overAmount, false );
			}
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
				{
					this.initial = KoLCharacter.getAvailableMeat() / this.price;
				}
				else
				{
					this.initial = KoLCharacter.getAvailableMeat() / this.price;
				}

				this.creatable = -1;
				this.total = this.initial;
			}
		}

		public void setPossible( final boolean wasPossible )
		{
			this.wasPossible = wasPossible;
		}

		public boolean wasPossible()
		{
			return this.wasPossible;
		}

		public void addIngredient( final AdventureResult ingredient )
		{
			SortedListModel uses = ConcoctionsDatabase.knownUses.get( ingredient.getItemId() );
			if ( uses == null )
			{
				uses = new SortedListModel();
				ConcoctionsDatabase.knownUses.set( ingredient.getItemId(), uses );
			}

			uses.add( this.concoction );
			this.ingredients.add( ingredient );

			this.ingredientArray = new AdventureResult[ this.ingredients.size() ];
			this.ingredients.toArray( this.ingredientArray );
		}

		public int getMixingMethod()
		{
			return this.mixingMethod;
		}

		public AdventureResult[] getIngredients()
		{
			return this.ingredientArray;
		}

		public void calculate( final List availableIngredients )
		{
			// If a calculation has already been done for this
			// concoction, no need to calculate again.

			if ( this.initial != -1 )
			{
				return;
			}

			// Initialize creatable item count to 0.  This way,
			// you ensure that you're not always off by one.

			this.creatable = 0;

			// If the item doesn't exist in the item table,
			// then assume it can't be created.

			if ( this.concoction == null || this.name == null )
			{
				return;
			}

			// Determine how many were available initially in the
			// available ingredient list.

			this.initial = this.concoction.getCount( availableIngredients );
			this.total = this.initial;

			if ( !ConcoctionsDatabase.isPermittedMethod( this.mixingMethod ) )
			{
				this.visibleTotal = this.total;
				return;
			}

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				ConcoctionsDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).calculate(
					availableIngredients );
			}

			this.mark( 0, 1 );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			if ( this.mixingMethod == KoLConstants.ROLLING_PIN || this.mixingMethod == KoLConstants.CLOVER )
			{
				// If there's only one ingredient, then the
				// quantity depends entirely on it.

				this.creatable = ConcoctionsDatabase.concoctions.get( this.ingredientArray[ 0 ].getItemId() ).initial;
				this.total = this.initial + this.creatable;
			}
			else
			{
				this.total = MallPurchaseRequest.MAX_QUANTITY;
				for ( int i = 0; i < this.ingredientArray.length; ++i )
				{
					this.total =
						Math.min( this.total, ConcoctionsDatabase.concoctions.get(
							this.ingredientArray[ i ].getItemId() ).quantity() );
				}

				if ( ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
				{
					this.total = Math.min( this.total, ConcoctionsDatabase.adventureLimit.quantity() );
				}

				if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
				{
					this.total = Math.min( this.total, ConcoctionsDatabase.stillsLimit.quantity() );
				}

				// The total available for other creations is equal
				// to the total, less the initial.

				this.creatable = this.total - this.initial;

				if ( ConcoctionsDatabase.tripleReagent && ( this.mixingMethod == KoLConstants.COOK_REAGENT || this.mixingMethod == KoLConstants.SUPER_REAGENT ) )
				{
					this.creatable *= 3;
					this.total = this.initial + this.creatable;
				}
			}

			// Now that all the calculations are complete, unmark
			// the ingredients so that later calculations can make
			// the correct calculations.

			this.visibleTotal = this.total;
			this.unmark();
		}

		/**
		 * Utility method which calculates the quantity available for a recipe based on the modifier/multiplier of its
		 * ingredients
		 */

		private int quantity()
		{
			// If there is no multiplier, assume that an infinite
			// number is available.

			if ( this.multiplier == 0 )
			{
				return MallPurchaseRequest.MAX_QUANTITY;
			}

			// The maximum value is equivalent to the total, plus
			// the modifier, divided by the multiplier, if the
			// multiplier exists.

			int quantity = ( this.total + this.modifier ) / this.multiplier;

			// Avoid mutual recursion.

			if ( this.mixingMethod == KoLConstants.ROLLING_PIN || this.mixingMethod == KoLConstants.CLOVER || !ConcoctionsDatabase.isPermittedMethod( this.mixingMethod ) )
			{
				return quantity;
			}

			// The true value is affected by the maximum value for
			// the ingredients.  Therefore, calculate the quantity
			// for all other ingredients to complete the solution
			// of the linear inequality.

			for ( int i = 0; quantity > 0 && i < this.ingredientArray.length; ++i )
			{
				quantity =
					Math.min(
						quantity,
						ConcoctionsDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).quantity() );
			}

			// Adventures are also considered an ingredient; if
			// no adventures are necessary, the multiplier should
			// be zero and the infinite number available will have
			// no effect on the calculation.

			if ( ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
			{
				quantity = Math.min( quantity, ConcoctionsDatabase.adventureLimit.quantity() );
			}

			// Still uses are also considered an ingredient.

			if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
			{
				quantity = Math.min( quantity, ConcoctionsDatabase.stillsLimit.quantity() );
			}

			// The true value is now calculated.  Return this
			// value to the requesting method.

			return quantity;
		}

		/**
		 * Utility method which marks the ingredient for usage with the given added modifier and the given additional
		 * multiplier.
		 */

		private void mark( final int modifier, final int multiplier )
		{
			this.modifier += modifier;
			this.multiplier += multiplier;

			// Avoid mutual recursion

			if ( this.mixingMethod == KoLConstants.ROLLING_PIN || this.mixingMethod == KoLConstants.CLOVER || !ConcoctionsDatabase.isPermittedMethod( this.mixingMethod ) )
			{
				return;
			}

			// Mark all the ingredients, being sure to multiply
			// by the number of that ingredient needed in this
			// concoction.

			int instanceCount;

			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				boolean shouldMark = true;
				instanceCount = this.ingredientArray[ i ].getCount();

				for ( int j = 0; j < i; ++j )
				{
					shouldMark &= this.ingredientArray[ i ].getItemId() != this.ingredientArray[ j ].getItemId();
				}

				if ( shouldMark )
				{
					for ( int j = i + 1; j < this.ingredientArray.length; ++j )
					{
						if ( this.ingredientArray[ i ].getItemId() == this.ingredientArray[ j ].getItemId() )
						{
							instanceCount += this.ingredientArray[ j ].getCount();
						}
					}

					ConcoctionsDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).mark(
						( this.modifier + this.initial ) * instanceCount, this.multiplier * instanceCount );
				}
			}

			// Mark the implicit adventure ingredient, being
			// sure to multiply by the number of adventures
			// which are required for this mixture.

			if ( ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
			{
				ConcoctionsDatabase.adventureLimit.mark(
					( this.modifier + this.initial ) * ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ],
					this.multiplier * ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ] );
			}

			if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
			{
				ConcoctionsDatabase.stillsLimit.mark( ( this.modifier + this.initial ), this.multiplier );
			}
		}

		/**
		 * Utility method which undoes the yielding process, resetting the ingredient and current total values to the
		 * given number.
		 */

		private void unmark()
		{
			if ( this.modifier == 0 && this.multiplier == 0 )
			{
				return;
			}

			this.modifier = 0;
			this.multiplier = 0;

			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				ConcoctionsDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).unmark();
			}

			if ( ConcoctionsDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
			{
				ConcoctionsDatabase.adventureLimit.unmark();
			}

			if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
			{
				ConcoctionsDatabase.stillsLimit.unmark();
			}
		}

		private int getMeatPasteNeeded( final int quantityNeeded )
		{
			// Avoid mutual recursion.

			if ( this.mixingMethod != KoLConstants.COMBINE || KoLCharacter.inMuscleSign() )
			{
				return 0;
			}

			// Count all the meat paste from the different
			// levels in the creation tree.

			int runningTotal = 0;
			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				runningTotal +=
					ConcoctionsDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).getMeatPasteNeeded(
						quantityNeeded );
			}

			return runningTotal + quantityNeeded;
		}

		/**
		 * Returns the string form of this concoction. This is basically the display name for the item created.
		 */

		public String toString()
		{
			return this.name;
		}
	}

	/**
	 * Internal class which functions exactly an array of sorted lists, except it uses "sets" and "gets" like a list.
	 * This could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	private static class SortedListModelArray
	{
		private final ArrayList internalList = new ArrayList();

		public SortedListModel get( final int index )
		{
			if ( index < 0 )
			{
				return null;
			}

			while ( index >= this.internalList.size() )
			{
				this.internalList.add( null );
			}

			return (SortedListModel) this.internalList.get( index );
		}

		public void set( final int index, final SortedListModel value )
		{
			while ( index >= this.internalList.size() )
			{
				this.internalList.add( null );
			}

			this.internalList.set( index, value );
		}
	}

	/**
	 * Internal class which functions exactly an array of concoctions, except it uses "sets" and "gets" like a list.
	 * This could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	private static class ConcoctionArray
	{
		private final ArrayList internalList = new ArrayList();

		public ConcoctionArray()
		{
			int maxItemId = TradeableItemDatabase.maxItemId();
			for ( int i = 0; i <= maxItemId; ++i )
			{
				this.internalList.add( new Concoction(
					TradeableItemDatabase.getItemName( i ) == null ? null : new AdventureResult( i, 0 ),
					KoLConstants.NOCREATE ) );
			}
		}

		public Concoction get( final int index )
		{
			if ( index < 0 )
			{
				return null;
			}

			return (Concoction) this.internalList.get( index );
		}

		public void set( final int index, final Concoction value )
		{
			this.internalList.set( index, value );
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
