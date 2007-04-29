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

	private static IntegerArray useTypeById = new IntegerArray();
	private static IntegerArray priceById = new IntegerArray();

	private static StringArray descById = new StringArray();

	private static Map nameByItemId = new TreeMap();
	private static Map itemIdByName = new TreeMap();
	private static Map itemIdByPlural = new TreeMap();

	private static Map fullnessByName = new TreeMap();
	private static Map inebrietyByName = new TreeMap();
	private static Map spleenHitByName = new TreeMap();

	private static Map [][][][] advsByName = new TreeMap[2][2][2][2];

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

	private static Map muscleByName = new TreeMap();
	private static Map mysticalityByName = new TreeMap();
	private static Map moxieByName = new TreeMap();

	private static BooleanArray tradeableById = new BooleanArray();
	private static BooleanArray giftableById = new BooleanArray();
	private static BooleanArray displayableById = new BooleanArray();

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
				nameByItemId.put( id, getDisplayName( data[1] ) );

				tradeableById.set( itemId, data[3].equals( "all" ) );
				giftableById.set( itemId, data[3].equals( "all" ) || data[3].equals( "gift" ) );
				displayableById.set( itemId, data[3].equals( "all" ) || data[3].equals( "gift" ) || data[3].equals( "display" ) );
			}
		}

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
					descById.set( itemId, data[1] );
					if ( data.length == 4 )
						itemIdByPlural.put( getCanonicalName( data[3] ), new Integer( itemId ) );
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
			if ( data.length >= 2 )
			{
				String name = getCanonicalName( data[0] );
				fullnessByName.put( name, Integer.valueOf( data[1] ) );

				if ( data.length > 2 )
				{
					addAdventureRange( name, StaticEntity.parseInt( data[1] ), data[2] );
					muscleByName.put( name, extractRange( data[3] ) );
					mysticalityByName.put( name, extractRange( data[4] ) );
					moxieByName.put( name, extractRange( data[5] ) );
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
			if ( data.length >= 2 )
			{
				String name = getCanonicalName( data[0] );
				inebrietyByName.put( name, Integer.valueOf( data[1] ) );

				if ( data.length > 2 )
				{
					addAdventureRange( name, StaticEntity.parseInt( data[1] ), data[2] );
					muscleByName.put( name, extractRange( data[3] ) );
					mysticalityByName.put( name, extractRange( data[4] ) );
					moxieByName.put( name, extractRange( data[5] ) );
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

	private static int getIncreasingGains( int value )
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

	private static int getDecreasingGains( int value )
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

	private static void addAdventureRange( String name, int unitCost, String range )
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

		float count = (float) (end - start + 1);

		addAdventureRange( name, unitCost, false, false, (start + end) / 2.0f );
		addAdventureRange( name, unitCost, true, false, gainSum1 / count );
		addAdventureRange( name, unitCost, false, true, gainSum2 / count );
		addAdventureRange( name, unitCost, true, true, gainSum3 / count );
	}

	private static void addAdventureRange( String name, int unitCost, boolean gainEffect1, boolean gainEffect2, float result )
	{
		// Adventure gains from zodiac signs based on information
		// provided on the Iocaine Powder forums.
		// http://www.iocainepowder.org/forums/viewtopic.php?t=2742

		getAdventureMap( false, false, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result ) );
		getAdventureMap( false, true, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result * 1.1f ) );

		getAdventureMap( true, false, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result / unitCost ) );
		getAdventureMap( true, true, gainEffect1, gainEffect2 ).put( name, SINGLE_PRECISION_FORMAT.format( result * 1.1f / unitCost ) );
	}

	private static Map getAdventureMap( boolean perUnit, boolean gainZodiac, boolean gainEffect1, boolean gainEffect2 )
	{	return advsByName[ perUnit ? 1 : 0 ][ gainZodiac ? 1 : 0 ][ gainEffect1 ? 1 : 0 ][ gainEffect2 ? 1 : 0 ];
	}

	private static String extractRange( String range )
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

	private static String constructWikiName( String name )
	{	return StaticEntity.globalStringReplace( Character.toUpperCase( name.charAt(0) ) + name.substring(1), " ", "_" );
	}

	/**
	 * Utility method which searches for the plural version of
	 * the item on the KoL wiki.
	 */

	public static void determineWikiData( String name )
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

	public static void registerItem( int itemId, String itemName )
	{
		if ( itemName == null )
			return;

		RequestLogger.printLine( "Unknown item found: " + itemName + " (#" + itemId + ")" );

		useTypeById.set( itemId, 0 );
		priceById.set( itemId, -1 );
		descById.set( itemId, "" );

		Integer id = new Integer( itemId );

		itemIdByName.put( getCanonicalName( itemName ), id );
		nameByItemId.put( id, getDisplayName( itemName ) );
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
	 * Returns the name for an item, given its Id number.
	 * @param	itemId	The Id number of the item to lookup
	 * @return	The name of the corresponding item
	 */

	public static final String getItemName( int itemId )
	{	return (String) nameByItemId.get( new Integer( itemId ) );
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
	{	return descById.get( itemId );
	}

	/**
	 * Returns the set of items keyed by name
	 * @return	The set of items keyed by name
	 */

	public static Set entrySet()
	{	return nameByItemId.entrySet();
	}
}
