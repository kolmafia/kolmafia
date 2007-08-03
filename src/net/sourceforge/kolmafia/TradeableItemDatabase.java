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

public class TradeableItemDatabase extends KoLDatabase
{
	public static final AdventureResult ODE = new AdventureResult( "Ode to Booze", 1, true );
	public static final AdventureResult GOT_MILK = new AdventureResult( "Got Milk", 1, true );

	private static final Pattern WIKI_ITEMID_PATTERN = Pattern.compile( "Item number</a>:</b> (\\d+)<br />" );
	private static final Pattern WIKI_DESCID_PATTERN = Pattern.compile( "<b>Description ID:</b> (\\d+)<br />" );
	private static final Pattern WIKI_PLURAL_PATTERN = Pattern.compile( "\\(Plural: <i>(.*?)<\\/i>\\)", Pattern.DOTALL );
	private static final Pattern WIKI_AUTOSELL_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	private static int maxItemId = 0;

	private static final IntegerArray useTypeById = new IntegerArray();
	private static final IntegerArray priceById = new IntegerArray();
	private static final StringArray descriptionById = new StringArray();
	private static final StringArray pluralById = new StringArray();

	private static final Map nameById = new TreeMap();
	private static final Map dataNameById = new TreeMap();
	private static final Map itemIdByName = new TreeMap();
	private static final Map itemIdByPlural = new TreeMap();

	private static final Map levelReqByName = new TreeMap();
	private static final Map fullnessByName = new TreeMap();
	private static final Map inebrietyByName = new TreeMap();
	private static final Map spleenHitByName = new TreeMap();

	private static final Map [][][][] advsByName = new TreeMap[2][2][2][2];

