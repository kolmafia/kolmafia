package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class HellKitchenRequest extends CafeRequest {
  public HellKitchenRequest(final String name) {
    super("Hell's Kitchen", "3");

    int itemId = ItemDatabase.getItemId(name);
    int price = Math.max(1, ItemDatabase.getPriceById(itemId)) * 3;
    this.setItem(name, itemId, price);
  }

  @Override
  public void run() {
    if (KoLCharacter.inBadMoon()) {
      super.run();
    }
  }

  public static final boolean onMenu(final String name) {
    return KoLConstants.kitchenItems.contains(name);
  }

  public static final void getMenu() {
    KoLmafia.updateDisplay("Visiting Hell's Kitchen...");
    KoLConstants.kitchenItems.clear();
    CafeRequest.addMenuItem(KoLConstants.kitchenItems, "Jumbo Dr. Lucifer", 150);
    CafeRequest.addMenuItem(KoLConstants.kitchenItems, "Brimstone Chicken Sandwich", 300);
    CafeRequest.addMenuItem(KoLConstants.kitchenItems, "Lord of the Flies-sized fries", 300);
    CafeRequest.addMenuItem(KoLConstants.kitchenItems, "Double Bacon Beelzeburger", 300);
    CafeRequest.addMenuItem(KoLConstants.kitchenItems, "Imp Ale", 75);
    ConcoctionDatabase.getUsables().sort();
    KoLmafia.updateDisplay("Menu retrieved.");
  }

  public static final void reset() {
    CafeRequest.reset(KoLConstants.kitchenItems);
  }
}
