package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import kotlinx.coroutines.launch
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.databinding.ActivityManageMitraBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.showToast

class ManageMitraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageMitraBinding
    private val viewModel: MitraViewModel by viewModels {
        val app = application as VanishaBakeryApp
        MitraViewModel.Factory(app.mitraRepository, app.authRepository, app.ruteRepository)
    }

    private lateinit var adapter: MitraAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageMitraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupRefresh()
        observeData()
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().terminate().addOnCompleteListener {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork().addOnCompleteListener {
                        binding.swipeRefresh.isRefreshing = false
                        showToast("Sinkronisasi data ulang berhasil")
                        // The flows will automatically reconnect
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = MitraAdapter(
            onItemClick = { mitra -> openDetail(mitra) },
            onMapClick = { mitra -> openMap(mitra) }
        )
        binding.rvManageMitra.layoutManager = LinearLayoutManager(this)
        binding.rvManageMitra.adapter = adapter

        // Hide Sales Filter for Sales accounts
        val app = application as VanishaBakeryApp
        lifecycleScope.launch {
            val user = app.authRepository.getCurrentUserData()
            if (user?.isAdmin() != true) {
                // Change title for Sales
                binding.toolbar.title = "DATA RUTE"
                // For Sales, hide the staff filter card
                (binding.spinnerSales.parent as? View)?.visibility = View.GONE
            }
        }
    }

    private fun openDetail(mitra: Mitra) {
        val intent = Intent(this, MitraDetailActivity::class.java).apply {
            putExtra(Constants.KEY_MITRA, mitra)
        }
        startActivity(intent)
    }

    private fun openMap(mitra: Mitra) {
        val intent = Intent(this, id.mohamadsuhendy.vanishabakery.ui.maps.MapsActivity::class.java).apply {
            putExtra(Constants.KEY_MITRA, mitra)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish() // Close manage screen to focus on map
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnReset.setOnClickListener {
            binding.etSearch.text.clear()
            binding.spinnerSales.setSelection(0)
            binding.spinnerRute.setSelection(0)
            viewModel.resetFilters()
        }
    }

    private fun setupFilters() {
        // Observers for Staff and Rute are already in observeData()
    }

    private fun observeData() {
        viewModel.mitraList.observe(this) { list ->
            adapter.submitList(list)
            binding.tvResultCount.text = "MENAMPILKAN ${list.size} MITRA"
        }

        // Observe Staff for Spinner
        viewModel.staffList.observe(this) { staffList ->
            val names = mutableListOf("Semua Sales")
            names.addAll(staffList.map { it.nama })
            
            val staffAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            staffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSales.adapter = staffAdapter
            
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

        // Observe Rute for Spinner
        viewModel.ruteList.observe(this) { ruteList ->
            val names = mutableListOf("Semua Rute")
            names.addAll(ruteList.map { it.namaRute })
            
            val ruteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            ruteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRute.adapter = ruteAdapter
            
            val preSelectedRuteId = intent.getStringExtra("SELECTED_RUTE_ID")
            if (preSelectedRuteId != null) {
                val index = ruteList.indexOfFirst { it.id == preSelectedRuteId }
                if (index != -1) {
                    binding.spinnerRute.setSelection(index + 1)
                    viewModel.filterByRute(preSelectedRuteId)
                }
            }

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
}
