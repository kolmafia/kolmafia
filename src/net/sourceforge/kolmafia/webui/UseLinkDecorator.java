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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public abstract class UseLinkDecorator
{
	private static final Pattern ACQUIRE_PATTERN =
		Pattern.compile( "(You acquire|O hai, I made dis)([^<]*?)<b>(.*?)</b>(.*?)</td>", Pattern.DOTALL );

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( buffer.indexOf( "You acquire" ) == -1 &&
		     buffer.indexOf( "O hai, I made dis" ) == -1)
		{
			return;
		}

		// No use link if you get the item via pickpocketing; you're still in battle
		if ( buffer.indexOf( "deftly slip your fingers" ) != -1 )
		{
			return;
		}

		String text = buffer.toString();
		buffer.setLength( 0 );

		Matcher useLinkMatcher = ACQUIRE_PATTERN.matcher( text );

		int specialLinkId = 0;
		String specialLinkText = null;

		while ( useLinkMatcher.find() )
		{
			int itemCount = 1;
			String itemName = useLinkMatcher.group( 3 );

			int spaceIndex = itemName.indexOf( " " );
			if ( spaceIndex != -1 && useLinkMatcher.group( 2 ).indexOf( ":" ) == -1 )
			{
				itemCount = StringUtilities.parseInt( itemName.substring( 0, spaceIndex ) );
				itemName = itemName.substring( spaceIndex + 1 );
			}

			int itemId = ItemDatabase.getItemId( itemName, itemCount, false );
			if ( itemId == -1 )
			{
				// It's an effect or an unknown item
				continue;
			}

			// Certain items get use special links to minimize the amount
			// of scrolling to find the item again.

			if ( location.startsWith( "inventory.php" ) )
			{
				switch ( itemId )
				{
				case ItemPool.ICEBERGLET:
				case ItemPool.ICE_SICKLE:
				case ItemPool.ICE_BABY:
				case ItemPool.ICE_PICK:
				case ItemPool.ICE_SKATES:
					specialLinkId = itemId;
					specialLinkText = "squeeze";
					break;

				case ItemPool.MAKESHIFT_TURBAN:
				case ItemPool.MAKESHIFT_CAPE:
				case ItemPool.MAKESHIFT_SKIRT:
				case ItemPool.MAKESHIFT_CRANE:
				case ItemPool.TOWEL:
					specialLinkId = itemId;
					specialLinkText = "fold";
					break;
				}
			}

			UseLinkDecorator.addUseLink( itemId, itemCount, location, useLinkMatcher, buffer );
		}

		useLinkMatcher.appendTail( buffer );

		if ( specialLinkText != null )
		{
			StringUtilities.singleStringReplace(
				buffer,
				"</center></blockquote>",
				"<p><center><a href=\"inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=2&whichitem=" + specialLinkId + "\">[" + specialLinkText + " it again]</a></center></blockquote>" );
		}
		
		StringUtilities.singleStringReplace( buffer,
			"A sticker falls off your weapon, faded and torn.",
			"A sticker falls off your weapon, faded and torn. <font size=1>" +
			"[<a href=\"bedazzle.php\">bedazzle</a>]</font>" );
	}

	private static final boolean shouldAddCreateLink( int itemId, String location )
	{
		if ( location == null || location.indexOf( "combine.php" ) != -1 || location.indexOf( "cocktail.php" ) != -1 || location.indexOf( "cook.php" ) != -1 ||
			location.indexOf( "paster" ) != -1 || location.indexOf( "smith" ) != -1 )
		{
			return false;
		}

		// Retrieve the known ingredient uses for the item.
		SortedListModel creations = ConcoctionDatabase.getKnownUses( itemId );
		if ( creations.isEmpty() )
		{
			return false;
		}

		// Skip items which are multi-use or are mp restores.
		int consumeMethod = ItemDatabase.getConsumptionType( itemId );
		if ( consumeMethod == KoLConstants.CONSUME_MULTIPLE || consumeMethod == KoLConstants.MP_RESTORE )
		{
			return false;
		}

		switch ( itemId )
		{
		// If you find goat cheese, let the trapper link handle it.
		case ItemPool.GOAT_CHEESE:
			return false;

		// If you find ore, let the trapper link handle it.
		case ItemPool.LINOLEUM_ORE:
		case ItemPool.ASBESTOS_ORE:
		case ItemPool.CHROME_ORE:
			return false;

		// Dictionaries and bridges should link to the chasm quest.
		case ItemPool.DICTIONARY:
		case ItemPool.BRIDGE:
			return false;

		// Enchanted beans are primarily used for the beanstalk quest.
		case ItemPool.ENCHANTED_BEAN:
			return KoLCharacter.getLevel() < 10 || InventoryManager.hasItem( ItemPool.SOCK ) || InventoryManager.hasItem( ItemPool.ROWBOAT );
		}

		AdventureResult creation = null;
		CreateItemRequest irequest = null;
		int mixingMethod = KoLConstants.NOCREATE;

		for ( int i = 0; i < creations.size(); ++i )
		{
			creation = (AdventureResult) creations.get( i );
			mixingMethod = ConcoctionDatabase.getMixingMethod( creation );

			// Only accept if it's a creation method that the editor kit
			// currently understands and links.

			switch ( mixingMethod )
			{
			case KoLConstants.NOCREATE:
			case KoLConstants.PIXEL:
			case KoLConstants.ROLLING_PIN:
			case KoLConstants.CRIMBO05:
			case KoLConstants.CREATE_VIA_USE:
			case KoLConstants.STILL_BOOZE:
			case KoLConstants.STILL_MIXER:
			case KoLConstants.SMITH:
			case KoLConstants.SMITH_WEAPON:
			case KoLConstants.SMITH_ARMOR:
			case KoLConstants.CATALYST:
			case KoLConstants.STARCHART:
				continue;
			}

			irequest = CreateItemRequest.getInstance( creation );

			if ( ConcoctionDatabase.isPermittedMethod( mixingMethod ) && irequest != null && irequest.getQuantityPossible() > 0 )
			{
				return true;
			}
		}

		return false;
	}

	private static final void addUseLink( int itemId, int itemCount, String location, Matcher useLinkMatcher, StringBuffer buffer )
	{
		UseLink link;
		int consumeMethod = ItemDatabase.getConsumptionType( itemId );

		if ( shouldAddCreateLink( itemId, location ) )
		{
			link = getCreateLink( itemId, itemCount );
		}
		else if ( consumeMethod == KoLConstants.NO_CONSUME || consumeMethod == KoLConstants.COMBAT_ITEM )
		{
			link = getNavigationLink( itemId );
		}
		else
		{
			link = getUseLink( itemId, itemCount, consumeMethod );
		}

		if ( link == null )
		{
			return;
		}

		itemId = link.getItemId();
		itemCount = link.getItemCount();
		String useType = link.getUseType();
		String useLocation = link.getUseLocation();

		// If you can add a creation link, then add one instead.
		// That way, the player can click and KoLmafia will save
		// the player a click or two (well, if they trust it).

		if ( useLocation.equals( "#" ) )
		{
			useLinkMatcher.appendReplacement( buffer, "$1$2<b>$3</b>$4" );

			// Append a multi-use field rather than forcing
			// an additional page load.

			buffer.append( "</td></tr><tr><td colspan=2 align=center><div id=\"multiuse" );
			buffer.append( itemId );
			buffer.append( "\">" );

			buffer.append( "<form><input type=text size=3 id=\"quantity" );
			buffer.append( itemId );
			buffer.append( "\" value=" );
			buffer.append( Math.min( itemCount, UseItemRequest.maximumUses( itemId ) ) );
			buffer.append( ">&nbsp;<input type=button class=button value=\"Use\" onClick=\"multiUse('" );

			if ( ItemDatabase.getConsumptionType( itemId ) == KoLConstants.MP_RESTORE )
			{
				buffer.append( "skills.php" );
			}
			else
			{
				buffer.append( "multiuse.php" );
			}

			buffer.append( "'," );
			buffer.append( itemId );
			buffer.append( "); void(0);\"></form></div>" );
		}
		else if ( !Preferences.getBoolean( "relayUsesInlineLinks" ) || !useLocation.startsWith( "inv" ) )
		{
			useLinkMatcher.appendReplacement(
				buffer,
				"$1$2<b>$3</b> <font size=1>[<a href=\"" + useLocation + "\">" + useType + "</a>]</font>" );
		}
		else
		{
			String[] pieces = useLocation.toString().split( "\\?" );

			useLinkMatcher.appendReplacement(
				buffer,
				"$1$2<b>$3</b> <font size=1>[<a href=\"javascript: " + "singleUse('" + pieces[ 0 ].trim() + "','" + pieces[ 1 ].trim() + "'); void(0);\">" + useType + "</a>]</font>" );
		}

		buffer.append( "</td>" );
	}

	private static final UseLink getCreateLink( int itemId, int itemCount )
	{
		switch ( ConcoctionDatabase.getMixingMethod( itemId ) )
		{
		case KoLConstants.COMBINE:
			return new UseLink( itemId, itemCount, "combine", KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "craft.php?mode=combine&a=" );

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
			return new UseLink( itemId, itemCount, "mix", "craft.php?mode=cocktail&a=" );

		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:
			return new UseLink( itemId, itemCount, "cook", "craft.php?mode=cook&a=" );

		case KoLConstants.JEWELRY:
		case KoLConstants.EXPENSIVE_JEWELRY:
			return new UseLink( itemId, itemCount, "jewelry", "craft.php?mode=jewelry&a=" );
		}

		return null;
	}

	private static final UseLink getUseLink( int itemId, int itemCount, int consumeMethod )
	{
		switch ( consumeMethod )
		{
		case KoLConstants.GROW_FAMILIAR:

			if ( itemId  == ItemPool.MOSQUITO_LARVA )
			{
				return new UseLink( itemId, "council", "council.php" );
			}

			return new UseLink( itemId, "grow", "inv_familiar.php?whichitem=" );

		case KoLConstants.CONSUME_EAT:

			if ( itemId == ItemPool.GOAT_CHEESE )
			{
				return new UseLink( itemId, InventoryManager.getCount( itemId ), "trapper.php" );
			}

			if ( !KoLCharacter.canEat() )
			{
				return null;
			}

			return new UseLink( itemId, itemCount, "eat", "inv_eat.php?which=1&whichitem=" );

		case KoLConstants.CONSUME_DRINK:

			if ( !KoLCharacter.canDrink() )
			{
				return null;
			}

			return new UseLink( itemId, itemCount, "drink", "inv_booze.php?which=1&whichitem=" );
		
		case KoLConstants.CONSUME_FOOD_HELPER:
			if ( !KoLCharacter.canEat() )
			{
				return null;
			}
			return new UseLink( itemId, 1, "eat with", "inv_use.php?which=1&whichitem=" );
		
		case KoLConstants.CONSUME_DRINK_HELPER:
			if ( !KoLCharacter.canDrink() )
			{
				return null;
			}
			return new UseLink( itemId, 1, "drink with", "inv_use.php?which=1&whichitem=" );
		
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:

			int useCount = Math.min( UseItemRequest.maximumUses( itemId ), InventoryManager.getCount( itemId ) );

			if ( useCount == 0 )
			{
				return null;
			}

			if ( useCount == 1 )
			{
				String page = ( consumeMethod == KoLConstants.CONSUME_MULTIPLE ) ? "3" : "1";
				return new UseLink( itemId, useCount, "use", "inv_use.php?which=" + page + "&whichitem=" );
			}

			if ( Preferences.getBoolean( "relayUsesInlineLinks" ) )
			{
				return new UseLink( itemId, useCount, "use", "#" );
			}

			return new UseLink( itemId, useCount, "use", "multiuse.php?passitem=" );

		case KoLConstants.CONSUME_USE:
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.INFINITE_USES:

			switch ( itemId )
			{
			case ItemPool.MACGUFFIN_DIARY:

				return new UseLink( itemId, itemCount, "read", "diary.php?textversion=1" );

			case ItemPool.SPOOKY_MAP:
			case ItemPool.SPOOKY_SAPLING:
			case ItemPool.SPOOKY_FERTILIZER:

				if ( !InventoryManager.hasItem( ItemPool.SPOOKY_MAP ) ||
				     !InventoryManager.hasItem( ItemPool.SPOOKY_SAPLING ) ||
				     !InventoryManager.hasItem( ItemPool.SPOOKY_FERTILIZER ) )
				{
					return null;
				}

				return new UseLink( ItemPool.SPOOKY_MAP, 1, "map", "inv_use.php?which=3&whichitem=" );

			case ItemPool.DINGHY_PLANS:

				if ( InventoryManager.hasItem( ItemPool.DINGY_PLANKS ) )
				{
					return new UseLink( itemId, 1, "use", "inv_use.php?which=3&whichitem=" );
				}

				if ( HermitRequest.getWorthlessItemCount() == 0 )
				{
					return new UseLink( ItemPool.DINGY_PLANKS, 1, "planks", "hermit.php?autopermit=on&action=trade&quantity=1&whichitem=" );
				}

				return new UseLink( itemId, "sewer", "sewer.php" );

			case ItemPool.TOWEL:

				return new UseLink( itemId, itemCount, "fold", "inv_use.php?which=3&whichitem=" );

			default:

				return new UseLink( itemId, itemCount, "use", "inv_use.php?which=3&whichitem=" );
			}

		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_FAMILIAR:

			int outfit = EquipmentDatabase.getOutfitWithItem( itemId );

			if ( outfit != -1 && EquipmentManager.hasOutfit( outfit ) )
			{
				return new UseLink( itemId, itemCount, "outfit", "inv_equip.php?action=outfit&which=2&whichoutfit=" + outfit );
			}

			return new UseLink( itemId, itemCount, "equip", "inv_equip.php?which=2&action=equip&whichitem=" );

		case KoLConstants.CONSUME_ZAP:

			return new UseLink( itemId, itemCount, "zap", "wand.php?whichwand=" );
		}

		return null;
	}

	private static final UseLink getNavigationLink( int itemId )
	{
		String useType = null;
		String useLocation = null;

		switch ( itemId )
		{
		// Soft green echo eyedrop antidote gets an uneffect link

		case ItemPool.REMEDY:

			useType = "use";
			useLocation = "uneffect.php";
			break;

		// Strange leaflet gets a quick 'read' link which sends you
		// to the leaflet completion page.

		case ItemPool.STRANGE_LEAFLET:

			useType = "read";
			useLocation = "leaflet.php?action=auto";
			break;

		// Hedge maze puzzle and hedge maze key have a link to the maze
		// for easy access.

		case ItemPool.HEDGE_KEY:
		case ItemPool.PUZZLE_PIECE:

			useType = "maze";
			useLocation = "hedgepuzzle.php";
			break;

		// Pixels have handy links indicating how many white pixels are
		// present in the player's inventory.

		case ItemPool.WHITE_PIXEL:
		case ItemPool.RED_PIXEL:
		case ItemPool.GREEN_PIXEL:
		case ItemPool.BLUE_PIXEL:

			int whiteCount = InventoryManager.getCount( ItemPool.WHITE_PIXEL ) + CreateItemRequest.getInstance( ItemPool.WHITE_PIXEL ).getQuantityPossible();
			useType = whiteCount + " white";
			useLocation = "mystic.php";
			break;

		// Special handling for star charts, lines, and stars, where
		// KoLmafia shows you how many of each you have.

		case ItemPool.STAR_CHART:
		case ItemPool.STAR:
		case ItemPool.LINE:

			useType = InventoryManager.getCount( ItemPool.STAR_CHART ) + "," + InventoryManager.getCount( ItemPool.STAR ) + "," + InventoryManager.getCount( ItemPool.LINE );
			useLocation = "starchart.php";
			break;

		// Worthless items get a link to the hermit.

		case ItemPool.WORTHLESS_TRINKET:
		case ItemPool.WORTHLESS_GEWGAW:
		case ItemPool.WORTHLESS_KNICK_KNACK:

			useType = "hermit";
			useLocation = "hermit.php?autopermit=on";
			break;

		// The different kinds of ores will only have a link if they're
		// the ones applicable to the trapper quest.

		case ItemPool.LINOLEUM_ORE:
		case ItemPool.ASBESTOS_ORE:
		case ItemPool.CHROME_ORE:

			if ( itemId != ItemDatabase.getItemId( Preferences.getString( "trapperOre" ) ) )
			{
				return null;
			}

			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = "trapper.php";
			break;

		// Disintegrating sheet music gets a link which lets you sing it
		// to yourself.  We'll call it "sing" for now.

		case ItemPool.SHEET_MUSIC:

			useType = "sing";
			useLocation = "curse.php?action=use&targetplayer=" + KoLCharacter.getPlayerId() + "&whichitem=";
			break;

		// Link which uses the plans when you acquire the planks.

		case ItemPool.DINGY_PLANKS:

			if ( !InventoryManager.hasItem( ItemPool.DINGHY_PLANS ) )
			{
				return null;
			}

			useType = "plans";
			useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
			itemId = ItemPool.DINGHY_PLANS;
			break;

		// Link which use the Knob map when you get the encryption key.

		case ItemPool.ENCRYPTION_KEY:

			if ( !InventoryManager.hasItem( ItemPool.COBBS_KNOB_MAP ) )
			{
				return null;
			}

			useType = "use map";
			useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
			itemId = ItemPool.COBBS_KNOB_MAP;
			break;

		// Link to the guild upon completion of the Citadel quest.

		case ItemPool.CITADEL_SATCHEL:

			useType = "guild";
			useLocation = "guild.php?place=paco";
			break;
		
		// Link to the guild when receiving guild quest items.
		
		case ItemPool.FERNSWARTHYS_KEY:
		case ItemPool.DUSTY_BOOK:
			useType = "guild";
			useLocation = "guild.php?place=ocg";
			break;

		// Link to the untinkerer if you find an abridged dictionary.

		case ItemPool.ABRIDGED:

			useType = "untinker";
			useLocation = "town_right.php?action=untinker&whichitem=";
			break;

		// Link to the chasm if you just untinkered a dictionary.

		case ItemPool.BRIDGE:
		case ItemPool.DICTIONARY:

			useType = "chasm";
			useLocation = "mountains.php?orcs=1";
			break;

		// Link to the frat house if you acquired a Spanish Fly

		case ItemPool.SPANISH_FLY:

			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = "adventure.php?snarfblat=27";
			break;

		// Link to Big Brother if you pick up a sand dollar

		case ItemPool.SAND_DOLLAR:

			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = "monkeycastle.php?who=2";
			break;

		// Link to the Old Man if you buy the damp old boot

		case ItemPool.DAMP_OLD_BOOT:

			useType = "old man";
			useLocation = "oldman.php?action=talk";
			break;

		// Link to use the Orcish Frat House Blueprints

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			
			useType = "use";
			useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
			break;

		// Link to use the Black Market Map if you get blackbird parts

		case ItemPool.BROKEN_WINGS:
		case ItemPool.SUNKEN_EYES:

			if ( !InventoryManager.hasItem( ItemPool.BROKEN_WINGS ) ||
			     !InventoryManager.hasItem( ItemPool.SUNKEN_EYES ) ||
			     !InventoryManager.hasItem( ItemPool.BLACK_MARKET_MAP ) )
			{
				return null;
			}

			useType = "use map";
			useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
			itemId = ItemPool.BLACK_MARKET_MAP;
			break;

		default:

			// Bounty items get a count and a link to the Bounty
			// Hunter Hunter.

			if ( ItemDatabase.isBountyItem( itemId ) )
			{
				if ( itemId != Preferences.getInteger( "currentBountyItem" ) )
				{
					Preferences.setInteger( "currentBountyItem", itemId );
				}
				useType = String.valueOf( InventoryManager.getCount( itemId ) );
				useLocation = "bhh.php";
			}
		}

		if ( useType == null || useLocation == null )
		{
			return null;
		}

		return new UseLink( itemId, useType, useLocation );
	}

	private static class UseLink
	{
		private int itemId;
		private int itemCount;
		private String useType;
		private String useLocation;

		public UseLink( int itemId, String useType, String useLocation )
		{
			this( itemId, 1, useType, useLocation );
		}

		public UseLink( int itemId, int itemCount, String useLocation )
		{
			this( itemId, itemCount, String.valueOf( itemCount ), useLocation );
		}

		public UseLink( int itemId, int itemCount, String useType, String useLocation )
		{
			this.itemId = itemId;
			this.itemCount = itemCount;
			this.useType = useType;
			this.useLocation = useLocation;

			if ( this.useLocation.endsWith( "=" ) )
			{
				this.useLocation += this.itemId;
			}

			int formIndex = this.useLocation.indexOf( "?" );
			if ( formIndex != -1 )
			{
				this.useLocation = this.useLocation.substring( 0, formIndex ) + "?pwd=" + GenericRequest.passwordHash + "&" +
					this.useLocation.substring( formIndex + 1 );
			}
		}

		public int getItemId()
		{
			return this.itemId;
		}

		public int getItemCount()
		{
			return this.itemCount;
		}

		public String getUseType()
		{
			return this.useType;
		}

		public String getUseLocation()
		{
			return this.useLocation;
		}
	}
}
