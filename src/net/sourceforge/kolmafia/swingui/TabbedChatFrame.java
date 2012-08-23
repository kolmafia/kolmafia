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

package net.sourceforge.kolmafia.swingui;

import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;

import java.awt.event.MouseEvent;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.listener.TabFocusingListener;

import net.sourceforge.kolmafia.swingui.panel.CommandDisplayPanel;

import tab.CloseListener;
import tab.CloseTabPaneUI;
import tab.CloseTabbedPane;

public class TabbedChatFrame
	extends ChatFrame
	implements CloseListener, CloseableTabbedPaneListener
{
	public TabbedChatFrame()
	{
		super( null );

		this.setTitle( "Loathing Chat" );

		if ( Preferences.getBoolean( "addChatCommandLine" ) )
		{
			this.tabs.addTab( "[gcli]", new CommandDisplayPanel() );
		}

		this.tabs.addChangeListener( new TabFocusingListener() );
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return Preferences.getBoolean( "useShinyTabbedChat" ) ? (JTabbedPane) new CloseTabbedPane() : (JTabbedPane) new CloseableTabbedPane();
	}

	/**
	 * Utility method called to initialize the frame. This method should be overridden, should a different means of
	 * initializing the content of the frame be needed.
	 */

	@Override
	public void initialize( final String associatedContact )
	{
		if ( this.tabs instanceof CloseTabbedPane )
		{
			( (CloseTabbedPane) this.tabs ).setCloseIconStyle( CloseTabPaneUI.GRAY_CLOSE_ICON );
			( (CloseTabbedPane) this.tabs ).addCloseListener( this );
		}
		else
		{
			( (CloseableTabbedPane) this.tabs ).addCloseableTabbedPaneListener( this );
		}

		this.setCenterComponent( this.tabs );
	}

	public boolean closeTab( final int tabIndexToClose )
	{
		if ( tabIndexToClose == -1 )
		{
			return false;
		}

		String closedTab = this.tabs.getTitleAt( tabIndexToClose );

		RequestThread.runInParallel( new CloseWindowRunnable( closedTab ) );

		return true;
	}

	public void closeOperation( final MouseEvent e, final int overTabIndex )
	{
		if ( this.closeTab( overTabIndex ) )
		{
			this.tabs.removeTabAt( overTabIndex );
		}
	}

	/**
	 * Adds a new tab to represent the given name. Note that this will not shift tab focus; however, if it is the first
	 * tab added, the name of the contact will be reset.
	 */

	public void addTab( final String tabName )
	{
		for ( int i = 0; i < this.tabs.getTabCount(); ++i )
		{
			if ( this.tabs.getTitleAt( i ).trim().equals( tabName ) )
			{
				return;
			}
		}

		try
		{
			TabAdder add = new TabAdder( tabName );

			if ( SwingUtilities.isEventDispatchThread() )
			{
				add.run();
			}
			else
			{
				SwingUtilities.invokeAndWait( add );
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  However, skip it
			// since nothing bad really happened.
		}
	}

	public void removeTab( final String tabName )
	{
		for ( int i = 0; i < this.tabs.getTabCount(); ++i )
		{
			if ( this.tabs.getTitleAt( i ).trim().equals( tabName ) )
			{
				this.closeOperation( null, i );
				return;
			}
		}
	}

	public void highlightTab( final String tabName )
	{
		if ( tabName == null )
		{
			return;
		}

		for ( int i = 0; i < this.tabs.getTabCount(); ++i )
		{
			if ( tabName.equals( this.tabs.getTitleAt( i ).trim() ) )
			{
				SwingUtilities.invokeLater( new TabHighlighter( i ) );
				return;
			}
		}
	}

	private class TabAdder
		implements Runnable
	{
		private final String tabName;

		private TabAdder( final String tabName )
		{
			this.tabName = tabName;
		}

		public void run()
		{
			JTabbedPane tabs = TabbedChatFrame.this.tabs;
			ChatPanel createdPanel = new ChatPanel( this.tabName );

			int tabOrder = this.getTabOrder( this.tabName );

			int tabCount = tabs.getTabCount();
			int tabIndex = tabCount;

			for ( int i = 0; i < tabCount; ++i )
			{
				String currentTabName = tabs.getTitleAt( i ).trim();

				int currentTabOrder = this.getTabOrder( currentTabName );

				if ( tabOrder < currentTabOrder )
				{
					tabIndex = i;
					break;
				}
			}

			tabs.insertTab( this.tabName, null, createdPanel, "", tabIndex );
		}

		private int getTabOrder( final String tabName )
		{
			if ( tabName.startsWith( "[" ) )
			{
				return 2;
			}

			if ( tabName.startsWith( "/" ) )
			{
				return 0;
			}

			return 1;
		}
	}

	private class TabHighlighter
		implements Runnable
	{
		private final int tabIndex;

		public TabHighlighter( final int tabIndex )
		{
			this.tabIndex = tabIndex;
		}

		public void run()
		{
			if ( TabbedChatFrame.this.tabs.getSelectedIndex() == this.tabIndex )
			{
				return;
			}

			if ( TabbedChatFrame.this.tabs instanceof CloseTabbedPane )
			{
				( (CloseTabbedPane) TabbedChatFrame.this.tabs ).highlightTab( this.tabIndex );
			}
			else
			{
				( (CloseableTabbedPane) TabbedChatFrame.this.tabs ).highlightTab( this.tabIndex );
			}
		}
	}

	private static class CloseWindowRunnable
		implements Runnable
	{
		private String closedTab;

		public CloseWindowRunnable( String closedTab )
		{
			this.closedTab = closedTab;
		}

		public void run()
		{
			ChatManager.closeWindow( this.closedTab );
		}
	}
}
