package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.pages.PageRegistry;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AccountRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanFortuneRequest;
import net.sourceforge.kolmafia.request.ClanHallRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeSwimmingPoolRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo09Request;
import net.sourceforge.kolmafia.request.Crimbo10Request;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.Crimbo21TreeRequest;
import net.sourceforge.kolmafia.request.CurseRequest;
import net.sourceforge.kolmafia.request.CustomOutfitRequest;
import net.sourceforge.kolmafia.request.DigRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.DreadsylvaniaRequest;
import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamTeamRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FleaMarketRequest;
import net.sourceforge.kolmafia.request.FleaMarketSellRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GnomeTinkerRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.GrandpaRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HeyDezeRequest;
import net.sourceforge.kolmafia.request.IslandRequest;
import net.sourceforge.kolmafia.request.LatteRequest;
import net.sourceforge.kolmafia.request.LeafletRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MomRequest;
import net.sourceforge.kolmafia.request.MonsterManuelRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.MushroomRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.NemesisRequest;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.PhineasRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.SeaMerkinRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.ShowClanRequest;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.SummoningChamberRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.TrophyHutRequest;
import net.sourceforge.kolmafia.request.TutorialRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.VolcanoIslandRequest;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;
import net.sourceforge.kolmafia.request.WitchessRequest;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.MineDecorator;

public class ResponseTextParser {
  private static final Pattern NEWSKILL1_PATTERN =
      Pattern.compile("<td>You (?:have learned|learn) a new skill: <b>(.*?)</b>");
  private static final Pattern NEWSKILL2_PATTERN = Pattern.compile("whichskill=(\\d+)");

  // You acquire a skill:&nbsp;&nbsp;</td><td><img
  // src="http://images.kingdomofloathing.com/itemimages/wosp_stink.gif"
  // onClick='javascript:poop("desc_skill.php?whichskill=67&self=true","skill", 350, 300)' width=30
  // height=30></td><td><b><a
  // onClick='javascript:poop("desc_skill.php?whichskill=67&self=true","skill", 350,
  // 300)'>Stinkpalm</a></b>
  private static final Pattern NEWSKILL3_PATTERN =
      Pattern.compile("You (?:gain|acquire) a skill:.*?whichskill=(\\d+)");
  private static final Pattern DESCITEM_PATTERN = Pattern.compile("whichitem=(\\d+)");
  private static final Pattern DESCEFFECT_PATTERN = Pattern.compile("whicheffect=([0-9a-zA-Z]+)");

  private ResponseTextParser() {}

  public static boolean hasResult(final String location) {
    if (location == null) {
      return false;
    }

    int queryStringBegin = location.indexOf('?');
    String path = location;
    String queryString = "";

    if (queryStringBegin != -1) {
      path = location.substring(0, queryStringBegin);
      queryString = location.substring(queryStringBegin + 1);
    }

    // *** This is an unfinished and unused feature.
    if (!PageRegistry.isGameAction(path, queryString)) {
      return false;
    }

    switch (path) {
      case "login.php", "logout.php" -> {
        return false;
      }
      case "api.php", "game.php", "doc.php" -> {
        return false;
      }
      case "topmenu.php", "awesomemenu.php" -> {
        return false;
      }
      case "actionbar.php" -> {
        return false;
      }
      case "lchat.php", "mchat.php", "newchatmessages.php", "submitnewchat.php" -> {
        // Nothing from chat.
        return false;
      }
      case "desc_effect.php",
          "desc_familiar.php",
          "desc_guardian.php",
          "desc_item.php",
          "desc_outfit.php",
          "desc_skill.php" -> {
        // No object descriptions
        return false;
      }
      case "showplayer.php" -> {
        // We want to register new items from Equipment and Familiar
        return true;
      }
      case "displaycollection.php" -> {
        // We want to register new items in display cases
        return true;
      }
      case "managecollectionshelves.php" -> {
        return false;
      }
      case "clan_stash.php",
          "clan_hall.php",
          "clan_rumpus.php",
          "clan_viplounge.php",
          "clan_hobopolis.php",
          "clan_slimetube.php",
          "clan_dreadsylvania.php" -> {
        // Other clan*.php URLs have no result.
        // Which are those?
        return true;
      }
      case "questlog.php" -> {
        return false;
      }
    }

    // Things not covered by above - but which could be, if we listed
    // all the appropriate urls.

    if (location.startsWith("static")) {
      return false;
    }

    if (location.startsWith("makeoffer")
        || location.startsWith("message")
        || location.startsWith("display")
        || location.startsWith("search")
        || location.startsWith("show")) {
      return false;
    }

    if (location.startsWith("valhalla")) {
      return false;
    }

    // Specific clan pages with results called out above
    if (location.startsWith("clan")) {
      return false;
    }

    if (location.startsWith("dev") || location.startsWith("ilovebugs.php")) {
      return false;
    }

    return true;
  }

