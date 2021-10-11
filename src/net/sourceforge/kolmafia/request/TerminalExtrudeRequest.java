package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;

public class TerminalExtrudeRequest extends CreateItemRequest {
  public TerminalExtrudeRequest(final Concoction conc) {
    super("choice.php", conc);
  }

  @Override
  public void run() {
    if (!KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0) {
      return;
    }

    String creation = this.getName();
    String output = null;

    if (creation.equals("hacked gibson")) {
      output = "extrude -f booze.ext";
    } else if (creation.equals("browser cookie")) {
      output = "extrude -f food.ext";
    } else if (creation.equals("software bug")) {
      output = "extrude -f familiar.ext";
    } else if (creation.equals("Source shades")) {
      output = "extrude -f goggles.ext";
    } else if (creation.equals("Source terminal CRAM chip")) {
      output = "extrude -f cram.ext";
    } else if (creation.equals("Source terminal DRAM chip")) {
      output = "extrude -f dram.ext";
    } else if (creation.equals("Source terminal GRAM chip")) {
      output = "extrude -f gram.ext";
    } else if (creation.equals("Source terminal PRAM chip")) {
      output = "extrude -f pram.ext";
    } else if (creation.equals("Source terminal SPAM chip")) {
      output = "extrude -f spam.ext";
    } else if (creation.equals("Source terminal TRAM chip")) {
      output = "extrude -f tram.ext";
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot create " + creation);
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + creation + "...");

    while (this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue()) {
      this.beforeQuantity = this.createdItem.getCount(KoLConstants.inventory);
      RequestThread.postRequest(new TerminalRequest(output));
      int createdQuantity = this.createdItem.getCount(KoLConstants.inventory) - this.beforeQuantity;

      if (createdQuantity == 0) {
        if (KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Creation failed, no results detected.");
        }

        return;
      }

      KoLmafia.updateDisplay("Successfully created " + creation + " (" + createdQuantity + ")");
      this.quantityNeeded -= createdQuantity;
    }
  }
}
