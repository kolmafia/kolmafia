package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Optional;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public enum Limitmode {
  NONE("0"),
  SPELUNKY("spelunky"),
  BATMAN("batman"),
  ED("edunder");

  public static Limitmode find(final String name) {
    return Arrays.stream(values())
        .filter(l -> l.getName().equals(name))
        .findAny()
        .orElse(Limitmode.NONE);
  }

  private final String name;

  Limitmode(final String name) {
    this.name = name;
  }

  private Optional<Runnable> resetFunction() {
    return switch (this) {
      case SPELUNKY -> Optional.of(SpelunkyRequest::reset);
      case BATMAN -> Optional.of(BatManager::begin);
      default -> Optional.empty();
    };
  }

  public boolean requiresReset() {
    return resetFunction().isPresent();
  }

  public void reset() {
    resetFunction().ifPresent(Runnable::run);
  }

  public static boolean limitSkill(final int skillId) {
    var limitmode = KoLCharacter.getLimitmode();

    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY -> skillId < 7238 || skillId > 7244;
      case BATMAN -> true;
    };
  }

  public static boolean limitSkill(final UseSkillRequest skill) {
    int skillId = skill.getSkillId();
    return Limitmode.limitSkill(skillId);
  }

  public static boolean limitItem(final int itemId) {
    var limitmode = KoLCharacter.getLimitmode();

    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY -> itemId < 8040 || itemId > 8062;
      case BATMAN -> itemId < 8797 || itemId > 8815 || itemId == 8800;
      case ED -> true;
    };
  }

  public static boolean limitSlot(final int slot) {
    var limitmode = KoLCharacter.getLimitmode();

    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY -> switch (slot) {
        case EquipmentManager.HAT,
            EquipmentManager.WEAPON,
            EquipmentManager.OFFHAND,
            EquipmentManager.CONTAINER,
            EquipmentManager.ACCESSORY1 -> false;
        default -> true;
      };
      case ED, BATMAN -> true;
    };
  }

  public static boolean limitOutfits() {
    var limitmode = KoLCharacter.getLimitmode();

    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitFamiliars() {
    var limitmode = KoLCharacter.getLimitmode();

    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitAdventure(KoLAdventure adventure) {
    return Limitmode.limitZone(adventure.getZone());
  }

  public static boolean limitZone(String zoneName) {
    var limitmode = KoLCharacter.getLimitmode();
    String rootZone = AdventureDatabase.getRootZone(zoneName);

    return switch (limitmode) {
      case NONE -> rootZone.equals("Spelunky Area") || rootZone.equals("Batfellow Area");
      case SPELUNKY -> !rootZone.equals("Spelunky Area");
      case BATMAN -> !rootZone.equals("Batfellow Area");
      case ED -> true;
    };
  }

  public static boolean limitMeat() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitMall() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitNPCStores() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public static boolean limitCoinmasters() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitClan() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public static boolean limitCampground() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public static boolean limitStorage() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitEating() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public static boolean limitDrinking() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public static boolean limitSpleening() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public static boolean limitPickpocket() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public static boolean limitMCD() {
    var limitmode = KoLCharacter.getLimitmode();
    return switch (limitmode) {
      case NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public String getName() {
    return name;
  }
}
