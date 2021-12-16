package net.sourceforge.kolmafia.textui;

import static net.sourceforge.kolmafia.textui.parsetree.AggregateType.badAggregateType;
import static net.sourceforge.kolmafia.textui.parsetree.VariableReference.badVariableReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.Line.Token;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayLiteral;
import net.sourceforge.kolmafia.textui.parsetree.Assignment;
import net.sourceforge.kolmafia.textui.parsetree.BasicScope;
import net.sourceforge.kolmafia.textui.parsetree.BasicScript;
import net.sourceforge.kolmafia.textui.parsetree.Catch;
import net.sourceforge.kolmafia.textui.parsetree.Command;
import net.sourceforge.kolmafia.textui.parsetree.CompositeReference;
import net.sourceforge.kolmafia.textui.parsetree.Concatenate;
import net.sourceforge.kolmafia.textui.parsetree.Conditional;
import net.sourceforge.kolmafia.textui.parsetree.Else;
import net.sourceforge.kolmafia.textui.parsetree.ElseIf;
import net.sourceforge.kolmafia.textui.parsetree.Evaluable;
import net.sourceforge.kolmafia.textui.parsetree.ForEachLoop;
import net.sourceforge.kolmafia.textui.parsetree.ForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.Function.BadFunction;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import net.sourceforge.kolmafia.textui.parsetree.FunctionCall;
import net.sourceforge.kolmafia.textui.parsetree.FunctionInvocation;
import net.sourceforge.kolmafia.textui.parsetree.FunctionReturn;
import net.sourceforge.kolmafia.textui.parsetree.If;
import net.sourceforge.kolmafia.textui.parsetree.IncDec;
import net.sourceforge.kolmafia.textui.parsetree.JavaForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Loop;
import net.sourceforge.kolmafia.textui.parsetree.LoopBreak;
import net.sourceforge.kolmafia.textui.parsetree.LoopContinue;
import net.sourceforge.kolmafia.textui.parsetree.MapLiteral;
import net.sourceforge.kolmafia.textui.parsetree.Operation;
import net.sourceforge.kolmafia.textui.parsetree.Operator;
import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordType.BadRecordType;
import net.sourceforge.kolmafia.textui.parsetree.RepeatUntilLoop;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.ScriptExit;
import net.sourceforge.kolmafia.textui.parsetree.ScriptState.BadScriptState;
import net.sourceforge.kolmafia.textui.parsetree.SortBy;
import net.sourceforge.kolmafia.textui.parsetree.StaticScope;
import net.sourceforge.kolmafia.textui.parsetree.Switch;
import net.sourceforge.kolmafia.textui.parsetree.SwitchScope;
import net.sourceforge.kolmafia.textui.parsetree.TernaryExpression;
import net.sourceforge.kolmafia.textui.parsetree.Try;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Type.BadType;
import net.sourceforge.kolmafia.textui.parsetree.TypeDef;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.Value.Constant;
import net.sourceforge.kolmafia.textui.parsetree.VarArgType;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.Variable.BadVariable;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.textui.parsetree.WhileLoop;
import net.sourceforge.kolmafia.utilities.ByteArrayStream;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Positions;

/**
 * See devdoc/ParseRoadmap.ebnf for a simplified representation of this class's parsing methods'
 * call hierarchy.
 */
public class Parser {
  public static final String APPROX = "\u2248";
  public static final String PRE_INCREMENT = "++X";
  public static final String PRE_DECREMENT = "--X";
  public static final String POST_INCREMENT = "X++";
  public static final String POST_DECREMENT = "X--";

  // Variables used during parsing

  private final String fileName;
  private final String shortFileName;
  private final URI fileUri;
  private String scriptName;
  private final InputStream istream;

  private Line currentLine;
  private int currentIndex;
  private Token currentToken;

  private final Map<File, Long> imports;
  private final List<AshDiagnostic> diagnostics = new ArrayList<>();
  private Function mainMethod = null;
  private String notifyRecipient = null;

  public Parser() {
    this(null, null, null);
  }

  public Parser(final File scriptFile, final Map<File, Long> imports) {
    this(scriptFile, null, imports);
  }

  public Parser(final File scriptFile, final InputStream stream, final Map<File, Long> imports) {
    this.imports = imports != null ? imports : new TreeMap<>();

    this.istream =
        stream != null
            ? stream
            : scriptFile != null ? DataUtilities.getInputStream(scriptFile) : null;

    if (scriptFile != null) {
      this.fileName = scriptFile.getPath();
      this.shortFileName = this.fileName.substring(this.fileName.lastIndexOf(File.separator) + 1);
      this.fileUri = scriptFile.toURI();

      if (this.imports.isEmpty()) {
        this.imports.put(scriptFile, scriptFile.lastModified());
      }
    } else {
      this.fileName = null;
      this.shortFileName = null;
      this.fileUri = null;
    }

    if (this.istream == null) {
      return;
    }

    try {
      final LineNumberReader commandStream =
          new LineNumberReader(new InputStreamReader(this.istream, StandardCharsets.UTF_8));
      this.currentLine = new Line(commandStream);

      Line line = this.currentLine;
      while (line.content != null) {
        line = new Line(commandStream, line);
      }

      // Move up to the first non-empty line
      while (this.currentLine.content != null && this.currentLine.content.length() == 0) {
        this.currentLine = this.currentLine.nextLine;
      }
      this.currentIndex = this.currentLine.offset;
    } catch (Exception e) {
      // If any part of the initialization fails,
      // then throw an exception.

      this.error(this.fileName + " could not be accessed");
    } finally {
      try {
        this.istream.close();
      } catch (IOException e) {
      }
    }
  }

  public Scope parse() {
    if (this.istream == null) {
      throw new RuntimeException(
          "Parser was not properly initialized before parsing was attempted");
    }

    return this.parseFile(null);
  }

  public String getFileName() {
    return this.fileName;
  }

  public String getShortFileName() {
    return this.shortFileName;
  }

  public URI getUri() {
    return this.fileUri;
  }

  public String getStringUri() {
    return this.fileUri != null ? this.fileUri.toString() : this.istream.toString();
  }

  public String getScriptName() {
    return (this.scriptName != null) ? this.scriptName : this.shortFileName;
  }

  public int getLineNumber() {
    if (this.istream == null) {
      return 0;
    }

    return this.currentLine.lineNumber;
  }

  public Map<File, Long> getImports() {
    return this.imports;
  }

  public List<AshDiagnostic> getDiagnostics() {
    return this.diagnostics;
  }

  public Function getMainMethod() {
    return this.mainMethod;
  }

  public String getNotifyRecipient() {
    return this.notifyRecipient;
  }

  public static Scope getExistingFunctionScope() {
    return new Scope(RuntimeLibrary.functions, null, DataTypes.simpleTypes);
  }

  // **************** Parser *****************

  private static final HashSet<String> multiCharTokens = new HashSet<>();
  private static final HashSet<String> reservedWords = new HashSet<>();

  static {
    // Tokens
    multiCharTokens.add("==");
    multiCharTokens.add("!=");
    multiCharTokens.add("<=");
    multiCharTokens.add(">=");
    multiCharTokens.add("||");
    multiCharTokens.add("&&");
    multiCharTokens.add("//");
    multiCharTokens.add("/*");
    multiCharTokens.add("<<");
    multiCharTokens.add(">>");
    multiCharTokens.add(">>>");
    multiCharTokens.add("++");
    multiCharTokens.add("--");
    multiCharTokens.add("**");
    multiCharTokens.add("+=");
    multiCharTokens.add("-=");
    multiCharTokens.add("*=");
    multiCharTokens.add("/=");
    multiCharTokens.add("%=");
    multiCharTokens.add("**=");
    multiCharTokens.add("&=");
    multiCharTokens.add("^=");
    multiCharTokens.add("|=");
    multiCharTokens.add("<<=");
    multiCharTokens.add(">>=");
    multiCharTokens.add(">>>=");
    multiCharTokens.add("...");

    // Constants
    reservedWords.add("true");
    reservedWords.add("false");

    // Operators
    reservedWords.add("contains");
    reservedWords.add("remove");
    reservedWords.add("new");

    // Control flow
    reservedWords.add("if");
    reservedWords.add("else");
    reservedWords.add("foreach");
    reservedWords.add("in");
    reservedWords.add("for");
    reservedWords.add("from");
    reservedWords.add("upto");
    reservedWords.add("downto");
    reservedWords.add("by");
    reservedWords.add("while");
    reservedWords.add("repeat");
    reservedWords.add("until");
    reservedWords.add("break");
    reservedWords.add("continue");
    reservedWords.add("return");
    reservedWords.add("exit");
    reservedWords.add("switch");
    reservedWords.add("case");
    reservedWords.add("default");
    reservedWords.add("try");
    reservedWords.add("catch");
    reservedWords.add("finally");
    reservedWords.add("static");

    // Data types
    reservedWords.add("void");
    reservedWords.add("boolean");
    reservedWords.add("int");
    reservedWords.add("float");
    reservedWords.add("string");
    reservedWords.add("buffer");
    reservedWords.add("matcher");
    reservedWords.add("aggregate");

    reservedWords.add("item");
    reservedWords.add("location");
    reservedWords.add("class");
    reservedWords.add("stat");
    reservedWords.add("skill");
    reservedWords.add("effect");
    reservedWords.add("familiar");
    reservedWords.add("slot");
    reservedWords.add("monster");
    reservedWords.add("element");
    reservedWords.add("coinmaster");

    reservedWords.add("record");
    reservedWords.add("typedef");
  }

  private static boolean isReservedWord(final String name) {
    return name != null && Parser.reservedWords.contains(name.toLowerCase());
  }

  public Scope importFile(final String fileName, final Scope scope) {
    return this.importFile(fileName, scope, null);
  }

  private Scope importFile(final String fileName, final Scope scope, final Location location) {
    List<File> matches = KoLmafiaCLI.findScriptFile(fileName);
    if (matches.size() > 1) {
      StringBuilder s = new StringBuilder();
      for (File f : matches) {
        if (s.length() > 0) s.append("; ");
        s.append(f.getPath());
      }
      this.error(location, "too many matches for " + fileName + ": " + s);

      return scope;
    }
    if (matches.size() == 0) {
      this.error(location, fileName + " could not be found");

      return scope;
    }

    File scriptFile = matches.get(0);

    if (this.imports.containsKey(scriptFile)) {
      return scope;
    }

    this.imports.put(scriptFile, scriptFile.lastModified());

    Parser parser = new Parser(scriptFile, null, this.imports);
    Scope result = parser.parseFile(scope);

    for (AshDiagnostic diagnostic : parser.diagnostics) {
      this.diagnostics.add(diagnostic);
    }

    if (parser.mainMethod
        != null) { // Make imported script's main() available under a different name
      UserDefinedFunction f =
          new UserDefinedFunction(
              parser.mainMethod.getName()
                  + "@"
                  + parser.getScriptName().replace(".ash", "").replaceAll("[^a-zA-Z0-9]", "_"),
              parser.mainMethod.getType(),
              parser.mainMethod.getVariableReferences(),
              parser.mainMethod.getDefinitionLocation());
      f.setScope(((UserDefinedFunction) parser.mainMethod).getScope());
      result.addFunction(f);
    }

    return result;
  }

  private Scope parseCommandOrDeclaration(final Scope result, final Type expectedType) {
    Type t = this.parseType(result, true);

    // If there is no data type, it's a command of some sort
    if (t == null) {
      Command c = this.parseCommand(expectedType, result, false, false, false);
      if (c != null) {
        result.addCommand(c, this);
      } else {
        this.error(this.currentToken(), "command or declaration required");
      }
    } else if (this.parseVariables(t, result)) {
      if (this.currentToken().equals(";")) {
        this.readToken(); // read ;
      } else {
        this.unexpectedTokenError(";", this.currentToken());
      }
    } else {
      // Found a type but no function or variable to tie it to
      this.error(
          Parser.makeLocation(t.getLocation(), this.currentToken()),
          "Type given but not used to declare anything");
    }

    return result;
  }

  private Scope parseFile(final Scope startScope) {
    final Scope result =
        startScope == null
            ? new Scope((VariableList) null, Parser.getExistingFunctionScope())
            : startScope;

    Token firstToken = this.currentToken();
    this.parseScope(result, null, result.getParentScope(), true, false, false);

    Location scriptLocation = this.makeLocation(firstToken, this.peekPreviousToken());
    result.setScopeLocation(scriptLocation);

    if (this.currentLine.nextLine != null) {
      this.error("Script parsing error", "thought we reached the end of the file");
    }

    return result;
  }

  private Scope parseScope(
      final Type expectedType,
      final VariableList variables,
      final BasicScope parentScope,
      final boolean allowBreak,
      final boolean allowContinue) {
    Scope result = new Scope(variables, parentScope);
    return this.parseScope(result, expectedType, parentScope, false, allowBreak, allowContinue);
  }

  private Scope parseScope(
      Scope result,
      final Type expectedType,
      final BasicScope parentScope,
      final boolean wholeFile,
      final boolean allowBreak,
      final boolean allowContinue) {
    Directive importDirective;

    this.parseScriptName();
    this.parseNotify();
    this.parseSince();

    while ((importDirective = this.parseImport()) != null) {
      result =
          this.importFile(importDirective.value, result, this.makeLocation(importDirective.range));
    }

    Position previousPosition = null;
    while (!this.atEndOfFile()) {
      // Infinite loop prevention
      if (!this.madeProgress(previousPosition, previousPosition = this.getCurrentPosition())) {
        if (!wholeFile) {
          break;
        }

        // If we're at the top scope of a file, and we reached a node we
        // couldn't parse, just read the current token and continue
        this.error(this.currentToken(), "Empty or unknown node");

        this.readToken();
        continue;
      }

      if (this.parseTypedef(result)) {
        if (this.currentToken().equals(";")) {
          this.readToken(); // read ;
        } else {
          this.unexpectedTokenError(";", this.currentToken());
        }

        continue;
      }

      Type t = this.parseType(result, true);

      // If there is no data type, it's a command of some sort
      if (t == null) {
        // See if it's a regular command
        Command c = this.parseCommand(expectedType, result, false, allowBreak, allowContinue);
        if (c != null) {
          result.addCommand(c, this);
        }

        continue;
      }

      // If this is a new record definition, enter it
      if (t.getType() == DataTypes.TYPE_RECORD && this.currentToken().equals(";")) {
        this.readToken(); // read ;
        continue;
      }

      Function f = this.parseFunction(t, result);
      if (f != null) {
        if (f.getName().equalsIgnoreCase("main")) {
          if (parentScope.getParentScope() == null) {
            this.mainMethod = f;
          } else {
            this.error(f.getDefinitionLocation(), "main method must appear at top level");
          }
        }

        continue;
      }

      if (this.parseVariables(t, result)) {
        if (this.currentToken().equals(";")) {
          this.readToken(); // read ;
        } else {
          this.unexpectedTokenError(";", this.currentToken());
        }

        continue;
      }

      if (this.currentToken().equals("{")) {
        if (t.getBaseType() instanceof AggregateType) {
          result.addCommand(this.parseAggregateLiteral(result, (AggregateType) t), this);
        } else {
          this.error(
              Parser.makeLocation(t.getLocation(), this.currentToken()),
              "Aggregate type required to make an aggregate literal");

          this.parseAggregateLiteral(result, badAggregateType());
        }
      } else {
        // Found a type but no function or variable to tie it to
        this.error(
            Parser.makeLocation(t.getLocation(), this.currentToken()),
            "Type given but not used to declare anything");
      }
    }

    return result;
  }

  private Type parseRecord(final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("record")) {
      return null;
    }

    Token recordStartToken = this.currentToken();

    this.readToken(); // read record

    if (this.currentToken().equals(";")) {
      this.error(this.currentToken(), "Record name expected");

      return new BadRecordType(null, this.makeLocation(recordStartToken));
    }

    // Allow anonymous records
    String recordName = null;

    if (!this.currentToken().equals("{")) {
      // Named record
      recordName = this.currentToken().content;

      if (!this.parseIdentifier(recordName)) {
        this.error(this.currentToken(), "Invalid record name '" + recordName + "'");

        recordName = null;
      } else if (Parser.isReservedWord(recordName)) {
        this.error(
            this.currentToken(), "Reserved word '" + recordName + "' cannot be a record name");

        recordName = null;
      } else if (parentScope.findType(recordName) != null) {
        this.error(this.currentToken(), "Record name '" + recordName + "' is already defined");

        recordName = null;
      }

      this.readToken(); // read name
    }

    if (this.currentToken().equals("{")) {
      this.readToken(); // read {
    } else {
      this.unexpectedTokenError("{", this.currentToken());

      return new BadRecordType(
          recordName, this.makeLocation(recordStartToken, this.peekPreviousToken()));
    }

    // Loop collecting fields
    List<Type> fieldTypes = new ArrayList<Type>();
    List<String> fieldNames = new ArrayList<String>();

