package com.example.decisionbot.repository

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

    @Query(
        """
            insert into choice (prompt)
            values (:prompt)
        """
    )
    suspend fun insertChoice(prompt: String): Long

    @Query(
        """
            select *
            from choice
            where id = :choiceId
        """
    )
    suspend fun getChoice(choiceId: Long): Choice

    @Query(
        """
            update choice
            set prompt = :prompt
            where id = :choiceId
        """
    )
    suspend fun updateChoice(choiceId: Long, prompt: String)

    @Query(
        """
            delete from choice
            where id = :choiceId
        """
    )
    suspend fun deleteChoice(choiceId: Long)

    @Query(
        """
            insert into answer (choice, description)
            values (:choiceId, :description)
        """
    )
    suspend fun insertAnswer(choiceId: Long, description: String): Long

    @Query(
        """
            update answer
            set description = :description
            where id = :answerId
        """
    )
    suspend fun updateAnswer(answerId: Long, description: String)

    @Query(
        """
            delete from answer
            where id = :answerId
        """
    )
    suspend fun deleteAnswer(answerId: Long)

    @Query(
        """
            insert into requirement (choice, answer)
            values (:choiceId, :answerId)
        """
    )
    suspend fun insertRequirement(choiceId: Long, answerId: Long): Long

    @Query(
        """
            select *
            from requirement
            where id = :requirementId
        """
    )
    suspend fun getRequirement(requirementId: Long): Requirement

    @Query(
        """
            update requirement
            set choice = :choiceId
              , answer = :answerId
            where id = :requirementId
        """
    )
    suspend fun updateRequirement(requirementId: Long, choiceId: Long, answerId: Long)

    @Query(
        """
            delete from requirement
            where id = :requirementId
        """
    )
    suspend fun deleteRequirement(requirementId: Long)

    @Query(
        """
            select *
            from answer
            where answer.choice = :choiceId
        """
    )
    suspend fun getAnswersFor(choiceId: Long): List<Answer>

    @Query(
        """
            select c.id, c.prompt 
            from answer a 
               join choice c on c.id = a.choice
            where a.id = :answerId
        """
    )
    suspend fun getChoiceForAnswer(answerId: Long): Choice

    @Query(
        """
            select r.id, r.choice, r.answer, c.prompt, a.description
            from requirement r
              join answer a on r.answer = a.id
              join choice c on a.choice = c.id
            where r.choice = :choiceId
        """
    )
    suspend fun getRequirementBoxFor(choiceId: Long): List<RequirementBox>

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
            select * from answer
            where id = :answerId
        """
    )
    suspend fun getAnswer(answerId: Long): Answer
}