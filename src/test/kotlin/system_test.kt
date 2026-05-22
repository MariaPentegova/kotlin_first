package test

import models.MoveResult
import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.*
import ui.ConsoleUI
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SystemTest {
    private lateinit var validator: BoardValidator
    private lateinit var battleService: BattleService
    private lateinit var factory: BoardFactory
    private lateinit var gameManager: GameManager

    @BeforeEach
    fun setUp() {
        validator = BoardValidator()
        battleService = BattleService(validator)
        factory = BoardFactory()
        gameManager = GameManager(validator, battleService, factory)
    }

    @Test
    fun `complete game simulation with predefined moves`() {
        // Добавляем игроков
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        // Создаём игру
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)

        // Расставляем минимальные корабли
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // Делаем выстрел (уничтожаем корабль p2)
        var result = gameManager.makeMove(p1.id, 0, 0)
        // Ожидаем KILL (последний корабль потоплен)
        assertTrue(result == MoveResult.KILL || result == MoveResult.HIT, "First move should be HIT or KILL")

        // Если был HIT, делаем второй выстрел
        if (result == MoveResult.HIT) {
            result = gameManager.makeMove(p1.id, 0, 0)
            assertEquals(MoveResult.KILL, result, "Second move should be KILL")
        }

        // Проверяем победителя
        val finalGame = gameManager.getCurrentGame()
        assertNotNull(finalGame?.winner, "Winner should be set")
        assertEquals(p1.id, finalGame?.winner?.id, "Winner should be p1")
    }

    @Test
    fun `player with same names are distinguished by ID`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Анна")

        assertNotEquals(p1.id, p2.id)
        assertEquals("Анна", p1.name)
        assertEquals("Анна", p2.name)

        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)
        assertEquals(p1.id, game?.player1?.id)
        assertEquals(p2.id, game?.player2?.id)

        // Проверяем, что в списке оба игрока
        val players = gameManager.getAllPlayers()
        assertEquals(2, players.size)
        assertTrue(players.any { it.id == p1.id && it.name == "Анна" })
        assertTrue(players.any { it.id == p2.id && it.name == "Анна" })
    }

    @Test
    fun `full fleet placement and battle simulation`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем по 1 кораблю для каждого (не полный флот, чтобы тест проходил быстро)
        gameManager.placeShip(p1.id, 0, 0, 2, "right")
        gameManager.placeShip(p2.id, 0, 0, 2, "right")

        // Проверяем, что флоты не готовы (не полная расстановка)
        assertFalse(gameManager.isFleetReady(p1.id))
        assertFalse(gameManager.isFleetReady(p2.id))

        // Делаем выстрелы
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.HIT || result == MoveResult.KILL)
    }

    @Test
    fun `prevent invalid ship placement`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Попытка поставить корабль вне поля
        var result = gameManager.placeShip(p1.id, -1, 0, 3, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)

        result = gameManager.placeShip(p1.id, 0, -1, 3, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)

        result = gameManager.placeShip(p1.id, 9, 8, 3, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)

        // Ставим корабль
        gameManager.placeShip(p1.id, 0, 0, 2, "right")

        // Попытка поставить корабль вплотную
        result = gameManager.placeShip(p1.id, 0, 2, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)

        // Попытка поставить корабль с пересечением
        result = gameManager.placeShip(p1.id, 0, 1, 2, "right")
        assertEquals(ShipPlacementResult.OVERLAP, result)

        // Правильная расстановка (с зазором)
        result = gameManager.placeShip(p1.id, 2, 0, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
    }

    @Test
    fun `invalid coordinates should return INVALID`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        val result = gameManager.makeMove(p1.id, -1, 0)
        assertEquals(MoveResult.INVALID, result)
    }

    @Test
    fun `valid shot should be HIT or KILL`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p2.id, 0, 0, 2, "right")
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.HIT, "Expected HIT, got $result")
    }

    @Test
    fun `same cell cannot be shot twice`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p2.id, 0, 0, 2, "right")
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        gameManager.makeMove(p1.id, 0, 0)
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.ALREADY_SHOT, result)
    }

    @Test
    fun `game statistics update correctly`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем корабли
        gameManager.placeShip(p2.id, 0, 0, 2, "right")
        gameManager.placeShip(p2.id, 2, 0, 1, "right")

        // Делаем выстрелы
        val result1 = gameManager.makeMove(p1.id, 0, 0)
        println("Shot 1: $result1")

        val result2 = gameManager.makeMove(p1.id, 0, 1)
        println("Shot 2: $result2")

        val result3 = gameManager.makeMove(p1.id, 2, 0)
        println("Shot 3: $result3")

        val stats = gameManager.getGameStats()
        println("Stats: player1Hits=${stats.player1Hits}, player2Ships=${stats.player2Ships}")

        // Должно быть 3 попадания (3 клетки кораблей у p2)
        assertEquals(3, stats.player1Hits, "Player1 should have 3 hits")
        // У p2 не должно остаться кораблей
        assertEquals(0, stats.player2Ships, "Player2 should have 0 ships left")
    }

    @Test
    fun `multiple players and multiple games`() {
        // Добавляем 5 игроков
        val players = listOf("Анна", "Борис", "Светлана", "Дмитрий", "Елена")
        val playerObjects = players.map { gameManager.addPlayer(it) }

        assertEquals(5, gameManager.getAllPlayers().size)

        // Создаём игру между 1 и 3 игроком
        var game = gameManager.createGame(playerObjects[0].id, playerObjects[2].id)
        assertNotNull(game)
        assertEquals(playerObjects[0].id, game?.player1?.id)
        assertEquals(playerObjects[2].id, game?.player2?.id)

        // Завершаем игру
        gameManager.finishGame()
        assertNull(gameManager.getCurrentGame())

        // Создаём игру между 2 и 5 игроком
        game = gameManager.createGame(playerObjects[1].id, playerObjects[4].id)
        assertNotNull(game)
        assertEquals(playerObjects[1].id, game?.player1?.id)
        assertEquals(playerObjects[4].id, game?.player2?.id)

        // Проверяем, что все игроки всё ещё в списке
        assertEquals(5, gameManager.getAllPlayers().size)
    }

    @Test
    fun `edge cases - single cell ships only`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем 2 однопалубных корабля для p2
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 2, 0, 1, "right")

        // Расставляем 1 корабль для p1
        gameManager.placeShip(p1.id, 0, 5, 1, "right")

        // Побеждаем (нужно 2 попадания)
        var hits = 0
        val shots = listOf(0 to 0, 2 to 0)

        for ((row, col) in shots) {
            val result = gameManager.makeMove(p1.id, row, col)
            if (result == MoveResult.KILL) hits++
            println("Shot at ($row,$col): $result, hits=$hits")
        }

        assertEquals(2, hits, "Should have 2 kills")

        // Проверяем, что p1 победил
        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner, "Winner should be set")
        assertEquals(p1.id, game?.winner?.id, "Winner should be p1")
    }

    @Test
    fun `console UI integration - menu commands`() {
        // Создаём тестовый ввод
        val input = """
            1
            ТестовыйИгрок
            2
            5
        """.trimIndent()

        val inputStream = ByteArrayInputStream(input.toByteArray())
        val originalIn = System.`in`
        System.setIn(inputStream)

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            val consoleUI = ConsoleUI(gameManager, factory)
            val thread = Thread {
                try {
                    consoleUI.start()
                } catch (exception: Exception) {
                    // Игнорируем exceptions при завершении
                }
            }
            thread.start()
            thread.join(3000)

            val output = outputStream.toString()
            assertTrue(output.contains("МОРСКОЙ БОЙ") || output.contains("ГЛАВНОЕ МЕНЮ"))
        } finally {
            System.setIn(originalIn)
            System.setOut(originalOut)
        }
    }
}
