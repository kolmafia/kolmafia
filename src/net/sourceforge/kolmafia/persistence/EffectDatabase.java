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

package net.sourceforge.kolmafia.persistence;

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

import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;

public class EffectDatabase
	extends KoLDatabase
{
	private static final Map nameById = new TreeMap();
	private static final Map dataNameById = new TreeMap();
	private static final Map effectByName = new TreeMap();
	private static final TreeMap defaultActions = new TreeMap();

	private static final Map imageById = new TreeMap();
	private static final Map descriptionById = new TreeMap();
	private static final Map effectByDescription = new TreeMap();

	static
	{
		BufferedReader reader =
			KoLDatabase.getVersionedReader( "statuseffects.txt", KoLConstants.STATUSEFFECTS_VERSION );
		String[] data;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				EffectDatabase.addToDatabase(
					Integer.valueOf( data[ 0 ] ), data[ 1 ], data[ 2 ], data.length > 3 ? data[ 3 ] : null,
					data.length > 4 ? data[ 4 ] : null );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private static final void addToDatabase( final Integer effectId, final String name, final String image,
		final String descriptionId, final String defaultAction )
	{
		EffectDatabase.nameById.put( effectId, KoLDatabase.getDisplayName( name ) );
		EffectDatabase.dataNameById.put( effectId, name );
		EffectDatabase.effectByName.put( KoLDatabase.getCanonicalName( name ), effectId );
		EffectDatabase.imageById.put( effectId, image );

		if ( descriptionId == null )
		{
			return;
		}

		EffectDatabase.descriptionById.put( effectId, descriptionId );
		EffectDatabase.effectByDescription.put( descriptionId, effectId );

		if ( defaultAction != null )
		{
			EffectDatabase.defaultActions.put( KoLDatabase.getDisplayName( name ), defaultAction );
		}
	}

	public static final String getDefaultAction( final String effectName )
	{
		return KoLDatabase.getDisplayName( (String) EffectDatabase.defaultActions.get( effectName ) );
	}

	/**
	 * Returns the name for an effect, given its Id.
	 *
	 * @param effectId The Id of the effect to lookup
	 * @return The name of the corresponding effect
	 */

	public static final String getEffectName( final int effectId )
	{
		return effectId == -1 ? "Unknown effect" : KoLDatabase.getDisplayName( (String) EffectDatabase.nameById.get( new Integer(
			effectId ) ) );
	}

	public static final String getEffectName( final String descriptionId )
	{
		Object effectId = EffectDatabase.effectByDescription.get( descriptionId );
		return effectId == null ? null : EffectDatabase.getEffectName( ( (Integer) effectId ).intValue() );
	}

	public static final int getEffect( final String descriptionId )
	{
		Object effectId = EffectDatabase.effectByDescription.get( descriptionId );
		return effectId == null ? -1 : ( (Integer) effectId ).intValue();
	}

	public static final String getDescriptionId( final int effectId )
	{
		return (String) EffectDatabase.descriptionById.get( new Integer( effectId ) );
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 *
	 * @param effectName The name of the effect to lookup
	 * @return The Id number of the corresponding effect
	 */

	public static final int getEffectId( final String effectName )
	{
		Object effectId = EffectDatabase.effectByName.get( KoLDatabase.getCanonicalName( effectName ) );
		if ( effectId != null )
		{
			return ( (Integer) effectId ).intValue();
		}

		List names = EffectDatabase.getMatchingNames( effectName );
		if ( names.size() == 1 )
		{
			return EffectDatabase.getEffectId( (String) names.get( 0 ) );
		}

		return -1;
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 *
	 * @param effectId The Id of the effect to lookup
	 * @return The name of the corresponding effect
	 */

	public static final String getImage( final int effectId )
	{
		Object imageName = effectId == -1 ? null : EffectDatabase.imageById.get( new Integer( effectId ) );
		return imageName == null ? "/images/debug.gif" : "http://images.kingdomofloathing.com/itemimages/" + imageName;
	}

	/**
	 * Returns the set of status effects keyed by Id
	 *
	 * @return The set of status effects keyed by Id
	 */

	public static final Set entrySet()
	{
		return EffectDatabase.nameById.entrySet();
	}

	public static final Collection values()
	{
		return EffectDatabase.nameById.values();
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @param effectName The name of the effect to lookup
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String effectName )
	{
		return EffectDatabase.effectByName.containsKey( KoLDatabase.getCanonicalName( effectName ) );
	}

	/**
	 * Returns a list of all items which contain the given substring. This is useful for people who are doing lookups on
	 * items.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return KoLDatabase.getMatchingNames( EffectDatabase.effectByName, substring );
	}

	public static final void addDescriptionId( final int effectId, final String descriptionId )
	{
		if ( effectId == -1 )
		{
			return;
		}

		Integer id = new Integer( effectId );

		EffectDatabase.effectByDescription.put( descriptionId, id );
		EffectDatabase.descriptionById.put( id, descriptionId );

		EffectDatabase.saveDataOverride();
	}

	private static final Pattern STATUS_EFFECT_PATTERN =
		Pattern.compile( "<input type=radio name=whicheffect value=(\\d+)></td><td><img src=\"http://images.kingdomofloathing.com/itemimages/(.*?)\" width=30 height=30></td><td>(.*?) \\(" );

	public static final void findStatusEffects()
	{
		if ( !KoLConstants.inventory.contains( UneffectRequest.REMEDY ) )
		{
			return;
		}

		GenericRequest effectChecker = new GenericRequest( "uneffect.php" );
		RequestLogger.printLine( "Checking for new status effects..." );
		RequestThread.postRequest( effectChecker );

		Matcher effectsMatcher = EffectDatabase.STATUS_EFFECT_PATTERN.matcher( effectChecker.responseText );
		boolean foundChanges = false;

		while ( effectsMatcher.find() )
		{
			Integer effectId = Integer.valueOf( effectsMatcher.group( 1 ) );
			if ( EffectDatabase.nameById.containsKey( effectId ) )
			{
				continue;
			}

			foundChanges = true;
			EffectDatabase.addToDatabase(
				effectId, effectsMatcher.group( 3 ), effectsMatcher.group( 2 ), null, null );
		}

		RequestThread.postRequest( CharPaneRequest.getInstance() );

		if ( foundChanges )
		{
			EffectDatabase.saveDataOverride();
		}
	}

	public static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "statuseffects.txt" );
		LogStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.STATUSEFFECTS_VERSION );

		int lastInteger = 1;
		String defaultAction;
		Iterator it = EffectDatabase.dataNameById.keySet().iterator();

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
			{
				writer.println( i );
			}

			lastInteger = nextInteger.intValue() + 1;
			writer.print( nextInteger + "\t" + EffectDatabase.dataNameById.get( nextInteger ) + "\t" + EffectDatabase.imageById.get( nextInteger ) );

			if ( EffectDatabase.descriptionById.containsKey( nextInteger ) )
			{
				writer.print( "\t" + EffectDatabase.descriptionById.get( nextInteger ) );

				defaultAction =
					(String) EffectDatabase.defaultActions.get( EffectDatabase.dataNameById.get( nextInteger ) );
				if ( defaultAction != null && !defaultAction.equals( "" ) )
				{
					writer.print( "\t" + defaultAction );
				}
			}

			writer.println();
		}

		writer.close();
	}
}
