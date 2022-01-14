package net.sourceforge.kolmafia.moods;

import java.util.HashMap;
import javax.swing.JCheckBox;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.textui.command.NunneryCommand;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class HPRestoreItemList {
  private static boolean purchaseBasedSort = false;
  private static final HashMap<String, HPRestoreItem> restoreByName = new HashMap<>();

  public static final HPRestoreItem WALRUS = new HPRestoreItemSkill("Tongue of the Walrus", 35);

  private static final HPRestoreItem SOFA =
      new HPRestoreItemAction("sleep on your clan sofa", Integer.MAX_VALUE);
  private static final HPRestoreItem CHATEAU = new HPRestoreItemAction("rest at the chateau", 250);
  private static final HPRestoreItem CAMPAWAY =
      new HPRestoreItemAction("rest in your campaway tent", 300);
  private static final HPRestoreItem CAMPGROUND =
      new HPRestoreItemAction("rest at your campground", 40);
  private static final HPRestoreItem FREEREST = new HPRestoreItemAction("free rest", 40);
  private static final HPRestoreItem DISCONAP = new HPRestoreItemSkill("Disco Nap", 20);

  private static final HPRestoreItem NUNS = new HPRestoreItemAction("visit the nuns", 1000);
  private static final HPRestoreItem HERBS =
      new HPRestoreItemItem("Medicinal Herb's medicinal herbs", Integer.MAX_VALUE, 100);

  public static final HPRestoreItem HOTTUB =
      new HPRestoreItemAction("relaxing hot tub", Integer.MAX_VALUE);
  public static final HPRestoreItem SCROLL =
      new HPRestoreItemItem("scroll of drastic healing", Integer.MAX_VALUE);
  private static final HPRestoreItem MASSAGE_OIL =
      new HPRestoreItemItem("scented massage oil", Integer.MAX_VALUE);
  private static final HPRestoreItem COCOON = new HPRestoreItemSkill("Cannelloni Cocoon", 1000);
  private static final HPRestoreItem SHAKE_IT_OFF =
      new HPRestoreItemSkill("Shake It Off", Integer.MAX_VALUE);
  private static final HPRestoreItem GRUB = new HPRestoreItemItem("plump juicy grub", 95);
  private static final HPRestoreItem DOCS_UNGUENT =
      new HPRestoreItemItem("Doc Galaktik's Pungent Unguent", 4, 30);
  private static final HPRestoreItem DOCS_ELIXIR =
      new HPRestoreItemItem("Doc Galaktik's Homeopathic Elixir", 19, 120);
  private static final HPRestoreItem GELATINOUS_RECONSTRUCTION =
      new HPRestoreItemSkill("Gelatinous Reconstruction", 13);

  // Path of the Plumber items. purchase cost is coins, not Meat
  private static final HPRestoreItem SUPER_DELUXE_MUSHROOM =
      new HPRestoreItemItem("super deluxe mushroom", 100, 20);
  private static final HPRestoreItem DELUXE_MUSHROOM =
      new HPRestoreItemItem("deluxe mushroom", 30, 10);
  private static final HPRestoreItem MUSHROOM = new HPRestoreItemItem("mushroom", 10, 5);
  private static final AdventureResult COIN = ItemPool.get(ItemPool.COIN, 1);

  public static final HPRestoreItem[] CONFIGURES =
      new HPRestoreItem[] {
        HPRestoreItemList.SOFA,
        HPRestoreItemList.CAMPAWAY,
        HPRestoreItemList.CHATEAU,
        HPRestoreItemList.CAMPGROUND,
        HPRestoreItemList.FREEREST,
        HPRestoreItemList.HERBS,
        HPRestoreItemList.SCROLL,
        HPRestoreItemList.MASSAGE_OIL,
        HPRestoreItemList.COCOON,
        HPRestoreItemList.SHAKE_IT_OFF,
        HPRestoreItemList.NUNS,
        HPRestoreItemList.HOTTUB,
        new HPRestoreItemItem("Camp Scout pup tent", 1000),
        new HPRestoreItemItem("pixel energy tank", 1000),
        new HPRestoreItemItem("extra-strength red potion", 200),
        new HPRestoreItemItem("red pixel potion", 110),
        new HPRestoreItemItem("really thick bandage", 109),
        new HPRestoreItemItem("Pok&euml;mann band-aid", 100),
        new HPRestoreItemItem("filthy poultice", 100),
        new HPRestoreItemItem("gauze garter", 100),
        new HPRestoreItemItem("red potion", 100),
        new HPRestoreItemItem("bottle of Vangoghbitussin", 100),
        HPRestoreItemList.GRUB,
        new HPRestoreItemItem("elven medi-pack", 90),
        new HPRestoreItemItem("generic healing potion", 77),
        new HPRestoreItemItem("generic restorative potion", 77),
        new HPRestoreItemItem("cotton candy bale", 61),
        new HPRestoreItemItem("ancient Magi-Wipes", 55),
        new HPRestoreItemItem("cotton candy pillow", 51),
        new HPRestoreItemItem("green pixel potion", 50),
        new HPRestoreItemItem("cartoon heart", 50),
        new HPRestoreItemItem("phonics down", 48),
        new HPRestoreItemItem("cotton candy cone", 39),
        new HPRestoreItemItem("palm-frond fan", 37),
        new HPRestoreItemItem("honey-dipped locust", 36),
        HPRestoreItemList.WALRUS,
        new HPRestoreItemItem("red paisley oyster egg", 33),
        new HPRestoreItemItem("red polka-dot oyster egg", 33),
        new HPRestoreItemItem("red striped oyster egg", 33),
        new HPRestoreItemItem("cotton candy plug", 28),
        new HPRestoreItemItem("tiny house", 22),
        new HPRestoreItemItem("cotton candy skoshe", 22),
        HPRestoreItemList.DISCONAP,
        new HPRestoreItemSkill("Lasagna Bandages", 20),
        HPRestoreItemList.DOCS_ELIXIR,
        new HPRestoreItemItem("cast", 17),
        new HPRestoreItemItem("cotton candy smidgen", 17),
        new HPRestoreItemItem("sugar shard", 15),
        new HPRestoreItemItem("cotton candy pinch", 9),
        new HPRestoreItemItem("forest tears", 7),
        HPRestoreItemList.DOCS_UNGUENT,
        new HPRestoreItemItem("Notes from the Elfpocalypse, Chapter I", 35),
        new HPRestoreItemItem("Notes from the Elfpocalypse, Chapter II", 35),
        new HPRestoreItemItem("Notes from the Elfpocalypse, Chapter III", 35),
        new HPRestoreItemItem("Notes from the Elfpocalypse, Chapter IV", 35),
        new HPRestoreItemItem("Notes from the Elfpocalypse, Chapter V", 35),
        new HPRestoreItemItem("Notes from the Elfpocalypse, Chapter VI", 35),
        new HPRestoreItemItem("dueling turtle", 15),
        HPRestoreItemList.GELATINOUS_RECONSTRUCTION,
      };

  // These are the only items usable as a Plumber - and, since they are
  // quest items which disappear when you free Princess Ralph, they are
  // usable ONLY as a Plumber.
  public static final HPRestoreItem[] PLUMBER_CONFIGURES =
      new HPRestoreItem[] {
        HPRestoreItemList.SUPER_DELUXE_MUSHROOM,
        HPRestoreItemList.DELUXE_MUSHROOM,
        HPRestoreItemList.MUSHROOM,
      };

  public static final void setPurchaseBasedSort(final boolean purchaseBasedSort) {
    HPRestoreItemList.purchaseBasedSort = purchaseBasedSort;
  }

  public static int getHealthRestored(String restoreName) {
    HPRestoreItem restoreItem = HPRestoreItemList.restoreByName.get(restoreName);
    return restoreItem == null ? Integer.MIN_VALUE : restoreItem.healthPerUse;
  }

  public static void updateHealthRestored() {
    HPRestoreItemList.CAMPGROUND.healthPerUse = KoLCharacter.getRestingHP();
    HPRestoreItemList.FREEREST.healthPerUse =
        ChateauRequest.chateauRestUsable()
            ? 250
            : (Preferences.getBoolean("restUsingCampAwayTent")
                    && Preferences.getBoolean("getawayCampsiteUnlocked"))
                ? 250
                : KoLCharacter.getRestingHP();
    HPRestoreItemList.SOFA.healthPerUse = KoLCharacter.getLevel() * 5 + 1;
    HPRestoreItemList.DISCONAP.healthPerUse =
        KoLCharacter.hasSkill("Adventurer of Leisure") ? 40 : 20;
    HPRestoreItemList.DOCS_UNGUENT.purchaseCost =
        QuestDatabase.isQuestFinished(Quest.DOC) ? 20 : 30;
    HPRestoreItemList.DOCS_ELIXIR.purchaseCost =
        QuestDatabase.isQuestFinished(Quest.DOC) ? 80 : 120;
  }

  public static final boolean contains(final AdventureResult item) {
    return getHealthRestored(item.getName()) != Integer.MIN_VALUE;
  }

  public static final JCheckBox[] getCheckboxes() {
    String hpRestoreSetting = Preferences.getString("hpAutoRecoveryItems");
    // Automatically convert changed restorative name
    hpRestoreSetting =
        StringUtilities.singleStringReplace(hpRestoreSetting, "free disco rest", "free rest");
    JCheckBox[] restoreCheckbox = new JCheckBox[HPRestoreItemList.CONFIGURES.length];

    for (int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i) {
      restoreCheckbox[i] = new JCheckBox(HPRestoreItemList.CONFIGURES[i].toString());
      restoreCheckbox[i].setSelected(
          hpRestoreSetting.contains(HPRestoreItemList.CONFIGURES[i].toString().toLowerCase()));
    }

    return restoreCheckbox;
  }

  public static final void updateCheckboxes(final JCheckBox[] restoreCheckbox) {
    String hpRestoreSetting = Preferences.getString("hpAutoRecoveryItems");

    for (int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i) {
      restoreCheckbox[i].setSelected(
          hpRestoreSetting.contains(HPRestoreItemList.CONFIGURES[i].toString().toLowerCase()));
    }
  }

  /**
   * Abstract base class for HP restoration sources.
   *
   * <p>Due to historical reasons, this class is named "HPRestoreItem" even though it encompasses
   * items, skills, and actions.
   */
  public abstract static class HPRestoreItem extends RestoreItem {
    private int healthPerUse;
    private int purchaseCost;

    public HPRestoreItem(final String restoreName, final int healthPerUse) {
      this(restoreName, healthPerUse, 0);
    }

    public HPRestoreItem(final String restoreName, final int healthPerUse, final int purchaseCost) {
      super(restoreName);
      this.healthPerUse = healthPerUse;
      this.purchaseCost = purchaseCost;

      HPRestoreItemList.restoreByName.put(restoreName, this);
    }

    public long getHealthRestored() {
      return Math.min(this.healthPerUse, KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP());
    }

    @Override
    public boolean usableInCurrentPath() {
      if (KoLCharacter.isEd()) {
        return false;
      }

      // Plumbers can heal HP ONLY with plumber items; no
      // skills or "resting" mechanism heal HP
      boolean isPlumber = KoLCharacter.isPlumber();

      if (this.itemUsed == null) {
        return !isPlumber;
      }

      int itemId = this.itemUsed.getItemId();

      // Plumber-only items are all quest items which
      // disappear when you free Princess Ralph.
      if (ItemDatabase.usableOnlyAsPlumber(itemId)) {
        return isPlumber;
      }

      if (KoLCharacter.inBeecore()) {
        return !ItemDatabase.unusableInBeecore(itemId);
      }

      if (KoLCharacter.inGLover()) {
        return !ItemDatabase.unusableInGLover(itemId);
      }

      return true;
    }

    @Override
    public int compareTo(final RestoreItem o) {
      if (!(o instanceof HPRestoreItem)) {
        return super.compareTo(o);
      }

      HPRestoreItem hpi = (HPRestoreItem) o;

      // Health restores are special because skills are preferred
      // over items, so test for that first.

      if (this.itemUsed == null && hpi.itemUsed != null) {
        return -1;
      }

      if (this.itemUsed != null && hpi.itemUsed == null) {
        return 1;
      }

      float restoreAmount = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
      float leftRatio = restoreAmount / this.getHealthRestored();
      float rightRatio = restoreAmount / hpi.getHealthRestored();

      // If you're comparing skills, then you compare MP cost for
      // casting the skill, with more expensive skills coming later.

      if (this.itemUsed == null && this.skillId > 0) {
        leftRatio =
            (float) (Math.ceil(leftRatio) * SkillDatabase.getMPConsumptionById(this.skillId));
        rightRatio =
            (float) (Math.ceil(rightRatio) * SkillDatabase.getMPConsumptionById(hpi.skillId));
      } else if (HPRestoreItemList.purchaseBasedSort) {
        if (this.purchaseCost != 0 || hpi.purchaseCost != 0) {
          leftRatio = (float) Math.ceil(leftRatio) * this.purchaseCost;
          rightRatio = (float) Math.ceil(rightRatio) * hpi.purchaseCost;
        }
      }

      float ratioDifference = leftRatio - rightRatio;
      return Float.compare(ratioDifference, 0.0f);
    }

    @Override
    public void recover(final int needed, final boolean purchase) {
      if (!KoLmafia.permitsContinue()) {
        return;
      }

      // None of these work in Ed
      if (KoLCharacter.isEd()) {
        return;
      }

      if (this == HPRestoreItemList.GRUB
          && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.FORM_OF_BIRD))) {
        return;
      }

      if (this == HPRestoreItemList.CHATEAU) {
        if (ChateauRequest.chateauRestUsable()) {
          RequestThread.postRequest(new ChateauRequest("chateau_restbox"));
        }
        return;
      }

      if (this == HPRestoreItemList.CAMPAWAY) {
        if (CampAwayRequest.campAwayTentRestUsable()) {
          RequestThread.postRequest(new CampAwayRequest(CampAwayRequest.TENT));
        }
        return;
      }

      if (this == HPRestoreItemList.CAMPGROUND) {
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

      if (this == HPRestoreItemList.FREEREST) {
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

      if (this == HPRestoreItemList.NUNS) {
        if (Preferences.getInteger("nunsVisits") >= 3) return;
        String side = Preferences.getString("sidequestNunsCompleted");
        if (!side.equals("fratboy") && !side.equals("hippy")) return;
        if (KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() < 1000) {
          // don't waste this limited resource on small restores
          return;
        }
        if (side.equals("fratboy")
            && (KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() < 1000)) {
          // don't waste the MP restoration, either
          return;
        }

        NunneryCommand.visit("hp");
        return;
      }

      if (this == HPRestoreItemList.HOTTUB) {
        if (Preferences.getInteger("_hotTubSoaks") >= 5) {
          // done for the day
          return;
        }

        if (KoLCharacter.getCurrentHP() > KoLCharacter.getMaximumHP() / 2) {
          // don't waste this limited resource on small restores
          return;
        }

        RequestThread.postRequest(new ClanLoungeRequest(ClanLoungeRequest.HOTTUB));
        return;
      }

      // Can't use items that consume more spleen than we have left

      if (this.spleenHit > 0
          && this.spleenHit > KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse()) {
        return;
      }

      // For all other instances, you will need to calculate
      // the number of times this technique must be used.

      long hpShort = needed - KoLCharacter.getCurrentHP();
      if (hpShort <= 0) {
        return;
      }

      int numberToUse =
          Math.max((int) Math.floor((float) hpShort / (float) this.getHealthRestored()), 1);

      if (this == HPRestoreItemList.SOFA) {
        RequestThread.postRequest(
            (new ClanRumpusRequest(RequestType.SOFA)).setTurnCount(numberToUse));
        return;
      }

      if (SkillDatabase.contains(this.restoreName)) {
        if (!KoLCharacter.hasSkill(this.restoreName)) {
          numberToUse = 0;
        }
      } else if (ItemDatabase.contains(this.restoreName)) {
        // In certain instances, you are able to buy
        // more of the given item from NPC stores, or
        // from the mall.

        int numberAvailable = this.itemUsed.getCount(KoLConstants.inventory);
        int itemId = this.itemUsed.getItemId();

        if (purchase && numberAvailable < numberToUse) {
          long numberToBuy = numberAvailable;

          if (KoLCharacter.isPlumber()) {
            // Healing items are bought with coins
            int unitPrice = this.purchaseCost;
            int coins = COIN.getCount(KoLConstants.inventory);
            int canBuy = coins / unitPrice;
            numberToBuy = Math.min(canBuy, numberToUse - numberAvailable);
          } else if (NPCStoreDatabase.contains(itemId)) {
            // Healing items are bought with Meat
            int unitPrice = ItemDatabase.getPriceById(itemId) * 2;
            long canBuy = KoLCharacter.getAvailableMeat() / unitPrice;
            numberToBuy = Math.min(canBuy, (this == HPRestoreItemList.HERBS) ? 3 : numberToUse);
          }

          // We may need to switch outfits to buy the
          // recovery item, but make sure we are wearing
          // our original outfit before consuming it.

          if (!InventoryManager.checkpointedRetrieveItem(this.itemUsed.getInstance(numberToBuy))) {
            return;
          }

          numberAvailable = this.itemUsed.getCount(KoLConstants.inventory);
        }

        numberToUse = Math.min(numberToUse, numberAvailable);
      } else {
        // It's neither a skill nor an item
        // We should have handled it above
        return;
      }

      // If you don't have any items to use, then return
      // without doing anything.

      if (numberToUse <= 0 || !KoLmafia.permitsContinue()) {
        return;
      }

      if (SkillDatabase.contains(this.restoreName)) {
        RequestThread.postRequest(UseSkillRequest.getInstance(this.restoreName, "", numberToUse));
      } else if (ItemDatabase.contains(this.restoreName)) {
        RequestThread.postRequest(
            UseItemRequest.getInstance(this.itemUsed.getInstance(numberToUse)));
      }
    }
  }

  /**
   * HP restoration sources that are usable items. This includes items that are consumed upon use
   * (including spleen items), as well as items that are not consumed upon use (usually limited to N
   * times per day).
   */
  public static class HPRestoreItemItem extends HPRestoreItemList.HPRestoreItem {
    public HPRestoreItemItem(String restoreName, int healthPerUse) {
      super(restoreName, healthPerUse);
    }

    public HPRestoreItemItem(String restoreName, int healthPerUse, int purchaseCost) {
      super(restoreName, healthPerUse, purchaseCost);
    }
  }

  /** Skills that restore HP upon casting. */
  public static class HPRestoreItemSkill extends HPRestoreItemList.HPRestoreItem {
    public HPRestoreItemSkill(String restoreName, int healthPerUse) {
      super(restoreName, healthPerUse);
    }

    public HPRestoreItemSkill(String restoreName, int healthPerUse, int purchaseCost) {
      super(restoreName, healthPerUse, purchaseCost);
    }
  }

  /**
   * HP restoration sources that require the player to do something special. This includes various
   * forms of resting.
   */
  public static class HPRestoreItemAction extends HPRestoreItemList.HPRestoreItem {
    public HPRestoreItemAction(String restoreName, int healthPerUse) {
      super(restoreName, healthPerUse);
    }

    public HPRestoreItemAction(String restoreName, int healthPerUse, int purchaseCost) {
      super(restoreName, healthPerUse, purchaseCost);
    }
  }
}
