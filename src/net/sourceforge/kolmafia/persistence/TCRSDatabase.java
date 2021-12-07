package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TCRSDatabase {
  // Item attributes that vary by class/sign in a Two Random Crazy Summer run
  public static class TCRS {
    public final String name;
    public final int size;
    public final String quality;
    public final String modifiers;

    TCRS(String name, int size, String quality, String modifiers) {
      this.name = name;
      this.size = size;
      this.quality = quality;
      this.modifiers = modifiers;
    }
  }

  private static class TCRSDeriveRunnable implements Runnable {
    private int itemId;

    public TCRSDeriveRunnable(final int itemId) {
      this.itemId = itemId;
    }

    public void run() {
      String text = DebugDatabase.itemDescriptionText(itemId, false);
      if (text == null) {
        return;
      }

      TCRS tcrs = deriveItem(text);

      if (tcrs == null) {
        return;
      }

      TCRSMap.put(itemId, tcrs);
    }
  }

  private static String currentClassSign; // Character class/Zodiac Sign

  // Sorted by itemId
  private static final Map<Integer, TCRS> TCRSMap = new TreeMap<Integer, TCRS>();
  private static final Map<Integer, TCRS> TCRSBoozeMap =
      new TreeMap<Integer, TCRS>(new CafeDatabase.InverseIntegerOrder());
  private static final Map<Integer, TCRS> TCRSFoodMap =
      new TreeMap<Integer, TCRS>(new CafeDatabase.InverseIntegerOrder());

  static {
    TCRSDatabase.reset();
  }

  public static void reset() {
    currentClassSign = "";
    TCRSMap.clear();
    TCRSBoozeMap.clear();
    TCRSFoodMap.clear();
  }

  public static String getTCRSName(int itemId) {
    TCRS tcrs = TCRSMap.get(itemId);
    return (tcrs == null) ? ItemDatabase.getDataName(itemId) : tcrs.name;
  }

  public static String filename() {
    return filename(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), "");
  }

  public static boolean validate(AscensionClass ascensionClass, String csign) {
    return (ascensionClass != null
        && ascensionClass.isStandard()
        && ZodiacSign.find(csign).isStandard());
  }

  public static String filename(AscensionClass ascensionClass, String sign, String suffix) {
    if (!validate(ascensionClass, sign)) {
      return "";
    }

    return "TCRS_"
        + StringUtilities.globalStringReplace(ascensionClass.getName(), " ", "_")
        + "_"
        + sign
        + suffix
        + ".txt";
  }

  public static boolean load(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }
    boolean retval = true;
    retval &= load(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    retval &= loadCafe(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    return retval;
  }

  public static boolean load(AscensionClass ascensionClass, String csign, final boolean verbose) {
    if (load(filename(ascensionClass, csign, ""), TCRSMap, verbose)) {
      currentClassSign = ascensionClass.getName() + "/" + csign;
      return true;
    }
    return false;
  }

  public static boolean loadCafe(
      AscensionClass ascensionClass, String csign, final boolean verbose) {
    boolean retval = true;
    retval &= load(filename(ascensionClass, csign, "_cafe_booze"), TCRSBoozeMap, verbose);
    retval &= load(filename(ascensionClass, csign, "_cafe_food"), TCRSFoodMap, verbose);
    return true;
  }

  private static boolean load(String fileName, Map<Integer, TCRS> map, final boolean verbose) {
    map.clear();

    BufferedReader reader = FileUtilities.getReader(fileName);

    // No reader, no file
    if (reader == null) {
      if (verbose) {
        RequestLogger.printLine("Could not read file " + fileName);
      }
      return false;
    }

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 5) {
        continue;
      }
      int itemId = StringUtilities.parseInt(data[0]);
      String name = data[1];
      int size = StringUtilities.parseInt(data[2]);
      String quality = data[3];
      String modifiers = data[4];

      TCRS item = new TCRS(name, size, quality, modifiers);
      map.put(itemId, item);
    }

    if (verbose) {
      RequestLogger.printLine("Read file " + fileName);
    }

    return true;
  }

  public static boolean save(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }
    boolean retval = true;
    retval &= save(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    retval &= saveCafe(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    return retval;
  }

  public static boolean save(AscensionClass ascensionClass, String csign, final boolean verbose) {
    return save(filename(ascensionClass, csign, ""), TCRSMap, verbose);
  }

  public static boolean saveCafe(
      AscensionClass ascensionClass, String csign, final boolean verbose) {
    boolean retval = true;
    retval &= save(filename(ascensionClass, csign, "_cafe_booze"), TCRSBoozeMap, verbose);
    retval &= save(filename(ascensionClass, csign, "_cafe_food"), TCRSFoodMap, verbose);
    return true;
  }

  public static boolean saveCafeBooze(
      AscensionClass ascensionClass, String csign, final boolean verbose) {
    return save(filename(ascensionClass, csign, "_cafe_booze"), TCRSBoozeMap, verbose);
  }

  public static boolean saveCafeFood(
      AscensionClass ascensionClass, String csign, final boolean verbose) {
    return save(filename(ascensionClass, csign, "_cafe_food"), TCRSFoodMap, verbose);
  }

  private static boolean save(
      final String fileName, final Map<Integer, TCRS> map, final boolean verbose) {
    if (fileName == null) {
      return false;
    }

    PrintStream writer = LogStream.openStream(new File(KoLConstants.DATA_LOCATION, fileName), true);

    // No writer, no file
    if (writer == null) {
      if (verbose) {
        RequestLogger.printLine("Could not write file " + fileName);
      }
      return false;
    }

    for (Entry<Integer, TCRS> entry : map.entrySet()) {
      TCRS tcrs = entry.getValue();
      Integer itemId = entry.getKey();
      String name = tcrs.name;
      Integer size = tcrs.size;
      String quality = tcrs.quality;
      String modifiers = tcrs.modifiers;
      String line = itemId + "\t" + name + "\t" + size + "\t" + quality + "\t" + modifiers;
      writer.println(line);
    }

    writer.close();

    if (verbose) {
      RequestLogger.printLine("Wrote file " + fileName);
    }

    return true;
  }

  public static boolean derive(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    derive(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    deriveCafe(verbose);
    return true;
  }

  private static boolean derive(
      final AscensionClass ascensionClass, final String sign, final boolean verbose) {
    // If we don't currently have data for this class/sign, start fresh
    String classSign = ascensionClass.getName() + "/" + sign;
    if (!currentClassSign.equals(classSign)) {
      reset();
    }

    Set<Integer> keys = ItemDatabase.descriptionIdKeySet();

    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all real items...");
    }

    List<Runnable> actions = new ArrayList<>();

    for (Integer id : keys) {
      actions.add(new TCRSDeriveRunnable(id));
    }

    RequestThread.runInParallel(actions, verbose);

    currentClassSign = classSign;

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    return true;
  }

  public static boolean derive(final int itemId) {
    // Don't do this if we already know the item
    if (TCRSMap.containsKey(itemId)) {
      return false;
    }

    TCRS tcrs = deriveItem(itemId);
    if (tcrs == null) {
      return false;
    }

    TCRSMap.put(itemId, tcrs);

    return true;
  }

  public static int update(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return 0;
    }

    Set<Integer> keys = ItemDatabase.descriptionIdKeySet();

    if (verbose) {
      KoLmafia.updateDisplay("Updating TCRS item adjustments for real items...");
    }

    int count = 0;
    for (Integer id : keys) {
      // For a while, we stored the hewn moon-rune spoon
      // without modifiers.  If the data file we loaded has
      // that, force derive here to get the real modifiers.
      if (id == ItemPool.HEWN_MOON_RUNE_SPOON) {
        TCRS tcrs = TCRSMap.get(id);
        if (tcrs != null && "hewn moon-rune spoon".equals(tcrs.name)) {
          TCRSMap.remove(id);
        }
      }

      if (derive(id)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new items seen");
    }

    return count;
  }

  public static int updateCafeBooze(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return 0;
    }

    if (verbose) {
      KoLmafia.updateDisplay("Updating TCRS item adjustments for cafe booze items...");
    }

    int count = 0;
    for (Integer id : CafeDatabase.cafeBoozeKeySet()) {
      if (deriveCafe(id, CafeDatabase.boozeDescId(id), TCRSBoozeMap)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new cafe boozes seen");
    }

    return count;
  }

  public static int updateCafeFood(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return 0;
    }

    if (verbose) {
      KoLmafia.updateDisplay("Updating TCRS item adjustments for cafe food items...");
    }

    int count = 0;
    for (Integer id : CafeDatabase.cafeFoodKeySet()) {
      if (deriveCafe(id, CafeDatabase.foodDescId(id), TCRSFoodMap)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new cafe foods seen");
    }

    return count;
  }

  public static TCRS deriveItem(final int itemId) {
    // The "ring" is the path reward for completing a TCRS run.
    // Its enchantments are character-specific.
    if (itemId == ItemPool.RING) {
      return new TCRS("ring", 0, "", "Single Equip");
    }

    // Read the Item Description
    String text = DebugDatabase.itemDescriptionText(itemId, false);
    if (text == null) {
      return null;
    }
    return deriveItem(text);
  }

  public static TCRS deriveAndSaveItem(final int itemId) {
    TCRS tcrs = deriveItem(itemId);
    if (tcrs != null) {
      TCRSMap.put(itemId, tcrs);
    }
    return tcrs;
  }

  public static TCRS deriveRing() {
    String text = DebugDatabase.itemDescriptionText(ItemPool.RING, false);
    return deriveItem(text);
  }

  public static TCRS deriveSpoon() {
    String text = DebugDatabase.itemDescriptionText(ItemPool.HEWN_MOON_RUNE_SPOON, false);
    return deriveItem(text);
  }

  public static void deriveApplyItem(final int id) {
    applyModifiers(id, deriveItem(DebugDatabase.itemDescriptionText(id, false)));
  }

  private static TCRS deriveItem(final String text) {
    // Parse the things that are changed in TCRS
    String name = DebugDatabase.parseName(text);
    int size = DebugDatabase.parseConsumableSize(text);
    String quality = DebugDatabase.parseQuality(text);
    ArrayList<String> unknown = new ArrayList<String>();
    String modifiers = DebugDatabase.parseItemEnchantments(text, unknown, -1);

    // Create and return the TCRS object
    return new TCRS(name, size, quality, modifiers);
  }

  private static boolean deriveCafe(final boolean verbose) {
    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all cafe booze items...");
    }

    for (Integer id : CafeDatabase.cafeBoozeKeySet()) {
      deriveCafe(id, CafeDatabase.boozeDescId(id), TCRSBoozeMap);
    }

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all cafe food items...");
    }

    for (Integer id : CafeDatabase.cafeFoodKeySet()) {
      deriveCafe(id, CafeDatabase.foodDescId(id), TCRSFoodMap);
    }

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    return true;
  }

  private static boolean deriveCafe(final int itemId, String descId, Map<Integer, TCRS> map) {
    // Don't do this if we already know the item
    if (map.containsKey(itemId)) {
      return false;
    }

    String text = DebugDatabase.cafeItemDescriptionText(descId);

    TCRS tcrs = deriveItem(text);
    if (tcrs == null) {
      return false;
    }

    map.put(itemId, tcrs);

    return true;
  }

  public static boolean applyModifiers() {
    // Remove food/booze/spleen/potion sources for effects
    StringBuilder buffer = new StringBuilder();
    for (Integer id : EffectDatabase.keys()) {
      String actions = EffectDatabase.getActions(id);
      if (actions == null || actions.startsWith("#")) {
        continue;
      }
      if (actions.contains("eat ")
          || actions.contains("drink ")
          || actions.contains("chew ")
          || actions.contains("use ")) {
        String[] split = actions.split(" *\\| *");
        buffer.setLength(0);
        for (String action : split) {
          if (action.equals("")
              || action.startsWith("eat ")
              || action.startsWith("drink ")
              || action.startsWith("chew ")
              || action.startsWith("use ")) {
            continue;
          }
          if (buffer.length() > 0) {
            buffer.append("|");
          }
          buffer.append(action);
        }
        EffectDatabase.setActions(id, buffer.length() == 0 ? null : buffer.toString());
      }
    }

    // Adjust non-cafe item data to have TCRS modifiers
    for (Entry<Integer, TCRS> entry : TCRSMap.entrySet()) {
      Integer id = entry.getKey();
      TCRS tcrs = entry.getValue();
      applyModifiers(id, tcrs);
    }

    // Do the same for cafe consumables
    for (Entry<Integer, TCRS> entry : TCRSBoozeMap.entrySet()) {
      Integer id = entry.getKey();
      TCRS tcrs = entry.getValue();
      String name = CafeDatabase.getCafeBoozeName(id.intValue());
      applyConsumableModifiers(KoLConstants.CONSUME_DRINK, name, tcrs);
    }

    for (Entry<Integer, TCRS> entry : TCRSFoodMap.entrySet()) {
      Integer id = entry.getKey();
      TCRS tcrs = entry.getValue();
      String name = CafeDatabase.getCafeFoodName(id.intValue());
      applyConsumableModifiers(KoLConstants.CONSUME_EAT, name, tcrs);
    }

    // Fix all the consumables whose adv yield varies by level
    ConsumablesDatabase.setVariableConsumables();

    ConcoctionDatabase.refreshConcoctions();
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
    return true;
  }

  public static boolean applyModifiers(int itemId) {
    Integer id = IntegerPool.get(itemId);
    return applyModifiers(id, TCRSMap.get(id));
  }

  private static int qualityMultiplier(String quality) {
    return "EPIC".equals(quality)
        ? 5
        : "awesome".equals(quality)
            ? 4
            : "good".equals(quality)
                ? 3
                : "decent".equals(quality) ? 2 : "crappy".equals(quality) ? 1 : 0;
  }

  public static boolean applyModifiers(final Integer itemId, final TCRS tcrs) {
    // Adjust item data to have TCRS modifiers
    if (tcrs == null) {
      return false;
    }

    if (ItemDatabase.isFamiliarEquipment(itemId)) {
      return false;
    }

    if (IntStream.of(CampgroundRequest.campgroundItems).anyMatch(i -> i == itemId)) {
      return false;
    }

    String itemName = ItemDatabase.getItemDataName(itemId);
    if (itemName == null) {
      return false;
    }

    // Set modifiers
    Modifiers.updateItem(itemName, tcrs.modifiers);

    // *** Do this after modifiers are set so can log effect modifiers
    int usage = ItemDatabase.getConsumptionType(itemId);
    if (usage == KoLConstants.CONSUME_EAT
        || usage == KoLConstants.CONSUME_DRINK
        || usage == KoLConstants.CONSUME_SPLEEN) {
      applyConsumableModifiers(usage, itemName, tcrs);
    }

    // Add as effect source, if appropriate
    String effectName = Modifiers.getStringModifier("Item", itemName, "Effect");
    if (effectName != null && !effectName.equals("")) {
      addEffectSource(itemName, usage, effectName);
    }

    // Whether or not there is an effect name, reset the concoction
    setEffectName(itemId, itemName);

    return true;
  }

  public static void setEffectName(final Integer itemId, String name) {
    Concoction c = ConcoctionPool.get(itemId, name);
    if (c != null) {
      c.setEffectName();
    }
  }

  private static void addEffectSource(
      final String itemName, final int usage, final String effectName) {
    int effectId = EffectDatabase.getEffectId(effectName);
    if (effectId == -1) {
      return;
    }
    String verb =
        (usage == KoLConstants.CONSUME_EAT)
            ? "eat "
            : (usage == KoLConstants.CONSUME_DRINK)
                ? "drink "
                : (usage == KoLConstants.CONSUME_SPLEEN) ? "chew " : "use ";
    String actions = EffectDatabase.getActions(effectId);
    boolean added = false;
    StringBuilder buffer = new StringBuilder();
    if (actions != null) {
      String either = verb + "either ";
      String[] split = actions.split(" *\\| *");
      for (String action : split) {
        if (action == "") {
          continue;
        }
        if (buffer.length() > 0) {
          buffer.append("|");
        }
        if (added) {
          buffer.append(action);
          continue;
        }
        if (action.startsWith(either)) {
          buffer.append(action);
          buffer.append(", 1 ");
        } else if (action.startsWith(verb)) {
          buffer.append(StringUtilities.singleStringReplace(action, verb, either));
          buffer.append(", 1 ");
        } else {
          buffer.append(action);
          continue;
        }
        buffer.append(itemName);
        added = true;
      }
    }

    if (!added) {
      if (buffer.length() > 0) {
        buffer.append("|");
      }
      buffer.append(verb);
      buffer.append("1 ");
      buffer.append(itemName);
    }
    EffectDatabase.setActions(effectId, buffer.toString());
  }

  private static void applyConsumableModifiers(
      final int usage, final String itemName, final TCRS tcrs) {
    Integer lint = ConsumablesDatabase.getLevelReqByName(itemName);
    int level = lint == null ? 0 : lint.intValue();
    // Guess
    int adv =
        (usage == KoLConstants.CONSUME_SPLEEN) ? 0 : (tcrs.size * qualityMultiplier(tcrs.quality));
    int mus = 0;
    int mys = 0;
    int mox = 0;

    String comment = "Unspaded";
    String effectName = Modifiers.getStringModifier("Item", itemName, "Effect");
    if (effectName != null && !effectName.equals("")) {
      int duration = (int) Modifiers.getNumericModifier("Item", itemName, "Effect Duration");
      String effectModifiers = Modifiers.getStringModifier("Effect", effectName, "Modifiers");
      String buf = comment + " " + duration + " " + effectName + " (" + effectModifiers + ")";
      comment = buf;
    }

    ConsumablesDatabase.updateConsumableSize(itemName, usage, tcrs.size);
    ConsumablesDatabase.updateConsumable(
        itemName,
        tcrs.size,
        level,
        tcrs.quality,
        String.valueOf(adv),
        String.valueOf(mus),
        String.valueOf(mys),
        String.valueOf(mox),
        comment);
  }

  public static void resetModifiers() {
    // Reset all the data structures that we altered in-place to
    // supper a particular TCRS class/sign to standard KoL values.

    // Nothing to reset if we didn't load TCRS data
    if (currentClassSign.equals("")) {
      return;
    }

    TCRSDatabase.reset();

    Modifiers.resetModifiers();
    EffectDatabase.reset();
    ConsumablesDatabase.reset();

    // Check items that vary per person.  Not all of these are in
    // Standard, but TCRS will be out of standard soon.
    // (Copied from KoLmafia.refreshSessionData)
    InventoryManager.checkNoHat();
    InventoryManager.checkJickSword();
    InventoryManager.checkPantogram();
    InventoryManager.checkLatte();
    InventoryManager.checkSaber();

    deriveApplyItem(ItemPool.RING);

    ConcoctionDatabase.resetEffects();
    ConcoctionDatabase.refreshConcoctions();
    ConsumablesDatabase.setSmoresData();
    ConsumablesDatabase.setAffirmationCookieData();
    ConsumablesDatabase.setVariableConsumables();
    ConsumablesDatabase.calculateAdventureRanges();

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  // *** Primitives for checking presence of local files

  public static boolean localFileExists(
      AscensionClass ascensionClass, String sign, final boolean verbose) {
    boolean retval = false;
    retval |= localFileExists(filename(ascensionClass, sign, ""), verbose);
    return retval;
  }

  public static boolean localCafeFileExists(
      AscensionClass ascensionClass, String sign, final boolean verbose) {
    boolean retval = true;
    retval &= localFileExists(filename(ascensionClass, sign, "_cafe_booze"), verbose);
    retval &= localFileExists(filename(ascensionClass, sign, "_cafe_food"), verbose);
    return retval;
  }

  public static boolean anyLocalFileExists(
      AscensionClass ascensionClass, String sign, final boolean verbose) {
    boolean retval = false;
    retval |= localFileExists(filename(ascensionClass, sign, ""), verbose);
    retval |= localFileExists(filename(ascensionClass, sign, "_cafe_booze"), verbose);
    retval |= localFileExists(filename(ascensionClass, sign, "_cafe_food"), verbose);
    return retval;
  }

  private static boolean localFileExists(String localFilename, final boolean verbose) {
    if (localFilename == null) {
      return false;
    }
    File localFile = new File(KoLConstants.DATA_LOCATION, localFilename);
    return localFileExists(localFile, verbose);
  }

  private static boolean localFileExists(File localFile, final boolean verbose) {
    boolean exists = localFile.exists() && localFile.length() > 0;
    if (verbose) {
      RequestLogger.printLine(
          "Local file "
              + localFile.getName()
              + " "
              + (exists ? "already exists" : "does not exist")
              + ".");
    }
    return exists;
  }

  // *** support for fetching TCRS files from KoLmafia's SVN repository

  // Remote files we have fetched this session
  private static final Set<String> remoteFetched =
      new HashSet<String>(); // remote files fetched this session

  // *** Fetching files from the SVN repository, in two parts, since the
  // non-cafe code was released a week before the cafe code, and some
  // class/signs have only the non-cafe file

  public static boolean fetch(
      final AscensionClass ascensionClass, final String sign, final boolean verbose) {
    boolean retval = fetchRemoteFile(filename(ascensionClass, sign, ""), verbose);
    return retval;
  }

  public static boolean fetchCafe(
      final AscensionClass ascensionClass, final String sign, final boolean verbose) {
    boolean retval = true;
    retval &= fetchRemoteFile(filename(ascensionClass, sign, "_cafe_booze"), verbose);
    retval &= fetchRemoteFile(filename(ascensionClass, sign, "_cafe_food"), verbose);
    return retval;
  }

  // *** If we want to get all three files at once - and count it a
  // success as long as the non-cafe file is present -use these.
  // Not recommended.

  public static boolean fetchRemoteFiles(final boolean verbose) {
    return fetchRemoteFiles(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
  }

  public static boolean fetchRemoteFiles(
      AscensionClass ascensionClass, String sign, final boolean verbose) {
    boolean retval = fetchRemoteFile(filename(ascensionClass, sign, ""), verbose);
    fetchRemoteFile(filename(ascensionClass, sign, "_cafe_booze"), verbose);
    fetchRemoteFile(filename(ascensionClass, sign, "_cafe_food"), verbose);
    return retval;
  }

  // *** Primitives for fetching a file from the SVN repository, overwriting existing file, if any.

  public static boolean fetchRemoteFile(String localFilename, final boolean verbose) {
    String remoteFileName =
        "https://raw.githubusercontent.com/kolmafia/kolmafia/main/data/TCRS/" + localFilename;
    if (remoteFetched.contains(remoteFileName)) {
      if (verbose) {
        RequestLogger.printLine(
            "Already fetched remote version of " + localFilename + " in this session.");
      }
      return true;
    }

    // Because we know we want a remote file the directory and override parameters will be ignored.
    BufferedReader remoteReader = DataUtilities.getReader("", remoteFileName, false);
    File output = new File(KoLConstants.DATA_LOCATION, localFilename);

    try {
      PrintWriter writer = new PrintWriter(new FileWriter(output));
      String aLine;
      while ((aLine = remoteReader.readLine()) != null) {
        // if the remote copy uses a different EOl than
        // the local OS then this will implicitly convert
        writer.println(aLine);
      }
      remoteReader.close();
      writer.close();
      if (verbose) {
        RequestLogger.printLine(
            "Fetched remote version of " + localFilename + " from the repository.");
      }
    } catch (IOException exception) {
      // The reader and writer should be closed but since
      // that can throw an exception...
      RequestLogger.printLine("IO Exception for " + localFilename + ": " + exception.toString());
      return false;
    }

    if (output.length() <= 0) {
      // Do we care if we delete a file that is known to
      // exist and is empty?  No.
      if (verbose) {
        RequestLogger.printLine("File " + localFilename + " is empty. Deleting.");
      }
      output.delete();
      return false;
    }

    remoteFetched.add(remoteFileName);
    return true;
  }

  // *** support for loading up TCRS data appropriate to your current class/sign

  public static boolean loadTCRSData() {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    return loadTCRSData(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), true);
  }

  private static boolean loadTCRSData(
      final AscensionClass ascensionClass, final String sign, final boolean verbose) {
    // If local TCRS data file is not present, fetch from repository
    if (!localFileExists(ascensionClass, sign, verbose)) {
      fetch(ascensionClass, sign, verbose);
    }

    boolean nonCafeLoaded = false;

    // If local TCRS data file is not present, offer to derive it
    if (!localFileExists(ascensionClass, sign, false)) {
      String message =
          "No TCRS data is available for "
              + ascensionClass.getName()
              + "/"
              + sign
              + ". Would you like to derive it? (This will take a long time, but you only have to do it once.)";
      if (InputFieldUtilities.confirm(message) && derive(ascensionClass, sign, verbose)) {
        save(ascensionClass, sign, verbose);
        nonCafeLoaded = true;
      } else {
        nonCafeLoaded = false;
      }

    }
    // Otherwise, load it
    else {
      nonCafeLoaded = load(ascensionClass, sign, verbose);
    }

    // Now do the same thing for cafe data.
    if (!localCafeFileExists(ascensionClass, sign, verbose)) {
      fetchCafe(ascensionClass, sign, verbose);
    }

    boolean cafeLoaded = false;

    // If local TCRS data file is not present, offer to derive it
    if (!localCafeFileExists(ascensionClass, sign, false)) {
      String message =
          "No TCRS cafe data is available for "
              + ascensionClass.getName()
              + "/"
              + sign
              + ". Would you like to derive it? (This will not take long, and you only have to do it once.)";
      if (InputFieldUtilities.confirm(message) && deriveCafe(verbose)) {

        saveCafe(ascensionClass, sign, verbose);
        cafeLoaded = true;
      } else {
        cafeLoaded = false;
      }

    }
    // Otherwise, load it
    else {
      cafeLoaded = loadCafe(ascensionClass, sign, verbose);
    }

    // If we loaded data files, update them.

    if (nonCafeLoaded) {
      if (update(verbose) > 0) {
        save(ascensionClass, sign, verbose);
      }
    }

    if (cafeLoaded) {
      if (updateCafeBooze(verbose) > 0) {
        saveCafeBooze(ascensionClass, sign, verbose);
      }
      if (updateCafeFood(verbose) > 0) {
        saveCafeFood(ascensionClass, sign, verbose);
      }
    }

    if (nonCafeLoaded || cafeLoaded) {
      applyModifiers();
      deriveApplyItem(ItemPool.RING);
    }

    return true;
  }
}
