package ru.netology.nework.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentLoginBinding
import ru.netology.nework.viewmodel.AuthViewModel

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTextWatchers()
        setupClickListeners()
        observeAuthState()
        observeUiState()
    }

    private fun setupTextWatchers() {
        binding.loginEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateLogin(s.toString())
                updateSubmitButtonState()
            }
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
                updateSubmitButtonState()
            }
        })
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            if (validateForm()) {
                val login = binding.loginEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString()
                authViewModel.signIn(login, password)
            }
        }

        binding.registerTextView.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        binding.retryButton.setOnClickListener {
            if (validateForm()) {
                val login = binding.loginEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString()
                authViewModel.signIn(login, password)
            }
        }

        binding.loginEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearFieldErrors()
            }
        }

        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearFieldErrors()
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        AuthViewModel.AuthState.Loading -> {
                        }
                        AuthViewModel.AuthState.Success -> {
                            navigateToPosts()
                        }
                        is AuthViewModel.AuthState.Error -> {
                        }
                        AuthViewModel.AuthState.Idle -> {
                        }
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { uiState ->
                    updateUi(uiState)
                }
            }
        }
    }

    private fun updateUi(uiState: AuthViewModel.AuthUiState) {
        with(binding) {
            progressBar.isVisible = uiState.isLoading
            loadingOverlay.isVisible = uiState.isLoading
            loginEditText.isEnabled = !uiState.isLoading
            passwordEditText.isEnabled = !uiState.isLoading
            signInButton.isEnabled = !uiState.isLoading && isFormValid()
            registerTextView.isEnabled = !uiState.isLoading
            if (uiState.showError && uiState.error != null) {
                showError(uiState.error)
                authViewModel.clearError()
            }
            updateAuthState(uiState.isAuthenticated)
        }
    }

    private fun updateAuthState(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            navigateToPosts()
        }
    }

    private fun validateLogin(login: String): Boolean {
        return when {
            login.isBlank() -> {
                binding.loginEditText.error = "Логин не может быть пустым"
                false
            }
            login.length < 3 -> {
                binding.loginEditText.error = "Логин должен содержать минимум 3 символа"
                false
            }
            else -> {
                binding.loginEditText.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isBlank() -> {
                binding.passwordEditText.error = "Пароль не может быть пустым"
                false
            }
            password.length < 6 -> {
                binding.passwordEditText.error = "Пароль должен содержать минимум 6 символов"
                false
            }
            else -> {
                binding.passwordEditText.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val login = binding.loginEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        val isLoginValid = validateLogin(login)
        val isPasswordValid = validatePassword(password)

        val isValid = isLoginValid && isPasswordValid

        if (!isValid) {
            showFormError("Заполните все поля правильно")
        }
        return isValid
    }

    private fun isFormValid(): Boolean {
        val login = binding.loginEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        return login.isNotBlank() && password.isNotBlank() &&
                login.length >= 3 && password.length >= 6
    }

    private fun updateSubmitButtonState() {
        binding.signInButton.isEnabled = isFormValid() && !authViewModel.uiState.value.isLoading
    }

    private fun clearFieldErrors() {
        binding.loginEditText.error = null
        binding.passwordEditText.error = null
        hideError()
    }

    private fun showError(message: String) {
        with(binding) {
            errorTextView.text = message
            errorLayout.isVisible = true
            retryButton.isVisible = message.contains("сети", ignoreCase = true)
            errorLayout.postDelayed({
                hideError()
            }, 5000)
        }
    }

    private fun showFormError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun hideError() {
        binding.errorLayout.isVisible = false
    }

    private fun navigateToPosts() {
        findNavController().navigate(R.id.postsFragment)
    }

    override fun onPause() {
        super.onPause()
        authViewModel.clearError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}