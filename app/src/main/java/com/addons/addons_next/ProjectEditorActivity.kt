package com.addons.addons_next

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.addons.addons_next.databinding.ActivityProjectEditorBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

class ProjectEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectEditorBinding
    private lateinit var projectStorage: AddonProjectStorage
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private lateinit var projectRoot: File

    private var projectId: String = ""
    private var projectName: String = ""
    private var loadedContent: String = ""
    private var currentFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityProjectEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectStorage = AddonProjectStorage(this)
        projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        projectName = intent.getStringExtra(EXTRA_PROJECT_NAME).orEmpty()
        val projectRootPath = intent.getStringExtra(EXTRA_PROJECT_ROOT).orEmpty()

        if (projectId.isBlank() || projectName.isBlank() || projectRootPath.isBlank()) {
            Toast.makeText(this, getString(R.string.editor_load_root_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        projectRoot = File(projectRootPath)
        if (!projectRoot.exists() || !projectRoot.isDirectory) {
            Toast.makeText(this, getString(R.string.editor_load_root_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fileTreeAdapter = FileTreeAdapter(::handleFileSelected)

        setupViews()
        applyInsets()
        bindActions()
        refreshTree(openFirstFileIfNeeded = true)
    }

    override fun onPause() {
        persistCurrentFile(showFeedback = false)
        super.onPause()
    }

    override fun onDestroy() {
        binding.codeEditor.release()
        super.onDestroy()
    }

    private fun setupViews() = with(binding) {
        editorTitleText.text = projectName
        editorWorkspacePathText.text = getString(R.string.editor_workspace_path, projectRoot.absolutePath)
        fileTreeRecycler.layoutManager = LinearLayoutManager(this@ProjectEditorActivity)
        fileTreeRecycler.adapter = fileTreeAdapter
        fileTreeRecycler.setHasFixedSize(true)

        codeEditor.typefaceText = Typeface.MONOSPACE
        codeEditor.setText("")
        showEmptyEditor()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.editorRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.editorTopBar.updatePadding(
                left = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.left,
                top = resources.getDimensionPixelSize(R.dimen.space_8) + systemBars.top,
                right = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.right
            )
            binding.editorContent.updatePadding(
                left = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.left,
                right = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.right,
                bottom = resources.getDimensionPixelSize(R.dimen.space_16) + systemBars.bottom
            )

            insets
        }
        ViewCompat.requestApplyInsets(binding.editorRoot)
    }

    private fun bindActions() = with(binding) {
        onBackPressedDispatcher.addCallback(
            this@ProjectEditorActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishEditor()
                }
            }
        )
        editorBackButton.setOnClickListener { finishEditor() }
        editorRefreshButton.setOnClickListener {
            refreshTree(openFirstFileIfNeeded = currentFile?.exists() != true)
        }
        editorSaveButton.setOnClickListener { persistCurrentFile(showFeedback = true) }
    }

    private fun refreshTree(openFirstFileIfNeeded: Boolean) {
        fileTreeAdapter.submitTree(projectRoot, currentFile)
        if (openFirstFileIfNeeded) {
            val firstFile = findFirstEditableFile(projectRoot)
            if (firstFile != null) {
                openFile(firstFile)
            } else {
                currentFile = null
                loadedContent = ""
                showEmptyEditor()
                binding.editorStatusText.text = getString(R.string.editor_tree_empty)
                fileTreeAdapter.setSelectedFile(null)
            }
        }
    }

    private fun handleFileSelected(file: File) {
        if (!persistCurrentFile(showFeedback = false)) {
            return
        }
        openFile(file)
    }

    private fun openFile(file: File) {
        if (!file.exists() || !file.isFile || !isChildOfProject(file)) {
            Snackbar.make(binding.root, getString(R.string.editor_open_failed, file.name), Snackbar.LENGTH_LONG).show()
            return
        }
        if (!isEditableTextFile(file)) {
            Snackbar.make(binding.root, getString(R.string.editor_binary_unsupported), Snackbar.LENGTH_LONG).show()
            return
        }
        if (file.length() > MAX_EDITABLE_FILE_BYTES) {
            Snackbar.make(
                binding.root,
                getString(R.string.editor_file_too_large, MAX_EDITABLE_FILE_BYTES / 1024),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        runCatching {
            file.readText()
        }.onSuccess { content ->
            currentFile = file
            loadedContent = content
            binding.codeEditor.setText(content)
            binding.codeEditor.isVisible = true
            binding.editorEmptyState.isVisible = false
            binding.currentFileValueText.text = relativePath(file)
            binding.editorStatusText.text = getString(R.string.editor_status_ready)
            fileTreeAdapter.setSelectedFile(file)
        }.onFailure { error ->
            Snackbar.make(
                binding.root,
                getString(R.string.editor_open_failed, error.message ?: file.name),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun persistCurrentFile(showFeedback: Boolean): Boolean {
        val file = currentFile ?: run {
            if (showFeedback) {
                Snackbar.make(binding.root, getString(R.string.editor_tree_empty), Snackbar.LENGTH_SHORT).show()
            }
            return true
        }
        val currentContent = binding.codeEditor.text.toString()
        if (currentContent == loadedContent) {
            if (showFeedback) {
                Snackbar.make(binding.root, getString(R.string.editor_no_changes), Snackbar.LENGTH_SHORT).show()
            }
            return true
        }

        return runCatching {
            file.writeText(currentContent)
            loadedContent = currentContent
            projectStorage.touchProject(projectId)
            binding.editorStatusText.text = getString(R.string.editor_status_saved)
            if (showFeedback) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.editor_save_success, relativePath(file)),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            true
        }.getOrElse { error ->
            Snackbar.make(
                binding.root,
                getString(R.string.editor_save_failed, error.message ?: file.name),
                Snackbar.LENGTH_LONG
            ).show()
            false
        }
    }

    private fun finishEditor() {
        if (persistCurrentFile(showFeedback = false)) {
            finish()
        } else {
            Snackbar.make(binding.root, getString(R.string.editor_leave_save_failed), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showEmptyEditor() = with(binding) {
        codeEditor.isVisible = false
        editorEmptyState.isVisible = true
        currentFileValueText.text = getString(R.string.editor_current_file_empty)
    }

    private fun relativePath(file: File): String {
        return file.absolutePath.removePrefix(projectRoot.absolutePath).trimStart(File.separatorChar)
    }

    private fun isChildOfProject(file: File): Boolean {
        val projectRootPath = projectRoot.canonicalFile
        val candidatePath = file.canonicalFile
        return candidatePath == projectRootPath ||
            candidatePath.path.startsWith("${projectRootPath.path}${File.separator}")
    }

    private fun isEditableTextFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        if (extension in BINARY_EXTENSIONS) {
            return false
        }
        return true
    }

    private fun findFirstEditableFile(directory: File): File? {
        val children = directory.listFiles()
            ?.filterNot { it.name.startsWith(".") }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .orEmpty()

        children.forEach { child ->
            if (child.isDirectory) {
                val nested = findFirstEditableFile(child)
                if (nested != null) {
                    return nested
                }
            } else if (isEditableTextFile(child) && child.length() <= MAX_EDITABLE_FILE_BYTES) {
                return child
            }
        }
        return null
    }

    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_PROJECT_NAME = "project_name"
        const val EXTRA_PROJECT_ROOT = "project_root"

        private const val MAX_EDITABLE_FILE_BYTES = 512 * 1024L
        private val BINARY_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "webp", "gif", "bmp",
            "ogg", "mp3", "wav", "ttf", "otf", "zip", "mcaddon", "mcpack"
        )
    }
}
