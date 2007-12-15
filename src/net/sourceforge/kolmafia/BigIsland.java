/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.PrintStream;

public class BigIsland implements KoLConstants
{
        private static AreaCombatData fratboyBattlefield = AdventureDatabase.getAreaCombatData( "Battlefield (Frat Uniform)" );
        private static AreaCombatData hippyBattlefield = AdventureDatabase.getAreaCombatData( "Battlefield (Hippy Uniform)" );

	private static final String progressLineStyle = "<td style=\"color: red;font-size: 80%\" align=center>";

	private static String missingGremlinTool = null;

	private static int fratboysDefeated = 0;
	private static int fratboyDelta = 1;
	private static int fratboyQuestsCompleted = 0;
	private static int fratboyImage = 0;
	private static int fratboyMin = 0;
	private static int fratboyMax = 0;

	private static int hippiesDefeated = 0;
	private static int hippyDelta = 1;
	private static int hippyQuestsCompleted = 0;
	private static int hippyImage = 0;
	private static int hippyMin = 0;
	private static int hippyMax = 0;

	// Data about current fight
	private static boolean fratboy = false;
	private static int lastFratboysDefeated = 0;
	private static int lastHippiesDefeated = 0;

        // Data about sidequests
	private static String currentJunkyardTool = "";
	private static String currentJunkyardLocation = "";
	private static int lastNunneryMeat = 0;
	private static int currentNunneryMeat = 0;

	private static final Pattern MAP_PATTERN = Pattern.compile( "bfleft(\\d*).*bfright(\\d*)", Pattern.DOTALL );
	private static final Pattern JUNKYARD_PATTERN = Pattern.compile( "The last time I saw my (.*?), (it was|they were) (.*?)\\.", Pattern.DOTALL );

	private static final AdventureResult JAM_FLYERS = new AdventureResult( 2404, -1 );
	private static final AdventureResult ROCK_FLYERS = new AdventureResult( 2405, -1 );

	private static final AdventureResult MAGNET = new AdventureResult( 2497, -1 );
	private static final AdventureResult HAMMER = new AdventureResult( 2498, -1 );
	private static final AdventureResult SCREWDRIVER = new AdventureResult( 2499, -1 );
	private static final AdventureResult PLIERS = new AdventureResult( 2500, -1 );
	private static final AdventureResult WRENCH = new AdventureResult( 2501, -1 );

	public static final int NONE = 0;
	public static final int JUNKYARD = 1;
	public static final int ORCHARD = 2;
	public static final int ARENA = 3;
	public static final int FARM = 4;
	public static final int LIGHTHOUSE = 5;
	public static final int NUNS = 6;

	private static int quest = NONE;

	// KoLmafia images showing each quest area on bigisland.php

	private static final String IMAGE_ROOT = "http://images.kingdomofloathing.com/otherimages/bigisland/";
	private static final String [] SIDEQUEST_IMAGES =
	{
		null,			// NONE
		IMAGE_ROOT + "2.gif",	// JUNKYARD
		IMAGE_ROOT + "3.gif",	// ORCHARD
		IMAGE_ROOT + "6.gif",	// ARENA
		IMAGE_ROOT + "15.gif",	// FARM
		IMAGE_ROOT + "17.gif",	// LIGHTHOUSE
		IMAGE_ROOT + "19.gif",	// NUNS
	};

	// Here are JHunz's replacement images for Big Island sidequest areas
	// from his BattlefieldCounter Greasemonkey script:
	//
	//	http://userscripts.org/scripts/show/11720

