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

import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import tab.CloseListener;
import tab.CloseTabPaneUI;
import tab.CloseTabbedPane;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.java.forums.CloseableTabbedPane;

public class KoLDesktop extends KoLFrame implements ChangeListener, CloseListener
{
	private static final DisplayDesktopRunnable DISPLAYER = new DisplayDesktopRunnable();

	private static KoLDesktop INSTANCE = null;
	private static boolean isInitializing = false;

	private ArrayList tabListing = new ArrayList();

	public JPanel compactPane;
	public JLabel levelLabel, roninLabel, mcdLabel;
	public JLabel musLabel, mysLabel, moxLabel, drunkLabel;
	public JLabel hpLabel, mpLabel, meatLabel, advLabel;
	public JLabel familiarLabel;

	public KoLCharacterAdapter refreshListener;

	public KoLDesktop()
	{
	}

	public KoLDesktop( String title )
	{
		super( "Main Interface" );

		if ( StaticEntity.usesSystemTray() )
			this.addWindowListener( new MinimizeListener() );

		this.getContentPane().setLayout( new BorderLayout() );

		this.tabs.setTabPlacement( CloseTabbedPane.TOP );
		this.getContentPane().add( this.tabs, BorderLayout.CENTER );
		this.addCompactPane();

		JToolBar toolbarPanel = null;

		switch ( StaticEntity.getIntegerProperty( "toolbarPosition" ) )
		{
		case 1:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			this.getContentPane().add( toolbarPanel, BorderLayout.NORTH );
			break;

		case 2:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			this.getContentPane().add( toolbarPanel, BorderLayout.SOUTH );
			break;

		case 3:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
			this.getContentPane().add( toolbarPanel, BorderLayout.WEST );
			break;
		}

		this.setJMenuBar( new KoLMenuBar() );
		if ( toolbarPanel != null )
			addMainToolbar( toolbarPanel );

		this.tabs.addChangeListener( this );
		int scriptButtons = StaticEntity.getIntegerProperty( "scriptButtonPosition" );

		if ( scriptButtons != 0 )
		{
			String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );

			JToolBar scriptBar = null;

			if ( scriptButtons == 1 )
			{
				scriptBar = toolbarPanel;
				scriptBar.add( Box.createHorizontalStrut( 10 ) );
			}
			else
			{
				scriptBar =  new JToolBar( JToolBar.VERTICAL );
				scriptBar.setFloatable( false );
			}

			for ( int i = 0; i < scriptList.length; ++i )
				scriptBar.add( new LoadScriptButton( i + 1, scriptList[i] ) );

			if ( scriptButtons == 2 )
			{
				JPanel scriptBarHolder = new JPanel();
				scriptBarHolder.add( scriptBar );

				this.getContentPane().add( scriptBarHolder, BorderLayout.EAST );
			}
		}
	}

	public JTabbedPane getTabbedPane()
	{
		if ( StaticEntity.getBooleanProperty( "useDecoratedTabs" ) )
		{
			JTabbedPane tabs = new CloseTabbedPane();

			if ( StaticEntity.getBooleanProperty( "allowCloseableDesktopTabs" ) )
			{
				((CloseTabbedPane)tabs).setCloseIconStyle( CloseTabPaneUI.RED_CLOSE_ICON );
				((CloseTabbedPane)tabs).addCloseListener( this );
			}

			return tabs;
		}

		return StaticEntity.getBooleanProperty( "allowCloseableDesktopTabs" ) ?
			new CloseableTabbedPane() : new JTabbedPane();
	}

	public void stateChanged( ChangeEvent e )
	{
		int selectedIndex = this.tabs.getSelectedIndex();
		if ( selectedIndex != -1 && selectedIndex < this.tabListing.size() )
			((KoLFrame) this.tabListing.get( selectedIndex )).requestFocus();
	}

	public void closeOperation( MouseEvent e, int overTabIndex )
	{
		if ( overTabIndex == -1 )
			return;

		this.tabListing.remove( overTabIndex );
		this.tabs.removeTabAt( overTabIndex );
	}

	public void initializeTabs()
	{
		if ( isInitializing )
			return;

		isInitializing = true;
		KoLmafiaGUI.checkFrameSettings();

		String interfaceSetting = StaticEntity.getGlobalProperty( "initialDesktop" );
		if ( !interfaceSetting.equals( "" ) )
		{
			String [] interfaceArray = interfaceSetting.split( "," );

			if ( !interfaceSetting.equals( "" ) )
			{
				for ( int i = 0; i < interfaceArray.length; ++i )
				{
					if ( interfaceArray[i].equals( "LocalRelayServer" ) )
					{
						StaticEntity.getClient().startRelayServer();
						continue;
					}

					KoLmafiaGUI.constructFrame( interfaceArray[i] );
				}
			}
		}

		isInitializing = false;
	}

	public static boolean isInitializing()
	{	return isInitializing;
	}

	public void dispose()
	{
		setVisible( false );

		String setting = StaticEntity.getGlobalProperty( "initialDesktop" );
		KoLFrame [] frames = StaticEntity.getExistingFrames();

		for ( int i = 0; i < frames.length; ++i )
			if ( setting.indexOf( frames[i].getFrameName() ) != -1 )
				frames[i].dispose();

		if ( tabs != null && tabs instanceof CloseTabbedPane )
			((CloseTabbedPane)tabs).removeCloseListener( this );

		super.dispose();
		INSTANCE = null;
	}

	public static boolean instanceExists()
	{	return INSTANCE != null;
	}

	public static KoLDesktop getInstance()
	{
		if ( INSTANCE == null )
			INSTANCE = new KoLDesktop( VERSION_NAME );

		return INSTANCE;
	}

	public static void addTab( KoLFrame content )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex == -1 )
		{
			INSTANCE.tabListing.add( content );
			INSTANCE.tabs.addTab( content.lastTitle, content.getContentPane() );

			if ( content.tabs != null && !isInversionExempt( content ) )
				content.tabs.setTabPlacement( CloseTabbedPane.BOTTOM );

			INSTANCE.tabs.setSelectedIndex( 0 );
		}
		else
		{
			INSTANCE.tabs.setSelectedIndex( tabIndex );
		}
	}

	public static boolean isInversionExempt( KoLFrame content )
	{
		return StaticEntity.getBooleanProperty( "avoidInvertingTabs" ) || content instanceof AdventureFrame || content instanceof FlowerHunterFrame ||
			content instanceof MailboxFrame || content instanceof SendMessageFrame;
	}

	public static void displayDesktop()
	{
		try
		{
			if ( SwingUtilities.isEventDispatchThread() )
				DISPLAYER.run();
			else
				SwingUtilities.invokeAndWait( DISPLAYER );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static class DisplayDesktopRunnable implements Runnable
	{
		public void run()
		{
			KoLDesktop.getInstance().pack();
			KoLDesktop.getInstance().setVisible( true );
			KoLDesktop.getInstance().requestFocus();
		}
	}

	public void pack()
	{
		super.pack();
		if ( this.tabs.getTabCount() > 0 )
			this.tabs.setSelectedIndex( 0 );
	}

	public static void removeTab( KoLFrame content )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
		{
			INSTANCE.tabListing.remove( tabIndex );
			INSTANCE.tabs.removeTabAt( tabIndex );
		}
	}

	public static void requestFocus( KoLFrame content )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
			INSTANCE.tabs.setSelectedIndex( tabIndex );
	}

	public static void setTitle( KoLFrame content, String newTitle )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
			INSTANCE.tabs.setTitleAt( tabIndex, newTitle );
	}

	public static void updateTitle()
	{
		if ( INSTANCE != null )
			INSTANCE.setTitle( INSTANCE.lastTitle );

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		for ( int i = 0; i < frames.length; ++i )
			frames[i].setTitle( frames[i].lastTitle );
	}

	public static void addMainToolbar( JToolBar toolbarPanel )
	{
		if ( toolbarPanel != null )
		{
			toolbarPanel.setFloatable( false );
			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

			toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", "CouncilFrame" ) );

			if ( StaticEntity.getBooleanProperty( "mapLoadsMiniBrowser" ) )
				toolbarPanel.add( new DisplayFrameButton( "Load in Mini Browser", "browser.gif", "RequestFrame" ) );
			else
				toolbarPanel.add( new InvocationButton( "Load in Web Browser", "browser.gif", StaticEntity.getClient(), "openRelayBrowser" ) );

			toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", "CommandDisplayFrame" ) );

			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

			toolbarPanel.add( new DisplayFrameButton( "IcePenguin Express", "mail.gif", "MailboxFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "KoLmafia Chat", "chat.gif", "KoLMessenger" ) );
			toolbarPanel.add( new DisplayFrameButton( "Clan Manager", "clan.gif", "ClanManageFrame" ) );

			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

			toolbarPanel.add( new DisplayFrameButton( "Player Status", "hp.gif", "CharsheetFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", "ItemManageFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Equipment Manager", "equipment.gif", "GearChangeFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", "StoreManageFrame") );

			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

			toolbarPanel.add( new DisplayFrameButton( "Purchase Buffs", "buff.gif", "BuffRequestFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", "FamiliarTrainingFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Player vs. Player", "flower.gif", "FlowerHunterFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Mushroom Plot", "mushroom.gif", "MushroomFrame" ) );

			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

			toolbarPanel.add( new InvocationButton( "Radio KoL", "radsword.gif", StaticEntity.getClient(), "launchRadioKoL" ) );
			toolbarPanel.add( new DisplayFrameButton( "Farmer's Almanac", "calendar.gif", "CalendarFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", "OptionsFrame" ) );

			toolbarPanel.add( Box.createVerticalStrut( 50 ) );
		}
	}

	public static void removeExtraTabs()
	{
		if ( INSTANCE == null )
			return;

		String setting = StaticEntity.getGlobalProperty( "initialDesktop" );
		for ( int i = 0; i < INSTANCE.tabListing.size(); ++i )
		{
			KoLFrame frame = (KoLFrame) INSTANCE.tabListing.get( i );
			if ( setting.indexOf( frame.getFrameName() ) != -1 )
				continue;

			frame.dispose();
		}
	}

	private class MinimizeListener extends WindowAdapter
	{
		public void windowIconified( WindowEvent e )
		{	KoLDesktop.this.setVisible( false );
		}
	}
}
