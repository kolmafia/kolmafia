package net.sourceforge.kolmafia.persistence;

import java.util.LinkedList;
import java.util.List;

public final class EffectData {
  public enum Quality {
    NEUTRAL("neutral"),
    GOOD("good"),
    BAD("bad");

    private final String description;

    Quality(final String description) {
      this.description = description;
    }

    public String description() {
      return this.description;
    }

    public static Quality fromDescription(final String description) {
      return switch (description) {
        case "good" -> GOOD;
        case "bad" -> BAD;
        default -> NEUTRAL;
      };
    }
  }

  public int effectId;
  public String name;
  public String image;
  public String descriptionId;
  public Quality quality = Quality.NEUTRAL;
  public List<String> attributes = new LinkedList<>();
  public String actions;

  public void setQuality(final String quality) {
    this.quality = Quality.fromDescription(quality);
  }

  public String getQualityDescription() {
    return this.quality.description();
  }

  public String toString() {
    // The effect file can have 3, 4, or 5 fields. "image" must be
    // present, even if we don't have the actual file name.
    if (image == null) {
      image = "";
    }

    if (descriptionId == null) {
      descriptionId = "";
    }

    String attrs = (attributes == null) ? "none" : String.join(",", attributes);

    String effectString =
        effectId
            + "\t"
            + name
            + "\t"
            + image
            + "\t"
            + descriptionId
            + "\t"
            + getQualityDescription()
            + "\t"
            + attrs;

    if (actions != null) {
      effectString += "\t" + actions;
    }

    return effectString;
  }
}
