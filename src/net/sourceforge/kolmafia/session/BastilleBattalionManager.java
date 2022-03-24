package net.sourceforge.kolmafia.session;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BastilleBattalionManager {

  private static Map<String, Setting> imageToSetting = new HashMap<>();
  private static Map<Type, Setting> currentSettings = new HashMap<>();
  private static Map<Integer, Type> optionToType = new HashMap<>();

  public static enum Type {
    BARBICAN(1, "Barbican", "barb"),
    DRAWBRIDGE(2, "Drawbridge", "bridge"),
    MURDER_HOLE(3, "Murder holes", "holes"),
    MOAT(4, "Moat", "moat");

    String name;
    String prefix;
    int option;

    private Type(int option, String name, String prefix) {
      this.option = option;
      this.name = name;
      this.prefix = prefix;
      optionToType.put(this.option, this);
    }

    public String getPrefix() {
      return this.prefix;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static enum Setting {
    BARBEQUE("Barbecue", 1, Type.BARBICAN, 3, 2, 0, 0, 0, 0),
    BABAR("Babar", 2, Type.BARBICAN, 0, 0, 2, 3, 0, 0),
    BARBERSHOP("Barbershop", 3, Type.BARBICAN, 0, 0, 0, 0, 2, 3),

    SHARKS("Sharks", 1, Type.MOAT, 0, 1, 0, 0, 2, 0),
    LAVA("Lava", 2, Type.MOAT, 2, 0, 0, 1, 0, 0),
    TRUTH_SERUM("Truth Serum", 3, Type.MOAT, 0, 0, 2, 0, 0, 1),

    BRUTALIST("Brutalist", 1, Type.DRAWBRIDGE, 2, 0, 1, 0, 2, 0),
    DRAFTSMAN("Draftsman", 2, Type.DRAWBRIDGE, 0, 3, 0, 3, 0, 3),
    ART_NOUVEAU("Art Nouveau", 3, Type.DRAWBRIDGE, 0, 0, 0, 0, 2, 2),

    CANNON("Cannon", 1, Type.MURDER_HOLE, 2, 1, 0, 0, 0, 1),
    CATAPULT("Catapult", 2, Type.MURDER_HOLE, 0, 1, 1, 1, 0, 0),
    GESTURE("Gesture", 3, Type.MURDER_HOLE, 0, 0, 0, 1, 1, 1);

    private Type type;
    private String image;
    private int MA;
    private int MD;
    private int CA;
    private int CD;
    private int PA;
    private int PD;
    private String name;

    private Setting(
        String name, int index, Type type, int MA, int MD, int CA, int CD, int PA, int PD) {
      this.type = type;
      this.image = type.getPrefix() + index + ".png";
      this.MA = MA;
      this.MD = MD;
      this.CA = CA;
      this.CD = CD;
      this.PA = PA;
      this.PD = PD;
      this.name = type + " " + name;
      imageToSetting.put(this.image, this);
    }

    @Override
    public String toString() {
      return this.name;
    }

    public void apply() {
      currentSettings.put(this.type, this);
    }

    public void apply(int[] stats) {
      if (stats.length >= 6) {
        stats[0] += this.MA;
        stats[1] += this.MD;
        stats[2] += this.CA;
        stats[3] += this.CD;
        stats[4] += this.PA;
        stats[5] += this.PD;
      }
    }
  }

  private static Map<String, Castle> imageToCastle = new HashMap<>();

  public static enum Castle {
    ART("frenchcastle", "an avant-garde rt castle"),
    BORING("masterofnone", "a boring, run-of-the-mill castle"),
    CHATEAU("bigcastle", "a sprawling chateau"),
    CITADEL("berserker", "a dark and menacing citadel"),
    FORTIFIED("shieldmaster", "a fortress that puts the 'fort' in 'fortified'"),
    MILITARY("barracks", "an imposing military fortress");

    String prefix;
    String description;

    private Castle(String prefix, String description) {
      this.prefix = prefix;
      this.description = description;
      imageToCastle.put(prefix + "_1.png", this);
      imageToCastle.put(prefix + "_2.png", this);
      imageToCastle.put(prefix + "_3.png", this);
    }

    public String getPrefix() {
      return this.prefix;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

  static {
    // This forces the Setting enum to be initialized, which will populate
    // all the various sets and maps from the constructors.
    Setting[] settings = Setting.values();
    Castle[] castles = Castle.values();
  }

  private static final Pattern BASTILLE_PATTERN = Pattern.compile("You can play <b>(\\d+)</b>");

  // <img style='position: absolute; top: 233; left: 124;'
  // src=https://d2uyhvukfffg5a.cloudfront.net/otherimages/bbatt/needle.png>
  private static final Pattern IMAGE_PATTERN =
      Pattern.compile(
          "<img style='(.*?top: (\\d+).*?; left: (\\d+).*?;.*?)'[^>]*otherimages/bbatt/([^>]*)>");

  private static void parseNeedle(String topString, String leftString) {
    int top = StringUtilities.parseInt(topString);
    int left = StringUtilities.parseInt(leftString);
    switch (top) {
      case 233:
        Preferences.setInteger(
            left < 200 ? "_bastilleMilitaryAttack" : "_bastilleMilitaryDefense", left);
        break;
      case 252:
        Preferences.setInteger(
            left < 200 ? "_bastilleCastleAttack" : "_bastilleCastleDefense", left);
        break;
      case 270:
        Preferences.setInteger(
            left < 200 ? "_bastillePsychologicalAttack" : "_bastillePsychologicalDefense", left);
        break;
      default:
        break;
    }
  }

  public static void parseSettings(String text) {
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      if (image.startsWith("needle")) {
        parseNeedle(matcher.group(2), matcher.group(3));
        continue;
      }
      Setting setting = imageToSetting.get(image);
      if (setting != null) {
        setting.apply();
        continue;
      }
    }
  }

  public static void parseNeedles(String text) {
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      if (!image.startsWith("needle")) {
        continue;
      }
      parseNeedle(matcher.group(2), matcher.group(3));
    }
  }

  public static void parseCastleImage(String text) {
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      Castle castle = imageToCastle.get(image);
      if (castle != null) {
        Preferences.setString("_bastilleEnemyCastle", castle.getPrefix());
        return;
      }
    }
  }

  // *** Game control flow

  private static void startGame() {
    Preferences.setInteger("_bastilleCheeseCollected", 0);
  }

  private static void nextTurn() {
    Preferences.increment("_bastilleGameTurn", 1);
    Preferences.setString("_bastilleChoice1", "");
    Preferences.setString("_bastilleChoice2", "");
    Preferences.setString("_bastilleChoice3", "");
  }

  private static void nextCastle() {
    Preferences.setString("_bastilleEnemyCastle", "");
    Preferences.setString("_bastilleEnemyName", "");
  }

  private static void getChoices(String responseText) {
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    if (choices.size() == 3) {
      Preferences.setString("_bastilleChoice1", choices.get(1));
      Preferences.setString("_bastilleChoice2", choices.get(2));
      Preferences.setString("_bastilleChoice3", choices.get(3));
    }
  }

  private static void endGame() {
    Preferences.setInteger("_bastilleGameTurn", 0);
  }

  // *** Logging

  private static void logLine(String message) {
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  private static void logLine() {
    logLine("");
  }

  private static void logStrength() {
    StringBuilder buf = new StringBuilder();
    buf.append("Military ");
    buf.append(Preferences.getInteger("_bastilleMilitaryAttack"));
    buf.append("/");
    buf.append(Preferences.getInteger("_bastilleMilitaryDefense"));
    buf.append(" ");
    buf.append("Castle ");
    buf.append(Preferences.getInteger("_bastilleCastleAttack"));
    buf.append("/");
    buf.append(Preferences.getInteger("_bastilleCastleDefense"));
    buf.append(" ");
    buf.append("Psychological ");
    buf.append(Preferences.getInteger("_bastillePsychologicalAttack"));
    buf.append("/");
    buf.append(Preferences.getInteger("_bastillePsychologicalDefense"));
    String message = buf.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  // *** Interface for TestCommand (test bastille)

  private static void checkStat(int calculated, String name, String property) {
    int expected = Preferences.getInteger(property);
    if (calculated != expected) {
      logLine(name + " was calculated to be " + calculated + " but is actually " + expected);
    }
  }

  private static AdventureResult SHARK_TOOTH_GRIN = EffectPool.get(EffectPool.SHARK_TOOTH_GRIN);
  private static AdventureResult BOILING_DETERMINATION =
      EffectPool.get(EffectPool.BOILING_DETERMINATION);
  private static AdventureResult ENHANCED_INTERROGATION =
      EffectPool.get(EffectPool.ENHANCED_INTERROGATION);

  public static void checkPredictions() {
    int[] stats = new int[] {124, 240, 124, 240, 125, 240};
    for (Setting setting : currentSettings.values()) {
      setting.apply(stats);
    }
    if (KoLConstants.activeEffects.contains(SHARK_TOOTH_GRIN)) {
      // Boosts military attack and defense in Bastille Battalion.
    }
    if (KoLConstants.activeEffects.contains(BOILING_DETERMINATION)) {
      // Boosts castle attack and defense in Bastille Battalion
    }
    if (KoLConstants.activeEffects.contains(ENHANCED_INTERROGATION)) {
      // Boosts psychological attack and defense in Bastille Battalion.
    }
    checkStat(stats[0], "Military Attack", "_bastilleMilitaryAttack");
    checkStat(stats[1], "Military Defense", "_bastilleMilitaryDefense");
    checkStat(stats[2], "Castle Attack", "_bastilleCastleAttack");
    checkStat(stats[3], "Castle Defense", "_bastilleCastleDefense");
    checkStat(stats[4], "Psychological Attack", "_bastillePsychologicalAttack");
    checkStat(stats[5], "Psychological Defense", "_bastillePsychologicalDefense");
  }

  // *** Interface for AdventureRequest.parseChoiceEncounter

  private static final Pattern CHEESE_PATTERN = Pattern.compile("You gain (\\d+) cheese!");

  public static void gainCheese(final String text) {
    Matcher matcher = CHEESE_PATTERN.matcher(text);
    if (matcher.find()) {
      String message = matcher.group(0);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      int cheese = StringUtilities.parseInt(matcher.group(1));
      Preferences.increment("_bastilleCheeseCollected", cheese);
    }
  }

  public static String parseChoiceEncounter(final int choice, final String responseText) {
    switch (choice) {
      case 1314: // Bastille Battalion (Master of None)
      case 1315: // Castle vs. Castle
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        // Print cheese gain from previous encounter before logging this one.
        BastilleBattalionManager.gainCheese(responseText);
    }
    return null;
  }

  // *** Interface for ChoiceManager

  // According to your scanners, the nearest enemy castle is Humongous Craine, a sprawling chateau.
  private static final Pattern CASTLE_PATTERN =
      Pattern.compile("the nearest enemy castle is (.*?), an? (.*?)\\.");

  public static void visitChoice(final GenericRequest request) {
    if (request.getURLString().equals("choice.php?forceoption=0")) {
      logLine("Entering your Bastille Battalion");
    }

    int choice = ChoiceManager.lastChoice;
    String text = request.responseText;

    switch (choice) {
      case 1313: // Bastille Battalion
        if (!text.contains("option=5")) {
          Preferences.setInteger("_bastilleGames", 5);
        }
        parseSettings(text);
        checkPredictions();
        return;

      case 1314: // Bastille Battalion (Master of None)
        if (Preferences.getInteger("_bastilleGameTurn") == 0) {
          startGame();
        }
        nextTurn();
        Matcher castleMatcher = CASTLE_PATTERN.matcher(text);
        if (castleMatcher.find()) {
          Preferences.setString("_bastilleEnemyName", castleMatcher.group(1));
          parseCastleImage(text);
        }

        parseNeedles(text);
        logStrength();
        return;

      case 1315: // Castle vs. Castle
        nextTurn();
        return;

      case 1316: // GAME OVER
        Matcher matcher = BASTILLE_PATTERN.matcher(text);
        if (matcher.find()) {
          Preferences.setInteger("_bastilleGames", 5 - StringUtilities.parseInt(matcher.group(1)));
        }
        endGame();
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        getChoices(text);
        return;
    }
  }

  public static void postChoice1(final String urlString, final GenericRequest request) {
    int choice = ChoiceManager.lastChoice;
    int decision = ChoiceManager.lastDecision;
    String text = request.responseText;

    switch (choice) {
      case 1313: // Bastille Battalion
        if (decision == 0 || decision > 4) {
          return;
        }
        parseSettings(text);
        logLine(currentSettings.get(optionToType.get(decision)).toString());
        logStrength();
        checkPredictions();
        return;

      case 1314: // Bastille Battalion (Master of None)
        return;

      case 1315: // Castle vs. Castle
        nextCastle();
        return;

      case 1316: // GAME OVER
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        parseNeedles(text);
        logStrength();
        return;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    int choice = ChoiceManager.extractChoiceFromURL(urlString);
    int decision = ChoiceManager.extractOptionFromURL(urlString);

    StringBuilder buf = new StringBuilder();
    switch (choice) {
      case 1313: // Bastille Battalion
        switch (decision) {
          case 1:
            buf.append("Decorating the Barbican");
            break;
          case 2:
            buf.append("Changing the Drawbridge");
            break;
          case 3:
            buf.append("Sizing the Murder Holes");
            break;
          case 4:
            buf.append("Filling the Moat");
            break;
          case 5:
            buf.append("Starting game #");
            buf.append(Preferences.getInteger("_bastilleGames") + 1);
            break;
          case 6:
            // Hi Scores
            return true;
          case 8:
            logLine("Walking away from the game");
            logLine();
            return true;
        }
        break;
      case 1314: // Bastille Battalion (Master of None)
        switch (decision) {
          case 1:
            buf.append("Improving offense.");
            break;
          case 2:
            buf.append("Focusing on defense.");
            break;
          case 3:
            buf.append("Looking for cheese.");
            break;
        }
        break;
      case 1315: // Castle vs. Castle
        switch (decision) {
          case 1:
            buf.append("Charge!");
            break;
          case 2:
            buf.append("Watch warily.");
            break;
          case 3:
            buf.append("Wait to be attacked.");
            break;
        }
        break;
      case 1316: // GAME OVER
        break;
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        switch (decision) {
          case 1:
            buf.append(Preferences.getString("_bastilleChoice1"));
            break;
          case 2:
            buf.append(Preferences.getString("_bastilleChoice2"));
            break;
          case 3:
            buf.append(Preferences.getString("_bastilleChoice3"));
            break;
        }
        break;
    }

    if (buf.length() > 0) {
      logLine(buf.toString());
    }

    return true;
  }
}
