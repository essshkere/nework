package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.databinding.ItemJobBinding
import ru.netology.nework.data.Job
import java.text.SimpleDateFormat
import java.util.Locale

class JobAdapter : ListAdapter<Job, JobAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: Job) {
            binding.apply {
                companyNameTextView.text = job.name
                positionTextView.text = job.position

                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val startDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(job.start)!!)
                val finishDate = job.finish?.let {
                    dateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)!!)
                } ?: "по настоящее время"

                durationTextView.text = "$startDate - $finishDate"

                job.link?.let { link ->
                    linkTextView.visibility = View.VISIBLE
                    linkTextView.text = link
                } ?: run {
                    linkTextView.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Job>() {
            override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
                return oldItem == newItem
            }
        }
    }
}