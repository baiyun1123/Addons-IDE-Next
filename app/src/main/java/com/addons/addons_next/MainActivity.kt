package com.addons.addons_next

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.addons.addons_next.databinding.ActivityMainBinding
import com.addons.addons_next.databinding.DialogCreateProjectBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectStorage: AddonProjectStorage
    private lateinit var projectListAdapter: ProjectListAdapter

    private var projects: List<AddonProject> = emptyList()
    private var settings: UserSettings = UserSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectStorage = AddonProjectStorage(this)
        settings = projectStorage.loadSettings()
        projectListAdapter = ProjectListAdapter(
            onOpenEditor = ::showEditorPlaceholder,
            onShowLocation = ::showProjectLocation
        )

        setupViews()
        applyInsets()
        bindActions()
        renderSettings()
        refreshProjects()
        switchPage(R.id.menu_home)
        startIntroAnimation()
    }

    private fun setupViews() = with(binding) {
        projectRecycler.layoutManager = LinearLayoutManager(this@MainActivity)
        projectRecycler.adapter = projectListAdapter
        projectRecycler.setHasFixedSize(true)
        projectRecycler.itemAnimator = null
        projectRecycler.isNestedScrollingEnabled = false
        profileDocLinksText.text = getString(R.string.profile_doc_links)
        workspacePathText.text = getString(
            R.string.profile_workspace_format,
            projectStorage.workspaceRoot().absolutePath
        )
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.topBar.updatePadding(
                top = resources.getDimensionPixelSize(R.dimen.space_8) + systemBars.top
            )
            binding.pageContainer.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = resources.getDimensionPixelSize(R.dimen.bottom_navigation_space) + systemBars.bottom
            )
            binding.bottomNavigation.updatePadding(
                left = resources.getDimensionPixelSize(R.dimen.screen_padding),
                right = resources.getDimensionPixelSize(R.dimen.screen_padding),
                bottom = resources.getDimensionPixelSize(R.dimen.space_8) + systemBars.bottom
            )
            binding.addProjectFab.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.fab_bottom_margin) + systemBars.bottom
                rightMargin = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.right
            }

            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun bindActions() = with(binding) {
        bottomNavigation.setOnItemSelectedListener { item ->
            switchPage(item.itemId)
            true
        }
        addProjectFab.setOnClickListener { showCreateProjectSheet() }
        emptyCreateButton.setOnClickListener { showCreateProjectSheet() }
        saveSettingsButton.setOnClickListener { saveSettings() }
    }

    private fun renderSettings() = with(binding) {
        profileAuthorInput.setText(settings.authorId)
        profileVersionInput.setText(settings.minEngineVersion)
        profileDescriptionInput.setText(settings.defaultDescription)
        profileResourceSwitch.isChecked = settings.includeResourcePackByDefault
    }

    private fun refreshProjects() {
        projects = projectStorage.loadProjects()
        projectListAdapter.submitList(projects)
        binding.emptyStateGroup.isVisible = projects.isEmpty()
        binding.projectRecycler.isVisible = projects.isNotEmpty()
        binding.projectCountValue.text = projects.size.toString()
        binding.latestProjectValue.text = projects.firstOrNull()?.name
            ?: getString(R.string.home_stat_latest_empty)
    }

    private fun switchPage(menuId: Int) = with(binding) {
        val isHome = menuId == R.id.menu_home
        homePage.isVisible = isHome
        profilePage.isVisible = !isHome
        addProjectFab.isVisible = isHome

        if (isHome) {
            topbarTitle.text = getString(R.string.home_page_title)
            topbarSubtitle.text = getString(R.string.home_page_subtitle)
            topbarBadge.text = getString(R.string.topbar_badge)
        } else {
            topbarTitle.text = getString(R.string.profile_page_title)
            topbarSubtitle.text = getString(R.string.profile_page_subtitle)
            topbarBadge.text = getString(R.string.nav_profile)
        }
    }

    private fun showCreateProjectSheet() {
        val dialogBinding = DialogCreateProjectBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.projectAuthorInput.setText(settings.authorId)
        dialogBinding.projectVersionInput.setText(settings.minEngineVersion)
        dialogBinding.projectDescriptionInput.setText(settings.defaultDescription)
        dialogBinding.resourcePackSwitch.isChecked = settings.includeResourcePackByDefault
        dialogBinding.copyLatestButton.isEnabled = projects.isNotEmpty()

        var namespaceEditedManually = false
        var updatingNamespaceFromName = false

        dialogBinding.projectNamespaceInput.doAfterTextChanged {
            if (!updatingNamespaceFromName) {
                namespaceEditedManually = !it.isNullOrBlank()
            }
        }
        dialogBinding.projectNameInput.doAfterTextChanged { editable ->
            if (!namespaceEditedManually) {
                updatingNamespaceFromName = true
                dialogBinding.projectNamespaceInput.setText(
                    AddonProjectStorage.sanitizeIdentifier(editable?.toString().orEmpty())
                )
                dialogBinding.projectNamespaceInput.setSelection(
                    dialogBinding.projectNamespaceInput.text?.length ?: 0
                )
                updatingNamespaceFromName = false
            }
        }
        dialogBinding.copyLatestButton.setOnClickListener {
            val latestProject = projects.firstOrNull()
            if (latestProject == null) {
                Snackbar.make(binding.root, getString(R.string.copy_latest_empty), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialogBinding.projectNameInput.setText(latestProject.name)
            dialogBinding.projectNamespaceInput.setText("${latestProject.namespace}_copy")
            dialogBinding.projectAuthorInput.setText(latestProject.authorId)
            dialogBinding.projectDescriptionInput.setText(latestProject.description)
            dialogBinding.projectVersionInput.setText(latestProject.minEngineVersion)
            dialogBinding.behaviorPackSwitch.isChecked = latestProject.hasBehaviorPack
            dialogBinding.resourcePackSwitch.isChecked = latestProject.hasResourcePack
            Snackbar.make(binding.root, getString(R.string.copy_latest_done), Snackbar.LENGTH_SHORT).show()
        }
        dialogBinding.cancelCreateButton.setOnClickListener { dialog.dismiss() }
        dialogBinding.createProjectButton.setOnClickListener {
            createProjectFromDialog(dialogBinding, dialog)
        }

        dialog.show()
    }

    private fun createProjectFromDialog(
        dialogBinding: DialogCreateProjectBinding,
        dialog: BottomSheetDialog
    ) {
        val name = dialogBinding.projectNameInput.text?.toString()?.trim().orEmpty()
        val namespaceInput = dialogBinding.projectNamespaceInput.text?.toString()?.trim().orEmpty()
        val authorId = dialogBinding.projectAuthorInput.text?.toString()?.trim().orEmpty()
        val description = dialogBinding.projectDescriptionInput.text?.toString()?.trim().orEmpty()
        val minEngineVersion = dialogBinding.projectVersionInput.text?.toString()?.trim().orEmpty()
        val includeBehaviorPack = dialogBinding.behaviorPackSwitch.isChecked
        val includeResourcePack = dialogBinding.resourcePackSwitch.isChecked

        if (name.isBlank() || (!includeBehaviorPack && !includeResourcePack)) {
            Snackbar.make(binding.root, getString(R.string.create_project_validation), Snackbar.LENGTH_SHORT).show()
            return
        }

        val request = CreateProjectRequest(
            name = name,
            namespace = namespaceInput,
            authorId = authorId.ifBlank { settings.authorId },
            description = description.ifBlank { settings.defaultDescription },
            minEngineVersion = minEngineVersion.ifBlank { settings.minEngineVersion },
            includeBehaviorPack = includeBehaviorPack,
            includeResourcePack = includeResourcePack
        )

        runCatching {
            projectStorage.createProject(request)
        }.onSuccess { project ->
            refreshProjects()
            binding.projectRecycler.scrollToPosition(0)
            binding.bottomNavigation.selectedItemId = R.id.menu_home
            dialog.dismiss()
            Snackbar.make(
                binding.root,
                getString(R.string.create_project_success, project.name),
                Snackbar.LENGTH_SHORT
            ).show()
        }.onFailure { error ->
            Snackbar.make(
                binding.root,
                getString(R.string.create_project_error, error.message ?: "unknown"),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun saveSettings() {
        val authorId = binding.profileAuthorInput.text?.toString()?.trim().orEmpty()
        val minEngineVersion = binding.profileVersionInput.text?.toString()?.trim().orEmpty()
        val description = binding.profileDescriptionInput.text?.toString()?.trim().orEmpty()

        val newSettings = UserSettings(
            authorId = authorId.ifBlank { "studio.author" },
            minEngineVersion = minEngineVersion.ifBlank { AddonProjectStorage.DEFAULT_ENGINE_VERSION },
            defaultDescription = description.ifBlank { "Generated with Addons IDE Next" },
            includeResourcePackByDefault = binding.profileResourceSwitch.isChecked
        )

        runCatching {
            projectStorage.validateEngineVersion(newSettings.minEngineVersion)
            projectStorage.saveSettings(newSettings)
        }.onSuccess {
            settings = newSettings
            Snackbar.make(binding.root, getString(R.string.settings_saved), Snackbar.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, getString(R.string.settings_validation_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditorPlaceholder(project: AddonProject) {
        startActivity(
            Intent(this, ProjectEditorActivity::class.java)
                .putExtra(ProjectEditorActivity.EXTRA_PROJECT_ID, project.id)
                .putExtra(ProjectEditorActivity.EXTRA_PROJECT_NAME, project.name)
                .putExtra(ProjectEditorActivity.EXTRA_PROJECT_ROOT, project.rootPath)
        )
    }

    private fun showProjectLocation(project: AddonProject) {
        Snackbar.make(
            binding.root,
            getString(R.string.project_location_feedback, project.rootPath),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun startIntroAnimation() {
        binding.root.doOnPreDraw {
            val listView = if (binding.emptyStateGroup.isVisible) binding.emptyStateGroup else binding.projectRecycler
            val animatedViews = listOf(
                binding.topBar,
                binding.homeSummaryCard,
                binding.projectListSection.takeIf { binding.homePage.isVisible } ?: listView,
                binding.bottomNavigation,
                binding.addProjectFab
            )

            animatedViews.forEachIndexed { index, view ->
                view.prepareForEntrance()
                view.postDelayed({
                    view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(360L)
                        .start()
                }, index * 70L)
            }
        }
    }

    private fun View.prepareForEntrance() {
        alpha = 0f
        translationY = resources.getDimension(R.dimen.enter_offset)
        isVisible = true
    }
}
