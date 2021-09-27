package net.sourceforge.kolmafia.swingui;

import java.awt.Component;
import java.awt.GridLayout;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLGUIConstants;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

import net.sourceforge.kolmafia.utilities.HTMLListEntry;

public class ContactListFrame
	extends GenericFrame
{
	private final LockableListModel contacts;
	private final JList contactsDisplay;

	public ContactListFrame()
	{
		this( ContactManager.getMailContacts() );
	}

	public ContactListFrame( final LockableListModel contacts )
	{
		super( "Contact List" );

		this.contacts = contacts;

		this.contactsDisplay = new JList( contacts );
		this.contactsDisplay.setVisibleRowCount( 25 );
		this.contactsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
		this.contactsDisplay.addMouseListener( new SendInstantMessageAdapter() );

		this.setCenterComponent( new ContactListPanel( this.contactsDisplay ) );
		this.getToolbar();

		// Choose an appropriate size based on what is in the contact list
		this.pack();
	}

	@Override
	public JToolBar getToolbar()
	{
		JToolBar toolbarPanel = super.getToolbar( true );
		toolbarPanel.add( new InvocationButton( "Show as list", "copy.gif", this, "listSelected" ) );
		toolbarPanel.add( new InvocationButton( "Mass-buff", "buff.gif", this, "buffSelected" ) );
		toolbarPanel.add( new InvocationButton( "Mass-mail", "mail.gif", this, "mailSelected" ) );

		return toolbarPanel;
	}

	@Override
	public Component getCenterComponent()
	{
		return this.getFramePanel();
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public String[] getSelectedPlayers()
	{
		Object[] selectedValues = this.contactsDisplay.getSelectedValuesList().toArray();

		// If no players are selected, and the player uses the
		// option, assume they want everyone.

		if ( selectedValues.length == 0 )
		{
			selectedValues = this.contacts.toArray();
		}

		String[] selectedPlayers = new String[ selectedValues.length ];

		for ( int i = 0; i < selectedPlayers.length; ++i )
		{
			selectedPlayers[ i ] = getContactName( selectedValues[ i ] );
		}

		return selectedPlayers;
	}

	public String convertToCDL()
	{
		StringBuffer listCDL = new StringBuffer();
		String[] selectedPlayers = this.getSelectedPlayers();

		for ( int i = 0; i < selectedPlayers.length; ++i )
		{
			if ( i != 0 )
			{
				listCDL.append( ", " );
			}

			listCDL.append( selectedPlayers[ i ] );
		}

		return listCDL.toString();
	}

	public void listSelected()
	{
		JDialog dialogCDL = new JDialog( (java.awt.Frame) null, "Here's your CDL!" );
		JTextArea entryCDL = new JTextArea();

		entryCDL.setFont( KoLGUIConstants.DEFAULT_FONT );
		entryCDL.setLineWrap( true );
		entryCDL.setWrapStyleWord( true );

		GenericScrollPane scrollCDL = new GenericScrollPane( entryCDL );
		JComponentUtilities.setComponentSize( scrollCDL, 250, 120 );
		dialogCDL.getContentPane().add( scrollCDL );

		entryCDL.setText( this.convertToCDL() );
		dialogCDL.pack();
		dialogCDL.setVisible( true );
	}

	public void buffSelected()
	{
		Object[] parameters = new Object[ 1 ];
		parameters[ 0 ] = this.convertToCDL();

		GenericFrame.createDisplay( SkillBuffFrame.class, parameters );
	}

	public void mailSelected()
	{
		// Make sure there's only eleven players
		// selected, since that's the kmail limit.

		Object[] parameters = new Object[ 1 ];
		parameters[ 0 ] = this.convertToCDL();

		GenericFrame.createDisplay( SendMessageFrame.class, parameters );
	}

	private class ContactListPanel
		extends JPanel
	{
		public ContactListPanel( JList contactsDisplay )
		{
			this.setLayout( new GridLayout( 1, 1 ) );
			this.add( new GenericScrollPane( contactsDisplay ) );
		}
	}

	private class SendInstantMessageAdapter
		extends MouseAdapter
	{
		@Override
		public void mouseClicked( final MouseEvent e )
		{
			// The only event handled by the adapter is a double-click;
			// when a double-click is detected, a new ChatFrame is created
			// for the specified player(s).

			if ( e.getClickCount() == 2 )
			{
				String[] selectedPlayers = getSelectedPlayers();
				String bufferKey;

				for ( String contact : selectedPlayers )
				{
					bufferKey = ChatManager.getBufferKey( contact );
					ChatManager.openWindow( bufferKey, false );
				}
			}
		}
	}

	protected String getContactName( Object contact )
	{
		if ( contact instanceof HTMLListEntry )
		{
			return ( (HTMLListEntry) contact ).getValue();
		}

		return (String) contact;
	}
}
