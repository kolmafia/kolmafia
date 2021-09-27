package net.sourceforge.kolmafia.textui.command;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PocketType;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MonsterPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneResultPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.StatsPocket;

import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CargoCultCommand
	extends AbstractCommand
{
	public CargoCultCommand()
	{
		this.usage =
			"[?] # | monster MONSTER | effect EFFECT | item ITEM | stat STAT - pick pocket from your shorts" +
			KoLConstants.LINE_BREAK + "cargo pocket # - describe contents of specified pocket" +
			KoLConstants.LINE_BREAK + "cargo pick # - pick the specified pocket" +
			KoLConstants.LINE_BREAK + "cargo count ( type TYPE | unpicked TYPE | monster MONSTER | effect EFFECT | item ITEM | stat STAT ) - count matching pockets" +
			KoLConstants.LINE_BREAK + "cargo list ( type TYPE | unpicked TYPE |monster MONSTER | effect EFFECT | item ITEM | stat STAT ) - list matching pockets" +
			KoLConstants.LINE_BREAK + "cargo inspect - check which pockets you've picked" +
			KoLConstants.LINE_BREAK + "cargo demon - check which demon name syllables you've collected" +
			KoLConstants.LINE_BREAK + "cargo - list which pockets you've picked so far during this ascension";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		boolean checking = KoLmafiaCLI.isExecutingCheckOnlyCommand;
		
		if ( parameters.equals( "" ) )
		{
			this.printPickedPockets();
			return;
		}

		String[] split = parameters.split( " +" );
		String command = split[ 0 ];

		if ( command.equals( "pocket" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "cargo pocket POCKET" );
				return;
			}

			Pocket pocket = parsePocket( split[ 1 ] );
			if ( pocket == null )
			{
				// Error message already produced
				return;
			}

			RequestLogger.printLine( "Pocket #" + pocket.getPocket() + " contains " + pocket.toString() );
			return;
		}

		if ( command.equals( "demon" ) )
		{
			Map<Integer, String> known = CargoCultistShortsRequest.knownScrapPockets();
			for ( Pocket p : PocketDatabase.scrapSyllables )
			{
				Integer pocket = p.getPocket();
				String syllable = known.get( pocket );
				RequestLogger.printLine( "Pocket #" + pocket + ": " + p.toString() + ( syllable == null ? "" : " = " + syllable ) );
			}
			return;
		}

		if ( command.equals( "count" ) || command.equals( "list" ) )
		{
			String usage = "cargo " + command + " ( type TYPE | unpicked TYPE | monster MONSTER | item ITEM | effect EFFECT | stat STAT )";
			if ( split.length < 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, usage );
				return;
			}
			String subset = split[ 1 ];
			switch ( subset )
			{
			case "type":
			case "unpicked":
			{
				String tag = split[ 2 ];
				PocketType type = parsePocketType( tag );
				if ( type == null )
				{
					// Error message already issued
					return;
				}
				Map<Integer, Pocket> pockets = PocketDatabase.getPockets( type );
				if ( pockets == null )
				{
					// Error message already issued
					return;
				}
				String modifier = " ";
				if ( subset.equals( "unpicked" ) )
				{
					pockets = PocketDatabase.removePickedPockets( pockets );
					modifier = " unpicked ";
				}
				RequestLogger.printLine( "There are " + pockets.size() + modifier + tag + " pockets." );
				if ( command.equals( "list" ) )
				{
					List<Pocket> sorted = PocketDatabase.sortPockets( type, pockets );
					printPockets( sorted );
				}
				break;
			}
			case "monster":
			{
				String monster = parseMonster( parameters );
				MonsterPocket pocket = getMonsterPocket( monster );
				if ( pocket == null )
				{
					// Error message already issued
					return;
				}
				if ( command.equals( "count" ) )
				{
					RequestLogger.printLine( "There is one pocket that contains a '" + monster + "'." );
				}
				else
				{
					printPocket( pocket );
				}
				break;
			}
			case "effect":
			{
				String effect = parseEffect( parameters );
				Set<OneResultPocket> pockets = getEffectPockets( effect );
				if ( pockets == null )
				{
					// Error message already issued
					return;
				}
				boolean plural = pockets.size() != 1;
				RequestLogger.printLine( "There " + ( plural ? "are " : "is " ) + pockets.size() + " pocket" + ( plural ? "s" : "" ) + " that grant" + ( plural ? "" : "s" ) + " the '" + effect + "' effect." );
				if ( command.equals( "list" ) )
				{
					List<Pocket> sorted = PocketDatabase.sortResults( effect, pockets );
					printPockets( sorted );
				}
				break;
			}
			case "item":
			{
				String item = parseItem( parameters );
				Set<OneResultPocket> pockets = getItemPockets( item );
				if ( pockets == null )
				{
					// Error message already issued
					return;
				}
				boolean plural = pockets.size() != 1;
				RequestLogger.printLine( "There " + ( plural ? "are " : "is " ) + pockets.size() + " pocket" + ( plural ? "s" : "" ) + " that contain" + ( plural ? "" : "s" ) + " a '" + item + "'." );
				if ( command.equals( "list" ) )
				{
					List<Pocket> sorted = PocketDatabase.sortResults( item, pockets );
					printPockets( sorted );
				}
				break;
			}
			case "stat":
			{
				String stat = parseStat( parameters );
				Set<StatsPocket> pockets = getStatsPockets( stat );
				if ( pockets == null )
				{
					// Error message already issued
					return;
				}
				boolean plural = pockets.size() != 1;
				RequestLogger.printLine( "There " + ( plural ? "are " : "is " ) + pockets.size() + " pocket" + ( plural ? "s" : "" ) + " that contain" + ( plural ? "" : "s" ) + " '" + stat + "' stats." );
				if ( command.equals( "list" ) )
				{
					List<Pocket> sorted = PocketDatabase.sortStats( stat, pockets );
					printPockets( sorted );
				}
				break;
			}
			default:
				KoLmafia.updateDisplay( MafiaState.ERROR, usage );
				return;
			}
			return;
		}

		if ( command.equals( "inspect" ) )
		{
			if ( !haveCargoShorts() )
			{
				return;
			}

			CargoCultistShortsRequest visit = new CargoCultistShortsRequest();
			visit.run();

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}

			this.printPickedPockets();
			return;
		}

		if ( command.equals( "monster" ) )
		{
			String monster = parseMonster( parameters );
			MonsterPocket pocket = getMonsterPocket( monster );
			if ( pocket != null )
			{
				pickPocket( checking, pocket );
			}
			return;
		}

		if ( command.equals( "effect" ) )
		{
			String effect = parseEffect( parameters );
			Set<OneResultPocket> pockets = getEffectPockets( effect );
			if ( pockets != null )
			{
				List<Pocket> sorted = PocketDatabase.sortResults( effect, pockets );
				Pocket pocket = firstUnpickedPocket( effect, sorted );
				pickPocket( checking, pocket );
			}
			return;
		}

		if (command.equals( "item" ) )
		{
			String item = parseItem( parameters );
			Set<OneResultPocket> pockets = getItemPockets( item );
			if ( pockets != null )
			{
				List<Pocket> sorted = PocketDatabase.sortResults( item, pockets );
				Pocket pocket = firstUnpickedPocket( item, sorted );
				pickPocket( checking, pocket );
			}
			return;
		}

		if (command.equals( "stat" ) )
		{
			String stat = parseStat( parameters );
			Set<StatsPocket> pockets = getStatsPockets( stat );
			if ( pockets != null )
			{
				List<Pocket> sorted = PocketDatabase.sortStats( stat, pockets );
				Pocket pocket = firstUnpickedPocket( stat, sorted );
				pickPocket( checking, pocket );
			}
			return;
		}

		if ( command.equals( "pick" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "cargo pick POCKET" );
				return;
			}
			Pocket pocket = parsePocket( split[ 1 ] );
			pickPocket( checking, pocket );
			return;
		}

		if ( StringUtilities.isNumeric( command ) )
		{
			Pocket pocket = parsePocket( command );
			pickPocket( checking, pocket );
			return;
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "What does '" + parameters + "' mean?" );
	}

	private Pocket firstUnpickedPocket( String name, List<Pocket> pockets )
	{
		if ( pockets == null )
		{
			// Error message already issued
			return null;
		}

		Pocket result = PocketDatabase.firstUnpickedPocket( pockets );
		if ( result == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No unpicked pockets contain '" + name + "'." );
		}
		return result;
	}

	private void pickPocket( boolean checking, Pocket pocket )
	{
		if ( pocket == null )
		{
			// Error message already produced
			return;
		}

		if ( checking )
		{
			printPocket( pocket );
			return;
		}

		if ( !haveCargoShorts() )
		{
			return;
		}

		CargoCultistShortsRequest pick = new CargoCultistShortsRequest( pocket.getPocket() );
		pick.run();
	}

	private boolean haveCargoShorts()
	{
		if ( InventoryManager.getAccessibleCount( ItemPool.CARGO_CULTIST_SHORTS ) == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a pair of Cargo Cultist Shorts" );
			return false;
		}
		return true;
	}

	private Pocket parsePocket( String input )
	{
		if ( !StringUtilities.isNumeric( input ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Specify a pocket # from 1-666" );
			return null;
		}

		int pocket = StringUtilities.parseInt( input );
		if ( pocket < 1 || pocket > 666 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Pocket must be from 1-666" );
			return null;
		}

		return PocketDatabase.pocketByNumber( pocket );
	}

	private PocketType parsePocketType( String tag )
	{
		PocketType type = PocketDatabase.getPocketType( tag );
		if ( type == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What is type '" + tag + "'?" );
			return null;
		}
		return type;
	}

	private String parseMonster( String parameters )
	{
		String name = parseName( "monster", parameters );
		MonsterData monster;
		if ( StringUtilities.isNumeric( name ) )
		{
			int monsterId = StringUtilities.parseInt( parameters );
			monster = MonsterDatabase.findMonsterById( monsterId );
			if ( monster == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "What is monster #" + monsterId + "?" );
				return null;
			}
			return monster.getName();
		}
		monster = MonsterDatabase.findMonster( name, false, false );
		if ( monster == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What is monster '" + name + "'?" );
			return null;
		}
		return monster.getName();
	}

	private String parseEffect( String parameters )
	{
		String name = parseName( "effect", parameters );
		int effectId;
		if ( StringUtilities.isNumeric( name ) )
		{
			effectId = StringUtilities.parseInt( parameters );
			name = EffectDatabase.getEffectName( effectId );
			if ( name == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "What is effect #" + effectId + "?" );
			}
			return name;
		}
		effectId = EffectDatabase.getEffectId( name, false );
		if ( effectId == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What is effect '" + name + "'?" );
			return null;
		}
		return EffectDatabase.getEffectName( effectId );
	}

	private String parseItem( String parameters )
	{
		String name = parseName( "item", parameters );
		int itemId;
		if ( StringUtilities.isNumeric( name ) )
		{
			itemId = StringUtilities.parseInt( parameters );
			name = ItemDatabase.getDataName( itemId );
			if ( name == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "What is item #" + itemId + "?" );
			}
			return name;
		}
		itemId = ItemDatabase.getItemId( name, 1, true );
		if ( itemId == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What is item '" + name + "'?" );
			return null;
		}
		return ItemDatabase.getDataName( itemId );
	}

	private String parseStat( String parameters )
	{
		String name = parseName( "stat", parameters );
		return name;
	}

	private String parseName( String type, String parameters )
	{
		int index = parameters.indexOf( type + " " );
		if ( index == -1 )
		{
			return "";
		}
		return parameters.substring( parameters.indexOf( " ", index ) ).trim();
	}

	private Map<Integer, Pocket> getPockets( String tag )
	{
		return PocketDatabase.getPockets( parsePocketType( tag ) );
	}

	private MonsterPocket getMonsterPocket( String monsterName )
	{
		if ( monsterName == null )
		{
			// Error message already produced
			return null;
		}
		MonsterPocket pocket = PocketDatabase.monsterPockets.get( monsterName.toLowerCase() );
		if ( pocket == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not contain a monster named '" + monsterName + "'." );
		}
		return pocket;
	}

	private Set<OneResultPocket> getEffectPockets( String effectName )
	{
		if ( effectName == null )
		{
			// Error message already produced
			return null;
		}
		Set<OneResultPocket> pockets = PocketDatabase.effectPockets.get( effectName );
		if ( pockets == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not contain an effect named '" + effectName + "'." );
		}
		return pockets;
	}

	private Set<OneResultPocket> getItemPockets( String itemName )
	{
		if ( itemName == null )
		{
			// Error message already produced
			return null;
		}
		Set<OneResultPocket> pockets = PocketDatabase.itemPockets.get( itemName );
		if ( pockets == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not contain an item named '" + itemName + "'." );
		}
		return pockets;
	}

	private Set<StatsPocket> getStatsPockets( String stat )
	{
		if ( stat == null )
		{
			// Error message already produced
			return null;
		}
		Set<StatsPocket> pockets = PocketDatabase.statsPockets.get( stat.toLowerCase() );
		if ( pockets == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not produce stat '" + stat + "'." );
		}
		return pockets;
	}

	private void printPockets( final Collection<Pocket> pockets )
	{
		for ( Pocket p : pockets )
		{
			printPocket( p );
		}
	}

	private void printPocket( final Pocket p )
	{
		RequestLogger.printLine( "Pocket #" + p.getPocket() + ": " + p.toString() );
	}

	private void printPickedPockets()
	{
		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		if ( pockets.size() == 0 )
		{
			RequestLogger.printLine( "You have not picked any pockets yet during this ascension." );
			return;
		}

		RequestLogger.printLine( "You have picked the following pockets during this ascension:" );
		for ( Integer pocket : pockets )
		{
			RequestLogger.printLine( String.valueOf( pocket ) );
		}
	}
}
