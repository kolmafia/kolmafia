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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sourceforge.kolmafia.utilities.IntWrapper;

/**
 * A fixed value with a label and extended description that updates its {@link
 * IntWrapper} in its <code>actionPerformed</code>.
 */
public class PossibleSelection
	implements ActionListener
{
	private String label;
	private String description;
	private int value;
	private IntWrapper wrapper;

	/**
	 * Sole constructor.
	 *
	 * @param initLabel	a string (treated as containing HTML)
	 * @param initDescription	a string (treated as containing HTML)
	 * @param initValue	the integer value to assign to the wrapper when this value is selected
	 * @param initWrapper	the {@link IntWrapper} to update when this value is selected
	 */
	public PossibleSelection( String initLabel, String initDescription, int initValue, IntWrapper initWrapper )
	{
		label = initLabel;
		description = initDescription;
		value = initValue;
		wrapper = initWrapper;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription( String description )
	{
		this.description = description;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel( String label )
	{
		this.label = label;
	}

	public int getValue()
	{
		return value;
	}

	public void setValue( int value )
	{
		this.value = value;
	}

	public IntWrapper getWrapper()
	{
		return wrapper;
	}

	public void setWrapper( IntWrapper wrapper )
	{
		this.wrapper = wrapper;
	}

	public void actionPerformed( ActionEvent e )
	{
		wrapper.setChoice( value );
	}
}
