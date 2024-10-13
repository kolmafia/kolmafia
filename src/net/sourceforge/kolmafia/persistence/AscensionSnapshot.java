package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest.AscensionDataField;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

@SuppressWarnings("incomplete-switch")
public class AscensionSnapshot {
  public enum AscensionFilter {
    UNKNOWN_TYPE,
    NO_FILTER,
    NORMAL,
    HARDCORE,
    CASUAL;
  }

  private static final Map<String, String> ascensionMap = new TreeMap<>();
  private static final List<AscensionHistoryRequest> ascensionDataList = new ArrayList<>();
  private static final List<AscensionDataField> softcoreAscensionList = new ArrayList<>();
  private static final List<AscensionDataField> hardcoreAscensionList = new ArrayList<>();
  private static final List<AscensionDataField> casualAscensionList = new ArrayList<>();

  private static final Pattern LINK_PATTERN = Pattern.compile("</?a[^>]+>");

  private AscensionSnapshot() {}

  public static final void clearCache() {
    // First, initialize all of the lists and
    // arrays which are used by the request.

    AscensionSnapshot.ascensionMap.clear();

    AscensionSnapshot.ascensionDataList.clear();
    AscensionSnapshot.softcoreAscensionList.clear();
    AscensionSnapshot.hardcoreAscensionList.clear();
    AscensionSnapshot.casualAscensionList.clear();
  }

  public static final void registerMember(final String playerName) {
    String lowerCaseName = playerName.toLowerCase();
    AscensionSnapshot.ascensionMap.put(lowerCaseName, "");
  }

  public static final void unregisterMember(final String playerId) {
    String lowerCaseName = ContactManager.getPlayerName(playerId).toLowerCase();
    AscensionSnapshot.ascensionMap.remove(lowerCaseName);
  }

  public static final Map<String, String> getAscensionMap() {
    return AscensionSnapshot.ascensionMap;
  }

