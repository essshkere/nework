package ru.netology.nework.repository

import ru.netology.nework.api.JobApi
import ru.netology.nework.data.Job
import ru.netology.nework.mapper.toDto
import ru.netology.nework.mapper.toModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val jobApi: JobApi
) : JobRepository {

    override suspend fun getMyJobs(): List<Job> {
        return try {
            val response = jobApi.getMyJobs()
            if (response.isSuccessful) {
                response.body()?.map { it.toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun save(job: Job): Job {
        val response = jobApi.save(job.toDto())
        if (!response.isSuccessful) {
            throw Exception("Failed to save job: ${response.code()}")
        }
        val savedJob = response.body()!!.toModel()
        return savedJob
    }

    override suspend fun removeById(id: Long) {
        val response = jobApi.removeById(id)
        if (!response.isSuccessful) {
            throw Exception("Failed to remove job: ${response.code()}")
        }
    }
    private fun validateJobDateFormat(job: Job) {
        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2}")

        if (!dateRegex.matches(job.start)) {
            throw IllegalArgumentException("Invalid start date format: ${job.start}. Expected: yyyy-MM-dd")
        }

        job.finish?.let { finish ->
            if (!dateRegex.matches(finish)) {
                throw IllegalArgumentException("Invalid finish date format: $finish. Expected: yyyy-MM-dd")
            }
        }
    }
}