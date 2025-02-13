package net.sourceforge.kolmafia.persistence;

import static net.sourceforge.kolmafia.preferences.Preferences.setBoolean;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.FaxRequestFrame;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FaxBotDatabase {
  private static boolean isInitialized = false;
  private static boolean faxBotError = false;
  private static String faxBotErrorMessage = "";

  // List of bots from faxbots.txt
  public static final ArrayList<BotData> botData = new ArrayList<>();

  // List of faxbots named in config files.
  public static final ArrayList<FaxBot> faxbots = new ArrayList<>();

  private FaxBotDatabase() {}

  public static void reconfigure() {
    FaxBotDatabase.isInitialized = false;
    FaxBotDatabase.configure();
  }

  public static void configure() {
    if (FaxBotDatabase.isInitialized) {
      return;
    }

    FaxBotDatabase.readFaxbotConfig();
    FaxBotDatabase.configureFaxBots();
  }

  // visible for testing.  used to force download and read.
  static void resetInitialization() {
    isInitialized = false;
  }

  private static void readFaxbotConfig() {
    FaxBotDatabase.botData.clear();

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("faxbots.txt", KoLConstants.FAXBOTS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length > 1) {
          FaxBotDatabase.botData.add(new BotData(data[0].trim().toLowerCase(), data[1].trim()));
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void configureFaxBots() {
    KoLmafia.updateDisplay("Configuring faxable monsters.");

    FaxBotDatabase.faxbots.clear();

    for (BotData data : FaxBotDatabase.botData) {
      FaxBotDatabase.configureFaxBot(data);
    }

    KoLmafia.updateDisplay("Faxable monster lists fetched.");
    FaxBotDatabase.isInitialized = true;
  }

  private static void configureFaxBot(final BotData data) {
    FaxBotDatabase.faxBotError = false;
    FaxBotDatabase.faxBotErrorMessage = "";
    KoLmafia.forceContinue();

    RequestThread.postRequest(new DynamicBotFetcher(data));

    if (FaxBotDatabase.faxBotError) {
      KoLmafia.updateDisplay(
          MafiaState.ABORT,
          "Could not load " + data.name + " configuration from \"" + data.URL + "\"");
      RequestLogger.printLine(FaxBotDatabase.faxBotErrorMessage);
    }
  }

  public static FaxBot getFaxbot(final int i) {
    return FaxBotDatabase.faxbots.get(Math.max(0, i % faxbots.size()));
  }

  public static FaxBot getFaxbot(final String botName) {
    for (FaxBot bot : faxbots) {
      if (bot == null) {
        continue;
      }
      if (bot.name.equalsIgnoreCase(botName)) {
        return bot;
      }
    }

    return null;
  }

  public static List<FaxBot> getSortedFaxbots() {
    // Get preferred faxbot or null
    FaxBot preferred = getFaxbot(Preferences.getString("lastSuccessfulFaxbot"));
    // Use original list
    List<FaxBot> list = faxbots;

    // If faxbot is in the list and isn't the first entry
    if (faxbots.indexOf(preferred) > 0) {
      // Clone list to maintain original faxbot order
      list = new ArrayList<>(faxbots);

      // Re-add at first index of list
      list.remove(preferred);
      list.add(0, preferred);
    }

    return list;
  }

  public static class BotData {
    public final String name;
    public final String URL;

    public BotData(final String name, final String URL) {
      this.name = name;
      this.URL = URL;
    }
  }

  public static class FaxBot implements Comparable<FaxBot> {
    // Who is this bot?
    private final String name;
    private final int playerId;

    // What monsters does it serve?
    public final SortedListModel<Monster> monsters = new SortedListModel<>();

    // Lists derived from the list of monsters
    private final LockableListModel<String> categories = new LockableListModel<>();
    private List<LockableListModel<Monster>> monstersByCategory = new ArrayList<>(0);

    private final Map<Integer, Monster> monsterByMonsterId = new HashMap<>();
    private final Map<String, Monster> monsterByActualName = new HashMap<>();
    private final Map<String, Monster> monsterByCommand = new HashMap<>();
    private String[] canonicalCommands;

    public FaxBot(final String name, final String playerId) {
      this(name, StringUtilities.parseInt(playerId));
    }

    public FaxBot(final String name, final int playerId) {
      this.name = name;
      this.playerId = playerId;
    }

    public String getName() {
      return this.name;
    }

    public int getPlayerId() {
      return this.playerId;
    }

    public LockableListModel<String> getCategories() {
      return this.categories;
    }

    public List<LockableListModel<Monster>> getMonstersByCategory() {
      return this.monstersByCategory;
    }

    public boolean request(final MonsterData monster) {
      String name = this.getName();

      if (name == null) {
        return false;
      }

      int monsterId = monster.getId();
      Monster monsterObject = this.monsterByMonsterId.get(monsterId);
      if (monsterObject == null) {
        return false;
      }

      if (!FaxRequestFrame.isBotOnline(name)) {
        return false;
      }

      return FaxRequestFrame.requestFax(name, monsterObject, false);
    }

    public Monster getMonsterByMonsterId(final int monsterId) {
      return this.monsterByMonsterId.get(monsterId);
    }

    public Monster getMonsterByActualName(final String actualName) {
      return this.monsterByActualName.get(StringUtilities.getCanonicalName(actualName));
    }

    public Monster getMonsterByCommand(final String command) {
      return this.monsterByCommand.get(StringUtilities.getCanonicalName(command));
    }

    public void addMonsters(final List<Monster> monsters) {
      // Build the list of monsters and derived mappings
      this.monsters.clear();
      this.monsterByMonsterId.clear();
      this.monsterByActualName.clear();
      this.monsterByCommand.clear();

      SortedListModel<String> tempCategories = new SortedListModel<>();
      for (Monster monster : monsters) {
        MonsterData data = monster.getMonster();
        if (data == null) {
          continue;
        }

        int monsterId = data.getId();
        if (this.monsterByMonsterId.containsKey(monsterId)) {
          continue;
        }

        this.monsters.add(monster);
        String category = monster.category;
        if (!category.isEmpty()
            && !category.equalsIgnoreCase("none")
            && !tempCategories.contains(category)) {
          tempCategories.add(category);
        }

        // Build monsterId / actual name / command lookup
        this.monsterByMonsterId.put(monsterId, monster);
        String canonicalName = StringUtilities.getCanonicalName(monster.actualName);
        this.monsterByActualName.put(canonicalName, monster);
        String canonicalCommand = StringUtilities.getCanonicalName(monster.command);
        this.monsterByCommand.put(canonicalCommand, monster);
      }

      // Create the canonical command list
      Set<String> commands = this.monsterByCommand.keySet();
      String[] array = new String[0];
      this.canonicalCommands = commands.toArray(array);
      Arrays.sort(this.canonicalCommands);

      this.categories.clear();
      this.categories.add("All Monsters");
      this.categories.addAll(tempCategories);

      // Make one list for each category
      this.monstersByCategory = new ArrayList<>(this.categories.size());
      for (int i = 0; i < this.categories.size(); ++i) {
        String category = categories.get(i);
        SortedListModel<Monster> model = new SortedListModel<>();
        this.monstersByCategory.add(model);
        for (Monster monster : monsters) {
          if (i == 0 || category.equals(monster.category)) {
            model.add(monster);
          }
        }
      }
    }

    public List<String> findMatchingCommands(final String command) {
      String canonical = StringUtilities.getCanonicalName(command);
      return StringUtilities.getMatchingNames(this.canonicalCommands, canonical);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof FaxBot that)) {
        return false;
      }

      return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return this.name != null ? this.name.hashCode() : 0;
    }

    @Override
    public int compareTo(final FaxBot o) {
      if (!(o instanceof FaxBot)) {
        return -1;
      }

      return this.name.compareTo(o.name);
    }
  }

  private static final Pattern MONSTER_COMMENT_PATTERN =
      Pattern.compile("<!-- monsterid: (\\d+) -->");
  private static final Pattern MONSTER_ID_PATTERN = Pattern.compile("\\[(\\d+)\\]");

  public static class Monster implements Comparable<Monster> {
    // The specific monster that is available from a FaxBot
    private final MonsterData monster;

    // The display name, presented to the user in the GUI
    private final String name;

    // The Faxbot's idea of what the actual name is - to KoLmafia
    private final String actualName;

    // The command that the FaxBot expects to request this monster
    private final String command;

    // The Faxbot's categorization of this monster, if any
    private final String category;

    private final String stringForm;
    private final String lowerCaseStringForm;

    public Monster(
        final String name, final String actualName, final String command, final String category) {
      this.command = command;
      this.category = category;
      this.monster = this.deriveMonster(command, actualName);
      if (monster != null) {
        // Excellent. We know the monster - including monsterId.
        String displayName = monster.getName();
        this.name = displayName;
        this.actualName = displayName;
      } else {
        // Rats. The FaxBot did not give us a usable monsterId.
        this.name = CharacterEntities.unescape(name);
        this.actualName = CharacterEntities.unescape(actualName);
      }
      this.stringForm = this.name + " [" + command + "]";
      this.lowerCaseStringForm = this.stringForm.toLowerCase();
    }

    private MonsterData deriveMonster(String command, String actualName) {
      // Since monster names in KoL can be ambiguous, FaxBots that want
      // to offer several versions of a monster need to disambiguate.
      //
      // The best way is to include the monsterId in the command used to request it.

      Matcher idMatcher = MONSTER_ID_PATTERN.matcher(command);
      if (idMatcher.find()) {
        int monsterId = Integer.valueOf(idMatcher.group(1));
        return MonsterDatabase.findMonsterById(monsterId);
      }

      Matcher commentMatcher = MONSTER_COMMENT_PATTERN.matcher(CharacterEntities.unescape(command));
      if (commentMatcher.find()) {
        int monsterId = Integer.valueOf(commentMatcher.group(1));
        return MonsterDatabase.findMonsterById(monsterId);
      }

      // An alternative is to use KoLmafia's disambiguated name as the actualName.
      String name = CharacterEntities.unescape(actualName);
      return MonsterDatabase.findMonster(name, false, true);
    }

    public MonsterData getMonster() {
      return this.monster;
    }

    public String getName() {
      return this.name;
    }

    public String getActualName() {
      return this.actualName;
    }

    public String getCommand() {
      return this.command;
    }

    public String getCategory() {
      return this.category;
    }

    @Override
    public String toString() {
      return this.stringForm;
    }

    public String toLowerCaseString() {
      return this.lowerCaseStringForm;
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Monster that)) {
        return false;
      }

      return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return this.name != null ? this.name.hashCode() : 0;
    }

    @Override
    public int compareTo(final Monster o) {
      if (!(o instanceof Monster)) {
        return -1;
      }

      return this.name.compareToIgnoreCase(o.name);
    }
  }

  private static class DynamicBotFetcher implements Runnable {
    private final BotData data;

    public DynamicBotFetcher(final BotData data) {
      this.data = data;
    }

    @Override
    public void run() {
      // Start with a clean slate
      FaxBotDatabase.faxBotError = false;
      FaxBotDatabase.faxBotErrorMessage = "";
      KoLmafia.forceContinue();

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      Document dom = null;

      try {
        File local = new File(KoLConstants.DATA_LOCATION, this.data.name + ".xml");
        String beforeHash = computeHash(local);
        FileUtilities.downloadFile(this.data.URL, local, true);
        String afterHash = computeHash(local);
        if (!(beforeHash.equals(afterHash))) {
          setBoolean("_faxDataChanged", true);
        }

        // Get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();

        // Parse using builder to get DOM
        // representation of the XML file
        dom = db.parse(local);
      } catch (ParserConfigurationException pce) {
        FaxBotDatabase.faxBotErrorMessage = pce.getMessage();
      } catch (SAXException se) {
        FaxBotDatabase.faxBotErrorMessage = se.getMessage();
      } catch (IOException ioe) {
        FaxBotDatabase.faxBotErrorMessage = ioe.getMessage();
      }

      if (dom == null) {
        FaxBotDatabase.faxBotError = true;
        return;
      }

      Element doc = dom.getDocumentElement();

      // Get a nodelist of bots
      ArrayList<FaxBot> bots = new ArrayList<>();
      NodeList bl = doc.getElementsByTagName("botdata");
      if (bl != null) {
        for (int i = 0; i < bl.getLength(); i++) {
          Element el = (Element) bl.item(i);
          FaxBot fb = getFaxBot(el);
          bots.add(fb);
        }
      }

      // Get a nodelist of monsters
      NodeList fl = doc.getElementsByTagName("monsterdata");
      ArrayList<Monster> monsters = new ArrayList<>();
      if (fl != null) {
        for (int i = 0; i < fl.getLength(); i++) {
          Element el = (Element) fl.item(i);
          Monster monster = getMonster(el);
          if (monster != null) {
            monsters.add(monster);
          }
        }
      }

      // For each bot, add available monsters
      for (FaxBot bot : bots) {
        bot.addMonsters(monsters);
      }

      // Add the bots to the list of available bots
      FaxBotDatabase.faxbots.addAll(bots);
    }

    private static final String NOHASH = "Problem with hash";

    private static String computeHash(File file) {
      String checksum;
      try {
        byte[] data = Files.readAllBytes(file.toPath());
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        checksum = HexFormat.of().formatHex(hash);
      } catch (Exception e) {
        checksum = NOHASH;
      }
      return checksum;
    }

    private FaxBot getFaxBot(Element el) {
      String name = getTextValue(el, "name");
      String playerId = getTextValue(el, "playerid");
      ContactManager.registerPlayerId(name, playerId);
      KoLmafia.updateDisplay("Configuring " + name + " (" + playerId + ")");
      return new FaxBot(name, playerId);
    }

    private Monster getMonster(Element el) {
      String monster = getTextValue(el, "name");
      if (monster.isEmpty() || monster.equals("none")) {
        return null;
      }
      String actualMonster = getTextValue(el, "actual_name");
      if (actualMonster.isEmpty()) {
        return null;
      }
      String command = getTextValue(el, "command");
      if (command.isEmpty()) {
        return null;
      }
      String category = getTextValue(el, "category");
      return new Monster(monster, actualMonster, command, category);
    }

    private String getTextValue(Element ele, String tagName) {
      NodeList nl = ele.getElementsByTagName(tagName);
      if (nl != null && nl.getLength() > 0) {
        Element el = (Element) nl.item(0);
        return el.getFirstChild().getNodeValue();
      }

      return "";
    }
  }
}
