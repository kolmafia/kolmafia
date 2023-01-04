package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ElVibratoManager {

  // Avoid useless warning
  private ElVibratoManager() {}

  public record Punchcard(int id, String name, String alias, String tag) {}

  // Verbs

  private static final Punchcard ATTACK =
      new Punchcard(
          ItemPool.PUNCHCARD_ATTACK,
          "El Vibrato punchcard (115 holes)",
          "El Vibrato punchcard (ATTACK)",
          "ATTACK");

  private static final Punchcard REPAIR =
      new Punchcard(
          ItemPool.PUNCHCARD_REPAIR,
          "El Vibrato punchcard (97 holes)",
          "El Vibrato punchcard (REPAIR)",
          "REPAIR");

  private static final Punchcard BUFF =
      new Punchcard(
          ItemPool.PUNCHCARD_BUFF,
          "El Vibrato punchcard (129 holes)",
          "El Vibrato punchcard (BUFF)",
          "BUFF");

  private static final Punchcard MODIFY =
      new Punchcard(
          ItemPool.PUNCHCARD_MODIFY,
          "El Vibrato punchcard (213 holes)",
          "El Vibrato punchcard (MODIFY)",
          "MODIFY");

  private static final Punchcard BUILD =
      new Punchcard(
          ItemPool.PUNCHCARD_BUILD,
          "El Vibrato punchcard (165 holes)",
          "El Vibrato punchcard (BUILD)",
          "BUILD");

  // Objects

  private static final Punchcard TARGET =
      new Punchcard(
          ItemPool.PUNCHCARD_TARGET,
          "El Vibrato punchcard (142 holes)",
          "El Vibrato punchcard (TARGET)",
          "TARGET");

  private static final Punchcard SELF =
      new Punchcard(
          ItemPool.PUNCHCARD_SELF,
          "El Vibrato punchcard (216 holes)",
          "El Vibrato punchcard (SELF)",
          "SELF");

  private static final Punchcard FLOOR =
      new Punchcard(
          ItemPool.PUNCHCARD_FLOOR,
          "El Vibrato punchcard (88 holes)",
          "El Vibrato punchcard (FLOOR)",
          "FLOOR");

  private static final Punchcard DRONE =
      new Punchcard(
          ItemPool.PUNCHCARD_DRONE,
          "El Vibrato punchcard (182 holes)",
          "El Vibrato punchcard (DRONE)",
          "DRONE");

  private static final Punchcard WALL =
      new Punchcard(
          ItemPool.PUNCHCARD_WALL,
          "El Vibrato punchcard (176 holes)",
          "El Vibrato punchcard (WALL)",
          "WALL");

  private static final Punchcard SPHERE =
      new Punchcard(
          ItemPool.PUNCHCARD_SPHERE,
          "El Vibrato punchcard (104 holes)",
          "El Vibrato punchcard (SPHERE)",
          "SPHERE");

  public static Punchcard[] PUNCHCARDS = {
    // Verbs
    ATTACK,
    REPAIR,
    BUFF,
    MODIFY,
    BUILD,
    // Objects
    TARGET,
    SELF,
    FLOOR,
    DRONE,
    WALL,
    SPHERE
  };

  private static final Set<Integer> allPunchcards =
      Arrays.stream(PUNCHCARDS).map(Punchcard::id).collect(Collectors.toSet());

  private static final Map<Integer, Integer> CARD_EXCHANGES =
      Map.ofEntries(
          Map.entry(ItemPool.PUNCHCARD_ATTACK, ItemPool.PUNCHCARD_TARGET),
          Map.entry(ItemPool.PUNCHCARD_TARGET, ItemPool.PUNCHCARD_ATTACK),
          Map.entry(ItemPool.PUNCHCARD_REPAIR, ItemPool.PUNCHCARD_SELF),
          Map.entry(ItemPool.PUNCHCARD_SELF, ItemPool.PUNCHCARD_REPAIR),
          Map.entry(ItemPool.PUNCHCARD_FLOOR, ItemPool.PUNCHCARD_BUFF),
          Map.entry(ItemPool.PUNCHCARD_BUFF, ItemPool.PUNCHCARD_FLOOR),
          Map.entry(ItemPool.PUNCHCARD_DRONE, ItemPool.PUNCHCARD_MODIFY),
          Map.entry(ItemPool.PUNCHCARD_MODIFY, ItemPool.PUNCHCARD_DRONE),
          Map.entry(ItemPool.PUNCHCARD_BUILD, ItemPool.PUNCHCARD_WALL),
          Map.entry(ItemPool.PUNCHCARD_WALL, ItemPool.PUNCHCARD_BUILD));

  private static final Map<String, Construct> nameToConstruct = new HashMap<>();

  private static boolean isElVibratoConstruct(MonsterData monster) {
    return monster != null && nameToConstruct.containsKey(monster.getName());
  }

  private record Command(Punchcard card1, Punchcard card2, AdventureResult object, String desc) {}

  private static final AdventureResult POWER_SPHERE = ItemPool.get(ItemPool.POWER_SPHERE);
  private static final AdventureResult EV_DRONE = ItemPool.get(ItemPool.DRONE);
  private static final AdventureResult BROKEN_DRONE = ItemPool.get(ItemPool.BROKEN_DRONE);
  private static final AdventureResult REPAIRED_DRONE = ItemPool.get(ItemPool.REPAIRED_DRONE);
  private static final AdventureResult AUGMENTED_DRONE = ItemPool.get(ItemPool.AUGMENTED_DRONE);

  public enum Construct {
    BIZARRE("bizarre"),
    HULKING("hulking"),
    INDUSTRIOUS("industrious"),
    LONELY("lonely"),
    MENACING("menacing"),
    TOWERING("towering");

    private final List<Command> commands = new ArrayList<>();

    private Construct(String type) {
      nameToConstruct.put(type + " construct", this);
      nameToConstruct.put(type + " construct (translated)", this);
    }

    public List<Command> getCommands() {
      return this.commands;
    }

    public void addCommand(Punchcard card1, Punchcard card2, AdventureResult object, String desc) {
      commands.add(new Command(card1, card2, object, desc));
    }
  }

  static {
    Construct.BIZARRE.addCommand(BUFF, DRONE, REPAIRED_DRONE, "-> augmented drone");
    Construct.BIZARRE.addCommand(BUFF, TARGET, null, "10 turns of Fitter, Happier");
    Construct.BIZARRE.addCommand(BUFF, SELF, null, "augments construct");
    Construct.BIZARRE.addCommand(REPAIR, TARGET, null, "heals you");
    Construct.BIZARRE.addCommand(REPAIR, SELF, null, "heals construct");
    Construct.HULKING.addCommand(ATTACK, FLOOR, null, "get punchcards");
    Construct.HULKING.addCommand(ATTACK, WALL, null, "get punchcards (can include SPHERE)");
    Construct.HULKING.addCommand(ATTACK, SELF, null, "destroys construct");
    Construct.HULKING.addCommand(ATTACK, TARGET, null, "damages you construct");
    Construct.HULKING.addCommand(BUILD, SELF, null, "augments construct");
    Construct.INDUSTRIOUS.addCommand(BUFF, FLOOR, null, "no effect");
    Construct.INDUSTRIOUS.addCommand(BUFF, WALL, null, "no effect");
    Construct.INDUSTRIOUS.addCommand(BUFF, TARGET, null, "damages you");
    Construct.INDUSTRIOUS.addCommand(BUFF, SELF, null, "damages construct");
    Construct.LONELY.addCommand(MODIFY, SPHERE, POWER_SPHERE, "-> overcharged power sphere");
    Construct.LONELY.addCommand(REPAIR, DRONE, BROKEN_DRONE, "-> repaired drone");
    Construct.LONELY.addCommand(REPAIR, SELF, null, "manipulates construct");
    Construct.LONELY.addCommand(REPAIR, TARGET, null, "damages you");
    Construct.MENACING.addCommand(ATTACK, WALL, null, "no effect");
    Construct.MENACING.addCommand(ATTACK, FLOOR, null, "no effect");
    Construct.MENACING.addCommand(ATTACK, SELF, null, "damages construct");
    Construct.MENACING.addCommand(ATTACK, TARGET, null, "damages you");
    Construct.TOWERING.addCommand(BUILD, DRONE, null, "get El Vibrato drone");
    Construct.TOWERING.addCommand(BUILD, TARGET, null, "no effect");
    Construct.TOWERING.addCommand(MODIFY, SELF, null, "transform into a new construct");
    Construct.TOWERING.addCommand(MODIFY, DRONE, EV_DRONE, "-> broken drone");
    Construct.TOWERING.addCommand(MODIFY, SPHERE, POWER_SPHERE, "-> El Vibrato outfit item");
  }

  private static void addCommandButton(
      final StringBuilder buffer, Command command, boolean enabled) {
    buffer.append("<form method=POST action=\"fight.php\"><td>");
    buffer.append("<input type=hidden name=\"action\" value=\"macro\">");
    buffer.append("<input type=hidden name=\"macrotext\" value=\"");

    int id1 = command.card1().id();
    int id2 = command.card2().id();

    buffer.append("use ");
    buffer.append(id1);
    if (KoLCharacter.hasSkill(SkillPool.AMBIDEXTROUS_FUNKSLINGING)) {
      buffer.append(",");
    } else {
      buffer.append("; use ");
    }
    buffer.append(id2);

    buffer.append("\"><input onclick=\"return killforms(this);\" type=\"submit\" value=\"");
    buffer.append("COMMAND!");
    buffer.append("\"");
    if (!enabled) {
      buffer.append(" disabled");
    }

    buffer.append(">&nbsp;</td></form>");
  }

  private static void addMonsterCommands(MonsterData monster, StringBuilder buffer) {
    Construct construct = nameToConstruct.get(monster.getName());
    for (var command : construct.getCommands()) {
      int card1 = command.card1().id();
      int card1Count = InventoryManager.getCount(card1);
      int card2 = command.card2().id();
      int card2Count = InventoryManager.getCount(card2);
      AdventureResult item = command.object();
      int itemCount = item == null ? 0 : InventoryManager.getCount(item);

      boolean enabled = card1Count > 0 && card2Count > 0;
      if (item != null) {
        enabled &= itemCount > 0;
      }

      buffer.append("<tr>");

      // Add a button
      addCommandButton(buffer, command, enabled);

      // Verb
      buffer.append("<td>");
      buffer.append(command.card1().tag());
      buffer.append(" (");
      buffer.append(card1Count);
      buffer.append(")");
      buffer.append("</td>");

      // Object
      buffer.append("<td>");
      buffer.append(command.card2().tag());
      buffer.append(" (");
      buffer.append(card2Count);
      buffer.append(")");
      buffer.append("</td>");

      // Item
      buffer.append("<td>");
      if (item != null) {
        buffer.append(item.getName());
        buffer.append(" (");
        buffer.append(itemCount);
        buffer.append(")");
      } else {
        buffer.append("&nbsp;");
      }
      buffer.append("</td>");

      // description
      buffer.append("<td>");
      buffer.append(command.desc());
      buffer.append("</td>");

      buffer.append("</tr>");
    }
  }

  private static void generateTable(StringBuilder buffer, MonsterData monster) {
    buffer.append("<table border=2 cols=5>");
    addMonsterCommands(monster, buffer);
    buffer.append("</table>");
  }

  public static void decorate(final StringBuffer buffer) {
    // If we are not on El Vibrato Island, nothing to do.
    KoLAdventure location = KoLAdventure.lastVisitedLocation;
    if (location == null || location.getAdventureNumber() != AdventurePool.EL_VIBRATO_ISLAND) {
      return;
    }

    // If the fight is over, punt
    if (FightRequest.getCurrentRound() == 0) {
      return;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (!isElVibratoConstruct(monster)) {
      // Unfortunately, wanders can intrude
      return;
    }

    int index = buffer.lastIndexOf("</table></center></td>");
    if (index != -1) {
      StringBuilder table = new StringBuilder("<tr><td><center>");
      generateTable(table, monster);
      table.append("</center></td></tr>");
      buffer.insert(index, table);
    }
  }

  private static final Pattern WHICHCARD_PATTERN = Pattern.compile("whichcard=(\\d+)");

  private static AdventureResult extractCard(String urlString) {
    Matcher matcher = WHICHCARD_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return null;
    }
    int cardId = StringUtilities.parseInt(matcher.group(1));
    if (!allPunchcards.contains(cardId)) {
      return null;
    }
    return ItemPool.get(cardId);
  }

  public static void parseResponse(String urlString, String responseText) {
    var card = extractCard(urlString);
    if (card == null) {
      return;
    }
    ResultProcessor.removeItem(card.getItemId());
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("elvmachine.php")) {
      return false;
    }

    // elvmachine.php
    // elvmachine.php?action=slot&whichcard=3151
    // elvmachine.php?action=button

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return true;
    }

    String message = null;

    switch (action) {
      case "slot":
        var card = extractCard(urlString);
        if (card == null) {
          return true;
        }
        message = "Inserting a " + card.getName() + " into the slot.";
        break;
      case "button":
        message = "Pushing the button.";
        break;
      default:
        return true;
    }

    if (message != null) {
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }
}
