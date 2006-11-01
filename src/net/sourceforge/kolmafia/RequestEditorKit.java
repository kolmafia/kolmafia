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

import java.awt.Color;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;

import javax.swing.text.View;
import javax.swing.text.Element;
import javax.swing.text.ViewFactory;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.FormView;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.HTMLEditorKit;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of a standard <code>HTMLEditorKit</code> which overrides the
 * <code>getViewFactory()</code> method in order to return a different factory
 * instance which properly handles data submission requests.
 */

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

// Because the browser can render quotes, go ahead and
// leave them (changing them breaks forms).

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

	private static Map entities = new TreeMap();
	private static Map unicodes = new TreeMap();

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
	private static final Pattern IMAGESERVER_PATTERN = Pattern.compile( "http://images\\.kingdomofloathing\\.com/[^\\s\">]+" );
	private static final Pattern COLOR_PATTERN = Pattern.compile( "(color|class)=\"?\'?([^\"\'>]*)" );

	private static final Pattern ACQUIRE_PATTERN = Pattern.compile( "You acquire([^<]*?<b>.*?</b>.*?)</td>", Pattern.DOTALL );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option.*?value=(.*?)>.*?\\((.*?)\\)</option>" );
	private static final Pattern BOOKSHELF_PATTERN = Pattern.compile( "onclick=\"location.href='(.*?)';\"", Pattern.DOTALL );

	private static void downloadFile( File local, String remote )
	{
		try
		{
			URLConnection connection = (new URL( remote )).openConnection();
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

			outbytes.writeTo( new FileOutputStream( local ) );
		}
		catch ( Exception e )
		{
			// This can happen whenever there is bad internet
			// or whenever the familiar is brand-new.
		}
	}

	/**
	 * Allow mass download of images so that you are always
	 * using a local image cache.
	 */

	public static void downloadImages( String text )
	{
		Matcher imageMatcher = IMAGESERVER_PATTERN.matcher( text );
		while ( imageMatcher.find() )
			downloadImage( imageMatcher.group() );
	}

	/**
	 * Downloads the given file from the KoL images server
	 * and stores it locally.
	 */

	public static URL downloadImage( String filename )
	{
		if ( filename == null || filename.equals( "" ) )
			return null;

		String localname = filename.substring( filename.indexOf( "/", "http://".length() ) + 1 );
		if ( localname.startsWith( "albums/" ) )
			localname = localname.substring( "albums/".length() );

		if ( filename.indexOf( "images.kingdomofloathing.com" ) != -1 )
			filename = filename.replaceAll( "images\\.kingdomofloathing\\.com", IMAGE_SERVER );

		File localfile = new File( "images/" + localname );
		File parentfile = new File( "images/" + localname.substring( 0, localname.lastIndexOf( "/" ) + 1 ) );

		if ( !parentfile.exists() )
			parentfile.mkdirs();

		if ( !localfile.exists() || localfile.length() == 0 )
		{
			// If it's something contained inside of KoLmafia's JAR archive,
			// then download that one instead, as it won't be present on the
			// KoL image server.

			if ( JComponentUtilities.getImage( localname ) != null )
				StaticEntity.loadLibrary( localname );
			else
				downloadFile( localfile, filename );
		}

		try
		{
			return localfile.toURL();
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
			String src = (String) getElement().getAttributes().getAttribute( HTML.Attribute.SRC );

			if ( src == null )
				return null;

			return downloadImage( src );
		}
	}

	public static final String getEntities( String unicodeVersion )
	{
		// Iterate over all the characters in the string looking for unicode
		int length = unicodeVersion.length();
		StringBuffer entityVersion = null;
		int start = 0;

		for ( int i = 0; i < length; ++i )
		{
			Character unicode = new Character( unicodeVersion.charAt( i ) );
			String entity = (String)entities.get( unicode );

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

	public static final String getUnicode( String entityVersion )
	{	return getUnicode( entityVersion, true );
	}

	public static final String getUnicode( String entityVersion, boolean replaceQuotes )
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
				break;

			// Replace entity with unicode
			String entity = entityVersion.substring( index, semi + 1 );
			Character unicode = !replaceQuotes && entity.equals( "&quot;" ) ? null : (Character)unicodes.get( entity );

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
	 * can be displayed properly in a <code>JEditorPane</code>.  This
	 * method is necessary primarily due to the bad HTML which is used
	 * but can still be properly rendered by post-3.2 browsers.
	 */

	public static final String getDisplayHTML( String location, String responseText )
	{
		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		KoLmafia.getDebugStream().println( "Rendering hypertext..." );
		String displayHTML = getFeatureRichHTML( location, responseText );

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

			displayHTML = sortItemList( "whichitem", displayHTML );
			displayHTML = sortItemList( "whichitem2", displayHTML );
		}

		// Doc Galaktik's page is going to get completely
		// killed, except for the main purchases.

		if ( displayHTML.indexOf( "action=galaktik.php") != -1 )
			displayHTML = displayHTML.replaceFirst( "<table><table.*", "" );


		// The library bookshelf has some secretive Javascript
		// which needs to be removed.

		displayHTML = BOOKSHELF_PATTERN.matcher( displayHTML ).replaceAll( "href=\"$1\"" );

		// All HTML is now properly rendered!  Return the
		// compiled string.  Print it to the debug log for
		// reference purposes.

		KoLmafia.getDebugStream().println( displayHTML );
		return displayHTML;
	}

	public static String getFeatureRichHTML( String location, String text )
	{
		StringBuffer buffer = new StringBuffer( text );
		getFeatureRichHTML( location, buffer );
		return buffer.toString();
	}


	public static void getFeatureRichHTML( String location, StringBuffer buffer )
	{
		// If you found a marmot clover, it would have
		// automatically been disassembled.  Update
		// the HTML to reflect this.

		int cloverIndex = buffer.indexOf( "you look down and notice a ten-leaf clover" );
		if ( cloverIndex != -1 )
		{
			StaticEntity.singleStringReplace( buffer, "<b>ten-leaf clover</b>", "<b>disassembled clover</b>" );
			StaticEntity.singleStringReplace( buffer, "clover.gif", "disclover.gif" );
			StaticEntity.singleStringReplace( buffer, "370834526", "328909735" );
		}

		// Now, for a little fun HTML manipulation.  See
		// if there's an item present, and if so, modify
		// it so that you get a use link.

		if ( location.indexOf( "charpane.php" ) != -1 && StaticEntity.getBooleanProperty( "relayAddsRestoreLinks" ) )
			addRestoreLinks( buffer );

		if ( location.indexOf( "charpane.php" ) != -1 && StaticEntity.getBooleanProperty( "relayAddsUpArrowLinks" ) )
			addUpArrowLinks( buffer );

		if ( StaticEntity.getBooleanProperty( "relayAddsUseLinks" ) )
			addUseLinks( location, buffer );

		if ( location.indexOf( "fight.php" ) != -1 )
			addFightModifiers( buffer );

		if ( location.indexOf( "choice.php" ) != -1 )
			addChoiceSpoilers( buffer );

		if ( location.indexOf( "rats.php" ) != -1 )
			addTavernSpoilers( buffer );

		if ( location.indexOf( "ascend.php" ) != -1 || location.indexOf( "valhalla.php" ) != -1 )
			addAscensionReminders( location, buffer );

		String defaultColor = StaticEntity.getProperty( "defaultBorderColor" );
		if ( !defaultColor.equals( "blue" ) )
		{
			StaticEntity.globalStringReplace( buffer, "bgcolor=blue", "bgcolor=\"" + defaultColor + "\"" );
			StaticEntity.globalStringReplace( buffer, "border: 1px solid blue", "border: 1px solid " + defaultColor );
		}

		StaticEntity.globalStringReplace( buffer, "images.kingdomofloathing.com", IMAGE_SERVER );
	}

	private static void addAscensionReminders( String location, StringBuffer buffer )
	{
		if ( location.indexOf( "ascend.php" ) != -1 )
		{
			StaticEntity.singleStringReplace( buffer,
				"<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)",
				"<table><tr><td align=left valign=center><input type=submit class=button value=\"Ascend\">&nbsp;&nbsp;&nbsp;&nbsp;</td><td align=left><input type=checkbox name=confirm> I remembered to buy the skill I wanted.<br><input type=checkbox name=confirm2> I remembered to stock up for my next ascension.</td></tr></table>" );
			return;
		}

		if ( buffer.indexOf( "<form" ) == -1 )
			return;

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

		buffer.append( "<form action=valhalla.php method=post>" );
		buffer.append( "<input type=hidden name=action value=\"resurrect\"><input type=hidden name=pwd value=\"\">" );

		buffer.append( "<center><table><tr><td align=right><b>New Class:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=whichclass><option value=0>- select a class -</option><option value=1>Seal Clubber</option><option value=2>Turtle Tamer</option><option value=3>Pastamancer</option><option value=4>Sauceror</option><option value=5>Disco Bandit</option><option value=6>Accordion Thief</option></select>" );
		buffer.append( "</td></tr><tr><td align=right><b>Gender:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=gender><option value=1>Male</option><option value=2>Female</option></select>" );
		buffer.append( "</td></tr><tr><td align=right><b>Skill to Keep:</b>&nbsp;</td><td>" );

		buffer.append( "<select style=\"width: 250px\" name=keepskill><option value=9999 selected>- select a skill -</option><option value=0>(no skill)</option>" );

		int skillID;
		for ( int i = 0; i < recentSkills.size(); ++i )
		{
			skillID = Integer.parseInt( (String) recentSkills.get(i) );
			if ( skillID == 0 )
				continue;

			buffer.append( "<option value=" );
			buffer.append( skillID );
			buffer.append( ">" );
			buffer.append( ClassSkillsDatabase.getSkillName( skillID ) );
			buffer.append( "</option>" );
		}

		buffer.append( "</select></td></tr><tr><td align=right><b>Moon Sign:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=whichsign><option value=0>- Muscle Signs -</option><option value=1>The Mongoose</option><option value=2>The Wallaby</option><option value=3>The Vole</option><option value=0>- Mysticality Signs -</option><option value=4>The Platypus</option><option value=5>The Opossum</option><option value=6>The Marmot</option><option value=0>- Moxie Signs -</option><option value=7>The Wombat</option><option value=8>The Blender</option><option value=9>The Packrat</option></select>" );
		buffer.append( "</td></tr><tr><td align=right><b>Hardcore:</b>&nbsp;</td><td><input type=checkbox id=hardcore name=hardcore onClick=\"var softstyle = document.getElementById(\'softskills\').style; var hardstyle = document.getElementById(\'hardskills\').style; if ( document.getElementById('hardcore').checked ) { softstyle.display = 'none'; hardstyle.display = ''; } else { hardstyle.display = 'none'; softstyle.display = ''; } return true;\"></td></tr>" );
		buffer.append( "<tr><td align=right><b>Restrictions:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=whichpath><option value=0>No dietary restrictions</option><option value=1>Boozetafarian</option><option value=2>Teetotaler</option><option value=3>Oxygenarian</option></select></td></tr>" );

		buffer.append( "<tr><td colspan=2>&nbsp;</td></tr><tr><td>&nbsp;</td><td>" );
		buffer.append( "<input class=button type=submit value=\"Resurrect\"> <input type=checkbox name=confirm> (confirm)</td></tr></table></center></form>" );

		// Finished with adding all the data in a more compact form.  Now, we
		// go ahead and add in all the missing data that players might want to
		// look at to see which class to go for next.

		buffer.append( "<center><div id=\"softskills\"><h2>Unpermed Softcore Skills</h2>" );
		createSkillTable( buffer, softSkills );
		buffer.append( "</div><div id=\"hardskills\" style=\"display:none\"><h2>Unpermed Hardcore Skills</h2>" );
		createSkillTable( buffer, hardSkills );
		buffer.append( "</div></center>" );

		buffer.append( suffix );
	}

	private static void createSkillTable( StringBuffer buffer, ArrayList skillList )
	{
		buffer.append( "<table width=\"80%\"><tr>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ffcccc\"><table><tr><th style=\"font-size: 120%; text-decoration: underline; text-align: left;\">Muscle Skills</th></tr><tr><td>" );
		listPermanentSkills( buffer, skillList, 1000 );
		listPermanentSkills( buffer, skillList, 2000 );
		buffer.append( "</td></tr></table></td>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ccccff\"><table><tr><th style=\"font-size: 120%; text-decoration: underline; text-align: left;\">Mysticality Skills</th></tr><tr><td>" );
		listPermanentSkills( buffer, skillList, 3000 );
		listPermanentSkills( buffer, skillList, 4000 );
		buffer.append( "</td></tr></table></td>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ccffcc\"><table><tr><th style=\"font-size: 120%; text-decoration: underline; text-align: left;\">Moxie Skills</th></tr><tr><td>" );
		listPermanentSkills( buffer, skillList, 5000 );
		listPermanentSkills( buffer, skillList, 6000 );
		buffer.append( "</td></tr></table></td>" );
		buffer.append( "</tr></table>" );
	}

	private static void listPermanentSkills( StringBuffer buffer, ArrayList skillList, int startingPoint )
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

			buffer.append( "<a onClick='javascript:window.open(\"desc_skill.php?whichskill=" );
			buffer.append( startingPoint + i );
			buffer.append( "\",\"\",\"height=350,width=300\");'>" );
			buffer.append( skillName );
			buffer.append( "</a>" );

			if ( alreadyPermed )
				buffer.append( "</s></font>" );

			buffer.append( "</nobr><br>" );
		}
	}

	private static void addFightModifiers( StringBuffer buffer )
	{
		// Now, remove the runaway button and place it inside of the user's
		// skill list, IF the preference is active.

		if ( StaticEntity.getBooleanProperty( "relayRemovesRunaway" ) && buffer.indexOf( "You attempt to run away" ) == -1 )
		{
			int startIndex = buffer.indexOf( "<form name=runaway" );
			int stopIndex = buffer.indexOf( "</form>", startIndex );
			int insertIndex = buffer.lastIndexOf( "</select>" );

			if ( startIndex != -1 && stopIndex != -1 && insertIndex != -1 )
			{
				buffer.delete( startIndex, stopIndex );

				StringBuffer runawayString = new StringBuffer( "<option value=\"runaway\">Run Away (0 " );
				if ( KoLCharacter.isMuscleClass() )
					runawayString.append( "Muscularity" );
				else if ( KoLCharacter.isMysticalityClass() )
					runawayString.append( "Mana" );
				else
					runawayString.append( "Mojo" );
				runawayString.append( " Points)</option>" );

				buffer.insert( insertIndex, runawayString.toString() );
			}
		}

		// If the person opts to add a plinking link, check to see if it's
		// a valid page to add plinking, and make sure the person hasn't
		// already started plinking.

		if ( StaticEntity.getBooleanProperty( "relayAddsCustomCombat" ) )
		{
			int firstFormIndex = buffer.indexOf( "</form>" ) + 7;
			if ( firstFormIndex > 6 )
			{
				buffer.insert( firstFormIndex,
					"<tr><td align=center><form action=fight.php method=\"GET\"><input type=hidden name=\"action\" value=\"script\"><input class=\"button\" type=\"submit\" value=\"Run Custom Combat Script\"></form></td></tr>" );
			}
		}

	}

	private static void addUseLinks( String location, StringBuffer buffer )
	{
		if ( buffer.indexOf( "You acquire" ) == -1 )
			return;

		String text = buffer.toString();
		buffer.setLength( 0 );

		Matcher useLinkMatcher = ACQUIRE_PATTERN.matcher( text );

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

			int itemID = TradeableItemDatabase.getItemID( itemName, itemCount );

			String useType = null;
			String useLocation = null;

			boolean addCreateLink = location != null && location.indexOf( "combine.php" ) == -1 && location.indexOf( "cocktail.php" ) == -1 &&
				location.indexOf( "cook.php" ) == -1 && location.indexOf( "paster" ) == -1 && location.indexOf( "smith" ) == -1;

			AdventureResult creation = null;
			ItemCreationRequest irequest = null;

			int mixingMethod = ItemCreationRequest.NOCREATE;
			SortedListModel creations = ConcoctionsDatabase.getKnownUses( itemID );

			// If you find goat cheese, let the trapper link handle it.
			// Ore is skipped for now, so no need to check for it.  And,
			// finally, enchanted beans are primarily use.

			addCreateLink &= !creations.isEmpty() && itemID != 322;
			addCreateLink &= itemID != KoLAdventure.BEAN.getItemID() || KoLCharacter.hasItem( KoLAdventure.SOCK ) || KoLCharacter.hasItem( KoLAdventure.ROWBOAT );

			if ( addCreateLink )
			{
				addCreateLink = false;

				for ( int i = 0; !addCreateLink && i < creations.size(); ++i )
				{
					creation = (AdventureResult) creations.get(i);
					mixingMethod = ConcoctionsDatabase.getMixingMethod( creation.getItemID() );

					// Only accept if it's a creation method that the editor kit
					// currently understands and links.

					switch ( mixingMethod )
					{
					case ItemCreationRequest.NOCREATE:
					case ItemCreationRequest.PIXEL:
					case ItemCreationRequest.ROLLING_PIN:
					case ItemCreationRequest.TOY:
					case ItemCreationRequest.CLOVER:
					case ItemCreationRequest.STILL_BOOZE:
					case ItemCreationRequest.STILL_MIXER:
					case ItemCreationRequest.SMITH:
					case ItemCreationRequest.SMITH_WEAPON:
					case ItemCreationRequest.SMITH_ARMOR:
					case ItemCreationRequest.CATALYST:
						continue;
					}

					irequest = ItemCreationRequest.getInstance( creation.getItemID() );
					addCreateLink = irequest != null && irequest.getQuantityPossible() > 0;
				}
			}

			// If you can add a creation link, then add one instead.
			// That way, the player can click and KoLmafia will save
			// the player a click or two (well, if they trust it).

			if ( addCreateLink )
			{
				switch ( mixingMethod )
				{
				case ItemCreationRequest.STARCHART:
					useType = "chart";
					useLocation = "starchart.php";
					break;

				case ItemCreationRequest.COMBINE:
					useType = "combine";
					useLocation = KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "combine.php";
					break;

				case ItemCreationRequest.MIX:
				case ItemCreationRequest.MIX_SPECIAL:
				case ItemCreationRequest.MIX_SUPER:
					useType = "mix";
					useLocation = "cocktail.php";
					break;

				case ItemCreationRequest.COOK:
				case ItemCreationRequest.COOK_REAGENT:
				case ItemCreationRequest.SUPER_REAGENT:
				case ItemCreationRequest.COOK_PASTA:
					useType = "cook";
					useLocation = "cook.php";
					break;

				case ItemCreationRequest.JEWELRY:
					useType = "jewelry";
					useLocation = "jewelry.php";
					break;
				}
			}
			else
			{
				switch ( TradeableItemDatabase.getConsumptionType( itemID ) )
				{
				case ConsumeItemRequest.CONSUME_EAT:

					if ( itemID == 322 )
					{
						AdventureResult cheese = new AdventureResult( itemID, 1 );
						useType = String.valueOf( cheese.getCount( inventory ) );
						useLocation = "trapper.php";
					}
					else
					{
						useType = KoLCharacter.canEat() ? "eat" : null;
						useLocation = "inv_eat.php?pwd=&which=1&whichitem=";
					}

					break;

				case ConsumeItemRequest.CONSUME_DRINK:
					useType = KoLCharacter.canDrink() ? "drink" : null;
					useLocation = "inv_booze.php?pwd=&which=1&whichitem=";
					break;

				case ConsumeItemRequest.CONSUME_MULTIPLE:
					useType = "use";
					useLocation = itemCount != 1 ? "multiuse.php?passitem=" : "inv_use.php?pwd=&which=1&whichitem=";
					break;

				case ConsumeItemRequest.CONSUME_RESTORE:
					useType = "skills";
					useLocation = "skills.php";
					break;

				case ConsumeItemRequest.CONSUME_USE:

					useType = "use";
					useLocation = itemID == UneffectRequest.REMEDY.getItemID() ? "uneffect.php" :
						"inv_use.php?pwd=&which=3&whichitem=";

					break;

				case ConsumeItemRequest.EQUIP_HAT:
				case ConsumeItemRequest.EQUIP_PANTS:
				case ConsumeItemRequest.EQUIP_SHIRT:
				case ConsumeItemRequest.EQUIP_OFFHAND:
				case ConsumeItemRequest.EQUIP_ACCESSORY:
				case ConsumeItemRequest.EQUIP_FAMILIAR:

					useType = null;
					int outfit = EquipmentDatabase.getOutfitWithItem( itemID );

					if ( outfit != -1 )
					{
						if ( EquipmentDatabase.hasOutfit( outfit ) )
						{
							useType = "outfit";
							useLocation = "inv_equip.php?action=outfit&which=2&whichoutfit=" + outfit;
						}
					}

					if ( useType == null )
					{
						useType = "equip";
						useLocation = "inv_equip.php?pwd=&which=2&action=equip&whichitem=";
					}

					break;

				default:

					if ( itemID == SorceressLair.HEDGE_KEY.getItemID() || itemID == SorceressLair.PUZZLE_PIECE.getItemID() )
					{
						useType = "maze";
						useLocation = "hedgepuzzle.php";
					}
					else if ( (itemID == 363 || itemID == 364 || itemID == 365) )
					{
						AdventureResult ore = new AdventureResult( StaticEntity.getProperty( "trapperOre" ), itemCount, false );

						if ( ore.getItemID() == itemID )
						{
							useType = String.valueOf( ore.getCount( inventory ) );
							useLocation = "trapper.php";
						}
					}
					else if ( itemID == 459 || itemID == 461 || itemID == 462 || itemID == 463 )
					{
						AdventureResult white = new AdventureResult( 459, 1 );
						useType = String.valueOf( white.getCount( inventory ) + ItemCreationRequest.getInstance( 459 ).getQuantityPossible() ) + " white";
						useLocation = "town_wrong.php?place=crackpot";
					}
				}
			}

			if ( useType != null && useLocation != null )
			{
				useLinkMatcher.appendReplacement( buffer,
					"You acquire$1 <font size=1>[<a href=\"" + useLocation.toString() +
					(useLocation.endsWith( "=" ) ? String.valueOf( itemID ) : "") +
					"\">" + useType + "</a>]</font></td>" );
			}
			else
			{
				useLinkMatcher.appendReplacement( buffer, "$0" );
			}
		}

		useLinkMatcher.appendTail( buffer );
	}

	private static void addChoiceSpoilers( StringBuffer buffer )
	{
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
				String itemID = possibleDecisions[3][i];

				// If this decision leads to an item...
				if ( itemID != null )
				{
					// List # in inventory
					buffer.append( " - " );
					AdventureResult result = new AdventureResult( StaticEntity.parseInt( itemID ), 1 );

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

	private static void addTavernSpoilers( StringBuffer buffer )
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

	private static void addRestoreLinks( StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String fontTag = "";

		int startingIndex = 0;
		int lastAppendIndex = 0;

		// First, locate your HP information inside of the response
		// text and replace it with a restore HP link.

		float threshold = StaticEntity.getFloatProperty( "hpAutoRecoveryTarget" ) * ((float) KoLCharacter.getMaximumHP());
		float dangerous = StaticEntity.getFloatProperty( "hpAutoRecovery" ) * ((float) KoLCharacter.getMaximumHP());

		if ( KoLCharacter.getCurrentHP() < threshold )
		{
			if ( KoLRequest.isCompactMode )
			{
				startingIndex = text.indexOf( "<td align=right>HP:", startingIndex );
				startingIndex = text.indexOf( "<b>", startingIndex ) + 3;

				fontTag = text.substring( startingIndex, text.indexOf( ">", startingIndex ) + 1 );
				if ( KoLCharacter.getCurrentHP() < dangerous )
					fontTag = "<font color=red>";
			}
			else
			{
				startingIndex = text.indexOf( "doc(\"hp\")", startingIndex );
				startingIndex = text.indexOf( "<br>", startingIndex ) + 4;

				fontTag = text.substring( startingIndex, text.indexOf( ">", startingIndex ) + 1 );
				if ( KoLCharacter.getCurrentHP() < dangerous )
					fontTag = "<span class=red>";
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			startingIndex = text.indexOf( ">", startingIndex ) + 1;
			lastAppendIndex = startingIndex;

			startingIndex = text.indexOf( KoLRequest.isCompactMode ? "/" : "&", startingIndex );

			if ( !KoLRequest.isCompactMode )
				buffer.append( fontTag );

			buffer.append( "<a title=\"Restore your HP\" href=\"/KoLmafia/sideCommand?cmd=restore+hp\" style=\"color:" );

			Matcher colorMatcher = COLOR_PATTERN.matcher( fontTag );
			if ( colorMatcher.find() )
				buffer.append( colorMatcher.group(2) + "\">" );
			else
				buffer.append( "black\"><b>" );

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "</a>" );
			if ( !KoLRequest.isCompactMode )
				buffer.append( "</span>" );

			buffer.append( fontTag );
		}

		// Next, locate your MP information inside of the response
		// text and replace it with a restore MP link.

		threshold = StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" ) * ((float) KoLCharacter.getMaximumMP());
		dangerous = StaticEntity.getFloatProperty( "mpAutoRecovery" ) * ((float) KoLCharacter.getMaximumMP());

		if ( KoLCharacter.getCurrentMP() < threshold )
		{
			if ( KoLRequest.isCompactMode )
			{
				startingIndex = text.indexOf( "<td align=right>MP:", startingIndex );
				startingIndex = text.indexOf( "<b>", startingIndex ) + 3;
			}
			else
			{

				startingIndex = text.indexOf( "doc(\"mp\")", startingIndex );
				startingIndex = text.indexOf( "<br>", startingIndex ) + 4;
				startingIndex = text.indexOf( ">", startingIndex ) + 1;
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "<a style=\"color:" );
			buffer.append( KoLCharacter.getCurrentMP() < dangerous ? "red" : "black" );
			buffer.append( "\" title=\"Restore your MP\" href=\"/KoLmafia/sideCommand?cmd=restore+mp\">" );
			startingIndex = KoLRequest.isCompactMode ? text.indexOf( "/", startingIndex ) : text.indexOf( "&", startingIndex );
			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "</a>" );
		}

		buffer.append( text.substring( lastAppendIndex ) );
	}


	private static void addUpArrowLinks( StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String fontTag = "";

		int startingIndex = 0;
		int lastAppendIndex = 0;

		// First, add in a mood-execute link, in the event that the person
		// has a non-empty list of triggers.

		String fontColor = MoodSettings.willExecute( true ) ? "black" : "gray";

		if ( MoodSettings.getTriggers().isEmpty() )
		{
		}
		else if ( KoLRequest.isCompactMode )
		{
			int effectIndex = text.indexOf( "eff(", startingIndex );
			boolean shouldAddDivider = effectIndex == -1;

			if ( shouldAddDivider )
				startingIndex = text.lastIndexOf( "</table>" ) + 8;
			else
				startingIndex = text.lastIndexOf( "<table", effectIndex );

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( shouldAddDivider )
				buffer.append( "<hr width=50%>" );

			buffer.append( "<font size=2 color=" );
			buffer.append( fontColor );
			buffer.append( "><a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=mood+execute\" style=\"color:" );
			buffer.append( fontColor );
			buffer.append( "\"><img src=\"/images/buff.gif\"></a></font><br><br>" );
		}
		else
		{
			int effectIndex = text.indexOf( "Effects:</font></b>", startingIndex );
			if ( effectIndex != -1 )
			{
				startingIndex = text.indexOf( "<br>", effectIndex );
			}
			else
			{
				startingIndex = text.lastIndexOf( "<table" );
				if ( startingIndex < text.lastIndexOf( "target=mainpane" ) )
					startingIndex = text.lastIndexOf( "</center>" );
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( effectIndex == -1 )
				buffer.append( "<center><p><b><font size=2>Effects:</font></b>" );

			buffer.append( "<br><font size=2 color=" );
			buffer.append( fontColor );
			buffer.append( ">[<a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=mood+execute\" style=\"color:" );
			buffer.append( fontColor );
			buffer.append( "\">mood " +
				StaticEntity.getProperty( "currentMood" ) + "</a>]</font>" );

			if ( effectIndex == -1 )
				buffer.append( "<br></p></center>" );
		}


		// Finally, replace all of the shrug off links associated with
		// this response text.

		while ( startingIndex != -1 )
		{
			startingIndex = text.indexOf( "onClick='eff", lastAppendIndex + 1 );
			if ( startingIndex != -1 )
			{
				int nextAppendIndex = text.indexOf( "(", startingIndex ) + 1;
				buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
				lastAppendIndex = nextAppendIndex;

				int effectID = StaticEntity.parseInt(
					text.substring( nextAppendIndex, text.indexOf( ")", nextAppendIndex ) ) );

				// If the player is in compact mode, then if they wish to textualize
				// their effects, go ahead and do so.


				if ( KoLRequest.isCompactMode )
				{
					if ( StaticEntity.getBooleanProperty( "relayTextualizesEffects" ) )
					{
						nextAppendIndex = text.indexOf( "></td>", startingIndex );
						buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
						lastAppendIndex = nextAppendIndex + 6;

						int deleteIndex = buffer.lastIndexOf( "<img" );
						buffer.delete( deleteIndex, buffer.length() );

						buffer.append( StatusEffectDatabase.getShortName( effectID ) );
						buffer.append( "</td>" );

					}

					nextAppendIndex = text.indexOf( "<td>(", startingIndex ) + 5;
				}
				else
					nextAppendIndex = text.indexOf( "(", text.indexOf( "<font size=2>", startingIndex ) ) + 1;

				buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
				lastAppendIndex = nextAppendIndex;

				String effectName = StatusEffectDatabase.getEffectName( effectID );

				if ( effectName == null )
					continue;

				String upkeepAction = MoodSettings.getDefaultAction( "lose_effect", effectName );
				String removeAction = MoodSettings.getDefaultAction( "gain_effect", effectName );

				if ( upkeepAction.endsWith( "snowcone" ) || upkeepAction.endsWith( "mushroom" ) || upkeepAction.endsWith( "cupcake" ) )
					upkeepAction = "";

				String skillName = UneffectRequest.effectToSkill( effectName );
				int skillType = ClassSkillsDatabase.getSkillType( ClassSkillsDatabase.getSkillID( skillName ) );

				// Add a removal link to the duration for buffs which can
				// be removed.  This is either when the buff can be shrugged
				// or the buff has a default removal method.

				if ( skillType == ClassSkillsDatabase.BUFF || KoLCharacter.hasItem( UneffectRequest.REMEDY ) )
					removeAction = "uneffect " + effectName;

				if ( !removeAction.equals( "" ) )
				{
					buffer.append( "<a href=\"/KoLmafia/sideCommand?cmd=" );

					try
					{
						buffer.append( URLEncoder.encode( removeAction, "UTF-8" ) );
					}
					catch ( Exception e )
					{
						// Hm, something bad happened.  Instead of giving a real link,
						// give a fake link instead.

						buffer.append( "win+game" );
					}

					buffer.append( "\" title=\"" );

					if ( skillType == ClassSkillsDatabase.BUFF )
						buffer.append( "Shrug off the " );
					else if ( removeAction.startsWith( "uneffect" ) )
						buffer.append( "Use a remedy to remove the " );
					else
						buffer.append( Character.toUpperCase( removeAction.charAt(0) ) + removeAction.substring(1) + " to remove the " );

					buffer.append( effectName );
					buffer.append( " effect\"" );

					if ( effectName.equals( "Poisoned" ) || effectName.equals( "Beaten Up" ) )
						buffer.append( " style=\"color:red\"" );

					buffer.append( ">" );
				}

				nextAppendIndex = text.indexOf( ")", lastAppendIndex ) + 1;
				int duration = StaticEntity.parseInt( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );

				buffer.append( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );
				lastAppendIndex = nextAppendIndex;

				if ( skillType == ClassSkillsDatabase.BUFF || !removeAction.equals( "" ) )
					buffer.append( "</a>" );

				buffer.append( ")" );

				// Add the up-arrow icon for buffs which can be maintained, based
				// on information known to the mood maintenance module.

				if ( !upkeepAction.equals( "" ) )
				{
					buffer.append( "&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=" );

					try
					{
						buffer.append( URLEncoder.encode( upkeepAction, "UTF-8" ) );
					}
					catch ( Exception e )
					{
						// Hm, something bad happened.  Instead of giving a real link,
						// give a fake link instead.

						buffer.append( "win+game" );
					}

					buffer.append( "\" title=\"Increase rounds of " );
					buffer.append( effectName );
					buffer.append( "\"><img src=\"/images/" );

					if ( duration <= 5 )
						buffer.append( "red" );

					buffer.append( "up.gif\" border=0></a>" );
				}
			}
		}

		buffer.append( text.substring( lastAppendIndex ) );
	}

	private static String sortItemList( String select, String displayHTML )
	{
		Matcher selectMatcher = Pattern.compile( "<select name=" + select + ">.*?</select>" ).matcher( displayHTML );
		if ( selectMatcher.find() )
		{
			ArrayList items = new ArrayList();
			int selectedItem = -1;
			Matcher itemMatcher = OPTION_PATTERN.matcher( selectMatcher.group() );

			while ( itemMatcher.find() )
			{
				int id = StaticEntity.parseInt( itemMatcher.group(1) );
				if ( id == 0 )
					continue;

				int count = StaticEntity.parseInt( itemMatcher.group(2) );
				AdventureResult currentItem = new AdventureResult( id, count );
				if ( itemMatcher.group().indexOf( "selected" ) != -1 )
					selectedItem = id;

				items.add( currentItem );
			}

			Collections.sort( items );
			AdventureResult [] itemArray = new AdventureResult[ items.size() ];
			items.toArray( itemArray );

			StringBuffer itemString = new StringBuffer();
			itemString.append( "<select name=whichitem><option value=0>(select an item)</option>" );

			for ( int i = 0; i < itemArray.length; ++i )
			{
				itemString.append( "<option value=" );
				itemString.append( itemArray[i].getItemID() );

				if ( itemArray[i].getItemID() == selectedItem )
					itemString.append( " selected" );

				itemString.append( ">" );
				itemString.append( itemArray[i].toString() );
				itemString.append( "</option>" );
			}

			itemString.append( "</select>" );
			displayHTML = displayHTML.replaceFirst( "<select name=whichitem>.*?</select>", itemString.toString() );
		}
		return displayHTML;
	}

	private static class KoLSubmitView extends FormView
	{
		public KoLSubmitView( Element elem )
		{	super( elem );
		}

		protected Component createComponent()
		{
			Component c = super.createComponent();

			if ( c != null && (c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox) )
				c.setBackground( Color.white );

			return c;
		}

		protected void submitData( String data )
		{
			// Get the element

			Element inputElement = getElement();

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

			RequestFrame frame = findFrame( value );

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

			// Now, prepare the request string that will
			// be posted to KoL.

			KoLRequest request;

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
					request = new KoLRequest( URLDecoder.decode( actionString.toString(), "UTF-8" ), true );
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
					request = new KoLRequest( actionString.toString(), true );
				}
			}
			else
			{
				// For normal URLs, the form data can be submitted
				// just like in every other request.

				request = new KoLRequest( action, true );
				if ( elements[0].length() > 0 )
					for ( int i = 0; i < elements.length; ++i )
						if ( elements[i] != null )
							request.addEncodedFormField( elements[i] );
			}

			frame.refresh( request );
		}

		private RequestFrame findFrame( String value )
		{
			Object [] frames = existingFrames.toArray();
			String search = "value=\"" + value + "\"";

			for ( int i = 0; i < frames.length; ++i )
			{
				if ( !( frames[i] instanceof RequestFrame ) )
					continue;

				RequestFrame frame = (RequestFrame)frames[i];
				if  ( frame.mainDisplay.getText().indexOf( search ) != -1 )
				      return frame;
			}

			return null;
		}
	}

	/**
	 * Utility method used to determine the KoLRequest that
	 * should be sent, given the appropriate location.
	 */

	protected static KoLRequest extractRequest( String location )
	{
		if ( location.indexOf( "pics.communityofloathing.com" ) != -1 )
		{
			downloadImage( location );
			location = location.substring( location.indexOf( "/" ) );

			KoLRequest request = new KoLRequest( location );
			request.responseCode = 200;
			request.responseText = "<html><img src=\"" + location + "\"></html>";
			request.responseText = request.responseText;
			return request;
		}

		String [] urlData = location.split( "\\?" );
		String [] formData = urlData.length == 1 ? new String[0] : urlData[1].split( "&" );

		String [] currentField;
		KoLRequest request = null;

		if ( location.startsWith( "campground.php" ) )
			request = new CampgroundRequest();
		else
			request = new KoLRequest( urlData[0], true );

		for ( int i = 0; i < formData.length; ++i )
		{
			currentField = formData[i].split( "=" );

			if ( currentField.length == 2 )
				request.addFormField( currentField[0], currentField[1] );
			else
				request.addFormField( formData[i] );
		}

		return request;
	}
}
