package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.BeerPongRequest;

public class PirateInsultsCommand extends AbstractCommand {
  {
    this.usage = " - list the pirate insult comebacks you know.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    KoLCharacter.ensureUpdatedPirateInsults();

    RequestLogger.printLine();
    RequestLogger.printLine("Known insults:");

    int count = 0;
    for (int i = 1; i <= BeerPongRequest.VALID_PIRATE_INSULTS; ++i) {
      String retort = BeerPongRequest.knownPirateRetort(i);
      if (retort != null) {
        if (count == 0) {
          RequestLogger.printLine();
        }
        count += 1;

        RequestLogger.printLine(retort);
      }
    }

    float odds = BeerPongRequest.pirateInsultOdds(count) * 100.0f;

    if (count == 0) {
      RequestLogger.printLine("None.");
    }

    RequestLogger.printLine();
    RequestLogger.printLine(
        "Since you know "
            + count
            + " insult"
            + (count == 1 ? "" : "s")
            + ", you have a "
            + KoLConstants.FLOAT_FORMAT.format(odds)
            + "% chance of winning at Insult Beer Pong.");
  }
}
