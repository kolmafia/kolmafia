package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class UserDefinedFunctionStub extends AshStub {
  private static final long serialVersionUID = 1L;

  public UserDefinedFunctionStub(AshRuntime interpreter, String ashFunctionName) {
    super(interpreter, ashFunctionName);
  }

  @Override
  protected FunctionList getAllFunctions() {
    return ((AshRuntime) controller).getFunctions();
  }

  @Override
  protected Value execute(Function function, List<Value> ashArgs) {
    UserDefinedFunction ashFunction;
    if (function instanceof UserDefinedFunction) {
      ashFunction = (UserDefinedFunction) function;
    } else {
      throw controller.runtimeException(Parser.undefinedFunctionMessage(ashFunctionName, ashArgs));
    }

    List<Object> ashArgsWithInterpreter = new ArrayList<>(ashArgs.size() + 1);
    ashArgsWithInterpreter.add(controller);
    ashArgsWithInterpreter.addAll(ashArgs);

    return ashFunction.execute((AshRuntime) controller, ashArgsWithInterpreter.toArray());
  }
}
