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

import java.awt.CardLayout;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;

/**
 * An extension of <code>ChatFrame</code> used to display the current
 * chat contents.  This form of the chat frame is tabbed, so those
 * users who want tabbed chat (rather than multiple windows) can
 * have that flexibility.
 */

public class TabbedChatFrame extends ChatFrame implements CloseableTabbedPaneListener
{
	private CloseableTabbedPane tabs;

	public TabbedChatFrame( KoLmafia client, KoLMessenger messenger )
	{	super( client, messenger );
	}

	/**
	 * Utility method called to initialize the frame.  This
	 * method should be overridden, should a different means
	 * of initializing the content of the frame be needed.
	 */

	protected void initialize( String associatedContact )
	{
		getContentPane().setLayout( new CardLayout( 5, 5 ) );
		tabs = new CloseableTabbedPane();
		tabs.addCloseableTabbedPaneListener( this );
		getContentPane().add( tabs, "" );
	}

	/**
	 * Adds a new tab to represent the given name.  Note that
	 * this will not shift tab focus; however, if it is the
	 * first tab added, the name of the contact will be reset.
	 */

	public JEditorPane addTab( String tabName )
	{
		ChatPanel createdPanel = new ChatPanel( tabName );
		(new AddTabRunnable( tabName, createdPanel )).run();
		return createdPanel.getChatDisplay();
	}

	public boolean closeTab( int tabIndexToClose )
	{
		client.getMessenger().removeChat( tabs.getTitleAt( tabIndexToClose ) );
		return true;
	}

	private class AddTabRunnable implements Runnable
	{
		private String name;
		private ChatPanel panel;

		public AddTabRunnable( String name, ChatPanel panel )
		{
			this.name = name;
			this.panel = panel;
		}

		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			tabs.addTab( name, panel );
		}
	}

	public boolean hasFocus()
	{	return false;
	}

	public void requestFocus()
	{
	}
}
