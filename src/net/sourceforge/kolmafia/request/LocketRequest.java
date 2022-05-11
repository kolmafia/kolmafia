package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.session.LocketManager;

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
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "You can only reminisce thrice daily.");
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

  @Override
  public int getAdventuresUsed() {
    return (this.monster != null) ? 1 : 0;
  }
}
