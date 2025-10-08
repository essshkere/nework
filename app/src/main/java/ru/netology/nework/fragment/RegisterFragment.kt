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

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var avatarUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                avatarUri = uri
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.avatarImageView)
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

        binding.signUpButton.setOnClickListener {
            if (validateForm()) {
                val login = binding.loginEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString()
                val name = binding.nameEditText.text.toString().trim()

                viewModel.signUp(login, password, name, avatarUri?.toString())
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

    private fun validateLogin(login: String): Boolean {
        return if (login.isBlank()) {
            binding.loginEditText.error = "Логин не может быть пустым"
            false
        } else {
            binding.loginEditText.error = null
            true
        }
    }

    private fun validateName(name: String): Boolean {
        return if (name.isBlank()) {
            binding.nameEditText.error = "Имя не может быть пустым"
            false
        } else {
            binding.nameEditText.error = null
            true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return if (password.isBlank()) {
            binding.passwordEditText.error = "Пароль не может быть пустым"
            false
        } else if (password.length < 6) {
            binding.passwordEditText.error = "Пароль должен содержать минимум 6 символов"
            false
        } else {
            binding.passwordEditText.error = null
            true
        }
    }

    private fun validatePasswordConfirmation(password: String, confirmation: String): Boolean {
        return if (confirmation != password) {
            binding.passwordConfirmEditText.error = "Пароли не совпадают"
            false
        } else {
            binding.passwordConfirmEditText.error = null
            true
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

        return isLoginValid && isNameValid && isPasswordValid && isPasswordConfirmationValid && isAvatarValid
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signUpButton.isEnabled = !isLoading
        binding.selectAvatarButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        val errorMessage = when {
            message.contains("400") -> "Пользователь с таким логином уже зарегистрирован"
            message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение"
            else -> "Ошибка: $message"
        }

        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}