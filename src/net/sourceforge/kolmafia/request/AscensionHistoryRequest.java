package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AscensionHistoryRequest extends GenericRequest
    implements Comparable<AscensionHistoryRequest> {
  private static int typeComparator = AscensionSnapshot.NORMAL;

  private static final SimpleDateFormat ASCEND_DATE_FORMAT =
      new SimpleDateFormat("MM/dd/yy", Locale.US);
  private static final Pattern FIELD_PATTERN =
      Pattern.compile("</tr><td class=small.*?</tr>", Pattern.DOTALL);
  private static final Pattern NAME_PATTERN =
      Pattern.compile("who=(\\d+)\\\" class=nounder><font color=white>(.*?)</font>");

  private final String playerName;
  private final String playerId;
  private final List<AscensionDataField> ascensionData;
  private int hardcoreCount, softcoreCount, casualCount;

  public AscensionHistoryRequest(final String playerName, final String playerId) {
    super("ascensionhistory.php");

    this.addFormField("back", "self");
    this.addFormField("who", ContactManager.getPlayerId(playerName));

    this.playerName = playerName;
    this.playerId = playerId;

    this.ascensionData = new ArrayList<>();
  }

  public static final void setComparator(final int typeComparator) {
    AscensionHistoryRequest.typeComparator = typeComparator;
  }

  @Override
  public String toString() {
    StringBuilder stringForm = new StringBuilder();
    stringForm
        .append("<tr><td><a href=\"ascensions/")
        .append(ClanManager.getURLName(this.playerName))
        .append("\"><b>");

    String name = ContactManager.getPlayerName(this.playerId);
    stringForm.append(name.equals(this.playerId) ? this.playerName : name);

    stringForm.append("</b></a></td>");
    stringForm.append("<td align=right>");
    stringForm.append(
        typeComparator == AscensionSnapshot.NORMAL
            ? this.softcoreCount
            : typeComparator == AscensionSnapshot.HARDCORE ? this.hardcoreCount : casualCount);
    stringForm.append("</td></tr>");
    return stringForm.toString();
  }

  @Override
  public int compareTo(final AscensionHistoryRequest o) {
    return o == null
        ? -1
        : typeComparator == AscensionSnapshot.NORMAL
            ? o.softcoreCount - this.softcoreCount
            : typeComparator == AscensionSnapshot.HARDCORE
                ? o.hardcoreCount - this.hardcoreCount
                : o.casualCount - this.casualCount;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  public static final void parseResponse(final String urlString, String responseText) {
    if (responseText == null
        || responseText.length() == 0
        || !urlString.startsWith("ascensionhistory.php")) {
      return;
    }

    var challengePathPoints = new HashMap<Path, Integer>();
    var challengeClassPoints = new HashMap<AscensionClass, Integer>();

    String playerName = null;
    String playerId = null;

    // Add something into familiar column if blank so later processing works
    responseText =
        responseText
            .replaceAll("<a[^>]*?>Back[^<?]</a>", "")
            .replaceAll(
                "<td></td>",
                "<td><img src=\""
                    + KoLmafia.imageServerPath()
                    + "itemimages/confused.gif\" height=30 width=30></td>");

    Matcher nameMatcher = AscensionHistoryRequest.NAME_PATTERN.matcher(responseText);
    if (nameMatcher.find()) {
      playerName = nameMatcher.group(2);
      playerId = nameMatcher.group(1);
    }

    // Only continue if looking at ourself
    if (playerId == null || !playerId.equals(KoLCharacter.getPlayerId())) {
      return;
    }

    Matcher fieldMatcher = AscensionHistoryRequest.FIELD_PATTERN.matcher(responseText);

    int lastFindIndex = 0;
    AscensionDataField lastField;

    while (fieldMatcher.find(lastFindIndex)) {
      lastFindIndex = fieldMatcher.end() - 5;

      String[] columns = AscensionHistoryRequest.extractColumns(fieldMatcher.group());

      if (columns == null) {
        continue;
      }

      lastField = new AscensionDataField(playerName, playerId, columns);

      int pointsEarned = lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;

      if (lastField.path == Path.AVATAR_OF_WEST_OF_LOATHING) {
        challengeClassPoints.merge(lastField.ascensionClass, pointsEarned, Integer::sum);
      } else {
        challengePathPoints.merge(lastField.path, pointsEarned, Integer::sum);
      }
    }

    for (Entry<Path, Integer> entry : challengePathPoints.entrySet()) {
      Path path = entry.getKey();
      int points = entry.getValue();

      path.setPoints(points);
    }

    for (Entry<AscensionClass, Integer> entry : challengeClassPoints.entrySet()) {
      int points = entry.getValue();

      final String pref;

      switch (entry.getKey()) {
        case COWPUNCHER:
          pref = "awolPointsCowpuncher";
          break;
        case BEANSLINGER:
          pref = "awolPointsBeanslinger";
          break;
        case SNAKE_OILER:
          pref = "awolPointsSnakeoiler";
          break;
        default:
          continue;
      }

      if (points > Preferences.getInteger(pref)) {
        Preferences.setInteger(pref, points);
      }
    }
  }

  @Override
  public void processResults() {
    this.responseText =
        this.responseText
            .replaceAll("<a[^>]*?>Back[^<?]</a>", "")
            .replaceAll(
                "<td></td>",
                "<td><img src=\""
                    + KoLmafia.imageServerPath()
                    + "itemimages/confused.gif\" height=30 width=30></td>");

    this.refreshFields();
  }

  private String getBackupFileData() {
    File clan = new File(KoLConstants.ROOT_LOCATION, "clan");
    if (!clan.exists()) {
      return "";
    }

    File[] resultFolders = DataUtilities.listFiles(clan);

    File backupFile = null;
    int bestMonth = 0, bestWeek = 0;
    int currentMonth, currentWeek;

    for (File resultFolder : resultFolders) {
      if (!resultFolder.isDirectory()) {
        continue;
      }

      File[] ascensionFolders = DataUtilities.listFiles(resultFolder);

      for (File ascensionFolder : ascensionFolders) {
        if (!ascensionFolder.getName().startsWith("2005")) {
          continue;
        }

        currentMonth = StringUtilities.parseInt(ascensionFolder.getName().substring(4, 6));
        currentWeek = StringUtilities.parseInt(ascensionFolder.getName().substring(8, 9));

        boolean shouldReplace;

        shouldReplace = currentMonth > bestMonth;

        if (!shouldReplace) {
          shouldReplace = currentMonth == bestMonth && currentWeek > bestWeek;
        }

        if (shouldReplace) {
          shouldReplace = currentMonth == 9 || currentMonth == 10;
        }

        if (shouldReplace) {
          File checkFile = new File(ascensionFolder, "ascensions/" + this.playerId + ".htm");
          if (checkFile.exists()) {
            backupFile = checkFile;
            bestMonth = currentMonth;
            bestWeek = currentWeek;
          }
        }
      }
    }

    if (backupFile == null) {
      return "";
    }

    try {
      BufferedReader istream = FileUtilities.getReader(backupFile);
      StringBuilder ascensionBuffer = new StringBuilder();
      String currentLine;

      while ((currentLine = istream.readLine()) != null) {
        ascensionBuffer.append(currentLine);
        ascensionBuffer.append(KoLConstants.LINE_BREAK);
      }

      return ascensionBuffer.toString();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
      return "";
    }
  }

  /**
   * Internal method used to refresh the fields of the profile request based on the response text.
   * This should be called after the response text is already retrieved.
   */
  private void refreshFields() {
    if (this.responseText == null || this.responseText.length() == 0) {
      return;
    }

    this.ascensionData.clear();
    Matcher fieldMatcher = AscensionHistoryRequest.FIELD_PATTERN.matcher(this.responseText);

    StringBuffer ascensionBuffer = new StringBuffer();
    ascensionBuffer.append(this.getBackupFileData());

    int lastFindIndex = 0;
    AscensionDataField lastField;

    if (ascensionBuffer.length() != 0) {
      int oldFindIndex = 0;
      boolean inconsistency = false;
      boolean newDataAvailable = true;
      String[] columnsNew = null;

      Matcher oldDataMatcher = AscensionHistoryRequest.FIELD_PATTERN.matcher(ascensionBuffer);
      if (!fieldMatcher.find(lastFindIndex)) {
        newDataAvailable = false;
      } else {
        lastFindIndex = fieldMatcher.end() - 5;
        columnsNew = AscensionHistoryRequest.extractColumns(fieldMatcher.group());
      }

      while (oldDataMatcher.find(oldFindIndex)) {
        oldFindIndex = oldDataMatcher.end() - 5;

        String[] columnsOld = AscensionHistoryRequest.extractColumns(oldDataMatcher.group());
        if (!newDataAvailable) {
          lastField = new AscensionDataField(this.playerName, this.playerId, columnsOld);
          this.ascensionData.add(lastField);

          switch (lastField.typeId) {
            case AscensionSnapshot.NORMAL:
              ++this.softcoreCount;
              break;
            case AscensionSnapshot.HARDCORE:
              ++this.hardcoreCount;
              break;
            case AscensionSnapshot.CASUAL:
              ++this.casualCount;
              break;
          }
        } else if (columnsNew != null && columnsNew[0].equals(columnsOld[0])) {
          if (!fieldMatcher.find(lastFindIndex)) {
            newDataAvailable = false;
          } else {
            lastFindIndex = fieldMatcher.end() - 5;
            columnsNew = AscensionHistoryRequest.extractColumns(fieldMatcher.group());
          }

          lastField = new AscensionDataField(this.playerName, this.playerId, columnsOld);
          this.ascensionData.add(lastField);

          switch (lastField.typeId) {
            case AscensionSnapshot.NORMAL:
              ++this.softcoreCount;
              break;
            case AscensionSnapshot.HARDCORE:
              ++this.hardcoreCount;
              break;
            case AscensionSnapshot.CASUAL:
              ++this.casualCount;
              break;
          }
        } else {
          lastField = new AscensionDataField(this.playerName, this.playerId, columnsOld);
          this.ascensionData.add(lastField);

          switch (lastField.typeId) {
            case AscensionSnapshot.NORMAL:
              ++this.softcoreCount;
              break;
            case AscensionSnapshot.HARDCORE:
              ++this.hardcoreCount;
              break;
            case AscensionSnapshot.CASUAL:
              ++this.casualCount;
              break;
          }

          try {
            // Subtract columns[turns] from columnsNew[turns];
            // currently, this is [5]

            inconsistency = true;
            columnsNew[5] =
                String.valueOf(
                    StringUtilities.parseInt(columnsNew[5])
                        - StringUtilities.parseInt(columnsOld[5]));

            // Subtract columns[days] from columnsNew[days];
            // currently, this is [6].  Ascensions count
            // both first day and last day, so remember to
            // add it back in.

            long timeDifference =
                AscensionHistoryRequest.ASCEND_DATE_FORMAT.parse(columnsNew[1]).getTime()
                    - AscensionHistoryRequest.ASCEND_DATE_FORMAT.parse(columnsOld[1]).getTime();

            columnsNew[6] = String.valueOf(Math.round(timeDifference / 86400000L) + 1);
          } catch (Exception e) {
            // This should not happen.  Therefore, print
            // a stack trace for debug purposes.

            StaticEntity.printStackTrace(e);
          }
        }
      }

      if (inconsistency) {
        lastField = new AscensionDataField(this.playerName, this.playerId, columnsNew);
        this.ascensionData.add(lastField);

        switch (lastField.typeId) {
          case AscensionSnapshot.NORMAL:
            ++this.softcoreCount;
            break;
          case AscensionSnapshot.HARDCORE:
            ++this.hardcoreCount;
            break;
          case AscensionSnapshot.CASUAL:
            ++this.casualCount;
            break;
        }

        lastFindIndex = fieldMatcher.end() - 5;
      }
    }

    while (fieldMatcher.find(lastFindIndex)) {
      lastFindIndex = fieldMatcher.end() - 5;

      String[] columns = AscensionHistoryRequest.extractColumns(fieldMatcher.group());

      if (columns == null) {
        continue;
      }

      lastField = new AscensionDataField(this.playerName, this.playerId, columns);
      this.ascensionData.add(lastField);

      switch (lastField.typeId) {
        case AscensionSnapshot.NORMAL:
          ++this.softcoreCount;
          break;
        case AscensionSnapshot.HARDCORE:
          ++this.hardcoreCount;
          break;
        case AscensionSnapshot.CASUAL:
          ++this.casualCount;
          break;
      }
    }
  }

  /**
   * static final method used by the clan manager in order to get an instance of a profile request
   * based on the data already known.
   */
  public static final AscensionHistoryRequest getInstance(
      final String playerName, final String playerId, final String responseText) {
    AscensionHistoryRequest instance = new AscensionHistoryRequest(playerName, playerId);

    instance.responseText = responseText;
    instance.refreshFields();

    return instance;
  }

  public String getPlayerName() {
    return this.playerName;
  }

  public String getPlayerId() {
    return this.playerId;
  }

  public void initialize() {
    if (this.responseText == null) {
      RequestThread.postRequest(this);
    }
  }

  public List<AscensionDataField> getAscensionData() {
    return this.ascensionData;
  }

  private static String[] extractColumns(String rowData) {
    rowData = rowData.replaceFirst("</tr><td.*?>", "");

    rowData = StringUtilities.globalStringDelete(rowData, "&nbsp;");
    rowData = StringUtilities.globalStringDelete(rowData, " ");

    String[] columns = rowData.split("(</?t[rd].*?>)+");

    if (columns.length < 7) {
      return null;
    }

    // These three columns now have text that would mess up parsing.

    columns[2] = KoLConstants.ANYTAG_PATTERN.matcher(columns[2]).replaceAll("");
    columns[5] = KoLConstants.ANYTAG_PATTERN.matcher(columns[5]).replaceAll("");
    columns[6] = KoLConstants.ANYTAG_PATTERN.matcher(columns[6]).replaceAll("");

    return columns;
  }

  public static class AscensionDataField implements Comparable<AscensionDataField> {
    private String playerName;
    private String playerId;
    private StringBuffer stringForm;

    private Date timestamp;
    private int level, typeId;
    private int dayCount, turnCount;
    private Path path;
    private AscensionClass ascensionClass;

    public AscensionDataField(
        final String playerName, final String playerId, final String rowData) {
      this.setData(playerName, playerId, AscensionHistoryRequest.extractColumns(rowData));
    }

    public AscensionDataField(
        final String playerName, final String playerId, final String[] columns) {
      this.setData(playerName, playerId, columns);
    }

    private void setData(final String playerName, final String playerId, final String[] columns) {
      this.playerId = playerId;
      this.playerName = ContactManager.getPlayerName(playerId);

      if (this.playerName.equals(this.playerId)) {
        this.playerName = playerName;
      }

      // The level at which the ascension took place is found
      // in the third column, or index 2 in the array.

      try {
        this.timestamp = AscensionHistoryRequest.ASCEND_DATE_FORMAT.parse(columns[1]);
        this.level = StringUtilities.parseInt(columns[2]);
      } catch (Exception e) {
        StaticEntity.printStackTrace(e);
      }

      this.turnCount = StringUtilities.parseInt(columns[5]);
      this.dayCount = StringUtilities.parseInt(columns[6]);

      if (columns.length == 9) {
        this.setCurrentColumns(columns);
      } else {
        this.setHistoricColumns(columns);
      }

      this.stringForm = new StringBuffer();
      this.stringForm
          .append("<tr><td><a href=\"ascensions/")
          .append(ClanManager.getURLName(this.playerName))
          .append("\"><b>");
      this.stringForm.append(this.playerName);
      this.stringForm.append("</b></a>&nbsp;(");
      this.stringForm.append(ascensionClass.getInitials());
      this.stringForm.append(")&nbsp;&nbsp;&nbsp;&nbsp;</td><td align=right>");
      this.stringForm.append(this.dayCount);
      this.stringForm.append("</td><td align=right>");
      this.stringForm.append(this.turnCount);
      this.stringForm.append("</td></tr>");
    }

    private void setHistoricColumns(final String[] columns) {
      // Check if any data present
      if (!columns[7].contains(",")) {
        return;
      }

      for (AscensionClass ascensionClass : AscensionClass.values()) {
        if (columns[3].startsWith(ascensionClass.getInitials())) {
          this.ascensionClass = ascensionClass;
        }
      }

      String[] path = columns[7].split(",");

      this.typeId =
          path[0].equals("Normal")
              ? AscensionSnapshot.NORMAL
              : path[0].equals("Hardcore")
                  ? AscensionSnapshot.HARDCORE
                  : path[0].equals("Casual")
                      ? AscensionSnapshot.CASUAL
                      : AscensionSnapshot.UNKNOWN_TYPE;

      String pathName = path[1];
      this.path = AscensionPath.nameToPath(pathName);
    }

    private void setCurrentColumns(final String[] columns) {
      try {
        for (AscensionClass ascensionClass : AscensionClass.values()) {
          String image = ascensionClass.getImage();
          if (image != null && columns[3].contains(image)) {
            this.ascensionClass = ascensionClass;
            break;
          }
        }

        this.typeId =
            columns[8].contains("hardcore")
                ? AscensionSnapshot.HARDCORE
                : columns[8].contains("beanbag")
                    ? AscensionSnapshot.CASUAL
                    : AscensionSnapshot.NORMAL;

        for (Path path : Path.values()) {
          String image = path.getImage();
          if (image != null && columns[8].contains(image)) {
            this.path = path;
            break;
          }
        }

        if (this.path == null) {
          this.path = Path.NONE;
        }
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }

    public String getDateAsString() {
      return ProfileRequest.OUTPUT_FORMAT.format(this.timestamp);
    }

    public int getTypeId() {
      return this.typeId;
    }

    public Path getPath() {
      return this.path;
    }

    public AscensionClass getAscensionClass() {
      return this.ascensionClass;
    }

    public int getAge() {
      long ascensionDate = this.timestamp.getTime();
      float difference = System.currentTimeMillis() - ascensionDate;
      return Math.round((difference / (1000 * 60 * 60 * 24)));
    }

    @Override
    public String toString() {
      return this.stringForm.toString();
    }

    @Override
    public boolean equals(final Object o) {
      return o instanceof AscensionDataField
          && this.playerId.equals(((AscensionDataField) o).playerId);
    }

    @Override
    public int hashCode() {
      return this.playerId != null ? this.playerId.hashCode() : 0;
    }

    public boolean matchesFilter(
        final int typeFilter,
        final Path pathFilter,
        final AscensionClass classFilter,
        final int maxAge) {
      return (typeFilter == AscensionSnapshot.NO_FILTER || typeFilter == this.typeId)
          && (pathFilter == null || pathFilter == this.path)
          && (classFilter == null || classFilter == this.ascensionClass)
          && (maxAge == 0 || maxAge >= this.getAge());
    }

    public boolean matchesFilter(
        final int typeFilter, final Path pathFilter, final AscensionClass classFilter) {
      return (typeFilter == AscensionSnapshot.NO_FILTER || typeFilter == this.typeId)
          && (pathFilter == null || pathFilter == this.path)
          && (classFilter == null || classFilter == this.ascensionClass);
    }

    @Override
    public int compareTo(final AscensionDataField o) {
      if (o == null) {
        return -1;
      }

      // First, compare the number of days between
      // ascension runs.

      int dayDifference = this.dayCount - o.dayCount;
      if (dayDifference != 0) {
        return dayDifference;
      }

      // Next, compare the number of turns it took
      // in order to complete the ascension.

      int turnDifference = this.turnCount - o.turnCount;
      if (turnDifference != 0) {
        return turnDifference;
      }

      // Earlier ascensions take priority.  Therefore,
      // compare the timestamp.  Later, this will also
      // take the 60-day sliding window into account.

      if (this.timestamp.before(o.timestamp)) {
        return -1;
      }
      if (this.timestamp.after(o.timestamp)) {
        return 1;
      }

      // If it still is equal, then check the difference
      // in levels, and return that -- effectively, if all
      // comparable elements are the same, then they are equal.

      return this.level - o.level;
    }
  }
}
