package service

import models.Player
import java.io.File

class PlayerRegistry(private val filePath: String = "players.json") {

    private val players = mutableMapOf<Int, Player>()

    init {
        load()
    }

    fun save(player: Player) {
        players[player.id] = player
        saveToFile()
    }

    fun getAll(): List<Player> = players.values.toList()

    fun getById(id: Int): Player? = players[id]

    fun delete(id: Int): Boolean = players.remove(id) != null

    fun clear() {
        players.clear()
        saveToFile()
    }

    private fun saveToFile() {
        val content = players.values.joinToString("\n") { "${it.id}|${it.name}" }
        File(filePath).writeText(content)
    }

    private fun load() {
        val file = File(filePath)
        if (!file.exists()) return

        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split("|")
            if (parts.size == 2) {
                val id = parts[0].toInt()
                val name = parts[1]
                players[id] = Player(id, name)
            }
        }
    }
}
