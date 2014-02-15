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

package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.BooleanArray;

public class MaximizerSpeculation
	extends Speculation
implements Comparable<MaximizerSpeculation>, Cloneable
{
	private boolean scored = false;
	private boolean tiebreakered = false;
	private boolean exceeded;
	private double score, tiebreaker;
	private int simplicity;
	private int beeosity;

	public boolean failed = false;
	public CheckedItem attachment;

	@Override
	public Object clone()
	{
		try
		{
			MaximizerSpeculation copy = (MaximizerSpeculation) super.clone();
			copy.equipment = (AdventureResult[]) this.equipment.clone();
			return copy;
		}
		catch ( CloneNotSupportedException e )
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		if ( this.attachment != null )
		{
			return this.attachment.getInstance( (int) this.getScore() ).toString();
		}
		return super.toString();
	}

	public void setUnscored()
	{
		this.scored = false;
		this.calculated = false;
	}

	public double getScore()
	{
		if ( this.scored ) return this.score;
		if ( !this.calculated ) this.calculate();
		this.score = Maximizer.eval.getScore( this.mods );
		if ( KoLCharacter.inBeecore() )
		{
			this.beeosity = KoLCharacter.getBeeosity( this.equipment );
		}
		Maximizer.eval.checkEquipment( this.mods, this.equipment,
			this.beeosity );
		this.failed = Maximizer.eval.failed;
		if ( (this.mods.getRawBitmap( Modifiers.MUTEX_VIOLATIONS )
			& ~KoLCharacter.currentRawBitmapModifier( Modifiers.MUTEX_VIOLATIONS )) != 0 )
		{	// We're speculating about something that would create a
			// mutex problem that the player didn't already have.
			this.failed = true;
		}
		this.exceeded = Maximizer.eval.exceeded;
		this.scored = true;
		return this.score;
	}

	public double getTiebreaker()
	{
		if ( this.tiebreakered ) return this.tiebreaker;
		if ( !this.calculated ) this.calculate();
		this.tiebreaker = Maximizer.eval.getTiebreaker( this.mods );
		this.tiebreakered = true;
		this.simplicity = 0;
		for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
		{
			AdventureResult item = this.equipment[ slot ];
			if ( item == null ) item = EquipmentRequest.UNEQUIP;
			if ( EquipmentManager.getEquipment( slot ).equals( item ) )
			{
				this.simplicity += 2;
			}
			else if ( item.equals( EquipmentRequest.UNEQUIP ) )
			{
				this.simplicity += slot == EquipmentManager.WEAPON ? -1 : 1;
			}
		}
		return this.tiebreaker;
	}

	public int compareTo( MaximizerSpeculation o )
	{
		if ( !(o instanceof MaximizerSpeculation) ) return 1;
		MaximizerSpeculation other = (MaximizerSpeculation) o;
		int rv = Double.compare( this.getScore(), other.getScore() );
		if ( this.failed != other.failed ) return this.failed ? -1 : 1;
		if ( rv != 0 ) return rv;
		rv = other.beeosity - this.beeosity;
		if ( rv != 0 ) return rv;
		rv = Double.compare( this.getTiebreaker(), other.getTiebreaker() );
		if ( rv != 0 ) return rv;
		rv = this.simplicity - other.simplicity;
		if ( rv != 0 ) return rv;
		if ( this.attachment != null && other.attachment != null )
		{
			// prefer items that you don't have to buy
			if ( this.attachment.buyableFlag != other.attachment.buyableFlag )
			{
				 return this.attachment.buyableFlag ? -1 : 1;
			}

			if ( KoLCharacter.inBeecore() )
			{	// prefer fewer Bs
				rv = KoLCharacter.getBeeosity( other.attachment.getName() ) -
					KoLCharacter.getBeeosity( this.attachment.getName() );
			}
		}
		return rv;
	}

	// Remember which equipment slots were null, so that this
	// state can be restored later.
	public Object mark()
	{
		return this.equipment.clone();
	}

	public void restore( Object mark )
	{
		System.arraycopy( mark, 0, this.equipment, 0, EquipmentManager.ALL_SLOTS );
	}

	public void tryAll( ArrayList familiars, ArrayList enthronedFamiliars, BooleanArray usefulOutfits, TreeMap outfitPieces, ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		this.tryOutfits( enthronedFamiliars, usefulOutfits, outfitPieces, possibles, bestCard );
		for ( int i = 0; i < familiars.size(); ++i )
		{
			this.setFamiliar( (FamiliarData) familiars.get( i ) );
			possibles[ EquipmentManager.FAMILIAR ] =
				possibles[ EquipmentManager.ALL_SLOTS + i ];
			this.tryOutfits( enthronedFamiliars, usefulOutfits, outfitPieces, possibles, bestCard );
		}
	}

	public void tryOutfits( ArrayList<FamiliarData> enthronedFamiliars, BooleanArray usefulOutfits, TreeMap outfitPieces, ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		for ( int outfit = usefulOutfits.size() - 1; outfit >= 0; --outfit )
		{
			if ( !usefulOutfits.get( outfit ) ) continue;
			AdventureResult[] pieces = EquipmentDatabase.getOutfit( outfit ).getPieces();
		pieceloop:
			for ( int idx = pieces.length - 1; ; --idx )
			{
				if ( idx == -1 )
				{	// all pieces successfully put on
					this.tryFamiliarItems( enthronedFamiliars, possibles, bestCard );
					break;
				}
				AdventureResult item = (AdventureResult) outfitPieces.get( pieces[ idx ] );
				if ( item == null ) break;	// not available
				int count = item.getCount();
				int slot = EquipmentManager.itemIdToEquipmentType( item.getItemId() );

				switch ( slot )
				{
				case EquipmentManager.HAT:
				case EquipmentManager.PANTS:
				case EquipmentManager.SHIRT:
					if ( item.equals( this.equipment[ slot ] ) )
					{	// already worn
						continue pieceloop;
					}
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					break;
				case EquipmentManager.WEAPON:
				case EquipmentManager.OFFHAND:
					if ( item.equals( this.equipment[ EquipmentManager.WEAPON ] ) ||
						item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
					{	// already worn
						continue pieceloop;
					}
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					break;
				case EquipmentManager.ACCESSORY1:
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY1 ] ) ||
						item.equals( this.equipment[ EquipmentManager.ACCESSORY2 ] ) ||
						item.equals( this.equipment[ EquipmentManager.ACCESSORY3 ] ) )
					{	// already worn
						continue pieceloop;
					}
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
					{
						slot = EquipmentManager.ACCESSORY3;
					}
					else if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
					{
						slot = EquipmentManager.ACCESSORY2;
					}
					break;
				default:
					break pieceloop;	// don't know how to wear that
				}

				if ( count <= 0 ) break;	// none available
				if ( this.equipment[ slot ] != null ) break; // slot taken
				this.equipment[ slot ] = item;
			}
			this.restore( mark );
		}

		this.tryFamiliarItems( enthronedFamiliars, possibles, bestCard );
	}

	public void tryFamiliarItems( ArrayList<FamiliarData> enthronedFamiliars, ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		if ( this.equipment[ EquipmentManager.FAMILIAR ] == null )
		{
			ArrayList possible = possibles[ EquipmentManager.FAMILIAR ];
			boolean any = false;
			for ( int pos = 0; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				int count = item.getCount();
				if ( item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.WEAPON ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.HAT ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.PANTS ] ) )
				{
					--count;
				}
				if ( count <= 0 ) continue;
				this.equipment[ EquipmentManager.FAMILIAR ] = item;
				this.tryContainers( enthronedFamiliars, possibles, bestCard );
				any = true;
				this.restore( mark );
			}

			if ( any ) return;
			this.equipment[ EquipmentManager.FAMILIAR ] = EquipmentRequest.UNEQUIP;
		}

		this.tryContainers( enthronedFamiliars, possibles, bestCard );
		this.restore( mark );
	}

	public void tryContainers( ArrayList<FamiliarData> enthronedFamiliars, ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		if ( this.equipment[ EquipmentManager.CONTAINER ] == null )
		{
			ArrayList possible = possibles[ EquipmentManager.CONTAINER ];
			boolean any = false;
			for ( int pos = 0; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				//int count = item.getCount();
				//if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
				//{
				//	--count;
				//}
				//if ( count <= 0 ) continue;
				this.equipment[ EquipmentManager.CONTAINER ] = item;
				if ( item.getItemId() == ItemPool.BUDDY_BJORN )
				{
					for ( FamiliarData f : enthronedFamiliars )
					{
						this.setBjorned( f );
						this.tryAccessories( enthronedFamiliars, possibles, 0, bestCard );
						any = true;
						this.restore( mark );
					}
				}
				else
				{
					this.tryAccessories( enthronedFamiliars, possibles, 0, bestCard );
					any = true;
					this.restore( mark );
				}
			}

			if ( any ) return;
			this.equipment[ EquipmentManager.CONTAINER ] = EquipmentRequest.UNEQUIP;
		}

		this.tryAccessories( enthronedFamiliars, possibles, 0, bestCard );
		this.restore( mark );
	}

	public void tryAccessories( ArrayList<FamiliarData> enthronedFamiliars, ArrayList[] possibles, int pos, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		int free = 0;
		if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null ) ++free;
		if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null ) ++free;
		if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null ) ++free;
		if ( free > 0 )
		{
			ArrayList possible = possibles[ EquipmentManager.ACCESSORY1 ];
			boolean any = false;
			for ( ; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				int count = item.getCount();
				if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY1 ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY2 ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY3 ] ) )
				{
					--count;
				}
				if ( count <= 0 ) continue;
				for ( count = Math.min( free, count ); count > 0; --count )
				{
					if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null )
					{
						this.equipment[ EquipmentManager.ACCESSORY1 ] = item;
					}
					else if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
					{
						this.equipment[ EquipmentManager.ACCESSORY2 ] = item;
					}
					else if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
					{
						this.equipment[ EquipmentManager.ACCESSORY3 ] = item;
					}
					else
					{
						System.out.println( "no room left???" );
						break;	// no room left - shouldn't happen
					}

					this.tryAccessories( enthronedFamiliars, possibles, pos + 1, bestCard );
					any = true;
				}
				this.restore( mark );
			}

			if ( any ) return;

			if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null )
			{
				this.equipment[ EquipmentManager.ACCESSORY1 ] = EquipmentRequest.UNEQUIP;
			}
			if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
			{
				this.equipment[ EquipmentManager.ACCESSORY2 ] = EquipmentRequest.UNEQUIP;
			}
			if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
			{
				this.equipment[ EquipmentManager.ACCESSORY3 ] = EquipmentRequest.UNEQUIP;
			}
		}

		this.trySwap( EquipmentManager.ACCESSORY1, EquipmentManager.ACCESSORY2 );
		this.trySwap( EquipmentManager.ACCESSORY2, EquipmentManager.ACCESSORY3 );
		this.trySwap( EquipmentManager.ACCESSORY3, EquipmentManager.ACCESSORY1 );

		this.tryHats( enthronedFamiliars, possibles, bestCard );
		this.restore( mark );
	}

	public void tryHats( ArrayList<FamiliarData> enthronedFamiliars, ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		if ( this.equipment[ EquipmentManager.HAT ] == null )
		{
			ArrayList possible = possibles[ EquipmentManager.HAT ];
			boolean any = false;
			for ( int pos = 0; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				int count = item.getCount();
				if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
				{
					--count;
				}
				if ( count <= 0 ) continue;
				this.equipment[ EquipmentManager.HAT ] = item;
				if ( item.getItemId() == ItemPool.HATSEAT )
				{
					for ( FamiliarData f : enthronedFamiliars )
					{
						// Cannot use same familiar for this and Bjorn
						if( f != this.getBjorned() )
						{
							this.setEnthroned( f );
							this.tryShirts( possibles, bestCard );
							any = true;
							this.restore( mark );
						}
					}
				}
				else
				{
					this.tryShirts( possibles, bestCard );
					any = true;
					this.restore( mark );
				}
			}

			if ( any ) return;
			this.equipment[ EquipmentManager.HAT ] = EquipmentRequest.UNEQUIP;
		}

		this.tryShirts( possibles, bestCard );
		this.restore( mark );
	}

	public void tryShirts( ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		if ( this.equipment[ EquipmentManager.SHIRT ] == null )
		{
			boolean any = false;
			if ( KoLCharacter.isTorsoAware()  )
			{
				ArrayList possible = possibles[ EquipmentManager.SHIRT ];
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount();
					//if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					//{
					//	--count;
					//}
					//if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.SHIRT ] = item;
					this.tryPants( possibles, bestCard );
					any = true;
					this.restore( mark );
				}
			}

			if ( any ) return;
			this.equipment[ EquipmentManager.SHIRT ] = EquipmentRequest.UNEQUIP;
		}

		this.tryPants( possibles, bestCard );
		this.restore( mark );
	}

	public void tryPants( ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		if ( this.equipment[ EquipmentManager.PANTS ] == null )
		{
			ArrayList possible = possibles[ EquipmentManager.PANTS ];
			boolean any = false;
			for ( int pos = 0; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				int count = item.getCount();
				if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
				{
					--count;
				}
				if ( count <= 0 ) continue;
				this.equipment[ EquipmentManager.PANTS ] = item;
				this.tryWeapons( possibles, bestCard );
				any = true;
				this.restore( mark );
			}

			if ( any ) return;
			this.equipment[ EquipmentManager.PANTS ] = EquipmentRequest.UNEQUIP;
		}

		this.tryWeapons( possibles, bestCard );
		this.restore( mark );
	}

	public void tryWeapons( ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		boolean chefstaffable = KoLCharacter.hasSkill( "Spirit of Rigatoni" ) || KoLCharacter.isJarlsberg();
		if ( !chefstaffable && KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
		{
			chefstaffable =
				this.equipment[ EquipmentManager.ACCESSORY1 ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE ||
				this.equipment[ EquipmentManager.ACCESSORY2 ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE ||
				this.equipment[ EquipmentManager.ACCESSORY3 ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE;
		}
		if ( this.equipment[ EquipmentManager.WEAPON ] == null )
		{
			ArrayList possible = possibles[ EquipmentManager.WEAPON ];
			//boolean any = false;
			for ( int pos = 0; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				if ( !chefstaffable &&
					EquipmentDatabase.getItemType( item.getItemId() ).equals( "chefstaff" ) )
				{
					continue;
				}
				int count = item.getCount();
				if ( item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
				{
					--count;
				}
				if ( count <= 0 ) continue;
				this.equipment[ EquipmentManager.WEAPON ] = item;
				this.tryOffhands( possibles, bestCard );
				//any = true;
				this.restore( mark );
			}

			// if ( any && <no unarmed items in shortlists> ) return;
			if ( Maximizer.eval.melee < -1 || Maximizer.eval.melee > 1 )
			{
				return;
			}
			this.equipment[ EquipmentManager.WEAPON ] = EquipmentRequest.UNEQUIP;
		}
		else if ( !chefstaffable &&
			EquipmentDatabase.getItemType( this.equipment[ EquipmentManager.WEAPON ].getItemId() ).equals( "chefstaff" ) )
		{
			return;
		}

		this.tryOffhands( possibles, bestCard );
		this.restore( mark );
	}

	public void tryOffhands( ArrayList[] possibles, AdventureResult bestCard )
		throws MaximizerInterruptedException
	{
		Object mark = this.mark();
		int weapon = this.equipment[ EquipmentManager.WEAPON ].getItemId();
		if ( EquipmentDatabase.getHands( weapon ) > 1 )
		{
			this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
		}

		if ( this.equipment[ EquipmentManager.OFFHAND ] == null )
		{
			ArrayList possible;
			WeaponType weaponType = WeaponType.NONE;
			if ( KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" ) )
			{
				weaponType = EquipmentDatabase.getWeaponType( weapon );
			}
			switch ( weaponType )
			{
			case MELEE:
				possible = possibles[ Evaluator.OFFHAND_MELEE ];
				break;
			case RANGED:
				possible = possibles[ Evaluator.OFFHAND_RANGED ];
				break;
			default:
				possible = possibles[ EquipmentManager.OFFHAND ];
			}
			boolean any = false;

			for ( int pos = 0; pos < possible.size(); ++pos )
			{
				AdventureResult item = (AdventureResult) possible.get( pos );
				int count = item.getCount();
				if ( item.equals( this.equipment[ EquipmentManager.WEAPON ] ) )
				{
					--count;
				}
				if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
				{
					--count;
				}
				if ( count <= 0 ) continue;
				if ( item.getItemId() == ItemPool.CARD_SLEEVE )
				{
					this.equipment[ EquipmentManager.CARD_SLEEVE ] = bestCard;
				}
				this.equipment[ EquipmentManager.OFFHAND ] = item;
				this.tryOffhands( possibles, bestCard );
				any = true;
				this.restore( mark );
			}

			if ( any && weapon > 0 ) return;
			this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
		}

		// doit
		this.calculated = false;
		this.scored = false;
		this.tiebreakered = false;
		if ( this.compareTo( Maximizer.best ) > 0 )
		{
			Maximizer.best = (MaximizerSpeculation) this.clone();
		}
		Maximizer.bestChecked++;
		long t = System.currentTimeMillis();
		if ( t > Maximizer.bestUpdate )
		{
			MaximizerSpeculation.showProgress();
			Maximizer.bestUpdate = t + 5000;
		}
		this.restore( mark );
		if ( !KoLmafia.permitsContinue() )
		{
			throw new MaximizerInterruptedException();
		}
		if ( this.exceeded )
		{
			throw new MaximizerExceededException();
		}
		long comboLimit = Preferences.getLong( "maximizerCombinationLimit" );
		if ( comboLimit != 0 && Maximizer.bestChecked >= comboLimit )
		{
			throw new MaximizerLimitException();
		}
	}

	private static int getMutex( AdventureResult item )
	{
		Modifiers mods = Modifiers.getModifiers( item.getName() );
		if ( mods == null )
		{
			return 0;
		}
		return mods.getRawBitmap( Modifiers.MUTEX );
	}

	private void trySwap( int slot1, int slot2 )
	{
		// If we are suggesting an accessory that's already being worn,
		// make sure we suggest the same slot (to minimize server hits).
		AdventureResult item1, item2, eq1, eq2;
		item1 = this.equipment[ slot1 ];
		if ( item1 == null ) item1 = EquipmentRequest.UNEQUIP;
		eq1 = EquipmentManager.getEquipment( slot1 );
		if ( eq1.equals( item1 ) ) return;
		item2 = this.equipment[ slot2 ];
		if ( item2 == null ) item2 = EquipmentRequest.UNEQUIP;
		eq2 = EquipmentManager.getEquipment( slot2 );
		if ( eq2.equals( item2 ) ) return;

		// The same thing applies to mutually exclusive accessories -
		// putting the new one in an earlier slot would cause an error
		// when the equipment is being changed.
		int imutex1, imutex2, emutex1, emutex2;
		imutex1 = getMutex( item1 );
		emutex1 = getMutex( eq1 );
		if ( (imutex1 & emutex1) != 0 ) return;
		imutex2 = getMutex( item2 );
		emutex2 = getMutex( eq2 );
		if ( (imutex2 & emutex2) != 0 ) return;

		if ( eq1.equals( item2 ) || eq2.equals( item1 ) ||
			(imutex1 & emutex2) != 0 || (imutex2 & emutex1) != 0 )
		{
			this.equipment[ slot1 ] = item2;
			this.equipment[ slot2 ] = item1;
		}
	}

	public static void showProgress()
	{
		StringBuilder msg = new StringBuilder();
		msg.append( Maximizer.bestChecked );
		msg.append( " combinations checked, best score " );
		double score = Maximizer.best.getScore();
		msg.append( KoLConstants.FLOAT_FORMAT.format( score ) );
		if ( Maximizer.best.failed )
		{
			msg.append( " (FAIL)" );
		}
		//if ( MaximizerFrame.best.tiebreakered )
		//{
		//	msg = msg + " / " + MaximizerFrame.best.getTiebreaker() + " / " +
		//		MaximizerFrame.best.simplicity;
		//}
		KoLmafia.updateDisplay( msg.toString() );
	}
}