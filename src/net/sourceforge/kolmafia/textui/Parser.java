package net.sourceforge.kolmafia.textui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.Parser.Line.Token;
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
import net.sourceforge.kolmafia.textui.parsetree.ForEachLoop;
import net.sourceforge.kolmafia.textui.parsetree.ForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Function;
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
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RepeatUntilLoop;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.ScriptExit;
import net.sourceforge.kolmafia.textui.parsetree.SortBy;
import net.sourceforge.kolmafia.textui.parsetree.StaticScope;
import net.sourceforge.kolmafia.textui.parsetree.Switch;
import net.sourceforge.kolmafia.textui.parsetree.SwitchScope;
import net.sourceforge.kolmafia.textui.parsetree.TernaryExpression;
import net.sourceforge.kolmafia.textui.parsetree.Try;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.TypeDef;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.Value.LocatedValue;
import net.sourceforge.kolmafia.textui.parsetree.VarArgType;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.textui.parsetree.WhileLoop;
import net.sourceforge.kolmafia.utilities.ByteArrayStream;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

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

      throw this.parseException(this.fileName + " could not be accessed");
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

    Token firstToken = this.currentToken();
    Scope scope =
        this.parseScope(null, null, null, Parser.getExistingFunctionScope(), false, false);

    scope.setScopeLocation(this.makeLocation(firstToken, this.peekPreviousToken()));

    if (this.currentLine.nextLine != null) {
      throw this.parseException("Script parsing error");
    }

    return scope;
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
    List<File> matches = KoLmafiaCLI.findScriptFile(fileName);
    if (matches.size() > 1) {
      StringBuilder s = new StringBuilder();
      for (File f : matches) {
        if (s.length() > 0) s.append("; ");
        s.append(f.getPath());
      }
      throw this.parseException("too many matches for " + fileName + ": " + s);
    }
    if (matches.size() == 0) {
      throw this.parseException(fileName + " could not be found");
    }

    File scriptFile = matches.get(0);

    if (this.imports.containsKey(scriptFile)) {
      return scope;
    }

    this.imports.put(scriptFile, scriptFile.lastModified());

    Parser parser = new Parser(scriptFile, null, this.imports);
    Scope result = parser.parseScope(scope, null, null, scope.getParentScope(), false, false);
    if (parser.currentLine.nextLine != null) {
      throw this.parseException("Script parsing error");
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
        throw this.parseException("command or declaration required");
      }
    } else if (this.parseVariables(t, result)) {
      if (this.currentToken().equals(";")) {
        this.readToken(); // read ;
      } else {
        throw this.parseException(";", this.currentToken());
      }
    } else {
      // Found a type but no function or variable to tie it to
      throw this.parseException("Type given but not used to declare anything");
    }

    return result;
  }

  private Scope parseScope(
      final Scope startScope,
      final Type expectedType,
      final VariableList variables,
      final BasicScope parentScope,
      final boolean allowBreak,
      final boolean allowContinue) {
    Scope result = startScope == null ? new Scope(variables, parentScope) : startScope;
    return this.parseScope(result, expectedType, parentScope, allowBreak, allowContinue);
  }

  private Scope parseScope(
      Scope result,
      final Type expectedType,
      final BasicScope parentScope,
      final boolean allowBreak,
      final boolean allowContinue) {
    String importString;

    this.parseScriptName();
    this.parseNotify();
    this.parseSince();

    while ((importString = this.parseImport()) != null) {
      result = this.importFile(importString, result);
    }

    while (true) {
      if (this.parseTypedef(result)) {
        if (this.currentToken().equals(";")) {
          this.readToken(); // read ;
        } else {
          throw this.parseException(";", this.currentToken());
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
          continue;
        }

        // No type and no command -> done.
        break;
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
            throw this.parseException("main method must appear at top level");
          }
        }

        continue;
      }

      if (this.parseVariables(t, result)) {
        if (this.currentToken().equals(";")) {
          this.readToken(); // read ;
        } else {
          throw this.parseException(";", this.currentToken());
        }

        continue;
      }

      if ((t.getBaseType() instanceof AggregateType) && this.currentToken().equals("{")) {
        LocatedValue<? extends Value> aggregate =
            this.parseAggregateLiteral(result, (AggregateType) t);

        if (aggregate != null) {
          result.addCommand(aggregate.value, this);
        }
      } else {
        // Found a type but no function or variable to tie it to
        throw this.parseException("Type given but not used to declare anything");
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
      throw this.parseException("Record name expected");
    }

    // Allow anonymous records
    String recordName = null;

    if (!this.currentToken().equals("{")) {
      // Named record
      recordName = this.currentToken().content;

      if (!this.parseIdentifier(recordName)) {
        throw this.parseException("Invalid record name '" + recordName + "'");
      } else if (Parser.isReservedWord(recordName)) {
        throw this.parseException("Reserved word '" + recordName + "' cannot be a record name");
      } else if (parentScope.findType(recordName) != null) {
        throw this.parseException("Record name '" + recordName + "' is already defined");
      }

      this.readToken(); // read name
    }

    if (this.currentToken().equals("{")) {
      this.readToken(); // read {
    } else {
      throw this.parseException("{", this.currentToken());
    }

    // Loop collecting fields
    List<Type> fieldTypes = new ArrayList<Type>();
    List<String> fieldNames = new ArrayList<String>();

    while (true) {
      if (this.atEndOfFile()) {
        throw this.parseException("}", this.currentToken());
      }

      if (this.currentToken().equals("}")) {
        if (fieldTypes.isEmpty()) {
          throw this.parseException("Record field(s) expected");
        }

        this.readToken(); // read }
        break;
      }

      // Get the field type
      Type fieldType = this.parseType(parentScope, true);
      if (fieldType == null) {
        throw this.parseException("Type name expected");
      }

      if (fieldType.getBaseType().equals(DataTypes.VOID_TYPE)) {
        throw this.parseException("Non-void field type expected");
      }

      // Get the field name
      Token fieldName = this.currentToken();
      if (fieldName.equals(";")) {
        throw this.parseException("Field name expected");
      } else if (!this.parseIdentifier(fieldName.content)) {
        throw this.parseException("Invalid field name '" + fieldName + "'");
      } else if (Parser.isReservedWord(fieldName.content)) {
        throw this.parseException(
            "Reserved word '" + fieldName + "' cannot be used as a field name");
      } else if (fieldNames.contains(fieldName.content)) {
        throw this.parseException("Field name '" + fieldName + "' is already defined");
      } else {
        this.readToken(); // read name
      }

      fieldTypes.add(fieldType);
      fieldNames.add(fieldName.content.toLowerCase());

      if (this.currentToken().equals(";")) {
        this.readToken(); // read ;
      } else {
        throw this.parseException(";", this.currentToken());
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
      throw this.parseException(
          "Reserved word '" + functionName + "' cannot be used as a function name");
    }

    this.readToken(); // read Function name
    this.readToken(); // read (

    VariableList paramList = new VariableList();
    List<VariableReference> variableReferences = new ArrayList<VariableReference>();
    boolean vararg = false;

    while (true) {
      if (this.currentToken().equals(")")) {
        this.readToken(); // read )
        break;
      }

      Type paramType = this.parseType(parentScope, false);
      if (paramType == null) {
        throw this.parseException(")", this.currentToken());
      }

      if (this.currentToken().equals("...")) {
        // Make a vararg type out of the previously parsed type.
        paramType =
            new VarArgType(paramType)
                .reference(this.makeLocation(paramType.getLocation(), this.currentToken()));

        this.readToken(); // read ...
      }

      Variable param = this.parseVariable(paramType, null);
      if (param == null) {
        throw this.parseException("identifier", this.currentToken());
      }

      if (vararg) {
        if (paramType instanceof VarArgType) {
          // We can only have a single vararg parameter
          throw this.parseException("Only one vararg parameter is allowed");
        } else {
          // The single vararg parameter must be the last one
          throw this.parseException("The vararg parameter must be the last one");
        }
      } else if (!paramList.add(param)) {
        throw this.parseException("Parameter " + param.getName() + " is already defined");
      }

      if (this.currentToken().equals("=")) {
        throw this.parseException("Cannot initialize parameter " + param.getName());
      }

      if (paramType instanceof VarArgType) {
        // Only one vararg is allowed
        vararg = true;
      }

      if (!this.currentToken().equals(")")) {
        if (this.currentToken().equals(",")) {
          this.readToken(); // read comma
        } else {
          throw this.parseException(",", this.currentToken());
        }
      }

      variableReferences.add(new VariableReference(param));
    }

    // Add the function to the parent scope before we parse the
    // function scope to allow recursion.

    Location functionLocation = this.makeLocation(functionName, this.peekLastToken());

    UserDefinedFunction f =
        new UserDefinedFunction(
            functionName.content, functionType, variableReferences, functionLocation);

    if (f.overridesLibraryFunction()) {
      throw this.overridesLibraryFunctionException(f);
    }

    UserDefinedFunction existing = parentScope.findFunction(f);

    if (existing != null && existing.getScope() != null) {
      throw this.multiplyDefinedFunctionException(f);
    }

    if (vararg) {
      Function clash = parentScope.findVarargClash(f);

      if (clash != null) {
        throw this.varargClashException(f, clash);
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
      throw this.parseException("Missing return value");
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
      VariableReference lhs = new VariableReference(v);
      LocatedValue<? extends Value> rhs;

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

      parentScope.addCommand(new Assignment(lhs, rhs == null ? null : rhs.value), this);

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
      throw this.parseException("Reserved word '" + variableName + "' cannot be a variable name");
    } else if (scope != null && scope.findVariable(variableName.content) != null) {
      throw this.parseException("Variable " + variableName + " is already defined");
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
  private LocatedValue<? extends Value> parseInitialization(
      final VariableReference lhs, final BasicScope scope) {
    LocatedValue<? extends Value> result;

    Type t = lhs.target.getType();
    Type ltype = t.getBaseType();
    if (this.currentToken().equals("{")) {
      if (ltype instanceof AggregateType) {
        result = this.parseAggregateLiteral(scope, (AggregateType) ltype);
      } else {
        throw this.parseException(
            "Cannot initialize " + lhs + " of type " + t + " with an aggregate literal");
      }
    } else {
      result = this.parseExpression(scope);

      if (result != null) {
        result = this.autoCoerceValue(t, result, scope);
        if (!Operator.validCoercion(ltype, result.value.getType(), "assign")) {
          throw this.parseException(
              "Cannot store " + result.value.getType() + " in " + lhs + " of type " + ltype);
        }
      } else {
        throw this.parseException("Expression expected");
      }
    }

    return result;
  }

  private LocatedValue<? extends Value> autoCoerceValue(
      Type ltype, final LocatedValue<? extends Value> rhs, final BasicScope scope) {
    // DataTypes.TYPE_ANY has no name
    if (ltype == null || ltype.getName() == null) {
      return rhs;
    }

    // If the types are the same no coercion needed
    // A TypeDef or a RecordType match names for equal.
    Type rtype = rhs.value.getRawType();
    if (ltype.equals(rtype)) {
      return rhs;
    }

    // Look for a function:  LTYPE to_LTYPE( RTYPE )
    String name = "to_" + ltype.getName();
    List<Value> params = Collections.singletonList(rhs.value);

    // A typedef can overload a coercion function to a basic type or a typedef
    if (ltype instanceof TypeDef || ltype instanceof RecordType) {
      Function target = scope.findFunction(name, params, MatchType.EXACT);
      if (target != null && target.getType().equals(ltype)) {
        return Value.wrap(new FunctionCall(target, params, this), rhs.location);
      }
    }

    if (ltype instanceof AggregateType) {
      return rhs;
    }

    if (rtype instanceof TypeDef || rtype instanceof RecordType) {
      Function target = scope.findFunction(name, params, MatchType.EXACT);
      if (target != null && target.getType().equals(ltype)) {
        return Value.wrap(new FunctionCall(target, params, this), rhs.location);
      }
    }

    // No overloaded coercions found for typedefs or records
    return rhs;
  }

  private List<LocatedValue<? extends Value>> autoCoerceParameters(
      final Function target,
      final List<LocatedValue<? extends Value>> params,
      final BasicScope scope) {
    ListIterator<VariableReference> refIterator = target.getVariableReferences().listIterator();
    ListIterator<LocatedValue<? extends Value>> valIterator = params.listIterator();
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

      LocatedValue<? extends Value> currentValue = valIterator.next();
      LocatedValue<? extends Value> coercedValue =
          this.autoCoerceValue(paramType, currentValue, scope);
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
      throw this.parseException("Missing data type for typedef");
    }

    Token typeName = this.currentToken();

    if (typeName.equals(";")) {
      throw this.parseException("Type name expected");
    } else if (!this.parseIdentifier(typeName.content)) {
      throw this.parseException("Invalid type name '" + typeName + "'");
    } else if (Parser.isReservedWord(typeName.content)) {
      throw this.parseException("Reserved word '" + typeName + "' cannot be a type name");
    } else {
      this.readToken(); // read name
    }

    Type existingType = parentScope.findType(typeName.content);
    if (existingType != null) {
      if (existingType.getBaseType().equals(t)) {
        // It is OK to redefine a typedef with an equivalent type
        return true;
      }

      throw this.parseException("Type name '" + typeName + "' is already defined");
    } else {
      // Add the type to the type table
      TypeDef type =
          new TypeDef(
              typeName.content, t, this.makeLocation(typedefToken, this.peekPreviousToken()));
      parentScope.addType(type);
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
        throw this.parseException("Encountered 'break' outside of loop");
      }

      this.readToken(); // break
    } else if (this.currentToken().equalsIgnoreCase("continue")) {
      if (allowContinue) {
        result = new LoopContinue(this.makeLocation(this.currentToken()));
      } else {
        throw this.parseException("Encountered 'continue' outside of loop");
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
    } else {
      LocatedValue<? extends Value> value = this.parseValue(scope);

      if (value == null) {
        return null;
      }

      result = value.value;
    }

    if (this.currentToken().equals(";")) {
      this.readToken(); // ;
    } else {
      throw this.parseException(";", this.currentToken());
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
        throw this.parseException("Existing type expected for function parameter");
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
  private LocatedValue<? extends Value> parseAggregateLiteral(
      final BasicScope scope, final AggregateType aggr) {
    Token aggregateLiteralStartToken = this.currentToken();

    this.readToken(); // read {

    Type index = aggr.getIndexType();
    Type data = aggr.getDataType();

    List<Value> keys = new ArrayList<>();
    List<Value> values = new ArrayList<>();

    // If index type is an int, it could be an array or a map
    boolean arrayAllowed = index.equals(DataTypes.INT_TYPE);

    // Assume it is a map.
    boolean isArray = false;

    while (true) {
      if (this.atEndOfFile()) {
        throw this.parseException("}", this.currentToken());
      }

      if (this.currentToken().equals("}")) {
        this.readToken(); // read }
        break;
      }

      LocatedValue<? extends Value> lhs;

      // If we know we are reading an ArrayLiteral or haven't
      // yet ensured we are reading a MapLiteral, allow any
      // type of Value as the "key"
      Type dataType = data.getBaseType();
      if ((isArray || arrayAllowed)
          && this.currentToken().equals("{")
          && dataType instanceof AggregateType) {
        lhs = this.parseAggregateLiteral(scope, (AggregateType) dataType);
      } else {
        lhs = this.parseExpression(scope);
      }

      if (lhs == null) {
        throw this.parseException("Script parsing error");
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
          if (!Operator.validCoercion(dataType, lhs.value.getType(), "assign")) {
            throw this.parseException("Invalid array literal");
          }

          values.add(lhs.value);
        } else {
          throw this.parseException(":", delim);
        }

        // Move on to the next value
        if (delim.equals(",")) {
          this.readToken(); // read ,
        } else if (!delim.equals("}")) {
          throw this.parseException("}", delim);
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
          throw this.parseException("Cannot include keys when making an array literal");
        } else {
          // If not, we can't tell why there's a colon here.
          throw this.parseException(", or }", delim);
        }
      }

      LocatedValue<? extends Value> rhs;

      if (this.currentToken().equals("{") && dataType instanceof AggregateType) {
        rhs = this.parseAggregateLiteral(scope, (AggregateType) dataType);
      } else {
        rhs = this.parseExpression(scope);
      }

      if (rhs == null) {
        throw this.parseException("Script parsing error");
      }

      // Check that each type is valid via validCoercion
      lhs = this.autoCoerceValue(index, lhs, scope);
      rhs = this.autoCoerceValue(data, rhs, scope);
      if (!Operator.validCoercion(index, lhs.value.getType(), "assign")
          || !Operator.validCoercion(data, rhs.value.getType(), "assign")) {
        throw this.parseException("Invalid map literal");
      }

      keys.add(lhs.value);
      values.add(rhs.value);

      // Move on to the next value
      if (this.currentToken().equals(",")) {
        this.readToken(); // read ,
      } else if (!this.currentToken().equals("}")) {
        throw this.parseException("}", this.currentToken());
      }
    }

    Location aggregateLiteralLocation =
        this.makeLocation(aggregateLiteralStartToken, this.peekPreviousToken());

    if (isArray) {
      int size = aggr.getSize();
      if (size > 0 && size < values.size()) {
        throw this.parseException(
            "Array has " + size + " elements but " + values.size() + " initializers.");
      }
    }

    Value result = isArray ? new ArrayLiteral(aggr, values) : new MapLiteral(aggr, keys, values);

    return Value.wrap(result, aggregateLiteralLocation);
  }

  private Type parseAggregateType(Type dataType, final BasicScope scope) {
    Token separatorToken = this.currentToken();

    this.readToken(); // [ or ,

    Type indexType = null;
    int size = 0;

    Token indexToken = this.currentToken();

    if (indexToken.equals("]")) {
      if (!separatorToken.equals("[")) {
        throw this.parseException("Missing index token");
      }
    } else if (this.readIntegerToken(indexToken.content)) {
      size = StringUtilities.parseInt(indexToken.content);
      this.readToken(); // integer
    } else if (this.parseIdentifier(indexToken.content)) {
      indexType = scope.findType(indexToken.content);

      if (indexType != null) {
        indexType = indexType.reference(this.makeLocation(indexToken));

        if (!indexType.isPrimitive()) {
          throw this.parseException(
              "Index type '" + this.currentToken() + "' is not a primitive type");
        }
      } else {
        throw this.parseException("Invalid type name '" + this.currentToken() + "'");
      }

      this.readToken(); // type name
    } else {
      throw this.parseException("Missing index token");
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
      throw this.parseException(", or ]", this.currentToken());
    }

    Type type =
        indexType != null
            ? new AggregateType(dataType, indexType)
            : new AggregateType(dataType, size);

    return type.reference(this.makeLocation(dataType.getLocation(), this.peekPreviousToken()));
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
      throw this.parseException("Cannot return when outside of a function");
    }

    if (this.currentToken().equals(";")) {
      if (expectedType != null && !expectedType.equals(DataTypes.TYPE_VOID)) {
        throw this.parseException("Return needs " + expectedType + " value");
      }

      return new FunctionReturn(this.makeLocation(returnStartToken), null, DataTypes.VOID_TYPE);
    }

    if (expectedType != null && expectedType.equals(DataTypes.TYPE_VOID)) {
      throw this.parseException("Cannot return a value from a void function");
    }

    LocatedValue<? extends Value> value = this.parseExpression(parentScope);

    if (value != null) {
      value = this.autoCoerceValue(expectedType, value, parentScope);
    } else {
      throw this.parseException("Expression expected");
    }

    if (expectedType != null
        && !Operator.validCoercion(expectedType, value.value.getType(), "return")) {
      throw this.parseException(
          "Cannot return " + value.value.getType() + " value from " + expectedType + " function");
    }

    Location returnLocation = this.makeLocation(returnStartToken, this.peekPreviousToken());
    return new FunctionReturn(returnLocation, value.value, expectedType);
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
        throw this.parseException(";", this.currentToken());
      }
    }

    result.setScopeLocation(this.makeLocation(scopeStartToken, this.peekPreviousToken()));

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

    Scope scope =
        this.parseScope(null, functionType, variables, parentScope, allowBreak, allowContinue);

    if (this.currentToken().equals("}")) {
      this.readToken(); // read }
    } else {
      throw this.parseException("}", this.currentToken());
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
      throw this.parseException("(", this.currentToken());
    }

    LocatedValue<? extends Value> condition = this.parseExpression(parentScope);

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      throw this.parseException(")", this.currentToken());
    }

    if (condition == null || !condition.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      throw this.parseException("\"if\" requires a boolean conditional expression");
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

      if (result == null) {
        result = new If(conditionalLocation, scope, condition.value);
      } else if (finalElse) {
        result.addElseLoop(new Else(conditionalLocation, scope, condition.value));
      } else {
        result.addElseLoop(new ElseIf(conditionalLocation, scope, condition.value));
      }

      if (!noElse && this.currentToken().equalsIgnoreCase("else")) {
        conditionalStartToken = this.currentToken();

        if (finalElse) {
          throw this.parseException("Else without if");
        }

        this.readToken(); // else
        if (this.currentToken().equalsIgnoreCase("if")) {
          this.readToken(); // if

          if (this.currentToken().equals("(")) {
            this.readToken(); // (
          } else {
            throw this.parseException("(", this.currentToken());
          }

          condition = this.parseExpression(parentScope);

          if (this.currentToken().equals(")")) {
            this.readToken(); // )
          } else {
            throw this.parseException(")", this.currentToken());
          }

          if (condition == null || !condition.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
            throw this.parseException("\"if\" requires a boolean conditional expression");
          }
        } else
        // else without condition
        {
          condition = Value.wrap(DataTypes.TRUE_VALUE, this.makeZeroWidthLocation());
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
        throw this.parseException("}", this.currentToken());
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
      throw this.parseException("(", this.currentToken());
    }

    LocatedValue<? extends Value> condition = this.parseExpression(parentScope);

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      throw this.parseException(")", this.currentToken());
    }

    if (condition == null || !condition.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      throw this.parseException("\"while\" requires a boolean conditional expression");
    }

    Scope scope = this.parseLoopScope(functionType, null, parentScope);

    Location whileLocation = this.makeLocation(whileStartToken, this.peekPreviousToken());
    return new WhileLoop(whileLocation, scope, condition.value);
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
      throw this.parseException("until", this.currentToken());
    }

    if (this.currentToken().equals("(")) {
      this.readToken(); // (
    } else {
      throw this.parseException("(", this.currentToken());
    }

    LocatedValue<? extends Value> condition = this.parseExpression(parentScope);

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      throw this.parseException(")", this.currentToken());
    }

    if (condition == null || !condition.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      throw this.parseException("\"repeat\" requires a boolean conditional expression");
    }

    Location repeatLocation = this.makeLocation(repeatStartToken, this.peekPreviousToken());
    return new RepeatUntilLoop(repeatLocation, scope, condition.value);
  }

  private Switch parseSwitch(
      final Type functionType, final BasicScope parentScope, final boolean allowContinue) {
    if (!this.currentToken().equalsIgnoreCase("switch")) {
      return null;
    }

    Token switchStartToken = this.currentToken();

    this.readToken(); // switch

    if (!this.currentToken().equals("(") && !this.currentToken().equals("{")) {
      throw this.parseException("( or {", this.currentToken());
    }

    LocatedValue<? extends Value> condition =
        Value.wrap(DataTypes.TRUE_VALUE, this.makeZeroWidthLocation());
    if (this.currentToken().equals("(")) {
      this.readToken(); // (

      condition = this.parseExpression(parentScope);

      if (this.currentToken().equals(")")) {
        this.readToken(); // )
      } else {
        throw this.parseException(")", this.currentToken());
      }

      if (condition == null) {
        throw this.parseException("\"switch ()\" requires an expression");
      }
    }

    Type type = condition.value.getType();

    Token switchScopeStartToken = this.currentToken();

    if (this.currentToken().equals("{")) {
      this.readToken(); // {
    } else {
      throw this.parseException("{", this.currentToken());
    }

    List<Value> tests = new ArrayList<>();
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

        LocatedValue<? extends Value> test = this.parseExpression(parentScope);

        if (test == null) {
          throw this.parseException("Case label needs to be followed by an expression");
        }

        if (this.currentToken().equals(":")) {
          this.readToken(); // :
        } else {
          throw this.parseException(":", this.currentToken());
        }

        if (!test.value.getType().equals(type)) {
          throw this.parseException(
              "Switch conditional has type "
                  + type
                  + " but label expression has type "
                  + test.value.getType());
        }

        if (currentInteger == null) {
          currentInteger = IntegerPool.get(currentIndex);
        }

        if (test.value.getClass() == Value.class) {
          if (labels.get(test.value) != null) {
            throw this.parseException("Duplicate case label: " + test);
          } else {
            labels.put(test.value, currentInteger);
          }
        } else {
          constantLabels = false;
        }

        tests.add(test.value);
        indices.add(currentInteger);
        scope.resetBarrier();

        continue;
      }

      if (this.currentToken().equalsIgnoreCase("default")) {
        this.readToken(); // default

        if (this.currentToken().equals(":")) {
          this.readToken(); // :
        } else {
          throw this.parseException(":", this.currentToken());
        }

        if (defaultIndex == -1) {
          defaultIndex = currentIndex;
        } else {
          throw this.parseException("Only one default label allowed in a switch statement");
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
        throw this.parseException("Type given but not used to declare anything");
      }

      if (this.currentToken().equals(";")) {
        this.readToken(); // read ;
      } else {
        throw this.parseException(";", this.currentToken());
      }

      currentIndex = scope.commandCount();
      currentInteger = null;
    }

    if (this.currentToken().equals("}")) {
      this.readToken(); // }
    } else {
      throw this.parseException("}", this.currentToken());
    }

    Location switchScopeLocation =
        this.makeLocation(switchScopeStartToken, this.peekPreviousToken());
    scope.setScopeLocation(switchScopeLocation);

    Location switchLocation = this.makeLocation(switchStartToken, this.peekPreviousToken());
    return new Switch(
        switchLocation,
        condition.value,
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
      throw this.parseException("\"try\" without \"finally\" is pointless");
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

    this.readToken(); // catch

    Scope body =
        this.parseBlockOrSingleCommand(
            functionType, null, parentScope, false, allowBreak, allowContinue);

    return new Catch(body);
  }

  private LocatedValue<? extends Catch> parseCatchValue(final BasicScope parentScope) {
    if (!this.currentToken().equalsIgnoreCase("catch")) {
      return null;
    }

    Token catchStartToken = this.currentToken();

    this.readToken(); // catch

    Command body = this.parseBlock(null, null, parentScope, true, false, false);
    if (body == null) {
      LocatedValue<? extends Value> value = this.parseExpression(parentScope);
      if (value != null) {
        body = value.value;
      } else {
        throw this.parseException("\"catch\" requires a block or an expression");
      }
    }

    return Value.wrap(
        new Catch(body), this.makeLocation(catchStartToken, this.peekPreviousToken()));
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

      this.parseScope(result, functionType, parentScope, false, false);

      if (this.currentToken().equals("}")) {
        this.readToken(); // read }
      } else {
        throw this.parseException("}", this.currentToken());
      }
    } else // body is a single call
    {
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
    LocatedValue<? extends Value> aggregate = this.parseVariableReference(parentScope);

    if (aggregate == null
        || !(aggregate.value instanceof VariableReference)
        || !(aggregate.value.getType().getBaseType() instanceof AggregateType)) {
      throw this.parseException("Aggregate reference expected");
    }

    if (this.currentToken().equalsIgnoreCase("by")) {
      this.readToken(); // by
    } else {
      throw this.parseException("by", this.currentToken());
    }

    // Define key variables of appropriate type
    VariableList varList = new VariableList();
    AggregateType type = (AggregateType) aggregate.value.getType().getBaseType();
    Variable valuevar = new Variable("value", type.getDataType(), this.makeZeroWidthLocation());
    varList.add(valuevar);
    Variable indexvar = new Variable("index", type.getIndexType(), this.makeZeroWidthLocation());
    varList.add(indexvar);

    // Parse the key expression in a new scope containing 'index' and 'value'
    Scope scope = new Scope(varList, parentScope);
    LocatedValue<? extends Value> expr = this.parseExpression(scope);

    if (expr == null) {
      throw this.parseException("Expression expected");
    }

    Location sortLocation = this.makeLocation(sortStartToken, this.peekPreviousToken());
    return new SortBy(
        sortLocation, (VariableReference) aggregate.value, indexvar, valuevar, expr.value, this);
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
        throw this.parseException("Key variable name expected");
      } else if (Parser.isReservedWord(name.content)) {
        throw this.parseException("Reserved word '" + name + "' cannot be a key variable name");
      } else if (names.contains(name.content)) {
        throw this.parseException("Key variable '" + name + "' is already defined");
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

      throw this.parseException("in", this.currentToken());
    }

    // Get an aggregate reference
    LocatedValue<? extends Value> aggregate = this.parseValue(parentScope);

    if (aggregate == null || !(aggregate.value.getType().getBaseType() instanceof AggregateType)) {
      throw this.parseException("Aggregate reference expected");
    }

    // Define key variables of appropriate type
    VariableList varList = new VariableList();
    List<VariableReference> variableReferences = new ArrayList<>();
    Type type = aggregate.value.getType().getBaseType();

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      Location location = locations.get(i);

      Type itype;
      if (type == null) {
        throw this.parseException("Too many key variables specified");
      }

      if (type instanceof AggregateType) {
        itype = ((AggregateType) type).getIndexType();
        type = ((AggregateType) type).getDataType();
      } else { // Variable after all key vars holds the value instead
        itype = type;
        type = null;
      }

      Variable keyvar = new Variable(name, itype, location);
      varList.add(keyvar);
      variableReferences.add(new VariableReference(keyvar));
    }

    // Parse the scope with the list of keyVars
    Scope scope = this.parseLoopScope(functionType, varList, parentScope);

    Location foreachLocation = this.makeLocation(foreachStartToken, this.peekPreviousToken());

    // Add the foreach node with the list of varRefs
    return new ForEachLoop(foreachLocation, scope, variableReferences, aggregate.value, this);
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

    if (Parser.isReservedWord(name.content)) {
      throw this.parseException("Reserved word '" + name + "' cannot be an index variable name");
    } else if (parentScope.findVariable(name.content) != null) {
      throw this.parseException("Index variable '" + name + "' is already defined");
    }

    this.readToken(); // name

    if (this.currentToken().equalsIgnoreCase("from")) {
      this.readToken(); // from
    } else {
      throw this.parseException("from", this.currentToken());
    }

    LocatedValue<? extends Value> initial = this.parseExpression(parentScope);

    if (initial == null) {
      throw this.parseException("Expression for initial value expected");
    }

    int direction = 0;

    if (this.currentToken().equalsIgnoreCase("upto")) {
      direction = 1;
    } else if (this.currentToken().equalsIgnoreCase("downto")) {
      direction = -1;
    } else if (this.currentToken().equalsIgnoreCase("to")) {
      direction = 0;
    } else {
      throw this.parseException("to, upto, or downto", this.currentToken());
    }

    this.readToken(); // upto/downto/to

    LocatedValue<? extends Value> last = this.parseExpression(parentScope);

    if (last == null) {
      throw this.parseException("Expression for floor/ceiling value expected");
    }

    LocatedValue<? extends Value> increment = Value.wrap(DataTypes.ONE_VALUE, last.location);
    if (this.currentToken().equalsIgnoreCase("by")) {
      this.readToken(); // by
      increment = this.parseExpression(parentScope);

      if (increment == null) {
        throw this.parseException("Expression for increment value expected");
      }
    }

    // Create integer index variable
    Variable indexvar = new Variable(name.content, DataTypes.INT_TYPE, this.makeLocation(name));

    // Put index variable onto a list
    VariableList varList = new VariableList();
    varList.add(indexvar);

    Scope scope = this.parseLoopScope(functionType, varList, parentScope);

    Location forLocation = this.makeLocation(forStartToken, this.peekPreviousToken());
    return new ForLoop(
        forLocation,
        scope,
        new VariableReference(indexvar),
        initial.value,
        last.value,
        increment.value,
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

    Token loopScopeStartToken = this.currentToken();

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
        throw this.parseException("Identifier required");
      }

      // If there is no data type, it is using an existing variable
      if (t == null) {
        variable = parentScope.findVariable(name.content);
        if (variable == null) {
          throw this.parseException("Unknown variable '" + name + "'");
        }
        t = variable.getType();
      } else {
        if (scope.findVariable(name.content, true) != null) {
          throw this.parseException("Variable '" + name + "' already defined");
        }

        // Create variable and add it to the scope
        variable = new Variable(name.content, t, this.makeLocation(name));
        scope.addVariable(variable);
      }

      this.readToken(); // name

      VariableReference lhs = new VariableReference(variable);
      LocatedValue<? extends Value> rhs = null;

      if (this.currentToken().equals("=")) {
        this.readToken(); // =

        rhs = this.parseExpression(scope);

        if (rhs == null) {
          throw this.parseException("Expression expected");
        }

        Type ltype = t.getBaseType();
        rhs = this.autoCoerceValue(t, rhs, scope);
        Type rtype = rhs.value.getType();

        if (!Operator.validCoercion(ltype, rtype, "assign")) {
          throw this.parseException("Cannot store " + rtype + " in " + name + " of type " + ltype);
        }
      }

      Assignment initializer = new Assignment(lhs, rhs == null ? null : rhs.value);

      initializers.add(initializer);

      if (this.currentToken().equals(",")) {
        this.readToken(); // ,

        if (this.currentToken().equals(";")) {
          throw this.parseException("Identifier expected");
        }
      }
    }

    if (this.currentToken().equals(";")) {
      this.readToken(); // ;
    } else {
      throw this.parseException(";", this.currentToken());
    }

    // Parse condition in context of scope

    LocatedValue<? extends Value> condition =
        this.currentToken().equals(";")
            ? Value.wrap(DataTypes.TRUE_VALUE, this.makeZeroWidthLocation())
            : this.parseExpression(scope);

    if (this.currentToken().equals(";")) {
      this.readToken(); // ;
    } else {
      throw this.parseException(";", this.currentToken());
    }

    if (condition == null || !condition.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      throw this.parseException("\"for\" requires a boolean conditional expression");
    }

    // Parse incrementers in context of scope

    List<Command> incrementers = new ArrayList<>();

    while (!this.atEndOfFile() && !this.currentToken().equals(")")) {
      LocatedValue<? extends Value> value = this.parsePreIncDec(scope);
      if (value != null) {
        incrementers.add(value.value);
      } else {
        value = this.parseVariableReference(scope);
        if (value == null || !(value.value instanceof VariableReference)) {
          throw this.parseException("Variable reference expected");
        }

        @SuppressWarnings("unchecked")
        LocatedValue<? extends VariableReference> ref =
            (LocatedValue<? extends VariableReference>) value;
        LocatedValue<? extends Value> lhs = this.parsePostIncDec(ref);

        if (lhs == ref) {
          LocatedValue<? extends Assignment> incrementer = this.parseAssignment(scope, ref);

          if (incrementer != null) {
            incrementers.add(incrementer.value);
          } else {
            throw this.parseException("Variable '" + ref.value.getName() + "' not incremented");
          }
        } else {
          incrementers.add(lhs.value);
        }
      }

      if (this.currentToken().equals(",")) {
        this.readToken(); // ,

        if (this.atEndOfFile() || this.currentToken().equals(")")) {
          throw this.parseException("Identifier expected");
        }
      }
    }

    if (this.currentToken().equals(")")) {
      this.readToken(); // )
    } else {
      throw this.parseException(")", this.currentToken());
    }

    // Parse scope body
    this.parseLoopScope(scope, functionType, parentScope);

    Location loopScopeLocation = this.makeLocation(loopScopeStartToken, this.peekPreviousToken());
    scope.setScopeLocation(loopScopeLocation);

    Location javaForLocation = this.makeLocation(javaForStartToken, this.peekPreviousToken());
    return new JavaForLoop(javaForLocation, scope, initializers, condition.value, incrementers);
  }

  private Scope parseLoopScope(
      final Type functionType, final VariableList varList, final BasicScope parentScope) {
    Scope result = new Scope(varList, parentScope);

    Token loopScopeStartToken = this.currentToken();

    this.parseLoopScope(result, functionType, parentScope);

    Location loopScopeLocation = this.makeLocation(loopScopeStartToken, this.peekPreviousToken());
    result.setScopeLocation(loopScopeLocation);

    return result;
  }

  private Scope parseLoopScope(
      final Scope result, final Type functionType, final BasicScope parentScope) {
    if (this.currentToken().equals("{")) {
      // Scope is a block

      this.readToken(); // {

      this.parseScope(result, functionType, parentScope, true, true);

      if (this.currentToken().equals("}")) {
        this.readToken(); // }
      } else {
        throw this.parseException("}", this.currentToken());
      }
    } else {
      // Scope is a single command
      Command command = this.parseCommand(functionType, result, false, true, true);
      if (command == null) {
        if (this.currentToken().equals(";")) {
          this.readToken(); // ;
        } else {
          throw this.parseException(";", this.currentToken());
        }
      } else {
        result.addCommand(command, this);
      }
    }

    return result;
  }

  private LocatedValue<? extends Value> parseNewRecord(final BasicScope scope) {
    if (!this.currentToken().equalsIgnoreCase("new")) {
      return null;
    }

    Token newRecordStartToken = this.currentToken();

    this.readToken();

    if (!this.parseIdentifier(this.currentToken().content)) {
      throw this.parseException("Record name", this.currentToken());
    }

    String name = this.currentToken().content;
    Type type = scope.findType(name);

    if (!(type instanceof RecordType)) {
      throw this.parseException("'" + name + "' is not a record type");
    }

    RecordType target = (RecordType) type;

    this.readToken(); // name

    List<Value> params = new ArrayList<>();
    String[] names = target.getFieldNames();
    Type[] types = target.getFieldTypes();
    int param = 0;

    if (this.currentToken().equals("(")) {
      this.readToken(); // (

      while (true) {
        if (this.atEndOfFile()) {
          throw this.parseException(")", this.currentToken());
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
          throw this.parseException("Too many field initializers for record " + name);
        }

        Type expected = currentType.getBaseType();
        LocatedValue<? extends Value> val;

        if (this.currentToken().equals(",")) {
          val = Value.wrap(DataTypes.VOID_VALUE, this.makeZeroWidthLocation());
        } else if (this.currentToken().equals("{") && expected instanceof AggregateType) {
          val = this.parseAggregateLiteral(scope, (AggregateType) expected);
        } else {
          val = this.parseExpression(scope);
        }

        if (val == null) {
          throw this.parseException(
              "Expression expected for field #" + (param + 1) + errorMessageFieldName);
        }

        if (val.value != DataTypes.VOID_VALUE) {
          val = this.autoCoerceValue(currentType, val, scope);
          Type given = val.value.getType();
          if (!Operator.validCoercion(expected, given, "assign")) {
            throw this.parseException(
                given
                    + " found when "
                    + expected
                    + " expected for field #"
                    + (param + 1)
                    + errorMessageFieldName);
          }
        }

        params.add(val.value);
        param++;

        if (this.currentToken().equals(",")) {
          this.readToken(); // ,
        } else if (!this.currentToken().equals(")")) {
          throw this.parseException(", or )", this.currentToken());
        }
      }
    }

    Location newRecordLocation = this.makeLocation(newRecordStartToken, this.peekPreviousToken());
    return Value.wrap(target.initialValueExpression(params), newRecordLocation);
  }

  private LocatedValue<? extends Value> parseCall(final BasicScope scope) {
    return this.parseCall(scope, null);
  }

  private LocatedValue<? extends Value> parseCall(
      final BasicScope scope, final LocatedValue<? extends Value> firstParam) {
    if (!"(".equals(this.nextToken())) {
      return null;
    }

    if (!this.parseScopedIdentifier(this.currentToken().content)) {
      return null;
    }

    Token name = this.currentToken();
    this.readToken(); // name

    List<LocatedValue<? extends Value>> params = this.parseParameters(scope, firstParam);
    Function target =
        scope.findFunction(
            name.content, params.stream().map(param -> param.value).collect(Collectors.toList()));

    Location functionCallLocation = this.makeLocation(name, this.peekPreviousToken());

    if (target != null) {
      params = this.autoCoerceParameters(target, params, scope);
    } else {
      throw this.undefinedFunctionException(name.content, params);
    }

    // Include the first parameter, if any, in the FunctionCall's location
    if (firstParam != null) {
      functionCallLocation = this.makeLocation(firstParam.location, functionCallLocation);
    }

    FunctionCall call =
        new FunctionCall(
            target, params.stream().map(param -> param.value).collect(Collectors.toList()), this);
    return this.parsePostCall(scope, Value.wrap(call, functionCallLocation));
  }

  private List<LocatedValue<? extends Value>> parseParameters(
      final BasicScope scope, final LocatedValue<? extends Value> firstParam) {
    if (!this.currentToken().equals("(")) {
      return null;
    }

    this.readToken(); // (

    List<LocatedValue<? extends Value>> params = new ArrayList<>();
    if (firstParam != null) {
      params.add(firstParam);
    }

    while (true) {
      if (this.atEndOfFile()) {
        throw this.parseException(")", this.currentToken());
      }

      if (this.currentToken().equals(")")) {
        this.readToken(); // )
        break;
      }

      LocatedValue<? extends Value> val = this.parseExpression(scope);
      if (val != null) {
        params.add(val);
      }

      if (this.atEndOfFile()) {
        throw this.parseException(")", this.currentToken());
      }

      if (!this.currentToken().equals(",")) {
        if (!this.currentToken().equals(")")) {
          throw this.parseException(")", this.currentToken());
        }
        continue;
      }

      this.readToken(); // ,

      if (this.atEndOfFile()) {
        throw this.parseException("parameter", this.currentToken());
      }

      if (this.currentToken().equals(")")) {
        throw this.parseException("parameter", this.currentToken());
      }
    }

    return params;
  }

  private LocatedValue<? extends Value> parsePostCall(
      final BasicScope scope, LocatedValue<? extends FunctionCall> call) {
    LocatedValue<? extends Value> result = call;
    while (result != null && this.currentToken().equals(".")) {
      Variable current = new Variable(result.value.getType());
      current.setExpression(result.value);

      result =
          this.parseVariableReference(
              scope, Value.wrap(new VariableReference(current), result.location));
    }

    return result;
  }

  private LocatedValue<? extends Value> parseInvoke(final BasicScope scope) {
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
    LocatedValue<? extends Value> name = null;

    if (current.equals("(")) {
      name = this.parseExpression(scope);
      if (name == null || !name.value.getType().equals(DataTypes.STRING_TYPE)) {
        throw this.parseException("String expression expected for function name");
      }
    } else {
      name = this.parseVariableReference(scope);

      if (name == null || !(name.value instanceof VariableReference)) {
        throw this.parseException("Variable reference expected for function name");
      }
    }

    List<LocatedValue<? extends Value>> params;

    if (this.currentToken().equals("(")) {
      params = this.parseParameters(scope, null);
    } else {
      throw this.parseException("(", this.currentToken());
    }

    FunctionInvocation call =
        new FunctionInvocation(
            scope,
            type,
            name.value,
            params.stream().map(param -> param.value).collect(Collectors.toList()),
            this);
    return this.parsePostCall(
        scope, Value.wrap(call, this.makeLocation(invokeStartToken, this.peekPreviousToken())));
  }

  private LocatedValue<? extends Assignment> parseAssignment(
      final BasicScope scope, final LocatedValue<? extends VariableReference> lhs) {
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

    Type ltype = lhs.value.getType().getBaseType();
    boolean isAggregate = (ltype instanceof AggregateType);

    if (isAggregate && !operStr.equals("=")) {
      throw this.parseException("Cannot use '" + operStr + "' on an aggregate");
    }

    Operator oper = new Operator(this.makeLocation(operStr), operStr.content, this);
    this.readToken(); // oper

    LocatedValue<? extends Value> rhs;

    if (this.currentToken().equals("{")) {
      if (isAggregate) {
        rhs = this.parseAggregateLiteral(scope, (AggregateType) ltype);
      } else {
        throw this.parseException(
            "Cannot use an aggregate literal for type " + lhs.value.getType());
      }
    } else {
      rhs = this.parseExpression(scope);
    }

    if (rhs == null) {
      throw this.parseException("Expression expected");
    }

    rhs = this.autoCoerceValue(lhs.value.getRawType(), rhs, scope);
    if (!oper.validCoercion(lhs.value.getType(), rhs.value.getType())) {
      String error =
          oper.isLogical()
              ? (oper
                  + " requires an integer or boolean expression and an integer or boolean variable reference")
              : oper.isInteger()
                  ? (oper + " requires an integer expression and an integer variable reference")
                  : ("Cannot store "
                      + rhs.value.getType()
                      + " in "
                      + lhs.value
                      + " of type "
                      + lhs.value.getType());
      throw this.parseException(error);
    }

    Operator op = null;

    if (!operStr.equals("=")) {
      op =
          new Operator(
              this.makeLocation(this.makeInlineRange(operStr.getStart(), operStr.length() - 1)),
              operStr.substring(0, operStr.length() - 1),
              this);
    }

    return Value.wrap(
        new Assignment(lhs.value, rhs.value, op), this.makeLocation(lhs.location, rhs.location));
  }

  private Value parseRemove(final BasicScope scope) {
    if (!this.currentToken().equalsIgnoreCase("remove")) {
      return null;
    }

    LocatedValue<? extends Value> lhs = this.parseExpression(scope);

    if (lhs == null) {
      throw this.parseException("Bad 'remove' statement");
    }

    return lhs.value;
  }

  private LocatedValue<? extends IncDec> parsePreIncDec(final BasicScope scope) {
    if (this.nextToken() == null) {
      return null;
    }

    // --[VariableReference]
    // ++[VariableReference]

    if (!this.currentToken().equals("++") && !this.currentToken().equals("--")) {
      return null;
    }

    Token operToken = this.currentToken();
    String operStr = this.currentToken().equals("++") ? Parser.PRE_INCREMENT : Parser.PRE_DECREMENT;

    this.readToken(); // oper

    LocatedValue<? extends Value> lhs = this.parseVariableReference(scope);
    if (lhs == null || !(lhs.value instanceof VariableReference)) {
      throw this.parseException("Variable reference expected");
    }

    int ltype = lhs.value.getType().getType();
    if (ltype != DataTypes.TYPE_INT && ltype != DataTypes.TYPE_FLOAT) {
      throw this.parseException(operStr + " requires a numeric variable reference");
    }

    Operator oper = new Operator(this.makeLocation(operToken), operStr, this);

    return Value.wrap(
        new IncDec((VariableReference) lhs.value, oper),
        this.makeLocation(operToken, this.peekPreviousToken()));
  }

  private LocatedValue<? extends Value> parsePostIncDec(
      final LocatedValue<? extends VariableReference> lhs) {
    // [VariableReference]++
    // [VariableReference]--

    if (!this.currentToken().equals("++") && !this.currentToken().equals("--")) {
      return lhs;
    }

    Token operToken = this.currentToken();
    String operStr =
        this.currentToken().equals("++") ? Parser.POST_INCREMENT : Parser.POST_DECREMENT;

    int ltype = lhs.value.getType().getType();
    if (ltype != DataTypes.TYPE_INT && ltype != DataTypes.TYPE_FLOAT) {
      throw this.parseException(operStr + " requires a numeric variable reference");
    }

    this.readToken(); // oper

    Operator oper = new Operator(this.makeLocation(operToken), operStr, this);

    return Value.wrap(
        new IncDec(lhs.value, oper), this.makeLocation(lhs.location, oper.getLocation()));
  }

  private LocatedValue<? extends Value> parseExpression(final BasicScope scope) {
    return this.parseExpression(scope, null);
  }

  private LocatedValue<? extends Value> parseExpression(
      final BasicScope scope, final Operator previousOper) {
    if (this.currentToken().equals(";")) {
      return null;
    }

    LocatedValue<? extends Value> lhs = null;
    LocatedValue<? extends Value> rhs = null;
    Operator oper = null;

    Token operator = this.currentToken();
    if (operator.equals("!")) {
      oper = new Operator(this.makeLocation(operator), operator.content, this);
      this.readToken(); // !
      if ((lhs = this.parseValue(scope)) == null) {
        throw this.parseException("Value expected");
      }

      lhs = this.autoCoerceValue(DataTypes.BOOLEAN_TYPE, lhs, scope);
      lhs =
          Value.wrap(
              new Operation(lhs.value, oper), this.makeLocation(oper.getLocation(), lhs.location));
      if (!lhs.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
        throw this.parseException("\"!\" operator requires a boolean value");
      }
    } else if (operator.equals("~")) {
      oper = new Operator(this.makeLocation(operator), operator.content, this);
      this.readToken(); // ~
      if ((lhs = this.parseValue(scope)) == null) {
        throw this.parseException("Value expected");
      }

      lhs =
          Value.wrap(
              new Operation(lhs.value, oper), this.makeLocation(oper.getLocation(), lhs.location));
      if (!lhs.value.getType().equals(DataTypes.INT_TYPE)
          && !lhs.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
        throw this.parseException("\"~\" operator requires an integer or boolean value");
      }
    } else if (operator.equals("-")) {
      // See if it's a negative numeric constant
      if ((lhs = this.parseValue(scope)) == null) {
        // Nope. Unary minus.
        oper = new Operator(this.makeLocation(operator), operator.content, this);
        this.readToken(); // -
        if ((lhs = this.parseValue(scope)) == null) {
          throw this.parseException("Value expected");
        }

        lhs =
            Value.wrap(
                new Operation(lhs.value, oper),
                this.makeLocation(oper.getLocation(), lhs.location));
      }
    } else if (operator.equals("remove")) {
      oper = new Operator(this.makeLocation(operator), operator.content, this);
      this.readToken(); // remove

      lhs = this.parseVariableReference(scope);
      if (lhs == null || !(lhs.value instanceof CompositeReference)) {
        throw this.parseException("Aggregate reference expected");
      }

      lhs =
          Value.wrap(
              new Operation(lhs.value, oper), this.makeLocation(oper.getLocation(), lhs.location));
    } else if ((lhs = this.parseValue(scope)) == null) {
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

        LocatedValue<? extends Value> conditional = lhs;

        if (!conditional.value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
          throw this.parseException(
              "Non-boolean expression " + conditional + " (" + conditional.value.getType() + ")");
        }

        if ((lhs = this.parseExpression(scope)) == null) {
          throw this.parseException("Value expected in left hand side");
        }

        if (this.currentToken().equals(":")) {
          this.readToken(); // :
        } else {
          throw this.parseException(":", this.currentToken());
        }

        if ((rhs = this.parseExpression(scope)) == null) {
          throw this.parseException("Value expected");
        }

        if (!oper.validCoercion(lhs.value.getType(), rhs.value.getType())) {
          throw this.parseException(
              "Cannot choose between "
                  + lhs
                  + " ("
                  + lhs.value.getType()
                  + ") and "
                  + rhs
                  + " ("
                  + rhs.value.getType()
                  + ")");
        }

        lhs =
            Value.wrap(
                new TernaryExpression(conditional.value, lhs.value, rhs.value),
                this.makeLocation(conditional.location, rhs.location));
      } else {
        this.readToken(); // operator

        if ((rhs = this.parseExpression(scope, oper)) == null) {
          throw this.parseException("Value expected");
        }

        Type ltype = lhs.value.getType();
        Type rtype = rhs.value.getType();

        if (oper.equals("+")
            && (ltype.equals(DataTypes.TYPE_STRING) || rtype.equals(DataTypes.TYPE_STRING))) {
          // String concatenation
          if (!ltype.equals(DataTypes.TYPE_STRING)) {
            lhs = this.autoCoerceValue(DataTypes.STRING_TYPE, lhs, scope);
          }
          if (!rtype.equals(DataTypes.TYPE_STRING)) {
            rhs = this.autoCoerceValue(DataTypes.STRING_TYPE, rhs, scope);
          }
          if (lhs.value instanceof Concatenate) {
            ((Concatenate) lhs.value).addString(rhs.value);
            // re-wrap
            lhs = Value.wrap(lhs.value, this.makeLocation(lhs.location, rhs.location));
          } else {
            lhs =
                Value.wrap(
                    new Concatenate(lhs.value, rhs.value),
                    this.makeLocation(lhs.location, rhs.location));
          }
        } else {
          Location operationLocation = this.makeLocation(lhs.location, rhs.location);

          rhs = this.autoCoerceValue(ltype, rhs, scope);
          if (!oper.validCoercion(ltype, rhs.value.getType())) {
            throw this.parseException(
                "Cannot apply operator "
                    + oper
                    + " to "
                    + lhs
                    + " ("
                    + lhs.value.getType()
                    + ") and "
                    + rhs
                    + " ("
                    + rhs.value.getType()
                    + ")");
          }
          lhs = Value.wrap(new Operation(lhs.value, rhs.value, oper), operationLocation);
        }
      }
    } while (true);
  }

  private LocatedValue<? extends Value> parseValue(final BasicScope scope) {
    if (this.currentToken().equals(";")) {
      return null;
    }

    Token valueStartToken = this.currentToken();

    LocatedValue<? extends Value> result = null;

    // Parse parenthesized expressions
    if (valueStartToken.equals("(")) {
      this.readToken(); // (

      result = this.parseExpression(scope);

      if (this.currentToken().equals(")")) {
        this.readToken(); // )
      } else {
        throw this.parseException(")", this.currentToken());
      }

      if (result != null) {
        // re-wrap with the parenthesis
        result =
            Value.wrap(result.value, this.makeLocation(valueStartToken, this.peekPreviousToken()));
      }
    }

    // Parse constant values
    // true and false are reserved words

    else if (valueStartToken.equalsIgnoreCase("true")) {
      this.readToken();
      result = Value.wrap(DataTypes.TRUE_VALUE, this.makeLocation(valueStartToken));
    } else if (valueStartToken.equalsIgnoreCase("false")) {
      this.readToken();
      result = Value.wrap(DataTypes.FALSE_VALUE, this.makeLocation(valueStartToken));
    } else if (valueStartToken.equals("__FILE__")) {
      this.readToken();
      result =
          Value.wrap(
              new Value(String.valueOf(this.shortFileName)), this.makeLocation(valueStartToken));
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
          throw this.parseException("{", this.currentToken());
        }
      } else {
        if (baseType != null) {
          this.rewindBackTo(anchor);
        }
        if ((result = this.parseVariableReference(scope)) != null) {}
      }
    }

    while (result != null && (this.currentToken().equals(".") || this.currentToken().equals("["))) {
      Variable current = new Variable(result.value.getType());
      current.setExpression(result.value);

      result =
          this.parseVariableReference(
              scope, Value.wrap(new VariableReference(current), result.location));
    }

    if (result != null && result.value instanceof VariableReference) {
      @SuppressWarnings("unchecked")
      LocatedValue<? extends VariableReference> ref =
          (LocatedValue<? extends VariableReference>) result;
      result = this.parseAssignment(scope, ref);
      if (result == null) {
        result = this.parsePostIncDec(ref);
      }
    }

    return result;
  }

  private LocatedValue<? extends Value> parseNumber() {
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
        throw this.parseException("numeric value", fraction);
      }

      return Value.wrap(number, this.makeLocation(numberStartToken, this.peekPreviousToken()));
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

    return Value.wrap(number, this.makeLocation(numberStartToken, this.peekPreviousToken()));
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

  private LocatedValue<? extends Value> parseString(final BasicScope scope) {
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

    final Value result;
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

        // Plain strings can't span lines
        throw this.parseException("No closing " + stopCharacter + " found");
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

        LocatedValue<? extends Value> rhs = this.parseExpression(scope);

        if (rhs == null) {
          throw this.parseException("Expression expected");
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
          throw this.parseException("}", this.currentToken());
        }

        this.clearCurrentToken();

        Value lhs = new Value(resultString.toString());
        if (conc == null) {
          conc = new Concatenate(lhs, rhs.value);
        } else {
          conc.addString(lhs);
          conc.addString(rhs.value);
        }

        resultString.setLength(0);
        continue;
      }

      if (ch == stopCharacter) {
        this.currentToken =
            this.currentLine.makeToken(i + 1); // + 1 to get rid of stop character token
        this.readToken();

        Value newString = new Value(resultString.toString());

        if (conc == null) {
          result = newString;
        } else {
          conc.addString(newString);
          result = conc;
        }

        break;
      }
      resultString.append(ch);
    }

    return Value.wrap(result, this.makeLocation(stringStartPosition));
  }

  private int parseEscapeSequence(final StringBuilder resultString, int i) {
    final String line = this.restOfLine();

    if (++i == line.length()) {
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
          throw this.parseException("Hexadecimal character escape requires 2 digits");
        }
        break;

      case 'u':
        try {
          int hex16 = Integer.parseInt(line.substring(i + 1, i + 5), 16);
          resultString.append((char) hex16);
          i += 4;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
          throw this.parseException("Unicode character escape requires 4 digits");
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
            throw this.parseException("Octal character escape requires 3 digits");
          }
        }
        resultString.append(ch);
    }

    return i;
  }

  private Value parseLiteral(Type type, String element) {
    Value value = DataTypes.parseValue(type, element, false);
    if (value == null) {
      throw this.parseException("Bad " + type.toString() + " value: \"" + element + "\"");
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
          ScriptException ex =
              this.parseException2(
                  "Multiple matches for \"" + s1 + "\"; using \"" + s2 + "\".",
                  "Clarify by using one of:");
          RequestLogger.printLine(ex.getMessage());
          for (String name : names) {
            RequestLogger.printLine(name);
          }
        } else {
          ScriptException ex =
              this.parseException(
                  "Changing \"" + s1 + "\" to \"" + s2 + "\" would get rid of this message.");
          RequestLogger.printLine(ex.getMessage());
        }
      }
    }

    return value;
  }

  private LocatedValue<? extends Value> parseTypedConstant(final BasicScope scope) {
    if (!this.currentToken().equals("$")) {
      return null;
    }

    Token typedConstantStartToken = this.currentToken();

    this.readToken(); // read $

    Token name = this.currentToken();
    Type type = scope.findType(name.content);
    boolean plurals = false;

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
      } else {
        throw this.parseException("Unknown type " + name);
      }

      type = scope.findType(buf.toString());

      plurals = true;
    }

    this.readToken();

    if (type == null) {
      throw this.parseException("Unknown type " + name);
    } else {
      type = type.reference(this.makeLocation(name));
    }

    if (!type.isPrimitive()) {
      throw this.parseException("Non-primitive type " + name);
    }

    if (this.currentToken().equals("[")) {
      this.readToken(); // read [
    } else {
      throw this.parseException("[", this.currentToken());
    }

    if (plurals) {
      Value value = this.parsePluralConstant(scope, type);

      Location typedConstantLocation =
          this.makeLocation(typedConstantStartToken, this.peekPreviousToken());

      if (value != null) {
        return Value.wrap(value, typedConstantLocation); // explicit list of values
      }
      value = type.allValues();
      if (value != null) {
        return Value.wrap(value, typedConstantLocation); // implicit enumeration
      }
      throw this.parseException("Can't enumerate all " + name);
    }

    StringBuilder resultString = new StringBuilder();
    final Value result;

    int level = 1;
    for (int i = 0; ; ++i) {
      final String line = this.restOfLine();

      if (i == line.length()) {
        throw this.parseException("No closing ] found");
      }

      char c = line.charAt(i);
      if (c == '\\') {
        if (++i == line.length()) {
          throw this.parseException("No closing ] found");
        }

        resultString.append(line.charAt(i));
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

        this.readToken(); // read ]
        String input = resultString.toString().trim();
        result = this.parseLiteral(type, input);
        break;
      } else {
        resultString.append(c);
      }
    }

    return Value.wrap(result, this.makeLocation(typedConstantStartToken, this.peekPreviousToken()));
  }

  private PluralValue parsePluralConstant(final BasicScope scope, final Type type) {
    // Directly work with currentLine - ignore any "tokens" you meet until
    // the string is closed

    List<Value> list = new ArrayList<>();
    int level = 1;
    boolean slash = false;

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
          throw this.parseException("No closing ] found");
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
          this.currentLine.makeToken(i - 1);
          this.currentIndex += i - 1;
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
        list.add(this.parseLiteral(type, element));
      }

      if (ch == ']') {
        if (i > 0) {
          this.currentLine.makeToken(i);
          this.currentIndex += i;
        }
        this.readToken(); // read ]
        if (list.size() == 0) {
          // Empty list - caller will interpret this specially
          return null;
        }
        return new PluralValue(type, list);
      }
    }
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

  private LocatedValue<? extends Value> parseVariableReference(final BasicScope scope) {
    if (!this.parseIdentifier(this.currentToken().content)) {
      return null;
    }

    Token name = this.currentToken();
    Location variableLocation = this.makeLocation(name);

    Variable var = scope.findVariable(name.content, true);

    if (var == null) {
      throw this.parseException("Unknown variable '" + name + "'");
    }

    this.readToken(); // read name

    return this.parseVariableReference(
        scope, Value.wrap(new VariableReference(var), variableLocation));
  }

  /**
   * Look for an index/key, and return the corresponding data, expecting {@code var} to be a {@link
   * AggregateType}/{@link RecordType}, e.g., {@code map.key}, {@code array[0]}.
   *
   * <p>May also return a {@link FunctionCall} if the chain ends with/is a function call, e.g.,
   * {@code var.function()}.
   *
   * <p>There may also be nothing, in which case the submitted variable reference is returned as is.
   */
  private LocatedValue<? extends Value> parseVariableReference(
      final BasicScope scope, final LocatedValue<? extends VariableReference> var) {
    LocatedValue<? extends VariableReference> current = var;
    Type type = var.value.getType();
    List<Value> indices = new ArrayList<>();

    boolean parseAggregate = this.currentToken().equals("[");

    while (this.currentToken().equals("[")
        || this.currentToken().equals(".")
        || parseAggregate && this.currentToken().equals(",")) {
      LocatedValue<? extends Value> index;

      type = type.getBaseType();

      if (this.currentToken().equals("[") || this.currentToken().equals(",")) {
        this.readToken(); // read [ or ,
        parseAggregate = true;

        if (!(type instanceof AggregateType)) {
          if (indices.isEmpty()) {
            throw this.parseException("Variable '" + var.value.getName() + "' cannot be indexed");
          } else {
            throw this.parseException("Too many keys for '" + var.value.getName() + "'");
          }
        }

        AggregateType atype = (AggregateType) type;
        index = this.parseExpression(scope);
        if (index == null) {
          throw this.parseException("Index for '" + current.value.getName() + "' expected");
        }

        if (!index.value.getType().getBaseType().equals(atype.getIndexType().getBaseType())) {
          throw this.parseException(
              "Index for '"
                  + current.value.getName()
                  + "' has wrong data type "
                  + "(expected "
                  + atype.getIndexType()
                  + ", got "
                  + index.value.getType()
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
          throw this.parseException("Record expected");
        }

        RecordType rtype = (RecordType) type;

        Token field = this.currentToken();
        if (this.parseIdentifier(field.content)) {
          this.readToken(); // read name
        } else {
          throw this.parseException("Field name expected");
        }

        index = Value.wrap(rtype.getFieldIndex(field.content), this.makeLocation(field));
        if (index != null) {
          type = rtype.getDataType(index.value);
        } else {
          throw this.parseException("Invalid field name '" + field + "'");
        }
      }

      indices.add(index.value);

      if (parseAggregate && this.currentToken().equals("]")) {
        this.readToken(); // read ]
        parseAggregate = false;
      }

      current =
          Value.wrap(
              new CompositeReference(current.value.target, indices, this),
              this.makeLocation(current.location, this.peekPreviousToken()));
    }

    if (parseAggregate) {
      throw this.parseException("]", this.currentToken());
    }

    return current;
  }

  private String parseDirective(final String directive) {
    if (!this.currentToken().equalsIgnoreCase(directive)) {
      return null;
    }

    this.readToken(); // directive

    if (this.atEndOfFile()) {
      throw this.parseException("<", this.currentToken());
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
        throw this.parseException("No closing " + ch + " found");
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
      this.currentToken = this.currentLine.makeToken(endIndex);
      this.readToken();
    }

    if (this.currentToken().equals(";")) {
      this.readToken(); // read ;
    }

    return resultString;
  }

  private void parseScriptName() {
    String resultString = this.parseDirective("script");
    if (this.scriptName == null) {
      this.scriptName = resultString;
    }
  }

  private void parseNotify() {
    String resultString = this.parseDirective("notify");
    if (this.notifyRecipient == null) {
      this.notifyRecipient = resultString;
    }
  }

  private void parseSince() {
    String revision = this.parseDirective("since");
    if (revision != null) {
      // enforce "since" directives RIGHT NOW at parse time
      this.enforceSince(revision);
    }
  }

  private String parseImport() {
    return this.parseDirective("import");
  }

  // **************** Tokenizer *****************

  private static final char BOM = '\ufeff';

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
          this.currentLine.makeComment(restOfLine.length());

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
          this.currentLine.makeComment(restOfLine.length());

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
      this.currentLine.tokens.removeLast();

      while (this.currentLine.tokens.isEmpty()) {
        // Don't do null checks. If previousLine is null, it means we never saw the
        // destination token, meaning we'd want to throw an error anyway.
        this.currentLine = this.currentLine.previousLine;
      }

      this.currentToken = this.currentLine.tokens.getLast();
      this.currentIndex = this.currentToken.getStart().getCharacter();
    }
  }

  /** Finds the last token that was *read* */
  private final Token peekPreviousToken() {
    if (this.currentToken != null) {
      // Temporarily remove currentToken
      this.currentLine.tokens.removeLast();
    }

    final Token previousToken = this.peekLastToken();

    if (this.currentToken != null) {
      // Add the previously removed token back in
      this.currentLine.tokens.addLast(this.currentToken);
    }

    return previousToken;
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
      this.currentLine.tokens.removeLast();
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

  public final class Line {
    private final String content;
    private final int lineNumber;
    private final int offset;

    private final Deque<Token> tokens = new LinkedList<>();

    private final Line previousLine;
    /* Not made final to avoid a possible StackOverflowError. Do not modify. */
    private Line nextLine = null;

    private Line(final LineNumberReader commandStream) {
      this(commandStream, null);
    }

    private Line(final LineNumberReader commandStream, final Line previousLine) {
      this.previousLine = previousLine;
      if (previousLine != null) {
        previousLine.nextLine = this;
      }

      int offset = 0;
      String line;

      try {
        line = commandStream.readLine();
      } catch (IOException e) {
        // This should not happen. Therefore, print a stack trace for debug purposes.
        StaticEntity.printStackTrace(e);
        line = null;
      }

      if (line == null) {
        // We are the "end of file" (or there was an IOException when reading)
        this.content = null;
        this.lineNumber = this.previousLine != null ? this.previousLine.lineNumber : 0;
        this.offset = this.previousLine != null ? this.previousLine.offset : 0;
        return;
      }

      // If the line starts with a Unicode BOM, remove it.
      if (line.length() > 0 && line.charAt(0) == Parser.BOM) {
        line = line.substring(1);
        offset += 1;
      }

      // Remove whitespace at front and end
      final String trimmed = line.trim();

      if (!trimmed.isEmpty()) {
        // While the more "obvious" solution would be to use line.indexOf( trimmed ), we
        // know that the only difference between these strings is leading/trailing
        // whitespace.
        //
        // There are two cases:
        //  1. `trimmed` is empty, in which case `line` was entirely composed of whitespace.
        //  2. `trimmed` is non-empty. The first non-whitespace character in `line`
        //     indicates the start of `trimmed`.
        //
        // This is more efficient in that we don't need to confirm that the rest of
        // `trimmed` is present in `line`.

        final int ltrim = line.indexOf(trimmed.charAt(0));
        offset += ltrim;
      }

      line = trimmed;

      this.content = line;
      this.lineNumber = commandStream.getLineNumber();
      this.offset = offset;
    }

    private String substring(final int beginIndex) {
      if (this.content == null) {
        return "";
      }

      // subtract "offset" from beginIndex, since we already removed it
      return this.content.substring(beginIndex - this.offset);
    }

    private Token makeToken(final int tokenLength) {
      final Token newToken = new Token(tokenLength);
      this.tokens.addLast(newToken);
      return newToken;
    }

    private Token makeComment(final int commentLength) {
      final Token newToken = new Comment(commentLength);
      this.tokens.addLast(newToken);
      return newToken;
    }

    private Token peekLastToken() {
      Line line = this;

      while (line != null) {
        final Iterator<Token> iter = line.tokens.descendingIterator();
        while (iter.hasNext()) {
          final Token token = iter.next();

          if (!(token instanceof Comment)) {
            return token;
          }
        }

        line = this.previousLine;
      }

      return null;
    }

    @Override
    public String toString() {
      return this.content;
    }

    public class Token extends Range {
      final String content;
      final String followingWhitespace;
      final int restOfLineStart;

      private Token(final int tokenLength) {
        final int offset;

        if (!Line.this.tokens.isEmpty()) {
          offset = Line.this.tokens.getLast().restOfLineStart;
        } else {
          offset = Line.this.offset;
        }

        final String lineRemainder;

        if (Line.this.content == null) {
          // At end of file
          this.content = ";";
          // Going forward, we can just assume lineRemainder is an
          // empty string.
          lineRemainder = "";
        } else {
          final String lineRemainderWithToken = Line.this.substring(offset);

          this.content = lineRemainderWithToken.substring(0, tokenLength);
          lineRemainder = lineRemainderWithToken.substring(tokenLength);
        }

        // 0-indexed line
        final int lineNumber = Math.max(0, Line.this.lineNumber - 1);
        this.setStart(new Position(lineNumber, offset));
        this.setEnd(new Position(lineNumber, offset + tokenLength));

        // As in Line(), this is more efficient than lineRemainder.indexOf( lineRemainder.trim() ).
        String trimmed = lineRemainder.trim();
        final int lTrim = trimmed.isEmpty() ? 0 : lineRemainder.indexOf(trimmed.charAt(0));

        this.followingWhitespace = lineRemainder.substring(0, lTrim);

        this.restOfLineStart = offset + tokenLength + lTrim;
      }

      /** The Line in which this token exists */
      final Line getLine() {
        return Line.this;
      }

      public boolean equals(final String s) {
        return this.content.equals(s);
      }

      public boolean equalsIgnoreCase(final String s) {
        return this.content.equalsIgnoreCase(s);
      }

      public int length() {
        return this.content.length();
      }

      public String substring(final int beginIndex) {
        return this.content.substring(beginIndex);
      }

      public String substring(final int beginIndex, final int endIndex) {
        return this.content.substring(beginIndex, endIndex);
      }

      public boolean endsWith(final String suffix) {
        return this.content.endsWith(suffix);
      }

      @Override
      public String toString() {
        return this.content;
      }
    }

    private class Comment extends Token {
      private Comment(final int commentLength) {
        super(commentLength);
      }
    }
  }

  public List<Token> getTokens() {
    final List<Token> result = new LinkedList<>();

    Line line = this.currentLine;

    // Go back to the start
    while (line != null && line.previousLine != null) {
      line = line.previousLine;
    }

    while (line != null && line.content != null) {
      for (final Token token : line.tokens) {
        result.add(token);
      }

      line = line.nextLine;
    }

    return result;
  }

  public List<String> getTokensContent() {
    return this.getTokens().stream().map(token -> token.content).collect(Collectors.toList());
  }

  private Position getCurrentPosition() {
    // 0-indexed
    int lineNumber = Math.max(0, this.getLineNumber() - 1);
    return new Position(lineNumber, this.currentIndex);
  }

  private Range rangeToHere(final Position start) {
    return new Range(start != null ? start : this.getCurrentPosition(), this.getCurrentPosition());
  }

  private Range makeInlineRange(final Position start, final int offset) {
    Position end = new Position(start.getLine(), start.getCharacter() + offset);

    return offset >= 0 ? new Range(start, end) : new Range(end, start);
  }

  private Range makeRange(final Range start, final Range end) {
    if (end == null
        || start.getStart().getLine() > end.getEnd().getLine()
        || (start.getStart().getLine() == end.getEnd().getLine()
            && start.getStart().getCharacter() > end.getEnd().getCharacter())) {
      return start;
    }

    return new Range(start.getStart(), end.getEnd());
  }

  private Location makeLocation(final Position start) {
    return this.makeLocation(this.rangeToHere(start));
  }

  private Location makeLocation(final Range start, final Range end) {
    return this.makeLocation(this.makeRange(start, end));
  }

  private Location makeLocation(final Location start, final Location end) {
    return this.makeLocation(start.getRange(), end.getRange());
  }

  private Location makeLocation(final Location start, final Range end) {
    return this.makeLocation(start.getRange(), end);
  }

  private Location makeLocation(final Range range) {
    String uri = this.fileUri != null ? this.fileUri.toString() : this.istream.toString();
    return new Location(uri, range);
  }

  private Location makeZeroWidthLocation() {
    return this.makeLocation(this.getCurrentPosition());
  }

  private Location makeZeroWidthLocation(final Position position) {
    return this.makeLocation(new Range(position, position));
  }

  // **************** Parse errors *****************

  private ScriptException parseException(final String expected, final Token found) {
    String foundString = found.content;

    if (found.getLine().content == null) {
      foundString = "end of file";
    }

    return this.parseException("Expected " + expected + ", found " + foundString);
  }

  private ScriptException parseException(final String message) {
    return new ScriptException(message + " " + this.getLineAndFile());
  }

  private ScriptException parseException2(final String message1, final String message2) {
    return new ScriptException(message1 + " " + this.getLineAndFile() + " " + message2);
  }

  private ScriptException undefinedFunctionException(
      final String name, final List<LocatedValue<? extends Value>> params) {
    return this.parseException(
        Parser.undefinedFunctionMessage(
            name, params.stream().map(param -> param.value).collect(Collectors.toList())));
  }

  private ScriptException multiplyDefinedFunctionException(final Function f) {
    String buffer = "Function '" + f.getSignature() + "' defined multiple times.";
    return this.parseException(buffer);
  }

  private ScriptException overridesLibraryFunctionException(final Function f) {
    String buffer = "Function '" + f.getSignature() + "' overrides a library function.";
    return this.parseException(buffer);
  }

  private ScriptException varargClashException(final Function f, final Function clash) {
    String buffer =
        "Function '"
            + f.getSignature()
            + "' clashes with existing function '"
            + clash.getSignature()
            + "'.";
    return this.parseException(buffer);
  }

  public final ScriptException sinceException(
      String current, String target, boolean targetIsRevision) {
    String template;
    if (targetIsRevision) {
      template =
          "'%s' requires revision r%s of kolmafia or higher (current: r%s).  Up-to-date builds can be found at https://ci.kolmafia.us/.";
    } else {
      template =
          "'%s' requires version %s of kolmafia or higher (current: %s).  Up-to-date builds can be found at https://ci.kolmafia.us/.";
    }

    return new ScriptException(String.format(template, this.shortFileName, target, current));
  }

  public static String undefinedFunctionMessage(final String name, final List<Value> params) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Function '");
    Parser.appendFunctionCall(buffer, name, params);
    buffer.append(
        "' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts.");
    return buffer.toString();
  }

  private void enforceSince(String revision) {
    try {
      if (revision.startsWith("r")) // revision
      {
        revision = revision.substring(1);
        int targetRevision = Integer.parseInt(revision);
        int currentRevision = StaticEntity.getRevision();
        // A revision of zero means you're probably running in a debugger, in which
        // case you should be able to run anything.
        if (currentRevision != 0 && currentRevision < targetRevision) {
          throw this.sinceException(String.valueOf(currentRevision), revision, true);
        }
      } else // version (or syntax error)
      {
        String[] target = revision.split("\\.");
        if (target.length != 2) {
          throw this.parseException("invalid 'since' format");
        }

        int targetMajor = Integer.parseInt(target[0]);
        int targetMinor = Integer.parseInt(target[1]);

        if (targetMajor > 21 || targetMajor == 21 && targetMinor > 9) {
          throw this.parseException("invalid 'since' format (21.09 was the final point release)");
        }
      }
    } catch (NumberFormatException e) {
      throw this.parseException("invalid 'since' format");
    }
  }

  public final void warning(final String msg) {
    RequestLogger.printLine("WARNING: " + msg + " " + this.getLineAndFile());
  }

  private static void appendFunctionCall(
      final StringBuilder buffer, final String name, final List<Value> params) {
    buffer.append(name);
    buffer.append("(");

    String sep = " ";
    for (Value current : params) {
      buffer.append(sep);
      sep = ", ";
      buffer.append(current.getType());
    }

    buffer.append(" )");
  }

  private String getLineAndFile() {
    return Parser.getLineAndFile(this.shortFileName, this.getLineNumber());
  }

  public static String getLineAndFile(final String fileName, final int lineNumber) {
    if (fileName == null) {
      return "(" + Preferences.getString("commandLineNamespace") + ")";
    }

    return "(" + fileName + ", line " + lineNumber + ")";
  }

  public static void printIndices(
      final List<Value> indices, final PrintStream stream, final int indent) {
    if (indices == null) {
      return;
    }

    for (Value current : indices) {
      AshRuntime.indentLine(stream, indent);
      stream.println("<KEY>");
      current.print(stream, indent + 1);
    }
  }
}
