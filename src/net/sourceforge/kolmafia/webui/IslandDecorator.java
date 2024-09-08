package net.sourceforge.kolmafia.webui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.IslandRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.IslandManager.Quest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IslandDecorator {
  private static final String progressLineStyle =
      "<td style=\"color: red;font-size: 80%\" align=center>";

  // KoLmafia images showing each quest area on bigisland.php

  // JHunz's replacement images for Big Island sidequest areas from his
  // BattlefieldCounter Greasemonkey script, used with his permission.
  //
  //	http://userscripts.org/scripts/show/11720

  private static final String LOCAL_ROOT = "/images/otherimages/bigisland/";

  private static final Pattern GREMLIN_TOOL_MESSAGE = Pattern.compile("<!--moly\\d-->");

  private record Image(Quest quest, String image, String frat, String hippy) {}

  private static final Image[] IMAGES = {
    new Image(
        Quest.JUNKYARD,
        IslandDecorator.LOCAL_ROOT + "2.gif",
        IslandDecorator.LOCAL_ROOT + "2F.gif",
        IslandDecorator.LOCAL_ROOT + "2H.gif"),
    new Image(
        Quest.ORCHARD,
        IslandDecorator.LOCAL_ROOT + "3.gif",
        IslandDecorator.LOCAL_ROOT + "3F.gif",
        IslandDecorator.LOCAL_ROOT + "3H.gif"),
    new Image(
        Quest.ARENA,
        IslandDecorator.LOCAL_ROOT + "6.gif",
        IslandDecorator.LOCAL_ROOT + "6F.gif",
        IslandDecorator.LOCAL_ROOT + "6H.gif"),
    new Image(
        Quest.FARM,
        IslandDecorator.LOCAL_ROOT + "15.gif",
        IslandDecorator.LOCAL_ROOT + "15F.gif",
        IslandDecorator.LOCAL_ROOT + "15H.gif"),
    new Image(
        Quest.LIGHTHOUSE,
        IslandDecorator.LOCAL_ROOT + "17.gif",
        IslandDecorator.LOCAL_ROOT + "17F.gif",
        IslandDecorator.LOCAL_ROOT + "17H.gif"),
    new Image(
        Quest.NUNS,
        IslandDecorator.LOCAL_ROOT + "19.gif",
        IslandDecorator.LOCAL_ROOT + "19F.gif",
        IslandDecorator.LOCAL_ROOT + "19H.gif"),
  };

  private IslandDecorator() {}

  private static Image findImages(final Quest quest) {
    for (int i = 0; i < IslandDecorator.IMAGES.length; ++i) {
      Image row = IslandDecorator.IMAGES[i];
      if (row.quest == quest) {
        return row;
      }
    }
    return null;
  }

  private static String originalImage(final Quest quest) {
    Image row = IslandDecorator.findImages(quest);
    return row == null ? "" : row.image;
  }

  private static String fratImage(final Quest quest) {
    Image row = IslandDecorator.findImages(quest);
    return row == null ? "" : row.frat;
  }

  private static String hippyImage(final Quest quest) {
    Image row = IslandDecorator.findImages(quest);
    return row == null ? "" : row.hippy;
  }

  private static String sidequestImage(final String setting, final Quest quest) {
    String status = Preferences.getString(setting);
    return status.equals("fratboy")
        ? IslandDecorator.fratImage(quest)
        : status.equals("hippy") ? IslandDecorator.hippyImage(quest) : null;
  }

  /*
   * Methods to decorate the Fight page
   */

  public static final void decorateThemtharFight(final StringBuffer buffer) {
    int index = buffer.indexOf("<!--WINWINWIN-->");
    if (index == -1) {
      return;
    }

    String message = "<p><center>" + IslandDecorator.meatMessage() + "<br>";
    buffer.insert(index, message);
  }

  // Meat drops from dirty thieving brigands
  private static final MonsterData BRIGAND = MonsterDatabase.findMonster("dirty thieving brigand");
  private static final int BRIGAND_MIN = BRIGAND.getMinMeat();
  private static final int BRIGAND_MAX = BRIGAND.getMaxMeat();

  private static String meatMessage() {
    StringBuilder message = new StringBuilder();

    int current = IslandManager.currentNunneryMeat();
    message.append(KoLConstants.COMMA_FORMAT.format(current));
    message.append(" meat recovered, ");

    double left = 100000 - current;

    message.append(KoLConstants.COMMA_FORMAT.format(left));
    message.append(" left (");

    double mod = (KoLCharacter.currentNumericModifier(DoubleModifier.MEATDROP) + 100.0f) / 100.0f;
    double min = BRIGAND_MIN * mod;
    double max = BRIGAND_MAX * mod;

    int minTurns = (int) Math.ceil(left / max);
    int maxTurns = (int) Math.ceil(left / min);

    message.append(minTurns);

    if (minTurns != maxTurns) {
      message.append("-");
      message.append(maxTurns);
    }

    message.append(" turns).");

    return message.toString();
  }

  public enum GremlinTool {
    HAMMER(ItemPool.MOLYBDENUM_HAMMER, "batwinged gremlin"),
    WRENCH(ItemPool.MOLYBDENUM_WRENCH, "erudite gremlin"),
    PLIERS(ItemPool.MOLYBDENUM_PLIERS, "spider gremlin"),
    SCREWDRIVER(ItemPool.MOLYBDENUM_SCREWDRIVER, "vegetable gremlin");

    public final AdventureResult tool;
    public final MonsterData goodGremlin;
    public final MonsterData badGremlin;

    GremlinTool(int toolId, String gremlin) {
      this.tool = ItemPool.get(toolId, 1);
      this.goodGremlin = MonsterDatabase.findMonster(gremlin + " (tool)");
      this.badGremlin = MonsterDatabase.findMonster(gremlin);
    }
  }

  private static final Map<Integer, GremlinTool> goodGremlins = new HashMap<>();
  private static final Map<Integer, GremlinTool> badGremlins = new HashMap<>();

  static {
    for (GremlinTool tool : GremlinTool.values()) {
      goodGremlins.put(tool.goodGremlin.getId(), tool);
      badGremlins.put(tool.badGremlin.getId(), tool);
    }
  }

  public static final void decorateGremlinFight(
      final MonsterData monster, final StringBuffer buffer) {
    GremlinTool tool = goodGremlins.get(monster.getId());

    if (tool == null) {
      // This is not a gremlin which drops a tool
      return;
    }

    // We only color the tool if the junkyard quest is active
    if (!IslandManager.junkyardQuestActive()) {
      return;
    }

    // We only color the tool if we don't already have it
    if (KoLConstants.inventory.contains(tool.tool)) {
      return;
    }

    // Color the tool in the monster spoiler text

    String toolName = tool.tool.getName();
    StringUtilities.singleStringReplace(
        buffer, toolName, "<font color=#DD00FF>" + toolName + "</font>");

    // Is the monster presenting the tool?
    Matcher matcher = GREMLIN_TOOL_MESSAGE.matcher(buffer.toString());
    if (matcher.find()) {
      int molyIndex = matcher.start();
      int td = buffer.substring(0, molyIndex).lastIndexOf("<td>");
      String message = buffer.substring(td + 4, molyIndex);
      // Make the message pink
      buffer.replace(td + 4, molyIndex, "<td><font color=#DD00FF>" + message + "</font>");

      // If we already have the molybdenum magnet selected
      // (which should only be possible if we are
      // funkslinging), cool. Otherwise, get magnet on the
      // first combat item dropdown.

      String select1 = "<option picurl=magnet2 selected value=2497>";
      String select2 = "<option selected value=2497>";

      if (buffer.indexOf(select1) == -1 && buffer.indexOf(select2) == -1) {
        // Unselect battle actions in dropdowns on the fight page
        StringUtilities.globalStringReplace(buffer, " selected ", " ");

        // Select the molybdenum magnet
        // <option picurl=magnet2 selected value=2497>molybdenum magnet (1)</option>
        String search = "<option picurl=magnet2 value=2497>";
        StringUtilities.singleStringReplace(buffer, search, select1);
      }
    }
  }

  public static final void appendMissingGremlinTool(
      MonsterData monster, final StringBuilder buffer) {
    GremlinTool tool = badGremlins.get(monster.getId());
    if (tool == null) {
      return;
    }

    // Only mention the tool if the junkyard quest is active
    if (!IslandManager.junkyardQuestActive()) {
      return;
    }

    // Don't mention the missing tool if we already have the tool
    if (KoLConstants.inventory.contains(tool.tool)) {
      return;
    }

    buffer.append("<br />This gremlin does <b>NOT</b> have a ").append(tool.tool.getName());
  }

  private static String victoryMessageHTML(final int last, final int current) {
    String message = IslandManager.victoryMessage(last, current);
    return message == null ? "" : message + "<br>";
  }

  private static String areaMessageHTML(final int last, final int current) {
    String message = IslandManager.areaMessage(last, current);
    return message == null ? "" : "<b>" + message + "</b><br>";
  }

  private static String heroMessageHTML(final int last, final int current) {
    String message = IslandManager.heroMessage(last, current);
    return message == null ? "" : "<b>" + message + "</b><br>";
  }

  public static final void decorateBattlefieldFight(final StringBuffer buffer) {
    int index = buffer.indexOf("<!--WINWINWIN-->");
    if (index == -1) {
      return;
    }

    // Don't bother showing progress of the war if you've just won
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = "";
    if (monster != null) {
      monsterName = monster.getName();
    }
    if (monsterName.equals("The Big Wisniewski") || monsterName.equals("The Man")) {
      return;
    }

    int last;
    int current;

    if (IslandManager.fratboy()) {
      last = IslandManager.lastFratboysDefeated();
      current = IslandManager.fratboysDefeated();
    } else {
      last = IslandManager.lastHippiesDefeated();
      current = IslandManager.hippiesDefeated();
    }

    if (last == current) {
      return;
    }

    String message =
        "<p><center>"
            + victoryMessageHTML(last, current)
            + areaMessageHTML(last, current)
            + heroMessageHTML(last, current)
            + "</center>";

    buffer.insert(index, message);
  }

  /*
   * Method to decorate the Big Island map
   */

  // Decorate the HTML with custom goodies
  public static final void decorateBigIsland(final String url, final StringBuffer buffer) {
    // Quest-specific page decorations
    IslandDecorator.decorateJunkyard(buffer);
    IslandDecorator.decorateArena(url, buffer);
    IslandDecorator.decorateNunnery(url, buffer);

    // Find the table that contains the map.
    int tableIndex =
        buffer.indexOf(
            "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Mysterious Island of Mystery</b></td>");
    if (tableIndex != -1) {
      String fratboyMessage = IslandManager.sideSummary("frat boys");
      String hippyMessage = IslandManager.sideSummary("hippies");
      String row =
          "<tr><td><center><table width=100%><tr>"
              + IslandDecorator.progressLineStyle
              + fratboyMessage
              + "</td>"
              + IslandDecorator.progressLineStyle
              + hippyMessage
              + "</td>"
              + "</tr></table></td></tr>";

      buffer.insert(tableIndex, row);
    }

    // Replace all KoL image servers with KoLmafia image cache locations
    for (String path : KoLmafia.IMAGE_SERVER_PATHS) {
      StringUtilities.globalStringReplace(buffer, path, "/images/");
    }

    // Replace sidequest location images for completed quests
    IslandDecorator.sidequestImage(buffer, "sidequestArenaCompleted", Quest.ARENA);
    IslandDecorator.sidequestImage(buffer, "sidequestFarmCompleted", Quest.FARM);
    IslandDecorator.sidequestImage(buffer, "sidequestJunkyardCompleted", Quest.JUNKYARD);
    IslandDecorator.sidequestImage(buffer, "sidequestLighthouseCompleted", Quest.LIGHTHOUSE);
    IslandDecorator.sidequestImage(buffer, "sidequestNunsCompleted", Quest.NUNS);
    IslandDecorator.sidequestImage(buffer, "sidequestOrchardCompleted", Quest.ORCHARD);
  }

  private static void sidequestImage(
      final StringBuffer buffer, final String setting, final Quest quest) {
    String image = IslandDecorator.sidequestImage(setting, quest);
    if (image == null) {
      return;
    }

    String old = IslandDecorator.originalImage(quest);
    StringUtilities.singleStringReplace(buffer, old, image);
  }

  public static final void decorateJunkyard(final StringBuffer buffer) {
    if (IslandManager.currentJunkyardLocation().equals("")) {
      return;
    }

    int tableIndex =
        buffer.indexOf(
            "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Junkyard</b></td>");
    if (tableIndex == -1) {
      return;
    }

    String message;

    if (!InventoryManager.hasItem(ItemPool.MOLYBDENUM_MAGNET)) {
      message = "Visit Yossarian in uniform to get a molybdenum magnet";
    } else if (IslandManager.currentJunkyardTool().equals("")) {
      message = "Visit Yossarian for your next assignment";
    } else if (InventoryManager.hasItem(ItemPool.MOLYBDENUM_HAMMER)
        && InventoryManager.hasItem(ItemPool.MOLYBDENUM_SCREWDRIVER)
        && InventoryManager.hasItem(ItemPool.MOLYBDENUM_PLIERS)
        && InventoryManager.hasItem(ItemPool.MOLYBDENUM_WRENCH)) {
      message =
          "Visit Yossarian in uniform to receive your reward for finding all four molybdenum tools";
    } else {
      message =
          "Look for the "
              + IslandManager.currentJunkyardTool()
              + " "
              + IslandManager.currentJunkyardLocation();
    }

    String row =
        "<tr><td><center><table width=100%><tr>"
            + IslandDecorator.progressLineStyle
            + message
            + ".</td>"
            + "</tr></table></td></tr>";

    buffer.insert(tableIndex, row);
  }

  public static final void decorateArena(final String urlString, final StringBuffer buffer) {
    // If he's not visiting the arena, punt
    if (!urlString.contains("place=concert")) {
      return;
    }

    // If there's no concert available, see if quest is in progress
    if (buffer.indexOf("value=\"concert\"") == -1) {
      if (Preferences.getString("warProgress").equals("finished")
          || !Preferences.getString("sidequestArenaCompleted").equals("none")) {
        // War is over or quest is finished. Punt.
        return;
      }

      int tableIndex =
          buffer.indexOf(
              "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>Mysterious Island Arena</b></td>");
      if (tableIndex != -1) {
        String message = RequestEditorKit.advertisingMessage();
        String row =
            "<tr><td><center><table width=100%><tr>"
                + IslandDecorator.progressLineStyle
                + message
                + "</td></tr></table></td></tr>";
        buffer.insert(tableIndex, row);
      }
      return;
    }

    String quest = Preferences.getString("sidequestArenaCompleted");
    String[][] array =
        quest.equals("hippy") ? IslandRequest.HIPPY_CONCERTS : IslandRequest.FRATBOY_CONCERTS;

    String text = buffer.toString();
    buffer.setLength(0);

    int index1 = 0, index2;

    // Add first choice spoiler
    String choice = array[0][0] + ": " + array[0][1];
    index2 = text.indexOf("</form>", index1);
    buffer.append(text, index1, index2);
    buffer.append("<br><font size=-1>(").append(choice).append(")</font><br/></form>");
    index1 = index2 + 7;

    // Add second choice spoiler
    choice = array[1][0] + ": " + array[1][1];
    index2 = text.indexOf("</form>", index1);
    buffer.append(text, index1, index2);
    buffer.append("<br><font size=-1>(").append(choice).append(")</font><br/></form>");
    index1 = index2 + 7;

    // Add third choice spoiler
    choice = array[2][0] + ": " + array[2][1];
    index2 = text.indexOf("</form>", index1);
    buffer.append(text, index1, index2);
    buffer.append("<br><font size=-1>(").append(choice).append(")</font><br/></form>");
    index1 = index2 + 7;

    // Append remainder of buffer
    buffer.append(text.substring(index1));
  }

  public static final void decorateNunnery(final String urlString, final StringBuffer buffer) {
    // If he's not visiting the nunnery, punt
    if (!urlString.contains("place=nunnery")) {
      return;
    }

    // See if quest is in progress
    if (Preferences.getString("warProgress").equals("finished")
        || !Preferences.getString("sidequestNunsCompleted").equals("none")) {
      // Either the war or quest is over. Punt
      return;
    }

    int tableIndex =
        buffer.indexOf(
            "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>Our Lady of Perpetual Indecision</b></td>");
    if (tableIndex == -1) {
      return;
    }

    String message = IslandDecorator.meatMessage();
    String row =
        "<tr><td><center><table width=100%><tr>"
            + IslandDecorator.progressLineStyle
            + message
            + "</td></tr></table></td></tr>";
    buffer.insert(tableIndex, row);
  }

  public static final void decoratePostwarIsland(final String url, final StringBuffer buffer) {
    // Quest-specific page decorations
    IslandDecorator.decorateArena(url, buffer);

    // Replace all KoL image servers with KoLmafia image cache locations
    for (String path : KoLmafia.IMAGE_SERVER_PATHS) {
      StringUtilities.globalStringReplace(buffer, path, "/images/");
    }

    // Replace sidequest location images for completed quests

    // The arena is available after the war only if the fans of the
    // concert you promoted won the war.
    String arena = IslandManager.questCompleter("sidequestArenaCompleted");
    String winner = IslandManager.warWinner();
    if (arena.equals(winner)) {
      IslandDecorator.sidequestImage(buffer, "sidequestArenaCompleted", Quest.ARENA);
    }

    // If you aided the nuns during the war, they will help you
    // after the war, regardless of who won.
    IslandDecorator.sidequestImage(buffer, "sidequestNunsCompleted", Quest.NUNS);
  }
}
