package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.Expression;
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

    public DailyLimit(DailyLimitType type, String uses, String max, int id) {
      this.type = type;
      this.uses = uses;
      this.max = max;
      this.id = id;

      // Add to map of all limits
      DailyLimitDatabase.allDailyLimits.add(this);
      // Add to map of limits of this type
      type.addDailyLimit(this);
    }

    public DailyLimitType getType() {
      return this.type;
    }

    public int getUses() {
      String stringValue = Preferences.getString(this.uses);

      if (stringValue.equals("true")) {
        return 1;
      }

      if (stringValue.equals("false")) {
        return 0;
      }

      return Preferences.getInteger(this.uses);
    }

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

      return 1;
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
      if (getMax() == 1 && Preferences.getDefault(this.uses).equals("false")) {
        Preferences.setBoolean(this.uses, true);
        return 1;
      }

      return Preferences.increment(this.uses, 1, getMax(), false);
    }
  }

  static {
    for (DailyLimitType type : DailyLimitType.values()) {
      DailyLimitDatabase.tagToDailyLimitType.put(type.toString(), type);
    }

    DailyLimitDatabase.reset();
  }

  public static void reset() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("dailylimits.txt", KoLConstants.DAILYLIMITS_VERSION);
    String[] data;
    boolean error = false;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length >= 2) {
        String tag = data[0];

        DailyLimitType type = DailyLimitDatabase.tagToDailyLimitType.get(tag.toLowerCase());
        DailyLimit dailyLimit = parseDailyLimit(type, data);
        if (dailyLimit == null) {
          RequestLogger.printLine("Daily Limit: " + data[0] + " " + data[1] + " is bogus");
          error = true;
        }
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
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

    int id = -1;

    switch (type) {
      case USE:
      case EAT:
      case DRINK:
      case SPLEEN:
        id = ItemDatabase.getItemId(thing);
        break;
      case CAST:
        id = SkillDatabase.getSkillId(thing);
        break;
    }

    if (id == -1) {
      return null;
    }

    return new DailyLimit(type, uses, max, id);
  }
}
