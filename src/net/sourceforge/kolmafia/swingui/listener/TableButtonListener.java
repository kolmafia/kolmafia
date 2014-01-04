/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import javax.swing.table.TableColumnModel;

/**
 * Utility class used to forward events to JButtons enclosed inside of a JTable object.
 */

public class TableButtonListener
	extends ThreadedListener
{
	private final JTable table;

	public TableButtonListener( final JTable table )
	{
		this.table = table;
	}

	@Override
	protected void execute()
	{
		TableColumnModel columnModel = this.table.getColumnModel();

		int row = getMousePositionY() / this.table.getRowHeight();
		int column = columnModel.getColumnIndexAtX( getMousePositionX() );

		if ( row >= 0 && row < this.table.getRowCount() && column >= 0 && column < this.table.getColumnCount() )
		{
			Object value = this.table.getValueAt( row, column );

			if ( value instanceof JButton )
			{
				MouseEvent event = SwingUtilities.convertMouseEvent( this.table, getMouseEvent(), (JButton) value );
				( (JButton) value ).dispatchEvent( event );
				this.table.repaint();
			}
		}
	}
}
