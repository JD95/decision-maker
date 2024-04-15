package com.example.decisionbot.repository

import android.util.Log
import com.example.decisionbot.repository.entity.*

class AppRepository(
    private val dao: AppDao
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

    suspend fun insertAnswer(choice: Choice, description: String): Answer {
        val id = dao.insertAnswer(choice.id, description)
        return Answer(id, choice.id, description)
    }

    suspend fun editAnswer(answer: Answer) {
        Log.d("repo.editAnswer", "answer:${answer.id} updated to '${answer.description}'")
        dao.updateAnswer(answer.id, answer.description)
    }

    suspend fun deleteAnswer(answer: Answer) {
        dao.deleteAnswer(answer.id)
    }

    suspend fun insertRequirement(choice: Choice, answer: Answer): Requirement {
        val id = dao.insertRequirement(choice.id, answer.id)
        return Requirement(id, choice.id, answer.id)
    }

    suspend fun editRequirement(requirement: Requirement) {
        dao.updateRequirement(requirement.id, requirement.choice, requirement.answer)
    }

    suspend fun getRequirementBoxInfoFor(choice: Choice): List<RequirementBox> {
        return dao.getRequirementBoxFor(choice.id)
    }

    suspend fun getChoiceForRequirement(req: Requirement): Choice {
        return dao.getChoiceForAnswer(req.answer)
    }

    suspend fun deleteRequirementBox(value: RequirementBox) {
        dao.deleteRequirement(value.id)
    }

    suspend fun getNextChoiceForDecision(): Choice? {
        val results = dao.getNextChoice()
        return if (results.isNotEmpty()) {
            results[0]
        } else {
            null
        }
    }

    suspend fun getResults(): List<Result> {
        return dao.getResults()
    }

    suspend fun deleteChoice(value: Choice) {
        dao.deleteChoice(value.id)
    }
}