package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase.Candy;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.SweetSynthesisRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SynthesizeCommand extends AbstractCommand {
  public SynthesizeCommand() {
    this.usage = "[?] CANDY1, CANDY2 or [?] EFFECT - get an effect via Sweet Synthesis";
  }

  private static class Effect {
    public final String name;
    public final String description;
    public final int effectId;
    public final String canonicalName;

    public Effect(final String name, final String description) {
      this.name = name;
      this.description = description;
      this.effectId = EffectDatabase.getEffectId(name);
      this.canonicalName = StringUtilities.getCanonicalName(name);
    }

    public String getName() {
      return this.name;
    }

    public String getDescription() {
      return this.description;
    }

    public int getEffectId() {
      return this.effectId;
    }

    public String getCanonicalName() {
      return this.canonicalName;
    }
  }

  static final Effect[] EFFECTS = {
    new Effect("Synthesis: Hot", "Hot Res +9"),
    new Effect("Synthesis: Cold", "Cold Res +9"),
    new Effect("Synthesis: Pungent", "Stench Res +9"),
    new Effect("Synthesis: Scary", "Spooky Res +9"),
    new Effect("Synthesis: Greasy", "Sleaze Res +9"),
    new Effect("Synthesis: Strong", "Mus +300%"),
    new Effect("Synthesis: Smart", "Mys +300%"),
    new Effect("Synthesis: Cool", "Mox +300%"),
    new Effect("Synthesis: Hardy", "Max HP +300%"),
    new Effect("Synthesis: Energy", "Max MP +300%"),
    new Effect("Synthesis: Greed", "Meat +300%"),
    new Effect("Synthesis: Collection", "Item +150%"),
    new Effect("Synthesis: Movement", "Mus Exp +50%"),
    new Effect("Synthesis: Learning", "Mys Exp +50%"),
    new Effect("Synthesis: Style", "Mox Exp +50%"),
  };

  private static Effect findEffectByCanonicalName(final String canonicalName) {
    for (Effect effect : EFFECTS) {
      if (effect.canonicalName.equals(canonicalName)) {
        return effect;
      }
    }
    return null;
  }

  private static Effect findEffectByEffectId(final int effectId) {
    for (Effect effect : EFFECTS) {
      if (effect.effectId == effectId) {
        return effect;
      }
    }
    return null;
  }

  public static String[] CANONICAL_EFFECT_NAMES = new String[EFFECTS.length];

  static {
    // Save canonical name
    for (int i = 0; i < EFFECTS.length; ++i) {
      CANONICAL_EFFECT_NAMES[i] = EFFECTS[i].canonicalName;
    }

    Arrays.sort(CANONICAL_EFFECT_NAMES);
  }

  public static final float AGE_LIMIT = (60.0f * 60.0f) / 86400.0f; // One hour

  private void updatePrices(final AdventureResult candy1, final AdventureResult candy2) {
    StoreManager.getMallPrice(candy1, AGE_LIMIT);
    StoreManager.getMallPrice(candy2, AGE_LIMIT);
  }

  private void analyzeCandy(final AdventureResult candy) {
    StringBuilder message = new StringBuilder();
    int itemId = candy.getItemId();
    String candyType = CandyDatabase.getCandyType(itemId);

    if (candyType == CandyDatabase.NONE) {
      message.append("Item '");
      message.append(candy.getName());
      message.append("' has candy type ");
      message.append(candyType);
      KoLmafia.updateDisplay(message.toString());
      return;
    }

    int count = InventoryManager.getAccessibleCount(candy);
    boolean tradeable = ItemDatabase.isTradeable(itemId);
    int cost = !tradeable ? 0 : StoreManager.getMallPrice(candy, AGE_LIMIT);

    message.append("Item '");
    message.append(candy.getName());
    message.append("' is a ");
    if (!tradeable) {
      message.append("non-tradeable ");
    }
    message.append(candyType);
    message.append(" candy. You have ");
    message.append(count);
    message.append(" available to you");
    if (!tradeable) {
      message.append(".");
    } else {
      message.append(" without using the mall, where it costs ");
      message.append(cost);
      message.append(" Meat.");
    }

    KoLmafia.updateDisplay(message.toString());
  }

  @Override
  public void run(final String cmd, String parameters) {
    boolean checking = KoLmafiaCLI.isExecutingCheckOnlyCommand;
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

    if (parameters.equals("")) {
      return;
    }

    Effect effect = null;
    AdventureResult candy1 = null;
    AdventureResult candy2 = null;
    int count = 1;

    if (parameters.contains(" ")) {
      int space = parameters.indexOf(" ");
      String token = parameters.substring(0, space);
      if (StringUtilities.isNumeric(token)) {
        count = StringUtilities.parseInt(token);
        parameters = parameters.substring(space + 1).trim();
      }
    }

    if (parameters.contains(",")) {
      // Two candies

      Match filter = Match.CANDY;

      AdventureResult[] itemList = ItemFinder.getMatchingItemList(parameters, true, null, filter);

      if (!KoLmafia.permitsContinue()) {
        return;
      }

      int length = itemList.length;

      candy1 = (length > 0) ? itemList[0] : null;
      candy2 = (length > 1) ? itemList[1] : (length == 1 && candy1.getCount() == 2) ? candy1 : null;

      if (candy1 == null || candy2 == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You must specify two candies");
        return;
      }

      int effectId = CandyDatabase.synthesisResult(candy1.getItemId(), candy2.getItemId());
      if (effectId == -1) {
        KoLmafia.updateDisplay("Unknown result from synthesizing those two candies");
        // Allow the attempt. For science!
      } else {
        effect = SynthesizeCommand.findEffectByEffectId(effectId);
      }

      // If the user specifies two candies, the candy blacklist is not used
    } else {
      // Effect

      List<String> matchingEffects =
          StringUtilities.getMatchingNames(CANONICAL_EFFECT_NAMES, parameters.trim());
      if (matchingEffects.isEmpty()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown effect: " + parameters);
        return;
      }

      if (matchingEffects.size() > 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Ambiguous effect name: " + parameters);
        RequestLogger.printList(matchingEffects);
        return;
      }

      effect = SynthesizeCommand.findEffectByCanonicalName(matchingEffects.get(0));

      // If the user wants us to pick two candies, use the blacklist
      CandyDatabase.loadBlacklist();

      Candy[] pair = CandyDatabase.synthesisPair(effect.effectId);
      if (pair.length == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Can't find a pair of candies for that effect");
        return;
      }

      candy1 = ItemPool.get(pair[0].getItemId(), 1);
      candy2 = ItemPool.get(pair[1].getItemId(), 1);
    }

    int itemId1 = candy1.getItemId();
    int itemId2 = candy2.getItemId();

    if (checking) {
      updatePrices(candy1, candy2);

      analyzeCandy(candy1);
      analyzeCandy(candy2);

      StringBuilder message = new StringBuilder();
      message.append("Synthesizing those two candies will give you 30 turns of ");
      if (effect != null) {
        message.append(effect.name);
        message.append(" (");
        message.append(effect.description);
        message.append(")");
      } else {
        message.append("an unknown effect");
      }

      KoLmafia.updateDisplay(message.toString());
      return;
    }

    SweetSynthesisRequest request = new SweetSynthesisRequest(count, itemId1, itemId2);
    RequestThread.postRequest(request);
    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("Done!");
    }
  }
}
