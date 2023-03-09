package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.*;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.OrcChasmRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UpdateSuppressedRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class QuestManager {
  private static final Pattern ORE_PATTERN =
      Pattern.compile("(asbestos|linoleum|chrome) ore[\\. ]");
  private static final Pattern BATHOLE_PATTERN = Pattern.compile("bathole_(\\d)\\.gif");
  private static final Pattern DRAWER_PATTERN =
      Pattern.compile("search through <b>(\\d+)</b> drawers");
  private static final Pattern LIGHTER_PATTERN =
      Pattern.compile("group of (\\d+) nearby protesters do the same");
  private static final Pattern TACO_FISH_PATTERN = Pattern.compile("gain (\\d+) taco fish meat");
  private static final Pattern LOWER_CHAMBER_PATTERN =
      Pattern.compile("action=pyramid_state(\\d+)");
  private static final Pattern GORE_PATTERN = Pattern.compile("(\\d+) pounds of (?:the gore|gore)");
  private static final Pattern TOURIST_PATTERN =
      Pattern.compile("and the (\\d+) tourists in front");
  private static final Pattern WALFORD_PATTERN =
      Pattern.compile("\\(Walford's bucket filled by (?:an additional |)(\\d+)%\\)");
  private static final Pattern SNOWMAN_PATTERN = Pattern.compile("otherimages/combatsnowman/");
  private static final Pattern PARANORMAL_PATTERN =
      Pattern.compile("&quot;Paranormal disturbance reported (.*?).&quot;");
  private static final Pattern DJ_MEAT_PATTERN = Pattern.compile("collect (.*?) Meat for the DJ");
  private static final Pattern TRASH_PATTERN = Pattern.compile("you clean up (\\d+) ");

  private static Set<String> friarElbowNCs =
      Set.of(
          "Deep Imp Act",
          "Imp Art, Some Wisdom",
          "A Secret, But Not the Secret You're Looking For",
          "Butter Knife?  I'll Take the Knife");
  private static Set<String> friarNeckNCs =
      Set.of(
          "How Do We Do It? Quaint and Curious Volume!",
          "Strike One!",
          "Olive My Love To You, Oh.",
          "Dodecahedrariffic!");
  private static Set<String> friarHeartNCs =
      Set.of(
          "Moon Over the Dark Heart",
          "Running the Lode",
          "I, Martin",
          "Imp Be Nimble, Imp Be Quick");

  private QuestManager() {}

  public static final void handleQuestChange(GenericRequest request) {
    // Certain location-specific quest changes are noticed by
    // simply adventuring in a location. Get the location.

    String location = request.getURLString();
    String field = request.getFormField("snarfblat");
    int locationId = StringUtilities.isNumeric(field) ? StringUtilities.parseInt(field) : 0;

    // If we redirected to a choice or fight, there is no response
    // text here. Look for the above-mentioned quest changes which
    // don't depend on a responseText.

    String redirectLocation = request.redirectLocation;
    if (redirectLocation != null) {
      if (location.startsWith("adventure")) {
        switch (locationId) {
          case AdventurePool.PALINDOME -> QuestDatabase.setQuestIfBetter(
              Quest.PALINDOME, QuestDatabase.STARTED);
          case AdventurePool.EL_VIBRATO_ISLAND -> handleElVibratoChange(location, "");
        }
      }
      return;
    }

    // If there was no redirect but we didn't get a response for
    // some reason, that is puzzling, but nothing to do.
    String responseText = request.responseText;
    if (responseText == null || responseText.equals("")) {
      // Similarly, don't process an empty response
      return;
    }

    if (location.startsWith("adventure")) {
      switch (locationId) {
        case AdventurePool.ROAD_TO_WHITE_CITADEL:
          handleWhiteCitadelChange(responseText);
          break;
        case AdventurePool.WHITEYS_GROVE:
          handleWhiteysGroveChange(responseText);
          break;
        case AdventurePool.TOWER_RUINS:
          handleTowerRuinsChange(responseText);
          break;
        case AdventurePool.BARROOM_BRAWL:
          handleBarroomBrawlChange(responseText);
          break;
        case AdventurePool.KNOB_SHAFT:
          handleKnobShaftChange(responseText);
          break;
        case AdventurePool.ARRRBORETUM:
          handleArrrboretumChange(responseText);
          break;
        case AdventurePool.EXTREME_SLOPE:
          handleExtremityChange(responseText);
          break;
        case AdventurePool.ICY_PEAK:
          handleIcyPeakChange(responseText);
          break;
        case AdventurePool.AIRSHIP:
        case AdventurePool.CASTLE_BASEMENT:
        case AdventurePool.CASTLE_GROUND:
        case AdventurePool.CASTLE_TOP:
          handleBeanstalkChange(location, responseText);
          break;
        case AdventurePool.ZEPPELIN_PROTESTORS:
          handleZeppelinMobChange(responseText);
          break;
        case AdventurePool.RED_ZEPPELIN:
          handleZeppelinChange(responseText);
          break;
        case AdventurePool.PALINDOME:
          {
            // Non-fight, non-choice. Could be a regular
            // non-combat, or it could be an error message.
            //
            // You get the following if you haven't made a
            // Talisman o' Namsilat.
            if (!responseText.contains("That place isn't accessible to you right now.")
                && !responseText.contains("You find yourself unable to get near the Palindome.")) {
              QuestDatabase.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED);
            }
            break;
          }
        case AdventurePool.EL_VIBRATO_ISLAND:
          handleElVibratoChange(location, responseText);
          break;
        case AdventurePool.ABOO_PEAK:
          handleABooPeakChange(responseText);
          break;
        case AdventurePool.OIL_PEAK:
          handleOilPeakChange(responseText);
          break;
        case AdventurePool.POOP_DECK:
          handlePoopDeckChange(responseText);
          break;
        case AdventurePool.HAUNTED_BILLIARDS_ROOM:
          handleBilliardsRoomChange(responseText);
          break;
        case AdventurePool.HAUNTED_BALLROOM:
          handleManorSecondFloorChange(location, responseText);
          break;
        case AdventurePool.UPPER_CHAMBER:
        case AdventurePool.MIDDLE_CHAMBER:
          handlePyramidChange(location, responseText);
          break;
        case AdventurePool.SLOPPY_SECONDS_DINER:
        case AdventurePool.FUN_GUY_MANSION:
        case AdventurePool.YACHT:
        case AdventurePool.DR_WEIRDEAUX:
        case AdventurePool.SECRET_GOVERNMENT_LAB:
        case AdventurePool.DEEP_DARK_JUNGLE:
        case AdventurePool.BARF_MOUNTAIN:
        case AdventurePool.GARBAGE_BARGES:
        case AdventurePool.TOXIC_TEACUPS:
        case AdventurePool.LIQUID_WASTE_SLUICE:
        case AdventurePool.SMOOCH_ARMY_HQ:
        case AdventurePool.VELVET_GOLD_MINE:
        case AdventurePool.LAVACO_LAMP_FACTORY:
        case AdventurePool.BUBBLIN_CALDERA:
        case AdventurePool.ICE_HOTEL:
        case AdventurePool.VYKEA:
        case AdventurePool.ICE_HOLE:
          handleAirportChange(location, responseText);
          break;
        case AdventurePool.MERKIN_OUTPOST:
        case AdventurePool.CALIGINOUS_ABYSS:
          handleSeaChange(location, responseText);
          break;
        case AdventurePool.GINGERBREAD_CIVIC_CENTER:
        case AdventurePool.GINGERBREAD_TRAIN_STATION:
        case AdventurePool.GINGERBREAD_INDUSTRIAL_ZONE:
        case AdventurePool.GINGERBREAD_RETAIL_DISTRICT:
        case AdventurePool.GINGERBREAD_SEWERS:
          handleGingerbreadCityChange(location, responseText);
          break;
        case AdventurePool.SPACEGATE:
          handleSpacegateChange(location, responseText);
          break;
        case AdventurePool.THE_POND:
        case AdventurePool.THE_BACK_40:
        case AdventurePool.THE_OTHER_BACK_40:
        case AdventurePool.THE_GRANARY:
        case AdventurePool.THE_BOG:
        case AdventurePool.THE_FAMILY_PLOT:
        case AdventurePool.THE_SHADY_THICKET:
          handleFarmChange(location, responseText);
          break;
        case AdventurePool.DARK_ELBOW_OF_THE_WOODS:
        case AdventurePool.DARK_HEART_OF_THE_WOODS:
        case AdventurePool.DARK_NECK_OF_THE_WOODS:
          handleFriarsCopseChange(locationId, responseText);
          break;
        default:
          if (KoLCharacter.getInebriety() > 25) {
            handleSneakyPeteChange(responseText);
          }
      }
    } else if (location.startsWith("choice.php") && location.contains("forceoption=0")) {
      // This can have no active choice options and therefore
      // won't be interpreted by ChoiceManager
      parseSpacegateTerminal(responseText, false);
    } else if (location.startsWith("cobbsknob.php")) {
      if (location.contains("action=cell37")) {
        handleCell37(responseText);
      }
    } else if (location.startsWith("council")) {
      handleCouncilChange(responseText);
    } else if (location.startsWith("da")) {
      handleDungeonsChange(responseText);
    } else if (location.startsWith("fernruin")) {
      QuestDatabase.setQuestIfBetter(Quest.EGO, "step3");
    } else if (location.startsWith("friars")) {
      handleFriarsChange(responseText);
    } else if (location.contains("whichplace=highlands")) {
      handleHighlandsChange(location, responseText);
    } else if (location.startsWith("inv_use")) {
      if (location.contains("whichitem=" + ItemPool.AWOL_COMMENDATION)) {
        AWOLQuartermasterRequest.parseResponse(responseText);
      } else if (location.contains("whichitem=" + ItemPool.BURT)) {
        BURTRequest.parseResponse(responseText);
      }
    } else if (location.startsWith("main")) {
      if (Preferences.getInteger("lastIslandUnlock") != KoLCharacter.getAscensions()
          && responseText.contains("island.php")) {
        Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
      }
    } else if (location.startsWith("manor")) {
      handleManorFirstFloorChange(location, responseText);
    } else if (location.startsWith("monkeycastle")) {
      handleSeaChange(location, responseText);
    } else if (location.startsWith("pandamonium")) {
      // Quest starts the very instant you click on pandamonium.php
      QuestDatabase.setQuestIfBetter(Quest.AZAZEL, QuestDatabase.STARTED);
    } else if (location.startsWith("place.php")) {
      String place = GenericRequest.getPlace(location);
      if (place != null) {
        String action = GenericRequest.getAction(location);
        if (place.startsWith("airport")) {
          handleAirportChange(location, responseText);
        }
        switch (place) {
          case "bathole" -> handleBatholeChange(responseText);
          case "beanstalk" -> {
            if (responseText.contains("otherimages/stalktop/beanstalk.gif")) {
              QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step1");
            }
          }
          case "canadia" -> handleCanadiaChange(location, responseText);
          case "desertbeach" -> {
            if (action != null && action.equals("db_pyramid1")) {
              handlePyramidChange(location, responseText);
            } else {
              handleBeachChange(responseText);
            }
          }
          case "exploathing_beach" -> {
            if (action != null && action.equals("expl_pyramidpre")) {
              handlePyramidChange(location, responseText);
            } else {
              handleBeachChange(responseText);
            }
          }
          case "exploathing" -> {
            if (action != null && action.equals("expl_council")) {
              handleCouncilChange(responseText);
            }
          }
          case "gingerbreadcity" -> handleGingerbreadCityChange(location, responseText);
          case "hiddencity" -> handleHiddenCityChange(location, responseText);
          case "manor1" -> handleManorFirstFloorChange(location, responseText);
          case "manor2" -> handleManorSecondFloorChange(location, responseText);
          case "manor3" -> {
            if (responseText.contains("Spookyraven Manor Third Floor")) {
              // If here at all, Necklace and Dance quests are complete and second floor open
              QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
              QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED);
              // Legacy code support
              Preferences.setInteger("lastSecondFloorUnlock", KoLCharacter.getAscensions());
            }
          }
          case "manor4" -> {
            if (responseText.contains("Spookyraven Manor Cellar")) {
              // If here at all, Necklace and Dance quests are complete and second floor and
              // basement
              // open
              QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
              QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED);
            }
            if (responseText.contains("sr_brickhole.gif")) {
              QuestDatabase.setQuestIfBetter(Quest.MANOR, "step3");
            } else if (!responseText.contains(
                "You shouldn't be down here yet.  I mean here.  Wherever here is.")) {
              // sr_masonry.gif
              QuestDatabase.setQuestIfBetter(Quest.MANOR, "step1");
            }
            // Legacy code support
            if (!responseText.contains(
                "You shouldn't be down here yet.  I mean here.  Wherever here is.")) {
              Preferences.setInteger("lastSecondFloorUnlock", KoLCharacter.getAscensions());
            }
            if (responseText.contains("Cold as ice and twice as smooth")) {
              QuestDatabase.setQuestProgress(Quest.MANOR, QuestDatabase.FINISHED);
              ResultProcessor.removeItem(ItemPool.ED_EYE);
              if (InventoryManager.getCount(ItemPool.ED_FATS_STAFF) == 0
                  && InventoryManager.getCount(ItemPool.ED_AMULET) == 0) {
                QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
              }
            }
          }
          case "marais" -> handleMaraisChange(responseText);
          case "mclargehuge" -> {
            if (action != null) {
              switch (action) {
                case "trappercabin" -> handleTrapperChange(responseText);
                case "cloudypeak" -> handleMcLargehugeChange(responseText);
              }
            }
          }
          case "monorail" -> handleMonorailChange(location, responseText);
          case "mountains" -> handleMountainsChange(location, action, responseText);
          case "orc_chasm" -> handleChasmChange(responseText);
          case "palindome" -> handlePalindomeChange(location, responseText);
          case "plains" -> handlePlainsChange(responseText);
          case "pyramid" -> handlePyramidChange(location, responseText);
          case "realm_fantasy" -> handleFantasyRealmChange(location, responseText);
          case "realm_pirate" -> handlePirateRealmChange(location, responseText);
          case "sea_oldman" -> handleSeaChange(location, responseText);
          case "spacegate" -> handleSpacegateChange(location, responseText);
          case "speakeasy" -> handleSpeakeasyChange(responseText);
            // don't catch town_wrong, town_right, or town_market
          case "town" -> handleTownChange(location, responseText);
          case "town_right" -> handleTownRightChange(location, responseText);
          case "town_wrong" -> handleTownWrongChange(location, responseText);
          case "town_market" -> handleTownMarketChange(location, responseText);
          case "woods" -> handleWoodsChange(location, responseText);
          case "zeppelin" -> {
            if (responseText.contains("zep_mob1.gif")) {
              QuestDatabase.setQuestIfBetter(Quest.RON, "step2");
            }
          }
        }
      }
    } else if (location.startsWith("questlog")) {
      QuestLogRequest.registerQuests(false, location, responseText);
    } else if (location.startsWith("sea_merkin")) {
      handleSeaChange(location, responseText);
    } else if (location.startsWith("seafloor")) {
      handleSeaChange(location, responseText);
    } else if (location.startsWith("tavern")) {
      TavernManager.handleTavernChange(responseText);
    } else if (location.startsWith("trickortreat")) {
      handleTrickOrTreatingChange(responseText);
    } else if (location.startsWith("wham")) {
      if (responseText.contains("Congratulations! You solved the case")) {
        Preferences.increment("_detectiveCasesCompleted");
      }
    }
    // Obsolete. Sigh.
    else if (location.startsWith("generate15")) {
      // You slide the last tile into place ...
      if (AdventureRequest.registerDemonName("Strange Cube", responseText)
          || responseText.contains("slide the last tile")) {
        ResultProcessor.processItem(ItemPool.STRANGE_CUBE, -1);
      }
    }
  }

  private static void handleArrrboretumChange(String responseText) {
    if (!responseText.contains("Plant a Tree, Plant a Tree!")
        && !responseText.contains("Stumped")
        && !responseText.contains("Timbarrrr!")) {
      Preferences.increment("_saplingsPlanted");
    }
  }

  private static void handleMcLargehugeChange(String responseText) {
    if (responseText.contains("you spy a crude stone staircase")
        || responseText.contains("notice a set of crude carved stairs")) {
      QuestDatabase.setQuestIfBetter(Quest.TRAPPER, "step3");
      Preferences.setInteger("currentExtremity", 0);
    }
  }

  private static void handleBarroomBrawlChange(String responseText) {
    if (responseText.contains("Jackin' the Jukebox")) {
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step1");
    }
  }

  private static void handleKnobShaftChange(String responseText) {
    if (responseText.contains("A Miner Variation")) {
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step3");
    }
  }

  private static void handleIcyPeakChange(String responseText) {
    if (responseText.contains("Mercury Rising")) {
      QuestDatabase.setQuestProgress(Quest.CLANCY, "step7");
    }
  }

  private static void handleTownChange(final String location, String responseText) {
    QuestManager.handleTimeTower(responseText.contains("town_tower"));
    QuestManager.handleEldritchFissure(responseText.contains("town_eincursion"));
    QuestManager.handleEldritchHorror(responseText.contains("town_eicfight2"));
  }

  private static void handleTownRightChange(final String location, String responseText) {
    if (!location.contains("action") && !KoLCharacter.inBadMoon()) {
      if (responseText.contains("Voting Booth") && !Preferences.getBoolean("voteAlways")) {
        Preferences.setBoolean("_voteToday", true);
      }
    }
    if (responseText.contains("Horsery")) {
      Preferences.setBoolean("horseryAvailable", true);
    }
    if (responseText.contains("Telegraph Office")
        && !Preferences.getBoolean("telegraphOfficeAvailable")) {
      Preferences.setBoolean("_telegraphOfficeToday", true);
    }
    if (responseText.contains("Madness Bakery")) {
      Preferences.setBoolean("madnessBakeryAvailable", true);
    }
  }

  private static void handleTownWrongChange(final String location, String responseText) {
    if (!location.contains("action") && !KoLCharacter.inBadMoon()) {
      Preferences.setBoolean("hasDetectiveSchool", responseText.contains("Precinct"));
      if (responseText.contains("The Neverending Party")
          && !Preferences.getBoolean("neverendingPartyAlways")) {
        Preferences.setBoolean("_neverendingPartyToday", true);
      }
      if (Preferences.getInteger("_neverendingPartyFreeTurns") < 10
          && responseText.contains("The Neverending Party (1)")) {
        Preferences.setInteger("_neverendingPartyFreeTurns", 10);
      }
      if (responseText.contains("Boxing Daycare") && !Preferences.getBoolean("daycareOpen")) {
        Preferences.setBoolean("_daycareToday", true);
      }
      if (responseText.contains("Tunnel of L.O.V.E.")
          && !Preferences.getBoolean("loveTunnelAvailable")) {
        Preferences.setBoolean("_loveTunnelToday", true);
      }
      if (responseText.contains("Speakeasy")) {
        handleSpeakeasyName(responseText);
        Preferences.setBoolean("ownsSpeakeasy", true);
      }
      if (responseText.contains("Overgrown Lot")) {
        Preferences.setBoolean("overgrownLotAvailable", true);
      }
    }
  }

  private static Pattern SPEAKEASY_NAME = Pattern.compile("whichplace=speakeasy.*?title=\"(.*?)\"");

  private static void handleSpeakeasyName(final String text) {
    var matcher = SPEAKEASY_NAME.matcher(text);
    if (!matcher.find()) return;
    Preferences.setString("speakeasyName", matcher.group(1));
  }

  private static void handleSpeakeasyChange(final String text) {
    if (!text.contains("olivers_nocost")) {
      Preferences.setInteger("_speakeasyFreeFights", 3);
    }
  }

  private static void handleTownMarketChange(final String location, String responseText) {
    if (!location.contains("action") && !KoLCharacter.inBadMoon()) {
      if (responseText.contains("The Skeleton Store")) {
        Preferences.setBoolean("skeletonStoreAvailable", true);
      }
    }
  }

  private static void handleMonorailChange(final String location, String responseText) {
    if (responseText.contains("FantasyRealm") && !Preferences.getBoolean("frAlways")) {
      Preferences.setBoolean("_frToday", true);
    }
    if (responseText.contains("PirateRealm") && !Preferences.getBoolean("prAlways")) {
      Preferences.setBoolean("_prToday", true);
    }
  }

  private static void handleMountainsChange(
      final String location, String action, String responseText) {
    if (action != null && action.equals("mts_melvin")) {
      // I saw this awesome T-shirt
      // haven't you fougnd my T-shirt yet?
      if (responseText.contains("I saw this awesome T-shirt")
          || responseText.contains("haven't you fougnd my T-shirt yet")) {
        QuestDatabase.setQuestProgress(Quest.SHIRT, QuestDatabase.STARTED);
        ResultProcessor.removeItem(ItemPool.LETTER_FOR_MELVIGN);
      }
      // I dogn't have a torso.
      else if (responseText.contains("I dogn't have a torso.")) {
        QuestDatabase.setQuestProgress(Quest.SHIRT, QuestDatabase.FINISHED);
        ResultProcessor.removeItem(ItemPool.PROFESSOR_WHAT_GARMENT);
      }
    }
    if (responseText.contains("Melvin's Comic Shop")) {
      QuestDatabase.setQuestIfBetter(Quest.SHIRT, QuestDatabase.STARTED);
    } else if (responseText.contains("The Thinknerd Warehouse")) {
      QuestDatabase.setQuestProgress(Quest.SHIRT, QuestDatabase.FINISHED);
    }
  }

  public static void handleTimeTower(final boolean available) {
    if (Preferences.getBoolean("timeTowerAvailable") == available) {
      return;
    }

    Preferences.setBoolean("timeTowerAvailable", available);

    // time-twitching toolbelt is a free pull if the time tower is
    // available. Place it in correct storage list.

    ModifierDatabase.getModifiers(ModifierType.ITEM, "time-twitching toolbelt");
    AdventureResult toolbelt = ItemPool.get(ItemPool.TIME_TWITCHING_TOOLBELT, 1);
    List<AdventureResult> source = available ? KoLConstants.storage : KoLConstants.freepulls;
    List<AdventureResult> dest = available ? KoLConstants.freepulls : KoLConstants.storage;
    int index = source.indexOf(toolbelt);
    if (index > -1) {
      AdventureResult item = source.get(index);
      source.remove(item);
      dest.add(item);
    }

    ConcoctionDatabase.setRefreshNeeded(false);
  }

  public static void handleEldritchFissure(final boolean available) {
    if (Preferences.getBoolean("eldritchFissureAvailable") == available) {
      return;
    }

    Preferences.setBoolean("eldritchFissureAvailable", available);
  }

  public static void handleEldritchHorror(final boolean available) {
    if (Preferences.getBoolean("eldritchHorrorAvailable") == available) {
      return;
    }

    Preferences.setBoolean("eldritchHorrorAvailable", available);
  }

  private static void handlePoopDeckChange(final String responseText) {
    if (responseText.contains("unlocks a padlock on a trap door")) {
      QuestDatabase.setQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED);
    }
  }

  private static void handleWhiteysGroveChange(final String responseText) {
    if (responseText.contains("It's A Sign!")) {
      QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step1");
    }
  }

  private static void handleTowerRuinsChange(final String responseText) {
    if (QuestDatabase.getQuest(Quest.EGO).equals(QuestDatabase.FINISHED)) {
      return;
    }

    if (responseText.contains("Take a Dusty Look!")) {
      QuestDatabase.setQuestIfBetter(Quest.EGO, "step6");
    } else if (responseText.contains("Into the Maw of Deepness")) {
      QuestDatabase.setQuestIfBetter(Quest.EGO, "step5");
    } else if (responseText.contains("Staring into Nothing")) {
      QuestDatabase.setQuestIfBetter(Quest.EGO, "step4");
    } else {
      QuestDatabase.setQuestIfBetter(Quest.EGO, "step3");
    }
  }

  private static void handleWhiteCitadelChange(final String responseText) {
    if (responseText.contains("I Guess They Were the Existential Blues Brothers")) {
      QuestDatabase.setQuestProgress(Quest.CITADEL, "step3");
    } else {
      QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step2");
    }
  }

  private static void handleGingerbreadCityChange(
      final String location, final String responseText) {
    if (!Preferences.getBoolean("gingerbreadCityAvailable")
        && !responseText.contains("That's not a real place.")) {
      Preferences.setBoolean("_gingerbreadCityToday", true);
    }
    if (responseText.contains("snarfblat=480")) {
      Preferences.setBoolean("gingerRetailUnlocked", true);
    }
    if (responseText.contains("snarfblat=481")) {
      Preferences.setBoolean("gingerSewersUnlocked", true);
    }
    if (responseText.contains("digitalclock.gif")) {
      Preferences.setBoolean("gingerAdvanceClockUnlocked", true);
    }
    if (responseText.contains("Infrastructure Failure")) {
      Preferences.increment("_gingerbreadCityTurns");
    }
  }

  private static void handleHiddenCityChange(final String location, final String responseText) {
    if (responseText.contains("snarfblat=341")) {
      if (Preferences.getInteger("hiddenApartmentProgress") == 0) {
        Preferences.setInteger("hiddenApartmentProgress", 1);
      }
    }
    if (responseText.contains("snarfblat=342")) {
      if (Preferences.getInteger("hiddenHospitalProgress") == 0) {
        Preferences.setInteger("hiddenHospitalProgress", 1);
      }
    }
    if (responseText.contains("snarfblat=343")) {
      if (Preferences.getInteger("hiddenOfficeProgress") == 0) {
        Preferences.setInteger("hiddenOfficeProgress", 1);
      }
    }
    if (responseText.contains("snarfblat=344")) {
      if (Preferences.getInteger("hiddenBowlingAlleyProgress") == 0) {
        Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
      }
    }
    if (responseText.contains("whichshop=hiddentavern")) {
      Preferences.setInteger("hiddenTavernUnlock", KoLCharacter.getAscensions());
    }
  }

  private static void handleSpacegateChange(final String location, final String responseText) {
    if (!Preferences.getBoolean("spacegateAlways")) {
      Preferences.setBoolean("_spacegateToday", true);
    }
    if (location.contains("action=sg_Terminal")) {
      parseSpacegateTerminal(responseText, false);
    }
    if (location.startsWith("adventure.php")) {
      parseSpacegateAdventure(responseText);
    }
  }

  private static void handleFarmChange(final String location, final String responseText) {
    if (responseText.contains("There are no more ducks here")) {
      KoLAdventure adventure = KoLAdventure.lastVisitedLocation;
      if (adventure != null) {
        StringBuilder buffer = new StringBuilder(Preferences.getString("duckAreasCleared"));
        if (buffer.length() != 0) {
          buffer.append(",");
        }
        buffer.append(adventure.getAdventureId());
        Preferences.setString("duckAreasCleared", buffer.toString());
      }
    }
  }

  private static void handleFriarsCopseChange(final int locationId, final String responseText) {
    String encounterName = AdventureRequest.parseEncounter(responseText);
    String location = AdventureDatabase.getAdventure(locationId).getAdventureName();
    switch (location) {
      case "The Dark Elbow of the Woods":
        if (friarElbowNCs.contains(encounterName)) {
          Preferences.setInteger("lastFriarsElbowNC", AdventureSpentDatabase.getTurns(location));
        }
        break;
      case "The Dark Heart of the Woods":
        if (friarHeartNCs.contains(encounterName)) {
          Preferences.setInteger("lastFriarsHeartNC", AdventureSpentDatabase.getTurns(location));
        }
        break;
      case "The Dark Neck of the Woods":
        if (friarNeckNCs.contains(encounterName)) {
          Preferences.setInteger("lastFriarsNeckNC", AdventureSpentDatabase.getTurns(location));
        }
        break;
    }
  }

  private static void handleFantasyRealmChange(final String location, final String responseText) {
    if (!Preferences.getBoolean("frAlways")) {
      Preferences.setBoolean("_frToday", true);
    }
    // If Welcome Center is present, then any area available is permanently unlocked
    if (responseText.contains("action=fr_initcenter")) {
      Preferences.setBoolean("frMountainsUnlocked", responseText.contains("snarfblat=503"));
      Preferences.setBoolean("frWoodUnlocked", responseText.contains("snarfblat=504"));
      Preferences.setBoolean("frSwampUnlocked", responseText.contains("snarfblat=505"));
      Preferences.setBoolean("frVillageUnlocked", responseText.contains("snarfblat=506"));
      Preferences.setBoolean("frCemetaryUnlocked", responseText.contains("snarfblat=507"));
    }
    // Otherwise we can see everything that is unlocked at present and correct
    else {
      StringBuilder unlocks = new StringBuilder();
      if (responseText.contains("snarfblat=502")) unlocks.append("The Bandit Crossroads,");
      if (responseText.contains("snarfblat=503")) unlocks.append("The Towering Mountains,");
      if (responseText.contains("snarfblat=504")) unlocks.append("The Mystic Wood,");
      if (responseText.contains("snarfblat=505")) unlocks.append("The Putrid Swamp,");
      if (responseText.contains("snarfblat=506")) unlocks.append("The Cursed Village,");
      if (responseText.contains("snarfblat=507")) unlocks.append("The Sprawling Cemetery,");
      if (responseText.contains("snarfblat=509")) unlocks.append("The Old Rubee Mine,");
      if (responseText.contains("snarfblat=510")) unlocks.append("The Foreboding Cave,");
      if (responseText.contains("snarfblat=511")) unlocks.append("The Faerie Cyrkle,");
      if (responseText.contains("snarfblat=512")) unlocks.append("The Druidic Campsite,");
      if (responseText.contains("snarfblat=513")) unlocks.append("Near the Witch's House,");
      if (responseText.contains("snarfblat=514")) unlocks.append("The Evil Cathedral,");
      if (responseText.contains("snarfblat=515")) unlocks.append("The Barrow Mounds,");
      if (responseText.contains("snarfblat=516"))
        unlocks.append("The Cursed Village Thieves' Guild,");
      if (responseText.contains("snarfblat=517")) unlocks.append("The Troll Fortress,");
      if (responseText.contains("snarfblat=518")) unlocks.append("The Labyrinthine Crypt,");
      if (responseText.contains("snarfblat=519")) unlocks.append("The Lair of the Phoenix,");
      if (responseText.contains("snarfblat=520")) unlocks.append("The Dragon's Moor,");
      if (responseText.contains("snarfblat=521")) unlocks.append("Duke Vampire's Chateau,");
      if (responseText.contains("snarfblat=522")) unlocks.append("The Master Thief's Chalet,");
      if (responseText.contains("snarfblat=523")) unlocks.append("The Spider Queen's Lair,");
      if (responseText.contains("snarfblat=524")) unlocks.append("The Archwizard's Tower,");
      if (responseText.contains("snarfblat=525")) unlocks.append("The Ley Nexus,");
      if (responseText.contains("snarfblat=526")) unlocks.append("The Ghoul King's Catacomb,");
      if (responseText.contains("snarfblat=527")) unlocks.append("The Ogre Chieftain's Keep,");
      Preferences.setString("_frAreasUnlocked", unlocks.toString());
    }
  }

  private static void addFantasyRealmKill(String monster) {
    if (monster == null) {
      return;
    }
    StringBuffer kills = new StringBuffer(Preferences.getString("_frMonstersKilled"));

    String monsterPattern = monster;
    if (monster.contains("barrow wraith")) {
      monsterPattern = "barrow wraith\\?";
    } else if (monster.contains("Phoenix")) {
      monsterPattern = "\\\"Phoenix\\\"";
    }
    Pattern FR_MONSTER_PATTERN = Pattern.compile(monsterPattern + ":(\\d+),");
    Matcher MonsterMatcher = FR_MONSTER_PATTERN.matcher(kills.toString());
    if (MonsterMatcher.find()) {
      String newMonster =
          monster + ":" + (StringUtilities.parseInt(MonsterMatcher.group(1)) + 1) + ",";
      StringUtilities.singleStringReplace(kills, MonsterMatcher.group(0), newMonster);
    } else {
      kills.append(monster);
      kills.append(":1,");
    }

    Preferences.setString("_frMonstersKilled", kills.toString());
  }

  private static void handlePirateRealmChange(final String location, final String responseText) {
    if (!Preferences.getBoolean("prAlways")) {
      Preferences.setBoolean("_prToday", true);
    }
  }

  private static void handleBilliardsRoomChange(final String responseText) {
    if (responseText.contains("That's Your Cue")) {
      QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, "step2");
    }
  }

  private static void handleManorFirstFloorChange(
      final String location, final String responseText) {
    if (location.contains("action=manor1_ladys")) {
      if (responseText.contains("ghostly copy of the necklace")) {
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
      }
    }
    // Derive quest status from available rooms
    if (responseText.contains("snarfblat=" + AdventurePool.HAUNTED_KITCHEN)) {
      QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED);
    }
    if (responseText.contains("whichplace=manor2")) {
      QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
      // Legacy code support
      Preferences.setInteger("lastSecondFloorUnlock", KoLCharacter.getAscensions());
    }
    if (responseText.contains("whichplace=manor4")) {
      QuestDatabase.setQuestIfBetter(Quest.MANOR, "step1");
    }
  }

  private static void handleManorSecondFloorChange(
      final String location, final String responseText) {
    if (!responseText.contains("Spookyraven Manor Second Floor")) {
      return;
    }
    int area = AdventureRequest.parseArea(location);
    if (location.contains("action=manor2_ladys")) {
      if (responseText.contains("just want to dance")) {
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step1");
      }
      if (responseText.contains("Meet me in the ballroom")) {
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step3");
        ResultProcessor.removeItem(ItemPool.POWDER_PUFF);
        ResultProcessor.removeItem(ItemPool.FINEST_GOWN);
        ResultProcessor.removeItem(ItemPool.DANCING_SHOES);
      }
    }
    if (area == AdventurePool.HAUNTED_BALLROOM) {
      if (responseText.contains("Having a Ball in the Ballroom")) {
        QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED);
        // You cannot visit the third floor unless you have visited the second floor
        new UpdateSuppressedRequest("place.php?whichplace=manor2").run();
      }
    }
    // Derive quest status from available rooms
    if (responseText.contains("snarfblat=" + AdventurePool.HAUNTED_BATHROOM)) {
      QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, "step1");
    }
    if (responseText.contains("snarfblat=" + AdventurePool.HAUNTED_BALLROOM)) {
      QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, "step3");
    }
    if (responseText.contains("whichplace=manor3")) {
      QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED);
    }
    // If here at all, Necklace quest is complete
    QuestDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
    // Legacy code support
    Preferences.setInteger("lastSecondFloorUnlock", KoLCharacter.getAscensions());
  }

  public static final void handlePyramidChange(final String location, final String responseText) {
    int area = AdventureRequest.parseArea(location);
    if (location.contains("action=db_pyramid1") || location.contains("action=expl_pyramidpre")) {
      // Unlock Pyramid
      if (responseText.contains("the model bursts into flames and is quickly consumed")) {
        QuestDatabase.setQuestProgress(Quest.PYRAMID, QuestDatabase.STARTED);
      }
    } else if (area == AdventurePool.UPPER_CHAMBER) {
      if (responseText.contains("Down Dooby-Doo Down Down")) {
        // Open Middle Chamber
        Preferences.setBoolean("middleChamberUnlock", true);
        QuestDatabase.setQuestProgress(Quest.PYRAMID, "step1");
      }
    } else if (area == AdventurePool.MIDDLE_CHAMBER) {
      if (responseText.contains("Further Down Dooby-Doo Down Down")) {
        // Open Lower Chamber
        Preferences.setBoolean("lowerChamberUnlock", true);
        QuestDatabase.setQuestProgress(Quest.PYRAMID, "step2");
      } else if (responseText.contains("Under Control")) {
        // Open Control Room
        Preferences.setBoolean("controlRoomUnlock", true);
        Preferences.setInteger("pyramidPosition", 1);
        QuestDatabase.setQuestProgress(Quest.PYRAMID, "step3");
      } else if (responseText.contains("Don't You Know Who I Am?")) {
        QuestDatabase.setQuestProgress(Quest.CLANCY, QuestDatabase.FINISHED);
      }
      // If here, must have unlocked middle chamber
      Preferences.setBoolean("middleChamberUnlock", true);
      QuestDatabase.setQuestIfBetter(Quest.PYRAMID, "step1");
    } else if (location.contains("whichplace=pyramid")
        && !responseText.contains("No, that isn't a place yet.")) {
      // Verify settings based on images displayed, in case above steps were missed
      QuestDatabase.setQuestIfBetter(Quest.PYRAMID, QuestDatabase.STARTED);
      if (responseText.contains("pyramid_middle.gif")) {
        Preferences.setBoolean("middleChamberUnlock", true);
        QuestDatabase.setQuestIfBetter(Quest.PYRAMID, "step1");
      }
      if (responseText.contains("pyramid_bottom")) {
        Preferences.setBoolean("lowerChamberUnlock", true);
        QuestDatabase.setQuestIfBetter(Quest.PYRAMID, "step2");
      }
      if (responseText.contains("pyramid_controlroom.gif")) {
        Preferences.setBoolean("controlRoomUnlock", true);
        QuestDatabase.setQuestIfBetter(Quest.PYRAMID, "step3");
      }
      Matcher LowerChamberMatcher = QuestManager.LOWER_CHAMBER_PATTERN.matcher(responseText);
      if (LowerChamberMatcher.find()) {
        Preferences.setInteger(
            "pyramidPosition", StringUtilities.parseInt(LowerChamberMatcher.group(1)));
      }
      if (responseText.contains("action=pyramid_state1a")) {
        Preferences.setBoolean("pyramidBombUsed", true);
      }
      // Lower chamber parsing
      if (location.contains("action=pyramid_state")) {
        if (responseText.contains("the rubble is gone")) {
          Preferences.setBoolean("pyramidBombUsed", true);
          ResultProcessor.processItem(ItemPool.ANCIENT_BOMB, -1);
        }
      }
    }
    return;
  }

  public static final void handleAirportChange(final String location, final String responseText) {
    int area = AdventureRequest.parseArea(location);
    // Check Cold settings
    if (!Preferences.getBoolean("coldAirportAlways")) {
      // Detect if Airport is open today
      if (area == AdventurePool.ICE_HOTEL
          || area == AdventurePool.VYKEA
          || area == AdventurePool.ICE_HOLE
          || location.contains("whichplace=airport_cold")) {
        if (!responseText.contains("You don't know where that is.")
            && !responseText.contains("That isn't a place you can go.")) {
          Preferences.setBoolean("_coldAirportToday", true);
        }
      } else if (location.contains("whichplace=airport")) {
        if (responseText.contains("whichplace=airport_cold")) {
          Preferences.setBoolean("_coldAirportToday", true);
        }
      }
    }

    // Check Hot settings
    if (!Preferences.getBoolean("hotAirportAlways")) {
      // Detect if Airport is open today
      if (area == AdventurePool.SMOOCH_ARMY_HQ
          || area == AdventurePool.VELVET_GOLD_MINE
          || area == AdventurePool.LAVACO_LAMP_FACTORY
          || area == AdventurePool.BUBBLIN_CALDERA
          || location.contains("whichplace=airport_hot")) {
        if (!responseText.contains("You don't know where that is.")
            && !responseText.contains("That isn't a place you can go.")) {
          Preferences.setBoolean("_hotAirportToday", true);
        }
      } else if (location.contains("whichplace=airport")) {
        if (responseText.contains("whichplace=airport_hot")) {
          Preferences.setBoolean("_hotAirportToday", true);
        }
      }
    }

    // Check Sleaze settings
    if (!Preferences.getBoolean("sleazeAirportAlways")) {
      // Detect if Airport is open today
      if (area == AdventurePool.FUN_GUY_MANSION
          || area == AdventurePool.SLOPPY_SECONDS_DINER
          || area == AdventurePool.YACHT
          || location.contains("whichplace=airport_sleaze")) {
        if (!responseText.contains("You don't know where that is.")
            && !responseText.contains("That isn't a place you can go.")) {
          Preferences.setBoolean("_sleazeAirportToday", true);
        }
      } else if (location.contains("whichplace=airport")) {
        if (responseText.contains("whichplace=airport_sleaze")) {
          Preferences.setBoolean("_sleazeAirportToday", true);
        }
      }
    }

    // Check Spooky settings
    if (!Preferences.getBoolean("spookyAirportAlways")) {
      // Detect if Airport is open today
      if (area == AdventurePool.DR_WEIRDEAUX
          || area == AdventurePool.SECRET_GOVERNMENT_LAB
          || area == AdventurePool.DEEP_DARK_JUNGLE
          || location.contains("whichplace=airport_spooky")) {
        if (!responseText.contains("You don't know where that is.")
            && !responseText.contains("That isn't a place you can go.")) {
          Preferences.setBoolean("_spookyAirportToday", true);
        }
      } else if (location.contains("whichplace=airport")) {
        if (responseText.contains("whichplace=airport_spooky")) {
          Preferences.setBoolean("_spookyAirportToday", true);
        }
      }
    }

    // Check Stench settings
    if (!Preferences.getBoolean("stenchAirportAlways")) {
      // Detect if Airport is open today
      if (area == AdventurePool.BARF_MOUNTAIN
          || area == AdventurePool.GARBAGE_BARGES
          || area == AdventurePool.TOXIC_TEACUPS
          || area == AdventurePool.LIQUID_WASTE_SLUICE
          || location.contains("whichplace=airport_stench")) {
        if (!responseText.contains("You don't know where that is.")
            && !responseText.contains("That isn't a place you can go.")) {
          Preferences.setBoolean("_stenchAirportToday", true);
        }
      } else if (location.contains("whichplace=airport")) {
        if (responseText.contains("whichplace=airport_stench")) {
          Preferences.setBoolean("_stenchAirportToday", true);
        }
      }
    }

    // Detect Bunker state
    if (location.contains("whichplace=airport_spooky_bunker")) {
      if (responseText.contains("action=si_shop1locked")) {
        Preferences.setBoolean("SHAWARMAInitiativeUnlocked", false);
      } else if (responseText.contains("whichshop=si_shop1")) {
        Preferences.setBoolean("SHAWARMAInitiativeUnlocked", true);
      }

      if (responseText.contains("action=si_shop2locked")) {
        Preferences.setBoolean("canteenUnlocked", false);
      } else if (responseText.contains("whichshop=si_shop2")) {
        Preferences.setBoolean("canteenUnlocked", true);
      }

      if (responseText.contains("action=si_shop3locked")) {
        Preferences.setBoolean("armoryUnlocked", false);
      } else if (responseText.contains("whichshop=si_shop3")) {
        Preferences.setBoolean("armoryUnlocked", true);
      }

      if (responseText.contains("insert the keycard and the door slides open")) {
        if (location.contains("action=si_shop1locked")) {
          Preferences.setBoolean("SHAWARMAInitiativeUnlocked", true);
          ResultProcessor.removeItem(ItemPool.SHAWARMA_KEYCARD);
        } else if (location.contains("action=si_shop2locked")) {
          Preferences.setBoolean("canteenUnlocked", true);
          ResultProcessor.removeItem(ItemPool.BOTTLE_OPENER_KEYCARD);
        } else if (location.contains("action=si_shop3locked")) {
          Preferences.setBoolean("armoryUnlocked", true);
          ResultProcessor.removeItem(ItemPool.ARMORY_KEYCARD);
        }
      }
    }
    return;
  }

  private static void handleWoodsChange(final String location, final String responseText) {
    if (responseText.contains("wcroad.gif")) {
      QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step1");
    }

    if (location.contains("action=woods_dakota")) {
      if (responseText.contains("need you to pick up a couple things for me")) {
        QuestDatabase.setQuestProgress(Quest.TEMPLE, QuestDatabase.STARTED);
      } else if (responseText.contains("make a note of the temple's location")) {
        KoLCharacter.setTempleUnlocked();
        ResultProcessor.removeItem(ItemPool.BENDY_STRAW);
        ResultProcessor.removeItem(ItemPool.PLANT_FOOD);
        ResultProcessor.removeItem(ItemPool.SEWING_KIT);
      }
    } else if (location.contains("action=woods_hippy")
        && responseText.contains("You've got this cool boat")) {
      QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.FINISHED);
    }

    // If we see the Hidden Temple, mark it as unlocked
    if (responseText.contains("temple.gif")) {
      KoLCharacter.setTempleUnlocked();
    }

    // If we see the Black Market, update Black Market quest
    if (responseText.contains("blackmarket.gif")) {
      QuestDatabase.setQuestIfBetter(Quest.BLACK, "step2");
      QuestDatabase.setQuestIfBetter(Quest.MACGUFFIN, "step1");
      Preferences.setInteger("blackForestProgress", 5);
    }

    // If we see the link to the empty Black Market, Wu Tang has been defeated
    if (responseText.contains("action=emptybm")) {
      Preferences.setInteger("lastWuTangDefeated", KoLCharacter.getAscensions());
    }
  }

  private static void handleBatholeChange(final String responseText) {
    Matcher m = BATHOLE_PATTERN.matcher(responseText);

    if (!m.find()) {
      return;
    }

    int image = StringUtilities.parseInt(m.group(1));
    String status = "";

    if (image == 1) {
      status = QuestDatabase.STARTED;
    } else if (image == 2) {
      status = "step1";
    } else if (image == 3) {
      status = "step2";
    } else if (image == 4) {
      status = "step3";
    } else if (image == 5) {
      status = "step4";
    }

    QuestDatabase.setQuestIfBetter(Quest.BAT, status);
  }

  private static void handleSneakyPeteChange(final String responseText) {
    if (responseText.contains("You hand him your button and take his glowstick")) {
      EquipmentManager.discardEquipment(ItemPool.NOVELTY_BUTTON);
      return;
    }

    if (responseText.contains("Ah, man, you dropped your crown back there!")) {
      EquipmentManager.discardEquipment(ItemPool.TATTERED_PAPER_CROWN);
      return;
    }
  }

  private static void handleTrickOrTreatingChange(final String responseText) {
    if (responseText.contains("pull the pumpkin off of your head")) {
      EquipmentManager.discardEquipment(ItemPool.PUMPKINHEAD_MASK);
      return;
    }
    if (responseText.contains("gick all over your mummy costume")) {
      EquipmentManager.discardEquipment(ItemPool.MUMMY_COSTUME);
      return;
    }
    if (responseText.contains("unzipping the mask and throwing it behind you")) {
      EquipmentManager.discardEquipment(ItemPool.WOLFMAN_MASK);
      return;
    }
    if (responseText.contains("Right on, brah. Here, have some gum.")) {
      ResultProcessor.processItem(ItemPool.RUSSIAN_ICE, -1);
      return;
    }
  }

  private static void handleCell37(final String responseText) {
    if (responseText.contains("scientists should have a file on me")
        || responseText.contains("Did you find that file yet")) {
      QuestDatabase.setQuestProgress(Quest.ESCAPE, QuestDatabase.STARTED);
    }
    // You pass the folder through the little barred window, and hear Subject 37 flipping through
    // the pages
    if (responseText.contains("pass the folder through")) {
      ResultProcessor.processItem(ItemPool.SUBJECT_37_FILE, -1);
      // You may already have the item you are sent to get
      if (InventoryManager.getCount(ItemPool.GOTO) > 0) {
        QuestDatabase.setQuestProgress(Quest.ESCAPE, "step3");
      } else {
        QuestDatabase.setQuestProgress(Quest.ESCAPE, "step2");
      }
    }
    // You pass the GOTO through the window, and Subject 37 thanks you.
    if (responseText.contains("pass the GOTO through")) {
      ResultProcessor.processItem(ItemPool.GOTO, -1);
      // You may already have the item you are sent to get
      if (InventoryManager.getCount(ItemPool.WEREMOOSE_SPIT) > 0) {
        QuestDatabase.setQuestProgress(Quest.ESCAPE, "step5");
      } else {
        QuestDatabase.setQuestProgress(Quest.ESCAPE, "step4");
      }
    }
    // You pass the little vial of of weremoose spit through the window.
    if (responseText.contains("pass the little vial")) {
      ResultProcessor.processItem(ItemPool.WEREMOOSE_SPIT, -1);
      // You may already have the item you are sent to get
      if (InventoryManager.getCount(ItemPool.ABOMINABLE_BLUBBER) > 0) {
        QuestDatabase.setQuestProgress(Quest.ESCAPE, "step7");
      } else {
        QuestDatabase.setQuestProgress(Quest.ESCAPE, "step6");
      }
    }
    // You hand Subject 37 the glob of abominable blubber.
    if (responseText.contains("hand Subject 37 the glob")) {
      ResultProcessor.processItem(ItemPool.ABOMINABLE_BLUBBER, -1);
      QuestDatabase.setQuestProgress(Quest.ESCAPE, QuestDatabase.FINISHED);
    }
  }

  private static void handleFriarsChange(final String responseText) {
    // "Thank you, Adventurer."

    if (responseText.contains("Thank you")
        || responseText.contains(
            "Please return to us if there's ever anything we can do for you in return")) {
      ResultProcessor.processItem(ItemPool.DODECAGRAM, -1);
      ResultProcessor.processItem(ItemPool.CANDLES, -1);
      ResultProcessor.processItem(ItemPool.BUTTERKNIFE, -1);
      int knownAscensions = Preferences.getInteger("knownAscensions");
      Preferences.setInteger("lastFriarCeremonyAscension", knownAscensions);
      QuestDatabase.setQuestProgress(Quest.FRIAR, QuestDatabase.FINISHED);
      if (KoLmafia.isAdventuring()) {
        KoLmafia.updateDisplay(MafiaState.PENDING, "Taint cleansed.");
      }
    }
  }

  private static void handleChasmChange(final String responseText) {
    // Using the Pocket Portable Bridge in License to Adventure: "You deploy your handy-dandy
    // portable bridge and quickly finish the job."
    if (responseText.contains("Huzzah!  The bridge is finished!")
        || responseText.contains("deploy your handy-dandy portable bridge")) {
      ResultProcessor.processItem(
          ItemPool.MORNINGWOOD_PLANK, -1 * InventoryManager.getCount(ItemPool.MORNINGWOOD_PLANK));
      ResultProcessor.processItem(
          ItemPool.HARDWOOD_PLANK, -1 * InventoryManager.getCount(ItemPool.HARDWOOD_PLANK));
      ResultProcessor.processItem(
          ItemPool.WEIRDWOOD_PLANK, -1 * InventoryManager.getCount(ItemPool.WEIRDWOOD_PLANK));
      ResultProcessor.processItem(
          ItemPool.THICK_CAULK, -1 * InventoryManager.getCount(ItemPool.THICK_CAULK));
      ResultProcessor.processItem(
          ItemPool.LONG_SCREW, -1 * InventoryManager.getCount(ItemPool.LONG_SCREW));
      ResultProcessor.processItem(
          ItemPool.BUTT_JOINT, -1 * InventoryManager.getCount(ItemPool.BUTT_JOINT));
      if (KoLmafia.isAdventuring()) {
        KoLmafia.updateDisplay(MafiaState.PENDING, "You have bridged the Orc Chasm.");
      }
      QuestDatabase.setQuestIfBetter(Quest.TOPPING, "step1");
    }
  }

  private static void handleABooPeakChange(final String responseText) {
    if (responseText.contains("Come On Ghosty, Light My Pyre")) {
      Preferences.setBoolean("booPeakLit", true);
      Preferences.setInteger("booPeakProgress", 0);
    }
  }

  private static void handleOilPeakChange(final String responseText) {
    if (responseText.contains("Unimpressed with Pressure")) {
      Preferences.setBoolean("oilPeakLit", true);
      Preferences.setFloat("oilPeakProgress", 0);
    }
  }

  private static void handleHighlandsChange(final String location, final String responseText) {
    if (location.contains("action=highlands_dude")) {
      if (responseText.contains("trying to, like, order a pizza")
          || responseText.contains("trying to order a pizza")) {
        QuestDatabase.setQuestProgress(Quest.TOPPING, "step2");
      } else if (responseText.contains("you're the one who totally lit all those fires")) {
        QuestDatabase.setQuestProgress(Quest.TOPPING, QuestDatabase.FINISHED);
      }
    }
    if (responseText.contains("orcchasm/fire1.gif")) {
      Preferences.setBoolean("booPeakLit", true);
      Preferences.setInteger("booPeakProgress", 0);
    }
    if (responseText.contains("orcchasm/fire2.gif")) {
      Preferences.setInteger("twinPeakProgress", 15);
    }
    if (responseText.contains("orcchasm/fire3.gif")) {
      Preferences.setBoolean("oilPeakLit", true);
      Preferences.setFloat("oilPeakProgress", 0);
    }

    if (Preferences.getBoolean("booPeakLit")
        && Preferences.getInteger("twinPeakProgress") == 15
        && Preferences.getBoolean("oilPeakLit")) {
      QuestDatabase.setQuestIfBetter(Quest.TOPPING, "step3");
    }
  }

  // Easily navigating the intense currents atop your trusty seahorse
  // <b>Shimmerswim</b>, you crest an undersea ridge and discover, spread
  // out beneath you, a magnificent Mer-kin City!
  private static final Pattern SEAHORSE_PATTERN =
      Pattern.compile("atop your trusty seahorse <b>(.*?)</b>");

  private static void handleSeaChange(final String location, final String responseText) {
    int area = AdventureRequest.parseArea(location);
    if (location.contains("action=oldman_oldman")) {
      if (responseText.contains("I lost my favorite boot, you see.")
          || responseText.contains("have you found my boot yet?")) {
        QuestDatabase.setQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED);
      } else if (responseText.contains("The old man snores fitfully")) {
        QuestDatabase.setQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.FINISHED);
      }
    }
    // Little Brother
    else if (location.contains("who=1")) {
      if (responseText.contains("wish my big brother was here")) {
        QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step1");
      } else if (responseText.contains("Wanna help me find Grandpa?")) {
        QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step4");
        if (KoLCharacter.isMuscleClass()) {
          Preferences.setBoolean("mapToAnemoneMinePurchased", true);
        } else if (KoLCharacter.isMysticalityClass()) {
          Preferences.setBoolean("mapToTheMarinaraTrenchPurchased", true);
        } else if (KoLCharacter.isMoxieClass()) {
          Preferences.setBoolean("mapToTheDiveBarPurchased", true);
        }
      } else if (responseText.contains("he's been actin' awful weird lately")) {
        QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step10");
      }
    }
    // Big Brother
    else if (location.contains("who=2")) {
      if (responseText.contains("I found this thing")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step11");
      }
    }
    // Grandpa
    else if (location.contains("action=grandpastory")) {
      if (responseText.contains("bet those lousy Mer-kin up and kidnapped her")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step6");
      } else if (responseText.contains("that note's definitely Grandma Sea Monkee's handwriting")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step7");
      } else if (responseText.contains("Gonna need one of them seahorses")) {
        Preferences.setBoolean("corralUnlocked", true);
      }
    } else if (area == AdventurePool.MERKIN_OUTPOST) {
      if (responseText.contains("Phew, that was a close one")) {
        QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step9");
        ConcoctionDatabase.setRefreshNeeded(false);
      }
    } else if (area == AdventurePool.CALIGINOUS_ABYSS) {
      if (responseText.contains("I should get dinner on the table for the boys")) {
        QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, QuestDatabase.FINISHED);
      }
    }
    // Learn about quest progress if visiting sea floor
    else if (location.startsWith("seafloor")) {
      if (responseText.contains("abyss")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step12");
      } else if (responseText.contains("outpost")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step6");
      } else if (responseText.contains("shipwreck")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step1");
      } else if (responseText.contains("monkeycastle")) {
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.STARTED);
      }

      if (responseText.contains("mine")) {
        if (KoLCharacter.isMuscleClass()) {
          QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step4");
        }
        Preferences.setBoolean("mapToAnemoneMinePurchased", true);
      }

      if (responseText.contains("trench")) {
        if (KoLCharacter.isMysticalityClass()) {
          QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step4");
        }
        Preferences.setBoolean("mapToTheMarinaraTrenchPurchased", true);
      }

      if (responseText.contains("divebar")) {
        if (KoLCharacter.isMoxieClass()) {
          QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step4");
        }
        Preferences.setBoolean("mapToTheDiveBarPurchased", true);
      }

      if (responseText.contains("reef")) {
        Preferences.setBoolean("mapToMadnessReefPurchased", true);
      }

      if (responseText.contains("skatepark")) {
        Preferences.setBoolean("mapToTheSkateParkPurchased", true);
      }

      if (responseText.contains("currents")) {
        Preferences.setBoolean("intenseCurrents", true);
      }

      if (responseText.contains("corral")) {
        Preferences.setBoolean("corralUnlocked", true);
      }
    }
    // Learn about quest progress if visiting sea monkey castle
    else if (location.startsWith("monkeycastle")) {
      if (responseText.contains("who=4")) {
        // Mom
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.FINISHED);
      } else if (responseText.contains("whichshop=grandma")) {
        // Grandma
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step9");
      } else if (responseText.contains("who=3")) {
        // Grandpa
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step5");
      } else if (responseText.contains("who=2")) {
        // Big Brother
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step2");
        Preferences.setBoolean("bigBrotherRescued", true);
      } else if (responseText.contains("who=1")) {
        // Little Brother
        QuestDatabase.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.STARTED);
      }
    }
    // Learn seahorse name by visiting Mer-Kin Deepcity
    else if (location.startsWith("sea_merkin")) {
      Matcher m = SEAHORSE_PATTERN.matcher(responseText);
      if (m.find()) {
        Preferences.setString("seahorseName", m.group(1));
      }
    }
  }

  private static void handlePlainsChange(final String responseText) {
    // You stare at the pile of coffee grounds for a minute and it
    // occurs to you that maybe your grandma wasn't so crazy after
    // all. You pull out an enchanted bean and plop it into the
    // pile of grounds. It immediately grows into an enormous
    // beanstalk.

    if (responseText.contains("immediately grows into an enormous beanstalk")) {
      ResultProcessor.processItem(ItemPool.ENCHANTED_BEAN, -1);
      QuestDatabase.setQuestProgress(Quest.GARBAGE, "step1");
      if (KoLmafia.isAdventuring()) {
        KoLmafia.updateDisplay(MafiaState.PENDING, "You have planted a beanstalk.");
      }
    }

    if (responseText.contains("palinlink.gif")) {
      QuestDatabase.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED);
    }
  }

  public static final void handleElVibratoChange(final String location, final String responseText) {
    Preferences.decrement("currentPortalEnergy", 1, 0);
    CampgroundRequest.updateElVibratoPortal();
  }

  public static final void handleBeanstalkChange(final String location, final String responseText) {
    // If you can adventure in areas, it tells us about quests
    int area = AdventureRequest.parseArea(location);
    if (area == AdventurePool.AIRSHIP) {
      // Airship available
      QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step1");
      if (responseText.contains("we're looking for the Four Immateria")) {
        QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step2");
      }
    } else if (area == AdventurePool.CASTLE_BASEMENT) {
      // Castle basement available
      QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step7");
      if (responseText.contains("New Area Unlocked") && responseText.contains("The Ground Floor")) {
        Preferences.setInteger("lastCastleGroundUnlock", KoLCharacter.getAscensions());
        QuestDatabase.setQuestProgress(Quest.GARBAGE, "step8");
      }
    } else if (area == AdventurePool.CASTLE_GROUND) {
      // Castle Ground floor available
      QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step8");
      if (responseText.contains("New Area Unlocked") && responseText.contains("The Top Floor")) {
        Preferences.setInteger("lastCastleTopUnlock", KoLCharacter.getAscensions());
        QuestDatabase.setQuestProgress(Quest.GARBAGE, "step9");
      }
    } else if (area == AdventurePool.CASTLE_TOP
        && !responseText.contains("You have to learn to walk")
        && !responseText.contains("You'll have to figure out some other way")) {
      // Castle Top floor available
      QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step9");
    }
  }

  private static void handleZeppelinMobChange(final String responseText) {
    if (responseText.contains("mob has cleared out")) {
      QuestDatabase.setQuestProgress(Quest.RON, "step2");
    } else {
      QuestDatabase.setQuestIfBetter(Quest.RON, "step1");
    }
  }

  private static void handleZeppelinChange(final String responseText) {
    if (responseText.contains("sneak aboard the Zeppelin")) {
      QuestDatabase.setQuestProgress(Quest.RON, "step3");
    } else {
      QuestDatabase.setQuestIfBetter(Quest.RON, "step2");
    }
  }

  private static void handlePalindomeChange(final String location, final String responseText) {
    if (location.contains("action=pal_mr")) {
      if (responseText.contains("in the mood for a bowl of wet stunt nut stew")) {
        QuestDatabase.setQuestProgress(Quest.PALINDOME, "step3");
        ResultProcessor.removeItem(ItemPool.PALINDROME_BOOK_2);
      }
    }
  }

  private static void handleCanadiaChange(final String location, final String responseText) {
    if (location.contains("action=lc_marty")) {
      if (responseText.contains("All right, Marty, I'll see what I can do")) {
        QuestDatabase.setQuestProgress(Quest.SWAMP, QuestDatabase.STARTED);
      }
    }
  }

  private static void handleMaraisChange(final String responseText) {
    // Detect unlocked areas
    if (responseText.contains("The Edge of the Swamp")) {
      QuestDatabase.setQuestIfBetter(Quest.SWAMP, QuestDatabase.STARTED);
    }
    if (responseText.contains("The Dark and Spooky Swamp")) {
      Preferences.setBoolean("maraisDarkUnlock", true);
    }
    if (responseText.contains("The Wildlife Sanctuarrrrrgh")) {
      Preferences.setBoolean("maraisWildlifeUnlock", true);
    }
    if (responseText.contains("The Corpse Bog")) {
      Preferences.setBoolean("maraisCorpseUnlock", true);
    }
    if (responseText.contains("The Ruined Wizard Tower")) {
      Preferences.setBoolean("maraisWizardUnlock", true);
    }
    if (responseText.contains("Swamp Beaver Territory")) {
      Preferences.setBoolean("maraisBeaverUnlock", true);
    }
    if (responseText.contains("The Weird Swamp Village")) {
      Preferences.setBoolean("maraisVillageUnlock", true);
    }
  }

  private static final Pattern EXP_PATTERN = Pattern.compile("\\(([\\d]+)%explored\\)");

  private static void handleBeachChange(final String responseText) {
    String expString = ResponseTextParser.parseDivLabel("db_l11desertlabel", responseText);
    Matcher matcher = QuestManager.EXP_PATTERN.matcher(expString);
    if (matcher.find()) {
      int explored = StringUtilities.parseInt(matcher.group(1));
      QuestManager.setDesertExploration(explored);
    }
    Preferences.setBoolean("oasisAvailable", responseText.contains("db_oasis"));
  }

  private static void setDesertExploration(final int explored) {
    int current = Preferences.getInteger("desertExploration");
    QuestManager.setDesertExploration(current, explored - current);
  }

  public static final void incrementDesertExploration(final int increment) {
    int current = Preferences.getInteger("desertExploration");
    QuestManager.setDesertExploration(current, increment);
  }

  private static void setDesertExploration(final int current, final int increment) {
    // If we've already registered complete desert exploration, we're done
    if (current == 100) {
      return;
    }

    // Peg new exploration percentage at 100
    int explored = Math.min(current + increment, 100);

    // Save new exploration percentage
    Preferences.setInteger("desertExploration", explored);

    // If we are done, update the quest
    if (explored == 100) {
      QuestDatabase.setQuestProgress(Quest.DESERT, QuestDatabase.FINISHED);
    }
  }

  private static void handleTrapperChange(final String responseText) {
    Matcher oreMatcher = ORE_PATTERN.matcher(responseText);
    if (oreMatcher.find()) {
      Preferences.setString("trapperOre", oreMatcher.group(1) + " ore");
      QuestDatabase.setQuestIfBetter(Quest.TRAPPER, "step1");
    } else if (responseText.contains("He takes the load of cheese and ore")
        || responseText.contains("haul your load of ore and cheese")) {
      AdventureResult item = ItemPool.get(Preferences.getString("trapperOre"), -3);
      ResultProcessor.processResult(item);
      ResultProcessor.processResult(ItemPool.get(ItemPool.GOAT_CHEESE, -3));
      QuestDatabase.setQuestIfBetter(Quest.TRAPPER, "step2");
    }

    // Yeehaw!  I heard the noise and seen them mists dissapatin' from clear down here!  Ya done it!
    //  Ya rightly done it!
    else if (responseText.contains("Yeehaw!  I heard the noise")) {
      Preferences.setInteger("lastTr4pz0rQuest", KoLCharacter.getAscensions());
      ResultProcessor.removeItem(ItemPool.GROARS_FUR);
      QuestDatabase.setQuestProgress(Quest.TRAPPER, QuestDatabase.FINISHED);
    }

    // "Yeehaw!" John exclaims as you drag the huge yeti pelt into his shack.
    else if (responseText.contains("drag the huge yeti pelt into his shack")) {
      Preferences.setInteger("lastTr4pz0rQuest", KoLCharacter.getAscensions());
      ResultProcessor.removeItem(ItemPool.WINGED_YETI_FUR);
      QuestDatabase.setQuestProgress(Quest.TRAPPER, QuestDatabase.FINISHED);
    }
  }

  private static void handleExtremityChange(final String responseText) {
    if (responseText.contains("Discovering Your Extremity")
        || responseText.contains("2 eXXtreme 4 U")
        || responseText.contains("3 eXXXtreme 4ever 6pack")) {
      Preferences.increment("currentExtremity");
    }
  }

  private static void handleCouncilChange(final String responseText) {
    Preferences.setInteger("lastCouncilVisit", KoLCharacter.getLevel());

    if (responseText.contains("Thanks for the larva, Adventurer. We'll put this to good use.")) {
      ResultProcessor.removeItem(ItemPool.MOSQUITO_LARVA);
    }
    QuestDatabase.handleCouncilText(responseText);
    if (QuestDatabase.isQuestStarted(Quest.MACGUFFIN)) {
      QuestDatabase.setQuestIfBetter(Quest.BLACK, QuestDatabase.STARTED);
    }
  }

  private static void handleDungeonsChange(final String responseText) {
    if (responseText.contains("barrelshrine")) {
      Preferences.setBoolean("barrelShrineUnlocked", true);
    }
  }

  public static final void unlockGoatlet() {
    AdventureRequest goatlet = new AdventureRequest("Goatlet", AdventurePool.GOATLET);

    if (KoLCharacter.inFistcore()) {
      // You can actually get here without knowing Worldpunch
      // in Softcore by pulling ores.
      if (!KoLCharacter.hasSkill(SkillPool.WORLDPUNCH)) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "Try again after you learn Worldpunch.");
        return;
      }

      // If you don't have Earthen Fist active, get it.
      if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.EARTHEN_FIST))) {
        UseSkillRequest request = UseSkillRequest.getInstance(SkillPool.WORLDPUNCH);
        request.setBuffCount(1);
        RequestThread.postRequest(request);
      }

      // Perhaps you ran out of MP.
      if (!KoLmafia.permitsContinue()) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "Cast Worldpunch and try again.");
      }

      RequestThread.postRequest(goatlet);
      return;
    }

    if (KoLCharacter.inAxecore()) {
      // No outfit needed; just take option #3
      RequestThread.postRequest(goatlet);
      return;
    }

    if (!EquipmentManager.hasOutfit(OutfitPool.MINING_OUTFIT)) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "You need a mining outfit to continue.");
      return;
    }

    if (EquipmentManager.isWearingOutfit(OutfitPool.MINING_OUTFIT)) {
      RequestThread.postRequest(goatlet);
      return;
    }

    try (Checkpoint checkpoint = new Checkpoint()) {
      (new EquipmentRequest(EquipmentDatabase.getOutfit(OutfitPool.MINING_OUTFIT))).run();
      RequestThread.postRequest(goatlet);
    }
  }

  private static Map<String, Float> OIL_MONSTER_PROGRESS =
      Map.ofEntries(
          Map.entry("oil slick", 6.34f),
          Map.entry("oil tycoon", 19.02f),
          Map.entry("oil baron", 31.7f),
          Map.entry("oil cartel", 63.4f));

  /**
   * After we win a fight, some quests may need to be updated. Centralize handling for it here.
   *
   * @param responseText The text from (at least) the winning round of the fight
   * @param monsterName The monster which <s>died</s>got beaten up.
   */
  public static void updateQuestData(String responseText, String monsterName) {
    String adventureId = KoLAdventure.lastAdventureIdString();
    String counter =
        adventureId.equals("ns_01_crowd1")
            ? "nsContestants1"
            : adventureId.equals("ns_01_crowd2")
                ? "nsContestants2"
                : adventureId.equals("ns_01_crowd3") ? "nsContestants3" : null;

    if (counter != null) {
      int crowd = Preferences.getInteger(counter);
      if (crowd > 0) {
        Preferences.setInteger(counter, crowd - 1);
      }
      if (Preferences.getInteger("nsContestants1") == 0
          && Preferences.getInteger("nsContestants2") == 0
          && Preferences.getInteger("nsContestants3") == 0) {
        QuestDatabase.setQuestProgress(Quest.FINAL, "step2");
      }
      return;
    }

    boolean ghostBusted = false;

    switch (monsterName.trim()) {
      case "screambat" -> {
        if (!QuestDatabase.isQuestLaterThan(Quest.BAT, "step2")) {
          QuestDatabase.advanceQuest(Quest.BAT);
        }
      }
      case "dirty thieving brigand" -> {
        // "Well," you say, "it would really help the war effort if
        // your convent could serve as a hospital for our wounded
        // troops."
        if (responseText.contains("could serve as a hospital")) {
          Preferences.setString("sidequestNunsCompleted", "hippy");
        } else if (responseText.contains("could serve as a massage parlor")) {
          Preferences.setString("sidequestNunsCompleted", "fratboy");
        }
      }
      case "oil slick", "oil tycoon", "oil baron", "oil cartel" -> {
        // oil slick: 6.34
        // oil tycoon: 19.02
        // oil baron: 31.7
        // oil cartel: 63.4
        // dress pants: 6.34
        // lovebug: 6.34
        if (OIL_MONSTER_PROGRESS.containsKey(monsterName)) {
          double pantsBonus =
              InventoryManager.getEquippedCount(ItemPool.DRESS_PANTS) > 0 ? 6.34 : 0;
          float current = Preferences.getFloat("oilPeakProgress");
          double lovebug = responseText.contains("love oil beetle trundles up") ? 6.34 : 0;

          // normalize
          String setTo =
              String.format(
                  Locale.US,
                  "%.2f",
                  Math.max(
                      0, current - OIL_MONSTER_PROGRESS.get(monsterName) - pantsBonus - lovebug));

          Preferences.setString("oilPeakProgress", setTo);
        }
      }
      case "Battlie Knight Ghost",
          "Claybender Sorcerer Ghost",
          "Dusken Raider Ghost",
          "Space Tourist Explorer Ghost",
          "Whatsian Commando Ghost" -> {
        Preferences.decrement("booPeakProgress", 2);
      }
      case "panicking Knott Yeti" -> {
        QuestDatabase.setQuestIfBetter(Quest.TRAPPER, "step4");
      }
      case "pygmy witch accountant" -> {
        // If you don't have McClusky File (complete), or
        // McClusky File 5, and accountant doesn't drop file,
        // you must have unlocked office boss
        if (InventoryManager.getCount(ItemPool.MCCLUSKY_FILE) == 0
            && InventoryManager.getCount(ItemPool.MCCLUSKY_FILE_PAGE5) == 0
            && Preferences.getInteger("hiddenOfficeProgress") < 6
            && !responseText.contains("McClusky file")) {
          Preferences.setInteger("hiddenOfficeProgress", 6);
        }
      }
      case "topiary gopher", "topiary chihuahua herd", "topiary duck", "topiary kiwi" -> {
        // We are still in the Hedge Maze
        QuestDatabase.setQuestProgress(Quest.FINAL, "step4");
      }
      case "wall of skin" -> {
        QuestDatabase.setQuestProgress(Quest.FINAL, "step7");
      }
      case "wall of meat" -> {
        if (responseText.contains("the stairs to the next floor are clear")) {
          QuestDatabase.setQuestProgress(Quest.FINAL, "step8");
        }
      }
      case "wall of bones" -> {
        QuestDatabase.setQuestProgress(Quest.FINAL, "step9");
      }
      case "Your Shadow" -> {
        QuestDatabase.setQuestProgress(Quest.FINAL, "step11");
      }
      case "Clancy" -> {
        // We do not currently have a distinct step for Clancy
        QuestDatabase.setQuestProgress(Quest.FINAL, "step11");
      }
      case "Naughty Sorceress (3)",
          "The Avatar of Sneaky Pete",
          "The Avatar of Boris",
          "Principal Mooney",
          "Rene C. Corman",
          "The Avatar of Jarlsberg",
          "The Rain King",
          "One Thousand Source Agents",
          "Jerry Bradford, Pok&eacute;fam World Champion",
          "\"Blofeld\"",
          "Nautomatic Sorceress",
          "%alucard%" -> {
        QuestDatabase.setQuestProgress(Quest.FINAL, "step13");
      }
      case "Guy Made Of Bees" -> {
        // In addition to appearing in the Haunted Bathroom, this monster
        // replaces the Naughty Sorceress in Bees Hate You.
        if (responseText.contains("Thwaitgold bee statuette")) {
          QuestDatabase.setQuestProgress(Quest.FINAL, "step13");
        }
        // Interestingly enough, we track defeating the other one by
        // detecting his item drop in ResultProcessor
      }
      case "The Unknown Seal Clubber",
          "The Unknown Turtle Tamer",
          "The Unknown Pastamancer",
          "The Unknown Sauceror",
          "The Unknown Disco Bandit",
          "The Unknown Accordion Thief" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step2");
      }
      case "The Clownlord Beelzebozo" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step6");
      }
      case "menacing thug" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step19");
      }
      case "Mob Penguin hitman" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step21");
      }
      case "hunting seal",
          "turtle trapper",
          "evil spaghetti cult assassin",
          "b&eacute;arnaise zombie",
          "flock of seagulls",
          "mariachi bandolero" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step23");
      }
      case "Gorgolok, the Infernal Seal (Volcanic Cave)",
          "Stella, the Turtle Poacher (Volcanic Cave)",
          "Spaghetti Elemental (Volcanic Cave)",
          "Lumpy, the Sinister Sauceblob (Volcanic Cave)",
          "Spirit of New Wave (Volcanic Cave)",
          "Somerset Lopez, Dread Mariachi (Volcanic Cave)" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step29");
      }
      case "Sloppy Seconds Burger" -> {
        if (responseText.contains("You consult the list and grab the next ingredient")) {
          Preferences.increment("buffJimmyIngredients", 1);
          if (Preferences.getInteger("buffJimmyIngredients") >= 15) {
            QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, "step1");
          }
        }
      }
      case "Sloppy Seconds Cocktail" -> {
        if (responseText.contains("cocktail sauce bottle")
            || responseText.contains("defeated foe with your bottle")) {
          Preferences.increment("tacoDanCocktailSauce", 1);
          if (Preferences.getInteger("tacoDanCocktailSauce") >= 15) {
            QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, "step1");
          }
        }
      }
      case "Sloppy Seconds Sundae" -> {
        if (responseText.contains("sprinkles off")) {
          Preferences.increment("brodenSprinkles", 1);
          if (Preferences.getInteger("brodenSprinkles") >= 15) {
            QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, "step1");
          }
        }
      }
      case "taco fish" -> {
        Matcher FishMeatMatcher = QuestManager.TACO_FISH_PATTERN.matcher(responseText);
        if (FishMeatMatcher.find()) {
          Preferences.increment(
              "tacoDanFishMeat", StringUtilities.parseInt(FishMeatMatcher.group(1)));
          if (Preferences.getInteger("tacoDanFishMeat") >= 300) {
            QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, "step1");
          }
        }
      }
      case "Fun-Guy Playmate" -> {
        Preferences.increment("funGuyMansionKills");
        if (responseText.contains("hot tub with some more bacteria")) {
          Preferences.increment("brodenBacteria", 1);
          if (Preferences.getInteger("brodenBacteria") >= 10) {
            QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, "step1");
          }
        }
      }
      case "Wu Tang the Betrayer" -> {
        Preferences.setInteger("lastWuTangDefeated", KoLCharacter.getAscensions());
      }
      case "Baron Von Ratsworth" -> {
        TavernRequest.addTavernLocation('6');
      }
      case "Source Agent" -> {
        Preferences.increment("sourceAgentsDefeated");
      }
      case "pair of burnouts" -> {
        int increment = responseText.contains("throw the opium grenade") ? 3 : 1;
        Preferences.increment("burnoutsDefeated", increment, 30, false);
        if (Preferences.getInteger("burnoutsDefeated") == 30) {
          QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step4");
        }
      }
      case "biclops" -> {
        QuestDatabase.setQuestProgress(Quest.CITADEL, "step5");
      }
      case "surprised and annoyed witch", "extremely annoyed witch" -> {
        QuestDatabase.setQuestProgress(Quest.CITADEL, "step7");
      }
      case "Elp&iacute;zo & Crosybdis" -> {
        QuestDatabase.setQuestProgress(Quest.CITADEL, "step10");
      }
      case "hulking bridge troll" -> {
        OrcChasmRequest.setChasmProgress(0);
      }
      case "warehouse guard", "warehouse janitor", "warehouse clerk" -> {
        Preferences.increment("warehouseProgress", 1);
      }
      case "E.V.E., the robot zombie" -> {
        QuestDatabase.setQuestProgress(Quest.EVE, "step1");
      }
      case "writing desk" -> {
        if (QuestDatabase.isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE)
            && !InventoryManager.hasItem(ItemPool.SPOOKYRAVEN_NECKLACE)
            && !QuestDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)) {
          Preferences.increment("writingDesksDefeated", 1, 5, false);
        }
      }
      case "nasty bear" -> {
        Preferences.increment("dinseyNastyBearsDefeated", 1, 8, false);
        QuestDatabase.setQuestProgress(
            Quest.NASTY_BEARS,
            (Preferences.getInteger("dinseyNastyBearsDefeated") == 8 ? "step2" : "step1"));
      }
      case "Wart Dinsey" -> {
        Preferences.setInteger("lastWartDinseyDefeated", KoLCharacter.getAscensions());
      }
      case "Cake Lord" -> {
        QuestDatabase.setQuestProgress(Quest.ARMORER, "step3");
      }
      case "X-32-F Combat Training Snowman" -> {
        int snowparts = Preferences.getInteger("_snojoParts");
        Preferences.setInteger("_snojoFreeFights", Math.min(snowparts, 10));
        if (snowparts <= 10) {
          String snojoSetting = Preferences.getString("snojoSetting");
          switch (snojoSetting) {
            case "MUSCLE" -> Preferences.increment("snojoMuscleWins");
            case "MYSTICALITY" -> Preferences.increment("snojoMysticalityWins");
            case "MOXIE" -> Preferences.increment("snojoMoxieWins");
          }
        }
      }
      case "drunk cowpoke",
          "surly gambler",
          "wannabe gunslinger",
          "cow cultist",
          "hired gun",
          "camp cook",
          "skeletal gunslinger",
          "restless ghost",
          "buzzard",
          "mountain lion",
          "grizzled bear",
          "diamondback rattler",
          "coal snake",
          "frontwinder",
          "caugr",
          "pyrobove",
          "spidercow",
          "moomy" -> {
        Preferences.increment("lttQuestStageCount", 1);
      }
      case "Jeff the Fancy Skeleton",
          "Daisy the Unclean",
          "Pecos Dave",
          "Pharaoh Amoon-Ra Cowtep",
          "Snake-Eyes Glenn",
          "Former Sheriff Dan Driscoll",
          "unusual construct",
          "Clara",
          "Granny Hackleton" -> {
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.UNSTARTED);
        Preferences.setInteger("lttQuestDifficulty", 0);
        Preferences.setInteger("lttQuestStageCount", 0);
        Preferences.setString("lttQuestName", "");
      }
      case "the ghost of Oily McBindle",
          "boneless blobghost",
          "the ghost of Monsieur Baguelle",
          "The Headless Horseman",
          "The Icewoman",
          "The ghost of Ebenoozer Screege",
          "The ghost of Lord Montague Spookyraven",
          "The ghost of Vanillica \"Trashblossom\" Gorton",
          "The ghost of Sam McGee",
          "The ghost of Richard Cockingham",
          "The ghost of Waldo the Carpathian",
          "Emily Koops, a spooky lime",
          "The ghost of Jim Unfortunato" -> {
        QuestDatabase.setQuestProgress(Quest.GHOST, QuestDatabase.UNSTARTED);
        Preferences.setString("ghostLocation", "");
        ghostBusted = true;
      }
      case "Drab Bard", "Bob Racecar", "Racecar Bob" -> {
        if (QuestDatabase.isQuestStep(Quest.PALINDOME, QuestDatabase.STARTED)) {
          Preferences.increment("palindomeDudesDefeated", 1, 20, false);
        }
      }
      case "fantasy bandit",
          "fantasy ourk",
          "fantasy forest faerie",
          "swamp monster",
          "cursed villager",
          "mining grobold",
          "rubber bat",
          "quadfaerie",
          "druid plants",
          "flock of every birds",
          "plywood cultists",
          "barrow wraith",
          "regular thief",
          "swamp troll",
          "crypt creeper",
          "\"Phoenix\"",
          "Sewage Treatment Dragon",
          "Duke Vampire",
          "Spider Queen",
          "Archwizard",
          "Ley Incursion",
          "Ghoul King",
          "Ogre Chieftain",
          "Ted Schwartz, Master Thief",
          "Skeleton Lord" -> {
        QuestManager.addFantasyRealmKill(monsterName);
      }
      case "spooky ghost" -> {
        // A monster with this name appears in both Dreadsylvania and FantasyRealm
        if (KoLAdventure.lastAdventureId() != AdventurePool.DREAD_VILLAGE) {
          QuestManager.addFantasyRealmKill(monsterName);
        }
      }
      case "biker", "\"plain\" girl", "jock", "party girl", "burnout" -> {
        int turnsSpent = Preferences.getInteger("_neverendingPartyFreeTurns");
        if (turnsSpent < 10) {
          Preferences.setInteger("_neverendingPartyFreeTurns", turnsSpent + 1);
        }
        if (Preferences.getString("_questPartyFairQuest").equals("partiers")) {
          int kills = KoLCharacter.hasEquipped(ItemPool.INTIMIDATING_CHAINSAW) ? 2 : 1;
          Preferences.decrement("_questPartyFairProgress", kills, 0);
          if (Preferences.getInteger("_questPartyFairProgress") < 1) {
            QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step2");
          }
          String message =
              "There are "
                  + Preferences.getInteger("_questPartyFairProgress")
                  + " partiers remaining.";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        } else if (Preferences.getString("_questPartyFairQuest").equals("woots")) {
          // Do not know exactly how many woots are added each time, so find out from quest log
          (new GenericRequest("questlog.php?which=1")).run();
          String message =
              "The Party is at "
                  + Preferences.getInteger("_questPartyFairProgress")
                  + "/100 woots.";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        } else if (Preferences.getString("_questPartyFairQuest").equals("dj")) {
          Matcher djMatcher = DJ_MEAT_PATTERN.matcher(responseText);
          if (djMatcher.find()) {
            int meat = StringUtilities.parseInt(djMatcher.group(0).replaceAll(",", ""));
            Preferences.decrement("_questPartyFairProgress", meat, 0);
            if (Preferences.getInteger("_questPartyFairProgress") < 1) {
              QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step2");
            }
            ResultProcessor.processMeat(-meat);
            String message = "You collect " + meat + " Meat for the DJ.";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        } else if (Preferences.getString("_questPartyFairQuest").equals("trash")) {
          Matcher trashMatcher = TRASH_PATTERN.matcher(responseText);
          if (trashMatcher.find()) {
            int trash = StringUtilities.parseInt(trashMatcher.group(1));
            Preferences.decrement("_questPartyFairProgress", trash, 0);
            if (Preferences.getInteger("_questPartyFairProgress") < 1) {
              QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step2");
            }
            String message = "You clean up " + trash + " for the environment.";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        }
      }
      case "Steve Belmont", "Koopa Paratroopa", "Boss Bot" -> {
        QuestDatabase.setQuestProgress(Quest.BAT, "step4");
      }
      case "Ricardo Belmont", "Hammer Brother", "Gobot King" -> {
        QuestDatabase.setQuestProgress(Quest.GOBLIN, QuestDatabase.FINISHED);
      }
      case "Jayden Belmont", "Very Dry Bones", "Robonerdagon" -> {
        QuestDatabase.setQuestProgress(Quest.CYRPT, "step1");
      }
      case "Sharona", "Angry Sun", "Groarbot" -> {
        QuestDatabase.setQuestProgress(Quest.TRAPPER, "step5");
      }
      case "smut orc jacker", "smut orc nailer", "smut orc pipelayer", "smut orc screwer" -> {
        // Out of the corner of your eye, you see another smut orc shiver and
        // disappear into a nasty-looking shack.
        if (responseText.contains("another smut orc shiver")) {
          Preferences.increment("smutOrcNoncombatProgress", 1, 15, false);
        }
        // Out of the corner of your eye, you see two smut orcs give one
        // another a meaningful glance, shiver, and disappear into a nearby
        // shack.
        else if (responseText.contains("you see two smut orcs")) {
          Preferences.increment("smutOrcNoncombatProgress", 2, 15, false);
        }
        // You see a nearby group of smut orcs huddling together for
        // warmth. You are glad they are as far away as they are.
        else if (responseText.contains("smut orcs huddling together")) {
          Preferences.increment("smutOrcNoncombatProgress", 3, 15, false);
        }
        // You hear a bunch of windows being slammed shut against the cold.
        else if (responseText.contains("windows being slammed shut")) {
          Preferences.increment("smutOrcNoncombatProgress", 4, 15, false);
        }
        // Dozens of nearby smut orcs, shivering in the cold, rush into their
        // shacks and slam the doors.
        else if (responseText.contains("Dozens of nearby smut orcs")) {
          Preferences.increment("smutOrcNoncombatProgress", 5, 15, false);
        }
      }
      case "The Thing with No Name" -> {
        ResultProcessor.processResult(ItemPool.get(ItemPool.FURIOUS_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.VANITY_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.LECHEROUS_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.JEALOUSY_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.AVARICE_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.GLUTTONOUS_STONE, -1));

        QuestDatabase.setQuest(Quest.CLUMSINESS, QuestDatabase.UNSTARTED);
        QuestDatabase.setQuest(Quest.GLACIER, QuestDatabase.UNSTARTED);
        QuestDatabase.setQuest(Quest.MAELSTROM, QuestDatabase.UNSTARTED);

        Preferences.setInteger("lastThingWithNoNameDefeated", KoLCharacter.getAscensions());
      }
      case "The Superconductor" -> {
        Preferences.setBoolean("superconductorDefeated", true);
      }
      case "shadow spire",
          "shadow orrery",
          "shadow tongue",
          "shadow scythe",
          "shadow cauldron",
          "shadow matrix" -> {
        QuestDatabase.setQuestProgress(Quest.RUFUS, "step1");
      }
    }

    int adventure = KoLAdventure.lastAdventureId();

    switch (adventure) {
      case AdventurePool.MERKIN_COLOSSEUM:
        // Do not increment round for wandering monsters
        switch (monsterName) {
          case "Mer-kin balldodger",
              "Mer-kin netdragger",
              "Mer-kin bladeswitcher",
              "Georgepaul, the Balldodger",
              "Johnringo, the Netdragger",
              "Ringogeorge, the Bladeswitcher" -> {
            // Don't mark path chosen unless won round 15
            if (Preferences.increment("lastColosseumRoundWon", 1) == 15) {
              Preferences.setString("merkinQuestPath", "gladiator");
            }
          }
        }
        break;

      case AdventurePool.THE_DAILY_DUNGEON:
        Preferences.increment("_lastDailyDungeonRoom", 1);
        break;

      case AdventurePool.ARID_DESERT:
        // As you're about to collapse from dehydration, you stagger
        // over one last dune to discover a verdant oasis.
        if (responseText.contains("discover a verdant oasis")) {
          Preferences.setBoolean("oasisAvailable", true);
        }

        // clingy monsters do not increment exploration
        if (!responseText.contains("Desert exploration")) {
          break;
        }

        int explored = 1;

        if (KoLCharacter.hasEquipped(ItemPool.UV_RESISTANT_COMPASS)) {
          explored += 1;
        } else if (KoLCharacter.hasEquipped(ItemPool.DOWSING_ROD)) {
          explored += 2;
        }

        if (KoLCharacter.hasEquipped(ItemPool.SURVIVAL_KNIFE)
            && KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ULTRAHYDRATED))) {
          explored += 2;
        }

        if (KoLCharacter.getFamiliar().getId() == FamiliarPool.MELODRAMEDARY) {
          explored += 1;
        }

        if (Preferences.getString("peteMotorbikeHeadlight").equals("Blacklight Bulb")) {
          explored += 2;
        } else if (Preferences.getBoolean("bondDesert")
            && Preferences.getInteger("desertExploration") > 0) {
          // Universal GPS doesn't help on the first turn.  Probably a KoL bug, but it probably
          // won't get fixed.
          explored += 2;
        }

        QuestManager.incrementDesertExploration(explored);
        break;

      case AdventurePool.ZEPPELIN_PROTESTORS:
        Matcher LighterMatcher = QuestManager.LIGHTER_PATTERN.matcher(responseText);
        if (LighterMatcher.find()) {
          int flamingProtesters = StringUtilities.parseInt(LighterMatcher.group(1));
          // Lighter defeats the protester being attacked as well as the nearby group
          Preferences.increment("zeppelinProtestors", flamingProtesters + 1);
          RequestLogger.printLine("Set fire to " + flamingProtesters + " protesters");
        } else {
          Preferences.increment("zeppelinProtestors", 1);
        }
        break;

      case AdventurePool.RED_ZEPPELIN:
        if (responseText.contains("inevitable confrontation with Ron Copperhead")) {
          QuestDatabase.setQuestProgress(Quest.RON, "step4");
        }
        break;

      case AdventurePool.HAUNTED_KITCHEN:
        if (!InventoryManager.hasItem(ItemPool.BILLIARDS_KEY)) {
          Matcher drawerMatcher = QuestManager.DRAWER_PATTERN.matcher(responseText);
          if (drawerMatcher.find()) {
            Preferences.increment(
                "manorDrawerCount", StringUtilities.parseInt(drawerMatcher.group(1)));
          } else {
            Preferences.increment("manorDrawerCount", 1);
          }
        }
        break;

      case AdventurePool.BLACK_FOREST:
        if (responseText.contains("discover the trail leading to the Black Market")) {
          QuestDatabase.setQuestProgress(Quest.MACGUFFIN, "step1");
          QuestDatabase.setQuestProgress(Quest.BLACK, "step2");
          Preferences.setInteger("blackForestProgress", 5);
        } else {
          if (responseText.contains("find a row of blackberry bushes so thick")) {
            Preferences.setInteger("blackForestProgress", 1);
          } else if (responseText.contains("find a cozy black cottage nestled deep")) {
            Preferences.setInteger("blackForestProgress", 2);
          } else if (responseText.contains("spot a mineshaft sunk deep into the black depths")) {
            Preferences.setInteger("blackForestProgress", 3);
          } else if (responseText.contains(
              "find a church that would be picturesque if it wasn't so sinister")) {
            Preferences.setInteger("blackForestProgress", 4);
          }

          QuestDatabase.setQuestIfBetter(Quest.BLACK, "step1");
        }
        break;

      case AdventurePool.DEEP_DARK_JUNGLE:
        if (responseText.contains("jungle pun occurs to you")) {
          Preferences.increment("junglePuns", 1);
          if (Preferences.getInteger("junglePuns") >= 11) {
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, "step2");
          }
        }
        break;

      case AdventurePool.SECRET_GOVERNMENT_LAB:
        Matcher GoreMatcher = QuestManager.GORE_PATTERN.matcher(responseText);
        if (GoreMatcher.find()) {
          Preferences.increment("goreCollected", StringUtilities.parseInt(GoreMatcher.group(1)));
          if (Preferences.getInteger("goreCollected") >= 100) {
            QuestDatabase.setQuestProgress(Quest.GORE, "step2");
          }
        }
        if (responseText.contains("The gore sloshes around nauseatingly in your bucket")) {
          QuestDatabase.setQuestProgress(Quest.GORE, "step2");
        }
        break;

      case AdventurePool.BARF_MOUNTAIN:
        if (responseText.contains("made it to the front of the line")) {
          Preferences.setBoolean("dinseyRollercoasterNext", true);
        }
        break;

      case AdventurePool.GARBAGE_BARGES:
        if (QuestDatabase.isQuestStarted(Quest.SOCIAL_JUSTICE_I)) {
          Preferences.increment("dinseySocialJusticeIProgress", 1);
        } else if (responseText.contains("probably not embarrassingly sexist anymore")) {
          Preferences.setInteger("dinseySocialJusticeIProgress", 15);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, "step1");
        }
        if (responseText.contains("at least the barges aren't getting hung up on it anymore")) {
          Preferences.setInteger("dinseyFilthLevel", 0);
          QuestDatabase.setQuestProgress(Quest.FISH_TRASH, "step2");
        } else if (responseText.contains("larger chunks of garbage out of the waterway")) {
          Preferences.decrement("dinseyFilthLevel", 5, 0);
          QuestDatabase.setQuestProgress(Quest.FISH_TRASH, "step1");
        }
        break;

      case AdventurePool.TOXIC_TEACUPS:
        if (responseText.contains("pretend to be having a good time")) {
          Preferences.increment("dinseyFunProgress", 1);
        } else if (responseText.contains(
            "surrounding crowd seems to be pretty excited about the ride")) {
          Preferences.setInteger("dinseyFunProgress", 15);
          QuestDatabase.setQuestProgress(Quest.ZIPPITY_DOO_DAH, "step2");
        }
        break;

      case AdventurePool.LIQUID_WASTE_SLUICE:
        if (responseText.contains("probably not unacceptably racist anymore")) {
          Preferences.setInteger("dinseySocialJusticeIIProgress", 15);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, "step1");
        } else if (QuestDatabase.isQuestLaterThan(
            Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED)) {
          Preferences.increment("dinseySocialJusticeIIProgress", 1);
        }
        break;

      case AdventurePool.ICE_HOTEL:
      case AdventurePool.VYKEA:
      case AdventurePool.ICE_HOLE:
        if (responseText.contains("you should take it back to Walford!")) {
          Preferences.setInteger("walfordBucketProgress", 100);
          QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
          break;
        }
        Matcher WalfordMatcher = QuestManager.WALFORD_PATTERN.matcher(responseText);
        while (WalfordMatcher.find()) {
          Preferences.increment(
              "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
          if (Preferences.getInteger("walfordBucketProgress") >= 100) {
            QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
          }
        }
        break;

      case AdventurePool.SUPER_VILLAIN_LAIR:
        if (monsterName.equals("Villainous Minion")) {
          Preferences.increment("_villainLairProgress");
        } else if (monsterName.equals("Villainous Villain")) {
          Preferences.setInteger("_villainLairProgress", 999);
          Preferences.increment("bondVillainsDefeated");
        }
        break;

      case AdventurePool.FUNGUS_PLAINS:
      case AdventurePool.HEROS_FIELD:
      case AdventurePool.VANYAS_CASTLE:
      case AdventurePool.MEGALO_CITY:
        Preferences.increment("8BitBonusTurns");
        break;
    }

    // Can get a message about a ghost if wearing a Proton Accelerator Pack,
    // but if you got on the turn you busted a ghost, it is a false alarm.
    //
    if (!ghostBusted && KoLCharacter.hasEquipped(ItemPool.get(ItemPool.PROTON_ACCELERATOR, 1))) {
      Matcher ParanormalMatcher = QuestManager.PARANORMAL_PATTERN.matcher(responseText);
      String ghostLocation = null;
      while (ParanormalMatcher.find()) {
        String location = ParanormalMatcher.group(1);
        // Locations don't exactly match location name or quest log entries, so make them
        if (location.contains("Overgrown Lot")) {
          ghostLocation = "The Overgrown Lot";
        } else if (location.contains("Skeleton Store")) {
          ghostLocation = "The Skeleton Store";
        } else if (location.contains("Madness Bakery")) {
          ghostLocation = "Madness Bakery";
        } else if (location.contains("Spooky Forest")) {
          ghostLocation = "The Spooky Forest";
        } else if (location.contains("Kitchen")) {
          ghostLocation = "The Haunted Kitchen";
        } else if (location.contains("Knob Treasury")) {
          ghostLocation = "Cobb's Knob Treasury";
        } else if (location.contains("Conservatory")) {
          ghostLocation = "The Haunted Conservatory";
        } else if (location.contains("Landfill")) {
          ghostLocation = "The Old Landfill";
        } else if (location.contains("Icy Peak")) {
          ghostLocation = "The Icy Peak";
        } else if (location.contains("Smut Orc Logging Camp")) {
          ghostLocation = "The Smut Orc Logging Camp";
        } else if (location.contains("Gallery")) {
          ghostLocation = "The Haunted Gallery";
        } else if (location.contains("Palindome")) {
          ghostLocation = "Inside the Palindome";
        } else if (location.contains("Wine Cellar")) {
          ghostLocation = "The Haunted Wine Cellar";
        }
      }
      if (ghostLocation == null) {
        if (responseText.contains(
            "The walkie-talkie on your proton accelerator crackles to life")) {
          // Work around KoL bug. Fetch from quest log
          (new GenericRequest("questlog.php?which=1")).run();
          ghostLocation = Preferences.getString("ghostLocation");
        }
      }
      if (ghostLocation != null) {
        QuestDatabase.setQuestProgress(Quest.GHOST, QuestDatabase.STARTED);
        Preferences.setString("ghostLocation", ghostLocation);
        Preferences.setInteger("nextParanormalActivity", KoLCharacter.getTurnsPlayed() + 51);
        String message = "Paranormal activity reported at " + ghostLocation + ".";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
    }
  }

  /**
   * After we lose a fight, some quests may need to be updated. Centralize handling for it here.
   *
   * @param responseText The text from (at least) the losing round of the fight
   * @param monster The monster which beat us up.
   */
  private static final Pattern CYRUS_PATTERN =
      Pattern.compile("you remember him getting ([^.]*?)\\.");

  public static void updateQuestFightLost(String responseText, String monsterName) {
    switch (monsterName) {
      case "menacing thug" -> QuestDatabase.setQuestProgress(Quest.NEMESIS, "step18");
      case "Mob Penguin hitman" -> QuestDatabase.setQuestProgress(Quest.NEMESIS, "step20");
      case "Naughty Sorceress (3)" -> QuestDatabase.setQuestProgress(Quest.FINAL, "step12");
      case "hunting seal",
          "turtle trapper",
          "evil spaghetti cult assassin",
          "b&eacute;arnaise zombie",
          "flock of seagulls",
          "mariachi bandolero" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step22");
      }
      case "Argarggagarg the Dire Hellseal",
          "Safari Jack, Small-Game Hunter",
          "Yakisoba the Executioner",
          "Heimandatz, Nacho Golem",
          "Jocko Homo",
          "The Mariachi With No Name" -> {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step24");
      }
      case "mother hellseal" -> Preferences.decrement("_sealScreeches", 1, 0);
      case "Cyrus the Virus" -> {
        QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, "step2");
        Matcher matcher = CYRUS_PATTERN.matcher(responseText);
        if (matcher.find()) {
          QuestManager.updateCyrusAdjective(matcher.group(1));
        }
      }
      case "Travoltron" -> Preferences.setBoolean("_infernoDiscoVisited", false);
      case "Source Agent" -> Preferences.decrement("sourceAgentsDefeated", 1, 0);
    }
  }

  public static void updateCyrusAdjective(int itemId) {
    String adjective;
    switch (itemId) {
      case ItemPool.CA_BASE_PAIR:
        adjective = "stronger";
        break;
      case ItemPool.CG_BASE_PAIR:
        adjective = "smarter";
        break;
      case ItemPool.CT_BASE_PAIR:
        adjective = "more attractive";
        break;
      case ItemPool.AG_BASE_PAIR:
        adjective = "faster";
        break;
      case ItemPool.AT_BASE_PAIR:
        adjective = "more aggressive";
        break;
      case ItemPool.GT_BASE_PAIR:
        adjective = "more resilient";
        break;
      default:
        return;
    }
    QuestManager.updateCyrusAdjective(adjective);
  }

  public static void updateCyrusAdjective(String adjective) {
    String adjectives = Preferences.getString("cyrusAdjectives");
    if (adjectives.contains(adjective)) {
      return;
    } else if (adjectives.length() == 0) {
      adjectives = adjective;
    } else {
      adjectives = adjectives + "," + adjective;
    }
    Preferences.setString("cyrusAdjectives", adjectives);
  }

  /**
   * After we start a fight, some quests may need to be updated. Centralize handling for it here.
   *
   * @param responseText The text from (at least) the first round of the fight
   * @param monsterName The monster
   */
  public static void updateQuestFightStarted(final String responseText, final String monsterName) {
    if (EncounterManager.ignoreSpecialMonsters) {
      return;
    }

    switch (monsterName) {
      case "Gorgolok, the Infernal Seal (Volcanic Cave)":
      case "Stella, the Turtle Poacher (Volcanic Cave)":
      case "Spaghetti Elemental (Volcanic Cave)":
      case "Lumpy, the Sinister Sauceblob (Volcanic Cave)":
      case "Spirit of New Wave (Volcanic Cave)":
      case "Somerset Lopez, Dread Mariachi (Volcanic Cave)":
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step28");
        break;
      case "Cake Lord":
        QuestDatabase.setQuestProgress(Quest.ARMORER, "step2");
        break;
      case "GNG-3-R":
        if (EquipmentManager.discardEquipment(ItemPool.get(ItemPool.GINGERSERVO)) == Slot.NONE) {
          // Remove it from equipment if it is equipped, otherwise remove it from inventory
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERSERVO, -1));
        }
        break;
      case "X-32-F Combat Training Snowman":
        int snowParts = -2;
        Matcher snowmanMatcher = SNOWMAN_PATTERN.matcher(responseText);
        while (snowmanMatcher.find()) {
          snowParts++;
        }
        Preferences.setInteger("_snojoParts", snowParts);
        break;
      case "angry ghost":
      case "annoyed snake":
      case "government bureaucrat":
      case "terrible mutant":
      case "slime blob":
        Preferences.increment("_voteFreeFights", 1, 3, false);
        Preferences.setInteger("lastVoteMonsterTurn", KoLCharacter.getTurnsPlayed());
        Preferences.setString("_voteMonster", monsterName);
        TurnCounter.stopCounting("Vote Monster");
        VoteMonsterManager.checkCounter();
        break;
    }
  }

  public static void updateQuestItemUsed(final int itemId, final String responseText) {
    switch (itemId) {
      case ItemPool.FINGERNAIL_CLIPPERS:
        if (responseText.contains("little sliver of something fingernail-like")) {
          Preferences.increment("fingernailsClipped", 1);
          if (Preferences.getInteger("fingernailsClipped") >= 23) {
            QuestDatabase.setQuestProgress(Quest.CLIPPER, "step1");
          }
        }
        break;

      case ItemPool.DINSEY_REFRESHMENTS:
        if (responseText.contains("realize that the box of refreshments is empty")
            || responseText.contains("box of snacks is empty")) {
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, "step1");
          Preferences.setInteger("dinseyTouristsFed", 30);
        } else if (responseText.contains("hand out snacks to your opponent")) {
          int count = 1;
          if (responseText.contains("and the tourist in front")) {
            count++;
          } else {
            Matcher touristMatcher = QuestManager.TOURIST_PATTERN.matcher(responseText);
            if (touristMatcher.find()) {
              count += StringUtilities.parseInt(touristMatcher.group(1));
            }
          }
          Preferences.increment("dinseyTouristsFed", count, 30, false);
        }
        break;
    }
  }

  public static void updateQuestItemEquipped(final int itemId) {
    switch (itemId) {
      case ItemPool.GORE_BUCKET -> QuestDatabase.setQuestIfBetter(Quest.GORE, "step1");
      case ItemPool.MINI_CASSETTE_RECORDER -> QuestDatabase.setQuestIfBetter(
          Quest.JUNGLE_PUN, "step1");
      case ItemPool.GPS_WATCH -> QuestDatabase.setQuestIfBetter(Quest.OUT_OF_ORDER, "step1");
      case ItemPool.TRASH_NET -> QuestDatabase.setQuestIfBetter(Quest.FISH_TRASH, "step1");
      case ItemPool.LUBE_SHOES -> QuestDatabase.setQuestIfBetter(Quest.SUPER_LUBER, "step1");
      case ItemPool.MASCOT_MASK -> QuestDatabase.setQuestIfBetter(Quest.ZIPPITY_DOO_DAH, "step1");
      case ItemPool.WALFORDS_BUCKET -> QuestDatabase.setQuestIfBetter(Quest.BUCKET, "step1");
    }
  }

  public static final Pattern SPACEGATE_PLANET_PATTERN =
      Pattern.compile("<td>Current planet: Planet Name: ([^<]+)<br>");
  public static final Pattern SPACEGATE_COORDINATES_PATTERN =
      Pattern.compile("<br>Coordinates: ([^<]+)<br>");
  public static final Pattern SPACEGATE_HAZARDS_PATTERN =
      Pattern.compile("<br><p>Environmental Hazards:<[Bb]r>(.*)<br>Plant Life:", Pattern.DOTALL);
  public static final Pattern SPACEGATE_PLANT_LIFE_PATTERN =
      Pattern.compile(
          "<br>Plant Life: (?:<font color=\\w+>)?([^<]+)(?:</font>)? ?(?:<font color=\\w+>(\\(hostile\\))</font>)?<br>");
  public static final Pattern SPACEGATE_ANIMAL_LIFE_PATTERN =
      Pattern.compile(
          "<br>Animal Life: (?:<font color=\\w+>)?([^<]+)(?:</font>)? ?(?:<font color=\\w+>(\\(hostile\\))</font>)?<br>");
  public static final Pattern SPACEGATE_INTELLIGENT_LIFE_PATTERN =
      Pattern.compile(
          "<br>Intelligent Life: (?:<font color=\\w+>)?([^<]+) ?(?:</font>)?(?:<font color=\\w+>(\\(hostile\\))</font>)?<br>");
  public static final Pattern SPACEGATE_SPANT_PATTERN = Pattern.compile("<b>Spant</b>");
  public static final Pattern SPACEGATE_MURDERBOT_PATTERN = Pattern.compile("<b>Murderbot</b>");
  public static final Pattern SPACEGATE_RUINS_PATTERN =
      Pattern.compile("<br>ALERT: ANCIENT RUINS DETECTED<br>");
  public static final Pattern SPACEGATE_TURNS_PATTERN =
      Pattern.compile("<p>Spacegate Energy remaining: <b><font size=\\+2>(\\d+) </font>");

  public record Hazard(String terminal, String adventure, String gear) {}

  public static final Hazard[] HAZARDS = {
    new Hazard("toxic atmosphere", "Toxic environment", "filter helmet"),
    new Hazard("high gravity", "Extremely high gravity", "exo-servo leg braces"),
    new Hazard("irradiated", "High radiation levels", "rad cloak"),
    new Hazard("magnetic storms", "High levels of magnetic interference", "gate transceiver"),
    new Hazard("high winds", "Intense winds", "high-friction boots"),
  };

  public static void parseSpacegateAdventure(final String text) {
    // If we've already parsed the hazards, nothing to do here.
    if (!Preferences.getString("_spacegateHazards").equals("")) {
      return;
    }
    // Otherwise, this is from a portable spacegate.
    // Parse hazards and needed equipment
    Set<String> hazards = new HashSet<>();
    Set<String> gear = new HashSet<>();

    for (Hazard hazard : HAZARDS) {
      if (text.contains(hazard.adventure)) {
        hazards.add(hazard.terminal);
        gear.add(hazard.gear);
      }
    }
    Preferences.setString("_spacegateHazards", String.join("|", hazards));
    Preferences.setString("_spacegateGear", String.join("|", gear));
  }

  public static void parseSpacegateTerminal(final String text, final boolean print) {
    if (!text.contains("Spacegate Terminal")) {
      return;
    }

    Matcher m = QuestManager.SPACEGATE_PLANET_PATTERN.matcher(text);
    String name = m.find() ? m.group(1).trim() : "";
    Preferences.setString("_spacegatePlanetName", name);
    if (print) {
      RequestLogger.updateSessionLog("Planet: " + name);
    }

    m = QuestManager.SPACEGATE_COORDINATES_PATTERN.matcher(text);
    String coordinates = m.find() ? m.group(1).trim() : "";
    Preferences.setString("_spacegateCoordinates", coordinates);
    int index = coordinates.length() > 0 ? (coordinates.charAt(0) - 'A') : 0;
    Preferences.setInteger("_spacegatePlanetIndex", Math.max(0, Math.min(25, index)));
    if (print) {
      RequestLogger.updateSessionLog("Coordinates: " + coordinates);
    }

    m = QuestManager.SPACEGATE_HAZARDS_PATTERN.matcher(text);
    String hazards = m.find() ? m.group(1).trim() : "";
    hazards = StringUtilities.globalStringDelete(hazards, "&nbsp;");
    hazards = StringUtilities.globalStringReplace(hazards, "<br>", "|").trim();
    Preferences.setString("_spacegateHazards", hazards);
    if (print) {
      RequestLogger.updateSessionLog("Hazards: " + hazards);
    }

    var terminalHazards = hazards;
    String gear =
        Arrays.stream(HAZARDS)
            .filter(h -> terminalHazards.contains(h.terminal))
            .map(h -> h.gear)
            .collect(Collectors.joining("|"));

    Preferences.setString("_spacegateGear", gear);
    if (print) {
      RequestLogger.updateSessionLog("Gear: " + gear);
    }

    m = QuestManager.SPACEGATE_PLANT_LIFE_PATTERN.matcher(text);
    String plants =
        m.find() ? m.group(1).trim() + (m.group(2) == null ? "" : (" " + m.group(2))) : "none";
    Preferences.setString("_spacegatePlantLife", plants);
    if (print) {
      RequestLogger.updateSessionLog("Plant Life: " + plants);
    }

    m = QuestManager.SPACEGATE_ANIMAL_LIFE_PATTERN.matcher(text);
    String animals =
        m.find() ? m.group(1).trim() + (m.group(2) == null ? "" : (" " + m.group(2))) : "none";
    Preferences.setString("_spacegateAnimalLife", animals);
    if (print) {
      RequestLogger.updateSessionLog("Animal Life: " + animals);
    }

    m = QuestManager.SPACEGATE_INTELLIGENT_LIFE_PATTERN.matcher(text);
    String intelligent =
        m.find() ? m.group(1).trim() + (m.group(2) == null ? "" : (" " + m.group(2))) : "none";
    Preferences.setString("_spacegateIntelligentLife", intelligent);
    if (print) {
      RequestLogger.updateSessionLog("Intelligent Life: " + intelligent);
    }

    m = QuestManager.SPACEGATE_SPANT_PATTERN.matcher(text);
    boolean spants = m.find();
    Preferences.setBoolean("_spacegateSpant", spants);
    if (print) {
      RequestLogger.updateSessionLog("Spant chemical signature detected: " + spants);
    }

    m = QuestManager.SPACEGATE_MURDERBOT_PATTERN.matcher(text);
    boolean murderbots = m.find();
    Preferences.setBoolean("_spacegateMurderbot", murderbots);
    if (print) {
      RequestLogger.updateSessionLog("Murderbots frequencies detected: " + murderbots);
    }

    m = QuestManager.SPACEGATE_RUINS_PATTERN.matcher(text);
    boolean ruins = m.find();
    Preferences.setBoolean("_spacegateRuins", ruins);
    if (print) {
      RequestLogger.updateSessionLog("Ancient ruins detected: " + ruins);
    }

    m = QuestManager.SPACEGATE_TURNS_PATTERN.matcher(text);
    String turns = m.find() ? m.group(1) : "0";
    Preferences.setString("_spacegateTurnsLeft", turns);
    if (print) {
      RequestLogger.updateSessionLog("Spacegate turns left: " + turns);
    }
  }
}
