package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public class GooSkillsCommand extends AbstractCommand {

  private static final Comparator<GooSkill> idComparator = new IdComparator();
  private static final Comparator<GooSkill> nameComparator = new NameComparator();
  private static final Comparator<GooSkill> monsterComparator = new MonsterComparator();
  private static final Comparator<GooSkill> zoneComparator = new ZoneComparator();

  public static final GooSkill[] GOO_SKILLS = {
    new GooSkill(SkillPool.PSEUDOPOD_SLAP, "", "Deals 10 damage"),
    new GooSkill(SkillPool.HARDSLAB, "remaindered skeleton", "Deals Muscle in physical damage"),
    new GooSkill(
        SkillPool.TELEKINETIC_MURDER, "cr&ecirc;ep", "Deals Mysticality in physical damage"),
    new GooSkill(
        SkillPool.SNAKESMACK,
        "sewer snake with a sewer snake in it",
        "Deals Moxie in physical damage"),
    new GooSkill(SkillPool.IRE_PROOF, "raging bull"),
    new GooSkill(SkillPool.NANOFUR, "ratbat"),
    new GooSkill(SkillPool.AUTOVAMPIRISM_ROUTINES, "spooky vampire"),
    new GooSkill(SkillPool.CONIFER_POLYMERS, "pine bat"),
    new GooSkill(SkillPool.ANTI_SLEAZE_RECURSION, "werecougar"),
    new GooSkill(SkillPool.MICROBURNER, "Cobb's Knob oven"),
    new GooSkill(SkillPool.CRYOCURRENCY, "Knob Goblin MBA"),
    new GooSkill(SkillPool.CURSES_LIBRARY, "lihc"),
    new GooSkill(SkillPool.EXHAUST_TUBULES, "beanbat"),
    new GooSkill(SkillPool.CAMP_SUBROUTINES, "Knob Goblin Harem Girl"),
    new GooSkill(SkillPool.GREY_NOISE, "Boss Bat", "Deals 5 damage + bonus elemental damage"),
    new GooSkill(SkillPool.ADVANCED_EXO_ALLOY, "Knob Goblin Elite Guard"),
    new GooSkill(SkillPool.LOCALIZED_VACUUM, "cubist bull"),
    new GooSkill(SkillPool.MICROWEAVE, "eXtreme cross-country hippy"),
    new GooSkill(SkillPool.ECTOGENESIS, "Claybender Sorcerer Ghost"),
    new GooSkill(SkillPool.CLAMMY_MICROCILIA, "malevolent hair clog"),
    new GooSkill(SkillPool.LUBRICANT_LAYER, "oil slick"),
    new GooSkill(SkillPool.INFERNAL_AUTOMATA, "demonic icebox"),
    new GooSkill(SkillPool.COOLING_TUBULES, "Ninja Snowman Weaponmaster"),
    new GooSkill(SkillPool.OMINOUS_SUBSTRATE, "animated ornate nightstand"),
    new GooSkill(SkillPool.SECONDARY_FERMENTATION, "drunk goat"),
    new GooSkill(SkillPool.PROCGEN_RIBALDRY, "smut orc screwer"),
    new GooSkill(SkillPool.SOLID_FUEL, "Knob Goblin Alchemist"),
    new GooSkill(SkillPool.AUTOCHRONY, "zombie waltzers"),
    new GooSkill(SkillPool.TEMPORAL_HYPEREXTENSION, "Pr Imp"),
    new GooSkill(SkillPool.PROPAGATION_DRIVE, "junksprite bender"),
    new GooSkill(SkillPool.FINANCIAL_SPREADSHEETS, "me4t begZ0r"),
    new GooSkill(
        SkillPool.PHASE_SHIFT, "Spectral Jellyfish", "10 turns of Shifted Phase (Combat Rate -10)"),
    new GooSkill(
        SkillPool.PIEZOELECTRIC_HONK, "white lion", "10 turns of Hooooooooonk! (Combat Rate +10)"),
    new GooSkill(SkillPool.OVERCLOCKING, "Big Wheelin' Twins"),
    new GooSkill(SkillPool.SUBATOMIC_HARDENING, "pooltergeist"),
    new GooSkill(SkillPool.GRAVITATIONAL_COMPRESSION, "suckubus"),
    new GooSkill(SkillPool.HIVEMINDEDNESS, "mind flayer"),
    new GooSkill(SkillPool.PONZI_APPARATUS, "anglerbush"),
    new GooSkill(SkillPool.FLUID_DYNAMICS_SIMULATION, "Carnivorous Moxie Weed"),
    new GooSkill(SkillPool.NANTLERS, "stuffed moose head", "Deals Muscle in damage + bonus damage"),
    new GooSkill(
        SkillPool.NANOSHOCK, "Jacob's adder", "Deals Mysticality in damage + bonus damage"),
    new GooSkill(SkillPool.AUDIOCLASM, "spooky music box", "Deals Moxie in damage + bonus damage"),
    new GooSkill(
        SkillPool.SYSTEM_SWEEP, "pygmy janitor", "Deals Muscle in physical sweep & banish"),
    new GooSkill(
        SkillPool.DOUBLE_NANOVISION,
        "drunk pygmy",
        "Deals Mysticality in physical damage, +100% Item Drop"),
    new GooSkill(
        SkillPool.INFINITE_LOOP, "pygmy witch lawyer", "Deals Muscle in physical sweep & +3 exp"),
    new GooSkill(
        SkillPool.PHOTONIC_SHROUD,
        "black panther",
        "10 turns of Darkened Photons (Combat Rate -10)"),
    // new GooSkill(SkillPool.UNUSED_COMBAT_SELFBUFF, ""),
    new GooSkill(SkillPool.STEAM_MYCELIA, "steam elemental"),
    new GooSkill(SkillPool.SNOW_COOLING_System, "Snow Queen"),
    new GooSkill(SkillPool.LEGACY_CODE, "possessed wine rack"),
    new GooSkill(SkillPool.AUTOEXEC_BAT, "Flock of Stab-bats"),
    new GooSkill(SkillPool.INNUENDO_CIRCUITRY, "Astronomer"),
    new GooSkill(SkillPool.SUBATOMIC_TANGO, "fan dancer"),
    new GooSkill(SkillPool.EXTRA_INNINGS, "baseball bat"),
    new GooSkill(SkillPool.RELOADING, "Bullet Bill"),
    new GooSkill(SkillPool.HARRIED, "rushing bum"),
    new GooSkill(SkillPool.TEMPORAL_BENT, "undead elbow macaroni"),
    new GooSkill(SkillPool.PROVABLY_EFFICIENT, "Sub-Assistant Knob Mad Scientist"),
    new GooSkill(SkillPool.BASIC_IMPROVEMENTS, "BASIC Elemental"),
    new GooSkill(SkillPool.SHIFTED_ABOUT, "shifty pirate"),
    new GooSkill(SkillPool.SPOOKY_VEINS, "ghost miner"),
    new GooSkill(SkillPool.SEVEN_FOOT_FEELINGS, "dopey 7-Foot Dwarf"),
    new GooSkill(SkillPool.SELF_ACTUALIZED, "banshee librarian"),
  };

  public GooSkillsCommand() {
    this.usage =
        " [all | needed] [id | name | monster | zone] - Grey You skills, either all (default) or 'needed' (not yet unlocked).";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // gooskills - List all Grey You skills
    // gooskills needed - List only not-yet-known skills
    //
    // The skills are ordered by skill ID.
    // You can specify a sorting order: skill id, skill name, monster name, zone "level", etc.

    String[] params = parameters.trim().split("\\s+");

    boolean all = true;
    String order = "id";

    for (String keyword : params) {
      switch (keyword) {
        case "all":
          all = true;
          break;
        case "needed":
          all = false;
          break;
        case "id":
        case "name":
        case "monster":
        case "zone":
          order = keyword;
          break;
        case "":
          break;
        default:
          KoLmafia.updateDisplay(MafiaState.ERROR, "Use command gooskills " + this.usage);
          return;
      }
    }

    if (!all) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You are not in a Grey You run.");
      return;
    }

    GooSkill[] skills = sortGooSkills(order);

    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=3>");
    output.append("<tr>");
    output.append("<th rowspan=2>Name</th>");
    output.append("<th>Type</th>");
    output.append("<th>Source</th>");
    output.append("</tr>");

    output.append("<tr>");
    output.append("<th>Known</th>");
    output.append("<th>Effect</th>");
    output.append("</tr>");

    for (GooSkill skill : skills) {
      boolean known = skill.haveSkill();
      if (!all && known) {
        continue;
      }

      output.append("<tr>");
      output.append("<td rowspan=2>");
      output.append(skill.getName());
      output.append("</td>");
      output.append("<td>");
      output.append(skill.getTypeName());
      if (skill.getType() != SkillDatabase.PASSIVE) {
        output.append(" (");
        output.append(String.valueOf(skill.getMPCost()));
        output.append(" MP)");
      }
      output.append("</td>");
      output.append("<td>");
      output.append(skill.getMonsterName());
      if (!skill.getMonsterZone().equals("")) {
        output.append(" (");
        output.append(skill.getMonsterZone());
        output.append(")");
      }
      output.append("</td>");
      output.append("</tr>");

      output.append("<tr>");
      output.append("<td>");
      output.append(known ? "yes" : "no");
      output.append("</td>");
      output.append("<td>");
      output.append(skill.getEvaluatedEnchantments());
      output.append("</td>");
      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }

  private static GooSkill[] sortGooSkills(String order) {
    Comparator<GooSkill> comparator =
        order.equals("id")
            ? idComparator
            : order.equals("name")
                ? nameComparator
                : order.equals("monster")
                    ? monsterComparator
                    : order.equals("zone") ? zoneComparator : null;

    if (comparator == null) {
      return GOO_SKILLS;
    }

    GooSkill[] skills = Arrays.copyOf(GOO_SKILLS, GOO_SKILLS.length);
    Arrays.sort(skills, comparator);

    return skills;
  }

  public static class GooSkill {
    private final int skillId;
    private final MonsterData monster;
    private final String zone;

    private final String name;
    private final int type;
    private final String typeName;
    private final long mpCost;

    private final String enchantments;
    private final String modsLookup;

    public GooSkill(final int skillId, final String monsterName) {
      // This is for passive skills; we will look up the enchantments
      this(skillId, monsterName, "");
    }

    public GooSkill(final int skillId, final String monsterName, final String effects) {
      this.skillId = skillId;
      this.monster = MonsterDatabase.findMonster(monsterName);
      List<String> zones = AdventureDatabase.getAreasWithMonster(this.monster);
      this.zone = (zones.size() > 0) ? zones.get(0) : "";

      this.name = SkillDatabase.getSkillName(skillId);
      this.type = SkillDatabase.getSkillType(skillId);
      this.typeName = SkillDatabase.getSkillTypeName(skillId);

      Modifiers mods = null;
      if (this.type == SkillDatabase.PASSIVE) {
        mods = Modifiers.getModifiers("Skill", this.name);
      }

      if (mods != null) {
        this.enchantments = mods.getString("Modifiers");
        this.modsLookup = mods.getName();
      } else {
        this.enchantments = effects;
        this.modsLookup = "";
      }

      this.mpCost =
          (this.type != SkillDatabase.PASSIVE) ? SkillDatabase.getMPConsumptionById(skillId) : 0;
    }

    public boolean haveSkill() {
      UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(this.skillId);
      return KoLCharacter.hasSkill(skill, KoLConstants.availableSkills)
          || KoLCharacter.hasSkill(skill, KoLConstants.availableCombatSkills);
    }

    public int getSkillId() {
      return this.skillId;
    }

    public String getName() {
      return this.name;
    }

    public int getType() {
      return this.type;
    }

    public String getTypeName() {
      return this.typeName;
    }

    public long getMPCost() {
      return this.mpCost;
    }

    public String getMonsterName() {
      return (this.monster == null) ? "" : this.monster.getName();
    }

    public String getMonsterZone() {
      return (this.zone == null) ? "" : this.zone;
    }

    public String getEnchantments() {
      return this.enchantments;
    }

    public String getEvaluatedEnchantments() {
      if (this.modsLookup.equals("")) {
        return this.enchantments;
      }
      return Modifiers.evaluateModifiers(this.modsLookup, this.enchantments).toString();
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static class IdComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.skillId - o2.skillId;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof IdComparator;
    }
  }

  public static class NameComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.name.compareTo(o2.name);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof NameComparator;
    }
  }

  public static class MonsterComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.getMonsterName().compareTo(o2.getMonsterName());
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MonsterComparator;
    }
  }

  public static class ZoneComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.getMonsterZone().compareTo(o2.getMonsterZone());
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ZoneComparator;
    }
  }
}
