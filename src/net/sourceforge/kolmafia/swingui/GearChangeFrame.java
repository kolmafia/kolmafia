/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Iterator;
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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GearChangeFrame
	extends GenericFrame
{
	private static GearChangeFrame INSTANCE = null;

	private boolean isEnabled;
	private JButton outfitButton;

	private JRadioButton[] weaponTypes;
	private JCheckBox weapon1H;
	private JRadioButton[] offhandTypes;
	private final EquipmentComboBox[] equipment;
	private final SortedListModel hats = new SortedListModel();
	private final SortedListModel pants = new SortedListModel();
	private final SortedListModel weapons = new SortedListModel();
	private final SortedListModel offhands = new SortedListModel();
	private final SortedListModel familiars = new SortedListModel();
	private final SortedListModel crownFamiliars = new SortedListModel();
	private final SortedListModel bjornFamiliars = new SortedListModel();

	private final EquipmentPanel equipmentPanel;
	private final CustomizablePanel customizablePanel;

	private final OutfitComboBox outfitSelect, customSelect;
	private final FamiliarComboBox familiarSelect;
	private final ThroneComboBox crownSelect;
	private final BjornComboBox bjornSelect;
	private JLabel sticker1Label, sticker2Label, sticker3Label;
	private FamLockCheckbox famLockCheckbox;
	private FakeHandsSpinner fakeHands;

	private final static AdventureResult fakeHand = ItemPool.get( ItemPool.FAKE_HAND, 1 );
	private final static AdventureResult crownOfThrones = ItemPool.get( ItemPool.HATSEAT, 1 );
	private final static AdventureResult buddyBjorn = ItemPool.get( ItemPool.BUDDY_BJORN, 1 );
	public final static AdventureResult FOLDER_HOLDER = ItemPool.get( ItemPool.FOLDER_HOLDER, 1 );

	public GearChangeFrame()
	{
		super( "Gear Changer" );

		this.equipment = new EquipmentComboBox[ EquipmentManager.ALL_SLOTS ];

		LockableListModel[] lists = EquipmentManager.getEquipmentLists();
		// We maintain our own lists of valid hats, pants, weapons and offhand items
		for ( int i = 0; i < this.equipment.length; ++i )
		{
			LockableListModel list;
			switch ( i )
			{
			case EquipmentManager.HAT:
				list = this.hats;
				break;
			case EquipmentManager.PANTS:
				list = this.pants;
				break;
			case EquipmentManager.WEAPON:
				list = this.weapons;
				break;
			case EquipmentManager.OFFHAND:
				list = this.offhands;
				break;
			default:
				list = lists[ i ];
				break;
			}

			this.equipment[ i ] = new EquipmentComboBox( list, i );
		}

		this.familiarSelect = new FamiliarComboBox( this.familiars );
		this.crownSelect = new ThroneComboBox( this.crownFamiliars );
		this.bjornSelect = new BjornComboBox( this.bjornFamiliars );
		this.outfitSelect = new OutfitComboBox( EquipmentManager.getOutfits() );
		this.customSelect = new OutfitComboBox( EquipmentManager.getCustomOutfits() );

		this.equipmentPanel = new EquipmentPanel();
		this.customizablePanel = new CustomizablePanel();

		this.tabs.addTab( "Equipment", equipmentPanel );
		this.tabs.addTab( "Customizable", this.customizablePanel );

		JPanel gearPanel = new JPanel( new BorderLayout() );
		gearPanel.add( this.tabs, BorderLayout.CENTER );
		this.setCenterComponent( gearPanel );

		GearChangeFrame.INSTANCE = this;

		RequestThread.executeMethodAfterInitialization( this, "validateSelections" );
	}

	public static void showModifiers( Object value, boolean isFamiliarItem )
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		EquipmentTabPanel pane = (EquipmentTabPanel)GearChangeFrame.INSTANCE.tabs.getSelectedComponent();

		String name = null;
		if ( value instanceof AdventureResult )
		{
			name = ((AdventureResult) value).getName();
			if ( isFamiliarItem &&
			     ( KoLCharacter.getFamiliar().getId() == FamiliarPool.HATRACK ||
			       KoLCharacter.getFamiliar().getId() == FamiliarPool.SCARECROW ) )
			{
				name = "FamItem:" + name;
			}
		}
		else if ( value instanceof SpecialOutfit )
		{
			name = ((SpecialOutfit) value).getName();
		}
		else if ( value instanceof FamiliarData && pane == GearChangeFrame.INSTANCE.customizablePanel )
		{
			name = "Throne:" + ((FamiliarData) value).getRace();
		}
		else
		{
			return;
		}

		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			pane.getModifiersLabel().setText( "" );
			return;
		}

		name = mods.getString( Modifiers.INTRINSIC_EFFECT );
		if ( name.length() > 0 )
		{
			Modifiers newMods = new Modifiers();
			newMods.add( mods );
			newMods.add( Modifiers.getModifiers( name ) );
			mods = newMods;
		}

		StringBuilder buff = new StringBuilder();
		buff.append( "<html><table><tr><td width=" );
		buff.append( pane.getModifiersWidth() );
		buff.append( ">" );

		for ( int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i )
		{
			double val = mods.get( i );
			if ( val == 0.0f ) continue;
			name = Modifiers.getModifierName( i );
			name = StringUtilities.singleStringReplace( name, "Familiar", "Fam" );
			name = StringUtilities.singleStringReplace( name, "Experience", "Exp" );
			name = StringUtilities.singleStringReplace( name, "Damage", "Dmg" );
			name = StringUtilities.singleStringReplace( name, "Resistance", "Res" );
			name = StringUtilities.singleStringReplace( name, "Percent", "%" );
			buff.append( name );
			buff.append( ":<div align=right>" );
			buff.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( val ) );
			buff.append( "</div>" );
		}

		boolean anyBool = false;
		for ( int i = 1; i < Modifiers.BITMAP_MODIFIERS; ++i )
		{
			if ( mods.getRawBitmap( i ) == 0 ) continue;
			if ( anyBool )
			{
				buff.append( ", " );
			}
			anyBool = true;
			buff.append( Modifiers.getBitmapModifierName( i ) );
		}

		for ( int i = 1; i < Modifiers.BOOLEAN_MODIFIERS; ++i )
		{
			if ( !mods.getBoolean( i ) ) continue;
			if ( anyBool )
			{
				buff.append( ", " );
			}
			anyBool = true;
			buff.append( Modifiers.getBooleanModifierName( i ) );
		}

		for ( int i = 1; i < Modifiers.STRING_MODIFIERS; ++i )
		{
			if ( i == Modifiers.WIKI_NAME ||
			     i == Modifiers.MODIFIERS ||
			     i == Modifiers.OUTFIT ||
			     i == Modifiers.FAMILIAR_EFFCT )
			{
				continue;
			}

			String strval = mods.getString( i );
			if ( strval.equals( "" ) ) continue;
			name = Modifiers.getStringModifierName( i );
			name = StringUtilities.singleStringReplace( name, "Familiar", "Fam" );
			if ( anyBool )
			{
				buff.append( ", " );
			}
			buff.append( name );
			buff.append( ": " );
			buff.append( strval );
		}

		buff.append( "</td></tr></table></html>" );
		pane.getModifiersLabel().setText( buff.toString() );
	}

	private abstract class EquipmentTabPanel
		extends GenericPanel
	{
		protected JLabel modifiersLabel;
		protected int modifiersWidth;

		public EquipmentTabPanel( final String confirmedText, final String cancelledText, Dimension left, Dimension right )
		{
			super( confirmedText, cancelledText, left, right );
		}

		public EquipmentTabPanel( final String confirmedText, Dimension left, Dimension right )
		{
			super( confirmedText, null, left, right );
		}

		public JLabel getModifiersLabel()
		{
			return this.modifiersLabel;
		}

		public int getModifiersWidth()
		{
			return this.modifiersWidth;
		}
	}

	private class EquipmentPanel
		extends EquipmentTabPanel
	{
		public EquipmentPanel()
		{
			super( "change gear", "save as outfit", new Dimension( 120, 20 ), new Dimension( 320, 20 ) );

			ArrayList rows = new ArrayList<VerifiableElement>();
			VerifiableElement element;

			rows.add( new VerifiableElement( "Hat:", GearChangeFrame.this.equipment[ EquipmentManager.HAT ] ) );
			rows.add( new VerifiableElement( "Weapon:", GearChangeFrame.this.equipment[ EquipmentManager.WEAPON ] ) );

			JPanel radioPanel1 = new JPanel( new GridLayout( 1, 4 ) );
			ButtonGroup radioGroup1 = new ButtonGroup();
			GearChangeFrame.this.weaponTypes = new JRadioButton[ 3 ];

			GearChangeFrame.this.weaponTypes[ 0 ] = new JRadioButton( "all", true );
			GearChangeFrame.this.weaponTypes[ 1 ] = new JRadioButton( "melee" );
			GearChangeFrame.this.weaponTypes[ 2 ] = new JRadioButton( "ranged" );

			for ( int i = 0; i < weaponTypes.length; ++i )
			{
				radioGroup1.add( GearChangeFrame.this.weaponTypes[ i ] );
				radioPanel1.add( GearChangeFrame.this.weaponTypes[ i ] );
				GearChangeFrame.this.weaponTypes[ i ].addActionListener( new RefilterListener() );
			}

			GearChangeFrame.this.weapon1H = new JCheckBox( "1-hand" );
			radioPanel1.add( GearChangeFrame.this.weapon1H );
			GearChangeFrame.this.weapon1H.addActionListener( new RefilterListener() );

			rows.add( new VerifiableElement( "", radioPanel1 ) );

			rows.add( new VerifiableElement( "Off-Hand:", GearChangeFrame.this.equipment[ EquipmentManager.OFFHAND ] ) );

			JPanel radioPanel2 = new JPanel( new GridLayout( 1, 4 ) );
			ButtonGroup radioGroup2 = new ButtonGroup();
			GearChangeFrame.this.offhandTypes = new JRadioButton[ 4 ];

			GearChangeFrame.this.offhandTypes[ 0 ] = new JRadioButton( "all", true );
			GearChangeFrame.this.offhandTypes[ 1 ] = new JRadioButton( "weapon" );
			GearChangeFrame.this.offhandTypes[ 2 ] = new JRadioButton( "shields" );
			GearChangeFrame.this.offhandTypes[ 3 ] = new JRadioButton( "other" );

			for ( int i = 0; i < offhandTypes.length; ++i )
			{
				radioGroup2.add( GearChangeFrame.this.offhandTypes[ i ] );
				radioPanel2.add( GearChangeFrame.this.offhandTypes[ i ] );
				GearChangeFrame.this.offhandTypes[ i ].addActionListener( new RefilterListener() );
			}

			rows.add( new VerifiableElement( "", radioPanel2 ) );

			rows.add( new VerifiableElement( "Back:", GearChangeFrame.this.equipment[ EquipmentManager.CONTAINER ] ) );

			rows.add( new VerifiableElement( "Shirt:", GearChangeFrame.this.equipment[ EquipmentManager.SHIRT ] ) );
			rows.add( new VerifiableElement( "Pants:", GearChangeFrame.this.equipment[ EquipmentManager.PANTS ] ) );

			rows.add( new VerifiableElement() );

			rows.add( new VerifiableElement( "Accessory:", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY1 ] ) );
			rows.add( new VerifiableElement( "Accessory:", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY2 ] ) );
			rows.add( new VerifiableElement( "Accessory:", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY3 ] ) );

			rows.add( new VerifiableElement() );

			rows.add( new VerifiableElement( "Familiar:", GearChangeFrame.this.familiarSelect ) );
			rows.add( new VerifiableElement( "Fam Item:", GearChangeFrame.this.equipment[ EquipmentManager.FAMILIAR ] ) );

			GearChangeFrame.this.famLockCheckbox = new FamLockCheckbox();
			JPanel boxholder = new JPanel( new BorderLayout() );
			boxholder.add( GearChangeFrame.this.famLockCheckbox );
			rows.add( new VerifiableElement( "", boxholder ) );

			rows.add( new VerifiableElement( "Outfit:", GearChangeFrame.this.outfitSelect ) );
			rows.add( new VerifiableElement( "Custom:", GearChangeFrame.this.customSelect ) );

			VerifiableElement[] elements = new VerifiableElement[ rows.size() ];
			elements = (VerifiableElement[])rows.toArray( elements );

			this.setContent( elements );

			GearChangeFrame.this.outfitButton = this.cancelledButton;

			this.modifiersLabel = new JLabel();
			this.confirmedButton.getParent().getParent().add( this.modifiersLabel, BorderLayout.CENTER );
			this.modifiersWidth = this.eastContainer.getPreferredSize().width;
			this.setEnabled( true );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			GearChangeFrame.this.isEnabled = isEnabled;

			GearChangeFrame.this.outfitSelect.setEnabled( isEnabled );
			GearChangeFrame.this.customSelect.setEnabled( isEnabled );
			GearChangeFrame.this.familiarSelect.setEnabled( isEnabled );
			GearChangeFrame.this.outfitButton.setEnabled( isEnabled );

			if ( isEnabled )
			{
				GearChangeFrame.this.ensureValidSelections();
			}
		}

		@Override
		public void actionConfirmed()
		{
			synchronized ( GearChangeFrame.class )
			{
				GearChangeFrame.this.changeItems();
			}
		}

		@Override
		public void actionCancelled()
		{
			synchronized ( GearChangeFrame.class )
			{
				GearChangeFrame.this.changeItems();
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

		for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
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

	private class CustomizablePanel
		extends EquipmentTabPanel
	{
		public CustomizablePanel()
		{
			super( "change gear", new Dimension( 120, 20 ), new Dimension( 300, 20 ) );

			ArrayList rows = new ArrayList<VerifiableElement>();
			VerifiableElement element;

			rows.add(  new VerifiableElement( "Crown of Thrones:", GearChangeFrame.this.crownSelect ) );

			rows.add( new VerifiableElement() );

			rows.add(  new VerifiableElement( "Buddy Bjorn:", GearChangeFrame.this.bjornSelect ) );

			rows.add( new VerifiableElement() );

			element = new VerifiableElement( "Sticker:", GearChangeFrame.this.equipment[ EquipmentManager.STICKER1 ]  );
			GearChangeFrame.this.sticker1Label = element.getLabel();
			rows.add( element );

			element = new VerifiableElement( "Sticker:", GearChangeFrame.this.equipment[ EquipmentManager.STICKER2 ]  );
			GearChangeFrame.this.sticker2Label = element.getLabel();
			rows.add( element );

			element = new VerifiableElement( "Sticker:", GearChangeFrame.this.equipment[ EquipmentManager.STICKER3 ]  );
			GearChangeFrame.this.sticker3Label = element.getLabel();
			rows.add( element );

			rows.add( new VerifiableElement() );

			GearChangeFrame.this.fakeHands = new FakeHandsSpinner();
			GearChangeFrame.this.fakeHands.setHorizontalAlignment( AutoHighlightTextField.RIGHT );
			rows.add( new VerifiableElement( "Fake Hands:", GearChangeFrame.this.fakeHands ) );

			rows.add( new VerifiableElement() );

			rows.add( new VerifiableElement( "Card Sleeve:", GearChangeFrame.this.equipment[ EquipmentManager.CARD_SLEEVE ] ) );

			rows.add( new VerifiableElement() );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER1 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER2 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER3 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER4 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER5 ] ) );

			VerifiableElement[] elements = new VerifiableElement[ rows.size() ];
			elements = (VerifiableElement[])rows.toArray( elements );

			this.setContent( elements );

			this.modifiersLabel = new JLabel();
			this.confirmedButton.getParent().getParent().add( this.modifiersLabel, BorderLayout.CENTER );
			this.modifiersWidth = this.eastContainer.getPreferredSize().width;
			this.setEnabled( true );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );

			boolean hasCrownOfThrones = KoLCharacter.hasEquipped( GearChangeFrame.crownOfThrones );
			GearChangeFrame.this.crownSelect.setEnabled( isEnabled && hasCrownOfThrones );

			boolean hasBuddyBjorn = KoLCharacter.hasEquipped( GearChangeFrame.buddyBjorn );
			GearChangeFrame.this.bjornSelect.setEnabled( isEnabled && hasBuddyBjorn );

			boolean hasFakeHands = GearChangeFrame.this.fakeHands.getAvailableFakeHands() > 0;
			GearChangeFrame.this.fakeHands.setEnabled( isEnabled && hasFakeHands );

			boolean hasCardSleeve = EquipmentRequest.cardSleeve.getCount( KoLConstants.inventory ) > 0 ||
				KoLCharacter.hasEquipped( EquipmentRequest.cardSleeve );
			GearChangeFrame.this.equipment[ EquipmentManager.CARD_SLEEVE ].setEnabled( isEnabled && hasCardSleeve );

			boolean hasFolderHolder = GearChangeFrame.FOLDER_HOLDER.getCount( KoLConstants.inventory ) > 0 ||
				KoLCharacter.hasEquipped( GearChangeFrame.FOLDER_HOLDER );
			boolean inHighSchool = KoLCharacter.inHighschool();

			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER1 ].setEnabled( isEnabled && hasFolderHolder );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER2 ].setEnabled( isEnabled && hasFolderHolder );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER3 ].setEnabled( isEnabled && hasFolderHolder );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER4 ].setEnabled( isEnabled && hasFolderHolder && inHighSchool );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER5 ].setEnabled( isEnabled && hasFolderHolder && inHighSchool );
		}

		@Override
		public void actionConfirmed()
		{
			synchronized ( GearChangeFrame.class )
			{
				GearChangeFrame.this.customizeItems();
			}
		}

		@Override
		public void actionCancelled()
		{
		}
	}

	private void customizeItems()
	{
		// Crown of Thrones
		FamiliarData familiar = (FamiliarData) crownSelect.getSelectedItem();
		FamiliarData enthronedFamiliar = KoLCharacter.getEnthroned();
		if ( familiar != enthronedFamiliar )
		{
			RequestThread.postRequest( FamiliarRequest.enthroneRequest( familiar ) );
		}

		// Buddy Bjorn
		familiar = (FamiliarData) bjornSelect.getSelectedItem();
		FamiliarData bjornedFamiliar = KoLCharacter.getBjorned();
		if ( familiar != bjornedFamiliar )
		{
			RequestThread.postRequest( FamiliarRequest.bjornifyRequest( familiar ) );
		}

		// Card Sleeve
		AdventureResult card = (AdventureResult) this.equipment[ EquipmentManager.CARD_SLEEVE ].getSelectedItem();
		if ( !EquipmentManager.getEquipment( EquipmentManager.CARD_SLEEVE ).equals( card ) )
		{
			RequestThread.postRequest( new EquipmentRequest( card, EquipmentManager.CARD_SLEEVE, true ) );
		}

		// Stickers
		AdventureResult[] stickers = new AdventureResult[] {
			(AdventureResult) this.equipment[ EquipmentManager.STICKER1 ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.STICKER2 ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.STICKER3 ].getSelectedItem(),
		};

		for ( int i = 0; i < stickers.length; ++i )
		{
			AdventureResult sticker = stickers[ i ];
			int slot = EquipmentManager.STICKER1 + i;
			if ( !EquipmentManager.getEquipment( slot ).equals( sticker ) )
			{
				RequestThread.postRequest( new EquipmentRequest( sticker, slot, true ) );
			}
		}

		// Folders
		AdventureResult[] folders = new AdventureResult[] {
			(AdventureResult) this.equipment[ EquipmentManager.FOLDER1 ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.FOLDER2 ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.FOLDER3 ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.FOLDER4 ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.FOLDER5 ].getSelectedItem(),
		};

		for ( int i = 0; i < folders.length; ++i )
		{
			AdventureResult folder = folders[ i ];
			int slot = EquipmentManager.FOLDER1 + i;
			if ( !EquipmentManager.getEquipment( slot ).equals( folder ) )
			{
				RequestThread.postRequest( new EquipmentRequest( folder, slot, true ) );
			}
		}

		int oldFakeHands = EquipmentManager.getFakeHands();
		int newFakeHands = ((Integer)this.fakeHands.getValue()).intValue();
		if ( oldFakeHands != newFakeHands )
		{
			// If we want fewer fake hands than we currently have, unequip one - which will unequip all of them.
			if ( newFakeHands < oldFakeHands )
			{
				EquipmentRequest request = new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.FAKEHAND );
				RequestThread.postRequest( request );
				oldFakeHands = 0;
			}

			// Equip fake hands one at a time until we have enough
			while ( oldFakeHands++ < newFakeHands )
			{
				EquipmentRequest request = new EquipmentRequest( GearChangeFrame.fakeHand, EquipmentManager.FAKEHAND );
				RequestThread.postRequest( request );
			}
		}
	}

	public static final void validateSelections()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static final void updateHats()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.hats.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.HAT ) );

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static final void updatePants()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.pants.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.PANTS ) );

		GearChangeFrame.INSTANCE.ensureValidSelections();
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
		public EquipmentComboBox( final LockableListModel model, final int slot )
		{
			super( model );

			DefaultListCellRenderer renderer =
				( slot == EquipmentManager.FAMILIAR ) ?
				ListCellRendererFactory.getFamiliarEquipmentRenderer() :
				ListCellRendererFactory.getUsableEquipmentRenderer();

			this.setRenderer( renderer );
			this.addPopupMenuListener( new ChangeItemListener() );
		}

		private class ChangeItemListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
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

			this.setRenderer( ListCellRendererFactory.getDefaultRenderer() );
			this.addActionListener( new ChangeOutfitListener() );
		}

		private class ChangeOutfitListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
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
				}

				OutfitComboBox.this.setSelectedItem( null );
			}
		}
	}

	public static final void updateFamiliars()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		FamiliarData current = KoLCharacter.getFamiliar();
		GearChangeFrame.INSTANCE.familiars.setSelectedItem( current );
		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static final void clearFamiliarList()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.familiars.clear();
	}

	private class CarriedFamiliarComboBox
		extends JComboBox
	{
		public CarriedFamiliarComboBox( final LockableListModel model )
		{
			super( model );
			DefaultListCellRenderer renderer = ListCellRendererFactory.getFamiliarRenderer();
			this.setRenderer( renderer );
		}
	}

	private class ThroneComboBox
		extends CarriedFamiliarComboBox
		implements Listener
	{
		public ThroneComboBox( final LockableListModel model )
		{
			super( model );
			NamedListenerRegistry.registerNamedListener( "(throne)", this );
			this.update();
		}

		public void update()
		{
			FamiliarData enthronedFamiliar = KoLCharacter.getEnthroned();
			FamiliarData selectedThroneFamiliar = (FamiliarData) this.getSelectedItem();
			if ( enthronedFamiliar != selectedThroneFamiliar )
			{
				this.setSelectedItem( enthronedFamiliar );
			}
		}
	}

	private class BjornComboBox
		extends CarriedFamiliarComboBox
		implements Listener
	{
		public BjornComboBox( final LockableListModel model )
		{
			super( model );
			NamedListenerRegistry.registerNamedListener( "(bjorn)", this );
			this.update();
		}

		public void update()
		{
			FamiliarData bjornedFamiliar = KoLCharacter.getBjorned();
			FamiliarData selectedBjornFamiliar = (FamiliarData) this.getSelectedItem();
			if ( bjornedFamiliar != selectedBjornFamiliar )
			{
				this.setSelectedItem( bjornedFamiliar );
			}
		}
	}

	private class FamiliarComboBox
		extends JComboBox
	{
		public FamiliarComboBox( final LockableListModel model )
		{
			super( model );
			this.addActionListener( new ChangeFamiliarListener() );
		}

		private class ChangeFamiliarListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
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
					GearChangeFrame.this.changeItems();
					RequestThread.postRequest( new FamiliarRequest( familiar ) );
				}
			}
		}
	}

	private class RefilterListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			GearChangeFrame.this.ensureValidSelections();
		}
	}

	private void ensureValidSelections()
	{
		// If we are still logging in, defer this
		if ( KoLmafia.isRefreshing() )
		{
			return;
		}

		AdventureResult hatItem = (AdventureResult) this.equipment[ EquipmentManager.HAT ].getSelectedItem();
		AdventureResult currentHat = EquipmentManager.getEquipment( EquipmentManager.HAT );
		if ( hatItem == null )
		{
			hatItem = currentHat;
		}

		List hatItems = this.validHatItems( currentHat );
		this.updateEquipmentList( this.hats, hatItems, hatItem );

		AdventureResult pantsItem = (AdventureResult) this.equipment[ EquipmentManager.PANTS ].getSelectedItem();
		AdventureResult currentPants = EquipmentManager.getEquipment( EquipmentManager.PANTS );
		if ( pantsItem == null )
		{
			pantsItem = currentPants;
		}

		List pantsItems = this.validPantsItems( currentPants );
		this.updateEquipmentList( this.pants, pantsItems, pantsItem );

		this.equipment[ EquipmentManager.SHIRT ].setEnabled( this.isEnabled && KoLCharacter.isTorsoAware() );

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

		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
		FamiliarData selectedFamiliar = (FamiliarData) this.familiars.getSelectedItem();
		if ( selectedFamiliar == null )
		{
			selectedFamiliar = currentFamiliar;
		}

		FamiliarData enthronedFamiliar = KoLCharacter.getEnthroned();
		FamiliarData selectedThroneFamiliar = (FamiliarData) this.crownFamiliars.getSelectedItem();
		if ( selectedThroneFamiliar == null )
		{
			selectedThroneFamiliar = enthronedFamiliar;
		}

		FamiliarData bjornedFamiliar = KoLCharacter.getBjorned();
		FamiliarData selectedBjornFamiliar = (FamiliarData) this.bjornFamiliars.getSelectedItem();
		if ( selectedBjornFamiliar == null )
		{
			selectedBjornFamiliar = bjornedFamiliar;
		}

		this.updateEquipmentList( this.familiars, this.validFamiliars( currentFamiliar ), selectedFamiliar );
		this.updateEquipmentList( this.crownFamiliars, this.carriableFamiliars( currentFamiliar, bjornedFamiliar ), selectedThroneFamiliar );
		this.updateEquipmentList( this.bjornFamiliars, this.carriableFamiliars( currentFamiliar, enthronedFamiliar ), selectedBjornFamiliar );
	}

	private List validHatItems( final AdventureResult currentHat )
	{
		List<AdventureResult> items = new ArrayList<AdventureResult>();

		// Search inventory for hats
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			addHat( items, currentItem );
		}

		// Add the current hat
		addHat( items, currentHat );

		// Add anything your Hatrack is wearing unless it is your current familiar
		FamiliarData hatrack = KoLCharacter.findFamiliar( FamiliarPool.HATRACK );
		if ( hatrack != null && hatrack != KoLCharacter.getFamiliar() )
		{
			addHat( items, hatrack.getItem() );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addHat( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( !addItem( items, item, KoLConstants.EQUIP_HAT ) )
		{
			return;
		}

		items.add( item );
	}

	private List validPantsItems( final AdventureResult currentPants )
	{
		List<AdventureResult> items = new ArrayList<AdventureResult>();

		// Search inventory for pantss
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			addPants( items, currentItem );
		}

		// Add the current pants
		addPants( items, currentPants );

		// Add anything your Scarecrow is wearing unless it is your current familiar
		FamiliarData scarecrow = KoLCharacter.findFamiliar( FamiliarPool.SCARECROW );
		if ( scarecrow != null && scarecrow != KoLCharacter.getFamiliar() )
		{
			addPants( items, scarecrow.getItem() );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addPants( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( !addItem( items, item, KoLConstants.EQUIP_PANTS ) )
		{
			return;
		}

		items.add( item );
	}

	private List validWeaponItems( final AdventureResult currentWeapon )
	{
		List<AdventureResult> items = new ArrayList<AdventureResult>();

		if ( KoLCharacter.inFistcore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			return items;
		}

		if ( KoLCharacter.inAxecore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			items.add( EquipmentRequest.TRUSTY );
			return items;
		}

		// Search inventory for weapons
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			addWeapon( items, currentItem );
		}

		// Add the current weapon
		addWeapon( items, currentWeapon );

		// Add anything your Disembodied Hand is holding unless it is your current familiar
		FamiliarData hand = KoLCharacter.findFamiliar( FamiliarPool.HAND );
		if ( hand != null && hand != KoLCharacter.getFamiliar() )
		{
			addWeapon( items, hand.getItem() );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addWeapon( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( !addItem( items, item, KoLConstants.EQUIP_WEAPON ) )
		{
			return;
		}

		if ( !filterWeapon( item ) )
		{
			return;
		}

		items.add( item );
	}

	private boolean addItem( final List items, final AdventureResult item, final int type )
	{
		// Only add it once
		if ( items.contains( item ) )
		{
			return false;
		}

		// Only add items of specified type
		if ( type != ItemDatabase.getConsumptionType( item.getItemId() ) )
		{
			return false;
		}

		// Make sure we meet requirements
		if ( !EquipmentManager.canEquip( item.getName() ) )
		{
			return false;
		}

		return true;
	}

	private boolean filterWeapon( final AdventureResult weapon )
	{
		if ( this.weapon1H.isSelected() && EquipmentDatabase.getHands( weapon.getName() ) > 1 )
		{
			return false;
		}

		if ( this.weaponTypes[ 0 ].isSelected() )
		{
			return true;
		}

		switch ( EquipmentDatabase.getWeaponType( weapon.getName() ) )
		{
		case MELEE:
			return this.weaponTypes[ 1 ].isSelected();
		case RANGED:
			return this.weaponTypes[ 2 ].isSelected();
		}
		return false;
	}

	private List validOffhandItems( final AdventureResult weapon, final AdventureResult offhandItem )
	{
		List<AdventureResult> items = new ArrayList<AdventureResult>();

		// In Fistcore, you must have both hands free.
		// In Axecore, you can equip only Trusty, a two-handed axe
		if ( KoLCharacter.inFistcore() || KoLCharacter.inAxecore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			return items;
		}

		// Find all offhand items that are compatible with the selected
		// weapon.

		// We can have weapons if we can dual wield and there is
		// one-handed weapon in the main hand
		boolean weapons =
			EquipmentDatabase.getHands( weapon.getName() ) == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );

		// The type of weapon in the off hand must
		// agree with the weapon in the main hand
		WeaponType type = EquipmentDatabase.getWeaponType( weapon.getName() );

		// Search inventory for suitable items

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			// Fake hands are handled specially
			if ( currentItem.getItemId() == ItemPool.FAKE_HAND )
			{
				continue;
			}
			if ( !items.contains( currentItem ) && this.validOffhandItem( currentItem, weapons, type ) )
			{
				items.add( currentItem );
			}
		}

		// Add the selected off-hand item
		if ( !items.contains( offhandItem ) &&
		     validOffhandItem( offhandItem, weapons, type ) )
		{
			items.add( offhandItem );
		}

		// Possibly add the current off-hand item
		AdventureResult currentOffhand = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
		if ( !items.contains( currentOffhand ) &&
		     validOffhandItem( currentOffhand, weapons, type ) )
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

	private boolean validOffhandItem( final AdventureResult currentItem, boolean weapons, final WeaponType type )
	{
		switch ( ItemDatabase.getConsumptionType( currentItem.getItemId() ) )
		{
		case KoLConstants.EQUIP_WEAPON:
			if ( !weapons )
			{
				return false;
			}
			if ( EquipmentDatabase.isMainhandOnly( currentItem.getItemId() ) )
			{
				return false;
			}
			if ( type != EquipmentDatabase.getWeaponType( currentItem.getName() ) )
			{
				return false;
			}
			// Fall through
		case KoLConstants.EQUIP_OFFHAND:
			// See if user wants this type of item
			if ( !filterOffhand( currentItem ) )
			{
				return false;
			}
			// Make sure we meet requirements
			if ( EquipmentManager.canEquip( currentItem.getName() ) )
			{
				return true;
			}
			break;
		}
		return false;
	}

	private boolean filterOffhand( final AdventureResult offhand )
	{
		if ( this.offhandTypes[ 0 ].isSelected() )
		{
			return true;
		}

		int itemId = offhand.getItemId();

		if ( ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_WEAPON )
		{
			return this.offhandTypes[ 1 ].isSelected();
		}

		String type = EquipmentDatabase.getItemType( itemId );
		if ( this.offhandTypes[ 2 ].isSelected() )
		{
			// Shields
			return type.equals( "shield" );
		}

		if ( this.offhandTypes[ 3 ].isSelected() )
		{
			// Everything Else
			return type.equals( "offhand" );
		}

		return false;
	}

	private List validFamiliars( final FamiliarData currentFamiliar )
	{
		List<FamiliarData> familiars = new ArrayList<FamiliarData>();

		// Look at terrarium

		Iterator it = KoLCharacter.getFamiliarList().iterator();
		while ( it.hasNext() )
		{
			FamiliarData fam = (FamiliarData) it.next();

			// Only add it once
			if ( familiars.contains( fam ) )
			{
				continue;
			}

			if ( filterFamiliar( fam ) )
			{
				familiars.add( fam );
			}
		}

		// Add the current familiar

		if ( !familiars.contains( currentFamiliar ) &&
		     filterFamiliar( currentFamiliar ) )
		{
			familiars.add( currentFamiliar );
		}

		// Add "(none)"
		if ( !familiars.contains( FamiliarData.NO_FAMILIAR ) )
		{
			familiars.add( FamiliarData.NO_FAMILIAR );
		}

		return familiars;
	}

	private boolean filterFamiliar( final FamiliarData familiar )
	{
		return familiar.canEquip();
	}

	private List carriableFamiliars( final FamiliarData exclude1, final FamiliarData exclude2 )
	{
		List<FamiliarData> familiars = new ArrayList<FamiliarData>();

		// Look at terrarium

		Iterator it = KoLCharacter.getFamiliarList().iterator();
		while ( it.hasNext() )
		{
			FamiliarData fam = (FamiliarData) it.next();

			// Cannot carry a familiar if it is current familiar or is carried elsewhere
			if ( fam == exclude1 || fam == exclude2 )
			{
				continue;
			}

			// Certain familiars cannot be carried
			if ( !fam.canCarry() )
			{
				continue;
			}

			// Certain familiars can not be equipped in certain paths
			if ( !fam.canEquip() )
			{
				continue;
			}

			// Only add it once
			if ( familiars.contains( fam ) )
			{
				continue;
			}

			familiars.add( fam );
		}

		// Add "(none)"
		if ( !familiars.contains( FamiliarData.NO_FAMILIAR ) )
		{
			familiars.add( FamiliarData.NO_FAMILIAR );
		}

		return familiars;
	}

	private void updateEquipmentList( final LockableListModel currentItems, final List newItems, final Object equippedItem )
	{
		currentItems.retainAll( newItems );
		newItems.removeAll( currentItems );
		currentItems.addAll( newItems );

		currentItems.setSelectedItem( equippedItem );
	}

	private class FakeHandsSpinner
		extends AutoHighlightSpinner
		implements ChangeListener, Listener
	{
		private int currentFakeHands = 0;
		private int availableFakeHands = 0;

		public FakeHandsSpinner()
		{
			super();
			this.addChangeListener( this );
			NamedListenerRegistry.registerNamedListener( "(fakehands)", this );
			this.update();
		}

		public void stateChanged( final ChangeEvent e )
		{
			int maximum = this.availableFakeHands;
			if ( maximum == 0 )
			{
				this.setValue( IntegerPool.get( 0 ) );
				return;
			}

			int desired = InputFieldUtilities.getValue( this, maximum );
			if ( desired == maximum + 1 )
			{
				this.setValue( IntegerPool.get( 0 ) );
			}
			else if ( desired < 0 || desired > maximum )
			{
				this.setValue( IntegerPool.get( maximum ) );
			}
		}

		public int getAvailableFakeHands()
		{
			return this.availableFakeHands;
		}

		public void update()
		{
			int available = GearChangeFrame.fakeHand.getCount( KoLConstants.inventory );
			this.currentFakeHands = EquipmentManager.getFakeHands();
			this.availableFakeHands = this.currentFakeHands + available;
			this.setValue( IntegerPool.get( this.currentFakeHands ) );
		}
	}

	private class FamLockCheckbox
		extends JCheckBox
		implements ActionListener, Listener
	{
		public FamLockCheckbox()
		{
			super( "familiar item locked" );
			this.addActionListener( this );
			NamedListenerRegistry.registerNamedListener( "(familiarLock)", this );
			this.update();
		}

		public void actionPerformed( ActionEvent e )
		{
			RequestThread.postRequest( new FamiliarRequest( true ) );
		}

		public void update()
		{
			this.setSelected( EquipmentManager.familiarItemLocked() );
			this.setEnabled( EquipmentManager.familiarItemLockable() );
		}
	}
}
