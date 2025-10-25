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
    var onJobClicked: ((Job) -> Unit)? = null

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

                val periodText = formatJobPeriod(job.start, job.finish)
                durationTextView.text = periodText

                job.link?.let { link ->
                    linkTextView.visibility = View.VISIBLE
                    linkTextView.text = "🔗 ${getDomainFromUrl(link)}"
                } ?: run {
                    linkTextView.visibility = View.GONE
                }

                root.setOnClickListener {
                    onJobClicked?.invoke(job)
                }

                if (job.finish == null) {
                    currentJobIndicator.visibility = View.VISIBLE
                    currentJobIndicator.text = "Текущая работа"
                } else {
                    currentJobIndicator.visibility = View.GONE
                }
            }
        }

        private fun formatJobPeriod(start: String, finish: String?): String {
            val startFormatted = formatDate(start)
            val finishFormatted = finish?.let { formatDate(it) } ?: "настоящее время"

            return "$startFormatted - $finishFormatted"
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString
            }
        }

        private fun getDomainFromUrl(url: String): String {
            return try {
                val domain = url
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .removePrefix("www.")
                    .split("/")[0]
                domain
            } catch (e: Exception) {
                url
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