  public static final String getAscensionData(
      final AscensionFilter typeFilter,
      final int mostAscensionsBoardSize,
      final int mainBoardSize,
      final int classBoardSize,
      final int maxAge,
      final boolean playerMoreThanOnce,
      boolean localProfileLink) {
    AscensionSnapshot.initializeAscensionData();
    StringBuilder strbuf = new StringBuilder();

    strbuf.append("<html><head>");
    strbuf.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");

    strbuf.append("<title>");

    switch (typeFilter) {
      case NORMAL -> strbuf.append("Normal");
      case HARDCORE -> strbuf.append("Hardcore");
      case CASUAL -> strbuf.append("Casual");
    }

    String clanName = ClanManager.getClanName(true);

    strbuf.append(" Ascension Data for ");
    strbuf.append(clanName);
    strbuf.append(" (");
    strbuf.append(new Date());
    strbuf.append(")</title>");
    strbuf.append(KoLConstants.LINE_BREAK);

    strbuf.append("<style> body, td { font-family: sans-serif; } </style></head><body>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<center><table cellspacing=0 cellpadding=0><tr><td align=center><h2><u>");
    strbuf.append(clanName);
    strbuf.append(" (#");
    strbuf.append(ClanManager.getClanId());
    strbuf.append(")</u></h2></td></tr>");
    strbuf.append(KoLConstants.LINE_BREAK);

    // Right below the name of the clan, write the average
    // number of this kind of ascension.

    strbuf.append("<tr><td align=center><h3>Avg: ");
    strbuf.append(
        ((typeFilter == AscensionFilter.NORMAL
                    ? (float) AscensionSnapshot.softcoreAscensionList.size()
                    : 0.0f)
                + AscensionSnapshot.hardcoreAscensionList.size()
                + AscensionSnapshot.casualAscensionList.size())
            / AscensionSnapshot.ascensionMap.size());
    strbuf.append("</h3></td></tr></table><br><br>");
    strbuf.append(KoLConstants.LINE_BREAK);

    // Next, the ascension leaderboards for most (numeric)
    // ascensions.

    strbuf.append("<table width=500 cellspacing=0 cellpadding=0>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<tr><td style=\"color:white\" align=center bgcolor=blue><b>Most ");
    strbuf.append(
        typeFilter == AscensionFilter.NORMAL
            ? "Normal "
            : typeFilter == AscensionFilter.HARDCORE ? "Hardcore " : "Casual ");
    strbuf.append(
        "Ascensions</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<tr><td align=center><b>Player&nbsp;&nbsp;&nbsp;&nbsp;</b></td>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<td align=center><b>Ascensions</b></td></tr>");
    strbuf.append(KoLConstants.LINE_BREAK);

    // Resort the lists, and print the results to the buffer
    // so that you have the "most ascensions" leaderboard.

    AscensionHistoryRequest.setComparator(typeFilter);
    Collections.sort(AscensionSnapshot.ascensionDataList);

    for (int i = 0;
        i < AscensionSnapshot.ascensionDataList.size()
            && (mostAscensionsBoardSize == 0 ? i < 20 : i < mostAscensionsBoardSize);
        ++i) {
      String leader = AscensionSnapshot.ascensionDataList.get(i).toString();

      if (!localProfileLink) {
        leader = AscensionSnapshot.LINK_PATTERN.matcher(leader).replaceAll("");
      }

      strbuf.append(leader);
      strbuf.append(KoLConstants.LINE_BREAK);
    }

    strbuf.append("</table></td></tr></table><br><br>");
    strbuf.append(KoLConstants.LINE_BREAK);

    // Finally, the ascension leaderboards for fastest
    // ascension speed.  Do this for all paths individually.

    Consumer<Path> appendPathBoard =
        (p) -> {
          strbuf.append(
              AscensionSnapshot.getPathedAscensionData(
                  typeFilter,
                  p,
                  mainBoardSize,
                  classBoardSize,
                  maxAge,
                  playerMoreThanOnce,
                  localProfileLink));
          strbuf.append(KoLConstants.LINE_BREAK);
        };

    if (typeFilter != AscensionFilter.CASUAL) {
      strbuf.append(KoLConstants.LINE_BREAK);
      for (var path : Path.values()) {
        if (path != Path.NONE && path != Path.BAD_MOON) {
          appendPathBoard.accept(path);
        }
      }
      if (typeFilter == AscensionFilter.HARDCORE) {
        appendPathBoard.accept(Path.BAD_MOON);
      }
    }
    appendPathBoard.accept(Path.NONE);

    strbuf.append("</center>");
    return strbuf.toString();
  }

