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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;

// event listeners
import javax.swing.ListSelectionModel;

// containers
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.BorderFactory;

// other imports
import java.text.ParseException;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class ItemManageFrame extends KoLFrame
{
	public ItemManageFrame( KoLmafia client )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
			" Item Management", client );

		JTabbedPane inventoryTabs = new JTabbedPane();
		inventoryTabs.addTab( "Use", new JPanel() );
		inventoryTabs.addTab( "Equip", new JPanel() );
		inventoryTabs.addTab( "Sell", new JPanel() );
		inventoryTabs.addTab( "Store", new JPanel() );
		inventoryTabs.addTab( "Create", new ItemCreationPanel() );
		inventoryTabs.addTab( "Meat Paste", new MeatManagementPanel() );
		inventoryTabs.addTab( "Execute", new JPanel() );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( inventoryTabs, "" );
		addWindowListener( new ReturnFocusAdapter() );
	}

	public void setStatusMessage( String s )
	{
	}

	public void clear()
	{	requestFocus();
	}

	protected void actionConfirmed()
	{
	}

	protected void actionCancelled()
	{
	}

	/**
	 * An internal class used to handle item creation for
	 * the inventory screen.
	 */

	private class ItemCreationPanel extends KoLPanel
	{
		private JComboBox createField;
		private JTextField countField;

		public ItemCreationPanel()
		{
			super( "add", "cancel", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			createField = new JComboBox();
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Result: ", createField );
			elements[1] = new VerifiableElement( "Quantity: ", countField );

			setContent( elements );
		}

		public void setStatusMessage( String s )
		{
		}

		public void clear()
		{
			createField.setSelectedIndex( 0 );
			countField.setText( "" );
		}

		protected void actionConfirmed()
		{
		}

		protected void actionCancelled()
		{
		}

		public void requestFocus()
		{
			super.requestFocus();
			createField.requestFocus();
		}
	}

	/**
	 * An internal class used to handle meat management for
	 * the inventory screen.
	 */

	private class MeatManagementPanel extends KoLPanel
	{
		private JComboBox actionField;
		private JTextField countField;
		private LockableListModel actions;

		public MeatManagementPanel()
		{
			super( "add", "cancel", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			LockableListModel actions = new LockableListModel();
			actions.add( "Deposit Meat" );
			actions.add( "Withdraw Meat" );
			actions.add( "Make Meat Paste" );
			actions.add( "Make Meat Stack" );
			actions.add( "Make Dense Stack" );

			actionField = new JComboBox( actions );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Action: ", actionField );
			elements[1] = new VerifiableElement( "Amount: ", countField );

			setContent( elements );
		}

		public void setStatusMessage( String s )
		{
		}

		public void clear()
		{
			actionField.setSelectedIndex( 0 );
			countField.setText( "" );
			requestFocus();
		}

		protected void actionConfirmed()
		{
			try
			{
				int quantity = df.parse( countField.getText() ).intValue();

				switch ( actionField.getSelectedIndex() )
				{
					case 0:
						break;
					case 1:
						break;
					case 2:
						break;
					case 3:
						break;
					case 4:
						break;
				}
			}
			catch ( ParseException e )
			{
				// If the number placed inside of the count list was not
				// an actual integer value, pretend nothing happened.
				// Using exceptions for flow control is bad style, but
				// this will be fixed once we add functionality.
			}
		}

		protected void actionCancelled()
		{
		}

		public void requestFocus()
		{
			super.requestFocus();
			actionField.requestFocus();
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new ItemManageFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
