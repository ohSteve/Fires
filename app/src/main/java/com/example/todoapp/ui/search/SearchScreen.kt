package com.example.todoapp.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenJournal: (String) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenTodo: () -> Unit
) {
    val keyword by viewModel.keyword.collectAsState()
    val categories by viewModel.selectedCategories.collectAsState()
    val results by viewModel.results.collectAsState()
    val history by viewModel.history.collectAsState()

    BackHandler(onBack = onBack)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 顶部：返回 + 搜索框 + 等宽分类筛选
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = viewModel::setKeyword,
                        placeholder = {
                            Text(
                                "搜索日志、目标、待办",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (keyword.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setKeyword("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清空",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.commitToHistory() }),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 三个分类 chip：等宽，整体与搜索框左右对齐（左边距 = 8dp 外边距 + 48dp 返回按钮）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 56.dp, end = 8.dp, top = 2.dp, bottom = 10.dp)
                ) {
                    SearchCategory.entries.forEach { category ->
                        FilterChip(
                            selected = category in categories,
                            onClick = { viewModel.toggleCategory(category) },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(category.label, maxLines = 1)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (keyword.isBlank()) {
                HistorySection(
                    history = history,
                    onPick = {
                        viewModel.setKeyword(it)
                        viewModel.commitToHistory()
                    },
                    onDelete = viewModel::removeHistory,
                    onClearAll = viewModel::clearHistory
                )
            } else {
                ResultSection(
                    results = results,
                    keyword = keyword,
                    noCategorySelected = categories.isEmpty(),
                    onOpenJournal = onOpenJournal,
                    onOpenGoal = onOpenGoal,
                    onOpenTodo = onOpenTodo
                )
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<String>,
    onPick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp)
        ) {
            Text(
                "搜索历史",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("全部清除", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (history.isEmpty()) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                "暂无搜索历史",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(history, key = { it }) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(item) }
                            .padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            item,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除该记录",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSection(
    results: List<SearchHit>,
    keyword: String,
    noCategorySelected: Boolean,
    onOpenJournal: (String) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenTodo: () -> Unit
) {
    if (results.isEmpty()) {
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            text = if (noCategorySelected) "请至少选择一个分类"
                   else "未找到与「$keyword」相关的内容",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "共 ${results.size} 条结果",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(results) { hit ->
                ResultCard(
                    hit = hit,
                    keyword = keyword,
                    onClick = {
                        when {
                            hit.journalDate != null -> onOpenJournal(hit.journalDate)
                            hit.goalId != null -> onOpenGoal(hit.goalId)
                            else -> onOpenTodo()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultCard(hit: SearchHit, keyword: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hit.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    hit.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            HighlightedText(
                text = snippetAround(hit.text, keyword),
                keyword = keyword,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 命中片段：以关键词为中心截取上下文，首尾截断处加省略号 */
private fun snippetAround(text: String, keyword: String, before: Int = 24, after: Int = 60): String {
    if (keyword.isBlank()) return text
    val flat = text.replace("\n", " ")
    val idx = flat.indexOf(keyword, ignoreCase = true)
    if (idx < 0) return flat.take(before + after) + if (flat.length > before + after) "…" else ""
    val start = (idx - before).coerceAtLeast(0)
    val end = (idx + keyword.length + after).coerceAtMost(flat.length)
    return buildString {
        if (start > 0) append("…")
        append(flat.substring(start, end))
        if (end < flat.length) append("…")
    }
}

@Composable
private fun HighlightedText(text: String, keyword: String, modifier: Modifier = Modifier) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, keyword, highlightColor) {
        buildAnnotatedString {
            if (keyword.isBlank()) {
                append(text)
            } else {
                var index = 0
                while (index < text.length) {
                    val match = text.indexOf(keyword, index, ignoreCase = true)
                    if (match < 0) {
                        append(text.substring(index))
                        break
                    }
                    append(text.substring(index, match))
                    withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                        append(text.substring(match, match + keyword.length))
                    }
                    index = match + keyword.length
                }
            }
        }
    }
    Text(
        annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
