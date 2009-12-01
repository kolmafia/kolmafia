/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

package net.sourceforge.kolmafia.chat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public class ChatPoller
	extends Thread
{
	private static final LinkedList chatHistoryEntries = new RollingLinkedList( 5 );

	private static long localLastSeen = 0;
	private static long serverLastSeen = 0;

	private static final int CHAT_DELAY = 500;
	private static final int CHAT_DELAY_COUNT = 16;

	private static String rightClickMenu = "";

	public void run()
	{
		long lastSeen = 0;

		PauseObject pauser = new PauseObject();

		do
		{
			List entries = ChatPoller.getEntries( lastSeen );
			Iterator entryIterator = entries.iterator();

			while ( entryIterator.hasNext() )
			{
				HistoryEntry entry = (HistoryEntry) entryIterator.next();
				lastSeen = Math.max( lastSeen, entry.getLocalLastSeen() );
			}

			for ( int i = 0; i < ChatPoller.CHAT_DELAY_COUNT && ChatManager.isRunning(); ++i )
			{
				pauser.pause( ChatPoller.CHAT_DELAY );
			}
		}
		while ( ChatManager.isRunning() );
	}

	public synchronized static List getEntries( final long lastSeen )
	{
		if ( ChatManager.getCurrentChannel() == null )
		{
			ChatSender.sendMessage( "/listen" );
		}

		List newEntries = new ArrayList();

		Iterator entryIterator = ChatPoller.chatHistoryEntries.iterator();

		if ( lastSeen != 0 )
		{
			while ( entryIterator.hasNext() )
			{
				HistoryEntry entry = (HistoryEntry) entryIterator.next();
	
				if ( entry.getLocalLastSeen() > lastSeen )
				{
					newEntries.add( entry );
	
					while ( entryIterator.hasNext() )
					{
						entry = (HistoryEntry) entryIterator.next();
	
						newEntries.add( entry );
					}
				}
			}
		}

		ChatRequest request = null;

		request = new ChatRequest( ChatPoller.serverLastSeen );
		request.run();

		HistoryEntry entry = new HistoryEntry( request.responseText, ++ChatPoller.localLastSeen );
		ChatPoller.serverLastSeen = entry.getServerLastSeen();

		newEntries.add( entry );

		ChatPoller.chatHistoryEntries.add( entry );
		ChatManager.processMessages( entry.getChatMessages() );

		return newEntries;
	}

	public static final String getRightClickMenu()
	{
		if ( ChatPoller.rightClickMenu.equals( "" ) )
		{
			GenericRequest request = new GenericRequest( "lchat.php" );
			RequestThread.postRequest( request );

			int actionIndex = request.responseText.indexOf( "actions = {" );
			if ( actionIndex != -1 )
			{
				ChatPoller.rightClickMenu =
					request.responseText.substring( actionIndex, request.responseText.indexOf( ";", actionIndex ) + 1 );
			}
		}

		return ChatPoller.rightClickMenu;
	}
}
