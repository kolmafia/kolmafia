package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tries to parse valid and invalid ASH programs. */
@RunWith(Parameterized.class)
public class ParserTest {
  // Short description of the test case.
  public final String desc;

  // ASH Script contents.
  public final String script;

  public final List<String> tokens;

  // Error text. If null, then we expect no error to be thrown (i.e. the provided script is
  // well-formed).
  public final String errorText;

  @Before
  public void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  @After
  public void clearRevision() {
    StaticEntity.overrideRevision(null);
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    /**
     * @return A list containing arrays with the following spec: String description String errorText
     *     A substring of the expected error message. List<String> tokens that are expected for a
     *     successful parse.
     *     <p>Note that exactly one of (errorText, tokens) should be non-null.
     */
    return Arrays.asList(
        new Object[][] {
          {
            "Unterminated Java for-loop", "for (", "Expected ), found end of file", null,
          },
          {
            "Java for-loop, bad incrementer", "for (;;1)", "Variable reference expected", null,
          },
          {
            "valid empty Java for-loop",
            "for (;;) {}",
            null,
            Arrays.asList("for", "(", ";", ";", ")", "{", "}"),
          },
          {
            "Java for-loop with new variable",
            "for (int i = 0; i < 5; ++i) {}",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "++", "i", ")", "{",
                "}"),
          },
          {
            "Multiline string, end of line not escaped", "'\n'", "No closing ' found", null,
          },
          {
            "Multiline string, end of line properly escaped",
            "'\\\n'",
            null,
            Arrays.asList("'\\", "'"),
          },
          {
            "Multiline string, end of line properly escaped + empty lines",
            "'\\\n\n   \n\n\n'",
            null,
            Arrays.asList("'\\", "'"),
          },
          {
            "Multiline string, end of line properly escaped + empty lines + comment",
            "'\\\n\n\n//Comment\n\n'",
            "No closing ' found",
            null,
          },
          {
            "Unterminated string template",
            "`{`",
            // The parser tries to start a new string template inside the expression
            "No closing ` found",
            null,
          },
          {
            "Abruptly unterminated string template, expecting expression",
            "`{",
            "Expression expected",
            null,
          },
          {
            "Abruptly unterminated string template, parsed expression",
            "`{1",
            "Expected }, found end of file",
            null,
          },
          {
            // Idem

            "Typed constant, no bracket", "$int", "Expected [, found end of file", null,
          },
          {
            "Typed constant non-successive characters",
            "$		boolean 	 [ 		false ]",
            null,
            Arrays.asList("$", "boolean", "[", "false ", "]"),
          },
          {
            "Typed constant escaped characters",
            "$boolean[\\f\\a\\l\\s\\e]",
            null,
            Arrays.asList("$", "boolean", "[", "\\f\\a\\l\\s\\e", "]"),
          },
          {
            "Typed constant bad typecast", "$boolean['']", "Bad boolean value: \"''\"", null,
          },
          {
            "Typed constant, unknown type", "$foo[]", "Unknown type foo", null,
          },
          {
            "Typed constant, non-primitive type",
            "record r {int i;}; $r[]",
            "Non-primitive type r",
            null,
          },
          {
            "Typed constant multiline", "$boolean[\n]", "No closing ] found", null,
          },
          {
            "Typed constant, escaped multiline", "$boolean[\\\n]", "No closing ] found", null,
          },
          {
            "Typed constant, nested brackets, proper",
            "$item[[8042]rock]",
            null,
            Arrays.asList("$", "item", "[", "[8042]rock", "]"),
          },
          {
            "Typed constant, nested brackets, improper",
            "$item[[abc]]",
            "Bad item value: \"[abc]\"",
            null,
          },
          {
            "Typed constant, numeric literal",
            "$item[1]",
            null,
            Arrays.asList("$", "item", "[", "1", "]"),
          },
          {
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
                "]"),
          },
          {"empty typed constant", "$string[]", null, Arrays.asList("$", "string", "[", "]")},
          {
            "Plural constant, abrupt end", "$booleans[", "No closing ] found", null,
          },
          {
            "Plural constant, unknown plural type", "$kolmafia[]", "Unknown type kolmafia", null,
          },
          {
            "Plural constant, RAM-protection", "$strings[]", "Can't enumerate all strings", null,
          },
          {
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
                "]"),
          },
          {
            "Plural constant non-successive characters",
            "$		booleans 	 [ 		 ]",
            null,
            Arrays.asList("$", "booleans", "[", "]"),
          },
          {
            "Plural constant multiline",
            // End-of-lines can appear anywhere
            "$booleans[true\n\n\n,\nfalse, false\n,false,\ntrue,tr\nue]",
            null,
            Arrays.asList(
                "$", "booleans", "[", "true", ",", "false, false", ",false,", "true,tr", "ue", "]"),
          },
          {
            "Plural constant w/ escape characters",
            // *Escaped* end-of-lines and escaped "\n"s get added to the value
            "$booleans[tr\\\nu\\ne]",
            "Bad boolean value: \"tr\nu\ne\"",
            null,
          },
          {
            "Plural constant, nested brackets, proper",
            "$items[[8042]rock]",
            null,
            Arrays.asList("$", "items", "[", "[8042]rock", "]"),
          },
          {
            "Plural constant, nested brackets, improper",
            "$items[[abc]]",
            "Bad item value: \"[abc]\"",
            null,
          },
          {
            "Plural constant, single slash",
            "$booleans[tr/Comment\nue]",
            "Bad boolean value: \"tr/Commentue\"",
            null,
          },
          {
            "Plural constant, comment",
            "$booleans[tr//Comment\nue]",
            null,
            Arrays.asList("$", "booleans", "[", "tr", "//Comment", "ue", "]"),
          },
          {
            "Plural constant, two line-separated slashes",
            "$booleans[tr/\n/ue]",
            "Bad boolean value: \"tr//ue\"",
            null,
          },
          {
            "Plural constant, line-separated multiline comment",
            "$booleans[tr/\n**/ue]",
            "Bad boolean value: \"tr/**/ue\"",
            null,
          },
          {
            "Mid-line // comment",
            "int x = // interrupting comment\n  5;",
            null,
            Arrays.asList("int", "x", "=", "// interrupting comment", "5", ";"),
          },
          {
            "Mid-line # comment",
            // This ought to only accept full-line comments, but it's incorrectly implemented,
            // and at this point, widely used enough that this isn't feasible to change.
            "int x = # interrupting comment\n  5;",
            null,
            Arrays.asList("int", "x", "=", "# interrupting comment", "5", ";"),
          },
          {
            "Multiline comment",
            "int x =/* this\n    is a comment\n   */ 5;",
            null,
            // Note that this drops some leading whitespace.
            Arrays.asList("int", "x", "=", "/* this", "is a comment", "*/", "5", ";"),
          },
          {
            "Multiline comment on one line",
            "int x =/* this is a comment */ 5;",
            null,
            Arrays.asList("int", "x", "=", "/* this is a comment */", "5", ";"),
          },
          {
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
                "}"),
          },
          {
            "Unterminated aggregate literal", "int[int] {", "Expected }, found end of file", null,
          },
          {
            "Interrupted map literal", "int[int] {:", "Script parsing error", null,
          },
          {
            "Simple array literal",
            "int[5] { 1, 2, 3, 4, 5}",
            null,
            Arrays.asList(
                "int", "[", "5", "]", "{", "1", ",", "2", ",", "3", ",", "4", ",", "5", "}"),
          },
          {
            "Array literal with trailing comma",
            "int[2] { 1, 2, }",
            null,
            Arrays.asList("int", "[", "2", "]", "{", "1", ",", "2", ",", "}"),
          },
          {
            "Array literal with variable",
            "int x = 10; int[5] { 1, 2, x, 4, 5}",
            null,
            Arrays.asList(
                "int", "x", "=", "10", ";", "int", "[", "5", "]", "{", "1", ",", "2", ",", "x", ",",
                "4", ",", "5", "}"),
          },
          /*
          {
          	// We ought to check for this case too, but we don't...
          	"Array literal not enough elements",
          	"int[10] { 1, 2, 3, 4, 5}",
          	"Array has 10 elements but 5 initializers.",
          	null,
          },
          */
          {
            "Array literal too many elements",
            "int[1] { 1, 2, 3, 4, 5 }",
            "Array has 1 elements but 5 initializers.",
            null,
          },
          {
            "Empty multidimensional map literal",
            "int[int, int]{}",
            null,
            Arrays.asList("int", "[", "int", ",", "int", "]", "{", "}"),
          },
          {
            "Non-empty multidimensional map literal",
            "int[int, int, int]{ {}, { 1:{2, 3} } }",
            null,
            Arrays.asList(
                "int", "[", "int", ",", "int", ",", "int", "]", "{", "{", "}", ",", "{", "1", ":",
                "{", "2", ",", "3", "}", "}", "}"),
          },
          {
            "Interrupted multidimensional map literal",
            "int[int, int] {1:",
            "Script parsing error",
            null,
          },
          {
            "Invalid array literal coercion", "int[int]{ 'foo' }", "Invalid array literal", null,
          },
          {
            "Invalid map literal coercion", "int[int]{ 'foo':'bar' }", "Invalid map literal", null,
          },
          {
            "Ambiguity between array and map literal",
            "boolean[5]{ 0:true, 1:true, 2:false, true, 4:false }",
            "Expected :, found ,",
            null,
          },
          {
            "Ambiguity between array and map literal 2: index and data are both integers",
            "int[5]{ 0, 1, 2, 3:3, 4 }",
            "Cannot include keys when making an array literal",
            null,
          },
          {
            "Ambiguity between array and map literal 3: that can't be a key",
            "string[5]{ '0', '1', '2', '3':'3', '4' }",
            "Expected , or }, found :",
            null,
          },
          {
            // This... exercises a different code path.
            "Parenthesized map literal",
            "(int[int]{})",
            null,
            Arrays.asList("(", "int", "[", "int", "]", "{", "}", ")"),
          },
          {
            // Why is this allowed...? This is the only way I could think of to exercise the
            // rewind functionality.
            "Typedef of existing variable",
            "int foo; typedef int foo; (\nfoo\n\n + 2);",
            null,
            Arrays.asList(
                "int", "foo", ";", "typedef", "int", "foo", ";", "(", "foo", "+", "2", ")", ";"),
          },
          {
            "interrupted script directive", "script", "Expected <, found end of file", null,
          },
          {
            "script directive delimited with <>",
            "script <zlib.ash>;",
            null,
            Arrays.asList("script", "<zlib.ash>", ";"),
          },
          {
            "script directive delimited with \"\"",
            "script \"zlib.ash\"",
            null,
            Arrays.asList("script", "\"zlib.ash\""),
          },
          {
            "script directive without delimiter",
            "script zlib.ash",
            null,
            Arrays.asList("script", "zlib.ash"),
          },
          {
            "Unterminated script directive", "script \"zlib.ash", "No closing \" found", null,
          },
          {
            "Script with bom", "\ufeff    'hello world'", null, Arrays.asList("'hello world'"),
          },
          {
            "Simple operator assignment",
            "int x = 3;",
            null,
            Arrays.asList("int", "x", "=", "3", ";"),
          },
          {
            "Compound operator assignment",
            "int x; x += 3;",
            null,
            Arrays.asList("int", "x", ";", "x", "+=", "3", ";"),
          },
          {
            "Aggregate assignment to primitive",
            // What is this, C?
            "int x; x = {1};",
            "Cannot use an aggregate literal for type int",
            null,
          },
          {
            "Primitive assignment to aggregate",
            "int[4] x = 1;",
            "Cannot store int in x of type int [4]",
            null,
          },
          {
            "Compound assignment to aggregate",
            "int[4] x; x += 1;",
            "Cannot use '+=' on an aggregate",
            null,
          },
          {
            "Aggregate assignment to aggregate",
            "int[4] x; x = {1, 2, 3, 4};",
            null,
            Arrays.asList(
                "int", "[", "4", "]", "x", ";", "x", "=", "{", "1", ",", "2", ",", "3", ",", "4",
                "}", ";"),
          },
          {
            "since passes for low revision",
            "since r100;",
            null,
            Arrays.asList("since", "r100", ";"),
          },
          {
            "since fails for high revision",
            "since r2000000000;",
            "requires revision r2000000000 of kolmafia or higher",
            null,
          },
          {
            "Invalid since version", "since 10;", "invalid 'since' format", null,
          },
          {
            "since passes for low version", "since 1.0;", null, Arrays.asList("since", "1.0", ";"),
          },
          {
            "since fails for high version", "since 2000000000.0;", "final point release", null,
          },
          {
            "since fails for not-a-number", "since yesterday;", "invalid 'since' format", null,
          },
          {
            "Basic function with one argument",
            "void f(int a) {}",
            null,
            Arrays.asList("void", "f", "(", "int", "a", ")", "{", "}"),
          },
          {
            "Basic function with forward reference",
            "void foo(); void bar() { foo(); } void foo() { return; } bar();",
            null,
            Arrays.asList(
                "void", "foo", "(", ")", ";", "void", "bar", "(", ")", "{", "foo", "(", ")", ";",
                "}", "void", "foo", "(", ")", "{", "return", ";", "}", "bar", "(", ")", ";"),
          },
          {
            "Invalid function name",
            "void float() {}",
            "Reserved word 'float' cannot be used as a function name",
            null,
          },
          {
            "Basic function interrupted", "void f(", "Expected ), found end of file", null,
          },
          {
            "Basic function parameter interrupted",
            "void f(int",
            "Expected identifier, found end of file",
            null,
          },
          {
            "Basic function duplicate parameter",
            "void f(int a, float a) {}",
            "Parameter a is already defined",
            null,
          },
          {
            "Basic function missing parameter separator",
            "void f(int a float a) {}",
            "Expected ,, found float", // Uh... doesn't quite stand out does it...
            null,
          },
          {
            "Basic function with vararg",
            "void f(int a, string b, float... c) {} f(5, 'foo', 1.2, 6.3, 4.9, 10, -0)",
            null,
            Arrays.asList(
                "void", "f", "(", "int", "a", ",", "string", "b", ",", "float", "...", "c", ")",
                "{", "}", "f", "(", "5", ",", "'foo'", ",", "1", ".", "2", ",", "6", ".", "3", ",",
                "4", ".", "9", ",", "10", ",", "-", "0", ")"),
          },
          {
            "Basic function with multiple varargs",
            "void f(int ... a, int ... b) {}",
            "Only one vararg parameter is allowed",
            null,
          },
          {
            "Basic function with non-terminal vararg",
            "void f(int ... a, float b) {}",
            "The vararg parameter must be the last one",
            null,
          },
          {
            "Basic function overrides library",
            "void round(float n) {}",
            "Function 'round(float)' overrides a library function.",
            null,
          },
          {
            "Basic function defined multiple times",
            "void f() {} void f() {}",
            "Function 'f()' defined multiple times.",
            null,
          },
          {
            "Basic function vararg clash",
            "void f(int a, int ... b) {} void f(int ... a) {}",
            "Function 'f(int ...)' clashes with existing function 'f(int, int ...)'.",
            null,
          },
          {
            "Complex expression parsing",
            // Among other things, this tests that true / false are case insensitive, in case
            // you want to pretend you're working in Python, C, or both.
            "(!(~-5 == 10) && True || FALSE);",
            null,
            Arrays.asList(
                "(", "!", "(", "~", "-", "5", "==", "10", ")", "&&", "True", "||", "FALSE", ")",
                ";"),
          },
          {
            "Interrupted ! expression", "(!", "Value expected", null,
          },
          {
            "Non-boolean ! expression",
            "(!'abc');",
            "\"!\" operator requires a boolean value",
            null,
          },
          {
            "Interrupted ~ expression", "(~", "Value expected", null,
          },
          {
            "Non-boolean/integer ~ expression",
            "(~'abc');",
            "\"~\" operator requires an integer or boolean value",
            null,
          },
          {
            "Interrupted - expression", "(-", "Value expected", null,
          },
          {
            "Interrupted expression after operator", "(1 +", "Value expected", null,
          },
          {
            "String concatenation with left-side coercion",
            "(1 + 'abc');",
            null,
            Arrays.asList("(", "1", "+", "'abc'", ")", ";"),
          },
          {
            "String concatenation with right-side coercion",
            "('abc' + 1);",
            null,
            Arrays.asList("(", "'abc'", "+", "1", ")", ";"),
          },
          {
            "Invalid expression coercion",
            "(true + 1);",
            "Cannot apply operator + to true (boolean) and 1 (int)",
            null,
          },
          {
            "Numeric literal split after negative",
            "-/*negative \nnumber*/1.23;",
            null,
            Arrays.asList("-", "/*negative", "number*/", "1", ".", "23", ";"),
          },
          {
            "Float literal split after decimal",
            "1./*decimal\n*/23;",
            null,
            Arrays.asList("1", ".", "/*decimal", "*/", "23", ";"),
          },
          {
            "Float literal with no integral component",
            "-.123;",
            null,
            Arrays.asList("-", ".", "123", ";"),
          },
          {
            "Float literal with no integral part, non-numeric fractional part",
            "-.123abc;",
            "Expected numeric value, found 123abc",
            null,
          },
          {
            "unary negation",
            "int x; (-x);",
            null,
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
          },
          /*
            There's code for this case, but we encounter a separate error ("Record expected").
          {
          	"Float literal with no decimal component",
          	"123.;",
          	null,
          	Arrays.asList( "123", ".", ";" ),
          },
          */
          {
            "Int literal with method call.",
            "123.to_string();",
            null,
            Arrays.asList("123", ".", "to_string", "(", ")", ";"),
          },
          {
            "Unary minus",
            "int x; (-x);",
            null,
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
          },
          {
            "Chained if/else-if/else",
            "if (false) {} else if (false) {} else if (false) {} else {}",
            null,
            Arrays.asList(
                "if", "(", "false", ")", "{", "}", "else", "if", "(", "false", ")", "{", "}",
                "else", "if", "(", "false", ")", "{", "}", "else", "{", "}"),
          },
          {
            "Multiple else", "if (false) {} else {} else {}", "Else without if", null,
          },
          {
            "else-if after else",
            "if (false) {} else {} else if (true) {}",
            "Else without if",
            null,
          },
          {
            "else without if", "else {}", "Unknown variable 'else'", null,
          },
          {
            "Multiline cli_execute script",
            "cli_execute {\n  echo hello world;\n  echo sometimes we don't have a semicolon\n}",
            null,
            Arrays.asList(
                "cli_execute",
                "{",
                "echo hello world;",
                "echo sometimes we don't have a semicolon",
                "}"),
          },
          {
            "Interrupted cli_execute script",
            "cli_execute {",
            "Expected }, found end of file",
            null,
          },
          {
            "Non-basic-script cli_execute",
            "int cli_execute; cli_execute++",
            null,
            Arrays.asList("int", "cli_execute", ";", "cli_execute", "++"),
          },
          {
            "For loop, no/bad initial expression",
            "for x from",
            "Expression for initial value expected",
            null,
          },
          {
            "For loop, no/bad ceiling expression",
            "for x from 0 to",
            "Expression for floor/ceiling value expected",
            null,
          },
          {
            "For loop, no/bad increment expression",
            "for x from 0 to 1 by",
            "Expression for increment value expected",
            null,
          },
          {
            "basic template string",
            "`this is some math: {4 + 7}`",
            null,
            Arrays.asList("`this is some math: {", "4", "+", "7", "}`"),
          },
          {
            "template string with a new variable",
            "`this is some math: {int x = 4; x + 7}`",
            "Unknown variable 'int'",
            null,
          },
          {
            "template string with predefined variable",
            "int x; `this is some math: {(x = 4) + 7}`",
            null,
            Arrays.asList(
                "int", "x", ";", "`this is some math: {", "(", "x", "=", "4", ")", "+", "7", "}`"),
          },
          {
            "template string with unclosed comment",
            "`this is some math: {7 // what determines the end?}`",
            "Expected }, found end of file",
            null,
          },
          {
            "template string with terminated comment",
            "`this is some math: {7 // turns out you need a newline\r\n\t}`",
            null,
            Arrays.asList(
                "`this is some math: {", "7",
                "// turns out you need a newline", "}`")
          },
          {
            "template string with multiple templates",
            "`{'hello'} {'world'}`",
            null,
            Arrays.asList("`{", "'hello'", "} {", "'world'", "}`")
          },
          {"string with uninteresting escape", "'\\z'", null, Arrays.asList("'\\z'")},
          {
            "string with unclearly terminated octal escape",
            "'\\1131'",
            null,
            // \113 is char code 75, which maps to 'K'
            Arrays.asList("'\\1131'")
          },
          {
            "string with insufficient octal digits",
            "'\\11'",
            "Octal character escape requires 3 digits",
            null,
          },
          {
            "string with invalid octal digits",
            "'\\118'",
            "Octal character escape requires 3 digits",
            null,
          },
          {
            "string with hex digits", "'\\x3fgh'", null, Arrays.asList("'\\x3fgh'"),
          },
          {
            "string with invalid hex digits",
            "'\\xhello'",
            "Hexadecimal character escape requires 2 digits",
            null,
          },
          {
            "string with insufficient hex digits",
            "'\\x1'",
            "Hexadecimal character escape requires 2 digits",
            null,
          },
          {
            "string with unicode digits", "'\\u0041'", null, Arrays.asList("'\\u0041'"),
          },
          {
            "string with invalid unicode digits",
            "'\\uzzzz'",
            "Unicode character escape requires 4 digits",
            null,
          },
          {
            "string with insufficient unicode digits",
            "'\\u1'",
            "Unicode character escape requires 4 digits",
            null,
          },
          {
            "string with escaped eof", "'\\", "No closing ' found", null,
          },
          {
            "plural typed constant with escaped eof", "$items[true\\", "No closing ] found", null,
          },
          {
            "plural typed constant with escaped space",
            "$effects[\n\tBuy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!\n]",
            null,
            Arrays.asList("$", "effects", "[", "Buy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!", "]")
          },
          {
            "unterminated plural aggregate typ", "int[", "Missing index token", null,
          },
          // TODO: add tests for size == 0, size < 0, which ought to fail.
          {
            "array with explicit size",
            "int[2] x;",
            null,
            Arrays.asList("int", "[", "2", "]", "x", ";"),
          },
          {
            "array with unspecified size",
            "int[] x;",
            null,
            Arrays.asList("int", "[", "]", "x", ";"),
          },
          {
            "multidimensional array with partially specified size (1)",
            "int[2][] x;",
            null,
            Arrays.asList("int", "[", "2", "]", "[", "]", "x", ";"),
          },
          {
            "multidimensional array with partially specified size (2)",
            "int[][2] x;",
            null,
            Arrays.asList("int", "[", "]", "[", "2", "]", "x", ";"),
          },
          {
            "multidimensional array with explicit size",
            "int[2, 2] x;",
            null,
            Arrays.asList("int", "[", "2", ",", "2", "]", "x", ";"),
          },
          // TODO: `typedef int[] intArray` and aggregate shouldn't be valid keys.
          {
            "map with non-primitive key type",
            "record a{int b;}; int[a] x;",
            "Index type 'a' is not a primitive type",
            null,
          },
          {
            "multidimensional map with partially specified comma-separated type",
            "int[2,] x;",
            "Missing index token",
            null,
          },
          {
            "multidimensional map with unspecified comma-separated type",
            "int[,,,] x;",
            "Missing index token",
            null,
          },
          {
            "multidimensional map with partially-specified comma-separated type",
            "int[int,] x;",
            "Missing index token",
            null,
          },
          {
            "abruptly terminated 1-d map", "int[int", "Expected , or ], found end of file", null,
          },
          {
            "abruptly terminated 1-d array", "int[4", "Expected , or ], found end of file", null,
          },
          {
            "map with unknown type", "int[a] x;", "Invalid type name 'a'", null,
          },
          {
            "map containing aggregate type", "int[int[]] x;", "Expected , or ], found [", null,
          },
          {
            "multidimensional map with comma-separated type",
            "int[int, int] x;",
            null,
            Arrays.asList("int", "[", "int", ",", "int", "]", "x", ";"),
          },
          {
            "multidimensional map with chained brackets with types",
            "int[int][int] x;",
            null,
            Arrays.asList("int", "[", "int", "]", "[", "int", "]", "x", ";"),
          },
          {
            "multidimensional map with unspecified type in chained empty brackets",
            "int[][] x;",
            null,
            Arrays.asList("int", "[", "]", "[", "]", "x", ";"),
          },
          {
            "single static declaration",
            "static int x;",
            null,
            Arrays.asList("static", "int", "x", ";"),
          },
          {
            "single static definition",
            "static int x = 1;",
            null,
            Arrays.asList("static", "int", "x", "=", "1", ";"),
          },
          {
            "multiple static definition",
            "static int x = 1, y = 2;",
            null,
            Arrays.asList("static", "int", "x", "=", "1", ",", "y", "=", "2", ";"),
          },
          {
            "static type but no declaration",
            "static int;",
            "Type given but not used to declare anything",
            null,
          },
          {
            "static command",
            "static print('hello world');",
            null,
            Arrays.asList("static", "print", "(", "'hello world'", ")", ";"),
          },
          {
            "simple static block",
            "static { int x; }",
            null,
            Arrays.asList("static", "{", "int", "x", ";", "}"),
          },
          {
            "unterminated static declaration",
            "static int x\nint y;",
            "Expected ;, found int",
            null,
          },
          {
            "unterminated static block", "static {", "Expected }, found end of file", null,
          },
          {
            "lone static", "static;", "command or declaration required", null,
          },
          {
            "unterminated typedef", "typedef int foo\n foo bar;", "Expected ;, found foo", null,
          },
          {
            "typedef with no type", "typedef;", "Missing data type for typedef", null,
          },
          {
            "typedef with no name", "typedef int;", "Type name expected", null,
          },
          {
            "typedef with non-identifier name", "typedef int 1;", "Invalid type name '1'", null,
          },
          {
            "typedef with reserved name",
            "typedef int remove;",
            "Reserved word 'remove' cannot be a type name",
            null,
          },
          {
            "typedef with existing name",
            "typedef float double; typedef int double;",
            "Type name 'double' is already defined",
            null,
          },
          {
            "equivalent typedef redefinition",
            "typedef float double; typedef float double;",
            null,
            Arrays.asList("typedef", "float", "double", ";", "typedef", "float", "double", ";"),
          },
          {
            "inner main",
            "void foo() { void main() {} }",
            "main method must appear at top level",
            null,
          },
          {
            "unterminated top-level declaration", "int x\nint y", "Expected ;, found int", null,
          },
          {
            "type but no declaration", "int;", "Type given but not used to declare anything", null,
          },
          {
            "aggregate-initialized definition",
            "int[1] x { 1 };",
            null,
            Arrays.asList("int", "[", "1", "]", "x", "{", "1", "}", ";"),
          },
          {
            "record with no body", "record a;", "Expected {, found ;", null,
          },
          {
            "record with no name or body", "record;", "Record name expected", null,
          },
          {
            "record with non-identifier name",
            "record 1 { int a;};",
            "Invalid record name '1'",
            null,
          },
          {
            "record with reserved name",
            "record int { int a;};",
            "Reserved word 'int' cannot be a record name",
            null,
          },
          {
            "record redefinition",
            "record a { int a;}; record a { int a;};",
            "Record name 'a' is already defined",
            null,
          },
          {
            "record without fields", "record a {};", "Record field(s) expected", null,
          },
          {
            "record with void field type",
            "record a { void a;};",
            "Non-void field type expected",
            null,
          },
          {
            "record with unknown field type", "record a { foo a;};", "Type name expected", null,
          },
          {
            "record with no field name", "record a { int;};", "Field name expected", null,
          },
          {
            "record with non-identifier field name",
            "record a { int 1;};",
            "Invalid field name '1'",
            null,
          },
          {
            "record with reserved field name",
            "record a { int class;};",
            "Reserved word 'class' cannot be used as a field name",
            null,
          },
          {
            "record with repeated field name",
            "record a { int b; int b;};",
            "Field name 'b' is already defined",
            null,
          },
          {
            "record with missing semicolon",
            "record a { int b\n float c;};",
            "Expected ;, found float",
            null,
          },
          {
            "unterminated record", "record a { int b;", "Expected }, found end of file", null,
          },
          {
            "declaration with non-identifier name",
            "int 1;",
            "Type given but not used to declare anything",
            null,
          },
          {
            "declaration with reserved name",
            "int class;",
            "Reserved word 'class' cannot be a variable name",
            null,
          },
          {
            "multiple-definition", "int x = 1; int x = 2;", "Variable x is already defined", null,
          },
          {
            "variable declaration followed by definition",
            // One might expect this to be acceptable.
            "int x; int x = 1;",
            "Variable x is already defined",
            null,
          },
          {
            "parameter initialization",
            "int f(int x = 1) {}",
            "Cannot initialize parameter x",
            null,
          },
          {
            "brace assignment of primitive",
            "int x = {1}",
            "Cannot initialize x of type int with an aggregate literal",
            null,
          },
          {
            "brace assignment of array",
            "int[] x = {1, 2, 3};",
            null,
            Arrays.asList("int", "[", "]", "x", "=", "{", "1", ",", "2", ",", "3", "}", ";"),
          },
          {
            "invalid coercion", "int x = 'hello';", "Cannot store string in x of type int", null,
          },
          {
            "float->int assignment",
            "int x = 1.0;",
            null,
            Arrays.asList("int", "x", "=", "1", ".", "0", ";"),
          },
          {
            "int->float assignment",
            "float y = 1;",
            null,
            Arrays.asList("float", "y", "=", "1", ";"),
          },
          {
            "assignment missing rhs", "int x =", "Expression expected", null,
          },
          {
            "assignment missing rhs 2", "int x; x =", "Expression expected", null,
          },
          {
            "Invalid assignment coercion - logical",
            "int x; x &= true",
            "&= requires an integer or boolean expression and an integer or boolean variable reference",
            null,
          },
          {
            "Invalid assignment coercion - integer",
            "boolean x; x >>= 1",
            ">>= requires an integer expression and an integer variable reference",
            null,
          },
          {
            "Invalid assignment coercion - assignment",
            "boolean x; x += 'foo'",
            "Cannot store string in x of type boolean",
            null,
          },
          {
            "break outside loop", "break;", "Encountered 'break' outside of loop", null,
          },
          {
            "continue outside loop", "continue;", "Encountered 'continue' outside of loop", null,
          },
          {
            // Catch in ASH is comparable to a function that catches and
            // returns the text of any errors thrown by its block.
            "catch without try", "catch {}", null, Arrays.asList("catch", "{", "}"),
          },
          {
            "finally without try",
            "finally {}",
            // "Encountered 'finally' without try",
            "Unknown variable 'finally'",
            null,
          },
          {
            "try without catch or finally",
            "try {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless",
            null,
          },
          {
            "try-catch",
            "try {} catch {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless",
            null,
          },
          {
            "try-finally",
            "try {} finally {}",
            null,
            Arrays.asList("try", "{", "}", "finally", "{", "}"),
          },
          {
            "catch value block",
            "string error_message = catch {}",
            null,
            Arrays.asList("string", "error_message", "=", "catch", "{", "}"),
          },
          {
            "catch value expression",
            "string error_message = catch ( print(__FILE__) )",
            null,
            Arrays.asList(
                "string", "error_message", "=", "catch", "(", "print", "(", "__FILE__", ")", ")"),
          },
          {
            "catch value interrupted",
            "string error_message = catch",
            "\"catch\" requires a block or an expression",
            null,
          },
          {
            // This has been permitted historically...
            "return outside function",
            "return;",
            // "Return needs null value",
            "Cannot return when outside of a function",
            null,
            // Arrays.asList( "return", ";" ),
          },
          {
            "top-level exit", "exit;", null, Arrays.asList("exit", ";"),
          },
          {
            "empty block", "{}", null, Arrays.asList("{", "}"),
          },
          {
            "exit with parameter", "exit 1;", "Expected ;, found 1", null,
          },
          {
            "break inside while-loop",
            "while (true) break;",
            null,
            Arrays.asList("while", "(", "true", ")", "break", ";"),
          },
          {
            "continue inside while-loop",
            "while (true) continue;",
            null,
            Arrays.asList("while", "(", "true", ")", "continue", ";"),
          },
          {
            "break inside switch",
            "switch (true) { default: break; }",
            null,
            Arrays.asList("switch", "(", "true", ")", "{", "default", ":", "break", ";", "}"),
          },
          {
            "continue inside switch",
            "switch (true) { default: continue; }",
            "Encountered 'continue' outside of loop",
            null,
          },
          {
            "empty foreach",
            "foreach cls in $classes[];",
            null,
            Arrays.asList("foreach", "cls", "in", "$", "classes", "[", "]", ";"),
          },
          {
            "for-from-to",
            "for i from 1 to 10000;",
            null,
            Arrays.asList("for", "i", "from", "1", "to", "10000", ";"),
          },
          {
            "for-from-upto",
            "for i from 1 upto 10000;",
            null,
            Arrays.asList("for", "i", "from", "1", "upto", "10000", ";"),
          },
          {
            "for-from-to-by",
            "for i from 1 to 10000 by 100;",
            null,
            Arrays.asList("for", "i", "from", "1", "to", "10000", "by", "100", ";"),
          },
          {
            "for-from-downto",
            // This is valid, but will immediately return.
            "for i from 1 downto 10000;",
            null,
            Arrays.asList("for", "i", "from", "1", "downto", "10000", ";"),
          },
          {
            "no return from int function", "int f() {}", "Missing return value", null,
          },
          {
            "return void from int function", "int f() { return; }", "Return needs int value", null,
          },
          {
            "return string from int function",
            "int f() { return 'str'; }",
            "Cannot return string value from int function",
            null,
          },
          {
            "return int from void function",
            "void f() { return 1; }",
            "Cannot return a value from a void function",
            null,
          },
          {
            "non-expression return", "int f() { return (); }", "Expression expected", null,
          },
          {
            "single-command if",
            "if (true) print('msg');",
            null,
            Arrays.asList("if", "(", "true", ")", "print", "(", "'msg'", ")", ";"),
          },
          {
            "empty if", "if (true);", null, Arrays.asList("if", "(", "true", ")", ";"),
          },
          {
            "unclosed block scope", "{", "Expected }, found end of file", null,
          },
          // if / else-if / else
          {
            "if without condition", "if true", "Expected (, found true", null,
          },
          {
            "if with empty condition", "if ()", "\"if\" requires a boolean condition", null,
          },
          {
            "if with numeric condition", "if (1)", "\"if\" requires a boolean condition", null,
          },
          {
            "if with unclosed condition", "if (true", "Expected ), found end of file", null,
          },
          // These probably shouldn't need to be separate test cases...
          {
            "else if without condition", "if (false); else if true", "Expected (, found true", null,
          },
          {
            "else if with empty condition",
            "if (false); else if ()",
            "\"if\" requires a boolean condition",
            null,
          },
          {
            "else if with unclosed condition",
            "if (false); else if (true",
            "Expected ), found end of file",
            null,
          },
          // while
          {
            "while without condition", "while true", "Expected (, found true", null,
          },
          {
            "while with empty condition",
            "while ()",
            "\"while\" requires a boolean condition",
            null,
          },
          {
            "while with unclosed condition", "while (true", "Expected ), found end of file", null,
          },
          {
            "while with unclosed loop", "while (true) {", "Expected }, found end of file", null,
          },
          {
            "while with multiple statements but no semicolon",
            "while (true) print(5)\nprint(6)",
            "Expected ;, found print",
            null,
          },
          // repeat
          {
            "repeat statement",
            "repeat print('hello'); until(true);",
            null,
            Arrays.asList(
                "repeat", "print", "(", "'hello'", ")", ";", "until", "(", "true", ")", ";"),
          },
          {
            "repeat without until", "repeat {}", "Expected until, found end of file", null,
          },
          {
            "repeat without condition", "repeat {} until true", "Expected (, found true", null,
          },
          {
            "repeat with empty condition",
            "repeat {} until ('done')",
            // This should probably read as "'until' requires a
            // boolean condition"...
            "\"repeat\" requires a boolean condition",
            null,
          },
          // So many cases of identical tests for duplicate code...
          {
            "repeat with unclosed condition",
            "repeat {} until (true",
            "Expected ), found end of file",
            null,
          },
          // switch
          {
            "switch without condition or block",
            "switch true {}",
            "Expected ( or {, found true",
            null,
          },
          {
            "switch with empty condition",
            "switch ()",
            "\"switch ()\" requires an expression",
            null,
          },
          {
            "switch with unclosed condition", "switch (true", "Expected ), found end of file", null,
          },
          {
            "switch with condition but no block",
            "switch (true)",
            "Expected {, found end of file",
            null,
          },
          {
            "switch with condition but unclosed block",
            "switch (true) {",
            "Expected }, found end of file",
            null,
          },
          {
            "switch with block and no condition",
            "switch { }",
            null,
            Arrays.asList("switch", "{", "}"),
          },
          {
            "switch with block, non-const label",
            "boolean x; switch { case x: }",
            null,
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "x", ":", "}"),
          },
          {
            "switch with block, label expression",
            "boolean x; switch { case !x: }",
            null,
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "!", "x", ":", "}"),
          },
          {
            "switch with block, nested variable",
            "switch { case true: int x; }",
            null,
            Arrays.asList("switch", "{", "case", "true", ":", "int", "x", ";", "}"),
          },
          {
            "switch with block, nested type but no variable",
            "switch { case true: int; }",
            "Type given but not used to declare anything",
            null,
          },
          {
            "switch with block, nested variable but missing semicolon",
            "switch { case true: int x }",
            "Expected ;, found }",
            null,
          },
          {
            "switch, case label without expression",
            "switch { case: }",
            "Case label needs to be followed by an expression",
            null,
          },
          {
            "switch case not terminated by colon",
            "switch { case true; }",
            "Expected :, found ;",
            null,
          },
          {
            "switch default not terminated by colon",
            "switch { default true; }",
            "Expected :, found true",
            null,
          },
          {
            "switch type mismatch",
            "switch (1) { case true: }",
            "Switch conditional has type int but label expression has type boolean",
            null,
          },
          {
            "switch block type mismatch",
            // Note that the implicit switch type is boolean here.
            "switch { case 1: }",
            "Switch conditional has type boolean but label expression has type int",
            null,
          },
          {
            "duplicate switch label",
            "switch (1) { case 0: case 0: }",
            "Duplicate case label: 0",
            null,
          },
          {
            "switch, multiple default labels",
            "switch (1) { default: default: }",
            "Only one default label allowed in a switch statement",
            null,
          },
          {
            "switch block, multiple default labels",
            "switch { default: default: }",
            "Only one default label allowed in a switch statement",
            null,
          },
          {
            "variable definition of sort",
            "int sort = 0;",
            null,
            Arrays.asList("int", "sort", "=", "0", ";"),
          },
          {
            "variable declaration of sort", "int sort;", null, Arrays.asList("int", "sort", ";"),
          },
          {
            "function named sort",
            "void sort(){} sort();",
            null,
            Arrays.asList("void", "sort", "(", ")", "{", "}", "sort", "(", ")", ";"),
          },
          {
            "sort not-a-variable primitive",
            "sort 2 by value;",
            "Aggregate reference expected",
            null,
          },
          {
            "sort without by", "int[] x {3,2,1}; sort x;", "Expected by, found ;", null,
          },
          {
            "Sort, no sorting expression", "int[] x; sort x by", "Expression expected", null,
          },
          {
            "valid sort",
            "int[] x; sort x by value*3;",
            null,
            Arrays.asList("int", "[", "]", "x", ";", "sort", "x", "by", "value", "*", "3", ";"),
          },
          {
            "foreach with non-identifier key",
            "foreach 'key' in $items[];",
            "Key variable name expected",
            null,
          },
          {
            "foreach with reserved key",
            "foreach item in $items[];",
            "Reserved word 'item' cannot be a key variable",
            null,
          },
          {
            "foreach missing `in`", "foreach it;", "Expected in, found ;", null,
          },
          {
            "foreach missing key variable name",
            "foreach in it;",
            "Key variable name expected",
            null,
          },
          {
            "foreach key variable named 'in'",
            "foreach in in it;",
            "Reserved word 'in' cannot be a key variable name",
            null,
          },
          {
            "foreach key variable named 'in' 2",
            "foreach in, on, under, below, through in it;",
            "Reserved word 'in' cannot be a key variable name",
            null,
          },
          {
            "foreach in not-a-reference",
            "foreach it in $item[none];",
            "Aggregate reference expected",
            null,
          },
          {
            "foreach with duplicate key",
            "foreach it, it in $items[];",
            "Key variable 'it' is already defined",
            null,
          },
          {
            "foreach with multiple keys",
            "foreach key, value in int[int]{} {}",
            null,
            Arrays.asList(
                "foreach", "key", ",", "value", "in", "int", "[", "int", "]", "{", "}", "{", "}"),
          },
          {
            "foreach with too many keys",
            "foreach a, b, c in $items[];",
            "Too many key variables specified",
            null,
          },
          {
            "for with reserved index",
            "for int from 1 upto 10;",
            "Reserved word 'int' cannot be an index variable",
            null,
          },
          {
            "for with existing index",
            // Oddly, this is unsupported, when other for loops will create
            // a nested scope.
            "int i; for i from 1 upto 10;",
            "Index variable 'i' is already defined",
            null,
          },
          {
            "for without from", "for i in range(10):\n  print(i)", "Expected from, found in", null,
          },
          {
            "for with invalid dest keyword",
            "for i from 1 until 10;",
            "Expected to, upto, or downto, found until",
            null,
          },
          {
            "javaFor with multiple declarations",
            "for (int i=0, int length=5; i < length; i++);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ",", "int", "length", "=", "5", ";", "i", "<",
                "length", ";", "i", "++", ")", ";"),
          },
          {
            "javaFor with empty initializer",
            "for (int i=0,; i < 5; ++i);",
            "Identifier expected",
            null,
          },
          {
            "javaFor with compound increment",
            "for (int i=0; i < 5; i+=1);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "+=", "1", ")",
                ";"),
          },
          {
            "javaFor with existing variable",
            "int i; for (i=0; i < 5; i++);",
            null,
            Arrays.asList(
                "int", "i", ";", "for", "(", "i", "=", "0", ";", "i", "<", "5", ";", "i", "++", ")",
                ";"),
          },
          {
            "javaFor with unknown existing variable",
            "for (i=0; i < 5; i++);",
            "Unknown variable 'i'",
            null,
          },
          {
            "javaFor with redefined existing variable",
            "int i; for (int i=0; i < 5; i++);",
            "Variable 'i' already defined",
            null,
          },
          {
            "javaFor with not-an-increment",
            "for (int i=0; i < 5; i==1);",
            "Variable 'i' not incremented",
            null,
          },
          {
            "javaFor with constant assignment",
            // I guess this technically works... but will be an infinite
            // loop in practice.
            "for (int i=0; i < 5; i=1);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "=", "1", ")", ";"),
          },
          {
            "javaFor missing initial identifier",
            "for (0; i < 5; i++);",
            "Identifier required",
            null,
          },
          {
            "javaFor missing initializer expression",
            "for (int i =; i < 5; i++);",
            "Expression expected",
            null,
          },
          {
            "javaFor invalid assignment",
            "for (int i ='abc'; i < 5; i++);",
            "Cannot store string in i of type int",
            null,
          },
          {
            "javaFor non-boolean condition",
            "for (int i; i + 5; i++);",
            "\"for\" requires a boolean conditional expression",
            null,
          },
          {
            "javaFor multiple increments",
            "for (int i, int j; i + j < 5; i++, j++);",
            null,
            Arrays.asList(
                "for", "(", "int", "i", ",", "int", "j", ";", "i", "+", "j", "<", "5", ";", "i",
                "++", ",", "j", "++", ")", ";"),
          },
          {
            "javaFor interrupted multiple increments",
            "for (int i; i < 5; i++,);",
            "Identifier expected",
            null,
          },
          {
            "undefined function call",
            "prin();",
            "Function 'prin( )' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts.",
            null,
          },
          {
            "function call interrupted", "print(", "Expected ), found end of file", null,
          },
          {
            "function call interrupted after comma",
            "print(1,",
            "Expected parameter, found end of file",
            null,
          },
          {
            "function call closed after comma", "print(1,);", "Expected parameter, found )", null,
          },
          {
            "function call interrupted after param",
            "print(1",
            "Expected ), found end of file",
            null,
          },
          {
            "function call with non-comma separator", "print(1; 2);", "Expected ), found ;", null,
          },
          {
            "function parameter coercion to ANY_TYPE",
            "dump('foo', 'bar');",
            null,
            Arrays.asList("dump", "(", "'foo'", ",", "'bar'", ")", ";"),
          },
          {
            "function parameter no typedef coercion",
            "typedef int foo; foo a = 1; void bar(int x, foo y) {} bar(a, 1);",
            null,
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "void", "bar", "(", "int",
                "x", ",", "foo", "y", ")", "{", "}", "bar", "(", "a", ",", "1", ")", ";"),
          },
          {
            "function parameter typedef-to-simple typedef coercion",
            // Mmh... there's no real way to "prove" the function was used other than
            // seeing it checked from clover...
            "typedef int foo; foo a = 1; int to_int(foo x) {return 1;} void bar(int x) {} bar(a);",
            null,
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "int", "to_int", "(",
                "foo", "x", ")", "{", "return", "1", ";", "}", "void", "bar", "(", "int", "x", ")",
                "{", "}", "bar", "(", "a", ")", ";"),
          },
          {
            "function parameter simple-to-typedef typedef coercion",
            "typedef int foo; foo a = 1; foo to_foo(int x) {return a;} void bar(foo x) {} bar(1);",
            null,
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "foo", "to_foo", "(",
                "int", "x", ")", "{", "return", "a", ";", "}", "void", "bar", "(", "foo", "x", ")",
                "{", "}", "bar", "(", "1", ")", ";"),
          },
          {
            "function invocation interrupted",
            "call",
            "Variable reference expected for function name",
            null,
          },
          {
            "function invocation non-string expression",
            "call (2)()",
            "String expression expected for function name",
            null,
          },
          {
            "function invocation interrupted after name expression",
            "call ('foo')",
            "Expected (, found end of file",
            null,
          },
          {
            "function invocation with non-void function",
            // ummm this should insist that the variable is a string...
            "int x; call string x('foo')",
            null,
            Arrays.asList("int", "x", ";", "call", "string", "x", "(", "'foo'", ")"),
          },
          {
            "preincrement with non-numeric variable",
            "string x; ++x;",
            "++X requires a numeric variable reference",
            null,
          },
          {
            "preincrement requires a variable", "++1;", "Variable reference expected", null,
          },
          {
            "predecrement with float variable",
            "float x; --x;",
            null,
            Arrays.asList("float", "x", ";", "--", "x", ";"),
          },
          {
            "postincrement with non-numeric variable",
            "string x; x++;",
            "X++ requires a numeric variable reference",
            null,
          },
          /* Currently fails with "Expected ;, found ++" which is asymmetric.
          {
          	"postincrement requires a variable",
          	"1++;",
          	"Variable reference expected",
          	null,
          },
          */
          {
            "postdecrement with float variable",
            "float x; x--;",
            null,
            Arrays.asList("float", "x", ";", "x", "--", ";"),
          },
          {
            "ternary with non-boolean condition",
            "int x = 1 ? 1 : 2;",
            "Non-boolean expression 1 (int)",
            null,
          },
          {
            "ternary without lhs", "int x = true ? : 2;", "Value expected in left hand side", null,
          },
          {
            "ternary without colon", "int x = true ? 1;", "Expected :, found ;", null,
          },
          {
            "ternary without rhs",
            "int x = true ? 1:;",
            // Another asymmetry: not "Value expected in right hand side"
            "Value expected",
            null,
          },
          {
            "non-coercible value mismatch",
            "(true ? 1 : $item[none];",
            "Cannot choose between 1 (int) and none (item)",
            null,
          },
          // parseValue
          {
            "unclosed parenthetical expression", "(true", "Expected ), found end of file", null,
          },
          {
            "aggregate literal without braces", "(int[])", "Expected {, found )", null,
          },
          {
            "indexed variable reference",
            "int[5] x; x[0];",
            null,
            Arrays.asList("int", "[", "5", "]", "x", ";", "x", "[", "0", "]", ";"),
          },
          {
            "indexed primitive", "int x; x[0];", "Variable 'x' cannot be indexed", null,
          },
          {
            "over-indexed variable reference", "int[5] x; x[0,1];", "Too many keys for 'x'", null,
          },
          {
            "empty indexed variable reference", "int[5] x; x[];", "Index for 'x' expected", null,
          },
          {
            "unterminated aggregate variable reference",
            "int[5] x; x[0",
            "Expected ], found end of file",
            null,
          },
          {
            "type-mismatched indexed variable reference",
            "int[5] x; x['str'];",
            "Index for 'x' has wrong data type (expected int, got string)",
            null,
          },
          {
            "type-mismatched indexed composite reference",
            "int[5, 5] x; x[0]['str'];",
            "Index for 'x[]' has wrong data type (expected int, got string)",
            null,
          },
          {
            "multidimensional comma-separated array index",
            "int[5,5] x; x[0,1];",
            null,
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", ",", "1", "]", ";"),
          },
          {
            "multidimensional bracket-separated array index",
            "int[5,5] x; x[0][1];",
            null,
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", "]", "[", "1", "]", ";"),
          },
          {
            "method call of primitive var",
            "string x = 'hello'; x.print();",
            null,
            Arrays.asList("string", "x", "=", "'hello'", ";", "x", ".", "print", "(", ")", ";"),
          },
          {
            "method call of aggregate index",
            "string[2] x; x[0].print();",
            null,
            Arrays.asList(
                "string", "[", "2", "]", "x", ";", "x", "[", "0", "]", ".", "print", "(", ")", ";"),
          },
          {
            "non-record property reference", "int i; i.a;", "Record expected", null,
          },
          {
            "record field reference",
            "record {int a;} r; r.a;",
            null,
            Arrays.asList("record", "{", "int", "a", ";", "}", "r", ";", "r", ".", "a", ";"),
          },
          {
            "record field reference without field",
            "record {int a;} r; r.",
            "Field name expected",
            null,
          },
          {
            "record unknown field reference",
            "record {int a;} r; r.b;",
            "Invalid field name 'b'",
            null,
          },
          {
            "Illegal record creation",
            "void f( record foo {int a; int b;} bar )",
            "Existing type expected for function parameter",
            null,
          },
          {
            "array of record",
            "record {int a;}[] r;",
            null,
            Arrays.asList("record", "{", "int", "a", ";", "}", "[", "]", "r", ";"),
          },
          {
            "standalone new", "new;", "Expected Record name, found ;", null,
          },
          {
            "new non-record", "int x = new int();", "'int' is not a record type", null,
          },
          {
            "new record without parens",
            // Yields a default-constructed record.
            "record r {int a;}; new r;",
            null,
            Arrays.asList("record", "r", "{", "int", "a", ";", "}", ";", "new", "r", ";"),
          },
          {
            "new record with semicolon",
            "record r {int a;}; new r(;",
            "Expression expected for field #1 (a)",
            null,
          },
          {
            "new with aggregate field",
            "record r {int[] a;}; new r({1,2});",
            null,
            Arrays.asList(
                "record", "r", "{", "int", "[", "]", "a", ";", "}", ";", "new", "r", "(", "{", "1",
                ",", "2", "}", ")", ";"),
          },
          {
            "new with field type mismatch",
            "record r {int a;}; new r('str');",
            "string found when int expected for field #1 (a)",
            null,
          },
          {
            "new with too many void fields",
            "record r {int a;}; new r(,,,,,,,);",
            "Too many field initializers for record r",
            null,
          },
          {
            "new without closing paren",
            "record r {int a;}; new r(",
            "Expected ), found end of file",
            null,
          },
          {
            "new without closing paren 2",
            "record r {int a;}; new r(4",
            "Expected , or ), found end of file",
            null,
          },
          {
            "new without closing comma separator",
            "record r {int a; int b;}; new r(4 5)",
            "Expected , or ), found 5",
            null,
          },
          {
            "improper remove", "int i; remove i;", "Aggregate reference expected", null,
          },
          {
            "proper remove",
            "int[] map; remove map[0];",
            null,
            Arrays.asList("int", "[", "]", "map", ";", "remove", "map", "[", "0", "]", ";"),
          },
        });
  }

  public ParserTest(String desc, String script, String errorMessage, List<String> resultTokens) {
    this.desc = desc;
    this.script = script;

    this.tokens = resultTokens;

    this.errorText = errorMessage;
  }

  @Test
  public void testScriptValidity() {
    ByteArrayInputStream istream =
        new ByteArrayInputStream(this.script.getBytes(StandardCharsets.UTF_8));
    Parser p = new Parser(/*scriptFile=*/ null, /*stream=*/ istream, /*imports=*/ null);

    if (this.errorText != null) {
      ScriptException e = assertThrows(this.desc, ScriptException.class, p::parse);
      assertThat(this.desc, e.getMessage(), containsString(this.errorText));
      return;
    }

    // This will fail if an exception is thrown.
    p.parse();
    assertEquals(this.desc, this.tokens, p.getTokensContent());
  }
}
