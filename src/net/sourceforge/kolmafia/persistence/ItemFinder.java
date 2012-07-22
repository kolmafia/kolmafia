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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CombineMeatRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ItemFinder
{
	public static final int ANY_MATCH = 1;
	public static final int FOOD_MATCH = 2;
	public static final int BOOZE_MATCH = 3;
	public static final int USE_MATCH = 4;
	public static final int CREATE_MATCH = 5;
	public static final int UNTINKER_MATCH = 6;
	public static final int EQUIP_MATCH = 7;

	private static int matchType = ANY_MATCH;

	public static final void setMatchType( int matchType )
	{
		ItemFinder.matchType = matchType;
	}

	public static final List getMatchingNames( String searchString )
	{
		return ItemDatabase.getMatchingNames( searchString );
	}

	public static final String getFirstMatchingItemName( List nameList, String searchString )
	{
		return ItemFinder.getFirstMatchingItemName( nameList, searchString, ItemFinder.matchType );
	}

	public static final String getFirstMatchingItemName( List nameList, String searchString, int filterType )
	{
		if ( nameList == null || nameList.isEmpty() )
		{
			return null;
		}

		if ( nameList.size() == 1 )
		{

			String name = (String) nameList.get( 0 );
			return ItemDatabase.getCanonicalName( name );
		}

		// If there are multiple matches, such that one is a substring of the
		// others, choose the shorter one, on the grounds that the user would
		// have included part of the unique section of the longer name if that
		// was the item they actually intended.  This makes it easier to refer
		// to non-clockwork in-a-boxes, and DoD potions by flavor.
		while ( nameList.size() >= 2 )
		{
			String name0 = (String) nameList.get( 0 );
			String name1 = (String) nameList.get( 1 );
			if ( name0.indexOf( name1 ) != -1 )
			{
				nameList.remove( 0 );
			}
			else if ( name1.indexOf( name0 ) != -1 )
			{
				nameList.remove( 1 );
			}
			else break;
		}

		if ( nameList.size() == 1 )
		{

			String name = (String) nameList.get( 0 );
			return ItemDatabase.getCanonicalName( name );
		}

		ItemFinder.filterNameList( nameList, filterType );
		if ( nameList.isEmpty() )
		{
			return null;
		}

		// Do the shortest-substring check again, in case the filter removed
		// an item that was before or between two qualifying items.
		while ( nameList.size() >= 2 )
		{
			String name0 = (String) nameList.get( 0 );
			String name1 = (String) nameList.get( 1 );
			if ( name0.indexOf( name1 ) != -1 )
			{
				nameList.remove( 0 );
			}
			else if ( name1.indexOf( name0 ) != -1 )
			{
				nameList.remove( 1 );
			}
			else break;
		}

		// If there were no matches, or there was an exact match,
		// then return from this method.

		if ( nameList.size() == 1 )
		{
			String name = (String) nameList.get( 0 );
			return ItemDatabase.getCanonicalName( name );
		}

		// If there's only one unique item in there, return it.

		Set itemIdSet = new HashSet();

		for ( int i = 0; i < nameList.size(); ++i )
		{
			int itemId = ItemDatabase.getItemId( (String) nameList.get( i ) );

			itemIdSet.add( IntegerPool.get( itemId ) );
		}

		if ( itemIdSet.size() == 1 )
		{
			String name = (String) nameList.get( 0 );
			return ItemDatabase.getCanonicalName( name );
		}

		String itemName;
		String rv = null;

		// Candy hearts, snowcones and cupcakes take precedence over
		// all the other items in the game, IF exactly one such item
		// matches.

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			if ( !itemName.startsWith( "pix" ) && itemName.endsWith( "candy heart" ) )
			{
				if ( rv != null ) return "";
				rv = ItemDatabase.getCanonicalName( itemName );
			}
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			if ( !itemName.startsWith( "abo" ) && !itemName.startsWith( "yel" ) && itemName.endsWith( "snowcone" ) )
			{
				if ( rv != null ) return "";
				rv = ItemDatabase.getCanonicalName( itemName );
			}
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			if ( itemName.endsWith( "cupcake" ) )
			{
				if ( rv != null ) return "";
				rv = ItemDatabase.getCanonicalName( itemName );
			}
		}

		if ( rv != null ) return rv;
		return "";
	}

	private static final void filterNameList( List nameList, int filterType )
	{
		String itemName;
		int itemId, useType;

		// First, check to see if there are an HP/MP restores
		// in the list of matches.  If there are, only return
		// the restorative items (the others are irrelevant).

		ArrayList restoreList = new ArrayList();

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			itemId = ItemDatabase.getItemId( itemName );
			useType = ItemDatabase.getConsumptionType( itemId );

			switch ( useType )
			{
			case KoLConstants.HP_RESTORE:
			case KoLConstants.MP_RESTORE:
			case KoLConstants.HPMP_RESTORE:

				restoreList.add( itemName );
				break;
			}
		}

		if ( !restoreList.isEmpty() && filterType != ItemFinder.FOOD_MATCH && filterType != ItemFinder.BOOZE_MATCH )
		{
			nameList.clear();
			nameList.addAll( restoreList );
		}

		if ( nameList.size() == 1 )
		{
			return;
		}

		// Check for consumption filters when matching against the
		// item name.

		Iterator nameIterator = nameList.iterator();

		while ( nameIterator.hasNext() )
		{
			itemName = (String) nameIterator.next();
			itemId = ItemDatabase.getItemId( itemName );
			useType = ItemDatabase.getConsumptionType( itemId );

			switch ( filterType )
			{
			case ItemFinder.FOOD_MATCH:
				ItemFinder.conditionalRemove( nameIterator, useType != KoLConstants.CONSUME_EAT
					&& useType != KoLConstants.CONSUME_FOOD_HELPER );
				break;
			case ItemFinder.BOOZE_MATCH:
				ItemFinder.conditionalRemove( nameIterator, useType != KoLConstants.CONSUME_DRINK
					&& useType != KoLConstants.CONSUME_DRINK_HELPER );
				break;
			case ItemFinder.CREATE_MATCH:
				ItemFinder.conditionalRemove( nameIterator, (ConcoctionDatabase.getMixingMethod( itemName ) & KoLConstants.CT_MASK) == KoLConstants.NOCREATE && CombineMeatRequest.getCost( itemId ) == 0 );
				break;
			case ItemFinder.UNTINKER_MATCH:
				ItemFinder.conditionalRemove( nameIterator, (ConcoctionDatabase.getMixingMethod( itemId ) & KoLConstants.CT_MASK) != KoLConstants.COMBINE );
				break;
			case ItemFinder.EQUIP_MATCH:
				switch ( useType )
				{
				case KoLConstants.EQUIP_FAMILIAR:
				case KoLConstants.EQUIP_ACCESSORY:
				case KoLConstants.EQUIP_HAT:
				case KoLConstants.EQUIP_PANTS:
				case KoLConstants.EQUIP_SHIRT:
				case KoLConstants.EQUIP_WEAPON:
				case KoLConstants.EQUIP_OFFHAND:
				case KoLConstants.EQUIP_CONTAINER:
				case KoLConstants.CONSUME_STICKER:
					break;

				default:
					nameIterator.remove();
				}

				break;

			case ItemFinder.USE_MATCH:

				switch ( useType )
				{
				case KoLConstants.CONSUME_USE:
				case KoLConstants.CONSUME_MULTIPLE:
				case KoLConstants.HP_RESTORE:
				case KoLConstants.MP_RESTORE:
				case KoLConstants.HPMP_RESTORE:
				case KoLConstants.CONSUME_SPHERE:
					break;

				default:
					nameIterator.remove();
				}

				break;
			}
		}

		if ( nameList.size() == 1 )
		{
			return;
		}

		// Never match against untradeable items not available
		// in NPC stores when other items are possible.
		// This can be overridden by adding "matchable" as a secondary
		// use; this is needed for untradeables that do need to be
		// explicitly referred to, and have names similar to other items
		// (such as the NS Tower keys).

		nameIterator = nameList.iterator();

		while ( nameIterator.hasNext() )
		{
			itemName = (String) nameIterator.next();
			itemId = ItemDatabase.getItemId( itemName );

			conditionalRemove( nameIterator, itemId != -1 &&
				!ItemDatabase.getAttribute( itemId,
					ItemDatabase.ATTR_TRADEABLE | ItemDatabase.ATTR_MATCHABLE ) &&
				!NPCStoreDatabase.contains( itemName ) );
		}
	}

	private static final void conditionalRemove( Iterator iterator, boolean condition )
	{
		if ( condition )
		{
			iterator.remove();
		}
	}

	public static final AdventureResult getFirstMatchingItem( String parameters )
	{
		return ItemFinder.getFirstMatchingItem( parameters, ItemFinder.matchType );
	}

	/**
	 * Utility method which determines the first item which matches the given parameter string. Note that the string may
	 * also specify an item quantity before the string.
	 */

	public static final AdventureResult getFirstMatchingItem( String parameters, int filterType )
	{
		return ItemFinder.getFirstMatchingItem( KoLConstants.inventory, parameters, filterType, true );
	}

	public static final AdventureResult getFirstMatchingItem( String parameters, boolean errorOnFailure )
	{
		return ItemFinder.getFirstMatchingItem( KoLConstants.inventory, parameters, ItemFinder.matchType, errorOnFailure );
	}

	public static final AdventureResult getFirstMatchingItem( String parameters, int filterType, boolean errorOnFailure )
	{
		return getFirstMatchingItem( KoLConstants.inventory, parameters, filterType, errorOnFailure );
	}

	public static final AdventureResult getFirstMatchingItem( List sourceList, String parameters, int filterType, boolean errorOnFailure )
	{
		// Ignore spaces and tabs in front of the parameter string

		while ( parameters.length() > 0 && ( parameters.charAt( 0 ) == ' ' || parameters.charAt( 0 ) == '\t' ) )
		{
			parameters = parameters.substring( 1 );
		}

		// Find the item id

		int itemId = -1;
		int itemCount = 1;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		if ( parameters.charAt( 0 ) == '*' )
		{
			itemCount = 0;
			parameters = parameters.substring( 1 ).trim();
		}
		else if ( parameters.indexOf( "\u00B6" ) == -1 &&
			ItemDatabase.getItemId( parameters, 1 ) != -1 )
		{
			itemCount = 1;
		}
		else
		{
			int spaceIndex = parameters.indexOf( ' ' );

			if ( spaceIndex != -1 )
			{
				String itemCountString = parameters.substring( 0, spaceIndex );

				if ( StringUtilities.isNumeric( itemCountString ) )
				{
					itemCount = StringUtilities.parseInt( itemCountString );
					parameters = parameters.substring( spaceIndex + 1 ).trim();
				}
			}
		}

		List matchList;
		if ( parameters.startsWith( "\u00B6" ) )
		{
			matchList = new ArrayList();
			String name = ItemDatabase.getItemName(
				StringUtilities.parseInt( parameters.substring( 1 ) ) );
			if ( name != null )
			{
				matchList.add( name );
			}
		}
		else
		{
			matchList = ItemFinder.getMatchingNames( parameters );
		}

		String itemName = ItemFinder.getFirstMatchingItemName( matchList, parameters, filterType );

		if ( itemName == null )
		{
			if ( errorOnFailure )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "[" + parameters + "] has no matches." );
			}

			return null;
		}

		if ( itemName.equals( "" ) )
		{
			if ( errorOnFailure )
			{
				RequestLogger.printList( matchList );
				RequestLogger.printLine();

				KoLmafia.updateDisplay( MafiaState.ERROR, "[" + parameters + "] has too many matches." );
			}

			return null;
		}

		AdventureResult firstMatch = ItemPool.get( itemName, itemCount );

		// The result also depends on the number of items which
		// are available in the given match area.

		int matchCount;

		if ( filterType == ItemFinder.CREATE_MATCH )
		{
			boolean skipNPCs = Preferences.getBoolean( "autoSatisfyWithNPCs" )
				&& itemCount <= 0;

			if ( skipNPCs )
			{
				// Let '*' and negative counts be interpreted
				// relative to the quantity that can be created
				// with on-hand ingredients.

				Preferences.setBoolean( "autoSatisfyWithNPCs", false );
				ConcoctionDatabase.refreshConcoctions( true );
			}

			CreateItemRequest instance = CreateItemRequest.getInstance( firstMatch );
			matchCount = instance == null ? 0 : instance.getQuantityPossible();

			if ( skipNPCs )
			{
				Preferences.setBoolean( "autoSatisfyWithNPCs", true );
				ConcoctionDatabase.refreshConcoctions( true );
			}
		}
		else
		{
			matchCount = firstMatch.getCount( sourceList );
		}

		// In the event that the person wanted all except a certain
		// quantity, be sure to update the item count.

		if ( sourceList == KoLConstants.storage && KoLCharacter.canInteract() )
		{
			itemCount = matchCount;
			firstMatch = firstMatch.getInstance( itemCount );
		}
		else if ( itemCount <= 0 )
		{
			itemCount = matchCount + itemCount;
			firstMatch = firstMatch.getInstance( itemCount );
		}

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			KoLmafia.updateDisplay( firstMatch == null ? "No match" : firstMatch.toString() );
			return null;
		}

		return itemCount <= 0 ? null : firstMatch;
	}

	public static Object[] getMatchingItemList( List sourceList, String itemList )
	{
		return getMatchingItemList( sourceList, itemList, true );
	}

	public static Object[] getMatchingItemList( List sourceList, String itemList, boolean errorOnFailure )
	{
		String[] itemNames = itemList.split( "\\s*,\\s*" );

		boolean isMeatMatch = false;
		AdventureResult firstMatch = null;
		ArrayList items = new ArrayList();

		for ( int i = 0; i < itemNames.length; ++i )
		{
			isMeatMatch = false;

			if ( itemNames[ i ].endsWith( " meat" ) )
			{
				String amountString = itemNames[ i ].substring( 0, itemNames[ i ].length() - 5 ).trim();

				if ( amountString.equals( "*" ) || StringUtilities.isNumeric( amountString ) )
				{
					isMeatMatch = true;

					int amount = 0;

					if ( !amountString.equals( "*" ) )
					{
						amount = StringUtilities.parseInt( amountString );
					}

					if ( amount <= 0 )
					{
						amount += sourceList == KoLConstants.storage ? KoLCharacter.getStorageMeat() :
							sourceList == KoLConstants.closet ? KoLCharacter.getClosetMeat() : KoLCharacter.getAvailableMeat();
					}

					firstMatch = new AdventureResult( AdventureResult.MEAT, amount );
				}
			}

			if ( !isMeatMatch )
			{
				firstMatch = ItemFinder.getFirstMatchingItem( sourceList, itemNames[ i ], ItemFinder.matchType, errorOnFailure );
			}

			if ( firstMatch != null )
			{
				AdventureResult.addResultToList( items, firstMatch );
			}
		}

		return items.toArray();
	}

}
