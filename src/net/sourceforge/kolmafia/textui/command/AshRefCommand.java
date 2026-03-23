package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public class AshRefCommand extends AbstractCommand {
  public AshRefCommand() {
    this.usage = " [<filter>] - summarize ASH built-in functions [matching filter].";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    KoLmafiaASH.showExistingFunctions(parameters);
  }

  public static class Formatting {
    private Formatting() {}

    private static String docColor() {
      return KoLmafiaGUI.isDarkTheme() ? "#888888" : "#666666";
    }

    public static String formatDocBlock(LibraryFunction func) {
      var description = func.getDescription();
      var params = func.getVariableReferences();

      var lines = new ArrayList<String>();

      if (description != null && !description.isEmpty()) {
        lines.add(description);
      }

      for (VariableReference var : params) {
        var paramDescription = var.getDescription();
        if (paramDescription != null && !paramDescription.isEmpty()) {
          lines.add("@param " + var.getName() + " " + paramDescription);
        }
      }

      if (lines.isEmpty()) {
        return null;
      }

      var color = docColor();
      var sb = new StringBuilder();
      sb.append("<font color='").append(color).append("'>");

      if (lines.size() == 1) {
        sb.append("/** ").append(lines.get(0)).append(" */");
      } else {
        sb.append("/**");
        for (var line : lines) {
          sb.append("<br>&nbsp;* ").append(line);
        }
        sb.append("<br>&nbsp;*/");
      }

      sb.append("</font>");
      return sb.toString();
    }
  }
}
