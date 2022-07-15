package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.sourceforge.kolmafia.Expression;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DailyLimitDatabase {
  private DailyLimitDatabase() {}

  public enum DailyLimitType {
    USE("use"),
    EAT("eat"),
    DRINK("drink"),
    SPLEEN("spleen"),
    CAST("cast");

    private final String tag;

    // Pockets self-add themselves to this map
    private final Map<Integer, DailyLimit> dailyLimits = new HashMap<>();

    DailyLimitType(String tag) {
      this.tag = tag;
    }

    public void addDailyLimit(DailyLimit dailyLimit) {
      this.dailyLimits.put(dailyLimit.getId(), dailyLimit);
    }

    public Map<Integer, DailyLimit> getDailyLimits() {
      return this.dailyLimits;
    }

    public DailyLimit getDailyLimit(int id) {
      return this.dailyLimits.get(id);
    }

    @Override
    public String toString() {
      return this.tag;
    }
  }

  private static final Map<String, DailyLimitType> tagToDailyLimitType = new HashMap<>();

  // Pockets self-add themselves to this map
  public static final List<DailyLimit> allDailyLimits = new ArrayList<>();

  public static class DailyLimit {
    protected final DailyLimitType type;
    protected final String uses;
    protected final String max;
    protected final int id;
    protected final String subType;

    public DailyLimit(DailyLimitType type, String uses, String max, int id, String subType) {
      this.type = type;
      this.uses = uses;
      this.max = max;
      this.id = id;
      this.subType = subType;

      // Add to map of all limits
      allDailyLimits.add(this);
      // Add to map of limits of this type
      type.addDailyLimit(this);
    }

    public DailyLimitType getType() {
      return this.type;
    }

    public String getPref() {
      return this.uses;
    }

    private boolean isTome() {
      return Objects.equals(this.subType, "tome");
    }

    public int getUses() {
      if (isTome() && !KoLCharacter.canInteract()) {
        // Tomes can be used three times per day.  In aftercore, each tome can be used 3 times per
        // day.
        return Preferences.getInteger("tomeSummons");
      }

      return switch (Preferences.getString(this.uses)) {
        case "true" -> 1;
        case "false" -> 0;
        default -> Preferences.getInteger(this.uses);
      };
    }

    /**
     * Get the maximum number of daily uses for the given DailyLimit item
     *
     * @return Maximum number of uses or -1 if the underlying data cannot be parsed
     */
    public int getMax() {
      if (this.max.length() == 0) {
        return 1;
      }

      if (StringUtilities.isNumeric(this.max)) {
        return StringUtilities.parseInt(this.max);
      }

      if (this.max.startsWith("[") && this.max.endsWith("]")) {
        String exprString = this.max.substring(1, this.max.length() - 1);
        Expression expr =
            new Expression(exprString, "daily limit for " + this.getType() + " " + this.getId());
        if (!expr.hasErrors()) {
          return (int) expr.eval();
        }
      }

      return -1;
    }

    public int getUsesRemaining() {
      return Math.max(0, getMax() - getUses());
    }

    public boolean hasUsesRemaining() {
      return getUsesRemaining() > 0;
    }

    public int getId() {
      return this.id;
    }

    public int increment() {
      return increment(1);
    }

    public int increment(final int delta) {
      if (getMax() == 1 && Preferences.getDefault(this.uses).equals("false")) {
        Preferences.setBoolean(this.uses, true);
        return 1;
      }

      if (isTome()) {
        Preferences.increment("tomeSummons", delta, getMax(), false);
      }

      return Preferences.increment(this.uses, delta, getMax(), false);
    }
  }

  static {
    for (var type : DailyLimitType.values()) {
      tagToDailyLimitType.put(type.toString(), type);
    }

    tagToDailyLimitType.put("tome", DailyLimitType.CAST);

    reset();
  }

  public static void reset() {
    boolean error = false;

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("dailylimits.txt", KoLConstants.DAILYLIMITS_VERSION)) {

      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length >= 2) {
          String tag = data[0];

          var type = tagToDailyLimitType.get(tag.toLowerCase());
          var dailyLimit = parseDailyLimit(type, data);
          if (dailyLimit == null) {
            RequestLogger.printLine("Daily Limit: " + data[0] + " " + data[1] + " is bogus");
            error = true;
          } else if (dailyLimit.getMax() < 0) {
            RequestLogger.printLine("Daily Limit: " + data[0] + " " + data[1] + " has invalid max");
            error = true;
          }
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
      error = true;
    }

    if (error) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Error loading daily limit database.");
    }
  }

  private static DailyLimit parseDailyLimit(DailyLimitType type, String[] data) {
    String thing = data[1];
    String uses = data[2];
    String max = data.length >= 4 ? data[3] : "";

    int id =
        switch (type) {
          case USE, EAT, DRINK, SPLEEN -> ItemDatabase.getItemId(thing);
          case CAST -> SkillDatabase.getSkillId(thing);
        };

    if (id == -1) {
      return null;
    }

    var subType = (data[0].equalsIgnoreCase("tome")) ? "tome" : null;

    return new DailyLimit(type, uses, max, id, subType);
  }
}
