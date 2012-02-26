/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.CrimboCafeRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.SortedListModelArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcoctionDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	private static final SortedListModel creatableList = new SortedListModel();
	private static final LockableListModel usableList = new LockableListModel();

	public static String excuse;	// reason why creation is impossible

	private static boolean refreshNeeded = true;
	private static boolean recalculateAdventureRange = false;
	public static int refreshLevel = 0;

	public static int queuedAdventuresUsed = 0;
	public static int queuedFreeCraftingTurns = 0;
	public static int queuedStillsUsed = 0;
	public static int queuedTomesUsed = 0;
	public static int queuedPullsUsed = 0;
	public static int queuedMeatSpent = 0;

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
	public static final Concoction tomeLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	public static final Concoction adventureLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	public static final Concoction turnFreeLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );
	public static final Concoction meatLimit = new Concoction( (AdventureResult) null, KoLConstants.NOCREATE );

	public static final SortedListModelArray knownUses = new SortedListModelArray();

	public static final boolean[] PERMIT_METHOD = new boolean[ KoLConstants.METHOD_COUNT ];
	public static final int[] ADVENTURE_USAGE = new int[ KoLConstants.METHOD_COUNT ];
	public static final int[] CREATION_COST = new int[ KoLConstants.METHOD_COUNT ];
	public static final String[] EXCUSE = new String[ KoLConstants.METHOD_COUNT ];
	public static int creationFlags;

	private static final AdventureResult[] NO_INGREDIENTS = new AdventureResult[ 0 ];

	public static final AdventureResult INIGO = new AdventureResult( "Inigo's Incantation of Inspiration", 0, true );

	private static final HashMap mixingMethods = new HashMap();
	private static final String[] METHOD_DESCRIPTION = new String[ KoLConstants.METHOD_COUNT ];

	static
	{
		// Basic creation types:

		// Items anybody can create using meat paste or The Plunger
		ConcoctionDatabase.mixingMethods.put( "COMBINE", IntegerPool.get( KoLConstants.COMBINE ));
		METHOD_DESCRIPTION[ KoLConstants.COMBINE ] = "Meatpasting";
		// Items anybody can create with an E-Z Cook Oven or Dramatic Range
		ConcoctionDatabase.mixingMethods.put( "COOK", IntegerPool.get( KoLConstants.COOK ));
		METHOD_DESCRIPTION[ KoLConstants.COOK ] = "Cooking";
		// Items anybody can create with a Shaker or Cocktailcrafting Kit
		ConcoctionDatabase.mixingMethods.put( "MIX", IntegerPool.get( KoLConstants.MIX ));
		METHOD_DESCRIPTION[ KoLConstants.MIX ] = "Mixing";
		// Items anybody can create with a tenderizing hammer or via Innabox
		ConcoctionDatabase.mixingMethods.put( "SMITH", IntegerPool.get( KoLConstants.SMITH ));
		METHOD_DESCRIPTION[ KoLConstants.SMITH ] = "Meatsmithing";
		// Items that can only be created with a tenderizing hammer, not via Innabox
		ConcoctionDatabase.mixingMethods.put( "SSMITH", IntegerPool.get( KoLConstants.SSMITH ));
		METHOD_DESCRIPTION[ KoLConstants.SSMITH ] = "Meatsmithing (not Innabox)";
		// Items requiring access to Nash Crosby's Still -- booze
		ConcoctionDatabase.mixingMethods.put( "BSTILL", IntegerPool.get( KoLConstants.STILL_BOOZE ));
		METHOD_DESCRIPTION[ KoLConstants.STILL_BOOZE ] = "Nash Crosby's Still";
		// Items requiring Superhuman Cocktailcrafting -- mixer
		ConcoctionDatabase.mixingMethods.put( "MSTILL", IntegerPool.get( KoLConstants.STILL_MIXER ));
		METHOD_DESCRIPTION[ KoLConstants.STILL_MIXER ] = "Nash Crosby's Still";
		// Items requiring access to the Wok of Ages
		ConcoctionDatabase.mixingMethods.put( "WOK", IntegerPool.get( KoLConstants.WOK ));
		METHOD_DESCRIPTION[ KoLConstants.WOK ] = "Wok of Ages";
		// Items requiring access to the Malus of Forethought
		ConcoctionDatabase.mixingMethods.put( "MALUS", IntegerPool.get( KoLConstants.MALUS ));
		METHOD_DESCRIPTION[ KoLConstants.MALUS ] = "Malus of Forethought";
		// Items anybody can create with jewelry-making pliers
		ConcoctionDatabase.mixingMethods.put( "JEWEL", IntegerPool.get( KoLConstants.JEWELRY ));
		METHOD_DESCRIPTION[ KoLConstants.JEWELRY ] = "Jewelry-making pliers";
		// Items anybody can create with starcharts, stars, and lines
		ConcoctionDatabase.mixingMethods.put( "STAR", IntegerPool.get( KoLConstants.STARCHART ));
		METHOD_DESCRIPTION[ KoLConstants.STARCHART ] = "star chart";
		// Items anybody can create by folding sugar sheets
		ConcoctionDatabase.mixingMethods.put( "SUGAR", IntegerPool.get( KoLConstants.SUGAR_FOLDING ));
		METHOD_DESCRIPTION[ KoLConstants.SUGAR_FOLDING ] = "sugar sheet";
		// Items anybody can create with pixels
		ConcoctionDatabase.mixingMethods.put( "PIXEL", IntegerPool.get( KoLConstants.PIXEL ));
		METHOD_DESCRIPTION[ KoLConstants.PIXEL ] = "Crackpot Mystic";
		// Items created with a rolling pin or and an unrolling pin
		ConcoctionDatabase.mixingMethods.put( "ROLL", IntegerPool.get( KoLConstants.ROLLING_PIN ));
		METHOD_DESCRIPTION[ KoLConstants.ROLLING_PIN ] = "rolling pin/unrolling pin";
		// Items requiring access to the Gnome supertinker
		ConcoctionDatabase.mixingMethods.put( "TINKER", IntegerPool.get( KoLConstants.GNOME_TINKER ));
		METHOD_DESCRIPTION[ KoLConstants.GNOME_TINKER ] = "Supertinkering";
		// Items requiring access to Roderick the Staffmaker
		ConcoctionDatabase.mixingMethods.put( "STAFF", IntegerPool.get( KoLConstants.STAFF ));
		METHOD_DESCRIPTION[ KoLConstants.STAFF ] = "Rodoric, the Staffcrafter";
		// Items anybody can create with a sushi-rolling mat
		ConcoctionDatabase.mixingMethods.put( "SUSHI", IntegerPool.get( KoLConstants.SUSHI ));
		METHOD_DESCRIPTION[ KoLConstants.SUSHI ] = "sushi-rolling mat";
		// Items created by single (or multi) using a single item.
		// Extra ingredients might also be consumed.
		// Multi-using multiple of the item creates multiple results.
		ConcoctionDatabase.mixingMethods.put( "SUSE", IntegerPool.get( KoLConstants.SINGLE_USE ));
		METHOD_DESCRIPTION[ KoLConstants.SINGLE_USE ] = "single-use";
		// Items created by multi-using specific # of a single item.
		// Extra ingredients might also be consumed.
		// You must create multiple result items one at a time.
		ConcoctionDatabase.mixingMethods.put( "MUSE", IntegerPool.get( KoLConstants.MULTI_USE ));
		METHOD_DESCRIPTION[ KoLConstants.MULTI_USE ] = "multi-use";
		// Items formerly creatable in Crimbo Town during Crimbo 2005
		ConcoctionDatabase.mixingMethods.put( "CRIMBO05", IntegerPool.get( KoLConstants.CRIMBO05 ));
		METHOD_DESCRIPTION[ KoLConstants.CRIMBO05 ] = "Crimbo Town Toy Factory (Crimbo 2005)";
		// Items formerly creatable in Crimbo Town during Crimbo 2006
		ConcoctionDatabase.mixingMethods.put( "CRIMBO06", IntegerPool.get( KoLConstants.CRIMBO06 ));
		METHOD_DESCRIPTION[ KoLConstants.CRIMBO06 ] = "Uncle Crimbo's Mobile Home (Crimboween 2006)";
		// Items formerly creatable in Crimbo Town during Crimbo 2007
		ConcoctionDatabase.mixingMethods.put( "CRIMBO07", IntegerPool.get( KoLConstants.CRIMBO07 ));
		METHOD_DESCRIPTION[ KoLConstants.CRIMBO07 ] = "Uncle Crimbo's Mobile Home (Crimbo 2007)";
		// Items requiring access to Phineas
		ConcoctionDatabase.mixingMethods.put( "PHINEAS", IntegerPool.get( KoLConstants.PHINEAS ));
		METHOD_DESCRIPTION[ KoLConstants.PHINEAS ] = "Phineas";
		// Items that require a Dramatic Range
		ConcoctionDatabase.mixingMethods.put( "COOK_FANCY", IntegerPool.get( KoLConstants.COOK_FANCY ));
		METHOD_DESCRIPTION[ KoLConstants.COOK_FANCY ] = "Cooking (fancy)";
		// Items that require a Cocktailcrafting Kit
		ConcoctionDatabase.mixingMethods.put( "MIX_FANCY", IntegerPool.get( KoLConstants.MIX_FANCY ));
		METHOD_DESCRIPTION[ KoLConstants.MIX_FANCY ] = "Mixing (fancy)";
		// Un-untinkerable Amazing Ideas
		ConcoctionDatabase.mixingMethods.put( "ACOMBINE", IntegerPool.get( KoLConstants.ACOMBINE ));
		METHOD_DESCRIPTION[ KoLConstants.ACOMBINE ] = "Meatpasting (not untinkerable)";
		// Coinmaster purchase
		METHOD_DESCRIPTION[ KoLConstants.COINMASTER ] = "Coin Master purchase";
		// Summon Clip Art items
		ConcoctionDatabase.mixingMethods.put( "CLIPART", IntegerPool.get( KoLConstants.CLIPART ));
		METHOD_DESCRIPTION[ KoLConstants.CLIPART ] = "Summon Clip Art";

		// Creation flags

		// Character gender (for kilt vs. skirt)
		ConcoctionDatabase.mixingMethods.put( "MALE", IntegerPool.get( KoLConstants.CR_MALE ));
		// Character gender (for kilt vs. skirt)
		ConcoctionDatabase.mixingMethods.put( "FEMALE", IntegerPool.get( KoLConstants.CR_FEMALE ));
		// Holiday-only
		ConcoctionDatabase.mixingMethods.put( "SSPD", IntegerPool.get( KoLConstants.CR_SSPD ));
		// Requires tenderizing hammer (implied for SMITH & SSMITH)
		ConcoctionDatabase.mixingMethods.put( "HAMMER", IntegerPool.get( KoLConstants.CR_HAMMER ));
		// Requires depleted Grimacite hammer
		ConcoctionDatabase.mixingMethods.put( "GRIMACITE", IntegerPool.get( KoLConstants.CR_GRIMACITE ));
		// Requires Torso Awaregness
		ConcoctionDatabase.mixingMethods.put( "TORSO", IntegerPool.get( KoLConstants.CR_TORSO ));
		// Requires Super-Advanced Meatsmithing
		ConcoctionDatabase.mixingMethods.put( "WEAPON", IntegerPool.get( KoLConstants.CR_WEAPON ));
		// Requires Armorcraftiness
		ConcoctionDatabase.mixingMethods.put( "ARMOR", IntegerPool.get( KoLConstants.CR_ARMOR ));
		// Requires Really Expensive Jewerlycrafting
		ConcoctionDatabase.mixingMethods.put( "EXPENSIVE", IntegerPool.get( KoLConstants.CR_EXPENSIVE ));
		// Requires Advanced Saucecrafting
		ConcoctionDatabase.mixingMethods.put( "REAGENT", IntegerPool.get( KoLConstants.CR_REAGENT ));
		// Requires The Way of Sauce
		ConcoctionDatabase.mixingMethods.put( "WAY", IntegerPool.get( KoLConstants.CR_WAY ));
		// Requires Deep Saucery
		ConcoctionDatabase.mixingMethods.put( "DEEP", IntegerPool.get( KoLConstants.CR_DEEP ));
		// Requires Pastamastery
		ConcoctionDatabase.mixingMethods.put( "PASTAMASTERY", IntegerPool.get( KoLConstants.CR_PASTA ));
		// Requires Tempuramancy
		ConcoctionDatabase.mixingMethods.put( "TEMPURAMANCY", IntegerPool.get( KoLConstants.CR_TEMPURA ));
		// Requires Advanced Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "AC", IntegerPool.get( KoLConstants.CR_AC ));
		// Requires Superhuman Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SHC", IntegerPool.get( KoLConstants.CR_SHC ));
		// Requires Salacious Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SALACIOUS", IntegerPool.get( KoLConstants.CR_SALACIOUS ));

		// Items creatable only if not on Bees Hate You path
		ConcoctionDatabase.mixingMethods.put( "NOBEE", IntegerPool.get( KoLConstants.CR_NOBEE ));

		// Saucerors make 3 of this item at a time
		ConcoctionDatabase.mixingMethods.put( "SX3", IntegerPool.get( KoLConstants.CF_SX3 ));
		// Recipe unexpectedly does not appear in Discoveries, even though
		// it uses a discoverable crafting type
		ConcoctionDatabase.mixingMethods.put( "NODISCOVERY", IntegerPool.get( KoLConstants.CF_NODISCOVERY ));
		// Recipe should never be used automatically
		ConcoctionDatabase.mixingMethods.put( "MANUAL", IntegerPool.get( KoLConstants.CF_MANUAL ));

		// Combinations of creation type & flags, for convenience

		// Items requiring Pastamastery
		ConcoctionDatabase.mixingMethods.put( "PASTA", IntegerPool.get( KoLConstants.COOK_FANCY | KoLConstants.CR_PASTA ));
		// Items requiring Tempuramancy
		ConcoctionDatabase.mixingMethods.put( "TEMPURA", IntegerPool.get( KoLConstants.COOK_FANCY | KoLConstants.CR_TEMPURA ));
		// Items requiring Super-Advanced Meatsmithing
		ConcoctionDatabase.mixingMethods.put( "WSMITH", IntegerPool.get( KoLConstants.SSMITH | KoLConstants.CR_WEAPON ));
		// Items requiring Armorcraftiness
		ConcoctionDatabase.mixingMethods.put( "ASMITH", IntegerPool.get( KoLConstants.SSMITH | KoLConstants.CR_ARMOR ));
		// Items requiring Advanced Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "ACOCK", IntegerPool.get( KoLConstants.MIX_FANCY | KoLConstants.CR_AC ));
		// Items requiring Superhuman Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SCOCK", IntegerPool.get( KoLConstants.MIX_FANCY | KoLConstants.CR_SHC ));
		// Items requiring Salacious Cocktailcrafting
		ConcoctionDatabase.mixingMethods.put( "SACOCK", IntegerPool.get( KoLConstants.MIX_FANCY | KoLConstants.CR_SALACIOUS ));
		// Items requiring pliers and Really Expensive Jewelrycrafting
		ConcoctionDatabase.mixingMethods.put( "EJEWEL", IntegerPool.get( KoLConstants.JEWELRY | KoLConstants.CR_EXPENSIVE ));
		// Items requiring Advanced Saucecrafting
		ConcoctionDatabase.mixingMethods.put( "SAUCE", IntegerPool.get( KoLConstants.COOK_FANCY | KoLConstants.CR_REAGENT ));
		// Items requiring The Way of Sauce
		ConcoctionDatabase.mixingMethods.put( "SSAUCE", IntegerPool.get( KoLConstants.COOK_FANCY | KoLConstants.CR_WAY ));
		// Items requiring Deep Saucery
		ConcoctionDatabase.mixingMethods.put( "DSAUCE", IntegerPool.get( KoLConstants.COOK_FANCY | KoLConstants.CR_DEEP ));
	}

	private static final HashMap chefStaff = new HashMap();
	private static final HashMap singleUse = new HashMap();
	private static final HashMap multiUse = new HashMap();
	private static final HashMap noodles = new HashMap();
	private static final HashMap meatStack = new HashMap();

	public static final void resetQueue()
	{
		Stack queuedChanges = ConcoctionDatabase.queuedFoodChanges;
		while ( !queuedChanges.empty() )
		{
			ConcoctionDatabase.pop( true, false, false );
		}
		queuedChanges = ConcoctionDatabase.queuedBoozeChanges;
		while ( !queuedChanges.empty() )
		{
			ConcoctionDatabase.pop( false, true, false );
		}
		queuedChanges = ConcoctionDatabase.queuedSpleenChanges;
		while ( !queuedChanges.empty() )
		{
			ConcoctionDatabase.pop( false, false, true );
		}
	}

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

		String name = new String( data[ 0 ] );
		String[] mixes = data[ 1 ].split( "\\s*,\\s*" );
		int mixingMethod = 0;
		for ( int i = 0; i < mixes.length; ++i )
		{
			String mix = mixes[ i ];
			Integer val = (Integer) ConcoctionDatabase.mixingMethods.get( mix );
			if ( val == null )
			{
				RequestLogger.printLine( "Unknown mixing method or flag (" + mix + ") for concoction: " + name );
				ConcoctionDatabase.mixingMethods.put( new String( mix ), IntegerPool.get( 0 ) );
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
		int param = 0;
		for ( int i = 2; i < data.length; ++i )
		{
			if ( StringUtilities.isNumeric( data[ i ] ) )
			{	// Treat all-numeric element as parameter instead of item.
				// Up to 4 such parameters can be given if each fits in a byte.
				param = (param << 8) | StringUtilities.parseInt( data[ i ] );
				continue;
			}
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
			concoction.setParam( param );

			Concoction existing = ConcoctionPool.get( item );
			if ( (concoction.getMixingMethod() & KoLConstants.CF_MANUAL) != 0 ||
			     (existing != null && existing.getMixingMethod() != 0) )
			{	// Until multiple recipes are supported...
				return;
			}

			for ( int i = 0; i < ingredients.length; ++i )
			{
				AdventureResult ingredient = ingredients[ i ];
				if ( ingredient == null )
				{	// Was a parameter, not an ingredient.
					continue;
				}
				concoction.addIngredient( ingredient );
				if ( ingredient.getItemId() == ItemPool.MEAT_STACK )
				{
					ConcoctionDatabase.meatStack.put( concoction.getName(), concoction );
				}
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
			case KoLConstants.WOK:
				ConcoctionDatabase.noodles.put( concoction.getName(), concoction );
				break;
			}

			if ( ( mixingMethod & KoLConstants.CR_PASTA ) != 0 )
			{
				ConcoctionDatabase.noodles.put( concoction.getName(), concoction );
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

	public static Concoction noodleCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.noodles.get( name );
	}

	public static Concoction meatStackCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.meatStack.get( name );
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
		int base = method & KoLConstants.CT_MASK;

		// If we can't make anything via this method, punt
		if ( !ConcoctionDatabase.PERMIT_METHOD[ base ] )
		{
			return false;
		}

		// If we don't meet special creation requirements for this item, punt
		if ( ( method & KoLConstants.CR_MASK & ~ConcoctionDatabase.creationFlags) != 0 )
		{
			return false;
		}

		// Otherwise, go for it!
		return true;
	}

	public static final boolean checkPermittedMethod( int method )
	{
		// Same as isPermittedMethod(), but sets excuse.
		ConcoctionDatabase.excuse = null;

		int base = method & KoLConstants.CT_MASK;

		if ( !ConcoctionDatabase.PERMIT_METHOD[ base ] )
		{
			ConcoctionDatabase.excuse = ConcoctionDatabase.EXCUSE[ method & KoLConstants.CT_MASK ];
			return false;
		}

		method = method & KoLConstants.CR_MASK & ~ConcoctionDatabase.creationFlags;
		if ( method != 0 )
		{
			String reason = "unknown";
			Iterator i = ConcoctionDatabase.mixingMethods.entrySet().iterator();
			while ( i.hasNext() )
			{
				Map.Entry e = (Map.Entry) i.next();
				int v = ((Integer) e.getValue()).intValue();
				// Look for a mixingMethod token that corresponds to a CR_xxx
				// flag, and has at least one of the failing CR bits.
				if ( (v & method) != 0 &&
					(v & ~KoLConstants.CR_MASK) == 0 )
				{
					reason = (String) e.getKey();
					break;
				}
			}
			ConcoctionDatabase.excuse = "You lack a skill or other prerequisite for creating that item (" + reason + ").";
			return false;
		}

		return true;
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
		     id == ItemPool.MUNCHIES_PILL || id == ItemPool.DISTENTION_PILL )
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
		int freeCraftChange = ConcoctionDatabase.queuedFreeCraftingTurns;
		int stillChange = ConcoctionDatabase.queuedStillsUsed;
		int tomeChange = ConcoctionDatabase.queuedTomesUsed;
		int pullChange = ConcoctionDatabase.queuedPullsUsed;
		int meatChange = ConcoctionDatabase.queuedMeatSpent;

		ArrayList ingredientChange = new ArrayList();
		c.queue( queuedIngredients, ingredientChange, quantity );

		adventureChange = ConcoctionDatabase.queuedAdventuresUsed - adventureChange;
		if ( adventureChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.ADV, adventureChange ) );
		}
		
		freeCraftChange = ConcoctionDatabase.queuedFreeCraftingTurns - freeCraftChange;
		if ( freeCraftChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.FREE_CRAFT, freeCraftChange ) );
		}

		stillChange = ConcoctionDatabase.queuedStillsUsed - stillChange;
		if ( stillChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.STILL, stillChange ) );
		}

		tomeChange = ConcoctionDatabase.queuedTomesUsed - tomeChange;
		if ( tomeChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.TOME, tomeChange ) );
		}

		pullChange = ConcoctionDatabase.queuedPullsUsed - pullChange;
		if ( pullChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.PULL, pullChange ) );
		}

		meatChange = ConcoctionDatabase.queuedMeatSpent - meatChange;
		if ( meatChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.MEAT_SPENT, meatChange ) );
		}

		queuedChanges.push( IntegerPool.get( meatChange ) );
		queuedChanges.push( IntegerPool.get( pullChange ) );
		queuedChanges.push( IntegerPool.get( tomeChange ) );
		queuedChanges.push( IntegerPool.get( stillChange ) );
		queuedChanges.push( IntegerPool.get( adventureChange ) );
		queuedChanges.push( IntegerPool.get( freeCraftChange ) );

		queuedChanges.push( ingredientChange );
		queuedChanges.push( IntegerPool.get( quantity ) );
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

		Integer freeCraftChange = (Integer) queuedChanges.pop();
		Integer adventureChange = (Integer) queuedChanges.pop();
		Integer stillChange = (Integer) queuedChanges.pop();
		Integer tomeChange = (Integer) queuedChanges.pop();
		Integer pullChange = (Integer) queuedChanges.pop();
		Integer meatChange = (Integer) queuedChanges.pop();

		c.queued -= quantity.intValue();
		c.queuedPulls -= pullChange.intValue();
		for ( int i = 0; i < ingredientChange.size(); ++i )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, ( (AdventureResult) ingredientChange.get( i ) ).getNegation() );
		}
		
		int free = freeCraftChange.intValue();
		if ( free != 0 )
		{
			ConcoctionDatabase.queuedFreeCraftingTurns -= free;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.FREE_CRAFT, -free ) );
		}

		int advs = adventureChange.intValue();
		if ( advs != 0 )
		{
			ConcoctionDatabase.queuedAdventuresUsed -= advs;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.ADV, -advs ) );
		}

		int stills = stillChange.intValue();
		if ( stills != 0 )
		{
			ConcoctionDatabase.queuedStillsUsed -= stills;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.STILL, -stills ) );
		}

		int tome = tomeChange.intValue();
		if ( tome != 0 )
		{
			ConcoctionDatabase.queuedTomesUsed -= tome;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.TOME, -tome ) );
		}

		int pulls = pullChange.intValue();
		if ( pulls != 0 )
		{
			ConcoctionDatabase.queuedPullsUsed -= pulls;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.PULL, -pulls ) );
		}

		int meat = meatChange.intValue();
		if ( meat != 0 )
		{
			ConcoctionDatabase.queuedMeatSpent -= meat;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.MEAT_SPENT, -meat ) );
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

		// If we happen to have refreshed concoctions while there were
		// items queued, the creatable amounts will assume that queued
		// ingredients are already spoken for. Refresh again now that
		// the queue is empty.

		ConcoctionDatabase.refreshConcoctions( true );

		Concoction c;
		int quantity = 0;

		SpecialOutfit.createImplicitCheckpoint();

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
					RequestThread.postRequest( UseItemRequest.getInstance( consumptionType, toConsume ) );
				}

				continue;
			}

			ConcoctionDatabase.consumeItem( c, quantity );
		}

		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void consumeItem( Concoction c, int quantity )
	{
		AdventureResult item = c.getItem();

		// First, consume any items which appear in the inventory.

		if ( item != null )
		{
			int initialConsume = Math.min( quantity, InventoryManager.getCount( item.getItemId() ) );

			UseItemRequest request = UseItemRequest.getInstance( item.getInstance( initialConsume ) );
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
				UseItemRequest request = UseItemRequest.getInstance( item.getInstance( quantity ) );
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
		boolean includeCloset =
			!KoLConstants.closet.isEmpty() &&
			Preferences.getBoolean( "autoSatisfyWithCloset" );
		boolean includeStorage =
			KoLCharacter.canInteract() &&
			!KoLConstants.storage.isEmpty() &&
			Preferences.getBoolean( "autoSatisfyWithStorage" );
		boolean includeStash =
			KoLCharacter.canInteract() &&
			Preferences.getBoolean( "autoSatisfyWithStash" ) &&
			!ClanManager.getStash().isEmpty();

		boolean includeQueue =
			!ConcoctionDatabase.queuedFoodIngredients.isEmpty() ||
			!ConcoctionDatabase.queuedBoozeIngredients.isEmpty() ||
			!ConcoctionDatabase.queuedSpleenIngredients.isEmpty();

		if ( !includeCloset && !includeStorage && !includeStash && !includeQueue )
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
		if ( flag )
		{
			++ConcoctionDatabase.refreshLevel;
		}
		else if ( ConcoctionDatabase.refreshLevel > 0 )
		{
			if ( --ConcoctionDatabase.refreshLevel == 0 )
			{
				ConcoctionDatabase.refreshConcoctions( false );
			}
		}
	}

	public static final void setRefreshNeeded( int itemId )
	{
		switch ( ItemDatabase.getConsumptionType( itemId ) )
		{
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
			ConcoctionDatabase.setRefreshNeeded( false );
			return;
		}

		switch ( itemId )
		{
		// Items that affect creatability of other items, but
		// aren't explicitly listed in their recipes:
		case ItemPool.WORTHLESS_TRINKET:
		case ItemPool.WORTHLESS_GEWGAW:
		case ItemPool.WORTHLESS_KNICK_KNACK:

		// Interchangeable ingredients, which might have been missed
		// by the getKnownUses check because the recipes are set to
		// use the other possible ingredient:
		case ItemPool.SCHLITZ:
		case ItemPool.WILLER:
		case ItemPool.KETCHUP:
		case ItemPool.CATSUP:
		case ItemPool.DYSPEPSI_COLA:
		case ItemPool.CLOACA_COLA:
		case ItemPool.TITANIUM_UMBRELLA:
		case ItemPool.GOATSKIN_UMBRELLA:
			ConcoctionDatabase.setRefreshNeeded( false );
			return;
		}

		List uses = ConcoctionDatabase.getKnownUses( itemId );

		for ( int i = 0; i < uses.size(); ++i )
		{
			AdventureResult use = (AdventureResult) uses.get( i );
			int method = ConcoctionDatabase.getMixingMethod( use.getItemId() );

			if ( ConcoctionDatabase.isPermittedMethod( method ) )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
				return;
			}
		}

		for ( int i = 0; i < CoinmasterRegistry.COINMASTERS.length; ++i )
		{
			AdventureResult item = CoinmasterRegistry.COINMASTERS[ i ].getItem();
			if ( item != null && itemId == item.getItemId() )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
				return;
			}
		}
	}

	public static final void setRefreshNeeded( boolean recalculateAdventureRange )
	{
		ConcoctionDatabase.refreshNeeded = true;

		if ( recalculateAdventureRange )
		{
			ConcoctionDatabase.recalculateAdventureRange = true;
		}
	}

	/**
	 * Returns the concoctions which are available given the list of ingredients. The list returned contains formal
	 * requests for item creation.
	 */

	public static final synchronized void refreshConcoctions( boolean force )
	{
		if ( !force && !ConcoctionDatabase.refreshNeeded )
		{
			return;
		}

		if ( ConcoctionDatabase.refreshLevel > 0 )
		{
			return;
		}

		if ( FightRequest.initializingAfterFight() )
		{
			return;
		}

		ConcoctionDatabase.refreshNeeded = false;

		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();

		// Iterate through the concoction table, Initialize each one
		// appropriately depending on whether it is an NPC item, a Coin
		// Master item, or anything else.

		boolean useNPCStores = Preferences.getBoolean( "autoSatisfyWithNPCs" );

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();

			// Initialize all the variables
			item.resetCalculations();

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
			{
				continue;
			}

			int itemId = concoction.getItemId();

			if ( itemId == ItemPool.WORTHLESS_ITEM )
			{
				item.price = useNPCStores ? InventoryManager.currentWorthlessItemCost() : 0;
				item.initial = HermitRequest.getWorthlessItemCount( true );
				item.creatable = 0;
				item.total = item.initial;
				item.visibleTotal = item.total;
				continue;
			}


			String name = concoction.getName();

			if ( useNPCStores && NPCStoreDatabase.contains( name, true ) )
			{
				if ( itemId != ItemPool.FLAT_DOUGH )
				{
					// Don't buy flat dough from Degrassi Knoll Bakery -
					// buy wads of dough for 20 meat less, instead.

					item.price = NPCStoreDatabase.price( name );
					item.initial = concoction.getCount( availableIngredients );
					item.creatable = 0;
					item.total = item.initial;
					item.visibleTotal = item.total;
					continue;
				}
			}

			PurchaseRequest purchaseRequest = item.getPurchaseRequest();
			if (  purchaseRequest != null )
			{
				purchaseRequest.setCanPurchase();
				int acquirable = purchaseRequest.canPurchase() ?
					purchaseRequest.affordableCount() : 0;
				item.price = 0;
				item.initial = concoction.getCount( availableIngredients );
				item.creatable = acquirable;
				item.total = item.initial + acquirable;
				item.visibleTotal = item.total;
				continue;
			}

			// Set initial quantity of all remaining items.

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
			item.calculate3();
		}

		// Now, to update the list of creatables without removing
		// all creatable items.	 We do this by determining the
		// number of items inside of the old list.

		boolean changeDetected = false;
		boolean considerPulls = !KoLCharacter.canInteract() &&
			!KoLCharacter.isHardcore() &&
			ConcoctionDatabase.getPullsBudgeted() > ConcoctionDatabase.queuedPullsUsed;

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
				item.setPullable( Math.min ( ar.getCount( KoLConstants.storage ) - item.queuedPulls, ConcoctionDatabase.getPullsBudgeted() - ConcoctionDatabase.queuedPullsUsed ) );
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

		ConcoctionDatabase.creatableList.updateFilter( changeDetected );
		ConcoctionDatabase.creatableList.sort();
		ConcoctionDatabase.usableList.updateFilter( changeDetected );
		ConcoctionDatabase.usableList.sort();

		if ( ConcoctionDatabase.recalculateAdventureRange )
		{
			ItemDatabase.calculateAdventureRanges();
			ConcoctionDatabase.recalculateAdventureRange = false;
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
		int toolCost = KoLCharacter.inBadMoon() ? 500 : 1000;
		boolean willBuyTool =
			KoLCharacter.getAvailableMeat() >= toolCost &&
			Preferences.getBoolean( "autoSatisfyWithNPCs" );
		boolean willBuyServant = KoLCharacter.canInteract() &&
			Preferences.getBoolean( "autoRepairBoxServants" ) &&
			( Preferences.getBoolean( "autoSatisfyWithMall" ) ||
			  Preferences.getBoolean( "autoSatisfyWithStash" ) );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		ConcoctionDatabase.adventureLimit.total = KoLCharacter.getAdventuresLeft() + ConcoctionDatabase.getFreeCraftingTurns();
		ConcoctionDatabase.adventureLimit.initial =
			ConcoctionDatabase.adventureLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
		ConcoctionDatabase.adventureLimit.creatable = 0;
		ConcoctionDatabase.adventureLimit.visibleTotal = ConcoctionDatabase.adventureLimit.total;
		
		// If we want to do turn-free crafting, we can only use free turns in lieu of adventures.
		
		ConcoctionDatabase.turnFreeLimit.total = ConcoctionDatabase.getFreeCraftingTurns();
		ConcoctionDatabase.turnFreeLimit.initial = ConcoctionDatabase.turnFreeLimit.total - ConcoctionDatabase.queuedFreeCraftingTurns;
		ConcoctionDatabase.turnFreeLimit.creatable = 0;
		ConcoctionDatabase.turnFreeLimit.visibleTotal = ConcoctionDatabase.turnFreeLimit.total;

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		ConcoctionDatabase.stillsLimit.total = KoLCharacter.getStillsAvailable();
		ConcoctionDatabase.stillsLimit.initial =
			ConcoctionDatabase.stillsLimit.total - ConcoctionDatabase.queuedStillsUsed;
		ConcoctionDatabase.stillsLimit.creatable = 0;
		ConcoctionDatabase.stillsLimit.visibleTotal = ConcoctionDatabase.stillsLimit.total;

		// Tomes are also also also considered Item #0 in the event that the
		// concoction requires a tome summon.

		ConcoctionDatabase.tomeLimit.total = 3 - Preferences.getInteger( "tomeSummons" );
		ConcoctionDatabase.tomeLimit.initial =
			ConcoctionDatabase.tomeLimit.total - ConcoctionDatabase.queuedTomesUsed;
		ConcoctionDatabase.tomeLimit.creatable = 0;
		ConcoctionDatabase.tomeLimit.visibleTotal = ConcoctionDatabase.tomeLimit.total;

		// Meat is also also considered Item #0 in the event that the
		// concoction will create paste/stacks or buy NPC items.

		ConcoctionDatabase.meatLimit.total = KoLCharacter.getAvailableMeat();
		ConcoctionDatabase.meatLimit.initial =
			ConcoctionDatabase.meatLimit.total - ConcoctionDatabase.queuedMeatSpent;
		ConcoctionDatabase.meatLimit.creatable = 0;
		ConcoctionDatabase.meatLimit.visibleTotal = ConcoctionDatabase.meatLimit.total;

		ConcoctionDatabase.calculateBasicItems( availableIngredients );

		int flags = KoLCharacter.getGender() == KoLCharacter.MALE ?
			KoLConstants.CR_MALE : KoLConstants.CR_FEMALE;
		Arrays.fill( ConcoctionDatabase.PERMIT_METHOD, false );
		Arrays.fill( ConcoctionDatabase.ADVENTURE_USAGE, 0 );
		Arrays.fill( ConcoctionDatabase.CREATION_COST, 0 );
		Arrays.fill( ConcoctionDatabase.EXCUSE, null );
		int Inigo = ConcoctionDatabase.getFreeCraftingTurns();

		// It is never possible to create items which are flagged
		// NOCREATE

		// It is always possible to create items through meat paste
		// combination.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COMBINE ] = true;
		ConcoctionDatabase.CREATION_COST[ KoLConstants.COMBINE ] = 10;

		// Un-untinkerable Amazing Ideas
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.ACOMBINE ] = true;
		ConcoctionDatabase.CREATION_COST[ KoLConstants.ACOMBINE ] = 10;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.GNOME_TINKER ] = KoLCharacter.gnomadsAvailable();
		ConcoctionDatabase.EXCUSE[ KoLConstants.GNOME_TINKER ] = "Only moxie signs can use the Supertinkerer.";

		// Smithing of items is possible whenever the person
		// has a hammer.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] = willBuyTool;
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = Math.max( 0, 1 - Inigo );

		if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) || willBuyTool )
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
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SSMITH ] = Math.max( 0, 1 - Inigo );

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.knollAvailable() )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SMITH ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.SMITH ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COMBINE ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.ACOMBINE ] = 0;
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
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.JEWELRY ] = Math.max( 0, 3 - Inigo );

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
		ConcoctionDatabase.EXCUSE[ KoLConstants.STAFF ] =
			"Only mysticality classes can make chefstaves.";

		// Phineas will make things for Seal Clubbers who have defeated
		// their Nemesis, and hence have their ULEW

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.PHINEAS ] =
			InventoryManager.hasItem( ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR );
		ConcoctionDatabase.EXCUSE[ KoLConstants.PHINEAS ] = "Only Seal Clubbers who have defeated Gorgolok can use Phineas.";

		// It's not possible to ask Uncle Crimbo 2005 to make toys
		// It's not possible to ask Ugh Crimbo 2006 to make toys
		// It's not possible to ask Uncle Crimbo 2007 to make toys

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		ConcoctionPool.get( ItemPool.CHEF ).calculate2();
		ConcoctionPool.get( ItemPool.CLOCKWORK_CHEF ).calculate2();
		ConcoctionPool.get( ItemPool.BARTENDER ).calculate2();
		ConcoctionPool.get( ItemPool.CLOCKWORK_BARTENDER ).calculate2();

		// Cooking is permitted, so long as the person has an oven or a
		// range installed in their kitchen

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK ] =
			KoLCharacter.hasOven() || KoLCharacter.hasRange();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK ] = 0;
		ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK ] = 0;
		ConcoctionDatabase.EXCUSE[ KoLConstants.COOK ] =
			"You cannot cook without an oven or a range.";

		// If we have a range and a chef installed, cooking fancy foods
		// costs no adventure. If we have no chef, cooking takes
		// adventures unless we have Inigo's active.

		// If you don't have a range, you can't cook fancy food
		// We could auto buy & install a range if the character
		// has at least 1,000 Meat and autoSatisfyWithNPCs = true
		if ( !KoLCharacter.hasRange() && !willBuyTool )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_FANCY ] = false;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.COOK_FANCY ] =
				"You cannot cook fancy foods without a range.";
		}
		// If you have (or will have) a chef, fancy cooking is free
		else if ( KoLCharacter.hasChef() || willBuyServant ||
			  ConcoctionDatabase.isAvailable( ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_FANCY ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK_FANCY ] =
				MallPriceDatabase.getPrice( ItemPool.CHEF ) / 90;
			ConcoctionDatabase.EXCUSE[ KoLConstants.COOK_FANCY ] = null;
		}
		// If we don't have a chef, Inigo's makes cooking free
