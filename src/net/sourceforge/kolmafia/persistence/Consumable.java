package net.sourceforge.kolmafia.persistence;

import java.util.Arrays;
import java.util.Set;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;

// Consumable represents the static attributes of a consumable as pulled out of a line in the
// relevant data file.
public class Consumable {
  // indexes into statRanges.
  public static final int MUSCLE = 0;
  public static final int MYSTICALITY = 1;
  public static final int MOXIE = 2;

  public String name;
  public int level;
  private Integer fullness;
  private Integer inebriety;
  private Integer spleenHit;
  public ConsumablesDatabase.ConsumableQuality quality;
  public String adventureRange;
  public String[] statRangeStrings;
  public String notes;

  public int itemId;
  public int adventureStart;
  public int adventureEnd;
  public int[][] statRanges;

  public Set<String> aliases;

  public Consumable(
      String name,
      Integer fullness,
      Integer inebriety,
      Integer spleenHit,
      int level,
      ConsumablesDatabase.ConsumableQuality quality,
      String adventureRange,
      String[] statRangeStrings,
      String notes,
      Set<String> aliases) {
    this.name = name;
    this.fullness = fullness;
    this.inebriety = inebriety;
    this.spleenHit = spleenHit;
    this.level = level;
    this.quality = quality;
    this.adventureRange = adventureRange;
    this.statRangeStrings = statRangeStrings;
    this.notes = notes;
    this.aliases = aliases;

    this.itemId = ItemDatabase.getItemId(this.name, 1, false);

    int[] adventureArray = rangeStringToArray(this.adventureRange);
    this.adventureStart = adventureArray[0];
    this.adventureEnd = adventureArray[1];

    this.statRanges =
        Arrays.stream(this.statRangeStrings)
            .map(Consumable::rangeStringToArray)
            .toArray(int[][]::new);
  }

  private static int[] rangeStringToArray(String rangeString) {
    if (rangeString.length() == 0) {
      return new int[] {0, 0};
    }

    boolean invert = rangeString.charAt(0) == '-';

    String[] rangeArray = rangeString.substring(invert ? 1 : 0).split("-");
    return new int[] {
      (invert ? -1 : 1) * Integer.parseInt(rangeArray[0]),
      (invert ? -1 : 1) * Integer.parseInt(rangeArray[rangeArray.length == 2 ? 1 : 0])
    };
  }

  public ConsumptionType getConsumptionType() {
    return this.fullness != null
        ? ConsumptionType.EAT
        : this.inebriety != null
            ? ConsumptionType.DRINK
            : this.spleenHit != null ? ConsumptionType.SPLEEN : ConsumptionType.USE;
  }

  public Integer getRawFullness() {
    return this.fullness;
  }

  public int getFullness() {
    return this.fullness == null ? 0 : this.fullness;
  }

  public Integer getRawInebriety() {
    return this.inebriety;
  }

  public int getInebriety() {
    return this.inebriety == null ? 0 : this.inebriety;
  }

  public Integer getRawSpleenHit() {
    return this.spleenHit;
  }

  public int getSpleenHit() {
    return this.spleenHit == null ? 0 : this.spleenHit;
  }

  public Integer getRawSize() {
    return this.fullness != null
        ? this.fullness
        : this.inebriety != null ? this.inebriety : this.spleenHit;
  }

  public int getSize() {
    Integer rawSize = this.getRawSize();
    return rawSize == null ? 0 : rawSize;
  }

  public int[] getStatRange(int stat) {
    return this.statRanges[stat];
  }

  public Concoction getConcoction() {
    return ConcoctionPool.get(this.itemId, this.name);
  }

  public String toString() {
    return this.name
        + "\t"
        + this.getSize()
        + "\t"
        + this.level
        + "\t"
        + this.quality
        + "\t"
        + this.adventureRange
        + "\t"
        + String.join("\t", this.statRangeStrings)
        + (notes == null ? "" : ("\t" + notes));
  }

  public boolean isMartini() {
    return this.notes != null && this.notes.contains("MARTINI");
  }

  public boolean isLasagna() {
    return this.notes != null && this.notes.contains("LASAGNA");
  }

  public boolean isSaucy() {
    return this.notes != null && this.notes.contains("SAUCY");
  }

  public boolean isPizza() {
    return this.notes != null && this.notes.contains("PIZZA");
  }

  public boolean isBeans() {
    return this.notes != null && this.notes.contains("BEANS");
  }

  public boolean isWine() {
    return this.notes != null && this.notes.contains("WINE");
  }

  public boolean isSalad() {
    return this.notes != null && this.notes.contains("SALAD");
  }

  public boolean isBeer() {
    return this.notes != null && this.notes.contains("BEER");
  }

  public boolean isCannedBeer() {
    return this.notes != null && this.notes.contains("CANNED");
  }
}
