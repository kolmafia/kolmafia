/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
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

	private static int matchType = ANY_MATCH;

	public static final void setMatchType( int matchType )
	{
		ItemFinder.matchType = matchType;
	}

	public static final List getMatchingNames( String searchString )
	{
		return ItemDatabase.getMatchingNames( searchString );
	}

	public static final int getFirstMatchingItemId( List nameList, String searchString )
	{
		return ItemFinder.getFirstMatchingItemId( nameList, searchString, ItemFinder.matchType );
	}

	public static final int getFirstMatchingItemId( List nameList, String searchString, int filterType )
	{
		if ( nameList == null || nameList.isEmpty() )
		{
			return -1;
		}

		if ( nameList.size() == 1 )
		{
			return ItemDatabase.getItemId( (String) nameList.get( 0 ) );
		}

		ItemFinder.filterNameList( nameList, filterType );

		// If there were no matches, or there was an exact match,
		// then return from this method.

		if ( nameList.size() == 1 )
		{
			return ItemDatabase.getItemId( (String) nameList.get( 0 ) );
		}

		String itemName;

		// Candy hearts, snowcones and cupcakes take precedence over
		// all the other items in the game.

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			if ( !itemName.startsWith( "pix" ) && itemName.endsWith( "candy heart" ) )
			{
				return ItemDatabase.getItemId( itemName );
			}
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			if ( !itemName.startsWith( "abo" ) && !itemName.startsWith( "yel" ) && itemName.endsWith( "snowcone" ) )
			{
				return ItemDatabase.getItemId( itemName );
			}
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get( i );
			if ( itemName.endsWith( "cupcake" ) )
			{
				return ItemDatabase.getItemId( itemName );
			}
		}

		return 0;
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
				ItemFinder.conditionalRemove( nameIterator, ConcoctionDatabase.getMixingMethod( itemId ) == KoLConstants.NOCREATE );
				break;
			case ItemFinder.UNTINKER_MATCH:
				ItemFinder.conditionalRemove( nameIterator, ConcoctionDatabase.getMixingMethod( itemId ) != KoLConstants.COMBINE );
				break;

			case ItemFinder.USE_MATCH:

				switch ( useType )
				{
				case KoLConstants.CONSUME_USE:
				case KoLConstants.CONSUME_MULTIPLE:
				case KoLConstants.HP_RESTORE:
				case KoLConstants.MP_RESTORE:
				case KoLConstants.HPMP_RESTORE:
				case KoLConstants.CONSUME_SPECIAL:
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

		nameIterator = nameList.iterator();

		while ( nameIterator.hasNext() )
		{
			itemName = (String) nameIterator.next();
			itemId = ItemDatabase.getItemId( itemName );

			conditionalRemove( nameIterator,
				!ItemDatabase.isTradeable( itemId ) && !NPCStoreDatabase.contains( itemName ) );
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
		int itemId = -1;
		int itemCount = 1;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		if ( parameters.indexOf( " " ) != -1 )
		{
			if ( parameters.charAt( 0 ) == '*' )
			{
				itemCount = 0;
				parameters = parameters.substring( 1 ).trim();
			}
			else if ( ItemDatabase.getItemId( parameters, 1 ) != -1 )
			{
				itemCount = 1;
			}
			else
			{
				boolean isNumeric = parameters.charAt( 0 ) == '-' || Character.isDigit( parameters.charAt( 0 ) );

				int spaceIndex = 1;
				while ( isNumeric && spaceIndex < parameters.length() && parameters.charAt( spaceIndex ) != ' ' )
				{
					isNumeric = Character.isDigit( parameters.charAt( spaceIndex++ ) );
				}

				if ( isNumeric )
				{
					itemCount = StringUtilities.parseInt( parameters.substring( 0, spaceIndex ) );
					parameters = parameters.substring( spaceIndex + 1 ).trim();
				}
			}
		}

		List matchList = ItemFinder.getMatchingNames( parameters );
		itemId = ItemFinder.getFirstMatchingItemId( matchList, parameters, filterType );

		if ( itemId == 0 )
		{
			if ( errorOnFailure )
			{
				RequestLogger.printList( matchList );
				RequestLogger.printLine();

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + parameters + "] has too many matches." );
			}

			return null;
		}

		if ( itemId == -1 )
		{
			if ( errorOnFailure )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + parameters + "] has no matches." );
			}

			return null;
		}

		AdventureResult firstMatch = ItemPool.get( itemId, itemCount );

		// The result also depends on the number of items which
		// are available in the given match area.

		int matchCount;

		if ( filterType == ItemFinder.CREATE_MATCH )
		{
			boolean includeNPCs = Preferences.getBoolean( "autoSatisfyWithNPCs" );

			if ( includeNPCs )
			{
				Preferences.setBoolean( "autoSatisfyWithNPCs", false );
				ConcoctionDatabase.refreshConcoctions();
			}

			CreateItemRequest instance = CreateItemRequest.getInstance( firstMatch.getItemId() );
			matchCount = instance == null ? 0 : instance.getQuantityPossible();

			if ( includeNPCs )
			{
				Preferences.setBoolean( "autoSatisfyWithNPCs", true );
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
		String[] itemNames = itemList.split( "\\s*,\\s*" );

		boolean isMeatMatch = false;
		AdventureResult firstMatch = null;
		ArrayList items = new ArrayList();

		for ( int i = 0; i < itemNames.length; ++i )
		{
			isMeatMatch = false;

			if ( itemNames[ i ].endsWith( "meat" ) )
			{
				String amountString = itemNames[ i ].split( " " )[ 0 ];
				char first = amountString.charAt( 0 );
				isMeatMatch = first == '-' || first == '*' || Character.isDigit( first );

				for ( int j = 1; j < amountString.length() && isMeatMatch; ++j )
				{
					isMeatMatch &= Character.isDigit( amountString.charAt( j ) );
				}

				if ( isMeatMatch )
				{
					int amount = StringUtilities.parseInt( amountString );

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
				firstMatch = ItemFinder.getFirstMatchingItem( sourceList, itemNames[ i ], ItemFinder.matchType, true );
			}

			if ( firstMatch != null )
			{
				AdventureResult.addResultToList( items, firstMatch );
			}
		}

		return items.toArray();
	}

}
