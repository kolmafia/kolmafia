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

import java.io.*;
import java.util.*;

/**
 * Main HTML tokenizer.
 * <p>It's task is to parse HTML and produce list of valid tokens:
 * open tag tokens, end tag tokens, contents (text) and comments.
 * As soon as new item is added to token list, cleaner is invoked
 * to clean current list at the end.</p>
 *
 * Created by: Vladimir Nikic.<br>
 * Date: November, 2006

 */
public class HtmlTokenizer {

	private final static int WORKING_BUFFER_SIZE = 1024;

    private BufferedReader _reader;
    private char[] _working = new char[WORKING_BUFFER_SIZE];

    private transient int _pos;
    private transient int _len = -1;
    private transient int _row = 1;
    private transient int _col = 1;
    

    private transient StringBuffer _saved = new StringBuffer(512);

    private transient boolean _isLateForDoctype;
    private transient DoctypeToken _docType;
    private transient TagToken _currentTagToken;
    private transient List<BaseToken> _tokenList = new ArrayList<BaseToken>();
    private transient Set<String> _namespacePrefixes = new HashSet<String>();

    private boolean _asExpected = true;
    
    private boolean _isSpecialContext;
    private String _isSpecialContextName;

    private HtmlCleaner cleaner;
    private CleanerProperties props;
    private CleanerTransformations transformations;
    private CleanTimeValues cleanTimeValues;


    /**
     * Constructor - creates instance of the parser with specified content.
     * @param cleaner
     * @param reader
     */
    public HtmlTokenizer(HtmlCleaner cleaner, Reader reader, final CleanTimeValues cleanTimeValues) {
        this._reader = new BufferedReader(reader);
        this.cleaner = cleaner;
        this.props = cleaner.getProperties();
        this.transformations = cleaner.getTransformations();
        this.cleanTimeValues = cleanTimeValues;
    }

    private void addToken(BaseToken token) {
        token.setRow(_row);
        token.setCol(_col);
        _tokenList.add(token);
        cleaner.makeTree( _tokenList, _tokenList.listIterator(_tokenList.size() - 1), this.cleanTimeValues );
    }

    private void readIfNeeded(int neededChars) throws IOException {
        if (_len == -1 && _pos + neededChars >= WORKING_BUFFER_SIZE) {
            int numToCopy = WORKING_BUFFER_SIZE - _pos;
            System.arraycopy(_working, _pos, _working, 0, numToCopy);
            _pos = 0;

            int expected = WORKING_BUFFER_SIZE - numToCopy;
            int size = 0;
            int charsRead = 0;
            int offset = numToCopy;
            do {
                charsRead = _reader.read(_working, offset, expected);
                if (charsRead >= 0) {
                    size += charsRead;
                    offset += charsRead;
                    expected -= charsRead;
                }
            } while (charsRead >= 0 && expected > 0);

            if (expected > 0) {
    		_len = size + numToCopy;
    		
            }

            // convert invalid XML characters to spaces or the UTF replacement character
            for (int i = 0; i < (_len >= 0 ? _len : WORKING_BUFFER_SIZE); i++) {
                int ch = _working[i];
                if (ch >= 1 && ch <= 32 && ch != 10 && ch != 13) {
                    _working[i] = ' ';
                }
                if (ch == 0){
                	_working[i] = '\uFFFD'; 
                }
            }
        }
    }

    List<BaseToken> getTokenList() {
    	return this._tokenList;
    }

    Set<String> getNamespacePrefixes() {
        return _namespacePrefixes;
    }

    private void go() throws IOException {
    	go(1);
    }

    private void go(int step) throws IOException {
    	_pos += step;
    	readIfNeeded(step - 1);
    	//
    	// If we use go() to wind back, make sure _pos
    	// doesn't go negative.
    	//
		if (_pos < 0) _pos=0;
    }

    /**
     * Checks if content starts with specified value at the current position.
     * @param value
     * @return true if starts with specified value, false otherwise.
     * @throws IOException
     */
    private boolean startsWith(String value) throws IOException {
        int valueLen = value.length();
        readIfNeeded(valueLen);
        if (_len >= 0 && _pos + valueLen  > _len) {
            return false;
        }

        for (int i = 0; i < valueLen; i++) {
        	char ch1 = Character.toLowerCase( value.charAt(i) );
        	char ch2 = Character.toLowerCase( _working[_pos + i] );
        	if (ch1 != ch2) {
        		return false;
        	}
        }

        return true;
    }

