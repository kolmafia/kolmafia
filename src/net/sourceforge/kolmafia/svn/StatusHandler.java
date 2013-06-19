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

import net.sourceforge.kolmafia.RequestLogger;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;

public class StatusHandler
	implements ISVNStatusHandler, ISVNEventHandler
{
	/*
	 * This is an implementation of ISVNStatusHandler.handleStatus(SVNStatus status)
	 */
	public void handleStatus( SVNStatus status )
	{
		/*
		 * Gets the status of file/directory/symbolic link text contents. It is SVNStatusType who contains information
		 * on the state of an item.
		 */
		SVNStatusType contentsStatus = status.getContentsStatus();

		if ( contentsStatus == SVNStatusType.STATUS_MODIFIED )
		{
			/*
			 * The contents of the file have been Modified.
			 */
			RequestLogger.printLine( "M " + status.getFile().getPath() );
			SVNManager.queueFileEvent( new SVNFileEvent( status.getFile(), null ) );
		}
		else if ( contentsStatus == SVNStatusType.STATUS_CONFLICTED )
		{
			/*
			 * The file item is in a state of Conflict. That is, changes received from the server during an update
			 * overlap with local changes the user has in his working copy.
			 */
			RequestLogger.printLine( "C " + status.getFile().getPath() );
			RequestLogger.printLine( "<font color=\"red\">Note: conflict must be resolved manually.</font>" );
		}
	}

	/*
	 * This is an implementation for ISVNEventHandler.handleEvent(SVNEvent event, double progress)
	 */
	public void handleEvent( SVNEvent event, double progress )
	{
		/*
		 * Gets the current action. An action is represented by SVNEventAction. In case of a status operation a current
		 * action can be determined via SVNEvent.getAction() and SVNEventAction.STATUS_-like constants.
		 */
		SVNEventAction action = event.getAction();
		/*
		 * Print out the revision against which the status was performed. This event is dispatched when the
		 * SVNStatusClient.doStatus() was invoked with the flag remote set to true - that is for a local status it won't
		 * be dispatched.
		 */
		if ( action == SVNEventAction.STATUS_COMPLETED )
		{
			RequestLogger.printLine( "Status against revision:  " + event.getRevision() );
		}

	}

	/*
	 * Should be implemented to check if the current operation is cancelled. If it is, this method should throw an
	 * SVNCancelException.
	 */
	public void checkCancelled()
		throws SVNCancelException
	{

	}

}