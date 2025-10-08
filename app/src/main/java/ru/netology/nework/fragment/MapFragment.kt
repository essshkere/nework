package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.databinding.FragmentMapBinding

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupMap()
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.confirmButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор локации будет реализован позже", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        // TODO: Implement Yandex Maps integration
        Snackbar.make(binding.root, "Карта будет реализована позже", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}