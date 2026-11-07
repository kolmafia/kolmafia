package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CakeArenaManager;
import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.session.FamiliarManager;
import net.sourceforge.kolmafia.session.FamiliarTrainingManager;
import net.sourceforge.kolmafia.session.FamiliarTrainingManager.Goal;
import net.sourceforge.kolmafia.swingui.button.DisplayFrameButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

@SuppressWarnings("incomplete-switch")
public class FamiliarTrainingFrame extends GenericFrame {
  private final FamiliarTrainingPanel training;
  private CharacterListener weightListener;

  public FamiliarTrainingFrame() {
    super("Familiar Trainer");

    this.training = new FamiliarTrainingPanel();
    this.setCenterComponent(this.training);

    // Clear left over results from the buffer
    FamiliarTrainingManager.getResults().clear();
  }

  @Override
  public void updateDisplayState(final MafiaState displayState) {
    this.training.setEnabled(displayState != MafiaState.CONTINUE);
  }

  @Override
  public void dispose() {
    FamiliarTrainingManager.stopTraining();
    CharacterListenerRegistry.removeCharacterListener(this.weightListener);
    super.dispose();
  }

  private class FamiliarTrainingPanel extends JPanel {
    private FamiliarData familiar;
    private final JComboBox<FamiliarData> familiars;
    private final JLabel winCount;
    private final JLabel prizeCounter;
    private final JLabel totalWeight;

    private final OpponentsPanel opponentsPanel;
    private final ButtonPanel buttonPanel;
    private final ResultsPanel resultsPanel;

    public FamiliarTrainingPanel() {
      super(new BorderLayout(10, 10));

      JPanel container = new JPanel(new BorderLayout(10, 10));

      // Get current familiar
      this.familiar = KoLCharacter.getFamiliar();

      // Put familiar changer on top
      this.familiars = new ChangeComboBox(KoLCharacter.getFamiliarList());
      this.familiars.setRenderer(FamiliarData.getRenderer());
      container.add(this.familiars, BorderLayout.NORTH);

      // Put results in center
      this.resultsPanel = new ResultsPanel();
      container.add(this.resultsPanel, BorderLayout.CENTER);
      this.add(container, BorderLayout.CENTER);

      // Put opponents on left
      this.opponentsPanel = new OpponentsPanel();
      this.add(this.opponentsPanel, BorderLayout.WEST);

      // Put buttons on right
      JPanel buttonContainer = new JPanel(new BorderLayout());

      this.buttonPanel = new ButtonPanel();
      buttonContainer.add(this.buttonPanel, BorderLayout.NORTH);

      // List of counters at bottom
      JPanel counterPanel = new JPanel();
      counterPanel.setLayout(new GridLayout(3, 1));

      // First the win counter
      this.winCount = new JLabel("", JLabel.CENTER);
      counterPanel.add(this.winCount);

      // Next the prize counter
      this.prizeCounter = new JLabel("", JLabel.CENTER);
      counterPanel.add(this.prizeCounter);

      // Finally the total familiar weight
      this.totalWeight = new JLabel("", JLabel.CENTER);
      counterPanel.add(this.totalWeight);

      // Make a refresher for the counters
      FamiliarTrainingFrame.this.weightListener = new CharacterListener(new TotalWeightRefresher());
      CharacterListenerRegistry.addCharacterListener(FamiliarTrainingFrame.this.weightListener);

      // Show the counters
      buttonContainer.add(counterPanel, BorderLayout.SOUTH);

      this.add(buttonContainer, BorderLayout.EAST);

      this.add(new StatusPanel(), BorderLayout.SOUTH);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (this.buttonPanel == null || this.familiars == null) {
        return;
      }

      super.setEnabled(isEnabled);
      this.buttonPanel.setEnabled(isEnabled);
      this.familiars.setEnabled(isEnabled);
    }

    private class TotalWeightRefresher implements Runnable {
      public TotalWeightRefresher() {
        this.run();
      }

      @Override
      public void run() {
        // Arena wins
        int arenaWins = KoLCharacter.getArenaWins();
        FamiliarTrainingPanel.this.winCount.setText(arenaWins + " arena wins");

        // Wins to next prize
        int nextPrize = 5 - arenaWins % 5;
        FamiliarTrainingPanel.this.prizeCounter.setText(nextPrize + " wins to next prize");

        // Terrarium weight
        // Weight counts if above 1 or with experience, to a maximum of 20
        int totalTerrariumWeight = 0;

        FamiliarData[] familiarArray = KoLCharacter.ownedFamiliars().toArray(new FamiliarData[0]);

        for (FamiliarData familiarData : familiarArray) {
          if (familiarData.getWeight() != 1 || familiarData.getTotalExperience() > 0) {
            totalTerrariumWeight += Math.min(familiarData.getWeight(), 20);
          }
        }

        FamiliarTrainingPanel.this.totalWeight.setText(totalTerrariumWeight + " lb. terrarium");
      }
    }

