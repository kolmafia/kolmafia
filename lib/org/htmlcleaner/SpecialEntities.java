/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.
	
    Redistribution and use of this software in source and binary forms, 
    with or without modification, are permitted provided that the following 
    conditions are met:
	
    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.
	
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.
	
    * The name of HtmlCleaner may not be used to endorse or promote 
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
	
    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "HtmlCleaner" in the
    subject line.
*/

package org.htmlcleaner;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>This class contains map with special entities used in HTML and their
 * unicodes.</p>
 * 
 * Created by: Vladimir Nikic<br/>
 * Date: November, 2006.
 */
public class SpecialEntities {
	
	static Map entities = new HashMap();

	static {
		entities.put("nbsp",	new Integer(160));
		entities.put("iexcl",	new Integer(161));
		entities.put("curren",	new Integer(164));
		entities.put("cent",	new Integer(162));
		entities.put("pound",	new Integer(163));
		entities.put("yen",		new Integer(165));
		entities.put("brvbar",	new Integer(166));
		entities.put("sect",	new Integer(167));
		entities.put("uml",		new Integer(168));
		entities.put("copy",	new Integer(169));
		entities.put("ordf",	new Integer(170));
		entities.put("laquo",	new Integer(171));
		entities.put("not",		new Integer(172));
		entities.put("shy",		new Integer(173));
		entities.put("reg",		new Integer(174));
		entities.put("trade",	new Integer(8482));
		entities.put("macr",	new Integer(175));
		entities.put("deg",		new Integer(176));
		entities.put("plusmn",	new Integer(177));
		entities.put("sup2",	new Integer(178));
		entities.put("sup3",	new Integer(179));
		entities.put("acute",	new Integer(180));
		entities.put("micro",	new Integer(181));
		entities.put("para",	new Integer(182));
		entities.put("middot",	new Integer(183));
		entities.put("cedil",	new Integer(184));
		entities.put("sup1",	new Integer(185));
		entities.put("ordm",	new Integer(186));
		entities.put("raquo",	new Integer(187));
		entities.put("frac14",	new Integer(188));
		entities.put("frac12",	new Integer(189));
		entities.put("frac34",	new Integer(190));
		entities.put("iquest",	new Integer(191));
		entities.put("times",	new Integer(215));
		entities.put("divide",	new Integer(247));

		entities.put("Agrave",	new Integer(192));
		entities.put("Aacute",	new Integer(193));
		entities.put("Acirc",	new Integer(194));
		entities.put("Atilde",	new Integer(195));
		entities.put("Auml",	new Integer(196));
		entities.put("Aring",	new Integer(197));
		entities.put("AElig",	new Integer(198));
		entities.put("Ccedil",	new Integer(199));
		entities.put("Egrave",	new Integer(200));
		entities.put("Eacute",	new Integer(201));
		entities.put("Ecirc",	new Integer(202));
		entities.put("Euml",	new Integer(203));
		entities.put("Igrave",	new Integer(204));
		entities.put("Iacute",	new Integer(205));
		entities.put("Icirc",	new Integer(206));
		entities.put("Iuml",	new Integer(207));
		entities.put("ETH",		new Integer(208));
		entities.put("Ntilde",	new Integer(209));
		entities.put("Ograve",	new Integer(210));
		entities.put("Oacute",	new Integer(211));
		entities.put("Ocirc",	new Integer(212));
		entities.put("Otilde",	new Integer(213));
		entities.put("Ouml",	new Integer(214));
		entities.put("Oslash",	new Integer(216));
		entities.put("Ugrave",	new Integer(217));
		entities.put("Uacute",	new Integer(218));
		entities.put("Ucirc",	new Integer(219));
		entities.put("Uuml",	new Integer(220));
		entities.put("Yacute",	new Integer(221));
		entities.put("THORN",	new Integer(222));
		entities.put("szlig",	new Integer(223));
		entities.put("agrave",	new Integer(224));
		entities.put("aacute",	new Integer(225));
		entities.put("acirc",	new Integer(226));
		entities.put("atilde",	new Integer(227));
		entities.put("auml",	new Integer(228));
		entities.put("aring",	new Integer(229));
		entities.put("aelig",	new Integer(230));
		entities.put("ccedil",	new Integer(231));
		entities.put("egrave",	new Integer(232));
		entities.put("eacute",	new Integer(233));
		entities.put("ecirc",	new Integer(234));
		entities.put("euml",	new Integer(235));
		entities.put("igrave",	new Integer(236));
		entities.put("iacute",	new Integer(237));
		entities.put("icirc",	new Integer(238));
		entities.put("iuml",	new Integer(239));
		entities.put("eth",		new Integer(240));
		entities.put("ntilde",	new Integer(241));
		entities.put("ograve",	new Integer(242));
		entities.put("oacute",	new Integer(243));
		entities.put("ocirc",	new Integer(244));
		entities.put("otilde",	new Integer(245));
		entities.put("ouml",	new Integer(246));
		entities.put("oslash",	new Integer(248));
		entities.put("ugrave",	new Integer(249));
		entities.put("uacute",	new Integer(250));
		entities.put("ucirc",	new Integer(251));
		entities.put("uuml",	new Integer(252));
		entities.put("yacute",	new Integer(253));
		entities.put("thorn",	new Integer(254));
		entities.put("yuml",	new Integer(255));

		entities.put("OElig",	new Integer(338));
		entities.put("oelig",	new Integer(339));
		entities.put("Scaron",	new Integer(352));
		entities.put("scaron",	new Integer(353));
		entities.put("Yuml",	new Integer(376));
		entities.put("circ",	new Integer(710));
		entities.put("tilde",	new Integer(732));
		entities.put("ensp",	new Integer(8194));
		entities.put("emsp",	new Integer(8195));
		entities.put("thinsp",	new Integer(8201));
		entities.put("zwnj",	new Integer(8204));
		entities.put("zwj",		new Integer(8205));
		entities.put("lrm",		new Integer(8206));
		entities.put("rlm",		new Integer(8207));
		entities.put("ndash",	new Integer(8211));
		entities.put("mdash",	new Integer(8212));
		entities.put("lsquo",	new Integer(8216));
		entities.put("rsquo",	new Integer(8217));
		entities.put("sbquo",	new Integer(8218));
		entities.put("ldquo",	new Integer(8220));
		entities.put("rdquo",	new Integer(8221));
		entities.put("bdquo",	new Integer(8222));
		entities.put("dagger",	new Integer(8224));
		entities.put("Dagger",	new Integer(8225));
		entities.put("hellip",	new Integer(8230));
		entities.put("permil",	new Integer(8240));
		entities.put("lsaquo",	new Integer(8249));
		entities.put("rsaquo",	new Integer(8250));
		entities.put("euro",	new Integer(8364));
	}
	
}