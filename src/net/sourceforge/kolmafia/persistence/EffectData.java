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

  private int effectId;
  private String name;
  private String image;
  private String descriptionId;
  private Quality quality = Quality.NEUTRAL;
  private List<String> attributes = new LinkedList<>();
  private String actions;

  public EffectData() {}

  public EffectData(
      final int effectId,
      final String name,
      final String image,
      final String descriptionId,
      final String quality,
      final List<String> attributes,
      final String actions) {
    this(
        effectId,
        name,
        image,
        descriptionId,
        Quality.fromDescription(quality),
        attributes,
        actions);
  }

  public EffectData(
      final int effectId,
      final String name,
      final String image,
      final String descriptionId,
      final Quality quality,
      final List<String> attributes,
      final String actions) {
    this.effectId = effectId;
    this.name = name;
    this.image = image;
    this.descriptionId = descriptionId;
    this.quality = quality;
    this.attributes = attributes;
    this.actions = actions;
  }

  public int getEffectId() {
    return this.effectId;
  }

  public String getName() {
    return this.name;
  }

  public String getImage() {
    return this.image;
  }

  public String getDescriptionId() {
    return this.descriptionId;
  }

  public Quality getQuality() {
    return this.quality;
  }

  public List<String> getAttributes() {
    return this.attributes;
  }

  public void setAttributes(final List<String> attributes) {
    this.attributes = attributes;
  }

  public String getActions() {
    return this.actions;
  }

  public void setActions(final String actions) {
    this.actions = actions;
  }

  public String getQualityDescription() {
    return this.quality.description();
  }

  public String toString() {
    // The effect file can have 3, 4, or 5 fields. "image" must be
    // present, even if we don't have the actual file name.
    String image = this.image == null ? "" : this.image;
    String descriptionId = this.descriptionId == null ? "" : this.descriptionId;

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
