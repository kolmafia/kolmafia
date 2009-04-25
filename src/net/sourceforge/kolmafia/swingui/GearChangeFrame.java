/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

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
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

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
	private final EquipmentComboBox[] equipment;
	private final SortedListModel weapons = new SortedListModel();
	private final SortedListModel offhands = new SortedListModel();
	private final OutfitComboBox outfitSelect, customSelect;
	private final FamiliarComboBox familiarSelect;
	private JLabel sticker1Label, sticker2Label, sticker3Label;
	private FamLockCheckbox	famLockCheckbox;

	public GearChangeFrame()
	{
		super( "Gear Changer" );
		GearChangeFrame.INSTANCE = this;

		this.equipment = new EquipmentComboBox[ EquipmentManager.ALL_SLOTS ];

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

			this.equipment[ i ] = new EquipmentComboBox( list, i == EquipmentManager.FAMILIAR );
		}

		this.familiarSelect = new FamiliarComboBox( KoLCharacter.getFamiliarList() );
		this.outfitSelect = new OutfitComboBox( EquipmentManager.getOutfits() );
		this.customSelect = new OutfitComboBox( EquipmentManager.getCustomOutfits() );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( new JScrollPane( new EquipPanel() ), "" );
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

			VerifiableElement[] elements = new VerifiableElement[ 20 ];

			elements[ 0 ] = new VerifiableElement( "Hat: ", GearChangeFrame.this.equipment[ EquipmentManager.HAT ] );
			elements[ 1 ] = new VerifiableElement( "Weapon: ", GearChangeFrame.this.equipment[ EquipmentManager.WEAPON ] );

			JPanel radioPanel = new JPanel( new GridLayout( 1, 4 ) );
			ButtonGroup radioGroup = new ButtonGroup();
			GearChangeFrame.this.weaponTypes = new JRadioButton[ 3 ];

			GearChangeFrame.this.weaponTypes[ 0 ] = new JRadioButton( "all", true );

			GearChangeFrame.this.weaponTypes[ 1 ] = new JRadioButton( "melee" );
			GearChangeFrame.this.weaponTypes[ 2 ] = new JRadioButton( "ranged" );

			for ( int i = 0; i < weaponTypes.length; ++i )
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

			elements[ 3 ] = new VerifiableElement( "Off-Hand: ", GearChangeFrame.this.equipment[ EquipmentManager.OFFHAND ] );
			elements[ 4 ] = new VerifiableElement( "Shirt: ", GearChangeFrame.this.equipment[ EquipmentManager.SHIRT ] );
			elements[ 5 ] = new VerifiableElement( "Pants: ", GearChangeFrame.this.equipment[ EquipmentManager.PANTS ] );

			elements[ 6 ] = new VerifiableElement();

			elements[ 7 ] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY1 ] );
			elements[ 8 ] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY2 ] );
			elements[ 9 ] = new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY3 ] );

			elements[ 10 ] = new VerifiableElement();

			elements[ 11 ] = new VerifiableElement( "Familiar: ", GearChangeFrame.this.familiarSelect );
			elements[ 12 ] = new VerifiableElement( "Fam Item: ", GearChangeFrame.this.equipment[ EquipmentManager.FAMILIAR ] );

			GearChangeFrame.this.famLockCheckbox = new FamLockCheckbox();
			JPanel boxholder = new JPanel( new BorderLayout() );
			boxholder.add( GearChangeFrame.this.famLockCheckbox );
			elements[ 13 ] = new VerifiableElement( "", boxholder );
			GearChangeFrame.updateFamiliarLock();

			elements[ 14 ] = new VerifiableElement( "Outfit: ", GearChangeFrame.this.outfitSelect );
			elements[ 15 ] = new VerifiableElement( "Custom: ", GearChangeFrame.this.customSelect );

			elements[ 16 ] = new VerifiableElement();

			elements[ 17 ] = new VerifiableElement( "Sticker: ", GearChangeFrame.this.equipment[ EquipmentManager.STICKER1 ]  );
			GearChangeFrame.this.sticker1Label = elements[ 17 ].getLabel();
			elements[ 18 ] = new VerifiableElement( "Sticker: ", GearChangeFrame.this.equipment[ EquipmentManager.STICKER2 ]  );
			GearChangeFrame.this.sticker2Label = elements[ 18 ].getLabel();
			elements[ 19 ] = new VerifiableElement( "Sticker: ", GearChangeFrame.this.equipment[ EquipmentManager.STICKER3 ]  );
			GearChangeFrame.this.sticker3Label = elements[ 19 ].getLabel();

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
			synchronized ( GearChangeFrame.class )
			{
				RequestThread.openRequestSequence();
				GearChangeFrame.this.changeItems();
				RequestThread.closeRequestSequence();
			}
		}

		public void actionCancelled()
		{
			synchronized ( GearChangeFrame.class )
			{
				RequestThread.openRequestSequence();
				GearChangeFrame.this.changeItems();
				RequestThread.closeRequestSequence();
			}

			String currentValue = InputFieldUtilities.input( "Name your outfit!", "Backup" );
			if ( currentValue != null )
			{
				RequestThread.postRequest( new EquipmentRequest( currentValue ) );
			}

		}
	}

	private void changeItems()
	{
		// Find out what changed

		AdventureResult[] pieces = new AdventureResult[ EquipmentManager.ALL_SLOTS ];

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

		for ( int i = EquipmentManager.STICKER1; i <= EquipmentManager.STICKER3; ++i )
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

	public static final void updateStickers( int st1, int st2, int st3 )
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}
		
		GearChangeFrame.INSTANCE.sticker1Label.setText( "Sticker (" + st1 + "): " );
		GearChangeFrame.INSTANCE.sticker2Label.setText( "Sticker (" + st2 + "): " );
		GearChangeFrame.INSTANCE.sticker3Label.setText( "Sticker (" + st3 + "): " );
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

	private class EquipmentComboBox
		extends JComboBox
	{
		public EquipmentComboBox( final LockableListModel slot, boolean familiarItems )
		{
			super( slot );

			DefaultListCellRenderer renderer = familiarItems ?
				ListCellRendererFactory.getFamiliarEquipmentRenderer() :
				ListCellRendererFactory.getUsableEquipmentRenderer();

			this.setRenderer( renderer );
			this.addPopupMenuListener( new ChangeItemListener() );
		}

		private class ChangeItemListener
			extends ThreadedListener
		{
			public void run()
			{
				LockableListModel model = (LockableListModel) EquipmentComboBox.this.getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// Simply re-validate what it is you need to
				// equip.

				GearChangeFrame.this.ensureValidSelections();
			}
		}
	}

	private class OutfitComboBox
		extends JComboBox
	{
		public OutfitComboBox( final LockableListModel slot )
		{
			super( slot );
			this.addActionListener( new ChangeOutfitListener() );
		}

		private class ChangeOutfitListener
			extends ThreadedListener
		{
			public void run()
			{
				LockableListModel model = (LockableListModel) OutfitComboBox.this.getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// If you're changing an outfit, then the
				// change must occur right away.

				Object outfit = OutfitComboBox.this.getSelectedItem();
				if ( outfit == null || !( outfit instanceof SpecialOutfit ) )
				{
					return;
				}

				synchronized ( GearChangeFrame.class )
				{
					RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) outfit ) );
					RequestThread.enableDisplayIfSequenceComplete();
				}

				OutfitComboBox.this.setSelectedItem( null );
			}
		}
	}

	private class FamiliarComboBox
		extends JComboBox
	{
		public FamiliarComboBox( final LockableListModel slot )
		{
			super( slot );
			this.addActionListener( new ChangeFamiliarListener() );
		}

		private class ChangeFamiliarListener
			extends ThreadedListener
		{
			public void run()
			{
				LockableListModel model = (LockableListModel) FamiliarComboBox.this.getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// If you're changing your familiar, then make
				// sure all the equipment pieces get changed
				// and the familiar gets changed right after.

				FamiliarData familiar = (FamiliarData) FamiliarComboBox.this.getSelectedItem();
				if ( familiar == null || familiar.equals( KoLCharacter.getFamiliar() ) )
				{
					return;
				}

				synchronized ( GearChangeFrame.class )
				{
					RequestThread.openRequestSequence();
					GearChangeFrame.this.changeItems();
					RequestThread.postRequest( new FamiliarRequest( familiar ) );
					RequestThread.closeRequestSequence();
				}
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

			if ( filterWeapon( currentItem ) )
			{
				items.add( currentItem );
			}
		}

		// Add the current weapon

		if ( !items.contains( currentWeapon ) &&
		     filterWeapon( currentWeapon ) )
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

	private boolean filterWeapon( final AdventureResult weapon )
	{
		if ( this.weaponTypes[ 0 ].isSelected() )
		{
			return true;
		}

		switch ( EquipmentDatabase.getWeaponType( weapon.getName() ) )
		{
		case KoLConstants.MELEE:
			return this.weaponTypes[ 1 ].isSelected();
		case KoLConstants.RANGED:
			return this.weaponTypes[ 2 ].isSelected();
		}
		return false;
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
		int type = EquipmentDatabase.getWeaponType( weapon.getName() );

		// Search inventory for suitable items

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			if ( !items.contains( currentItem ) && this.validOffhandItem( currentItem, weapons, type ) )
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
		if ( !items.contains( currentOffhand ) && this.validOffhandItem( currentOffhand, weapons, type ) )
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

	private boolean validOffhandItem( final AdventureResult currentItem, boolean weapons, final int type )
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
			if ( type != EquipmentDatabase.getWeaponType( currentItem.getName() ) )
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
	
	private class FamLockCheckbox
	extends JCheckBox
	implements ActionListener
	{
		public FamLockCheckbox()
		{
			super( "familiar item locked" );
			this.addActionListener( this );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			RequestThread.postRequest( new FamiliarRequest( true ) );
		}
	}
	
	public static void updateFamiliarLock()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}
		FamLockCheckbox box = GearChangeFrame.INSTANCE.famLockCheckbox;
		if ( box == null )
		{
			return;
		}
		box.setSelected( EquipmentManager.familiarItemLocked() );
		box.setEnabled( EquipmentManager.familiarItemLockable() );
	}
}