	private static final String [] FRAT_IMAGES =
	{
		// NONE = 0
		null,

		// JUNKYARD = 1
		"data:image/gif;base64,R0lGODlhZABkAMQAAP0DA%2F0SEv0iIv0xMf1BQf5hYf5xcf6Bgf6QkP6goP7AwP%2FQ0P%2Ff3%2F%2Fv7%2F%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAACwAAAAAZABkAAAF%2F6Ajjs7jUBfXrSunWZApPvL4TNa1qZy6apgJhEQsGo%2FI5PFBsWQ6wIhkKsFkehmLpbLNaTg7cAZDkU6kkaFyzW4bIZbvJqguSiqZjEaj72MsE0Jug4SFEXFzFBNuNIWOj4UaGxsWEjWQmI43EpkjWZedoWwQFxguGJyiqqtIEVcdLhqwGRGstqwVsnlzGBdPGhW3wpkVPRcWPrC9LovDzoMRGx06LB2vsFC1z9tKGNYqGRLIld4sF9zoRhWw0hcmExznDiktzenpEmA9ljQW0zMSzN27560dCX8WSLDYcMHewFuHvrSgQIXCEzC9AlYDY%2BkhKwmyWlTbKG2kyQt1Vv9F0OBQRAQugDpNCGmSRYYUybKUHBnM1gUoqSBkILWwUzkWHMbsNDkm2UgNth5skITBgdAOGzJsoFAsEwSnWKeCRSqxJqyWnVJMWKeFA7J%2FXTH9HGt2JAZTvTDslCfKn7yf%2F1xZi%2FsIAk2yeujCSvpnBGAooCBJHSoCwgp5E1aYwpR5JIeeDvyZvLsCdIlqKHFEJvRAL4XWpxys3ErYjZqrI%2FlaXdqhwgMgGzhFiPDga7VJHDCsHpSZ4WcZKYLFITTT0rUWlCpcqBXhaAtTsC7MtVLX9CNk8Ubk%2BzwP6qCfpD1DkRbOW4%2Fk9GBJug%2Fs%2FoYKFARIQUqP5IOBGg9oRYn%2FA2MQMsYKTfGSVTD5HUgEgDZI8Qw8yo1QDB4UOLCgGxaUMQYNFIkgQR0TkEGgRxNkVYMEMr4gYkJuTLCcR21YMBWL0vimgTsj8ngLKRikMZ5V4smyhykNsWekLSupgIGTsvSwkGjWeENGilOKckUpmhX3E0P5QeHdCnOYF2YhKbzGxGvqaRMBBajEIM4eN8mC45uOgPQnJnAACgkE2hiq6KKMNuroo5BGKumklFZqKSEjOdABpmxsmomn24AKahujIlGqI6cKI6qmoq5wxKarViOCp7COQKurmubKqq6h2korr6nWOiuwuf46bKzG4trrsMfKWoSwxdq6q7Sturoq%2F7PLEpuqrtB2G%2B2x2HqL7TPXfssrt9x6Cy2xu16rLLnSMsvCs62aK2y57Z5r7aWc8quqvwAHLPDABBcMsAklGiwXH7NAAAEKU0IQyBlFtIZWJuvlAdbF6ByVwTzNEPXxKnNx4YMPVfE4Ri5AYDVEMZNw7Mg6YEDxAwspe9RgZ3pFeeAcosBjzWE%2F6ZHzQ2P4%2BFwxLgyBjG6Q5HEUJUNO06DK4MlDQWkzeJNoYRpQwBs2PRw90Mpf1KJ0BsVdqWMmPnJZXzu%2FvNixcvmwLYLYZEId6FIHivfEnSuk8pAVD0TTQYgiwEwZJgmWldRSEBQ08kM%2BQpCPJKk88JObheQDkv80erHg51QXaPB1OhF0MNxW6XX9XyZDlnITgy0go3E%2BoG9jmRQczMMB4yKYQrwjGeBZwQa1tAiFUJSg4pZsdg9jmQSZPUDj8CNAb8GO3Uxg%2Bx1OA%2BNNBQAm9cs9lgEyTemUoBQa6Wpvt0biDuRyRRwd4K3CDllCDTcSR7gsmOMiUPiClvrjMiU8QQtPIYvp%2BlCB6q1CAr4wCcNsciVrhIgeQ3LB6kiwvdxZ6RAqgEEOLFALw22DAlhQYPKGg6cLDEg2NywBCizBISSk7mEwsEoMKmPBdMSkExTQgAtJACaFrUEQilLECEboxBkcwh1EMEVwRMCnKpKAKJchQTS0woHuRfAheF7cW%2F%2BSeLTUCSZEGahNFWP0PT6MYGvkkcdN0NgoBfjxj4Bsw284kKAtLu8P2XAASFLgMPB5pAABAIAkBUCASAKgDTRanAqkkBXwQMEJNQNHDpY4pQNIUgEjQAAAGICE1vimHCURXzXuMpaT9c4jCTglCQ7QACT4IzlJcYFWIISHZnTnQE5wh3YcORAF6FIEl0zCGafBBE7AkHsAcyYAEOBHA0TzCPnAARadqM0BEIAAkkxCBTqURm2i0gHOTGMb3DmCd8pTCfS8pxu0iQB9tqEAApAkAAiwAH%2FiE5AK6KVBF8rQhjr0oQELAQA7", 

		// ORCHARD = 2
		"data:image/gif;base64,R0lGODlhZABkAMQAAP0DA%2F0SEv0iIv0xMf1BQf5RUf5hYf5xcf6Bgf6goP7AwP%2Ff3%2F%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3tbW1s7Ozr29vZycnG9vb0pKSjExMSEhIRQUFAgICAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpnmiqrmzrvnAsz3Rt33iu73zv%2F8CgcEgsGo%2FIpHLJbDqfoo1UytjApjkrtDrStrze7Q1cBVNJYa5VS2Wfp%2BzoeWmOcu3d0jpevqvza3l0gmpwaHp%2BZGVtgH5Nin8naYF4kZaBaYOVlI6Jm3J3jHacmUaQbqVYlX1ul6FzYjulsUWGtLe4ubq7vL0%2Bqiuzwb6fwzLCtJORqntdb6%2BdusrPo3Fk1LC704TFhqfE26ue14TIyYLf5Kzo5slzYczsluvE9fb3%2BPkkDfo%2BDRQXMlxw0K%2FGPwsVKmjQYMGChgoWMGSg8EQBgosYMSrQ8cBChgoYNlD4d4HCho8VMv80gOCAn5IFBwAACEDgwIEBABDowGCBoASKE6SYpNhAQ4aFPU%2B4dPlDJgESBArgcFDhggmTGiZQiMCgAUIJExhStUCBoAMKEP5teOgD51MRCxQsuPEgwwYMTEdIUKoVpMCUGBQuDFlhQmARLXMQAPCWwUUcECdMaNAgYQUTKT%2FalaJhL4MKJ7cGHcmgbsSHD3AsFnDRQACdNy7wq8ww4gQSFwKDrhDhgcnbDChY4Coi7QgKEyNksKAaAGsEBnLOcCkhpQXDGCTww0BRBEAUkvOWoOB5RFjxMhY3NgAbxr%2BEGXhe2JC0YwYREgJeliFYu14Nqdmg3gwt%2FRPfBQ%2B5VAH%2FQQxEoMFaIDk0mQzzbVBecAkKyFgMBgokEj%2BXoSfCAyABFaAIIp5QHWkjQFDVfjTgBMBcL5SIXAVMAQYBeFmZIAFCNlCAAQ0JyGRkYysYpuCOIkQwxYTjPcjiCKABOEIDym0A3Aog0bAAjTFkh0JdG8BYgkOlKJQBkyKAViYLD2jQnRH5EafilscRBIEFd82p14UY9lhCZfthl0RlH7owXwYBhgVoCg4wOJ4Uk3l0Ip0MwfBgZwoy98JZk%2FYIUIpBBCVSjQwxZRiKGFilAp%2ByXTmBZ8iRCoRw%2FgX3aFckXOoXPyZpKR5T82mAQmWuIjFBrA1KYWZXUJqAZlnKYcCm%2F0sUUJCakNGiiKGfRjRwwX4K0ectA2GRhZ4Dvt3HwI7YkgWBUCnEadezR2AlQV1ZNUDVAxCEtNBC6IXV6wNqrVWmBoct1eZDbCoBEVq9CjzYQxYkO4JpvDpwlEl8YmBsdfEJWdIEEj6h3JyViTxRtgRLwDCOLkHQ47IiC%2FXAXRoA%2B%2BBaFyBoQcRNKETrBhcENVBDPQcnRWCpSYB0Sj8PFSeCpd37JmWxxLnfPw%2FN55FDDmQZUJk%2FPQ3YQvMFlVJV9Dl0KRN58WNYBstVydBJhK2VMWdGWSCBBDw5MJ9EUla4nAZ2EhFBQxkHzVnGkEMeHNOFrSWBA6dhMAHeEVh3mf%2BDFzwg0b0KofygQ1MSITPgRi0MB2dzO22V52qVnvUFCC1nulUK8d6qwG8iIdjUDK%2FOGYKUmuDAQsip7qkIp5VJW2oOrBUf3P0mEdJyDg7NXQQ3ks6oCeImJCd9eXELwQVSd9cAZxDN13oRQRkFUmAYDO%2By38ZCH0Rk5hCiISdoeLPAUkTmF97ZCghVukvQgha5htQvIigACHI%2BIh6APA0Dl6Lg%2BvBlBM7JiUspySDvpPAorJikceKagkKcICFbJQQvJwCIqSR1HIY9zwTzAc18nAAacGEmhTkclxTQE5EKSA2IGngfn3iYBIfkCgU3VGGF0COybGUCQREYVxSLZiH%2FFOIQM3Z5EBc%2F4qRBHUUgDiGhEeSmFF5VgAJNw4wU5iMe0oEwA9EqCtAggiMmiI13lENI5fDGKSAiCEFUFJJDGJAxpjhJgg3RmPemVaHZwQFPiHkIQzqzD4EIBALxo5IUMsadDRDtCD%2BLyCr3dhQp2KVb6JLTQ5ZDgqEERjOM2s5aPscoXi5BA8zDm0KsE0eF4ZKDbdISfujDHRLRJ1MgG1JKfMMW50nglbKgT9AUQgGItOqAIenWMicAkkVV5UG50RPkkEbN5YyrnA%2BqSkMg4CK7MCsHSwkLyixgup9sSmCbAc5YNsO2CiHzexHJGdBW%2BDSHBmSCAuPOBGpXg1llZQwgDDnK0CSwmSkwJCXlPIpRxMIirnUFIMtByISWsrnZOAxFD6BiDl7Xqvj0CSIYS2P%2FSnoSshRkY6g0y0quJJySDA4xswLLA49aR6pa9apYzapWt8rVrnr1q2ANq1jHStay1iAEADs%3D",

		// ARENA = 3
		"data:image/gif;base64,R0lGODlhZABkAMQAAP0DA%2F0SEv0iIv0xMf1BQf5RUf5hYf5xcf6Bgf6QkP6goP6wsP7AwP%2Ff3%2F%2Fv7%2F%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAACwAAAAAZABkAAAF%2F%2BAjitKWRaPopc%2FKvi3MunI90ilu33CkbagZzaOTFXfI1%2FFoK0IyHAlEOVwNVcRWVuuyxrgqHjiH7ZbL1W%2FK0pkYY96rPBzeeuu8a34%2Fx2tZEx0XQUpwX3d%2FdESLaoiOWIWJh4x9aiIaGk2GZjpmlo2genRiiqF7ow8XHBNTb4ekfZWPXY%2Bnm5CxOBoZO56iY3Z2Yoy4Z2TAxXpZNIGsSTBMz9LT1CwYmdWo2dvcSLvd4OHiOW7j5ufTHoTo7O0sHOvu8uga8fLR89MbrfmI%2Bdsa%2BM2bZWxRFXzieP0zBMmUn0%2FoFPYbdsMgKC4IwUkcaPEiLI%2FtNrrr5JGgpJD%2FSP%2FiIKbMHzuRC2NmyyBQpk1pAW%2FqlLZvp88dPX8KfQFzqNCiMiNUsHChqdOmFiw4W0jhJoQIFjBp6GCwKxEOWitEqHmOrLsJFzBsMLgrQ4YLbplmwOA2gwaDHDBckMDO7LkJFjZw8NCBw4YLFSj4BeTBApTBHjZYWJyNMri0iwRbyDZB3Qi6gzVgsCyNdDUIFQYbxjDh2jZVNSVc4CrZ9E0JGQhjqCrCdTZMZink9oDNqAgIbDycYGG38obDUaP%2BQIF8rWTjDzAQvvACAtgHEMKLHx%2B%2BtV2tXL16KPcggnZBQyFg6FCPRdQLkelqgKzea4cNHbjFQV3LpUDBVqP55EP%2FBxgEAcEPABYmIWH%2FIVYBJhdKZkEFHF5DgVQWeHDBBHzB8KAHCd7kA4opuOeWKtw90FmJIgSW11o%2F1LjVWl1xUAMEa212E340wQDYaks1VkE5w3XFV5OLgEWfDWwUJ9NzNUDZFQYiRKBeVfh5xQGDO%2BQm5D9HEoGUllG2UkGP6z0ggXoYcBBjDRRwVcFCdy2yZn%2FwiPCmQbm54WVmTeV55wvXbEUcn4HaZRabX5XYZ1d7duZoV4umIAF%2FxNnWjXYodMYbc%2F11sNkTDHLIVAddUtjBrLSeyUIEG2hQAX6dnuVZC3M1mEKTJtQl7A7y2WNDbrzMuNCc5bDl6Ygk5gOF%2F5oy%2FprPp3tmh4F2VtrUZLPazvOprUCGG9MEY1IwVrbK9qUBurnu9GlOInQW70tcjuBlXmmdmg9qyqWg70JPbPSEQRssdIFgBY9w8ELNGUxXnT7%2Bk9tzi06ssbojZNDwxxLQmK8H3X5cQ73yTCAgEC9AO42oRIEswn9RpXyOdkSA3Bl7SehMTcUwpBcZzTKMF4F7dl5gq8SNTYNUEkTDsCGWUqCmmAS7WsgUYl87BdqYUQZlZLneIB0yvjXMJ9hzswqW3qyL0F03V7TSenQNHieBtjRVywAFgYQXXniDyL2bynw259vBvtAILHXjIwAIluSvLUJ530i4pfYDgcNwqf%2B6FFxcuAWlB5vWNcQ5bQPnOzwMeZaUHydBIIblTathP%2FxgtN5fEdHrC6ZOQ0EUM9XurwdNP4XYBNSB17XT0TWFFnw7%2BDC1DG0g7UO%2FSBwaKmeXgr%2Fs9kUD%2FcynJtsAZHrtIwGBBO%2FNOjxR6L9wHTWw2xBBbt%2B6WAAHSEC6fGVoT%2FNG%2Foj3tzIxjwOGyRUEJ0hBChKhA0Ir0%2F3aVqRp9A9Pg5kdUdJjPmTpihoXUB6jHjeztThoAuojXmgUIw1ccS0qmINBCqsBnOwZLIT%2F28%2BA4jWBPvXLNBTI06YwiISsPAMrVfmB9UgAFcD0aQMUkMClonQXDigmAwFKC2SMxrD%2FPmWAL7LR0i4whIQdJuEuHaAAj5gnmjmSjW6GiQwYNcSK2fynK%2FTZEFMAJLIx6S2CF8wVBsQyjRSaZo1EKNZwBkSXF4ngYSjyy4FMEJ0EcnAtbxFhDbIiShREIIk0SmIMRyABGtYgAvGbRis%2Ftwa2YccnFxLlLeVxoV0KxYm%2B9Ikbb6IAABjzmMdEgE8cQ0twOMCYA2CAAxywgAAcQJgqdEcAAECAFCRAmTvxzU4IwM0RKEABPwmlT8jZzQc0oADg3MkqfkJOZAIgnjrpQCxjwk4RNEAA%2BLQJBPRJz3KKgAEM8Ikc97mQfhplmOMEgACMIxqfNAAB2wSAAdAplDb4LkQBBkCASBHA0Z80MJgLMRtKxdXMlXJjgS5FxwVaGtNq5LCm8tAlTsVx052yIwQAOw%3D%3D",

		// FARM = 4
		"data:image/gif;base64,R0lGODlhZABkAMQAAP0DA%2F0SEv0iIv0xMf1BQf5RUf5hYf5xcf6Bgf6goP7AwP%2Ff3%2F%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3sbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpnmiqrmyLNm4sz3QN13iu73zv%2F8CgcEgsGo%2FIpHLJbL6c0Kh0Sq1ar9isdsvter%2FgsHhMLpvP6LR6qym1Te9Re86gG%2BMivDynifdPf3l%2BdUl6enl8fm99i4qEdI2KkZKIlX%2BNj3uRMpCZiJ2gnqJ2o4SfgqaXe5WmLaGGma%2BtcoytpKykuackgTGyvLHBt7bEsLulosC%2BlsXMv56qx3bR1IPKsyuLloHawqm9qta1qJjlq7y9azOH6nzt7%2FDx8vMqjOn0rvg4h%2BOg4%2FL2cNUxhw2FAgQIEyZUoIVfLXGsVCw4AABAAAIHDgwAgKChG02rurWoSIAEgQIelf9FSxVxxcaSIhYoWJASHTKRLAgAgMkAob6cAAQgNBCg408VOoUiMMDxKNKdIwwYVSIhQ4YIVXTybDIhQ58MEqhohVJBA4YIFPpUuAFlIwCaSxxY0GABQgQMjDBIeNAkQcW%2FW43c1VDhwVx7XyOwRbIALhMJfSSUpQvB8FdGE36mvTDhAmUSgzNg8Hp23gS8nK1OWCzCQVezeDVcoADhHQS1ZTHUFtHA7mmsEDwjxsBX3dzOGiiQsHDBa5%2FMIk73YZ5c3QPCEwiX8OqcXYYLDUYXR9PAc3kMDrZr6H4iLYSy0NFkr1AHK2irAU98hyCa9RgHojFQwQUmpIWYfSWkNUH%2FWmGdkZ1i1SWImHYnNPcABhj4l4MDEqRnBH8EUoBBe4xYYGKDJtxWQVr08SAXXhmMN0QFGTQAoHImNBCBAx6uQMFVc6FIwwMVxGbPbBruEB59PyYpA4AWPBAgDRIchoEF3a23XgUI9hCBBpVFyAODx8kQwWEWDPgVltM5Z0GXOqTVgAUj%2FoAhfxrEp0IDr6XpZoPlTVfkenDWIGVdGgjJQ3YTlJUBCxF4VgGN671pwmRmUVpBjzUoiKEQovVBoArZaTndCofJFtsFFewmA57ZWWAmBBRMSgEFijKQQVkXcFpCpGaVCIGTI0BA54SEFapCBN3JasIE0FLAKnNZllhC%2FwMZTLAjCmeu59wFypLgmratSdfHaIS%2BSumooCGr1q0UoPVpu7mOYCClV10bQQTFOYCuWWwxi28frsYgJbvo1OVhWc6OgOViX9bLwHWzfZVhdMzF1mJaadLY448QGMvdCcQycFvDJDDcY6ktRqcBp1%2FqWQKmVbLrpogt3ibCXR%2FXGV29vprwJcojOBpldBkoOMKXnGIrMwnXZVDblSMkXRwGLV4XQQVzufpjCxIvTeHMen2XXldzamBfAy9v93TKu0k9gm4mA9hiqeeOV2QLLXM7dsoZQqZcVwz4G2MeimZAdApgjnABX7PtKsJ7t8J5weImOIDwCV%2F2XfTFc0VQlf8IUl6c6HaYA%2BLq4wxA%2BzcKSbMw%2BrKvi7C3yd8RLoLg9W3nOeOr7yYljipYdeILs63Q%2BQm3C5jco7x9qrbvLsgtQq8i%2FNgwkSg68CMjzmXg62lB%2Fyom4CMAuB4J2bm3nc%2B7Y8VnWPsmKwFeGEhbIqWidoeBtvw6DfFagyTlne5SCEsL9FoTmwHq6lEPkIAEFMgmulRLNlbZVaMyeCssieZW5SsB%2BfbUm9fZ6HIMsFHhrHIDHrHMRnaZzL9kg6bJSI1IFQhbDi5EAWJJS0pJS5PtvKKt0VgAObIqkuI8gyEjgS9NPYTCjxSjArwMyl15uWCwvtIcVk0AAjwq2RGyo8Nb1mWLEVyKwGm4dIErKQYCEzjiGoe1rwjURkZV4E8UVTBBBlQldeq4XAidYoKziJGQAyIk2LKlyBUIbpCNvBEkG8mcSRLSMIdrJLdEg0dN8gZLhfHktV5URhGEAAA7",

		// LIGHTHOUSE = 5
		"data:image/gif;base64,R0lGODlhZABkAMQAAP0DA%2F0SEv0iIv0xMf1BQf5RUf5hYf5xcf6Bgf6goP7AwP%2Ff3%2F%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpnmiqrmzrvnAsz3Rt33iu73zv%2F8CgcEgsGo%2FIpHLJbDqf0Kh0Sq1ar9isFrXpdhkbV5g0LpVF43O1rE61T%2Bp0lg1mf8lmdHhfr%2FPvfmgjaYBePnaCdIN5hH1gj458iYt0ijuWe15ncZCPlYKToZCFmj2YnXl4n6Grnouvbzqnlqiufautkl%2B5pq%2BjgHqGjZKexISIx4PAc1tEpc3Q0dLT1NXW19jZ2i4OFg3bORMY3%2BA3FBjUCgjr7OwKNhcZDtMLBwAAAQQHBwMACDUUulCodo8ACQIFaEzI0MVCtX4GRSxQsGAGBQ2aBk4jACAig3UzFmraoEHCRgAC1v8ZCPAvxsWRXTA8kMYxJQID%2FmB0g6nJYTSOHg20bFEBI0meDX92nIEBKc8K0IAy1WCUqtWnzfoBqDjjQQaf5CBYsIAhA0YNELIkuMfWI4wGF8BSmECOgoUHeMldWcD1hoWBDSyYnSCCwoVyLv4yiLDhQQV0DOwiZuGVMIUMEipc%2BGZ48goIG0xGoLrBZ2fPKR6PiECBwrzIh1GfcIABQ4UKrdOOOC27xIQuRoF%2FtTuudwkH4i5IWP44A0OSEYyX8OqTBOixr6WL%2BJ09sgPQ3bUzuABZRIOSDjaEl97ANonzEtLPFC8CQgaoJCo8SK%2BR%2FoSSs22An38Z6PWee%2FRFlkH%2FCmMlOF5sDEhA2AgWQCheBv1d0B8DFSbYAIa7TchAYBZKB4EGIprQIX2X6XZChQYap5kKK4o3I4MlGgfjeiPWqN0Ftw0oQgUStJdjbygiOMJXDtyX4AYQzFVCBhc4oMGGxqXnIgkUQJCekMYtNN9sX7G4WQrtVSddgyk0eSRqmkWHgpVvIvaABE1dySOd0onTRQUWbIClCOnVuc1oFrj425aEaoABXagF5g0JDlxwZoQTnAOcodc8gJYJFWxg6QVn3SeOBqjh%2BU035lEQqgW3UTBmUajBKgJjInj14YAPtNbAaGBqUylUD5A3YmeqsZanA3AVR8SgP1yUFnnRSZBB%2BHQXkaoBbnKCN4QDocboQwQgMpDBhGQVpoEFcr5nqRB4lpTiD5Z%2BI0GV20F2kUkvHsbjDcWKigYF4gLMmagONJnoaxFAkKmg381UaZe1kWUBszoshBk5v13Qbg5OBiQgBE1t8GhgXZj12AZflfycF3F9TMNFF5cQ78UFxxDquhp8ta3JZBml3AVHkUfSbVRaQJcEJPFLwwMW6DmbxrLa4OkEj2EAAcoXpEUbBq4xUCnYDTwQQXQNeJlfBTm3UCyKK3STAQYyw9DrN3o1MMG%2FRiA3d90oQEDqBQ%2B0Tc29247ZQgOXbctoNW%2BzSwO4PbNrOBZQz10kCyEAADs%3D",

		// NUNS = 6
		"data:image/gif;base64,R0lGODlhZABkAMQAAP0DA%2F0SEv0iIv0xMf5RUf5hYf6Bgf6goP6wsP7AwP%2FQ0P%2Ff3%2F%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpbqipjtuqtu4Kx3TtznOd07tu%2F0DRroVjEEk9FO5YRKaMz6BUVjoKodXsFZZqXr%2B9qdiYLSZPSjJYzWqzx%2FAh24rUkpv0rzoMl%2BKFeXpgVlFna32IaSxMaIpdgHMvhYiUlTZ8lpmaN5udnp%2BgoaKjpKWmp6gxSphArHWpl25xP66welxPjIVdj4Nctj5bbbm%2FZreRtbargGleRMu%2FvoHAJ3bHwq%2FG2NRUr3eL24SQuG%2Fc1szHu%2BRY5NPmU8nvpPHy9fb3%2BPn6%2B6euyWH05Pnjt%2BnZrj3qfCmEEqUeHUIPkUnUVs5cRDPQtEyiGHCexG2C3hQbJkvgx17fhP8Z43gvIjt0MHFFc1SSoM2bOHPq3MmzJxADQIMK9dnnwAAAAAYQMEAgAACfDSg0GGMAaYIRCQAo6OlgQ4UxCKxivcozAgUNGS48mJIVAFkGT3tWwJBBwwUIbJEWAHoUqgMLF6bmBbDXgIC4Pi1YGNOW7ALEPS1gcCCmMVESDS5sWDz47WUJKDREmBKW8GUREDIowYAXyGGkYqFqXnUXiOfLDSysWvX1tJQJqyRUWEWBsm8boJX0Hq78eA0ItEkwR9HbuQoJqlFkWMtAMAXaxq2PiJB9gwYIUSuorwAhuZLR4hk4wKAkg4TZGVRr2HDh%2B6oJggkWhIDy5LaKBWhN4ED%2FVMVFgIEGZ9G2XgUURNBaCRBQQMEEwk0YHjUPzGbeBRlYkAEGo1kQQQPA8SfBfrvtpsGMNMIYIwrwcSPiBi9K9UAFGkxA4VkabgBBiDcmqeQG9OUIzHQbUPAidwxM8OCMFhQpgXw7LunlBh%2BOQaAUBoY2AQMVqNXdBBQwR%2BMFEIqAoIXCXWCnnRTeqd4FillQwQQaWHBhH2MC4QCUZ6KJVn5pKaEiZUHKhwFnJTQw5lQEZlYdIpsGEcEqGbSWZgSkQgCYnRjcBd1oD3g1gp8USHYBCRq2WYGAF3QKRwZh%2FmAghPaJ8NdUf6l1QQQhNopXq1uKACeTJ47QAAYPqkZC%2F66W9FeoDbndGqyzGFSg2FQodqfZZAy0Gh6fTM41wgQnorUBrrqO4eC2Qdgn2AP0qclABgA6yCNqOfSXFgUYjDCXuN%2BtW%2B8YfiKiLwlWphUBhAhqIIFgn0rL56SSKawBtQ%2BOMN%2FDYjjAax8YNEvCoXU5OmjHwsLJ6KwiUMCoiiOA5nIlafZxQaIqmLXhmMO93GsMFWSwyQOh4lsDwCY0YKqdTo5wQcJwhCv1GEGP8a3JuqE1Y9YMYEuDpSI08ICAGFDQyXwbizHxq0wyQAG%2FopEA9a2VltC0YvRRGvfXYyC89NR1i%2FBifwxo8MB3XIvwtwjirpflq6muJmwGUiHSgP8DPzOgmN2N682nVBsgqcHLXv%2B7G86mw0gtzqkRba9kr%2FtdrhS%2FOxurVLxOqkF4ucu5W2%2BZaVhioCJcrLsUf41MwaA5r5xv6vZBvl3cx4%2FAb28tKoGzA7Yr1iy%2F0%2FsKAV1yu8CnFGNPG4F6aWcGWvyScjbBsyjgn9sWVIKLhQ4IbtMNa2iQO8SRIHgMYI16rPQ%2BQG1KMyIAlI1KV7X9decHENAM5GxAHvT8YGz%2FUs%2BGMAAozSzGARNA0gQiUD4UEC1TtdJNliiQKyqpADh3caC0eLg4BiALcxuI4epspAH12Khp5oHRjGR0olrZKWZpOREKqNVEFViNRAccEGC2pcHU%2FIxsincSVARMZJc6papOf5rLwSJwJ%2Fps8U4SWMsDHACBCDjAAW%2B7zsgCKYYQoUtwEhDOBI4kARVZikAPgEB4lua2MTmgVEKsVAgD5cNCbq2TN2nA4zg4hr%2BgKJPvaEDTirOJVaKSGiGkmid6VER5kIc1r5SCgw6ZDyBRKhSxxJ48dBNGUeTmlPVw0N1KERXQ5dITUbEL2koBrwrUchT3gdAzNSGBSYGSFA9AUG3MQUe1bFMMDVhjfvj3DlMS8hORBAxaKvBNbqhSlpuAIYnM86dPhAAAOw%3D%3D",
	};

