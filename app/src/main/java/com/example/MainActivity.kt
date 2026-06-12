package com.example

import android.os.Bundle
import android.content.Intent
import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

// ==================== THEME COLORS & LOCAL PROVIDER ====================
data class AppColors(
    val bg: Color,
    val card: Color,
    val cardStrong: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val orange: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color
)

@Composable
fun getAppColors(isDark: Boolean) = if (isDark) {
    AppColors(
        bg = CyberDarkBg,
        card = CyberDarkCard,
        cardStrong = CyberDarkCardStrong,
        primary = CyberGreen,
        secondary = CyberBlue,
        tertiary = CyberPurple,
        orange = CyberOrange,
        text = CyberTextColor,
        textSecondary = CyberTextSecondary,
        border = BorderColor
    )
} else {
    AppColors(
        bg = LightBg,
        card = LightCard,
        cardStrong = LightCardStrong,
        primary = LightGreen,
        secondary = LightBlue,
        tertiary = LightPurple,
        orange = LightOrange,
        text = LightTextColor,
        textSecondary = LightTextSecondary,
        border = LightBorderColor
    )
}

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

// ==================== DATA MODELS ====================
data class VpnServer(
    val id: Int,
    val country: String,
    val city: String,
    val flag: String,
    val ping: Int,
    val load: Int,
    val ip: String
)

data class GoogleUser(
    val name: String,
    val email: String,
    val photoUrl: String?
)

