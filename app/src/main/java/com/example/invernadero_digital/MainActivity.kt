package com.example.invernadero_digital

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.invernadero_digital.datos.BaseDatosInvernadero
import com.example.invernadero_digital.datos.Usuario
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00D084),
                    onPrimary = Color(0xFF003B27),
                    secondary = Color(0xFF38BDF8),
                    background = Color(0xFF071311),
                    onBackground = Color(0xFFE8F3EF),
                    surface = Color(0xFF10221F),
                    onSurface = Color(0xFFE8F3EF),
                    surfaceVariant = Color(0xFF1A302B),
                    onSurfaceVariant = Color(0xFFB6C9C3)
                )
            ) {
                AplicacionInvernadero()
            }
        }
    }
}

@Composable
fun AplicacionInvernadero() {
    var mostrarRegistro by remember { mutableStateOf(false) }
    var haIniciadoSesion by remember { mutableStateOf(false) }
    var mensajeLogin by remember { mutableStateOf("") }
    val context = LocalContext.current
    val baseDatos = remember { BaseDatosInvernadero.obtener(context) }
    val usuarioDao = remember { baseDatos.usuarioDao() }
    val scope = rememberCoroutineScope()

    when {
        haIniciadoSesion -> {
            PantallaPanel(
                alCerrarSesion = {
                    haIniciadoSesion = false
                    mensajeLogin = ""
                }
            )
        }

        mostrarRegistro -> {
            PantallaRegistro(
                alVolver = { mostrarRegistro = false },
                alRegistrarse = { nombre, correo, contrasena ->
                    scope.launch {
                        val correoNormalizado = correo.trim().lowercase()
                        val existente = usuarioDao.buscarPorCorreo(correoNormalizado)
                        if (existente != null) {
                            mostrarRegistro = false
                            mensajeLogin = "Ese correo ya tiene una cuenta. Inicia sesión."
                        } else {
                            val (salt, hash) = crearCredencial(contrasena)
                            usuarioDao.insertar(
                                Usuario(
                                    nombre = nombre.trim(),
                                    correo = correoNormalizado,
                                    saltContrasena = salt,
                                    hashContrasena = hash
                                )
                            )
                            mostrarRegistro = false
                            mensajeLogin = "Cuenta creada. Inicia sesión."
                        }
                    }
                }
            )
        }

        else -> {
            PantallaInicioSesion(
                mensaje = mensajeLogin,
                alIrARegistro = {
                    mostrarRegistro = true
                    mensajeLogin = ""
                },
                alIniciarSesion = { correo, contrasena ->
                    scope.launch {
                        val usuario = usuarioDao.buscarPorCorreo(correo.trim().lowercase())
                        if (
                            usuario != null &&
                            verificarContrasena(
                                contrasena,
                                usuario.saltContrasena,
                                usuario.hashContrasena
                            )
                        ) {
                            haIniciadoSesion = true
                            mensajeLogin = ""
                        } else {
                            mensajeLogin = "Correo o contraseña incorrectos."
                        }
                    }
                }
            )
        }
    }
}
@Composable
fun PantallaInicioSesion(
    mensaje: String,
    alIrARegistro: () -> Unit,
    alIniciarSesion: (String, String) -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌱", fontSize = 64.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Invernadero Digital",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(text = "Controla el crecimiento de tus plantas")

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { alIniciarSesion(correo.trim(), contrasena) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Iniciar sesión", fontSize = 16.sp)
            }

            if (mensaje.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = mensaje)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = alIrARegistro) {
                Text("¿No tienes una cuenta? Regístrate")
            }
        }
    }
}

@Composable
fun PantallaRegistro(
    alVolver: () -> Unit,
    alRegistrarse: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌱", fontSize = 64.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crear cuenta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(text = "Regístrate para continuar")

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (
                        nombre.isBlank() ||
                        !correo.contains("@") ||
                        contrasena.length < 6
                    ) {
                        mensaje =
                            "Completa los campos. La contraseña debe tener 6 caracteres o más."
                    } else {
                        alRegistrarse(nombre.trim(), correo.trim().lowercase(), contrasena)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Registrarme", fontSize = 16.sp)
            }

            if (mensaje.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = mensaje)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = alVolver) {
                Text("Volver al inicio de sesión")
            }
        }
    }
}

