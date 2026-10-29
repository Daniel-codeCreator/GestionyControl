package com.universidad.sigelab.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.ui.catalogos.LaboratoriosScreen
import com.universidad.sigelab.ui.catalogos.TiposEquipoScreen
import com.universidad.sigelab.ui.dashboard.DashboardScreen
import com.universidad.sigelab.ui.equipos.ARG_ID_EQUIPO
import com.universidad.sigelab.ui.equipos.EquipoDetalleScreen
import com.universidad.sigelab.ui.equipos.EquipoFormScreen
import com.universidad.sigelab.ui.equipos.EquiposScreen
import com.universidad.sigelab.ui.equipos.EscanerQrScreen
import com.universidad.sigelab.ui.login.LoginScreen
import com.universidad.sigelab.ui.mantenimiento.MantenimientoScreen
import com.universidad.sigelab.ui.modulos.ModuloScreen
import com.universidad.sigelab.ui.modulos.SinAccesoScreen
import com.universidad.sigelab.ui.prestamos.PrestamosScreen
import com.universidad.sigelab.ui.reportes.ReportesScreen
import com.universidad.sigelab.ui.usuarios.CrearUsuarioScreen

private object Rutas {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val ARG_MODULO = "modulo"
    const val MODULO = "modulo/{$ARG_MODULO}"
    const val EQUIPO = "equipo/{$ARG_ID_EQUIPO}"
    const val EQUIPO_FORM = "equipo-form/{$ARG_ID_EQUIPO}"
    const val ESCANER_QR = "escanear-qr"

    fun modulo(modulo: Modulo) = "modulo/${modulo.name}"
    fun equipo(id: Int) = "equipo/$id"

    /** id = 0 para un equipo nuevo. */
    fun equipoForm(id: Int = 0) = "equipo-form/$id"
}

@Composable
fun SigelabNavHost(sessionManager: SessionManager) {
    val nav = rememberNavController()
    val sesion by sessionManager.sesion.collectAsStateWithLifecycle()
    val autenticado = sesion != null
    val rol = sesion?.usuario?.rol
    val volver: () -> Unit = { nav.popBackStack() }

    // La sesión decide la pantalla raíz: al completar los dos factores va al Dashboard;
    // al cerrar sesión o si el token expira (401), vuelve al Login sin historial.
    LaunchedEffect(autenticado) {
        val destino = if (autenticado) Rutas.DASHBOARD else Rutas.LOGIN
        if (nav.currentDestination?.route != destino) {
            nav.navigate(destino) {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Rutas.LOGIN) {
        composable(Rutas.LOGIN) {
            LoginScreen()
        }
        composable(Rutas.DASHBOARD) {
            DashboardScreen(onAbrirModulo = { nav.navigate(Rutas.modulo(it)) })
        }
        composable(
            Rutas.MODULO,
            arguments = listOf(navArgument(Rutas.ARG_MODULO) { type = NavType.StringType }),
        ) { entrada ->
            val modulo = entrada.arguments?.getString(Rutas.ARG_MODULO)
                ?.let { nombre -> Modulo.entries.firstOrNull { it.name == nombre } }
            // Segunda barrera además del menú filtrado (p. ej. un deep link).
            SiPermitido(modulo != null && rol?.puedeVer(modulo) == true, volver) {
                when (modulo) {
                    Modulo.USUARIOS -> CrearUsuarioScreen(onVolver = volver)
                    Modulo.EQUIPOS -> EquiposScreen(
                        onVolver = volver,
                        onAbrirEquipo = { nav.navigate(Rutas.equipo(it)) },
                        onNuevoEquipo = { nav.navigate(Rutas.equipoForm()) },
                        onEscanear = { nav.navigate(Rutas.ESCANER_QR) },
                    )
                    Modulo.MANTENIMIENTO -> MantenimientoScreen(onVolver = volver)
                    Modulo.LABORATORIOS -> LaboratoriosScreen(onVolver = volver)
                    Modulo.TIPOS_EQUIPO -> TiposEquipoScreen(onVolver = volver)
                    Modulo.REPORTES -> ReportesScreen(onVolver = volver)
                    Modulo.PRESTAMOS -> PrestamosScreen(onVolver = volver)
                    else -> modulo?.let {
                        ModuloScreen(it, soloLectura = rol?.puedeEditar(it) != true, onVolver = volver)
                    }
                }
            }
        }

        // --- Gestión de Equipos: DOCENTE consulta, ADMIN y ENCARGADO también editan ---
        val argumentoId = listOf(navArgument(ARG_ID_EQUIPO) { type = NavType.IntType })

        composable(Rutas.EQUIPO, arguments = argumentoId) {
            SiPermitido(rol?.puedeVer(Modulo.EQUIPOS) == true, volver) {
                EquipoDetalleScreen(onVolver = volver, onEditar = { nav.navigate(Rutas.equipoForm(it)) })
            }
        }
        composable(Rutas.EQUIPO_FORM, arguments = argumentoId) { entrada ->
            val esNuevo = (entrada.arguments?.getInt(ARG_ID_EQUIPO) ?: 0) == 0
            SiPermitido(rol?.puedeEditar(Modulo.EQUIPOS) == true, volver) {
                EquipoFormScreen(
                    onVolver = volver,
                    onGuardado = { id ->
                        if (esNuevo) {
                            // El alta lleva al detalle, donde está el QR para imprimir.
                            nav.navigate(Rutas.equipo(id)) { popUpTo(Rutas.EQUIPO_FORM) { inclusive = true } }
                        } else {
                            nav.popBackStack()
                        }
                    },
                )
            }
        }
        composable(Rutas.ESCANER_QR) {
            SiPermitido(rol?.puedeVer(Modulo.EQUIPOS) == true, volver) {
                EscanerQrScreen(
                    onVolver = volver,
                    onEquipoEncontrado = { id ->
                        nav.navigate(Rutas.equipo(id)) { popUpTo(Rutas.ESCANER_QR) { inclusive = true } }
                    },
                )
            }
        }
    }
}

@Composable
private fun SiPermitido(permitido: Boolean, onVolver: () -> Unit, contenido: @Composable () -> Unit) {
    if (permitido) contenido() else SinAccesoScreen(onVolver = onVolver)
}
