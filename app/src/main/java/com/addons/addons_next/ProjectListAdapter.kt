package com.addons.addons_next

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.addons.addons_next.databinding.ItemProjectBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectListAdapter(
    private val onOpenEditor: (AddonProject) -> Unit,
    private val onShowLocation: (AddonProject) -> Unit
) : ListAdapter<AddonProject, ProjectListAdapter.ProjectViewHolder>(ProjectDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: AddonProject) = with(binding) {
            val context = root.context
            projectNameText.text = project.name
            projectMetaText.text = context.getString(
                R.string.project_meta_format,
                project.namespace,
                project.packVersion,
                project.minEngineVersion
            )
            projectDescriptionText.text = project.description
            projectTypeChip.text = when {
                project.hasBehaviorPack && project.hasResourcePack -> context.getString(R.string.project_type_full)
                project.hasBehaviorPack -> context.getString(R.string.project_type_behavior_only)
                else -> context.getString(R.string.project_type_resource_only)
            }
            projectPathText.text = project.rootPath
            projectUpdatedText.text = context.getString(
                R.string.project_updated_format,
                timestampFormat.format(Date(project.updatedAt))
            )

            openEditorButton.setOnClickListener { onOpenEditor(project) }
            showLocationButton.setOnClickListener { onShowLocation(project) }
        }
    }

    private object ProjectDiffCallback : DiffUtil.ItemCallback<AddonProject>() {
        override fun areItemsTheSame(oldItem: AddonProject, newItem: AddonProject): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AddonProject, newItem: AddonProject): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }
}
