package com.example.alerta

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.alerta.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var temperature by remember { mutableStateOf("--°C") }
    var currentTime by remember { mutableStateOf("") }
    var showAbout by remember { mutableStateOf(false) }
    var alarmaActiva by remember { mutableStateOf(false) }

    // Reloj en tiempo real
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("EEEE dd 'de' MMMM yyyy\nHH:mm:ss", Locale("es", "CO"))
                .format(Date())
                .replaceFirstChar { it.uppercaseChar() }
            delay(1000L)
        }
    }

    // Temperatura placeholder
    LaunchedEffect(Unit) {
        fetchTemperaturaCucuta { temperature = it }
    }

    // Listener Firebase - Sincronización del estado del relé
    DisposableEffect(Unit) {
        val db = Firebase.database.reference.child("alarma")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val valor = snapshot.getValue(Int::class.java) ?: 0
                alarmaActiva = (valor == 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al sincronizar: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        db.addValueEventListener(listener)

        onDispose {
            db.removeEventListener(listener)
        }
    }

    // Diálogo Acerca de
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Acerca de", color = Primary) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Aplicación desarrollada por")
                    Text(
                        "Napoleon Roca Perez",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text("Ing. Electrónico")
                    Text("Cúcuta - 2026", color = Secondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Cerrar", color = Secondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚨 ALERTA IE CARLOS RAMIREZ PARIS", color = OnPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Tarjeta hora y temperatura
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = currentTime, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Temperatura en Cúcuta", color = Primary)
                    Text(
                        text = temperature,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón Principal de Pánico Remoto (Verde / Rojo)
            val buttonColor by animateColorAsState(
                targetValue = if (alarmaActiva) Color.Red else Color.Green,
                animationSpec = tween(600)
            )

            Button(
                onClick = {
                    alarmaActiva = !alarmaActiva
                    val nuevoValor = if (alarmaActiva) 1 else 0

                    Firebase.database.reference.child("alarma")
                        .setValue(nuevoValor)
                        .addOnSuccessListener {
                            vibrar(context, 600)
                            Toast.makeText(
                                context,
                                if (alarmaActiva) "🚨 ALARMA ACTIVADA" else "✅ Alarma apagada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            alarmaActiva = !alarmaActiva
                        }
                },
                modifier = Modifier
                    .size(220.dp)
                    .shadow(16.dp, CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                shape = CircleShape
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (alarmaActiva) Icons.Default.Warning else Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (alarmaActiva) "ALARMA\nENCENDIDA" else "ACTIVAR\nALARMA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Alertas de Sismos (El límite máximo permitido por Android)
            OutlinedButton(
                onClick = {
                    // Ruta directa a los ajustes de Ubicación
                    val intentUbicacion = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                    try {
                        context.startActivity(intentUbicacion)

                        // Mensaje super claro con los dos pasos faltantes que Android no deja automatizar
                        Toast.makeText(
                            context,
                            "Toca en: 1. Servicios de ubicación  ➡️  2. Alertas de sismos",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {
                        // Plan B de emergencia (Ajustes generales)
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        Toast.makeText(
                            context,
                            "Usa la lupa para buscar 'Alertas de sismos'",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                border = BorderStroke(2.dp, Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Activar alertas automáticas de sismos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Acerca de
            OutlinedButton(
                onClick = { showAbout = true },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                border = BorderStroke(1.dp, Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acerca de", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Función de vibración
private fun vibrar(context: Context, ms: Long) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }
}

// Placeholder temperatura
private fun fetchTemperaturaCucuta(onResult: (String) -> Unit) {
    onResult("27°C")
}