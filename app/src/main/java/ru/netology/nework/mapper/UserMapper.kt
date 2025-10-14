package ru.netology.nework.mapper

import ru.netology.nework.data.Job
import ru.netology.nework.data.User
import ru.netology.nework.data.UserEntity
import ru.netology.nework.dto.JobDto
import ru.netology.nework.dto.UserDto

fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    login = login,
    name = name,
    avatar = avatar
)

fun UserEntity.toModel(): User = User(
    id = id,
    login = login,
    name = name,
    avatar = avatar
)

fun User.toDto(): UserDto {
    return UserDto(
        id = id,
        login = login,
        name = name,
        avatar = avatar
    )
}

fun JobDto.toModel(): Job = Job(
    id = id,
    name = name,
    position = position,
    start = start,
    finish = finish,
    link = link
)
fun Job.toDto(): JobDto {
    val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
    if (!dateRegex.matches(start)) {
        throw IllegalArgumentException("Invalid start date format in Job: $start. Expected: yyyy-MM-dd")
    }
    finish?.let {
        if (!dateRegex.matches(it)) {
            throw IllegalArgumentException("Invalid finish date format in Job: $it. Expected: yyyy-MM-dd")
        }
    }

    return JobDto(
        id = id,
        name = name,
        position = position,
        start = start,
        finish = finish,
        link = link
    )
}