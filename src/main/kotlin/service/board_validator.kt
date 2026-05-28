package service

import models.ShipPlacementResult

class BoardValidator {
    companion object {
        const val SIZE = 10
    }

    fun canPlaceShip(
        board: Array<Array<Char>>,
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): ShipPlacementResult {
        val validDirections = setOf("up", "down", "left", "right")
        if (direction.lowercase() !in validDirections) {
            return ShipPlacementResult.INVALID_DIRECTION
        }

        val cells = getShipCells(startRow, startCol, length, direction)
        if (cells.isEmpty()) return ShipPlacementResult.OUT_OF_BOUNDS

        for ((r, c) in cells) {
            if (!isInBounds(r, c)) return ShipPlacementResult.OUT_OF_BOUNDS
            if (board[r][c] != '~') return ShipPlacementResult.OVERLAP
        }

        for ((r, c) in cells) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (isInBounds(nr, nc) && board[nr][nc] != '~' && (nr to nc) !in cells) {
                        return ShipPlacementResult.TOO_CLOSE
                    }
                }
            }
        }

        return ShipPlacementResult.SUCCESS
    }

    fun placeShip(
        board: Array<Array<Char>>,
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): Boolean {
        val cells = getShipCells(startRow, startCol, length, direction)
        for ((r, c) in cells) {
            board[r][c] = '■'
        }
        return true
    }

    fun isInBounds(row: Int, col: Int): Boolean = row in 0 until SIZE && col in 0 until SIZE

    private fun getShipCells(
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        repeat(length) { step ->
            val (r, c) = when (direction.lowercase()) {
                "up" -> startRow - step to startCol
                "down" -> startRow + step to startCol
                "left" -> startRow to startCol - step
                "right" -> startRow to startCol + step
                else -> return emptyList()
            }
            if (!isInBounds(r, c)) return emptyList()
            cells.add(r to c)
        }
        return cells
    }
}
