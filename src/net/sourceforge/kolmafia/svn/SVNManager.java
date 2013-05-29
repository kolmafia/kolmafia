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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

//some functions taken/adapted from http://wiki.svnkit.com/Managing_A_Working_Copy

/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package net.sourceforge.kolmafia.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;

public class SVNManager
{
	private static Stack<SVNFileEvent> eventStack = new Stack<SVNFileEvent>();

	private static SVNClientManager ourClientManager;
	//private static ISVNEventHandler myCommitEventHandler;
	private static ISVNEventHandler myUpdateEventHandler;
	private static ISVNEventHandler myWCEventHandler;

	private static Pattern SOURCEFORGE_PATTERN = Pattern.compile( "/p/(\\w+)/code(.*)", Pattern.DOTALL );
	private static List<String> permissibles = Arrays.asList( "scripts", "data", "images", "relay" );

	/**
	 * Initializes the library to work with a repository via different protocols.
	 */
	public static void setupLibrary()
	{
		/*
		 * For using over http:// and https://
		 */
		DAVRepositoryFactory.setup();
		/*
		 * For using over svn:// and svn+xxx://
		 */
		SVNRepositoryFactoryImpl.setup();

		if ( ourClientManager != null )
			return;

		ISVNOptions options = SVNWCUtil.createDefaultOptions( true );

		/*
		 * Creates an instance of SVNClientManager providing authentication information (name, password) and an options
		 * driver
		 */
		ourClientManager = SVNClientManager.newInstance( options );

		myUpdateEventHandler = new UpdateEventHandler();

		myWCEventHandler = new WCEventHandler();

		/*
		 * Sets a custom event handler for operations of an SVNUpdateClient instance
		 */
		ourClientManager.getUpdateClient().setEventHandler( myUpdateEventHandler );

		/*
		 * Sets a custom event handler for operations of an SVNWCClient instance
		 */
		ourClientManager.getWCClient().setEventHandler( myWCEventHandler );
	}

	/*
	 * Creates a new version controlled directory (doesn't create any intermediate directories) right in a repository.
	 * Like 'svn mkdir URL -m "some comment"' command. It's done by invoking SVNCommitClient.doMkDir(SVNURL[] urls,
	 * String commitMessage) which takes the following parameters: urls - an array of URLs that are to be created;
	 * commitMessage - a commit log message since a URL-based directory creation is immediately committed to a
	 * repository.
	 */
	private static SVNCommitInfo makeDirectory( SVNURL url, String commitMessage )
		throws SVNException
	{
		/*
		 * Returns SVNCommitInfo containing information on the new revision committed (revision number, etc.)
		 */
		return ourClientManager.getCommitClient().doMkDir( new SVNURL[]
		{ url
		}, commitMessage );
	}

	/*
	 * Imports an unversioned directory into a repository location denoted by a destination URL (all necessary parent
	 * non-existent paths will be created automatically). This operation commits the repository to a new revision. Like
	 * 'svn import PATH URL (-N) -m "some comment"' command. It's done by invoking SVNCommitClient.doImport(File path,
	 * SVNURL dstURL, String commitMessage, boolean recursive) which takes the following parameters: path - a local
	 * unversioned directory or singal file that will be imported into a repository; dstURL - a repository location
	 * where the local unversioned directory/file will be imported into; this URL path may contain non-existent parent
	 * paths that will be created by the repository server; commitMessage - a commit log message since the new
	 * directory/file are immediately created in the repository; recursive - if true and path parameter corresponds to a
	 * directory then the directory will be added with all its child subdirictories, otherwise the operation will cover
	 * only the directory itself (only those files which are located in the directory).
	 */
	private static SVNCommitInfo importDirectory( File localPath, SVNURL dstURL, String commitMessage,
		boolean isRecursive )
		throws SVNException
	{
		/*
		 * Returns SVNCommitInfo containing information on the new revision committed (revision number, etc.)
		 */
		return ourClientManager.getCommitClient().doImport( localPath, dstURL, commitMessage, isRecursive );

	}

