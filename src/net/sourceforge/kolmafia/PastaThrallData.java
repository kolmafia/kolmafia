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

package net.sourceforge.kolmafia;

import java.awt.Component;

import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PastaThrallData
	implements Comparable<PastaThrallData>
{
	public static final Object[][] PASTA_THRALLS =
	{
		// Thrall type
		// Thrall ID
		// Skill to bind
		// Pattern to find name when first summoned
		// Pattern to find name when subsequently summoned
		// Image file name
		// Tiny image file name

		{
			"Vampieroghi",
			IntegerPool.get( 1 ),
			"Bind Vampieroghi",
			// My name is written in blood across the history of
			// time . . . but you can call me <name>.
			Pattern.compile( "but you can call me ([^.]*)\\." ),
			// You conjure a pieroghi, and there is a hiss as it
			// becomes inflated with <name>'s presence.
			Pattern.compile( "inflated with ([^']*)'s presence" ),
			"vampieroghi.gif",
			"t_vampieroghi.gif",
		},
		{
			"Vermincelli",
			IntegerPool.get( 2 ),
			"Bind Vermincelli",
			// I think little <name> will be the best helper.
			Pattern.compile( "I think little (.*?) will be the best helper\\." ),
			// You summon a tangled mass of noodles. There is a
			// rustling sound as <name> chews his way into the
			// world to occupy his new body.
			Pattern.compile( "rustling sound as (.*?) chews his way into the world" ),
			"vermincelli.gif",
			"t_vermincelli.gif",
		},
		{
			"Angel Hair Wisp",
			IntegerPool.get( 3 ),
			"Bind Angel Hair Wisp",
			// "You must call me <name>. You must give me form. I
			// must live."
			Pattern.compile( "You must call me ([^.]*])\\." ),
			// You concentrate, and summon a mass of writhing angel
			// hair. A chill perm eates the air as <name>'s spirit
			// enters it. "I live..."
			Pattern.compile( "A chill perm ?eates the air as (.*?)'s spirit enters it\\." ),
			"angelwisp.gif",
			"t_angelwisp.gif",
		},
		{
			"Undead Elbow Macaroni",
			IntegerPool.get( 4 ),
			"Bind Undead Elbow Macaroni",
			// "<name>. My name is <name>."
			Pattern.compile( "My name is ([^.]*)\\." ),
			// You focus your thoughts and call out to <name>. He
			// claws his way up from beneath the ground at your
			// feet.
			Pattern.compile( "You focus your thoughts and call out to (.*?)\\." ),
			"macaroni.gif",
			"t_macaroni.gif",
		},
		{
			"Penne Dreadful",
			IntegerPool.get( 5 ),
			"Bind Penne Dreadful",
			// "All right, palookah," the private eye says, opening
			// his mouth for the first time, "the name's
			// <name>. I'm a gumshoe. You know, a shamus, a
			// flatfoot, a sleuth.
			Pattern.compile( "the name's ([^.]*)\\." ),
			// You calm your mind, and imagine a skeletal assembly
			// of penne. A lone saxophone breaks the night's
			// stillness as it appears and <name> possesses it.
			Pattern.compile( "it appears and (.*?) possesses it" ),
			"pennedreadful.gif",
			"t_pennedreadful.gif",
		},
		{
			"Lasagmbie",
			IntegerPool.get( 6 ),
			"Bind Lasagmbie",
			// Okay. See you on the other side, <name>.
			Pattern.compile( "See you on the other side, (.*?)\\." ),
			// You conjure up a good-sized sheet of lasagna, and
			// there is a wet thud as <name>'s spirit lands in it.
			Pattern.compile( "a wet thud as ([^']*)'s spirit lands in it" ),
			"lasagmbie.gif",
			"t_lasagmbie.gif",
		},
		{
			"Spice Ghost",
			IntegerPool.get( 7 ),
			"Bind Spice Ghost",
			// My name is <name>, and I am in your debt.
			Pattern.compile( "My name is ([^,]*), and I am in your debt\\." ),
			// You conjure up a swirling cloud of spicy dried
			// couscous, and there is a crackle of psychokinetic
			// energy as <name> possesses it.
			Pattern.compile( "crackle of psychokinetic energy as (.*?) possesses it\\." ),
			"spiceghost.gif",
			"t_spiceghost.gif",
		},
		{
			"Spaghetti Elemental",
			IntegerPool.get( 8 ),
			"Bind Spaghetti Elemental",
			// "I guess you need a name, huh?" you reply. "I'll
			// call you... um... SshoKodo. That'll do."
			Pattern.compile( "I'll call you... *um... *([^.]*). * That'll do." ),
			// You close your eyes and reach out across the border
			// between worlds, to the Elemental Plane of
			// Spaghetti. Soon you feel a familiar presence, and
			// pull SshoKodo into the material world.
			Pattern.compile( "and pull (.*?) into the material world\\." ),
			new String[] {
				"spagelem1.gif",
				"spagelem2.gif",
			},
			"t_spagdemon.gif"
		},

		/*
		{
			"Boba Fettucini",
			IntegerPool.get( ItemPool.TWITCHING_TRIGGER_FINGER ),
			// You decide to name it <name>.
			Pattern.compile( "You decide to name it ([^.]*)\\." ),
			// <i>pew pew pew!&quot;</i> <name> shouts excitedly,
			// drawing a laser pistol from some spiritual dimension
			// of necessity.
			Pattern.compile( "</i> (.*) shouts excitedly" ),
			"bobafett.gif",
		},
		{
			"Bow Tie Bat",
			IntegerPool.get( ItemPool.SMOKING_TALON ),
			// Ugh. I'll take that guano as a yes? You'll need a
			// name. Let's call you..." You glance around, hoping
			// for some inspiration. "How about... <name>."
			Pattern.compile( "How about... ([^.]*)\\." ),
			// You call out to <name> with your mind, summoning his
			// spirit even as you create a body for him. He lets
			// loose an ear-splitting screech.
			Pattern.compile( "You call out to (.*?) with your mind" ),
			"bowtiebat.gif",
		},
		*/
	};

	public static String dataToType( Object[] data )
	{
		return data == null ? "" : (String)data[ 0 ];
	}

	public static int dataToId( Object[] data )
	{
		return data == null ? 0 : ((Integer)data[ 1 ]).intValue();
	}

	public static Object[] idToData( final int id )
	{
		for ( int i = 0; i < PastaThrallData.PASTA_THRALLS.length; ++i )
		{
			Object[] data = PastaThrallData.PASTA_THRALLS[ i ];
			if ( PastaThrallData.dataToId( data ) == id )
			{
				return data;
			}
		}
		return null;
	}

	public static String idToType( final int id )
	{
		return PastaThrallData.dataToType( PastaThrallData.idToData( id ) );
	}

	public static final PastaThrallData NO_THRALL = new PastaThrallData( 0 );

	private final int id;
	private final String type;
	private String name;
	private int experience;
	private int level;

	public PastaThrallData( final int id )
	{
		this( id, "", 1 );
	}

	public PastaThrallData( final int id, final String name, final int level )
	{
		this.id = id;
		this.type = id == 0 ? "(none)" : PastaThrallData.idToType( id );
		this.name = name;
		this.level = level;
	}

	public int getId()
	{
		return this.id;
	}

	public String getType()
	{
		return this.type;
	}

	public String getName()
	{
		return this.name;
	}

	public final void setName( final String name )
	{
		this.name = name;
	}

	public int getExperience()
	{
		return this.experience;
	}

	public void setExperience( final int experience )
	{
		this.experience = experience;
	}

	public int getLevel()
	{
		return this.level;
	}

	public void setLevel( final int level )
	{
		this.level = level;
	}

	@Override
	public String toString()
	{
		return this.id == -1 ? "(none)" : this.type + " (lvl. " + this.getLevel() + ")";
	}

	@Override
	public boolean equals( final Object o )
	{
		return o != null && o instanceof PastaThrallData && this.id == ( (PastaThrallData) o ).id;
	}

	@Override
	public int hashCode()
	{
		return this.id;
	}

	public int compareTo( final PastaThrallData td )
	{
		return this.type.compareToIgnoreCase( td.type );
	}
}
