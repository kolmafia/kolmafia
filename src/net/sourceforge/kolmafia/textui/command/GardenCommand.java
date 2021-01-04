/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropType;

import net.sourceforge.kolmafia.session.Limitmode;

public class GardenCommand
	extends AbstractCommand
{
	public GardenCommand()
	{
		this.usage = " [pick] - get status of garden, or harvest it.";
	}

	private boolean checkMushroomGarden( CropType cropType )
	{
		if ( cropType != CropType.MUSHROOM )
		{
			KoLmafia.updateDisplay( "You don't have a mushroom garden." );
			return false;
		}
		if ( Preferences.getBoolean( "_mushroomGardenVisited" ) )
		{
			KoLmafia.updateDisplay( "You've already dealt with your mushroom garden today." );
			return false;
		}
		if ( KoLCharacter.isFallingDown() )
		{
			KoLmafia.updateDisplay( "You are too drunk to enter your mushroom garden." );
			return false;
		}
		if ( KoLCharacter.getAdventuresLeft() <= 0 )
		{
			KoLmafia.updateDisplay( "You need an available turn to fight through piranha plants." );
			return false;
		}
		return true;
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( KoLCharacter.isEd() || KoLCharacter.inNuclearAutumn() || Limitmode.limitCampground() )
		{
			KoLmafia.updateDisplay( "You can't get to your campground to visit your garden." );
			return;
		}

		AdventureResult crop = CampgroundRequest.getCrop();
		CropType cropType = CampgroundRequest.getCropType( crop );
		
		if ( crop == null )
		{
			KoLmafia.updateDisplay( "You don't have a garden." );
			return;
		}

		if ( parameters.equals( "" ) )
		{
			int count = crop.getPluralCount();
			String name = crop.getPluralName();
			String gardenType = cropType.toString();
			KoLmafia.updateDisplay( "Your " + gardenType + " garden has " + count + " " + name + " in it." );
			return;
		}

		if ( parameters.equals( "fertilize" ) )
		{
			// Mushroom garden only
			if ( checkMushroomGarden( cropType ) )
			{
				CampgroundRequest.harvestMushrooms( false );
			}
			return;
		}

		if ( parameters.equals( "pick" ) )
		{
			// Mushroom garden only
			if ( cropType == CropType.MUSHROOM && checkMushroomGarden( cropType ) )
			{
				CampgroundRequest.harvestMushrooms( true );
				return;
			}

			int count = crop.getCount();
			if ( count == 0 )
			{
				KoLmafia.updateDisplay( "There is nothing ready to pick in your garden." );
				return;
			}

			CampgroundRequest.harvestCrop();
			return;
		}
	}
}
