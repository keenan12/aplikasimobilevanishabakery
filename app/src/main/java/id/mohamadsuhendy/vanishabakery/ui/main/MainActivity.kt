package id.mohamadsuhendy.vanishabakery.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.ActivityMainBinding
import id.mohamadsuhendy.vanishabakery.ui.auth.LoginActivity
import id.mohamadsuhendy.vanishabakery.ui.bottomsheet.ActionBottomSheetFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val app get() = application as VanishaBakeryApp

    private val navItems: List<Pair<Int, Int>> get() = listOf(
        R.id.navHome     to R.id.homeFragment,
        R.id.navRiwayat  to R.id.aktivitasFragment,
        R.id.navProduk   to R.id.produkFragment,
        R.id.navProfil   to R.id.profileFragment
    )

    private val iconViews: List<Pair<Int, ImageView>> get() = listOf(
        R.id.homeFragment     to binding.ivNavHome,
        R.id.aktivitasFragment to binding.ivNavRiwayat,
        R.id.produkFragment   to binding.ivNavProduk,
        R.id.profileFragment  to binding.ivNavProfil
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        observeNetwork()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { app.authRepository.updateOnlineStatus(true) }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { app.authRepository.updateOnlineStatus(false) }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        val navOptions = androidx.navigation.NavOptions.Builder()
            .setEnterAnim(R.anim.nav_enter)
            .setExitAnim(R.anim.nav_exit)
            .setPopEnterAnim(R.anim.nav_pop_enter)
            .setPopExitAnim(R.anim.nav_pop_exit)
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, false, true)
            .build()

        binding.navHome.setOnClickListener { navController.navigate(R.id.homeFragment, null, navOptions) }
        binding.navProduk.setOnClickListener { navController.navigate(R.id.produkFragment, null, navOptions) }
        binding.navRiwayat.setOnClickListener { navController.navigate(R.id.aktivitasFragment, null, navOptions) }
        binding.navProfil.setOnClickListener { navController.navigate(R.id.profileFragment, null, navOptions) }
        
        // Integrated Add Button
        binding.navAdd.setOnClickListener {
            ActionBottomSheetFragment().show(supportFragmentManager, ActionBottomSheetFragment.TAG)
        }

        navController.addOnDestinationChangedListener { _, dest, _ ->
            updateActiveIcon(dest.id)
        }
        updateActiveIcon(R.id.homeFragment)
    }

    private fun updateActiveIcon(destId: Int) {
        // Hide all backgrounds
        binding.bgHome.visibility = android.view.View.GONE
        binding.bgProduk.visibility = android.view.View.GONE
        binding.bgRiwayat.visibility = android.view.View.GONE
        binding.bgProfil.visibility = android.view.View.GONE

        // Show active background
        when (destId) {
            R.id.homeFragment      -> binding.bgHome.visibility = android.view.View.VISIBLE
            R.id.produkFragment    -> binding.bgProduk.visibility = android.view.View.VISIBLE
            R.id.aktivitasFragment -> binding.bgRiwayat.visibility = android.view.View.VISIBLE
            R.id.profileFragment   -> binding.bgProfil.visibility = android.view.View.VISIBLE
        }
    }

    fun setNotifBadge(visible: Boolean) {}

    private fun observeNetwork() {
        lifecycleScope.launch {
            app.networkUtils.networkStatus.collect { isOnline ->
                if (isOnline) {
                    launch { app.mitraRepository.syncPendingMitra() }
                    launch { app.pengirimanRepository.syncPendingPengiriman() }
                    launch { app.penjualanRepository.syncPendingPenjualan() }
                }
            }
        }
    }

    fun logout() {
        app.authRepository.logout()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
