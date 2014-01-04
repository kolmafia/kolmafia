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

import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionTable;

public class ItemTableManagePanel
	extends ItemManagePanel
{
	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel,
				     final boolean addFilterField, final boolean addRefreshButton, final boolean[] flags )
	{
		super( confirmedText, cancelledText, elementModel, new ShowDescriptionTable( elementModel, flags ), addFilterField, addRefreshButton );

		ShowDescriptionTable elementList = (ShowDescriptionTable) this.scrollComponent;
		elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		elementList.setVisibleRowCount( 8 );
		if ( addFilterField )
		{
			this.filterfield.setModel( elementList.getDisplayModel() );
		}
	}

	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel )
	{
		this( confirmedText, cancelledText, elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      new boolean[] { false, false });
	}

	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel,
				     final boolean addFilterField, final boolean addRefreshButton )
	{
		this( confirmedText, cancelledText, elementModel,
		      addFilterField, addRefreshButton,
		      new boolean[] { false, false });
	}
	
	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel, final boolean[] flags )
	{
		this( confirmedText, cancelledText, elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      flags);
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean addFilterField,
				     final boolean addRefreshButton, final boolean[] flags )
	{
		this( null, null, elementModel,
		      addFilterField, addRefreshButton,
		      flags );
	}

	public ItemTableManagePanel( final LockableListModel elementModel )
	{
		this( elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      new boolean[] {false, false} );
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean[] flags )
	{
		this( elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      flags);
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean addFilterField,
				     final boolean addRefreshButton )
	{
		this( elementModel,
		      addFilterField, addRefreshButton,
		      new boolean[] {false, false} );
	}

	public ShowDescriptionTable getElementList()
	{
		return (ShowDescriptionTable)this.scrollComponent;
	}

	@Override
	public Object[] getSelectedValues()
	{
		return this.getElementList().getSelectedValues();
	}
}
