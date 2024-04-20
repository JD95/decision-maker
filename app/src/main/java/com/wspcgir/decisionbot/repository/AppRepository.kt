package com.wspcgir.decisionbot.repository

import android.util.Log
import com.wspcgir.decisionbot.model.DecisionData
import com.wspcgir.decisionbot.repository.entity.*
import java.util.*

class AppRepository(
    private val dao: AppDao
) {
    suspend fun getAllChoices(): List<Choice> {
        return dao.getAllChoices()
    }

    suspend fun insertChoice(prompt: String): Choice {
        val new = Choice(UUID.randomUUID().toString(), prompt)
        dao.insertChoice(new)
        return new
    }

    suspend fun editChoice(value: Choice) {
        dao.updateChoice(value.id, value.prompt)
    }

    suspend fun getAnswersForChoice(value: Choice): List<Answer> {
        return dao.getAnswersFor(value.id)
    }

    suspend fun insertAnswer(choice: Choice, description: String): Answer {
        val new = Answer(UUID.randomUUID().toString(), choice.id, description)
        dao.insertAnswer(new)
        return new
    }

    suspend fun editAnswer(answer: Answer) {
        Log.d("repo.editAnswer", "answer:${answer.id} updated to '${answer.description}'")
        dao.updateAnswer(answer.id, answer.description)
    }

    suspend fun deleteAnswer(answer: Answer) {
        dao.deleteAnswer(answer)
    }

    suspend fun insertRequirement(choice: Choice, answer: Answer): Requirement {
        val new = Requirement(UUID.randomUUID().toString(), choice.id, answer.id)
        dao.insertRequirement(new)
        return new
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
        dao.deleteRequirement(Requirement(value.id, value.choice, value.answer))
    }

    suspend fun deleteChoice(value: Choice) {
        dao.deleteChoice(value)
    }

    suspend fun getAllDecisionData(): List<DecisionData> {
        return dao.getAllChoices().map {
            val answers = dao.getAnswersFor(it.id)
            val requirements = dao
                .getRequirementBoxFor(it.id)
                .map { r -> Requirement(r.id, r.choice, r.answer) }
            DecisionData(it, answers, requirements)
        }
    }

    suspend fun getChoiceForAnswer(it: Answer): Choice {
        return dao.getChoice(it.choice)
    }

    suspend fun getAnswerForRequirement(requirement: Requirement): Answer {
        return dao.getAnswer(requirement.answer)
    }
}