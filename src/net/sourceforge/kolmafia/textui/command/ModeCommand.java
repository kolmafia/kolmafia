package net.sourceforge.kolmafia.textui.command;

import java.util.Set;

public interface ModeCommand {
  /**
   * Basic side-effect-less command validation
   *
   * @param command Command string
   * @param parameters Parameters as a string
   * @return Whether command is valid
   */
  public boolean validate(final String command, final String parameters);

  /**
   * Normalise parameters to the value expected in modifiers.txt
   *
   * <p>For example, one might specify "ml" to the "umbrella" command, but the value to compare in
   * state would be "broken".
   *
   * @param parameters Input parameters
   * @return Appropriate state value
   */
  public String normalize(final String parameters);

  /** List of possible modes */
  public abstract Set<String> getModes();
}
