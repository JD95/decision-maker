package com.example.decisionbot.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp

data class ListItem<T, K>(
    val edit: () -> Unit,
    val delete: () -> Unit,
    val get: () -> Pair<T, K>
)

@Composable
fun <Item> EditDeleteBoxList(
    items: List<ListItem<Item, Unit>>,
    displayItem: @Composable (Item) -> Unit,
) {
    EditDeleteBoxList(items) { x, _ -> displayItem(x) }
}

@Composable
fun <Item, Context> EditDeleteBoxList(
    items: List<ListItem<Item, Context>>,
    displayItem: @Composable (Item, Context) -> Unit,
) {
    LazyColumn {
        items(items) { item ->
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pair = item.get()
                    displayItem(pair.first, pair.second)
                    TextButton(onClick = { item.edit() }) {
                        Text(text = "edit")
                    }
                    TextButton(onClick = { item.delete() }) {
                        Text(text = "delete")
                    }
                }
            }
        }
    }
}

@Composable
fun ListTitle(title: String, newItem: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, fontSize = 25.sp)
        TextButton(onClick = newItem) {
            Icon(Icons.Default.Add, contentDescription = "add")
        }
    }
}

fun <T> makeListItems(
    items: MutableState<List<T>>,
    edit: (MutableState<T>) -> Unit,
    delete: (T) -> Unit
): List<ListItem<T, Unit>> {
    return makeListItems(
        items,
        editContext = { },
        edit = { x, _ -> edit(x) },
        delete = delete
    )
}

fun <T, K> makeListItems(
    items: MutableState<List<T>>,
    editContext: (MutableState<T>) -> K,
    edit: (MutableState<T>, K) -> Unit,
    delete: (T) -> Unit
): List<ListItem<T, K>> {
    return items.value.map { t ->
        val st = mutableStateOf(t)
        val ctx = editContext(st)
        ListItem(
            get = { Pair(st.value, ctx) },
            edit = { edit(st, ctx) },
            delete = {
                delete(t)
                items.value = items.value.minus(t)
            }
        )
    }.toList()
}