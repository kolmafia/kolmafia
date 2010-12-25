/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JViewport;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class FaxRequestFrame
	extends GenericFrame
{
	private MonsterCategoryComboBox categorySelect;
	private GenericScrollPane monsterSelect;
	private static ShowDescriptionList [] monsters;
	private static final int ROWS = 15;

	static
	{
		FaxBotDatabase.configure();
		FaxRequestFrame.monsters = new ShowDescriptionList[ FaxBotDatabase.monstersByCategory.length ];
		for ( int i = 0; i < FaxRequestFrame.monsters.length; ++i )
		{
			FaxRequestFrame.monsters[ i ] = new ShowDescriptionList( FaxBotDatabase.monstersByCategory[ i ], ROWS );
		}
	}

	public FaxRequestFrame()
	{
		super( "Request a Fax" );
		this.framePanel.add( new FaxRequestPanel(), BorderLayout.NORTH );
	}

	private class FaxRequestPanel
		extends GenericPanel
	{
		public FaxRequestPanel()
		{
			super( "request", "online?" );

			FaxRequestFrame.this.categorySelect = new MonsterCategoryComboBox();
			FaxRequestFrame.this.monsterSelect = new GenericScrollPane( FaxRequestFrame.monsters[0], ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Category: ", FaxRequestFrame.this.categorySelect );
			elements[ 1 ] = new VerifiableElement( "Monster: ", FaxRequestFrame.this.monsterSelect );

			this.setContent( elements );
		}

		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			if ( FaxRequestFrame.this.categorySelect != null )
			{
				FaxRequestFrame.this.categorySelect.setEnabled( isEnabled );
			}
			if ( FaxRequestFrame.this.monsterSelect != null )
			{
				FaxRequestFrame.this.monsterSelect.setEnabled( isEnabled );
			}
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			FaxRequestFrame.this.isBotOnline();
		}
	}

	private void isBotOnline()
	{
		String botName = FaxBotDatabase.botName( 0 );
		if ( botName == null )
		{
			InputFieldUtilities.alert( "No faxbots configured." );
		}
		else if ( KoLmafia.isPlayerOnline( botName ) )
		{
			InputFieldUtilities.alert( botName + " is online." );
		}
		else
		{
			InputFieldUtilities.alert( botName + " is probably not online." );
		}
	}

	private class MonsterCategoryComboBox
		extends JComboBox
	{
		public MonsterCategoryComboBox()
		{
			super();
			int count = FaxBotDatabase.categories.size();
			for ( int i = 0; i < count; ++i )
			{
				addItem( FaxBotDatabase.categories.get( i ) );
			}
			addActionListener( new MonsterCategoryListener() );
		}

		private class MonsterCategoryListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				int index = MonsterCategoryComboBox.this.getSelectedIndex();
				FaxRequestFrame.this.monsterSelect.getViewport().setView( FaxRequestFrame.monsters[ index ] );
			}
		}
	}
}