	static
	{
		advsByName[0][0][0][0] = new TreeMap();
		advsByName[0][0][0][1] = new TreeMap();
		advsByName[0][0][1][0] = new TreeMap();
		advsByName[0][0][1][1] = new TreeMap();

		advsByName[0][1][0][0] = new TreeMap();
		advsByName[0][1][0][1] = new TreeMap();
		advsByName[0][1][1][0] = new TreeMap();
		advsByName[0][1][1][1] = new TreeMap();

		advsByName[1][0][0][0] = new TreeMap();
		advsByName[1][0][0][1] = new TreeMap();
		advsByName[1][0][1][0] = new TreeMap();
		advsByName[1][0][1][1] = new TreeMap();

		advsByName[1][1][0][0] = new TreeMap();
		advsByName[1][1][0][1] = new TreeMap();
		advsByName[1][1][1][0] = new TreeMap();
		advsByName[1][1][1][1] = new TreeMap();
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

		BufferedReader reader = getReader( "tradeitems.txt" );

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 5 )
			{
				int itemId = StaticEntity.parseInt( data[0] );
				Integer id = new Integer( itemId );

				useTypeById.set( itemId, StaticEntity.parseInt( data[2] ) );
				priceById.set( itemId, StaticEntity.parseInt( data[4] ) );

				itemIdByName.put( getCanonicalName( data[1] ), id );
				dataNameById.put( id, data[1] );
				nameById.put( id, getDisplayName( data[1] ) );

				accessById.put( id, data[3] );
				tradeableById.set( itemId, data[3].equals( "all" ) );
				giftableById.set( itemId, data[3].equals( "all" ) || data[3].equals( "gift" ) );
				displayableById.set( itemId, data[3].equals( "all" ) || data[3].equals( "gift" ) || data[3].equals( "display" ) );

				if ( itemId > maxItemId )
					maxItemId = itemId;
			}
		}

		// Add in dummy information for tracking worthless
		// items so they can be added as conditions.

		dataNameById.put( new Integer(13), "worthless item" );
		itemIdByName.put( "worthless item", new Integer(13) );
		nameById.put( new Integer(13), "worthless item" );

		// Next, retrieve the description Ids using the data
		// table present in MaxDemian's database.

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

		reader = getReader( "itemdescs.txt" );

		while ( (data = readData( reader )) != null )
		{
			boolean isDescriptionId = true;
			if ( data.length >= 2 && data[1].length() > 0 )
			{
				isDescriptionId = true;
				for ( int i = 0; i < data[1].length() && isDescriptionId; ++i )
					if ( !Character.isDigit( data[1].charAt(i) ) )
						isDescriptionId = false;

				if ( isDescriptionId )
				{
					int itemId = StaticEntity.parseInt( data[0].trim() );
					descriptionById.set( itemId, data[1] );

					if ( data.length == 4 )
					{
						pluralById.set( itemId, data[3] );
						itemIdByPlural.put( getCanonicalName( data[3] ), new Integer( itemId ) );
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

		reader = getReader( "fullness.txt" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				String name = getCanonicalName( data[0] );
				fullnessByName.put( name, Integer.valueOf( data[1] ) );
				levelReqByName.put( name, Integer.valueOf( data[2] ) );

				if ( data.length > 3 )
				{
					addAdventureRange( name, StaticEntity.parseInt( data[1] ), data[3] );
					muscleByName.put( name, extractRange( data[4] ) );
					mysticalityByName.put( name, extractRange( data[5] ) );
					moxieByName.put( name, extractRange( data[6] ) );
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

		reader = getReader( "inebriety.txt" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				String name = getCanonicalName( data[0] );
				inebrietyByName.put( name, Integer.valueOf( data[1] ) );
				levelReqByName.put( name, Integer.valueOf( data[2] ) );

				if ( data.length > 3 )
				{
					addAdventureRange( name, StaticEntity.parseInt( data[1] ), data[3] );
					muscleByName.put( name, extractRange( data[4] ) );
					mysticalityByName.put( name, extractRange( data[5] ) );
					moxieByName.put( name, extractRange( data[6] ) );
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

		reader = getReader( "spleenhit.txt" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				String name = getCanonicalName( data[0] );
				spleenHitByName.put( name, Integer.valueOf( data[1] ) );
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

	private static final int getIncreasingGains( int value )
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

	private static final int getDecreasingGains( int value )
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

	private static final void addAdventureRange( String name, int unitCost, String range )
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
			gainSum1 += i + getIncreasingGains(i);
			gainSum2 += i + getDecreasingGains(i);
			gainSum3 += i + getIncreasingGains(i + getDecreasingGains(i));
		}

		float count = (end - start + 1);

		addAdventureRange( name, unitCost, false, false, (start + end) / 2.0f );
		addAdventureRange( name, unitCost, true, false, gainSum1 / count );
		addAdventureRange( name, unitCost, false, true, gainSum2 / count );
		addAdventureRange( name, unitCost, true, true, gainSum3 / count );
	}

	private static final void addAdventureRange( String name, int unitCost, boolean gainEffect1, boolean gainEffect2, float result )
	{
		// Adventure gains from zodiac signs based on information
		// provided on the Iocaine Powder forums.
		// http://www.iocainepowder.org/forums/viewtopic.php?t=2742

		getAdventureMap( false, false, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result ) );
		getAdventureMap( false, true, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result * 1.1f ) );

		getAdventureMap( true, false, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result / unitCost ) );
		getAdventureMap( true, true, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result * 1.1f / unitCost ) );
	}

	private static final Map getAdventureMap( boolean perUnit, boolean gainZodiac, boolean gainEffect1, boolean gainEffect2 )
	{	return advsByName[ perUnit ? 1 : 0 ][ gainZodiac ? 1 : 0 ][ gainEffect1 ? 1 : 0 ][ gainEffect2 ? 1 : 0 ];
	}

	private static final String extractRange( String range )
	{
		range = range.trim();

		boolean isNegative = range.startsWith( "-" );
		if ( range.startsWith( "-" ) )
		{
			isNegative = true;
			range = range.substring(1);
		}

		int dashIndex = range.indexOf( "-" );
		int start = StaticEntity.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );

		if ( dashIndex == -1 )
			return SINGLE_PRECISION_FORMAT.format( isNegative ? 0 - start : start );

		int end = StaticEntity.parseInt( range.substring( dashIndex + 1 ) );
		return SINGLE_PRECISION_FORMAT.format( (start + end) / (isNegative ? -2.0f : 2.0f) );
	}

	/**
	 * Takes an item name and constructs the likely Wiki equivalent
	 * of that item name.
	 */

	private static final String constructWikiName( String name )
	{	return StaticEntity.globalStringReplace( Character.toUpperCase( name.charAt(0) ) + name.substring(1), " ", "_" );
	}

	/**
	 * Utility method which searches for the plural version of
	 * the item on the KoL wiki.
	 */

	public static final void determineWikiData( String name )
	{
		String line = null;
		StringBuffer wikiRecord = new StringBuffer();

		try
		{
			BufferedReader reader = KoLDatabase.getReader( "http://kol.coldfront.net/thekolwiki/index.php/" + constructWikiName( name ) );
			while ( (line = reader.readLine()) != null )
				wikiRecord.append( line );

			String wikiData = wikiRecord.toString();

			Matcher itemMatcher = WIKI_ITEMID_PATTERN.matcher( wikiData );
			if ( !itemMatcher.find() )
			{
				RequestLogger.printLine( name + " did not match a valid an item entry." );
				return;
			}

			Matcher descMatcher = WIKI_DESCID_PATTERN.matcher( wikiData );
			if ( !descMatcher.find() )
			{
				RequestLogger.printLine( name + " did not match a valid an item entry." );
				return;
			}

			RequestLogger.printLine( "item: " + name + " (#" + itemMatcher.group(1) + ")" );
			RequestLogger.printLine( "desc: " + descMatcher.group(1) );

			Matcher pluralMatcher = WIKI_PLURAL_PATTERN.matcher( wikiData );
			if ( pluralMatcher.find() )
				RequestLogger.printLine( "plural: " + pluralMatcher.group(1) );

			Matcher sellMatcher = WIKI_AUTOSELL_PATTERN.matcher( wikiData );
			if ( sellMatcher.find() )
				RequestLogger.printLine( "autosell: " + sellMatcher.group(1) );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Temporarily adds an item to the item database.  This
	 * is used whenever KoLmafia encounters an unknown item
	 * in the mall or in the player's inventory.
	 */

	public static final void registerItem( int itemId, String itemName )
	{	registerItem( itemId, itemName, "" );
	}

	public static final void registerItem( int itemId, String itemName, String descriptionId )
	{
		if ( itemName == null )
			return;

		RequestLogger.printLine( "Unknown item found: " + itemName + " (#" + itemId + ")" );

		useTypeById.set( itemId, 0 );
		priceById.set( itemId, -1 );
		descriptionById.set( itemId, descriptionId );

		Integer id = new Integer( itemId );

		itemIdByName.put( getCanonicalName( itemName ), id );
		dataNameById.put( id, itemName );
		nameById.put( id, getDisplayName( itemName ) );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @return	The Id number of the corresponding item
	 */

	public static final int getItemId( String itemName )
	{	return getItemId( itemName, 1 );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @param	count		How many there are
	 * @return	The Id number of the corresponding item
	 */

	public static final int getItemId( String itemName, int count )
	{
		if ( itemName == null || itemName.length() == 0 )
			return -1;

		// Get the canonical name of the item, and attempt
		// to parse based on that.

		String canonicalName = getCanonicalName( itemName );
		Object itemId = itemIdByName.get( canonicalName );

		// If the name, as-is, exists in the item database,
		// then go ahead and return the item Id.

		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Work around a specific KoL bug: the "less-than-three-shaped
		// box" is sometimes listed as a "less-than-three- shaped box"
		if ( canonicalName.equals( "less-than-three- shaped box" ) )
			return 1168;

		// See if it's a weird pluralization with a pattern we can't
		// guess.

		itemId = itemIdByPlural.get( canonicalName );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// It's possible that you're looking for a substring.  In
		// that case, prefer complete versions containing the substring
		// over truncated versions which are plurals.

		List possibilities = getMatchingNames( itemIdByName, canonicalName );
		if ( possibilities.size() == 1 )
			return getItemId( (String) possibilities.get(0), count );

		// Abort if it's clearly not going to be a plural,
		// since this might kill off multi-item detection.

		if ( count < 2 )
			return -1;

		// Or maybe it's a standard plural where they just add a letter
		// to the end.

		itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a snowcone, then reverse the word order
		if ( canonicalName.startsWith( "snowcones" ) )
			return getItemId( canonicalName.split( " " )[1] + " snowcone", count );

		// Lo mein has this odd pluralization where there's a dash
		// introduced into the name when no such dash exists in the
		// singular form.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "-", " " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// The word right before the dash may also be pluralized,
		// so make sure the dashed words are recognized.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "es-", "-" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "s-", "-" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a plural form of "tooth", then make
		// sure that it's handled.  Other things which
		// also have "ee" plural forms should be clumped
		// in as well.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ee", "oo" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also handle the plural of vortex, which is
		// "vortices" -- this should only appear in the
		// meat vortex, but better safe than sorry.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ex" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Handling of appendices (which is the plural
		// of appendix, not appendex, so it is not caught
		// by the previous test).

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ix" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also add in a special handling for knives
		// and other things ending in "ife".

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ives", "ife" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also add in a special handling for elves
		// and other things ending in "f".

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ves", "f" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also add in a special handling for staves
		// and other things ending in "aff".

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "aves", "aff" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a pluralized form of something that
		// ends with "y", then return the appropriate
		// item Id for the "y" version.

		if ( canonicalName.endsWith( "ies" ) )
		{
			itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 3 ) + "y" );
			if ( itemId != null )
				return ((Integer)itemId).intValue();
		}

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ies ", "y " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a pluralized form of something that
		// ends with "o", then return the appropriate
		// item Id for the "o" version.

		if ( canonicalName.endsWith( "es" ) )
		{
			itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 2 ) );
			if ( itemId != null )
				return ((Integer)itemId).intValue();
		}

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "es ", " " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a pluralized form of something that
		// ends with "an", then return the appropriate
		// item Id for the "en" version.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "en ", "an " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a standard pluralized forms, then
		// return the appropriate item Id.

		itemId = itemIdByName.get( canonicalName.replaceFirst( "([A-Za-z])s ", "$1 " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's something that ends with 'i', then
		// it might be a singular ending with 'us'.

		if ( canonicalName.endsWith( "i" ) )
		{
			itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) + "us" );
			if ( itemId != null )
				return ((Integer)itemId).intValue();
		}

		// Attempt to find the item name by brute force
		// by checking every single space location.  Do
		// this recursively for best results.

		int lastSpaceIndex = canonicalName.indexOf( " " );
		return lastSpaceIndex != -1 ? getItemId( canonicalName.substring( lastSpaceIndex ).trim(), count ) : -1;
	}

	public static final boolean meetsLevelRequirement( String name )
	{
		if ( name == null )
			return false;

		Integer requirement = (Integer) levelReqByName.get( getCanonicalName( name ) );
		return requirement == null ? false : KoLCharacter.getLevel() >= requirement.intValue();
	}

	public static final int getFullness( String name )
	{
		if ( name == null )
			return 0;

		Integer fullness = (Integer) fullnessByName.get( getCanonicalName( name ) );
		return fullness == null ? 0 : fullness.intValue();
	}

	public static final int getInebriety( String name )
	{
		if ( name == null )
			return 0;

		Integer inebriety = (Integer) inebrietyByName.get( getCanonicalName( name ) );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	public static final int getSpleenHit( String name )
	{
		if ( name == null )
			return 0;

		Integer spleenhit = (Integer) spleenHitByName.get( getCanonicalName( name ) );
		return spleenhit == null ? 0 : spleenhit.intValue();
	}

	public static final String getAdventureRange( String name )
	{
		if ( name == null )
			return "+0.0";

		int fullness = getFullness( name );
		int inebriety = getInebriety( name );

		String range = null;

		if ( fullness > 0 )
		{
			range = (String) getAdventureMap(
				StaticEntity.getBooleanProperty( "showGainsPerUnit" ),
				KoLCharacter.getSign().indexOf( "Opossum" ) != -1,
				activeEffects.contains( GOT_MILK ),
				StaticEntity.getIntegerProperty( "munchiesPillsUsed" ) > 0 ).get( getCanonicalName( name ) );
		}
		else if ( inebriety > 0 )
		{
			range = (String) getAdventureMap(
				StaticEntity.getBooleanProperty( "showGainsPerUnit" ),
				KoLCharacter.getSign().indexOf( "Blender" ) != -1,
				activeEffects.contains( ODE ),
				false ).get( getCanonicalName( name ) );
		}

		if ( range == null )
			return "+0.0";

		return range;
	}

	public static final String getMuscleRange( String name )
	{
		if ( name == null )
			return "+0.0";

		String range = (String) muscleByName.get( getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMysticalityRange( String name )
	{
		if ( name == null )
			return "+0.0";

		String range = (String) mysticalityByName.get( getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMoxieRange( String name )
	{
		if ( name == null )
			return "+0.0";

		String range = (String) moxieByName.get( getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	/**
	 * Returns the price for the item with the given Id.
	 * @return	The price associated with the item
	 */

	public static final int getPriceById( int itemId )
	{	return priceById.get( itemId );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isTradeable( int itemId )
	{	return tradeableById.get( itemId );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isGiftable( int itemId )
	{	return giftableById.get( itemId );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isDisplayable( int itemId )
	{	return displayableById.get( itemId );
	}

	/**
	 * Returns true if the item is a bounty, otherwise false
	 * @return	true if item is a bounty
	 */

	public static final boolean isBountyItem( int itemId )
	{
		return	(itemId >= 2099 && itemId <= 2107) ||
			(itemId >= 2407 && itemId <= 2415) ||
			(itemId >= 2468 && itemId <= 2473);
	}

	/**
	 * Returns the name for an item, given its Id number.
	 * @param	itemId	The Id number of the item to lookup
	 * @return	The name of the corresponding item
	 */

	public static final String getItemName( int itemId )
	{	return (String) nameById.get( new Integer( itemId ) );
	}

	/**
	 * Returns a list of all items which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on items.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( itemIdByName, substring );
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String itemName )
	{	return getItemId( itemName ) != -1;
	}

	/**
	 * Returns whether or not the item with the given name
	 * is usable (this includes edibility).
	 *
	 * @return	<code>true</code> if the item is usable
	 */

	public static final boolean isUsable( String itemName )
	{
		int itemId = getItemId( itemName );
		if ( itemId <= 0 )
			return false;

		switch ( useTypeById.get( itemId ) )
		{
		case CONSUME_EAT:
		case CONSUME_DRINK:
		case CONSUME_USE:
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
	 * @return	The consumption associated with the item
	 */

	public static final int getConsumptionType( int itemId )
	{	return itemId <= 0 ? NO_CONSUME : useTypeById.get( itemId );
	}

	public static final int getConsumptionType( String itemName )
	{	return getConsumptionType( getItemId( itemName ) );
	}

	/**
	 * Returns the item description Id used by the given
	 * item, given its item Id.
	 *
	 * @return	The description Id associated with the item
	 */

	public static final String getDescriptionId( int itemId )
	{	return descriptionById.get( itemId );
	}

	/**
	 * Returns the set of item names keyed by id
	 * @return	The set of item names keyed by id
	 */

	public static final Set entrySet()
	{	return nameById.entrySet();
	}

	/**
	 * Returns the largest item ID
	 * @return	The largest item ID
	 */

	public static final int maxItemId()
	{	return maxItemId;
	}

	private static final Pattern CLOSET_ITEM_PATTERN = Pattern.compile( "<option value='(\\d+)' descid='(.*?)'>(.*?) \\(" );
	private static final Pattern DESCRIPTION_PATTERN = Pattern.compile( "onClick='descitem\\((\\d+)\\);'></td><td valign=top><b>(.*?)</b>" );

	public static final void findItemDescriptions()
	{
		RequestLogger.printLine( "Checking for new non-quest items..." );

		KoLRequest updateRequest = new EquipmentRequest( EquipmentRequest.CLOSET );
		RequestThread.postRequest( updateRequest );
		Matcher itemMatcher = CLOSET_ITEM_PATTERN.matcher( updateRequest.responseText );

		boolean foundChanges = false;

		while ( itemMatcher.find() )
		{
			int itemId = parseInt( itemMatcher.group(1) );
			if ( !descriptionById.get( itemId ).equals( "" ) )
				continue;

			foundChanges = true;
			registerItem( itemId, itemMatcher.group(3), itemMatcher.group(2) );
		}

		RequestLogger.printLine( "Parsing for quest items..." );

		for ( int pageId = 1; pageId <= 3; ++pageId )
		{
			RequestThread.postRequest( KoLRequest.VISITOR.constructURLString( "inventory.php?which=" + pageId ) );
			itemMatcher = DESCRIPTION_PATTERN.matcher( KoLRequest.VISITOR.responseText );

			while ( itemMatcher.find() )
			{
				String itemName = itemMatcher.group(2);
				int itemId = TradeableItemDatabase.getItemId( itemName );

				if ( itemId == -1 )
					continue;

				if ( !descriptionById.get( itemId ).equals( "" ) )
					continue;

				foundChanges = true;
				registerItem( itemId, itemName, itemMatcher.group(1) );
			}
		}

		if ( foundChanges )
			saveDataOverride();
	}

	public static final void saveDataOverride()
	{
		File output = new File( DATA_DIRECTORY, "tradeitems.txt" );
		LogStream writer = LogStream.openStream( output, true );

		int lastInteger = 1;
		Iterator it = nameById.keySet().iterator();

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
				writer.println(i);

			lastInteger = nextInteger.intValue() + 1;

			if ( accessById.containsKey( nextInteger ) )
			{
				writer.println( nextInteger + "\t" + dataNameById.get( nextInteger ) + "\t" +
					useTypeById.get( nextInteger.intValue() ) + "\t" +
					accessById.get( nextInteger ) + "\t" + priceById.get( nextInteger.intValue() ) );
			}
			else
			{
				writer.println( nextInteger + "\t" + dataNameById.get( nextInteger ) +
					"\t0\tunknown\t-1" );
			}
		}

		writer.close();

		output = new File( DATA_DIRECTORY, "itemdescs.txt" );
		writer = LogStream.openStream( output, true );

		for ( int i = 0; i < descriptionById.size(); ++i )
		{
			if ( descriptionById.get(i).equals( "" ) )
				continue;

			writer.println( i + "\t" + descriptionById.get(i) + "\t" + nameById.get( new Integer(i) ) + "\t" + pluralById.get(i) );
		}

		writer.close();
	}

	// Support for dusty bottles of wine

	public static final void identifyDustyBottles()
	{
		int lastAscension = StaticEntity.getIntegerProperty( "lastDustyBottleReset" );
		if ( lastAscension == KoLCharacter.getAscensions() )
			return;

		KoLmafia.updateDisplay( "Identifying dusty bottles..." );

		// Identify the six dusty bottles

		for ( int i = 2271; i <= 2276; ++i )
			identifyDustyBottle( i );

		StaticEntity.setProperty( "lastDustyBottleReset", String.valueOf( KoLCharacter.getAscensions() ) );

		// Set the consumption data

		setDustyBottles();
	}

	private static final Pattern GLYPH_PATTERN = Pattern.compile( "Arcane Glyph #(\\d)" );

	private static final void identifyDustyBottle( int itemId )
	{
		String glyph = "";

		String description = rawDescriptionText( itemId );
		if ( description != null )
		{
			Matcher matcher = GLYPH_PATTERN.matcher( description );
			if ( matcher.find() )
				glyph = matcher.group(1);
		}

		StaticEntity.setProperty( "lastDustyBottle" + itemId, glyph );
	}

	public static final void getDustyBottles()
	{
		int lastAscension = StaticEntity.getIntegerProperty( "lastDustyBottleReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			for ( int i = 2271; i <= 2276; ++i )
				StaticEntity.setProperty( "lastDustyBottle" + i, "" );
		}

		setDustyBottles();
	}

	private static final void setDustyBottles()
	{
		setDustyBottle( 2271 );
		setDustyBottle( 2272 );
		setDustyBottle( 2273 );
		setDustyBottle( 2274 );
		setDustyBottle( 2275 );
		setDustyBottle( 2276 );
	}

	private static final void setDustyBottle( int itemId )
	{
		int glyph = StaticEntity.getIntegerProperty( "lastDustyBottle" + itemId );
		switch ( glyph )
		{
		case 0:
			// Unidentified
			setDustyBottle( itemId, 2, "0", "0", "0", "0" );
			break;
		case 1:
			// "You drink the wine. You've had better, but you've
			// had worse."
			setDustyBottle( itemId, 2, "4-5", "6-8", "5-7", "5-9" );
			break;
		case 2:
			// "You guzzle the entire bottle of wine before you
			// realize that it has turned into vinegar. Bleeah."
			setDustyBottle( itemId, 0, "0", "0", "0", "0" );
			break;
		case 3:
			// "You drink the bottle of wine, then belch up a cloud
			// of foul-smelling green smoke. Looks like this wine
			// was infused with wormwood. Spoooooooky."
			setDustyBottle( itemId, 2, "3-4", "3-6", "15-18", "3-6" );
			break;
		case 4:
			// "This wine is fantastic! You gulp down the entire
			// bottle, and feel great!"
			setDustyBottle( itemId, 2, "7-10", "10-15", "10-15", "10-15" );
			break;
		case 5:
			// "You drink the wine. It tastes pretty good, but when
			// you get to the bottom, it's full of sediment, which
			// turns out to be powdered glass. Ow."
			setDustyBottle( itemId, 2, "4-5", "7-10", "5-7", "8-10" );
			break;
		case 6:
			// "You drink the wine, but it seems to have gone
			// bad. Not in the "turned to vinegar" sense, but the
			// "turned to crime" sense. It perpetrates some
			// violence against you on the inside."
			setDustyBottle( itemId, 2, "0-1", "0", "0", "0" );
			break;
		}
	}

	private static final void setDustyBottle( int itemId, int inebriety, String adventures, String muscle, String mysticality, String moxie )
	{
		String name = getCanonicalName( (String)dataNameById.get( new Integer( itemId ) ) );

		inebrietyByName.put( name, new Integer( inebriety ) );
		addAdventureRange( name, inebriety, adventures );
		muscleByName.put( name, extractRange( muscle ) );
		mysticalityByName.put( name, extractRange( mysticality ) );
		moxieByName.put( name, extractRange( moxie ) );
	}

	public static final String dustyBottleType( int itemId )
	{
		int glyph = StaticEntity.getIntegerProperty( "lastDustyBottle" + itemId );
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

	public static final void checkInternalData( int itemId )
	{
		loadScrapeData();
		RequestLogger.printLine( "Checking internal data..." );
		LogStream report = LogStream.openStream( new File( DATA_DIRECTORY, "itemdata.txt" ), true );

		foods.clear();
		boozes.clear();
		hats.clear();
		weapons.clear();
		offhands.clear();
		shirts.clear();
		pants.clear();
		accessories.clear();
		containers.clear();
		famitems.clear();

		// Check item names, desc ID, consumption type

		if ( itemId == 0 )
		{
			for ( int i = 1; i < descriptionById.size(); ++i )
				checkItem( i, report );

			String description;
			LogStream livedata = LogStream.openStream( new File( DATA_DIRECTORY, "itemhtml.txt" ), true );

			for ( int i = 1; i < descriptionById.size(); ++i )
			{
				description = rawDescriptions.get(i);
				if ( !description.equals( "" ) )
				{
					livedata.println( i );
					livedata.println( description );
				}
			}

			livedata.close();
		}
		else
		{
			checkItem( itemId, report );
		}

		// Check level limits, equipment, modifiers

		checkLevels( report );
		checkEquipment( report );
		checkModifiers( report );

		report.close();
	}

	private static final void checkItem( int itemId, LogStream report )
	{
		Integer id = new Integer( itemId );

		String name = (String)dataNameById.get( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String rawText = rawDescriptionText( itemId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has no description." );
			return;
		}

		String text = descriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has malformed description text." );
			return;
		}

		String descriptionName = parseName( text );
		if ( !name.equals( descriptionName ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has description of " + descriptionName + "." );
			return;

		}

		boolean correct = true;

		int type = getConsumptionType( itemId );
		String descType = parseType( text );
		if ( !typesMatch( type, descType ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has consumption type of " + type + " but is described as " + descType + "." );
			correct = false;

		}

		int price = priceById.get( itemId );
		int descPrice = parsePrice( text );
		if ( price != descPrice )
		{
			report.println( "# *** " + name + " (" + itemId + ") has price of " + price + " but should be " + descPrice + "." );
			correct = false;

		}

		String access = (String)accessById.get( id );
		String descAccess = parseAccess( text );
		if ( !access.equals( descAccess ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has access of " + access + " but should be " + descAccess + "." );
			correct = false;

		}

		switch ( type )
		{
		case CONSUME_EAT:
			foods.put( name, text );
			break;
		case CONSUME_DRINK:
			boozes.put( name, text );
			break;
		case EQUIP_HAT:
			hats.put( name, text );
			break;
		case EQUIP_PANTS:
			pants.put( name, text );
			break;
		case EQUIP_SHIRT:
			shirts.put( name, text );
			break;
		case EQUIP_WEAPON:
			weapons.put( name, text );
			break;
		case EQUIP_OFFHAND:
			offhands.put( name, text );
			break;
		case EQUIP_ACCESSORY:
			accessories.put( name, text );
			break;
		case EQUIP_CONTAINER:
			containers.put( name, text );
			break;
		case EQUIP_FAMILIAR:
			famitems.put( name, text );
			break;
		}

		report.println( itemId + "\t" + name + "\t" + type + "\t" + descAccess + "\t" + descPrice );
	}

	private static final KoLRequest DESC_REQUEST = new KoLRequest( "desc_item.php" );

	private static final String rawDescriptionText( int itemId )
	{
		String descId = descriptionById.get( itemId );
		if ( descId == null || descId.equals( "" ) )
			return null;

		String previous = rawDescriptions.get( itemId );
		if ( previous != null && !previous.equals( "" ) )
			return previous;

		DESC_REQUEST.addFormField( "whichitem", descId );
		RequestThread.postRequest( DESC_REQUEST );
		rawDescriptions.set( itemId, DESC_REQUEST.responseText );

		return DESC_REQUEST.responseText;
	}

	private static final void loadScrapeData()
	{
		if ( rawDescriptions.size() > 0 )
			return;

		try
		{
			File saveData = new File( DATA_DIRECTORY, "itemhtml.txt" );
			if ( !saveData.exists() )
				return;

			RequestLogger.printLine( "Loading previous data..." );

			String currentLine;
			StringBuffer currentHTML = new StringBuffer();
			BufferedReader reader = KoLDatabase.getReader( saveData );

			while ( !(currentLine = reader.readLine()).equals( "" ) )
			{
				currentHTML.setLength(0);
				int currentId = StaticEntity.parseInt( currentLine );

				do
				{
					currentLine = reader.readLine();
					currentHTML.append( currentLine );
					currentHTML.append( LINE_BREAK );
				}
				while ( !currentLine.equals( "</html>" ) );

				rawDescriptions.set( currentId, currentHTML.toString() );
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

	private static final String descriptionText( String rawText )
	{
		Matcher matcher = DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
			return null;

		return matcher.group(1);
	}

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>(.*?)</b>" );

	private static final String parseName( String text )
	{
		Matcher matcher = NAME_PATTERN.matcher( text );
		if ( !matcher.find() )
			return "";

		// One item is known to have an extra internal space
		return StaticEntity.globalStringReplace( matcher.group(1), "  ", " " );
	}

	private static final Pattern PRICE_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	private static final int parsePrice( String text )
	{
		Matcher matcher = PRICE_PATTERN.matcher( text );
		if ( !matcher.find() )
			return 0;

		return StaticEntity.parseInt( matcher.group(1) );
	}

	private static final String parseAccess( String text )
	{
		if ( text.indexOf( "Quest Item" ) != -1 )
			return "none";

		if ( text.indexOf( "Gift Item" ) != -1 )
			return "gift";

		if ( text.indexOf( "Cannot be traded" ) != -1 )
			return "display";

		return "all";
	}

	private static final Pattern TYPE_PATTERN = Pattern.compile( "Type: <b>(.*?)</b>" );

	private static final String parseType( String text )
	{
		Matcher matcher = TYPE_PATTERN.matcher( text );
		if ( !matcher.find() )
			return "";

		return matcher.group(1);
	}

	private static final boolean typesMatch( int type, String descType )
	{
		if ( descType.equals( "" ) )
			return true;

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

	private static final void checkLevels( LogStream report )
	{

		RequestLogger.printLine( "Checking level requirements..." );

		checkLevelMap( report, foods, "Food" );
		checkLevelMap( report, boozes, "Booze" );
	}

	private static final void checkLevelMap( LogStream report, Map map, String tag )
	{
		if ( map.size() == 0 )
			return;

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# Level requirements in " + ( map == foods ? "fullness" : "inebriety" ) + ".txt" );

		Object [] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String)keys[i];
			String text = (String)map.get( name );
			checkLevelDatum( name, text, report );
		}
	}

	private static final void checkLevelDatum( String name, String text, LogStream report )
	{
		Integer requirement = (Integer)levelReqByName.get( getCanonicalName( name ) );
		int level = requirement == null ? 0 : requirement.intValue();
		int descLevel = parseLevel( text );
		if ( level != descLevel )
			report.println( "# *** " + name + " requires level " + level + " but should be " + descLevel + "." );
	}

	private static final Pattern LEVEL_PATTERN = Pattern.compile( "Level required: <b>(.*?)</b>" );

	private static final int parseLevel( String text )
	{
		Matcher matcher = LEVEL_PATTERN.matcher( text );
		if ( !matcher.find() )
			return 1;

		return StaticEntity.parseInt( matcher.group(1) );
	}

	private static final void checkEquipment( LogStream report )
	{

		RequestLogger.printLine( "Checking equipment..." );

		checkEquipmentMap( report, hats, "Hats" );
		checkEquipmentMap( report, pants, "Pants" );
		checkEquipmentMap( report, shirts, "Shirts" );
		checkEquipmentMap( report, weapons, "Weapons" );
		checkEquipmentMap( report, offhands, "Off-hand" );
		checkEquipmentMap( report, accessories, "Accessories" );
		checkEquipmentMap( report, containers, "Containers" );
	}

	private static final void checkEquipmentMap( LogStream report, Map map, String tag )
	{
		if ( map.size() == 0 )
			return;

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# " + tag + " section of equipment.txt" );
		report.println();

		Object [] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String)keys[i];
			String text = (String)map.get( name );
			checkEquipmentDatum( name, text, report );
		}
	}

	private static final Pattern POWER_PATTERN = Pattern.compile( "Power: <b>(\\d+)</b>" );
	private static final Pattern REQ_PATTERN = Pattern.compile( "(\\w+) Required: <b>(\\d+)</b>" );
	private static final Pattern WEAPON_PATTERN = Pattern.compile( "weapon [(](.*?)[)]" );

	private static final void checkEquipmentDatum( String name, String text, LogStream report )
	{
		Matcher matcher;

		String type = parseType( text );
		boolean isWeapon = false, isShield = false, hasPower = false;

		if ( type.indexOf( "weapon" ) != -1 )
			isWeapon = true;
		else if ( type.indexOf( "shield" ) != -1 )
			isShield = true;
		else if ( type.indexOf( "hat" ) != -1 || type.indexOf( "pants" ) != -1	|| type.indexOf( "shirt" ) != -1 )
			hasPower = true;

		int power = 0;
		if ( isWeapon || hasPower )
		{
			matcher = POWER_PATTERN.matcher( text );
			if ( matcher.find() )
				power = StaticEntity.parseInt( matcher.group(1) );
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
			matcher = WEAPON_PATTERN.matcher( text );
			if ( matcher.find() )
				weaponType = matcher.group(1);
		}

		String req = "none";

		matcher = REQ_PATTERN.matcher( text );
		if (matcher.find() )
		{
			String stat = matcher.group(1);
			if ( stat.equals( "Muscle" ) )
				req = "Mus: " + matcher.group(2);
			else if ( stat.equals( "Mysticality" ) )
				req = "Mys: " + matcher.group(2);
			else if ( stat.equals( "Moxie" ) )
				req = "Mox: " + matcher.group(2);
		}
		else if ( isWeapon )
		{
			if ( type.indexOf( "ranged" ) != -1 )
				req = "Mox: 0";
			else if ( weaponType.indexOf( "utensil" ) != -1 || weaponType.indexOf( "saucepan" ) != -1 || weaponType.indexOf( "chefstaff" ) != -1 )
				req = "Mys: 0";
			else
				req = "Mus: 0";
		}

		// Now check against what we already have
		int oldPower = EquipmentDatabase.getPower( name );
		if ( power != oldPower )
			report.println( "# *** " + name + " has power " + oldPower + " but should be " + power + "." );

		String oldReq = EquipmentDatabase.getReq( name );
		if ( !req.equals( oldReq ) )
			report.println( "# *** " + name + " has requirement " + oldReq + " but should be " + req + "." );

		if ( isWeapon )
		{
			String oldWeaponType = String.valueOf( EquipmentDatabase.getHands( name ) ) + "-handed " + EquipmentDatabase.getType( name );
			if ( !weaponType.equals( oldWeaponType ) )
				report.println( "# *** " + name + " has weapon type " + oldWeaponType + " but should be " + weaponType + "." );
		}
		else if ( isShield && power == 0 )
		{
			report.println( "# *** " + name + " is a shield of unknown power." );
		}

		if ( isWeapon )
			report.println( name + "\t" + power + "\t" + req  + "\t" + weaponType );
		else
			report.println( name + "\t" + power + "\t" + req );
	}

	private static final void checkModifiers( LogStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );
		ArrayList unknown = new ArrayList();

		checkModifierMap( report, hats, "Hats", unknown );
		checkModifierMap( report, pants, "Pants", unknown );
		checkModifierMap( report, shirts, "Shirts", unknown );
		checkModifierMap( report, weapons, "Weapons", unknown );
		checkModifierMap( report, offhands, "Off-hand", unknown );
		checkModifierMap( report, accessories, "Accessories", unknown );
		checkModifierMap( report, containers, "Containers", unknown );
		checkModifierMap( report, famitems, "Familiar Items", unknown );

		if ( unknown.size() == 0 )
			return;

		Collections.sort( unknown );

		for ( int i = 0; i < 10; ++i )
			report.println();

		report.println( "# Unknown Modifiers section of modifiers.txt" );
		report.println();

		for ( int i = 0; i < unknown.size(); ++i )
			report.println( "# " + (String)unknown.get(i) );
	}

	private static final void checkModifierMap( LogStream report, Map map, String tag, ArrayList unknown )
	{
		if ( map.size() == 0 )
			return;

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println();
		report.println( "# " + tag + " section of modifiers.txt" );
		report.println();

		Object [] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String)keys[i];
			String text = (String)map.get( name );
			checkModifierDatum( name, text, report, unknown );
		}
	}

	private static final void checkModifierDatum( String name, String text, LogStream report, ArrayList unknown )
	{
		String known = parseEnchantments( text, unknown );

		if ( known.equals( "" ) )
			report.println( "# " + name );
		else
			report.println( name + "\t" + known );
	}

	private static final Pattern ENCHANTMENT_PATTERN = Pattern.compile( "Enchantment:.*?<font color=blue>(.*)</font>", Pattern.DOTALL );

	private static final String parseEnchantments( String text, ArrayList unknown )
	{
		String known = "";

		// Several modifiers can appear outside the "Enchantments"
		// section of the item description.

		String dr = Modifiers.parseDamageReduction( text );
		if ( dr != null )
		{
			if ( !known.equals( "" ) )
				known += ", ";
			known += dr;
		}

		String single = Modifiers.parseSingleEquip( text );
		if ( single != null )
		{
			if ( !known.equals( "" ) )
				known += ", ";
			known += single;
		}

		Matcher matcher = ENCHANTMENT_PATTERN.matcher( text );
		if ( !matcher.find() )
			return known;

		StringBuffer enchantments = new StringBuffer( matcher.group(1) );

		StaticEntity.globalStringDelete( enchantments, "<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total." );
		StaticEntity.globalStringDelete( enchantments, "<b>NOTE:</b> If you wear multiple items that increase Critical Hit chances, only the highest multiplier applies." );
		StaticEntity.globalStringReplace( enchantments, "<br>", "\n" );

		String [] mods = enchantments.toString().split( "\n+" );
		for ( int i = 0; i < mods.length; ++i )
		{
			String enchantment = mods[i].trim();

			if ( enchantment.equals( "" ) )
				continue;

			String mod = Modifiers.parseModifier( enchantment );
			if ( mod != null )
			{
				if ( !known.equals( "" ) )
					known += ", ";
				known += mod;
				continue;
			}

			if ( !unknown.contains( enchantment ) )
				unknown.add( enchantment );
		}

		return known;
	}
}
