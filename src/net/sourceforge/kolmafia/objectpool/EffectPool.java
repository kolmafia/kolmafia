/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.objectpool;

import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;

public class EffectPool
{
	private static final TreeMap effectCache = new TreeMap();

	public static final String ASTRAL_SHELL = "Astral Shell";
	public static final String ELEMENTAL_SPHERE = "Elemental Saucesphere";
	public static final String EXPERT_OILINESS = "Expert Oiliness";
	public static final String GHOSTLY_SHELL = "Ghostly Shell";
	public static final String ASTRAL = "Half-Astral";
	public static final String PERFUME = "Knob Goblin Perfume";
	public static final String ONTHETRAIL = "On the Trail";
	public static final String SLIPPERY_OILINESS = "Slippery Oiliness";
	public static final String STABILIZING_OILINESS = "Stabilizing Oiliness";
	public static final String HYDRATED = "Ultrahydrated";

	public static final AdventureResult get( String effectName )
	{
		if ( effectCache.containsKey( effectName ) )
		{
			return (AdventureResult) effectCache.get( effectName );
		}

		AdventureResult effect = new AdventureResult( effectName, 1, true );
		effectCache.put( effectName, effect );
		return effect;
	}
}
