package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public enum ModifierType {
  ITEM,
  EFFECT,
  SKILL,
  PATH,
  CLASS,
  LOC,
  ZONE,
  OUTFIT,
  CAMPGROUND,
  SIGN,
  MCD,
  BALLROOM,
  EVENT,
  PSEUDO_ITEM,
  PSEUDO_EFFECT,
  PSEUDO_SKILL,
  EQUIPMENT_POWER,
  FAKE_HANDS,
  INVENTORY_ITEM,
  EL_VIBRATO,
  SYNERGY,
  MAX_CAT,
  BJORN,
  THRONE,
  EDPIECE,
  BACKUP_CAMERA,
  BOOM_BOX,
  JURASSIC_PARKA,
  MASK,
  RETRO_CAPE,
  UNBREAKABLE_UMBRELLA,
  LED_CANDLE,
  AUTUMNATON,
  SNOW_SUIT,
  FAMILIAR,
  FAM_EQ,
  VOLLEYBALL,
  TUNED_VOLLEYBALL,
  MUMMERY,
  THRALL,
  VYKEA,
  MOTORBIKE,
  CLANCY,
  COMPANION,
  SERVANT,
  ENSORCEL,
  FLORIST,
  HORSERY,
  LOCAL_VOTE,
  ROBOT,
  ROBOT_CPU,
  ROBOT_TOP,
  ROBOT_BOTTOM,
  ROBOT_LEFT,
  ROBOT_RIGHT,
  DAILY_LIMIT,
  MUTEX_GENERIC,
  MUTEX_I,
  MUTEX_E,
  PASSIVES,
  GENERATED,
  TERRARIUM_FAMILIAR,
  SEAHORSE,
  NONE;

  private static Map<String, ModifierType> stringToType =
      Arrays.stream(values())
          .collect(
              Collectors.toMap(
                  type -> type.name().replace("_", "").toLowerCase(), Function.identity()));

  public static ModifierType fromString(String typeString) {
    return stringToType.get(typeString.replace("_", "").toLowerCase());
  }

  public String pascalCaseName() {
    return StringUtilities.upperSnakeToPascalCase(this.name());
  }

  public String wordsName() {
    return StringUtilities.upperSnakeToWords(this.name());
  }
}
