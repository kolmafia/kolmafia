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

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.Concoction;

public class ClipArtRequest
	extends CreateItemRequest
{
	public ClipArtRequest( final Concoction conc )
	{
		super( "campground.php", conc );
	}

	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	public void run()
	{
		if ( !KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0 )
		{
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + this.getName() + "..." );

		UseSkillRequest request = UseSkillRequest.getInstance( "Summon Clip Art", this.concoction );

		int createdQuantity = 0;
		int beforeQuantity;

		do
		{
			beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );

			request.run();

			createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - beforeQuantity;

			if ( createdQuantity == 0 )
			{
				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Creation failed, no results detected." );
				}

				return;
			}

			KoLmafia.updateDisplay( "Successfully created " + this.getName() + " (" + createdQuantity + ")" );
			this.setQuantityNeeded( this.getQuantityNeeded() - createdQuantity );
		}
		while ( this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue() );
	}
}
