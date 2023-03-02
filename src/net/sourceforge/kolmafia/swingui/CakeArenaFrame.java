package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CakeArenaManager;
import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.swingui.listener.TableButtonListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CakeArenaFrame extends GenericFrame {
  private JTable familiarTable;
  private LockableListModel<ArenaOpponent> opponents;
  private final CharacterListener familiarListener;

  public CakeArenaFrame() {
    super("Susie's Secret Bedroom!");

    this.setCenterComponent(new CakeArenaPanel());
    this.familiarListener = new CharacterListener(new FamiliarRefresher());
    CharacterListenerRegistry.addCharacterListener(this.familiarListener);
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

  @Override
  public void dispose() {
    CharacterListenerRegistry.removeCharacterListener(this.familiarListener);
    super.dispose();
  }

  private class FamiliarRefresher implements Runnable {
    @Override
    public void run() {
      if (CakeArenaFrame.this.familiarTable != null) {
        CakeArenaFrame.this.familiarTable.validate();
      }
    }
  }

  private class CakeArenaPanel extends JPanel {
    public CakeArenaPanel() {
      super(new BorderLayout(0, 10));
      CakeArenaFrame.this.opponents = CakeArenaManager.getOpponentList();

      String opponentRace;
      String[] columnNames = {
        "Familiar", "Cage Match", "Scavenger Hunt", "Obstacle Course", "Hide and Seek"
      };

      // Register the data for your current familiar to be
      // rendered in the table.

      Object[][] familiarData = new Object[1][5];

      CakeArenaFrame.this.familiarTable = new JTable(familiarData, columnNames);
      CakeArenaFrame.this.familiarTable.setRowHeight(40);

      for (int i = 0; i < 5; ++i) {
        CakeArenaFrame.this.familiarTable.setDefaultEditor(
            CakeArenaFrame.this.familiarTable.getColumnClass(i), null);
        CakeArenaFrame.this.familiarTable.setDefaultRenderer(
            CakeArenaFrame.this.familiarTable.getColumnClass(i), new OpponentRenderer());
      }

      JPanel familiarPanel = new JPanel(new BorderLayout());
      familiarPanel.add(CakeArenaFrame.this.familiarTable.getTableHeader(), BorderLayout.NORTH);
      familiarPanel.add(CakeArenaFrame.this.familiarTable, BorderLayout.CENTER);

      Object[][] opponentData = new Object[CakeArenaFrame.this.opponents.size()][5];

      // Register the data for your opponents to be rendered
      // in the table, taking into account the offset due to
      // your own familiar's data.

      for (int i = 0; i < CakeArenaFrame.this.opponents.size(); ++i) {
        opponentRace = CakeArenaFrame.this.opponents.get(i).getRace();
        opponentData[i][0] = CakeArenaFrame.this.opponents.get(i).toString();

        for (int j = 1; j <= 4; ++j) {
          Integer skill = FamiliarDatabase.getFamiliarSkill(opponentRace, j);
          if (skill == null) skill = 0;
          JButton opponentButton =
              new JButton(JComponentUtilities.getImage(skill.toString() + "star.gif"));
          opponentButton.addMouseListener(new OpponentListener(i, j, skill));
          opponentData[i][j] = opponentButton;
        }
      }

      JTable opponentTable = new JTable(opponentData, columnNames);
      opponentTable.addMouseListener(new TableButtonListener(opponentTable));
      opponentTable.setRowHeight(40);

      for (int i = 0; i < 5; ++i) {
        opponentTable.setDefaultEditor(opponentTable.getColumnClass(i), null);
        opponentTable.setDefaultRenderer(opponentTable.getColumnClass(i), new OpponentRenderer());
      }

      JPanel opponentPanel = new JPanel(new BorderLayout());
      opponentPanel.add(opponentTable.getTableHeader(), BorderLayout.NORTH);
      opponentPanel.add(opponentTable, BorderLayout.CENTER);

      this.add(familiarPanel, BorderLayout.NORTH);
      this.add(opponentPanel, BorderLayout.CENTER);
    }
  }

  private class OpponentListener extends ThreadedListener {
    private final int row, column;
    private final String opponentSkill;

    public OpponentListener(final int row, final int column, final Integer skill) {
      this.row = row;
      this.column = column;
      this.opponentSkill =
          skill.intValue() == 1 ? "1 star (opponent)" : skill + " stars (opponent)";
    }

    @Override
    protected void execute() {
      int yourSkillValue =
          FamiliarDatabase.getFamiliarSkill(KoLCharacter.getFamiliar().getRace(), this.column)
              .intValue();
      String yourSkill = yourSkillValue == 1 ? "1 star (yours)" : yourSkillValue + " stars (yours)";

      int battleCount =
          StringUtilities.parseInt(
              InputFieldUtilities.input(
                  "<html>"
                      + CakeArenaFrame.this.opponents.get(this.row).toString()
                      + ", "
                      + CakeArenaManager.eventIdToName(this.column)
                      + "<br>"
                      + yourSkill
                      + " vs. "
                      + this.opponentSkill
                      + "</html>"));

      if (battleCount > 0) {
        CakeArenaManager.fightOpponent(
            CakeArenaFrame.this.opponents.get(this.row).toString(), this.column, battleCount);
      }
    }
  }

  private static class OpponentRenderer implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        final JTable table,
        final Object value,
        final boolean isSelected,
        final boolean hasFocus,
        final int row,
        final int column) {
      return value == null ? this.getFamiliarComponent(column) : this.getStandardComponent(value);
    }

    private Component getFamiliarComponent(final int column) {
      FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

      if (column == 0) {
        return currentFamiliar == null
            ? this.getStandardComponent("NO DATA (0 lbs)")
            : this.getStandardComponent(currentFamiliar.toString());
      }

      return currentFamiliar == null
          ? new JLabel(JComponentUtilities.getImage("0star.gif"))
          : new JLabel(
              JComponentUtilities.getImage(
                  FamiliarDatabase.getFamiliarSkill(currentFamiliar.getRace(), column).toString()
                      + "star.gif"));
    }

    private Component getStandardComponent(final Object value) {
      if (value instanceof JButton) {
        return (JButton) value;
      }

      String name = value.toString();

      JPanel component = new JPanel(new BorderLayout());
      component.add(
          new JLabel(name.substring(0, name.indexOf("(") - 1), SwingConstants.CENTER),
          BorderLayout.CENTER);
      component.add(
          new JLabel(name.substring(name.indexOf("(")), SwingConstants.CENTER), BorderLayout.SOUTH);

      return component;
    }
  }
}
