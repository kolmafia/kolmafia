package net.sourceforge.kolmafia.request;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ProfileRequest extends GenericRequest implements Comparable<ProfileRequest> {
  private static final Pattern DATA_PATTERN = Pattern.compile("<td.*?>(.*?)</td>");
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");
  private static final Pattern CLAN_ID_PATTERN =
      Pattern.compile(
          "Clan: <b><a class=nounder href=\"showclan\\.php\\?whichclan=(\\d+)\">(.*?)</a>");
  private static final SimpleDateFormat INPUT_FORMAT =
      new SimpleDateFormat("MMMM d, yyyy", Locale.US);
  public static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.US);

  private final String playerName;
  private String playerId;
  private Integer playerLevel;
  private boolean isHardcore;
  private String restriction;
  private Integer currentMeat;
  private Integer turnsPlayed, currentRun;
  private String classType;

  private Date created, lastLogin;
  private String food, drink;
  private Integer ascensionCount, pvpRank, karma;
  private Integer muscle, mysticism, moxie;
  private String title, rank;

  private String clanName;
  private int clanId;

  private int equipmentPower;

  public ProfileRequest(final String playerName) {
    super("showplayer.php");

    if (playerName.startsWith("#")) {
      this.playerId = playerName.substring(1);
      this.playerName = ContactManager.getPlayerName(this.playerId);
    } else {
      this.playerName = playerName;
      this.playerId = ContactManager.getPlayerId(playerName);
    }

    this.addFormField("who", this.playerId);

    this.muscle = 0;
    this.mysticism = 0;
    this.moxie = 0;
    this.karma = 0;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  /**
   * Internal method used to refresh the fields of the profile request based on the response text.
   * This should be called after the response text is already retrieved.
   */
  private void refreshFields() {
    // Nothing to refresh if no text
    if (this.responseText == null || this.responseText.length() == 0) {
      return;
    }

    this.isHardcore = this.responseText.contains("<b>(Hardcore)</b></td>");

    // This is a massive replace which makes the profile easier to
    // parse and re-represent inside of editor panes.

    String cleanHTML = this.responseText.replaceAll("><", "").replaceAll("<.*?>", "\n");
    StringTokenizer st = new StringTokenizer(cleanHTML, "\n");

    String token = st.nextToken();

    this.playerLevel = 0;
    this.classType = "Recent Ascension";
    this.currentMeat = 0;
    this.ascensionCount = 0;
    this.turnsPlayed = 0;
    this.created = new Date();
    this.lastLogin = new Date();
    this.food = "none";
    this.drink = "none";
    this.pvpRank = 0;

    if (cleanHTML.contains("\nClass:")) { // has custom title
      while (!st.nextToken().startsWith(" (#")) {}
      String title = st.nextToken(); // custom title, may include level
      // Next token will be one of:
      //	(Level n), if the custom title doesn't include the level
      //	(In Ronin) or possibly similar messages
      //	Class:,	if neither of the above applies
      token = st.nextToken();
      if (token.startsWith("(Level")) {
        this.playerLevel = StringUtilities.parseInt(token.substring(6).trim());
      } else { // Must attempt to parse the level out of the custom title.
        // This is inherently inaccurate, since the title can contain other digits,
        // before, after, or adjacent to the level.
        Matcher m = ProfileRequest.NUMERIC_PATTERN.matcher(title);
        if (m.find() && m.group().length() < 5) {
          this.playerLevel = StringUtilities.parseInt(m.group());
        }
      }

      while (!token.startsWith("Class")) {
        token = st.nextToken();
      }
      token = st.nextToken();
      AscensionClass ascensionClass = AscensionClass.find(token.trim());
      this.classType = ascensionClass == null ? token : ascensionClass.getName();
    } else { // no custom title
      if (!cleanHTML.contains("Level")) {
        return;
      }

      while (!token.contains("Level")) {
        token = st.nextToken();
      }

      this.playerLevel = StringUtilities.parseInt(token.substring(5).trim());

      token = st.nextToken();
      AscensionClass ascensionClass = AscensionClass.find(token.trim());
      this.classType = ascensionClass == null ? token : ascensionClass.getName();
    }

    if (cleanHTML.contains("\nAscensions") && cleanHTML.contains("\nPath")) {
      while (!st.nextToken().startsWith("Path")) {}
      this.restriction = st.nextToken().trim();
    } else {
      this.restriction = "No-Path";
    }

    if (cleanHTML.contains("\nMeat:")) {
      while (!st.nextToken().startsWith("Meat")) {}
      this.currentMeat = StringUtilities.parseInt(st.nextToken().trim());
    }

    if (cleanHTML.contains("\nAscensions")) {
      while (!st.nextToken().startsWith("Ascensions")) {}
      st.nextToken();
      this.ascensionCount = StringUtilities.parseInt(st.nextToken().trim());
    } else {
      this.ascensionCount = 0;
    }

    while (!st.nextToken().startsWith("Turns")) {}
    this.turnsPlayed = StringUtilities.parseInt(st.nextToken().trim());

    if (cleanHTML.contains("\nAscensions")) {
      while (!st.nextToken().startsWith("Turns")) {}
      this.currentRun = StringUtilities.parseInt(st.nextToken().trim());
    } else {
      this.currentRun = this.turnsPlayed;
    }

    String dateString = null;
    while (!st.nextToken().startsWith("Account")) {}
    try {
      dateString = st.nextToken().trim();
      this.created = ProfileRequest.INPUT_FORMAT.parse(dateString);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e, "Could not parse date \"" + dateString + "\"");
      this.created = new Date();
    }

    while (!st.nextToken().startsWith("Last")) {}

    try {
      dateString = st.nextToken().trim();
      this.lastLogin = ProfileRequest.INPUT_FORMAT.parse(dateString);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e, "Could not parse date \"" + dateString + "\"");
      this.lastLogin = this.created;
    }

    if (cleanHTML.contains("\nFavorite Food")) {
      while (!st.nextToken().startsWith("Favorite")) {}
      this.food = st.nextToken().replaceFirst("\\([\\d,]+\\)", "").trim();
    } else {
      this.food = "none";
    }

    if (cleanHTML.contains("\nFavorite Booze")) {
      while (!st.nextToken().startsWith("Favorite")) {}
      this.drink = st.nextToken().replaceFirst("\\([\\d,]+\\)", "").trim();
    } else {
      this.drink = "none";
    }

    if (cleanHTML.contains("\nFame")) {
      while (!st.nextToken().startsWith("Fame")) {}
      this.pvpRank = StringUtilities.parseInt(st.nextToken().trim());
    } else {
      this.pvpRank = 0;
    }

    this.equipmentPower = 0;
    if (cleanHTML.contains("\nEquipment")) {
      while (!st.nextToken().startsWith("Equipment")) {}

      int itemId = -1;
      while (st.hasMoreTokens()
          && EquipmentDatabase.contains(itemId = ItemDatabase.getItemId(token = st.nextToken()))) {
        switch (ItemDatabase.getConsumptionType(itemId)) {
          case HAT:
          case PANTS:
          case SHIRT:
            this.equipmentPower += EquipmentDatabase.getPower(itemId);
            break;
        }
      }
    }

    if (cleanHTML.contains("\nClan")) {
      Matcher m = CLAN_ID_PATTERN.matcher(this.responseText);
      if (m.find()) {
        this.clanId = StringUtilities.parseInt(m.group(1));
        this.clanName = m.group(2);
      }
    } else {
      this.clanId = -1;
      this.clanName = "";
    }

    // If we're looking at our own profile, update ClanManager
    if (this.playerId.equals(KoLCharacter.getPlayerId())) {
      ClanManager.setClanId(this.clanId);
      ClanManager.setClanName(this.clanName);
    }

    if (cleanHTML.contains("\nTitle")) {
      while (!token.startsWith("Title")) {
        token = st.nextToken();
      }

      this.title = st.nextToken();
    }
  }

  /**
   * static final method used by the clan manager in order to get an instance of a profile request
   * based on the data already known.
   */
  public static final ProfileRequest getInstance(
      final String playerName,
      final String playerId,
      final String playerLevel,
      final String responseText,
      final String rosterRow) {
    ProfileRequest instance = new ProfileRequest(playerName);

    instance.playerId = playerId;

    // First, initialize the level field for the
    // current player.

    if (playerLevel == null) {
      instance.playerLevel = 0;
    } else {
      instance.playerLevel = Integer.valueOf(playerLevel);
    }

    // Next, refresh the fields for this player.
    // The response text should be copied over
    // before this happens.

    instance.responseText = responseText;
    instance.refreshFields();

    // Next, parse out all the data in the
    // row of the detail roster table.

    if (rosterRow == null) {
      instance.muscle = 0;
      instance.mysticism = 0;
      instance.moxie = 0;

      instance.rank = "";
      instance.karma = 0;
    } else {
      Matcher dataMatcher = ProfileRequest.DATA_PATTERN.matcher(rosterRow);

      // The name of the player occurs in the first
      // field of the table.  Because you already
      // know the name of the player, this can be
      // arbitrarily skipped.

      dataMatcher.find();

      // At some point the player class was added to the table.  Skip over it.

      dataMatcher.find();

      // The player's three primary stats appear in
      // the next three fields of the table.

      dataMatcher.find();
      instance.muscle = StringUtilities.parseInt(dataMatcher.group(1));

      dataMatcher.find();
      instance.mysticism = StringUtilities.parseInt(dataMatcher.group(1));

      dataMatcher.find();
      instance.moxie = StringUtilities.parseInt(dataMatcher.group(1));

      // The next field contains the total power,
      // and since this is calculated, it can be
      // skipped in data retrieval.

      dataMatcher.find();

      // The next three fields contain the ascension
      // count, number of hardcore runs, and their
      // pvp ranking.

      dataMatcher.find();
      dataMatcher.find();
      dataMatcher.find();

      // Next is the player's rank inside of this clan.
      // Title was removed, so ... not visible here.

      dataMatcher.find();
      instance.rank = dataMatcher.group(1);

      // The last field contains the total karma
      // accumulated by this player.

      dataMatcher.find();
      instance.karma = StringUtilities.parseInt(dataMatcher.group(1));
    }

    return instance;
  }

  public void initialize() {
    if (this.responseText == null) {
      RequestThread.postRequest(this);
    }
  }

  public String getPlayerName() {
    return this.playerName;
  }

  public String getPlayerId() {
    return this.playerId;
  }

  public String getClanName() {
    this.initialize();
    return this.clanName;
  }

  public int getClanId() {
    this.initialize();
    return this.clanId;
  }

  public boolean isHardcore() {
    this.initialize();
    return this.isHardcore;
  }

  public String getRestriction() {
    this.initialize();
    return this.restriction;
  }

  public String getClassType() {
    if (this.classType == null) {
      this.initialize();
    }

    return this.classType;
  }

  public Integer getPlayerLevel() {
    if (this.playerLevel == null || this.playerLevel.intValue() == 0) {
      this.initialize();
    }

    return this.playerLevel;
  }

  public Integer getCurrentMeat() {
    this.initialize();
    return this.currentMeat;
  }

  public Integer getTurnsPlayed() {
    this.initialize();
    return this.turnsPlayed;
  }

  public Integer getCurrentRun() {
    this.initialize();
    return this.currentRun;
  }

  public Date getLastLogin() {
    this.initialize();
    return this.lastLogin;
  }

  public Date getCreation() {
    this.initialize();
    return this.created;
  }

  public String getCreationAsString() {
    this.initialize();
    return ProfileRequest.OUTPUT_FORMAT.format(this.created);
  }

  public String getLastLoginAsString() {
    this.initialize();
    return ProfileRequest.OUTPUT_FORMAT.format(this.lastLogin);
  }

  public String getFood() {
    this.initialize();
    return this.food;
  }

  public String getDrink() {
    this.initialize();
    return this.drink;
  }

  public Integer getPvpRank() {
    if (this.pvpRank == null || this.pvpRank.intValue() == 0) {
      this.initialize();
    }

    return this.pvpRank;
  }

  public Integer getMuscle() {
    this.initialize();
    return this.muscle;
  }

  public Integer getMysticism() {
    this.initialize();
    return this.mysticism;
  }

  public Integer getMoxie() {
    this.initialize();
    return this.moxie;
  }

  public Integer getPower() {
    this.initialize();
    return this.muscle.intValue() + this.mysticism.intValue() + this.moxie.intValue();
  }

  public Integer getEquipmentPower() {
    this.initialize();
    return this.equipmentPower;
  }

  public String getTitle() {
    this.initialize();
    return this.title != null ? this.title : ClanManager.getTitle(this.playerName);
  }

  public String getRank() {
    this.initialize();
    return this.rank;
  }

  public Integer getKarma() {
    this.initialize();
    return this.karma;
  }

  public Integer getAscensionCount() {
    this.initialize();
    return this.ascensionCount;
  }

  private static final Pattern GOBACK_PATTERN =
      Pattern.compile(
          "https?://www\\.kingdomofloathing\\.com/ascensionhistory\\.php?back=self&who=([\\d]+)");

  @Override
  public void processResults() {
    Matcher dataMatcher = ProfileRequest.GOBACK_PATTERN.matcher(this.responseText);
    if (dataMatcher.find()) {
      this.responseText =
          dataMatcher.replaceFirst(
              "../ascensions/"
                  + ClanManager.getURLName(ContactManager.getPlayerName(dataMatcher.group(1))));
    }

    this.refreshFields();
  }

  @Override
  public int compareTo(final ProfileRequest o) {
    if (!(o instanceof ProfileRequest)) {
      return -1;
    }

    if (this.getPvpRank().intValue() != o.getPvpRank().intValue()) {
      return this.getPvpRank().intValue() - o.getPvpRank().intValue();
    }

    return this.getPlayerLevel().intValue() - o.getPlayerLevel().intValue();
  }

  private static final Pattern WHO_PATTERN = Pattern.compile("who=(\\d+)");

  public static int getWho(final String urlString) {
    Matcher matcher = ProfileRequest.WHO_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  private static final Pattern EQUIPMENT_PATTERN =
      Pattern.compile("<center>Equipment:</center>(<table>.*?</table>)");
  private static final Pattern FAMILIAR_PATTERN =
      Pattern.compile("<p>Familiar:.*?(<table>.*?</table>)");

  public static void parseResponse(String location, String responseText) {
    int who = ProfileRequest.getWho(location);
    if (who == 1) { // if we're looking at Jick's profile
      // and we have an empty jar
      if (InventoryManager.hasItem(ItemPool.PSYCHOANALYTIC_JAR)
          // and we haven't already filled a jar
          && !Preferences.getBoolean("_psychoJarFilled")) {
        Preferences.setString(
            "_jickJarAvailable", Boolean.toString(responseText.contains("psychoanalytic jar")));
      }
      if (responseText.contains("jar of psychoses (Jick)")) {
        ResultProcessor.processItem(
            false, "You acquire an item:", ItemPool.get(ItemPool.JICK_JAR), null);
      }
    }

    if (location.contains("action=crossthestreams")
        && (responseText.contains("creating an intense but localized nuclear reaction")
            || responseText.contains("You've already crossed the streams today"))) {
      Preferences.setBoolean("_streamsCrossed", true);
    }

    // Look for new items in equipment
    Matcher matcher = ProfileRequest.EQUIPMENT_PATTERN.matcher(responseText);
    if (matcher.find()) {
      ItemDatabase.parseNewItems(matcher.group(1));
    }

    // Look for new item on current familiar
    matcher = ProfileRequest.FAMILIAR_PATTERN.matcher(responseText);
    if (matcher.find()) {
      ItemDatabase.parseNewItems(matcher.group(1));
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("showplayer.php")) {
      return false;
    }

    int who = ProfileRequest.getWho(urlString);
    if (who == 1) { // if we're looking at Jick's profile
      if (urlString.contains("action=jung") && urlString.contains("whichperson=jick")) {
        String message = "Psychoanalyzing Jick";
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog(message);
        return true;
      }
    }

    // No need to log looking at player profiles
    return true;
  }
}
