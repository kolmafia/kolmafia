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

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ItemDatabase
	extends KoLDatabase
{
	public static final AdventureResult ODE = EffectPool.get( EffectPool.ODE );
	public static final AdventureResult MILK = EffectPool.get( EffectPool.MILK );

	private static final Pattern WIKI_ITEMID_PATTERN = Pattern.compile( "Item number</a>:</b> (\\d+)<br />" );
	private static final Pattern WIKI_DESCID_PATTERN = Pattern.compile( "<b>Description ID:</b> (\\d+)<br />" );
	private static final Pattern WIKI_PLURAL_PATTERN =
		Pattern.compile( "\\(.*?In-game plural</a>: <i>(.*?)</i>\\)", Pattern.DOTALL );
	private static final Pattern WIKI_AUTOSELL_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	private static int maxItemId = 0;

	private static String [] canonicalNames = new String[0];
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
		ItemDatabase.advsByName[ 0 ][ 0 ][ 0 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 0 ][ 0 ][ 0 ][ 1 ] = new TreeMap();
		ItemDatabase.advsByName[ 0 ][ 0 ][ 1 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 0 ][ 0 ][ 1 ][ 1 ] = new TreeMap();

		ItemDatabase.advsByName[ 0 ][ 1 ][ 0 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 0 ][ 1 ][ 0 ][ 1 ] = new TreeMap();
		ItemDatabase.advsByName[ 0 ][ 1 ][ 1 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 0 ][ 1 ][ 1 ][ 1 ] = new TreeMap();

		ItemDatabase.advsByName[ 1 ][ 0 ][ 0 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 1 ][ 0 ][ 0 ][ 1 ] = new TreeMap();
		ItemDatabase.advsByName[ 1 ][ 0 ][ 1 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 1 ][ 0 ][ 1 ][ 1 ] = new TreeMap();

		ItemDatabase.advsByName[ 1 ][ 1 ][ 0 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 1 ][ 1 ][ 0 ][ 1 ] = new TreeMap();
		ItemDatabase.advsByName[ 1 ][ 1 ][ 1 ][ 0 ] = new TreeMap();
		ItemDatabase.advsByName[ 1 ][ 1 ][ 1 ][ 1 ] = new TreeMap();
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
		ItemDatabase.reset();
	}

	public static void reset()
	{
		boolean isFullReset = ItemDatabase.itemIdByName.isEmpty();
		ItemDatabase.itemIdByName.clear();

		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = FileUtilities.getVersionedReader( "tradeitems.txt", KoLConstants.TRADEITEMS_VERSION );

		String[] data;
		Integer id;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length == 5 )
			{
				int itemId = StringUtilities.parseInt( data[ 0 ] );
				id = new Integer( itemId );

				if ( isFullReset )
				{
					ItemDatabase.useTypeById.set( itemId, StringUtilities.parseInt( data[ 2 ] ) );
					ItemDatabase.priceById.set( itemId, StringUtilities.parseInt( data[ 4 ] ) );

					ItemDatabase.dataNameById.put( id, data[ 1 ] );
					ItemDatabase.nameById.put( id, StringUtilities.getDisplayName( data[ 1 ] ) );

					ItemDatabase.accessById.put( id, data[ 3 ] );
					ItemDatabase.tradeableById.set( itemId, data[ 3 ].equals( "all" ) );
					ItemDatabase.giftableById.set( itemId, data[ 3 ].equals( "all" ) || data[ 3 ].equals( "gift" ) );
					ItemDatabase.displayableById.set(
						itemId, data[ 3 ].equals( "all" ) || data[ 3 ].equals( "gift" ) || data[ 3 ].equals( "display" ) );

					if ( itemId > ItemDatabase.maxItemId )
					{
						ItemDatabase.maxItemId = itemId;
					}
				}

				ItemDatabase.itemIdByName.put( StringUtilities.getCanonicalName( data[ 1 ] ), id );
			}
		}

		// Add in dummy information for tracking worthless
		// items so they can be added as conditions.

		id = new Integer( 13 );

		if ( isFullReset )
		{
			ItemDatabase.dataNameById.put( id, "worthless item" );
			ItemDatabase.nameById.put( id, "worthless item" );
		}

		ItemDatabase.itemIdByName.put( "worthless item", id );

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

		if ( isFullReset )
		{
			// Next, retrieve the description ids.

			reader = FileUtilities.getVersionedReader( "itemdescs.txt", KoLConstants.ITEMDESCS_VERSION );

			while ( ( data = FileUtilities.readData( reader ) ) != null )
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
						int itemId = StringUtilities.parseInt( data[ 0 ].trim() );
						id = new Integer( itemId );
						ItemDatabase.descriptionById.put( id, data[ 1 ] );
						ItemDatabase.itemIdByDescription.put( data[ 1 ], new Integer( itemId ) );

						if ( data.length == 4 )
						{
							ItemDatabase.pluralById.set( itemId, data[ 3 ] );
							ItemDatabase.itemIdByPlural.put( StringUtilities.getCanonicalName( data[ 3 ] ), id );
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

			reader = FileUtilities.getVersionedReader( "fullness.txt", KoLConstants.FULLNESS_VERSION );

			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				ItemDatabase.saveItemValues( data, ItemDatabase.fullnessByName );
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

			reader = FileUtilities.getVersionedReader( "inebriety.txt", KoLConstants.INEBRIETY_VERSION );

			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				ItemDatabase.saveItemValues( data, ItemDatabase.inebrietyByName );
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

			reader = FileUtilities.getVersionedReader( "spleenhit.txt", KoLConstants.SPLEENHIT_VERSION );

			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				ItemDatabase.saveItemValues( data, ItemDatabase.spleenHitByName );
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

		// Set aliases for the El Vibrato punch cards

		for ( int i = 0; i < RequestEditorKit.PUNCHCARDS.length; ++i )
		{
			Object [] punchcard = RequestEditorKit.PUNCHCARDS[i];
			Integer itemId = (Integer) punchcard[0];

			String name = StringUtilities.getCanonicalName( (String) punchcard[1] );
			itemIdByName.put( name, itemId );
			String alias = StringUtilities.getCanonicalName( (String) punchcard[2] );
			itemIdByName.put( alias, itemId );
			String plural = StringUtilities.singleStringReplace( alias, "punchcard", "punchcards" );
			itemIdByPlural.put( plural, id );
		}

		ItemDatabase.canonicalNames = new String[ ItemDatabase.itemIdByName.size() ];
		ItemDatabase.itemIdByName.keySet().toArray( ItemDatabase.canonicalNames );
	}

	private static final void saveItemValues( String[] data, Map map )
	{
		if ( data.length < 2 )
			return;

		String name = StringUtilities.getCanonicalName( data[ 0 ] );
		map.put( name, Integer.valueOf( data[ 1 ] ) );

		if ( data.length < 7 )
			return;

		ItemDatabase.levelReqByName.put( name, Integer.valueOf( data[ 2 ] ) );
		ItemDatabase.addAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), data[ 3 ] );
		ItemDatabase.muscleByName.put( name, ItemDatabase.extractRange( data[ 4 ] ) );
		ItemDatabase.mysticalityByName.put( name, ItemDatabase.extractRange( data[ 5 ] ) );
		ItemDatabase.moxieByName.put( name, ItemDatabase.extractRange( data[ 6 ] ) );
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
		int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );
		int end = dashIndex == -1 ? start : StringUtilities.parseInt( range.substring( dashIndex + 1 ) );

		int gainSum1 = 0;
		int gainSum2 = 0;
		int gainSum3 = 0;

		for ( int i = start; i <= end; ++i )
		{
			gainSum1 += i + ItemDatabase.getIncreasingGains( i );
			gainSum2 += i + ItemDatabase.getDecreasingGains( i );
			gainSum3 +=
				i + ItemDatabase.getIncreasingGains( i + ItemDatabase.getDecreasingGains( i ) );
		}

		float count = end - start + 1;

		ItemDatabase.addAdventureRange( name, unitCost, false, false, ( start + end ) / 2.0f );
		ItemDatabase.addAdventureRange( name, unitCost, true, false, gainSum1 / count );
		ItemDatabase.addAdventureRange( name, unitCost, false, true, gainSum2 / count );
		ItemDatabase.addAdventureRange( name, unitCost, true, true, gainSum3 / count );
	}

	private static final void addAdventureRange( final String name, final int unitCost, final boolean gainEffect1,
		final boolean gainEffect2, final float result )
	{
		// Adventure gains from zodiac signs based on information
		// provided on the Iocaine Powder forums.
		// http://www.iocainepowder.org/forums/viewtopic.php?t=2742

		ItemDatabase.getAdventureMap( false, false, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result ) );
		ItemDatabase.getAdventureMap( false, true, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result * 1.1f ) );

		ItemDatabase.getAdventureMap( true, false, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result / unitCost ) );
		ItemDatabase.getAdventureMap( true, true, gainEffect1, gainEffect2 ).put(
			name, KoLConstants.SINGLE_PRECISION_FORMAT.format( result * 1.1f / unitCost ) );
	}

	private static final Map getAdventureMap( final boolean perUnit, final boolean gainZodiac,
		final boolean gainEffect1, final boolean gainEffect2 )
	{
		return ItemDatabase.advsByName[ perUnit ? 1 : 0 ][ gainZodiac ? 1 : 0 ][ gainEffect1 ? 1 : 0 ][ gainEffect2 ? 1 : 0 ];
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
		int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );

		if ( dashIndex == -1 )
		{
			return KoLConstants.SINGLE_PRECISION_FORMAT.format( isNegative ? 0 - start : start );
		}

		int end = StringUtilities.parseInt( range.substring( dashIndex + 1 ) );
		return KoLConstants.SINGLE_PRECISION_FORMAT.format( ( start + end ) / ( isNegative ? -2.0f : 2.0f ) );
	}

	/**
	 * Takes an item name and constructs the likely Wiki equivalent of that item name.
	 */

	private static final String constructWikiName( String name )
	{
		name = StringUtilities.globalStringReplace( StringUtilities.getDisplayName( name ), " ", "_" );
		return Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
	}

	private static final String readWikiData( final String name )
	{
		String line = null;
		StringBuffer wikiRecord = new StringBuffer();

		try
		{
			BufferedReader reader =
				FileUtilities.getReader( "http://kol.coldfront.net/thekolwiki/index.php/" + ItemDatabase.constructWikiName( name ) );
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
		String wikiData = ItemDatabase.readWikiData( name );

		Matcher itemMatcher = ItemDatabase.WIKI_ITEMID_PATTERN.matcher( wikiData );
		if ( !itemMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		Matcher descMatcher = ItemDatabase.WIKI_DESCID_PATTERN.matcher( wikiData );
		if ( !descMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		RequestLogger.printLine( "item: " + name + " (#" + itemMatcher.group( 1 ) + ")" );
		RequestLogger.printLine( "desc: " + descMatcher.group( 1 ) );

		Matcher pluralMatcher = ItemDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
		if ( pluralMatcher.find() )
		{
			RequestLogger.printLine( "plural: " + pluralMatcher.group( 1 ) );
		}

		Matcher sellMatcher = ItemDatabase.WIKI_AUTOSELL_PATTERN.matcher( wikiData );
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
		ItemDatabase.registerItem( itemId, itemName, "" );
	}

	public static final void registerItem( final int itemId, final String itemName, final String descriptionId )
	{
		if ( itemName == null )
		{
			return;
		}

		RequestLogger.printLine( "Unknown item found: " + itemName + " (#" + itemId + ")" );

		ItemDatabase.useTypeById.set( itemId, 0 );
		ItemDatabase.priceById.set( itemId, -1 );

		Integer id = new Integer( itemId );

		ItemDatabase.descriptionById.put( id, descriptionId );
		ItemDatabase.dataNameById.put( id, itemName );
		ItemDatabase.nameById.put( id, StringUtilities.getDisplayName( itemName ) );

		ItemDatabase.registerItemAlias( itemId, itemName, null );
	}

	public static void registerItemAlias( final int itemId, final String itemName, final String plural )
	{
		Integer id = new Integer( itemId );
		ItemDatabase.itemIdByName.put( StringUtilities.getCanonicalName( itemName ), id );

		ItemDatabase.canonicalNames = new String[ ItemDatabase.itemIdByName.size() ];
		ItemDatabase.itemIdByName.keySet().toArray( ItemDatabase.canonicalNames );
		if ( plural != null )
		{
			ItemDatabase.itemIdByPlural.put( StringUtilities.getCanonicalName( plural ), id );
		}
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName )
	{
		return ItemDatabase.getItemId( itemName, 1 );
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
		return getItemId( itemName, count, true );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @param count How many there are
	 * @param substringMatch Whether or not we match against substrings
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName, final int count, final boolean substringMatch )
	{
		if ( itemName == null || itemName.length() == 0 )
		{
			return -1;
		}

		// Get the canonical name of the item, and attempt
		// to parse based on that.

		String canonicalName = StringUtilities.getCanonicalName( itemName );
		Object itemId;

		// See if it's a weird pluralization with a pattern we can't
		// guess before checking for singles.

		if ( count > 1 )
		{
			itemId = ItemDatabase.itemIdByPlural.get( canonicalName );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		itemId = ItemDatabase.itemIdByName.get( canonicalName );

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

		if ( !substringMatch )
		{
			return -1;
		}

		// It's possible that you're looking for a substring.  In
		// that case, prefer complete versions containing the substring
		// over truncated versions which are plurals.

		List possibilities = StringUtilities.getMatchingNames( ItemDatabase.canonicalNames, canonicalName );
		if ( possibilities.size() == 1 )
		{
			return ((Integer)ItemDatabase.itemIdByName.get( possibilities.get( 0 ) )).intValue();
		}

		// Abort if it's clearly not going to be a plural,
		// since this might kill off multi-item detection.

		if ( count == 1 )
		{
			return -1;
		}

		// Or maybe it's a standard plural where they just add a letter
		// to the end.

		itemId = ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a snowcone, then reverse the word order
		if ( canonicalName.startsWith( "snowcones" ) )
		{
			return ItemDatabase.getItemId( canonicalName.split( " " )[ 1 ] + " snowcone", count );
		}

		// Lo mein has this odd pluralization where there's a dash
		// introduced into the name when no such dash exists in the
		// singular form.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "-", " " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// The word right before the dash may also be pluralized,
		// so make sure the dashed words are recognized.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "es-", "-" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "s-", "-" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a plural form of "tooth", then make
		// sure that it's handled.  Other things which
		// also have "ee" plural forms should be clumped
		// in as well.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ee", "oo" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also handle the plural of vortex, which is
		// "vortices" -- this should only appear in the
		// meat vortex, but better safe than sorry.

		itemId =
			ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ices", "ex" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Handling of appendices (which is the plural
		// of appendix, not appendex, so it is not caught
		// by the previous test).

		itemId =
			ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ices", "ix" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also add in a special handling for knives
		// and other things ending in "ife".

		itemId =
			ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ives", "ife" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also add in a special handling for elves
		// and other things ending in "f".

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ves", "f" ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// Also add in a special handling for staves
		// and other things ending in "aff".

		itemId =
			ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "aves", "aff" ) );
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
				ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 3 ) + "y" );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		itemId =
			ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ies ", "y " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a pluralized form of something that
		// ends with "o", then return the appropriate
		// item Id for the "o" version.

		if ( canonicalName.endsWith( "es" ) )
		{
			itemId = ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 2 ) );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "es ", " " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a pluralized form of something that
		// ends with "an", then return the appropriate
		// item Id for the "en" version.

		itemId =
			ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "en ", "an " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's a standard pluralized forms, then
		// return the appropriate item Id.

		itemId = ItemDatabase.itemIdByName.get( canonicalName.replaceFirst( "([A-Za-z])s ", "$1 " ) );
		if ( itemId != null )
		{
			return ( (Integer) itemId ).intValue();
		}

		// If it's something that ends with 'i', then
		// it might be a singular ending with 'us'.

		if ( canonicalName.endsWith( "i" ) )
		{
			itemId =
				ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) + "us" );
			if ( itemId != null )
			{
				return ( (Integer) itemId ).intValue();
			}
		}

		// Unknown item

		return -1;
	}

	/**
	 * Returns the plural for an item, given its Id number
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The plural name of the corresponding item
	 */

	public static final String getPluralName( final int itemId )
	{
		String plural = pluralById.get( itemId );
		return plural == null ? ItemDatabase.getItemName( itemId ) : plural;
	}

	public static final boolean meetsLevelRequirement( final String name )
	{
		if ( name == null )
		{
			return false;
		}

		Integer requirement = (Integer) ItemDatabase.levelReqByName.get( StringUtilities.getCanonicalName( name ) );
		return requirement == null ? false : KoLCharacter.getLevel() >= requirement.intValue();
	}

	public static final int getFullness( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer fullness = (Integer) ItemDatabase.fullnessByName.get( StringUtilities.getCanonicalName( name ) );
		return fullness == null ? 0 : fullness.intValue();
	}

	public static final int getInebriety( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer inebriety = (Integer) ItemDatabase.inebrietyByName.get( StringUtilities.getCanonicalName( name ) );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	public static final int getSpleenHit( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer spleenhit = (Integer) ItemDatabase.spleenHitByName.get( StringUtilities.getCanonicalName( name ) );
		return spleenhit == null ? 0 : spleenhit.intValue();
	}

	public static final String getAdventureRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		int fullness = ItemDatabase.getFullness( name );
		int inebriety = ItemDatabase.getInebriety( name );
		int spleenhit = ItemDatabase.getSpleenHit( name );

		String range = null;

		if ( fullness > 0 )
		{
			range =
				(String) ItemDatabase.getAdventureMap(
					Preferences.getBoolean( "showGainsPerUnit" ),
					KoLCharacter.getSign().indexOf( "Opossum" ) != -1,
					KoLConstants.activeEffects.contains( ItemDatabase.MILK ),
					Preferences.getInteger( "munchiesPillsUsed" ) > 0 ).get(
					StringUtilities.getCanonicalName( name ) );
		}
		else if ( inebriety > 0 )
		{
			range =
				(String) ItemDatabase.getAdventureMap(
					Preferences.getBoolean( "showGainsPerUnit" ),
					KoLCharacter.getSign().indexOf( "Blender" ) != -1,
					KoLConstants.activeEffects.contains( ItemDatabase.ODE ),
					false ).get(
					StringUtilities.getCanonicalName( name ) );
		}
		else if ( spleenhit > 0 )
		{
			range =
				(String) ItemDatabase.getAdventureMap(
					Preferences.getBoolean( "showGainsPerUnit" ),
					false,
					false,
					false ).get(
					StringUtilities.getCanonicalName( name ) );
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

		String range = (String) ItemDatabase.muscleByName.get( StringUtilities.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMysticalityRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) ItemDatabase.mysticalityByName.get( StringUtilities.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMoxieRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) ItemDatabase.moxieByName.get( StringUtilities.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	/**
	 * Returns the price for the item with the given Id.
	 *
	 * @return The price associated with the item
	 */

	public static final int getPriceById( final int itemId )
	{
		return ItemDatabase.priceById.get( itemId );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isTradeable( final int itemId )
	{
		return ItemDatabase.tradeableById.get( itemId );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isGiftable( final int itemId )
	{
		return ItemDatabase.giftableById.get( itemId );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isDisplayable( final int itemId )
	{
		return ItemDatabase.displayableById.get( itemId );
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
		return (String) ItemDatabase.nameById.get( new Integer( itemId ) );
	}

	/**
	 * Returns the name for an item, given its Id number.
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The name of the corresponding item
	 */

	public static final String getItemName( final String descriptionId )
	{
		Integer itemId = (Integer) ItemDatabase.itemIdByDescription.get( descriptionId );
		return itemId == null ? null : ItemDatabase.getItemName( itemId.intValue() );
	}

	/**
	 * Returns the id for an item, given its description id number.
	 *
	 * @param itemId The description id number of the item to lookup
	 * @return The item id of the corresponding item
	 */

	public static final int getItemIdFromDescription( final String descriptionId )
	{
		Integer itemId = (Integer) ItemDatabase.itemIdByDescription.get( descriptionId );
		return itemId == null ? -1 : itemId.intValue();
	}

	/**
	 * Returns a list of all items which contain the given substring. This is useful for people who are doing lookups on
	 * items.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames( ItemDatabase.canonicalNames, substring );
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String itemName )
	{
		return ItemDatabase.getItemId( itemName ) != -1;
	}

	/**
	 * Returns whether or not the item with the given name is usable (this includes edibility).
	 *
	 * @return <code>true</code> if the item is usable
	 */

	public static final boolean isUsable( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName );
		if ( itemId <= 0 )
		{
			return false;
		}

		switch ( ItemDatabase.useTypeById.get( itemId ) )
		{
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
		case KoLConstants.CONSUME_USE:
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.INFINITE_USES:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.GROW_FAMILIAR:
		case KoLConstants.CONSUME_ZAP:
		case KoLConstants.MP_RESTORE:
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
		return itemId <= 0 ? KoLConstants.NO_CONSUME : ItemDatabase.useTypeById.get( itemId );
	}

	public static final int getConsumptionType( final String itemName )
	{
		return ItemDatabase.getConsumptionType( ItemDatabase.getItemId( itemName ) );
	}

	/**
	 * Returns the item description Id used by the given item, given its item Id.
	 *
	 * @return The description Id associated with the item
	 */

	public static final String getDescriptionId( final int itemId )
	{
		return (String) ItemDatabase.descriptionById.get( new Integer( itemId ) );
	}

	/**
	 * Returns the set of item names keyed by id
	 *
	 * @return The set of item names keyed by id
	 */

	public static final Set entrySet()
	{
		return ItemDatabase.nameById.entrySet();
	}

	/**
	 * Returns the largest item ID
	 *
	 * @return The largest item ID
	 */

	public static final int maxItemId()
	{
		return ItemDatabase.maxItemId;
	}

	private static final Pattern CLOSET_ITEM_PATTERN =
		Pattern.compile( "<option value='(\\d+)' descid='(.*?)'>(.*?) \\(" );
	private static final Pattern DESCRIPTION_PATTERN =
		Pattern.compile( "onClick='descitem\\((\\d+)\\);'></td><td valign=top><b>(.*?)</b>" );

	public static final void findItemDescriptions()
	{
		RequestLogger.printLine( "Checking for new non-quest items..." );

		GenericRequest updateRequest = new EquipmentRequest( EquipmentRequest.CLOSET );
		RequestThread.postRequest( updateRequest );
		Matcher itemMatcher = ItemDatabase.CLOSET_ITEM_PATTERN.matcher( updateRequest.responseText );

		boolean foundChanges = false;

		while ( itemMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String descId = (String)ItemDatabase.descriptionById.get( new Integer( itemId ) );
			if ( descId != null && !descId.equals( "" ) )
			{
				continue;
			}

			foundChanges = true;
			ItemDatabase.registerItem( itemId, itemMatcher.group( 3 ), itemMatcher.group( 2 ) );
		}

		RequestLogger.printLine( "Parsing for quest items..." );
		GenericRequest itemChecker = new GenericRequest( "inventory.php?which=3" );

		RequestThread.postRequest( itemChecker );
		itemMatcher = ItemDatabase.DESCRIPTION_PATTERN.matcher( itemChecker.responseText );

		while ( itemMatcher.find() )
		{
			String itemName = itemMatcher.group( 2 );
			int itemId = ItemDatabase.getItemId( itemName );

			if ( itemId == -1 )
			{
				continue;
			}

			if ( !ItemDatabase.descriptionById.get( new Integer( itemId ) ).equals( "" ) )
			{
				continue;
			}

			foundChanges = true;
			ItemDatabase.registerItem( itemId, itemName, itemMatcher.group( 1 ) );
		}

		if ( foundChanges )
		{
			ItemDatabase.saveDataOverride();
		}
	}

	public static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "tradeitems.txt" );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.TRADEITEMS_VERSION );

		int lastInteger = 1;
		Iterator it = ItemDatabase.nameById.keySet().iterator();

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
			{
				writer.println( i );
			}

			lastInteger = nextInteger.intValue() + 1;

			if ( ItemDatabase.accessById.containsKey( nextInteger ) )
			{
				writer.println( nextInteger + "\t" + ItemDatabase.dataNameById.get( nextInteger ) + "\t" + ItemDatabase.useTypeById.get( nextInteger.intValue() ) + "\t" + ItemDatabase.accessById.get( nextInteger ) + "\t" + ItemDatabase.priceById.get( nextInteger.intValue() ) );
			}
			else
			{
				writer.println( nextInteger + "\t" + ItemDatabase.dataNameById.get( nextInteger ) + "\t0\tunknown\t-1" );
			}
		}

		writer.close();

		output = new File( UtilityConstants.DATA_LOCATION, "itemdescs.txt" );
		writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.ITEMDESCS_VERSION );

		it = ItemDatabase.descriptionById.entrySet().iterator();

		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();

			Integer id = (Integer) entry.getKey();
			int i = id.intValue();

			if ( i < 0 )
			{
				continue;
			}

			if ( entry.getValue().equals( "" ) )
			{
				continue;
			}

			writer.println( i + "\t" + entry.getValue() + "\t" + ItemDatabase.nameById.get( id ) + "\t" + ItemDatabase.pluralById.get( i ) );
		}

		writer.close();
	}

	// Support for dusty bottles of wine

	public static final void identifyDustyBottles()
	{
		int lastAscension = Preferences.getInteger( "lastDustyBottleReset" );
		if ( lastAscension == KoLCharacter.getAscensions() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Identifying dusty bottles..." );

		// Identify the six dusty bottles

		for ( int i = 2271; i <= 2276; ++i )
		{
			ItemDatabase.identifyDustyBottle( i );
		}

		Preferences.setInteger( "lastDustyBottleReset", KoLCharacter.getAscensions() );

		// Set the consumption data

		ItemDatabase.setDustyBottles();
	}

	private static final Pattern GLYPH_PATTERN = Pattern.compile( "Arcane Glyph #(\\d)" );

	private static final void identifyDustyBottle( final int itemId )
	{
		String glyph = "";

		String description = ItemDatabase.rawDescriptionText( itemId );
		if ( description != null )
		{
			Matcher matcher = ItemDatabase.GLYPH_PATTERN.matcher( description );
			if ( matcher.find() )
			{
				glyph = matcher.group( 1 );
			}
		}

		Preferences.setString( "lastDustyBottle" + itemId, glyph );
	}

	public static final void getDustyBottles()
	{
		int lastAscension = Preferences.getInteger( "lastDustyBottleReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			for ( int i = 2271; i <= 2276; ++i )
			{
				Preferences.setString( "lastDustyBottle" + i, "" );
			}
		}

		ItemDatabase.setDustyBottles();
	}

	private static final void setDustyBottles()
	{
		ItemDatabase.setDustyBottle( 2271 );
		ItemDatabase.setDustyBottle( 2272 );
		ItemDatabase.setDustyBottle( 2273 );
		ItemDatabase.setDustyBottle( 2274 );
		ItemDatabase.setDustyBottle( 2275 );
		ItemDatabase.setDustyBottle( 2276 );
	}

	private static final void setDustyBottle( final int itemId )
	{
		int glyph = Preferences.getInteger( "lastDustyBottle" + itemId );
		switch ( glyph )
		{
		case 0:
			// Unidentified
			ItemDatabase.setDustyBottle( itemId, 2, "0", "0", "0", "0" );
			break;
		case 1: // "Prince"
			// "You drink the wine. You've had better, but you've
			// had worse."
			ItemDatabase.setDustyBottle( itemId, 2, "4-5", "6-8", "5-7", "5-9" );
			break;
		case 2:
			// "You guzzle the entire bottle of wine before you
			// realize that it has turned into vinegar. Bleeah."
			ItemDatabase.setDustyBottle( itemId, 0, "0", "0", "0", "0" );
			break;
		case 3: // "Widget"
			// "You drink the bottle of wine, then belch up a cloud
			// of foul-smelling green smoke. Looks like this wine
			// was infused with wormwood. Spoooooooky."
			ItemDatabase.setDustyBottle( itemId, 2, "3-4", "3-6", "15-18", "3-6" );
			break;
		case 4: // "Snake"
			// "This wine is fantastic! You gulp down the entire
			// bottle, and feel great!"
			ItemDatabase.setDustyBottle( itemId, 2, "5-7", "10-15", "10-15", "10-15" );
			break;
		case 5: // "Pitchfork"
			// "You drink the wine. It tastes pretty good, but when
			// you get to the bottom, it's full of sediment, which
			// turns out to be powdered glass. Ow."
			ItemDatabase.setDustyBottle( itemId, 2, "4-5", "7-10", "5-7", "8-10" );
			break;
		case 6:
			// "You drink the wine, but it seems to have gone
			// bad. Not in the "turned to vinegar" sense, but the
			// "turned to crime" sense. It perpetrates some
			// violence against you on the inside."
			ItemDatabase.setDustyBottle( itemId, 2, "0-1", "0", "0", "0" );
			break;
		}
	}

	private static final void setDustyBottle( final int itemId, final int inebriety, final String adventures,
		final String muscle, final String mysticality, final String moxie )
	{
		String name =
			StringUtilities.getCanonicalName( (String) ItemDatabase.dataNameById.get( new Integer( itemId ) ) );

		ItemDatabase.inebrietyByName.put( name, new Integer( inebriety ) );
		ItemDatabase.addAdventureRange( name, inebriety, adventures );
		ItemDatabase.muscleByName.put( name, ItemDatabase.extractRange( muscle ) );
		ItemDatabase.mysticalityByName.put( name, ItemDatabase.extractRange( mysticality ) );
		ItemDatabase.moxieByName.put( name, ItemDatabase.extractRange( moxie ) );
	}

	public static final String dustyBottleType( final int itemId )
	{
		int glyph = Preferences.getInteger( "lastDustyBottle" + itemId );
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
		ItemDatabase.loadScrapeData();
		RequestLogger.printLine( "Checking internal data..." );
		PrintStream report = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "itemdata.txt" ), true );

		ItemDatabase.foods.clear();
		ItemDatabase.boozes.clear();
		ItemDatabase.hats.clear();
		ItemDatabase.weapons.clear();
		ItemDatabase.offhands.clear();
		ItemDatabase.shirts.clear();
		ItemDatabase.pants.clear();
		ItemDatabase.accessories.clear();
		ItemDatabase.containers.clear();
		ItemDatabase.famitems.clear();

		// Check item names, desc ID, consumption type

		if ( itemId == 0 )
		{
			Set keys = ItemDatabase.descriptionById.keySet();
			Iterator it = keys.iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 1 )
				{
					continue;
				}

				ItemDatabase.checkItem( id, report );
			}

			String description;
			PrintStream livedata =
				LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "itemhtml.txt" ), true );

			it = keys.iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 1 )
				{
					continue;
				}

				description = ItemDatabase.rawDescriptions.get( id );
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
			ItemDatabase.checkItem( itemId, report );
		}

		// Check level limits, equipment, modifiers

		ItemDatabase.checkLevels( report );
		ItemDatabase.checkEquipment( report );
		ItemDatabase.checkModifiers( report );

		report.close();
	}

	private static final void checkItem( final int itemId, final PrintStream report )
	{
		Integer id = new Integer( itemId );

		String name = (String) ItemDatabase.dataNameById.get( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String rawText = ItemDatabase.rawDescriptionText( itemId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has no description." );
			return;
		}

		String text = ItemDatabase.descriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has malformed description text." );
			return;
		}

		String descriptionName = ItemDatabase.parseName( text );
		if ( !name.equals( descriptionName ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has description of " + descriptionName + "." );
			return;

		}

		boolean correct = true;

		int type = ItemDatabase.getConsumptionType( itemId );
		String descType = ItemDatabase.parseType( text );
		if ( !ItemDatabase.typesMatch( type, descType ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has consumption type of " + type + " but is described as " + descType + "." );
			correct = false;

		}

		int price = ItemDatabase.priceById.get( itemId );
		int descPrice = ItemDatabase.parsePrice( text );
		if ( price != descPrice )
		{
			report.println( "# *** " + name + " (" + itemId + ") has price of " + price + " but should be " + descPrice + "." );
			correct = false;

		}

		String access = (String) ItemDatabase.accessById.get( id );
		String descAccess = ItemDatabase.parseAccess( text );
		if ( !access.equals( descAccess ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has access of " + access + " but should be " + descAccess + "." );
			correct = false;

		}

		switch ( type )
		{
		case KoLConstants.CONSUME_EAT:
			ItemDatabase.foods.put( name, text );
			break;
		case KoLConstants.CONSUME_DRINK:
			ItemDatabase.boozes.put( name, text );
			break;
		case KoLConstants.EQUIP_HAT:
			ItemDatabase.hats.put( name, text );
			break;
		case KoLConstants.EQUIP_PANTS:
			ItemDatabase.pants.put( name, text );
			break;
		case KoLConstants.EQUIP_SHIRT:
			ItemDatabase.shirts.put( name, text );
			break;
		case KoLConstants.EQUIP_WEAPON:
			ItemDatabase.weapons.put( name, text );
			break;
		case KoLConstants.EQUIP_OFFHAND:
			ItemDatabase.offhands.put( name, text );
			break;
		case KoLConstants.EQUIP_ACCESSORY:
			ItemDatabase.accessories.put( name, text );
			break;
		case KoLConstants.EQUIP_CONTAINER:
			ItemDatabase.containers.put( name, text );
			break;
		case KoLConstants.EQUIP_FAMILIAR:
			ItemDatabase.famitems.put( name, text );
			break;
		}

		report.println( itemId + "\t" + name + "\t" + type + "\t" + descAccess + "\t" + descPrice );
	}

	private static final GenericRequest DESC_REQUEST = new GenericRequest( "desc_item.php" );

	private static final String rawDescriptionText( final int itemId )
	{
		Integer id = new Integer( itemId );
		String descId = (String) ItemDatabase.descriptionById.get( id );
		if ( descId == null || descId.equals( "" ) )
		{
			return null;
		}

		String previous = ItemDatabase.rawDescriptions.get( itemId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		ItemDatabase.DESC_REQUEST.addFormField( "whichitem", descId );
		RequestThread.postRequest( ItemDatabase.DESC_REQUEST );
		ItemDatabase.rawDescriptions.set( itemId, ItemDatabase.DESC_REQUEST.responseText );

		return ItemDatabase.DESC_REQUEST.responseText;
	}

	private static final void loadScrapeData()
	{
		if ( ItemDatabase.rawDescriptions.size() > 0 )
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
			BufferedReader reader = FileUtilities.getReader( saveData );

			while ( !( currentLine = reader.readLine() ).equals( "" ) )
			{
				currentHTML.setLength( 0 );
				int currentId = StringUtilities.parseInt( currentLine );

				do
				{
					currentLine = reader.readLine();
					currentHTML.append( currentLine );
					currentHTML.append( KoLConstants.LINE_BREAK );
				}
				while ( !currentLine.equals( "</html>" ) );

				ItemDatabase.rawDescriptions.set( currentId, currentHTML.toString() );
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
		Matcher matcher = ItemDatabase.DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 1 );
	}

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>(.*?)</b>" );

	private static final String parseName( final String text )
	{
		Matcher matcher = ItemDatabase.NAME_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return "";
		}

		// One item is known to have an extra internal space
		return StringUtilities.globalStringReplace( matcher.group( 1 ), "  ", " " );
	}

	private static final Pattern PRICE_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	private static final int parsePrice( final String text )
	{
		Matcher matcher = ItemDatabase.PRICE_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
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
		Matcher matcher = ItemDatabase.TYPE_PATTERN.matcher( text );
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
		case KoLConstants.NO_CONSUME:
			return descType.equals( "" ) || descType.indexOf( "combat" ) != -1 || descType.equals( "crafting item" );
		case KoLConstants.CONSUME_EAT:

			return descType.equals( "food" ) || descType.equals( "beverage" );
		case KoLConstants.CONSUME_DRINK:
			return descType.equals( "booze" );
		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.INFINITE_USES:
			return descType.indexOf( "usable" ) != -1 || descType.equals( "gift package" ) || descType.equals( "food" ) || descType.equals( "booze" );
		case KoLConstants.CONSUME_SPECIAL:
			return descType.indexOf( "usable" ) != -1 || descType.equals( "food" ) || descType.equals( "beverage" ) || descType.equals( "booze" );
		case KoLConstants.GROW_FAMILIAR:
			return descType.equals( "familiar" );
		case KoLConstants.CONSUME_ZAP:
			return descType.equals( "" );
		case KoLConstants.EQUIP_FAMILIAR:
			return descType.equals( "familiar equipment" );
		case KoLConstants.EQUIP_ACCESSORY:
			return descType.equals( "accessory" );
		case KoLConstants.EQUIP_CONTAINER:
			return descType.equals( "container" );
		case KoLConstants.EQUIP_HAT:
			return descType.equals( "hat" );
		case KoLConstants.EQUIP_PANTS:
			return descType.equals( "pants" );
		case KoLConstants.EQUIP_SHIRT:
			return descType.equals( "shirt" );
		case KoLConstants.EQUIP_WEAPON:
			return descType.indexOf( "weapon" ) != -1;
		case KoLConstants.EQUIP_OFFHAND:
			return descType.indexOf( "off-hand item" ) != -1;
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
			return false;
		}
		return false;
	}

	private static final void checkLevels( final PrintStream report )
	{
		RequestLogger.printLine( "Checking level requirements..." );

		ItemDatabase.checkLevelMap( report, ItemDatabase.foods, "Food" );
		ItemDatabase.checkLevelMap( report, ItemDatabase.boozes, "Booze" );
	}

	private static final void checkLevelMap( final PrintStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# Level requirements in " + ( map == ItemDatabase.foods ? "fullness" : "inebriety" ) + ".txt" );

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = (String) map.get( name );
			ItemDatabase.checkLevelDatum( name, text, report );
		}
	}

	private static final void checkLevelDatum( final String name, final String text, final PrintStream report )
	{
		Integer requirement = (Integer) ItemDatabase.levelReqByName.get( StringUtilities.getCanonicalName( name ) );
		int level = requirement == null ? 0 : requirement.intValue();
		int descLevel = ItemDatabase.parseLevel( text );
		if ( level != descLevel )
		{
			report.println( "# *** " + name + " requires level " + level + " but should be " + descLevel + "." );
		}
	}

	private static final Pattern LEVEL_PATTERN = Pattern.compile( "Level required: <b>(.*?)</b>" );

	private static final int parseLevel( final String text )
	{
		Matcher matcher = ItemDatabase.LEVEL_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 1;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final void checkEquipment( final PrintStream report )
	{

		RequestLogger.printLine( "Checking equipment..." );

		ItemDatabase.checkEquipmentMap( report, ItemDatabase.hats, "Hats" );
		ItemDatabase.checkEquipmentMap( report, ItemDatabase.pants, "Pants" );
		ItemDatabase.checkEquipmentMap( report, ItemDatabase.shirts, "Shirts" );
		ItemDatabase.checkEquipmentMap( report, ItemDatabase.weapons, "Weapons" );
		ItemDatabase.checkEquipmentMap( report, ItemDatabase.offhands, "Off-hand" );
		ItemDatabase.checkEquipmentMap( report, ItemDatabase.accessories, "Accessories" );
		ItemDatabase.checkEquipmentMap( report, ItemDatabase.containers, "Containers" );
	}

	private static final void checkEquipmentMap( final PrintStream report, final Map map, final String tag )
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
			ItemDatabase.checkEquipmentDatum( name, text, report );
		}
	}

	private static final Pattern POWER_PATTERN = Pattern.compile( "Power: <b>(\\d+)</b>" );
	private static final Pattern REQ_PATTERN = Pattern.compile( "(\\w+) Required: <b>(\\d+)</b>" );
	private static final Pattern WEAPON_PATTERN = Pattern.compile( "weapon [(](.*?)[)]" );

	private static final void checkEquipmentDatum( final String name, final String text, final PrintStream report )
	{
		Matcher matcher;

		String type = ItemDatabase.parseType( text );
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
			matcher = ItemDatabase.POWER_PATTERN.matcher( text );
			if ( matcher.find() )
			{
				power = StringUtilities.parseInt( matcher.group( 1 ) );
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
			matcher = ItemDatabase.WEAPON_PATTERN.matcher( text );
			if ( matcher.find() )
			{
				weaponType = matcher.group( 1 );
			}
		}

		String req = "none";

		matcher = ItemDatabase.REQ_PATTERN.matcher( text );
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

		String oldReq = EquipmentDatabase.getEquipRequirement( name );
		if ( !req.equals( oldReq ) )
		{
			report.println( "# *** " + name + " has requirement " + oldReq + " but should be " + req + "." );
		}

		if ( isWeapon )
		{
			int spaceIndex = weaponType.indexOf( " " );
			String oldHanded = EquipmentDatabase.getHands( name ) + "-handed";

			if ( spaceIndex != -1 && !weaponType.startsWith( oldHanded ) )
			{
				String handed = weaponType.substring( 0, spaceIndex );
				report.println( "# *** " + name + " is marked as " + oldHanded + " but should be " + handed + "." );
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

	private static final void checkModifiers( final PrintStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );
		ArrayList unknown = new ArrayList();

		ItemDatabase.checkModifierMap( report, ItemDatabase.hats, "Hats", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.pants, "Pants", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.shirts, "Shirts", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.weapons, "Weapons", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.offhands, "Off-hand", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.accessories, "Accessories", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.containers, "Containers", unknown );
		ItemDatabase.checkModifierMap( report, ItemDatabase.famitems, "Familiar Items", unknown );

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

	private static final void checkModifierMap( final PrintStream report, final Map map, final String tag,
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
			ItemDatabase.checkModifierDatum( name, text, report, unknown );
		}
	}

	private static final void checkModifierDatum( final String name, final String text, final PrintStream report,
		final ArrayList unknown )
	{
		String known = ItemDatabase.parseEnchantments( text, unknown );

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

		known = parseStandardEnchantments( text, known, unknown );

		String softcore = Modifiers.parseSoftcoreOnly( text );
		if ( softcore != null )
		{
			if ( !known.equals( "" ) )
			{
				known += ", ";
			}
			known += softcore;
		}

		return known;
	}

	private static final String parseStandardEnchantments( final String text, String known, final ArrayList unknown )
	{
		Matcher matcher = ItemDatabase.ENCHANTMENT_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return known;
		}

		StringBuffer enchantments = new StringBuffer( matcher.group( 1 ) );

		StringUtilities.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total." );
		StringUtilities.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> If you wear multiple items that increase Critical Hit chances, only the highest multiplier applies." );
		StringUtilities.globalStringReplace( enchantments, "<br>", "\n" );

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
		PrintStream report = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "plurals.txt" ), true );

		if ( itemId == 0 )
		{
			Iterator it = ItemDatabase.descriptionById.keySet().iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 0 )
				{
					continue;
				}
				ItemDatabase.checkPlural( id, report );
			}
		}
		else
		{
			ItemDatabase.checkPlural( itemId, report );
		}

		report.close();
	}

	private static final void checkPlural( final int itemId, final PrintStream report )
	{
		Integer id = new Integer( itemId );

		String name = (String) ItemDatabase.dataNameById.get( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String descId = (String) ItemDatabase.descriptionById.get( id );
		String plural = ItemDatabase.pluralById.get( itemId );

		// Don't bother checking quest items
		String access = (String) ItemDatabase.accessById.get( id );
		if ( access != null && !access.equals( "none" ) )
		{
			String wikiData = ItemDatabase.readWikiData( name );
			Matcher matcher = ItemDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
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
