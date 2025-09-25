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

fun User.toDto(): UserDto = UserDto(
    id = id,
    login = login,
    name = name,
    avatar = avatar
)

fun JobDto.toModel(): Job = Job(
    id = id,
    name = name,
    position = position,
    start = start,
    finish = finish,
    link = link
)

fun Job.toDto(): JobDto = JobDto(
    id = id,
    name = name,
    position = position,
    start = start,
    finish = finish,
    link = link
)