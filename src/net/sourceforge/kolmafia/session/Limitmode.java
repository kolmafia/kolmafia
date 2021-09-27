package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.UseSkillRequest;

public class Limitmode
{
	// Limitmode
	public static final String SPELUNKY = "spelunky";
	public static final String BATMAN = "batman";
	public static final String ED = "edunder";

	public static final boolean limitSkill( final int skillId )
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY )
		{
			// Return false for Spelunky skills, FIND limits!
			if ( skillId >= 7238 && skillId <= 7244 )
			{
				return false; 
			}
			return true;
		}

		if ( limitmode == Limitmode.BATMAN )
		{
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
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY )
		{
			// Return false for Spelunky items, add them here
			if ( itemId >= 8040 && itemId <= 8062 )
			{
				return false;
			}
			return true;
		}

		if ( limitmode == Limitmode.BATMAN )
		{
			// Return false for Batman items, add them here
			if ( itemId >= 8797 && itemId <= 8815 && itemId != 8800 )
			{
				return false;
			}
			return true;
		}

		if ( limitmode == Limitmode.ED )
		{
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
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY )
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

		if ( limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitOutfits()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitFamiliars()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	private static String rootZone( String zoneName )
	{
		while ( true )
		{
			if ( zoneName.equals( "Spelunky Area" ) || zoneName.equals( "Batfellow Area" ) )
			{
				return zoneName;
			}

			String parent = AdventureDatabase.getParentZone( zoneName );
			if ( parent == null || parent.equals( zoneName ) )
			{
				return zoneName;
			}
			zoneName = parent;
		}
	}

	public static final boolean limitAdventure( KoLAdventure adventure )
	{
		return Limitmode.limitZone( adventure.getZone() );
	}

	public static final boolean limitZone( String zoneName )
	{
		String limitmode = KoLCharacter.getLimitmode();
		String rootZone = Limitmode.rootZone( zoneName );
		if ( limitmode == null )
		{
			return rootZone.equals( "Spelunky Area" ) || rootZone.equals( "Batfellow Area" );
		}

		if ( limitmode == Limitmode.SPELUNKY )
		{
			return !rootZone.equals( "Spelunky Area" );
		}

		if ( limitmode == Limitmode.BATMAN )
		{
			return !rootZone.equals( "Batfellow Area" );
		}

		if ( limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitMeat()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitMall()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}
		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitNPCStores()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitCoinmasters()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitClan()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitCampground()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitStorage()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitEating()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitDrinking()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitSpleening()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN || limitmode == Limitmode.ED )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitPickpocket()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}

	public static final boolean limitMCD()
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode == null )
		{
			return false;
		}

		if ( limitmode == Limitmode.SPELUNKY || limitmode == Limitmode.BATMAN )
		{
			return true;
		}

		// Should only hit this when a new limitmode is added, default to allow
		return false;
	}
}
