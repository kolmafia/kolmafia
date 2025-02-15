package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.concoction.ChefStaffRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GuildRequest extends GenericRequest {
  public static final Pattern SKILL_PATTERN = Pattern.compile("skillid=(\\d*)");

  public GuildRequest() {
    super("guild.php");
  }

  public GuildRequest(final String place) {
    this();
    this.addFormField("place", place);
  }

  public static String whichGuild() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> "The Brotherhood of the Smackdown";
      case MYSTICALITY -> "The League of Chef-Magi";
      case MOXIE -> "The Department of Shadowy Arts and Crafts";
      default -> "None";
    };
  }

  public static String getStoreName() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> "The Smacketeria";
      case MYSTICALITY -> "Gouda's Grimoire and Grocery";
      case MOXIE -> "The Shadowy Store";
      default -> "Nowhere";
    };
  }

  public static String getImplementName() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> "The Malus of Forethought";
      case MOXIE -> "Nash Crosby's Still";
      default -> "Nothing";
    };
  }

  public static String getMasterName() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> "Gunther, Lord of the Smackdown";
      case MYSTICALITY -> "Gorgonzola, the Chief Chef";
      case MOXIE -> "Shifty, the Thief Chief";
      default -> "Nobody";
    };
  }

  public static String getTrainerName() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> "Torg, the Trainer";
      case MYSTICALITY -> "Brie, the Trainer";
      case MOXIE -> "Lefty, the Trainer";
      default -> "Nobody";
    };
  }

  public static String getPacoName() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> "Olaf the Janitor";
      case MYSTICALITY -> "Blaine";
      case MOXIE -> "Izzy the Lizard";
      default -> "Nobody";
    };
  }

  public static String getSCGName() {
    return switch (KoLCharacter.getAscensionClass()) {
      case SEAL_CLUBBER -> "Grignr, the Seal Clubber";
      case TURTLE_TAMER -> "Terry, the Turtle Tamer";
      case PASTAMANCER -> "Asiago, the Pastamancer";
      case SAUCEROR -> "Edam, the Sauceror";
      case DISCO_BANDIT -> "Duncan Drisorderly, the Disco Bandit";
      case ACCORDION_THIEF -> "Stradella, the Accordion Thief";
      default -> "Nobody";
    };
  }

  public static String getOCGName() {
    return switch (KoLCharacter.getAscensionClass()) {
      case SEAL_CLUBBER -> "Terry, the Turtle Tamer";
      case TURTLE_TAMER -> "Grignr, the Seal Clubber";
      case PASTAMANCER -> "Edam, the Sauceror";
      case SAUCEROR -> "Asiago, the Pastamancer";
      case DISCO_BANDIT -> "Stradella, the Accordion Thief";
      case ACCORDION_THIEF -> "Duncan Drisorderly, the Disco Bandit";
      default -> "Nobody";
    };
  }

  public static String getNPCName(final String place) {
    if (place == null) {
      return null;
    }

    if (place.equals("paco")) {
      return GuildRequest.getPacoName();
    }

    if (place.equals("ocg")) {
      return GuildRequest.getOCGName();
    }

    if (place.equals("scg")) {
      return GuildRequest.getSCGName();
    }

    if (place.equals("trainer")) {
      return GuildRequest.getTrainerName();
    }

    if (place.equals("challenge")) {
      return GuildRequest.getMasterName();
    }

    return null;
  }

  @Override
  public void processResults() {
    GuildRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final int findSkill(final String urlString) {
    Matcher matcher = GuildRequest.SKILL_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return 0;
    }

    return SkillDatabase.classSkillsBase() + StringUtilities.parseInt(matcher.group(1));
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("guild.php")) {
      return;
    }

    KoLCharacter.setGuildStoreOpen(responseText.contains("\"shop.php"));

    // Guild quests are offered by several NPCs. Update progress.
    if (handleGuildQuests(GenericRequest.getPlace(urlString), responseText)) {
      return;
    }

    String action = GenericRequest.getAction(urlString);

    // We have nothing special to do for other simple visits.

    if (action == null) {
      return;
    }

    if (action.equals("buyskill")) {
      if (responseText.contains("You learn a new skill")) {
        int skillId = GuildRequest.findSkill(urlString);
        int cost = SkillDatabase.getSkillPurchaseCost(skillId);
        if (cost > 0) {
          ResultProcessor.processMeat(-cost);
        }
        // New skill may affect concocoction list,
        // uneffect methods, or amount healed.
        ConcoctionDatabase.refreshConcoctions();
        if (skillId == SkillPool.ADVENTURER_OF_LEISURE) {
          UneffectRequest.reset();
          HPRestoreItemList.updateHealthRestored();
        }
      }
      return;
    }

    if (action.equals("makestaff")) {
      ChefStaffRequest.parseCreation(urlString, responseText);
      return;
    }

    if (action.equals("malussmash")) {
      CreateItemRequest.parseGuildCreation(urlString, responseText);
      return;
    }
  }

  public static boolean handleGuildQuests(String place, String responseText) {
    // Return true if this is all that needs to be done with this response.
    // Return false if there might be more to do

    if (place == null) {
      return false;
    }

    if (place.equals("paco")) {
      // "paco" = Olaf the Janitor, Blaine, or Izzy the Lizard.
      // Quest.MEATCAR	questG01Meatcar		Retrieve the Meatcar
      //
      // started	"paco" asks you to retrieve Meatcar
      // finished	"paco" thanks you; Desert Beach is open

      // Quest.CITADEL	questG02Whitecastle	Pick up takeout from White Citadel
      //
      // started	"paco" asks you to fetch lunch from White Citadel
      // step1		It's a Sign! - Open The Road to White Citadel
      // step2		They Aren't Blind, They're Just Wearing Shades
      // step3		I Guess They Were the Existential Blues Brothers‎‎
      // step4		30 burnouts defeated
      // step5		biclops defeated
      // step6		Life Ain't Nothin But Witches and Mummies (931)
      // step7		Defeat witch
      // step8		No Whammies (932)
      // step9		No Whammies (932 - done with treasure chests)
      // step10		Defeat Elp&iacute;zo & Crosybdis
      // finished	"paco" takes satchel. You gain lucky rabbit's foot

      // Quest.FACTORY	questG06Delivery	Fetch package from Dwarven Factory
      //
      // started	"paco" asks you to fetch a parcel from the Dwarven Factory
      // finished	"paco" takes parcel. You gain 1000 Meat

      if (responseText.contains("Degrassi Knoll")) {
        QuestDatabase.setQuestIfBetter(Quest.MEATCAR, QuestDatabase.STARTED);
        // You are given the Degrassi Knoll shopping list
      }

      if (responseText.contains("South of the Border")) {
        QuestDatabase.setQuestIfBetter(Quest.MEATCAR, QuestDatabase.FINISHED);
        KoLCharacter.setDesertBeachAvailable();
      }

      // The White Citadel quest is offered in choice #930.
      // You accept it via 930/1
      //
      // If you revisit "paco" before fetching the satchel, "paco" reminds you
      // to go to Whitey's Grove.

      if (responseText.contains("Whitey's Grove")) {
        QuestDatabase.setQuestIfBetter(Quest.CITADEL, QuestDatabase.STARTED);
      }

      // When you return with the satchel, they give you a lucky rabbit's
      // foot. that is handled in choice #930.
      //
      // Therefore, the quest is started and finished in choice handling.

      if (responseText.contains("7-foot Dwarves") || responseText.contains("McLargeHuge")) {
        QuestDatabase.setQuestIfBetter(Quest.FACTORY, QuestDatabase.STARTED);
        // Dwarven Factory Complex is now accessible
        // We don't have a setting to track that
      }

      if (InventoryManager.hasItem(ItemPool.THICK_PADDED_ENVELOPE)) {
        ResultProcessor.processItem(ItemPool.THICK_PADDED_ENVELOPE, -1);
        QuestDatabase.setQuestProgress(Quest.FACTORY, QuestDatabase.FINISHED);
      }

      return true;
    }

    if (place.equals("ocg")) {
      // "ocg" (Other Class in Guild)
      // Quest.EGO	questG03Ego		The Wizard of Ego
      //
      // started	"ocg" offers quest; Misspelled Cemetary is open
      // step1		"ocg" takes Fernswarthy's key
      // step2		"ocg" gives Fernswarthy's key; Tower Ruins are open
      // step3		(Visiting the tower ruins for the first time.)
      // step4		Tower Ruins NC: Staring into Nothing
      // step5		Tower Ruins NC: Into the Maw of Deepness
      // step6		Tower Ruins NC: Take a Dusty Look!
      // finished	"ocg" takes dusty book

      // <Muscle class>: I'll mark the location of the Cemetary on your map.
      // <Mysticality class>: You already know where the Cemetary is, right?
      // <Moxie class>: You already know where the Cemetary is, right?

      if (responseText.contains("the location of the Cemetary")
          || responseText.contains("already know where the Cemetary is")) {
        QuestDatabase.setQuestProgress(Quest.EGO, QuestDatabase.STARTED);
      }

      // <Muscle class> looks surprised as you hand over Fernswarthy's key.
      // "So, have you returned with Fernswarthy's key?"
      // <Mysticality class> nods approvingly as you hand the key to him.
      // <Moxie class> grins and takes Fernswarthy's key from you.

      if (responseText.contains("hand over Fernswarthy's key")
          || responseText.contains("returned with Fernswarthy's key")
          || responseText.contains("takes Fernswarthy's key")) {
        ResultProcessor.processItem(ItemPool.FERNSWARTHYS_KEY, -1);
        QuestDatabase.setQuestProgress(Quest.EGO, "step1");
      }

      // <Muscle class> gives you the key you found earlier
      // <Mysticality class>: returns the key you found earlier
      // <Moxie class>: Here's the key, and I'll mark the location of the tower on your map

      if (responseText.contains("gives you the key")
          || responseText.contains("returns the key")
          || responseText.contains("Here's the key")) {
        QuestDatabase.setQuestProgress(Quest.EGO, "step2");
      }

      if (responseText.contains("Manual of Labor")
          || responseText.contains("Manual of Transmission")
          || responseText.contains("Manual of Dexterity")) {
        // These are done in ResultProcessor
        // ResultProcessor.processItem(ItemPool.DUSTY_BOOK, -1);
        // ResultProcessor.processItem(ItemPool.FERNSWARTHYS_KEY, -1);
        QuestDatabase.setQuestProgress(Quest.EGO, QuestDatabase.FINISHED);
      }

      return true;
    }

    if (place.equals("scg")) {
      // "scg" (Other Class in Guild)
      // Quest.DARK	questG05Dark		old Nemesis Quest
      // Quest.NEMESIS	questG04Nemesis		new Nemesis Quest
      //
      // started	"scg" offers quest; Misspelled Cemetary is open
      // step1		Tomb of the Unknown Your Class Here (1049 - meet ghost)
      // step2		Fight The Unknown <class>
      // step3		Tomb of the Unknown Your Class Here (1049 - defeated ghost)
      // step4		Tomb of the Unknown Your Class Here (1049 - claimed Epic Weapon)
      // step5		"scg" sends to Beelzebozo; The "Fun" House is open
      // step6		Fight The Clownlord Beelzebozo
      // step7		"scg" directs you to craft legendary Epic Weapon
      // step8		Crafted Legendary Epic Weapon
      // step9		"scg" sees Legendary Epic Weapon
      // step10		"scg" sends to cave; Dark and Dank and Sinister Cave is open
      // step11		The Dark and Dank and Sinister Cave Entrance (1087 - see puzzle)
      // step12		The Dark and Dank and Sinister Cave Entrance (1087 - passed)
      // step13		Rubble, Rubble, Toil and Trouble (1088 - see pile of rubble)
      // step14		Acquired 6 or more fizzing spore pods
      // step15		Rubble, Rubble, Toil and Trouble (1088 - blew up rubble)
      // step16		Defeated Nemesis (Inner Sanctum) and acquire Epic Hat
      // step17		Second visit; unlocks nemesis assassins
      // step18		Lose to menacing thug
      // step19		Defeat menacing thug
      // step20		Lose to Mob Penguin hitman
      // step21		Defeat Mob Penguin hitman
      // step22		Lose to third Nemesis assassin
      // step23		Defeat third Nemesis assassin
      // step24		Lose to fourth Nemesis assassin
      // step25		Defeat 4th Nemesis assassin (gain volcano map)
      // step26		(Obsolete; unlocks volcano lair via navigator on the Poop Deck)
      // step27		Defeat Nemesis (The Nemesis; Lair) and obtain Legendary Pants
      // step28		Start fight with Nemesis (Volcanic Cave)
      // step29		Defeat Nemesis (Volcanic Cave)
      // finished	Defeat Demonic Nemesis and obtain final quest rewards

      if (
      // Muscle classes
      responseText.contains("The Tomb is within the Misspelled")
          ||
          // Mysticality classes
          responseText.contains("the Tomb, which is within the Misspelled")
          ||
          // Moxie classes
          responseText.contains("the Tomb is in the Misspelled")) {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, QuestDatabase.STARTED);
      }

      if (
      // Muscle classes
      responseText.contains("not recovered the Epic Weapon yet")
          ||
          // Mysticality classes
          responseText.contains("not yet claimed the Epic Weapon")
          ||
          // Moxie classes
          responseText.contains("the delay on that Epic Weapon")) {
        QuestDatabase.setQuestIfBetter(Quest.NEMESIS, QuestDatabase.STARTED);
      }

      if (responseText.contains("Clownlord Beelzebozo")) {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step5");
      }

      if (responseText.contains("a Meatsmithing hammer")) {
        QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step7");
      }

      if (QuestDatabase.isQuestStep(Quest.NEMESIS, "step8")) {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step9");
      }

      if (responseText.contains("in the Big Mountains")
          && !responseText.contains("not the required mettle to defeat")) {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step10");
      }

      if (QuestDatabase.isQuestStep(Quest.NEMESIS, "step16")) {
        QuestDatabase.setQuestProgress(Quest.NEMESIS, "step17");
      }

      return true;
    }

    if (place.equals("challenge")) {
      // Quest.MUSCLE	questG09Muscle		Fetch big knob sausage
      // Quest.MYST	questG07Myst		Exorcise a sandwich
      // Quest.MOXIE	questG08Moxie		Steal your own pants
      //
      // started	Guildmaster offers challenge to join guild
      // step1		Challenge completed. Time to return to guild.
      // finished	Guildmaster admits you to the guild

      // Muscle guild quest
      // "Eleven inches!" he exclaims.
      if (responseText.contains("Eleven inches")) {
        ResultProcessor.processItem(ItemPool.BIG_KNOB_SAUSAGE, -1);
        QuestDatabase.setQuestProgress(Quest.MUSCLE, QuestDatabase.FINISHED);
      } else if (responseText.contains("sausage")) {
        QuestDatabase.setQuestProgress(Quest.MUSCLE, QuestDatabase.STARTED);
      }

      // Myst guild quests
      else if (responseText.contains("captured poltersandwich")) {
        ResultProcessor.processItem(ItemPool.EXORCISED_SANDWICH, -1);
        QuestDatabase.setQuestProgress(Quest.MYST, QuestDatabase.FINISHED);
      } else if (responseText.contains("poltersandwich")) {
        QuestDatabase.setQuestProgress(Quest.MYST, QuestDatabase.STARTED);
      }

      // Moxie guild quests
      else if (responseText.contains("stole my own pants")) {
        QuestDatabase.setQuestProgress(Quest.MOXIE, QuestDatabase.FINISHED);
      } else if (responseText.contains("check out the Sleazy Back Alley")) {
        QuestDatabase.setQuestProgress(Quest.MOXIE, QuestDatabase.STARTED);
      }

      // If we finished the quest, we can enter the guild. Perhaps there is more to do.
      return false;
    }

    return false;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("guild.php")) {
      return false;
    }

    Matcher matcher = GenericRequest.PLACE_PATTERN.matcher(urlString);
    String place = matcher.find() ? matcher.group(1) : null;

    if (place != null && place.equals("still")) {
      return true;
    }

    String npc = getNPCName(place);

    if (npc != null) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Visiting " + npc);
      return true;
    }

    matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    String action = matcher.find() ? matcher.group(1) : null;

    // We have nothing special to do for other simple visits.

    if (action == null) {
      return true;
    }

    if (action.equals("train")) {
      return true;
    }

    // Other requests handle other actions in the Guild

    // action = makestaff
    // action = wokcook
    // action = malussmash

    return false;
  }
}
