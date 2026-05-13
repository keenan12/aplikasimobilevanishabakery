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
            checkExistingStok()
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
                checkExistingStok()
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
            val adapterRute = ArrayAdapter(this, android.R.layout.simple_list_item_1, ruteList.map { "${it.namaRute} (${it.staffNama})" })
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
                    binding.btnSimpan.text = if (binding.btnSimpan.text.toString().contains("UPDATE")) "UPDATE STOK BAWAAN" else "SIMPAN STOK BAWAAN"
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkExistingStok() {
        val rute = selectedRute ?: return
        
        selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
        selectedCalendar.set(Calendar.MINUTE, 0)
        selectedCalendar.set(Calendar.SECOND, 0)
        selectedCalendar.set(Calendar.MILLISECOND, 0)

        viewModel.checkExistingStok(rute.id, selectedCalendar.timeInMillis).observe(this) { existing ->
            if (existing != null) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
                    val map: Map<String, Int> = com.google.gson.Gson().fromJson(existing.stokDataJson, type)
                    adapter.restoreQuantities(map)
                    binding.btnSimpan.text = "UPDATE STOK BAWAAN"
                } catch (e: Exception) {
                    adapter.resetAll()
                    binding.btnSimpan.text = "SIMPAN STOK BAWAAN"
                }
            } else {
                adapter.resetAll()
                binding.btnSimpan.text = "SIMPAN STOK BAWAAN"
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

        fun restoreQuantities(stokData: Map<String, Int>) {
            quantities.clear()
            quantities.putAll(stokData)
            notifyDataSetChanged()
        }

        fun resetAll() {
            quantities.clear()
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
            
            // Remove previous listener to avoid infinite loop when setting text
            if (holder.textWatcher != null) {
                holder.binding.etJumlah.removeTextChangedListener(holder.textWatcher)
            }
            
            val savedQty = quantities[produk.nama] ?: 0
            if (savedQty > 0) {
                holder.binding.etJumlah.setText(savedQty.toString())
            } else {
                holder.binding.etJumlah.setText("")
            }
            
            holder.textWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val qty = s?.toString()?.toIntOrNull() ?: 0
                    quantities[produk.nama] = qty
                }
            }
            holder.binding.etJumlah.addTextChangedListener(holder.textWatcher)
        }

        override fun getItemCount() = list.size

        class ViewHolder(val binding: ItemStokProdukEntryBinding) : RecyclerView.ViewHolder(binding.root) {
            var textWatcher: android.text.TextWatcher? = null
        }
    }
}
