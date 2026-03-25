package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.text.CharSequenceLength.hasLength;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TypescriptDefinitionTest {
  private LibraryFunction findFunction(final String signature) {
    var name = signature.substring(0, signature.indexOf("("));
    return Arrays.stream(RuntimeLibrary.functions.findFunctions(name))
        .filter(f -> f.getSignature().equals(signature))
        .map(LibraryFunction.class::cast)
        .findFirst()
        .orElse(null);
  }

  private List<LibraryFunction> findFunctionOverloads(final String signaturePrefix) {
    var name = signaturePrefix.substring(0, signaturePrefix.indexOf("("));

    return Arrays.stream(RuntimeLibrary.functions.findFunctions(name))
        .filter(f -> f.getSignature().startsWith(signaturePrefix))
        .filter(LibraryFunction.class::isInstance)
        .map(LibraryFunction.class::cast)
        .toList();
  }

  @Test
  void producesAnyOutput() {
    assertThat(TypescriptDefinition.getTypeDefContents(), hasLength(greaterThan(0)));
  }

  private static Stream<Arguments> provideStringsForFormatFunction() {
    return Stream.of(
        Arguments.of("abort()", "export function abort(): never;"),
        Arguments.of(
            "adv1(location)",
            "/** Adventures once at a location. */\nexport function adv1(locationValue: Location): boolean;"),
        Arguments.of(
            "adv1(location, int, string)",
"""
/**
 * Adventures once at a location, spending at most the specified number of adventures.
 * @param filterFunction Name of a combat filter function to call each round
 */
export function adv1(locationValue: Location, adventuresUsedValue: number, filterFunction: string | ((round: number, monster: Monster, text: string) => string)): boolean;"""),
        Arguments.of(
            "run_combat(string)",
            "export function runCombat(filterFunction: string | ((round: number, monster: Monster, text: string) => string)): string;"),
        Arguments.of(
            "fact_type(class, path, monster)",
            "export function factType(cls: Class, path: Path, monster: Monster): \"none\" | \"effect\" | \"item\" | \"stats\" | \"hp\" | \"mp\" | \"meat\" | \"modifier\";"),
        Arguments.of(
            "get_items_hash(string)",
"""
export function getItemsHash(itemsSource: "inventory" | "closet" | "storage" | "display" | "shop"): number;"""));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForFormatFunction")
  void canFormatFunctions(final String signature, final String formatted) {
    var fn = findFunction(signature);
    assertThat(TypescriptDefinition.formatFunction(fn), equalTo(formatted));
  }

  private static Stream<Arguments> provideStringsForMergeFunction() {
    return Stream.of(
        Arguments.of(
            "adv1(location",
            List.of(
                "/** Adventures once at a location, spending at most the specified number of adventures. */\nexport function adv1(locationValue: Location, adventuresUsedValue?: number): boolean;",
                "/**\n * Adventures once at a location, spending at most the specified number of adventures.\n * @param filterFunction Name of a combat filter function to call each round\n */\nexport function adv1(locationValue: Location, adventuresUsedValue: number, filterFunction?: string | ((round: number, monster: Monster, text: string) => string)): boolean;")),
        Arguments.of(
            "buy(item",
            List.of(
                "/**\n * Purchases items from the mall or NPC stores. Returns whether requested quantity of the item was bought.\n * @param item The item to purchase\n * @param quantity Number of items to buy\n */\nexport function buy(item: Item, quantity?: number): boolean;",
                "/**\n * Purchases items from the mall or NPC stores up to a price limit. Returns the number of items bought.\n * @param item The item to purchase\n * @param quantity Number of items to buy\n * @param price Maximum price to pay per item\n */\nexport function buy(item: Item, quantity: number, price: number): number;")));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForMergeFunction")
  void mergesOverloadsToOptionalParameters(final String name, final List<String> lines) {
    var fns = findFunctionOverloads(name);
    assertThat(TypescriptDefinition.formatFunction(fns), equalTo(lines));
  }

  @Test
  void showsDeprecationMessage() {
    var fn = findFunction("my_path_id()");
    // This is ash styled and not JS styled, but still better than nothing
    assertThat(
        TypescriptDefinition.formatFunction(fn),
        is(
            "/** @deprecated Changing 'my_path_id()' to 'my_path().id' will remove this warning */\nexport function myPathId(): number;"));
  }

  @Test
  void showsDescriptionWithParamsAndDeprecation() {
    var fn = findFunction("path_name_to_id(string)");
    var formatted = TypescriptDefinition.formatFunction(fn);
    assertThat(formatted, containsString("Converts a path name to its numeric id."));
    assertThat(formatted, containsString("@param value The name of the path to look up"));
    assertThat(formatted, containsString("@deprecated"));
  }

  @Test
  void firstLineContainsValidVersionNumber() {
    // We get the version number with `PACKAGE_VERSION=$(head -n 1 index.d.ts | cut -c 5-)`
    // As such, here we test that this produces a valid version number
    var contents = TypescriptDefinition.getTypeDefContents();
    var firstLine = contents.substring(0, contents.indexOf("\n"));
    var version = firstLine.substring(4);
    assertThat(version, matchesPattern("^\\d+\\.\\d+\\.\\d+$"));
  }

  @Test
  void documentedFunctionIncludesJsdocDescription() {
    var fn = findFunction("print()");
    var formatted = TypescriptDefinition.formatFunction(fn);
    assertThat(formatted, containsString("Prints a blank line to the CLI and session log."));
    assertThat(formatted, startsWith("/** "));
  }

  @Test
  void undocumentedFunctionHasNoJsdoc() {
    var fn = findFunction("is_adventuring()");
    var formatted = TypescriptDefinition.formatFunction(fn);
    assertThat(formatted, is("export function isAdventuring(): boolean;"));
  }

  @Test
  void paramDescriptionsAppearInJsdoc() {
    var fn = findFunction("contains_text(string, string)");
    var formatted = TypescriptDefinition.formatFunction(fn);
    assertThat(formatted, containsString("@param source The string to search in"));
    assertThat(formatted, containsString("@param search The substring to search for"));
  }

  @Test
  void allOverloadsHaveDescription() {
    var fns = findFunctionOverloads("visit_url(");
    var lines = TypescriptDefinition.formatFunction(fns);
    for (var line : lines) {
      assertThat(line, containsString("Fetches a URL from the KoL server"));
    }
  }

  @Test
  void containsEnvironmentUnion() {
    assertThat(TypescriptDefinition.getEnvironmentUnion(), startsWith("\"indoor\""));
  }

  @Test
  void headerFile() {
    var contents = TypescriptDefinition.getHeaderFileContents();
    assertThat(contents, containsString("module.exports.visitUrl = "));
  }
}
