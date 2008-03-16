package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class BreakfastManager
{
	public static void getBreakfast( final boolean checkSettings, final boolean runComplete )
	{
		SpecialOutfit.createImplicitCheckpoint();

		if ( runComplete )
		{
			checkCampground();
			checkRumpusRoom();
			readGuildManual();
			useCrimboToys();
			getHermitClovers();
			visitBigIsland();
		}

		castSkills( checkSettings, 0 );

		SpecialOutfit.restoreImplicitCheckpoint();
		KoLmafia.forceContinue();
	}

	public static void checkCampground()
	{
		if ( KoLCharacter.hasToaster() )
		{
			for ( int i = 0; i < 3 && KoLmafia.permitsContinue(); ++i )
			{
				RequestThread.postRequest( new CampgroundRequest( "toast" ) );
			}

			KoLmafia.forceContinue();
		}
	}

	public static void checkRumpusRoom()
	{
		if ( Preferences.getBoolean( "visitRumpus" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			RequestThread.postRequest( new ClanRumpusRequest( ClanRumpusRequest.SEARCH ) );
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
			AdventureResult [] toys = new AdventureResult []
			{
				ItemPool.get( ItemPool.HOBBY_HORSE, 1 ),
				ItemPool.get( ItemPool.BALL_IN_A_CUP, 1 ),
				ItemPool.get( ItemPool.SET_OF_JACKS, 1 )
			};

			for ( int i = 0; i < toys.length; ++i )
			{
				if ( InventoryManager.hasItem( toys[ i ] ) )
				{
					RequestThread.postRequest( new UseItemRequest( toys[ i ] ) );
					KoLmafia.forceContinue();
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

	public static void castSkills( final boolean checkSettings, final int manaRemaining )
	{
		BreakfastManager.castSkills(
			checkSettings,
			Preferences.getBoolean( "loginRecovery" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ),
			manaRemaining );
	}

	public static boolean castSkills( boolean checkSettings, final boolean allowRestore, final int manaRemaining )
	{
		if ( Preferences.getBoolean( "breakfastCompleted" ) )
		{
			return true;
		}

		boolean shouldCast = false;
		boolean limitExceeded = true;

		String skillSetting =
			Preferences.getString( "breakfast" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );
		boolean pathedSummons =
			Preferences.getBoolean( "pathedSummons" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );

		if ( skillSetting != null )
		{
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				shouldCast = !checkSettings || skillSetting.indexOf( UseSkillRequest.BREAKFAST_SKILLS[ i ] ) != -1;
				shouldCast &= KoLCharacter.hasSkill( UseSkillRequest.BREAKFAST_SKILLS[ i ] );

				if ( checkSettings && pathedSummons )
				{
					if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
					{
						shouldCast = false;
					}
					if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
					{
						shouldCast = false;
					}
				}

				if ( shouldCast )
				{
					limitExceeded &=
						BreakfastManager.castSkill( UseSkillRequest.BREAKFAST_SKILLS[ i ], allowRestore, manaRemaining );
				}
			}
		}

		Preferences.setBoolean( "breakfastCompleted", limitExceeded );
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
		RequestThread.postRequest( new GenericRequest( "bigisland.php?place=lighthouse&action=pyro&pwd" ) );
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
