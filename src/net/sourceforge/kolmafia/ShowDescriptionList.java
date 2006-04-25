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

import javax.swing.JList;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A special class which displays an item's description after you double
 * click on the JList.
 */

public class ShowDescriptionList extends JList
{
	public ShowDescriptionList( LockableListModel model )
	{
		super( model );
		addMouseListener( new ShowDescriptionAdapter() );
	}


	private class ShowDescriptionAdapter extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			if ( e.getClickCount() == 2 )
			{
				int index = locationToIndex( e.getPoint() );
				Object item = getModel().getElementAt( index );
				
				if ( item == null )
					return;

				ensureIndexIsVisible( index );

				if ( item instanceof AdventureResult )
				{
					if ( ((AdventureResult)item).isItem() )
						FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionID( ((AdventureResult)item).getItemID() ) );
					if ( ((AdventureResult)item).isStatusEffect() )
						FightFrame.showLocation( "desc_effect.php?whicheffect=" + StatusEffectDatabase.getEffectID( ((AdventureResult)item).getName() ) );
				}
				if ( item instanceof ItemCreationRequest )
				{
					FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionID( ((ItemCreationRequest)item).getItemID() ) );
				}
				if ( item instanceof String )
				{
					Matcher playerMatcher = Pattern.compile( "\\(#(\\d+)\\)" ).matcher( (String) item );
					if ( playerMatcher.find() )
					{
						Object [] parameters = new Object [] { "#" + playerMatcher.group(1) };
						(new CreateFrameRunnable( ProfileFrame.class, parameters )).run();
					}
				}
			}
		}
	}
}
