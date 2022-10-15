package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SuburbanDisRequest extends GenericRequest {
  private static final Pattern STONE1_PATTERN = Pattern.compile("stone1=(\\d+)");
  private static final Pattern STONE2_PATTERN = Pattern.compile("stone2=(\\d+)");
  public static final AdventureResult FOLIO = ItemPool.get(ItemPool.DEVILISH_FOLIO, 1);

  private final String action;

  public SuburbanDisRequest(String action) {
    super("suburbandis.php");
    this.action = action;
    this.addFormField("action", action);
  }

  public SuburbanDisRequest() {
    this("altar");
  }

  public SuburbanDisRequest(int stone1, int stone2) {
    this("stoned");
    this.addFormField("stone1", String.valueOf(stone1));
    this.addFormField("stone2", String.valueOf(stone2));
  }

  @Override
  public int getAdventuresUsed() {
    return getAdventuresUsed("dothis".equals(this.action));
  }

  public static int getAdventuresUsed(String urlString) {
    return getAdventuresUsed("dothis".equals(GenericRequest.getAction(urlString)));
  }

  private static int getAdventuresUsed(boolean dothis) {
    return dothis ? 1 : 0;
  }

  @Override
  public void processResults() {
    SuburbanDisRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String location, final String responseText) {
    if (location.equals("suburbandis.php")) {
      return;
    }

    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (responseText.contains("Since you carved up the Thing With no Name")) {
        Preferences.setInteger("lastThingWithNoNameDefeated", KoLCharacter.getAscensions());
      }

      return;
    }

    if (action.equals("altar")) {
      // The stones are actually removed from your inventory on defeating The Thing with No Name
      // so handle that in QuestManager.updateQuestData
      return;
    }

    if (action.equals("stoned")) {
      // Look for success.
      if (!responseText.contains("You acquire an effect")) {
        return;
      }

      Preferences.setInteger("lastThingWithNoNameDefeated", KoLCharacter.getAscensions());

      Matcher matcher = SuburbanDisRequest.STONE1_PATTERN.matcher(location);
      if (matcher.find()) {
        int stone1 = StringUtilities.parseInt(matcher.group(1));
        ResultProcessor.processResult(ItemPool.get(stone1, -1));
        setQuestsBasedOnStone(stone1);
      }

      matcher = SuburbanDisRequest.STONE2_PATTERN.matcher(location);
      if (matcher.find()) {
        int stone2 = StringUtilities.parseInt(matcher.group(1));
        ResultProcessor.processResult(ItemPool.get(stone2, -1));
        setQuestsBasedOnStone(stone2);
      }

      return;
    }
  }

  private static Quest getQuestForStone(final int stoneId) {
    return switch (stoneId) {
      case ItemPool.FURIOUS_STONE, ItemPool.VANITY_STONE -> Quest.CLUMSINESS;
      case ItemPool.LECHEROUS_STONE, ItemPool.JEALOUSY_STONE -> Quest.MAELSTROM;
      case ItemPool.AVARICE_STONE, ItemPool.GLUTTONOUS_STONE -> Quest.GLACIER;
      default -> null;
    };
  }

  private static void setQuestsBasedOnStone(final int stoneId) {
    Quest quest = getQuestForStone(stoneId);
    String current = QuestDatabase.getQuest(quest);

    QuestDatabase.setQuest(
        quest,
        switch (current) {
            // If the quest was finished its now like you've defeated one boss
          case QuestDatabase.FINISHED -> "step2";
            // If you'd just queued your second boss its like you'd picked your first
          case "step3" -> "step1";
            // If you'd only defeated one boss its like you'd done nothing
          case "step2" -> QuestDatabase.UNSTARTED;
          default -> current;
        });
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("suburbandis.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return false;
    }

    String message;
    if (action.equals("altar")) {
      // Visiting the altar
      return true;
    }

    if (action.equals("dothis")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] An Altar in The Suburbs of Dis";
    } else if (action.equals("stoned")) {
      Matcher matcher = SuburbanDisRequest.STONE1_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        return true;
      }

      int stone1 = StringUtilities.parseInt(matcher.group(1));
      matcher = SuburbanDisRequest.STONE2_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        return true;
      }

      int stone2 = StringUtilities.parseInt(matcher.group(1));
      message =
          "Placing "
              + ItemDatabase.getItemName(stone1)
              + " and "
              + ItemDatabase.getItemName(stone2)
              + " into altar.";
    } else {
      return false;
    }

    RequestLogger.printLine("");
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