    /**
     * Checks if character at specified position is whitespace.
     * @param position
     * @return true is whitespace, false otherwise.
     */
    private boolean isWhitespace(int position) {
    	if (_len >= 0 && position >= _len) {
            return false;
        }

        return Character.isWhitespace( _working[position] );
    }

    /**
     * Checks if character at current runtime position is whitespace.
     * @return true is whitespace, false otherwise.
     */
    private boolean isWhitespace() {
        return isWhitespace(_pos);
    }

    /**
     * Checks if character at specified position is equal to specified char.
     * @param position
     * @param ch
     * @return true is equals, false otherwise.
     */
    private boolean isChar(int position, char ch) {
    	if (_len >= 0 && position >= _len) {
            return false;
        }

        return Character.toLowerCase(ch) == Character.toLowerCase(_working[position]);
    }

    /**
     * Checks if character at current runtime position is equal to specified char.
     * @param ch
     * @return true is equal, false otherwise.
     */
    private boolean isChar(char ch) {
        return isChar(_pos, ch);
    }

    /**
     * Checks if character at specified position can be identifier start.
     * @param position
     * @return true is may be identifier start, false otherwise.
     */
    private boolean isElementIdentifierStartChar(int position) {
    	if (_len >= 0 && position >= _len) {
            return false;
        }

        char ch = _working[position];
        return Character.isUnicodeIdentifierStart(ch);
    }

    /**
     * Checks if character at current runtime position can be identifier start.
     * @return true is may be identifier start, false otherwise.
     */
    private boolean isHtmlAttributeIdentifierStartChar() {
    	//
    	// Bizarrely, HTML allows '<' in attribute names. However, it can't start with one.
    	//
    	char ch = _working[_pos];
    	if (ch == '<') return false;
    	//
    	// Check other identifier rules.
    	//
    	return isHtmlAttributeIdentifierChar();
    }
    
    private boolean isHtmlAttributeIdentifierChar(){
    	return isHtmlAttributeIdentifierChar(_pos);
    }    
    
    private boolean isHtmlElementIdentifier(){
    	return isHtmlElementIdentifier(_pos);
    }    
    
    private boolean isHtmlElementIdentifier(int position){
    	if (!isHtmlAttributeIdentifierChar(position)) return false;
    	
    	if (_len >= 0 && position >= _len) {
            return false;
        } 
    	
        char ch = _working[position];
        if (ch == '<') return false;
        
        return true;
    }
    
    /**
     * Check whether the character at the specified position in the stream is a
     * valid character for part of an attribute identifier in HTML
     * @param position
     * @return
     */
    private boolean isHtmlAttributeIdentifierChar(int position){
    	
    	// The following can't be used as HTML attribute names
    	// spaces, the control characters, and any characters that are not defined by Unicode
    	// U+0000 NULL
    	// U+0022 QUOTATION MARK (")
    	// U+0027 APOSTROPHE (')
    	// U+003E GREATER-THAN SIGN (>)
    	// U+002F SOLIDUS (/)
    	// U+003D EQUALS SIGN (=)
    	// See: https://html.spec.whatwg.org/multipage/syntax.html#attributes-2
    	
    	if (_len >= 0 && position >= _len) {
            return false;
        } 
    	
        char ch = _working[position];
                        
        if (Character.isWhitespace(ch)) return false;
        
        if ( ch == '\u0000' || ch =='\uFFFD' || ch == '\u0022' || ch == "\u0027".charAt(0) || ch == '\u003E' || ch== '\u002F' || ch == '\u003D'){
        	return false;
        }
        
        if (Character.isISOControl(ch)) return false;       
        
        if (!Character.isDefined(ch)) return false;
        
        return true;

    }

    /**
     * Checks if end of the content is reached.
     */
    private boolean isAllRead() {
        return _len >= 0 && _pos >= _len;
    }

    /**
     * Saves specified character to the temporary buffer.
     * @param ch
     */
    private void save(char ch) {
        updateCoordinates(ch);
        _saved.append(ch);
    }

