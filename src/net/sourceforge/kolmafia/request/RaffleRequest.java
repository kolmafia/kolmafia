package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RaffleRequest extends GenericRequest {
  private static final Pattern WHERE_PATTERN = Pattern.compile("where=(\\d+)");

  public enum RaffleSource {
    INVENTORY("0"),
    STORAGE("1");

    private final String name;

    RaffleSource(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public RaffleRequest(final int count, RaffleSource source) {
    super("raffle.php");

    this.addFormField("action", "buy");
    this.addFormField("where", source.toString());
    this.addFormField("quantity", String.valueOf(count));
  }

  public RaffleRequest(final int count) {
    this(count, RaffleRequest.chooseMeatSource());
  }

  private static RaffleSource chooseMeatSource() {
    if (KoLCharacter.isHardcore() || KoLCharacter.inRonin()) {
      return RaffleSource.STORAGE;
    }

    return RaffleSource.INVENTORY;
  }

  @Override
  public void run() {
    if (KoLCharacter.inZombiecore()) {
      KoLmafia.updateDisplay("You can't buy tickets as a Zombie");
      return;
    }

    KoLmafia.updateDisplay("Visiting the Raffle House...");
    super.run();
  }

  @Override
  public void processResults() {
    RaffleRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("raffle.php")) {
      return;
    }

    Matcher matcher = RaffleRequest.WHERE_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return;
    }

    String where = matcher.group(1);

    // You cannot afford that many tickets.
    if (responseText.contains("You cannot afford")) {
      String loc = where.equals(RaffleSource.INVENTORY.toString()) ? "inventory" : "storage";
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have enough meat in " + loc);
      return;
    }

    if (!responseText.contains("Here you go")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Ticket purchase failed");
      return;
    }

    matcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return;
    }

    int quantity = StringUtilities.parseInt(matcher.group(1));
    int cost = 10000 * quantity;

    if (where.equals(RaffleSource.STORAGE.toString())) {
      KoLCharacter.setStorageMeat(KoLCharacter.getStorageMeat() - cost);
    } else {
      ResultProcessor.processMeat(-cost);
    }

    AdventureResult item = ItemPool.get(ItemPool.RAFFLE_TICKET, quantity);
    ResultProcessor.processItem(false, "You acquire", item, null);
  }

  public static final boolean registerRequest(final String location) {
    if (!location.startsWith("raffle.php")) {
      return false;
    }

    Matcher matcher = RaffleRequest.WHERE_PATTERN.matcher(location);
    if (!matcher.find()) {
      return true;
    }

    String where = matcher.group(1);
    String loc =
        where.equals(RaffleSource.INVENTORY.toString())
            ? "inventory"
            : where.equals(RaffleSource.STORAGE.toString()) ? "storage" : "nowhere";

    matcher = GenericRequest.QUANTITY_PATTERN.matcher(location);
    if (!matcher.find()) {
      return true;
    }

    int quantity = StringUtilities.parseInt(matcher.group(1));

    RequestLogger.updateSessionLog("raffle " + quantity + " " + loc);

    return true;
  }
}
