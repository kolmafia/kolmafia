package net.sourceforge.kolmafia.combat;

public class CombatUtilities {
  private CombatUtilities() {}

  public static final float hitChance(
      final int attack, final int defense, final float critical, final float fumble) {
    // The +d10-d10 in the Hit Chance formula means the distribution is not linear.
    //
    // According to the Wiki
    //    http://kol.coldfront.net/thekolwiki/index.php/Monsters#Monster_Hit_Chance
    // it is the Cumulative Distribution Function of a triangular distribution
    //    https://en.wikipedia.org/wiki/Triangular_distribution

    // a = -9, b = 10, c = 0.5, x = defense - attack

    float missChance =
        CombatUtilities.triangularDistributionCDF(-9.0f, 10.0f, 0.5f, defense - attack);
    return Math.min(1.0f - missChance + critical, 1.0f - fumble);
  }

  public static final float triangularDistributionCDF(
      final float a, final float b, final float c, final float x) {
    /*
      (defun cdf (a b c x)
        (cond ((<= x a)
               0)
              ((<= b x)
               1)
              ((<= x c)
               (/ (* (- x a) (- x a)) (* (- b a) (- c a))))
              (t
               (- 1 (/ (* (- b x) (- b x)) (* (- b a) (- b c)))))))
    */
    return x <= a
        ? 0.0f
        : b <= x
            ? 1.0f
            : x <= c
                ? ((x - a) * (x - a)) / ((b - a) * (c - a))
                : (1.0f - ((b - x) * (b - x)) / ((b - a) * (b - c)));
  }
}
