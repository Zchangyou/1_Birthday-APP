package com.birthday.friends.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.birthday.friends.R
import com.birthday.friends.alarm.AlarmScheduler
import com.birthday.friends.databinding.FragmentMainBinding
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EventAdapter(
            onClick = { item ->
                findNavController().navigate(
                    R.id.action_main_to_detail,
                    bundleOf("eventId" to item.event.id)
                )
            },
            onLongClick = { item ->
                showDeleteDialog(item)
                true
            }
        )

        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_edit)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { items ->
                    adapter.submitList(items)
                    binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showDeleteDialog(item: EventUiItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除")
            .setMessage("确定要删除「${item.event.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                AlarmScheduler.cancelAll(requireContext(), item.event)
                viewModel.delete(item.event)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
