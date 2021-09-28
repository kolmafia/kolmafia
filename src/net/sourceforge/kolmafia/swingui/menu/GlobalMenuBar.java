package net.sourceforge.kolmafia.swingui.menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSeparator;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.GuildUnlockManager;
import net.sourceforge.kolmafia.session.LeafletManager;
import net.sourceforge.kolmafia.session.LogoutManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TowerDoorManager;
import net.sourceforge.kolmafia.session.VioletFogManager;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.swingui.listener.LicenseDisplayListener;

public class GlobalMenuBar extends JMenuBar {
  private final ScriptMenu scriptMenu;
  private final WindowMenu windowMenu;
  private BookmarkMenu bookmarkMenu;

  public GlobalMenuBar() {
    // Add general features.

    JMenu statusMenu = new JMenu("General");
    this.add(statusMenu);

    // Add the refresh menu, which holds the ability to refresh
    // everything in the session.

    statusMenu.add(new DisplayFrameMenuItem("Adventure", "AdventureFrame"));
    statusMenu.add(new DisplayFrameMenuItem("Purchases", "MallSearchFrame"));
    statusMenu.add(new DisplayFrameMenuItem("Graphical CLI", "CommandDisplayFrame"));
    statusMenu.add(new DisplayFrameMenuItem("Preferences", "OptionsFrame"));

    statusMenu.add(new JSeparator());

    statusMenu.add(new DisplayFrameMenuItem("Mini-Browser", "RequestFrame"));
    statusMenu.add(new RelayBrowserMenuItem());
    statusMenu.add(new RelayBrowserMenuItem("KoLmafia Forums", "https://kolmafia.us/"));

    statusMenu.add(new JSeparator());

    statusMenu.add(new DisplayFrameMenuItem("Daily Deeds", "CharSheetFrame"));
    statusMenu.add(new DisplayFrameMenuItem("Item Manager", "ItemManageFrame"));
    statusMenu.add(new DisplayFrameMenuItem("Gear Changer", "GearChangeFrame"));
    statusMenu.add(new DisplayFrameMenuItem("Skill Casting", "SkillBuffFrame"));

    if (!System.getProperty("os.name").startsWith("Mac")) {
      statusMenu.add(new JSeparator());
      statusMenu.add(new InvocationMenuItem("Logout of KoL", LogoutManager.class, "logout"));
      statusMenu.add(new InvocationMenuItem("Exit KoLmafia", KoLmafia.class, "quit"));
    }

    // Add specialized tools.

    JMenu toolsMenu = new JMenu("Tools");
    this.add(toolsMenu);

    toolsMenu.add(new InvocationMenuItem("Clear Results", KoLmafia.class, "resetSession"));
    toolsMenu.add(
        new InvocationMenuItem("Stop Everything", RequestThread.class, "declareWorldPeace"));
    toolsMenu.add(new InvocationMenuItem("Refresh Session", KoLmafia.class, "refreshSession"));

    toolsMenu.add(new JSeparator());

    toolsMenu.add(new DisplayFrameMenuItem("Meat Manager", "MeatManageFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Store Manager", "StoreManageFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Coin Masters", "CoinmastersFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Museum Display", "MuseumFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Trophy Arranger", "TrophyFrame"));

    toolsMenu.add(new JSeparator());

    toolsMenu.add(new DisplayFrameMenuItem("Mushroom Plot", "MushroomFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Familiar Trainer", "FamiliarTrainingFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Sweet Synthesis", "SynthesizeFrame"));
    toolsMenu.add(new DisplayFrameMenuItem("Modifier Maximizer", "MaximizerFrame"));

    // Add the old-school people menu.

    JMenu peopleMenu = new JMenu("People");
    this.add(peopleMenu);

    peopleMenu.add(new RelayBrowserMenuItem("Read KoLmail", "game.php?mainpane=messages"));
    peopleMenu.add(new DisplayFrameMenuItem("KoLmafia Chat", "ChatManager"));
    peopleMenu.add(new DisplayFrameMenuItem("Contact List", "ContactListFrame"));

    peopleMenu.add(new JSeparator());

    peopleMenu.add(new DisplayFrameMenuItem("Clan Manager", "ClanManageFrame"));
    peopleMenu.add(new DisplayFrameMenuItem("Send a Message", "SendMessageFrame"));
    peopleMenu.add(new RelayBrowserMenuItem("Propose a Trade", "makeoffer.php"));

    peopleMenu.add(new JSeparator());

    peopleMenu.add(new DisplayFrameMenuItem("Run a Buffbot", "BuffBotFrame"));
    peopleMenu.add(new DisplayFrameMenuItem("Purchase Buffs", "BuffRequestFrame"));

    peopleMenu.add(new JSeparator());
    peopleMenu.add(new DisplayFrameMenuItem("Request a Fax", "FaxRequestFrame"));

    // Add in common tasks menu

    JMenu travelMenu = new JMenu("Travel");
    this.add(travelMenu);

    travelMenu.add(new RestChateauMenuItem());
    travelMenu.add(new RestCampgroundMenuItem());
    travelMenu.add(new RestClanSofaMenuItem());

    travelMenu.add(new JSeparator());

    travelMenu.add(new MonsterLevelMenuItem());
    travelMenu.add(new UntinkerMenuItem());
    travelMenu.add(new WandZapMenuItem());

    travelMenu.add(new JSeparator());

    travelMenu.add(new LootHermitMenuItem());
    travelMenu.add(new LootTrapperMenuItem());
    travelMenu.add(new LootHunterMenuItem());

    // Add in automatic quest completion scripts.

    JMenu questsMenu = new JMenu("Quests");
    this.add(questsMenu);

    questsMenu.add(new InvocationMenuItem("Unlock Guild", GuildUnlockManager.class, "unlockGuild"));
    questsMenu.add(
        new InvocationMenuItem("Tavern Quest (faucet)", TavernManager.class, "locateTavernFaucet"));
    questsMenu.add(
        new InvocationMenuItem("Tavern Quest (baron)", TavernManager.class, "locateBaron"));
    questsMenu.add(
        new InvocationMenuItem("Tavern Quest (explore)", TavernManager.class, "exploreTavern"));

    questsMenu.add(new JSeparator());

    questsMenu.add(
        new InvocationMenuItem("Leaflet (No Stats)", LeafletManager.class, "leafletNoMagic"));
    questsMenu.add(
        new InvocationMenuItem("Leaflet (With Stats)", LeafletManager.class, "leafletWithMagic"));

    // Naughty Sorceress Quest Scripts follow

    questsMenu.add(new JSeparator());

    questsMenu.add(
        new InvocationMenuItem(
            "Hedge Maze (Traps)", SorceressLairManager.class, "hedgeMazeTrapsScript"));
    questsMenu.add(
        new InvocationMenuItem(
            "Hedge Maze (Gopher & Duck)", SorceressLairManager.class, "hedgeMazeGopherDuckScript"));
    questsMenu.add(
        new InvocationMenuItem(
            "Hedge Maze (Chihuahua Herd & Kiwi)",
            SorceressLairManager.class,
            "hedgeMazeChihuahuaKiwiScript"));
    questsMenu.add(
        new InvocationMenuItem(
            "Hedge Maze (Nugglets)", SorceressLairManager.class, "hedgeMazeNuggletsScript"));
    questsMenu.add(new InvocationMenuItem("Tower Door", TowerDoorManager.class, "towerDoorScript"));

    // Add script and bookmark menus, which use the
    // listener-driven static final lists.

    if (!KoLConstants.bookmarks.isEmpty()) {
      this.bookmarkMenu = new BookmarkMenu();
      this.add(this.bookmarkMenu);
    }

    this.scriptMenu = new ScriptMenu();
    this.add(this.scriptMenu);

    this.windowMenu = new WindowMenu();
    this.add(this.windowMenu);

    // Add help information for KoLmafia.  This includes
    // the additional help-oriented stuffs.

    JMenu helperMenu = new JMenu("Help");
    this.add(helperMenu);

    helperMenu.add(new ThreadedMenuItem("Copyright Notice", new LicenseDisplayListener()));
    helperMenu.add(new DebugLogMenuItem());
    helperMenu.add(new DebugLogNoteMenuItem());

    helperMenu.add(new JSeparator());

    helperMenu.add(new DisplayFrameMenuItem("Farmer's Almanac", "CalendarFrame"));
    helperMenu.add(new DisplayFrameMenuItem("Encyclopedia", "DatabaseFrame"));

    helperMenu.add(new JSeparator());

    helperMenu.add(
        new RelayBrowserMenuItem(
            "KoLmafia Thread", "http://forums.kingdomofloathing.com/vb/showthread.php?t=186011"));
    helperMenu.add(new RelayBrowserMenuItem("KoLmafia Forums", "https://kolmafia.us/"));
    helperMenu.add(
        new RelayBrowserMenuItem(
            "KoLmafia Wiki", "https://wiki.kolmafia.us/index.php?title=Main_Page"));
    helperMenu.add(
        new RelayBrowserMenuItem(
            "KoLmafia FAQ",
            "https://wiki.kolmafia.us/index.php?title=Help:Frequently_Asked_Questions"));
    helperMenu.add(
        new RelayBrowserMenuItem(
            "Unofficial Guide", "https://wiki.kolmafia.us/index.php?title=KoLmafia_Guide"));

    helperMenu.add(new JSeparator());

    helperMenu.add(
        new RelayBrowserMenuItem(
            "The KoL Wiki", "http://kol.coldfront.net/thekolwiki/index.php/Main_Page"));
    helperMenu.add(
        new InvocationMenuItem("Violet Fog Mapper", VioletFogManager.class, "showGemelliMap"));
    helperMenu.add(new InvocationMenuItem("Wumpinator", WumpusManager.class, "invokeWumpinator"));
  }

  public void dispose() {
    if (this.scriptMenu != null) {
      this.scriptMenu.dispose();
    }

    if (this.windowMenu != null) {
      this.windowMenu.dispose();
    }

    if (this.bookmarkMenu != null) {
      this.bookmarkMenu.dispose();
    }
  }
}
