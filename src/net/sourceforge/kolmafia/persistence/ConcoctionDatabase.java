/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.HashMap;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.CrimboCafeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.SortedListModelArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcoctionDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	public static final SortedListModel creatableList = new SortedListModel();
	public static final LockableListModel usableList = new LockableListModel();

	public static boolean ignoreRefresh = false;
	public static boolean deferRefresh = false;
	public static boolean refreshDeferred = false;

	public static int queuedAdventuresUsed = 0;
	public static int queuedStillsUsed = 0;
	public static int queuedPullsUsed = 0;

	private static int queuedFullness = 0;
	private static final Stack queuedFoodChanges = new Stack();
	private static final SortedListModel queuedFoodIngredients = new SortedListModel();

	private static int queuedInebriety = 0;
	private static final Stack queuedBoozeChanges = new Stack();
	private static final SortedListModel queuedBoozeIngredients = new SortedListModel();

	private static int queuedSpleenHit = 0;
	private static final Stack queuedSpleenChanges = new Stack();
	private static final SortedListModel queuedSpleenIngredients = new SortedListModel();

	public static final Concoction stillsLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	public static final Concoction adventureLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	public static final Concoction meatLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );

	public static final SortedListModelArray knownUses = new SortedListModelArray();

	public static final boolean[] PERMIT_METHOD = new boolean[ KoLConstants.METHOD_COUNT ];
	public static final int[] ADVENTURE_USAGE = new int[ KoLConstants.METHOD_COUNT ];
	public static final int[] CREATION_COST = new int[ KoLConstants.METHOD_COUNT ];
	public static int creationFlags;

	private static final AdventureResult[] NO_INGREDIENTS = new AdventureResult[ 0 ];

	private static final HashMap mixingMethods = new HashMap();

	static
	{
		// Basic creation types:
		
		// Items anybody can create using meat paste
		ConcoctionDatabase.mixingMethods.put( "COMBINE", new Integer( KoLConstants.COMBINE ));
		// Items anybody can create with an E-Z Cook Oven
		ConcoctionDatabase.mixingMethods.put( "COOK", new Integer( KoLConstants.COOK ));
		// Items anybody can create with a cocktailcrafting kit
		ConcoctionDatabase.mixingMethods.put( "MIX", new Integer( KoLConstants.MIX ));
		// Items anybody can create with a tenderizing hammer or via Innabox
		ConcoctionDatabase.mixingMethods.put( "SMITH", new Integer( KoLConstants.SMITH ));
		// Items that can only be created with a tenderizing hammer, not via Innabox
		ConcoctionDatabase.mixingMethods.put( "SSMITH", new Integer( KoLConstants.SSMITH ));
		// Items requiring access to Nash Crosby's Still -- booze
		ConcoctionDatabase.mixingMethods.put( "BSTILL", new Integer( KoLConstants.STILL_BOOZE ));
		// Items requiring Superhuman Cocktailcrafting -- mixer
		ConcoctionDatabase.mixingMethods.put( "MSTILL", new Integer( KoLConstants.STILL_MIXER ));
		// Items requiring access to the Wok of Ages
		ConcoctionDatabase.mixingMethods.put( "WOK", new Integer( KoLConstants.WOK ));
		// Items requiring access to the Malus of Forethought
		ConcoctionDatabase.mixingMethods.put( "MALUS", new Integer( KoLConstants.MALUS ));
		// Items anybody can create with jewelry-making pliers
		ConcoctionDatabase.mixingMethods.put( "JEWEL", new Integer( KoLConstants.JEWELRY ));
		// Items anybody can create with starcharts, stars, and lines
		ConcoctionDatabase.mixingMethods.put( "STAR", new Integer( KoLConstants.STARCHART ));
		// Items anybody can create by folding sugar sheets
		ConcoctionDatabase.mixingMethods.put( "SUGAR", new Integer( KoLConstants.SUGAR_FOLDING ));
		// Items anybody can create with pixels
		ConcoctionDatabase.mixingMethods.put( "PIXEL", new Integer( KoLConstants.PIXEL ));
		// Items created with a rolling pin or and an unrolling pin
		ConcoctionDatabase.mixingMethods.put( "ROLL", new Integer( KoLConstants.ROLLING_PIN ));
		// Items requiring access to the Gnome supertinker
		ConcoctionDatabase.mixingMethods.put( "TINKER", new Integer( KoLConstants.GNOME_TINKER ));
		// Items requiring access to Roderick the Staffmaker
		ConcoctionDatabase.mixingMethods.put( "STAFF", new Integer( KoLConstants.STAFF ));
		// Items anybody can create with a sushi-rolling mat
		ConcoctionDatabase.mixingMethods.put( "SUSHI", new Integer( KoLConstants.SUSHI ));
		// Items created by single (or multi) using a single item.
		// Extra ingredients might also be consumed.
		// Multi-using multiple of the item creates multiple results.
		ConcoctionDatabase.mixingMethods.put( "SUSE", new Integer( KoLConstants.SINGLE_USE ));
		// Items created by multi-using specific # of a single item.
		// Extra ingredients might also be consumed.
		// You must create multiple result items one at a time.
		ConcoctionDatabase.mixingMethods.put( "MUSE", new Integer( KoLConstants.MULTI_USE ));
		// Items formerly creatable in Crimbo Town during Crimbo 2005
		ConcoctionDatabase.mixingMethods.put( "CRIMBO05", new Integer( KoLConstants.CRIMBO05 ));
		// Items formerly creatable in Crimbo Town during Crimbo 2006
		ConcoctionDatabase.mixingMethods.put( "CRIMBO06", new Integer( KoLConstants.CRIMBO06 ));
		// Items formerly creatable in Crimbo Town during Crimbo 2007
		ConcoctionDatabase.mixingMethods.put( "CRIMBO07", new Integer( KoLConstants.CRIMBO07 ));
		
		// Creation flags
		
		// Character gender (for kilt vs. skirt)
		ConcoctionDatabase.mixingMethods.put( "MALE", new Integer( KoLConstants.CR_MALE ));
		// Character gender (for kilt vs. skirt)
		ConcoctionDatabase.mixingMethods.put( "FEMALE", new Integer( KoLConstants.CR_FEMALE ));
		// Holiday-only
		ConcoctionDatabase.mixingMethods.put( "SSPD", new Integer( KoLConstants.CR_SSPD ));
		// Requires tenderizing hammer (implied for SMITH & SSMITH)
		ConcoctionDatabase.mixingMethods.put( "HAMMER", new Integer( KoLConstants.CR_HAMMER ));
		// Requires depleted Grimacite hammer
		ConcoctionDatabase.mixingMethods.put( "GRIMACITE", new Integer( KoLConstants.CR_GRIMACITE ));
		// Requires Torso Awaregness
		ConcoctionDatabase.mixingMethods.put( "TORSO", new Integer( KoLConstants.CR_TORSO ));
		// Requires Super-Advanced Meatsmithing
		ConcoctionDatabase.mixingMethods.put( "WEAPON", new Integer( KoLConstants.CR_WEAPON ));
		// Requires Armorcraftiness
		ConcoctionDatabase.mixingMethods.put( "ARMOR", new Integer( KoLConstants.CR_ARMOR ));
		// Requires Really Expensive Jewerlycrafting
		ConcoctionDatabase.mixingMethods.put( "EXPENSIVE", new Integer( KoLConstants.CR_EXPENSIVE ));
		// Requires Advanced Saucecrafting
		ConcoctionDatabase.mixingMethods.put( "REAGENT", new Integer( KoLConstants.CR_REAGENT ));
		// Requires The Way of Sauce
		ConcoctionDatabase.mixingMethods.put( "WAY", new Integer( KoLConstants.CR_WAY ));
		// Requires Deep Saucery
		ConcoctionDatabase.mixingMethods.put( "DEEP", new Integer( KoLConstants.CR_DEEP ));
		// Requires Pastamastery
		ConcoctionDatabase.mixingMethods.put( "PASTAMASTERY", new Integer( KoLConstants.CR_PASTA ));
		// Requires Tempuramancy
		ConcoctionDatabase.mixingMethods.put( "TEMPURAMANCY", new Integer( KoLConstants.CR_TEMPURA ));
		// Requires Advanced Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "AC", new Integer( KoLConstants.CR_AC ));
		// Requires Superhuman Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SHC", new Integer( KoLConstants.CR_SHC ));
		// Requires Salacious Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SALACIOUS", new Integer( KoLConstants.CR_SALACIOUS ));
		// Saucerors make 3 of this item at a time
		ConcoctionDatabase.mixingMethods.put( "SX3", new Integer( KoLConstants.CF_SX3 ));
		// Recipe unexpectedly does not appear in Discoveries, even though
		// it uses a discoverable crafting type
		ConcoctionDatabase.mixingMethods.put( "NODISCOVERY", new Integer( KoLConstants.CF_NODISCOVERY ));
		// Recipe should never be used automatically
		ConcoctionDatabase.mixingMethods.put( "MANUAL", new Integer( KoLConstants.CF_MANUAL ));
		
		// Combinations of creation type & flags, for convenience
		
		// Items requiring Pastamastery
		ConcoctionDatabase.mixingMethods.put( "PASTA", new Integer( KoLConstants.COOK | KoLConstants.CR_PASTA ));
		// Items requiring Tempuramancy
		ConcoctionDatabase.mixingMethods.put( "TEMPURA", new Integer( KoLConstants.COOK | KoLConstants.CR_TEMPURA ));
		// Items requiring Super-Advanced Meatsmithing
		ConcoctionDatabase.mixingMethods.put( "WSMITH", new Integer( KoLConstants.SSMITH | KoLConstants.CR_WEAPON ));
		// Items requiring Armorcraftiness
		ConcoctionDatabase.mixingMethods.put( "ASMITH", new Integer( KoLConstants.SSMITH | KoLConstants.CR_ARMOR ));
		// Items requiring Advanced Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "ACOCK", new Integer( KoLConstants.MIX | KoLConstants.CR_AC ));
		// Items requiring Superhuman Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SCOCK", new Integer( KoLConstants.MIX | KoLConstants.CR_SHC ));
		// Items requiring Salacious Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SACOCK", new Integer( KoLConstants.MIX | KoLConstants.CR_SALACIOUS ));
		// Items requiring pliers and Really Expensive Jewelrycrafting
		ConcoctionDatabase.mixingMethods.put( "EJEWEL", new Integer( KoLConstants.JEWELRY | KoLConstants.CR_EXPENSIVE ));
		// Items requiring Advanced Saucecrafting
		ConcoctionDatabase.mixingMethods.put( "SAUCE", new Integer( KoLConstants.COOK | KoLConstants.CR_REAGENT ));
		// Items requiring The Way of Sauce
		ConcoctionDatabase.mixingMethods.put( "SSAUCE", new Integer( KoLConstants.COOK | KoLConstants.CR_WAY ));
		// Items requiring Deep Saucery
		ConcoctionDatabase.mixingMethods.put( "DSAUCE", new Integer( KoLConstants.COOK | KoLConstants.CR_DEEP ));
	}

	private static final HashMap chefStaff = new HashMap();
	private static final HashMap singleUse = new HashMap();
	private static final HashMap multiUse = new HashMap();

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

		// Add all concoctions to usable list

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction current = (Concoction) it.next();
			ConcoctionDatabase.usableList.add( current );
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
		String[] mixes = data[ 1 ].split( "\\s*,\\s*" );
		int mixingMethod = 0;
		for ( int i = 0; i < mixes.length; ++i )
		{
			String mix = mixes[ i ];
			Integer val = (Integer) ConcoctionDatabase.mixingMethods.get( mix );
			if ( val == null )
			{
				RequestLogger.printLine( "Unknown mixing method or flag (" + mix + ") for concoction: " + name );
				ConcoctionDatabase.mixingMethods.put( mix, new Integer( 0 ) );
				// This is not necessarily a fatal error; it could just be a
				// newly-defined informational flag.
				continue;
			}
			int v = val.intValue();
			if ( (v & KoLConstants.CT_MASK) != 0 &&
				(mixingMethod & KoLConstants.CT_MASK) != 0 )
			{
				RequestLogger.printLine( "Multiple mixing methods for concoction: " + name );
				bogus = true;
			}
			mixingMethod |= v;
		}

		if ( (mixingMethod & KoLConstants.CT_MASK) == KoLConstants.NOCREATE )
		{
			RequestLogger.printLine( "No mixing method specified for concoction: " + name );
			bogus = true;
		}

		AdventureResult item = AdventureResult.parseItem( name, true );
		int itemId = item.getItemId();

		if ( itemId < 0 && !ConcoctionDatabase.pseudoItemMixingMethod( mixingMethod ) )
		{
			RequestLogger.printLine( "Unknown concoction: " + name );
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

			Concoction existing = ConcoctionPool.get( item );
			if ( (concoction.getMixingMethod() & KoLConstants.CF_MANUAL) != 0 ||
				(existing != null && existing.getMixingMethod() != 0) )
			{	// Until multiple recipes are supported...
				return;
			}
			ConcoctionPool.set( concoction );

			switch ( mixingMethod & KoLConstants.CT_MASK )
			{
			case KoLConstants.STAFF:
				ConcoctionDatabase.chefStaff.put( ingredients[ 0 ].getName(), concoction );
				break;
			case KoLConstants.SINGLE_USE:
				ConcoctionDatabase.singleUse.put( ingredients[ 0 ].getName(), concoction );
				break;
			case KoLConstants.MULTI_USE:
				ConcoctionDatabase.multiUse.put( ingredients[ 0 ].getName(), concoction );
				break;
			}
		}
	}

	public static Concoction chefStaffCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.chefStaff.get( name );
	}

	public static Concoction singleUseCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.singleUse.get( name );
	}

	public static Concoction multiUseCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.multiUse.get( name );
	}

	private static boolean pseudoItemMixingMethod( final int mixingMethod )
	{
		return (mixingMethod & KoLConstants.CT_MASK) == KoLConstants.SUSHI;
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

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction current = (Concoction) it.next();
			if ( current.hasIngredients( ingredients ) )
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
		return ConcoctionDatabase.PERMIT_METHOD[ method & KoLConstants.CT_MASK ] &&
			(method & KoLConstants.CR_MASK & ~ConcoctionDatabase.creationFlags) == 0;
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

	public static final SortedListModel getQueuedIngredients( boolean food, boolean booze, boolean spleen )
	{
		return food ? ConcoctionDatabase.queuedFoodIngredients :
			booze ? ConcoctionDatabase.queuedBoozeIngredients :
			ConcoctionDatabase.queuedSpleenIngredients;
	}

	public static final void push( final Concoction c, final int quantity )
	{
		Stack queuedChanges;
		LockableListModel queuedIngredients;
		int id = c.getItemId();
		int consumpt = ItemDatabase.getConsumptionType( id );

		if ( c.getFullness() > 0 || consumpt == KoLConstants.CONSUME_FOOD_HELPER ||
			id == ItemPool.MUNCHIES_PILL )
		{
			queuedChanges = ConcoctionDatabase.queuedFoodChanges;
			queuedIngredients = ConcoctionDatabase.queuedFoodIngredients;
			ConcoctionDatabase.queuedFullness += c.getFullness() * quantity;
		}
		else if ( c.getInebriety() > 0 || consumpt == KoLConstants.CONSUME_DRINK_HELPER )
		{
			queuedChanges = ConcoctionDatabase.queuedBoozeChanges;
			queuedIngredients = ConcoctionDatabase.queuedBoozeIngredients;
			ConcoctionDatabase.queuedInebriety += c.getInebriety() * quantity;
		}
		else
		{
			queuedChanges = ConcoctionDatabase.queuedSpleenChanges;
			queuedIngredients = ConcoctionDatabase.queuedSpleenIngredients;
			ConcoctionDatabase.queuedSpleenHit += c.getSpleenHit() * quantity;
		}

		int adventureChange = ConcoctionDatabase.queuedAdventuresUsed;
		int stillChange = ConcoctionDatabase.queuedStillsUsed;
		int pullChange = ConcoctionDatabase.queuedPullsUsed;

		ArrayList ingredientChange = new ArrayList();
		c.queue( queuedIngredients, ingredientChange, quantity );

		adventureChange = ConcoctionDatabase.queuedAdventuresUsed - adventureChange;
		if ( adventureChange != 0 )
		{
			AdventureResult.addResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.ADV, adventureChange ) );
		}

		stillChange = ConcoctionDatabase.queuedStillsUsed - stillChange;

		pullChange = ConcoctionDatabase.queuedPullsUsed - pullChange;
		if ( pullChange != 0 )
		{
			AdventureResult.addResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.PULL, pullChange ) );
		}

		queuedChanges.push( new Integer( pullChange ) );
		queuedChanges.push( new Integer( stillChange ) );
		queuedChanges.push( new Integer( adventureChange ) );

		queuedChanges.push( ingredientChange );
		queuedChanges.push( new Integer( quantity ) );
		queuedChanges.push( c );
	}

	public static final Object [] pop( boolean food, boolean booze, boolean spleen )
	{
		Stack queuedChanges;
		LockableListModel queuedIngredients;

		if ( food )
		{
			queuedChanges = ConcoctionDatabase.queuedFoodChanges;
			queuedIngredients = ConcoctionDatabase.queuedFoodIngredients;
		}
		else if ( booze )
		{
			queuedChanges = ConcoctionDatabase.queuedBoozeChanges;
			queuedIngredients = ConcoctionDatabase.queuedBoozeIngredients;
		}
		else
		{
			queuedChanges = ConcoctionDatabase.queuedSpleenChanges;
			queuedIngredients = ConcoctionDatabase.queuedSpleenIngredients;
		}

		if ( queuedChanges.isEmpty() )
		{
			return null;
		}

		Concoction c = (Concoction) queuedChanges.pop();
		Integer quantity = (Integer) queuedChanges.pop();
		ArrayList ingredientChange = (ArrayList) queuedChanges.pop();

		Integer adventureChange = (Integer) queuedChanges.pop();
		Integer stillChange = (Integer) queuedChanges.pop();
		Integer pullChange = (Integer) queuedChanges.pop();

		c.queued -= quantity.intValue();
		c.queuedPulls -= pullChange.intValue();
		for ( int i = 0; i < ingredientChange.size(); ++i )
		{
			AdventureResult.addResultToList(
				queuedIngredients, ( (AdventureResult) ingredientChange.get( i ) ).getNegation() );
		}

		int advs = adventureChange.intValue();
		if ( advs != 0 )
		{
			ConcoctionDatabase.queuedAdventuresUsed -= advs;
			AdventureResult.addResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.ADV, -advs ) );
		}

		int stills = stillChange.intValue();
		if ( stills != 0 )
		{
			ConcoctionDatabase.queuedStillsUsed -= stills;
		}

		int pulls = pullChange.intValue();
		if ( pulls != 0 )
		{
			ConcoctionDatabase.queuedPullsUsed -= pulls;
			AdventureResult.addResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.PULL, -pulls ) );
		}

		ConcoctionDatabase.queuedFullness -= c.getFullness() * quantity.intValue();
		ConcoctionDatabase.queuedInebriety -= c.getInebriety() * quantity.intValue();
		ConcoctionDatabase.queuedSpleenHit -= c.getSpleenHit() * quantity.intValue();

		return new Object [] { c, quantity };
	}

	public static final LockableListModel getUsables()
	{
		return ConcoctionDatabase.usableList;
	}

	public static final SortedListModel getCreatables()
	{
		return ConcoctionDatabase.creatableList;
	}

	public static final void handleQueue( boolean food, boolean booze, boolean spleen, int consumptionType )
	{
		Object [] currentItem;
		Stack toProcess = new Stack();

		while ( ( currentItem = ConcoctionDatabase.pop( food, booze, spleen ) ) != null )
		{
			toProcess.push( currentItem );
		}

		Concoction c;
		int quantity = 0;

		SpecialOutfit.createImplicitCheckpoint();
		RequestThread.openRequestSequence();

		while ( !toProcess.isEmpty() )
		{
			currentItem = (Object []) toProcess.pop();

			c = (Concoction) currentItem[ 0 ];
			quantity = ( (Integer) currentItem[ 1 ] ).intValue();

			if ( consumptionType != KoLConstants.CONSUME_USE && c.getItem() != null )
			{
				int consumpt = ItemDatabase.getConsumptionType( c.getItemId() );
				if ( consumpt == KoLConstants.CONSUME_FOOD_HELPER ||
					consumpt == KoLConstants.CONSUME_DRINK_HELPER )
				{
					continue;
				}
				AdventureResult toConsume = c.getItem().getInstance( quantity );
				InventoryManager.retrieveItem( toConsume );

				if ( consumptionType == KoLConstants.CONSUME_GHOST || consumptionType == KoLConstants.CONSUME_HOBO )
				{
					RequestThread.postRequest( new UseItemRequest( consumptionType, toConsume ) );
				}

				continue;
			}

			ConcoctionDatabase.consumeItem( c, quantity );
		}

		SpecialOutfit.restoreImplicitCheckpoint();
		RequestThread.closeRequestSequence();

		ConcoctionDatabase.ignoreRefresh = false;
		ConcoctionDatabase.refreshConcoctions();
	}

	private static final void consumeItem( Concoction c, int quantity )
	{
		AdventureResult item = c.getItem();

		// First, consume any items which appear in the inventory.

		if ( item != null )
		{
			int initialConsume = Math.min( quantity, InventoryManager.getCount( item.getItemId() ) );

			UseItemRequest request = new UseItemRequest( c.getItem().getInstance( initialConsume ) );
			RequestThread.postRequest( request );

			quantity -= initialConsume;

			if ( quantity == 0 )
			{
				return;
			}
		}

		// If there's an actual item, it's not from a store

		if ( item != null )
		{
			// If concoction is a normal item, use normal item
			// acquisition methods.

			if ( item.getItemId() > 0 )
			{
				UseItemRequest request = new UseItemRequest( item.getInstance( quantity ) );
				RequestThread.postRequest( request );
				return;
			}

			// Otherwise, making item will consume it.
			CreateItemRequest request = CreateItemRequest.getInstance( item.getInstance( quantity ) );
			request.setQuantityNeeded( quantity );
			RequestThread.postRequest( request );
			return;
		}

		// Otherwise, acquire them from the restaurant.

		String name = c.getName();
		CafeRequest request;

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
		else if ( CrimboCafeRequest.onMenu( name ) )
		{
			request = new CrimboCafeRequest( name );
		}
		else
		{
			return;
		}

		for ( int j = 0; j < quantity; ++j )
		{
			RequestThread.postRequest( request );
		}
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
		boolean includeCloset = !KoLConstants.closet.isEmpty() && Preferences.getBoolean( "autoSatisfyWithCloset" );
		boolean includeStorage = KoLCharacter.canInteract() && !KoLConstants.storage.isEmpty();
		boolean includeStash =
			KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithStash" ) && !ClanManager.getStash().isEmpty();

		boolean includeQueue =
			!ConcoctionDatabase.queuedFoodIngredients.isEmpty() ||
			!ConcoctionDatabase.queuedBoozeIngredients.isEmpty() ||
			!ConcoctionDatabase.queuedSpleenIngredients.isEmpty();

		if ( !includeCloset && !includeStash && !includeQueue &&
			ConcoctionDatabase.queuedFoodIngredients.isEmpty() &&
			ConcoctionDatabase.queuedBoozeIngredients.isEmpty() &&
			ConcoctionDatabase.queuedSpleenIngredients.isEmpty() )
		{
			return KoLConstants.inventory;
		}

		SortedListModel availableIngredients = new SortedListModel();
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

		if ( !ConcoctionDatabase.queuedFoodIngredients.isEmpty() )
		{
			for ( int i = 0; i < ConcoctionDatabase.queuedFoodIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedFoodIngredients.get( i );
				if ( ingredient.isItem() )
				{
					AdventureResult.addResultToList(
						availableIngredients,
						ingredient.getNegation() );
				}
			}
		}

		if ( !ConcoctionDatabase.queuedBoozeIngredients.isEmpty() )
		{
			for ( int i = 0; i < ConcoctionDatabase.queuedBoozeIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedBoozeIngredients.get( i );
				if ( ingredient.isItem() )
				{
					AdventureResult.addResultToList(
						availableIngredients,
						ingredient.getNegation() );
				}
			}
		}

		if ( !ConcoctionDatabase.queuedSpleenIngredients.isEmpty() )
		{
			for ( int i = 0; i < ConcoctionDatabase.queuedSpleenIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedSpleenIngredients.get( i );
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

	public static final void deferRefresh( boolean flag )
	{
		ConcoctionDatabase.deferRefresh = flag;
		if ( !flag && ConcoctionDatabase.refreshDeferred )
		{
			ConcoctionDatabase.refreshConcoctions();
			ConcoctionDatabase.refreshDeferred = false;
		}
	}

	/**
	 * Returns the concoctions which are available given the list of ingredients. The list returned contains formal
	 * requests for item creation.
	 */

	public static final synchronized void refreshConcoctions()
	{
		if ( ConcoctionDatabase.deferRefresh )
		{
			ConcoctionDatabase.refreshDeferred = true;
			return;
		}
		
		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();
			item.resetCalculations();
		}

		if ( Preferences.getBoolean( "autoSatisfyWithNPCs" ) )
		{
			it = ConcoctionPool.iterator();

			while ( it.hasNext() )
			{
				Concoction item = (Concoction) it.next();

				AdventureResult concoction = item.concoction;
				if ( concoction == null )
				{
					continue;
				}

				if ( !NPCStoreDatabase.contains( concoction.getName(), true ) )
				{
					continue;
				}

				if ( concoction.getItemId() == ItemPool.FLAT_DOUGH )
				{	// Don't buy flat dough from Degrassi Knoll Bakery -
					// buy wads of dough for 20 meat less, instead.
					continue;
				}

				item.price = NPCStoreDatabase.price( concoction.getName() );
				item.initial = concoction.getCount( availableIngredients );
				item.creatable = 0;
				item.total = item.initial;
				item.visibleTotal = item.total;
			}
		}

		// Set initial quantity of all remaining items.

		it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();
			if ( item.initial != -1 )
			{
				continue;
			}

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
			{
				continue;
			}
			
			// Switch to the better of any interchangeable ingredients
			ConcoctionDatabase.getIngredients( item.getIngredients(), availableIngredients );

			item.initial = concoction.getCount( availableIngredients );
			item.price = 0;
			item.creatable = 0;
			item.total = item.initial;
			item.visibleTotal = item.total;
		}

		// Make assessment of availability of mixing methods.
		// This method will also calculate the availability of
		// chefs and bartenders automatically so a second call
		// is not needed.

		ConcoctionDatabase.cachePermitted( availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();

			item.calculate2();
		}

		// Now, to update the list of creatables without removing
		// all creatable items.	 We do this by determining the
		// number of items inside of the old list.

		boolean changeDetected = false;
		boolean considerPulls = !KoLCharacter.canInteract() &&
			!KoLCharacter.isHardcore() &&
			ItemManageFrame.getPullsBudgeted() > ConcoctionDatabase.queuedPullsUsed;

		it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();
			
			AdventureResult ar = item.getItem();
			if ( ar == null )
			{
				continue;
			}

			if ( considerPulls &&
				ar.getItemId() > 0 &&
				item.getPrice() <= 0 &&
				ItemDatabase.meetsLevelRequirement( item.getName() ) )
			{
				item.setPullable( Math.min ( ar.getCount( KoLConstants.storage ) - item.queuedPulls, ItemManageFrame.getPullsBudgeted() - ConcoctionDatabase.queuedPullsUsed ) );
			}
			else
			{
				item.setPullable( 0 );
			}

			CreateItemRequest instance = CreateItemRequest.getInstance( ar, false );

			if ( instance == null )
			{
				continue;
			}

			int creatable = Math.max( item.creatable, 0 );
			int pullable = Math.max( item.pullable, 0 );

			instance.setQuantityPossible( creatable );
			instance.setQuantityPullable( pullable );
			
			if ( creatable + pullable == 0 )
			{
				if ( item.wasPossible() )
				{
					ConcoctionDatabase.creatableList.remove( instance );
					item.setPossible( false );
					changeDetected = true;
				}
			}
			else if ( !item.wasPossible() )
			{
				ConcoctionDatabase.creatableList.add( instance );
				item.setPossible( true );
				changeDetected = true;
			}
		}

		if ( !ConcoctionDatabase.ignoreRefresh )
		{
			ConcoctionDatabase.creatableList.updateFilter( changeDetected );
			ConcoctionDatabase.usableList.updateFilter( changeDetected );
		}
	}

	/**
	 * Reset concoction stat gains when you've logged in a new
	 * character.
	 */

	public static final void resetConcoctionStatGains()
	{
		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction current = (Concoction) it.next();
			current.setStatGain();
		}

		ConcoctionDatabase.usableList.sort();
	}

	private static final void calculateBasicItems( final List availableIngredients )
	{
		// Meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.

		ConcoctionDatabase.setBuyableItem(
			availableIngredients, ItemPool.MEAT_PASTE, 10 );
		ConcoctionDatabase.setBuyableItem(
			availableIngredients, ItemPool.MEAT_STACK, 100 );
		ConcoctionDatabase.setBuyableItem(
			availableIngredients, ItemPool.DENSE_STACK, 1000 );

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

	private static final void setBasicItem( final List availableIngredients, final int itemId, final int acquirable )
	{
		Concoction creation = ConcoctionPool.get( itemId );
		if ( creation == null )
		{
			return;
		}

		creation.initial = ItemPool.get( itemId, 1 ).getCount( availableIngredients );
		creation.price = 0;
		creation.acquirable = acquirable;
		creation.creatable = acquirable;
		creation.total = creation.initial + acquirable;
		creation.visibleTotal = creation.total;
	}

	private static final void setBuyableItem( final List availableIngredients, final int itemId, final int price )
	{
		Concoction creation = ConcoctionPool.get( itemId );
		if ( creation == null )
		{
			return;
		}

		creation.initial = ItemPool.get( itemId, 1 ).getCount( availableIngredients );
		creation.price = price;
		creation.creatable = 0;
		creation.total = creation.initial;
		creation.visibleTotal = creation.total;
	}

	/**
	 * Utility method used to cache the current permissions on item creation.
	 */

	private static final void cachePermitted( final List availableIngredients )
	{
		boolean willBuyServant = KoLCharacter.canInteract() &&
			Preferences.getBoolean( "autoRepairBoxServants" ) &&
			( Preferences.getBoolean( "autoSatisfyWithMall" ) ||
				Preferences.getBoolean( "autoSatisfyWithStash" ) );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		ConcoctionDatabase.adventureLimit.total = KoLCharacter.getAdventuresLeft();
		ConcoctionDatabase.adventureLimit.initial =
			ConcoctionDatabase.adventureLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
		ConcoctionDatabase.adventureLimit.creatable = 0;
		ConcoctionDatabase.adventureLimit.visibleTotal = ConcoctionDatabase.adventureLimit.total;

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		ConcoctionDatabase.stillsLimit.total = KoLCharacter.getStillsAvailable();
		ConcoctionDatabase.stillsLimit.initial =
			ConcoctionDatabase.stillsLimit.total - ConcoctionDatabase.queuedStillsUsed;
		ConcoctionDatabase.stillsLimit.creatable = 0;
		ConcoctionDatabase.stillsLimit.visibleTotal = ConcoctionDatabase.stillsLimit.total;

		// Meat is also also considered Item #0 in the event that the
		// concoction will create paste/stacks or buy NPC items.

		ConcoctionDatabase.meatLimit.total = KoLCharacter.getAvailableMeat();
		ConcoctionDatabase.meatLimit.initial =
			ConcoctionDatabase.meatLimit.total; 
			// - ConcoctionDatabase.queuedMeatSpent ???
		ConcoctionDatabase.meatLimit.creatable = 0;
		ConcoctionDatabase.meatLimit.visibleTotal = ConcoctionDatabase.meatLimit.total;

		ConcoctionDatabase.calculateBasicItems( availableIngredients );
		
		int flags = KoLCharacter.getGender() == KoLCharacter.MALE ?
			KoLConstants.CR_MALE : KoLConstants.CR_FEMALE;
		Arrays.fill( ConcoctionDatabase.PERMIT_METHOD, false );
		Arrays.fill( ConcoctionDatabase.ADVENTURE_USAGE, 0 );
		Arrays.fill( ConcoctionDatabase.CREATION_COST, 0 );

		// It is never possible to create items which are flagged
		// NOCREATE

		// It is always possible to create items through meat paste
		// combination.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COMBINE ] = true;
		ConcoctionDatabase.CREATION_COST[ KoLConstants.COMBINE ] = 10;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.GNOME_TINKER ] = KoLCharacter.inMoxieSign();

		// Smithing of items is possible whenever the person
		// has a hammer.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] =
			KoLCharacter.getAvailableMeat() >= 1000;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 1;
		
		if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] = true;
			flags |= KoLConstants.CR_HAMMER;
		}

		if ( InventoryManager.hasItem( ItemPool.GRIMACITE_HAMMER ) )
		{
			flags |= KoLConstants.CR_GRIMACITE;
		}

		// Advanced smithing is available whenever the person can
		// smith.  The appropriate skill is checked separately.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SSMITH ] =
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ];
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SSMITH ] = 1;

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.inMuscleSign() )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COMBINE ] = 0;
		}

		if ( KoLCharacter.canSmithWeapons() )
		{
			flags |= KoLConstants.CR_WEAPON;
		}

		if ( KoLCharacter.canSmithArmor() )
		{
			flags |= KoLConstants.CR_ARMOR;
		}

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.JEWELRY ] =
			InventoryManager.hasItem( ItemPool.JEWELRY_PLIERS );
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.JEWELRY ] = 3;

		if ( KoLCharacter.canCraftExpensiveJewelry() )
		{
			flags |= KoLConstants.CR_EXPENSIVE;
		}

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STARCHART ] = true;
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.PIXEL ] = true;
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MULTI_USE ] = true;
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SINGLE_USE ] = true;
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SUGAR_FOLDING ] = true;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.ROLLING_PIN ] = true;

		// Rodoric will make chefstaves for mysticality class
		// characters who can get to the guild.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STAFF ] =
			KoLCharacter.isMysticalityClass();

		// It's not possible to ask Uncle Crimbo 2005 to make toys
		// It's not possible to ask Ugh Crimbo 2006 to make toys
		// It's not possible to ask Uncle Crimbo 2007 to make toys

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		ConcoctionPool.get( ItemPool.CHEF ).calculate2();
		ConcoctionPool.get( ItemPool.CLOCKWORK_CHEF ).calculate2();
		ConcoctionPool.get( ItemPool.BARTENDER ).calculate2();
		ConcoctionPool.get( ItemPool.CLOCKWORK_BARTENDER ).calculate2();

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
			willBuyServant || KoLCharacter.hasChef() || ConcoctionDatabase.isAvailable( ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF );
		ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK ] =
			MallPriceDatabase.getPrice( ItemPool.CHEF ) / 90;

		if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] &&
			!Preferences.getBoolean( "requireBoxServants" ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
				InventoryManager.hasItem( ItemPool.BAKE_OVEN ) || KoLCharacter.getAvailableMeat() >= 1000;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ] = 1;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK ] = 0;
		}

		// Cooking may require an additional skill.

		if ( KoLCharacter.canSummonReagent() )
		{
			flags |= KoLConstants.CR_REAGENT;
		}

		if ( KoLCharacter.hasSkill( "The Way of Sauce" ) )
		{
			flags |= KoLConstants.CR_WAY;
		}

		if ( KoLCharacter.hasSkill( "Deep Saucery" ) )
		{
			flags |= KoLConstants.CR_DEEP;
		}

		if ( KoLCharacter.canSummonNoodles() )
		{
			flags |= KoLConstants.CR_PASTA;
		}

		if ( KoLCharacter.hasSkill( "Tempuramancy" ) )
		{
			flags |= KoLConstants.CR_TEMPURA;
		}

		// Mixing is possible whenever the person has a bartender
		// or they don't need a box servant and have a kit.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
			willBuyServant || KoLCharacter.hasBartender() || ConcoctionDatabase.isAvailable( ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER );
		ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX ] =
			MallPriceDatabase.getPrice( ItemPool.BARTENDER ) / 90;

		if ( !ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] &&
			!Preferences.getBoolean( "requireBoxServants" ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
				InventoryManager.hasItem( ItemPool.COCKTAIL_KIT ) || KoLCharacter.getAvailableMeat() >= 1000;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 1;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX ] = 0;
		}

		// Mixing may require an additional skill.

		if ( KoLCharacter.canSummonShore() )
		{
			flags |= KoLConstants.CR_AC;
		}

		if ( KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) )
		{
			flags |= KoLConstants.CR_SHC;
		}

		if ( KoLCharacter.hasSkill( "Salacious Cocktailcrafting" ) )
		{
			flags |= KoLConstants.CR_SALACIOUS;
		}

		// Using Crosby Nash's Still is possible if the person has
		// Superhuman Cocktailcrafting and is a Moxie class character.

		boolean hasStillsAvailable = ConcoctionDatabase.stillsLimit.total > 0;
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STILL_MIXER ] = hasStillsAvailable;

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.STILL_BOOZE ] = hasStillsAvailable;

		ConcoctionDatabase.CREATION_COST[ KoLConstants.STILL_MIXER ] =
			ConcoctionDatabase.CREATION_COST[ KoLConstants.STILL_BOOZE ] =
				Preferences.getInteger( "valueOfStill" );

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.WOK ] =
			KoLCharacter.canUseWok() && KoLCharacter.getAdventuresLeft() > 0;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.WOK ] = 1;

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MALUS ] = KoLCharacter.canUseMalus();

		// You can make Sushi if you have a sushi-rolling mat

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SUSHI ] = InventoryManager.hasItem( ItemPool.SUSHI_ROLLING_MAT );
		
		// Other creatability flags
		
		if ( KoLCharacter.hasSkill( "Torso Awaregness" ) )
		{
			flags |= KoLConstants.CR_TORSO;
		}

		if ( HolidayDatabase.getHoliday().equals( "St. Sneaky Pete's Day" ) )
		{
			flags |= KoLConstants.CR_SSPD;
		}

		// Now, go through all the cached adventure usage values and if
		// the number of adventures left is zero and the request requires
		// adventures, it is not permitted.

		int value = Preferences.getInteger( "valueOfAdventure" );
		for ( int i = 0; i < KoLConstants.METHOD_COUNT; ++i )
		{
			int adv = ConcoctionDatabase.ADVENTURE_USAGE[ i ];
			if ( ConcoctionDatabase.PERMIT_METHOD[ i ] && adv > 0 )
			{
				ConcoctionDatabase.PERMIT_METHOD[ i ] =
					ConcoctionDatabase.ADVENTURE_USAGE[ i ] <= KoLCharacter.getAdventuresLeft();
				ConcoctionDatabase.CREATION_COST[ i ] += adv * value;
			}
		}
		
		ConcoctionDatabase.creationFlags = flags;
	}

	private static final boolean isAvailable( final int servantId, final int clockworkId )
	{
		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		return Preferences.getBoolean( "autoRepairBoxServants" ) &&
			( ConcoctionPool.get( servantId ).total > 0 ||
				ConcoctionPool.get( clockworkId ).total > 0 );
	}

	/**
	 * Returns the mixing method for the item with the given Id.
	 */

	public static final int getMixingMethod( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? KoLConstants.NOCREATE : item.getMixingMethod();
	}

	public static final int getMixingMethod( final String name )
	{
		Concoction item = ConcoctionPool.get( name );
		return item == null ? KoLConstants.NOCREATE : item.getMixingMethod();
	}

	public static final int getMixingMethod( final AdventureResult ar )
	{
		Concoction item = ConcoctionPool.get( ar );
		return item == null ? KoLConstants.NOCREATE : item.getMixingMethod();
	}

	/**
	 * Returns the item Ids of the ingredients for the given item. Note
	 * that if there are no ingredients, then <code>null</code> will be
	 * returned instead.
	 */

	public static final AdventureResult[] getIngredients( final int itemId )
	{
		return ConcoctionDatabase.getIngredients( ConcoctionDatabase.getStandardIngredients( itemId ) );
	}

	public static final AdventureResult[] getIngredients( final String name )
	{
		return ConcoctionDatabase.getIngredients( ConcoctionDatabase.getStandardIngredients( name ) );
	}

	public static final AdventureResult[] getIngredients( AdventureResult[] ingredients )
	{
		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();
		return ConcoctionDatabase.getIngredients( ingredients, availableIngredients );
	}

	private static final AdventureResult[] getIngredients( AdventureResult[] ingredients, List availableIngredients )
	{
		// Ensure that you're retrieving the same ingredients that
		// were used in the calculations.  Usually this is the case,
		// but ice-cold beer and ketchup are tricky cases.

		for ( int i = 0; i < ingredients.length; ++i )
		{
			switch ( ingredients[ i ].getItemId() )
			{
			case ItemPool.SCHLITZ:
			case ItemPool.WILLER:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.SCHLITZ, ItemPool.WILLER, availableIngredients );
				break;
				
			case ItemPool.KETCHUP:
			case ItemPool.CATSUP:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.KETCHUP, ItemPool.CATSUP, availableIngredients );
				break;
				
			case ItemPool.DYSPEPSI_COLA:
			case ItemPool.CLOACA_COLA:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.DYSPEPSI_COLA, ItemPool.CLOACA_COLA, availableIngredients );
				break;
				
			case ItemPool.TITANIUM_UMBRELLA:
			case ItemPool.GOATSKIN_UMBRELLA:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.TITANIUM_UMBRELLA, ItemPool.GOATSKIN_UMBRELLA, availableIngredients );
				break;
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
		return ConcoctionDatabase.getStandardIngredients( ConcoctionPool.get( itemId ) );
	}

	public static final AdventureResult[] getStandardIngredients( final String name )
	{
		return ConcoctionDatabase.getStandardIngredients( ConcoctionPool.get( name ) );
	}

	public static final AdventureResult[] getStandardIngredients( final Concoction item )
	{
		return item == null ? ConcoctionDatabase.NO_INGREDIENTS : item.getIngredients();
	}

	private static final AdventureResult getBetterIngredient( final int itemId1,
		final int itemId2, final List availableIngredients )
	{
		AdventureResult ingredient1 = ItemPool.get( itemId1, 1 );
		AdventureResult ingredient2 = ItemPool.get( itemId2, 1 );
		int diff = ingredient1.getCount( availableIngredients ) -
			ingredient2.getCount( availableIngredients );
		if ( diff == 0 )
		{
			diff = MallPriceDatabase.getPrice( itemId2 ) -
				MallPriceDatabase.getPrice( itemId1 );
		}
		return diff > 0 ? ingredient1 : ingredient2;
	}
}
