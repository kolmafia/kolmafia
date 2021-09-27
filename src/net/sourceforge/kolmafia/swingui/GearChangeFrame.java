package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
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
import javax.swing.SwingUtilities;

import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.dev.spellcast.utilities.ActionVerifyPanel.HideableVerifiableElement;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
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
			case EquipmentManager.FAMILIAR:
			case EquipmentManager.BOOTSKIN:
			case EquipmentManager.BOOTSPUR:
			case EquipmentManager.HOLSTER:
				list = this.equipmentModels[ i ] = new SortedListModel<>();
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

		Modifiers mods;

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
				case ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE:
				{
					newMods.add( Modifiers.getModifiers( "RetroCape", Preferences.getString( "retroCapeSuperhero" ) + " " + Preferences.getString( "retroCapeWashingInstructions" ) ) );
					break;
				}
				case ItemPool.BACKUP_CAMERA:
				{
					newMods.add( Modifiers.getModifiers( "BackupCamera", Preferences.getString( "backupCameraMode" ) ) );
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
				case ItemPool.VAMPYRIC_CLOAKE:
					newMods.applyVampyricCloakeModifiers();
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
			     i == Modifiers.FAMILIAR_EFFECT )
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

			for ( JRadioButton weaponType : weaponTypes )
			{
				radioGroup1.add( weaponType );
				radioPanel1.add( weaponType );
				weaponType.addActionListener( new RefilterListener() );
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

			for ( JRadioButton offhandType : offhandTypes )
			{
				radioGroup2.add( offhandType );
				radioPanel2.add( offhandType );
				offhandType.addActionListener( new RefilterListener() );
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
			if ( !KoLCharacter.inQuantum() )
			{
				boxholder.add( GearChangeFrame.this.famLockCheckbox );
			}
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
			GearChangeFrame.this.fakeHands.getEditor().setBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED) );

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
		int newFakeHands = (Integer) this.fakeHands.getValue();
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

		if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS )
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

	private boolean slotItemCanBeNone( final int slot)
	{
		switch ( slot )
		{
		case EquipmentManager.BOOTSKIN:
		case EquipmentManager.BOOTSPUR:
			// You cannot remove the item in this slot, but if
			// nothing is equipped, need a placeholder
			return EquipmentManager.getEquipment( slot ).equals( EquipmentRequest.UNEQUIP );
		default:
			return true;
		}
	}

	private FamiliarData familiarCarryingEquipment( final int slot)
	{
		switch ( slot )
		{
		case EquipmentManager.HAT:
			return KoLCharacter.findFamiliar( FamiliarPool.HATRACK );
		case EquipmentManager.PANTS:
			return KoLCharacter.findFamiliar( FamiliarPool.SCARECROW );
		case EquipmentManager.WEAPON:
			return KoLCharacter.findFamiliar( FamiliarPool.HAND );
		case EquipmentManager.OFFHAND:
			return KoLCharacter.findFamiliar( FamiliarPool.LEFT_HAND );
		default:
			return null;
		}
	}

	private List<AdventureResult>[] populateEquipmentLists()
	{
		List<AdventureResult>[] lists = new ArrayList[ EquipmentManager.ALL_SLOTS ];

		// Create all equipment lists
		for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
		{
			List<AdventureResult> items = new ArrayList<>();
			
			// Almost every list gets a "(none)"
			if ( this.slotItemCanBeNone( slot ) )
			{
				items.add( EquipmentRequest.UNEQUIP );
			}

			lists[ slot ] = items;
		}

		// Certain familiars can carry non-familiar-items
		FamiliarData myFamiliar = KoLCharacter.getFamiliar();
		int specialFamiliarType = myFamiliar.specialEquipmentType();
		boolean specialFamiliar = ( specialFamiliarType != KoLConstants.NO_CONSUME );

		// Look at every item in inventory
		for ( AdventureResult item : KoLConstants.inventory )
		{
			int consumption = ItemDatabase.getConsumptionType( item.getItemId() );
			int slot = EquipmentManager.consumeFilterToEquipmentType( consumption );
			switch( consumption )
			{
			case KoLConstants.EQUIP_WEAPON:
				if ( this.shouldAddItem( item, consumption, EquipmentManager.WEAPON ) )
				{
					lists[ EquipmentManager.WEAPON ].add( item );
				}
				if ( this.shouldAddItem( item, consumption, EquipmentManager.OFFHAND ) )
				{
					lists[ EquipmentManager.OFFHAND ].add( item );
				}
				break;

			case KoLConstants.EQUIP_ACCESSORY:
				if ( this.shouldAddItem( item, consumption, slot ) )
				{
					lists[ EquipmentManager.ACCESSORY1 ].add( item );
					lists[ EquipmentManager.ACCESSORY2 ].add( item );
					lists[ EquipmentManager.ACCESSORY3 ].add( item );
				}
				break;

			/*
			case KoLConstants.CONSUME_STICKER:
				if ( this.shouldAddItem( item, consumption, slot ) )
				{
					lists[ EquipmentManager.STICKER1 ].add( item );
					lists[ EquipmentManager.STICKER2 ].add( item );
					lists[ EquipmentManager.STICKER3 ].add( item );
				}
				break;

			case KoLConstants.CONSUME_FOLDER:
				if ( this.shouldAddItem( item, consumption, slot ) )
				{
					lists[ EquipmentManager.FOLDER1 ].add( item );
					lists[ EquipmentManager.FOLDER2 ].add( item );
					lists[ EquipmentManager.FOLDER3 ].add( item );
					lists[ EquipmentManager.FOLDER4 ].add( item );
					lists[ EquipmentManager.FOLDER5 ].add( item );
				}
				break;
			*/

			default:
				if ( this.shouldAddItem( item, consumption, slot ) )
				{
					lists[ slot ].add( item );
				}
				break;
			}

			if ( specialFamiliar && (consumption == specialFamiliarType ) && myFamiliar.canEquip( item ) )
			{
				lists[ EquipmentManager.FAMILIAR ].add( item );
			}
		}

		// Add current equipment
		for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
		{
			List<AdventureResult> items = lists[ slot ];
			if ( items == null )
			{
				continue;
			}

			AdventureResult currentItem = EquipmentManager.getEquipment( slot );
			if ( !items.contains( currentItem ) && this.filterItem( currentItem, slot ) )
			{
				items.add( currentItem );
			}

			// If a non-current familiar has an appropriate item, add it.
			if ( slot != EquipmentManager.FAMILIAR )
			{
				FamiliarData familiar = familiarCarryingEquipment( slot );
				if ( familiar != null && familiar != KoLCharacter.getFamiliar() )
				{
					AdventureResult familiarItem = familiar.getItem();
					if ( !items.contains( familiarItem ) && this.filterItem( familiarItem, slot ) )
					{
						items.add( familiarItem );
					}
				}
			}
		}

		// Add stealable familiar equipment
		if ( myFamiliar != FamiliarData.NO_FAMILIAR )
		{
			List<AdventureResult> items = lists[ EquipmentManager.FAMILIAR ];
			for ( FamiliarData familiar : KoLCharacter.familiars )
			{
				if ( familiar == myFamiliar )
				{
					continue;
				}
				AdventureResult famItem = familiar.getItem();
				if ( famItem != EquipmentRequest.UNEQUIP &&
				     myFamiliar.canEquip( famItem ) &&
				     !items.contains( famItem ) )
				{
					items.add( famItem );
				}
			}
		}

		return lists;
	}

	private boolean filterItem( AdventureResult item, int slot )
	{
		return this.shouldAddItem( item, ItemDatabase.getConsumptionType( item.getItemId() ), slot );
	}

	private boolean shouldAddItem( AdventureResult item, int consumption, int slot )
	{
		switch ( consumption )
		{
			// The following lists are local to GearChanger
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.CONSUME_BOOTSKIN:
		case KoLConstants.CONSUME_BOOTSPUR:
		case KoLConstants.CONSUME_SIXGUN:
			break;
		case KoLConstants.EQUIP_WEAPON:
			if ( !this.filterWeapon( item, slot ) )
			{
				return false;
			}
			break;
		case KoLConstants.EQUIP_OFFHAND:
			if ( !this.filterOffhand( item, KoLConstants.EQUIP_OFFHAND ) )
			{
				return false;
			}
			break;
		case KoLConstants.EQUIP_FAMILIAR:
			if ( !KoLCharacter.getFamiliar().canEquip( item ) )
			{
				return false;
			}
			break;
			// The following lists are in EquipmentManager
		case KoLConstants.CONSUME_STICKER:
		case KoLConstants.CONSUME_CARD:
		case KoLConstants.CONSUME_FOLDER:
			break;
		default:
			return false;
		}

		return KoLCharacter.getLimitmode() == null || EquipmentManager.canEquip( item );
	}

	private boolean filterWeapon( final AdventureResult weapon, final int slot )
	{
		if ( KoLCharacter.inFistcore() )
		{
			return false;
		}

		if ( slot == EquipmentManager.OFFHAND )
		{
			return this.filterOffhand( weapon, KoLConstants.EQUIP_WEAPON );
		}

		if ( KoLCharacter.inAxecore() )
		{
			return weapon.getItemId() == ItemPool.TRUSTY;
		}

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
		case MELEE:
			return this.weaponTypes[ 1 ].isSelected();
		case RANGED:
			return this.weaponTypes[ 2 ].isSelected();
		}
		return false;
	}

	private boolean filterOffhand( final AdventureResult offhand, int consumption )
	{
		// In Fistcore, you must have both hands free.
		// In Axecore, you can equip only Trusty, a two-handed axe
		if ( KoLCharacter.inFistcore() || KoLCharacter.inAxecore() )
		{
			return false;
		}

		int offhandId = offhand.getItemId();

		// Fake hands are handled specially
		if ( offhandId == ItemPool.FAKE_HAND )
		{
			return false;
		}

		// Do not even consider weapons unless we can dual-wield
		if ( consumption == KoLConstants.EQUIP_WEAPON )
		{
			if ( !KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" ) )
			{
				return false;
			}
	
			// Only consider 1-handed weapons
			if ( EquipmentDatabase.getHands( offhandId ) != 1 )
			{
				return false;
			}

			// There must be a current weapon
			AdventureResult weapon = this.currentOrSelectedItem( EquipmentManager.WEAPON );
			if ( weapon == EquipmentRequest.UNEQUIP )
			{
				return false;
			}

			// The current weapon must be 1-handed
			int weaponId = weapon.getItemId();
			if ( EquipmentDatabase.getHands( weaponId ) != 1 )
			{
				return false;
			}

			if ( EquipmentDatabase.isMainhandOnly( offhandId ) )
			{
				return false;
			}

			// The weapon types must agree
			if ( EquipmentDatabase.getWeaponType( weaponId ) !=
			     EquipmentDatabase.getWeaponType( offhandId ) )
			{
				return false;
			}

			// Now check filters
		}

		if ( this.offhandTypes[ 0 ].isSelected() )
		{
			return true;
		}

		if ( consumption == KoLConstants.EQUIP_WEAPON )
		{
			return this.offhandTypes[ 1 ].isSelected();
		}

		String type = EquipmentDatabase.getItemType( offhandId );
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

	private boolean shouldAddItem( final AdventureResult item, final int type )
	{
		// Only add items of specified type
		if ( type != ItemDatabase.getConsumptionType( item.getItemId() ) )
		{
			return false;
		}

		// Make sure we meet requirements in Limitmode, otherwise show (greyed out)
		return KoLCharacter.getLimitmode() == null || EquipmentManager.canEquip( item.getName() );
	}

	private AdventureResult currentOrSelectedItem( final int slot )
	{
		AdventureResult item = (AdventureResult) this.equipment[ slot ].getSelectedItem();
		return ( item != null ) ? item : EquipmentManager.getEquipment( slot );
	}

	private static void updateEquipmentList( final LockableListModel<AdventureResult> currentItems, final List<AdventureResult> newItems, final AdventureResult equippedItem )
	{
		currentItems.retainAll( newItems );
		newItems.removeAll( currentItems );
		currentItems.addAll( newItems );
		currentItems.setSelectedItem( equippedItem );
	}

	private void updateEquipmentModelsInternal( final List<AdventureResult>[] equipmentLists )
	{
		// For all the slots that we maintain a custom list, update the model specially
		for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
		{
			LockableListModel<AdventureResult> model = equipmentModels[ slot ];
			if ( model == null )
			{
				continue;
			}

			if ( slot == EquipmentManager.WEAPON || slot == EquipmentManager.OFFHAND )
			{
				if ( KoLCharacter.inFistcore() )
				{
					this.equipment[ slot ].setEnabled( false );
					continue;
				}
			}

			List<AdventureResult> items = equipmentLists[ slot ];
			AdventureResult selectedItem = this.currentOrSelectedItem( slot );
			this.updateEquipmentList( model, items, selectedItem );
			this.equipment[ slot ].setEnabled( this.isEnabled && !Limitmode.limitSlot( slot ) );

			if ( slot == EquipmentManager.WEAPON )
			{
				// Equipping 2 or more handed weapon: nothing in off-hand
				if ( EquipmentDatabase.getHands( selectedItem.getItemId() ) > 1 )
				{
					this.equipment[ EquipmentManager.OFFHAND ].setSelectedItem( EquipmentRequest.UNEQUIP );
					this.equipment[ EquipmentManager.OFFHAND ].setEnabled( false );
				}
			}
		}
	}

	private void updateEquipmentModels( final List<AdventureResult>[] equipmentLists )
	{
		if ( SwingUtilities.isEventDispatchThread() )
		{
			updateEquipmentModelsInternal( equipmentLists );
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait( () -> updateEquipmentModelsInternal( equipmentLists ) );
			}
			catch ( Exception ie )
			{
			}
		}
	}

	private void ensureValidSelections()
	{
		// If we are still logging in, defer this
		if ( KoLmafia.isRefreshing() )
		{
			return;
		}

		// Calculate all the AdventureResult lists
		List<AdventureResult>[] equipmentLists = this.populateEquipmentLists();

		// Update the models in the Swing Thread
		this.updateEquipmentModels( equipmentLists );

		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
		FamiliarData selectedFamiliar = (FamiliarData) this.familiars.getSelectedItem();
		if ( selectedFamiliar == null )
		{
			selectedFamiliar = currentFamiliar;
		}
		if ( KoLCharacter.inPokefam() || KoLCharacter.inQuantum() )
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
