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

import java.awt.Color;
import java.awt.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import javax.swing.text.html.FormView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

import com.velocityreviews.forums.HttpTimeoutHandler;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

public class RequestEditorKit extends HTMLEditorKit implements KoLConstants
{
	// Overkill Unicode table borrowed from HTMLParser
	// http://htmlparser.sourceforge.net/

	private static final String [][] UNICODE_TABLE =
	{
		// Portions (c) International Organization for Standardization 1986
		// Permission to copy in any form is granted for use with
		// conforming SGML systems and applications as defined in
		// ISO 8879, provided this notice is included in all copies.
		// Character entity set. Typical invocation:

		// <!ENTITY % HTMLlat1 PUBLIC
		// "-//W3C//ENTITIES Latin 1//EN//HTML">
		// %HTMLlat1;

		{ "&nbsp;",	"\u00a0" }, // no-break space = non-breaking space, U+00A0 ISOnum
		{ "&iexcl;",	"\u00a1" }, // inverted exclamation mark, U+00A1 ISOnum
		{ "&cent;",	"\u00a2" }, // cent sign, U+00A2 ISOnum
		{ "&pound;",	"\u00a3" }, // pound sign, U+00A3 ISOnum
		{ "&curren;",	"\u00a4" }, // currency sign, U+00A4 ISOnum
		{ "&yen;",	"\u00a5" }, // yen sign = yuan sign, U+00A5 ISOnum
		{ "&brvbar;",	"\u00a6" }, // broken bar = broken vertical bar, U+00A6 ISOnum
		{ "&sect;",	"\u00a7" }, // section sign, U+00A7 ISOnum
		{ "&uml;",	"\u00a8" }, // diaeresis = spacing diaeresis, U+00A8 ISOdia
		{ "&copy;",	"\u00a9" }, // copyright sign, U+00A9 ISOnum
		{ "&ordf;",	"\u00aa" }, // feminine ordinal indicator, U+00AA ISOnum
		{ "&laquo;",	"\u00ab" }, // left-pointing float angle quotation mark = left pointing guillemet, U+00AB ISOnum
		{ "&not;",	"\u00ac" }, // not sign, U+00AC ISOnum
		{ "&shy;",	"\u00ad" }, // soft hyphen = discretionary hyphen, U+00AD ISOnum
		{ "&reg;",	"\u00ae" }, // registered sign = registered trade mark sign, U+00AE ISOnum
		{ "&macr;",	"\u00af" }, // macron = spacing macron = overline = APL overbar, U+00AF ISOdia
		{ "&deg;",	"\u00b0" }, // degree sign, U+00B0 ISOnum
		{ "&plusmn;",	"\u00b1" }, // plus-minus sign = plus-or-minus sign, U+00B1 ISOnum
		{ "&sup2;",	"\u00b2" }, // superscript two = superscript digit two = squared, U+00B2 ISOnum
		{ "&sup3;",	"\u00b3" }, // superscript three = superscript digit three = cubed, U+00B3 ISOnum
		{ "&acute;",	"\u00b4" }, // acute accent = spacing acute, U+00B4 ISOdia
		{ "&micro;",	"\u00b5" }, // micro sign, U+00B5 ISOnum
		{ "&para;",	"\u00b6" }, // pilcrow sign = paragraph sign, U+00B6 ISOnum
		{ "&middot;",	"\u00b7" }, // middle dot = Georgian comma = Greek middle dot, U+00B7 ISOnum
		{ "&cedil;",	"\u00b8" }, // cedilla = spacing cedilla, U+00B8 ISOdia
		{ "&sup1;",	"\u00b9" }, // superscript one = superscript digit one, U+00B9 ISOnum
		{ "&ordm;",	"\u00ba" }, // masculine ordinal indicator, U+00BA ISOnum
		{ "&raquo;",	"\u00bb" }, // right-pointing float angle quotation mark = right pointing guillemet, U+00BB ISOnum
		{ "&frac14;",	"\u00bc" }, // vulgar fraction one quarter = fraction one quarter, U+00BC ISOnum
		{ "&frac12;",	"\u00bd" }, // vulgar fraction one half = fraction one half, U+00BD ISOnum
		{ "&frac34;",	"\u00be" }, // vulgar fraction three quarters = fraction three quarters, U+00BE ISOnum
		{ "&iquest;",	"\u00bf" }, // inverted question mark = turned question mark, U+00BF ISOnum
		{ "&Agrave;",	"\u00c0" }, // latin capital letter A with grave = latin capital letter A grave, U+00C0 ISOlat1
		{ "&Aacute;",	"\u00c1" }, // latin capital letter A with acute, U+00C1 ISOlat1
		{ "&Acirc;",	"\u00c2" }, // latin capital letter A with circumflex, U+00C2 ISOlat1
		{ "&Atilde;",	"\u00c3" }, // latin capital letter A with tilde, U+00C3 ISOlat1
		{ "&Auml;",	"\u00c4" }, // latin capital letter A with diaeresis, U+00C4 ISOlat1
		{ "&Aring;",	"\u00c5" }, // latin capital letter A with ring above = latin capital letter A ring, U+00C5 ISOlat1
		{ "&AElig;",	"\u00c6" }, // latin capital letter AE = latin capital ligature AE, U+00C6 ISOlat1
		{ "&Ccedil;",	"\u00c7" }, // latin capital letter C with cedilla, U+00C7 ISOlat1
		{ "&Egrave;",	"\u00c8" }, // latin capital letter E with grave, U+00C8 ISOlat1
		{ "&Eacute;",	"\u00c9" }, // latin capital letter E with acute, U+00C9 ISOlat1
		{ "&Ecirc;",	"\u00ca" }, // latin capital letter E with circumflex, U+00CA ISOlat1
		{ "&Euml;",	"\u00cb" }, // latin capital letter E with diaeresis, U+00CB ISOlat1
		{ "&Igrave;",	"\u00cc" }, // latin capital letter I with grave, U+00CC ISOlat1
		{ "&Iacute;",	"\u00cd" }, // latin capital letter I with acute, U+00CD ISOlat1
		{ "&Icirc;",	"\u00ce" }, // latin capital letter I with circumflex, U+00CE ISOlat1
		{ "&Iuml;",	"\u00cf" }, // latin capital letter I with diaeresis, U+00CF ISOlat1
		{ "&ETH;",	"\u00d0" }, // latin capital letter ETH, U+00D0 ISOlat1
		{ "&Ntilde;",	"\u00d1" }, // latin capital letter N with tilde, U+00D1 ISOlat1
		{ "&Ograve;",	"\u00d2" }, // latin capital letter O with grave, U+00D2 ISOlat1
		{ "&Oacute;",	"\u00d3" }, // latin capital letter O with acute, U+00D3 ISOlat1
		{ "&Ocirc;",	"\u00d4" }, // latin capital letter O with circumflex, U+00D4 ISOlat1
		{ "&Otilde;",	"\u00d5" }, // latin capital letter O with tilde, U+00D5 ISOlat1
		{ "&Ouml;",	"\u00d6" }, // latin capital letter O with diaeresis, U+00D6 ISOlat1
		{ "&times;",	"\u00d7" }, // multiplication sign, U+00D7 ISOnum
		{ "&Oslash;",	"\u00d8" }, // latin capital letter O with stroke = latin capital letter O slash, U+00D8 ISOlat1
		{ "&Ugrave;",	"\u00d9" }, // latin capital letter U with grave, U+00D9 ISOlat1
		{ "&Uacute;",	"\u00da" }, // latin capital letter U with acute, U+00DA ISOlat1
		{ "&Ucirc;",	"\u00db" }, // latin capital letter U with circumflex, U+00DB ISOlat1
		{ "&Uuml;",	"\u00dc" }, // latin capital letter U with diaeresis, U+00DC ISOlat1
		{ "&Yacute;",	"\u00dd" }, // latin capital letter Y with acute, U+00DD ISOlat1
		{ "&THORN;",	"\u00de" }, // latin capital letter THORN, U+00DE ISOlat1
		{ "&szlig;",	"\u00df" }, // latin small letter sharp s = ess-zed, U+00DF ISOlat1
		{ "&agrave;",	"\u00e0" }, // latin small letter a with grave = latin small letter a grave, U+00E0 ISOlat1
		{ "&aacute;",	"\u00e1" }, // latin small letter a with acute, U+00E1 ISOlat1
		{ "&acirc;",	"\u00e2" }, // latin small letter a with circumflex, U+00E2 ISOlat1
		{ "&atilde;",	"\u00e3" }, // latin small letter a with tilde, U+00E3 ISOlat1
		{ "&auml;",	"\u00e4" }, // latin small letter a with diaeresis, U+00E4 ISOlat1
		{ "&aring;",	"\u00e5" }, // latin small letter a with ring above = latin small letter a ring, U+00E5 ISOlat1
		{ "&aelig;",	"\u00e6" }, // latin small letter ae = latin small ligature ae, U+00E6 ISOlat1
		{ "&ccedil;",	"\u00e7" }, // latin small letter c with cedilla, U+00E7 ISOlat1
		{ "&egrave;",	"\u00e8" }, // latin small letter e with grave, U+00E8 ISOlat1
		{ "&eacute;",	"\u00e9" }, // latin small letter e with acute, U+00E9 ISOlat1
		{ "&ecirc;",	"\u00ea" }, // latin small letter e with circumflex, U+00EA ISOlat1
		{ "&euml;",	"\u00eb" }, // latin small letter e with diaeresis, U+00EB ISOlat1
		{ "&igrave;",	"\u00ec" }, // latin small letter i with grave, U+00EC ISOlat1
		{ "&iacute;",	"\u00ed" }, // latin small letter i with acute, U+00ED ISOlat1
		{ "&icirc;",	"\u00ee" }, // latin small letter i with circumflex, U+00EE ISOlat1
		{ "&iuml;",	"\u00ef" }, // latin small letter i with diaeresis, U+00EF ISOlat1
		{ "&eth;",	"\u00f0" }, // latin small letter eth, U+00F0 ISOlat1
		{ "&ntilde;",	"\u00f1" }, // latin small letter n with tilde, U+00F1 ISOlat1
		{ "&ograve;",	"\u00f2" }, // latin small letter o with grave, U+00F2 ISOlat1
		{ "&oacute;",	"\u00f3" }, // latin small letter o with acute, U+00F3 ISOlat1
		{ "&ocirc;",	"\u00f4" }, // latin small letter o with circumflex, U+00F4 ISOlat1
		{ "&otilde;",	"\u00f5" }, // latin small letter o with tilde, U+00F5 ISOlat1
		{ "&ouml;",	"\u00f6" }, // latin small letter o with diaeresis, U+00F6 ISOlat1
		{ "&divide;",	"\u00f7" }, // division sign, U+00F7 ISOnum
		{ "&oslash;",	"\u00f8" }, // latin small letter o with stroke, = latin small letter o slash, U+00F8 ISOlat1
		{ "&ugrave;",	"\u00f9" }, // latin small letter u with grave, U+00F9 ISOlat1
		{ "&uacute;",	"\u00fa" }, // latin small letter u with acute, U+00FA ISOlat1
		{ "&ucirc;",	"\u00fb" }, // latin small letter u with circumflex, U+00FB ISOlat1
		{ "&uuml;",	"\u00fc" }, // latin small letter u with diaeresis, U+00FC ISOlat1
		{ "&yacute;",	"\u00fd" }, // latin small letter y with acute, U+00FD ISOlat1
		{ "&thorn;",	"\u00fe" }, // latin small letter thorn, U+00FE ISOlat1
		{ "&yuml;",	"\u00ff" }, // latin small letter y with diaeresis, U+00FF ISOlat1

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

		{ "&fnof;",	"\u0192" }, // latin small f with hook = function = florin, U+0192 ISOtech

		// Greek

		{ "&Alpha;",	"\u0391" }, // greek capital letter alpha, U+0391
		{ "&Beta;",	"\u0392" }, // greek capital letter beta, U+0392
		{ "&Gamma;",	"\u0393" }, // greek capital letter gamma, U+0393 ISOgrk3
		{ "&Delta;",	"\u0394" }, // greek capital letter delta, U+0394 ISOgrk3
		{ "&Epsilon;",	"\u0395" }, // greek capital letter epsilon, U+0395
		{ "&Zeta;",	"\u0396" }, // greek capital letter zeta, U+0396
		{ "&Eta;",	"\u0397" }, // greek capital letter eta, U+0397
		{ "&Theta;",	"\u0398" }, // greek capital letter theta, U+0398 ISOgrk3
		{ "&Iota;",	"\u0399" }, // greek capital letter iota, U+0399
		{ "&Kappa;",	"\u039a" }, // greek capital letter kappa, U+039A
		{ "&Lambda;",	"\u039b" }, // greek capital letter lambda, U+039B ISOgrk3
		{ "&Mu;",	"\u039c" }, // greek capital letter mu, U+039C
		{ "&Nu;",	"\u039d" }, // greek capital letter nu, U+039D
		{ "&Xi;",	"\u039e" }, // greek capital letter xi, U+039E ISOgrk3
		{ "&Omicron;",	"\u039f" }, // greek capital letter omicron, U+039F
		{ "&Pi;",	"\u03a0" }, // greek capital letter pi, U+03A0 ISOgrk3
		{ "&Rho;",	"\u03a1" }, // greek capital letter rho, U+03A1

		// there is no Sigmaf, and no U+03A2 character either

		{ "&Sigma;",	"\u03a3" }, // greek capital letter sigma, U+03A3 ISOgrk3
		{ "&Tau;",	"\u03a4" }, // greek capital letter tau, U+03A4
		{ "&Upsilon;",	"\u03a5" }, // greek capital letter upsilon, U+03A5 ISOgrk3
		{ "&Phi;",	"\u03a6" }, // greek capital letter phi, U+03A6 ISOgrk3
		{ "&Chi;",	"\u03a7" }, // greek capital letter chi, U+03A7
		{ "&Psi;",	"\u03a8" }, // greek capital letter psi, U+03A8 ISOgrk3
		{ "&Omega;",	"\u03a9" }, // greek capital letter omega, U+03A9 ISOgrk3
		{ "&alpha;",	"\u03b1" }, // greek small letter alpha, U+03B1 ISOgrk3
		{ "&beta;",	"\u03b2" }, // greek small letter beta, U+03B2 ISOgrk3
		{ "&gamma;",	"\u03b3" }, // greek small letter gamma, U+03B3 ISOgrk3
		{ "&delta;",	"\u03b4" }, // greek small letter delta, U+03B4 ISOgrk3
		{ "&epsilon;",	"\u03b5" }, // greek small letter epsilon, U+03B5 ISOgrk3
		{ "&zeta;",	"\u03b6" }, // greek small letter zeta, U+03B6 ISOgrk3
		{ "&eta;",	"\u03b7" }, // greek small letter eta, U+03B7 ISOgrk3
		{ "&theta;",	"\u03b8" }, // greek small letter theta, U+03B8 ISOgrk3
		{ "&iota;",	"\u03b9" }, // greek small letter iota, U+03B9 ISOgrk3
		{ "&kappa;",	"\u03ba" }, // greek small letter kappa, U+03BA ISOgrk3
		{ "&lambda;",	"\u03bb" }, // greek small letter lambda, U+03BB ISOgrk3
		{ "&mu;",	"\u03bc" }, // greek small letter mu, U+03BC ISOgrk3
		{ "&nu;",	"\u03bd" }, // greek small letter nu, U+03BD ISOgrk3
		{ "&xi;",	"\u03be" }, // greek small letter xi, U+03BE ISOgrk3
		{ "&omicron;",	"\u03bf" }, // greek small letter omicron, U+03BF NEW
		{ "&pi;",	"\u03c0" }, // greek small letter pi, U+03C0 ISOgrk3
		{ "&rho;",	"\u03c1" }, // greek small letter rho, U+03C1 ISOgrk3
		{ "&sigmaf;",	"\u03c2" }, // greek small letter final sigma, U+03C2 ISOgrk3
		{ "&sigma;",	"\u03c3" }, // greek small letter sigma, U+03C3 ISOgrk3
		{ "&tau;",	"\u03c4" }, // greek small letter tau, U+03C4 ISOgrk3
		{ "&upsilon;",	"\u03c5" }, // greek small letter upsilon, U+03C5 ISOgrk3
		{ "&phi;",	"\u03c6" }, // greek small letter phi, U+03C6 ISOgrk3
		{ "&chi;",	"\u03c7" }, // greek small letter chi, U+03C7 ISOgrk3
		{ "&psi;",	"\u03c8" }, // greek small letter psi, U+03C8 ISOgrk3
		{ "&omega;",	"\u03c9" }, // greek small letter omega, U+03C9 ISOgrk3
		{ "&thetasym;",	"\u03d1" }, // greek small letter theta symbol, U+03D1 NEW
		{ "&upsih;",	"\u03d2" }, // greek upsilon with hook symbol, U+03D2 NEW
		{ "&piv;",	"\u03d6" }, // greek pi symbol, U+03D6 ISOgrk3

		// General Punctuation

		{ "&bull;",	"\u2022" }, // bullet = black small circle, U+2022 ISOpub
		{ "&hellip;",	"\u2026" }, // horizontal ellipsis = three dot leader, U+2026 ISOpub
		{ "&prime;",	"\u2032" }, // prime = minutes = feet, U+2032 ISOtech
		{ "&Prime;",	"\u2033" }, // float prime = seconds = inches, U+2033 ISOtech
		{ "&oline;",	"\u203e" }, // overline = spacing overscore, U+203E NEW
		{ "&frasl;",	"\u2044" }, // fraction slash, U+2044 NEW

		// Letterlike Symbols

		{ "&weierp;",	"\u2118" }, // script capital P = power set = Weierstrass p, U+2118 ISOamso
		{ "&image;",	"\u2111" }, // blackletter capital I = imaginary part, U+2111 ISOamso
		{ "&real;",	"\u211c" }, // blackletter capital R = real part symbol, U+211C ISOamso
		{ "&trade;",	"\u2122" }, // trade mark sign, U+2122 ISOnum
		{ "&alefsym;",	"\u2135" }, // alef symbol = first transfinite cardinal, U+2135 NEW

		// Arrows

		{ "&larr;",	"\u2190" }, // leftwards arrow, U+2190 ISOnum
		{ "&uarr;",	"\u2191" }, // upwards arrow, U+2191 ISOnum
		{ "&rarr;",	"\u2192" }, // rightwards arrow, U+2192 ISOnum
		{ "&darr;",	"\u2193" }, // downwards arrow, U+2193 ISOnum
		{ "&harr;",	"\u2194" }, // left right arrow, U+2194 ISOamsa
		{ "&crarr;",	"\u21b5" }, // downwards arrow with corner leftwards = carriage return, U+21B5 NEW
		{ "&lArr;",	"\u21d0" }, // leftwards float arrow, U+21D0 ISOtech
		{ "&uArr;",	"\u21d1" }, // upwards float arrow, U+21D1 ISOamsa
		{ "&rArr;",	"\u21d2" }, // rightwards float arrow, U+21D2 ISOtech

		{ "&dArr;",	"\u21d3" }, // downwards float arrow, U+21D3 ISOamsa
		{ "&hArr;",	"\u21d4" }, // left right float arrow, U+21D4 ISOamsa

		// Mathematical Operators

		{ "&forall;",	"\u2200" }, // for all, U+2200 ISOtech
		{ "&part;",	"\u2202" }, // partial differential, U+2202 ISOtech
		{ "&exist;",	"\u2203" }, // there exists, U+2203 ISOtech
		{ "&empty;",	"\u2205" }, // empty set = null set = diameter, U+2205 ISOamso
		{ "&nabla;",	"\u2207" }, // nabla = backward difference, U+2207 ISOtech
		{ "&isin;",	"\u2208" }, // element of, U+2208 ISOtech
		{ "&notin;",	"\u2209" }, // not an element of, U+2209 ISOtech
		{ "&ni;",	"\u220b" }, // contains as member, U+220B ISOtech
		{ "&prod;",	"\u220f" }, // n-ary product = product sign, U+220F ISOamsb
		{ "&sum;",	"\u2211" }, // n-ary sumation, U+2211 ISOamsb
		{ "&minus;",	"\u2212" }, // minus sign, U+2212 ISOtech
		{ "&lowast;",	"\u2217" }, // asterisk operator, U+2217 ISOtech
		{ "&radic;",	"\u221a" }, // square root = radical sign, U+221A ISOtech
		{ "&prop;",	"\u221d" }, // proportional to, U+221D ISOtech
		{ "&infin;",	"\u221e" }, // infinity, U+221E ISOtech
		{ "&ang;",	"\u2220" }, // angle, U+2220 ISOamso
		{ "&and;",	"\u2227" }, // logical and = wedge, U+2227 ISOtech
		{ "&or;",	"\u2228" }, // logical or = vee, U+2228 ISOtech
		{ "&cap;",	"\u2229" }, // intersection = cap, U+2229 ISOtech
		{ "&cup;",	"\u222a" }, // union = cup, U+222A ISOtech
		{ "&int;",	"\u222b" }, // integral, U+222B ISOtech
		{ "&there4;",	"\u2234" }, // therefore, U+2234 ISOtech
		{ "&sim;",	"\u223c" }, // tilde operator = varies with = similar to, U+223C ISOtech
		{ "&cong;",	"\u2245" }, // approximately equal to, U+2245 ISOtech
		{ "&asymp;",	"\u2248" }, // almost equal to = asymptotic to, U+2248 ISOamsr
		{ "&ne;",	"\u2260" }, // not equal to, U+2260 ISOtech
		{ "&equiv;",	"\u2261" }, // identical to, U+2261 ISOtech
		{ "&le;",	"\u2264" }, // less-than or equal to, U+2264 ISOtech
		{ "&ge;",	"\u2265" }, // greater-than or equal to, U+2265 ISOtech
		{ "&sub;",	"\u2282" }, // subset of, U+2282 ISOtech
		{ "&sup;",	"\u2283" }, // superset of, U+2283 ISOtech
		{ "&nsub;",	"\u2284" }, // not a subset of, U+2284 ISOamsn
		{ "&sube;",	"\u2286" }, // subset of or equal to, U+2286 ISOtech
		{ "&supe;",	"\u2287" }, // superset of or equal to, U+2287 ISOtech
		{ "&oplus;",	"\u2295" }, // circled plus = direct sum, U+2295 ISOamsb
		{ "&otimes;",	"\u2297" }, // circled times = vector product, U+2297 ISOamsb
		{ "&perp;",	"\u22a5" }, // up tack = orthogonal to = perpendicular, U+22A5 ISOtech
		{ "&sdot;",	"\u22c5" }, // dot operator, U+22C5 ISOamsb

		// Miscellaneous Technical

		{ "&lceil;",	"\u2308" }, // left ceiling = apl upstile, U+2308 ISOamsc
		{ "&rceil;",	"\u2309" }, // right ceiling, U+2309 ISOamsc
		{ "&lfloor;",	"\u230a" }, // left floor = apl downstile, U+230A ISOamsc
		{ "&rfloor;",	"\u230b" }, // right floor, U+230B ISOamsc
		{ "&lang;",	"\u2329" }, // left-pointing angle bracket = bra, U+2329 ISOtech
		{ "&rang;",	"\u232a" }, // right-pointing angle bracket = ket, U+232A ISOtech
		{ "&loz;",	"\u25ca" }, // lozenge, U+25CA ISOpub

		// Miscellaneous Symbols

		{ "&spades;",	"\u2660" }, // black spade suit, U+2660 ISOpub
		{ "&clubs;",	"\u2663" }, // black club suit = shamrock, U+2663 ISOpub
		{ "&hearts;",	"\u2665" }, // black heart suit = valentine, U+2665 ISOpub
		{ "&diams;",	"\u2666" }, // black diamond suit, U+2666 ISOpub

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

		{ "&quot;",	"\"" }, // quotation mark = APL quote, U+0022 ISOnum
		{ "&amp;",	"\u0026" }, // ampersand, U+0026 ISOnum
		{ "&lt;",	"\u003c" }, // less-than sign, U+003C ISOnum
		{ "&gt;",	"\u003e" }, // greater-than sign, U+003E ISOnum

		// Latin Extended-A

		{ "&OElig;",	"\u0152" }, // latin capital ligature OE, U+0152 ISOlat2
		{ "&oelig;",	"\u0153" }, // latin small ligature oe, U+0153 ISOlat2
		{ "&Scaron;",	"\u0160" }, // latin capital letter S with caron, U+0160 ISOlat2
		{ "&scaron;",	"\u0161" }, // latin small letter s with caron, U+0161 ISOlat2
		{ "&Yuml;",	"\u0178" }, // latin capital letter Y with diaeresis, U+0178 ISOlat2

		// Spacing Modifier Letters

		{ "&circ;",	"\u02c6" }, // modifier letter circumflex accent, U+02C6 ISOpub
		{ "&tilde;",	"\u02dc" }, // small tilde, U+02DC ISOdia

		// General Punctuation

		{ "&ensp;",	"\u2002" }, // en space, U+2002 ISOpub
		{ "&emsp;",	"\u2003" }, // em space, U+2003 ISOpub
		{ "&thinsp;",	"\u2009" }, // thin space, U+2009 ISOpub
		{ "&zwnj;",	"\u200c" }, // zero width non-joiner, U+200C NEW RFC 2070
		{ "&zwj;",	"\u200d" }, // zero width joiner, U+200D NEW RFC 2070
		{ "&lrm;",	"\u200e" }, // left-to-right mark, U+200E NEW RFC 2070
		{ "&rlm;",	"\u200f" }, // right-to-left mark, U+200F NEW RFC 2070
		{ "&ndash;",	"\u2013" }, // en dash, U+2013 ISOpub
		{ "&mdash;",	"\u2014" }, // em dash, U+2014 ISOpub
		{ "&lsquo;",	"\u2018" }, // left single quotation mark, U+2018 ISOnum
		{ "&rsquo;",	"\u2019" }, // right single quotation mark, U+2019 ISOnum
		{ "&sbquo;",	"\u201a" }, // single low-9 quotation mark, U+201A NEW
		{ "&ldquo;",	"\u201c" }, // left float quotation mark, U+201C ISOnum
		{ "&rdquo;",	"\u201d" }, // right float quotation mark, U+201D ISOnum
		{ "&bdquo;",	"\u201e" }, // float low-9 quotation mark, U+201E NEW
		{ "&dagger;",	"\u2020" }, // dagger, U+2020 ISOpub
		{ "&Dagger;",	"\u2021" }, // float dagger, U+2021 ISOpub
		{ "&permil;",	"\u2030" }, // per mille sign, U+2030 ISOtech
		{ "&lsaquo;",	"\u2039" }, // single left-pointing angle quotation mark, U+2039 ISO proposed
		{ "&rsaquo;",	"\u203a" }, // single right-pointing angle quotation mark, U+203A ISO proposed
		{ "&euro;",	"\u20ac" }  // euro sign, U+20AC NEW
	};

