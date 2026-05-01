package net.sourceforge.kolmafia.persistence;

import java.util.List;

public final class EffectData {
  public int effectId;
  public String name;
  public String image;
  public String descriptionId;
  public int quality;
  public List<String> attributes;
  public String defaultAction;

  public void setQuality(final String quality) {
    this.quality = parseQuality(quality);
  }

  private int parseQuality(final String quality) {
    return switch (quality) {
      case "good" -> EffectDatabase.GOOD;
      case "bad" -> EffectDatabase.BAD;
      default -> EffectDatabase.NEUTRAL;
    };
  }

  public String getQualityDescription() {
    return switch (quality) {
      case EffectDatabase.GOOD -> "good";
      case EffectDatabase.BAD -> "bad";
      case EffectDatabase.NEUTRAL -> "neutral";
      default -> "";
    };
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
      effectId + "\t" + name + "\t" + image + "\t" + descriptionId + "\t" + getQualityDescription() + "\t" + attrs;

    if (defaultAction != null) {
      effectString += "\t" + defaultAction;
    }

    return effectString;
  }
}
