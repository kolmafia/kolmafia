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

	public UpdateRunnable( SVNURL url, SVNRevision rev )
	{
		this.repo = url;
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
				if ( !SVNWCUtil.isWorkingCopyRoot( new File( KoLConstants.SVN_DIRECTORY, UUID ) ) )
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
				this.repo = SVNManager.getClientManager().getStatusClient().doStatus( this.WCDir, false ).getURL();
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
