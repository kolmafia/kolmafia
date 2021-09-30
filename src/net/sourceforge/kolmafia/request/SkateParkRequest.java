package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public class SkateParkRequest extends GenericRequest {
  public static final int LUTZ = 0;
  public static final int COMET = 1;
  public static final int BAND_SHELL = 2;
  public static final int ECLECTIC_EELS = 3;
  public static final int MERRY_GO_ROUND = 4;

  public static final Object[][] BUFF_DATA = {
    {
      "Lutz, the Ice Skate",
      "lutz, the ice skate",
      "state2buff1",
      IntegerPool.get(SkateParkRequest.LUTZ),
      "_skateBuff1",
      "You've already dined with Lutz",
      "ice",
    },
    {
      "Comet, the Roller Skate",
      "comet, the roller skate",
      "state3buff1",
      IntegerPool.get(SkateParkRequest.COMET),
      "_skateBuff2",
      "You should probably leave Comet alone for the rest of the day",
      "roller",
    },
    {
      "the Band Shell",
      "the band shell",
      "state4buff1",
      IntegerPool.get(SkateParkRequest.BAND_SHELL),
      "_skateBuff3",
      "You've had about all of that crap you can stand today",
      "peace",
    },
    {
      "the Eclectic Eels",
      "the eclectic eels",
      "state4buff2",
      IntegerPool.get(SkateParkRequest.ECLECTIC_EELS),
      "_skateBuff4",
      "You should probably leave those guys alone until tomorrow",
      "peace",
    },
    {
      "the Merry-Go-Round",
      "the merry-go-round",
      "state4buff3",
      IntegerPool.get(SkateParkRequest.MERRY_GO_ROUND),
      "_skateBuff5",
      "Wait until tomorrow",
      "peace",
    },
  };

  public static final AdventureResult AERATED_DIVING_HELMET =
      ItemPool.get(ItemPool.AERATED_DIVING_HELMET, 1);
  public static final AdventureResult SCUBA_GEAR = ItemPool.get(ItemPool.SCUBA_GEAR, 1);
  public static final AdventureResult BATHYSPHERE = ItemPool.get(ItemPool.BATHYSPHERE, 1);
  public static final AdventureResult DAS_BOOT = ItemPool.get(ItemPool.DAS_BOOT, 1);
  public static final AdventureResult AMPHIBIOUS_TOPHAT =
      ItemPool.get(ItemPool.AMPHIBIOUS_TOPHAT, 1);
  public static final AdventureResult BUBBLIN_STONE = ItemPool.get(ItemPool.BUBBLIN_STONE, 1);
  public static final AdventureResult OLD_SCUBA_TANK = ItemPool.get(ItemPool.OLD_SCUBA_TANK, 1);
  public static final AdventureResult SCHOLAR_MASK = ItemPool.get(ItemPool.SCHOLAR_MASK, 1);
  public static final AdventureResult GLADIATOR_MASK = ItemPool.get(ItemPool.GLADIATOR_MASK, 1);
  public static final AdventureResult CRAPPY_MASK = ItemPool.get(ItemPool.CRAPPY_MASK, 1);

  private static AdventureResult self = null;
  private static AdventureResult familiar = null;

  public SkateParkRequest() {
    super("sea_skatepark.php");
  }

  public SkateParkRequest(final int buff) {
    this();
    String action = SkateParkRequest.buffToAction(buff);
    if (action != null) {
      this.addFormField("action", action);
    }
  }

  @Override
  public void run() {
    // Equip for underwater adventuring if not
    try (Checkpoint checkpoint = new Checkpoint()) {
      SkateParkRequest.equip();
      super.run();
    }
  }

  private static void update() {
    if (InventoryManager.getAccessibleCount(SkateParkRequest.AERATED_DIVING_HELMET) > 0) {
      SkateParkRequest.self = SkateParkRequest.AERATED_DIVING_HELMET;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.SCHOLAR_MASK) > 0) {
      SkateParkRequest.self = SkateParkRequest.SCHOLAR_MASK;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.GLADIATOR_MASK) > 0) {
      SkateParkRequest.self = SkateParkRequest.GLADIATOR_MASK;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.CRAPPY_MASK) > 0) {
      SkateParkRequest.self = SkateParkRequest.CRAPPY_MASK;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.SCUBA_GEAR) > 0) {
      SkateParkRequest.self = SkateParkRequest.SCUBA_GEAR;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.OLD_SCUBA_TANK) > 0) {
      SkateParkRequest.self = SkateParkRequest.OLD_SCUBA_TANK;
    }

    FamiliarData familiar = KoLCharacter.getFamiliar();

    // For the dancing frog, the amphibious tophat is the best familiar equipment
    if (familiar.getId() == FamiliarPool.DANCING_FROG
        && InventoryManager.getAccessibleCount(SkateParkRequest.AMPHIBIOUS_TOPHAT) > 0) {
      SkateParkRequest.familiar = SkateParkRequest.AMPHIBIOUS_TOPHAT;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.DAS_BOOT) > 0) {
      SkateParkRequest.familiar = SkateParkRequest.DAS_BOOT;
    } else if (InventoryManager.getAccessibleCount(SkateParkRequest.BATHYSPHERE) > 0) {
      SkateParkRequest.familiar = SkateParkRequest.BATHYSPHERE;
    }
  }

  private static void equip() {
    SkateParkRequest.update();
    if (!KoLCharacter.currentBooleanModifier("Adventure Underwater")) {
      EquipmentRequest request = new EquipmentRequest(SkateParkRequest.self);
      RequestThread.postRequest(request);
    }

    if (!KoLCharacter.currentBooleanModifier("Underwater Familiar")) {
      EquipmentRequest request = new EquipmentRequest(SkateParkRequest.familiar);
      RequestThread.postRequest(request);
    }
  }

  private static String dataPlace(final Object[] data) {
    return (data == null) ? null : ((String) data[0]);
  }

  private static String dataCanonicalPlace(final Object[] data) {
    return (data == null) ? null : ((String) data[1]);
  }

  private static String dataAction(final Object[] data) {
    return (data == null) ? null : ((String) data[2]);
  }

  private static int dataBuff(final Object[] data) {
    return (data == null) ? -1 : ((Integer) data[3]).intValue();
  }

  private static String dataSetting(final Object[] data) {
    return (data == null) ? null : ((String) data[4]);
  }

  private static String dataError(final Object[] data) {
    return (data == null) ? null : ((String) data[5]);
  }

  private static String dataState(final Object[] data) {
    return (data == null) ? null : ((String) data[6]);
  }

  private static Object[] placeToData(final String place) {
    Object[] retval = null;
    for (int i = 0; i < BUFF_DATA.length; ++i) {
      Object[] data = BUFF_DATA[i];
      String canonicalPlace = dataCanonicalPlace(data);
      if (canonicalPlace.indexOf(place) == -1) {
        continue;
      }
      if (retval != null) {
        return null;
      }
      retval = data;
    }
    return retval;
  }

  private static Object[] actionToData(final String action) {
    for (int i = 0; i < BUFF_DATA.length; ++i) {
      Object[] data = BUFF_DATA[i];
      if (action.equals(dataAction(data))) {
        return data;
      }
    }
    return null;
  }

  public static Object[] buffToData(final int buff) {
    for (int i = 0; i < BUFF_DATA.length; ++i) {
      Object[] data = BUFF_DATA[i];
      if (buff == dataBuff(data)) {
        return data;
      }
    }
    return null;
  }

  public static int placeToBuff(final String place) {
    return dataBuff(placeToData(place));
  }

  private static String buffToAction(final int buff) {
    return dataAction(buffToData(buff));
  }

  private static String actionToSetting(final String action) {
    return dataSetting(actionToData(action));
  }

  private static String actionToPlace(final String action) {
    return dataPlace(actionToData(action));
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;
    Matcher matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    String action = matcher.find() ? matcher.group(1) : null;
    Object[] data = actionToData(action);

    int index = KoLAdventure.findAdventureFailure(responseText);
    if (index >= 0) {
      String failure = KoLAdventure.adventureFailureMessage(index);
      MafiaState severity = KoLAdventure.adventureFailureSeverity(index);
      KoLmafia.updateDisplay(severity, failure);
      return;
    }

    if (SkateParkRequest.parseResponse(urlString, responseText)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You've already visited " + dataPlace(data) + " today.");
      return;
    }

    // Now that we have (perhaps) visited the Skate Park, check war
    // status. There are no special messages for visiting a place
    // that is not accessible in the current state.

    SkateParkRequest.ensureUpdatedSkatePark();
    String status = Preferences.getString("skateParkStatus");
    if (!status.equals(dataState(data))) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot visit " + dataPlace(data) + ".");
      return;
    }
  }

  public static final boolean parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("sea_skatepark.php")) {
      return false;
    }

    // Deduce the state of war
    String status = null;

    if (responseText.indexOf("ocean/rumble") != -1) {
      status = "war";
    } else if (responseText.indexOf("ocean/ice_territory") != -1) {
      status = "ice";
    } else if (responseText.indexOf("ocean/roller_territory") != -1) {
      status = "roller";
    } else if (responseText.indexOf("ocean/fountain") != -1) {
      status = "peace";
    }

    if (status != null) {
      SkateParkRequest.ensureUpdatedSkatePark();
      Preferences.setString("skateParkStatus", status);
    }

    Matcher matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    String action = matcher.find() ? matcher.group(1) : null;

    if (action == null) {
      return false;
    }

    Object[] data = actionToData(action);
    boolean effect = responseText.indexOf("You acquire an effect") != -1;
    boolean error = responseText.indexOf(dataError(data)) != -1;
    if (effect || error) {
      Preferences.setBoolean(dataSetting(data), true);
    }
    return error;
  }

  private static void ensureUpdatedSkatePark() {
    int lastAscension = Preferences.getInteger("lastSkateParkReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastSkateParkReset", KoLCharacter.getAscensions());
      Preferences.setString("skateParkStatus", "war");
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("sea_skatepark.php")) {
      return false;
    }

    Matcher matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    String action = matcher.find() ? matcher.group(1) : null;

    // We have nothing special to do for simple visits.

    if (action == null) {
      return true;
    }

    String place = SkateParkRequest.actionToPlace(action);

    if (place == null) {
      return false;
    }

    String message = "Visiting " + place;

    RequestLogger.printLine("");
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
