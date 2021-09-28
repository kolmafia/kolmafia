package net.sourceforge.kolmafia.swingui.widget;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoHighlightSpinner extends JSpinner {
  private final AutoHighlightNumberEditor editor;

  public AutoHighlightSpinner() {
    super();
    this.editor = new AutoHighlightNumberEditor(this);
    this.setEditor(editor);
  }

  public void setValue(int value) {
    this.setValue(IntegerPool.get(value));
  }

  public void setHorizontalAlignment(int alignment) {
    this.editor.setHorizontalAlignment(alignment);
  }

  private class AutoHighlightNumberEditor extends AutoHighlightTextField implements ChangeListener {
    private boolean changing = true;

    public AutoHighlightNumberEditor(JSpinner spinner) {
      super();
      AutoHighlightSpinner.this.addChangeListener(this);
      this.getDocument().addDocumentListener(new AutoHighlightNumberEditorDocumentListener());
      this.setText("0");
      this.changing = false;
    }

    public void stateChanged(ChangeEvent evt) {
      if (this.changing) {
        return;
      }

      Integer value = (Integer) AutoHighlightSpinner.this.getValue();
      this.setText(String.valueOf(value));
    }

    private class AutoHighlightNumberEditorDocumentListener implements DocumentListener {
      public void changedUpdate(DocumentEvent e) {}

      public void insertUpdate(DocumentEvent e) {
        this.updateSpinnerModel();
      }

      public void removeUpdate(DocumentEvent e) {
        this.updateSpinnerModel();
      }

      private void updateSpinnerModel() {
        try {
          String text = AutoHighlightNumberEditor.this.getText();
          int value = StringUtilities.parseInt(text);
          AutoHighlightNumberEditor.this.changing = true;
          AutoHighlightSpinner.this.setValue(IntegerPool.get(value));
          AutoHighlightNumberEditor.this.changing = false;
        } catch (NumberFormatException e) {
        }
      }
    }
  }
}
