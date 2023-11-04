package com.example.decisionbot

import androidx.compose.runtime.MutableState

sealed class Navigation {
    class Home : Navigation()

    class SelectChoice : Navigation()
    class EditChoice(val choice: Choice) : Navigation()
    class EditRequirement(val req: MutableState<PartialRequirement>): Navigation()

    class AnswerQuestion : Navigation()
    class SeeResults : Navigation()
}
