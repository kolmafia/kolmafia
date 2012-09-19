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

package net.sourceforge.kolmafia.utilities;

import java.util.HashMap;
import java.util.Map;

public class CharacterEntities
{
	// Overkill Unicode table borrowed from HTMLParser
	// http://htmlparser.sourceforge.net/

	private static final String[][] UNICODE_TABLE =
	{
		// Portions (c) International Organization for Standardization 1986
		// Permission to copy in any form is granted for use with
		// conforming SGML systems and applications as defined in
		// ISO 8879, provided this notice is included in all copies.
		// Character entity set. Typical invocation:

		// <!ENTITY % HTMLlat1 PUBLIC
		// "-//W3C//ENTITIES Latin 1//EN//HTML">
		// %HTMLlat1;

		{
			"&nbsp;",
			" "
		}, // no-break space = non-breaking space, U+00A0 ISOnum
		{
			"&iexcl;",
			"\u00a1"
		}, // inverted exclamation mark, U+00A1 ISOnum
		{
			"&cent;",
			"\u00a2"
		}, // cent sign, U+00A2 ISOnum
		{
			"&pound;",
			"\u00a3"
		}, // pound sign, U+00A3 ISOnum
		{
			"&curren;",
			"\u00a4"
		}, // currency sign, U+00A4 ISOnum
		{
			"&yen;",
			"\u00a5"
		}, // yen sign = yuan sign, U+00A5 ISOnum
		{
			"&brvbar;",
			"\u00a6"
		}, // broken bar = broken vertical bar, U+00A6 ISOnum
		{
			"&sect;",
			"\u00a7"
		}, // section sign, U+00A7 ISOnum
		{
			"&uml;",
			"\u00a8"
		}, // diaeresis = spacing diaeresis, U+00A8 ISOdia
		{
			"&copy;",
			"\u00a9"
		}, // copyright sign, U+00A9 ISOnum
		{
			"&ordf;",
			"\u00aa"
		}, // feminine ordinal indicator, U+00AA ISOnum
		{
			"&laquo;",
			"\u00ab"
		}, // left-pointing float angle quotation mark = left pointing guillemet, U+00AB ISOnum
		{
			"&not;",
			"\u00ac"
		}, // not sign, U+00AC ISOnum
		{
			"&shy;",
			"\u00ad"
		}, // soft hyphen = discretionary hyphen, U+00AD ISOnum
		{
			"&reg;",
			"\u00ae"
		}, // registered sign = registered trade mark sign, U+00AE ISOnum
		{
			"&macr;",
			"\u00af"
		}, // macron = spacing macron = overline = APL overbar, U+00AF ISOdia
		{
			"&deg;",
			"\u00b0"
		}, // degree sign, U+00B0 ISOnum
		{
			"&plusmn;",
			"\u00b1"
		}, // plus-minus sign = plus-or-minus sign, U+00B1 ISOnum
		{
			"&sup2;",
			"\u00b2"
		}, // superscript two = superscript digit two = squared, U+00B2 ISOnum
		{
			"&sup3;",
			"\u00b3"
		}, // superscript three = superscript digit three = cubed, U+00B3 ISOnum
		{
			"&acute;",
			"\u00b4"
		}, // acute accent = spacing acute, U+00B4 ISOdia
		{
			"&micro;",
			"\u00b5"
		}, // micro sign, U+00B5 ISOnum
		{
			"&para;",
			"\u00b6"
		}, // pilcrow sign = paragraph sign, U+00B6 ISOnum
		{
			"&middot;",
			"\u00b7"
		}, // middle dot = Georgian comma = Greek middle dot, U+00B7 ISOnum
		{
			"&cedil;",
			"\u00b8"
		}, // cedilla = spacing cedilla, U+00B8 ISOdia
		{
			"&sup1;",
			"\u00b9"
		}, // superscript one = superscript digit one, U+00B9 ISOnum
		{
			"&ordm;",
			"\u00ba"
		}, // masculine ordinal indicator, U+00BA ISOnum
		{
			"&raquo;",
			"\u00bb"
		}, // right-pointing float angle quotation mark = right pointing guillemet, U+00BB ISOnum
		{
			"&frac14;",
			"\u00bc"
		}, // vulgar fraction one quarter = fraction one quarter, U+00BC ISOnum
		{
			"&frac12;",
			"\u00bd"
		}, // vulgar fraction one half = fraction one half, U+00BD ISOnum
		{
			"&frac34;",
			"\u00be"
		}, // vulgar fraction three quarters = fraction three quarters, U+00BE ISOnum
		{
			"&iquest;",
			"\u00bf"
		}, // inverted question mark = turned question mark, U+00BF ISOnum
		{
			"&Agrave;",
			"\u00c0"
		}, // latin capital letter A with grave = latin capital letter A grave, U+00C0 ISOlat1
		{
			"&Aacute;",
			"\u00c1"
		}, // latin capital letter A with acute, U+00C1 ISOlat1
		{
			"&Acirc;",
			"\u00c2"
		}, // latin capital letter A with circumflex, U+00C2 ISOlat1
		{
			"&Atilde;",
			"\u00c3"
		}, // latin capital letter A with tilde, U+00C3 ISOlat1
		{
			"&Auml;",
			"\u00c4"
		}, // latin capital letter A with diaeresis, U+00C4 ISOlat1
		{
			"&Aring;",
			"\u00c5"
		}, // latin capital letter A with ring above = latin capital letter A ring, U+00C5 ISOlat1
		{
			"&AElig;",
			"\u00c6"
		}, // latin capital letter AE = latin capital ligature AE, U+00C6 ISOlat1
		{
			"&Ccedil;",
			"\u00c7"
		}, // latin capital letter C with cedilla, U+00C7 ISOlat1
		{
			"&Egrave;",
			"\u00c8"
		}, // latin capital letter E with grave, U+00C8 ISOlat1
		{
			"&Eacute;",
			"\u00c9"
		}, // latin capital letter E with acute, U+00C9 ISOlat1
		{
			"&Ecirc;",
			"\u00ca"
		}, // latin capital letter E with circumflex, U+00CA ISOlat1
		{
			"&Euml;",
			"\u00cb"
		}, // latin capital letter E with diaeresis, U+00CB ISOlat1
		{
			"&Igrave;",
			"\u00cc"
		}, // latin capital letter I with grave, U+00CC ISOlat1
		{
			"&Iacute;",
			"\u00cd"
		}, // latin capital letter I with acute, U+00CD ISOlat1
		{
			"&Icirc;",
			"\u00ce"
		}, // latin capital letter I with circumflex, U+00CE ISOlat1
		{
			"&Iuml;",
			"\u00cf"
		}, // latin capital letter I with diaeresis, U+00CF ISOlat1
		{
			"&ETH;",
			"\u00d0"
		}, // latin capital letter ETH, U+00D0 ISOlat1
		{
			"&Ntilde;",
			"\u00d1"
		}, // latin capital letter N with tilde, U+00D1 ISOlat1
		{
			"&Ograve;",
			"\u00d2"
		}, // latin capital letter O with grave, U+00D2 ISOlat1
		{
			"&Oacute;",
			"\u00d3"
		}, // latin capital letter O with acute, U+00D3 ISOlat1
		{
			"&Ocirc;",
			"\u00d4"
		}, // latin capital letter O with circumflex, U+00D4 ISOlat1
		{
			"&Otilde;",
			"\u00d5"
		}, // latin capital letter O with tilde, U+00D5 ISOlat1
		{
			"&Ouml;",
			"\u00d6"
		}, // latin capital letter O with diaeresis, U+00D6 ISOlat1
		{
			"&times;",
			"\u00d7"
		}, // multiplication sign, U+00D7 ISOnum
		{
			"&Oslash;",
			"\u00d8"
		}, // latin capital letter O with stroke = latin capital letter O slash, U+00D8 ISOlat1
		{
			"&Ugrave;",
			"\u00d9"
		}, // latin capital letter U with grave, U+00D9 ISOlat1
		{
			"&Uacute;",
			"\u00da"
		}, // latin capital letter U with acute, U+00DA ISOlat1
		{
			"&Ucirc;",
			"\u00db"
		}, // latin capital letter U with circumflex, U+00DB ISOlat1
		{
			"&Uuml;",
			"\u00dc"
		}, // latin capital letter U with diaeresis, U+00DC ISOlat1
		{
			"&Yacute;",
			"\u00dd"
		}, // latin capital letter Y with acute, U+00DD ISOlat1
		{
			"&THORN;",
			"\u00de"
		}, // latin capital letter THORN, U+00DE ISOlat1
		{
			"&szlig;",
			"\u00df"
		}, // latin small letter sharp s = ess-zed, U+00DF ISOlat1
		{
			"&agrave;",
			"\u00e0"
		}, // latin small letter a with grave = latin small letter a grave, U+00E0 ISOlat1
		{
			"&aacute;",
			"\u00e1"
		}, // latin small letter a with acute, U+00E1 ISOlat1
		{
			"&acirc;",
			"\u00e2"
		}, // latin small letter a with circumflex, U+00E2 ISOlat1
		{
			"&atilde;",
			"\u00e3"
		}, // latin small letter a with tilde, U+00E3 ISOlat1
		{
			"&auml;",
			"\u00e4"
		}, // latin small letter a with diaeresis, U+00E4 ISOlat1
		{
			"&aring;",
			"\u00e5"
		}, // latin small letter a with ring above = latin small letter a ring, U+00E5 ISOlat1
		{
			"&aelig;",
			"\u00e6"
		}, // latin small letter ae = latin small ligature ae, U+00E6 ISOlat1
		{
			"&ccedil;",
			"\u00e7"
		}, // latin small letter c with cedilla, U+00E7 ISOlat1
		{
			"&egrave;",
			"\u00e8"
		}, // latin small letter e with grave, U+00E8 ISOlat1
		{
			"&eacute;",
			"\u00e9"
		}, // latin small letter e with acute, U+00E9 ISOlat1
		{
			"&ecirc;",
			"\u00ea"
		}, // latin small letter e with circumflex, U+00EA ISOlat1
		{
			"&euml;",
			"\u00eb"
		}, // latin small letter e with diaeresis, U+00EB ISOlat1
		{
			"&igrave;",
			"\u00ec"
		}, // latin small letter i with grave, U+00EC ISOlat1
		{
			"&iacute;",
			"\u00ed"
		}, // latin small letter i with acute, U+00ED ISOlat1
		{
			"&icirc;",
			"\u00ee"
		}, // latin small letter i with circumflex, U+00EE ISOlat1
		{
			"&iuml;",
			"\u00ef"
		}, // latin small letter i with diaeresis, U+00EF ISOlat1
		{
			"&eth;",
			"\u00f0"
		}, // latin small letter eth, U+00F0 ISOlat1
		{
			"&ntilde;",
			"\u00f1"
		}, // latin small letter n with tilde, U+00F1 ISOlat1
		{
			"&ograve;",
			"\u00f2"
		}, // latin small letter o with grave, U+00F2 ISOlat1
		{
			"&oacute;",
			"\u00f3"
		}, // latin small letter o with acute, U+00F3 ISOlat1
		{
			"&ocirc;",
			"\u00f4"
		}, // latin small letter o with circumflex, U+00F4 ISOlat1
		{
			"&otilde;",
			"\u00f5"
		}, // latin small letter o with tilde, U+00F5 ISOlat1
		{
			"&ouml;",
			"\u00f6"
		}, // latin small letter o with diaeresis, U+00F6 ISOlat1
		{
			"&divide;",
			"\u00f7"
		}, // division sign, U+00F7 ISOnum
		{
			"&oslash;",
			"\u00f8"
		}, // latin small letter o with stroke, = latin small letter o slash, U+00F8 ISOlat1
		{
			"&ugrave;",
			"\u00f9"
		}, // latin small letter u with grave, U+00F9 ISOlat1
		{
			"&uacute;",
			"\u00fa"
		}, // latin small letter u with acute, U+00FA ISOlat1
		{
			"&ucirc;",
			"\u00fb"
		}, // latin small letter u with circumflex, U+00FB ISOlat1
		{
			"&uuml;",
			"\u00fc"
		}, // latin small letter u with diaeresis, U+00FC ISOlat1
		{
			"&yacute;",
			"\u00fd"
		}, // latin small letter y with acute, U+00FD ISOlat1
		{
			"&thorn;",
			"\u00fe"
		}, // latin small letter thorn, U+00FE ISOlat1
		{
			"&yuml;",
			"\u00ff"
		}, // latin small letter y with diaeresis, U+00FF ISOlat1

		// Mathematical, Greek and Symbolic characters for HTML
		// Character entity set. Typical invocation:
		// <!ENTITY % HTMLsymbol PUBLIC
		// "-//W3C//ENTITIES Symbols//EN//HTML">
		// %HTMLsymbol;

		// Portions (c) International Organization for Standardization 1986
		// Permission to copy in any form is granted for use with
		// conforming SGML systems and applications as defined in
		// ISO 8879, provided this notice is included in all copies.
		// Relevant ISO entity set is given unless names are newly introduced.
		// New names (i.e., not in ISO 8879 list) do not clash with any
		// existing ISO 8879 entity names. ISO 10646 character numbers
		// are given for each character, in hex. CDATA values are decimal
		// conversions of the ISO 10646 values and refer to the document
		// character set. Names are ISO 10646 names.

		// Latin Extended-B

		{
			"&fnof;",
			"\u0192"
		}, // latin small f with hook = function = florin, U+0192 ISOtech

		// Greek

		{
			"&Alpha;",
			"\u0391"
		}, // greek capital letter alpha, U+0391
		{
			"&Beta;",
			"\u0392"
		}, // greek capital letter beta, U+0392
		{
			"&Gamma;",
			"\u0393"
		}, // greek capital letter gamma, U+0393 ISOgrk3
		{
			"&Delta;",
			"\u0394"
		}, // greek capital letter delta, U+0394 ISOgrk3
		{
			"&Epsilon;",
			"\u0395"
		}, // greek capital letter epsilon, U+0395
		{
			"&Zeta;",
			"\u0396"
		}, // greek capital letter zeta, U+0396
		{
			"&Eta;",
			"\u0397"
		}, // greek capital letter eta, U+0397
		{
			"&Theta;",
			"\u0398"
		}, // greek capital letter theta, U+0398 ISOgrk3
		{
			"&Iota;",
			"\u0399"
		}, // greek capital letter iota, U+0399
		{
			"&Kappa;",
			"\u039a"
		}, // greek capital letter kappa, U+039A
		{
			"&Lambda;",
			"\u039b"
		}, // greek capital letter lambda, U+039B ISOgrk3
		{
			"&Mu;",
			"\u039c"
		}, // greek capital letter mu, U+039C
		{
			"&Nu;",
			"\u039d"
		}, // greek capital letter nu, U+039D
		{
			"&Xi;",
			"\u039e"
		}, // greek capital letter xi, U+039E ISOgrk3
		{
			"&Omicron;",
			"\u039f"
		}, // greek capital letter omicron, U+039F
		{
			"&Pi;",
			"\u03a0"
		}, // greek capital letter pi, U+03A0 ISOgrk3
		{
			"&Rho;",
			"\u03a1"
		}, // greek capital letter rho, U+03A1

		// there is no Sigmaf, and no U+03A2 character either

		{
			"&Sigma;",
			"\u03a3"
		}, // greek capital letter sigma, U+03A3 ISOgrk3
		{
			"&Tau;",
			"\u03a4"
		}, // greek capital letter tau, U+03A4
		{
			"&Upsilon;",
			"\u03a5"
		}, // greek capital letter upsilon, U+03A5 ISOgrk3
		{
			"&Phi;",
			"\u03a6"
		}, // greek capital letter phi, U+03A6 ISOgrk3
		{
			"&Chi;",
			"\u03a7"
		}, // greek capital letter chi, U+03A7
		{
			"&Psi;",
			"\u03a8"
		}, // greek capital letter psi, U+03A8 ISOgrk3
		{
			"&Omega;",
			"\u03a9"
		}, // greek capital letter omega, U+03A9 ISOgrk3
		{
			"&alpha;",
			"\u03b1"
		}, // greek small letter alpha, U+03B1 ISOgrk3
		{
			"&beta;",
			"\u03b2"
		}, // greek small letter beta, U+03B2 ISOgrk3
		{
			"&gamma;",
			"\u03b3"
		}, // greek small letter gamma, U+03B3 ISOgrk3
		{
			"&delta;",
			"\u03b4"
		}, // greek small letter delta, U+03B4 ISOgrk3
		{
			"&epsilon;",
			"\u03b5"
		}, // greek small letter epsilon, U+03B5 ISOgrk3
		{
			"&zeta;",
			"\u03b6"
		}, // greek small letter zeta, U+03B6 ISOgrk3
		{
			"&eta;",
			"\u03b7"
		}, // greek small letter eta, U+03B7 ISOgrk3
		{
			"&theta;",
			"\u03b8"
		}, // greek small letter theta, U+03B8 ISOgrk3
		{
			"&iota;",
			"\u03b9"
		}, // greek small letter iota, U+03B9 ISOgrk3
		{
			"&kappa;",
			"\u03ba"
		}, // greek small letter kappa, U+03BA ISOgrk3
		{
			"&lambda;",
			"\u03bb"
		}, // greek small letter lambda, U+03BB ISOgrk3
		{
			"&mu;",
			"\u03bc"
		}, // greek small letter mu, U+03BC ISOgrk3
		{
			"&nu;",
			"\u03bd"
		}, // greek small letter nu, U+03BD ISOgrk3
		{
			"&xi;",
			"\u03be"
		}, // greek small letter xi, U+03BE ISOgrk3
		{
			"&omicron;",
			"\u03bf"
		}, // greek small letter omicron, U+03BF NEW
		{
			"&pi;",
			"\u03c0"
		}, // greek small letter pi, U+03C0 ISOgrk3
		{
			"&rho;",
			"\u03c1"
		}, // greek small letter rho, U+03C1 ISOgrk3
		{
			"&sigmaf;",
			"\u03c2"
		}, // greek small letter final sigma, U+03C2 ISOgrk3
		{
			"&sigma;",
			"\u03c3"
		}, // greek small letter sigma, U+03C3 ISOgrk3
		{
			"&tau;",
			"\u03c4"
		}, // greek small letter tau, U+03C4 ISOgrk3
		{
			"&upsilon;",
			"\u03c5"
		}, // greek small letter upsilon, U+03C5 ISOgrk3
		{
			"&phi;",
			"\u03c6"
		}, // greek small letter phi, U+03C6 ISOgrk3
		{
			"&chi;",
			"\u03c7"
		}, // greek small letter chi, U+03C7 ISOgrk3
		{
			"&psi;",
			"\u03c8"
		}, // greek small letter psi, U+03C8 ISOgrk3
		{
			"&omega;",
			"\u03c9"
		}, // greek small letter omega, U+03C9 ISOgrk3
		{
			"&thetasym;",
			"\u03d1"
		}, // greek small letter theta symbol, U+03D1 NEW
		{
			"&upsih;",
			"\u03d2"
		}, // greek upsilon with hook symbol, U+03D2 NEW
		{
			"&piv;",
			"\u03d6"
		}, // greek pi symbol, U+03D6 ISOgrk3

		// General Punctuation

		{
			"&bull;",
			"\u2022"
		}, // bullet = black small circle, U+2022 ISOpub
		{
			"&hellip;",
			"\u2026"
		}, // horizontal ellipsis = three dot leader, U+2026 ISOpub
		{
			"&prime;",
			"\u2032"
		}, // prime = minutes = feet, U+2032 ISOtech
		{
			"&Prime;",
			"\u2033"
		}, // float prime = seconds = inches, U+2033 ISOtech
		{
			"&oline;",
			"\u203e"
		}, // overline = spacing overscore, U+203E NEW
		{
			"&frasl;",
			"\u2044"
		}, // fraction slash, U+2044 NEW

		// Letterlike Symbols

		{
			"&weierp;",
			"\u2118"
		}, // script capital P = power set = Weierstrass p, U+2118 ISOamso
		{
			"&image;",
			"\u2111"
		}, // blackletter capital I = imaginary part, U+2111 ISOamso
		{
			"&real;",
			"\u211c"
		}, // blackletter capital R = real part symbol, U+211C ISOamso
		{
			"&trade;",
			"\u2122"
		}, // trade mark sign, U+2122 ISOnum
		{
			"&alefsym;",
			"\u2135"
		}, // alef symbol = first transfinite cardinal, U+2135 NEW

		// Arrows

		{
			"&larr;",
			"\u2190"
		}, // leftwards arrow, U+2190 ISOnum
		{
			"&uarr;",
			"\u2191"
		}, // upwards arrow, U+2191 ISOnum
		{
			"&rarr;",
			"\u2192"
		}, // rightwards arrow, U+2192 ISOnum
		{
			"&darr;",
			"\u2193"
		}, // downwards arrow, U+2193 ISOnum
		{
			"&harr;",
			"\u2194"
		}, // left right arrow, U+2194 ISOamsa
		{
			"&crarr;",
			"\u21b5"
		}, // downwards arrow with corner leftwards = carriage return, U+21B5 NEW
		{
			"&lArr;",
			"\u21d0"
		}, // leftwards float arrow, U+21D0 ISOtech
		{
			"&uArr;",
			"\u21d1"
		}, // upwards float arrow, U+21D1 ISOamsa
		{
			"&rArr;",
			"\u21d2"
		}, // rightwards float arrow, U+21D2 ISOtech

		{
			"&dArr;",
			"\u21d3"
		}, // downwards float arrow, U+21D3 ISOamsa
		{
			"&hArr;",
			"\u21d4"
		}, // left right float arrow, U+21D4 ISOamsa

		// Mathematical Operators

		{
			"&forall;",
			"\u2200"
		}, // for all, U+2200 ISOtech
		{
			"&part;",
			"\u2202"
		}, // partial differential, U+2202 ISOtech
		{
			"&exist;",
			"\u2203"
		}, // there exists, U+2203 ISOtech
		{
			"&empty;",
			"\u2205"
		}, // empty set = null set = diameter, U+2205 ISOamso
		{
			"&nabla;",
			"\u2207"
		}, // nabla = backward difference, U+2207 ISOtech
		{
			"&isin;",
			"\u2208"
		}, // element of, U+2208 ISOtech
		{
			"&notin;",
			"\u2209"
		}, // not an element of, U+2209 ISOtech
		{
			"&ni;",
			"\u220b"
		}, // contains as member, U+220B ISOtech
		{
			"&prod;",
			"\u220f"
		}, // n-ary product = product sign, U+220F ISOamsb
		{
			"&sum;",
			"\u2211"
		}, // n-ary sumation, U+2211 ISOamsb
		{
			"&minus;",
			"\u2212"
		}, // minus sign, U+2212 ISOtech
		{
			"&lowast;",
			"\u2217"
		}, // asterisk operator, U+2217 ISOtech
		{
			"&radic;",
			"\u221a"
		}, // square root = radical sign, U+221A ISOtech
		{
			"&prop;",
			"\u221d"
		}, // proportional to, U+221D ISOtech
		{
			"&infin;",
			"\u221e"
		}, // infinity, U+221E ISOtech
		{
			"&ang;",
			"\u2220"
		}, // angle, U+2220 ISOamso
		{
			"&and;",
			"\u2227"
		}, // logical and = wedge, U+2227 ISOtech
		{
			"&or;",
			"\u2228"
		}, // logical or = vee, U+2228 ISOtech
		{
			"&cap;",
			"\u2229"
		}, // intersection = cap, U+2229 ISOtech
		{
			"&cup;",
			"\u222a"
		}, // union = cup, U+222A ISOtech
		{
			"&int;",
			"\u222b"
		}, // integral, U+222B ISOtech
		{
			"&there4;",
			"\u2234"
		}, // therefore, U+2234 ISOtech
		{
			"&sim;",
			"\u223c"
		}, // tilde operator = varies with = similar to, U+223C ISOtech
		{
			"&cong;",
			"\u2245"
		}, // approximately equal to, U+2245 ISOtech
		{
			"&asymp;",
			"\u2248"
		}, // almost equal to = asymptotic to, U+2248 ISOamsr
		{
			"&ne;",
			"\u2260"
		}, // not equal to, U+2260 ISOtech
		{
			"&equiv;",
			"\u2261"
		}, // identical to, U+2261 ISOtech
		{
			"&le;",
			"\u2264"
		}, // less-than or equal to, U+2264 ISOtech
		{
			"&ge;",
			"\u2265"
		}, // greater-than or equal to, U+2265 ISOtech
		{
			"&sub;",
			"\u2282"
		}, // subset of, U+2282 ISOtech
		{
			"&sup;",
			"\u2283"
		}, // superset of, U+2283 ISOtech
		{
			"&nsub;",
			"\u2284"
		}, // not a subset of, U+2284 ISOamsn
		{
			"&sube;",
			"\u2286"
		}, // subset of or equal to, U+2286 ISOtech
		{
			"&supe;",
			"\u2287"
		}, // superset of or equal to, U+2287 ISOtech
		{
			"&oplus;",
			"\u2295"
		}, // circled plus = direct sum, U+2295 ISOamsb
		{
			"&otimes;",
			"\u2297"
		}, // circled times = vector product, U+2297 ISOamsb
		{
			"&perp;",
			"\u22a5"
		}, // up tack = orthogonal to = perpendicular, U+22A5 ISOtech
		{
			"&sdot;",
			"\u22c5"
		}, // dot operator, U+22C5 ISOamsb

		// Miscellaneous Technical

		{
			"&lceil;",
			"\u2308"
		}, // left ceiling = apl upstile, U+2308 ISOamsc
		{
			"&rceil;",
			"\u2309"
		}, // right ceiling, U+2309 ISOamsc
		{
			"&lfloor;",
			"\u230a"
		}, // left floor = apl downstile, U+230A ISOamsc
		{
			"&rfloor;",
			"\u230b"
		}, // right floor, U+230B ISOamsc
		{
			"&lang;",
			"\u2329"
		}, // left-pointing angle bracket = bra, U+2329 ISOtech
		{
			"&rang;",
			"\u232a"
		}, // right-pointing angle bracket = ket, U+232A ISOtech
		{
			"&loz;",
			"\u25ca"
		}, // lozenge, U+25CA ISOpub

		// Miscellaneous Symbols

		{
			"&spades;",
			"\u2660"
		}, // black spade suit, U+2660 ISOpub
		{
			"&clubs;",
			"\u2663"
		}, // black club suit = shamrock, U+2663 ISOpub
		{
			"&hearts;",
			"\u2665"
		}, // black heart suit = valentine, U+2665 ISOpub
		{
			"&diams;",
			"\u2666"
		}, // black diamond suit, U+2666 ISOpub

		// Special characters for HTML
		// Character entity set. Typical invocation:
		// <!ENTITY % HTMLspecial PUBLIC
		// "-//W3C//ENTITIES Special//EN//HTML">
		// %HTMLspecial;

		// Portions (c) International Organization for Standardization 1986
		// Permission to copy in any form is granted for use with
		// conforming SGML systems and applications as defined in
		// ISO 8879, provided this notice is included in all copies.
		// Relevant ISO entity set is given unless names are newly introduced.
		// New names (i.e., not in ISO 8879 list) do not clash with any
		// existing ISO 8879 entity names. ISO 10646 character numbers
		// are given for each character, in hex. CDATA values are decimal
		// conversions of the ISO 10646 values and refer to the document
		// character set. Names are ISO 10646 names.
		// C0 Controls and Basic Latin

		{
			"&quot;",
			"\""
		}, // quotation mark = APL quote, U+0022 ISOnum
		{
			"&amp;",
			"\u0026"
		}, // ampersand, U+0026 ISOnum
		{
			"&lt;",
			"\u003c"
		}, // less-than sign, U+003C ISOnum
		{
			"&gt;",
			"\u003e"
		}, // greater-than sign, U+003E ISOnum

		// Latin Extended-A

		{
			"&OElig;",
			"\u0152"
		}, // latin capital ligature OE, U+0152 ISOlat2
		{
			"&oelig;",
			"\u0153"
		}, // latin small ligature oe, U+0153 ISOlat2
		{
			"&Scaron;",
			"\u0160"
		}, // latin capital letter S with caron, U+0160 ISOlat2
		{
			"&scaron;",
			"\u0161"
		}, // latin small letter s with caron, U+0161 ISOlat2
		{
			"&Yuml;",
			"\u0178"
		}, // latin capital letter Y with diaeresis, U+0178 ISOlat2

		// Spacing Modifier Letters

		{
			"&circ;",
			"\u02c6"
		}, // modifier letter circumflex accent, U+02C6 ISOpub
		{
			"&tilde;",
			"\u02dc"
		}, // small tilde, U+02DC ISOdia

		// General Punctuation

		{
			"&ensp;",
			"\u2002"
		}, // en space, U+2002 ISOpub
		{
			"&emsp;",
			"\u2003"
		}, // em space, U+2003 ISOpub
		{
			"&thinsp;",
			"\u2009"
		}, // thin space, U+2009 ISOpub
		{
			"&zwnj;",
			"\u200c"
		}, // zero width non-joiner, U+200C NEW RFC 2070
		{
			"&zwj;",
			"\u200d"
		}, // zero width joiner, U+200D NEW RFC 2070
		{
			"&lrm;",
			"\u200e"
		}, // left-to-right mark, U+200E NEW RFC 2070
		{
			"&rlm;",
			"\u200f"
		}, // right-to-left mark, U+200F NEW RFC 2070
		{
			"&ndash;",
			"\u2013"
		}, // en dash, U+2013 ISOpub
		{
			"&mdash;",
			"\u2014"
		}, // em dash, U+2014 ISOpub
		{
			"&lsquo;",
			"\u2018"
		}, // left single quotation mark, U+2018 ISOnum
		{
			"&rsquo;",
			"\u2019"
		}, // right single quotation mark, U+2019 ISOnum
		{
			"&sbquo;",
			"\u201a"
		}, // single low-9 quotation mark, U+201A NEW
		{
			"&ldquo;",
			"\u201c"
		}, // left float quotation mark, U+201C ISOnum
		{
			"&rdquo;",
			"\u201d"
		}, // right float quotation mark, U+201D ISOnum
		{
			"&bdquo;",
			"\u201e"
		}, // float low-9 quotation mark, U+201E NEW
		{
			"&dagger;",
			"\u2020"
		}, // dagger, U+2020 ISOpub
		{
			"&Dagger;",
			"\u2021"
		}, // float dagger, U+2021 ISOpub
		{
			"&permil;",
			"\u2030"
		}, // per mille sign, U+2030 ISOtech
		{
			"&lsaquo;",
			"\u2039"
		}, // single left-pointing angle quotation mark, U+2039 ISO proposed
		{
			"&rsaquo;",
			"\u203a"
		}, // single right-pointing angle quotation mark, U+203A ISO proposed
		{
			"&euro;",
			"\u20ac"
		}
	// euro sign, U+20AC NEW
		};

