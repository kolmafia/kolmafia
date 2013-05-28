/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core.internal.wc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.wc.SVNDiffOptions;

import de.regnis.q.sequence.QSequenceDifferenceBlock;
import de.regnis.q.sequence.core.QSequenceException;
import de.regnis.q.sequence.line.QSequenceLine;
import de.regnis.q.sequence.line.QSequenceLineCache;
import de.regnis.q.sequence.line.QSequenceLineMedia;
import de.regnis.q.sequence.line.QSequenceLineRAData;
import de.regnis.q.sequence.line.QSequenceLineResult;
import de.regnis.q.sequence.line.simplifier.QSequenceLineDummySimplifier;
import de.regnis.q.sequence.line.simplifier.QSequenceLineEOLUnifyingSimplifier;
import de.regnis.q.sequence.line.simplifier.QSequenceLineSimplifier;
import de.regnis.q.sequence.line.simplifier.QSequenceLineTeeSimplifier;
import de.regnis.q.sequence.line.simplifier.QSequenceLineWhiteSpaceReducingSimplifier;
import de.regnis.q.sequence.line.simplifier.QSequenceLineWhiteSpaceSkippingSimplifier;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSMergerBySequence {

	// Constants ==============================================================

	public static final String DEFAULT_EOL = System.getProperty("line.separator");
	public static final int NOT_MODIFIED = 0;
	public static final int MERGED = 4;
	public static final int CONFLICTED = 2;

	// Fields =================================================================

	private final byte[] myConflictStart;
	private final byte[] myConflictSeparator;
	private final byte[] myConflictEnd;
	private final byte[] myOriginalMarker;
	
	// Setup ==================================================================

	public FSMergerBySequence(byte[] conflictStart, byte[] conflictSeparator, byte[] conflictEnd) {
	    this(conflictStart, conflictSeparator, conflictEnd, null);
	}


	public FSMergerBySequence(byte[] conflictStart, byte[] conflictSeparator, byte[] conflictEnd, byte[] originalMarker) {
	    myConflictStart = conflictStart;
	    myConflictSeparator = conflictSeparator;
	    myConflictEnd = conflictEnd;
	    myOriginalMarker = originalMarker;
	}

	// Accessing ==============================================================

	public int merge(QSequenceLineRAData baseData,
	                 QSequenceLineRAData localData, QSequenceLineRAData latestData,
	                 SVNDiffOptions options,
	                 OutputStream result, 
	                 SVNDiffConflictChoiceStyle style) throws IOException {
	    
	    style = style == null ? SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST : style;
	    
		final QSequenceLineResult localResult;
		final QSequenceLineResult latestResult;
		final QSequenceLineTeeSimplifier mySimplifer = createSimplifier(options);
		try {
			localResult = QSequenceLineMedia.createBlocks(baseData, localData, mySimplifer);
			latestResult = QSequenceLineMedia.createBlocks(baseData, latestData, mySimplifer);
		}
		catch (QSequenceException ex) {
			throw new IOException(ex.getMessage());
		}

		try {
			final QSequenceLineCache baseLines = localResult.getLeftCache();
			final QSequenceLineCache localLines = localResult.getRightCache();
			final QSequenceLineCache latestLines = latestResult.getRightCache();
			final FSMergerBySequenceList local = new FSMergerBySequenceList(localResult.getBlocks());
			final FSMergerBySequenceList latest = new FSMergerBySequenceList(latestResult.getBlocks());
			final List transformedLocalLines = transformLocalLines(localResult.getBlocks(), localLines);

			int baseLineIndex = -1;
			boolean conflict = false;
			boolean merged = false;
			boolean chooseOnlyConflicts = style == SVNDiffConflictChoiceStyle.CHOOSE_ONLY_CONFLICTS;
			OutputStream realResult = result;
			if (chooseOnlyConflicts) {
			    result = SVNFileUtil.DUMMY_OUT;
			}
			
			while (local.hasCurrent() || latest.hasCurrent()) {
				if (local.hasCurrent() && latest.hasCurrent() && isEqualChange(local.current(), latest.current(), localLines, latestLines)) {
				    baseLineIndex = appendLines(result, local.current(), localLines, baseLineIndex, transformedLocalLines);
					local.forward();
					latest.forward();
					continue;
				}

				if (local.hasCurrent() && latest.hasCurrent()) {
					final QSequenceDifferenceBlock localStartBlock = local.current();
					final QSequenceDifferenceBlock latestStartBlock = latest.current();
					if (checkConflict(local, latest, localLines, latestLines, baseLines.getLineCount())) {
						if (style == SVNDiffConflictChoiceStyle.CHOOSE_LATEST) {
                            baseLineIndex = createConflict(result, localStartBlock, local.current(), latestStartBlock, latest.current(), 
                                    localLines, latestLines, baseLineIndex, transformedLocalLines, style);
		                    local.forward();
		                    latest.forward();
		                    merged = true;
						} else if (style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED) {
                            baseLineIndex = createConflict(result, localStartBlock, local.current(), latestStartBlock, latest.current(), 
                                    localLines, latestLines, baseLineIndex, transformedLocalLines, style);
		                    local.forward();
		                    latest.forward();
		                    merged = true;
						} else if (style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST) {
						    baseLineIndex = createConflict(result, localStartBlock, local.current(), latestStartBlock, latest.current(), 
						            localLines, latestLines, baseLineIndex, transformedLocalLines, style);
	                        local.forward();
	                        latest.forward();
	                        conflict = true;
						} else if (style == SVNDiffConflictChoiceStyle.CHOOSE_ONLY_CONFLICTS) {
                            baseLineIndex = createOnlyConflictWithContext(realResult, localStartBlock, local.current(), latestStartBlock, latest.current(), 
                                    localLines, latestLines, baseLines);
                            local.forward();
                            latest.forward();
                            conflict = true;
						}
						continue;
					}
				}

				if (local.hasCurrent() && isBefore(local.current(), latest.hasCurrent() ? latest.current() : null)) {
				    baseLineIndex = appendLines(result, local.current(), localLines, baseLineIndex, transformedLocalLines);
					local.forward();
					merged = true;
					continue;
				}

				if (latest.hasCurrent()) {
				    baseLineIndex = appendLines(result, latest.current(), latestLines, baseLineIndex, transformedLocalLines);
					latest.forward();
					merged = true;
				}
			}

			appendTransformedLocalLines(baseLineIndex, baseLines.getLineCount(), transformedLocalLines, result);

			if (conflict) {
				return CONFLICTED;
			}
			else if (merged) {
				return MERGED;
			}
			else {
				return NOT_MODIFIED;
			}
		}
		finally {
			latestResult.close();
			localResult.close();
		}
	}

	// Utils ==================================================================

	private List transformLocalLines(List blocks, QSequenceLineCache localLines) throws IOException {
		final List transformedLocalLines = new ArrayList();
		final FSMergerBySequenceList blockList = new FSMergerBySequenceList(blocks);

		int localIndex = 0;
		int baseIndex = 0;

		for (;localIndex < localLines.getLineCount();) {
			final int baseTo;
			if (blockList.hasCurrent()) {
				final QSequenceDifferenceBlock block = blockList.current();
				baseTo = block.getLeftFrom() -1;
			}
			else {
				baseTo = Integer.MAX_VALUE;
			}

			while (localIndex < localLines.getLineCount() && baseIndex <= baseTo) {
				transformedLocalLines.add(localLines.getLine(localIndex));
				localIndex++;
				baseIndex++;
			}

			if (blockList.hasCurrent()) {
				for (int index = 0; index < blockList.current().getLeftSize(); index++) {
					transformedLocalLines.add(null);
				}

				baseIndex += blockList.current().getLeftSize();
				localIndex += blockList.current().getRightSize();

				blockList.forward();
			}
		}

		return transformedLocalLines;
	}

	private boolean isBefore(QSequenceDifferenceBlock block1,
	                         QSequenceDifferenceBlock block2) {
		return block1 != null && (block2 == null || block1.getLeftTo() < block2.getLeftFrom());
	}

	private boolean intersect(QSequenceDifferenceBlock block1,
	                          QSequenceDifferenceBlock block2, int baseLineCount) {
		final int from1 = block1.getLeftFrom();
		final int from2 = block2.getLeftFrom();
		final int to1 = block1.getLeftTo();
		final int to2 = block2.getLeftTo();

		if (to1 < from1) {
			if (to2 < from2) {
				return from1 == from2;
			}
			if (from1 == baseLineCount && to2 >= baseLineCount - 1) {
				return true;
			}
			return from1 >= from2 && from1 <= to2;
		}
		else if (to2 < from2) {
			if (from2 == baseLineCount && to1 >= baseLineCount - 1) {
				return true;
			}
			return from2 >= from1 && from2 <= to1;
		}
		else {
			return (from1 >= from2 && from1 <= to2) || (from2 >= from1 && from2 <= to1);
		}
	}

	private int appendLines(OutputStream result,
	                        QSequenceDifferenceBlock block, QSequenceLineCache changedLines,
	                        int baseLineIndex, List transformedLocalLines) throws IOException {
		appendTransformedLocalLines(baseLineIndex, block.getLeftFrom(), transformedLocalLines, result);

		for (int changedLineIndex = block.getRightFrom(); changedLineIndex <= block.getRightTo(); changedLineIndex++) {
			writeLine(result, changedLines.getLine(changedLineIndex));
		}

		return block.getLeftTo();
	}

	private boolean isEqualChange(QSequenceDifferenceBlock localBlock,
	                              QSequenceDifferenceBlock latestBlock,
	                              QSequenceLineCache localLines, QSequenceLineCache latestLines)
		 throws IOException {
		if (localBlock.getLeftFrom() != latestBlock.getLeftFrom() || localBlock.getLeftTo() != latestBlock.getLeftTo()) {
			return false;
		}

		if (localBlock.getRightTo() - localBlock.getRightFrom() != latestBlock.getRightTo() - latestBlock.getRightFrom()) {
			return false;
		}

		for (int index = 0; index < localBlock.getRightTo() - localBlock.getRightFrom() + 1; index++) {
			final QSequenceLine localLine = localLines.getLine(localBlock.getRightFrom() + index);
			final QSequenceLine latestLine = latestLines.getLine(latestBlock.getRightFrom() + index);
			if (!localLine.equals(latestLine)) {
				return false;
			}
		}

		return true;
	}

	private boolean checkConflict(FSMergerBySequenceList localChanges,
	                              FSMergerBySequenceList latestChanges,
	                              QSequenceLineCache localLines, QSequenceLineCache latestLines, int baseLineCount)
		 throws IOException {
		boolean conflict = false;
		while (intersect(localChanges.current(), latestChanges.current(), baseLineCount) && !isEqualChange(localChanges.current(), latestChanges.current(), localLines, latestLines)) {
			conflict = true;

			if (localChanges.current().getLeftTo() <= latestChanges.current().getLeftTo()) {
				if (localChanges.hasNext() && intersect(localChanges.peekNext(), latestChanges.current(), baseLineCount)) {
					localChanges.forward();
				}
				else {
					break;
				}
			}
			else {
				if (latestChanges.hasNext() && intersect(localChanges.current(), latestChanges.peekNext(), baseLineCount)) {
					latestChanges.forward();
				}
				else {
					break;
				}
			}
		}
		return conflict;
	}

	private int createConflict(OutputStream result,
	                           QSequenceDifferenceBlock localStart,
	                           QSequenceDifferenceBlock localEnd,
	                           QSequenceDifferenceBlock latestStart,
	                           QSequenceDifferenceBlock latestEnd,
	                           QSequenceLineCache localLines, QSequenceLineCache latestLines,
	                           int baseLineIndex, List transformedLocalLines, 
	                           SVNDiffConflictChoiceStyle style) throws IOException {
		final int minBaseFrom = Math.min(localStart.getLeftFrom(), latestStart.getLeftFrom());
		final int maxBaseTo = Math.max(localEnd.getLeftTo(), latestEnd.getLeftTo());

		appendTransformedLocalLines(baseLineIndex, minBaseFrom, transformedLocalLines, result);

		final int localFrom = Math.max(0, localStart.getRightFrom() - (localStart.getLeftFrom() - minBaseFrom));
		final int localTo = Math.min(localLines.getLineCount() - 1, localEnd.getRightTo() + (maxBaseTo - localEnd.getLeftTo()));
		final int latestFrom = Math.max(0, latestStart.getRightFrom() - (latestStart.getLeftFrom() - minBaseFrom));
		final int latestTo = Math.min(latestLines.getLineCount() - 1, latestEnd.getRightTo() + (maxBaseTo - latestEnd.getLeftTo()));

		if (style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST) {
	        writeBytesAndEol(result, myConflictStart);
		}

		if (style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED || style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST) {
	        for (int index = localFrom; index <= localTo; index++) {
	            writeLine(result, localLines.getLine(index));
	        }
		}
		
		if (style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST) {
	        writeBytesAndEol(result, myConflictSeparator);
		}

		if (style == SVNDiffConflictChoiceStyle.CHOOSE_LATEST || style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST) {
	        for (int index = latestFrom; index <= latestTo; index++) {
	            writeLine(result, latestLines.getLine(index));
	        }
		}
		
		if (style == SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST) {
	        writeBytesAndEol(result, myConflictEnd);
		}

		return maxBaseTo;
	}

	   private int createOnlyConflictWithContext(OutputStream result, QSequenceDifferenceBlock localStart, QSequenceDifferenceBlock localEnd, 
	           QSequenceDifferenceBlock latestStart, QSequenceDifferenceBlock latestEnd, QSequenceLineCache localLines, QSequenceLineCache latestLines, 
	           QSequenceLineCache baseLines) throws IOException {

	       final int minBaseFrom = Math.min(localStart.getLeftFrom(), latestStart.getLeftFrom());
	       final int maxBaseTo = Math.max(localEnd.getLeftTo(), latestEnd.getLeftTo());
	       final int localFrom = Math.max(0, localStart.getRightFrom() - (localStart.getLeftFrom() - minBaseFrom));
	       final int localTo = Math.min(localLines.getLineCount() - 1, localEnd.getRightTo() + (maxBaseTo - localEnd.getLeftTo()));
	       final int latestFrom = Math.max(0, latestStart.getRightFrom() - (latestStart.getLeftFrom() - minBaseFrom));
	       final int latestTo = Math.min(latestLines.getLineCount() - 1, latestEnd.getRightTo() + (maxBaseTo - latestEnd.getLeftTo()));

	       //local changes block
	       writeBytes(result, myConflictStart);

	       int localLinesNum = localTo - localFrom + 1;
           String localContext = null;
	       int localStartFrom = localFrom + 1;
           if (localLinesNum > 1) {
	           localContext = " (" + localStartFrom + "," + localLinesNum + ")"; 
           } else {
               localContext = " (" + localStartFrom + ")"; 
           }

	       writeBytesAndEol(result, localContext.getBytes());
	       
	       for (int index = localFrom; index <= localTo; index++) {
	           writeLine(result, localLines.getLine(index));
	       }

	       //original block
           writeBytes(result, myOriginalMarker);
           int originalLinesNum = maxBaseTo - minBaseFrom + 1;
           String originalContext = null;
           int originalStartFrom = minBaseFrom + 1;
           if (originalLinesNum > 1) {
               originalContext = " (" + originalStartFrom + "," + originalLinesNum + ")"; 
           } else {
               originalContext = " (" + originalStartFrom + ")"; 
           }
           writeBytesAndEol(result, originalContext.getBytes());
           
           for (int index = minBaseFrom; index <= maxBaseTo; index++) {
               writeLine(result, baseLines.getLine(index));
           }
	       
	       //conflict separator
	       writeBytesAndEol(result, myConflictSeparator);

	       //server changes block
	       for (int index = latestFrom; index <= latestTo; index++) {
	           writeLine(result, latestLines.getLine(index));
	       }

	       writeBytes(result, myConflictEnd);

	       int latestLinesNum = latestTo - latestFrom + 1;
           String latestContext = null;
           int latestStartFrom = latestFrom + 1;
           if (latestLinesNum > 1) {
               latestContext = " (" + latestStartFrom + "," + latestLinesNum + ")"; 
           } else {
               latestContext = " (" + latestStartFrom + ")"; 
           }

           writeBytesAndEol(result, latestContext.getBytes());

	       return maxBaseTo;

	   }

	private void appendTransformedLocalLines(int baseLineIndex, int to, List transformedLocalLines, OutputStream result) throws IOException {
		for (baseLineIndex++; baseLineIndex < to; baseLineIndex++) {
			final QSequenceLine sequenceLine = (QSequenceLine)transformedLocalLines.get(baseLineIndex);
			if (sequenceLine == null) {
				throw new IOException("Can not merge: sequence line is null for this base index");
			}
			writeLine(result, sequenceLine);
		}
	}

	private void writeLine(OutputStream os, QSequenceLine line) throws IOException {
		final byte[] bytes = line.getContentBytes();
		if (bytes.length == 0) {
			return;
		}

		os.write(bytes);
	}

	private void writeBytesAndEol(OutputStream os, final byte[] bytes)
		 throws IOException {
		if (bytes.length > 0) {
			os.write(bytes);
			os.write(DEFAULT_EOL.getBytes());
		}
	}


	private void writeBytes(OutputStream os, final byte[] bytes) throws IOException {
	    if (bytes.length > 0) {
	        os.write(bytes);
	    }
	}

	private QSequenceLineTeeSimplifier createSimplifier(SVNDiffOptions options) {
		final QSequenceLineSimplifier eolSimplifier = options != null && options.isIgnoreEOLStyle() ?
			 (QSequenceLineSimplifier)new QSequenceLineEOLUnifyingSimplifier() :
			 (QSequenceLineSimplifier)new QSequenceLineDummySimplifier();

		QSequenceLineSimplifier spaceSimplifier = new QSequenceLineDummySimplifier();
		if (options != null) {
			if (options.isIgnoreAllWhitespace()) {
				spaceSimplifier = new QSequenceLineWhiteSpaceSkippingSimplifier();
			}
			else if (options.isIgnoreAmountOfWhitespace()) {
				spaceSimplifier = new QSequenceLineWhiteSpaceReducingSimplifier();
			}
		}
		return new QSequenceLineTeeSimplifier(eolSimplifier, spaceSimplifier);
	}
}
