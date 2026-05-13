package id.mohamadsuhendy.vanishabakery.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.databinding.ActivityManageStokRuteBinding
import id.mohamadsuhendy.vanishabakery.databinding.ItemStokProdukEntryBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import java.text.SimpleDateFormat
import java.util.*

class ManageStokRuteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStokRuteBinding
    private val viewModel: StokRuteViewModel by viewModels {
        val app = application as VanishaBakeryApp
        StokRuteViewModel.Factory(
            app.stokRuteRepository,
            app.ruteRepository,
            app.produkRepository,
            app.authRepository
        )
    }

    private var allRute = listOf<Rute>()
    private var selectedRute: Rute? = null
    private var selectedCalendar = Calendar.getInstance()
    private val adapter = StokProdukAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStokRuteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rvProdukStok.adapter = adapter

        binding.acRute.setOnClickListener { binding.acRute.showDropDown() }
        binding.acRute.setOnItemClickListener { _, _, position, _ ->
            selectedRute = allRute[position]
        }

        binding.btnPilihTanggal.setOnClickListener {
            showDatePicker()
        }

        binding.btnSimpan.setOnClickListener {
            saveStok()
        }

        updateDateButtonText()
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedCalendar.set(year, month, day)
                updateDateButtonText()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtonText() {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        binding.btnPilihTanggal.text = sdf.format(selectedCalendar.time)
    }

    private fun observeViewModel() {
        viewModel.ruteList.observe(this) { ruteList ->
            allRute = ruteList
            val adapterRute = ArrayAdapter(this, android.R.layout.simple_list_item_1, ruteList.map { it.namaRute })
            binding.acRute.setAdapter(adapterRute)
        }

        viewModel.produkList.observe(this) { produkList ->
            adapter.setProdukList(produkList)
        }

        viewModel.saveState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.btnSimpan.isEnabled = false
                    binding.btnSimpan.text = "MENYIMPAN..."
                }
                is Result.Success -> {
                    Toast.makeText(this, result.data, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "SIMPAN STOK BAWAAN"
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveStok() {
        val rute = selectedRute ?: return Toast.makeText(this, "Pilih rute dulu", Toast.LENGTH_SHORT).show()
        val stokData = adapter.getStokData()
        
        if (stokData.isEmpty()) {
            return Toast.makeText(this, "Input minimal satu jumlah produk", Toast.LENGTH_SHORT).show()
        }

        // Set time to 00:00:00 for exact date matching
        selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
        selectedCalendar.set(Calendar.MINUTE, 0)
        selectedCalendar.set(Calendar.SECOND, 0)
        selectedCalendar.set(Calendar.MILLISECOND, 0)

        viewModel.saveStokRute(
            ruteId = rute.id,
            ruteNama = rute.namaRute,
            tanggal = selectedCalendar.timeInMillis,
            stokData = stokData
        )
    }

    class StokProdukAdapter : RecyclerView.Adapter<StokProdukAdapter.ViewHolder>() {
        private var list = listOf<Produk>()
        private val quantities = mutableMapOf<String, Int>()

        fun setProdukList(newList: List<Produk>) {
            list = newList
            notifyDataSetChanged()
        }

        fun getStokData(): Map<String, Int> {
            return quantities.filter { it.value > 0 }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStokProdukEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val produk = list[position]
            holder.binding.tvNamaProduk.text = produk.nama
            
            // Handle edit text change
            holder.binding.etJumlah.setOnFocusChangeListener { _, _ ->
                val qty = holder.binding.etJumlah.text.toString().toIntOrNull() ?: 0
                quantities[produk.nama] = qty
            }
        }

        override fun getItemCount() = list.size

        class ViewHolder(val binding: ItemStokProdukEntryBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
