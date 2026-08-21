package lk.prime.dailyactivity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

suspend fun encodeProfilePhoto(
    context: android.content.Context,
    uri: android.net.Uri
): String = withContext(Dispatchers.IO) {
    val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: error("Could not read selected image.")

    val maxDimension = 320f
    val scale = min(1f, min(maxDimension / original.width.toFloat(), maxDimension / original.height.toFloat()))
    val width = (original.width * scale).toInt().coerceAtLeast(1)
    val height = (original.height * scale).toInt().coerceAtLeast(1)
    val resized = if (width != original.width || height != original.height) {
        Bitmap.createScaledBitmap(original, width, height, true)
    } else {
        original
    }

    val output = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 68, output)
    val bytes = output.toByteArray()

    if (resized !== original) resized.recycle()
    original.recycle()

    require(bytes.size < 500_000) { "Selected photo is too large. Please choose another photo." }
    "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

fun isSavedProfilePhoto(value: String?): Boolean =
    !value.isNullOrBlank() && value.startsWith("data:image")

@Composable
fun ProfilePhotoImage(photoValue: String?, modifier: Modifier = Modifier) {
    val safePhoto = photoValue?.takeIf(::isSavedProfilePhoto)
    if (safePhoto == null) {
        Box(modifier = modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
            Text("👤", fontSize = 28.sp)
        }
        return
    }

    val image = remember(safePhoto) {
        runCatching {
            val encoded = safePhoto.substringAfter("base64,")
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(modifier = modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
            Text("👤", fontSize = 28.sp)
        }
    }
}
