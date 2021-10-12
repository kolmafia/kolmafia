package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest.AscensionDataField;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

public class AscensionSnapshot {
  public static final int NO_FILTER = 0;

  public static final int UNKNOWN_CLASS = -1;
  public static final int SEAL_CLUBBER = 1;
  public static final int TURTLE_TAMER = 2;
  public static final int PASTAMANCER = 3;
  public static final int SAUCEROR = 4;
  public static final int DISCO_BANDIT = 5;
  public static final int ACCORDION_THIEF = 6;
  public static final int BORIS = 11;
  public static final int ZOMBIE_MASTER = 12;
  public static final int JARLSBERG = 14;
  public static final int SNEAKY_PETE = 15;
  public static final int ED = 17;
  public static final int COW_PUNCHER = 18;
  public static final int BEAN_SLINGER = 19;
  public static final int SNAKE_OILER = 20;
  public static final int NOOB = 23;
  public static final int VAMPYRE = 24;
  public static final int PLUMBER = 25;

  public static final int UNKNOWN_TYPE = -1;
  public static final int NORMAL = 1;
  public static final int HARDCORE = 2;
  public static final int CASUAL = 3;

  private static final Map<String, String> ascensionMap = new TreeMap<String, String>();
  private static final List<AscensionHistoryRequest> ascensionDataList =
      new ArrayList<AscensionHistoryRequest>();
  private static final List<AscensionDataField> softcoreAscensionList =
      new ArrayList<AscensionDataField>();
  private static final List<AscensionDataField> hardcoreAscensionList =
      new ArrayList<AscensionDataField>();
  private static final List<AscensionDataField> casualAscensionList =
      new ArrayList<AscensionDataField>();

  private static final Pattern LINK_PATTERN = Pattern.compile("</?a[^>]+>");

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
      final int typeFilter,
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
      case AscensionSnapshot.NORMAL:
        strbuf.append("Normal");
        break;
      case AscensionSnapshot.HARDCORE:
        strbuf.append("Hardcore");
        break;
      case AscensionSnapshot.CASUAL:
        strbuf.append("Casual");
        break;
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
        ((typeFilter == AscensionSnapshot.NORMAL
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
        typeFilter == AscensionSnapshot.NORMAL
            ? "Normal "
            : typeFilter == AscensionSnapshot.HARDCORE ? "Hardcore " : "Casual ");
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

    if (typeFilter != AscensionSnapshot.CASUAL) {
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.PATH_OF_THE_PLUMBER,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.KINGDOM_OF_EXPLOATHING,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.CRAZY_RANDOM_SUMMER_TWO,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.DARK_GYFFTE,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.DISGUISES_DELIMIT,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.GLOVER,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.POKEFAM,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.LIVE_ASCEND_REPEAT,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.LICENSE_TO_ADVENTURE,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.GELATINOUS_NOOB,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.NUCLEAR_AUTUMN,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.THE_SOURCE,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.AVATAR_OF_WEST_OF_LOATHING,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.COMMUNITY_SERVICE,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.CRAZY_RANDOM_SUMMER,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.ACTUALLY_ED_THE_UNDYING,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.STANDARD,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.PICKY,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.HEAVY_RAINS,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.SLOW_AND_STEADY,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.AVATAR_OF_SNEAKY_PETE,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.CLASS_ACT_II,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.KOLHS,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.BIG,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.AVATAR_OF_JARLSBERG,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.CLASS_ACT,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.ZOMBIE_SLAYER,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.BUGBEAR_INVASION,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.AVATAR_OF_BORIS,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.TRENDY,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.SURPRISING_FIST,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.BEES_HATE_YOU,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.BAD_MOON,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.OXYGENARIAN,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.TEETOTALER,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
      strbuf.append(
          AscensionSnapshot.getPathedAscensionData(
              typeFilter,
              Path.BOOZETAFARIAN,
              mainBoardSize,
              classBoardSize,
              maxAge,
              playerMoreThanOnce,
              localProfileLink));
      strbuf.append(KoLConstants.LINE_BREAK);
    }
    strbuf.append(
        AscensionSnapshot.getPathedAscensionData(
            typeFilter,
            Path.NONE,
            mainBoardSize,
            classBoardSize,
            maxAge,
            playerMoreThanOnce,
            localProfileLink));
    strbuf.append(KoLConstants.LINE_BREAK);

    strbuf.append("</center>");
    return strbuf.toString();
  }

