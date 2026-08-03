package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class PortalRequest extends GenericRequest {
  private static final Pattern WHERE_PATTERN = Pattern.compile("action=(\\w*)elvibratoportal");

  private final AdventureResult item;

  /** Constructs a new <code>PortalRequest</code> */
  public PortalRequest(final AdventureResult item) {
    super("campground.php");

    this.item = item;

    switch (item.getItemId()) {
      case ItemPool.POWER_SPHERE -> this.addFormField("action", "powerelvibratoportal");
      case ItemPool.OVERCHARGED_POWER_SPHERE ->
          this.addFormField("action", "overpowerelvibratoportal");
    }
  }

  @Override
  public void run() {
    int iterations = this.item.getCount();

    for (int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i) {
      if (iterations == 1) {
        KoLmafia.updateDisplay("Charging your El Vibrato portal...");
      } else {
        KoLmafia.updateDisplay(
            "Charging your El Vibrato portal (" + i + " of " + iterations + ")...");
      }
      super.run();
    }

    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("Finished using " + iterations + " " + this.item.getName() + ".");
    }
  }

  @Override
  public void processResults() {
    PortalRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static AdventureResult getSphere(final String urlString) {
    Matcher matcher = PortalRequest.WHERE_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return null;
    }

    int itemId =
        switch (matcher.group(1)) {
          case "power" -> ItemPool.POWER_SPHERE;
          case "overpower" -> ItemPool.OVERCHARGED_POWER_SPHERE;
          default -> 0;
        };

    return itemId == 0 ? null : ItemPool.get(itemId, -1);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("campground.php")) {
      return;
    }

    AdventureResult item = PortalRequest.getSphere(urlString);
    if (item == null) {
      return;
    }

    int itemId = item.getItemId();
    int charges =
        switch (itemId) {
          case ItemPool.POWER_SPHERE -> 5;
          case ItemPool.OVERCHARGED_POWER_SPHERE -> 10;
          default -> 0;
        };

    // You insert the sphere into the base of the portal. There is a crackle of energy as it sinks
    // into the device and vanishes. The beams surrounding the portal glow more brightly.
    // You insert the sphere into the base of the portal. The pieces of the device rise from the
    // ground and energy arcs between them as the El Vibrato portal is reopened.

    // You insert the supercharged sphere into the base of the portal. There is a deafening crackle
    // of energy as it sinks into the device and vanishes. The beams surrounding the portal glow
    // more brightly.
    // You insert the supercharged sphere into the base of the portal. The pieces of the device rise
    // energetically from the ground and energy arcs between them as the El Vibrato portal is
    // reopened.

    if (responseText.contains("The pieces of the device rise")) {
      Preferences.setInteger("currentPortalEnergy", charges);
    } else if (responseText.contains("crackle of energy")) {
      Preferences.increment("currentPortalEnergy", charges);
    }

    CampgroundRequest.updateElVibratoPortal();
    ResultProcessor.processResult(item);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("campground.php")) {
      return false;
    }

    AdventureResult item = PortalRequest.getSphere(urlString);
    if (item == null) {
      return false;
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("use 1 " + item.getName());

    return true;
  }
}
