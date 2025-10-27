package ru.netology.nework

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.databinding.ActivityMainBinding
import ru.netology.nework.fragment.ConfirmLogoutDialog
import ru.netology.nework.viewmodel.AuthViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(
                R.id.postsFragment,
                R.id.eventsFragment,
                R.id.usersFragment,
                R.id.myProfileFragment
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupToolbar()
        observeAuthState()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.bottomNavigation.isVisible = false
                    binding.toolbar.isVisible = false
                }
                else -> {
                    binding.bottomNavigation.isVisible = true
                    binding.toolbar.isVisible = true
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.uiState.collectLatest { uiState ->
                invalidateOptionsMenu()
                updateUi(uiState)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isAuthenticated = authViewModel.isAuthenticated()
        menu.findItem(R.id.action_profile).isVisible = isAuthenticated
        menu.findItem(R.id.action_logout).isVisible = isAuthenticated
        menu.findItem(R.id.action_login).isVisible = !isAuthenticated
        menu.findItem(R.id.action_register).isVisible = !isAuthenticated

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                navigateToMyProfile()
                true
            }
            R.id.action_login -> {
                navigateToLogin()
                true
            }
            R.id.action_register -> {
                navigateToRegister()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUi(uiState: AuthViewModel.AuthUiState) {
        updateToolbarTitle()
        if (uiState.showError && uiState.error != null) {
            showError(uiState.error)
            authViewModel.clearError()
        }
    }

    private fun updateToolbarTitle() {
        val currentFragment = getCurrentFragment()
        when (currentFragment) {
            is ru.netology.nework.fragment.PostsFragment -> {
                supportActionBar?.title = "Посты"
            }
            is ru.netology.nework.fragment.EventsFragment -> {
                supportActionBar?.title = "События"
            }
            is ru.netology.nework.fragment.UsersFragment -> {
                supportActionBar?.title = "Пользователи"
            }
            is ru.netology.nework.fragment.MyProfileFragment -> {
                supportActionBar?.title = "Мой профиль"
            }
            is ru.netology.nework.fragment.UserProfileFragment -> {
                supportActionBar?.title = "Профиль пользователя"
            }
            else -> {
                supportActionBar?.title = getString(R.string.app_name)
            }
        }
    }

    private fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
    }

    private fun navigateToMyProfile() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigate(R.id.myProfileFragment)
    }

    private fun navigateToLogin() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigate(R.id.loginFragment)
    }

    private fun navigateToRegister() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigate(R.id.registerFragment)
    }

    private fun showLogoutConfirmation() {
        val dialog = ConfirmLogoutDialog.newInstance()
        dialog.onLogoutConfirmed = {
            authViewModel.logout()
            Snackbar.make(binding.root, "Вы вышли из аккаунта", Snackbar.LENGTH_SHORT).show()
        }
        dialog.show(supportFragmentManager, ConfirmLogoutDialog.TAG)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}