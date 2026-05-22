import gui.MainFrame
import service.*
import ui.ConsoleUI
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main() {
    val choice = JOptionPane.showOptionDialog(
        null,
        "Выберите режим запуска:",
        "Морской Бой",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        arrayOf("GUI", "Консоль"),
        "GUI"
    )

    when (choice) {
        0 -> {
            // GUI режим
            SwingUtilities.invokeLater {
                val frame = MainFrame()
                frame.isVisible = true
            }
        }
        1 -> {
            // Консольный режим - создаём зависимости
            val validator = BoardValidator()
            val battleService = BattleService(validator)
            val boardFactory = BoardFactory()
            val registry = PlayerRegistry()  // для сохранения игроков
            val gameManager = GameManager(validator, battleService, boardFactory, registry)
            val consoleUI = ConsoleUI(gameManager, boardFactory)
            consoleUI.start()
        }
        else -> {
            println("Выход")
        }
    }
}
