/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import java.util.Stack;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class SpecialOutfit implements Comparable, KoLConstants
{
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option value=(.*?)>(.*?)</option>" );

	private static Stack implicitPoints = new Stack();
	private static Stack explicitPoints = new Stack();

	private int outfitId;
	private String outfitName;
	private ArrayList pieces;

	public static final String NO_CHANGE = " - No Change - ";
	public static final SpecialOutfit BIRTHDAY_SUIT = new SpecialOutfit();

	private static SpecialOutfit implicitOutfit = null;
	private static int markedCheckpoint = -1;

	private SpecialOutfit()
	{
		this.outfitId = Integer.MAX_VALUE;
		this.outfitName = "Birthday Suit";
		this.pieces = new ArrayList();
	}

	public SpecialOutfit( int outfitId, String outfitName )
	{
		this.outfitId = outfitId;
		this.outfitName = outfitName;
		this.pieces = new ArrayList();
	}

	public boolean hasAllPieces()
	{
		for ( int i = 0; i < pieces.size(); ++i )
		{
			boolean itemAvailable = KoLCharacter.hasItem( (AdventureResult) pieces.get(i) ) &&
				EquipmentDatabase.canEquip( ((AdventureResult) pieces.get(i)).getName() );

			if ( !itemAvailable )
				return false;
		}

		return true;
	}

	public boolean isWearing()
	{
		for ( int i = 0; i < pieces.size(); ++i )
			if ( !KoLCharacter.hasEquipped( (AdventureResult) pieces.get(i) ) )
				return false;

		return true;
	}

	public AdventureResult [] getPieces()
	{
		AdventureResult [] piecesArray = new AdventureResult[ pieces.size() ];
		pieces.toArray( piecesArray );
		return piecesArray;
	}

	public void addPiece( AdventureResult piece )
	{	this.pieces.add( piece );
	}

	public String toString()
	{	return outfitName;
	}

	public int getOutfitId()
	{	return outfitId;
	}

	public boolean equals( Object o )
	{
		if ( o == null || !(o instanceof SpecialOutfit) )
			return false;

		if ( outfitId != ((SpecialOutfit)o).outfitId )
			return false;

		return outfitName.equalsIgnoreCase( ((SpecialOutfit)o).outfitName );
	}

	public int compareTo( Object o )
	{
		if ( o == null || !(o instanceof SpecialOutfit) )
			return -1;

		return outfitName.compareToIgnoreCase( ((SpecialOutfit)o).outfitName );
	}

	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */

	private static void restoreCheckpoint( AdventureResult [] checkpoint )
	{
		RequestThread.openRequestSequence();

		AdventureResult equippedItem;
		for ( int i = 0; i < checkpoint.length && !KoLmafia.refusesContinue(); ++i )
		{
			if ( checkpoint[i] == null )
				continue;

			equippedItem = KoLCharacter.getEquipment( i );
			if ( checkpoint[i].equals( EquipmentRequest.UNEQUIP ) || equippedItem.equals( checkpoint[i] ) )
				continue;

			RequestThread.postRequest( new EquipmentRequest( checkpoint[i], i ) );
		}

		RequestThread.closeRequestSequence();
	}

	/**
	 * Creates a checkpoint.  This should be called whenever
	 * the player needs an outfit marked to revert to.
	 */

	public static void createExplicitCheckpoint()
	{
		AdventureResult [] explicit = new AdventureResult[ KoLCharacter.FAMILIAR + 1 ];

		for ( int i = 0; i < explicit.length; ++i )
			explicit[i] = KoLCharacter.getEquipment(i);

		explicitPoints.push( explicit );
	}

	public static boolean markImplicitCheckpoint()
	{
		if ( markedCheckpoint != -1 || implicitPoints.isEmpty() )
			return false;

		boolean isIdentical = true;
		String itemName;

		for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
		{
			itemName = KoLCharacter.getEquipment(i).getName();
			isIdentical &= StaticEntity.getProperty( "implicitEquipmentSlot" + i ).equals( itemName );
		}

		markedCheckpoint = implicitPoints.size();
		return !isIdentical;
	}

	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */

	public static void restoreExplicitCheckpoint()
	{
		if ( explicitPoints.isEmpty() )
			return;

		restoreCheckpoint( (AdventureResult []) explicitPoints.pop() );
	}

	/**
	 * Creates a checkpoint.  This should be called whenever
	 * the player needs an outfit marked to revert to.
	 */

	public static void createImplicitCheckpoint()
	{
		AdventureResult [] implicit = new AdventureResult[ KoLCharacter.FAMILIAR + 1 ];

		for ( int i = 0; i < implicit.length; ++i )
			implicit[i] = KoLCharacter.getEquipment(i);

		implicitPoints.push( implicit );

		if ( StaticEntity.getBooleanProperty( "useFastOutfitSwitch" ) )
			EquipmentRequest.savePreviousOutfit();
	}

	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */

	public static void restoreImplicitCheckpoint()
	{
		if ( implicitPoints.isEmpty() )
			return;

		AdventureResult [] implicit = (AdventureResult []) implicitPoints.pop();

		UseSkillRequest.restoreEquipment();

		if ( implicitPoints.size() < markedCheckpoint )
		{
			RequestThread.postRequest( new EquipmentRequest( implicitOutfit ) );
			markedCheckpoint = -1;
		}
		else if ( markedCheckpoint == -1 )
		{
			restoreCheckpoint( implicit );
		}
	}

	/**
	 * Static method used to determine all of the custom outfits,
	 * based on the given HTML enclosed in <code><select></code> tags.
	 *
	 * @return	A list of available outfits
	 */

	public static LockableListModel parseOutfits( String selectHTML )
	{
		Matcher singleOutfitMatcher = OPTION_PATTERN.matcher( selectHTML );

		int outfitId;
		String outfitName;
		SpecialOutfit outfit;

		implicitOutfit = null;
		SortedListModel outfits = new SortedListModel();

		while ( singleOutfitMatcher.find() )
		{
			outfitId = StaticEntity.parseInt( singleOutfitMatcher.group(1) );
			if ( outfitId < 0 )
			{
				outfitName = singleOutfitMatcher.group(2);
				outfit = new SpecialOutfit( outfitId, singleOutfitMatcher.group(2) );

				if ( outfitName.equals( "Custom: Backup" ) )
					implicitOutfit = outfit;

				outfits.add( outfit );
			}
		}

		return outfits;
	}
}
