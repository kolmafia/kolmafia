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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui.listener;

import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.LicenseDisplay;

import net.sourceforge.kolmafia.CreateFrameRunnable;

import net.sourceforge.kolmafia.swingui.panel.VersionDataPanel;


public class LicenseDisplayListener
	extends ThreadedListener
{
	private static final String[] LICENSE_FILENAME =
	{
		"kolmafia-license.txt",
		"spellcast-license.txt",
		"sungraphics-license.txt",
		"jsmooth-license.txt",
		"osxadapter-license.txt",
		"htmlcleaner-license.txt",
		"json.txt",
		"swinglabs-license.txt",
		"sorttable-license.txt",
		"centerkey-license.txt",
		"unlicensed.htm"
	};

	private static final String[] LICENSE_NAME =
	{
		"KoLmafia",
		"Spellcast",
		"BrowserLauncher",
		"Sun Graphics",
		"JSmooth",
		"OSXAdapter",
		"HtmlCleaner",
		"JSON",
		"SwingLabs",
		"Sort Table",
		"Unlicensed"
	};

	@Override
	protected void execute()
	{
		Object[] parameters = new Object[ 4 ];
		parameters[ 0 ] = "KoLmafia: Copyright Notices";
		parameters[ 1 ] = new VersionDataPanel();
		parameters[ 2 ] = LICENSE_FILENAME;
		parameters[ 3 ] = LICENSE_NAME;

		SwingUtilities.invokeLater( new CreateFrameRunnable( LicenseDisplay.class, parameters ) );
	}
}

