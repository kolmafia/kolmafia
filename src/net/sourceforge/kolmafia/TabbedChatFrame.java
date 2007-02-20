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

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tab.CloseTabbedPane;
import tab.CloseListener;

import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;

public class TabbedChatFrame extends ChatFrame implements ChangeListener, CloseListener, CloseableTabbedPaneListener
{
	private static boolean instanceExists = false;

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
			((CloseTabbedPane)tabs).setCloseIcon( true );
			((CloseTabbedPane)tabs).addCloseListener( this );
		}
		else
		{
			tabs = new CloseableTabbedPane();
			((CloseableTabbedPane)tabs).addCloseableTabbedPaneListener( this );
		}

		tabs.addChangeListener( this );
		framePanel.add( tabs, BorderLayout.CENTER );
	}

	public void stateChanged( ChangeEvent e )
	{
		int selectedIndex = tabs.getSelectedIndex();

		if ( selectedIndex != -1 )
		{
			tabs.setBackgroundAt( selectedIndex, null );
			tabs.setForegroundAt( selectedIndex, null );

			KoLMessenger.setUpdateChannel( tabs.getTitleAt( selectedIndex ).trim() );
		}
	}

	public boolean closeTab( int tabIndexToClose )
	{
		KoLMessenger.removeChat( tabs.getTitleAt( tabIndexToClose ).trim() );
		return true;
	}

	public void closeOperation( MouseEvent e, int overTabIndex )
	{
		if ( overTabIndex == -1 || !instanceExists )
			return;

		String toRemove = tabs.getTitleAt( overTabIndex );

		tabs.removeTabAt( overTabIndex );
		KoLMessenger.removeChat( toRemove );
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

			tabs.addTab( tabName, createdPanel );
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
			{
				((CloseTabbedPane)tabs).highlightTab( tabIndex );
			}
			else
			{
				tabs.setBackgroundAt( tabIndex, new Color( 0, 0, 128 ) );
				tabs.setForegroundAt( tabIndex, Color.white );
			}
		}
	}
}
