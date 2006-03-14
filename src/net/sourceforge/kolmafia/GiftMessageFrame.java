/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import java.awt.Component;
import javax.swing.JComboBox;
import net.java.dev.spellcast.utilities.LockableListModel;

public class GiftMessageFrame extends SendMessageFrame
{
	private static final String [] HEADERS = { "Message on the outside:", "Message on the inside:" };
	private static final String [] WEST_HEADERS = { "Desired package:" };

	private JComboBox packageSelect;

	public GiftMessageFrame( KoLmafia client )
	{	this( client, "" );
	}

	public GiftMessageFrame( KoLmafia client, String recipient )
	{	super( client, "Send a Purple Message", recipient );
	}

	public void dispose()
	{
		GiftMessageRequest.PACKAGES.removeListDataListener( packageSelect );

		packageSelect = null;
		super.dispose();
	}

	protected String [] getEntryHeaders()
	{	return HEADERS;
	}

	protected String [] getWestHeaders()
	{	return WEST_HEADERS;
	}

	protected Component [] getWestComponents()
	{
		// Which packages are available depends on ascension count.
		// You start with two packages and receive an additional
		// package every three ascensions you complete.

		int ascensions = KoLCharacter.getAscensions();
		int count = Math.min( ascensions / 3 + 2, 11 );
		LockableListModel packages = new LockableListModel();

		for ( int i = 0; i < count; ++i )
			packages.add( GiftMessageRequest.PACKAGES.get( i ) );

		packageSelect = new JComboBox( packages );
		Component [] westComponents = new Component[1];
		westComponents[0] = packageSelect;
		return westComponents;
	}

	protected boolean sendMessage( String recipient, String [] messages )
	{
		GiftMessageFrame.this.setEnabled( false );
		(new GiftMessageRequest( client, recipient, messages[0], messages[1], packageSelect.getSelectedItem(), getAttachedItems(), getAttachedMeat() )).run();
		GiftMessageFrame.this.setEnabled( true );

		if ( client.permitsContinue() )
		{
			DEFAULT_SHELL.updateDisplay( "Gift sent to " + recipient );
			setTitle( "Gift sent to " + recipient );
			return true;
		}
		else
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Failed to send gift to " + recipient );
			setTitle( "Failed to send gift to " + recipient );
			return false;
		}
	}

	/**
	 * Main class used to view the user interface without having to actually
	 * start the program.  Used primarily for user interface testing.
	 */

	public static void main( String [] args )
	{	(new CreateFrameRunnable( GiftMessageFrame.class )).run();
	}
}
