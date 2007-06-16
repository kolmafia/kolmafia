/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class GearChangeFrame extends KoLFrame
{
	private static GearChangeFrame INSTANCE = null;

	private boolean isEnabled;
	private JButton outfitButton;

	private JRadioButton [] weaponTypes;
	private ChangeComboBox [] equipment;
	private SortedListModel weapons = new SortedListModel();
	private SortedListModel offhands = new SortedListModel();
	private ChangeComboBox outfitSelect, familiarSelect;

	public GearChangeFrame()
	{
		super( "Gear Changer" );
		INSTANCE = this;

		this.equipment = new ChangeComboBox[9];

		LockableListModel [] lists = KoLCharacter.getEquipmentLists();
		// We maintain our own lists of valid weapons and offhand items
		for ( int i = 0; i < this.equipment.length; ++i )
		{
			LockableListModel list;
			if ( i == KoLCharacter.WEAPON )
				list = this.weapons;
			else if ( i == KoLCharacter.OFFHAND )
				list = this.offhands;
			else
				list = lists[i];

			this.equipment[i] = new ChangeComboBox( list );
		}

		this.familiarSelect = new ChangeComboBox( KoLCharacter.getFamiliarList() );
		this.outfitSelect = new ChangeComboBox( KoLCharacter.getOutfits() );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( new EquipPanel(), "" );
		this.ensureValidSelections();
	}

	private class EquipPanel extends KoLPanel
	{
		public EquipPanel()
		{
			super( "change gear", "save as outfit", new Dimension( 120, 20 ), new Dimension( 300, 20 ) );

			VerifiableElement [] elements = new VerifiableElement[15];

			elements[0] = new VerifiableElement( "Hat: ", GearChangeFrame.this.equipment[0] );
			elements[1] = new VerifiableElement( "Weapon: ", GearChangeFrame.this.equipment[1] );

			JPanel radioPanel = new JPanel( new GridLayout( 1, 4 ) );
			ButtonGroup radioGroup = new ButtonGroup();
			GearChangeFrame.this.weaponTypes = new JRadioButton[3];

			GearChangeFrame.this.weaponTypes[0] = new JRadioButton( "both", true );

			GearChangeFrame.this.weaponTypes[1] = new JRadioButton( "melee" );
			GearChangeFrame.this.weaponTypes[2] = new JRadioButton( "ranged" );

			for ( int i = 0; i < 3; ++i )
			{
				if ( i == 1 )
					radioPanel.add( new JLabel( " " ) );

				radioGroup.add( GearChangeFrame.this.weaponTypes[i] );
				radioPanel.add( GearChangeFrame.this.weaponTypes[i] );
				GearChangeFrame.this.weaponTypes[i].addActionListener( new RefilterListener() );
			}

			elements[2] = new VerifiableElement( "", radioPanel );

			elements[3] = new VerifiableElement( "Off-Hand: ", GearChangeFrame.this.equipment[2] );
			elements[4] = new VerifiableElement( "Shirt: ", GearChangeFrame.this.equipment[3] );
			elements[5] = new VerifiableElement( "Pants: ", GearChangeFrame.this.equipment[4] );

			elements[6] = new VerifiableElement();

			elements[7] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[5] );
			elements[8] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[6] );
			elements[9] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[7] );

			elements[10] = new VerifiableElement();

			elements[11] = new VerifiableElement( "Familiar: ", GearChangeFrame.this.familiarSelect );
			elements[12] = new VerifiableElement( "Fam Item: ", GearChangeFrame.this.equipment[8] );

			elements[13] = new VerifiableElement();

			elements[14] = new VerifiableElement( "Outfit: ", GearChangeFrame.this.outfitSelect );

			this.setContent( elements );
			GearChangeFrame.this.outfitButton = this.cancelledButton;
			this.setEnabled( true );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			GearChangeFrame.this.isEnabled = isEnabled;

			GearChangeFrame.this.outfitButton.setEnabled( isEnabled );

			if ( isEnabled )
				GearChangeFrame.this.ensureValidSelections();
		}

		public void actionConfirmed()
		{
			RequestThread.openRequestSequence();
			GearChangeFrame.this.changeItems();
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			RequestThread.openRequestSequence();
			GearChangeFrame.this.changeItems();
			RequestThread.closeRequestSequence();

			String currentValue = JOptionPane.showInputDialog( "Name your outfit!", "KoLmafia Checkpoint" );
			if ( currentValue != null )
				RequestThread.postRequest( new EquipmentRequest( currentValue ) );

		}
	}

	private void changeItems()
	{
		// Find out what changed.
		AdventureResult [] pieces = new AdventureResult[8];

		for ( int i = 0; i < pieces.length; ++i )
		{
			pieces[i] = (AdventureResult) this.equipment[i].getSelectedItem();
			if ( KoLCharacter.getEquipment(i).equals( pieces[i] ) )
				pieces[i] = null;
		}

		AdventureResult famitem = (AdventureResult) this.equipment[KoLCharacter.FAMILIAR].getSelectedItem();

		// If current offhand item is not compatible with new
		// weapon, unequip it first.

		AdventureResult offhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
		if ( EquipmentDatabase.getHands( offhand.getName() ) == 1 )
		{
			AdventureResult weapon = pieces[ KoLCharacter.WEAPON ];
			if ( weapon != null )
			{
				if ( EquipmentDatabase.getHands( weapon.getName() ) == 1 && EquipmentDatabase.isRanged( weapon.getName() ) != EquipmentDatabase.isRanged( offhand.getName() ) )
					RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.OFFHAND ) );
			}
		}

		for ( int i = 0; i < pieces.length; ++i )
		{
			if ( pieces[i] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[i], i, true ) );
				pieces[i] = null;
			}
		}

		if ( KoLCharacter.getFamiliar().canEquip( famitem ) )
			RequestThread.postRequest( new EquipmentRequest( famitem, KoLCharacter.FAMILIAR ) );
	}

	public static void updateWeapons()
	{
		if ( INSTANCE == null )
			return;

		INSTANCE.weapons.setSelectedItem( KoLCharacter.getEquipment( KoLCharacter.WEAPON ) );
		INSTANCE.offhands.setSelectedItem( KoLCharacter.getEquipment( KoLCharacter.OFFHAND ) );

		INSTANCE.ensureValidSelections();
	}

	public static void clearWeaponLists()
	{
		if ( INSTANCE == null )
			return;

		INSTANCE.weapons.clear();
		INSTANCE.offhands.clear();
	}

	public void dispose()
	{
		INSTANCE = null;
		super.dispose();
	}

	private class ChangeComboBox extends JComboBox
	{
		public ChangeComboBox( LockableListModel slot )
		{
			super( slot );
			this.setRenderer( AdventureResult.getEquipmentRenderer() );
			this.addActionListener( new ChangeItemListener() );
		}

		private class ChangeItemListener extends ThreadedListener
		{
			public void run()
			{
				LockableListModel model = (LockableListModel) ChangeComboBox.this.getModel();
				if ( model.isEmpty() )
					return;

				// If you're changing an outfit, then the change must
				// occur right away.

				if ( ChangeComboBox.this == GearChangeFrame.this.outfitSelect )
				{
					Object outfit = ChangeComboBox.this.getSelectedItem();
					if ( outfit == null || !(outfit instanceof SpecialOutfit) )
						return;

					RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) outfit ) );
					RequestThread.enableDisplayIfSequenceComplete();

					ChangeComboBox.this.setSelectedItem( null );
					return;
				}

				// If you're changing your familiar, then make sure all
				// the equipment pieces get changed and the familiar
				// gets changed right after.

				if ( ChangeComboBox.this == GearChangeFrame.this.familiarSelect )
				{
					RequestThread.openRequestSequence();

					FamiliarData familiar = (FamiliarData) GearChangeFrame.this.familiarSelect.getSelectedItem();
					if ( familiar != null && !familiar.equals( KoLCharacter.getFamiliar() ) )
						RequestThread.postRequest( new FamiliarRequest( familiar ) );

					GearChangeFrame.this.changeItems();
					RequestThread.closeRequestSequence();

					return;
				}

				// In all other cases, simply re-validate what it is
				// you need to equip.

				GearChangeFrame.this.ensureValidSelections();
			}
		}
	}

	private class RefilterListener extends ThreadedListener
	{
		public void run()
		{	GearChangeFrame.this.ensureValidSelections();
		}
	}

	private void ensureValidSelections()
	{
		this.equipment[ KoLCharacter.SHIRT ].setEnabled( this.isEnabled && KoLCharacter.hasSkill( "Torso Awaregness" ) );

		AdventureResult weaponItem = (AdventureResult) this.equipment[ KoLCharacter.WEAPON ].getSelectedItem();
		AdventureResult currentWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		if ( weaponItem == null )
			weaponItem = currentWeapon;

		List weaponItems = this.validWeaponItems( currentWeapon );
		this.updateEquipmentList( this.weapons, weaponItems, weaponItem );

		int weaponHands = EquipmentDatabase.getHands( weaponItem.getName() );
		if ( weaponHands > 1 )
		{
			// Equipping 2 or more handed weapon: nothing in off-hand
			this.equipment[ KoLCharacter.OFFHAND ].setSelectedItem( EquipmentRequest.UNEQUIP );
			this.equipment[ KoLCharacter.OFFHAND ].setEnabled( false );
		}
		else
		{
			AdventureResult offhandItem = (AdventureResult) this.equipment[ KoLCharacter.OFFHAND ].getSelectedItem();
			AdventureResult currentOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
			if ( offhandItem == null )
				offhandItem = currentOffhand;

			if ( EquipmentDatabase.getHands( offhandItem.getName() ) > 0 )
			{
				// Weapon in offhand. Must have compatible
				// weapon in weapon hand
				if ( weaponHands == 0 || EquipmentDatabase.isRanged( weaponItem.getName() ) != EquipmentDatabase.isRanged( offhandItem.getName() ) )
					offhandItem = EquipmentRequest.UNEQUIP;
			}

			List offhandItems = this.validOffhandItems( weaponItem, offhandItem );
			this.updateEquipmentList( this.offhands, offhandItems, offhandItem );
			this.equipment[ KoLCharacter.OFFHAND ].setEnabled( this.isEnabled );
		}
	}

	private List validWeaponItems( AdventureResult currentWeapon )
	{
		List items = new ArrayList();

		// Search inventory for weapons

		for ( int i = 0; i < inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) inventory.get(i);
			int type = TradeableItemDatabase.getConsumptionType( currentItem.getItemId() );

			if ( type != EQUIP_WEAPON )
				continue;

			// Make sure we meet requirements
			if ( items.contains( currentItem ) || !EquipmentDatabase.canEquip( currentItem.getName() ) )
				continue;

			boolean ranged = EquipmentDatabase.isRanged( currentItem.getName() );

			if ( this.weaponTypes[0].isSelected() || (this.weaponTypes[1].isSelected() && !ranged) || (this.weaponTypes[2].isSelected() && ranged) )
				items.add( currentItem );
		}

		// Add the current weapon

		boolean ranged = EquipmentDatabase.isRanged( currentWeapon.getName() );

		if ( !items.contains( currentWeapon ) )
			if ( this.weaponTypes[0].isSelected() || (this.weaponTypes[1].isSelected() && !ranged) || (this.weaponTypes[2].isSelected() && ranged) )
				items.add( currentWeapon );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
			items.add( EquipmentRequest.UNEQUIP );

		return items;
	}

	private List validOffhandItems( AdventureResult weapon, AdventureResult offhandItem )
	{
		List items = new ArrayList();

		// Find all offhand items that are compatible with the selected
		// weapon.

		// We can have weapons if we can dual wield and there is
		// one-handed weapon in the main hand
		boolean weapons = EquipmentDatabase.getHands( weapon.getName() ) == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );

		// The type of weapon in the off hand - ranged or melee - must
		// agree with the weapon in the main hand
		boolean ranged = EquipmentDatabase.isRanged( weapon.getName() );

		// Search inventory for suitable items

		for ( int i = 0; i < inventory.size(); ++i )
		{
			AdventureResult currentItem = ((AdventureResult)inventory.get(i));
			if ( !items.contains( currentItem ) && this.validOffhandItem( currentItem, weapons, ranged ) )
				items.add( currentItem );
		}

		// Add the selected off-hand item
		if ( !items.contains( offhandItem ) )
			items.add( offhandItem );

		// Possibly add the current off-hand item
		AdventureResult currentOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
		if ( !items.contains( currentOffhand ) && this.validOffhandItem( currentOffhand, weapons, ranged )  )
			items.add( currentOffhand );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
			items.add( EquipmentRequest.UNEQUIP );

		return items;
	}

	private boolean validOffhandItem( AdventureResult currentItem, boolean weapons, boolean ranged )
	{
		switch ( TradeableItemDatabase.getConsumptionType( currentItem.getItemId() ) )
		{
		case EQUIP_WEAPON:
			if ( !weapons )
				return false;
			if ( EquipmentDatabase.getHands( currentItem.getName() ) != 1 )
				return false;
			if ( ranged != EquipmentDatabase.isRanged( currentItem.getName() ) )
				return false;
			// Fall through
		case EQUIP_OFFHAND:
			// Make sure we meet requirements
			if ( EquipmentDatabase.canEquip( currentItem.getName() ) )
				return true;
			break;
		}
		return false;
	}

	private void updateEquipmentList( LockableListModel currentItems, List newItems, AdventureResult equippedItem )
	{
		currentItems.retainAll( newItems );
		newItems.removeAll( currentItems );
		currentItems.addAll( newItems );

		currentItems.setSelectedItem( equippedItem );
	}
}