	/*
	 * Committs changes in a working copy to a repository. Like 'svn commit PATH -m "some comment"' command. It's done
	 * by invoking SVNCommitClient.doCommit(File[] paths, boolean keepLocks, String commitMessage, boolean force,
	 * boolean recursive) which takes the following parameters: paths - working copy paths which changes are to be
	 * committed; keepLocks - if true then doCommit(..) won't unlock locked paths; otherwise they will be unlocked after
	 * a successful commit; commitMessage - a commit log message; force - if true then a non-recursive commit will be
	 * forced anyway; recursive - if true and a path corresponds to a directory then doCommit(..) recursively commits
	 * changes for the entire directory, otherwise - only for child entries of the directory;
	 */
	private static SVNCommitInfo commit( File wcPath, boolean keepLocks, String commitMessage )
		throws SVNException
	{
		/*
		 * Returns SVNCommitInfo containing information on the new revision committed (revision number, etc.)
		 */
		return ourClientManager.getCommitClient().doCommit( new File[]
		{ wcPath
		}, keepLocks, commitMessage, false, true );
	}

	/*
	 * Checks out a working copy from a repository. Like 'svn checkout URL[@REV] PATH (-r..)' command; It's done by
	 * invoking SVNUpdateClient.doCheckout(SVNURL url, File dstPath, SVNRevision pegRevision, SVNRevision revision,
	 * boolean recursive) which takes the following parameters: url - a repository location from where a working copy is
	 * to be checked out; dstPath - a local path where the working copy will be fetched into; pegRevision - an
	 * SVNRevision representing a revision to concretize url (what exactly URL a user means and is sure of being the URL
	 * he needs); in other words that is the revision in which the URL is first looked up; revision - a revision at
	 * which a working copy being checked out is to be; recursive - if true and url corresponds to a directory then
	 * doCheckout(..) recursively fetches out the entire directory, otherwise - only child entries of the directory;
	 */
	public static long checkout( SVNURL url, SVNRevision revision, File destPath, boolean isRecursive )
		throws SVNException
	{

		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the checkout
		 */
		updateClient.setIgnoreExternals( false );
		/*
		 * returns the number of the revision at which the working copy is
		 */
		return updateClient.doCheckout( url, destPath, revision, revision, isRecursive );
	}

