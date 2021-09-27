package net.sourceforge.kolmafia.svn;

import java.io.File;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class CleanupRunnable
	implements Runnable
{
	public void run()
	{
		if ( !KoLmafia.permitsContinue() )
			return;

		KoLmafia.updateDisplay( "Performing Cleanup..." );

		SVNClientManager manager = SVNManager.getClientManager();
		SVNWCClient wcClient = manager.getWCClient();
		wcClient.setIgnoreExternals( false );
		
		File[] projects = KoLConstants.SVN_LOCATION.listFiles();

		if ( !KoLmafia.permitsContinue() )
			return;

		try
		{
			SVNManager.SVN_LOCK.lock();

			for ( File project: projects )
			{
				wcClient.doCleanup( project );
			}
		}
		catch ( SVNException e )
		{
			error( e, "SVN ERROR.  Aborting..." );
			return;
		}
		finally
		{
			SVNManager.SVN_LOCK.unlock();
		}

		RequestLogger.printLine();
		RequestLogger.printLine( "Successfully performed cleanup." );
	}

	private static void error( SVNException e, String addMessage )
	{
		RequestLogger.printLine( e.getErrorMessage().getMessage() );
		if ( addMessage != null )
			KoLmafia.updateDisplay( MafiaState.ERROR, addMessage );
	}
}
