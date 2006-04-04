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

import java.net.HttpURLConnection;

public class LocalRelayRequest extends KoLRequest
{
	protected String fullResponse;
	
	public LocalRelayRequest( KoLmafia client, String formURLString, boolean followRedirects )
	{	super( client, formURLString, followRedirects );
	}
	
	public HttpURLConnection getFormConnection()
	{	return formConnection;
	}
	
	public String getFullResponse()
	{	return fullResponse;
	}
	
	protected void processRawResponse()
	{
		super.processRawResponse();
		fullResponse = responseText;
		
		if ( getURLString().indexOf("compactmenu.php") != -1 )
		{
			// Change the function menu
			fullResponse = fullResponse.replaceAll(	"<option value=.?inventory.?php.?>Inventory</option>",
																							"<option value=\"inventory.php?which=1\">Consumables</option>\n" + 
																							"<option value=\"inventory.php\">Inventory</option>" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?inventory.?php.?>Inventory</option>", 
																							"<option value=\"inventory.php?which=2\">Equipment</option>\n" + 
																							"<option value=\"inventory.php\">Inventory</option>" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?inventory.?php.?>Inventory</option>", 
																							"<option value=\"inventory.php?which=3\">Miscellaneous</option>" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?questlog.?php.?>Quests</option>",
																							"<option value=\"familiar.php\">Terrarium</option>" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?messages.?php.?>Read Messages</option>",
																							"<option value=\"questlog.php?which=1\">Quest Log</option>\n" + 
																							"<option value=\"messages.php\">Read Messages</option>" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?documentation.?>Documentation</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?forums.?>Forums</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?radio.?>Radio</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?sendmessage.*?</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?store.?>Store</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?logout.?php.?>Log Out</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?donate.?>Donate</option>", "" );

			// Mafiatize the goto menu
			fullResponse = fullResponse.replaceAll(	"<option value=.?mall.?php.*?</option>", "" );
			fullResponse = fullResponse.replaceAll(	"<option value=.?mountains.?php.?>",
																							"<option value=\"mclargehuge.php\">Mt. McLargeHuge</option>\n" + 
																							"<option value=\"mountains.php\">" );
			fullResponse = fullResponse.replaceAll(	"Nearby Plains</a>",
																							"Nearby Plains</a>\n" +
																							"<option value=\"beanstalk.php\">Above Beanstalk</option>\n" );
		}
		// Add [refresh] link to charpane.php (may remove this later)
		if ( getURLString().indexOf("charpane.php") != -1 )
			fullResponse = fullResponse.replaceAll( "<centeR><b><a target=mainpane href=.?charsheet.?php.?>", 
																							"<centeR>[<a href=\"javascript:parent.charpane.location.href='charpane.php';\">" +
																							"refresh</a>]<br><br><b><a target=mainpane href=\"charsheet.php\">" );
		// Fix chat javascript problems with relay system
		if ( getURLString().indexOf("lchat.php") != -1 )
			fullResponse = fullResponse.replaceAll( "window.?location.?hostname", 
																							"\"127.0.0.1:" + KoLmafia.getRelayPort() + "\"" );
		// Fix chat context menu problems with relay
		if ( getURLString().indexOf("lchat.php") != -1 )
			fullResponse = fullResponse.replaceAll( "</head>", 
																							"<script language=\"Javascript\">base = \"http://127.0.0.1:" + 
																							KoLmafia.getRelayPort() + "\";</script></head>" );
	}
}