package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.EveryCard;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PlayCommand extends AbstractCommand {
  private static String[] CANONICAL_STAT_ARRAY;
  private static final TreeMap<String, Stat> canonicalNameToStat = new TreeMap<String, Stat>();

  private static void addStat(final Stat stat, final List<String> stats) {
    String canonical = StringUtilities.getCanonicalName(stat.toString());
    stats.add(canonical);
    PlayCommand.canonicalNameToStat.put(canonical, stat);
  }

  static {
    ArrayList<String> stats = new ArrayList<String>();
    PlayCommand.addStat(Stat.MUSCLE, stats);
    PlayCommand.addStat(Stat.MYSTICALITY, stats);
    PlayCommand.addStat(Stat.MOXIE, stats);
    PlayCommand.CANONICAL_STAT_ARRAY = stats.toArray(new String[stats.size()]);
    Arrays.sort(PlayCommand.CANONICAL_STAT_ARRAY);
  }

  private static String[] CANONICAL_BUFF_ARRAY;
  private static final TreeMap<String, AdventureResult> canonicalNameToBuff =
      new TreeMap<String, AdventureResult>();

  private static void addBuff(
      final String name, final AdventureResult buff, final List<String> buffs) {
    String canonical = StringUtilities.getCanonicalName(name);
    buffs.add(canonical);
    PlayCommand.canonicalNameToBuff.put(canonical, buff);
  }

  static {
    ArrayList<String> buffs = new ArrayList<String>();
    PlayCommand.addBuff("muscle", DeckOfEveryCardRequest.STRONGLY_MOTIVATED, buffs);
    PlayCommand.addBuff(
        DeckOfEveryCardRequest.STRONGLY_MOTIVATED.getName(),
        DeckOfEveryCardRequest.STRONGLY_MOTIVATED,
        buffs);
    PlayCommand.addBuff("mysticality", DeckOfEveryCardRequest.MAGICIANSHIP, buffs);
    PlayCommand.addBuff(
        DeckOfEveryCardRequest.MAGICIANSHIP.getName(), DeckOfEveryCardRequest.MAGICIANSHIP, buffs);
    PlayCommand.addBuff("moxie", DeckOfEveryCardRequest.DANCIN_FOOL, buffs);
    PlayCommand.addBuff(
        DeckOfEveryCardRequest.DANCIN_FOOL.getName(), DeckOfEveryCardRequest.DANCIN_FOOL, buffs);
    PlayCommand.addBuff("items", DeckOfEveryCardRequest.FORTUNE_OF_THE_WHEEL, buffs);
    PlayCommand.addBuff("item drop", DeckOfEveryCardRequest.FORTUNE_OF_THE_WHEEL, buffs);
    PlayCommand.addBuff(
        DeckOfEveryCardRequest.FORTUNE_OF_THE_WHEEL.getName(),
        DeckOfEveryCardRequest.FORTUNE_OF_THE_WHEEL,
        buffs);
    PlayCommand.addBuff("initiative", DeckOfEveryCardRequest.RACING, buffs);
    PlayCommand.addBuff(
        DeckOfEveryCardRequest.RACING.getName(), DeckOfEveryCardRequest.RACING, buffs);
    PlayCommand.CANONICAL_BUFF_ARRAY = buffs.toArray(new String[buffs.size()]);
    Arrays.sort(PlayCommand.CANONICAL_BUFF_ARRAY);
  }

  public PlayCommand() {
    this.usage =
        " random | phylum [PHYLUM] | stat [STAT] | buff [BUFF] | CARDNAME - Play a random or specified card";
  }

  @Override
  public void run(final String cmd, String parameter) {
    EveryCard card = null;

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay("Play what?");
      return;
    }

    if (parameter.startsWith("random")) {
      card = null;
    } else if (parameter.startsWith("phylum")) {
      parameter = parameter.substring(6).trim();

      if (parameter.equals("")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Which monster phylum do you want?");
        return;
      }

      Phylum phylum = Phylum.find(parameter);
      if (phylum == Phylum.NONE) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "What kind of random monster is a " + parameter + "?");
        return;
      }

      card = DeckOfEveryCardRequest.phylumToCard(phylum);
      if (card == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "'" + parameter + "' is not a known monster phylum");
        return;
      }
    } else if (parameter.startsWith("stat")) {
      parameter = parameter.substring(4).trim();

      if (parameter.equals("")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Which stat do you want?");
        return;
      }

      Stat stat = null;
      if (parameter.startsWith("main")) {
        stat = KoLCharacter.mainStat();
      } else {
        List<String> matchingNames =
            StringUtilities.getMatchingNames(PlayCommand.CANONICAL_STAT_ARRAY, parameter);
        if (matchingNames.size() == 0) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Which stat is " + parameter + "?");
          return;
        }

        if (matchingNames.size() > 1) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "'" + parameter + "' is an ambiguous stat");
          return;
        }

        stat = PlayCommand.canonicalNameToStat.get(matchingNames.get(0));
      }

      card = DeckOfEveryCardRequest.statToCard(stat);
      if (card == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Internal error: could not find card for stat " + parameter);
        return;
      }
    } else if (parameter.startsWith("buff")) {
      parameter = parameter.substring(4).trim();

      if (parameter.equals("")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Which buff do you want?");
        return;
      }

      List<String> matchingNames =
          StringUtilities.getMatchingNames(PlayCommand.CANONICAL_BUFF_ARRAY, parameter);
      if (matchingNames.size() == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Which buff is " + parameter + "?");
        return;
      }

      if (matchingNames.size() > 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "'" + parameter + "' is an ambiguous buff");
        return;
      }

      AdventureResult buff = PlayCommand.canonicalNameToBuff.get(matchingNames.get(0));
      card = DeckOfEveryCardRequest.buffToCard(buff);
      if (card == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Internal error: could not find card for buff " + parameter);
        return;
      }
    } else {
      List<String> matchingNames = DeckOfEveryCardRequest.getMatchingNames(parameter);
      if (matchingNames.size() == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "I don't know how to play " + parameter);
        return;
      }

      if (matchingNames.size() > 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "'" + parameter + "' is an ambiguous card name ");
        return;
      }

      String name = matchingNames.get(0);

      card = DeckOfEveryCardRequest.canonicalNameToCard(name);
      if (card == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "I don't know how to play " + parameter);
        return;
      }
    }

    DeckOfEveryCardRequest request =
        card == null ? new DeckOfEveryCardRequest() : new DeckOfEveryCardRequest(card);

    RequestThread.postRequest(request);
  }
}
