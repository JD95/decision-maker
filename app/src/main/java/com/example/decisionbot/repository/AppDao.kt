package com.example.decisionbot.repository

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.decisionbot.repository.entity.*

@androidx.room.Dao
interface AppDao {
    @Query(
        """
            select *
            from choice
        """
    )
    suspend fun getAllChoices(): List<Choice>

    @Insert
    suspend fun insertChoice(choice: Choice)

    @Query(
        """
            select *
            from choice
            where id = :choiceId
        """
    )
    suspend fun getChoice(choiceId: String): Choice

    @Query(
        """
            update choice
            set prompt = :prompt
            where id = :choiceId
        """
    )
    suspend fun updateChoice(choiceId: String, prompt: String)

    @Delete
    suspend fun deleteChoice(choice: Choice)

    @Insert
    suspend fun insertAnswer(answer: Answer)

    @Query(
        """
            update answer
            set description = :description
            where id = :answerId
        """
    )
    suspend fun updateAnswer(answerId: String, description: String)

    @Delete
    suspend fun deleteAnswer(answer: Answer)

    @Insert
    suspend fun insertRequirement(requirement: Requirement)

    @Query(
        """
            select *
            from requirement
            where id = :requirementId
        """
    )
    suspend fun getRequirement(requirementId: String): Requirement

    @Query(
        """
            update requirement
            set choice = :choiceId
              , answer = :answerId
            where id = :requirementId
        """
    )
    suspend fun updateRequirement(requirementId: String, choiceId: String, answerId: String)

    @Delete
    suspend fun deleteRequirement(requirement: Requirement)

    @Query(
        """
            select *
            from answer
            where answer.choice = :choiceId
        """
    )
    suspend fun getAnswersFor(choiceId: String): List<Answer>

    @Query(
        """
            select c.id, c.prompt 
            from answer a 
               join choice c on c.id = a.choice
            where a.id = :answerId
        """
    )
    suspend fun getChoiceForAnswer(answerId: String): Choice

    @Query(
        """
            select r.id, r.choice, r.answer, c.prompt, a.description
            from requirement r
              join answer a on r.answer = a.id
              join choice c on a.choice = c.id
            where r.choice = :choiceId
        """
    )
    suspend fun getRequirementBoxFor(choiceId: String): List<RequirementBox>

    @Query(
        """
            delete from choice
        """
    )
    suspend fun deleteAllChoices()

    @Query(
        """
            delete from answer 
        """
    )
    suspend fun deleteAllAnswers()

    @Query(
        """
            delete from requirement
        """
    )
    suspend fun deleteAllRequirements()

    @Query(
        """
            select id, choice, description from answer
            where id = :answerId
        """
    )
    suspend fun getAnswer(answerId: String): Answer
}