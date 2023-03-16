package net.sourceforge.kolmafia.session;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;

public class RufusManager {
  private RufusManager() {}

  // First thing.  There's a big shadow entity.  In the place those rifts connect to.
  //
  // It's a big spire.  Kinda like a radio tower.
  // It's an orrery.  It probably looks like a model of a solar system, only gigantic.
  // It's a tongue.  Just... a big tongue, as far as I can tell.
  // It's something like a scythe.  Maybe just... a scythe.  But a big one.
  // It's a cauldron.  Big.  Shaped like an iron one, but not made of iron.
  // It's a... matrix.  A manifold.  It's kind of a... grid?  You'll know it when you see it.
  //
  // I've also detected an artifact I need somebody to recover for me.
  //
  // A shadow lighter,
  // A shadow heptahedron,
  // A shadow snowflake,
  // A shadow heart,
  // A shadow bucket,
  // A shadow wave,
  //
  // I can also always use samples of more mundane items from the rifts.
  //
  // Right now, 3 handfuls of shadow venom would be valuable.
  // Right now, 3 shadow bricks would be valuable.

  private static final Pattern ENTITY =
      Pattern.compile("(spire|orrery|tongue|scythe|cauldron|matrix)");
  private static final Pattern ARTIFACT =
      Pattern.compile("(lighter|heptahedron|snowflake|heart|bucket|wave)");
  private static final Pattern ITEMS = Pattern.compile("Right now, 3 (.*?) would be valuable");

  public static void parseCall(final String text) {
    var entityMatcher = ENTITY.matcher(text);
    if (entityMatcher.find()) {
      String entity = "shadow " + entityMatcher.group(1);
      Preferences.setString("rufusDesiredEntity", entity);
    }
    var artifactMatcher = ARTIFACT.matcher(text);
    if (artifactMatcher.find()) {
      String artifact = "shadow " + artifactMatcher.group(1);
      Preferences.setString("rufusDesiredArtifact", artifact);
    }
    var itemMatcher = ITEMS.matcher(text);
    if (itemMatcher.find()) {
      // This will be a plural name
      String items = itemMatcher.group(1);
      // Convert to itemId and back to non-plural item name
      int itemId = ItemDatabase.getItemId(items, 3);
      String itemName = ItemDatabase.getItemName(itemId);
      Preferences.setString("rufusDesiredItems", itemName);
    }
  }

  public static void parseCallBack(final String text) {}