	private static final String [] HIPPY_IMAGES =
	{
		// NONE = 0
		null,

		// JUNKYARD = 1
		"data:image/gif;base64,R0lGODlhZABkAMQAAO%2F38gKHKxGONyGWRTGdUkGlX2C0enC7h4DDlZDKoqDSr8DhyrDZvN%2Fw5P%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAACwAAAAAZABkAAAF%2F6Ajjs7jUBfXrSunWZApPvL4TNa1qZy6apgJhEQsGo%2FI5PFBsWQ6wIhkKsFkehmLpbLNaTg7cAZDkU6kkaFyzW4bIZbvJqguSiqZjEaj72MsE0Jug4SFEXFzFBNuNIWOj4UaGxsWEjWQmI43EpkjWZedoWwQFxguGJyiqqtIEVcdLhqwGRGstqwVsnlzGBdPGhW3wpkVPRcWPrC9LovDzoMRGx06LB2vsFC1z9tKGNYqGRLIld4sF9zoRhWw0hcmExznDiktzenpEmA9ljQW0zMSzN27560dCX8WSLDYcMHewFuHvrSgQIXCEzC9AlYDY%2BkhKwmyWlTbKG2kyQt1Vv9F0OBQRAQugDpNCGmSRYYUybKUHBnM1gUoqSBkILWwUzkWHMbsNDkm2UgNth5skITBgdAOGzJsoFAsEwSnWKeCRSqxJqyWnVJMWKeFA7J%2FXTH9HGt2JAZTvTDslCfKn7yf%2F1xZi%2FsIAk2yeujCSvpnBGAooCBJHSoCwgp5E1aYwpR5JIeeDvyZvLsCdIlqKHFEJvRAL4XWpxys3ErYjZqrI%2FlaXdqhwgMgGzhFiPDga7VJHDCsHpSZ4WcZKYLFITTT0rUWlCpcqBXhaAtTsC7MtVLX9CNk8Ubk%2BzwP6qCfpD1DkRbOW4%2Fk9GBJug%2Fs%2FoYKFARIQUqP5IOBGg9oRYn%2FA2MQMsYKTfGSVTD5HUgEgDZI8Qw8yo1QDB4UOLCgGxaUMQYNFIkgQR0TkEGgRxNkVYMEMr4gYkJuTLCcR21YMBWL0vimgTsj8ngLKRikMZ5V4smyhykNsWekLSupgIGTsvSwkGjWeENGilOKckUpmhX3E0P5QeHdCnOYF2YhKbzGxGvqaRMBBajEIM4eN8mC45uOgPQnJnAACgkE2hiq6KKMNuroo5BGKumklFZqKSEjOdABpmxsmomn24AKahujIlGqI6cKI6qmoq5wxKarViOCp7COQKurmubKqq6h2korr6nWOiuwuf46bKzG4trrsMfKWoSwxdq6q7Sturoq%2F7PLEpuqrtB2G%2B2x2HqL7TPXfssrt9x6Cy2xu16rLLnSMsvCs62aK2y57Z5r7aWc8quqvwAHLPDABBcMsAklGiwXH7NAAAEKU0IQyBlFtIZWJuvlAdbF6ByVwTzNEPXxKnNx4YMPVfE4Ri5AYDVEMZNw7Mg6YEDxAwspe9RgZ3pFeeAcosBjzWE%2F6ZHzQ2P4%2BFwxLgyBjG6Q5HEUJUNO06DK4MlDQWkzeJNoYRpQwBs2PRw90Mpf1KJ0BsVdqWMmPnJZXzu%2FvNixcvmwLYLYZEId6FIHivfEnSuk8pAVD0TTQYgiwEwZJgmWldRSEBQ08kM%2BQpCPJKk88JObheQDkv80erHg51QXaPB1OhF0MNxW6XX9XyZDlnITgy0go3E%2BoG9jmRQczMMB4yKYQrwjGeBZwQa1tAiFUJSg4pZsdg9jmQSZPUDj8CNAb8GO3Uxg%2Bx1OA%2BNNBQAm9cs9lgEyTemUoBQa6Wpvt0biDuRyRRwd4K3CDllCDTcSR7gsmOMiUPiClvrjMiU8QQtPIYvp%2BlCB6q1CAr4wCcNsciVrhIgeQ3LB6kiwvdxZ6RAqgEEOLFALw22DAlhQYPKGg6cLDEg2NywBCizBISSk7mEwsEoMKmPBdMSkExTQgAtJACaFrUEQilLECEboxBkcwh1EMEVwRMCnKpKAKJchQTS0woH%2FRfAheF7cW%2F%2BSeLTUCSZEGaiNoRqQgAXYcQENEAECGHDHBCghRt%2Fjwwi2Rh553ASNi1pAAQLAyAIsQAQHGAAjB2AAJfyGAwna4vL%2BkA0HgCQFDgOfRwgQgAIQgQGMfKQSaLQ4FUghK%2BCBghNqBo4cLNFIizQlCRaQyiVgwDflKIn4qnGXsZysdw%2FJJRF4GQBVFsEfyUmKC7QCITw0ozsHcoI7tCPKeyySAHe0YwJ6aYQzToMJnIAh9%2Fy1SEa6853OJGEZc9BNSylzl%2BS8UIe8eM8RMDOeaSxCP0Xwz4AiQZK6HME4m2nQIjCgnaVUgAMAUAABuDOhDXVAA8KJx4lyDxSgGQ2pSEdK0pKatIohAAA7",

		// ORCHARD = 2
		"data:image/gif;base64,R0lGODlhZABkAMQAAAKHKxGONyGWRTGdUkGlX2C0eoDDlZDKoqDSr8DhyrDZvN%2Fw5P%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3tbW1s7Ozr29vZycnG9vb0pKSjExMSEhIRQUFAgICAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpnmiqrmzrvnAsz3Rt33iu73zv%2F8CgcEgsGo%2FIpHLJbDqfoo1UytjApjkrtDrStrze7Q1cBVNJYa5VS2Wfp%2BzoeWmOcu3d0jpevqvza3l0gmpwaHp%2BZGVtgH5Nin8naYF4kZaBaYOVlI6Jm3J3jHacmUaQbqVYlX1ul6FzYjulsUWGtLe4ubq7vL0%2Bqiuzwb6fwzLCtJORqntdb6%2BdusrPo3Fk1LC704TFhqfE26ue14TIyYLf5Kzo5slzYczsluvE9fb3%2BPkkDfo%2BDRQXMlxw0K%2FGPwsVKmjQYMGChgoWMGSg8CSBgYsXE4jAiNHGAwsZKmDYQOHfBQobQv9WyNAAggN%2BShYYAAAggIEFIhQQoEkAgQ0MFghKoDhBCkqKDTRkWBj0BEyYP3YSKJGApkYaDipcMIFSwwQKERg0QChhAsOsFigQdEABwr8ND31IpWqVxoMMGzBAHSHB6VeRAldiULhwZIUJg0W8zDGXRFUAV2VAnDChQYOEFUysDIlXioa%2BDCqkBFu0JIO7ER8%2BwLFTAEcDBerOuMDvMsOIE0hcGCy6QoQHKHMzoGAhrAi3IyhMjJDBAuuaBKJHHyC7BUwJKy0gxiCBHwaKIgCioLy3BAXQI8yWl9F4xOPIK%2F4lzAD0woamHzOIkBAws%2BSF3fGlwWo2tCfCeyq89A%2F%2FfRc8BFMFBDEQgQZwieRQZTLYtwF6wzlYIABTOVZdCQsKRBI%2Fma0nwgMiEUWgCCqegJ1pI0CglX80BADAACXMBMABKLSoXAVQCQbBeF6ZIAFCNlCAAQ0H0CQlkAtISVMAJiD24JEiRDAFhuZRSOMIog04QgPMbSDcCiIlwR0Kd22AYwkOlaJQBlyKIJqcLDygAXhG8GecjGsmRxAEFuQFKF8cdpgkiQmJsF0Sl53ogn0ZEGhWoyk4EKF5UlQG0ouBMgQDhZ896NwLbIGaJEAxBlEUSS8oZAFUiMGIwVYqJErbmROAplysQBAX4HCcikUCqYDxg5Ka5UFlnwYoXMYr%2FxIT%2FCqhFHOKBaYJdarFHAZ5wkQBBas5%2BS2MHS5qRAMX%2BKfQfewyYFZa6zkAnH4MHGluWhAYlYKfeHV7RFcS3OVVA1k9AMFICy20nlnLPvAWXHJqkNhTej6UpxIQtbUsxIU9ZMG1I6CmrANLoZQoBtRiR5%2BTJ01w4RPMAXoZzBOdK7EEGhMJEwRJZguzUQ%2FkpYGzFMJ1QYMWfNyEQsJucEFRAzW09HBSDLaaBFav1PRRfjZ4WsF8WhaLn%2F7985B9IDnkQJoByTlU14ItZF9RK2l1n0OkMrEXP4hl0FyZDKVkGFwne6aUBRJIAJQD9kkkpobNaTAoERE0dPLTnp3suf%2Fnw2l9GFwSOJAaBhMYHkF2mU14wQMSFayQzRQ6NCYRQDuuVMZweBY411ux%2FtbsZ1%2BAUHO0b6WQ8rtCzCcShIWtce6eNRiqCQ4spBzuq4qQmpy2reYAXPT5vbCbKRXH0MNgDSl7pibAm9Cf9%2B2lLgQXgA1eA56BiH12V4SiKEUkg8FA9HjGOGrVDyJAc4jUlPM0w91KVw%2BRiPKIBYQy5eVpT%2FtcQwQYERQARDkhKQ9AuoYBUoUQfwYzgur%2BxKaVmFB5UuBUV1CyOXhNQSFOuBCxEqKXEwBkVp9Kjsa6ZwL7iMY%2BThCNuzRjQyPGSwrriUgFwNZEDfAvUUlMgkOOFaT%2FNllRQ%2BuB2bky0aAIxMuLU9tQDYuoGbxQKI0h8RKJliIQh8TQCIBzirIqQIGtaUYK9imP7FqYgW8lxWkQIRIT4KY80SFkdIZLVRMb1KAwOskhDDgZVLz0wYagzE3h0lDw4FAoxTyEIZ%2FZh0AEAgH%2FkUkKJ%2FvOBqR2hKZFBJeJW4oU8LIue%2F3pIc0hwVEGw5lMeQcurctUMpegAe0ZTiHZ8SPGiplCPalpP%2Ff5DovuYyqXPWklwIkL9yTAS1nc52kKoQBEdkXBkawLmxMQCaa0QqHdHMpzVgtnc%2BIlTwpppSEQsBFetJWDp5jFZhag3VBQBbHOCActndGbhqo5kuYoebBp1cRh1zYaEBBC7DsTGF4NgnUygDBkKVGTQGemwJCVyHMpSjkLjdQmFoA0ByEYekrqasMxGD0gjDno3a7ooyiImMyOCpxp%2B6aYD4eljgEvKRdxThI5xQSrLBwsSLXEStaymvWsaE2rWtfK1ra69a1wjatc53qEEAAAOw%3D%3D",

		// ARENA = 3
		"data:image/gif;base64,R0lGODlhZABkAMQAAAKHKxGONyGWRTGdUkGlX4DDlZDKoqDSr8Dhys%2Fo17DZvN%2Fw5P%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjig9mOaO4pczKvi3MunI90ilu37BzYagZbaOTFXfI1%2FFoKzYsmUdDOVwNVcRWVuuyxrgqHjiH7ZbL1W9qooEYY96rPBzeeuu8a34%2Fx2tZEBoUQUpwX3d%2FdESLaoiOWIWJh4x9aiIXF02GZjpmlo2genRiiqF7owwUGRBTb4ekfZWPXY%2Bnm5CxOBcWO56iY3Z2Yoy4Z2TAxXpZNIGsSTBMz9LT1CwVmdWo2dvcSLvd4OHiOW7j5ufTG4To7O0sGevu8ugX8fLR89MYrfmI%2BdsX%2BM2bZWxRFXzieP0zBMmUn0%2FoFPYbdsMgKC4IwUkcaPEiLI%2FtNrrr5JGgpJD%2FSP%2FiIKbMHzuRC2NmsyBQpk1pAW%2FqlLZvp88dPX8KfQFzqNCiMh1ImEChqdOmEyY4WxjhZgMHEzBd0GCwK5EMWiU4qHmOrDsIFCpgMLjLggUKbplaqODWwgWDGSpQeMDO7DkIEzBk2KAhAwYKEiL4BbRhApTBGzBMWJyNMri0iwRPyAZB3Qi6gy9UsCyNdLUGEgYbrgDh2jZVNR9Q4CrZ9M0HFghXqCrCdTZMZiPk3oDNqIgGbDacYGG3MobDUaP%2BQIF8rWTjDCoQpvCiAVgGDcKLHx%2B%2BtV2tXL1uKMfAgXZBQxtU0FCPRVQKkelegKzeqwYMGriVQV3LpRDBVqP55IP%2FBhUE0cAPABYmIWH%2FISYBJhdKNoEEHF4TgVQTbEABBHzB8OAGCd7kA4opuOeWKtwx0FmJIgSW11o%2F1LjVWl1lUEMDa212E340wQDYaks1JkE5w3XFV5OLgEWfDWwUJ9NzNUDZVQUiOKBeVfh5lQGDO%2BQm5D9HEoGUllG2IkGP6zHwgHoVZBBjDRFwJcFCdy2yZn%2FwiPCmQbm54WVmTeV55wvXbEUcn4HaZRabX5XYZ1d7duZoV4um8AB%2FxNnWjXYodMYbc%2F1psNkTDHLIlAZdUqjBrLSeyYIDGFwgAX6dnuVZC3M1mEKTJtQl7A7y2WNDbrzMuNCc5bDl6Ygk5gOF%2F5oy%2FprPp3tmV4F2VtrUZLPazvOprUCGGxMEY0YwVrbK9nUBurnu9GlOInQW70tcjuBlXmmdmg9qyqWg70JPbPSEQRgsRIFgBY9w8ELNGUxXnT7%2Bk9tzi06ssbojWNDwxw%2FQmO8G3X5cQ73yQCAgEC9AO42oRIEswn9RpXyOdkSA3Bl7SehMTcUwpBcZzTKM54B7dlJgq8SNTYNUEkTDsCGWUqCm2AO7WsgUYl87BdqYUQZlZLneIB0yvjXMJ9hzswqW3qyL0F03V7TSenQNHieBtjRVywAFgYQXXniDyL2bynw256vBvtAILHXjIwAIluSvLUJ530i4pTYDgcNwqf%2B6EVxc%2BASlB5vWNcQ5bQPnOzwMeZaUH%2FdAIIblTathP%2FxgtN5fEdHrC6ZOE0EUM9Xu7wZNP4UYBNSB17XT0TWFFnw7%2BDC1DG0g7UO%2FSBwaKmeXgr%2Fs9kUD%2FcynJtsAZHrtI9HAA%2B%2FNOjxR6L9wHTWw2%2BBAbt%2B6WAAHSEC6fGVoT%2FNG%2Foj3tzIxLwOGyRUEJ0hBChJBA0Ir0%2F3aVqRp9A9Pg5kdUdJjPmTpihoUUB6jHjeztTgIAuojXmgUIw1ccS0qmINBCqsBnOwZLIT%2F28%2BA4gWBPvXLNBHI06YwiISsPAMrVfmB9UgAFcD0CQMReMClonSXDCjGAgFKC2SMxrD%2FPlmAL7LR0i4whIQdJuEuGogAj5gnmjmSjW6GiQwYNcSK2fynK%2FTZEFMAJLIx6S2CF8xVBcQyjRSaZo1EKNZwBkSXF4ngYSjyy4FMEJ0EcnAtbxFhDbIiShQ4IIk0SmIMR%2FAAGtbAAfGbRis%2Ftwa2YccnFxLlLeVxoV0KxYm%2B9Ikbb3IAABjTmAcQwQCOCQAB2MQxtAyHAAAwABYUEwDJtMkwdUIAABCABQgwJgJu4puddPObKQgnAMYprkH45JzgFOdNVvGTbjLznuyUiQZiGZNuDgABAAWoAeQpkwbss57ejOc6bSJHfi4EnukkaEy2qZNlonMECpAon0ookwQUQsCYAShAAkRQgGVS0wD6XOVCEFCAlrZ0nAtwqUtl0sBgLsRsNhVXNHPKjQXyFB0U2OlPq5HDocpDl0YVR1GTyo4QAAA7",

		// FARM = 4
		"data:image/gif;base64,R0lGODlhZABkAMQAAAKHKxGONyGWRTGdUkGlX2C0eoDDlZDKoqDSr8DhyrDZvN%2Fw5P%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3sbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpnmiqrmyLNm4sz3QN13iu73zv%2F8CgcEgsGo%2FIpHLJbL6c0Kh0Sq1ar9isdsvter%2FgsHhMLpvP6LR6qym1Te9Re86gG%2BMivDynifdPf3l%2BdUl6enl8fm99i4qEdI2KkZKIlX%2BNj3uRMpCZiJ2gnqJ2o4SfgqaXe5WmLaGGma%2BtcoytpKykuackgTGyvLHBt7bEsLulosC%2BlsXMv56qx3bR1IPKsyuLloHawqm9qta1qJjlq7y9azOH6nzt7%2FDx8vMqjOn0rvg4h%2BOg4%2FL2cNUxhw1FAgMIESYQkTChFX61xLFSscAAAAABDCwQoYDARQIIHrrRtKpbC48ESv8kuLhQpLJoqSauQKmS5RWIyEyyoEliJYCW%2Bkx4FNDQQAGbQYViJMCU6QCkSUnwHOETaBEJGTJEqDJVRNUjEzL0ySCBSlcGX4tU0IAhAoU%2BFW5ACQBgQAmLAA4QcWBBgwUIETAwwiDhQZMDFxPrXZD4YgAhgTVUeNDX3tgIcvVJ6CNhrV8IlMcymhD07YUJFz6TiJwBg9i28yYIPp11QmYRDsKyFazhAgUI7yDAXYsBuIgGgGVvhZDaMgbD6vqi1kCBhIULYvuQFiG7z3Xq6h5IniC5hNjs7DJcaOAaOpoGqeFjcGBeA%2FoTbyGs3Y6GfIU6W62WVUAnqAdBa7eN4UD%2FawxUcIEJb1kWYAlvTfBWWWeQhxl4FFpW3gnYPYABBgnm4IAE9Blx4IMUYIAfIxbEiKEJwlXw1n888CVYBu4NUUEGDSxYnQkNROBAiitQoFVfM9LwQAW82eNbiTuw95%2BSVMqwoAUPMEiDBJVhYAF69tlXwYQ9RKABaBzycKF0MkRQmQUOjjWmd9lZgKYObzVggYs%2FjHigBvyp0IBudOaJIXzeQWnfnjV0%2BZcGTfJA3gRrZcBCBKlV8KN9eprgGVufVoBkDRWOKERrfTyoAnllerdCZb3xdkEFxskwKHkWxAkBBZ5SQEGlDGSw1gWnlsApWzBCkOUIEPzpoWSQqhAB%2F3q9mjDBthTceh2ZMJbQQAYTGImCnPZld0G1JORWLm7d9eHao7p%2B6upq08IlLAVuqYovsSNE%2BKlW4kYQAXQOzMuWXNcO3EeuMXR5Lzp%2FpbhWtiOMmZmaADMgnm9jkcjddbzh%2BBadPyKpJATRnnfCswwIhzEJFyMJK47caXCqmoWWMCqY9%2BbZIo7CiRCYyoByB3CyJqg58wiZcsldBhWOoOap4%2FZMgngZACfmCFRDhwGO4kVQQV%2B5KtlCx1Z%2F6DNh6tEXlp8aBNiAzuZpTbNxXY9QXMwL4girvO5B2QLO57pNM4mbVRcWAwnzmEelGTydwpojXGCYb8aKoJ%2Bwe15guf8JDkx8gpqIQy1yXxFgJUKXIlNq3uiA5Ko5A9sqjgLVLLhure4iGB6zeo%2BL0DiA5qV%2Bue3GdTmkClnJ%2BIJvK6B%2BgvANUqfpcarWnbwLfYuArAhKYvzkjA4oyUh2GSQrG9PKtrn4CAvaRwJ5%2BZmXtPFbHVqWwdSSgGAw0C0YfapV6MFAuQ4mm%2BfhZkrVk52oJvaW7eGGNw4slqYeIAEJVPBOfgFXb7JiLEyRUFhjao2w4FeC9xkKOboLkugYECTIZeUGR7pZkADjGYX1Zk6e6dqTKsC2HIiIAs%2FqVpeoRqfgiaVcrrHAdHoFpcqlZkRRWh%2BdkAgFJWFGBYJxVL4GI0JpZo0FO7eaAASOBDOwSDAFYRncmSIgmzNdQEyYgcAEpFhHZxksAsDpURUOxEUVeJABWKGdOkTHwqiYoC1tdKSDHLk2clFyBY1r5CWFpMlLXqeTjqSM5C55rtYIkpTHGdNkUCkuHRVxBCEAADs%3D",

		// LIGHTHOUSE = 5
		"data:image/gif;base64,R0lGODlhZABkAMQAAAKHKxGONyGWRTGdUkGlX2C0eoDDlZDKoqDSr8DhyrDZvN%2Fw5P%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAAAAAAAAAACwAAAAAZABkAAAF%2FyAjjmRpnmiqrmzrvnAsz3Rt33iu73zv%2F8CgcEgsGo%2FIpHLJbDqf0Kh0Sq1ar9isFrXpdhkbV5g0LpVF43O1rE61T%2Bp0lg1mf8lmdHhfr%2FPvfmgjaYBePnaCdIN5hH1gj458iYt0ijuWe15ncZCPlYKToZCFmj2YnXl4n6Grnouvbzqnlqiufautkl%2B5pq%2BjgHqGjZKexISIx4PAc1tEpc3Q0dLT1NXW19jZ2i4OFg3bORMY3%2BA3FBjUCQbr6wki7OwyFxkO0wsGAAABBgsiCgT5CCCIQaELhWoACZRIkM8djAkZulhACEAhCYYAHLqgoEHTwWkJFzZ8AVHTBg0SQP8CEADPQIGRLTia7ILhgTSAAQjo1DkApopuMzVNjBbyok8UFTqeDCqRaEWRGVVgYBq0ArSiIzBqPIFBg1KvYKs2CwBgQAl8AA6weJBhKDkIFixgyNBRA4QsB%2FLpVbtAb74AKxpccEthAjkKFh4oJlcOhYWDDSzQnSCCwoXGLR4ziLDhQQV0DBBjXsGWMoUMEipc%2BGZ5tAoIG1JG8LphaGvXSEFvpkChXujLuE04wIChQgXed0fcDk5iQhelz9siHsechANxFyRo%2F5wh4skI1UmwHUoCdlzf4Rk4Rx%2FaAWz26S%2FoZtAApYMN8Ks3KE6ivoT7NqUnAgQZWEVCBQ%2Fc95H%2FgBOgdMJ9BjKYAWP98SdgaBmkENeFDFwAHAMSUDaCBR%2Bml8GCFyzIAIkXNnCiciLSx6KAEGgQowkzpndacieQSCFzqqmQY3hBalhidT7mJ%2BORzF1gXIQiVCDBfkwGZ6OFI7TlQIEXbgBBYSVkcIEDGqjI3H08kkABBBBKGOCDbQlo2Y8VkoekncKJKaBq4KFAZpWYPSDBVGUq%2BWd14nRRgQUbmCnCfYCCM5sFPDqX5qMaYGCYa5F5Y52H5IR4znORYvOAXSZUsIGHF9RVoDgauDboN92I0AAFqlpgHAVvJuWariJwJgJbLkb4AG8NzAblNg44ycAD8tHX2me7EeqA%2F2DUEeHoDxzdJR94EmQAHketanBcn%2B8N4YCqdPYQwYsMZCCiXJVpYEGf%2FXkoxKAo3fgDqCCOKYI49abU42VK3gDtqmhQ0K4ND7C2qgNbUupbBBBMUFBvENjU7JrEyWXBtTpAhBo5zl2Abw5cFrRBBRBMtYGmkXVB12cbtCWzd14MtjINHI1cAr8jPwyDqvZq0Ja5M8ulVHYXLCXfScaJaYFhEpxkMA0PWFDogybzCrGNn2EAQc0X3DUcBr0x0CzbDTwQAXgNsHlgBUav1aq%2FwkmGwc8wHPsNYw1MkLAR12XwtwsQtHpBxOVI0CqCMNx686XVQGsv4Nwk1VYEeWPRtQXiU7IQAgA7",

		// NUNS = 6
		"data:image/gif;base64,R0lGODlhZABkAMQAAO%2F38gKHKxGONyGWRTGdUkGlX1CsbWC0eoDDlZDKoqDSr8DhyrDZvN%2Fw5P%2F%2F%2F%2Ff39%2B%2Fv7%2Bfn597e3s7OzsbGxrW1taWlpYyMjHNzc1JSUjk5OSkpKRgYGAAAAAAAAAAAACwAAAAAZABkAAAF%2F6AjjmRpdqipjt2qtu4Kx3TtznOd07tu%2F0DRroVzEEk9FO5YRKaMz6BUVjoKodXsFZZqXr%2B9qdiYLSZPSjJYzWqzx%2FAh24rUkpv0rzoMl%2BKFeXpgVlFna32IaSxMaIpdgHMvhYiUlTZ8lpmaN5udnp%2BgoaKjpKWmp6gxSphArHWpl25xP66welxPjIVdj4Nctj5bbbm%2FZreRtbargGleRMu%2FvoHAJ3bHwq%2FG2NRUr3eL24SQuG%2Fc1szHu%2BRY5NPmU8nvpPHy9fb3%2BPn6%2B6euyWH05Pnjt%2BnZrj3qfCmEEqUeHUIPkUnUVs5cRDPQtEyiGHCexG2C3hQbJkvgx17fhP8Z43gvIjt0MHFFc1SSoM2bOHPq3Mmzpw0EQIOKWBAU6AKfUhQUCBDAgAIRDRIwDYCgwc4HFh5MWcD0KImlBXpC6HBha9cSYHlOsMBhQ4YIQbgG8Doi7c4LGjZwyCAhLtMDRREMCBD2KgQMGbT6DUCggGPHAgj7xIDB7Fy0knti0ABBily6IuxezdCh8mLQDkTrpICCw4QgUgMwIAEgMgGrOiVsUKKhr43IUwWIODCVaYKcD0iv4osUzgMMq1aVbT6lwioKF1ZZ6Ez9B2sl07OD725DwnIS4lFMJ7%2BCwm4UG%2BA6UGxhOXf2JCa878BBAtYLAF4gwXdKvIafCBBooMT%2FBhQot8FuHHSQQX2rVKCYYkFgKM9zq2DQVgUQYLXdBBpwwNZyAV5gwQS%2BlSCBBRZUgF2K91ETgXL8ZbABBhto8BoGEzxgnYQURBhddBwkqaSRR6JgIDc4dlBkVhFcwEEFKrIFYwcS3Njkl2B2oOCTwKTXgQVFyudABSUmicGWFDgAQZRh1tlBjWNoKAWHrVXgwAVvzVeBBeIpmYGJInjIInYZNNqoio4CmAFlGFxQAQcYtNiHnkBAYKaff7b1oFtKANnZlXJqYFoJD%2BiplYbJrYeIrEFMsMoGvgE6wa4SINaoBnyZ91oEZI1QqQWbZUACjIRegGEGtMKxAZ4%2FcGgi%2F4MIYqDVYW9lMMGNpPZFbJwiHCpmjyM8oEGJu5EArSUTcmrDc85iW64GF1CmlY%2FzkcaZA8TeN6mYeI1QQY9tdfBstGOQKG8QDCoWgYKBOrCBhSRKKYJ57lrglgUajIBXvvUJzPAYlSISMQlsujWBiR5yQIFitqY7qaqbiczBuiWOkODJYkAwbR8akEuCp3qVqmnNCB46qrIiePwgkCOwZnQlgPaRAagqrBWjntkdTW0MF2ywSQS4PlzDxSY80GujZI6QQchw4Kv2GFmPYa%2FP0LWVZNwOvEtDqyI8EAGGGljQSYIzi7GysWI6YMHErpGAtrOsllA2ZQqumvjdY4A89v%2FajYtQ5IQOcBBBfXSLcLkI%2BQb4prHA8obgBlkh8gAEVztAmeOlSz5pVh14ycHRdlscHdS%2BG7ku1Lpx3fBmx1vOrxTXl4tsVtOqysF90Sca3XTJwbgjpiK8LL0Uh%2B1sgaZRDw1x8AyiHl%2Fi348w8XRDKgE1BM6jDLkmtr5qSSAvinPBpKSwN3VNAECBSw5rEpgq01TAXCigoOFCVIKX5Q4IhoNOb2gQPdCRIHsO6A2A2HTAS8mKNCK4FJN617YJzucHEiAN6mygH%2F%2F8YG8WA1CMNHAp0lQGAhXwUgUm0D8UcA1WzILOmywALTWpwDp8MWG6qDg6B3wLdh1I4vCYxAH2ADGpbPwxUpKQ1CNmNSppbukRCtZVRhW4TUcfzBBi5CXDB%2B1sjY7K1AR4tBdGAYtRlsLLxybgKAXN0VEUgEsEICCBCUAAAodTQZE0kEkx3OhfmqMAdirQJQoAqVUaioAE7jM2w%2BkJArzSIqtyiCkrenJutrzJA05HwzEcxkeyfMcDyradTRAzmNTIIds8MaUuykM%2FvUGmFEgEynxYaVWhUCb85AGdPIriOcCsB4keVwqs4E6ansDKXgBXioNdwJmjaJCJ0KkJCqgql6SIgIeYYw5GvoWeYnjAIB9EwXf8spOfUCVi2nIBfHJjmMvcBBJ1xB9LfSIEADs%3D",
	};

