import database.DatabaseManager
import gui.MainFrame
import service.*
import ui.console.ConsoleUI
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main() {
    val dbManager = DatabaseManager()
    dbManager.initialize()

    val choice = JOptionPane.showOptionDialog(
        null,
        "Выберите режим запуска:",
        "Морской Бой",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        arrayOf("GUI (Swing)", "Консоль"),
        "GUI (Swing)"
    )

    when (choice) {
        0 -> {
            SwingUtilities.invokeLater {
                val frame = MainFrame(dbManager)
                frame.isVisible = true
            }
        }
        1 -> {
            val validator = BoardValidator()
            val battleService = BattleService(validator)
            val boardFactory = BoardFactory()
            val gameManager = GameManager(validator, battleService, boardFactory, dbManager)
            val consoleUI = ConsoleUI(gameManager, boardFactory)
            consoleUI.start()
        }
        else -> println("Выход")
    }
}
