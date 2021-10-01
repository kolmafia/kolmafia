package net.sourceforge.kolmafia.swingui.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class AddCustomDeedsPanel extends JPanel {
  public static JFrame builderFrame;
  public static CardLayoutSelectorPanel selectorPanel;

  public AddCustomDeedsPanel() {
    AddCustomDeedsPanel.builderFrame = new JFrame("Building Custom Deed");

    buildCustomDeed();

    AddCustomDeedsPanel.builderFrame.getContentPane().add(AddCustomDeedsPanel.selectorPanel);
    AddCustomDeedsPanel.builderFrame.pack();
    AddCustomDeedsPanel.builderFrame.setResizable(false);
    AddCustomDeedsPanel.builderFrame.setLocationRelativeTo(null);
    AddCustomDeedsPanel.builderFrame.setVisible(true);
  }

  private void buildCustomDeed() {
    AddCustomDeedsPanel.selectorPanel = new CardLayoutSelectorPanel();
    AddCustomDeedsPanel.selectorPanel.addCategory("Custom Deeds");

    new SimpleDeedConstructor();
    new CommandDeedConstructor();
    new ItemDeedConstructor();
    new SkillDeedConstructor();
    new TextDeedConstructor();

    AddCustomDeedsPanel.selectorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  }

  private abstract class CustomDeedConstructor {
    final String kind;

    final JPanel panel;

    protected ThreadedButton button;

    final JPanel textPanel;
    final GridBagConstraints c;

    public final JTextField[] fields;
    public final JLabel[] labels;

    public final String[] defaultLabels;
    public final String[] defaultTooltips;

    public CustomDeedConstructor(final String kind, final int fieldCount) {
      this.kind = kind;

      this.panel = new JPanel();
      this.panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

      final JPanel title = new JPanel();
      title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
      title.add(new JLabel("Adding " + this.kind + " deed."));
      this.panel.add(title);

      this.fields = new JTextField[fieldCount];
      for (int i = 0; i < fieldCount; ++i) {
        this.fields[i] = new JTextField(25);
      }
      this.labels = new JLabel[fieldCount];
      this.defaultLabels = new String[fieldCount];
      this.defaultTooltips = new String[fieldCount];

      this.textPanel = new JPanel(new GridBagLayout());
      this.c = new GridBagConstraints();

      this.c.fill = GridBagConstraints.HORIZONTAL;
      this.c.gridx = 0;
      this.c.gridy = 0;
      this.c.weighty = 0.5;
      this.c.anchor = GridBagConstraints.NORTH;
      this.c.gridwidth = 3;
      this.c.gridheight = 1;
      textPanel.add(new JSeparator(), this.c);
    }

    protected void setFieldNames(String[] fieldNames) {
      this.c.fill = GridBagConstraints.NONE;
      this.c.weighty = 0.5;
      this.c.gridwidth = 1;
      this.c.gridheight = 1;
      this.c.anchor = GridBagConstraints.EAST;
      this.c.gridx = 0;

      for (int i = 0; i < this.fields.length; ++i) {
        String text = i < fieldNames.length ? fieldNames[i] : null;

        this.c.gridy = i + 1;

        this.textPanel.add(new JLabel(text), this.c);
      }
    }

    protected void setFields() {
      this.c.fill = GridBagConstraints.HORIZONTAL;
      this.c.weighty = 0.5;
      this.c.gridwidth = GridBagConstraints.RELATIVE;
      this.c.gridheight = 1;
      this.c.anchor = GridBagConstraints.CENTER;
      this.c.gridx = 1;

      for (int i = 0; i < this.fields.length; ++i) {
        this.c.gridy = i + 1;

        this.textPanel.add(this.fields[i], this.c);
      }
    }

    /**
     * @param requiredAmount The label of the <input> first will be set as "required". The rest will
     *     be "(optional)".
     * @param tooltips The ordered tooltip texts.
     */
    protected void setLabels(int requiredAmount, String[] tooltips) {
      this.c.fill = GridBagConstraints.HORIZONTAL;
      this.c.weighty = 0.5;
      this.c.gridwidth = 1;
      this.c.gridheight = 1;
      this.c.anchor = GridBagConstraints.CENTER;
      this.c.gridx = 2;

      for (int i = 0; i < this.fields.length; ++i) {
        String text = i < requiredAmount ? "required" : "(optional)";
        JLabel label = new JLabel(text);

        this.labels[i] = label;
        this.defaultLabels[i] = text;

        if (i < tooltips.length) {
          label.setToolTipText(tooltips[i]);
          this.defaultTooltips[i] = tooltips[i];
        }

        this.c.gridy = i + 1;

        this.textPanel.add(label, this.c);
      }
    }

    protected void setBottom() {
      this.setBottom(this.fields.length + 1);
    }

    protected void setBottom(int gridy) {
      this.c.fill = GridBagConstraints.HORIZONTAL;
      this.c.weighty = 0.5;
      this.c.gridwidth = 1;
      this.c.gridheight = 1;
      this.c.anchor = GridBagConstraints.CENTER;
      this.c.gridy = gridy;

      for (int i : new int[] {0, 2}) {
        this.c.gridx = i;

        this.textPanel.add(Box.createHorizontalStrut(75), this.c);
      }

      this.c.fill = GridBagConstraints.NONE;
      this.c.anchor = GridBagConstraints.SOUTHEAST;
      this.c.gridx = 1;

      this.textPanel.add(this.button, this.c);
      this.button.setEnabled(false);
    }

    protected void addToSelector() {
      this.panel.add(this.textPanel);
      AddCustomDeedsPanel.selectorPanel.addPanel("- " + this.kind, this.panel);
    }

    public class FieldListener implements DocumentListener {
      private final int fieldIndex;

      private final boolean isIntegerField;

      public FieldListener(int fieldIndex) {
        this(fieldIndex, false);
      }

      public FieldListener(int fieldIndex, boolean isIntegerField) {
        this.fieldIndex = fieldIndex;
        this.isIntegerField = isIntegerField;
      }

      public void changedUpdate(DocumentEvent e) {
        String fieldText = CustomDeedConstructor.this.fields[this.fieldIndex].getText();
        JLabel label = CustomDeedConstructor.this.labels[this.fieldIndex];

        // Reset to default if field is empty
        if (fieldText.isEmpty()) {
          resetLabel(this.fieldIndex);
        } else {
          label.setText("OK");
          label.setToolTipText(null);

          if (this.isIntegerField) {
            try {
              Integer.parseInt(fieldText);
            } catch (NumberFormatException exception) {
              label.setText("BAD");
              label.setToolTipText("Integer only, please.");
            }
          }
        }

        updateButton();
      }

      public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
      }

      public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
      }

      public void updateButton() {
        boolean enableButton = true;

        for (JLabel label : CustomDeedConstructor.this.labels) {
          String text = label.getText();
          enableButton &= text == "OK" || text == "(optional)";
        }

        CustomDeedConstructor.this.button.setEnabled(enableButton);
      }

      protected void setLabel(int labelIndex, String labelText) {
        setLabel(labelIndex, labelText, null);
      }

      protected void setLabel(int labelIndex, String labelText, String tip) {
        JLabel label = CustomDeedConstructor.this.labels[labelIndex];
        label.setText(labelText);
        label.setToolTipText(tip);
      }

      protected void resetLabel(int labelIndex) {
        setLabel(
            labelIndex,
            CustomDeedConstructor.this.defaultLabels[labelIndex],
            CustomDeedConstructor.this.defaultTooltips[labelIndex]);
      }
    }

    public class CustomActionRunnable implements Runnable {
      public void run() {
        String deed = "$CUSTOM|" + CustomDeedConstructor.this.kind;

        for (JTextField field : CustomDeedConstructor.this.fields) {
          deed += "|" + field.getText().replaceAll(",", ",|");
        }

        this.submitNewDeed(deed);
      }

      public void submitNewDeed(String deed) {
        String oldString = Preferences.getString("dailyDeedsOptions");
        Preferences.setString("dailyDeedsOptions", oldString + "," + deed);

        RequestLogger.printLine("Custom deed added: " + deed);
        CustomDeedConstructor.this.button.setEnabled(false);
      }
    }
  }

  private class SimpleDeedConstructor extends CustomDeedConstructor {
    public SimpleDeedConstructor() {
      super("Simple", 5);

      this.button = new ThreadedButton("add deed", new CustomActionRunnable());

      this.setFieldNames(
          new String[] {"displayText:", "command:", "maxUses:", "toolTip:", "compMessage:"});
      this.setFields();
      this.setLabels(
          1,
          new String[] {
            "The text to display on the button.",
            "The command that the button will execute.",
            "<html>Provide an integer to disable the button at.<br>The button will be enabled until the preference reaches this number.</html>",
            "The tooltip when moused over.",
            "Message to display when no more can be done today."
          });
      this.setBottom();

      this.fields[0].getDocument().addDocumentListener(new FieldListener(0));
      this.fields[1].getDocument().addDocumentListener(new FieldListener(1));
      this.fields[2].getDocument().addDocumentListener(new FieldListener(2, true));
      this.fields[3].getDocument().addDocumentListener(new FieldListener(3));
      this.fields[4].getDocument().addDocumentListener(new FieldListener(4));

      this.addToSelector();
    }
  }

  private class CommandDeedConstructor extends CustomDeedConstructor {
    public CommandDeedConstructor() {
      super("Command", 6);

      this.button = new ThreadedButton("add deed", new CustomActionRunnable());

      this.setFieldNames(
          new String[] {
            "displayText:", "preference:", "command:", "maxUses:", "toolTip:", "compMessage:"
          });
      this.setFields();
      this.setLabels(
          2,
          new String[] {
            "The text to display on the button.",
            "The preference that the button will track.",
            "The command that the button will execute.",
            "<html>Provide an integer to disable the button at.<br>The button will be enabled until the preference reaches this number.</html>",
            "The tooltip when moused over.",
            "Message to display when no more can be done today."
          });
      this.setBottom();

      this.fields[0].getDocument().addDocumentListener(new FieldListener(0));
      this.fields[1].getDocument().addDocumentListener(new FieldListener(1));
      this.fields[2].getDocument().addDocumentListener(new FieldListener(2));
      this.fields[3].getDocument().addDocumentListener(new FieldListener(3, true));
      this.fields[4].getDocument().addDocumentListener(new FieldListener(4));
      this.fields[5].getDocument().addDocumentListener(new FieldListener(5));

      this.addToSelector();
    }
  }

  private class ItemDeedConstructor extends CustomDeedConstructor {
    public ItemDeedConstructor() {
      super("Item", 6);

      this.button = new ThreadedButton("add deed", new CustomActionRunnable());

      this.setFieldNames(
          new String[] {
            "displayText:", "preference:", "item:", "maxUses:", "toolTip:", "compMessage:"
          });
      this.setFields();
      this.setLabels(
          2,
          new String[] {
            "The text to display on the button.",
            "The preference that the button will track.",
            "<html>If an item is not specified, defaults to displayText.  Uses fuzzy matching.<br>Can add arbitrary GCLI commands by following the item name with a semi-colon.</html>",
            "<html>Provide an integer to disable the button at.<br>The button will be enabled until the preference reaches this number.</html>",
            "The tooltip when moused over.",
            "Message to display when no more can be done today."
          });
      this.setBottom();

      this.fields[0].getDocument().addDocumentListener(new ItemSpecificListener(0));
      this.fields[1].getDocument().addDocumentListener(new FieldListener(1));
      this.fields[2].getDocument().addDocumentListener(new ItemSpecificListener(2));
      this.fields[3].getDocument().addDocumentListener(new FieldListener(3, true));
      this.fields[4].getDocument().addDocumentListener(new FieldListener(4));
      this.fields[5].getDocument().addDocumentListener(new FieldListener(5));

      this.addToSelector();
    }

    private class ItemSpecificListener extends FieldListener {
      public ItemSpecificListener(int fieldIndex) {
        super(fieldIndex);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        String field0 = ItemDeedConstructor.this.fields[0].getText();
        String field2 = ItemDeedConstructor.this.fields[2].getText();

        boolean field0empty = field0.isEmpty();
        boolean field2empty = field2.isEmpty();
        String[] field2split = field2.split(";", 2);
        int field0matchID = ItemDatabase.getItemId(field0);
        int field2matchID = ItemDatabase.getItemId(field2split[0]);
        String field2command = field2split.length > 1 ? field2split[1] : "";
        boolean field0matching = field0matchID != -1;
        boolean field2matching = field2matchID != -1;
        String field0matchItem = ItemDatabase.getItemName(field0matchID);
        String field2matchItem = ItemDatabase.getItemName(field2matchID);

        /*
         * Since the states of field 0 and field 2 depend on each other, set the states of both
         * whenever one of the fields is changed.
         *
         * State 1: displayText empty, item empty = [ required, (optional) ]
         * State 2: displayText non-matching, item empty = [ (need item), required ]
         * State 3: displayText matching, item empty = [ OK, (optional) ]
         * State 4: displayText empty, item non-matching = [ required, BAD ]
         * State 5: displayText empty, item matching = [ required, OK ]
         * State 6: displayText non-empty, item non-matching = [ OK, BAD ]
         * State 7: displayText non-empty, item matching = [ OK, OK ]
         */

        /* State 1 */
        if (field0empty && field2empty) {
          this.resetLabel(0);
          this.resetLabel(2);
        }

        /* State 2 */
        else if (!field0matching && field2empty) {
          this.setLabel(
              0,
              "(need item)",
              "The display text does not match an item, so you need to specify one under item:");
          this.setLabel(
              2,
              "required",
              "The display text does not match an item, so you need to specify one.");
        }

        /* State 3 */
        else if (field0matching && field2empty) {
          this.setLabel(0, "OK", "Display text matches item: " + field0matchItem);
          this.setLabel(
              2,
              "(optional)",
              "The display text matches an item, so you don't need to specify one here.");
        }

        /* State 4 */
        else if (field0empty && !field2matching) {
          this.resetLabel(0);
          this.setLabel(2, "BAD", "Could not find a matching item for: " + field2split[0]);
        }

        /* State 5 */
        else if (field0empty && field2matching) {
          String tooltipText =
              field2command != ""
                  ? "<html>Command will be: &nbsp; &nbsp; use "
                      + field2matchItem
                      + "<br>Followed by: &nbsp; &nbsp; "
                      + field2command
                      + "</html>"
                  : "Matching item found: " + field2matchItem;

          this.setLabel(0, "required", "You still need to specify the text to display.");
          this.setLabel(2, "OK", tooltipText);
        }

        /* State 6 */
        else if (!field0empty && !field2matching) {
          this.setLabel(0, "OK");
          this.setLabel(2, "BAD", "Could not find a matching item for: " + field2split[0]);
        }

        /* State 7 */
        else if (!field0empty && field2matching) {
          String tooltipText =
              field2command != ""
                  ? "<html>Command will be: &nbsp; &nbsp; use "
                      + field2matchItem
                      + "<br>Followed by: &nbsp; &nbsp; "
                      + field2command
                      + "</html>"
                  : "Matching item found: " + field2matchItem;

          this.setLabel(0, "OK");
          this.setLabel(2, "OK", tooltipText);
        }

        updateButton();
      }
    }
  }

  private class SkillDeedConstructor extends CustomDeedConstructor {
    public SkillDeedConstructor() {
      super("Skill", 6);

      this.button = new ThreadedButton("add deed", new CustomActionRunnable());

      this.setFieldNames(
          new String[] {
            "displayText:", "preference:", "skill:", "maxCasts:", "toolTip:", "compMessage:"
          });
      this.setFields();
      this.setLabels(
          2,
          new String[] {
            "The text to display on the button.",
            "The preference that the button will track.",
            "The skill that the button will cast.",
            "<html>Provide an integer to disable the button at.<br>The button will be enabled until the preference reaches this number.</html>",
            "The tooltip when moused over.",
            "Message to display when no more can be done today."
          });
      this.setBottom();

      this.fields[0].getDocument().addDocumentListener(new SkillSpecificListener(0));
      this.fields[1].getDocument().addDocumentListener(new FieldListener(1));
      this.fields[2].getDocument().addDocumentListener(new SkillSpecificListener(2));
      this.fields[3].getDocument().addDocumentListener(new FieldListener(3, true));
      this.fields[4].getDocument().addDocumentListener(new FieldListener(4));
      this.fields[5].getDocument().addDocumentListener(new FieldListener(5));

      this.addToSelector();
    }

    private class SkillSpecificListener extends FieldListener {
      public SkillSpecificListener(int fieldIndex) {
        super(fieldIndex);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        String field0 = SkillDeedConstructor.this.fields[0].getText();
        String field2 = SkillDeedConstructor.this.fields[2].getText();

        boolean field0empty = field0.isEmpty();
        boolean field2empty = field2.isEmpty();
        List<String> field0matches = SkillDatabase.getMatchingNames(field0);
        List<String> field2matches = SkillDatabase.getMatchingNames(field2);
        boolean field0matching = field0matches.size() == 1;
        boolean field2matching = field2matches.size() == 1;

        /*
         * Since the states of field 0 and field 2 depend on each other, set the states of both
         * whenever one of the fields is changed.
         *
         * State 1: displayText empty, skill empty = [ required, (optional) ]
         * State 2: displayText non-matching, skill empty = [ (need skill), required ]
         * State 3: displayText matching, skill empty = [ OK, (optional) ]
         * State 4: displayText empty, skill non-matching = [ required, BAD ]
         * State 5: displayText empty, skill matching = [ required, OK ]
         * State 6: displayText non-empty, skill non-matching = [ OK, BAD ]
         * State 7: displayText non-empty, skill matching = [ OK, OK ]
         */

        /* State 1 */
        if (field0empty && field2empty) {
          this.resetLabel(0);
          this.resetLabel(2);
        }

        /* State 2 */
        else if (!field0matching && field2empty) {
          String badMatchText =
              field0matches.size() == 0 ? "does not match a skill" : "matches too many skills";

          this.setLabel(
              0,
              "(need skill)",
              "The display text " + badMatchText + ", so you need to specify one under skill:");
          this.setLabel(
              2, "required", "The display text " + badMatchText + ", so you need to specify one.");
        }

        /* State 3 */
        else if (field0matching && field2empty) {
          this.setLabel(0, "OK", "Display text matches skill: " + field0matches.get(0));
          this.setLabel(
              2,
              "(optional)",
              "The display text matches a skill, so you don't need to specify one here.");
        }

        /* State 4 */
        else if (field0empty && !field2matching) {
          String badMatchText =
              field2matches.size() == 0
                  ? "Could not find a matching skill"
                  : "Found too many matching skills";

          this.resetLabel(0);
          this.setLabel(2, "BAD", badMatchText + " for: " + field2);
        }

        /* State 5 */
        else if (field0empty && field2matching) {
          this.setLabel(0, "required", "You still need to specify the text to display.");
          this.setLabel(2, "OK", "Matching skill found: " + field2matches.get(0));
        }

        /* State 6 */
        else if (!field0empty && !field2matching) {
          String badMatchText =
              field2matches.size() == 0
                  ? "Could not find a matching skill"
                  : "Found too many matching skills";

          this.setLabel(0, "OK");
          this.setLabel(2, "BAD", badMatchText + " for: " + field2);
        }

        /* State 7 */
        else if (!field0empty && field2matching) {
          this.setLabel(0, "OK");
          this.setLabel(2, "OK", "Matching skill found: " + field2matches.get(0));
        }

        updateButton();
      }
    }
  }

  private class TextDeedConstructor extends CustomDeedConstructor {
    protected JTextArea textArea;

    protected ThreadedButton addTextButton;
    protected ThreadedButton undoButton;
    protected ThreadedButton clearButton;

    protected final List<String> textDeed = new ArrayList<String>();

    public TextDeedConstructor() {
      super("Text", 1);

      this.button = new ThreadedButton("add deed", new TextActionRunnable());

      this.addTextArea();
      this.setFields();

      this.addTextButton = new ThreadedButton("add text", new AddTextRunnable());
      this.undoButton = new ThreadedButton("undo", new RemoveLastTextRunnable());
      this.clearButton = new ThreadedButton("clear", new ClearTextRunnable());

      this.setBottom(5);
      this.addSideButtons();

      this.fields[0].getDocument().addDocumentListener(new TextSpecificListener(0));

      this.addToSelector();
    }

    private void addTextArea() {
      this.textArea = new JTextArea();
      this.textArea.setColumns(10);
      this.textArea.setRows(4);
      this.textArea.setMaximumSize(this.textArea.getPreferredSize());
      this.textArea.setBorder(BorderFactory.createLoweredBevelBorder());
      this.textArea.setLineWrap(true);
      this.textArea.setWrapStyleWord(true);
      this.textArea.setEditable(false);
      this.textArea.setOpaque(false);
      this.textArea.setFont(KoLGUIConstants.DEFAULT_FONT);

      this.c.fill = GridBagConstraints.BOTH;
      this.c.weightx = 1;
      this.c.weighty = 1;
      this.c.gridwidth = 1;
      this.c.gridheight = 2;
      this.c.anchor = GridBagConstraints.CENTER;
      this.c.gridx = 1;
      this.c.gridy = 2;
      this.textPanel.add(
          new GenericScrollPane(
              this.textArea,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
          this.c);
    }

    private void addSideButtons() {
      this.c.fill = GridBagConstraints.NONE;
      this.c.weighty = 0.5;
      this.c.gridwidth = 1;
      this.c.gridheight = 1;
      this.c.gridx = 2;

      this.c.gridy = 1;
      this.c.anchor = GridBagConstraints.WEST;
      this.textPanel.add(this.addTextButton, this.c);
      this.addTextButton.setEnabled(false);

      this.c.gridy = 2;
      this.c.anchor = GridBagConstraints.WEST;
      this.textPanel.add(this.undoButton, this.c);
      this.undoButton.setEnabled(false);

      this.c.gridy = 3;
      this.c.anchor = GridBagConstraints.NORTHWEST;
      this.textPanel.add(this.clearButton, this.c);
      this.clearButton.setEnabled(false);
    }

    public void buildTextArea() {
      String display = "";

      for (String piece : this.textDeed) {
        for (String section : piece.split("\\|")) {
          if (section == null || section.isEmpty()) {
            continue;
          } else if (Preferences.getString(section).isEmpty()) {
            display += section;
          } else {
            display += Preferences.getString(section);
          }
        }
      }

      boolean textAreaHasText = !this.textDeed.isEmpty();
      this.button.setEnabled(textAreaHasText);
      this.undoButton.setEnabled(textAreaHasText);
      this.clearButton.setEnabled(textAreaHasText);

      this.textArea.setText(display);
    }

    public class AddTextRunnable extends CustomActionRunnable {
      @Override
      public void run() {
        JTextField textField = TextDeedConstructor.this.fields[0];
        String fieldText = textField.getText();
        List<String> textDeed = TextDeedConstructor.this.textDeed;

        textDeed.add(fieldText.replaceAll(",", ",|"));

        TextDeedConstructor.this.buildTextArea();

        textField.setText("");
        TextDeedConstructor.this.addTextButton.setEnabled(false);
      }
    }

    public class RemoveLastTextRunnable extends CustomActionRunnable {
      @Override
      public void run() {
        List<String> textDeed = TextDeedConstructor.this.textDeed;

        textDeed.remove(textDeed.size() - 1);

        TextDeedConstructor.this.buildTextArea();
      }
    }

    public class ClearTextRunnable extends CustomActionRunnable {
      @Override
      public void run() {
        TextDeedConstructor.this.textDeed.clear();

        TextDeedConstructor.this.buildTextArea();
      }
    }

    private class TextActionRunnable extends CustomActionRunnable {
      @Override
      public void run() {
        List<String> textDeed = TextDeedConstructor.this.textDeed;
        String deed = "$CUSTOM|" + TextDeedConstructor.this.kind + "|";

        deed += String.join("|", textDeed);

        this.submitNewDeed(deed);
        textDeed.clear();

        TextDeedConstructor.this.buildTextArea();
      }
    }

    private class TextSpecificListener extends FieldListener {
      public TextSpecificListener(int fieldIndex) {
        super(fieldIndex);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        boolean enableAddTextButton = !TextDeedConstructor.this.fields[0].getText().isEmpty();

        SwingUtilities.invokeLater(
            () -> {
              TextDeedConstructor.this.addTextButton.setEnabled(enableAddTextButton);
            });
      }
    }
  }
}
