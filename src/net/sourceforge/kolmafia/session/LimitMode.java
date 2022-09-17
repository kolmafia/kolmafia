package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public enum LimitMode {
  UNKNOWN("unknown"),
  NONE(""),
  SPELUNKY("spelunky"),
  // Batfellow
  BATMAN("batman"),
  // Ed in the Underworld
  ED("edunder");

  public static LimitMode find(final String name) {
    return Arrays.stream(values())
        .filter(l -> l.getName().equals(name))
        .findAny()
        .orElse(LimitMode.UNKNOWN);
  }

  private final String name;

  LimitMode(final String name) {
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

  public boolean limitSkill(final int skillId) {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY -> skillId < 7238 || skillId > 7244;
      case BATMAN -> true;
    };
  }

  public boolean limitSkill(final UseSkillRequest skill) {
    int skillId = skill.getSkillId();
    return limitSkill(skillId);
  }

  public boolean limitItem(final int itemId) {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY -> itemId < 8040 || itemId > 8062;
      case BATMAN -> itemId < 8797 || itemId > 8815 || itemId == 8800;
      case ED -> true;
    };
  }

  public boolean limitSlot(final int slot) {
    return switch (this) {
      case UNKNOWN, NONE -> false;
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

  public boolean limitOutfits() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitFamiliars() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitAdventure(KoLAdventure adventure) {
    return limitZone(adventure.getZone());
  }

  private final List<String> limitZones = List.of("Batfellow Area", "Spelunky Area");

  public boolean limitZone(String zoneName) {
    String rootZone = AdventureDatabase.getRootZone(zoneName, limitZones);

    return switch (this) {
      case UNKNOWN -> false;
      case NONE -> limitZones.contains(rootZone);
      case SPELUNKY -> !rootZone.equals("Spelunky Area");
      case BATMAN -> !rootZone.equals("Batfellow Area");
      case ED -> true;
    };
  }

  public boolean limitMeat() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitMall() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitNPCStores() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public boolean limitCoinmasters() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitClan() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public boolean limitCampground() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public boolean limitStorage() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitEating() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public boolean limitDrinking() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public boolean limitSpleening() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
    };
  }

  public boolean limitPickpocket() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public boolean limitMCD() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
    };
  }

  public String getName() {
    return name;
  }
}
