/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * An encapsulation of a special outfit.  This includes
 * custom outfits as well as standard in-game outfits.
 */

public class SpecialOutfit implements Comparable
{
	private int outfitID;
	private String outfitName;
	private ArrayList pieces;

	public static SpecialOutfit CHECKPOINT = null;
	public static final String NO_CHANGE = " - No Change - ";
	public static final SpecialOutfit BIRTHDAY_SUIT = new SpecialOutfit();

	private SpecialOutfit()
	{
		this.outfitID = Integer.MAX_VALUE;
		this.outfitName = "Birthday Suit";
		this.pieces = new ArrayList();
	}

	public SpecialOutfit( int outfitID, String outfitName )
	{
		this.outfitID = outfitID;
		this.outfitName = outfitName;

		if ( this.outfitName.equals( "Custom: KoLmafia Checkpoint" ) )
			CHECKPOINT = this;
		
		this.pieces = new ArrayList();
	}

	public boolean hasAllPieces()
	{
		for ( int i = 0; i < pieces.size(); ++i )
			if ( !KoLCharacter.hasItem( (AdventureResult) pieces.get(i), false ) || !EquipmentDatabase.canEquip( ((AdventureResult) pieces.get(i)).getName() ) )
				return false;

		return true;
	}

	public boolean isWearing()
	{
		for ( int i = 0; i < pieces.size(); ++i )
			if ( !KoLCharacter.hasEquipped( (AdventureResult) pieces.get(i) ) )
				return false;

		return true;
	}

	public String [] getPieces()
	{
		ArrayList piecesList = new ArrayList();
		for ( int i = 0; i < pieces.size(); ++i )
			piecesList.add( ((AdventureResult) pieces.get(i)).getName() );

		String [] piecesArray = new String[ piecesList.size() ];
		piecesList.toArray( piecesArray );
		return piecesArray;
	}


	public void addPiece( AdventureResult piece )
	{	this.pieces.add( piece );
	}

	public String toString()
	{	return outfitName;
	}

	public int getOutfitID()
	{	return outfitID;
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof SpecialOutfit && outfitID == ((SpecialOutfit)o).outfitID;
	}

	public int compareTo( Object o )
	{
		if ( o == null || !(o instanceof SpecialOutfit) )
			return -1;

		return outfitName.compareToIgnoreCase( ((SpecialOutfit)o).outfitName );
	}
	
	/**
	 * Creates a checkpoint.  This should be called whenever
	 * the player needs an outfit marked to revert to.
	 */

	public static void createCheckpoint()
	{	(new EquipmentRequest( StaticEntity.getClient(), "KoLmafia Checkpoint" )).run();
	}
	
	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */
	
	public static void restoreCheckpoint()
	{
		if ( CHECKPOINT != null )
		{
			(new EquipmentRequest( StaticEntity.getClient(), CHECKPOINT )).run();
			SpecialOutfit.deleteCheckpoint();
		}
	}
	
	/**
	 * Deletes the checkpoint outfit, if present.  This should
	 * be called whenever KoLmafia is done using the checkpoint.
	 */

	public static void deleteCheckpoint()
	{
		if ( CHECKPOINT != null )
		{
			(new KoLRequest( StaticEntity.getClient(), "account_manageoutfits.php?action=Yep.&delete" +
				(0 - CHECKPOINT.getOutfitID()) + "=on" )).run();

			CHECKPOINT = null;
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
		Matcher singleOutfitMatcher = Pattern.compile(
			"<option value=(.*?)>(.*?)</option>" ).matcher( selectHTML );

		int outfitID;
		CHECKPOINT = null;
		SortedListModel outfits = new SortedListModel();

		while ( singleOutfitMatcher.find() )
		{
			outfitID = Integer.parseInt( singleOutfitMatcher.group(1) );
			if ( outfitID < 0 )
				outfits.add( new SpecialOutfit( outfitID, singleOutfitMatcher.group(2) ) );
		}
		
		return outfits;
	}
}
