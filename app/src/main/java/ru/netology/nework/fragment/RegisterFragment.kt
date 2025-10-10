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

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
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
                binding.removeAvatarButton.visibility = View.VISIBLE
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
    }

    private fun setupTextWatchers() {
        binding.loginEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateLogin(s.toString())
            }
        })

        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateName(s.toString())
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
                viewModel.signUp(login, password, name, avatarUriString)
            }
        }
    }
    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        AuthViewModel.AuthState.Loading -> {
                            showLoading(true)
                        }
                        AuthViewModel.AuthState.Success -> {
                            showLoading(false)
                            Snackbar.make(binding.root, "Регистрация успешна!", Snackbar.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.postsFragment)
                        }
                        is AuthViewModel.AuthState.Error -> {
                            showLoading(false)
                            showError(state.message)
                        }
                        AuthViewModel.AuthState.Idle -> {
                            showLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun removeAvatar() {
        avatarUri = null
        avatarFile = null
        binding.avatarImageView.setImageResource(R.drawable.ic_account_circle)
        binding.removeAvatarButton.visibility = View.GONE
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
                binding.loginEditText.error = "Логин может содержать только буквы, цифры, точки, дефисы и подчеркивания"
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
        val isPasswordConfirmationValid = validatePasswordConfirmation(password, passwordConfirmation)
        val isAvatarValid = validateAvatar()
        val isValid = isLoginValid && isNameValid && isPasswordValid &&
                isPasswordConfirmationValid && isAvatarValid
        if (!isValid) {
            Snackbar.make(binding.root, "Заполните все обязательные поля правильно", Snackbar.LENGTH_LONG).show()
        }
        return isValid
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signUpButton.isEnabled = !isLoading
        binding.selectAvatarButton.isEnabled = !isLoading
        binding.removeAvatarButton.isEnabled = !isLoading

        binding.loginEditText.isEnabled = !isLoading
        binding.nameEditText.isEnabled = !isLoading
        binding.passwordEditText.isEnabled = !isLoading
        binding.passwordConfirmEditText.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        val errorMessage = when {
            message.contains("403") -> "Пользователь с таким логином уже зарегистрирован"
            message.contains("415") -> "Неправильный формат изображения. Используйте JPG или PNG"
            message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение к интернету"
            else -> "Ошибка: $message"
        }
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        avatarFile?.delete()
        _binding = null
    }
}