  public static void parseCallResponse(final String text, int option) {
    switch (option) {
      case 6 -> {
        // Hang up
        QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED);
        return;
      }
      case 1 -> {
        Preferences.setString("rufusQuestType", "entity");
        Preferences.setString("rufusQuestTarget", Preferences.getString("rufusDesiredEntity"));
        QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.STARTED);
      }
      case 2 -> {
        // You tell Rufus you'll retrieve the artifact for him.
        Preferences.setString("rufusQuestType", "artifact");
        Preferences.setString("rufusQuestTarget", Preferences.getString("rufusDesiredArtifact"));
        QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.STARTED);
      }
      case 3 -> {
        Preferences.setString("rufusQuestType", "items");
        String itemName = Preferences.getString("rufusDesiredItems");
        Preferences.setString("rufusQuestTarget", itemName);
        handleShadowItems(ItemDatabase.getExactItemId(itemName));
      }
    }
    if (text.contains("Shadow Affinity")) {
      Preferences.setBoolean("_shadowAffinityToday", true);
    }
    Preferences.setString("rufusDesiredEntity", "");
    Preferences.setString("rufusDesiredArtifact", "");
    Preferences.setString("rufusDesiredItems", "");
  }

  public static void parseCallBackResponse(final String text, int option) {
    switch (option) {
      case 1 -> {
        // "Yeah, I got it."
        if (text.contains("Rufus's shadow lodestone")) {
          switch (Preferences.getString("rufusQuestType")) {
            case "artifact" -> {
              String artifact = Preferences.getString("rufusQuestTarget");
              int itemId = ItemDatabase.getExactItemId(artifact);
              ResultProcessor.removeItem(itemId);
            }
            case "items" -> {
              String item = Preferences.getString("rufusQuestTarget");
              int itemId = ItemDatabase.getExactItemId(item);
              ResultProcessor.processResult(ItemPool.get(itemId, -3));
            }
          }
          QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED);
          Preferences.setString("rufusQuestType", "");
          Preferences.setString("rufusQuestTarget", "");
        }
      }
      case 5 -> {
        // "I have not got it."
      }
      case 6 -> {
        // Hang up
        return;
      }
    }
  }

  public static void handleShadowItems(String itemName) {
    if (QuestDatabase.isQuestStarted(Quest.RUFUS)
        && Preferences.getString("rufusQuestType").equals("items")
        && Preferences.getString("rufusQuestTarget").equals(itemName)) {
      handleShadowItems(ItemDatabase.getExactItemId(itemName));
    }
  }

  private static void handleShadowItems(int itemId) {
    int count = InventoryManager.getCount(itemId);
    QuestDatabase.setQuestProgress(Quest.RUFUS, (count >= 3) ? "step1" : QuestDatabase.STARTED);
  }

  //  Rufus wants you to go into a Shadow Rift and defeat a shadow scythe.
  //  Call Rufus and let him know you defeated that monster.
  public static final Pattern RUFUS_ENTITY_PATTERN = Pattern.compile("defeat a (.*?)\\.");

  //  Rufus wants you to go into a Shadow Rift and find a shadow bucket.
  //  Call Rufus and tell him you found his shadow bucket.
  public static final Pattern RUFUS_ARTIFACT_PATTERN = Pattern.compile("find a (.*?)\\.");

  //  Rufus wants you to find him 3 wisps of shadow flame from Shadow Rifts.
  //  Call Rufus and tell him you've got the 3 wisps of shadow flame he wanted.
  public static final Pattern RUFUS_ITEMS_PATTERN =
      Pattern.compile("find him 3 (.*?) from Shadow Rifts\\.");

  public static String handleQuestLog(String details) {
    details = details.trim();
    if (details.startsWith("Rufus wants you")) {
      // The quest has been started
      Matcher entityMatcher = RUFUS_ENTITY_PATTERN.matcher(details);
      if (entityMatcher.find()) {
        Preferences.setString("rufusQuestType", "entity");
        Preferences.setString("rufusQuestTarget", entityMatcher.group(1));
        return QuestDatabase.STARTED;
      }
      Matcher artifactMatcher = RUFUS_ARTIFACT_PATTERN.matcher(details);
      if (artifactMatcher.find()) {
        Preferences.setString("rufusQuestType", "artifact");
        Preferences.setString("rufusQuestTarget", artifactMatcher.group(1));
        return QuestDatabase.STARTED;
      }
      Matcher itemsMatcher = RUFUS_ITEMS_PATTERN.matcher(details);
      if (itemsMatcher.find()) {
        Preferences.setString("rufusQuestType", "items");
        String items = itemsMatcher.group(1);
        // Convert to itemId and back to non-plural item name
        int itemId = ItemDatabase.getItemId(items, 3);
        String itemName = ItemDatabase.getItemName(itemId);
        Preferences.setString("rufusQuestTarget", itemName);
        return QuestDatabase.STARTED;
      }
      // This should not be possible
      return QuestDatabase.STARTED;
    }
    if (details.startsWith("Call Rufus")) {
      // You have done what Rufus wanted
      return "step1";
    }
    // This should not be possible
    return QuestDatabase.UNSTARTED;
  }

  // Support for The Shadow Labyrinth.
  //
  // This is a (turn-free) scheduled choice adventure in the Shadow Rift
  // It appears even if you don't have a quest from Rufus, but is
  // modified by such.
  //
  // (no quest) - unmodified
  // entity - replaced by a boss fight
  // artifact - one of the options is replaced by the artifact
  // items - unmodified
  //
  // Support for it is a fair chunk of code, so it's here, rather than
  // in the massive ChoiceAdventures package.

  public enum ShadowTheme {
    FIRE("90-100 Muscle substats", "muscle", "shadow lighter"),
    MATH("90-100 Mysticality substats", "mysticality", "shadow heptahedron"),
    WATER("90-100 Moxie substats", "moxie", "shadow bucket"),
    TIME("+3 turns to 3 random effects", "effects", null),
    BLOOD("30 Shadow's Heart: Maximum HP +300%", "maxHP", "shadow heart"),
    COLD("30 Shadow's Chill: Maximum MP +300%", "maxMP", "shadow snowflake"),
    GHOST(
        "30 Shadow's Thickness: Superhuman (+5) Spooky, Hot, Sleaze resistance",
        "resistance",
        "shadow wave");

    // Class fields
    final String normal;
    final String goal;
    final String artifact;

    // Lookups for Shadow Themes
    private static final Map<String, ShadowTheme> goalToTheme = new HashMap<>();
    private static final Map<String, ShadowTheme> artifactToTheme = new HashMap<>();

    ShadowTheme(String normal, String goal, String artifact) {
      this.normal = normal;
      this.goal = goal;
      this.artifact = artifact;
    }

    public String getNormal() {
      return this.normal;
    }

    public String getArtifact() {
      return this.artifact;
    }

    public void populateMaps() {
      ShadowTheme.goalToTheme.put(this.goal, this);
      if (artifact != null) {
        ShadowTheme.artifactToTheme.put(this.artifact, this);
      }
    }

    public static ShadowTheme findGoal(String goal) {
      return ShadowTheme.goalToTheme.get(goal);
    }

    public static ShadowTheme findArtifact(String artifact) {
      return ShadowTheme.artifactToTheme.get(artifact);
    }
  }

  static {
    for (var theme : EnumSet.allOf(ShadowTheme.class)) {
      theme.populateMaps();
    }
  }

  // Adjectives for ShadowThemes
  private static final Map<String, ShadowTheme> adjectiveToTheme = new HashMap<>();

  private static void addAdjectives(ShadowTheme theme, String... adjectives) {
    for (String adjective : adjectives) {
      adjectiveToTheme.put(adjective, theme);
    }
  }

  static {
    addAdjectives(
        ShadowTheme.FIRE,
        "blazing",
        "blistering",
        "burning",
        "burnt",
        "charred",
        "ember-lit",
        "flame-choked",
        "scalded",
        "scalding",
        "scorched",
        "scorching",
        "seared",
        "singed",
        "sizzling",
        "smoldering",
        "steaming",
        "white-hot");
    addAdjectives(
        ShadowTheme.MATH,
        "algebraic",
        "angular",
        "binomial",
        "boolean",
        "Cartesian",
        "cubic",
        "decimal",
        "divided",
        "Euclidean",
        "exponential",
        "Fibonacci",
        "fractal",
        "fractional",
        "geometric",
        "hyperbolic",
        "integer",
        "irrational",
        "logarithmic",
        "monomial",
        "multiplicative",
        "ordinal",
        "parabolic",
        "periodic",
        "prime",
        "Pythagorean",
        "quadratic",
        "Riemannian",
        "self-referential",
        "sinusoidal",
        "trigonometric",
        "vector");
    addAdjectives(
        ShadowTheme.WATER,
        "aqueous",
        "damp",
        "drenched",
        "dripping",
        "drowned",
        "drowning",
        "foggy",
        "humid",
        "moist",
        "runny",
        "soaked",
        "sodden",
        "underwater",
        "wet",
        "water-logged",
        "watery");
    addAdjectives(
        ShadowTheme.TIME,
        "ancient",
        "antique",
        "broken-down",
        "crumbling",
        "decaying",
        "derelict",
        "dilapidated",
        "old",
        "ramshackle",
        "rickety",
        "ruined",
        "shabby",
        "unkempt");
    addAdjectives(
        ShadowTheme.BLOOD,
        "bleeding",
        "blood-drenched",
        "blood-soaked",
        "bloodstained",
        "bloody",
        "crimson",
        "hematic",
        "pulsing",
        "sanguine",
        "vein-shot",
        "veiny");
    addAdjectives(
        ShadowTheme.COLD,
        "arctic",
        "chilly",
        "cold-numbed",
        "freezing",
        "frigid",
        "frost-rimed",
        "frosty",
        "frozen",
        "hyperborean",
        "iced-over",
        "icy",
        "snow-covered",
        "wintry");
    addAdjectives(
        ShadowTheme.GHOST,
        "diaphanous",
        "ephemeral",
        "ghostly",
        "gossamer",
        "half-there",
        "insubstantial",
        // The only "two word" adjective
        "nearly invisible",
        "see-through",
        "spectral",
        "translucent",
        "transparent",
        "wispy");
  }

  public static ShadowTheme shadowLabyrinthTheme(String text) {
    // VERB the ADJECTIVE NOUN
    //
    // VERB is an action phrase
    // ADJECTIVE is a single word - with one known exception
    // NOUN is a single-word destination

    int the = text.indexOf(" the ");
    int place = text.lastIndexOf(" ");
    if (the == -1 || place == -1) {
      return null;
    }

    // String verb = text.substring(0, the);
    String adjective = text.substring(the + 5, place);
    // String noun = text.substring(place + 1);

    return adjectiveToTheme.get(adjective);
  }

  public static ChoiceOption shadowLabyrinthSpoiler(String text) {
    ShadowTheme theme = shadowLabyrinthTheme(text);
    String spoiler = "unknown theme";
    if (theme != null) {
      spoiler = theme.getNormal();
      if (Preferences.getString("rufusQuestType").equals("artifact")
          && QuestDatabase.isQuestStep(Quest.RUFUS, QuestDatabase.STARTED)) {
        String target = Preferences.getString("rufusQuestTarget");
        String artifact = theme.getArtifact();
        if (target.equals(artifact)) {
          spoiler = artifact;
        }
      }
    }
    return new ChoiceOption(spoiler);
  }

  public static String specialChoiceDecision(int choice, String responseText) {
    switch (choice) {
      case 1498 -> {
        // Calling Rufus Back
        boolean finished = QuestDatabase.isQuestStep(Quest.RUFUS, "step1");
        // If you have accomplished the goal, finish quest.
        // Otherwise, rudely hang up on Rufus
        return finished ? "1" : "6";
      }
      case 1499 -> {
        // A Labyrinth of Shadows
        return shadowLabyrinthChoiceDecision(responseText);
      }
    }
    return "0";
  }

  // Automation of the Labyrinth of Shadows
  private static String shadowLabyrinthChoiceDecision(String responseText) {
    boolean artifact =
        QuestDatabase.isQuestStep(Quest.RUFUS, QuestDatabase.STARTED)
            && Preferences.getString("rufusQuestType").equals("artifact");
    ShadowTheme needed =
        artifact
            ? ShadowTheme.findArtifact(Preferences.getString("rufusQuestTarget"))
            : ShadowTheme.findGoal(Preferences.getString("shadowLabyrinthGoal"));
    if (needed == null) {
      // Show in browser
      return "0";
    }

    // See what options are on offer in the responseText
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);

    for (int i = 2; i <= 4; ++i) {
      ShadowTheme offered = shadowLabyrinthTheme(choices.get(i));
      if (offered == needed) {
        return String.valueOf(i);
      }
    }

    // The desired theme is not available. Tell KoL to randomize again
    return "1";
  }
}
