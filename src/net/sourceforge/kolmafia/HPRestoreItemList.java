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
	private static final HPRestoreItem REMEDY = new HPRestoreItem( "soft green echo eyedrop antidote", 0 );
	private static final HPRestoreItem TINY_HOUSE = new HPRestoreItem( "tiny house", 22 );

	public static final HPRestoreItem WALRUS = new HPRestoreItem( "tongue of the walrus", 35 );
	public static final HPRestoreItem OTTER = new HPRestoreItem( "tongue of the otter", 15 );
	public static final HPRestoreItem BANDAGES = new HPRestoreItem( "lasagna bandages", 24 );
	public static final HPRestoreItem COCOON = new HPRestoreItem( "cannelloni cocoon", Integer.MAX_VALUE );
	public static final HPRestoreItem NAP = new HPRestoreItem( "disco nap", 20 );
	public static final HPRestoreItem POWERNAP = new HPRestoreItem( "disco power nap", 40 );

	private static LockableListModel list = new LockableListModel();
	static
	{
		list.add( WALRUS );
		list.add( OTTER );

		list.add( REMEDY );
		list.add( TINY_HOUSE );

		list.add( COCOON );
		list.add( BANDAGES );
		list.add( POWERNAP );
		list.add( NAP );

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

	public static JCheckBox [] getCheckboxes()
	{
		Object [] restoreName = list.toArray();
		String hpRestoreSetting = getProperty( "hpRestores" );

		JCheckBox [] restoreCheckbox = new JCheckBox[ restoreName.length ];

		for ( int i = 0; i < restoreName.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox( restoreName[i].toString() );
			restoreCheckbox[i].setSelected( hpRestoreSetting.indexOf( restoreName[i].toString() ) != -1 );
		}

		return restoreCheckbox;
	}

	public static class HPRestoreItem
	{
		private int skillID;
		private String itemName;
		private int hpPerUse;
		private AdventureResult itemUsed;

		public HPRestoreItem( String itemName, int hpPerUse )
		{
			this.itemName = itemName;
			this.hpPerUse = hpPerUse;
			this.skillID = ClassSkillsDatabase.getSkillID( itemName );
			this.itemUsed = new AdventureResult( itemName, 0 );
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public void recoverHP( boolean canUseOtherTechnique, int needed )
		{
			// Remedies are only used if the player is beaten up.
			// Otherwise, it is not used.
			
			if ( this == REMEDY )
			{
				if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new UneffectRequest( client, KoLAdventure.BEATEN_UP )).run();

				return;
			}
			
			// For all other instances, you will need to calculate
			// the number of times this technique must be used.
			
			int hpShort = needed - KoLCharacter.getCurrentHP();
			int numberToUse = (int) Math.ceil( hpShort / hpPerUse );

			if ( TradeableItemDatabase.contains( itemName ) && canUseOtherTechnique )
				numberToUse = Math.min( numberToUse, itemUsed.getCount( KoLCharacter.getInventory() ) );

			if ( ClassSkillsDatabase.contains( itemName ) && canUseOtherTechnique )
			{
				int mpPerUse = ClassSkillsDatabase.getMPConsumptionByID( skillID );
				numberToUse = Math.min( numberToUse, KoLCharacter.getCurrentHP() / numberToUse );
			}

			if ( numberToUse == 0 )
				return;

			// Test for tiny houses.  If there is no other technique,
			// use as many as possible.  Otherwise, just use one.
			
			if ( this == TINY_HOUSE )
			{
				if ( !canUseOtherTechnique )
					(new ConsumeItemRequest( client, new AdventureResult( "tiny house", numberToUse ) )).run();
				else if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new ConsumeItemRequest( client, new AdventureResult( "tiny house", 1 ) )).run();

				return;
			}

			if ( this == WALRUS || this == OTTER )
			{
				if ( !canUseOtherTechnique )
					(new UseSkillRequest( client, toString(), "", numberToUse )).run();
				else if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new UseSkillRequest( client, toString(), "", 1 )).run();
					
				return;
			}

			if ( ClassSkillsDatabase.contains( this.toString() ) )
			{
				(new UseSkillRequest( client, this.toString(), "", numberToUse )).run();
			}
			else
			{
				DEFAULT_SHELL.updateDisplay( "Consuming " + numberToUse + " " + itemName + "..." );
				(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
			}
		}

		public String toString()
		{	return itemName;
		}
	}
}
