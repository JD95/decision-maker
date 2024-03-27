package com.example.decisionbot

import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice

sealed class PartialRequirement {
    object Empty : PartialRequirement()
    class WithChoice(val choice: Choice) : PartialRequirement()
    class Complete(val choice: Choice, val answer: Answer) : PartialRequirement()
}