    /**
     * Looks onto the char passed and updates current position coordinates. 
     * If char is a line break, increments row coordinate, if not -- col coordinate. 
     * 
     * @param ch - char to analyze.
     */
    private void updateCoordinates(char ch) {
        if(ch == '\n'){
            _row++;
            _col = 1;
        }else{
            _col++;
        }
    }

    /**
     * Saves character at current runtime position to the temporary buffer.
     */
    private void saveCurrent() {
        if (!isAllRead()) {
            save( _working[_pos] );
        }
    }

    /**
     * Saves specified number of characters at current runtime position to the temporary buffer.
     * @throws IOException
     */
    private void saveCurrent(int size) throws IOException {
    	readIfNeeded(size);
        int pos = _pos;
        while ( !isAllRead() && (size > 0) ) {
            save( _working[pos] );
            pos++;
            size--;
        }
    }

    /**
     * Skips whitespaces at current position and moves foreward until
     * non-whitespace character is found or the end of content is reached.
     * @throws IOException
     */
    private void skipWhitespaces() throws IOException {
        while ( !isAllRead() && isWhitespace() ) {
            saveCurrent();
            go();
        }
    }

    private boolean addSavedAsContent() {
        if (_saved.length() > 0) {
            addToken( new ContentNode(props.isDeserializeEntities() ? Utils.deserializeEntities(_saved.toString(), props.isRecognizeUnicodeChars()) : _saved.toString()) );
            _saved.delete(0, _saved.length());
            return true;
        }

        return false;
    }

    /**
     * Starts parsing HTML.
     * @throws IOException
     */
    void start() throws IOException {
    	// initialize runtime values
    	_currentTagToken = null;
    	_tokenList.clear();
    	_asExpected = true;
    	_isSpecialContext = false;
    	_isLateForDoctype = false;
    	_namespacePrefixes.clear();

    	this._pos = WORKING_BUFFER_SIZE;
    	readIfNeeded(0);

    	boolean isSpecialEmpty = true;

    	while ( !isAllRead() ) {
    		if (Thread.currentThread().isInterrupted()) {
    			this.handleInterruption();
    			_tokenList.clear();
    			_namespacePrefixes.clear();
    			_reader.close();
            	return;
            }
    		// resets all the runtime values
    		_saved.delete(0, _saved.length());
    		_currentTagToken = null;
    		_asExpected = true;

    		// this is enough for making decision
    		readIfNeeded(10);

    		if (_isSpecialContext) {
    			int nameLen = _isSpecialContextName.length();
    			if ( startsWith("</" + _isSpecialContextName) && (isWhitespace(_pos + nameLen + 2) || isChar(_pos + nameLen + 2, '>')) ) {
    				tagEnd();
    			} else if ( isSpecialEmpty && startsWith("<!--") ) {
    				comment();
    			} else if ( startsWith(CData.SAFE_BEGIN_CDATA) || startsWith(CData.BEGIN_CDATA) || startsWith(CData.SAFE_BEGIN_CDATA_ALT)) { 
    				cdata();
    			} else {
    				boolean isTokenAdded = content();
    				if (isSpecialEmpty && isTokenAdded) {
    					final BaseToken lastToken = (BaseToken) _tokenList.get(_tokenList.size() - 1);
    					if (lastToken != null) {
    						final String lastTokenAsString = lastToken.toString();
    						if (lastTokenAsString != null && lastTokenAsString.trim().length() > 0) {
    							isSpecialEmpty = false;
    						}
    					}
    				}
    			}
    			if (!_isSpecialContext) {
    				isSpecialEmpty = true;
    			}
    		} else {
    			if ( startsWith("<!doctype") ) {
    				if ( !_isLateForDoctype ) {
    					doctype();
    					_isLateForDoctype = true;
    				} else {
    					ignoreUntil('<');
    				}
    			} else if ( startsWith("</") && isElementIdentifierStartChar(_pos + 2) ) {
    				_isLateForDoctype = true;
    				tagEnd();
    			} else if ( startsWith(CData.SAFE_BEGIN_CDATA) || startsWith(CData.BEGIN_CDATA) || startsWith(CData.SAFE_BEGIN_CDATA_ALT)) { 
    				cdata();
    			} else if ( startsWith("<!--") ) {
    				comment();
    			} else if ( startsWith("<") && isElementIdentifierStartChar(_pos + 1) ) {
    				_isLateForDoctype = true;
    				tagStart();
    			} else if ( props.isIgnoreQuestAndExclam() && (startsWith("<!") || startsWith("<?")) ) {
    				ignoreUntil('<');
    				if (isChar('>')) {
    					go();
    				}
    			} else if ( startsWith("<?xml")){
    				ignoreUntil('<');
    			} else {
    				content();
    			}
    		}
    	}


    	_reader.close();
    }

