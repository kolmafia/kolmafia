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

  /** List of possible modes */
  public abstract Set<String> getModes();
}
