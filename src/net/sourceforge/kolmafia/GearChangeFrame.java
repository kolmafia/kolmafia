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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.BorderLayout;

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;

// utilities
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extension of <code>KoLFrame</code> used to display the character
 * sheet for the current user.  Note that this can only be instantiated
 * when the character is logged in; if the character has logged out,
 * this method will contain blank data.  Note also that the avatar that
 * is currently displayed will be the default avatar from the class and
 * will not reflect outfits or customizations.
 */

public class GearChangeFrame extends KoLFrame
{
	private boolean isChanging = false;
	private KoLCharacter characterData;
	private JComboBox [] equipment;
	private JComboBox outfitSelect, familiarSelect;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 *
	 * @param	client	The client containing the data associated with the character
	 */

	public GearChangeFrame( KoLmafia client )
	{
		super( client, "KoLmafia: Changing Gears" );

		// For now, because character listeners haven't been implemented
		// yet, re-request the character sheet from the server

		if ( client != null )
			characterData = client.getCharacterData();
		else
			characterData = new KoLCharacter( "UI Test" );

		setResizable( false );
		contentPanel = null;

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		getContentPane().add( createEquipPanel(), "" );
		refreshEquipPanel();
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu( "Refresh" );
		fileMenu.setMnemonic( KeyEvent.VK_R );
		menuBar.add( fileMenu );

		fileMenu.add( new RefreshMenuItem( "Equipment", KeyEvent.VK_E, new EquipmentRequest( client, EquipmentRequest.EQUIPMENT ) ) );
		fileMenu.add( new RefreshMenuItem( "Familiars", KeyEvent.VK_F, new FamiliarRequest( client ) ) );

		addHelpMenu( menuBar );
	}

	/**
	 * Sets all of the internal panels to a disabled or enabled state; this
	 * prevents the user from modifying the data as it's getting sent, leading
	 * to uncertainty and generally bad things.
	 */

	public void setEnabled( boolean isEnabled )
	{
		if ( equipment != null )
			for ( int i = 0; i < equipment.length; ++i )
				equipment[i].setEnabled( isEnabled );

		if ( outfitSelect != null )
			outfitSelect.setEnabled( isEnabled );

		if ( familiarSelect != null )
			familiarSelect.setEnabled( isEnabled );
	}

	/**
	 * Utility method for creating a panel displaying the character's current
	 * equipment, accessories and familiar item.
	 *
	 * @return	a <code>JPanel</code> displaying the character's equipment
	 */

	private JPanel createEquipPanel()
	{
		JPanel fieldPanel = new JPanel();
		fieldPanel.setLayout( new GridLayout( 14, 1 ) );

		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Hat:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Weapon:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Shirt:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Pants:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Familiar:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Item:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Outfit:  ", JLabel.RIGHT ) );

		JPanel valuePanel = new JPanel();
		valuePanel.setLayout( new GridLayout( 14, 1 ) );

		valuePanel.add( new JLabel( " " ) );

		equipment = new JComboBox[8];
		LockableListModel [] equipmentLists = characterData.getEquipmentLists();

		for ( int i = 0; i < 8; ++i )
		{
			equipment[i] = new JComboBox( equipmentLists[i] );
			equipment[i].addActionListener( new ChangeListener( equipment[i], EquipmentRequest.class, String.class, new Integer(i) ) );
			JComponentUtilities.setComponentSize( equipment[i], 240, 20 );

			if ( i != KoLCharacter.FAMILIAR )
				valuePanel.add( equipment[i] );
		}

		valuePanel.add( new JLabel( " " ) );

		familiarSelect = new JComboBox( characterData.getFamiliars() );
		familiarSelect.addActionListener( new ChangeListener( familiarSelect, FamiliarRequest.class, FamiliarData.class ) );
		JComponentUtilities.setComponentSize( familiarSelect, 240, 20 );
		valuePanel.add( familiarSelect );

		valuePanel.add( equipment[KoLCharacter.FAMILIAR] );
		valuePanel.add( new JLabel( " " ) );

		outfitSelect = new JComboBox( characterData.getOutfits() );
		outfitSelect.addActionListener( new ChangeListener( outfitSelect, EquipmentRequest.class, SpecialOutfit.class ) );
		JComponentUtilities.setComponentSize( outfitSelect, 240, 20 );
		valuePanel.add( outfitSelect );

		JPanel equipPanel = new JPanel();
		equipPanel.setLayout( new BorderLayout() );
		equipPanel.add( fieldPanel, BorderLayout.WEST );
		equipPanel.add( valuePanel, BorderLayout.EAST );

		return equipPanel;
	}

