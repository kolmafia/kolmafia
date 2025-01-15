package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HashingViseRequest extends GenericRequest {
  public static final List<AdventureResult> schematics = new ArrayList<>();

  static {
    schematics.add(new AdventureResult("dedigitizer schematic: cyburger", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: cybeer", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: psilocyber mushroom", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: brute force hammer", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: malware injector", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: cybervisor", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: digibritches", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: cryptocloak", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: zero-trust tanktop", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: retro floppy disk", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: pocket GPU", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: trojan horseshoe", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: familiar-in-the-middle", 1, false));
    schematics.add(
        new AdventureResult("dedigitizer schematic: 3d printed server room key", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: SLEEP(5) rom chip", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: OVERCLOCK(10) rom chip", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: STATS+++ rom chip", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: insignificant bit", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: hashing vise", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: geofencing rapier", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: geofencing shield", 1, false));
    schematics.add(new AdventureResult("dedigitizer schematic: virtual cybertattoo", 1, false));
  }

  // Hash a schematic
  public HashingViseRequest(AdventureResult schematic) {
    super("choice.php");
    this.addFormField("whichchoice", "1551");
    this.addFormField("option", "1");
    this.addFormField("iid", String.valueOf(schematic.getItemId()));
  }

  public static final Pattern URL_IID_PATTERN = Pattern.compile("iid=(\\d+)");

  public static int getIID(final String urlString) {
    Matcher matcher = HashingViseRequest.URL_IID_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    int itemId = ItemPool.HASHING_VISE;
    if (!InventoryManager.hasItem(itemId)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have a hashing vise to use.");
      return;
    }

    InventoryManager.retrieveItem(itemId, 1, true, false, false);

    GenericRequest useRequest = new GenericRequest("inv_use.php");
    useRequest.addFormField("whichitem", String.valueOf(itemId));
    useRequest.run();

    super.run();
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php")) {
      return false;
    }

    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);

    if (choice != 1551) {
      return false;
    }

    int iid = getIID(urlString);
    if (iid == 0) {
      return false;
    }

    String name = ItemDatabase.getDisplayName(iid);

    String message = "vise " + name;
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
