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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.UtilityConstants;

public class TradeableItemDatabase
	extends KoLDatabase
{
	public static final AdventureResult ODE = new AdventureResult( "Ode to Booze", 1, true );
	public static final AdventureResult GOT_MILK = new AdventureResult( "Got Milk", 1, true );

	private static final Pattern WIKI_ITEMID_PATTERN = Pattern.compile( "Item number</a>:</b> (\\d+)<br />" );
	private static final Pattern WIKI_DESCID_PATTERN = Pattern.compile( "<b>Description ID:</b> (\\d+)<br />" );
	private static final Pattern WIKI_PLURAL_PATTERN =
		Pattern.compile( "\\(.*?In-game plural</a>: <i>(.*?)</i>\\)", Pattern.DOTALL );
	private static final Pattern WIKI_AUTOSELL_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	private static int maxItemId = 0;

	private static final IntegerArray useTypeById = new IntegerArray();
	private static final IntegerArray priceById = new IntegerArray();
	private static final StringArray pluralById = new StringArray();

	private static final Map nameById = new TreeMap();
	private static final Map dataNameById = new TreeMap();
	private static final Map descriptionById = new TreeMap();
	private static final Map itemIdByName = new TreeMap();
	private static final Map itemIdByPlural = new TreeMap();

	private static final Map itemIdByDescription = new TreeMap();

	private static final Map levelReqByName = new TreeMap();
	private static final Map fullnessByName = new TreeMap();
	private static final Map inebrietyByName = new TreeMap();
	private static final Map spleenHitByName = new TreeMap();

	private static final Map[][][][] advsByName = new TreeMap[ 2 ][ 2 ][ 2 ][ 2 ];

	static
	{
		TradeableItemDatabase.advsByName[ 0 ][ 0 ][ 0 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 0 ][ 0 ][ 0 ][ 1 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 0 ][ 0 ][ 1 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 0 ][ 0 ][ 1 ][ 1 ] = new TreeMap();

		TradeableItemDatabase.advsByName[ 0 ][ 1 ][ 0 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 0 ][ 1 ][ 0 ][ 1 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 0 ][ 1 ][ 1 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 0 ][ 1 ][ 1 ][ 1 ] = new TreeMap();

		TradeableItemDatabase.advsByName[ 1 ][ 0 ][ 0 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 1 ][ 0 ][ 0 ][ 1 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 1 ][ 0 ][ 1 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 1 ][ 0 ][ 1 ][ 1 ] = new TreeMap();

		TradeableItemDatabase.advsByName[ 1 ][ 1 ][ 0 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 1 ][ 1 ][ 0 ][ 1 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 1 ][ 1 ][ 1 ][ 0 ] = new TreeMap();
		TradeableItemDatabase.advsByName[ 1 ][ 1 ][ 1 ][ 1 ] = new TreeMap();
	}

	private static final Map muscleByName = new TreeMap();
	private static final Map mysticalityByName = new TreeMap();
	private static final Map moxieByName = new TreeMap();

	private static final Map accessById = new TreeMap();
	private static final BooleanArray tradeableById = new BooleanArray();
	private static final BooleanArray giftableById = new BooleanArray();
	private static final BooleanArray displayableById = new BooleanArray();

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = KoLDatabase.getVersionedReader( "tradeitems.txt", KoLConstants.TRADEITEMS_VERSION );

		String[] data;
		Integer id;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length == 5 )
			{
				int itemId = StaticEntity.parseInt( data[ 0 ] );
				id = new Integer( itemId );

				TradeableItemDatabase.useTypeById.set( itemId, StaticEntity.parseInt( data[ 2 ] ) );
				TradeableItemDatabase.priceById.set( itemId, StaticEntity.parseInt( data[ 4 ] ) );

				TradeableItemDatabase.itemIdByName.put( KoLDatabase.getCanonicalName( data[ 1 ] ), id );
				TradeableItemDatabase.dataNameById.put( id, data[ 1 ] );
				TradeableItemDatabase.nameById.put( id, KoLDatabase.getDisplayName( data[ 1 ] ) );

				TradeableItemDatabase.accessById.put( id, data[ 3 ] );
				TradeableItemDatabase.tradeableById.set( itemId, data[ 3 ].equals( "all" ) );
				TradeableItemDatabase.giftableById.set( itemId, data[ 3 ].equals( "all" ) || data[ 3 ].equals( "gift" ) );
				TradeableItemDatabase.displayableById.set(
					itemId, data[ 3 ].equals( "all" ) || data[ 3 ].equals( "gift" ) || data[ 3 ].equals( "display" ) );

				if ( itemId > TradeableItemDatabase.maxItemId )
				{
					TradeableItemDatabase.maxItemId = itemId;
				}
			}
		}

		// Add in dummy information for tracking worthless
		// items so they can be added as conditions.

		id = new Integer( 13 );
		TradeableItemDatabase.dataNameById.put( id, "worthless item" );
		TradeableItemDatabase.itemIdByName.put( "worthless item", id );
		TradeableItemDatabase.nameById.put( id, "worthless item" );

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

		// Next, retrieve the description Ids.

		reader = KoLDatabase.getVersionedReader( "itemdescs.txt", KoLConstants.ITEMDESCS_VERSION );

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			boolean isDescriptionId = true;
			if ( data.length >= 2 && data[ 1 ].length() > 0 )
			{
				isDescriptionId = true;
				for ( int i = 0; i < data[ 1 ].length() && isDescriptionId; ++i )
				{
					if ( !Character.isDigit( data[ 1 ].charAt( i ) ) )
					{
						isDescriptionId = false;
					}
				}

				if ( isDescriptionId )
				{
					int itemId = StaticEntity.parseInt( data[ 0 ].trim() );
					id = new Integer( itemId );
					TradeableItemDatabase.descriptionById.put( id, data[ 1 ] );
					TradeableItemDatabase.itemIdByDescription.put( data[ 1 ], new Integer( itemId ) );

					if ( data.length == 4 )
					{
						TradeableItemDatabase.pluralById.set( itemId, data[ 3 ] );
						TradeableItemDatabase.itemIdByPlural.put( KoLDatabase.getCanonicalName( data[ 3 ] ), id );
					}
				}
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

		// Next, retrieve the table of fullness

		reader = KoLDatabase.getVersionedReader( "fullness.txt", KoLConstants.FULLNESS_VERSION );

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				String name = KoLDatabase.getCanonicalName( data[ 0 ] );
				TradeableItemDatabase.fullnessByName.put( name, Integer.valueOf( data[ 1 ] ) );
				TradeableItemDatabase.levelReqByName.put( name, Integer.valueOf( data[ 2 ] ) );

				if ( data.length > 3 )
				{
					TradeableItemDatabase.addAdventureRange( name, StaticEntity.parseInt( data[ 1 ] ), data[ 3 ] );
					TradeableItemDatabase.muscleByName.put( name, TradeableItemDatabase.extractRange( data[ 4 ] ) );
					TradeableItemDatabase.mysticalityByName.put( name, TradeableItemDatabase.extractRange( data[ 5 ] ) );
					TradeableItemDatabase.moxieByName.put( name, TradeableItemDatabase.extractRange( data[ 6 ] ) );
				}
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

		// Next, retrieve the table of inebriety

		reader = KoLDatabase.getVersionedReader( "inebriety.txt", KoLConstants.INEBRIETY_VERSION );

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				String name = KoLDatabase.getCanonicalName( data[ 0 ] );
				TradeableItemDatabase.inebrietyByName.put( name, Integer.valueOf( data[ 1 ] ) );
				TradeableItemDatabase.levelReqByName.put( name, Integer.valueOf( data[ 2 ] ) );

				if ( data.length > 3 )
				{
					TradeableItemDatabase.addAdventureRange( name, StaticEntity.parseInt( data[ 1 ] ), data[ 3 ] );
					TradeableItemDatabase.muscleByName.put( name, TradeableItemDatabase.extractRange( data[ 4 ] ) );
					TradeableItemDatabase.mysticalityByName.put( name, TradeableItemDatabase.extractRange( data[ 5 ] ) );
					TradeableItemDatabase.moxieByName.put( name, TradeableItemDatabase.extractRange( data[ 6 ] ) );
				}
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

		// Next, retrieve the table of spleen hits

		reader = KoLDatabase.getVersionedReader( "spleenhit.txt", KoLConstants.SPLEENHIT_VERSION );

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length == 2 )
			{
				String name = KoLDatabase.getCanonicalName( data[ 0 ] );
				TradeableItemDatabase.spleenHitByName.put( name, Integer.valueOf( data[ 1 ] ) );
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

	private static final int getIncreasingGains( final int value )
	{
		// Adventure gains from Ode/Milk based on information
		// derived by Istari Asuka on the Hardcore Oxygenation forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=2321

		switch ( value )
		{
		case 0:
			return 0;

		case 1:
		case 2:
		case 3:
		case 4:
			return 1;

		case 5:
		case 6:
		case 7:
			return 2;

		case 8:
		case 9:
		case 10:
			return 3;

		default:
			return 4;
		}
	}

	private static final int getDecreasingGains( final int value )
	{
		// Adventure gains from Ode/Milk based on information
		// derived by Istari Asuka on the Hardcore Oxygenation forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=2321

		switch ( value )
		{
		case 0:
			return 0;

		case 1:
		case 2:
		case 3:
			return 3;

		case 4:
		case 5:
		case 6:
			return 2;

		default:
			return 1;
		}
	}

	private static final void addAdventureRange( final String name, final int unitCost, String range )
	{
		range = range.trim();

		int dashIndex = range.indexOf( "-" );
		int start = StaticEntity.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );
		int end = dashIndex == -1 ? start : StaticEntity.parseInt( range.substring( dashIndex + 1 ) );

		int gainSum1 = 0;
		int gainSum2 = 0;
		int gainSum3 = 0;

		for ( int i = start; i <= end; ++i )
		{
			gainSum1 += i + TradeableItemDatabase.getIncreasingGains( i );
			gainSum2 += i + TradeableItemDatabase.getDecreasingGains( i );
			gainSum3 +=
				i + TradeableItemDatabase.getIncreasingGains( i + TradeableItemDatabase.getDecreasingGains( i ) );
		}

		float count = end - start + 1;

		TradeableItemDatabase.addAdventureRange( name, unitCost, false, false, ( start + end ) / 2.0f );
		TradeableItemDatabase.addAdventureRange( name, unitCost, true, false, gainSum1 / count );
		TradeableItemDatabase.addAdventureRange( name, unitCost, false, true, gainSum2 / count );
		TradeableItemDatabase.addAdventureRange( name, unitCost, true, true, gainSum3 / count );
	}

	private static final void addAdventureRange( final String name, final int unitCost, final boolean gainEffect1,
		final boolean gainEffect2, final float result )
	{
		// Adventure gains from zodiac signs based on information
		// provided on the Iocaine Powder forums.
		// http://www.iocainepowder.org/forums/viewtopic.php?t=2742

		TradeableItemDatabase.getAdventureMap( false, false, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result ) );
		TradeableItemDatabase.getAdventureMap( false, true, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result * 1.1f ) );

		TradeableItemDatabase.getAdventureMap( true, false, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result / unitCost ) );
		TradeableItemDatabase.getAdventureMap( true, true, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result * 1.1f / unitCost ) );
	}

	private static final Map getAdventureMap( final boolean perUnit, final boolean gainZodiac,
		final boolean gainEffect1, final boolean gainEffect2 )
	{
		return TradeableItemDatabase.advsByName[ perUnit ? 1 : 0 ][ gainZodiac ? 1 : 0 ][ gainEffect1 ? 1 : 0 ][ gainEffect2 ? 1 : 0 ];
	}

	private static final String extractRange( String range )
	{
		range = range.trim();

		boolean isNegative = range.startsWith( "-" );
		if ( range.startsWith( "-" ) )
		{
			isNegative = true;
			range = range.substring( 1 );
		}

		int dashIndex = range.indexOf( "-" );
		int start = StaticEntity.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );

		if ( dashIndex == -1 )
		{
			return KoLConstants.SINGLE_PRECISION_FORMAT.format( isNegative ? 0 - start : start );
		}

		int end = StaticEntity.parseInt( range.substring( dashIndex + 1 ) );
		return KoLConstants.SINGLE_PRECISION_FORMAT.format( ( start + end ) / ( isNegative ? -2.0f : 2.0f ) );
	}

	/**
	 * Takes an item name and constructs the likely Wiki equivalent of that item name.
	 */

	private static final String constructWikiName( String name )
	{
		name = StaticEntity.globalStringReplace( KoLDatabase.getDisplayName( name ), " ", "_" );
		return Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
	}

	private static final String readWikiData( final String name )
	{
		String line = null;
		StringBuffer wikiRecord = new StringBuffer();

		try
		{
			BufferedReader reader =
				KoLDatabase.getReader( "http://kol.coldfront.net/thekolwiki/index.php/" + TradeableItemDatabase.constructWikiName( name ) );
			while ( ( line = reader.readLine() ) != null )
			{
				wikiRecord.append( line );
			}
			reader.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return wikiRecord.toString();
	}

	/**
	 * Utility method which searches for the plural version of the item on the KoL wiki.
	 */

	public static final void determineWikiData( final String name )
	{
		String wikiData = TradeableItemDatabase.readWikiData( name );

		Matcher itemMatcher = TradeableItemDatabase.WIKI_ITEMID_PATTERN.matcher( wikiData );
		if ( !itemMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		Matcher descMatcher = TradeableItemDatabase.WIKI_DESCID_PATTERN.matcher( wikiData );
		if ( !descMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		RequestLogger.printLine( "item: " + name + " (#" + itemMatcher.group( 1 ) + ")" );
		RequestLogger.printLine( "desc: " + descMatcher.group( 1 ) );

		Matcher pluralMatcher = TradeableItemDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
		if ( pluralMatcher.find() )
		{
			RequestLogger.printLine( "plural: " + pluralMatcher.group( 1 ) );
		}

		Matcher sellMatcher = TradeableItemDatabase.WIKI_AUTOSELL_PATTERN.matcher( wikiData );
		if ( sellMatcher.find() )
		{
			RequestLogger.printLine( "autosell: " + sellMatcher.group( 1 ) );
		}
	}

	/**
	 * Temporarily adds an item to the item database. This is used whenever KoLmafia encounters an unknown item in the
	 * mall or in the player's inventory.
	 */

	public static final void registerItem( final int itemId, final String itemName )
	{
		TradeableItemDatabase.registerItem( itemId, itemName, "" );
	}

	public static final void registerItem( final int itemId, final String itemName, final String descriptionId )
	{
		if ( itemName == null )
		{
			return;
		}

		RequestLogger.printLine( "Unknown item found: " + itemName + " (#" + itemId + ")" );

		TradeableItemDatabase.useTypeById.set( itemId, 0 );
		TradeableItemDatabase.priceById.set( itemId, -1 );

		Integer id = new Integer( itemId );

		TradeableItemDatabase.descriptionById.put( id, descriptionId );
		TradeableItemDatabase.itemIdByName.put( KoLDatabase.getCanonicalName( itemName ), id );
		TradeableItemDatabase.dataNameById.put( id, itemName );
		TradeableItemDatabase.nameById.put( id, KoLDatabase.getDisplayName( itemName ) );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName )
	{
		return TradeableItemDatabase.getItemId( itemName, 1 );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @param count How many there are
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName, final int count )
	{
		if ( itemName == null || itemName.length() == 0 )
		{
			return -1;
		}

		// Get the canonical name of the item, and attempt
		// to parse based on that.

		String canonicalName = KoLDatabase.getCanonicalName( itemName );
		Object itemId = TradeableItemDatabase.itemIdByName.get( canonicalName );

		// If the name, as-is, exists in the item database,
		// then go ahead and return the item Id.

		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Work around a specific KoL bug: the "less-than-three-shaped
		// box" is sometimes listed as a "less-than-three- shaped box"
		if ( canonicalName.equals( "less-than-three- shaped box" ) )
		{
			return 1168;
		}

		// See if it's a weird pluralization with a pattern we can't
		// guess.

		itemId = TradeableItemDatabase.itemIdByPlural.get( canonicalName );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// It's possible that you're looking for a substring.  In
		// that case, prefer complete versions containing the substring
		// over truncated versions which are plurals.

		List possibilities = KoLDatabase.getMatchingNames( TradeableItemDatabase.itemIdByName, canonicalName );
		if ( possibilities.size() == 1 )
		{
			return TradeableItemDatabase.getItemId( (String) possibilities.get( 0 ), count );
		}

		// Abort if it's clearly not going to be a plural,
		// since this might kill off multi-item detection.

		if ( count < 2 )
		{
			return -1;
		}

		// Or maybe it's a standard plural where they just add a letter
		// to the end.

		itemId = TradeableItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a snowcone, then reverse the word order
		if ( canonicalName.startsWith( "snowcones" ) )
		{
			return TradeableItemDatabase.getItemId( canonicalName.split( " " )[ 1 ] + " snowcone", count );
		}

		// Lo mein has this odd pluralization where there's a dash
		// introduced into the name when no such dash exists in the
		// singular form.

		itemId = TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "-", " " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// The word right before the dash may also be pluralized,
		// so make sure the dashed words are recognized.

		itemId = TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "es-", "-" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		itemId = TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "s-", "-" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a plural form of "tooth", then make
		// sure that it's handled.  Other things which
		// also have "ee" plural forms should be clumped
		// in as well.

		itemId = TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ee", "oo" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also handle the plural of vortex, which is
		// "vortices" -- this should only appear in the
		// meat vortex, but better safe than sorry.

		itemId =
			TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ex" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Handling of appendices (which is the plural
		// of appendix, not appendex, so it is not caught
		// by the previous test).

		itemId =
			TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ix" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also add in a special handling for knives
		// and other things ending in "ife".

		itemId =
			TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ives", "ife" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also add in a special handling for elves
		// and other things ending in "f".

		itemId = TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ves", "f" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also add in a special handling for staves
		// and other things ending in "aff".

		itemId =
			TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "aves", "aff" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a pluralized form of something that
		// ends with "y", then return the appropriate
		// item Id for the "y" version.

		if ( canonicalName.endsWith( "ies" ) )
		{
			itemId =
				TradeableItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 3 ) + "y" );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		itemId =
			TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ies ", "y " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a pluralized form of something that
		// ends with "o", then return the appropriate
		// item Id for the "o" version.

		if ( canonicalName.endsWith( "es" ) )
		{
			itemId = TradeableItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 2 ) );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		itemId = TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "es ", " " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a pluralized form of something that
		// ends with "an", then return the appropriate
		// item Id for the "en" version.

		itemId =
			TradeableItemDatabase.itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "en ", "an " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a standard pluralized forms, then
		// return the appropriate item Id.

		itemId = TradeableItemDatabase.itemIdByName.get( canonicalName.replaceFirst( "([A-Za-z])s ", "$1 " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's something that ends with 'i', then
		// it might be a singular ending with 'us'.

		if ( canonicalName.endsWith( "i" ) )
		{
			itemId =
				TradeableItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) + "us" );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		// Attempt to find the item name by brute force
		// by checking every single space location.  Do
		// this recursively for best results.

		// int lastSpaceIndex = canonicalName.indexOf( " " );
		// return lastSpaceIndex != -1 ? getItemId( canonicalName.substring( lastSpaceIndex ).trim(), count ) : -1;

		// Unknown item
		return -1;
	}

	public static final boolean meetsLevelRequirement( final String name )
	{
		if ( name == null )
		{
			return false;
		}

		Integer requirement = (Integer) TradeableItemDatabase.levelReqByName.get( KoLDatabase.getCanonicalName( name ) );
		return requirement == null ? false : KoLCharacter.getLevel() >= requirement.intValue();
	}

	public static final int getFullness( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer fullness = (Integer) TradeableItemDatabase.fullnessByName.get( KoLDatabase.getCanonicalName( name ) );
		return fullness == null ? 0 : fullness.intValue();
	}

	public static final int getInebriety( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer inebriety = (Integer) TradeableItemDatabase.inebrietyByName.get( KoLDatabase.getCanonicalName( name ) );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	public static final int getSpleenHit( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer spleenhit = (Integer) TradeableItemDatabase.spleenHitByName.get( KoLDatabase.getCanonicalName( name ) );
		return spleenhit == null ? 0 : spleenhit.intValue();
	}

	public static final String getAdventureRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		int fullness = TradeableItemDatabase.getFullness( name );
		int inebriety = TradeableItemDatabase.getInebriety( name );

		String range = null;

		if ( fullness > 0 )
		{
			range =
				(String) TradeableItemDatabase.getAdventureMap(
					KoLSettings.getBooleanProperty( "showGainsPerUnit" ),
					KoLCharacter.getSign().indexOf( "Opossum" ) != -1,
					KoLConstants.activeEffects.contains( TradeableItemDatabase.GOT_MILK ),
					KoLSettings.getIntegerProperty( "munchiesPillsUsed" ) > 0 ).get(
					KoLDatabase.getCanonicalName( name ) );
		}
		else if ( inebriety > 0 )
		{
			range =
				(String) TradeableItemDatabase.getAdventureMap(
					KoLSettings.getBooleanProperty( "showGainsPerUnit" ),
					KoLCharacter.getSign().indexOf( "Blender" ) != -1,
					KoLConstants.activeEffects.contains( TradeableItemDatabase.ODE ), false ).get(
					KoLDatabase.getCanonicalName( name ) );
		}

		if ( range == null )
		{
			return "+0.0";
		}

		return range;
	}

	public static final String getMuscleRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) TradeableItemDatabase.muscleByName.get( KoLDatabase.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMysticalityRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) TradeableItemDatabase.mysticalityByName.get( KoLDatabase.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMoxieRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) TradeableItemDatabase.moxieByName.get( KoLDatabase.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	/**
	 * Returns the price for the item with the given Id.
	 *
	 * @return The price associated with the item
	 */

	public static final int getPriceById( final int itemId )
	{
		return TradeableItemDatabase.priceById.get( itemId );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isTradeable( final int itemId )
	{
		return TradeableItemDatabase.tradeableById.get( itemId );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isGiftable( final int itemId )
	{
		return TradeableItemDatabase.giftableById.get( itemId );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isDisplayable( final int itemId )
	{
		return TradeableItemDatabase.displayableById.get( itemId );
	}

	/**
	 * Returns true if the item is a bounty, otherwise false
	 *
	 * @return true if item is a bounty
	 */

	public static final boolean isBountyItem( final int itemId )
	{
		return itemId >= 2099 && itemId <= 2107 || itemId >= 2407 && itemId <= 2415 || itemId >= 2468 && itemId <= 2473;
	}

	/**
	 * Returns the name for an item, given its Id number.
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The name of the corresponding item
	 */

	public static final String getItemName( final int itemId )
	{
		return (String) TradeableItemDatabase.nameById.get( new Integer( itemId ) );
	}

	/**
	 * Returns the name for an item, given its Id number.
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The name of the corresponding item
	 */

	public static final String getItemName( final String descriptionId )
	{
		Integer itemId = (Integer) TradeableItemDatabase.itemIdByDescription.get( descriptionId );
		return itemId == null ? null : TradeableItemDatabase.getItemName( itemId.intValue() );
	}

	/**
	 * Returns the id for an item, given its description id number.
	 *
	 * @param itemId The description id number of the item to lookup
	 * @return The item id of the corresponding item
	 */

	public static final int getItemIdFromDescription( final String descriptionId )
	{
		Integer itemId = (Integer) TradeableItemDatabase.itemIdByDescription.get( descriptionId );
		return itemId == null ? -1 : itemId.intValue();
	}

	/**
	 * Returns a list of all items which contain the given substring. This is useful for people who are doing lookups on
	 * items.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return KoLDatabase.getMatchingNames( TradeableItemDatabase.itemIdByName, substring );
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String itemName )
	{
		return TradeableItemDatabase.getItemId( itemName ) != -1;
	}

	/**
	 * Returns whether or not the item with the given name is usable (this includes edibility).
	 *
	 * @return <code>true</code> if the item is usable
	 */

	public static final boolean isUsable( final String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );
		if ( itemId <= 0 )
		{
			return false;
		}

		switch ( TradeableItemDatabase.useTypeById.get( itemId ) )
		{
		case CONSUME_EAT:
		case CONSUME_DRINK:
		case CONSUME_USE:
		case MESSAGE_DISPLAY:
		case INFINITE_USES:
		case CONSUME_MULTIPLE:
		case GROW_FAMILIAR:
		case CONSUME_ZAP:
		case MP_RESTORE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the kind of consumption associated with an item
	 *
	 * @return The consumption associated with the item
	 */

	public static final int getConsumptionType( final int itemId )
	{
		return itemId <= 0 ? KoLConstants.NO_CONSUME : TradeableItemDatabase.useTypeById.get( itemId );
	}

	public static final int getConsumptionType( final String itemName )
	{
		return TradeableItemDatabase.getConsumptionType( TradeableItemDatabase.getItemId( itemName ) );
	}

	/**
	 * Returns the item description Id used by the given item, given its item Id.
	 *
	 * @return The description Id associated with the item
	 */

	public static final String getDescriptionId( final int itemId )
	{
		return (String) TradeableItemDatabase.descriptionById.get( new Integer( itemId ) );
	}

	/**
	 * Returns the set of item names keyed by id
	 *
	 * @return The set of item names keyed by id
	 */

	public static final Set entrySet()
	{
		return TradeableItemDatabase.nameById.entrySet();
	}

	/**
	 * Returns the largest item ID
	 *
	 * @return The largest item ID
	 */

	public static final int maxItemId()
	{
		return TradeableItemDatabase.maxItemId;
	}

	private static final Pattern CLOSET_ITEM_PATTERN =
		Pattern.compile( "<option value='(\\d+)' descid='(.*?)'>(.*?) \\(" );
	private static final Pattern DESCRIPTION_PATTERN =
		Pattern.compile( "onClick='descitem\\((\\d+)\\);'></td><td valign=top><b>(.*?)</b>" );

	public static final void findItemDescriptions()
	{
		RequestLogger.printLine( "Checking for new non-quest items..." );

		KoLRequest updateRequest = new EquipmentRequest( EquipmentRequest.CLOSET );
		RequestThread.postRequest( updateRequest );
		Matcher itemMatcher = TradeableItemDatabase.CLOSET_ITEM_PATTERN.matcher( updateRequest.responseText );

		boolean foundChanges = false;

		while ( itemMatcher.find() )
		{
			int itemId = StaticEntity.parseInt( itemMatcher.group( 1 ) );
			if ( !TradeableItemDatabase.descriptionById.get( new Integer( itemId ) ).equals( "" ) )
			{
				continue;
			}

			foundChanges = true;
			TradeableItemDatabase.registerItem( itemId, itemMatcher.group( 3 ), itemMatcher.group( 2 ) );
		}

		RequestLogger.printLine( "Parsing for quest items..." );
		KoLRequest itemChecker = new KoLRequest( "inventory.php?which=3" );

		RequestThread.postRequest( itemChecker );
		itemMatcher = TradeableItemDatabase.DESCRIPTION_PATTERN.matcher( itemChecker.responseText );

		while ( itemMatcher.find() )
		{
			String itemName = itemMatcher.group( 2 );
			int itemId = TradeableItemDatabase.getItemId( itemName );

			if ( itemId == -1 )
			{
				continue;
			}

			if ( !TradeableItemDatabase.descriptionById.get( new Integer( itemId ) ).equals( "" ) )
			{
				continue;
			}

			foundChanges = true;
			TradeableItemDatabase.registerItem( itemId, itemName, itemMatcher.group( 1 ) );
		}

		if ( foundChanges )
		{
			TradeableItemDatabase.saveDataOverride();
		}
	}

	public static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "tradeitems.txt" );
		LogStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.TRADEITEMS_VERSION );

		int lastInteger = 1;
		Iterator it = TradeableItemDatabase.nameById.keySet().iterator();

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
			{
				writer.println( i );
			}

			lastInteger = nextInteger.intValue() + 1;

			if ( TradeableItemDatabase.accessById.containsKey( nextInteger ) )
			{
				writer.println( nextInteger + "\t" + TradeableItemDatabase.dataNameById.get( nextInteger ) + "\t" + TradeableItemDatabase.useTypeById.get( nextInteger.intValue() ) + "\t" + TradeableItemDatabase.accessById.get( nextInteger ) + "\t" + TradeableItemDatabase.priceById.get( nextInteger.intValue() ) );
			}
			else
			{
				writer.println( nextInteger + "\t" + TradeableItemDatabase.dataNameById.get( nextInteger ) + "\t0\tunknown\t-1" );
			}
		}

		writer.close();

		output = new File( UtilityConstants.DATA_LOCATION, "itemdescs.txt" );
		writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.ITEMDESCS_VERSION );

		it = TradeableItemDatabase.descriptionById.keySet().iterator();
		while ( it.hasNext() )
		{
			Integer id = (Integer) it.next();
			int i = id.intValue();
			if ( i < 0 )
			{
				continue;
			}

			if ( TradeableItemDatabase.descriptionById.get( id ).equals( "" ) )
			{
				continue;
			}

			writer.println( i + "\t" + TradeableItemDatabase.descriptionById.get( id ) + "\t" + TradeableItemDatabase.nameById.get( id ) + "\t" + TradeableItemDatabase.pluralById.get( i ) );
		}

		writer.close();
	}

	// Support for dusty bottles of wine

	public static final void identifyDustyBottles()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastDustyBottleReset" );
		if ( lastAscension == KoLCharacter.getAscensions() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Identifying dusty bottles..." );

		// Identify the six dusty bottles

		for ( int i = 2271; i <= 2276; ++i )
		{
			TradeableItemDatabase.identifyDustyBottle( i );
		}

		KoLSettings.setUserProperty( "lastDustyBottleReset", String.valueOf( KoLCharacter.getAscensions() ) );

		// Set the consumption data

		TradeableItemDatabase.setDustyBottles();
	}

	private static final Pattern GLYPH_PATTERN = Pattern.compile( "Arcane Glyph #(\\d)" );

	private static final void identifyDustyBottle( final int itemId )
	{
		String glyph = "";

		String description = TradeableItemDatabase.rawDescriptionText( itemId );
		if ( description != null )
		{
			Matcher matcher = TradeableItemDatabase.GLYPH_PATTERN.matcher( description );
			if ( matcher.find() )
			{
				glyph = matcher.group( 1 );
			}
		}

		KoLSettings.setUserProperty( "lastDustyBottle" + itemId, glyph );
	}

	public static final void getDustyBottles()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastDustyBottleReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			for ( int i = 2271; i <= 2276; ++i )
			{
				KoLSettings.setUserProperty( "lastDustyBottle" + i, "" );
			}
		}

		TradeableItemDatabase.setDustyBottles();
	}

	private static final void setDustyBottles()
	{
		TradeableItemDatabase.setDustyBottle( 2271 );
		TradeableItemDatabase.setDustyBottle( 2272 );
		TradeableItemDatabase.setDustyBottle( 2273 );
		TradeableItemDatabase.setDustyBottle( 2274 );
		TradeableItemDatabase.setDustyBottle( 2275 );
		TradeableItemDatabase.setDustyBottle( 2276 );
	}

	private static final void setDustyBottle( final int itemId )
	{
		int glyph = KoLSettings.getIntegerProperty( "lastDustyBottle" + itemId );
		switch ( glyph )
		{
		case 0:
			// Unidentified
			TradeableItemDatabase.setDustyBottle( itemId, 2, "0", "0", "0", "0" );
			break;
		case 1: // "Prince"
			// "You drink the wine. You've had better, but you've
			// had worse."
			TradeableItemDatabase.setDustyBottle( itemId, 2, "4-5", "6-8", "5-7", "5-9" );
			break;
		case 2:
			// "You guzzle the entire bottle of wine before you
			// realize that it has turned into vinegar. Bleeah."
			TradeableItemDatabase.setDustyBottle( itemId, 0, "0", "0", "0", "0" );
			break;
		case 3: // "Widget"
			// "You drink the bottle of wine, then belch up a cloud
			// of foul-smelling green smoke. Looks like this wine
			// was infused with wormwood. Spoooooooky."
			TradeableItemDatabase.setDustyBottle( itemId, 2, "3-4", "3-6", "15-18", "3-6" );
			break;
		case 4: // "Snake"
			// "This wine is fantastic! You gulp down the entire
			// bottle, and feel great!"
			TradeableItemDatabase.setDustyBottle( itemId, 2, "5-7", "10-15", "10-15", "10-15" );
			break;
		case 5: // "Pitchfork"
			// "You drink the wine. It tastes pretty good, but when
			// you get to the bottom, it's full of sediment, which
			// turns out to be powdered glass. Ow."
			TradeableItemDatabase.setDustyBottle( itemId, 2, "4-5", "7-10", "5-7", "8-10" );
			break;
		case 6:
			// "You drink the wine, but it seems to have gone
			// bad. Not in the "turned to vinegar" sense, but the
			// "turned to crime" sense. It perpetrates some
			// violence against you on the inside."
			TradeableItemDatabase.setDustyBottle( itemId, 2, "0-1", "0", "0", "0" );
			break;
		}
	}

	private static final void setDustyBottle( final int itemId, final int inebriety, final String adventures,
		final String muscle, final String mysticality, final String moxie )
	{
		String name =
			KoLDatabase.getCanonicalName( (String) TradeableItemDatabase.dataNameById.get( new Integer( itemId ) ) );

		TradeableItemDatabase.inebrietyByName.put( name, new Integer( inebriety ) );
		TradeableItemDatabase.addAdventureRange( name, inebriety, adventures );
		TradeableItemDatabase.muscleByName.put( name, TradeableItemDatabase.extractRange( muscle ) );
		TradeableItemDatabase.mysticalityByName.put( name, TradeableItemDatabase.extractRange( mysticality ) );
		TradeableItemDatabase.moxieByName.put( name, TradeableItemDatabase.extractRange( moxie ) );
	}

	public static final String dustyBottleType( final int itemId )
	{
		int glyph = KoLSettings.getIntegerProperty( "lastDustyBottle" + itemId );
		switch ( glyph )
		{
		case 1:
			return "average wine";
		case 2:
			return "vinegar (Full of Vinegar)";
		case 3:
			return "spooky wine (Kiss of the Black Fairy)";
		case 4:
			return "great wine";
		case 5:
			return "glassy wine";
		case 6:
			return "bad wine";
		}
		return "";
	}

	// Support for the "checkdata" command, which compares KoLmafia's
	// internal item data from what can be mined from the item description.

	private static final StringArray rawDescriptions = new StringArray();

	private static final Map foods = new TreeMap();
	private static final Map boozes = new TreeMap();
	private static final Map hats = new TreeMap();
	private static final Map weapons = new TreeMap();
	private static final Map offhands = new TreeMap();
	private static final Map shirts = new TreeMap();
	private static final Map pants = new TreeMap();
	private static final Map accessories = new TreeMap();
	private static final Map containers = new TreeMap();
	private static final Map famitems = new TreeMap();

	public static final void checkInternalData( final int itemId )
	{
		TradeableItemDatabase.loadScrapeData();
		RequestLogger.printLine( "Checking internal data..." );
		LogStream report = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "itemdata.txt" ), true );

		TradeableItemDatabase.foods.clear();
		TradeableItemDatabase.boozes.clear();
		TradeableItemDatabase.hats.clear();
		TradeableItemDatabase.weapons.clear();
		TradeableItemDatabase.offhands.clear();
		TradeableItemDatabase.shirts.clear();
		TradeableItemDatabase.pants.clear();
		TradeableItemDatabase.accessories.clear();
		TradeableItemDatabase.containers.clear();
		TradeableItemDatabase.famitems.clear();

		// Check item names, desc ID, consumption type

		if ( itemId == 0 )
		{
			Set keys = TradeableItemDatabase.descriptionById.keySet();
			Iterator it = keys.iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 1 )
				{
					continue;
				}

				TradeableItemDatabase.checkItem( id, report );
			}

			String description;
			LogStream livedata =
				LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "itemhtml.txt" ), true );

			it = keys.iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 1 )
				{
					continue;
				}

				description = TradeableItemDatabase.rawDescriptions.get( id );
				if ( !description.equals( "" ) )
				{
					livedata.println( id );
					livedata.println( description );
				}
			}

			livedata.close();
		}
		else
		{
			TradeableItemDatabase.checkItem( itemId, report );
		}

		// Check level limits, equipment, modifiers

		TradeableItemDatabase.checkLevels( report );
		TradeableItemDatabase.checkEquipment( report );
		TradeableItemDatabase.checkModifiers( report );

		report.close();
	}

	private static final void checkItem( final int itemId, final LogStream report )
	{
		Integer id = new Integer( itemId );

		String name = (String) TradeableItemDatabase.dataNameById.get( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String rawText = TradeableItemDatabase.rawDescriptionText( itemId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has no description." );
			return;
		}

		String text = TradeableItemDatabase.descriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has malformed description text." );
			return;
		}

		String descriptionName = TradeableItemDatabase.parseName( text );
		if ( !name.equals( descriptionName ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has description of " + descriptionName + "." );
			return;

		}

		boolean correct = true;

		int type = TradeableItemDatabase.getConsumptionType( itemId );
		String descType = TradeableItemDatabase.parseType( text );
		if ( !TradeableItemDatabase.typesMatch( type, descType ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has consumption type of " + type + " but is described as " + descType + "." );
			correct = false;

		}

		int price = TradeableItemDatabase.priceById.get( itemId );
		int descPrice = TradeableItemDatabase.parsePrice( text );
		if ( price != descPrice )
		{
			report.println( "# *** " + name + " (" + itemId + ") has price of " + price + " but should be " + descPrice + "." );
			correct = false;

		}

		String access = (String) TradeableItemDatabase.accessById.get( id );
		String descAccess = TradeableItemDatabase.parseAccess( text );
		if ( !access.equals( descAccess ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has access of " + access + " but should be " + descAccess + "." );
			correct = false;

		}

		switch ( type )
		{
		case CONSUME_EAT:
			TradeableItemDatabase.foods.put( name, text );
			break;
		case CONSUME_DRINK:
			TradeableItemDatabase.boozes.put( name, text );
			break;
		case EQUIP_HAT:
			TradeableItemDatabase.hats.put( name, text );
			break;
		case EQUIP_PANTS:
			TradeableItemDatabase.pants.put( name, text );
			break;
		case EQUIP_SHIRT:
			TradeableItemDatabase.shirts.put( name, text );
			break;
		case EQUIP_WEAPON:
			TradeableItemDatabase.weapons.put( name, text );
			break;
		case EQUIP_OFFHAND:
			TradeableItemDatabase.offhands.put( name, text );
			break;
		case EQUIP_ACCESSORY:
			TradeableItemDatabase.accessories.put( name, text );
			break;
		case EQUIP_CONTAINER:
			TradeableItemDatabase.containers.put( name, text );
			break;
		case EQUIP_FAMILIAR:
			TradeableItemDatabase.famitems.put( name, text );
			break;
		}

		report.println( itemId + "\t" + name + "\t" + type + "\t" + descAccess + "\t" + descPrice );
	}

	private static final KoLRequest DESC_REQUEST = new KoLRequest( "desc_item.php" );

	private static final String rawDescriptionText( final int itemId )
	{
		Integer id = new Integer( itemId );
		String descId = (String) TradeableItemDatabase.descriptionById.get( id );
		if ( descId == null || descId.equals( "" ) )
		{
			return null;
		}

		String previous = TradeableItemDatabase.rawDescriptions.get( itemId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		TradeableItemDatabase.DESC_REQUEST.addFormField( "whichitem", descId );
		RequestThread.postRequest( TradeableItemDatabase.DESC_REQUEST );
		TradeableItemDatabase.rawDescriptions.set( itemId, TradeableItemDatabase.DESC_REQUEST.responseText );

		return TradeableItemDatabase.DESC_REQUEST.responseText;
	}

	private static final void loadScrapeData()
	{
		if ( TradeableItemDatabase.rawDescriptions.size() > 0 )
		{
			return;
		}

		try
		{
			File saveData = new File( UtilityConstants.DATA_LOCATION, "itemhtml.txt" );
			if ( !saveData.exists() )
			{
				return;
			}

			RequestLogger.printLine( "Loading previous data..." );

			String currentLine;
			StringBuffer currentHTML = new StringBuffer();
			BufferedReader reader = KoLDatabase.getReader( saveData );

			while ( !( currentLine = reader.readLine() ).equals( "" ) )
			{
				currentHTML.setLength( 0 );
				int currentId = StaticEntity.parseInt( currentLine );

				do
				{
					currentLine = reader.readLine();
					currentHTML.append( currentLine );
					currentHTML.append( KoLConstants.LINE_BREAK );
				}
				while ( !currentLine.equals( "</html>" ) );

				TradeableItemDatabase.rawDescriptions.set( currentId, currentHTML.toString() );
				reader.readLine();
			}

			reader.close();
		}
		catch ( Exception e )
		{
			// This shouldn't happen, but if it does, go ahead and
			// fall through.  You're done parsing.
		}
	}

	private static final Pattern DATA_PATTERN = Pattern.compile( "<img.*?><br>(.*?)<script", Pattern.DOTALL );

	private static final String descriptionText( final String rawText )
	{
		Matcher matcher = TradeableItemDatabase.DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 1 );
	}

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>(.*?)</b>" );

	private static final String parseName( final String text )
	{
		Matcher matcher = TradeableItemDatabase.NAME_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return "";
		}

		// One item is known to have an extra internal space
		return StaticEntity.globalStringReplace( matcher.group( 1 ), "  ", " " );
	}

	private static final Pattern PRICE_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	private static final int parsePrice( final String text )
	{
		Matcher matcher = TradeableItemDatabase.PRICE_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StaticEntity.parseInt( matcher.group( 1 ) );
	}

	private static final String parseAccess( final String text )
	{
		if ( text.indexOf( "Quest Item" ) != -1 )
		{
			return "none";
		}

		if ( text.indexOf( "Gift Item" ) != -1 )
		{
			return "gift";
		}

		if ( text.indexOf( "Cannot be traded" ) != -1 )
		{
			return "display";
		}

		return "all";
	}

	private static final Pattern TYPE_PATTERN = Pattern.compile( "Type: <b>(.*?)</b>" );

	private static final String parseType( final String text )
	{
		Matcher matcher = TradeableItemDatabase.TYPE_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return "";
		}

		return matcher.group( 1 );
	}

	private static final boolean typesMatch( final int type, final String descType )
	{
		if ( descType.equals( "" ) )
		{
			return true;
		}

		switch ( type )
		{
		case NO_CONSUME:
			return descType.equals( "" ) || descType.indexOf( "combat" ) != -1 || descType.equals( "crafting item" );
		case CONSUME_EAT:

			return descType.equals( "food" ) || descType.equals( "beverage" );
		case CONSUME_DRINK:
			return descType.equals( "booze" );
		case CONSUME_USE:
		case CONSUME_MULTIPLE:
		case MP_RESTORE:
		case HP_RESTORE:
		case HPMP_RESTORE:
		case MESSAGE_DISPLAY:
		case INFINITE_USES:
			return descType.indexOf( "usable" ) != -1 || descType.equals( "gift package" ) || descType.equals( "food" ) || descType.equals( "booze" );
		case CONSUME_SPECIAL:
			return descType.indexOf( "usable" ) != -1 || descType.equals( "food" ) || descType.equals( "beverage" ) || descType.equals( "booze" );
		case GROW_FAMILIAR:
			return descType.equals( "familiar" );
		case CONSUME_ZAP:
			return descType.equals( "" );
		case EQUIP_FAMILIAR:
			return descType.equals( "familiar equipment" );
		case EQUIP_ACCESSORY:
			return descType.equals( "accessory" );
		case EQUIP_CONTAINER:
			return descType.equals( "container" );
		case EQUIP_HAT:
			return descType.equals( "hat" );
		case EQUIP_PANTS:
			return descType.equals( "pants" );
		case EQUIP_SHIRT:
			return descType.equals( "shirt" );
		case EQUIP_WEAPON:
			return descType.indexOf( "weapon" ) != -1;
		case EQUIP_OFFHAND:
			return descType.indexOf( "off-hand item" ) != -1;
		case CONSUME_HOBO:
			// What is this, anyway?
			return false;
		}
		return false;
	}

	private static final void checkLevels( final LogStream report )
	{
		RequestLogger.printLine( "Checking level requirements..." );

		TradeableItemDatabase.checkLevelMap( report, TradeableItemDatabase.foods, "Food" );
		TradeableItemDatabase.checkLevelMap( report, TradeableItemDatabase.boozes, "Booze" );
	}

	private static final void checkLevelMap( final LogStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# Level requirements in " + ( map == TradeableItemDatabase.foods ? "fullness" : "inebriety" ) + ".txt" );

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = (String) map.get( name );
			TradeableItemDatabase.checkLevelDatum( name, text, report );
		}
	}

	private static final void checkLevelDatum( final String name, final String text, final LogStream report )
	{
		Integer requirement = (Integer) TradeableItemDatabase.levelReqByName.get( KoLDatabase.getCanonicalName( name ) );
		int level = requirement == null ? 0 : requirement.intValue();
		int descLevel = TradeableItemDatabase.parseLevel( text );
		if ( level != descLevel )
		{
			report.println( "# *** " + name + " requires level " + level + " but should be " + descLevel + "." );
		}
	}

	private static final Pattern LEVEL_PATTERN = Pattern.compile( "Level required: <b>(.*?)</b>" );

	private static final int parseLevel( final String text )
	{
		Matcher matcher = TradeableItemDatabase.LEVEL_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 1;
		}

		return StaticEntity.parseInt( matcher.group( 1 ) );
	}

	private static final void checkEquipment( final LogStream report )
	{

		RequestLogger.printLine( "Checking equipment..." );

		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.hats, "Hats" );
		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.pants, "Pants" );
		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.shirts, "Shirts" );
		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.weapons, "Weapons" );
		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.offhands, "Off-hand" );
		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.accessories, "Accessories" );
		TradeableItemDatabase.checkEquipmentMap( report, TradeableItemDatabase.containers, "Containers" );
	}

	private static final void checkEquipmentMap( final LogStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# " + tag + " section of equipment.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = (String) map.get( name );
			TradeableItemDatabase.checkEquipmentDatum( name, text, report );
		}
	}

	private static final Pattern POWER_PATTERN = Pattern.compile( "Power: <b>(\\d+)</b>" );
	private static final Pattern REQ_PATTERN = Pattern.compile( "(\\w+) Required: <b>(\\d+)</b>" );
	private static final Pattern WEAPON_PATTERN = Pattern.compile( "weapon [(](.*?)[)]" );

	private static final void checkEquipmentDatum( final String name, final String text, final LogStream report )
	{
		Matcher matcher;

		String type = TradeableItemDatabase.parseType( text );
		boolean isWeapon = false, isShield = false, hasPower = false;

		if ( type.indexOf( "weapon" ) != -1 )
		{
			isWeapon = true;
		}
		else if ( type.indexOf( "shield" ) != -1 )
		{
			isShield = true;
		}
		else if ( type.indexOf( "hat" ) != -1 || type.indexOf( "pants" ) != -1 || type.indexOf( "shirt" ) != -1 )
		{
			hasPower = true;
		}

		int power = 0;
		if ( isWeapon || hasPower )
		{
			matcher = TradeableItemDatabase.POWER_PATTERN.matcher( text );
			if ( matcher.find() )
			{
				power = StaticEntity.parseInt( matcher.group( 1 ) );
			}
		}
		else if ( isShield )
		{
			// Until KoL puts shield power into the description,
			// use hand-entered "secret" value.
			power = EquipmentDatabase.getPower( name );
		}

		String weaponType = "";
		if ( isWeapon )
		{
			matcher = TradeableItemDatabase.WEAPON_PATTERN.matcher( text );
			if ( matcher.find() )
			{
				weaponType = matcher.group( 1 );
			}
		}

		String req = "none";

		matcher = TradeableItemDatabase.REQ_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			String stat = matcher.group( 1 );
			if ( stat.equals( "Muscle" ) )
			{
				req = "Mus: " + matcher.group( 2 );
			}
			else if ( stat.equals( "Mysticality" ) )
			{
				req = "Mys: " + matcher.group( 2 );
			}
			else if ( stat.equals( "Moxie" ) )
			{
				req = "Mox: " + matcher.group( 2 );
			}
		}
		else if ( isWeapon )
		{
			if ( type.indexOf( "ranged" ) != -1 )
			{
				req = "Mox: 0";
			}
			else if ( weaponType.indexOf( "utensil" ) != -1 || weaponType.indexOf( "saucepan" ) != -1 || weaponType.indexOf( "chefstaff" ) != -1 )
			{
				req = "Mys: 0";
			}
			else
			{
				req = "Mus: 0";
			}
		}

		// Now check against what we already have
		int oldPower = EquipmentDatabase.getPower( name );
		if ( power != oldPower )
		{
			report.println( "# *** " + name + " has power " + oldPower + " but should be " + power + "." );
		}

		String oldReq = EquipmentDatabase.getReq( name );
		if ( !req.equals( oldReq ) )
		{
			report.println( "# *** " + name + " has requirement " + oldReq + " but should be " + req + "." );
		}

		if ( isWeapon )
		{
			String oldWeaponType =
				String.valueOf( EquipmentDatabase.getHands( name ) ) + "-handed " + EquipmentDatabase.getType( name );
			if ( !weaponType.equals( oldWeaponType ) )
			{
				report.println( "# *** " + name + " has weapon type " + oldWeaponType + " but should be " + weaponType + "." );
			}
		}
		else if ( isShield && power == 0 )
		{
			report.println( "# *** " + name + " is a shield of unknown power." );
		}

		if ( isWeapon )
		{
			report.println( name + "\t" + power + "\t" + req + "\t" + weaponType );
		}
		else
		{
			report.println( name + "\t" + power + "\t" + req );
		}
	}

	private static final void checkModifiers( final LogStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );
		ArrayList unknown = new ArrayList();

		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.hats, "Hats", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.pants, "Pants", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.shirts, "Shirts", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.weapons, "Weapons", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.offhands, "Off-hand", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.accessories, "Accessories", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.containers, "Containers", unknown );
		TradeableItemDatabase.checkModifierMap( report, TradeableItemDatabase.famitems, "Familiar Items", unknown );

		if ( unknown.size() == 0 )
		{
			return;
		}

		Collections.sort( unknown );

		for ( int i = 0; i < 10; ++i )
		{
			report.println();
		}

		report.println( "# Unknown Modifiers section of modifiers.txt" );
		report.println();

		for ( int i = 0; i < unknown.size(); ++i )
		{
			report.println( "# " + (String) unknown.get( i ) );
		}
	}

	private static final void checkModifierMap( final LogStream report, final Map map, final String tag,
		final ArrayList unknown )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println();
		report.println( "# " + tag + " section of modifiers.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = (String) map.get( name );
			TradeableItemDatabase.checkModifierDatum( name, text, report, unknown );
		}
	}

	private static final void checkModifierDatum( final String name, final String text, final LogStream report,
		final ArrayList unknown )
	{
		String known = TradeableItemDatabase.parseEnchantments( text, unknown );

		if ( known.equals( "" ) )
		{
			report.println( "# " + name );
		}
		else
		{
			report.println( name + "\t" + known );
		}
	}

	private static final Pattern ENCHANTMENT_PATTERN =
		Pattern.compile( "Enchantment:.*?<font color=blue>(.*)</font>", Pattern.DOTALL );

	private static final String parseEnchantments( final String text, final ArrayList unknown )
	{
		String known = "";

		// Several modifiers can appear outside the "Enchantments"
		// section of the item description.

		String dr = Modifiers.parseDamageReduction( text );
		if ( dr != null )
		{
			if ( !known.equals( "" ) )
			{
				known += ", ";
			}
			known += dr;
		}

		String single = Modifiers.parseSingleEquip( text );
		if ( single != null )
		{
			if ( !known.equals( "" ) )
			{
				known += ", ";
			}
			known += single;
		}

		Matcher matcher = TradeableItemDatabase.ENCHANTMENT_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return known;
		}

		StringBuffer enchantments = new StringBuffer( matcher.group( 1 ) );

		StaticEntity.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total." );
		StaticEntity.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> If you wear multiple items that increase Critical Hit chances, only the highest multiplier applies." );
		StaticEntity.globalStringReplace( enchantments, "<br>", "\n" );

		String[] mods = enchantments.toString().split( "\n+" );
		for ( int i = 0; i < mods.length; ++i )
		{
			String enchantment = mods[ i ].trim();

			if ( enchantment.equals( "" ) )
			{
				continue;
			}

			String mod = Modifiers.parseModifier( enchantment );
			if ( mod != null )
			{
				if ( !known.equals( "" ) )
				{
					known += ", ";
				}
				known += mod;
				continue;
			}

			if ( !unknown.contains( enchantment ) )
			{
				unknown.add( enchantment );
			}
		}

		return known;
	}

	public static final void checkPlurals( final int itemId )
	{
		RequestLogger.printLine( "Checking plurals..." );
		LogStream report = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "plurals.txt" ), true );

		if ( itemId == 0 )
		{
			Iterator it = TradeableItemDatabase.descriptionById.keySet().iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 0 )
				{
					continue;
				}
				TradeableItemDatabase.checkPlural( id, report );
			}
		}
		else
		{
			TradeableItemDatabase.checkPlural( itemId, report );
		}

		report.close();
	}

	private static final void checkPlural( final int itemId, final LogStream report )
	{
		Integer id = new Integer( itemId );

		String name = (String) TradeableItemDatabase.dataNameById.get( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String descId = (String) TradeableItemDatabase.descriptionById.get( id );
		String plural = TradeableItemDatabase.pluralById.get( itemId );

		// Don't bother checking quest items
		String access = (String) TradeableItemDatabase.accessById.get( id );
		if ( access != null && !access.equals( "none" ) )
		{
			String wikiData = TradeableItemDatabase.readWikiData( name );
			Matcher matcher = TradeableItemDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
			String wikiPlural = matcher.find() ? matcher.group( 1 ) : "";
			if ( plural == null || plural.equals( "" ) )
			{
				// No plural. Wiki plural replaces it
				plural = wikiPlural;
			}
			else if ( !wikiPlural.equals( plural ) )
			{
				// Wiki plural differs from KoLmafia plural
				// Assume Wiki is wrong. (!)
				RequestLogger.printLine( "*** " + name + ": KoLmafia plural = \"" + plural + "\", Wiki plural = \"" + wikiPlural + "\"" );
			}
		}

		if ( plural.equals( "" ) )
		{
			report.println( itemId + "\t" + descId + "\t" + name );
		}
		else
		{
			report.println( itemId + "\t" + descId + "\t" + name + "\t" + plural );
		}
	}
}
