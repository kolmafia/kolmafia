package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;

public class VoteMonsterManager {
  private VoteMonsterManager() {}

  public static void checkCounter() {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "voter registration form")) {
      return;
    }

    if (Preferences.getString("trackVoteMonster").equals("false")) {
      return;
    }

    if (Preferences.getString("trackVoteMonster").equals("free")
        && Preferences.getInteger("_voteFreeFights") >= 3) {
      return;
    }

    if (TurnCounter.isCounting("Vote Monster")) {
      return;
    }

    int turns = 11 - ((KoLCharacter.getTurnsPlayed() - 1) % 11);
    TurnCounter.startCounting(turns, "Vote Monster", "absballot.gif");
  }

  public static boolean voteMonsterNow() {
    int totalTurns = KoLCharacter.getTurnsPlayed();
    return totalTurns % 11 == 1 && Preferences.getInteger("lastVoteMonsterTurn") != totalTurns;
  }

  private static final Pattern VOTE_PATTERN =
      Pattern.compile(
          "initiatives: </b><div style='margin-left: 1em; color: blue'>(.*?)<br>(.*?)</div>");
  private static final Pattern VOTE_SPEECH_PATTERN =
      Pattern.compile("<b>Today's Leader: </b>(.*?)<br><blockquote>(.*?)</blockquote>");

  public static void parseBooth(final String responseText) {
    if (responseText.contains("Today's Leader")) {
      Matcher matcher = VOTE_PATTERN.matcher(responseText);
      if (matcher.find()) {
        ModifierList modList = new ModifierList();
        ModifierList addModList =
            ModifierDatabase.splitModifiers(ModifierDatabase.parseModifier(matcher.group(1)));
        for (ModifierList.ModifierValue modifier : addModList) {
          modList.addToModifier(modifier);
        }
        addModList =
            ModifierDatabase.splitModifiers(ModifierDatabase.parseModifier(matcher.group(2)));
        for (ModifierList.ModifierValue modifier : addModList) {
          modList.addToModifier(modifier);
        }
        Preferences.setString("_voteModifier", modList.toString());
      }
      if (Preferences.getString("_voteMonster").equals("")) {
        String monster = null;
        matcher = VOTE_SPEECH_PATTERN.matcher(responseText);
        if (matcher.find()) {
          String party = matcher.group(1);
          String speech = matcher.group(2);
          if (party.contains("Pork Elf Historical Preservation Party")) {
            if (speech.contains("strict curtailing of unnatural modern technologies")) {
              monster = "government bureaucrat";
            } else if (speech.contains("reintroduce Pork Elf DNA")) {
              monster = "terrible mutant";
            } else if (speech.contains("kingdom-wide seance")) {
              monster = "angry ghost";
            } else if (speech.contains("very interested in snakes")) {
              monster = "annoyed snake";
            } else if (speech.contains("lots of magical lard")) {
              monster = "slime blob";
            }
          } else if (party.contains("Clan Ventrilo")) {
            if (speech.contains("bringing this blessing to the entire population")) {
              monster = "slime blob";
            } else if (speech.contains("see your deceased loved ones again")) {
              monster = "angry ghost";
            } else if (speech.contains("stronger and more vigorous")) {
              monster = "terrible mutant";
            } else if (speech.contains("implement healthcare reforms")) {
              monster = "government bureaucrat";
            } else if (speech.contains("flavored drink in a tube")) {
              monster = "annoyed snake";
            }
          } else if (party.contains("Bureau of Efficient Government")) {
            if (speech.contains("graveyards are a terribly inefficient use of space")) {
              monster = "angry ghost";
            } else if (speech.contains("strictly enforced efficiency laws")) {
              monster = "government bureaucrat";
            } else if (speech.contains("distribute all the medications for all known diseases ")) {
              monster = "terrible mutant";
            } else if (speech.contains("introduce an influx of snakes")) {
              monster = "annoyed snake";
            } else if (speech.contains("releasing ambulatory garbage-eating slimes")) {
              monster = "slime blob";
            }
          } else if (party.contains("Scions of Ich'Xuul'kor")) {
            if (speech.contains("increase awareness of our really great god")) {
              monster = "terrible mutant";
            } else if (speech.contains("hunt these evil people down")) {
              monster = "government bureaucrat";
            } else if (speech.contains("sound of a great hissing")) {
              monster = "annoyed snake";
            } else if (speech.contains("make things a little bit more like he's used to")) {
              monster = "slime blob";
            } else if (speech.contains("kindness energy")) {
              monster = "angry ghost";
            }
          } else if (party.contains("Extra-Terrific Party")) {
            if (speech.contains("wondrous chemical")) {
              monster = "terrible mutant";
            } else if (speech.contains("comprehensive DNA harvesting program")) {
              monster = "government bureaucrat";
            } else if (speech.contains("mining and refining processes begin")) {
              monster = "slime blob";
            } else if (speech.contains("warp engines will not destabilize")) {
              monster = "angry ghost";
            } else if (speech.contains("breeding pair of these delightful creatures")) {
              monster = "annoyed snake";
            }
          }
        }
        if (monster != null) {
          Preferences.setString("_voteMonster", monster);
        }
      }
    }
  }
}