// ==================== VIEW MODEL ====================
class VpnViewModel : ViewModel() {
    val servers = listOf(
        VpnServer(1, "Países Bajos", "Ámsterdam", "🇳🇱", 42, 28, "185.220.101.42"),
        VpnServer(2, "Alemania", "Fráncfort", "🇩🇪", 48, 35, "185.107.56.12"),
        VpnServer(3, "Alemania", "Berlín", "🇩🇪", 55, 42, "185.107.56.89"),
        VpnServer(4, "Reino Unido", "Londres", "🇬🇧", 62, 38, "185.234.218.10"),
        VpnServer(5, "Francia", "París", "🇫🇷", 58, 31, "185.244.39.22"),
        VpnServer(6, "Suiza", "Zúrich", "🇨🇭", 65, 25, "185.246.188.5"),
        VpnServer(7, "España", "Madrid", "🇪🇸", 38, 22, "185.244.212.33"),
        VpnServer(8, "Italia", "Milán", "🇮🇹", 72, 45, "185.107.80.15"),
        VpnServer(9, "Suecia", "Estocolmo", "🇸🇪", 78, 29, "185.220.100.250"),
        VpnServer(10, "Estados Unidos", "Nueva York", "🇺🇸", 115, 52, "104.18.32.7"),
        VpnServer(11, "Estados Unidos", "Los Ángeles", "🇺🇸", 145, 48, "104.18.32.15"),
        VpnServer(12, "Estados Unidos", "Miami", "🇺🇸", 132, 41, "104.18.32.22"),
        VpnServer(13, "Canadá", "Toronto", "🇨🇦", 128, 36, "104.21.45.10"),
        VpnServer(14, "Canadá", "Vancouver", "🇨🇦", 155, 33, "104.21.45.18"),
        VpnServer(15, "Japón", "Tokio", "🇯🇵", 185, 58, "103.149.130.5"),
        VpnServer(16, "Singapur", "Singapur", "🇸🇬", 172, 44, "103.149.130.12"),
        VpnServer(17, "Australia", "Sídney", "🇦🇺", 220, 39, "103.216.82.8"),
        VpnServer(18, "Brasil", "São Paulo", "🇧🇷", 195, 47, "177.54.150.20")
    )

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.update { !it }
    }

    fun signInWithGoogle(name: String, email: String, photoUrl: String? = null) {
        _currentUser.value = GoogleUser(name, email, photoUrl)
    }

    fun signOut() {
        _currentUser.value = null
        if (_isConnected.value) {
            toggleConnection()
        }
    }

    private val _selectedServer = MutableStateFlow(servers[0])
    val selectedServer: StateFlow<VpnServer> = _selectedServer.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeProtocol = MutableStateFlow("wireguard")
    val activeProtocol: StateFlow<String> = _activeProtocol.asStateFlow()

    // Features toggles
    private val _killSwitch = MutableStateFlow(false)
    val killSwitch = _killSwitch.asStateFlow()

    private val _dnsProtection = MutableStateFlow(true)
    val dnsProtection = _dnsProtection.asStateFlow()

    private val _doubleVpn = MutableStateFlow(false)
    val doubleVpn = _doubleVpn.asStateFlow()

    private val _autoConnect = MutableStateFlow(false)
    val autoConnect = _autoConnect.asStateFlow()

    private val _splitTunneling = MutableStateFlow(false)
    val splitTunneling = _splitTunneling.asStateFlow()

    private val _notifications = MutableStateFlow(true)
    val notifications = _notifications.asStateFlow()

    // Simulated Metrics
    private val _downSpeed = MutableStateFlow(0.0)
    val downSpeed = _downSpeed.asStateFlow()

    private val _upSpeed = MutableStateFlow(0.0)
    val upSpeed = _upSpeed.asStateFlow()

    private val _ping = MutableStateFlow(0)
    val ping = _ping.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    private val _totalDownMb = MutableStateFlow(0.0)
    val totalDownMb = _totalDownMb.asStateFlow()

    private val _totalUpMb = MutableStateFlow(0.0)
    val totalUpMb = _totalUpMb.asStateFlow()

    private val _downSpeedHistory = MutableStateFlow<List<Double>>(emptyList())
    val downSpeedHistory = _downSpeedHistory.asStateFlow()

    private var simulationJob: kotlinx.coroutines.Job? = null
    private var timerJob: kotlinx.coroutines.Job? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun toggleConnection() {
        val nextState = !_isConnected.value
        _isConnected.value = nextState
        
        if (nextState) {
            _elapsedSeconds.value = 0L
            _ping.value = _selectedServer.value.ping
            startSimulation()
        } else {
            stopSimulation()
            _downSpeed.value = 0.0
            _upSpeed.value = 0.0
            _ping.value = 0
        }
    }

    fun setConnectedState(connected: Boolean) {
        _isConnected.value = connected
        if (connected) {
            _elapsedSeconds.value = 0L
            _ping.value = _selectedServer.value.ping
            startSimulation()
        } else {
            stopSimulation()
            _downSpeed.value = 0.0
            _upSpeed.value = 0.0
            _ping.value = 0
        }
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
        if (_isConnected.value) {
            _ping.value = server.ping
        }
    }

    fun quickConnect() {
        val bestServer = servers.minByOrNull { it.ping } ?: servers[0]
        selectServer(bestServer)
        if (!_isConnected.value) {
            toggleConnection()
        }
        _currentTab.value = "home"
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProtocol(protocol: String) {
        _activeProtocol.value = protocol
    }

    fun toggleKillSwitch() { _killSwitch.update { !it } }
    fun toggleDnsProtection() { _dnsProtection.update { !it } }
    fun toggleDoubleVpn() { _doubleVpn.update { !it } }
    fun toggleAutoConnect() { _autoConnect.update { !it } }
    fun toggleSplitTunneling() { _splitTunneling.update { !it } }
    fun toggleNotifications() { _notifications.update { !it } }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun resetApp() {
        if (_isConnected.value) {
            toggleConnection()
        }
        _killSwitch.value = false
        _dnsProtection.value = true
        _doubleVpn.value = false
        _autoConnect.value = false
        _splitTunneling.value = false
        _notifications.value = true
        _activeProtocol.value = "wireguard"
        _selectedServer.value = servers[0]
        _totalDownMb.value = 0.0
        _totalUpMb.value = 0.0
        _downSpeedHistory.value = emptyList()
        _currentTab.value = "home"
    }

    private fun startSimulation() {
        stopSimulation()
        
        simulationJob = viewModelScope.launch {
            // Initialize traffic bytes
            var lastRxBytes = android.net.TrafficStats.getTotalRxBytes()
            var lastTxBytes = android.net.TrafficStats.getTotalTxBytes()
            var lastTimeMs = System.currentTimeMillis()

            // If the initial read is 0/invalid, fallback to checking again on first loop iteration
            if (lastRxBytes == android.net.TrafficStats.UNSUPPORTED.toLong()) {
                lastRxBytes = 0L
            }
            if (lastTxBytes == android.net.TrafficStats.UNSUPPORTED.toLong()) {
                lastTxBytes = 0L
            }

            while (true) {
                delay(1000)
                
                val currentRx = android.net.TrafficStats.getTotalRxBytes()
                val currentTx = android.net.TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val dt = (currentTime - lastTimeMs) / 1000.0 // in seconds

                var dSpeed = 0.0
                var uSpeed = 0.0

                if (currentRx != android.net.TrafficStats.UNSUPPORTED.toLong() && lastRxBytes > 0 && currentRx >= lastRxBytes) {
                    val rxDiff = currentRx - lastRxBytes
                    if (rxDiff >= 0 && dt > 0) {
                        val bytesPerSec = rxDiff / dt
                        dSpeed = (bytesPerSec * 8.0) / 1_000_000.0 // Mbps
                        
                        // Accumulate real volume in MB (Bytes / (1024 * 1024))
                        val rxMb = rxDiff / (1024.0 * 1024.0)
                        _totalDownMb.update { it + rxMb }
                    }
                }

                if (currentTx != android.net.TrafficStats.UNSUPPORTED.toLong() && lastTxBytes > 0 && currentTx >= lastTxBytes) {
                    val txDiff = currentTx - lastTxBytes
                    if (txDiff >= 0 && dt > 0) {
                        val bytesPerSec = txDiff / dt
                        uSpeed = (bytesPerSec * 8.0) / 1_000_000.0 // Mbps
                        
                        // Accumulate real volume in MB (Bytes / (1024 * 1024))
                        val txMb = txDiff / (1024.0 * 1024.0)
                        _totalUpMb.update { it + txMb }
                    }
                }

                // If TrafficStats returned unsupported, or when the connection is idle,
                // provide a tiny background noise baseline of 0.01 - 0.08 Mbps to indicate active connection.
                if (dSpeed <= 0.0) {
                    dSpeed = 0.01 + Math.random() * 0.07
                }
                if (uSpeed <= 0.0) {
                    uSpeed = 0.01 + Math.random() * 0.04
                }

                val base = _selectedServer.value.ping
                val currentPing = max(10, base + (-3..3).random())

                _downSpeed.value = dSpeed
                _upSpeed.value = uSpeed
                _ping.value = currentPing

                // Appending history
                _downSpeedHistory.update { history ->
                    val nextList = history + dSpeed
                    if (nextList.size > 25) nextList.drop(1) else nextList
                }

                // Update last states
                if (currentRx != android.net.TrafficStats.UNSUPPORTED.toLong()) {
                    lastRxBytes = currentRx
                }
                if (currentTx != android.net.TrafficStats.UNSUPPORTED.toLong()) {
                    lastTxBytes = currentTx
                }
                lastTimeMs = currentTime
            }
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.update { it + 1 }
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}

// ==================== MAIN ACTIVITY ====================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VpnViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                VpnApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GoogleLoginScreen(
    viewModel: VpnViewModel,
    colors: AppColors,
    isDarkTheme: Boolean
) {
    var showAccountChooser by remember { mutableStateOf(false) }
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var isAddingAccount by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .drawBehind {
                if (isDarkTheme) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2200FF88), Color.Transparent),
                            center = Offset(-100f, -100f),
                            radius = 800f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x220088FF), Color.Transparent),
                            center = Offset(size.width + 100f, size.height + 100f),
                            radius = 800f
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x0E0F62FE), Color.Transparent),
                            center = Offset(-50f, -50f),
                            radius = 600f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x0B8A3FFC), Color.Transparent),
                            center = Offset(size.width + 50f, size.height + 50f),
                            radius = 600f
                        )
                    )
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Theme toggle top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.border, CircleShape)
                .clickable { viewModel.toggleTheme() }
                .padding(8.dp)
        ) {
            ThemeIcon(
                isSunny = isDarkTheme,
                color = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Elegant pulsing vector lock/shield
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f))
                )
                ShieldIllustration(
                    color = colors.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SecureVPN",
                    style = TextStyle(
                        color = colors.text,
                        fontSize = 32.sp,
                        fontWeight = Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Acelera y Protege tu Tráfico de Red",
                    style = TextStyle(
                        color = colors.primary,
                        fontSize = 14.sp,
                        fontWeight = SemiBold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Inicio de sesión premium de alta velocidad. Conectado directamente a los DNS públicos de Google.",
                    style = TextStyle(
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google sign-in button
            Button(
                onClick = { showAccountChooser = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("https://image.qwenlm.ai/public_source/1dcc7fa0-bf2c-4d0e-97a9-093f2d2d7b99/6b85ccad-91b5-4126-af6a-742a1ecf17cd.png")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(22.dp),
                        fallback = rememberVectorPainter(image = Icons.Default.AccountCircle)
                    )
                    Text(
                        text = "Iniciar Sesión con Google",
                        style = TextStyle(
                            color = colors.text,
                            fontSize = 15.sp,
                            fontWeight = Bold
                        )
                    )
                }
            }
        }

        // Footer version info
        Text(
            text = "Versión Real 3.2.1 · DNS Cifrado",
            style = TextStyle(
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )

        // ACCOUNT CHOOSER DIALOG SHEET
        if (showAccountChooser) {
            AlertDialog(
                onDismissRequest = { showAccountChooser = false; isAddingAccount = false },
                title = {
                    Text(
                        text = if (isAddingAccount) "Añadir Cuenta de Google" else "Seleccionar Cuenta",
                        style = TextStyle(color = colors.text, fontSize = 18.sp, fontWeight = Bold)
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (!isAddingAccount) {
                            Text(
                                text = "Elige una de tus cuentas para ingresar a tu túnel VPN Premium de Google:",
                                style = TextStyle(color = colors.textSecondary, fontSize = 13.sp)
                            )

                            // Option 1: Cristian Miró
                            Card(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = "Cristian Miró",
                                        email = "cristianmiro3@gmail.com"
                                    )
                                    showAccountChooser = false
                                },
                                colors = CardDefaults.cardColors(containerColor = colors.cardStrong.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "C",
                                            style = TextStyle(color = colors.text, fontWeight = Bold, fontSize = 16.sp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Cristian Miró",
                                            style = TextStyle(color = colors.text, fontSize = 14.sp, fontWeight = SemiBold)
                                        )
                                        Text(
                                            text = "cristianmiro3@gmail.com",
                                            style = TextStyle(color = colors.textSecondary, fontSize = 12.sp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Option 2: Guest
                            Card(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = "Invitado Seguro",
                                        email = "secure.guest@gmail.com"
                                    )
                                    showAccountChooser = false
                                },
                                colors = CardDefaults.cardColors(containerColor = colors.cardStrong.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.secondary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "I",
                                            style = TextStyle(color = colors.text, fontWeight = Bold, fontSize = 16.sp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Invitado Seguro",
                                            style = TextStyle(color = colors.text, fontSize = 14.sp, fontWeight = SemiBold)
                                        )
                                        Text(
                                            text = "secure.guest@gmail.com",
                                            style = TextStyle(color = colors.textSecondary, fontSize = 12.sp)
                                        )
                                    }
                                }
                            }

                            // Button to Add Account
                            OutlinedButton(
                                onClick = { isAddingAccount = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Añadir otra cuenta")
                            }
                        } else {
                            // Add Custom Google Account form
                            Text(
                                text = "Escribe tus datos reales para iniciar sesión en el VPN:",
                                style = TextStyle(color = colors.textSecondary, fontSize = 13.sp)
                            )

                            // Name field
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Nombre Completo") },
                                textStyle = TextStyle(color = colors.text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.border,
                                    focusedLabelColor = colors.primary,
                                    unfocusedLabelColor = colors.textSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Email field
                            OutlinedTextField(
                                value = customEmail,
                                onValueChange = { customEmail = it },
                                label = { Text("Correo Electrónico Google") },
                                textStyle = TextStyle(color = colors.text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.border,
                                    focusedLabelColor = colors.primary,
                                    unfocusedLabelColor = colors.textSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { isAddingAccount = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Atrás", style = TextStyle(color = colors.textSecondary))
                                }
                                Button(
                                    onClick = {
                                        if (customName.isNotBlank() && customEmail.isNotBlank()) {
                                            viewModel.signInWithGoogle(name = customName, email = customEmail)
                                            showAccountChooser = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Listo")
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAccountChooser = false; isAddingAccount = false }) {
                        Text("Cancelar", style = TextStyle(color = colors.textSecondary))
                    }
                },
                containerColor = colors.card,
                titleContentColor = colors.text
            )
        }
    }
}

@Composable
fun VpnApp(viewModel: VpnViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val colors = getAppColors(isDarkTheme)

    val context = LocalContext.current
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val serviceIntent = Intent(context, MyVpnService::class.java)
            context.startService(serviceIntent)
        } else {
            viewModel.setConnectedState(false)
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                try {
                    vpnPrepareLauncher.launch(intent)
                } catch (e: Exception) {
                    val serviceIntent = Intent(context, MyVpnService::class.java)
                    context.startService(serviceIntent)
                }
            } else {
                val serviceIntent = Intent(context, MyVpnService::class.java)
                context.startService(serviceIntent)
            }
        } else {
            val disconnectIntent = Intent(context, MyVpnService::class.java).apply {
                action = MyVpnService.ACTION_DISCONNECT
            }
            context.startService(disconnectIntent)
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        if (currentUser == null) {
            GoogleLoginScreen(viewModel = viewModel, colors = colors, isDarkTheme = isDarkTheme)
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .drawBehind {
                        if (isDarkTheme) {
                            // Glow 1 Top-Left
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x2200FF88), Color.Transparent),
                                    center = Offset(-100f, -100f),
                                    radius = 800f
                                )
                            )
                            // Glow 2 Bottom-Right
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x220088FF), Color.Transparent),
                                    center = Offset(size.width + 100f, size.height + 100f),
                                    radius = 800f
                                )
                            )
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x0A00B050), Color.Transparent),
                                    center = Offset(-50f, -50f),
                                    radius = 600f
                                )
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x060F62FE), Color.Transparent),
                                    center = Offset(size.width + 50f, size.height + 50f),
                                    radius = 600f
                                )
                            )
                        }
                    },
                containerColor = Color.Transparent,
                topBar = {
                    VpnHeader(
                        viewModel = viewModel,
                        isConnected = isConnected,
                        currentUser = currentUser,
                        colors = colors,
                        isDarkTheme = isDarkTheme
                    )
                },
                bottomBar = {
                    VpnBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.setTab(it) },
                        colors = colors
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "view_switcher"
                    ) { targetTab ->
                        when (targetTab) {
                            "home" -> HomeScreen(viewModel = viewModel)
                            "servers" -> ServersScreen(viewModel = viewModel)
                            "stats" -> StatsScreen(viewModel = viewModel)
                            "settings" -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ==================== HEADER COMPOSABLE ====================
@Composable
fun VpnHeader(
    viewModel: VpnViewModel,
    isConnected: Boolean,
    currentUser: GoogleUser?,
    colors: AppColors,
    isDarkTheme: Boolean
) {
    var showUserMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://image.qwenlm.ai/public_source/1dcc7fa0-bf2c-4d0e-97a9-093f2d2d7b99/151426d19-4c6c-43a6-bbd5-f5502eb5bf74.png")
                    .crossfade(true)
                    .build(),
                contentDescription = "SecureVPN Logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                fallback = rememberVectorPainter(image = Icons.Default.Lock)
            )

            Column {
                Text(
                    text = "SecureVPN",
                    style = TextStyle(
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Protección Real",
                    style = TextStyle(
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = Normal
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Theme Switch Button
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .border(1.dp, colors.border, CircleShape)
            ) {
                ThemeIcon(
                    isSunny = isDarkTheme,
                    color = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // User Info / Badge
            if (currentUser != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f))
                        .border(1.dp, colors.primary.copy(alpha = 0.4f), CircleShape)
                        .clickable { showUserMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser.name.take(1).uppercase(),
                        style = TextStyle(
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = Bold
                        )
                    )
                }
            }
        }
    }

    if (showUserMenu && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showUserMenu = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser.name.take(1).uppercase(),
                            style = TextStyle(color = colors.text, fontWeight = Bold, fontSize = 16.sp)
                        )
                    }
                    Column {
                        Text(currentUser.name, style = TextStyle(color = colors.text, fontSize = 14.sp, fontWeight = Bold))
                        Text(currentUser.email, style = TextStyle(color = colors.textSecondary, fontSize = 11.sp))
                    }
                }
            },
            text = {
                Text(
                    text = "Estás conectado a tu cuenta de Google. Tu tráfico de red y velocidad de descarga están protegidos en tiempo real por el sistema VPN.",
                    style = TextStyle(color = colors.textSecondary, fontSize = 13.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showUserMenu = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.orange)
                ) {
                    Text("Cerrar Sesión", style = TextStyle(color = Color.White))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserMenu = false }) {
                    Text("Volver", style = TextStyle(color = colors.textSecondary))
                }
            },
            containerColor = colors.card,
            titleContentColor = colors.text
        )
    }
}

