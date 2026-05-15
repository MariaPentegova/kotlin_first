/**
 * Результат совершенного выстрела.
 */
enum class ShotResult {
    MISS, // Мимо
    HIT,  // Попал
    SUNK  // Потопил
}

/**
 * Управление игровым процессом и сессией.
 */
interface IGameSession {
    fun startGame()
    fun nextTurn()
    fun checkWinCondition(): Boolean
    fun calculateResults(): Map<String, Int>
}

/**
 * Сущность игрока.
 */
interface IPlayer {
    val name: String
    val board: IBoard       // Собственное игровое поле
    val enemyBoard: IBoard  // Доступ к игровому полю противника
    
    fun makeShot(x: Int, y: Int): ShotResult
}

/**
 * Игровое поле, администрирующее расстановку и фиксацию выстрелов.
 */
interface IBoard {
    val size: Int
    
    /**
     * Размещает корабль на поле.
     * @return true, если корабль успешно размещен и не нарушает правил.
     */
    fun placeShip(ship: IShip, x: Int, y: Int, vertical: Boolean): Boolean
    
    /**
     * Принимает выстрел по координатам и обрабатывает его результат.
     */
    fun receiveShot(x: Int, y: Int): ShotResult
}

/**
 * Сущность корабля на игровом поле.
 */
interface IShip {
    val length: Int
    val health: Int
    val isSunk: Boolean
    
    /**
     * Фиксирует попадание в сегмент корабля и уменьшает его здоровье.
     */
    fun hit()
}

/**
 * Менеджер сбора статистики и истории игр.
 */
interface IStatisticsManager {
    fun saveGameHistory(history: String)
    fun getPlayerRating(playerId: String): Double
}
