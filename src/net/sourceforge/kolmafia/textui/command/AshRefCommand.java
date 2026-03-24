package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public class AshRefCommand extends AbstractCommand {
  public AshRefCommand() {
    this.usage = " [<filter>] - summarize ASH built-in functions [matching filter].";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    Formatting.showFunctions(RuntimeLibrary.getFunctions(), parameters.toLowerCase(), true);
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

    public static void showFunctions(
        final FunctionList functions, final String filter, boolean addLinks) {
      addLinks = addLinks && StaticEntity.isGUIRequired();

      if (functions.isEmpty()) {
        RequestLogger.printLine("No functions in your current namespace.");
        return;
      }

      for (Function func : functions) {
        boolean matches = filter.isEmpty();

        if (!matches) {
          matches = func.getName().toLowerCase().contains(filter);
        }

        if (!matches) {
          for (VariableReference ref : func.getVariableReferences()) {
            String refType = ref.getType().toString();
            matches |= refType != null && refType.contains(filter);
          }
        }

        if (!matches) {
          continue;
        }

        if (func instanceof LibraryFunction lf) {
          var docBlock = formatDocBlock(lf);
          if (docBlock != null) {
            RequestLogger.printHtml(docBlock);
          }
        }

        StringBuilder signature = new StringBuilder();

        signature.append(func.getType());
        signature.append(" ");
        if (addLinks) {
          signature.append("<a href='https://wiki.kolmafia.us/index.php?title=");
          signature.append(func.getName());
          signature.append("'>");
        }
        signature.append(func.getName());
        if (addLinks) {
          signature.append("</a>");
        }
        signature.append("( ");

        String sep = "";
        for (VariableReference var : func.getVariableReferences()) {
          signature.append(sep);
          sep = ", ";

          signature.append(var.getRawType());

          if (var.getName() != null) {
            signature.append(" ");
            signature.append(var.getName());
          }
        }

        signature.append(" )");

        RequestLogger.printHtml(signature.toString());
      }
    }
  }
}
