/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CustomOutfitRequest
	extends GenericRequest
{
	private static final Pattern LIST_PATTERN = Pattern.compile( "<form name=manageoutfits.*?</form>" );
	private static final Pattern ENTRY_PATTERN =
		Pattern.compile( "name=name([\\d]+) value=\"([^\"]*)\".*?<center><b>Contents:</b></cente[rR]>(.*?)</td>" );

	public CustomOutfitRequest()
	{
		super( "account_manageoutfits.php" );
		// action=Yep.
		// name4982=Backup
		// delete4982=on
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void processResults()
	{
		CustomOutfitRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		SortedListModel outfits = new SortedListModel();
		SpecialOutfit.clearImplicitOutfit();

		Matcher listMatcher = CustomOutfitRequest.LIST_PATTERN.matcher( responseText );
		if ( !listMatcher.find() )
		{
			EquipmentManager.setOutfits( outfits );
			return;
		}

		Matcher entryMatcher = CustomOutfitRequest.ENTRY_PATTERN.matcher( listMatcher.group() );
		while ( entryMatcher.find() )
		{
			int outfitId = StringUtilities.parseInt( entryMatcher.group(1) );
			String outfitName = entryMatcher.group(2);

			SpecialOutfit outfit = new SpecialOutfit( -outfitId, outfitName );

			String[] outfitPieces = entryMatcher.group(3).split( "<br>" );
			for ( int i = 0; i < outfitPieces.length; ++i )
			{
				String pieceName = outfitPieces[ i ];
				if ( pieceName.equals( "" ) )
				{
					continue;
				}
				AdventureResult piece = new AdventureResult( pieceName, 1, false );
				outfit.addPiece( piece );
			}

			outfits.add( outfit );
		}

		EquipmentManager.setOutfits( outfits );
	}
}
