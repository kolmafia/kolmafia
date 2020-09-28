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
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PocketType;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MeatPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MonsterPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneEffectPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneItemPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PoemPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.ScrapPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.TwoEffectPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.TwoItemPocket;

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
			int pocket = parsePocket( split[ 1 ] );
			if ( pocket != 0 )
			{
				Pocket data = PocketDatabase.pocketByNumber( pocket );
				if ( data == null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "No data for pocket #" + pocket );
				}
				else
				{
					RequestLogger.printLine( "Pocket #" + pocket + " contains " + data.toString() );
				}
				return;
			}
			// Error message already produced
			return;
		}

		if ( command.equals( "jokes" ) )
		{
			printPocketMap( PocketDatabase.getPockets( PocketType.JOKE ) );
			return;
		}

		if ( command.equals( "meat" ) )
		{
			printPockets( PocketDatabase.meatClues );
			return;
		}

		if ( command.equals( "poem" ) )
		{
			printPockets( PocketDatabase.poemVerses );
			return;
		}

		if ( command.equals( "scraps" ) || command.equals( "demon" ) )
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

		if ( command.equals( "count" ) )
		{
			if ( split.length < 3 || !split[ 1 ].equals( "type" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "cargo count type TYPE" );
				return;
			}
			String tag = split[ 2 ];
			Map<Integer, Pocket> pockets = getPockets( tag );
			if ( pockets == null )
			{
				// Error message already issued
				return;
			}
			RequestLogger.printLine( "There are " + pockets.size() + " " + tag + " pockets." );
			return;
		}

		if ( command.equals( "list" ) )
		{
			if ( split.length < 3 || !split[ 1 ].equals( "type" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "cargo list type TYPE" );
				return;
			}
			String tag = split[ 2 ];
			PocketType type = getPocketType( tag );
			if ( type == null )
			{
				// Error message already issued
				return;
			}
			Map<Integer, Pocket> pockets = PocketDatabase.getPockets( type );
			RequestLogger.printLine( "There are " + pockets.size() + " " + tag + " pockets." );
			Collection<Pocket> sorted = sortPockets( type, pockets );
			printPockets( sorted );
			return;
		}

		if ( InventoryManager.getAccessibleCount( ItemPool.CARGO_CULTIST_SHORTS ) == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a pair of Cargo Cultist Shorts" );
			return;
		}

		if ( command.equals( "inspect" ) )
		{
			CargoCultistShortsRequest visit = new CargoCultistShortsRequest();
			visit.run();

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}

			this.printPickedPockets();
			return;
		}

		if ( StringUtilities.isNumeric( command ) )
		{
			int pocket = parsePocket( command );
			if ( pocket != 0 )
			{
				CargoCultistShortsRequest pick = new CargoCultistShortsRequest( pocket );
				pick.run();
				return;
			}
			// Error message already produced
			return;
		}
	}

	private PocketType getPocketType( String tag )
	{
		PocketType type = PocketDatabase.getPocketType( tag );
		if ( type == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What is type '" + tag + "'?" );
			return null;
		}
		return type;
	}

	private Map<Integer, Pocket> getPockets( String tag )
	{
		PocketType type = getPocketType( tag );
		return ( type == null ) ? null : PocketDatabase.getPockets( type );
	}

	private Collection<Pocket> sortPockets( PocketType type, Map<Integer, Pocket> pockets )
	{
		// PocketType is derivable from the first pocket in the
		// collection, but easy enough to pass it in.
		Collection<Pocket> values = pockets.values();
		switch ( type )
		{
		case SCRAP:
			// Sort on scrap index
			return values.stream().sorted( Comparator.comparing(p -> ((ScrapPocket) p).getScrap() ) ).collect( Collectors.toList() );
		case MEAT:
			// Sort on Meat
			return values.stream().sorted( Comparator.comparing(p -> ((MeatPocket) p).getMeat() ) ).collect( Collectors.toList() );
		case POEM:
			// Sort on line index
			return values.stream().sorted( Comparator.comparing(p -> ((PoemPocket) p).getIndex() ) ).collect( Collectors.toList() );
		case MONSTER:
			// Monsters sort on monster name
			return values.stream().sorted( Comparator.comparing(p -> ((MonsterPocket) p).getMonster().getName().toLowerCase() ) ).collect( Collectors.toList() );
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
			// Single items sort on item name
			return values.stream().sorted( Comparator.comparing(p -> ((OneItemPocket) p).getItem().getName() ) ).collect( Collectors.toList() );
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
			// Single effects with single sources sort on effect name
			return values.stream().sorted( Comparator.comparing(p -> ((OneEffectPocket) p).getEffect1().getName() ) ).collect( Collectors.toList() );
		case COMMON:
		case ELEMENT:
			// Single effects with multiple sources sort first on effect name then  on pocket number then
			return values.stream().sorted( Comparator.comparing(p -> ((OneEffectPocket) p).getEffect1().getName()).thenComparing(p -> ((Pocket) p).getPocket() ) ).collect( Collectors.toList() );
		case ITEM2:
			// Sort first on item 1 then on item 2
			return values.stream().sorted( Comparator.comparing(p -> ((TwoItemPocket) p).getItem().getName()).thenComparing(p -> ((TwoItemPocket) p).getItem2().getName() ) ).collect( Collectors.toList() );
		case CANDY:
		case CHIPS:
		case GUM:
		case LENS:
		case NEEDLE:
		case TEETH:
			// Sort first on effect 1 then on effect 2
			return values.stream().sorted( Comparator.comparing(p -> ((TwoEffectPocket) p).getEffect1().getName()).thenComparing(p -> ((TwoEffectPocket) p).getEffect2().getName() ) ).collect( Collectors.toList() );
		case STATS:
			// *** What here?
		case JOKE:
		default:
			// Pocket number is good enough
			return values.stream().sorted( Comparator.comparing(Pocket::getPocket) ).collect( Collectors.toList() );
		}
	}

	private int parsePocket( String input )
	{
		if ( StringUtilities.isNumeric( input ) )
		{
			int pocket = StringUtilities.parseInt( input );

			if ( pocket < 1 || pocket > 666 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Pocket must be from 1-666" );
				return 0;
			}

			return pocket;
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "Specify a pocket # from 1-666" );
		return 0;
	}

	private void printPockets( final Collection<Pocket> pockets )
	{
		for ( Pocket p : pockets )
		{
			RequestLogger.printLine( "Pocket #" + p.getPocket() + ": " + p.toString() );
		}
	}

	private void printPocketMap( final Map<Integer, Pocket> map )
	{
		printPockets( map.values() );
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
