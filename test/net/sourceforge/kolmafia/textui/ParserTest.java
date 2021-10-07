package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tries to parse valid and invalid ASH programs. */
public class ParserTest {

  @BeforeEach
  public void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  @AfterEach
  public void clearRevision() {
    StaticEntity.overrideRevision(null);
  }

  public static Stream<Arguments> data() {
    /**
     * @return A list containing arrays with the following spec: String description String errorText
     *     A substring of the expected error message. List<String> tokens that are expected for a
     *     successful parse.
     *     <p>Note that exactly one of (errorText, tokens) should be non-null.
     */
    return Stream.of(
        Arguments.of("Unterminated Java for-loop", "for (", "Expected ), found end of file", null),
        Arguments.of(
            "Java for-loop, bad incrementer", "for (;;1)", "Variable reference expected", null),
        Arguments.of(
            "valid empty Java for-loop",
            "for (;;) {}",
            null,
            Arrays.asList("for", "(", ";", ";", ")", "{", "}")),
        Arguments.of(
            "Java for-loop with new variable",
            "for (int i = 0; i < 5; ++i) {}",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "++", "i", ")", "{",
                "}")),
        Arguments.of(
            "Multiline string, end of line not escaped", "'\n'", "No closing ' found", null),
        Arguments.of(
            "Multiline string, end of line properly escaped",
            "'\\\n'",
            null,
            Arrays.asList("'\\", "'")),
        Arguments.of(
            "Multiline string, end of line properly escaped + empty lines",
            "'\\\n\n   \n\n\n'",
            null,
            Arrays.asList("'\\", "'")),
        Arguments.of(
            "Multiline string, end of line properly escaped + empty lines + comment",
            "'\\\n\n\n//Comment\n\n'",
            "No closing ' found",
            null),
        Arguments.of(
            "Unterminated string template",
            "`{`",
            // The parser tries to start a new string template inside the expression
            "No closing ` found",
            null),
        Arguments.of(
            "Abruptly unterminated string template, expecting expression",
            "`{",
            "Expression expected",
            null),
        Arguments.of(
            "Abruptly unterminated string template, parsed expression",
            "`{1",
            "Expected }, found end of file",
            null),
        Arguments.of(
            // Idem

            "Typed constant, no bracket", "$int", "Expected [, found end of file", null),
        Arguments.of(
            "Typed constant non-successive characters",
            "$		boolean 	 [ 		false ]",
            null,
            Arrays.asList("$", "boolean", "[", "false ", "]")),
        Arguments.of(
            "Typed constant escaped characters",
            "$boolean[\\f\\a\\l\\s\\e]",
            null,
            Arrays.asList("$", "boolean", "[", "\\f\\a\\l\\s\\e", "]")),
        Arguments.of(
            "Typed constant bad typecast", "$boolean['']", "Bad boolean value: \"''\"", null),
        Arguments.of("Typed constant, unknown type", "$foo[]", "Unknown type foo", null),
        Arguments.of(
            "Typed constant, non-primitive type",
            "record r {int i;}; $r[]",
            "Non-primitive type r",
            null),
        Arguments.of("Typed constant multiline", "$boolean[\n]", "No closing ] found", null),
        Arguments.of(
            "Typed constant, escaped multiline", "$boolean[\\\n]", "No closing ] found", null),
        Arguments.of(
            "Typed constant, nested brackets, proper",
            "$item[[8042]rock]",
            null,
            Arrays.asList("$", "item", "[", "[8042]rock", "]")),
        Arguments.of(
            "Typed constant, nested brackets, improper",
            "$item[[abc]]",
            "Bad item value: \"[abc]\"",
            null),
        Arguments.of(
            "Typed constant, numeric literal",
            "$item[1]",
            null,
            Arrays.asList("$", "item", "[", "1", "]")),
        Arguments.of(
            "Typed constant literal correction",
            "$item[rock]; $effect[buy!]; $monster[eldritch]; $skill[boon]; $location[dire warren]",
            null,
            Arrays.asList(
                "$",
                "item",
                "[",
                "rock",
                "]",
                ";",
                "$",
                "effect",
                "[",
                "buy!",
                "]",
                ";",
                "$",
                "monster",
                "[",
                "eldritch",
                "]",
                ";",
                "$",
                "skill",
                "[",
                "boon",
                "]",
                ";",
                "$",
                "location",
                "[",
                "dire warren",
                "]")),
        Arguments.of(
            "empty typed constant", "$string[]", null, Arrays.asList("$", "string", "[", "]")),
        Arguments.of("Plural constant, abrupt end", "$booleans[", "No closing ] found", null),
        Arguments.of(
            "Plural constant, unknown plural type", "$kolmafia[]", "Unknown type kolmafia", null),
        Arguments.of(
            "Plural constant, RAM-protection", "$strings[]", "Can't enumerate all strings", null),
        Arguments.of(
            "Plural constant, non... \"traditional\" plurals",
            "$bounties[]; $classes[]; $phyla[]",
            null,
            Arrays.asList(
                "$",
                "bounties",
                "[",
                "]",
                ";",
                "$",
                "classes",
                "[",
                "]",
                ";",
                "$",
                "phyla",
                "[",
                "]")),
        Arguments.of(
            "Plural constant non-successive characters",
            "$		booleans 	 [ 		 ]",
            null,
            Arrays.asList("$", "booleans", "[", "]")),
        Arguments.of(
            "Plural constant multiline",
            // End-of-lines can appear anywhere
            "$booleans[true\n\n\n,\nfalse, false\n,false,\ntrue,tr\nue]",
            null,
            Arrays.asList(
                "$",
                "booleans",
                "[",
                "true",
                ",",
                "false, false",
                ",false,",
                "true,tr",
                "ue",
                "]")),
        Arguments.of(
            "Plural constant w/ escape characters",
            // *Escaped* end-of-lines and escaped "\n"s get added to the value
            "$booleans[tr\\\nu\\ne]",
            "Bad boolean value: \"tr\nu\ne\"",
            null),
        Arguments.of(
            "Plural constant, nested brackets, proper",
            "$items[[8042]rock]",
            null,
            Arrays.asList("$", "items", "[", "[8042]rock", "]")),
        Arguments.of(
            "Plural constant, nested brackets, improper",
            "$items[[abc]]",
            "Bad item value: \"[abc]\"",
            null),
        Arguments.of(
            "Plural constant, single slash",
            "$booleans[tr/Comment\nue]",
            "Bad boolean value: \"tr/Commentue\"",
            null),
        Arguments.of(
            "Plural constant, comment",
            "$booleans[tr//Comment\nue]",
            null,
            Arrays.asList("$", "booleans", "[", "tr", "//Comment", "ue", "]")),
        Arguments.of(
            "Plural constant, two line-separated slashes",
            "$booleans[tr/\n/ue]",
            "Bad boolean value: \"tr//ue\"",
            null),
        Arguments.of(
            "Plural constant, line-separated multiline comment",
            "$booleans[tr/\n**/ue]",
            "Bad boolean value: \"tr/**/ue\"",
            null),
        Arguments.of(
            "Mid-line // comment",
            "int x = // interrupting comment\n  5;",
            null,
            Arrays.asList("int", "x", "=", "// interrupting comment", "5", ";")),
        Arguments.of(
            "Mid-line # comment",
            // This ought to only accept full-line comments, but it's incorrectly implemented,
            // and at this point, widely used enough that this isn't feasible to change.
            "int x = # interrupting comment\n  5;",
            null,
            Arrays.asList("int", "x", "=", "# interrupting comment", "5", ";")),
        Arguments.of(
            "Multiline comment",
            "int x =/* this\n    is a comment\n   */ 5;",
            null,
            // Note that this drops some leading whitespace.
            Arrays.asList("int", "x", "=", "/* this", "is a comment", "*/", "5", ";")),
        Arguments.of(
            "Multiline comment on one line",
            "int x =/* this is a comment */ 5;",
            null,
            Arrays.asList("int", "x", "=", "/* this is a comment */", "5", ";")),
        Arguments.of(
            "Simple map literal",
            "int[item] { $item[seal-clubbing club]: 1, $item[helmet turtle]: 2}",
            null,
            Arrays.asList(
                "int",
                "[",
                "item",
                "]",
                "{",
                "$",
                "item",
                "[",
                "seal-clubbing club",
                "]",
                ":",
                "1",
                ",",
                "$",
                "item",
                "[",
                "helmet turtle",
                "]",
                ":",
                "2",
                "}")),
        Arguments.of(
            "Unterminated aggregate literal", "int[int] {", "Expected }, found end of file", null),
        Arguments.of("Interrupted map literal", "int[int] {:", "Script parsing error", null),
        Arguments.of(
            "Simple array literal",
            "int[5] { 1, 2, 3, 4, 5}",
            null,
            Arrays.asList(
                "int", "[", "5", "]", "{", "1", ",", "2", ",", "3", ",", "4", ",", "5", "}")),
        Arguments.of(
            "Array literal with trailing comma",
            "int[2] { 1, 2, }",
            null,
            Arrays.asList("int", "[", "2", "]", "{", "1", ",", "2", ",", "}")),
        Arguments.of(
            "Array literal with variable",
            "int x = 10; int[5] { 1, 2, x, 4, 5}",
            null,
            Arrays.asList(
                "int", "x", "=", "10", ";", "int", "[", "5", "]", "{", "1", ",", "2", ",", "x", ",",
                "4", ",", "5", "}")),
        /*
        Arguments.of(
          // We ought to check for this case too, but we don't...
          "Array literal not enough elements",
          "int[10] { 1, 2, 3, 4, 5}",
          "Array has 10 elements but 5 initializers.",
          null),
        */
        Arguments.of(
            "Array literal too many elements",
            "int[1] { 1, 2, 3, 4, 5 }",
            "Array has 1 elements but 5 initializers.",
            null),
        Arguments.of(
            "Empty multidimensional map literal",
            "int[int, int]{}",
            null,
            Arrays.asList("int", "[", "int", ",", "int", "]", "{", "}")),
        Arguments.of(
            "Non-empty multidimensional map literal",
            "int[int, int, int]{ {}, { 1:{2, 3} } }",
            null,
            Arrays.asList(
                "int", "[", "int", ",", "int", ",", "int", "]", "{", "{", "}", ",", "{", "1", ":",
                "{", "2", ",", "3", "}", "}", "}")),
        Arguments.of(
            "Interrupted multidimensional map literal",
            "int[int, int] {1:",
            "Script parsing error",
            null),
        Arguments.of(
            "Invalid array literal coercion", "int[int]{ 'foo' }", "Invalid array literal", null),
        Arguments.of(
            "Invalid map literal coercion", "int[int]{ 'foo':'bar' }", "Invalid map literal", null),
        Arguments.of(
            "Ambiguity between array and map literal",
            "boolean[5]{ 0:true, 1:true, 2:false, true, 4:false }",
            "Expected :, found ,",
            null),
        Arguments.of(
            "Ambiguity between array and map literal 2: index and data are both integers",
            "int[5]{ 0, 1, 2, 3:3, 4 }",
            "Cannot include keys when making an array literal",
            null),
        Arguments.of(
            "Ambiguity between array and map literal 3: that can't be a key",
            "string[5]{ '0', '1', '2', '3':'3', '4' }",
            "Expected , or }, found :",
            null),
        Arguments.of(
            // This... exercises a different code path.
            "Parenthesized map literal",
            "(int[int]{})",
            null,
            Arrays.asList("(", "int", "[", "int", "]", "{", "}", ")")),
        Arguments.of(
            // Why is this allowed...? This is the only way I could think of to exercise the
            // rewind functionality.
            "Typedef of existing variable",
            "int foo; typedef int foo; (\nfoo\n\n + 2);",
            null,
            Arrays.asList(
                "int", "foo", ";", "typedef", "int", "foo", ";", "(", "foo", "+", "2", ")", ";")),
        Arguments.of(
            "interrupted script directive", "script", "Expected <, found end of file", null),
        Arguments.of(
            "script directive delimited with <>",
            "script <zlib.ash>;",
            null,
            Arrays.asList("script", "<zlib.ash>", ";")),
        Arguments.of(
            "script directive delimited with \"\"",
            "script \"zlib.ash\"",
            null,
            Arrays.asList("script", "\"zlib.ash\"")),
        Arguments.of(
            "script directive without delimiter",
            "script zlib.ash",
            null,
            Arrays.asList("script", "zlib.ash")),
        Arguments.of(
            "Unterminated script directive", "script \"zlib.ash", "No closing \" found", null),
        Arguments.of(
            "Script with bom", "\ufeff    'hello world'", null, Arrays.asList("'hello world'")),
        Arguments.of(
            "Simple operator assignment",
            "int x = 3;",
            null,
            Arrays.asList("int", "x", "=", "3", ";")),
        Arguments.of(
            "Compound operator assignment",
            "int x; x += 3;",
            null,
            Arrays.asList("int", "x", ";", "x", "+=", "3", ";")),
        Arguments.of(
            "Aggregate assignment to primitive",
            // What is this, C?
            "int x; x = {1};",
            "Cannot use an aggregate literal for type int",
            null),
        Arguments.of(
            "Primitive assignment to aggregate",
            "int[4] x = 1;",
            "Cannot store int in x of type int [4]",
            null),
        Arguments.of(
            "Compound assignment to aggregate",
            "int[4] x; x += 1;",
            "Cannot use '+=' on an aggregate",
            null),
        Arguments.of(
            "Aggregate assignment to aggregate",
            "int[4] x; x = {1, 2, 3, 4};",
            null,
            Arrays.asList(
                "int", "[", "4", "]", "x", ";", "x", "=", "{", "1", ",", "2", ",", "3", ",", "4",
                "}", ";")),
        Arguments.of(
            "since passes for low revision",
            "since r100;",
            null,
            Arrays.asList("since", "r100", ";")),
        Arguments.of(
            "since fails for high revision",
            "since r2000000000;",
            "requires revision r2000000000 of kolmafia or higher",
            null),
        Arguments.of("Invalid since version", "since 10;", "invalid 'since' format", null),
        Arguments.of(
            "since passes for low version", "since 1.0;", null, Arrays.asList("since", "1.0", ";")),
        Arguments.of(
            "since fails for high version", "since 2000000000.0;", "final point release", null),
        Arguments.of(
            "since fails for not-a-number", "since yesterday;", "invalid 'since' format", null),
        Arguments.of(
            "Basic function with one argument",
            "void f(int a) {}",
            null,
            Arrays.asList("void", "f", "(", "int", "a", ")", "{", "}")),
        Arguments.of(
            "Basic function with forward reference",
            "void foo(); void bar() { foo(); } void foo() { return; } bar();",
            null,
            Arrays.asList(
                "void", "foo", "(", ")", ";", "void", "bar", "(", ")", "{", "foo", "(", ")", ";",
                "}", "void", "foo", "(", ")", "{", "return", ";", "}", "bar", "(", ")", ";")),
        Arguments.of(
            "Invalid function name",
            "void float() {}",
            "Reserved word 'float' cannot be used as a function name",
            null),
        Arguments.of(
            "Basic function interrupted", "void f(", "Expected ), found end of file", null),
        Arguments.of(
            "Basic function parameter interrupted",
            "void f(int",
            "Expected identifier, found end of file",
            null),
        Arguments.of(
            "Basic function duplicate parameter",
            "void f(int a, float a) {}",
            "Parameter a is already defined",
            null),
        Arguments.of(
            "Basic function missing parameter separator",
            "void f(int a float a) {}",
            "Expected ,, found float", // Uh... doesn't quite stand out does it...
            null),
        Arguments.of(
            "Basic function with vararg",
            "void f(int a, string b, float... c) {} f(5, 'foo', 1.2, 6.3, 4.9, 10, -0)",
            null,
            Arrays.asList(
                "void", "f", "(", "int", "a", ",", "string", "b", ",", "float", "...", "c", ")",
                "{", "}", "f", "(", "5", ",", "'foo'", ",", "1", ".", "2", ",", "6", ".", "3", ",",
                "4", ".", "9", ",", "10", ",", "-", "0", ")")),
        Arguments.of(
            "Basic function with multiple varargs",
            "void f(int ... a, int ... b) {}",
            "Only one vararg parameter is allowed",
            null),
        Arguments.of(
            "Basic function with non-terminal vararg",
            "void f(int ... a, float b) {}",
            "The vararg parameter must be the last one",
            null),
        Arguments.of(
            "Basic function overrides library",
            "void round(float n) {}",
            "Function 'round(float)' overrides a library function.",
            null),
        Arguments.of(
            "Basic function defined multiple times",
            "void f() {} void f() {}",
            "Function 'f()' defined multiple times.",
            null),
        Arguments.of(
            "Basic function vararg clash",
            "void f(int a, int ... b) {} void f(int ... a) {}",
            "Function 'f(int ...)' clashes with existing function 'f(int, int ...)'.",
            null),
        Arguments.of(
            "Complex expression parsing",
            // Among other things, this tests that true / false are case insensitive, in case
            // you want to pretend you're working in Python, C, or both.
            "(!(~-5 == 10) && True || FALSE);",
            null,
            Arrays.asList(
                "(", "!", "(", "~", "-", "5", "==", "10", ")", "&&", "True", "||", "FALSE", ")",
                ";")),
        Arguments.of("Interrupted ! expression", "(!", "Value expected", null),
        Arguments.of(
            "Non-boolean ! expression",
            "(!'abc');",
            "\"!\" operator requires a boolean value",
            null),
        Arguments.of("Interrupted ~ expression", "(~", "Value expected", null),
        Arguments.of(
            "Non-boolean/integer ~ expression",
            "(~'abc');",
            "\"~\" operator requires an integer or boolean value",
            null),
        Arguments.of("Interrupted - expression", "(-", "Value expected", null),
        Arguments.of("Interrupted expression after operator", "(1 +", "Value expected", null),
        Arguments.of(
            "String concatenation with left-side coercion",
            "(1 + 'abc');",
            null,
            Arrays.asList("(", "1", "+", "'abc'", ")", ";")),
        Arguments.of(
            "String concatenation with right-side coercion",
            "('abc' + 1);",
            null,
            Arrays.asList("(", "'abc'", "+", "1", ")", ";")),
        Arguments.of(
            "Invalid expression coercion",
            "(true + 1);",
            "Cannot apply operator + to true (boolean) and 1 (int)",
            null),
        Arguments.of(
            "Numeric literal split after negative",
            "-/*negative \nnumber*/1.23;",
            null,
            Arrays.asList("-", "/*negative", "number*/", "1", ".", "23", ";")),
        Arguments.of(
            "Float literal split after decimal",
            "1./*decimal\n*/23;",
            null,
            Arrays.asList("1", ".", "/*decimal", "*/", "23", ";")),
        Arguments.of(
            "Float literal with no integral component",
            "-.123;",
            null,
            Arrays.asList("-", ".", "123", ";")),
        Arguments.of(
            "Float literal with no integral part, non-numeric fractional part",
            "-.123abc;",
            "Expected numeric value, found 123abc",
            null),
        Arguments.of(
            "unary negation",
            "int x; (-x);",
            null,
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";")),
        /*
          There's code for this case, but we encounter a separate error ("Record expected").
        Arguments.of(
          "Float literal with no decimal component",
          "123.;",
          null,
          Arrays.asList( "123", ".", ";" )),
        */
        Arguments.of(
            "Int literal with method call.",
            "123.to_string();",
            null,
            Arrays.asList("123", ".", "to_string", "(", ")", ";")),
        Arguments.of(
            "Unary minus",
            "int x; (-x);",
            null,
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";")),
        Arguments.of(
            "Chained if/else-if/else",
            "if (false) {} else if (false) {} else if (false) {} else {}",
            null,
            Arrays.asList(
                "if", "(", "false", ")", "{", "}", "else", "if", "(", "false", ")", "{", "}",
                "else", "if", "(", "false", ")", "{", "}", "else", "{", "}")),
        Arguments.of("Multiple else", "if (false) {} else {} else {}", "Else without if", null),
        Arguments.of(
            "else-if after else",
            "if (false) {} else {} else if (true) {}",
            "Else without if",
            null),
        Arguments.of("else without if", "else {}", "Unknown variable 'else'", null),
        Arguments.of(
            "Multiline cli_execute script",
            "cli_execute {\n  echo hello world;\n  echo sometimes we don't have a semicolon\n}",
            null,
            Arrays.asList(
                "cli_execute",
                "{",
                "echo hello world;",
                "echo sometimes we don't have a semicolon",
                "}")),
        Arguments.of(
            "Interrupted cli_execute script",
            "cli_execute {",
            "Expected }, found end of file",
            null),
        Arguments.of(
            "Non-basic-script cli_execute",
            "int cli_execute; cli_execute++",
            null,
            Arrays.asList("int", "cli_execute", ";", "cli_execute", "++")),
        Arguments.of(
            "For loop, no/bad initial expression",
            "for x from",
            "Expression for initial value expected",
            null),
        Arguments.of(
            "For loop, no/bad ceiling expression",
            "for x from 0 to",
            "Expression for floor/ceiling value expected",
            null),
        Arguments.of(
            "For loop, no/bad increment expression",
            "for x from 0 to 1 by",
            "Expression for increment value expected",
            null),
        Arguments.of(
            "basic template string",
            "`this is some math: {4 + 7}`",
            null,
            Arrays.asList("`this is some math: {", "4", "+", "7", "}`")),
        Arguments.of(
            "template string with a new variable",
            "`this is some math: {int x = 4; x + 7}`",
            "Unknown variable 'int'",
            null),
        Arguments.of(
            "template string with predefined variable",
            "int x; `this is some math: {(x = 4) + 7}`",
            null,
            Arrays.asList(
                "int", "x", ";", "`this is some math: {", "(", "x", "=", "4", ")", "+", "7", "}`")),
        Arguments.of(
            "template string with unclosed comment",
            "`this is some math: {7 // what determines the end?}`",
            "Expected }, found end of file",
            null),
        Arguments.of(
            "template string with terminated comment",
            "`this is some math: {7 // turns out you need a newline\r\n\t}`",
            null,
            Arrays.asList(
                "`this is some math: {", "7",
                "// turns out you need a newline", "}`")),
        Arguments.of(
            "template string with multiple templates",
            "`{'hello'} {'world'}`",
            null,
            Arrays.asList("`{", "'hello'", "} {", "'world'", "}`")),
        Arguments.of("string with uninteresting escape", "'\\z'", null, Arrays.asList("'\\z'")),
        Arguments.of(
            "string with unclearly terminated octal escape",
            "'\\1131'",
            null,
            // \113 is char code 75, which maps to 'K'
            Arrays.asList("'\\1131'")),
        Arguments.of(
            "string with insufficient octal digits",
            "'\\11'",
            "Octal character escape requires 3 digits",
            null),
        Arguments.of(
            "string with invalid octal digits",
            "'\\118'",
            "Octal character escape requires 3 digits",
            null),
        Arguments.of("string with hex digits", "'\\x3fgh'", null, Arrays.asList("'\\x3fgh'")),
        Arguments.of(
            "string with invalid hex digits",
            "'\\xhello'",
            "Hexadecimal character escape requires 2 digits",
            null),
        Arguments.of(
            "string with insufficient hex digits",
            "'\\x1'",
            "Hexadecimal character escape requires 2 digits",
            null),
        Arguments.of("string with unicode digits", "'\\u0041'", null, Arrays.asList("'\\u0041'")),
        Arguments.of(
            "string with invalid unicode digits",
            "'\\uzzzz'",
            "Unicode character escape requires 4 digits",
            null),
        Arguments.of(
            "string with insufficient unicode digits",
            "'\\u1'",
            "Unicode character escape requires 4 digits",
            null),
        Arguments.of("string with escaped eof", "'\\", "No closing ' found", null),
        Arguments.of(
            "plural typed constant with escaped eof", "$items[true\\", "No closing ] found", null),
        Arguments.of(
            "plural typed constant with escaped space",
            "$effects[\n\tBuy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!\n]",
            null,
            Arrays.asList("$", "effects", "[", "Buy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!", "]")),
        Arguments.of("unterminated plural aggregate typ", "int[", "Missing index token", null),
        // TODO: add tests for size == 0, size < 0, which ought to fail.
        Arguments.of(
            "array with explicit size",
            "int[2] x;",
            null,
            Arrays.asList("int", "[", "2", "]", "x", ";")),
        Arguments.of(
            "array with unspecified size",
            "int[] x;",
            null,
            Arrays.asList("int", "[", "]", "x", ";")),
        Arguments.of(
            "multidimensional array with partially specified size (1)",
            "int[2][] x;",
            null,
            Arrays.asList("int", "[", "2", "]", "[", "]", "x", ";")),
        Arguments.of(
            "multidimensional array with partially specified size (2)",
            "int[][2] x;",
            null,
            Arrays.asList("int", "[", "]", "[", "2", "]", "x", ";")),
        Arguments.of(
            "multidimensional array with explicit size",
            "int[2, 2] x;",
            null,
            Arrays.asList("int", "[", "2", ",", "2", "]", "x", ";")),
        // TODO: `typedef int[] intArray` and aggregate shouldn't be valid keys.
        Arguments.of(
            "map with non-primitive key type",
            "record a{int b;}; int[a] x;",
            "Index type 'a' is not a primitive type",
            null),
        Arguments.of(
            "multidimensional map with partially specified comma-separated type",
            "int[2,] x;",
            "Missing index token",
            null),
        Arguments.of(
            "multidimensional map with unspecified comma-separated type",
            "int[,,,] x;",
            "Missing index token",
            null),
        Arguments.of(
            "multidimensional map with partially-specified comma-separated type",
            "int[int,] x;",
            "Missing index token",
            null),
        Arguments.of(
            "abruptly terminated 1-d map", "int[int", "Expected , or ], found end of file", null),
        Arguments.of(
            "abruptly terminated 1-d array", "int[4", "Expected , or ], found end of file", null),
        Arguments.of("map with unknown type", "int[a] x;", "Invalid type name 'a'", null),
        Arguments.of(
            "map containing aggregate type", "int[int[]] x;", "Expected , or ], found [", null),
        Arguments.of(
            "multidimensional map with comma-separated type",
            "int[int, int] x;",
            null,
            Arrays.asList("int", "[", "int", ",", "int", "]", "x", ";")),
        Arguments.of(
            "multidimensional map with chained brackets with types",
            "int[int][int] x;",
            null,
            Arrays.asList("int", "[", "int", "]", "[", "int", "]", "x", ";")),
        Arguments.of(
            "multidimensional map with unspecified type in chained empty brackets",
            "int[][] x;",
            null,
            Arrays.asList("int", "[", "]", "[", "]", "x", ";")),
        Arguments.of(
            "single static declaration",
            "static int x;",
            null,
            Arrays.asList("static", "int", "x", ";")),
        Arguments.of(
            "single static definition",
            "static int x = 1;",
            null,
            Arrays.asList("static", "int", "x", "=", "1", ";")),
        Arguments.of(
            "multiple static definition",
            "static int x = 1, y = 2;",
            null,
            Arrays.asList("static", "int", "x", "=", "1", ",", "y", "=", "2", ";")),
        Arguments.of(
            "static type but no declaration",
            "static int;",
            "Type given but not used to declare anything",
            null),
        Arguments.of(
            "static command",
            "static print('hello world');",
            null,
            Arrays.asList("static", "print", "(", "'hello world'", ")", ";")),
        Arguments.of(
            "simple static block",
            "static { int x; }",
            null,
            Arrays.asList("static", "{", "int", "x", ";", "}")),
        Arguments.of(
            "unterminated static declaration",
            "static int x\nint y;",
            "Expected ;, found int",
            null),
        Arguments.of(
            "unterminated static block", "static {", "Expected }, found end of file", null),
        Arguments.of("lone static", "static;", "command or declaration required", null),
        Arguments.of(
            "unterminated typedef", "typedef int foo\n foo bar;", "Expected ;, found foo", null),
        Arguments.of("typedef with no type", "typedef;", "Missing data type for typedef", null),
        Arguments.of("typedef with no name", "typedef int;", "Type name expected", null),
        Arguments.of(
            "typedef with non-identifier name", "typedef int 1;", "Invalid type name '1'", null),
        Arguments.of(
            "typedef with reserved name",
            "typedef int remove;",
            "Reserved word 'remove' cannot be a type name",
            null),
        Arguments.of(
            "typedef with existing name",
            "typedef float double; typedef int double;",
            "Type name 'double' is already defined",
            null),
        Arguments.of(
            "equivalent typedef redefinition",
            "typedef float double; typedef float double;",
            null,
            Arrays.asList("typedef", "float", "double", ";", "typedef", "float", "double", ";")),
        Arguments.of(
            "inner main",
            "void foo() { void main() {} }",
            "main method must appear at top level",
            null),
        Arguments.of(
            "unterminated top-level declaration", "int x\nint y", "Expected ;, found int", null),
        Arguments.of(
            "type but no declaration", "int;", "Type given but not used to declare anything", null),
        Arguments.of(
            "aggregate-initialized definition",
            "int[1] x { 1 };",
            null,
            Arrays.asList("int", "[", "1", "]", "x", "{", "1", "}", ";")),
        Arguments.of("record with no body", "record a;", "Expected {, found ;", null),
        Arguments.of("record with no name or body", "record;", "Record name expected", null),
        Arguments.of(
            "record with non-identifier name",
            "record 1 { int a;};",
            "Invalid record name '1'",
            null),
        Arguments.of(
            "record with reserved name",
            "record int { int a;};",
            "Reserved word 'int' cannot be a record name",
            null),
        Arguments.of(
            "record redefinition",
            "record a { int a;}; record a { int a;};",
            "Record name 'a' is already defined",
            null),
        Arguments.of("record without fields", "record a {};", "Record field(s) expected", null),
        Arguments.of(
            "record with void field type",
            "record a { void a;};",
            "Non-void field type expected",
            null),
        Arguments.of(
            "record with unknown field type", "record a { foo a;};", "Type name expected", null),
        Arguments.of("record with no field name", "record a { int;};", "Field name expected", null),
        Arguments.of(
            "record with non-identifier field name",
            "record a { int 1;};",
            "Invalid field name '1'",
            null),
        Arguments.of(
            "record with reserved field name",
            "record a { int class;};",
            "Reserved word 'class' cannot be used as a field name",
            null),
        Arguments.of(
            "record with repeated field name",
            "record a { int b; int b;};",
            "Field name 'b' is already defined",
            null),
        Arguments.of(
            "record with missing semicolon",
            "record a { int b\n float c;};",
            "Expected ;, found float",
            null),
        Arguments.of(
            "unterminated record", "record a { int b;", "Expected }, found end of file", null),
        Arguments.of(
            "declaration with non-identifier name",
            "int 1;",
            "Type given but not used to declare anything",
            null),
        Arguments.of(
            "declaration with reserved name",
            "int class;",
            "Reserved word 'class' cannot be a variable name",
            null),
        Arguments.of(
            "multiple-definition", "int x = 1; int x = 2;", "Variable x is already defined", null),
        Arguments.of(
            "variable declaration followed by definition",
            // One might expect this to be acceptable.
            "int x; int x = 1;",
            "Variable x is already defined",
            null),
        Arguments.of(
            "parameter initialization",
            "int f(int x = 1) {}",
            "Cannot initialize parameter x",
            null),
        Arguments.of(
            "brace assignment of primitive",
            "int x = {1}",
            "Cannot initialize x of type int with an aggregate literal",
            null),
        Arguments.of(
            "brace assignment of array",
            "int[] x = {1, 2, 3};",
            null,
            Arrays.asList("int", "[", "]", "x", "=", "{", "1", ",", "2", ",", "3", "}", ";")),
        Arguments.of(
            "invalid coercion", "int x = 'hello';", "Cannot store string in x of type int", null),
        Arguments.of(
            "float->int assignment",
            "int x = 1.0;",
            null,
            Arrays.asList("int", "x", "=", "1", ".", "0", ";")),
        Arguments.of(
            "int->float assignment",
            "float y = 1;",
            null,
            Arrays.asList("float", "y", "=", "1", ";")),
        Arguments.of("assignment missing rhs", "int x =", "Expression expected", null),
        Arguments.of("assignment missing rhs 2", "int x; x =", "Expression expected", null),
        Arguments.of(
            "Invalid assignment coercion - logical",
            "int x; x &= true",
            "&= requires an integer or boolean expression and an integer or boolean variable reference",
            null),
        Arguments.of(
            "Invalid assignment coercion - integer",
            "boolean x; x >>= 1",
            ">>= requires an integer expression and an integer variable reference",
            null),
        Arguments.of(
            "Invalid assignment coercion - assignment",
            "boolean x; x += 'foo'",
            "Cannot store string in x of type boolean",
            null),
        Arguments.of("break outside loop", "break;", "Encountered 'break' outside of loop", null),
        Arguments.of(
            "continue outside loop", "continue;", "Encountered 'continue' outside of loop", null),
        Arguments.of(
            // Catch in ASH is comparable to a function that catches and
            // returns the text of any errors thrown by its block.
            "catch without try", "catch {}", null, Arrays.asList("catch", "{", "}")),
        Arguments.of(
            "finally without try",
            "finally {}",
            // "Encountered 'finally' without try",
            "Unknown variable 'finally'",
            null),
        Arguments.of(
            "try without catch or finally",
            "try {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless",
            null),
        Arguments.of(
            "try-catch",
            "try {} catch {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless",
            null),
        Arguments.of(
            "try-finally",
            "try {} finally {}",
            null,
            Arrays.asList("try", "{", "}", "finally", "{", "}")),
        Arguments.of(
            "catch value block",
            "string error_message = catch {}",
            null,
            Arrays.asList("string", "error_message", "=", "catch", "{", "}")),
        Arguments.of(
            "catch value expression",
            "string error_message = catch ( print(__FILE__) )",
            null,
            Arrays.asList(
                "string", "error_message", "=", "catch", "(", "print", "(", "__FILE__", ")", ")")),
        Arguments.of(
            "catch value interrupted",
            "string error_message = catch",
            "\"catch\" requires a block or an expression",
            null),
        Arguments.of(
            // This has been permitted historically...
            "return outside function",
            "return;",
            // "Return needs null value",
            "Cannot return when outside of a function",
            null
            // Arrays.asList( "return", ";" )
            ),
        Arguments.of("top-level exit", "exit;", null, Arrays.asList("exit", ";")),
        Arguments.of("empty block", "{}", null, Arrays.asList("{", "}")),
        Arguments.of("exit with parameter", "exit 1;", "Expected ;, found 1", null),
        Arguments.of(
            "break inside while-loop",
            "while (true) break;",
            null,
            Arrays.asList("while", "(", "true", ")", "break", ";")),
        Arguments.of(
            "continue inside while-loop",
            "while (true) continue;",
            null,
            Arrays.asList("while", "(", "true", ")", "continue", ";")),
        Arguments.of(
            "break inside switch",
            "switch (true) { default: break; }",
            null,
            Arrays.asList("switch", "(", "true", ")", "{", "default", ":", "break", ";", "}")),
        Arguments.of(
            "continue inside switch",
            "switch (true) { default: continue; }",
            "Encountered 'continue' outside of loop",
            null),
        Arguments.of(
            "empty foreach",
            "foreach cls in $classes[];",
            null,
            Arrays.asList("foreach", "cls", "in", "$", "classes", "[", "]", ";")),
        Arguments.of(
            "for-from-to",
            "for i from 1 to 10000;",
            null,
            Arrays.asList("for", "i", "from", "1", "to", "10000", ";")),
        Arguments.of(
            "for-from-upto",
            "for i from 1 upto 10000;",
            null,
            Arrays.asList("for", "i", "from", "1", "upto", "10000", ";")),
        Arguments.of(
            "for-from-to-by",
            "for i from 1 to 10000 by 100;",
            null,
            Arrays.asList("for", "i", "from", "1", "to", "10000", "by", "100", ";")),
        Arguments.of(
            "for-from-downto",
            // This is valid, but will immediately return.
            "for i from 1 downto 10000;",
            null,
            Arrays.asList("for", "i", "from", "1", "downto", "10000", ";")),
        Arguments.of("no return from int function", "int f() {}", "Missing return value", null),
        Arguments.of(
            "return void from int function", "int f() { return; }", "Return needs int value", null),
        Arguments.of(
            "return string from int function",
            "int f() { return 'str'; }",
            "Cannot return string value from int function",
            null),
        Arguments.of(
            "return int from void function",
            "void f() { return 1; }",
            "Cannot return a value from a void function",
            null),
        Arguments.of(
            "non-expression return", "int f() { return (); }", "Expression expected", null),
        Arguments.of(
            "single-command if",
            "if (true) print('msg');",
            null,
            Arrays.asList("if", "(", "true", ")", "print", "(", "'msg'", ")", ";")),
        Arguments.of("empty if", "if (true);", null, Arrays.asList("if", "(", "true", ")", ";")),
        Arguments.of("unclosed block scope", "{", "Expected }, found end of file", null),
        // if / else-if / else
        Arguments.of("if without condition", "if true", "Expected (, found true", null),
        Arguments.of(
            "if with empty condition", "if ()", "\"if\" requires a boolean condition", null),
        Arguments.of(
            "if with numeric condition", "if (1)", "\"if\" requires a boolean condition", null),
        Arguments.of(
            "if with unclosed condition", "if (true", "Expected ), found end of file", null),
        // These probably shouldn't need to be separate test cases...
        Arguments.of(
            "else if without condition",
            "if (false); else if true",
            "Expected (, found true",
            null),
        Arguments.of(
            "else if with empty condition",
            "if (false); else if ()",
            "\"if\" requires a boolean condition",
            null),
        Arguments.of(
            "else if with unclosed condition",
            "if (false); else if (true",
            "Expected ), found end of file",
            null),
        // while
        Arguments.of("while without condition", "while true", "Expected (, found true", null),
        Arguments.of(
            "while with empty condition",
            "while ()",
            "\"while\" requires a boolean condition",
            null),
        Arguments.of(
            "while with unclosed condition", "while (true", "Expected ), found end of file", null),
        Arguments.of(
            "while with unclosed loop", "while (true) {", "Expected }, found end of file", null),
        Arguments.of(
            "while with multiple statements but no semicolon",
            "while (true) print(5)\nprint(6)",
            "Expected ;, found print",
            null),
        // repeat
        Arguments.of(
            "repeat statement",
            "repeat print('hello'); until(true);",
            null,
            Arrays.asList(
                "repeat", "print", "(", "'hello'", ")", ";", "until", "(", "true", ")", ";")),
        Arguments.of(
            "repeat without until", "repeat {}", "Expected until, found end of file", null),
        Arguments.of(
            "repeat without condition", "repeat {} until true", "Expected (, found true", null),
        Arguments.of(
            "repeat with empty condition",
            "repeat {} until ('done')",
            // This should probably read as "'until' requires a
            // boolean condition"...
            "\"repeat\" requires a boolean condition",
            null),
        // So many cases of identical tests for duplicate code...
        Arguments.of(
            "repeat with unclosed condition",
            "repeat {} until (true",
            "Expected ), found end of file",
            null),
        // switch
        Arguments.of(
            "switch without condition or block",
            "switch true {}",
            "Expected ( or {, found true",
            null),
        Arguments.of(
            "switch with empty condition",
            "switch ()",
            "\"switch ()\" requires an expression",
            null),
        Arguments.of(
            "switch with unclosed condition",
            "switch (true",
            "Expected ), found end of file",
            null),
        Arguments.of(
            "switch with condition but no block",
            "switch (true)",
            "Expected {, found end of file",
            null),
        Arguments.of(
            "switch with condition but unclosed block",
            "switch (true) {",
            "Expected }, found end of file",
            null),
        Arguments.of(
            "switch with block and no condition",
            "switch { }",
            null,
            Arrays.asList("switch", "{", "}")),
        Arguments.of(
            "switch with block, non-const label",
            "boolean x; switch { case x: }",
            null,
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "x", ":", "}")),
        Arguments.of(
            "switch with block, label expression",
            "boolean x; switch { case !x: }",
            null,
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "!", "x", ":", "}")),
        Arguments.of(
            "switch with block, nested variable",
            "switch { case true: int x; }",
            null,
            Arrays.asList("switch", "{", "case", "true", ":", "int", "x", ";", "}")),
        Arguments.of(
            "switch with block, nested type but no variable",
            "switch { case true: int; }",
            "Type given but not used to declare anything",
            null),
        Arguments.of(
            "switch with block, nested variable but missing semicolon",
            "switch { case true: int x }",
            "Expected ;, found }",
            null),
        Arguments.of(
            "switch, case label without expression",
            "switch { case: }",
            "Case label needs to be followed by an expression",
            null),
        Arguments.of(
            "switch case not terminated by colon",
            "switch { case true; }",
            "Expected :, found ;",
            null),
        Arguments.of(
            "switch default not terminated by colon",
            "switch { default true; }",
            "Expected :, found true",
            null),
        Arguments.of(
            "switch type mismatch",
            "switch (1) { case true: }",
            "Switch conditional has type int but label expression has type boolean",
            null),
        Arguments.of(
            "switch block type mismatch",
            // Note that the implicit switch type is boolean here.
            "switch { case 1: }",
            "Switch conditional has type boolean but label expression has type int",
            null),
        Arguments.of(
            "duplicate switch label",
            "switch (1) { case 0: case 0: }",
            "Duplicate case label: 0",
            null),
        Arguments.of(
            "switch, multiple default labels",
            "switch (1) { default: default: }",
            "Only one default label allowed in a switch statement",
            null),
        Arguments.of(
            "switch block, multiple default labels",
            "switch { default: default: }",
            "Only one default label allowed in a switch statement",
            null),
        Arguments.of(
            "variable definition of sort",
            "int sort = 0;",
            null,
            Arrays.asList("int", "sort", "=", "0", ";")),
        Arguments.of(
            "variable declaration of sort", "int sort;", null, Arrays.asList("int", "sort", ";")),
        Arguments.of(
            "function named sort",
            "void sort(){} sort();",
            null,
            Arrays.asList("void", "sort", "(", ")", "{", "}", "sort", "(", ")", ";")),
        Arguments.of(
            "sort not-a-variable primitive",
            "sort 2 by value;",
            "Aggregate reference expected",
            null),
        Arguments.of("sort without by", "int[] x {3,2,1}; sort x;", "Expected by, found ;", null),
        Arguments.of(
            "Sort, no sorting expression", "int[] x; sort x by", "Expression expected", null),
        Arguments.of(
            "valid sort",
            "int[] x; sort x by value*3;",
            null,
            Arrays.asList("int", "[", "]", "x", ";", "sort", "x", "by", "value", "*", "3", ";")),
        Arguments.of(
            "foreach with non-identifier key",
            "foreach 'key' in $items[];",
            "Key variable name expected",
            null),
        Arguments.of(
            "foreach with reserved key",
            "foreach item in $items[];",
            "Reserved word 'item' cannot be a key variable",
            null),
        Arguments.of("foreach missing `in`", "foreach it;", "Expected in, found ;", null),
        Arguments.of(
            "foreach missing key variable name",
            "foreach in it;",
            "Key variable name expected",
            null),
        Arguments.of(
            "foreach key variable named 'in'",
            "foreach in in it;",
            "Reserved word 'in' cannot be a key variable name",
            null),
        Arguments.of(
            "foreach key variable named 'in' 2",
            "foreach in, on, under, below, through in it;",
            "Reserved word 'in' cannot be a key variable name",
            null),
        Arguments.of(
            "foreach in not-a-reference",
            "foreach it in $item[none];",
            "Aggregate reference expected",
            null),
        Arguments.of(
            "foreach with duplicate key",
            "foreach it, it in $items[];",
            "Key variable 'it' is already defined",
            null),
        Arguments.of(
            "foreach with multiple keys",
            "foreach key, value in int[int]{} {}",
            null,
            Arrays.asList(
                "foreach", "key", ",", "value", "in", "int", "[", "int", "]", "{", "}", "{", "}")),
        Arguments.of(
            "foreach with too many keys",
            "foreach a, b, c in $items[];",
            "Too many key variables specified",
            null),
        Arguments.of(
            "for with reserved index",
            "for int from 1 upto 10;",
            "Reserved word 'int' cannot be an index variable",
            null),
        Arguments.of(
            "for with existing index",
            // Oddly, this is unsupported, when other for loops will create
            // a nested scope.
            "int i; for i from 1 upto 10;",
            "Index variable 'i' is already defined",
            null),
        Arguments.of(
            "for without from", "for i in range(10):\n  print(i)", "Expected from, found in", null),
        Arguments.of(
            "for with invalid dest keyword",
            "for i from 1 until 10;",
            "Expected to, upto, or downto, found until",
            null),
        Arguments.of(
            "javaFor with multiple declarations",
            "for (int i=0, int length=5; i < length; i++);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ",", "int", "length", "=", "5", ";", "i", "<",
                "length", ";", "i", "++", ")", ";")),
        Arguments.of(
            "javaFor with empty initializer",
            "for (int i=0,; i < 5; ++i);",
            "Identifier expected",
            null),
        Arguments.of(
            "javaFor with compound increment",
            "for (int i=0; i < 5; i+=1);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "+=", "1", ")",
                ";")),
        Arguments.of(
            "javaFor with existing variable",
            "int i; for (i=0; i < 5; i++);",
            null,
            Arrays.asList(
                "int", "i", ";", "for", "(", "i", "=", "0", ";", "i", "<", "5", ";", "i", "++", ")",
                ";")),
        Arguments.of(
            "javaFor with unknown existing variable",
            "for (i=0; i < 5; i++);",
            "Unknown variable 'i'",
            null),
        Arguments.of(
            "javaFor with redefined existing variable",
            "int i; for (int i=0; i < 5; i++);",
            "Variable 'i' already defined",
            null),
        Arguments.of(
            "javaFor with not-an-increment",
            "for (int i=0; i < 5; i==1);",
            "Variable 'i' not incremented",
            null),
        Arguments.of(
            "javaFor with constant assignment",
            // I guess this technically works... but will be an infinite
            // loop in practice.
            "for (int i=0; i < 5; i=1);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "=", "1", ")",
                ";")),
        Arguments.of(
            "javaFor missing initial identifier",
            "for (0; i < 5; i++);",
            "Identifier required",
            null),
        Arguments.of(
            "javaFor missing initializer expression",
            "for (int i =; i < 5; i++);",
            "Expression expected",
            null),
        Arguments.of(
            "javaFor invalid assignment",
            "for (int i ='abc'; i < 5; i++);",
            "Cannot store string in i of type int",
            null),
        Arguments.of(
            "javaFor non-boolean condition",
            "for (int i; i + 5; i++);",
            "\"for\" requires a boolean conditional expression",
            null),
        Arguments.of(
            "javaFor multiple increments",
            "for (int i, int j; i + j < 5; i++, j++);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", ",", "int", "j", ";", "i", "+", "j", "<", "5", ";", "i",
                "++", ",", "j", "++", ")", ";")),
        Arguments.of(
            "javaFor interrupted multiple increments",
            "for (int i; i < 5; i++,);",
            "Identifier expected",
            null),
        Arguments.of(
            "undefined function call",
            "prin();",
            "Function 'prin( )' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts.",
            null),
        Arguments.of("function call interrupted", "print(", "Expected ), found end of file", null),
        Arguments.of(
            "function call interrupted after comma",
            "print(1,",
            "Expected parameter, found end of file",
            null),
        Arguments.of(
            "function call closed after comma", "print(1,);", "Expected parameter, found )", null),
        Arguments.of(
            "function call interrupted after param",
            "print(1",
            "Expected ), found end of file",
            null),
        Arguments.of(
            "function call with non-comma separator", "print(1; 2);", "Expected ), found ;", null),
        Arguments.of(
            "function parameter coercion to ANY_TYPE",
            "dump('foo', 'bar');",
            null,
            Arrays.asList("dump", "(", "'foo'", ",", "'bar'", ")", ";")),
        Arguments.of(
            "function parameter no typedef coercion",
            "typedef int foo; foo a = 1; void bar(int x, foo y) {} bar(a, 1);",
            null,
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "void", "bar", "(", "int",
                "x", ",", "foo", "y", ")", "{", "}", "bar", "(", "a", ",", "1", ")", ";")),
        Arguments.of(
            "function parameter typedef-to-simple typedef coercion",
            // Mmh... there's no real way to "prove" the function was used other than
            // seeing it checked from clover...
            "typedef int foo; foo a = 1; int to_int(foo x) {return 1;} void bar(int x) {} bar(a);",
            null,
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "int", "to_int", "(",
                "foo", "x", ")", "{", "return", "1", ";", "}", "void", "bar", "(", "int", "x", ")",
                "{", "}", "bar", "(", "a", ")", ";")),
        Arguments.of(
            "function parameter simple-to-typedef typedef coercion",
            "typedef int foo; foo a = 1; foo to_foo(int x) {return a;} void bar(foo x) {} bar(1);",
            null,
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "foo", "to_foo", "(",
                "int", "x", ")", "{", "return", "a", ";", "}", "void", "bar", "(", "foo", "x", ")",
                "{", "}", "bar", "(", "1", ")", ";")),
        Arguments.of(
            "function invocation interrupted",
            "call",
            "Variable reference expected for function name",
            null),
        Arguments.of(
            "function invocation non-string expression",
            "call (2)()",
            "String expression expected for function name",
            null),
        Arguments.of(
            "function invocation interrupted after name expression",
            "call ('foo')",
            "Expected (, found end of file",
            null),
        Arguments.of(
            "function invocation with non-void function",
            // ummm this should insist that the variable is a string...
            "int x; call string x('foo')",
            null,
            Arrays.asList("int", "x", ";", "call", "string", "x", "(", "'foo'", ")")),
        Arguments.of(
            "preincrement with non-numeric variable",
            "string x; ++x;",
            "++X requires a numeric variable reference",
            null),
        Arguments.of(
            "preincrement requires a variable", "++1;", "Variable reference expected", null),
        Arguments.of(
            "predecrement with float variable",
            "float x; --x;",
            null,
            Arrays.asList("float", "x", ";", "--", "x", ";")),
        Arguments.of(
            "postincrement with non-numeric variable",
            "string x; x++;",
            "X++ requires a numeric variable reference",
            null),
        /* Currently fails with "Expected ;, found ++" which is asymmetric.
        Arguments.of(
          "postincrement requires a variable",
          "1++;",
          "Variable reference expected",
          null),
        */
        Arguments.of(
            "postdecrement with float variable",
            "float x; x--;",
            null,
            Arrays.asList("float", "x", ";", "x", "--", ";")),
        Arguments.of(
            "ternary with non-boolean condition",
            "int x = 1 ? 1 : 2;",
            "Non-boolean expression 1 (int)",
            null),
        Arguments.of(
            "ternary without lhs", "int x = true ? : 2;", "Value expected in left hand side", null),
        Arguments.of("ternary without colon", "int x = true ? 1;", "Expected :, found ;", null),
        Arguments.of(
            "ternary without rhs",
            "int x = true ? 1:;",
            // Another asymmetry: not "Value expected in right hand side"
            "Value expected",
            null),
        Arguments.of(
            "non-coercible value mismatch",
            "(true ? 1 : $item[none];",
            "Cannot choose between 1 (int) and none (item)",
            null),
        // parseValue
        Arguments.of(
            "unclosed parenthetical expression", "(true", "Expected ), found end of file", null),
        Arguments.of("aggregate literal without braces", "(int[])", "Expected {, found )", null),
        Arguments.of(
            "indexed variable reference",
            "int[5] x; x[0];",
            null,
            Arrays.asList("int", "[", "5", "]", "x", ";", "x", "[", "0", "]", ";")),
        Arguments.of("indexed primitive", "int x; x[0];", "Variable 'x' cannot be indexed", null),
        Arguments.of(
            "over-indexed variable reference", "int[5] x; x[0,1];", "Too many keys for 'x'", null),
        Arguments.of(
            "empty indexed variable reference", "int[5] x; x[];", "Index for 'x' expected", null),
        Arguments.of(
            "unterminated aggregate variable reference",
            "int[5] x; x[0",
            "Expected ], found end of file",
            null),
        Arguments.of(
            "type-mismatched indexed variable reference",
            "int[5] x; x['str'];",
            "Index for 'x' has wrong data type (expected int, got string)",
            null),
        Arguments.of(
            "type-mismatched indexed composite reference",
            "int[5, 5] x; x[0]['str'];",
            "Index for 'x[]' has wrong data type (expected int, got string)",
            null),
        Arguments.of(
            "multidimensional comma-separated array index",
            "int[5,5] x; x[0,1];",
            null,
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", ",", "1", "]", ";")),
        Arguments.of(
            "multidimensional bracket-separated array index",
            "int[5,5] x; x[0][1];",
            null,
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", "]", "[", "1", "]", ";")),
        Arguments.of(
            "method call of primitive var",
            "string x = 'hello'; x.print();",
            null,
            Arrays.asList("string", "x", "=", "'hello'", ";", "x", ".", "print", "(", ")", ";")),
        Arguments.of(
            "method call of aggregate index",
            "string[2] x; x[0].print();",
            null,
            Arrays.asList(
                "string", "[", "2", "]", "x", ";", "x", "[", "0", "]", ".", "print", "(", ")",
                ";")),
        Arguments.of("non-record property reference", "int i; i.a;", "Record expected", null),
        Arguments.of(
            "record field reference",
            "record {int a;} r; r.a;",
            null,
            Arrays.asList("record", "{", "int", "a", ";", "}", "r", ";", "r", ".", "a", ";")),
        Arguments.of(
            "record field reference without field",
            "record {int a;} r; r.",
            "Field name expected",
            null),
        Arguments.of(
            "record unknown field reference",
            "record {int a;} r; r.b;",
            "Invalid field name 'b'",
            null),
        Arguments.of(
            "Illegal record creation",
            "void f( record foo {int a; int b;} bar )",
            "Existing type expected for function parameter",
            null),
        Arguments.of(
            "array of record",
            "record {int a;}[] r;",
            null,
            Arrays.asList("record", "{", "int", "a", ";", "}", "[", "]", "r", ";")),
        Arguments.of("standalone new", "new;", "Expected Record name, found ;", null),
        Arguments.of("new non-record", "int x = new int();", "'int' is not a record type", null),
        Arguments.of(
            "new record without parens",
            // Yields a default-constructed record.
            "record r {int a;}; new r;",
            null,
            Arrays.asList("record", "r", "{", "int", "a", ";", "}", ";", "new", "r", ";")),
        Arguments.of(
            "new record with semicolon",
            "record r {int a;}; new r(;",
            "Expression expected for field #1 (a)",
            null),
        Arguments.of(
            "new with aggregate field",
            "record r {int[] a;}; new r({1,2});",
            null,
            Arrays.asList(
                "record", "r", "{", "int", "[", "]", "a", ";", "}", ";", "new", "r", "(", "{", "1",
                ",", "2", "}", ")", ";")),
        Arguments.of(
            "new with field type mismatch",
            "record r {int a;}; new r('str');",
            "string found when int expected for field #1 (a)",
            null),
        Arguments.of(
            "new with too many void fields",
            "record r {int a;}; new r(,,,,,,,);",
            "Too many field initializers for record r",
            null),
        Arguments.of(
            "new without closing paren",
            "record r {int a;}; new r(",
            "Expected ), found end of file",
            null),
        Arguments.of(
            "new without closing paren 2",
            "record r {int a;}; new r(4",
            "Expected , or ), found end of file",
            null),
        Arguments.of(
            "new without closing comma separator",
            "record r {int a; int b;}; new r(4 5)",
            "Expected , or ), found 5",
            null),
        Arguments.of("improper remove", "int i; remove i;", "Aggregate reference expected", null),
        Arguments.of(
            "proper remove",
            "int[] map; remove map[0];",
            null,
            Arrays.asList("int", "[", "]", "map", ";", "remove", "map", "[", "0", "]", ";")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(
      String desc, String script, String errorText, List<String> tokens) {
    ByteArrayInputStream istream =
        new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
    Parser p = new Parser(/*scriptFile=*/ null, /*stream=*/ istream, /*imports=*/ null);

    if (errorText != null) {
      ScriptException e = assertThrows(ScriptException.class, p::parse, desc);
      assertThat(desc, e.getMessage(), containsString(errorText));
      return;
    }

    // This will fail if an exception is thrown.
    p.parse();
    assertEquals(tokens, p.getTokensContent(), desc);
  }
}
