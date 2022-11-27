package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PulverizeRequest extends GenericRequest {
  public static final Pattern ITEMID_PATTERN = Pattern.compile("smashitem=(\\d+)");
  private static final Map<String, String> UPGRADES = new HashMap<>();

  static {
    UPGRADES.put("powder", "nuggets");
    UPGRADES.put("nuggets", "wad");
    UPGRADES.put("sand", "pebbles");
    UPGRADES.put("pebbles", "gravel");
    UPGRADES.put("gravel", "rock");
  }

  private AdventureResult item;

  public PulverizeRequest(final AdventureResult item) {
    super("craft.php");
    this.addFormField("action", "pulverize");

    this.item = item;
    this.addFormField("smashitem", String.valueOf(item.getItemId()));
    this.addFormField("qty", String.valueOf(item.getCount()));
    this.addFormField("ajax", "1");

    // 1 to confirm smashing untradables
    this.addFormField("conftrade", "1");
  }

  public void useMalus(final String itemName, final int quantity) {
    if (itemName == null || !ItemDatabase.contains(itemName)) {
      return;
    }

    int itemId = ItemDatabase.getItemId(itemName);
    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);

    if (ingredients == null || ingredients.length == 0) {
      return;
    }

    int amountNeeded = Math.min(quantity, ingredients[0].getCount(KoLConstants.inventory) / 5);
    if (amountNeeded == 0) {
      return;
    }

    CreateItemRequest icr = CreateItemRequest.getInstance(itemId);
    if (icr == null) {
      return;
    }

    icr.setQuantityNeeded(amountNeeded);
    icr.run();
  }

  @Override
  public void run() {
    if (Preferences.getBoolean("mementoListActive")
        && KoLConstants.mementoList.contains(this.item)) {
      KoLmafia.updateDisplay("(smashing of 'Memento' item " + this.item + " disallowed)");
      return;
    }

    if (this.item.getCount(KoLConstants.inventory) == this.item.getCount()) {
      if (!KoLCharacter.canInteract() && !KoLConstants.junkList.contains(this.item)) {
        KoLConstants.junkList.add(this.item);
      }

      if (KoLConstants.singletonList.contains(this.item)
          && !KoLConstants.closet.contains(this.item)) {
        KoLmafia.updateDisplay(
            "(smashable quantity of 'Keep One' item " + this.item + " reduced by 1)");
        this.item = this.item.getInstance(this.item.getCount() - 1);
        if (this.item.getCount() <= 0) {
          return;
        }
        this.addFormField("qty", String.valueOf(this.item.getCount()));
      }
    }

    switch (ItemDatabase.getConsumptionType(this.item.getItemId())) {
      case ACCESSORY:
      case HAT:
      case PANTS:
      case SHIRT:
      case WEAPON:
      case OFFHAND:
      case CONTAINER:
        break;

      default:
        int qty = this.item.getCount();
        String name = this.item.getName();
        int space = name.lastIndexOf(" ") + 1;
        String upgrade = PulverizeRequest.UPGRADES.get(name.substring(space));
        if (upgrade != null) {
          this.useMalus(name.substring(0, space) + upgrade, qty / 5);
        }

        return;
    }

    if (!KoLCharacter.hasSkill("Pulverize")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't know how to pulverize objects.");
      return;
    }

    if (!InventoryManager.retrieveItem(ItemPool.TENDER_HAMMER)) {
      return;
    }

    if (this.item.getCount(KoLConstants.inventory) < this.item.getCount()) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You don't have that many " + this.item.getName() + ".");
      return;
    }

    KoLmafia.updateDisplay("Pulverizing " + this.item + "...");
    super.run();
  }

  @Override
  public void processResults() {
    if (PulverizeRequest.parseResponse(this.getURLString(), this.responseText) == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "The " + this.item + " could not be smashed.");
      return;
    }

    KoLmafia.updateDisplay(this.item + " smashed.");
  }

  public static final int parseResponse(final String urlString, final String responseText) {
    // That's too important to pulverize.
    // That's not something you can pulverize.
    // You don't know how to properly smash stuff.
    // You haven't got that many.

    if (responseText.indexOf("too important to pulverize") != -1
        || responseText.indexOf("not something you can pulverize") != -1
        || responseText.indexOf("don't know how to properly smash stuff") != -1
        || responseText.indexOf("haven't got that many") != -1) {
      return 0;
    }

    Matcher itemMatcher = PulverizeRequest.ITEMID_PATTERN.matcher(urlString);

    if (!itemMatcher.find()) {
      return 0;
    }

    Matcher quantityMatcher = GenericRequest.QTY_PATTERN.matcher(urlString);

    if (!quantityMatcher.find()) {
      return 0;
    }

    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    int quantity = StringUtilities.parseInt(quantityMatcher.group(1));

    ResultProcessor.processResult(ItemPool.get(itemId, 0 - quantity));

    return quantity;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("craft.php") || urlString.indexOf("action=pulverize") == -1) {
      return false;
    }

    Matcher itemMatcher = PulverizeRequest.ITEMID_PATTERN.matcher(urlString);

    if (!itemMatcher.find()) {
      return false;
    }

    Matcher quantityMatcher = GenericRequest.QTY_PATTERN.matcher(urlString);

    if (!quantityMatcher.find()) {
      return false;
    }

    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    String name = ItemDatabase.getItemName(itemId);

    if (name == null) {
      return true;
    }

    int quantity = StringUtilities.parseInt(quantityMatcher.group(1));

    RequestLogger.updateSessionLog("pulverize " + quantity + " " + name);

    return true;
  }
}
