package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;

/** Yam battery seeding spaded by VeeArr. */
public class MayamManager {
  private MayamManager() {}

  /** A yam battery's three effects arrive with these durations, in this order. */
  private static final int[] YAM_BATTERY_DURATIONS = {10, 20, 30};

  public static List<Integer> getYamBatteryPool() {
    return EffectDatabase.getGoodEffects(EffectPool.TIKI_TEMERITY);
  }

  /** The effects a yam battery would grant today. */
  public static List<AdventureResult> yamBatteryEffects() {
    return yamBatteryEffects(KoLCharacter.getGlobalDays());
  }

  /**
   * The effects a yam battery grants on a given global day.
   *
   * @param daycount Global KoL day, as in KoLCharacter.getGlobalDays()
   * @return Three effects, paired with their durations
   */
  public static List<AdventureResult> yamBatteryEffects(final int daycount) {
    if (daycount <= 0) return List.of();

    var pool = getYamBatteryPool();
    var mtRng = new PHPMTRandom(11L * daycount);

    var effects = new ArrayList<AdventureResult>(YAM_BATTERY_DURATIONS.length);
    for (var duration : YAM_BATTERY_DURATIONS) {
      // The roll can overflow the pool by 1, which resolves to the last effect, so it comes up
      // twice as often as the rest.
      var index = Math.min(mtRng.nextInt(0, pool.size()), pool.size() - 1);
      effects.add(EffectPool.get(pool.get(index), duration));
    }

    return effects;
  }
}