    /**
     * Checks if specified tag name is one of the reserved tags: HTML, HEAD or BODY
     * @param tagName
     * @return
     */
    private boolean isReservedTag(String tagName) {
        return "html".equalsIgnoreCase(tagName) || "head".equalsIgnoreCase(tagName) || "body".equalsIgnoreCase(tagName);
    }

    /**
     * Parses start of the tag.
     * It expects that current position is at the "<" after which
     * the tag's name follows.
     * @throws IOException
     */
    private void tagStart() throws IOException {
        saveCurrent();
        go();

        if ( isAllRead() ) {
            return;
        }

        String originalTagName = identifier(false);
        String tagName = transformations.getTagName(originalTagName);

        if (tagName != null) {
            ITagInfoProvider tagInfoProvider = cleaner.getTagInfoProvider();
            TagInfo tagInfo = tagInfoProvider.getTagInfo(tagName);
            if ( (tagInfo == null && !props.isOmitUnknownTags() && props.isTreatUnknownTagsAsContent() && !isReservedTag(tagName) && !props.isNamespacesAware()) ||
                 (tagInfo != null && tagInfo.isDeprecated() && !props.isOmitDeprecatedTags() && props.isTreatDeprecatedTagsAsContent()) ) {
                content();
                return;
            }
        }

        TagNode tagNode = new TagNode(tagName);
        tagNode.setTrimAttributeValues(props.isTrimAttributeValues());
        _currentTagToken = tagNode;

        if (_asExpected) {
            skipWhitespaces();
            tagAttributes();

            if (tagName != null) {
                if (transformations != null) {
                    tagNode.setAttributes(transformations.transformAttributes(originalTagName, tagNode.getAttributesInLowerCase()));
                }
                addToken(_currentTagToken);
            }

            if ( isChar('>') ) {
            	go();
            	if ( props.isUseCdataFor(tagName) ) {
            		_isSpecialContext = true;
            		_isSpecialContextName = tagName;
            	}           
            } else if ( startsWith("/>") ) {
            	go(2);
            	//
            	// If the tag is self-closing, add an end tag token here to avoid
            	// encapsulating the following content. See issue #93.
            	//
            	addToken(new EndTagToken(tagName));
            }

            _currentTagToken = null;
        } else {
        	addSavedAsContent();
        }
    }


    /**
     * Parses end of the tag.
     * It expects that current position is at the "<" after which
     * "/" and the tag's name follows.
     * @throws IOException
     */
    private void tagEnd() throws IOException {
        saveCurrent(2);
        go(2);
        _col += 2;

        if ( isAllRead() ) {
            return;
        }

        String tagName = identifier(false);
        if (transformations != null && transformations.hasTransformationForTag(tagName)) {
            TagTransformation tagTransformation = transformations.getTransformation(tagName);
            if (tagTransformation != null) {
                tagName = tagTransformation.getDestTag();
            }
        }

        if (tagName != null) {
            ITagInfoProvider tagInfoProvider = cleaner.getTagInfoProvider();
            TagInfo tagInfo = tagInfoProvider.getTagInfo(tagName);
        	if ( (tagInfo == null && !props.isOmitUnknownTags() && props.isTreatUnknownTagsAsContent() && !isReservedTag(tagName) &&!props.isNamespacesAware()) ||
                 (tagInfo != null && tagInfo.isDeprecated() && !props.isOmitDeprecatedTags() && props.isTreatDeprecatedTagsAsContent()) ) {
                content();
                return;
            }
        }

        _currentTagToken = new EndTagToken(tagName);

        if (_asExpected) {
            skipWhitespaces();
            tagAttributes();

            if (tagName != null) {
                addToken(_currentTagToken);
            }

            if ( isChar('>') ) {
            	go();
            }
            
            if ( props.isUseCdataFor(tagName) ) {
            	_isSpecialContext = false;
            	_isSpecialContextName = tagName;
            }

			//
			// Skip any whitespace after the closing HTML tag rather than move
			// it.
			//
			// If there is any content after the closing HTML tag it will still
			// be moved, just with any preceding whitespace stripped. This
			// removes the situation of extra newlines following the end of the
			// document being moved inside the body (see #67).
			//
            if (tagName != null && tagName.equalsIgnoreCase("html")){
            	skipWhitespaces();
            }
            
            _currentTagToken = null;
        } else {
            addSavedAsContent();
        }
    }

