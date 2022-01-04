package net.sourceforge.kolmafia.textui;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;

public class NamespaceInterpreter extends AshRuntime {
  private String lastImportString;

  public NamespaceInterpreter() {
    super();
    refresh("");
  }

  @Override
  public Value execute(final String functionName, final Object[] parameters) {
    String importString = Preferences.getString("commandLineNamespace");

    boolean shouldRefresh = !this.lastImportString.equals(importString);

    if (!shouldRefresh) {
      Map<File, Parser> imports = this.parser.getImports();

      for (Entry<File, Parser> entry : imports.entrySet()) {
        File file = entry.getKey();
        long date = entry.getValue().getModificationTimestamp();
        shouldRefresh = date != file.lastModified();
      }
    }

    if (shouldRefresh && !refresh(importString)) {
      return DataTypes.VOID_VALUE;
    }

    return super.execute(functionName, parameters, shouldRefresh);
  }

  private boolean refresh(String importString) {
    this.scope = new Scope(new VariableList(), Parser.getExistingFunctionScope());
    this.parser.getImports().clear();

    if (importString.length() > 0) {
      String[] importList = importString.split(",");

      for (int i = 0; i < importList.length; ++i) {
        try {
          this.parser.importFile(importList[i], this.scope);
        } catch (ScriptException e) {
          // The user changed the script since it was validated
          KoLmafia.updateDisplay(MafiaState.ERROR, e.getMessage());
          return false;
        } catch (Exception e) {
          StaticEntity.printStackTrace(e);
          return false;
        }
      }
    }

    this.lastImportString = importString;
    return true;
  }
}
