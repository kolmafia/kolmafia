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

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.persistence.BuffBotDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.TransferItemRequest;

public class SendMessageCommand
	extends AbstractCommand
{
	public SendMessageCommand()
	{
		this.usage = " <item> [, <item>]... to <recipient> [ || <message> ] - send kmail";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( RecoveryManager.isRecoveryActive() || MoodManager.isExecuting() )
		{
			RequestLogger.printLine( "Send request \"" + parameters + "\" ignored in between-battle execution." );
			return;
		}

		SendMessageCommand.send( parameters, cmd.equals( "csend" ) );
	}

	public static void send( final String parameters, final boolean isConvertible )
	{
		String[] splitParameters = parameters.replaceFirst( "(?:^| )[tT][oO] ", " => " ).split( " => " );

		if ( splitParameters.length != 2 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid send request." );
			return;
		}

		String itemList = splitParameters[ 0 ];
		String recipient = splitParameters[ 1 ];

		String message = KoLConstants.DEFAULT_KMAIL;

		int separatorIndex = recipient.indexOf( "||" );

		if ( separatorIndex != -1 )
		{
			message = recipient.substring( separatorIndex + 2 ).trim();
			recipient = recipient.substring( 0, separatorIndex );
		}

		// String.split() is weird!  An empty string, split on commas, produces
		// a 1-element array containing an empty string.  However, a string
		// containing just one or more commas produces a 0-element array???!!!

		itemList = itemList.trim() + ",";
		itemList = itemList.trim();

		AdventureResult[] attachments = ItemFinder.getMatchingItemList( KoLConstants.inventory, itemList );

		if ( attachments.length == 0 && ( itemList.length() > 1 || message == KoLConstants.DEFAULT_KMAIL ) )
		{
			return;
		}

		int meatAmount = 0;
		ArrayList<AdventureResult> attachmentList = new ArrayList<AdventureResult>();

		for ( int i = 0; i < attachments.length; ++i )
		{
			AdventureResult attachment = attachments[ i ];
			if ( attachment.getName().equals( AdventureResult.MEAT ) )
			{
				meatAmount += attachment.getCount();
			}
			else
			{
				AdventureResult.addResultToList( attachmentList, attachment );
			}
		}

		if ( !isConvertible && meatAmount > 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Please use 'csend' if you need to transfer meat." );
			return;
		}

		// Validate their attachments.  If they happen to be
		// scripting a philanthropic buff request, then figure
		// out if there's a corresponding full-price buff.

		if ( meatAmount > 0 )
		{
			meatAmount = BuffBotDatabase.getOffering( recipient, meatAmount );
			AdventureResult.addResultToList( attachmentList, new AdventureResult( AdventureResult.MEAT, meatAmount ) );
		}

		AdventureResult[] items = new AdventureResult[ attachmentList.size() ];
		SendMessageCommand.send( recipient, message, (AdventureResult[])attachmentList.toArray( items ), false, true );
	}

	public static void send( final String recipient, final String message, final AdventureResult[] attachments,
				 final boolean usingStorage, final boolean isInternal )
	{
		if ( !usingStorage )
		{
			TransferItemRequest.setUpdateDisplayOnFailure( false );
			RequestThread.postRequest( new SendMailRequest( recipient, message, attachments, isInternal ) );
			TransferItemRequest.setUpdateDisplayOnFailure( true );

			if ( !TransferItemRequest.hadSendMessageFailure() )
			{
				KoLmafia.updateDisplay( "Message sent to " + recipient );
				return;
			}
		}

		List availablePackages = SendGiftRequest.getPackages();
		int desiredPackageIndex = Math.min( Math.min( availablePackages.size() - 1, attachments.length ), 5 );

		if ( HolidayDatabase.getHoliday().startsWith( "Valentine's" ) )
		{
			desiredPackageIndex = 0;
		}

		// Clear the error state for continuation on the
		// message sending attempt.

		if ( !KoLmafia.refusesContinue() )
		{
			KoLmafia.forceContinue();
		}

		RequestThread.postRequest( new SendGiftRequest(
			recipient, message, desiredPackageIndex, attachments, usingStorage ) );

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Gift sent to " + recipient );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to send message to " + recipient );
		}
	}
}
