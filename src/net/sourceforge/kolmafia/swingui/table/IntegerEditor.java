package net.sourceforge.kolmafia.swingui.table;

import javax.swing.DefaultCellEditor;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IntegerEditor
	extends DefaultCellEditor
{
	public IntegerEditor()
	{
		super( new AutoHighlightTextField() );
	}

	@Override
	public Object getCellEditorValue()
	{
		AutoHighlightTextField field = (AutoHighlightTextField) getComponent();
		int value = StringUtilities.parseInt( field.getText() );

		return IntegerPool.get( value );
	}

	@Override
	public boolean stopCellEditing()
	{
		AutoHighlightTextField field = (AutoHighlightTextField) getComponent();
		int value = StringUtilities.parseInt( field.getText() );
		field.setText( KoLConstants.COMMA_FORMAT.format( value ) );

		return super.stopCellEditing();
	}
}