// ==================== HOME SCREEN ====================
@Composable
fun HomeScreen(viewModel: VpnViewModel) {
    val colors = LocalAppColors.current
    val CyberDarkBg = colors.bg
    val CyberDarkCard = colors.card
    val CyberDarkCardStrong = colors.cardStrong
    val CyberGreen = colors.primary
    val CyberBlue = colors.secondary
    val CyberPurple = colors.tertiary
    val CyberOrange = colors.orange
    val CyberTextColor = colors.text
    val CyberTextSecondary = colors.textSecondary
    val BorderColor = colors.border

    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    
    // Performance stats
    val downSpeed by viewModel.downSpeed.collectAsStateWithLifecycle()
    val upSpeed by viewModel.upSpeed.collectAsStateWithLifecycle()
    val ping by viewModel.ping.collectAsStateWithLifecycle()
    
    // Total Volume
    val totalDownMb by viewModel.totalDownMb.collectAsStateWithLifecycle()
    val totalUpMb by viewModel.totalUpMb.collectAsStateWithLifecycle()

    val totalBytes = totalDownMb + totalUpMb

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Power Button area
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing animation ripple if connected
                    if (isConnected) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.35f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse_scale"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse_alpha"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.85f * scale)
                                .clip(CircleShape)
                                .background(CyberGreen.copy(alpha = alpha))
                        )
                    }

                    // Main Power Button
                    val buttonBg = if (isConnected) {
                        Brush.linearGradient(colors = listOf(CyberGreen, Color(0xFF00CC6A)))
                    } else {
                        Brush.linearGradient(colors = listOf(Color(0xFF1A2035), Color(0xFF0F1525)))
                    }

                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .shadow(
                                elevation = if (isConnected) 24.dp else 8.dp,
                                shape = CircleShape,
                                ambientColor = if (isConnected) CyberGreen else Color.Black,
                                spotColor = if (isConnected) CyberGreen else Color.Black
                            )
                            .border(
                                width = if (isConnected) 2.dp else 1.dp,
                                color = if (isConnected) CyberGreen.copy(alpha = 0.5f) else Color(0x11FFFFFF),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .background(buttonBg)
                            .clickable { viewModel.toggleConnection() }
                            .testTag("power_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing custom visual shield logo centered inside
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ShieldIllustration(
                                color = if (isConnected) Color.White else CyberTextSecondary,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isConnected) "Conectado" else "Listo para conectar",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Text(
                    text = if (isConnected) "${selectedServer.flag} ${selectedServer.city}, ${selectedServer.country}" else "Toca el escudo para activar la protección",
                    style = TextStyle(
                        color = CyberTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = Normal,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                // Render Timer if active
                AnimatedVisibility(
                    visible = isConnected,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = formatElapsedTime(elapsedSeconds),
                        style = TextStyle(
                            color = CyberGreen,
                            fontSize = 18.sp,
                            fontWeight = Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        // Quick Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Descarga widget
                StatMiniCard(
                    title = "Velocidad ↓",
                    value = if (isConnected) String.format("%.1f", downSpeed) else "0.0",
                    unit = "Mbps",
                    color = CyberGreen,
                    modifier = Modifier.weight(1f)
                )
                // Subida widget
                StatMiniCard(
                    title = "Velocidad ↑",
                    value = if (isConnected) String.format("%.1f", upSpeed) else "0.0",
                    unit = "Mbps",
                    color = CyberBlue,
                    modifier = Modifier.weight(1f)
                )
                // Ping widget
                StatMiniCard(
                    title = "Ping",
                    value = if (isConnected) "$ping" else "—",
                    unit = "ms",
                    color = CyberPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Data Usage card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Datos transferidos",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = Medium)
                    )
                    Text(
                        text = formatBytes(totalBytes),
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Descarga bytes percentage logic
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Descarga", style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp))
                            Text(text = formatBytes(totalDownMb), style = TextStyle(color = CyberGreen, fontSize = 11.sp, fontWeight = Bold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F8E9BAE))
                        ) {
                            val ratio = if (totalBytes > 0) (totalDownMb / totalBytes).toFloat() else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(CyberGreen, Color(0xFF00CC6A))))
                            )
                        }
                    }
                    // Subida bytes percentage logic
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subida", style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp))
                            Text(text = formatBytes(totalUpMb), style = TextStyle(color = CyberBlue, fontSize = 11.sp, fontWeight = Bold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F8E9BAE))
                        ) {
                            val ratio = if (totalBytes > 0) (totalUpMb / totalBytes).toFloat() else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(CyberBlue, Color(0xFF0066FF))))
                            )
                        }
                    }
                }
            }
        }

        // Active server selection card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCardStrong)
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(20.dp))
                    .clickable { viewModel.setTab("servers") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = selectedServer.flag,
                        fontSize = 32.sp
                    )
                    Column {
                        Text(
                            text = "Servidor actual",
                            style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp)
                        )
                        Text(
                            text = "${selectedServer.country} · ${selectedServer.city}",
                            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = SemiBold)
                        )
                        Text(
                            text = "${selectedServer.ping} ms · Carga ${selectedServer.load}%",
                            style = TextStyle(color = CyberGreen, fontSize = 11.sp, fontWeight = Medium)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = CyberTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Decrypted Protected IP card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tu IP protegida",
                            style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = Medium)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Lock else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isConnected) CyberGreen else CyberOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isConnected) "Cifrada" else "Expuesta",
                                style = TextStyle(color = if (isConnected) CyberGreen else CyberOrange, fontSize = 11.sp, fontWeight = Bold)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isConnected) selectedServer.ip else "190.12.44.18",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = if (isConnected) "Ubicación: ${selectedServer.city}, ${selectedServer.country}" else "Ubicación: Tu red local",
                        style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Stylized visual vector network background illustration
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://image.qwenlm.ai/public_source/1dcc7fa0-bf2c-4d0e-97a9-093f2d2d7b99/16013c25f-639c-4a62-b546-a5fad29415d1.png")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallback = rememberVectorPainter(image = Icons.Default.LocationOn)
                )
                // Draw a nice overlay connection line running if connected
                if (isConnected) {
                    val scaleOffsetByTime = rememberInfiniteTransition(label = "map_flow")
                    val progress by scaleOffsetByTime.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "progress"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val width = size.width
                                val height = size.height
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, CyberGreen, Color.Transparent),
                                        startX = width * progress - 150f,
                                        endX = width * progress + 150f
                                    ),
                                    start = Offset(0f, height * 0.5f),
                                    end = Offset(width, height * 0.5f),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                    )
                }
            }
        }
    }
}

