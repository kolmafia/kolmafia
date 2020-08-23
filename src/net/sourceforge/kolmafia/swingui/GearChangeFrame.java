/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.dev.spellcast.utilities.ActionVerifyPanel.HideableVerifiableElement;
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

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.Limitmode;

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
	private final SortedListModel<AdventureResult>[] equipmentModels;

	private final SortedListModel<FamiliarData> familiars = new SortedListModel<>();
	private final SortedListModel<FamiliarData> crownFamiliars = new SortedListModel<>();
	private final SortedListModel<FamiliarData> bjornFamiliars = new SortedListModel<>();

	private final EquipmentPanel equipmentPanel;
	private final CustomizablePanel customizablePanel;

	private final OutfitComboBox outfitSelect, customSelect;
	private final FamiliarComboBox familiarSelect;
	private final ThroneComboBox crownSelect;
	private final BjornComboBox bjornSelect;
	private JLabel sticker1Label, sticker2Label, sticker3Label;
	private FamLockCheckbox famLockCheckbox;
	private FakeHandsSpinner fakeHands;

	public GearChangeFrame()
	{
		super( "Gear Changer" );

		this.equipment = new EquipmentComboBox[ EquipmentManager.ALL_SLOTS ];
		this.equipmentModels = new SortedListModel[ EquipmentManager.ALL_SLOTS ];

		List<AdventureResult> [] lists = EquipmentManager.getEquipmentLists();

		for ( int i = 0; i < this.equipment.length; ++i )
		{
			LockableListModel<AdventureResult> list;

			// We maintain our own lists for certain slots
			switch ( i )
			{
			case EquipmentManager.HAT:
			case EquipmentManager.PANTS:
			case EquipmentManager.SHIRT:
			case EquipmentManager.CONTAINER:
			case EquipmentManager.WEAPON:
			case EquipmentManager.OFFHAND:
			case EquipmentManager.ACCESSORY1:
			case EquipmentManager.ACCESSORY2:
			case EquipmentManager.ACCESSORY3:
			case EquipmentManager.BOOTSKIN:
			case EquipmentManager.BOOTSPUR:
			case EquipmentManager.HOLSTER:
				list = this.equipmentModels[ i ] = new SortedListModel<AdventureResult>();
				break;
			default:
				list = (LockableListModel<AdventureResult>) lists[ i ];
				break;
			}

			this.equipment[ i ] = new EquipmentComboBox( list, i );
		}

		this.familiarSelect = new FamiliarComboBox( this.familiars );
		this.crownSelect = new ThroneComboBox( this.crownFamiliars );
		this.bjornSelect = new BjornComboBox( this.bjornFamiliars );
		this.outfitSelect = new OutfitComboBox( (LockableListModel<SpecialOutfit>) EquipmentManager.getOutfits() );
		this.customSelect = new OutfitComboBox( (LockableListModel<SpecialOutfit>) EquipmentManager.getCustomOutfits() );

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

		Modifiers mods = null;

		if ( value instanceof AdventureResult )
		{
			AdventureResult item = (AdventureResult) value;
			int itemId = item.getItemId();
			int familiarId = KoLCharacter.getFamiliar().getId();
			int consumption = ItemDatabase.getConsumptionType( itemId );

			if ( itemId == -1 )
			{
				// Nothing for (none)
				mods = null;
			}
			if ( isFamiliarItem && consumption != KoLConstants.EQUIP_FAMILIAR &&
			     ( familiarId == FamiliarPool.HATRACK || ( familiarId == FamiliarPool.SCARECROW ) ) )
			{
				// Mad Hat Racks can equip hats.
				// Fancypants Scarecrows can equip pants.
				//
				// In each case, there is a special familiar
				// effect; the standard item modifiers are
				// meaningless.
				//
				// Disembodied Hands can equip one-handed weapons.
				// Left Mand Man can equip off-hand items.
				//
				// In each case, the standard item modifiers
				// are in force.
				mods = null;
			}
			else
			{
				Modifiers newMods = new Modifiers();
				newMods.add( Modifiers.getItemModifiers( itemId ) );

				switch ( itemId )
				{
				case ItemPool.CROWN_OF_ED:
				{
					newMods.add( Modifiers.getModifiers( "Edpiece", Preferences.getString( "edPiece" ) ) );
					break;
				}
				case ItemPool.SNOW_SUIT:
				{
					newMods.add( Modifiers.getModifiers( "Snowsuit", Preferences.getString( "snowsuit" ) ) );
					break;
				}
				case ItemPool.COWBOY_BOOTS:
				{
					AdventureResult skin = EquipmentManager.getEquipment( EquipmentManager.BOOTSKIN );
					AdventureResult spur = EquipmentManager.getEquipment( EquipmentManager.BOOTSPUR );
					if ( skin != null && skin != EquipmentRequest.UNEQUIP )
					{
						newMods.add( Modifiers.getItemModifiers( skin.getItemId() ) );
					}
					if ( spur != null && spur != EquipmentRequest.UNEQUIP )
					{
						newMods.add( Modifiers.getItemModifiers( spur.getItemId() ) );
					}
					break;
				}
				case ItemPool.FOLDER_HOLDER:
				{
					for ( int i = EquipmentManager.FOLDER1; i <= EquipmentManager.FOLDER5; ++i )
					{
						AdventureResult folder = EquipmentManager.getEquipment( i );
						if ( folder != null && folder != EquipmentRequest.UNEQUIP )
						{
							newMods.add( Modifiers.getItemModifiers( folder.getItemId() ) );
						}
					}
					break;
				}
				case ItemPool.STICKER_CROSSBOW:
				case ItemPool.STICKER_SWORD:
				{
					for ( int i = EquipmentManager.STICKER1; i <= EquipmentManager.STICKER3; ++i )
					{
						AdventureResult sticker = EquipmentManager.getEquipment( i );
						if ( sticker != null && sticker != EquipmentRequest.UNEQUIP )
						{
							newMods.add( Modifiers.getItemModifiers( sticker.getItemId() ) );
						}
					}
					break;
				}
				case ItemPool.CARD_SLEEVE:
				{
					AdventureResult card = EquipmentManager.getEquipment( EquipmentManager.CARDSLEEVE );
					if ( card != null && card != EquipmentRequest.UNEQUIP )
					{
						newMods.add( Modifiers.getItemModifiers( card.getItemId() ) );
					}
					break;
				}
				}
				mods = newMods;
			}
		}
		else if ( value instanceof SpecialOutfit )
		{
			mods = Modifiers.getModifiers( "Outfit", ((SpecialOutfit) value).getName() );
		}
		else if ( value instanceof FamiliarData && pane == GearChangeFrame.INSTANCE.customizablePanel )
		{
			mods = Modifiers.getModifiers( "Throne", ((FamiliarData) value).getRace() );
		}
		else
		{
			return;
		}

		if ( mods == null )
		{
			pane.getModifiersLabel().setText( "" );
			return;
		}

		String name = mods.getString( Modifiers.INTRINSIC_EFFECT );
		if ( name.length() > 0 )
		{
			Modifiers newMods = new Modifiers();
			newMods.add( mods );
			newMods.add( Modifiers.getModifiers( "Effect", name ) );
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

			ArrayList<VerifiableElement> rows = new ArrayList<>();
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

			rows.add( new AWoLClassVerifiableElement( "Holstered:", GearChangeFrame.this.equipment[ EquipmentManager.HOLSTER ] ) );

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
			elements = rows.toArray( elements );

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
			GearChangeFrame.this.famLockCheckbox.setEnabled( isEnabled );

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

		private class AWoLClassVerifiableElement
			extends HideableVerifiableElement
		{
			public AWoLClassVerifiableElement( final String label, final JComponent inputField )
			{
				super( label, inputField );
			}

			@Override
			public boolean isHidden()
			{
				return !KoLCharacter.isAWoLClass();
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

			ArrayList<VerifiableElement> rows = new ArrayList<>();
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

			rows.add( new VerifiableElement( "Card Sleeve:", GearChangeFrame.this.equipment[ EquipmentManager.CARDSLEEVE ] ) );

			rows.add( new VerifiableElement() );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER1 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER2 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER3 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER4 ] ) );
			rows.add( new VerifiableElement( "Folder:", GearChangeFrame.this.equipment[ EquipmentManager.FOLDER5 ] ) );

			rows.add( new VerifiableElement() );
			rows.add( new VerifiableElement( "Boot Skin:", GearChangeFrame.this.equipment[ EquipmentManager.BOOTSKIN ] ) );
			rows.add( new VerifiableElement( "Boot Spur:", GearChangeFrame.this.equipment[ EquipmentManager.BOOTSPUR ] ) );

			VerifiableElement[] elements = new VerifiableElement[ rows.size() ];
			elements = rows.toArray( elements );

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

			boolean hasCrownOfThrones = KoLCharacter.hasEquipped( EquipmentManager.CROWN_OF_THRONES );
			GearChangeFrame.this.crownSelect.setEnabled( isEnabled && hasCrownOfThrones );

			boolean hasBuddyBjorn = KoLCharacter.hasEquipped( EquipmentManager.BUDDY_BJORN );
			GearChangeFrame.this.bjornSelect.setEnabled( isEnabled && hasBuddyBjorn );

			boolean hasFakeHands = GearChangeFrame.this.fakeHands.getAvailableFakeHands() > 0;
			GearChangeFrame.this.fakeHands.setEnabled( isEnabled && hasFakeHands );

			boolean hasCardSleeve = EquipmentManager.CARD_SLEEVE.getCount( KoLConstants.inventory ) > 0 ||
				KoLCharacter.hasEquipped( EquipmentManager.CARD_SLEEVE );
			GearChangeFrame.this.equipment[ EquipmentManager.CARDSLEEVE ].setEnabled( isEnabled && hasCardSleeve );

			boolean hasFolderHolder = EquipmentManager.FOLDER_HOLDER.getCount( KoLConstants.inventory ) > 0 ||
				KoLCharacter.hasEquipped( EquipmentManager.FOLDER_HOLDER );
			boolean inHighSchool = KoLCharacter.inHighschool();

			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER1 ].setEnabled( isEnabled && hasFolderHolder );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER2 ].setEnabled( isEnabled && hasFolderHolder );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER3 ].setEnabled( isEnabled && hasFolderHolder );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER4 ].setEnabled( isEnabled && hasFolderHolder && inHighSchool );
			GearChangeFrame.this.equipment[ EquipmentManager.FOLDER5 ].setEnabled( isEnabled && hasFolderHolder && inHighSchool );

			boolean hasBoots = EquipmentManager.COWBOY_BOOTS.getCount( KoLConstants.inventory ) > 0 ||
				KoLCharacter.hasEquipped( EquipmentManager.COWBOY_BOOTS );
			GearChangeFrame.this.equipment[ EquipmentManager.BOOTSKIN ].setEnabled( isEnabled && hasBoots );
			GearChangeFrame.this.equipment[ EquipmentManager.BOOTSPUR ].setEnabled( isEnabled && hasBoots );
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
		AdventureResult card = (AdventureResult) this.equipment[ EquipmentManager.CARDSLEEVE ].getSelectedItem();
		if ( !EquipmentManager.getEquipment( EquipmentManager.CARDSLEEVE ).equals( card ) )
		{
			RequestThread.postRequest( new EquipmentRequest( card, EquipmentManager.CARDSLEEVE, true ) );
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

		// Cowboy Boots
		AdventureResult[] bootDecorations = new AdventureResult[] {
			(AdventureResult) this.equipment[ EquipmentManager.BOOTSKIN ].getSelectedItem(),
			(AdventureResult) this.equipment[ EquipmentManager.BOOTSPUR ].getSelectedItem(),
		};

		for ( int i = 0; i < bootDecorations.length; ++i )
		{
			AdventureResult decoration = bootDecorations[ i ];
			int slot = EquipmentManager.BOOTSKIN + i;
			if ( !EquipmentManager.getEquipment( slot ).equals( decoration ) )
			{
				RequestThread.postRequest( new EquipmentRequest( decoration, slot, true ) );
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
				EquipmentRequest request = new EquipmentRequest( EquipmentManager.FAKE_HAND, EquipmentManager.FAKEHAND );
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
		GearChangeFrame.INSTANCE.equipmentPanel.hideOrShowElements();
	}

	public static final void updateSlot( final int slot )
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		LockableListModel<AdventureResult> model = GearChangeFrame.INSTANCE.equipmentModels[slot];
		if ( model == null )
		{
			return;
		}

		model.setSelectedItem( EquipmentManager.getEquipment( slot ) );

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

	public static final void clearEquipmentModels()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
		{
			LockableListModel<AdventureResult> model = GearChangeFrame.INSTANCE.equipmentModels[ slot ];
			if ( model != null )
			{
				model.clear();
			}
		}
	}

	private class EquipmentComboBox
		extends JComboBox<AdventureResult>
	{
		public EquipmentComboBox( final LockableListModel<AdventureResult> model, final int slot )
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
				ComboBoxModel<AdventureResult> model = EquipmentComboBox.this.getModel();
				if ( model.getSize() == 0 )
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
		extends JComboBox<SpecialOutfit>
	{
		public OutfitComboBox( final LockableListModel<SpecialOutfit> model )
		{
			super( model );

			this.setRenderer( ListCellRendererFactory.getDefaultRenderer() );
			this.addActionListener( new ChangeOutfitListener() );
		}

		private class ChangeOutfitListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				ComboBoxModel<SpecialOutfit> model = OutfitComboBox.this.getModel();
				if ( model.getSize() == 0 )
				{
					return;
				}

				// If you're changing an outfit, then the
				// change must occur right away.

				SpecialOutfit outfit = (SpecialOutfit)OutfitComboBox.this.getSelectedItem();
				if ( outfit == null )
				{
					return;
				}
				synchronized ( GearChangeFrame.class )
				{
					RequestThread.postRequest( new EquipmentRequest( outfit ) );
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
		extends JComboBox<FamiliarData>
	{
		public CarriedFamiliarComboBox( final LockableListModel<FamiliarData> model )
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
		public ThroneComboBox( final LockableListModel<FamiliarData> model )
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
		public BjornComboBox( final LockableListModel<FamiliarData> model )
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
		extends JComboBox<FamiliarData>
	{
		public FamiliarComboBox( final LockableListModel<FamiliarData> model )
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
				ComboBoxModel<FamiliarData> model = FamiliarComboBox.this.getModel();
				if ( model.getSize() == 0 )
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

		// For all the slots that we maintain a custom list, update the model specially
		for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
		{
			LockableListModel<AdventureResult> model = equipmentModels[ slot ];
			if ( model == null )
			{
				continue;
			}
			AdventureResult item = (AdventureResult) this.equipment[ slot ].getSelectedItem();

			if ( slot == EquipmentManager.WEAPON || slot == EquipmentManager.OFFHAND )
			{
				if ( KoLCharacter.inFistcore() )
				{
					this.equipment[ slot ].setEnabled( false );
					continue;
				}
			}

			AdventureResult currentItem = EquipmentManager.getEquipment( slot );
			if ( item == null )
			{
				item = currentItem;
			}

			List<AdventureResult> items = this.validItems( slot, currentItem );
			this.updateEquipmentList( model, items, item );
			this.equipment[ slot ].setEnabled( this.isEnabled && !Limitmode.limitSlot( slot ) );

			if ( slot == EquipmentManager.WEAPON )
			{
				// Equipping 2 or more handed weapon: nothing in off-hand
				if ( EquipmentDatabase.getHands( item.getItemId() ) > 1 )
				{
					this.equipment[ EquipmentManager.OFFHAND ].setSelectedItem( EquipmentRequest.UNEQUIP );
					this.equipment[ EquipmentManager.OFFHAND ].setEnabled( false );
				}
			}
		}

		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
		FamiliarData selectedFamiliar = (FamiliarData) this.familiars.getSelectedItem();
		if ( selectedFamiliar == null )
		{
			selectedFamiliar = currentFamiliar;
		}
		if ( KoLCharacter.inPokefam() )
		{
			this.familiarSelect.setEnabled( false );
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

		this.updateFamiliarList( this.familiars, this.validFamiliars( currentFamiliar ), selectedFamiliar );
		this.equipment[ EquipmentManager.FAMILIAR ].setEnabled( this.isEnabled && !Limitmode.limitFamiliars() && !KoLCharacter.inPokefam() );
		this.updateFamiliarList( this.crownFamiliars, this.carriableFamiliars( currentFamiliar, bjornedFamiliar ), selectedThroneFamiliar );
		this.equipment[ EquipmentManager.CROWNOFTHRONES ].setEnabled( this.isEnabled && !Limitmode.limitFamiliars() && !KoLCharacter.inPokefam() );
		this.updateFamiliarList( this.bjornFamiliars, this.carriableFamiliars( currentFamiliar, enthronedFamiliar ), selectedBjornFamiliar );
		this.equipment[ EquipmentManager.BUDDYBJORN ].setEnabled( this.isEnabled && !Limitmode.limitFamiliars() && !KoLCharacter.inPokefam() );

		this.outfitSelect.setEnabled( this.isEnabled && !Limitmode.limitOutfits() );
		this.customSelect.setEnabled( this.isEnabled && !Limitmode.limitOutfits() );
	}

	private List<AdventureResult> validItems( final int slot, final AdventureResult currentItem )
	{
		List<AdventureResult> items = new ArrayList<>();

		switch ( slot )
		{
		case EquipmentManager.HAT:
			return validHatItems( currentItem );
		case EquipmentManager.WEAPON:
			return validWeaponItems( currentItem );
		case EquipmentManager.OFFHAND:
			return validOffhandItems( currentOrSelectedItem( EquipmentManager.WEAPON ), currentItem );
		case EquipmentManager.SHIRT:
			return validShirtItems( currentItem );
		case EquipmentManager.CONTAINER:
			return validContainerItems( currentItem );
		case EquipmentManager.PANTS:
			return validPantsItems( currentItem );
		case EquipmentManager.ACCESSORY1:
		case EquipmentManager.ACCESSORY2:
		case EquipmentManager.ACCESSORY3:
			return validAccessoryItems( currentItem );
		case EquipmentManager.BOOTSKIN:
			return validBootskinItems( currentItem );
		case EquipmentManager.BOOTSPUR:
			return validBootspurItems( currentItem );
		case EquipmentManager.HOLSTER:
			return validSixgunItems( currentItem );
		}

		// Search inventory for specified equipment type
		int consumption = EquipmentManager.equipmentTypeToConsumeFilter( slot );
		for ( AdventureResult item : KoLConstants.inventory )
		{
			addItem( items, item, consumption );
		}

		// Add the current item
		addItem( items, currentItem, consumption );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private AdventureResult currentOrSelectedItem( final int slot )
	{
		AdventureResult item = (AdventureResult) this.equipment[ slot ].getSelectedItem();
		return ( item != null ) ? item : EquipmentManager.getEquipment( slot );
	}

	private List<AdventureResult> validHatItems( final AdventureResult currentHat )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for hats
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
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
		if ( addItem( items, item, KoLConstants.EQUIP_HAT ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validPantsItems( final AdventureResult currentPants )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for pantss
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
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
		if ( addItem( items, item, KoLConstants.EQUIP_PANTS ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validShirtItems( final AdventureResult currentShirt )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for shirts
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
			addShirt( items, currentItem );
		}

		// Add the current shirt
		addShirt( items, currentShirt );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addShirt( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( addItem( items, item, KoLConstants.EQUIP_SHIRT ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validContainerItems( final AdventureResult currentContainer )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for containers
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
			addContainer( items, currentItem );
		}

		// Add the current container
		addContainer( items, currentContainer );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addContainer( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( addItem( items, item, KoLConstants.EQUIP_CONTAINER ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validWeaponItems( final AdventureResult currentWeapon )
	{
		List<AdventureResult> items = new ArrayList<>();

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
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
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
		if ( addItem( items, item, KoLConstants.EQUIP_WEAPON ) &&
		     filterWeapon( item ) )
		{
			items.add( item );
		}
	}

	private boolean filterWeapon( final AdventureResult weapon )
	{
		if ( this.weapon1H.isSelected() && EquipmentDatabase.getHands( weapon.getItemId() ) > 1 )
		{
			return false;
		}

		if ( this.weaponTypes[ 0 ].isSelected() )
		{
			return true;
		}

		switch ( EquipmentDatabase.getWeaponType( weapon.getItemId() ) )
		{
		case  MELEE:
			return this.weaponTypes[ 1 ].isSelected();
		case  RANGED:
			return this.weaponTypes[ 2 ].isSelected();
		}
		return false;
	}

	private List<AdventureResult> validAccessoryItems( final AdventureResult currentAccessory )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for accessories
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
			addAccessory( items, currentItem );
		}

		// Add the current accessory
		addAccessory( items, currentAccessory );

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addAccessory( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( addItem( items, item, KoLConstants.EQUIP_ACCESSORY ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validOffhandItems( final AdventureResult weapon, final AdventureResult offhandItem )
	{
		List<AdventureResult> items = new ArrayList<>();

		// In Fistcore, you must have both hands free.
		// In Axecore, you can equip only Trusty, a two-handed axe
		if ( KoLCharacter.inFistcore() || KoLCharacter.inAxecore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			return items;
		}

		// Find all offhand items that are compatible with the selected weapon.

		// We can have weapons if we can dual wield and there is
		// one-handed weapon in the main hand
		boolean weapons =
			EquipmentDatabase.getHands( weapon.getItemId() ) == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );

		// The type of weapon in the off hand must
		// agree with the weapon in the main hand
		WeaponType type = EquipmentDatabase.getWeaponType( weapon.getItemId() );

		// Search inventory for suitable items

		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
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
			if ( type != EquipmentDatabase.getWeaponType( currentItem.getItemId() ) )
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

	private List<AdventureResult> validBootskinItems( final AdventureResult currentBootskin )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for containers
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
			addBootskins( items, currentItem );
		}

		// Add the current skin
		addBootskins( items, currentBootskin );

		// There is no way to remove skins, but if there isn't currently a skin applied then
		// that state needs to be represented
		if ( EquipmentManager.getEquipment( EquipmentManager.BOOTSKIN ).equals( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addBootskins( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( addItem( items, item, KoLConstants.CONSUME_BOOTSKIN ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validBootspurItems( final AdventureResult currentBootspur )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for containers
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
			addBootspurs( items, currentItem );
		}

		// Add the current container
		addBootspurs( items, currentBootspur );

		// There is no way to remove spurs, but if there isn't currently a spur applied then
		// that state needs to be represented
		if ( EquipmentManager.getEquipment( EquipmentManager.BOOTSPUR ).equals( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addBootspurs( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( addItem( items, item, KoLConstants.CONSUME_BOOTSPUR ) )
		{
			items.add( item );
		}
	}

	private List<AdventureResult> validSixgunItems( final AdventureResult currentSixgun )
	{
		List<AdventureResult> items = new ArrayList<>();

		// Search inventory for sixguns
		for ( AdventureResult currentItem : KoLConstants.inventory )
		{
			addSixgun( items, currentItem );
		}

		// The current sixgun is still in inventory

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addSixgun( final List<AdventureResult> items, final AdventureResult item )
	{
		if ( addItem( items, item, KoLConstants.CONSUME_SIXGUN ) )
		{
			items.add( item );
		}
	}

	private boolean addItem( final List<AdventureResult> items, final AdventureResult item )
	{
		// Only add it once
		if ( items.contains( item ) )
		{
			return false;
		}

		// Make sure we meet requirements in Limitmode, otherwise show (greyed out)
		if ( KoLCharacter.getLimitmode() != null && !EquipmentManager.canEquip( item.getName() ) )
		{
			return false;
		}

		return true;
	}

	private boolean addItem( final List<AdventureResult> items, final AdventureResult item, final int type )
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

		// Make sure we meet requirements in Limitmode, otherwise show (greyed out)
		if ( KoLCharacter.getLimitmode() != null && !EquipmentManager.canEquip( item.getName() ) )
		{
			return false;
		}

		return true;
	}

	private static void updateEquipmentListInternal( final LockableListModel<AdventureResult> currentItems, final List<AdventureResult> newItems, final AdventureResult equippedItem )
	{
		currentItems.retainAll( newItems );
		newItems.removeAll( currentItems );
		currentItems.addAll( newItems );
		currentItems.setSelectedItem( equippedItem );
	}

	private void updateEquipmentList( final LockableListModel<AdventureResult> currentItems, final List<AdventureResult> newItems, final AdventureResult equippedItem )
	{
		if ( SwingUtilities.isEventDispatchThread() )
		{
			updateEquipmentListInternal( currentItems, newItems, equippedItem );
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait( new Runnable()
				{
					public void run()
					{
						updateEquipmentListInternal( currentItems, newItems, equippedItem );
					}
				} );
			}
			catch ( Exception ie )
			{
			}
		}
	}

	private List<FamiliarData> validFamiliars( final FamiliarData currentFamiliar )
	{
		List<FamiliarData> familiars = new ArrayList<>();

		for ( FamiliarData fam : KoLCharacter.getFamiliarList() )
		{
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

	private List<FamiliarData> carriableFamiliars( final FamiliarData exclude1, final FamiliarData exclude2 )
	{
		List<FamiliarData> familiars = new ArrayList<>();

		for ( FamiliarData fam : KoLCharacter.getFamiliarList() )
		{
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

	private void updateFamiliarList( final LockableListModel<FamiliarData> currentFamiliars, final List<FamiliarData> newFamiliars, final FamiliarData activeFamiliar )
	{
		currentFamiliars.retainAll( newFamiliars );
		newFamiliars.removeAll( currentFamiliars );
		currentFamiliars.addAll( newFamiliars );
		currentFamiliars.setSelectedItem( activeFamiliar );
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
			int available = EquipmentManager.FAKE_HAND.getCount( KoLConstants.inventory );
			this.currentFakeHands = EquipmentManager.getFakeHands();
			this.availableFakeHands = this.currentFakeHands + available;
			this.setValue( IntegerPool.get( this.currentFakeHands ) );
		}
	}

	private class FamLockCheckbox
		extends JCheckBox
		implements Listener
	{
		public FamLockCheckbox()
		{
			super( "familiar item locked" );
			this.addActionListener( new LockFamiliarItemListener() );
			NamedListenerRegistry.registerNamedListener( "(familiarLock)", this );
			this.update();
		}

		private class LockFamiliarItemListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				RequestThread.postRequest( new FamiliarRequest( true ) );
			}
		}

		public void update()
		{
			this.setSelected( EquipmentManager.familiarItemLocked() );
			this.setEnabled( this.isEnabled() );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled && EquipmentManager.familiarItemLockable() );
		}
	}
}