@Composable
fun PantallaPanel(alCerrarSesion: () -> Unit) {
    val fondo = Color(0xFF071311)
    val tarjeta = Color(0xFF10221F)
    val verde = Color(0xFF00D084)
    val textoSecundario = Color(0xFF9AB5AE)
    var mensaje by remember { mutableStateOf("") }
    var mostrarConsola by remember { mutableStateOf(false) }

    if (mostrarConsola) {
        PantallaConsola(alVolver = { mostrarConsola = false })
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = fondo
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = alCerrarSesion) {
                    Text("☰", color = verde, fontSize = 24.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "INVERNADERO: LOTE 1",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Monitoreo ambiental",
                        color = textoSecundario,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "● EN LÍNEA",
                    color = verde,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF29463B),
                                Color(0xFF132D25),
                                Color(0xFF0A1915)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌿  🌱  🌿", fontSize = 38.sp)
                    Text(
                        text = "Cultivo protegido",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TarjetaMedicion(
                    titulo = "Humedad del suelo",
                    valor = "68 %",
                    estado = "ADECUADO",
                    icono = "♧",
                    colorEstado = verde,
                    fondo = tarjeta,
                    modifier = Modifier.weight(1f)
                )

                TarjetaMedicion(
                    titulo = "Precipitaciones",
                    valor = "72 %",
                    estado = "ESTABLE",
                    icono = "☁",
                    colorEstado = Color(0xFF38BDF8),
                    fondo = tarjeta,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TarjetaMedicion(
                    titulo = "Iluminación solar",
                    valor = "71",
                    estado = "MODERADA",
                    icono = "☀",
                    colorEstado = Color(0xFFFFC857),
                    fondo = tarjeta,
                    modifier = Modifier.weight(1f)
                )

                TarjetaMedicion(
                    titulo = "Estado del sistema",
                    valor = "Activo",
                    estado = "Riego automático listo",
                    icono = "⚙",
                    colorEstado = verde,
                    fondo = tarjeta,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tarjeta)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ESTADO DEL INVERNADERO",
                        color = textoSecundario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "💧  Válvula de riego: Cerrada",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "🌀  Rociador: En espera",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { mostrarConsola = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "IR A CONSOLA DE MANDO",
                    fontWeight = FontWeight.Bold
                )
            }

            if (mensaje.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensaje,
                    color = textoSecundario,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = alCerrarSesion,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cerrar sesión", color = textoSecundario)
            }
        }
    }
}

@Composable
fun TarjetaMedicion(
    titulo: String,
    valor: String,
    estado: String,
    icono: String,
    colorEstado: Color,
    fondo: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(text = icono, color = colorEstado, fontSize = 24.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = titulo,
                color = Color(0xFFB6C9C3),
                fontSize = 12.sp
            )

            Text(
                text = valor,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = estado,
                color = colorEstado,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
fun PantallaConsola(alVolver: () -> Unit) {
    val fondo = Color(0xFF071311)
    val tarjeta = Color(0xFF10221F)
    val verde = Color(0xFF00D084)
    val rojo = Color(0xFFE53935)
    val textoSecundario = Color(0xFF9AB5AE)
    var valvulaAbierta by remember { mutableStateOf(false) }
    var paradaEmergencia by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = fondo) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TextButton(onClick = alVolver) {
                Text("← Volver al monitoreo", color = verde)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("MANDO REMOTO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Control manual prioritario", color = textoSecundario, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { if (!paradaEmergencia) valvulaAbierta = !valvulaAbierta },
                enabled = !paradaEmergencia,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (valvulaAbierta) "💧  CERRAR VÁLVULA" else "💧  ABRIR VÁLVULA")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { paradaEmergencia = true; valvulaAbierta = false },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (paradaEmergencia) Color(0xFF7A2020) else rojo
                )
            ) {
                Text(if (paradaEmergencia) "PARADA DE EMERGENCIA ACTIVA" else "⚠  PARADA GENERAL DE EMERGENCIA")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (paradaEmergencia) "Sistema detenido. Válvula cerrada." else "La parada cierra la válvula y bloquea la apertura manual.",
                color = textoSecundario,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tarjeta)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PANEL DE ESTADO DE SEGURIDAD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("●  Enlace local: Simulado", color = verde, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "●  Válvula: ${if (valvulaAbierta) "Abierta" else "Cerrada"}",
                        color = if (valvulaAbierta) Color(0xFFFFC857) else verde,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "●  Seguridad: ${if (paradaEmergencia) "Parada activa" else "Sistema listo"}",
                        color = if (paradaEmergencia) rojo else verde,
                        fontSize = 13.sp
                    )
                }
            }
            if (paradaEmergencia) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { paradaEmergencia = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("REANUDAR SISTEMA")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Modo demostración: estos controles solo cambian el estado mostrado en la app; no están conectados a una válvula real.",
                color = textoSecundario,
                fontSize = 11.sp
            )
        }
    }
}
private fun crearCredencial(contrasena: String): Pair<ByteArray, ByteArray> {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return salt to derivarHash(contrasena, salt)
}

private fun verificarContrasena(
    contrasena: String,
    salt: ByteArray,
    hashGuardado: ByteArray
): Boolean = MessageDigest.isEqual(hashGuardado, derivarHash(contrasena, salt))

private fun derivarHash(contrasena: String, salt: ByteArray): ByteArray {
    val algoritmo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        "PBKDF2WithHmacSHA256"
    } else {
        "PBKDF2WithHmacSHA1"
    }
    val especificacion = PBEKeySpec(contrasena.toCharArray(), salt, 120_000, 256)
    return try {
        SecretKeyFactory.getInstance(algoritmo).generateSecret(especificacion).encoded
    } finally {
        especificacion.clearPassword()
    }
}