    Position previousPosition = null;
    while (this.madeProgress(previousPosition, previousPosition = this.getCurrentPosition())) {
      if (this.atEndOfFile()) {
        this.unexpectedTokenError("}", this.currentToken());
        break;
      }

      if (this.currentToken().equals("}")) {
        if (fieldTypes.isEmpty()) {
          this.error(this.currentToken(), "Record field(s) expected");
        }

        this.readToken(); // read }
        break;
      }

      // Get the field type
      Type fieldType = this.parseType(parentScope, true);
      if (fieldType == null) {
        this.error(this.currentToken(), "Type name expected");
      } else if (fieldType.getBaseType().equals(DataTypes.VOID_TYPE)) {
        this.error(fieldType.getLocation(), "Non-void field type expected");
      }

      // Get the field name
      Token fieldName = this.currentToken();
      if (fieldName.equals(";")) {
        this.error(fieldName, "Field name expected");
        // don't read
      } else if (!this.parseIdentifier(fieldName.content)) {
        this.error(fieldName, "Invalid field name '" + fieldName + "'");
        // don't read
      } else if (Parser.isReservedWord(fieldName.content)) {
        this.error(fieldName, "Reserved word '" + fieldName + "' cannot be used as a field name");

        this.readToken(); // read name
      } else if (fieldNames.contains(fieldName.content)) {
        this.error(fieldName, "Field name '" + fieldName + "' is already defined");

        this.readToken(); // read name
      } else {
        this.readToken(); // read name
      }

      if (fieldType != null) {
        fieldTypes.add(fieldType);
        fieldNames.add(fieldName.content.toLowerCase());
      }

      if (this.currentToken().equals(";")) {
        this.readToken(); // read ;
      } else {
        this.unexpectedTokenError(";", this.currentToken());
      }
    }

    Location recordDefinition = this.makeLocation(recordStartToken, this.peekPreviousToken());

    String[] fieldNameArray = new String[fieldNames.size()];
    Type[] fieldTypeArray = new Type[fieldTypes.size()];
    fieldNames.toArray(fieldNameArray);
    fieldTypes.toArray(fieldTypeArray);

    RecordType rec =
        new RecordType(
            recordName != null
                ? recordName
                : ("(anonymous record "
                    + Integer.toHexString(Arrays.hashCode(fieldNameArray))
                    + ")"),
            fieldNameArray,
            fieldTypeArray,
            recordDefinition);

    if (recordName != null) {
      // Enter into type table
      parentScope.addType(rec);
    }