/*		else if ( Inigo > 0 )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_FANCY ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.COOK_FANCY ] = null;
		}*/
		// We might not care if cooking takes adventures
		else if ( Preferences.getBoolean( "requireBoxServants" ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_FANCY ] = false;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.COOK_FANCY ] =
				"You have chosen not to cook fancy food without a chef-in-the-box.";
		}
		// Otherwise, spend those adventures!
		else
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_FANCY ] = KoLCharacter.getAdventuresLeft() + Inigo > 0;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_FANCY ] = 1;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.COOK_FANCY ] =
				"You cannot cook fancy foods without adventures.";
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

		// Mixing is permitted, so long as the person has a shaker or a
		// cocktailcrafting kit installed in their kitchen

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX ] =
			KoLCharacter.hasShaker() || KoLCharacter.hasCocktailKit();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX ] = 0;
		ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX ] = 0;
		ConcoctionDatabase.EXCUSE[ KoLConstants.MIX ] =
			"You cannot mix without a shaker or a cocktailcrafting kit.";

		// If we have a kit and a bartender installed, mixing fancy drinks
		// costs no adventure. If we have no bartender, mixing takes
		// adventures unless we have Inigo's active.

		// If you don't have a cocktailcrafting kit, you can't mix fancy drinks
		// We will auto buy & install a cocktailcrafting kit if the character
		// has at least 1,000 Meat and autoSatisfyWithNPCs = true
		if ( !KoLCharacter.hasCocktailKit() && !willBuyTool )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_FANCY ] = false;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.MIX_FANCY ] =
				"You cannot mix fancy drinks without a cocktailcrafting kit.";
		}
		// If you have (or will have) a bartender, fancy mixing is free
		else if ( KoLCharacter.hasBartender() || willBuyServant ||
			  ConcoctionDatabase.isAvailable( ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_FANCY ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX_FANCY ] =
				MallPriceDatabase.getPrice( ItemPool.BARTENDER ) / 90;
			ConcoctionDatabase.EXCUSE[ KoLConstants.MIX_FANCY ] = null;
		}
		// If we don't have a bartender, Inigo's makes mixing free
