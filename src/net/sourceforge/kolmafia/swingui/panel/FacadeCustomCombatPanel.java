package net.sourceforge.kolmafia.swingui.panel;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

public class FacadeCustomCombatPanel {
  protected JTree combatTree;
  protected JTextArea combatEditor;
  protected DefaultTreeModel combatModel;

  protected CardLayout combatCards;

  protected JPanel combatCardPanel;

  public JTree getCombatTree() {
    return combatTree;
  }

  public JPanel getCombatCardPanel() {
    return combatCardPanel;
  }

  public void initCombatPanelUI(
      CustomCombatPanel.CustomCombatTreePanel customCombatTreePanel,
      CustomCombatPanel.CustomCombatEditorPanel customCombatEditorPanel) {
    this.combatTree = new JTree();
    this.combatModel = (DefaultTreeModel) this.combatTree.getModel();

    this.combatCards = new CardLayout();

    this.combatCardPanel.add("tree", customCombatTreePanel);
    this.combatCardPanel.add("editor", customCombatEditorPanel);
  }

  public void initCustomCombatEditorPanel(JTextArea scrollComponent) {
    this.combatEditor = scrollComponent;
    this.combatEditor.setFont(KoLGUIConstants.DEFAULT_FONT);

    this.refreshCombatTree();
  }

  /** Internal class used to handle everything related to displaying custom combat. */
  public void refreshCombatTree() {
    this.combatModel.setRoot(CombatActionManager.getStrategyLookup());
    this.combatTree.setRootVisible(false);

    for (int i = 0; i < this.combatTree.getRowCount(); ++i) {
      this.combatTree.expandRow(i);
    }
  }

  public void refreshCombatEditor(String script) {
    try {
      try (BufferedReader reader =
          FileUtilities.getReader(CombatActionManager.getStrategyLookupFile(script))) {

        if (reader == null) {
          return;
        }

        StringBuffer buffer = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
          buffer.append(line);
          buffer.append('\n');
        }

        this.combatEditor.setText(buffer.toString());
      }
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }

    this.refreshCombatTree();
  }

  public void showCombatCardPanel(String name) {
    this.combatCards.show(this.combatCardPanel, name);
  }

  public void actionConfirmed(String script) {
    String saveText = this.combatEditor.getText();

    File location = CombatActionManager.getStrategyLookupFile(script);
    PrintStream writer = LogStream.openStream(location, true);

    writer.print(saveText);
    writer.close();
    writer = null;

    KoLCharacter.battleSkillNames.setSelectedItem("custom combat script");
    Preferences.setString("battleAction", "custom combat script");

    // After storing all the data on disk, go ahead
    // and reload the data inside of the tree.

    CombatActionManager.loadStrategyLookup(script);
    CombatActionManager.saveStrategyLookup(script);

    this.refreshCombatTree();
    this.combatCards.show(this.combatCardPanel, "tree");
  }
}
