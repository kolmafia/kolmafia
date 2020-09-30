/**
 * Copyright (c) 2005-2020, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PocketType;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MeatPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MonsterPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneResultPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PoemPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.ScrapPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.TwoResultPocket;

import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CargoCultCommand
	extends AbstractCommand
{
	public CargoCultCommand()
	{
		this.usage = " inspect | [pocket] - get status of Cargo Cult Shorts, or pick a pocket.";
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

			RequestLogger.printLine( "Pocket #" + pocket + " contains " + pocket.toString() );
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
			String usage = "cargo " + command + " ( type TYPE | effect EFFECT )";
			if ( split.length < 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, usage );
				return;
			}
			switch ( split[ 1 ] )
			{
			case "type":
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
				RequestLogger.printLine( "There are " + pockets.size() + " " + tag + " pockets." );
				if ( command.equals( "list" ) )
				{
					List<Pocket> sorted = sortPockets( type, pockets );
					printPockets( sorted );
				}
				break;
			}
			case "monster":
			{
				String monster = parseName( "monster", parameters );
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
				String effect = parseName( "effect", parameters );
				Set<OneResultPocket> pockets = getEffectPockets( effect );
				if ( pockets == null )
				{
					// Error message already issued
					return;
				}
				boolean plural = pockets.size() != 1;
				RequestLogger.printLine( "There " + ( plural ? "are " : "is " ) + pockets.size() + " pocket" + ( plural ? "s" : "" ) + " that grant" + ( plural ? "" : "s" ) + " only the '" + effect + "' effect." );
				if ( command.equals( "list" ) )
				{
					List<Pocket> sorted = sortResults( effect, pockets );
					printPockets( sorted );
				}
				break;
			}
			case "item":
			{
				String item = parseName( "item", parameters );
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
					List<Pocket> sorted = sortResults( item, pockets );
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
			String monster = parseName( "monster", parameters );
			MonsterPocket pocket = getMonsterPocket( monster );
			pickPocket( checking, pocket );
			return;
		}

		if ( command.equals( "effect" ) )
		{
			String effect = parseName( "effect", parameters );
			Set<OneResultPocket> pockets = getEffectPockets( effect );
			Pocket pocket = firstUnpickedPocket( effect, pockets );
			pickPocket( checking, pocket );
			return;
		}

		if (command.equals( "item" ) )
		{
			String item = parseName( "item", parameters );
			Set<OneResultPocket> pockets = getItemPockets( item );
			Pocket pocket = firstUnpickedPocket( item, pockets );
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

	private Pocket firstUnpickedPocket( String name, Set<OneResultPocket> pockets )
	{
		if ( pockets == null )
		{
			// Error message already issued
			return null;
		}

		Set<Integer> picked = CargoCultistShortsRequest.pickedPockets;
		for ( Pocket pocket : sortResults( name, pockets ) )
		{
			if ( !picked.contains( IntegerPool.get( pocket.getPocket() ) ) )
			{
				return pocket;
			}
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "No unpicked pockets contain '" + name + "'." );
		return null;
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
		MonsterPocket pocket = PocketDatabase.monsterPockets.get( monsterName.toLowerCase() );
		if ( pocket == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not contain a monster named '" + monsterName + "'." );
		}
		return pocket;
	}

	private Set<OneResultPocket> getEffectPockets( String effectName )
	{
		Set<OneResultPocket> pockets = PocketDatabase.effectPockets.get( effectName );
		if ( pockets == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not contain an effect named '" + effectName + "'." );
		}
		return pockets;
	}

	private Set<OneResultPocket> getItemPockets( String itemName )
	{
		Set<OneResultPocket> pockets = PocketDatabase.itemPockets.get( itemName );
		if ( pockets == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your shorts do not contain an item named '" + itemName + "'." );
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

	private List<Pocket> sortPockets( PocketType type )
	{
		return sortPockets( type, PocketDatabase.getPockets( type ) );
	}

	private List<Pocket> sortPockets( PocketType type, Map<Integer, Pocket> pockets )
	{
		// PocketType is derivable from the first pocket in the
		// collection, but since caller always knows it, pass in
		switch ( type )
		{
		case SCRAP:
			// Sort on scrap index. Created at database load, since it is used elsewhere.
			return PocketDatabase.scrapSyllables;
		case MEAT:
			// Sort on Meat
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(p -> ((MeatPocket) p).getMeat() ) )
				.collect( Collectors.toList() );
		case POEM:
			// Sort on line index
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(p -> ((PoemPocket) p).getIndex() ) )
				.collect( Collectors.toList() );
		case MONSTER:
			// Sort on monster name
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(p -> ((MonsterPocket) p).getMonster().getName().toLowerCase() ) )
				.collect( Collectors.toList() );
		case ITEM:
		case AVATAR:
		case BELL:
		case BOOZE:
		case CASH:
		case CHESS:
		case CHOCO:
		case FOOD:
		case FRUIT:
		case OYSTER:
		case POTION:
		case YEG:
		case EFFECT:
		case RESTORE:
		case BUFF:
		case CANDY1:
		case CANDY2:
		case CHIPS1:
		case GUM1:
		case LENS1:
		case NEEDLE1:
		case TEETH1:
			// Single results with a single source sort on effect name
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(p -> ((OneResultPocket) p).getResult1().getName() ) )
				.collect( Collectors.toList() );
		case COMMON:
		case ELEMENT:
			// Single effects with multiple sources sort first on effect name then on pocket number
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(p -> ((OneResultPocket) p).getResult1().getName())
					 	   .thenComparing(p -> ((Pocket) p).getPocket() ) )
				.collect( Collectors.toList() );
		case ITEM2:
		case CANDY:
		case CHIPS:
		case GUM:
		case LENS:
		case NEEDLE:
		case TEETH:
			// Two results sort first on result 1 then on result 2
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(p -> ((TwoResultPocket) p).getResult1().getName() )
					 	   .thenComparing(p -> ((TwoResultPocket) p).getResult2().getName() ) )
				.collect( Collectors.toList() );
		case STATS:
			// *** What here?
		case JOKE:
		default:
			// Pocket number is good enough
			return pockets.values()
				.stream()
				.sorted( Comparator.comparing(Pocket::getPocket) )
				.collect( Collectors.toList() );
		}
	}

	private List<Pocket> sortResults( String name, Set<OneResultPocket> pockets )
	{
		return pockets
			.stream()
			.sorted( Comparator.comparing(p -> ((OneResultPocket) p).getCount( name ) ).reversed()
				 .thenComparing(p -> ((Pocket) p).getPocket() ) )
			.collect( Collectors.toList() );
	}
}
