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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.SortedListModelArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class ConcoctionDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	public static final SortedListModel creatableList = new SortedListModel();
	public static final LockableListModel usableList = new LockableListModel();

	public static boolean tripleReagent = false;
	public static boolean ignoreRefresh = false;

	public static int queuedFullness = 0;
	public static int queuedInebriety = 0;
	public static int queuedSpleenHit = 0;
	public static int queuedAdventuresUsed = 0;
	public static int queuedStillsUsed = 0;

	public static final Stack queuedChanges = new Stack();
	public static final SortedListModel queuedIngredients = new SortedListModel();

	public static final Concoction stillsLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	public static final Concoction adventureLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );

	public static final SortedListModelArray knownUses = new SortedListModelArray();

	public static final boolean[] PERMIT_METHOD = new boolean[ KoLConstants.METHOD_COUNT ];
	public static final int[] ADVENTURE_USAGE = new int[ KoLConstants.METHOD_COUNT ];

	private static final AdventureResult[] NO_INGREDIENTS = new AdventureResult[ 0 ];

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = FileUtilities.getVersionedReader( "concoctions.txt", KoLConstants.CONCOCTIONS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			ConcoctionDatabase.addConcoction( data );
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

		Concoction current;
		int count = ConcoctionPool.count();

		for ( int i = 0; i < count; ++i )
		{
			current = ConcoctionPool.get( i );
			if ( current != null )
			{
				ConcoctionDatabase.usableList.add( current );
			}
		}

		ConcoctionDatabase.usableList.sort();
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

		int mixingMethod = StringUtilities.parseInt( data[ 1 ] );
		if ( mixingMethod <= 0 || mixingMethod >= KoLConstants.METHOD_COUNT )
		{
			RequestLogger.printLine( "Unknown mixing method (" + mixingMethod + ") for concoction: " + name );
			bogus = true;
		}

		AdventureResult[] ingredients = new AdventureResult[ data.length - 2 ];
		for ( int i = 2; i < data.length; ++i )
		{
			AdventureResult ingredient = ConcoctionDatabase.parseIngredient( data[ i ] );
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

			ConcoctionPool.set( itemId, concoction );
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

			if ( ingredients[ 0 ].getItemId() == ItemPool.GRAVY_BOAT && ingredients[ 1 ].getItemId() == ItemPool.MEAT_FROM_YESTERDAY )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.GRAVY_BOAT && ingredients[ 0 ].getItemId() == ItemPool.MEAT_FROM_YESTERDAY )
			{
				return true;
			}

			// Handle plain pizza, which also allows flat dough
			// to be used instead of wads of dough.

			if ( ingredients[ 0 ].getItemId() == ItemPool.TOMATO && ingredients[ 1 ].getItemId() == ItemPool.DOUGH )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.TOMATO && ingredients[ 0 ].getItemId() == ItemPool.FLAT_DOUGH )
			{
				return true;
			}

			// Handle catsup recipes, which only exist in the
			// item table as ketchup recipes.

			if ( ingredients[ 0 ].getItemId() == ItemPool.CATSUP )
			{
				ingredients[ 0 ] = ItemPool.get( ItemPool.KETCHUP, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.CATSUP )
			{
				ingredients[ 1 ] = ItemPool.get( ItemPool.KETCHUP, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}

			// Handle ice-cold beer recipes, which only uses the
			// recipe for item #41 at this time.

			if ( ingredients[ 0 ].getItemId() == ItemPool.WILLER )
			{
				ingredients[ 0 ] = ItemPool.get( ItemPool.SCHLITZ, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.WILLER )
			{
				ingredients[ 1 ] = ItemPool.get( ItemPool.SCHLITZ, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}

			// Handle cloaca recipes, which only exist in the
			// item table as dyspepsi cola.

			if ( ingredients[ 0 ].getItemId() == ItemPool.CLOACA_COLA )
			{
				ingredients[ 0 ] = ItemPool.get( ItemPool.DYSPEPSI_COLA, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.CLOACA_COLA )
			{
				ingredients[ 1 ] = ItemPool.get( ItemPool.DYSPEPSI_COLA, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
		}

		int[] ingredientTestIds;
		AdventureResult[] ingredientTest;

		int count = ConcoctionPool.count();

		for ( int i = 0; i < count; ++i )
		{
			ingredientTest = ConcoctionPool.get( i ).getIngredients();
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
		SortedListModel uses = ConcoctionDatabase.knownUses.get( itemId );
		return uses == null ? ConcoctionDatabase.EMPTY_LIST : uses;
	}

	public static final SortedListModel getKnownUses( final AdventureResult item )
	{
		return ConcoctionDatabase.getKnownUses( item.getItemId() );
	}

	public static final boolean isPermittedMethod( final int method )
	{
		return ConcoctionDatabase.PERMIT_METHOD[ method ];
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

			return ItemPool.get(
				StringUtilities.parseInt( itemIdString ),
				quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString.replaceAll( "[\\(\\)]", "" ) ) );
		}

		// Otherwise, it's a standard ingredient - use
		// the standard adventure result parsing routine.

		return AdventureResult.parseResult( data );
	}

	public static final SortedListModel getQueue()
	{
		return ConcoctionDatabase.queuedIngredients;
	}

	public static final void push( final Concoction c, final int quantity )
	{
		int adventureChange = ConcoctionDatabase.queuedAdventuresUsed;
		int stillChange = ConcoctionDatabase.queuedStillsUsed;

		ConcoctionDatabase.queuedFullness += c.getFullness() * quantity;
		ConcoctionDatabase.queuedInebriety += c.getInebriety() * quantity;
		ConcoctionDatabase.queuedSpleenHit += c.getSpleenHit() * quantity;

		ArrayList ingredientChange = new ArrayList();
		c.queue( ingredientChange, quantity );

		adventureChange -= ConcoctionDatabase.queuedAdventuresUsed;
		stillChange -= ConcoctionDatabase.queuedStillsUsed;

		ConcoctionDatabase.queuedChanges.push( new Integer( stillChange ) );
		ConcoctionDatabase.queuedChanges.push( new Integer( adventureChange ) );

		ConcoctionDatabase.queuedChanges.push( ingredientChange );
		ConcoctionDatabase.queuedChanges.push( new Integer( quantity ) );
		ConcoctionDatabase.queuedChanges.push( c );
	}

	public static final void pop()
	{
		if ( ConcoctionDatabase.queuedChanges.isEmpty() )
		{
			return;
		}

		Concoction c = (Concoction) ConcoctionDatabase.queuedChanges.pop();
		Integer quantity = (Integer) ConcoctionDatabase.queuedChanges.pop();
		ArrayList ingredientChange = (ArrayList) ConcoctionDatabase.queuedChanges.pop();

		Integer adventureChange = (Integer) ConcoctionDatabase.queuedChanges.pop();
		Integer stillChange = (Integer) ConcoctionDatabase.queuedChanges.pop();

		c.queued -= quantity.intValue();
		for ( int i = 0; i < ingredientChange.size(); ++i )
		{
			AdventureResult.addResultToList(
				ConcoctionDatabase.queuedIngredients, ( (AdventureResult) ingredientChange.get( i ) ).getNegation() );
		}

		int advs = adventureChange.intValue();
		if ( advs != 0 )
		{
			ConcoctionDatabase.queuedAdventuresUsed += advs;
			AdventureResult.addResultToList(
				ConcoctionDatabase.queuedIngredients,
				new AdventureResult( AdventureResult.ADV, advs ) );
		}
		ConcoctionDatabase.queuedStillsUsed += stillChange.intValue();

		ConcoctionDatabase.queuedFullness -= c.getFullness() * quantity.intValue();
		ConcoctionDatabase.queuedInebriety -= c.getInebriety() * quantity.intValue();
		ConcoctionDatabase.queuedSpleenHit -= c.getSpleenHit() * quantity.intValue();
	}

	public static final LockableListModel getUsables()
	{
		return ConcoctionDatabase.usableList;
	}

	public static final SortedListModel getCreatables()
	{
		return ConcoctionDatabase.creatableList;
	}

	public static final void handleQueue( boolean consume )
	{

		ConcoctionDatabase.queuedChanges.clear();
		ConcoctionDatabase.queuedIngredients.clear();
		ConcoctionDatabase.refreshConcoctions();

		ConcoctionDatabase.queuedStillsUsed = 0;
		ConcoctionDatabase.queuedAdventuresUsed = 0;
		ConcoctionDatabase.queuedFullness = 0;
		ConcoctionDatabase.queuedInebriety = 0;
		ConcoctionDatabase.queuedSpleenHit = 0;

		ConcoctionDatabase.ignoreRefresh = true;

		RequestThread.openRequestSequence();
		KoLmafia.updateDisplay( "Processing queued items.." );

		SpecialOutfit.createImplicitCheckpoint();
		Concoction c;

		for ( int i = 0; i < ConcoctionDatabase.usableList.size(); ++i )
		{
			c = (Concoction) ConcoctionDatabase.usableList.get( i );

			if ( c.getQueued() == 0 )
			{
				continue;
			}

			GenericRequest request = null;

			if ( !consume && c.getItem() != null )
			{
				InventoryManager.retrieveItem( c.getItem().getInstance( c.getQueued() ) );
			}
			else if ( c.getPrice() > 0 )
			{
				String name = c.getName();
				if ( HellKitchenRequest.onMenu( name ) )
				{
					request = new HellKitchenRequest( name );
				}
				else if ( ChezSnooteeRequest.onMenu( name ) )
				{
					request = new ChezSnooteeRequest( name );
				}
				else if ( MicroBreweryRequest.onMenu( name ) )
				{
					request = new MicroBreweryRequest( name );
				}
			}
			else
			{
				request = new UseItemRequest( c.getItem().getInstance( c.getQueued() ) );
			}

			if ( request != null )
			{
				if ( request instanceof UseItemRequest )
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

		ConcoctionDatabase.ignoreRefresh = false;
		ConcoctionDatabase.refreshConcoctions();
	}

	public static final int getQueuedFullness()
	{
		return ConcoctionDatabase.queuedFullness;
	}

	public static final int getQueuedInebriety()
	{
		return ConcoctionDatabase.queuedInebriety;
	}

	public static final int getQueuedSpleenHit()
	{
		return ConcoctionDatabase.queuedSpleenHit;
	}

	private static final List getAvailableIngredients()
	{
		boolean includeCloset = !KoLConstants.closet.isEmpty();
		boolean includeStorage = KoLCharacter.canInteract() && !KoLConstants.storage.isEmpty();
		boolean includeStash =
			KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithStash" ) && !ClanManager.getStash().isEmpty();
		boolean includeQueue = !ConcoctionDatabase.queuedIngredients.isEmpty();

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
			for ( int i = 0; i < ConcoctionDatabase.queuedIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedIngredients.get( i );
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

	private static final void setBetterIngredient( final int itemId1, final int itemId2,
		final List availableIngredients )
	{
		Concoction item;
		int available =
			ConcoctionDatabase.getBetterIngredient( itemId1, itemId2, availableIngredients ).getCount(
				availableIngredients );

		item = ConcoctionPool.get( itemId1 );
		item.initial = available;
		item.creatable = 0;
		item.total = available;
		item.visibleTotal = available;

		item = ConcoctionPool.get( itemId2 );
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
		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		Concoction item;
		int count = ConcoctionPool.count();

		for ( int i = 1; i < count; ++i )
		{
			item = ConcoctionPool.get( i );
			if ( item != null )
			{
				item.resetCalculations();
			}
		}

		// Make assessment of availability of mixing methods.
		// This method will also calculate the availability of
		// chefs and bartenders automatically so a second call
		// is not needed.

		boolean includeNPCs = Preferences.getBoolean( "autoSatisfyWithNPCs" );

		// Next, do calculations on all mixing methods which cannot
		// be created at this time.

		for ( int i = 1; i < count; ++i )
		{
			item = ConcoctionPool.get( i );
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

		ConcoctionDatabase.cachePermitted( availableIngredients );

		for ( int i = 1; i < count; ++i )
		{
			item = ConcoctionPool.get( i );
			if ( item == null )
			{
				continue;
			}

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
			{
				continue;
			}

			if ( !ConcoctionDatabase.isPermittedMethod( item.getMixingMethod() ) && item.initial == -1 )
			{
				item.initial = concoction.getCount( availableIngredients );
				item.creatable = 0;
				item.total = item.initial;
				item.visibleTotal = item.total;
			}
		}

		ConcoctionDatabase.calculateBasicItems( availableIngredients );

		// Ice-cold beer and ketchup are special instances -- for the
		// purposes of calculation, we assume that they will use the
		// ingredient which is present in the greatest quantity.

		ConcoctionDatabase.setBetterIngredient(
			ItemPool.DYSPEPSI_COLA, ItemPool.CLOACA_COLA, availableIngredients );
		ConcoctionDatabase.setBetterIngredient(
			ItemPool.SCHLITZ, ItemPool.WILLER, availableIngredients );
		ConcoctionDatabase.setBetterIngredient(
			ItemPool.KETCHUP, ItemPool.CATSUP, availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < count; ++i )
		{
			item = ConcoctionPool.get( i );
			if ( item != null && item.toString() != null && item.creatable > -1 )
			{
				item.calculate( availableIngredients );
			}
		}

		// Now, to update the list of creatables without removing
		// all creatable items.	 We do this by determining the
		// number of items inside of the old list.

		CreateItemRequest instance;
		boolean changeDetected = false;

		for ( int i = 1; i < count; ++i )
		{
			item = ConcoctionPool.get( i );
			if ( item == null )
			{
				continue;
			}

			instance = CreateItemRequest.getInstance( i, false );
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
					ConcoctionDatabase.creatableList.remove( instance );
					item.setPossible( false );
				}
			}
			else if ( !item.wasPossible() )
			{
				ConcoctionDatabase.creatableList.add( instance );
				item.setPossible( true );
			}
			else
			{
				changeDetected = true;
			}
		}

		if ( !ConcoctionDatabase.ignoreRefresh )
		{
			ConcoctionDatabase.creatableList.updateFilter( changeDetected );
			ConcoctionDatabase.usableList.updateFilter( changeDetected );
		}
	}

	public static final int getMeatPasteRequired( final int itemId, final int creationCount )
	{
		Concoction item = ConcoctionPool.get( itemId );
		if ( item == null )
		{
			return 0;
		}

		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();
		item.calculate( availableIngredients );
		return item.getMeatPasteNeeded( creationCount + item.initial );
	}

	private static final void calculateBasicItems( final List availableIngredients )
	{
		// Meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.

		ConcoctionDatabase.setBasicItem(
			availableIngredients, ItemPool.MEAT_PASTE, KoLCharacter.getAvailableMeat() / 10 );
		ConcoctionDatabase.setBasicItem(
			availableIngredients, ItemPool.MEAT_STACK, KoLCharacter.getAvailableMeat() / 100 );
		ConcoctionDatabase.setBasicItem(
			availableIngredients, ItemPool.DENSE_STACK, KoLCharacter.getAvailableMeat() / 1000 );

		AdventureResult item;
		int worthlessItems = Math.min( HermitRequest.getWorthlessItemCount(), KoLCharacter.getAvailableMeat() / 100 );

		for ( int i = 0; i < KoLConstants.hermitItems.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.hermitItems.get( i );
			if ( item.getItemId() != ItemPool.TEN_LEAF_CLOVER )
			{
				ConcoctionDatabase.setBasicItem( availableIngredients, item.getItemId(), worthlessItems );
			}
		}

		int furCount = CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory );
		for ( int i = 0; i < KoLConstants.trapperItems.size(); ++i )
		{
			ConcoctionDatabase.setBasicItem(
				availableIngredients, ( (AdventureResult) KoLConstants.trapperItems.get( i ) ).getItemId(), furCount );
		}
	}

	private static final void setBasicItem( final List availableIngredients, final int itemId, final int creatable )
	{
		Concoction creation = ConcoctionPool.get( itemId );
		if ( creation == null )
		{
			return;
		}

		creation.initial = ItemPool.get( itemId, 1 ).getCount( availableIngredients );
		creation.creatable = creatable;
		creation.total = creation.initial + creatable;
		creation.visibleTotal = creation.total;
	}

	/**
	 * Utility method used to cache the current permissions on item creation.
	 */

	private static final void cachePermitted( final List availableIngredients )
	{
		ConcoctionDatabase.tripleReagent = KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR );
		boolean willBuyServant = KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithMall" );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		ConcoctionDatabase.adventureLimit.initial =
			KoLCharacter.getAdventuresLeft() - ConcoctionDatabase.queuedAdventuresUsed;
		ConcoctionDatabase.adventureLimit.creatable = 0;
		ConcoctionDatabase.adventureLimit.total = KoLCharacter.getAdventuresLeft();
		ConcoctionDatabase.adventureLimit.visibleTotal = ConcoctionDatabase.adventureLimit.total;

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		ConcoctionDatabase.stillsLimit.initial =
			KoLCharacter.getStillsAvailable() - ConcoctionDatabase.queuedStillsUsed;
		ConcoctionDatabase.stillsLimit.creatable = 0;
		ConcoctionDatabase.stillsLimit.total = ConcoctionDatabase.stillsLimit.initial;
		ConcoctionDatabase.stillsLimit.visibleTotal = ConcoctionDatabase.stillsLimit.total;

		ConcoctionDatabase.calculateBasicItems( availableIngredients );

		// It is never possible to create items which are flagged
		// NOCREATE, and it is always possible to create items
		// through meat paste combination.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.NOCREATE ] = false;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.NOCREATE ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COMBINE ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COMBINE ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CLOVER ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.CLOVER ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CATALYST ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.CATALYST ] = 0;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.GNOME_TINKER ] = KoLCharacter.inMoxieSign();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.GNOME_TINKER ] = 0;

		// Smithing of items is possible whenever the person
		// has a hammer.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] =
			InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) || KoLCharacter.getAvailableMeat() >= 1000;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 1;

		// Advanced smithing is available whenever the person can
		// smith and has access to the appropriate skill.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH_WEAPON ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] && KoLCharacter.canSmithWeapons() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH_WEAPON ] = 1;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH_ARMOR ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] && KoLCharacter.canSmithArmor() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH_ARMOR ] = 1;

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.inMuscleSign() )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 0;
		}

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.JEWELRY ] =
			InventoryManager.hasItem( ItemPool.JEWELRY_PLIERS );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.JEWELRY ] = 3;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.EXPENSIVE_JEWELRY ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.JEWELRY ] && KoLCharacter.canCraftExpensiveJewelry();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.EXPENSIVE_JEWELRY ] = 3;

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STARCHART ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.STARCHART ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.PIXEL ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.PIXEL ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MULTI_USE ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MULTI_USE ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SINGLE_USE ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SINGLE_USE ] = 0;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.ROLLING_PIN ] = true;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.ROLLING_PIN ] = 0;

		// Rodoric will make chefstaves for mysticality class
		// characters who can get to the guild.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STAFF ] = KoLCharacter.isMysticalityClass();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.STAFF ] = 0;

		// It's not possible to ask Uncle Crimbo 2005 to make toys

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CRIMBO05 ] = false;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.CRIMBO05 ] = 0;

		// It's not possible to ask Ugh Crimbo 2006 to make toys

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CRIMBO06 ] = false;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.CRIMBO06 ] = 0;

		// It's not possible to ask Uncle Crimbo 2007 to make toys

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CRIMBO07 ] = false;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.CRIMBO07 ] = 0;

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		ConcoctionPool.get( ItemPool.CHEF ).calculate( availableIngredients );
		ConcoctionPool.get( ItemPool.CLOCKWORK_CHEF ).calculate( availableIngredients );
		ConcoctionPool.get( ItemPool.BARTENDER ).calculate( availableIngredients );
		ConcoctionPool.get( ItemPool.CLOCKWORK_BARTENDER ).calculate( availableIngredients );

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
			willBuyServant || KoLCharacter.hasChef() || ConcoctionDatabase.isAvailable(
				ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ] = 0;

		boolean useMall = Preferences.getBoolean( "autoSatisfyWithMall" );
		boolean useStash = Preferences.getBoolean( "autoSatisfyWithStash" );

		if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
				KoLCharacter.canInteract() && ( useMall || useStash );

			if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && !Preferences.getBoolean( "requireBoxServants" ) )
			{
				ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
					InventoryManager.hasItem( ItemPool.BAKE_OVEN ) || KoLCharacter.getAvailableMeat() >= 1000;
				ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ] = 1;
			}
		}

		// Since catalysts aren't purchasable, you can only
		// create items based on them if it's already in your
		// inventory.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CATALYST ] =
			KoLCharacter.isMysticalityClass() || KoLCharacter.getClassType().equals(  KoLCharacter.ACCORDION_THIEF );

		// Cooking of reagents and noodles is possible whenever
		// the person can cook and has the appropriate skill.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_REAGENT ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && KoLCharacter.canSummonReagent();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_REAGENT ] =
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ];

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SUPER_REAGENT ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && KoLCharacter.hasSkill( "The Way of Sauce" );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SUPER_REAGENT ] =
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ];

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_PASTA ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] && KoLCharacter.canSummonNoodles();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_PASTA ] =
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ];

		// Mixing is possible whenever the person has a bartender
		// or they don't need a box servant and have a kit.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
			KoLCharacter.hasBartender() || ConcoctionDatabase.isAvailable(
				ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 0;

		if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
				KoLCharacter.canInteract() && ( useMall || useStash );

			if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && !Preferences.getBoolean( "requireBoxServants" ) )
			{
				ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
					InventoryManager.hasItem( ItemPool.COCKTAIL_KIT ) || KoLCharacter.getAvailableMeat() >= 1000;
				ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 1;
			}
		}

		// Mixing of advanced drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_SPECIAL ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && KoLCharacter.canSummonShore();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_SPECIAL ] =
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ];

		// Mixing of super-special drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_SUPER ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_SUPER ] =
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ];

		// Using Crosby Nash's Still is possible if the person has
		// Superhuman Cocktailcrafting and is a Moxie class character.

		boolean hasStillsAvailable = ConcoctionDatabase.stillsLimit.total > 0;
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STILL_MIXER ] = hasStillsAvailable;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.STILL_MIXER ] = 0;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STILL_BOOZE ] = hasStillsAvailable;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.STILL_BOOZE ] = 0;

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.WOK ] =
			KoLCharacter.canUseWok() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.WOK ] = 1;

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MALUS ] = KoLCharacter.canUseMalus();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MALUS ] = 0;

		// Now, go through all the cached adventure usage values and if
		// the number of adventures left is zero and the request requires
		// adventures, it is not permitted.

		for ( int i = 0; i < KoLConstants.METHOD_COUNT; ++i )
		{
			if ( ConcoctionDatabase.PERMIT_METHOD[ i ] && ConcoctionDatabase.ADVENTURE_USAGE[ i ] > 0 )
			{
				ConcoctionDatabase.PERMIT_METHOD[ i ] =
					ConcoctionDatabase.ADVENTURE_USAGE[ i ] <= KoLCharacter.getAdventuresLeft();
			}
		}
	}

	private static final boolean isAvailable( final int servantId, final int clockworkId )
	{
		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		return Preferences.getBoolean( "autoRepairBoxServants" ) && ConcoctionPool.get( servantId ).total > 0 || ConcoctionPool.get( clockworkId ).total > 0;
	}

	/**
	 * Returns the mixing method for the item with the given Id.
	 */

	public static final int getMixingMethod( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? KoLConstants.NOCREATE : item.getMixingMethod();
	}

	/**
	 * Returns the item Ids of the ingredients for the given item. Note that if there are no ingredients, then
	 * <code>null</code> will be returned instead.
	 */

	public static final AdventureResult[] getIngredients( final int itemId )
	{
		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();

		// Ensure that you're retrieving the same ingredients that
		// were used in the calculations.  Usually this is the case,
		// but ice-cold beer and ketchup are tricky cases.

		AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( ingredients[ i ].getItemId() == ItemPool.SCHLITZ || ingredients[ i ].getItemId() == ItemPool.WILLER )
			{
				ingredients[ i ] =
					ConcoctionDatabase.getBetterIngredient(
						ItemPool.SCHLITZ, ItemPool.WILLER, availableIngredients );
			}
			else if ( ingredients[ i ].getItemId() == ItemPool.KETCHUP || ingredients[ i ].getItemId() == ItemPool.CATSUP )
			{
				ingredients[ i ] =
					ConcoctionDatabase.getBetterIngredient(
						ItemPool.KETCHUP, ItemPool.CATSUP, availableIngredients );
			}
		}

		return ingredients;
	}

	public static final int getYield( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? 1 : item.getYield();
	}

	public static final AdventureResult[] getStandardIngredients( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? ConcoctionDatabase.NO_INGREDIENTS : item.getIngredients();
	}

	private static final AdventureResult getBetterIngredient( final int itemId1,
		final int itemId2, final List availableIngredients )
	{
		AdventureResult ingredient1 = ItemPool.get( itemId1, 1 );
		AdventureResult ingredient2 = ItemPool.get( itemId2, 1 );
		return ingredient1.getCount( availableIngredients ) > ingredient2.getCount( availableIngredients ) ? ingredient1 : ingredient2;
	}
}
