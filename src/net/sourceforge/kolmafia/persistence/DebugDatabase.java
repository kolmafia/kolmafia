/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DebugDatabase
	extends KoLDatabase
{
	//private static final Pattern WIKI_ITEMID_PATTERN = Pattern.compile( "Item number</a>:</b> (\\d+)<br />" );
	//private static final Pattern WIKI_DESCID_PATTERN = Pattern.compile( "<b>Description ID:</b> (\\d+)<br />" );
	private static final Pattern WIKI_PLURAL_PATTERN =
		Pattern.compile( "\\(.*?In-game plural</a>: <i>(.*?)</i>\\)", Pattern.DOTALL );
	//private static final Pattern WIKI_AUTOSELL_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

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
		StringBuilder wikiRecord = new StringBuilder();

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

	/*public static final void determineWikiData( final String name )
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
	}*/

	// **********************************************************

	// Support for the "checkitems" command, which compares KoLmafia's
	// internal item data from what can be mined from the item description.

	private static final String ITEM_HTML = "itemhtml.txt";
	private static final String ITEM_DATA = "itemdata.txt";
	private static final StringArray rawItems = new StringArray();

	private static final Map<String, String> foods = new TreeMap<String, String>();
	private static final Map<String, String> boozes = new TreeMap<String, String>();
	private static final Map<String, String> hats = new TreeMap<String, String>();
	private static final Map<String, String> weapons = new TreeMap<String, String>();
	private static final Map<String, String> offhands = new TreeMap<String, String>();
	private static final Map<String, String> shirts = new TreeMap<String, String>();
	private static final Map<String, String> pants = new TreeMap<String, String>();
	private static final Map<String, String> accessories = new TreeMap<String, String>();
	private static final Map<String, String> containers = new TreeMap<String, String>();
	private static final Map<String, String> famitems = new TreeMap<String, String>();
	private static final Map<String, String> others = new TreeMap<String, String>();

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
		Set<Integer> keys = ItemDatabase.descriptionIdKeySet();
		Iterator<Integer> it = keys.iterator();
		int lastId = 0;

		while ( it.hasNext() )
		{
			int id = ( it.next() ).intValue();
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
		Integer id = IntegerPool.get( itemId );

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
			DebugDatabase.rawItems.set( itemId, null );
			return;
		}

		String descriptionName = DebugDatabase.parseName( text );
		if ( !name.equals( descriptionName ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has description of " + descriptionName + "." );
			DebugDatabase.rawItems.set( itemId, null );
			return;

		}

		int type = ItemDatabase.getConsumptionType( itemId );
		String descType = DebugDatabase.parseType( text );
		int descPrimary = DebugDatabase.typeToPrimary( descType );
		if ( !typesMatch( type, descPrimary ) )
		{
			String primary = ItemDatabase.typeToPrimaryUsage( type );
			report.println( "# *** " + name + " (" + itemId + ") has primary usage of " + primary + " but is described as " + descType + "." );
		}

		int attrs = ItemDatabase.getAttributes( itemId );
		int descAttrs = DebugDatabase.typeToSecondary( descType );
		if ( !DebugDatabase.attributesMatch( attrs, descAttrs ) )
		{
			String secondary = ItemDatabase.attrsToSecondaryUsage( attrs );
			String descSecondary = ItemDatabase.attrsToSecondaryUsage( descAttrs );
			report.println( "# *** " + name + " (" + itemId + ") has secondary usage of " + secondary + " but is described as " + descSecondary + "." );
		}

		int price = ItemDatabase.getPriceById( itemId );
		int descPrice = DebugDatabase.parsePrice( text );
		if ( price != descPrice && ( price >= 0 || descPrice != 0 ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has price of " + price + " but should be " + descPrice + "." );
		}

		String access = ItemDatabase.getAccessById( id );
		String descAccess = DebugDatabase.parseAccess( text );
		if ( !access.equals( descAccess ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has access of " + access + " but should be " + descAccess + "." );
		}

		String image = ItemDatabase.getImage( id );
		String descImage = DebugDatabase.parseImage( rawText );
		if ( !image.equals( descImage ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has image of " + image + " but should be " + descImage + "." );
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

		String descId = ItemDatabase.getDescriptionId( id );
		String plural = ItemDatabase.getPluralById( id );

		report.println( ItemDatabase.itemString( itemId, name, descId, image, type, attrs, access, price, plural ) );
	}

	private static final GenericRequest DESC_ITEM_REQUEST = new GenericRequest( "desc_item.php" );

	public static final String itemDescriptionText( final int itemId )
	{
		return DebugDatabase.itemDescriptionText( itemId, false );
	}

	public static final String itemDescriptionText( final int itemId, boolean forceReload )
	{
		return DebugDatabase.itemDescriptionText( DebugDatabase.rawItemDescriptionText( itemId, forceReload ) );
	}

	public static final String rawItemDescriptionText( final int itemId )
	{
		return DebugDatabase.rawItemDescriptionText( itemId, false );
	}

	public static final String rawItemDescriptionText( final int itemId, boolean forceReload )
	{
		Integer id = IntegerPool.get( itemId );
		String descId = ItemDatabase.getDescriptionId( id );
		if ( descId == null || descId.equals( "" ) )
		{
			return null;
		}

		String previous = DebugDatabase.rawItems.get( itemId );
		if ( !forceReload && previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		DebugDatabase.DESC_ITEM_REQUEST.clearDataFields();
		DebugDatabase.DESC_ITEM_REQUEST.addFormField( "whichitem", descId );
		RequestThread.postRequest( DebugDatabase.DESC_ITEM_REQUEST );
		DebugDatabase.rawItems.set( itemId, DebugDatabase.DESC_ITEM_REQUEST.responseText );

		return DebugDatabase.DESC_ITEM_REQUEST.responseText;
	}

	private static final Pattern ITEM_DATA_PATTERN = Pattern.compile( "<div id=\"description\"(.*?)<script", Pattern.DOTALL );

	public static final String itemDescriptionText( final String rawText )
	{
		if ( rawText == null )
		{
			return null;
		}

		Matcher matcher = DebugDatabase.ITEM_DATA_PATTERN.matcher( rawText );
		return matcher.find() ? matcher.group( 1 ) : null;
	}

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>(.*?)</b>" );
	public static final String parseName( final String text )
	{
		Matcher matcher = DebugDatabase.NAME_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return "";
		}

		// One item is known to have an extra internal space
		return StringUtilities.globalStringReplace( matcher.group( 1 ), "  ", " " ).trim();
	}

	private static final Pattern PRICE_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );
	public static final int parsePrice( final String text )
	{
		Matcher matcher = DebugDatabase.PRICE_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final StringBuilder appendAccessTypes( StringBuilder accessTypes, String accessType )
	{
		if ( accessTypes.length() > 0 )
		{
			return accessTypes.append( "," ).append( accessType );
		}
		return accessTypes.append( accessType );
	}

	public static final String parseAccess( final String text )
	{
		StringBuilder accessTypes = new StringBuilder();

		if ( text.contains( "Quest Item" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.QUEST_FLAG );
		}

		// Quest items cannot be gifted or traded
		else if ( text.contains( "Gift Item" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.GIFT_FLAG );
		}

		// Gift items cannot be (normally) traded
		else if ( !text.contains( "Cannot be traded" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.TRADE_FLAG );
		}

		//We shouldn't just check for "discarded", in case "discarded" appears somewhere else in the description.
		if ( !text.contains( "Cannot be discarded" ) && !text.contains( "Cannot be traded or discarded" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.DISCARD_FLAG );
		}

		return accessTypes.toString();
	}

	private static final Pattern TYPE_PATTERN = Pattern.compile( "Type: <b>(.*?)</b>" );
	public static final String parseType( final String text )
	{
		Matcher matcher = DebugDatabase.TYPE_PATTERN.matcher( text );
		String type = matcher.find() ? matcher.group( 1 ) : "";
		return type.equals( "back item" ) ? "container" : type;
	}

	public static final int typeToPrimary( final String type )
	{
		// Type: <b>food <font color=#999999>(crappy)</font></b>
		// Type: <b>food (decent)</b>
		// Type: <b>booze <font color=green>(good)</font></b>
		// Type: <b>food <font color=blue>(awesome)</font></b>
		// Type: <b>food <font color=blueviolet>(EPIC)</font></b>

		if ( type.equals( "" ) || type.equals( "crafting item" ) )
		{
			return KoLConstants.NO_CONSUME;
		}
		if ( type.startsWith( "food" ) || type.startsWith( "beverage" ) )
		{
			return KoLConstants.CONSUME_EAT;
		}
		if ( type.startsWith( "booze" ) )
		{
			return KoLConstants.CONSUME_DRINK;
		}
		if ( type.contains( "self or others" ) )
		{
			// Curse items are special
			return KoLConstants.NO_CONSUME;
		}
		if ( type.contains( "usable" ) || type.equals( "gift package" ) )
		{
			return KoLConstants.CONSUME_USE;
		}
		if ( type.equals( "potion" ) )
		{
			// Although most potions end up being multi-usable, KoL
			// almost always forgets to add that flag when the item
			// is first introduced.
			//
			// Therefore, rather than generating bug reports
			// because KoLmafia tries to multi-use a single-use
			// item (which doesn't work), generate bug reports when
			// KoLmafia single-uses when it could multi-use (which
			// does work, but is slower.)
			//
			// return KoLConstants.CONSUME_MULTIPLE;
			return KoLConstants.CONSUME_USE;
		}
		if ( type.equals( "familiar" ) )
		{
			return KoLConstants.GROW_FAMILIAR;
		}
		if ( type.equals( "familiar equipment" ) )
		{
			return KoLConstants.EQUIP_FAMILIAR;
		}
		if ( type.startsWith( "accessory" ) )
		{
			return KoLConstants.EQUIP_ACCESSORY;
		}
		if ( type.startsWith( "container" ) )
		{
			return KoLConstants.EQUIP_CONTAINER;
		}
		if ( type.startsWith( "hat" ) )
		{
			return KoLConstants.EQUIP_HAT;
		}
		if ( type.startsWith( "shirt" ) )
		{
			return KoLConstants.EQUIP_SHIRT;
		}
		if ( type.startsWith( "pants" ) )
		{
			return KoLConstants.EQUIP_PANTS;
		}
		if ( type.contains( "weapon" ) )
		{
			return KoLConstants.EQUIP_WEAPON;
		}
		if ( type.startsWith( "off-hand item" ) )
		{
			return KoLConstants.EQUIP_OFFHAND;
		}
		return KoLConstants.NO_CONSUME;
	}

	public static final int typeToSecondary( final String type )
	{
		int attributes = 0;
		if ( type.contains( "(reusable)" ) )
		{
			attributes |= ItemDatabase.ATTR_COMBAT_REUSABLE;
		}
		else if ( type.contains( "combat" ) )
		{
			attributes |= ItemDatabase.ATTR_COMBAT;
		}
		if ( type.contains( "self or others" ) )
		{
			attributes |= ItemDatabase.ATTR_CURSE;
		}
		if ( type.contains( "(Fancy" ) )
		{
			attributes |= ItemDatabase.ATTR_FANCY;
		}
		return attributes;
	}

	private static final boolean typesMatch( final int type, final int descType )
	{
		switch ( type )
		{
		case KoLConstants.NO_CONSUME:
			// We intentionally disallow certain items from being
			// "used" through the GUI.
			return descType == KoLConstants.NO_CONSUME ||
			       descType == KoLConstants.CONSUME_USE;
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.GROW_FAMILIAR:
		case KoLConstants.EQUIP_FAMILIAR:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
			return descType == type;
		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.INFINITE_USES:
			return descType == KoLConstants.CONSUME_USE ||
			       descType == KoLConstants.CONSUME_MULTIPLE ||
			       descType == KoLConstants.CONSUME_EAT ||
			       descType == KoLConstants.CONSUME_DRINK ||
			       descType == KoLConstants.NO_CONSUME;
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
		case KoLConstants.CONSUME_STICKER:
			return descType == KoLConstants.NO_CONSUME ||
			       descType == KoLConstants.CONSUME_USE;
		case KoLConstants.CONSUME_SPHERE:
		case KoLConstants.CONSUME_ZAP:
			return descType == KoLConstants.NO_CONSUME;
		}
		return true;
	}

	private static final boolean attributesMatch( final int attrs, final int descAttrs )
	{
		// If the description says an item is "combat", "(reusable)" or "(on self or others)",
		// our database must mark the item as ATTR_COMBAT, ATTR_COMBAT_REUSABLE, or ATTR_CURSE
		//
		// However, there are quite a few items that we mark with those secondary attributes that are
		// not tagged that way by KoL itself. Assume those are correct.

		if ( ( descAttrs & ItemDatabase.ATTR_COMBAT ) != 0 && ( attrs & ItemDatabase.ATTR_COMBAT ) == 0 )
		{
			return false;
		}

		if ( ( descAttrs & ItemDatabase.ATTR_COMBAT_REUSABLE ) != 0 && ( attrs & ItemDatabase.ATTR_COMBAT_REUSABLE ) == 0 )
		{
			return false;
		}

		if ( ( descAttrs & ItemDatabase.ATTR_CURSE ) != 0 && ( attrs & ItemDatabase.ATTR_CURSE ) == 0 )
		{
			return false;
		}

		return true;
	}

	private static final void checkLevels( final PrintStream report )
	{
		RequestLogger.printLine( "Checking level requirements..." );

		DebugDatabase.checkLevelMap( report, DebugDatabase.foods, "Food" );
		DebugDatabase.checkLevelMap( report, DebugDatabase.boozes, "Booze" );
	}

	private static final void checkLevelMap( final PrintStream report, final Map<String, String> map, final String tag )
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
			String text = map.get( name );
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

	public static final int parseLevel( final String text )
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

	private static final void checkEquipmentMap( final PrintStream report, final Map<String, String> map, final String tag )
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
			String text = map.get( name );
			DebugDatabase.checkEquipmentDatum( name, text, report );
		}
	}

	private static final void checkEquipmentDatum( final String name, final String text, final PrintStream report )
	{
		String type = DebugDatabase.parseType( text );
		boolean isWeapon = false, isShield = false, hasPower = false;

		if ( type.contains( "weapon" ) )
		{
			isWeapon = true;
		}
		else if ( type.contains( "shield" ) )
		{
			isShield = true;
		}
		else if ( type.contains( "hat" ) || type.contains( "pants" ) || type.contains( "shirt" ) )
		{
			hasPower = true;
		}

		int power = 0;
		if ( isWeapon || hasPower )
		{
			power = DebugDatabase.parsePower( text );
		}
		else
		{
			// Until KoL puts off-hand and accessory power into the
			// description, use hand-entered "secret" value.
			power = EquipmentDatabase.getPower( name );
		}

		// Now check against what we already have
		int oldPower = EquipmentDatabase.getPower( name );
		if ( power != oldPower )
		{
			report.println( "# *** " + name + " has power " + oldPower + " but should be " + power + "." );
		}

		String weaponType = isWeapon ? DebugDatabase.parseWeaponType( type ) : "";
		String req = DebugDatabase.parseReq( text, type );

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

		EquipmentDatabase.writeEquipmentItem( report, name, power, req, weaponType, isWeapon, isShield );
	}

	private static final Pattern POWER_PATTERN = Pattern.compile( "Power: <b>(\\d+)</b>" );
	private static final Pattern DAMAGE_PATTERN_WEAPON = Pattern.compile( "Damage: <b>[\\d]+ - (\\d+)</b>" );
	public static final int parsePower( final String text )
	{
		Matcher matcher = DebugDatabase.POWER_PATTERN.matcher( text );
		// This should match non-weapon power
		if ( matcher.find() )
		{
			return StringUtilities.parseInt( matcher.group( 1 ) );
		}
		// This will match weapon damage and use it to calculate power
		matcher = DebugDatabase.DAMAGE_PATTERN_WEAPON.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) * 5 ) : 0;
	}

	private static final Pattern WEAPON_PATTERN = Pattern.compile( "weapon [(](.*?)[)]" );
	public static final String parseWeaponType( final String text )
	{
		Matcher matcher = DebugDatabase.WEAPON_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final Pattern REQ_PATTERN = Pattern.compile( "(\\w+) Required: <b>(\\d+)</b>" );
	public static final String parseReq( final String text, final String type )
	{
		Matcher matcher = DebugDatabase.REQ_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			String stat = matcher.group( 1 );
			if ( stat.equals( "Muscle" ) )
			{
				return "Mus: " + matcher.group( 2 );
			}
			if ( stat.equals( "Mysticality" ) )
			{
				return "Mys: " + matcher.group( 2 );
			}
			if ( stat.equals( "Moxie" ) )
			{
				return "Mox: " + matcher.group( 2 );
			}
		}

		if ( type.contains( "weapon" ) )
		{
			if ( type.contains( "ranged" ) )
			{
				return "Mox: 0";
			}
			else if ( type.contains( "utensil" ) ||
				  type.contains( "saucepan" ) ||
				  type.contains( "chefstaff" ) )
			{
				return "Mys: 0";
			}
			else
			{
				return "Mus: 0";
			}
		}

		return "none";
	}

	public static final int parseSize( final String text )
	{
		int size = DebugDatabase.parseFullness( text );
		if ( size > 0 )
		{
			return size;
		}

		return DebugDatabase.parseInebriety( text );
	}

	private static final Pattern FULLNESS_PATTERN = Pattern.compile( "Size: <b>(\\d+)</b>" );

	public static final int parseFullness( final String text )
	{
		Matcher matcher = DebugDatabase.FULLNESS_PATTERN.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) ) : 0;
	}

	private static final Pattern INEBRIETY_PATTERN = Pattern.compile( "Potency: <b>(\\d+)</b>" );

	public static final int parseInebriety( final String text )
	{
		Matcher matcher = DebugDatabase.INEBRIETY_PATTERN.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) ) : 0;
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

	private static final void checkItemModifierMap( final PrintStream report, final Map<String, String> map, final String tag )
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
			String text = map.get( name );
			DebugDatabase.checkItemModifierDatum( name, text, report );
		}
	}

	private static final void checkItemModifierDatum( final String name, final String text, final PrintStream report )
	{
		ArrayList<String> unknown = new ArrayList<String>();
		String known = DebugDatabase.parseItemEnchantments( text, unknown );
		DebugDatabase.logModifierDatum( name, known, unknown, report );
	}

	private static final void logModifierDatum( final String name, final String known, final ArrayList<String> unknown, final PrintStream report )
	{
		for ( int i = 0; i < unknown.size(); ++i )
		{
			Modifiers.writeModifierComment( report, name, unknown.get( i ) );
		}

		if ( known.equals( "" ) )
		{
			if ( unknown.size() == 0 )
			{
				Modifiers.writeModifierComment( report, name );
			}
		}
		else
		{
			Modifiers.writeModifierString( report, name, known );
		}
	}

	private static final Pattern ITEM_ENCHANTMENT_PATTERN =
		Pattern.compile( "Enchantment:.*?<font color=blue>(.*)</font>", Pattern.DOTALL );

	public static final String parseItemEnchantments( final String text, final ArrayList<String> unknown )
	{
		String known = parseStandardEnchantments( text, unknown, DebugDatabase.ITEM_ENCHANTMENT_PATTERN );

		// Several modifiers can appear outside the "Enchantments"
		// section of the item description.

		// Damage Reduction can appear in either place
		if ( !known.contains( "Damage Reduction" ) )
		{
			String dr = Modifiers.parseDamageReduction( text );
			known = DebugDatabase.appendModifier( known, dr );
		}

		String single = Modifiers.parseSingleEquip( text );
		known = DebugDatabase.appendModifier( known, single );

		String softcore = Modifiers.parseSoftcoreOnly( text );
		known = DebugDatabase.appendModifier( known, softcore );

		String freepull = Modifiers.parseFreePull( text );
		known = DebugDatabase.appendModifier( known, freepull );

		String effect = Modifiers.parseEffect( text );
		known = DebugDatabase.appendModifier( known, effect );

		String duration = Modifiers.parseDuration( text );
		known = DebugDatabase.appendModifier( known, duration );

		return known;
	}

	private static final String parseStandardEnchantments( final String text, final ArrayList<String> unknown, final Pattern pattern )
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
				known = DebugDatabase.appendModifier( known, mod );
				continue;
			}

			if ( !unknown.contains( enchantment ) )
			{
				unknown.add( enchantment );
			}
		}

		return known;
	}

	private static final String appendModifier( final String known, final String mod )
	{
                return mod == null ? known : known.equals( "" ) ? mod : known + ", " + mod;
	}

	// **********************************************************

	// Support for the "checkeffects" command, which compares KoLmafia's
	// internal status effect data from what can be mined from the effect
	// description.

	private static final String EFFECT_HTML = "effecthtml.txt";
	private static final String EFFECT_DATA = "effectdata.txt";
	private static final StringArray rawEffects = new StringArray();
	private static final Map<String, String> effects = new TreeMap<String, String>();

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
		Set<Integer> keys = EffectDatabase.descriptionIdKeySet();
		Iterator<Integer> it = keys.iterator();

		while ( it.hasNext() )
		{
			int id = it.next().intValue();
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

		int id = DebugDatabase.parseEffectId( text );
		if ( id != effectId )
		{
			report.println( "# *** " + name + " (" + effectId + ") should have effectId " + id + "." );
		}

		String descriptionName = DebugDatabase.parseName( text );
		if ( !name.equalsIgnoreCase( StringUtilities.getCanonicalName( descriptionName ) ) )
		{
			report.println( "# *** " + name + " (" + effectId + ") has description of " + descriptionName + "." );
			return;
		}

		String descriptionImage = DebugDatabase.parseImage( rawText );
		if ( !descriptionImage.equals( EffectDatabase.getImageName( id ) ) )
		{
			report.println( "# *** " + name + " (" + effectId + ") has image of " + descriptionImage + "." );
			return;
		}

		DebugDatabase.effects.put( name, text );
	}

	// <!-- effectid: 806 -->
	private static final Pattern EFFECTID_PATTERN = Pattern.compile( "<!-- effectid: ([\\d]*) -->" );
	public static final int parseEffectId( final String text )
	{
		Matcher matcher = DebugDatabase.EFFECTID_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final Pattern IMAGE_PATTERN = Pattern.compile( "itemimages/(.*?\\.gif)" );
	public static final String parseImage( final String text )
	{
		Matcher matcher = DebugDatabase.IMAGE_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	// href="desc_effect.php?whicheffect=138ba5cbeccb6334a1d473710372e8d6"
	private static final Pattern EFFECT_DESCID_PATTERN = Pattern.compile( "whicheffect=(.*?)\"" );
	public static final String parseEffectDescid( final String text )
	{
		Matcher matcher = DebugDatabase.EFFECT_DESCID_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final GenericRequest DESC_EFFECT_REQUEST = new GenericRequest( "desc_effect.php" );

	public static final String effectDescriptionText( final int effectId )
	{
                return DebugDatabase.effectDescriptionText( DebugDatabase.rawEffectDescriptionText( effectId ) );
	}

	public static final String readEffectDescriptionText( final String descId )
	{
		DebugDatabase.DESC_EFFECT_REQUEST.clearDataFields();
		DebugDatabase.DESC_EFFECT_REQUEST.addFormField( "whicheffect", descId );
		RequestThread.postRequest( DebugDatabase.DESC_EFFECT_REQUEST );
		return DebugDatabase.DESC_EFFECT_REQUEST.responseText;
	}

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

		String text = DebugDatabase.readEffectDescriptionText( descId );
		DebugDatabase.rawEffects.set( effectId, text );

		return text;
	}

	private static final Pattern EFFECT_DATA_PATTERN = Pattern.compile( "<div id=\"description\">(.*?)</div>", Pattern.DOTALL );

	private static final String effectDescriptionText( final String rawText )
	{
		if ( rawText == null )
		{
			return null;
		}

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

	private static final void checkEffectModifierMap( final PrintStream report, final Map<String, String> map, final String tag )
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
			String text = map.get( name );
			DebugDatabase.checkEffectModifierDatum( name, text, report );
		}
	}

	private static final Pattern EFFECT_ENCHANTMENT_PATTERN =
		Pattern.compile( "<font color=blue><b>(.*)</b></font>", Pattern.DOTALL );

	public static final String parseEffectEnchantments( final String text, final ArrayList<String> unknown )
	{
		return parseStandardEnchantments( text, unknown, DebugDatabase.EFFECT_ENCHANTMENT_PATTERN );
	}

	private static final void checkEffectModifierDatum( final String name, final String text, final PrintStream report )
	{
		ArrayList<String> unknown = new ArrayList<String>();
		String known = DebugDatabase.parseEffectEnchantments( text, unknown );
		DebugDatabase.logModifierDatum( name, known, unknown, report );
	}

	// **********************************************************

	// Utilities for dealing with KoL description data

	private static final PrintStream openReport( final String fileName )
	{
		return LogStream.openStream( new File( KoLConstants.DATA_LOCATION, fileName ), true );
	}

	private static final void loadScrapeData( final StringArray array, final String fileName )
	{
		if ( array.size() > 0 )
		{
			return;
		}

		try
		{
			File saveData = new File( KoLConstants.DATA_LOCATION, fileName );
			if ( !saveData.exists() )
			{
				return;
			}

			String currentLine;
			StringBuilder currentHTML = new StringBuilder();
			BufferedReader reader = FileUtilities.getReader( saveData );

			while ( ( currentLine = reader.readLine() ) != null && !currentLine.equals( "" ) )
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

	private static final void saveScrapeData( final Iterator<Integer> it, final StringArray array, final String fileName )
	{
		File file = new File( KoLConstants.DATA_LOCATION, fileName );
		PrintStream livedata = LogStream.openStream( file, true );

		while ( it.hasNext() )
		{
			int id = it.next().intValue();
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

	public static final void checkPlurals( int itemId )
	{
		RequestLogger.printLine( "Checking plurals..." );
		PrintStream report = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "plurals.txt" ), true );

		if ( itemId == 0 )
		{
			Iterator it = ItemDatabase.descriptionIdKeySet().iterator();
			++itemId;
			while ( it.hasNext() )
			{
				int id = ( (Integer) it.next() ).intValue();
				if ( id < 0 )
				{
					continue;
				}
				while ( itemId < id )
				{
					report.println( itemId++ );
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
		Integer id = IntegerPool.get( itemId );

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
		if ( access != null && !access.contains( ItemDatabase.QUEST_FLAG ) )
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

	// **********************************************************

	public static final void checkPowers( final String option )
	{
		// We can check the power of any items in inventory or closet.
		// We'll assume that any item with a non-zero power is correct.
		// Off-hand items and accessories don't have visible power and
		// might be 0 in the database. Look them up and fix them.

		if ( StringUtilities.isNumeric( option ) )
		{
			DebugDatabase.checkPower( StringUtilities.parseInt( option ), true );
			return;
		}

		boolean force = option.equals( "all" );
		DebugDatabase.checkPowers( KoLConstants.inventory, force );
		DebugDatabase.checkPowers( KoLConstants.closet, force );
		// DebugDatabase.checkPowers( KoLConstants.storage, force );
	}

	private static final void checkPowers( final Collection items, final boolean force  )
	{
		Iterator it = items.iterator();
		while ( it.hasNext() )
		{
			AdventureResult item = (AdventureResult)it.next();
			int itemId = item.getItemId();
			int type = ItemDatabase.getConsumptionType( itemId );
			if ( type == KoLConstants.EQUIP_OFFHAND || type == KoLConstants.EQUIP_ACCESSORY )
			{
				DebugDatabase.checkPower( itemId, force );
			}
		}
	}

	private static final void checkPower( final int itemId, final boolean force  )
	{
		int current = EquipmentDatabase.getPower( itemId );
		if ( !force && current != 0 )
		{
			return;
		}

		// Look it up and register it anew
		ApiRequest request = new ApiRequest( "item", itemId );
		RequestThread.postRequest( request );

		JSONObject JSON = request.JSON;
		if ( JSON == null )
		{
			KoLmafia.updateDisplay( "Could not look up item #" + itemId );
			return;
		}

		try
		{
			int power = JSON.getInt( "power" );

			// Yes, some items really are power 0
			if ( power == 0 || power == current )
			{
				return;
			}

			String name = JSON.getString( "name" );
			String descid = JSON.getString( "descid" );
			RequestLogger.printLine( "Item \"" + name +"\" power incorrect: " + current + " should be " + power );
			ItemDatabase.registerItem( itemId, name, descid, null, power );
		}
		catch ( JSONException e )
		{
			KoLmafia.updateDisplay( "Error parsing JSON string!" );
			StaticEntity.printStackTrace( e );
		}
	}

	// **********************************************************

	public static final void checkShields()
	{
		DebugDatabase.checkShields( KoLConstants.inventory );
		DebugDatabase.checkShields( KoLConstants.closet );
		DebugDatabase.checkShields( KoLConstants.storage );
	}

	public static final void checkShields( final Collection items )
	{
		Iterator it = items.iterator();
		while ( it.hasNext() )
		{
			AdventureResult item = (AdventureResult)it.next();
			int itemId = item.getItemId();
			if ( !EquipmentDatabase.getItemType( itemId ).equals( "shield" ) )
			{
				continue;
			}

			ApiRequest request = new ApiRequest( "item", itemId );
			RequestThread.postRequest( request );

			JSONObject JSON = request.JSON;
			if ( JSON == null )
			{
				continue;
			}

			try
			{
				int oldPower = EquipmentDatabase.getPower( itemId );
				int correctPower = JSON.getInt( "power" );
				if ( oldPower == correctPower )
				{
					continue;
				}

				String name = JSON.getString( "name" );
				String descid = JSON.getString( "descid" );

				RequestLogger.printLine( "Shield \"" + name +"\" power incorrect: " + oldPower + " should be " + correctPower );
				ItemDatabase.registerItem( itemId, name, descid, null, correctPower );
			}
			catch ( JSONException e )
			{
				KoLmafia.updateDisplay( "Error parsing JSON string!" );
				StaticEntity.printStackTrace( e );
			}
		}
	}

	// **********************************************************

	public static final void checkPotions()
	{
		Set keys = ItemDatabase.descriptionIdKeySet();
		Iterator it = keys.iterator();

		while ( it.hasNext() )
		{
			Integer id = ( (Integer) it.next() );
			int itemId = id.intValue();
			if ( itemId < 1 || !ItemDatabase.isUsable( itemId ) )
			{
				continue;
			}

			// Potions grant an effect. Check for a new effect.
			String itemName = ItemDatabase.getItemDataName( id );
			String effectName = Modifiers.getStringModifier( itemName, "Effect" );
			if ( !effectName.equals( "" ) && EffectDatabase.getEffectId( effectName ) == -1 )
			{
				String rawText = DebugDatabase.rawItemDescriptionText( itemId );
				String effectDescid = DebugDatabase.parseEffectDescid( rawText );
				EffectDatabase.registerEffect( effectName, effectDescid, "use 1 " + itemName );
			}
		}
	}

	// **********************************************************

	private static final String CONSUMABLE_DATA = "consumables.txt";

	public static final void checkConsumables()
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawItems, ITEM_HTML );
		RequestLogger.printLine( "Checking internal data..." );
		PrintStream report = DebugDatabase.openReport( CONSUMABLE_DATA );
		DebugDatabase.checkConsumables( report );
		report.close();
	}

	private static final void checkConsumables( final PrintStream report )
	{
		DebugDatabase.checkConsumables( report, ItemDatabase.fullnessByName, "Fullness" );
		DebugDatabase.checkConsumables( report, ItemDatabase.inebrietyByName, "Inebriety" );
		DebugDatabase.checkConsumables( report, ItemDatabase.spleenHitByName, "Spleenhit" );
	}

	private static final void checkConsumables( final PrintStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		report.println( "" );
		report.println( "# Consumption data in " + tag + ".txt" );
		report.println( "#" );

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			int size = ((Integer) map.get( name ) ).intValue();
			DebugDatabase.checkConsumable( report, name, size );
		}
	}

	private static final void checkConsumable( final PrintStream report, final String name, final int size )
	{
		int itemId = ItemDatabase.getItemId( name );
		// It is valid for items to have no itemId: sushi, Cafe offerings, and so on
		String text = itemId == -1 ? "" : DebugDatabase.itemDescriptionText( itemId, false );
		if ( text == null )
		{
			return;
		}

		int level = ItemDatabase.getLevelReqByName( name ).intValue();
		String adv = ItemDatabase.getAdvRangeByName( name );
		String quality = DebugDatabase.parseQuality( text );
		String mus = ItemDatabase.getMuscleByName( name );
		String mys = ItemDatabase.getMysticalityByName( name );
		String mox = ItemDatabase.getMoxieByName( name );
		String notes = ItemDatabase.getNotes( name );

		ItemDatabase.writeConsumable( report, name, size, level, quality, adv, mus, mys, mox, notes );
	}

	// Type: <b>food <font color=#999999>(crappy)</font></b>
	// Type: <b>food (decent)</b>
	// Type: <b>booze <font color=green>(good)</font></b>
	// Type: <b>food <font color=blue>(awesome)</font></b>
	// Type: <b>food <font color=blueviolet>(EPIC)</font></b>

	private static final Pattern QUALITY_PATTERN = Pattern.compile( "Type: <b>.*?\\((.*?)\\).*?</b>" );
	public static final String parseQuality( final String text )
	{
		Matcher matcher = DebugDatabase.QUALITY_PATTERN.matcher( text );
		return ItemDatabase.qualityValue( matcher.find() ? matcher.group( 1 ) : "" );
	}

	// **********************************************************

	public static final void checkFamiliars()
	{
		// Get familiar images from the familiar description
		for ( int i = 1; i <= FamiliarDatabase.maxFamiliarId; ++i )
		{
			DebugDatabase.checkFamiliar( i );
		}

		FamiliarDatabase.saveDataOverride();
	}

	private static final Pattern FAMILIAR_IMAGE_PATTERN = Pattern.compile( "http://images\\.kingdomofloathing\\.com/itemimages/(.*?\\.gif)" );
	private static final void checkFamiliar( final int id )
	{
		String file = "desc_familiar.php?which=" + String.valueOf( id );
		GenericRequest request = new GenericRequest( file );
		RequestThread.postRequest( request );
		String text = request.responseText;
		if ( text == null )
		{
			return;
		}
		Matcher matcher = FAMILIAR_IMAGE_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			FamiliarDatabase.setFamiliarImageLocation( id, matcher.group( 1 ) );
		}
	}

	// **********************************************************

	public static final void checkConsumptionData()
	{
		RequestLogger.printLine( "Checking consumption data..." );

		PrintStream writer = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "consumption.txt" ), true );

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

		PrintStream writer = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "pulvereport.txt" ), true );

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

		HashSet<Integer> seen = new HashSet<Integer>();
		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkPulverize( element, writer, seen );
		}

		for ( int id = 1; id <= ItemDatabase.maxItemId(); ++id )
		{
			int pulver = EquipmentDatabase.getPulverization( id );
			if ( pulver != -1 && !seen.contains( IntegerPool.get( id ) ) )
			{
				String name = ItemDatabase.getItemName( id );
				writer.println( name + ": not listed in anvil" );
			}
		}
	}

	private static final void checkPulverize( final Node element, final PrintStream writer,
		HashSet<Integer> seen )
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
				seen.add( IntegerPool.get( id ) );
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
		PrintStream report = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "zapreport.txt" ), true );

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
			ArrayList<String> items = new ArrayList<String>();
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

	private static void checkZapGroup( ArrayList<String> items, PrintStream report )
	{
		String firstItem = items.get( 0 );
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
			Iterator<String> i = items.iterator();
			while ( i.hasNext() )
			{
				report.print( i.next() );
				report.print( ", " );
			}
			report.println( "" );
			return;
		}
		ArrayList<String> existing = new ArrayList<String>();
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
		Iterator<String> i = items.iterator();
		while ( i.hasNext() )
		{
			report.print( i.next() );
			report.print( ", " );
		}
		report.println( "" );
		report.println( "Removed:" );
		i = existing.iterator();
		while ( i.hasNext() )
		{
			report.print( i.next() );
			report.print( ", " );
		}
		report.println( "" );
	}
}