  public static final String getPathedAscensionData(
      final AscensionFilter typeFilter,
      final Path pathFilter,
      final int mainBoardSize,
      final int classBoardSize,
      final int maxAge,
      final boolean playerMoreThanOnce,
      final boolean localProfileLink) {
    StringBuilder strbuf = new StringBuilder();

    // First, print the table showing the top ascenders
    // without a class-based filter.

    strbuf.append(
        AscensionSnapshot.getAscensionData(
            typeFilter,
            pathFilter,
            null,
            mainBoardSize,
            classBoardSize,
            maxAge,
            playerMoreThanOnce,
            localProfileLink));

    // Next, print the nifty disappearing link bar that
    // is used in the KoL leaderboard frame.

    strbuf.append(KoLConstants.LINE_BREAK);

    // Finally, add in all the breakdown tables, just like
    // in the KoL leaderboard frame, for class based paths.

    Runnable hideShowByClass =
        () -> {
          strbuf.append(
              "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec");
          strbuf.append(pathFilter);
          strbuf.append(
              "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">");
          strbuf.append("hide/show records by class</a><div id=\"sec");
          strbuf.append(pathFilter);
          strbuf.append("\" style=\"display:none\"><br><br>");
          strbuf.append(KoLConstants.LINE_BREAK);
        };

    Consumer<AscensionClass> appendClassBoard =
        (c) -> {
          strbuf.append(KoLConstants.LINE_BREAK);
          strbuf.append(
              AscensionSnapshot.getAscensionData(
                  typeFilter,
                  pathFilter,
                  c,
                  mainBoardSize,
                  classBoardSize,
                  maxAge,
                  playerMoreThanOnce,
                  localProfileLink));
          strbuf.append(KoLConstants.LINE_BREAK);
        };

    switch (pathFilter) {
      case AVATAR_OF_BORIS:
      case ZOMBIE_SLAYER:
      case AVATAR_OF_JARLSBERG:
      case AVATAR_OF_SNEAKY_PETE:
      case ACTUALLY_ED_THE_UNDYING:
      case GELATINOUS_NOOB:
      case DARK_GYFFTE:
      case PATH_OF_THE_PLUMBER:
      case GREY_YOU:
        break;
      case AVATAR_OF_WEST_OF_LOATHING:
        hideShowByClass.run();
        strbuf.append("<table><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.COW_PUNCHER);
        strbuf.append("</td><td valign=top>");
        appendClassBoard.accept(AscensionClass.BEANSLINGER);
        strbuf.append("</td></tr><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.SNAKE_OILER);
        strbuf.append("</td></tr></table>");
        break;
      case SHADOWS_OVER_LOATHING:
        hideShowByClass.run();
        strbuf.append("<table><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.PIG_SKINNER);
        strbuf.append("</td><td valign=top>");
        appendClassBoard.accept(AscensionClass.CHEESE_WIZARD);
        strbuf.append("</td></tr><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.JAZZ_AGENT);
        strbuf.append("</td></tr></table>");
        break;
      default:
        hideShowByClass.run();
        strbuf.append("<table><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.SEAL_CLUBBER);
        strbuf.append("</td><td valign=top>");
        appendClassBoard.accept(AscensionClass.SAUCEROR);
        strbuf.append("</td></tr><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.TURTLE_TAMER);
        strbuf.append("</td><td valign=top>");
        appendClassBoard.accept(AscensionClass.DISCO_BANDIT);
        strbuf.append("</td></tr><tr><td valign=top>");
        appendClassBoard.accept(AscensionClass.PASTAMANCER);
        strbuf.append("</td><td valign=top>");
        appendClassBoard.accept(AscensionClass.ACCORDION_THIEF);
        strbuf.append("</td></tr></table>");
        break;
    }

    // Close the disappearing section and return the complete
    // code for this path filter.

    strbuf.append("</div><br><br>");
    return strbuf.toString();
  }

  public static final String getAscensionData(
      final AscensionFilter typeFilter,
      final Path pathFilter,
      final AscensionClass classFilter,
      final int mainBoardSize,
      final int classBoardSize,
      final int maxAge,
      final boolean playerMoreThanOnce,
      boolean localProfileLink) {
    StringBuilder strbuf = new StringBuilder();

    AscensionDataField[] fields = null;

    switch (typeFilter) {
      case NORMAL:
        fields = new AscensionDataField[AscensionSnapshot.softcoreAscensionList.size()];
        AscensionSnapshot.softcoreAscensionList.toArray(fields);
        break;
      case HARDCORE:
        fields = new AscensionDataField[AscensionSnapshot.hardcoreAscensionList.size()];
        AscensionSnapshot.hardcoreAscensionList.toArray(fields);
        break;
      case CASUAL:
        fields = new AscensionDataField[AscensionSnapshot.casualAscensionList.size()];
        AscensionSnapshot.casualAscensionList.toArray(fields);
        break;
      default:
        return "";
    }

    // First, retrieve all the ascensions which
    // satisfy the current filter so that the
    // total count can be displayed in the header.

    List<AscensionDataField> resultsList = new ArrayList<>();

    for (AscensionDataField field : fields) {
      if (field.matchesFilter(typeFilter, pathFilter, classFilter, maxAge)) {
        resultsList.add(field);
      }
    }

    // Next, retrieve only the top ten list so that
    // a maximum of ten elements are printed.

    List<AscensionDataField> leaderList = new ArrayList<>();
    int leaderListSize =
        classFilter == null
            ? (mainBoardSize == 0 ? 10 : mainBoardSize)
            : classBoardSize == 0 ? 5 : classBoardSize;

    fields = new AscensionDataField[resultsList.size()];
    resultsList.toArray(fields);

    for (int i = 0; i < fields.length && leaderList.size() < leaderListSize; ++i) {
      AscensionDataField field = fields[i];
      if (!leaderList.contains(field) || playerMoreThanOnce) {
        leaderList.add(field);
      }
    }

    // Now that the data has been retrieved, go ahead
    // and print the table header data.

    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<table width=500 cellspacing=0 cellpadding=0>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<tr><td style=\"color:white\" align=center bgcolor=blue><b>");

    if (classFilter == null) {
      strbuf.append("Fastest ");

      strbuf.append(
          typeFilter == AscensionFilter.NORMAL
              ? "Normal "
              : typeFilter == AscensionFilter.HARDCORE ? "Hardcore " : "Casual ");
      strbuf.append(
          pathFilter == null
              ? ""
              : (pathFilter == Path.NONE ? "No Path" : pathFilter.getName()) + " ");

      strbuf.append("Ascensions (Out of ");
      strbuf.append(resultsList.size());
      strbuf.append(")");
    } else {
      strbuf.append(classFilter.getName());
    }

    strbuf.append(
        "</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<tr><td align=center><b>Player&nbsp;&nbsp;&nbsp;&nbsp;</b></td>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<td align=center><b>Days</b></td>");
    strbuf.append(KoLConstants.LINE_BREAK);
    strbuf.append("<td align=center><b>Adventures</b></td></tr>");
    strbuf.append(KoLConstants.LINE_BREAK);

    // Now, print the actual table data inside, using
    // the top ten list.

    for (AscensionDataField field : leaderList) {
      String leader = field.toString();

      if (!localProfileLink) {
        leader = AscensionSnapshot.LINK_PATTERN.matcher(leader).replaceAll("");
      }

      strbuf.append(leader);
      strbuf.append(KoLConstants.LINE_BREAK);
    }

    strbuf.append("</table></td></tr></table>");
    strbuf.append(KoLConstants.LINE_BREAK);

    return strbuf.toString();
  }

  private static void initializeAscensionData() {
    // If the ascension lists have already been initialized,
    // then return from this method call.

    if (!AscensionSnapshot.ascensionDataList.isEmpty()) {
      return;
    }

    // If the lists are not initialized, then go ahead and
    // load the appropriate data into them.

    String[] names = new String[AscensionSnapshot.ascensionMap.size()];
    AscensionSnapshot.ascensionMap.keySet().toArray(names);

    for (String name : names) {
      AscensionHistoryRequest request =
          AscensionHistoryRequest.getInstance(
              name, ContactManager.getPlayerId(name), AscensionSnapshot.ascensionMap.get(name));
      AscensionSnapshot.ascensionDataList.add(request);

      AscensionDataField[] fields = new AscensionDataField[request.getAscensionData().size()];
      request.getAscensionData().toArray(fields);

      for (AscensionDataField field : fields) {
        if (field.matchesFilter(AscensionFilter.NORMAL, null, null, 0)) {
          AscensionSnapshot.softcoreAscensionList.add(field);
        } else if (field.matchesFilter(AscensionFilter.HARDCORE, null, null, 0)) {
          AscensionSnapshot.hardcoreAscensionList.add(field);
        } else {
          AscensionSnapshot.casualAscensionList.add(field);
        }
      }
    }

    // Now that you've retrieved all the data from all the
    // players, sort the lists for easier loading later.

    Collections.sort(AscensionSnapshot.softcoreAscensionList);
    Collections.sort(AscensionSnapshot.hardcoreAscensionList);
    Collections.sort(AscensionSnapshot.casualAscensionList);
  }
}
