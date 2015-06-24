/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.combat;

public class CombatUtilities
{
	public static final float hitChance( final int attack, final int defense, final float critical, final float fumble )
	{
		// The +d10-d10 in the Hit Chance formula means the distribution is not linear.
		//
		// According to the Wiki
		//    http://kol.coldfront.net/thekolwiki/index.php/Monsters#Monster_Hit_Chance
		// it is the Cumulative Distribution Function of a triangular distribution
		//    https://en.wikipedia.org/wiki/Triangular_distribution

		// a = -9, b = 10, c = 0.5, x = defense - attack

		float missChance = CombatUtilities.triangularDistributionCDF( -9.0f, 10.0f, 0.5f, defense - attack);
		return Math.min( 1.0f - missChance + critical, 1.0f - fumble );
	}

	public static final float triangularDistributionCDF( final float a, final float b, final float c, final float x )
	{
		/*
		  (defun cdf (a b c x)
		    (cond ((<= x a)
		           0)
		          ((<= b x)
		           1)
		          ((<= x c)
		           (/ (* (- x a) (- x a)) (* (- b a) (- c a))))
		          (t
		           (- 1 (/ (* (- b x) (- b x)) (* (- b a) (- b c)))))))
		*/
		return  x <= a ? 0.0f :
			b <= x ? 1.0f :
			x <= c ? ( ( x - a) * ( x - a) ) / ( ( b - a) * ( c - a ) ) :
			( 1.0f -  ( ( b - x) * ( b - x) ) / ( ( b - a) * ( b - c ) ) );
	}
}
