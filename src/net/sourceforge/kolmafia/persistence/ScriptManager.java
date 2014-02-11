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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.svn.SVNManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public class ScriptManager
{
	private static class ScriptFactory
	{
		public static Script fromJSON( JSONObject jObj )
			throws JSONException
		{
			String name = jObj.getString( "name" );
			String repo = jObj.getString( "repo" );
			String author = jObj.getString( "author" );
			String category = jObj.getString( "category" );
			String shortDesc = jObj.getString( "shortDesc" );
			String longDesc = jObj.getString( "longDesc" );

			return new Script( name, author, shortDesc, repo, longDesc, category );
		}

		public static Script fromFile( File scriptFolder )
			throws SVNException, JSONException
		{
			// convert the folder name to a repo.  Then see if there's a matching entry in the repo file.

			SVNURL repo = SVNManager.workingCopyToSVNURL( scriptFolder );

			JSONObject ob = repoToJSONObject( repo );

			if ( ob == null )
			{
				return fromUnknown( repo, scriptFolder );
			}

			Script s = fromJSON( ob );
			return new InstalledScript( s, scriptFolder );
		}

		private static Script fromUnknown( SVNURL repo, File scriptFolder )
		{
			// we can still fetch info on the repo...
			String uuid = SVNManager.getFolderUUIDNoRemote( repo );

			return new InstalledScript( new Script( uuid, null, null, repo.toString(), null, null ), scriptFolder );
		}

		private static JSONObject repoToJSONObject( SVNURL repo )
			throws JSONException, SVNException
		{
			JSONArray jArray = getJSONArray();

			if ( jArray == null )
				return null;

			for ( int i = 0; i < jArray.length(); i++ )
			{
				Object next = jArray.get( i );
				if ( !( next instanceof JSONObject ) )
				{
					throw new JSONException( "The JSON input file was not properly formatted: " + next.toString() );
				}

				JSONObject jNext = (JSONObject) next;

				SVNURL fromRepo = SVNURL.parseURIEncoded( jNext.getString( "repo" ) );

				if ( repo.equals( fromRepo ) )
				{
					return jNext;
				}
			}
			return null;
		}

	}

	private static final LockableListModel installedScripts = new LockableListModel();
	private static final LockableListModel repoScripts = new LockableListModel();
	private static final String REPO_FILE_LOCATION = "https://gist.github.com/roippi/b1b9620aaa5ada9983f4/raw/d449160dbd67c2d2771a65890d5b8a652a0ac8b9/svnrepo.json"; //this will change.

	static
	{
		ScriptManager.updateRepoScripts( false );
		ScriptManager.updateInstalledScripts();
	}

	public static void updateRepoScripts( boolean force )
	{
		if ( force || !Preferences.getBoolean( "_svnRepoFileFetched" ) )
		{
			repoScripts.clear();
			FileUtilities.downloadFile( REPO_FILE_LOCATION, KoLConstants.SVN_REPO_FILE, true);
			Preferences.setBoolean( "_svnRepoFileFetched", true );
		}
		JSONArray jArray = ScriptManager.getJSONArray();
		updateRepoState( jArray );
	}

	private static JSONArray getJSONArray()
	{
		File repoFile = KoLConstants.SVN_REPO_FILE;

		if ( !repoFile.exists() )
			return null;
		BufferedReader reader = FileUtilities.getReader( repoFile );
		StringBuilder builder = new StringBuilder();

		try
		{
			String[] data;
			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				for ( String s : data )
				{
					builder.append( s );
				}
			}
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch ( IOException e )
			{
				StaticEntity.printStackTrace( e );
			}
		}

		try
		{
			JSONArray jArray = new JSONArray( builder.toString() );
			return jArray;
		}
		catch ( JSONException e )
		{
			StaticEntity.printStackTrace( e );
		}

		return null;
	}

	private static void updateRepoState( JSONArray jArray )
	{
		if ( jArray == null )
			return;

		ArrayList<Script> scripts = new ArrayList<Script>();

		try
		{
			for ( int i = 0; i < jArray.length(); i++ )
			{
				Object next = jArray.get( i );
				if ( !( next instanceof JSONObject ) )
				{
					throw new JSONException( "The JSON input file was not properly formatted: " + next.toString() );
				}

				JSONObject jNext = (JSONObject) next;

				Script script = ScriptFactory.fromJSON( jNext );

				scripts.add( script );
			}

		}
		catch ( JSONException e )
		{
			StaticEntity.printStackTrace( e );
			return;
		}

		repoScripts.clear();
		repoScripts.addAll( scripts );
	}

	public static LockableListModel getInstalledScripts()
	{
		return installedScripts;
	}

	public static void updateInstalledScripts()
	{
		installedScripts.clear();
		File[] scripts = KoLConstants.SVN_LOCATION.listFiles();

		for ( File script : scripts )
		{
			try
			{
				installedScripts.add( ScriptFactory.fromFile( script ) );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
				continue;
			}
		}
	}

	public static LockableListModel getRepoScripts( boolean force )
	{
		return repoScripts;
	}
}
