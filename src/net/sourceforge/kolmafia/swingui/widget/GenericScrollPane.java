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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.preferences.Preferences;

public class GenericScrollPane
	extends JScrollPane
{
	public GenericScrollPane( final LockableListModel model )
	{
		this( model, 8 );
	}

	public GenericScrollPane( final LockableListModel model, final int visibleRows )
	{
		this(
			new ShowDescriptionList( model, visibleRows ), ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
	}

	public GenericScrollPane( final Component view )
	{
		this( view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
	}

	public GenericScrollPane( final Component view, final int hsbPolicy )
	{
		this( view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, hsbPolicy );
	}

	public GenericScrollPane( final Component view, final int vsbPolicy, final int hsbPolicy )
	{
		super( view, vsbPolicy, hsbPolicy );
		this.setOpaque( true );

		if ( view instanceof JList )
		{
			if ( Preferences.getString( "swingLookAndFeel" ).equals(
				UIManager.getCrossPlatformLookAndFeelClassName() ) )
			{
				( (JList) view ).setFont( KoLConstants.DEFAULT_FONT );
			}
		}
		else if ( !( view instanceof JTextComponent ) )
		{
			this.getVerticalScrollBar().setUnitIncrement( 30 );
		}
	}
}
