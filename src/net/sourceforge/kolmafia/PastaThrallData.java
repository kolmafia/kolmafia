package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PastaThrallData implements Comparable<PastaThrallData> {
  public static class PastaThrallType {
    public String name;
    public int id;
    public String settingName;
    public int skill;
    public Pattern firstSummonPattern;
    public Pattern subsequentSummonPattern;
    public String image;
    public String tinyImage;
    public String level1Power;
    public String level5Power;
    public String level10Power;
    public String canonicalName;

    public PastaThrallType(
        final String name,
        final int id,
        final String settingName,
        final int skill,
        final Pattern firstSummonPattern,
        final Pattern subsequentSummonPattern,
        final String image,
        final String tinyImage,
        final String level1Power,
        final String level5Power,
        final String level10Power,
        final String canonicalName) {
      this.name = name;
      this.id = id;
      this.settingName = settingName;
      this.skill = skill;
      this.firstSummonPattern = firstSummonPattern;
      this.subsequentSummonPattern = subsequentSummonPattern;
      this.image = image;
      this.tinyImage = tinyImage;
      this.level1Power = level1Power;
      this.level5Power = level5Power;
      this.level10Power = level10Power;
      this.canonicalName = canonicalName;
    }
  }

  public static final PastaThrallType[] PASTA_THRALLS = {
    new PastaThrallType(
        "Vampieroghi",
        1,
        "pastaThrall1",
        SkillPool.BIND_VAMPIEROGHI,
        // My name is written in blood across the history of
        // time . . . but you can call me <name>.
        Pattern.compile("but you can call me ([^.]*)\\."),
        // You conjure a pieroghi, and there is a hiss as it
        // becomes inflated with <name>'s presence.
        Pattern.compile("inflated with ([^']*)'s presence"),
        "vampieroghi.gif",
        "t_vampieroghi.gif",
        "Damage and restore HP during combat",
        "Dispel negative effects after combat",
        "Maximum HP: +60",
        StringUtilities.getCanonicalName("Vampieroghi")),
    new PastaThrallType(
        "Vermincelli",
        2,
        "pastaThrall2",
        SkillPool.BIND_VERMINCELLI,
        // I think little <name> will be the best helper.
        Pattern.compile("I think little (.*?) will be the best helper\\."),
        // You summon a tangled mass of noodles. There is a
        // rustling sound as <name> chews his way into the
        // world to occupy his new body.
        Pattern.compile("rustling sound as (.*?) chews his way into the world"),
        "vermincelli.gif",
        "t_vermincelli.gif",
        "Damage and restore MP during combat",
        "Attack and poison foe during combat",
        "Maximum MP: +30",
        StringUtilities.getCanonicalName("Vermincelli")),
    new PastaThrallType(
        "Angel Hair Wisp",
        3,
        "pastaThrall3",
        SkillPool.BIND_ANGEL_HAIR_WISP,
        // "You must call me <name>. You must give me form. I
        // must live."
        Pattern.compile("You must call me ([^.]*])\\."),
        // You concentrate, and summon a mass of writhing angel
        // hair. A chill perm eates the air as <name>'s spirit
        // enters it. "I live..."
        Pattern.compile("A chill perm ?eates the air as (.*?)'s spirit enters it\\."),
        "angelwisp.gif",
        "t_wisp.gif",
        "Initiative: +5 per level",
        "Prevent enemy critical hits",
        "Blocks enemy attacks",
        StringUtilities.getCanonicalName("Angel Hair Wisp")),
    new PastaThrallType(
        "Elbow Macaroni",
        4,
        "pastaThrall4",
        SkillPool.BIND_UNDEAD_ELBOW_MACARONI,
        // "<name>. My name is <name>."
        Pattern.compile("My name is ([^.]*)\\."),
        // You focus your thoughts and call out to <name>. He
        // claws his way up from beneath the ground at your
        // feet.
        Pattern.compile("You focus your thoughts and call out to (.*?)\\."),
        "macaroni.gif",
        "t_elbowmac.gif",
        "Muscle can't be lower than Mysticality",
        "Weapon Damage: +2 per level",
        "Critical Hit Percentage: +10",
        StringUtilities.getCanonicalName("Elbow Macaroni")),
    new PastaThrallType(
        "Penne Dreadful",
        5,
        "pastaThrall5",
        SkillPool.BIND_PENNE_DREADFUL,
        // "All right, palookah," the private eye says, opening
        // his mouth for the first time, "the name's
        // <name>. I'm a gumshoe. You know, a shamus, a
        // flatfoot, a sleuth.
        Pattern.compile("the name's ([^.]*)\\."),
        // You calm your mind, and imagine a skeletal assembly
        // of penne. A lone saxophone breaks the night's
        // stillness as it appears and <name> possesses it.
        Pattern.compile("it appears and (.*?) possesses it"),
        "pennedreadful.gif",
        "t_dreadful.gif",
        "Moxie can't be lower than Mysticality",
        "Delevel at start of combat",
        "Damage Reduction: +10",
        StringUtilities.getCanonicalName("Penne Dreadful")),
    new PastaThrallType(
        "Lasagmbie",
        6,
        "pastaThrall6",
        SkillPool.BIND_LASAGMBIE,
        // Okay. See you on the other side, <name>.
        Pattern.compile("See you on the other side, (.*?)\\."),
        // You conjure up a good-sized sheet of lasagna, and
        // there is a wet thud as <name>'s spirit lands in it.
        Pattern.compile("a wet thud as ([^']*)'s spirit lands in it"),
        "lasagmbie.gif",
        "t_lasagmbie.gif",
        "Meat Drop: +20 + 2 per level",
        "Attacks for Spooky Damage",
        "Spooky Spell Damage: +10",
        StringUtilities.getCanonicalName("Lasagmbie")),
    new PastaThrallType(
        "Spice Ghost",
        7,
        "pastaThrall7",
        SkillPool.BIND_SPICE_GHOST,
        // My name is <name>, and I am in your debt.
        Pattern.compile("My name is ([^,]*), and I am in your debt\\."),
        // You conjure up a swirling cloud of spicy dried
        // couscous, and there is a crackle of psychokinetic
        // energy as <name> possesses it.
        Pattern.compile("crackle of psychokinetic energy as (.*?) possesses it\\."),
        "spiceghost.gif",
        "t_spiceghost.gif",
        "Item Drop: +10 + 1 per level",
        "spice drop 10/day",
        "Increases duration of Entangling Noodles",
        StringUtilities.getCanonicalName("Spice Ghost")),
    new PastaThrallType(
        "Spaghetti Elemental",
        8,
        "pastaThrall8",
        SkillPool.BIND_SPAGHETTI_ELEMENTAL,
        // "I guess you need a name, huh?" you reply. "I'll
        // call you... um... SshoKodo. That'll do."
        Pattern.compile("I'll call you... *um... *([^.]*). * That'll do."),
        // You close your eyes and reach out across the border
        // between worlds, to the Elemental Plane of
        // Spaghetti. Soon you feel a familiar presence, and
        // pull SshoKodo into the material world.
        Pattern.compile("and pull (.*?) into the material world\\."),
        "spagelem1.gif",
        "t_spagdemon.gif",
        "Experience: 1-ceil(level/3)",
        "Prevents first attack",
        "Spell Damage: +5",
        StringUtilities.getCanonicalName("Spaghetti Elemental")),
  };

  public static final String[] THRALL_ARRAY = new String[PastaThrallData.PASTA_THRALLS.length];
  public static final String[] CANONICAL_THRALL_ARRAY =
      new String[PastaThrallData.PASTA_THRALLS.length];

  static {
    for (int i = 0; i < PastaThrallData.THRALL_ARRAY.length; ++i) {
      PastaThrallData.THRALL_ARRAY[i] = PastaThrallData.dataToType(PASTA_THRALLS[i]);
      PastaThrallData.CANONICAL_THRALL_ARRAY[i] =
          PastaThrallData.dataToCanonicalType(PASTA_THRALLS[i]);
    }
    Arrays.sort(PastaThrallData.THRALL_ARRAY);
  }

  public static String dataToType(PastaThrallType data) {
    return data == null ? "" : data.name;
  }

  public static int dataToId(PastaThrallType data) {
    return data == null ? 0 : data.id;
  }

  public static String dataToSetting(PastaThrallType data) {
    return data == null ? "" : data.settingName;
  }

  public static int dataToSkillId(PastaThrallType data) {
    return data == null ? 0 : data.skill;
  }

  public static Pattern dataToPattern1(PastaThrallType data) {
    return data == null ? null : data.firstSummonPattern;
  }

  public static Pattern dataToPattern2(PastaThrallType data) {
    return data == null ? null : data.subsequentSummonPattern;
  }

  public static String dataToImage(PastaThrallType data) {
    return data == null ? null : data.image;
  }

  public static String dataToTinyImage(PastaThrallType data) {
    return data == null ? null : data.tinyImage;
  }

  public static String dataToLevel1Ability(PastaThrallType data) {
    return data == null ? null : data.level1Power;
  }

  public static String dataToLevel5Ability(PastaThrallType data) {
    return data == null ? null : data.level5Power;
  }

  public static String dataToLevel10Ability(PastaThrallType data) {
    return data == null ? null : data.level10Power;
  }

  public static String dataToCanonicalType(PastaThrallType data) {
    return data == null ? "" : data.canonicalName;
  }

  public static PastaThrallType idToData(final int id) {
    for (int i = 0; i < PastaThrallData.PASTA_THRALLS.length; ++i) {
      PastaThrallType data = PastaThrallData.PASTA_THRALLS[i];
      if (PastaThrallData.dataToId(data) == id) {
        return data;
      }
    }
    return null;
  }

  public static final List<String> getMatchingNames(final String substring) {
    return StringUtilities.getMatchingNames(PastaThrallData.CANONICAL_THRALL_ARRAY, substring);
  }

  public static PastaThrallType typeToData(final String type) {
    // Do fuzzy matching
    List<String> matchingNames = PastaThrallData.getMatchingNames(type);
    if (matchingNames.size() != 1) {
      return null;
    }

    String name = matchingNames.get(0);
    for (int i = 0; i < PastaThrallData.PASTA_THRALLS.length; ++i) {
      PastaThrallType data = PastaThrallData.PASTA_THRALLS[i];
      if (name.equals(PastaThrallData.dataToCanonicalType(data))) {
        return data;
      }
    }
    return null;
  }

  public static PastaThrallType skillIdToData(final int skillId) {
    for (int i = 0; i < PastaThrallData.PASTA_THRALLS.length; ++i) {
      PastaThrallType data = PastaThrallData.PASTA_THRALLS[i];
      if (PastaThrallData.dataToSkillId(data) == skillId) {
        return data;
      }
    }
    return null;
  }

  public static String idToType(final int id) {
    return id == 0 ? "(none)" : PastaThrallData.dataToType(PastaThrallData.idToData(id));
  }

  public static final PastaThrallData NO_THRALL = new PastaThrallData(0);

  public static void initialize() {
    List<PastaThrallData> thralls = KoLCharacter.getPastaThrallList();
    if (!thralls.contains(PastaThrallData.NO_THRALL)) {
      thralls.add(PastaThrallData.NO_THRALL);
    }

    for (int i = 0; i < PastaThrallData.PASTA_THRALLS.length; ++i) {
      PastaThrallType data = PastaThrallData.PASTA_THRALLS[i];
      PastaThrallData thrall = new PastaThrallData(data);
      if (!thralls.contains(thrall)) {
        thralls.add(thrall);
      }
      thrall.updateFromSetting();
    }

    Collections.sort(thralls);
  }

  private final PastaThrallType data;
  private final int id;
  private final String type;
  private int level;
  private String name;
  private String mods;
  private Modifiers.Lookup modsLookup;

  public PastaThrallData(final PastaThrallType data) {
    this.data = data;
    this.id = PastaThrallData.dataToId(data);
    this.type = PastaThrallData.dataToType(data);
    this.level = 0;
    this.name = "";

    if (this.id != 0) {
      Modifiers mods = Modifiers.getModifiers(ModifierType.THRALL, this.type);
      if (mods != null) {
        this.mods = mods.getString(StringModifier.MODIFIERS);
        this.modsLookup = mods.getLookup();
      }
    }
  }

  public PastaThrallData(final int id) {
    this(PastaThrallData.idToData(id));
  }

  public void updateFromSetting() {
    if (this.data == null) {
      return;
    }

    String setting = Preferences.getString(PastaThrallData.dataToSetting(this.data));
    int comma = setting.indexOf(",");
    String levelString = comma == -1 ? setting.trim() : setting.substring(0, comma).trim();
    String name = comma == -1 ? "" : setting.substring(comma + 1).trim();

    this.level = levelString.isEmpty() ? 0 : StringUtilities.parseInt(levelString);
    this.name = name;
  }

  private void updateSetting() {
    if (this.data == null) {
      return;
    }

    String settingName = PastaThrallData.dataToSetting(this.data);
    if (this.name.isEmpty()) {
      Preferences.setString(settingName, String.valueOf(this.level));
    } else {
      String value = this.level + "," + this.name;
      Preferences.setString(settingName, value);
    }
  }

  public PastaThrallType getData() {
    return this.data;
  }

  public int getId() {
    return this.id;
  }

  public String getType() {
    return this.type;
  }

  public int getLevel() {
    return this.level;
  }

  public String getName() {
    return this.name;
  }

  public String getLevel1Ability() {
    return this.data == null ? "" : PastaThrallData.dataToLevel1Ability(this.data);
  }

  public String getLevel5Ability() {
    return this.data == null ? "" : PastaThrallData.dataToLevel5Ability(this.data);
  }

  public String getLevel10Ability() {
    return this.data == null ? "" : PastaThrallData.dataToLevel10Ability(this.data);
  }

  public String getCurrentModifiers() {
    if (this.mods == null) {
      return "";
    }

    PastaThrallData current = KoLCharacter.currentPastaThrall();
    try {
      KoLCharacter.setPastaThrall(this);
      return Modifiers.evaluateModifiers(this.modsLookup, this.mods).toString();
    } finally {
      KoLCharacter.setPastaThrall(current);
    }
  }

  public void update(final int level, final String name) {
    boolean change = false;
    if (level != 0 && level != this.level) {
      this.level = level;
      change = true;
    }
    if (name != null && !name.equals(this.name)) {
      this.name = name;
      change = true;
    }
    if (change) {
      this.updateSetting();
    }
  }

  @Override
  public String toString() {
    return this.id == 0 ? "(none)" : this.type + " (lvl. " + this.getLevel() + ")";
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof PastaThrallData data && this.id == data.id;
  }

  @Override
  public int hashCode() {
    return this.id;
  }

  @Override
  public int compareTo(final PastaThrallData td) {
    return this.id - td.id;
  }

  public static void handleBinding(final int skillId, final String responseText) {
    PastaThrallType data = PastaThrallData.skillIdToData(skillId);
    if (data == null) {
      return;
    }

    PastaThrallData thrall = KoLCharacter.findPastaThrall(PastaThrallData.dataToId(data));
    if (thrall == null) {
      return;
    }

    KoLCharacter.setPastaThrall(thrall);

    Matcher matcher = PastaThrallData.dataToPattern1(data).matcher(responseText);
    if (matcher.find()) {
      thrall.update(0, matcher.group(1));
      return;
    }

    matcher = PastaThrallData.dataToPattern2(data).matcher(responseText);
    if (matcher.find()) {
      thrall.update(0, matcher.group(1));
      return;
    }
  }

  public static void handleDismissal(final String responseText) {
    KoLCharacter.setPastaThrall(PastaThrallData.NO_THRALL);
  }
}
