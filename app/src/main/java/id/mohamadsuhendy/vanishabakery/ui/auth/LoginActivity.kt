package id.mohamadsuhendy.vanishabakery.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.ActivityLoginBinding
import id.mohamadsuhendy.vanishabakery.ui.main.MainActivity
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.showSnackbarError
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible
import android.view.autofill.AutofillManager
import android.os.Build

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory((application as VanishaBakeryApp).authRepository)
    }

    private val PREFS_NAME = "vanisha_prefs"
    private val KEY_SAVED_EMAIL = "saved_email"
    private val KEY_SAVED_PASS = "saved_pass"
    private val KEY_REMEMBER_ME = "remember_me"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Restore night mode preference before setContentView
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Auto-redirect if already logged in
        if (viewModel.isLoggedIn()) {
            navigateToMain()
            return
        }

        // Restore saved credentials if "Simpan Sandi" was checked
        val isRemembered = prefs.getBoolean(KEY_REMEMBER_ME, false)
        if (isRemembered) {
            binding.etEmail.setText(prefs.getString(KEY_SAVED_EMAIL, ""))
            binding.etPassword.setText(prefs.getString(KEY_SAVED_PASS, ""))
            binding.cbSimpanSandi.isChecked = true
        }

        setupObservers()
        setupClickListeners()
        checkLogoutReason()
    }

    private fun checkLogoutReason() {
        intent.getStringExtra("LOGOUT_REASON")?.let { reason ->
            showToast(reason)
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Save or clear credentials based on checkbox
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (binding.cbSimpanSandi.isChecked) {
                prefs.putBoolean(KEY_REMEMBER_ME, true)
                prefs.putString(KEY_SAVED_EMAIL, email)
                prefs.putString(KEY_SAVED_PASS, password)
            } else {
                prefs.putBoolean(KEY_REMEMBER_ME, false)
                prefs.remove(KEY_SAVED_EMAIL)
                prefs.remove(KEY_SAVED_PASS)
            }
            prefs.apply()

            viewModel.login(email, password)
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visible()
                    binding.btnLogin.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.gone()
                    // Trigger system autofill save prompt
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getSystemService(AutofillManager::class.java)?.commit()
                    }
                    navigateToMain()
                }
                is Result.Error -> {
                    binding.progressBar.gone()
                    binding.btnLogin.isEnabled = true
                    binding.root.showSnackbarError(result.message)
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
