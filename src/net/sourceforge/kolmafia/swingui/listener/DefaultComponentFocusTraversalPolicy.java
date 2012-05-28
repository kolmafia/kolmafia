/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.awt.Component;
import java.awt.Container;

import java.lang.ref.WeakReference;

import java.util.Comparator;

import javax.swing.LayoutFocusTraversalPolicy;

public class DefaultComponentFocusTraversalPolicy
	extends LayoutFocusTraversalPolicy
{
	private WeakReference component;

	public DefaultComponentFocusTraversalPolicy( Component component )
	{
		this.component = new WeakReference( component );

		this.setComparator( getComparator() );
	}

	@Override
	public void setComparator( Comparator c )
	{
		if ( c != null )
		{
			super.setComparator( new DefaultComponentFirstComparator( c ) );
		}
	}

	@Override
	public Component getDefaultComponent( Container container )
	{
		Component component = (Component) this.component.get();

		if ( component != null )
		{
			return component;
		}

		return super.getDefaultComponent( container );
	}

	private class DefaultComponentFirstComparator
		implements Comparator
	{
		private Comparator parent;

		public DefaultComponentFirstComparator( Comparator parent )
		{
			this.parent = parent;
		}

		public int compare( Object o1, Object o2 )
		{
			Component defaultComponent = (Component) DefaultComponentFocusTraversalPolicy.this.component.get();

			if ( defaultComponent == null )
			{
				return this.parent.compare( o1, o2 );
			}

			int compare1 = this.parent.compare( o1, defaultComponent );
			int compare2 = this.parent.compare( o2, defaultComponent );

			// If either o1 or o2 is the default component, that
			// comes first

			if ( compare1 == 0 )
			{
				return -1;
			}

			if ( compare2 == 0 )
			{
				return 1;
			}

			// Otherwise, they both occur in the same direction relative
			// to the default component, just compare them.

			return this.parent.compare( o1, o2 );
		}
	}
}
