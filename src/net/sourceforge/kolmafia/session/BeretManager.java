package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;

/** Busking beret seeding. */
public class BeretManager {
  private BeretManager() {}

  public static List<Integer> getBuskingPool() {
    var pool = new ArrayList<>(EffectDatabase.getGoodEffects(EffectPool.LIFTING_WETS));
    // The last entry is duplicated, so it comes up twice as often as the rest.
    pool.add(pool.getLast());
    return pool;
  }

  /** Power above 1100 has diminishing returns. */
  private static long capPower(final long power) {
    return Math.min(power, 1100) + (long) Math.floor(Math.pow(Math.max(0, power - 1100), 0.8));
  }

  /** What busking would grant right now. */
  public static List<AdventureResult> buskingEffects() {
    return buskingEffects(
        KoLCharacter.getTotalPower(), Preferences.getInteger("_beretBuskingUses"));
  }

  /**
   * What busking grants at a given power, on a given cast.
   *
   * @param power Total power of the player's equipment
   * @param cast How many times the beret has already been busked with today
   * @return The meat gained, followed by the effects granted
   */
  public static List<AdventureResult> buskingEffects(final long power, final long cast) {
    var cappedPower = capPower(power);

    var results = new ArrayList<AdventureResult>();
    results.add(new AdventureResult(AdventureResult.MEAT, (int) Math.ceil(cappedPower / 5.0) + 1));

    var pool = getBuskingPool();
    var rng = new PHPMTRandom(cappedPower + cast);

    var total = Math.ceil(cappedPower / 100.0);
    for (var i = 0; i < total; i++) {
      var effectId = rng.pickOne(pool);
      var duration = effectId == EffectPool.FISHY ? 1 : 10;
      results.add(new AdventureResult(EffectDatabase.getEffectName(effectId), duration, true));
    }

    return results;
  }
}
