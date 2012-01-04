/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

import java.io.File;

/**
 * Formed after the same idea as <code>WindowConstants</code>, this contains common constants needed by many of the
 * utility-related classes. Any methods which are used by multiple instances of a JComponent and have a
 * non-class-specific purpose should be placed into this class in order to simplify the overall design of the system and
 * to facilitate documentation.
 */

public interface UtilityConstants
{
	public static final ClassLoader SYSTEM_CLASSLOADER = ClassLoader.getSystemClassLoader();
	public static final ClassLoader MAINCLASS_CLASSLOADER =
		net.java.dev.spellcast.utilities.UtilityConstants.class.getClassLoader();

	public static final boolean USE_OSX_STYLE_DIRECTORIES = System.getProperty( "os.name" ).startsWith( "Mac" );
	public static final boolean USE_LINUX_STYLE_DIRECTORIES =
		!UtilityConstants.USE_OSX_STYLE_DIRECTORIES && !System.getProperty( "os.name" ).startsWith( "Win" );

	public static final File BASE_LOCATION = new File( System.getProperty( "user.dir" ) ).getAbsoluteFile();
	public static final File HOME_LOCATION = new File( System.getProperty( "user.home" ) ).getAbsoluteFile();

	public static final File ROOT_LOCATION =
		Boolean.getBoolean( "useCWDasROOT" ) ?
		UtilityConstants.BASE_LOCATION :
		UtilityConstants.USE_OSX_STYLE_DIRECTORIES ?
		new File( UtilityConstants.HOME_LOCATION, "Library/Application Support/KoLmafia" ) :
		UtilityConstants.USE_LINUX_STYLE_DIRECTORIES ?
		new File( UtilityConstants.HOME_LOCATION, ".kolmafia" ) :
		UtilityConstants.BASE_LOCATION;

	public static final String DATA_DIRECTORY = "data/";
	public static final String IMAGE_DIRECTORY = "images/";
	public static final String SETTINGS_DIRECTORY = "settings/";

	public static final File DATA_LOCATION = new File( UtilityConstants.ROOT_LOCATION, UtilityConstants.DATA_DIRECTORY );
	public static final File IMAGE_LOCATION =
		new File( UtilityConstants.ROOT_LOCATION, UtilityConstants.IMAGE_DIRECTORY );
	public static final File SETTINGS_LOCATION =
		new File( UtilityConstants.ROOT_LOCATION, UtilityConstants.SETTINGS_DIRECTORY );
}
