package com.example.decisionbot

import androidx.room.Query

@androidx.room.Dao
interface AppDao {
    @Query(
        """
        with
          not_answered as (
            select c.id
            from choice c
              left join decision d on c.id = d.id
            where d.id is null
        ), choice_status as (
            select na.id
                 , min(case when (r.id is null or d.id is not null) then 1 else 0 end) as ready
            from not_answered as na
              left join requirement r on na.id = r.choice
              left join decision d on r.answer = d.answer
            group by r.id, d.id
        )
        select c.id, c.prompt
        from choice as c
          join choice_status cs on c.id = cs.id
        where cs.ready
        limit 1
        """
    )
    suspend fun getNextChoice(): List<Choice>

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
            insert into answer (choice, description)
            values (:choiceId, :description)
        """
    )
    suspend fun insertAnswer(choiceId: Long, description: String): Long

    @Query(
        """
            update answer
            set description = :description
            where choice = :choiceId
        """
    )
    suspend fun updateAnswer(choiceId: Long, description: String)

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
            update requirement
            set choice = :choiceId
            set answer = :answerid
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
            insert into decision (choice, answer)
            values (:choiceId, :answerId)
        """
    )
    suspend fun insertDecision(choiceId: Long, answerId: Long): Long

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
            select *
            from requirement
            where requirement.choice = :choiceId
        """
    )
    suspend fun getRequirementsFor(choiceId: Long): List<Requirement>

    @Query(
        """
            select c.prompt as prompt, a.description as answer
            from decision d
              join choice c on d.choice = c.id
              join answer a on d.answer = a.id
        """
    )
    suspend fun getResults(): List<Result>
}