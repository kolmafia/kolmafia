package net.sourceforge.kolmafia.svn;

import java.io.File;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class UpdateRunnable
	implements Runnable
{
	private SVNURL repo = null;
	private SVNRevision revision = null;
	private File WCDir = null;

	public UpdateRunnable( SVNURL url )
	{
		this.repo = url;
		this.revision = SVNRevision.HEAD;
	}

	public UpdateRunnable( File f )
	{
		this.WCDir = f;
		this.revision = SVNRevision.HEAD;
	}

	public UpdateRunnable( File f, SVNRevision rev )
	{
		this.WCDir = f;
		this.revision = rev;
	}

	public void run()
	{
		if ( this.WCDir == null )
		{
			if ( !KoLmafia.permitsContinue() )
				return;

			String UUID = SVNManager.getFolderUUID( this.repo );

			if ( UUID == null )
			{
				return;
			}

			// If we're updating it, make sure the project is already checked out first
			try
			{
				if ( !SVNWCUtil.isWorkingCopyRoot( new File( KoLConstants.SVN_LOCATION, UUID ) ) )
				{
					KoLmafia.updateDisplay( "No existing project named " + UUID +
						". Did you mean to do \"checkout\" instead of \"update\"?" );
					return;
				}
			}
			catch ( SVNException e )
			{
				// Shouldn't happen, print a stack trace
				StaticEntity.printStackTrace( e );
				return;
			}

			this.WCDir = SVNManager.doDirSetup( UUID );
			if ( this.WCDir == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Something went wrong creating directories..." );
				return;
			}
		}
		else
		// this.repo is null, we need to find it
		{
			if ( !KoLmafia.permitsContinue() )
				return;

			try
			{
				this.repo = SVNManager.workingCopyToSVNURL( this.WCDir );
			}
			catch ( SVNException e )
			{
				error( e );
				return;
			}
		}

		if ( SVNManager.validateRepo( this.repo ) )
		{
			return;
		}

		if ( !KoLmafia.permitsContinue() )
			return;

		KoLmafia.updateDisplay( "Updating " + this.WCDir.getName() + "..." );

		try
		{
			SVNManager.update( this.WCDir, this.revision, true );
		}
		catch ( SVNException e )
		{
			error( e );
			return;
		}
	}

	private static void error( SVNException e )
	{
		error( e, null );
	}

	private static void error( SVNException e, String addMessage )
	{
		RequestLogger.printLine( e.getErrorMessage().getMessage() );
		if ( addMessage != null )
			KoLmafia.updateDisplay( MafiaState.ERROR, addMessage );
	}
}