	private static final Map<Character, String> entities = new HashMap<Character, String>();
	private static final Map<String, Character> unicodes = new HashMap<String, Character>();

	static
	{
		for ( int i = 0; i < CharacterEntities.UNICODE_TABLE.length; ++i )
		{
			String entity = CharacterEntities.UNICODE_TABLE[ i ][ 0 ];
			Character unicode = new Character( CharacterEntities.UNICODE_TABLE[ i ][ 1 ].charAt( 0 ) );

			CharacterEntities.entities.put( unicode, entity );
			CharacterEntities.unicodes.put( entity, unicode );
		}
	}

	public static final String escape( final String unicodeVersion )
	{
		// Iterate over all the characters in the string looking for unicode

		StringBuffer entityVersion = null;

		char ch;
		int start = 0;
		int length = unicodeVersion.length();

		for ( int i = 0; i < length; ++i )
		{
			ch = unicodeVersion.charAt( i );

			if ( ( ch >= 'A' && ch <= 'Z' ) || ( ch >= 'a' && ch <= 'z' ) || Character.isWhitespace( ch ) || Character.isDigit( ch ) )
			{
				continue;
			}

			String entity = (String) CharacterEntities.entities.get( new Character( ch ) );

			// If we don't have a translation, use Unicode escape
			if ( entity == null )
			{
				if ( ch < 0x80 )
				{
					continue;
				}
				entity = "&#" + (int) ch + ";";
			}

			// If we don't have a string buffer, make one
			if ( entityVersion == null )
			{
				entityVersion = new StringBuffer();
			}

			// Append prefix
			if ( i > start )
			{
				entityVersion.append( unicodeVersion.substring( start, i ) );
			}

			// Insert entity
			entityVersion.append( entity );

			// Start new prefix
			start = i + 1;
		}

		// If we didn't find anything, return original string
		if ( start == 0 )
		{
			return unicodeVersion;
		}

		// Append suffix
		if ( start < length )
		{
			entityVersion.append( unicodeVersion.substring( start ) );
		}

		return entityVersion.toString();
	}

