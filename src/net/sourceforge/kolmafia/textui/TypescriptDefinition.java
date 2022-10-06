package net.sourceforge.kolmafia.textui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.command.JsRefCommand;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TypescriptDefinition {
  public static final int VERSION = 5;
  private static String kolmafiaVersion = "0";

  private static final JsRefCommand INSTANCE = new JsRefCommand();

  private static final String combatFilterType =
      "string | ((round: number, monster: Monster, text: string) => string)";

  private static final Map<String, List<String>> descriptiveParamNames =
      Map.ofEntries(
          Map.entry("canEquip[Item]", List.of("equipment")),
          Map.entry("canEquip[Familiar]", List.of("familiar")),
          Map.entry("canEquip[Familiar, Item]", List.of("familiar", "equipment")),
          Map.entry("buy[Item,number]", List.of("item", "quantity")),
          Map.entry("buy[Item,number,number]", List.of("item", "quantity", "price")),
          Map.entry("buy[number,Item,number]", List.of("quantity", "item", "price")),
          Map.entry("buy[number,Item]", List.of("quantity", "item")),
          Map.entry("buy[Coinmaster,number,Item]", List.of("coinmaster", "quantity", "item")),
          Map.entry("buyUsingStorage[Item,number]", List.of("item", "quantity")),
          Map.entry("buyUsingStorage[Item,number,number]", List.of("item", "quantity", "price")),
          Map.entry("buyUsingStorage[number,Item,number]", List.of("quantity", "item", "price")),
          Map.entry("buyUsingStorage[number,Item]", List.of("quantity", "item")));

  private static final Map<String, String> descriptiveFieldTypes =
      Map.ofEntries(
          Map.entry("Bounty.monster", "Monster"),
          Map.entry("Bounty.location", "Location"),
          Map.entry("Class.primestat", "Stat"),
          Map.entry("Coinmaster.item", "Item"),
          Map.entry("Item.buyer", "Coinmaster"),
          Map.entry("Item.seller", "Coinmaster"),
          Map.entry("Item.noob_skill", "Skill"),
          Map.entry("Location.bounty", "Bounty"),
          Map.entry("Skill.class", "Class"),
          Map.entry("Thrall.skill", "Skill"),
          Map.entry("Vykea.rune", "Item"),
          Map.entry("Vykea.attack_element", "Element"),
          Map.entry("Familiar.hatchling", "Item"),
          Map.entry("Familiar.drop_item", "Item"),
          Map.entry("Monster.attack_element", "Element"),
          Map.entry("Monster.defense_element", "Element"),
          Map.entry("Monster.phylum", "Phylum"),
          Map.entry("Monster.poison", "Effect"));

  private static final List<Type> typesWithNumbers =
      List.of(
          DataTypes.CLASS_TYPE,
          DataTypes.EFFECT_TYPE,
          DataTypes.FAMILIAR_TYPE,
          DataTypes.ITEM_TYPE,
          DataTypes.LOCATION_TYPE,
          DataTypes.MONSTER_TYPE,
          DataTypes.SERVANT_TYPE,
          DataTypes.SKILL_TYPE,
          DataTypes.SLOT_TYPE,
          DataTypes.THRALL_TYPE);

  private static List<String> getParamTypes(Function f) {
    var paramTypes =
        f.getVariableReferences().stream()
            .map(VariableReference::getRawType)
            .map(TypescriptDefinition::getType)
            .collect(Collectors.toList());

    switch (f.getName()) {
      case "adv1", "adventure" -> {
        if (paramTypes.size() >= 3) {
          paramTypes.set(2, combatFilterType);
        }
      }
      case "run_combat" -> {
        if (paramTypes.size() >= 1) {
          paramTypes.set(0, combatFilterType);
        }
      }
    }

    return paramTypes;
  }

  private static String getType(Type t) {
    return INSTANCE.toJavascriptTypeName(t);
  }

  private static String getReturnType(Function f) {
    if (f.getName().equals("abort")) {
      return "never";
    }

    return getType(f.getType());
  }

  public static String formatFunction(LibraryFunction f) {
    var name = JavascriptRuntime.toCamelCase(f.getName());
    var type = getReturnType(f);
    var paramTypes = getParamTypes(f);
    var overrideKey = String.format("%s[%s]", name, String.join(",", paramTypes));
    var paramNames = descriptiveParamNames.getOrDefault(overrideKey, f.getParameterNames());

    var params =
        IntStream.range(0, paramNames.size())
            .mapToObj(i -> String.format("%s: %s", paramNames.get(i), paramTypes.get(i)))
            .collect(Collectors.joining(", "));

    var deprecationWarning =
        (f.deprecationWarning.length > 0)
            ? "/** @deprecated " + String.join("<br>", f.deprecationWarning) + " */\n"
            : "";

    return String.format("%sexport function %s(%s): %s;", deprecationWarning, name, params, type);
  }

  private static List<String> getFunctions() {
    return JavascriptRuntime.getFunctions().stream()
        .filter(f -> !f.getName().equals("delete"))
        .filter(LibraryFunction.class::isInstance)
        .map(LibraryFunction.class::cast)
        .map(TypescriptDefinition::formatFunction)
        .toList();
  }

  private static List<String> formatMafiaClassMethods(final String type, final String argType) {
    boolean isAbstract = (type == null);
    return List.of(
        String.format(
            "    static get%s(name: %s): %s;",
            isAbstract ? "<T extends MafiaClass>" : "", argType, isAbstract ? "T" : type),
        String.format(
            "    static get%s(names: %s[]): %s[];",
            isAbstract ? "<T extends MafiaClass>" : "", argType, isAbstract ? "T" : type),
        String.format(
            "    static all<T %s>(): T[];", isAbstract ? "extends MafiaClass" : "= " + type),
        String.format("    static none: %s;", isAbstract ? "MafiaClass" : type));
  }

  private static List<String> getAbstractMafiaClass() {
    var abstractClass = new ArrayList<String>();
    abstractClass.add("declare abstract class MafiaClass {");
    abstractClass.addAll(formatMafiaClassMethods(null, "(string | number)"));
    abstractClass.add("}");
    return abstractClass;
  }

  private static List<String> formatField(
      final String className, final String unionType, final String name, String type) {
    if (name.equals("name") && unionType != null) {
      type = unionType;
    } else {
      var override = String.format("%s.%s", className, name);
      type = descriptiveFieldTypes.getOrDefault(override, type);
    }

    return List.of(
        "    /**",
        String.format("     * %s */", StringUtilities.capitalize(name.replaceAll("_", " "))),
        String.format("    readonly %s: %s;", JavascriptRuntime.toCamelCase(name), type));
  }

  private static List<String> formatMafiaClass(final Type t) {
    var name = StringUtilities.capitalize(t.getName());

    List<String> values =
        t.allValues().count() < 30
            ? Arrays.stream(t.allValues().keys()).map(v -> v.contentString).toList()
            : List.of();

    var unionType = values.size() > 0 ? name + "Type" : null;

    // Prepare the field names
    var proxy = (RecordType) t.asProxy();
    var fieldNames = proxy.getFieldNames();
    var fieldTypes =
        Arrays.stream(proxy.getFieldTypes()).map(TypescriptDefinition::getType).toList();
    var fields =
        IntStream.range(0, fieldTypes.size())
            .mapToObj(i -> formatField(name, unionType, fieldNames[i], fieldTypes.get(i)))
            .flatMap(List::stream)
            .toList();

    // Prepare the methods
    var argType = unionType != null ? unionType : "string";
    if (typesWithNumbers.contains(t)) argType = String.format("(%s | number)", argType);
    var methods = formatMafiaClassMethods(name, argType);

    var result = new ArrayList<String>();

    if (unionType != null) {
      result.add(
          String.format(
              "export type %s = %s;",
              unionType,
              values.stream()
                  .sorted()
                  .map(v -> String.format("\"%s\"", v))
                  .collect(Collectors.joining(" | "))));
    }

    result.add(String.format("export class %s extends MafiaClass {", name));
    result.addAll(methods);

    if (unionType != null) {
      result.add(String.format("    toString(): %s;", unionType));
    }

    result.addAll(fields);
    result.add("}");

    return result;
  }

  private static List<String> getMafiaClasses() {
    return DataTypes.enumeratedTypes.stream()
        .sorted(Type::compareTo)
        .flatMap(t -> formatMafiaClass(t).stream())
        .toList();
  }

  private static List<String> getFrontMatter() {
    return List.of(
        "// v" + VERSION + "." + kolmafiaVersion + ".0",
        "// Generated by KoLmafia r" + kolmafiaVersion + " with type generator v" + VERSION);
  }

  public static String getContents() {
    return Stream.of(getFrontMatter(), getFunctions(), getAbstractMafiaClass(), getMafiaClasses())
        .flatMap(Collection::stream)
        .collect(Collectors.joining("\n"));
  }

  private static String getVersion(final String[] args) {
    if (args.length > 0) {
      return args[0];
    }

    return String.valueOf(StaticEntity.getRevision());
  }

  public static void main(final String[] args) {
    kolmafiaVersion = getVersion(args);

    var contents = getContents();

    try {
      Files.write(
          Path.of("./index.d.ts"),
          contents.getBytes(),
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.CREATE);
    } catch (IOException e) {
      System.out.println("Cannot write to index.d.ts");
    }
  }
}
