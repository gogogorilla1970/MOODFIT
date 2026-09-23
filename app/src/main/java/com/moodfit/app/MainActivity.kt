package com.moodfit.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
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
import com.moodfit.app.ai.MakeoverProvider
import com.moodfit.app.ai.MakeoverRequest
import com.moodfit.app.ai.MakeoverResult
import com.moodfit.app.ai.OpenAiGptImageProvider
import kotlinx.coroutines.launch

private val Cream = Color(0xFFFFF7FA)
private val Plum = Color(0xFF6E365D)
private val Mauve = Color(0xFFB75C8D)
private val Rose = Color(0xFFF4C6DC)
private val SoftRose = Color(0xFFFFEAF3)
private val Ink = Color(0xFF2E2430)

private enum class AiProvider(val title: String, val subtitle: String) {
    OPENAI("OpenAI", "GPT Image 2"),
    FAL("FLUX", "fal.ai FLUX.2")
}

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
    var meUri by remember { mutableStateOf<Uri?>(null) }
    var lookUri by remember { mutableStateOf<Uri?>(null) }
    var extraPrompt by remember { mutableStateOf("") }
    var outfit by remember { mutableStateOf(true) }
    var hair by remember { mutableStateOf(true) }
    var makeup by remember { mutableStateOf(true) }
    var pose by remember { mutableStateOf(true) }
    var background by remember { mutableStateOf(false) }
    val pickMe = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { meUri = it }
    val pickLook = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { lookUri = it }
    val prompt = composerPrompt(outfit, hair, makeup, pose, background, extraPrompt)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 36.dp)) {
        Header()
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text("Makeover vorbereiten", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Text("YOU + LOOK auswählen und anschließend mit deinem ChatGPT- oder Gemini-Konto erstellen.", color = Ink.copy(alpha = .62f))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ImageCard(Modifier.weight(1f), "YOU", "Dein Foto", meUri) { pickMe.launch("image/*") }
                ImageCard(Modifier.weight(1f), "LOOK", "Deine Vorlage", lookUri) { pickLook.launch("image/*") }
            }
            Spacer(Modifier.height(18.dp))
            Text("Aus LOOK übernehmen", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            FeatureRow("Outfit", outfit) { outfit = it }
            FeatureRow("Frisur", hair) { hair = it }
            FeatureRow("Make-up", makeup) { makeup = it }
            FeatureRow("Pose", pose) { pose = it }
            FeatureRow("Hintergrund", background) { background = it }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = extraPrompt, onValueChange = { extraPrompt = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Optionaler Zusatzwunsch") }, placeholder = { Text("z. B. amateur smartphone photo, natural light") },
                shape = RoundedCornerShape(16.dp), minLines = 2)
            Spacer(Modifier.height(16.dp))
            Button(enabled = meUri != null && lookUri != null,
                onClick = { shareComposer(context, meUri, lookUri, prompt, "com.openai.chatgpt", "ChatGPT") },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Send, null); Spacer(Modifier.width(8.dp)); Text("In ChatGPT erstellen", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(enabled = meUri != null && lookUri != null,
                onClick = { shareComposer(context, meUri, lookUri, prompt, "com.google.android.apps.bard", "Gemini") },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Send, null); Spacer(Modifier.width(8.dp)); Text("In Gemini / Nano Banana erstellen", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { copyComposerPrompt(context, prompt) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Prompt kopieren")
            }
            Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(18.dp)) {
                Text("Keine zusätzliche MOODFIT-API nötig. Die Bildgenerierung erfolgt in ChatGPT oder Gemini mit deinem dortigen Konto.", Modifier.padding(15.dp), color = Plum, fontSize = 13.sp)
            }
        }
    }
}

private fun composerPrompt(outfit:Boolean,hair:Boolean,makeup:Boolean,pose:Boolean,background:Boolean,extra:String):String {
    val parts = mutableListOf<String>()
    if (outfit) parts += "outfit and clothing"
    if (hair) parts += "hairstyle"
    if (makeup) parts += "make-up"
    if (pose) parts += "pose and body position"
    if (background) parts += "background and environment"
    val transfer = if (parts.isEmpty()) "overall styling" else parts.joinToString(", ")
    return "Use the two attached reference images for a realistic makeover. IMAGE 1 (YOU) is the identity reference: preserve the adult person's recognizable identity, age, facial proportions, face shape, eyes, nose, mouth, skin texture and natural anatomy. IMAGE 2 (LOOK) is the styling reference. Transfer only: " + transfer + ". Do not blend the two identities. The result must clearly remain the person from IMAGE 1, styled according to IMAGE 2. Keep believable proportions, hands, lighting and photographic detail. No text or watermark. " + extra.trim()
}

private fun shareComposer(context:Context,you:Uri?,look:Uri?,prompt:String,pkg:String,label:String) {
    if (you == null || look == null) return
    val streams = arrayListOf(you, look)
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams); putExtra(Intent.EXTRA_TEXT, prompt)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); setPackage(pkg)
    }
    try { context.startActivity(intent) } catch (_:Exception) {
        val fallback = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams); putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(fallback, "Mit " + label + " öffnen"))
    }
}

private fun copyComposerPrompt(context:Context,prompt:String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MOODFIT Prompt", prompt))
    Toast.makeText(context, "Prompt kopiert", Toast.LENGTH_SHORT).show()
}

@Composable
private fun Header() {
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
                Text("Makeover Composer · v0.3", color = Mauve, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProviderCard(
    modifier: Modifier,
    provider: AiProvider,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Rose else Color.White),
        border = if (selected) BorderStroke(1.5.dp, Plum) else null
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onClick)
                Spacer(Modifier.width(4.dp))
                Text(provider.title, fontWeight = FontWeight.Bold, color = Plum)
            }
            Text(provider.subtitle, fontSize = 12.sp, color = Ink.copy(alpha = .60f))
        }
    }
}

@Composable
private fun ApiKeyField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        supportingText = { Text("Nur für diese App-Sitzung; nicht im GitHub-Projekt gespeichert.") }
    )
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
