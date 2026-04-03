package com.addons.addons_next

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.addons.addons_next.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()
        bindActions()
        startIntroAnimation()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.topBar.updatePadding(
                top = resources.getDimensionPixelSize(R.dimen.space_8) + systemBars.top,
                bottom = resources.getDimensionPixelSize(R.dimen.space_20)
            )
            binding.contentScroll.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_bar_space)
            )
            binding.bottomActionBar.updatePadding(
                left = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.left,
                top = resources.getDimensionPixelSize(R.dimen.space_12),
                right = resources.getDimensionPixelSize(R.dimen.screen_padding) + systemBars.right,
                bottom = resources.getDimensionPixelSize(R.dimen.space_12) + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun bindActions() = with(binding) {
        val clickMessage = getString(R.string.action_placeholder)
        primaryButton.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.primary_action_feedback), Snackbar.LENGTH_SHORT).show()
        }
        heroButton.setOnClickListener {
            Snackbar.make(binding.root, clickMessage, Snackbar.LENGTH_SHORT).show()
        }
        templateButton.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.template_action_feedback), Snackbar.LENGTH_SHORT).show()
        }
        exportButton.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.export_action_feedback), Snackbar.LENGTH_SHORT).show()
        }
        resetButton.setOnClickListener {
            moduleNameInput.text?.clear()
            packageNameInput.text?.clear()
            namespaceInput.text?.clear()
            switchAnimations.isChecked = true
            switchManifest.isChecked = true
            switchScriptApi.isChecked = false
            minecraftVersionChipGroup.check(R.id.chip_version_preview)
            Snackbar.make(binding.root, getString(R.string.reset_feedback), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startIntroAnimation() {
        val animatedViews = listOf(
            binding.heroCard,
            binding.statusCard,
            binding.generatorCard,
            binding.previewCard,
            binding.pipelineCard
        )

        animatedViews.forEachIndexed { index, view ->
            view.prepareForEntrance()
            view.postDelayed({
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(420L)
                    .setStartDelay(0L)
                    .start()
            }, index * 90L)
        }
    }

    private fun View.prepareForEntrance() {
        alpha = 0f
        translationY = resources.getDimension(R.dimen.enter_offset)
        isVisible = true
    }
}
