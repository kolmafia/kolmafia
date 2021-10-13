package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public enum AscensionClass {
  ASTRAL_SPIRIT("Astral Spirit", -1),
  SEAL_CLUBBER("Seal Clubber", 1, 0, "Club Foot"),
  TURTLE_TAMER("Turtle Tamer", 2, 0, "Shell Up"),
  PASTAMANCER("Pastamancer", 3, 1, "Entangling Noodles"),
  SAUCEROR("Sauceror", 4, 1, "Soul Bubble"),
  DISCO_BANDIT("Disco Bandit", 2, 5),
  ACCORDION_THIEF("Accordion Thief", 2, 6, "Accordion Bash"),
  AVATAR_OF_BORIS("Avatar of Boris", 11, 0, "Broadside"),
  ZOMBIE_MASTER("Zombie Master", 12, 0, "Corpse Pile"),
  AVATAR_OF_JARLSBERG("Avatar of Jarlsberg", 14, 1, "Blend"),
  AVATAR_OF_SNEAKY_PETE("Avatar of Sneaky Pete", 2, 15, "Snap Fingers"),
  ED("Ed", 17, 1, "Curse of Indecision"),
  COWPUNCHER("Cow Puncher", 18, 0),
  BEANSLINGER("Beanslinger", 19, 1),
  SNAKE_OILER("Snake Oiler", 2, 20),
  GELATINOUS_NOOB("Gelatinous Noob", 2, 23),
  VAMPYRE("Vampyre", 24, 1, "Chill of the Tomb"),
  PLUMBER("Plumber", 25, -1);

  private static final List<String> sealClubberRanks =
      Arrays.asList(
          "Lemming Trampler",
          "Tern Slapper",
          "Puffin Intimidator",
          "Ermine Thumper",
          "Penguin Frightener",
          "Malamute Basher",
          "Narwhal Pummeler",
          "Otter Crusher",
          "Caribou Smacker",
          "Moose Harasser",
          "Reindeer Threatener",
          "Ox Wrestler",
          "Walrus Bludgeoner",
          "Whale Boxer",
          "Seal Clubber");

  private static final List<String> turtleTamerRanks =
      Arrays.asList(
          "Toad Coach",
          "Skink Trainer",
          "Frog Director",
          "Gecko Supervisor",
          "Newt Herder",
          "Frog Boss",
          "Iguana Driver",
          "Salamander Subduer",
          "Bullfrog Overseer",
          "Rattlesnake Chief",
          "Crocodile Lord",
          "Cobra Commander",
          "Alligator Subjugator",
          "Asp Master",
          "Turtle Tamer");

  private static final List<String> pastamancerRanks =
      Arrays.asList(
          "Dough Acolyte",
          "Yeast Scholar",
          "Noodle Neophyte",
          "Starch Savant",
          "Carbohydrate Cognoscenti",
          "Spaghetti Sage",
          "Macaroni Magician",
          "Vermicelli Enchanter",
          "Linguini Thaumaturge",
          "Ravioli Sorcerer",
          "Manicotti Magus",
          "Spaghetti Spellbinder",
          "Cannelloni Conjurer",
          "Angel-Hair Archmage",
          "Pastamancer");

  private static final List<String> saucerorRanks =
      Arrays.asList(
          "Allspice Acolyte",
          "Cilantro Seer",
          "Parsley Enchanter",
          "Sage Sage",
          "Rosemary Diviner",
          "Thyme Wizard",
          "Tarragon Thaumaturge",
          "Oreganoccultist",
          "Basillusionist",
          "Coriander Conjurer",
          "Bay Leaf Brujo",
          "Sesame Soothsayer",
          "Marinara Mage",
          "Alfredo Archmage",
          "Sauceror");

  private static final List<String> discoBanditRanks =
      Arrays.asList(
          "Funk Footpad",
          "Rhythm Rogue",
          "Chill Crook",
          "Jiggy Grifter",
          "Beat Snatcher",
          "Sample Swindler",
          "Move Buster",
          "Jam Horker",
          "Groove Filcher",
          "Vibe Robber",
          "Boogie Brigand",
          "Flow Purloiner",
          "Jive Pillager",
          "Rhymer and Stealer",
          "Disco Bandit");

  private static final List<String> accordionThiefRanks =
      Arrays.asList(
          "Polka Criminal",
          "Mariachi Larcenist",
          "Zydeco Rogue",
          "Chord Horker",
          "Chromatic Crook",
          "Squeezebox Scoundrel",
          "Concertina Con Artist",
          "Button Box Burglar",
          "Hurdy-Gurdy Hooligan",
          "Sub-Sub-Apprentice Accordion Thief",
          "Sub-Apprentice Accordion Thief",
          "Pseudo-Apprentice Accordion Thief",
          "Hemi-Apprentice Accordion Thief",
          "Apprentice Accordion Thief",
          "Accordion Thief");

  public static final List<AscensionClass> standardClasses =
      Arrays.asList(
          AscensionClass.SEAL_CLUBBER,
          AscensionClass.TURTLE_TAMER,
          AscensionClass.PASTAMANCER,
          AscensionClass.SAUCEROR,
          AscensionClass.DISCO_BANDIT,
          AscensionClass.ACCORDION_THIEF);

  public final String name;
  public final int id;
  private final int primeStatIndex;
  private final String stun;

  public static AscensionClass nameToClass(String name) {
    for (AscensionClass ascensionClass : AscensionClass.values()) {
      if (name.equalsIgnoreCase(ascensionClass.getName())) {
        return ascensionClass;
      }
    }
    return null;
  }

  public static AscensionClass idToClass(int id) {
    for (AscensionClass ascensionClass : AscensionClass.values()) {
      if (id == ascensionClass.getId()) {
        return ascensionClass;
      }
    }
    return null;
  }

  AscensionClass(String name, int id, int primeStatIndex, String stun) {
    this.name = name;
    this.id = id;
    this.primeStatIndex = primeStatIndex;
    this.stun = stun;
  }

  AscensionClass(String name, int id, int primeStatIndex) {
    this(name, id, -1, null);
  }

  AscensionClass(String name, int id) {
    this(name, id, -1);
  }

  public String getName() {
    return this.name;
  }

  public int getId() {
    return this.id;
  }

  public String getStun() {
    if (this.stun != null) {
      return this.stun;
    }

    return Preferences.getBoolean("considerShadowNoodles") ? "Shadow Noodles" : "none";
  }

  public int getStarterWeapon() {
    switch (this) {
      case SEAL_CLUBBER:
        return ItemPool.SEAL_CLUB;
      case TURTLE_TAMER:
        return ItemPool.TURTLE_TOTEM;
      case PASTAMANCER:
        return ItemPool.PASTA_SPOON;
      case SAUCEROR:
        return ItemPool.SAUCEPAN;
      case DISCO_BANDIT:
        return ItemPool.DISCO_BALL;
      case ACCORDION_THIEF:
        return ItemPool.STOLEN_ACCORDION;
    }

    return -1;
  }

  public final int getPrimeStatIndex() {
    if (this == AscensionClass.PLUMBER) {
      long mus = KoLCharacter.getTotalMuscle();
      long mys = KoLCharacter.getTotalMysticality();
      long mox = KoLCharacter.getTotalMoxie();
      return (mus >= mys) ? (mus >= mox ? 0 : 2) : (mys >= mox) ? 1 : 2;
    }

    return this.primeStatIndex;
  }

  public final Stat getMainStat() {
    switch (getPrimeStatIndex()) {
      case 0:
        return Stat.MUSCLE;
      case 1:
        return Stat.MYSTICALITY;
      case 2:
        return Stat.MOXIE;
    }

    return Stat.NONE;
  }

  public boolean isStandard() {
    return standardClasses.contains(this);
  }

  public final int getSkillBase() {
    return this.getId() * 1000;
  }
}
