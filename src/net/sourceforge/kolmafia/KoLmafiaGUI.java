package net.sourceforge.kolmafia;

import java.awt.Frame;
import java.util.ArrayList;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.BuffBotDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClanWarRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.session.BuffBotManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.swingui.BuffBotFrame;
import net.sourceforge.kolmafia.swingui.BuffRequestFrame;
import net.sourceforge.kolmafia.swingui.CakeArenaFrame;
import net.sourceforge.kolmafia.swingui.CalendarFrame;
import net.sourceforge.kolmafia.swingui.ClanManageFrame;
import net.sourceforge.kolmafia.swingui.ContactListFrame;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.swingui.LoginFrame;
import net.sourceforge.kolmafia.swingui.MuseumFrame;
import net.sourceforge.kolmafia.swingui.MushroomFrame;
import net.sourceforge.kolmafia.swingui.OptionsFrame;
import net.sourceforge.kolmafia.swingui.SendMessageFrame;
import net.sourceforge.kolmafia.swingui.SkillBuffFrame;
import net.sourceforge.kolmafia.swingui.StoreManageFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;
import net.sourceforge.kolmafia.webui.RelayServer;
import tab.CloseTabbedPane;

public class KoLmafiaGUI {
  /**
   * The main method. Currently, it instantiates a single instance of the <code>KoLmafia</code>after
   * setting the default look and feel of all <code>JFrame</code> objects to decorated.
   */
  static void initialize() {
    KoLmafiaGUI.initializeLoginInterface();

    // All that completed, check to see if there is an auto-login
    // which should occur.

    String autoLogin = Preferences.getString("autoLogin");
    if (!autoLogin.equals("")) {
      // Make sure that a password was stored for this
      // character (would fail otherwise):

      String password = KoLmafia.getSaveState(autoLogin);
      if (password != null && !password.equals("")) {
        RequestThread.postRequest(new LoginRequest(autoLogin, password));
      }
    }
  }

  public static void checkFrameSettings() {
    String frameSetting = Preferences.getString("initialFrames");
    String desktopSetting = Preferences.getString("initialDesktop");

    // If there is still no data (somehow the global data
    // got emptied), default to relay-browser only).

    if (desktopSetting.equals("") && frameSetting.equals("")) {
      Preferences.setString("initialDesktop", "AdventureFrame,CommandDisplayFrame,GearChangeFrame");
    }
  }

  public static void initializeLoginInterface() {
    KoLmafiaGUI.constructFrame(LoginFrame.class);

    if (Preferences.getString("useDecoratedTabs").equals("")) {
      Preferences.setBoolean("useDecoratedTabs", !System.getProperty("os.name").startsWith("Mac"));
    }

    if (!Preferences.getBoolean("customizedTabs")) {
      KoLmafiaGUI.constructFrame(OptionsFrame.class);
      Preferences.setBoolean("customizedTabs", true);
    }
  }

  public static void intializeMainInterfaces() {
    LoginFrame.hideInstance();

    KoLmafiaGUI.checkFrameSettings();
    String frameSetting = Preferences.getString("initialFrames");
    String desktopSetting = Preferences.getString("initialDesktop");

    // Reset all the titles on all existing frames.

    SystemTrayFrame.updateToolTip();
    KoLDesktop.updateTitle();

    // Instantiate the appropriate instance of the
    // frame that should be loaded based on the mode.

    if (!desktopSetting.equals("")) {
      if (!Preferences.getBoolean("relayBrowserOnly")) {
        KoLDesktop.getInstance().setVisible(true);
      }
    }

    String[] frameArray = frameSetting.split(",");
    String[] desktopArray = desktopSetting.split(",");

    ArrayList<String> initialFrameList = new ArrayList<>();

    if (!frameSetting.equals("")) {
      for (String s : frameArray) {
        if (!initialFrameList.contains(s)) {
          initialFrameList.add(s);
        }
      }
    }

    for (String s : desktopArray) {
      initialFrameList.remove(s);
    }

    if (!initialFrameList.isEmpty() && !Preferences.getBoolean("relayBrowserOnly")) {
      String[] initialFrames = new String[initialFrameList.size()];
      initialFrameList.toArray(initialFrames);

      for (String initialFrame : initialFrames) {
        KoLmafiaGUI.constructFrame(initialFrame);
      }
      String lastOne = initialFrames[initialFrames.length - 1];
      Frame[] frames = Frame.getFrames();
      for (Frame f : frames) {
        if (f.getClass().getName().endsWith(lastOne)) {
          f.requestFocus();
          break;
        }
      }
    }

    // Figure out which user interface is being
    // used -- account for minimalist loadings.

    LoginFrame.disposeInstance();
  }

