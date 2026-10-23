package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StoreManageFrameTest {
  private JTable table;

  @BeforeEach
  public void setUp() {
    DefaultTableModel model =
        new DefaultTableModel(new Object[][] {{"item", 0}}, new String[] {"Name", "Lim"}) {
          @Override
          public Class<?> getColumnClass(int column) {
            return column == 1 ? Integer.class : String.class;
          }
        };
    this.table = new JTable(model);
  }

  @Nested
  class LimitEditorTest {
    private StoreManageFrame.LimitEditor editor;

    @BeforeEach
    public void setUp() {
      this.editor = new StoreManageFrame.LimitEditor();
    }

    @Test
    public void noLimitIsEditedAsABlankField() {
      JTextField field = this.fieldFor(0);
      assertThat(field.getText(), equalTo(""));
    }

    @Test
    public void aLimitIsEditedAsThatNumber() {
      JTextField field = this.fieldFor(25);
      assertThat(field.getText(), equalTo("25"));
    }

    @Test
    public void aBlankFieldMeansNoLimit() {
      JTextField field = this.fieldFor(25);
      field.setText("");
      assertThat(this.editor.getCellEditorValue(), equalTo(0));
    }

    @Test
    public void aNumberMeansThatLimit() {
      JTextField field = this.fieldFor(0);
      field.setText("42");
      assertThat(this.editor.getCellEditorValue(), equalTo(42));
    }

    @Test
    public void nonsenseMeansNoLimit() {
      JTextField field = this.fieldFor(25);
      field.setText("a handful");
      assertThat(this.editor.getCellEditorValue(), equalTo(0));
    }

    private JTextField fieldFor(int limit) {
      Component component =
          this.editor.getTableCellEditorComponent(
              StoreManageFrameTest.this.table, limit, false, 0, 1);
      assertThat(component, instanceOf(JTextField.class));
      return (JTextField) component;
    }
  }

  @Nested
  class LimitRendererTest {
    @Test
    public void noLimitIsRenderedAsNothing() {
      assertThat(render(0), equalTo(""));
    }

    @Test
    public void aLimitIsRenderedAsThatNumber() {
      assertThat(render(25), equalTo("25"));
    }

    private String render(int limit) {
      Component component =
          new StoreManageFrame.LimitRenderer()
              .getTableCellRendererComponent(
                  StoreManageFrameTest.this.table, limit, false, false, 0, 1);
      assertThat(component, instanceOf(JLabel.class));
      return ((JLabel) component).getText();
    }
  }
}
