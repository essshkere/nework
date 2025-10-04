package ru.netology.nework.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val viewModel: AuthViewModel by viewModels()

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
    }

    private fun setupTextWatchers() {
        binding.loginEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateLogin(s.toString())
            }
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            if (validateForm()) {
                val login = binding.loginEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString()
                viewModel.signIn(login, password)
            }
        }

        binding.registerTextView.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
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

    private fun validateLogin(login: String): Boolean {
        return if (login.isBlank()) {
            binding.loginEditText.error = "Логин не может быть пустым"
            false
        } else {
            binding.loginEditText.error = null
            true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return if (password.isBlank()) {
            binding.passwordEditText.error = "Пароль не может быть пустым"
            false
        } else {
            binding.passwordEditText.error = null
            true
        }
    }

    private fun validateForm(): Boolean {
        val login = binding.loginEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        val isLoginValid = validateLogin(login)
        val isPasswordValid = validatePassword(password)

        return isLoginValid && isPasswordValid
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !isLoading
        binding.registerTextView.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        val errorMessage = when {
            message.contains("400") -> "Неверный логин или пароль"
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