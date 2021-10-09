package net.sourceforge.kolmafia.request;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
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
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PlaceRequest extends GenericRequest {
  public static TreeSet<String> places = new TreeSet<String>();
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
  public void processResults() {
    PlaceRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern VOTE_PATTERN =
      Pattern.compile(
          "initiatives: </b><div style='margin-left: 1em; color: blue'>(.*?)<br>(.*?)</div>");
  private static final Pattern VOTE_SPEECH_PATTERN =
      Pattern.compile("<b>Today's Leader: </b>(.*?)<br><blockquote>(.*?)</blockquote>");

  public static void parseResponse(final String urlString, final String responseText) {
    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      action = "";
    }

    if (place.equals("arcade")) {
      ArcadeRequest.parseResponse(urlString, responseText);
    } else if (place.startsWith("batman")) {
      BatFellowRequest.parseResponse(urlString, responseText);
    } else if (place.equals("campaway")) {
      CampAwayRequest.parseResponse(urlString, responseText);
    } else if (place.equals("chateau")) {
      ChateauRequest.parseResponse(urlString, responseText);
    } else if (place.equals("crimbo16m")) {
      // A Meditation Mat
    } else if (place.equals("desertbeach")) {
      if (action.equals("db_nukehouse")) {
        if (responseText.contains("anticheese")) {
          Preferences.setInteger("lastAnticheeseDay", KoLCharacter.getCurrentDays());
        }
      }
    } else if (place.equals("drip")) {
      // You can't enter The Drip without wearing one of those harnesses Jeremy told you about.
      if (responseText.contains("otherimages/drip/hall.gif")) {
        Preferences.setBoolean("drippingHallUnlocked", true);
      }
    } else if (place.equals("dripfacility")) {
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
    } else if (place.equals("falloutshelter")) {
      FalloutShelterRequest.parseResponse(urlString, responseText);
    } else if (place.equals("forestvillage")) {
      if (action.equals("fv_untinker")) {
        UntinkerRequest.parseResponse(urlString, responseText);
      }
    } else if (place.startsWith("junggate")) {
      UseItemRequest.parseConsumption(responseText, false);
    } else if (place.equals("kgb")) {
      KGBRequest.parseResponse(urlString, responseText);
    } else if (place.equals("knoll_friendly")) {
      KnollRequest.parseResponse(urlString, responseText);
    } else if (place.equals("manor1")) {
      if (action.equals("manor1_ladys")) {
        if (responseText.contains("ghost of a necklace")) {
          ResultProcessor.removeItem(ItemPool.SPOOKYRAVEN_NECKLACE);
        }
      }
    } else if (place.equals("manor2")) {
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
    } else if (place.equals("manor4")) {
      if (action.equals("manor4_chamberwall") || action.equals("manor4_chamberwalllabel")) {
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
    } else if (place.equals("mountains")) {
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
    } else if (place.equals("nstower")) {
      SorceressLairManager.parseTowerResponse(action, responseText);
    } else if (place.equals("nstower_door") || place.equals("nstower_doorlowkey")) {
      TowerDoorManager.parseTowerDoorResponse(action, responseText);
    } else if (place.equals("orc_chasm")) {
      OrcChasmRequest.parseResponse(urlString, responseText);
    } else if (place.equals("rabbithole")) {
      RabbitHoleRequest.parseResponse(urlString, responseText);
    } else if (place.equals("scrapheap")) {
      ScrapheapRequest.parseResponse(urlString, responseText);
    } else if (place.equals("spacegate")) {
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
    } else if (place.equals("spelunky")) {
      SpelunkyRequest.parseResponse(urlString, responseText);
    } else if (place.equals("town_right")) {
      Preferences.setBoolean("telegraphOfficeAvailable", responseText.contains("townright_ltt"));
      Preferences.setBoolean("horseryAvailable", responseText.contains("horsery.gif"));
      if (action.equals("townright_vote")) {
        if (responseText.contains("Today's Leader")) {
          Matcher matcher = PlaceRequest.VOTE_PATTERN.matcher(responseText);
          if (matcher.find()) {
            ModifierList modList = new ModifierList();
            ModifierList addModList =
                Modifiers.splitModifiers(Modifiers.parseModifier(matcher.group(1)));
            for (Modifier modifier : addModList) {
              modList.addToModifier(modifier);
            }
            addModList = Modifiers.splitModifiers(Modifiers.parseModifier(matcher.group(2)));
            for (Modifier modifier : addModList) {
              modList.addToModifier(modifier);
            }
            Preferences.setString("_voteModifier", modList.toString());
          }
          if (Preferences.getString("_voteMonster").equals("")) {
            String monster = null;
            matcher = PlaceRequest.VOTE_SPEECH_PATTERN.matcher(responseText);
            if (matcher.find()) {
              String party = matcher.group(1);
              String speech = matcher.group(2);
              if (party.contains("Pork Elf Historical Preservation Party")) {
                if (speech.contains("strict curtailing of unnatural modern technologies")) {
                  monster = "government bureaucrat";
                } else if (speech.contains("reintroduce Pork Elf DNA")) {
                  monster = "terrible mutant";
                } else if (speech.contains("kingdom-wide seance")) {
                  monster = "angry ghost";
                } else if (speech.contains("very interested in snakes")) {
                  monster = "annoyed snake";
                } else if (speech.contains("lots of magical lard")) {
                  monster = "slime blob";
                }
              } else if (party.contains("Clan Ventrilo")) {
                if (speech.contains("bringing this blessing to the entire population")) {
                  monster = "slime blob";
                } else if (speech.contains("see your deceased loved ones again")) {
                  monster = "angry ghost";
                } else if (speech.contains("stronger and more vigorous")) {
                  monster = "terrible mutant";
                } else if (speech.contains("implement healthcare reforms")) {
                  monster = "government bureaucrat";
                } else if (speech.contains("flavored drink in a tube")) {
                  monster = "annoyed snake";
                }
              } else if (party.contains("Bureau of Efficient Government")) {
                if (speech.contains("graveyards are a terribly inefficient use of space")) {
                  monster = "angry ghost";
                } else if (speech.contains("strictly enforced efficiency laws")) {
                  monster = "government bureaucrat";
                } else if (speech.contains(
                    "distribute all the medications for all known diseases ")) {
                  monster = "terrible mutant";
                } else if (speech.contains("introduce an influx of snakes")) {
                  monster = "annoyed snake";
                } else if (speech.contains("releasing ambulatory garbage-eating slimes")) {
                  monster = "slime blob";
                }
              } else if (party.contains("Scions of Ich'Xuul'kor")) {
                if (speech.contains("increase awareness of our really great god")) {
                  monster = "terrible mutant";
                } else if (speech.contains("hunt these evil people down")) {
                  monster = "government bureaucrat";
                } else if (speech.contains("sound of a great hissing")) {
                  monster = "annoyed snake";
                } else if (speech.contains("make things a little bit more like he's used to")) {
                  monster = "slime blob";
                } else if (speech.contains("kindness energy")) {
                  monster = "angry ghost";
                }
              } else if (party.contains("Extra-Terrific Party")) {
                if (speech.contains("wondrous chemical")) {
                  monster = "terrible mutant";
                } else if (speech.contains("comprehensive DNA harvesting program")) {
                  monster = "government bureaucrat";
                } else if (speech.contains("mining and refining processes begin")) {
                  monster = "slime blob";
                } else if (speech.contains("warp engines will not destabilize")) {
                  monster = "angry ghost";
                } else if (speech.contains("breeding pair of these delightful creatures")) {
                  monster = "annoyed snake";
                }
              }
            }
            if (monster != null) {
              Preferences.setString("_voteMonster", monster);
            }
          }
        }
      }
    } else if (place.equals("town_wrong")) {
      if (action.equals("townwrong_artist_quest") || action.equals("townwrong_artist_noquest")) {
        ArtistRequest.parseResponse(urlString, responseText);
      }
      Preferences.setBoolean("loveTunnelAvailable", responseText.contains("townwrong_tunnel"));
    } else if (place.equals("twitch")) {
      // The Time-Twitching Tower has faded back into the
      // swirling mists of the temporal ether. Or maybe you
      // only thought it was there in the first place because
      // you were huffing the temporal ether.

      QuestManager.handleTimeTower(!responseText.contains("temporal ether"));

      if (action.equals("twitch_bank")
          && responseText.contains("Thanks fer bringin' the money back")) {
        ResultProcessor.removeItem(ItemPool.BIG_BAG_OF_MONEY);
      }
    } else if (place.equals("woods")) {
      Preferences.setBoolean("getawayCampsiteUnlocked", responseText.contains("campaway"));
    } else if (place.equals("wildfire_camp")) {
      WildfireCampRequest.parseResponse(urlString, responseText);
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

    if (place.equals("airport_hot")) {
      if (action.equals("airport4_zone1")) {
        // Visiting The Towering Inferno Discotheque
        // This redirects to a choice.
        message = "Visiting The Towering Inferno Discotheque";
      }
      if (action.equals("airport4_questhub")) {
        // Visiting the The WLF Bunker
        // This redirects to a choice.
        message = "Visiting The WLF Bunker";
      }
    } else if (place.equals("airport_sleaze")) {
      if (action.equals("airport1_npc1")) {
        message = "Talking to Buff Jimmy";
      } else if (action.equals("airport1_npc2")) {
        message = "Talking to Taco Dan";
      } else if (action.equals("airport1_npc3")) {
        message = "Talking to Broden";
      }
    } else if (place.equals("airport_spooky")) {
      if (action.equals("airport2_radio")) {
        message = "Using the radio on Conspiracy Island";
      }
    } else if (place.equals("airport_spooky_bunker")) {
      if (action.equals("si_shop1locked")
          || action.equals("si_shop2locked")
          || action.equals("si_shop3locked")) {
        return true;
      }
      if (action.equals("si_controlpanel")) {
        message = "Manipulating the Control Panel in the Conspiracy Island bunker";
      }
    } else if (place.equals("airport_stench")) {
      if (action.equals("airport3_tunnels")) {
        message = "Visiting the Maintenance Tunnels";
      } else if (action.equals("airport3_kiosk")) {
        message = "Visiting the Employee Assignment Kiosk";
      }
    } else if (place.equals("canadia")) {
      if (action.equals("lc_mcd")) {
        message = "Visiting the Super-Secret Canadian Mind Control Device";
      } else if (action.equals("lc_marty")) {
        message = "Talking to Marty";
      }
    } else if (place.equals("cemetery")) {
      if (action.equals("cem_advtomb")) {
        message = "The Unknown Tomb";
        turns = true;
      }
    } else if (place.equals("crimbo2016")) {
      if (action.equals("crimbo16_trailer")) {
        message = "Visiting Uncle Crimbo's Mobile Home";
      } else if (action.equals("crimbo16_tammy")) {
        message = "Visiting Tammy's Tent";
      } else if (action.equals("crimbo16_guy2")) {
        message = "Visiting A Ninja Snowman";
      } else if (action.equals("crimbo16_guy2a")) {
        message = "Visiting An Elf Boot-Polisher";
      } else if (action.equals("crimbo16_guy3")) {
        message = "Visiting A Hobo";
      } else if (action.equals("crimbo16_guy3a")) {
        message = "Visiting An Elf Cook";
      } else if (action.equals("crimbo16_guy4")) {
        message = "Visiting A Bugbear";
      } else if (action.equals("crimbo16_guy4a")) {
        message = "Visiting An Elf Reindeerstler";
      } else if (action.equals("crimbo16_guy5")) {
        message = "Visiting A Hippy";
      } else if (action.equals("crimbo16_guy5a")) {
        message = "Visiting An Elf Bearddresser";
      } else if (action.equals("crimbo16_guy6")) {
        message = "Visiting A Frat Boy";
      } else if (action.equals("crimbo16_guy6a")) {
        message = "Visiting An Elf Haberdasher";
      }
    } else if (place.equals("crimbo16m")) {
      // A Meditation Mat
    } else if (place.equals("crimbo17_silentnight")) {
      if (action.equals("crimbo17_bossfight")) {
        message = "Mime-Head Building";
      } else if (action.equals("crimbo17_warehouse")) {
        message = "The Warehouse";
      }
    } else if (place.equals("crashsite")) {
      if (action.equals("crash_ship")) {
        message = "Visiting the Crashed Spaceship";
      }
    } else if (place.equals("desertbeach")) {
      if (action.equals("db_gnasir")) {
        message = "Talking to Gnasir";
      } else if (action.equals("db_nukehouse")) {
        message = "Visiting the Ruined House";
        compact = true; // Part of Breakfast
      } else if (action.equals("db_pyramid1")) {
        // message = "Visiting the Small Pyramid";
      }
    } else if (place.equals("dripfacility")) {
      if (action.equals("")) {
        // message = "Visiting The Drip Institute";
      } else if (action.equals("drip_jeremy")) {
        message = "Talking to Jeremy Science";
      } else if (action.equals("drip_armory")) {
        // Redirects to shop.php?whichshop=driparmory
      } else if (action.equals("drip_cafeteria")) {
        // Redirects to shop.php?whichshop=dripcafeteria
      }
    } else if (place.equals("exploathing")) {
      if (action.equals("expl_council")) {
        message = "Visiting The Council";
      }
    } else if (place.equals("exploathing_beach")) {
      if (action.equals("expl_gnasir")) {
        message = "Talking to Gnasir";
      } else if (action.equals("expl_pyramidpre")) {
        // message = "Visiting the Small Pyramid";
      }
    } else if (place.equals("exploathing_other")) {
    } else if (place.equals("forestvillage")) {
      if (action.equals("fv_friar")) {
        // Don't log this
        return true;
      }
      if (action.startsWith("fv_untinker")) {
        // Let UntinkerRequest claim this
        return false;
      }
      if (action.equals("fv_mystic")) {
        message = "Talking to the Crackpot Mystic";
      } else if (action.equals("fv_scientist")) {
        message = "Visiting a Science Tent";
      }
    } else if (place.equals("greygoo")) {
      if (action.equals("goo_prism")) {
        message = "Visiting a Prism of Goo";
        turns = true;
      }
    } else if (place.equals("highlands")) {
      if (action.equals("highlands_dude")) {
        message = "Talking to the Highland Lord";
      }
    } else if (place.equals("ioty2014_candy")) {
      if (action.equals("witch_house")) {
        message = "Visiting the Candy Witch's House";
      }
    } else if (place.equals("ioty2014_rumple")) {
      if (action.equals("workshop")) {
        message = "Visiting Rumplestiltskin's Workshop";
      }
    } else if (place.equals("kgb")) {
      // Kremlin's Greatest Briefcase is a "place"
      if (action.equals("")) {
        message = "Examining Kremlin's Greatest Briefcase";
      }
    } else if (place.equals("manor1")) {
      if (action.equals("manor1lock_kitchen")
          || action.equals("manor1lock_billiards")
          || action.equals("manor1lock_library")
          || action.equals("manor1lock_stairsup")) {
        return true;
      }
      if (action.equals("manor1_ladys")) {
        message = "Talking to Lady Spookyraven";
      }
    } else if (place.equals("manor2")) {
      if (action.equals("manor2lock_ballroom")
          || action.equals("manor2lock_bathroom")
          || action.equals("manor2lock_bedroom")
          || action.equals("manor2lock_gallery")
          || action.equals("manor2lock_stairsup")) {
        return true;
      }
      if (action.equals("manor2_ladys")) {
        message = "Talking to Lady Spookyraven";
      }
    } else if (place.equals("manor3")) {
      if (action.equals("manor3_ladys")) {
        message = "Talking to Lady Spookyraven";
      }
    } else if (place.equals("manor4")) {
      if (action.equals("manor4_chamber")) {
        return true;
      }
      if (action.equals("manor4_chamberwall") || action.equals("manor4_chamberwalllabel")) {
        message = "Inspecting the Suspicious Masonry";
      }
    } else if (place.equals("mclargehuge")) {
      if (action.equals("trappercabin")) {
        message = "Visiting the Trapper";
      } else if (action.equals("cloudypeak")) {
        message = "Ascending the Mist-Shrouded Peak";
      }
    } else if (place.equals("monorail")) {
      if (action.equals("monorail_lyle")) {
        message = "Visiting Lyle, LyleCo CEO";
      } else if (action.equals("monorail_downtown")) {
        message = "Train to Downtown";
        turns = true;
      }
    } else if (place.equals("mountains")) {
      if (action.equals("mts_melvin")) {
        message = "Talking to Melvign the Gnome";
      } else if (action.equals("mts_caveblocked")) {
        message = "Entering the Nemesis Cave";
      }
    } else if (place.equals("nemesiscave")) {
      if (action.equals("nmcave_rubble")) {
        message = "Examining the rubble in the Nemesis Cave";
      } else if (action.equals("nmcave_boss")) {
        message = "Confronting your Nemesis";
        turns = true;
      }
    } else if (place.equals("orc_chasm")) {
      if (action.startsWith("bridge") || action.equals("label1") || action.equals("label2")) {
        // Building the bridge. Do we need to log anything?
        return true;
      }
    } else if (place.equals("palindome")) {
      if (action.equals("pal_drlabel") || action.equals("pal_droffice")) {
        message = "Visiting Dr. Awkward's office";
        turns = true;
      } else if (action.equals("pal_mrlabel") || action.equals("pal_mroffice")) {
        message = "Visiting Mr. Alarm's office";
      }
    } else if (place.equals("plains")) {
      if (action.equals("rift_scorch") || action.equals("rift_light")) {
        return true;
      }
      if (action.equals("garbage_grounds")) {
        message = "Inspecting the Giant Pile of Coffee Grounds";
      } else if (action.equals("lutersgrave")) {
        if (!InventoryManager.hasItem(ItemPool.CLANCY_LUTE)) {
          message = "The Luter's Grave";
          turns = true;
        }
      }
    } else if (place.equals("pyramid")) {
      if (action.equals("pyramid_control")) {
        message = "Visiting the Pyramid Control Room";
      }
    } else if (place.equals("rabbithole")) {
      if (action.equals("rabbithole_teaparty")) {
        message = "Visiting the Mad Tea Party";
      }
    } else if (place.equals("snojo")) {
      if (action.equals("snojo_controller")) {
        message = "Visiting Snojo Control Console";
      }
    } else if (place.equals("spacegate")) {
      if (action.equals("sg_requisition")) {
        message = "Visiting Spacegate Equipment Requisition";
      } else if (action.equals("sg_tech")) {
        message = "Visiting Spacegate R&D";
      } else if (action.equals("sg_Terminal")) {
        message = "Visiting the Spacegate Terminal";
      } else if (action.equals("sg_vaccinator")) {
        message = "Visiting the Spacegate Vaccination Machine";
      }
    } else if (place.equals("sea_oldman")) {
      // place.php?whichplace=sea_oldman&action=oldman_oldman&preaction=pickreward&whichreward=6313[/code]
      if (action.equals("oldman_oldman")) {
        message = "Talking to the Old Man";
      }
    } else if (place.equals("thesea")) {
      if (action.equals("thesea_left2")) {
        message = "Visiting the Swimmy Little Fishes and Such";
      }
    } else if (place.equals("town")) {
      if (action.equals("town_oddjobs")) {
        message = "Visiting the Odd Jobs Board";
      }
    } else if (place.equals("town_market")) {
      if (action.equals("town_bookmobile")) {
        message = "Visiting The Bookmobile";
      }
    } else if (place.equals("town_right")) {
      if (action.equals("town_horsery")) {
        message = "Visiting The Horsery";
        compact = true; // Part of logging in
      } else if (action.equals("townright_vote")) {
        message = "Visiting The Voting Booth";
      }
    } else if (place.equals("town_wrong")) {
      if (action.equals("townwrong_precinct")) {
        message = "Visiting the 11th Precinct Headquarters";
      } else if (action.equals("townwrong_boxingdaycare")) {
        message = "Visiting the Boxing Daycare";
      }
    } else if (place.equals("twitch")) {
      if (action.equals("twitch_votingbooth")) {
        message = "Visiting the Voting / Phone Booth";
      } else if (action.equals("twitch_dancave1") || action.equals("twitch_dancave1")) {
        message = "Visiting Caveman Dan's Cave";
      } else if (action.equals("twitch_shoerepair")) {
        message = "Visiting the Shoe Repair Store";
      } else if (action.equals("twitch_colosseum")) {
        message = "Visiting the Chariot-Racing Colosseum";
      } else if (action.equals("twitch_survivors")) {
        message = "Visiting the Post-Apocalyptic Survivor Encampment";
      } else if (action.equals("twitch_bank")) {
        message = "Visiting the Third Four-Fifths Bank of the West";
      } else if (action.equals("twitch_boat2")) {
        message = "Visiting The Pinta";
      } else if (action.equals("twitch_boat3")) {
        message = "Visiting The Santa Claus";
      }
    } else if (place.equals("woods")) {
      if (action.equals("woods_emptybm")) {
        // Visiting the Empty Black Market
        return true;
      }
      if (action.equals("woods_smokesignals")) {
        message = "Investigating the Smoke Signals";
      } else if (action.equals("woods_hippy")) {
        message = "Talking to that Hippy";
      } else if (action.equals("woods_dakota_anim") || action.equals("woods_dakota")) {
        message = "Talking to Dakota Fanning";
      }
    } else if (place.equals("airport")
        || place.equals("arcade")
        || place.equals("bathole")
        || place.equals("beanstalk")
        || place.equals("chateau")
        || place.equals("giantcastle")
        || place.equals("hiddencity")
        || place.equals("knoll_friendly")
        || place.equals("nstower")
        || place.equals("scrapheap")
        || place.equals("town_market")
        || place.equals("town_right")
        || place.equals("town_wrong")
        || place.equals("wormwood")
        || place.equals("wildfire_camp")) {
      // It is not interesting to log simple visits to these
      // places. Other classes may claim specific actions.
      return action.equals("");
    } else {
      // Let any other "place" be claimed by other classes.
      return false;
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

    if (place.equals("forestvillage")) {
      // We decorate simple visits to the untinker and also
      // accepting his quest
      if (action.equals("fv_untinker") || urlString.contains("preaction=screwquest")) {
        UntinkerRequest.decorate(buffer);
      }
    } else if (place.equals("manor1")) {
      if (action.equals("manor1_ladys")) {
        if (buffer.indexOf("ghost of a necklace") != -1) {
          RequestEditorKit.addAdventureAgainSection(
              buffer,
              "place.php?whichplace=manor2&action=manor2_ladys",
              "Talk to Lady Spookyraven on the Second Floor");
        }
      }
    } else if (place.equals("rabbithole")) {
      RabbitHoleManager.decorateRabbitHole(buffer);
    } else if (place.equals("town_right")) {
      if (action.equals("townright_vote")) {
        String pref = Preferences.getString("_voteMonster");
        if (pref.equals("")) {
          pref = "unknown";
        }
        String replace = "<br />(wanderer today is " + pref + ")</blockquote>";
        StringUtilities.singleStringReplace(buffer, "</blockquote>", replace);
      }
    }
  }
}
