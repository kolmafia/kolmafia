package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.ConcoctionType;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class UseItemDequeuePanel extends ItemListManagePanel<QueuedConcoction> implements Listener {
  private final JTabbedPane queueTabs;
  private final LockableListModel<QueuedConcoction> queue;
  private final ConcoctionType type;

  public UseItemDequeuePanel(ConcoctionType type) {
    super(ConcoctionDatabase.getQueue(type), false, false);
    // Remove the default borders inherited from ScrollablePanel.
    BorderLayout a = (BorderLayout) this.actualPanel.getLayout();
    a.setVgap(0);
    CardLayout b = (CardLayout) this.actualPanel.getParent().getLayout();
    b.setVgap(0);

    // Add a 10px top border.
    this.northPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);

    this.type = type;

    this.queueTabs = KoLmafiaGUI.getTabbedPane();

    switch (type) {
      case FOOD:
        this.queueTabs.addTab("0 Full Queued", this.centerPanel);
        this.queue = ConcoctionDatabase.queuedFood.getMirrorImage();
        break;
      case BOOZE:
        this.queueTabs.addTab("0 Drunk Queued", this.centerPanel);
        this.queue = ConcoctionDatabase.queuedBooze.getMirrorImage();
        break;
      case SPLEEN:
        this.queueTabs.addTab("0 Spleen Queued", this.centerPanel);
        this.queue = ConcoctionDatabase.queuedSpleen.getMirrorImage();
        break;
      case POTION:
        this.queueTabs.addTab("Potions Queued", this.centerPanel);
        this.queue = ConcoctionDatabase.queuedPotions.getMirrorImage();
        break;
      default:
        this.queue = null;
        break;
    }

    this.queueTabs.addTab(
        "Resources Used", new GenericScrollPane(ConcoctionDatabase.getQueuedIngredients(type), 7));

    JLabel test = new JLabel("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    this.getElementList().setCellRenderer(ListCellRendererFactory.getCreationQueueRenderer());
    this.getElementList().setFixedCellHeight((int) (test.getPreferredSize().getHeight() * 2.5f));

    this.getElementList().setVisibleRowCount(3);
    this.getElementList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    this.actualPanel.add(this.queueTabs, BorderLayout.CENTER);

    this.setButtons(false, new ActionListener[] {new ConsumeListener(), new CreateListener()});

    this.eastPanel.add(new ThreadedButton("undo", new UndoQueueRunnable()), BorderLayout.SOUTH);

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

    // The "consume" listener is the first button
    // The "create" listener is the second button

    int index = 0;

    switch (this.type) {
      case FOOD:
        boolean canEat = KoLCharacter.canEat();
        this.buttons[index++].setEnabled(canEat);
        break;
      case BOOZE:
        boolean canDrink = KoLCharacter.canDrink();
        this.buttons[index++].setEnabled(canDrink);
        break;
      case SPLEEN:
        boolean canSpleen = KoLCharacter.canSpleen();
        this.buttons[index++].setEnabled(canSpleen);
        break;
      case POTION:
        // Potions.
        boolean canUsePotions = KoLCharacter.canUsePotions();
        this.buttons[index++].setEnabled(canUsePotions);
        break;
    }

    // You may not be able to consume the item, but you may wish to create it
    this.buttons[index++].setEnabled(true);
  }

  @Override
  public void filterItems() {
    this.queue.updateFilter(true);
  }

  public JTabbedPane getQueueTabs() {
    return this.queueTabs;
  }

  private class ConsumeListener extends ThreadedListener {
    @Override
    protected void execute() {
      switch (UseItemDequeuePanel.this.type) {
        case FOOD:
          ConcoctionDatabase.handleQueue(type, KoLConstants.CONSUME_EAT);
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
          break;
        case BOOZE:
          ConcoctionDatabase.handleQueue(type, KoLConstants.CONSUME_DRINK);
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
          break;
        case SPLEEN:
          ConcoctionDatabase.handleQueue(type, KoLConstants.CONSUME_SPLEEN);
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
          break;
        case POTION:
          ConcoctionDatabase.handleQueue(type, KoLConstants.CONSUME_USE);
          break;
      }
      ConcoctionDatabase.getUsables().sort();
    }

    @Override
    public String toString() {
      return "consume";
    }
  }

  private class CreateListener extends ThreadedListener {
    @Override
    protected void execute() {
      switch (UseItemDequeuePanel.this.type) {
        case FOOD:
          ConcoctionDatabase.handleQueue(type, KoLConstants.NO_CONSUME);
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
          break;
        case BOOZE:
          ConcoctionDatabase.handleQueue(type, KoLConstants.NO_CONSUME);
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
          break;
        case POTION:
          ConcoctionDatabase.handleQueue(type, KoLConstants.NO_CONSUME);
          break;
      }
      ConcoctionDatabase.getUsables().sort();
    }

    @Override
    public String toString() {
      return "create";
    }
  }

  private class UndoQueueRunnable implements Runnable {
    @Override
    public void run() {
      ConcoctionType type = UseItemDequeuePanel.this.type;
      ConcoctionDatabase.pop(type);
      ConcoctionDatabase.refreshConcoctions();

      switch (type) {
        case FOOD:
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
          break;
        case BOOZE:
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
          break;
        case SPLEEN:
          UseItemDequeuePanel.this.queueTabs.setTitleAt(
              0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
          break;
      }
      ConcoctionDatabase.getUsables().sort();
    }
  }
}
