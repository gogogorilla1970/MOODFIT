package com.moodfit.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
    MaterialTheme(colorScheme = lightColorScheme(primary=Plum, secondary=Mauve, background=Cream, surface=Color.White, onPrimary=Color.White, onBackground=Ink, onSurface=Ink)) {
        Surface(Modifier.fillMaxSize(), color=Cream) { CreateScreen() }
    }
}

@Composable
private fun CreateScreen() {
    var meUri by remember { mutableStateOf<Uri?>(null) }
    var lookUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val pickMe = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { meUri = it }
    val pickLook = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { lookUri = it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=30.dp)) {
        Box(Modifier.fillMaxWidth().height(118.dp).background(Brush.linearGradient(listOf(Color(0xFFFBE0EC), Color(0xFFF0D8F1), Cream))).padding(horizontal=18.dp), contentAlignment=Alignment.CenterStart) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Plum), contentAlignment=Alignment.Center) { Icon(Icons.Rounded.Favorite, null, tint=Color.White, modifier=Modifier.size(30.dp)) }
                Spacer(Modifier.width(14.dp)); Column { Text("MOODFIT", fontSize=25.sp, fontWeight=FontWeight.Black, color=Plum); Text("AI Makeover Studio", color=Mauve, fontWeight=FontWeight.SemiBold) }
            }
        }
        Column(Modifier.padding(horizontal=18.dp)) {
            Text("Create your new look", fontSize=28.sp, fontWeight=FontWeight.Black, color=Ink)
            Text("Dein Gesicht + dein Wunschlook. Du bestimmst, was übernommen wird.", color=Ink.copy(alpha=.62f))
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ImageCard(Modifier.weight(1f), "YOU", "Dein Foto", meUri) { pickMe.launch("image/*") }
                ImageCard(Modifier.weight(1f), "LOOK", "Deine Vorlage", lookUri) { pickLook.launch("image/*") }
            }
            Spacer(Modifier.height(20.dp)); Text("Übernehmen", fontWeight=FontWeight.Bold, fontSize=17.sp); Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("Outfit","Hair","Make-up").forEach { AssistChip(onClick={}, label={Text(it, fontSize=12.sp)}, modifier=Modifier.weight(1f), colors=AssistChipDefaults.assistChipColors(containerColor=Rose)) } }
            Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("Pose","Background","Full Look").forEach { AssistChip(onClick={}, label={Text(it, fontSize=11.sp)}, modifier=Modifier.weight(1f)) } }
            Spacer(Modifier.height(18.dp))
            Button(enabled=meUri!=null && lookUri!=null, onClick={status="MOODFIT ist bereit. Als Nächstes verbinden wir den KI-Provider."}, modifier=Modifier.fillMaxWidth().height(58.dp), shape=RoundedCornerShape(20.dp), colors=ButtonDefaults.buttonColors(containerColor=Plum)) { Icon(Icons.Rounded.AutoAwesome,null); Spacer(Modifier.width(10.dp)); Text("Create Makeover", fontSize=17.sp, fontWeight=FontWeight.Bold) }
            status?.let { Spacer(Modifier.height(14.dp)); Card(colors=CardDefaults.cardColors(containerColor=SoftRose), shape=RoundedCornerShape(18.dp)) { Text(it, Modifier.padding(16.dp), color=Plum) } }
        }
    }
}

@Composable
private fun ImageCard(modifier:Modifier,title:String,subtitle:String,uri:Uri?,onClick:()->Unit) {
    Card(modifier=modifier.height(220.dp).clickable(onClick=onClick), shape=RoundedCornerShape(26.dp), colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Box(Modifier.fillMaxSize()) {
            if(uri!=null) AsyncImage(model=uri, contentDescription=title, modifier=Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
            else Column(Modifier.fillMaxSize(), verticalArrangement=Arrangement.Center, horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(SoftRose), contentAlignment=Alignment.Center) { Icon(if(title=="YOU") Icons.Rounded.Face else Icons.Rounded.AddPhotoAlternate,null,tint=Plum,modifier=Modifier.size(30.dp)) }
                Spacer(Modifier.height(14.dp)); Text(title,fontWeight=FontWeight.Black,color=Plum); Text(subtitle,color=Ink.copy(alpha=.55f),fontSize=13.sp)
            }
        }
    }
}
