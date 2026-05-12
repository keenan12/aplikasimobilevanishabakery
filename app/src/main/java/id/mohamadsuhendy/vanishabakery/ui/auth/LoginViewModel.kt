package id.mohamadsuhendy.vanishabakery.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.mohamadsuhendy.vanishabakery.data.model.User
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Result<User>>()
    val loginState: LiveData<Result<User>> = _loginState

    fun login(email: String, password: String) {
        if (email.isBlank()) { _loginState.value = Result.Error("Email tidak boleh kosong"); return }
        if (password.isBlank()) { _loginState.value = Result.Error("Password tidak boleh kosong"); return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = Result.Error("Format email tidak valid"); return
        }
        _loginState.value = Result.Loading
        viewModelScope.launch {
            _loginState.value = authRepository.login(email, password)
        }
    }

    fun isLoggedIn() = authRepository.isLoggedIn

    class Factory(private val repo: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel(repo) as T
    }
}