  public static void constructFrame(final String frameName) {
    if (frameName.equals("")) {
      return;
    }

    if (frameName.equals("ChatManager")) {
      KoLmafia.updateDisplay("Initializing chat interface...");
      ChatManager.initialize();
      return;
    }

    if (frameName.equals("LocalRelayServer")) {
      if (StaticEntity.isGUIRequired()) {
        RelayLoader.openRelayBrowser();
      }
      return;
    }

    try {
      Class<?> frameClass = Class.forName("net.sourceforge.kolmafia.swingui." + frameName);
      KoLmafiaGUI.constructFrame(frameClass);
    } catch (ClassNotFoundException e) {
      // Can happen if preference file made by an earlier
      // version of KoLmafia and the frame has been renamed.

      // We don't need a full stack trace, but an informative
      // message would be nice.
    } catch (Exception e) {
      // Should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static void constructFrame(final Class<?> frameClass) {
    // Now, test to see if any requests need to be run before
    // you fall into the event dispatch thread.

    if (frameClass == BuffBotFrame.class) {
      BuffBotManager.loadSettings();
    } else if (frameClass == BuffRequestFrame.class) {
      if (!BuffBotDatabase.hasOfferings()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No buffs found to purchase.");
        return;
      }
    } else if (frameClass == CakeArenaFrame.class) {
      if (CakeArenaManager.getOpponentList().isEmpty()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Equip a familiar first.");
        return;
      }
    } else if (frameClass == CalendarFrame.class) {
      String base = KoLmafia.imageServerPath() + "otherimages/bikini/";
      for (int i = 1; i < CalendarFrame.CALENDARS.length; ++i) {
        FileUtilities.downloadImage(base + CalendarFrame.CALENDARS[i] + ".gif");
      }
      base = KoLmafia.imageServerPath() + "otherimages/beefcake/";
      for (int i = 1; i < CalendarFrame.CALENDARS.length; ++i) {
        FileUtilities.downloadImage(base + CalendarFrame.CALENDARS[i] + ".gif");
      }
    } else if (frameClass == ClanManageFrame.class) {
      if (Preferences.getBoolean("clanAttacksEnabled")) {
        RequestThread.postRequest(new ClanWarRequest());
      }

      if (InventoryManager.canUseClanStash()) {
        ClanManager.getStash();
      }
    } else if (frameClass == ContactListFrame.class) {
      ContactManager.updateMailContacts();
    } else if (frameClass == FamiliarTrainingFrame.class) {
      if (CakeArenaManager.getOpponentList().isEmpty()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Equip a familiar first.");
        return;
      }
    } else if (frameClass == ItemManageFrame.class) {
      if (InventoryManager.canUseClanStash()) {
        if (!ClanManager.isStashRetrieved()) {
          RequestThread.postRequest(new ClanStashRequest());
        }
      }

    } else if (frameClass == RelayServer.class) {
      RelayLoader.openRelayBrowser();
      return;
    } else if (frameClass == MuseumFrame.class) {
      if (CharPaneRequest.inValhalla()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't find your display case in Valhalla.");
        return;
      }

      if (!KoLCharacter.hasDisplayCase()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Sorry, you don't have a display case.");
        return;
      }

      if (DisplayCaseManager.getHeaders().isEmpty()) {
        RequestThread.postRequest(new DisplayCaseRequest());
      }
    } else if (frameClass == MushroomFrame.class) {
      String base = KoLmafia.imageServerPath() + "itemimages/";
      for (int i = 0; i < MushroomManager.MUSHROOMS.length; ++i) {
        FileUtilities.downloadImage(base + MushroomManager.MUSHROOMS[i][1]);
      }
    } else if (frameClass == SendMessageFrame.class) {
      ContactManager.updateMailContacts();
    } else if (frameClass == SkillBuffFrame.class) {
      ContactManager.updateMailContacts();
    } else if (frameClass == StoreManageFrame.class) {
      if (!KoLCharacter.hasStore()) {
        KoLmafia.updateDisplay("You don't own a store in the Mall of Loathing.");
        return;
      }

      StoreManager.clearCache();
      RequestThread.postRequest(new ManageStoreRequest());
    }

    (new CreateFrameRunnable(frameClass)).run();
  }

  public static JTabbedPane getTabbedPane() {
    return Preferences.getBoolean("useDecoratedTabs") ? new CloseTabbedPane() : new JTabbedPane();
  }

  public static Boolean isDarkTheme() {
    String currentTheme = UIManager.getLookAndFeel().getClass().getName();
    return KoLGUIConstants.FLATMAP_DARK_LOOKS.containsValue(currentTheme);
  }
}