    return rec;
  }

  private Function parseFunction(final Type functionType, final Scope parentScope) {
    if (!this.parseIdentifier(this.currentToken().content)) {
      return null;
    }

    if (!"(".equals(this.nextToken())) {
      return null;
    }

    Token functionName = this.currentToken();

    if (Parser.isReservedWord(functionName.content)) {
      this.error(
          functionName, "Reserved word '" + functionName + "' cannot be used as a function name");
    }

    this.readToken(); // read Function name
    this.readToken(); // read (

    VariableList paramList = new VariableList();
    List<VariableReference> variableReferences = new ArrayList<>();
    boolean vararg = false;

    Position previousPosition = null;
    while (this.madeProgress(previousPosition, previousPosition = this.getCurrentPosition())) {
      if (this.atEndOfFile()) {
        this.unexpectedTokenError(")", this.currentToken());
        break;
      }

      if (this.currentToken().equals(")")) {
        this.readToken(); // read )
        break;
      }

      Type paramType = this.parseType(parentScope, false);
      if (paramType == null) {
        this.unexpectedTokenError(")", this.currentToken());
        break;
      }

      if (this.currentToken().equals("...")) {
        // Make a vararg type out of the previously parsed type.
        paramType =
            new VarArgType(paramType)
                .reference(Parser.makeLocation(paramType.getLocation(), this.currentToken()));

        this.readToken(); // read ...
      }

      Variable param = this.parseVariable(paramType, null);
      if (param == null) {
        this.unexpectedTokenError("identifier", this.currentToken());
        continue;
      }

      if (vararg) {
        if (paramType instanceof VarArgType) {
          // We can only have a single vararg parameter
          this.error(paramType.getLocation(), "Only one vararg parameter is allowed");
        } else {
          // The single vararg parameter must be the last one
          this.error(paramType.getLocation(), "The vararg parameter must be the last one");
        }
      } else if (!paramList.add(param)) {
        this.error(param.getLocation(), "Parameter " + param.getName() + " is already defined");
      } else {
        variableReferences.add(new VariableReference(param.getLocation(), param));
      }

      if (this.currentToken().equals("=")) {
        this.error(this.currentToken(), "Cannot initialize parameter " + param.getName());
      }

      if (paramType instanceof VarArgType) {
        // Only one vararg is allowed
        vararg = true;
      }

      if (!this.currentToken().equals(")")) {
        if (this.currentToken().equals(",")) {
          this.readToken(); // read comma
        } else {
          this.unexpectedTokenError(",", this.currentToken());
        }
      }
    }

    // Add the function to the parent scope before we parse the
    // function scope to allow recursion.

    Location functionLocation = this.makeLocation(functionName, this.peekLastToken());

    UserDefinedFunction f =
        new UserDefinedFunction(
            functionName.content, functionType, variableReferences, functionLocation);

    if (f.overridesLibraryFunction()) {
      this.overridesLibraryFunctionError(f);
    }

    UserDefinedFunction existing = parentScope.findFunction(f);

    if (existing != null && existing.getScope() != null) {
      this.multiplyDefinedFunctionError(f);
    }

    if (vararg) {
      Function clash = parentScope.findVarargClash(f);

      if (clash != null) {
        this.varargClashError(f, clash);
      }
    }

    // Add new function or replace existing forward reference

    UserDefinedFunction result = parentScope.replaceFunction(existing, f);

    if (this.currentToken().equals(";")) {
      // Return forward reference
      this.readToken(); // ;
      return result;
    }

    Scope scope =
        this.parseBlockOrSingleCommand(functionType, paramList, parentScope, false, false, false);

    result.setScope(scope);
    if (!scope.assertBarrier() && !functionType.equals(DataTypes.TYPE_VOID)) {
      this.error(functionLocation, "Missing return value");
    }

    return result;
  }

  private boolean parseVariables(final Type t, final BasicScope parentScope) {
    while (true) {
      Variable v = this.parseVariable(t, parentScope);
      if (v == null) {
        return false;
      }

      parentScope.addVariable(v);
      VariableReference lhs = new VariableReference(v.getLocation(), v);
      Evaluable rhs;

      if (this.currentToken().equals("=")) {
        this.readToken(); // read =

        rhs = this.parseInitialization(lhs, parentScope);
      } else if (this.currentToken().equals("{")) {
        // We allow two ways of initializing aggregates:
        // <aggregate type> <name> = {};
        // <aggregate type> <name> {};

        rhs = this.parseInitialization(lhs, parentScope);
      } else {
        rhs = null;
      }

      parentScope.addCommand(new Assignment(lhs, rhs), this);

      if (this.currentToken().equals(",")) {
        this.readToken(); // read ,
        continue;
      }

      return true;
    }
  }

  private Variable parseVariable(final Type t, final BasicScope scope) {
    if (!this.parseIdentifier(this.currentToken().content)) {
      return null;
    }

    Token variableName = this.currentToken();
    Variable result;

    if (Parser.isReservedWord(variableName.content)) {
      this.error(variableName, "Reserved word '" + variableName + "' cannot be a variable name");

      result = new BadVariable(variableName.content, t, this.makeLocation(variableName));
    } else if (scope != null && scope.findVariable(variableName.content) != null) {
      this.error(variableName, "Variable " + variableName + " is already defined");

      result = new BadVariable(variableName.content, t, this.makeLocation(variableName));
    } else {
      result = new Variable(variableName.content, t, this.makeLocation(variableName));
    }

    this.readToken(); // read name

    return result;
  }

  /**
   * Parses the right-hand-side of a variable definition. It is assumed that the caller expects an
   * expression to be found, so this method never returns null.
   */
  private Evaluable parseInitialization(final VariableReference lhs, final BasicScope scope) {
    Evaluable result;

    Type t = lhs.target.getType();
    Type ltype = t.getBaseType();
    if (this.currentToken().equals("{")) {
      if (ltype instanceof AggregateType) {
        result = this.parseAggregateLiteral(scope, (AggregateType) ltype);
      } else {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(
            errorLocation,
            "Cannot initialize " + lhs + " of type " + t + " with an aggregate literal");

        result = this.parseAggregateLiteral(scope, badAggregateType());
      }
    } else {
      result = this.parseExpression(scope);

      if (result != null) {
        result = this.autoCoerceValue(t, result, scope);
        if (!Operator.validCoercion(ltype, result.getType(), "assign")) {
          this.error(
              result.getLocation(),
              "Cannot store " + result.getType() + " in " + lhs + " of type " + ltype);
        }
      } else {
        this.error(this.currentToken(), "Expression expected");
      }
    }

    return result;
  }

  private Evaluable autoCoerceValue(Type ltype, final Evaluable rhs, final BasicScope scope) {
    // DataTypes.TYPE_ANY has no name
    if (ltype == null || ltype.getName() == null) {
      return rhs;
    }

    // If the types are the same no coercion needed
    // A TypeDef or a RecordType match names for equal.
    Type rtype = rhs.getRawType();
    if (ltype.equals(rtype)) {
      return rhs;
    }

    // Look for a function:  LTYPE to_LTYPE( RTYPE )
    String name = "to_" + ltype.getName();
    List<Evaluable> params = Collections.singletonList(rhs);

    // A typedef can overload a coercion function to a basic type or a typedef
    if (ltype instanceof TypeDef || ltype instanceof RecordType) {
      Function target = scope.findFunction(name, params, MatchType.EXACT);
      if (target != null && target.getType().equals(ltype)) {
        return new FunctionCall(rhs.getLocation(), target, params, this);
      }
    }

    if (ltype instanceof AggregateType) {
      return rhs;
    }

    if (rtype instanceof TypeDef || rtype instanceof RecordType) {
      Function target = scope.findFunction(name, params, MatchType.EXACT);
      if (target != null && target.getType().equals(ltype)) {
        return new FunctionCall(rhs.getLocation(), target, params, this);
      }
    }

    // No overloaded coercions found for typedefs or records
    return rhs;
  }

  private List<Evaluable> autoCoerceParameters(
      final Function target, final List<Evaluable> params, final BasicScope scope) {
    ListIterator<VariableReference> refIterator = target.getVariableReferences().listIterator();
    ListIterator<Evaluable> valIterator = params.listIterator();
    VariableReference vararg = null;
    VarArgType varargType = null;

    while ((vararg != null || refIterator.hasNext()) && valIterator.hasNext()) {
      // A VarArg parameter will consume all remaining values
      VariableReference currentParam = (vararg != null) ? vararg : refIterator.next();
      Type paramType = currentParam.getRawType();

      // If have found a vararg, remember it.
      if (vararg == null && paramType instanceof VarArgType) {
        vararg = currentParam;
        varargType = ((VarArgType) paramType);
      }

      // If we are matching a vararg, coerce to data type
      if (vararg != null) {
        paramType = varargType.getDataType();
      }

      Evaluable currentValue = valIterator.next();
      Evaluable coercedValue = this.autoCoerceValue(paramType, currentValue, scope);
      valIterator.set(coercedValue);
    }

    return params;
  }

  private boolean parseTypedef(final Scope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("typedef")) {
      return false;
    }

    Token typedefToken = this.currentToken();

    this.readToken(); // read typedef

    Type t = this.parseType(parentScope, true);
    if (t == null) {
      this.error(typedefToken, this.currentToken(), "Missing data type for typedef");

      t = new BadType(null, null);
    }

    Token typeName = this.currentToken();

    if (typeName.equals(";")) {
      this.error(typeName, "Type name expected");
      // don't read
    } else if (!this.parseIdentifier(typeName.content)) {
      this.error(typeName, "Invalid type name '" + typeName + "'");
      // don't read
    } else if (Parser.isReservedWord(typeName.content)) {
      this.error(typeName, "Reserved word '" + typeName + "' cannot be a type name");

      this.readToken(); // read name
    } else {
      this.readToken(); // read name

      Type existingType = parentScope.findType(typeName.content);
      if (existingType != null) {
        if (existingType.getBaseType().equals(t)) {
          // It is OK to redefine a typedef with an equivalent type
          return true;
        }

        this.error(typeName, "Type name '" + typeName + "' is already defined");
      } else {
        // Add the type to the type table
        TypeDef type =
            new TypeDef(
                typeName.content, t, this.makeLocation(typedefToken, this.peekPreviousToken()));
        parentScope.addType(type);
      }
    }

    return true;
  }

  private Command parseCommand(
      final Type functionType,
      final BasicScope scope,
      final boolean noElse,
      final boolean allowBreak,
      final boolean allowContinue) {
    Command result;

    if (this.currentToken().equalsIgnoreCase("break")) {
      if (allowBreak) {
        result = new LoopBreak(this.makeLocation(this.currentToken()));
      } else {
        this.error(this.currentToken(), "Encountered 'break' outside of loop");

        result = new BadScriptState(this.makeLocation(this.currentToken()));
      }

      this.readToken(); // break
    } else if (this.currentToken().equalsIgnoreCase("continue")) {
      if (allowContinue) {
        result = new LoopContinue(this.makeLocation(this.currentToken()));
      } else {
        this.error(this.currentToken(), "Encountered 'continue' outside of loop");

        result = new BadScriptState(this.makeLocation(this.currentToken()));
      }

      this.readToken(); // continue
    } else if (this.currentToken().equalsIgnoreCase("exit")) {
      result = new ScriptExit(this.makeLocation(this.currentToken()));
      this.readToken(); // exit
    } else if ((result = this.parseReturn(functionType, scope)) != null) {
    } else if ((result = this.parseBasicScript()) != null) {
      // basic_script doesn't have a ; token
      return result;
    } else if ((result = this.parseWhile(functionType, scope)) != null) {
      // while doesn't have a ; token
      return result;
    } else if ((result = this.parseForeach(functionType, scope)) != null) {
      // foreach doesn't have a ; token
      return result;
    } else if ((result = this.parseJavaFor(functionType, scope)) != null) {
      // for doesn't have a ; token
      return result;
    } else if ((result = this.parseFor(functionType, scope)) != null) {
      // for doesn't have a ; token
      return result;
    } else if ((result = this.parseRepeat(functionType, scope)) != null) {
    } else if ((result = this.parseSwitch(functionType, scope, allowContinue)) != null) {
      // switch doesn't have a ; token
      return result;
    } else if ((result =
            this.parseConditional(functionType, scope, noElse, allowBreak, allowContinue))
        != null) {
      // loop doesn't have a ; token
      return result;
    } else if ((result = this.parseTry(functionType, scope, allowBreak, allowContinue)) != null) {
      // try doesn't have a ; token
      return result;
    } else if ((result = this.parseCatch(functionType, scope, allowBreak, allowContinue)) != null) {
      // standalone catch doesn't have a ; token
      return result;
    } else if ((result = this.parseStatic(functionType, scope)) != null) {
      // try doesn't have a ; token
      return result;
    } else if ((result = this.parseSort(scope)) != null) {
    } else if ((result = this.parseRemove(scope)) != null) {
    } else if ((result =
            this.parseBlock(functionType, null, scope, noElse, allowBreak, allowContinue))
        != null) {
      // {} doesn't have a ; token
      return result;
    } else if ((result = this.parseEvaluable(scope)) != null) {
    } else {
      return null;
    }

    if (this.currentToken().equals(";")) {
      this.readToken(); // ;
    } else {
      this.unexpectedTokenError(";", this.currentToken());
    }

    return result;
  }

  private Type parseType(final BasicScope scope, final boolean records) {
    if (!this.parseIdentifier(this.currentToken().content)) {
      return null;
    }

    Type valType;

    if ((valType = this.parseRecord(scope)) != null) {
      if (!records) {
        this.error(valType.getLocation(), "Existing type expected for function parameter");
      }
    } else if ((valType = scope.findType(this.currentToken().content)) != null) {
      valType = valType.reference(this.makeLocation(this.currentToken()));
      this.readToken();
    } else {
      return null;
    }

    if (this.currentToken().equals("[")) {
      return this.parseAggregateType(valType, scope);
    }

    return valType;
  }

  /**
   * Parses the content of an aggregate literal, e.g., `{1:true, 2:false, 3:false}`.
   *
   * <p>The presence of the opening bracket "{" is ALWAYS assumed when entering this method, and as
   * such, MUST be checked before calling it. This method will never return null.
   */
  private Evaluable parseAggregateLiteral(final BasicScope scope, final AggregateType aggr) {
    Token aggregateLiteralStartToken = this.currentToken();

    this.readToken(); // read {

    Type index = aggr.getIndexType();
    Type data = aggr.getDataType();

    List<Evaluable> keys = new ArrayList<>();
    List<Evaluable> values = new ArrayList<>();

    // If index type is an int, it could be an array or a map
    boolean arrayAllowed = index.equals(DataTypes.INT_TYPE);

    // Assume it is a map.
    boolean isArray = false;

    Position previousPosition = null;
    while (this.madeProgress(previousPosition, previousPosition = this.getCurrentPosition())) {
      if (this.atEndOfFile()) {
        this.unexpectedTokenError("}", this.currentToken());
        break;
      }

      if (this.currentToken().equals("}")) {
        this.readToken(); // read }
        break;
      }

      Evaluable lhs;

      // If we know we are reading an ArrayLiteral or haven't
      // yet ensured we are reading a MapLiteral, allow any
      // type of Value as the "key"
      Type dataType = data.getBaseType();

      if (this.currentToken().equals("{")) {
        if (!isArray && !arrayAllowed) {
          // We know this is a map, but they placed
          // an aggregate literal as a key
          this.error(
              this.currentToken(),
              "Expected a key of type " + index.toString() + ", found an aggregate");
        }

        if (dataType instanceof AggregateType) {
          lhs = this.parseAggregateLiteral(scope, (AggregateType) dataType);
        } else {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(
              errorLocation,
              "Expected an element of type " + dataType.toString() + ", found an aggregate");

          lhs = this.parseAggregateLiteral(scope, badAggregateType());
        }
      } else {
        lhs = this.parseExpression(scope);
      }

      if (lhs == null) {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(
            errorLocation, "Script parsing error; couldn't figure out value of aggregate key");

        lhs = Value.locate(errorLocation, Value.BAD_VALUE);
      }

      Token delim = this.currentToken();

      // If this could be an array and we haven't already
      // decided it is one, if the delimiter is a comma,
      // parse as an ArrayLiteral
      if (arrayAllowed) {
        if (delim.equals(",") || delim.equals("}")) {
          isArray = true;
        }
        arrayAllowed = false;
      }

      if (!delim.equals(":")) {
        // If parsing an ArrayLiteral, accumulate only values
        if (isArray) {
          // The value must have the correct data type
          lhs = this.autoCoerceValue(data, lhs, scope);
          if (!Operator.validCoercion(dataType, lhs.getType(), "assign")) {
            this.error(
                lhs.getLocation(),
                "Invalid array literal; cannot assign type "
                    + dataType.toString()
                    + " to type "
                    + lhs.getType().toString());
          }

          values.add(lhs);
        } else {
          this.unexpectedTokenError(":", delim);
        }

        // Move on to the next value
        if (delim.equals(",")) {
          this.readToken(); // read ,
        } else if (!delim.equals("}")) {
          this.unexpectedTokenError("}", delim);
        }

        continue;
      }

      // We are parsing a MapLiteral
      this.readToken(); // read :

      if (isArray) {
        // In order to reach this point without an error, we must have had a correct
        // array literal so far, meaning the index type is an integer, and what we saw before
        // the colon must have matched the aggregate's data type. Therefore, the next
        // question is: is the data type also an integer?

        if (data.equals(DataTypes.INT_TYPE)) {
          // If so, this is an int[int] aggregate. They could have done something like
          // {0, 1, 2, 3:3, 4:4, 5:5}
          this.error(lhs.getLocation(), "Cannot include keys when making an array literal");
        } else {
          // If not, we can't tell why there's a colon here.
          this.unexpectedTokenError(", or }", delim);
        }
      }

      Evaluable rhs;

      if (this.currentToken().equals("{")) {
        if (dataType instanceof AggregateType) {
          rhs = this.parseAggregateLiteral(scope, (AggregateType) dataType);
        } else {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(
              errorLocation,
              "Expected a value of type " + dataType.toString() + ", found an aggregate");

          rhs = this.parseAggregateLiteral(scope, badAggregateType());
        }
      } else {
        rhs = this.parseExpression(scope);
      }

      if (rhs == null) {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(
            errorLocation, "Script parsing error; couldn't figure out value of aggregate value");

        rhs = Value.locate(errorLocation, Value.BAD_VALUE);
      }

      // Check that each type is valid via validCoercion
      lhs = this.autoCoerceValue(index, lhs, scope);
      rhs = this.autoCoerceValue(data, rhs, scope);

      if (!Operator.validCoercion(index, lhs.getType(), "assign")) {
        this.error(
            lhs.getLocation(),
            "Invalid map literal; cannot assign type "
                + index.toString()
                + " to key of type "
                + lhs.getType().toString());
      }

      if (!Operator.validCoercion(data, rhs.getType(), "assign")) {
        this.error(
            rhs.getLocation(),
            "Invalid map literal; cannot assign type "
                + dataType.toString()
                + " to value of type "
                + rhs.getType().toString());
      }

      keys.add(lhs);
      values.add(rhs);

      // Move on to the next value
      if (this.currentToken().equals(",")) {
        this.readToken(); // read ,
      } else if (!this.currentToken().equals("}")) {
        this.unexpectedTokenError("}", this.currentToken());
      }
    }

    Location aggregateLiteralLocation =
        this.makeLocation(aggregateLiteralStartToken, this.peekPreviousToken());

    if (isArray) {
      int size = aggr.getSize();
      if (size > 0 && size < values.size()) {
        this.error(
            aggregateLiteralLocation,
            "Array has " + size + " elements but " + values.size() + " initializers.");
      }
    }

    Value result = isArray ? new ArrayLiteral(aggr, values) : new MapLiteral(aggr, keys, values);

    return Value.locate(aggregateLiteralLocation, result);
  }

  private Type parseAggregateType(Type dataType, final BasicScope scope) {
    Token separatorToken = this.currentToken();

    this.readToken(); // [ or ,

    Type indexType = null;
    int size = 0;

    Token indexToken = this.currentToken();

    if (indexToken.equals("]")) {
      if (!separatorToken.equals("[")) {
        this.error(indexToken, "Missing index token");
      }
    } else if (this.readIntegerToken(indexToken.content)) {
      size = StringUtilities.parseInt(indexToken.content);
      this.readToken(); // integer
    } else if (this.parseIdentifier(indexToken.content)) {
      indexType = scope.findType(indexToken.content);

      if (indexType != null) {
        indexType = indexType.reference(this.makeLocation(indexToken));

        if (!indexType.isPrimitive()) {
          this.error(indexToken, "Index type '" + indexToken + "' is not a primitive type");

          indexType = new BadType(indexToken.content, this.makeLocation(indexToken));
        }
      } else {
        this.error(indexToken, "Invalid type name '" + indexToken + "'");

        indexType = new BadType(indexToken.content, this.makeLocation(indexToken));
      }

      this.readToken(); // type name
    } else {
      this.error(indexToken, "Missing index token");

      Type type = new AggregateType(dataType, new BadType(null, null));
      return type.reference(Parser.makeLocation(dataType.getLocation(), this.peekPreviousToken()));
    }

    if (this.currentToken().equals(",")
        || (this.currentToken().equals("]") && "[".equals(this.nextToken()))) {
      if (this.currentToken().equals("]")) {
        this.readToken(); // ]
      }

      dataType = this.parseAggregateType(dataType, scope);
    } else if (this.currentToken().equals("]")) {
      this.readToken(); // ]
    } else {
      this.unexpectedTokenError(", or ]", this.currentToken());
    }

    Type type =
        indexType != null
            ? new AggregateType(dataType, indexType)
            : new AggregateType(dataType, size);

    return type.reference(Parser.makeLocation(dataType.getLocation(), this.peekPreviousToken()));
  }

  private boolean parseIdentifier(final String identifier) {
    if (identifier == null) {
      return false;
    }

    if (!Character.isLetter(identifier.charAt(0)) && identifier.charAt(0) != '_') {
      return false;
    }

    for (int i = 1; i < identifier.length(); ++i) {
      if (!Character.isLetterOrDigit(identifier.charAt(i)) && identifier.charAt(i) != '_') {
        return false;
      }
    }

    return true;
  }

  private boolean parseScopedIdentifier(final String identifier) {
    if (identifier == null) {
      return false;
    }

    if (!Character.isLetter(identifier.charAt(0)) && identifier.charAt(0) != '_') {
      return false;
    }

    for (int i = 1; i < identifier.length(); ++i) {
      if (!Character.isLetterOrDigit(identifier.charAt(i))
          && identifier.charAt(i) != '_'
          && identifier.charAt(i) != '@') {
        return false;
      }
    }

    return true;
  }

  private FunctionReturn parseReturn(final Type expectedType, final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("return")) {
      return null;
    }

    Token returnStartToken = this.currentToken();

    this.readToken(); // return

    if (expectedType == null) {
      this.error(returnStartToken, "Cannot return when outside of a function");
    }

    if (this.currentToken().equals(";")) {
      if (expectedType != null && !expectedType.equals(DataTypes.TYPE_VOID)) {
        this.error(returnStartToken, "Return needs " + expectedType + " value");
      }

      return new FunctionReturn(this.makeLocation(returnStartToken), null, DataTypes.VOID_TYPE);
    }

    if (expectedType != null && expectedType.equals(DataTypes.TYPE_VOID)) {
      this.error(this.currentToken(), "Cannot return a value from a void function");
    }

    Evaluable value = this.parseExpression(parentScope);

    if (value != null) {
      value = this.autoCoerceValue(expectedType, value, parentScope);
    } else {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      value = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    if (expectedType != null && !Operator.validCoercion(expectedType, value.getType(), "return")) {
      this.error(
          value.getLocation(),
          "Cannot return " + value.getType() + " value from " + expectedType + " function");
    }

    Location returnLocation = this.makeLocation(returnStartToken, this.peekPreviousToken());
    return new FunctionReturn(returnLocation, value, expectedType);
  }

  private Scope parseSingleCommandScope(
      final Type functionType,
      final BasicScope parentScope,
      final boolean noElse,
      final boolean allowBreak,
      final boolean allowContinue) {
    Token scopeStartToken = this.currentToken();
    Scope result = new Scope(parentScope);

    Command command =
        this.parseCommand(functionType, parentScope, noElse, allowBreak, allowContinue);
    if (command != null) {
      result.addCommand(command, this);
    } else {
      if (this.currentToken().equals(";")) {
        this.readToken(); // ;
      } else {
        this.unexpectedTokenError(";", this.currentToken());
      }
    }

    Location scopeLocation = this.makeLocation(scopeStartToken, this.peekPreviousToken());
    result.setScopeLocation(scopeLocation);

    return result;
  }

  private Scope parseBlockOrSingleCommand(
      final Type functionType,
      final VariableList variables,
      final BasicScope parentScope,
      final boolean noElse,
      final boolean allowBreak,
      final boolean allowContinue) {
    Scope scope =
        this.parseBlock(functionType, variables, parentScope, noElse, allowBreak, allowContinue);
    if (scope != null) {
      return scope;
    }
    return this.parseSingleCommandScope(
        functionType, parentScope, noElse, allowBreak, allowContinue);
  }

  private Scope parseBlock(
      final Type functionType,
      final VariableList variables,
      final BasicScope parentScope,
      final boolean noElse,
      final boolean allowBreak,
      final boolean allowContinue) {
    if (!this.currentToken().equals("{")) {
      return null;
    }

    Token blockStartToken = this.currentToken();

    this.readToken(); // {

    Scope scope = this.parseScope(functionType, variables, parentScope, allowBreak, allowContinue);

    if (this.currentToken().equals("}")) {
      this.readToken(); // read }
    } else {
      this.unexpectedTokenError("}", this.currentToken());
    }

    Location blockLocation = this.makeLocation(blockStartToken, this.peekPreviousToken());
    scope.setScopeLocation(blockLocation);

    return scope;
  }

  private Conditional parseConditional(
      final Type functionType,
      final BasicScope parentScope,
      final boolean noElse,
      final boolean allowBreak,
      final boolean allowContinue) {
    if (!this.currentToken().equalsIgnoreCase("if")) {
      return null;
    }

    Token conditionalStartToken = this.currentToken();

    this.readToken(); // if

    if (this.currentToken().equals("(")) {
      this.readToken(); // (
    } else {
      this.unexpectedTokenError("(", this.currentToken());
    }

    Evaluable condition = this.parseExpression(parentScope);

    if (condition == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      this.unexpectedTokenError(")", this.currentToken());
    }

    if (!condition.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      Location errorLocation = condition.getLocation();

      this.error(errorLocation, "\"if\" requires a boolean conditional expression");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    If result = null;
    boolean elseFound = false;
    boolean finalElse = false;

    do {
      Scope scope =
          parseBlockOrSingleCommand(
              functionType, null, parentScope, !elseFound, allowBreak, allowContinue);

      Location conditionalLocation =
          this.makeLocation(conditionalStartToken, this.peekPreviousToken());

      // TODO save conditional chains as their own class so that we can access the first
      // If's location
      if (result == null) {
        result = new If(conditionalLocation, scope, condition);
      } else if (finalElse) {
        result.addElseLoop(new Else(conditionalLocation, scope, condition));
      } else {
        result.addElseLoop(new ElseIf(conditionalLocation, scope, condition));
      }

      if (!noElse && this.currentToken().equalsIgnoreCase("else")) {
        conditionalStartToken = this.currentToken();

        if (finalElse) {
          this.error(this.currentToken(), "Else without if");
        }

        this.readToken(); // else
        if (this.currentToken().equalsIgnoreCase("if")) {
          this.readToken(); // if

          if (this.currentToken().equals("(")) {
            this.readToken(); // (
          } else {
            this.unexpectedTokenError("(", this.currentToken());
          }

          condition = this.parseExpression(parentScope);

          if (condition == null) {
            Location errorLocation = this.makeLocation(this.currentToken());

            this.error(errorLocation, "Expression expected");

            condition = Value.locate(errorLocation, Value.BAD_VALUE);
          }

          if (this.currentToken().equals(")")) {
            this.readToken(); // )
          } else {
            this.unexpectedTokenError(")", this.currentToken());
          }

          if (!condition.getType().equals(DataTypes.BOOLEAN_TYPE)) {
            Location errorLocation = condition.getLocation();

            this.error(errorLocation, "\"if\" requires a boolean conditional expression");

            condition = Value.locate(errorLocation, Value.BAD_VALUE);
          }
        } else {
          // else without condition
          condition = Value.locate(this.makeZeroWidthLocation(), DataTypes.TRUE_VALUE);
          finalElse = true;
        }

        elseFound = true;
        continue;
      }

      elseFound = false;
    } while (elseFound);

    return result;
  }

  private BasicScript parseBasicScript() {
    if (!this.currentToken().equalsIgnoreCase("cli_execute")) {
      return null;
    }

    if (!"{".equals(this.nextToken())) {
      return null;
    }

    Token basicScriptStartToken = this.currentToken();

    this.readToken(); // cli_execute
    this.readToken(); // {

    ByteArrayStream ostream = new ByteArrayStream();

    while (true) {
      if (this.atEndOfFile()) {
        this.unexpectedTokenError("}", this.currentToken());
        break;
      }

      if (this.currentToken().equals("}")) {
        this.readToken(); // }
        break;
      }

      this.clearCurrentToken();

      final String line = this.restOfLine();

      try {
        ostream.write(line.getBytes());
        ostream.write(KoLConstants.LINE_BREAK.getBytes());
      } catch (Exception e) {
        // Byte array output streams do not throw errors,
        // other than out of memory errors.

        StaticEntity.printStackTrace(e);
      }

      if (line.length() > 0) {
        this.currentLine.makeToken(line.length());
      }
      this.currentLine = this.currentLine.nextLine;
      this.currentIndex = this.currentLine.offset;
    }

    Location basicScriptLocation =
        this.makeLocation(basicScriptStartToken, this.peekPreviousToken());
    return new BasicScript(basicScriptLocation, ostream);
  }

  private Loop parseWhile(final Type functionType, final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("while")) {
      return null;
    }

    Token whileStartToken = this.currentToken();

    this.readToken(); // while

    if (this.currentToken().equals("(")) {
      this.readToken(); // (
    } else {
      this.unexpectedTokenError("(", this.currentToken());
    }

    Evaluable condition = this.parseExpression(parentScope);

    if (condition == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      this.unexpectedTokenError(")", this.currentToken());
    }

    if (!condition.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      Location errorLocation = condition.getLocation();

      this.error(errorLocation, "\"while\" requires a boolean conditional expression");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    Scope scope = this.parseLoopScope(functionType, null, parentScope);

    Location whileLocation = this.makeLocation(whileStartToken, this.peekPreviousToken());
    return new WhileLoop(whileLocation, scope, condition);
  }

  private Loop parseRepeat(final Type functionType, final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("repeat")) {
      return null;
    }

    Token repeatStartToken = this.currentToken();

    this.readToken(); // repeat

    Scope scope = this.parseLoopScope(functionType, null, parentScope);

    if (this.currentToken().equalsIgnoreCase("until")) {
      this.readToken(); // until
    } else {
      this.unexpectedTokenError("until", this.currentToken());
    }

    if (this.currentToken().equals("(")) {
      this.readToken(); // (
    } else {
      this.unexpectedTokenError("(", this.currentToken());
    }

    Evaluable condition = this.parseExpression(parentScope);

    if (condition == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      this.unexpectedTokenError(")", this.currentToken());
    }

    if (!condition.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      Location errorLocation = condition.getLocation();

      this.error(errorLocation, "\"repeat\" requires a boolean conditional expression");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    Location repeatLocation = this.makeLocation(repeatStartToken, this.peekPreviousToken());
    return new RepeatUntilLoop(repeatLocation, scope, condition);
  }

  private Switch parseSwitch(
      final Type functionType, final BasicScope parentScope, final boolean allowContinue) {
    if (!this.currentToken().equalsIgnoreCase("switch")) {
      return null;
    }

    Token switchStartToken = this.currentToken();

    this.readToken(); // switch

    if (!this.currentToken().equals("(") && !this.currentToken().equals("{")) {
      this.unexpectedTokenError("( or {", this.currentToken());
    }

    Evaluable condition = Value.locate(this.makeZeroWidthLocation(), DataTypes.TRUE_VALUE);
    if (this.currentToken().equals("(")) {
      this.readToken(); // (

      condition = this.parseExpression(parentScope);

      if (condition == null) {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(errorLocation, "\"switch ()\" requires an expression");

        condition = Value.locate(errorLocation, Value.BAD_VALUE);
      }

      if (this.currentToken().equals(")")) {
        this.readToken(); // )
      } else {
        this.unexpectedTokenError(")", this.currentToken());
      }
    }

    Type type = condition.getType();

    Token switchScopeStartToken = this.currentToken();

    if (this.currentToken().equals("{")) {
      this.readToken(); // {
    } else {
      this.unexpectedTokenError("{", this.currentToken());
    }

    List<Evaluable> tests = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();
    int defaultIndex = -1;

    SwitchScope scope = new SwitchScope(parentScope);
    int currentIndex = 0;
    Integer currentInteger = null;

    Map<Value, Integer> labels = new TreeMap<>();
    boolean constantLabels = true;

    while (true) {
      if (this.currentToken().equalsIgnoreCase("case")) {
        this.readToken(); // case

        Evaluable test = this.parseExpression(parentScope);

        if (test == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Case label needs to be followed by an expression");

          test = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        if (this.currentToken().equals(":")) {
          this.readToken(); // :
        } else {
          this.unexpectedTokenError(":", this.currentToken());
        }

        if (!test.getType().equals(type)) {
          this.error(
              test.getLocation(),
              "Switch conditional has type "
                  + type
                  + " but label expression has type "
                  + test.getType());

          test = Value.locate(test.getLocation(), Value.BAD_VALUE);
        }

        if (currentInteger == null) {
          currentInteger = IntegerPool.get(currentIndex);
        }

        if (test instanceof Constant && ((Constant) test).value.getClass() == Value.class) {
          if (labels.get(((Constant) test).value) != null) {
            this.error(test.getLocation(), "Duplicate case label: " + test);
          } else {
            labels.put(((Constant) test).value, currentInteger);
          }
        } else {
          constantLabels = false;
        }

        tests.add(test);
        indices.add(currentInteger);
        scope.resetBarrier();

        continue;
      }

      if (this.currentToken().equalsIgnoreCase("default")) {
        Token defaultToken = this.currentToken();

        this.readToken(); // default

        if (this.currentToken().equals(":")) {
          this.readToken(); // :
        } else {
          this.unexpectedTokenError(":", this.currentToken());
        }

        if (defaultIndex == -1) {
          defaultIndex = currentIndex;
        } else {
          this.error(defaultToken, "Only one default label allowed in a switch statement");
        }

        scope.resetBarrier();

        continue;
      }

      Type t = this.parseType(scope, true);

      // If there is no data type, it's a command of some sort
      if (t == null) {
        // See if it's a regular command
        Command c = this.parseCommand(functionType, scope, false, true, allowContinue);
        if (c != null) {
          scope.addCommand(c, this);
          currentIndex = scope.commandCount();
          currentInteger = null;
          continue;
        }

        // No type and no command -> done.
        break;
      }

      if (!this.parseVariables(t, scope)) {
        // Found a type but no function or variable to tie it to
        this.error(
            Parser.makeLocation(t.getLocation(), this.currentToken()),
            "Type given but not used to declare anything");
      }

      if (this.currentToken().equals(";")) {
        this.readToken(); // read ;
      } else {
        this.unexpectedTokenError(";", this.currentToken());
      }

      currentIndex = scope.commandCount();
      currentInteger = null;
    }

    if (this.currentToken().equals("}")) {
      this.readToken(); // }
    } else {
      this.unexpectedTokenError("}", this.currentToken());
    }

    Location switchScopeLocation =
        this.makeLocation(switchScopeStartToken, this.peekPreviousToken());
    scope.setScopeLocation(switchScopeLocation);

    Location switchLocation = this.makeLocation(switchStartToken, this.peekPreviousToken());
    return new Switch(
        switchLocation,
        condition,
        tests,
        indices,
        defaultIndex,
        scope,
        constantLabels ? labels : null);
  }

  private Try parseTry(
      final Type functionType,
      final BasicScope parentScope,
      final boolean allowBreak,
      final boolean allowContinue) {
    if (!this.currentToken().equalsIgnoreCase("try")) {
      return null;
    }

    Token tryStartToken = this.currentToken();

    this.readToken(); // try

    Scope body =
        this.parseBlockOrSingleCommand(
            functionType, null, parentScope, false, allowBreak, allowContinue);

    // catch clauses would be parsed here

    Scope finalClause;

    if (this.currentToken().equalsIgnoreCase("finally")) {
      this.readToken(); // finally

      finalClause =
          this.parseBlockOrSingleCommand(
              functionType, null, body, false, allowBreak, allowContinue);
    } else {
      // this would not be an error if at least one catch was present
      this.error(
          this.makeLocation(tryStartToken, this.peekPreviousToken()),
          "\"try\" without \"finally\" is pointless");
      finalClause = new Scope(body);
    }

    Location tryLocation = this.makeLocation(tryStartToken, this.peekPreviousToken());
    return new Try(tryLocation, body, finalClause);
  }

  private Catch parseCatch(
      final Type functionType,
      final BasicScope parentScope,
      final boolean allowBreak,
      final boolean allowContinue) {
    if (!this.currentToken().equalsIgnoreCase("catch")) {
      return null;
    }

    Token catchStartToken = this.currentToken();

    this.readToken(); // catch

    Scope body =
        this.parseBlockOrSingleCommand(
            functionType, null, parentScope, false, allowBreak, allowContinue);

    return new Catch(this.makeLocation(catchStartToken, this.peekPreviousToken()), body);
  }

  private Catch parseCatchValue(final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("catch")) {
      return null;
    }

    Token catchStartToken = this.currentToken();

    this.readToken(); // catch

    Command body = this.parseBlock(null, null, parentScope, true, false, false);
    if (body == null) {
      Evaluable value = this.parseExpression(parentScope);
      if (value != null) {
        body = value;
      } else {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(errorLocation, "\"catch\" requires a block or an expression");

        body = Value.locate(errorLocation, Value.BAD_VALUE);
      }
    }

    return new Catch(this.makeLocation(catchStartToken, this.peekPreviousToken()), body);
  }

  private Scope parseStatic(final Type functionType, final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("static")) {
      return null;
    }

    Token staticStartToken = this.currentToken();

    this.readToken(); // static

    Scope result = new StaticScope(parentScope);

    if (this.currentToken().equals("{")) {
      this.readToken(); // read {

      this.parseScope(result, functionType, parentScope, false, false, false);

      if (this.currentToken().equals("}")) {
        this.readToken(); // read }
      } else {
        this.unexpectedTokenError("}", this.currentToken());
      }
    } else { // body is a single call
      this.parseCommandOrDeclaration(result, functionType);
    }

    Location staticLocation = this.makeLocation(staticStartToken, this.peekPreviousToken());
    result.setScopeLocation(staticLocation);

    return result;
  }

  private SortBy parseSort(final BasicScope parentScope) {
    // sort aggregate by expr

    if (!this.currentToken().equalsIgnoreCase("sort")) {
      return null;
    }

    if (this.nextToken() == null
        || this.nextToken().equals("(")
        || this.nextToken()
            .equals("=")) { // it's a call to a function named sort(), or an assignment to
      // a variable named sort, not the sort statement.
      return null;
    }

    Token sortStartToken = this.currentToken();

    this.readToken(); // sort

    // Get an aggregate reference
    Evaluable aggregate = this.parseVariableReference(parentScope);

    if (!(aggregate instanceof VariableReference)
        || !(aggregate.getType().getBaseType() instanceof AggregateType)) {
      Location errorLocation =
          aggregate != null ? aggregate.getLocation() : this.makeLocation(this.currentToken());

      this.error(errorLocation, "Aggregate reference expected");

      aggregate = badVariableReference(errorLocation, badAggregateType());
    }

    if (this.currentToken().equalsIgnoreCase("by")) {
      this.readToken(); // by
    } else {
      this.unexpectedTokenError("by", this.currentToken());
    }

    Token scopeStartToken = this.currentToken();

    // Define key variables of appropriate type
    VariableList varList = new VariableList();
    AggregateType type = (AggregateType) aggregate.getType().getBaseType();
    Variable valuevar = new Variable("value", type.getDataType(), this.makeZeroWidthLocation());
    varList.add(valuevar);
    Variable indexvar = new Variable("index", type.getIndexType(), this.makeZeroWidthLocation());
    varList.add(indexvar);

    // Parse the key expression in a new scope containing 'index' and 'value'
    Scope scope = new Scope(varList, parentScope);
    Evaluable expr = this.parseExpression(scope);

    if (expr == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      expr = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    Location scopeLocation = this.makeLocation(scopeStartToken, this.peekPreviousToken());
    scope.setScopeLocation(scopeLocation);

    Location sortLocation = this.makeLocation(sortStartToken, this.peekPreviousToken());
    return new SortBy(sortLocation, (VariableReference) aggregate, indexvar, valuevar, expr, this);
  }

  private Loop parseForeach(final Type functionType, final BasicScope parentScope) {
    // foreach key [, key ... ] in aggregate { scope }

    if (!this.currentToken().equalsIgnoreCase("foreach")) {
      return null;
    }

    Token foreachStartToken = this.currentToken();

    this.readToken(); // foreach

    List<String> names = new ArrayList<>();
    List<Location> locations = new ArrayList<>();

    while (true) {
      Token name = this.currentToken();

      if (!this.parseIdentifier(name.content)
          // "foreach in aggregate" (i.e. no key)
          || name.equalsIgnoreCase("in")
              && !"in".equalsIgnoreCase(this.nextToken())
              && !",".equals(this.nextToken())) {
        this.error(name, "Key variable name expected");

        if (this.currentToken().equals(",")) {
          // the variable name is missing, but they are not done
          this.readToken(); // ,
          continue;
        }

        if (name.equalsIgnoreCase("in") && !"in".equalsIgnoreCase(this.nextToken())) {
          break; // the variable name is missing, and they are done
        }

        this.readToken(); // unknown; skip
        break;
      } else if (Parser.isReservedWord(name.content)) {
        this.error(name, "Reserved word '" + name + "' cannot be a key variable name");
        names.add(null);
        locations.add(null);
      } else if (names.contains(name.content)) {
        this.error(name, "Key variable '" + name + "' is already defined");
        names.add(null);
        locations.add(null);
      } else {
        names.add(name.content);
        locations.add(this.makeLocation(name));
      }

      this.readToken(); // name

      if (this.currentToken().equals(",")) {
        this.readToken(); // ,
        continue;
      }

      if (this.currentToken().equalsIgnoreCase("in")) {
        this.readToken(); // in
        break;
      }

      this.unexpectedTokenError("in", this.currentToken());
      break;
    }

    // Get an aggregate reference
    Evaluable aggregate = this.parseEvaluable(parentScope);

    if (aggregate == null || !(aggregate.getType().getBaseType() instanceof AggregateType)) {
      Location errorLocation =
          aggregate != null ? aggregate.getLocation() : this.makeLocation(this.currentToken());

      this.error(errorLocation, "Aggregate reference expected");

      aggregate = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    // Define key variables of appropriate type
    VariableList varList = new VariableList();
    List<VariableReference> variableReferences = new ArrayList<>();
    Type type = aggregate.getType().getBaseType();

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      Location location = locations.get(i);

      Type itype;
      if (type == null) {
        this.error(location, "Too many key variables specified");

        break;
      }

      if (type instanceof AggregateType) {
        itype = ((AggregateType) type).getIndexType();
        type = ((AggregateType) type).getDataType();
      } else if (type.isBad()) {
        itype = new BadType(null, null);
      } else { // Variable after all key vars holds the value instead
        itype = type;
        type = null;
      }

      if (name != null) {
        Variable keyvar = new Variable(name, itype, location);
        varList.add(keyvar);
        variableReferences.add(new VariableReference(keyvar.getLocation(), keyvar));
      }
    }

    // Parse the scope with the list of keyVars
    Scope scope = this.parseLoopScope(functionType, varList, parentScope);

    // Add the foreach node with the list of varRefs
    Location foreachLocation = this.makeLocation(foreachStartToken, this.peekPreviousToken());
    return new ForEachLoop(foreachLocation, scope, variableReferences, aggregate, this);
  }

  private Loop parseFor(final Type functionType, final BasicScope parentScope) {
    // for identifier from X [upto|downto|to|] Y [by Z]? {scope }

    if (!this.currentToken().equalsIgnoreCase("for")) {
      return null;
    }

    if (!this.parseIdentifier(this.nextToken())) {
      return null;
    }

    Token forStartToken = this.currentToken();

    this.readToken(); // for

    Token name = this.currentToken();

    Variable indexvar;

    if (Parser.isReservedWord(name.content)) {
      this.error(name, "Reserved word '" + name + "' cannot be an index variable name");

      indexvar = new BadVariable(name.content, DataTypes.INT_TYPE, this.makeLocation(name));
    } else if (parentScope.findVariable(name.content) != null) {
      this.error(name, "Index variable '" + name + "' is already defined");

      indexvar = new BadVariable(name.content, DataTypes.INT_TYPE, this.makeLocation(name));
    } else {
      indexvar = new Variable(name.content, DataTypes.INT_TYPE, this.makeLocation(name));
    }

    this.readToken(); // name

    if (this.currentToken().equalsIgnoreCase("from")) {
      this.readToken(); // from
    } else {
      this.unexpectedTokenError("from", this.currentToken());
    }

    Evaluable initial = this.parseExpression(parentScope);

    if (initial == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression for initial value expected");

      initial = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    int direction = 0;

    if (this.currentToken().equalsIgnoreCase("upto")) {
      direction = 1;
      this.readToken(); // upto
    } else if (this.currentToken().equalsIgnoreCase("downto")) {
      direction = -1;
      this.readToken(); // downto
    } else if (this.currentToken().equalsIgnoreCase("to")) {
      direction = 0;
      this.readToken(); // to
    } else {
      this.unexpectedTokenError("to, upto, or downto", this.currentToken());
    }

    Evaluable last = this.parseExpression(parentScope);

    if (last == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression for floor/ceiling value expected");

      last = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    Evaluable increment = Value.locate(this.makeZeroWidthLocation(), DataTypes.ONE_VALUE);
    if (this.currentToken().equalsIgnoreCase("by")) {
      this.readToken(); // by
      increment = this.parseExpression(parentScope);

      if (increment == null) {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(errorLocation, "Expression for increment value expected");

        increment = Value.locate(errorLocation, Value.BAD_VALUE);
      }
    }

    // Put index variable onto a list
    VariableList varList = new VariableList();
    varList.add(indexvar);

    Scope scope = this.parseLoopScope(functionType, varList, parentScope);

    Location forLocation = this.makeLocation(forStartToken, this.peekPreviousToken());
    return new ForLoop(
        forLocation,
        scope,
        new VariableReference(indexvar.getLocation(), indexvar),
        initial,
        last,
        increment,
        direction,
        this);
  }

  private Loop parseJavaFor(final Type functionType, final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("for")) {
      return null;
    }

    if (!"(".equals(this.nextToken())) {
      return null;
    }

    Token javaForStartToken = this.currentToken();

    this.readToken(); // for
    this.readToken(); // (

    // Parse variables and initializers

    Scope scope = new Scope(parentScope);
    List<Assignment> initializers = new ArrayList<>();

    // Parse each initializer in the context of scope, adding
    // variable to variable list in the scope, and saving
    // initialization expressions in initializers.

    while (!this.currentToken().equals(";")) {
      Type t = this.parseType(scope, true);

      Token name = this.currentToken();
      Variable variable;

      if (!this.parseIdentifier(name.content) || Parser.isReservedWord(name.content)) {
        this.error(name, "Identifier required");
      }

      // If there is no data type, it is using an existing variable
      if (t == null) {
        variable = parentScope.findVariable(name.content);
        if (variable == null) {
          this.error(name, "Unknown variable '" + name + "'");

          variable =
              new BadVariable(name.content, new BadType(null, null), this.makeLocation(name));
        }

        t = variable.getType();
      } else {
        // Create variable and add it to the scope
        variable = scope.findVariable(name.content, true);
        if (variable == null) {
          variable = new Variable(name.content, t, this.makeLocation(name));

          scope.addVariable(variable);
        } else {
          this.error(name, "Variable '" + name + "' already defined");
        }
      }

      this.readToken(); // name

      VariableReference lhs = new VariableReference(variable.getLocation(), variable);
      Evaluable rhs = null;

      if (this.currentToken().equals("=")) {
        this.readToken(); // =

        rhs = this.parseExpression(scope);

        if (rhs == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Expression expected");

          rhs = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        Type ltype = t.getBaseType();
        rhs = this.autoCoerceValue(t, rhs, scope);
        Type rtype = rhs.getType();

        if (!Operator.validCoercion(ltype, rtype, "assign")) {
          this.error(
              rhs.getLocation(), "Cannot store " + rtype + " in " + name + " of type " + ltype);

          rhs = Value.locate(rhs.getLocation(), Value.BAD_VALUE);
        }
      }

      Assignment initializer = new Assignment(lhs, rhs);

      initializers.add(initializer);

      if (this.currentToken().equals(",")) {
        this.readToken(); // ,

        if (this.currentToken().equals(";")) {
          this.error(this.currentToken(), "Identifier expected");
        }
      }
    }

    if (this.currentToken().equals(";")) {
      this.readToken(); // ;
    } else {
      this.unexpectedTokenError(";", this.currentToken());
    }

    // Parse condition in context of scope

    Evaluable condition =
        this.currentToken().equals(";")
            ? Value.locate(this.makeZeroWidthLocation(), DataTypes.TRUE_VALUE)
            : this.parseExpression(scope);

    if (condition == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    if (this.currentToken().equals(";")) {
      this.readToken(); // ;
    } else {
      this.unexpectedTokenError(";", this.currentToken());
    }

    if (!condition.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      Location errorLocation = condition.getLocation();

      this.error(errorLocation, "\"for\" requires a boolean conditional expression");

      condition = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    // Parse incrementers in context of scope

    List<Command> incrementers = new ArrayList<>();

    Position previousPosition = null;
    while (this.madeProgress(previousPosition, previousPosition = this.getCurrentPosition())) {
      if (this.atEndOfFile() || this.currentToken().equals(")")) {
        break;
      }

      Evaluable value = this.parsePreIncDec(scope);
      if (value != null) {
        incrementers.add(value);
      } else {
        value = this.parseVariableReference(scope);
        if (!(value instanceof VariableReference)) {
          Location errorLocation =
              value != null ? value.getLocation() : this.makeLocation(this.currentToken());

          this.error(errorLocation, "Variable reference expected");

          value = badVariableReference(errorLocation);
        }

        VariableReference ref = (VariableReference) value;
        Evaluable lhs = this.parsePostIncDec(ref);

        if (lhs == ref) {
          Assignment incrementer = this.parseAssignment(scope, ref);

          if (incrementer != null) {
            incrementers.add(incrementer);
          } else {
            this.error(value.getLocation(), "Variable '" + ref.getName() + "' not incremented");
          }
        } else {
          incrementers.add(lhs);
        }
      }

      if (this.currentToken().equals(",")) {
        this.readToken(); // ,

        if (this.atEndOfFile() || this.currentToken().equals(")")) {
          this.error(this.currentToken(), "Identifier expected");
        }
      }
    }

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      this.unexpectedTokenError(")", this.currentToken());
    }

    // Parse scope body
    this.parseLoopScope(scope, functionType, parentScope);

    Location javaForLocation = this.makeLocation(javaForStartToken, this.peekPreviousToken());
    return new JavaForLoop(javaForLocation, scope, initializers, condition, incrementers);
  }

  private Scope parseLoopScope(
      final Type functionType, final VariableList varList, final BasicScope parentScope) {
    return this.parseLoopScope(new Scope(varList, parentScope), functionType, parentScope);
  }

  private Scope parseLoopScope(
      final Scope result, final Type functionType, final BasicScope parentScope) {
    Token loopScopeStartToken = this.currentToken();

    if (this.currentToken().equals("{")) {
      // Scope is a block

      this.readToken(); // {

      this.parseScope(result, functionType, parentScope, false, true, true);

      if (this.currentToken().equals("}")) {
        this.readToken(); // }
      } else {
        this.unexpectedTokenError("}", this.currentToken());
      }
    } else {
      // Scope is a single command
      Command command = this.parseCommand(functionType, result, false, true, true);
      if (command == null) {
        if (this.currentToken().equals(";")) {
          this.readToken(); // ;
        } else {
          this.unexpectedTokenError(";", this.currentToken());
        }
      } else {
        result.addCommand(command, this);
      }
    }

    Location loopScopeLocation = this.makeLocation(loopScopeStartToken, this.peekPreviousToken());
    result.setScopeLocation(loopScopeLocation);

    return result;
  }

  private Evaluable parseNewRecord(final BasicScope scope) {
    if (!this.currentToken().equalsIgnoreCase("new")) {
      return null;
    }

    Token newRecordStartToken = this.currentToken();

    this.readToken();

    if (!this.parseIdentifier(this.currentToken().content)) {
      this.unexpectedTokenError("Record name", this.currentToken());

      return Value.locate(this.makeLocation(this.peekPreviousToken()), Value.BAD_VALUE);
    }

    Token name = this.currentToken();
    Type type = scope.findType(name.content);

    if (!(type instanceof RecordType)) {
      this.error(name, "'" + name + "' is not a record type");

      type = new BadRecordType(null, this.makeLocation(name));
    }

    RecordType target = (RecordType) type;

    this.readToken(); // name

    List<Evaluable> params = new ArrayList<>();
    String[] names = target.getFieldNames();
    Type[] types = target.getFieldTypes();
    int param = 0;

    if (this.currentToken().equals("(")) {
      this.readToken(); // (

      Position previousPosition = null;
      while (this.madeProgress(previousPosition, previousPosition = this.getCurrentPosition())) {
        if (this.atEndOfFile()) {
          this.unexpectedTokenError(")", this.currentToken());
          break;
        }

        if (this.currentToken().equals(")")) {
          this.readToken(); // )
          break;
        }

        Type currentType;
        String errorMessageFieldName = "";

        if (param < types.length) {
          currentType = types[param];
          errorMessageFieldName = " (" + names[param] + ")";
        } else {
          this.error(this.currentToken(), "Too many field initializers for record " + name);

          currentType = new BadType(null, null);
        }

        Type expected = currentType.getBaseType();
        Evaluable val;

        if (this.currentToken().equals(",")) {
          val = Value.locate(this.makeZeroWidthLocation(), DataTypes.VOID_VALUE);
        } else if (this.currentToken().equals("{")) {
          if (expected instanceof AggregateType) {
            val = this.parseAggregateLiteral(scope, (AggregateType) expected);
          } else {
            this.error(
                this.currentToken(),
                "Aggregate literal found when "
                    + expected
                    + " expected for field #"
                    + (param + 1)
                    + errorMessageFieldName);

            val = this.parseAggregateLiteral(scope, badAggregateType());
          }
        } else {
          val = this.parseExpression(scope);
        }

        if (val == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(
              errorLocation,
              "Expression expected for field #" + (param + 1) + errorMessageFieldName);

          val = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        if (!val.evaluatesTo(DataTypes.VOID_VALUE)) {
          val = this.autoCoerceValue(currentType, val, scope);
          Type given = val.getType();
          if (!Operator.validCoercion(expected, given, "assign")) {
            Location errorLocation = val.getLocation();

            this.error(
                errorLocation,
                given
                    + " found when "
                    + expected
                    + " expected for field #"
                    + (param + 1)
                    + errorMessageFieldName);

            val = Value.locate(errorLocation, Value.BAD_VALUE);
          }
        }

        params.add(val);
        param++;

        if (this.currentToken().equals(",")) {
          this.readToken(); // ,
        } else if (!this.currentToken().equals(")")) {
          this.unexpectedTokenError(", or )", this.currentToken());
          break;
        }
      }
    }

    Location newRecordLocation = this.makeLocation(newRecordStartToken, this.peekPreviousToken());
    return Value.locate(newRecordLocation, target.initialValueExpression(params));
  }

  private Evaluable parseCall(final BasicScope scope) {
    return this.parseCall(scope, null);
  }

  private Evaluable parseCall(final BasicScope scope, final Evaluable firstParam) {
    if (!"(".equals(this.nextToken())) {
      return null;
    }

    if (!this.parseScopedIdentifier(this.currentToken().content)) {
      return null;
    }

    Token name = this.currentToken();
    this.readToken(); // name

    List<Evaluable> params = this.parseParameters(scope, firstParam);
    Function target = scope.findFunction(name.content, params);

    Location functionCallLocation = this.makeLocation(name, this.peekPreviousToken());
    // Include the first parameter, if any, in the FunctionCall's location
    if (firstParam != null) {
      functionCallLocation = Parser.mergeLocations(firstParam.getLocation(), functionCallLocation);
    }

    if (target != null) {
      params = this.autoCoerceParameters(target, params, scope);
    } else {
      this.undefinedFunctionError(name, params);

      target = new BadFunction(name.content);
    }

    FunctionCall call = new FunctionCall(functionCallLocation, target, params, this);
    return this.parsePostCall(scope, call);
  }

  private List<Evaluable> parseParameters(final BasicScope scope, final Evaluable firstParam) {
    if (!this.currentToken().equals("(")) {
      return null;
    }

    this.readToken(); // (

    List<Evaluable> params = new ArrayList<>();
    if (firstParam != null) {
      params.add(firstParam);
    }

    while (true) {
      if (this.atEndOfFile()) {
        this.unexpectedTokenError(")", this.currentToken());
        break;
      }

      if (this.currentToken().equals(")")) {
        this.readToken(); // )
        break;
      }

      Evaluable val = this.parseExpression(scope);
      if (val != null) {
        params.add(val);
      }

      if (this.atEndOfFile()) {
        this.unexpectedTokenError(")", this.currentToken());
        break;
      }

      if (!this.currentToken().equals(",")) {
        if (!this.currentToken().equals(")")) {
          this.unexpectedTokenError(")", this.currentToken());
          break;
        }
        continue;
      }

      this.readToken(); // ,

      if (this.atEndOfFile()) {
        this.unexpectedTokenError("parameter", this.currentToken());
        break;
      }

      if (this.currentToken().equals(")")) {
        this.unexpectedTokenError("parameter", this.currentToken());
        // we'll break out at the start of the next loop
      }
    }

    return params;
  }

  private Evaluable parsePostCall(final BasicScope scope, FunctionCall call) {
    Evaluable result = call;
    while (result != null && this.currentToken().equals(".")) {
      Variable current = new Variable(result.getType());
      current.setExpression(result);

      result =
          this.parseVariableReference(scope, new VariableReference(result.getLocation(), current));
    }

    return result;
  }

  private Evaluable parseInvoke(final BasicScope scope) {
    if (!this.currentToken().equalsIgnoreCase("call")) {
      return null;
    }

    Token invokeStartToken = this.currentToken();

    this.readToken(); // call

    Type type = this.parseType(scope, false);

    // You can omit the type, but then this function invocation
    // cannot be used in an expression

    if (type == null) {
      type = DataTypes.VOID_TYPE;
    }

    Token current = this.currentToken();
    Evaluable name = null;

    if (current.equals("(")) {
      name = this.parseExpression(scope);

      if (name == null || !name.getType().equals(DataTypes.STRING_TYPE)) {
        Location errorLocation = name != null ? name.getLocation() : this.makeLocation(current);

        this.error(errorLocation, "String expression expected for function name");

        name = Value.locate(errorLocation, Value.BAD_VALUE);
      }
    } else {
      name = this.parseVariableReference(scope);

      if (!(name instanceof VariableReference)) {
        Location errorLocation =
            name != null ? name.getLocation() : this.makeLocation(this.currentToken());

        this.error(errorLocation, "Variable reference expected for function name");

        name = badVariableReference(errorLocation);
      }
    }

    List<Evaluable> params;

    if (this.currentToken().equals("(")) {
      params = this.parseParameters(scope, null);
    } else {
      this.unexpectedTokenError("(", this.currentToken());

      params = new ArrayList<>();
    }

    Location invokeLocation = this.makeLocation(invokeStartToken, this.peekPreviousToken());
    FunctionInvocation call =
        new FunctionInvocation(invokeLocation, scope, type, name, params, this);

    return this.parsePostCall(scope, call);
  }

  private Assignment parseAssignment(final BasicScope scope, final VariableReference lhs) {
    Token operStr = this.currentToken();
    if (!operStr.equals("=")
        && !operStr.equals("+=")
        && !operStr.equals("-=")
        && !operStr.equals("*=")
        && !operStr.equals("/=")
        && !operStr.equals("%=")
        && !operStr.equals("**=")
        && !operStr.equals("&=")
        && !operStr.equals("^=")
        && !operStr.equals("|=")
        && !operStr.equals("<<=")
        && !operStr.equals(">>=")
        && !operStr.equals(">>>=")) {
      return null;
    }

    Type ltype = lhs.getType().getBaseType();
    boolean isAggregate = (ltype instanceof AggregateType);

    if (isAggregate && !operStr.equals("=")) {
      this.error(operStr, "Cannot use '" + operStr + "' on an aggregate");
    }

    Operator oper = new Operator(this.makeLocation(operStr), operStr.content, this);
    this.readToken(); // oper

    Evaluable rhs;

    if (this.currentToken().equals("{")) {
      if (isAggregate) {
        rhs = this.parseAggregateLiteral(scope, (AggregateType) ltype);
      } else {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(errorLocation, "Cannot use an aggregate literal for type " + lhs.getType());

        rhs = this.parseAggregateLiteral(scope, badAggregateType());
      }
    } else {
      rhs = this.parseExpression(scope);
    }

    if (rhs == null) {
      Location errorLocation = this.makeLocation(this.currentToken());

      this.error(errorLocation, "Expression expected");

      rhs = Value.locate(errorLocation, Value.BAD_VALUE);
    }

    rhs = this.autoCoerceValue(lhs.getRawType(), rhs, scope);
    if (!oper.validCoercion(lhs.getType(), rhs.getType())) {
      String error =
          oper.isLogical()
              ? (oper
                  + " requires an integer or boolean expression and an integer or boolean variable reference")
              : oper.isInteger()
                  ? (oper + " requires an integer expression and an integer variable reference")
                  : ("Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType());
      this.error(Parser.mergeLocations(lhs.getLocation(), rhs.getLocation()), error);
    }

    Operator op = null;

    if (!operStr.equals("=")) {
      op =
          new Operator(
              this.makeLocation(Parser.makeInlineRange(operStr.getStart(), operStr.length() - 1)),
              operStr.substring(0, operStr.length() - 1),
              this);
    }

    return new Assignment(lhs, rhs, op);
  }

  private Evaluable parseRemove(final BasicScope scope) {
    if (!this.currentToken().equalsIgnoreCase("remove")) {
      return null;
    }

    return this.parseExpression(scope);
  }

  private Evaluable parsePreIncDec(final BasicScope scope) {
    if (this.nextToken() == null) {
      return null;
    }

    // --[VariableReference]
    // ++[VariableReference]

    if (!this.currentToken().equals("++") && !this.currentToken().equals("--")) {
      return null;
    }

    Token operToken = this.currentToken();
    String operStr = operToken.equals("++") ? Parser.PRE_INCREMENT : Parser.PRE_DECREMENT;

    this.readToken(); // oper

    Evaluable lhs = this.parseVariableReference(scope);
    if (!(lhs instanceof VariableReference)) {
      Location errorLocation =
          lhs != null ? lhs.getLocation() : this.makeLocation(this.currentToken());

      this.error(errorLocation, "Variable reference expected");

      lhs = badVariableReference(errorLocation);
    }

    int ltype = lhs.getType().getType();
    if (ltype != DataTypes.TYPE_INT && ltype != DataTypes.TYPE_FLOAT) {
      this.error(lhs.getLocation(), operStr + " requires a numeric variable reference");
    }

    Operator oper = new Operator(this.makeLocation(operToken), operStr, this);

    Location preIncDecLocation = this.makeLocation(operToken, this.peekPreviousToken());
    return new IncDec(preIncDecLocation, (VariableReference) lhs, oper);
  }

  private Evaluable parsePostIncDec(final VariableReference lhs) {
    // [VariableReference]++
    // [VariableReference]--

    if (!this.currentToken().equals("++") && !this.currentToken().equals("--")) {
      return lhs;
    }

    Token operToken = this.currentToken();
    String operStr = operToken.equals("++") ? Parser.POST_INCREMENT : Parser.POST_DECREMENT;

    int ltype = lhs.getType().getType();
    if (ltype != DataTypes.TYPE_INT && ltype != DataTypes.TYPE_FLOAT) {
      this.error(lhs.getLocation(), operStr + " requires a numeric variable reference");
    }

    this.readToken(); // oper

    Operator oper = new Operator(this.makeLocation(operToken), operStr, this);

    Location postIncDecLocation = Parser.mergeLocations(lhs, oper);
    return new IncDec(postIncDecLocation, lhs, oper);
  }

  private Evaluable parseExpression(final BasicScope scope) {
    return this.parseExpression(scope, null);
  }

  private Evaluable parseExpression(final BasicScope scope, final Operator previousOper) {
    if (this.currentToken().equals(";")) {
      return null;
    }

    Evaluable lhs = null;
    Evaluable rhs = null;
    Operator oper = null;

    Token operator = this.currentToken();
    if (operator.equals("!")) {
      oper = new Operator(this.makeLocation(operator), operator.content, this);
      this.readToken(); // !
      if ((lhs = this.parseEvaluable(scope)) == null) {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(errorLocation, "Value expected");

        lhs = Value.locate(errorLocation, Value.BAD_VALUE);
      }

      lhs = this.autoCoerceValue(DataTypes.BOOLEAN_TYPE, lhs, scope);
      lhs = new Operation(lhs, oper);
      if (!lhs.getType().equals(DataTypes.BOOLEAN_TYPE)) {
        this.error(lhs.getLocation(), "\"!\" operator requires a boolean value");
      }
    } else if (operator.equals("~")) {
      oper = new Operator(this.makeLocation(operator), operator.content, this);
      this.readToken(); // ~
      if ((lhs = this.parseEvaluable(scope)) == null) {
        Location errorLocation = this.makeLocation(this.currentToken());

        this.error(errorLocation, "Value expected");

        lhs = Value.locate(errorLocation, Value.BAD_VALUE);
      }

      lhs = new Operation(lhs, oper);
      if (!lhs.getType().equals(DataTypes.INT_TYPE)
          && !lhs.getType().equals(DataTypes.BOOLEAN_TYPE)) {
        this.error(lhs.getLocation(), "\"~\" operator requires an integer or boolean value");
      }
    } else if (operator.equals("-")) {
      // See if it's a negative numeric constant
      if ((lhs = this.parseEvaluable(scope)) == null) {
        // Nope. Unary minus.
        oper = new Operator(this.makeLocation(operator), operator.content, this);
        this.readToken(); // -
        if ((lhs = this.parseEvaluable(scope)) == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Value expected");

          lhs = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        lhs = new Operation(lhs, oper);
        if (!lhs.getType().equals(DataTypes.INT_TYPE)
            && !lhs.getType().equals(DataTypes.FLOAT_TYPE)) {
          this.error(lhs.getLocation(), "\"-\" operator requires an integer or float value");
        }
      }
    } else if (operator.equalsIgnoreCase("remove")) {
      oper = new Operator(this.makeLocation(operator), operator.content.toLowerCase(), this);
      this.readToken(); // remove

      lhs = this.parseVariableReference(scope);
      if (!(lhs instanceof CompositeReference)) {
        Location errorLocation =
            lhs != null ? lhs.getLocation() : this.makeLocation(this.currentToken());

        this.error(errorLocation, "Aggregate reference expected");

        if (!(lhs instanceof VariableReference)) {
          lhs = badVariableReference(errorLocation);
        }
      }

      lhs = new Operation(lhs, oper);
    } else if ((lhs = this.parseEvaluable(scope)) == null) {
      return null;
    }

    do {
      oper = this.parseOperator(this.currentToken());

      if (oper == null) {
        return lhs;
      }

      if (previousOper != null && !oper.precedes(previousOper)) {
        return lhs;
      }

      if (this.currentToken().equals(":")) {
        return lhs;
      }

      if (this.currentToken().equals("?")) {
        this.readToken(); // ?

        Evaluable conditional = lhs;

        if (!conditional.getType().equals(DataTypes.BOOLEAN_TYPE)) {
          this.error(
              conditional.getLocation(),
              "Non-boolean expression " + conditional + " (" + conditional.getType() + ")");
        }

        if ((lhs = this.parseExpression(scope)) == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Value expected in left hand side");

          lhs = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        if (this.currentToken().equals(":")) {
          this.readToken(); // :
        } else {
          this.unexpectedTokenError(":", this.currentToken());
        }

        if ((rhs = this.parseExpression(scope)) == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Value expected in right hand side");

          rhs = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        if (!oper.validCoercion(lhs.getType(), rhs.getType())) {
          this.error(
              Parser.mergeLocations(lhs.getLocation(), rhs.getLocation()),
              "Cannot choose between "
                  + lhs
                  + " ("
                  + lhs.getType()
                  + ") and "
                  + rhs
                  + " ("
                  + rhs.getType()
                  + ")");
        }

        lhs = new TernaryExpression(conditional, lhs, rhs);
      } else {
        this.readToken(); // operator

        if ((rhs = this.parseExpression(scope, oper)) == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Value expected");

          rhs = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        Type ltype = lhs.getType();
        Type rtype = rhs.getType();

        if (oper.equals("+")
            && (ltype.equals(DataTypes.TYPE_STRING) || rtype.equals(DataTypes.TYPE_STRING))) {
          // String concatenation
          if (!ltype.equals(DataTypes.TYPE_STRING)) {
            lhs = this.autoCoerceValue(DataTypes.STRING_TYPE, lhs, scope);
          }
          if (!rtype.equals(DataTypes.TYPE_STRING)) {
            rhs = this.autoCoerceValue(DataTypes.STRING_TYPE, rhs, scope);
          }
          if (lhs instanceof Concatenate) {
            ((Concatenate) lhs).addString(rhs);
          } else {
            lhs = new Concatenate(lhs, rhs);
          }
        } else {
          Location operationLocation = Parser.mergeLocations(lhs.getLocation(), rhs.getLocation());

          rhs = this.autoCoerceValue(ltype, rhs, scope);
          if (!oper.validCoercion(ltype, rhs.getType())) {
            this.error(
                operationLocation,
                "Cannot apply operator "
                    + oper
                    + " to "
                    + lhs
                    + " ("
                    + lhs.getType()
                    + ") and "
                    + rhs
                    + " ("
                    + rhs.getType()
                    + ")");
          }
          lhs = new Operation(lhs, rhs, oper);
        }
      }
    } while (true);
  }

  private Evaluable parseEvaluable(final BasicScope scope) {
    if (this.currentToken().equals(";")) {
      return null;
    }

    Token valueStartToken = this.currentToken();

    Evaluable result = null;

    // Parse parenthesized expressions
    if (valueStartToken.equals("(")) {
      this.readToken(); // (

      result = this.parseExpression(scope);

      if (this.currentToken().equals(")")) {
        this.readToken(); // )
      } else {
        this.unexpectedTokenError(")", this.currentToken());
      }

      if (result != null) {
        // Include the parenthesis in its location
        result.growLocation(this.makeLocation(valueStartToken, this.peekPreviousToken()));
      }
    }

    // Parse constant values
    // true and false are reserved words

    else if (valueStartToken.equalsIgnoreCase("true")) {
      this.readToken();
      result = Value.locate(this.makeLocation(valueStartToken), DataTypes.TRUE_VALUE);
    } else if (valueStartToken.equalsIgnoreCase("false")) {
      this.readToken();
      result = Value.locate(this.makeLocation(valueStartToken), DataTypes.FALSE_VALUE);
    } else if (valueStartToken.equals("__FILE__")) {
      this.readToken();
      result =
          Value.locate(
              this.makeLocation(valueStartToken), new Value(String.valueOf(this.shortFileName)));
    }

    // numbers
    else if ((result = this.parseNumber()) != null) {
    } else if ((result = this.parseString(scope)) != null) {
    } else if ((result = this.parseTypedConstant(scope)) != null) {
    } else if ((result = this.parseNewRecord(scope)) != null) {
    } else if ((result = this.parseCatchValue(scope)) != null) {
    } else if ((result = this.parsePreIncDec(scope)) != null) {
      return result;
    } else if ((result = this.parseInvoke(scope)) != null) {
    } else if ((result = this.parseCall(scope)) != null) {
    } else {
      Token anchor = this.currentToken();

      Type baseType = this.parseType(scope, false);
      if (baseType != null && baseType.getBaseType() instanceof AggregateType) {
        if (this.currentToken().equals("{")) {
          result = this.parseAggregateLiteral(scope, (AggregateType) baseType.getBaseType());
        } else {
          this.unexpectedTokenError("{", this.currentToken());
          // don't parse. We don't know if they just didn't put anything.

          result = Value.locate(this.makeZeroWidthLocation(), Value.BAD_VALUE);
        }
      } else {
        if (baseType != null) {
          this.rewindBackTo(anchor);
        }
        if ((result = this.parseVariableReference(scope)) != null) {}
      }
    }

    while (result != null && (this.currentToken().equals(".") || this.currentToken().equals("["))) {
      Variable current = new Variable(result.getType());
      current.setExpression(result);

      result =
          this.parseVariableReference(scope, new VariableReference(result.getLocation(), current));
    }

    if (result instanceof VariableReference) {
      VariableReference ref = (VariableReference) result;
      result = this.parseAssignment(scope, ref);
      if (result == null) {
        result = this.parsePostIncDec(ref);
      }
    }

    return result;
  }

  private Evaluable parseNumber() {
    Token numberStartToken = this.currentToken();

    Value number;
    int sign = 1;

    if (this.currentToken().equals("-")) {
      String next = this.nextToken();

      if (!".".equals(next) && !this.readIntegerToken(next)) {
        // Unary minus
        return null;
      }

      sign = -1;
      this.readToken(); // Read -
    }

    if (this.currentToken().equals(".")) {
      this.readToken(); // Read .
      Token fraction = this.currentToken();

      if (this.readIntegerToken(fraction.content)) {
        this.readToken(); // integer
        number = new Value(sign * StringUtilities.parseDouble("0." + fraction));
      } else {
        this.unexpectedTokenError("numeric value", fraction);

        number = new Value(0);
      }

      return Value.locate(this.makeLocation(numberStartToken, this.peekPreviousToken()), number);
    }

    Token integer = this.currentToken();
    if (!this.readIntegerToken(integer.content)) {
      return null;
    }

    this.readToken(); // integer

    String fraction = this.nextToken();

    if (this.currentToken().equals(".") && this.readIntegerToken(fraction)) {
      this.readToken(); // .
      this.readToken(); // fraction

      number = new Value(sign * StringUtilities.parseDouble(integer + "." + fraction));
    } else {
      number = new Value(sign * StringUtilities.parseLong(integer.content));
    }

    return Value.locate(this.makeLocation(numberStartToken, this.peekPreviousToken()), number);
  }

  private boolean readIntegerToken(final String token) {
    if (token == null) {
      return false;
    }

    for (int i = 0; i < token.length(); ++i) {
      if (!Character.isDigit(token.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  private Evaluable parseString(final BasicScope scope) {
    if (!this.currentToken().equals("\"")
        && !this.currentToken().equals("'")
        && !this.currentToken().equals("`")) {
      return null;
    }

    this.clearCurrentToken();

    // Directly work with currentLine - ignore any "tokens" you meet until
    // the string is closed

    Position stringStartPosition = this.getCurrentPosition();

    char startCharacter = this.restOfLine().charAt(0);
    char stopCharacter = startCharacter;
    boolean template = startCharacter == '`';

    Concatenate conc = null;
    StringBuilder resultString = new StringBuilder();
    for (int i = 1; ; ++i) {
      final String line = this.restOfLine();

      if (i == line.length()) {
        if (i == 0
            && this.currentIndex == this.currentLine.offset
            && this.currentLine.content != null) {
          // Empty lines are OK.
          this.currentLine = this.currentLine.nextLine;
          this.currentIndex = this.currentLine.offset;
          i = -1;
          continue;
        }

        if (i > 0) {
          this.currentLine.makeToken(i);
          this.currentIndex += i;
        }

        // Plain strings can't span lines
        this.error(stringStartPosition, "No closing " + stopCharacter + " found");

        Evaluable result =
            Value.locate(
                this.makeLocation(stringStartPosition), new Value(resultString.toString()));

        if (conc == null) {
          return result;
        } else {
          conc.addString(result);
          return conc;
        }
      }

      char ch = line.charAt(i);

      // Handle escape sequences
      if (ch == '\\') {
        i = this.parseEscapeSequence(resultString, i);
        continue;
      }

      // Handle template substitutions
      if (template && ch == '{') {
        // Move the current token to the expression
        this.currentToken = this.currentLine.makeToken(++i);
        this.readToken(); // read the string so far, including the {

        Evaluable rhs = this.parseExpression(scope);

        if (rhs == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Expression expected");

          rhs = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        // Set i to -1 so that it is set to zero by the loop, as the
        // currentLine has been shortened.
        i = -1;

        // Skip comments before the next token, look at what it is, then
        // discard said token.
        if (this.currentToken().equals("}")) {
          // Increment manually to not skip whitespace after the curly brace.
          ++i; // }
        } else {
          this.unexpectedTokenError("}", this.currentToken());
        }

        this.clearCurrentToken();

        Evaluable lhs =
            Value.locate(
                this.makeLocation(stringStartPosition), new Value(resultString.toString()));
        if (conc == null) {
          conc = new Concatenate(lhs, rhs);
        } else {
          conc.addString(lhs);
          conc.addString(rhs);
        }

        resultString.setLength(0);
        stringStartPosition = this.getCurrentPosition();
        continue;
      }

      if (ch == stopCharacter) {
        this.currentToken =
            this.currentLine.makeToken(i + 1); // + 1 to get rid of stop character token
        this.readToken();

        Location resultLocation =
            this.makeLocation(stringStartPosition, this.peekPreviousToken().getEnd());
        Evaluable result = Value.locate(resultLocation, new Value(resultString.toString()));

        if (conc == null) {
          return result;
        } else {
          conc.addString(result);
          return conc;
        }
      }
      resultString.append(ch);
    }
  }

  private int parseEscapeSequence(final StringBuilder resultString, int i) {
    final int backslashIndex = i++;
    final String line = this.restOfLine();

    if (i == line.length()) {
      resultString.append('\n');
      this.currentLine.makeToken(i);
      this.currentLine = this.currentLine.nextLine;
      this.currentIndex = this.currentLine.offset;
      return -1;
    }

    char ch = line.charAt(i);

    switch (ch) {
      case 'n':
        resultString.append('\n');
        break;

      case 'r':
        resultString.append('\r');
        break;

      case 't':
        resultString.append('\t');
        break;

      case 'x':
        try {
          int hex08 = Integer.parseInt(line.substring(i + 1, i + 3), 16);
          resultString.append((char) hex08);
          i += 2;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
          Location errorLocation =
              this.makeLocation(
                  Parser.makeInlineRange(
                      new Position(this.getLineNumber() - 1, backslashIndex),
                      Math.min(4, line.length() - backslashIndex)));

          this.error(errorLocation, "Hexadecimal character escape requires 2 digits");

          resultString.append(ch);
        }
        break;

      case 'u':
        try {
          int hex16 = Integer.parseInt(line.substring(i + 1, i + 5), 16);
          resultString.append((char) hex16);
          i += 4;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
          Location errorLocation =
              this.makeLocation(
                  Parser.makeInlineRange(
                      new Position(this.getLineNumber() - 1, backslashIndex),
                      Math.min(6, line.length() - backslashIndex)));

          this.error(errorLocation, "Unicode character escape requires 4 digits");

          resultString.append(ch);
        }
        break;

      default:
        if (Character.isDigit(ch)) {
          try {
            int octal = Integer.parseInt(line.substring(i, i + 3), 8);
            resultString.append((char) octal);
            i += 2;
            break;
          } catch (IndexOutOfBoundsException | NumberFormatException e) {
            Location errorLocation =
                this.makeLocation(
                    Parser.makeInlineRange(
                        new Position(this.getLineNumber() - 1, backslashIndex),
                        Math.min(4, line.length() - backslashIndex)));

            this.error(errorLocation, "Octal character escape requires 3 digits");
          }
        }
        resultString.append(ch);
    }

    return i;
  }

  private Value parseLiteral(final Type type, final String element, final Location location) {
    Value value = DataTypes.parseValue(type, element, false);
    if (value == null) {
      this.error(location, "Bad " + type.toString() + " value: \"" + element + "\"");

      return Value.BAD_VALUE;
    }

    if (!StringUtilities.isNumeric(element)) {
      String fullName = value.toString();
      if (!element.equalsIgnoreCase(fullName)) {
        String s1 =
            CharacterEntities.escape(
                StringUtilities.globalStringReplace(element, ",", "\\,")
                    .replaceAll("(?<= ) ", "\\\\ "));
        String s2 =
            CharacterEntities.escape(
                StringUtilities.globalStringReplace(fullName, ",", "\\,")
                    .replaceAll("(?<= ) ", "\\\\ "));
        List<String> names = new ArrayList<String>();
        if (type.equals(DataTypes.ITEM_TYPE)) {
          int itemId = (int) value.contentLong;
          String name = ItemDatabase.getItemName(itemId);
          int[] ids = ItemDatabase.getItemIds(name, 1, false);
          for (int id : ids) {
            String s3 = "$item[[" + id + "]" + name + "]";
            names.add(s3);
          }
        } else if (type.equals(DataTypes.EFFECT_TYPE)) {
          int effectId = (int) value.contentLong;
          String name = EffectDatabase.getEffectName(effectId);
          int[] ids = EffectDatabase.getEffectIds(name, false);
          for (int id : ids) {
            String s3 = "$effect[[" + id + "]" + name + "]";
            names.add(s3);
          }
        } else if (type.equals(DataTypes.MONSTER_TYPE)) {
          int monsterId = (int) value.contentLong;
          String name = MonsterDatabase.findMonsterById(monsterId).getName();
          int[] ids = MonsterDatabase.getMonsterIds(name, false);
          for (int id : ids) {
            String s3 = "$monster[[" + id + "]" + name + "]";
            names.add(s3);
          }
        } else if (type.equals(DataTypes.SKILL_TYPE)) {
          int skillId = (int) value.contentLong;
          String name = SkillDatabase.getSkillName(skillId);
          int[] ids = SkillDatabase.getSkillIds(name, false);
          for (int id : ids) {
            String s3 = "$skill[[" + id + "]" + name + "]";
            names.add(s3);
          }
        }

        if (names.size() > 1) {
          this.warning(
              location,
              "Multiple matches for \"" + s1 + "\"; using \"" + s2 + "\".",
              "Clarify by using one of:"
                  + KoLConstants.LINE_BREAK
                  + String.join(KoLConstants.LINE_BREAK, names));
        } else {
          this.warning(
              location, "Changing \"" + s1 + "\" to \"" + s2 + "\" would get rid of this message.");
        }
      }
    }

    return value;
  }

  private Evaluable parseTypedConstant(final BasicScope scope) {
    if (!this.currentToken().equals("$")) {
      return null;
    }

    Token typedConstantStartToken = this.currentToken();

    this.readToken(); // read $

    Token name = this.currentToken();
    Type type = null;
    boolean plurals = false;

    if (this.parseIdentifier(name.content)) {
      type = scope.findType(name.content);

      if (type == null) {
        StringBuilder buf = new StringBuilder(name.content);
        int length = name.length();

        if (name.endsWith("ies")) {
          buf.delete(length - 3, length);
          buf.insert(length - 3, "y");
        } else if (name.endsWith("es")) {
          buf.delete(length - 2, length);
        } else if (name.endsWith("s")) {
          buf.deleteCharAt(length - 1);
        } else if (name.endsWith("a")) {
          buf.deleteCharAt(length - 1);
          buf.insert(length - 1, "um");
        }

        type = scope.findType(buf.toString());

        plurals = true;
      }

      this.readToken();
    }

    if (type == null) {
      this.error(name, "Unknown type " + name);

      type = new BadType(name.content, this.makeLocation(name));
    } else {
      type = type.reference(this.makeLocation(name));
    }

    if (!type.isPrimitive()) {
      this.error(name, "Non-primitive type " + name);

      type = new BadType(name.content, this.makeLocation(name));
    }

    if (this.currentToken().equals("[")) {
      this.readToken(); // read [
    } else {
      this.unexpectedTokenError("[", this.currentToken());

      return Value.locate(
          this.makeLocation(typedConstantStartToken, this.peekPreviousToken()), Value.BAD_VALUE);
    }

    if (plurals) {
      Value value = this.parsePluralConstant(scope, type);

      Location typedConstantLocation =
          this.makeLocation(typedConstantStartToken, this.peekPreviousToken());

      if (value != null) {
        return Value.locate(typedConstantLocation, value); // explicit list of values
      }
      value = type.allValues();
      if (value != null) {
        return Value.locate(typedConstantLocation, value); // implicit enumeration
      }

      this.error(typedConstantLocation, "Can't enumerate all " + name);

      return Value.locate(typedConstantLocation, Value.BAD_VALUE);
    }

    StringBuilder resultString = new StringBuilder();
    final Value result;

    Position currentElementStartPosition = this.getCurrentPosition();

    int level = 1;
    for (int i = 0; ; ++i) {
      final String line = this.restOfLine();

      if (i == line.length()) {
        if (i > 0) {
          this.currentLine.makeToken(i);
          this.currentIndex += i;
        }

        Location currentElementLocation = this.makeLocation(currentElementStartPosition);

        this.error(currentElementLocation, "No closing ] found");

        String input = resultString.toString().trim();
        result = this.parseLiteral(type, input, currentElementLocation);
        break;
      }

      char c = line.charAt(i);
      if (c == '\\') {
        if (i == line.length() - 1) {
          // Will throw an error at the start of the next loop
          continue;
        }

        resultString.append(line.charAt(++i));
      } else if (c == '[') {
        level++;
        resultString.append(c);
      } else if (c == ']') {
        if (--level > 0) {
          resultString.append(c);
          continue;
        }

        if (i > 0) {
          this.currentLine.makeToken(i);
          this.currentIndex += i;
        }

        Location currentElementLocation = this.makeLocation(currentElementStartPosition);
        this.readToken(); // read ]
        String input = resultString.toString().trim();
        result = this.parseLiteral(type, input, currentElementLocation);
        break;
      } else {
        resultString.append(c);
      }
    }

    return Value.locate(
        this.makeLocation(typedConstantStartToken, this.peekPreviousToken()), result);
  }

  private PluralValue parsePluralConstant(final BasicScope scope, final Type type) {
    // Directly work with currentLine - ignore any "tokens" you meet until
    // the string is closed

    List<Value> list = new ArrayList<>();
    int level = 1;
    boolean slash = false;

    Position currentElementStartPosition = this.getCurrentPosition();

    StringBuilder resultString = new StringBuilder();
    for (int i = 0; ; ++i) {
      final String line = this.restOfLine();

      if (i == line.length()) {
        if (i > 0) {
          this.currentLine.makeToken(i);
          this.currentIndex += i;
        }

        if (slash) {
          slash = false;
          resultString.append('/');
        }

        if (this.currentLine.content == null) {
          Location currentElementLocation = this.makeLocation(currentElementStartPosition);

          this.error(currentElementLocation, "No closing ] found");

          String element = resultString.toString().trim();
          if (element.length() != 0) {
            list.add(this.parseLiteral(type, element, currentElementLocation));
          }

          break;
        }

        this.currentLine = this.currentLine.nextLine;
        this.currentIndex = this.currentLine.offset;
        i = -1;
        continue;
      }

      char ch = line.charAt(i);

      // Handle escape sequences
      if (ch == '\\') {
        i = this.parseEscapeSequence(resultString, i);
        continue;
      }

      // Potentially handle comments
      // If we've already seen a slash
      if (slash) {
        slash = false;
        if (ch == '/') {
          if (i > 1) {
            this.currentLine.makeToken(i - 1);
            this.currentIndex += i - 1;
          }
          // Throw away the rest of the line
          this.currentLine.makeComment(this.restOfLine().length());
          this.currentIndex += this.restOfLine().length();
          i = -1;
          continue;
        }
        resultString.append('/');
      } else if (ch == '/') {
        slash = true;
        continue;
      }

      // Allow start char without escaping
      if (ch == '[') {
        level++;
        resultString.append(ch);
        continue;
      }

      // Match non-initial start char
      if (ch == ']' && --level > 0) {
        resultString.append(ch);
        continue;
      }

      if (ch != ']' && ch != ',') {
        resultString.append(ch);
        continue;
      }

      // Add a new element to the list
      String element = resultString.toString().trim();
      resultString.setLength(0);
      if (element.length() != 0) {
        Position currentElementEndPosition =
            new Position(this.getLineNumber() - 1, this.currentIndex + i);
        Location currentElementLocation =
            this.makeLocation(new Range(currentElementStartPosition, currentElementEndPosition));
        currentElementStartPosition = currentElementEndPosition;

        list.add(this.parseLiteral(type, element, currentElementLocation));
      }

      if (ch == ']') {
        if (i > 0) {
          this.currentLine.makeToken(i);
          this.currentIndex += i;
        }
        this.readToken(); // read ]

        break;
      }
    }

    if (list.size() == 0) {
      // Empty list - caller will interpret this specially
      return null;
    }
    return new PluralValue(type, list);
  }

  private Operator parseOperator(final Token oper) {
    if (!this.isOperator(oper.content)) {
      return null;
    }

    return new Operator(this.makeLocation(oper), oper.content, this);
  }

  private boolean isOperator(final String oper) {
    return oper.equals("!")
        || oper.equals("?")
        || oper.equals(":")
        || oper.equals("*")
        || oper.equals("**")
        || oper.equals("/")
        || oper.equals("%")
        || oper.equals("+")
        || oper.equals("-")
        || oper.equals("&")
        || oper.equals("^")
        || oper.equals("|")
        || oper.equals("~")
        || oper.equals("<<")
        || oper.equals(">>")
        || oper.equals(">>>")
        || oper.equals("<")
        || oper.equals(">")
        || oper.equals("<=")
        || oper.equals(">=")
        || oper.equals("==")
        || oper.equals(Parser.APPROX)
        || oper.equals("!=")
        || oper.equals("||")
        || oper.equals("&&")
        || oper.equals("contains")
        || oper.equals("remove");
  }

  private Evaluable parseVariableReference(final BasicScope scope) {
    if (!this.parseIdentifier(this.currentToken().content)) {
      return null;
    }

    Token name = this.currentToken();
    Location variableLocation = this.makeLocation(name);

    Variable variable = scope.findVariable(name.content, true);

    if (variable == null) {
      this.error(variableLocation, "Unknown variable '" + name + "'");

      variable = new BadVariable(name.content, new BadType(null, null), variableLocation);
    }

    this.readToken(); // read name

    return this.parseVariableReference(scope, new VariableReference(variableLocation, variable));
  }

  /**
   * Look for an index/key, and return the corresponding data, expecting {@code varRef} to be a
   * {@link AggregateType}/{@link RecordType}, e.g., {@code map.key}, {@code array[0]}.
   *
   * <p>May also return a {@link FunctionCall} if the chain ends with/is a function call, e.g.,
   * {@code varRef.function()}.
   *
   * <p>There may also be nothing, in which case the submitted variable reference is returned as is.
   */
  private Evaluable parseVariableReference(final BasicScope scope, final VariableReference varRef) {
    VariableReference current = varRef;
    Type type = varRef.getType();
    List<Evaluable> indices = new ArrayList<>();

    boolean parseAggregate = this.currentToken().equals("[");

    while (this.currentToken().equals("[")
        || this.currentToken().equals(".")
        || parseAggregate && this.currentToken().equals(",")) {
      Evaluable index;

      type = type.getBaseType();

      if (this.currentToken().equals("[") || this.currentToken().equals(",")) {
        this.readToken(); // read [ or ,
        parseAggregate = true;

        if (!(type instanceof AggregateType)) {
          Location location = Parser.makeLocation(current.getLocation(), this.peekPreviousToken());
          String message;
          if (indices.isEmpty()) {
            message = "Variable '" + varRef.getName() + "' cannot be indexed";
          } else {
            message = "Too many keys for '" + varRef.getName() + "'";
          }
          this.error(location, message);

          type = badAggregateType();
        }

        AggregateType atype = (AggregateType) type;
        index = this.parseExpression(scope);
        if (index == null) {
          Location errorLocation = this.makeLocation(this.currentToken());

          this.error(errorLocation, "Index for '" + current.getName() + "' expected");

          index = Value.locate(errorLocation, Value.BAD_VALUE);
        }

        if (!index.getType().getBaseType().equals(atype.getIndexType().getBaseType())) {
          this.error(
              index.getLocation(),
              "Index for '"
                  + current.getName()
                  + "' has wrong data type "
                  + "(expected "
                  + atype.getIndexType()
                  + ", got "
                  + index.getType()
                  + ")");
        }

        type = atype.getDataType();
      } else {
        this.readToken(); // read .

        // Maybe it's a function call with an implied "this" parameter.

        if ("(".equals(this.nextToken())) {
          return this.parseCall(scope, current);
        }

        type = type.asProxy();
        if (!(type instanceof RecordType)) {
          this.error(current.getLocation(), "Record expected");

          type = new BadRecordType(null, null);
        }

        RecordType rtype = (RecordType) type;

        Token field = this.currentToken();
        if (this.parseIdentifier(field.content)) {
          this.readToken(); // read name
        } else {
          this.error(field, "Field name expected");
        }

        index = Value.locate(this.makeLocation(field), rtype.getFieldIndex(field.content));
        if (index != null) {
          type = rtype.getDataType(index);
        } else {
          this.error(field, "Invalid field name '" + field + "'");

          index = Value.locate(this.makeLocation(field), Value.BAD_VALUE);
          type = new BadType(null, null);
        }
      }

      indices.add(index);

      if (parseAggregate && this.currentToken().equals("]")) {
        this.readToken(); // read ]
        parseAggregate = false;
      }

      Location currentLocation =
          Parser.makeLocation(current.getLocation(), this.peekPreviousToken());
      current = new CompositeReference(currentLocation, current.target, indices, this);
    }

    if (parseAggregate) {
      this.unexpectedTokenError("]", this.currentToken());
    }

    return current;
  }

  private class Directive {
    final String value;
    final Range range;

    Directive(final String value, final Range range) {
      this.value = value;
      this.range = range;
    }
  }

  private Directive parseDirective(final String directive) {
    if (!this.currentToken().equalsIgnoreCase(directive)) {
      return null;
    }

    Token directiveToken = this.currentToken();

    this.readToken(); // directive

    if (this.atEndOfFile()) {
      this.unexpectedTokenError("<", this.currentToken());

      return null;
    }

    // We called atEndOfFile(), which calls currentToken() to trim whitespace
    // and skip comments. Remove the resulting token.

    this.clearCurrentToken();

    String resultString = null;
    int endIndex = -1;
    final String line = this.restOfLine();
    final char firstChar = line.charAt(0);

    for (char ch : new char[] {'<', '\'', '"'}) {
      if (ch != firstChar) {
        continue;
      }

      if (ch == '<') {
        ch = '>';
      }

      endIndex = line.indexOf(ch, 1);

      if (endIndex == -1) {
        endIndex = line.indexOf(";");

        if (endIndex == -1) {
          endIndex = line.length();
        }

        resultString = line.substring(0, endIndex);
        this.currentToken = this.currentLine.makeToken(endIndex);
        this.readToken();

        this.error(this.peekPreviousToken(), "No closing " + ch + " found");

        break;
      }

      resultString = line.substring(1, endIndex);
      // +1 to include and get rid of '>', '\'' or '"' token
      this.currentToken = this.currentLine.makeToken(endIndex + 1);
      this.readToken();

      break;
    }

    if (endIndex == -1) {
      endIndex = line.indexOf(";");

      if (endIndex == -1) {
        endIndex = line.length();
      }

      resultString = line.substring(0, endIndex);
      if (endIndex > 0) {
        this.currentToken = this.currentLine.makeToken(endIndex);
        this.readToken();
      }
    }

    Directive result =
        new Directive(resultString, Parser.mergeRanges(directiveToken, this.peekPreviousToken()));

    if (this.currentToken().equals(";")) {
      this.readToken(); // read ;
    }

    return result;
  }

  private void parseScriptName() {
    Directive scriptDirective = this.parseDirective("script");
    if (this.scriptName == null && scriptDirective != null) {
      this.scriptName = scriptDirective.value;
    }
  }

  private void parseNotify() {
    Directive notifyDirective = this.parseDirective("notify");
    if (this.notifyRecipient == null && notifyDirective != null) {
      this.notifyRecipient = notifyDirective.value;
    }
  }

  private void parseSince() {
    Directive sinceDirective = this.parseDirective("since");
    if (sinceDirective != null) {
      // enforce "since" directives RIGHT NOW at parse time
      this.enforceSince(sinceDirective.value, sinceDirective.range);
    }
  }

  private Directive parseImport() {
    return this.parseDirective("import");
  }

  // **************** Tokenizer *****************

  /**
   * Returns {@link #currentToken} if non-null. Otherwise, moves in front of the next non-comment
   * token that we can find, before assigning it to {@link #currentToken} and returning it.
   *
   * <p>Never returns {@code null}.
   */
  private Token currentToken() {
    // If we've already parsed a token, return it
    if (this.currentToken != null) {
      return this.currentToken;
    }

    boolean inMultiLineComment = false;

    // Repeat until we get a token
    while (true) {
      // at "end of file"
      if (this.currentLine.content == null) {
        // will make an "end of file" token
        return this.currentToken = this.currentLine.makeToken(0);
      }

      final String restOfLine = this.restOfLine();

      if (inMultiLineComment) {
        final int commentEnd = restOfLine.indexOf("*/");

        if (commentEnd == -1) {
          if (!restOfLine.isEmpty()) {
            this.currentLine.makeComment(restOfLine.length());
          }

          this.currentLine = this.currentLine.nextLine;
          this.currentIndex = this.currentLine.offset;
        } else {
          this.currentToken = this.currentLine.makeComment(commentEnd + 2);
          this.readToken();
          inMultiLineComment = false;
        }

        continue;
      }

      if (restOfLine.length() == 0) {
        this.currentLine = this.currentLine.nextLine;
        this.currentIndex = this.currentLine.offset;
        continue;
      }

      // "#" was "supposed" to start a whole-line comment, but a bad implementation made it
      // act just like "//"

      // "//" starts a comment which consumes the rest of the line
      if (restOfLine.startsWith("#") || restOfLine.startsWith("//")) {
        this.currentLine.makeComment(restOfLine.length());

        this.currentLine = this.currentLine.nextLine;
        this.currentIndex = this.currentLine.offset;
        continue;
      }

      // "/*" starts a comment which is terminated by "*/"
      if (restOfLine.startsWith("/*")) {
        final int commentEnd = restOfLine.indexOf("*/", 2);

        if (commentEnd == -1) {
          if (!restOfLine.isEmpty()) {
            this.currentLine.makeComment(restOfLine.length());
          }

          this.currentLine = this.currentLine.nextLine;
          this.currentIndex = this.currentLine.offset;
          inMultiLineComment = true;
        } else {
          this.currentToken = this.currentLine.makeComment(commentEnd + 2);
          this.readToken();
        }

        continue;
      }

      return this.currentToken = this.currentLine.makeToken(this.tokenLength(restOfLine));
    }
  }

  /**
   * Calls {@link #currentToken()} to make sure we are currently in front of an unread token. Then,
   * returns a string version of the next token that can be found after that.
   *
   * @return the content of the next token to come after the token we are currently in front of, or
   *     {@code null} if we are at the end of the file.
   */
  private String nextToken() {
    int offset = this.currentToken().restOfLineStart;
    Line line = this.currentLine;
    boolean inMultiLineComment = false;

    while (true) {
      // at "end of file"
      if (line.content == null) {
        return null;
      }

      final String restOfLine = line.substring(offset).trim();

      if (inMultiLineComment) {
        final int commentEnd = restOfLine.indexOf("*/");

        if (commentEnd == -1) {
          line = line.nextLine;
          offset = line.offset;
        } else {
          offset += commentEnd + 2;
          inMultiLineComment = false;
        }

        continue;
      }

      // "#" was "supposed" to start a whole-line comment, but a bad implementation made it
      // act just like "//"

      if (restOfLine.length() == 0 || restOfLine.startsWith("#") || restOfLine.startsWith("//")) {
        line = line.nextLine;
        offset = line.offset;
        continue;
      }

      if (restOfLine.startsWith("/*")) {
        offset += 2;
        inMultiLineComment = true;
        continue;
      }

      return restOfLine.substring(0, this.tokenLength(restOfLine));
    }
  }

  /**
   * Forget every token up to {@code destinationToken}, so that we can resume parsing from there.
   */
  private void rewindBackTo(final Token destinationToken) {
    this.currentToken();

    while (this.currentToken != destinationToken) {
      this.currentLine.removeLastToken();

      while (!this.currentLine.hasTokens()) {
        // Don't do null checks. If previousLine is null, it means we never saw the
        // destination token, meaning we'd want to throw an error anyway.
        this.currentLine = this.currentLine.previousLine;
      }

      this.currentToken = this.currentLine.getLastToken();
      this.currentIndex = this.currentToken.getStart().getCharacter();
    }
  }

  /** Finds the last token that was *read* */
  private final Token peekPreviousToken() {
    return this.currentLine.peekPreviousToken(this.currentToken);
  }

  /** Finds the last token that was *discovered* */
  private final Token peekLastToken() {
    return this.currentLine.peekLastToken();
  }

  /**
   * If we are not at the end of the file, null out {@link #currentToken} (allowing a new one to be
   * gathered next time we call {@link #currentToken()}), and move {@link #currentIndex} forward.
   */
  private void readToken() {
    // at "end of file"
    if (this.currentToken().getLine().content == null) {
      return;
    }

    this.currentIndex = this.currentToken.restOfLineStart;
    this.currentToken = null;
  }

  /**
   * If we have an unread token saved in {@link #currentToken}, null the field, and delete it from
   * its {@link Line#tokens}, effectively forgetting that we saw it.
   *
   * <p>This method is made for parsing methods that manipulate lines character-by-character, and
   * need to create Tokens of custom lengths.
   */
  private void clearCurrentToken() {
    if (this.currentToken != null) {
      this.currentToken = null;
      this.currentLine.removeLastToken();
    }
  }

  private int tokenLength(final String s) {
    int result;
    if (s == null) {
      return 0;
    }

    for (result = 0; result < s.length(); result++) {
      if (result + 3 < s.length() && this.tokenString(s.substring(result, result + 4))) {
        return result == 0 ? 4 : result;
      }

      if (result + 2 < s.length() && this.tokenString(s.substring(result, result + 3))) {
        return result == 0 ? 3 : result;
      }

      if (result + 1 < s.length() && this.tokenString(s.substring(result, result + 2))) {
        return result == 0 ? 2 : result;
      }

      if (this.tokenChar(s.charAt(result))) {
        return result == 0 ? 1 : result;
      }
    }

    return result; // == s.length()
  }

  private boolean tokenChar(final char ch) {
    switch (ch) {
      case ' ':
      case '\t':
      case '.':
      case ',':
      case '{':
      case '}':
      case '(':
      case ')':
      case '$':
      case '!':
      case '~':
      case '+':
      case '-':
      case '=':
      case '"':
      case '`':
      case '\'':
      case '*':
      case '/':
      case '%':
      case '|':
      case '^':
      case '&':
      case '[':
      case ']':
      case ';':
      case '<':
      case '>':
      case '?':
      case ':':
      case '\u2248':
        return true;
    }
    return false;
  }

  private boolean tokenString(final String s) {
    return Parser.multiCharTokens.contains(s);
  }

  /** Returns the content of {@link #currentLine} starting at {@link #currentIndex}. */
  private String restOfLine() {
    return this.currentLine.substring(this.currentIndex);
  }

  /**
   * Calls {@link #currentToken()} in order to skip any comment or whitespace we would be in front
   * of, then return whether or not we reached the end of the file.
   */
  private boolean atEndOfFile() {
    this.currentToken();

    return this.currentLine.content == null;
  }

  private boolean madeProgress(final Position previousPosition, final Position currentPosition) {
    return previousPosition == null
        || previousPosition.getLine() < currentPosition.getLine()
        || previousPosition.getCharacter() < currentPosition.getCharacter();
  }

  public List<Token> getTokens() {
    final List<Token> result = new LinkedList<>();

    Line line = this.currentLine;

    // Go back to the start
    while (line != null && line.previousLine != null) {
      line = line.previousLine;
    }

    while (line != null && line.content != null) {
      for (final Token token : line.getTokensIterator()) {
        result.add(token);
      }

      line = line.nextLine;
    }

    return result;
  }

  private Position getCurrentPosition() {
    // 0-indexed
    int lineNumber = this.getLineNumber() - 1;
    return new Position(lineNumber, this.currentIndex);
  }

  private Range rangeToHere(final Position start) {
    return new Range(start != null ? start : this.getCurrentPosition(), this.getCurrentPosition());
  }

  private static Range makeInlineRange(final Position start, final int offset) {
    Position end = new Position(start.getLine(), start.getCharacter() + offset);

    return offset >= 0 ? new Range(start, end) : new Range(end, start);
  }

  private static Range mergeRanges(final Range start, final Range end) {
    if (end == null || Positions.isBefore(end.getEnd(), start.getStart())) {
      return start;
    }

    return new Range(start.getStart(), end.getEnd());
  }

  private Location makeZeroWidthLocation() {
    return this.makeLocation(this.getCurrentPosition());
  }

  private Location makeLocation(final Position start) {
    return this.makeLocation(this.rangeToHere(start));
  }

  private Location makeLocation(final Position start, final Position end) {
    return this.makeLocation(new Range(start, end));
  }

  private Location makeLocation(final Range start, final Range end) {
    return this.makeLocation(Parser.mergeRanges(start, end));
  }

  private Location makeLocation(final Range range) {
    return new Location(this.getStringUri(), range);
  }

  private static Location makeLocation(final Location start, final Range end) {
    return Parser.mergeLocations(start, new Location(start.getUri(), end));
  }

  public static Location mergeLocations(final Location start, final Location end) {
    if (start == null) {
      return end;
    }

    if (end == null || !start.getUri().equals(end.getUri())) {
      return start;
    }

    return new Location(start.getUri(), Parser.mergeRanges(start.getRange(), end.getRange()));
  }

  public static Location mergeLocations(final Command start, final Command end) {
    if (start == null && end == null) {
      return null;
    }

    if (start == null) {
      return end.getLocation();
    }

    if (end == null) {
      return start.getLocation();
    }

    return Parser.mergeLocations(start.getLocation(), end.getLocation());
  }

  // **************** Parse errors *****************

  public class AshDiagnostic {
    final Location location;
    final DiagnosticSeverity severity;
    final String message;
    final List<String> additionalMessages;

    private AshDiagnostic(
        final Location location,
        final DiagnosticSeverity severity,
        final String message,
        final String... additionalMessages) {
      // First, make sure that its Location corresponds to the Parser that submitted it
      if (!Parser.this.getStringUri().equals(location.getUri())) {
        throw new IllegalArgumentException();
      }

      this.location = location;
      this.severity = severity;
      this.message = message;
      this.additionalMessages = new ArrayList<>();
      for (final String additionalMessage : additionalMessages) {
        this.additionalMessages.add(additionalMessage);
      }
    }

    public String toString() {
      StringBuilder result = new StringBuilder();

      result.append(this.message);
      result.append(" (");

      result.append(Parser.getFileAndRange(Parser.this.shortFileName, this.location.getRange()));

      result.append(")");

      for (final String additionalMessage : this.additionalMessages) {
        result.append(" ");
        result.append(additionalMessage);
      }

      return result.toString();
    }
  }

  private void unexpectedTokenError(final String expected, final Token found) {
    String foundString = found.content;

    if (found.getLine().content == null) {
      foundString = "end of file";
    }

    this.error(found, "Expected " + expected + ", found " + foundString);
  }

  private void undefinedFunctionError(final Token name, final List<Evaluable> params) {
    this.error(name, Parser.undefinedFunctionMessage(name.content, params));
  }

  private void multiplyDefinedFunctionError(final Function f) {
    String buffer = "Function '" + f.getSignature() + "' defined multiple times.";
    this.error(f.getLocation(), buffer);
  }

  private void overridesLibraryFunctionError(final Function f) {
    String buffer = "Function '" + f.getSignature() + "' overrides a library function.";
    this.error(f.getLocation(), buffer);
  }

  private void varargClashError(final Function f, final Function clash) {
    String buffer =
        "Function '"
            + f.getSignature()
            + "' clashes with existing function '"
            + clash.getSignature()
            + "'.";
    this.error(f.getLocation(), buffer);
  }

  public final void sinceError(
      final String current, final String target, final Range directiveRange) {
    String template =
        "'%s' requires revision r%s of kolmafia or higher (current: r%s).  Up-to-date builds can be found at https://ci.kolmafia.us/.";

    this.error(directiveRange, String.format(template, this.shortFileName, target, current));
  }

  public static String undefinedFunctionMessage(
      final String name, final List<? extends TypedNode> params) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Function '");
    Parser.appendFunctionCall(buffer, name, params);
    buffer.append(
        "' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts.");
    return buffer.toString();
  }

  private void enforceSince(String revision, final Range directiveRange) {
    try {
      if (revision.startsWith("r")) { // revision
        revision = revision.substring(1);
        int targetRevision = Integer.parseInt(revision);
        int currentRevision = StaticEntity.getRevision();
        // A revision of zero means you're probably running in a debugger, in which
        // case you should be able to run anything.
        if (currentRevision != 0 && currentRevision < targetRevision) {
          this.sinceError(String.valueOf(currentRevision), revision, directiveRange);
          return;
        }
      } else { // version (or syntax error)
        String[] target = revision.split("\\.");
        if (target.length != 2) {
          this.error(directiveRange, "invalid 'since' format");
          return;
        }

        int targetMajor = Integer.parseInt(target[0]);
        int targetMinor = Integer.parseInt(target[1]);

        if (targetMajor > 21 || targetMajor == 21 && targetMinor > 9) {
          this.error(directiveRange, "invalid 'since' format (21.09 was the final point release)");
          return;
        }
      }
    } catch (NumberFormatException e) {
      this.error(directiveRange, "invalid 'since' format");
    }
  }

  public final void error(final String msg, final String... otherInfo) {
    this.error(this.getCurrentPosition(), msg, otherInfo);
  }

  public final void error(final Position start, final String msg, final String... otherInfo) {
    this.error(this.rangeToHere(start), msg, otherInfo);
  }

  public final void error(final Range range, final String msg, final String... otherInfo) {
    this.error(this.makeLocation(range), msg, otherInfo);
  }

  public final void error(
      final Range start, final Range end, final String msg, final String... otherInfo) {
    this.error(Parser.mergeRanges(start, end), msg, otherInfo);
  }

  public final void error(final Location location, final String msg, final String... otherInfo) {
    this.diagnostics.add(
        new AshDiagnostic(
            location != null ? location : this.makeZeroWidthLocation(),
            DiagnosticSeverity.Error,
            msg,
            otherInfo));
  }

  public final void warning(final String msg, final String... otherInfo) {
    this.warning(this.getCurrentPosition(), msg, otherInfo);
  }

  public final void warning(final Position start, final String msg, final String... otherInfo) {
    this.warning(this.rangeToHere(start), msg, otherInfo);
  }

  public final void warning(final Range range, final String msg, final String... otherInfo) {
    this.warning(this.makeLocation(range), msg, otherInfo);
  }

  public final void warning(final Location location, final String msg, final String... otherInfo) {
    this.diagnostics.add(
        new AshDiagnostic(
            location != null ? location : this.makeZeroWidthLocation(),
            DiagnosticSeverity.Warning,
            msg,
            otherInfo));
  }

  private static void appendFunctionCall(
      final StringBuilder buffer, final String name, final List<? extends TypedNode> params) {
    buffer.append(name);
    buffer.append("(");

    String sep = " ";
    for (TypedNode current : params) {
      buffer.append(sep);
      sep = ", ";
      buffer.append(current.getType());
    }

    buffer.append(" )");
  }

  public static String getLineAndFile(final String fileName, final int lineNumber) {
    if (fileName == null) {
      return "(" + Preferences.getString("commandLineNamespace") + ")";
    }

    return "(" + fileName + ", line " + lineNumber + ")";
  }

  public static final String getFileAndRange(String fileName, final Range range) {
    if (range == null || Positions.isBefore(range.getEnd(), range.getStart())) {
      throw new IllegalArgumentException();
    }

    final StringBuilder result = new StringBuilder();

    if (fileName == null) {
      String commandLineNamespace = Preferences.getString("commandLineNamespace");

      if (!commandLineNamespace.isEmpty()) {
        result.append(commandLineNamespace);
        result.append(", ");
      }

      // It's impossible to submit multiple lines from the command line, except maybe with
      // "ash cli_execute('ash \n')"
      // As such, don't display the start's line if it's '0', because it can easily be assumed.
      if (range.getStart().getLine() > 0) {
        result.append("line " + (range.getStart().getLine() + 1));
        result.append(", ");
      }
    } else {
      result.append(fileName);
      result.append(", line " + (range.getStart().getLine() + 1));
      result.append(", ");
    }

    result.append("char " + (range.getStart().getCharacter() + 1));

    if (!range.getStart().equals(range.getEnd())) {
      result.append(" to ");

      if (range.getStart().getLine() < range.getEnd().getLine()) {
        result.append("line " + (range.getEnd().getLine() + 1));
        result.append(", ");
      }

      result.append("char " + (range.getEnd().getCharacter() + 1));
    }

    return result.toString();
  }

  public static void printIndices(
      final List<Evaluable> indices, final PrintStream stream, final int indent) {
    if (indices == null) {
      return;
    }

    for (Evaluable current : indices) {
      AshRuntime.indentLine(stream, indent);
      stream.println("<KEY>");
      current.print(stream, indent + 1);
    }
  }
}
