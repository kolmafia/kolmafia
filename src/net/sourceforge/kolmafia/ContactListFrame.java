package net.sourceforge.kolmafia;

import java.awt.Color;
import java.awt.CardLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of a generic <code>JFrame</code> which contains the buddy list
 * for this KoLmafia session.
 */

public class ContactListFrame extends JFrame
{
	private KoLmafia client;
	private SortedListModel contacts;

	public ContactListFrame( KoLmafia client, SortedListModel contacts )
	{
		super( "KoLIM: " + client.getLoginName() );

		this.client = client;
		this.contacts = contacts;
		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( new ContactListPanel(), "" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	private class ContactListPanel extends JPanel
	{
		public ContactListPanel()
		{
			setLayout( new GridLayout( 1, 1 ) );
			JList contactsDisplay = new JList( contacts );
			contactsDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			contactsDisplay.addMouseListener( new SendInstantMessageAdapter() );

			add( new JScrollPane( contactsDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ) );
		}
	}

	private class NotifyMessengerAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{	client.getMessenger().notifyContactListClosed();
		}
	}

	private class SendInstantMessageAdapter extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			JList list = (JList) e.getSource();

			// The only event handled by the adapter is a double-click;
			// when a double-click is detected, a new ChatFrame is created
			// for the specified player.

			if ( e.getClickCount() == 2 )
			{
				int index = list.locationToIndex( e.getPoint() );

				if ( index >= 0 && index < contacts.size() )
				{
					String contactName = (String) contacts.get( index );
					client.getMessenger().openInstantMessage( contactName );
				}
			}
		}
	}
}
