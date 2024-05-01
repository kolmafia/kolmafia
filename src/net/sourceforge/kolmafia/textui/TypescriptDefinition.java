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
import net.sourceforge.kolmafia.modifiers.ModifierValueType;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.persistence.FactDatabase;
import net.sourceforge.kolmafia.textui.command.JsRefCommand;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.Symbol;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.VarArgType;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TypescriptDefinition {
  public static final int VERSION = 5;
  private static String kolmafiaVersion = "0";

  private static final JsRefCommand INSTANCE = new JsRefCommand();

  private static final String combatFilterType =
      "string | ((round: number, monster: Monster, text: string) => string)";

  private static final Map<String, String> descriptiveFieldTypes =
      Map.ofEntries(
          Map.entry("Bounty.location", "Location"),
          Map.entry("Bounty.monster", "Monster"),
          Map.entry("Class.primestat", "Stat"),
          Map.entry("Coinmaster.item", "Item"),
          Map.entry("Familiar.drop_item", "Item"),
          Map.entry("Familiar.hatchling", "Item"),
          Map.entry("Item.buyer", "Coinmaster"),
          Map.entry("Item.noob_skill", "Skill"),
          Map.entry("Item.seller", "Coinmaster"),
          Map.entry("Location.bounty", "Bounty"),
          Map.entry("Location.environment", "Environment"),
          Map.entry("Modifier.type", "ModifierValueType"),
          Map.entry("Monster.attack_element", "Element"),
          Map.entry("Monster.defense_element", "Element"),
          Map.entry("Monster.phylum", "Phylum"),
          Map.entry("Monster.poison", "Effect"),
          Map.entry("Skill.class", "Class"),
          Map.entry("Thrall.skill", "Skill"),
          Map.entry("Vykea.attack_element", "Element"),
          Map.entry("Vykea.rune", "Item"));

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

  private static List<Boolean> getVariadicParams(Function f) {
    return f.getVariableReferences().stream()
        .map(VariableReference::getRawType)
        .map(type -> type instanceof VarArgType)
        .collect(Collectors.toList());
  }

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
    return switch (f.getName()) {
      case "abort" -> "never";
      case "fact_type" -> Arrays.stream(FactDatabase.FactType.values())
          .map(v -> "\"" + v.toString() + "\"")
          .collect(Collectors.joining(" | "));
      default -> getType(f.getType());
    };
  }

  public static String formatFunction(LibraryFunction f) {
    var name = JavascriptRuntime.toCamelCase(f.getName());
    var type = getReturnType(f);
    var paramTypes = getParamTypes(f);
    var paramNames = f.getParameterNames();
    var variadicParams = getVariadicParams(f);

    var params =
        IntStream.range(0, paramNames.size())
            .mapToObj(
                i ->
                    variadicParams.get(i)
                        ? String.format("...%s: %s", paramNames.get(i), paramTypes.get(i))
                        : String.format("%s: %s", paramNames.get(i), paramTypes.get(i)))
            .collect(Collectors.joining(", "));

    var deprecationWarning =
        (f.deprecationWarning.length > 0)
            ? "/** @deprecated " + String.join("<br>", f.deprecationWarning) + " */\n"
            : "";

    return String.format("%sexport function %s(%s): %s;", deprecationWarning, name, params, type);
  }

  private static Stream<LibraryFunction> getFunctions() {
    return JavascriptRuntime.getFunctions().stream()
        .filter(f -> !f.getName().equals("delete"))
        .filter(LibraryFunction.class::isInstance)
        .map(LibraryFunction.class::cast);
  }

  private static List<String> getFunctionDefinitions() {
    return getFunctions().map(TypescriptDefinition::formatFunction).toList();
  }

  private static List<String> getFunctionHeaders() {
    return getFunctions()
        .map(Symbol::getName)
        .distinct()
        .map(
            f ->
                String.format(
                    "module.exports.%1$s = function %1$s() { throw new Error(`Cannot access the KoLmafia standard library from a normal JavaScript context.`); };",
                    JavascriptRuntime.toCamelCase(f)))
        .toList();
  }

  private static List<String> formatMafiaClassMethods() {
    return List.of(
        "    static get<T extends MafiaClass>(this: { new (): T; }, name: (string | number)): T;",
        "    static get<T extends MafiaClass>(this: { new (): T; }, names: readonly (string | number)[]): T[];",
        "    static all<T extends MafiaClass>(this: { new (): T; }): T[];",
        "    static none: MafiaClass;");
  }

  private static List<String> getAbstractMafiaClass() {
    var abstractClass = new ArrayList<String>();
    abstractClass.add("declare abstract class MafiaClass {");
    abstractClass.addAll(formatMafiaClassMethods());
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

    var unionType = values.isEmpty() ? null : name + "Type";

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
    var methods = List.of(String.format("    static none: %s;", name));

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

  private static List<String> getMafiaClassHeaders() {
    return DataTypes.enumeratedTypes.stream()
        .map(t -> StringUtilities.capitalize(t.getName()))
        .map(
            t ->
                String.format(
                    "module.exports.%s = class %<s { static get = (v) => Array.isArray(v) ? v.map(() => new %<s()) : new %<s(); static all = () => []; static none = new %<s(); }",
                    t))
        .toList();
  }

  private static List<String> getMafiaClassDefs() {
    return DataTypes.enumeratedTypes.stream()
        .sorted(Type::compareTo)
        .flatMap(t -> formatMafiaClass(t).stream())
        .toList();
  }

  protected static String getEnvironmentUnion() {
    return Arrays.stream(Environment.values())
        .map(Environment::toString)
        .sorted()
        .map(e -> "\"" + e + "\"")
        .collect(Collectors.joining(" | "));
  }

  protected static String getModifierValueTypeUnion() {
    return Arrays.stream(ModifierValueType.values())
        .map(ModifierValueType::toString)
        .sorted()
        .map(t -> "\"" + t + "\"")
        .collect(Collectors.joining(" | "));
  }

  protected static List<String> getHelperTypes() {
    return List.of(
        "type Environment = " + getEnvironmentUnion() + ";",
        "type ModifierValueType = " + getModifierValueTypeUnion() + ";");
  }

  protected static List<String> getSessionStorageTyping() {
    return List.of(
        """
            declare class Storage {
                /**
                 * Returns the number of key/value pairs.
                 */
                readonly length: number;

                /**
                 * Removes all key/value pairs, if there are any.
                 */
                clear(): void;

                /**
                 * Returns the current value associated with the given key, or null if the given key does not exist.
                 */
                getItem(key: string): string | null;

                /**
                 * Returns the name of the nth key, or null if n is greater than or equal to the number of key/value pairs.
                 */
                key(index: number): string | null;

                /**
                 * Removes the key/value pair with the given key, if a key/value pair with the given key exists.
                 */
                removeItem(key: string): void;

                /**
                 * Sets the value of the pair identified by key to value, creating a new key/value pair if none existed for key previously.
                 */
                setItem(key: string, value: string): void;
            }
            export const sessionStorage: Storage;
            """);
  }

  protected static List<String> getFrontMatter() {
    return List.of(
        "// v" + VERSION + "." + kolmafiaVersion + ".0",
        "// Generated by KoLmafia r" + kolmafiaVersion + " with type generator v" + VERSION);
  }

  protected static List<String> getScriptFunctionDefs() {
    return List.of(
        "export type AfterAdventureScript = () => void;",
        "export type BeforePVPScript = () => void;",
        "export type BetweenBattleScript = () => void;",
        "export type BuyScript = (item: string, quantity: string, ingredientLevel: string, defaultBuy: string) => boolean;",
        "export type ChatPlayerScript = (playerName: string, playerId: string, channel: string) => void;",
        "export type ChatbotScript = (sender: string, content: string, channel?: string) => void;",
        "export type ChoiceAdventureScript = (choiceNumber: number, responseText: string) => void;",
        "export type ConsultScript = (round: number, monster: Monster, responseText: string) => void;",
        "export type CounterScript = (label: string, turnsRemaining: string) => boolean;",
        "export type FamiliarScript = () => boolean;",
        "export type KingLiberatedScript = () => void;",
        "export type PostAscensionScript = () => void;",
        "export type PreAscensionScript = () => void;",
        "export type RecoveryScript = (type: \"HP\" | \"MP\", needed: number) => boolean;",
        "export type SpadingScript = (event: string, meta: string, responseText: string) => void;");
  }

  public static String getTypeDefContents() {
    return Stream.of(
            getFrontMatter(),
            getHelperTypes(),
            getFunctionDefinitions(),
            getAbstractMafiaClass(),
            getMafiaClassDefs(),
            getScriptFunctionDefs(),
            getSessionStorageTyping())
        .flatMap(Collection::stream)
        .collect(Collectors.joining("\n"));
  }

  private static String getVersion(final String[] args) {
    if (args.length > 0) {
      return args[0];
    }

    return String.valueOf(StaticEntity.getRevision());
  }

  public static String getHeaderFileContents() {
    return Stream.of(
            List.of("module.exports.sessionStorage = {};"),
            getFunctionHeaders(),
            getMafiaClassHeaders())
        .flatMap(Collection::stream)
        .collect(Collectors.joining("\n"));
  }

  public static void main(final String[] args) {
    kolmafiaVersion = getVersion(args);

    var files =
        List.of(
            Map.entry("index.d.ts", getTypeDefContents()),
            Map.entry("index.js", getHeaderFileContents()));

    for (var entry : files) {
      var file = entry.getKey();
      try {
        Files.write(
            Path.of("./.github/npm/" + file),
            entry.getValue().getBytes(),
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE);
      } catch (IOException e) {
        System.out.println("Cannot write to " + file);
      }
    }
  }
}
