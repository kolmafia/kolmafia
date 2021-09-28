package net.sourceforge.kolmafia;

import java.util.HashMap;
import java.util.Map;

public class PokefamData implements Comparable<PokefamData> {
  private final String race;
  private int power2;
  private int hp2;
  private int power3;
  private int hp3;
  private int power4;
  private int hp4;
  private String move1;
  private String move2;
  private String move3;
  private String attribute;

  public static final String UNKNOWN = "Unknown";

  public static final String BITE = "Bite";
  public static final String BONK = "Bonk";
  public static final String CLAW = "Claw";
  public static final String PECK = "Peck";
  public static final String PUNCH = "Punch";
  public static final String STING = "Sting";

  private static final Map<String, String> MOVE1 = new HashMap<String, String>();

  static {
    MOVE1.put(UNKNOWN, UNKNOWN);
    MOVE1.put(BITE, BITE);
    MOVE1.put(BONK, BONK);
    MOVE1.put(CLAW, CLAW);
    MOVE1.put(PECK, PECK);
    MOVE1.put(PUNCH, PUNCH);
    MOVE1.put(STING, STING);
  }

  public static final String ARMOR_UP = "Armor Up";
  public static final String BACKSTAB = "Backstab";
  public static final String BREATHE_FIRE = "Breathe Fire";
  public static final String CHILL_OUT = "Chill Out";
  public static final String EMBARRASS = "Embarrass";
  public static final String ENCOURAGE = "Encourage";
  public static final String FRIGHTEN = "Frighten";
  public static final String GROWL = "Growl";
  public static final String HOWL = "Howl";
  public static final String HUG = "Hug";
  public static final String LASER_BEAM = "Laser Beam";
  public static final String LICK = "Lick";
  public static final String REGROW = "Regrow";
  public static final String RETREAT = "Retreat";
  public static final String SPLASH = "Splash";
  public static final String STINKBLAST = "Stinkblast";
  public static final String SWOOP = "Swoop";
  public static final String TACKLE = "Tackle";

  private static final Map<String, String> MOVE2 = new HashMap<String, String>();

  static {
    MOVE2.put(UNKNOWN, UNKNOWN);
    MOVE2.put(ARMOR_UP, ARMOR_UP);
    MOVE2.put(BACKSTAB, BACKSTAB);
    MOVE2.put(BREATHE_FIRE, BREATHE_FIRE);
    MOVE2.put(CHILL_OUT, CHILL_OUT);
    MOVE2.put(EMBARRASS, EMBARRASS);
    MOVE2.put(ENCOURAGE, ENCOURAGE);
    MOVE2.put(FRIGHTEN, FRIGHTEN);
    MOVE2.put(GROWL, GROWL);
    MOVE2.put(HOWL, HOWL);
    MOVE2.put(HUG, HUG);
    MOVE2.put(LASER_BEAM, LASER_BEAM);
    MOVE2.put(LICK, LICK);
    MOVE2.put(REGROW, REGROW);
    MOVE2.put(RETREAT, RETREAT);
    MOVE2.put(SPLASH, SPLASH);
    MOVE2.put(STINKBLAST, STINKBLAST);
    MOVE2.put(SWOOP, SWOOP);
    MOVE2.put(TACKLE, TACKLE);
  }

  public static final String BEAR_HUG = "Bear Hug";
  public static final String BLOOD_BATH = "Blood Bath";
  public static final String DEFENSE_MATRIX = "Defense Matrix";
  public static final String DELUXE_IMPALE = "Deluxe Impale";
  public static final String EMPOWERING_CHEER = "Empowering Cheer";
  public static final String HEALING_RAIN = "Healing Rain";
  public static final String NASTY_CLOUD = "Nasty Cloud";
  public static final String NUCLEAR_BOMB = "Nuclear Bomb";
  public static final String OWL_STARE = "Owl Stare";
  public static final String PEPPERSCORN = "Pepperscorn";
  public static final String RAINBOW_STORM = "Rainbow Storm";
  public static final String SPIKY_BURST = "Spiky Burst";
  public static final String STICK_TREATS = "Stick Treats";
  public static final String UNIVERSAL_BACKRUB = "Universal Backrub";
  public static final String VIOLENT_SHRED = "Violent Shred";
  public static final String VULGAR_DISPLAY = "Vulgar Display";

  private static final Map<String, String> MOVE3 = new HashMap<String, String>();

  static {
    MOVE3.put(UNKNOWN, UNKNOWN);
    MOVE3.put(BEAR_HUG, BEAR_HUG);
    MOVE3.put(BLOOD_BATH, BLOOD_BATH);
    MOVE3.put(DEFENSE_MATRIX, DEFENSE_MATRIX);
    MOVE3.put(DELUXE_IMPALE, DELUXE_IMPALE);
    MOVE3.put(EMPOWERING_CHEER, EMPOWERING_CHEER);
    MOVE3.put(HEALING_RAIN, HEALING_RAIN);
    MOVE3.put(NASTY_CLOUD, NASTY_CLOUD);
    MOVE3.put(NUCLEAR_BOMB, NUCLEAR_BOMB);
    MOVE3.put(OWL_STARE, OWL_STARE);
    MOVE3.put(PEPPERSCORN, PEPPERSCORN);
    MOVE3.put(RAINBOW_STORM, RAINBOW_STORM);
    MOVE3.put(SPIKY_BURST, SPIKY_BURST);
    MOVE3.put(STICK_TREATS, STICK_TREATS);
    MOVE3.put(UNIVERSAL_BACKRUB, UNIVERSAL_BACKRUB);
    MOVE3.put(VIOLENT_SHRED, VIOLENT_SHRED);
    MOVE3.put(VULGAR_DISPLAY, VULGAR_DISPLAY);
  }

