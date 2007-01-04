/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.ItemManagePanel.TransferListener;

public abstract class KoLFrame extends JFrame implements KoLConstants
{
	public static final TradeableItemFilter TRADE_FILTER = new TradeableItemFilter();

	public JTabbedPane tabs = null;
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
	}

	public final void addTab( String name, JComponent panel )
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

	public final JToolBar getToolbar()
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

		if ( !(this instanceof LoginFrame) && existingFrames.isEmpty() && !StaticEntity.getGlobalProperty( "initialFrames" ).equals( "LocalRelayServer" ) && StaticEntity.getClient() instanceof KoLmafiaGUI )
			(new RestartThread()).start();
	}

	private class RestartThread extends Thread
	{
		public void run()
		{
			if ( !KoLmafia.executedLogin() || StaticEntity.getIntegerProperty( "closeLastFrameAction" ) == 1 )
				System.exit(0);

			createDisplay( LoginFrame.class );
			StaticEntity.getClient().makeRequest( new LogoutRequest() );
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

		refresher = new StatusRefresher();
		refresher.run();

		refreshListener = new KoLCharacterAdapter( refresher );
		KoLCharacter.addCharacterListener( refreshListener );

		refresher.getCompactPane().setBackground( ENABLED_COLOR );
		getContentPane().add( refresher.getCompactPane(), BorderLayout.WEST );
	}

	public static class StatusRefresher implements Runnable
	{
		private JPanel compactPane;
		private JPanel levelPanel;
		private JProgressBar levelMeter;
		private JLabel levelLabel, roninLabel, mcdLabel;
		private JLabel musLabel, mysLabel, moxLabel, drunkLabel;
		private JLabel hpLabel, mpLabel, meatLabel, advLabel;
		private JLabel familiarLabel;
		private JLabel mlLabel, combatLabel, initLabel;
		private JLabel xpLabel, meatDropLabel, itemDropLabel;

		public StatusRefresher()
		{
			JPanel [] panels = new JPanel[5];
			int panelCount = -1;

			panels[ ++panelCount ] = new JPanel( new BorderLayout() );
			levelPanel = panels[0];

			panels[ panelCount ].add( levelLabel = new JLabel( " ", JLabel.CENTER ), BorderLayout.NORTH );
			panels[ panelCount ].add( levelMeter = new JProgressBar(), BorderLayout.CENTER );
			levelMeter.setOpaque( true );
			levelMeter.setStringPainted( true );
			JComponentUtilities.setComponentSize( levelMeter, 40, 6 );
			panels[ panelCount ].add( Box.createHorizontalStrut( 10 ), BorderLayout.WEST );
			panels[ panelCount ].add( Box.createHorizontalStrut( 10 ), BorderLayout.EAST );
			panels[ panelCount ].setOpaque( false );

			JPanel holderPanel = new JPanel( new GridLayout( 2, 1 ) );
			holderPanel.add( roninLabel = new JLabel( " ", JLabel.CENTER ) );
			holderPanel.add( mcdLabel = new JLabel( " ", JLabel.CENTER ) );
			holderPanel.setOpaque( false );
			panels[ panelCount ].add( holderPanel, BorderLayout.SOUTH );

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

		public String getStatText( int adjusted, int base )
		{
			return adjusted == base ? "<html>" + Integer.toString( base ) :
				adjusted >  base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" :
				"<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
		}

		public void run()
		{
			levelLabel.setText( "Level " + KoLCharacter.getLevel() );
			roninLabel.setText( KoLCharacter.isHardcore() ? "(Hardcore)" : KoLCharacter.getTotalTurnsUsed() >= 600 ? "(Ronin Clear)" :
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

			int currentLevel = KoLCharacter.calculateLastLevel();
			int nextLevel = KoLCharacter.calculateNextLevel();
			int totalPrime = KoLCharacter.getTotalPrime();

			levelMeter.setMaximum( nextLevel - currentLevel );
			levelMeter.setValue( totalPrime - currentLevel );
			levelMeter.setString( " " );

			levelPanel.setToolTipText( "<html>&nbsp;&nbsp;" + KoLCharacter.getAdvancement() + "&nbsp;&nbsp;<br>&nbsp;&nbsp;(" +
				COMMA_FORMAT.format( nextLevel - totalPrime ) + " subpoints needed)&nbsp;&nbsp;</html>" );

			FamiliarData familiar = KoLCharacter.getFamiliar();
			int id = familiar == null ? -1 : familiar.getId();

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

		public JPanel getCompactPane()
		{	return compactPane;
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

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	public static class DisplayFrameButton extends ActionButton
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

		public void actionPerformed( ActionEvent e )
		{	KoLmafiaGUI.constructFrame( frameClass );
		}
	}

	public static abstract class ActionButton extends JButton implements ActionListener
	{
		public ActionButton( String text )
		{
			super( text );
			addActionListener( this );
		}

		public ActionButton( ImageIcon icon )
		{
			super( icon );
			addActionListener( this );
		}
	}

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	public static class InvocationButton extends ActionButton
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

		public void actionPerformed( ActionEvent e )
		{
			try
			{
				if ( method != null )
					method.invoke( object, null );
			}
			catch ( Exception e1 )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e1 );
			}
		}
	}

	/**
	 * An internal class used to handle requests to open a new frame
	 * using a local panel inside of the adventure frame.
	 */

	public static class KoLPanelFrameButton extends ActionButton
	{
		public Object [] parameters;

		public KoLPanelFrameButton( String tooltip, String icon, ActionPanel panel )
		{
			super( JComponentUtilities.getImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			setToolTipText( tooltip );

			parameters = new Object[2];
			parameters[0] = tooltip;
			parameters[1] = panel;
		}

		public void actionPerformed( ActionEvent e )
		{	createDisplay( KoLPanelFrame.class, parameters );
		}
	}

	public static class RequestButton extends ActionButton
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
			setToolTipText( title );
			this.request = request;
		}

		public void actionPerformed( ActionEvent e )
		{	StaticEntity.getClient().makeRequest( request );
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

		return StaticEntity.parseInt( field.getText().trim() );
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
		return desiredValue <= 0 ? maximumValue - desiredValue : Math.min( desiredValue, maximumValue );
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

		if ( isVisible )
		{
			super.setExtendedState( NORMAL );
			super.repaint();
		}
	}

	public class KoLHyperlinkAdapter extends HyperlinkAdapter
	{
		public void handleInternalLink( String location )
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

		if ( !isVisible() )
			restorePosition();
	}

	private void rememberPosition()
	{
		Point p = getLocation();
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

	public class LoadScriptButton extends ActionButton
	{
		private String scriptPath;

		public LoadScriptButton( int scriptId, String scriptPath )
		{
			super( String.valueOf( scriptId ) );

			this.scriptPath = scriptPath;
			setToolTipText( scriptPath );

			JComponentUtilities.setComponentSize( this, 30, 30 );
		}

		public void actionPerformed( ActionEvent e )
		{	DEFAULT_SHELL.executeLine( scriptPath );
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
			TableColumnModel columnModel = table.getColumnModel();

		    int row = e.getY() / table.getRowHeight();
		    int column = columnModel.getColumnIndexAtX( e.getX() );

			if ( row >= 0 && row < table.getRowCount() && column >= 0 && column < table.getColumnCount() )
			{
				Object value = table.getValueAt( row, column );

				if ( value instanceof JButton )
				{
					MouseEvent event = SwingUtilities.convertMouseEvent( table, e, (JButton) value );
					((JButton)value).dispatchEvent( event );
					table.repaint();
				}
			}
		}
	}

	public abstract class NestedInsideTableButton extends JButton implements MouseListener
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

	public abstract class ListWrapperTableModel extends DefaultTableModel implements ListDataListener
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

		public abstract Vector constructVector( Object o );

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

			if ( editCellAt( row, column ) )
			{
				getEditorComponent().requestFocusInWindow();
				if ( getEditorComponent() instanceof JTextField )
					((JTextField)getEditorComponent()).selectAll();
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
		{
			super( elementModel );

			boolean isCloset = (elementModel == closet);

			setButtons( new ActionListener [] {

				new ConsumeListener(),
				new PutInClosetListener( isCloset ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOSELL ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOMALL ),
				new PulverizeListener( isCloset ),
				new PutOnDisplayListener( isCloset ),
				new GiveToClanListener( isCloset )

			} );

			if ( !isCloset )
				eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );
		}

		private class RefreshButton extends RequestButton
		{
			public RefreshButton()
			{	super( "refresh", new EquipmentRequest( EquipmentRequest.CLOSET ) );
			}

			public void actionPerformed( ActionEvent e )
			{
				super.actionPerformed( e );
				refreshFilter();
			}
		}


		private class ConsumeListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				Object [] items = getDesiredItems( "Consume" );
				if ( items.length == 0 )
					return;

				for ( int i = 0; i < items.length; ++i )
				{
					int usageType = TradeableItemDatabase.getConsumptionType( ((AdventureResult)items[i]).getItemId() );

					switch ( usageType )
					{
					case ConsumeItemRequest.NO_CONSUME:
						break;

					case ConsumeItemRequest.EQUIP_FAMILIAR:
					case ConsumeItemRequest.EQUIP_ACCESSORY:
					case ConsumeItemRequest.EQUIP_HAT:
					case ConsumeItemRequest.EQUIP_PANTS:
					case ConsumeItemRequest.EQUIP_SHIRT:
					case ConsumeItemRequest.EQUIP_WEAPON:
					case ConsumeItemRequest.EQUIP_OFFHAND:
						StaticEntity.getClient().makeRequest( new EquipmentRequest( (AdventureResult) items[i], KoLCharacter.consumeFilterToEquipmentType( usageType ) ) );
						break;

					default:
						StaticEntity.getClient().makeRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
						break;
					}
				}
			}

			public String toString()
			{	return "use item";
			}
		}

		public class PutInClosetListener extends TransferListener
		{
			public PutInClosetListener( boolean retrieveFromClosetFirst )
			{	super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				if ( !retrieveFromClosetFirst )
					StaticEntity.getClient().makeRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
			}

			public String toString()
			{	return retrieveFromClosetFirst ? "inventory" : "closet";
			}
		}

		public class AutoSellListener extends TransferListener
		{
			private int sellType;

			public AutoSellListener( boolean retrieveFromClosetFirst, int sellType )
			{
				super( sellType == AutoSellRequest.AUTOSELL ? "Autoselling" : "Automalling", retrieveFromClosetFirst );
				this.sellType = sellType;
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( sellType == AutoSellRequest.AUTOMALL && !KoLCharacter.hasStore() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You don't own a store in the mall.");
					return;
				}

				if ( sellType == AutoSellRequest.AUTOSELL && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
					"Are you sure you would like to sell the selected items?",
						"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
							return;

				if ( sellType == AutoSellRequest.AUTOMALL && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
					"Are you sure you would like to place the selected items in your store?",
						"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
							return;

				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				StaticEntity.getClient().makeRequest( new AutoSellRequest( items, sellType ) );
			}

			public String toString()
			{	return sellType == AutoSellRequest.AUTOSELL ? "auto sell" : "place in mall";
			}
		}

		public class GiveToClanListener extends TransferListener
		{
			public GiveToClanListener( boolean retrieveFromClosetFirst )
			{	super( "Stashing", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				StaticEntity.getClient().makeRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
			}

			public String toString()
			{	return "clan stash";
			}
		}

		public class PutOnDisplayListener extends TransferListener
		{
			public PutOnDisplayListener( boolean retrieveFromClosetFirst )
			{	super( "Showcasing", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = initialSetup();
				if ( items == null )
					return;

				if ( !KoLCharacter.hasDisplayCase() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You don't own a display case in the Cannon Museum.");
					return;
				}

				StaticEntity.getClient().makeRequest( new MuseumRequest( items, true ) );
			}

			public String toString()
			{	return "display case";
			}
		}

		public class PulverizeListener extends TransferListener
		{
			public PulverizeListener( boolean retrieveFromClosetFirst )
			{	super( "Smashing", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null || items.length == 0 )
					return;

				for ( int i = 0; i < items.length; ++i )
				{
					boolean willSmash = TradeableItemDatabase.isTradeable( items[i].getItemId() ) ||
						JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
							items[i].getName() + " is untradeable.  Are you sure?", "Smash request nag screen!", JOptionPane.YES_NO_OPTION );

					if ( willSmash )
						StaticEntity.getClient().makeRequest( new PulverizeRequest( items[i] ) );
				}
			}

			public String toString()
			{	return "pulverize";
			}
		}
	}
}
