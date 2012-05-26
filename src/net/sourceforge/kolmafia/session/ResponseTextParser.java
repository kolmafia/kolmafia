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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.pages.PageRegistry;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AccountRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.ArtistRequest;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo09Request;
import net.sourceforge.kolmafia.request.Crimbo10Request;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.CurseRequest;
import net.sourceforge.kolmafia.request.CustomOutfitRequest;
import net.sourceforge.kolmafia.request.DigRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HedgePuzzleRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HeyDezeRequest;
import net.sourceforge.kolmafia.request.HiddenCityRequest;
import net.sourceforge.kolmafia.request.KnollRequest;
import net.sourceforge.kolmafia.request.LeafletRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.MushroomRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.NemesisRequest;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.PhineasRequest;
import net.sourceforge.kolmafia.request.PixelRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.PyroRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RabbitHoleRequest;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StarChartRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.SugarSheetRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.Tr4pz0rRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.TrophyHutRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.VendingMachineRequest;
import net.sourceforge.kolmafia.request.VolcanoIslandRequest;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.MineDecorator;

public class ResponseTextParser
{
	private static final Pattern NEWSKILL1_PATTERN =
			Pattern.compile( "<td>You (have learned|learn) a new skill: <b>(.*?)</b>" );
	private static final Pattern NEWSKILL2_PATTERN = Pattern.compile( "whichskill=(\\d+)" );

	// You acquire a skill:&nbsp;&nbsp;</td><td><img src="http://images.kingdomofloathing.com/itemimages/wosp_stink.gif" onClick='javascript:poop("desc_skill.php?whichskill=67&self=true","skill", 350, 300)' width=30 height=30></td><td><b><a onClick='javascript:poop("desc_skill.php?whichskill=67&self=true","skill", 350, 300)'>Stinkpalm</a></b>
	private static final Pattern NEWSKILL3_PATTERN =
		Pattern.compile( "You (?:gain|acquire) a skill:.*?<[bB]>(?:<a[^>]*>)?(.*?)(?:</a>)?</[bB]>" );
	private static final Pattern RECIPE_PATTERN = Pattern.compile( "You learn to craft a new item: <b>(.*?)</b>" );
	private static final Pattern RECIPE2_PATTERN = Pattern.compile( "You have discovered a new recipe: <b>(.*?)</b>" );
	private static final Pattern DESCITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern DESCEFFECT_PATTERN = Pattern.compile( "whicheffect=([0-9a-zA-Z]+)" );

	public static boolean hasResult( final String location )
	{
		String path = location;
		String queryString = "";

		int queryStringBegin = location.indexOf( '?' );

		if ( queryStringBegin != -1 )
		{
			path = location.substring( 0, queryStringBegin );
			queryString = location.substring( queryStringBegin + 1 );
		}

		boolean hasResult = PageRegistry.isGameAction( path, queryString );

		if ( !hasResult )
		{
			return false;
		}

		if ( location.startsWith( "lchat.php" ) ||
		     location.startsWith( "newchatmessages.php" ) ||
		     location.startsWith( "submitnewchat.php" ) )
		{
			return false;
		}

		if ( location.endsWith( "menu.php") ||
		     location.startsWith( "actionbar" ) )
		{
			return false;
		}

		if ( location.startsWith( "api.php" ) ||
		     location.startsWith( "game.php" ) ||
		     location.startsWith( "charsheet" ) ||
		     location.startsWith( "desc" ) ||
		     location.startsWith( "quest" ) )
		{

			return false;
		}

		if ( location.startsWith( "makeoffer" ) ||
		     location.startsWith( "message" ) ||
		     location.startsWith( "display" ) ||
		     location.startsWith( "managecollectionshelves" ) ||
		     location.startsWith( "search" ) ||
		     location.startsWith( "show" ) )
		{
			return false;
		}

		if ( location.startsWith( "valhalla" ) )
		{
			return false;
		}

		if ( location.startsWith( "clan" ) )
		{
			return location.startsWith( "clan_stash" ) ||
				location.startsWith( "clan_rumpus" ) ||
				location.startsWith( "clan_viplounge" ) ||
				location.startsWith( "clan_slimetube" ) ||
				location.startsWith( "clan_hobopolis" );
		}

		if ( location.startsWith( "login.php" ) ||
		     location.startsWith( "logout.php" ) )
		{
			return false;
		}

		return true;
	}

