package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class SaberRequest extends GenericRequest {
  public SaberRequest() {
    super("choice.php");
  }

  public static final void parseUpgrade(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1386")) {
      return;
    }
    // choice 5, walk away
    // You'll decide later. Maybe in a sequel.
    if (urlString.contains("option=5")) {
      return;
    }
    // choice 1, 10-15 MP regen
    // You fit the Kaiburr crystal to the end of your saber and feel energy course through you.
    if (urlString.contains("option=1") && responseText.contains("Kaiburr crystal")) {
      Preferences.setInteger("_saberMod", 1);
    }

    // choice 2, 20 ML
    // You pry out your boring blue crystal and put in a badass purple crystal.
    else if (urlString.contains("option=2") && responseText.contains("blue crystal")) {
      Preferences.setInteger("_saberMod", 2);
    }

    // choice 3, 3 resist all
    // Afraid of falling into some lava, you opt fo[sic] the resistance multiplier. The Force sure
    // works in mysterious ways.
    else if (urlString.contains("option=3") && responseText.contains("resistance multiplier")) {
      Preferences.setInteger("_saberMod", 3);
    }

    // choice 4, 10 familiar wt
    // You click the empathy chip in to place and really start to feel for your familiar companions.
    else if (urlString.contains("option=4") && responseText.contains("empathy chip")) {
      Preferences.setInteger("_saberMod", 4);
    }
  }

  public static final void parseForce(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1387")) {
      return;
    }

    if (urlString.contains("option=1")) {
      BanishManager.banishCurrentMonster(BanishManager.Banisher.SABER_FORCE);
      Preferences.increment("_saberForceUses");
    } else if (urlString.contains("option=2")) {
      Preferences.setString("_saberForceMonster", MonsterStatusTracker.getLastMonsterName());
      Preferences.setInteger("_saberForceMonsterCount", 3);
      Preferences.increment("_saberForceUses");
    } else if (urlString.contains("option=3")) {
      Preferences.increment("_saberForceUses");
    } else {
      return;
    }

    // Reset all combat state for the next fight.
    FightRequest.clearInstanceData();

    // Eventually try to reduce delay in the last adventured area, and remove the
    // last monster from the queue.  Not reducing delay when the fight didn't come
    // from a location will likely be non-trivial.
  }

  public static final void postForce(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1387")) {
      return;
    }

    // This is called from postChoice2, after items have been processed
    // and we are no longer handling the choice.

    if (urlString.contains("option=3")) {
      // The monster dropped all its items. If one of them forces
      // autocrafting, the attempt failed, because we were still
      // handling a choice when it was attempted.
      //
      // This is the only example we know with a non-conditional drop
      // that can be forced.
      ResultProcessor.autoCreate(ItemPool.BONERDAGON_NECKLACE);
    }
  }
}
