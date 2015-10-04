/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PottedTeaTreeRequest
	extends GenericRequest
{
	private static final TreeMap<Integer,PottedTea> idToTea = new TreeMap<Integer,PottedTea>();
	private static final TreeMap<String,PottedTea> canonicalNameToTea = new TreeMap<String,PottedTea>();

	static
	{
		PottedTeaTreeRequest.registerTea( "Activi tea", 8624, "Spleen Item" );
		PottedTeaTreeRequest.registerTea( "Alacri tea", 8626, "Initiative" );
		PottedTeaTreeRequest.registerTea( "Boo tea", 8603, "Spooky damage" );
		PottedTeaTreeRequest.registerTea( "Chari tea", 8611, "Meat" );
		PottedTeaTreeRequest.registerTea( "Craft tea", 8636, "Free crafting" );
		PottedTeaTreeRequest.registerTea( "Cruel tea", 8625, "Spleen Item" );
		PottedTeaTreeRequest.registerTea( "Dexteri tea", 8617, "Moxie" );
		PottedTeaTreeRequest.registerTea( "Feroci tea", 8613, "Muscle" );
		PottedTeaTreeRequest.registerTea( "Flamibili tea", 8601, "Hot damage" );
		PottedTeaTreeRequest.registerTea( "Flexibili tea", 8618, "Moxie stats" );
		PottedTeaTreeRequest.registerTea( "Frost tea", 8606, "Hot resistance" );
		PottedTeaTreeRequest.registerTea( "Gill tea", 8631, "Underwater" );
		PottedTeaTreeRequest.registerTea( "Impregnabili tea", 8619, "DR" );
		PottedTeaTreeRequest.registerTea( "Improprie tea", 8605, "Sleaze damage" );
		PottedTeaTreeRequest.registerTea( "Insani tea", 8637, "Random Monster Mods" );
		PottedTeaTreeRequest.registerTea( "Irritabili tea", 8621, "+Combat" );
		PottedTeaTreeRequest.registerTea( "Loyal tea", 8623, "Familiar weight" );
		PottedTeaTreeRequest.registerTea( "Mana tea", 8628, "MP" );
		PottedTeaTreeRequest.registerTea( "Mediocri tea", 8622, "+ML" );
		PottedTeaTreeRequest.registerTea( "Monstrosi tea", 8629, "-ML" );
		PottedTeaTreeRequest.registerTea( "Morbidi tea", 8610, "Spooky resist" );
		PottedTeaTreeRequest.registerTea( "Nas tea", 8604, "Stench damage" );
		PottedTeaTreeRequest.registerTea( "Net tea", 8608, "Stench resist" );
		PottedTeaTreeRequest.registerTea( "Neuroplastici tea", 8616, "Myst stats" );
		PottedTeaTreeRequest.registerTea( "Obscuri tea", 8620, "-Combat" );
		PottedTeaTreeRequest.registerTea( "Physicali tea", 8614, "Muscle stats" );
		PottedTeaTreeRequest.registerTea( "Proprie tea", 8609, "Sleaze resist" );
		PottedTeaTreeRequest.registerTea( "Royal tea", 8635, "Royalty" );
		PottedTeaTreeRequest.registerTea( "Serendipi tea", 8612, "Item" );
		PottedTeaTreeRequest.registerTea( "Sobrie tea", 8634, "Drunk reduction" );
		PottedTeaTreeRequest.registerTea( "Toast tea", 8607, "Cold resist" );
		PottedTeaTreeRequest.registerTea( "Twen tea", 8630, "Lots of boosts" );
		PottedTeaTreeRequest.registerTea( "Uncert	ain tea", 8632, "???" );
		PottedTeaTreeRequest.registerTea( "Vitali tea", 8627, "HP" );
		PottedTeaTreeRequest.registerTea( "Voraci tea", 8633, "Stomach increase" );
		PottedTeaTreeRequest.registerTea( "Wit tea", 8615, "Myst" );
		PottedTeaTreeRequest.registerTea( "Yet tea", 8602, "Cold damage" );
	};

	private static PottedTea registerTea( String name, int id, String effect )
	{
		PottedTea tea = new PottedTea( name, id, effect );
		PottedTeaTreeRequest.idToTea.put( id, tea );
		PottedTeaTreeRequest.canonicalNameToTea.put( StringUtilities.getCanonicalName( name ), tea );
		return tea;
	}

	private static String [] CANONICAL_TEA_ARRAY;
	static
	{
		Set<String> keys = PottedTeaTreeRequest.canonicalNameToTea.keySet();
		PottedTeaTreeRequest.CANONICAL_TEA_ARRAY = keys.toArray( new String[ keys.size() ] );
	};

	public static final List<String> getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames(PottedTeaTreeRequest.CANONICAL_TEA_ARRAY, substring );
	}

	public static PottedTea canonicalNameToTea( String name )
	{
		return PottedTeaTreeRequest.canonicalNameToTea.get( name );
	}

	private PottedTea tea;

	// Shake the tree
	public PottedTeaTreeRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1104" );
		this.addFormField( "option", "1" );
		this.tea = null;
	}

	// Pick a specific tea
	public PottedTeaTreeRequest( PottedTea tea )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1105" );
		this.addFormField( "option", "1" );
		this.addFormField( "itemid", String.valueOf( tea.id ) );
		this.tea = tea;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// If you don't have a potted tea tree, punt
		if ( !KoLConstants.campground.contains( ItemPool.get( ItemPool.POTTED_TEA_TREE, 1 ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a potted tea tree." );
			return;
		}

		// If you already used your potted tea tree, punt
		if ( Preferences.getBoolean( "_pottedTeaTreeUsed" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already harvested your potted tea tree today." );
			return;
		}

		GenericRequest campRequest = new GenericRequest( "campground.php" );
		campRequest.addFormField( "action", "teatree" );
		campRequest.run();

		if ( this.tea != null )
		{
			GenericRequest pickRequest = new GenericRequest( "choice.php" );
			pickRequest.addFormField( "whichchoice", "1104" );
			pickRequest.addFormField( "option", "2" );
			pickRequest.run();
		}

		super.run();
	}

	public static class PottedTea
	{
		public int id;
		public String name;
		private String effect;

		public PottedTea( String name, int id, String effect )
		{
			this.id = id;
			this.name = name;
			this.effect = effect;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}
	public static final Pattern URL_TEA_PATTERN = Pattern.compile( "itemid=(\\d+)" );
	public static PottedTea extractTeaFromURL( final String urlString )
	{
		Matcher matcher = PottedTeaTreeRequest.URL_TEA_PATTERN.matcher( urlString );
		return  matcher.find() ?
			PottedTeaTreeRequest.idToTea.get( StringUtilities.parseInt( matcher.group( 1 ) ) ) :
			null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1104 && choice != 1105 )
		{
			return false;
		}
		
		String teaharvested = null;

		if ( choice == 1104 && urlString.contains( "option=1" ) )
		{
			teaharvested = "shake";
		}
		else if ( choice == 1105 )
		{
			PottedTea tea = PottedTeaTreeRequest.extractTeaFromURL( urlString );
			if ( tea != null )
			teaharvested = tea.toString();
		}

		if ( teaharvested == null )
		{
			return true;
		}

		String message = "teatree " + teaharvested;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