  public static final String NONE = "None";
  public static final String ARMOR = "Armor";
  public static final String REGENERATING = "Regenerating";
  public static final String SMART = "Smart";
  public static final String SPIKED = "Spiked";

  private static final Map<String, String> ATTRIBUTES = new HashMap<String, String>();

  static {
    ATTRIBUTES.put(UNKNOWN, UNKNOWN);
    ATTRIBUTES.put(NONE, NONE);
    ATTRIBUTES.put(ARMOR, ARMOR);
    ATTRIBUTES.put(REGENERATING, REGENERATING);
    ATTRIBUTES.put(SMART, SMART);
    ATTRIBUTES.put(SPIKED, SPIKED);
  }

  public PokefamData(
      final String race,
      final String level2,
      final String level3,
      final String level4,
      final String move1,
      final String move2,
      final String move3,
      final String attribute) {

    this(
        race,
        level2Power(level2),
        level2HP(level2),
        level2Power(level3),
        level2HP(level3),
        level2Power(level4),
        level2HP(level4),
        move1,
        move2,
        move3,
        attribute);
  }

  private static int level2Power(final String level) {
    if (level != null && level.length() > 0) {
      switch (level.charAt(0)) {
        case '1':
          return 1;
        case '2':
          return 2;
        case '3':
          return 3;
        case '4':
          return 4;
      }
    }
    return 0;
  }

  private static int level2HP(final String level) {
    if (level != null && level.length() > 2) {
      switch (level.charAt(2)) {
        case '1':
          return 1;
        case '2':
          return 2;
        case '3':
          return 3;
        case '4':
          return 4;
        case '5':
          return 5;
      }
    }
    return 0;
  }

  public PokefamData(
      final String race,
      final int power2,
      final int hp2,
      final int power3,
      final int hp3,
      final int power4,
      final int hp4,
      final String move1,
      final String move2,
      final String move3,
      final String attribute) {

    this.race = race;
    this.power2 = power2;
    this.hp2 = hp2;
    this.power3 = power3;
    this.hp3 = hp3;
    this.power4 = power4;
    this.hp4 = hp4;
    this.move1 = normalizeMove1(move1, race);
    this.move2 = normalizeMove2(move2, race);
    this.move3 = normalizeMove3(move3, race);
    this.attribute = normalizeAttribute(attribute, race);
  }

  private static String normalizeMove1(String move1, String race) {
    String value = MOVE1.get(move1);
    if (value == null) {
      RequestLogger.printLine("Unknown move1 (" + move1 + ") for familiar " + race);
      value = move1;
    }
    return value;
  }

  private static String normalizeMove2(String move2, String race) {
    String value = MOVE2.get(move2);
    if (value == null) {
      RequestLogger.printLine("Unknown move2 (" + move2 + ") for familiar " + race);
      value = move2;
    }
    return value;
  }

  private static String normalizeMove3(String move3, String race) {
    String value = MOVE3.get(move3);
    if (value == null) {
      RequestLogger.printLine("Unknown move3 (" + move3 + ") for familiar " + race);
      value = move3;
    }
    return value;
  }

  private static String normalizeAttribute(String attribute, String race) {
    String value = ATTRIBUTES.get(attribute);
    if (value == null) {
      RequestLogger.printLine("Unknown attribute (" + attribute + ") for familiar " + race);
      value = attribute;
    }
    return value;
  }

  public String getRace() {
    return this.race;
  }

  public int getPower2() {
    return this.power2;
  }

  public void setPower2(final int power2) {
    this.power2 = power2;
  }

  public int getHP2() {
    return this.hp2;
  }

  public void setHP2(final int hp2) {
    this.hp2 = hp2;
  }

  public int getPower3() {
    return this.power3;
  }

  public void setPower3(final int power3) {
    this.power3 = power3;
  }

  public int getHP3() {
    return this.hp3;
  }

  public void setHP3(final int hp3) {
    this.hp3 = hp3;
  }

  public int getPower4() {
    return this.power4;
  }

  public void setPower4(final int power4) {
    this.power4 = power4;
  }

  public int getHP4() {
    return this.hp4;
  }

  public void setHP4(final int hp4) {
    this.hp4 = hp4;
  }

  public String getMove1() {
    return this.move1;
  }

  public void setMove1(final String move1) {
    this.move1 = normalizeMove1(move1, this.race);
  }

  public String getMove2() {
    return this.move2;
  }

  public void setMove2(final String move2) {
    this.move2 = normalizeMove2(move2, this.race);
  }

  public String getMove3() {
    return this.move3;
  }

  public void setMove3(final String move3) {
    this.move3 = normalizeMove3(move3, this.race);
  }

  public String getAttribute() {
    return this.attribute;
  }

  public void setAttribute(final String attribute) {
    this.attribute = normalizeAttribute(attribute, this.race);
  }

  @Override
  public String toString() {
    return this.race;
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof PokefamData && this.race.equals(((PokefamData) o).race);
  }

  @Override
  public int hashCode() {
    return this.race.hashCode();
  }

  @Override
  public int compareTo(final PokefamData pf) {
    return this.race.compareToIgnoreCase(pf.race);
  }
}
