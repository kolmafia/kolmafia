package net.sourceforge.kolmafia.svn;

import java.io.File;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class CheckoutRunnable
	implements Runnable
{
	private final SVNURL repo;

	public CheckoutRunnable( SVNURL url )
	{
		this.repo = url;
	}

	public void run()
	{
		if ( !KoLmafia.permitsContinue() )
			return;

		String UUID = SVNManager.getFolderUUID( repo );

		if ( UUID == null )
		{
			return;
		}

		KoLmafia.updateDisplay( "Starting Checkout..." );

		if ( SVNManager.validateRepo( repo ) )
		{
			return;
		}

		File WCDir = SVNManager.doDirSetup( UUID );
		if ( WCDir == null )
		{
			RequestLogger.printLine( MafiaState.ERROR, "Something went wrong creating directories..." );
			return;
		}

		if ( !KoLmafia.permitsContinue() )
			return;

		try
		{
			SVNManager.SVN_LOCK.lock();
			SVNManager.checkout( repo, SVNRevision.HEAD, WCDir, true );
		}
		catch ( SVNException e )
		{
			error( e, "SVN ERROR during checkout operation.  Aborting..." );
			return;
		}
		finally
		{
			SVNManager.SVN_LOCK.unlock();
		}

		RequestLogger.printLine();
		RequestLogger.printLine( "Successfully checked out working copy." );
	}

	private static void error( SVNException e, String addMessage )
	{
		RequestLogger.printLine( e.getErrorMessage().getMessage() );
		if ( addMessage != null )
			KoLmafia.updateDisplay( MafiaState.ERROR, addMessage );
	}

}