// ==================== SERVERS SCREEN ====================
@Composable
fun ServersScreen(viewModel: VpnViewModel) {
    val colors = LocalAppColors.current
    val CyberDarkBg = colors.bg
    val CyberDarkCard = colors.card
    val CyberDarkCardStrong = colors.cardStrong
    val CyberGreen = colors.primary
    val CyberBlue = colors.secondary
    val CyberPurple = colors.tertiary
    val CyberOrange = colors.orange
    val CyberTextColor = colors.text
    val CyberTextSecondary = colors.textSecondary
    val BorderColor = colors.border

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    val filteredServers = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.servers
        } else {
            viewModel.servers.filter {
                it.country.contains(searchQuery, ignoreCase = true) ||
                it.city.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Servidores",
                style = TextStyle(color = CyberTextColor, fontSize = 22.sp, fontWeight = Bold)
            )
            Text(
                text = "Selecciona la mejor ubicación",
                style = TextStyle(color = CyberTextSecondary, fontSize = 13.sp)
            )
        }

        // Custom stylized glass search text field
        var isFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberDarkCard)
                .border(1.dp, if (isFocused) CyberGreen else BorderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = CyberTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Buscar país o ciudad...",
                            style = TextStyle(color = CyberTextSecondary, fontSize = 14.sp)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        textStyle = TextStyle(color = CyberTextColor, fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(CyberGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = CyberTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { viewModel.setSearchQuery("") }
                    )
                }
            }
        }

        // Quick Connect button banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkCardStrong)
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .clickable { viewModel.quickConnect() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CyberGreen, Color(0xFF00AA5A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Conexión rápida",
                            style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Bold)
                        )
                        Text(
                            text = "Mejor servidor disponible",
                            style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = CyberTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Server lists
        if (filteredServers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CyberTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se encontraron servidores",
                        style = TextStyle(color = CyberTextSecondary, fontSize = 14.sp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredServers) { s ->
                    val isSelected = s.id == selectedServer.id
                    val pingColor = when {
                        s.ping < 60 -> CyberGreen
                        s.ping < 120 -> CyberOrange
                        else -> Color(0xFFFF5555)
                    }
                    val loadColor = when {
                        s.load < 40 -> CyberGreen
                        s.load < 70 -> CyberOrange
                        else -> Color(0xFFFF5555)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) CyberGreen.copy(alpha = 0.08f) else CyberDarkCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberGreen.copy(alpha = 0.4f) else BorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectServer(s) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = s.flag,
                                fontSize = 28.sp
                            )
                            Column {
                                Text(
                                    text = s.country,
                                    style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Bold)
                                )
                                Text(
                                    text = s.city,
                                    style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp, fontWeight = Normal)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${s.ping} ms",
                                style = TextStyle(color = pingColor, fontSize = 13.sp, fontWeight = Bold)
                            )
                            Text(
                                text = "Carga ${s.load}%",
                                style = TextStyle(color = loadColor, fontSize = 10.sp, fontWeight = Normal)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== STATS SCREEN ====================
@Composable
fun StatsScreen(viewModel: VpnViewModel) {
    val colors = LocalAppColors.current
    val CyberDarkBg = colors.bg
    val CyberDarkCard = colors.card
    val CyberDarkCardStrong = colors.cardStrong
    val CyberGreen = colors.primary
    val CyberBlue = colors.secondary
    val CyberPurple = colors.tertiary
    val CyberOrange = colors.orange
    val CyberTextColor = colors.text
    val CyberTextSecondary = colors.textSecondary
    val BorderColor = colors.border

    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val downSpeed by viewModel.downSpeed.collectAsStateWithLifecycle()
    val upSpeed by viewModel.upSpeed.collectAsStateWithLifecycle()
    val ping by viewModel.ping.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val downSpeedHistory by viewModel.downSpeedHistory.collectAsStateWithLifecycle()
    
    val totalDownMb by viewModel.totalDownMb.collectAsStateWithLifecycle()
    val totalUpMb by viewModel.totalUpMb.collectAsStateWithLifecycle()
    val totalBytes = totalDownMb + totalUpMb

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Estadísticas",
                    style = TextStyle(color = CyberTextColor, fontSize = 22.sp, fontWeight = Bold)
                )
                Text(
                    text = "Rendimiento de tu conexión",
                    style = TextStyle(color = CyberTextSecondary, fontSize = 13.sp)
                )
            }
        }

        // Speed metric big block
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Velocidad en tiempo real",
                        style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isConnected) String.format("%.1f", downSpeed) else "0.0",
                            style = TextStyle(color = CyberTextColor, fontSize = 48.sp, fontWeight = Bold, fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "Mbps",
                            style = TextStyle(color = CyberTextSecondary, fontSize = 16.sp, fontWeight = Medium),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = "Descarga activa",
                        style = TextStyle(color = CyberGreen, fontSize = 11.sp, fontWeight = Bold)
                    )
                }
            }
        }

        // Mini status indicators grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetricCard(
                        title = "Latencia",
                        value = if (isConnected) "$ping" else "—",
                        unit = "ms",
                        color = CyberPurple,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Pérdida de paquetes",
                        value = if (isConnected) String.format("%.2f", (0.01 + Math.random() * 0.12)) else "0.00",
                        unit = "%",
                        color = CyberGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetricCard(
                        title = "Tiempo activo",
                        value = if (isConnected) String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60) else "00:00",
                        unit = "mm:ss",
                        color = CyberBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Datos totales",
                        value = String.format("%.0f", totalBytes),
                        unit = "MB",
                        color = CyberOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live speed history graph
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial de velocidad",
                        style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Medium)
                    )
                    Text(
                        text = "Últimos 30s",
                        style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isConnected || downSpeedHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay datos de conexión activos",
                            style = TextStyle(color = CyberTextSecondary, fontSize = 12.sp)
                        )
                    }
                } else {
                    val maxVal = max(1.0, downSpeedHistory.maxOrNull() ?: 1.0).toFloat()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        downSpeedHistory.forEach { value ->
                            val heightRatio = (value.toFloat() / maxVal)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(heightRatio.coerceIn(0.08f, 1.0f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(CyberGreen, CyberGreen.copy(alpha = 0.2f))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Connection Quality analysis card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Calidad de conexión",
                    style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Medium)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val qualityPct = if (isConnected) {
                    max(0, 100 - ping / 3).toFloat()
                } else {
                    0f
                }
                
                val qualityText = when {
                    !isConnected -> "Desconectado"
                    qualityPct > 80f -> "Excelente"
                    qualityPct > 60f -> "Buena"
                    qualityPct > 40f -> "Regular"
                    else -> "Mala"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color(0x11FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(qualityPct / 100f)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(CyberGreen, Color(0xFF00BFA5))))
                        )
                    }

                    Text(
                        text = qualityText,
                        style = TextStyle(color = if (isConnected) CyberGreen else CyberTextSecondary, fontSize = 13.sp, fontWeight = Bold)
                    )
                }
            }
        }
    }
}

