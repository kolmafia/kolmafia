package net.sourceforge.kolmafia;

import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;

public enum ZodiacSign {
  NONE("None", 0, ZodiacType.NONE, ZodiacZone.NONE),
  MONGOOSE("Mongoose", 1, ZodiacType.MUSCLE, ZodiacZone.KNOLL),
  WALLABY("Wallaby", 2, ZodiacType.MYSTICALITY, ZodiacZone.KNOLL),
  VOLE("Vole", 3, ZodiacType.MOXIE, ZodiacZone.KNOLL),
  PLATYPUS("Platypus", 4, ZodiacType.MUSCLE, ZodiacZone.CANADIA),
  OPOSSUM("Opossum", 5, ZodiacType.MYSTICALITY, ZodiacZone.CANADIA),
  MARMOT("Marmot", 6, ZodiacType.MOXIE, ZodiacZone.CANADIA),
  WOMBAT("Wombat", 7, ZodiacType.MUSCLE, ZodiacZone.GNOMADS),
  BLENDER("Blender", 8, ZodiacType.MYSTICALITY, ZodiacZone.GNOMADS),
  PACKRAT("Packrat", 9, ZodiacType.MOXIE, ZodiacZone.GNOMADS),
  BAD_MOON("Bad Moon", 10, ZodiacType.BAD_MOON, ZodiacZone.NONE),
  ;

  public static final String[] ZODIACS =
      new String[] {
        "Mongoose",
        "Wallaby",
        "Vole",
        "Platypus",
        "Opossum",
        "Marmot",
        "Wombat",
        "Blender",
        "Packrat"
      };

  private final String name;
  private final int id;
  private final ZodiacType type;
  private final ZodiacZone zone;

  ZodiacSign(String name, int id, ZodiacType type, ZodiacZone zone) {
    this.name = name;
    this.id = id;
    this.type = type;
    this.zone = zone;
  }

  public String getName() {
    return name;
  }

  public int getId() {
    return id;
  }

  public ZodiacType getType() {
    return type;
  }

  public ZodiacZone getZone() {
    return zone;
  }

  public static final ZodiacSign find(final String name) {
    for (ZodiacSign sign : ZodiacSign.values()) {
      if (name.equalsIgnoreCase(sign.getName())) {
        return sign;
      }
    }
    return ZodiacSign.NONE;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
