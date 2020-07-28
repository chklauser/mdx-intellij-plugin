package org.intellij.plugin.mdx.lang.parse

import org.intellij.markdown.IElementType
import org.intellij.markdown.parser.LookaheadText
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkdownParserUtil
import org.intellij.markdown.parser.markerblocks.MarkerBlock
import org.intellij.markdown.parser.markerblocks.MarkerBlockImpl
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import kotlin.text.Regex

/**
 * It is a copy-paste, only changed HTML to JSX
 *
 * @see org.intellij.markdown.parser.markerblocks.impl.HtmlBlockMarkerBlock
 */

class JsxBlockMarkerBlock(myConstraints: MarkdownConstraints,
                           private val productionHolder: ProductionHolder,
                           private val endCheckingRegex: Regex?,
                           startPosition: LookaheadText.Position)
    : MarkerBlockImpl(myConstraints, productionHolder.mark()) {
    init {
        productionHolder.addProduction(listOf(SequentialParser.Node(
                startPosition.offset..startPosition.nextLineOrEofOffset, MdxTokenTypes.JSX_BLOCK_CONTENT)))
    }

    override fun allowsSubBlocks(): Boolean = false

    override fun isInterestingOffset(pos: LookaheadText.Position): Boolean = true

    override fun getDefaultAction(): MarkerBlock.ClosingAction {
        return MarkerBlock.ClosingAction.DONE
    }

    override fun doProcessToken(pos: LookaheadText.Position, currentConstraints: MarkdownConstraints): MarkerBlock.ProcessingResult {
        if (pos.offsetInCurrentLine != -1) {
            return MarkerBlock.ProcessingResult.CANCEL
        }


        val prevLine = pos.prevLine ?: return MarkerBlock.ProcessingResult.DEFAULT
        if (!MarkdownConstraints.fillFromPrevious(pos, constraints).extendsPrev(constraints)) {
            return MarkerBlock.ProcessingResult.DEFAULT
        }

        if (endCheckingRegex == null && MarkdownParserUtil.calcNumberOfConsequentEols(pos, constraints) >= 2) {
            return MarkerBlock.ProcessingResult.DEFAULT
        } else if (endCheckingRegex != null && endCheckingRegex.find(prevLine) != null) {
            return MarkerBlock.ProcessingResult.DEFAULT
        }

        if (pos.currentLine.isNotEmpty()) {
            productionHolder.addProduction(listOf(SequentialParser.Node(
                    pos.offset + 1 + constraints.getCharsEaten(pos.currentLine)..pos.nextLineOrEofOffset,
                    MdxTokenTypes.JSX_BLOCK_CONTENT)))
        }


        return MarkerBlock.ProcessingResult.CANCEL
    }

    override fun calcNextInterestingOffset(pos: LookaheadText.Position): Int {
        return pos.nextLineOrEofOffset
    }

    override fun getDefaultNodeType(): IElementType {
        return MdxElementTypes.JSX_BLOCK
    }
}