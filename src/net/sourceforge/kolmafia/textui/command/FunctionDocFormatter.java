package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public class FunctionDocFormatter {
  private static final String DOC_COLOR_LIGHT = "#666666";
  private static final String DOC_COLOR_DARK = "#888888";

  private FunctionDocFormatter() {}

  private static String docColor() {
    return KoLmafiaGUI.isDarkTheme() ? DOC_COLOR_DARK : DOC_COLOR_LIGHT;
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