        /*
         * Methods to decorate the Fight page
         */

	public static final void addNunneryMeat( AdventureResult result )
	{
		int delta = result.getCount();
		lastNunneryMeat = currentNunneryMeat;
		currentNunneryMeat = KoLSettings.incrementIntegerProperty( "currentNunneryMeat", delta, 100000, false );
	}

	public static final void decorateThemtharFight( StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
			return;

		int current = currentNunneryMeat;
		if ( current < 100000 )
		{
			int left = 100000 - current;
			int delta = current - lastNunneryMeat;
			int turns = (int)Math.ceil( (double)left / (double) delta );
			String message = "<p><center>" + 
				COMMA_FORMAT.format( current ) + " Meat recovered, " +
				COMMA_FORMAT.format( left ) + " left (" +
				turns + " turns).<br>";

			buffer.insert( index, message );
		}

		// "Well," you say, "it would really help the war effort if
		// your convent could serve as a hospital for our wounded
		// troops."
		if ( buffer.indexOf( "could serve as a hospital" ) != -1 )
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "hippy" );

		// "Well," you say, "it would really help the war effort if
		// your convent could serve as a massage parlor. Y'know... to
		// help our troops relax."
		else if ( buffer.indexOf( "could serve as a massage parlor" ) != -1  )
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "fratboy" );
	}

	private static final String [] GREMLIN_TOOLS =
	{
		"It whips out a hammer",
		"He whips out a crescent wrench",
		"It whips out a pair of pliers",
		"It whips out a screwdriver",
	};

	public static final void decorateGremlinFight( StringBuffer buffer )
	{
		// Color the tool in the monster spoiler text
		if ( missingGremlinTool == null && !currentJunkyardTool.equals( "" ) )
			StaticEntity.singleStringReplace( buffer, currentJunkyardTool, "<font color=#DD00FF>" + currentJunkyardTool + "</font>" );

		for ( int i = 0; i < GREMLIN_TOOLS.length; ++i)
		{
			String tool = GREMLIN_TOOLS[i];
			StaticEntity.singleStringReplace( buffer, tool, "<font color=#DD00FF>" + tool + "</font>" );
		}
	}

	public static final void appendMissingGremlinTool( StringBuffer buffer )
	{
		if ( missingGremlinTool != null )
			buffer.append( "<br />This gremlin does <b>NOT</b> have a " + missingGremlinTool );
	}

	private static final int [] AREA_UNLOCK =
	{
		64,
		192,
		458
	};

	private static final String [] HIPPY_AREA_UNLOCK =
	{
		"Lighthouse",
		"Junkyard",
		"Arena"
	};

	private static final String [] FRATBOY_AREA_UNLOCK =
	{
		"Orchard",
		"Nunnery",
		"Farm"
	};

	private static final String openArea( int last, int current, String [] areas )
	{
		for ( int i = 0; i < AREA_UNLOCK.length; ++i)
		{
			int threshold = AREA_UNLOCK[i];
			if ( last < threshold && current >= threshold )
				return areas[i];
		}
		return null;
	}

	public static final void decorateBattlefieldFight( StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
			return;

		// Don't bother showing progress of the war if you've just won
		String monster = FightRequest.getLastMonsterName();
		if ( monster.equalsIgnoreCase( "Big Wisniewski" ) || monster.equalsIgnoreCase( "Man" ) )
			return;

		String side;
		int delta;
		int last;
		int current;
		String area;

		if ( fratboy )
		{
			last = lastFratboysDefeated;
			current = fratboysDefeated;
			delta = current - last;
			side = ( delta == 1 ) ? "frat boy" : "frat boys";
			area = openArea( last, current, HIPPY_AREA_UNLOCK );
		}
		else
		{
			last = lastHippiesDefeated;
			current = hippiesDefeated;
			delta = current - last;
			side = ( delta == 1 ) ? "hippy" : "hippies";
			area = openArea( last, current, FRATBOY_AREA_UNLOCK );
		}

                area = ( area == null ) ? "" : ( " The " + area + " is now accessible in this uniform!<br>" );
		String message = "<p><center>" + delta + " " + side + " defeated; " + current + " down, " + ( 1000 - current ) + " left.<br>" + area;

		buffer.insert( index, message );
	}

        /*
         * Method to decorate the Big Island map
         */

	// Decorate the HTML with custom goodies
	public static final void decorateBigIsland( String url, StringBuffer buffer )
	{
		// Quest-specific page decorations
		decorateJunkyard( buffer );

		// Find the table that contains the map.
		String fratboyMessage = sideSummary( "frat boys", fratboysDefeated, fratboyImage, fratboyMin, fratboyMax );
		String hippyMessage = sideSummary( "hippies", hippiesDefeated, hippyImage, hippyMin, hippyMax );
		String row = "<tr><td><center><table width=100%><tr>" +
			progressLineStyle + fratboyMessage + "</td>" +
			progressLineStyle + hippyMessage + "</td>" +
			"</tr></table></td></tr>";

		int tableIndex = buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Mysterious Island of Mystery</b></td>" );
		if ( tableIndex != -1 )
			buffer.insert( tableIndex, row );

                // Now replace sidequest location images for completed quests
                sidequestImage( buffer, "sidequestArenaCompleted", ARENA );
                sidequestImage( buffer, "sidequestFarmCompleted", FARM );
                sidequestImage( buffer, "sidequestJunkyardCompleted", JUNKYARD );
                sidequestImage( buffer, "sidequestLighthouseCompleted", LIGHTHOUSE );
                sidequestImage( buffer, "sidequestNunsCompleted", NUNS );
                sidequestImage( buffer, "sidequestOrchardCompleted", ORCHARD );
	}

	private static final String sideSummary( String side, int kills, int image, int min, int max )
	{
		if ( kills > min )
			min = kills;
                int minLeft = 1000 - max;
                int maxLeft = 1000 - min;
                String range = ( minLeft == maxLeft ) ? String.valueOf( minLeft ) : ( String.valueOf( minLeft ) + "-" + String.valueOf( maxLeft ) );
                return kills + " " + side + " defeated; " + range + " left (image " + image + ").";
	}

	private static final void sidequestImage( StringBuffer buffer, String setting, int quest )
	{
		// Firefox and Safari can handle this. Internet Exploder can't
		// All the user to opt out of inline image replacement.
		if ( !KoLSettings.getBooleanProperty( "relayMarksSidequests" ) )
			return;

		String status = KoLSettings.getUserProperty( setting );
		String image;
		if ( status.equals( "fratboy" ) )
			image = FRAT_IMAGES[quest];
		else if ( status.equals( "hippy" ) )
			image = HIPPY_IMAGES[quest];
		else
			return;

		String old = SIDEQUEST_IMAGES[quest];
		StaticEntity.singleStringReplace( buffer, old, image );
	}

	public static final void decorateJunkyard( StringBuffer buffer )
	{
		if ( currentJunkyardTool.equals( "" ) )
			return;

		int tableIndex = buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Junkyard</b></td>" );
		if ( tableIndex == -1 )
			return;

		String row = "<tr><td><center><table width=100%><tr>" +
			progressLineStyle + "Look for the " +
			currentJunkyardTool + " " +
			currentJunkyardLocation + ".</td>" +
			"</tr></table></td></tr>";

		buffer.insert( tableIndex, row );
	}

	public static final void startFight()
	{
		missingGremlinTool = null;
	}

        /*
         * Methods to mine data from request responses
         */

	public static void handleGremlin( String responseText )
	{
		// Batwinged Gremlin has molybdenum hammer OR
		// "It does a bombing run over your head..."

		// Erudite Gremlin has molybdenum crescent wrench OR
		// "He uses the random junk around him to make an automatic
		// eyeball-peeler..."

		// Spider Gremlin has molybdenum pliers OR
		// "It bites you in the fibula with its mandibles..."

		// Vegetable Gremlin has molybdenum screwdriver OR
		// "It picks a <x> off of itself and beats you with it..."

		String text = responseText;
		if ( text.indexOf( "bombing run" ) != -1 )
			missingGremlinTool = "molybdenum hammer";
		else if ( text.indexOf( "eyeball-peeler" ) != -1 )
			missingGremlinTool = "molybdenum crescent wrench";
		else if ( text.indexOf( "fibula" ) != -1 )
			missingGremlinTool = "molybdenum pliers";
		else if ( text.indexOf( "off of itself" ) != -1 )
			missingGremlinTool = "molybdenum screwdriver";
	}

	private static final String [][] HIPPY_MESSAGES =
	{
		// 2 total
		{
                        // You see one of your frat brothers take out an
                        // M.C. Escher drawing and show it to a War Hippy
                        // (space) Cadet. The hippy looks at it and runs away
                        // screaming about how he doesn't know which way is
                        // down.
			"M.C. Escher",

                        // You see a hippy loading his didgeridooka, but before
                        // he can fire it, he's dragged off the battlefield by
                        // another hippy protesting the war.
			"protesting the war",

                        // You see a "Baker Company" hippy take one bite too
                        // many from a big plate of brownies, then curl up to
                        // take a nap. Looks like he's out of commission for a
                        // while.
			"Baker Company",

                        // You see a hippy a few paces away suddenly realize
                        // that he's violating his deeply held pacifist
                        // beliefs, scream in horror, and run off the
                        // battlefield.
			"pacifist beliefs",

                        // You look over and see a fellow frat brother
                        // garotting a hippy shaman with the hippy's own
                        // dreadlocks. "Right on, bra!" you shout.
			"garotting",

                        // You glance over and see one of your frat brothers
                        // hosing down a hippy with soapy water. You laugh and
                        // run over for a high-five.
			"soapy water",

                        // You glance out over the battlefield and see a hippy
                        // from the F.R.O.G. division get the hiccups and knock
                        // himself out on his own nasty breath.
			"nasty breath",

                        // You see one of the War Hippy's "Jerry's Riggers"
                        // sneeze midway through making a bomb, inadvertently
                        // turning himself into smoke and dust. In the wind.
			"smoke and dust",

                        // You see a frat boy hose down a hippy Airborne
                        // Commander with sugar water. You applaud as the
                        // Commander gets attacked by her own ferrets.
			"sugar water",

                        // You see one of your frat brothers paddling a hippy
                        // who seems to be enjoying it. You say "uh, keep up
                        // the good work... bra... yeah."
			"enjoying it",

                        // As the hippy falls, you see a hippy a few yards away
                        // clutch his chest and fall over, too. Apparently the
                        // hippy you were fighting was just the astral
                        // projection of another hippy several yards
                        // away. Freaky.
			"astral projection",
		},
		// 4 total
		{ 
                        // You see a War Frat Grill Sergeant hose down three
                        // hippies with white-hot chicken wing sauce. You love
                        // the smell of jabañero in the morning. It smells like
                        // victory.
			"three hippies",

                        // As you finish your fight, you see a nearby Wartender
                        // mixing up a cocktail of vodka and pain for a trio of
                        // charging hippies. "Right on, bra!" you shout.
			"vodka and pain",

                        // You see one of your frat brothers douse a trio of
                        // nearby hippies in cheap aftershave. They scream and
                        // run off the battlefield to find some incense to
                        // burn.
			"cheap aftershave",

                        // You see one of your frat brothers line up three
                        // hippies for simultaneous paddling. Don't bathe --
                        // that's a paddlin'. Light incense -- that's a
                        // paddlin'. Paddlin' a homemade canoe -- oh, you
                        // better believe that's a paddlin'.
			// "three hippies",

                        // You see one of the "Fortunate 500" make a quick call
                        // on his cell phone. Some mercenaries drive up, shove
                        // three hippies into their bitchin' meat car, and
                        // drive away.
			// "three hippies",

                        // As you deliver the finishing blow, you see a frat
                        // boy lob a sake bomb into a trio of nearby
                        // hippies. "Nice work, bra!" you shout.
			"lob a sake bomb",
		},
		// 8 total
		{ 
                        // You see one of your Beer Bongadier frat brothers use
                        // a complicated beer bong to spray cheap, skunky beer
                        // on a whole squad hippies at once. "Way to go, bra!"
                        // you shout.
			"skunky beer",

                        // You glance over and see one of the Roaring Drunks
                        // from the 151st Division overturning a mobile sweat
                        // lodge in a berserker rage. Several sweaty, naked
                        // hippies run out and off the battlefield, brushing
                        // burning coals out of their dreadlocks.
			"burning coals",

                        // You see one of your frat brothers punch an
                        // F.R.O.G. in the solar plexus, then aim the
                        // subsequent exhale at a squad of hippies standing
                        // nearby. You watch all of them fall to the ground,
                        // gasping for air.
			"solar plexus",

                        // You see a Grillmaster flinging hot kabobs as fast as
                        // he can make them. He skewers one, two, three, four,
                        // five, six... seven! Seven hippies! Ha ha ha!
			"hot kabobs",
		},
		// 16 total
		{ 
                        // A streaking frat boy runs past a nearby funk of
                        // hippies. One look at him makes the hippies have to
                        // go ponder their previous belief that the naked human
                        // body is a beautiful, wholesome thing.
			"naked human body",

                        // You see one of the Fortunate 500 call in an air
                        // strike. His daddy's personal airship flies over and
                        // dumps cheap beer all over a nearby funk of hippies.
			"personal airship",

                        // You look over and see a platoon of frat boys round
                        // up a funk of hippies and take them prisoner. Since
                        // being a POW of the frat boys involves a lot of beer
                        // drinking, you're slightly envious. Since it also
                        // involves a lot of paddling, you're somewhat less so.
			"slightly envious",

                        // You see a kegtank and a mobile sweat lodge facing
                        // off in the distance. Since the kegtank's made of
                        // steel and the sweat lodge is made of wood, you can
                        // guess the outcome.
			"guess the outcome",
		},
		// 32 total
		{ 
                        // You see an entire regiment of hippies throw down
                        // their arms (and their weapons) in disgust and walk
                        // off the battlefield. War! What is it good for?
                        // Absolutely nothing!
			"Absolutely nothing",

                        // You see a squadron of police cars drive up, and a
                        // squad of policemen arrest a funk of hippies who were
                        // sitting around inhaling smoke from some sort of
                        // glass sculpture.
			"glass sculpture",

                        // You see a kegtank rumble through the battlefield,
                        // firing beer cans out of its top turret. It mows
                        // down, like, 30 hippies in a row, but then runs out
                        // of ammo. They really should have stocked one more
                        // six-pack.
			"one more six-pack",
		},
		// 64 total
		{ 
                        // You see the a couple of frat boys attaching big,
                        // long planks of wood to either side of a
                        // kegtank. Then they drive through the rank hippy
                        // ranks, mass-paddling as they go. Dozens of hippies
                        // flee the battlefield, tears in their filthy, filthy
                        // eyes.
			"planks of wood",

                        // You see one of the "Fortunate 500" hang up his PADL
                        // phone, looking smug. Several SWAT vans of police in
                        // full riot gear pull up, and one of them informs the
                        // hippies through a megaphone that this is not a
                        // "designated free speech zone." The hippies throw
                        // rocks and bottles at the police, but most of them
                        // end up shoved into paddy wagons in chains. Er, the
                        // hippies are the ones in the chains. Not the wagons.
			"SWAT vans",

                        // You see a couple of frat boys stick a fuse into a
                        // huge wooden barrel, light the fuse, and roll it down
                        // the hill to where the hippy forces are
                        // fighting. Judging by the big bada boom that follows,
                        // that barrel was either full of scotch or gunpowder,
                        // and possibly both.
			"wooden barrel",
		},
	};

	private static final String [][] FRAT_MESSAGES =
	{
		// 2 total
		{
                        // You look over and see a fellow hippy warrior using
                        // his dreadlocks to garotte a frat warrior. "Way to
                        // enforce karmic retribution!" you shout.
			"garotte",

                        // You see a Green Gourmet give a frat boy a plate of
                        // herbal brownies. The frat boy scarfs them all, then
                        // wanders off staring at his hands.
			"herbal brownies",

                        // Elsewhere on the battlefield, you see a fellow hippy
                        // grab a frat warrior's paddle and give the frat boy a
                        // taste of his own medicine. I guess that could count
                        // as homeopathic healing...
			"homeopathic healing",

                        // You see a Wartender pour too much lighter fluid on
                        // his grill and go up in a great ball of
                        // fire. Goodness gracious!
			"lighter fluid",

                        // You see a Fire Spinner blow a gout of flame onto a
                        // Wartender's grill, charring all the Wartender's
                        // meaty goodness. The Wartender wanders off crying.
			"meaty goodness",

                        // Nearby, you see one of your sister hippies
                        // explaining the rules of Ultimate Frisbee to a member
                        // of the frat boys' "armchair infantry." His eyes
                        // glaze and he passes out.
			"Ultimate Frisbee",

                        // You see a member of the frat boy's 151st division
                        // pour himself a stiff drink, knock it back, and
                        // finally pass out from alcohol poisoning.
			"alcohol poisoning",

                        // You glance over your shoulder and see a squadron of
                        // winged ferrets descend on a frat warrior, entranced
                        // by the sun glinting off his keg shield.
			"entranced by the sun",

                        // You see a hippy shaman casting a Marxist spell over
                        // a member of the "Fortunate 500" division of the frat
                        // boy army. The frat boy gets on his cell phone and
                        // starts redistributing his wealth.
			"Marxist spell",

                        // You see a frat boy warrior pound a beer, smash the
                        // can against his forehead, and pass out. You chuckle
                        // to yourself.
			"smash the can",

                        // You see an F.R.O.G. crunch a bulb of garlic in his
                        // teeth and breathe all over a nearby frat boy, who
                        // turns green and falls over.
			"bulb of garlic",
		},
		// 4 total
		{ 
                        // You hear chanting behind you, and turn to see thick,
                        // ropy (almost anime-esque) vines sprout from a War
                        // Hippy Shaman's dreads and entangle three attacking
                        // frat boy warriors.
			"three attacking",

                        // Nearby, you see an Elite Fire Spinner take down
                        // three frat boys in a whirl of flame and pain.
			"three frat boys",

                        // You look over and see three ridiculously drunk
                        // members of the 151st Division run together for a
                        // three-way congratulatory headbutt, which turns into
                        // a three-way concussion.
			"three-way",

                        // You see a member of the Fortunate 500 take a phone
                        // call, hear him holler something about a stock market
                        // crash, then watch him and two of his fortunate
                        // buddies run off the battlefield in a panic.
			"stock market crash",

                        // Over the next hill, you see three frat boys abruptly
                        // vanish into a cloud of green smoke. Apparently the
                        // Green Ops Soldiers are on the prowl.
			// "three frat boys",

                        // You hear excited chittering overhead, and look up to
                        // see a squadron of winged ferrets making a
                        // urine-based bombing run over three frat boys. The
                        // frat boys quickly run off the field to find some
                        // cheap aftershave to cover up the smell.
			// "three frat boys",
		},
		// 8 total
		{ 
                        // Nearby, a War Hippy Elder Shaman nods almost
                        // imperceptibly. A Kegtank hits a gopher hole and tips
                        // over. A squad of confused frat boys stumble out and
                        // off the battlefield.
			"gopher hole",

                        // You leap out of the way of a runaway Mobile Sweat
                        // Lodge, then watch it run over one, two, three, four,
                        // five, six, seven! Seven frat boys! Ha ha ha!
			"runaway",

                        // A few yards away, one of the Jerry's Riggers hippies
                        // detonates a bomb underneath a Wartender's grill. An
                        // entire squad of frat boys run from the battlefield
                        // under the onslaught of red-hot coals.
			"red-hot coals",

                        // You look over and see one of Jerry's Riggers placing
                        // land mines he made out of paperclips, rubber bands,
                        // and psychedelic mushrooms. A charging squad of frat
                        // boys trips them, and is subsequently dragged off the
                        // field ranting about the giant purple squirrels.
			"purple squirrels",
		},
		// 16 total
		{ 
                        // You turn to see a nearby War Hippy Elder Shaman
                        // making a series of complex hand gestures. A flock of
                        // pigeons swoops down out of the sky and pecks the
                        // living daylights out of a whole platoon of frat
                        // boys.
			"platoon of",

                        // You see a platoon of charging frat boys get mowed
                        // down by a hippy. Remember, kids, a short-range
                        // weapon (like a paddle) usually does poorly against a
                        // long-range weapon (like a didgeridooka).
			// "platoon of",

                        // You look over and see a funk of hippies round up a
                        // bunch of frat boys to take as prisoners of
                        // war. Since being a hippy prisoner involves lounging
                        // around inhaling clouds of smoke and eating brownies,
                        // you're somewhat jealous. Since it also involves
                        // non-stop olfactory assault, you're somewhat less so.
			"funk of hippies",

                        // Nearby, a platoon of frat boys is rocking a mobile
                        // sweat lodge back and forth, trying to tip it
                        // over. When they succeed, they seem surprised by the
                        // hot coals and naked hippies that pour forth, and the
                        // frat boys run away screaming.
			// "platoon of",
		},
		// 32 total
		{ 
                        // A mobile sweat lodge rumbles into a regiment of frat
                        // boys and the hippies inside open all of its vents
                        // simultaneously. Steam that smells like a dozen
                        // baking (and baked) hippies pours out, enveloping the
                        // platoon and sending the frat boys into fits of
                        // nauseated coughing.
			"regiment",

                        // You see a squadron of police cars drive up, and a
                        // squad of policemen arrest an entire regiment of frat
                        // boys. You hear cries of "She told me she was 18,
                        // bra!" and "I told you, I didn't hit her with a
                        // roofing shingle!" as they're dragged off the
                        // battlefield.
			// "regiment",

                        // You see a regiment of frat boys decide they're tired
                        // of drinking non-alcoholic beer and tired of not
                        // hitting on chicks, so they throw down their arms,
                        // and then their weapons, and head back to the frat
                        // house.
			// "regiment",
		},
		// 64 total
		{ 
                        // You see an airborne commander trying out a new
                        // strategy: she mixes a tiny bottle of rum she found
                        // on one of the frat boy casualties with a little of
                        // the frat boy's blood, then adds that to the ferret
                        // bait. A fleet of ferrets swoops down, eats the bait,
                        // and goes berserk with alcohol/bloodlust. The frat
                        // boys scream like schoolgirls as the ferrets decimate
                        // their ranks.
			"scream like schoolgirls",

                        // You see a couple of hippies rigging a mobile sweat
                        // lodge with a public address system. They drive it
                        // through the battlefield, blaring some concept album
                        // about the dark side of Ronald. Frat boys fall asleep
                        // en masse, helpless before music that's horribly
                        // boring if you're not under the influence of
                        // mind-altering drugs.
			"en masse",

                        // You see an elder hippy shaman close her eyes, clench
                        // her fists, and start to chant. She glows with an
                        // eerie green light as storm clouds bubble and roil
                        // overhead. A funnel cloud descends from the
                        // thunderheads and dances through the frat boy ranks,
                        // whisking them up and away like so many miniature
                        // mobile homes.
			"mobile homes",
		},
	};

	private static final boolean findBattlefieldMessage( String responseText, String [] table )
	{
		for ( int i = 0; i < table.length; ++i)
			if ( responseText.indexOf( table[i] ) != -1 )
				return true;
		return false;
	}

	public static final void handleBattlefield( String responseText )
	{
		// Nothing to do until battle is done
		if ( responseText.indexOf( "WINWINWIN" ) == -1 )
			return;

                // Just in case
		PrintStream sessionStream = RequestLogger.getSessionStream();

		// We only count known monsters
		MonsterDatabase.Monster monster = FightRequest.getLastMonster();
		if ( monster == null )
		{
			// The monster is not in the monster database.
			sessionStream.println( "Unknown monster found on battlefield: " + FightRequest.getLastMonsterName());
			return;
		}

		// Decide whether we defeated a hippy or a fratboy warrior
		if ( fratboyBattlefield.hasMonster( monster ) )
			fratboy = false;
		else if ( hippyBattlefield.hasMonster( monster ) )
			fratboy = true;
		else
		{
			// Known but unexpected monster on battlefield.
			sessionStream.println( "Unexpected monster found on battlefield: " + FightRequest.getLastMonsterName());
			return;
		}

		// Initialize settings if necessary
		ensureUpdatedBigIsland();

		// Figure out how many enemies were defeated
		String [][] table = fratboy ? FRAT_MESSAGES : HIPPY_MESSAGES;

		int quests = 0;
		int delta = 1;
		int test = 2;

		for ( int i = 0; i < table.length; ++i)
		{
			if ( findBattlefieldMessage( responseText, table[i] ) )
			{
				quests = i + 1;
				delta = test;
				break;
			}
			test *= 2;
		}

		lastFratboysDefeated = fratboysDefeated;
		lastHippiesDefeated = hippiesDefeated;

		if ( fratboy )
		{
			fratboysDefeated = KoLSettings.incrementIntegerProperty( "fratboysDefeated", delta, 1000, false );
			fratboyDelta = delta;
			hippyQuestsCompleted = quests;
		}
		else
		{
			hippiesDefeated = KoLSettings.incrementIntegerProperty( "hippiesDefeated", delta, 1000, false );
			hippyDelta = delta;
			fratboyQuestsCompleted = quests;
		}
	}

	// Crowther spaded how many kills it takes to display an image in:
	// http://jick-nerfed.us/forums/viewtopic.php?p=58270#58270

	private static final int [] IMAGES =
	{
		0,	// Image 0
		3,	// Image 1
		9,	// Image 2
		17,	// Image 3
		28,	// Image 4
		40,	// Image 5
		52,	// Image 6
		64,	// Image 7
		80,	// Image 8
		96,	// Image 9
		114,	// Image 10
		132,	// Image 11
		152,	// Image 12
		172,	// Image 13
		192,	// Image 14
		224,	// Image 15
		258,	// Image 16
		294,	// Image 17
		332,	// Image 18
		372,	// Image 19
		414,	// Image 20
		458,	// Image 21
		506,	// Image 22
		556,	// Image 23
		606,	// Image 24
		658,	// Image 25
		711,	// Image 26
		766,	// Image 27
		822,	// Image 28
		880,	// Image 29
		939,	// Image 30
		999,	// Image 31
		1000	// Image 32
	};

	public static final void parseBigIsland( String location, String responseText )
	{
		if ( !location.startsWith( "bigisland.php" ) )
			return;

		// Set variables from user settings
		ensureUpdatedBigIsland();

		// Parse the map and deduce how many soldiers remain
		parseBattlefield( responseText );

		// Deduce things about quests
		quest = parseQuest( location );

		switch ( quest )
		{
		case ARENA:
			parseArena( responseText );
			break;
		case JUNKYARD:
			parseJunkyard( responseText );
			break;
		case ORCHARD:
			parseOrchard( responseText );
			break;
		case FARM:
			parseFarm( responseText );
			break;
		case NUNS:
			parseNunnery( responseText );
			break;
		case LIGHTHOUSE:
			parseLighthouse( responseText );
			break;
		}
	}

	private static final int parseQuest( String location )
	{
		if ( location.indexOf( "place=concert") != -1 )
			return ARENA;

		if ( location.indexOf( "action=junkman") != -1 )
			return JUNKYARD;

		if ( location.indexOf( "action=stand") != -1 )
			return ORCHARD;

		if ( location.indexOf( "action=farmer") != -1 )
			return FARM;

		if ( location.indexOf( "place=nunnery") != -1 )
			return NUNS;

		if ( location.indexOf( "action=pyro") != -1 )
			return LIGHTHOUSE;

		return NONE;
	}

	private static final void parseBattlefield( String responseText )
	{
		Matcher matcher = MAP_PATTERN.matcher( responseText );
		if ( !matcher.find() )
			return;

		fratboyImage = StaticEntity.parseInt( matcher.group(1) );
		hippyImage = StaticEntity.parseInt( matcher.group(2) );

		if ( fratboyImage >= 0 && fratboyImage <= 32 )
		{
			fratboyMin = IMAGES[ fratboyImage ];
			if ( fratboyMin == 1000 )
				fratboyMax = 1000;
			else
				fratboyMax = IMAGES[ fratboyImage + 1 ] - 1;
		}

		if ( hippyImage >= 0 && hippyImage <= 32 )
		{
			hippyMin = IMAGES[ hippyImage ];
			if ( hippyMin == 1000 )
				hippyMax = 1000;
			else
				hippyMax = IMAGES[ hippyImage + 1 ] - 1;
		}

		// Consistency check settings against map
		if ( fratboysDefeated < fratboyMin )
		{
			fratboysDefeated = fratboyMin;
			KoLSettings.setUserProperty( "fratboysDefeated", String.valueOf( fratboysDefeated ) );
		}
		else if ( fratboysDefeated > fratboyMax )
		{
			fratboysDefeated = fratboyMax;
			KoLSettings.setUserProperty( "fratboysDefeated", String.valueOf( fratboysDefeated ) );
		}

		if ( hippiesDefeated < hippyMin )
		{
			hippiesDefeated = hippyMin;
			KoLSettings.setUserProperty( "hippiesDefeated", String.valueOf( hippiesDefeated ) );
		}
		else if ( hippiesDefeated > hippyMax )
		{
			hippiesDefeated = hippyMax;
			KoLSettings.setUserProperty( "hippiesDefeated", String.valueOf( hippiesDefeated ) );
		}
	}

	private static final void parseArena( String responseText )
	{
		// You roll up to the amphitheater and see that the Goat Cheese
		// Occurence is well into the first song of their four-hour,
		// one-song set.
		if ( responseText.indexOf( "well into the first song" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "hippy" );
			return;
		}

		// "Hey, man," he says laconically. "You did a, like, totally
		// awesome job promoting the concert, man. If you have any
		// flyers left, I'll take 'em; we can use them at the next
		// show. Speaking of which, they're hitting the stage in just a
		// couple of minutes -- you should come back in a few and check
		// 'em out. It's a totally awesome show, man."
		if ( responseText.indexOf( "I'll take 'em" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "hippy" );
			if ( KoLCharacter.hasItem( JAM_FLYERS ) )
				StaticEntity.getClient().processResult( JAM_FLYERS );
			return;
		}
		
		// You roll up to the amphitheater and see that Radioactive
		// Child has already taken the stage.
		if ( responseText.indexOf( "has already taken the stage" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "fratboy" );
			return;
		}

		// "Hey, bra," he says, "you did excellent work promoting the
		// show. If you have any flyers left, I'll take them; we can
		// use them at the next show."
		if ( responseText.indexOf( "I'll take them" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "fratboy" );
			if ( KoLCharacter.hasItem( JAM_FLYERS ) )
				StaticEntity.getClient().processResult( ROCK_FLYERS );
			return;
		}

		// The stage at the Mysterious Island Arena is empty.

		if ( responseText.indexOf( "The stage at the Mysterious Island Arena is empty" ) != -1 )
		{
			// Didn't complete quest or defeated the side you
			// advertised for.
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "none" );
		}
	}

	private static final void parseJunkyard( String responseText )
	{
		String tool = currentJunkyardTool;
		String location = currentJunkyardLocation;
		boolean done = false;

		// The last time I saw my <tool> it was <location>.
		//
		//	next to that barrel with something burning in it
		//	near an abandoned refrigerator
		//	over where the old tires are
		//	out by that rusted-out car

		Matcher matcher = JUNKYARD_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			tool = matcher.group(1);
			tool = "molybdenum " + ( tool.equals( "wrench" ) ? "crescent " : "" ) + tool;
			location = matcher.group(3);
		}

		// As you turn to walk away, he taps you on the shoulder. "I
		// almost forgot. I made this while you were off getting my
		// tools. It was boring, but I figure the more time I spend
		// bored, the longer my life will seem. Anyway, I don't really
		// want it, so you might as well take it."

		else if ( responseText.indexOf( "I made this while you were off getting my tools" ) != -1 )
		{
			tool = "";
			done = true;
		}

		if ( tool != currentJunkyardTool )
		{
			currentJunkyardTool = tool;
			KoLSettings.setUserProperty( "currentJunkyardTool", tool );
			currentJunkyardLocation = location;
			KoLSettings.setUserProperty( "currentJunkyardLocation", location );
		}

		if ( !done )
			return;

		// Give the magnet and the tools to Yossarian
		StaticEntity.getClient().processResult( MAGNET );
		StaticEntity.getClient().processResult( HAMMER );
		StaticEntity.getClient().processResult( PLIERS );
		StaticEntity.getClient().processResult( WRENCH );
		StaticEntity.getClient().processResult( SCREWDRIVER );

		if ( responseText.indexOf( "spark plug earring" ) != -1 ||
		     responseText.indexOf( "woven baling wire bracelets" ) != -1 ||
		     responseText.indexOf( "gearbox necklace" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestJunkyardCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "rusty chain necklace" ) != -1 ||
			  responseText.indexOf( "sawblade shield" ) != -1 ||
			  responseText.indexOf( "wrench bracelet" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestJunkyardCompleted", "fratboy" );
		}
	}

	private static final void parseOrchard( String responseText )
	{
		// "Is that... it is! The heart of the filthworm queen! You've
		// done it! You've freed our orchard from the tyranny of
		// nature!"
		if ( responseText.indexOf( "tyranny of nature" ) == -1 )
			return;

		String side = EquipmentDatabase.isWearingOutfit( 32 ) ? "hippy" : "fratboy";
		KoLSettings.resetUserProperty( "sidequestOrchardCompleted", side );
	}

	private static final void parseFarm( String responseText )
	{
		// "Well... How about dedicating a portion of your farm to
		// growing soybeans, to help feed the hippy army?"
		if ( responseText.indexOf( "growing soybeans" ) != -1 )
			KoLSettings.resetUserProperty( "sidequestFarmCompleted", "hippy" );

		// "Well... How about dedicating a portion of your farm to
		// growing hops, to make better beer for the frat army?"
		else if ( responseText.indexOf( "growing hops" ) != -1 )
			KoLSettings.resetUserProperty( "sidequestFarmCompleted", "fratboy" );
	}

	private static final void parseNunnery( String responseText )
	{
		// "Hello, weary Adventurer! Please, allow us to tend to your
		// wounds."
		if ( responseText.indexOf( "tend to your wounds" ) != -1 )
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "hippy" );

		// "Adventurer! You look so... so tense! Please, allow us to
		// use our skilled hands to give you a refreshing massage."
		else if ( responseText.indexOf( "refreshing massage" ) != -1 )
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "fratboy" );

		// "Hello, Adventurer. I'm afraid there's not much here at our
		// simple convent that would be of interest to a world-weary
		// traveler like yourself."		   
		else if ( responseText.indexOf( "world-weary traveler" ) != -1 )
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "none" );
	}

	private static final void parseLighthouse( String responseText )
	{
		// He gazes at you thoughtfully for a few seconds, then a smile
		// lights up his face and he says "My life... er... my bombs
		// for you. My bombs for you, bumpty-bumpty-bump!"
		if ( responseText.indexOf( "My bombs for you" ) == -1 )
			return;

		String side = EquipmentDatabase.isWearingOutfit( 32 ) ? "hippy" : "fratboy";
		KoLSettings.resetUserProperty( "sidequestLighthouseCompleted", side );
	}

	public static final void ensureUpdatedBigIsland()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			KoLSettings.setUserProperty( "lastBattlefieldReset", String.valueOf( KoLCharacter.getAscensions() ) );

			KoLSettings.setUserProperty( "fratboysDefeated", "0" );
			KoLSettings.setUserProperty( "hippiesDefeated", "0" );
			KoLSettings.setUserProperty( "sidequestArenaCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestFarmCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestJunkyardCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestLighthouseCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestNunsCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", "none" );
			KoLSettings.setUserProperty( "currentJunkyardTool", "" );
			KoLSettings.setUserProperty( "currentJunkyardLocation", "" );
			KoLSettings.setUserProperty( "currentNunneryMeat", "0" );
		}

		// Set variables from user settings

		fratboysDefeated = KoLSettings.getIntegerProperty( "fratboysDefeated" );
		hippiesDefeated = KoLSettings.getIntegerProperty( "hippiesDefeated" );
		currentJunkyardTool = KoLSettings.getUserProperty( "currentJunkyardTool" );
		currentJunkyardLocation = KoLSettings.getUserProperty( "currentJunkyardLocation" );
		currentNunneryMeat = KoLSettings.getIntegerProperty( "currentNunneryMeat" );
		lastNunneryMeat = currentNunneryMeat;
	}

	public static final void parsePostwarIsland( String location, String responseText )
	{
		if ( !location.startsWith( "postwarisland.php" ) )
			return;

		// Set variables from user settings
		ensureUpdatedPostwarIsland();

		// Deduce things about quests
		quest = parseQuest( location );

		switch ( quest )
		{
		case ARENA:
			parseArena( responseText );
			break;
		case NUNS:
			parseNunnery( responseText );
			break;
		}
	}

	public static final void ensureUpdatedPostwarIsland()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			KoLSettings.setUserProperty( "lastBattlefieldReset", String.valueOf( KoLCharacter.getAscensions() ) );

			KoLSettings.setUserProperty( "sidequestArenaCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", 
						     KoLSettings.getUserProperty( "currentHippyStore" ) );
			KoLSettings.setUserProperty( "sidequestNunsCompleted", "none" );
		}
	}

	public static final void decoratePostwarIsland( String url, StringBuffer buffer )
	{
		// Now replace sidequest location images for completed quests
		sidequestImage( buffer, "sidequestArenaCompleted", ARENA );
		sidequestImage( buffer, "sidequestNunsCompleted", NUNS );
	}
}