	public static final String unescape( final String entityVersion )
	{
		int index = entityVersion.indexOf( "&" );

		// If there are no character entities, return original string
		if ( index < 0 )
		{
			return entityVersion;
		}

		// Otherwise, make a StringBuffer to create unicode version of input
		StringBuffer unicodeVersion = null;
		int start = 0;

		// Replace all entities
		while ( index >= 0 )
		{
			// Find the end of the character entity
			int semi = entityVersion.indexOf( ";", index + 1 );

			// If no semicolon, bogus, but quit now
			if ( semi < 0 )
			{
				index = entityVersion.indexOf( " ", index + 1 );
				continue;
			}

			// Replace entity with unicode
			String entity = entityVersion.substring( index, semi + 1 );
			Character unicode;
			if ( entity.charAt( 1 ) == '#' )
			{
				unicode = new Character( (char) StringUtilities.parseInt( entity.substring( 2, entity.length() - 1 ) ) );
			}
			else
			{
				unicode = (Character) CharacterEntities.unicodes.get( entity );
			}

			// If we don't have a translation, skip past entity
			if ( unicode == null )
			{
				index = entityVersion.indexOf( "&", index + 1 );
				continue;
			}

			// If we don't have a string buffer, make one
			if ( unicodeVersion == null )
			{
				unicodeVersion = new StringBuffer();
			}

			// Copy in prefix
			if ( index > start )
			{
				unicodeVersion.append( entityVersion.substring( start, index ) );
			}

			// Insert unicode
			unicodeVersion.append( unicode.charValue() );

			// Skip past entity
			start = semi + 1;
			index = entityVersion.indexOf( "&", start );
		}

		// If we never translated an entity, return the original string
		if ( start == 0 )
		{
			return entityVersion;
		}

		// Append suffix
		if ( start < entityVersion.length() )
		{
			unicodeVersion.append( entityVersion.substring( start ) );
		}

		// Return the new string
		return unicodeVersion.toString();
	}
}
