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
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
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
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import tab.CloseTabPaneEnhancedUI;
import tab.CloseTabbedPane;
import tab.CloseTabPaneUI;

import com.informit.guides.JDnDList;
import com.sun.java.forums.CloseableTabbedPane;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public abstract class KoLFrame extends JFrame implements KoLConstants
{
	public static final TradeableItemFilter TRADE_FILTER = new TradeableItemFilter();

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
		compileScripts();
		compileBookmarks();
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given StaticEntity.getClient().
	 */

	public KoLFrame()
	{
		this( "" );
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given StaticEntity.getClient().
	 */

	public KoLFrame( String title )
	{
		this.setTitle( title );
		this.setDefaultCloseOperation( HIDE_ON_CLOSE );

		this.tabs = getTabbedPane();
		this.framePanel = new JPanel( new BorderLayout( 0, 0 ) );

		this.frameName = this.getClass().getName();
		this.frameName = this.frameName.substring( this.frameName.lastIndexOf( "." ) + 1 );

		if ( this.shouldAddStatusBar() )
		{
			JEditorPane statusDisplay = new JEditorPane();

			JScrollPane statusBar = commandBuffer.setChatDisplay( statusDisplay );
			JComponentUtilities.setComponentSize( statusBar, new Dimension( 200, 50 ) );

			JSplitPane doublePane = new JSplitPane( JSplitPane.VERTICAL_SPLIT,
				new SimpleScrollPane( this.framePanel, SimpleScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, SimpleScrollPane.HORIZONTAL_SCROLLBAR_NEVER ),
				statusBar );

			this.getContentPane().add( doublePane, BorderLayout.CENTER );

			doublePane.setOneTouchExpandable( true );
			doublePane.setDividerLocation( 0.9 );
		}
		else
		{
			this.getContentPane().add( this.framePanel, BorderLayout.CENTER );
		}

		this.addHotKeys();

		boolean shouldAddFrame = !(this instanceof KoLDesktop) && !(this instanceof ContactListFrame) && !(this instanceof LoginFrame);

		if ( this instanceof ChatFrame )
			shouldAddFrame = !KoLMessenger.usingTabbedChat() || this instanceof TabbedChatFrame;

		if ( shouldAddFrame )
			StaticEntity.registerFrame( this );
	}

	public void setJMenuBar( KoLMenuBar menuBar )
	{
		this.menuBar = menuBar;
		super.setJMenuBar( menuBar );
	}

	protected void addActionListener( JCheckBox component, ActionListener listener )
	{
		if ( listenerMap == null )
			listenerMap = new HashMap();

		component.addActionListener( listener );
		listenerMap.put( component, new WeakReference( listener ) );
	}

	protected void addActionListener( JComboBox component, ActionListener listener )
	{
		if ( this.listenerMap == null )
			this.listenerMap = new HashMap();

		component.addActionListener( listener );
		listenerMap.put( component, new WeakReference( listener ) );
	}

	private void removeActionListener( Object component, ActionListener listener )
	{
		if ( component instanceof JCheckBox )
			((JCheckBox)component).removeActionListener( listener );
		if ( component instanceof JComboBox )
			((JComboBox)component).removeActionListener( listener );
	}

	protected void removeThreadedListeners()
	{
		if ( this.listenerMap == null )
			return;

		Object [] keys = this.listenerMap.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			WeakReference ref = (WeakReference) listenerMap.get( keys[i] );
			if ( ref == null )
				continue;

			Object listener = ref.get();
			if ( listener == null )
				continue;

			if ( listener instanceof ActionListener )
				removeActionListener( keys[i], (ActionListener) listener );
		}

		listenerMap.clear();
		listenerMap = null;
	}

	public boolean shouldAddStatusBar()
	{
		return StaticEntity.getBooleanProperty( "addStatusBarToFrames" ) &&
			StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( this.frameName ) == -1;
	}

	public JTabbedPane getTabbedPane()
	{	return StaticEntity.getBooleanProperty( "useDecoratedTabs" ) ? new CloseTabbedPane() : new JTabbedPane();
	}

	public void addHotKeys()
	{
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_ESCAPE, new WorldPeaceListener() );
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_F5, new RefreshKeyListener() );

		if ( !System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_F6, InputEvent.CTRL_MASK, new TabForwardListener() );
			JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_F6, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK, new TabBackwardListener() );
		}
	}

	private class ExitListener extends ThreadedListener
	{
		public void run()
		{	System.exit(0);
		}
	}

	private class WorldPeaceListener extends ThreadedListener
	{
		public void run()
		{	RequestThread.declareWorldPeace();
		}
	}

	private class RefreshKeyListener extends ThreadedListener
	{
		public void run()
		{
			RequestThread.openRequestSequence();
			StaticEntity.getClient().refreshSession();
			RequestThread.closeRequestSequence();
		}
	}

	private class TabForwardListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			if ( KoLFrame.this.tabs == null )
				return;

			KoLFrame.this.tabs.setSelectedIndex( (KoLFrame.this.tabs.getSelectedIndex() + 1) % KoLFrame.this.tabs.getTabCount() );
		}
	}

	private class TabBackwardListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			if ( KoLFrame.this.tabs == null )
				return;

			KoLFrame.this.tabs.setSelectedIndex( KoLFrame.this.tabs.getSelectedIndex() == 0 ?
				KoLFrame.this.tabs.getTabCount() - 1 : KoLFrame.this.tabs.getSelectedIndex() - 1 );
		}
	}

	public final void addTab( String name, JComponent panel )
	{
		if ( this.tabs == null )
			return;

		this.tabs.setOpaque( true );

		SimpleScrollPane scroller = new SimpleScrollPane( panel );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		this.tabs.add( name, scroller );
	}

	public final void setTitle( String newTitle )
	{
		this.lastTitle = newTitle;
		KoLDesktop.setTitle( this, newTitle );

		super.setTitle( !(this instanceof LoginFrame || this instanceof ContactListFrame) && KoLCharacter.getUserName().length() > 0 ?
			KoLCharacter.getUserName() + ": " + this.lastTitle : this.lastTitle );
	}

	public void requestFocus()
	{
		super.requestFocus();
		KoLDesktop.requestFocus( this );
	}

	public boolean useSidePane()
	{	return false;
	}

	public void constructToolbar()
	{
	}

	public JToolBar getToolbar()
	{
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

		return toolbarPanel;
	}

	/**
	 * Overrides the default behavior of dispose so that the frames
	 * are removed from the internal list of existing frames.  Also
	 * allows for automatic exit.
	 */

	public void dispose()
	{
		if ( this.isVisible() )
			this.rememberPosition();

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		KoLDesktop.removeTab( this );
		StaticEntity.unregisterFrame( this );
		existingFrames.remove( this );

		if ( this.refreshListener != null )
			KoLCharacter.removeCharacterListener( this.refreshListener );

		removeThreadedListeners();
		this.getRootPane().resetKeyboardActions();

		if ( this.menuBar != null )
			this.menuBar.dispose();

		super.dispose();

		checkForLogout();
	}

	private void checkForLogout()
	{
		if ( !existingFrames.isEmpty() || LoginFrame.instanceExists() )
			return;

		createDisplay( LoginFrame.class );
		RequestThread.postRequest( new LogoutRequest() );
	}

	public String toString()
	{	return this.lastTitle;
	}

	public String getFrameName()
	{	return this.frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component.
	 * Note that this method can only be used if the KoLFrame on which it
	 * is called has not yet added any components.  If there are any added
	 * components, this method will do nothing.
	 */

	public void addCompactPane()
	{
		if ( this.refresher != null )
			return;

		this.refresher = new StatusRefresher();
		this.refresher.run();

		this.refreshListener = new KoLCharacterAdapter( this.refresher );
		KoLCharacter.addCharacterListener( this.refreshListener );

		this.refresher.getCompactPane().setBackground( ENABLED_COLOR );
		this.getContentPane().add( this.refresher.getCompactPane(), BorderLayout.WEST );
	}

	public static class StatusRefresher implements Runnable
	{
		private JPanel compactPane;
		private JPanel levelPanel;
		private JProgressBar levelMeter;
		private JLabel levelLabel, roninLabel, mcdLabel;
		private JLabel musLabel, mysLabel, moxLabel;
		private JLabel fullLabel, drunkLabel, spleenLabel;
		private JLabel hpLabel, mpLabel, meatLabel, advLabel;
		private JLabel familiarLabel;
		private JLabel mlLabel, encLabel, initLabel;
		private JLabel meatDropLabel, itemDropLabel;

		public StatusRefresher()
		{
			JPanel labelPanel, valuePanel;

			JPanel [] panels = new JPanel[6];
			int panelCount = -1;

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			this.levelPanel = panels[0];

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

				valuePanel.add( this.fullLabel = new JLabel( " ", JLabel.LEFT) );
				valuePanel.add( this.drunkLabel = new JLabel( " ", JLabel.LEFT) );
				valuePanel.add( this.spleenLabel = new JLabel( " ", JLabel.LEFT) );

			panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
			panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 1, 1 ) );
			panels[ panelCount ].add( this.familiarLabel = new UnanimatedLabel() );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 5, 2 ) );
			panels[ panelCount ].add( new JLabel( "ML: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.mlLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Enc: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.encLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Init: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.initLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Meat: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.meatDropLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Item: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.itemDropLabel = new JLabel( " ", JLabel.LEFT ) );

			JPanel compactContainer = new JPanel();
			compactContainer.setOpaque( false );
			compactContainer.setLayout( new BoxLayout( compactContainer, BoxLayout.Y_AXIS ) );

			for ( int i = 0; i < panels.length; ++i )
			{
				panels[i].setOpaque( false );
				compactContainer.add( panels[i] );
				compactContainer.add( Box.createVerticalStrut( 20 ) );
			}

			JPanel compactCard = new JPanel( new CardLayout( 8, 8 ) );
			compactCard.setOpaque( false );
			compactCard.add( compactContainer, "" );

			JPanel refreshPanel = new JPanel();
			refreshPanel.setOpaque( false );
			refreshPanel.add( new RequestButton( "Refresh Status", "refresh.gif", new CharsheetRequest() ) );

			this.compactPane = new JPanel( new BorderLayout() );
			this.compactPane.add( compactCard, BorderLayout.NORTH );
			this.compactPane.add( refreshPanel, BorderLayout.SOUTH );
		}

		public String getStatText( int adjusted, int base )
		{
			return adjusted == base ? "<html>" + Integer.toString( base ) :
				adjusted >  base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" :
				"<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
		}

		public void run()
		{
			this.levelLabel.setText( "Level " + KoLCharacter.getLevel() );

			if ( KoLCharacter.isHardcore() )
				this.roninLabel.setText( "(Hardcore)" );
			else if ( KoLCharacter.getAscensions() == 0 )
				this.roninLabel.setText( "(Unascended)" );
			else if ( KoLCharacter.getCurrentRun() >= 600 )
				this.roninLabel.setText( "(Ronin Clear)" );
			else
				this.roninLabel.setText( "(Ronin for " + (600 - KoLCharacter.getCurrentRun()) + ")" );

			this.mcdLabel.setText( "ML @ " + KoLCharacter.getSignedMLAdjustment() );

			this.musLabel.setText( this.getStatText( KoLCharacter.getAdjustedMuscle(), KoLCharacter.getBaseMuscle() ) );
			this.mysLabel.setText( this.getStatText( KoLCharacter.getAdjustedMysticality(), KoLCharacter.getBaseMysticality() ) );
			this.moxLabel.setText( this.getStatText( KoLCharacter.getAdjustedMoxie(), KoLCharacter.getBaseMoxie() ) );

			this.fullLabel.setText( KoLCharacter.getFullness() + " / " + KoLCharacter.getFullnessLimit() );
			this.drunkLabel.setText( KoLCharacter.getInebriety() + " / " + KoLCharacter.getInebrietyLimit() );
			this.spleenLabel.setText( KoLCharacter.getSpleenUse() + " / " + KoLCharacter.getSpleenLimit() );

			this.hpLabel.setText( COMMA_FORMAT.format( KoLCharacter.getCurrentHP() ) + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			this.mpLabel.setText( COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );
			this.meatLabel.setText( COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			this.advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );

			int ml = KoLCharacter.getMonsterLevelAdjustment();
			this.mlLabel.setText( MODIFIER_FORMAT.format( ml ) );
			this.encLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
			this.initLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );
			this.meatDropLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			this.itemDropLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );

			int currentLevel = KoLCharacter.calculateLastLevel();
			int nextLevel = KoLCharacter.calculateNextLevel();
			int totalPrime = KoLCharacter.getTotalPrime();

			this.levelMeter.setMaximum( nextLevel - currentLevel );
			this.levelMeter.setValue( totalPrime - currentLevel );
			this.levelMeter.setString( " " );

			this.levelPanel.setToolTipText( "<html>&nbsp;&nbsp;" + KoLCharacter.getAdvancement() + "&nbsp;&nbsp;<br>&nbsp;&nbsp;(" +
				COMMA_FORMAT.format( nextLevel - totalPrime ) + " subpoints needed)&nbsp;&nbsp;</html>" );

			FamiliarData familiar = KoLCharacter.getFamiliar();
			int id = familiar == null ? -1 : familiar.getId();

			if ( id == -1 )
			{
				this.familiarLabel.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
				this.familiarLabel.setText( "0 lbs." );
				this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
				this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
			}
			else
			{
				ImageIcon familiarIcon = FamiliarsDatabase.getFamiliarImage( id );
				this.familiarLabel.setIcon( familiarIcon );
				this.familiarLabel.setText( familiar.getModifiedWeight() + (familiar.getModifiedWeight() == 1 ? " lb." : " lbs.") );
				this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
				this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );

				this.familiarLabel.updateUI();
			}
		}

		public JPanel getCompactPane()
		{	return this.compactPane;
		}
	}

	public void setStatusMessage( String message )
	{
	}

	public void updateDisplayState( int displayState )
	{
		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		switch ( displayState )
		{
		case ABORT_STATE:
		case ERROR_STATE:

			if ( this.refresher != null )
				this.refresher.getCompactPane().setBackground( ERROR_COLOR );

			this.setEnabled( true );
			break;

		case ENABLE_STATE:

			if ( this.refresher != null )
				this.refresher.getCompactPane().setBackground( ENABLED_COLOR );

			this.setEnabled( true );
			break;

		default:

			if ( this.refresher != null )
				this.refresher.getCompactPane().setBackground( DISABLED_COLOR );

			this.setEnabled( false );
			break;
		}
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled()
	 * method does not call the superclass's version.
	 *
	 * @return	<code>true</code>
	 */

	public final boolean isEnabled()
	{	return true;
	}

	public void setEnabled( boolean isEnabled )
	{
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	public static class DisplayFrameButton extends ThreadedButton
	{
		private String frameClass;

		public DisplayFrameButton( String text, String frameClass )
		{
			super( text );
			this.frameClass = frameClass;
		}

		public DisplayFrameButton( String tooltip, String icon, String frameClass )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			this.setToolTipText( tooltip );

			this.frameClass = frameClass;
		}

		public void run()
		{	KoLmafiaGUI.constructFrame( this.frameClass );
		}
	}

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	public static class InvocationButton extends ThreadedButton
	{
		public Object object;
		public Method method;

		public InvocationButton( String text, Object object, String methodName )
		{
			this( text, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( String text, Class c, String methodName )
		{
			super( text );
			this.object = c;

			this.completeConstruction( c, methodName );
		}

		public InvocationButton( String tooltip, String icon, Object object, String methodName )
		{
			this( tooltip, icon, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( String tooltip, String icon, Class c, String methodName )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );

			this.object = c;
			this.setToolTipText( tooltip );
			this.completeConstruction( c, methodName );
		}

		public void completeConstruction( Class c, String methodName )
		{
			try
			{
				this.method = c.getMethod( methodName, NOPARAMS );
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
					this.method.invoke( this.object, null );
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
	 * An internal class used to handle requests to open a new frame
	 * using a local panel inside of the adventure frame.
	 */

	public static class KoLPanelFrameButton extends ThreadedButton
	{
		public Object [] parameters;

		public KoLPanelFrameButton( String tooltip, String icon, ActionPanel panel )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			this.setToolTipText( tooltip );

			this.parameters = new Object[2];
			this.parameters[0] = tooltip;
			this.parameters[1] = panel;
		}

		public void run()
		{	createDisplay( KoLPanelFrame.class, this.parameters );
		}
	}

	public static class RequestButton extends ThreadedButton
	{
		public KoLRequest request;

		public RequestButton( String title, KoLRequest request )
		{
			super( title );
			this.request = request;
		}

		public RequestButton( String title, String icon, KoLRequest request )
		{
			super( JComponentUtilities.getImage( icon ) );
			this.setToolTipText( title );
			this.request = request;
		}

		public void run()
		{	RequestThread.postRequest( this.request );
		}
	}

	/**
	 * Utility method which retrieves an integer value from the given
	 * field.  In the event that the field does not contain an integer
	 * value, the number "0" is returned instead.
	 */

	public static final int getValue( JTextField field )
	{	return getValue( field, 0 );
	}

	/**
	 * Utility method which retrieves an integer value from the given
	 * field.  In the event that the field does not contain an integer
	 * value, the default value provided will be returned instead.
	 */

	public static final int getValue( JTextField field, int defaultValue )
	{
		String currentValue = field.getText();

		if ( currentValue == null || currentValue.length() == 0 )
			return defaultValue;

		if ( currentValue.equals( "*" ) )
			return defaultValue;

		int result = StaticEntity.parseInt( currentValue );
		return result == 0 ? defaultValue : result;
	}

	public static final int getValue( JSpinner field, int defaultValue )
	{
		if ( !(field.getValue() instanceof Integer) )
			return defaultValue;

		return ((Integer) field.getValue()).intValue();
	}

	public static final int getQuantity( String title, int maximumValue, int defaultValue )
	{
		// Check parameters; avoid programmer error.
		if ( defaultValue > maximumValue )
			defaultValue = maximumValue;

		if ( maximumValue == 1 && maximumValue == defaultValue )
			return 1;

		String currentValue = JOptionPane.showInputDialog( title, COMMA_FORMAT.format( defaultValue ) );
		if ( currentValue == null )
			return 0;

		if ( currentValue.equals( "*" ) )
			return maximumValue;

		int desiredValue = StaticEntity.parseInt( currentValue );
		return desiredValue < 0 ? maximumValue - desiredValue : Math.min( desiredValue, maximumValue );
	}

	public static final int getQuantity( String title, int maximumValue )
	{	return getQuantity( title, maximumValue, maximumValue );
	}

	public static class UnanimatedLabel extends JLabel
	{
		public UnanimatedLabel()
		{	super( " ", null, CENTER );
		}

		public boolean imageUpdate( Image img, int infoflags, int x, int y, int width, int height )
		{
			if ( infoflags == FRAMEBITS )
				return true;

			super.imageUpdate( img, infoflags, x, y, width, height );
			return true;
		}
	}

	public void processWindowEvent( WindowEvent e )
	{
		if ( this.isVisible() )
			this.rememberPosition();

		if ( e.getID() == WindowEvent.WINDOW_CLOSING && existingFrames.contains( this ) )
		{
			existingFrames.remove( this );
			removedFrames.add( this );
			checkForLogout();
		}
		else if ( e.getID() == WindowEvent.WINDOW_ACTIVATED && removedFrames.contains( this ) )
		{
			existingFrames.add( this );
			removedFrames.remove( this );
		}

		super.processWindowEvent( e );
	}

	public void setVisible( boolean isVisible )
	{
		if ( isVisible )
			this.restorePosition();
		else
			this.rememberPosition();

		super.setVisible( isVisible );

		if ( isVisible )
		{
			super.setExtendedState( NORMAL );
			super.repaint();
			removedFrames.remove( this );
		}
	}

	private static final Pattern TOID_PATTERN = Pattern.compile( "toid=(\\d+)" );

	public class KoLHyperlinkAdapter extends HyperlinkAdapter
	{
		public void handleInternalLink( String location )
		{
			if ( location.equals( "lchat.php" ) )
			{
				// Chat should load up KoLmafia's internal chat
				// engine -- the mini-browser has no hope.

				KoLMessenger.initialize();
			}
			else if ( location.startsWith( "makeoffer.php" ) || location.startsWith( "counteroffer.php" ) )
			{
				StaticEntity.getClient().openRelayBrowser( location );
			}
			else if ( location.startsWith( "sendmessage.php" ) || location.startsWith( "town_sendgift.php" ) )
			{
				// Attempts to send a message should open up
				// KoLmafia's built-in message sender.

				Matcher idMatcher = TOID_PATTERN.matcher( location );

				String [] parameters = new String [] { idMatcher.find() ? idMatcher.group(1) : "" };
				createDisplay( SendMessageFrame.class, parameters );
			}
			else if ( KoLFrame.this instanceof RequestFrame )
			{
				// If this is a request frame, make sure that
				// you minimize the number of open windows by
				// making an attempt to refresh.

				((RequestFrame)KoLFrame.this).refresh( RequestEditorKit.extractRequest( location ) );
			}
			else
			{
				// Otherwise, if this isn't a request frame,
				// open up a new request frame in order to
				// display the appropriate data.

				StaticEntity.openRequestFrame( location );
			}
		}
	}

	public String getSettingString( JCheckBox [] restoreCheckbox )
	{
		StringBuffer restoreSetting = new StringBuffer();

		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			if ( restoreCheckbox[i].isSelected() )
			{
				if ( restoreSetting.length() != 0 )
					restoreSetting.append( ';' );

				restoreSetting.append( restoreCheckbox[i].getText().toLowerCase() );
			}
		}

		return restoreSetting.toString();
	}

	public SimpleScrollPane constructScroller( JCheckBox [] restoreCheckbox )
	{
		JPanel checkboxPanel = new JPanel( new GridLayout( restoreCheckbox.length, 1 ) );
		for ( int i = 0; i < restoreCheckbox.length; ++i )
			checkboxPanel.add( restoreCheckbox[i] );

		return new SimpleScrollPane( checkboxPanel,
			SimpleScrollPane.VERTICAL_SCROLLBAR_NEVER, SimpleScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	}

	public void pack()
	{
		if ( !(this instanceof ChatFrame) )
			super.pack();

		if ( !this.isVisible() )
			this.restorePosition();
	}

	private void rememberPosition()
	{
		Point p = this.getLocation();

		if ( this.tabs == null )
		{
			StaticEntity.setProperty( this.frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
		}
		else
		{
			StaticEntity.setProperty( this.frameName, ((int)p.getX()) + "," + ((int)p.getY()) + "," +
				tabs.getSelectedIndex() );
		}
	}

	private void restorePosition()
	{
		int xLocation = 0;
		int yLocation = 0;

		Dimension screenSize = TOOLKIT.getScreenSize();
		String position = StaticEntity.getProperty( this.frameName );

		if ( position == null || position.indexOf( "," ) == -1 )
		{
			this.setLocationRelativeTo( null );
			return;
		}

		String [] location = position.split( "," );
		xLocation = StaticEntity.parseInt( location[0] );
		yLocation = StaticEntity.parseInt( location[1] );

		if ( xLocation > 0 && yLocation > 0 && xLocation < screenSize.getWidth() && yLocation < screenSize.getHeight() )
			this.setLocation( xLocation, yLocation );
		else
			this.setLocationRelativeTo( null );

		if ( location.length > 2 && this.tabs != null )
		{
			int tabIndex = StaticEntity.parseInt( location[2] );

			if ( tabIndex >= 0 && tabIndex < this.tabs.getTabCount() )
				this.tabs.setSelectedIndex( tabIndex );
			else if ( this.tabs.getTabCount() > 0 )
				this.tabs.setSelectedIndex( 0 );
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the StaticEntity.getClient()'s current settings.
	 */

	public abstract class OptionsPanel extends LabeledKoLPanel
	{
		public OptionsPanel()
		{	this( new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
		}

		public OptionsPanel( String panelTitle )
		{	this( panelTitle, new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
		}

		public OptionsPanel( Dimension left, Dimension right )
		{	this( null, left, right );
		}

		public OptionsPanel( String panelTitle, Dimension left, Dimension right )
		{	super( panelTitle, left, right );
		}

		public void actionConfirmed()
		{
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public class LoadScriptButton extends ThreadedButton
	{
		private String scriptPath;

		public LoadScriptButton( int scriptId, String scriptPath )
		{
			super( String.valueOf( scriptId ) );

			this.scriptPath = scriptPath;
			this.setToolTipText( scriptPath );

			JComponentUtilities.setComponentSize( this, 30, 30 );
		}

		public void run()
		{	DEFAULT_SHELL.executeLine( this.scriptPath );
		}
	}

	/**
	 * Utility class used to forward events to JButtons enclosed inside
	 * of a JTable object.
	 */

	public class ButtonEventListener extends MouseAdapter
	{
		private JTable table;

		public ButtonEventListener( JTable table )
		{	this.table = table;
		}

		public void mouseReleased( MouseEvent e )
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
					((JButton)value).dispatchEvent( event );
					this.table.repaint();
				}
			}
		}
	}

	public abstract class NestedInsideTableButton extends JButton implements MouseListener
	{
		public NestedInsideTableButton( ImageIcon icon )
		{
			super( icon );
			this.addMouseListener( this );
		}

		public abstract void mouseReleased( MouseEvent e );

		public void mouseClicked( MouseEvent e )
		{
		}

		public void mouseEntered( MouseEvent e )
		{
		}

		public void mouseExited( MouseEvent e )
		{
		}

		public void mousePressed( MouseEvent e )
		{
		}
	}

	public abstract class ListWrapperTableModel extends DefaultTableModel implements ListDataListener
	{
		private String [] headers;
		private Class [] types;
		private boolean [] editable;

		protected LockableListModel listModel;

		public ListWrapperTableModel( String [] headers, Class [] types, boolean [] editable, LockableListModel listModel )
		{
			super( 0, headers.length );

			this.headers = headers;
			this.types = types;
			this.editable = editable;

			for ( int i = 0; i < listModel.size(); ++i )
				this.insertRow( i, this.constructVector( listModel.get(i) ) );

			this.listModel = listModel;
			listModel.addListDataListener( this );
		}

		public String getColumnName( int index )
		{	return index < 0 || index >= this.headers.length ? "" : this.headers[ index ];
		}

		public Class getColumnClass( int column )
		{	return column < 0 || column >= this.types.length ? Object.class : this.types[ column ];
		}

		public abstract Vector constructVector( Object o );

		public boolean isCellEditable( int row, int column )
		{	return column < 0 || column >= this.editable.length ? false : this.editable[ column ];
		}

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalAdded( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			for ( int i = index0; i <= index1; ++i )
				this.insertRow( i, this.constructVector( source.get(i) ) );
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalRemoved( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			for ( int i = index1; i >= index0; --i )
				this.removeRow(i);
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void contentsChanged( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index0 < 0 || index1 < 0 )
				return;

			int rowCount = this.getRowCount();

			for ( int i = index1; i >= index0; --i )
			{
				if ( source.size() < i )
				{
					this.removeRow(i);
				}
				else if ( i > rowCount )
				{
					this.insertRow( rowCount, this.constructVector( source.get(i) ) );
				}
				else
				{
					this.removeRow(i);
					this.insertRow( i, this.constructVector( source.get(i) ) );
				}
			}
		}
	}

	public class TransparentTable extends JTable
	{
		public TransparentTable( TableModel t )
		{	super( t );
		}

		public Component prepareRenderer( TableCellRenderer renderer, int row, int column )
		{
			Component c = super.prepareRenderer( renderer, row, column );
			if ( c instanceof JComponent )
				((JComponent)c).setOpaque( false );

			return c;
		}

		public void changeSelection( final int row, final int column, boolean toggle, boolean extend )
		{
			super.changeSelection( row, column, toggle, extend );

			if ( this.editCellAt( row, column ) )
			{
				this.getEditorComponent().requestFocusInWindow();
				if ( this.getEditorComponent() instanceof JTextField )
					((JTextField)this.getEditorComponent()).selectAll();
			}
		}
	}

	public class IntegerRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			Component c = super.getTableCellRendererComponent( table, value, false, hasFocus, row, column );
			if ( !(value instanceof Integer) )
				return c;

			((JLabel)c).setHorizontalAlignment( JLabel.RIGHT );
			((JLabel)c).setText( COMMA_FORMAT.format( ((Integer)value).intValue() ) );
			return c;
		}
	}

	public class ButtonRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			JPanel panel = new JPanel();
			panel.setOpaque( false );

			JComponentUtilities.setComponentSize( (JButton) value, 20, 20 );
			panel.add( (JButton) value );

			return panel;
		}
	}

	public boolean finalizeTable( JTable table )
	{
		if ( table.isEditing() )
		{
			int row = table.getEditingRow();
			int col = table.getEditingColumn();
			table.getCellEditor( row, col ).stopCellEditing();

			if ( table.isEditing() )
			{
				JOptionPane.showMessageDialog( null, "One or more fields contain invalid values.\n(Note: they are currently outlined in red)" );
				return false;
			}
		}

		return true;
	}

	public static void createDisplay( Class frameClass )
	{	createDisplay( frameClass, NOPARAMS );
	}

	public static void createDisplay( Class frameClass, Object [] parameters )
	{	SwingUtilities.invokeLater( new CreateFrameRunnable( frameClass, parameters ) );
	}

	public static void compileScripts()
	{
		scripts.clear();

		// Get the list of files in the current directory
		if ( !SCRIPT_LOCATION.exists() )
			SCRIPT_LOCATION.mkdirs();

		File [] scriptList = SCRIPT_LOCATION.listFiles( BACKUP_FILTER );

		// Iterate through the files.  Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		boolean hasDirectories = false;
		boolean hasNormalFiles = false;

		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( KoLMenuBar.shouldAddScript( scriptList[i] ) )
			{
				if ( scriptList[i].isDirectory() )
				{
					scripts.add( scriptList[i] );
					hasDirectories = true;
				}
				else
					hasNormalFiles = true;
			}
		}

		if ( hasNormalFiles )
		{
			for ( int i = 0; i < scriptList.length; ++i )
				if ( !scriptList[i].isDirectory() )
					scripts.add( scriptList[i] );
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings
	 * file.  This should be called after every update.
	 */

	public static void saveBookmarks()
	{
		StringBuffer bookmarkData = new StringBuffer();

		for ( int i = 0; i < bookmarks.size(); ++i )
		{
			if ( i > 0 )
				bookmarkData.append( '|' );
			bookmarkData.append( (String) bookmarks.get(i) );
		}

		StaticEntity.setProperty( "browserBookmarks", bookmarkData.toString() );
	}

	/**
	 * Utility method to compile the list of bookmarks based on the
	 * current settings.
	 */

	public static void compileBookmarks()
	{
		bookmarks.clear();
		String [] bookmarkData = StaticEntity.getProperty( "browserBookmarks" ).split( "\\|" );

		if ( bookmarkData.length > 1 )
			for ( int i = 0; i < bookmarkData.length; ++i )
				bookmarks.add( bookmarkData[i] + "|" + bookmarkData[++i] + "|" + bookmarkData[++i] );
	}

	private static class TradeableItemFilter extends ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			if ( !(element instanceof AdventureResult) )
				return true;

			int itemId = ((AdventureResult)element).getItemId();
			return itemId < 1 || TradeableItemDatabase.isTradeable( itemId );
		}
	}

	public class InventoryManagePanel extends ItemManagePanel
	{
		public InventoryManagePanel( LockableListModel elementModel )
		{	this( elementModel, true );
		}

		public InventoryManagePanel( LockableListModel elementModel, boolean addFilters )
		{
			super( elementModel );

			boolean isCloset = (elementModel == closet);

			this.setButtons( addFilters, new ActionListener [] {

				new ConsumeListener(),
				new PutInClosetListener( isCloset ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOSELL ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOMALL ),
				new PulverizeListener( isCloset ),
				new PutOnDisplayListener( isCloset ),
				new GiveToClanListener( isCloset )

			} );

			this.movers[ KoLCharacter.canInteract() ? 0 : 2 ].setSelected( true );
			this.filterItems();
		}
	}

	protected class LabelColorChanger extends JLabel implements MouseListener
	{
		protected String property;

		public LabelColorChanger( String property )
		{
			this.property = property;
			this.setOpaque( true );
			this.addMouseListener( this );
		}

		public void mousePressed( MouseEvent e )
		{
			Color c = JColorChooser.showDialog( null, "Choose a color:", this.getBackground() );
			if ( c == null )
				return;

			StaticEntity.setProperty( this.property, DataUtilities.toHexString( c ) );
			this.setBackground( c );
			this.applyChanges();
		}

		public void mouseReleased( MouseEvent e )
		{
		}

		public void mouseClicked( MouseEvent e )
		{
		}

		public void mouseEntered( MouseEvent e )
		{
		}

		public void mouseExited( MouseEvent e )
		{
		}

		public void applyChanges()
		{
		}
	}

	protected class StatusEffectPanel extends LabeledScrollPanel
	{
		private ShowDescriptionList elementList;

		public StatusEffectPanel()
		{
			super( "Active Effects", "uneffect", "add to mood", new ShowDescriptionList( activeEffects ) );
			this.elementList = (ShowDescriptionList) this.scrollComponent;
		}

		public void actionConfirmed()
		{
			Object [] effects = this.elementList.getSelectedValues();
			for ( int i = 0; i < effects.length; ++i )
				RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[i] ) );
		}

		public void actionCancelled()
		{
			Object [] effects = this.elementList.getSelectedValues();
			this.elementList.clearSelection();

			if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
				StaticEntity.setProperty( "currentMood", "default" );

			String name, action;

			for ( int i = 0; i < effects.length; ++i )
			{
				name = ((AdventureResult) effects[i]).getName();

				action = MoodSettings.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodSettings.addTrigger( "lose_effect", name, action );
					continue;
				}

				action = MoodSettings.getDefaultAction( "gain_effect", name );
				if ( !action.equals( "" ) )
					MoodSettings.addTrigger( "gain_effect", name, action );
			}
		}

	}

	/**
	 * An internal class which represents the panel used for storing and
	 * removing meat from the closet.
	 */

	protected class MeatTransferPanel extends LabeledKoLPanel
	{
		private int transferType;
		private JTextField amountField;
		private JLabel closetField;

		public MeatTransferPanel( int transferType )
		{
			super(
				transferType == ItemStorageRequest.MEAT_TO_CLOSET ? "Put Meat in Your Closet" :
				transferType == ItemStorageRequest.MEAT_TO_INVENTORY ? "Take Meat from Your Closet" :
				transferType == ItemStorageRequest.PULL_MEAT_FROM_STORAGE ? "Pull Meat from Hagnk's" :
				"Unknown Transfer Type", "transfer", "bedidall", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			this.amountField = new JTextField();
			this.closetField = new JLabel( " " );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Amount: ", this.amountField );
			elements[1] = new VerifiableElement( "Available: ", this.closetField );

			this.setContent( elements );

			this.transferType = transferType;
			this.refreshCurrentAmount();

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new AmountRefresher() ) );
		}

		private void refreshCurrentAmount()
		{
			switch ( this.transferType )
			{
			case ItemStorageRequest.MEAT_TO_CLOSET:
				this.closetField.setText( COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) + " meat" );
				break;

			case ItemStorageRequest.MEAT_TO_INVENTORY:
				this.closetField.setText( COMMA_FORMAT.format( KoLCharacter.getClosetMeat() ) + " meat" );
				break;

			case ItemStorageRequest.PULL_MEAT_FROM_STORAGE:
				this.closetField.setText( COMMA_FORMAT.format( KoLCharacter.getStorageMeat() ) + " meat" );
				break;

			default:
				this.closetField.setText( "Information not available" );
				break;
			}
		}

		public void actionConfirmed()
		{
			int amountToTransfer = getValue( this.amountField );

			RequestThread.openRequestSequence();
			RequestThread.postRequest( new ItemStorageRequest( this.transferType, amountToTransfer ) );
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{	KoLmafiaGUI.constructFrame( "MoneyMakingGameFrame" );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		private class AmountRefresher implements Runnable
		{
			public void run()
			{	MeatTransferPanel.this.refreshCurrentAmount();
			}
		}
	}

	protected class ScriptPanel extends OptionsPanel
	{
		private ScriptSelectPanel loginScript;
		private ScriptSelectPanel logoutScript;

		public ScriptPanel()
		{
			super( "Miscellaneous Scripts" );

			this.loginScript = new ScriptSelectPanel( new JTextField() );
			this.logoutScript = new ScriptSelectPanel( new JTextField() );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "On Login: ", this.loginScript );
			elements[1] = new VerifiableElement( "On Logout: ", this.logoutScript );
			elements[2] = new VerifiableElement();

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "loginScript", this.loginScript.getText() );
			StaticEntity.setProperty( "logoutScript", this.logoutScript.getText() );
		}

		public void actionCancelled()
		{
			String loginScript = StaticEntity.getProperty( "loginScript" );
			this.loginScript.setText( loginScript );

			String logoutScript = StaticEntity.getProperty( "logoutScript" );
			this.logoutScript.setText( logoutScript );
		}

	}

	protected class BreakfastPanel extends JPanel implements ActionListener
	{
		private String breakfastType;
		private JCheckBox [] skillOptions;

		private JCheckBox grabClovers;
		private JCheckBox mushroomPlot;
		private JCheckBox rumpusRoom;
		private JCheckBox readManual;
		private JCheckBox loginRecovery;
		private JCheckBox pathedSummons;

		public BreakfastPanel( String title, String breakfastType )
		{
			super( new BorderLayout() );

			this.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			JPanel centerPanel = new JPanel( new GridLayout( 4, 3 ) );

			this.loginRecovery = new JCheckBox( "enable auto-recovery" );
			this.loginRecovery.addActionListener( this );
			centerPanel.add( this.loginRecovery );

			this.pathedSummons = new JCheckBox( "honor path restrictions" );
			this.pathedSummons.addActionListener( this );
			centerPanel.add( this.pathedSummons );

			this.rumpusRoom = new JCheckBox( "visit clan rumpus room" );
			this.rumpusRoom.addActionListener( this );
			centerPanel.add( this.rumpusRoom );

			this.breakfastType = breakfastType;
			this.skillOptions = new JCheckBox[ UseSkillRequest.BREAKFAST_SKILLS.length ];
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				this.skillOptions[i] = new JCheckBox( UseSkillRequest.BREAKFAST_SKILLS[i].toLowerCase() );
				this.skillOptions[i].addActionListener( this );
				centerPanel.add( this.skillOptions[i] );
			}

			this.mushroomPlot = new JCheckBox( "plant mushrooms" );
			this.mushroomPlot.addActionListener( this );
			centerPanel.add( this.mushroomPlot );

			this.grabClovers = new JCheckBox( "get hermit clovers" );
			this.grabClovers.addActionListener( this );
			centerPanel.add( this.grabClovers );

			this.readManual = new JCheckBox( "read guild manual" );
			this.readManual.addActionListener( this );
			centerPanel.add( this.readManual );

			JPanel centerHolder = new JPanel( new BorderLayout() );
			centerHolder.add( centerPanel, BorderLayout.NORTH );

			JPanel centerContainer = new JPanel( new CardLayout( 10, 10 ) );
			centerContainer.add( centerHolder, "" );

			this.add( centerContainer, BorderLayout.CENTER );

			this.actionCancelled();
		}

		public void actionPerformed( ActionEvent e )
		{	this.actionConfirmed();
		}

		public void actionConfirmed()
		{
			StringBuffer skillString = new StringBuffer();

			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				if ( this.skillOptions[i].isSelected() )
				{
					if ( skillString.length() != 0 )
						skillString.append( "," );

					skillString.append( UseSkillRequest.BREAKFAST_SKILLS[i] );
				}
			}

			StaticEntity.setProperty( "breakfast" + this.breakfastType, skillString.toString() );
			StaticEntity.setProperty( "loginRecovery" + this.breakfastType, String.valueOf( this.loginRecovery.isSelected() ) );
			StaticEntity.setProperty( "pathedSummons" + this.breakfastType, String.valueOf( this.pathedSummons.isSelected() ) );
			StaticEntity.setProperty( "visitRumpus" + this.breakfastType, String.valueOf( this.rumpusRoom.isSelected() ) );
			StaticEntity.setProperty( "autoPlant" + this.breakfastType, String.valueOf( this.mushroomPlot.isSelected() ) );
			StaticEntity.setProperty( "grabClovers" + this.breakfastType, String.valueOf( this.grabClovers.isSelected() ) );
			StaticEntity.setProperty( "readManual" + this.breakfastType, String.valueOf( this.readManual.isSelected() ) );
		}

		public void actionCancelled()
		{
			String skillString = StaticEntity.getProperty( "breakfast" + this.breakfastType );
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
				this.skillOptions[i].setSelected( skillString.indexOf( UseSkillRequest.BREAKFAST_SKILLS[i] ) != -1 );

			this.loginRecovery.setSelected( StaticEntity.getBooleanProperty( "loginRecovery" + this.breakfastType ) );
			this.pathedSummons.setSelected( StaticEntity.getBooleanProperty( "pathedSummons" + this.breakfastType ) );
			this.rumpusRoom.setSelected( StaticEntity.getBooleanProperty( "visitRumpus" + this.breakfastType ) );
			this.mushroomPlot.setSelected( StaticEntity.getBooleanProperty( "autoPlant" + this.breakfastType ) );
			this.grabClovers.setSelected( StaticEntity.getBooleanProperty( "grabClovers" + this.breakfastType ) );
			this.readManual.setSelected( StaticEntity.getBooleanProperty( "readManual" + this.breakfastType ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	protected class StartupFramesPanel extends KoLPanel implements ListDataListener
	{
		private final String [][] FRAME_OPTIONS =
		{
			{ "Adventure", "AdventureFrame" },
			{ "Mini-Browser", "RequestFrame" },
			{ "Relay Server", "LocalRelayServer" },

			{ "Purchases", "MallSearchFrame" },
			{ "Graphical CLI", "CommandDisplayFrame" },

			{ "Player Status", "CharsheetFrame" },
			{ "Item Manager", "ItemManageFrame" },
			{ "Gear Changer", "GearChangeFrame" },

			{ "Store Manager", "StoreManageFrame" },
			{ "Museum Display", "MuseumFrame" },

			{ "Hall of Legends", "MeatManageFrame" },
			{ "Skill Casting", "SkillBuffFrame" },

			{ "Contact List", "ContactListFrame" },
			{ "Buffbot Manager", "BuffBotFrame" },
			{ "Purchase Buffs", "BuffRequestFrame" },

			{ "Flower Hunter", "FlowerHunterFrame" },
			{ "Mushroom Plot", "MushroomFrame" },
			{ "Familiar Trainer", "FamiliarTrainingFrame" },

			{ "IcePenguin Express", "MailboxFrame" },
			{ "KoLmafia Chat", "KoLMessenger" },
			{ "Recent Events", "EventsFrame" },

			{ "Clan Management", "ClanManageFrame" },
			{ "Farmer's Almanac", "CalendarFrame" },
			{ "Internal Database", "ExamineItemsFrame" },

			{ "Coin Toss Game", "MoneyMakingGameFrame" },
			{ "Preferences", "OptionsFrame" }
		};

		private boolean isRefreshing = false;
		private JComboBox usernameComboBox;

		private ScriptSelectPanel loginScript;
		private ScriptSelectPanel logoutScript;

		private LockableListModel completeList = new LockableListModel();
		private LockableListModel startupList = new LockableListModel();
		private LockableListModel desktopList = new LockableListModel();

		public StartupFramesPanel()
		{
			super( new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			this.usernameComboBox = new JComboBox( saveStateNames );
			this.loginScript = new ScriptSelectPanel( new JTextField() );
			this.logoutScript = new ScriptSelectPanel( new JTextField() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Settings:  ", this.usernameComboBox );

			this.setContent( elements );

			for ( int i = 0; i < this.FRAME_OPTIONS.length; ++i )
				this.completeList.add( this.FRAME_OPTIONS[i][0] );

			JPanel optionPanel = new JPanel( new GridLayout( 1, 3, 10, 10 ) );
			optionPanel.add( new LabeledScrollPanel( "Complete List", new JDnDList( this.completeList, false ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup as Window", new JDnDList( this.startupList ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup in Tabs", new JDnDList( this.desktopList ) ) );

			JTextArea message = new JTextArea(
				"These are the per-user settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n" +

				"When you place the Local Relay Server into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the Contact List into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n" );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( DEFAULT_FONT );

			this.container.add( message, BorderLayout.NORTH );
			this.container.add( optionPanel, BorderLayout.SOUTH );
			this.actionCancelled();

			this.completeList.addListDataListener( this );
			this.startupList.addListDataListener( this );
			this.desktopList.addListDataListener( this );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "loginScript", this.loginScript.getText() );
			StaticEntity.setProperty( "logoutScript", this.logoutScript.getText() );

			this.actionCancelled();
		}

		public void actionCancelled()
		{
			this.isRefreshing = true;

			String username = (String) saveStateNames.getSelectedItem();
			if ( username == null )
				username = "";

			this.startupList.clear();
			this.desktopList.clear();

			String frameString = StaticEntity.getGlobalProperty( username, "initialFrames" );
			String desktopString = StaticEntity.getGlobalProperty( username, "initialDesktop" );

			if ( frameString.equals( "" ) && desktopString.equals( "" ) )
			{
				frameString = StaticEntity.getGlobalProperty( "", "initialFrames" );
				desktopString = StaticEntity.getGlobalProperty( "", "initialDesktop" );
			}

			String [] pieces;

			pieces = frameString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( !this.startupList.contains( this.FRAME_OPTIONS[j][0] ) && this.FRAME_OPTIONS[j][1].equals( pieces[i] ) )
						this.startupList.add( this.FRAME_OPTIONS[j][0] );

			pieces = desktopString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( !this.desktopList.contains( this.FRAME_OPTIONS[j][0] ) && this.FRAME_OPTIONS[j][1].equals( pieces[i] ) )
						this.desktopList.add( this.FRAME_OPTIONS[j][0] );

			this.isRefreshing = false;
			this.saveLayoutSettings();

			this.loginScript.setText( StaticEntity.getProperty( "loginScript" ) );
			this.logoutScript.setText( StaticEntity.getProperty( "logoutScript" ) );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void intervalAdded( ListDataEvent e )
		{
			if ( e.getSource() == this.startupList )
				this.desktopList.removeAll( this.startupList );

			if ( e.getSource() == this.desktopList )
				this.startupList.removeAll( this.desktopList );

			this.saveLayoutSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	this.saveLayoutSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( this.isRefreshing )
				return;

			StringBuffer frameString = new StringBuffer();
			StringBuffer desktopString = new StringBuffer();

			for ( int i = 0; i < this.startupList.getSize(); ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( this.startupList.getElementAt(i).equals( this.FRAME_OPTIONS[j][0] ) )
					{
						if ( frameString.length() != 0 ) frameString.append( "," );
						frameString.append( this.FRAME_OPTIONS[j][1] );
					}

			for ( int i = 0; i < this.desktopList.getSize(); ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( this.desktopList.getElementAt(i).equals( this.FRAME_OPTIONS[j][0] ) )
					{
						if ( desktopString.length() != 0 ) desktopString.append( "," );
						desktopString.append( this.FRAME_OPTIONS[j][1] );
					}

			StaticEntity.setGlobalProperty( "", "initialFrames", frameString.toString() );
			StaticEntity.setGlobalProperty( "", "initialDesktop", desktopString.toString() );

			if ( saveStateNames.size() != 0 )
			{
				String username = (String) saveStateNames.getSelectedItem();
				if ( username == null )
					username = "";

				StaticEntity.setGlobalProperty( username, "initialFrames", frameString.toString() );
				StaticEntity.setGlobalProperty( username, "initialDesktop", desktopString.toString() );
			}
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	protected class UserInterfacePanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private String [][] options =

			System.getProperty( "os.name" ).startsWith( "Win" ) ?

			new String [][]
			{
				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "useSystemTrayIcon", "Minimize main interface to system tray" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
				{ "addStatusBarToFrames", "Add a status line to independent windows" },
				{},
				{ "addExitMenuItems", "Add logout and exit options to general menu" },
				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
			}

			:

			new String [][]
  			{
  				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
  				{ "addStatusBarToFrames", "Add a status line to independent windows" },
  				{},
				{ "addExitMenuItems", "Add logout and exit options to general menu" },
  				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
  				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
  			};

		private JComboBox looks, toolbars, scripts;

		public UserInterfacePanel()
		{
			super( "", new Dimension( 80, 20 ), new Dimension( 280, 20 ) );

			UIManager.LookAndFeelInfo [] installed = UIManager.getInstalledLookAndFeels();
			Object [] installedLooks = new Object[ installed.length ];

			for ( int i = 0; i < installedLooks.length; ++i )
				installedLooks[i] = installed[i].getClassName();

			this.looks = new JComboBox( installedLooks );

			this.toolbars = new JComboBox();
			this.toolbars.addItem( "Show global menus only" );
			this.toolbars.addItem( "Put toolbar along top of panel" );
			this.toolbars.addItem( "Put toolbar along bottom of panel" );
			this.toolbars.addItem( "Put toolbar along left of panel" );

			this.scripts = new JComboBox();
			this.scripts.addItem( "Do not show script bar on main interface" );
			this.scripts.addItem( "Put script bar after normal toolbar" );
			this.scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement [] elements = new VerifiableElement[3];

			elements[0] = new VerifiableElement( "Java L&F: ", this.looks );
			elements[1] = new VerifiableElement( "Toolbar: ", this.toolbars );
			elements[2] = new VerifiableElement( "Scripts: ", this.scripts );

			this.actionCancelled();
			this.setContent( elements );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			this.add( new InterfaceCheckboxPanel(), BorderLayout.CENTER );
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void actionConfirmed()
		{
			String lookAndFeel = (String) this.looks.getSelectedItem();
			if ( lookAndFeel != null )
				StaticEntity.setProperty( "swingLookAndFeel", lookAndFeel );

			StaticEntity.setProperty( "useToolbars", String.valueOf( this.toolbars.getSelectedIndex() != 0 ) );
			StaticEntity.setProperty( "scriptButtonPosition", String.valueOf( this.scripts.getSelectedIndex() ) );
			StaticEntity.setProperty( "toolbarPosition", String.valueOf( this.toolbars.getSelectedIndex() ) );
		}

		public void actionCancelled()
		{
			this.looks.setSelectedItem( StaticEntity.getProperty( "swingLookAndFeel" ) );
			this.toolbars.setSelectedIndex( StaticEntity.getIntegerProperty( "toolbarPosition" ) );
			this.scripts.setSelectedIndex( StaticEntity.getIntegerProperty( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel extends OptionsPanel
		{
			private JLabel innerGradient, outerGradient;

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
				VerifiableElement [] elements = new VerifiableElement[ UserInterfacePanel.this.options.length + 3 ];

				UserInterfacePanel.this.optionBoxes = new JCheckBox[ UserInterfacePanel.this.options.length ];
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					UserInterfacePanel.this.optionBoxes[i] = new JCheckBox();

				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					if ( UserInterfacePanel.this.options[i].length == 0 )
						elements[i] = new VerifiableElement();
					else
						elements[i] = new VerifiableElement( UserInterfacePanel.this.options[i][1], JLabel.LEFT, UserInterfacePanel.this.optionBoxes[i] );
				}

				elements[ UserInterfacePanel.this.options.length ] = new VerifiableElement();

				this.outerGradient = new TabColorChanger( "outerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 1 ] = new VerifiableElement( "Change the outer portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, this.outerGradient );

				this.innerGradient = new TabColorChanger( "innerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 2 ] = new VerifiableElement( "Change the inner portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, this.innerGradient );

				this.actionCancelled();
				this.setContent( elements );
			}

			public void actionConfirmed()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					if ( UserInterfacePanel.this.options[i].length > 0 )
						StaticEntity.setProperty( UserInterfacePanel.this.options[i][0], String.valueOf( UserInterfacePanel.this.optionBoxes[i].isSelected() ) );

				super.actionConfirmed();
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					if ( UserInterfacePanel.this.options[i].length > 0 )
						UserInterfacePanel.this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( UserInterfacePanel.this.options[i][0] ) );

				this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedA );
				this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedB );
			}

			public void setEnabled( boolean isEnabled )
			{
			}

			private class TabColorChanger extends LabelColorChanger
			{
				public TabColorChanger( String property )
				{	super( property );
				}

				public void applyChanges()
				{
					if ( this.property.equals( "innerTabColor" ) )
						CloseTabPaneEnhancedUI.selectedA = InterfaceCheckboxPanel.this.innerGradient.getBackground();
					else
						CloseTabPaneEnhancedUI.selectedB = InterfaceCheckboxPanel.this.outerGradient.getBackground();

					KoLFrame.this.tabs.repaint();
				}
			}
		}
	}

	protected class UsableItemPanel extends ItemManagePanel
	{
		public UsableItemPanel( boolean isRestoresOnly )
		{
			super( "Use Items", "use item", "check wiki", inventory );

			if ( !isRestoresOnly )
				this.setButtons( true, false, null );

			this.filterItems();
		}

		public FilterItemField getWordFilter()
		{	return new UsableItemFilterField();
		}

		public void actionConfirmed()
		{
			Object [] items = this.getDesiredItems( "Consume" );
			if ( items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
		}

		public void actionCancelled()
		{
			String name;
			Object [] values = this.elementList.getSelectedValues();

			for ( int i = 0; i < values.length; ++i )
			{
				name = ((AdventureResult)values[i]).getName();
				if ( name != null )
					StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
			}
		}

		private class UsableItemFilterField extends FilterItemField
		{
			public SimpleListFilter getFilter()
			{	return new UsableItemFilter();
			}

			private class UsableItemFilter extends SimpleListFilter
			{
				public UsableItemFilter()
				{	super( UsableItemFilterField.this );
				}

				public boolean isVisible( Object element )
				{
					AdventureResult item = (AdventureResult)element;
					int itemId = item.getItemId();

					if ( !UsableItemFilterField.this.notrade && !TradeableItemDatabase.isTradeable( itemId ) )
					     return false;

					boolean filter = false;

					switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
					{
					case CONSUME_EAT:
						filter = UsableItemFilterField.this.food;
						break;

					case CONSUME_DRINK:
						filter =  UsableItemFilterField.this.booze;
						break;

					case CONSUME_USE:
					case CONSUME_MULTIPLE:
					case GROW_FAMILIAR:
					case CONSUME_ZAP:
						filter = UsableItemFilterField.this.other;
						break;

					case EQUIP_FAMILIAR:
					case EQUIP_ACCESSORY:
					case EQUIP_HAT:
					case EQUIP_PANTS:
					case EQUIP_SHIRT:
					case EQUIP_WEAPON:
					case EQUIP_OFFHAND:
						filter = UsableItemFilterField.this.equip;
						break;

					case MP_RESTORE:
					case HP_RESTORE:
						filter = UsableItemFilterField.this.restores;
						break;

					default:
					case NO_CONSUME:
						filter = false;
						break;
					}

					return filter && super.isVisible( element );
				}
			}
		}
	}
}
