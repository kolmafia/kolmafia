package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SwingInputFieldUtilitiesTest {
  @ParameterizedTest
  @CsvSource({"'1,234', 1234", "' 17 ', 17", "'', 5", "'*', 5", "'0', 5"})
  void readsNumbersFromTextFields(final String text, final int expected) {
    assertThat(SwingInputFieldUtilities.getValue(new JTextField(text), 5), is(expected));
  }

  @Test
  void readsZeroFromAnEmptyTextFieldByDefault() {
    assertThat(SwingInputFieldUtilities.getValue(new JTextField()), is(0));
  }

  @Test
  void readsIntegersFromSpinners() {
    assertThat(SwingInputFieldUtilities.getValue(new JSpinner(), 5), is(0));
  }

  @Test
  void usesTheDefaultForSpinnersWithoutIntegers() {
    var spinner = new JSpinner(new SpinnerListModel(new String[] {"a", "b"}));

    assertThat(SwingInputFieldUtilities.getValue(spinner, 5), is(5));
  }
}
