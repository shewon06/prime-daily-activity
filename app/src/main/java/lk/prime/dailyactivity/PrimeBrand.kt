package lk.prime.dailyactivity

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PrimeOfficialLogo(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    val logo = remember(context) {
        runCatching {
            val encoded = context.resources
                .openRawResource(R.raw.prime_logo)
                .bufferedReader()
                .use { it.readText() }

            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (logo != null) {
            Image(
                bitmap = logo,
                contentDescription = "PRIME Agri Business & Plantations",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Text(
                text = "PRIME",
                color = Color(0xFFD6A62E),
                fontWeight = FontWeight.Black
            )
        }
    }
}
