/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;


public class Limitmode
{
	// Limitmode
	public static final String SPELUNKY = "spelunky";

	public static final boolean limitSkill( final int skillId )
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			// Return false for Spelunky skills, FIND limits!
			if ( skillId >= 7238 && skillId <= 7244 )
			{
				return false; 
			}
			return true;
		}
		// Should only hit this when a new limitmode is added, default to none
		return false;
	}

	public static final boolean limitSkill( final String skillName )
	{
		int skillId = SkillDatabase.getSkillId( skillName );
		return Limitmode.limitSkill( skillId );
	}

	public static final boolean limitSkill( final UseSkillRequest skill )
	{
		int skillId = skill.getSkillId();
		return Limitmode.limitSkill( skillId );
	}

	public static final boolean limitItem( final int itemId )
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			// Return false for Spelunky items, add them here
			if ( itemId >= 8040 && itemId <= 8062 )
			{
				return false;
			}
			return true;
		}
		// Should only hit this when a new limitmode is added, default to none
		return false;
	}

	public static final boolean limitItem( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName );
		return Limitmode.limitItem( itemId );
	}

	public static final boolean limitItem( final AdventureResult item )
	{
		int itemId = item.getItemId();
		return Limitmode.limitItem( itemId );
	}

	public static final boolean limitSlot( final int slot )
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			switch ( slot )
			{
			case EquipmentManager.HAT:
			case EquipmentManager.WEAPON:
			case EquipmentManager.OFFHAND:
			case EquipmentManager.CONTAINER:
			case EquipmentManager.ACCESSORY1:
				return false;
			}
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitFamiliars()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitAdventure( KoLAdventure adventure )
	{
		return Limitmode.limitZone( adventure.getZone() );
	}

	public static final boolean limitZone( final String zoneName )
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			if ( zoneName.equals( "Spelunky Area" ) )
			{
				return false;
			}
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitMall()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitNPCShops()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitCoinmasters()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitClan()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitCampground()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitEating()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitDrinking()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitSpleening()
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			return false;
		}
		else if ( KoLCharacter.getLimitmode() == Limitmode.SPELUNKY )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}
}
