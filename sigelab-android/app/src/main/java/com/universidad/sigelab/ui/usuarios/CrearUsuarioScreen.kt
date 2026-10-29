package com.universidad.sigelab.ui.usuarios

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.universidad.sigelab.domain.model.EstadoRegistro
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.usecase.CampoUsuario
import com.universidad.sigelab.ui.camera.CamaraFrontalConPermiso
import com.universidad.sigelab.ui.components.IndicadorPasos
import com.universidad.sigelab.ui.components.MensajeError
import com.universidad.sigelab.ui.theme.LocalColoresEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearUsuarioScreen(
    onVolver: () -> Unit,
    viewModel: CrearUsuarioViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val registrados by viewModel.registrados.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }

    val atras: () -> Unit = {
        when {
            ui.enviando -> Unit
            // Ya existe en SQL Server sin rostro: avisar antes de salir.
            ui.paso == PasoCrearUsuario.Rostro && ui.idUsuarioCreado != null -> { confirmarSalida = true }
            ui.paso == PasoCrearUsuario.Rostro -> viewModel.volverADatos()
            else -> onVolver()
        }
    }
    BackHandler(onBack = atras)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Usuario") },
                navigationIcon = {
                    IconButton(onClick = atras, enabled = !ui.enviando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val paso = ui.paso) {
                    PasoCrearUsuario.Datos -> {
                        IndicadorPasos(pasoActual = 1, total = 2, etiqueta = "Datos del usuario")
                        Spacer(Modifier.height(20.dp))
                        FormularioDatos(ui, viewModel)
                        Spacer(Modifier.height(28.dp))
                        UsuariosRegistrados(registrados, onRefrescar = viewModel::cargarUsuarios)
                    }
                    PasoCrearUsuario.Rostro -> {
                        IndicadorPasos(pasoActual = 2, total = 2, etiqueta = "Registro facial")
                        Spacer(Modifier.height(20.dp))
                        PasoRostro(
                            ui = ui,
                            onFoto = viewModel::onFotoCapturada,
                            onErrorCamara = viewModel::onErrorCamara,
                            onRepetir = viewModel::repetirFoto,
                            onRegistrar = viewModel::registrar,
                            onVolverADatos = viewModel::volverADatos,
                        )
                    }
                    is PasoCrearUsuario.Listo -> UsuarioCreado(paso, onOtro = viewModel::crearOtro, onVolver = onVolver)
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Salir sin registrar el rostro?") },
            text = {
                Text(
                    "El usuario ${ui.formulario.nombreUsuario.trim()} ya existe, pero no podrá iniciar sesión " +
                        "hasta que se registre su rostro.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onVolver()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Seguir aquí") }
            },
        )
    }
}