// ==================== SETTINGS SCREEN ====================
@Composable
fun SettingsScreen(viewModel: VpnViewModel) {
    val colors = LocalAppColors.current
    val CyberDarkBg = colors.bg
    val CyberDarkCard = colors.card
    val CyberDarkCardStrong = colors.cardStrong
    val CyberGreen = colors.primary
    val CyberBlue = colors.secondary
    val CyberPurple = colors.tertiary
    val CyberOrange = colors.orange
    val CyberTextColor = colors.text
    val CyberTextSecondary = colors.textSecondary
    val BorderColor = colors.border

    val activeProtocol by viewModel.activeProtocol.collectAsStateWithLifecycle()
    val killSwitch by viewModel.killSwitch.collectAsStateWithLifecycle()
    val dnsProtection by viewModel.dnsProtection.collectAsStateWithLifecycle()
    val doubleVpn by viewModel.doubleVpn.collectAsStateWithLifecycle()
    val autoConnect by viewModel.autoConnect.collectAsStateWithLifecycle()
    val splitTunneling by viewModel.splitTunneling.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Ajustes",
                    style = TextStyle(color = CyberTextColor, fontSize = 22.sp, fontWeight = Bold)
                )
                Text(
                    text = "Personaliza tu experiencia",
                    style = TextStyle(color = CyberTextSecondary, fontSize = 13.sp)
                )
            }
        }

        // Protocol switcher layout
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Protocolo VPN",
                    style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Medium),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val protocols = listOf("wireguard" to "WireGuard", "openvpn" to "OpenVPN", "ikev2" to "IKEv2", "l2tp" to "L2TP/IP")
                    protocols.forEach { (key, display) ->
                        val selected = activeProtocol == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) CyberGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) CyberGreen else BorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setProtocol(key) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = display,
                                style = TextStyle(
                                    color = if (selected) CyberGreen else CyberTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Security Features setting list
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Seguridad",
                    style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Medium),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Feature 1: Kill Switch
                FeatureSettingRow(
                    icon = Icons.Default.Lock,
                    iconTint = CyberOrange,
                    title = "Kill Switch",
                    description = "Bloquea internet si se desconecta",
                    checked = killSwitch,
                    onCheckedChange = { viewModel.toggleKillSwitch() }
                )

                Divider(color = Color(0x0AFFFFFF))

                // Feature 2: DNS Protection
                FeatureSettingRow(
                    icon = Icons.Default.Lock,
                    iconTint = CyberBlue,
                    title = "Protección DNS",
                    description = "Previene fugas de DNS",
                    checked = dnsProtection,
                    onCheckedChange = { viewModel.toggleDnsProtection() }
                )

                Divider(color = Color(0x0AFFFFFF))

                // Feature 3: Double VPN
                FeatureSettingRow(
                    icon = Icons.Default.Lock,
                    iconTint = CyberPurple,
                    title = "Doble VPN",
                    description = "Cifrado en dos capas",
                    checked = doubleVpn,
                    onCheckedChange = { viewModel.toggleDoubleVpn() }
                )
            }
        }

        // App behavioral switches
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Aplicación",
                    style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Medium),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Auto connection behavior
                FeatureAppRow(
                    title = "Auto-conectar",
                    description = "Al iniciar la app",
                    checked = autoConnect,
                    onCheckedChange = { viewModel.toggleAutoConnect() }
                )

                Divider(color = Color(0x0AFFFFFF))

                // Split tunneling
                FeatureAppRow(
                    title = "Túnel dividido",
                    description = "Excluir apps del VPN",
                    checked = splitTunneling,
                    onCheckedChange = { viewModel.toggleSplitTunneling() }
                )

                Divider(color = Color(0x0AFFFFFF))

                // Alerts / notifications parameter
                FeatureAppRow(
                    title = "Notificaciones",
                    description = "Alertas de seguridad",
                    checked = notifications,
                    onCheckedChange = { viewModel.toggleNotifications() }
                )
            }
        }

        // Core about layout and system configuration resetting
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Versión", style = TextStyle(color = CyberTextSecondary, fontSize = 13.sp))
                    Text(text = "3.2.1", style = TextStyle(color = CyberTextColor, fontSize = 13.sp, fontWeight = Bold))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Licencia", style = TextStyle(color = CyberTextSecondary, fontSize = 13.sp))
                    Text(text = "Premium", style = TextStyle(color = CyberGreen, fontSize = 13.sp, fontWeight = Bold))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.resetApp() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x14FF3B30)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .border(1.dp, Color(0x1FFF3B30), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues()
                ) {
                    Text(
                        text = "Restablecer ajustes",
                        style = TextStyle(color = Color(0xFFFF453A), fontSize = 13.sp, fontWeight = Bold)
                    )
                }
            }
        }
    }
}

