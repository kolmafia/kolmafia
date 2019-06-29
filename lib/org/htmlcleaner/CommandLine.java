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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.htmlcleaner.audit.HtmlModificationListenerLogger;

/**
 * <p>Command line usage class.</p>
 */
public class CommandLine {

    private static final String OMITXMLDECL = "omitxmldecl";

    /**
     * If the specified argument name exists without a value, return true.
     * If it exists with a value, translate it as a boolean.
     * @param args the command line arguments
     * @param name the switch name
     * @return true, or false, depending on whether the switch has been specified
     */
    private static boolean getSwitchArgument(String[] args, String name){
    	boolean value = false;
    	for (String curr : args){
    		int eqIndex = curr.indexOf('=');
            if (eqIndex >= 0) {
                String argName = curr.substring(0, eqIndex).trim();
                String argValue = curr.substring(eqIndex+1).trim();
                if (argName.toLowerCase().startsWith(name.toLowerCase())) {
                    value = toBoolean(argValue);
                }
            } else {
            	value = true;
            }
    	}
    	return value;
    }
    
    private static String getArgValue(String[] args, String name, String defaultValue) {
        for (String curr : args) {
            int eqIndex = curr.indexOf('=');
            if (eqIndex >= 0) {
                String argName = curr.substring(0, eqIndex).trim();
                String argValue = curr.substring(eqIndex+1).trim();

                if (argName.toLowerCase().startsWith(name.toLowerCase())) {
                    return argValue;
                }
            }
        }

        return defaultValue;
    }

