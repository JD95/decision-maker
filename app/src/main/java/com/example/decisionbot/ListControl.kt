package com.example.decisionbot

import androidx.compose.runtime.MutableState

class ListControl<T>(
    val items: MutableState<List<T>>,
    val makeNewItem: () -> T
) {


}