package com.baekseok.shvoca.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.baekseok.shvoca.data.BookSummary
import com.baekseok.shvoca.data.KanjiDatabase
import com.baekseok.shvoca.data.VocabBook
import com.baekseok.shvoca.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val LANG_OPTIONS = listOf("영어", "일본어", "중국어", "한자")

private fun langColor(type: String): Color = when (type) {
    "영어"  -> Color(0xFF3A7BD5)
    "일본어" -> Color(0xFFE05555)
    "중국어" -> Color(0xFFD4880A)
    else   -> Color(0xFF9B7FD4)
}

@Composable
fun LanguageSelectScreen(onLanguageSelected: (String) -> Unit) {
    val context = LocalContext.current
    val db      = remember { KanjiDatabase.getInstance(context.applicationContext) }
    val scope   = rememberCoroutineScope()

    val books by db.kanjiDao().getBookSummaries().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<BookSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 32.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "단어장 선택",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "+",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddDialog = true }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("학습할 단어장을 선택하세요.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))

        if (books.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("단어장이 없습니다", color = Muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books) { book ->
                    BookCard(
                        book    = book,
                        onClick = { onLanguageSelected(book.name) },
                        onEdit  = { editTarget = book }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        BookDialog(
            title     = "단어장 추가",
            initName  = "",
            initLang  = "일본어",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, langType ->
                scope.launch(Dispatchers.IO) {
                    db.kanjiDao().insertBook(VocabBook(name = name, languageType = langType))
                }
                showAddDialog = false
            }
        )
    }

    editTarget?.let { target ->
        BookDialog(
            title     = "단어장 수정",
            initName  = target.name,
            initLang  = target.languageType,
            onDismiss = { editTarget = null },
            onConfirm = { name, langType ->
                scope.launch(Dispatchers.IO) {
                    db.kanjiDao().renameBook(
                        oldName = target.name,
                        book    = VocabBook(id = target.id, name = name, languageType = langType)
                    )
                }
                editTarget = null
            },
            onDelete = {
                scope.launch(Dispatchers.IO) {
                    db.kanjiDao().deleteBookWithWords(
                        VocabBook(id = target.id, name = target.name, languageType = target.languageType)
                    )
                }
                editTarget = null
            }
        )
    }
}

// ── 단어장 카드 ────────────────────────────────────────────────────────────────

@Composable
private fun BookCard(book: BookSummary, onClick: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(langColor(book.languageType).copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    book.languageType,
                    color = langColor(book.languageType),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(book.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("${book.count}단어", color = Muted, fontSize = 13.sp)
        }

        Text(
            "수정",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEdit() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ── 추가 / 수정 다이얼로그 ──────────────────────────────────────────────────────

@Composable
private fun BookDialog(
    title: String,
    initName: String,
    initLang: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, langType: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var selectedLang by remember { mutableStateOf(initLang) }
    var name         by remember { mutableStateOf(initName) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

                Spacer(Modifier.height(20.dp))

                // 언어 선택 (수정 시에는 변경 불가하므로 표시하지 않음)
                val isEditMode = onDelete != null
                if (!isEditMode) {
                    Text("언어", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LANG_OPTIONS.forEach { lang ->
                            val selected = lang == selectedLang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) langColor(lang).copy(alpha = 0.25f) else Line
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) langColor(lang) else Line,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedLang = lang }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    lang,
                                    color = if (selected) langColor(lang) else Muted,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // 이름 입력
                Text("단어장 이름", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    singleLine    = true,
                    placeholder   = { Text("이름 입력", color = Muted, fontSize = 14.sp) },
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = Gold,
                        unfocusedBorderColor    = Line,
                        focusedTextColor        = Ink,
                        unfocusedTextColor      = Ink,
                        cursorColor             = Gold,
                        focusedContainerColor   = Paper,
                        unfocusedContainerColor = Paper
                    )
                )

                Spacer(Modifier.height(20.dp))

                // 버튼 행
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("삭제", color = Red, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = Muted)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick  = { if (name.isNotBlank()) onConfirm(name.trim(), selectedLang) },
                        enabled  = name.isNotBlank(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
                    ) {
                        Text(
                            if (onDelete != null) "저장" else "생성",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        var confirmText by remember { mutableStateOf("") }
        val canDelete = confirmText.trim() == initName || confirmText.trim() == "2003-11-21"

        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardBg,
            icon = { Text("⚠", color = Red, fontSize = 30.sp) },
            title = {
                Text(
                    "단어장을 삭제할까요?",
                    color = Red,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        "'$initName' 단어장과 안에 있는 단어가 모두 삭제돼요. 이 작업은 되돌릴 수 없어요.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "확인을 위해 단어장 이름을 똑같이 입력하세요.",
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        singleLine = true,
                        placeholder = { Text(initName, color = Muted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Red,
                            unfocusedBorderColor = Line,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                            cursorColor = Red,
                            focusedContainerColor = Paper,
                            unfocusedContainerColor = Paper
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    enabled = canDelete,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red,
                        contentColor = Ink,
                        disabledContainerColor = Line,
                        disabledContentColor = Muted
                    )
                ) {
                    Text("삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", color = Muted)
                }
            }
        )
    }
}
