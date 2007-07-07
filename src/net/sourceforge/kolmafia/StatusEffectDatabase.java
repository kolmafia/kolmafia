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

import java.io.BufferedReader;
import java.io.File;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusEffectDatabase extends KoLDatabase
{
	private static Map nameById = new TreeMap();
	private static Map dataNameById = new TreeMap();
	private static Map effectByName = new TreeMap();

	private static Map imageById = new TreeMap();
	private static Map descriptionById = new TreeMap();
	private static Map effectByDescription = new TreeMap();

	private static Map modifierMap = new TreeMap();

	static
	{
		BufferedReader reader = getReader( "statuseffects.txt" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
				addToDatabase( Integer.valueOf( data[0] ), data[1], data[2], data.length > 3 ? data[3] : null );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}

		reader = getReader( "modifiers.txt" );
		while ( (data = readData( reader )) != null )
			if ( data.length == 2 )
				modifierMap.put( data[0].toLowerCase(), data[1] );

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	private static void addToDatabase( Integer effectId, String name, String image, String descriptionId )
	{
		nameById.put( effectId, getDisplayName( name ) );
		dataNameById.put( effectId, name );
		effectByName.put( getCanonicalName( name ), effectId );
		imageById.put( effectId, image );

		if ( descriptionId == null )
			return;

		descriptionById.put( effectId, descriptionId );
		effectByDescription.put( descriptionId, effectId );
	}

	/**
	 * Returns the name for an effect, given its Id.
	 * @param	effectId	The Id of the effect to lookup
	 * @return	The name of the corresponding effect
	 */

	public static final String getEffectName( int effectId )
	{	return effectId == -1 ? "Unknown effect" : getDisplayName( (String) nameById.get( new Integer( effectId ) ) );
	}

	public static final int getEffect( String descriptionId )
	{
		Object effectId = effectByDescription.get( descriptionId );
		return effectId == null ? -1 : ((Integer)effectId).intValue();
	}

	public static final String getDescriptionId( int effectId )
	{	return (String) descriptionById.get( new Integer( effectId ) );
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 * @param	effectName	The name of the effect to lookup
	 * @return	The Id number of the corresponding effect
	 */

	public static final int getEffectId( String effectName )
	{
		Object effectId = effectByName.get( getCanonicalName( effectName ) );
		return effectId == null ? -1 : ((Integer)effectId).intValue();
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 * @param	effectId	The Id of the effect to lookup
	 * @return	The name of the corresponding effect
	 */

	public static final String getImage( int effectId )
	{
		Object imageName = effectId == -1 ? null : imageById.get( new Integer( effectId ) );
		return imageName == null ? "/images/debug.gif" : "http://images.kingdomofloathing.com/itemimages/" + imageName;
	}

	/**
	 * Returns the set of status effects keyed by Id
	 * @return	The set of status effects keyed by Id
	 */

	public static Set entrySet()
	{	return nameById.entrySet();
	}

	public static Collection values()
	{	return nameById.values();
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @param	effectName	The name of the effect to lookup
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String effectName )
	{	return effectByName.containsKey( getCanonicalName( effectName ) );
	}

	/**
	 * Returns a list of all items which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on items.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( effectByName, substring );
	}

	public static final int FAMILIAR_WEIGHT_MODIFIER = 0;
	public static final int MONSTER_LEVEL_MODIFIER = 1;
	public static final int COMBAT_RATE_MODIFIER = 2;
	public static final int INITIATIVE_MODIFIER = 3;
	public static final int EXPERIENCE_MODIFIER = 4;
	public static final int ITEMDROP_MODIFIER = 5;
	public static final int MEATDROP_MODIFIER = 6;
	public static final int DAMAGE_ABSORPTION_MODIFIER = 7;
	public static final int DAMAGE_REDUCTION_MODIFIER = 8;
	public static final int COLD_RESISTANCE_MODIFIER = 9;
	public static final int HOT_RESISTANCE_MODIFIER = 10;
	public static final int SLEAZE_RESISTANCE_MODIFIER = 11;
	public static final int SPOOKY_RESISTANCE_MODIFIER = 12;
	public static final int STENCH_RESISTANCE_MODIFIER = 13;
	public static final int MANA_COST_MODIFIER = 14;

	private static final Pattern [] MODIFIER_PATTERNS = new Pattern [] {
		Pattern.compile( "Weight: ([+-]\\d+)" ),
		Pattern.compile( "ML: ([+-]\\d+)" ),
		Pattern.compile( "Combat: ([+-][\\d.]+)" ),
		Pattern.compile( "Init: ([+-][\\d.]+)" ),
		Pattern.compile( "Exp: ([+-][\\d.]+)" ),
		Pattern.compile( "Item: ([+-][\\d.]+)" ),
		Pattern.compile( "Meat: ([+-][\\d.]+)" ),
		Pattern.compile( "DA: ([+-]\\d+)" ),
		Pattern.compile( "DR: (\\d+)" ),
		Pattern.compile( "Cold: ([+-]\\d+)" ),
		Pattern.compile( "Hot: ([+-]\\d+)" ),
		Pattern.compile( "Sleaze: ([+-]\\d+)" ),
		Pattern.compile( "Spooky: ([+-]\\d+)" ),
		Pattern.compile( "Stench: ([+-]\\d+)" ),
		Pattern.compile( "Mana: ([+-]\\d+)" ),
	};

	private static final float [] NO_MODIFIERS = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };

	public static final float [] getModifiers( String name )
	{
		if ( name == null )
			return NO_MODIFIERS;

		String modifier = (String) modifierMap.get( name.toLowerCase() );
		if ( modifier == null )
			return NO_MODIFIERS;

		float [] modifiers = new float[ MODIFIER_PATTERNS.length ];
		for ( int i = 0; i < modifiers.length; ++i )
		{
			Matcher effectMatcher = MODIFIER_PATTERNS[ i ].matcher( modifier );
			modifiers[i] = effectMatcher.find() ? Float.parseFloat( effectMatcher.group(1) ) : 0.0f;
		}

		return modifiers;
	}

	public static void addDescriptionId( int effectId, String descriptionId )
	{
		if ( effectId == -1 )
			return;

		Integer id = new Integer( effectId );

		effectByDescription.put( descriptionId, id );
		descriptionById.put( id, descriptionId );

		saveDataOverride();
	}

	private static final Pattern STATUS_EFFECT_PATTERN = Pattern.compile(
		"<input type=radio name=whicheffect value=(\\d+)></td><td><img src=\"http://images.kingdomofloathing.com/itemimages/(.*?)\" width=30 height=30></td><td>(.*?) \\(" );

	public static void findStatusEffects()
	{
		if ( !inventory.contains( UneffectRequest.REMEDY ) )
			return;

		RequestLogger.printLine( "Checking for new status effects..." );

		KoLRequest updateRequest = new KoLRequest( "uneffect.php" );
		RequestThread.postRequest( updateRequest );
		RequestThread.postRequest( CharpaneRequest.getInstance() );

		Matcher effectsMatcher = STATUS_EFFECT_PATTERN.matcher( updateRequest.responseText );

		boolean foundChanges = false;

		while ( effectsMatcher.find() )
		{
			Integer effectId = Integer.valueOf( effectsMatcher.group(1) );
			if ( nameById.containsKey( effectId ) )
				continue;

			foundChanges = true;
			addToDatabase( effectId, effectsMatcher.group(3), effectsMatcher.group(2), null );
		}

		if ( foundChanges )
			saveDataOverride();
	}

	public static void saveDataOverride()
	{
		File output = new File( DATA_DIRECTORY, "statuseffects.txt" );
		LogStream writer = LogStream.openStream( output, true );

		int lastInteger = 1;
		Iterator it = dataNameById.keySet().iterator();

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
					writer.println(i);

			lastInteger = nextInteger.intValue() + 1;
			writer.print( nextInteger + "\t" + dataNameById.get( nextInteger ) + "\t" +
				imageById.get( nextInteger ) );

			if ( descriptionById.containsKey( nextInteger ) )
				writer.print( "\t" + descriptionById.get( nextInteger ) );

			writer.println();
		}

		writer.close();
	}
}
