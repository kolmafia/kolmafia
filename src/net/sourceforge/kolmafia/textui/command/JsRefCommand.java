package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.PluralValueType;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public class JsRefCommand extends AbstractCommand {
  public JsRefCommand() {
    this.usage = " [<filter>] - summarize JS built-in functions [matching filter].";
  }

  private String toObjectKeyType(final Type type) {
    String jsTypeName = toJavascriptTypeName(type);
    if (jsTypeName.equals("number") || jsTypeName.equals("string")) {
      return "key: " + jsTypeName;
    }

    return type.toString() + ": string";
  }

  public String toJavascriptTypeName(final Type type) {
    if (type == null || type.toString() == null) {
      return "any";
    }

    if (type instanceof PluralValueType) {
      return "readonly " + toJavascriptTypeName(((PluralValueType) type).getIndexType()) + "[]";
    }

    if (type instanceof AggregateType) {
      if (((AggregateType) type).getSize() < 0) {
        return "{ ["
            + toObjectKeyType(((AggregateType) type).getIndexType())
            + "]: "
            + toJavascriptTypeName(((AggregateType) type).getDataType())
            + " }";
      } else {
        return toJavascriptTypeName(((AggregateType) type).getDataType()) + "[]";
      }
    }

    if (DataTypes.enumeratedTypes.contains(type)) {
      return JavascriptRuntime.capitalize(type.toString());
    }

    if (type instanceof RecordType) {
      StringBuilder object = new StringBuilder("{ ");
      for (int i = 0; i < ((RecordType) type).fieldCount(); i++) {
        object
            .append(((RecordType) type).getFieldNames()[i])
            .append(": ")
            .append(toJavascriptTypeName(((RecordType) type).getFieldTypes()[i]))
            .append("; ");
      }
      return object + "}";
    }

    return switch (type.toString()) {
      case "int", "float" -> "number";
      case "buffer", "strict_string" -> "string";
      case "aggregate" -> "any";
      case "rng" -> "Rng";
      default -> type.toString();
    };
  }

  @Override
  public void run(final String cmd, String filter) {
    boolean addLinks = StaticEntity.isGUIRequired();

    List<Function> functions = JavascriptRuntime.getFunctions();

    if (functions.isEmpty()) {
      RequestLogger.printLine("No functions in your current namespace.");
      return;
    }

    filter = filter.toLowerCase();

    for (Function func : functions) {
      boolean matches = filter.isEmpty();

      String funcName = func.getName();
      String jsFuncName = JavascriptRuntime.toCamelCase(funcName);

      if (!matches) {
        matches = funcName.toLowerCase().contains(filter);
        matches |= jsFuncName.toLowerCase().contains(filter);
      }

      if (!matches) {
        for (VariableReference ref : func.getVariableReferences()) {
          String refType = ref.getType().toString();
          if (refType != null) {
            matches = refType.toLowerCase().contains(filter);
            matches |= JavascriptRuntime.toCamelCase(refType).toLowerCase().contains(filter);
          }
        }
      }

      if (!matches) {
        continue;
      }

      if (func instanceof LibraryFunction lf) {
        var docBlock = AshRefCommand.Formatting.formatDocBlock(lf);
        if (docBlock != null) {
          RequestLogger.printHtml(docBlock);
        }
      }

      StringBuilder signature = new StringBuilder();

      signature.append("function ");

      if (addLinks) {
        signature.append("<a href='https://wiki.kolmafia.us/index.php?title=");
        signature.append(funcName);
        signature.append("'>");
      }
      signature.append(jsFuncName);
      if (addLinks) {
        signature.append("</a>");
      }
      signature.append("(");

      String sep = "";
      for (VariableReference var : func.getVariableReferences()) {
        signature.append(sep);
        sep = ", ";

        if (var.getName() != null) {
          signature.append(var.getName());
          signature.append(": ");
          signature.append(toJavascriptTypeName(var.getRawType()));
        }
      }

      signature.append("): ");
      signature.append(toJavascriptTypeName(func.getType()));
      signature.append(";");

      RequestLogger.printHtml(signature.toString());
    }
  }
}