    /**
     * Parses an identifier from the current position.
     * @attribute true if using attribute identifier rules, false otherwise
     * @throws IOException
     */
    private String identifier(boolean attribute) throws IOException {
        _asExpected = true;

        if ( !isHtmlAttributeIdentifierStartChar() ) {
            _asExpected = false;
            return null;
        }

        StringBuffer identifierValue = new StringBuffer();

        while ( !isAllRead() && (attribute && isHtmlAttributeIdentifierChar() || !attribute && isHtmlElementIdentifier()) ) {
            saveCurrent();
            identifierValue.append( _working[_pos] );
            go();
        }

        if ( identifierValue.length() == 0 ) {
            return null;
        }

        String id = identifierValue.toString();

        int columnIndex = id.indexOf(':');
        if (columnIndex >= 0) {
            String prefix = id.substring(0, columnIndex);
            String suffix = id.substring(columnIndex + 1);
            int nextColumnIndex = suffix.indexOf(':');
            if (nextColumnIndex >= 0) {
                suffix = suffix.substring(0, nextColumnIndex);
            }
            if (props.isNamespacesAware()) {
                id = prefix + ":" + suffix;
                if ( !"xmlns".equalsIgnoreCase(prefix) ) {
                    _namespacePrefixes.add( prefix.toLowerCase() );
                }
            } else {
                id = suffix;
            }
        }

        return id;
    }

    /**
     * Parses list tag attributes from the current position.
     * @throws IOException
     */
    private void tagAttributes() throws IOException {
        while( !isAllRead() && _asExpected && !isChar('>') && !startsWith("/>") ) {
        	if (Thread.currentThread().isInterrupted()) {
    	    	// Interruption: risk to take a lot of time in case of damaged file
        		handleInterruption();
            	return;
            }
            skipWhitespaces();
            String attName = identifier(true);

            if (!_asExpected) {
                if ( !isChar('<') && !isChar('>') && !startsWith("/>") ) {
                    saveCurrent();
                    go();
                }

                if (!isChar('<')) {
                    _asExpected = true;
                }

                continue;
            }

            String attValue;

            skipWhitespaces();
            if ( isChar('=') ) {
                saveCurrent();
                go();
                attValue = attributeValue();
            } else if (CleanerProperties.BOOL_ATT_EMPTY.equals(props.getBooleanAttributeValues())) {
                attValue = "";
            } else if (CleanerProperties.BOOL_ATT_TRUE.equals(props.getBooleanAttributeValues())) {
                attValue = "true";
            } else {
                attValue = attName;
            }

            if (_asExpected) {
                _currentTagToken.addAttribute(attName, attValue);
            }
        }
    }

