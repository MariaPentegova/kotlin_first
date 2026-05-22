package test

import models.MoveResult
import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import service.*
import java.io.File

class IntegrationTest {

    private lateinit var validator: BoardValidator
    private lateinit var battleService: BattleService
    private lateinit var factory: BoardFactory
    private lateinit var registry: PlayerRegistry
    private lateinit var gameManager: GameManager

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        validator = BoardValidator()
        battleService = BattleService(validator)
        factory = BoardFactory()
        registry = PlayerRegistry(tempDir.resolve("test_players.json").absolutePath)
        gameManager = GameManager(validator, battleService, factory, registry)
    }

    @Test
    fun `full game flow with two players`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        assertEquals(2, gameManager.getAllPlayers().size)

        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)
        assertEquals(p1.id, game?.currentPlayer?.id)

        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p1.id, 2, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 2, 0, 1, "right")

        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.HIT || result == MoveResult.KILL || result == MoveResult.MISS)

        val currentGame = gameManager.getCurrentGame()
        assertNotNull(currentGame)
    }

    @Test
    fun `player registry integration - players persist between game sessions`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        assertEquals(2, gameManager.getAllPlayers().size)

        val newGameManager = GameManager(validator, battleService, factory, registry)
        val loadedPlayers = newGameManager.getAllPlayers()

        assertEquals(2, loadedPlayers.size)
        assertTrue(loadedPlayers.any { it.name == "Анна" })
        assertTrue(loadedPlayers.any { it.name == "Борис" })
    }

    @Test
    fun `player registry integration - new players get unique IDs across sessions`() {
        val p1 = gameManager.addPlayer("Анна")
        assertEquals(1, p1.id)

        val newGameManager = GameManager(validator, battleService, factory, registry)
        val p2 = newGameManager.addPlayer("Борис")

        assertEquals(2, p2.id)
    }

    @Test
    fun `game statistics integration - stats update after game`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        gameManager.createGame(p1.id, p2.id)
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.KILL || result == MoveResult.GAME_WON)

        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
        assertEquals(p1.id, game?.winner?.id)
    }

    @Test
    fun `full game flow with registry and statistics`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        val p3 = gameManager.addPlayer("Светлана")

        assertEquals(3, gameManager.getAllPlayers().size)

        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)

        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        gameManager.makeMove(p1.id, 0, 0)

        val finalGame = gameManager.getCurrentGame()
        assertNotNull(finalGame?.winner)

        gameManager.finishGame()

        val newGame = gameManager.createGame(p2.id, p3.id)
        assertNotNull(newGame)
        assertEquals(p2.id, newGame?.player1?.id)
        assertEquals(p3.id, newGame?.player2?.id)
    }

    @Test
    fun `player registry integration - restart application preserves players`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        val newRegistry = PlayerRegistry(tempDir.resolve("test_players.json").absolutePath)
        val newGameManager = GameManager(validator, battleService, factory, newRegistry)

        val loadedPlayers = newGameManager.getAllPlayers()
        assertEquals(2, loadedPlayers.size)

        assertTrue(loadedPlayers.any { it.id == p1.id && it.name == "Анна" })
        assertTrue(loadedPlayers.any { it.id == p2.id && it.name == "Борис" })
    }

    @Test
    fun `player registry integration - duplicate names are allowed`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Анна")

        assertEquals(2, gameManager.getAllPlayers().size)
        assertNotEquals(p1.id, p2.id)
        assertEquals("Анна", p1.name)
        assertEquals("Анна", p2.name)

        val newGameManager = GameManager(validator, battleService, factory, registry)
        val loadedPlayers = newGameManager.getAllPlayers()
        assertEquals(2, loadedPlayers.size)
        assertTrue(loadedPlayers.any { it.id == p1.id })
        assertTrue(loadedPlayers.any { it.id == p2.id })
    }

    @Test
    fun `game manager integration - cannot create game with non-existent player`() {
        val p1 = gameManager.addPlayer("Анна")

        val game = gameManager.createGame(p1.id, 999)
        assertNull(game)
    }

    @Test
    fun `game manager integration - cannot create game with same player`() {
        val p1 = gameManager.addPlayer("Анна")

        val game = gameManager.createGame(p1.id, p1.id)
        assertNull(game)
    }

    @Test
    fun `battle service integration - multiple hits on same ship`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p2.id, 0, 0, 3, "right")

        var result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, result)

        result = gameManager.makeMove(p1.id, 0, 1)
        assertEquals(MoveResult.HIT, result)

        result = gameManager.makeMove(p1.id, 0, 2)
        assertEquals(MoveResult.KILL, result)
    }

    @Test
    fun `board validator integration - ships cannot be placed too close`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        val firstResult = gameManager.placeShip(p1.id, 0, 0, 3, "right")
        assertEquals(ShipPlacementResult.SUCCESS, firstResult)

        val closeResult = gameManager.placeShip(p1.id, 0, 3, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, closeResult)

        val overlapResult = gameManager.placeShip(p1.id, 0, 1, 2, "right")
        assertEquals(ShipPlacementResult.OVERLAP, overlapResult)

        val successResult = gameManager.placeShip(p1.id, 2, 0, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, successResult)
    }

    @Test
    fun `ship placement constraints integration`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        val firstResult = gameManager.placeShip(p1.id, 0, 0, 3, "right")
        assertEquals(ShipPlacementResult.SUCCESS, firstResult)

        val closeResult = gameManager.placeShip(p1.id, 0, 3, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, closeResult)

        val successResult = gameManager.placeShip(p1.id, 2, 0, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, successResult)
    }

    @Test
    fun `complete battle with win detection`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.KILL || result == MoveResult.GAME_WON)

        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
        assertEquals(p1.id, game?.winner?.id)
    }

    @Test
    fun `multiple games in sequence`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        gameManager.createGame(p1.id, p2.id)
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.makeMove(p1.id, 0, 0)

        gameManager.finishGame()
        assertNull(gameManager.getCurrentGame())

        val p3 = gameManager.addPlayer("Светлана")
        val p4 = gameManager.addPlayer("Дмитрий")

        gameManager.createGame(p3.id, p4.id)
        assertNotNull(gameManager.getCurrentGame())
        assertEquals(p3.id, gameManager.getCurrentGame()?.currentPlayer?.id)

        assertEquals(4, gameManager.getAllPlayers().size)
    }
