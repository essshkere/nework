package ru.netology.nework.repository

import ru.netology.nework.data.Job

interface JobRepository {
    suspend fun getMyJobs(): List<Job>
    suspend fun save(job: Job): Job
    suspend fun removeById(id: Long)
}