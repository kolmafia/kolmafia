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
import javax.swing.JToolBar;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;

// layout
import java.awt.Point;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.table.TableColumnModel;

// event listeners
import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// basic utilities
import java.io.FileInputStream;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

// other stuff
import javax.swing.SwingUtilities;
import java.lang.ref.WeakReference;

// spellcast imports
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends JFrame implements KoLConstants
{
	protected JTabbedPane tabs = null;
	protected String lastTitle;
	protected String frameName;
	protected JPanel framePanel;

	protected StatusRefresher refresher = null;
	protected KoLCharacterAdapter refreshListener = null;

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given StaticEntity.getClient().
	 */

	protected KoLFrame()
	{	this( "" );
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

		if ( !(this instanceof KoLDesktop) )
			existingFrames.add( this );
	}

	public final void setTitle( String newTitle )
	{
		this.lastTitle = newTitle;
		KoLDesktop.setTitle( this, newTitle );

		super.setTitle( KoLCharacter.getUsername().length() > 0 ?
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

		switch ( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "toolbarPosition" ) ) )
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
	
		super.dispose();

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		existingFrames.remove( this );
		KoLDesktop.removeTab( this );

		if ( refreshListener != null )
			KoLCharacter.removeCharacterListener( refreshListener );

		// If the list of frames is now empty, make sure
		// you end the session.  Ending the session for
		// a login frame involves exiting, and ending the
		// session for all other frames is calling main.

		if ( existingFrames.isEmpty() && StaticEntity.getClient() instanceof KoLmafiaGUI )
		{
			KoLMessenger.dispose();
			StaticEntity.closeSession();

			if ( this instanceof LoginFrame )
			{
				SystemTrayFrame.removeTrayIcon();
				System.exit(0);
			}
			else
				KoLmafiaGUI.main( new String[0] );
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

		boolean useTextOnly = GLOBAL_SETTINGS.getProperty( "useTextHeavySidepane" ).equals( "true" );

		refresher = new StatusRefresher( useTextOnly );
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
			JPanel [] panels = new JPanel[4];
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
			refreshPanel.add( new RequestButton( "Refresh Status", "refresh.gif", new CharsheetRequest( StaticEntity.getClient() ) ) );

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

			hpLabel.setText( df.format( KoLCharacter.getCurrentHP() ) + "/" + df.format( KoLCharacter.getMaximumHP() ) );
			mpLabel.setText( df.format( KoLCharacter.getCurrentMP() ) + "/" + df.format( KoLCharacter.getMaximumMP() ) );
			meatLabel.setText( df.format( KoLCharacter.getAvailableMeat() ) );
			advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );
		}

		protected void updateGraphical()
		{
			hpLabel.setText( KoLCharacter.getCurrentHP() + " / " + KoLCharacter.getMaximumHP() );
			hpLabel.setVerticalTextPosition( JLabel.BOTTOM );
			hpLabel.setHorizontalTextPosition( JLabel.CENTER );

			mpLabel.setText( KoLCharacter.getCurrentMP() + " / " + KoLCharacter.getMaximumMP() );
			mpLabel.setVerticalTextPosition( JLabel.BOTTOM );
			mpLabel.setHorizontalTextPosition( JLabel.CENTER );

			meatLabel.setText( df.format( KoLCharacter.getAvailableMeat() ) );
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

			case ABORT_STATE:
			case CONTINUE_STATE:

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
			enclosingPanel.add( new JScrollPane( elementList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

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
			elementList.setEnabled( isEnabled );
			for ( int i = 0; i < buttons.length; ++i )
				buttons[i].setEnabled( isEnabled );

			for ( int i = 0; i < movers.length; ++i )
				movers[i].setEnabled( isEnabled );
		}

		protected Object [] getDesiredItems( String message )
		{
			return KoLFrame.this.getDesiredItems( elementList, message,
				movers[0].isSelected() ? TAKE_ALL : movers[1].isSelected() ? TAKE_ALL_BUT_ONE :
				movers[2].isSelected() ? TAKE_MULTIPLE : TAKE_ONE );
		}

		protected void filterSelection( boolean eat, boolean drink, boolean other, boolean sell, boolean trade )
		{
			Object [] elements = elementList.getSelectedValues();
			for ( int i = 0; i < elements.length; ++i )
			{
				int actualIndex = ((LockableListModel)elementList.getModel()).indexOf( elements[i] );
				switch ( TradeableItemDatabase.getConsumptionType( ((AdventureResult)elements[i]).getName() ) )
				{
				case ConsumeItemRequest.CONSUME_EAT:

					if ( !eat )
						elementList.removeSelectionInterval( actualIndex, actualIndex );

					break;

				case ConsumeItemRequest.CONSUME_DRINK:

					if ( !drink )
						elementList.removeSelectionInterval( actualIndex, actualIndex );

					break;

				default:

					if ( !other )
						elementList.removeSelectionInterval( actualIndex, actualIndex );

					break;
				}


				int autoSellValue = TradeableItemDatabase.getPriceByID( ((AdventureResult)elements[i]).getItemID() );

				if ( !sell && ( autoSellValue == 0 || autoSellValue == -1 ) )
					elementList.removeSelectionInterval( actualIndex, actualIndex );

				if ( !trade && ( autoSellValue == 0 || autoSellValue < -1 ) )
					elementList.removeSelectionInterval( actualIndex, actualIndex );
			}
		}
	}

	protected static final int TAKE_ALL = 1;
	protected static final int TAKE_ALL_BUT_ONE = 2;
	protected static final int TAKE_MULTIPLE = 3;
	protected static final int TAKE_ONE = 4;

	protected Object [] getDesiredItems( JList elementList, String message, int quantityType )
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

		// If none of the array entries were nulled,
		// then return the array as-is.

		if ( neededSize == items.length )
			return items;

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		Object [] desiredItems = new Object[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
			if ( items[i] != null )
				desiredItems[ neededSize++ ] = items[i];

		return desiredItems;
	}
	
	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	protected static class DisplayFrameButton extends JButton implements ActionListener
	{
		private Class frameClass;

		public DisplayFrameButton( String text, Class frameClass )
		{
			super( text );

			addActionListener( this );
			this.frameClass = frameClass;
		}

		public DisplayFrameButton( String tooltip, String icon, Class frameClass )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			setToolTipText( tooltip );

			addActionListener( this );
			this.frameClass = frameClass;
		}

		public void actionPerformed( ActionEvent e )
		{	SwingUtilities.invokeLater( new CreateFrameRunnable( frameClass ) );
		}
	}

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	protected static class InvocationButton extends JButton implements ActionListener, Runnable
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
			addActionListener( this );

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

		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( this )).start();
		}

		public void run()
		{
			try
			{
				if ( method != null )
					method.invoke( object, null );
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

	protected static class KoLPanelFrameButton extends JButton implements ActionListener
	{
		protected Object [] parameters;

		public KoLPanelFrameButton( String tooltip, String icon, ActionPanel panel )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			setToolTipText( tooltip );
			addActionListener( this );

			parameters = new Object[3];
			parameters[0] = StaticEntity.getClient();
			parameters[1] = tooltip;
			parameters[2] = panel;
		}

		public void actionPerformed( ActionEvent e )
		{	SwingUtilities.invokeLater( new CreateFrameRunnable( KoLPanelFrame.class, parameters ) );
		}
	}

	protected static class RequestButton extends JButton implements ActionListener
	{
		protected KoLRequest request;

		public RequestButton( String title, KoLRequest request )
		{
			super( title );
			this.request = request;
			addActionListener( this );
		}

		public RequestButton( String title, String icon, KoLRequest request )
		{
			super( JComponentUtilities.getImage( icon ) );
			setToolTipText( title );
			this.request = request;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( request )).start();
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
		try
		{
			String currentValue = field.getText();

			if ( currentValue == null || currentValue.length() == 0 )
				return defaultValue;

			if ( currentValue.equals( "*" ) )
				return defaultValue;

			return df.parse( field.getText().trim() ).intValue();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
			return 0;
		}
	}

	protected static final int getQuantity( String title, int maximumValue, int defaultValue )
	{
		// Check parameters; avoid programmer error.
		if ( defaultValue > maximumValue )
			return 0;

		if ( maximumValue == 1 && maximumValue == defaultValue )
			return 1;

		try
		{
			String currentValue = JOptionPane.showInputDialog( title, df.format( defaultValue ) );
			if ( currentValue == null )
				return 0;

			if ( currentValue.equals( "*" ) )
				return maximumValue;

			int desiredValue = df.parse( currentValue ).intValue();
			return Math.max( 0, Math.min( desiredValue, maximumValue ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
			return 0;
		}
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
		if ( e.getID() == WindowEvent.WINDOW_CLOSING )
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

	protected final void setProperty( String name, String value )
	{	StaticEntity.setProperty( name, value );
	}

	protected final String getProperty( String name )
	{	return StaticEntity.getProperty( name );
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

				restoreSetting.append( restoreCheckbox[i].getText() );
			}
		}

		return restoreSetting.toString();
	}

	protected JScrollPane constructScroller( JCheckBox [] restoreCheckbox )
	{
		JPanel checkboxPanel = new JPanel( new GridLayout( restoreCheckbox.length, 1 ) );
		for ( int i = 0; i < restoreCheckbox.length; ++i )
			checkboxPanel.add( restoreCheckbox[i] );

		JScrollPane scrollArea = new JScrollPane( checkboxPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		return scrollArea;
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
			GLOBAL_SETTINGS.setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
		else
			StaticEntity.getSettings().setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
	}

	private void restorePosition()
	{
		int xLocation = 0;
		int yLocation = 0;
		Dimension screenSize = TOOLKIT.getScreenSize();
		if ( StaticEntity.getSettings().containsKey( frameName ) )
		{
			String [] location = StaticEntity.getSettings().getProperty( frameName ).split( "," );
			xLocation = Integer.parseInt( location[0] );
			yLocation = Integer.parseInt( location[1] );
		}
		if ( xLocation > 0 && yLocation > 0 && xLocation < screenSize.getWidth() && yLocation < screenSize.getHeight() )
			setLocation( xLocation, yLocation );
		else
			setLocationRelativeTo( null );
	}

	public static boolean executesConflictingRequest()
	{	return false;
	}
}