	/*
	 * Updates a working copy (brings changes from the repository into the working copy). Like 'svn update PATH'
	 * command; It's done by invoking SVNUpdateClient.doUpdate(File file, SVNRevision revision, boolean recursive) which
	 * takes the following parameters: file - a working copy entry that is to be updated; revision - a revision to which
	 * a working copy is to be updated; recursive - if true and an entry is a directory then doUpdate(..) recursively
	 * updates the entire directory, otherwise - only child entries of the directory;
	 */
	public static long update( File wcPath, SVNRevision updateToRevision, boolean isRecursive )
		throws SVNException
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the update
		 */
		updateClient.setIgnoreExternals( false );
		/*
		 * returns the number of the revision wcPath was updated to
		 */
		return updateClient.doUpdate( wcPath, updateToRevision, isRecursive );
	}

	/*
	 * Updates a working copy to a different URL. Like 'svn switch URL' command. It's done by invoking
	 * SVNUpdateClient.doSwitch(File file, SVNURL url, SVNRevision revision, boolean recursive) which takes the
	 * following parameters: file - a working copy entry that is to be switched to a new url; url - a target URL a
	 * working copy is to be updated against; revision - a revision to which a working copy is to be updated; recursive
	 * - if true and an entry (file) is a directory then doSwitch(..) recursively switches the entire directory,
	 * otherwise - only child entries of the directory;
	 */
	private static long switchToURL( File wcPath, SVNURL url, SVNRevision updateToRevision, boolean isRecursive )
		throws SVNException
	{
		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the switch
		 */
		updateClient.setIgnoreExternals( false );
		/*
		 * returns the number of the revision wcPath was updated to
		 */
		return updateClient.doSwitch( wcPath, url, updateToRevision, isRecursive );
	}

	/*
	 * Collects status information on local path(s). Like 'svn status (-u) (-N)' command. It's done by invoking
	 * SVNStatusClient.doStatus(File path, boolean recursive, boolean remote, boolean reportAll, boolean includeIgnored,
	 * boolean collectParentExternals, ISVNStatusHandler handler) which takes the following parameters: path - an entry
	 * which status info to be gathered; recursive - if true and an entry is a directory then doStatus(..) collects
	 * status info not only for that directory but for each item inside stepping down recursively; remote - if true then
	 * doStatus(..) will cover the repository (not only the working copy) as well to find out what entries are out of
	 * date; reportAll - if true then doStatus(..) will also include unmodified entries; includeIgnored - if true then
	 * doStatus(..) will also include entries being ignored; collectParentExternals - if true then externals definitions
	 * won't be ignored; handler - an implementation of ISVNStatusHandler to process status info per each entry
	 * doStatus(..) traverses; such info is collected in an SVNStatus object and is passed to a handler's
	 * handleStatus(SVNStatus status) method where an implementor decides what to do with it.
	 */
	private static void showStatus( File wcPath, boolean isRecursive, boolean isRemote, boolean isReportAll,
		boolean isIncludeIgnored, boolean isCollectParentExternals )
		throws SVNException
	{
		/*
		 * StatusHandler displays status information for each entry in the console (in the manner of the native
		 * Subversion command line client)
		 */
		ourClientManager.getStatusClient().doStatus( wcPath, isRecursive, isRemote, isReportAll, isIncludeIgnored, isCollectParentExternals, new StatusHandler(
			isRemote ) );
	}

	/*
	 * Collects information on local path(s). Like 'svn info (-R)' command. It's done by invoking
	 * SVNWCClient.doInfo(File path, SVNRevision revision, boolean recursive, ISVNInfoHandler handler) which takes the
	 * following parameters: path - a local entry for which info will be collected; revision - a revision of an entry
	 * which info is interested in; if it's not WORKING then info is got from a repository; recursive - if true and an
	 * entry is a directory then doInfo(..) collects info not only for that directory but for each item inside stepping
	 * down recursively; handler - an implementation of ISVNInfoHandler to process info per each entry doInfo(..)
	 * traverses; such info is collected in an SVNInfo object and is passed to a handler's handleInfo(SVNInfo info)
	 * method where an implementor decides what to do with it.
	 */
	public static void showInfo( File wcPath, SVNRevision revision, boolean isRecursive )
		throws SVNException
	{
		/*
		 * InfoHandler displays information for each entry in the console (in the manner of the native Subversion
		 * command line client)
		 */
		ourClientManager.getWCClient().doInfo( wcPath, revision, isRecursive, new InfoHandler() );
	}

	/*
	 * Puts directories and files under version control scheduling them for addition to a repository. They will be added
	 * in a next commit. Like 'svn add PATH' command. It's done by invoking SVNWCClient.doAdd(File path, boolean force,
	 * boolean mkdir, boolean climbUnversionedParents, boolean recursive) which takes the following parameters: path -
	 * an entry to be scheduled for addition; force - set to true to force an addition of an entry anyway; mkdir - if
	 * true doAdd(..) creates an empty directory at path and schedules it for addition, like 'svn mkdir PATH' command;
	 * climbUnversionedParents - if true and the parent of the entry to be scheduled for addition is not under version
	 * control, then doAdd(..) automatically schedules the parent for addition, too; recursive - if true and an entry is
	 * a directory then doAdd(..) recursively schedules all its inner dir entries for addition as well.
	 */
	private static void addEntry( File wcPath )
		throws SVNException
	{
		ourClientManager.getWCClient().doAdd( wcPath, false, false, false, true );
	}

	/*
	 * Locks working copy paths, so that no other user can commit changes to them. Like 'svn lock PATH' command. It's
	 * done by invoking SVNWCClient.doLock(File[] paths, boolean stealLock, String lockMessage) which takes the
	 * following parameters: paths - an array of local entries to be locked; stealLock - set to true to steal the lock
	 * from another user or working copy; lockMessage - an optional lock comment string.
	 */
	private static void lock( File wcPath, boolean isStealLock, String lockComment )
		throws SVNException
	{
		ourClientManager.getWCClient().doLock( new File[]
		{ wcPath
		}, isStealLock, lockComment );
	}

	/*
	 * Schedules directories and files for deletion from version control upon the next commit (locally). Like 'svn
	 * delete PATH' command. It's done by invoking SVNWCClient.doDelete(File path, boolean force, boolean dryRun) which
	 * takes the following parameters: path - an entry to be scheduled for deletion; force - a boolean flag which is set
	 * to true to force a deletion even if an entry has local modifications; dryRun - set to true not to delete an entry
	 * but to check if it can be deleted; if false - then it's a deletion itself.
	 */
	private static void delete( File wcPath, boolean force )
		throws SVNException
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		ourClientManager.getWCClient().doDelete( wcPath, force, false );
	}

	/*
	 * Displays error information and exits.
	 */
	private static void error( String message, Exception e )
	{
		KoLmafia.updateDisplay( MafiaState.ERROR, message + ( e != null ? ": " + e.getMessage() : "" ) );
	}

	public static void doCheckout( SVNURL repo )
	{
		String UUID = SVNManager.getFolderUUID( repo );

		if ( UUID == null )
		{
			return;
		}

		if ( SVNManager.validateRepo( repo ) )
		{
			RequestLogger.printLine( "repo at " + repo.getPath() + " did not pass validation.  Aborting Checkout." );
			return;
		}

		File WCDir = SVNManager.doDirSetup( UUID );
		if ( WCDir == null )
		{
			RequestLogger.printLine( "Something went wrong creating directories..." );
			return;
		}

		try
		{
			SVNManager.checkout( repo, SVNRevision.HEAD, WCDir, true );
		}
		catch ( SVNException e )
		{
			RequestLogger.printLine( "SVN ERROR during checkout operation.  Aborting..." );
			StaticEntity.printStackTrace( e );
			return;
		}

		RequestLogger.printLine();
		RequestLogger.printLine( "Successfully checked out working copy." );

		pushUpdates( true );
	}

	/**
	 * There are only 4 permissible files in the top level of the repo, all of them directories: scripts/, data/,
	 * images/, and relay/. Any other files (directories or otherwise) in the top-level fails validation. 
	 * TODO: check
	 * file extensions, reject naughty ones
	 */

	private static boolean validateRepo( SVNURL repo )
	{
		SVNRepository repository;
		Collection< ? > entries;
		boolean failed = false;

		try
		{
			repository = SVNRepositoryFactory.create( repo );
		}
		catch ( SVNException e )
		{
			RequestLogger.printLine( "Unable to connect with repository at " + repo.getPath() );
			return false;
		}

		RequestLogger.printLine( "Validating repo..." );

		try
		{
			entries = repository.getDir( "", -1, null, (Collection< ? >) null ); // this syntax is stupid, by the way
		}
		catch ( SVNException e )
		{
			RequestLogger.printLine( "Something went wrong while fetching svn directory info" );
			return false;
		}

		Iterator< ? > iterator = entries.iterator();
		while ( iterator.hasNext() )
		{
			SVNDirEntry entry = (SVNDirEntry) iterator.next();
			if ( entry.getKind().equals( SVNNodeKind.DIR ) )
			{
				failed |= !permissibles.contains( entry.getName() );
			}
			else
				// something other than a directory!  FAIL
				failed = true;
		}

		if ( failed )
		{
			RequestLogger.printLine( "The requested repo failed validation.  Complain to the script's author." );
		}
		else
		{
			RequestLogger.printLine( "Repo validated." );
		}

		return failed;
	}

	public static void pushUpdates()
	{
		pushUpdates( false );
	}

	/**
	 * Method to copy all queued updates from the individual Working Copy folders to the mafia subdirectories.
	 * <p>
	 * If a checkout was performed, we automatically push ALL files to subdirectories. If an update was performed, we
	 * push new files (SVNEventAction.UPDATE_ADD) always, and updates (SVNEventAction.UPDATE_UPDATE) iff the file still
	 * exists in the destination subdirectory. If the user deleted it, we don't want to re-add it.
	 * <p>
	 * For updates (SVNEventAction.UPDATE_UPDATE), also handle all three possible change results (SVNStatusType.CHANGED,
	 * SVNStatusType.CONFLICTED, and SVNStatusType.MERGED).
	 * 
	 * @param wasCheckout is <b>true</b> if a checkout was performed first.
	 */
	private static void pushUpdates( boolean wasCheckout )
	{
		
		List<String> pathsToSkip = doFinalChecks( wasCheckout );
		if ( pathsToSkip.size() > 0 )
		{
			RequestLogger.printLine( "NOTE: Skipping some updates due to user request." );
		}

		if ( !eventStack.isEmpty() )
			RequestLogger.printLine( "Pushing local updates..." );

		while ( !eventStack.isEmpty() )
		{
			SVNFileEvent event = eventStack.pop();
			if ( event.getFile().isDirectory() )
			{
				continue; // directories will be generated by mkdirs(), no need to explicitly create them
			}

			// we now need to obtain the file's path relative to one of the four "permissible" folders
			// iterate up f until we find the depth we want to be at
			File fDepth = findDepth( event.getFile().getParentFile() );
			if ( fDepth == null )
			{
				eventStack.clear();
				return;
			}

			// get the path of the file relative to the project folder.
			// example: C:/mafia/svn/<projectname>/scripts/dir1/dir2/scriptfile.ash becomes scripts/dir1/dir2/scriptfile.ash
			String relpath = FileUtilities.getRelativePath( fDepth.getParentFile(), event.getFile() );

			if ( pathsToSkip.contains( relpath ) )
			{
				// user wants to skip it
				RequestLogger.printLine( "Skipping " + relpath );
				continue;
			}

			if ( shouldPush( event, wasCheckout, relpath ) )
			{
				doPush( event.getFile(), relpath );
			}
			else if ( shouldDelete(event) )
			{
				doDelete( event.getFile(), relpath );
			}
		}

		RequestLogger.printLine( "Done." );
		eventStack.clear();
	}

	private static File findDepth( File f )
	{
		return findDepth( f, false );
	}
	
	private static File findDepth( File f, boolean force )
	{
		String original = f.getAbsolutePath();
		foundDepth :
		{
			while ( f != null )
			{
				// look two directories up.  If it is the svn folder, we're at the level we want to be.
				if ( f.getParentFile() != null && f.getParentFile().getParentFile() != null &&
					f.getParentFile().getParentFile().equals( KoLConstants.SVN_LOCATION ) )
					break foundDepth;

				f = f.getParentFile();
			}
			RequestLogger.printLine( "Internal error: could not find relative path for " + original + ".  Aborting." );
			return null;
		}

		if ( !force && !permissibles.contains( f.getName() ) )
		{
			//shouldn't happen.  Validation should have failed.
			RequestLogger.printLine( "Non-permissible folder in SVN root: " + f.getName() + " Stopping local updates." );
			return null;
		}

		return f;
	}

	/**
	 * If an update (not a checkout) results in svn adding a new file from the repo, make sure the user is okay with it.
	 * <p>
	 * This is to hopefully safeguard against malicious script injection, but the user has to catch it.
	 * 
	 * @return a <b>List</b> of files to skip.
	 */
	private static List<String> doFinalChecks( boolean wasCheckout )
	{
		List<String> skipFiles = new ArrayList<String>();

		if ( wasCheckout ) // never warn on checkout operations
			return skipFiles;

		List<SVNURL> skipURLs = new ArrayList<SVNURL>();
		@SuppressWarnings( "unchecked" )
		// no type-safe way to do this in Java 5 (6 has Deque)
		Stack<SVNFileEvent> eventStackCopy = (Stack<SVNFileEvent>) SVNManager.eventStack.clone();

		while ( !eventStackCopy.isEmpty() )
		{
			SVNFileEvent event = eventStackCopy.pop();
			if ( event.getFile().isDirectory() )
			{
				continue; // directories are harmless
			}
			File fDepth = findDepth( event.getFile().getParentFile() );
			if ( fDepth == null )
			{
				//shouldn't happen, punt
				return skipFiles;
			}

			// We only want to prompt if the file is new to the working copy (SVNEventAction.UPDATE_ADD) 
			// as this most likely means that it was recently added to the repo
			if ( event.getEvent().getAction() == SVNEventAction.UPDATE_ADD )
			{
				skipFiles.add( FileUtilities.getRelativePath( fDepth.getParentFile(), event.getFile() ) );
				skipURLs.add( event.getEvent().getURL() );
			}
		}

		if ( skipFiles.size() > 0 )
		{
			SVNRepository repo = null;
			try
			{
				repo = SVNRepositoryFactory.create( skipURLs.get( 0 ) );
			}
			catch ( SVNException e1 )
			{
				RequestLogger.printLine( "Something went wrong fetching SVN info" );
				//punt, NPE ensues if we continue with this method without initializing repo
				return skipFiles;
			}

			StringBuilder message = new StringBuilder("<html>New file(s) requesting confirmation:<p>" );
			for ( int i = 0; i < skipFiles.size(); ++i )
			{
				message.append( "<b>file</b>: " + skipFiles.get( i ) + "<p>" );
				try
				{
					repo.setLocation( skipURLs.get( i ), false );
					SVNDirEntry props = repo.info( "", -1 );
					message.append( "<b>author</b>: " + props.getAuthor() + "<p>" );
				}
				catch ( SVNException e )
				{
					RequestLogger.printLine( "Something went wrong fetching SVN info" );
				}
			}
			//message.append( "<br>SVN info:<p>" );

			try
			{
				SVNURL root = repo.getRepositoryRoot( false );

				message.append( "<br><b>repository url</b>:" + root.getPath() + "<p>" );
			}
			catch ( SVNException e )
			{
				RequestLogger.printLine( "Something went wrong fetching SVN info" );
			}
			message.append( "<br><b>Only click yes if you trust the author.</b>" +
					"<p>Clicking no will stop the files from being added locally. (until you checkout the project again)" );
			if ( JOptionPane.showConfirmDialog( null, message, "SVN wants to add new files", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
			{
				skipFiles.clear();
			}
		}

		return skipFiles;
	}

	private static void doPush( File file, String relpath )
	{
		File rebase = new File( KoLConstants.ROOT_LOCATION, relpath );
		rebase.getParentFile().mkdirs();

		RequestLogger.printLine( file.getName() + " => " + rebase.getPath() );
		FileUtilities.copyFile( file, rebase );
	}
	
	private static void doDelete( File file, String relpath )
	{
		File rebase = new File( KoLConstants.ROOT_LOCATION, relpath );
		if ( rebase.exists() )
		{
			if ( rebase.delete() )
				RequestLogger.printLine( relpath + " => DELETED" );
		}
	}

	private static boolean shouldPush( SVNFileEvent event, boolean wasCheckout, String relpath )
	{
		if ( wasCheckout )
			return true;

		if ( event.getEvent().getAction() == SVNEventAction.UPDATE_ADD ) // new file added to repo, user said it was okay
			return true;

		if ( event.getEvent().getAction() == SVNEventAction.UPDATE_UPDATE )
		{
			SVNStatusType status = event.getEvent().getContentsStatus();

			// only push updated files if the file still exists in the mafia subdirectory
			if ( status == SVNStatusType.MERGED )
				return rebaseExists( relpath );
			if ( status == SVNStatusType.CHANGED )
				return rebaseExists( relpath );
			if ( status == SVNStatusType.CONFLICTED )
				return false;
		}

		// deletion handling is separate from the "push" handling
		if ( event.getEvent().getAction() == SVNEventAction.UPDATE_DELETE )
			return false;

		// probably shouldn't get here.
		RequestLogger.printLine( "unhandled SVN event: " + event.getEvent() + "; please report this." );

		return false;
	}
	
	private static boolean shouldDelete( SVNFileEvent event )
	{
		if ( event.getEvent().getAction() == SVNEventAction.UPDATE_DELETE )
			return true;

		return false;
	}

	private static boolean rebaseExists( String relpath )
	{
		File rebase = new File( KoLConstants.ROOT_LOCATION, relpath );
		return rebase.exists();
	}

	/**
	 * We need to reproducibly format a unique identifier for a given project; there can be multiple projects for a
	 * given repo.
	 * <p>
	 * We hardcode a regex to handle sourceforge (in future, possibly other) URLS. We want to turn
	 * https://svn.code.sf.net/p/mafiasvntest/code/myvalidproject1/ into "mafiasvntest-myvalidproject1". Likewise
	 * https://svn.code.sf.net/p/mafiasvntest/code/trunk/branchA/myvalidproject1/ becomes
	 * "mafiasvntest-trunk-branchA-myvalidproject1".
	 * <p>
	 * If the regex fails to match, we fall back and get the SVN repo UUID. This means that checking out multiple
	 * projects will fail (since we can't put multiple working copies in one directory). Still, better than nothing.
	 * 
	 * @param repo the repo to get a unique folder name for
	 * @return a unique folder ID for a given repo URL
	 */

	private static String getFolderUUID( SVNURL repo )
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		String UUID = null;
		// first, make sure the repo is there.
		try
		{
			UUID = SVNRepositoryFactory.create( repo ).getRepositoryUUID( false );
		}
		catch ( SVNException e )
		{
			RequestLogger.printLine( "Unable to connect with repository at " + repo.getPath() );
			return null;
		}

		// repo exists.  Try sourceforge regex.

		Matcher m = SOURCEFORGE_PATTERN.matcher( repo.getPath() );

		if ( m.find() )
		{
			// replace awful SVN UUID with nicely-formatted string derived from URL
			UUID = m.group( 1 ) + StringUtilities.globalStringReplace( m.group( 2 ), "/", "-" );
		}

		return UUID;
	}

	private static File doDirSetup( String uuid )
	{
		File makeDir = new File( KoLConstants.SVN_LOCATION, uuid );

		if ( !makeDir.mkdirs() // if we successfully make the directory, ok
			&&
			!makeDir.exists() ) // if it already exists, ok
			return null; // else punt

		return makeDir;
	}

	/**
	 * Accessory method to queue up a local file copy event.
	 * 
	 * @param event
	 */
	public static void queueFileEvent( SVNFileEvent event )
	{
		SVNManager.eventStack.add( event );
	}

	public static void doUpdate()
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		File[] projects = KoLConstants.SVN_LOCATION.listFiles();

		if ( projects == null || projects.length == 0 )
		{
			RequestLogger.printLine( "No projects currently installed with SVN." );
			return;
		}

		for ( File f : projects )
		{
			SVNInfo info;
			try
			{
				info = ourClientManager.getWCClient().doInfo( f, SVNRevision.HEAD );
			}
			catch ( SVNException e )
			{
				RequestLogger.printLine( "SVN ERROR during update operation.  Aborting..." );
				e.printStackTrace();
				return;
			}

			RequestLogger.printLine( "Updating " + info.getURL().getPath() );

			if ( SVNManager.validateRepo( info.getURL() ) )
			{
				RequestLogger.printLine( "Installed project repo failed validation.  Complain to the script's author.  Skipping." );
				continue;
			}
			try
			{
				SVNManager.update( f, SVNRevision.HEAD, true );
			}
			catch ( SVNException e )
			{
				RequestLogger.printLine( "SVN ERROR during update operation.  Aborting..." );
				e.printStackTrace();
				return;
			}

			pushUpdates();
		}
	}

	/**
	 * Performs an <code>svn update</code> on one individual repo.
	 * 
	 * @param repo the <b>SVNURL</b> to update.
	 */
	public static void doUpdate( SVNURL repo )
	{
		String UUID = SVNManager.getFolderUUID( repo );

		if ( UUID == null )
		{
			return;
		}

		if ( SVNManager.validateRepo( repo ) )
		{
			RequestLogger.printLine( "repo at " + repo.getPath() + " did not pass validation.  Aborting Checkout." );
			return;
		}

		File WCDir = SVNManager.doDirSetup( UUID );
		if ( WCDir == null )
		{
			RequestLogger.printLine( "Something went wrong creating directories..." );
			return;
		}

		try
		{
			SVNManager.update( WCDir, SVNRevision.HEAD, true );
		}
		catch ( SVNException e )
		{
			RequestLogger.printLine( "SVN ERROR during update operation.  Aborting..." );
			StaticEntity.printStackTrace( e );
			return;
		}

		pushUpdates();
	}

	public static void deleteInstalledProject( String p )
	{
		File project = new File( KoLConstants.SVN_LOCATION, p );

		if ( !project.exists() )
		{
			return;
		}

		RequestLogger.printLine("Uninstalling project...");
		recursiveDelete( project );
	}
	
	private static void recursiveDelete( File f )
	{
		if ( f.isDirectory() )
		{
			for ( File c : f.listFiles() )
				recursiveDelete( c );
		}
		// findDepth doesn't know how to find the depth of the project folder itself, so don't try.  We don't need to rebase-delete it anyway.
		if ( !f.getParentFile().equals( KoLConstants.SVN_LOCATION ) )
		{
			File fDepth = findDepth( f, true );
			if ( fDepth == null )
			{
				return;
			}

			String relpath = FileUtilities.getRelativePath( fDepth.getParentFile(), f );
			if ( !relpath.startsWith( "." ) ) // do not try to delete the rebase of hidden folders such as .svn!
			{
				File rebase = new File( KoLConstants.ROOT_LOCATION, relpath );
				if ( rebase.exists() )
				{
					if ( rebase.delete() )
						RequestLogger.printLine( relpath + " => DELETED" );
				}
			}
		}
		f.delete();
	}

	// some functions taken/adapted from http://wiki.svnkit.com/Managing_A_Working_Copy
	// there are a number of other examples there.
}