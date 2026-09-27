package net.sourceforge.kolmafia.combat;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import net.sourceforge.kolmafia.KoLmafia;

public class CustomCombatStrategy extends DefaultMutableTreeNode {
  private final String name;

  private int actionCount;
  private int[] actionOffsets;

  public CustomCombatStrategy(final String name) {
    super(name, true);

    this.name = name;

    this.resetActionCount();
  }

  public String getName() {
    return this.name;
  }

  @Override
  public void removeAllChildren() {
    this.resetActionCount();

    super.removeAllChildren();
  }

  public void resetActionCount() {
    this.actionCount = 0;
    this.actionOffsets = null;
  }

  public int getActionCount(CustomCombatLookup lookup, Set<String> seen) {
    // Ignore any call to a section that results in a loop

    if (seen.contains(this.name)) {
      KoLmafia.abortAfter("CCS aborted due to recursive section reference: " + this.name);
      return 0;
    }

    seen.add(this.name);

    // If we've already computed the length, return the length

    if (actionOffsets != null) {
      return this.actionCount;
    }

    int childCount = getChildCount();

    this.actionCount = 0;
    this.actionOffsets = new int[childCount];

    for (int i = 0; i < childCount; ++i) {
      this.actionOffsets[i] = this.actionCount;

      CustomCombatAction actionNode = (CustomCombatAction) getChildAt(i);
      String sectionReference = actionNode.getSectionReference();

      CustomCombatStrategy strategy = null;

      if (sectionReference != null) {
        strategy = lookup.getStrategy(sectionReference);
      }

      if (strategy != null) {
        this.actionCount += strategy.getActionCount(lookup, seen);
      } else if (sectionReference != null) {
        KoLmafia.abortAfter("CCS aborted due to invalid section reference: " + sectionReference);
      } else {
        ++this.actionCount;
      }
    }

    return this.actionCount;
  }

  public String getAction(
      final CustomCombatLookup lookup, final int roundIndex, boolean allowMacro) {
    int childCount = getChildCount();

    if (childCount == 0) {
      return "attack";
    }

    getActionCount(lookup, new HashSet<>());

    for (int i = 0; i < childCount; ++i) {
      if (this.actionOffsets[i] > roundIndex) {
        CustomCombatAction actionNode = (CustomCombatAction) getChildAt(i - 1);
        String sectionReference = actionNode.getSectionReference();

        if (sectionReference != null) {
          int offset = (i > 0) ? this.actionOffsets[i - 1] : 0;
          CustomCombatStrategy strategy = lookup.getStrategy(sectionReference);

          if (strategy != null) {
            return strategy.getAction(lookup, roundIndex - offset, allowMacro);
          }

          KoLmafia.abortAfter("CCS aborted due to invalid section reference: " + sectionReference);
          return "abort";
        }

        if (!allowMacro && actionNode.isMacro()) {
          return "skip";
        }

        return actionNode.getAction();
      }
    }

    CustomCombatAction actionNode = (CustomCombatAction) getLastChild();
    String sectionReference = actionNode.getSectionReference();

    if (sectionReference != null) {
      CustomCombatStrategy strategy = lookup.getStrategy(sectionReference);

      if (strategy != null) {
        return strategy.getAction(
            lookup, roundIndex - this.actionOffsets[childCount - 1], allowMacro);
      }

      KoLmafia.abortAfter("CCS aborted due to invalid section reference: " + sectionReference);
      return "abort";
    }

    if (!allowMacro && actionNode.isMacro()) {
      return "skip";
    }

    return actionNode.getAction();
  }

  public void addCombatAction(
      final int roundIndex, final String indent, final String combatAction, boolean isMacro) {
    int currentIndex = getChildCount();

    if (roundIndex <= currentIndex) {
      return;
    }

    addRepeatActions(roundIndex, indent);

    CustomCombatAction node = new CustomCombatAction(roundIndex, indent, combatAction, isMacro);

    this.resetActionCount();

    super.add(node);
  }

  private void addRepeatActions(final int roundIndex, final String indent) {
    int currentIndex = getChildCount();

    if (roundIndex <= currentIndex) {
      return;
    }

    String repeatAction = "attack with weapon";
    boolean isMacro = false;

    if (currentIndex > 0) {
      CustomCombatAction node = (CustomCombatAction) getLastChild();

      repeatAction = node.getAction();
      isMacro = node.isMacro();
    }

    for (int i = currentIndex + 1; i < roundIndex; ++i) {
      CustomCombatAction node = new CustomCombatAction(i, indent, repeatAction, isMacro);

      super.add(node);
    }
  }

  public void store(PrintStream writer) {
    writer.println("[ " + this.name + " ]");

    int childCount = getChildCount();

    for (int i = 0; i < childCount; ++i) {
      CustomCombatAction action = (CustomCombatAction) getChildAt(i);

      action.store(writer);
    }

    writer.println();
  }
}
