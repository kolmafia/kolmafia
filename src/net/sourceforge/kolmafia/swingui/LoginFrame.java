package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.panel.OptionsPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.EditableAutoFilterComboBox;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LoginFrame extends GenericFrame {
  private static LoginFrame INSTANCE = null;

  private LoginPanel loginPanel = new LoginPanel();
  private ProxyOptionsPanel httpProxyOptions = new ProxyOptionsPanel("http");
  private ProxyOptionsPanel httpsProxyOptions = new ProxyOptionsPanel("https");

  public LoginFrame() {
    super(StaticEntity.getVersion() + ": Login");

    this.tabs.addTab("KoL Login", this.constructLoginPanel());

    JPanel proxyPanel = new JPanel();
    proxyPanel.setLayout(new BoxLayout(proxyPanel, BoxLayout.Y_AXIS));
    proxyPanel.add(new ProxySetPanel());
    proxyPanel.add(httpProxyOptions);
    proxyPanel.add(httpsProxyOptions);

    this.tabs.addTab("Connection", new ConnectionOptionsPanel());
    this.tabs.addTab("Proxy Settings", proxyPanel);

    this.setCenterComponent(this.tabs);

    LoginFrame.INSTANCE = this;

    this.setFocusCycleRoot(true);
    this.setFocusTraversalPolicy(
        new DefaultComponentFocusTraversalPolicy(loginPanel.usernameField));
  }

  @Override
  public boolean shouldAddStatusBar() {
    return false;
  }

  @Override
  public boolean showInWindowMenu() {
    return false;
  }

  public static final void hideInstance() {
    if (LoginFrame.INSTANCE != null) {
      LoginFrame.INSTANCE.setVisible(false);
    }
  }

  public static final void disposeInstance() {
    if (LoginFrame.INSTANCE != null) {
      LoginFrame.INSTANCE.dispose();
      LoginFrame.INSTANCE = null;
    }
  }

  @Override
  public void dispose() {
    super.dispose();

    this.loginPanel = null;
    this.httpProxyOptions = null;
    this.httpsProxyOptions = null;
  }

  private void honorProxySettings() {
    if (this.httpProxyOptions != null) {
      this.httpProxyOptions.actionConfirmed();
    }

    if (this.httpsProxyOptions != null) {
      this.httpsProxyOptions.actionConfirmed();
    }
  }

  @Override
  protected void checkForLogout() {
    this.honorProxySettings();

    if (!LoginRequest.isInstanceRunning()) {
      KoLmafia.quit();
    }
  }

  public JPanel constructLoginPanel() {
    String logoName = Preferences.getString("loginWindowLogo");

    if (logoName.endsWith(".jpg")) {
      logoName = logoName.substring(0, logoName.length() - 4) + ".gif";
      Preferences.setString("loginWindowLogo", logoName);
    }

    JPanel imagePanel = new JPanel(new BorderLayout(0, 0));
    imagePanel.add(new JLabel(" "), BorderLayout.NORTH);
    imagePanel.add(
        new JLabel(JComponentUtilities.getImage(logoName), SwingConstants.CENTER),
        BorderLayout.SOUTH);

    JPanel containerPanel = new JPanel(new BorderLayout());
    containerPanel.add(imagePanel, BorderLayout.NORTH);
    containerPanel.add(this.loginPanel, BorderLayout.CENTER);
    return containerPanel;
  }

  /**
   * An internal class which represents the panel which is nested inside of the <code>LoginFrame
   * </code>.
   */
  private class LoginPanel extends GenericPanel {
    private final LoginNameComboBox usernameField;
    private final JPasswordField passwordField;

    private final JCheckBox stealthLoginCheckBox;
    private final JCheckBox savePasswordCheckBox;
    private final JCheckBox getBreakfastCheckBox;

    /**
     * Constructs a new <code>LoginPanel</code>, containing a place for the users to input their
     * login name and password. This panel, because it is intended to be the content panel for
     * status message updates, also has a status label.
     */
    public LoginPanel() {
      super("login");

      this.usernameField = new LoginNameComboBox();
      this.passwordField = new JPasswordField();

      this.savePasswordCheckBox = new JCheckBox();
      this.stealthLoginCheckBox = new JCheckBox();
      this.getBreakfastCheckBox = new JCheckBox();

      VerifiableElement[] elements = new VerifiableElement[2];
      elements[0] = new VerifiableElement("Login: ", this.usernameField);
      elements[1] = new VerifiableElement("Password: ", this.passwordField);

      this.setContent(elements);

      JPanel checkBoxPanels = new JPanel();
      checkBoxPanels.add(Box.createHorizontalStrut(16));
      checkBoxPanels.add(new JLabel("Save Password: "), "");
      checkBoxPanels.add(this.savePasswordCheckBox);
      checkBoxPanels.add(Box.createHorizontalStrut(16));
      checkBoxPanels.add(new JLabel("Stealth Login: "), "");
      checkBoxPanels.add(this.stealthLoginCheckBox);
      checkBoxPanels.add(Box.createHorizontalStrut(16));
      checkBoxPanels.add(new JLabel("Breakfast: "), "");
      checkBoxPanels.add(this.getBreakfastCheckBox);
      checkBoxPanels.add(Box.createHorizontalStrut(16));

      this.actionStatusPanel.add(new JLabel(" ", SwingConstants.CENTER), BorderLayout.CENTER);
      this.actionStatusPanel.add(checkBoxPanels, BorderLayout.NORTH);

      String lastUsername = Preferences.getString("lastUsername");
      this.usernameField.setSelectedItem(lastUsername);

      String passwordSetting = KoLmafia.getSaveState(lastUsername);

      if (passwordSetting != null) {
        this.passwordField.setText(passwordSetting);
        this.savePasswordCheckBox.setSelected(true);
      }

      this.getBreakfastCheckBox.addActionListener(new GetBreakfastListener());
      this.savePasswordCheckBox.addActionListener(new RemovePasswordListener());

      LoginPanel.this.getBreakfastCheckBox.setSelected(
          Preferences.getBoolean(lastUsername, "getBreakfast"));
      LoginPanel.this.stealthLoginCheckBox.setSelected(Preferences.getBoolean("stealthLogin"));

      String holiday = HolidayDatabase.getHoliday(true);
      String moonEffect = HolidayDatabase.getMoonEffect();

      String updateText;

      if (holiday.equals("")) {
        updateText = moonEffect;
      } else {
        updateText = holiday + ", " + moonEffect;
      }

      updateText = StringUtilities.getEntityDecode(updateText, false);
      this.setStatusMessage(updateText);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (this.usernameField == null || this.passwordField == null) {
        return;
      }

      if (this.savePasswordCheckBox == null || this.getBreakfastCheckBox == null) {
        return;
      }

      super.setEnabled(isEnabled);

      this.usernameField.setEnabled(isEnabled);
      this.passwordField.setEnabled(isEnabled);
    }

    @Override
    public void actionConfirmed() {
      Preferences.setBoolean("relayBrowserOnly", false);
      this.doLogin();
    }

    @Override
    public void actionCancelled() {
      if (!LoginRequest.isInstanceRunning()) {
        Preferences.setBoolean("relayBrowserOnly", true);
        this.doLogin();
      }
    }

    private String getUsername() {
      if (this.usernameField.getSelectedItem() != null) {
        return (String) this.usernameField.getSelectedItem();
      }

      return (String) this.usernameField.currentMatch;
    }

    private void doLogin() {
      String username = getUsername();
      String password = new String(this.passwordField.getPassword());

      if (username == null || username.equals("") || password.equals("")) {
        this.setStatusMessage("Invalid login.");
        return;
      }

      this.setEnabled(false);

      Preferences.setBoolean(username, "getBreakfast", this.getBreakfastCheckBox.isSelected());

      Preferences.setBoolean("stealthLogin", LoginPanel.this.stealthLoginCheckBox.isSelected());

      LoginFrame.this.honorProxySettings();

      RequestThread.postRequest(new LoginRequest(username, password));
    }

    private class GetBreakfastListener extends ThreadedListener {
      @Override
      protected void execute() {
        Preferences.setBoolean(
            getUsername(), "getBreakfast", LoginPanel.this.getBreakfastCheckBox.isSelected());
      }
    }

    private class RemovePasswordListener extends ThreadedListener {
      @Override
      protected void execute() {
        if (!LoginPanel.this.savePasswordCheckBox.isSelected()) {
          String value =
              (String) ((SortedListModel<String>) KoLConstants.saveStateNames).getSelectedItem();
          if (value == null) {
            return;
          }

          KoLConstants.saveStateNames.remove(value);
          KoLmafia.removeSaveState(value);
          LoginPanel.this.passwordField.setText("");
        }

        Preferences.setBoolean(
            "saveStateActive", LoginPanel.this.savePasswordCheckBox.isSelected());
      }
    }

    /**
     * Special instance of a JComboBox which overrides the default key events of a JComboBox to
     * allow you to catch key events.
     */
    private class LoginNameComboBox extends EditableAutoFilterComboBox {
      public LoginNameComboBox() {
        super((SortedListModel<String>) KoLConstants.saveStateNames);
      }

      @Override
      public void setSelectedItem(final Object anObject) {
        super.setSelectedItem(anObject);
        this.setPassword();
      }

      @Override
      public synchronized void findMatch(final int keyCode) {
        super.findMatch(keyCode);
        this.setPassword();
      }

      private void setPassword() {
        if (this.currentMatch == null) {
          LoginPanel.this.passwordField.setText("");
          LoginPanel.this.setStatusMessage(" ");

          LoginPanel.this.setEnabled(true);
          return;
        }

        String password = KoLmafia.getSaveState((String) this.currentMatch);
        if (password == null) {
          LoginPanel.this.passwordField.setText("");
          LoginPanel.this.setStatusMessage(" ");

          LoginPanel.this.setEnabled(true);
          return;
        }

        LoginPanel.this.passwordField.setText(password);
        LoginPanel.this.savePasswordCheckBox.setSelected(true);

        boolean breakfastSetting =
            Preferences.getBoolean(((String) this.currentMatch), "getBreakfast");

        LoginPanel.this.getBreakfastCheckBox.setSelected(breakfastSetting);
        LoginPanel.this.setEnabled(true);
      }
    }
  }

  private class ProxySetPanel extends OptionsPanel {
    private final String[][] options = {
      {"proxySet", "KoLmafia needs to connect through a proxy server"},
    };

    public ProxySetPanel() {
      super(new Dimension(20, 20), new Dimension(250, 20));

      this.setOptions(this.options);

      String httpHost = System.getProperty("http.proxyHost");
      String httpsHost = System.getProperty("https.proxyHost");

      boolean proxySet =
          httpHost != null && httpHost.length() > 0 || httpsHost != null && httpsHost.length() > 0;

      if (System.getProperty("os.name").startsWith("Mac")) {
        this.optionBoxes[0].setSelected(proxySet);
        this.optionBoxes[0].setEnabled(false);
      } else {
        proxySet |= Preferences.getBoolean("proxySet");
        this.optionBoxes[0].setSelected(proxySet);
      }
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (System.getProperty("os.name").startsWith("Mac")) {
        return;
      }

      super.setEnabled(isEnabled);
    }
  }

  private class ConnectionOptionsPanel extends OptionsPanel {
    private final String[][] options = {
      {"useDevProxyServer", "Use devproxy.kingdomofloathing.com to login"},
      {"connectViaAddress", "Use IP address to connect instead of host name"},
      {"useNaiveSecureLogin", "Do not have Java try to validate SSL certificates"},
      {"allowSocketTimeout", "Forcibly time-out laggy requests"}
    };

    public ConnectionOptionsPanel() {
      super(new Dimension(20, 20), new Dimension(250, 20));

      this.setOptions(this.options);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
    }
  }

  /** This panel handles all of the things related to proxy options (if applicable). */
  private class ProxyOptionsPanel extends LabeledPanel {
    private final String protocol;

    private final AutoHighlightTextField proxyHost;
    private final AutoHighlightTextField proxyPort;
    private final AutoHighlightTextField proxyLogin;
    private final AutoHighlightTextField proxyPassword;

    /**
     * Constructs a new <code>ProxyOptionsPanel</code>, containing a place for the users to select
     * their desired server and for them to modify any applicable proxy settings.
     */
    public ProxyOptionsPanel(String protocol) {
      super("Proxy Settings: " + protocol, new Dimension(80, 20), new Dimension(240, 20));

      this.protocol = protocol;

      this.proxyHost = new AutoHighlightTextField();
      this.proxyPort = new AutoHighlightTextField();
      this.proxyLogin = new AutoHighlightTextField();
      this.proxyPassword = new AutoHighlightTextField();

      VerifiableElement[] elements = new VerifiableElement[4];
      elements[0] = new VerifiableElement("Host: ", this.proxyHost);
      elements[1] = new VerifiableElement("Port: ", this.proxyPort);
      elements[2] = new VerifiableElement("Login: ", this.proxyLogin);
      elements[3] = new VerifiableElement("Password: ", this.proxyPassword);

      this.actionCancelled();
      this.setContent(elements);
    }

    @Override
    public void actionConfirmed() {
      if (System.getProperty("os.name").startsWith("Mac")) {
        return;
      }

      Preferences.setString(this.protocol + ".proxyHost", this.proxyHost.getText());
      Preferences.setString(this.protocol + ".proxyPort", this.proxyPort.getText());
      Preferences.setString(this.protocol + ".proxyUser", this.proxyLogin.getText());
      Preferences.setString(this.protocol + ".proxyPassword", this.proxyPassword.getText());
    }

    @Override
    public void actionCancelled() {
      String proxyHost = System.getProperty(this.protocol + ".proxyHost");

      if (proxyHost != null && proxyHost.length() != 0
          || System.getProperty("os.name").startsWith("Mac")) {
        this.proxyHost.setText(System.getProperty(this.protocol + ".proxyHost"));
        this.proxyPort.setText(System.getProperty(this.protocol + ".proxyPort"));
        this.proxyLogin.setText(System.getProperty(this.protocol + ".proxyUser"));
        this.proxyPassword.setText(System.getProperty(this.protocol + ".proxyPassword"));
      } else {
        this.proxyHost.setText(Preferences.getString(this.protocol + ".proxyHost"));
        this.proxyPort.setText(Preferences.getString(this.protocol + ".proxyPort"));
        this.proxyLogin.setText(Preferences.getString(this.protocol + ".proxyUser"));
        this.proxyPassword.setText(Preferences.getString(this.protocol + ".proxyPassword"));
      }

      if (System.getProperty("os.name").startsWith("Mac")) {
        this.proxyHost.setEnabled(false);
        this.proxyPort.setEnabled(false);
        this.proxyLogin.setEnabled(false);
        this.proxyPassword.setEnabled(false);
      }
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (System.getProperty("os.name").startsWith("Mac")) {
        return;
      }

      super.setEnabled(isEnabled);
    }
  }
}
