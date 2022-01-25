package net.sourceforge.kolmafia.webui;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.NemesisManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NemesisDecorator {
  private static final String[][] SPECIAL_MOVES = {
    {
      "Breakdancing Raver",
      "Break It On Down",
      "dbNemesisSkill1",
      // Suddenly and unexpectedly, the raver drops to the
      // ground and whirls his legs around like a windmill,
      // catching you with a vicious kick to the knee. You
      // stagger backwards, barely managing to keep your
      // footing.
      //
      // The raver drops to the ground and starts spinning
      // his legs wildly. He's much too far away from you to
      // actually hit you, but it still looks pretty
      // impressive.
      "the raver drops to the ground and whirls his legs around like a windmill",
      "The raver drops to the ground and starts spinning his legs wildly",
    },
    {
      "Pop-and-Lock Raver",
      "Pop and Lock It",
      "dbNemesisSkill2",
      // The raver's movements suddenly become spastic and
      // jerky, like low-quality claymation. The weird
      // rhythmic effect is puzzling, and his final quick jab
      // to your head catches you off-guard.
      //
      // The raver's movements suddenly became spastic and
      // jerky. The weird rhythmic effect is puzzling, but
      // you make yourself feel better by calling him a spaz
      // and a jerk.
      "The raver's movements suddenly became spastic and jerky",
      "The raver's movements suddenly become spastic and jerky",
    },
    {
      "Running Man",
      "Run Like the Wind",
      "dbNemesisSkill3",
      // The raver turns around and flees. You start to give
      // chase, but stop short when you realize that he
      // hasn't actually gone anywhere at all, and that
      // you've left yourself wide open to his reverse-kick
      // to your stomach.
      //
      // The raver turns and runs away. You watch him go, and
      // soon realize he isn't actually running anywhere. He
      // glances over his shoulder, sees you watching him
      // with an eyebrow raised, and looks embarrassed.
      "You watch him go, and soon realize he isn't actually running anywhere",
      "You start to give chase, but stop short when you realize that he hasn't actually gone anywhere at all",
    },
  };

  private NemesisDecorator() {}

  public static final void useGothyHandwave(final String monster, final String responseText) {
    String setting = null;

    // See if this monster has a dance move worth studying
    for (int i = 0; i < NemesisDecorator.SPECIAL_MOVES.length; ++i) {
      String[] moves = NemesisDecorator.SPECIAL_MOVES[i];
      if (monster.equalsIgnoreCase(moves[0])) {
        setting = moves[2];
        break;
      }
    }

    // Punt if not.
    if (setting == null) {
      return;
    }

    // It seems like there's something to this guy's movements that
    // might be useful to you, if only you could find the right
    // moment to focus on.

    if (responseText.indexOf("find the right moment") != -1) {
      // Didn't use it on the right move
      return;
    }

    // Meh, you can't bring yourself to do that goofy move twice in
    // one fight. You have <i>some</i> self-respect, after all.

    if (responseText.indexOf("You have <i>some</i> self-respect") != -1) {
      // Only once per fight
      return;
    }

    NemesisManager.ensureUpdatedNemesisStatus();
    Preferences.increment(setting, 1);
  }

  private static String[] findRaver(final String monster) {
    for (int i = 0; i < NemesisDecorator.SPECIAL_MOVES.length; ++i) {
      String[] moves = NemesisDecorator.SPECIAL_MOVES[i];
      if (monster.equalsIgnoreCase(moves[0])) {
        return moves;
      }
    }

    return null;
  }

  public static boolean isRaver(final String monster) {
    return NemesisDecorator.findRaver(monster) != null;
  }

  public static final String danceMoveStatus(final String monster) {
    NemesisManager.ensureUpdatedNemesisStatus();
    String[] moves = NemesisDecorator.findRaver(monster);
    if (moves != null) {
      StringBuffer buffer = new StringBuffer();
      String skill = moves[1];
      buffer.append(skill);
      if (KoLCharacter.hasSkill(skill)) {
        buffer.append(" (<b>KNOWN</b>)");
      } else {
        String setting = moves[2];
        String current = Preferences.getString(setting);
        buffer.append(" (");
        buffer.append(current);
        buffer.append(")");
      }
      return buffer.toString();
    }

    return null;
  }

  public static final void decorateRaverFight(final StringBuffer buffer) {
    NemesisManager.ensureUpdatedNemesisStatus();

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return;
    }
    String[] moves = NemesisDecorator.findRaver(monster.getName());
    if (moves == null) {
      return;
    }

    String skill = moves[1];
    if (KoLCharacter.hasSkill(skill)) {
      return;
    }

    // A raver's special move either hits or misses and may have a
    // different message, depending
    if (moves.length > 3) {
      NemesisDecorator.decorateMove(buffer, moves[3]);
    }
    if (moves.length > 4) {
      NemesisDecorator.decorateMove(buffer, moves[4]);
    }
  }

  public static final boolean specialRaverMove(final String text) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return false;
    }
    String[] moves = NemesisDecorator.findRaver(monster.getName());
    if (moves == null) {
      return false;
    }
    if (moves.length > 3 && text.contains(moves[3])) {
      return true;
    }
    if (moves.length > 4 && text.contains(moves[4])) {
      return true;
    }
    return false;
  }

  private static void decorateMove(final StringBuffer buffer, final String move) {
    StringUtilities.singleStringReplace(buffer, move, "<font color=#DD00FF>" + move + "</font>");
  }
}
