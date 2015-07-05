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

package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DeckOfEveryCardRequest
	extends GenericRequest
{
	private static final TreeMap<Integer,EveryCard> idToCard = new TreeMap<Integer,EveryCard>();
	private static final TreeMap<String,EveryCard> canonicalNameToCard = new TreeMap<String,EveryCard>();
	private static final TreeMap<Phylum,EveryCard> phylumToCard = new TreeMap<Phylum,EveryCard>();

	static
	{
		registerCard( 1, "X of Clubs" );		// gives X seal-clubbing clubs, and 3 PvP fights
		registerCard( 3, "X of Diamonds" );		// gives X hyper-cubic zirconiae
		registerCard( 2, "X of Hearts" );		// gives X bubblegum hearts
		registerCard( 4, "X of Spades" );		// gives X grave-robbing shovels, and X letters of a string
		registerCard( 42, "X of Papayas" );		// gives X papayas
		registerCard( 65, "X of Kumquats" );		// gives X kumquats
		registerCard( 43, "X of Salads" );		// gives X delicious salads
		registerCard( 5, "X of Cups" );			// gives X random boozes
		registerCard( 8, "X of Coins" );		// gives X valuable coins
		registerCard( 7, "X of Swords" );		// gives X swords
		registerCard( 6, "X of Wands" );		// gives 5 turns of X random buffs.
		registerCard( 47, "XVI - The Tower" );		// Gives a random hero tower key
		registerCard( 66, "Professor Plum" );		// Get 10 plums
		registerCard( 59, "Spare Tire" );		// Get a tires
		registerCard( 60, "Extra Tank" );		// Get a full meat tank
		registerCard( 61, "Sheep" );			// Get 3 stone wool
		registerCard( 62, "Year of Plenty" );		// Get 5 random foods.
		registerCard( 63, "Mine" );			// Get one each of asbestos ore, linoleum ore, and chrome ore
		registerCard( 64, "Laboratory" );		// Get five random potions

		// The following give new items
		registerCard( 31, "Plains" );			// Gives a white mana
		registerCard( 32, "Swamp" );			// Gives a black mana
		registerCard( 33, "Mountain" );			// Gives a red mana
		registerCard( 34, "Forest" );			// Gives a green mana
		registerCard( 35, "Island" );			// Gives a blue mana
		registerCard( 52, "Lead Pipe" );			// Get a Lead Pipe
		registerCard( 53, "Rope" );			// Get a Rope
		registerCard( 54, "Wrench" );			// Get a Wrench
		registerCard( 55, "Candlestick" );		// Get a Candlestick
		registerCard( 56, "Knife" );			// Get a Knife
		registerCard( 57, "Revolver" );			// Get a Revolver
		registerCard( 41, "Gift Card" );		// Get a Gift Card
		registerCard( 58, "1952 Mickey Mantle" );	// Get a 1952 Mickey Mantle card

		// The following give stats
		registerCard( 70, "III - The Empress" );		// Gives 500 mysticality substats
		registerCard( 69, "VI - The Lovers" );		// Gives 500 moxie substats
		registerCard( 68, "XXI - The World" );		// Gives 500 muscle substats

		// The following give skills
		registerCard( 36, "Healing Salve" );		// Gives the skill Healing Salve
		registerCard( 37, "Dark Ritual" );		// Gives the skill Dark Ritual
		registerCard( 38, "Lightning Bolt" );		// Gives the skill Lightning Bolt
		registerCard( 39, "Giant Growth" );		// Gives the skill Giant Growth
		registerCard( 40, "Ancestral Recall" );		// Gives the skill Ancestral Recall

		// The following give buffs
		registerCard( 49, "0 - The Fool" );		// Gives 20 turns of Dancin' Fool (+200% moxie)
		registerCard( 50, "I - The Magician" );		// Gives 20 turns of Magicianship (+200% mysticality)
		registerCard( 67, "X - The Wheel of Fortune" );	// Gives 20 turns of a +100% item drops buff
		registerCard( 51, "XI - Strength" );		// Gives 20 turns of Strongly Motivated (+200% muscle)
		registerCard( 48, "The Race Card" );		// Gives 20 turns of Racing! (+200% init)

		// The following lead to fights
		registerCard( 46, "Green Card" );		// Fight a legal alien
		registerCard( 45, "IV - The Emperor" );		// Fight The Emperor (drops The Emperor's dry cleaning)
		registerCard( 44, "IX - The Hermit" );		// Fight The Hermit

		registerCard( 15, "Werewolf", Phylum.BEAST );			// Fight a random Beast
		registerCard( 11, "The Hive", Phylum.BUG );			// Fight a random Bug
		registerCard( 26, "XVII - The Star", Phylum.CONSTELLATION );	// Fight a random Constellation
		registerCard( 18, "VII - The Chariot", Phylum.CONSTRUCT );	// Fight a random Construct
		registerCard( 16, "XV - The Devil", Phylum.DEMON );		// Fight a random Demon
		registerCard( 13, "V - The Hierophant", Phylum.DUDE );		// Fight a random Dude
		registerCard( 17, "Fire Elemental", Phylum.ELEMENTAL );		// Fight a random Elemental
		registerCard( 28, "Christmas Card", Phylum.ELF );		// Fight a random Elf
		registerCard( 29, "Go Fish", Phylum.FISH );			// Fight a random Fish
		registerCard( 10, "Goblin Sapper", Phylum.GOBLIN );		// Fight a random Goblin
		registerCard( 20, "II - The High Priestess", Phylum.HIPPY );	// Fight a random Hippy
		registerCard( 24, "XIV - Temperance", Phylum.HOBO );		// Fight a random Hobo
		registerCard( 14, "XVIII - The Moon", Phylum.HORROR );		// Fight a random Horror
		registerCard( 12, "Hunky Fireman Card", Phylum.HUMANOID );	// Fight a random Humanoid
		registerCard( 30, "Aquarius Horoscope", Phylum.MER_KIN );	// Fight a random Mer-Kin
		registerCard( 21, "XII - The Hanged Man", Phylum.ORC );		// Fight a random Orc
		registerCard( 27, "Suit Warehouse Discount Card", Phylum.PENGUIN );	// Fight a random Penguin
		registerCard( 23, "Pirate Birthday Card", Phylum.PIRATE );	// Fight a random Pirate
		registerCard( 22, "Plantable Greeting Card", Phylum.PLANT );	// Fight a random Plant
		registerCard( 18, "Slimer Trading Card", Phylum.SLIME );		// Fight a random Slime
		registerCard( 9, "XIII - Death", Phylum.UNDEAD );		// Fight a random Undead
		registerCard( 25, "Unstable Portal", Phylum.WEIRD );		// Fight a random Weird
	};

	private static EveryCard registerCard( int id, String name )
	{
		EveryCard card = new EveryCard( id, name );
		DeckOfEveryCardRequest.idToCard.put( id, card );
		DeckOfEveryCardRequest.canonicalNameToCard.put( StringUtilities.getCanonicalName( name ), card );
		return card;
	}

	private static void registerCard( int id, String name, Phylum phylum )
	{
		EveryCard card = DeckOfEveryCardRequest.registerCard( id, name );
		card.phylum = phylum;
		DeckOfEveryCardRequest.phylumToCard.put( phylum, card );
	}

	private static String [] CANONICAL_CARDS_ARRAY;
	static
	{
		Set<String> keys = DeckOfEveryCardRequest.canonicalNameToCard.keySet();
		DeckOfEveryCardRequest.CANONICAL_CARDS_ARRAY = keys.toArray( new String[ keys.size() ] );
	};

	public static final List<String> getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames( DeckOfEveryCardRequest.CANONICAL_CARDS_ARRAY, substring );
	}

	public static EveryCard phylumToCard( Phylum phylum )
	{
		return DeckOfEveryCardRequest.phylumToCard.get( phylum );
	}

	public static EveryCard canonicalNameToCard( String name )
	{
		return DeckOfEveryCardRequest.canonicalNameToCard.get( name );
	}

	private EveryCard card;

	public DeckOfEveryCardRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1085" );
		this.addFormField( "option", "1" );
		this.card = null;
	}

	public DeckOfEveryCardRequest( EveryCard card )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1086" );
		this.addFormField( "option", "1" );
		this.addFormField( "which", String.valueOf( card.id ) );
		this.card = card;
	}

	@Override
	public void run()
	{
		if ( this.card == null )
		{
			RequestLogger.printLine( "Playing a random card" );
		}
		else
		{
			RequestLogger.printLine( "Playing " + this.card );
		}
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );
		if ( choice != 1085 && choice != 1086 )
		{
			return;
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );
		if ( choice != 1085 && choice != 1086 )
		{
			return false;
		}

		return false;
	}

	public static class EveryCard
	{
		public int id;
		public String name;
		public Phylum phylum;
		private String stringForm;

		public EveryCard( int id, String name )
		{
			this.id = id;
			this.name = name;
			this.phylum = null;
			this.stringForm = name + " (" + id + ")";
		}

		public EveryCard( int id, String name, Phylum phylum )
		{
			this( id, name );
			this.phylum = phylum;
		}

		@Override
		public String toString()
		{
			return this.stringForm; 
		}
	}
}
