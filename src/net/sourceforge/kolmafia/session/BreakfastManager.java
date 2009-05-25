package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.PyroRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class BreakfastManager
{
	private static final AdventureResult [] toys = new AdventureResult []
	{
		ItemPool.get( ItemPool.HOBBY_HORSE, 1 ),
		ItemPool.get( ItemPool.BALL_IN_A_CUP, 1 ),
		ItemPool.get( ItemPool.SET_OF_JACKS, 1 ),
		ItemPool.get( ItemPool.BAG_OF_CANDY, 1 ),
		ItemPool.get( ItemPool.EMBLEM_AKGYXOTH, 1 ),
		ItemPool.get( ItemPool.IDOL_AKGYXOTH, 1 ),
		ItemPool.get( ItemPool.BURROWGRUB_HIVE, 1 ),
		ItemPool.get( ItemPool.GNOLL_EYE, 1 ),
	};

	private static final AdventureResult toaster = ItemPool.get( ItemPool.TOASTER, 1 );
	private static final AdventureResult key = ItemPool.get( ItemPool.VIP_LOUNGE_KEY, 1 );

	public static void getBreakfast( final boolean runComplete )
	{
		SpecialOutfit.createImplicitCheckpoint();

		if ( runComplete )
		{
			checkToaster();
			checkRumpusRoom();
			checkVIPLounge();
			readGuildManual();
			useCrimboToys();
			getHermitClovers();
			visitBigIsland();
		}

		boolean recoverMana = Preferences.getBoolean( "loginRecovery" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );

		boolean done = true;

		done &= castSkills( recoverMana, 0 );
		done &= castBookSkills( recoverMana, 0 );

		Preferences.setBoolean( "breakfastCompleted", done );

		SpecialOutfit.restoreImplicitCheckpoint();
		KoLmafia.forceContinue();
	}

	public static void checkToaster()
	{
		if ( InventoryManager.hasItem( toaster ) )
		{
			for ( int i = 0; i < 3 && KoLmafia.permitsContinue(); ++i )
			{
				RequestThread.postRequest( new UseItemRequest( toaster ) );
			}

			KoLmafia.forceContinue();
		}
	}

	public static void checkRumpusRoom()
	{
		if ( Preferences.getBoolean( "visitRumpus" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			ClanRumpusRequest.getBreakfast();
			KoLmafia.forceContinue();
		}
	}

	public static void checkVIPLounge()
	{
		if ( InventoryManager.hasItem( key ) && Preferences.getBoolean( "visitLounge" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			InventoryManager.retrieveItem( key );
			ClanLoungeRequest.getBreakfast();
			KoLmafia.forceContinue();
		}
	}

	public static void readGuildManual()
	{
		if ( Preferences.getBoolean( "readManual" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			int manualId = KoLCharacter.isMuscleClass() ? ItemPool.MUS_MANUAL :
				KoLCharacter.isMysticalityClass() ? ItemPool.MYS_MANUAL : ItemPool.MOX_MANUAL;

			AdventureResult manual = ItemPool.get( manualId, 1 );

			if ( InventoryManager.hasItem( manual ) )
			{
				RequestThread.postRequest( new UseItemRequest( manual ) );
			}

			KoLmafia.forceContinue();
		}
	}

	public static void useCrimboToys()
	{
		if ( Preferences.getBoolean( "useCrimboToys" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			for ( int i = 0; i < toys.length; ++i )
			{
				AdventureResult toy = toys[ i ];
				if ( InventoryManager.hasItem( toy ) )
				{
					int slot = KoLCharacter.equipmentSlot( toy );
					RequestThread.postRequest( new UseItemRequest( toy ) );
					KoLmafia.forceContinue();
					if ( slot != EquipmentManager.NONE && !KoLCharacter.hasEquipped( toy, slot ) )
					{
						RequestThread.postRequest( new EquipmentRequest( toy, slot ) );
						KoLmafia.forceContinue();
					}
				}
			}
		}
	}

	public static void getHermitClovers()
	{
		if ( Preferences.getBoolean( "grabClovers" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			if ( HermitRequest.getWorthlessItemCount() > 0 )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit * ten-leaf clover" );
			}

			KoLmafia.forceContinue();
		}
	}

	public static boolean castSkills( final boolean allowRestore, final int manaRemaining )
	{
		if ( Preferences.getBoolean( "breakfastCompleted" ) )
		{
			return true;
		}

		String skillSetting =
			Preferences.getString( "breakfast" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );

		if ( skillSetting == null )
		{
			return true;
		}

		boolean pathedSummons =
			Preferences.getBoolean( "pathedSummons" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );

		boolean limitExceeded = true;

		for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
		{
			String skill = UseSkillRequest.BREAKFAST_SKILLS[ i ];

			if ( skillSetting.indexOf( skill ) == -1 )
			{
				continue;
			}
			     
			if ( !KoLCharacter.hasSkill( skill ) )
			{
				continue;
			}

			if ( pathedSummons )
			{
				if ( skill.equals( "Pastamastery" ) && !KoLCharacter.canEat() )
				{
					continue;
				}
				if ( skill.equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
				{
					continue;
				}
			}

			limitExceeded &= BreakfastManager.castSkill( skill, allowRestore, manaRemaining );
		}

		return limitExceeded;
	}

	public static boolean castSkill( final String skillName, final boolean allowRestore, final int manaRemaining )
	{
		UseSkillRequest summon = UseSkillRequest.getInstance( skillName );
		// For all other skills, if you don't need to cast them, then
		// skip this step.

		int maximumCast = summon.getMaximumCast();

		if ( maximumCast <= 0 )
		{
			return true;
		}

		int castCount =
			Math.min(
				maximumCast,
				allowRestore ? 5 : ( KoLCharacter.getCurrentMP() - manaRemaining ) / SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( skillName ) ) );

		if ( castCount == 0 )
		{
			return false;
		}

		summon.setBuffCount( castCount );
		RequestThread.postRequest( summon );

		return castCount == maximumCast;
	}

	public static boolean castBookSkills( final boolean allowRestore, final int manaRemaining )
	{
		if ( Preferences.getBoolean( "breakfastCompleted" ) )
		{
			return true;
		}

		String suffix = KoLCharacter.canInteract() ? "Softcore" : "Hardcore";

		boolean done = true;

		done &= castBookSkills( Preferences.getString( "tomeSkills" + suffix ), KoLConstants.TOME, UseSkillRequest.TOME_SKILLS, allowRestore, manaRemaining );
		done &= castBookSkills( Preferences.getString( "grimoireSkills" + suffix ), KoLConstants.GRIMOIRE, UseSkillRequest.GRIMOIRE_SKILLS, allowRestore, manaRemaining );
		done &= castBookSkills( Preferences.getString( "libramSkills" + suffix ), KoLConstants.LIBRAM, UseSkillRequest.LIBRAM_SKILLS, allowRestore, manaRemaining );

		return done;
	}

	public static boolean castBookSkills( final String name, final int type, final String [] skills, final boolean allowRestore, final int manaRemaining )
	{
		if ( name.equals( "none" ) )
		{
			return true;
		}

		boolean castAll = name.equals( "all" );
		int skillCount = 0;
		String skill = null;

		// Determine how many skills we will cast from this list
		for ( int i = 0; i < skills.length; ++i )
		{
			String skillName = skills[ i ];

			if ( !castAll && !name.equals( skillName ) )
			{
				continue;
			}

			if ( !KoLCharacter.hasSkill( skillName ) )
			{
				continue;
			}

			skillCount++;
			skill = skillName;
		}

		// If none, we are done
		if ( skillCount == 0 )
		{
			return true;
		}

		// Determine total number of times we will try to use skills of
		// this type.

		int totalCasts = 0;

		switch ( type )
		{
		case KoLConstants.TOME:
			// Tomes can be used three times a day, spread among
			// all available tomes.
			totalCasts = 3;
			break;
		case KoLConstants.GRIMOIRE:
			// Grimoires can be used once a day, each.
			totalCasts = skillCount;
			break;
		case KoLConstants.LIBRAM:
			// Librams can be used as many times per day as you
			// have mana available.
			totalCasts = SkillDatabase.libramSkillCasts( KoLCharacter.getCurrentMP() - manaRemaining );
			// Note that if we allow MP to be restored, we could
			// potentially summon a lot more. Maybe someday...
			break;
		}

		if ( skillCount == 1 )
		{
			// We are casting exactly one skill from this list.
			return BreakfastManager.castBookSkill( skill, totalCasts, allowRestore, manaRemaining );
		}

		// Determine number of times we will cast each skill. Divide
		// evenly, with any excess going to first skill.

		int nextCast = totalCasts / skillCount;
		int cast = nextCast + totalCasts - ( nextCast * skillCount );

		boolean done = true;

		// We are casting more than one skill from this list. Cast one
		// at a time until we are done.

		for ( int i = 0; i < skills.length; ++i )
		{
			String skillName = skills[ i ];

			if ( !KoLCharacter.hasSkill( skillName ) )
			{
				continue;
			}

			done &= BreakfastManager.castBookSkill( skillName, cast, allowRestore, manaRemaining );
			cast = nextCast;
		}

		return true;
	}

	public static boolean castBookSkill( final String name, final int casts, final boolean allowRestore, final int manaRemaining )
	{
		UseSkillRequest skill = UseSkillRequest.getInstance( name );

		int maximumCast = skill.getMaximumCast();

		if ( maximumCast <= 0 )
		{
			return true;
		}

		int castCount = Math.min( casts, maximumCast );

		if ( castCount > 0 && !allowRestore )
		{
			int available = KoLCharacter.getCurrentMP() - manaRemaining;
			int perCast = SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( name ) );
			castCount = Math.min( castCount, available / perCast );
		}

		if ( castCount == 0 )
		{
			return false;
		}

		skill.setBuffCount( castCount );
		RequestThread.postRequest( skill );

		return castCount == maximumCast;
	}

	public static void visitBigIsland()
	{
		if ( Preferences.getInteger( "lastFilthClearance" ) == KoLCharacter.getAscensions() )
		{
			visitHippy();
		}

		if ( !Preferences.getString( "warProgress" ).equals( "started" ) )
		{
			return;
		}

		SpecialOutfit hippy = EquipmentDatabase.getAvailableOutfit( CoinmastersFrame.WAR_HIPPY_OUTFIT );
		SpecialOutfit fratboy = EquipmentDatabase.getAvailableOutfit( CoinmastersFrame.WAR_FRAT_OUTFIT );

		String lighthouse = Preferences.getString( "sidequestLighthouseCompleted" );
		SpecialOutfit lighthouseOutfit = sidequestOutfit( lighthouse, hippy, fratboy );

		String farm = Preferences.getString( "sidequestFarmCompleted" );
		SpecialOutfit farmOutfit = sidequestOutfit( farm, hippy, fratboy );

		// If we can't get to (or don't need to get to) either
		// sidequest location, nothing more to do.

		if ( lighthouseOutfit == null && farmOutfit == null )
		{
			return;
		}

		// Visit locations accessible in current outfit

		SpecialOutfit current = EquipmentManager.currentOutfit();

		if ( farmOutfit != null && current == farmOutfit )
		{
			visitFarmer();
			farmOutfit = null;
		}

		if ( lighthouseOutfit != null && current == lighthouseOutfit )
		{
			visitPyro();
			lighthouseOutfit = null;
		}

		// Visit locations accessible in one outfit

		current = nextOutfit( farmOutfit, lighthouseOutfit );
		if ( current == null )
		{
			return;
		}

		if ( current == farmOutfit )
		{
			visitFarmer();
			farmOutfit = null;
		}

		if ( current == lighthouseOutfit )
		{
			visitPyro();
			lighthouseOutfit = null;
		}

		// Visit locations accessible in other outfit

		current = nextOutfit( farmOutfit, lighthouseOutfit );
		if ( current == null )
		{
			return;
		}

		if ( current == farmOutfit )
		{
			visitFarmer();
			farmOutfit = null;
		}

		if ( current == lighthouseOutfit )
		{
			visitPyro();
			lighthouseOutfit = null;
		}
	}

	public static void visitHippy()
	{
		KoLmafia.updateDisplay( "Collecting cut of hippy profits..." );
		RequestThread.postRequest( new GenericRequest( "store.php?whichstore=h" ) );
		KoLmafia.forceContinue();
	}

	public static void visitFarmer()
	{
		KoLmafia.updateDisplay( "Collecting produce from farmer..." );
		RequestThread.postRequest( new GenericRequest( "bigisland.php?place=farm&action=farmer&pwd" ) );
		KoLmafia.forceContinue();
	}

	public static void visitPyro()
	{
		KoLmafia.updateDisplay( "Collecting bombs from pyro..." );
		RequestThread.postRequest( new PyroRequest() );
		KoLmafia.forceContinue();
	}

	private static SpecialOutfit nextOutfit( final SpecialOutfit one, final SpecialOutfit two )
	{
		SpecialOutfit outfit = ( one != null ) ? one : two;
		if ( outfit != null )
		{
			RequestThread.postRequest( new EquipmentRequest( outfit ) );
		}
		return outfit;
	}

	public static SpecialOutfit sidequestOutfit( String winner, final SpecialOutfit hippy, final SpecialOutfit fratboy )
	{
		if ( winner.equals( "hippy" ) )
		{
			return hippy;
		}

		if ( winner.equals( "fratboy" ) )
		{
			return fratboy;
		}

		return null;
	}
}
