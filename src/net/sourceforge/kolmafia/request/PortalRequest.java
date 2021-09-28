package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class PortalRequest extends GenericRequest {
  private static final Pattern WHERE_PATTERN = Pattern.compile("action=(\\w*)elvibratoportal");

  private final AdventureResult item;

  /** Constructs a new <code>PortalRequest</code> */
  public PortalRequest(final AdventureResult item) {
    super("campground.php");

    this.item = item;

    switch (item.getItemId()) {
      case ItemPool.POWER_SPHERE:
        this.addFormField("action", "powerelvibratoportal");
        break;
      case ItemPool.OVERCHARGED_POWER_SPHERE:
        this.addFormField("action", "overpowerelvibratoportal");
        break;
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

    String action = matcher.group(1);
    if (action.equals("power")) {
      return ItemPool.get(ItemPool.POWER_SPHERE, -1);
    }
    if (action.equals("overpower")) {
      return ItemPool.get(ItemPool.OVERCHARGED_POWER_SPHERE, -1);
    }
    return null;
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("campground.php")) {
      return;
    }

    AdventureResult item = PortalRequest.getSphere(urlString);

    if (item == null) {
      return;
    }

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
