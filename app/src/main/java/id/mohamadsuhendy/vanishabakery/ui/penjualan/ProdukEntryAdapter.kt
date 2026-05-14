package id.mohamadsuhendy.vanishabakery.ui.penjualan

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.databinding.ItemProdukRapidEntryBinding

/**
 * Adapter untuk tabel produk di RapidEntryActivity.
 * Setiap row = 1 produk dengan input Kirim dan Retur.
 * Mendukung penyimpanan state per mitra agar data tidak hilang saat navigasi.
 */
class ProdukEntryAdapter(
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<ProdukEntryAdapter.ViewHolder>() {

    data class ProdukEntry(
        val produk: Produk,
        var kirim: Int = 0,
        var retur: Int = 0,
        val existingId: String? = null
    ) {
        val terjual: Int get() = maxOf(0, kirim - retur)
        val totalHarga: Int get() = terjual * produk.harga
    }

    private val items = mutableListOf<ProdukEntry>()

    /**
     * Set daftar produk (dipanggil sekali saat load produk).
     * Produk tersimpan sebagai template, state input dikelola terpisah per mitra.
     */
    fun setProducts(products: List<Produk>) {
        items.clear()
        items.addAll(products.map { ProdukEntry(it) })
        notifyDataSetChanged()
    }

    /**
     * Reset semua input ke 0 (saat pindah ke mitra berikutnya yang belum ada datanya).
     */
    fun resetAll() {
        items.forEachIndexed { index, _ ->
            items[index] = items[index].copy(kirim = 0, retur = 0)
        }
        notifyDataSetChanged()
    }

    /**
     * Restore data dari snapshot yang tersimpan (saat navigasi ke mitra yang pernah diisi).
     */
    fun restoreEntries(savedEntries: List<ProdukEntry>) {
        items.clear()
        items.addAll(savedEntries.map { it.copy() })
        notifyDataSetChanged()
    }

    /**
     * Ambil snapshot current entries untuk disimpan ke state map mitra.
     */
    fun getCurrentSnapshot(): List<ProdukEntry> = items.map { it.copy() }

    /** Get all entries with kirim > 0 or retur > 0, OR if they have an existingId (to allow deletion) */
    fun getFilledEntries(): List<ProdukEntry> = items.filter { it.kirim > 0 || it.retur > 0 || it.existingId != null }

    /** Get total terjual across all products */
    fun getTotalTerjual(): Int = items.sumOf { it.terjual }

    /** Get total omset across all products */
    fun getTotalOmset(): Int = items.sumOf { it.totalHarga }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProdukRapidEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class ViewHolder(private val binding: ItemProdukRapidEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var kirimWatcher: TextWatcher? = null
        private var returWatcher: TextWatcher? = null

        fun bind(entry: ProdukEntry, position: Int) {
            binding.tvProdukNama.text = entry.produk.nama

            // Remove old watchers before setting text to avoid feedback loop
            kirimWatcher?.let { binding.etKirim.removeTextChangedListener(it) }
            returWatcher?.let { binding.etRetur.removeTextChangedListener(it) }

            binding.etKirim.setText(if (entry.kirim > 0) entry.kirim.toString() else "")
            binding.etRetur.setText(if (entry.retur > 0) entry.retur.toString() else "")

            kirimWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        items[pos] = items[pos].copy(kirim = s?.toString()?.toIntOrNull() ?: 0)
                        onDataChanged()
                    }
                }
            }

            returWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        items[pos] = items[pos].copy(retur = s?.toString()?.toIntOrNull() ?: 0)
                        onDataChanged()
                    }
                }
            }

            binding.etKirim.addTextChangedListener(kirimWatcher)
            binding.etRetur.addTextChangedListener(returWatcher)
        }
    }
}
