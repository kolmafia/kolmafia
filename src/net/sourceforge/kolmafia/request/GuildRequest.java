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
    switch (KoLCharacter.mainStat()) {
      case MUSCLE:
        return "The Brotherhood of the Smackdown";
      case MYSTICALITY:
        return "The League of Chef-Magi";
      case MOXIE:
        return "The Department of Shadowy Arts and Crafts";
      default:
        return "None";
    }
  }

  public static String getStoreName() {
    switch (KoLCharacter.mainStat()) {
      case MUSCLE:
        return "The Smacketeria";
      case MYSTICALITY:
        return "Gouda's Grimoire and Grocery";
      case MOXIE:
        return "The Shadowy Store";
      default:
        return "Nowhere";
    }
  }

  public static String getImplementName() {
    switch (KoLCharacter.mainStat()) {
      case MUSCLE:
        return "The Malus of Forethought";
      case MOXIE:
        return "Nash Crosby's Still";
      default:
        return "Nothing";
    }
  }

  public static String getMasterName() {
    switch (KoLCharacter.mainStat()) {
      case MUSCLE:
        return "Gunther, Lord of the Smackdown";
      case MYSTICALITY:
        return "Gorgonzola, the Chief Chef";
      case MOXIE:
        return "Shifty, the Thief Chief";
      default:
        return "Nobody";
    }
  }

  public static String getTrainerName() {
    switch (KoLCharacter.mainStat()) {
      case MUSCLE:
        return "Torg, the Trainer";
      case MYSTICALITY:
        return "Brie, the Trainer";
      case MOXIE:
        return "Lefty, the Trainer";
      default:
        return "Nobody";
    }
  }

  public static String getPacoName() {
    switch (KoLCharacter.mainStat()) {
      case MUSCLE:
        return "Olaf the Janitor";
      case MYSTICALITY:
        return "Blaine";
      case MOXIE:
        return "Izzy the Lizard";
      default:
        return "Nobody";
    }
  }

  public static String getSCGName() {
    switch (KoLCharacter.getAscensionClass()) {
      case SEAL_CLUBBER:
        return "Grignr, the Seal Clubber";
      case TURTLE_TAMER:
        return "Terry, the Turtle Tamer";
      case PASTAMANCER:
        return "Asiago, the Pastamancer";
      case SAUCEROR:
        return "Edam, the Sauceror";
      case DISCO_BANDIT:
        return "Duncan Drisorderly, the Disco Bandit";
      case ACCORDION_THIEF:
        return "Stradella, the Accordion Thief";
      default:
        return "Nobody";
    }
  }

  public static String getOCGName() {
    switch (KoLCharacter.getAscensionClass()) {
      case SEAL_CLUBBER:
        return "Terry, the Turtle Tamer";
      case TURTLE_TAMER:
        return "Grignr, the Seal Clubber";
      case PASTAMANCER:
        return "Edam, the Sauceror";
      case SAUCEROR:
        return "Asiago, the Pastamancer";
      case DISCO_BANDIT:
        return "Stradella, the Accordion Thief";
      case ACCORDION_THIEF:
        return "Duncan Drisorderly, the Disco Bandit";
      default:
        return "Nobody";
    }
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

    Matcher matcher = GenericRequest.PLACE_PATTERN.matcher(urlString);
    String place = matcher.find() ? matcher.group(1) : null;

    if (place != null && place.equals("paco")) {
      // "paco" assigns the meat car, white citadel, and dwarven factory quests
      if (InventoryManager.hasItem(ItemPool.CITADEL_SATCHEL)) {
        ResultProcessor.processItem(ItemPool.CITADEL_SATCHEL, -1);
        QuestDatabase.setQuestProgress(Quest.CITADEL, QuestDatabase.FINISHED);
      }

      if (InventoryManager.hasItem(ItemPool.THICK_PADDED_ENVELOPE)) {
        ResultProcessor.processItem(ItemPool.THICK_PADDED_ENVELOPE, -1);
      }

      if (responseText.contains("White Citadel")) {
        QuestDatabase.setQuestIfBetter(Quest.CITADEL, QuestDatabase.STARTED);
      }

      return;
    }

    if (place != null && place.equals("ocg")) {
      // "ocg" (Other Class in Guild) assigns Fernswarthy
      // quest

      // <Muscle class> looks surprised as you hand over
      // Fernswarthy's key.

      // "So, have you returned with Fernswarthy's key?"
      // <Mysticality class> nods approvingly as you hand the
      // key to him.

      // <Moxie class> grins and takes Fernswarthy's key from
      // you.

      if (responseText.contains("hand over Fernswarthy's key")
          || responseText.contains("returned with Fernswarthy's key")
          || responseText.contains("takes Fernswarthy's key")) {
        ResultProcessor.processItem(ItemPool.FERNSWARTHYS_KEY, -1);
      }

      return;
    }

    if (place != null && place.equals("challenge")) {
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
    }

    matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    String action = matcher.find() ? matcher.group(1) : null;

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