    private class OpponentsPanel extends JPanel {
      public OpponentsPanel() {
        // Get current opponents
        LockableListModel<ArenaOpponent> opponents = CakeArenaManager.getOpponentList();
        int opponentCount = opponents.size();

        this.setLayout(new GridLayout(opponentCount, 1, 0, 20));

        for (int i = 0; i < opponentCount; ++i) {
          this.add(new OpponentLabel(opponents.get(i)));
        }
      }

      private class OpponentLabel extends JLabel {
        public OpponentLabel(final ArenaOpponent opponent) {
          super(
              "<html><center>"
                  + opponent.getName()
                  + "<br>"
                  + "("
                  + opponent.getWeight()
                  + " lbs)</center></html>",
              FamiliarDatabase.getFamiliarImage(opponent.getRace()),
              JLabel.CENTER);

          this.setVerticalTextPosition(JLabel.BOTTOM);
          this.setHorizontalTextPosition(JLabel.CENTER);
        }
      }
    }

    private class ButtonPanel extends JPanel {
      private final JButton matchup, base, buffed, turns, stop, save;
      // private JButton debug;
      final JButton learn, equip;

      public ButtonPanel() {
        JPanel containerPanel = new JPanel(new GridLayout(11, 1, 5, 5));

        this.matchup = new DisplayFrameButton("View Matchup", "CakeArenaFrame");
        containerPanel.add(this.matchup);

        containerPanel.add(new JLabel());

        this.base = new JButton("Train Base Weight");
        this.base.addActionListener(new BaseListener());
        containerPanel.add(this.base);

        this.buffed = new JButton("Train Buffed Weight");
        this.buffed.addActionListener(new BuffedListener());
        containerPanel.add(this.buffed);

        this.turns = new JButton("Train for Set Turns");
        this.turns.addActionListener(new TurnsListener());
        containerPanel.add(this.turns);

        containerPanel.add(new JLabel());

        this.stop = new JButton("Stop Training");
        this.stop.addActionListener(new StopListener());
        containerPanel.add(this.stop);

        this.save = new JButton("Save Transcript");
        this.save.addActionListener(new SaveListener());
        containerPanel.add(this.save);

        containerPanel.add(new JLabel());

        this.learn = new JButton("Learn Familiar Strengths");
        this.learn.addActionListener(new LearnListener());
        containerPanel.add(this.learn);

        this.equip = new JButton("Equip All Familiars");
        this.equip.addActionListener(new EquipAllListener());
        containerPanel.add(this.equip);

        // debug = new JButton( "Debug" );
        // debug.addActionListener( new DebugListener() );
        // this.add( debug );

        this.add(containerPanel);
      }

      @Override
      public void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);

        if (this.matchup != null) {
          this.matchup.setEnabled(isEnabled);
        }
        if (this.base != null) {
          this.base.setEnabled(isEnabled);
        }
        if (this.buffed != null) {
          this.buffed.setEnabled(isEnabled);
        }
        if (this.turns != null) {
          this.turns.setEnabled(isEnabled);
        }
        // this.stop is always enabled
        if (this.save != null) {
          this.save.setEnabled(isEnabled);
        }
        if (this.learn != null) {
          this.learn.setEnabled(isEnabled);
        }
        if (this.equip != null) {
          this.equip.setEnabled(isEnabled);
        }
      }

      private class BaseListener extends ThreadedListener {
        @Override
        protected void execute() {
          // Prompt for goal
          Integer value = InputFieldUtilities.getQuantity("Train up to what base weight?", 20, 20);
          int goal = (value == null) ? 0 : value;

          // Quit if canceled
          if (goal == 0) {
            return;
          }

          // Level the familiar

          FamiliarTrainingManager.levelFamiliar(goal, Goal.BASE);
        }
      }

      private class BuffedListener extends ThreadedListener {
        @Override
        protected void execute() {
          // Prompt for goal
          Integer value =
              InputFieldUtilities.getQuantity("Train up to what buffed weight?", 48, 20);
          int goal = (value == null) ? 0 : value;

          // Quit if canceled
          if (goal == 0) {
            return;
          }

          // Level the familiar

          FamiliarTrainingManager.levelFamiliar(goal, Goal.BUFFED);
        }
      }

      private class TurnsListener extends ThreadedListener {
        @Override
        protected void execute() {
          // Prompt for goal
          Integer value =
              InputFieldUtilities.getQuantity("Train for how many turns?", Integer.MAX_VALUE, 1);
          int goal = (value == null) ? 0 : value;

          // Quit if canceled
          if (goal == 0) {
            return;
          }

          // Level the familiar

          FamiliarTrainingManager.levelFamiliar(goal, Goal.TURNS);
        }
      }

      private class StopListener extends ThreadedListener {
        @Override
        protected void execute() {
          FamiliarTrainingManager.stopTraining();
        }
      }

