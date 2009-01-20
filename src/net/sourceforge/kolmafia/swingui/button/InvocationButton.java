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

package net.sourceforge.kolmafia.swingui.button;

import java.lang.reflect.Method;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

/**
 * Internal class used to invoke the given no-parameter method on the given object. This is used whenever there is
 * the need to invoke a method and the creation of an additional class is unnecessary.
 */

public class InvocationButton
	extends ThreadedButton
{
	public Object object;
	public Method method;

	public InvocationButton( final String text, final Object object, final String methodName )
	{
		this( text, object == null ? null : object.getClass(), methodName );
		this.object = object;
	}

	public InvocationButton( final String text, final Class c, final String methodName )
	{
		super( text );
		this.object = c;

		this.completeConstruction( c, methodName );
	}

	public InvocationButton( final String tooltip, final String icon, final Object object, final String methodName )
	{
		this( tooltip, icon, object == null ? null : object.getClass(), methodName );
		this.object = object;
	}

	public InvocationButton( final String tooltip, final String icon, final Class c, final String methodName )
	{
		super( JComponentUtilities.getImage( icon ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );

		this.object = c;
		this.setToolTipText( tooltip );
		this.completeConstruction( c, methodName );
	}

	public void completeConstruction( final Class c, final String methodName )
	{
		try
		{
			this.method = c.getMethod( methodName, KoLConstants.NOPARAMS );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public void run()
	{
		RequestThread.openRequestSequence();

		try
		{
			if ( this.method != null )
			{
				this.method.invoke( this.object, null );
			}
		}
		catch ( Exception e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}

		RequestThread.closeRequestSequence();
	}
}
