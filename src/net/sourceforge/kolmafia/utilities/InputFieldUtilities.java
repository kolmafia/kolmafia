/**
 * Copyright (c) 2005-2012, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.utilities;

import java.awt.BorderLayout;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

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

	public static final void alert(final String message)
	{

		JOptionPane.showMessageDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message));
	}

	public static final boolean confirm(final String message)
	{

		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message), "",
			JOptionPane.YES_NO_OPTION);
	}

	public static final String input(final String message)
	{

		return JOptionPane.showInputDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message));
	}

	public static final String input(final String message, final String initial)
	{

		return JOptionPane.showInputDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message),
			initial);
	}

	public static final Object input( final String message, final LockableListModel inputs, final Object initial )
	{
		JList selector = new JList(inputs);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add( new AutoFilterTextField( selector, initial ), BorderLayout.NORTH );
		panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

		int option =
			JOptionPane.showConfirmDialog(
				InputFieldUtilities.activeWindow, panel,
				StringUtilities.basicTextWrap(message),
				JOptionPane.OK_CANCEL_OPTION);
		return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION 
			? null : selector.getSelectedValue();
	}

	public static final Object input( final String message, final LockableListModel inputs )
	{
		return InputFieldUtilities.input( message, inputs, null );
	}

	public static final Object input(final String message, final Object[] inputs)
	{

		if (inputs == null || inputs.length == 0) {
			return null;
		}

		return InputFieldUtilities.input(message, inputs, inputs[0]);
	}

	public static final Object input( final String message, final Object[] inputs, final Object initial)
	{

		if (inputs == null || inputs.length == 0) {
			return null;
		}

		return JOptionPane.showInputDialog(
			InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message), "",
			JOptionPane.INFORMATION_MESSAGE, null, inputs, initial);
	}

	public static final Object[] multiple( final String message, final LockableListModel inputs )
	{
		return InputFieldUtilities.multiple( message, inputs, null );
	}


	public static final Object[] multiple( final String message, final LockableListModel inputs, final ListElementFilter filter )
	{

		JList selector = new JList(inputs);
		selector.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add( filter == null ? new AutoFilterTextField( selector ) :
			new AutoFilterTextField( selector ) {
				public boolean isVisible( Object o )
				{
					return filter.isVisible( o ) && super.isVisible( o );
				}			
			}, BorderLayout.NORTH);
		inputs.updateFilter( false );
		panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

		int option =
			JOptionPane.showConfirmDialog(
				InputFieldUtilities.activeWindow, panel,
				StringUtilities.basicTextWrap(message),
				JOptionPane.OK_CANCEL_OPTION);
		return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
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

		int result = StringUtilities.parseIntInternal2(currentValue);
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

		int desiredValue = StringUtilities.parseIntInternal2(currentValue);
		return desiredValue < 0 ? maximumValue - desiredValue : Math.min(
			desiredValue, maximumValue);
	}

	public static boolean finalizeTable( final JTable table )
	{
		if ( table.isEditing() )
		{
			int row = table.getEditingRow();
			int col = table.getEditingColumn();
			table.getCellEditor( row, col ).stopCellEditing();
	
			if ( table.isEditing() )
			{
				alert( "One or more fields contain invalid values. (Note: they are currently outlined in red)" );
				return false;
			}
		}
	
		return true;
	}

}
