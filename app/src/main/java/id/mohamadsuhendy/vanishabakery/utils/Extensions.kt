package id.mohamadsuhendy.vanishabakery.utils

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import id.mohamadsuhendy.vanishabakery.R
import java.text.SimpleDateFormat
import java.util.*

// View Extensions
fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.isVisible(show: Boolean) { visibility = if (show) View.VISIBLE else View.GONE }

// Snackbar
fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.showSnackbarError(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).apply {
        setBackgroundTint(context.getColor(R.color.error))
        setTextColor(context.getColor(R.color.  on_primary))
        show()
    }
}

// Toast
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// ImageView - Load with Glide
fun ImageView.loadImage(url: String?, placeholder: Int = R.drawable.ic_person) {
    if (url.isNullOrEmpty()) {
        this.setImageResource(placeholder)
        return
    }
    
    val loadSource: Any = if (url.startsWith("data:image")) {
        try {
            android.util.Base64.decode(url.substringAfter(","), android.util.Base64.DEFAULT)
        } catch (e: Exception) { url }
    } else url

    Glide.with(this)
        .load(loadSource)
        .placeholder(placeholder)
        .error(placeholder)
        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
        .transition(DrawableTransitionOptions.withCrossFade())
        .centerCrop()
        .into(this)
}

fun ImageView.loadImageCircle(url: String?, placeholder: Int = R.drawable.ic_person) {
    val loadSource: Any = when {
        url.isNullOrEmpty() -> placeholder
        url.startsWith("data:image") -> {
            try {
                android.util.Base64.decode(url.substringAfter(","), android.util.Base64.DEFAULT)
            } catch (e: Exception) { url }
        }
        else -> url
    }

    Glide.with(this)
        .load(loadSource)
        .placeholder(placeholder)
        .error(placeholder)
        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
        .circleCrop()
        .into(this)
}

// Date formatting
fun Long.toDateString(pattern: String = "dd MMM yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): CharSequence {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )
}

fun Date.toTimestamp() = com.google.firebase.Timestamp(this)

// String helpers
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}
