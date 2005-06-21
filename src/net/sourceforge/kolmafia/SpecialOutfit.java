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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An encapsulation of a special outfit.  This includes
 * custom outfits as well as standard in-game outfits.
 */

public class SpecialOutfit
{
	private int outfitID;
	private String outfitName;

	public static final SpecialOutfit BIRTHDAY_SUIT = new SpecialOutfit();

	private SpecialOutfit()
	{
		this.outfitID = Integer.MAX_VALUE;
		this.outfitName = "Birthday Suit";
	}

	/**
	 * Constructs a new <code>SpecialOutfit</code> from the given
	 * HTML.  The HTML should include the option value as well
	 * as the name, enclosed within <code><option></option></code>
	 * tags, as they are displayed on the equipment page.
	 */

	private SpecialOutfit( String optionHTML )
	{
		StringTokenizer parsedOutfit = new StringTokenizer( optionHTML, "<>=" );
		parsedOutfit.nextToken();

		this.outfitID = Integer.parseInt( parsedOutfit.nextToken() );
		this.outfitName = parsedOutfit.nextToken();
	}

	public String toString()
	{	return outfitName;
	}

	public int getOutfitID()
	{	return outfitID;
	}

	/**
	 * Static method used to determine all of the available outfits,
	 * based on the given HTML enclosed in <code><select></code> tags.
	 *
	 * @return	A list of available outfits
	 */

	public static LockableListModel parseOutfits( String selectHTML )
	{
		Matcher singleOutfitMatcher = Pattern.compile(
			"<option value=.*?>.*?</option>" ).matcher( selectHTML );
		int lastFindIndex = 0;

		LockableListModel outfits = new LockableListModel();

		while ( singleOutfitMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = singleOutfitMatcher.end();
			outfits.add( new SpecialOutfit( singleOutfitMatcher.group() ) );
		}

		// The first outfit is always "select an outfit"; to make
		// things easier, just remove it from the list.

		if ( !outfits.isEmpty() )
			outfits.remove(0);

		return outfits;
	}
}