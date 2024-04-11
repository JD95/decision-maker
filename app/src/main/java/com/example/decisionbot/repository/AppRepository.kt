package com.example.decisionbot.repository

import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice
import com.example.decisionbot.repository.entity.Result
import com.example.decisionbot.repository.entity.RequirementBox

class AppRepository(
    val dao: AppDao
) {
    suspend fun getAllChoices(): List<Choice> {
        return dao.getAllChoices()
    }

    suspend fun insertChoice(prompt: String): Choice {
        val id = dao.insertChoice(prompt)
        return Choice(id, prompt)
    }

    suspend fun editChoice(value: Choice) {
        dao.updateChoice(value.id, value.prompt)
    }

    suspend fun getAnswersForChoice(value: Choice): List<Answer> {
        return dao.getAnswersFor(value.id)
    }

    suspend fun editAnswer(answer: Answer) {
        dao.updateAnswer(answer.id, answer.description)
    }

    suspend fun deleteAnswer(answer: Answer) {
        dao.deleteAnswer(answer.id)
    }

    suspend fun getRequirementBoxInfoFor(choice: Choice): List<RequirementBox> {
        return dao.getRequirementBoxFor(choice.id)
    }

    suspend fun editRequirementBox(value: RequirementBox) {
        dao.updateRequirement(value.id, value.choice, value.answer)
    }

    suspend fun deleteRequirementBox(value: RequirementBox) {
        dao.deleteRequirement(value.id)
    }

    suspend fun getNextChoiceForDecision(): Choice? {
        val results = dao.getNextChoice()
        if (results.isNotEmpty()) {
            return results[0]
        }
        else {
            return null
        }
    }

    suspend fun getResults(): List<Result> {
        return dao.getResults()
    }
}