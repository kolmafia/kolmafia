package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

public class CondRefCommand extends AbstractCommand {
  public CondRefCommand() {
    this.usage = " - list <condition>s usable with if/while commands.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    RequestLogger.printLine(
        "<table border=2>"
            + "<tr><td colspan=3>today | tomorrow is mus | mys | mox day</td></tr>"
            + "<tr><td colspan=3>class is [not] sauceror | <i>etc.</i></td></tr>"
            + "<tr><td colspan=3>skill list contains | lacks <i>skill</i></td></tr>"
            + "<tr><td>level<br>health<br>mana<br>meat<br>adventures<br>"
            + "inebriety | drunkenness<br>muscle<br>mysticality<br>moxie<br>"
            + "worthless item<br>stickers<br><i>item</i><br><i>effect</i></td>"
            + "<td>=<br>==<br>&lt;&gt;<br>!=<br>&lt;<br>&lt;=<br>&gt;<br>&gt;=</td>"
            + "<td><i>number</i><br><i>number</i>%&nbsp;(health/mana only)<br>"
            + "<i>item</i> (qty in inventory)<br><i>effect</i> (turns remaining)</td>"
            + "</tr></table><br>");
  }
}
