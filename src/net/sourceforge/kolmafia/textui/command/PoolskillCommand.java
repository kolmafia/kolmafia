package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;

public class PoolskillCommand extends AbstractCommand {
  public PoolskillCommand() {
    this.usage = " - display estimated Pool skill.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    KoLCharacter.estimatedPoolSkill(true);
  }
}