    private static boolean toBoolean(String s) {
        return s != null && ( "on".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) );
    }
    
    private final static String className = CommandLine.class.getName();
    private final static Logger logger = Logger.getLogger(className);

    public static void main(String[] args) throws IOException, XPatherException {
        String source = getArgValue(args, "src", "");
        Scanner scan = new Scanner(System.in);
        String s = "";
        
        if ( "".equals(source) ) {
        	 while (scan.hasNext()) {
                 s += scan.nextLine();
             }
             if (s.compareTo("") != 0) {
                 System.err.println("Output:");
             } else {
            System.err.println("Usage: java -jar htmlcleanerXX.jar src=<url | file> [htmlver=4] [incharset=<charset>] " +
                               "[dest=<file>] [outcharset=<charset>] [taginfofile=<file>] [options...]");
            System.err.println("Alternative: java -jar htmlcleanerXX.jar (reads the input from console)");
            System.err.println("");
            System.err.println("where options include:");
            System.err.println("    outputtype=simple* | compact | browser-compact | pretty");
            System.err.println("    advancedxmlescape=true* | false");
            System.err.println("    usecdata=true* | false");
            System.err.println("    usecdatafor=<string value> [script,style]");
            System.err.println("    specialentities=true* | false");
            System.err.println("    unicodechars=true* | false");
            System.err.println("    omitunknowntags=true | false*");
            System.err.println("    treatunknowntagsascontent=true | false*");
            System.err.println("    omitdeprtags=true | false*");
            System.err.println("    treatdeprtagsascontent=true | false*");
            System.err.println("    omitcomments=true | false*");
            System.err.println("    " +OMITXMLDECL +"=true* | false");
            System.err.println("    omitdoctypedecl=true* | false");
            System.err.println("    omithtmlenvelope=true | false*");
            System.err.println("    useemptyelementtags=true* | false");
            System.err.println("    allowmultiwordattributes=true* | false");
            System.err.println("    allowhtmlinsideattributes=true | false*");
            System.err.println("    ignoreqe=true | false*");
            System.err.println("    namespacesaware=true* | false");
            System.err.println("    hyphenreplacement=<string value> [=]");
            System.err.println("    prunetags=<string value> []");
            System.err.println("    booleanatts=self* | empty | true");
            System.err.println("    nodebyxpath=<xpath expression>");
            System.err.println("    allowinvalidxmlattributenames=true | false*");     
            System.err.println("    invalidxmlattributenameprefix=<string value> []");     
            System.err.println("    t:<sourcetagX>[=<desttag>[,<preserveatts>]]");
            System.err.println("    t:<sourcetagX>.<destattrY>[=<template>]");
            System.exit(1);
             }
        }

        String inCharset = getArgValue(args, "incharset", "");
        if ("".equals(inCharset)) {
            inCharset = CleanerProperties.DEFAULT_CHARSET;
        }

        String outCharset = getArgValue(args, "outcharset", "");
        if ("".equals(outCharset)) {
            outCharset = CleanerProperties.DEFAULT_CHARSET;
        }

        String htmlversion = getArgValue(args, "htmlver", "");
        String destination = getArgValue(args, "dest", "");
        String outputType = getArgValue(args, "outputtype", "");
        String advancedXmlEscape = getArgValue(args, "advancedxmlescape", "");
        String useCData = getArgValue(args, "usecdata", "");
        String useCDataFor = getArgValue(args, "usecdatafor", "");
        String translateSpecialEntities = getArgValue(args, "specialentities", "");
        String unicodeChars = getArgValue(args, "unicodechars", "");
        String omitUnknownTags = getArgValue(args, "omitunknowntags", "");
        String treatUnknownTagsAsContent = getArgValue(args, "treatunknowntagsascontent", "");
        String omitDeprecatedTags = getArgValue(args, "omitdeprtags", "");
        String treatDeprecatedTagsAsContent = getArgValue(args, "treatdeprtagsascontent", "");
        String omitComments = getArgValue(args, "omitcomments", "");
        String omitXmlDeclaration = getArgValue(args, OMITXMLDECL, "");
        String omitDoctypeDeclaration = getArgValue(args, "omitdoctypedecl", "");
        String omitHtmlEnvelope = getArgValue(args, "omithtmlenvelope", "");
        String useEmptyElementTags = getArgValue(args, "useemptyelementtags", "");
        String allowMultiWordAttributes = getArgValue(args, "allowmultiwordattributes", "");
        String allowHtmlInsideAttributes = getArgValue(args, "allowhtmlinsideattributes", "");
        String ignoreQuestAndExclam = getArgValue(args, "ignoreqe", "");
        String namespacesAware= getArgValue(args, "namespacesaware", "");
        String commentHyphen = getArgValue(args, "hyphenreplacement", "");
        String pruneTags = getArgValue(args, "prunetags", "");
        String booleanAtts = getArgValue(args, "booleanatts", "");
        String nodeByXPath = getArgValue(args, "nodebyxpath", "");
        
        String allowInvalidAttributeNames = getArgValue(args, "allowinvalidattributenames", "");
        String invalidXmlAttributeNamePrefix = getArgValue(args, "invalidxmlattributenameprefix", "");

        HtmlCleaner cleaner;
        	
        String tagInfoFile = getArgValue(args, "taginfofile", "");
        if ( !"".equals(tagInfoFile) ) {
            cleaner = new HtmlCleaner(new ConfigFileTagProvider(new File(tagInfoFile)));
        } else { //Set appropriate TagProvider
        	if (htmlversion.compareTo("4")==0)
        		cleaner = new HtmlCleaner(Html4TagProvider.INSTANCE);
        	else
        		cleaner = new HtmlCleaner(Html5TagProvider.INSTANCE);
        }

        final CleanerProperties props = cleaner.getProperties();

        //
        // If the user specifies "quiet" or "quiet=true" then we don't add a modification
        // listener
        //
        if (!getSwitchArgument(args, "quiet"))
            props.addHtmlModificationListener(new HtmlModificationListenerLogger(logger));

        if ( !"".equals(omitUnknownTags) ) {
            props.setOmitUnknownTags( toBoolean(omitUnknownTags) );
        }

        if ( !"".equals(treatUnknownTagsAsContent) ) {
            props.setTreatUnknownTagsAsContent( toBoolean(treatUnknownTagsAsContent) );
        }

        if ( !"".equals(omitDeprecatedTags) ) {
            props.setOmitDeprecatedTags( toBoolean(omitDeprecatedTags) );
        }

        if ( !"".equals(treatDeprecatedTagsAsContent) ) {
            props.setTreatDeprecatedTagsAsContent( toBoolean(treatDeprecatedTagsAsContent) );
        }

        if ( !"".equals(advancedXmlEscape) ) {
            props.setAdvancedXmlEscape( toBoolean(advancedXmlEscape) );
        }
        
        if ( !"".equals(useCData) && "".equals(useCDataFor) ) {
            props.setUseCdataForScriptAndStyle( toBoolean(useCData) );
        }
        
        if ( !"".equals(useCDataFor) ) {
        	props.setUseCdataFor( useCDataFor );
        }

        if ( !"".equals(translateSpecialEntities) ) {
            props.setTranslateSpecialEntities( toBoolean(translateSpecialEntities) );
        }

        if ( !"".equals(unicodeChars) ) {
            props.setRecognizeUnicodeChars( toBoolean(unicodeChars) );
        }

        if ( !"".equals(omitComments) ) {
            props.setOmitComments( toBoolean(omitComments) );
        }

        if ( !"".equals(omitXmlDeclaration) ) {
            props.setOmitXmlDeclaration( toBoolean(omitXmlDeclaration) );
        }

        if ( !"".equals(omitDoctypeDeclaration) ) {
        	props.setOmitDoctypeDeclaration( toBoolean(omitDoctypeDeclaration) );
        }

        if ( !"".equals(omitHtmlEnvelope) ) {
        	props.setOmitHtmlEnvelope( toBoolean(omitHtmlEnvelope) );
        }

        if ( !"".equals(useEmptyElementTags) ) {
        	props.setUseEmptyElementTags( toBoolean(useEmptyElementTags) );
        }

        if ( !"".equals(allowMultiWordAttributes) ) {
        	props.setAllowMultiWordAttributes( toBoolean(allowMultiWordAttributes) );
        }

        if ( !"".equals(allowHtmlInsideAttributes) ) {
        	props.setAllowHtmlInsideAttributes( toBoolean(allowHtmlInsideAttributes) );
        }

        if ( !"".equals(ignoreQuestAndExclam) ) {
        	props.setIgnoreQuestAndExclam( toBoolean(ignoreQuestAndExclam) );
        }

        if ( !"".equals(namespacesAware) ) {
        	props.setNamespacesAware( toBoolean(namespacesAware) );
        }

        if ( !"".equals(commentHyphen) ) {
            props.setHyphenReplacementInComment(commentHyphen);
        }

        if ( !"".equals(pruneTags) ) {
            props.setPruneTags(pruneTags);
        }

        if ( !"".equals(booleanAtts) ) {
            props.setBooleanAttributeValues(booleanAtts);
        }

        if ( !"".equals(allowInvalidAttributeNames) ) {
        	props.setAllowInvalidAttributeNames( toBoolean(allowInvalidAttributeNames) );
        }
        
        if ( !"".equals(invalidXmlAttributeNamePrefix) ) {
        	props.setInvalidXmlAttributeNamePrefix( invalidXmlAttributeNamePrefix );
        }
        
        	
        // collect transformation info
        Map transInfos = new TreeMap();
        for (String arg2 : args) {
            String arg = arg2;
            if (arg.startsWith("t:") && arg.length() > 2) {
                arg = arg.substring(2);
                int index = arg.indexOf('=');
                String key = index <= 0 ? arg : arg.substring(0, index);
                String value = index <= 0 ? null : arg.substring(index + 1);
                transInfos.put(key, value);
            }
        }
        cleaner.initCleanerTransformations(transInfos);

        long start = System.currentTimeMillis();

        TagNode node;

        String src = source.toLowerCase();
        
        if (src.startsWith("http://") || src.startsWith("https://")) {
            node = cleaner.clean(new URL(src), inCharset);
        } else if (!source.isEmpty()) {
            node = cleaner.clean(new File(source), inCharset);
        } else {
            node = cleaner.clean(s);
        }

        // if user specifies XPath expresssion to choose node for serialization, then
        // try to evaluate XPath and look for first TagNode instance in the resulting array
        if ( !"".equals(nodeByXPath) ) {
            final Object[] xpathResult = node.evaluateXPath(nodeByXPath);
            int i;
            for (i = 0; i < xpathResult.length; i++) {
                if ( xpathResult[i] instanceof TagNode ) {
                    node = (TagNode) xpathResult[i];
                    System.out.println("Node successfully found by XPath.");
                    break;
                }
            }
            if (i == xpathResult.length) {
                System.out.println("Node not found by XPath expression - whole html tree is going to be serialized!");
            }
        }

        OutputStream out;
        if ( destination == null || "".equals(destination.trim()) ) {
            out = System.out;
        } else {
            out = new FileOutputStream(destination);
        }
        
        	

        if ( "compact".equals(outputType) ) {
            new CompactXmlSerializer(props).writeToStream(node, out, outCharset);
        } else if ( "browser-compact".equals(outputType) ) {
            new BrowserCompactXmlSerializer(props).writeToStream(node, out, outCharset);
        } else if ( "pretty".equals(outputType) ) {
            new PrettyXmlSerializer(props).writeToStream(node, out, outCharset);
        } else if ( "htmlsimple".equals(outputType) ) {
            new SimpleHtmlSerializer(props).writeToStream(node, out, outCharset); 
        } else if ( "htmlpretty".equals(outputType) ) {
            new PrettyHtmlSerializer(props).writeToStream(node, out, outCharset);
        } else if ( "htmlcompact".equals(outputType) ) {
            new CompactHtmlSerializer(props).writeToStream(node, out, outCharset);
        } else {
            new SimpleXmlSerializer(props).writeToStream(node, out, outCharset);
        }
        
        if (!getSwitchArgument(args, "quiet")){
        	System.out.println("Finished successfully in " + (System.currentTimeMillis() - start)+ "ms." );
        }
    }

}