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
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;

// event listeners
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Constructor;

// utilities
import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

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
	private String [] pieces = new String[9];
	private JButton outfitButton;

	private EquipPanel equip;
	private ChangeComboBox [] equipment;
	private SortedListModel weapons = new SortedListModel();
	private SortedListModel offhands = new SortedListModel();
	private ChangeComboBox outfitSelect, familiarSelect;

	public GearChangeFrame()
	{
		super( "Gear Changer" );

		equipment = new ChangeComboBox[9];

		LockableListModel [] lists = KoLCharacter.getEquipmentLists();
		// We maintain our own lists of valid weapons and offhand items
		for ( int i = 0; i < equipment.length; ++i )
		{
			LockableListModel list;
			if ( i == KoLCharacter.WEAPON )
				list = weapons;
			else if ( i == KoLCharacter.OFFHAND )
				list = offhands;
			else
				list = lists[i];
			equipment[i] = new ChangeComboBox( list );
		}

		familiarSelect = new ChangeComboBox( KoLCharacter.getFamiliarList() );
		outfitSelect = new ChangeComboBox( KoLCharacter.getOutfits() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( equip = new EquipPanel(), "" );
		KoLCharacter.updateEquipmentLists();
		ensureValidSelections();
	}

	private class EquipPanel extends KoLPanel
	{
		public EquipPanel()
		{
			super( "change gear", "save as outfit", new Dimension( 120, 20 ), new Dimension( 300, 20 ) );

			VerifiableElement [] elements = new VerifiableElement[14];

			elements[0] = new VerifiableElement( "Hat: ", equipment[0] );
			elements[1] = new VerifiableElement( "Weapon: ", equipment[1] );
			elements[2] = new VerifiableElement( "Off-Hand: ", equipment[2] );
			elements[3] = new VerifiableElement( "Shirt: ", equipment[3] );
			elements[4] = new VerifiableElement( "Pants: ", equipment[4] );

			elements[5] = new VerifiableElement( "", new JLabel() );

			elements[6] = new VerifiableElement( "Accessory 1: ", equipment[5] );
			elements[7] = new VerifiableElement( "Accessory 2: ", equipment[6] );
			elements[8] = new VerifiableElement( "Accessory 3: ", equipment[7] );

			elements[9] = new VerifiableElement( "", new JLabel() );

			elements[10] = new VerifiableElement( "Familiar: ", familiarSelect );
			elements[11] = new VerifiableElement( "Familiar Item: ", equipment[8] );

			elements[12] = new VerifiableElement( "", new JLabel() );

			elements[13] = new VerifiableElement( "Outfit: ", outfitSelect );

			setContent( elements );
			outfitButton = cancelledButton;
			setEnabled( true );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			outfitButton.setEnabled( isEnabled );

			if ( isEnabled )
				ensureValidSelections();
		}

		public void actionConfirmed()
		{
			ArrayList requestList = new ArrayList();

			// If current offhand item is not compatible with new
			// weapon, unequip it first.
			String offhand = KoLCharacter.getCurrentEquipmentName( KoLCharacter.OFFHAND );
			if ( EquipmentDatabase.getHands( offhand ) == 1 )
			{
				String weapon = pieces[ KoLCharacter.WEAPON ];
				if ( weapon != null )
				{
					weapon = KoLCharacter.getEquipmentName( weapon );
					if ( EquipmentDatabase.getHands( weapon ) == 1 &&
					     EquipmentDatabase.isRanged( weapon) != EquipmentDatabase.isRanged( offhand ) )
					{
						requestList.add( new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.UNEQUIP, KoLCharacter.OFFHAND ) );
					}
				}
			}

			for ( int i = 0; i < pieces.length; ++i )
			{
				if ( pieces[i] != null )
				{
					requestList.add( new EquipmentRequest( StaticEntity.getClient(), pieces[i], i, true ) );
					pieces[i] = null;
				}
			}

			FamiliarData familiar = (FamiliarData) familiarSelect.getSelectedItem();
			if ( familiar != null && !familiar.equals( KoLCharacter.getFamiliar() ) )
				requestList.add( new FamiliarRequest( StaticEntity.getClient(), familiar ) );

			if ( requestList.isEmpty() )
				return;

			Runnable [] requests = new Runnable[ requestList.size() ];
			requestList.toArray( requests );

			(new RequestThread( requests )).start();
		}

		public void actionCancelled()
		{
			String currentValue = JOptionPane.showInputDialog( "Name your outfit!", "KoLmafia Checkpoint" );
			if ( currentValue == null )
				return;

			(new RequestThread( new EquipmentRequest( StaticEntity.getClient(), currentValue ) )).start();
		}
	}

	private class ChangeComboBox extends JComboBox
	{
		public ChangeComboBox( LockableListModel slot )
		{	super( slot );
		}

		public void firePopupMenuWillBecomeInvisible()
		{
			super.firePopupMenuWillBecomeInvisible();

			if ( this == outfitSelect )
			{
				Object outfit = getSelectedItem();
				if ( outfit != null && !(outfit instanceof String) )
					(new RequestThread( new EquipmentRequest( StaticEntity.getClient(), (SpecialOutfit) outfit ) )).start();

				setSelectedItem( null );
				return;
			}
			else if ( this == familiarSelect )
			{
				equip.actionConfirmed();
				return;
			}

			for ( int i = 0; i < equipment.length; ++i )
				if ( this == equipment[i] )
				{
					pieces[i] = (String) getSelectedItem();
					if ( KoLCharacter.getEquipment(i).equals( pieces[i] ) )
						pieces[i] = null;
				}

			ensureValidSelections();
		}
	}

	private void ensureValidSelections()
	{
		equipment[ KoLCharacter.SHIRT ].setEnabled( KoLCharacter.hasSkill( "Torso Awaregness" ) );

		String weaponItem = pieces[ KoLCharacter.WEAPON ];
		String currentWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		if ( weaponItem == null )
			weaponItem = currentWeapon;

		List weaponItems = validWeaponItems( currentWeapon );
		updateEquipmentList( weapons, weaponItems, weaponItem );

		String weapon = KoLCharacter.getEquipmentName( weaponItem );
		int weaponHands = EquipmentDatabase.getHands( weapon );
		if ( weaponHands > 1 )
		{
			// Equipping 2 or more handed weapon: nothing in off-hand
			equipment[ KoLCharacter.OFFHAND ].setSelectedItem( EquipmentRequest.UNEQUIP );
			pieces[ KoLCharacter.OFFHAND ] = null;
			equipment[ KoLCharacter.OFFHAND ].setEnabled( false );
		}
		else
		{
			String offhandItem = pieces[ KoLCharacter.OFFHAND ];
			String currentOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
			if ( offhandItem == null )
				offhandItem = currentOffhand;

			String offhand = KoLCharacter.getEquipmentName( offhandItem );
			if ( EquipmentDatabase.getHands( offhand ) > 0 )
			{
				// Weapon in offhand. Must have compatible
				// weapon in weapon hand
				if ( weaponHands == 0 || EquipmentDatabase.isRanged( weapon ) != EquipmentDatabase.isRanged( offhand ) )
				{
					pieces[ KoLCharacter.OFFHAND ] = null;
					offhandItem = EquipmentRequest.UNEQUIP;
				}
			}

			List offhandItems = validOffhandItems( weapon, offhandItem );
			updateEquipmentList( offhands, offhandItems, offhandItem );
			equipment[ KoLCharacter.OFFHAND ].setEnabled( true );
		}

		boolean enableOutfits = true;
		for ( int i = 0; i < equipment.length; ++i )
			enableOutfits &= pieces[i] == null;

		outfitSelect.setEnabled( enableOutfits );
		outfitButton.setEnabled( enableOutfits );
	}

	private static List validWeaponItems( String currentWeapon )
	{
		List items = new ArrayList();

		// Search inventory for weapons
                
		for ( int i = 0; i < KoLCharacter.getInventory().size(); ++i )
		{
			String currentItem = ((AdventureResult)KoLCharacter.getInventory().get(i)).getName();
			int type = TradeableItemDatabase.getConsumptionType( currentItem );

			if ( type != ConsumeItemRequest.EQUIP_WEAPON )
				continue;

			// Make sure we meet requirements
			if ( !EquipmentDatabase.canEquip( currentItem ) )
				continue;

			items.add( currentItem + " (+" + EquipmentDatabase.getPower( currentItem ) + ")" );
		}

		// Add the current weapon
		if ( !items.contains( currentWeapon ) )
			items.add( currentWeapon );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
			items.add( EquipmentRequest.UNEQUIP );

		return items;
	}

	private static List validOffhandItems( String weapon, String offhandItem )
	{
		List items = new ArrayList();

		// Find all offhand items that are compatible with the selected
		// weapon.

		// We can have weapons if we can dual wield and there is
		// one-handed weapon in the main hand
		boolean weapons = EquipmentDatabase.getHands( weapon ) == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );

		// The type of weapon in the off hand - ranged or melee - must
		// agree with the weapon in the main hand
		boolean ranged = EquipmentDatabase.isRanged( weapon );

		// Search inventory for suitable items
		
		for ( int i = 0; i < KoLCharacter.getInventory().size(); ++i )
		{
			String currentItem = ((AdventureResult)KoLCharacter.getInventory().get(i)).getName();
			if ( validOffhandItem( currentItem, weapons, ranged ) )
				items.add( currentItem + " (+" + EquipmentDatabase.getPower( currentItem ) + ")" );
		}

		// Add the selected off-hand item
		if ( !items.contains( offhandItem ) )
			items.add( offhandItem );

		// Possibly add the current off-hand item
		String currentOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
		if ( !items.contains( currentOffhand ) &&
		     validOffhandItem( KoLCharacter.getEquipmentName( currentOffhand ), weapons, ranged )  )
			items.add( currentOffhand );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
			items.add( EquipmentRequest.UNEQUIP );

		return items;
	}

	private static boolean validOffhandItem( String currentItem, boolean weapons, boolean ranged )
	{
		switch ( TradeableItemDatabase.getConsumptionType( currentItem ) )
		{
		case ConsumeItemRequest.EQUIP_WEAPON:
			if ( !weapons )
				return false;
			if ( EquipmentDatabase.getHands( currentItem ) != 1 )
				return false;
			if ( ranged != EquipmentDatabase.isRanged( currentItem ) )
				return false;
			// Fall through
		case ConsumeItemRequest.EQUIP_OFFHAND:
			// Make sure we meet requirements
			if ( EquipmentDatabase.canEquip( currentItem ) )
				return true;
			break;
		}
		return false;
	}

	private void updateEquipmentList( LockableListModel currentList, List newItems, String equippedItem )
	{
		if ( currentList.equals( newItems ) )
			return;

		currentList.clear();
		currentList.addAll( newItems );
		currentList.setSelectedItem( equippedItem );
        }

}
