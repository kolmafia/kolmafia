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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EncounterManager.Encounter;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BugbearManager
{
	public static void resetStatus()
	{
		Preferences.setInteger( "statusEngineering", 0 );
		Preferences.setInteger( "statusGalley", 0 );
		Preferences.setInteger( "statusMedbay", 0 );
		Preferences.setInteger( "statusMorgue", 0 );
		Preferences.setInteger( "statusNavigation", 0 );
		Preferences.setInteger( "statusScienceLab", 0 );
		Preferences.setInteger( "statusSonar", 0 );
		Preferences.setInteger( "statusSpecialOps", 0 );
		Preferences.setInteger( "statusWasteProcessing", 0 );
		Preferences.setInteger( "mothershipProgress", 0 );
	}

	public static final Object[][] BUGBEAR_DATA =
	{
		{
			"Medbay",
			IntegerPool.get( 1 ),
			"hypodermic bugbear",
			"The Spooky Forest",
			IntegerPool.get( 1 ),
			"statusMedbay",
		},
		{
			"Waste Processing",
			IntegerPool.get( 2 ),
			"scavenger bugbear",
			"The Sleazy Back Alley",
			IntegerPool.get( 1 ),
			"statusWasteProcessing",
		},
		{
			"Sonar",
			IntegerPool.get( 3 ),
			"batbugbear",
			"Guano Junction",
			IntegerPool.get( 1 ),
			"statusSonar",
		},
		{
			"Science Lab",
			IntegerPool.get( 4 ),
			"bugbear scientist",
			"Cobb's Knob Laboratory",
			IntegerPool.get( 2 ),
			"statusScienceLab"
		},
		{
			"Morgue",
			IntegerPool.get( 5 ),
			"bugaboo",
			new String [] {
				"The Defiled Nook",
				"Post-Cyrpt Cemetary",
			},
			IntegerPool.get( 2 ),
			"statusMorgue",
		},
		{
			"Special Ops",
			IntegerPool.get( 6 ),
			"black ops bugbear",
			"Lair of the Ninja Snowmen",
			IntegerPool.get( 2 ),
			"statusSpecialOps",
		},
		{
			"Engineering",
			IntegerPool.get( 7 ),
			"battlesuit bugbear type",
			"The Penultimate Fantasy Airship",
			IntegerPool.get( 3 ),
			"statusEngineering",
		},
		{
			"Navigation",
			IntegerPool.get( 8 ),
			"ancient unspeakable bugbear",
			"The Haunted Gallery",
			IntegerPool.get( 3 ),
			"statusNavigation",
		},
		{
			"Galley",
			IntegerPool.get( 9 ),
			"trendy bugbear chef",
			new String [] {
				"The Battlefield (Frat Uniform)",
				"The Battlefield (Hippy Uniform)",
			},
			IntegerPool.get( 3 ),
			"statusGalley",
		}
	};

	public static String dataToShipZone( Object[] data )
	{
		return data == null ? "" : (String)data[ 0 ];
	}

	public static int dataToId( Object[] data )
	{
		return data == null ? 0 : ((Integer)data[ 1 ]).intValue();
	}

	public static String dataToBugbear( Object[] data )
	{
		return data == null ? "" : (String)data[ 2 ];
	}

	public static String dataToBugbearZone1( Object[] data )
	{
		if ( data == null )
		{
			return null;
		}

		Object zones = data[ 3 ];
		return  zones instanceof String ? (String)zones :
			zones instanceof String[] ? ((String[])zones)[0] :
			"";
	}

	public static String dataToBugbearZone2( Object[] data )
	{
		if ( data == null )
		{
			return null;
		}

		Object zones = data[ 3 ];
		return  zones instanceof String ? "" :
			zones instanceof String[] ? ((String[])zones)[1] :
			"";
	}

	public static int dataToLevel( Object[] data )
	{
		return data == null ? 0 : ((Integer)data[ 4 ]).intValue();
	}

	public static String dataToStatusSetting( Object[] data )
	{
		return data == null ? "" : (String)data[ 5 ];
	}

	public static Object[] idToData( final int id )
	{
		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] data = BugbearManager.BUGBEAR_DATA[ i ];
			if ( BugbearManager.dataToId( data ) == id )
			{
				return data;
			}
		}
		return null;
	}

	public static Object[] bugbearToData( final String bugbear )
	{
		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] data = BugbearManager.BUGBEAR_DATA[ i ];
			if ( bugbear.equals( BugbearManager.dataToBugbear( data ) ) )
			{
				return data;
			}
		}
		return null;
	}

	public static Object[] shipZoneToData( final String zone )
	{
		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] data = BugbearManager.BUGBEAR_DATA[ i ];
			if ( zone.equals( BugbearManager.dataToShipZone( data ) ) )
			{
				return data;
			}
		}
		return null;
	}

	public static void setBiodata( final Object[] data, final String countString )
	{
		BugbearManager.setBiodata( data, StringUtilities.parseInt( countString ) );
	}

	public static void setBiodata( final Object[] data, final int count )
	{
		if ( data == null )
		{
			return;
		}

		String statusSetting = BugbearManager.dataToStatusSetting( data );
		int level = BugbearManager.dataToLevel( data );
		if ( count < level * 3 )
		{
			Preferences.setInteger( statusSetting, count );
			return;
		}

		String currentStatus = Preferences.getString( statusSetting );
		if ( !StringUtilities.isNumeric( currentStatus ) )
		{
			return;
		}

		int currentProgress = Preferences.getInteger( "mothershipProgress" );
		String newStatus = ( level == currentProgress + 1 ) ? "open" : "unlocked";
		Preferences.setString( statusSetting, newStatus );
	}

	public static void clearShipZone( final String zone )
	{
		Object[] data = BugbearManager.shipZoneToData( zone );
		if ( data == null )
		{
			return;
		}

		String statusSetting = BugbearManager.dataToStatusSetting( data );
		if ( Preferences.getString( statusSetting ).equals( "cleared" ) )
		{
			return;
		}

		// Mark this ship zone cleared
		Preferences.setString( statusSetting, "cleared" );

		// Calculate which level of the ship this zone is on
		int level = BugbearManager.dataToLevel( data );

		// See if we have cleared all the zones on this level
		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] zoneData = BugbearManager.BUGBEAR_DATA[ i ];
			if ( BugbearManager.dataToLevel( zoneData ) != level )
			{
				continue;
			}
			String zoneSetting = BugbearManager.dataToStatusSetting( zoneData );
			String status = Preferences.getString( zoneSetting );
			if ( !status.equals( "cleared" ) )
			{
				return;
			}
		}

		// Yes. We have cleared this level
		Preferences.setInteger( "mothershipProgress", level );
		if ( level == 3 )
		{
			return;
		}

		// All "unlocked" zones on the next level are now "open"
		int nextLevel = level + 1;
		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] zoneData = BugbearManager.BUGBEAR_DATA[ i ];
			if ( BugbearManager.dataToLevel( zoneData ) != nextLevel )
			{
				continue;
			}
			String zoneSetting = BugbearManager.dataToStatusSetting( zoneData );
			String status = Preferences.getString( zoneSetting );
			if ( status.equals( "unlocked" ) )
			{
				Preferences.setString( zoneSetting, "open" );
			}
		}
	}

	public static void registerEncounter( final Encounter encounter, final String responseText )
	{
		// All BUGBEAR encounters indicate that a mothership zone has been cleared

		String zone = encounter.getLocation();
		String encounterName = encounter.getEncounter();

		// We could look at the responseText here to confirm, if we wanted.

		BugbearManager.clearShipZone( zone );
	}

}
