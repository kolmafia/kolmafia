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

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JPanel;
import javax.swing.JLabel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Date;
import java.util.ArrayList;

public class KoLDesktop extends KoLFrame implements ChangeListener
{
	private static KoLDesktop INSTANCE = null;
	private static boolean isInitializing = false;
	private ArrayList tabListing = new ArrayList();

	protected JPanel compactPane;
	protected JLabel levelLabel, roninLabel, mcdLabel;
	protected JLabel musLabel, mysLabel, moxLabel, drunkLabel;
	protected JLabel hpLabel, mpLabel, meatLabel, advLabel;
	protected JLabel familiarLabel;

	protected KoLCharacterAdapter refreshListener;

	protected KoLDesktop()
	{
	}

	protected KoLDesktop( String title )
	{
		super( "Main Interface" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		getContentPane().setLayout( new BorderLayout() );

		tabs = new JTabbedPane();
		tabs.setTabPlacement( JTabbedPane.TOP );
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		getContentPane().add( tabs, BorderLayout.CENTER );
		addCompactPane();

		JToolBar toolbarPanel = null;

		switch ( StaticEntity.getIntegerProperty( "toolbarPosition" ) )
		{
		case 1:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			getContentPane().add( toolbarPanel, BorderLayout.NORTH );
			break;

		case 2:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			getContentPane().add( toolbarPanel, BorderLayout.SOUTH );
			break;

		case 3:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
			getContentPane().add( toolbarPanel, BorderLayout.WEST );
			break;
		}

		setJMenuBar( new KoLMenuBar() );
		if ( toolbarPanel != null )
			addMainToolbar( toolbarPanel );

		tabs.addChangeListener( this );
		int scriptButtons = StaticEntity.getIntegerProperty( "scriptButtonPosition" );

		if ( scriptButtons != 0 )
		{
			String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );

			JToolBar scriptBar = null;

			if ( scriptButtons == 1 )
			{
				scriptBar = toolbarPanel;
				scriptBar.addSeparator();
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

				getContentPane().add( scriptBarHolder, BorderLayout.EAST );
			}
		}
	}

	public void stateChanged( ChangeEvent e )
	{
		int selectedIndex = tabs.getSelectedIndex();
		if ( selectedIndex != -1 && selectedIndex < tabListing.size() )
			((KoLFrame) tabListing.get( selectedIndex )).requestFocus();
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
				for ( int i = 0; i < interfaceArray.length; ++i )
					KoLmafiaGUI.constructFrame( interfaceArray[i] );
		}

		isInitializing = false;
		KoLmafia.enableDisplay();
	}

	public static boolean isInitializing()
	{	return isInitializing;
	}

	public void dispose()
	{
		KoLFrame [] frames = new KoLFrame[ tabListing.size() ];
		tabListing.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].dispose();

		INSTANCE = null;
		super.dispose();
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

			if ( content.tabs != null && !(content instanceof AdventureFrame) )
				content.tabs.setTabPlacement( JTabbedPane.BOTTOM );

			INSTANCE.tabs.setSelectedIndex( 0 );
		}
		else
		{
			INSTANCE.tabs.setSelectedIndex( tabIndex );
		}
	}

	public void pack()
	{
		super.pack();
		if ( tabs.getTabCount() > 0 )
			tabs.setSelectedIndex( 0 );
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

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].setTitle( frames[i].lastTitle );
	}

	public static void addMainToolbar( JToolBar toolbarPanel )
	{
		if ( toolbarPanel != null )
		{
			toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", "CouncilFrame" ) );
			toolbarPanel.add( new InvocationButton( "Load in Browser", "browser.gif", StaticEntity.getClient(), "loadPreferredBrowser" ) );
			toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", "CommandDisplayFrame" ) );
			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "IcePenguin Express", "mail.gif", "MailboxFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "KoLmafia Chat", "chat.gif", "KoLMessenger" ) );
			toolbarPanel.add( new DisplayFrameButton( "Clan Manager", "clan.gif", "ClanManageFrame" ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Player Status", "hp.gif", "CharsheetFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", "ItemManageFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Equipment Manager", "equipment.gif", "GearChangeFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", "StoreManageFrame") );
			toolbarPanel.add( new DisplayFrameButton( "Hagnk's Storage", "hagnk.gif", "HagnkStorageFrame" ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Purchase Buffs", "buff.gif", "BuffRequestFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", "FamiliarTrainingFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Player vs. Player", "flower.gif", "FlowerHunterFrame" ) );
			toolbarPanel.add( new DisplayFrameButton( "Mushroom Plot", "mushroom.gif", "MushroomFrame" ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", "OptionsFrame" ) );
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
			if ( setting.indexOf( frame.getFrameName() ) != -1 || frame instanceof ChatFrame )
				continue;

			frame.dispose();
		}
	}
}