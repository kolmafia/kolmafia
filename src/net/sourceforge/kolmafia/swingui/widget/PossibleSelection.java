package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.sourceforge.kolmafia.utilities.IntWrapper;

/**
 * A fixed value with a label and extended description that updates its {@link IntWrapper} in its
 * <code>actionPerformed</code>.
 */
public class PossibleSelection implements ActionListener {
  private String label;
  private String description;
  private int value;
  private IntWrapper wrapper;

  /**
   * Sole constructor.
   *
   * @param initLabel a string (treated as containing HTML)
   * @param initDescription a string (treated as containing HTML)
   * @param initValue the integer value to assign to the wrapper when this value is selected
   * @param initWrapper the {@link IntWrapper} to update when this value is selected
   */
  public PossibleSelection(
      String initLabel, String initDescription, int initValue, IntWrapper initWrapper) {
    label = initLabel;
    description = initDescription;
    value = initValue;
    wrapper = initWrapper;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public IntWrapper getWrapper() {
    return wrapper;
  }

  public void setWrapper(IntWrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    wrapper.setChoice(value);
  }
}