	public static final void externalUpdate( final String location, final String responseText )
	{
		if ( responseText == null || responseText.length() == 0 )
		{
			return;
		}

		if ( location.startsWith( "account.php" ) )
		{
			AccountRequest.parseAccountData( location, responseText );
		}

		else if ( location.startsWith( "account_contactlist.php" ) )
		{
			ContactListRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "account_manageoutfits.php" ) )
		{
			CustomOutfitRequest.parseResponse( location, responseText );
		}

		if ( location.startsWith( "api.php" ) )
		{
			ApiRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "arcade.php" ) )
		{
			ArcadeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "arena.php" ) )
		{
			CakeArenaRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "basement.php" ) )
		{
			BasementRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bedazzle.php" ) )
		{
			EquipmentRequest.parseBedazzlements( responseText );
		}

		else if ( location.startsWith( "beerpong.php" ) )
		{
			BeerPongRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bet.php" ) )
		{
			MoneyMakingGameRequest.parseResponse( location, responseText, false );
		}

		else if ( location.startsWith( "bhh.php" ) )
		{
			BountyHunterHunterRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bone_altar.php" ) )
		{
			AltarOfBonesRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "campground.php" ) )
		{
			CampgroundRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "cave.php" ) )
		{
			NemesisRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "charsheet.php" ) && location.indexOf( "ajax=1" ) == -1 )
		{
			CharSheetRequest.parseStatus( responseText );
		}

		else if ( location.startsWith( "choice.php" ) )
		{
			if ( location.indexOf( "whichchoice=562" ) != -1 )
			{
				FudgeWandRequest.parseResponse( location, responseText );
			}
		}

		else if ( location.startsWith( "clan_rumpus.php" ) )
		{
			ClanRumpusRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "clan_stash.php" ) )
		{
			ClanStashRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "clan_viplounge.php" ) )
		{
			ClanLoungeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "closet.php" ) || location.startsWith( "fillcloset.php" ) )
		{
			ClosetRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "craft.php" ) )
		{
			CreateItemRequest.parseCrafting( location, responseText );
		}

		else if ( location.startsWith( "crimbo09.php" ) )
		{
			Crimbo09Request.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "crimbo10.php" ) )
		{
			Crimbo10Request.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "crimbo11.php" ) )
		{
			Crimbo11Request.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "curse.php" ) )
		{
			CurseRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "da.php" ) )
		{
			ShrineRequest.parseResponse( location, responseText );
			VendingMachineRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "familiar.php" ) && location.indexOf( "ajax=1" ) == -1 )
		{
			FamiliarData.registerFamiliarData( responseText );
		}

		else if ( location.startsWith( "familiarbinger.php" ) )
		{
			UseItemRequest.parseBinge( location, responseText );
		}

		else if ( location.startsWith( "galaktik.php" ) )
		{
			GalaktikRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "gamestore.php" ) )
		{
			GameShoppeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "guild.php" ) )
		{
			GuildRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "hedgepuzzle.php" ) )
		{
			HedgePuzzleRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "hermit.php" ) )
		{
			HermitRequest.parseHermitTrade( location, responseText );
		}

		else if ( location.startsWith( "heydeze.php" ) )
		{
			HeyDezeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "hiddencity.php" ) )
		{
			HiddenCityRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "desc_skill.php" ) && location.indexOf( "self=true" ) != -1 )
		{
			Matcher m = ResponseTextParser.NEWSKILL2_PATTERN.matcher( location );
			if ( m.find() )
			{
				int skill = StringUtilities.parseInt( m.group( 1 ) );
				ConsequenceManager.parseSkillDesc( skill, responseText );
			}
		}

		else if ( location.startsWith( "desc_item.php" ) && location.indexOf( "otherplayer=" ) == -1 )
		{
			Matcher m = ResponseTextParser.DESCITEM_PATTERN.matcher( location );
			if ( m.find() )
			{
				ConsequenceManager.parseItemDesc( m.group( 1 ), responseText );
			}
		}

		else if ( location.startsWith( "desc_effect.php" ) )
		{
			Matcher m = ResponseTextParser.DESCEFFECT_PATTERN.matcher( location );
			if ( m.find() )
			{
				ConsequenceManager.parseEffectDesc( m.group( 1 ), responseText );
			}
		}

		else if ( location.startsWith( "dig.php" ) )
		{
			DigRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "dwarfcontraption.php" ) )
		{
			DwarfContraptionRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "dwarffactory.php" ) )
		{
			DwarfFactoryRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "forestvillage.php" ) )
		{
			UntinkerRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "friars.php" ) )
		{
			FriarRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "gamestore" ) )
		{
			GameShoppeRequest.parseResponse( location, responseText );
		}

		// Keep your current equipment and familiars updated, if you
		// visit the appropriate pages.

		else if ( location.startsWith( "inventory.php" ) )
		{
			// If KoL is showing us our current equipment, parse it.
			if ( location.indexOf( "which=2" ) != -1 || location.indexOf( "curequip=1" ) != -1 )
			{
				EquipmentRequest.parseEquipment( location, responseText );

				// Slimeling binge requests come here, too
				if ( location.indexOf( "action=slime" ) != -1 )
				{
					UseItemRequest.parseBinge( location, responseText );
				}
				// Certain requests, like inserting cards into
				// an El Vibrato helmet, have a usage message,
				// not an equipment page. Check for that, too.
				else
				{
					UseItemRequest.parseConsumption( responseText, false );
				}
			}

			// If there is a consumption message, parse it
			else if ( location.indexOf( "action=message" ) != -1 || location.indexOf( "action=breakbricko" ) != -1 )
			{
				UseItemRequest.parseConsumption( responseText, false );
				AWOLQuartermasterRequest.parseResponse( location, responseText );
				BURTRequest.parseResponse( location, responseText );
			}

			// If there is a binge message, parse it
			else if ( location.indexOf( "action=ghost" ) != -1 || location.indexOf( "action=hobo" ) != -1 || location.indexOf( "action=slime" ) != -1 || location.indexOf( "action=candy" ) != -1 )
			{
				UseItemRequest.parseBinge( location, responseText );
			}
			else if ( location.indexOf( "action=closetpush" ) != -1 || location.indexOf( "action=closetpull" ) != -1 )
			{
				ClosetRequest.parseTransfer( location, responseText );
			}

		}

		else if ( location.startsWith( "inv_equip.php" ) && location.indexOf( "ajax=1" ) != -1 )
		{
			// If we are changing equipment via a chat command,
			// try to deduce what changed.
			EquipmentRequest.parseEquipmentChange( location, responseText );
		}

		else if ( ( location.startsWith( "inv_eat.php" ) || location.startsWith( "inv_booze.php" ) || location.startsWith( "inv_use.php" ) || location.startsWith( "inv_familiar.php" ) ) && location.indexOf( "whichitem" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		else if ( location.startsWith( "knoll.php" ) )
		{
			KnollRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "knoll_mushrooms.php" ) )
		{
			MushroomRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "lair2.php" ) )
		{
			SorceressLairManager.parseEntrywayResponse( location, responseText );
		}

		else if ( location.startsWith( "lair6.php" ) )
		{
			SorceressLairManager.parseChamberResponse( location, responseText );
		}

		else if ( location.startsWith( "leaflet.php" ) )
		{
			LeafletRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mallstore.php" ) )
		{
			MallPurchaseRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "managecollection.php" ) )
		{
			DisplayCaseRequest.parseDisplayTransfer( location, responseText );
		}
		else if ( location.startsWith( "managecollectionshelves.php" ) )
		{
			DisplayCaseRequest.parseDisplayArrangement( location, responseText );
		}

		else if ( location.startsWith( "managestore.php" ) )
		{
			AutoMallRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "manor3" ) )
		{
			WineCellarRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mining.php" ) )
		{
			MineDecorator.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "monkeycastle" ) )
		{
			BigBrotherRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mrstore.php" ) )
		{
			MrStoreRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mystic.php" ) )
		{
			PixelRequest.parseResponse( location, responseText );
		}

		else if ( ( location.startsWith( "multiuse.php" ) || location.startsWith( "skills.php" ) ) && location.indexOf( "useitem" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		else if ( location.startsWith( "pandamonium.php" ) )
		{
			PandamoniumRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "peevpee.php" ) )
		{
			PeeVPeeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "pyramid.php" ) )
		{
			PyramidRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "questlog.php" ) )
		{
			QuestLogRequest.registerQuests( true, location, responseText );
		}

		else if ( location.startsWith( "rabbithole.php" ) )
		{
			RabbitHoleRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "raffle.php" ) )
		{
			RaffleRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sea_skatepark.php" ) )
		{
			SkateParkRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sellstuff.php" ) )
		{
			AutoSellRequest.parseCompactAutoSell( location, responseText );
		}

		else if ( location.startsWith( "sellstuff_ugly.php" ) )
		{
			AutoSellRequest.parseDetailedAutoSell( location, responseText );
		}

		else if ( location.startsWith( "sendmessage.php" ) )
		{
			SendMailRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "skills.php" ) )
		{
			if ( location.indexOf( "action=useditem" ) != -1 )
			{
				UseItemRequest.parseConsumption( responseText, false );
			}
			else
			{
				UseSkillRequest.parseResponse( location, responseText );
			}
		}

		else if ( location.startsWith( "spaaace.php" ) )
		{
			SpaaaceRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "starchart.php" ) )
		{
			StarChartRequest.parseCreation( location, responseText );
		}

		else if ( location.startsWith( "storage.php" ) )
		{
			StorageRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "store.php" ) )
		{
			NPCPurchaseRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "suburbandis.php" ) )
		{
			SuburbanDisRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sugarsheets.php" ) )
		{
			SugarSheetRequest.parseCreation( location, responseText );
		}

		else if ( location.startsWith( "sushi.php" ) )
		{
			SushiRequest.parseConsumption( location, responseText );
		}

		else if ( location.startsWith( "tavern.php" ) )
		{
			TavernRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "town_altar.php" ) )
		{
			AltarOfLiteracyRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "town_right.php" ) )
		{
			GourdRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "town_sendgift.php" ) )
		{
			SendGiftRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "town_wrong.php" ) )
		{
			ArtistRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "trapper.php" ) )
		{
			Tr4pz0rRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "traveler.php" ) )
		{
			TravelingTraderRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "trophy.php" ) )
		{
			TrophyHutRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "typeii.php" ) )
		{
			TrendyRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "volcanoisland.php" ) )
		{
			PhineasRequest.parseResponse( location, responseText );
			VolcanoIslandRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "volcanomaze.php" ) )
		{
			VolcanoMazeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "wand.php" ) )
		{
			ZapRequest.parseResponse( location, responseText );
		}

		else if ( location.indexOf( "action=pyro" ) != -1 )
		{
			PyroRequest.parseResponse( location, responseText );
		}

		// You can learn a skill on many pages.
		ResponseTextParser.learnSkill( location, responseText );

		// Currently, required recipes can only be learned via using an
		// item, but that's probably not guaranteed to be true forever.
		// Update: you can now learn them from the April Shower
		ResponseTextParser.learnRecipe( location, responseText );
	}

	public static void learnRecipe( String location, String responseText )
	{
		if ( !hasResult( location ) )
		{
			return;
		}

		Matcher matcher = ResponseTextParser.RECIPE_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			matcher = ResponseTextParser.RECIPE2_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				return;
			}
		}

		int id = ItemDatabase.getItemId( matcher.group( 1 ), 1, false );

		String message = "Learned recipe: " + matcher.group( 1 ) + " (" + id + ")";
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		if ( id <= 0 )
		{
			return;
		}

		Preferences.setBoolean( "unknownRecipe" + id, false );
		ConcoctionDatabase.setRefreshNeeded( false );
	}

	public static void learnSkill( final String location, final String responseText )
	{
		if ( !hasResult( location ) )
		{
			return;
		}

		// Don't parse skill acquisition via item use here, since
		// UseItemRequest will detect it.
		// Don't parse steel margarita/lasagna here, either.

		if ( location.startsWith( "inv_use.php" ) || location.startsWith( "inv_booze.php" )
			|| location.startsWith( "inv_eat.php" ) )
		{
			return;
		}

		// Unfortunately, if you learn a new skill from Frank
		// the Regnaissance Gnome at the Gnomish Gnomads
		// Camp, it doesn't tell you the name of the skill.
		// It simply says: "You leargn a new skill. Whee!"

		if ( responseText.indexOf( "You leargn a new skill." ) != -1 )
		{
			Matcher matcher = ResponseTextParser.NEWSKILL2_PATTERN.matcher( location );
			if ( matcher.find() )
			{
				int skillId = StringUtilities.parseInt( matcher.group( 1 ) );
				String skillName = SkillDatabase.getSkillName( skillId );
				ResponseTextParser.learnSkill( skillName );
				return;
			}
		}

		ResponseTextParser.learnSkillFromResponse( responseText );
	}

	public static void learnSkillFromResponse( final String responseText )
	{

		Matcher matcher = ResponseTextParser.NEWSKILL1_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			ResponseTextParser.learnSkill( matcher.group( 2 ) );
			return;
		}

		matcher = ResponseTextParser.NEWSKILL3_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			ResponseTextParser.learnSkill( matcher.group( 1 ) );
			return;
		}
	}

	public static final void learnSkill( final String skillName )
	{
		// The following skills are found in battle and result in
		// losing an item from inventory.

		if ( skillName.equals( "Snarl of the Timberwolf" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.TATTERED_WOLF_STANDARD ) )
			{
				ResultProcessor.processItem( ItemPool.TATTERED_WOLF_STANDARD, -1 );
			}
		}
		else if ( skillName.equals( "Spectral Snapper" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.TATTERED_SNAKE_STANDARD ) )
			{
				ResultProcessor.processItem( ItemPool.TATTERED_SNAKE_STANDARD, -1 );
			}
		}
		else if ( skillName.equals( "Scarysauce" ) || skillName.equals( "Fearful Fettucini" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY ) )
			{
				ResultProcessor.processItem( ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY, -1 );
			}
		}
		else if ( skillName.equals( "Tango of Terror" ) || skillName.equals( "Dirge of Dreadfulness" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC ) )
			{
				ResultProcessor.processItem( ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC, -1 );
			}
		}

		String message = "You learned a new skill: " + skillName;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
		KoLCharacter.addAvailableSkill( skillName );
		KoLCharacter.updateStatus();
		KoLCharacter.addDerivedSkills();
		KoLConstants.usableSkills.sort();
		DiscoCombatHelper.learnSkill( skillName );
		ConcoctionDatabase.setRefreshNeeded( true );
		if ( SkillDatabase.isBookshelfSkill( skillName ) )
		{
			KoLCharacter.setBookshelf( true );
		}
		PreferenceListenerRegistry.firePreferenceChanged( "(skill)" );
	}
}