    /**
     * Parses a single tag attribute - it is expected to be in one of the forms:
     * 		name=value
     * 		name="value"
     * 		name='value'
     * 		name
     * @throws IOException
     */
    private String attributeValue() throws IOException {
        skipWhitespaces();

        if ( isChar('<') || isChar('>') || startsWith("/>") ) {
        	return "";
        }

        boolean isQuoteMode = false;
        boolean isAposMode = false;

        StringBuffer result = new StringBuffer();

        if ( isChar('\'') ) {
            isAposMode = true;
            saveCurrent();
            go();
        } else if ( isChar('\"') ) {
            isQuoteMode = true;
            saveCurrent();
            go();
        }

        boolean isMultiWord = props.isAllowMultiWordAttributes();

        boolean allowHtml = props.isAllowHtmlInsideAttributes();

        while ( !isAllRead() &&
                ( (isAposMode && !isChar('\'') && (allowHtml || !isChar('>') && !isChar('<')) && (isMultiWord || !isWhitespace())) ||
                  (isQuoteMode && !isChar('\"') && (allowHtml || !isChar('>') && !isChar('<')) && (isMultiWord || !isWhitespace())) ||
                  (!isAposMode && !isQuoteMode && !isWhitespace() && !isChar('>') && !isChar('<'))
                )
              ) {
            result.append( _working[_pos] );
            saveCurrent();
            go();
        }

        if ( isChar('\'') && isAposMode ) {
            saveCurrent();
            go();
        } else if ( isChar('\"') && isQuoteMode ) {
            saveCurrent();
            go();
        }


        return result.toString();
    }

    private boolean content() throws IOException {
        while ( !isAllRead() ) {


            saveCurrent();
            go();
            
            if (startsWith(CData.SAFE_BEGIN_CDATA) || startsWith(CData.BEGIN_CDATA) || startsWith(CData.SAFE_BEGIN_CDATA_ALT)) {
            	break;
            }
 
            if (isTagStartOrEnd()) {
                break;
            }
            

            
        }

        return addSavedAsContent();
    }

    /**
     * Not all '<' (lt) symbols mean tag start or end. For example '<' can be part of 
     * mathematical expression. To avoid false breaks of content tags use this method to
     * determine content tag end.     
     * 
     * @return true if current position is tag start or end. 
     * 
     * @throws IOException
     */
    private boolean isTagStartOrEnd() throws IOException {
        return startsWith("</") || startsWith("<!") || startsWith("<?") || ((startsWith("<") && isElementIdentifierStartChar(_pos+1)));
    }

    private void ignoreUntil(char ch) throws IOException {
        while ( !isAllRead() ) {
        	go();
        	updateCoordinates(_working[_pos]);
            if ( isChar(ch) ) {
                break;
            }
        }
    }

    private void comment() throws IOException {
    	go(4);
        while ( !isAllRead() && !startsWith("-->") ) {
            saveCurrent();
            go();
        }

        if (startsWith("-->")) {
        	go(3);
        }

        if (_saved.length() > 0) {
            if ( !props.isOmitComments() ) {
                String hyphenRepl = props.getHyphenReplacementInComment();
                String comment = _saved.toString().replaceAll("--", hyphenRepl + hyphenRepl);

        		if ( comment.length() > 0 && comment.charAt(0) == '-' ) {
        			comment = hyphenRepl + comment.substring(1);
        		}
        		int len = comment.length();
        		if ( len > 0 && comment.charAt(len - 1) == '-' ) {
        			comment = comment.substring(0, len - 1) + hyphenRepl;
        		}

        		addToken( new CommentNode(comment) );
        	}
            _saved.delete(0, _saved.length());
        }
    }
    
    private void cdata() throws IOException {
    	
    	if (!_isSpecialContext){
    		//
    		// if we're not set to omit invalid CDATA, then we turn it into a regular ContentNode
    		//
    		if (!props.isOmitCdataOutsideScriptAndStyle()){
    			content();
    			return;
    		}
    	}
    	
    	if (startsWith(CData.SAFE_BEGIN_CDATA)){
    		go(CData.SAFE_BEGIN_CDATA.length());
    	} else if (startsWith(CData.SAFE_BEGIN_CDATA_ALT)){
    		go (CData.SAFE_BEGIN_CDATA_ALT.length());
    	} else {
    		go(CData.BEGIN_CDATA.length());
    	}
    	
    	int cdataStart = _saved.length();
    	
    	//
    	// Look ahead; if there are no end tokens we don't need to go through
    	// the whole stream locating the endpoint. Instead, we wind back to the 
    	// start of the open CDATA token and return.
    	//
    	if (!containsEndCData()){
    		go(cdataStart - _saved.length());
    		return;
    	}
    	
        while ( !isAllRead() && 
        		!startsWith(CData.SAFE_END_CDATA) && 
        		!startsWith(CData.END_CDATA) && 
        		!startsWith(CData.SAFE_END_CDATA_ALT)
        		) {
            saveCurrent();
            go();
        }        
        
        if (startsWith(CData.SAFE_END_CDATA)){
        	go(CData.SAFE_END_CDATA.length());
        }
        else if (startsWith(CData.SAFE_END_CDATA_ALT)){
        	go(CData.SAFE_END_CDATA_ALT.length());
        }
        else if (startsWith(CData.END_CDATA)) {
        	go(CData.END_CDATA.length());
        } else {
        	//
        	// There is no end CDATA token, so we wind back to the end of the CDATA token, and just save it there
        	// with no content. We should never see this code due to the look-ahead code above
        	//
        	go(cdataStart - _saved.length());
    		return;
        }

        if (_saved.length() > 0) {
        	//
        	// If we're not including CDATA outside of script and style tags, we don't
        	// add a token.
        	//
        	if (_isSpecialContext || !props.isOmitCdataOutsideScriptAndStyle()){
            		String cdata = _saved.toString().substring(cdataStart);
            		addToken( new CData(cdata) );
        	}

        }
        _saved.delete(cdataStart, _saved.length());

    }

