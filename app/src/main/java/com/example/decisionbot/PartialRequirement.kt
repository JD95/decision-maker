package com.example.decisionbot

sealed class PartialRequirement {
    object Empty : PartialRequirement()
    class WithChoice(val choice: Choice) : PartialRequirement()
    class Complete(val choice: Choice, val answer: Answer) : PartialRequirement()
}