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

// layout
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// containers
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

// other imports
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.Method;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.text.DecimalFormat;
import java.text.ParseException;

import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class HagnkStorageFrame extends KoLFrame
{
	private ItemWithdrawPanel items;
	private MeatWithdrawPanel meats;

	public HagnkStorageFrame( KoLmafia client )
	{
		super( "KoLmafia: Hagnk, the Secret Dwarf", client );

		this.meats = new MeatWithdrawPanel();
		this.items = new ItemWithdrawPanel();

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout() );
		centerPanel.add( meats, BorderLayout.NORTH );
		centerPanel.add( items, BorderLayout.CENTER );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( centerPanel, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( items != null )
			items.setEnabled( isEnabled );
		if ( meats != null )
			meats.setEnabled( isEnabled );
	}

	/**
	 * An internal class which represents the panel used for meatss to
	 * the clan coffer.
	 */

	private class MeatWithdrawPanel extends NonContentPanel
	{
		private JTextField amountField;

		public MeatWithdrawPanel()
		{
			super( "withdraw meat", "toss dwarf", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			amountField = new JTextField();
			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Amount: ", amountField );
			setContent( elements );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new ItemStorageRequest( client, ItemStorageRequest.PULL_MEAT_FROM_STORAGE, getValue( amountField ) ) )).start();
		}

		protected void actionCancelled()
		{	JOptionPane.showMessageDialog( null, "Hagnk's actually a gnome.  Tosser." );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class ItemWithdrawPanel extends ItemManagePanel
	{
		public ItemWithdrawPanel()
		{
			super( "Inside Storage", "put in bag", "put in closet", client == null ? new LockableListModel() : client.getStorage() );

			if ( client.getStorage().isEmpty() )
				(new ItemStorageRequest( client )).run();

		}

		protected void actionConfirmed()
		{	(new ItemWithdrawThread( false )).start();
		}

		protected void actionCancelled()
		{	(new ItemWithdrawThread( true )).start();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually move items around in the inventory.
		 */

		private class ItemWithdrawThread extends RequestThread
		{
			private boolean isCloset;

			public ItemWithdrawThread( boolean isCloset )
			{	this.isCloset = isCloset;
			}

			public void run()
			{
				Object [] items = elementList.getSelectedValues();
				AdventureResult selection;
				int itemID, quantity;

				try
				{
					for ( int i = 0; i < items.length; ++i )
					{
						selection = (AdventureResult) items[i];
						itemID = selection.getItemID();
						quantity = df.parse( JOptionPane.showInputDialog(
							"Retrieving " + selection.getName() + " from the storage...", String.valueOf( selection.getCount() ) ) ).intValue();

						items[i] = new AdventureResult( itemID, quantity );
					}
				}
				catch ( Exception e )
				{
					// If an exception occurs somewhere along the way
					// then return from the thread.

					return;
				}

				client.makeRequest( new ItemStorageRequest( client, ItemStorageRequest.STORAGE_TO_INVENTORY, items ), 1 );

				if ( isCloset )
					client.makeRequest( new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, items ), 1 );
			}
		}
	}

	public static void main( String [] args )
	{
		KoLFrame uitest = new HagnkStorageFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
