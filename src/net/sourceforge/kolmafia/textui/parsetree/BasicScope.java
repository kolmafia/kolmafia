package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import net.sourceforge.kolmafia.utilities.PauseObject;
import org.eclipse.lsp4j.Location;

public abstract class BasicScope extends Command {
  private final PauseObject pauser = new PauseObject();
  private static long nextPause = System.currentTimeMillis();

  protected static final int BARRIER_NONE = 0; // no return, etc. yet
  protected static final int BARRIER_SEEN = 1; // just seen
  protected static final int BARRIER_PAST = 2; // already warned about dead code

  protected TypeList types;
  protected VariableList variables;
  protected FunctionList functions;
  protected BasicScope parentScope;
  protected List<BasicScope> nestedScopes;
  boolean executed;

  public BasicScope(
      FunctionList functions, VariableList variables, TypeList types, BasicScope parentScope) {
    // Scopes need to be instantiated before we reach their end,
    // so we can't send their location straight away.
    super(null);
    this.functions = (functions == null) ? new FunctionList() : functions;
    this.types = (types == null) ? new TypeList() : types;
    this.variables = (variables == null) ? new VariableList() : variables;
    this.parentScope = parentScope;
    this.nestedScopes = new ArrayList<>();
    this.nestedScopes.add(this);
    while (parentScope != null) {
      parentScope.nestedScopes.add(this);
      parentScope = parentScope.parentScope;
    }
    this.executed = false;
  }

  public BasicScope(VariableList variables, final BasicScope parentScope) {
    this(null, variables, null, parentScope);
  }

  public BasicScope(final BasicScope parentScope) {
    this(null, null, null, parentScope);
  }

  /**
   * Scopes need to be instantiated before we reach their end, so we can't send their location
   * straight away.
   */
  public void setScopeLocation(final Location location) {
    if (this.getLocation() == null) {
      this.setLocation(location);
    }
  }

  public BasicScope getParentScope() {
    return this.parentScope;
  }

  public TypeList getTypes() {
    return this.types;
  }

  public boolean addType(final Type t) {
    return this.types.add(t);
  }

  public Type findType(final String name) {
    Type current = this.types.find(name);
    if (current != null) {
      return current;
    }
    if (this.parentScope != null) {
      return this.parentScope.findType(name);
    }
    return null;
  }

  public List<BasicScope> getScopes() {
    return this.nestedScopes;
  }

  public VariableList getVariables() {
    return this.variables;
  }

  public boolean addVariable(final Variable v) {
    return this.variables.add(v);
  }

  public Variable findVariable(final String name) {
    return this.findVariable(name, false);
  }

  public Variable findVariable(final String name, final boolean recurse) {
    Variable current = this.variables.find(name);
    if (current != null) {
      return current;
    }
    if (recurse && this.parentScope != null) {
      return this.parentScope.findVariable(name, true);
    }
    return null;
  }

  public FunctionList getFunctions() {
    return this.functions;
  }

  public boolean addFunction(final Function f) {
    return this.functions.add(f);
  }

  public boolean removeFunction(final Function f) {
    return this.functions.remove(f);
  }

  public final Function findFunction(final String name, final List<Evaluable> params) {
    return this.findFunction(name, params, MatchType.ANY);
  }

