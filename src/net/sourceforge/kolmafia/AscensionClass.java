package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public enum AscensionClass {
  ASTRAL_SPIRIT("Astral Spirit", -1),
  SEAL_CLUBBER("Seal Clubber", 1, "club", 0, "Club Foot"),
  TURTLE_TAMER("Turtle Tamer", 2, "turtle", 0, "Shell Up"),
  PASTAMANCER("Pastamancer", 3, "pasta", 1, "Entangling Noodles"),
  SAUCEROR("Sauceror", 4, "sauce", 1, "Soul Bubble"),
  DISCO_BANDIT("Disco Bandit", 5, "disco", 2),
  ACCORDION_THIEF("Accordion Thief", 6, "accordion", 2, "Accordion Bash"),
  AVATAR_OF_BORIS("Avatar of Boris", 11, "trusty", 0, "Broadside"),
  ZOMBIE_MASTER("Zombie Master", 12, "tombstone", 0, "Corpse Pile"),
  AVATAR_OF_JARLSBERG("Avatar of Jarlsberg", 14, "path12icon", 1, "Blend"),
  AVATAR_OF_SNEAKY_PETE("Avatar of Sneaky Pete", 15, "bigglasses", 2, "Snap Fingers"),
  ED("Ed the Undying", 17, "thoth", 1, "Curse of Indecision"),
  COWPUNCHER("Cow Puncher", 18, "darkcow", 0),
  BEANSLINGER("Beanslinger", 19, "beancan", 1),
  SNAKE_OILER("Snake Oiler", 20, "tinysnake", 2),
  GELATINOUS_NOOB("Gelatinous Noob", 23, "gelatinousicon", 2),
  VAMPYRE("Vampyre", 24, "vampirefangs", 1, "Chill of the Tomb"),
  PLUMBER("Plumber", 25, "mario_hammer2", -1);

  public static final List<AscensionClass> standardClasses =
      Arrays.asList(
          AscensionClass.SEAL_CLUBBER,
          AscensionClass.TURTLE_TAMER,
          AscensionClass.PASTAMANCER,
          AscensionClass.SAUCEROR,
          AscensionClass.DISCO_BANDIT,
          AscensionClass.ACCORDION_THIEF);

  private final String name;
  private final int id;
  private final String image;
  private final int primeStatIndex;
  private final String stun;

  public static AscensionClass nameToClass(String name) {
    if (name.equals("")) {
      return null;
    }

    for (AscensionClass ascensionClass : AscensionClass.values()) {
      if (ascensionClass.getName().toLowerCase().contains(name.toLowerCase())) {
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

  AscensionClass(String name, int id, String image, int primeStatIndex, String stun) {
    this.name = name;
    this.id = id;
    this.image = image;
    this.primeStatIndex = primeStatIndex;
    this.stun = stun;
  }

  AscensionClass(String name, int id, String image, int primeStatIndex) {
    this(name, id, image, primeStatIndex, null);
  }

  AscensionClass(String name, int id) {
    this(name, id, null, -1);
  }

  public final String getName() {
    return this.name;
  }

  public final int getId() {
    return this.id;
  }

  public final String getImage() {
    return this.image;
  }

  public String getStun() {
    if (this.stun != null) {
      return this.stun;
    }

    return Preferences.getBoolean("considerShadowNoodles") ? "Shadow Noodles" : "none";
  }

  public final int getStarterWeapon() {
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
      default:
        return -1;
    }
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

  public final boolean isStandard() {
    return standardClasses.contains(this);
  }

  public final int getSkillBase() {
    return this.getId() * 1000;
  }

  public final String getInitials() {
    return Arrays.stream(this.name().split("_"))
        .map(word -> String.valueOf(word.charAt(0)))
        .collect(Collectors.joining());
  }

  @Override
  public final String toString() {
    return this.getName();
  }
}
