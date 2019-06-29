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

    public static final SpecialEntities INSTANCE = new SpecialEntities(true, true) {
        @Override
        public void put(SpecialEntity specialEntity) {
            throw new UnsupportedOperationException("cannot add to this instance");
        }
    };

    /**
     * key is the {@link SpecialEntity#getKey()} ( i.e. "quot" )
     */
	private Map<String, SpecialEntity> entities = new HashMap<String, SpecialEntity>();
	/**
	 * Key is the Integer returned by {@link SpecialEntity#intValue()}
	 */
	private Map<Integer, SpecialEntity> entitiesByUnicodeCharcode = new HashMap<Integer, SpecialEntity>();
	private boolean greek;
	private boolean math;
	private int maxEntityLength;
	public static final char NON_BREAKABLE_SPACE = 160;

	public SpecialEntities(boolean greek, boolean math) {
	    this.greek = greek;
	    this.math = math;
	    _put(new SpecialEntity("null",	0, "", true));
		_put(new SpecialEntity("nbsp",	NON_BREAKABLE_SPACE, null, true));
		_put(new SpecialEntity("iexcl",	161, null, true));
		_put(new SpecialEntity("cent",	162, null, true));
		_put(new SpecialEntity("pound",	163, null, true));
		_put(new SpecialEntity("curren",	164, null, true));
		_put(new SpecialEntity("yen",		165, null, true));
		_put(new SpecialEntity("brvbar",	166, null, true));
		_put(new SpecialEntity("sect",	167, null, true));
		_put(new SpecialEntity("uml",		168, null, true));
		_put(new SpecialEntity("copy",	169, null, true));
		_put(new SpecialEntity("ordf",	170, null, true));
		_put(new SpecialEntity("laquo",	171, null, true));
		_put(new SpecialEntity("not",		172, null, true));
		_put(new SpecialEntity("shy",		173, null, true));
		_put(new SpecialEntity("reg",		174, null, true));
		_put(new SpecialEntity("macr",	175, null, true));
		_put(new SpecialEntity("deg",		176, null, true));
		_put(new SpecialEntity("plusmn",	177, null, true));
		_put(new SpecialEntity("sup2",	178, null, true));
		_put(new SpecialEntity("sup3",	179, null, true));
		_put(new SpecialEntity("acute",	180, null, true));
		_put(new SpecialEntity("micro",	181, null, true));
		_put(new SpecialEntity("para",	182, null, true));
		_put(new SpecialEntity("middot",	183, null, true));
		_put(new SpecialEntity("cedil",	184, null, true));
		_put(new SpecialEntity("sup1",	185, null, true));
		_put(new SpecialEntity("ordm",	186, null, true));
		_put(new SpecialEntity("raquo",	187, null, true));
		_put(new SpecialEntity("frac14",	188, null, true));
		_put(new SpecialEntity("frac12",	189, null, true));
		_put(new SpecialEntity("frac34",	190, null, true));
		_put(new SpecialEntity("iquest",	191, null, true));
		_put(new SpecialEntity("Agrave",	192, null, true));
		_put(new SpecialEntity("Aacute",	193, null, true));
		_put(new SpecialEntity("Acirc",	194, null, true));
		_put(new SpecialEntity("Atilde",	195, null, true));

		_put(new SpecialEntity("Auml",	196, null, true));
		_put(new SpecialEntity("Aring",	197, null, true));
		_put(new SpecialEntity("AElig",	198, null, true));
		_put(new SpecialEntity("Ccedil",	199, null, true));
		_put(new SpecialEntity("Egrave",	200, null, true));
		_put(new SpecialEntity("Eacute",	201, null, true));
		_put(new SpecialEntity("Ecirc",	202, null, true));
		_put(new SpecialEntity("Euml",	203, null, true));
		_put(new SpecialEntity("Igrave",	204, null, true));
		_put(new SpecialEntity("Iacute",	205, null, true));
		_put(new SpecialEntity("Icirc",	206, null, true));
		_put(new SpecialEntity("Iuml",	207, null, true));
		_put(new SpecialEntity("ETH",		208, null, true));
		_put(new SpecialEntity("Ntilde",	209, null, true));
		_put(new SpecialEntity("Ograve",	210, null, true));
		_put(new SpecialEntity("Oacute",	211, null, true));
		_put(new SpecialEntity("Ocirc",	212, null, true));
		_put(new SpecialEntity("Otilde",	213, null, true));
		_put(new SpecialEntity("Ouml",	214, null, true));
		_put(new SpecialEntity("times",	215, null, true));
		_put(new SpecialEntity("Oslash",	216, null, true));
		_put(new SpecialEntity("Ugrave",	217, null, true));
		_put(new SpecialEntity("Uacute",	218, null, true));
		_put(new SpecialEntity("Ucirc",	219, null, true));
		_put(new SpecialEntity("Uuml",	220, null, true));
		_put(new SpecialEntity("Yacute",	221, null, true));
		_put(new SpecialEntity("THORN",	222, null, true));
		_put(new SpecialEntity("szlig",	223, null, true));
		_put(new SpecialEntity("agrave",	224, null, true));
		_put(new SpecialEntity("aacute",	225, null, true));
		_put(new SpecialEntity("acirc",	226, null, true));
		_put(new SpecialEntity("atilde",	227, null, true));
		_put(new SpecialEntity("auml",	228, null, true));
		_put(new SpecialEntity("aring",	229, null, true));
		_put(new SpecialEntity("aelig",	230, null, true));
		_put(new SpecialEntity("ccedil",	231, null, true));
		_put(new SpecialEntity("egrave",	232, null, true));
		_put(new SpecialEntity("eacute",	233, null, true));
		_put(new SpecialEntity("ecirc",	234, null, true));
		_put(new SpecialEntity("euml",	235, null, true));
		_put(new SpecialEntity("igrave",	236, null, true));
		_put(new SpecialEntity("iacute",	237, null, true));
		_put(new SpecialEntity("icirc",	238, null, true));
		_put(new SpecialEntity("iuml",	239, null, true));
		_put(new SpecialEntity("eth",		240, null, true));
		_put(new SpecialEntity("ntilde",	241, null, true));
		_put(new SpecialEntity("ograve",	242, null, true));
		_put(new SpecialEntity("oacute",	243, null, true));
		_put(new SpecialEntity("ocirc",	244, null, true));
		_put(new SpecialEntity("otilde",	245, null, true));
		_put(new SpecialEntity("ouml",	246, null, true));
		_put(new SpecialEntity("divide",	247, null, true));
		_put(new SpecialEntity("oslash",	248, null, true));
		_put(new SpecialEntity("ugrave",	249, null, true));
		_put(new SpecialEntity("uacute",	250, null, true));
		_put(new SpecialEntity("ucirc",	251, null, true));
		_put(new SpecialEntity("uuml",	252, null, true));
		_put(new SpecialEntity("yacute",	253, null, true));
		_put(new SpecialEntity("thorn",	254, null, true));
		_put(new SpecialEntity("yuml",	255, null, true));

		_put(new SpecialEntity("OElig",	338, null, true));
		_put(new SpecialEntity("oelig",	339, null, true));
		_put(new SpecialEntity("Scaron",	352, null, true));
		_put(new SpecialEntity("scaron",	353, null, true));
        _put(new SpecialEntity("Yuml",  376, null, true));
        _put(new SpecialEntity("fnof",  402, null, true));
		_put(new SpecialEntity("circ",	710, null, true));
		_put(new SpecialEntity("tilde",	732, null, true));
		if ( this.greek ) {
		    // 913    Alpha   Α   greek capital letter alpha
		    _put(new SpecialEntity("Alpha", 913, null, true));
		    // 914 Beta    Β   greek capital letter beta
		    _put(new SpecialEntity("Beta", 914, null, true));
		    // 915 Gamma   Γ   greek capital letter gamma
            _put(new SpecialEntity("Gamma", 915, null, true));
            // 916 Delta   Δ   greek capital letter delta
            _put(new SpecialEntity("Delta", 916, null, true));
            // 917 Epsilon Ε   greek capital letter epsilon
            _put(new SpecialEntity("Epsilon", 917, null, true));
            // 918 Zeta    Ζ   greek capital letter zeta
            _put(new SpecialEntity("Zeta", 918, null, true));
            // 919 Eta Η   greek capital letter eta
            _put(new SpecialEntity("Eta", 919, null, true));
            // 920 Theta   Θ   greek capital letter theta
            _put(new SpecialEntity("Theta", 920, null, true));
            // 921 Iota    Ι   greek capital letter iota
            _put(new SpecialEntity("Iota", 921, null, true));
            // 922 Kappa   Κ   greek capital letter kappa
            _put(new SpecialEntity("Kappa", 922, null, true));
            // 923 Lambda  Λ   greek capital letter lambda
            _put(new SpecialEntity("Lambda", 923, null, true));
            // 924 Mu  Μ   greek capital letter mu
            _put(new SpecialEntity("Mu", 924, null, true));
            // 925 Nu  Ν   greek capital letter nu
            _put(new SpecialEntity("Nu", 925, null, true));
            // 926 Xi  Ξ   greek capital letter xi
            _put(new SpecialEntity("Xi", 926, null, true));
            // 927 Omicron Ο   greek capital letter omicron
            _put(new SpecialEntity("Omicron", 927, null, true));
            // 928 Pi  Π   greek capital letter pi
            _put(new SpecialEntity("Pi", 928, null, true));
            // 929 Rho Ρ   greek capital letter rho
            _put(new SpecialEntity("Rho", 929, null, true));
            // there is no Sigmaf, and no U+03A2 character either
            // 931 Sigma   Σ   greek capital letter sigma
            _put(new SpecialEntity("Sigma", 931, null, true));
            // 932 Tau Τ   greek capital letter tau
            _put(new SpecialEntity("Tau", 932, null, true));
            // 933 Upsilon Υ   greek capital letter upsilon
            _put(new SpecialEntity("Upsilon", 933, null, true));
            // 934 Phi Φ   greek capital letter phi
            _put(new SpecialEntity("Phi", 934, null, true));
            // 935 Chi Χ   greek capital letter chi
            _put(new SpecialEntity("Chi", 935, null, true));
            // 936 Psi Ψ   greek capital letter psi
            _put(new SpecialEntity("Psi", 936, null, true));
            // 937 Omega   Ω   greek capital letter omega
            _put(new SpecialEntity("Omega", 937, null, true));
            // 945 alpha   α   greek small letter alpha
            _put(new SpecialEntity("alpha", 945, null, true));
            // 946 beta    β   greek small letter beta
            _put(new SpecialEntity("beta", 946, null, true));
            // 947 gamma   γ   greek small letter gamma
            _put(new SpecialEntity("gamma", 947, null, true));
            // 948 delta   δ   greek small letter delta
            _put(new SpecialEntity("delta", 948, null, true));
            // 949 epsilon ε   greek small letter epsilon
            _put(new SpecialEntity("epsilon", 949, null, true));
            // 950 zeta    ζ   greek small letter zeta
            _put(new SpecialEntity("zeta", 950, null, true));
            // 951 eta η   greek small letter eta
            _put(new SpecialEntity("eta", 951, null, true));
            // 952 theta   θ   greek small letter theta
            _put(new SpecialEntity("theta", 952, null, true));
            // 953 iota    ι   greek small letter iota
            _put(new SpecialEntity("iota", 953, null, true));
            // 954 kappa   κ   greek small letter kappa
            _put(new SpecialEntity("kappa", 954, null, true));
            // 955 lambda  λ   greek small letter lambda
            _put(new SpecialEntity("lambda", 955, null, true));
            // 956 mu  μ   greek small letter mu
            _put(new SpecialEntity("mu", 956, null, true));
            // 957 nu  ν   greek small letter nu
            _put(new SpecialEntity("nu", 957, null, true));
            // 958 xi  ξ   greek small letter xi
            _put(new SpecialEntity("xi", 958, null, true));
            // 959 omicron ο   greek small letter omicron
            _put(new SpecialEntity("omicron", 959, null, true));
            // 960 pi  π   greek small letter pi
            _put(new SpecialEntity("pi", 960, null, true));
            // 961 rho ρ   greek small letter rho
            _put(new SpecialEntity("rho", 961, null, true));
            // 962 sigmaf  ς   greek small letter final sigma
            _put(new SpecialEntity("sigmaf", 962, null, true));
            // 963 sigma   σ   greek small letter sigma
            _put(new SpecialEntity("sigma", 963, null, true));
            // 964 tau τ   greek small letter tau
            _put(new SpecialEntity("tau", 964, null, true));
            // 965 upsilon υ   greek small letter upsilon
            _put(new SpecialEntity("upsilon", 965, null, true));
            // 966 phi φ   greek small letter phi
            _put(new SpecialEntity("phi", 966, null, true));
            // 967 chi χ   greek small letter chi
            _put(new SpecialEntity("chi", 967, null, true));
            // 968 psi ψ   greek small letter psi
            _put(new SpecialEntity("psi", 968, null, true));
            // 969 omega   ω   greek small letter omega
            _put(new SpecialEntity("omega", 969, null, true));
            // 977 thetasym    ϑ   greek small letter theta symbol
            _put(new SpecialEntity("thetasym", 977, null, true));
            // 978 upsih   ϒ   greek upsilon with hook symbol
            _put(new SpecialEntity("upsih", 978, null, true));
            // 982 piv ϖ   greek pi symbol
            _put(new SpecialEntity("piv", 982, null, true));
		}
		_put(new SpecialEntity("ensp",	8194, null, true));
		_put(new SpecialEntity("emsp",	8195, null, true));
		_put(new SpecialEntity("thinsp",	8201, null, true));
		_put(new SpecialEntity("zwnj",	8204, null, true));
		_put(new SpecialEntity("zwj",		8205, null, true));
		_put(new SpecialEntity("lrm",		8206, null, true));
		_put(new SpecialEntity("rlm",		8207, null, true));
		_put(new SpecialEntity("ndash",	8211, null, true));
		_put(new SpecialEntity("mdash",	8212, null, true));
		_put(new SpecialEntity("lsquo",	8216, null, true));
		_put(new SpecialEntity("rsquo",	8217, null, true));
		_put(new SpecialEntity("sbquo",	8218, null, true));
		_put(new SpecialEntity("ldquo",	8220, null, true));
		_put(new SpecialEntity("rdquo",	8221, null, true));
		_put(new SpecialEntity("bdquo",	8222, null, true));
		_put(new SpecialEntity("dagger",	8224, null, true));
        _put(new SpecialEntity("Dagger",    8225, null, true));
        _put(new SpecialEntity("bull",    8226, null, true));
        // three ellipses
		_put(new SpecialEntity("hellip",	8230, null, true));
        _put(new SpecialEntity("permil",    8240, null, true));
        _put(new SpecialEntity("prime",    8242, null, true));
        _put(new SpecialEntity("Prime",    8243, null, true));
		_put(new SpecialEntity("lsaquo",	8249, null, true));
		_put(new SpecialEntity("rsaquo",	8250, null, true));
        _put(new SpecialEntity("oline",    8254, null, true));
        _put(new SpecialEntity("frasl",    8260, null, true));
        _put(new SpecialEntity("euro",  8364, null, true));
        _put(new SpecialEntity("image",  8465, null, true));
        _put(new SpecialEntity("weierp",  8472, null, true));
        _put(new SpecialEntity("real",  8476, null, true));
        _put(new SpecialEntity("trade", 8482, null, true));
        _put(new SpecialEntity("alefsym", 8501, null, true));
        _put(new SpecialEntity("larr", 8592, null, true));
        _put(new SpecialEntity("uarr", 8593, null, true));
        _put(new SpecialEntity("rarr", 8594, null, true));
        _put(new SpecialEntity("darr", 8595, null, true));
        _put(new SpecialEntity("harr", 8596, null, true));
        _put(new SpecialEntity("crarr", 8629, null, true));
        _put(new SpecialEntity("lArr", 8656, null, true));
        _put(new SpecialEntity("uArr", 8657, null, true));
        _put(new SpecialEntity("rArr", 8658, null, true));
        _put(new SpecialEntity("dArr", 8659, null, true));
        _put(new SpecialEntity("hArr", 8660, null, true));
        if (this.math) {
            // 8704 forall  ∀   for all
            _put(new SpecialEntity("forall", 8704, null, true));
            //8706    part    ∂   partial differential
            _put(new SpecialEntity("part", 8706, null, true));
            //8707    exist   ∃   there exists
            _put(new SpecialEntity("exist", 8707, null, true));
            //8709    empty   ∅   empty set = null set = diameter
            _put(new SpecialEntity("empty", 8709, null, true));
            //8711    nabla   ∇   nabla = backward difference
            _put(new SpecialEntity("nabla", 8711, null, true));
            //8712    isin    ∈   element of
            _put(new SpecialEntity("isin", 8712, null, true));
            //8713    notin   ∉   not an element of
            _put(new SpecialEntity("notin", 8713, null, true));
            //8715    ni  ∋   contains as member
            _put(new SpecialEntity("ni", 8715, null, true));
            //8719    prod    ∏   n-ary product = product sign
            //prod is NOT the same character as U+03A0 'greek capital letter pi' though the same glyph might be used for both
            _put(new SpecialEntity("prod", 8719, null, true));
            //8721    sum ∑   n-ary sumation
            //sum is NOT the same character as U+03A3 'greek capital letter sigma' though the same glyph might be used for both
            _put(new SpecialEntity("sum", 8721, null, true));
            //8722    minus   −   minus sign
            _put(new SpecialEntity("minus", 8722, null, true));
            //8727    lowast  ∗   asterisk operator
            _put(new SpecialEntity("lowast", 8727, null, true));
            //8730    radic   √   square root = radical sign
            _put(new SpecialEntity("radic", 8730, null, true));
            //8733    prop    ∝   proportional to
            _put(new SpecialEntity("prop", 8733, null, true));
            //8734    infin   ∞   infinity
            _put(new SpecialEntity("infin", 8734, null, true));
            //8736    ang ∠   angle
            _put(new SpecialEntity("ang", 8736, null, true));
            //8743    and ∧   logical and = wedge
            _put(new SpecialEntity("and", 8743, null, true));
            //8744    or  ∨   logical or = vee
            _put(new SpecialEntity("or", 8744, null, true));
            //8745    cap ∩   intersection = cap
            _put(new SpecialEntity("cap", 8745, null, true));
            //8746    cup ∪   union = cup
            _put(new SpecialEntity("cup", 8746, null, true));
            //8747    int ∫   integral
            _put(new SpecialEntity("int", 8747, null, true));
            //8756    there4  ∴   therefore
            _put(new SpecialEntity("there4", 8756, null, true));
            //8764    sim ∼   tilde operator = varies with = similar to
            //tilde operator is NOT the same character as the tilde, U+007E, although the same glyph might be used to represent both
            _put(new SpecialEntity("sim", 8764, null, true));
            //8773    cong    ≅   approximately equal to
            _put(new SpecialEntity("cong", 8773, null, true));
            //8776    asymp   ≈   almost equal to = asymptotic to
            _put(new SpecialEntity("asymp", 8776, null, true));
            //8800    ne  ≠   not equal to
            _put(new SpecialEntity("ne", 8800, null, true));
            //8801    equiv   ≡   identical to
            _put(new SpecialEntity("equiv", 8801, null, true));
            //8804    le  ≤   less-than or equal to
            _put(new SpecialEntity("le", 8804, null, true));
            //8805    ge  ≥   greater-than or equal to
            _put(new SpecialEntity("ge", 8805, null, true));
            //8834    sub ⊂   subset of
            _put(new SpecialEntity("sub", 8834, null, true));
            //8835    sup ⊃   superset of
            _put(new SpecialEntity("sup", 8835, null, true));
            //note that nsup, 'not a superset of, U+2283' is not covered by the Symbol font encoding and is not included. Should it be, for symmetry? It is in ISOamsn
            //8836    nsub    ⊄   not a subset of
            _put(new SpecialEntity("nsub", 8836, null, true));
            //8838    sube    ⊆   subset of or equal to
            _put(new SpecialEntity("sube", 8838, null, true));
            //8839    supe    ⊇   superset of or equal to
            _put(new SpecialEntity("supe", 8839, null, true));
            //8853    oplus   ⊕   circled plus = direct sum
            _put(new SpecialEntity("oplus", 8853, null, true));
            //8855    otimes  ⊗   circled times = vector product
            _put(new SpecialEntity("otimes", 8855, null, true));
            //8869    perp    ⊥   up tack = orthogonal to = perpendicular
            _put(new SpecialEntity("perp", 8869, null, true));
            //8901    sdot    ⋅   dot operator
            _put(new SpecialEntity("sdot", 8901, null, true));
            //dot operator is NOT the same character as U+00B7 middle dot
            //8968    lceil   ⌈   left ceiling = apl upstile
            _put(new SpecialEntity("lceil", 8968, null, true));
            //8969    rceil   ⌉   right ceiling
            _put(new SpecialEntity("rceil", 8969, null, true));
            //8970    lfloor  ⌊   left floor = apl downstile
            _put(new SpecialEntity("lfloor", 8970, null, true));
            //8971    rfloor  ⌋   right floor
            _put(new SpecialEntity("rfloor", 8971, null, true));
            //9001    lang    〈   left-pointing angle bracket = bra
            //lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation mark'
            _put(new SpecialEntity("lang", 9001, null, true));
            //9002    rang    〉   right-pointing angle bracket = ket
            //rang is NOT the same character as U+003E 'greater than' or U+203A 'single right-pointing angle quotation mark'
            _put(new SpecialEntity("rang", 9002, null, true));
            //9674    loz ◊   lozenge
            _put(new SpecialEntity("loz", 9674, null, true));
            //black here seems to mean filled as opposed to hollow
            //9824    spades  ♠   black spade suit
            _put(new SpecialEntity("spades", 9824, null, true));
            //9827    clubs   ♣   black club suit = shamrock
            _put(new SpecialEntity("clubs", 9827, null, true));
            //9829    hearts  ♥   black heart suit = valentine
            _put(new SpecialEntity("hearts", 9829, null, true));
            //9830    diams   ♦   black diamond suit
            _put(new SpecialEntity("diams", 9830, null, true));
        }
        _put(new SpecialEntity("amp",  '&', null, false));
        _put(new SpecialEntity("lt", '<', null, false));
        _put(new SpecialEntity("gt",  '>', null, false));
        _put(new SpecialEntity("quot",  '"', null, false));
        // this is xml only -- apos appearing in html needs to be converted to ' or maybe &#39; to be universally safe
        // may need to special case for html attributes that use ' as surrounding delimeter on attribute value (instead of " ) : <a href='javascript:foo("bar'")' >wierd link</a>
        _put(new SpecialEntity("apos",  '\'', "'", false));
	}

	/**
	 *
	 * @param seq may have a leading & and/or trailing ; ( those will be removed prior to comparision)
	 * @return {@link SpecialEntity} if found.
	 */
	public SpecialEntity getSpecialEntity(String seq) {
		if (seq.length() == 0) return null;
	    int startIndex = seq.charAt(0) == '&'?1:0;
        int semiIndex = seq.indexOf(';');
        String entity;
        if (semiIndex < 0) {
            entity = seq.substring(startIndex);
        } else {
            entity = seq.substring(startIndex, semiIndex);
        }
        SpecialEntity specialEntity = entities.get(entity);
	    return specialEntity;
	}

	public SpecialEntity getSpecialEntityByUnicode(int unicodeCharcode) {
	    return this.entitiesByUnicodeCharcode.get(unicodeCharcode);
	}

	public void put(SpecialEntity specialEntity) {
	    _put(specialEntity);
	}

	/**
     * @param specialEntity
     */
    private void _put(SpecialEntity specialEntity) {
        SpecialEntity old;
        old = entities.put(specialEntity.getKey(), specialEntity);
        if ( old != null ) {
            throw new HtmlCleanerException("replaced "+old+" with "+specialEntity);
        }
        old = entitiesByUnicodeCharcode.put(specialEntity.intValue(), specialEntity);
        if ( old != null ) {
            throw new HtmlCleanerException("replaced "+old+" with "+specialEntity);
        }
        this.maxEntityLength = Math.max(this.maxEntityLength,specialEntity.getKey().length());
    }
    public int getMaxEntityLength() {
        return maxEntityLength;
    }
}