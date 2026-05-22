package test

import models.MoveResult
import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.*

class IntegrationTest {
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
    fun `full game flow with two players`() {
        // 1. Добавляем игроков
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        assertEquals(2, gameManager.getAllPlayers().size)

        // 2. Создаём игру
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)
        assertEquals(p1.id, game?.currentPlayer?.id)

        // 3. Расставляем минимальные корабли (чтобы поместились)
        // Используем "right" направление и разные строки
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p1.id, 2, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 2, 0, 1, "right")

        // 4. Игровой процесс - p1 стреляет
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.HIT || result == MoveResult.KILL || result == MoveResult.MISS)

        // 5. Проверяем, что игра продолжается
        val currentGame = gameManager.getCurrentGame()
        assertNotNull(currentGame)
    }

    @Test
    fun `ship placement constraints integration`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим первый корабль
        val firstResult = gameManager.placeShip(p1.id, 0, 0, 3, "right")
        assertEquals(ShipPlacementResult.SUCCESS, firstResult)

        // Пытаемся поставить корабль вплотную (должно быть TOO_CLOSE)
        val closeResult = gameManager.placeShip(p1.id, 0, 3, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, closeResult)

        // Ставим с зазором (должно быть SUCCESS)
        val successResult = gameManager.placeShip(p1.id, 2, 0, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, successResult)
    }

    @Test
    fun `complete battle with win detection`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем по одному кораблю для каждого
        val placeResult1 = gameManager.placeShip(p1.id, 0, 0, 1, "right")
        val placeResult2 = gameManager.placeShip(p2.id, 0, 0, 1, "right")
        assertEquals(ShipPlacementResult.SUCCESS, placeResult1)
        assertEquals(ShipPlacementResult.SUCCESS, placeResult2)

        // p1 стреляет и попадает (уничтожает корабль p2)
        val result = gameManager.makeMove(p1.id, 0, 0)
        println("First move result: $result")

        // Так как корабль p2 уничтожен, makeMove возвращает KILL
        assertEquals(MoveResult.KILL, result, "Expected KILL for last ship")

        // Проверяем победителя (должен быть установлен в GameState)
        val game = gameManager.getCurrentGame()
        assertNotNull(game, "Game should not be null")
        assertNotNull(game?.winner, "Winner should be set after game over")
        assertEquals(p1.id, game?.winner?.id, "Winner should be p1")
    }

    @Test
    fun `multiple games in sequence`() {
        // Первая игра
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        gameManager.createGame(p1.id, p2.id)
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.makeMove(p1.id, 0, 0)

        // Завершаем игру
        gameManager.finishGame()
        assertNull(gameManager.getCurrentGame())

        // Вторая игра с новыми игроками
        val p3 = gameManager.addPlayer("Светлана")
        val p4 = gameManager.addPlayer("Дмитрий")

        gameManager.createGame(p3.id, p4.id)
        assertNotNull(gameManager.getCurrentGame())
        assertEquals(p3.id, gameManager.getCurrentGame()?.currentPlayer?.id)

        // Проверяем, что старые игроки остались
        assertEquals(4, gameManager.getAllPlayers().size)
    }

    @Test
    fun `board factory integration`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем корабли
        gameManager.placeShip(p1.id, 0, 0, 2, "right")

        val game = gameManager.getCurrentGame()
        val originalBoard = game?.board1

        // Создаём скрытую доску
        val hiddenBoard = factory.getHiddenBoard(originalBoard!!)

        // Проверяем, что корабли скрыты
        assertEquals('~', hiddenBoard[0][0])
        assertEquals('~', hiddenBoard[0][1])

        // Создаём копию доски
        val copyBoard = factory.copyBoard(originalBoard)
        assertTrue(factory.boardsAreEqual(originalBoard, copyBoard))

        // Меняем оригинал - копия не должна измениться
        originalBoard[0][0] = 'X'
        assertFalse(factory.boardsAreEqual(originalBoard, copyBoard))
    }

    @Test
    fun `player management integration`() {
        // Добавляем игроков с одинаковыми именами
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Анна")
        val p3 = gameManager.addPlayer("Анна")

        assertEquals(1, p1.id)
        assertEquals(2, p2.id)
        assertEquals(3, p3.id)

        // Все должны быть в списке
        val players = gameManager.getAllPlayers()
        assertEquals(3, players.size)

        // Проверяем, что можно создать игру между игроками с одинаковыми именами
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)
        assertEquals(p1.id, game?.player1?.id)
        assertEquals(p2.id, game?.player2?.id)
    }

    @Test
    fun `hit and kill detection integration`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим двухпалубный корабль
        gameManager.placeShip(p2.id, 0, 0, 2, "right")

        // Первое попадание - HIT
        var result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, result)

        // Второе попадание - KILL
        result = gameManager.makeMove(p1.id, 0, 1)
        assertEquals(MoveResult.KILL, result)

        // Проверяем, что корабли потоплены
        val game = gameManager.getCurrentGame()
        val board = game?.board2
        assertEquals('X', board?.get(0)?.get(0))
        assertEquals('X', board?.get(0)?.get(1))
    }

    @Test
    fun `turn stays with player after hit`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим двухпалубный корабль для p2
        gameManager.placeShip(p2.id, 0, 0, 2, "right")

        // p1 стреляет - попадание
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, result)

        // Ход должен остаться у p1
        val game = gameManager.getCurrentGame()
        assertEquals(p1.id, game?.currentPlayer?.id)
    }

    @Test
    fun `turn switches to opponent after miss`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим корабли для обоих
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        // p1 стреляет в пустую клетку - промах
        val result = gameManager.makeMove(p1.id, 9, 9)
        assertEquals(MoveResult.MISS, result)

        // Ход должен перейти к p2
        val game = gameManager.getCurrentGame()
        assertEquals(p2.id, game?.currentPlayer?.id)
    }

    @Test
    fun `turn stays with player after kill`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим однопалубный корабль для p2
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        // Ставим корабль для p1, чтобы игра не закончилась
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        // p1 стреляет и топит
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.KILL, result)

        // Ход должен остаться у p1 (так как он попал)
        val game = gameManager.getCurrentGame()
        assertEquals(p1.id, game?.currentPlayer?.id)
    }
}
