/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FloristRequest
	extends GenericRequest
{
	private static boolean haveFlorist = true;

	private static final Pattern FLOWER_PATTERN =
		Pattern.compile( "<tr><td>([^>]*?)</td><td width.*?plant(\\d+)\\.gif.*?plant(\\d+)?\\.gif.*?plant(\\d+)?\\.gif.*?" );

	private static final Pattern LOCATION_PATTERN = Pattern.compile( "Ah, <b>(.*)</b>!" );
	public static String getLocation( final String responseText )
	{
		Matcher matcher = FloristRequest.LOCATION_PATTERN.matcher( responseText );
		return matcher.find() ? matcher.group( 1 ).toLowerCase() : null;
	}

	private static final Pattern OPTION_PATTERN = Pattern.compile( "option=(\\d+)" );
	public static int getOption( final String urlString )
	{
		Matcher matcher = FloristRequest.OPTION_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	private static final Pattern PLANT_PATTERN = Pattern.compile( "plant=(\\d+)" );
	public static int getPlant( final String urlString )
	{
		Matcher matcher = FloristRequest.PLANT_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	private static final Pattern DIG_PATTERN = Pattern.compile( "plnti=(\\d)" );
	public static int getDigIndex( final String urlString )
	{
		Matcher matcher = FloristRequest.DIG_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : -1;
	}

	public static final Map<String, ArrayList<Florist>> floristPlants = new HashMap<String, ArrayList<Florist>>();

	public enum Florist
	{
		// Outdoor Plants
		RABID_DOGWOOD( 1, "Rabid Dogwood" ),
		RUTABEGGAR( 2, "Rutabeggar" ),
		RADISH( 3, "Rad-ish Radish" ),
		ARTICHOKER( 4, "Artichoker" ),
		SMOKERA( 5, "Smoke-ra" ),
		SKUNK_CABBAGE( 6, "Skunk Cabbage" ),
		DEADLY_CINNAMON( 7, "Deadly Cinnamon" ),
		CELERY_STALKER( 8, "Celery Stalker" ),
		LETTUCE_SPRAY( 9, "Lettuce Spray" ),
		SELTZER_WATERCRESS( 10, "Seltzer Watercress" ),

		// Indoor Plants
		WAR_LILY( 11, "War Lily" ),
		STEALING_MAGNOLIA( 12, "Stealing Magnolia" ),
		CANNED_SPINACH( 13, "Canned Spinach" ),
		IMPATIENS( 14, "Impatiens" ),
		SPIDER_PLANT( 15, "Spider Plant" ),
		RED_FERN( 16, "Red Fern" ),
		BAMBOO( 17, "BamBOO!" ),
		ARCTIC_MOSS( 18, "Arctic Moss" ),
		ALOE_GUVNOR( 19, "Aloe Guv'nor" ),
		PITCHER_PLANT( 20, "Pitcher Plant" ),

		// Underground Plants
		BLUSTERY_PUFFBALL( 21, "Blustery Puffball" ),
		HORN_OF_PLENTY( 22, "Horn of Plenty" ),
		WIZARD_WIG( 23, "Wizard's Wig" ),
		SHUFFLE_TRUFFLE( 24, "Shuffle Truffle" ),
		DIS_LICHEN( 25, "Dis Lichen" ),
		LOOSE_MORELS( 26, "Loose Morels" ),
		FOUL_TOADSTOOL( 27, "Foul Toadstool" ),
		CHILLTERELLE( 28, "Chillterelle" ),
		PORTLYBELLA( 29, "Portlybella" ),
		MAX_HEADSHROOM( 30, "Max Headshroom" ),

		// Underwater Plants
		SPANKTON( 31, "Spankton" ),
		KELPTOMANIAC( 32, "Kelptomaniac" ),
		CROOKWEED( 33, "Crookweed" ),
		ELECTRIC_EELGRASS( 34, "Electric Eelgrass" ),
		DUCKWEED( 35, "Duckweed" ),
		ORCA_ORCHID( 36, "Orca Orchid" ),
		SARGASSUM( 37, "Sargassum" ),
		SUBSEA_ROSE( 38, "Sub-Sea Rose" ),
		SNORI( 39, "Snori" ),
		UPSEA_DAISY( 40, "Up Sea Daisy" ),
		;

		private int id;
		private String name;

		private Florist( int id, String name )
		{
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public int id()
		{
			return this.id;
		}

		public static Florist getFlower( int id )
		{
			if ( id == 0 )
			{
				return null;
			}
			for ( Florist flower : Florist.values() )
			{
				if ( id == flower.id )
				{
					return flower;
				}
			}
			return null;
		}

		public static Florist getFlower( String name )
		{
			if ( name == null )
			{
				return null;
			}
			for ( Florist flower : Florist.values() )
			{
				if ( name.equalsIgnoreCase( flower.name ) )
				{
					return flower;
				}
			}
			return null;
		}

		public boolean isTerritorial()
		{
			return this.id % 10 == 1 || this.id % 10 == 2 || this.id % 10 == 3;
		}
	}

	public static void reset()
	{
		FloristRequest.haveFlorist = true;
		FloristRequest.floristPlants.clear();
	}

	// forestvillage.php?action=floristfriar
	// choice.php?option=4&whichchoice=720&pwd
	public FloristRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "720" );
		this.addFormField( "option", "4" );
	}

	public FloristRequest( int plant )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "720" );
		this.addFormField( "option", "1" );
		this.addFormField( "plant", String.valueOf( plant ) );
	}

	@Override
	public void run()
	{
		if ( !FloristRequest.haveFlorist() )
		{
			return;
		}

		GenericRequest forestVisit = new GenericRequest( "place.php?whichplace=forestvillage&action=fv_friar" );
		RequestThread.postRequest( forestVisit );
		if ( !forestVisit.responseText.contains( "The Florist Friar's Cottage" ) )
		{
			FloristRequest.setHaveFlorist( false );
			return;
		}

		super.run();
	}

	public static boolean haveFlorist()
	{
		return FloristRequest.haveFlorist;
	}

	private static final void setHaveFlorist( final boolean haveFlorist )
	{
		FloristRequest.haveFlorist = haveFlorist;
	}

	public static final ArrayList<Florist> getPlants( String location )
	{
		if ( floristPlants.containsKey( location ) )
		{
			return floristPlants.get( location );
		}
		return null;
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=720" ) )
		{
			return;
		}

		switch ( FloristRequest.getOption( urlString ) )
		{
		case 1:
		{
			int plant = FloristRequest.getPlant( urlString );
			if ( plant == 0 )
			{
				return;
			}

			// The location is already full of plants
			if ( responseText.contains( "You need to dig up a space." ) )
			{
				return;
			}

			// It's the wrong location type for that plant
			if ( responseText.contains( "Invalid plant" ) )
			{
				return;
			}

			String location = FloristRequest.getLocation( responseText );
			if ( location == null )
			{
				// Something went wrong and somehow wasn't caught by the previous cases
				return;
			}
			FloristRequest.addPlant( location, plant );
			return;
		}

		case 2:
			if ( responseText.contains( "You dig up a plant." ) )
			{
				String location = FloristRequest.getLocation( responseText );
				int digIndex = FloristRequest.getDigIndex( urlString );
				FloristRequest.digPlant( location, digIndex );
			}
			return;

		case 4:
			if ( responseText.contains( "The Florist Friar's Cottage" ) )
			{
				FloristRequest.setHaveFlorist( true );
			}
			FloristRequest.floristPlants.clear();
			Matcher matcher = FloristRequest.FLOWER_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				ArrayList<Florist> plantList = new ArrayList<Florist>();
				String location = matcher.group( 1 ).toLowerCase();
				int flower1 = StringUtilities.parseInt( matcher.group( 2 ) );
				int flower2 = StringUtilities.parseInt( matcher.group( 3 ) );
				int flower3 = StringUtilities.parseInt( matcher.group( 4 ) );
				if ( flower1 != 0 )
				{
					plantList.add( Florist.getFlower( flower1 ) );
				}
				if ( flower2 != 0 )
				{
					plantList.add( Florist.getFlower( flower2 ) );
				}
				if ( flower3 != 0 )
				{
					plantList.add( Florist.getFlower( flower3 ) );
				}
				FloristRequest.floristPlants.put( location, plantList );
			}
			return;
		}
	}

	private static final void addPlant( final String location, final int plantId )
	{
		Florist plant = Florist.getFlower( plantId );
		if ( plant.isTerritorial() )
		{
			FloristRequest.clearTerritorial( location );
		}

		ArrayList<Florist> plants = FloristRequest.getPlants( location );

		if ( plants == null )
		{
			plants = new ArrayList<Florist>();
		}
		plants.add( plant );

		FloristRequest.floristPlants.put( location, plants );

		StringBuilder floristUsed = new StringBuilder();
		floristUsed.append( Preferences.getString( "_floristPlantsUsed" ) );
		if ( floristUsed.length() > 0 )
		{
			floristUsed.append( "," );
		}
		floristUsed.append( plant );
		Preferences.setString( "_floristPlantsUsed", floristUsed.toString() );
	}

	private static final void clearTerritorial( final String location )
	{
		ArrayList<Florist> plants = FloristRequest.getPlants( location );
		if ( plants == null )
		{
			return;
		}

		for ( Florist plant : plants )
		{
			if ( plant.isTerritorial() )
			{
				plants.remove( plant );
				FloristRequest.floristPlants.put( location, plants );
				// There can only be 1 territorial plant, so once we find it we are done
				return;
			}
		}
	}

	private static final void digPlant( final String location, final int digIndex )
	{
		ArrayList<Florist> plants = FloristRequest.getPlants( location );
		if ( plants == null )
		{
			return;
		}

		if ( digIndex >= plants.size() )
		{
			// This should only happen when KoL and KoLmafia use different names for a location
			// KoLmafia's tracking will be wrong, but that can't be helped
			return;
		}

		plants.remove( digIndex );
		FloristRequest.floristPlants.put( location, plants );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) || urlString.indexOf( "whichchoice=720" ) == -1 )
		{
			return false;
		}

		int option = FloristRequest.getOption( urlString );
		String message = null;

		switch (option )
		{
		case 1:
		{
			int plantId = FloristRequest.getPlant( urlString );
			if ( plantId > 0 )
			{
				Florist plant = Florist.getFlower( plantId );
				message = "Planting a " + plant;
			}
			break;
		}
		case 2:
		{
			int digIndex = FloristRequest.getDigIndex( urlString );
			if ( digIndex >= 0 )
			{
				message = "Digging up plant # " + ( digIndex + 1 );
			}
			break;
		}
		case 4:
			// Visiting the Florist Friar
			return true;
		}

		if ( message != null )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );

			return true;
		}

		// Unknown. Log it.
		return false;
	}
}
