/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An internal class which creates a panel which manages items.
 * This is done because most of the item management displays
 * are replicated.  Note that a lot of this code was borrowed
 * directly from the ActionVerifyPanel class in the utilities
 * package for Spellcast.
 */

public abstract class ItemManagePanel extends LabeledScrollPanel
{
	protected ShowDescriptionList elementList;

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
	{	this( title, confirmedText, cancelledText, elements, true );
	}

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements, boolean isRootPane )
	{
		super( title, confirmedText, cancelledText, new ShowDescriptionList( elements ), isRootPane );

		elementList = (ShowDescriptionList) scrollComponent;
		elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		elementList.setVisibleRowCount( 8 );
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		elementList.setEnabled( isEnabled );
	}
}
