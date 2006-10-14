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

// containers
import java.awt.Image;
import java.io.FilenameFilter;
import javax.swing.JSeparator;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;
import javax.swing.table.TableModel;

// layout
import java.awt.Component;
import java.awt.Point;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

// event listeners
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// basic utilities
import java.io.File;
import java.lang.reflect.Method;
import java.util.Vector;

// other stuff
import javax.swing.SwingUtilities;
import java.lang.ref.WeakReference;

// spellcast imports
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends JFrame implements KoLConstants
{
	protected static final FilenameFilter BACKUP_FILTER = new FilenameFilter()
	{
		public boolean accept( File dir, String name )
		{
			return !name.startsWith( "." ) && !name.endsWith( "~" ) && !name.endsWith( ".bak" ) && !name.endsWith( ".map" ) && !name.endsWith( ".dat" ) &&
				name.indexOf( "datamaps" ) == -1 && dir.getPath().indexOf( "datamaps" ) == -1;
		}
	};

	protected JTabbedPane tabs = null;
	protected String lastTitle;
	protected String frameName;
	protected JPanel framePanel;

	protected StatusRefresher refresher = null;
	protected KoLCharacterAdapter refreshListener = null;

	static
	{
		compileScripts();
		compileBookmarks();
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given StaticEntity.getClient().
	 */

	protected KoLFrame()
	{
		this( "" );
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given StaticEntity.getClient().
	 */

	protected KoLFrame( String title )
	{
		setTitle( title );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		this.framePanel = new JPanel( new BorderLayout( 0, 0 ) );
		getContentPane().add( this.framePanel, BorderLayout.CENTER );

		this.frameName = getClass().getName();
		this.frameName = frameName.substring( frameName.lastIndexOf( "." ) + 1 );

		boolean shouldAddFrame = !(this instanceof KoLDesktop) && !(this instanceof ContactListFrame);

		if ( this instanceof ChatFrame )
			shouldAddFrame = !KoLMessenger.usingTabbedChat() || this instanceof TabbedChatFrame;

		if ( shouldAddFrame )
			existingFrames.add( this );

		if ( StaticEntity.usesSystemTray() )
			addWindowListener( new MinimizeListener() );
	}

	protected final void addTab( String name, JComponent panel )
	{
		if ( tabs == null )
			return;

		SimpleScrollPane scroller = new SimpleScrollPane( panel );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		tabs.add( name, scroller );
	}

	public final void setTitle( String newTitle )
	{
		this.lastTitle = newTitle;
		KoLDesktop.setTitle( this, newTitle );

		super.setTitle( !(this instanceof LoginFrame || this instanceof ContactListFrame) && KoLCharacter.getUsername().length() > 0 ?
			KoLCharacter.getUsername() + ": " + this.lastTitle : this.lastTitle );
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

	protected final JToolBar getToolbar()
	{
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

		case 4:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
			getContentPane().add( toolbarPanel, BorderLayout.EAST );
			break;

		default:

			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			if ( this instanceof LoginFrame || this instanceof ChatFrame )
			{
				getContentPane().add( toolbarPanel, BorderLayout.NORTH );
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
		if ( isVisible() )
			rememberPosition();

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		super.dispose();
		existingFrames.remove( this );
		KoLDesktop.removeTab( this );

		if ( refreshListener != null )
			KoLCharacter.removeCharacterListener( refreshListener );

		// If the list of frames is now empty, make sure
		// you end the session.  Ending the session for
		// a login frame involves exiting, and ending the
		// session for all other frames is calling main.

		if ( existingFrames.isEmpty() && StaticEntity.getClient() instanceof KoLmafiaGUI )
			(new RestartThread()).start();
	}

	private class RestartThread extends Thread
	{
		public void run()
		{
			if ( !KoLmafia.executedLogin() )
				System.exit(0);

			KoLmafia.forceContinue();
			createDisplay( LoginFrame.class );
			(new LogoutRequest()).run();
		}
	}

	public String toString()
	{	return lastTitle;
	}

	public String getFrameName()
	{	return frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component.
	 * Note that this method can only be used if the KoLFrame on which it
	 * is called has not yet added any components.  If there are any added
	 * components, this method will do nothing.
	 */

	public void addCompactPane()
	{
		if ( refresher != null )
			return;

		refresher = new StatusRefresher( StaticEntity.getBooleanProperty( "useTextHeavySidepane" ) );
		refresher.run();

		refreshListener = new KoLCharacterAdapter( refresher );
		KoLCharacter.addCharacterListener( refreshListener );

		refresher.getCompactPane().setBackground( ENABLED_COLOR );
		getContentPane().add( refresher.getCompactPane(), BorderLayout.WEST );
	}

	protected static class StatusRefresher implements Runnable
	{
		private JPanel compactPane;
		private JLabel levelLabel, roninLabel, mcdLabel;
		private JLabel musLabel, mysLabel, moxLabel, drunkLabel;
		private JLabel hpLabel, mpLabel, meatLabel, advLabel;
		private JLabel familiarLabel;
		private JLabel mlLabel, combatLabel, initLabel;
		private JLabel xpLabel, meatDropLabel, itemDropLabel;

		protected boolean useTextOnly;

		public StatusRefresher( boolean useTextOnly )
		{
			this.useTextOnly = useTextOnly;

			if ( useTextOnly )
				addTextOnlyCompactPane();
			else
				addGraphicalCompactPane();
		}

		protected String getStatText( int adjusted, int base )
		{
			return adjusted == base ? "<html>" + Integer.toString( base ) :
				adjusted >  base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" :
				"<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
		}

		public void run()
		{
			if ( useTextOnly )
				updateTextOnly();
			else
				updateGraphical();

			FamiliarData familiar = KoLCharacter.getFamiliar();
			int id = familiar == null ? -1 : familiar.getID();

			if ( id == -1 )
			{
				familiarLabel.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
				familiarLabel.setText( "0 lbs." );
				familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
				familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
			}
			else
			{
				ImageIcon familiarIcon = FamiliarsDatabase.getFamiliarImage( id );
				familiarLabel.setIcon( familiarIcon );
				familiarLabel.setText( familiar.getModifiedWeight() + (familiar.getModifiedWeight() == 1 ? " lb." : " lbs.") );
				familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
				familiarLabel.setHorizontalTextPosition( JLabel.CENTER );

				familiarLabel.updateUI();
			}
		}

		public void addTextOnlyCompactPane()
		{
			JPanel [] panels = new JPanel[5];
			int panelCount = -1;

			panels[ ++panelCount ] = new JPanel( new GridLayout( 3, 1 ) );
			panels[ panelCount ].add( levelLabel = new JLabel( " ", JLabel.CENTER ) );
			panels[ panelCount ].add( roninLabel = new JLabel( " ", JLabel.CENTER ) );

			if ( KoLCharacter.inMysticalitySign() || true )
				panels[ panelCount ].add( mcdLabel = new JLabel( " ", JLabel.CENTER ) );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 4, 2 ) );
			panels[ panelCount ].add( new JLabel( "Mus: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( musLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Mys: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( mysLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Mox: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( moxLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Drunk: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( drunkLabel = new JLabel( " ", JLabel.LEFT) );

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			panels[ panelCount ].setOpaque( false );

				JPanel labelPanel = new JPanel( new GridLayout( 4, 1 ) );
				labelPanel.setOpaque( false );

				labelPanel.add( new JLabel( "    HP: ", JLabel.RIGHT ) );
				labelPanel.add( new JLabel( "    MP: ", JLabel.RIGHT ) );
				labelPanel.add( new JLabel( "    Meat: ", JLabel.RIGHT ) );
				labelPanel.add( new JLabel( "    Adv: ", JLabel.RIGHT ) );

				JPanel valuePanel = new JPanel( new GridLayout( 4, 1 ) );
				valuePanel.setOpaque( false );

				valuePanel.add( hpLabel = new JLabel( " ", JLabel.LEFT ) );
				valuePanel.add( mpLabel = new JLabel( " ", JLabel.LEFT ) );
				valuePanel.add( meatLabel = new JLabel( " ", JLabel.LEFT ) );
				valuePanel.add( advLabel = new JLabel( " ", JLabel.LEFT ) );

			panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
			panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 1, 1 ) );
			panels[ panelCount ].add( familiarLabel = new UnanimatedLabel() );

			panels[ ++panelCount ] = new JPanel( new GridLayout( 6, 2 ) );
			panels[ panelCount ].add( new JLabel( "ML: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( mlLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Combat: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( combatLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Init: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( initLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "XP: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( xpLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Meat: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( meatDropLabel = new JLabel( " ", JLabel.LEFT ) );
			panels[ panelCount ].add( new JLabel( "Item: ", JLabel.RIGHT ) );
			panels[ panelCount ].add( itemDropLabel = new JLabel( " ", JLabel.LEFT ) );

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
			refreshPanel.add( new RequestButton( "Refresh Status", "refresh.gif", CharpaneRequest.getInstance() ) );

			compactPane = new JPanel( new BorderLayout() );
			compactPane.add( compactCard, BorderLayout.NORTH );
			compactPane.add( refreshPanel, BorderLayout.SOUTH );
		}

		protected JPanel getCompactPane()
		{	return compactPane;
		}

		protected void addGraphicalCompactPane()
		{
			JPanel compactContainer = new JPanel( new GridLayout( 7, 1, 0, 20 ) );
			compactContainer.setOpaque( false );

			compactContainer.add( hpLabel = new JLabel( " ", JComponentUtilities.getImage( "hp.gif" ), JLabel.CENTER ) );
			compactContainer.add( mpLabel = new JLabel( " ", JComponentUtilities.getImage( "mp.gif" ), JLabel.CENTER ) );

			compactContainer.add( familiarLabel = new UnanimatedLabel() );

			compactContainer.add( meatLabel = new JLabel( " ", JComponentUtilities.getImage( "meat.gif" ), JLabel.CENTER ) );
			compactContainer.add( advLabel = new JLabel( " ", JComponentUtilities.getImage( "hourglass.gif" ), JLabel.CENTER ) );
			compactContainer.add( drunkLabel = new JLabel( " ", JComponentUtilities.getImage( "sixpack.gif" ), JLabel.CENTER) );

			compactContainer.add( Box.createHorizontalStrut( 80 ) );

			compactPane = new JPanel();
			compactPane.setLayout( new BoxLayout( this.compactPane, BoxLayout.Y_AXIS ) );
			compactPane.add( Box.createVerticalStrut( 20 ) );
			compactPane.add( compactContainer );
		}

		protected void updateTextOnly()
		{
			levelLabel.setText( "Level " + KoLCharacter.getLevel() );
			roninLabel.setText( KoLCharacter.isHardcore() ? "(Hardcore)" : KoLCharacter.canInteract() ? "(Ronin Clear)" :
				"(Ronin for " + (600 - KoLCharacter.getTotalTurnsUsed()) + ")" );

			mcdLabel.setText( "MCD @ " + KoLCharacter.getMindControlLevel() );

			musLabel.setText( getStatText( KoLCharacter.getAdjustedMuscle(), KoLCharacter.getBaseMuscle() ) );
			mysLabel.setText( getStatText( KoLCharacter.getAdjustedMysticality(), KoLCharacter.getBaseMysticality() ) );
			moxLabel.setText( getStatText( KoLCharacter.getAdjustedMoxie(), KoLCharacter.getBaseMoxie() ) );

			drunkLabel.setText( String.valueOf( KoLCharacter.getInebriety() ) );

			hpLabel.setText( COMMA_FORMAT.format( KoLCharacter.getCurrentHP() ) + "/" + COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			mpLabel.setText( COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + "/" + COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );
			meatLabel.setText( COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );

			int ml = KoLCharacter.getMonsterLevelAdjustment();
			mlLabel.setText( MODIFIER_FORMAT.format( ml ) );
			combatLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatPercentAdjustment() ) + "%" );
			initLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );
			xpLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getFixedXPAdjustment() + (float)ml / 5.0 ) );
			meatDropLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			itemDropLabel.setText( ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );
		}

		protected void updateGraphical()
		{
			hpLabel.setText( KoLCharacter.getCurrentHP() + " / " + KoLCharacter.getMaximumHP() );
			hpLabel.setVerticalTextPosition( JLabel.BOTTOM );
			hpLabel.setHorizontalTextPosition( JLabel.CENTER );

			mpLabel.setText( KoLCharacter.getCurrentMP() + " / " + KoLCharacter.getMaximumMP() );
			mpLabel.setVerticalTextPosition( JLabel.BOTTOM );
			mpLabel.setHorizontalTextPosition( JLabel.CENTER );

			meatLabel.setText( COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			meatLabel.setVerticalTextPosition( JLabel.BOTTOM );
			meatLabel.setHorizontalTextPosition( JLabel.CENTER );

			advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );
			advLabel.setVerticalTextPosition( JLabel.BOTTOM );
			advLabel.setHorizontalTextPosition( JLabel.CENTER );

			drunkLabel.setText( String.valueOf( KoLCharacter.getInebriety() ) );
			drunkLabel.setVerticalTextPosition( JLabel.BOTTOM );
			drunkLabel.setHorizontalTextPosition( JLabel.CENTER );
		}
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

			if ( refresher != null )
				refresher.getCompactPane().setBackground( ERROR_COLOR );

			setEnabled( true );
			break;

		case ENABLE_STATE:

			if ( refresher != null )
				refresher.getCompactPane().setBackground( ENABLED_COLOR );

			setEnabled( true );
			break;

		default:

			if ( refresher != null )
				refresher.getCompactPane().setBackground( DISABLED_COLOR );

			setEnabled( false );
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

	protected class MultiButtonPanel extends JPanel
	{
		protected boolean showMovers;
		protected JPanel enclosingPanel;
		protected JPanel optionPanel;
		protected LockableListModel elementModel;
		protected ShowDescriptionList elementList;

		protected JButton [] buttons;
		protected JRadioButton [] movers;

		public MultiButtonPanel( String title, LockableListModel elementModel, boolean showMovers )
		{
			existingPanels.add( new WeakReference( this ) );
			this.showMovers = showMovers;
			this.optionPanel = new JPanel();

			this.elementModel = elementModel;
			this.elementList = new ShowDescriptionList( elementModel );

			enclosingPanel = new JPanel( new BorderLayout( 10, 10 ) );
			enclosingPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			enclosingPanel.add( new SimpleScrollPane( elementList ), BorderLayout.CENTER );

			setLayout( new CardLayout( 10, 0 ) );
			add( enclosingPanel, "" );
		}

		public void setButtons( String [] buttonLabels, ActionListener [] buttonListeners )
		{
			JPanel buttonPanel = new JPanel();
			buttons = new JButton[ buttonLabels.length ];

			for ( int i = 0; i < buttonLabels.length; ++i )
			{
				buttons[i] = new JButton( buttonLabels[i] );
				buttons[i].addActionListener( buttonListeners[i] );
				buttonPanel.add( buttons[i] );
			}

			JPanel moverPanel = new JPanel();

			movers = new JRadioButton[4];
			movers[0] = new JRadioButton( "Move all", true );
			movers[1] = new JRadioButton( "Move all but one" );
			movers[2] = new JRadioButton( "Move multiple" );
			movers[3] = new JRadioButton( "Move exactly one" );

			ButtonGroup moverGroup = new ButtonGroup();
			for ( int i = 0; i < 4; ++i )
			{
				moverGroup.add( movers[i] );
				if ( showMovers )
					moverPanel.add( movers[i] );
			}

			JPanel northPanel = new JPanel( new BorderLayout() );
			northPanel.add( buttonPanel, BorderLayout.SOUTH );
			northPanel.add( moverPanel, BorderLayout.CENTER );
			northPanel.add( optionPanel, BorderLayout.NORTH );

			enclosingPanel.add( northPanel, BorderLayout.NORTH );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( elementList == null || buttons == null || movers == null )
				return;

			if ( buttons.length > 0 && buttons[ buttons.length - 1 ] == null )
				return;

			if ( movers.length > 0 && movers[ movers.length - 1 ] == null )
				return;

			elementList.setEnabled( isEnabled );
			for ( int i = 0; i < buttons.length; ++i )
				buttons[i].setEnabled( isEnabled );

			for ( int i = 0; i < movers.length; ++i )
				movers[i].setEnabled( isEnabled );
		}

		protected AdventureResult [] getDesiredItems( String message )
		{
			return KoLFrame.this.getDesiredItems( elementList, message,
				movers[0].isSelected() ? TAKE_ALL : movers[1].isSelected() ? TAKE_ALL_BUT_ONE :
				movers[2].isSelected() ? TAKE_MULTIPLE : TAKE_ONE );
		}

		protected void filterSelection( boolean eat, boolean drink, boolean other, boolean nosell, boolean notrade )
		{
			Object [] elements = elementList.getSelectedValues();
			for ( int i = 0; i < elements.length; ++i )
			{
				int actualIndex = ((LockableListModel)elementList.getModel()).indexOf( elements[i] );
				if ( !AdventureResult.isVisibleWithFilter( ((LockableListModel)elementList.getModel()).get( actualIndex ), eat, drink, other, nosell, notrade ) )
					elementList.removeSelectionInterval( actualIndex, actualIndex );
			}
		}
	}

	protected static final int TAKE_ALL = 1;
	protected static final int TAKE_ALL_BUT_ONE = 2;
	protected static final int TAKE_MULTIPLE = 3;
	protected static final int TAKE_ONE = 4;

	protected AdventureResult [] getDesiredItems( JList elementList, String message, int quantityType )
	{
		Object [] items = elementList.getSelectedValues();
		if ( items.length == 0 )
			return null;

		int neededSize = items.length;
		AdventureResult currentItem;

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[i];

			int quantity = 0;
			switch ( quantityType )
			{
				case TAKE_ALL:
					quantity = currentItem.getCount();
					break;
				case TAKE_ALL_BUT_ONE:
					quantity = currentItem.getCount() - 1;
					break;
				case TAKE_MULTIPLE:
					quantity = getQuantity( message + " " + currentItem.getName() + "...", currentItem.getCount() );
					break;
				default:
					quantity = 1;
					break;
			}

			// If the user manually enters zero, return from
			// this, since they probably wanted to cancel.

			if ( quantity == 0 && quantityType == TAKE_MULTIPLE )
				return null;

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity == 0 )
			{
				items[i] = null;
				--neededSize;
			}
			else
			{
				items[i] = currentItem.getInstance( quantity );
			}
		}

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		AdventureResult [] desiredItems = new AdventureResult[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
			if ( items[i] != null )
				desiredItems[ neededSize++ ] = (AdventureResult) items[i];

		return desiredItems;
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	protected static class DisplayFrameButton extends ThreadedActionButton
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
			setToolTipText( tooltip );

			this.frameClass = frameClass;
		}

		public void executeTask()
		{	KoLmafiaGUI.constructFrame( frameClass );
		}
	}

	protected static abstract class ThreadedActionButton extends JButton implements ActionListener, Runnable
	{
		public ThreadedActionButton( String text )
		{
			super( text );
			addActionListener( this );
		}

		public ThreadedActionButton( ImageIcon icon )
		{
			super( icon );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	(new Thread( this )).start();
		}

		public final void run()
		{
			KoLmafia.forceContinue();
			executeTask();
			KoLmafia.enableDisplay();
		}

		protected abstract void executeTask();
	}

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	protected static class InvocationButton extends ThreadedActionButton
	{
		protected Object object;
		protected Method method;

		public InvocationButton( String text, Object object, String methodName )
		{
			this( text, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( String text, Class c, String methodName )
		{
			super( text );
			this.object = c;

			completeConstruction( c, methodName );
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
			setToolTipText( tooltip );
			completeConstruction( c, methodName );
		}

		protected void completeConstruction( Class c, String methodName )
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

		public void executeTask()
		{
			try
			{
				if ( method != null )
					method.invoke( object, null );

				KoLmafia.enableDisplay();
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}
	}

	/**
	 * An internal class used to handle requests to open a new frame
	 * using a local panel inside of the adventure frame.
	 */

	protected static class KoLPanelFrameButton extends ThreadedActionButton
	{
		protected Object [] parameters;

		public KoLPanelFrameButton( String tooltip, String icon, ActionPanel panel )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			setToolTipText( tooltip );

			parameters = new Object[2];
			parameters[0] = tooltip;
			parameters[1] = panel;
		}

		public void executeTask()
		{	createDisplay( KoLPanelFrame.class, parameters );
		}
	}

	protected static class RequestButton extends ThreadedActionButton
	{
		protected KoLRequest request;

		public RequestButton( String title, KoLRequest request )
		{
			super( title );
			this.request = request;
		}

		public RequestButton( String title, String icon, KoLRequest request )
		{
			super( JComponentUtilities.getImage( icon ) );
			setToolTipText( title );
			this.request = request;
		}

		public void executeTask()
		{
			StaticEntity.getClient().makeRequest( request );
			KoLmafia.enableDisplay();
		}
	}

	/**
	 * Utility method which retrieves an integer value from the given
	 * field.  In the event that the field does not contain an integer
	 * value, the number "0" is returned instead.
	 */

	protected static final int getValue( JTextField field )
	{	return getValue( field, 0 );
	}

	/**
	 * Utility method which retrieves an integer value from the given
	 * field.  In the event that the field does not contain an integer
	 * value, the default value provided will be returned instead.
	 */

	protected static final int getValue( JTextField field, int defaultValue )
	{
		String currentValue = field.getText();

		if ( currentValue == null || currentValue.length() == 0 )
			return defaultValue;

		if ( currentValue.equals( "*" ) )
			return defaultValue;

		return StaticEntity.parseInt( field.getText().trim() );
	}

	protected static final int getQuantity( String title, int maximumValue, int defaultValue )
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
		return desiredValue <= 0 ? maximumValue - desiredValue : Math.min( desiredValue, maximumValue );
	}

	protected static final int getQuantity( String title, int maximumValue )
	{	return getQuantity( title, maximumValue, maximumValue );
	}

	protected static class FilterCheckBox extends JCheckBox implements ActionListener
	{
		protected boolean isTradeable;
		protected JCheckBox [] filters;
		protected ShowDescriptionList elementList;

		public FilterCheckBox( JCheckBox [] filters, ShowDescriptionList elementList, String label, boolean isSelected )
		{	this( filters, elementList, false, label, isSelected );
		}

		public FilterCheckBox( JCheckBox [] filters, ShowDescriptionList elementList, boolean isTradeable, String label, boolean isSelected )
		{
			super( label, isSelected );
			addActionListener( this );

			this.isTradeable = isTradeable;
			this.filters = filters;
			this.elementList = elementList;
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( isTradeable )
			{
				if ( !filters[0].isSelected() && !filters[1].isSelected() && !filters[2].isSelected() )
				{
					filters[3].setEnabled( false );
					filters[4].setEnabled( false );
				}
				else
				{
					filters[3].setEnabled( true );
					filters[4].setEnabled( true );
				}


				elementList.setCellRenderer(
					AdventureResult.getAutoSellCellRenderer( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(), filters[4].isSelected() ) );
			}
			else
			{
				elementList.setCellRenderer(
					AdventureResult.getConsumableCellRenderer( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected() ) );
			}

			elementList.validate();
		}
	}

	protected static class UnanimatedLabel extends JLabel
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

	protected abstract class ListeningRunnable implements ActionListener, Runnable
	{
		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( this )).start();
		}
	}

	protected void processWindowEvent( WindowEvent e )
	{
		if ( isVisible() )
			rememberPosition();

		super.processWindowEvent( e );
	}

	public void setVisible( boolean isVisible )
	{
		if ( isVisible )
			restorePosition();
		else
			rememberPosition();

		super.setVisible( isVisible );
	}

	protected class KoLHyperlinkAdapter extends HyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			if ( location.startsWith( "desc" ) || location.startsWith( "doc" ) || location.startsWith( "searchp" ) )
			{
				// Certain requests should open in a new window.
				// These include description data, documentation
				// and player searches.

				StaticEntity.openRequestFrame( location );
			}
			else if ( location.equals( "lchat.php" ) )
			{
				KoLMessenger.initialize();
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

	protected String getSettingString( JCheckBox [] restoreCheckbox )
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

	protected SimpleScrollPane constructScroller( JCheckBox [] restoreCheckbox )
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

		if ( !isVisible() )
			restorePosition();
	}

	private void rememberPosition()
	{
		Point p = getLocation();
		if ( this instanceof LoginFrame )
			StaticEntity.setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
		else
			StaticEntity.setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
	}

	private void restorePosition()
	{
		int xLocation = 0;
		int yLocation = 0;
		Dimension screenSize = TOOLKIT.getScreenSize();
		String position = StaticEntity.getProperty( frameName );

		if ( position != null && position.indexOf( "," ) != -1 )
		{
			String [] location = position.split( "," );
			xLocation = StaticEntity.parseInt( location[0] );
			yLocation = StaticEntity.parseInt( location[1] );
		}
		if ( xLocation > 0 && yLocation > 0 && xLocation < screenSize.getWidth() && yLocation < screenSize.getHeight() )
			setLocation( xLocation, yLocation );
		else
			setLocationRelativeTo( null );
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the StaticEntity.getClient()'s current settings.
	 */

	protected abstract class OptionsPanel extends LabeledKoLPanel
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

	protected class LoadScriptButton extends JButton implements Runnable, ActionListener
	{
		private String scriptPath;

		public LoadScriptButton( int scriptID, String scriptPath )
		{
			super( String.valueOf( scriptID ) );

			addActionListener( this );
			this.scriptPath = scriptPath;
			setToolTipText( scriptPath );

			JComponentUtilities.setComponentSize( this, 30, 30 );
		}

		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( this )).start();
		}

		public void run()
		{	DEFAULT_SHELL.executeLine( scriptPath );
		}
	}

	/**
	 * Utility class used to forward events to JButtons enclosed inside
	 * of a JTable object.
	 */

	protected class ButtonEventListener extends MouseAdapter
	{
		private JTable table;

		public ButtonEventListener( JTable table )
		{	this.table = table;
		}

		public void mouseReleased( MouseEvent e )
		{
			TableColumnModel columnModel = table.getColumnModel();

		    int row = e.getY() / table.getRowHeight();
		    int column = columnModel.getColumnIndexAtX( e.getX() );

			if ( row >= 0 && row < table.getRowCount() && column >= 0 && column < table.getColumnCount() )
			{
				Object value = table.getValueAt( row, column );

				if ( value instanceof JButton )
				{
					((JButton) value).dispatchEvent( SwingUtilities.convertMouseEvent( table, e, (JButton) value ) );
					table.repaint();
				}
			}
		}
	}

	protected abstract class NestedInsideTableButton extends JButton implements MouseListener
	{
		public NestedInsideTableButton( ImageIcon icon )
		{
			super( icon );
			addMouseListener( this );
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

	protected abstract class ListWrapperTableModel extends DefaultTableModel implements ListDataListener
	{
		private String [] headers;
		private Class [] types;
		private boolean [] editable;

		public ListWrapperTableModel( String [] headers, Class [] types, boolean [] editable, LockableListModel list )
		{
			super( 0, headers.length );

			this.headers = headers;
			this.types = types;
			this.editable = editable;

			for ( int i = 0; i < list.size(); ++i )
				insertRow( i, constructVector( list.get(i) ) );

			list.addListDataListener( this );
		}

		public String getColumnName( int index )
		{	return index < 0 || index >= headers.length ? "" : headers[ index ];
		}

		public Class getColumnClass( int column )
		{	return column < 0 || column >= types.length ? Object.class : types[ column ];
		}

		protected abstract Vector constructVector( Object o );

		public boolean isCellEditable( int row, int column )
		{	return column < 0 || column >= editable.length ? false : editable[ column ];
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

			if ( index1 >= source.size() || source.size() == getRowCount() )
				return;

			for ( int i = index0; i <= index1; ++i )
				insertRow( i, constructVector( source.get(i) ) );
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

			if ( index1 >= getRowCount() || source.size() == getRowCount() )
				return;

			for ( int i = index1; i >= index0; --i )
				removeRow(i);
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

			int rowCount = getRowCount();

			for ( int i = index1; i >= index0; --i )
			{
				if ( source.size() < i )
					removeRow(i);
				else if ( i > rowCount )
					insertRow( rowCount, constructVector( source.get(i) ) );
				else
				{
					removeRow(i);
					insertRow( i, constructVector( source.get(i) ) );
				}
			}
		}
	}

	protected class TransparentTable extends JTable
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
	}

	protected class IntegerRenderer extends DefaultTableCellRenderer
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

	protected class ButtonRenderer implements TableCellRenderer
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

	protected boolean finalizeTable( JTable table )
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

	private class MinimizeListener extends WindowAdapter
	{
		public void windowIconified( WindowEvent e )
		{	setVisible( false );
		}
	}

	protected static void createDisplay( Class frameClass )
	{	createDisplay( frameClass, NOPARAMS );
	}

	protected static void createDisplay( Class frameClass, Object [] parameters )
	{	SwingUtilities.invokeLater( new CreateFrameRunnable( frameClass, parameters ) );
	}

	public static void compileScripts()
	{
		scripts.clear();

		// Get the list of files in the current directory
		if ( !SCRIPT_DIRECTORY.exists() )
			SCRIPT_DIRECTORY.mkdirs();

		File [] scriptList = SCRIPT_DIRECTORY.listFiles( BACKUP_FILTER );

		// Iterate through the files.  Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		boolean hasDirectories = false;
		boolean hasNormalFiles = false;

		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( scriptList[i].isDirectory() )
			{
				scripts.add( scriptList[i] );
				hasDirectories = true;
			}
			else
				hasNormalFiles = true;
		}

		if ( hasNormalFiles )
		{
			if ( hasDirectories )
				scripts.add( new JSeparator() );

			for ( int i = 0; i < scriptList.length; ++i )
				if ( !scriptList[i].isDirectory() )
					scripts.add( scriptList[i] );
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings
	 * file.  This should be called after every update.
	 */

	protected static void saveBookmarks()
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

	protected static void compileBookmarks()
	{
		bookmarks.clear();
		String [] bookmarkData = StaticEntity.getProperty( "browserBookmarks" ).split( "\\|" );

		if ( bookmarkData.length > 1 )
			for ( int i = 0; i < bookmarkData.length; ++i )
				bookmarks.add( bookmarkData[i] + "|" + bookmarkData[++i] + "|" + bookmarkData[++i] );
	}
}
