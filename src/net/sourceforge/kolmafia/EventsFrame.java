/**
 * Copyright (c) 2006, KoLmafia development team
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

// layout
import javax.swing.Box;
import java.awt.CardLayout;
import java.awt.BorderLayout;

// containers
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import java.lang.ref.WeakReference;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A class which displays the list of events that arrived this session
 */

public class EventsFrame extends KoLFrame
{
	public EventsFrame()
	{
		super( "Recent Events" );
		getContentPane().add( new EventsPanel(), BorderLayout.CENTER );
	}

	private class EventsPanel extends ActionPanel
	{
		protected JPanel actualPanel;
		protected VerifyButtonPanel buttonPanel;
		protected JComponent scrollComponent;

		public EventsPanel()
		{
			scrollComponent = new JList( KoLCharacter.getEvents() );

			JPanel centerPanel = new JPanel( new BorderLayout() );
			centerPanel.add( new JScrollPane( scrollComponent,
							  JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
							  JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ),
					 BorderLayout.CENTER );

			actualPanel = new JPanel( new BorderLayout( 20, 10 ) );
			actualPanel.add( centerPanel, BorderLayout.CENTER );

			buttonPanel = new VerifyButtonPanel( "clear", "check", "check" );
			buttonPanel.setBothDisabledOnClick( true );
			actualPanel.add( buttonPanel, BorderLayout.EAST );

			getContentPane().setLayout( new CardLayout( 10, 10 ) );
			getContentPane().add( actualPanel, "" );

			existingPanels.add( new WeakReference( this ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			scrollComponent.setEnabled( isEnabled );
			buttonPanel.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			// Clear list of events
			KoLCharacter.clearEvents();
		}

		protected void actionCancelled()
		{
			// Connect to main map to pick up new events
			(new KoLRequest( StaticEntity.getClient(), "main.php" ) ).run();
		}
	}
}
