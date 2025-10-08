package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.data.Job
import ru.netology.nework.repository.JobRepository
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _jobs = MutableLiveData<List<Job>>()
    val jobs: LiveData<List<Job>> = _jobs

    private val _state = MutableLiveData<JobsState>()
    val state: LiveData<JobsState> = _state

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _state.value = JobsState.Loading
            try {
                val jobsList = repository.getMyJobs()
                _jobs.value = jobsList
                _state.value = JobsState.Success
            } catch (e: Exception) {
                _state.value = JobsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveJob(job: Job) {
        viewModelScope.launch {
            _state.value = JobsState.Loading
            try {
                repository.save(job)
                loadJobs()
            } catch (e: Exception) {
                _state.value = JobsState.Error(e.message ?: "Failed to save job")
            }
        }
    }

    fun removeJob(id: Long) {
        viewModelScope.launch {
            _state.value = JobsState.Loading
            try {
                repository.removeById(id)
                loadJobs()
            } catch (e: Exception) {
                _state.value = JobsState.Error(e.message ?: "Failed to remove job")
            }
        }
    }

    sealed class JobsState {
        object Loading : JobsState()
        object Success : JobsState()
        data class Error(val message: String) : JobsState()
    }
}