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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class OverlapPanel
	extends ItemListManagePanel
{
	private final boolean isOverlap;
	private final LockableListModel overlapModel;

	public OverlapPanel( final String confirmText, final String cancelText, final LockableListModel overlapModel,
		final boolean isOverlap )
	{
		super( confirmText, cancelText, isOverlap ? overlapModel : KoLConstants.inventory, true, false );
		this.overlapModel = overlapModel;
		this.isOverlap = isOverlap;

		if ( this.isOverlap )
		{
			this.getElementList().setCellRenderer( ListCellRendererFactory.getNameOnlyRenderer() );
		}

		this.getElementList().addKeyListener( new OverlapAdapter() );
		this.addFilters();
	}

	@Override
	public AutoFilterTextField getWordFilter()
	{
		return new OverlapFilterField();
	}

	private class OverlapFilterField
		extends FilterItemField
	{
		@Override
		public boolean isVisible( final Object element )
		{
			return super.isVisible( element ) && ( OverlapPanel.this.isOverlap ? KoLConstants.inventory.contains( element ) : !OverlapPanel.this.overlapModel.contains( element ) );
		}
	}

	private class OverlapAdapter
		extends KeyAdapter
	{
		@Override
		public void keyReleased( final KeyEvent e )
		{
			if ( e.isConsumed() )
			{
				return;
			}

			if ( e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE )
			{
				return;
			}

			Object[] items = OverlapPanel.this.getSelectedValues();
			OverlapPanel.this.getElementList().clearSelection();

			for ( int i = 0; i < items.length; ++i )
			{
				OverlapPanel.this.overlapModel.remove( items[ i ] );
				if ( OverlapPanel.this.overlapModel == KoLConstants.singletonList )
				{
					KoLConstants.junkList.remove( items[ i ] );
				}
			}

			OverlapPanel.this.filterItems();
			e.consume();
		}
	}
}
