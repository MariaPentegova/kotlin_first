package test

import models.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import service.PlayerRegistry
import java.io.File

class PlayerRegistryTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var registry: PlayerRegistry
    private lateinit var registryFile: String

    @BeforeEach
    fun setUp() {
        registryFile = tempDir.resolve("test_players.txt").absolutePath
        registry = PlayerRegistry(registryFile)
    }

    @Test
    fun `should save and load single player`() {
        val player = Player(1, "Анна")
        registry.save(player)

        val loaded = registry.getAll()
        assertEquals(1, loaded.size)
        assertEquals("Анна", loaded[0].name)
        assertEquals(1, loaded[0].id)
    }

    @Test
    fun `should save and load multiple players`() {
        registry.save(Player(1, "Анна"))
        registry.save(Player(2, "Борис"))
        registry.save(Player(3, "Светлана"))

        val loaded = registry.getAll()
        assertEquals(3, loaded.size)
    }

    @Test
    fun `should delete player`() {
        registry.save(Player(1, "Анна"))
        assertEquals(1, registry.getAll().size)

        registry.delete(1)
        assertEquals(0, registry.getAll().size)
    }

    @Test
    fun `should get player by id`() {
        registry.save(Player(1, "Анна"))

        val found = registry.getById(1)
        assertNotNull(found)
        assertEquals("Анна", found?.name)

        val notFound = registry.getById(999)
        assertNull(notFound)
    }

    @Test
    fun `should clear all players`() {
        registry.save(Player(1, "Анна"))
        registry.save(Player(2, "Борис"))

        assertEquals(2, registry.getAll().size)

        registry.clear()
        assertEquals(0, registry.getAll().size)
    }
}
