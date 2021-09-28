package net.sourceforge.kolmafia.moods;

import java.util.HashMap;
import javax.swing.JCheckBox;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.textui.command.NunneryCommand;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MPRestoreItemList {
  private static boolean purchaseBasedSort = false;
  private static final HashMap<String, MPRestoreItem> restoreByName = new HashMap<>();

  public static final MPRestoreItem EXPRESS =
      new MPRestoreItemItem("Platinum Yendorian Express Card", Integer.MAX_VALUE, false);
  public static final MPRestoreItem SOFA =
      new MPRestoreItemAction("sleep on your clan sofa", Integer.MAX_VALUE, false);
  public static final MPRestoreItem CAMPGROUND =
      new MPRestoreItemAction("rest at your campground", Integer.MAX_VALUE, false);
  public static final MPRestoreItem FREEREST =
      new MPRestoreItemAction("free rest", Integer.MAX_VALUE, false);

  public static final MPRestoreItem CHATEAU =
      new MPRestoreItemAction("rest at the chateau", 125, false);
  public static final MPRestoreItem CAMPAWAY =
      new MPRestoreItemAction("rest in your campaway tent", 150, false);
  private static final MPRestoreItem NUNS = new MPRestoreItemAction("visit the nuns", 1000, false);
  private static final MPRestoreItem OSCUS =
      new MPRestoreItemItem("Oscus's neverending soda", 250, false);
  private static final MPRestoreItem QUARK =
      new MPRestoreItemAction("unstable quark + junk item", 100, false);
  public static final MPRestoreItem MYSTERY_JUICE =
      new MPRestoreItemItem("magical mystery juice", Integer.MAX_VALUE, 100, true);
  public static final MPRestoreItem SELTZER =
      new MPRestoreItemItem("Knob Goblin seltzer", 10, 80, true);
  public static final MPRestoreItem DOCS_TONIC =
      new MPRestoreItemItem("Doc Galaktik's Invigorating Tonic", 10, 90, false);
  private static final MPRestoreItem MOTH =
      new MPRestoreItemItem("delicious shimmering moth", 35, false);

  public static final MPRestoreItem[] CONFIGURES =
      new MPRestoreItem[] {
        MPRestoreItemList.EXPRESS,
        MPRestoreItemList.SOFA,
        MPRestoreItemList.CAMPAWAY,
        MPRestoreItemList.CHATEAU,
        MPRestoreItemList.CAMPGROUND,
        MPRestoreItemList.FREEREST,
        MPRestoreItemList.NUNS,
        MPRestoreItemList.OSCUS,
        MPRestoreItemList.QUARK,
        new MPRestoreItemItem("CSA all-purpose soap", 1000, false),
        new MPRestoreItemItem("pixel energy tank", 1000, false),
        new MPRestoreItemItem("high-pressure seltzer bottle", 175, true),
        new MPRestoreItemItem("natural fennel soda", 100, false),
        new MPRestoreItemItem("fancy blue potion", 100, false),
        new MPRestoreItemItem("bottle of Vangoghbitussin", 100, false),
        new MPRestoreItemItem("can of CRIMBCOLA", 90, false),
        new MPRestoreItemItem("Monstar energy beverage", 75, false),
        new MPRestoreItemItem("carbonated soy milk", 75, false),
        new MPRestoreItemItem("carbonated water lily", 65, false),
        new MPRestoreItemItem("Nardz energy beverage", 65, false),
        new MPRestoreItemItem("blue pixel potion", 65, true),
        new MPRestoreItemItem("cotton candy bale", 61, false),
        new MPRestoreItemItem("bottle of Monsieur Bubble", 56, true),
        new MPRestoreItemItem("ancient Magi-Wipes", 55, false),
        new MPRestoreItemItem("unrefined mountain stream syrup", 55, true),
        new MPRestoreItemItem("cotton candy pillow", 51, false),
        new MPRestoreItemItem("blue potion", 50, false),
        new MPRestoreItemItem("phonics down", 48, false),
        new MPRestoreItemItem("elven magi-pack", 45, false),
        new MPRestoreItemItem("generic mana potion", 44, false),
        new MPRestoreItemItem("generic restorative potion", 44, false),
        new MPRestoreItemItem("tonic water", 40, false),
        new MPRestoreItemItem("cotton candy cone", 39, false),
        new MPRestoreItemItem("palm-frond fan", 37, false),
        new MPRestoreItemItem("Okee-Dokee soda", 37, false),
        new MPRestoreItemItem("honey-dipped locust", 36, false),
        new MPRestoreItemItem("Marquis de Poivre soda", 35, false),
        MPRestoreItemList.MOTH,
        new MPRestoreItemItem("green pixel potion", 35, true),
        new MPRestoreItemItem("blue paisley oyster egg", 33, false),
        new MPRestoreItemItem("blue polka-dot oyster egg", 33, false),
        new MPRestoreItemItem("blue striped oyster egg", 33, false),
        new MPRestoreItemItem("cotton candy plug", 28, false),
        new MPRestoreItemItem("Knob Goblin superseltzer", 27, true),
        new MPRestoreItemItem("psychokinetic energy blob", 25, false),
        new MPRestoreItemItem("gold star", 25, false),
        new MPRestoreItemItem("Blatantly Canadian", 23, false),
        new MPRestoreItemItem("cotton candy skoshe", 22, false),
        new MPRestoreItemItem("tiny house", 22, false),
        new MPRestoreItemItem("cotton candy smidgen", 17, false),
        new MPRestoreItemItem("Dyspepsi-Cola", 12, true),
        new MPRestoreItemItem("Cloaca-Cola", 12, true),
        new MPRestoreItemItem("Regular Cloaca Cola", 8, true),
        new MPRestoreItemItem("Diet Cloaca Cola", 8, true),
        new MPRestoreItemItem("cotton candy pinch", 12, false),
        new MPRestoreItemItem("sugar shard", 8, false),
        new MPRestoreItemItem("Mountain Stream soda", 35, true),
        MPRestoreItemList.MYSTERY_JUICE,
        new MPRestoreItemItem("black cherry soda", 10, 80, false),
        MPRestoreItemList.SELTZER,
        MPRestoreItemList.DOCS_TONIC,
        new MPRestoreItemItem("Cherry Cloaca Cola", 8, 80, true),
        new MPRestoreItemItem("soda water", 4, 70, false),
        new MPRestoreItemItem("Notes from the Elfpocalypse, Chapter I", 35, false),
        new MPRestoreItemItem("Notes from the Elfpocalypse, Chapter II", 35, false),
        new MPRestoreItemItem("Notes from the Elfpocalypse, Chapter III", 35, false),
        new MPRestoreItemItem("Notes from the Elfpocalypse, Chapter IV", 35, false),
        new MPRestoreItemItem("Notes from the Elfpocalypse, Chapter V", 35, false),
        new MPRestoreItemItem("Notes from the Elfpocalypse, Chapter VI", 35, false),
        new MPRestoreItemItem("dueling turtle", 15, false),
        new MPRestoreItemItem("unrefined Mountain Stream syrup", 55, true)
      };

  public static final void setPurchaseBasedSort(final boolean purchaseBasedSort) {
    MPRestoreItemList.purchaseBasedSort = purchaseBasedSort;
  }

  public static int getManaRestored(String restoreName) {
    MPRestoreItem restoreItem = MPRestoreItemList.restoreByName.get(restoreName);
    return restoreItem == null ? Integer.MIN_VALUE : restoreItem.manaPerUse;
  }

  public static void updateManaRestored() {
    MPRestoreItemList.CAMPGROUND.manaPerUse = KoLCharacter.getRestingMP();
    MPRestoreItemList.FREEREST.manaPerUse =
        ChateauRequest.chateauRestUsable()
            ? 125
            : (Preferences.getBoolean("restUsingCampAwayTent")
                    && Preferences.getBoolean("getawayCampsiteUnlocked"))
                ? 125
                : KoLCharacter.getRestingMP();
    MPRestoreItemList.SOFA.manaPerUse = KoLCharacter.getLevel() * 5 + 1;
    MPRestoreItemList.MYSTERY_JUICE.manaPerUse = (int) (KoLCharacter.getLevel() * 1.5f + 4.0f);
    MPRestoreItemList.DOCS_TONIC.purchaseCost = QuestDatabase.isQuestFinished(Quest.DOC) ? 60 : 90;
  }

  public static final boolean contains(final AdventureResult item) {
    return restoreByName.containsKey(item.getName());
  }

  public static final JCheckBox[] getCheckboxes() {
    String mpRestoreSetting = Preferences.getString("mpAutoRecoveryItems");
    // Automatically convert changed restorative name
    mpRestoreSetting =
        StringUtilities.singleStringReplace(mpRestoreSetting, "free disco rest", "free rest");
    JCheckBox[] restoreCheckbox = new JCheckBox[MPRestoreItemList.CONFIGURES.length];

    for (int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i) {
      restoreCheckbox[i] = new JCheckBox(MPRestoreItemList.CONFIGURES[i].toString());
      restoreCheckbox[i].setSelected(
          mpRestoreSetting.contains(MPRestoreItemList.CONFIGURES[i].toString().toLowerCase()));
    }

    return restoreCheckbox;
  }

  public static final void updateCheckboxes(final JCheckBox[] restoreCheckbox) {
    String mpRestoreSetting = Preferences.getString("mpAutoRecoveryItems");

    for (int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i) {
      restoreCheckbox[i].setSelected(
          mpRestoreSetting.contains(MPRestoreItemList.CONFIGURES[i].toString().toLowerCase()));
    }
  }

  /**
   * Abstract base class for MP restoration sources.
   *
   * <p>Due to historical reasons, this class is named "MPRestoreItem" even though it encompasses
   * items, skills, and actions.
   */
  public abstract static class MPRestoreItem extends RestoreItem
      implements Comparable<RestoreItem> {
    private int manaPerUse;
    private int purchaseCost;
    private final boolean isCombatUsable;

    public MPRestoreItem(
        final String restoreName, final int manaPerUse, final boolean isCombatUsable) {
      this(restoreName, manaPerUse, 0, isCombatUsable);
    }

    public MPRestoreItem(
        final String restoreName,
        final int manaPerUse,
        final int purchaseCost,
        final boolean isCombatUsable) {
      super(restoreName);
      this.manaPerUse = manaPerUse;
      this.purchaseCost = purchaseCost;
      this.isCombatUsable = isCombatUsable;

      MPRestoreItemList.restoreByName.put(restoreName, this);
    }

    public boolean isCombatUsable() {
      return this.isCombatUsable && this.usableInCurrentPath();
    }

    @Override
    public int compareTo(final RestoreItem o) {
      if (!(o instanceof MPRestoreItem)) {
        return super.compareTo(o);
      }

      MPRestoreItem mpi = (MPRestoreItem) o;

      float restoreAmount = KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP();

      float leftRatio = restoreAmount / this.getManaRestored();
      float rightRatio = restoreAmount / mpi.getManaRestored();

      if (MPRestoreItemList.purchaseBasedSort) {
        if (this.purchaseCost != 0 || mpi.purchaseCost != 0) {
          leftRatio = (float) Math.ceil(leftRatio) * this.purchaseCost;
          rightRatio = (float) Math.ceil(rightRatio) * mpi.purchaseCost;
        }
      }

      float ratioDifference = leftRatio - rightRatio;
      return Float.compare(ratioDifference, 0.0f);
    }

    public long getManaRestored() {
      return Math.min(this.manaPerUse, KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP());
    }

    public boolean usableInCurrentPath() {
      if (this.itemUsed == null) {
        return true;
      }

      int itemId = this.itemUsed.getItemId();

      if (KoLCharacter.inBeecore()) {
        return !ItemDatabase.unusableInBeecore(itemId);
      }

      if (KoLCharacter.inGLover()) {
        return !ItemDatabase.unusableInGLover(itemId);
      }

      return true;
    }

    public void recover(final int needed, final boolean purchase) {
      if (!KoLmafia.permitsContinue()) {
        return;
      }

      if (this == MPRestoreItemList.MOTH
          && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.FORM_OF_BIRD))) {
        return;
      }

      if (this == MPRestoreItemList.EXPRESS) {
        if (Preferences.getBoolean("expressCardUsed")) {
          return;
        }

        AdventureResult EXPRESS_CARD = this.getItem();

        if (KoLConstants.inventory.contains(EXPRESS_CARD)) {
          RequestThread.postRequest(UseItemRequest.getInstance(EXPRESS_CARD));
          return;
        }

        if (!KoLCharacter.canInteract()) {
          return;
        }

        if (!InventoryManager.canUseClanStash(EXPRESS_CARD)) {
          return;
        }

        RequestThread.postRequest(
            new ClanStashRequest(
                new AdventureResult[] {EXPRESS_CARD}, ClanStashRequest.STASH_TO_ITEMS));
        RequestThread.postRequest(UseItemRequest.getInstance(EXPRESS_CARD));
        RequestThread.postRequest(
            new ClanStashRequest(
                new AdventureResult[] {EXPRESS_CARD}, ClanStashRequest.ITEMS_TO_STASH));
        return;
      }

      if (this == MPRestoreItemList.CHATEAU) {
        if (ChateauRequest.chateauRestUsable()) {
          RequestThread.postRequest(new ChateauRequest("chateau_restbox"));
        }
        return;
      }

      if (this == MPRestoreItemList.CAMPAWAY) {
        if (CampAwayRequest.campAwayTentRestUsable()) {
          RequestThread.postRequest(new CampAwayRequest(CampAwayRequest.TENT));
        }
        return;
      }

      if (this == MPRestoreItemList.CAMPGROUND) {
        if (Limitmode.limitCampground() || KoLCharacter.isEd()) {
          return;
        }
        if (!KoLCharacter.inNuclearAutumn()) {
          RequestThread.postRequest(new CampgroundRequest("rest"));
        } else {
          RequestThread.postRequest(new FalloutShelterRequest("vault1"));
        }
        return;
      }

      if (this == MPRestoreItemList.FREEREST) {
        if (Preferences.getInteger("timesRested") < KoLCharacter.freeRestsAvailable()) {
          if (ChateauRequest.chateauRestUsable()) {
            RequestThread.postRequest(new ChateauRequest("chateau_restbox"));
            return;
          }
          if (CampAwayRequest.campAwayTentRestUsable()) {
            RequestThread.postRequest(new CampAwayRequest(CampAwayRequest.TENT));
            return;
          }
          if (!Limitmode.limitCampground()
              && !KoLCharacter.isEd()
              && !KoLCharacter.inNuclearAutumn()) {
            RequestThread.postRequest(new CampgroundRequest("rest"));
            return;
          }
        }
        return;
      }

      if (this == MPRestoreItemList.NUNS) {
        if (Preferences.getInteger("nunsVisits") >= 3) return;
        if (Limitmode.limitZone("IsleWar")) return;
        String side = Preferences.getString("sidequestNunsCompleted");
        if (!side.equals("fratboy")) return; // no MP for hippies!
        if (KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() < 1000) {
          // don't waste this limited resource on small restores
          return;
        }

        NunneryCommand.visit("mp");
        return;
      }

      if (this == MPRestoreItemList.QUARK) {
        if (ItemPool.get(ItemPool.UNSTABLE_QUARK, 1).getCount(KoLConstants.inventory) < 1) {
          return;
        }

        KoLmafiaCLI.DEFAULT_SHELL.executeCommand("quark", "");
        return;
      }

      if (this == MPRestoreItemList.OSCUS) {
        if (Preferences.getBoolean("oscusSodaUsed")) return;
        if (KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() < 250) {
          // don't waste this once-a-day item on small restores
          return;
        }
      }

      if (this == MPRestoreItemList.MYSTERY_JUICE) {
        this.manaPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
      }

      long mpShort = needed - KoLCharacter.getCurrentMP();
      if (mpShort <= 0) {
        return;
      }

      int numberToUse =
          Math.max((int) Math.floor((float) mpShort / (float) this.getManaRestored()), 1);

      if (this == MPRestoreItemList.SOFA) {
        if (!Limitmode.limitClan()) {
          RequestThread.postRequest(
              (new ClanRumpusRequest(RequestType.SOFA)).setTurnCount(numberToUse));
        }
        return;
      }

      if (this.itemUsed == null) {
        // It's neither a skill nor an item
        // We should have handled it above
        return;
      }

      int numberAvailable = this.itemUsed.getCount(KoLConstants.inventory);

      // If you need to purchase, then calculate a better
      // purchasing strategy.

      if (purchase
          && numberAvailable < numberToUse
          && InventoryManager.canUseNPCStores(this.itemUsed)) {
        long numberToBuy = numberToUse;
        int unitPrice = ItemDatabase.getPriceById(this.itemUsed.getItemId()) * 2;

        if (MoodManager.isExecuting()) {
          // For purchases involving between battle checks,
          // buy at least as many as is needed to sustain
          // the entire check.

          mpShort =
              Math.max(mpShort, MoodManager.getMaintenanceCost() - KoLCharacter.getCurrentMP());
          numberToBuy =
              Math.max((int) Math.floor((float) mpShort / (float) this.getManaRestored()), 1);
        }

        numberToBuy = Math.min(KoLCharacter.getAvailableMeat() / unitPrice, numberToBuy);

        // We may need to switch outfits to buy the
        // recovery item, but make sure we are wearing
        // our original outfit before consuming it.

        if (!InventoryManager.checkpointedRetrieveItem(this.itemUsed.getInstance(numberToBuy))) {
          return;
        }

        numberAvailable = this.itemUsed.getCount(KoLConstants.inventory);
      }

      numberToUse = Math.min(numberAvailable, numberToUse);

      // If you don't have any items to use, then return
      // without doing anything.

      if (numberToUse <= 0 || !KoLmafia.permitsContinue()) {
        return;
      }

      RequestThread.postRequest(UseItemRequest.getInstance(this.itemUsed.getInstance(numberToUse)));
    }
  }

  /**
   * MP restoration sources that are usable items. This includes items that are consumed upon use
   * (including spleen items), as well as items that are not consumed upon use (usually limited to N
   * times per day).
   */
  public static class MPRestoreItemItem extends MPRestoreItem {
    public MPRestoreItemItem(String restoreName, int manaPerUse, boolean isCombatUsable) {
      super(restoreName, manaPerUse, isCombatUsable);
    }

    public MPRestoreItemItem(
        String restoreName, int manaPerUse, int purchaseCost, boolean isCombatUsable) {
      super(restoreName, manaPerUse, purchaseCost, isCombatUsable);
    }
  }

  /** Skills that restore MP upon casting. */
  public static class MPRestoreItemSkill extends MPRestoreItem {
    public MPRestoreItemSkill(String restoreName, int manaPerUse, boolean isCombatUsable) {
      super(restoreName, manaPerUse, isCombatUsable);
    }

    public MPRestoreItemSkill(
        String restoreName, int manaPerUse, int purchaseCost, boolean isCombatUsable) {
      super(restoreName, manaPerUse, purchaseCost, isCombatUsable);
    }
  }

  /**
   * MP restoration sources that require the player to do something special. This includes various
   * forms of resting.
   */
  public static class MPRestoreItemAction extends MPRestoreItem {
    public MPRestoreItemAction(String restoreName, int manaPerUse, boolean isCombatUsable) {
      super(restoreName, manaPerUse, isCombatUsable);
    }

    public MPRestoreItemAction(
        String restoreName, int manaPerUse, int purchaseCost, boolean isCombatUsable) {
      super(restoreName, manaPerUse, purchaseCost, isCombatUsable);
    }
  }
}
