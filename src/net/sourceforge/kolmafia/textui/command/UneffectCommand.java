package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.request.UneffectRequest;

public class UneffectCommand extends AbstractCommand {
  public UneffectCommand() {
    this.usage = "[?] <effect> [, <effect>]... - remove effects using appropriate means.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.contains(",")) {
      // See if the whole parameter, comma and all,
      // matches an effect.
      if (EffectDatabase.getEffectId(parameters.trim()) == -1) {
        // Nope. It is a list of effects. Assume that
        // none contain a comma.
        String[] effects = parameters.split("\\s*,\\s*");
        for (int i = 0; i < effects.length; ++i) {
          this.run("uneffect", effects[i]);
        }

        return;
      }
    }

    List<String> matchingEffects = EffectDatabase.getMatchingNames(parameters.trim());
    if (matchingEffects.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown effect: " + parameters);
      return;
    }

    if (matchingEffects.size() > 1) {
      // If there's only one shruggable buff on the list, then
      // that's probably the one the player wants.

      int shruggableCount = 0;

      String buffToCheck;
      AdventureResult buffToRemove = null;

      for (int i = 0; i < matchingEffects.size(); ++i) {
        buffToCheck = matchingEffects.get(i);
        int effectId = EffectDatabase.getEffectId(buffToCheck);
        if (UneffectRequest.isShruggable(effectId)) {
          ++shruggableCount;
          buffToRemove = EffectPool.get(effectId);
        }
      }

      if (shruggableCount == 1) {
        if (KoLConstants.activeEffects.contains(buffToRemove)) {
          if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
            RequestLogger.printLine(buffToRemove.toString());
            return;
          }

          RequestThread.postRequest(new UneffectRequest(buffToRemove));
        }

        return;
      }

      KoLmafia.updateDisplay(MafiaState.ERROR, "Ambiguous effect name: " + parameters);
      RequestLogger.printList(matchingEffects);

      return;
    }

    int effectId = EffectDatabase.getEffectId(matchingEffects.get(0));
    AdventureResult effect = EffectPool.get(effectId);

    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printLine(effect.toString());
      return;
    }

    if (KoLConstants.activeEffects.contains(effect)) {
      RequestThread.postRequest(new UneffectRequest(effect));
    }
  }
}