  public final Function findFunction(
      final String name, List<Evaluable> params, final MatchType matchType) {
    // Functions with no params are fine.
    if (params == null) {
      params = Collections.emptyList();
    }

    // We will consider functions from this scope and from the RuntimeLibrary.
    Function[] functions = this.functions.findFunctions(name);

    Function result = null;

    if (matchType == MatchType.ANY || matchType == MatchType.EXACT) {
      // Exact, no vararg
      result = this.findFunction(functions, name, params, MatchType.EXACT, false);
      if (result != null) {
        return result;
      }

      // Exact, vararg
      result = this.findFunction(functions, name, params, MatchType.EXACT, true);
      if (result != null) {
        return result;
      }
    }

    if (matchType == MatchType.ANY || matchType == MatchType.BASE) {
      // Base, no vararg
      result = this.findFunction(functions, name, params, MatchType.BASE, false);
      if (result != null) {
        return result;
      }

      // Base, vararg
      result = this.findFunction(functions, name, params, MatchType.BASE, true);
      if (result != null) {
        return result;
      }
    }

    if (matchType == MatchType.ANY || matchType == MatchType.COERCE) {
      // Coerce, no vararg
      result = this.findFunction(functions, name, params, MatchType.COERCE, false);
      if (result != null) {
        return result;
      }

      // Coerce, vararg
      result = this.findFunction(functions, name, params, MatchType.COERCE, true);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  private Function findFunction(
      final Function[] functions,
      final String name,
      final List<Evaluable> params,
      final MatchType match,
      final boolean vararg) {
    // Search the function list for a match
    for (Function function : functions) {
      if (function.paramsMatch(params, match, vararg)) {
        return function;
      }
    }

    // We are searching a scope. Search the parent scope.
    BasicScope parent = this.getParentScope();
    if (parent != null) {
      Function[] parentFunctions = parent.functions.findFunctions(name);
      return parent.findFunction(parentFunctions, name, params, match, vararg);
    }

    return null;
  }

  public UserDefinedFunction findFunction(final UserDefinedFunction f) {
    if (f.getName().equals("main")) {
      return f;
    }

    Function[] options = this.functions.findFunctions(f.getName());
    for (Function function : options) {
      if (function instanceof UserDefinedFunction) {
        UserDefinedFunction existing = (UserDefinedFunction) function;
        if (f.paramsMatch(existing, false)) {
          return existing;
        }
      }
    }

    return null;
  }

  public Function findVarargClash(final UserDefinedFunction f) {
    // We will consider functions from this scope and from the RuntimeLibrary.
    Function[] userFunctions = this.functions.findFunctions(f.getName());
    Function[] libraryFunctions = RuntimeLibrary.functions.findFunctions(f.getName());

    Function result = this.findVarargClash(this, f, userFunctions);
    if (result != null) {
      return result;
    }

    result = this.findVarargClash(null, f, libraryFunctions);
    if (result != null) {
      return result;
    }

    return null;
  }

  private Function findVarargClash(
      BasicScope scope, final UserDefinedFunction f, final Function[] functions) {
    for (Function function : functions) {
      if (f.varargsClash(function)) {
        return function;
      }
    }

    if (scope == null) {
      return null;
    }

    BasicScope parent = scope.getParentScope();
    if (parent != null) {
      return parent.findVarargClash(f);
    }

    return null;
  }

  public UserDefinedFunction replaceFunction(
      final UserDefinedFunction existing, final UserDefinedFunction f) {
    if (f.getName().equals("main")) {
      return f;
    }

    if (existing != null) {
      // Must use new definition's variables

      existing.setVariableReferences(f.getVariableReferences());
      return existing;
    }

    this.addFunction(f);
    return f;
  }

  public Function findFunction(final String name, boolean hasParameters) {
    Function function = findFunction(name, this.functions, hasParameters);

    if (function != null) {
      return function;
    }

    function = findFunction(name, RuntimeLibrary.functions, hasParameters);

    return function;
  }

  public Function findFunction(
      final String name, final FunctionList functionList, final boolean hasParameters) {
    Function[] functions = functionList.findFunctions(name);

    if (functions.length == 0) {
      return null;
    }

    boolean isAmbiguous = false;
    int minParamCount = Integer.MAX_VALUE;
    Function bestMatch = null;

    for (int i = 0; i < functions.length; ++i) {
      int paramCount = 0;
      boolean isSingleString = false;

      Iterator<VariableReference> refIterator = functions[i].getVariableReferences().iterator();

      if (refIterator.hasNext()) {
        VariableReference reference = refIterator.next();
        if (reference.getType().equals(DataTypes.STRING_TYPE)) {
          isSingleString = true;
        }
        paramCount = 1;
      }

      while (refIterator.hasNext()) {
        refIterator.next();
        isSingleString = false;
        ++paramCount;
      }

      if (paramCount == 0) {
        if (!hasParameters) {
          return functions[i];
        }
      } else if (hasParameters && paramCount == 1) {
        if (isSingleString) {
          return functions[i];
        }

        if (minParamCount == 1) {
          isAmbiguous = true;
        }

        bestMatch = functions[i];
        minParamCount = 1;
      } else {
        if (paramCount < minParamCount) {
          bestMatch = functions[i];
          minParamCount = paramCount;
          isAmbiguous = false;
        } else if (minParamCount == paramCount) {
          isAmbiguous = true;
        }
      }
    }

    if (isAmbiguous) {
      return null;
    }

    return bestMatch;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<SCOPE>");

    AshRuntime.indentLine(stream, indent + 1);
    stream.println("<TYPES>");

    for (Type currentType : types) {
      currentType.print(stream, indent + 2);
    }

    AshRuntime.indentLine(stream, indent + 1);
    stream.println("<VARIABLES>");

    for (Variable currentVar : this.variables) {
      currentVar.print(stream, indent + 2);
    }

    AshRuntime.indentLine(stream, indent + 1);
    stream.println("<FUNCTIONS>");

    for (Function currentFunc : this.functions) {
      currentFunc.print(stream, indent + 2);
    }

    AshRuntime.indentLine(stream, indent + 1);
    stream.println("<COMMANDS>");

    Iterator<Command> it = this.getCommands();
    while (it.hasNext()) {
      Command currentCommand = it.next();
      currentCommand.print(stream, indent + 2);
    }
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    // Yield control at the top of the scope to
    // allow other tasks to run and keyboard input -
    // especially the Escape key - to be accepted.

    // Unfortunately, the following does not work
    // Thread.yield();

    // ...but the following does.
    long t = System.currentTimeMillis();
    if (t >= BasicScope.nextPause) {
      BasicScope.nextPause = t + 100L;
      this.pauser.pause(1);
    }

    try {
      Value result = DataTypes.VOID_VALUE;
      interpreter.traceIndent();

      Iterator<Command> it = this.getCommands();
      while (it.hasNext()) {
        Command current = it.next();
        result = current.execute(interpreter);

        // Abort processing now if command failed
        if (!KoLmafia.permitsContinue()) {
          interpreter.setState(ScriptRuntime.State.EXIT);
        }

        if (result == null) {
          result = DataTypes.VOID_VALUE;
        }

        if (ScriptRuntime.isTracing()) {
          interpreter.trace("[" + interpreter.getState() + "] <- " + result.toQuotedString());
        }

        if (interpreter.getState() != ScriptRuntime.State.NORMAL) {
          break;
        }
      }

      interpreter.traceUnindent();
      return result;
    } finally {
      this.executed = true;
    }
  }

  public abstract void addCommand(final Command c, final Parser p);

  public abstract Iterator<Command> getCommands();
}
