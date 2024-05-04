package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionType;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.PreferenceListenerCheckBox;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class UseItemEnqueuePanel extends ItemListManagePanel<Concoction> implements Listener {
  private final JCheckBox[] filters;
  private final JTabbedPane queueTabs;
  private final ConcoctionType type;
  private final boolean hasCreationQueue;

  public UseItemEnqueuePanel(final ConcoctionType type, JTabbedPane queueTabs) {
    super(ConcoctionDatabase.getUsables().get(type), true, true);

    this.type = type;

    // Remove the default borders inherited from ScrollablePanel.
    BorderLayout a = (BorderLayout) this.actualPanel.getLayout();
    a.setVgap(0);
    CardLayout b = (CardLayout) this.actualPanel.getParent().getLayout();
    b.setVgap(0);

    boolean potions = type == ConcoctionType.POTION;

    this.refreshButton.setAction(new RefreshListener());

    this.hasCreationQueue = queueTabs != null;

    if (!this.hasCreationQueue) {
      // Make a dummy tabbed pane, so that we don't have to do null
      // checks in the 8 places where setTitleAt(0, ...) is called.
      queueTabs = new JTabbedPane();
      queueTabs.addTab("dummy", new JLabel());
    }
    this.queueTabs = queueTabs;

    List<ThreadedListener> listeners = new ArrayList<>();

    if (this.hasCreationQueue) {
      listeners.add(new EnqueueListener());
    }

    listeners.add(new ExecuteListener());

    switch (this.type) {
      case FOOD -> {
        listeners.add(new BingeGhostListener());
        listeners.add(new MilkListener());
        listeners.add(new UniversalSeasoningListener());
        listeners.add(new LunchListener());
        listeners.add(new DistendListener());
      }
      case BOOZE -> {
        listeners.add(new BingeHoboListener());
        listeners.add(new OdeListener());
        listeners.add(new PrayerListener());
        listeners.add(new DogHairListener());
      }
      case SPLEEN -> listeners.add(new MojoListener());
    }

    ActionListener[] listenerArray = new ActionListener[listeners.size()];
    listeners.toArray(listenerArray);

    this.setButtons(false, listenerArray);

    JLabel test = new JLabel("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    this.getElementList().setFixedCellHeight((int) (test.getPreferredSize().getHeight() * 2.5f));

    this.getElementList().setVisibleRowCount(6);
    this.getElementList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    this.filters = new JCheckBox[potions ? 4 : 8];

    this.filters[0] = new JCheckBox("no create");
    this.filters[1] = new TurnFreeCheckbox();
    this.filters[2] = new NoSummonCheckbox();

    if (potions) {
      this.filters[3] = new EffectNameCheckbox();
    } else {
      this.filters[3] = new JCheckBox("+mus only");
      this.filters[4] = new JCheckBox("+mys only");
      this.filters[5] = new JCheckBox("+mox only");
      this.filters[6] = new PerUnitCheckBox(type);
      this.filters[7] = new ByRoomCheckbox();
    }

    for (JCheckBox checkbox : this.filters) {
      this.listenToCheckBox(checkbox);
    }

    JPanel filterPanel = new JPanel(new GridLayout());
    JPanel column1 = new JPanel(new BorderLayout());
    JPanel column2 = new JPanel(new BorderLayout());
    JPanel column3 = new JPanel(new BorderLayout());
    JPanel column4 = new JPanel(new BorderLayout());

    column1.add(this.filters[0], BorderLayout.NORTH);
    column2.add(this.filters[1], BorderLayout.NORTH);
    column3.add(this.filters[2], BorderLayout.NORTH);
    if (potions) {
      column4.add(this.filters[3], BorderLayout.NORTH);
    } else {
      column1.add(this.filters[3], BorderLayout.CENTER);
      column2.add(this.filters[4], BorderLayout.CENTER);
      column3.add(this.filters[5], BorderLayout.CENTER);
      column4.add(this.filters[6], BorderLayout.NORTH);
      column4.add(this.filters[7], BorderLayout.CENTER);
    }

    filterPanel.add(column1);
    filterPanel.add(column2);
    filterPanel.add(column3);
    filterPanel.add(column4);

    // Set the height of the filter panel to be just a wee bit taller than two checkboxes need
    filterPanel.setPreferredSize(
        new Dimension(10, (int) (this.filters[0].getPreferredSize().height * 2.1f)));

    this.setEnabled(true);

    this.northPanel.add(filterPanel, BorderLayout.NORTH);
    // Restore the 10px border that we removed from the bottom.
    this.actualPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

    NamedListenerRegistry.registerNamedListener(type.getSignal(), this);
    this.update();
  }

  public void update() {
    this.setEnabled(true);
    this.filterItems();
  }

  @Override
  public void setEnabled(final boolean isEnabled) {
    // Disable all buttons if false, otherwise allow buttons to only be lit
    // when they are valid to stop flashing buttons
    if (!isEnabled) {
      super.setEnabled(false);
      return;
    }

    this.getElementList().setEnabled(true);

    int index = 0;

    // Always enable the "enqueue" button. You may not be able to consume the
    // item, but you may wish to create it
    if (this.hasCreationQueue) {
      this.buttons[index++].setEnabled(true);
    }

    switch (this.type) {
      case FOOD -> {
        // The "consume" button depends on character path
        boolean canEat = KoLCharacter.canEat();
        this.buttons[index++].setEnabled(canEat);

        boolean haveGhost = KoLCharacter.usableFamiliar(FamiliarPool.GHOST) != null;
        this.buttons[index++].setEnabled(haveGhost);

        // The milk listener is just after the ghost listener
        boolean milkUsed = Preferences.getBoolean("_milkOfMagnesiumUsed");
        boolean milkAvailable =
            !milkUsed
                && (InventoryManager.itemAvailable(ItemPool.MILK_OF_MAGNESIUM)
                    || CreateItemRequest.getInstance(
                                ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 1), false)
                            .getQuantityPossible()
                        > 0);

        this.buttons[index++].setEnabled(milkAvailable);

        // The seasoning listener is just after the ghost listener
        boolean seasoningUsable = UseItemRequest.maximumUses(ItemPool.UNIVERSAL_SEASONING) > 0;
        boolean seasoningAvailable =
            seasoningUsable && (InventoryManager.itemAvailable(ItemPool.UNIVERSAL_SEASONING));

        this.buttons[index++].setEnabled(seasoningAvailable);

        // The lunch listener is just after the seasoning listener
        boolean lunchAvailable =
            canEat
                && (KoLCharacter.hasSkill(SkillPool.GLORIOUS_LUNCH)
                    || (Preferences.getBoolean("barrelShrineUnlocked")
                        && !KoLCharacter.isKingdomOfExploathing()
                        && !Preferences.getBoolean("_barrelPrayer")
                        && KoLCharacter.isTurtleTamer()
                        && StandardRequest.isAllowed(
                            RestrictedItemType.ITEMS, "shrine to the Barrel god")));

        this.buttons[index++].setEnabled(lunchAvailable);

        // We gray out the distend button unless we have a
        // pill, and haven't used one today.
        boolean havepill = InventoryManager.getAccessibleCount(ItemPool.DISTENTION_PILL) > 0;
        boolean usedpill = Preferences.getBoolean("_distentionPillUsed");
        boolean canFlush = (havepill && !usedpill);

        this.buttons[index++].setEnabled(canFlush);
      }
      case BOOZE -> {
        boolean canDrink = KoLCharacter.canDrink();
        this.buttons[index++].setEnabled(canDrink);

        boolean haveHobo = KoLCharacter.usableFamiliar(FamiliarPool.HOBO) != null;
        this.buttons[index++].setEnabled(haveHobo);

        // The ode listener is just after the hobo listener
        var hasOde = checkOdeCastable();
        this.buttons[index].setToolTipText(hasOde.reason);
        this.buttons[index].setEnabled(hasOde.enabled);
        index++;

        // The prayer listener is just after the ode listener
        boolean prayerAvailable =
            canDrink
                && (Preferences.getBoolean("barrelShrineUnlocked")
                    && !KoLCharacter.isKingdomOfExploathing()
                    && !Preferences.getBoolean("_barrelPrayer")
                    && KoLCharacter.isAccordionThief()
                    && StandardRequest.isAllowed(
                        RestrictedItemType.ITEMS, "shrine to the Barrel god"));
        this.buttons[index++].setEnabled(prayerAvailable);

        // We gray out the dog hair button unless we have
        // inebriety, have a pill, and haven't used one today.
        boolean havedrunk = KoLCharacter.getInebriety() > 0;
        boolean havepill =
            InventoryManager.getAccessibleCount(ItemPool.SYNTHETIC_DOG_HAIR_PILL) > 0;
        boolean usedpill = Preferences.getBoolean("_syntheticDogHairPillUsed");
        boolean canFlush = havedrunk && (havepill && !usedpill);
        this.buttons[index++].setEnabled(canFlush);
      }
      case SPLEEN -> {
        boolean canChew = KoLCharacter.canChew();
        this.buttons[index++].setEnabled(canChew);

        boolean filterAvailable = InventoryManager.itemAvailable(ItemPool.MOJO_FILTER);
        boolean haveSpleen = KoLCharacter.getSpleenUse() > 0;
        boolean canUseFilter = Preferences.getInteger("currentMojoFilters") < 3;
        boolean canFlush = filterAvailable && haveSpleen && canUseFilter;
        this.buttons[index++].setEnabled(canFlush);
      }
      case POTION -> {
        boolean canUsePotions = KoLCharacter.canUsePotions();
        this.buttons[index++].setEnabled(canUsePotions);
      }
    }
  }

  private record HasOde(boolean enabled, String reason) {}

  private HasOde checkOdeCastable() {
    if (!KoLCharacter.hasSkill(SkillPool.ODE_TO_BOOZE)) {
      return new HasOde(false, "You do not know The Ode to Booze");
    }

    if (KoLCharacter.getSongs() >= KoLCharacter.getMaxSongs()
        && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ODE))) {
      return new HasOde(false, "You can't remember any more songs");
    }

    return new HasOde(true, "");
  }

  @Override
  public AutoFilterTextField<Concoction> getWordFilter() {
    return new ConsumableFilterField();
  }

  @Override
  protected void listenToCheckBox(final JCheckBox box) {
    super.listenToCheckBox(box);
    box.addActionListener(new ReSortListener());
  }

  @Override
  public void actionConfirmed() {}

  @Override
  public void actionCancelled() {}

  private static class ReSortListener extends ThreadedListener {
    @Override
    protected void execute() {
      ConcoctionDatabase.getUsables().sort();
    }
  }

  private class EnqueueListener extends ThreadedListener {
    @Override
    protected void execute() {
      UseItemEnqueuePanel.this.getDesiredItems("Queue");
      ConcoctionDatabase.refreshConcoctions();

      switch (UseItemEnqueuePanel.this.type) {
        case FOOD -> UseItemEnqueuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
        case BOOZE -> UseItemEnqueuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
        case SPLEEN -> UseItemEnqueuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
      }
      ConcoctionDatabase.getUsables().sort();
    }

    @Override
    public String toString() {
      return "enqueue";
    }
  }

  private class ExecuteListener extends ThreadedListener {
    @Override
    protected void execute() {
      ConcoctionType type = UseItemEnqueuePanel.this.type;

      boolean warnFirst =
          (type == ConcoctionType.FOOD && ConcoctionDatabase.getQueuedFullness() != 0)
              || (type == ConcoctionType.BOOZE && ConcoctionDatabase.getQueuedInebriety() != 0)
              || (type == ConcoctionType.SPLEEN && ConcoctionDatabase.getQueuedSpleenHit() != 0);

      if (warnFirst
          && !InputFieldUtilities.confirm(
              "This action will also consume any queued items.  Are you sure you wish to continue?")) {
        return;
      }

      UseItemEnqueuePanel.this.setEnabled(false);

      AdventureResult[] items = UseItemEnqueuePanel.this.getDesiredItems("Consume");

      if (items == null) {
        return;
      }

      switch (type) {
        case FOOD -> {
          ConcoctionDatabase.handleQueue(type, ConsumptionType.EAT);
          UseItemEnqueuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
        }
        case BOOZE -> {
          ConcoctionDatabase.handleQueue(type, ConsumptionType.DRINK);
          UseItemEnqueuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
        }
        case SPLEEN -> {
          ConcoctionDatabase.handleQueue(type, ConsumptionType.SPLEEN);
          UseItemEnqueuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
        }
        case POTION -> ConcoctionDatabase.handleQueue(type, ConsumptionType.USE);
      }
      ConcoctionDatabase.getUsables().sort();
    }

    @Override
    public String toString() {
      return "consume";
    }
  }

  private class BingeGhostListener extends FamiliarFeedListener {
    @Override
    public boolean warnBeforeConsume() {
      return ConcoctionDatabase.getQueuedFullness() != 0;
    }

    @Override
    public void handleQueue() {
      ConcoctionDatabase.handleQueue(ConcoctionType.FOOD, ConsumptionType.GLUTTONOUS_GHOST);
    }

    @Override
    public String getTitle() {
      return ConcoctionDatabase.getQueuedFullness() + " Full Queued";
    }

    @Override
    public String toString() {
      return "feed ghost";
    }
  }

  private class BingeHoboListener extends FamiliarFeedListener {
    @Override
    public boolean warnBeforeConsume() {
      return ConcoctionDatabase.getQueuedInebriety() != 0;
    }

    @Override
    public void handleQueue() {
      ConcoctionDatabase.handleQueue(ConcoctionType.BOOZE, ConsumptionType.SPIRIT_HOBO);
    }

    @Override
    public String getTitle() {
      return ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued";
    }

    @Override
    public String toString() {
      return "feed hobo";
    }
  }

  private abstract class FamiliarFeedListener extends ThreadedListener {
    @Override
    protected void execute() {
      if (this.warnBeforeConsume()
          && !InputFieldUtilities.confirm(
              "This action will also feed any queued items to your familiar. Are you sure you wish to continue?")) {
        return;
      }

      AdventureResult[] items = UseItemEnqueuePanel.this.getDesiredItems("Feed");

      if (items == null) {
        return;
      }

      this.handleQueue();

      UseItemEnqueuePanel.this.queueTabs.setTitleAt(0, this.getTitle());
    }

    public abstract boolean warnBeforeConsume();

    public abstract void handleQueue();

    public abstract String getTitle();

    @Override
    public abstract String toString();
  }

  private static class MilkListener extends ThreadedListener {
    @Override
    protected void execute() {
      RequestThread.postRequest(
          UseItemRequest.getInstance(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 1)));
    }

    @Override
    public String toString() {
      return "use milk";
    }
  }

  private static class UniversalSeasoningListener extends ThreadedListener {
    @Override
    protected void execute() {
      RequestThread.postRequest(
          UseItemRequest.getInstance(ItemPool.get(ItemPool.UNIVERSAL_SEASONING, 1)));
    }

    @Override
    public String toString() {
      return "universal seasoning";
    }
  }

  private static class LunchListener extends ThreadedListener {
    @Override
    protected void execute() {
      if (KoLCharacter.hasSkill(SkillPool.GLORIOUS_LUNCH)) {
        RequestThread.postRequest(UseSkillRequest.getInstance(SkillPool.GLORIOUS_LUNCH, 1));
      } else {
        // Barrel shrine request
        GenericRequest request = new GenericRequest("da.php?barrelshrine=1");
        RequestThread.postRequest(request);
        request.constructURLString("choice.php?whichchoice=1100&option=4");
        RequestThread.postRequest(request);
      }
    }

    @Override
    public String toString() {
      return KoLCharacter.hasSkill(SkillPool.GLORIOUS_LUNCH) ? "glorious lunch" : "barrel prayer";
    }
  }

  private static class OdeListener extends ThreadedListener {
    @Override
    protected void execute() {
      RequestThread.postRequest(UseSkillRequest.getInstance(SkillPool.ODE_TO_BOOZE, 1));
      if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ODE))) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "Failed to cast Ode.");
      }
    }

    @Override
    public String toString() {
      return "cast ode";
    }
  }

  private static class PrayerListener extends ThreadedListener {
    @Override
    protected void execute() {
      // Barrel shrine request
      GenericRequest request = new GenericRequest("da.php?barrelshrine=1");
      RequestThread.postRequest(request);
      request.constructURLString("choice.php?whichchoice=1100&option=4");
      RequestThread.postRequest(request);
    }

    @Override
    public String toString() {
      return "barrel prayer";
    }
  }

  private static class DistendListener extends ThreadedListener {
    @Override
    protected void execute() {
      AdventureResult item = ItemPool.get(ItemPool.DISTENTION_PILL, 1);
      InventoryManager.retrieveItem(item, false);
      RequestThread.postRequest(UseItemRequest.getInstance(item));
    }

    @Override
    public String toString() {
      return "distend";
    }
  }

  private static class DogHairListener extends ThreadedListener {
    @Override
    protected void execute() {
      AdventureResult item = ItemPool.get(ItemPool.SYNTHETIC_DOG_HAIR_PILL, 1);
      InventoryManager.retrieveItem(item, false);
      RequestThread.postRequest(UseItemRequest.getInstance(item));
    }

    @Override
    public String toString() {
      return "dog hair";
    }
  }

  private static class MojoListener extends ThreadedListener {
    @Override
    protected void execute() {
      AdventureResult item = ItemPool.get(ItemPool.MOJO_FILTER, 1);
      InventoryManager.retrieveItem(item, false);
      RequestThread.postRequest(UseItemRequest.getInstance(item));
    }

    @Override
    public String toString() {
      return "flush mojo";
    }
  }

  private class ConsumableFilterField extends FilterItemField {
    public boolean isVisible(final Object element) {
      Concoction creation = (Concoction) element;

      if (creation.getAvailable() == 0) {
        return false;
      }

      AdventureResult item = creation.getItem();

      if (item != null) {
        // Apparently, Cafe items are allowed, whether or not they are in Standard
        if (creation.getPrice() <= 0 && !ItemDatabase.isAllowed(item)) {
          return false;
        }

        // no create
        if (UseItemEnqueuePanel.this.filters != null
            && UseItemEnqueuePanel.this.filters[0].isSelected()
            && item.getCount(KoLConstants.inventory) == 0) {
          return false;
        }
      }

      ConcoctionType type = UseItemEnqueuePanel.this.type;
      String name = creation.getName();

      if (item != null) {
        switch (type) {
          case FOOD -> {
            // Some foods cannot be eaten (regardless of fullness) even
            // if they can be created. Displaying them on a consumption
            // panel is distracting; you can see them on a create panel.
            String property =
                switch (item.getItemId()) {
                  case ItemPool.PIZZA_OF_LEGEND -> "pizzaOfLegendEaten";
                  case ItemPool.DEEP_DISH_OF_LEGEND -> "deepDishOfLegendEaten";
                  case ItemPool.CALZONE_OF_LEGEND -> "calzoneOfLegendEaten";
                  default -> null;
                };
            if (property != null && Preferences.getBoolean(property)) {
              return false;
            }
          }
        }
      }

      if (ConsumablesDatabase.getRawFullness(name) == null
          && ConsumablesDatabase.getRawInebriety(name) == null
          && ConsumablesDatabase.getRawSpleenHit(name) == null) {
        switch (ItemDatabase.getConsumptionType(creation.getItemId())) {
          case FOOD_HELPER:
            if (type != ConcoctionType.FOOD) {
              return false;
            }
            return super.isVisible(element);

          case DRINK_HELPER:
            if (type != ConcoctionType.BOOZE) {
              return false;
            }
            return super.isVisible(element);

          case USE:
            if (type == ConcoctionType.BOOZE) {
              if (creation.getItemId() != ItemPool.ICE_STEIN) {
                return false;
              }
            } else if (type == ConcoctionType.FOOD) {
              if (!ConcoctionDatabase.canQueueFood(creation.getItemId())) {
                return false;
              }
            } else if (type == ConcoctionType.SPLEEN) {
              return false;
            } else {
              return false;
            }
            return super.isVisible(element);

          case USE_MULTIPLE:
            if ((type == ConcoctionType.BOOZE) || (type == ConcoctionType.SPLEEN)) {
              return false;
            }
            if (type == ConcoctionType.FOOD) {
              if (!ConcoctionDatabase.canQueueFood(creation.getItemId())) {
                return false;
              }
            } else {
              return false;
            }
            return super.isVisible(element);

          case POTION:
          case AVATAR_POTION:
            if (type != ConcoctionType.POTION) {
              return false;
            }
            return super.isVisible(element);

          default:
            return false;
        }
      }

      if (creation.hotdog
          && !StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, "Clan hot dog stand")) {
        return false;
      }

      if (creation.speakeasy != null
          && !StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, "Clan speakeasy")) {
        return false;
      }

      if (item != null && ConsumablesDatabase.getAverageAdventures(item.getName()) == 0) {
        switch (item.getItemId()) {
          case ItemPool.BOTTLE_OF_CHATEAU_DE_VINEGAR, ItemPool.GLITCH_ITEM:
            return false;
        }
      }

      if (KoLCharacter.inBeecore()) {
        // If you have a GGG or Spirit Hobo equipped,
        // disable B filtering, since you may want to
        // binge your familiar with B consumables.
        int fam = KoLCharacter.getFamiliar().getId();
        boolean override =
            // You cannot equip a Spirit Hobo in Beecore.
            // ( UseItemEnqueuePanel.this.booze && fam == FamiliarPool.HOBO ) ||
            (type == ConcoctionType.FOOD && fam == FamiliarPool.GHOST);
        if (!override && item != null && KoLCharacter.hasBeeosity(item.getName())) {
          return false;
        }
      }

      if (KoLCharacter.inZombiecore() && type == ConcoctionType.FOOD) {
        // No hotdogs in Zombiecore
        if (creation.hotdog) {
          return false;
        }
        // If you don't have a GGG equipped, show only brains or a steel lasagna
        int fam = KoLCharacter.getFamiliar().getId();
        if (fam != FamiliarPool.GHOST) {
          if (item != null
              && !item.getName().equals("steel lasagna")
              && (ConsumablesDatabase.getNotes(name) == null
                  || !ConsumablesDatabase.getNotes(name).startsWith("Zombie Slayer"))) {
            return false;
          }
        }
      }

      if (KoLCharacter.isJarlsberg()
          && (type == ConcoctionType.FOOD || type == ConcoctionType.BOOZE)) {
        // No VIP items for Jarlsberg
        if (creation.hotdog || creation.speakeasy != null) {
          return false;
        }
        if (creation.getMixingMethod() != CraftingType.JARLS
            && !name.equals("steel margarita")
            && !name.equals("mediocre lager")) {
          return false;
        }
      }

      if (KoLCharacter.inHighschool() && type == ConcoctionType.BOOZE) {
        if (creation.speakeasy != null) {
          return false;
        }
        String notes = ConsumablesDatabase.getNotes(name);
        if (!name.equals("steel margarita") && (notes == null || !notes.startsWith("KOLHS"))) {
          return false;
        }
      }

      if (KoLCharacter.inNuclearAutumn()) {
        if (type == ConcoctionType.FOOD && ConsumablesDatabase.getFullness(name) > 1) {
          return false;
        }
        if (type == ConcoctionType.BOOZE && ConsumablesDatabase.getInebriety(name) > 1) {
          return false;
        }
        if (type == ConcoctionType.SPLEEN && ConsumablesDatabase.getSpleenHit(name) > 1) {
          return false;
        }
      }

      if (KoLCharacter.inBondcore()) {
        if (type == ConcoctionType.FOOD) {
          return false;
        }
        if (type == ConcoctionType.BOOZE
            && !"martini.gif".equals(ItemDatabase.getImage(creation.getItemId()))) {
          return false;
        }
      }

      if (KoLCharacter.inGLover()) {
        // Can't eat/drink items with G's, except from a Restaurant
        if (item != null
            && !KoLCharacter.hasGs(item.getName())
            && !KoLConstants.restaurantItems.contains(creation.getName())
            && !KoLConstants.microbreweryItems.contains(creation.getName())
            && !KoLConstants.cafeItems.contains(creation.getName())) {
          return false;
        }
        // Can't even drink a dusty bottle of great wine in G Lover
        if (name.startsWith("dusty bottle")) {
          return false;
        }
      }

      if (KoLCharacter.isPlumber()) {
        if (type == ConcoctionType.BOOZE) {
          return false;
        }
      }

      // Vampyres, and only Vampyres can eat/drink bag of blood concoctions
      if (KoLCharacter.isVampyre()) {
        if ((type == ConcoctionType.FOOD || type == ConcoctionType.BOOZE)
            && !ConsumablesDatabase.consumableByVampyres(name)) {
          return false;
        }
      } else if (ConsumablesDatabase.consumableOnlyByVampyres(name)) {
        return false;
      }

      if (KoLCharacter.getLimitMode().limitClan()) {
        if (creation.hotdog || creation.speakeasy != null) {
          return false;
        }
      }

      if (creation.fancydog
          && (ConcoctionDatabase.queuedFancyDog || Preferences.getBoolean("_fancyHotDogEaten"))) {
        return false;
      }

      if (creation.speakeasy != null
          && (ConcoctionDatabase.queuedSpeakeasyDrink
                  + Preferences.getInteger("_speakeasyDrinksDrunk")
              >= 3)) {
        return false;
      }

      // turn-free
      if (UseItemEnqueuePanel.this.filters[1].isSelected()) {
        if ((item != null && item.getItemId() > 0)
            && creation.getTurnFreeAvailable() == 0
            && !KoLConstants.restaurantItems.contains(creation.getName())
            && !KoLConstants.microbreweryItems.contains(creation.getName())) {
          return false;
        }
      }
      // no summon
      if (UseItemEnqueuePanel.this.filters[2].isSelected()) {
        if (item != null
            && creation.getMixingMethod() == CraftingType.CLIPART
            && item.getCount(KoLConstants.inventory) == 0) {
          return false;
        }
      }

      // Consumables only:
      if (creation.type != ConcoctionType.POTION) {
        if (UseItemEnqueuePanel.this.filters[3].isSelected()) {
          String range = ConsumablesDatabase.getMuscleRange(name);
          if (range.equals("+0.0") || range.startsWith("-")) {
            return false;
          }
        }

        if (UseItemEnqueuePanel.this.filters[4].isSelected()) {
          String range = ConsumablesDatabase.getMysticalityRange(name);
          if (range.equals("+0.0") || range.startsWith("-")) {
            return false;
          }
        }

        if (UseItemEnqueuePanel.this.filters[5].isSelected()) {
          String range = ConsumablesDatabase.getMoxieRange(name);
          if (range.equals("+0.0") || range.startsWith("-")) {
            return false;
          }
        }
      }

      // Don't display memento items if memento items are protected from destruction
      if (Preferences.getBoolean("mementoListActive") && item != null) {
        if (KoLConstants.mementoList.contains(item)) {
          return false;
        }
      }

      return super.isVisible(element);
    }
  }

  private static class PerUnitCheckBox extends PreferenceListenerCheckBox {
    public PerUnitCheckBox(final ConcoctionType type) {
      super(
          type == ConcoctionType.BOOZE
              ? "per drunk"
              : type == ConcoctionType.FOOD ? "per full" : "per spleen",
          "showGainsPerUnit");

      this.setToolTipText("Sort gains per adventure");
    }

    @Override
    protected void handleClick() {
      ConcoctionDatabase.getUsables().sort();
    }
  }

  private static class ByRoomCheckbox extends PreferenceListenerCheckBox {
    public ByRoomCheckbox() {
      super("by room", "sortByRoom");
      this.setToolTipText("Sort items you have no room for to the bottom");
    }

    @Override
    protected void handleClick() {
      ConcoctionDatabase.getUsables().sort();
    }
  }

  private class EffectNameCheckbox extends PreferenceListenerCheckBox {
    public EffectNameCheckbox() {
      super("by effect", "sortByEffect");
      this.setToolTipText("Sort items by the effect they produce");
    }

    @Override
    protected void handleClick() {
      ConcoctionDatabase.getUsables().sort();
    }
  }

  private static class TurnFreeCheckbox extends PreferenceListenerCheckBox {
    public TurnFreeCheckbox() {
      super("turn-free", "showTurnFreeOnly");

      this.setToolTipText("Only show creations that will not take a turn");
    }

    @Override
    protected void handleClick() {
      ConcoctionDatabase.getUsables().sort();
    }
  }

  private static class NoSummonCheckbox extends PreferenceListenerCheckBox {
    public NoSummonCheckbox() {
      super("no-summon", "showNoSummonOnly");

      this.setToolTipText("Do not show creations that use up summoning charges");
    }

    @Override
    protected void handleClick() {
      ConcoctionDatabase.getUsables().sort();
    }
  }

  private static class RefreshListener extends ThreadedListener {
    @Override
    protected void execute() {
      ConcoctionDatabase.refreshConcoctions();
    }

    @Override
    public String toString() {
      return "refresh";
    }
  }
}
