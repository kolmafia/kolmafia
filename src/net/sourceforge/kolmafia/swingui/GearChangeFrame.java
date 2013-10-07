/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.objectpool.FamiliarPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GearChangeFrame
	extends GenericFrame
	implements PreferenceListener
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
	private final OutfitComboBox outfitSelect, customSelect;
	private final FamiliarComboBox familiarSelect;
	private JLabel sticker1Label, sticker2Label, sticker3Label;
	private FamLockCheckbox famLockCheckbox;

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

			this.equipment[ i ] = new EquipmentComboBox( list, i == EquipmentManager.FAMILIAR );
		}

		this.familiarSelect = new FamiliarComboBox( this.familiars );
		this.outfitSelect = new OutfitComboBox( EquipmentManager.getOutfits() );
		this.customSelect = new OutfitComboBox( EquipmentManager.getCustomOutfits() );

		this.tabs.addTab( "Equipment", new EquipmentPanel() );
		this.tabs.addTab( "Customizable", new CustomizablePanel() );

		JPanel gearPanel = new JPanel( new BorderLayout() );
		gearPanel.add( this.tabs, BorderLayout.CENTER );
		this.setCenterComponent( gearPanel );

		GearChangeFrame.INSTANCE = this;

		RequestThread.executeMethodAfterInitialization( this, "validateSelections" );
	}

	public void update()
	{
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
		protected int modifiersWidth;
		protected JLabel modifiersLabel;

		public EquipmentTabPanel( final String confirmedText, final String cancelledText, Dimension left, Dimension right )
		{
			super( confirmedText, cancelledText, left, right );
		}

		public EquipmentTabPanel( final String confirmedText, Dimension left, Dimension right )
		{
			super( confirmedText, null, left, right );
		}

		public int getModifiersWidth()
		{
			return this.modifiersWidth;
		}

		public JLabel getModifiersLabel()
		{
			return this.modifiersLabel;
		}
	}

	private class EquipmentPanel
		extends EquipmentTabPanel
	{
		public EquipmentPanel()
		{
			super( "change gear", "save as outfit", new Dimension( 120, 20 ), new Dimension( 300, 20 ) );

			ArrayList rows = new ArrayList<VerifiableElement>();
			VerifiableElement element;

			rows.add( new VerifiableElement( "Hat: ", GearChangeFrame.this.equipment[ EquipmentManager.HAT ] ) );
			rows.add( new VerifiableElement( "Weapon: ", GearChangeFrame.this.equipment[ EquipmentManager.WEAPON ] ) );

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

			rows.add( new VerifiableElement( "Off-Hand: ", GearChangeFrame.this.equipment[ EquipmentManager.OFFHAND ] ) );

			JPanel radioPanel2 = new JPanel( new GridLayout( 1, 5 ) );
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

			rows.add( new VerifiableElement( "Back: ", GearChangeFrame.this.equipment[ EquipmentManager.CONTAINER ] ) );

			rows.add( new VerifiableElement( "Shirt: ", GearChangeFrame.this.equipment[ EquipmentManager.SHIRT ] ) );
			rows.add( new VerifiableElement( "Pants: ", GearChangeFrame.this.equipment[ EquipmentManager.PANTS ] ) );

			rows.add( new VerifiableElement() );

			rows.add( new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY1 ] ) );
			rows.add( new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY2 ] ) );
			rows.add( new VerifiableElement( "Accessory: ", GearChangeFrame.this.equipment[ EquipmentManager.ACCESSORY3 ] ) );

			rows.add( new VerifiableElement() );

			rows.add( new VerifiableElement( "Familiar: ", GearChangeFrame.this.familiarSelect ) );
			rows.add( new VerifiableElement( "Fam Item: ", GearChangeFrame.this.equipment[ EquipmentManager.FAMILIAR ] ) );

			GearChangeFrame.this.famLockCheckbox = new FamLockCheckbox();
			JPanel boxholder = new JPanel( new BorderLayout() );
			boxholder.add( GearChangeFrame.this.famLockCheckbox );
			rows.add( new VerifiableElement( "", boxholder ) );
			GearChangeFrame.updateFamiliarLock();

			rows.add( new VerifiableElement( "Outfit: ", GearChangeFrame.this.outfitSelect ) );
			rows.add( new VerifiableElement( "Custom: ", GearChangeFrame.this.customSelect ) );

			VerifiableElement[] elements = new VerifiableElement[ rows.size() ];
			elements = (VerifiableElement[])rows.toArray( elements );

			this.setContent( elements );

			GearChangeFrame.this.outfitButton = this.cancelledButton;
			this.modifiersWidth = this.eastContainer.getPreferredSize().width;
			this.modifiersLabel = new JLabel();
			this.confirmedButton.getParent().getParent().add( this.modifiersLabel, BorderLayout.CENTER );
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
			GearChangeFrame.updateFamiliarLock();

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
		private final AutoHighlightTextField fakeHands;

		public CustomizablePanel()
		{
			super( "change gear", new Dimension( 120, 20 ), new Dimension( 300, 20 ) );

			ArrayList rows = new ArrayList<VerifiableElement>();
			VerifiableElement element;

			rows.add(  new VerifiableElement( "Crown of Thrones: ", GearChangeFrame.this.equipment[ EquipmentManager.CROWN_OF_THRONES ] ) );

			rows.add( new VerifiableElement() );

			element = new VerifiableElement( "Sticker: ", GearChangeFrame.this.equipment[ EquipmentManager.STICKER1 ]  );
			GearChangeFrame.this.sticker1Label = element.getLabel();
			rows.add( element );

			element = new VerifiableElement( "Sticker: ", GearChangeFrame.this.equipment[ EquipmentManager.STICKER2 ]  );
			GearChangeFrame.this.sticker2Label = element.getLabel();
			rows.add( element );

			element = new VerifiableElement( "Sticker: ", GearChangeFrame.this.equipment[ EquipmentManager.STICKER3 ]  );
			GearChangeFrame.this.sticker3Label = element.getLabel();
			rows.add( element );

			rows.add( new VerifiableElement() );

			this.fakeHands = new AutoHighlightTextField();
			rows.add( new VerifiableElement( "Fake Hands: ", this.fakeHands ) );

			rows.add( new VerifiableElement() );

			rows.add( new VerifiableElement( "Card Sleeve: ", GearChangeFrame.this.equipment[ EquipmentManager.CARD_SLEEVE ] ) );

			VerifiableElement[] elements = new VerifiableElement[ rows.size() ];
			elements = (VerifiableElement[])rows.toArray( elements );

			this.setContent( elements );

			this.modifiersWidth = this.eastContainer.getPreferredSize().width;
			this.modifiersLabel = new JLabel();
			this.confirmedButton.getParent().getParent().add( this.modifiersLabel, BorderLayout.CENTER );
			this.setEnabled( true );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );

			// EquipmentManager.getEquipment( EquipmentManager.HAT ).getItemId() == ItemPool.HATSEAT
			GearChangeFrame.this.equipment[ EquipmentManager.CROWN_OF_THRONES ].setEnabled( false );

			this.fakeHands.setEnabled( false );

			// EquipmentManager.getEquipment( EquipmentManager.OFFHAND ).getItemId() == ItemPool.CARD_SLEEVE
			GearChangeFrame.this.equipment[ EquipmentManager.CARD_SLEEVE ].setEnabled( false );
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
		// Find out what changed

		AdventureResult[] pieces = new AdventureResult[ EquipmentManager.ALL_SLOTS ];

		// Start with first pseudo-slot
		for ( int i = EquipmentManager.SLOTS; i < pieces.length; ++i )
		{
			pieces[ i ] = (AdventureResult) this.equipment[ i ].getSelectedItem();
			if ( EquipmentManager.getEquipment( i ).equals( pieces[ i ] ) )
			{
				pieces[ i ] = null;
			}
		}

		// *** Crown of Thrones

		for ( int i = EquipmentManager.STICKER1; i <= EquipmentManager.STICKER3; ++i )
		{
			if ( pieces[ i ] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[ i ], i, true ) );
				pieces[ i ] = null;
			}
		}

		// *** fake hands
		// *** card sleeve
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

		GearChangeFrame.INSTANCE.familiars.setSelectedItem( KoLCharacter.getFamiliar() );
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

		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
		FamiliarData selectedFamiliar = (FamiliarData) this.familiars.getSelectedItem();
		if ( selectedFamiliar == null )
		{
			selectedFamiliar = currentFamiliar;
		}
		List familiars = this.validFamiliars( currentFamiliar );
		this.updateEquipmentList( this.familiars, familiars, selectedFamiliar );
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
			if ( EquipmentDatabase.getHands( currentItem.getName() ) != 1 )
			{
				return false;
			}
			if ( type != EquipmentDatabase.getWeaponType( currentItem.getName() ) )
			{
				return false;
			}
			if ( EquipmentDatabase.getItemType( currentItem.getItemId() ).equals( "chefstaff" ) )
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

	private void updateEquipmentList( final LockableListModel currentItems, final List newItems,
		final Object equippedItem )
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
