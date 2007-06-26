/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.event.MouseEvent;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tab.CloseTabbedPane;
import tab.CloseTabPaneUI;
import tab.CloseListener;

import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;

public class TabbedChatFrame extends ChatFrame implements ChangeListener, CloseListener, CloseableTabbedPaneListener
{
	private ChatPanel commandLineDisplay;
	private static boolean addGCLI = false;
	private static boolean instanceExists = false;

	public TabbedChatFrame()
	{
		commandLineDisplay = new ChatPanel( GCLI_TAB );
		addGCLI = StaticEntity.getBooleanProperty( "addChatCommandLine" );
	}

	public JTabbedPane getTabbedPane()
	{
		return StaticEntity.getBooleanProperty( "useShinyTabbedChat" ) ?
			(JTabbedPane) new CloseTabbedPane() : (JTabbedPane) new CloseableTabbedPane();
	}

	/**
	 * Utility method called to initialize the frame.  This
	 * method should be overridden, should a different means
	 * of initializing the content of the frame be needed.
	 */

	public void initialize( String associatedContact )
	{
		instanceExists = true;

		if ( tabs instanceof CloseTabbedPane )
		{
			((CloseTabbedPane)tabs).setCloseIconStyle( CloseTabPaneUI.GRAY_CLOSE_ICON );
			((CloseTabbedPane)tabs).addCloseListener( this );
		}
		else
		{
			((CloseableTabbedPane)tabs).addCloseableTabbedPaneListener( this );
		}

		tabs.addChangeListener( this );
		framePanel.add( tabs, BorderLayout.CENTER );
	}

	public void stateChanged( ChangeEvent e )
	{
		int selectedIndex = tabs.getSelectedIndex();

		if ( selectedIndex != -1 && selectedIndex != tabs.getTabCount() - 1 )
			KoLMessenger.setUpdateChannel( tabs.getTitleAt( selectedIndex ).trim() );
	}

	public boolean closeTab( int tabIndexToClose )
	{
		if ( tabIndexToClose == -1 || !instanceExists )
			return false;

		String toRemove = tabs.getTitleAt( tabIndexToClose );

		if ( toRemove.equals( GCLI_TAB ) )
			return false;

		KoLMessenger.removeChat( toRemove );
		return true;
	}

	public void closeOperation( MouseEvent e, int overTabIndex )
	{
		if ( closeTab( overTabIndex ) )
			tabs.removeTabAt( overTabIndex );
	}

	/**
	 * Adds a new tab to represent the given name.  Note that
	 * this will not shift tab focus; however, if it is the
	 * first tab added, the name of the contact will be reset.
	 */

	public void addTab( String tabName )
	{
		for ( int i = 0; i < tabs.getTabCount(); ++i )
			if ( tabs.getTitleAt(i).trim().equals( tabName ) )
				return;

		SwingUtilities.invokeLater( new TabAdder( tabName ) );
	}

	public void highlightTab( String tabName )
	{
		if ( tabName == null )
			return;

		for ( int i = 0; i < tabs.getTabCount(); ++i )
		{
			if ( tabName.equals( tabs.getTitleAt(i).trim() ) )
			{
				SwingUtilities.invokeLater( new TabHighlighter( i ) );
				return;
			}
		}
	}

	public void dispose()
	{
		instanceExists = false;
		super.dispose();
	}

	private class TabAdder implements Runnable
	{
		private String tabName;
		private ChatPanel createdPanel;

		private TabAdder( String tabName )
		{	this.tabName = tabName;
		}

		public void run()
		{
			createdPanel = new ChatPanel( tabName );

			// Add a little bit of whitespace to make the
			// chat tab larger and easier to click.

			if ( addGCLI && tabs.getTabCount() > 0 && tabs.getTitleAt( tabs.getTabCount() - 1 ).equals( GCLI_TAB ) )
				tabs.removeTabAt( tabs.getTabCount() - 1 );

			tabs.addTab( tabName, createdPanel );

			if ( addGCLI )
				tabs.addTab( GCLI_TAB, commandLineDisplay );

			createdPanel.requestFocus();
		}
	}

	private class TabHighlighter implements Runnable
	{
		private int tabIndex;

		public TabHighlighter( int tabIndex )
		{	this.tabIndex = tabIndex;
		}

		public void run()
		{
			if ( tabs.getSelectedIndex() == tabIndex )
				return;

			if ( tabs instanceof CloseTabbedPane )
				((CloseTabbedPane)tabs).highlightTab( tabIndex );
			else
				((CloseableTabbedPane)tabs).highlightTab( tabIndex );
		}
	}
}
