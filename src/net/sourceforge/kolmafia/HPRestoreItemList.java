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
 * "AS IS" AND ANY EXPRESS OR IHPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IHPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEHPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A special class used as a holder class to hold all of the
 * items which are available for use as HP buffers.
 */

public abstract class HPRestoreItemList extends StaticEntity
{
	public static final HPRestoreItem WALRUS = new HPRestoreItem( "tongue of the walrus", 35 );
	private static final HPRestoreItem REMEDY = new HPRestoreItem( "soft green echo eyedrop antidote", 0 );
	private static final HPRestoreItem TINY_HOUSE = new HPRestoreItem( "tiny house", 22 );

	public static final HPRestoreItem COCOON = new HPRestoreItem( "cannelloni cocoon", 1 );

	private static Object [] restoreName = new Object[0];
	private static JCheckBox [] restoreCheckbox = new JCheckBox[0];
	private static LockableListModel list = new LockableListModel();

	public static void reset()
	{
		list.clear();
		
		list.add( WALRUS );
		list.add( REMEDY );
		list.add( TINY_HOUSE );

		list.add( COCOON );
		list.add( new HPRestoreItem( "Medicinal Herb's medicinal herbs", Integer.MAX_VALUE ) );
		list.add( new HPRestoreItem( "scroll of drastic healing", Integer.MAX_VALUE ) );

		list.add( new HPRestoreItem( "phonics down", 48 ) );
		list.add( new HPRestoreItem( "cast", 17 ) );
		list.add( new HPRestoreItem( "Doc Galaktik's Ailment Ointment", 9 ) );
	}

	public static HPRestoreItem get( int index )
	{	return (HPRestoreItem) list.get( index );
	}

	public static int size()
	{	return list.size();
	}

	public static JScrollPane getDisplay()
	{
		restoreName = list.toArray();
		restoreCheckbox = new JCheckBox[ restoreName.length ];

		JPanel checkboxPanel = new JPanel();
		checkboxPanel.setLayout( new GridLayout( restoreCheckbox.length, 1 ) );

		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox();
			checkboxPanel.add( restoreCheckbox[i] );
		}

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout( new GridLayout( restoreName.length, 1 ) );
		for ( int i = 0; i < restoreName.length; ++i )
			labelPanel.add( new JLabel( restoreName[i].toString(), JLabel.LEFT ) );

		JPanel restorePanel = new JPanel();
		restorePanel.setLayout( new BorderLayout( 0, 0 ) );
		restorePanel.add( checkboxPanel, BorderLayout.WEST );
		restorePanel.add( labelPanel, BorderLayout.CENTER );

		String HPRestoreSetting = getProperty( "hpRestores" );

		for ( int i = 0; i < restoreName.length; ++i )
			if ( HPRestoreSetting.indexOf( restoreName[i].toString() ) != -1 )
				restoreCheckbox[i].setSelected( true );

		JScrollPane scrollArea = new JScrollPane( restorePanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		return scrollArea;
	}

	public static void setProperty()
	{
		StringBuffer hpRestoreSetting = new StringBuffer();

		if ( restoreCheckbox != null )
		{
			for ( int i = 0; i < restoreCheckbox.length; ++i )
			{
				if ( restoreCheckbox[i].isSelected() )
				{
					if ( hpRestoreSetting.length() != 0 )
						hpRestoreSetting.append( ';' );

					hpRestoreSetting.append( restoreName[i].toString() );
				}
			}
		}

		setProperty( "hpRestores", hpRestoreSetting.toString() );
	}

	public static class HPRestoreItem
	{
		private String itemName;
		private int hpPerUse;
		private AdventureResult itemUsed;

		public HPRestoreItem( String itemName, int hpPerUse )
		{
			this.itemName = itemName;
			this.hpPerUse = hpPerUse;
			this.itemUsed = new AdventureResult( itemName, 0 );
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public void recoverHP()
		{
			if ( this == REMEDY )
			{
				if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new UneffectRequest( client, KoLAdventure.BEATEN_UP )).run();

				return;
			}

			if ( this == TINY_HOUSE )
			{
				if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new ConsumeItemRequest( client, new AdventureResult( "tiny house", 1 ) )).run();
				
				return;
			}

			int currentHP = KoLCharacter.getCurrentHP();
			int maximumHP = KoLCharacter.getMaximumHP();
			int maximumMP = KoLCharacter.getMaximumMP();
			int hpShort = maximumHP - currentHP;
			
			if ( this == WALRUS )
			{
				int mpPerCast = ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( "Tongue of the Walrus" ) );
				(new UseSkillRequest( client, "Tongue of the Walrus", "", Math.min( maximumMP / mpPerCast, hpShort / 35 ) )).run();
				return;
			}

			if ( this == COCOON )
			{
				int mpPerCast = ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( "Cannelloni Cocoon" ) );
				(new UseSkillRequest( client, "Cannelloni Cocoon", "", 1 )).run();
				return;
			}

			// Always buff as close to max HP as possible, in order to
			// go as easy on the server as possible.

			int numberToUse = (int) Math.ceil( hpShort / hpPerUse );

			if ( StaticEntity.getProperty( "autoSatisfyChecks" ).equals( "false" ) )
				numberToUse = Math.min( numberToUse, itemUsed.getCount( KoLCharacter.getInventory() ) );


			if ( numberToUse < 1 )
				numberToUse = 1;

			DEFAULT_SHELL.updateDisplay( "Consuming " + numberToUse + " " + itemName + "..." );
			(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
		}

		public String toString()
		{	return itemName;
		}
	}
}
