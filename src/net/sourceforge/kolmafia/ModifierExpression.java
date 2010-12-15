/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ModifierExpression
	extends Expression
{
	private AdventureResult effect;
	private ArrayList loc;
	private ArrayList zone;
	private ArrayList fam;
	private ArrayList mainhand;

	public ModifierExpression( String text, String name )
	{
		super( text, name );
	}

	protected void initialize()
	{
		this.loc = new ArrayList();
		this.zone = new ArrayList();
		this.fam = new ArrayList();
		this.mainhand = new ArrayList();

		// The first check also matches "[zone(The Slime Tube)]"
		// Hence the second check.
		if ( text.indexOf( "T" ) != -1 && EffectDatabase.contains( this.name ) )
		{
			this.effect = new AdventureResult( name, 0, true );
		}
	}

	protected boolean validBytecode( char inst )
	{
		switch ( inst )
		{
		case 'l':
		case 'z':
		case 'w':
		case 'h':
		case 'B':
		case 'D':
		case 'F':
		case 'G':
		case 'H':
		case 'J':
		case 'L':
		case 'M':
		case 'R':
		case 'S':
		case 'T':
		case 'U':
		case 'W':
		case 'X':
			return true;
		}
		return false;
	}

	protected float evalBytecode( char inst )
	{
		float[] s = this.stack;
		float v = 0.0f;
		switch ( inst )
		{
		case 'l':
			v = Modifiers.currentLocation.indexOf( (String) this.loc.get( (int) s[ --this.sp ] ) ) == -1 ? 0.0f : 1.0f;
			break;
		case 'z':
			v = Modifiers.currentZone.indexOf( (String) this.zone.get( (int) s[ --this.sp ] ) ) == -1 ? 0.0f : 1.0f;
			break;
		case 'w':
			v = Modifiers.currentFamiliar.indexOf( (String) this.fam.get( (int) s[ --this.sp ] ) ) == -1 ? 0.0f : 1.0f;
			break;
		case 'h':
			v = Modifiers.mainhandClass.indexOf( (String) this.mainhand.get( (int) s[ --this.sp ] ) ) == -1 ? 0.0f : 1.0f;
			break;
		case 'A':
			v = KoLCharacter.getAscensions();
			break;
		case 'B':
			v = HolidayDatabase.getBloodEffect();
			break;
		case 'D':
			v = KoLCharacter.getInebriety();
			break;
		case 'F':
			v = KoLCharacter.getFullness();
			break;
		case 'G':
			v = HolidayDatabase.getGrimaciteEffect() / 10.0f;
			break;
		case 'H':
			v = Modifiers.hoboPower;
			break;
		case 'J':
			v = HolidayDatabase.getHoliday().equals( "Festival of Jarlsberg" ) ? 1.0f : 0.0f;
			break;
		case 'L':
			v = KoLCharacter.getLevel();
			break;
		case 'M':
			v = HolidayDatabase.getMoonlight();
			break;
		case 'R':
			v = KoLCharacter.getReagentPotionDuration();
			break;
		case 'S':
			v = KoLCharacter.getSpleenUse();
			break;
		case 'T':
			v = Math.max( 1, this.effect.getCount( KoLConstants.activeEffects ) );
			break;
		case 'U':
			v = KoLCharacter.getTelescopeUpgrades();
			break;
		case 'W':
			v = Modifiers.currentWeight;
			break;
		case 'X':
			v = KoLCharacter.getGender();
			break;
		}
		return v;
	}

	protected String function()
	{
		String rv;

		if ( this.optional( "loc(" ) )
		{
			this.loc.add( this.until( ")" ).toLowerCase() );
			return String.valueOf( (char)( ( this.loc.size()-1 ) + 0x8000) ) + "l";
		}
		if ( this.optional( "zone(" ) )
		{
			this.zone.add( this.until( ")" ).toLowerCase() );
			return String.valueOf( (char)( ( this.zone.size()-1 ) + 0x8000) ) + "z";
		}
		if ( this.optional( "fam(" ) )
		{
			this.fam.add( this.until( ")" ).toLowerCase() );
			return String.valueOf( (char)( ( this.fam.size()-1 ) + 0x8000) ) + "w";
		}
		if ( this.optional( "mainhand(" ) )
		{
			this.mainhand.add( this.until( ")" ).toLowerCase() );
			return String.valueOf( (char)( ( this.mainhand.size()-1 ) + 0x8000) ) + "h";
		}

		return null;
	}
}
