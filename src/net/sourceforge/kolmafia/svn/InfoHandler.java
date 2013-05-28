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

import net.sourceforge.kolmafia.RequestLogger;

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;

/*
 * An implementation of ISVNInfoHandler that is used to 
 * display  info  on  a  working  copy path.  This implementation is passed  to
 * 
 * SVNWCClient.doInfo(File path, SVNRevision revision, boolean recursive, 
 * ISVNInfoHandler handler) 
 * 
 * For each item to be processed doInfo(..) collects information and creates an 
 * SVNInfo which keeps that information. Then  doInfo(..)  calls  implementor's 
 * handler.handleInfo(SVNInfo) where it passes the gathered info.
 */
public class InfoHandler
	implements ISVNInfoHandler
{
	/*
	 * This is an implementation of ISVNInfoHandler.handleInfo(SVNInfo info). Just prints out information on a Working
	 * Copy path in the manner of the native SVN command line client.
	 */

	public void handleInfo( SVNInfo info )
	{
		RequestLogger.printLine( "-----------------INFO-----------------" );
		RequestLogger.printLine( "Local Path: " + info.getFile().getPath() );
		RequestLogger.printLine( "URL: " + info.getURL() );
		if ( info.isRemote() && info.getRepositoryRootURL() != null )
		{
			RequestLogger.printLine( "Repository Root URL: " + info.getRepositoryRootURL() );
		}
		if ( info.getRepositoryUUID() != null )
		{
			RequestLogger.printLine( "Repository UUID: " + info.getRepositoryUUID() );
		}
		RequestLogger.printLine( "Revision: " + info.getRevision().getNumber() );
		RequestLogger.printLine( "Node Kind: " + info.getKind().toString() );
		if ( !info.isRemote() )
		{
			RequestLogger.printLine( "Schedule: " + ( info.getSchedule() != null ? info.getSchedule() : "normal" ) );
		}
		RequestLogger.printLine( "Last Changed Author: " + info.getAuthor() );
		RequestLogger.printLine( "Last Changed Revision: " + info.getCommittedRevision().getNumber() );
		RequestLogger.printLine( "Last Changed Date: " + info.getCommittedDate() );
		if ( info.getPropTime() != null )
		{
			RequestLogger.printLine( "Properties Last Updated: " + info.getPropTime() );
		}
		if ( info.getKind() == SVNNodeKind.FILE && info.getChecksum() != null )
		{
			if ( info.getTextTime() != null )
			{
				RequestLogger.printLine( "Text Last Updated: " + info.getTextTime() );
			}
			RequestLogger.printLine( "Checksum: " + info.getChecksum() );
		}
		if ( info.getLock() != null )
		{
			if ( info.getLock().getID() != null )
			{
				RequestLogger.printLine( "Lock Token: " + info.getLock().getID() );
			}
			RequestLogger.printLine( "Lock Owner: " + info.getLock().getOwner() );
			RequestLogger.printLine( "Lock Created: " + info.getLock().getCreationDate() );
			if ( info.getLock().getExpirationDate() != null )
			{
				RequestLogger.printLine( "Lock Expires: " + info.getLock().getExpirationDate() );
			}
			if ( info.getLock().getComment() != null )
			{
				RequestLogger.printLine( "Lock Comment: " + info.getLock().getComment() );
			}
		}
	}
}