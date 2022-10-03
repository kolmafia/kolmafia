package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.session.LocketManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LocketRequest extends GenericRequest {

  private final MonsterData monster;

  public LocketRequest() {
    super("inventory.php?reminisce=1", false);
    this.monster = null;
  }

  public LocketRequest(final MonsterData monster) {
    super("choice.php");
    this.addFormField("whichchoice", "1463");
    this.addFormField("option", "1");
    this.addFormField("mid", String.valueOf(monster.getId()));
    this.monster = monster;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    // All of these various checks are also in the "reminisce" command, but
    // this request doesn't have to be invoked only from the command.

    if (!LocketManager.own()) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You do not own a combat lover's locket.");
      return;
    }

    if (LocketManager.getFoughtMonsters().size() >= 3) {
      // You can't even look at your locket if you've reminisced three times.
      //
      // So, don't try - but only generate an error if you are trying to
      // actually get another monster.
      if (monster != null) {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR, "You can only reminisce thrice daily.");
      }
      return;
    }

    if (monster != null) {
      if (LocketManager.foughtMonster(monster)) {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR,
            "You've already reminisced "
                + monster.getArticle()
                + " "
                + monster.getName()
                + " today.");
        return;
      }

      if (!LocketManager.remembersMonster(monster)) {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR,
            "You do not have a picture of "
                + monster.getArticle()
                + " "
                + monster.getName()
                + " in your locket.");
        return;
      }
    }

    // Ensure the locket is equipped or in inventory. This should not fail.
    if (!LocketManager.retrieve()) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR,
          "Internal error: failed to retrieve combat lover's locket");
      return;
    }

    if (monster != null) {
      // Open the locket.
      (new LocketRequest()).run();
    }

    super.run();
  }

  private static final Pattern URL_MID_PATTERN = Pattern.compile("mid=(\\d+)");

  private static MonsterData extractMonsterFromURL(final String urlString) {
    Matcher matcher = URL_MID_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return null;
    }
    int mid = StringUtilities.parseInt(matcher.group(1));
    return MonsterDatabase.findMonsterById(mid);
  }

  @Override
  public int getAdventuresUsed() {
    return LocketRequest.getAdventuresUsed(this.monster);
  }

  public static int getAdventuresUsed(String urlString) {
    MonsterData monster = extractMonsterFromURL(urlString);
    return LocketRequest.getAdventuresUsed(monster);
  }

  private static int getAdventuresUsed(MonsterData monster) {
    return (monster != null) ? 1 : 0;
  }
}
