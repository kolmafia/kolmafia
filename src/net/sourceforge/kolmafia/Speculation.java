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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Speculation
{
	private int MCD;
	public AdventureResult[] equipment;
	private ArrayList effects;
	private FamiliarData familiar;
	protected boolean calculated = false;
	protected Modifiers mods;

	public Speculation()
	{
		this.MCD = KoLCharacter.getMindControlLevel();
		this.equipment = EquipmentManager.allEquipment();			
		this.effects = new ArrayList();
		this.effects.addAll( KoLConstants.activeEffects );
		while ( this.effects.size() > 0 )
		{	// Strip out intrinsic effects - those granted by equipment
			// will be added from Intrinsic Effect modifiers.
			// This assumes that no intrinsic that is granted by anything
			// other than equipment has any real effect.
			int pos = this.effects.size() - 1;
			if ( ((AdventureResult) this.effects.get( pos )).getCount() >
				Integer.MAX_VALUE / 2 )
			{
				this.effects.remove( pos );
			}
			else break;
		}
		this.familiar = KoLCharacter.currentFamiliar;
	}
	
	public void setMindControlLevel( int MCD )
	{
		this.MCD = MCD;
	}
	
	public void setFamiliar( FamiliarData familiar )
	{
		this.familiar = familiar;
	}
	
	public void equip( int slot, AdventureResult item )
	{
		if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS ) return;
		this.equipment[ slot ] = item;
		if ( slot == EquipmentManager.WEAPON &&
			EquipmentDatabase.getHands( item.getItemId() ) > 1 )
		{
			this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
		}
	}
	
	public boolean hasEffect( AdventureResult effect )
	{
		return this.effects.contains( effect );
	}

	public void addEffect( AdventureResult effect )
	{
		this.effects.add( effect );
	}

	public void removeEffect( AdventureResult effect )
	{
		this.effects.remove( effect );
	}
	
	public Modifiers calculate()
	{
		this.mods = KoLCharacter.recalculateAdjustments(
			false,
			this.MCD,
			this.equipment,
			this.effects,
			this.familiar,
			true );
		this.calculated = true;
		return this.mods;
	}
	
	public boolean parse( String text )
	{
		boolean quiet = false;
		String[] pieces = text.toLowerCase().split( "\\s+;\\s+" );
		for ( int i = 0; i < pieces.length; ++i )
		{
			String[] piece = pieces[ i ].split( " ", 2 );
			String cmd = piece[ 0 ];
			String params = piece.length > 1 ? piece[ 1 ] : "";
			
			if ( cmd.equals( "" ) ) continue;
			else if ( cmd.equals( "mcd" ) )
			{
				this.setMindControlLevel( StringUtilities.parseInt( params ) );
			}
			else if ( cmd.equals( "equip" ) )
			{
				piece = params.split( " ", 2 );
				int slot = EquipmentRequest.slotNumber( piece[ 0 ] );
				if ( slot != -1 )
				{
					params = piece[ 1 ];
				}
		
				AdventureResult match = ItemFinder.getFirstMatchingItem( params,
					ItemFinder.EQUIP_MATCH );
				if ( match == null )
				{
					return true;
				}
				if ( slot == -1 )
				{
					slot = EquipmentRequest.chooseEquipmentSlot( ItemDatabase.getConsumptionType( match.getItemId() ) );
		
					// If it can't be equipped, give up
					if ( slot == -1 )
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't equip a " + match.getName() );
						return true;
					}
				}
				this.equip( slot, match );
			}
			else if ( cmd.equals( "unequip" ) )
			{
				int slot = EquipmentRequest.slotNumber( params );
				if ( slot == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unknown slot: " + params );
					return true;
				}
				this.equip( slot, EquipmentRequest.UNEQUIP );
			}
			else if ( cmd.equals( "familiar" ) )
			{
				int id = FamiliarDatabase.getFamiliarId( params );
				if ( id == -1 && !params.equals( "none" ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unknown familiar: " + params );
					return true;
				}
				FamiliarData fam = new FamiliarData( id );
				this.setFamiliar( fam );
			}
			else if ( cmd.equals( "up" ) )
			{
				List effects = EffectDatabase.getMatchingNames( params );
				if ( effects.isEmpty() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unknown effect: " + params );
					return true;
				}

				AdventureResult effect = new AdventureResult( (String) effects.get( 0 ), 1, true );
				if ( !this.hasEffect( effect ) )
				{
					this.addEffect( effect );
				}
			}
			else if ( cmd.equals( "uneffect" ) )
			{
				List effects = EffectDatabase.getMatchingNames( params );
				if ( effects.isEmpty() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unknown effect: " + params );
					return true;
				}

				AdventureResult effect = new AdventureResult( (String) effects.get( 0 ), 1, true );
				this.removeEffect( effect );
			}
			else if ( cmd.equals( "quiet" ) )
			{
				quiet = true;
			}
			else
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"I don't know how to speculate about " + cmd );
				return true;
			}
		}
		return quiet;
	}
}
