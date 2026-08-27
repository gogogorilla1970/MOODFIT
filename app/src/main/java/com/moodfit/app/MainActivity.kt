package com.moodfit.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moodfit.app.ai.FalFlux2Provider
import com.moodfit.app.ai.MakeoverRequest
import com.moodfit.app.ai.MakeoverResult
import kotlinx.coroutines.launch

private val Cream = Color(0xFFFFF7FA)
private val Plum = Color(0xFF6E365D)
private val Mauve = Color(0xFFB75C8D)
private val Rose = Color(0xFFF4C6DC)
private val SoftRose = Color(0xFFFFEAF3)
private val Ink = Color(0xFF2E2430)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoodfitApp() }
    }
}

@Composable
private fun MoodfitApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Plum,
            secondary = Mauve,
            background = Cream,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Cream) { CreateScreen() }
    }
}

@Composable
private fun CreateScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var meUri by remember { mutableStateOf<Uri?>(null) }
    var lookUri by remember { mutableStateOf<Uri?>(null) }
    var apiKey by remember { mutableStateOf("") }
    var extraPrompt by remember { mutableStateOf("") }
    var resultUrl by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    var outfit by remember { mutableStateOf(true) }
    var hair by remember { mutableStateOf(true) }
    var makeup by remember { mutableStateOf(true) }
    var pose by remember { mutableStateOf(true) }
    var background by remember { mutableStateOf(false) }

    val pickMe = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { meUri = it }
    val pickLook = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { lookUri = it }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(Brush.linearGradient(listOf(Color(0xFFFBE0EC), Color(0xFFF0D8F1), Cream)))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Plum),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("MOODFIT", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Plum)
                    Text("AI Makeover Studio · v0.2", color = Mauve, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(Modifier.padding(horizontal = 18.dp)) {
            Text("Create your new look", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Text("Dein Gesicht + eine Look-Vorlage werden mit FLUX.2 kombiniert.", color = Ink.copy(alpha = .62f))
            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ImageCard(Modifier.weight(1f), "YOU", "Dein Foto", meUri) { pickMe.launch("image/*") }
                ImageCard(Modifier.weight(1f), "LOOK", "Deine Vorlage", lookUri) { pickLook.launch("image/*") }
            }

            Spacer(Modifier.height(20.dp))
            Text("Aus LOOK übernehmen", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))

            FeatureRow("Outfit", outfit) { outfit = it }
            FeatureRow("Frisur", hair) { hair = it }
            FeatureRow("Make-up", makeup) { makeup = it }
            FeatureRow("Pose", pose) { pose = it }
            FeatureRow("Hintergrund", background) { background = it }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = extraPrompt,
                onValueChange = { extraPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optionaler Zusatzwunsch") },
                placeholder = { Text("z. B. candid dating photo, 9:16") },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("fal.ai API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                supportingText = { Text("Wird nicht im GitHub-Projekt gespeichert; in v0.2 nur für diese Sitzung.") }
            )

            Spacer(Modifier.height(16.dp))
            Button(
                enabled = meUri != null && lookUri != null && apiKey.isNotBlank() && !busy,
                onClick = {
                    val identity = meUri ?: return@Button
                    val look = lookUri ?: return@Button
                    busy = true
                    resultUrl = null
                    status = "Makeover wird erzeugt …"
                    scope.launch {
                        val provider = FalFlux2Provider(context, apiKey)
                        when (val result = provider.generate(
                            MakeoverRequest(
                                identityImage = identity,
                                lookImage = look,
                                transferOutfit = outfit,
                                transferHair = hair,
                                transferMakeup = makeup,
                                transferPose = pose,
                                transferBackground = background,
                                prompt = extraPrompt
                            )
                        )) {
                            is MakeoverResult.Success -> {
                                resultUrl = result.imageUrl
                                status = "Makeover fertig."
                            }
                            is MakeoverResult.Error -> status = result.message
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Plum)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, null)
                }
                Spacer(Modifier.width(10.dp))
                Text(if (busy) "Generating…" else "Create Makeover", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            status?.let {
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(18.dp)) {
                    Text(it, Modifier.padding(16.dp), color = Plum)
                }
            }

            resultUrl?.let { url ->
                Spacer(Modifier.height(22.dp))
                Text("Dein Ergebnis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                Card(shape = RoundedCornerShape(26.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Makeover result",
                        modifier = Modifier.fillMaxWidth().height(520.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = Ink)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ImageCard(modifier: Modifier, title: String, subtitle: String, uri: Uri?, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(220.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (uri != null) {
                AsyncImage(model = uri, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .38f)).padding(10.dp)
                ) { Text(title, color = Color.White, fontWeight = FontWeight.Black) }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(SoftRose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (title == "YOU") Icons.Rounded.Face else Icons.Rounded.AddPhotoAlternate,
                            null,
                            tint = Plum,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(title, fontWeight = FontWeight.Black, color = Plum)
                    Text(subtitle, color = Ink.copy(alpha = .55f), fontSize = 13.sp)
                }
            }
        }
    }
}
