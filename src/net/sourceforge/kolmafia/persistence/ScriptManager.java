package net.sourceforge.kolmafia.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

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
			String forumThread = jObj.getString( "forumThread" );

			return new Script( name, author, shortDesc, repo, longDesc, category, forumThread );
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

			return new InstalledScript( new Script( uuid, null, null, repo.toString(), null, null, null ), scriptFolder );
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

	private static final LockableListModel<Script> installedScripts = new LockableListModel<Script>();
	private static final LockableListModel<Script> repoScripts = new LockableListModel<Script>();
	private static final String REPO_FILE_LOCATION = "https://sourceforge.net/p/kolmafia/code/HEAD/tree/data/SVN/svnrepo.json" + "?format=raw"; //this will change.

	static
	{
		ScriptManager.updateRepoScripts( false );
		ScriptManager.updateInstalledScripts();
	}

	public static void updateRepoScripts( boolean force )
	{
		File repoFile = KoLConstants.SVN_REPO_FILE;
		if ( force || !repoFile.exists() || !Preferences.getBoolean( "_svnRepoFileFetched" ) )
		{
			repoScripts.clear();
			FileUtilities.downloadFile( REPO_FILE_LOCATION, KoLConstants.SVN_REPO_FILE, true );
			Preferences.setBoolean( "_svnRepoFileFetched", true );
		}
		JSONArray jArray = ScriptManager.getJSONArray();
		updateRepoState( jArray );
	}

	private static JSONArray getJSONArray()
	{
		File repoFile = KoLConstants.SVN_REPO_FILE;

		if ( !repoFile.exists() ) {
			return null;
		}
		else
		{
			if (repoFile.length() <= 0) {
				repoFile.delete();
				return null;
			}
		}

		byte[] bytes = ByteBufferUtilities.read( repoFile );
		String string = StringUtilities.getEncodedString( bytes, "UTF-8" );

		try
		{
			return new JSONArray( string );
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
		Set<SVNURL> alreadyInstalled = new HashSet<SVNURL>();

		File[] currentWCs = KoLConstants.SVN_LOCATION.listFiles();

		if ( currentWCs != null )
		{
			for ( File f : currentWCs )
			{
				if ( f.getName().startsWith( "." ) )
					continue;

				try
				{
					alreadyInstalled.add( SVNManager.workingCopyToSVNURL( f ) );
				}
				catch ( SVNException e )
				{
					StaticEntity.printStackTrace( e );
				}
			}
		}

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

				// check uniqueness - if we've already installed the script, leave it out.
				SVNURL jRepo = SVNURL.parseURIEncoded( script.getRepo() );
				if ( !alreadyInstalled.contains( jRepo ) )
					scripts.add( script );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return;
		}

		repoScripts.clear();
		repoScripts.addAll( scripts );
	}

	public static LockableListModel<Script> getInstalledScripts()
	{
		return installedScripts;
	}

	public static void updateInstalledScripts()
	{
		installedScripts.clear();
		File[] scripts = KoLConstants.SVN_LOCATION.listFiles();

		if ( scripts == null )
			return;

		for ( File script : scripts )
		{
			if ( script.getName().startsWith( "." ) )
				continue;

			try
			{
				installedScripts.add( ScriptFactory.fromFile( script ) );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}
	}

	public static LockableListModel<Script> getRepoScripts( )
	{
		return repoScripts;
	}
}