/*		else if ( Inigo > 0 )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_FANCY ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.MIX_FANCY ] = null;
		}*/
		// We might not care if mixing takes adventures
		else if ( Preferences.getBoolean( "requireBoxServants" ) )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_FANCY ] = false;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.MIX_FANCY ] =
				"You have chosen not to mix fancy drinks without a bartender-in-the-box.";
		}
		// Otherwise, spend those adventures!
		else
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_FANCY ] = KoLCharacter.getAdventuresLeft() + Inigo > 0;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_FANCY ] = 1;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.MIX_FANCY ] =
				"You cannot mix fancy drinks without adventures.";
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
		ConcoctionDatabase.EXCUSE[ KoLConstants.STILL_MIXER ] =
			ConcoctionDatabase.EXCUSE[ KoLConstants.STILL_BOOZE ] =
				KoLCharacter.isMoxieClass() ? "You have no Still uses remaining."
				: "Only moxie classes can use the Still.";

		// Summoning Clip Art is possible if the person has that tome,
		// and isn't in Bad Moon

		boolean hasClipArt = KoLCharacter.hasSkill( "Summon Clip Art" ) &&
			( !KoLCharacter.inBadMoon() || KoLCharacter.skillsRecalled() );
		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.CLIPART ] = hasClipArt &&
			Preferences.getInteger( "tomeSummons" ) < 3;
		ConcoctionDatabase.CREATION_COST[ KoLConstants.CLIPART ] =
			Preferences.getInteger( "valueOfTome" );
		ConcoctionDatabase.EXCUSE[ KoLConstants.CLIPART ] =
				hasClipArt ? "You have no Tome uses remaining."
				: "You don't have the Tome of Clip Art.";

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.WOK ] =
			KoLCharacter.canUseWok();
		ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.WOK ] = 1;
		ConcoctionDatabase.EXCUSE[ KoLConstants.WOK ] = "Only mysticality classes can use the Wok.";

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MALUS ] = KoLCharacter.canUseMalus();
		ConcoctionDatabase.EXCUSE[ KoLConstants.MALUS ] = "You require Malus access to be able to pulverize.";

		// You can make Sushi if you have a sushi-rolling mat installed
		// in your kitchen.

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.SUSHI ] = KoLCharacter.hasSushiMat();
		ConcoctionDatabase.EXCUSE[ KoLConstants.SUSHI ] = "You cannot make sushi without a sushi-rolling mat.";

		// You trade tokens to Coin Masters if you have opted in to do so,

		ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COINMASTER ] =
			Preferences.getBoolean( "autoSatisfyWithCoinmasters" );
		ConcoctionDatabase.EXCUSE[ KoLConstants.COINMASTER ] = "You have not selected the option to trade with coin masters.";

		// Other creatability flags

		if ( KoLCharacter.hasSkill( "Torso Awaregness" ) )
		{
			flags |= KoLConstants.CR_TORSO;
		}

		if ( HolidayDatabase.getHoliday().equals( "St. Sneaky Pete's Day" ) ||
		     HolidayDatabase.getHoliday().equals( "Drunksgiving" ))
		{
			flags |= KoLConstants.CR_SSPD;
		}

		if ( !KoLCharacter.inBeecore() )
		{
			flags |= KoLConstants.CR_NOBEE;
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
				if ( adv > KoLCharacter.getAdventuresLeft() + ConcoctionDatabase.getFreeCraftingTurns() )
				{
					ConcoctionDatabase.PERMIT_METHOD[ i ] = false;
					ConcoctionDatabase.EXCUSE[ i ] = "You don't have enough adventures left to create that.";
				}
				ConcoctionDatabase.CREATION_COST[ i ] += adv * value;
			}
		}

		ConcoctionDatabase.creationFlags = flags;
	}

	public static int getFreeCraftingTurns()
	{
		return ConcoctionDatabase.INIGO.getCount( KoLConstants.activeEffects ) / 5;
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
	 * Describes a method of creation in terms of the means of creation and the
	 * restrictions, if any.
	 * @param mixingMethod the method to describe
	 * @return the description
	 */
	public static String mixingMethodDescription( final int mixingMethod )
	{
		int base = mixingMethod & KoLConstants.CT_MASK;
		if ( base == KoLConstants.NOCREATE )
		{
			return "[cannot be created]";
		}

		StringBuffer result = new StringBuffer();

		String description = METHOD_DESCRIPTION[ base ];
		if ( description == null )
		{
			result.append( "[unknown method of creation]" );
		}
		else
		{
			result.append( description );
		}

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_MALE ) )
			result.append( " (males only)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_FEMALE ) )
			result.append( " (females only)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_SSPD ) )
			result.append( " (St. Sneaky Pete's Day only)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_HAMMER ) )
			result.append( " (tenderizing hammer)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_GRIMACITE ) )
			result.append( " (depleted Grimacite hammer)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_TORSO ) )
			result.append( " (Torso Awaregness)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_WEAPON ) )
			result.append( " (Super-Advanced Meatsmithing)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_ARMOR ) )
			result.append( " (Armorcraftiness)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_EXPENSIVE ) )
			result.append( " (Really Expensive Jewelrycrafting)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_REAGENT ) )
			result.append( " (Advanced Saucecrafting)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_WAY ) )
			result.append( " (The Way of Sauce)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_DEEP ) )
			result.append( " (Deep Saucery)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_PASTA ) )
			result.append( " (Pastamastery)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_TEMPURA ) )
			result.append( " (Tempuramancy)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_AC ) )
			result.append( " (Advanced Cocktailcrafting)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_SHC ) )
			result.append( " (Superhuman Cocktailcrafting)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_SALACIOUS ) )
			result.append( " (Salacious Cocktailcrafting)" );

		if ( hasCRFlag( mixingMethod, KoLConstants.CR_NOBEE ) )
			result.append( " (Unavailable in Beecore)" );

		return result.toString();
	}

	private static boolean hasCRFlag( final int mixingMethod, final int flag )
	{
		return ( mixingMethod & flag ) != 0;
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

		if ( ingredients.length > 2 )
		{	// This is not a standard crafting recipe - and in the one case
			// where such a recipe uses one of these ingredients (Sir Schlitz
			// for the Staff of the Short Order Cook), it's not interchangeable.
			return ingredients;
		}

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

	public static final int getPullsBudgeted()
	{
		return ConcoctionDatabase.pullsBudgeted;
	}

	public static int pullsBudgeted = 0;
	public static int pullsRemaining = 0;
	public static final int getPullsRemaining()
	{
		return pullsRemaining;
	}

	public static final void setPullsRemaining( final int pullsRemaining )
	{
		ConcoctionDatabase.pullsRemaining = pullsRemaining;

		if ( !StaticEntity.isHeadless() )
		{
			ItemManageFrame.updatePullsRemaining( pullsRemaining );
			CoinmastersFrame.externalUpdate();
		}

		if ( pullsRemaining < pullsBudgeted )
		{
			ConcoctionDatabase.setPullsBudgeted( pullsRemaining );
		}
	}

	public static final void setPullsBudgeted( int pullsBudgeted )
	{
		if ( pullsBudgeted < queuedPullsUsed )
		{
			pullsBudgeted = queuedPullsUsed;
		}

		if ( pullsBudgeted > pullsRemaining )
		{
			pullsBudgeted = pullsRemaining;
		}

		ConcoctionDatabase.pullsBudgeted = pullsBudgeted;

		if ( !StaticEntity.isHeadless() )
		{
			ItemManageFrame.updatePullsBudgeted( pullsBudgeted );
		}
	}
}
