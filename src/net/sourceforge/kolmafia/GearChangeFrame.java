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

// event listeners
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
	private JComboBox [] equipment;
	private LockableListModel [] equipmentLists;
	private JComboBox outfitSelect, familiarSelect;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 *
	 * @param	StaticEntity.getClient()	The StaticEntity.getClient() containing the data associated with the character
	 */

	public GearChangeFrame()
	{
		super( "Gear Changer" );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( createEquipPanel(), "" );
		refreshEquipPanel();
	}

	public void dispose()
	{
		for ( int i = 0; i < equipment.length; ++i )
			equipmentLists[i].removeListDataListener( equipment[i] );

		KoLCharacter.getOutfits().removeListDataListener( outfitSelect );
		KoLCharacter.getFamiliarList().removeListDataListener( familiarSelect );

		super.dispose();
	}

	/**
	 * Sets all of the internal panels to a disabled or enabled state; this
	 * prevents the user from modifying the data as it's getting sent, leading
	 * to uncertainty and generally bad things.
	 */

	public void setEnabled( boolean isEnabled )
	{
		if ( equipment != null )
		{
			for ( int i = 0; i < equipment.length; ++i )
			{
				if ( isEnabled )
				{
					// Do not enable off-hand if character
					// has a big weapon

					if ( i == KoLCharacter.OFFHAND && KoLCharacter.weaponHandedness() > 1 )
						continue;

					// Enable shirts only if character has
					// Torso Awaregness skill

					if ( i == KoLCharacter.SHIRT && !KoLCharacter.hasSkill( "Torso Awaregness" ) )
						continue;
				}

				equipment[i].setEnabled( isEnabled );
			}
		}

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
		JPanel fieldPanel = new JPanel( new GridLayout( 16, 1 ) );
		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Hat:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Weapon:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Off-Hand:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Shirt:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Pants:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Familiar:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Item:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( " " ) );
		fieldPanel.add( new JLabel( "Outfit:  ", JLabel.RIGHT ) );

		JPanel valuePanel = new JPanel( new GridLayout( 16, 1 ) );
		valuePanel.add( new JLabel( " " ) );

		equipment = new JComboBox[9];
		equipmentLists = KoLCharacter.getEquipmentLists();

		for ( int i = 0; i < 5; ++i )
		{
			equipment[i] = new ChangeComboBox( equipmentLists[i], EquipmentRequest.class, String.class, new Integer(i) );
			JComponentUtilities.setComponentSize( equipment[i], 300, 20 );
			valuePanel.add( equipment[i] );
		}

		valuePanel.add( new JLabel( " " ) );

		for ( int i = 5; i < 8; ++i )
		{
			equipment[i] = new ChangeComboBox( equipmentLists[i], EquipmentRequest.class, String.class, new Integer(i) );
			JComponentUtilities.setComponentSize( equipment[i], 300, 20 );
			valuePanel.add( equipment[i] );
		}

		valuePanel.add( new JLabel( " " ) );

		familiarSelect = new ChangeComboBox( KoLCharacter.getFamiliarList(), FamiliarRequest.class, FamiliarData.class );
		JComponentUtilities.setComponentSize( familiarSelect, 300, 20 );
		valuePanel.add( familiarSelect );

		equipment[8] = new ChangeComboBox( equipmentLists[8], EquipmentRequest.class, String.class, new Integer(8) );
		JComponentUtilities.setComponentSize( equipment[8], 300, 20 );
		valuePanel.add( equipment[8] );

		valuePanel.add( new JLabel( " " ) );

		outfitSelect = new ChangeComboBox( KoLCharacter.getOutfits(), EquipmentRequest.class, SpecialOutfit.class );
		JComponentUtilities.setComponentSize( outfitSelect, 300, 20 );
		valuePanel.add( outfitSelect );

		JPanel equipPanel = new JPanel( new BorderLayout() );
		equipPanel.add( fieldPanel, BorderLayout.WEST );
		equipPanel.add( valuePanel, BorderLayout.CENTER );

		return equipPanel;
	}

	private void refreshEquipPanel()
	{
		setEnabled( false );
		outfitSelect.setSelectedItem( null );
		KoLCharacter.updateEquipmentLists();
		setEnabled( true );
	}

	private class ChangeComboBox extends JComboBox implements Runnable
	{
		private Integer slot;
		private Object [] parameters;
		private Constructor constructor;

		public ChangeComboBox( LockableListModel selector, Class requestClass, Class parameterClass )
		{
			super( selector );
			addActionListener( this );

			Class [] parameterTypes = new Class[2];
			parameterTypes[0] = KoLmafia.class;
			parameterTypes[1] = parameterClass;

			initialize( requestClass, parameterTypes );
			this.slot = null;
		}

		public ChangeComboBox( LockableListModel selector, Class requestClass, Class parameterClass, Integer slot )
		{
			super( selector );
			addActionListener( this );

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
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}

			this.parameters = new Object[ parameterTypes.length ];
			this.parameters[0] = StaticEntity.getClient();
			for ( int i = 1; i < parameters.length; ++i )
				this.parameters[i] = null;
		}

		public void actionPerformed( ActionEvent e )
		{	return;
		}

		public void firePopupMenuWillBecomeInvisible()
		{
			super.firePopupMenuWillBecomeInvisible();

			if ( !isShowing() || !isEnabled() )
				return;

			executeChange();
		}

		public void executeChange()
		{
			parameters[1] = getSelectedItem();

			if ( parameters[1] == null )
				return;

			// In order to avoid constant misfiring of the table,
			// make sure that the change thread is not started
			// unless your current equipment does not match the
			// selected equipment.

			if ( ( slot != null && parameters[1].equals( KoLCharacter.getEquipment( slot.intValue() ) ) ) ||
			     ( this == familiarSelect && parameters[1].equals( KoLCharacter.getFamiliar() ) ) ||
			     ( this == outfitSelect && !( parameters[1] instanceof SpecialOutfit ) ) )
				return;

			(new RequestThread( this )).start();
		}

		public void run()
		{
			try
			{
				StaticEntity.getClient().makeRequest( (Runnable) constructor.newInstance( parameters ), 1 );
				refreshEquipPanel();
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}
		}
	}
}
