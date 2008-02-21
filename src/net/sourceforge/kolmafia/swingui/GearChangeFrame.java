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

package net.sourceforge.kolmafia.swingui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class GearChangeFrame
	extends GenericFrame
{
	private static GearChangeFrame INSTANCE = null;

	private boolean isEnabled;
	private JButton outfitButton;

	private JRadioButton[] weaponTypes;
	private final ChangeComboBox[] equipment;
	private final SortedListModel weapons = new SortedListModel();
	private final SortedListModel offhands = new SortedListModel();
	private final ChangeComboBox outfitSelect, customSelect, familiarSelect;

	public GearChangeFrame()
	{
		super( "Gear Changer" );
		GearChangeFrame.INSTANCE = this;

		this.equipment = new ChangeComboBox[ 9 ];

		LockableListModel[] lists = EquipmentManager.getEquipmentLists();
		// We maintain our own lists of valid weapons and offhand items
		for ( int i = 0; i < this.equipment.length; ++i )
		{
			LockableListModel list;
			if ( i == EquipmentManager.WEAPON )
			{
				list = this.weapons;
			}
			else if ( i == EquipmentManager.OFFHAND )
			{
				list = this.offhands;
			}
			else
			{
				list = lists[ i ];
			}

			this.equipment[ i ] = new ChangeComboBox( list );
		}

		this.familiarSelect = new ChangeComboBox( KoLCharacter.getFamiliarList() );
		this.outfitSelect = new ChangeComboBox( EquipmentManager.getOutfits() );
		this.customSelect = new ChangeComboBox( EquipmentManager.getCustomOutfits() );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( new EquipPanel(), "" );
		this.ensureValidSelections();
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	private class EquipPanel
		extends GenericPanel
	{
		public EquipPanel()
		{
			super( "change gear", "save as outfit", new Dimension( 120, 20 ), new Dimension( 300, 20 ) );

			VerifiableElement[] elements = new VerifiableElement[ 16 ];

			elements[ 0 ] = new VerifiableElement( "Hat: ", GearChangeFrame.this.equipment[ 0 ] );
			elements[ 1 ] = new VerifiableElement( "Weapon: ", GearChangeFrame.this.equipment[ 1 ] );

			JPanel radioPanel = new JPanel( new GridLayout( 1, 4 ) );
			ButtonGroup radioGroup = new ButtonGroup();
			GearChangeFrame.this.weaponTypes = new JRadioButton[ 4 ];

			GearChangeFrame.this.weaponTypes[ 0 ] = new JRadioButton( "all", true );

			GearChangeFrame.this.weaponTypes[ 1 ] = new JRadioButton( "mus" );
			GearChangeFrame.this.weaponTypes[ 2 ] = new JRadioButton( "mys" );
			GearChangeFrame.this.weaponTypes[ 3 ] = new JRadioButton( "mox" );

			for ( int i = 0; i < 4; ++i )
			{
				if ( i == 1 )
				{
					radioPanel.add( new JLabel( " " ) );
				}

				radioGroup.add( GearChangeFrame.this.weaponTypes[ i ] );
				radioPanel.add( GearChangeFrame.this.weaponTypes[ i ] );
				GearChangeFrame.this.weaponTypes[ i ].addActionListener( new RefilterListener() );
			}

			elements[ 2 ] = new VerifiableElement( "", radioPanel );

			elements[ 3 ] = new VerifiableElement( "Off-Hand: ", GearChangeFrame.this.equipment[ 2 ] );
			elements[ 4 ] = new VerifiableElement( "Shirt: ", GearChangeFrame.this.equipment[ 3 ] );
			elements[ 5 ] = new VerifiableElement( "Pants: ", GearChangeFrame.this.equipment[ 4 ] );

			elements[ 6 ] = new VerifiableElement();

			elements[ 7 ] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ 5 ] );
			elements[ 8 ] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ 6 ] );
			elements[ 9 ] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ 7 ] );

			elements[ 10 ] = new VerifiableElement();

			elements[ 11 ] = new VerifiableElement( "Familiar: ", GearChangeFrame.this.familiarSelect );
			elements[ 12 ] = new VerifiableElement( "Fam Item: ", GearChangeFrame.this.equipment[ 8 ] );

			elements[ 13 ] = new VerifiableElement();

			elements[ 14 ] = new VerifiableElement( "Outfit: ", GearChangeFrame.this.outfitSelect );
			elements[ 15 ] = new VerifiableElement( "Custom: ", GearChangeFrame.this.customSelect );

			this.setContent( elements );
			GearChangeFrame.this.outfitButton = this.cancelledButton;
			this.setEnabled( true );
		}

		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			GearChangeFrame.this.isEnabled = isEnabled;

			GearChangeFrame.this.outfitButton.setEnabled( isEnabled );

			if ( isEnabled )
			{
				GearChangeFrame.this.ensureValidSelections();
			}
		}

		public void actionConfirmed()
		{
			synchronized ( SpecialOutfit.class )
			{
				RequestThread.openRequestSequence();
				GearChangeFrame.this.changeItems();
				RequestThread.closeRequestSequence();
			}
		}

		public void actionCancelled()
		{
			synchronized ( SpecialOutfit.class )
			{
				RequestThread.openRequestSequence();
				GearChangeFrame.this.changeItems();
				RequestThread.closeRequestSequence();
			}

			String currentValue = GenericFrame.input( "Name your outfit!", "Backup" );
			if ( currentValue != null )
			{
				RequestThread.postRequest( new EquipmentRequest( currentValue ) );
			}

		}
	}

	private void changeItems()
	{
		// Find out what changed

		AdventureResult[] pieces = new AdventureResult[ 8 ];

		for ( int i = 0; i < pieces.length; ++i )
		{
			pieces[ i ] = (AdventureResult) this.equipment[ i ].getSelectedItem();
			if ( EquipmentManager.getEquipment( i ).equals( pieces[ i ] ) )
			{
				pieces[ i ] = null;
			}
		}

		AdventureResult famitem = (AdventureResult) this.equipment[ EquipmentManager.FAMILIAR ].getSelectedItem();

		// Start with accessories

		for ( int i = EquipmentManager.ACCESSORY1; i <= EquipmentManager.ACCESSORY3; ++i )
		{
			if ( pieces[ i ] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[ i ], i, true ) );
				pieces[ i ] = null;
			}
		}

		// Move on to other equipment

		for ( int i = 0; i < EquipmentManager.ACCESSORY1; ++i )
		{
			if ( pieces[ i ] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[ i ], i, true ) );
				pieces[ i ] = null;
			}
		}

		if ( KoLCharacter.getFamiliar().canEquip( famitem ) )
		{
			RequestThread.postRequest( new EquipmentRequest( famitem, EquipmentManager.FAMILIAR ) );
		}
	}

	public static final void updateWeapons()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.weapons.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.WEAPON ) );
		GearChangeFrame.INSTANCE.offhands.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.OFFHAND ) );

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static final void clearWeaponLists()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.weapons.clear();
		GearChangeFrame.INSTANCE.offhands.clear();
	}

	public void dispose()
	{
		GearChangeFrame.INSTANCE = null;
		super.dispose();
	}

	private class ChangeComboBox
		extends JComboBox
	{
		public ChangeComboBox( final LockableListModel slot )
		{
			super( slot );
			this.setRenderer( ListCellRendererFactory.getUsableEquipmentRenderer() );
			this.addActionListener( new ChangeItemListener() );
		}

		private class ChangeItemListener
			extends ThreadedListener
		{
			public void run()
			{
				LockableListModel model = (LockableListModel) ChangeComboBox.this.getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// If you're changing an outfit, then the change must
				// occur right away.

				if ( ChangeComboBox.this == GearChangeFrame.this.outfitSelect || ChangeComboBox.this == GearChangeFrame.this.customSelect )
				{
					Object outfit = ChangeComboBox.this.getSelectedItem();
					if ( outfit == null || !( outfit instanceof SpecialOutfit ) )
					{
						return;
					}

					synchronized ( SpecialOutfit.class )
					{
						RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) outfit ) );
						RequestThread.enableDisplayIfSequenceComplete();
					}

					ChangeComboBox.this.setSelectedItem( null );
					return;
				}

				// If you're changing your familiar, then make sure all
				// the equipment pieces get changed and the familiar
				// gets changed right after.

				if ( ChangeComboBox.this == GearChangeFrame.this.familiarSelect )
				{
					synchronized ( SpecialOutfit.class )
					{
						RequestThread.openRequestSequence();
						GearChangeFrame.this.changeItems();

						FamiliarData familiar = (FamiliarData) GearChangeFrame.this.familiarSelect.getSelectedItem();
						if ( familiar != null && !familiar.equals( KoLCharacter.getFamiliar() ) )
						{
							RequestThread.postRequest( new FamiliarRequest( familiar ) );
						}

						RequestThread.closeRequestSequence();
					}

					return;
				}

				// In all other cases, simply re-validate what it is
				// you need to equip.

				GearChangeFrame.this.ensureValidSelections();
			}
		}
	}

	private class RefilterListener
		extends ThreadedListener
	{
		public void run()
		{
			GearChangeFrame.this.ensureValidSelections();
		}
	}

	private void ensureValidSelections()
	{
		this.equipment[ EquipmentManager.SHIRT ].setEnabled( this.isEnabled && KoLCharacter.hasSkill( "Torso Awaregness" ) );

		AdventureResult weaponItem = (AdventureResult) this.equipment[ EquipmentManager.WEAPON ].getSelectedItem();
		AdventureResult currentWeapon = EquipmentManager.getEquipment( EquipmentManager.WEAPON );
		if ( weaponItem == null )
		{
			weaponItem = currentWeapon;
		}

		List weaponItems = this.validWeaponItems( currentWeapon );
		this.updateEquipmentList( this.weapons, weaponItems, weaponItem );

		int weaponHands = EquipmentDatabase.getHands( weaponItem.getName() );
		if ( weaponHands > 1 )
		{
			// Equipping 2 or more handed weapon: nothing in off-hand
			this.equipment[ EquipmentManager.OFFHAND ].setSelectedItem( EquipmentRequest.UNEQUIP );
			this.equipment[ EquipmentManager.OFFHAND ].setEnabled( false );
		}
		else
		{
			AdventureResult offhandItem = (AdventureResult) this.equipment[ EquipmentManager.OFFHAND ].getSelectedItem();
			AdventureResult currentOffhand = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
			if ( offhandItem == null )
			{
				offhandItem = currentOffhand;
			}

			if ( EquipmentDatabase.getHands( offhandItem.getName() ) > 0 )
			{
				// Weapon in offhand. Must have compatible
				// weapon in weapon hand
				if ( weaponHands == 0 || EquipmentDatabase.getWeaponType( weaponItem.getName() ) != EquipmentDatabase.getWeaponType( offhandItem.getName() ) )
				{
					offhandItem = EquipmentRequest.UNEQUIP;
				}
			}

			List offhandItems = this.validOffhandItems( weaponItem, offhandItem );
			this.updateEquipmentList( this.offhands, offhandItems, offhandItem );
			this.equipment[ EquipmentManager.OFFHAND ].setEnabled( this.isEnabled );
		}
	}

	private List validWeaponItems( final AdventureResult currentWeapon )
	{
		List items = new ArrayList();

		// Search inventory for weapons

		int equipStat;
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );

			// Only add it once
			if ( items.contains( currentItem ) )
			{
				continue;
			}

			// Only add weapons
			int type = ItemDatabase.getConsumptionType( currentItem.getItemId() );

			if ( type != KoLConstants.EQUIP_WEAPON )
			{
				continue;
			}

			// Make sure we meet requirements
			if ( !EquipmentManager.canEquip( currentItem.getName() ) )
			{
				continue;
			}

			equipStat = EquipmentDatabase.getWeaponType( currentItem.getName() );

			if ( this.weaponTypes[ 0 ].isSelected() || this.weaponTypes[ 1 ].isSelected() && equipStat == KoLConstants.MUSCLE || this.weaponTypes[ 2 ].isSelected() && equipStat == KoLConstants.MYSTICALITY || this.weaponTypes[ 3 ].isSelected() && equipStat == KoLConstants.MOXIE )
			{
				items.add( currentItem );
			}
		}

		// Add the current weapon

		equipStat = EquipmentDatabase.getWeaponType( currentWeapon.getName() );
		if ( !items.contains( currentWeapon ) && ( this.weaponTypes[ 0 ].isSelected() || this.weaponTypes[ 1 ].isSelected() && equipStat == KoLConstants.MUSCLE || this.weaponTypes[ 2 ].isSelected() && equipStat == KoLConstants.MYSTICALITY || this.weaponTypes[ 3 ].isSelected() && equipStat == KoLConstants.MOXIE ) )
		{
			items.add( currentWeapon );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private List validOffhandItems( final AdventureResult weapon, final AdventureResult offhandItem )
	{
		List items = new ArrayList();

		// Find all offhand items that are compatible with the selected
		// weapon.

		// We can have weapons if we can dual wield and there is
		// one-handed weapon in the main hand
		boolean weapons =
			EquipmentDatabase.getHands( weapon.getName() ) == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );

		// The type of weapon in the off hand must
		// agree with the weapon in the main hand
		int equipStat = EquipmentDatabase.getWeaponType( weapon.getName() );

		// Search inventory for suitable items

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			if ( !items.contains( currentItem ) && this.validOffhandItem( currentItem, weapons, equipStat ) )
			{
				items.add( currentItem );
			}
		}

		// Add the selected off-hand item
		if ( !items.contains( offhandItem ) )
		{
			items.add( offhandItem );
		}

		// Possibly add the current off-hand item
		AdventureResult currentOffhand = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
		if ( !items.contains( currentOffhand ) && this.validOffhandItem( currentOffhand, weapons, equipStat ) )
		{
			items.add( currentOffhand );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private boolean validOffhandItem( final AdventureResult currentItem, boolean weapons, final int equipStat )
	{
		switch ( ItemDatabase.getConsumptionType( currentItem.getItemId() ) )
		{
		case KoLConstants.EQUIP_WEAPON:
			if ( !weapons )
			{
				return false;
			}
			if ( EquipmentDatabase.getHands( currentItem.getName() ) != 1 )
			{
				return false;
			}
			if ( equipStat != EquipmentDatabase.getWeaponType( currentItem.getName() ) )
			{
				return false;
			}
			// Fall through
		case KoLConstants.EQUIP_OFFHAND:
			// Make sure we meet requirements
			if ( EquipmentManager.canEquip( currentItem.getName() ) )
			{
				return true;
			}
			break;
		}
		return false;
	}

	private void updateEquipmentList( final LockableListModel currentItems, final List newItems,
		final AdventureResult equippedItem )
	{
		currentItems.retainAll( newItems );
		newItems.removeAll( currentItems );
		currentItems.addAll( newItems );

		currentItems.setSelectedItem( equippedItem );
	}
}
