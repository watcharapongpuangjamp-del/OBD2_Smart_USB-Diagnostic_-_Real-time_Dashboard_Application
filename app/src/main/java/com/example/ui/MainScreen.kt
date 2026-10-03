package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.obd.OBD2Protocol
import com.example.obd.OBD2Command
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(viewModel: OBDViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Dashboard") }, icon = { Icon(imageVector = Icons.Default.Build, contentDescription = null) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DTCs") }, icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("History") }, icon = { Icon(imageVector = Icons.Default.History, contentDescription = null) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Perf") }, icon = { Icon(imageVector = Icons.Default.Speed, contentDescription = null) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Service") }, icon = { Icon(imageVector = Icons.Default.Build, contentDescription = null) })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Terminal") }, icon = { Icon(imageVector = Icons.Default.Terminal, contentDescription = null) })
            Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("Settings") }, icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) })
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> DashboardScreen(uiState, viewModel)
                1 -> DtcScreen(uiState, viewModel)
                2 -> HistoryScreen(uiState, viewModel)
                3 -> PerformanceScreen(uiState, viewModel)
                4 -> ServiceCenterScreen(uiState, viewModel)
                5 -> TerminalScreen(uiState, viewModel)
                6 -> SettingsScreen(uiState, viewModel)
            }
        }
        
        ConnectionStatusFooter(uiState)
    }
}

@Composable
fun DashboardScreen(state: OBDUiState, viewModel: OBDViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = com.example.R.drawable.dashboard_hero,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.2f),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.isConnected) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Device Disconnected", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { viewModel.connect() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Connect Now")
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GaugeCard(label = "RPM", value = state.rpm.toString(), unit = "rpm", modifier = Modifier.weight(1f), color = Color(0xFF4CAF50))
                GaugeCard(label = "Speed", value = state.speed.toString(), unit = "km/h", modifier = Modifier.weight(1f), color = Color(0xFF2196F3))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GaugeCard(label = "Coolant", value = state.coolantTemp.toString(), unit = "°C", modifier = Modifier.weight(1f), color = Color(0xFFFF9800))
                GaugeCard(label = "Load", value = "%.1f".format(state.engineLoad), unit = "%", modifier = Modifier.weight(1f), color = Color(0xFFE91E63))
            }
            GaugeCard(label = "Battery Voltage", value = state.voltage, unit = "", modifier = Modifier.fillMaxWidth(), color = Color(0xFF9C27B0))
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { viewModel.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect")
            }
        }
    }
}
}

@Composable
fun GaugeCard(label: String, value: String, unit: String, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = color)
            if (unit.isNotEmpty()) {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

@Composable
fun DtcScreen(state: OBDUiState, viewModel: OBDViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Diagnostic Trouble Codes", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (state.isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Scanning ECU...", modifier = Modifier.padding(top = 8.dp))
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.scanDTCs() }, modifier = Modifier.weight(1f)) {
                    Text("Scan Codes")
                }
                OutlinedButton(onClick = { viewModel.clearDTCs() }, modifier = Modifier.weight(1f)) {
                    Text("Clear Codes")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (state.dtcs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No Fault Codes Found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.dtcs) { dtc ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dtc, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Stored Fault Code", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { viewModel.getAiAdvice(dtc) }) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Advice", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        
                        if (state.aiAdvice.containsKey(dtc)) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Diagnostic Advice", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = state.aiAdvice[dtc] ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        
        if (state.isAnalyzingAi) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Card {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("AI is analyzing...")
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceScreen(state: OBDUiState, viewModel: OBDViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Performance Test", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "0-100 km/h", style = MaterialTheme.typography.titleLarge)
        Text(text = "${state.accelerationTime} s", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = { viewModel.startPerformanceTest() }, modifier = Modifier.fillMaxWidth()) {
            Text("Start 0-100 Test")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = { viewModel.toggleLogging() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (state.isLogging) Color.Red else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (state.isLogging) "Stop Data Logging" else "Start Data Logging")
        }
    }
}

@Composable
fun ServiceCenterScreen(state: OBDUiState, viewModel: OBDViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Service & Calibration", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Smart-Check Overview", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("System Voltage: ${state.voltage}", style = MaterialTheme.typography.bodyLarge)
                Text("Charging Status: ${if (state.voltage.replace("V","").toDoubleOrNull() ?: 0.0 > 13.5) "Charging" else "Normal/Low"}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Maintenance Tools (Dangerous)", style = MaterialTheme.typography.titleMedium, color = Color.Red)
        
        LazyColumn {
            item {
                ServiceButton("Throttle Body Reset", { viewModel.performService(OBD2Command.ThrottleReset) })
                ServiceButton("Injector Adaptation", { viewModel.performService(OBD2Command.InjectorAdaptation) })
                ServiceButton("ECU System Test", { viewModel.performService(OBD2Command.TestECU) })
            }
        }
    }
}

@Composable
fun ServiceButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Text(label)
    }
}

@Composable
fun HistoryScreen(state: OBDUiState, viewModel: OBDViewModel) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Scan History", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { viewModel.clearScanHistory() }) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear All History")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (state.scanHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scan history found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.scanHistory) { result ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = dateFormat.format(Date(result.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.deleteScan(result.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Scan", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("Protocol: ${result.protocol}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Detected DTCs:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = result.dtcs.replace(",", ", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Red,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.getAiAdvice(result.dtcs.split(",").first()) },
                                    modifier = Modifier.padding(start = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Fix", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalScreen(state: OBDUiState, viewModel: OBDViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Command Terminal", style = MaterialTheme.typography.headlineSmall)
                Text("Raw communication logs", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = { viewModel.clearHistory() }) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(8.dp),
            reverseLayout = true
        ) {
            items(state.commandHistory.reversed()) { log ->
                Text(
                    text = log,
                    color = if (log.startsWith(">>")) Color(0xFF00FF00) else Color(0xFF00CCFF),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(state: OBDUiState, viewModel: OBDViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf(OBD2Protocol.AUTO) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configuration", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("OBD2 Protocol Selection", style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedProtocol.description)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                OBD2Protocol.entries.forEach { protocol ->
                    DropdownMenuItem(
                        text = { Text(protocol.description) },
                        onClick = {
                            selectedProtocol = protocol
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.connect(selectedProtocol) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Re-Initialize with Selected Protocol")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { viewModel.resetAdapter() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset ELM327 Adapter (AT Z)")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Debug Info", style = MaterialTheme.typography.labelLarge)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Protocol in Use: ${state.currentProtocol}")
                Text("Connection: ${state.connectionStatus}")
            }
        }
    }
}

@Composable
fun ConnectionStatusFooter(state: OBDUiState) {
    Surface(color = if (state.isConnected) Color(0xFF4CAF50) else Color.Gray, contentColor = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = if (state.isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(state.connectionStatus, style = MaterialTheme.typography.labelSmall)
        }
    }
}
