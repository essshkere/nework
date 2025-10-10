package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.EventApi
import ru.netology.nework.dao.EventDao
import ru.netology.nework.data.Event
import ru.netology.nework.mapper.toDto
import ru.netology.nework.mapper.toEntity
import ru.netology.nework.mapper.toModel
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.EventApi
import ru.netology.nework.api.MediaApi
import ru.netology.nework.dao.EventDao
import ru.netology.nework.data.Event
import ru.netology.nework.dto.EventDto
import ru.netology.nework.mapper.toEntity
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventApi: EventApi,
    private val eventDao: EventDao,
    private val mediaApi: MediaApi
) : EventRepository {

    override fun getPagingData() = eventDao.pagingSource()
        .map { it.toModel() }

    override suspend fun getAll() {
        try {
            val response = eventApi.getAll()
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    eventDao.insert(events.map { it.toEntity() })
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка загрузки событий: ${e.message}")
        }
    }

    override suspend fun likeById(id: Long) {
        try {
            val response = eventApi.likeById(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для лайка")
                    404 -> Exception("Событие не найдено")
                    else -> Exception("Ошибка лайка: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка лайка события: ${e.message}")
        }
    }

    override suspend fun dislikeById(id: Long) {
        try {
            val response = eventApi.dislikeById(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для дизлайка")
                    404 -> Exception("Событие не найдено")
                    else -> Exception("Ошибка дизлайка: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка дизлайка события: ${e.message}")
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = eventApi.removeById(id)
            if (response.isSuccessful) {
                eventDao.removeById(id)
            } else {
                throw Exception("Ошибка удаления события: ${response.code()}")
            }
        } catch (e: Exception) {
            throw Exception("Ошибка удаления события: ${e.message}")
        }
    }

    private fun createEventDto(event: Event, uploadedAttachment: Event.Attachment?): EventDto {
        return EventDto(
            id = event.id,
            authorId = event.authorId,
            author = event.author,
            authorAvatar = event.authorAvatar,
            authorJob = event.authorJob,
            content = event.content,
            datetime = event.datetime,
            published = event.published,
            coords = event.coords?.let { coords ->
                ru.netology.nework.dto.CoordinatesDto(
                    lat = coords.lat.toString(),
                    long = coords.long.toString()
                )
            },
            type = when (event.type) {
                Event.EventType.ONLINE -> ru.netology.nework.dto.EventTypeDto.ONLINE
                Event.EventType.OFFLINE -> ru.netology.nework.dto.EventTypeDto.OFFLINE
            },
            likeOwnerIds = event.likeOwnerIds,
            likedByMe = event.likedByMe,
            speakerIds = event.speakerIds,
            participantsIds = event.participantsIds,
            participatedByMe = event.participatedByMe,
            attachment = uploadedAttachment?.let { attachment ->
                ru.netology.nework.dto.AttachmentDto(
                    url = attachment.url,
                    type = when (attachment.type) {
                        Event.AttachmentType.IMAGE -> ru.netology.nework.dto.AttachmentTypeDto.IMAGE
                        Event.AttachmentType.VIDEO -> ru.netology.nework.dto.AttachmentTypeDto.VIDEO
                        Event.AttachmentType.AUDIO -> ru.netology.nework.dto.AttachmentTypeDto.AUDIO
                    }
                )
            },
            link = event.link,
            users = emptyMap() // Сервер сам заполнит это поле
        )
    }


    override suspend fun save(event: Event) {
        try {
            val uploadedAttachment = event.attachment?.let { attachment ->
                if (attachment.url.startsWith("content://") || attachment.url.startsWith("file://")) {
                    uploadMediaFile(Uri.parse(attachment.url), attachment.type)
                } else {
                    attachment
                }
            }
            val eventDto = createEventDto(event, uploadedAttachment)
            val response = eventApi.save(eventDto)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для создания события")
                    else -> Exception("Ошибка сохранения события: ${response.code()}")
                }
            }

            response.body()?.let { eventDto ->
                eventDao.insert(eventDto.toEntity())
            }
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при сохранении события")
                e.message?.contains("15", ignoreCase = true) == true ->
                    Exception("Размер файла превышает 15 МБ")
                else -> Exception("Ошибка сохранения события: ${e.message}")
            }
        }
    }
    private suspend fun uploadMediaFile(
        fileUri: Uri,
        attachmentType: Event.AttachmentType
    ): Event.Attachment = withContext(Dispatchers.IO) {
        try {
            val file = createTempFileFromUri(fileUri)
            if (file.length() > 15 * 1024 * 1024) {
                throw Exception("Размер файла превышает 15 МБ")
            }

            val mimeType = when (attachmentType) {
                Event.AttachmentType.IMAGE -> "image/*"
                Event.AttachmentType.VIDEO -> "video/*"
                Event.AttachmentType.AUDIO -> "audio/*"
            }
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = mediaApi.uploadMedia(filePart)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для загрузки медиа")
                    415 -> Exception("Неподдерживаемый формат файла")
                    else -> Exception("Ошибка загрузки медиа: ${response.code()}")
                }
            }
            val mediaDto = response.body() ?: throw Exception("Пустой ответ при загрузке медиа")
            file.delete()
            return@withContext Event.Attachment(mediaDto.url, attachmentType)
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при загрузке медиа")
                else -> Exception("Ошибка загрузки файла: ${e.message}")
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val context = ru.netology.nework.App.applicationContext()
        val file = File.createTempFile("media_upload", ".tmp", context.cacheDir)
        file.deleteOnExit()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw Exception("Не удалось открыть файл")

        return file
    }

    override suspend fun participate(id: Long) {
        try {
            val response = eventApi.participate(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для участия")
                    404 -> Exception("Событие не найдено")
                    else -> Exception("Ошибка участия: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка участия в событии: ${e.message}")
        }
    }

    override suspend fun unparticipate(id: Long) {
        try {
            val response = eventApi.unparticipate(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для отказа от участия")
                    404 -> Exception("Событие не найдено")
                    else -> Exception("Ошибка отказа от участия: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка отказа от участия в событии: ${e.message}")
        }
    }

    override suspend fun getById(id: Long): Event? {
        return try {
            val cachedEvent = eventDao.getById(id)?.toModel()
            if (cachedEvent != null) {
                return cachedEvent
            }

            val response = eventApi.getById(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    val event = eventDto.toEntity().toModel()
                    eventDao.insert(eventDto.toEntity())
                    event
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}