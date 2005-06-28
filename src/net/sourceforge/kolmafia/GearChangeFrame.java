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
	private KoLCharacter characterData;
	private JComboBox [] equipment;
	private JComboBox outfitSelect, familiarSelect;
	private JMenuItem gearRefresh, familiarRefresh;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 *
	 * @param	client	The client containing the data associated with the character
	 */

	public GearChangeFrame( KoLmafia client )
	{
		super( "KoLmafia: Changing Gears", client );

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
		setDefaultCloseOperation( HIDE_ON_CLOSE );
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu( "Refresh" );
		fileMenu.setMnemonic( KeyEvent.VK_R );
		menuBar.add( fileMenu );

		gearRefresh = new JMenuItem( "Equipment", KeyEvent.VK_E );
		gearRefresh.addActionListener( new RefreshEquipmentListener() );
		fileMenu.add( gearRefresh );

		familiarRefresh = new JMenuItem( "Familiars", KeyEvent.VK_F );
		familiarRefresh.addActionListener( new RefreshFamiliarListener() );
		fileMenu.add( familiarRefresh );

		addHelpMenu( menuBar );
	}

	/**
	 * Sets all of the internal panels to a disabled or enabled state; this
	 * prevents the user from modifying the data as it's getting sent, leading
	 * to uncertainty and generally bad things.
	 */

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( equipment != null )
			for ( int i = 0; i < equipment.length; ++i )
				equipment[i].setEnabled( isEnabled );

		if ( outfitSelect != null )
			outfitSelect.setEnabled( isEnabled );

		if ( familiarSelect != null )
			familiarSelect.setEnabled( isEnabled );

		if ( gearRefresh != null )
			gearRefresh.setEnabled( isEnabled );

		if ( familiarRefresh != null )
			familiarRefresh.setEnabled( isEnabled );
	}

	/**
	 * Returns whether or not the internal panels are enabled.
	 */

	public boolean isEnabled()
	{	return gearRefresh != null && gearRefresh.isEnabled();
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
			equipment[i].addActionListener( new ChangeEquipmentListener( equipment[i] ) );
			JComponentUtilities.setComponentSize( equipment[i], 240, 20 );

			if ( i != KoLCharacter.FAMILIAR )
				valuePanel.add( equipment[i] );
		}

		valuePanel.add( new JLabel( " " ) );

		familiarSelect = new JComboBox( characterData.getFamiliars() );
		familiarSelect.addActionListener( new ChangeFamiliarListener() );
		JComponentUtilities.setComponentSize( familiarSelect, 240, 20 );
		valuePanel.add( familiarSelect );

		valuePanel.add( equipment[KoLCharacter.FAMILIAR] );
		valuePanel.add( new JLabel( " " ) );

		outfitSelect = new JComboBox( characterData.getOutfits() );
		outfitSelect.addActionListener( new ChangeOutfitListener() );
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
		characterData.updateEquipmentLists();
		setEnabled( true );
	}

	private class RefreshEquipmentListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			if ( isEnabled() )
				(new RefreshEquipmentThread()).start();
		}

		private class RefreshEquipmentThread extends DaemonThread
		{
			public void run()
			{
				setEnabled( false );
				(new EquipmentRequest( client, EquipmentRequest.EQUIPMENT )).run();
				refreshEquipPanel();
			}
		}
	}

	private class RefreshFamiliarListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			if ( isEnabled() )
				(new RefreshFamiliarThread()).start();
		}

		private class RefreshFamiliarThread extends DaemonThread
		{
			public void run()
			{
				setEnabled( false );
				(new FamiliarRequest( client )).run();
				refreshEquipPanel();
			}
		}
	}

	private class ChangeFamiliarListener implements ActionListener
	{
		private FamiliarData change;

		public void actionPerformed( ActionEvent e )
		{
			change = (FamiliarData) familiarSelect.getSelectedItem();
			if ( isEnabled() && change != null )
				(new ChangeFamiliarThread()).start();
		}

		private class ChangeFamiliarThread extends DaemonThread
		{
			public void run()
			{
				setEnabled( false );
				client.makeRequest( new FamiliarRequest( client, change ), 1 );
				refreshEquipPanel();
			}
		}
	}

	private class ChangeEquipmentListener implements ActionListener
	{
		private String change;
		private JComboBox select;

		public ChangeEquipmentListener( JComboBox select )
		{	this.select = select;
		}

		public void actionPerformed( ActionEvent e )
		{
			change = (String) select.getSelectedItem();
			if ( isEnabled() && change != null )
				(new ChangeEquipmentThread()).start();
		}

		private class ChangeEquipmentThread extends DaemonThread
		{
			public void run()
			{
				setEnabled( false );

				if ( select == equipment[KoLCharacter.ACCESSORY1] )
					client.makeRequest( new EquipmentRequest( client, "acc1" ), 1 );
				else if ( select == equipment[KoLCharacter.ACCESSORY2] )
					client.makeRequest( new EquipmentRequest( client, "acc2" ), 1 );
				else if ( select == equipment[KoLCharacter.ACCESSORY3] )
					client.makeRequest( new EquipmentRequest( client, "acc3" ), 1 );

				client.makeRequest( new EquipmentRequest( client, change ), 1 );
				refreshEquipPanel();
			}
		}
	}

	private class ChangeOutfitListener implements ActionListener
	{
		private SpecialOutfit change;

		public void actionPerformed( ActionEvent e )
		{
			change = (SpecialOutfit) outfitSelect.getSelectedItem();
			if ( isEnabled() && change != null )
				(new ChangeOutfitThread()).start();
		}

		private class ChangeOutfitThread extends DaemonThread
		{
			public void run()
			{
				setEnabled( false );
				client.makeRequest( new EquipmentRequest( client, change ), 1 );
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
