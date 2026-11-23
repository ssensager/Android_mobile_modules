package com.example.lab3.ui.screens

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lab3.data.FinanceSampleData
import com.example.lab3.data.FinanceUiState
import com.example.lab3.data.ThemeMode
import com.example.lab3.data.TransactionItem
import com.example.lab3.ui.components.AccountCard
import com.example.lab3.ui.components.BalanceHeroCard
import com.example.lab3.ui.components.CategoryBreakdownCard
import com.example.lab3.ui.components.FinanceEmptyState
import com.example.lab3.ui.components.FinanceLoadingSkeleton
import com.example.lab3.ui.components.TransactionRow
import com.example.lab3.ui.theme.FinanceTheme
import com.example.lab3.ui.theme.LocalFinanceColors

/**
 * Главный обзорный экран «ФинПульс» с поднятым состоянием (state hoisting).
 * Адаптируется под WindowWidthSizeClass:
 * - Compact (телефон): вертикальный LazyColumn;
 * - Medium / Expanded (планшет): две колонки — слева аналитика и счета, справа операции и детали.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceOverviewScreen(
    uiState: FinanceUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    selectedTransaction: TransactionItem?,
    onCycleThemeMode: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onSelectStateTab: (String) -> Unit,
    onTransactionClick: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWideScreen = windowWidthSizeClass != WindowWidthSizeClass.Compact

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        Text(
                            text = "ФинПульс",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Личные финансы · ${themeMode.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        IconButton(onClick = onToggleDynamicColor) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = "Переключить Material You",
                                tint = if (dynamicColor) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    IconButton(onClick = onCycleThemeMode) {
                        val themeIcon = when (themeMode) {
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                            ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                        }
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Переключить тему (Светлая / Тёмная / Авто)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Панель переключения трёх обязательных состояний экрана (для удобной проверки на защите)
            StateSwitcherRow(
                currentState = uiState,
                onSelect = onSelectStateTab
            )

            // Плавная анимация смены состояний Loading / Empty / Content
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "finance_state_transition",
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    FinanceUiState.Loading -> {
                        FinanceLoadingSkeleton(modifier = Modifier.padding(16.dp))
                    }
                    FinanceUiState.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            FinanceEmptyState(onLoadDemoData = { onSelectStateTab("content") })
                        }
                    }
                    is FinanceUiState.Content -> {
                        if (isWideScreen) {
                            WideOverviewLayout(
                                content = state,
                                selectedTransaction = selectedTransaction ?: state.transactions.firstOrNull(),
                                onTransactionClick = onTransactionClick
                            )
                        } else {
                            CompactOverviewLayout(
                                content = state,
                                onTransactionClick = onTransactionClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StateSwitcherRow(
    currentState: FinanceUiState,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentState is FinanceUiState.Content,
            onClick = { onSelect("content") },
            label = { Text("Наполнено") }
        )
        FilterChip(
            selected = currentState is FinanceUiState.Loading,
            onClick = { onSelect("loading") },
            label = { Text("Загрузка") }
        )
        FilterChip(
            selected = currentState is FinanceUiState.Empty,
            onClick = { onSelect("empty") },
            label = { Text("Пусто") }
        )
    }
}

@Composable
private fun CompactOverviewLayout(
    content: FinanceUiState.Content,
    onTransactionClick: (TransactionItem) -> Unit
) {
    // Используем LazyColumn, чтобы элементы списка операций создавались лениво
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "balance") {
            BalanceHeroCard(
                totalBalance = content.totalBalance,
                monthlyIncome = content.monthlyIncome,
                monthlyExpense = content.monthlyExpense
            )
        }
        item(key = "accounts_header") {
            Text(
                text = "Счета и карты",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { heading() }
            )
        }
        item(key = "accounts_row") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(content.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        modifier = Modifier.width(172.dp)
                    )
                }
            }
        }
        item(key = "categories") {
            CategoryBreakdownCard(categories = content.categories)
        }
        item(key = "tx_header") {
            Text(
                text = "Последние операции",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { heading() }
            )
        }
        items(content.transactions, key = { it.id }) { tx ->
            TransactionRow(
                item = tx,
                isSelected = false,
                onClick = { onTransactionClick(tx) }
            )
        }
        item(key = "bottom_spacer") {
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Адаптивная двухколоночная компоновка для планшетов (WindowWidthSizeClass.Medium / Expanded):
 * слева баланс, счета и расходы; справа список операций и детали выбранной операции.
 */
@Composable
private fun WideOverviewLayout(
    content: FinanceUiState.Content,
    selectedTransaction: TransactionItem?,
    onTransactionClick: (TransactionItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item(key = "wide_balance") {
                BalanceHeroCard(
                    totalBalance = content.totalBalance,
                    monthlyIncome = content.monthlyIncome,
                    monthlyExpense = content.monthlyExpense
                )
            }
            item(key = "wide_accounts") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content.accounts.forEach { acc ->
                        AccountCard(account = acc, modifier = Modifier.weight(1f))
                    }
                }
            }
            item(key = "wide_categories") {
                CategoryBreakdownCard(categories = content.categories)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            if (selectedTransaction != null) {
                item(key = "wide_selected_detail") {
                    TransactionDetailCard(item = selectedTransaction)
                }
            }
            item(key = "wide_tx_header") {
                Text(
                    text = "Последние операции",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics { heading() }
                )
            }
            items(content.transactions, key = { it.id }) { tx ->
                TransactionRow(
                    item = tx,
                    isSelected = selectedTransaction?.id == tx.id,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }
    }
}

/**
 * Второй экран (для телефонов): полная карточка деталей выбранной операции.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    item: TransactionItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Вернуться к обзору"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Детали операции",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TransactionDetailCard(item = item)
        }
    }
}

@Composable
fun TransactionDetailCard(
    item: TransactionItem,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinanceColors.current
    val isIncome = item.amount > 0
    val amountColor = if (isIncome) ext.income else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isIncome) ext.incomeContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.category,
                            tint = if (isIncome) ext.income else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = FinanceSampleData.formatRub(item.amount, showPlusSign = true),
                style = MaterialTheme.typography.displaySmall,
                color = amountColor
            )
            Spacer(Modifier.height(10.dp))

            // Статус передаётся иконкой, текстом и цветом одновременно (не одним цветом!)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = ext.incomeContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = ext.income,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.income
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            DetailRow("Дата и время", item.dateTime)
            Spacer(Modifier.height(10.dp))
            DetailRow("Счёт", item.accountTitle)
            Spacer(Modifier.height(10.dp))
            DetailRow("Комментарий", item.note)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(name = "Обзор · Светлая", showBackground = true, widthDp = 412, heightDp = 892)
@Preview(name = "Обзор · Тёмная", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun FinanceOverviewPreview() {
    FinanceTheme(themeMode = ThemeMode.SYSTEM) {
        FinanceOverviewScreen(
            uiState = FinanceSampleData.sampleContent,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            themeMode = ThemeMode.SYSTEM,
            dynamicColor = false,
            selectedTransaction = null,
            onCycleThemeMode = {},
            onToggleDynamicColor = {},
            onSelectStateTab = {},
            onTransactionClick = {}
        )
    }
}
