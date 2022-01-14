package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class UseItemDequeuePanel extends ItemListManagePanel<QueuedConcoction> {
  private final JTabbedPane queueTabs;
  private final boolean food, booze, spleen;
  private final LockableListModel<QueuedConcoction> queue;

  public UseItemDequeuePanel(final boolean food, final boolean booze, final boolean spleen) {
    super(ConcoctionDatabase.getQueue(food, booze, spleen), false, false);
    // Remove the default borders inherited from ScrollablePanel.
    BorderLayout a = (BorderLayout) this.actualPanel.getLayout();
    a.setVgap(0);
    CardLayout b = (CardLayout) this.actualPanel.getParent().getLayout();
    b.setVgap(0);

    // Add a 10px top border.
    this.northPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);

    this.food = food;
    this.booze = booze;
    this.spleen = spleen;

    this.queueTabs = KoLmafiaGUI.getTabbedPane();

    if (this.food) {
      this.queueTabs.addTab("0 Full Queued", this.centerPanel);
      this.queue = ConcoctionDatabase.queuedFood.getMirrorImage();
    } else if (this.booze) {
      this.queueTabs.addTab("0 Drunk Queued", this.centerPanel);
      this.queue = ConcoctionDatabase.queuedBooze.getMirrorImage();
    } else if (this.spleen) {
      this.queueTabs.addTab("0 Spleen Queued", this.centerPanel);
      this.queue = ConcoctionDatabase.queuedSpleen.getMirrorImage();
    } else {
      this.queueTabs.addTab("Potions Queued", this.centerPanel);
      this.queue = ConcoctionDatabase.queuedPotions.getMirrorImage();
    }

    this.queueTabs.addTab(
        "Resources Used",
        new GenericScrollPane(
            ConcoctionDatabase.getQueuedIngredients(this.food, this.booze, this.spleen), 7));

    JLabel test = new JLabel("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    this.getElementList().setCellRenderer(ListCellRendererFactory.getCreationQueueRenderer());
    this.getElementList().setFixedCellHeight((int) (test.getPreferredSize().getHeight() * 2.5f));

    this.getElementList().setVisibleRowCount(3);
    this.getElementList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    this.actualPanel.add(this.queueTabs, BorderLayout.CENTER);

    this.setButtons(false, new ActionListener[] {new ConsumeListener(), new CreateListener()});

    this.eastPanel.add(new ThreadedButton("undo", new UndoQueueRunnable()), BorderLayout.SOUTH);

    this.setEnabled(true);
    this.filterItems();
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
      if (UseItemDequeuePanel.this.food) {
        ConcoctionDatabase.handleQueue(true, false, false, KoLConstants.CONSUME_EAT);
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
      } else if (UseItemDequeuePanel.this.booze) {
        ConcoctionDatabase.handleQueue(false, true, false, KoLConstants.CONSUME_DRINK);
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
      } else if (UseItemDequeuePanel.this.spleen) {
        ConcoctionDatabase.handleQueue(false, false, true, KoLConstants.CONSUME_SPLEEN);
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
      } else {
        ConcoctionDatabase.handleQueue(false, false, false, KoLConstants.CONSUME_USE);
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
      if (UseItemDequeuePanel.this.food) {
        ConcoctionDatabase.handleQueue(true, false, false, KoLConstants.NO_CONSUME);
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
      } else if (UseItemDequeuePanel.this.booze) {
        ConcoctionDatabase.handleQueue(false, true, false, KoLConstants.NO_CONSUME);
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
      } else if (UseItemDequeuePanel.this.spleen) {
        ConcoctionDatabase.handleQueue(false, false, true, KoLConstants.NO_CONSUME);
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
      } else {
        ConcoctionDatabase.handleQueue(false, false, false, KoLConstants.NO_CONSUME);
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
      ConcoctionDatabase.pop(
          UseItemDequeuePanel.this.food,
          UseItemDequeuePanel.this.booze,
          UseItemDequeuePanel.this.spleen);
      ConcoctionDatabase.refreshConcoctions();

      if (UseItemDequeuePanel.this.food) {
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedFullness() + " Full Queued");
      } else if (UseItemDequeuePanel.this.booze) {
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued");
      } else if (UseItemDequeuePanel.this.spleen) {
        UseItemDequeuePanel.this.queueTabs.setTitleAt(
            0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued");
      }
      ConcoctionDatabase.getUsables().sort();
    }
  }
}
