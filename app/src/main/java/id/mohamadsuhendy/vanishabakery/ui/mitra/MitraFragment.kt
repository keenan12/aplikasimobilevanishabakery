package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.databinding.FragmentMitraBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.visible

import androidx.fragment.app.activityViewModels
import id.mohamadsuhendy.vanishabakery.ui.maps.MapsActivity

class MitraFragment : Fragment() {

    private var _binding: FragmentMitraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MitraViewModel by activityViewModels {
        val app = requireActivity().application as VanishaBakeryApp
        MitraViewModel.Factory(app.mitraRepository, app.authRepository, app.ruteRepository)
    }

    private lateinit var adapter: MitraAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMitraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupStatsClickListeners()
        observeViewModel()
    }

    private fun setupAdapter() {
        adapter = MitraAdapter(
            onItemClick = { mitra -> openDetail(mitra) },
            onMapClick = { mitra -> openMap(mitra) }
        )
        binding.rvMitra.adapter = adapter
    }

    private fun openMap(mitra: Mitra) {
        val intent = Intent(requireContext(), MapsActivity::class.java).apply {
            putExtra(Constants.KEY_MITRA, mitra)
        }
        startActivity(intent)
    }

    private fun setupStatsClickListeners() {
        binding.cardTotalMitra.setOnClickListener { viewModel.filterMitra("all") }
        binding.cardAktifMitra.setOnClickListener { viewModel.filterMitra(Constants.STATUS_APPROVED) }
        binding.cardPendingMitra.setOnClickListener { viewModel.filterMitra(Constants.STATUS_PENDING) }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.mitraList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.progressBar.gone()
            
            // Update counts manually for now or via LiveData if added to ViewModel
            binding.tvTotalCount.text = list.size.toString()
            binding.tvAktifCount.text = list.count { it.isApproved() }.toString()
            binding.tvPendingCount.text = list.count { it.isPending() }.toString()
        }
    }

    private fun openDetail(mitra: Mitra) {
        val intent = Intent(requireContext(), MitraDetailActivity::class.java).apply {
            putExtra(Constants.KEY_MITRA, mitra)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
