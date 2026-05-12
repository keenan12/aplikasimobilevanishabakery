package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetKelolaMitraBinding

class KelolaMitraBottomSheet(
    private val viewModel: MitraViewModel
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetKelolaMitraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetKelolaMitraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch()
        observeData()
        
        binding.btnReset.setOnClickListener {
            viewModel.resetFilters()
            dismiss()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeData() {
        // Observe Staff
        viewModel.staffList.observe(viewLifecycleOwner) { staffList ->
            val names = mutableListOf("Semua Sales")
            names.addAll(staffList.map { it.nama })
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSales.adapter = adapter
            
            binding.spinnerSales.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        viewModel.filterByStaff("")
                    } else {
                        viewModel.filterByStaff(staffList[position - 1].uid)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Observe Rute
        viewModel.ruteList.observe(viewLifecycleOwner) { ruteList ->
            val names = mutableListOf("Semua Rute")
            names.addAll(ruteList.map { it.namaRute })
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRute.adapter = adapter
            
            binding.spinnerRute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        viewModel.filterByRute("")
                    } else {
                        viewModel.filterByRute(ruteList[position - 1].id)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