    private void doctype() throws IOException {
    	go(9);

    	skipWhitespaces();
    	String part1 = identifier(false);
	    skipWhitespaces();
	    String part2 = identifier(false);
	    skipWhitespaces();
	    String part3 = attributeValue();
	    skipWhitespaces();
	    String part4 = attributeValue();
	    skipWhitespaces();
	    String part5 = attributeValue();

	    ignoreUntil('<');

	    if (part5 == null || part5.length()==0){
	    	_docType = new DoctypeToken(part1, part2, part3, part4);
	    } else {
	    	_docType = new DoctypeToken(part1, part2, part3, part4, part5);	    	
	    }
    }

    public DoctypeToken getDocType() {
        return _docType;
    }
    
	/**
	 * Called whenver the thread is interrupted. Currently this is a 
	 * placeholder, but could hold cleanup methods and user interaction
	 */
	private void handleInterruption(){
		
	}
	
	private boolean containsEndCData() throws IOException{ 
				
		//
		// Look in the current buffer
		//
		StringBuffer buffer = new StringBuffer();
		buffer.append(_working);
		String working = buffer.toString();
	    if (working.contains(CData.END_CDATA) || working.contains(CData.SAFE_END_CDATA) || working.contains(CData.SAFE_END_CDATA_ALT)){
	    	return true;
	    }
	    
		//
		// Check if the reader supports mark and reset. If not, return 
		// "false". This represents a "best effort" approach. In practice
	    // this shouldn't happen as we use BufferedReader, but overrides
	    // may use other Reader implementations so its good to have a 
	    // guard here.
		//
		if (!_reader.markSupported()){
			return false;
		}
	    	
    	//
    	// As we support mark and reset, we can look ahead and see if there are any end CDATA sections 
		// in the stream, then reset it to the current position.
    	//
		
		//
		// We can only read ahead so far in a stream; realistically,
		// we don't expect to find 512k of CDATA in any real web
		// page, but this is an arbitrary limit so YMMV.
		//
		final int MAX_BUFFER_SIZE = 512*1024;
		
		//
		// Set a mark at the current position in the stream
		//
    	_reader.mark(MAX_BUFFER_SIZE);
    	
    	buffer = new StringBuffer();
    	int c;
    	int read = 0;
    	
    	//
    	// Read characters from the stream into a working buffer and see if there is an
    	// end token
    	//
    	while ((c = _reader.read()) != -1 && read < MAX_BUFFER_SIZE) {
    		read++;
    	    buffer.append( (char)c ) ;  
    	    working = buffer.toString();
    	    if (working.contains(CData.END_CDATA) || working.contains(CData.SAFE_END_CDATA) || working.contains(CData.SAFE_END_CDATA_ALT)){
    	    	_reader.reset();
    	    	return true;
    	    }
    	    //
    	    // We only need a max of 8 chars to identify an end token, so lets keep the 
    	    // working buffer as small as we can
    	    //
    	    if (buffer.length() > 16) buffer.delete(0, 8);
    	}
    	
    	//
    	// We didn't find any end tokens
    	//
    	_reader.reset();    	
    	return false;
	}

}
