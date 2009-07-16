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
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DebugDatabase
	extends KoLDatabase
{
	private static final Pattern WIKI_ITEMID_PATTERN = Pattern.compile( "Item number</a>:</b> (\\d+)<br />" );
	private static final Pattern WIKI_DESCID_PATTERN = Pattern.compile( "<b>Description ID:</b> (\\d+)<br />" );
	private static final Pattern WIKI_PLURAL_PATTERN =
		Pattern.compile( "\\(.*?In-game plural</a>: <i>(.*?)</i>\\)", Pattern.DOTALL );
	private static final Pattern WIKI_AUTOSELL_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

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
				FileUtilities.getReader( "http://kol.coldfront.net/thekolwiki/index.php/" + DebugDatabase.constructWikiName( name ) );
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
		String wikiData = DebugDatabase.readWikiData( name );

		Matcher itemMatcher = DebugDatabase.WIKI_ITEMID_PATTERN.matcher( wikiData );
		if ( !itemMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		Matcher descMatcher = DebugDatabase.WIKI_DESCID_PATTERN.matcher( wikiData );
		if ( !descMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		RequestLogger.printLine( "item: " + name + " (#" + itemMatcher.group( 1 ) + ")" );
		RequestLogger.printLine( "desc: " + descMatcher.group( 1 ) );

		Matcher pluralMatcher = DebugDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
		if ( pluralMatcher.find() )
		{
			RequestLogger.printLine( "plural: " + pluralMatcher.group( 1 ) );
		}

		Matcher sellMatcher = DebugDatabase.WIKI_AUTOSELL_PATTERN.matcher( wikiData );
		if ( sellMatcher.find() )
		{
			RequestLogger.printLine( "autosell: " + sellMatcher.group( 1 ) );
		}
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
		Matcher itemMatcher = DebugDatabase.CLOSET_ITEM_PATTERN.matcher( updateRequest.responseText );

		boolean foundChanges = false;

		while ( itemMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String descId = ItemDatabase.getDescriptionId( itemId );
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
		itemMatcher = DebugDatabase.DESCRIPTION_PATTERN.matcher( itemChecker.responseText );

		while ( itemMatcher.find() )
		{
			String itemName = itemMatcher.group( 2 );
			int itemId = ItemDatabase.getItemId( itemName );

			if ( itemId == -1 )
			{
				continue;
			}

			if ( !ItemDatabase.getDescriptionId( itemId ).equals( "" ) )
			{
				continue;
			}

			foundChanges = true;
			ItemDatabase.registerItem( itemId, itemName, itemMatcher.group( 1 ) );
		}

		if ( foundChanges )
		{
			DebugDatabase.saveDataOverride();
		}
	}

	public static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "tradeitems.txt" );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.TRADEITEMS_VERSION );

		int lastInteger = 1;
		Iterator it = ItemDatabase.nameByIdKeySet().iterator();

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			int id = nextInteger.intValue();

			for ( int i = lastInteger; i < id; ++i )
			{
				writer.println( i );
			}

			lastInteger = id + 1;

			String name = ItemDatabase.getItemDataName( nextInteger );
			String access = ItemDatabase.getAccessById( nextInteger );
			if ( access != null )
			{
				int type = ItemDatabase.getConsumptionType( id );
				int price = ItemDatabase.getPriceById( id );
				writer.println( id + "\t" + name + "\t" + type + "\t" + access + "\t" + price );
			}
			else
			{
				writer.println( id + "\t" + name + "\t0\tunknown\t-1" );
			}
		}

		writer.close();

		output = new File( UtilityConstants.DATA_LOCATION, "itemdescs.txt" );
		writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.ITEMDESCS_VERSION );

		lastInteger = 1;
		it = ItemDatabase.descriptionIdEntrySet().iterator();

		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer nextInteger = (Integer) entry.getKey();
			int id = nextInteger.intValue();

			for ( int i = lastInteger; i < id; ++i )
			{
				writer.println( i );
			}

			lastInteger = id + 1;

			String descId = (String) entry.getValue();
			String name = ItemDatabase.getItemDataName( nextInteger );
			String plural = ItemDatabase.getPluralById( id );
			writer.println( id + "\t" + descId + "\t" + name + ( plural.equals( "" ) ? "" : "\t" + plural ) );
		}

		writer.close();
	}

	// **********************************************************

	// Support for the "checkitems" command, which compares KoLmafia's
	// internal item data from what can be mined from the item description.

	private static final String ITEM_HTML = "itemhtml.txt";
	private static final String ITEM_DATA = "itemdata.txt";
	private static final StringArray rawItems = new StringArray();

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
	private static final Map others = new TreeMap();

	public static final void checkItems( final int itemId )
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawItems, ITEM_HTML );

		RequestLogger.printLine( "Checking internal data..." );

		PrintStream report = DebugDatabase.openReport( ITEM_DATA );

		DebugDatabase.foods.clear();
		DebugDatabase.boozes.clear();
		DebugDatabase.hats.clear();
		DebugDatabase.weapons.clear();
		DebugDatabase.offhands.clear();
		DebugDatabase.shirts.clear();
		DebugDatabase.pants.clear();
		DebugDatabase.accessories.clear();
		DebugDatabase.containers.clear();
		DebugDatabase.famitems.clear();
		DebugDatabase.others.clear();

		// Check item names, desc ID, consumption type

		if ( itemId == 0 )
		{
			DebugDatabase.checkItems( report );
		}
		else
		{
			DebugDatabase.checkItem( itemId, report );
		}

		// Check level limits, equipment, modifiers

		DebugDatabase.checkLevels( report );
		DebugDatabase.checkEquipment( report );
		DebugDatabase.checkItemModifiers( report );

		report.close();
	}

	private static final void checkItems( final PrintStream report )
	{
		Set keys = ItemDatabase.descriptionIdKeySet();
		Iterator it = keys.iterator();
		int lastId = 0;

		while ( it.hasNext() )
		{
			int id = ( (Integer) it.next() ).intValue();
			if ( id < 1 )
			{
				continue;
			}

			while ( ++lastId < id )
			{
				report.println( lastId );
			}

			DebugDatabase.checkItem( id, report );
		}

		DebugDatabase.saveScrapeData( keys.iterator(), rawItems, ITEM_HTML );
	}

	private static final void checkItem( final int itemId, final PrintStream report )
	{
		Integer id = new Integer( itemId );

		String name = ItemDatabase.getItemDataName( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String rawText = DebugDatabase.rawItemDescriptionText( itemId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has no description." );
			return;
		}

		String text = DebugDatabase.itemDescriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has malformed description text." );
			return;
		}

		String descriptionName = DebugDatabase.parseName( text );
		if ( !name.equals( descriptionName ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has description of " + descriptionName + "." );
			return;

		}

		boolean correct = true;

		int type = ItemDatabase.getConsumptionType( itemId );
		String descType = DebugDatabase.parseType( text );
		if ( !DebugDatabase.typesMatch( type, descType ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has consumption type of " + type + " but is described as " + descType + "." );
			correct = false;

		}

		int price = ItemDatabase.getPriceById( itemId );
		int descPrice = DebugDatabase.parsePrice( text );
		if ( price != descPrice )
		{
			report.println( "# *** " + name + " (" + itemId + ") has price of " + price + " but should be " + descPrice + "." );
			correct = false;

		}

		String access = ItemDatabase.getAccessById( id );
		String descAccess = DebugDatabase.parseAccess( text );
		if ( !access.equals( descAccess ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has access of " + access + " but should be " + descAccess + "." );
			correct = false;

		}

		switch ( type )
		{
		case KoLConstants.CONSUME_EAT:
			DebugDatabase.foods.put( name, text );
			break;
		case KoLConstants.CONSUME_DRINK:
			DebugDatabase.boozes.put( name, text );
			break;
		case KoLConstants.EQUIP_HAT:
			DebugDatabase.hats.put( name, text );
			break;
		case KoLConstants.EQUIP_PANTS:
			DebugDatabase.pants.put( name, text );
			break;
		case KoLConstants.EQUIP_SHIRT:
			DebugDatabase.shirts.put( name, text );
			break;
		case KoLConstants.EQUIP_WEAPON:
			DebugDatabase.weapons.put( name, text );
			break;
		case KoLConstants.EQUIP_OFFHAND:
			DebugDatabase.offhands.put( name, text );
			break;
		case KoLConstants.EQUIP_ACCESSORY:
			DebugDatabase.accessories.put( name, text );
			break;
		case KoLConstants.EQUIP_CONTAINER:
			DebugDatabase.containers.put( name, text );
			break;
		case KoLConstants.EQUIP_FAMILIAR:
			DebugDatabase.famitems.put( name, text );
			break;
		default:
			DebugDatabase.others.put( name, text );
			break;
		}

		report.println( itemId + "\t" + name + "\t" + type + "\t" + descAccess + "\t" + descPrice );
	}

	private static final GenericRequest DESC_ITEM_REQUEST = new GenericRequest( "desc_item.php" );

	public static final String rawItemDescriptionText( final int itemId )
	{
		Integer id = new Integer( itemId );
		String descId = ItemDatabase.getDescriptionId( id );
		if ( descId == null || descId.equals( "" ) )
		{
			return null;
		}

		String previous = DebugDatabase.rawItems.get( itemId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		DebugDatabase.DESC_ITEM_REQUEST.clearDataFields();
		DebugDatabase.DESC_ITEM_REQUEST.addFormField( "whichitem", descId );
		RequestThread.postRequest( DebugDatabase.DESC_ITEM_REQUEST );
		DebugDatabase.rawItems.set( itemId, DebugDatabase.DESC_ITEM_REQUEST.responseText );

		return DebugDatabase.DESC_ITEM_REQUEST.responseText;
	}

	private static final Pattern ITEM_DATA_PATTERN = Pattern.compile( "<img.*?><(br|blockquote)>(.*?)<script", Pattern.DOTALL );

	private static final String itemDescriptionText( final String rawText )
	{
		Matcher matcher = DebugDatabase.ITEM_DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 2 );
	}

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>(.*?)</b>" );

	private static final String parseName( final String text )
	{
		Matcher matcher = DebugDatabase.NAME_PATTERN.matcher( text );
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
		Matcher matcher = DebugDatabase.PRICE_PATTERN.matcher( text );
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
		Matcher matcher = DebugDatabase.TYPE_PATTERN.matcher( text );
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
			return descType.equals( "" ) || descType.equals( "crafting item" );
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
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
		case KoLConstants.CONSUME_STICKER:
			return descType.indexOf( "usable" ) != -1 || descType.equals( "gift package" ) || descType.equals( "food" ) || descType.equals( "booze" ) || descType.equals( "potion" );
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
		case KoLConstants.COMBAT_ITEM:
			return descType.indexOf( "combat" ) != -1;
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
			return false;
		}
		return false;
	}

	private static final void checkLevels( final PrintStream report )
	{
		RequestLogger.printLine( "Checking level requirements..." );

		DebugDatabase.checkLevelMap( report, DebugDatabase.foods, "Food" );
		DebugDatabase.checkLevelMap( report, DebugDatabase.boozes, "Booze" );
	}

	private static final void checkLevelMap( final PrintStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# Level requirements in " + ( map == DebugDatabase.foods ? "fullness" : "inebriety" ) + ".txt" );

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = (String) map.get( name );
			DebugDatabase.checkLevelDatum( name, text, report );
		}
	}

	private static final void checkLevelDatum( final String name, final String text, final PrintStream report )
	{
		Integer requirement = ItemDatabase.getLevelReqByName( name );
		int level = requirement == null ? 0 : requirement.intValue();
		int descLevel = DebugDatabase.parseLevel( text );
		if ( level != descLevel )
		{
			report.println( "# *** " + name + " requires level " + level + " but should be " + descLevel + "." );
		}
	}

	private static final Pattern LEVEL_PATTERN = Pattern.compile( "Level required: <b>(.*?)</b>" );

	private static final int parseLevel( final String text )
	{
		Matcher matcher = DebugDatabase.LEVEL_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 1;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final void checkEquipment( final PrintStream report )
	{

		RequestLogger.printLine( "Checking equipment..." );

		DebugDatabase.checkEquipmentMap( report, DebugDatabase.hats, "Hats" );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.pants, "Pants" );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.shirts, "Shirts" );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.weapons, "Weapons" );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.offhands, "Off-hand" );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.accessories, "Accessories" );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.containers, "Containers" );
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
			DebugDatabase.checkEquipmentDatum( name, text, report );
		}
	}

	private static final Pattern POWER_PATTERN = Pattern.compile( "Power: <b>(\\d+)</b>" );
	private static final Pattern REQ_PATTERN = Pattern.compile( "(\\w+) Required: <b>(\\d+)</b>" );
	private static final Pattern WEAPON_PATTERN = Pattern.compile( "weapon [(](.*?)[)]" );

	private static final void checkEquipmentDatum( final String name, final String text, final PrintStream report )
	{
		Matcher matcher;

		String type = DebugDatabase.parseType( text );
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
			matcher = DebugDatabase.POWER_PATTERN.matcher( text );
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
			matcher = DebugDatabase.WEAPON_PATTERN.matcher( text );
			if ( matcher.find() )
			{
				weaponType = matcher.group( 1 );
			}
		}

		String req = "none";

		matcher = DebugDatabase.REQ_PATTERN.matcher( text );
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
		else if ( isShield )
		{
			report.println( name + "\t" + power + "\t" + req + "\tshield" );
		}
		else
		{
			report.println( name + "\t" + power + "\t" + req );
		}
	}

	private static final void checkItemModifiers( final PrintStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );

		DebugDatabase.checkItemModifierMap( report, DebugDatabase.hats, "Hats" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.pants, "Pants" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.shirts, "Shirts" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.weapons, "Weapons" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.offhands, "Off-hand" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.accessories, "Accessories" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.containers, "Containers" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.famitems, "Familiar Items" );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.others, "Everything Else" );
	}

	private static final void checkItemModifierMap( final PrintStream report, final Map map, final String tag )
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
			DebugDatabase.checkItemModifierDatum( name, text, report );
		}
	}

	private static final void checkItemModifierDatum( final String name, final String text, final PrintStream report )
	{
		ArrayList unknown = new ArrayList();
		String known = DebugDatabase.parseItemEnchantments( text, unknown );

		for ( int i = 0; i < unknown.size(); ++i )
		{
			report.println( "# " + name + ": " + (String) unknown.get( i ) );
		}

		if ( known.equals( "" ) )
		{
			if ( unknown.size() == 0 )
			{
				report.println( "# " + name );
			}
		}
		else
		{
			report.println( name + "\t" + known );
		}
	}

	private static final Pattern ITEM_ENCHANTMENT_PATTERN =
		Pattern.compile( "Enchantment:.*?<font color=blue>(.*)</font>", Pattern.DOTALL );

	private static final String parseItemEnchantments( final String text, final ArrayList unknown )
	{
		String known = parseStandardEnchantments( text, unknown, DebugDatabase.ITEM_ENCHANTMENT_PATTERN );

		// Several modifiers can appear outside the "Enchantments"
		// section of the item description.

		// Damage Reduction can appear in either place
		if ( known.indexOf( "Damage Reduction" ) == -1 )
		{
			String dr = Modifiers.parseDamageReduction( text );
			if ( dr != null )
			{
				if ( !known.equals( "" ) )
				{
					known += ", ";
				}
				known += dr;
			}
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

		String softcore = Modifiers.parseSoftcoreOnly( text );
		if ( softcore != null )
		{
			if ( !known.equals( "" ) )
			{
				known += ", ";
			}
			known += softcore;
		}

		String freepull = Modifiers.parseFreePull( text );
		if ( freepull != null )
		{
			if ( !known.equals( "" ) )
			{
				known += ", ";
			}
			known += freepull;
		}

		return known;
	}

	private static final String parseStandardEnchantments( final String text, final ArrayList unknown, final Pattern pattern )
	{
		String known = "";

		Matcher matcher = pattern.matcher( text );
		if ( !matcher.find() )
		{
			return known;
		}

		StringBuffer enchantments = new StringBuffer( matcher.group(1) );

		StringUtilities.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total." );
		StringUtilities.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> If you wear multiple items that increase Critical Hit chances, only the highest multiplier applies." );
		StringUtilities.globalStringReplace( enchantments, "<br>", "\n" );
		StringUtilities.globalStringReplace( enchantments, "<Br>", "\n" );

		String[] mods = enchantments.toString().split( "\n+" );
		for ( int i = 0; i < mods.length; ++i )
		{
			String enchantment = mods[i].trim();
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

	// **********************************************************

	// Support for the "checkeffects" command, which compares KoLmafia's
	// internal status effect data from what can be mined from the effect
	// description.

	private static final String EFFECT_HTML = "effecthtml.txt";
	private static final String EFFECT_DATA = "effectdata.txt";
	private static final StringArray rawEffects = new StringArray();
	private static final Map effects = new TreeMap();

	public static final void checkEffects()
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawEffects, EFFECT_HTML );

		RequestLogger.printLine( "Checking internal data..." );

		PrintStream report = DebugDatabase.openReport( EFFECT_DATA );

		DebugDatabase.effects.clear();
		DebugDatabase.checkEffects( report );
		DebugDatabase.checkEffectModifiers( report );

		report.close();
	}

	private static final void checkEffects(final PrintStream report )
	{
		Set keys = EffectDatabase.descriptionIdKeySet();
		Iterator it = keys.iterator();

		while ( it.hasNext() )
		{
			int id = ( (Integer) it.next() ).intValue();
			if ( id < 1 )
			{
				continue;
			}

			DebugDatabase.checkEffect( id, report );
		}

		DebugDatabase.saveScrapeData( keys.iterator(), rawEffects, EFFECT_HTML );
	}

	private static final void checkEffect( final int effectId, final PrintStream report )
	{
		String name = EffectDatabase.getEffectDataName( effectId );
		if ( name == null )
		{
			return;
		}

		String rawText = DebugDatabase.rawEffectDescriptionText( effectId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + effectId + ") has no description." );
			return;
		}

		String text = DebugDatabase.effectDescriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + effectId + ") has malformed description text." );
			return;
		}

		String descriptionName = DebugDatabase.parseName( text );
		if ( !name.equalsIgnoreCase( StringUtilities.getCanonicalName( descriptionName ) ) )
		{
			report.println( "# *** " + name + " (" + effectId + ") has description of " + descriptionName + "." );
			return;

		}

		DebugDatabase.effects.put( name, text );
	}

	private static final GenericRequest DESC_EFFECT_REQUEST = new GenericRequest( "desc_effect.php" );

	private static final String rawEffectDescriptionText( final int effectId )
	{
		String descId = EffectDatabase.getDescriptionId( effectId );
		if ( descId == null || descId.equals( "" ) )
		{
			return null;
		}

		String previous = DebugDatabase.rawEffects.get( effectId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		DebugDatabase.DESC_EFFECT_REQUEST.clearDataFields();
		DebugDatabase.DESC_EFFECT_REQUEST.addFormField( "whicheffect", descId );
		RequestThread.postRequest( DebugDatabase.DESC_EFFECT_REQUEST );
		DebugDatabase.rawEffects.set( effectId, DebugDatabase.DESC_EFFECT_REQUEST.responseText );

		return DebugDatabase.DESC_EFFECT_REQUEST.responseText;
	}

	private static final Pattern EFFECT_DATA_PATTERN = Pattern.compile( "<div id=\"description\">(.*?)</div>", Pattern.DOTALL );

	private static final String effectDescriptionText( final String rawText )
	{
		Matcher matcher = DebugDatabase.EFFECT_DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 1 );
	}

	private static final void checkEffectModifiers( final PrintStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );

		DebugDatabase.checkEffectModifierMap( report, DebugDatabase.effects, "Status Effects" );
	}

	private static final void checkEffectModifierMap( final PrintStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		report.println();
		report.println( "# " + tag + " section of modifiers.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = (String) map.get( name );
			DebugDatabase.checkEffectModifierDatum( name, text, report );
		}
	}

	private static final Pattern EFFECT_ENCHANTMENT_PATTERN =
		Pattern.compile( "<font color=blue><b>(.*)</b></font>", Pattern.DOTALL );

	private static final void checkEffectModifierDatum( final String name, final String text, final PrintStream report )
	{
		ArrayList unknown = new ArrayList();
		String known = DebugDatabase.parseStandardEnchantments( text, unknown, DebugDatabase.EFFECT_ENCHANTMENT_PATTERN );

		for ( int i = 0; i < unknown.size(); ++i )
		{
			report.println( "# " + name + ": " + (String) unknown.get( i ) );
		}

		if ( known.equals( "" ) )
		{
			if ( unknown.size() == 0 )
			{
				report.println( "# " + name );
			}
		}
		else
		{
			report.println( name + "\t" + known );
		}
	}

	// **********************************************************

	// Utilities for dealing with KoL description data

	private static final PrintStream openReport( final String fileName )
	{
		return LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, fileName ), true );
	}

	private static final void loadScrapeData( final StringArray array, final String fileName )
	{
		if ( array.size() > 0 )
		{
			return;
		}

		try
		{
			File saveData = new File( UtilityConstants.DATA_LOCATION, fileName );
			if ( !saveData.exists() )
			{
				return;
			}

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

				array.set( currentId, currentHTML.toString() );
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

	private static final void saveScrapeData( final Iterator it, final StringArray array, final String fileName )
	{
		File file = new File( UtilityConstants.DATA_LOCATION, fileName );
		PrintStream livedata = LogStream.openStream( file, true );

		while ( it.hasNext() )
		{
			int id = ( (Integer) it.next() ).intValue();
			if ( id < 1 )
			{
				continue;
			}

			String description = array.get( id );
			if ( description != null && !description.equals( "" ) )
			{
				livedata.println( id );
				livedata.println( description );
			}
		}

		livedata.close();
	}

	// **********************************************************

	public static final void checkPlurals( final int itemId )
	{
		RequestLogger.printLine( "Checking plurals..." );
		PrintStream report = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "plurals.txt" ), true );

		if ( itemId == 0 )
		{
			Iterator it = ItemDatabase.descriptionIdKeySet().iterator();
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 0 )
				{
					continue;
				}
				DebugDatabase.checkPlural( id, report );
			}
		}
		else
		{
			DebugDatabase.checkPlural( itemId, report );
		}

		report.close();
	}

	private static final void checkPlural( final int itemId, final PrintStream report )
	{
		Integer id = new Integer( itemId );

		String name = ItemDatabase.getItemDataName( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String descId = ItemDatabase.getDescriptionId( id );
		String plural = ItemDatabase.getPluralById( itemId );

		// Don't bother checking quest items
		String access = ItemDatabase.getAccessById( id );
		if ( access != null && !access.equals( "none" ) )
		{
			String wikiData = DebugDatabase.readWikiData( name );
			Matcher matcher = DebugDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
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

	public static final void checkConsumptionData()
	{
		RequestLogger.printLine( "Checking consumption data..." );

		PrintStream writer = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "consumption.txt" ), true );

		DebugDatabase.checkEpicure( writer );
		DebugDatabase.checkMixologist( writer );

		writer.close();
	}

	private static final String EPICURE = "http://epicure.bewarethefgc.com/export_data.php";

	private static final void checkEpicure( final PrintStream writer )
	{
		RequestLogger.printLine( "Connecting to Well-Tempered Epicure..." );
		Document doc = getXMLDocument( EPICURE );

		if ( doc == null )
		{
			return;
		}

		writer.println( KoLConstants.FULLNESS_VERSION );
		writer.println( "# Data provided courtesy of the Garden of Earthly Delights" );
		writer.println( "# The Well-Tempered Epicure: " + EPICURE );
		writer.println();
		writer.println( "# Food" + "\t" + "Fullness" + "\t" + "Level Req" + "\t" + "Adv" + "\t" + "Musc" + "\t" + "Myst" + "\t" + "Moxie" );
		writer.println();

		NodeList elements = doc.getElementsByTagName( "iteminfo" );

		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkFood( element, writer );
		}
	}

	private static final void checkFood( final Node element, final PrintStream writer )
	{
		String name= "";
		String advs= "";
		String musc= "";
		String myst= "";
		String mox= "";
		String fullness= "";
		String level= "";

		for ( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
		{
			String tag = node.getNodeName();
			Node child = node.getFirstChild();

			if ( tag.equals( "title" ) )
			{
				name = DebugDatabase.getStringValue( child );
			}
			else if ( tag.equals( "advs" ) )
			{
				advs = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "musc" ) )
			{
				musc = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "myst" ) )
			{
				myst = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "mox" ) )
			{
				mox = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "fullness" ) )
			{
				fullness = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "level" ) )
			{
				level = DebugDatabase.getNumericValue( child );
			}
		}

		String line = name + "\t" + fullness + "\t" + level + "\t" + advs + "\t" + musc + "\t" + myst + "\t" + mox;

		int present = ItemDatabase.getFullness( name );

		if ( present == 0 )
		{
			writer.println( "# Unknown food:" );
			writer.print( "# " );
		}
		else
		{
			String note = ItemDatabase.getNotes( name );
			if ( note != null )
			{
				line = line + "\t" + note;
			}
		}

		writer.println( line );
	}

	private static final String MIXOLOGIST = "http://mixology.bewarethefgc.com/export_data.php";

	private static final void checkMixologist( final PrintStream writer )
	{
		RequestLogger.printLine( "Connecting to Well-Tempered Mixologist..." );
		Document doc = getXMLDocument( MIXOLOGIST );

		if ( doc == null )
		{
			return;
		}

		writer.println( KoLConstants.INEBRIETY_VERSION );
		writer.println( "# Data provided courtesy of the Garden of Earthly Delights" );
		writer.println( "# The Well-Tempered Mixologist: " + MIXOLOGIST );
		writer.println();
		writer.println( "# Drink" + "\t" + "Inebriety" + "\t" + "Level Req" + "\t" + "Adv" + "\t" + "Musc" + "\t" + "Myst" + "\t" + "Moxie" );
		writer.println();

		NodeList elements = doc.getElementsByTagName( "iteminfo" );

		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkBooze( element, writer );
		}
	}

	private static final void checkBooze( final Node element, final PrintStream writer )
	{
		String name= "";
		String advs= "";
		String musc= "";
		String myst= "";
		String mox= "";
		String drunk= "";
		String level= "";

		for ( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
		{
			String tag = node.getNodeName();
			Node child = node.getFirstChild();

			if ( tag.equals( "title" ) )
			{
				name = DebugDatabase.getStringValue( child );
			}
			else if ( tag.equals( "advs" ) )
			{
				advs = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "musc" ) )
			{
				musc = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "myst" ) )
			{
				myst = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "mox" ) )
			{
				mox = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "drunk" ) )
			{
				drunk = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "level" ) )
			{
				level = DebugDatabase.getNumericValue( child );
			}
		}


		String line = name + "\t" + drunk + "\t" + level + "\t" + advs + "\t" + musc + "\t" + myst + "\t" + mox;

		int present = ItemDatabase.getInebriety( name );

		if ( present == 0 )
		{
			writer.println( "# Unknown booze:" );
			writer.print( "# " );
		}
		else
		{
			String note = ItemDatabase.getNotes( name );
			if ( note != null )
			{
				line = line + "\t" + note;
			}
		}

		writer.println( line );
	}

	private static final String getStringValue( final Node node )
	{
		return StringUtilities.getEntityEncode( node.getNodeValue().trim() );
	}

	private static final String getNumericValue( final Node node )
	{
		String value = node.getNodeValue().trim();

		int sign = value.startsWith( "-" ) ? -1 : 1;
		if ( sign == -1 )
		{
			value = value.substring( 1 );
		}

		int dash = value.indexOf( "-" );
		if ( dash == -1 )
		{
			return String.valueOf( sign * StringUtilities.parseInt( value ) );
		}

		int first = sign * StringUtilities.parseInt( value.substring( 0, dash) );
		int second = StringUtilities.parseInt( value.substring( dash + 1 ) );
		return String.valueOf( first ) + "-" + String.valueOf( second );
	}

	private static final Document getXMLDocument( final String uri )
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse( uri );
		}
		catch ( Exception e )
		{
			RequestLogger.printLine( "Failed to parse XML document from \"" + uri + "\"" );
		}

		return null;
	}

	public static final void checkPulverizationData()
	{
		RequestLogger.printLine( "Checking pulverization data..." );

		PrintStream writer = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "pulvereport.txt" ), true );

		DebugDatabase.checkAnvil( writer );

		writer.close();
	}

	private static final String ANVIL = "http://anvil.bewarethefgc.com/export_data.php";

	private static final void checkAnvil( final PrintStream writer )
	{
		RequestLogger.printLine( "Connecting to Well-Tempered Anvil..." );
		Document doc = getXMLDocument( ANVIL );

		if ( doc == null )
		{
			return;
		}

		writer.println( KoLConstants.PULVERIZE_VERSION );
		writer.println( "# Data provided courtesy of the Garden of Earthly Delights" );
		writer.println( "# The Well-Tempered Anvil: " + ANVIL );
		writer.println();

		NodeList elements = doc.getElementsByTagName( "iteminfo" );

		HashSet seen = new HashSet();
		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkPulverize( element, writer, seen );
		}
		
		for ( int id = 1; id <= ItemDatabase.maxItemId(); ++id )
		{
			int pulver = EquipmentDatabase.getPulverization( id );
			if ( pulver != -1 && !seen.contains( new Integer( id ) ) )
			{
				String name = ItemDatabase.getItemName( id );
				writer.println( name + ": not listed in anvil" );
			}
		}
	}

	private static final void checkPulverize( final Node element, final PrintStream writer,
		HashSet seen )
	{
		String name= "";
		int id = -1;
		int yield = -1;
		boolean cansmash = false;
		boolean confirmed = false;
		boolean twinkly = false;
		boolean hot = false;
		boolean cold = false;
		boolean stench = false;
		boolean spooky = false;
		boolean sleaze = false;
		String advs= "";
		String musc= "";
		String myst= "";
		String mox= "";
		String fullness= "";
		String level= "";

		for ( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
		{
			String tag = node.getNodeName();
			Node child = node.getFirstChild();

			if ( tag.equals( "cansmash" ) )
			{
				cansmash = DebugDatabase.getStringValue( child ).equals( "y" );
			}
			else if ( tag.equals( "confirmed" ) )
			{
				confirmed = DebugDatabase.getStringValue( child ).equals( "y" );
			}
			else if ( tag.equals( "title" ) )
			{
				name = DebugDatabase.getStringValue( child );
			}
			else if ( tag.equals( "kolid" ) )
			{
				id = StringUtilities.parseInt( DebugDatabase.getNumericValue( child ) );
				seen.add( new Integer( id ) );
			}
			else if ( tag.equals( "yield" ) )
			{
				yield = StringUtilities.parseInt( DebugDatabase.getNumericValue( child ) );
			}
			else if ( tag.equals( "cold" ) )
			{
				cold = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "hot" ) )
			{
				hot = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "sleazy" ) )
			{
				sleaze = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "spooky" ) )
			{
				spooky = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "stinky" ) )
			{
				stench = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "twinkly" ) )
			{
				twinkly = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
		}
		
		if ( id < 1 )
		{
			writer.println( name + ": anvil doesn't know ID, so can't check" );
			return;
		}
		int pulver = EquipmentDatabase.getPulverization( id );
		if ( !name.equalsIgnoreCase( ItemDatabase.getItemName( id ) ) )
		{
			writer.println( name + ": doesn't match mafia name: " + 
				ItemDatabase.getItemName( id ) );
		}
		name = ItemDatabase.getItemName( id );
		if ( !confirmed )
		{
			name = "(unconfirmed) " + name;
		}
		if ( pulver == -1 )
		{
			if ( cansmash )
			{
				writer.println( name + ": anvil says this is smashable" );
			}
			return;
		}
		if ( !cansmash )
		{
			writer.println( name + ": anvil says this is not smashable" );
			return;
		}
		if ( pulver == ItemPool.USELESS_POWDER )
		{
			if ( yield != 1 || twinkly || hot || cold || stench || spooky || sleaze )
			{
				writer.println( name + ": anvil says something other than useless powder" );
			}
			return;
		}
		if ( yield == 1 && !(twinkly || hot || cold || stench || spooky || sleaze ) )
		{
			writer.println( name + ": anvil says useless powder" );
			return;
		}
		if ( pulver == ItemPool.EPIC_WAD )
		{
			if ( yield != 10 )
			{
				writer.println( name + ": anvil says something other than epic wad" );
			}
			return;
		}
		if ( yield == 10 )
		{
			writer.println( name + ": anvil says epic wad" );
			return;
		}
		if ( pulver == ItemPool.ULTIMATE_WAD )
		{
			if ( yield != 11 )
			{
				writer.println( name + ": anvil says something other than ultimate wad" );
			}
			return;
		}
		if ( yield == 11 )
		{
			writer.println( name + ": anvil says ultimate wad" );
			return;
		}
		if ( pulver == ItemPool.SEA_SALT_CRYSTAL )
		{
			if ( yield != 12 )
			{
				writer.println( name + ": anvil says something other than sea salt crystal" );
			}
			return;
		}
		if ( yield == 12 )
		{
			writer.println( name + ": anvil says sea salt crystal" );
			return;
		}
		if ( pulver >= 0 )
		{
			writer.println( name + ": I don't know how anvil would say " +
				ItemDatabase.getItemName( pulver ) );
			return;
		}
		if ( yield < 1 || yield > 12 )
		{
			writer.println( name + ": anvil said yield=" + yield + ", wut?" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_TWINKLY) != 0 )
		{
			if ( !twinkly )
			{
				writer.println( name + ": anvil didn't say twinkly" );
			}
			return;
		}
		else if ( twinkly )
		{
			writer.println( name + ": anvil said twinkly" );
			return;
		}


		if ( (pulver & EquipmentDatabase.ELEM_HOT) != 0 )
		{
			if ( !hot )
			{
				writer.println( name + ": anvil didn't say hot" );
			}
			return;
		}
		else if ( hot )
		{
			writer.println( name + ": anvil said hot" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_COLD) != 0 )
		{
			if ( !cold )
			{
				writer.println( name + ": anvil didn't say cold" );
			}
			return;
		}
		else if ( cold )
		{
			writer.println( name + ": anvil said cold" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_STENCH) != 0 )
		{
			if ( !stench )
			{
				writer.println( name + ": anvil didn't say stench" );
			}
			return;
		}
		else if ( stench )
		{
			writer.println( name + ": anvil said stench" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_SPOOKY) != 0 )
		{
			if ( !spooky )
			{
				writer.println( name + ": anvil didn't say spooky" );
			}
			return;
		}
		else if ( spooky )
		{
			writer.println( name + ": anvil said spooky" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_SLEAZE) != 0 )
		{
			if ( !sleaze )
			{
				writer.println( name + ": anvil didn't say sleaze" );
			}
			return;
		}
		else if ( sleaze )
		{
			writer.println( name + ": anvil said sleaze" );
			return;
		}
		int myyield = 1;
		while ( (pulver & EquipmentDatabase.YIELD_1P) == 0 )
		{
			myyield++;
		}
		if ( yield != myyield )
		{
			writer.println( name + ": anvil said yield is " + yield + ", not " + myyield );
			return;
		}
	}

	private static final Pattern ZAPGROUP_PATTERN = Pattern.compile( "Template:ZAP .*?</a>.*?<td>.*?<td>" );
	private static final Pattern ZAPITEM_PATTERN = Pattern.compile( ">([^<]+)</a>" );
	
	public static final void checkZapGroups()
	{
		RequestLogger.printLine( "Checking zap groups..." );
		PrintStream report = LogStream.openStream( new File( UtilityConstants.DATA_LOCATION, "zapreport.txt" ), true );
		
		String[] groups = DebugDatabase.ZAPGROUP_PATTERN.split(
			DebugDatabase.readWikiData( "Zapping" ) );
		for ( int i = 1; i < groups.length; ++i )
		{
			String group = groups[ i ];
			int pos = group.indexOf( "</td>" );
			if ( pos != -1 )
			{
				group = group.substring( 0, pos );
			}
			Matcher m = DebugDatabase.ZAPITEM_PATTERN.matcher( group );
			ArrayList items = new ArrayList();
			while ( m.find() )
			{
				items.add( m.group( 1 ) );
			}
			if ( items.size() > 1 )
			{
				DebugDatabase.checkZapGroup( items, report );
			}
		}
		report.close();
	}
	
	private static void checkZapGroup( ArrayList items, PrintStream report )
	{
		String firstItem = (String) items.get( 0 );
		int itemId = ItemDatabase.getItemId( firstItem );

		if ( itemId == -1 )
		{
			report.println( "Group with unrecognized item: " + firstItem );
			return;
		}
		String[] zapgroup = ZapRequest.getZapGroup( itemId );
		if ( zapgroup.length == 0 )
		{
			report.println( "New group:" );
			Iterator i = items.iterator();
			while ( i.hasNext() )
			{
				report.print( (String) i.next() );
				report.print( ", " );
			}
			report.println( "" );
			return;
		}
		ArrayList existing = new ArrayList();
		existing.addAll( Arrays.asList( zapgroup ) );
		existing.removeAll( items );
		items.removeAll( Arrays.asList( zapgroup ) );
		if ( items.size() == 0 && existing.size() == 0 )
		{
			report.println( "Group OK: " + firstItem );
			return;
		}
		report.println( "Modified group: " + firstItem );
		report.println( "Added:" );
		Iterator i = items.iterator();
		while ( i.hasNext() )
		{
			report.print( (String) i.next() );
			report.print( ", " );
		}
		report.println( "" );
		report.println( "Removed:" );
		i = existing.iterator();
		while ( i.hasNext() )
		{
			report.print( (String) i.next() );
			report.print( ", " );
		}
		report.println( "" );
	}
}
