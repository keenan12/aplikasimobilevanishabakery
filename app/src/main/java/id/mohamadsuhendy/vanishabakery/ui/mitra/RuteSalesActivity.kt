package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.databinding.ActivityRuteSalesBinding
import id.mohamadsuhendy.vanishabakery.databinding.ItemRuteNeoBinding

class RuteSalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRuteSalesBinding
    private val viewModel: MitraViewModel by viewModels {
        val app = application as VanishaBakeryApp
        MitraViewModel.Factory(app.mitraRepository, app.authRepository, app.ruteRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuteSalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        val adapter = RuteSalesAdapter { rute ->
            // Open Manage Mitra filtered by this route
            val intent = Intent(this, ManageMitraActivity::class.java).apply {
                putExtra("SELECTED_RUTE_ID", rute.id)
            }
            startActivity(intent)
        }
        binding.rvRuteSales.layoutManager = LinearLayoutManager(this)
        binding.rvRuteSales.adapter = adapter

        viewModel.ruteList.observe(this) { list ->
            adapter.submitList(list)
        }
    }
}

class RuteSalesAdapter(private val onClick: (Rute) -> Unit) : 
    ListAdapter<Rute, RuteSalesAdapter.VH>(Diff()) {
    
    inner class VH(val binding: ItemRuteNeoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemRuteNeoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvKodeRute.text = item.kode
            tvNamaRute.text = item.namaRute
            tvTotalMitra.text = "${item.totalMitra} MITRA TERDAFTAR"
            root.setOnClickListener { onClick(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Rute>() {
        override fun areItemsTheSame(a: Rute, b: Rute) = a.id == b.id
        override fun areContentsTheSame(a: Rute, b: Rute) = a == b
    }
}
