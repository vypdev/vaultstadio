package com.vaultstadio.core.domain.service

import com.vaultstadio.core.domain.model.DiffOperation
import com.vaultstadio.core.domain.model.DiffPatch

/** Preserve the existing line matching policy; content access and authorization belong elsewhere. */
internal object VersionLineDiff {
    private data class Position(val from: Int, val to: Int)

    fun compare(from: List<String>, to: List<String>): List<DiffPatch> {
        val patches = mutableListOf<DiffPatch>()
        var position = Position(0, 0)
        while (position.from < from.size && position.to < to.size) {
            position = if (from[position.from] == to[position.to]) {
                Position(position.from + 1, position.to + 1)
            } else {
                advanceDifference(from, to, position, patches)
            }
        }
        deleteLines(from, position.from, from.size, patches)
        addLines(to, position.to, to.size, patches)
        return patches
    }

    private fun advanceDifference(
        from: List<String>,
        to: List<String>,
        position: Position,
        patches: MutableList<DiffPatch>,
    ): Position {
        val toOffset = from.subList(position.from, from.size).indexOf(to[position.to])
        val fromOffset = to.subList(position.to, to.size).indexOf(from[position.from])
        return when {
            toOffset == -1 && fromOffset == -1 -> {
                patches.add(
                    DiffPatch(
                        DiffOperation.MODIFY,
                        position.from + 1,
                        position.from + 1,
                        from[position.from],
                        to[position.to],
                    ),
                )
                Position(position.from + 1, position.to + 1)
            }
            toOffset != -1 && (fromOffset == -1 || toOffset <= fromOffset) -> {
                deleteLines(from, position.from, position.from + toOffset, patches)
                Position(position.from + toOffset, position.to)
            }
            else -> {
                addLines(to, position.to, position.to + fromOffset, patches)
                Position(position.from, position.to + fromOffset)
            }
        }
    }

    private fun addLines(lines: List<String>, start: Int, end: Int, patches: MutableList<DiffPatch>) {
        for (index in start until end) {
            patches.add(DiffPatch(DiffOperation.ADD, index + 1, index + 1, newContent = lines[index]))
        }
    }

    private fun deleteLines(lines: List<String>, start: Int, end: Int, patches: MutableList<DiffPatch>) {
        for (index in start until end) {
            patches.add(DiffPatch(DiffOperation.DELETE, index + 1, index + 1, oldContent = lines[index]))
        }
    }
}
