/*  Copyright (c) 2006-2015, Philokypros Ioulianou
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

	You can contact Philokypros Ioulianou by sending e-mail to
	philokypro_s@hotmail.com. Please include the word "HtmlCleaner" in the
	subject line.
*/

package org.htmlcleaner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Html4TagProvider implements ITagInfoProvider {

	private static final String STRONG = "strong";
    private ConcurrentMap<String, TagInfo> tagInfoMap = new ConcurrentHashMap<String, TagInfo>();
    // singleton instance, used if no other TagInfoProvider is specified
    public final static Html4TagProvider INSTANCE= new Html4TagProvider();
    
    private static final String CLOSE_BEFORE_COPY_INSIDE_TAGS = "bdo,"+STRONG+",em,q,b,i,u,tt,sub,sup,big,small,strike,s,font";
    private static final String CLOSE_BEFORE_TAGS = "p,details,summary,menuitem,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml";
  
    /**
     * Phrasing tags are those that can make up paragraphs along with text to make Phrasing Content
     */
    private static final String PHRASING_TAGS = "a,abbr,area,b,bdi,bdo,br,button,canvas,cite,code,command,data,datalist,del,dfn,em,embed,i,iframe,img,input,ins,kbd,keygen,label,link,map,mark,math,meta,meter,noscript,object,output,progress,q,s,samp,script,select,small,span,strong,sub,sup,svg,template,text,textarea,time,u,var,wbr";
    
    public Html4TagProvider() {

    	TagInfo tagInfo=null;
    	basicElements(tagInfo);
    	formattingElements(tagInfo);
    	formElements(tagInfo);
    	imgElements(tagInfo);
    	listElements(tagInfo);
    	linkElements(tagInfo);
    	tableElements(tagInfo);
    	styleElements(tagInfo);
    	olderElements(tagInfo);
    	scriptElements(tagInfo);
    }
    
    public void basicElements(TagInfo tagInfo){
    
    	  tagInfo = new TagInfo("title",  ContentType.text, BelongsTo.HEAD, false, true, false, CloseTag.required, Display.none);
          this.put("title", tagInfo);

          tagInfo = new TagInfo("h1", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("h1", tagInfo);

          tagInfo = new TagInfo("h2", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("h2", tagInfo);

          tagInfo = new TagInfo("h3", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("h3", tagInfo);

          tagInfo = new TagInfo("h4", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("h4", tagInfo);

          tagInfo = new TagInfo("h5", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("h5", tagInfo);

          tagInfo = new TagInfo("h6", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("h6", tagInfo);
          
          // jericho parser requires <p></p>
          tagInfo = new TagInfo("p", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
          this.put("p", tagInfo);
          
          tagInfo = new TagInfo("br", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.none);
          this.put("br", tagInfo);

          tagInfo = new TagInfo("hr", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
          this.put("hr", tagInfo);
          
          tagInfo = new TagInfo("div", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
          tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
          tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
          this.put("div", tagInfo);
    }
    
    
    public void formattingElements(TagInfo tagInfo){
    
    	tagInfo = new TagInfo("abbr", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("abbr", tagInfo);

        tagInfo = new TagInfo("acronym", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("acronym", tagInfo);

        tagInfo = new TagInfo("address", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("address", tagInfo);

        tagInfo = new TagInfo("b", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("u,i,tt,sub,sup,big,small,strike,blink,s");
        this.put("b", tagInfo);
        
        tagInfo = new TagInfo("bdo", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("bdo", tagInfo);

        
        tagInfo = new TagInfo("blockquote", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("blockquote", tagInfo);

        tagInfo = new TagInfo("cite", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("cite", tagInfo);

        tagInfo = new TagInfo("q", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("q", tagInfo);

        tagInfo = new TagInfo("code", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("code", tagInfo);

        tagInfo = new TagInfo("ins", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.any);
        this.put("ins", tagInfo);


        tagInfo = new TagInfo("i", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,tt,sub,sup,big,small,strike,blink,s");
        this.put("i", tagInfo);

        tagInfo = new TagInfo("u", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,i,tt,sub,sup,big,small,strike,blink,s");
        this.put("u", tagInfo);

        tagInfo = new TagInfo("tt", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,sub,sup,big,small,strike,blink,s");
        this.put("tt", tagInfo);

        tagInfo = new TagInfo("sub", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sup,big,small,strike,blink,s");
        this.put("sub", tagInfo);

        tagInfo = new TagInfo("sup", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,big,small,strike,blink,s");
        this.put("sup", tagInfo);

        tagInfo = new TagInfo("big", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,small,strike,blink,s");
        this.put("big", tagInfo);

        tagInfo = new TagInfo("small", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,strike,blink,s");
        this.put("small", tagInfo);

        tagInfo = new TagInfo("strike", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,small,blink,s");
        this.put("strike", tagInfo);

        tagInfo = new TagInfo("blink", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,small,strike,s");
        this.put("blink", tagInfo);

        tagInfo = new TagInfo("marquee", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("marquee", tagInfo);

        tagInfo = new TagInfo("s", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,small,strike,blink");
        this.put("s", tagInfo);


        tagInfo = new TagInfo("font", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.inline);
        this.put("font", tagInfo);

        tagInfo = new TagInfo("basefont", ContentType.none, BelongsTo.BODY, true, false, false, CloseTag.forbidden, Display.none);
        this.put("basefont", tagInfo);

        tagInfo = new TagInfo("center", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("center", tagInfo);

        
        tagInfo = new TagInfo("del", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.any);
        this.put("del", tagInfo);

        tagInfo = new TagInfo("dfn", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("dfn", tagInfo);

        tagInfo = new TagInfo("kbd", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("kbd", tagInfo);

        tagInfo = new TagInfo("pre", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("pre", tagInfo);

        tagInfo = new TagInfo("samp", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("samp", tagInfo);
        
        tagInfo = new TagInfo(STRONG, ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put(STRONG, tagInfo);

        tagInfo = new TagInfo("em", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("em", tagInfo);

        tagInfo = new TagInfo("var", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("var", tagInfo);

        
        tagInfo = new TagInfo("wbr", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.none);
        this.put("wbr", tagInfo);
        
        
    }
    
    
    public void formElements(TagInfo tagInfo){
    	tagInfo = new TagInfo("form", ContentType.all, BelongsTo.BODY, false, false, true, CloseTag.required, Display.block);
        tagInfo.defineForbiddenTags("form");
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("option,optgroup,textarea,select,fieldset,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("form", tagInfo);

        tagInfo = new TagInfo("input", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.inline);
        tagInfo.defineCloseBeforeTags("select,optgroup,option");
        this.put("input", tagInfo);

        tagInfo = new TagInfo("textarea", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineCloseBeforeTags("select,optgroup,option");
        this.put("textarea", tagInfo);

        tagInfo = new TagInfo("select", ContentType.all, BelongsTo.BODY, false, false, true, CloseTag.required, Display.inline);
        tagInfo.defineAllowedChildrenTags("option,optgroup");
        tagInfo.defineCloseBeforeTags("option,optgroup,select");
        this.put("select", tagInfo);

        tagInfo = new TagInfo("option",  ContentType.text, BelongsTo.BODY, false, false, true, CloseTag.optional, Display.inline);
        tagInfo.defineFatalTags("select");
        tagInfo.defineCloseBeforeTags("option");
        this.put("option", tagInfo);

        tagInfo = new TagInfo("optgroup", ContentType.all, BelongsTo.BODY, false, false, true, CloseTag.required, Display.inline);
        tagInfo.defineFatalTags("select");
        tagInfo.defineAllowedChildrenTags("option");
        tagInfo.defineCloseBeforeTags("optgroup");
        this.put("optgroup", tagInfo);

        tagInfo = new TagInfo("button", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.any);
        tagInfo.defineCloseBeforeTags("select,optgroup,option");
        this.put("button", tagInfo);

        tagInfo = new TagInfo("label", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("label", tagInfo);
        
        tagInfo = new TagInfo("legend", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        //
        // If we include this rule, we get an out-of-memory error. See issue 129.
        //
        //tagInfo.defineRequiredEnclosingTags("fieldset");
        tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
        this.put("legend", tagInfo);

        tagInfo = new TagInfo("fieldset", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("fieldset", tagInfo);
    
    }
    
    
    public void listElements(TagInfo tagInfo){
    	
    	tagInfo = new TagInfo("ul", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("ul", tagInfo);

        tagInfo = new TagInfo("ol", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("ol", tagInfo);

        tagInfo = new TagInfo("li", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("li,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("li", tagInfo);

        tagInfo = new TagInfo("dl", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("dl", tagInfo);

        tagInfo = new TagInfo("dt", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineCloseBeforeTags("dt,dd");
        this.put("dt", tagInfo);

        tagInfo = new TagInfo("dd", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineCloseBeforeTags("dt,dd");
        this.put("dd", tagInfo);

        tagInfo = new TagInfo("menu", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("menu", tagInfo);
        
        tagInfo = new TagInfo("dir", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.block);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("dir", tagInfo);
    	
    }
    
    
    public void linkElements(TagInfo tagInfo){
    	
    	 tagInfo = new TagInfo("link", ContentType.none, BelongsTo.HEAD, false, false, false, CloseTag.forbidden, Display.none);
         this.put("link", tagInfo);
         
         tagInfo = new TagInfo("a", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
         tagInfo.defineCloseBeforeTags("a");
         this.put("a", tagInfo);
    }
    	
    
    public void tableElements(TagInfo tagInfo){
    	
    	tagInfo = new TagInfo("table", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineAllowedChildrenTags("tr,tbody,thead,tfoot,colgroup,caption");
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("tr,thead,tbody,tfoot,caption,colgroup,table,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("table", tagInfo);

        tagInfo = new TagInfo("tr", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineRequiredEnclosingTags("tbody");
        tagInfo.defineAllowedChildrenTags("td,th");
        tagInfo.defineHigherLevelTags("thead,tfoot");
        tagInfo.defineCloseBeforeTags("tr,td,th,caption,colgroup");
        this.put("tr", tagInfo);

        // jericho parser requires <td></td>
        tagInfo = new TagInfo("td", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineRequiredEnclosingTags("tr");
        tagInfo.defineCloseBeforeTags("td,th,caption,colgroup");
        this.put("td", tagInfo);

        tagInfo = new TagInfo("th", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineRequiredEnclosingTags("tr");
        tagInfo.defineCloseBeforeTags("td,th,caption,colgroup");
        this.put("th", tagInfo);

        tagInfo = new TagInfo("tbody", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineAllowedChildrenTags("tr,form");
        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
        this.put("tbody", tagInfo);

        tagInfo = new TagInfo("thead", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineAllowedChildrenTags("tr,form");
        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
        this.put("thead", tagInfo);

        tagInfo = new TagInfo("tfoot", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineAllowedChildrenTags("tr,form");
        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
        this.put("tfoot", tagInfo);

        tagInfo = new TagInfo("col", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.block);
        tagInfo.defineFatalTags("colgroup");
        this.put("col", tagInfo);

        tagInfo = new TagInfo("colgroup", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
        tagInfo.defineFatalTags("table");
        tagInfo.defineAllowedChildrenTags("col");
        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
        this.put("colgroup", tagInfo);
        
        tagInfo = new TagInfo("caption", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        tagInfo.defineFatalTags("table");
        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
        this.put("caption", tagInfo);
        
    }
    
    
    public void styleElements(TagInfo tagInfo){
    	
    	tagInfo = new TagInfo("span", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
        this.put("span", tagInfo);

        tagInfo = new TagInfo("style",  ContentType.text, BelongsTo.HEAD, false, false, false, CloseTag.required, Display.none);
        this.put("style", tagInfo);

        tagInfo = new TagInfo("bgsound", ContentType.none, BelongsTo.HEAD, false, false, false, CloseTag.forbidden, Display.none);
        this.put("bgsound", tagInfo);

        tagInfo = new TagInfo("meta", ContentType.none, BelongsTo.HEAD, false, false, false, CloseTag.forbidden, Display.none);
        this.put("meta", tagInfo);

        tagInfo = new TagInfo("base", ContentType.none, BelongsTo.HEAD, false, false, false, CloseTag.forbidden, Display.none);
        this.put("base", tagInfo);
    }
    	
    
    public void scriptElements(TagInfo tagInfo){
    	
    	tagInfo = new TagInfo("script", ContentType.all, BelongsTo.HEAD_AND_BODY, false, false, false, CloseTag.required, Display.none);
        this.put("script", tagInfo);

        tagInfo = new TagInfo("noscript", ContentType.all, BelongsTo.HEAD_AND_BODY, false, false, false, CloseTag.required, Display.block);
        this.put("noscript", tagInfo);
        
        tagInfo = new TagInfo("applet", ContentType.all, BelongsTo.BODY, true, false, false, CloseTag.required, Display.any);
        this.put("applet", tagInfo);
        
        tagInfo = new TagInfo("object", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.any);
        this.put("object", tagInfo);

        tagInfo = new TagInfo("param", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.none);
        tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
        this.put("param", tagInfo);
    }
        
       
    public void imgElements(TagInfo tagInfo){
    	  tagInfo = new TagInfo("img", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.inline);
          this.put("img", tagInfo);

          tagInfo = new TagInfo("area", ContentType.none, BelongsTo.BODY, false, false, false, CloseTag.forbidden, Display.none);
          tagInfo.defineFatalTags("map");
          tagInfo.defineCloseBeforeTags("area");
          this.put("area", tagInfo);

          tagInfo = new TagInfo("map", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.any);
          tagInfo.defineCloseBeforeTags("map");
          this.put("map", tagInfo);

    }

      
    	public void olderElements(TagInfo tagInfo){
    		tagInfo = new TagInfo("listing", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
            tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
            tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
            this.put("listing", tagInfo);

            tagInfo = new TagInfo("nobr", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
            tagInfo.defineCloseBeforeTags("nobr");
            this.put("nobr", tagInfo);

            tagInfo = new TagInfo("xmp",  ContentType.text, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
            this.put("xmp", tagInfo);

            tagInfo = new TagInfo("xml", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.none);
            this.put("xml", tagInfo);

            tagInfo = new TagInfo("isindex", ContentType.none, BelongsTo.BODY, true, false, false, CloseTag.forbidden, Display.block);
            tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
            tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
            this.put("isindex", tagInfo);

            tagInfo = new TagInfo("comment", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.none);
            this.put("comment", tagInfo);

            tagInfo = new TagInfo("server", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.none);
            this.put("server", tagInfo);

            tagInfo = new TagInfo("iframe", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.any);
            this.put("iframe", tagInfo);
    		
    	}
      
    
    protected void put(String tagName, TagInfo tagInfo) {
        this.tagInfoMap.put(tagName, tagInfo);
    }

    public TagInfo getTagInfo(String tagName) {
        if ( tagName == null) {
            // null named tagNode happens when a html fragment is being dealt with
            return null;
        } else {
            return this.tagInfoMap.get(tagName.toLowerCase());
        }
    }

}
