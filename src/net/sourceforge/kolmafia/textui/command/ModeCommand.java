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
  boolean validate(final String command, final String parameters);

  /** List of possible modes */
  Set<String> getModes();

  /** Mode name for the given parameters, resolving aliases if needed */
  default String normalize(final String parameters) {
    return parameters.trim().toLowerCase();
  }
}