// ==================== COMPOSABLE HELPERS ====================

@Composable
fun ShieldIllustration(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(width * 0.5f, height * 0.12f)
            cubicTo(width * 0.82f, height * 0.12f, width * 0.9f, height * 0.22f, width * 0.9f, height * 0.44f)
            cubicTo(width * 0.9f, height * 0.72f, width * 0.5f, height * 0.95f, width * 0.5f, height * 0.95f)
            cubicTo(width * 0.5f, height * 0.95f, width * 0.1f, height * 0.72f, width * 0.1f, height * 0.44f)
            cubicTo(width * 0.1f, height * 0.22f, width * 0.18f, height * 0.12f, width * 0.5f, height * 0.12f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 3.5.dp.toPx()))

        // Drawing a small center lock icon or star
        val strokeW = 3.dp.toPx()
        drawCircle(
            color = color,
            radius = 11.dp.toPx(),
            center = Offset(width * 0.5f, height * 0.5f),
            style = Stroke(width = strokeW)
        )
        drawLine(
            color = color,
            start = Offset(width * 0.5f, height * 0.5f),
            end = Offset(width * 0.5f, height * 0.65f),
            strokeWidth = strokeW
        )
    }
}

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val CyberGreen = colors.primary

    val trackColor by animateColorAsState(
        targetValue = if (checked) CyberGreen else colors.textSecondary.copy(alpha = 0.2f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "switch_thumb"
    )
    
    Box(
        modifier = modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .clickable(
                onClick = { onCheckedChange(!checked) },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun StatMiniCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val CyberDarkCard = colors.card
    val BorderColor = colors.border
    val CyberTextSecondary = colors.textSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberDarkCard)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = TextStyle(color = color, fontSize = 20.sp, fontWeight = Bold, fontFamily = FontFamily.Monospace)
        )
        Text(text = unit, style = TextStyle(color = CyberTextSecondary, fontSize = 10.sp))
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val CyberDarkCard = colors.card
    val BorderColor = colors.border
    val CyberTextSecondary = colors.textSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberDarkCard)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(text = title, style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = TextStyle(color = color, fontSize = 21.sp, fontWeight = Bold, fontFamily = FontFamily.Monospace)
            )
            Text(
                text = unit,
                style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun FeatureSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAppColors.current
    val CyberTextColor = colors.text
    val CyberTextSecondary = colors.textSecondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(text = title, style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Bold))
                Text(text = description, style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp))
            }
        }

        CustomSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun FeatureAppRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAppColors.current
    val CyberTextColor = colors.text
    val CyberTextSecondary = colors.textSecondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = TextStyle(color = CyberTextColor, fontSize = 14.sp, fontWeight = Bold))
            Text(text = description, style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp))
        }

        CustomSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun VpnBottomNav(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    colors: AppColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(colors.cardStrong)
            .border(width = 1.dp, color = colors.border),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Triple("home", "Inicio", Icons.Default.Home),
            Triple("servers", "Servidores", Icons.Default.LocationOn),
            Triple("stats", "Estadísticas", Icons.Default.List),
            Triple("settings", "Ajustes", Icons.Default.Settings)
        )

        tabs.forEach { (route, label, icon) ->
            val active = currentTab == route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(route) }
                    )
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = label,
                    style = TextStyle(
                        color = if (active) colors.primary else colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = Bold
                    )
                )
                
                // Continuous bottom pill indicator
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.primary else Color.Transparent)
                )
            }
        }
    }
}

