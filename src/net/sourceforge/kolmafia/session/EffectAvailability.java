package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessing;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessingLevel;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public final class EffectAvailability {
  private EffectAvailability() {}

  public static boolean cannotGain(int effectId) {
    return switch (effectId) {
      case EffectPool.NEARLY_SILENT_HUNTING -> KoLCharacter.isSealClubber();
      case EffectPool.SILENT_HUNTING, EffectPool.BARREL_CHESTED -> !KoLCharacter.isSealClubber();
      case EffectPool.BOON_OF_SHE_WHO_WAS ->
          KoLCharacter.getBlessingType() != TurtleBlessing.SHE_WHO_WAS
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BOON_OF_THE_STORM_TORTOISE ->
          KoLCharacter.getBlessingType() != TurtleBlessing.STORM
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BOON_OF_THE_WAR_SNAPPER ->
          KoLCharacter.getBlessingType() != TurtleBlessing.WAR
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.AVATAR_OF_SHE_WHO_WAS ->
          KoLCharacter.getBlessingType() != TurtleBlessing.SHE_WHO_WAS
              || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.AVATAR_OF_THE_STORM_TORTOISE ->
          KoLCharacter.getBlessingType() != TurtleBlessing.STORM
              || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.AVATAR_OF_THE_WAR_SNAPPER ->
          KoLCharacter.getBlessingType() != TurtleBlessing.WAR
              || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.BLESSING_OF_SHE_WHO_WAS ->
          !KoLCharacter.isTurtleTamer()
              || KoLCharacter.getBlessingType() == TurtleBlessing.SHE_WHO_WAS
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BLESSING_OF_THE_STORM_TORTOISE ->
          !KoLCharacter.isTurtleTamer()
              || KoLCharacter.getBlessingType() == TurtleBlessing.STORM
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BLESSING_OF_THE_WAR_SNAPPER ->
          !KoLCharacter.isTurtleTamer()
              || KoLCharacter.getBlessingType() == TurtleBlessing.WAR
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.DISDAIN_OF_SHE_WHO_WAS,
          EffectPool.DISDAIN_OF_THE_STORM_TORTOISE,
          EffectPool.DISDAIN_OF_THE_WAR_SNAPPER ->
          KoLCharacter.isTurtleTamer();
      case EffectPool.BARREL_OF_LAUGHS -> !KoLCharacter.isTurtleTamer();
      case EffectPool.FLIMSY_SHIELD_OF_THE_PASTALORD,
          EffectPool.BLOODY_POTATO_BITS,
          EffectPool.SLINKING_NOODLE_GLOB,
          EffectPool.WHISPERING_STRANDS,
          EffectPool.MACARONI_COATING,
          EffectPool.PENNE_FEDORA,
          EffectPool.PASTA_EYEBALL,
          EffectPool.SPICE_HAZE,
          EffectPool.LEGENDARY_BLOODY_POTATO_BITS,
          EffectPool.LEGENDARY_SLINKING_NOODLE_GLOB,
          EffectPool.LEGENDARY_WHISPERING_STRANDS,
          EffectPool.LEGENDARY_MACARONI_COATING,
          EffectPool.LEGENDARY_PENNE_FEDORA,
          EffectPool.LEGENDARY_PASTA_EYEBALL,
          EffectPool.LEGENDARY_SPICE_HAZE ->
          KoLCharacter.isPastamancer();
      case EffectPool.SHIELD_OF_THE_PASTALORD, EffectPool.PORK_BARREL ->
          !KoLCharacter.isPastamancer();
      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC,
          EffectPool.SOULERSKATES,
          EffectPool.WARLOCK_WARSTOCK_WARBARREL ->
          !KoLCharacter.isSauceror();
      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC_LITE -> KoLCharacter.isSauceror();
      case EffectPool.DOUBLE_BARRELED -> !KoLCharacter.isDiscoBandit();
      case EffectPool.BEER_BARREL_POLKA -> !KoLCharacter.isAccordionThief();
      case EffectPool.UNMUFFLED ->
          !Preferences.getString("peteMotorbikeMuffler").equals("Extra-Loud Muffler");
      case EffectPool.MUFFLED ->
          !Preferences.getString("peteMotorbikeMuffler").equals("Extra-Quiet Muffler");
      default -> false;
    };
  }
}
