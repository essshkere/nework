package ru.netology.nework.repository

import ru.netology.nework.api.JobApi
import ru.netology.nework.data.Job
import ru.netology.nework.mapper.toDto
import ru.netology.nework.mapper.toModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val jobApi: JobApi
) : JobRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override suspend fun getMyJobs(): List<Job> {
        return try {
            val response = jobApi.getMyJobs()
            if (response.isSuccessful) {
                response.body()?.map { it.toModel() } ?: emptyList()
            } else {
                when (response.code()) {
                    403 -> throw Exception("Необходимо авторизоваться для просмотра работ")
                    else -> emptyList()
                }
            }
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при загрузке работ")
                else -> Exception("Ошибка загрузки работ: ${e.message}")
            }
        }
    }

    override suspend fun save(job: Job): Job {
        try {
            validateJobData(job)
            val jobDto = job.toDto()
            val response = jobApi.save(jobDto)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    403 -> Exception("Необходимо авторизоваться для сохранения работы")
                    else -> Exception("Ошибка сохранения работы: ${response.code()}")
                }
            }

            val savedJob = response.body()!!.toModel()
            return savedJob
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при сохранении работы")
                e.message?.contains("формат даты") == true ->
                    Exception("Неправильный формат даты. Используйте формат ГГГГ-ММ-ДД")
                e.message?.contains("дата начала") == true ->
                    Exception("Дата начала не может быть позже даты окончания")
                e.message?.contains("дата окончания") == true ->
                    Exception("Дата окончания не может быть в будущем")
                else -> Exception("Ошибка при сохранении работы: ${e.message}")
            }
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = jobApi.removeById(id)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    403 -> Exception("Необходимо авторизоваться для удаления работы")
                    404 -> Exception("Работа не найдена")
                    else -> Exception("Ошибка удаления работы: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при удалении работы")
                else -> Exception("Ошибка при удалении работы: ${e.message}")
            }
        }
    }

    private fun validateJobData(job: Job) {
        if (!isValidDateFormat(job.start)) {
            throw IllegalArgumentException("Неверный формат даты начала: ${job.start}. Ожидается: yyyy-MM-dd")
        }

        job.finish?.let { finish ->
            if (!isValidDateFormat(finish)) {
                throw IllegalArgumentException("Неверный формат даты окончания: $finish. Ожидается: yyyy-MM-dd")
            }
        }

        validateJobDates(job.start, job.finish)

        if (job.name.isBlank()) {
            throw IllegalArgumentException("Название компании не может быть пустым")
        }

        if (job.name.length < 2) {
            throw IllegalArgumentException("Название компании должно содержать минимум 2 символа")
        }

        if (job.position.isBlank()) {
            throw IllegalArgumentException("Должность не может быть пустой")
        }

        if (job.position.length < 2) {
            throw IllegalArgumentException("Должность должна содержать минимум 2 символа")
        }

        job.link?.let { link ->
            if (link.isNotBlank() && !isValidUrl(link)) {
                throw IllegalArgumentException("Некорректный формат ссылки")
            }
        }
    }

    private fun validateJobDates(start: String, finish: String?) {
        try {
            val startDate = dateFormat.parse(start) ?: return

            finish?.let { finishDateStr ->
                val finishDate = dateFormat.parse(finishDateStr) ?: return

                if (startDate.after(finishDate)) {
                    throw IllegalArgumentException("Дата начала не может быть позже даты окончания")
                }

                if (finishDate.after(Date())) {
                    throw IllegalArgumentException("Дата окончания не может быть в будущем")
                }
            }

            if (startDate.after(Date())) {
                throw IllegalArgumentException("Дата начала не может быть в будущем")
            }

        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> throw e
                else -> throw IllegalArgumentException("Ошибка валидации дат: ${e.message}")
            }
        }
    }

    private fun isValidDateFormat(dateString: String): Boolean {
        return try {
            dateFormat.parse(dateString)
            dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val urlRegex = Regex("^https?://(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$")
        return urlRegex.matches(url)
    }

    override fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString)
            displayDateFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun parseDateFromDisplay(displayDate: String): String {
        return try {
            val date = displayDateFormat.parse(displayDate)
            dateFormat.format(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("Неверный формат даты: $displayDate. Ожидается: ДД.ММ.ГГГГ")
        }
    }

    override fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    override fun isCurrentJob(job: Job): Boolean {
        return job.finish == null
    }
}