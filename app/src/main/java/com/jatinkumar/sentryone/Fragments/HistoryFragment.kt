package com.jatinkumar.sentryone.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jatinkumar.sentryone.Adapters.SOSHistoryAdapter
import com.jatinkumar.sentryone.databinding.FragmentHistoryBinding
import com.jatinkumar.sentryone.viewModels.SOSHistoryViewModel
import androidx.fragment.app.activityViewModels
import com.jatinkumar.sentryone.SOSHistoryViewModelFactory

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SOSHistoryViewModel by activityViewModels {
        SOSHistoryViewModelFactory(requireActivity().application)
    }
    private lateinit var sosAdapter: SOSHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sosAdapter = SOSHistoryAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvSetup()
    }

    private fun rvSetup() {
        binding.historyRv.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sosAdapter
        }
        viewModel.historySOS.observe(viewLifecycleOwner) { sosHistoryList ->
            sosAdapter.submitList(sosHistoryList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}