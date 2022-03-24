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
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BastilleBattalionManager {

  private static Map<String, Setting> imageToSetting = new HashMap<>();
  private static Map<Type, Setting> currentSettings = new HashMap<>();

  public static enum Type {
    BARBICAN("Barbican", "barb"),
    DRAWBRIDGE("Drawbridge", "bridge"),
    MOAT("Moat", "moat"),
    MURDER_HOLE("Murder holes", "holes");

    String name;
    String prefix;

    private Type(String name, String prefix) {
      this.name = name;
      this.prefix = prefix;
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

  static {
    // This forces the Setting enum to be initialized, which will populate
    // all the various sets and maps from the constructors.
    Setting[] values = Setting.values();
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

  private static void checkStat(int calculated, String name, String property) {
    int expected = Preferences.getInteger(property);
    if (calculated != expected) {
      RequestLogger.printLine(
          name + " was calculated to be " + calculated + " but is actually " + expected);
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

  public static void visitChoice(final GenericRequest request) {
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
        // *** Parse castle type
        parseNeedles(text);
        logStrength();
        return;

      case 1315: // Castle vs. Castle
        // Chateau - Castle: bigcastle
        // Boring - Castle: masterofnone
        // Fortress - Castle: barracks
        return;

      case 1316: // GAME OVER
        Matcher matcher = BASTILLE_PATTERN.matcher(text);
        if (matcher.find()) {
          Preferences.setInteger("_bastilleGames", 5 - StringUtilities.parseInt(matcher.group(1)));
        }
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        // *** Parse choices?
        return;
    }
  }

  public static void postChoice1(final String urlString, final GenericRequest request) {
    int choice = ChoiceManager.lastChoice;
    int decision = ChoiceManager.lastDecision;
    String text = request.responseText;

    switch (choice) {
      case 1313: // Bastille Battalion
        if (decision >= 1 && decision <= 4) {
          parseSettings(text);
          checkPredictions();
        }
        return;

      case 1314: // Bastille Battalion (Master of None)
        return;

      case 1315: // Castle vs. Castle
        return;

      case 1316: // GAME OVER
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        parseNeedles(text);
        return;
    }
  }

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
}