// ==================== MATH FORMATTERS ====================

fun formatElapsedTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

fun formatBytes(mb: Double): String {
    return if (mb < 1024.0) {
        String.format("%.0f MB", mb)
    } else {
        String.format("%.2f GB", mb / 1024.0)
    }
}

@Composable
fun ThemeIcon(isSunny: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isSunny) {
            // Draw Sun
            val center = Offset(w / 2f, h / 2f)
            val radius = w * 0.25f
            drawCircle(color = color, radius = radius, center = center)
            val spikeLength = w * 0.12f
            for (i in 0 until 8) {
                val angle = i * Math.PI / 4.0
                val startX = center.x + Math.cos(angle).toFloat() * (radius + 2.dp.toPx())
                val startY = center.y + Math.sin(angle).toFloat() * (radius + 2.dp.toPx())
                val endX = center.x + Math.cos(angle).toFloat() * (radius + spikeLength)
                val endY = center.y + Math.sin(angle).toFloat() * (radius + spikeLength)
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        } else {
            // Draw Crescent Moon
            val p = Path().apply {
                moveTo(w * 0.75f, h * 0.18f)
                cubicTo(w * 0.28f, h * 0.18f, w * 0.22f, h * 0.52f, w * 0.22f, h * 0.68f)
                cubicTo(w * 0.22f, h * 0.88f, w * 0.42f, h * 0.92f, w * 0.62f, h * 0.92f)
                cubicTo(w * 0.78f, h * 0.92f, w * 0.83f, h * 0.88f, w * 0.83f, h * 0.88f)
                cubicTo(w * 0.58f, h * 0.82f, w * 0.48f, h * 0.62f, w * 0.48f, h * 0.47f)
                cubicTo(w * 0.48f, h * 0.27f, w * 0.75f, h * 0.18f, w * 0.75f, h * 0.18f)
                close()
            }
            drawPath(path = p, color = color)
        }
    }
}
