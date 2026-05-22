package gui

import service.GameManager
import service.PlayerStats
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel

class StatsPanel(private val gameManager: GameManager) : JPanel() {

    private val tableModel = DefaultTableModel(
        arrayOf("ID", "Игрок", "Игр", "Побед", "% побед", "Попаданий", "Кораблей"), 0
    )
    private val statsTable = JTable(tableModel)
    private val refreshButton = JButton("🔄 Обновить")

    init {
        layout = BorderLayout()
        border = EmptyBorder(5, 5, 5, 5)

        statsTable.font = Font("Monospaced", Font.PLAIN, 11)
        statsTable.rowHeight = 20
        statsTable.getTableHeader().reorderingAllowed = false

        val scrollPane = JScrollPane(statsTable)
        scrollPane.preferredSize = java.awt.Dimension(280, 300)

        refreshButton.addActionListener { refresh() }

        add(scrollPane, BorderLayout.CENTER)
        add(refreshButton, BorderLayout.SOUTH)

        refresh()
    }

    fun refresh() {
        tableModel.setRowCount(0)

        val players = gameManager.getAllPlayers()
        val allStats = mutableListOf<PlayerStats>()

        // Собираем статистику из GameManager (если есть)
        players.forEach { player ->
            val stats = calculateStats(player.id)
            allStats.add(stats)
        }

        // Сортируем по количеству побед
        allStats.sortedByDescending { it.gamesWon }.forEach { stats ->
            tableModel.addRow(arrayOf(
                stats.playerId,
                stats.playerName,
                stats.gamesPlayed,
                stats.gamesWon,
                "${(stats.winRate * 100).toInt()}%",
                stats.totalHits,
                stats.shipsSunk
            ))
        }
    }

    private fun calculateStats(playerId: Int): PlayerStats {
        val game = gameManager.getCurrentGame()
        var gamesPlayed = 0
        var gamesWon = 0
        var totalHits = 0
        var shipsSunk = 0

        // Если есть активная игра и игрок в ней участвует
        if (game != null) {
            if (game.player1.id == playerId || game.player2.id == playerId) {
                val stats = gameManager.getGameStats()
                if (playerId == stats.player1Id) {
                    totalHits = stats.player1Hits
                } else {
                    totalHits = stats.player2Hits
                }
                gamesPlayed = 1
            }
        }

        val player = gameManager.getPlayerById(playerId)
        return PlayerStats(
            playerId = playerId,
            playerName = player?.name ?: "?",
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            totalHits = totalHits,
            shipsSunk = shipsSunk
        )
    }
}