	private void refreshEquipPanel()
	{
		setEnabled( false );
		outfitSelect.setSelectedItem( null );
		characterData.updateEquipmentLists();
		setEnabled( true );
	}

	private class RefreshMenuItem extends JMenuItem implements ActionListener, Runnable
	{
		private KoLRequest request;

		public RefreshMenuItem( String title, int mnemonic, KoLRequest request )
		{
			super( title, mnemonic );
			addActionListener( this );

			this.request = request;
		}

		public void actionPerformed( ActionEvent e )
		{	(new DaemonThread( this )).start();
		}

		public void run()
		{
			GearChangeFrame.this.setEnabled( false );

			if ( request instanceof FamiliarRequest )
				familiarSelect.setSelectedItem( null );
			else
			{
				for ( int i = 0; i < equipment.length; ++i )
					equipmentSelect.setSelectedItem( null );
			}

			request.run();
			refreshEquipPanel();
		}
	}

	private class ChangeListener implements ActionListener, Runnable
	{
		private Integer slot;
		private JComboBox selector;
		private Object [] parameters;
		private Constructor constructor;

		public ChangeListener( JComboBox selector, Class requestClass, Class parameterClass )
		{
			this.selector = selector;

			Class [] parameterTypes = new Class[2];
			parameterTypes[0] = KoLmafia.class;
			parameterTypes[1] = parameterClass;

			initialize( requestClass, parameterTypes );
			this.slot = null;
		}

		public ChangeListener( JComboBox selector, Class requestClass, Class parameterClass, Integer slot )
		{
			this.selector = selector;

			Class [] parameterTypes = new Class[3];
			parameterTypes[0] = KoLmafia.class;
			parameterTypes[1] = parameterClass;
			parameterTypes[2] = Integer.class;

			initialize( requestClass, parameterTypes );
			this.parameters[2] = slot;
			this.slot = slot;
		}

		private void initialize( Class requestClass, Class [] parameterTypes )
		{
			try
			{
				this.constructor = requestClass.getConstructor( parameterTypes );
			}
			catch ( Exception e )
			{
				client.getLogStream().println(e);
			}

			this.parameters = new Object[ parameterTypes.length ];
			this.parameters[0] = client;
			for ( int i = 1; i < parameters.length; ++i )
				this.parameters[i] = null;
		}

		public void actionPerformed( ActionEvent e )
		{
			parameters[1] = selector.getSelectedItem();

			// In order to avoid constant misfiring of the table,
			// make sure that the change thread is not started
			// unless your current equipment does not match the
			// selected equipment.

			if ( !isChanging && selector.isEnabled() && this.parameters[1] != null )
			{
				if ( slot == null )
				{
					if ( selector == outfitSelect )
					{
						// Outfit event firing is usually only caused by
						// an actual attempt to change the outfit.

						(new DaemonThread( this )).start();
					}
					else if ( selector == familiarSelect )
					{
						// Familiar event firing is usually only caused
						// by an actual attempt to change the familiar.

						(new DaemonThread( this )).start();
					}
					else if ( selector == equipment[ KoLCharacter.FAMILIAR ] )
					{
						// If you're attempting to change your familiar
						// equipment, check to see if the selected item
						// is the same as your current familiar item
						// before executing the change.

						if ( !parameters[1].equals( client.getCharacterData().getEquipment( KoLCharacter.FAMILIAR ) ) )
							(new DaemonThread( this )).start();
					}
				}
				else if ( !client.getCharacterData().getEquipment( this.slot.intValue() ).equals( this.parameters[1] ) )
				{
					// Other equipment only gets fired when there's an
					// actual equipment change.

					(new DaemonThread( this )).start();
				}
			}
		}

		public void run()
		{
			try
			{
				isChanging = true;
				GearChangeFrame.this.setEnabled( false );
				client.makeRequest( (Runnable) constructor.newInstance( parameters ), 1 );
				refreshEquipPanel();
				isChanging = false;
			}
			catch ( Exception e )
			{
				setEnabled( false );
				refreshEquipPanel();
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new GearChangeFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
