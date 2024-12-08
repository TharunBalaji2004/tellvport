package com.ldihackos.tellvport.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.ldihackos.tellvport.models.PlatformData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ldihackos.tellvport.databinding.PlatformItemBinding

class PlatformsAdapter : ListAdapter<PlatformData, PlatformsAdapter.PlatformsViewHolder>(ComparatorDiffUtil()) {

    inner class PlatformsViewHolder(private val binding: PlatformItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(note: PlatformData) {
            binding.tvPlatformName.text = note.platform_name.uppercase()
            binding.tvTrainDetails.text = "${note.platform_lastest_train_name} (${note.platform_lastest_train_number})"
            //binding.tvRoute.text = ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformsViewHolder {
        val view = PlatformItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlatformsViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlatformsViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class ComparatorDiffUtil: DiffUtil.ItemCallback<PlatformData>() {
        override fun areItemsTheSame(oldItem: PlatformData, newItem: PlatformData): Boolean {
            return oldItem.platform_id == newItem.platform_id
        }

        override fun areContentsTheSame(oldItem: PlatformData, newItem: PlatformData): Boolean {
            return oldItem == newItem
        }
    }


}