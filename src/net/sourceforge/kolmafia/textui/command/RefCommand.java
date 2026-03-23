package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public class RefCommand {
  private RefCommand() {}

  private static String docColor() {
    return KoLmafiaGUI.isDarkTheme() ? "#888888" : "#666666";
  }

  public static String linkColor() {
    return KoLmafiaGUI.isDarkTheme() ? "#6ba5e1" : null;
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
