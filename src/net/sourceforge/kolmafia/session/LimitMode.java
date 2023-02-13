package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public enum LimitMode {
  UNKNOWN("unknown"),
  NONE(""),
  SPELUNKY("spelunky"),
  // Batfellow
  BATMAN("batman"),
  // Ed in the Underworld
  ED("edunder"),
  // Mutually exclusive pseudo Limit Modes
  // Form of...Bird!
  BIRD("bird"),
  // Form of...Cockroach!
  ROACH("cockroach"),
  // Shape of...Mole!
  MOLE("mole"),
  // Half-Astral
  ASTRAL("astral");

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

  public void finish() {
    switch (this) {
      case ASTRAL -> {
        Preferences.setString("currentAstralTrip", "");
      }
      case BIRD, ROACH, MOLE -> {
        // These could clear currentLlamaForm, but that is handled in ChoiceControl
      }
      default -> {}
    }
  }

  public String effectName() {
    return switch (this) {
      case BIRD -> "Form of...Bird!";
      case MOLE -> "Shape of...Mole!";
      case ROACH -> "Form of...Cockroach!";
      case ASTRAL -> "Half-Astral!";
      default -> null;
    };
  }

  public boolean requiresCharPane() {
    return switch (this) {
      case SPELUNKY, BATMAN -> true;
      default -> false;
    };
  }

  public boolean limitRecovery() {
    return switch (this) {
      case NONE, BIRD, ROACH, MOLE, ASTRAL -> false;
      default -> true;
    };
  }

  public boolean limitSkill(final int skillId) {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY -> skillId < 7238 || skillId > 7244;
      case BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
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
      case BIRD, ROACH, MOLE, ASTRAL -> itemId == ItemPool.GONG
          || itemId == ItemPool.ASTRAL_MUSHROOM;
    };
  }

  public boolean limitSlot(final Slot slot) {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY -> switch (slot) {
        case HAT, WEAPON, OFFHAND, CONTAINER, ACCESSORY1 -> false;
        default -> true;
      };
      case ED, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitOutfits() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitFamiliars() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitAdventure(KoLAdventure adventure) {
    if (this == LimitMode.ASTRAL) {
      String trip = Preferences.getString("currentAstralTrip");
      boolean chosen = !trip.equals("");
      // If we're Half-Astral and have not chosen a trip, any Astral area is
      // allowed, since attempting to automate any of them will choose.
      return switch (adventure.getAdventureNumber()) {
        case AdventurePool.BAD_TRIP -> chosen && !trip.equals("Bad Trip");
        case AdventurePool.MEDIOCRE_TRIP -> chosen && !trip.equals("Mediocre Trip");
        case AdventurePool.GREAT_TRIP -> chosen && !trip.equals("Great Trip");
        default -> true;
      };
    }
    return limitZone(adventure.getZone());
  }

  private final List<String> limitZones =
      List.of("Batfellow Area", "Spelunky Area", "Shape of Mole", "Astral");

  public boolean limitZone(String zoneName) {
    String rootZone = AdventureDatabase.getRootZone(zoneName, limitZones);

    switch (rootZone) {
      case "Astral" -> {
        if (Preferences.getString("currentAstralTrip").equals("")) {
          // We can use an astral mushroom to go here
          return this != LimitMode.NONE;
        }
      }
      case "Shape of Mole" -> {
        // We can use a llamma lama gong to go here
        if (Preferences.getString("currentLlamaForm").equals("")) {
          return this != LimitMode.NONE;
        }
      }
    }

    return switch (this) {
      case UNKNOWN -> false;
      case NONE -> limitZones.contains(rootZone);
      case SPELUNKY -> !rootZone.equals("Spelunky Area");
      case BATMAN -> !rootZone.equals("Batfellow Area");
      case ED -> true;
        // Mutually exclusive pseudo Limit Modes
      case BIRD -> zoneName.equals("Shape of Mole") || rootZone.equals("Astral");
      case ROACH -> false;
      case MOLE -> !zoneName.equals("Shape of Mole");
        // Astral travelers are actually limited to a specific adventure areas in
        // the "Astral" zone, but given just the zone, we cannot enforce that.
        // limitAdventure() will do that.
      case ASTRAL -> !zoneName.equals("Astral");
    };
  }

  public boolean limitMeat() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitMall() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitNPCStores() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitCoinmasters() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitClan() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitCampground() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitStorage() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitEating() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitDrinking() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitSpleening() {
    return switch (this) {
      case UNKNOWN, NONE -> false;
      case SPELUNKY, BATMAN, ED -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitPickpocket() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public boolean limitMCD() {
    return switch (this) {
      case UNKNOWN, NONE, ED -> false;
      case SPELUNKY, BATMAN -> true;
      case BIRD, ROACH, MOLE, ASTRAL -> false;
    };
  }

  public String getName() {
    return name;
  }
}
