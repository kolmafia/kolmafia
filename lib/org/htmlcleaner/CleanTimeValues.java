/*  Copyright (c) 2006-2013, HtmlCleaner Team (Vladimir Nikic, Pat Moore, Scott Wilson)
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

*/
package org.htmlcleaner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.htmlcleaner.HtmlCleaner.NestingState;
import org.htmlcleaner.conditional.ITagNodeCondition;

/**
 * This class is for thread-safe handling of private instance variables from HtmlCleaner
 */
class CleanTimeValues {
	
    boolean _headOpened = false;
    boolean _bodyOpened = false;
    @SuppressWarnings("rawtypes")
	Set _headTags = new LinkedHashSet();
    @SuppressWarnings("rawtypes")
	Set allTags = new TreeSet();
    transient Stack<NestingState> nestingStates = new Stack<NestingState>();

    TagNode htmlNode;
    TagNode bodyNode;
    TagNode headNode;
    TagNode rootNode;

    Set<ITagNodeCondition> pruneTagSet = new HashSet<ITagNodeCondition>();
    Set<TagNode> pruneNodeSet = new HashSet<TagNode>();
    Set<ITagNodeCondition> allowTagSet;
    
    /**
     * A stack of namespaces for currently open tags. Every xmlns declaration
     * on a tag adds another namespace to the stack, which is removed when the
     * tag is closed. In this way you can keep track of what namespace a tag
     * belongs to.
     */
    transient Stack<String> namespace = new Stack<String>();
    
    /**
     * A map of all the namespace prefixes and URIs declared within the document.
     * We use this to check whether any prefixes remain undeclared.
     */ 
    transient HashMap<String, String> namespaceMap = new HashMap<String, String>();
}