package net.sourceforge.kolmafia.textui.command;

public class MallSellCommand extends AbstractCommand {
  public MallSellCommand() {
    this.usage =
        " [using storage] <item> [[@] <price> [[limit] <num>]] [, <another>]... - sell in Mall.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    ShopCommand.put(parameters);
  }
}
