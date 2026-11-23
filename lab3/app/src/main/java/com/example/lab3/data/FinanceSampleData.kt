package com.example.lab3.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.util.Locale

// Режим темы, который пользователь может переключать вручную (Бонус +1)
enum class ThemeMode(val title: String) {
    SYSTEM("Авто"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

// Три обязательных состояния основного экрана по ТЗ
sealed interface FinanceUiState {
    data object Loading : FinanceUiState
    data object Empty : FinanceUiState
    data class Content(
        val totalBalance: Int,
        val monthlyIncome: Int,
        val monthlyExpense: Int,
        val accounts: List<AccountItem>,
        val categories: List<CategorySpend>,
        val transactions: List<TransactionItem>
    ) : FinanceUiState
}

data class AccountItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val balance: Int,
    val icon: ImageVector
)

data class CategorySpend(
    val id: String,
    val title: String,
    val amount: Int,
    val share: Float,
    val colorSlot: Int, // 0..3 — индекс цвета из темы (никаких литералов Color в данных!)
    val icon: ImageVector
)

data class TransactionItem(
    val id: String,
    val title: String,
    val category: String,
    val accountTitle: String,
    val dateTime: String,
    val amount: Int, // положительное — доход, отрицательное — расход
    val status: String,
    val note: String,
    val icon: ImageVector
)

/**
 * Простая обёртка над SharedPreferences для сохранения выбранной темы
 * и настройки Material You между запусками приложения (Бонус +1).
 */
class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("lab3_theme_prefs", Context.MODE_PRIVATE)

    fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun loadDynamicColor(): Boolean = prefs.getBoolean("dynamic_color", false)

    fun saveDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }
}

/**
 * Демонстрационные данные (заглушки в коде, как требует ТЗ работы №3).
 */
object FinanceSampleData {

    val sampleContent = FinanceUiState.Content(
        totalBalance = 148_450,
        monthlyIncome = 92_000,
        monthlyExpense = 41_850,
        accounts = listOf(
            AccountItem("acc1", "Основная карта", "•• 4821", 64_250, Icons.Outlined.CreditCard),
            AccountItem("acc2", "Накопительный", "14% годовых", 78_900, Icons.Outlined.AccountBalance),
            AccountItem("acc3", "Кэшбэк-копилка", "Доступно сейчас", 5_300, Icons.Outlined.Savings)
        ),
        categories = listOf(
            CategorySpend("cat1", "Продукты и кафе", 16_740, 0.40f, 0, Icons.Outlined.Fastfood),
            CategorySpend("cat2", "Жильё и связь", 11_720, 0.28f, 1, Icons.Outlined.Home),
            CategorySpend("cat3", "Транспорт", 7_530, 0.18f, 2, Icons.Outlined.DirectionsCar),
            CategorySpend("cat4", "Покупки", 5_860, 0.14f, 3, Icons.Outlined.ShoppingBag)
        ),
        transactions = listOf(
            TransactionItem(
                id = "tx1",
                title = "ВкусВилл",
                category = "Продукты и кафе",
                accountTitle = "Основная карта •• 4821",
                dateTime = "Сегодня, 18:24",
                amount = -1_480,
                status = "Проведено",
                note = "Покупка продуктов на ужин, начислен кэшбэк 44 ₽",
                icon = Icons.Outlined.Fastfood
            ),
            TransactionItem(
                id = "tx2",
                title = "Зарплата · Аванс",
                category = "Доход",
                accountTitle = "Основная карта •• 4821",
                dateTime = "Вчера, 11:05",
                amount = 46_000,
                status = "Зачислено",
                note = "Поступление от работодателя по зарплатному проекту",
                icon = Icons.Outlined.WorkOutline
            ),
            TransactionItem(
                id = "tx3",
                title = "Яндекс Go · Такси",
                category = "Транспорт",
                accountTitle = "Основная карта •• 4821",
                dateTime = "Вчера, 08:40",
                amount = -540,
                status = "Проведено",
                note = "Поездка по тарифу Комфорт",
                icon = Icons.Outlined.DirectionsCar
            ),
            TransactionItem(
                id = "tx4",
                title = "Оплата ЖКХ и интернета",
                category = "Жильё и связь",
                accountTitle = "Накопительный",
                dateTime = "19 ноября, 14:15",
                amount = -6_890,
                status = "Проведено",
                note = "Автоплатёж за коммунальные услуги без комиссии",
                icon = Icons.Outlined.Home
            ),
            TransactionItem(
                id = "tx5",
                title = "Ozon · Электроника",
                category = "Покупки",
                accountTitle = "Основная карта •• 4821",
                dateTime = "17 ноября, 20:10",
                amount = -3_290,
                status = "Проведено",
                note = "Зарядное устройство USB-C и кабель",
                icon = Icons.Outlined.ShoppingBag
            )
        )
    )

    // Форматирование сумм с пробелами между разрядами: 148 450 ₽
    fun formatRub(amount: Int, showPlusSign: Boolean = false): String {
        val nf = NumberFormat.getIntegerInstance(Locale("ru", "RU"))
        val absText = nf.format(kotlin.math.abs(amount))
        return when {
            amount < 0 -> "−$absText ₽"
            showPlusSign && amount > 0 -> "+$absText ₽"
            else -> "$absText ₽"
        }
    }
}
