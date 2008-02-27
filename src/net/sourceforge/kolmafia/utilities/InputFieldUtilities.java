package net.sourceforge.kolmafia.utilities;

import java.awt.BorderLayout;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;


public class InputFieldUtilities {

	private static GenericFrame activeWindow = null;

	public static void setActiveWindow( GenericFrame activeWindow )
	{
		InputFieldUtilities.activeWindow = activeWindow;
	}

	public static final void alert(final String message) {

		JOptionPane.showMessageDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message));
	}

	public static final boolean confirm(final String message) {

		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message), "",
			JOptionPane.YES_NO_OPTION);
	}

	public static final String input(final String message) {

		return JOptionPane.showInputDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message));
	}

	public static final String input(final String message, final String initial) {

		return JOptionPane.showInputDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message),
			initial);
	}

	public static final Object input(
		final String message, final LockableListModel inputs) {

		JList selector = new JList(inputs);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new AutoFilterTextField(selector), BorderLayout.NORTH);
		panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

		int option =
			JOptionPane.showConfirmDialog(
				InputFieldUtilities.activeWindow, panel,
				StringUtilities.basicTextWrap(message),
				JOptionPane.OK_CANCEL_OPTION);
		return option == JOptionPane.CANCEL_OPTION
			? null : selector.getSelectedValue();
	}

	public static final Object input(final String message, final Object[] inputs) {

		if (inputs == null || inputs.length == 0) {
			return null;
		}

		return InputFieldUtilities.input(message, inputs, inputs[0]);
	}

	public static final Object input(
		final String message, final Object[] inputs, final Object initial) {

		if (inputs == null || inputs.length == 0) {
			return null;
		}

		return JOptionPane.showInputDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message), "",
			JOptionPane.INFORMATION_MESSAGE, null, inputs, initial);
	}

	public static final Object[] multiple(
		final String message, final LockableListModel inputs) {

		JList selector = new JList(inputs);
		selector.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new AutoFilterTextField(selector), BorderLayout.NORTH);
		panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

		int option =
			JOptionPane.showConfirmDialog(
				InputFieldUtilities.activeWindow, panel,
				StringUtilities.basicTextWrap(message),
				JOptionPane.OK_CANCEL_OPTION);
		return option == JOptionPane.CANCEL_OPTION
			? new Object[0] : selector.getSelectedValues();
	}

	/**
	 * Utility method which retrieves an integer value from the given field. In
	 * the event that the field does not contain an integer value, the number
	 * "0" is returned instead.
	 */

	public static final int getValue(final JTextField field) {

		return InputFieldUtilities.getValue(field, 0);
	}

	public static final int getValue(final JSpinner field) {

		return InputFieldUtilities.getValue(field, 0);
	}

	/**
	 * Utility method which retrieves an integer value from the given field. In
	 * the event that the field does not contain an integer value, the default
	 * value provided will be returned instead.
	 */

	public static final int getValue(
		final JTextField field, final int defaultValue) {

		String currentValue = field.getText();

		if (currentValue == null || currentValue.length() == 0) {
			return defaultValue;
		}

		if (currentValue.equals("*")) {
			return defaultValue;
		}

		int result = StringUtilities.parseInt(currentValue);
		return result == 0 ? defaultValue : result;
	}

	public static final int getValue(
		final JSpinner field, final int defaultValue) {

		if (!(field.getValue() instanceof Integer)) {
			return defaultValue;
		}

		return ((Integer) field.getValue()).intValue();
	}

	public static final int getQuantity(
		final String title, final int maximumValue) {

		return InputFieldUtilities.getQuantity(title, maximumValue, maximumValue);
	}

	public static final int getQuantity(
		final String message, final int maximumValue, int defaultValue) {

		// Check parameters; avoid programmer error.
		if (defaultValue > maximumValue) {
			defaultValue = maximumValue;
		}

		if (maximumValue == 1 && maximumValue == defaultValue) {
			return 1;
		}

		String currentValue =
			InputFieldUtilities.input(
				message, KoLConstants.COMMA_FORMAT.format(defaultValue));
		if (currentValue == null) {
			return 0;
		}

		if (currentValue.equals("*")) {
			return maximumValue;
		}

		int desiredValue = StringUtilities.parseInt(currentValue);
		return desiredValue < 0 ? maximumValue - desiredValue : Math.min(
			desiredValue, maximumValue);
	}

}
