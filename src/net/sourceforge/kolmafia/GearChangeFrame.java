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
import java.util.ArrayList;
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
	private Object outfit = null;
	private FamiliarData familiar = null;
	private String [] pieces = new String[9];

	private ChangeComboBox [] equipment;
	private LockableListModel [] equipmentLists;
	private ChangeComboBox outfitSelect, familiarSelect;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 *
	 * @param	StaticEntity.getClient()	The StaticEntity.getClient() containing the data associated with the character
	 */

	public GearChangeFrame()
	{
		super( "Gear Changer" );

		equipment = new ChangeComboBox[9];
		equipmentLists = KoLCharacter.getEquipmentLists();

		for ( int i = 0; i < 9; ++i )
			equipment[i] = new ChangeComboBox( equipmentLists[i] );

		familiarSelect = new ChangeComboBox( KoLCharacter.getFamiliarList() );
		outfitSelect = new ChangeComboBox( KoLCharacter.getOutfits() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( new EquipPanel(), "" );
		KoLCharacter.updateEquipmentLists();
	}

	public void dispose()
	{
		for ( int i = 0; i < equipment.length; ++i )
			equipmentLists[i].removeListDataListener( equipment[i] );

		KoLCharacter.getOutfits().removeListDataListener( outfitSelect );
		KoLCharacter.getFamiliarList().removeListDataListener( familiarSelect );

		super.dispose();
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
		}
		
		public void actionConfirmed()
		{
			ArrayList requestList = new ArrayList();
			for ( int i = 0; i < pieces.length; ++i )
			{
				if ( pieces[i] != null )
				{
					requestList.add( new EquipmentRequest( StaticEntity.getClient(), pieces[i], i ) );
					pieces[i] = null;
				}
				
				equipment[i].setEnabled( true );
			}
			
			if ( familiar != null )
			{
				requestList.add( new FamiliarRequest( StaticEntity.getClient(), familiar ) );
				familiar = null;
				familiarSelect.setEnabled( true );
			}
			
			if ( outfit != null )
			{
				requestList.add( new EquipmentRequest( StaticEntity.getClient(), (SpecialOutfit) outfit ) );
				outfit = null;
				outfitSelect.setEnabled( true );
			}
			
			if ( requestList.isEmpty() )
				return;
			
			Runnable [] requests = new Runnable[ requestList.size() ];
			requestList.toArray( requests );

			(new RequestThread( requests )).start();
		}

		public void actionCancelled()
		{	setStatusMessage( NULL_STATE, "Feature not yet available." );
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
				outfit = getSelectedItem();
				boolean shouldEnable = outfit instanceof String;

				if ( shouldEnable )
					outfit = null;
				
				for ( int i = 0; i < 8; ++i )
					equipment[i].setEnabled( shouldEnable );
			}
			else
			if ( this == familiarSelect )
			{
				familiar = (FamiliarData) getSelectedItem();
				if ( familiar.equals( KoLCharacter.getFamiliar() ) )
					familiar = null;

				pieces[8] = null;
				equipmentLists[8].clear();
				equipment[8].setEnabled( false );
			}
			else
			{
				for ( int i = 0; i < equipment.length; ++i )
					if ( this == equipment[i] )
						pieces[i] = (String) getSelectedItem();

				if ( this != equipment[8] )
					outfitSelect.setEnabled( false );
			}
		}
	}
}
