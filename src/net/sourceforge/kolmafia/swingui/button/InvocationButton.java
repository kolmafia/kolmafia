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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.swingui.listener.InvocationListener;

/**
 * Internal class used to invoke the given no-parameter method on the given object. This is used whenever there is
 * the need to invoke a method and the creation of an additional class is unnecessary.
 */

public class InvocationButton
	extends ThreadedButton
{
	public InvocationButton( final String text, final Object object, final String methodName )
	{
		super( text, new InvocationListener( object, object == null ? null : object.getClass(), methodName ) );
	}

	public InvocationButton( final String text, final Class c, final String methodName )
	{
		super( text, new InvocationListener( null, c, methodName ) );
	}

	public InvocationButton( final String tooltip, final String icon, final Object object, final String methodName )
	{
		super( JComponentUtilities.getImage( icon ), new InvocationListener( object, object == null ? null : object.getClass(), methodName ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );

		this.setToolTipText( tooltip );
	}

	public InvocationButton( final String tooltip, final String icon, final Class c, final String methodName )
	{
		super( JComponentUtilities.getImage( icon ), new InvocationListener( null, c, methodName ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );

		this.setToolTipText( tooltip );
	}
}
