package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CafeDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.MallSearchFrame;
import net.sourceforge.kolmafia.swingui.ProfileFrame;
import net.sourceforge.kolmafia.swingui.listener.PopupListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class ShowDescriptionList<E> extends JList<E> {
  public int lastSelectIndex;
  public JPopupMenu contextMenu;
  public ListElementFilter filter;

  private final LockableListModel<E> displayModel, originalModel;
  private static final Pattern PLAYERID_MATCHER = Pattern.compile("\\(#(\\d+)\\)");

  public ShowDescriptionList(final LockableListModel<E> displayModel) {
    this(displayModel, null, 4);
  }

  public ShowDescriptionList(final LockableListModel<E> displayModel, final int visibleRowCount) {
    this(displayModel, null, visibleRowCount);
  }

  public ShowDescriptionList(
      final LockableListModel<E> displayModel, final ListElementFilter filter) {
    this(displayModel, filter, 4);
  }

  public ShowDescriptionList(
      final LockableListModel<E> displayModel,
      final ListElementFilter filter,
      final int visibleRowCount) {
    this.contextMenu = new JPopupMenu();

    boolean isMoodList = displayModel == MoodManager.getTriggers();
    boolean isEncyclopedia = !displayModel.isEmpty() && displayModel.get(0) instanceof Entry;

    if (!isMoodList) {
      if (displayModel.isEmpty() || !isEncyclopedia) {
        this.contextMenu.add(new ContextMenuItem("Game description", new DescriptionRunnable()));
      }

      this.contextMenu.add(new ContextMenuItem("Wiki description", new WikiLookupRunnable()));
    }

    if (displayModel == MallSearchFrame.results) {
      this.contextMenu.add(new JSeparator());
      this.contextMenu.add(new ContextMenuItem("Go To Store...", new StoreLookupRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Toggle Forbidden Store", new ForbidStoreRunnable()));
    }

    if (displayModel == KoLConstants.activeEffects) {
      this.contextMenu.add(new ContextMenuItem("Remove this effect", new ShrugOffRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Add to current mood", new AddToMoodEffectRunnable()));
      this.contextMenu.add(new ContextMenuItem("Extend this effect", new ExtendEffectRunnable()));
    }

    if (displayModel == KoLConstants.usableSkills || displayModel == KoLConstants.availableSkills) {
      this.contextMenu.add(new ContextMenuItem("Cast the skill once", new CastSkillRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Add to current mood", new AddToMoodSkillRunnable()));
    }

    if (displayModel == KoLConstants.tally) {
      this.contextMenu.add(new ContextMenuItem("Zero out entries", new ZeroTallyRunnable()));
      this.contextMenu.add(new JSeparator());

      this.contextMenu.add(new ContextMenuItem("Add to junk list", new AddToJunkListRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Add to singleton list", new AddToSingletonListRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Add to memento list", new AddToMementoListRunnable()));

      this.contextMenu.add(new JSeparator());

      this.contextMenu.add(new ContextMenuItem("Autosell selected", new AutoSellRunnable()));
      this.contextMenu.add(new ContextMenuItem("Add selected to mall", new AutoMallRunnable()));
      this.contextMenu.add(new ContextMenuItem("Consume selected", new ConsumeRunnable()));
      this.contextMenu.add(new ContextMenuItem("Pulverize selected", new PulverizeRunnable()));
    } else if (displayModel == KoLConstants.inventory
        || displayModel == KoLConstants.closet
        || isEncyclopedia) {
      this.contextMenu.add(new ContextMenuItem("Add to junk list", new AddToJunkListRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Add to singleton list", new AddToSingletonListRunnable()));
      this.contextMenu.add(
          new ContextMenuItem("Add to memento list", new AddToMementoListRunnable()));
    } else if (isMoodList) {
      this.contextMenu.add(new ContextMenuItem("Force execution", new ForceExecuteRunnable()));
      this.contextMenu.add(new ContextMenuItem("Remove selected", new RemoveTriggerRunnable()));

      this.addKeyListener(new RemoveTriggerListener());
    }

    this.addMouseListener(new PopupListener(this.contextMenu));

    this.originalModel = displayModel;
    this.displayModel =
        filter == null ? displayModel.getMirrorImage() : displayModel.getMirrorImage(filter);
    this.setModel(this.displayModel);

    this.setVisibleRowCount(visibleRowCount);
    this.setCellRenderer(ListCellRendererFactory.getDefaultRenderer());
    ((ShowDescriptionList) this).setPrototypeCellValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
  }

  public LockableListModel<E> getOriginalModel() {
    return this.originalModel;
  }

  public AdventureResult[] getSelectedItems() {
    // Obviously, this only works if the model contains AdventureResults
    List<E> values = this.getSelectedValuesList();
    AdventureResult[] result = new AdventureResult[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      result[i] = (AdventureResult) values.get(i);
    }
    return result;
  }

  public PurchaseRequest[] getSelectedPurchases() {
    // Obviously, this only works if the model contains PurchaseRequests
    List<E> values = this.getSelectedValuesList();
    PurchaseRequest[] result = new PurchaseRequest[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      result[i] = (PurchaseRequest) values.get(i);
    }
    return result;
  }

  public static final void showGameDescription(Object item) {
    if (item instanceof Boost) {
      item = ((Boost) item).getItem();
    }

    if (item instanceof AdventureResult) {
      AdventureResult ar = (AdventureResult) item;
      if (ar.isItem()) {
        int itemId = ar.getItemId();
        String descId =
            (itemId != -1)
                ? ItemDatabase.getDescriptionId(itemId)
                : CafeDatabase.nameToDescId(ar.getName());
        StaticEntity.openDescriptionFrame("desc_item.php?whichitem=" + descId);
      }
      if (ar.isStatusEffect()) {
        String descId = EffectDatabase.getDescriptionId(EffectDatabase.getEffectId(ar.getName()));
        StaticEntity.openDescriptionFrame("desc_effect.php?whicheffect=" + descId);
      }
    } else if (item instanceof Concoction) {
      Concoction c = (Concoction) item;
      int itemId = c.getItemId();
      String descId =
          (itemId != -1)
              ? ItemDatabase.getDescriptionId(itemId)
              : CafeDatabase.nameToDescId(c.getName());
      StaticEntity.openDescriptionFrame("desc_item.php?whichitem=" + descId);
    } else if (item instanceof QueuedConcoction) {
      QueuedConcoction c = (QueuedConcoction) item;
      int itemId = c.getItemId();
      String descId =
          (itemId != -1)
              ? ItemDatabase.getDescriptionId(itemId)
              : CafeDatabase.nameToDescId(c.getName());
      StaticEntity.openDescriptionFrame("desc_item.php?whichitem=" + descId);
    } else if (item instanceof CreateItemRequest) {
      StaticEntity.openDescriptionFrame(
          "desc_item.php?whichitem="
              + ItemDatabase.getDescriptionId(((CreateItemRequest) item).getItemId()));
    } else if (item instanceof PurchaseRequest) {
      StaticEntity.openDescriptionFrame(
          "desc_item.php?whichitem="
              + ItemDatabase.getDescriptionId(((PurchaseRequest) item).getItemId()));
    } else if (item instanceof UseSkillRequest) {
      StaticEntity.openDescriptionFrame(
          "desc_skill.php?whichskill=" + ((UseSkillRequest) item).getSkillId() + "&self=true");
    } else if (item instanceof String) {
      Matcher playerMatcher = ShowDescriptionList.PLAYERID_MATCHER.matcher((String) item);
      if (playerMatcher.find()) {
        Object[] parameters = new Object[] {"#" + playerMatcher.group(1)};
        SwingUtilities.invokeLater(new CreateFrameRunnable(ProfileFrame.class, parameters));
      }
    }
  }

  public static void showMallStore(Object item) {
    if (item instanceof PurchaseRequest) {
      RelayLoader.openSystemBrowser(
          "mallstore.php?whichstore=" + ((PurchaseRequest) item).getFormField("whichstore"));
    }
  }

  private class ContextMenuItem extends ThreadedMenuItem {
    public ContextMenuItem(final String title, final ThreadedListener action) {
      super(title, action);
    }
  }

  private abstract class ContextMenuListener extends ThreadedListener {
    public int index;
    public E item;

    @Override
    protected void execute() {
      this.index =
          ShowDescriptionList.this.lastSelectIndex == -1
              ? ShowDescriptionList.this.getSelectedIndex()
              : ShowDescriptionList.this.lastSelectIndex;

      this.item = ShowDescriptionList.this.displayModel.getElementAt(this.index);

      if (this.item == null) {
        return;
      }

      ShowDescriptionList.this.ensureIndexIsVisible(this.index);

      this.executeAction();
    }

    protected abstract void executeAction();
  }

  /** Utility class which shows the description of the item which is currently selected. */
  private class DescriptionRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      ShowDescriptionList.showGameDescription(this.item);
    }
  }

  /**
   * Utility class which shows the description of the item which is currently selected, as it
   * appears on the wiki.
   */
  private class WikiLookupRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      WikiUtilities.showWikiDescription(this.item);
    }
  }

  public class StoreLookupRunnable extends ContextMenuListener {
    @Override
    protected void executeAction() {
      ShowDescriptionList.showMallStore(this.item);
    }
  }

  public class ForbidStoreRunnable extends ContextMenuListener {
    @Override
    protected void executeAction() {
      if (!(this.item instanceof PurchaseRequest)) {
        return;
      }

      try {
        int storeId = Integer.parseInt(((PurchaseRequest) this.item).getFormField("whichstore"));
        MallPurchaseRequest.toggleForbiddenStore(storeId);
      } catch (NumberFormatException e) {
      }
    }
  }

  public void removeTriggers() {
    List<E> items = ShowDescriptionList.this.getSelectedValuesList();
    List<MoodTrigger> triggers = new ArrayList<>(items.size());

    for (final E item : items) {
      triggers.add((MoodTrigger) item);
    }

    ShowDescriptionList.this.clearSelection();

    MoodManager.removeTriggers(triggers);
    MoodManager.saveSettings();
  }

  private class ForceExecuteRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      for (final E item : ShowDescriptionList.this.getSelectedValuesList()) {
        KoLmafiaCLI.DEFAULT_SHELL.executeLine(((MoodTrigger) item).getAction());
      }

      ShowDescriptionList.this.clearSelection();
    }
  }

  private class RemoveTriggerRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      ShowDescriptionList.this.removeTriggers();
    }
  }

  private class RemoveTriggerListener extends KeyAdapter {
    @Override
    public void keyReleased(final KeyEvent e) {
      if (e.isConsumed()) {
        return;
      }

      if (e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
        return;
      }

      ShowDescriptionList.this.removeTriggers();
      e.consume();
    }
  }

  private class CastSkillRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      UseSkillRequest request;

      for (final E skill : ShowDescriptionList.this.getSelectedValuesList()) {
        request = (UseSkillRequest) skill;

        request.setTarget(null);
        request.setBuffCount(1);

        RequestThread.postRequest(request);
      }

      ShowDescriptionList.this.clearSelection();
    }
  }

  private class AddToMoodSkillRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      if (Preferences.getString("currentMood").equals("apathetic")) {
        Preferences.setString("currentMood", "default");
      }

      String name, action;

      for (final E skill : ShowDescriptionList.this.getSelectedValuesList()) {
        name = UneffectRequest.skillToEffect(((UseSkillRequest) skill).getSkillName());

        action = MoodManager.getDefaultAction("lose_effect", name);
        if (!action.equals("")) {
          MoodManager.addTrigger("lose_effect", name, action);
        }
      }

      ShowDescriptionList.this.clearSelection();
      MoodManager.saveSettings();
    }
  }

  private class AddToMoodEffectRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      if (Preferences.getString("currentMood").equals("apathetic")) {
        Preferences.setString("currentMood", "default");
      }

      String name, action;

      for (final E effect : ShowDescriptionList.this.getSelectedValuesList()) {
        name = ((AdventureResult) effect).getName();

        action = MoodManager.getDefaultAction("lose_effect", name);
        if (!action.equals("")) {
          MoodManager.addTrigger("lose_effect", name, action);
          continue;
        }

        action = MoodManager.getDefaultAction("gain_effect", name);
        if (!action.equals("")) {
          MoodManager.addTrigger("gain_effect", name, action);
        }
      }

      ShowDescriptionList.this.clearSelection();
      MoodManager.saveSettings();
    }
  }

  private class ExtendEffectRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      String name, action;

      for (final E effect : ShowDescriptionList.this.getSelectedValuesList()) {
        name = ((AdventureResult) effect).getName();

        action = MoodManager.getDefaultAction("lose_effect", name);
        if (!action.equals("")) {
          CommandDisplayFrame.executeCommand(action);
        }
      }

      ShowDescriptionList.this.clearSelection();
    }
  }

  private class ShrugOffRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      for (final E effect : ShowDescriptionList.this.getSelectedValuesList()) {
        RequestThread.postRequest(new UneffectRequest((AdventureResult) effect));
      }
    }
  }

  private class AddToJunkListRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      AdventureResult data;

      for (final E item : ShowDescriptionList.this.getSelectedValuesList()) {
        data = null;

        if (item instanceof CreateItemRequest) {
          data = ((CreateItemRequest) item).createdItem;
        } else if (item instanceof AdventureResult && ((AdventureResult) item).isItem()) {
          data = (AdventureResult) item;
        } else if (item instanceof String && ItemDatabase.contains((String) item)) {
          int itemId = ItemDatabase.getItemId((String) item);
          data = ItemPool.get(itemId);
        } else if (item instanceof Entry
            && ItemDatabase.contains((String) ((Entry) item).getValue())) {
          int itemId = ItemDatabase.getItemId((String) ((Entry) item).getValue());
          data = ItemPool.get(itemId);
        }

        if (data == null) {
          continue;
        }

        if (!KoLConstants.junkList.contains(data)) {
          KoLConstants.junkList.add(data);
        }
      }

      ShowDescriptionList.this.clearSelection();
    }
  }

  private class AddToSingletonListRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      AdventureResult data;

      for (final E item : ShowDescriptionList.this.getSelectedValuesList()) {
        data = null;

        if (item instanceof CreateItemRequest) {
          data = ((CreateItemRequest) item).createdItem;
        } else if (item instanceof AdventureResult && ((AdventureResult) item).isItem()) {
          data = (AdventureResult) item;
        } else if (item instanceof String && ItemDatabase.contains((String) item)) {
          int itemId = ItemDatabase.getItemId((String) item);
          data = ItemPool.get(itemId);
        } else if (item instanceof Entry
            && ItemDatabase.contains((String) ((Entry) item).getValue())) {
          int itemId = ItemDatabase.getItemId((String) ((Entry) item).getValue());
          data = ItemPool.get(itemId);
        }

        if (data == null) {
          continue;
        }

        if (!KoLConstants.junkList.contains(data)) {
          KoLConstants.junkList.add(data);
        }
        if (!KoLConstants.singletonList.contains(data)) {
          KoLConstants.singletonList.add(data);
        }
      }

      ShowDescriptionList.this.clearSelection();
    }
  }

  private class AddToMementoListRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      AdventureResult data;

      for (final E item : ShowDescriptionList.this.getSelectedValuesList()) {
        data = null;

        if (item instanceof CreateItemRequest) {
          data = ((CreateItemRequest) item).createdItem;
        } else if (item instanceof AdventureResult && ((AdventureResult) item).isItem()) {
          data = (AdventureResult) item;
        } else if (item instanceof String && ItemDatabase.contains((String) item)) {
          int itemId = ItemDatabase.getItemId((String) item);
          data = ItemPool.get(itemId);
        } else if (item instanceof Entry
            && ItemDatabase.contains((String) ((Entry) item).getValue())) {
          int itemId = ItemDatabase.getItemId((String) ((Entry) item).getValue());
          data = ItemPool.get(itemId);
        }

        if (data != null && !KoLConstants.mementoList.contains(data)) {
          KoLConstants.mementoList.add(data);
        }
      }

      ShowDescriptionList.this.clearSelection();
      Preferences.setBoolean("mementoListActive", true);
    }
  }

  private class ZeroTallyRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
      for (int i = 0; i < items.length; ++i) {
        AdventureResult.addResultToList(KoLConstants.tally, items[i].getNegation());
      }
    }
  }

  private class AutoSellRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      if (!InputFieldUtilities.confirm("Are you sure you would like to sell the selected items?")) {
        return;
      }

      AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
      RequestThread.postRequest(new AutoSellRequest(items));
    }
  }

  private class AutoMallRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      if (!InputFieldUtilities.confirm(
          "Are you sure you would like to add the selected items to your store?")) {
        return;
      }

      AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
      RequestThread.postRequest(new AutoMallRequest(items));
    }
  }

  private class ConsumeRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      if (!InputFieldUtilities.confirm("Are you sure you want to use the selected items?")) {
        return;
      }

      AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
      for (int i = 0; i < items.length; ++i) {
        RequestThread.postRequest(UseItemRequest.getInstance(items[i]));
      }
    }
  }

  private class PulverizeRunnable extends ContextMenuListener {
    @Override
    public void executeAction() {
      if (!InputFieldUtilities.confirm(
          "The items you've selected will be smashed to pieces.  Are you sure?")) {
        return;
      }

      AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
      for (int i = 0; i < items.length; ++i) {
        RequestThread.postRequest(new PulverizeRequest(items[i]));
      }
    }
  }
}
