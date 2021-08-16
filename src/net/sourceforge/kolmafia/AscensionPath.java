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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.util.HashMap;
import java.util.Map;

public class AscensionPath
{
	public enum Path
	{
		// Path Name, Path ID, is Avatar?, image in ascension history, article
		NONE( "None", 0, false, "blank.gif", null ),
		BOOZETAFARIAN( "Boozetafarian", 1, false,  "martini", "a" ),
		TEETOTALER( "Teetotaler", 2, false, "bowl", "a" ),
		OXYGENARIAN( "Oxygenarian", 3, false, "oxy", "an" ),
		BEES_HATE_YOU( "Bees Hate You", 4, false, "beeicon", "a" ),
		SURPRISING_FIST( "Way of the Surprising Fist", 6, false,  "wasp_fist", "a" ),
		TRENDY( "Trendy", 7, false,  "trendyicon", "a" ),
		AVATAR_OF_BORIS( "Avatar of Boris", 8, true, "trusty", "an" ),
		BUGBEAR_INVASION( "Bugbear Invasion", 9, false, "familiar39", "a" ),
		ZOMBIE_SLAYER( "Zombie Slayer", 10, true, "tombstone", "a" ),
		CLASS_ACT( "Class Act", 11, false,  "motorboat", "a" ),
		AVATAR_OF_JARLSBERG( "Avatar of Jarlsberg", 12, true, "jarlhat", "an" ),
		BIG( "BIG!", 14, false, "bigicon", "a" ),
		KOLHS( "KOLHS", 15, false, "kolhsicon", "a" ),
		CLASS_ACT_II( "Class Act II: A Class For Pigs", 16, false, "motorboat2", "a" ),
		AVATAR_OF_SNEAKY_PETE( "Avatar of Sneaky Pete", 17, true, "bigglasses", "an" ),
		SLOW_AND_STEADY( "Slow and Steady", 18, false, "sas", "a" ),
		HEAVY_RAINS( "Heavy Rains", 19, false, "familiar31", "a" ),
		PICKY( "Picky", 21, false, "pickypath", "a" ),
		STANDARD( "Standard", 22, false, "standardicon", "the" ),
		ACTUALLY_ED_THE_UNDYING( "Actually Ed the Undying", 23, true, "scarab", "an" ),
		CRAZY_RANDOM_SUMMER( "One Crazy Random Summer", 24, false, "dice", "the" ),
		COMMUNITY_SERVICE( "Community Service", 25, false, "csplaquesmall", "a" ),
		AVATAR_OF_WEST_OF_LOATHING( "Avatar of West of Loathing", 26,  false, "badge", "an" ),
		THE_SOURCE( "The Source", 27, false, "ss_datasiphon", "a" ),
		NUCLEAR_AUTUMN( "Nuclear Autumn", 28, false, "radiation", "a" ),
		GELATINOUS_NOOB( "Gelatinous Noob", 29, true, "gcube", "a" ),
		LICENSE_TO_ADVENTURE( "License to Adventure", 30, false, "briefcase", "a" ),
		LIVE_ASCEND_REPEAT( "Live. Ascend. Repeat.", 31, false, "watch", "a" ),
		POKEFAM( "Pocket Familiars", 32, false, "spiritorb", "a" ),
		GLOVER( "G-Lover", 33,false,  "g-loveheart", "a" ),
		DISGUISES_DELIMIT( "Disguises Delimit", 34, false, "dd_icon", "a" ),
		DARK_GYFFTE( "Dark Gyffte", 35, true, "darkgift", "a" ),
		CRAZY_RANDOM_SUMMER_TWO( "Two Crazy Random Summer", 36, false, "twocrazydice", "a" ),
		KINGDOM_OF_EXPLOATHING( "Kingdom of Exploathing", 37, false, "puff", "a" ),
		PATH_OF_THE_PLUMBER( "Path of the Plumber", 38, true, "mario_mushroom1", "a" ),
		LOWKEY( "Low Key Summer", 39, false, "littlelock", "a" ),
		GREY_GOO( "Grey Goo", 40, false, "greygooball", "a" ),
		YOU_ROBOT( "You, Robot", 41, false, "yourobot", "a" ),
		// Not yet implemented
		QUANTUM( "Quantum Terrarium", 42, false, "quantum", "a" ),
		WILDFIRE( "Wildfire", 43, false, "brushfire", "a" ),
		// A "sign" rather than a "path" for some reason
		BAD_MOON( "Bad Moon", 999, false, "badmoon", null ),
		;

		final public String name;
		final public int id;
		final public boolean isAvatar;
		final public String image;
		final public String article;

		Path( String name, int id, boolean isAvatar, String image, String article )
		{
			this.name = name;
			this.id = id;
			this.isAvatar = isAvatar;
			this.image = image + ".gif";
			this.article = article;
		}

		public String getName()
		{
			return this.name;
		}

		public int getId()
		{
			return this.id;
		}

		public boolean isAvatar()
		{
			return this.isAvatar;
		}

		public String getImage()
		{
			return this.image;
		}

		public String description()
		{
			String description =
				( this == Path.NONE ) ?
				"no path" :
				( this.article == null ) ?
				this.name :
				( this.article + " " + this.name + " path" );
			return description;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	private static final Map<String, Path> pathByName = new HashMap<>();
	private static final Map<Integer, Path> pathById = new HashMap<>();
	private static final Map<String, Path> pathByImage = new HashMap<>();

	static
	{
		for ( Path path : Path.values() )
		{
			pathByName.put( path.name, path );
			pathById.put( path.id, path );
			if ( path.image != null )
			{
				pathByImage.put( path.image, path );
			}
		}
	}

	public static Path nameToPath( String name )
	{
		return pathByName.get( name );
	}

	public static Path idToPath( int id )
	{
		Path retval = pathById.get( id );
		return retval == null ? Path.NONE : retval;
	}

	public static Path imageToPath( String image )
	{
		return pathByImage.get( image );
	}
}
