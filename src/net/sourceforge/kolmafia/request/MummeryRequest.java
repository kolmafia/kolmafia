/**
 * Copyright (c) 2005-2017, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.preferences.Preferences;

public class MummeryRequest
	extends GenericRequest
{
	public MummeryRequest()
	{
		super( "choice.php" );
	}

	public static final void parseResponse( final int choice, final String responseText )
	{
		if ( !responseText.contains( "You dress" ) )
		{
			// We shouldn't be here
			return;
		}

		if ( KoLCharacter.currentFamiliar == null )
		{
			// This probably can't happen, but just in case...
			return;
		}

		String mods = Preferences.getString( "_mummeryMods" );
		String familiar = KoLCharacter.currentFamiliar.getRace();
		switch ( choice )
		{
		case 1:
			mods += "Meat Drop: [15*fam(" + familiar + ")],";
			break;
		case 2:
			mods += "MP Regen Min: [4*fam(" + familiar + ")], MP Regen Max: [5*fam(" + familiar + ")],";
			break;
		case 3:
			mods += "Experience (Muscle): [3*fam(" + familiar + ")],";
			break;
		case 4:
			mods += "Item Drop: [15*fam(" + familiar + ")],";
			break;
		case 5:
			mods += "Experience (Mysticality): [3*fam(" + familiar + ")],";
			break;
		case 6:
			mods += "HP Regen Min: [8*fam(" + familiar + ")], HP Regen Max: [10*fam(" + familiar + ")],";
			break;
		case 7:
			mods += "Experience (Moxie): [2*fam(" + familiar + ")],";
			break;

		}
		Preferences.setString( "_mummeryUses", Preferences.getString( "_mummeryUses" ) + choice + "," );
		Preferences.setString( "_mummeryMods", mods );

		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
	}
}
