package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.BuffBotDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.session.BuffBotManager.Offering;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BuffRequestFrame extends GenericFrame {
  private static final String NO_REQUEST_TEXT =
      "\nTo whom it may concern:\n\n"
          + "At the frequent request of individuals wanting to see the name 'BOT_NAME' listed in KoLmafia's buff purchase interface, "
          + "BOT_NAME has been added to our internal buffbot database.\n\n"
          + "However, at the request of the individuals responsible for maintaining BOT_NAME, "
          + "BOT_NAME's formal price list and buff offerings are not available directly through KoLmafia.\n\n"
          + "You are welcome to use this interface to check whether or not BOT_NAME is currently logged in to KoL.  "
          + "However, we hope this message helps you understand why additional support was not added.\n\n\n"
          + "Respectfully yours,\nThe KoLmafia development team";

  private String botName;
  private final JComboBox<String> names, types;
  private final List<SortedListModel<String>> nameList;

  private final TreeMap<String, RequestPanel> panelMap;

  private final JPanel nameContainer;
  private final CardLayout nameCards;

  private RequestPanel lastPanel;
  private final BuffRequestPanel mainPanel;
  private final TotalPriceUpdater priceUpdater = new TotalPriceUpdater();

  public BuffRequestFrame() {
    super("Purchase Buffs");

    this.panelMap = new TreeMap<>();
    this.nameList = new ArrayList<>(4);
    for (int i = 0; i < 4; ++i) {
      this.nameList.add(new SortedListModel<>());
    }

    this.names = new JComboBox<>(this.nameList.get(0));

    this.types = new JComboBox<>();
    this.types.addItem("buff packs");
    this.types.addItem("sauceror buffs");
    this.types.addItem("turtle tamer buffs");
    this.types.addItem("accordion thief buffs");

    CardSwitchListener listener = new CardSwitchListener();

    this.addActionListener(this.names, listener);
    this.addActionListener(this.types, listener);

    this.nameCards = new CardLayout();
    this.nameContainer = new JPanel(this.nameCards);

    this.mainPanel = new BuffRequestPanel();
    this.setCenterComponent(this.mainPanel);

    this.nameContainer.add(new GenericScrollPane(new JPanel()), "");

    int lastSelectedIndex = Preferences.getInteger("lastBuffRequestType");
    if (lastSelectedIndex >= 0 && lastSelectedIndex < 4) {
      this.types.setSelectedIndex(lastSelectedIndex);
    }

    this.resetCard();
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

  @Override
  public void dispose() {
    Preferences.setInteger("lastBuffRequestType", this.types.getSelectedIndex());
    super.dispose();
  }

  private void isBotOnline(final String botName) {
    if (KoLmafia.isPlayerOnline(botName)) {
      InputFieldUtilities.alert(botName + " is online.");
    } else {
      InputFieldUtilities.alert(botName + " is probably not online.");
    }
  }

  private class BuffRequestPanel extends GenericPanel {
    public BuffRequestPanel() {
      super("request", "online?");

      String[] list = BuffBotDatabase.getCompleteBotList();

      for (int i = 0; i < list.length; ++i) {
        if (list[i] == null || list[i].equals("")) {
          continue;
        }

        RequestPanel panel = new RequestPanel(list[i]);
        BuffRequestFrame.this.panelMap.put(list[i], panel);
        BuffRequestFrame.this.nameContainer.add(panel, list[i]);
      }

      VerifiableElement[] elements = new VerifiableElement[2];
      elements[0] = new VerifiableElement("Category:  ", BuffRequestFrame.this.types);
      elements[1] = new VerifiableElement("Bot Name:  ", BuffRequestFrame.this.names);

      this.setContent(elements);
      this.add(BuffRequestFrame.this.nameContainer, BorderLayout.SOUTH);
    }

    @Override
    public void actionConfirmed() {
      RequestPanel panel = BuffRequestFrame.this.getPanel();

      if (panel == null) {
        return;
      }

      JCheckBox[] checkboxes = panel.checkboxes;
      Offering[] offerings = panel.offerings;

      ArrayList<SendMailRequest> requests = new ArrayList<>();
      for (int i = 0; i < checkboxes.length; ++i) {
        if (checkboxes[i] != null && checkboxes[i].isSelected()) {
          checkboxes[i].setSelected(false);
          requests.add(offerings[i].toRequest());
        }
      }

      if (requests.isEmpty()) {
        return;
      }

      for (int i = 0; i < requests.size(); ++i) {
        KoLmafia.updateDisplay(
            "Submitting buff request "
                + (i + 1)
                + " of "
                + requests.size()
                + " to "
                + BuffRequestFrame.this.botName
                + "...");
        RequestThread.postRequest(requests.get(i));
      }

      KoLmafia.updateDisplay("Buff requests complete.");
    }

    @Override
    public boolean shouldAddStatusLabel() {
      return true;
    }

    @Override
    public void actionCancelled() {
      BuffRequestFrame.this.isBotOnline(BuffRequestFrame.this.botName);
    }
  }

  private class RequestPanel extends JPanel {
    private int lastBuffId = 0;
    private boolean addedBuffPackLabel = false;
    private final CardLayout categoryCards = new CardLayout();
    private final JPanel[] categoryPanels = new JPanel[4];

    private final JCheckBox[] checkboxes;
    private final Offering[] offerings;

    public RequestPanel(final String botName) {
      this.setLayout(this.categoryCards);

      if (BuffBotDatabase.getStandardOfferings(botName).isEmpty()) {
        this.checkboxes = null;
        this.offerings = null;

        this.addNoRequestMessage(botName);
        return;
      }

      for (int i = 0; i < 4; ++i) {
        this.categoryPanels[i] = new JPanel();
        this.categoryPanels[i].setLayout(new BoxLayout(this.categoryPanels[i], BoxLayout.Y_AXIS));

        GenericScrollPane scroller = new GenericScrollPane(this.categoryPanels[i]);
        JComponentUtilities.setComponentSize(scroller, 500, 400);

        this.add(scroller, String.valueOf(i));
      }

      ArrayList<Offering> list = new ArrayList<>();
      list.addAll(BuffBotDatabase.getStandardOfferings(botName));
      list.addAll(BuffBotDatabase.getPhilanthropicOfferings(botName));

      Collections.sort(list);

      this.offerings = new Offering[list.size()];
      list.toArray(this.offerings);

      this.checkboxes = new JCheckBox[this.offerings.length];

      for (int i = 0; i < this.checkboxes.length; ++i) {
        if (this.offerings[i].getLowestBuffId() < 1000) {
          continue;
        }

        this.checkboxes[i] = new JCheckBox(this.offerings[i].toString());
        this.checkboxes[i].setVerticalTextPosition(SwingConstants.TOP);
        BuffRequestFrame.this.addActionListener(
            this.checkboxes[i], BuffRequestFrame.this.priceUpdater);

        int price = this.offerings[i].getPrice();
        int[] turns = this.offerings[i].getTurns();
        String tooltip =
            price
                + " meat ("
                + KoLConstants.FLOAT_FORMAT.format((float) turns[0] / (float) price)
                + " turns/meat)";
        this.checkboxes[i].setToolTipText(tooltip);

        int buffId = this.offerings[i].getLowestBuffId();
        int categoryId = this.getCategory(turns.length, buffId);

        this.addBuffLabel(turns.length, buffId, categoryId);

        if (!BuffRequestFrame.this.nameList.get(categoryId).contains(botName)) {
          BuffRequestFrame.this.nameList.get(categoryId).add(botName);
        }

        this.categoryPanels[categoryId].add(this.checkboxes[i]);
      }
    }

    private int getCategory(final int count, final int buffId) {
      if (count > 1) {
        return 0;
      } else if (SkillDatabase.isSaucerorBuff(buffId)) {
        return 1;
      } else if (SkillDatabase.isTurtleTamerBuff(buffId)) {
        return 2;
      } else if (SkillDatabase.isAccordionThiefSong(buffId)) {
        return 3;
      } else {
        return 0;
      }
    }

    private void addNoRequestMessage(final String botName) {
      for (int i = 0; i < 4; ++i) {
        JTextArea message =
            new JTextArea(
                StringUtilities.globalStringReplace(
                    BuffRequestFrame.NO_REQUEST_TEXT, "BOT_NAME", botName));

        message.setColumns(40);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setEditable(false);
        message.setOpaque(false);
        message.setFont(KoLGUIConstants.DEFAULT_FONT);

        this.add(new GenericScrollPane(message), String.valueOf(i));
      }
    }

    private void addBuffLabel(final int count, final int buffId, final int categoryId) {
      if (count > 1) {
        if (this.addedBuffPackLabel) {
          return;
        }

        this.addedBuffPackLabel = true;
        this.categoryPanels[categoryId].add(new JLabel("<html><h3>Buff Packs</h3></html>"));
        this.categoryPanels[categoryId].add(Box.createVerticalStrut(5));
        return;
      }

      if (buffId == this.lastBuffId) {
        return;
      }

      this.lastBuffId = buffId;
      this.categoryPanels[categoryId].add(
          new JLabel("<html><h3>" + SkillDatabase.getSkillName(buffId) + "</h3></html>"));
      this.categoryPanels[categoryId].add(Box.createVerticalStrut(5));
    }
  }

  private void updateSendPrice() {
    if (this.mainPanel == null) {
      return;
    }

    RequestPanel panel = this.getPanel();
    if (panel == null || panel.checkboxes == null || panel.offerings == null) {
      return;
    }

    if (this.lastPanel != null && this.lastPanel != panel) {
      JCheckBox[] checkboxes = this.lastPanel.checkboxes;

      for (int i = 0; i < checkboxes.length; ++i) {
        if (checkboxes[i] != null) {
          checkboxes[i].setSelected(false);
        }
      }
    }

    this.lastPanel = panel;

    int price = 0;
    JCheckBox[] checkboxes = panel.checkboxes;
    Offering[] offerings = panel.offerings;

    for (int i = 0; i < checkboxes.length; ++i) {
      if (checkboxes[i] == null || offerings[i] == null) {
        continue;
      }

      if (checkboxes[i].isSelected()) {
        price += offerings[i].getPrice();
      }
    }

    this.mainPanel.setStatusMessage(
        KoLConstants.COMMA_FORMAT.format(price) + " meat will be sent to " + this.botName);
  }

  private String getCardId() {
    this.botName = (String) this.names.getSelectedItem();
    return this.botName;
  }

  private void resetCard() {
    int typeId = this.types.getSelectedIndex();
    if (typeId != -1 && this.names.getModel() != this.nameList.get(typeId)) {
      this.names.setModel(this.nameList.get(typeId));
    }

    RequestPanel panel = this.getPanel();
    if (typeId == -1 || panel == null) {
      this.nameCards.show(this.nameContainer, "");
      this.mainPanel.setStatusMessage(" ");
      return;
    }

    panel.categoryCards.show(panel, String.valueOf(typeId));
    this.nameCards.show(this.nameContainer, this.getCardId());

    this.updateSendPrice();
  }

  private RequestPanel getPanel() {
    String cardId = this.getCardId();
    if (cardId == null) {
      return null;
    }

    return this.panelMap.get(cardId);
  }

  private class CardSwitchListener extends ThreadedListener {
    @Override
    protected void execute() {
      BuffRequestFrame.this.resetCard();
    }
  }

  private class TotalPriceUpdater implements ActionListener {
    @Override
    public void actionPerformed(final ActionEvent e) {
      BuffRequestFrame.this.updateSendPrice();
    }
  }
}
