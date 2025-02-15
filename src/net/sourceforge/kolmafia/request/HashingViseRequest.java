package net.sourceforge.kolmafia.request;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HashingViseRequest extends GenericRequest {
  public static final Set<AdventureResult> schematics = new HashSet<>();
  public static final Set<String> canonicalNames = new HashSet<>();

  static {
    registerSchematic("dedigitizer schematic: cyburger");
    registerSchematic("dedigitizer schematic: cybeer");
    registerSchematic("dedigitizer schematic: psilocyber mushroom");
    registerSchematic("dedigitizer schematic: brute force hammer");
    registerSchematic("dedigitizer schematic: malware injector");
    registerSchematic("dedigitizer schematic: cybervisor");
    registerSchematic("dedigitizer schematic: digibritches");
    registerSchematic("dedigitizer schematic: cryptocloak");
    registerSchematic("dedigitizer schematic: zero-trust tanktop");
    registerSchematic("dedigitizer schematic: retro floppy disk");
    registerSchematic("dedigitizer schematic: pocket GPU");
    registerSchematic("dedigitizer schematic: trojan horseshoe");
    registerSchematic("dedigitizer schematic: familiar-in-the-middle");
    registerSchematic("dedigitizer schematic: 3d printed server room key");
    registerSchematic("dedigitizer schematic: SLEEP(5) rom chip");
    registerSchematic("dedigitizer schematic: OVERCLOCK(10) rom chip");
    registerSchematic("dedigitizer schematic: STATS+++ rom chip");
    registerSchematic("dedigitizer schematic: insignificant bit");
    registerSchematic("dedigitizer schematic: hashing vise");
    registerSchematic("dedigitizer schematic: geofencing rapier");
    registerSchematic("dedigitizer schematic: geofencing shield");
    registerSchematic("dedigitizer schematic: virtual cybertattoo");
  }

  private static void registerSchematic(String name) {
    schematics.add(new AdventureResult(name, 1, false));
    canonicalNames.add(StringUtilities.getCanonicalName(name));
  }

  private static String[] CANONICAL_SCHEMATIC_ARRAY;

  static {
    CANONICAL_SCHEMATIC_ARRAY = canonicalNames.toArray(new String[canonicalNames.size()]);
  }

  public static final List<String> getMatchingNames(final String substring) {
    return StringUtilities.getMatchingNames(CANONICAL_SCHEMATIC_ARRAY, substring);
  }

  public static final AdventureResult HASHING_VISE = ItemPool.get(ItemPool.HASHING_VISE);
  private final AdventureResult item;

  // Hash a schematic
  public HashingViseRequest(AdventureResult schematic) {
    super("choice.php");
    this.addFormField("whichchoice", "1551");
    this.addFormField("option", "1");
    this.item = schematic;
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
    // If we are in a fight or a choice we can't walk away from, can't do this.
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    // If we don't have an accessible hashing vice, can't do this.
    if (!InventoryManager.hasItem(HASHING_VISE)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have a hashing vise to use.");
      return;
    }

    // If the item is not a schematic, we can't hash it.
    if (!schematics.contains(item)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "A hashing vise cannot be used on a " + item.getName());
      return;
    }

    // Determine how many schematics will be hashed.
    int available = InventoryManager.getCount(item);
    int needed = item.getCount();
    if (needed > available) {
      KoLmafia.updateDisplay(
          "(hashable quantity of "
              + item.getName()
              + " is limited to "
              + available
              + " by availability in inventory)");
      needed = available;
    }

    // If no schematics will be smashed, nothing more to do here.
    if (needed <= 0) {
      return;
    }

    // If we are already in choice 1551, we don't need to "use" the vise
    if (ChoiceManager.currentChoice() != 1551) {
      InventoryManager.retrieveItem(HASHING_VISE, true, false, false);
      GenericRequest useRequest = new GenericRequest("inv_use.php");
      useRequest.addFormField("whichitem", String.valueOf(ItemPool.HASHING_VISE));
      useRequest.run();
    }

    // Smash one schematic at a time
    while (KoLmafia.permitsContinue() && needed-- > 0) {
      super.run();
    }
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
