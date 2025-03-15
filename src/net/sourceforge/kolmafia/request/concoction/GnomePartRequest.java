package net.sourceforge.kolmafia.request.concoction;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.FamiliarManager;

public class GnomePartRequest extends CreateItemRequest {
  public GnomePartRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "597");
    // items are in order, starting at 5768
    var option = (conc.getItemId() - ItemPool.GNOMISH_EAR) + 1;
    this.addFormField("option", option);
  }

  public static int canMake(final Concoction conc) {
    if (!KoLCharacter.canUseFamiliar(FamiliarPool.REAGNIMATED_GNOME)) return 0;
    if (conc.getItemId() < ItemPool.GNOMISH_EAR || conc.getItemId() > ItemPool.GNOMISH_FOOT) {
      return 0;
    }
    return Preferences.getBoolean("_gnomePart") ? 0 : 1;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  protected boolean shouldAbortIfInChoice() {
    return false;
  }

  @Override
  public void run() {
    int count = 1;

    KoLmafia.updateDisplay("Creating " + count + " " + this.getName() + "...");

    int yield = this.getYield();

    var gnome = KoLCharacter.usableFamiliar(FamiliarPool.REAGNIMATED_GNOME);
    if (gnome == null) return;

    FamiliarData currentFam = KoLCharacter.getFamiliar();
    FamiliarManager.changeFamiliar(gnome, false);

    new GenericRequest("arena.php").run();
    this.setQuantityNeeded(Math.min(count, yield));
    super.run();

    FamiliarManager.changeFamiliar(currentFam);
  }

  public static boolean registerRequest(final String urlString) {
    return urlString.startsWith("choice.php") && urlString.contains("whichchoice=597");
  }
}
