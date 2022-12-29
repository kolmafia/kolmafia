package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanLogRequest extends GenericRequest {
  private static final SimpleDateFormat STASH_FORMAT =
      new SimpleDateFormat("MM/dd/yy, hh:mma", Locale.US);

  private static final String STASH_ADD = "add";
  private static final String STASH_TAKE = "take";
  private static final String WAR_BATTLE = "warfare";

  private static final String CLAN_WHITELIST = "whitelist";
  private static final String CLAN_ACCEPT = "accept";
  private static final String CLAN_LEAVE = "leave";
  private static final String CLAN_BOOT = "boot";

  private static final String TIME_REGEX = "(\\d\\d/\\d\\d/\\d\\d, \\d\\d:\\d\\d[AP]M)";
  private static final String PLAYER_REGEX =
      "<a class=nounder href='showplayer.php\\?who=\\d+'>([^<]*?) \\(#\\d+\\)</a>";

  private static final Pattern WAR_PATTERN =
      Pattern.compile(
          ClanLogRequest.TIME_REGEX + ": ([^<]*?) launched an attack against (.*?)\\.<br>");
  private static final Pattern LOGENTRY_PATTERN =
      Pattern.compile("\t<li class=\"(.*?)\">(.*?): (.*?)</li>");

  private final Map<String, List<StashLogEntry>> stashMap = new TreeMap<>();

  public ClanLogRequest() {
    super("clan_log.php");
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    KoLmafia.updateDisplay("Retrieving clan stash log...");

    File file =
        new File(KoLConstants.ROOT_LOCATION, "clan/" + ClanManager.getClanId() + "/stashlog.htm");

    this.loadPreviousData(file);
    super.run();

    KoLmafia.updateDisplay("Stash log retrieved.");

    // First, process all additions to the clan stash.
    // These are designated with the word "added to".

    this.handleItems(true);

    // Next, process all the removals from the clan stash.
    // These are designated with the word "took from".

    this.handleItems(false);

    // Next, process all the clan warfare log entries.
    // Though grouping by player isn't very productive,
    // KoLmafia is meant to show a historic history, and
    // showing it by player may prove enlightening.

    this.handleBattles();

    // Now, handle all of the administrative-related
    // things in the clan.

    this.handleAdmin(
        ClanLogRequest.CLAN_WHITELIST,
        "was accepted into the clan \\(whitelist\\)",
        "",
        "auto-accepted through whitelist");
    this.handleAdmin(ClanLogRequest.CLAN_ACCEPT, "accepted", " into the clan", "accepted by ");
    this.handleAdmin(ClanLogRequest.CLAN_LEAVE, "left the clan", "", "left clan");
    this.handleAdmin(ClanLogRequest.CLAN_BOOT, "booted", "", "booted by ");

    this.saveCurrentData(file);
  }

  private void loadPreviousData(final File file) {
    this.stashMap.clear();

    List<StashLogEntry> entryList = null;
    StashLogEntry entry = null;

    if (file.exists()) {
      try {
        String currentMember = "";
        BufferedReader istream = FileUtilities.getReader(file);
        String line;

        boolean startReading = false;

        while ((line = istream.readLine()) != null) {
          if (startReading) {
            if (line.startsWith(" ")) {
              currentMember = line.substring(1, line.length() - 1);
              entryList = this.stashMap.computeIfAbsent(currentMember, k -> new ArrayList<>());
            } else if (line.length() > 0 && !line.startsWith("<")) {
              entry = new StashLogEntry(line);
              if (!entryList.contains(entry)) {
                entryList.add(entry);
              }
            }
          } else if (line.equals("<!-- Begin Stash Log: Do Not Modify Beyond This Point -->")) {
            startReading = true;
          }
        }

        istream.close();
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }
  }

  private void saveCurrentData(final File file) {
    String[] members = new String[this.stashMap.size()];
    this.stashMap.keySet().toArray(members);

    PrintStream ostream = LogStream.openStream(file, true);
    Object[] entries;

    List<StashLogEntry> entryList = null;
    ostream.println("<html><head>");
    ostream.println("<title>Clan Stash Log @ " + (new Date()).toString() + "</title>");
    ostream.println("<style><!--");
    ostream.println();
    ostream.println("\tbody { font-family: Verdana; font-size: 9pt }");
    ostream.println();
    ostream.println("\t." + ClanLogRequest.STASH_ADD + " { color: green }");
    ostream.println("\t." + ClanLogRequest.STASH_TAKE + " { color: olive }");
    ostream.println("\t." + ClanLogRequest.WAR_BATTLE + " { color: orange }");
    ostream.println("\t." + ClanLogRequest.CLAN_WHITELIST + " { color: blue }");
    ostream.println("\t." + ClanLogRequest.CLAN_ACCEPT + " { color: blue }");
    ostream.println("\t." + ClanLogRequest.CLAN_LEAVE + " { color: red }");
    ostream.println("\t." + ClanLogRequest.CLAN_BOOT + " { color: red }");
    ostream.println();
    ostream.println("--></style></head>");

    ostream.println();
    ostream.println("<body>");
    ostream.println();
    ostream.println("<!-- Begin Stash Log: Do Not Modify Beyond This Point -->");

    for (int i = 0; i < members.length; ++i) {
      ostream.println(" " + members[i] + ":");

      entryList = this.stashMap.get(members[i]);
      Collections.sort(entryList);
      entries = entryList.toArray();

      ostream.println("<ul>");
      for (int j = 0; j < entries.length; ++j) {
        ostream.println(entries[j].toString());
      }
      ostream.println("</ul>");

      ostream.println();
    }

    ostream.println("</body></html>");
    ostream.close();
  }

  private static final String ADD_REGEX =
      ClanLogRequest.TIME_REGEX
          + ": "
          + ClanLogRequest.PLAYER_REGEX
          + " added ([\\d,]+) (.*?)\\.<br>";
  private static final String TAKE_REGEX =
      ClanLogRequest.TIME_REGEX
          + ": "
          + ClanLogRequest.PLAYER_REGEX
          + " took ([\\d,]+) (.*?)\\.<br>";

  private void handleItems(final boolean parseAdditions) {
    String handleType = parseAdditions ? ClanLogRequest.STASH_ADD : ClanLogRequest.STASH_TAKE;
    String regex = parseAdditions ? ClanLogRequest.ADD_REGEX : ClanLogRequest.TAKE_REGEX;
    String suffixDescription = parseAdditions ? "added to stash" : "taken from stash";

    int lastItemId;
    int entryCount;

    List<StashLogEntry> entryList;
    String currentMember;

    StashLogEntry entry;
    StringBuilder entryBuffer = new StringBuilder();
    Matcher entryMatcher = Pattern.compile(regex, Pattern.DOTALL).matcher(this.responseText);

    while (entryMatcher.find()) {
      try {
        entryBuffer.setLength(0);
        currentMember = entryMatcher.group(2).trim();

        if (!this.stashMap.containsKey(currentMember)) {
          this.stashMap.put(currentMember, new ArrayList<>());
        }

        entryList = this.stashMap.get(currentMember);
        entryCount = StringUtilities.parseInt(entryMatcher.group(3));

        lastItemId = ItemDatabase.getItemId(entryMatcher.group(4), entryCount);
        entryBuffer.append((ItemPool.get(lastItemId, entryCount)).toString());

        entryBuffer.append(" ");
        entryBuffer.append(suffixDescription);

        entry =
            new StashLogEntry(
                handleType,
                ClanLogRequest.STASH_FORMAT.parse(entryMatcher.group(1)),
                entryBuffer.toString());
        if (!entryList.contains(entry)) {
          entryList.add(entry);
        }
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }

    this.responseText = entryMatcher.replaceAll("");
  }

  private void handleBattles() {
    List<StashLogEntry> entryList;
    String currentMember;

    StashLogEntry entry;
    Matcher entryMatcher = ClanLogRequest.WAR_PATTERN.matcher(this.responseText);

    while (entryMatcher.find()) {
      try {
        currentMember = entryMatcher.group(2).trim();
        if (!this.stashMap.containsKey(currentMember)) {
          this.stashMap.put(currentMember, new ArrayList<>());
        }

        entryList = this.stashMap.get(currentMember);
        entry =
            new StashLogEntry(
                ClanLogRequest.WAR_BATTLE,
                ClanLogRequest.STASH_FORMAT.parse(entryMatcher.group(1)),
                "<i>" + entryMatcher.group(3) + "</i> attacked");

        if (!entryList.contains(entry)) {
          entryList.add(entry);
        }
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }

    this.responseText = entryMatcher.replaceAll("");
  }

  private void handleAdmin(
      final String entryType,
      final String searchString,
      final String suffixString,
      final String descriptionString) {
    String regex =
        ClanLogRequest.TIME_REGEX
            + ": ([^<]*?) \\(#\\d+\\) "
            + searchString
            + "(.*?)"
            + suffixString
            + "\\.?<br>";

    List<StashLogEntry> entryList;
    String currentMember;

    StashLogEntry entry;
    String entryString;
    Matcher entryMatcher = Pattern.compile(regex).matcher(this.responseText);

    while (entryMatcher.find()) {
      try {
        currentMember = entryMatcher.group(descriptionString.endsWith(" ") ? 3 : 2).trim();
        if (!this.stashMap.containsKey(currentMember)) {
          this.stashMap.put(currentMember, new ArrayList<>());
        }

        entryList = this.stashMap.get(currentMember);
        entryString =
            descriptionString.endsWith(" ")
                ? descriptionString + entryMatcher.group(2)
                : descriptionString;
        entry =
            new StashLogEntry(
                entryType, ClanLogRequest.STASH_FORMAT.parse(entryMatcher.group(1)), entryString);

        if (!entryList.contains(entry)) {
          entryList.add(entry);
        }
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }

    this.responseText = entryMatcher.replaceAll("");
  }

  public static class StashLogEntry implements Comparable<StashLogEntry> {
    private Date timestamp;
    private final String stringform;

    public StashLogEntry(final String entryType, final Date timestamp, final String entry) {
      this.timestamp = timestamp;

      this.stringform =
          "\t<li class=\""
              + entryType
              + "\">"
              + ClanLogRequest.STASH_FORMAT.format(timestamp)
              + ": "
              + entry
              + "</li>";
    }

    public StashLogEntry(final String stringform) {
      Matcher entryMatcher = ClanLogRequest.LOGENTRY_PATTERN.matcher(stringform);
      entryMatcher.find();

      // this.entryType = entryMatcher.group( 1 );

      try {
        this.timestamp = ClanLogRequest.STASH_FORMAT.parse(entryMatcher.group(2));
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
        this.timestamp = new Date();
      }

      this.stringform = stringform;
    }

    @Override
    public int compareTo(final StashLogEntry o) {
      return !(o instanceof StashLogEntry)
          ? -1
          : this.timestamp.before(o.timestamp) ? 1 : this.timestamp.after(o.timestamp) ? -1 : 0;
    }

    @Override
    public boolean equals(final Object o) {
      return !(o instanceof StashLogEntry) ? false : this.stringform.equals(o.toString());
    }

    @Override
    public int hashCode() {
      return this.stringform != null ? this.stringform.hashCode() : 0;
    }

    @Override
    public String toString() {
      return this.stringform;
    }
  }
}