	private static final Map entities = new TreeMap();
	private static final Map unicodes = new TreeMap();

	static
	{
		for ( int i = 0; i < UNICODE_TABLE.length; ++i )
		{
			String entity = UNICODE_TABLE[i][0];
			Character unicode = new Character( UNICODE_TABLE[i][1].charAt( 0 ) );
			entities.put( unicode, entity );
			unicodes.put( entity, unicode );
		}
	}

	private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

	/**
	 * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts
	 * some of the form handling to ensure that <code>KoLRequest</code> objects
	 * are instantiated on form submission rather than the <code>HttpRequest</code>
	 * objects created by the default HTML editor kit.
	 */

	public ViewFactory getViewFactory()
	{	return DEFAULT_FACTORY;
	}

	/**
	 * Registers thethat is supposed to be used for handling data submission
	 * to the Kingdom of Loathing server.
	 */

	private static class RequestViewFactory extends HTMLFactory
	{
		public View create( Element elem )
		{
			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.INPUT )
				return new KoLSubmitView( elem );

			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.IMG )
				return new KoLImageView( elem );

			return super.create( elem );
		}
	}

	private static final Pattern FILEID_PATTERN = Pattern.compile( "(\\d+)\\." );
	private static final Pattern ACQUIRE_PATTERN = Pattern.compile( "You acquire([^<]*?<b>.*?</b>.*?)</td>", Pattern.DOTALL );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );
	private static final Pattern BOOKSHELF_PATTERN = Pattern.compile( "onclick=\"location.href='(.*?)';\"", Pattern.DOTALL );

	public static final void downloadFile( String remote, File local )
	{
		if ( local.exists() )
			return;

		try
		{
			URLConnection connection = (new URL( null, remote, HttpTimeoutHandler.getInstance() )).openConnection();
			if ( remote.startsWith( "http://pics.communityofloathing.com" ) )
			{
				Matcher idMatcher = FILEID_PATTERN.matcher( local.getPath() );
				if ( idMatcher.find() )
					connection.setRequestProperty( "Referer", "http://www.kingdomofloathing.com/showplayer.php?who=" + idMatcher.group(1)  );
			}

			BufferedInputStream in = new BufferedInputStream( connection.getInputStream() );
			ByteArrayOutputStream outbytes = new ByteArrayOutputStream();

			byte [] buffer = new byte[4096];

			int offset;
			while ((offset = in.read(buffer)) > 0)
				outbytes.write(buffer, 0, offset);

			in.close();

			// If it's textual data, then go ahead and modify it so
			// that all the variables point to KoLmafia.

			if ( remote.endsWith( ".js" ) )
			{
				String text = outbytes.toString();
				outbytes.reset();

				text = StaticEntity.globalStringReplace( text, "location.hostname", "location.host" );
				outbytes.write( text.getBytes() );
			}

			FileOutputStream outfile = new FileOutputStream( local );
			outbytes.writeTo( outfile );
			outfile.close();
		}
		catch ( Exception e )
		{
			// This can happen whenever there is bad internet
			// or whenever the familiar is brand-new.
		}
	}

	/**
	 * Downloads the given file from the KoL images server
	 * and stores it locally.
	 */

	public static final URL downloadImage( String filename )
	{
		if ( filename == null || filename.equals( "" ) )
			return null;

		String localname = filename.substring( filename.indexOf( "/", "http://".length() ) + 1 );
		if ( localname.startsWith( "albums/" ) )
			localname = localname.substring( "albums/".length() );

		File localfile = new File( IMAGE_LOCATION, localname );
		File parentfile = new File( IMAGE_LOCATION, localname.substring( 0, localname.lastIndexOf( "/" ) + 1 ) );

		if ( !parentfile.exists() )
			parentfile.mkdirs();

		if ( !localfile.exists() || localfile.length() == 0 )
		{
			// If it's something contained inside of KoLmafia's JAR archive,
			// then download that one instead, as it won't be present on the
			// KoL image server.

			if ( JComponentUtilities.getImage( localname ) != null )
				StaticEntity.loadLibrary( IMAGE_LOCATION, IMAGE_DIRECTORY, localname );
			else
				downloadFile( filename, localfile );
		}

		try
		{
			return localfile.toURI().toURL();
		}
		catch ( Exception e )
		{
			// This can happen whenever there is bad internet
			// or whenever the familiar is brand-new.

			return null;
		}
	}

	private static class KoLImageView extends ImageView
	{
		public KoLImageView( Element elem )
		{	super( elem );
		}

		public URL getImageURL()
		{
			String src = (String) this.getElement().getAttributes().getAttribute( HTML.Attribute.SRC );

			if ( src == null )
				return null;

			return downloadImage( src );
		}
	}

	public static final String getEntities( String unicodeVersion )
	{
		// Iterate over all the characters in the string looking for unicode

		StringBuffer entityVersion = null;

		char ch;
		int start = 0;
		int length = unicodeVersion.length();

		for ( int i = 0; i < length; ++i )
		{
			ch = unicodeVersion.charAt(i);

			if ( Character.isJavaIdentifierPart( ch ) || Character.isWhitespace( ch ) )
				continue;

			String entity = (String) entities.get( new Character( ch ) );

			// If we don't have a translation, move along in string
			if ( entity == null )
				continue;

			// If we don't have a string buffer, make one
			if ( entityVersion == null )
				entityVersion = new StringBuffer();

			// Append prefix
			if ( i > start )
				entityVersion.append( unicodeVersion.substring( start, i ) );

			// Insert entity
			entityVersion.append( entity );

			// Start new prefix
			start = i + 1;
		}

		// If we didn't find anything, return original string
		if ( start == 0 )
			return unicodeVersion;

		// Append suffix
		if ( start < length )
			entityVersion.append( unicodeVersion.substring( start ) );

		return entityVersion.toString();
	}

	public static final String getStripped( String entityVersion )
	{
		int index = entityVersion.indexOf( "&" );

		// If there are no character entities, return original string
		if ( index < 0 )
			return entityVersion;

		// Otherwise, make a StringBuffer to create unicode version of input
		StringBuffer strippedVersion = null;
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

			// If we don't have a string buffer, make one
			if ( strippedVersion == null )
				strippedVersion = new StringBuffer();

			// Copy in prefix
			if ( index > start )
				strippedVersion.append( entityVersion.substring( start, index ) );

			String entity = entityVersion.substring( index, semi + 1 );

			if ( entity.equals( "&nbsp;" ) )
				strippedVersion.append( " " );
			else if ( entity.equals( "&quot;" ) )
				strippedVersion.append( "\"" );
			else if ( entity.equals( "&amp;" ) )
				strippedVersion.append( "&" );
			else if ( entity.equals( "&lt;" ) )
				strippedVersion.append( "<" );
			else if ( entity.equals( "&gt;" ) )
				strippedVersion.append( ">" );
			else if ( entity.equals( "&ntilde;" ) )
				strippedVersion.append( "n" );
			else if ( entity.equals( "&eacute;" ) )
				strippedVersion.append( "e" );

			// Skip past entity
			start = semi + 1;
			index = entityVersion.indexOf( "&", start );
		}

		// If we never translated an entity, return the original string
		if ( start == 0 )
			return entityVersion;

		// Append suffix
		if ( start < entityVersion.length() )
			strippedVersion.append( entityVersion.substring( start ) );

		// Return the new string
		return strippedVersion.toString();
	}

	public static final String getUnicode( String entityVersion )
	{
		int index = entityVersion.indexOf( "&" );

		// If there are no character entities, return original string
		if ( index < 0 )
			return entityVersion;

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
			Character unicode = (Character) unicodes.get( entity );

			// If we don't have a translation, skip past entity
			if ( unicode == null )
			{
				index = entityVersion.indexOf( "&", index + 1 );
				continue;
			}

			// If we don't have a string buffer, make one
			if ( unicodeVersion == null )
				unicodeVersion = new StringBuffer();

			// Copy in prefix
			if ( index > start )
				unicodeVersion.append( entityVersion.substring( start, index ) );

			// Insert unicode
			unicodeVersion.append( unicode.charValue() );

			// Skip past entity
			start = semi + 1;
			index = entityVersion.indexOf( "&", start );
		}

		// If we never translated an entity, return the original string
		if ( start == 0 )
			return entityVersion;

		// Append suffix
		if ( start < entityVersion.length() )
			unicodeVersion.append( entityVersion.substring( start ) );

		// Return the new string
		return unicodeVersion.toString();
	}

	/**
	 * Utility method which converts the given text into a form which
	 * can be displayed properly in a <code>RequestPane</code>.  This
	 * method is necessary primarily due to the bad HTML which is used
	 * but can still be properly rendered by post-3.2 browsers.
	 */

	public static final String getDisplayHTML( String location, String responseText )
	{
		if ( responseText == null || responseText.length() == 0 )
			return "";

		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		RequestLogger.updateDebugLog( "Rendering hypertext..." );
		String displayHTML = getFeatureRichHTML( location, responseText, false );

		displayHTML = LINE_BREAK_PATTERN.matcher( COMMENT_PATTERN.matcher( STYLE_PATTERN.matcher( SCRIPT_PATTERN.matcher(
			displayHTML ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ).replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" ).replaceAll( "<[Hh][Rr].*?>", "<br>" );

		// The default Java browser doesn't display blank lines correctly

		displayHTML = displayHTML.replaceAll( "<br><br>", "<br>&nbsp;<br>" );

		// Fix all the tables which decide to put a row end,
		// but no row beginning.

		displayHTML = displayHTML.replaceAll( "</tr><td", "</tr><tr><td" );

		// Fix all the super-small font displays used in the
		// various KoL panes.

		displayHTML = displayHTML.replaceAll( "font-size: .8em;", "" ).replaceAll( "<font size=[12]>", "" ).replaceAll(
			" class=small", "" ).replaceAll( " class=tiny", "" );

		// This is to replace all the rows with a black background
		// because they are not properly rendered.

		displayHTML = displayHTML.replaceAll( "<td valign=center><table[^>]*?><tr><td([^>]*?) bgcolor=black([^>]*?)>.*?</table></td>", "" );
		displayHTML = displayHTML.replaceAll( "<tr[^>]*?><td[^>]*bgcolor=\'?\"?black(.*?)</tr>", "" );
		displayHTML = displayHTML.replaceAll( "<table[^>]*title=.*?</table>", "" );

		// The default browser doesn't understand the table directive
		// style="border: 1px solid black"; turn it into a simple "border=1"

		displayHTML = displayHTML.replaceAll( "style=\"border: 1px solid black\"", "border=1" );

		// turn:  <form...><td...>...</td></form>
		// into:  <td...><form...>...</form></td>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)?(<td[^>]*>)", "$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></form>", "</form></td>" );

		// KoL also has really crazy nested Javascript links, and
		// since the default browser doesn't recognize these, be
		// sure to convert them to standard <A> tags linking to
		// the correct document.

		displayHTML = displayHTML.replaceAll( "<a[^>]*?\\([\'\"](.*?)[\'\"].*?>", "<a href=\"$1\">" );
		displayHTML = displayHTML.replaceAll( "<img([^>]*?) onClick=\'window.open\\(\"(.*?)\".*?\'(.*?)>", "<a href=\"$2\"><img$1 $3 border=0></a>" );

		// The search form for viewing players has an </html>
		// tag appearing right after </style>, which may confuse
		// the HTML parser.

		displayHTML = displayHTML.replaceAll( "</style></html>" , "</style>" );

		// Image links are mangled a little bit because they use
		// Javascript now -- fix them.

		displayHTML = displayHTML.replaceAll( "<img([^>]*?) onClick=\'descitem\\((\\d+)\\);\'>",
			"<a href=\"desc_item.php?whichitem=$2\"><img$1 border=0></a>" );

		// The last thing to worry about is the problems in
		// specific pages.

		// The first of these is the familiar page, where the
		// first "Take this one with you" link does not work.
		// On a related note, the sewer page suffers from an
		// odd problem as well, where you must remove cells in
		// the table before it works.

		displayHTML = displayHTML.replaceFirst( "<input class=button type=submit value=\"Take this one with you\">", "" );
		displayHTML = displayHTML.replaceFirst( "</td></tr><tr><td align=center>(<input class=button type=submit value='Take Items'>)", "$1" );

		// The second of these is the betting page.  Here, the
		// problem is an "onClick" in the input field, if the
		// Hagnk option is available.

		if ( displayHTML.indexOf( "whichbet" ) != -1 )
		{
			// Since the introduction of MMG bots, bets are usually
			// placed and taken instantaneously.  Therefore, the
			// search form is extraneous.

			displayHTML = displayHTML.replaceAll( "<center><b>Search.*?<center>", "<center>" );

			// Also, placing a bet is awkward through the KoLmafia
			// interface.  Remove this capability.

			displayHTML = displayHTML.replaceAll( "<center><b>Add.*?</form><br>", "<br>" );

			// Checkboxes were a safety which were added server-side,
			// but they do not really help anything and Java is not
			// very good at rendering them -- remove it.

			displayHTML = displayHTML.replaceFirst( "\\(confirm\\)", "" );
			displayHTML = displayHTML.replaceAll( "<input type=checkbox name=confirm>", "<input type=hidden name=confirm value=on>" );

			// In order to avoid the problem of having two submits,
			// which confuses the built-in Java parser, remove one
			// of the buttons and leave the one that makes sense.

			if ( KoLCharacter.canInteract() )
				displayHTML = displayHTML.replaceAll( "whichbet value='(\\d+)'><input type=hidden name=from value=0>.*?</td><td><input type=hidden",
					"whichbet value='$1'><input type=hidden name=from value=0><input class=button type=submit value=\"On Hand\"><input type=hidden" );
			else
				displayHTML = displayHTML.replaceAll( "whichbet value='(\\d+)'><input type=hidden name=from value=0>.*?</td><td><input type=hidden",
					"whichbet value='$1'><input type=hidden name=from value=1><input class=button type=submit value=\"In Hagnk's\"><input type=hidden" );
		}

		// The third of these is the outfit managing page,
		// which requires that the form for the table be
		// on the outside of the table.

		if ( displayHTML.indexOf( "action=account_manageoutfits.php" ) != -1 )
		{
			// turn:  <center><table><form>...</center></td></tr></form></table>
			// into:  <form><center><table>...</td></tr></table></center></form>

			displayHTML = displayHTML.replaceAll( "<center>(<table[^>]*>)(<form[^>]*>)", "$2<center>$1" );
			displayHTML = displayHTML.replaceAll( "</center></td></tr></form></table>", "</td></tr></table></center></form>" );
		}

		// The fourth of these is the fight page, which is
		// totally mixed up -- in addition to basic modifications,
		// also resort the combat item list.

		if ( displayHTML.indexOf( "action=fight.php" ) != -1 )
		{
			displayHTML = displayHTML.replaceAll( "<form(.*?)<tr><td([^>]*)>", "<tr><td$2><form$1" );
			displayHTML = displayHTML.replaceAll( "</td></tr></form>", "</form></td></tr>" );
		}

		// Doc Galaktik's page is going to get completely
		// killed, except for the main purchases.

		if ( displayHTML.indexOf( "action=galaktik.php" ) != -1 )
		{
			displayHTML = StaticEntity.globalStringReplace( displayHTML, "</tr><td valign=center>", "</tr><tr><td valign=center>" );
			displayHTML = StaticEntity.globalStringReplace( displayHTML, "<td>", "</td><td>" );
			displayHTML = StaticEntity.globalStringReplace( displayHTML, "</td></td>", "</td>" );

			displayHTML = displayHTML.replaceAll( "<table><table>(.*?)(<form action=galaktik\\.php method=post><input[^>]+><input[^>]+>)", "<table><tr><td>$2<table>$1<tr>" );
		}

		// The library bookshelf has some secretive Javascript
		// which needs to be removed.

		displayHTML = BOOKSHELF_PATTERN.matcher( displayHTML ).replaceAll( "href=\"$1\"" );

		// All HTML is now properly rendered!  Return the
		// compiled string.  Print it to the debug log for
		// reference purposes.

		RequestLogger.updateDebugLog( displayHTML );
		return displayHTML;
	}

	public static final String getFeatureRichHTML( String location, String text, boolean addComplexFeatures )
	{
		if ( text == null || text.length() == 0 )
			return "";

		StringBuffer buffer = new StringBuffer( text );
		getFeatureRichHTML( location, buffer, addComplexFeatures );
		return buffer.toString();
	}

	private static final String NO_HERMIT_TEXT = "<img src=\"http://images.kingdomofloathing.com/otherimages/mountains/mount4.gif\" width=100 height=100>";
	private static final String AUTO_HERMIT_TEXT = "<a href=\"hermit.php?autopermit=on\"><img src=\"http://images.kingdomofloathing.com/otherimages/mountains/hermitage.gif\" width=100 height=100 border=0></a>";

	private static final String NO_PERMIT_TEXT = "<p>You don't have a Hermit Permit, so you're not allowed to visit the Hermit.<p><center>";
	private static final String BUY_PERMIT_TEXT = NO_PERMIT_TEXT + "<a href=\"hermit.php?autopermit=on\">Buy a Hermit Permit</a></center></p><p><center>";

	public static final void getFeatureRichHTML( String location, StringBuffer buffer, boolean addComplexFeatures )
	{
		if ( buffer.length() == 0 )
			return;

		// Make all the character pane adjustments first, since
		// they only happen once and they occur frequently.

		if ( location.startsWith( "charpane.php" ) )
		{
			if ( addComplexFeatures )
				CharpaneRequest.decorate( buffer );

			return;
		}

		if ( location.indexOf( "menu" ) != -1 )
		{
			MoonPhaseRequest.decorate( buffer );
			return;
		}

		// Now handle the changes which only impact a single
		// page one at a time.

		if ( location.startsWith( "adventure.php" ) )
		{
			addFightButtons( location, buffer );

			if ( AdventureRequest.useMarmotClover( location, buffer.toString() ) )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}
		}
		else if ( location.startsWith( "ascend.php" ) )
		{
			addAscensionReminders( location, buffer );
		}
		else if ( location.startsWith( "ascensionhistory.php" ) )
		{
			if ( addComplexFeatures )
			{
				StaticEntity.singleStringReplace( buffer, "</head>", "<script language=\"Javascript\" src=\"/sorttable.js\"></script></head>" );
				StaticEntity.singleStringReplace( buffer, "<table><tr><td class=small>", "<table class=\"sortable\" id=\"history\"><tr><td class=small>" );
				StaticEntity.globalStringReplace( buffer, "<tr><td colspan=9", "<tr class=\"sortbottom\" style=\"display:none\"><td colspan=9" );
				StaticEntity.globalStringReplace( buffer, "<td></td>", "<td><img src=\"http://images.kingdomofloathing.com/itemimages/confused.gif\" title=\"No Data\" alt=\"No Data\" height=30 width=30></td>" );
			}
		}
		else if ( location.startsWith( "barrels.php" ) )
		{
			if ( AdventureRequest.useMarmotClover( location, buffer.toString() ) )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}
		}
		else if ( location.startsWith( "basement.php" ) )
		{
			BasementRequest.decorate( buffer );
		}
		else if ( location.startsWith( "bathole.php" ) )
		{
			StaticEntity.globalStringReplace( buffer, "action=bathole.php", "action=adventure.php" );
		}
		else if ( location.startsWith( "choice.php" ) )
		{
			addFightButtons( location, buffer );
			addChoiceSpoilers( buffer );
		}
		else if ( location.startsWith( "fight.php" ) )
		{
			addFightModifiers( location, buffer, addComplexFeatures );
		}
		else if ( location.startsWith( "hermit.php" ) )
		{
			if ( HermitRequest.useHermitClover( location ) )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}

			StaticEntity.singleStringReplace( buffer, NO_PERMIT_TEXT, BUY_PERMIT_TEXT );
		}
		else if ( location.startsWith( "hiddencity.php" ) )
		{
			addHiddenCityModifiers( buffer );
		}
		else if ( location.startsWith( "inventory.php" ) )
		{
			if ( KoLCharacter.inMuscleSign() )
				StaticEntity.globalStringReplace( buffer, "combine.php", "knoll.php?place=paster" );

			AdventureResult wand = KoLCharacter.getZapper();
			if ( wand != null )
			{
				StaticEntity.singleStringReplace( buffer, "]</a></font></td></tr></table></center>",
					"]</a>&nbsp;&nbsp;<a href=\"wand.php?whichwand=" + wand.getItemId() + "\">[zap items]</a></font></td></tr></table></center>" );
			}

			changeSphereImages( buffer );

			// Automatically name the outfit "backup" for simple save
			// purposes while adventuring in browser.

			StaticEntity.singleStringReplace( buffer, "<input type=text name=outfitname", "<input type=text name=outfitname value=\"Backup\"" );

			// Split the custom outfits from the normal outfits for
			// easier browsing.

			int selectBeginIndex = buffer.indexOf( "<option value=-" );
			if ( selectBeginIndex != -1 && addComplexFeatures )
			{
				int selectEndIndex = buffer.indexOf( "</select>", selectBeginIndex );
				String outfitString = buffer.substring( selectBeginIndex, selectEndIndex );
				buffer.delete( selectBeginIndex, selectEndIndex );

				int formEndIndex = buffer.indexOf( "</form>", selectBeginIndex ) + 7;

				StringBuffer customString = new StringBuffer();
				customString.append( "<tr><td align=right><form name=outfit2 action=inv_equip.php><input type=hidden name=action value=\"outfit\"><input type=hidden name=which value=2><b>Custom:</b></td><td><select name=whichoutfit><option value=0>(select a custom outfit)</option>" );
				customString.append( outfitString );
				customString.append( "</select></td><td> <input class=button type=submit value=\"Dress Up!\"></form></td></tr></table>" );
				StaticEntity.globalStringDelete( customString, "Custom: " );

				buffer.insert( formEndIndex, customString.toString() );

				StaticEntity.singleStringReplace( buffer, "<form name=outfit", "<table><tr><td align=right><form name=outfit" );
				StaticEntity.singleStringReplace( buffer, "<select", "</td><td><select" );
				StaticEntity.singleStringReplace( buffer, "</select>", "</select></td><td>" );
				StaticEntity.singleStringReplace( buffer, "</form>", "</form></td></tr>" );

				StaticEntity.globalStringReplace( buffer, "<select", "<select style=\"width: 250px\"" );
			}
		}
		else if ( location.indexOf( "lchat.php" ) != -1 )
		{
			StaticEntity.globalStringDelete( buffer, "spacing: 0px;" );
			StaticEntity.globalStringReplace( buffer, "cycles++", "cycles = 0" );
			StaticEntity.globalStringReplace( buffer, "location.hostname", "location.host" );

			StaticEntity.singleStringReplace( buffer, "if (postedgraf",
				"if (postedgraf == \"/exit\") { document.location.href = \"chatlaunch.php\"; return true; } if (postedgraf" );

			// This is a hack to fix KoL chat as handled in earlier
			// versions of Opera (doubled chat).

			StaticEntity.singleStringReplace( buffer, "http.onreadystatechange", "executed = false; http.onreadystatechange" );
			StaticEntity.singleStringReplace( buffer, "readyState==4) {", "readyState==4 && !executed) { executed = true;" );
		}
		else if ( location.startsWith( "manor3.php" ) )
		{
			addWineCellarSpoilers( buffer );
		}
		else if ( location.startsWith( "mountains.php" ) )
		{
			StaticEntity.singleStringReplace( buffer, NO_HERMIT_TEXT, AUTO_HERMIT_TEXT );
		}
		else if ( location.startsWith( "multiuse.php" ) )
		{
			addMultiuseModifiers( buffer );
		}
		else if ( location.startsWith( "palinshelves.php" ) )
		{
			StaticEntity.singleStringReplace( buffer, "</html>", "<script language=\"Javascript\" src=\"/palinshelves.js\" /></html>" );
		}
		else if ( location.startsWith( "pvp.php" ) )
		{
			StaticEntity.singleStringReplace( buffer, "value=rank checked", "value=rank" );
			StaticEntity.singleStringReplace( buffer, "value=flowers", "value=flowers checked" );
		}
		else if ( location.startsWith( "rats.php" ) )
		{
			addTavernSpoilers( buffer );
		}
		else if ( location.startsWith( "searchplayer.php" ) )
		{
			StaticEntity.singleStringReplace( buffer, "name=pvponly", "name=pvponly checked" );
			StaticEntity.singleStringReplace( buffer, "value=0 checked", "value=0" );

			if ( KoLCharacter.isHardcore() )
				StaticEntity.singleStringReplace( buffer, "value=1", "value=1 checked" );
			else
				StaticEntity.singleStringReplace( buffer, "value=2", "value=2 checked" );
		}
		else if ( location.startsWith( "valhalla.php" ) )
		{
			addAscensionReminders( location, buffer );
		}

		// Always select the contents of text fields when you click on them
		// to make for easy editing.

		if ( addComplexFeatures )
		{
			// Now handle all the changes which happen on a lot of
			// different pages rather than just one or two.

			changePotionImages( buffer );

			if ( StaticEntity.getBooleanProperty( "relayAddsUseLinks" ) )
				addUseLinks( location, buffer );

			if ( buffer.indexOf( "showplayer.php" ) != -1 && buffer.indexOf( "rcm.js" ) == -1 && buffer.indexOf( "rcm.2.js" ) == -1 )
				addChatFeatures( buffer );

			StaticEntity.globalStringReplace( buffer, "type=text ", "type=text onFocus='this.select();' " );
		}

		String defaultColor = StaticEntity.getProperty( "defaultBorderColor" );
		if ( !defaultColor.equals( "blue" ) )
		{
			StaticEntity.globalStringReplace( buffer, "bgcolor=blue", "bgcolor=\"" + defaultColor + "\"" );
			StaticEntity.globalStringReplace( buffer, "border: 1px solid blue", "border: 1px solid " + defaultColor );
		}
	}

	public static final void addChatFeatures( StringBuffer buffer )
	{
		StaticEntity.singleStringReplace( buffer, "</head>", "<script language=\"Javascript\"> var " + ChatRequest.getRightClickMenu() + " </script>" +
			 "<script language=\"Javascript\" src=\"/images/scripts/rcm.2.js\"></script></head>" );

		StaticEntity.singleStringReplace( buffer, "</body>", "<div id='menu' class='rcm'></div></body>" );
	}

	private static final void addPreAscensionReminders( StringBuffer buffer )
	{
		buffer.delete( buffer.indexOf( "<p>Are you" ), buffer.indexOf( "<p><center>" ) );
		StaticEntity.singleStringReplace( buffer, "<p>Please", " Please" );

		StringBuffer predictions = new StringBuffer();

		predictions.append( "</center></td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );
		predictions.append( "<td><div style=\"padding-top: 10px; padding-left: 10px; padding-right: 10px; padding-bottom: 10px\"><font size=-1>" );
		MoonPhaseDatabase.addPredictionHTML( predictions, new Date(), MoonPhaseDatabase.getPhaseStep(), false );
		predictions.append( "</font></div></td></tr><tr><td colspan=3><br>" );
		predictions.append( LINE_BREAK );
		predictions.append( LINE_BREAK );

		StaticEntity.singleStringReplace( buffer, "</center><p>", predictions.toString() );

		int startPoint = 0;

		if ( KoLCharacter.getClassType().equals( KoLCharacter.SEAL_CLUBBER ) )
			startPoint = 1000;
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.TURTLE_TAMER ) )
			startPoint = 2000;
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER ) )
			startPoint = 3000;
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
			startPoint = 4000;
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.DISCO_BANDIT ) )
			startPoint = 5000;
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.ACCORDION_THIEF ) )
			startPoint = 6000;

		StringBuffer reminders = new StringBuffer();
		reminders.append( "<br><table>" );

		reminders.append( "<tr><td><img id = 'current' src=\"http://images.kingdomofloathing.com/" );
		reminders.append( FamiliarsDatabase.getFamiliarImageLocation( KoLCharacter.getFamiliar().getId() ) );
		reminders.append( "\"></td><td><select id=\"familiar\" style=\"width: 250px\" onChange=\"var select = document.getElementById('familiar');  var option = select.options[select.selectedIndex]; top.charpane.document.location.href = '/KoLmafia/sideCommand?cmd=familiar+' + option.value; document.getElementById('current').src = 'http://images.kingdomofloathing.com/' + option.id; return true;\"><option value=\"none\">- No Familiar -</option>" );

		Object [] familiars = KoLCharacter.getFamiliarList().toArray();

		for ( int i = 1; i < familiars.length; ++i )
		{
			reminders.append( "<option id=\"" );
			reminders.append( FamiliarsDatabase.getFamiliarImageLocation( ((FamiliarData)familiars[i]).getId() ) );
			reminders.append( "\" value=\"" );
			reminders.append( StaticEntity.globalStringReplace( ((FamiliarData)familiars[i]).getRace(), " ", "+" ) );
			reminders.append( "\"" );

			if ( familiars[i].equals( KoLCharacter.getFamiliar() ) )
				reminders.append( " selected" );

			reminders.append( ">" );
			reminders.append( ((FamiliarData)familiars[i]).getRace() );
			reminders.append( " (" );
			reminders.append( ((FamiliarData)familiars[i]).getWeight() );
			reminders.append( " lbs.)" );
			reminders.append( "</option>" );
		}

		reminders.append( "</select></td><td><input type=submit class=button value=\"Ascend\"><input type=hidden name=confirm value=on><input type=hidden name=confirm2 value=on></td></tr>" );
		reminders.append( "</table>" );

		reminders.append( "<br><table cellspacing=10 cellpadding=10><tr>" );
		reminders.append( "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Didn't Buy</th></tr><tr><td align=center><font size=\"-1\">" );

		ArrayList skillList = new ArrayList();
		for ( int i = 0; i < availableSkills.size(); ++i )
			skillList.add( String.valueOf( ((UseSkillRequest)availableSkills.get(i)).getSkillId() ) );

		listPermanentSkills( reminders, skillList, startPoint );
		reminders.append( "</font></td></tr></table></td>" );
		reminders.append( "<td bgcolor=\"#eeeeff\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Common Stuff You Didn't Do</th></tr><tr><td align=center><font size=\"-1\">" );

		if ( KoLCharacter.hasChef() )
			reminders.append( "<nobr>blow up your chef</nobr><br>" );

		if ( KoLCharacter.hasBartender() )
			reminders.append( "<nobr>blow up your bartender</nobr><br>" );

		AdventureResult roe = new AdventureResult( "rubber emo roe", 1, false );
		if ( inventory.contains( roe ) )
			reminders.append( "<nobr>send your rubber emo roes to Veracity</nobr><br>" );

		AdventureResult trinket = new AdventureResult( "valuable trinket", 1, false );
		if ( inventory.contains( trinket ) )
			reminders.append( "<nobr>send your valuable trinkets to Veracity</nobr><br>" );

		AdventureResult lime = new AdventureResult( "lime", 1, false );
		if ( inventory.contains( lime ) )
			reminders.append( "<nobr>send your limes to shwei</nobr><br>" );

		AdventureResult cocoabo = new AdventureResult( "stuffed cocoabo", 1, false );
		if ( inventory.contains( cocoabo ) )
			reminders.append( "<nobr>send your stuffed cocoabos to holatuwol</nobr><br>" );

		reminders.append( "</font></td></tr></table></td></tr></table>" );

		reminders.append( "<br><br>" );
		StaticEntity.singleStringReplace( buffer, "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)", reminders.toString() );
		return;
	}

	private static final void addAscensionReminders( String location, StringBuffer buffer )
	{
		if ( location.indexOf( "ascend.php" ) != -1 )
		{
			addPreAscensionReminders( buffer );
			return;
		}

		if ( buffer.indexOf( "<form" ) == -1 )
			return;

		boolean newMoonSign = buffer.indexOf( "<option value=9>The Packrat</option><option value=10>" ) != -1;

		// What we're going to do is kill the standard form and replace it with
		// one that requires a lot less scrolling while still retaining all of
		// the form fields.  But first, extract needed information from it.

		ArrayList softSkills = new ArrayList();
		ArrayList hardSkills = new ArrayList();

		Matcher permedMatcher = Pattern.compile( "<b>Permanent Skills:</b>.*?</table>", Pattern.DOTALL ).matcher( buffer.toString() );
		if ( permedMatcher.find() )
		{
			Matcher skillMatcher = Pattern.compile( "desc_skill.php\\?whichskill=(\\d+)[^>]+>[^<+]+</a>(.*?)</td>" ).matcher( permedMatcher.group() );

			while ( skillMatcher.find() )
			{
				softSkills.add( skillMatcher.group(1) );
				if ( skillMatcher.group(2).length() > 0 )
					hardSkills.add( skillMatcher.group(1) );
			}
		}

		ArrayList recentSkills = new ArrayList();
		Matcher recentMatcher = Pattern.compile( "<b>Current Skills:</b>.*?</table>", Pattern.DOTALL ).matcher( buffer.toString() );
		if ( recentMatcher.find() )
		{
			Matcher skillMatcher = Pattern.compile( "value=(\\d+)" ).matcher( recentMatcher.group() );
			while ( skillMatcher.find() )
				recentSkills.add( skillMatcher.group(1) );
		}


		// Now we begin replacing the standard Valhalla form with one that is much
		// more compact.

		int endIndex = buffer.indexOf( "</form>" );
		String suffix = buffer.toString().substring( endIndex + 7 );
		buffer.delete( buffer.indexOf( "<form" ), buffer.length() );

		String skillListScript = "var a, b; if ( document.getElementById( 'skillsview' ).options[0].selected ) { a = 'soft'; b = 'hard'; } else { a = 'hard'; b = 'soft'; } document.getElementById( a + 'skills' ).style.display = 'inline'; document.getElementById( b + 'skills' ).style.display = 'none'; void(0);";

		// Add some holiday predictions to the page to make things more useful,
		// since people sometimes forget KoLmafia has a calendar.

		buffer.append( "</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<td><div style=\"background-color: #ffffcc; padding-top: 10px; padding-left: 10px; padding-right: 10px; padding-bottom: 10px\"><font size=-1>" );
		MoonPhaseDatabase.addPredictionHTML( buffer, new Date(), MoonPhaseDatabase.getPhaseStep() );
		buffer.append( "</font></div></td></tr><tr><td colspan=3><br><br>" );
		buffer.append( LINE_BREAK );
		buffer.append( LINE_BREAK );

		buffer.append( "<form name=\"ascform\" action=valhalla.php method=post onSubmit=\"document.ascform.whichsign.value = document.ascform.whichsignhc.value; return true;\">" );
		buffer.append( "<input type=hidden name=action value=\"resurrect\"><input type=hidden name=pwd value=\"\"><center><table>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Lifestyle:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"asctype\"><option value=1>Casual</option><option value=2>Softcore</option><option value=3 selected>Hardcore</option></select>" );
		buffer.append( "</td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td align=right><b>New Class:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichclass\"><option value=0 selected></option>" );
		buffer.append( "<option value=1>Seal Clubber</option><option value=2>Turtle Tamer</option>" );
		buffer.append( "<option value=3>Pastamancer</option><option value=4>Sauceror</option>" );
		buffer.append( "<option value=5>Disco Bandit</option><option value=6>Accordion Thief</option>" );
		buffer.append( "</select></td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Gender:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"gender\"><option value=1" );
		if ( !KoLCharacter.getAvatar().endsWith( "_f.gif" ) )
			buffer.append( " selected" );
		buffer.append( ">Male</option><option value=2" );
		if ( KoLCharacter.getAvatar().endsWith( "_f.gif" ) )
			buffer.append( " selected" );
		buffer.append( ">Female</option></select>" );

		buffer.append( "</td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td colspan=2>&nbsp;</td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Moon Sign:</b>&nbsp;</td><td>" );
		buffer.append( "<input type=\"hidden\" name=\"whichsign\" value=\"\">" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichsignhc\"><option value=0 selected></option>" );
		buffer.append( "<option value=1>The Mongoose</option><option value=2>The Wallaby</option><option value=3>The Vole</option>" );
		buffer.append( "<option value=4>The Platypus</option><option value=5>The Opossum</option><option value=6>The Marmot</option>" );
		buffer.append( "<option value=7>The Wombat</option><option value=8>The Blender</option><option value=9>The Packrat</option>" );
		if ( newMoonSign )
			buffer.append( "<option value=10>Bad Moon</option>" );
		buffer.append( "</select></td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Restrictions:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichpath\"><option value=0 selected>No dietary restrictions</option><option value=1>Boozetafarian</option><option value=2>Teetotaler</option><option value=3>Oxygenarian</option></select>" );
		buffer.append( "</td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Skill to Keep:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=keepskill><option value=9999 selected></option><option value=0 selected>(no skill)</option>" );

		int skillId;
		for ( int i = 0; i < recentSkills.size(); ++i )
		{
			skillId = Integer.parseInt( (String) recentSkills.get(i) );
			if ( skillId == 0 )
				continue;

			buffer.append( "<option value=" );
			buffer.append( skillId );
			buffer.append( ">" );
			buffer.append( ClassSkillsDatabase.getSkillName( skillId ) );

			if ( skillId % 1000 == 0 )
				buffer.append( " (Trivial)" );

			buffer.append( "</option>" );
		}

		buffer.append( "</select></td></tr>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<tr><td colspan=2>&nbsp;</td></tr><tr><td>&nbsp;</td><td>" );
		buffer.append( "<input class=button type=submit value=\"Resurrect\"><input type=hidden name=\"confirm\" value=on></td></tr></table></center></form>" );
		buffer.append( LINE_BREAK );
		buffer.append( LINE_BREAK );

		// Finished with adding all the data in a more compact form.  Now, we
		// go ahead and add in all the missing data that players might want to
		// look at to see which class to go for next.

		buffer.append( "<center><br><br><select id=\"skillsview\" onChange=\"" + skillListScript + "\"><option>Unpermed Softcore Skills</option><option selected>Unpermed Hardcore Skills</option></select>" );
		buffer.append( LINE_BREAK );

		buffer.append( "<br><br><div id=\"softskills\" style=\"display:none\">" );
		buffer.append( LINE_BREAK );
		createSkillTable( buffer, softSkills );
		buffer.append( LINE_BREAK );

		buffer.append( "</div><div id=\"hardskills\" style=\"display:inline\">" );
		buffer.append( LINE_BREAK );
		createSkillTable( buffer, hardSkills );
		buffer.append( LINE_BREAK );
		buffer.append( "</div></center>" );
		buffer.append( LINE_BREAK );
		buffer.append( LINE_BREAK );

		buffer.append( suffix );
	}

	private static final void createSkillTable( StringBuffer buffer, ArrayList skillList )
	{
		buffer.append( "<table width=\"80%\"><tr>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ffcccc\"><table><tr><th style=\"text-decoration: underline; text-align: left;\">Muscle Skills</th></tr><tr><td><font size=\"-1\">" );
		listPermanentSkills( buffer, skillList, 1000 );
		listPermanentSkills( buffer, skillList, 2000 );
		buffer.append( "</font></td></tr></table></td>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ccccff\"><table><tr><th style=\"text-decoration: underline; text-align: left;\">Mysticality Skills</th></tr><tr><td><font size=\"-1\">" );
		listPermanentSkills( buffer, skillList, 3000 );
		listPermanentSkills( buffer, skillList, 4000 );
		buffer.append( "</font></td></tr></table></td>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ccffcc\"><table><tr><th style=\"text-decoration: underline; text-align: left;\">Moxie Skills</th></tr><tr><td><font size=\"-1\">" );
		listPermanentSkills( buffer, skillList, 5000 );
		listPermanentSkills( buffer, skillList, 6000 );
		buffer.append( "</font></td></tr></table></td>" );
		buffer.append( "</tr></table>" );
	}

	private static final void listPermanentSkills( StringBuffer buffer, ArrayList skillList, int startingPoint )
	{
		String skillName;
		for ( int i = 0; i < 100; ++i )
		{
			skillName = ClassSkillsDatabase.getSkillName( startingPoint + i );
			if ( skillName == null )
				continue;

			buffer.append( "<nobr>" );
			boolean alreadyPermed = skillList.contains( String.valueOf( startingPoint + i ) );
			if ( alreadyPermed )
				buffer.append( "<font color=darkgray><s>" );

			buffer.append( "<a onClick=\"skill('" );
			buffer.append( startingPoint + i );
			buffer.append( "');\">" );
			buffer.append( skillName );
			buffer.append( "</a>" );

			if ( alreadyPermed )
				buffer.append( "</s></font>" );

			buffer.append( "</nobr><br>" );
		}
	}

	private static final String getActionName( String action )
	{
		if ( action.equals( "attack" ) )
			return FightRequest.getCurrentRound() == 0 ? "again" : "attack";

		if ( action.equals( "steal" ) || action.equals( "script" ) )
			return action;

		int skillId = StaticEntity.parseInt( action );
		String name = ClassSkillsDatabase.getSkillName( skillId ).toLowerCase();

		switch ( skillId )
		{
		case 15: // CLEESH
		case 7002: // Shake Hands
		case 7003: // Hot Breath
		case 7004: // Cold Breath
		case 7005: // Spooky Breath
		case 7006: // Stinky Breath
		case 7007: // Sleazy Breath
			name = StaticEntity.globalStringDelete( name, " " );
			break;

		case 7001: // Give In To Your Vampiric Urges
			name = "bakula";
			break;

		case 7008: // Moxious Maneuver
			name = "moxman";
			break;

		case 7010: // red bottle-rocket
		case 7011: // blue bottle-rocket
		case 7012: // orange bottle-rocket
		case 7013: // purple bottle-rocket
		case 7014: // black bottle-rocket
			name = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( name, "fire "), "bottle-" );
			break;

		case 2103: // Head + Knee Combo
		case 2105: // Head + Shield Combo
		case 2106: // Knee + Shield Combo
		case 2107: // Head + Knee + Shield Combo
			name = name.substring( 0, name.length() - 6 );
			break;

		case 1003: // thrust-smack
			name = "thrust";
			break;

		case 1004: // lunge-smack
		case 1005: // lunging thrust-smack
			name = "lunge";
			break;

		case 2:    // Chronic Indigestion
		case 7009: // Magic Missile
		case 3004: // Entangling Noodles
		case 3009: // Lasagna Bandages
		case 3019: // Fearful Fettucini
			name = name.substring( name.lastIndexOf( " " ) + 1 );
			break;

		case 3003: // Minor Ray of Something
		case 3005: // eXtreme Ray of Something
		case 3007: // Cone of Whatever
		case 3008: // Weapon of the Pastalord
		case 3020: // Spaghetti Spear
		case 4003: // Stream of Sauce
		case 4009: // Wave of Sauce
		case 5019: // Tango of Terror
			name = name.substring( 0, name.indexOf( " " ) );
			break;

		case 5003: // Disco Eye-Poke
		case 5012: // Disco Face Stab
			name = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( name.substring( 6 ), "-" ), " " );
			break;

		case 5005: // Disco Dance of Doom
			name = "dance1";
			break;

		case 5008: // Disco Dance II: Electric Boogaloo
			name = "dance2";
			break;
		}

		return name;
	}

	private static final void addFightButton( String urlString, StringBuffer response, StringBuffer buffer, String action, boolean isEnabled )
	{
		String name = getActionName( action );
		buffer.append( "<input type=\"button\" onClick=\"document.location.href='" );

		if ( FightRequest.getCurrentRound() == 0 )
		{
			String location = "main.php";

			int startIndex = response.indexOf( "<a href=\"" );
			if ( startIndex != -1 )
				location = response.substring( startIndex + 9, response.indexOf( "\"", startIndex + 10 ) );

			buffer.append( location );
			isEnabled &= buffer.indexOf( "adventure.php" ) != -1;
		}
		else
		{
			buffer.append( "fight.php?" );

			if ( action.equals( "script" ) )
			{
				buffer.append( "action=" );

				if ( urlString.endsWith( "action=script" ) && isEnabled )
				{
					name = "abort";
					buffer.append( "abort" );
				}
				else
				{
					buffer.append( "custom" );
				}
			}
			else if ( action.equals( "attack" ) || action.equals( "steal" ) )
			{
				buffer.append( "action=" );
				buffer.append( action );
			}
			else
			{
				buffer.append( "action=skill&whichskill=" );
				buffer.append( action );
				isEnabled &= ClassSkillsDatabase.getMPConsumptionById( StaticEntity.parseInt( action ) ) <= KoLCharacter.getCurrentMP();
			}
		}

		buffer.append( "'; void(0);\" value=\"" );
		buffer.append( name );

		if ( isEnabled )
			buffer.append( "\">&nbsp;" );
		else
			buffer.append( "\" disabled>&nbsp;" );
	}

	private static final void addFightButtons( String urlString, StringBuffer buffer )
	{
		int debugIndex = buffer.indexOf( "<font size=1>" );
		if ( debugIndex != -1 )
			buffer.delete( debugIndex, buffer.indexOf( "</font>", debugIndex ) );

		if ( !StaticEntity.getBooleanProperty( "relayAddsCustomCombat" ) )
			return;

		int insertionPoint = buffer.indexOf( "<tr" );
		if ( insertionPoint != -1 )
		{
			StringBuffer actionBuffer = new StringBuffer();
			actionBuffer.append( "<tr><td align=left>" );

			addFightButton( urlString, buffer, actionBuffer, "attack", true );

			if ( KoLCharacter.isMoxieClass() )
				addFightButton( urlString, buffer, actionBuffer, "steal", FightRequest.getCurrentRound() == 1 );

			if ( KoLCharacter.hasSkill( "Entangling Noodles" ) )
				addFightButton( urlString, buffer, actionBuffer, "3004", FightRequest.getCurrentRound() > 0 );

			addFightButton( urlString, buffer, actionBuffer, "script", FightRequest.getCurrentRound() > 0 );

			for ( int i = 1; i <= 5; ++i )
			{
				String action = StaticEntity.getProperty( "customCombatSkill" + i );
				if ( !action.equals( "" ) )
					addFightButton( urlString, buffer, actionBuffer, action, FightRequest.getCurrentRound() > 0 );
			}

			actionBuffer.append( "</td></tr><tr height=4><td></td></tr>" );
			buffer.insert( insertionPoint, actionBuffer.toString() );
		}
	}

	private static final void addFightModifiers( String location, StringBuffer buffer, boolean addComplexFeatures )
	{
		if ( addComplexFeatures )
			addFightButtons( location, buffer );

		// Change bang potion names in item dropdown
		changePotionNames( buffer );

		// Change stone sphere names in item dropdown
		changeSphereNames( buffer );

		if ( StaticEntity.getBooleanProperty( "relayAddsRoundNumber" ) )
		{
			int combatIndex = buffer.indexOf( "!</b>" );
			if ( combatIndex != -1 )
				buffer.insert( combatIndex, ": Round " + FightRequest.getCurrentRound() );
		}

		if ( !StaticEntity.getBooleanProperty( "relayAddsMonsterHealth" ) )
			return;

		if ( FightRequest.getLastMonster() == null || FightRequest.getLastMonster().getAdjustedHP(0) == 0 )
			return;

		int nameIndex = buffer.indexOf( "<span id='monname" );
		if ( nameIndex == -1 )
			return;

		int combatIndex = buffer.indexOf( "</span>", nameIndex );
		if ( combatIndex == -1 )
			return;

		StringBuffer monsterData = new StringBuffer();
		monsterData.append( "<br /><font size=2 color=gray>HP: " );
		monsterData.append( FightRequest.getMonsterHealth() );
		monsterData.append( ", Atk: " );
		monsterData.append( FightRequest.getMonsterAttack() );
		monsterData.append( ", Def: " );
		monsterData.append( FightRequest.getMonsterDefense() );

		List items = FightRequest.getLastMonster().getItems();
		if ( !items.isEmpty() )
		{
			monsterData.append( "<br />Drops: " );
			for ( int i = 0; i < items.size(); ++i )
			{
				if ( i != 0 ) monsterData.append( ", " );
				monsterData.append( items.get(i) );
			}
		}

		monsterData.append( "</font>" );
		buffer.insert( combatIndex + 7, monsterData.toString() );
	}

	private static final void addMultiuseModifiers( StringBuffer buffer )
	{
		// Change bang potion names in item dropdown
		changePotionNames( buffer );
	}

	private static final void addWineCellarSpoilers( StringBuffer buffer )
	{
		// Change dusty bottle names in item dropdown
		changeDustyBottleNames( buffer );
	}

	private static final void changePotionImages( StringBuffer buffer )
	{
		ConsumeItemRequest.ensureUpdatedPotionEffects();

		if ( buffer.indexOf( "exclam.gif" ) == -1 )
			return;

		ArrayList potionNames = new ArrayList();
		ArrayList potionEffects = new ArrayList();

		for ( int i = 819; i <= 827; ++i )
		{
			String name = TradeableItemDatabase.getItemName( i );
			if ( buffer.indexOf( name ) != -1 )
			{
				String effect = StaticEntity.getProperty( "lastBangPotion" + i );
				if ( !effect.equals( "" ) )
				{
					potionNames.add( name );
					potionEffects.add( effect );
				}
			}
		}

		if ( potionNames.isEmpty() )
			return;

		for ( int i = 0; i < potionNames.size(); ++i )
		{
			String name = (String)potionNames.get(i);
			String effect = (String)potionEffects.get(i);

			StaticEntity.singleStringReplace( buffer, name + "</b>",
					name + " of " + effect + "</b>" );
			StaticEntity.singleStringReplace( buffer, name + "s</b>",
					name + "s of " + effect + "</b>" );
		}
	}

	private static final void changePotionNames( StringBuffer buffer )
	{
		ConsumeItemRequest.ensureUpdatedPotionEffects();

		for ( int i = 819; i <= 827; ++i )
		{
			String name = TradeableItemDatabase.getItemName( i );
			if ( buffer.indexOf( name ) != -1 )
			{
				String effect = StaticEntity.getProperty( "lastBangPotion" + i );
				if ( effect.equals( "" ) )
					continue;

				StaticEntity.globalStringReplace( buffer, name, name + " of " + effect );
				// Pluralize correctly
				StaticEntity.globalStringReplace( buffer, name + " of " + effect + "s", name + "s of " + effect );
			}
		}
	}

	private static final Pattern GLYPH_PATTERN = Pattern.compile( "title=\"Arcane Glyph #(\\d)\"" );

	private static final void changeDustyBottleNames( StringBuffer buffer )
	{
		FightRequest.ensureUpdatedSphereEffects();

		int glyphs[] = new int[3];

		Matcher matcher = GLYPH_PATTERN.matcher( buffer );

		if ( !matcher.find() )
			return;
		glyphs[0] = StaticEntity.parseInt ( matcher.group(1) );

		if ( !matcher.find() )
			return;
		glyphs[1] = StaticEntity.parseInt ( matcher.group(1) );

		if ( !matcher.find() )
			return;
		glyphs[2] = StaticEntity.parseInt ( matcher.group(1) );

		int wines[] = new int[3];

		for ( int i = 2271; i <= 2276; ++i )
		{
			int glyph = StaticEntity.getIntegerProperty( "lastDustyBottle" + i );
			for ( int j = 0; j < 3; ++j )
				if ( glyph == glyphs[j] )
				{
					wines[j] = i;
					break;
				}
		}

		for ( int i = 0; i < 3; ++i )
		{
			String name = TradeableItemDatabase.getItemName( wines[i] );
			StaticEntity.globalStringReplace( buffer, name, ( i + 1 ) + " " + name );
		}
	}

	private static final void changeSphereImages( StringBuffer buffer )
	{
		FightRequest.ensureUpdatedSphereEffects();

		changeSphereImage( buffer, "spheremoss.gif", 2174 );
		changeSphereImage( buffer, "spheresmooth.gif", 2175 );
		changeSphereImage( buffer, "spherecrack.gif", 2176 );
		changeSphereImage( buffer, "sphererough.gif", 2177 );
	}

	private static final void changeSphereImage( StringBuffer buffer, String image, int itemId )
	{
		if ( buffer.indexOf( image ) == -1 )
			return;

		String name = TradeableItemDatabase.getItemName( itemId );
		if ( buffer.indexOf( name ) == -1 )
			return;

		String effect = StaticEntity.getProperty( "lastStoneSphere" + itemId );
		if ( effect.equals( "" ) )
			return;

		StaticEntity.globalStringReplace( buffer, name, name + " of " + effect );
	}

	private static final void addHiddenCityModifiers( StringBuffer buffer )
	{
		// Change stone sphere names in item dropdown
		changeSphereNames( buffer );
	}

	private static final void changeSphereNames( StringBuffer buffer )
	{
		for ( int i = 2174; i <= 2177; ++i )
		{
			String name = TradeableItemDatabase.getItemName( i );
			String effect = StaticEntity.getProperty( "lastStoneSphere" + i );

			if ( buffer.indexOf( name ) != -1 && !effect.equals( "" ) )
				StaticEntity.globalStringReplace( buffer, name, name + " of " + effect );
		}
	}

	private static final void addUseLinks( String location, StringBuffer buffer )
	{
		if ( buffer.indexOf( "You acquire" ) == -1 )
			return;

		// No use link if you get the item via pickpocketing; you're still in battle
		if ( buffer.indexOf( "deftly slip your fingers" ) != -1 )
			return;

		String text = buffer.toString();
		buffer.setLength( 0 );

		Matcher useLinkMatcher = ACQUIRE_PATTERN.matcher( text );

		int specialLinkId = 0;
		boolean addedInlineLink = false;

		String specialLinkText = null;

		while ( useLinkMatcher.find() )
		{
			String itemName = useLinkMatcher.group(1);
			if ( itemName.indexOf( "<br>" ) != -1 )
				itemName = itemName.substring( 0, itemName.indexOf( "<br>" ) );

			int itemCount = itemName.indexOf( ":" ) != -1 ? 1 : 2;

			if ( itemCount == 1 )
				itemName = itemName.substring( itemName.indexOf( ":" ) + 1 ).replaceAll( "<.*?>", "" ).trim();
			else
			{
				itemName = itemName.replaceAll( "<.*?>", "" );
				itemName = itemName.substring( itemName.indexOf( " " ) + 1 ).trim();
			}

			int itemId = TradeableItemDatabase.getItemId( itemName, itemCount );

			String useType = null;
			String useLocation = null;

			boolean addCreateLink = location != null && location.indexOf( "combine.php" ) == -1 && location.indexOf( "cocktail.php" ) == -1 &&
				location.indexOf( "cook.php" ) == -1 && location.indexOf( "paster" ) == -1 && location.indexOf( "smith" ) == -1;

			AdventureResult creation = null;
			ItemCreationRequest irequest = null;

			int mixingMethod = NOCREATE;
			int consumeMethod = TradeableItemDatabase.getConsumptionType( itemId );

			// Retrieve the known ingredient uses for the item.
			SortedListModel creations = ConcoctionsDatabase.getKnownUses( itemId );

			// If you find goat cheese, let the trapper link handle it.
			addCreateLink &= !creations.isEmpty() && itemId != 322;

			// Dictionaries and bridges should link to the chasm quest.
			addCreateLink &= itemId != FightRequest.DICTIONARY1.getItemId() && itemId != AdventureRequest.BRIDGE.getItemId();

			// Enchanted beans are primarily used for the beanstalk quest.
			addCreateLink &= itemId != KoLAdventure.BEAN.getItemId() || KoLCharacter.getLevel() < 10 || KoLCharacter.hasItem( KoLAdventure.SOCK ) || KoLCharacter.hasItem( KoLAdventure.ROWBOAT );

			// Skip items which are multi-use or are mp restores.
			addCreateLink &= consumeMethod != CONSUME_MULTIPLE && consumeMethod != MP_RESTORE;

			if ( addCreateLink )
			{
				addCreateLink = false;

				for ( int i = 0; !addCreateLink && i < creations.size(); ++i )
				{
					creation = (AdventureResult) creations.get(i);
					mixingMethod = ConcoctionsDatabase.getMixingMethod( creation.getItemId() );

					// Only accept if it's a creation method that the editor kit
					// currently understands and links.

					switch ( mixingMethod )
					{
					case NOCREATE:
					case PIXEL:
					case ROLLING_PIN:
					case TOY:
					case CLOVER:
					case STILL_BOOZE:
					case STILL_MIXER:
					case SMITH:
					case SMITH_WEAPON:
					case SMITH_ARMOR:
					case CATALYST:
						continue;
					}

					irequest = ItemCreationRequest.getInstance( creation.getItemId() );
					addCreateLink = ConcoctionsDatabase.isPermittedMethod( mixingMethod ) && irequest != null && irequest.getQuantityPossible() > 0;
				}
			}

			// Certain items get use special links to minimize the amount
			// of scrolling to find the item again.

			if ( location.startsWith( "inventory.php" ) )
			{
				switch ( itemId )
				{
				case 1423:
				case 1424:
				case 1425:
				case 1426:
				case 1427:

					specialLinkId = itemId;
					specialLinkText = "squeeze";
					break;

				case 2079:
				case 2080:
				case 2081:
				case 2083:
				case 2095:

					specialLinkId = itemId;
					specialLinkText = "fold";
					break;

				case 2221:
				case 2222:
				case 2223:
				case 2224:
				case 2225:
				case 2226:

					// specialLinkId = itemId;
					// specialLinkText = "melt";
					break;
				}
			}

			// If you can add a creation link, then add one instead.
			// That way, the player can click and KoLmafia will save
			// the player a click or two (well, if they trust it).

			if ( addCreateLink )
			{
				switch ( mixingMethod )
				{
				case STARCHART:
					useType = StarChartRequest.CHART.getCount( inventory ) + "," + StarChartRequest.STARS.getCount( inventory ) + "," + StarChartRequest.LINES.getCount( inventory );
					useLocation = "starchart.php";
					break;

				case COMBINE:
					useType = "combine";
					useLocation = KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "combine.php";
					break;

				case MIX:
				case MIX_SPECIAL:
				case MIX_SUPER:
					useType = "mix";
					useLocation = "cocktail.php";
					break;

				case COOK:
				case COOK_REAGENT:
				case SUPER_REAGENT:
				case COOK_PASTA:
					useType = "cook";
					useLocation = "cook.php";
					break;

				case JEWELRY:
				case EXPENSIVE_JEWELRY:
					useType = "jewelry";
					useLocation = "jewelry.php";
					break;
				}
			}
			else
			{
				switch ( consumeMethod )
				{
				case GROW_FAMILIAR:

					if ( itemId == 275 )
					{
						useType = "council";
						useLocation = "council.php";
					}

					break;

				case CONSUME_EAT:

					if ( itemId == 322 )
					{
						AdventureResult cheese = new AdventureResult( itemId, 1 );
						useType = String.valueOf( cheese.getCount( inventory ) );
						useLocation = "trapper.php";
					}
					else
					{
						useType = KoLCharacter.canEat() ? "eat" : null;
						useLocation = "inv_eat.php?pwd=&which=1&whichitem=";
					}

					break;

				case CONSUME_DRINK:
					useType = KoLCharacter.canDrink() ? "drink" : null;
					useLocation = "inv_booze.php?pwd=&which=1&whichitem=";
					break;

				case CONSUME_MULTIPLE:
				case HP_RESTORE:
				case MP_RESTORE:
				case HPMP_RESTORE:

					AdventureResult result = new AdventureResult( itemId, 1 );
					itemCount = Math.min( ConsumeItemRequest.maximumUses( itemId ), result.getCount( inventory ) );

					if ( itemCount == 0 )
					{
						useType = null;
						useLocation = null;
					}
					else if ( itemCount == 1 )
					{
						useType = "use";
						useLocation = "inv_use.php?pwd=&which=1&whichitem=";
					}
					else if ( StaticEntity.getBooleanProperty( "relayUsesInlineLinks" ) )
					{
						useType = "use";
						useLocation = "#";
					}
					else
					{
						useType = "use";
						useLocation = "multiuse.php?passitem=";
					}

					break;

				case CONSUME_USE:
				case MESSAGE_DISPLAY:
				case INFINITE_USES:

					if ( itemId == 146 )
					{
						AdventureResult planks = new AdventureResult( 140, 1 );
						if ( !KoLCharacter.hasItem( planks ) && HermitRequest.getWorthlessItemCount() > 0 )
						{
							useType = "planks";
							useLocation = "hermit.php?autopermit=on&action=trade&pwd&quantity=1&whichitem=140";
						}

					}

					if ( itemId == ConsumeItemRequest.MACGUFFIN_DIARY )
					{
						useType = "read";
						useLocation = "diary.php?textversion=1";
					}

					if ( useType == null )
					{
						useType = itemId == 2095 ? "fold" : "use";
						useLocation = "inv_use.php?pwd=&which=3&whichitem=";
					}

					break;

				case EQUIP_HAT:
				case EQUIP_WEAPON:
				case EQUIP_OFFHAND:
				case EQUIP_SHIRT:
				case EQUIP_PANTS:
				case EQUIP_ACCESSORY:
				case EQUIP_FAMILIAR:

					useType = null;
					int outfit = EquipmentDatabase.getOutfitWithItem( itemId );

					if ( outfit != -1 && EquipmentDatabase.hasOutfit( outfit ) )
					{
						useType = "outfit";
						useLocation = "inv_equip.php?action=outfit&which=2&whichoutfit=" + outfit;
					}
					else
					{
						useType = "equip";
						useLocation = "inv_equip.php?pwd=&which=2&action=equip&whichitem=";
					}

					break;

				case CONSUME_ZAP:
					useType = "zap";
					useLocation = "wand.php?whichwand=";
					break;

				default:

					// Soft green echo eyedrop antidote gets an uneffect link

					if ( itemId == UneffectRequest.REMEDY.getItemId() )
					{
						useType = "use";
						useLocation = "uneffect.php";
					}

					// Special handling for star charts, lines, and stars, where
					// KoLmafia shows you how many of each you have.

					else if ( itemId == StarChartRequest.CHART.getItemId() || itemId == StarChartRequest.STARS.getItemId() || itemId == StarChartRequest.LINES.getItemId() )
					{
						useType = StarChartRequest.CHART.getCount( inventory ) + "," + StarChartRequest.STARS.getCount( inventory ) + "," + StarChartRequest.LINES.getCount( inventory );
						useLocation = "starchart.php";
					}

					// Hedge maze puzzle and hedge maze key have a link to the maze
					// for easy access.

					else if ( itemId == SorceressLair.HEDGE_KEY.getItemId() || itemId == SorceressLair.PUZZLE_PIECE.getItemId() )
					{
						useType = "maze";
						useLocation = "hedgepuzzle.php";
					}

					// The different kinds of ores will only have a link if they're
					// the ones applicable to the trapper quest.

					else if ( (itemId == 363 || itemId == 364 || itemId == 365) )
					{
						AdventureResult ore = new AdventureResult( StaticEntity.getProperty( "trapperOre" ), itemCount, false );

						if ( ore.getItemId() == itemId )
						{
							useType = String.valueOf( ore.getCount( inventory ) );
							useLocation = "trapper.php";
						}
					}

					// Pixels have handy links indicating how many white pixels are
					// present in the player's inventory.

					else if ( itemId == 459 || itemId == 461 || itemId == 462 || itemId == 463 )
					{
						AdventureResult white = new AdventureResult( 459, 1 );
						useType = String.valueOf( white.getCount( inventory ) + ItemCreationRequest.getInstance( 459 ).getQuantityPossible() ) + " white";
						useLocation = "mystic.php";
					}

					// Disintegrating sheet music gets a link which lets you sing it
					// to yourself.  We'll call it "hum" for now.

					else if ( itemId == 2192 )
					{
						useType = "sing";
						useLocation = "curse.php?action=use&pwd&whichitem=2192&targetplayer=" + KoLCharacter.getUserName();
					}

					// Link which uses the plans when you acquire the planks.

					else if ( itemId == 140 )
					{
						AdventureResult plans = new AdventureResult( 146, 1 );
						if ( KoLCharacter.hasItem( plans ) )
						{
							useType = "plans";
							useLocation = "inv_use.php?pwd=&which=3&whichitem=";
							itemId = 146;
						}
					}

					// Link to the guild upon completion of the Citadel quest.

					else if ( itemId == 1656 )
					{
						useType = "guild";
						useLocation = "guild.php?place=paco";
					}

					// Link to the untinkerer if you find an abridged dictionary.

					else if ( itemId == AdventureRequest.ABRIDGED.getItemId() )
					{
						useType = "untinker";
						useLocation = "town_right.php?action=untinker&pwd&whichitem=";
					}

					// Link to the chasm if you just untinkered a dictionary.

					else if ( itemId == FightRequest.DICTIONARY1.getItemId() || itemId == AdventureRequest.BRIDGE.getItemId() )
					{
						useType = "chasm";
						useLocation = "mountains.php?pwd&orcs=1";
					}

					// Bounty items get a count and a link
					// to the Bounty Hunter Hunter.

					else if ( itemId == StaticEntity.getIntegerProperty( "currentBountyItem" ) ||
						  TradeableItemDatabase.isBountyItem( itemId ) )
					{
						StaticEntity.setProperty( "currentBountyItem", String.valueOf( itemId ) );
						AdventureResult item = new AdventureResult( itemId, 0 );
						useType = String.valueOf( item.getCount( inventory ) );
						useLocation = "bhh.php";
					}
				}
			}

			if ( useType != null && useLocation != null )
			{
				if ( useLocation.endsWith( "=" ) )
					useLocation += itemId;

				if ( useLocation.equals( "#" ) )
				{
					addedInlineLink = true;
					useLinkMatcher.appendReplacement( buffer, "You acquire$1" );

					// Append a multi-use field rather than forcing
					// an additional page load.

					buffer.append( "</td></tr><tr><td colspan=2 align=center><div id=\"multiuse" );
					buffer.append( itemId );
					buffer.append( "\">" );

					buffer.append( "<form><input type=text size=3 id=\"quantity" );
					buffer.append( itemId );
					buffer.append( "\" value=" );
					buffer.append( Math.min( itemCount, ConsumeItemRequest.maximumUses( itemId ) ) );
					buffer.append( ">&nbsp;<input type=button class=button value=\"Use\" onClick=\"multiUse('" );

					if ( consumeMethod == MP_RESTORE )
						buffer.append( "skills.php" );
					else
						buffer.append( "multiuse.php" );

					buffer.append( "', " );
					buffer.append( itemId );
					buffer.append( "); void(0);\"></form></div>" );
				}
				else if ( !StaticEntity.getBooleanProperty( "relayUsesInlineLinks" ) || !useLocation.startsWith( "inv" ) )
				{
					useLinkMatcher.appendReplacement( buffer, "You acquire$1 <font size=1>[<a href=\"" +
						useLocation.trim() + "\">" + useType + "</a>]</font>" );
				}
				else
				{
					addedInlineLink = true;
					String [] pieces = useLocation.toString().split( "\\?" );

					useLinkMatcher.appendReplacement( buffer, "You acquire$1 <font size=1>[<a href=\"javascript: " +
						"singleUse('" + pieces[0] + "', '" + pieces[1] + "'); void(0);\">" + useType + "</a>]</font>" );
				}

				buffer.append( "</td>" );
			}
			else
			{
				useLinkMatcher.appendReplacement( buffer, "$0" );
			}
		}

		useLinkMatcher.appendTail( buffer );

		if ( addedInlineLink )
			StaticEntity.singleStringReplace( buffer, "</head>", "<script language=Javascript src=\"/basics.js\"></script></head>" );

		if ( specialLinkText != null )
		{
			StaticEntity.singleStringReplace( buffer, "</center></blockquote>", "<p><center><a href=\"inv_use.php?pwd&which=2&whichitem=" + specialLinkId + "\">[" +
				specialLinkText + " it again]</a></center></blockquote>" );
		}
	}

	private static final void addChoiceSpoilers( StringBuffer buffer )
	{
		// For the plus sign teleportitis adventure, replace the book
		// message with a link to the plus sign.

		StaticEntity.singleStringReplace( buffer, "It's actually a book.  Read it.",
			"It's actually a book. <font size=1>[<a href=\"inv_use.php?pwd=&which=3&whichitem=818\">read it</a>]</font>" );

		// For everything else, make sure that it's an actual choice adventure
		Matcher choiceMatcher = CHOICE_PATTERN.matcher( buffer.toString() );
		if ( !choiceMatcher.find() )
			return;

		// Find the options for the choice we've encountered
		int choice = StaticEntity.parseInt( choiceMatcher.group(1) );
		String [][] possibleDecisions = AdventureDatabase.choiceSpoilers( choice );

		if ( possibleDecisions == null )
			return;

		int index1 = 0, index2 = 0;

		String text = buffer.toString();
		buffer.setLength(0);

		for ( int i = 0; i < possibleDecisions[2].length; ++i )
		{
			index2 = text.indexOf( "</form>", index1 );

			// If KoL says we've run out of choices, quit now
			if ( index2 == -1 )
				break;

			// Start spoiler text
			buffer.append( text.substring( index1, index2 ) );
			buffer.append( "<br><font size=-1>(" );

			// Say what the choice will give you
			String item = possibleDecisions[2][i];
			buffer.append( item );

			// If this choice helps complete an outfit...
			if ( possibleDecisions.length > 3 )
			{
				String itemId = possibleDecisions[3][i];

				// If this decision leads to an item...
				if ( itemId != null )
				{
					// List # in inventory
					buffer.append( " - " );
					AdventureResult result = new AdventureResult( StaticEntity.parseInt( itemId ), 1 );

					int available = KoLCharacter.hasEquipped( result ) ? 1 : 0;
					available += result.getCount( inventory );

					buffer.append( available );
					buffer.append( " in inventory" );
				}
			}

			// Finish spoiler text
			buffer.append( ")</font></form>" );
			index1 = index2 + 7;
		}

		buffer.append( text.substring( index1 ) );
	}

	private static final void addTavernSpoilers( StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength(0);

		String layout = StaticEntity.getProperty( "tavernLayout" );

		for ( int i = 1; i <= 25; ++i )
		{
			int squareType = Character.digit( layout.charAt(i-1), 10 );

			switch ( squareType )
			{
			case 0:
				break;

			case 1:
				text = text.replaceFirst( "(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
					" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/adventureimages/rat.gif\" border=0></a>" );
				break;

			case 2:
				text = text.replaceFirst( "(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
					" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/otherimages/sigils/fratboy.gif\" border=0></a>" );
				break;

			case 3:
				text = text.replaceFirst( "(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
					" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/adventureimages/faucet.gif\" height=60 width=60 border=0></a>" );
				break;

			case 4:
				text = text.replaceFirst( "(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
					" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/adventureimages/ratsworth.gif\" height=60 width=60 border=0></a>" );
				break;
			}
		}

		buffer.append( text );
	}

	private static class KoLSubmitView extends FormView
	{
		public KoLSubmitView( Element elem )
		{	super( elem );
		}

		public Component createComponent()
		{
			Component c = super.createComponent();

			if ( c != null && (c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox) )
				c.setBackground( Color.white );

			return c;
		}

		public void submitData( String data )
		{
			// Get the element

			Element inputElement = this.getElement();

			if ( inputElement == null )
				return;

			// Get the "value" associated with this input

			String value = (String)inputElement.getAttributes().getAttribute( HTML.Attribute.VALUE );

			// If there is no value, we won't be able to find the
			// frame that handles this form.

			if ( value == null )
				return;

			// Retrieve the frame which is being used by this form
			// viewer.

			RequestFrame frame = this.findFrame( value );

			// If there is no frame, then there's nothing to
			// refresh, so return.

			if ( frame == null )
				return;

			// Retrieve the form element so that you know where you
			// need to submit the data.

			Element formElement = inputElement;

			while ( formElement != null && formElement.getAttributes().getAttribute( StyleConstants.NameAttribute ) != HTML.Tag.FORM )
				formElement = formElement.getParentElement();

			// If the form element is null, then there was no
			// enclosing form for the <INPUT> tag, so you can
			// return, doing nothing.

			if ( formElement == null )
				return;

			// Now that you know you have a form element,
			// get the action field, attach the data, and
			// refresh the appropriate request frame.

			String action = (String) formElement.getAttributes().getAttribute( HTML.Attribute.ACTION );

			// If there is no action, how do we know which page to
			// connect to?  We assume it's the originating page.

			if ( action == null )
				action = frame.getCurrentLocation();

			// Now get the data fields we will submit to this form

			String [] elements = data.split( "&" );
			String [] fields = new String[ elements.length ];

			if ( elements[0].length() > 0 )
			{
				for ( int i = 0; i < elements.length; ++i )
					fields[i] = elements[i].substring( 0, elements[i].indexOf( "=" ) );
			}
			else
				fields[0] = "";

			// Prepare the element string -- make sure that
			// you don't have duplicate fields.

			for ( int i = 0; i < elements.length; ++i )
				for ( int j = i + 1; j < elements.length; ++j )
					if ( elements[i] != null && elements[j] != null && fields[i].equals( fields[j] ) )
						elements[j] = null;

			if ( action.indexOf( "?" ) != -1 )
			{
				// For quirky URLs where there's a question mark
				// in the middle of the URL, just string the data
				// onto the URL.  This is the way browsers work,
				// so it's the way KoL expects the data.

				StringBuffer actionString = new StringBuffer();
				actionString.append( action );

				for ( int i = 0; i < elements.length; ++i )
					if ( elements[i] != null )
					{
						actionString.append( '&' );
						actionString.append( elements[i] );
					}

				try
				{
					KoLRequest.VISITOR.constructURLString( URLDecoder.decode( actionString.toString(), "ISO-8859-1" ) );
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
					KoLRequest.VISITOR.constructURLString( actionString.toString() );
				}
			}
			else
			{
				// For normal URLs, the form data can be submitted
				// just like in every other request.

				KoLRequest.VISITOR.constructURLString( action );
				if ( elements[0].length() > 0 )
					for ( int i = 0; i < elements.length; ++i )
						if ( elements[i] != null )
							KoLRequest.VISITOR.addEncodedFormField( elements[i] );
			}

			frame.refresh( KoLRequest.VISITOR );
		}

		private RequestFrame findFrame( String value )
		{
			KoLFrame [] frames = StaticEntity.getExistingFrames();
			String search = "value=\"" + value + "\"";

			for ( int i = 0; i < frames.length; ++i )
				if ( frames[i] instanceof RequestFrame && ((RequestFrame)frames[i]).containsText( search ) )
					return (RequestFrame) frames[i];

			return null;
		}
	}

	/**
	 * Utility method used to determine the KoLRequest that
	 * should be sent, given the appropriate location.
	 */

	public static final KoLRequest extractRequest( String location )
	{
		if ( location.indexOf( "pics.communityofloathing.com" ) != -1 )
		{
			downloadImage( location );
			location = location.substring( location.indexOf( "/" ) );

			KoLRequest.VISITOR.constructURLString( location );
			KoLRequest.VISITOR.responseCode = 200;
			KoLRequest.VISITOR.responseText = "<html><img src=\"" + location + "\"></html>";
			KoLRequest.VISITOR.responseText = KoLRequest.VISITOR.responseText;
			return KoLRequest.VISITOR;
		}

		String [] urlData = location.split( "\\?" );
		String [] formData = urlData.length == 1 ? new String[0] : urlData[1].split( "&" );
		String [] currentField;

		KoLRequest.VISITOR.constructURLString( urlData[0] );

		for ( int i = 0; i < formData.length; ++i )
		{
			currentField = formData[i].split( "=" );

			if ( currentField.length == 2 )
				KoLRequest.VISITOR.addFormField( currentField[0], currentField[1] );
			else
				KoLRequest.VISITOR.addFormField( formData[i] );
		}

		return KoLRequest.VISITOR;
	}
}