  public static final void externalUpdate(final GenericRequest request) {
    String responseText = request.responseText;
    if (responseText == null || responseText.length() == 0) {
      return;
    }

    String location = request.getURLString();

    switch (request.getPage()) {
      case "account.php" -> {
        AccountRequest.parseAccountData(location, responseText);
      }
      case "account_contactlist.php" -> {
        ContactListRequest.parseResponse(location, responseText);
      }
      case "account_manageoutfits.php" -> {
        CustomOutfitRequest.parseResponse(location, responseText);
      }
      case "adventure.php" -> {
        SeaMerkinRequest.parseColosseumResponse(location, responseText);
      }
      case "api.php" -> {
        ApiRequest.parseResponse(location, responseText);
      }
      case "ascend.php" -> {
        if (location.contains("alttext=communityservice")
            && !Preferences.getBoolean("kingLiberated")) {
          // Redirect from donating body to science in Community Service
          ChoiceManager.canWalkAway();
          KoLCharacter.liberateKing();
        }
      }
      case "ascensionhistory.php" -> {
        AscensionHistoryRequest.parseResponse(location, responseText);
      }
      case "arena.php" -> {
        CakeArenaRequest.parseResponse(location, responseText);
      }
      case "backoffice.php" -> {
        ManageStoreRequest.parseResponse(location, responseText);
      }
      case "basement.php" -> {
        BasementRequest.checkBasement(responseText);
      }
      case "bedazzle.php" -> {
        EquipmentRequest.parseBedazzlements(responseText);
      }
      case "beerpong.php" -> {
        BeerPongRequest.parseResponse(location, responseText);
      }
      case "bigisland.php", "postwarisland.php" -> {
        IslandRequest.parseResponse(location, responseText);
      }
      case "bone_altar.php" -> {
        AltarOfBonesRequest.parseResponse(location, responseText);
      }
      case "bounty.php" -> {
        BountyHunterHunterRequest.parseResponse(location, responseText);
      }
      case "campground.php" -> {
        CampgroundRequest.parseResponse(location, responseText);
      }
      case "cafe.php" -> {
        ChezSnooteeRequest.parseResponse(location, responseText);
        MicroBreweryRequest.parseResponse(location, responseText);
      }
      case "cave.php" -> {
        NemesisRequest.parseResponse(location, responseText);
      }
      case "charsheet.php" -> {
        if (!location.contains("ajax=1")) {
          CharSheetRequest.parseStatus(responseText);
        }
      }
      case "choice.php" -> {
        // *** Seems like all of these could be in postChoice1
        if (location.contains("whichchoice=562")) {
          FudgeWandRequest.parseResponse(location, responseText);
        } else if (location.contains("whichchoice=585")) {
          ClanLoungeSwimmingPoolRequest.parseResponse(location, responseText);
        } else if (location.contains("whichchoice=922")) {
          SummoningChamberRequest.parseResponse(location, responseText);
        } else if (location.contains("whichchoice=1278")) {
          ClanFortuneRequest.parseResponse(location, responseText);
        }
      }
      case "clan_hall.php" -> {
        ClanHallRequest.parseResponse(location, responseText);
      }
      case "clan_rumpus.php" -> {
        ClanRumpusRequest.parseResponse(location, responseText);
      }
      case "clan_stash.php" -> {
        ClanStashRequest.parseTransfer(location, responseText);
      }
      case "clan_dreadsylvania.php" -> {
        DreadsylvaniaRequest.parseResponse(location, responseText);
      }
      case "clan_viplounge.php" -> {
        if (location.contains("preaction=lovetester")) {
          ClanFortuneRequest.parseResponse(location, responseText);
        } else {
          ClanLoungeRequest.parseResponse(location, responseText);
        }
      }
      case "closet.php", "fillcloset.php" -> {
        ClosetRequest.parseTransfer(location, responseText);
      }
      case "craft.php" -> {
        CreateItemRequest.parseCrafting(location, responseText);
      }
      case "crimbo09.php" -> {
        Crimbo09Request.parseResponse(location, responseText);
      }
      case "crimbo10.php" -> {
        Crimbo10Request.parseResponse(location, responseText);
      }
      case "crimbo11.php" -> {
        Crimbo11Request.parseResponse(location, responseText);
      }
      case "crimbo21tree.php" -> {
        Crimbo21TreeRequest.parseResponse(location, responseText);
      }
      case "curse.php" -> {
        CurseRequest.parseResponse(location, responseText);
      }
      case "crypt.php" -> {
        // Check if crypt areas have unexpectedly vanished and correct if so
        if (!responseText.contains("The Defiled Alcove")
                && Preferences.getInteger("cyrptAlcoveEvilness") > 0
            || !responseText.contains("The Defiled Cranny")
                && Preferences.getInteger("cyrptCrannyEvilness") > 0
            || !responseText.contains("The Defiled Niche")
                && Preferences.getInteger("cyrptNicheEvilness") > 0
            || !responseText.contains("The Defiled Nook")
                && Preferences.getInteger("cyrptNookEvilness") > 0) {
          if (InventoryManager.hasItem(ItemPool.EVILOMETER)) {
            RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.EVILOMETER));
          } else {
            // Must have completed quest and already used and lost Evilometer
            Preferences.setInteger("cyrptAlcoveEvilness", 0);
            Preferences.setInteger("cyrptCrannyEvilness", 0);
            Preferences.setInteger("cyrptNicheEvilness", 0);
            Preferences.setInteger("cyrptNookEvilness", 0);
            Preferences.setInteger("cyrptTotalEvilness", 0);
          }
        }
      }
      case "da.php" -> {
        ShrineRequest.parseResponse(location, responseText);
      }
      case "desc_skill.php" -> {
        Matcher m = ResponseTextParser.NEWSKILL2_PATTERN.matcher(location);
        if (m.find()) {
          int skill = StringUtilities.parseInt(m.group(1));
          String skillName = SkillDatabase.getSkillName(skill);
          if (skillName == null) {
            SkillDatabase.registerSkill(responseText, skill, null);
          }
          if (location.contains("self=true")) {
            ConsequenceManager.parseSkillDesc(skill, responseText);
          }
        }
      }
      case "desc_item.php" -> {
        if (!location.contains("otherplayer=")) {
          Matcher m = ResponseTextParser.DESCITEM_PATTERN.matcher(location);
          if (m.find()) {
            String descid = m.group(1);
            ConsequenceManager.parseItemDesc(descid, responseText);
            int itemId = ItemDatabase.getItemIdFromDescription(descid);

            boolean changesFromTimeToTime = true;

            switch (itemId) {
              case ItemPool.YEARBOOK_CAMERA -> ItemDatabase.parseYearbookCamera(responseText);
              case ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE -> ItemDatabase.parseRetroCape(
                  responseText);
              case ItemPool.HATSEAT -> ItemDatabase.parseCrownOfThrones(responseText);
              case ItemPool.BUDDY_BJORN -> ItemDatabase.parseBuddyBjorn(responseText);
              case ItemPool.FOURTH_SABER, ItemPool.REPLICA_FOURTH_SABER -> ItemDatabase.parseSaber(
                  responseText);
              case ItemPool.VAMPIRE_VINTNER_WINE -> ItemDatabase.parseVampireVintnerWine(
                  responseText);
              case ItemPool.COMBAT_LOVERS_LOCKET -> LocketManager.parseLocket(responseText);
              case ItemPool.UNBREAKABLE_UMBRELLA -> ItemDatabase.parseUmbrella(responseText);
              case ItemPool.JUNE_CLEAVER -> ItemDatabase.parseCleaver(responseText);
              case ItemPool.DESIGNER_SWEATPANTS,
                  ItemPool.REPLICA_DESIGNER_SWEATPANTS -> ItemDatabase.parseDesignerSweatpants(
                  responseText);
              case ItemPool.POWERFUL_GLOVE, ItemPool.REPLICA_POWERFUL_GLOVE -> ItemDatabase
                  .parsePowerfulGlove(responseText);
              case ItemPool.RING -> ItemDatabase.parseRing(responseText);
              case ItemPool.LATTE_MUG -> LatteRequest.parseDescription(responseText);
              default -> changesFromTimeToTime = false;
            }

            if (changesFromTimeToTime) {
              SpadingManager.processDescItem(ItemPool.get(itemId), responseText);
            }
          }
        }
      }
      case "desc_effect.php" -> {
        Matcher m = ResponseTextParser.DESCEFFECT_PATTERN.matcher(location);
        if (m.find()) {
          String descid = m.group(1);
          ConsequenceManager.parseEffectDesc(descid, responseText);
          int effectId = EffectDatabase.getEffectIdFromDescription(descid);
          switch (effectId) {
            case EffectPool.WINE_FORTIFIED,
                EffectPool.WINE_HOT,
                EffectPool.WINE_FRISKY,
                EffectPool.WINE_COLD,
                EffectPool.WINE_DARK,
                EffectPool.WINE_BEFOULED,
                EffectPool.WINE_FRIENDLY -> EffectDatabase.parseVampireVintnerWineEffect(
                responseText, effectId);
          }
        }
      }
      case "diary.php" -> {
        UseItemRequest.handleDiary(responseText);
      }
      case "dig.php" -> {
        DigRequest.parseResponse(location, responseText);
      }
      case "dwarfcontraption.php" -> {
        DwarfContraptionRequest.parseResponse(location, responseText);
      }
      case "dwarffactory.php" -> {
        DwarfFactoryRequest.parseResponse(location, responseText);
      }
      case "elvmachine.php" -> {
        ElVibratoManager.parseResponse(location, responseText);
      }
      case "familiar.php" -> {
        FamiliarRequest.parseResponse(location, responseText);
        if (!location.contains("ajax=1")) {
          FamiliarData.registerFamiliarData(responseText);
        }
      }
      case "famteam.php" -> {
        FamTeamRequest.parseResponse(location, responseText);
      }
      case "familiarbinger.php" -> {
        UseItemRequest.parseBinge(location, responseText);
      }
      case "friars.php" -> {
        FriarRequest.parseResponse(location, responseText);
      }
      case "gamestore.php" -> {
        GameShoppeRequest.parseResponse(location, responseText);
      }
      case "gnomes.php" -> {
        GnomeTinkerRequest.parseCreation(location, responseText);
      }
      case "guild.php" -> {
        GuildRequest.parseResponse(location, responseText);
      }
      case "hermit.php" -> {
        HermitRequest.parseHermitTrade(location, responseText);
      }
      case "heydeze.php" -> {
        HeyDezeRequest.parseResponse(location, responseText);
      }
      case "inventory.php" -> {
        // If KoL is showing us our current equipment, parse it.
        if (location.contains("which=2") || location.contains("curequip=1")) {
          EquipmentRequest.parseEquipment(location, responseText);

          // Slimeling binge requests come here, too
          if (location.contains("action=slime")) {
            UseItemRequest.parseBinge(location, responseText);
          }
          // Certain requests, like inserting cards into
          // an El Vibrato helmet, have a usage message,
          // not an equipment page. Check for that, too.
          else {
            AdventureResult item = UseItemRequest.getLastItemUsed();
            UseItemRequest.parseConsumption(responseText, false);
            SpadingManager.processConsumeItem(item, responseText);
          }
        }

        // If there is a consumption message, parse it
        else if (location.contains("action=message")) {
          AdventureResult item = UseItemRequest.getLastItemUsed();
          UseItemRequest.parseConsumption(responseText, false);
          AWOLQuartermasterRequest.parseResponse(responseText);
          BURTRequest.parseResponse(responseText);
          SpadingManager.processConsumeItem(item, responseText);
        }

        // If there is a bricko message, parse it
        else if (location.contains("action=breakbricko")) {
          UseItemRequest.parseBricko(responseText);
        }

        // If there is a binge message, parse it
        else if (location.contains("action=ghost")
            || location.contains("action=hobo")
            || location.contains("action=slime")
            || location.contains("action=candy")) {
          UseItemRequest.parseBinge(location, responseText);
        }

        // Robortender consumption
        else if (location.contains("action=robooze")) {
          UseItemRequest.parseRobortenderBinge(location, responseText);
        }

        // If there is an absorb message, parse it
        else if (location.contains("absorb=")) {
          UseItemRequest.parseAbsorb(location, responseText);
        }

        // Closet transfers can come via inventory.php
        else if (location.contains("action=closetpush") || location.contains("action=closetpull")) {
          ClosetRequest.parseTransfer(location, responseText);
        }

        // Emptying storage can come via inventory.php
        else if (location.contains("action=pullall")) {
          StorageRequest.parseTransfer(location, responseText);
        }
      }
      case "inv_equip.php" -> {
        if (location.contains("ajax=1")) {
          // If we are changing equipment via a chat command,
          // try to deduce what changed.
          EquipmentRequest.parseEquipmentChange(location, responseText);
        }
      }
      case "inv_eat.php", "inv_booze.php", "inv_spleen.php", "inv_use.php", "inv_familiar.php" -> {
        if (location.contains("whichitem")) {
          AdventureResult item = UseItemRequest.getLastItemUsed();
          UseItemRequest.parseConsumption(responseText, false);
          SpadingManager.processConsumeItem(item, responseText);
        }
      }
      case "knoll_mushrooms.php" -> {
        MushroomRequest.parseResponse(location, responseText);
      }
      case "leaflet.php" -> {
        LeafletRequest.parseResponse(location, responseText);
      }
      case "mallstore.php" -> {
        MallPurchaseRequest.parseResponse(location, responseText);
      }
      case "managecollection.php" -> {
        DisplayCaseRequest.parseDisplayTransfer(location, responseText);
      }
      case "managecollectionshelves.php" -> {
        DisplayCaseRequest.parseDisplayArrangement(location, responseText);
      }
      case "managestore.php" -> {
        AutoMallRequest.parseTransfer(location, responseText);
      }
      case "mining.php" -> {
        MineDecorator.parseResponse(location, responseText);
      }
      case "monkeycastle.php" -> {
        if (location.contains("who=2") || location.contains("action=buyitem")) {
          BigBrotherRequest.parseResponse(location, responseText);
        } else if (location.contains("action=grandpastory")) {
          GrandpaRequest.parseResponse(location, responseText);
        } else if (location.contains("who=4")) {
          MomRequest.parseResponse(location, responseText);
        }
      }
      case "mrstore.php" -> {
        MrStoreRequest.parseResponse(location, responseText);
      }
      case "multiuse.php" -> {
        if (location.contains("useitem")) {
          AdventureResult item = UseItemRequest.getLastItemUsed();
          UseItemRequest.parseConsumption(responseText, false);
          SpadingManager.processConsumeItem(item, responseText);
        }
      }
      case "pandamonium.php" -> {
        PandamoniumRequest.parseResponse(location, responseText);
      }
      case "peevpee.php" -> {
        PeeVPeeRequest.parseResponse(location, responseText);
      }
      case "place.php" -> {
        PlaceRequest.parseResponse(location, responseText);
      }
      case "qterrarium.php" -> {
        QuantumTerrariumRequest.parseResponse(location, responseText);
      }
      case "questlog.php" -> {
        MonsterManuelRequest.parseResponse(location, responseText);
        QuestLogRequest.registerQuests(true, location, responseText);
      }
      case "raffle.php" -> {
        RaffleRequest.parseResponse(location, responseText);
      }
      case "runskillz.php", "skillz.php" -> {
        UseSkillRequest.parseResponse(location, responseText);
      }
      case "sea_merkin.php" -> {
        SeaMerkinRequest.parseResponse(location, responseText);
      }
      case "sea_skatepark.php" -> {
        SkateParkRequest.parseResponse(location, responseText);
      }
      case "sellstuff.php" -> {
        AutoSellRequest.parseCompactAutoSell(location);
      }
      case "sellstuff_ugly.php" -> {
        AutoSellRequest.parseDetailedAutoSell(location, responseText);
      }
      case "sendmessage.php" -> {
        SendMailRequest.parseTransfer(location, responseText);
      }
      case "shop.php" -> {
        NPCPurchaseRequest.parseShopResponse(location, responseText);
      }
      case "showclan.php" -> {
        ShowClanRequest.parseResponse(location, responseText);
      }
      case "skills.php" -> {
        if (location.contains("useitem") || location.contains("action=useditem")) {
          AdventureResult item = UseItemRequest.getLastItemUsed();
          UseItemRequest.parseConsumption(responseText, false);
          SpadingManager.processConsumeItem(item, responseText);
        }
      }
      case "spaaace.php" -> {
        SpaaaceRequest.parseResponse(location, responseText);
      }
      case "storage.php" -> {
        StorageRequest.parseTransfer(location, responseText);
      }
      case "suburbandis.php" -> {
        SuburbanDisRequest.parseResponse(location, responseText);
      }
      case "sushi.php" -> {
        SushiRequest.parseConsumption(location, responseText, true);
      }
      case "tavern.php" -> {
        TavernRequest.parseResponse(location, responseText);
      }
      case "tiles.php" -> {
        if (responseText.contains("charpane.php")) {
          // Since a charpane refresh was requested, this might have taken a turn
          AdventureSpentDatabase.setNoncombatEncountered(true);
        }
        DvorakManager.parseResponse(location, responseText);
      }
      case "topmenu.php" -> {
        if (KoLCharacter.getLimitMode() == LimitMode.BATMAN) {
          BatManager.parseTopMenu(responseText);
        }
      }
      case "town_altar.php" -> {
        AltarOfLiteracyRequest.parseResponse(location, responseText);
      }
      case "town_fleamarket.php" -> {
        FleaMarketRequest.parseResponse(location, responseText);
      }
      case "town_right.php" -> {
        GourdRequest.parseResponse(location, responseText);
      }
      case "town_sendgift.php" -> {
        SendGiftRequest.parseTransfer(location, responseText);
      }
      case "town_sellflea.php" -> {
        FleaMarketSellRequest.parseResponse(location, responseText);
      }
      case "traveler.php" -> {
        TravelingTraderRequest.parseResponse(location, responseText);
      }
      case "trophy.php" -> {
        TrophyHutRequest.parseResponse(location, responseText);
      }
      case "tutorial.php" -> {
        TutorialRequest.parseResponse(location, responseText);
      }
      case "typeii.php" -> {
        TrendyRequest.parseResponse(location, responseText);
      }
      case "volcanoisland.php" -> {
        PhineasRequest.parseResponse(location, responseText);
        VolcanoIslandRequest.parseResponse(location, responseText);
      }
      case "volcanomaze.php" -> {
        VolcanoMazeRequest.parseResponse(location, responseText);
      }
      case "wand.php" -> {
        ZapRequest.parseResponse(location, responseText);
      }
      case "witchess.php" -> {
        WitchessRequest.parseResponse(location, responseText);
      }
    }

    // You can learn a skill on many pages.
    ResponseTextParser.learnSkill(location, responseText);

    // Currently, required recipes can only be learned via using an
    // item, but that's probably not guaranteed to be true forever.
    // Update: you can now learn them from the April Shower
    ResponseTextParser.learnRecipe(location, responseText);

    // New items may show up on many pages.
    ResponseTextParser.findNewItems(responseText);
  }

  private static final Pattern DIV_LINK_PATTERN =
      Pattern.compile("<div id=([^ ]+)[^>]*><a .*?</a></div>", Pattern.DOTALL);

  public static String parseDivLabel(final String label, final String responseText) {
    Matcher matcher = ResponseTextParser.DIV_LINK_PATTERN.matcher(responseText);
    while (matcher.find()) {
      if (matcher.group(1).equals(label)) {
        return parseDivLabel(matcher.group(0));
      }
    }
    return "";
  }

  // <img src="http://images.kingdomofloathing.com/otherimages/zonefont/percent.gif" height="10"
  // border="0" style="margin-right: 4px"/>
  private static final Pattern DIV_CHAR_PATTERN =
      Pattern.compile("otherimages/zonefont/(.*?)\\.gif");

  public static String parseDivLabel(final String divText) {
    StringBuilder string = new StringBuilder();
    Matcher matcher = ResponseTextParser.DIV_CHAR_PATTERN.matcher(divText);
    while (matcher.find()) {
      String c = matcher.group(1);
      if (c.length() == 1) {
        string.append(c.charAt(0));
      } else if (c.equals("lparen")) {
        string.append("(");
      } else if (c.equals("percent")) {
        string.append("%");
      } else if (c.equals("rparen")) {
        string.append(")");
      }
    }
    return string.toString();
  }

  private static final Pattern[] RECIPE_PATTERNS = {
    Pattern.compile("You learn to .*?craft.*? a new item:.*?<b>(.*?)</b>"),
    Pattern.compile("You (?:have|just) .*?discovered.*? a new recipe.*?<b>(.*?)</b>")
  };

  public static void learnRecipe(String location, String responseText) {
    if (!ResponseTextParser.hasResult(location)) {
      return;
    }

    String itemName = null;

    for (Pattern recipePattern : RECIPE_PATTERNS) {
      Matcher matcher = recipePattern.matcher(responseText);
      if (matcher.find()) {
        itemName = matcher.group(1);
        break;
      }
    }

    if (itemName == null) {
      return;
    }

    learnRecipe(itemName);
  }

  public static void learnRecipe(String itemName) {
    int itemId = ItemDatabase.getItemId(itemName.trim(), 1, false);
    if (itemId <= 0) {
      return;
    }

    String property = "unknownRecipe" + itemId;
    if (Preferences.getBoolean(property)) {
      // Get the pretty "display name" with HTML entities decoded
      itemName = ItemDatabase.getItemName(itemId);
      String message = "Learned recipe: " + itemName + " (" + itemId + ")";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);

      Preferences.setBoolean(property, false);
      ConcoctionDatabase.setRefreshNeeded(false);
    }
  }

  public static final Pattern ITEM_DESC_PATTERN =
      Pattern.compile("on[cC]lick='(?:javascript:)?descitem\\(([\\d]*)\\)'");

  public static void findNewItems(final String responseText) {
    Matcher matcher = ResponseTextParser.ITEM_DESC_PATTERN.matcher(responseText);
    while (matcher.find()) {
      ItemDatabase.lookupItemIdFromDescription(matcher.group(1));
    }
  }

  public static int learnSkill(final String location, final String responseText) {
    if (!ResponseTextParser.hasResult(location)) {
      return 0;
    }

    // Don't parse skill acquisition via item use here, since
    // UseItemRequest will detect it.
    // Don't parse steel margarita/lasagna here, either.

    if (location.startsWith("inv_use.php")
        || location.startsWith("inv_eat.php")
        || location.startsWith("inv_booze.php")
        || location.startsWith("inv_spleen.php")
        || location.startsWith("showplayer.php")) {
      return 0;
    }

    // Unfortunately, if you learn a new skill from Frank
    // the Regnaissance Gnome at the Gnomish Gnomads
    // Camp, it doesn't tell you the name of the skill.
    // It simply says: "You leargn a new skill. Whee!"

    if (responseText.contains("You leargn a new skill.")) {
      Matcher matcher = ResponseTextParser.NEWSKILL2_PATTERN.matcher(location);
      if (matcher.find()) {
        int skillId = StringUtilities.parseInt(matcher.group(1));
        String skillName = SkillDatabase.getSkillName(skillId);
        ResponseTextParser.learnSkill(skillName);
        return 0;
      }
    }

    return ResponseTextParser.learnSkillFromResponse(responseText);
  }

  public static int learnSkillFromResponse(final String responseText) {
    int skillFound = 0;

    Matcher matcher = ResponseTextParser.NEWSKILL1_PATTERN.matcher(responseText);
    while (matcher.find()) {
      skillFound = ResponseTextParser.learnSkill(matcher.group(1));
    }

    if (skillFound != 0) {
      return skillFound;
    }

    matcher = ResponseTextParser.NEWSKILL3_PATTERN.matcher(responseText);
    while (matcher.find()) {
      skillFound = ResponseTextParser.learnSkill(Integer.parseInt(matcher.group(1)));
    }

    return skillFound;
  }

  public static final int learnSkill(final String skillName) {
    return learnSkill(SkillDatabase.getSkillId(skillName));
  }

  public static final int learnSkill(final int skillId) {
    // The following skills are found in battle and result in
    // losing an item from inventory.
    var levelPref = "skillLevel" + skillId;

    switch (skillId) {
      case SkillPool.SNARL_OF_THE_TIMBERWOLF -> {
        if (InventoryManager.hasItem(ItemPool.TATTERED_WOLF_STANDARD)) {
          ResultProcessor.processItem(ItemPool.TATTERED_WOLF_STANDARD, -1);
        }
      }
      case SkillPool.SPECTRAL_SNAPPER -> {
        if (InventoryManager.hasItem(ItemPool.TATTERED_SNAKE_STANDARD)) {
          ResultProcessor.processItem(ItemPool.TATTERED_SNAKE_STANDARD, -1);
        }
      }
      case SkillPool.SCARYSAUCE, SkillPool.FEARFUL_FETTUCINI -> {
        if (InventoryManager.hasItem(ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY)) {
          ResultProcessor.processItem(ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY, -1);
        }
      }
      case SkillPool.TANGO_OF_TERROR, SkillPool.DIRGE_OF_DREADFULNESS -> {
        if (InventoryManager.hasItem(ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC)) {
          ResultProcessor.processItem(ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC, -1);
        }
      }
      case SkillPool.BELCH_THE_RAINBOW, SkillPool.CHITINOUS_SOUL -> Preferences.increment(
          levelPref, 1, 11, false);
      case SkillPool.TOGGLE_OPTIMALITY,
          SkillPool.PIRATE_BELLOW,
          SkillPool.HOLIDAY_FUN,
          SkillPool.SUMMON_CARROT,
          SkillPool.BEAR_ESSENCE,
          SkillPool.CALCULATE_THE_UNIVERSE,
          SkillPool.EXPERIENCE_SAFARI -> Preferences.increment(levelPref);
      case SkillPool.SLIMY_SHOULDERS,
          SkillPool.SLIMY_SINEWS,
          SkillPool.SLIMY_SYNAPSES -> Preferences.increment(levelPref, 1, 10, false);
      case SkillPool.IMPLODE_UNIVERSE -> Preferences.increment(levelPref, 1, 13, false);
    }

    if (KoLCharacter.inNuclearAutumn()) {
      int cost =
          switch (skillId) {
            case SkillPool.BOILING_TEAR_DUCTS,
                SkillPool.PROJECTILE_SALIVARY_GLANDS,
                SkillPool.TRANSLUCENT_SKIN,
                SkillPool.SKUNK_GLANDS,
                SkillPool.THROAT_REFRIDGERANT,
                SkillPool.INTERNAL_SODA_MACHINE -> 30;
            case SkillPool.STEROID_BLADDER,
                SkillPool.MAGIC_SWEAT,
                SkillPool.FLAPPY_EARS,
                SkillPool.SELF_COMBING_HAIR,
                SkillPool.INTRACRANIAL_EYE,
                SkillPool.MIND_BULLETS,
                SkillPool.EXTRA_KIDNEY,
                SkillPool.EXTRA_GALL_BLADDER -> 60;
            case SkillPool.EXTRA_MUSCLES,
                SkillPool.ADIPOSE_POLYMERS,
                SkillPool.METALLIC_SKIN,
                SkillPool.HYPNO_EYES,
                SkillPool.EXTRA_BRAIN,
                SkillPool.MAGNETIC_EARS,
                SkillPool.EXTREMELY_PUNCHABLE_FACE,
                SkillPool.FIREFLY_ABDOMEN,
                SkillPool.BONE_SPRINGS,
                SkillPool.SQUID_GLANDS -> 90;
            case SkillPool.SUCKER_FINGERS, SkillPool.BACKWARDS_KNEES -> 120;
            default -> 0;
          };

      ResultProcessor.processResult(ItemPool.get(ItemPool.RAD, -cost));
    }

    UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);
    if (skill == null) {
      SkillDatabase.registerSkill(skillId);
      skill = UseSkillRequest.getUnmodifiedInstance(skillId);
    }

    String message = "You learned a new skill: " + skill.getSkillName();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
    KoLCharacter.addAvailableSkill(skill);
    KoLCharacter.updateStatus();
    LockableListFactory.sort(KoLConstants.usableSkills);
    DiscoCombatHelper.learnSkill(skill.getSkillName());
    GreyYouManager.learnSkill(skill.getSkillId());
    ConcoctionDatabase.setRefreshNeeded(true);
    if (SkillDatabase.isBookshelfSkill(skillId)) {
      KoLCharacter.setBookshelf(true);
    }

    if (skillId == SkillPool.POWER_PLUS) {
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.resetCurrentPP();
    }

    return skillId;
  }

  public static final String[][] COMBAT_MOVE_DATA = {
    {
      "gladiatorBallMovesKnown", "Ball Bust", "Ball Sweat", "Ball Sack",
    },
    {
      "gladiatorBladeMovesKnown", "Blade Sling", "Blade Roller", "Blade Runner",
    },
    {
      "gladiatorNetMovesKnown", "Net Gain", "Net Loss", "Net Neutrality",
    },
  };

  public static final void learnCombatMove(final String skillName) {
    for (int type = 0; type < COMBAT_MOVE_DATA.length; ++type) {
      String[] moves = COMBAT_MOVE_DATA[type];
      for (int index = 1; index < moves.length; ++index) {
        if (skillName.equals(moves[index])) {
          String setting = moves[0];
          Preferences.setInteger(setting, index);
          String message = "You learned a new special combat move: " + skillName;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          // KoLCharacter.addCombatSkill( skillName );
          // PreferenceListenerRegistry.firePreferenceChanged( "(skill)" );
          return;
        }
      }
    }
  }
}