// ------------------------------------------------------------------ paso 1: datos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioDatos(ui: CrearUsuarioUiState, vm: CrearUsuarioViewModel) {
    val f = ui.formulario
    val errores = ui.erroresCampo
    var verContrasena by rememberSaveable { mutableStateOf(false) }
    val transformacion = if (verContrasena) VisualTransformation.None else PasswordVisualTransformation()
    val nombrePropio = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Campo("Nombre", f.nombre, vm::onNombre, errores[CampoUsuario.NOMBRE], Modifier.weight(1f), teclado = nombrePropio)
        Campo("Apellido", f.apellido, vm::onApellido, errores[CampoUsuario.APELLIDO], Modifier.weight(1f), teclado = nombrePropio)
    }
    Campo(
        "Nombre de usuario", f.nombreUsuario, vm::onNombreUsuario, errores[CampoUsuario.NOMBRE_USUARIO],
        ayuda = "3 a 50 caracteres: letras, números, punto o guion bajo",
        icono = Icons.Outlined.Person,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
    )
    Campo(
        "Correo", f.correo, vm::onCorreo, errores[CampoUsuario.CORREO],
        icono = Icons.Outlined.Email,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    Campo(
        "Contraseña", f.contrasena, vm::onContrasena, errores[CampoUsuario.CONTRASENA],
        ayuda = "Mínimo 8 caracteres, con letras y números. Se guarda cifrada con BCrypt.",
        icono = Icons.Outlined.Lock,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        transformacion = transformacion,
        accion = {
            IconButton(onClick = { verContrasena = !verContrasena }) {
                Icon(
                    if (verContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (verContrasena) "Ocultar contraseñas" else "Mostrar contraseñas",
                )
            }
        },
    )
    Campo(
        "Confirmar contraseña", f.confirmacion, vm::onConfirmacion, ui.errorConfirmacion,
        icono = Icons.Outlined.Lock,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        transformacion = transformacion,
    )

    Spacer(Modifier.height(8.dp))
    Text(
        "Rol",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Rol.entries.forEachIndexed { i, rol ->
            SegmentedButton(
                selected = f.rol == rol,
                onClick = { vm.onRol(rol) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = Rol.entries.size),
            ) { Text(rol.etiqueta()) }
        }
    }
    Text(
        f.rol.descripcion(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    )

    MensajeError(ui.error)
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = vm::continuarARostro,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) { Text("Continuar al registro facial") }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    ayuda: String? = null,
    icono: ImageVector? = null,
    teclado: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    transformacion: VisualTransformation = VisualTransformation.None,
    accion: (@Composable () -> Unit)? = null,
) {
    val apoyo = error ?: ayuda
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        leadingIcon = if (icono != null) { { Icon(icono, contentDescription = null) } } else null,
        trailingIcon = accion,
        isError = error != null,
        supportingText = if (apoyo != null) { { Text(apoyo) } } else null,
        singleLine = true,
        keyboardOptions = teclado,
        visualTransformation = transformacion,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

// ------------------------------------------------------------------ paso 2: rostro

@Composable
private fun PasoRostro(
    ui: CrearUsuarioUiState,
    onFoto: (ByteArray) -> Unit,
    onErrorCamara: (String) -> Unit,
    onRepetir: () -> Unit,
    onRegistrar: () -> Unit,
    onVolverADatos: () -> Unit,
) {
    val f = ui.formulario
    val usuarioYaCreado = ui.idUsuarioCreado != null

    Text(
        "${f.nombre.trim()} ${f.apellido.trim()}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Text(
        "${f.nombreUsuario.trim()} · ${f.rol.etiqueta()}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))

    if (usuarioYaCreado) {
        Aviso("Usuario creado con Id ${ui.idUsuarioCreado}. Falta registrar su rostro para que pueda iniciar sesión.")
    } else {
        Aviso("Pida a la persona que mire de frente a la cámara, sin lentes oscuros ni gorra y con luz pareja.")
    }
    Spacer(Modifier.height(16.dp))

    val foto = ui.foto
    if (foto == null) {
        CamaraFrontalConPermiso(
            procesando = false,
            textoBoton = "Tomar foto",
            alCapturar = onFoto,
            alFallar = onErrorCamara,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        VistaPreviaFoto(foto, enviando = ui.enviando)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRepetir,
                enabled = !ui.enviando,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) { Text("Repetir foto") }
            Button(
                onClick = onRegistrar,
                enabled = !ui.enviando,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                if (ui.enviando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text(if (usuarioYaCreado) "Registrar rostro" else "Crear usuario")
                }
            }
        }
    }

    MensajeError(ui.error)
    if (!usuarioYaCreado) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onVolverADatos, enabled = !ui.enviando) { Text("Volver a los datos") }
    }
}

@Composable
private fun VistaPreviaFoto(foto: FotoCapturada, enviando: Boolean) {
    val imagen = remember(foto) {
        BitmapFactory.decodeByteArray(foto.jpeg, 0, foto.jpeg.size)?.asImageBitmap()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = "Foto capturada para el registro facial",
                contentScale = ContentScale.Crop,
                // Se muestra en espejo, como la vista previa; al servidor se envía sin espejar.
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = -1f },
            )
        }
        if (enviando) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("Registrando…", color = Color.White)
            }
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ------------------------------------------------------------------ resultado

@Composable
private fun UsuarioCreado(paso: PasoCrearUsuario.Listo, onOtro: () -> Unit, onVolver: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    Icon(
        Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint = LocalColoresEstado.current.disponible,
        modifier = Modifier.size(72.dp),
    )
    Spacer(Modifier.height(16.dp))
    Text("Usuario creado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Text(
        "${paso.nombreUsuario} (Id ${paso.idUsuario}) ya puede iniciar sesión con su contraseña y su rostro.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    Button(
        onClick = onOtro,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) { Text("Crear otro usuario") }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onVolver,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) { Text("Volver al menú") }
}

// --------------------------------------------------------- usuarios registrados

/** Tabla "USUARIOS REGISTRADOS" de escritorio: ID, Nombre Completo, Usuario, Correo, Rol y Estado. */
@Composable
private fun UsuariosRegistrados(estado: UsuariosRegistradosUi, onRefrescar: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "USUARIOS REGISTRADOS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRefrescar, enabled = !estado.cargando) { Text(if (estado.cargando) "Cargando…" else "Actualizar") }
    }
    MensajeError(estado.error)
    if (!estado.cargando && estado.error == null && estado.lista.isEmpty()) {
        Text(
            "No hay usuarios registrados.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    estado.lista.forEach { usuario ->
        OutlinedCard(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${usuario.id} · ${usuario.nombreCompleto}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        usuario.estado.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (usuario.estado == EstadoRegistro.ACTIVO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "${usuario.nombreUsuario} · ${usuario.rol?.etiqueta() ?: "Rol desconocido"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    usuario.correo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun Rol.etiqueta() = when (this) {
    Rol.ADMIN -> "Admin"
    Rol.ENCARGADO -> "Encargado"
    Rol.DOCENTE -> "Docente"
}

private fun Rol.descripcion() = when (this) {
    Rol.ADMIN -> "Acceso total al sistema, incluida la creación de usuarios."
    Rol.ENCARGADO -> "Inventario, préstamos, devoluciones y mantenimiento. No crea usuarios."
    Rol.DOCENTE -> "Solo consulta el estado de los equipos."
}
