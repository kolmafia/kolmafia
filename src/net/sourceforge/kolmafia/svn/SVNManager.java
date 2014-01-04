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

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
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
	static final Lock SVN_LOCK = new ReentrantLock();

	private static final int RETRY_LIMIT = 3;
	private static final int DEPENDENCY_RECURSION_LIMIT = 5;

	private static Stack<SVNFileEvent> eventStack = new Stack<SVNFileEvent>();
	private static TreeMap<File, Long[]> updateMessages = new TreeMap<File, Long[]>();

	private static SVNClientManager ourClientManager;
	//private static ISVNEventHandler myCommitEventHandler;
	private static ISVNEventHandler myUpdateEventHandler;
	private static ISVNEventHandler myWCEventHandler;

	private static Pattern SOURCEFORGE_PATTERN = Pattern.compile( "/p/(.*?)/code(.*)", Pattern.DOTALL );
	private static Pattern GOOGLECODE_HOST_PATTERN = Pattern.compile( "([^\\.]+)\\.googlecode\\.com", Pattern.DOTALL );
	private static List<String> permissibles = Arrays.asList( "scripts", "data", "images", "relay", "ccs", "planting" );

	/**
	 * Initializes the library to work with a repository via different protocols.
	 */
	public synchronized static void setupLibrary()
	{
		if ( ourClientManager != null )
			return;

		/*
		 * For using over http:// and https://
		 */
		DAVRepositoryFactory.setup();
		/*
		 * For using over svn:// and svn+xxx://
		 */
		SVNRepositoryFactoryImpl.setup();

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


	/**
	 * Meant to be called before any operation that interacts with a remote repository. Prepares client manager and
	 * cleans up static variables in case they were not cleaned up in the previous operation.
	 */
	private static void initialize()
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		eventStack.clear();
		updateMessages.clear();
	}

	/*
	 * Creates a new version controlled directory (doesn't create any intermediate directories) right in a repository.
	 * Like 'svn mkdir URL -m "some comment"' command. It's done by invoking SVNCommitClient.doMkDir(SVNURL[] urls,
	 * String commitMessage) which takes the following parameters: urls - an array of URLs that are to be created;
	 * commitMessage - a commit log message since a URL-based directory creation is immediately committed to a
	 * repository.
	 */
/*	private static SVNCommitInfo makeDirectory( SVNURL url, String commitMessage )
		throws SVNException
	{
		
		 * Returns SVNCommitInfo containing information on the new revision committed (revision number, etc.)
		 
		return ourClientManager.getCommitClient().doMkDir( new SVNURL[]
		{ url
		}, commitMessage );
	}*/

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
/*	private static SVNCommitInfo importDirectory( File localPath, SVNURL dstURL, String commitMessage,
		boolean isRecursive )
		throws SVNException
	{
		
		 * Returns SVNCommitInfo containing information on the new revision committed (revision number, etc.)
		 
		return ourClientManager.getCommitClient().doImport( localPath, dstURL, commitMessage, isRecursive );

	}*/

	/*
	 * Committs changes in a working copy to a repository. Like 'svn commit PATH -m "some comment"' command. It's done
	 * by invoking SVNCommitClient.doCommit(File[] paths, boolean keepLocks, String commitMessage, boolean force,
	 * boolean recursive) which takes the following parameters: paths - working copy paths which changes are to be
	 * committed; keepLocks - if true then doCommit(..) won't unlock locked paths; otherwise they will be unlocked after
	 * a successful commit; commitMessage - a commit log message; force - if true then a non-recursive commit will be
	 * forced anyway; recursive - if true and a path corresponds to a directory then doCommit(..) recursively commits
	 * changes for the entire directory, otherwise - only for child entries of the directory;
	 */
/*	private static SVNCommitInfo commit( File wcPath, boolean keepLocks, String commitMessage )
		throws SVNException
	{
		
		 * Returns SVNCommitInfo containing information on the new revision committed (revision number, etc.)
		 
		return ourClientManager.getCommitClient().doCommit( new File[]
		{ wcPath
		}, keepLocks, commitMessage, false, true );
	}*/

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
		return updateClient.doCheckout( url, destPath, revision, revision, SVNDepth.fromRecurse( isRecursive ), false );
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
		return update( wcPath, updateToRevision, isRecursive, 0 );
	}

	private static long update( File wcPath, SVNRevision updateToRevision, boolean isRecursive, int retryCount )
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

		long rev = -1;
		try
		{
			rev = updateClient.doUpdate( wcPath, updateToRevision, SVNDepth.fromRecurse( isRecursive ), false, false );
		}
		catch ( SVNException e )
		{
			if ( e.getErrorMessage().getErrorCode() == SVNErrorCode.ATOMIC_INIT_FAILURE && retryCount <= RETRY_LIMIT )
			{
				retryCount++ ;
				// workaround for stupid sourceforge Apache bug
				RequestLogger.printLine( "Server-side error during svn update, retrying " + retryCount + " of " +
					RETRY_LIMIT );
				return update( wcPath, updateToRevision, isRecursive, retryCount );
			}
			else
				throw e;
		}
		return rev;
	}

	/*
	 * Updates a working copy to a different URL. Like 'svn switch URL' command. It's done by invoking
	 * SVNUpdateClient.doSwitch(File file, SVNURL url, SVNRevision revision, boolean recursive) which takes the
	 * following parameters: file - a working copy entry that is to be switched to a new url; url - a target URL a
	 * working copy is to be updated against; revision - a revision to which a working copy is to be updated; recursive
	 * - if true and an entry (file) is a directory then doSwitch(..) recursively switches the entire directory,
	 * otherwise - only child entries of the directory;
	 */
