package net.sourceforge.kolmafia.swingui.table;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TransparentTable
	extends JTable
{
	public TransparentTable( final TableModel t )
	{
		super( t );

		this.setDefaultEditor( Integer.class, new IntegerEditor() );
		this.setDefaultRenderer( Integer.class, new IntegerRenderer() );
		this.setDefaultRenderer( JButton.class, new ButtonRenderer() );
	}

	@Override
	public Component prepareRenderer( final TableCellRenderer renderer, final int row, final int column )
	{
		Component c = super.prepareRenderer( renderer, row, column );
		if ( c instanceof JComponent )
		{
			( (JComponent) c ).setOpaque( false );
		}

		return c;
	}

	@Override
	public void changeSelection( final int row, final int column, final boolean toggle, final boolean extend )
	{
		super.changeSelection( row, column, toggle, extend );

		if ( !this.editCellAt( row, column ) )
		{
			return;
		}

		Component editor = this.getEditorComponent();
		if ( !( editor instanceof JTextField ) )
		{
			return;
		}

		JTextField field = (JTextField) this.getEditorComponent();

		if ( getColumnClass( column ) == Integer.class )
		{
			field.setText( KoLConstants.COMMA_FORMAT.format(
				StringUtilities.parseInt( field.getText() ) ) );
		}
	}
}
