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

public class KoLCharacter
{
	private String username;
	private int userID, level;
	private String classname;

	private int currentHP, maximumHP, baseMaxHP;
	private int currentMP, maximumMP, baseMaxMP;

	private int [] adjustedStats;
	private int [] totalSubpoints;
	private String [] equipment;

	private int inebriety;
	private int availableMeat;
	private int adventuresLeft;
	private int totalTurnsUsed;

	public KoLCharacter( String username )
	{
		this.username = username;
		this.classname = "";

		this.adjustedStats = new int[3];
		this.totalSubpoints = new int[3];
		this.equipment = new String[7];

		for ( int i = 0; i < 7; ++i )
			equipment[i] = "none";
	}

	public String getUsername()
	{	return username;
	}

	public void setUserID( int userID )
	{	this.userID = userID;
	}

	public int getUserID()
	{	return userID;
	}

	public void setLevel( int level )
	{	this.level = level;
	}

	public int getLevel()
	{	return level;
	}

	public void setClassName( String classname )
	{	this.classname = classname;
	}

	public String getClassName()
	{	return classname;
	}

	public void setHP( int currentHP, int maximumHP, int baseMaxHP )
	{
		this.currentHP = currentHP;
		this.maximumHP = maximumHP;
		this.baseMaxHP = baseMaxHP;
	}

	public int getCurrentHP()
	{	return currentHP;
	}

	public int getMaximumHP()
	{	return maximumHP;
	}

	public int getBaseMaxHP()
	{	return baseMaxHP;
	}

	public void setMP( int currentMP, int maximumMP, int baseMaxMP )
	{
		this.currentMP = currentMP;
		this.maximumMP = maximumMP;
		this.baseMaxMP = baseMaxMP;
	}

	public int getCurrentMP()
	{	return currentMP;
	}

	public int getMaximumMP()
	{	return maximumMP;
	}

	public int getBaseMaxMP()
	{	return baseMaxMP;
	}

	public void setAvailableMeat( int availableMeat )
	{	this.availableMeat = availableMeat;
	}

	public int getAvailableMeat()
	{	return availableMeat;
	}

	public void setStats( int adjustedMuscle, int totalMuscle,
		int adjustedMysticality, int totalMysticality, int adjustedMoxie, int totalMoxie )
	{
		adjustedStats[0] = adjustedMuscle;
		adjustedStats[1] = adjustedMysticality;
		adjustedStats[2] = adjustedMoxie;

		totalSubpoints[0] = totalMuscle;
		totalSubpoints[1] = totalMysticality;
		totalSubpoints[2] = totalMoxie;
	}

	public int getAdjustedMuscle()
	{	return adjustedStats[0];
	}

	public int getAdjustedMysticality()
	{	return adjustedStats[1];
	}

	public int getAdjustedMoxie()
	{	return adjustedStats[2];
	}

	public void setInebriety( int inebriety )
	{	this.inebriety = inebriety;
	}

	public int getInebriety()
	{	return inebriety;
	}

	public void setAdventuresLeft( int adventuresLeft )
	{	this.adventuresLeft = adventuresLeft;
	}

	public int getAdventuresLeft()
	{	return adventuresLeft;
	}

	public void setTotalTurnsUsed( int totalTurnsUsed )
	{	this.totalTurnsUsed = totalTurnsUsed;
	}

	public int getTotalTurnsUsed()
	{	return totalTurnsUsed;
	}

	public void setEquipment( String hat, String weapon, String pants, String accessory1, String accessory2, String accessory3, String familiarItem )
	{
		equipment[0] = hat;
		equipment[1] = weapon;
		equipment[2] = pants;
		equipment[3] = accessory1;
		equipment[4] = accessory2;
		equipment[5] = accessory3;
		equipment[6] = familiarItem;
	}

	public String getHat()
	{	return equipment[0];
	}

	public String getWeapon()
	{	return equipment[1];
	}

	public String getPants()
	{	return equipment[2];
	}

	public String getAccessory1()
	{	return equipment[3];
	}

	public String getAccessory2()
	{	return equipment[4];
	}

	public String getAccessory3()
	{	return equipment[5];
	}

	public String getFamiliarItem()
	{	return equipment[6];
	}
}