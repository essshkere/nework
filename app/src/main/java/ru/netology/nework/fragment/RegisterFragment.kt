package ru.netology.nework.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentRegisterBinding
import ru.netology.nework.viewmodel.AuthViewModel
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private var avatarUri: Uri? = null
    private var avatarFile: File? = null
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                avatarUri = uri
                avatarFile = createTempFileFromUri(uri)
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle)
                    .into(binding.avatarImageView)
                binding.removeAvatarButton.isVisible = true
                binding.avatarErrorTextView.isVisible = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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

        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateName(s.toString())
                updateSubmitButtonState()
            }
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
                validatePasswordConfirmation(
                    binding.passwordEditText.text.toString(),
                    binding.passwordConfirmEditText.text.toString()
                )
                updateSubmitButtonState()
            }
        })

        binding.passwordConfirmEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswordConfirmation(
                    binding.passwordEditText.text.toString(),
                    s.toString()
                )
                updateSubmitButtonState()
            }
        })
    }

    private fun setupClickListeners() {
        binding.selectAvatarButton.setOnClickListener {
            pickImageFromGallery()
        }

        binding.avatarImageView.setOnClickListener {
            pickImageFromGallery()
        }

        binding.removeAvatarButton.setOnClickListener {
            removeAvatar()
        }

        binding.signUpButton.setOnClickListener {
            if (validateForm()) {
                val login = binding.loginEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString()
                val name = binding.nameEditText.text.toString().trim()
                val avatarUriString = avatarUri?.toString()
                authViewModel.signUp(login, password, name, avatarUriString)
            }
        }

        binding.retryButton.setOnClickListener {
            if (validateForm()) {
                val login = binding.loginEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString()
                val name = binding.nameEditText.text.toString().trim()
                val avatarUriString = avatarUri?.toString()
                authViewModel.signUp(login, password, name, avatarUriString)
            }
        }

        listOf(
            binding.loginEditText,
            binding.nameEditText,
            binding.passwordEditText,
            binding.passwordConfirmEditText
        ).forEach { editText ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    clearFieldErrors()
                }
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
            nameEditText.isEnabled = !uiState.isLoading
            passwordEditText.isEnabled = !uiState.isLoading
            passwordConfirmEditText.isEnabled = !uiState.isLoading
            signUpButton.isEnabled = !uiState.isLoading && isFormValid()
            selectAvatarButton.isEnabled = !uiState.isLoading
            removeAvatarButton.isEnabled = !uiState.isLoading

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

    private fun pickImageFromGallery() {
        val intent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        pickImageLauncher.launch(intent)
    }

    private fun removeAvatar() {
        avatarUri = null
        avatarFile = null
        binding.avatarImageView.setImageResource(R.drawable.ic_account_circle)
        binding.removeAvatarButton.isVisible = false
        avatarFile?.delete()
        avatarFile = null
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val file = File.createTempFile("avatar_preview", ".jpg", requireContext().cacheDir)
        file.deleteOnExit()

        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return file
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

            !login.matches(Regex("^[a-zA-Z0-9_.-]+$")) -> {
                binding.loginEditText.error =
                    "Логин может содержать только буквы, цифры, точки, дефисы и подчеркивания"
                false
            }

            else -> {
                binding.loginEditText.error = null
                true
            }
        }
    }

    private fun validateName(name: String): Boolean {
        return when {
            name.isBlank() -> {
                binding.nameEditText.error = "Имя не может быть пустым"
                false
            }

            name.length < 2 -> {
                binding.nameEditText.error = "Имя должно содержать минимум 2 символа"
                false
            }

            else -> {
                binding.nameEditText.error = null
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

    private fun validatePasswordConfirmation(password: String, confirmation: String): Boolean {
        return when {
            confirmation != password -> {
                binding.passwordConfirmEditText.error = "Пароли не совпадают"
                false
            }

            else -> {
                binding.passwordConfirmEditText.error = null
                true
            }
        }
    }

    private fun validateAvatar(): Boolean {
        return true
    }

    private fun validateForm(): Boolean {
        val login = binding.loginEditText.text.toString().trim()
        val name = binding.nameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val passwordConfirmation = binding.passwordConfirmEditText.text.toString()

        val isLoginValid = validateLogin(login)
        val isNameValid = validateName(name)
        val isPasswordValid = validatePassword(password)
        val isPasswordConfirmationValid =
            validatePasswordConfirmation(password, passwordConfirmation)
        val isAvatarValid = validateAvatar()

        val isValid = isLoginValid && isNameValid && isPasswordValid &&
                isPasswordConfirmationValid && isAvatarValid

        if (!isValid) {
            showFormError("Заполните все обязательные поля правильно")
        }

        return isValid
    }

    private fun isFormValid(): Boolean {
        val login = binding.loginEditText.text.toString().trim()
        val name = binding.nameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val passwordConfirmation = binding.passwordConfirmEditText.text.toString()

        return login.isNotBlank() && name.isNotBlank() &&
                password.isNotBlank() && passwordConfirmation.isNotBlank() &&
                login.length >= 3 && name.length >= 2 &&
                password.length >= 6 && password == passwordConfirmation
    }

    private fun updateSubmitButtonState() {
        binding.signUpButton.isEnabled = isFormValid() && !authViewModel.uiState.value.isLoading
    }

    private fun clearFieldErrors() {
        binding.loginEditText.error = null
        binding.nameEditText.error = null
        binding.passwordEditText.error = null
        binding.passwordConfirmEditText.error = null
        binding.avatarErrorTextView.isVisible = false
        hideError()
    }

    private fun showError(message: String) {
        with(binding) {
            errorTextView.text = message
            errorLayout.isVisible = true
            retryButton.isVisible = message.contains("сети", ignoreCase = true)
            if (message.contains("аватар", ignoreCase = true) ||
                message.contains("изображен", ignoreCase = true)
            ) {
                avatarErrorTextView.text = message
                avatarErrorTextView.isVisible = true
            }
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
        Snackbar.make(binding.root, "Регистрация успешна!", Snackbar.LENGTH_SHORT).show()
        findNavController().navigate(R.id.postsFragment)
    }
}