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
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class ConcoctionDatabase
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

		for ( int i = 0; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			if ( ConcoctionDatabase.concoctions.get( i ) != null )
			{
				ConcoctionDatabase.usableList.add( ConcoctionDatabase.concoctions.get( i ) );
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
			ConcoctionDatabase.concoctions.set( itemId, concoction );
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

			if ( ingredients[ 0 ].getItemId() == ConcoctionDatabase.TOMATO && ingredients[ 1 ].getItemId() == ConcoctionDatabase.FLAT_DOUGH )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionDatabase.TOMATO && ingredients[ 0 ].getItemId() == ConcoctionDatabase.FLAT_DOUGH )
			{
				return true;
			}

			// Handle catsup recipes, which only exist in the
			// item table as ketchup recipes.

			if ( ingredients[ 0 ].getItemId() == ConcoctionDatabase.CATSUP.getItemId() )
			{
				ingredients[ 0 ] = ConcoctionDatabase.KETCHUP;
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionDatabase.CATSUP.getItemId() )
			{
				ingredients[ 1 ] = ConcoctionDatabase.KETCHUP;
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}

			// Handle ice-cold beer recipes, which only uses the
			// recipe for item #41 at this time.

			if ( ingredients[ 0 ].getItemId() == ConcoctionDatabase.WILLER.getItemId() )
			{
				ingredients[ 0 ] = ConcoctionDatabase.SCHLITZ;
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionDatabase.WILLER.getItemId() )
			{
				ingredients[ 1 ] = ConcoctionDatabase.SCHLITZ;
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}

			// Handle cloaca recipes, which only exist in the
			// item table as dyspepsi cola.

			if ( ingredients[ 0 ].getItemId() == ConcoctionDatabase.CLOACA.getItemId() )
			{
				ingredients[ 0 ] = ConcoctionDatabase.DYSPEPSI;
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ConcoctionDatabase.CLOACA.getItemId() )
			{
				ingredients[ 1 ] = ConcoctionDatabase.DYSPEPSI;
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
		}

		int[] ingredientTestIds;
		AdventureResult[] ingredientTest;

		for ( int i = 0; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			ingredientTest = ConcoctionDatabase.concoctions.get( i ).getIngredients();
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

			return new AdventureResult(
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

		ConcoctionDatabase.ignoreRefresh = true;

		RequestThread.openRequestSequence();
		KoLmafia.updateDisplay( "Processing queued items..." );

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

	private static final void setBetterIngredient( final AdventureResult item1, final AdventureResult item2,
		final List availableIngredients )
	{
		Concoction item;
		int available =
			ConcoctionDatabase.getBetterIngredient( item1, item2, availableIngredients ).getCount(
				availableIngredients );

		item = ConcoctionDatabase.concoctions.get( item1.getItemId() );
		item.initial = available;
		item.creatable = 0;
		item.total = available;
		item.visibleTotal = available;

		item = ConcoctionDatabase.concoctions.get( item2.getItemId() );
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
		for ( int i = 1; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionDatabase.concoctions.get( i );
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

		for ( int i = 1; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionDatabase.concoctions.get( i );
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

		for ( int i = 1; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionDatabase.concoctions.get( i );
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
			ConcoctionDatabase.DYSPEPSI, ConcoctionDatabase.CLOACA, availableIngredients );
		ConcoctionDatabase.setBetterIngredient(
			ConcoctionDatabase.SCHLITZ, ConcoctionDatabase.WILLER, availableIngredients );
		ConcoctionDatabase.setBetterIngredient(
			ConcoctionDatabase.KETCHUP, ConcoctionDatabase.CATSUP, availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionDatabase.concoctions.get( i );
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

		for ( int i = 1; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			item = ConcoctionDatabase.concoctions.get( i );
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
		Concoction item = ConcoctionDatabase.concoctions.get( itemId );
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
			availableIngredients, ConcoctionDatabase.PASTE, KoLCharacter.getAvailableMeat() / 10 );
		ConcoctionDatabase.setBasicItem(
			availableIngredients, ConcoctionDatabase.STACK, KoLCharacter.getAvailableMeat() / 100 );
		ConcoctionDatabase.setBasicItem(
			availableIngredients, ConcoctionDatabase.DENSE, KoLCharacter.getAvailableMeat() / 1000 );

		AdventureResult item;
		int worthlessItems = Math.min( HermitRequest.getWorthlessItemCount(), KoLCharacter.getAvailableMeat() / 100 );

		for ( int i = 0; i < KoLConstants.hermitItems.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.hermitItems.get( i );
			if ( item.getItemId() != ItemPool.TEN_LEAF_CLOVER )
			{
				ConcoctionDatabase.setBasicItem( availableIngredients, item, worthlessItems );
			}
		}

		int furCount = CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory );
		for ( int i = 0; i < KoLConstants.trapperItems.size(); ++i )
		{
			ConcoctionDatabase.setBasicItem(
				availableIngredients, (AdventureResult) KoLConstants.trapperItems.get( i ), furCount );
		}
	}

	private static final void setBasicItem( final List availableIngredients, final AdventureResult item,
		final int creatable )
	{
		Concoction creation = ConcoctionDatabase.concoctions.get( item.getItemId() );
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
			KoLConstants.inventory.contains( ConcoctionDatabase.HAMMER ) || KoLCharacter.getAvailableMeat() >= 1000;
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
			KoLConstants.inventory.contains( ConcoctionDatabase.PLIERS );
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

		ConcoctionDatabase.concoctions.get( ConcoctionDatabase.CHEF ).calculate( availableIngredients );
		ConcoctionDatabase.concoctions.get( ConcoctionDatabase.CLOCKWORK_CHEF ).calculate( availableIngredients );
		ConcoctionDatabase.concoctions.get( ConcoctionDatabase.BARTENDER ).calculate( availableIngredients );
		ConcoctionDatabase.concoctions.get( ConcoctionDatabase.CLOCKWORK_BARTENDER ).calculate( availableIngredients );

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
			willBuyServant || KoLCharacter.hasChef() || ConcoctionDatabase.isAvailable(
				ConcoctionDatabase.CHEF, ConcoctionDatabase.CLOCKWORK_CHEF );
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
					KoLConstants.inventory.contains( ConcoctionDatabase.OVEN ) || KoLCharacter.getAvailableMeat() >= 1000;
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
				ConcoctionDatabase.BARTENDER, ConcoctionDatabase.CLOCKWORK_BARTENDER );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 0;

		if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
				KoLCharacter.canInteract() && ( useMall || useStash );

			if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] && !Preferences.getBoolean( "requireBoxServants" ) )
			{
				ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
					KoLConstants.inventory.contains( ConcoctionDatabase.KIT ) || KoLCharacter.getAvailableMeat() >= 1000;
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

		return Preferences.getBoolean( "autoRepairBoxServants" ) && ConcoctionDatabase.concoctions.get( servantId ).total > 0 || ConcoctionDatabase.concoctions.get( clockworkId ).total > 0;
	}

	/**
	 * Returns the mixing method for the item with the given Id.
	 */

	public static final int getMixingMethod( final int itemId )
	{
		Concoction item = ConcoctionDatabase.concoctions.get( itemId );
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
			if ( ingredients[ i ].getItemId() == ConcoctionDatabase.SCHLITZ.getItemId() || ingredients[ i ].getItemId() == ConcoctionDatabase.WILLER.getItemId() )
			{
				ingredients[ i ] =
					ConcoctionDatabase.getBetterIngredient(
						ConcoctionDatabase.SCHLITZ, ConcoctionDatabase.WILLER, availableIngredients );
			}
			else if ( ingredients[ i ].getItemId() == ConcoctionDatabase.KETCHUP.getItemId() || ingredients[ i ].getItemId() == ConcoctionDatabase.CATSUP.getItemId() )
			{
				ingredients[ i ] =
					ConcoctionDatabase.getBetterIngredient(
						ConcoctionDatabase.KETCHUP, ConcoctionDatabase.CATSUP, availableIngredients );
			}
		}

		return ingredients;
	}

	public static final int getYield( final int itemId )
	{
		Concoction item = ConcoctionDatabase.concoctions.get( itemId );
		return item == null ? 1 : item.getYield();
	}

	public static final AdventureResult[] getStandardIngredients( final int itemId )
	{
		Concoction item = ConcoctionDatabase.concoctions.get( itemId );
		return item == null ? ConcoctionDatabase.NO_INGREDIENTS : item.getIngredients();
	}

	/**
	 * Find a concoction made in a particular way that includes the specified ingredient
	 */

	public static final int findConcoction( final int mixingMethod, final int itemId )
	{
		AdventureResult ingredient = new AdventureResult( itemId, 1 );
		for ( int i = 0; i < ConcoctionDatabase.concoctions.size(); ++i )
		{
			Concoction concoction = ConcoctionDatabase.concoctions.get( i );
			if ( concoction == null || concoction.getMixingMethod() != mixingMethod )
			{
				continue;
			}

			AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients( i );
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
		private final int yield;
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
			this.yield = 1;

			this.mixingMethod = KoLConstants.NOCREATE;
			this.wasPossible = true;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[ 0 ];

			this.fullness = ItemDatabase.getFullness( name );
			this.inebriety = ItemDatabase.getInebriety( name );

			int consumeType = this.fullness == 0 ? KoLConstants.CONSUME_DRINK : KoLConstants.CONSUME_EAT;

			switch ( consumeType )
			{
			case KoLConstants.CONSUME_EAT:
				this.sortOrder = this.fullness > 0 ? 1 : 3;
				break;
			case KoLConstants.CONSUME_DRINK:
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
				this.yield = Math.max( concoction.getCount(), 1 );
				this.name = concoction.getName();
			}
			else
			{
				this.yield = 1;
				this.name = "unknown";
			}

			this.mixingMethod = mixingMethod;
			this.wasPossible = false;

			this.fullness = ItemDatabase.getFullness( this.name );
			this.inebriety = ItemDatabase.getInebriety( this.name );

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[ 0 ];

			int consumeType =
				concoction == null ? 0 : ItemDatabase.getConsumptionType( concoction.getItemId() );

			switch ( consumeType )
			{
			case KoLConstants.CONSUME_EAT:
				this.sortOrder = this.fullness > 0 ? 1 : 3;
				break;
			case KoLConstants.CONSUME_DRINK:
				this.sortOrder = this.inebriety > 0 ? 2 : 3;
				break;
			default:
				this.sortOrder = 3;
				break;
			}

			this.price = -1;
		}

		public int getYield()
		{
			if ( ConcoctionDatabase.tripleReagent && this.isReagentPotion() )
			{
				return 3 * this.yield;
			}

			return this.yield;
		}

		public boolean isReagentPotion()
		{
			if ( this.mixingMethod != KoLConstants.COOK_REAGENT && this.mixingMethod != KoLConstants.SUPER_REAGENT )
			{
				return false;
			}

			int type = ItemDatabase.getConsumptionType( this.getItemId() );
			return type == KoLConstants.CONSUME_USE || type == KoLConstants.CONSUME_MULTIPLE;
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

			if ( !Preferences.getBoolean( "showGainsPerUnit" ) )
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

			float adventures1 = StringUtilities.parseFloat( ItemDatabase.getAdventureRange( this.name ) );
			float adventures2 =
				StringUtilities.parseFloat( ItemDatabase.getAdventureRange( ( (Concoction) o ).name ) );

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
				AdventureResult.addResultToList( ConcoctionDatabase.queuedIngredients, ingredient );
			}

			int advs = ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] * overAmount;
			if ( advs != 0 )
			{
				ConcoctionDatabase.queuedAdventuresUsed += advs;
				AdventureResult.addResultToList( ConcoctionDatabase.queuedIngredients, new AdventureResult( AdventureResult.ADV, advs ) );
			}

			if ( this.mixingMethod == KoLConstants.STILL_BOOZE || this.mixingMethod == KoLConstants.STILL_MIXER )
			{
				ConcoctionDatabase.queuedStillsUsed += overAmount;
			}

			if ( adjust )
			{
				this.queued += amount;
			}

			// Recipes that yield multiple units require smaller
			// quantities of ingredients.

			int mult = this.getYield();
			int icount = ( overAmount + ( mult - 1 ) ) / mult;
			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				AdventureResult ingredient = this.ingredientArray[ i ];
				Concoction c = ConcoctionDatabase.concoctions.get( ingredient.getItemId() );
				c.queue( ingredientChange, icount, false );
			}

			// Recipes that yield multiple units might result in
			// extra product which can be used for other recipes.

			int excess = mult * icount - overAmount;
			if ( excess > 0	 )
			{
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
				if ( ItemDatabase.getFullness( this.name ) > 0 )
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
			SortedListModel uses = ConcoctionDatabase.knownUses.get( ingredient.getItemId() );
			if ( uses == null )
			{
				uses = new SortedListModel();
				ConcoctionDatabase.knownUses.set( ingredient.getItemId(), uses );
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

			if ( !ConcoctionDatabase.isPermittedMethod( this.mixingMethod ) )
			{
				this.visibleTotal = this.total;
				return;
			}

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				AdventureResult ingredient = this.ingredientArray[ i ];
				Concoction c = ConcoctionDatabase.concoctions.get( ingredient.getItemId() );
				c.calculate( availableIngredients );
			}

			this.mark( 0, 1 );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			if ( this.mixingMethod == KoLConstants.ROLLING_PIN || this.mixingMethod == KoLConstants.CLOVER )
			{
				// If there's only one ingredient, then the
				// quantity depends entirely on it.
				AdventureResult ingredient = this.ingredientArray[ 0 ];
				Concoction c = ConcoctionDatabase.concoctions.get( ingredient.getItemId() );

				this.creatable = c.initial;
				this.total = this.initial + this.creatable;
			}
			else
			{
				this.total = MallPurchaseRequest.MAX_QUANTITY;

				for ( int i = 0; i < this.ingredientArray.length; ++i )
				{
					AdventureResult ingredient = this.ingredientArray[ i ];
					Concoction c = ConcoctionDatabase.concoctions.get( ingredient.getItemId() );
					int available = c.quantity();
					this.total = Math.min( this.total, available );
				}

				if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
				{
					Concoction c = ConcoctionDatabase.adventureLimit;
					int available = c.quantity();
					this.total = Math.min( this.total, available );
				}

				if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
				{
					Concoction c = ConcoctionDatabase.stillsLimit;
					int available = c.quantity();
					this.total = Math.min( this.total, available );
				}

				// The total available for other creations is
				// equal to the total, less the initial.

				this.creatable = ( this.total - this.initial ) * this.getYield();
				this.total = this.initial + this.creatable;
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

			if ( this.mixingMethod == KoLConstants.ROLLING_PIN || this.mixingMethod == KoLConstants.CLOVER || !ConcoctionDatabase.isPermittedMethod( this.mixingMethod ) )
			{
				return quantity;
			}

			// The true value is affected by the maximum value for
			// the ingredients.  Therefore, calculate the quantity
			// for all other ingredients to complete the solution
			// of the linear inequality.

			int mult = this.getYield();
			for ( int i = 0; quantity > 0 && i < this.ingredientArray.length; ++i )
			{
				AdventureResult ingredient = this.ingredientArray[ i ];
				Concoction c = ConcoctionDatabase.concoctions.get( ingredient.getItemId() );
				int available = c.quantity() * mult;
				quantity = Math.min( quantity, available );
			}

			// Adventures are also considered an ingredient; if
			// no adventures are necessary, the multiplier should
			// be zero and the infinite number available will have
			// no effect on the calculation.

			if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
			{
				Concoction c = ConcoctionDatabase.adventureLimit;
				int available = c.quantity() * mult;
				quantity = Math.min( quantity, available );
			}

			// Still uses are also considered an ingredient.

			if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
			{
				Concoction c = ConcoctionDatabase.stillsLimit;
				int available = c.quantity() * mult;
				quantity = Math.min( quantity, available );
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

			if ( this.mixingMethod == KoLConstants.ROLLING_PIN || this.mixingMethod == KoLConstants.CLOVER || !ConcoctionDatabase.isPermittedMethod( this.mixingMethod ) )
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

					ConcoctionDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).mark(
						( this.modifier + this.initial ) * instanceCount, this.multiplier * instanceCount );
				}
			}

			// Mark the implicit adventure ingredient, being
			// sure to multiply by the number of adventures
			// which are required for this mixture.

			if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
			{
				ConcoctionDatabase.adventureLimit.mark(
					( this.modifier + this.initial ) * ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ],
					this.multiplier * ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] );
			}

			if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
			{
				ConcoctionDatabase.stillsLimit.mark( ( this.modifier + this.initial ), this.multiplier );
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
				ConcoctionDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() ).unmark();
			}

			if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
			{
				ConcoctionDatabase.adventureLimit.unmark();
			}

			if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
			{
				ConcoctionDatabase.stillsLimit.unmark();
			}
		}

		private int getMeatPasteNeeded( final int quantityNeeded )
		{
			// Avoid mutual recursion.

			if ( this.mixingMethod != KoLConstants.COMBINE || KoLCharacter.inMuscleSign() || quantityNeeded <= this.initial )
			{
				return 0;
			}

			// Count all the meat paste from the different
			// levels in the creation tree.

			int runningTotal = 0;
			for ( int i = 0; i < this.ingredientArray.length; ++i )
			{
				Concoction ingredient = ConcoctionDatabase.concoctions.get( this.ingredientArray[ i ].getItemId() );

				runningTotal += ingredient.getMeatPasteNeeded( quantityNeeded - ingredient.initial );
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
			int maxItemId = ItemDatabase.maxItemId();
			for ( int i = 0; i <= maxItemId; ++i )
			{
				this.internalList.add( new Concoction(
					ItemDatabase.getItemName( i ) == null ? null : new AdventureResult( i, 1 ),
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
