package org.intellij.plugin.mdx.lang.parse

import org.intellij.markdown.parser.LookaheadText
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlock
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.sequentialparsers.SequentialParser

class JsxBlockProvider : MarkerBlockProvider<MarkerProcessor.StateInfo> {
    override fun createMarkerBlocks(pos: LookaheadText.Position, productionHolder: ProductionHolder, stateInfo: MarkerProcessor.StateInfo): List<MarkerBlock> {
        val matchingGroup = matches(pos, stateInfo.currentConstraints)
        if (matchingGroup != -1) {
            if (matchingGroup == IMPORT_EXPORT_ONE_LINE_CONST) {
                return listOf(JsxBlockMarkerBlock(stateInfo.currentConstraints, productionHolder, null, pos, false))
            }
            if (matchingGroup == IMPORT_EXPORT_MULTILINE_CONST) {
                productionHolder.addProduction(listOf(SequentialParser.Node(
                        pos.offset..pos.nextLineOrEofOffset, MdxTokenTypes.JSX_BLOCK_CONTENT)))
                return listOf(JsxBlockMarkerBlock(stateInfo.currentConstraints, productionHolder, END_REGEX, pos, true))
            }
            return listOf(JsxBlockMarkerBlock(stateInfo.currentConstraints, productionHolder, OPEN_CLOSE_REGEXES[matchingGroup].second, pos, false))
        }
        return emptyList()
    }

    override fun interruptsParagraph(pos: LookaheadText.Position, constraints: MarkdownConstraints): Boolean {
        return matches(pos, constraints) in 0..5
    }

    private fun matches(pos: LookaheadText.Position, constraints: MarkdownConstraints): Int {
        if (!MarkerBlockProvider.isStartOfLineWithConstraints(pos, constraints)) {
            return -1
        }
        val text = pos.currentLineFromPosition
        val offset = MarkerBlockProvider.passSmallIndent(text)
        if (offset >= text.length){
            return -1
        }
        if (text[offset] != '<') {
            if (IMPORT_EXPORT_REGEX.matches(text.substring(offset))) {
                return IMPORT_EXPORT_ONE_LINE_CONST
            } else {
                if (FIND_START_IMPORT_EXPORT.matches(text.substring(offset))) {
                    return IMPORT_EXPORT_MULTILINE_CONST
                }
                return -1;
            }
        }
        val matchResult = FIND_START_REGEX.find(text.substring(offset))
                ?: return -1
        assert(matchResult.groups.size == OPEN_CLOSE_REGEXES.size + 2) { "There are some excess capturing groups probably!" }
        for (i in 0..OPEN_CLOSE_REGEXES.size - 1) {
            if (matchResult.groups[i + 2] != null) {
                return i
            }
        }
        assert(false) { "Match found but all groups are empty!" }
        return -1
    }

    companion object {
        val IMPORT_EXPORT_ONE_LINE_CONST = 6;

        val IMPORT_EXPORT_MULTILINE_CONST = 7

        val TAG_NAMES =
                "address, article, aside, base, basefont, blockquote, body, caption, center, col, colgroup, dd, details, " +
                        "dialog, dir, div, dl, dt, fieldset, figcaption, figure, footer, form, frame, frameset, h1, " +
                        "head, header, hr, html, legend, li, link, main, menu, menuitem, meta, nav, noframes, ol, " +
                        "optgroup, option, p, param, pre, section, source, title, summary, table, tbody, td, tfoot, " +
                        "th, thead, title, tr, track, ul"

        val PATH_STRING = "(([a-zA-Z0-9_\\d]+(\\.[a-z]+)?|\\.|\\.\\.)/)*[a-zA-Z0-9_\\d]+(\\.[a-z]+)?/?"

        val OBJECT_NAME = "[a-zA-Z][a-zA-Z0-9\\d]+"

        val OBJECT_TO_IMPORT = "($OBJECT_NAME)|(\\{($OBJECT_NAME,\\s*)*$OBJECT_NAME\\})"

        val OBJECTS_TO_IMPORT = "($OBJECT_TO_IMPORT,\\s*)*$OBJECT_TO_IMPORT"

        val IMPORT_KEYWORD = "(^|\\s+)import($|\\s+|\\{)"

        val EXPORT_KEYWORD = "(^|\\s+)export($|\\s+)"

        val JSX_IMPORTS = "import\\s+($OBJECTS_TO_IMPORT)\\s+from\\s+\\'$PATH_STRING\\';?"

//        val JSX_EXPORTS = "export const $OBJECT_NAME = .+"

        val TAG_NAME = "[A-Z][a-zA-Z0-9-]*"

        val ATTR_NAME = "[A-Za-z:_][A-Za-z0-9_.:-]*"

        val ATTR_VALUE = "\\s*=\\s*(?:[^ \"'=<>`]+|'[^']*'|\"[^\"]*\")"

        val ATTRIBUTE = "\\s+$ATTR_NAME(?:$ATTR_VALUE)?"

        val OPEN_TAG = "<$TAG_NAME(?:$ATTRIBUTE)*\\s*/?>"

        /**
         * Closing tag allowance is not in public spec version yet
         */
        val CLOSE_TAG = "</$TAG_NAME\\s*>"

        /** see {@link http://spec.commonmark.org/0.21/#html-blocks}
         *
         * nulls mean "Next line should be blank"
         * */

        val IMPORT_EXPORT_REGEX = Regex("($JSX_IMPORTS)")
        val OPEN_CLOSE_REGEXES: List<Pair<Regex, Regex?>> = listOf(
                Pair(Regex("<(?i:script|pre|style)(?: |>|$)"), Regex("</(?i:script|style|pre)>")),
                Pair(Regex("</?(?i:${TAG_NAMES.replace(", ", "|")})(?: |/?>|$)"), null),
                Pair(Regex("(?:$OPEN_TAG|$CLOSE_TAG)(?: *|$)"), null)
        )
        val FIND_START_IMPORT_EXPORT = Regex("($IMPORT_KEYWORD|$EXPORT_KEYWORD).*")

        val FIND_START_REGEX = Regex(
                "\\A(${OPEN_CLOSE_REGEXES.joinToString(separator = "|", transform = { "(${it.first.pattern})" })})"
        )

        val END_REGEX = Regex("($IMPORT_KEYWORD|(^$)|${FIND_START_REGEX.pattern})")

    }
}