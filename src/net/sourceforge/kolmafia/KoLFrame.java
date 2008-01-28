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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.image.ImageObserver;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import tab.CloseTabbedPane;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public abstract class KoLFrame
	extends JFrame
{
	private static KoLFrame activeWindow = null;

	protected HashMap listenerMap;
	private KoLMenuBar menuBar;

	public JTabbedPane tabs;
	public String lastTitle;
	public String frameName;
	public JPanel framePanel;

	public StatusRefresher refresher = null;
	public KoLCharacterAdapter refreshListener = null;

	static
	{
		KoLFrame.compileScripts();
		KoLFrame.compileBookmarks();
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be associated with the given
	 * StaticEntity.getClient().
	 */

	public KoLFrame()
	{
		this( "" );
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be associated with the given
	 * StaticEntity.getClient().
	 */

	public KoLFrame( final String title )
	{
		this.setTitle( title );
		this.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

		this.tabs = this.getTabbedPane();
		this.framePanel = new JPanel( new BorderLayout( 0, 0 ) );

		this.frameName = this.getClass().getName();
		this.frameName = this.frameName.substring( this.frameName.lastIndexOf( "." ) + 1 );

		if ( this.shouldAddStatusBar() )
		{
			JScrollPane statusBar = KoLConstants.commandBuffer.setChatDisplay( new RequestPane() );
			JComponentUtilities.setComponentSize( statusBar, new Dimension( 200, 50 ) );

			JSplitPane doublePane =
				new JSplitPane( JSplitPane.VERTICAL_SPLIT, new SimpleScrollPane( this.framePanel ), statusBar );
			this.getContentPane().add( doublePane, BorderLayout.CENTER );

			doublePane.setOneTouchExpandable( true );
			doublePane.setDividerLocation( 0.9 );
		}
		else
		{
			this.getContentPane().add( this.framePanel, BorderLayout.CENTER );
		}

		this.addHotKeys();

		boolean shouldAddFrame =
			!( this instanceof KoLDesktop ) && !( this instanceof ContactListFrame ) && !( this instanceof LoginFrame );

		if ( this instanceof ChatFrame )
		{
			shouldAddFrame = !ChatManager.usingTabbedChat() || this instanceof TabbedChatFrame;
		}

		if ( shouldAddFrame )
		{
			StaticEntity.registerFrame( this );
		}
	}

	public void setJMenuBar( final KoLMenuBar menuBar )
	{
		this.menuBar = menuBar;
		super.setJMenuBar( menuBar );
	}

	protected void addActionListener( final JCheckBox component, final ActionListener listener )
	{
		if ( this.listenerMap == null )
		{
			this.listenerMap = new HashMap();
		}

		component.addActionListener( listener );
		this.listenerMap.put( component, new WeakReference( listener ) );
	}

	protected void addActionListener( final JComboBox component, final ActionListener listener )
	{
		if ( this.listenerMap == null )
		{
			this.listenerMap = new HashMap();
		}

		component.addActionListener( listener );
		this.listenerMap.put( component, new WeakReference( listener ) );
	}

	private void removeActionListener( final Object component, final ActionListener listener )
	{
		if ( component instanceof JCheckBox )
		{
			( (JCheckBox) component ).removeActionListener( listener );
		}
		if ( component instanceof JComboBox )
		{
			( (JComboBox) component ).removeActionListener( listener );
		}
	}

	protected void removeThreadedListeners()
	{
		if ( this.listenerMap == null )
		{
			return;
		}

		Object[] keys = this.listenerMap.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			WeakReference ref = (WeakReference) this.listenerMap.get( keys[ i ] );
			if ( ref == null )
			{
				continue;
			}

			Object listener = ref.get();
			if ( listener == null )
			{
				continue;
			}

			if ( listener instanceof ActionListener )
			{
				this.removeActionListener( keys[ i ], (ActionListener) listener );
			}
		}

		this.listenerMap.clear();
		this.listenerMap = null;
	}

	public boolean appearsInTab()
	{
		return KoLFrame.appearsInTab( this.frameName );
	}

	public static boolean appearsInTab( String frameName )
	{
		String tabSetting = Preferences.getString( "initialDesktop" );
		return tabSetting.indexOf( frameName ) != -1;
	}

	public boolean shouldAddStatusBar()
	{
		return Preferences.getBoolean( "addStatusBarToFrames" ) && !this.appearsInTab();
	}

	public JTabbedPane getTabbedPane()
	{
		return Preferences.getBoolean( "useDecoratedTabs" ) ? new CloseTabbedPane() : new JTabbedPane();
	}

	public void addHotKeys()
	{
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_ESCAPE, new WorldPeaceListener() );
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_F5, new RefreshKeyListener() );

		if ( !System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			JComponentUtilities.addGlobalHotKey(
				this.getRootPane(), KeyEvent.VK_F6, InputEvent.CTRL_MASK, new TabForwardListener() );
			JComponentUtilities.addGlobalHotKey(
				this.getRootPane(), KeyEvent.VK_F6, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK,
				new TabBackwardListener() );
		}
	}

	private class WorldPeaceListener
		extends ThreadedListener
	{
		public void run()
		{
			RequestThread.declareWorldPeace();
		}
	}

	private class RefreshKeyListener
		extends ThreadedListener
	{
		public void run()
		{
			RequestThread.openRequestSequence();
			StaticEntity.getClient().refreshSession();
			RequestThread.closeRequestSequence();
		}
	}

	private class TabForwardListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			if ( KoLFrame.this.tabs == null )
			{
				return;
			}

			KoLFrame.this.tabs.setSelectedIndex( ( KoLFrame.this.tabs.getSelectedIndex() + 1 ) % KoLFrame.this.tabs.getTabCount() );
		}
	}

	private class TabBackwardListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			if ( KoLFrame.this.tabs == null )
			{
				return;
			}

			KoLFrame.this.tabs.setSelectedIndex( KoLFrame.this.tabs.getSelectedIndex() == 0 ? KoLFrame.this.tabs.getTabCount() - 1 : KoLFrame.this.tabs.getSelectedIndex() - 1 );
		}
	}

	public final void addTab( final String name, final JComponent panel )
	{
		if ( this.tabs == null )
		{
			return;
		}

		this.tabs.setOpaque( true );

		SimpleScrollPane scroller = new SimpleScrollPane( panel );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		this.tabs.add( name, scroller );
	}

	public final void setTitle( final String newTitle )
	{
		this.lastTitle = newTitle;
		KoLDesktop.setTitle( this, newTitle );

		if ( this instanceof LoginFrame )
		{
			super.setTitle( this.lastTitle );
			return;
		}

		String username = KoLCharacter.getUserName();
		if ( username.equals( "" ) )
		{
			username = "Not Logged In";
		}

		super.setTitle( this.lastTitle + " (" + username + ")" );
	}

	public void requestFocus()
	{
	}

	public boolean useSidePane()
	{
		return false;
	}

	public JToolBar getToolbar()
	{
		return this.getToolbar( false );
	}

	public JToolBar getToolbar( final boolean force )
	{
		JToolBar toolbarPanel = null;

		if ( force )
		{
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			this.getContentPane().add( toolbarPanel, BorderLayout.NORTH );
		}
		else
		{
			switch ( Preferences.getInteger( "toolbarPosition" ) )
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

			case 4:
				toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
				this.getContentPane().add( toolbarPanel, BorderLayout.EAST );
				break;

			default:

				toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
				if ( this instanceof LoginFrame || this instanceof ChatFrame )
				{
					this.getContentPane().add( toolbarPanel, BorderLayout.NORTH );
					break;
				}
			}
		}

		if ( toolbarPanel != null )
		{
			toolbarPanel.setFloatable( false );
		}

		return toolbarPanel;
	}

	/**
	 * Overrides the default behavior of dispose so that the frames are removed from the internal list of existing
	 * frames. Also allows for automatic exit.
	 */

	public void dispose()
	{
		if ( this.isVisible() )
		{
			this.rememberPosition();
		}

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		KoLDesktop.removeTab( this );
		StaticEntity.unregisterFrame( this );
		KoLConstants.existingFrames.remove( this );

		if ( this.refreshListener != null )
		{
			KoLCharacter.removeCharacterListener( this.refreshListener );
		}

		this.removeThreadedListeners();
		this.getRootPane().resetKeyboardActions();

		if ( this.menuBar != null )
		{
			this.menuBar.dispose();
		}

		super.dispose();
		this.checkForLogout();
	}

	private void checkForLogout()
	{
		if ( !KoLConstants.existingFrames.isEmpty() || LoginFrame.instanceExists() )
		{
			return;
		}

		KoLFrame.createDisplay( LoginFrame.class );
		RequestThread.postRequest( new LogoutRequest() );
	}

	public String toString()
	{
		return this.lastTitle;
	}

	public String getFrameName()
	{
		return this.frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component. Note that this method can only be used if the
	 * KoLFrame on which it is called has not yet added any components. If there are any added components, this method
	 * will do nothing.
	 */

	public void addCompactPane()
	{
		if ( this.refresher != null )
		{
			return;
		}

		this.refresher = new StatusRefresher();
		this.refresher.run();

		this.refreshListener = new KoLCharacterAdapter( this.refresher );
		KoLCharacter.addCharacterListener( this.refreshListener );

		this.refresher.getCompactPane().setBackground( KoLConstants.ENABLED_COLOR );
		this.getContentPane().add( this.refresher.getCompactPane(), BorderLayout.WEST );
	}

	public static class StatusRefresher
		implements Runnable
	{
		private final JPanel compactPane;
		private final JPanel levelPanel;
		private JProgressBar levelMeter;
		private JLabel levelLabel, roninLabel, mcdLabel;
		private JLabel musLabel, mysLabel, moxLabel;
		private JLabel fullLabel, drunkLabel, spleenLabel;
		private JLabel hpLabel, mpLabel, meatLabel, advLabel;
		private JLabel familiarLabel;
		private JLabel mlLabel, encLabel, initLabel;
		private JLabel expLabel, meatDropLabel, itemDropLabel;

		public StatusRefresher()
		{
			JPanel labelPanel, valuePanel;

			JPanel[] panels = new JPanel[ 6 ];
			int panelCount = -1;

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			this.levelPanel = panels[ 0 ];

			panels[ panelCount ].add( this.levelLabel = new JLabel( " ", JLabel.CENTER ), BorderLayout.NORTH );
			panels[ panelCount ].add( this.levelMeter = new JProgressBar(), BorderLayout.CENTER );
			this.levelMeter.setOpaque( true );
			this.levelMeter.setStringPainted( true );
			JComponentUtilities.setComponentSize( this.levelMeter, 40, 6 );
			panels[ panelCount ].add( Box.createHorizontalStrut( 10 ), BorderLayout.WEST );
			panels[ panelCount ].add( Box.createHorizontalStrut( 10 ), BorderLayout.EAST );
			panels[ panelCount ].setOpaque( false );

			JPanel holderPanel = new JPanel( new GridLayout( 2, 1 ) );
			holderPanel.add( this.roninLabel = new JLabel( " ", JLabel.CENTER ) );
			holderPanel.add( this.mcdLabel = new JLabel( " ", JLabel.CENTER ) );
			holderPanel.setOpaque( false );
			panels[ panelCount ].add( holderPanel, BorderLayout.SOUTH );

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			panels[ panelCount ].setOpaque( false );

			labelPanel = new JPanel( new GridLayout( 3, 1 ) );
			labelPanel.setOpaque( false );

			labelPanel.add( new JLabel( "   Mus: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "   Mys: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "   Mox: ", JLabel.RIGHT ) );

			valuePanel = new JPanel( new GridLayout( 3, 1 ) );
			valuePanel.setOpaque( false );

			valuePanel.add( this.musLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.mysLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.moxLabel = new JLabel( " ", JLabel.LEFT ) );

			panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
			panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			panels[ panelCount ].setOpaque( false );

			labelPanel = new JPanel( new GridLayout( 4, 1 ) );
			labelPanel.setOpaque( false );

			labelPanel.add( new JLabel( "   HP: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "   MP: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "   Meat: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "   Adv: ", JLabel.RIGHT ) );

			valuePanel = new JPanel( new GridLayout( 4, 1 ) );
			valuePanel.setOpaque( false );

			valuePanel.add( this.hpLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.mpLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.meatLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.advLabel = new JLabel( " ", JLabel.LEFT ) );

			panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
			panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			panels[ panelCount ].setOpaque( false );

			labelPanel = new JPanel( new GridLayout( 3, 1 ) );
			labelPanel.setOpaque( false );

			labelPanel.add( new JLabel( "  Full: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "  Drunk: ", JLabel.RIGHT ) );
			labelPanel.add( new JLabel( "  Spleen: ", JLabel.RIGHT ) );

			valuePanel = new JPanel( new GridLayout( 3, 1 ) );
			valuePanel.setOpaque( false );

			valuePanel.add( this.fullLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.drunkLabel = new JLabel( " ", JLabel.LEFT ) );
			valuePanel.add( this.spleenLabel = new JLabel( " ", JLabel.LEFT ) );

			panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
			panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 1, 1 ) );
			panels[ panelCount ].add( this.familiarLabel = new UnanimatedLabel() );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 6, 2 ) );
			panels[ panelCount ].add( new JLabel( "ML: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.mlLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Enc: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.encLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Init: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.initLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Exp: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.expLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Meat: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.meatDropLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Item: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.itemDropLabel = new JLabel( " ", JLabel.LEFT ) );

			JPanel compactContainer = new JPanel();
			compactContainer.setOpaque( false );
			compactContainer.setLayout( new BoxLayout( compactContainer, BoxLayout.Y_AXIS ) );

			for ( int i = 0; i < panels.length; ++i )
			{
				panels[ i ].setOpaque( false );
				compactContainer.add( panels[ i ] );
				compactContainer.add( Box.createVerticalStrut( 20 ) );
			}

			JPanel compactCard = new JPanel( new CardLayout( 8, 8 ) );
			compactCard.setOpaque( false );
			compactCard.add( compactContainer, "" );

			JPanel refreshPanel = new JPanel();
			refreshPanel.setOpaque( false );
			refreshPanel.add( new RequestButton( "Refresh Status", "refresh.gif", new CharSheetRequest() ) );

			this.compactPane = new JPanel( new BorderLayout() );
			this.compactPane.add( compactCard, BorderLayout.NORTH );
			this.compactPane.add( refreshPanel, BorderLayout.SOUTH );

			this.levelLabel.setForeground( Color.BLACK );
			this.roninLabel.setForeground( Color.BLACK );
			this.mcdLabel.setForeground( Color.BLACK );
			this.musLabel.setForeground( Color.BLACK );
			this.mysLabel.setForeground( Color.BLACK );
			this.moxLabel.setForeground( Color.BLACK );
			this.fullLabel.setForeground( Color.BLACK );
			this.drunkLabel.setForeground( Color.BLACK );
			this.spleenLabel.setForeground( Color.BLACK );
			this.hpLabel.setForeground( Color.BLACK );
			this.mpLabel.setForeground( Color.BLACK );
			this.meatLabel.setForeground( Color.BLACK );
			this.advLabel.setForeground( Color.BLACK );
			this.familiarLabel.setForeground( Color.BLACK );
			this.mlLabel.setForeground( Color.BLACK );
			this.encLabel.setForeground( Color.BLACK );
			this.initLabel.setForeground( Color.BLACK );
			this.expLabel.setForeground( Color.BLACK );
			this.meatDropLabel.setForeground( Color.BLACK );
			this.itemDropLabel.setForeground( Color.BLACK );
		}

		public String getStatText( final int adjusted, final int base )
		{
			return adjusted == base ? "<html>" + Integer.toString( base ) : adjusted > base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" : "<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
		}

		public void run()
		{
			this.levelLabel.setText( "Level " + KoLCharacter.getLevel() );

			if ( KoLCharacter.inBadMoon() )
			{
				this.roninLabel.setText( "(Bad Moon)" );
			}
			else if ( KoLCharacter.isHardcore() )
			{
				this.roninLabel.setText( "(Hardcore)" );
			}
			else if ( KoLCharacter.canInteract() )
			{
				this.roninLabel.setText( "(Ronin Clear)" );
			}
			else
			{
				this.roninLabel.setText( "(Ronin for " + ( 1000 - KoLCharacter.getCurrentRun() ) + ")" );
			}

			this.mcdLabel.setText( "ML @ " + KoLCharacter.getSignedMLAdjustment() );

			this.musLabel.setText( this.getStatText( KoLCharacter.getAdjustedMuscle(), KoLCharacter.getBaseMuscle() ) );
			this.mysLabel.setText( this.getStatText(
				KoLCharacter.getAdjustedMysticality(), KoLCharacter.getBaseMysticality() ) );
			this.moxLabel.setText( this.getStatText( KoLCharacter.getAdjustedMoxie(), KoLCharacter.getBaseMoxie() ) );

			this.fullLabel.setText( KoLCharacter.getFullness() + " / " + KoLCharacter.getFullnessLimit() );
			this.drunkLabel.setText( KoLCharacter.getInebriety() + " / " + KoLCharacter.getInebrietyLimit() );
			this.spleenLabel.setText( KoLCharacter.getSpleenUse() + " / " + KoLCharacter.getSpleenLimit() );

			this.hpLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentHP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			this.mpLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );
			this.meatLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			this.advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );

			this.mlLabel.setText( KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
			this.encLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
			this.initLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );
			this.expLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
			this.meatDropLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			this.itemDropLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );

			int currentLevel = KoLCharacter.calculateLastLevel();
			int nextLevel = KoLCharacter.calculateNextLevel();
			int totalPrime = KoLCharacter.getTotalPrime();

			this.levelMeter.setMaximum( nextLevel - currentLevel );
			this.levelMeter.setValue( totalPrime - currentLevel );
			this.levelMeter.setString( " " );

			this.levelPanel.setToolTipText( "<html>&nbsp;&nbsp;" + KoLCharacter.getAdvancement() + "&nbsp;&nbsp;<br>&nbsp;&nbsp;(" + KoLConstants.COMMA_FORMAT.format( nextLevel - totalPrime ) + " subpoints needed)&nbsp;&nbsp;</html>" );

			FamiliarData familiar = KoLCharacter.getFamiliar();
			int id = familiar == null ? -1 : familiar.getId();

			if ( id == -1 )
			{
				this.familiarLabel.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
				this.familiarLabel.setText( "0 lbs." );
			}
			else
			{
				this.familiarLabel.setIcon( FamiliarDatabase.getFamiliarImage( id ) );
				this.familiarLabel.setText( familiar.getModifiedWeight() + ( familiar.getModifiedWeight() == 1 ? " lb." : " lbs." ) );
			}
			this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
			this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
		}

		public JPanel getCompactPane()
		{
			return this.compactPane;
		}
	}

	public void setStatusMessage( final String message )
	{
	}

	public void updateDisplayState( final int displayState )
	{
		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		switch ( displayState )
		{
		case KoLConstants.ABORT_STATE:
		case KoLConstants.ERROR_STATE:

			if ( this.refresher != null )
			{
				this.refresher.getCompactPane().setBackground( KoLConstants.ERROR_COLOR );
			}

			this.setEnabled( true );
			break;

		case KoLConstants.ENABLE_STATE:

			if ( this.refresher != null )
			{
				this.refresher.getCompactPane().setBackground( KoLConstants.ENABLED_COLOR );
			}

			this.setEnabled( true );
			break;

		default:

			if ( this.refresher != null )
			{
				this.refresher.getCompactPane().setBackground( KoLConstants.DISABLED_COLOR );
			}

			this.setEnabled( false );
			break;
		}
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled() method does not call the superclass's version.
	 *
	 * @return <code>true</code>
	 */

	public final boolean isEnabled()
	{
		return true;
	}

	public void setEnabled( final boolean isEnabled )
	{
	}

	/**
	 * In order to keep the user interface from freezing (or at least appearing to freeze), this internal class is used
	 * to process the request for viewing frames.
	 */

	public static class DisplayFrameButton
		extends ThreadedButton
	{
		private final String frameClass;

		public DisplayFrameButton( final String text, final String frameClass )
		{
			super( text );
			this.frameClass = frameClass;
		}

		public DisplayFrameButton( final String tooltip, final String icon, final String frameClass )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			this.setToolTipText( tooltip );

			this.frameClass = frameClass;
		}

		public void run()
		{
			KoLmafiaGUI.constructFrame( this.frameClass );
		}
	}

	/**
	 * Internal class used to invoke the given no-parameter method on the given object. This is used whenever there is
	 * the need to invoke a method and the creation of an additional class is unnecessary.
	 */

	public static class InvocationButton
		extends ThreadedButton
	{
		public Object object;
		public Method method;

		public InvocationButton( final String text, final Object object, final String methodName )
		{
			this( text, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( final String text, final Class c, final String methodName )
		{
			super( text );
			this.object = c;

			this.completeConstruction( c, methodName );
		}

		public InvocationButton( final String tooltip, final String icon, final Object object, final String methodName )
		{
			this( tooltip, icon, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( final String tooltip, final String icon, final Class c, final String methodName )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );

			this.object = c;
			this.setToolTipText( tooltip );
			this.completeConstruction( c, methodName );
		}

		public void completeConstruction( final Class c, final String methodName )
		{
			try
			{
				this.method = c.getMethod( methodName, KoLConstants.NOPARAMS );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		public void run()
		{
			RequestThread.openRequestSequence();

			try
			{
				if ( this.method != null )
				{
					this.method.invoke( this.object, null );
				}
			}
			catch ( Exception e1 )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e1 );
			}

			RequestThread.closeRequestSequence();
		}
	}

	/**
	 * An internal class used to handle requests to open a new frame using a local panel inside of the adventure frame.
	 */

	public static class KoLPanelFrameButton
		extends ThreadedButton
	{
		public Object[] parameters;

		public KoLPanelFrameButton( final String tooltip, final String icon, final ActionPanel panel )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			this.setToolTipText( tooltip );

			this.parameters = new Object[ 2 ];
			this.parameters[ 0 ] = tooltip;
			this.parameters[ 1 ] = panel;
		}

		public void run()
		{
			KoLFrame.createDisplay( KoLPanelFrame.class, this.parameters );
		}
	}

	public static class RequestButton
		extends ThreadedButton
	{
		public GenericRequest request;

		public RequestButton( final String title, final GenericRequest request )
		{
			super( title );
			this.request = request;
		}

		public RequestButton( final String title, final String icon, final GenericRequest request )
		{
			super( JComponentUtilities.getImage( icon ) );
			this.setToolTipText( title );
			this.request = request;
		}

		public void run()
		{
			RequestThread.postRequest( this.request );
			RequestThread.postRequest( CharPaneRequest.getInstance() );
		}
	}

	public static final void alert( final String message )
	{
		JOptionPane.showMessageDialog( KoLFrame.activeWindow, KoLFrame.basicTextWrap( message ) );
	}

	public static final boolean confirm( final String message )
	{
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
			KoLFrame.activeWindow, KoLFrame.basicTextWrap( message ), "", JOptionPane.YES_NO_OPTION );
	}

	public static final String input( final String message )
	{
		return JOptionPane.showInputDialog( KoLFrame.activeWindow, KoLFrame.basicTextWrap( message ) );
	}

	public static final String input( final String message, final String initial )
	{
		return JOptionPane.showInputDialog( KoLFrame.activeWindow, KoLFrame.basicTextWrap( message ), initial );
	}

	public static final Object input( final String message, final LockableListModel inputs )
	{
		JList selector = new JList( inputs );

		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( new FilterTextField( selector ), BorderLayout.NORTH );
		panel.add( new SimpleScrollPane( selector ), BorderLayout.CENTER );

		int option =
			JOptionPane.showConfirmDialog(
				KoLFrame.activeWindow, panel, KoLFrame.basicTextWrap( message ), JOptionPane.OK_CANCEL_OPTION );
		return option == JOptionPane.CANCEL_OPTION ? null : selector.getSelectedValue();
	}

	public static final Object[] multiple( final String message, final LockableListModel inputs )
	{
		JList selector = new JList( inputs );
		selector.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( new FilterTextField( selector ), BorderLayout.NORTH );
		panel.add( new SimpleScrollPane( selector ), BorderLayout.CENTER );

		int option =
			JOptionPane.showConfirmDialog(
				KoLFrame.activeWindow, panel, KoLFrame.basicTextWrap( message ), JOptionPane.OK_CANCEL_OPTION );
		return option == JOptionPane.CANCEL_OPTION ? new Object[ 0 ] : selector.getSelectedValues();
	}

	public static final Object input( final String message, final Object[] inputs )
	{
		if ( inputs == null || inputs.length == 0 )
		{
			return null;
		}

		return KoLFrame.input( message, inputs, inputs[ 0 ] );
	}

	public static final Object input( final String message, final Object[] inputs, final Object initial )
	{
		if ( inputs == null || inputs.length == 0 )
		{
			return null;
		}

		return JOptionPane.showInputDialog(
			KoLFrame.activeWindow, KoLFrame.basicTextWrap( message ), "", JOptionPane.INFORMATION_MESSAGE, null,
			inputs, initial );
	}

	public static final String basicTextWrap( String text )
	{
		if ( text.length() < 80 || text.startsWith( "<html>" ) )
		{
			return text;
		}

		StringBuffer result = new StringBuffer();

		while ( text.length() > 0 )
		{
			if ( text.length() < 80 )
			{
				result.append( text );
				text = "";
			}
			else
			{
				int spaceIndex = text.lastIndexOf( " ", 80 );
				int breakIndex = text.lastIndexOf( "\n", spaceIndex );

				if ( breakIndex != -1 )
				{
					result.append( text.substring( 0, breakIndex ) );
					result.append( "\n" );
					text = text.substring( breakIndex ).trim();
				}
				else
				{
					result.append( text.substring( 0, spaceIndex ).trim() );
					result.append( "\n" );
					text = text.substring( spaceIndex ).trim();
				}
			}
		}

		return result.toString();
	}

	/**
	 * Utility method which retrieves an integer value from the given field. In the event that the field does not
	 * contain an integer value, the number "0" is returned instead.
	 */

	public static final int getValue( final JTextField field )
	{
		return KoLFrame.getValue( field, 0 );
	}

	public static final int getValue( final JSpinner field )
	{
		return KoLFrame.getValue( field, 0 );
	}

	/**
	 * Utility method which retrieves an integer value from the given field. In the event that the field does not
	 * contain an integer value, the default value provided will be returned instead.
	 */

	public static final int getValue( final JTextField field, final int defaultValue )
	{
		String currentValue = field.getText();

		if ( currentValue == null || currentValue.length() == 0 )
		{
			return defaultValue;
		}

		if ( currentValue.equals( "*" ) )
		{
			return defaultValue;
		}

		int result = StaticEntity.parseInt( currentValue );
		return result == 0 ? defaultValue : result;
	}

	public static final int getValue( final JSpinner field, final int defaultValue )
	{
		if ( !( field.getValue() instanceof Integer ) )
		{
			return defaultValue;
		}

		return ( (Integer) field.getValue() ).intValue();
	}

	public static final int getQuantity( final String message, final int maximumValue, int defaultValue )
	{
		// Check parameters; avoid programmer error.
		if ( defaultValue > maximumValue )
		{
			defaultValue = maximumValue;
		}

		if ( maximumValue == 1 && maximumValue == defaultValue )
		{
			return 1;
		}

		String currentValue = KoLFrame.input( message, KoLConstants.COMMA_FORMAT.format( defaultValue ) );
		if ( currentValue == null )
		{
			return 0;
		}

		if ( currentValue.equals( "*" ) )
		{
			return maximumValue;
		}

		int desiredValue = StaticEntity.parseInt( currentValue );
		return desiredValue < 0 ? maximumValue - desiredValue : Math.min( desiredValue, maximumValue );
	}

	public static final int getQuantity( final String title, final int maximumValue )
	{
		return KoLFrame.getQuantity( title, maximumValue, maximumValue );
	}

	public static class UnanimatedLabel
		extends JLabel
	{
		public UnanimatedLabel()
		{
			super( " ", null, SwingConstants.CENTER );
		}

		public boolean imageUpdate( final Image img, final int infoflags, final int x, final int y, final int width,
			final int height )
		{
			if ( infoflags == ImageObserver.FRAMEBITS )
			{
				return true;
			}

			super.imageUpdate( img, infoflags, x, y, width, height );
			return true;
		}
	}

	public void processWindowEvent( final WindowEvent e )
	{
		if ( this.isVisible() )
		{
			this.rememberPosition();
		}

		super.processWindowEvent( e );

		if ( e.getID() == WindowEvent.WINDOW_CLOSING )
		{
			if ( KoLConstants.existingFrames.contains( this ) )
			{
				KoLConstants.existingFrames.remove( this );
				KoLConstants.removedFrames.add( this );
				this.checkForLogout();
			}
		}
		else if ( e.getID() == WindowEvent.WINDOW_ACTIVATED )
		{
			if ( KoLConstants.removedFrames.contains( this ) )
			{
				KoLConstants.removedFrames.remove( this );
				KoLConstants.existingFrames.add( this );
			}

			KoLFrame.activeWindow = this;
		}
	}

	public void setVisible( final boolean isVisible )
	{
		if ( isVisible )
		{
			this.restorePosition();
		}
		else
		{
			this.rememberPosition();
		}

		super.setVisible( isVisible );

		if ( isVisible )
		{
			super.setExtendedState( Frame.NORMAL );
			super.repaint();
			KoLConstants.removedFrames.remove( this );
		}
	}

	private static final Pattern TOID_PATTERN = Pattern.compile( "toid=(\\d+)" );

	public class KoLHyperlinkAdapter
		extends HyperlinkAdapter
	{
		public void handleInternalLink( final String location )
		{
			if ( location.equals( "lchat.php" ) )
			{
				ChatManager.initialize();
			}
			else if ( location.startsWith( "makeoffer.php" ) || location.startsWith( "counteroffer.php" ) )
			{
				StaticEntity.getClient().openRelayBrowser( location );
			}
			else if ( location.startsWith( "sendmessage.php" ) || location.startsWith( "town_sendgift.php" ) )
			{
				// Attempts to send a message should open up
				// KoLmafia's built-in message sender.

				Matcher idMatcher = KoLFrame.TOID_PATTERN.matcher( location );

				String[] parameters = new String[] { idMatcher.find() ? idMatcher.group( 1 ) : "" };
				KoLFrame.createDisplay( SendMessageFrame.class, parameters );
			}
			else if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
			{
				DescriptionFrame.showLocation( location );
				return;
			}
			else if ( KoLFrame.this instanceof RequestFrame )
			{
				( (RequestFrame) KoLFrame.this ).refresh( RequestEditorKit.extractRequest( location ) );
			}
			else
			{
				StaticEntity.openRequestFrame( location );
			}
		}
	}

	public String getSettingString( final JCheckBox[] restoreCheckbox )
	{
		StringBuffer restoreSetting = new StringBuffer();

		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			if ( restoreCheckbox[ i ].isSelected() )
			{
				if ( restoreSetting.length() != 0 )
				{
					restoreSetting.append( ';' );
				}

				restoreSetting.append( restoreCheckbox[ i ].getText().toLowerCase() );
			}
		}

		return restoreSetting.toString();
	}

	public SimpleScrollPane constructScroller( final JCheckBox[] restoreCheckbox )
	{
		JPanel checkboxPanel = new JPanel( new GridLayout( restoreCheckbox.length, 1 ) );
		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			checkboxPanel.add( restoreCheckbox[ i ] );
		}

		return new SimpleScrollPane(
			checkboxPanel, SimpleScrollPane.VERTICAL_SCROLLBAR_NEVER, SimpleScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	}

	public void pack()
	{
		if ( !( this instanceof ChatFrame ) )
		{
			super.pack();
		}

		if ( !this.isVisible() )
		{
			this.restorePosition();
		}
	}

	private void rememberPosition()
	{
		Point p = this.getLocation();

		if ( this.tabs == null )
		{
			Preferences.setString( this.frameName, (int) p.getX() + "," + (int) p.getY() );
		}
		else
		{
			Preferences.setString(
				this.frameName, (int) p.getX() + "," + (int) p.getY() + "," + this.tabs.getSelectedIndex() );
		}
	}

	private void restorePosition()
	{
		int xLocation = 0;
		int yLocation = 0;

		Dimension screenSize = KoLConstants.TOOLKIT.getScreenSize();
		String position = Preferences.getString( this.frameName );

		if ( position == null || position.indexOf( "," ) == -1 )
		{
			this.setLocationRelativeTo( null );

			if ( !( this instanceof OptionsFrame ) && this.tabs != null && this.tabs.getTabCount() > 0 )
			{
				this.tabs.setSelectedIndex( 0 );
			}

			return;
		}

		String[] location = position.split( "," );
		xLocation = StaticEntity.parseInt( location[ 0 ] );
		yLocation = StaticEntity.parseInt( location[ 1 ] );

		if ( xLocation > 0 && yLocation > 0 && xLocation < screenSize.getWidth() && yLocation < screenSize.getHeight() )
		{
			this.setLocation( xLocation, yLocation );
		}
		else
		{
			this.setLocationRelativeTo( null );
		}

		if ( location.length > 2 && this.tabs != null )
		{
			int tabIndex = StaticEntity.parseInt( location[ 2 ] );

			if ( tabIndex >= 0 && tabIndex < this.tabs.getTabCount() )
			{
				this.tabs.setSelectedIndex( tabIndex );
			}
			else if ( this.tabs.getTabCount() > 0 )
			{
				this.tabs.setSelectedIndex( 0 );
			}
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel to update the panel's status. It also provides a
	 * thread which is guaranteed to be a daemon thread for updating the frame which also retrieves a reference to the
	 * StaticEntity.getClient()'s current settings.
	 */

	public abstract class OptionsPanel
		extends LabeledKoLPanel
	{
		public OptionsPanel()
		{
			this( new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
		}

		public OptionsPanel( final String panelTitle )
		{
			this( panelTitle, new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
		}

		public OptionsPanel( final Dimension left, final Dimension right )
		{
			this( null, left, right );
		}

		public OptionsPanel( final String panelTitle, final Dimension left, final Dimension right )
		{
			super( panelTitle, left, right );
		}

		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	public class LoadScriptButton
		extends ThreadedButton
	{
		private final String scriptPath;

		public LoadScriptButton( final int scriptId, final String scriptPath )
		{
			super( String.valueOf( scriptId ) );

			this.scriptPath = scriptPath;
			this.setToolTipText( scriptPath );

			JComponentUtilities.setComponentSize( this, 30, 30 );
		}

		public void run()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.scriptPath );
		}
	}

	/**
	 * Utility class used to forward events to JButtons enclosed inside of a JTable object.
	 */

	public class ButtonEventListener
		extends MouseAdapter
	{
		private final JTable table;

		public ButtonEventListener( final JTable table )
		{
			this.table = table;
		}

		public void mouseReleased( final MouseEvent e )
		{
			TableColumnModel columnModel = this.table.getColumnModel();

			int row = e.getY() / this.table.getRowHeight();
			int column = columnModel.getColumnIndexAtX( e.getX() );

			if ( row >= 0 && row < this.table.getRowCount() && column >= 0 && column < this.table.getColumnCount() )
			{
				Object value = this.table.getValueAt( row, column );

				if ( value instanceof JButton )
				{
					MouseEvent event = SwingUtilities.convertMouseEvent( this.table, e, (JButton) value );
					( (JButton) value ).dispatchEvent( event );
					this.table.repaint();
				}
			}
		}
	}

	public abstract class NestedInsideTableButton
		extends JButton
		implements MouseListener
	{
		public NestedInsideTableButton( final ImageIcon icon )
		{
			super( icon );
			this.addMouseListener( this );
		}

		public abstract void mouseReleased( MouseEvent e );

		public void mouseClicked( final MouseEvent e )
		{
		}

		public void mouseEntered( final MouseEvent e )
		{
		}

		public void mouseExited( final MouseEvent e )
		{
		}

		public void mousePressed( final MouseEvent e )
		{
		}
	}

	public abstract class ListWrapperTableModel
		extends DefaultTableModel
		implements ListDataListener
	{
		private final String[] headers;
		private final Class[] types;
		private final boolean[] editable;

		protected LockableListModel listModel;

		public ListWrapperTableModel( final String[] headers, final Class[] types, final boolean[] editable,
			final LockableListModel listModel )
		{
			super( 0, headers.length );

			this.headers = headers;
			this.types = types;
			this.editable = editable;

			for ( int i = 0; i < listModel.size(); ++i )
			{
				this.insertRow( i, this.constructVector( listModel.get( i ) ) );
			}

			this.listModel = listModel;
			listModel.addListDataListener( this );
		}

		public String getColumnName( final int index )
		{
			return index < 0 || index >= this.headers.length ? "" : this.headers[ index ];
		}

		public Class getColumnClass( final int column )
		{
			return column < 0 || column >= this.types.length ? Object.class : this.types[ column ];
		}

		public abstract Vector constructVector( Object o );

		public boolean isCellEditable( final int row, final int column )
		{
			return column < 0 || column >= this.editable.length ? false : this.editable[ column ];
		}

		/**
		 * Called whenever contents have been added to the original list; a function required by every
		 * <code>ListDataListener</code>.
		 *
		 * @param e the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalAdded( final ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();
			int index1 = e.getIndex1();

			for ( int i = index0; i <= index1; ++i )
			{
				this.insertRow( i, this.constructVector( source.get( i ) ) );
			}
		}

		/**
		 * Called whenever contents have been removed from the original list; a function required by every
		 * <code>ListDataListener</code>.
		 *
		 * @param e the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalRemoved( final ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();
			int index1 = e.getIndex1();

			for ( int i = index1; i >= index0; --i )
			{
				this.removeRow( i );
			}
		}

		/**
		 * Called whenever contents in the original list have changed; a function required by every
		 * <code>ListDataListener</code>.
		 *
		 * @param e the <code>ListDataEvent</code> that triggered this function call
		 */

		public void contentsChanged( final ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();
			int index1 = e.getIndex1();

			if ( index0 < 0 || index1 < 0 )
			{
				return;
			}

			int rowCount = this.getRowCount();

			for ( int i = index1; i >= index0; --i )
			{
				if ( source.size() < i )
				{
					this.removeRow( i );
				}
				else if ( i > rowCount )
				{
					this.insertRow( rowCount, this.constructVector( source.get( i ) ) );
				}
				else
				{
					this.removeRow( i );
					this.insertRow( i, this.constructVector( source.get( i ) ) );
				}
			}
		}
	}

	public class TransparentTable
		extends JTable
	{
		public TransparentTable( final TableModel t )
		{
			super( t );
		}

		public Component prepareRenderer( final TableCellRenderer renderer, final int row, final int column )
		{
			Component c = super.prepareRenderer( renderer, row, column );
			if ( c instanceof JComponent )
			{
				( (JComponent) c ).setOpaque( false );
			}

			return c;
		}

		public void changeSelection( final int row, final int column, final boolean toggle, final boolean extend )
		{
			super.changeSelection( row, column, toggle, extend );

			if ( this.editCellAt( row, column ) )
			{
				if ( this.getEditorComponent() instanceof JTextField )
				{
					( (JTextField) this.getEditorComponent() ).selectAll();
				}
			}
		}
	}

	public class IntegerRenderer
		extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent( final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			Component c = super.getTableCellRendererComponent( table, value, false, hasFocus, row, column );
			if ( !( value instanceof Integer ) )
			{
				return c;
			}

			( (JLabel) c ).setHorizontalAlignment( JLabel.RIGHT );
			( (JLabel) c ).setText( KoLConstants.COMMA_FORMAT.format( ( (Integer) value ).intValue() ) );
			return c;
		}
	}

	public class ButtonRenderer
		implements TableCellRenderer
	{
		public Component getTableCellRendererComponent( final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			JPanel panel = new JPanel();
			panel.setOpaque( false );

			JComponentUtilities.setComponentSize( (JButton) value, 20, 20 );
			panel.add( (JButton) value );

			return panel;
		}
	}

	public boolean finalizeTable( final JTable table )
	{
		if ( table.isEditing() )
		{
			int row = table.getEditingRow();
			int col = table.getEditingColumn();
			table.getCellEditor( row, col ).stopCellEditing();

			if ( table.isEditing() )
			{
				KoLFrame.alert( "One or more fields contain invalid values. (Note: they are currently outlined in red)" );
				return false;
			}
		}

		return true;
	}

	public static final void createDisplay( final Class frameClass )
	{
		KoLmafiaGUI.constructFrame( frameClass );
	}

	public static final void createDisplay( final Class frameClass, final Object[] parameters )
	{
		CreateFrameRunnable creator = new CreateFrameRunnable( frameClass, parameters );
		creator.run();
	}

	public static final void compileScripts()
	{
		KoLConstants.scripts.clear();

		// Get the list of files in the current directory
		if ( !KoLConstants.SCRIPT_LOCATION.exists() )
		{
			KoLConstants.SCRIPT_LOCATION.mkdirs();
		}

		File[] scriptList = KoLConstants.SCRIPT_LOCATION.listFiles( KoLConstants.BACKUP_FILTER );
		Arrays.sort( scriptList );

		// Iterate through the files.  Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		boolean hasNormalFiles = false;

		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( KoLMenuBar.shouldAddScript( scriptList[ i ] ) )
			{
				if ( scriptList[ i ].isDirectory() )
				{
					KoLConstants.scripts.add( scriptList[ i ] );
				}
				else
				{
					hasNormalFiles = true;
				}
			}
		}

		if ( hasNormalFiles )
		{
			for ( int i = 0; i < scriptList.length; ++i )
			{
				if ( !scriptList[ i ].isDirectory() )
				{
					KoLConstants.scripts.add( scriptList[ i ] );
				}
			}
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings file. This should be called after every
	 * update.
	 */

	public static final void saveBookmarks()
	{
		StringBuffer bookmarkData = new StringBuffer();

		for ( int i = 0; i < KoLConstants.bookmarks.getSize(); ++i )
		{
			if ( i > 0 )
			{
				bookmarkData.append( '|' );
			}
			bookmarkData.append( (String) KoLConstants.bookmarks.getElementAt( i ) );
		}

		Preferences.setString( "browserBookmarks", bookmarkData.toString() );
	}

	/**
	 * Utility method to compile the list of bookmarks based on the current settings.
	 */

	public static final void compileBookmarks()
	{
		KoLConstants.bookmarks.clear();
		String[] bookmarkData = Preferences.getString( "browserBookmarks" ).split( "\\|" );

		if ( bookmarkData.length > 1 )
		{
			for ( int i = 0; i < bookmarkData.length; ++i )
			{
				KoLConstants.bookmarks.add( bookmarkData[ i ] + "|" + bookmarkData[ ++i ] + "|" + bookmarkData[ ++i ] );
			}
		}
	}

	protected class LabelColorChanger
		extends JLabel
		implements MouseListener
	{
		protected String property;

		public LabelColorChanger( final String property )
		{
			this.property = property;
			this.setOpaque( true );
			this.addMouseListener( this );
		}

		public void mousePressed( final MouseEvent e )
		{
			Color c = JColorChooser.showDialog( null, "Choose a color:", this.getBackground() );
			if ( c == null )
			{
				return;
			}

			Preferences.setString( this.property, DataUtilities.toHexString( c ) );
			this.setBackground( c );
			this.applyChanges();
		}

		public void mouseReleased( final MouseEvent e )
		{
		}

		public void mouseClicked( final MouseEvent e )
		{
		}

		public void mouseEntered( final MouseEvent e )
		{
		}

		public void mouseExited( final MouseEvent e )
		{
		}

		public void applyChanges()
		{
		}
	}

	protected class StatusEffectPanel
		extends LabeledScrollPanel
	{
		private final ShowDescriptionList elementList;

		public StatusEffectPanel()
		{
			super( "Active Effects", "uneffect", "add to mood", new ShowDescriptionList( KoLConstants.activeEffects ) );
			this.elementList = (ShowDescriptionList) this.scrollComponent;
		}

		public void actionConfirmed()
		{
			Object[] effects = this.elementList.getSelectedValues();
			for ( int i = 0; i < effects.length; ++i )
			{
				RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[ i ] ) );
			}
		}

		public void actionCancelled()
		{
			Object[] effects = this.elementList.getSelectedValues();
			this.elementList.clearSelection();

			if ( Preferences.getString( "currentMood" ).equals( "apathetic" ) )
			{
				Preferences.setString( "currentMood", "default" );
			}

			String name, action;

			for ( int i = 0; i < effects.length; ++i )
			{
				name = ( (AdventureResult) effects[ i ] ).getName();

				action = MoodManager.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodManager.addTrigger( "lose_effect", name, action );
					continue;
				}

				action = MoodManager.getDefaultAction( "gain_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodManager.addTrigger( "gain_effect", name, action );
				}
			}
		}

	}

	/**
	 * An internal class which represents the panel used for storing and removing meat from the closet.
	 */

	protected class MeatTransferPanel
		extends LabeledKoLPanel
	{
		private final int transferType;
		private final AutoHighlightTextField amountField;
		private final JLabel closetField;

		public MeatTransferPanel( final int transferType )
		{
			super(
				transferType == ClosetRequest.MEAT_TO_CLOSET ? "Put Meat in Your Closet" : transferType == ClosetRequest.MEAT_TO_INVENTORY ? "Take Meat from Your Closet" : transferType == ClosetRequest.PULL_MEAT_FROM_STORAGE ? "Pull Meat from Hagnk's" : "Unknown Transfer Type",
				"transfer", "bedidall", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			this.amountField = new AutoHighlightTextField();
			this.closetField = new JLabel( " " );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Amount: ", this.amountField );
			elements[ 1 ] = new VerifiableElement( "Available: ", this.closetField );

			this.setContent( elements );

			this.transferType = transferType;
			this.refreshCurrentAmount();

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new AmountRefresher() ) );
		}

		private void refreshCurrentAmount()
		{
			switch ( this.transferType )
			{
			case ClosetRequest.MEAT_TO_CLOSET:
				this.closetField.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) + " meat" );
				break;

			case ClosetRequest.MEAT_TO_INVENTORY:
				this.closetField.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getClosetMeat() ) + " meat" );
				break;

			case ClosetRequest.PULL_MEAT_FROM_STORAGE:
				this.closetField.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getStorageMeat() ) + " meat" );
				break;

			default:
				this.closetField.setText( "Information not available" );
				break;
			}
		}

		public void actionConfirmed()
		{
			int amountToTransfer = KoLFrame.getValue( this.amountField );

			RequestThread.openRequestSequence();
			RequestThread.postRequest( new ClosetRequest( this.transferType, amountToTransfer ) );
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			KoLmafiaGUI.constructFrame( "MoneyMakingGameFrame" );
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		private class AmountRefresher
			implements Runnable
		{
			public void run()
			{
				MeatTransferPanel.this.refreshCurrentAmount();
			}
		}
	}

	protected class OverlapPanel
		extends ItemManagePanel
	{
		private final boolean isOverlap;
		private final LockableListModel overlapModel;

		public OverlapPanel( final String confirmText, final String cancelText, final LockableListModel overlapModel,
			final boolean isOverlap )
		{
			super( confirmText, cancelText, isOverlap ? overlapModel : KoLConstants.inventory, true, false );
			this.overlapModel = overlapModel;
			this.isOverlap = isOverlap;

			if ( this.isOverlap )
			{
				this.elementList.setCellRenderer( ListCellRendererFactory.getNameOnlyRenderer() );
			}

			this.elementList.addKeyListener( new OverlapAdapter() );
			this.addFilters();
		}

		public FilterTextField getWordFilter()
		{
			return new OverlapFilterField();
		}

		private class OverlapFilterField
			extends FilterItemField
		{
			public boolean isVisible( final Object element )
			{
				return super.isVisible( element ) && ( OverlapPanel.this.isOverlap ? KoLConstants.inventory.contains( element ) : !OverlapPanel.this.overlapModel.contains( element ) );
			}
		}

		private class OverlapAdapter
			extends KeyAdapter
		{
			public void keyReleased( final KeyEvent e )
			{
				if ( e.isConsumed() )
				{
					return;
				}

				if ( e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE )
				{
					return;
				}

				Object[] items = OverlapPanel.this.elementList.getSelectedValues();
				OverlapPanel.this.elementList.clearSelection();

				for ( int i = 0; i < items.length; ++i )
				{
					OverlapPanel.this.overlapModel.remove( items[ i ] );
					if ( OverlapPanel.this.overlapModel == KoLConstants.singletonList )
					{
						KoLConstants.junkList.remove( items[ i ] );
					}
				}

				OverlapPanel.this.filterItems();
				e.consume();
			}
		}
	}

	protected class RestorativeItemPanel
		extends ItemManagePanel
	{
		public RestorativeItemPanel()
		{
			super( "use item", "check wiki", KoLConstants.inventory );
			this.filterItems();
		}

		public FilterTextField getWordFilter()
		{
			return new RestorativeItemFilterField();
		}

		public void actionConfirmed()
		{
			Object[] items = this.getDesiredItems( "Consume" );
			if ( items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new UseItemRequest( (AdventureResult) items[ i ] ) );
			}
		}

		public void actionCancelled()
		{
			String name;
			Object[] values = this.elementList.getSelectedValues();

			for ( int i = 0; i < values.length; ++i )
			{
				name = ( (AdventureResult) values[ i ] ).getName();
				if ( name != null )
				{
					StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
				}
			}
		}

		private class RestorativeItemFilterField
			extends FilterItemField
		{
			public boolean isVisible( final Object element )
			{
				AdventureResult item = (AdventureResult) element;
				int itemId = item.getItemId();

				switch ( ItemDatabase.getConsumptionType( itemId ) )
				{
				case KoLConstants.MP_RESTORE:
				case KoLConstants.HP_RESTORE:
				case KoLConstants.HPMP_RESTORE:
					return super.isVisible( element );

				default:
					return false;
				}
			}
		}
	}
}
