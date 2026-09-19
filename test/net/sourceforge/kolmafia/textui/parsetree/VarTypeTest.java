package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class VarTypeTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        // Valid var declarations: type inferred from initializer
        valid(
            "var int literal",
            "var x = 1;",
            Arrays.asList("var", "x", "=", "1", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.INT, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        valid(
            "var float literal",
            "var x = 1.0;",
            Arrays.asList("var", "x", "=", "1", ".", "0", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10", "1-11", "1-12"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.FLOAT, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        valid(
            "var string literal",
            "var x = \"s\";",
            Arrays.asList("var", "x", "=", "\"s\"", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-12"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.STRING, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        valid(
            "var boolean literal",
            "var x = true;",
            Arrays.asList("var", "x", "=", "true", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-13"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.BOOLEAN, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        valid(
            "var item literal",
            "var x = $item[briefcase];",
            Arrays.asList("var", "x", "=", "$", "item", "[", "briefcase", "]", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10", "1-14", "1-15", "1-24", "1-25"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.ITEM, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        valid(
            "var comma declaration",
            "var a = 1, b = 2;",
            Arrays.asList("var", "a", "=", "1", ",", "b", "=", "2", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10", "1-12", "1-14", "1-16", "1-17"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.INT, vars.next().getType().getType());
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.INT, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        valid(
            "static var",
            "static var x = 1;",
            Arrays.asList("static", "var", "x", "=", "1", ";"),
            Arrays.asList("1-1", "1-8", "1-12", "1-14", "1-16", "1-17"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.INT, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        // Error: var without initializer
        invalid("var no initializer", "var x;", "var requires an initializer", "char 5 to char 6"),
        // Error: var with composite literal
        invalid(
            "var with empty composite literal",
            "var x = {};",
            "Inference from composite literal not supported; declare full type",
            "char 9 to char 11"),
        invalid(
            "var with array literal",
            "var x = {\"string\"};",
            "Inference from composite literal not supported; declare full type",
            "char 9 to char 19"),
        // Error: var self-reference
        invalid(
            "var self-reference",
            "var x = x;",
            "Cannot infer type: variable references itself",
            "char 9 to char 10"),
        // Valid: var with void initializer (matches "void x = wait(1)")
        valid(
            "var with void initializer",
            "void g() {} var x = g();",
            Arrays.asList("void", "g", "(", ")", "{", "}", "var", "x", "=", "g", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-7", "1-8", "1-10", "1-11", "1-13", "1-17", "1-19", "1-21", "1-22",
                "1-23", "1-24"),
            scope -> {
              Iterator<Variable> vars = scope.getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.VOID, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        // Error: var in function parameter
        invalid(
            "var as function parameter type",
            "void f(var x) {}",
            "var is only valid in variable declarations",
            "char 8 to char 11"),
        // Error: var as function return type
        invalid(
            "var as function return type",
            "var f() {}",
            "var is only valid in variable declarations",
            "char 1 to char 4"),
        // Error: var as record field type
        invalid(
            "var as record field type",
            "record {var x;};",
            "var is only valid in variable declarations",
            "char 9 to char 12"),
        // Error: var as typedef base
        invalid(
            "var as typedef base",
            "typedef var Foo;",
            "var is only valid in variable declarations",
            "char 9 to char 12"),
        // Valid: var in java-style for loop initializer
        valid(
            "var in java for loop",
            "for (var i = 0; i < 10; i++) {}",
            Arrays.asList(
                "for", "(", "var", "i", "=", "0", ";", "i", "<", "10", ";", "i", "++", ")", "{",
                "}"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-12", "1-14", "1-15", "1-17", "1-19", "1-21", "1-23",
                "1-25", "1-26", "1-28", "1-30", "1-31"),
            scope -> {
              Iterator<Command> commands = scope.getCommands();
              assertTrue(commands.hasNext());
              JavaForLoop loop = (JavaForLoop) commands.next();
              Iterator<Variable> vars = loop.getScope().getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.INT, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        // Error: var without initializer in java-style for loop
        invalid(
            "var no initializer in java for loop",
            "for (var i; i < 10; i++) {}",
            "var requires an initializer",
            "char 10 to char 11"),
        // Error: var self-reference in java-style for loop
        invalid(
            "var self-reference in java for loop",
            "for (var i = i; i < 10; i++) {}",
            "Cannot infer type: variable references itself",
            "char 14 to char 15"),
        // Error: var with empty composite literal in java-style for loop
        invalid(
            "var with empty composite literal in java for",
            "for (var x = {}; false; ) {}",
            "Inference from composite literal not supported; declare full type",
            "char 14 to char 16"),
        // Error: var with non-empty composite literal in java-style for loop
        invalid(
            "var with non-empty composite literal in java for",
            "for (var x = {1, 2}; false; ) {}",
            "Inference from composite literal not supported; declare full type",
            "char 14 to char 20"),
        // Valid: var in java-style for loop with explicit aggregate type expression
        valid(
            "var aggregate in java for loop",
            "for (var x = int[3] {1, 2, 3}; false; ) {}",
            Arrays.asList(
                "for", "(", "var", "x", "=", "int", "[", "3", "]", "{", "1", ",", "2", ",", "3",
                "}", ";", "false", ";", ")", "{", "}"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-12", "1-14", "1-17", "1-18", "1-19", "1-21", "1-22",
                "1-23", "1-25", "1-26", "1-28", "1-29", "1-30", "1-32", "1-37", "1-39", "1-41",
                "1-42"),
            scope -> {
              Iterator<Command> commands = scope.getCommands();
              assertTrue(commands.hasNext());
              JavaForLoop loop = (JavaForLoop) commands.next();
              Iterator<Variable> vars = loop.getScope().getVariables().iterator();
              assertTrue(vars.hasNext());
              assertEquals(TypeSpec.AGGREGATE, vars.next().getType().getType());
              assertFalse(vars.hasNext());
            }),
        // Error: var as foreach key
        invalid(
            "var as foreach key",
            "foreach var k in m {}",
            "Reserved word 'var' cannot be a key variable name",
            "char 9 to char 12"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
