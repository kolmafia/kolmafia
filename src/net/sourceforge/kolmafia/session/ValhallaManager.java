package net.sourceforge.kolmafia.session;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CouncilRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class ValhallaManager {

  private static final AdventureResult[] USABLE =
      new AdventureResult[] {
        ItemPool.get(ItemPool.GATES_SCROLL, 1),
        ItemPool.get(ItemPool.FISHERMANS_SACK, 1),
        ItemPool.get(ItemPool.BONERDAGON_CHEST, 1),
      };

  private static final AdventureResult[] AUTOSELLABLE =
      new AdventureResult[] {
        ItemPool.get(ItemPool.SMALL_LAMINATED_CARD, 1),
        ItemPool.get(ItemPool.LITTLE_LAMINATED_CARD, 1),
        ItemPool.get(ItemPool.NOTBIG_LAMINATED_CARD, 1),
        ItemPool.get(ItemPool.UNLARGE_LAMINATED_CARD, 1),
        ItemPool.get(ItemPool.DWARVISH_DOCUMENT, 1),
        ItemPool.get(ItemPool.DWARVISH_PAPER, 1),
        ItemPool.get(ItemPool.DWARVISH_PARCHMENT, 1),
        ItemPool.get(ItemPool.CULTIST_ROBE, 1),
        ItemPool.get(ItemPool.CREASED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.CRINKLED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.CRUMPLED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.FOLDED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.RAGGED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.RIPPED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.RUMPLED_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.TORN_PAPER_STRIP, 1),
        ItemPool.get(ItemPool.RAVE_VISOR, 1),
        ItemPool.get(ItemPool.BAGGY_RAVE_PANTS, 1),
        ItemPool.get(ItemPool.PACIFIER_NECKLACE, 1),
        ItemPool.get(ItemPool.GLOWSTICK_ON_A_STRING, 1),
        ItemPool.get(ItemPool.CANDY_NECKLACE, 1),
        ItemPool.get(ItemPool.TEDDYBEAR_BACKPACK, 1),
        ItemPool.get(ItemPool.VIAL_OF_RED_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_YELLOW_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_BLUE_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_ORANGE_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_GREEN_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_VIOLET_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_VERMILION_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_AMBER_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_CHARTREUSE_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_TEAL_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_INDIGO_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_PURPLE_SLIME, 1),
        ItemPool.get(ItemPool.VIAL_OF_BROWN_SLIME, 1),
        ItemPool.get(ItemPool.FISH_OIL_SMOKE_BOMB, 1),
        ItemPool.get(ItemPool.VIAL_OF_SQUID_INK, 1),
        ItemPool.get(ItemPool.POTION_OF_FISHY_SPEED, 1),
        ItemPool.get(ItemPool.AUTOPSY_TWEEZERS, 1),
        ItemPool.get(ItemPool.GNOMISH_EAR, 1),
        ItemPool.get(ItemPool.GNOMISH_LUNG, 1),
        ItemPool.get(ItemPool.GNOMISH_ELBOW, 1),
        ItemPool.get(ItemPool.GNOMISH_KNEE, 1),
        ItemPool.get(ItemPool.GNOMISH_FOOT, 1),
      };

  private static final AdventureResult[] FREEPULL =
      new AdventureResult[] {
        ClanLoungeRequest.VIP_KEY,
        ItemPool.get(ItemPool.CURSED_KEG, 1),
        ItemPool.get(ItemPool.CURSED_MICROWAVE, 1),
      };

  public static void preAscension() {
    // Trade in gunpowder.

    if (InventoryManager.hasItem(ItemPool.GUNPOWDER)) {
      BreakfastManager.visitPyro();
    }

    // Use any usable quest items
    for (int i = 0; i < ValhallaManager.USABLE.length; ++i) {
      AdventureResult item = ValhallaManager.USABLE[i];
      int count = item.getCount(KoLConstants.inventory);
      if (count > 0) {
        RequestThread.postRequest(UseItemRequest.getInstance(item.getInstance(count)));
      }
    }

    // Sell autosellable quest items

    List<AdventureResult> items = new ArrayList<>();
    for (int i = 0; i < ValhallaManager.AUTOSELLABLE.length; ++i) {
      AdventureResult item = ValhallaManager.AUTOSELLABLE[i];
      int count = item.getCount(KoLConstants.inventory);
      if (count > 0) {
        items.add(item.getInstance(count));
      }
    }

    if (items.size() > 0) {
      AutoSellRequest request = new AutoSellRequest(items.toArray(new AdventureResult[0]));
      RequestThread.postRequest(request);
    }

    // Harvest your garden
    CampgroundRequest.harvestCrop();

    // Harvest your mushroom plot
    if (MushroomManager.ownsPlot()) {
      MushroomManager.harvestMushrooms();
    }

    // Repackage bear arms
    AdventureResult leftArm = ItemPool.get(ItemPool.LEFT_BEAR_ARM, 1);
    AdventureResult rightArm = ItemPool.get(ItemPool.RIGHT_BEAR_ARM, 1);
    AdventureResult armBox = ItemPool.get(ItemPool.BOX_OF_BEAR_ARM, 1);
    if (KoLConstants.inventory.contains(leftArm)
        && KoLConstants.inventory.contains(rightArm)
        && !KoLConstants.inventory.contains(armBox)) {
      UseItemRequest arm = UseItemRequest.getInstance(leftArm);
      RequestThread.postRequest(arm);
    }

    // As the final action before we enter the gash, run a user supplied script
    // If script aborts, we will not jump.
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(Preferences.getString("preAscensionScript"));
  }

  public static void onAscension() {
    // Save and restore chat literacy, since you are allowed to chat while in Valhalla
    boolean checkedLiteracy = ChatManager.checkedChatLiteracy();
    boolean chatLiterate = ChatManager.getChatLiteracy();

    KoLCharacter.reset(false);

    if (checkedLiteracy) {
      ChatManager.setChatLiteracy(chatLiterate);
    }

    try {
      PreferenceListenerRegistry.deferPreferenceListeners(true);
      Preferences.increment("knownAscensions", 1);
      Preferences.setInteger("lastBreakfast", -1);
      KoLCharacter.setCurrentRun(0);

      KoLmafia.resetCounters();
      ValhallaManager.resetPerAscensionCounters();

      UntinkerRequest.reset();
      KoLCharacter.setGuildStoreOpen(false);
    } finally {
      PreferenceListenerRegistry.deferPreferenceListeners(false);
    }

    KoLmafia.resetSession();
  }

  public static void postAscension() {
    ItemDatabase.reset();

    CharPaneRequest.setInValhalla(false);
    KoLmafia.refreshSession();

    EquipmentManager.updateEquipmentLists();
    ValhallaManager.resetMoonsignCafes();
    ConcoctionDatabase.refreshConcoctions();
    ConsumablesDatabase.setSmoresData();
    ConsumablesDatabase.setAffirmationCookieData();
    ConsumablesDatabase.setVariableConsumables();
    ConsumablesDatabase.calculateAdventureRanges();
    HermitRequest.initialize();

    // Reset certain settings that the player almost certainly will
    // use differently at the beginning of a run vs. at the end.

    MoodManager.setMood("apathetic");
    Preferences.setFloat("hpAutoRecovery", -0.05f);
    Preferences.setFloat("mpAutoRecovery", -0.05f);

    // Note the information in the session log
    // for recording purposes.

    ValhallaManager.logNewAscension();

    // First Rain monster expected on turns 9-11
    if (KoLCharacter.inRaincore()) {
      TurnCounter.startCounting(8, "Rain Monster window begin loc=*", "lparen.gif");
      TurnCounter.startCounting(10, "Rain Monster window end loc=*", "rparen.gif");
    }

    // First West of Loathing monster expected on turns 6-11
    else if (KoLCharacter.isWestOfLoathing()) {
      TurnCounter.startCounting(5, "WoL Monster window begin loc=*", "lparen.gif");
      TurnCounter.startCounting(10, "WoL Monster window end loc=*", "rparen.gif");
    }
    // Starting Source Enlightenment depends on current Source Points
    else if (KoLCharacter.inTheSource()) {
      Preferences.setInteger(
          "sourceEnlightenment", Math.min(Preferences.getInteger("sourcePoints"), 11));
    } else if (KoLCharacter.isKingdomOfExploathing()) {
      // In Kingdom of Exploathing, you need to visit the council before you can visit place.php
      RequestThread.postRequest(new CouncilRequest());
      // This will get you the telegram from Lady Spookyraven
      RequestThread.postRequest(new PlaceRequest("manor1"));
      // And potentially other things. Refresh inventory.
      ApiRequest.updateInventory();
    }

    // User-defined actions:
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(Preferences.getString("postAscensionScript"));

    ValhallaManager.pullFreeItems();

    if (Preferences.getBoolean("autoQuest")) {
      RequestThread.postRequest(
          UseItemRequest.getInstance(ItemPool.get(ItemPool.SPOOKYRAVEN_TELEGRAM, 1)));
    }

    // Standard paths can change what is available, so check clan
    ClanLoungeRequest.visitLounge();
    ClanLoungeRequest.visitLoungeFloor2();

    // Check hotdog stand, speakeasy, and floundry, if present
    if (ClanManager.getClanLounge().contains(ClanManager.HOT_DOG_STAND)) {
      ClanLoungeRequest.visitLounge(ClanLoungeRequest.HOT_DOG_STAND);
    }
    if (ClanManager.getClanLounge().contains(ClanManager.SPEAKEASY)) {
      ClanLoungeRequest.visitLounge(ClanLoungeRequest.SPEAKEASY);
    }
    if (ClanManager.getClanLounge().contains(ClanManager.FLOUNDRY)) {
      ClanLoungeRequest.visitLounge(ClanLoungeRequest.FLOUNDRY);
    }

    // force rebuild of daily deeds panel
    PreferenceListenerRegistry.firePreferenceChanged("dailyDeedsOptions");
  }

  // Pull items that
  private static void pullFreeItems() {
    for (int i = 0; i < ValhallaManager.FREEPULL.length; ++i) {
      AdventureResult item = ValhallaManager.FREEPULL[i];
      if (item.getCount(KoLConstants.inventory) > 0) {
        continue;
      }

      if (item.getCount(KoLConstants.freepulls) > 0) {
        RequestThread.postRequest(new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, item));
      }
    }
  }

  private static void resetMoonsignCafes() {
    // Change available items if they've changed due to ascension.
    if (KoLCharacter.inBadMoon() && KoLConstants.kitchenItems.isEmpty()) {
      HellKitchenRequest.getMenu();
    } else if (!KoLCharacter.inBadMoon() && !KoLConstants.kitchenItems.isEmpty()) {
      HellKitchenRequest.reset();
    }
    if (KoLCharacter.canEat()
        && KoLCharacter.canadiaAvailable()
        && KoLConstants.restaurantItems.isEmpty()) {
      ChezSnooteeRequest.getMenu();
    } else if ((!KoLCharacter.canEat() || !KoLCharacter.canadiaAvailable())
        && !KoLConstants.restaurantItems.isEmpty()) {
      ChezSnooteeRequest.reset();
    }
    if (KoLCharacter.canDrink()
        && KoLCharacter.gnomadsAvailable()
        && KoLConstants.microbreweryItems.isEmpty()) {
      MicroBreweryRequest.getMenu();
    } else if ((!KoLCharacter.canDrink() || !KoLCharacter.gnomadsAvailable())
        && !KoLConstants.microbreweryItems.isEmpty()) {
      MicroBreweryRequest.reset();
    }
  }

  private static void logNewAscension() {
    PrintStream sessionStream = RequestLogger.getSessionStream();

    sessionStream.println();
    sessionStream.println();
    sessionStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
    sessionStream.println("	   Beginning New Ascension	     ");
    sessionStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
    sessionStream.println();

    sessionStream.println("Ascension #" + KoLCharacter.getAscensions() + ":");

    if (KoLCharacter.isHardcore()) {
      sessionStream.print("Hardcore ");
    } else {
      sessionStream.print("Softcore ");
    }

    Path path = KoLCharacter.getPath();
    String pathName = (path == Path.NONE) ? "No-Path " : path.getName() + " ";
    sessionStream.print(pathName);

    sessionStream.println(KoLCharacter.getAscensionClassName());

    sessionStream.println(KoLCharacter.getSign());
    sessionStream.println();
    sessionStream.println();

    RequestLogger.printList(KoLConstants.availableSkills, sessionStream);
    sessionStream.println();

    sessionStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    sessionStream.println();
    sessionStream.println();
  }

  public static void resetPerAscensionCounters() {
    Preferences.resetPerAscension();
    QuestDatabase.resetQuests();
    IslandManager.resetIsland();
    BanishManager.resetAscension();
    BugbearManager.resetStatus();

    TurnCounter.clearCounters();
    AdventureQueueDatabase.resetQueue();
    AdventureSpentDatabase.resetTurns();
  }
}