/*	private static long switchToURL( File wcPath, SVNURL url, SVNRevision updateToRevision, boolean isRecursive )
		throws SVNException
	{
		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		
		 * sets externals not to be ignored during the switch
		 
		updateClient.setIgnoreExternals( false );
		
		 * returns the number of the revision wcPath was updated to
		 
		return updateClient.doSwitch( wcPath, url, updateToRevision, isRecursive );
	}*/

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
/*	private static void showStatus( File wcPath, boolean isRecursive, boolean isRemote, boolean isReportAll,
		boolean isIncludeIgnored, boolean isCollectParentExternals )
		throws SVNException
	{
		
		 * StatusHandler displays status information for each entry in the console (in the manner of the native
		 * Subversion command line client)
		 
		ourClientManager.getStatusClient().doStatus( wcPath, isRecursive, isRemote, isReportAll, isIncludeIgnored, isCollectParentExternals, new StatusHandler(
			isRemote ) );
	}*/

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
	public static void showInfo( File wcPath, SVNRevision revision )
		throws SVNException
	{
		/*
		 * InfoHandler displays information for each entry in the console (in the manner of the native Subversion
		 * command line client)
		 */
		getClientManager().getWCClient().doInfo( wcPath, SVNRevision.UNDEFINED, revision, SVNDepth.INFINITY, null, new InfoHandler() );
	}

	/**
	 * A wrapper for doInfo so that callers do not have to handle the client manager (or interface with the SVN_LOCK).
	 */
	public static SVNInfo doInfo( File projectFile )
		throws SVNException
	{
		try
		{
			SVN_LOCK.lock();
			return getClientManager().getWCClient().doInfo( projectFile, SVNRevision.WORKING );
		}
		finally
		{
			SVN_LOCK.unlock();
		}
	}

	public static void showCommitMessage( File wcPath, long from, long to )
		throws SVNException
	{
		if ( !KoLmafia.permitsContinue() )
			return;

		// we don't want to show the commit info from the revision that we're on if we're moving to another revision.
		// example: going from r13 -> r15 : we don't want to show r13's info.
		if ( from < to )
			++from;

		// alternately, if we're decreasing in revision, we don't want to show the revision that we came from.
		// example: going from r15 -> r14 : we don't want to show r15's info.
		if ( from > to )
			--from;

		getClientManager().getLogClient().doLog( new File[]{wcPath}, SVNRevision.create( from ),
			SVNRevision.create( to ), true, false, 10, new ISVNLogEntryHandler()
		{
			public void handleLogEntry( SVNLogEntry logEntry )
				throws SVNException
			{
				RequestLogger.printLine( "Commit <b>r" + logEntry.getRevision() + "<b>:" );
				RequestLogger.printLine( "Author: " + logEntry.getAuthor() );
				RequestLogger.printLine();
				RequestLogger.printLine( logEntry.getMessage() );
				RequestLogger.printLine( "------" );
			}
		} );
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
/*	private static void addEntry( File wcPath )
		throws SVNException
	{
		ourClientManager.getWCClient().doAdd( wcPath, false, false, false, true );
	}*/

	/*
	 * Locks working copy paths, so that no other user can commit changes to them. Like 'svn lock PATH' command. It's
	 * done by invoking SVNWCClient.doLock(File[] paths, boolean stealLock, String lockMessage) which takes the
	 * following parameters: paths - an array of local entries to be locked; stealLock - set to true to steal the lock
	 * from another user or working copy; lockMessage - an optional lock comment string.
	 */
/*	private static void lock( File wcPath, boolean isStealLock, String lockComment )
		throws SVNException
	{
		ourClientManager.getWCClient().doLock( new File[]
		{ wcPath
		}, isStealLock, lockComment );
	}*/

	/*
	 * Schedules directories and files for deletion from version control upon the next commit (locally). Like 'svn
	 * delete PATH' command. It's done by invoking SVNWCClient.doDelete(File path, boolean force, boolean dryRun) which
	 * takes the following parameters: path - an entry to be scheduled for deletion; force - a boolean flag which is set
	 * to true to force a deletion even if an entry has local modifications; dryRun - set to true not to delete an entry
	 * but to check if it can be deleted; if false - then it's a deletion itself.
	 */
/*	private static void delete( File wcPath, boolean force )
		throws SVNException
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		ourClientManager.getWCClient().doDelete( wcPath, force, false );
	}*/
	
	public static void doCleanup()
	{
		initialize();
		
		RequestThread.postRequest( new CleanupRunnable() );
	}

	public static void doCheckout( SVNURL repo )
	{
		initialize();

		RequestThread.postRequest( new CheckoutRunnable( repo ) );

		pushUpdates( true );

		if ( Preferences.getBoolean( "svnInstallDependencies" ) )
			checkDependencies();
	}

	private static void showCommitMessages()
	{
		for ( File f : updateMessages.keySet() )
		{
			if ( updateMessages.get( f ) == null || updateMessages.get( f )[0] <= 0 )
			{
				continue;
			}

			RequestLogger.printLine( "Update log for <b>" + f.getName() + "</b>:" );
			RequestLogger.printLine( "------" );
			try
			{
				SVN_LOCK.lock();
				showCommitMessage( f, updateMessages.get( f )[ 0 ], updateMessages.get( f )[ 1 ] );
			}
			catch ( SVNException e )
			{
				error( e );
			}
			finally
			{
				SVN_LOCK.unlock();
			}
		}
		updateMessages.clear();
	}

	/**
	 * There are only 5 permissible files in the top level of the repo, all of them directories: scripts/, data/, ccs/,
	 * images/, and relay/. Any other files (directories or otherwise) in the top-level fails validation. 
	 * TODO: check
	 * file extensions, reject naughty ones
	 */

	static boolean validateRepo( SVNURL repo )
	{
		return validateRepo( repo, false );
	}

	private static boolean validateRepo( SVNURL repo, boolean quiet )
	{
		SVNRepository repository;
		Collection< ? > entries;
		boolean failed = false;

		try
		{
			SVN_LOCK.lock();
			repository = getClientManager().createRepository( repo, true );
		}
		catch ( SVNException e )
		{
			if ( !quiet )
				error( e, "Unable to connect with repository at " + repo.getPath() );
			return true;
		}
		finally
		{
			SVN_LOCK.unlock();
		}
		if ( !quiet )
			RequestLogger.printLine( "Validating repo..." );

		try
		{
			SVN_LOCK.lock();
			entries = repository.getDir( "", -1, null, (Collection< ? >) null ); // this syntax is stupid, by the way
		}
		catch ( SVNException e )
		{
			if ( !quiet )
				error( e, "Something went wrong while fetching svn directory info" );
			return true;
		}
		finally
		{
			SVN_LOCK.unlock();
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
				// something other than a directory
				// we allow a single top-level file to declare dependencies, nothing else
				failed = !entry.getName().equals( "dependencies.txt" );
		}

		if ( failed && !quiet )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "The requested repo failed validation.  Complain to the script's author." );
		}
		else
		{
			if ( !quiet )
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
		if ( eventStack.isEmpty() )
		{
			RequestLogger.printLine( "Done." );
			return;
		}

		// make a copy of the event stack - we'll need this later for the optional info step
		@SuppressWarnings( "unchecked" )
		Stack<SVNFileEvent> eventStackCopy = (Stack<SVNFileEvent>) SVNManager.eventStack.clone();

		List<String> pathsToSkip = doFinalChecks( wasCheckout );
		if ( pathsToSkip.size() > 0 )
		{
			RequestLogger.printLine( "NOTE: Skipping some updates due to user request." );
		}

		RequestLogger.printLine( "Pushing local updates..." );

		while ( !eventStack.isEmpty() )
		{
			SVNFileEvent event = eventStack.pop();
			if ( event.getFile().isDirectory() )
			{
				continue; // directories will be generated by mkdirs(), no need to explicitly create them
			}

			if ( event.getFile().getParentFile().getParentFile().equals( KoLConstants.SVN_LOCATION ) )
			{
				continue; // no top-level files get pushed - including dependencies.txt
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
			else if ( shouldDelete( event ) )
			{
				doDelete( event.getFile(), relpath );
			}
		}

		RequestLogger.printLine( "Done." );
		eventStack.clear();

		// use the copy of the event stack that we made earlier to show commit info
		// no need to try to show commit messages for checkouts
		if ( !wasCheckout )
			queueCommitMessages( eventStackCopy );
	}

	private static void queueCommitMessages( Stack<SVNFileEvent> eventStackCopy )
	{
		if ( eventStackCopy.isEmpty() )
			return;

		if ( !Preferences.getBoolean( "svnShowCommitMessages" ) )
		{
			SVNManager.updateMessages.clear();
			return;
		}
		/*
		 * We need to turn this stack of files and events into {workingCopy, revision[]} pairs, where workingCopy = a
		 * File that represents the root of the working copy that was updated, and revision[] = two Long that represent
		 * the revision that we started at and the revision that we're going to. In other words, we need to collapse all
		 * of the individual file events into one object per working copy.
		 */

		TreeMap<File, Long[]> feMap = new TreeMap<File, Long[]>();

		while ( !eventStackCopy.isEmpty() )
		{
			SVNFileEvent fe = eventStackCopy.pop();
			File f = fe.getFile();

			while ( !f.getParentFile().equals( KoLConstants.SVN_LOCATION ) )
			{
				f = f.getParentFile();
				if ( f == null )
					// shouldn't happen, punt
					return;
			}

			// some file events can be "add" events where from is -1
			// "delete" events will also have to == -1
			long from = fe.getEvent().getPreviousRevision();
			long to = fe.getEvent().getRevision();
			if ( fe.getEvent().getAction().equals( SVNEventAction.UPDATE_ADD ) ||
				fe.getEvent().getAction().equals( SVNEventAction.UPDATE_DELETE ) )
			{
				// just get the notes from the revision instead of everything between -1 and the revision
				if ( from == -1 && to != -1)
					from = to;
				else if ( to == -1 && from != -1 )
					to = from;
				else
					continue;
			}

			// assume that getPreviousRevision is the same for every file
			feMap.put( f, new Long[]
			{ from, to
			} );
		}

		SVNManager.updateMessages.putAll( feMap );
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
		if ( wasCheckout )
		{
			return checkExisting();
		}

		List<String> skipFiles = new ArrayList<String>();
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
			if ( event.getFile().getParentFile().getParentFile().equals( KoLConstants.SVN_LOCATION ) )
			{
				continue; // no top-level files get pushed - including dependencies.txt
			}

			File fDepth = findDepth( event.getFile().getParentFile() );
			if ( fDepth == null )
			{
				//shouldn't happen, punt
				return skipFiles;
			}

			// We only want to prompt if the file is new to the working copy (SVNEventAction.UPDATE_ADD) 
			// as this most likely means that it was recently added to the repo
			// SVNEventAction.ADD is UPDATE_ADD for binary files
			if ( event.getEvent().getAction() == SVNEventAction.UPDATE_ADD ||
				event.getEvent().getAction() == SVNEventAction.ADD )
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
				SVN_LOCK.lock();
				repo = getClientManager().createRepository( skipURLs.get( 0 ), true );
			}
			catch ( SVNException e )
			{
				error( e, "Something went wrong fetching SVN info" );
				//punt, NPE ensues if we continue with this method without initializing repo
				return skipFiles;
			}
			finally
			{
				SVN_LOCK.unlock();
			}

			StringBuilder message = new StringBuilder( "<html>New file(s) requesting confirmation:<p>" );
			int extra = 0;
			for ( int i = 0; i < skipFiles.size(); ++i )
			{
				if ( i > 9 )
				{
					extra += 1;
					continue;
				}
				message.append( "<b>file</b>: " + skipFiles.get( i ) + "<p>" );
				try
				{
					SVN_LOCK.lock();
					repo.setLocation( skipURLs.get( i ), false );
					SVNDirEntry props = repo.info( "", -1 );
					if ( props == null || props.getAuthor() == null )
						message.append( "<b>author</b>: unknown<p>" );
					else
						message.append( "<b>author</b>: " + props.getAuthor() + "<p>" );
				}
				catch ( SVNException e )
				{
					error( e, "Something went wrong fetching SVN info" );
				}
				finally
				{
					SVN_LOCK.unlock();
				}
			}
			if ( extra > 0 )
			{
				message.append( "<b>and " + extra + " more...</b>" );
			}
			//message.append( "<br>SVN info:<p>" );

			try
			{
				SVN_LOCK.lock();
				SVNURL root = repo.getRepositoryRoot( false );

				message.append( "<br><b>repository url</b>:" + root.getPath() + "<p>" );
			}
			catch ( SVNException e )
			{
				error( e, "Something went wrong fetching SVN info" );
			}
			finally
			{
				SVN_LOCK.unlock();
			}
			message.append( "<br><b>Only click yes if you trust the author.</b>"
				+ "<p>Clicking no will stop the files from being added locally. (until you checkout the project again)" );
			if ( JOptionPane.showConfirmDialog( null, message, "SVN wants to add new files", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
			{
				skipFiles.clear();
			}
		}

		return skipFiles;
	}

	/**
	 * When a user does svn checkout, he/she may not want project files to overwrite existing local files. Therefore,
	 * warn if local files exist.
	 * 
	 * @return a <b>List</b> of relpaths to ignore
	 */
	private static List<String> checkExisting()
	{
		List<String> skipFiles = new ArrayList<String>();
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
			if ( event.getEvent().getAction() != SVNEventAction.UPDATE_ADD )
			{
				continue; // we only care about ADD events
			}
			if ( event.getFile().getParentFile().getParentFile().equals( KoLConstants.SVN_LOCATION ) )
			{
				continue; // no top-level files get pushed - including dependencies.txt
			}

			File fDepth = findDepth( event.getFile().getParentFile() );
			if ( fDepth == null )
			{
				//shouldn't happen, punt
				return skipFiles;
			}

			String relpath = FileUtilities.getRelativePath( fDepth.getParentFile(), event.getFile() );
			File rebase = getRebase( relpath );

			if ( rebase == null )
				continue;

			// We only want to prompt if the file already exists locally
			if ( rebase.exists() )
			{
				skipFiles.add( relpath );
				skipURLs.add( event.getEvent().getURL() );
			}
		}

		if ( skipFiles.size() > 0 )
		{
			SVNRepository repo = null;
			try
			{
				SVN_LOCK.lock();
				repo = getClientManager().createRepository( skipURLs.get( 0 ), true );
			}
			catch ( SVNException e )
			{
				error( e, "Something went wrong fetching SVN info" );
				//punt, NPE ensues if we continue with this method without initializing repo
				return skipFiles;
			}
			finally
			{
				SVN_LOCK.unlock();
			}

			StringBuilder message = new StringBuilder( "<html>New file(s) will overwrite local files:<p>" );
			for ( int i = 0; i < skipFiles.size(); ++i )
			{
				File rebase = SVNManager.getRebase( skipFiles.get( i ) );
				String rerebase = FileUtilities.getRelativePath( KoLConstants.ROOT_LOCATION , rebase );

				message.append( "<b>file</b>: " + rerebase + "<p>" );
				try
				{
					SVN_LOCK.lock();
					repo.setLocation( skipURLs.get( i ), false );
					SVNDirEntry props = repo.info( "", -1 );
					message.append( "<b>author</b>: " + props.getAuthor() + "<p>" );
				}
				catch ( SVNException e )
				{
					error( e, "Something went wrong fetching SVN info" );
				}
				finally
				{
					SVN_LOCK.unlock();
				}
			}

			try
			{
				SVN_LOCK.lock();
				SVNURL root = repo.getRepositoryRoot( false );

				message.append( "<br><b>repository url</b>:" + root.getPath() + "<p>" );
			}
			catch ( SVNException e )
			{
				error( e, "Something went wrong fetching SVN info" );
			}
			finally
			{
				SVN_LOCK.unlock();
			}
			message.append( "<br>Checking out this project will result in some local files (described above) being overwritten."
				+ "<p>Click yes to overwrite them, no to skip installing them." );
			if ( JOptionPane.showConfirmDialog( null, message, "SVN checkout wants to overwrite local files", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
			{
				skipFiles.clear();
			}
		}

		return skipFiles;
	}

	private static void doPush( File file, String relpath )
	{
		File rebase = getRebase( relpath );

		if ( rebase == null )
			// this is okay; just make the file in its default location
			rebase = new File( KoLConstants.ROOT_LOCATION, relpath );

		rebase.getParentFile().mkdirs();

		RequestLogger.printLine( file.getName() + " => " + rebase.getPath() );
		FileUtilities.copyFile( file, rebase );
	}

	private static void doDelete( File file, String relpath )
	{
		File rebase = getRebase( relpath );

		if ( rebase == null )
			return;

		if ( rebase.exists() )
		{
			String rerebase = FileUtilities.getRelativePath( KoLConstants.ROOT_LOCATION , rebase );
			if ( rebase.delete() )
				RequestLogger.printLine( rerebase + " => DELETED" );
		}
	}

	private static boolean shouldPush( SVNFileEvent event, boolean wasCheckout, String relpath )
	{
		if ( wasCheckout )
			return true;

		if ( event.getEvent().getAction() == SVNEventAction.UPDATE_ADD ) // new text file added to repo, user said it was okay
			return true;

		if ( event.getEvent().getAction() == SVNEventAction.ADD ) // new binary file added to repo, user said it was okay
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

		List<File> matches = KoLmafiaCLI.findScriptFile( rebase.getName() );
		// data/ is not in the searchable namespace for findScriptFile, but we need it to be.
		// getRebase will still find it if it exists in the default location, so look for that.
		if ( relpath.startsWith( "data" ) )
		{
			if ( rebase.exists() )
				matches.add( rebase );
		}
		if ( matches.size() > 1 )
		{
			RequestLogger.printLine( "WARNING: too many matches for " + rebase.getName() +
				" in your namespace; no local files were updated." );
		}
		if ( matches.size() == 0 )
		{
			RequestLogger.printLine( "NOTE: no local file named " + rebase.getName() +
				" in your namespace; no updates performed for this file." );
		}
		return matches.size() == 1;
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

	static String getFolderUUID( SVNURL repo )
	{
		String remote = null;
		// first, make sure the repo is there.
		try
		{
			SVN_LOCK.lock();
			remote = getClientManager().createRepository( repo, true ).getRepositoryUUID( false );
		}
		catch ( SVNException e )
		{
			error( e, "Unable to connect with repository at " + repo.getPath() );
			return null;
		}
		finally
		{
			SVN_LOCK.unlock();
		}

		String local = getFolderUUIDNoRemote( repo );
		return local != null ? local : remote;
	}

	private static String getFolderUUIDNoRemote( SVNURL repo )
	{
		String UUID = null;
		Matcher m = SOURCEFORGE_PATTERN.matcher( repo.getPath() );

		if ( m.find() )
		{
			// replace awful SVN UUID with nicely-formatted string derived from URL
			UUID = StringUtilities.globalStringReplace( m.group( 1 ) + m.group( 2 ), "/", "-" );
		}
		else
		{
			// try googlecode regex.
			m = GOOGLECODE_HOST_PATTERN.matcher( repo.getHost() );

			if ( m.find() )
			{
				UUID = m.group( 1 ) + StringUtilities.globalStringReplace( repo.getPath().substring( 4 ), "/", "-" );
			}
		}
		return UUID;
	}

	static File doDirSetup( String uuid )
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
		final File[] projects = KoLConstants.SVN_LOCATION.listFiles();

		if ( projects == null || projects.length == 0 )
		{
			RequestLogger.printLine( "No projects currently installed with SVN." );
			return;
		}

		initialize();

		Runnable runMe = new Runnable()
		{
			public void run()
			{
				KoLmafia.updateDisplay( "Updating all SVN projects..." );
				for ( File f : projects )
				{
					if ( !KoLmafia.permitsContinue() )
						return;

					if ( Preferences.getBoolean( "simpleSvnUpdate" ) )
					{
						if ( WCAtHead( f ) )
							continue;
					}

					RequestThread.postRequest( new UpdateRunnable( f ) );

					pushUpdates();
				}
			}
		};

		RequestThread.postRequest( runMe );

		showCommitMessages();

		Preferences.setBoolean( "_svnUpdated", true );

		if ( Preferences.getBoolean( "syncAfterSvnUpdate" ) )
		{
			syncAll();
		}

		if ( Preferences.getBoolean( "svnInstallDependencies" ) )
			checkDependencies();
	}

	/**
	 * For users who just want "simple" update behavior, check if the revision of the project root and the repo root are
	 * the same.
	 * <p>
	 * Users who have used <code>svn switch</code> on some of their project should not use this.
	 * 
	 * @param f the working copy
	 * @return <code>true</code> if the working copy is at HEAD
	 */
	private static boolean WCAtHead( File f )
	{
		return WCAtHead( f, false );
	}

	/**
	 * For users who just want "simple" update behavior, check if the revision of the project root and the repo root are
	 * the same.
	 * <p>
	 * Users who have used <code>svn switch</code> on some of their project should not use this.
	 * 
	 * @param f the working copy
	 * @param quiet if <code>true</code>, suppresses RequestLogger output.
	 * @return <code>true</code> if the working copy is at HEAD
	 */
	public static boolean WCAtHead( File f, boolean quiet )
	{
		try
		{
			SVN_LOCK.lock();
			if ( !SVNWCUtil.isWorkingCopyRoot( f ) )
			{
				return false;
			}

			SVNInfo wcinfo = getClientManager().getWCClient().doInfo( f, SVNRevision.WORKING );
			long repoRev = getClientManager().createRepository( wcinfo.getURL(), true ).getLatestRevision();

			if ( wcinfo.getRevision().getNumber() == repoRev )
			{
				if ( !quiet )
					RequestLogger.printLine( wcinfo.getFile().getName() + " is at HEAD (r" + repoRev + ")" );
				return true;
			}
		}
		catch ( SVNException e )
		{
			error( e );
			return true; // don't continue updating this project if there's an error here.
		}
		finally
		{
			SVN_LOCK.unlock();
		}

		return false;
	}

	/**
	 * Performs an <code>svn update</code> on a local working copy.
	 * 
	 * @param p the local working copy to update.
	 */
	public static void doUpdate( String p )
	{
		File project = new File( KoLConstants.SVN_LOCATION, p );

		if ( !project.exists() )
			return;

		initialize();

		RequestThread.postRequest( new UpdateRunnable( project ) );

		pushUpdates();
		showCommitMessages();

		if ( Preferences.getBoolean( "svnInstallDependencies" ) )
			checkDependencies();
	}

	/**
	 * Performs an <code>svn update</code> on one individual repo.
	 * 
	 * @param repo the <b>SVNURL</b> to update.
	 */
	public static void doUpdate( SVNURL repo )
	{
		initialize();

		RequestThread.postRequest( new UpdateRunnable( repo ) );

		pushUpdates();
		showCommitMessages();

		if ( Preferences.getBoolean( "svnInstallDependencies" ) )
			checkDependencies();
	}

	public static void deleteInstalledProject( String p )
	{
		final File project = new File( KoLConstants.SVN_LOCATION, p );

		if ( !project.exists() )
		{
			return;
		}

		RequestLogger.printLine( "Uninstalling project..." );
		recursiveDelete( project );
		if ( project.exists() )
		{
			// sometimes SVN daemon threads (like tsvncache) will have the lock for wc.db, causing delete to fail for that file (and therefore also the project directory).
			// dispatch a parallel thread that will wait for a little bit then re-try the delete.
			RequestThread.runInParallel( new Runnable()
			{
				public void run()
				{
					PauseObject p = new PauseObject();
					p.pause( 2000 );

					recursiveDelete( project );
				}
			});
		}
		RequestLogger.printLine( "Project uninstalled." );
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
				File rebase = getRebase( relpath );

				if ( rebase != null )
				{
					String rerebase = FileUtilities.getRelativePath( KoLConstants.ROOT_LOCATION , rebase );
					if ( rebase.delete() )
						RequestLogger.printLine( rerebase + " => DELETED" );
				}
			}
		}
		f.delete();
	}

	/**
	 * Move the working copy up or down <b>amount</b> revisions. <b>amount</b> can be negative.
	 * 
	 * @param p the name of the project to decrement
	 * @param amount the amount to increment/decrement
	 */

	public static void incrementProject( String p, int amount )
	{
		if ( amount == 0 )
			return;

		File project = new File( KoLConstants.SVN_LOCATION, p );

		if ( !project.exists() )
			return;

		initialize();

		try
		{
			SVN_LOCK.lock();
			long currentRev = getClientManager().getStatusClient().doStatus( project, false ).getRevision().getNumber();

			if ( currentRev + amount <= 0 )
			{
				RequestLogger.printLine( "At r" + currentRev + "; cannot decrement revision by " + amount + "." );
				return;
			}
			RequestLogger.printLine( ( ( amount > 0 ) ? "Incrementing" : "Decrementing" ) + " project " +
				project.getName() + " from r" + currentRev + " to r" + ( currentRev + amount ) );

			SVNManager.update( project, SVNRevision.create( currentRev + amount ), true );
		}
		catch ( SVNException e )
		{
			if ( e.getErrorMessage().getErrorCode().equals( SVNErrorCode.FS_NO_SUCH_REVISION ) )
			{
				RequestLogger.printLine( "SVN Error: no such revision.  Aborting..." );
				return;
			}
			error( e, "SVN ERROR during update operation.  Aborting..." );
			return;
		}
		finally
		{
			SVN_LOCK.unlock();
		}

		pushUpdates();
		showCommitMessages();
	}

	/**
	 * "sync" operations are for users who make edits to the working copy version of files (in svn/) and want those
	 * changes reflected in their local copy.
	 * <p>
	 * Sync first iterates through projects and builds a list of working copy files that are modified.
	 * <p>
	 * For files that are modified, it then compares the WC file against the local rebase. If the rebase is different,
	 * the WC file is copied over the rebase.
	 */
	public static void syncAll()
	{
		if ( !KoLmafia.permitsContinue() )
			return;

		File[] projects = KoLConstants.SVN_LOCATION.listFiles();

		if ( projects == null || projects.length == 0 )
		{
			// Nothing to do here.
			return;
		}

		initialize();

		RequestLogger.printLine( "Checking for working copy modifications..." );

		for ( File f : projects )
		{
			try
			{
				SVN_LOCK.lock();
				getClientManager().getStatusClient().doStatus( f, SVNRevision.UNDEFINED, SVNDepth.INFINITY, false, false, false, false, new StatusHandler(), null );
			}
			catch ( SVNException e )
			{
				error( e );
				return;
			}
			finally
			{
				SVN_LOCK.unlock();
			}
		}

		if ( eventStack.isEmpty() )
		{
			// nothing to do
			RequestLogger.printLine( "No modifications." );
			return;
		}

		RequestLogger.printLine( "Synchronizing with local copies..." );

		while ( !eventStack.isEmpty() )
		{
			File eventFile = eventStack.pop().getFile();

			if ( eventFile.isDirectory() )
			{
				continue; // directories are harmless
			}
			if ( eventFile.getParentFile().getParentFile().equals( KoLConstants.SVN_LOCATION ) )
			{
				continue; // no top-level files get pushed - including dependencies.txt
			}

			File fDepth = findDepth( eventFile.getParentFile() );
			if ( fDepth == null )
			{
				//shouldn't happen, punt
				eventStack.clear();
				return;
			}

			String relpath = FileUtilities.getRelativePath( fDepth.getParentFile(), eventFile );
			File rebase = getRebase( relpath );

			if ( rebase == null )
				continue;

			try
			{
				SVN_LOCK.lock();
				if ( rebaseExists( relpath ) && !SVNFileUtil.compareFiles( eventFile, rebase, null ) )
				{
					doPush( eventFile, relpath );
				}
			}
			catch ( SVNException e )
			{
				error( e );
				eventStack.clear();
				return;
			}
			finally
			{
				SVN_LOCK.unlock();
			}
		}

		RequestLogger.printLine( "Sync complete." );
	}

	private static File getRebase( String relpath )
	{
		File rebase = new File( KoLConstants.ROOT_LOCATION, relpath );

		// scripts/ and relay/ exist in the searchable namespace, so only search if we're looking there
		// the root location is also in the namespace, but svn isn't allowed to put stuff there, so ignore it
		if ( relpath.startsWith( "scripts" ) || relpath.startsWith( "relay" ) )
		{
			List<File> matches = KoLmafiaCLI.findScriptFile( rebase.getName() );

			if ( matches.size() == 1 )
				return matches.get( 0 );

			if ( matches.size() > 1 )
				return null;
		}

		// some directories are not searched by findScriptFile, but we can just check if the rebase exists in those cases
		if ( rebase.exists() )
			return rebase;

		return null;
	}

	private static void checkDependencies()
	{
		checkDependencies( 0 );
	}

	private static void checkDependencies( int recursionDepth )
	{
		if ( !KoLmafia.permitsContinue() )
			return;

		File[] projects = KoLConstants.SVN_LOCATION.listFiles();
		List<File> dependencyFiles = new ArrayList<File>();

		if ( projects == null || projects.length == 0 )
		{
			// Nothing to do here.
			return;
		}

		for ( File f : projects )
		{
			File dep = new File( f, "dependencies.txt" );

			if ( dep.exists() )
				dependencyFiles.add( dep );
		}

		if ( dependencyFiles.size() == 0 )
			return;

		// we have some dependencies to resolve.  We need to figure out what SVNURLs are already installed.
		initialize();

		Set<String> installed = new HashSet<String>();

		for ( File f : projects )
		{
			try
			{
				String uuid = getFolderUUIDNoRemote( SVNManager.getClientManager().getStatusClient().doStatus( f, false ).getURL() );
				if ( uuid == null )
					uuid = getFolderUUID( SVNManager.getClientManager().getStatusClient().doStatus( f, false ).getURL() );

				installed.add( uuid );
			}
			catch ( SVNException e )
			{
				// shouldn't happen, punt
				error( e );
				return;
			}
		}

		// Now we need to figure out the set of URLs specified by dependency files

		Set<SVNURL> dependencyURLs = new HashSet<SVNURL>();

		for ( File dep : dependencyFiles )
		{
			dependencyURLs.addAll( readDependencies( dep ) );
		}

		if ( dependencyURLs.size() == 0 )
			return;

		// now, see if there are any files in dependencyURLs that aren't yet installed
		Set<SVNURL> installMe = new HashSet<SVNURL>();

		for ( SVNURL url : dependencyURLs )
		{
			if ( url == null )
				continue;
			// to figure out if they are installed, compare what the UUID would be of both.
			// convert each dependencyURL to a UUID before comparing.

			String uuid = getFolderUUIDNoRemote( url );
			if ( uuid == null )
				uuid = getFolderUUID( url );

			if ( !installed.contains( uuid ) )
				installMe.add( url );
		}

		if ( installMe.size() == 0 )
			return;

		// before installing, we need to check that they're actually valid

		Iterator<SVNURL> it = installMe.iterator();
		while ( it.hasNext() )
		{
			SVNURL url = it.next();
			if ( validateRepo( url, true ) )
			{
				RequestLogger.printLine( "bogus dependency: " + url );
				it.remove();
			}
		}

		if ( installMe.size() == 0 )
			return;
		if ( !KoLmafia.permitsContinue() )
			return;

		// install them
		RequestLogger.printLine( "Installing " + installMe.size() + " new dependenc" +
			( installMe.size() == 1 ? "y." : "ies." ) );

		for ( SVNURL url : installMe )
		{
			RequestThread.postRequest( new CheckoutRunnable( url ) );
			pushUpdates( true );
		}

		if ( recursionDepth <= DEPENDENCY_RECURSION_LIMIT )
		{
			checkDependencies( ++recursionDepth );
		}
		else
		{
			RequestLogger.printLine( "Stopping dependency installation: Too Much Recursion.  Doing svn update will continue to resolve them if you want.");
		}
	}

	/**
	 * Reads a dependencies.txt text file and pulls out the URLs.
	 * <p>
	 * Output should be sanitized, with comments, bogus lines, and duplicate entries (obviously, since it's a set)
	 * removed.
	 * 
	 * @param dep the <code>File</code> that contains the dependencies
	 * @return a <code>Set</code> of <code>SVNURL</code>s that are dependencies
	 */
	private static Set<SVNURL> readDependencies( File dep )
	{
		BufferedReader reader = FileUtilities.getReader( dep );
		Set<SVNURL> depURLs = new HashSet<SVNURL>();
		try
		{
			String[] data;
			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				// turn it into an SVNURL
				try
				{
					depURLs.add( SVNURL.parseURIEncoded( data[ 0 ] ) );
				}
				catch ( SVNException e )
				{
					RequestLogger.printLine( "Bad line of data in " + dep + "; skipping this file." );
					depURLs.clear();
					break;
				}
			}
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}
		return depURLs;
	}

	private static void error( SVNException e )
	{
		error( e, null );
	}

	public static void error( SVNException e, String addMessage )
	{
		RequestLogger.printLine( e.getErrorMessage().getMessage() );
		if ( addMessage != null )
			KoLmafia.updateDisplay( MafiaState.ERROR, addMessage );
	}

	static SVNClientManager getClientManager()
	{
		if ( ourClientManager == null )
		{
			setupLibrary();
		}

		return ourClientManager;
	}

	// some functions taken/adapted from http://wiki.svnkit.com/Managing_A_Working_Copy
	// there are a number of other examples there.
}