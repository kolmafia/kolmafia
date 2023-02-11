package net.sourceforge.kolmafia.request;

import java.util.TreeSet;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.SpadingManager;
import net.sourceforge.kolmafia.session.TowerDoorManager;
import net.sourceforge.kolmafia.session.VoteMonsterManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PlaceRequest extends GenericRequest {
  public static TreeSet<String> places = new TreeSet<>();
  public boolean followRedirects = false;

  private String place = null;
  private String action = null;

  public PlaceRequest() {
    super("place.php");
  }

  public PlaceRequest(final String place) {
    this();
    this.place = place;
    this.addFormField("whichplace", place);
  }

  public PlaceRequest(final String place, final String action) {
    this(place);
    this.action = action;
    this.addFormField("action", action);
  }

  public PlaceRequest(final String place, final String action, final boolean followRedirects) {
    this(place, action);
    this.followRedirects = followRedirects;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return this.followRedirects;
  }

  @Override
  public int getAdventuresUsed() {
    return PlaceRequest.getAdventuresUsed(this.getURLString());
  }

  public static int getAdventuresUsed(final String urlString) {
    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return 0;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      action = "";
    }

    return switch (place) {
      case "bugbearship" -> action.equals("bb_bridge") ? 1 : 0;
      case "campaway" -> action.startsWith("campaway_tent")
              && KoLCharacter.freeRestsRemaining() == 0
          ? 1
          : 0;
      case "chateau" -> {
        if (action.equals("chateau_painting")) {
          yield (Preferences.getBoolean("_chateauMonsterFought") ? 0 : 1);
        }
        if (action.startsWith("chateau_rest") || action.startsWith("cheateau_rest")) {
          yield (KoLCharacter.freeRestsRemaining() == 0 ? 1 : 0);
        }
        yield 0;
      }
      case "falloutshelter" -> action.equals("vault1") ? 1 : 0;
      case "ioty2014_wolf" -> action.equals("wolf_houserun") ? 3 : 0;
      case "manor4" -> action.equals("manor4_chamberboss") ? 1 : 0;
      case "nemesiscave" -> action.equals("nmcave_boss") ? 1 : 0;
      case "nstower" -> switch (action) {
        case "ns_01_crowd1",
            "ns_01_crowd2",
            "ns_01_crowd3",
            // Wall of Skin
            "ns_05_monster1",
            // Wall of Meat
            "ns_06_monster2",
            // Wall of Bones
            "ns_07_monster3",
            // Mirror
            "ns_08_monster4",
            // Your Shadow
            "ns_09_monster5",
            // Her Naughtiness
            "ns_10_sorcfight" -> 1;
        default -> 0;
      };
      case "pyramid" -> action.startsWith("pyramid_state")
          ? PyramidRequest.lowerChamberTurnsUsed()
          : 0;
      default -> 0;
    };
  }

  @Override
  public void processResults() {
    PlaceRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      action = "";
    }

    switch (place) {
      case "arcade":
        ArcadeRequest.parseResponse(urlString, responseText);
        break;
      case "campaway":
        CampAwayRequest.parseResponse(urlString, responseText);
        break;
      case "chateau":
        ChateauRequest.parseResponse(urlString, responseText);
        break;
      case "crimbo16m":
        // A Meditation Mat
        break;
      case "crimbo22":
        if (action.equals("crimbo22_engine") || action.equals("c22_locobox")) {
          // This redirects to a fight until you have defeated the boss
          // If we are here now, we have done that.
          //
          // You've already defeated the Trainbot boss. There's nothing else of interest in that
          // locomotive. Not even an explanation for why the train is still running!
          Preferences.setBoolean("superconductorDefeated", true);
        }
        break;
      case "desertbeach":
        if ("db_nukehouse".equals(action)) {
          if (responseText.contains("anticheese")) {
            Preferences.setInteger("lastAnticheeseDay", KoLCharacter.getCurrentDays());
          }
        }
        break;
      case "drip":
        // You can't enter The Drip without wearing one of those harnesses Jeremy told you about.
        if (responseText.contains("otherimages/drip/hall.gif")) {
          Preferences.setBoolean("drippingHallUnlocked", true);
        }
        break;
      case "dripfacility":
        if (action.equals("drip_jeremy")) {
          // You show Jeremy the big snail shell you found.
          if (responseText.contains("You show Jeremy the big snail shell you found")) {
            Preferences.setBoolean("drippyShieldUnlocked", true);
            ResultProcessor.removeItem(ItemPool.DRIPPY_SNAIL_SHELL);
          }
          // Oooh, that big fingernail, that's interesting.
          // I'll send it up to the Armory right away.
          // In the meantime, our scouts have cleared the way to another section of the Drip.
          // There's some kind of big building there. Go check it out, would you?
          if (responseText.contains("Oooh, that big fingernail, that's interesting")) {
            Preferences.setBoolean("drippingHallUnlocked", true);
          }
        }
        break;
      case "falloutshelter":
        FalloutShelterRequest.parseResponse(urlString, responseText);
        break;
      case "forestvillage":
        if (action.startsWith("fv_untinker")) {
          UntinkerRequest.parseResponse(urlString, responseText);
        }
        break;
      case "junggate_1",
          "junggate_2",
          "junggate_3",
          "junggate_4",
          "junggate_5",
          "junggate_6",
          "junggate_7":
        UseItemRequest.parseConsumption(responseText, false);
        break;
      case "kgb":
        KGBRequest.parseResponse(urlString, responseText);
        break;
      case "knoll_friendly":
        KnollRequest.parseResponse(urlString, responseText);
        break;
      case "manor1":
        if (action.equals("manor1_ladys")) {
          if (responseText.contains("ghost of a necklace")) {
            ResultProcessor.removeItem(ItemPool.SPOOKYRAVEN_NECKLACE);
          }
        }
        break;
      case "manor2":
        if (action.equals("manor2_ladys")) {
          // Lady Spookyraven's ghostly eyes light up at the sight of her dancing
          // finery. She grabs it from you and excitedly shouts "Meet me in the
          // ballroom in five minutes!" as she darts through the wall.

          if (responseText.contains("She grabs it from you")) {
            ResultProcessor.removeItem(ItemPool.POWDER_PUFF);
            ResultProcessor.removeItem(ItemPool.FINEST_GOWN);
            ResultProcessor.removeItem(ItemPool.DANCING_SHOES);
          }
        }
        break;
      case "manor4":
        if (action.startsWith("manor4_chamberwall")) {
          // You mix the mortar-dissolving ingredients
          // into a nasty-smelling paste, and smear it
          // all over the brickwork with a mortar. Smoke
          // begins to pour from the cracks between
          // bricks as the solution does its work. The
          // wall collapses, revealing an eerily-lit
          // chamber beyond.
          if (responseText.contains("The wall collapses")) {
            ResultProcessor.processItem(ItemPool.LOOSENING_POWDER, -1);
            ResultProcessor.processItem(ItemPool.POWDERED_CASTOREUM, -1);
            ResultProcessor.processItem(ItemPool.DRAIN_DISSOLVER, -1);
            ResultProcessor.processItem(ItemPool.TRIPLE_DISTILLED_TURPENTINE, -1);
            ResultProcessor.processItem(ItemPool.DETARTRATED_ANHYDROUS_SUBLICALC, -1);
            ResultProcessor.processItem(ItemPool.TRIATOMACEOUS_DUST, -1);
            QuestDatabase.setQuestProgress(Quest.MANOR, "step3");
          }
          // You shake up the wine bomb and hurl it at the
          // masonry. The ensuing blast leaves a giant
          // jagged hole in the wall, leading into an
          // eerily lit chamber beyond.
          else if (responseText.contains("a giant jagged hole in the wall")) {
            ResultProcessor.processItem(ItemPool.WINE_BOMB, -1);
            QuestDatabase.setQuestProgress(Quest.MANOR, "step3");
          }
        }
        break;
      case "mountains":
        if (responseText.contains("chateau")) {
          Preferences.setBoolean("chateauAvailable", true);
        }
        if (responseText.contains("snojo")) {
          Preferences.setBoolean("snojoAvailable", true);
        }
        if (responseText.contains("gingerbreadcity")
            && !Preferences.getBoolean("gingerbreadCityAvailable")) {
          Preferences.setBoolean("_gingerbreadCityToday", true);
        }
        if (responseText.contains("spacegate")) {
          Preferences.setBoolean("spacegateAlways", true);
        }
        break;
      case "nstower":
        SorceressLairManager.parseTowerResponse(action, responseText);
        break;
      case "nstower_door", "nstower_doorlowkey":
        TowerDoorManager.parseTowerDoorResponse(action, responseText);
        break;
      case "orc_chasm":
        OrcChasmRequest.parseResponse(urlString, responseText);
        break;
      case "rabbithole":
        RabbitHoleRequest.parseResponse(urlString, responseText);
        break;
      case "scrapheap":
        ScrapheapRequest.parseResponse(urlString, responseText);
        break;
      case "spacegate":
        if (action.equals("sg_tech") && responseText.contains("You turn in")) {
          ResultProcessor.removeAllItems(ItemPool.ALIEN_ROCK_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.ALIEN_GEMSTONE);
          ResultProcessor.removeAllItems(ItemPool.ALIEN_PLANT_FIBERS);
          ResultProcessor.removeAllItems(ItemPool.ALIEN_PLANT_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.COMPLEX_ALIEN_PLANT_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.FASCINATING_ALIEN_PLANT_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.ALIEN_TOENAILS);
          ResultProcessor.removeAllItems(ItemPool.ALIEN_ZOOLOGICAL_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.COMPLEX_ALIEN_ZOOLOGICAL_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.FASCINATING_ALIEN_ZOOLOGICAL_SAMPLE);
          ResultProcessor.removeAllItems(ItemPool.MURDERBOT_MEMORY_CHIP);
          ResultProcessor.removeAllItems(ItemPool.SPANT_EGG_CASING);
        }
        break;
      case "spelunky":
        SpelunkyRequest.parseResponse(urlString, responseText);
        break;
      case "town_right":
        if ("townright_vote".equals(action)) {
          VoteMonsterManager.parseBooth(responseText);
        }
        break;
      case "town_wrong":
        if (action.equals("townwrong_artist_quest") || action.equals("townwrong_artist_noquest")) {
          ArtistRequest.parseResponse(urlString, responseText);
        }
        break;
      case "twitch":
        // The Time-Twitching Tower has faded back into the
        // swirling mists of the temporal ether. Or maybe you
        // only thought it was there in the first place because
        // you were huffing the temporal ether.

        QuestManager.handleTimeTower(!responseText.contains("temporal ether"));

        if (action.equals("twitch_bank")
            && responseText.contains("Thanks fer bringin' the money back")) {
          ResultProcessor.removeItem(ItemPool.BIG_BAG_OF_MONEY);
        }
        break;
      case "woods":
        Preferences.setBoolean("getawayCampsiteUnlocked", responseText.contains("campaway"));
        break;
      case "wildfire_camp":
        WildfireCampRequest.parseResponse(urlString, responseText);
        break;
      default:
        if (place.startsWith("batman")) {
          BatFellowRequest.parseResponse(urlString, responseText);
        }
    }

    SpadingManager.processPlace(urlString, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php")) {
      return false;
    }

    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return true;
    }

    if (place.equals("spelunky")) {
      return SpelunkyRequest.registerRequest(urlString);
    }

    if (place.startsWith("batman")) {
      return BatFellowRequest.registerRequest(place, urlString);
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      action = "";
    }

    String message = null;
    boolean turns = false;
    boolean compact = false;

    switch (place) {
      case "8bit" -> {
        switch (action) {
          case "8treasure" -> {
            message = "Visiting The Treasure House";
          }
          case "8rift" -> {
            message = "Entering the Shadow Rift via the 8-Bit Realm";
            Preferences.setString("shadowRiftIngress", "8bit");
          }
        }
      }
      case "airport_hot" -> {
        message =
            switch (action) {
              case "airport4_zone1" -> "Visiting The Towering Inferno Discotheque";
              case "airport4_questhub" -> "Visiting The WLF Bunker";
              default -> null;
            };
      }
      case "airport_sleaze" -> {
        message =
            switch (action) {
              case "airport1_npc1" -> "Talking to Buff Jimmy";
              case "airport1_npc2" -> "Talking to Taco Dan";
              case "airport1_npc3" -> "Talking to Broden";
              default -> null;
            };
      }
      case "airport_spooky" -> {
        if (action.equals("airport2_radio")) {
          message = "Using the radio on Conspiracy Island";
        }
      }
      case "airport_spooky_bunker" -> {
        switch (action) {
          case "si_shop1locked", "si_shop2locked", "si_shop3locked" -> {
            return true;
          }
          case "si_controlpanel" -> {
            message = "Manipulating the Control Panel in the Conspiracy Island bunker";
          }
        }
      }
      case "airport_stench" -> {
        message =
            switch (action) {
              case "airport3_tunnels" -> "Visiting the Maintenance Tunnels";
              case "airport3_kiosk" -> "Visiting the Employee Assignment Kiosk";
              default -> null;
            };
      }
      case "beanstalk" -> {
        if (action.equals("stalk-rift")) {
          message = "Entering the Shadow Rift via the The Misspelled Cemetary";
          Preferences.setString("shadowRiftIngress", "beanstalk");
        }
      }
      case "bugbearship" -> {
        if (action.equals("bb_bridge")) {
          message = "Bugbear Ship Bridge";
          turns = true;
        }
      }
      case "canadia" -> {
        message =
            switch (action) {
              case "lc_mcd" -> "Visiting the Super-Secret Canadian Mind Control Device";
              case "lc_marty" -> "Talking to Marty";
              default -> null;
            };
      }
      case "cemetery" -> {
        switch (action) {
          case "cem_advtomb" -> {
            message = "The Unknown Tomb";
            turns = true;
          }
          case "cem_shadowrift" -> {
            message = "Entering the Shadow Rift via the The Misspelled Cemetary";
            Preferences.setString("shadowRiftIngress", "cemetery");
          }
        }
      }
      case "crimbo2016" -> {
        message =
            switch (action) {
              case "crimbo16_trailer" -> "Visiting Uncle Crimbo's Mobile Home";
              case "crimbo16_tammy" -> "Visiting Tammy's Tent";
              case "crimbo16_guy2" -> "Visiting A Ninja Snowman";
              case "crimbo16_guy2a" -> "Visiting An Elf Boot-Polisher";
              case "crimbo16_guy3" -> "Visiting A Hobo";
              case "crimbo16_guy3a" -> "Visiting An Elf Cook";
              case "crimbo16_guy4" -> "Visiting A Bugbear";
              case "crimbo16_guy4a" -> "Visiting An Elf Reindeerstler";
              case "crimbo16_guy5" -> "Visiting A Hippy";
              case "crimbo16_guy5a" -> "Visiting An Elf Bearddresser";
              case "crimbo16_guy6" -> "Visiting A Frat Boy";
              case "crimbo16_guy6a" -> "Visiting An Elf Haberdasher";
              default -> null;
            };
      }
      case "crimbo16m" -> {
        // A Meditation Mat
      }
      case "crimbo17_silentnight" -> {
        message =
            switch (action) {
              case "crimbo17_bossfight" -> "Mime-Head Building";
              case "crimbo17_warehouse" -> "The Warehouse";
              default -> null;
            };
      }
      case "crashsite" -> {
        if (action.equals("crash_ship")) {
          message = "Visiting the Crashed Spaceship";
        }
      }
      case "desertbeach" -> {
        switch (action) {
          case "db_gnasir" -> {
            message = "Talking to Gnasir";
          }
          case "db_nukehouse" -> {
            message = "Visiting the Ruined House";
            compact = true; // Part of Breakfast
          }
          case "db_pyramid1" -> {
            // message = "Visiting the Small Pyramid";
          }
          case "db_shadowrift" -> {
            message = "Entering the Shadow Rift via the Desert Beach";
            Preferences.setString("shadowRiftIngress", "desertbeach");
          }
        }
      }
      case "dinorf" -> {
        message =
            switch (action) {
              case "dinorf_hunter" -> "Visiting the Dino World Game Warden's Shed";
              case "dinorf_chaos" -> "Visiting the Dino World Visitor's Center";
              case "dinorf_owner" -> "Visiting the Dino World Owner's Trailer";
              default -> null;
            };
      }
      case "dripfacility" -> {
        switch (action) {
          case "" -> {
            // message = "Visiting The Drip Institute";
          }
          case "drip_jeremy" -> {
            message = "Talking to Jeremy Science";
          }
          case "drip_armory" -> {
            // Redirects to shop.php?whichshop=driparmory
          }
          case "drip_cafeteria" -> {
            // Redirects to shop.php?whichshop=dripcafeteria
          }
        }
      }
      case "exploathing" -> {
        if (action.equals("expl_council")) {
          message = "Visiting The Council";
        }
      }
      case "exploathing_beach" -> {
        if (action.equals("expl_gnasir")) {
          message = "Talking to Gnasir";
        } else if (action.equals("expl_pyramidpre")) {
          // message = "Visiting the Small Pyramid";
        }
      }
      case "exploathing_other" -> {}
      case "forestvillage" -> {
        switch (action) {
          case "fv_friar":
            // Don't log this
            return true;
          case "fv_untinker", "fv_untinker_quest":
            // Let UntinkerRequest claim this
            return false;
          case "fv_mystic":
            message = "Talking to the Crackpot Mystic";
            break;
          case "fv_scientist":
            message = "Visiting a Science Tent";
            break;
          case "fv_shadowrift":
            message = "Entering the Shadow Rift via the Forest Village";
            Preferences.setString("shadowRiftIngress", "forestvillage");
            break;
        }
      }
      case "giantcastle" -> {
        if (action.equals("castle_shadowrift")) {
          message = "Entering the Shadow Rift via The Castle in the Clouds in the Sky";
          Preferences.setString("shadowRiftIngress", "giantcastle");
        }
      }
      case "greygoo" -> {
        if (action.equals("goo_prism")) {
          message = "Visiting a Prism of Goo";
          turns = true;
        }
      }
      case "hiddencity" -> {
        if (action.equals("hc_shadowrift")) {
          message = "Entering the Shadow Rift via The Hidden City";
          Preferences.setString("shadowRiftIngress", "hiddencity");
        }
      }
      case "highlands" -> {
        if (action.equals("highlands_dude")) {
          message = "Talking to the Highland Lord";
        }
      }
      case "ioty2014_candy" -> {
        if (action.equals("witch_house")) {
          message = "Visiting the Candy Witch's House";
        }
      }
      case "ioty2014_rumple" -> {
        if (action.equals("workshop")) {
          message = "Visiting Rumplestiltskin's Workshop";
        }
      }
      case "kgb" -> {
        // Kremlin's Greatest Briefcase is a "place"
        if (action.equals("")) {
          message = "Examining Kremlin's Greatest Briefcase";
        }
      }
      case "manor1" -> {
        switch (action) {
          case "manor1lock_kitchen",
              "manor1lock_billiards",
              "manor1lock_library",
              "manor1lock_stairsup" -> {
            return true;
          }
          case "manor1_ladys" -> {
            message = "Talking to Lady Spookyraven";
          }
        }
      }
      case "manor2" -> {
        switch (action) {
          case "manor2lock_ballroom",
              "manor2lock_bathroom",
              "manor2lock_bedroom",
              "manor2lock_gallery",
              "manor2lock_stairsup" -> {
            return true;
          }
          case "manor2_ladys" -> {
            message = "Talking to Lady Spookyraven";
          }
        }
      }
      case "manor3" -> {
        switch (action) {
          case "manor3_ladys" -> message = "Talking to Lady Spookyraven";
          case "manor3_shadowrift" -> {
            message = "Entering the Shadow Rift via Spookyraven Manor, Third Floor";
            Preferences.setString("shadowRiftIngress", "manor3");
          }
        }
      }
      case "manor4" -> {
        if (action.equals("manor4_chamber")) {
          return true;
        }
        if (action.startsWith("manor4_chamberwall")) {
          message = "Inspecting the Suspicious Masonry";
        }
      }
      case "mclargehuge" -> {
        switch (action) {
          case "trappercabin" -> message = "Visiting the Trapper";
          case "cloudypeak" -> message = "Ascending the Mist-Shrouded Peak";
          case "mcl_shadowrift" -> {
            message = "Entering the Shadow Rift via Mt. McLargeHuge";
            Preferences.setString("shadowRiftIngress", "mclargehuge");
          }
        }
      }
      case "monorail" -> {
        switch (action) {
          case "monorail_lyle" -> message = "Visiting Lyle, LyleCo CEO";
          case "monorail_downtown" -> {
            message = "Train to Downtown";
            turns = true;
          }
        }
      }
      case "mountains" -> {
        message =
            switch (action) {
              case "mts_melvin" -> "Talking to Melvign the Gnome";
              case "mts_caveblocked" -> "Entering the Nemesis Cave";
              default -> null;
            };
      }
      case "nemesiscave" -> {
        switch (action) {
          case "nmcave_rubble" -> message = "Examining the rubble in the Nemesis Cave";
          case "nmcave_boss" -> {
            message = "Confronting your Nemesis";
            turns = true;
          }
        }
      }
      case "northpole" -> {
        message =
            switch (action) {
              case "np_bonfire" -> "Visiting the Bonfire";
              case "np_sauna" -> "Entering the Sauna";
              case "np_foodlab" -> "Entering the Food Lab";
              case "np_boozelab" -> "Entering the Nog Lab";
              case "np_spleenlab" -> "Entering the Chem Lab";
              case "np_toylab" -> "Entering the Gift Fabrication Lab";
              default -> null;
            };
      }
      case "orc_chasm" -> {
        if (action.startsWith("bridge") || action.equals("label1") || action.equals("label2")) {
          // Building the bridge. Do we need to log anything?
          return true;
        }
      }
      case "palindome" -> {
        switch (action) {
          case "pal_drlabel", "pal_droffice" -> {
            message = "Visiting Dr. Awkward's office";
            turns = true;
          }
          case "pal_mrlabel", "pal_mroffice" -> {
            message = "Visiting Mr. Alarm's office";
          }
        }
      }
      case "plains" -> {
        switch (action) {
          case "rift_scorch", "rift_light" -> {
            return true;
          }
          case "plains_shadowrift" -> {
            message = "Entering the Shadow Rift via The Nearby Plains";
            Preferences.setString("shadowRiftIngress", "plains");
          }
          case "garbage_grounds" -> {
            message = "Inspecting the Giant Pile of Coffee Grounds";
          }
          case "lutersgrave" -> {
            if (!InventoryManager.hasItem(ItemPool.CLANCY_LUTE)) {
              message = "The Luter's Grave";
              turns = true;
            }
          }
        }
      }
      case "pyramid" -> {
        switch (action) {
          case "pyramid_control" -> message = "Visiting the Pyramid Control Room";
          case "pyramid_shadowrift" -> {
            message = "Entering the Shadow Rift via The Ancient Buried Pyramid";
            Preferences.setString("shadowRiftIngress", "pyramid");
          }
        }
      }
      case "rabbithole" -> {
        if (action.equals("rabbithole_teaparty")) {
          message = "Visiting the Mad Tea Party";
        }
      }
      case "snojo" -> {
        if (action.equals("snojo_controller")) {
          message = "Visiting Snojo Control Console";
        }
      }
      case "spacegate" -> {
        message =
            switch (action) {
              case "sg_requisition" -> "Visiting Spacegate Equipment Requisition";
              case "sg_tech" -> "Visiting Spacegate R&D";
              case "sg_Terminal" -> "Visiting the Spacegate Terminal";
              case "sg_vaccinator" -> "Visiting the Spacegate Vaccination Machine";
              default -> null;
            };
      }
      case "spacegate_portable" -> {
        message = "Visiting your portable Spacegate";
      }
      case "speakeasy" -> {
        message =
            switch (action) {
              case "olivers_pooltable" -> "Visiting the Pool Table";
              case "olivers_sot" -> "Talking to the Milky-Eyed Sot";
                // case "olivers_piano" -> "Examining the Piano";
              case "olivers_sign" -> "Looking at the conspicuous plaque";
                // case "olivers_codetable" -> "Looking at the scratched-Up Table";
                // case "olivers_bouncer" -> "Talking to the  Bouncer";
              case "" -> "Visiting " + Preferences.getString("speakeasyName");
              default -> null;
            };
      }
      case "sea_oldman" -> {
        // place.php?whichplace=sea_oldman&action=oldman_oldman&preaction=pickreward&whichreward=6313[/code]
        if (action.equals("oldman_oldman")) {
          message = "Talking to the Old Man";
        }
      }
      case "thesea" -> {
        if (action.equals("thesea_left2")) {
          message = "Visiting the Swimmy Little Fishes and Such";
        }
      }
      case "town" -> {
        if (action.equals("town_oddjobs")) {
          message = "Visiting the Odd Jobs Board";
        }
      }
      case "town_market" -> {
        if (action.equals("town_bookmobile")) {
          message = "Visiting The Bookmobile";
        }
      }
      case "town_right" -> {
        switch (action) {
          case "town_horsery" -> {
            message = "Visiting The Horsery";
            compact = true; // Part of logging in
          }
          case "townright_vote" -> message = "Visiting The Voting Booth";
          case "townright_shadowrift" -> {
            message = "Entering the Shadow Rift via the Right Side of the Tracks";
            Preferences.setString("shadowRiftIngress", "town_right");
          }
        }
      }
      case "town_wrong" -> {
        message =
            switch (action) {
              case "townwrong_precinct" -> "Visiting the 11th Precinct Headquarters";
              case "townwrong_boxingdaycare" -> "Visiting the Boxing Daycare";
              default -> null;
            };
      }
      case "twitch" -> {
        message =
            switch (action) {
              case "twitch_votingbooth" -> "Visiting the Voting / Phone Booth";
              case "twitch_dancave1" -> "Visiting Caveman Dan's Cave";
              case "twitch_shoerepair" -> "Visiting the Shoe Repair Store";
              case "twitch_colosseum" -> "Visiting the Chariot-Racing Colosseum";
              case "twitch_survivors" -> "Visiting the Post-Apocalyptic Survivor Encampment";
              case "twitch_bank" -> "Visiting the Third Four-Fifths Bank of the West";
              case "twitch_boat2" -> "Visiting The Pinta";
              case "twitch_boat3" -> "Visiting The Santa Claus";
              default -> null;
            };
      }
      case "woods" -> {
        switch (action) {
          case "woods_emptybm" -> {
            // Visiting the Empty Black Market
            return true;
          }
          case "woods_smokesignals" -> {
            message = "Investigating the Smoke Signals";
          }
          case "woods_hippy" -> {
            message = "Talking to that Hippy";
          }
          case "woods_dakota_anim", "woods_dakota" -> {
            message = "Talking to Dakota Fanning";
          }
          case "woods_shadowrift" -> {
            message = "Entering the Shadow Rift via The Distant Woods";
            Preferences.setString("shadowRiftIngress", "woods");
          }
        }
      }

      case "airport",
          "arcade",
          "bathole",
          "chateau",
          "knoll_friendly",
          "nstower",
          "scrapheap",
          "wormwood",
          "wildfire_camp" -> {
        // It is not interesting to log simple visits to these
        // places. Other classes may claim specific actions.
        return action.equals("");
      }
      default -> {
        // Let any other "place" be claimed by other classes.
        return false;
      }
    }

    if (message == null) {
      // For the "places" we claim here, do not log the URL
      // of simple visits, but do log unclaimed actions.
      return action.equals("");
    }

    if (turns) {
      message = "[" + KoLAdventure.getAdventureCount() + "] " + message;
    }

    if (!compact) {
      RequestLogger.printLine();
    }
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }

  public static boolean unclaimedPlace(final String urlString) {
    // Claim all place.php?whichplace=xxx with no action=yyy

    if (!urlString.startsWith("place.php")) {
      return false;
    }

    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      return false;
    }

    // Save the unclaimed "place". It might be interesting to log
    // them en masse when you log out, just to see if there are any
    // that we want to handle differently.<

    PlaceRequest.places.add(place);

    return true;
  }

  public static void decorate(final String urlString, final StringBuffer buffer) {
    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      action = "";
    }

    switch (place) {
      case "forestvillage":
        // We decorate simple visits to the untinker and also
        // accepting his quest
        if (action.equals("fv_untinker") || urlString.contains("preaction=screwquest")) {
          UntinkerRequest.decorate(buffer);
        }
        break;
      case "manor1":
        if (action.equals("manor1_ladys")) {
          if (buffer.indexOf("ghost of a necklace") != -1) {
            RequestEditorKit.addAdventureAgainSection(
                buffer,
                "place.php?whichplace=manor2&action=manor2_ladys",
                "Talk to Lady Spookyraven on the Second Floor");
          }
        }
        break;
      case "rabbithole":
        RabbitHoleManager.decorateRabbitHole(buffer);
        break;
      case "town_right":
        if (action.equals("townright_vote")) {
          String pref = Preferences.getString("_voteMonster");
          if (pref.equals("")) {
            pref = "unknown";
          }
          String replace = "<br />(wanderer today is " + pref + ")</blockquote>";
          StringUtilities.singleStringReplace(buffer, "</blockquote>", replace);
        }
        break;
    }
  }
}
