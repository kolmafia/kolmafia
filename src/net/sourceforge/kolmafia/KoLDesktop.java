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

package net.sourceforge.kolmafia;

import com.sun.java.forums.CloseableTabbedPane;

import java.awt.Dimension;
import java.awt.Frame;

import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.SendMessageFrame;

import net.sourceforge.kolmafia.swingui.button.DisplayFrameButton;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.button.RelayBrowserButton;

import net.sourceforge.kolmafia.swingui.listener.TabFocusingListener;

import net.sourceforge.kolmafia.utilities.PauseObject;

import net.sourceforge.kolmafia.webui.RelayLoader;

import tab.CloseListener;
import tab.CloseTabPaneUI;
import tab.CloseTabbedPane;

public class KoLDesktop
	extends GenericFrame
	implements CloseListener
{
	private final MinimizeListener minimizeListener = new MinimizeListener();
	private static KoLDesktop INSTANCE = null;
	private static boolean isInitializing = false;

	private final List tabListing = new ArrayList();

	public JPanel compactPane;

	private JProgressBar memoryUsageLabel;
	public JLabel levelLabel, roninLabel, mcdLabel;
	public JLabel musLabel, mysLabel, moxLabel, drunkLabel;
	public JLabel hpLabel, mpLabel, meatLabel, advLabel;
	public JLabel familiarLabel;

	public KoLDesktop( final String title )
	{
		super( StaticEntity.getVersion() + " Main Interface" );

		if ( StaticEntity.usesSystemTray() )
		{
			this.addWindowListener( minimizeListener );
		}

		this.tabs.setTabPlacement( SwingConstants.TOP );
		this.setCenterComponent( this.tabs );

		this.addCompactPane();

		this.getToolbar();
		this.addScriptPane();

		KoLDesktop.INSTANCE = this;
		new MemoryUsageMonitor().start();
	}

	public boolean showInWindowMenu()
	{
		return false;
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public JTabbedPane getTabbedPane()
	{
		if ( Preferences.getBoolean( "useDecoratedTabs" ) )
		{
			JTabbedPane tabs = new CloseTabbedPane();

			if ( Preferences.getBoolean( "allowCloseableDesktopTabs" ) )
			{
				( (CloseTabbedPane) tabs ).setCloseIconStyle( CloseTabPaneUI.RED_CLOSE_ICON );
				( (CloseTabbedPane) tabs ).addCloseListener( this );
			}

			return tabs;
		}

		return Preferences.getBoolean( "allowCloseableDesktopTabs" ) ? new CloseableTabbedPane() : new JTabbedPane();
	}

	public void closeOperation( final MouseEvent e, final int overTabIndex )
	{
		if ( overTabIndex == -1 )
		{
			return;
		}

		GenericFrame gframe = (GenericFrame) this.tabListing.remove( overTabIndex );
		gframe.dispose();

		this.tabs.removeTabAt( overTabIndex );
	}

	public void initializeTabs()
	{
		if ( KoLDesktop.isInitializing )
		{
			return;
		}

		KoLDesktop.isInitializing = true;
		KoLmafiaGUI.checkFrameSettings();

		String interfaceSetting = Preferences.getString( "initialDesktop" );
		if ( !interfaceSetting.equals( "" ) )
		{
			String[] interfaceArray = interfaceSetting.split( "," );

			if ( !interfaceSetting.equals( "" ) )
			{
				for ( int i = 0; i < interfaceArray.length; ++i )
				{
					if ( interfaceArray[ i ].equals( "LocalRelayServer" ) )
					{
						RelayLoader.startRelayServer();
						continue;
					}

					KoLmafiaGUI.constructFrame( interfaceArray[ i ] );
				}
			}
		}

		this.pack();

		this.tabs.addChangeListener( new TabFocusingListener() );

		KoLDesktop.isInitializing = false;
	}

	public static final boolean isInitializing()
	{
		return KoLDesktop.isInitializing;
	}

	public void dispose()
	{
		StaticEntity.unregisterPanels( this.tabs );

		this.removeWindowListener( minimizeListener );

		if ( Preferences.getBoolean( "rememberDesktopSize" ) )
		{
			Dimension tempDim = this.getSize();
			Preferences.setInteger( "desktopHeight", (int) tempDim.getHeight() );
			Preferences.setInteger( "desktopWidth", (int) tempDim.getWidth() );
		}

		Iterator tabIterator = this.tabListing.iterator();

		while ( tabIterator.hasNext() )
		{
			GenericFrame gframe = (GenericFrame) tabIterator.next();

			gframe.dispose();

			tabIterator.remove();
		}

		this.tabs.removeAll();

		KoLDesktop.INSTANCE = null;

		super.dispose();
	}

	public static final boolean instanceExists()
	{
		return KoLDesktop.INSTANCE != null;
	}

	public static final KoLDesktop getInstance()
	{
		if ( KoLDesktop.INSTANCE == null )
		{
			new KoLDesktop( StaticEntity.getVersion() );
			KoLDesktop.INSTANCE.initializeTabs();

			if ( Preferences.getBoolean( "rememberDesktopSize" ) )
			{
				int width = Preferences.getInteger( "desktopWidth" );
				int height = Preferences.getInteger( "desktopHeight" );

				KoLDesktop.INSTANCE.setSize( width, height );
			}

			KoLDesktop.INSTANCE.setVisible( true );
		}

		return KoLDesktop.INSTANCE;
	}

	public static final void addTab( final GenericFrame content )
	{
		if ( KoLDesktop.INSTANCE == null )
		{
			return;
		}

		int tabIndex = KoLDesktop.INSTANCE.tabListing.indexOf( content );
		if ( tabIndex == -1 )
		{
			KoLDesktop.INSTANCE.tabListing.add( content );
			KoLDesktop.INSTANCE.tabs.addTab( content.getLastTitle(), content.getCenterComponent() );

			if ( content.tabs != null && !KoLDesktop.isInversionExempt( content ) )
			{
				content.tabs.setTabPlacement( SwingConstants.BOTTOM );
			}
		}
		else
		{
			KoLDesktop.INSTANCE.tabs.setSelectedIndex( tabIndex );
		}
	}

	public static final boolean isInversionExempt( final GenericFrame content )
	{
		return content instanceof AdventureFrame || content instanceof SendMessageFrame;
	}

	public void pack()
	{
		super.pack();
	}

	public static final boolean showComponent( final GenericFrame content )
	{
		if ( KoLDesktop.INSTANCE == null )
		{
			return false;
		}

		int tabIndex = KoLDesktop.INSTANCE.tabListing.indexOf( content );
		if ( tabIndex == -1 )
		{
			return false;
		}

		int currentTabIndex = KoLDesktop.INSTANCE.tabs.getSelectedIndex();
		if ( tabIndex == currentTabIndex )
		{
			JComponent selected = (JComponent)KoLDesktop.INSTANCE.tabs.getSelectedComponent();
			selected.requestFocus();
		}
		else
		{
			KoLDesktop.INSTANCE.tabs.setSelectedIndex( tabIndex );
		}

		return true;
	}

	public static final void setTitle( final GenericFrame content, final String newTitle )
	{
		if ( KoLDesktop.INSTANCE == null )
		{
			return;
		}

		int tabIndex = KoLDesktop.INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
		{
			KoLDesktop.INSTANCE.tabs.setTitleAt( tabIndex, newTitle );
		}
	}

	public static final void updateTitle()
	{
		if ( KoLDesktop.INSTANCE != null )
		{
			KoLDesktop.INSTANCE.setTitle( KoLDesktop.INSTANCE.lastTitle );
		}

		Frame[] frames = Frame.getFrames();
		for ( int i = 0; i < frames.length; ++i )
		{
			if ( frames[ i ] instanceof GenericFrame )
			{
				GenericFrame frame = (GenericFrame) frames[ i ];

				frame.setTitle( frame.getLastTitle() );
			}
		}
	}

	public JToolBar getToolbar()
	{
		JToolBar toolbarPanel = super.getToolbar();
		if ( toolbarPanel == null )
		{
			return null;
		}

		toolbarPanel.add( Box.createVerticalStrut( 50 ) );

		toolbarPanel.add( Box.createHorizontalStrut( 5 ) );

		toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", "CouncilFrame" ) );
		toolbarPanel.add( new RelayBrowserButton( "Load in Web Browser", "browser.gif", null ) );

		toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", "CommandDisplayFrame" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new RelayBrowserButton( "Read KoLmail", "mail.gif", "game.php?mainpane=messages" ) );
		toolbarPanel.add( new DisplayFrameButton( "KoLmafia Chat", "chat.gif", "ChatManager" ) );
		toolbarPanel.add( new DisplayFrameButton( "Clan Manager", "clan.gif", "ClanManageFrame" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new DisplayFrameButton( "Daily Deeds", "hp.gif", "CharSheetFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", "ItemManageFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Equipment Manager", "equipment.gif", "GearChangeFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", "StoreManageFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Coin Masters", "coin.gif", "CoinmastersFrame" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new DisplayFrameButton( "Purchase Buffs", "buff.gif", "BuffRequestFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", "FamiliarTrainingFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Mushroom Plot", "mushroom.gif", "MushroomFrame" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new RelayBrowserButton( "Radio KoL", "radsword.gif", "http://209.9.238.5:8794/listen.pls" ) );
		toolbarPanel.add( new DisplayFrameButton( "Farmer's Almanac", "calendar.gif", "CalendarFrame" ) );
		toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", "OptionsFrame" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );
		toolbarPanel.add( Box.createHorizontalGlue() );

		this.memoryUsageLabel = new JProgressBar( JProgressBar.HORIZONTAL );
		this.memoryUsageLabel.setStringPainted( true );

		toolbarPanel.add( this.memoryUsageLabel );
		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );
		toolbarPanel.add( new InvocationButton( "Collect Garbage", "trashield.gif", StaticEntity.getClient(), "gc" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 5 ) );

		return toolbarPanel;
	}

	public static final void removeExtraTabs()
	{
		if ( KoLDesktop.INSTANCE == null )
		{
			return;
		}

		String setting = Preferences.getString( "initialDesktop" );
		for ( int i = 0; i < KoLDesktop.INSTANCE.tabListing.size(); ++i )
		{
			GenericFrame frame = (GenericFrame) KoLDesktop.INSTANCE.tabListing.get( i );
			if ( !( frame instanceof ChatFrame ) && setting.indexOf( frame.getFrameName() ) == -1 )
			{
				frame.dispose();
			}
		}
	}

	private class MinimizeListener
		extends WindowAdapter
	{
		public void windowIconified( final WindowEvent e )
		{
			KoLDesktop.this.setVisible( false );
		}
	}

	private static class DisplayDesktopFocusRunnable
		implements Runnable
	{
		public void run()
		{
			KoLDesktop.getInstance().setVisible( true );
			KoLDesktop.getInstance().requestFocus();
		}
	}

	private class MemoryUsageMonitor
		extends Thread
	{
		private PauseObject pauser;

		public MemoryUsageMonitor()
		{
			this.pauser = new PauseObject();

			this.setDaemon( true );
		}

		public void run()
		{
			while ( KoLDesktop.INSTANCE == KoLDesktop.this )
			{
				this.pauser.pause( 2000 );

				Runtime runtime = Runtime.getRuntime();

				int maxMemory = (int) ( runtime.maxMemory() >> 10 );
				int heapMemory = (int) ( runtime.totalMemory() >> 10 );
				int usedMemory = (int) ( heapMemory - ( runtime.freeMemory() >> 10 ) );

				KoLDesktop.this.memoryUsageLabel.setMaximum( maxMemory );
				KoLDesktop.this.memoryUsageLabel.setValue( usedMemory );

				KoLDesktop.this.memoryUsageLabel.setString( usedMemory + " KB / " + maxMemory + " KB" );
			}
		}
	}

}
