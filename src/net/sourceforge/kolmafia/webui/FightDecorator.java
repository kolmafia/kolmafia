package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.DadManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.UnusualConstructManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FightDecorator {
  private static final Pattern SKILL_FORM_PATTERN =
      Pattern.compile("<form name=skill.*?</form>", Pattern.DOTALL);
  private static final Pattern SKILL_OPTION_PATTERN =
      Pattern.compile("<option value=\\\"(\\d+)\\\".*?</option>", Pattern.DOTALL);

  private FightDecorator() {}

  public static final void selectSkill(final StringBuffer buffer, final String skill) {
    // Extract the "skill" form from the buffer
    Matcher matcher = SKILL_FORM_PATTERN.matcher(buffer);
    if (!matcher.find()) {
      return;
    }

    // Find the desired skill
    String oldForm = matcher.group(0);
    String search = ">" + skill;
    if (!oldForm.contains(search)) {
      return;
    }

    // Found it.
    StringBuffer newForm = new StringBuffer(oldForm);

    // If a skill is already selected, deselect it
    StringUtilities.globalStringDelete(newForm, "selected");

    // ... which may have moved the desired skill
    // Select the skill
    newForm.insert(newForm.indexOf(search), " selected");

    // Replace the skill form with the munged version
    StringUtilities.singleStringReplace(buffer, oldForm, newForm.toString());
  }

  public static final void decorateMonster(final StringBuffer buffer) {
    // If we won the fight and got the volcano map, force a topmenu
    // refresh so that the "volcano" link is there.
    if (buffer.indexOf("WINWINWIN") != -1
        && buffer.indexOf("secret tropical island volcano lair map") != -1) {
      RequestEditorKit.addTopmenuRefresh(buffer);
    }

    if (!Preferences.getBoolean("relayShowSpoilers")) {
      return;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return;
    }
    String name = monster.getName().toLowerCase();
    if (name.equals("dad sea monkee")) {
      FightDecorator.decorateDadSeaMonkee(buffer);
      return;
    }
    if (name.endsWith("balldodger")) {
      FightDecorator.decorateBallDodger(buffer);
      return;
    }
    if (name.endsWith("bladeswitcher")) {
      FightDecorator.decorateBladeSwitcher(buffer);
      return;
    }
    if (name.endsWith("netdragger")) {
      FightDecorator.decorateNetDragger(buffer);
      return;
    }
    if (name.equals("falls-from-sky")) {
      FightDecorator.decorateFallsFromSky(buffer);
      return;
    }
    if (name.equals("writing desk")) {
      FightDecorator.decorateWritingDesk(buffer);
      return;
    }
    if (name.equals("unusual construct")) {
      FightDecorator.decorateUnusualConstruct(buffer);
    }
    if (name.equals("performer of actions")
        || name.equals("thinker of thoughts")
        || name.equals("perceiver of sensations")) {
      FightDecorator.decorateMachineTunnelFight(name, buffer);
      return;
    }

    if (FightRequest.isSourceAgent(monster)) {
      FightDecorator.decorateSourceAgent(buffer);
      return;
    }
  }

  public static final void decorateLocation(final StringBuffer buffer) {
    if (!Preferences.getBoolean("relayShowSpoilers")) {
      return;
    }

    int adventure = KoLAdventure.lastAdventureId();

    switch (adventure) {
      case AdventurePool.HAUNTED_KITCHEN:
        FightDecorator.decorateHauntedKitchen(buffer);
        break;

      case AdventurePool.TRAINING_SNOWMAN:
        FightDecorator.decorateSnojo(buffer);
        break;

      case AdventurePool.NEVERENDING_PARTY:
        FightDecorator.decorateParty(buffer);
        break;
    }
  }

  private static void decorateDadSeaMonkee(final StringBuffer buffer) {
    int round = FightRequest.currentRound;
    if (round < 1 || round > 10) {
      return;
    }

    DadManager.Element element = DadManager.weakness(round);
    if (element == DadManager.Element.NONE) {
      return;
    }

    String spell = DadManager.elementToSpell(element);
    if (spell == null) {
      return;
    }

    FightDecorator.selectSkill(buffer, spell);
  }

  public static final void decorateMachineTunnelFight(
      final String monster, final StringBuffer buffer) {
    if (monster.equals("thinker of thoughts")) {
      RequestEditorKit.selectOption(
          buffer, "whichitem", String.valueOf(ItemPool.ABSTRACTION_ACTION));
    } else if (monster.equals("performer of actions")) {
      RequestEditorKit.selectOption(
          buffer, "whichitem", String.valueOf(ItemPool.ABSTRACTION_SENSATION));
    } else if (monster.equals("perceiver of sensations")) {
      RequestEditorKit.selectOption(
          buffer, "whichitem", String.valueOf(ItemPool.ABSTRACTION_THOUGHT));
    }
  }

  private static void decorateBallDodger(final StringBuffer buffer) {
    // Looks like he's trying to gain an advantage over you...
    if (buffer.indexOf("<b>gain</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Net Gain");
      return;
    }

    // He gets a crazy look in his eyes -- like he's about to experience a serious loss of
    // control...
    if (buffer.indexOf("<b>loss</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Net Loss");
      return;
    }

    // His facial features take on an ominous neutrality.
    if (buffer.indexOf("<b>neutrality</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Net Neutrality");
      return;
    }
  }

  private static void decorateBladeSwitcher(final StringBuffer buffer) {
    // He begins to bust an especially dope move with his switchblade.
    if (buffer.indexOf("<b>bust</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Ball Bust");
      return;
    }

    // He pauses to wipe the sweat from his brow.
    if (buffer.indexOf("<b>sweat</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Ball Sweat");
      return;
    }

    // He pulls a little bottle of oil out of his sack and applies it to his switchblade.
    if (buffer.indexOf("<b>sack</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Ball Sack");
      return;
    }
  }

  private static void decorateNetDragger(final StringBuffer buffer) {
    // He starts to fold his net up into some sort of a sling.
    if (buffer.indexOf("<b>sling</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Blade Sling");
      return;
    }

    // He rolls his net up and draws it back like a baseball bat.
    if (buffer.indexOf("<b>rolls</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Blade Roller");
      return;
    }

    // If you were a runner, you'd be tempted to run right now...
    if (buffer.indexOf("<b>runner</b>") != -1) {
      FightDecorator.selectSkill(buffer, "Blade Runner");
      return;
    }
  }

  private static void decorateFallsFromSky(final StringBuffer buffer) {
    // While under the effect of Chilled to the Bone,
    // Falls-From-Sky can do multiple attacks per round.
    //
    // Only the last one determines the correct response.

    int circle = buffer.lastIndexOf("begins to spin in a circle");
    int paw = buffer.lastIndexOf("begins to paw at the ground");
    int shuffle = buffer.lastIndexOf("shuffles toward you");

    if (circle > paw && circle > shuffle) {
      FightDecorator.selectSkill(buffer, "Hide Under a Rock");
      return;
    }

    if (paw > circle && paw > shuffle) {
      FightDecorator.selectSkill(buffer, "Dive Into a Puddle");
      return;
    }

    if (shuffle > circle && shuffle > paw) {
      FightDecorator.selectSkill(buffer, "Hide Behind a Tree");
      return;
    }
  }

  private static void decorateWritingDesk(final StringBuffer buffer) {
    String indexString = "any necklaces.";
    int index = buffer.indexOf(indexString);
    if (index == -1) {
      return;
    }

    index += indexString.length();

    buffer.insert(index, " (" + Preferences.getInteger("writingDesksDefeated") + "/5 defeated)");
  }

  private static void decorateSourceAgent(final StringBuffer buffer) {
    // Extract the "skill" form from the buffer
    Matcher skillForm = SKILL_FORM_PATTERN.matcher(buffer);
    if (!skillForm.find()) {
      return;
    }
    Matcher option = SKILL_OPTION_PATTERN.matcher(skillForm.group(0));
    while (option.find()) {
      // Remove skills not starting with 21 as they can't be used
      // Allow new source terminal skills until we know which are usable
      int skillId = StringUtilities.parseInt(option.group(1));
      if (!SkillDatabase.sourceAgentSkill(skillId)) {
        StringUtilities.singleStringDelete(buffer, option.group(0));
      }
    }
  }

  private static void decorateHauntedKitchen(final StringBuffer buffer) {
    if (InventoryManager.hasItem(ItemPool.BILLIARDS_KEY)) {
      // Don't show progress on the turn where the key is received
      return;
    }
    // The kitchen's resident flame-belching demon oven kicks into serious overdrive,
    // but you manage to tolerate the heat long enough to search through X drawers.

    // The garbage disposal turns itself on, filling the kitchen with an indescribably foul
    // odor, but you manage to tolerate it long enough to search through X drawers.
    String indexString = "drawers.";
    int index = buffer.indexOf(indexString);
    if (index == -1) {
      // You manage to dig through a single drawer looking for the key, but the
      // garbage disposal turns itself on, releasing a terrible, terrible smell.
      // It drives you back out into the hallway.
      indexString = "hallway.";
      index = buffer.indexOf(indexString);
      if (index == -1) {
        // You manage to dig through a single drawer looking for the key,
        // but the constant demonic flames belching out of the oven results
        // in the kitchen getting too hot for you, and you have to get out of it.
        indexString = "out of it.";
        index = buffer.indexOf(indexString);
        if (index == -1) {
          return;
        }
      }
    }

    index += indexString.length();

    int checked = Preferences.getInteger("manorDrawerCount");
    StringBuilder insertBuffer = new StringBuilder();
    insertBuffer.append(" (").append(checked).append("/21 searched");
    if (checked >= 21) {
      insertBuffer.append(", key next combat");
    }
    insertBuffer.append(")");

    buffer.insert(index, insertBuffer);
  }

  private static void decorateParty(final StringBuffer buffer) {
    String indexString = "Adventure Again (The Neverending Party)";
    int index = buffer.indexOf(indexString);
    if (index == -1) return;

    index += indexString.length();

    int turns = Preferences.getInteger("_neverendingPartyFreeTurns");
    StringBuilder insertBuffer = new StringBuilder();
    insertBuffer
        .append(" (")
        .append(turns)
        .append(" free fight")
        .append(turns == 1 ? "" : "s")
        .append(" used)");
    buffer.insert(index, insertBuffer);
  }

  private static void decorateSnojo(final StringBuffer buffer) {
    String indexString = "Adventure Again (The X-32-F Combat Training Snowman)";
    int index = buffer.indexOf(indexString);
    if (index == -1) return;

    index += indexString.length();

    int turns = Preferences.getInteger("_snojoFreeFights");
    StringBuilder insertBuffer = new StringBuilder();
    insertBuffer
        .append(" (")
        .append(turns)
        .append(" free fight")
        .append(turns == 1 ? "" : "s")
        .append(" used)");
    buffer.insert(index, insertBuffer);
  }

  private static void decorateUnusualConstruct(final StringBuffer buffer) {
    int disc = UnusualConstructManager.disc();

    if (disc == 0) {
      // Change to a bogus result to avoid being misleading, until full support is added.
      disc = ItemPool.SEAL_TOOTH;
    }

    RequestEditorKit.selectOption(buffer, "whichitem", String.valueOf(disc));
  }

  private static final String END_TAG = "<a name=\"end\"></a>";

  public static final void decorateEndOfFight(final StringBuffer buffer) {
    if (buffer.indexOf("fight.php") != -1) {
      return;
    }

    // If this was a Time-Spinner monster
    if (GenericRequest.itemMonster != null && GenericRequest.itemMonster.equals("Time-Spinner")) {
      int inventoryLink =
          buffer.indexOf("<Center><a href=\"inventory.php\">Back to your Inventory</a></center>");
      if (inventoryLink != -1) {
        // inv_use.php?whichitem=9104&pwd
        String link =
            "<center><a href=\"inv_use.php?whichitem=9104&pwd="
                + GenericRequest.passwordHash
                + "\">Back to your Time-Spinner</a></center><br>";
        buffer.insert(inventoryLink, link);
      }
      return;
    }

    int uses = 0;
    if (GenericRequest.itemMonster != null
        && GenericRequest.itemMonster.equals("lynyrd snare")
        && InventoryManager.getCount(ItemPool.LYNYRD_SNARE) > 0
        && (uses = Preferences.getInteger("_lynyrdSnareUses")) < 3) {
      int index = buffer.indexOf(END_TAG);
      if (index != -1) {
        // inv_use.php?whichitem=7204&pwd
        String link =
            "<p><a href=\"inv_use.php?whichitem=7204&pwd="
                + GenericRequest.passwordHash
                + "\">Use another lynyrd snare ("
                + uses
                + "/3 lynyrds fought today)</a>";
        buffer.insert(index + END_TAG.length(), link);
      }
      return;
    }
  }
}
