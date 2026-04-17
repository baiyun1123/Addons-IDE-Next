package com.addons.addons_next

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.RecyclerView
import com.addons.addons_next.databinding.ItemFileTreeBinding
import java.io.File
import java.util.Locale

class FileTreeAdapter(
    private val onFileSelected: (File) -> Unit
) : RecyclerView.Adapter<FileTreeAdapter.FileTreeViewHolder>() {

    private var rootDirectory: File? = null
    private var selectedPath: String? = null
    private var entries: List<FileTreeEntry> = emptyList()
    private val expandedDirectoryPaths = linkedSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileTreeViewHolder {
        val binding = ItemFileTreeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileTreeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileTreeViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    fun submitTree(root: File, selectedFile: File?) {
        rootDirectory = root
        selectedPath = selectedFile?.absolutePath
        if (expandedDirectoryPaths.isEmpty()) {
            root.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.forEach { expandedDirectoryPaths.add(it.absolutePath) }
        }
        rebuildEntries()
    }

    fun setSelectedFile(file: File?) {
        selectedPath = file?.absolutePath
        notifyDataSetChanged()
    }

    private fun rebuildEntries() {
        val currentRoot = rootDirectory ?: run {
            entries = emptyList()
            notifyDataSetChanged()
            return
        }

        entries = buildList {
            addChildren(currentRoot, depth = 0)
        }
        notifyDataSetChanged()
    }

    private fun MutableList<FileTreeEntry>.addChildren(directory: File, depth: Int) {
        val children = directory.listFiles()
            ?.filterNot { it.name.startsWith(".") }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
            .orEmpty()

        children.forEach { child ->
            val isExpanded = child.isDirectory && expandedDirectoryPaths.contains(child.absolutePath)
            add(
                FileTreeEntry(
                    file = child,
                    depth = depth,
                    isDirectory = child.isDirectory,
                    isExpanded = isExpanded
                )
            )
            if (child.isDirectory && isExpanded) {
                addChildren(child, depth + 1)
            }
        }
    }

    private fun toggleDirectory(directory: File) {
        val path = directory.absolutePath
        if (!expandedDirectoryPaths.add(path)) {
            expandedDirectoryPaths.remove(path)
        }
        rebuildEntries()
    }

    inner class FileTreeViewHolder(
        private val binding: ItemFileTreeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: FileTreeEntry) = with(binding) {
            val context = root.context
            val file = entry.file
            fileTreeItemRoot.isActivated = file.absolutePath == selectedPath
            fileTreeItemRoot.updatePaddingRelative(
                start = context.resources.getDimensionPixelSize(R.dimen.space_16) +
                    (entry.depth * context.resources.getDimensionPixelSize(R.dimen.space_16))
            )

            nodeIndicatorText.text = when {
                entry.isDirectory && entry.isExpanded -> "-"
                entry.isDirectory -> "+"
                else -> "."
            }
            nodeNameText.text = file.name
            nodeMetaText.isVisible = true
            nodeMetaText.text = if (entry.isDirectory) {
                context.getString(R.string.editor_tree_folder_meta)
            } else {
                context.getString(
                    R.string.editor_tree_file_meta,
                    file.extension.ifBlank { "TEXT" }.uppercase(Locale.ROOT),
                    formatFileSize(file.length())
                )
            }

            fileTreeItemRoot.setOnClickListener {
                if (entry.isDirectory) {
                    toggleDirectory(file)
                } else {
                    onFileSelected(file)
                }
            }
        }
    }

    data class FileTreeEntry(
        val file: File,
        val depth: Int,
        val isDirectory: Boolean,
        val isExpanded: Boolean
    )

    companion object {
        private fun formatFileSize(sizeInBytes: Long): String {
            return when {
                sizeInBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeInBytes / 1024f / 1024f)
                sizeInBytes >= 1024 -> String.format(Locale.US, "%.1f KB", sizeInBytes / 1024f)
                else -> "$sizeInBytes B"
            }
        }
    }
}
