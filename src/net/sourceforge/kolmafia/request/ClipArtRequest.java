package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;

public class ClipArtRequest extends CreateItemRequest {
  public ClipArtRequest(final Concoction conc) {
    super("campground.php", conc);
  }

  @Override
  public void run() {
    if (!KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    UseSkillRequest request = UseSkillRequest.getInstance("Summon Clip Art", null, 1);

    int param = this.concoction.getParam();
    int clip1 = (param >> 16) & 0xFF;
    int clip2 = (param >> 8) & 0xFF;
    int clip3 = (param) & 0xFF;

    request.addFormField("clip1", String.valueOf(clip1));
    request.addFormField("clip2", String.valueOf(clip2));
    request.addFormField("clip3", String.valueOf(clip3));

    while (this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue()) {
      this.beforeQuantity = this.createdItem.getCount(KoLConstants.inventory);
      request.run();
      int createdQuantity = this.createdItem.getCount(KoLConstants.inventory) - this.beforeQuantity;

      if (createdQuantity == 0) {
        if (KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Creation failed, no results detected.");
        }

        return;
      }

      KoLmafia.updateDisplay(
          "Successfully created " + this.getName() + " (" + createdQuantity + ")");
      this.quantityNeeded -= createdQuantity;
    }
  }
}
