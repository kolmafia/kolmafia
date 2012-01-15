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

	public Component prepareRenderer( final TableCellRenderer renderer, final int row, final int column )
	{
		Component c = super.prepareRenderer( renderer, row, column );
		if ( c instanceof JComponent )
		{
			( (JComponent) c ).setOpaque( false );
		}

		return c;
	}

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
