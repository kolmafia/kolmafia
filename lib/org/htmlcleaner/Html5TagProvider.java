/*  Copyright (c) 2006-2017, Philokypros Ioulianou and the HTMLCleaner team
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

public class Html5TagProvider implements ITagInfoProvider {

	private static final String STRONG = "strong";
	private ConcurrentMap<String, TagInfo> tagInfoMap = new ConcurrentHashMap<String, TagInfo>();
	// singleton instance, used if no other TagInfoProvider is specified
	public final static Html5TagProvider INSTANCE = new Html5TagProvider();
	public MathMLTagProvider INSTANCE2;

	private static final String CLOSE_BEFORE_COPY_INSIDE_TAGS = "bdo," + STRONG
			+ ",em,q,b,i,sub,sup,small,s";
	private static final String CLOSE_BEFORE_TAGS = "p,summary,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml";

	// private static final String CLOSE_BEFORE_TAGS =
	// "h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml";

	/**
	 * Phrasing tags are those that can make up paragraphs along with text to
	 * make Phrasing Content. Generally speaking, phrasing content only allows phrasing content as child tags.
	 */
	private static final String PHRASING_TAGS = "a,abbr,area,audio,b,bdi,bdo,br,button,canvas,cite,code,command,datalist,del,dfn,em,i,input,ins,kbd,keygen,label,link,map,mark,meta,meter,noscript,output,progress,p,ruby,samp,s,script,select,small,span,strong,sub,sup,template,textarea,time,u,var,wbr";

	/**
	 * Most elements that are used in the body of documents and applications are categorized as flow content.
	 */
	private static final String FLOW_TAGS = "a,abbr,address,area,article,aside,audio,b,bdi,bdo,blockquote,br,button,canvas,cite,code,data,datalist,del,dfn,div,dl,em,embed,fieldset,figure,footer,form,h1,h2,h3,h4,h5,h6,header,hr,i,iframe,img,input,ins,kbd,keygen,label,main,map,mark,math,meter,nav,noscript,object,ol,output,p,pre,progress,q,ruby,s,samp,script,section,select,small,span,strong,sub,sup,svg,table,template,textarea,time,u,ul,var,video,wbr,text";
	
	/**
	 * HTML5 Media Tags
	 */
	private static final String MEDIA_TAGS = "audio,video,object,source";

	public Html5TagProvider() {
		TagInfo tagInfo = null;

		embeddedContentTags(tagInfo);
		semanticFlowTags(tagInfo);
		interactiveTags(tagInfo);
		groupingTags(tagInfo);
		phrasingTags(tagInfo);
		mediaTags(tagInfo);
		editTags(tagInfo);
		formTags(tagInfo);
		tableTags(tagInfo);
		metadataTags(tagInfo);
		scriptingTags(tagInfo);
		INSTANCE2 = new MathMLTagProvider(tagInfo, tagInfoMap);
	}
	
	public void embeddedContentTags(TagInfo tagInfo) {

		// SVG
		tagInfo = new TagInfo("svg", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("svg," + CLOSE_BEFORE_TAGS);
		tagInfo.setAssumedNamespace("http://www.w3.org/2000/svg");
		this.put("svg", tagInfo);
			
		// MathML
		tagInfo = new TagInfo("math", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("math,summary,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
		//
		// We'll add this later - right now it causes more problems than it solves
		// as there are no tag name clashes between MathML and HTML unlike in SVG.
		//
		//tagInfo.setAssumedNamespace("http://www.w3.org/1998/Math/MathML");
		//
		this.put("math", tagInfo);
	}

	/**
	 * The HTML5 semantic flow tags-Sectioning tags (15 total)
	 * 
	 */
	public void semanticFlowTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("section", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("section", tagInfo);

		tagInfo = new TagInfo("nav", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("nav", tagInfo);

		tagInfo = new TagInfo("article", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineForbiddenTags("menu");
		this.put("article", tagInfo);

		tagInfo = new TagInfo("aside", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineForbiddenTags("menu");
		tagInfo.defineForbiddenTags("address");
		this.put("aside", tagInfo);

		tagInfo = new TagInfo("h1", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS+",h1,h2,h3,h4,h5,h6");
		this.put("h1", tagInfo);

		tagInfo = new TagInfo("h2", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS+",h1,h2,h3,h4,h5,h6");
		this.put("h2", tagInfo);

		tagInfo = new TagInfo("h3", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS+",h1,h2,h3,h4,h5,h6");
		this.put("h3", tagInfo);

		tagInfo = new TagInfo("h4", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS+",h1,h2,h3,h4,h5,h6");
		this.put("h4", tagInfo);

		tagInfo = new TagInfo("h5", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS+",h1,h2,h3,h4,h5,h6");
		this.put("h5", tagInfo);

		tagInfo = new TagInfo("h6", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS+",h1,h2,h3,h4,h5,h6");
		this.put("h6", tagInfo);

		tagInfo = new TagInfo("hgroup", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineAllowedChildrenTags("h1,h2,h3,h4,h5,h6");
		this.put("hgroup", tagInfo);

		// header and footer
		tagInfo = new TagInfo("header", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineForbiddenTags("menu,header,footer");
		this.put("header", tagInfo);

		tagInfo = new TagInfo("footer", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineForbiddenTags("menu,header,footer");
		this.put("footer", tagInfo);

		tagInfo = new TagInfo("main", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("main", tagInfo);

		tagInfo = new TagInfo("address", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineForbiddenTags("address");
		this.put("address", tagInfo);
	}

	/**
	 * The HTML5 Interactive tags (4 total)
	 */
	public void interactiveTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("details", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("details", tagInfo);

		tagInfo = new TagInfo("summary", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineRequiredEnclosingTags("details");
		tagInfo.defineForbiddenTags("summary");
		this.put("summary", tagInfo);

		tagInfo = new TagInfo("command", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineForbiddenTags("command");
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("command", tagInfo);

		tagInfo = new TagInfo("menu", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineAllowedChildrenTags("menuitem,li");
		this.put("menu", tagInfo);

		tagInfo = new TagInfo("menuitem", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineRequiredEnclosingTags("menu");
		this.put("menuitem", tagInfo);

		tagInfo = new TagInfo("dialog", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("dialog", tagInfo);

	}

	/**
	 * The HTML5 grouping tags (14 total)
	 */

	public void groupingTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("div", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("div", tagInfo);

		tagInfo = new TagInfo("figure", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("figure", tagInfo);

		tagInfo = new TagInfo("figcaption", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.any);
		tagInfo.defineRequiredEnclosingTags("figure");
		this.put("figcaption", tagInfo);

		tagInfo = new TagInfo("p", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("p,address,summary,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml,time");
		this.put("p", tagInfo);

		tagInfo = new TagInfo("pre", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("pre", tagInfo);

		tagInfo = new TagInfo("ul", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("dl,"+CLOSE_BEFORE_TAGS);
		//
		// This is not correct, but is how most browsers seem to handle
		// lists. Strictly, only an LI can be a child of a UL or OL
		//
		tagInfo.defineAllowedChildrenTags("li,ul,ol,div");
		//
		// Where we do have invalid children, we try to insert a LI to make it valid
		// rather than move out the content.
		//
		tagInfo.setPreferredChildTag("li");
		this.put("ul", tagInfo);

		tagInfo = new TagInfo("ol", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("dl,"+CLOSE_BEFORE_TAGS);
		//
		// This is not correct, but is how most browsers seem to handle
		// lists. Strictly, only an LI can be a child of a UL or OL
		//
		tagInfo.defineAllowedChildrenTags("li,ul,ol,div");
		//
		// Where we do have invalid children, we try to insert a LI to make it valid
		// rather than move out the content.
		//
		tagInfo.setPreferredChildTag("li");
		this.put("ol", tagInfo);

		tagInfo = new TagInfo("li", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("li," + CLOSE_BEFORE_TAGS);
		tagInfo.defineRequiredEnclosingTags("ol,menu,ul");
		this.put("li", tagInfo);

		tagInfo = new TagInfo("dl", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineAllowedChildrenTags("dt,dd");
		this.put("dl", tagInfo);

		tagInfo = new TagInfo("dt", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineCloseBeforeTags("dt,dd");
		tagInfo.defineRequiredEnclosingTags("dl");
		this.put("dt", tagInfo);

		tagInfo = new TagInfo("dd", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineCloseBeforeTags("dt,dd");
		tagInfo.defineRequiredEnclosingTags("dl");
		this.put("dd", tagInfo);

		tagInfo = new TagInfo("hr", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("hr", tagInfo);

		tagInfo = new TagInfo("blockquote", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("blockquote", tagInfo);
	}

	/**
	 * Html5 phrasing tags --text level semantics (31 total) thelw data
	 */
	public void phrasingTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("em", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("em", tagInfo);

		tagInfo = new TagInfo(STRONG, ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put(STRONG, tagInfo);

		tagInfo = new TagInfo("small", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("b,u,i,sub,sup,blink,s");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("small", tagInfo);

		tagInfo = new TagInfo("s", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("b,u,i,sub,sup,small,blink");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("s", tagInfo);

		tagInfo = new TagInfo("a", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseBeforeTags("a");
		this.put("a", tagInfo);

		tagInfo = new TagInfo("wbr", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.none);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("wbr", tagInfo);

		tagInfo = new TagInfo("mark", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("mark", tagInfo);

		tagInfo = new TagInfo("bdi", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("bdi", tagInfo);

		tagInfo = new TagInfo("time", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("time", tagInfo);

		tagInfo = new TagInfo("data", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("data", tagInfo);

		tagInfo = new TagInfo("cite", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("cite", tagInfo);

		tagInfo = new TagInfo("q", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("q", tagInfo);

		tagInfo = new TagInfo("code", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("code", tagInfo);

		tagInfo = new TagInfo("span", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);		
		this.put("span", tagInfo);

		tagInfo = new TagInfo("bdo", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("bdo", tagInfo);

		tagInfo = new TagInfo("dfn", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("dfn", tagInfo);

		tagInfo = new TagInfo("kbd", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("kbd", tagInfo);

		tagInfo = new TagInfo("abbr", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("abbr", tagInfo);

		tagInfo = new TagInfo("var", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("var", tagInfo);

		tagInfo = new TagInfo("samp", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("samp", tagInfo);

		tagInfo = new TagInfo("br", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.none);
		this.put("br", tagInfo);

		tagInfo = new TagInfo("sub", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("b,u,i,sup,small,blink,s");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("sub", tagInfo);

		tagInfo = new TagInfo("sup", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("b,u,i,sub,small,blink,s");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("sup", tagInfo);

		tagInfo = new TagInfo("b", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("u,i,sub,sup,small,blink,s");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("b", tagInfo);

		tagInfo = new TagInfo("i", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("b,u,sub,sup,small,blink,s");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("i", tagInfo);

		tagInfo = new TagInfo("u", ContentType.all, BelongsTo.BODY, true,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseInsideCopyAfterTags("b,i,sub,sup,small,blink,s");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("u", tagInfo);

		// ---->Html5 Ruby text (added rb,rtc)

		tagInfo = new TagInfo("ruby", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags("rt,rp,rb,rtc");
		this.put("ruby", tagInfo);

		tagInfo = new TagInfo("rtc", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.inline);
		tagInfo.defineRequiredEnclosingTags("ruby");
		tagInfo.defineAllowedChildrenTags("rt,"+PHRASING_TAGS);
		this.put("rtc", tagInfo);

		tagInfo = new TagInfo("rb", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.inline);
		tagInfo.defineRequiredEnclosingTags("ruby");
		this.put("rb", tagInfo);

		tagInfo = new TagInfo("rt", ContentType.text, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.inline);
		tagInfo.defineRequiredEnclosingTags("ruby");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("rt", tagInfo);

		tagInfo = new TagInfo("rp", ContentType.text, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.inline);
		tagInfo.defineRequiredEnclosingTags("ruby");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("rp", tagInfo);
	}

	/**
	 * Html5 media-embedded tags (12 tags)
	 */
	public void mediaTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("img", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.inline);
		this.put("img", tagInfo);

		tagInfo = new TagInfo("iframe", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		this.put("iframe", tagInfo);

		tagInfo = new TagInfo("embed", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		this.put("embed", tagInfo);

		tagInfo = new TagInfo("object", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		this.put("object", tagInfo);

		tagInfo = new TagInfo("param", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.none);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
		tagInfo.defineRequiredEnclosingTags("object");
		this.put("param", tagInfo);

		tagInfo = new TagInfo("audio", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseInsideCopyAfterTags(MEDIA_TAGS);
		this.put("audio", tagInfo);

		tagInfo = new TagInfo("picture", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseInsideCopyAfterTags(MEDIA_TAGS);
		this.put("picture", tagInfo);

		tagInfo = new TagInfo("video", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseInsideCopyAfterTags(MEDIA_TAGS);
		this.put("video", tagInfo);

		tagInfo = new TagInfo("source", ContentType.none, BelongsTo.BODY,
				false, false, false, CloseTag.forbidden, Display.any);
		tagInfo.defineRequiredEnclosingTags("audio,video,object");
		this.put("source", tagInfo);

		tagInfo = new TagInfo("track", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.any);
		tagInfo.defineRequiredEnclosingTags(MEDIA_TAGS);
		this.put("track", tagInfo);

		tagInfo = new TagInfo("canvas", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		this.put("canvas", tagInfo);

		tagInfo = new TagInfo("area", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.none);
		tagInfo.defineFatalTags("map");
		tagInfo.defineCloseBeforeTags("area");
		this.put("area", tagInfo);

		tagInfo = new TagInfo("map", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseBeforeTags("map");
		tagInfo.defineAllowedChildrenTags("area");
		this.put("map", tagInfo);
	}

	/**
	 * The HTML5 edits tags (2 total)
	 */
	public void editTags(TagInfo tagInfo) {
		tagInfo = new TagInfo("ins", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		this.put("ins", tagInfo);

		tagInfo = new TagInfo("del", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		this.put("del", tagInfo);
	}

	/**
	 * The HTML5 table tags (12 total)
	 */
	public void tableTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("table", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineAllowedChildrenTags("tr,tbody,thead,tfoot,col,colgroup,caption");
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("tr,thead,tbody,tfoot,caption,colgroup,table,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
		this.put("table", tagInfo);

		tagInfo = new TagInfo("tr", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineRequiredEnclosingTags("tbody");
		tagInfo.defineAllowedChildrenTags("td,th");
		//
		// Where we do have invalid children, we try to insert a TD to make it valid
		// rather than move out the content.
		//
		tagInfo.setPreferredChildTag("td");
		tagInfo.defineHigherLevelTags("thead,tfoot");
		tagInfo.defineCloseBeforeTags("tr,td,th,caption,colgroup");
		this.put("tr", tagInfo);

		// jericho parser requires <td></td>
		tagInfo = new TagInfo("td", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineRequiredEnclosingTags("tr");
		tagInfo.defineHigherLevelTags("tr");
		tagInfo.defineCloseBeforeTags("td,th,caption,colgroup");
		this.put("td", tagInfo);

		tagInfo = new TagInfo("th", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineRequiredEnclosingTags("tr");
		tagInfo.defineCloseBeforeTags("td,th,caption,colgroup");
		this.put("th", tagInfo);

		tagInfo = new TagInfo("tbody", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineAllowedChildrenTags("tr,form");
		tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
		this.put("tbody", tagInfo);

		tagInfo = new TagInfo("thead", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineAllowedChildrenTags("tr,form");
		tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
		this.put("thead", tagInfo);

		tagInfo = new TagInfo("tfoot", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.optional, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineAllowedChildrenTags("tr,form");
		tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
		this.put("tfoot", tagInfo);

		tagInfo = new TagInfo("col", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.block);
		tagInfo.defineFatalTags("colgroup");
		this.put("col", tagInfo);

		tagInfo = new TagInfo("colgroup", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.optional, Display.block);
		tagInfo.defineFatalTags("table");
		tagInfo.defineAllowedChildrenTags("col");
		tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
		this.put("colgroup", tagInfo);

		tagInfo = new TagInfo("caption", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.inline);
		tagInfo.defineFatalTags("table");
		tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
		this.put("caption", tagInfo);

	}

	/**
	 * The HTML5 forms tags (15 total)
	 * 
	 */
	public void formTags(TagInfo tagInfo) {

		tagInfo = new TagInfo("meter", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		tagInfo.defineCloseBeforeTags("meter");
		this.put("meter", tagInfo);

		tagInfo = new TagInfo("form", ContentType.all, BelongsTo.BODY, false,
				false, true, CloseTag.required, Display.block);
		tagInfo.defineForbiddenTags("form");
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("option,optgroup,textarea,select,fieldset,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
		this.put("form", tagInfo);

		tagInfo = new TagInfo("input", ContentType.none, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.inline);
		tagInfo.defineCloseBeforeTags("select,optgroup,option");
		this.put("input", tagInfo);

		tagInfo = new TagInfo("textarea", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.inline);
		tagInfo.defineCloseBeforeTags("select,optgroup,option");
		this.put("textarea", tagInfo);

		tagInfo = new TagInfo("select", ContentType.all, BelongsTo.BODY, false,
				false, true, CloseTag.required, Display.inline);
		tagInfo.defineAllowedChildrenTags("option,optgroup");
		tagInfo.defineCloseBeforeTags("option,optgroup,select");
		this.put("select", tagInfo);

		tagInfo = new TagInfo("option", ContentType.text, BelongsTo.BODY,
				false, false, true, CloseTag.optional, Display.inline);
		tagInfo.defineFatalTags("select,datalist");
		tagInfo.defineCloseBeforeTags("option");
		this.put("option", tagInfo);

		tagInfo = new TagInfo("optgroup", ContentType.all, BelongsTo.BODY,
				false, false, true, CloseTag.required, Display.inline);
		tagInfo.defineFatalTags("select");
		tagInfo.defineAllowedChildrenTags("option");
		tagInfo.defineCloseBeforeTags("optgroup");
		this.put("optgroup", tagInfo);

		tagInfo = new TagInfo("button", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseBeforeTags("select,optgroup,option");
		this.put("button", tagInfo);

		tagInfo = new TagInfo("label", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.inline);
		this.put("label", tagInfo);

		tagInfo = new TagInfo("legend", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.block);
		tagInfo.defineRequiredEnclosingTags("fieldset");
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		this.put("legend", tagInfo);

		tagInfo = new TagInfo("fieldset", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.block);
		tagInfo.defineCloseBeforeCopyInsideTags(CLOSE_BEFORE_COPY_INSIDE_TAGS);
		tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
		this.put("fieldset", tagInfo);

		tagInfo = new TagInfo("progress", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.any);
		tagInfo.defineAllowedChildrenTags(PHRASING_TAGS);
		tagInfo.defineCloseBeforeTags("progress");
		this.put("progress", tagInfo);

		tagInfo = new TagInfo("datalist", ContentType.all, BelongsTo.BODY,
				false, false, false, CloseTag.required, Display.any);
		tagInfo.defineAllowedChildrenTags("option");
		tagInfo.defineCloseBeforeTags("datalist");
		this.put("datalist", tagInfo);

		tagInfo = new TagInfo("keygen", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.forbidden, Display.any);
		this.put("keygen", tagInfo);

		tagInfo = new TagInfo("output", ContentType.all, BelongsTo.BODY, false,
				false, false, CloseTag.required, Display.any);
		tagInfo.defineCloseBeforeTags("output," + CLOSE_BEFORE_TAGS);
		this.put("output", tagInfo);
	}

	/**
	 * HTML5 Document metadata tags
	 */
	public void metadataTags(TagInfo tagInfo) {

		// As of HTML5, meta can be used in <body> where it has a @name attribute
		// TODO add attribute rules
		tagInfo = new TagInfo("meta", ContentType.none, BelongsTo.HEAD_AND_BODY, false,
				false, false, CloseTag.forbidden, Display.none);
		this.put("meta", tagInfo);
		// As of HTML5, link can be used in <body> where it has an  @itemprop attribute
		// TODO add attribute rules
		tagInfo = new TagInfo("link", ContentType.none, BelongsTo.HEAD_AND_BODY, false,
				false, false, CloseTag.forbidden, Display.none);
		this.put("link", tagInfo);

		tagInfo = new TagInfo("title", ContentType.text, BelongsTo.HEAD, false,
				true, false, CloseTag.required, Display.none);
		this.put("title", tagInfo);

		// As of HTML5, style can be used in <body> where it has an @scoped attribute
		// TODO add attribute rules
		tagInfo = new TagInfo("style", ContentType.text, BelongsTo.HEAD_AND_BODY, false,
				false, false, CloseTag.required, Display.none);
		this.put("style", tagInfo);

		tagInfo = new TagInfo("base", ContentType.none, BelongsTo.HEAD, false,
				false, false, CloseTag.forbidden, Display.none);
		this.put("base", tagInfo);
	}

	/**
	 * HTML5 scripting tags
	 */
	public void scriptingTags(TagInfo tagInfo) {
		tagInfo = new TagInfo("script", ContentType.all,
				BelongsTo.HEAD_AND_BODY, false, false, false,
				CloseTag.required, Display.none);
		this.put("script", tagInfo);

		tagInfo = new TagInfo("noscript", ContentType.all,
				BelongsTo.HEAD_AND_BODY, false, false, false,
				CloseTag.required, Display.block);
		this.put("noscript", tagInfo);
	}

	/**
	 * It inserts the tag node into the tagInfoMap.
	 * 
	 * @param tagName
	 *            The name of the tag
	 * @param tagInfo
	 *            The info about tag node
	 */
	protected void put(String tagName, TagInfo tagInfo) {
		this.tagInfoMap.put(tagName, tagInfo);
	}

	/**
	 * It returns the tag information.
	 * 
	 * @param tagName
	 *            The name of the tag to return
	 * @return TagInfo The information about tag node
	 */
	public TagInfo getTagInfo(String tagName) {
		if (tagName == null) {
			// null named tagNode happens when a html fragment is being dealt
			// with
			return null;
		} else {
			return this.tagInfoMap.get(tagName.toLowerCase());
		}
	}

}