  public static final String getPathedAscensionData(
      final int typeFilter,
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
            AscensionSnapshot.NO_FILTER,
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

    switch (pathFilter) {
      case AVATAR_OF_BORIS:
      case ZOMBIE_SLAYER:
      case AVATAR_OF_JARLSBERG:
      case AVATAR_OF_SNEAKY_PETE:
      case ACTUALLY_ED_THE_UNDYING:
      case GELATINOUS_NOOB:
      case DARK_GYFFTE:
      case PATH_OF_THE_PLUMBER:
        break;
      case AVATAR_OF_WEST_OF_LOATHING:
        strbuf.append(
            "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec");
        strbuf.append(pathFilter);
        strbuf.append(
            "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">");
        strbuf.append("hide/show records by class</a><div id=\"sec");
        strbuf.append(pathFilter);
        strbuf.append("\" style=\"display:none\"><br><br>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("<table><tr><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.COW_PUNCHER,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.BEAN_SLINGER,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td></tr><tr><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.SNAKE_OILER,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td></tr></table>");
        break;
      default:
        strbuf.append(
            "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec");
        strbuf.append(pathFilter);
        strbuf.append(
            "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">");
        strbuf.append("hide/show records by class</a><div id=\"sec");
        strbuf.append(pathFilter);
        strbuf.append("\" style=\"display:none\"><br><br>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("<table><tr><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.SEAL_CLUBBER,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.SAUCEROR,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td></tr><tr><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.TURTLE_TAMER,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.DISCO_BANDIT,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td></tr><tr><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.PASTAMANCER,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td><td valign=top>");
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append(
            AscensionSnapshot.getAscensionData(
                typeFilter,
                pathFilter,
                AscensionSnapshot.ACCORDION_THIEF,
                mainBoardSize,
                classBoardSize,
                maxAge,
                playerMoreThanOnce,
                localProfileLink));
        strbuf.append(KoLConstants.LINE_BREAK);
        strbuf.append("</td></tr></table>");
        break;
    }

    // Close the disappearing section and return the complete
    // code for this path filter.

    strbuf.append("</div><br><br>");
    return strbuf.toString();
  }

  public static final String getAscensionData(
      final int typeFilter,
      final Path pathFilter,
      final int classFilter,
      final int mainBoardSize,
      final int classBoardSize,
      final int maxAge,
      final boolean playerMoreThanOnce,
      boolean localProfileLink) {
    StringBuilder strbuf = new StringBuilder();

    AscensionDataField[] fields = null;

    switch (typeFilter) {
      case AscensionSnapshot.NORMAL:
        fields = new AscensionDataField[AscensionSnapshot.softcoreAscensionList.size()];
        AscensionSnapshot.softcoreAscensionList.toArray(fields);
        break;
      case AscensionSnapshot.HARDCORE:
        fields = new AscensionDataField[AscensionSnapshot.hardcoreAscensionList.size()];
        AscensionSnapshot.hardcoreAscensionList.toArray(fields);
        break;
      case AscensionSnapshot.CASUAL:
        fields = new AscensionDataField[AscensionSnapshot.casualAscensionList.size()];
        AscensionSnapshot.casualAscensionList.toArray(fields);
        break;
      default:
        return "";
    }

    // First, retrieve all the ascensions which
    // satisfy the current filter so that the
    // total count can be displayed in the header.

    List<AscensionDataField> resultsList = new ArrayList<AscensionDataField>();

    for (AscensionDataField field : fields) {
      if (field.matchesFilter(typeFilter, pathFilter, classFilter, maxAge)) {
        resultsList.add(field);
      }
    }

    // Next, retrieve only the top ten list so that
    // a maximum of ten elements are printed.

    List<AscensionDataField> leaderList = new ArrayList<AscensionDataField>();
    int leaderListSize =
        classFilter == AscensionSnapshot.NO_FILTER
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

    switch (classFilter) {
      case NO_FILTER:
        strbuf.append("Fastest ");

        strbuf.append(
            typeFilter == AscensionSnapshot.NORMAL
                ? "Normal "
                : typeFilter == AscensionSnapshot.HARDCORE ? "Hardcore " : "Casual ");
        strbuf.append(pathFilter == null ? "" : pathFilter.getName());

        strbuf.append("Ascensions (Out of ");
        strbuf.append(resultsList.size());
        strbuf.append(")");
        break;

      case SEAL_CLUBBER:
        strbuf.append("Seal Clubber");
        break;

      case TURTLE_TAMER:
        strbuf.append("Turtle Tamer");
        break;

      case PASTAMANCER:
        strbuf.append("Pastamancer");
        break;

      case SAUCEROR:
        strbuf.append("Sauceror");
        break;

      case DISCO_BANDIT:
        strbuf.append("Disco Bandit");
        break;

      case ACCORDION_THIEF:
        strbuf.append("Accordion Thief");
        break;

      case COW_PUNCHER:
        strbuf.append("Cow Puncher");
        break;

      case BEAN_SLINGER:
        strbuf.append("Bean Slinger");
        break;

      case SNAKE_OILER:
        strbuf.append("Snake Oiler");
        break;
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
        if (field.matchesFilter(AscensionSnapshot.NORMAL, null, AscensionSnapshot.NO_FILTER, 0)) {
          AscensionSnapshot.softcoreAscensionList.add(field);
        } else if (field.matchesFilter(
            AscensionSnapshot.HARDCORE, null, AscensionSnapshot.NO_FILTER, 0)) {
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