      private class SaveListener extends ThreadedListener {
        File output = null;
        String content = null;

        @Override
        protected void execute() {
          this.output = null;

          try {
            SwingUtilities.invokeAndWait(
                new Runnable() {
                  @Override
                  public void run() {
                    SaveListener.this.output =
                        InputFieldUtilities.chooseOutputFile(
                            KoLConstants.DATA_LOCATION, FamiliarTrainingFrame.this);
                    SaveListener.this.content =
                        FamiliarTrainingManager.getResults().getHTMLContent();
                  }
                });
          } catch (Exception ie) {
          }

          if (output == null) {
            return;
          }

          try {
            try (PrintStream ostream = LogStream.openStream(output, false)) {
              ostream.println(this.content.replaceAll("<br>", KoLConstants.LINE_BREAK));
            }
          } catch (Exception ex) {
            // This should not happen.  Therefore, print
            // a stack trace for debug purposes.

            StaticEntity.printStackTrace(ex);
          }
        }
      }

      private class LearnListener extends ThreadedListener {
        @Override
        protected void execute() {
          if (FamiliarTrainingPanel.this.familiar == FamiliarData.NO_FAMILIAR) {
            return;
          }

          // Prompt for trials
          Integer value =
              InputFieldUtilities.getQuantity("How many trials per event per rank?", 20, 10);
          int trials = (value == null) ? 0 : value;

          // Quit if canceled
          if (trials == 0) {
            return;
          }

          // Nag dialog
          int turns = trials * 12;
          if (!InputFieldUtilities.confirm(
              "This will take up to "
                  + turns
                  + " adventures and cost up to "
                  + KoLConstants.COMMA_FORMAT.format(turns * 100)
                  + " meat. Are you sure?")) {
            return;
          }

          // Learn familiar parameters

          int[] skills = FamiliarTrainingManager.learnFamiliarParameters(trials);

          // Save familiar parameters

          if (skills != null) {
            int[] original =
                FamiliarDatabase.getFamiliarSkills(FamiliarTrainingPanel.this.familiar.getId());
            boolean changed = false;

            for (int i = 0; i < original.length; ++i) {
              if (skills[i] != original[i]) {
                changed = true;
                break;
              }
            }

            if (changed
                && InputFieldUtilities.confirm(
                    "Save arena parameters for the "
                        + FamiliarTrainingPanel.this.familiar.getRace()
                        + "?")) {
              FamiliarDatabase.setFamiliarSkills(
                  FamiliarTrainingPanel.this.familiar.getId(), skills);
            }

            KoLmafia.updateDisplay(
                MafiaState.CONTINUE,
                "Learned skills are "
                    + (changed ? "different from" : "the same as")
                    + " those in familiar database.");
          }
        }
      }

      private class EquipAllListener extends ThreadedListener {
        @Override
        protected void execute() {
          FamiliarManager.equipAllFamiliars();
        }
      }

      /*
       * private class DebugListener extends ThreadedListener { public void run() { debug(); } }
       */
    }

    private class ResultsPanel extends JPanel {
      RequestPane resultsDisplay;

      public ResultsPanel() {
        this.setLayout(new BorderLayout(10, 10));

        RequestPane resultsDisplay = new RequestPane();
        JScrollPane scroller = FamiliarTrainingManager.getResults().addDisplay(resultsDisplay);
        JComponentUtilities.setComponentSize(scroller, 400, 400);

        this.add(scroller, BorderLayout.CENTER);
      }
    }

    private class ChangeComboBox extends JComboBox<FamiliarData> {
      public ChangeComboBox(final LockableListModel<FamiliarData> selector) {
        super(selector);
        this.addActionListener(new ChangeComboBoxListener());
      }

      private class ChangeComboBoxListener extends ThreadedListener {
        @Override
        protected void execute() {
          FamiliarData selection = (FamiliarData) ChangeComboBox.this.getSelectedItem();

          if (selection == null || selection == FamiliarTrainingPanel.this.familiar) {
            return;
          }

          RequestThread.postRequest(new FamiliarRequest(selection));
          FamiliarTrainingPanel.this.familiar = KoLCharacter.getFamiliar();
        }
      }
    }
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

  public static class LocalSettingChanger extends JButton implements ActionListener {
    private final String title;
    private final String property;

    public LocalSettingChanger(final String title, final String property) {
      super(title);

      this.title = title;
      this.property = property;

      // Turn everything off and back on again
      // so that it's off to start.

      this.actionPerformed(null);
      this.actionPerformed(null);

      this.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      boolean toggleValue = Preferences.getBoolean(this.property);
      Preferences.setBoolean(this.property, toggleValue);

      if (toggleValue) {
        this.setText("Turn Off " + this.title);
      } else {
        this.setText("Turn On " + this.title);
      }
    }
  }
}
