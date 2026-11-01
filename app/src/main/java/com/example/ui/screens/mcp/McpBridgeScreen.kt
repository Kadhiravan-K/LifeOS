package com.example.ui.screens.mcp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.McpAuditLogEntity
import com.example.data.local.entities.McpServerEntity
import com.example.mcp.McpToolDefinition
import com.example.ui.components.McpServerCard
import com.example.ui.viewmodel.LifeViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpBridgeScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Server (Inbound)", "External Clients", "Security & Audit")

    val mcpEnabled by viewModel.mcpEnabled.collectAsStateWithLifecycle()
    val mcpAuthToken by viewModel.mcpAuthToken.collectAsStateWithLifecycle()
    val mcpAllowedProfileId by viewModel.mcpAllowedProfileId.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfilesFlow.collectAsStateWithLifecycle()
    val mcpServers by viewModel.mcpServers.collectAsStateWithLifecycle()
    val mcpAuditLogs by viewModel.mcpAuditLogs.collectAsStateWithLifecycle()

    var showToken by remember { mutableStateOf(false) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var showClearAuditDialog by remember { mutableStateOf(false) }

    // External Tools Discovery & Execution Dialog States
    var inspectingServer by remember { mutableStateOf<McpServerEntity?>(null) }
    var discoveredToolsList by remember { mutableStateOf<List<McpToolDefinition>>(emptyList()) }
    var isLoadingTools by remember { mutableStateOf(false) }
    var selectedToolForExecution by remember { mutableStateOf<McpToolDefinition?>(null) }
    var toolParamInput by remember { mutableStateOf("{}") }
    var executionResultText by remember { mutableStateOf("") }
    var showApprovalDialog by remember { mutableStateOf(false) }
    var isExecutingExternalTool by remember { mutableStateOf(false) }

    // Inbound Console
    var consoleInput by remember {
        mutableStateOf("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")
    }
    var consoleOutput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Model Context Protocol (MCP)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("100% Offline Bidirectional Integration (#3)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("mcp_bridge_screen")
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: LifeOS Server (Inbound)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Overview Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("LifeOS Sovereign MCP Server", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Expose 27 tools, 7 resources, and 4 prompts to authorized local AI assistants (Cursor, Claude Desktop, local agents) via local IPC / JSON-RPC 2.0 frames.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enable Inbound MCP Server", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            if (mcpEnabled) "Server is ACTIVE and listening for authorized JSON-RPC requests." else "Server is inactive. External agents cannot access data.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = mcpEnabled,
                                        onCheckedChange = { checked ->
                                            viewModel.setMcpEnabled(checked)
                                            if (checked && mcpAuthToken.isBlank()) {
                                                viewModel.regenerateMcpToken()
                                            }
                                        },
                                        modifier = Modifier.testTag("mcp_toggle_switch")
                                    )
                                }

                                if (mcpEnabled) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Authorized Profile Boundary", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("AI tools are strictly quarantined to this profile. Personal data remains isolated.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        allProfiles.forEach { prof ->
                                            FilterChip(
                                                selected = prof.id == mcpAllowedProfileId,
                                                onClick = { viewModel.setMcpAllowedProfileId(prof.id) },
                                                label = { Text("${prof.emoji} ${prof.name}") },
                                                leadingIcon = if (prof.id == mcpAllowedProfileId) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bearer Token Authentication
                        AnimatedVisibility(visible = mcpEnabled) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Bearer Authentication Token", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("32-character constant-time bearer token required in all JSON-RPC requests.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (showToken) mcpAuthToken else "•".repeat(mcpAuthToken.length.coerceAtLeast(24)),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { showToken = !showToken }) {
                                                Icon(
                                                    if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle Visibility",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { copyToClipboard("Bearer Token", mcpAuthToken) }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Token", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = { showRegenerateDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Regenerate Token", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive MCP Test Console
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Interactive MCP Playground", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Execute MCP JSON-RPC 2.0 messages directly against the local engine.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Preset Test Commands:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val presets = listOf(
                                        "initialize" to """{"jsonrpc":"2.0","id":1,"method":"initialize"}""",
                                        "tools/list" to """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""",
                                        "get_tasks" to """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"get_tasks","arguments":{"status":"ALL"}}}""",
                                        "daily_summary" to """{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_daily_summary","arguments":{}}}""",
                                        "productivity_radar" to """{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"get_productivity_radar","arguments":{"horizon":"WEEKLY"}}}""",
                                        "resources/list" to """{"jsonrpc":"2.0","id":6,"method":"resources/list"}""",
                                        "prompts/list" to """{"jsonrpc":"2.0","id":7,"method":"prompts/list"}"""
                                    )

                                    presets.forEach { (label, json) ->
                                        FilledTonalButton(
                                            onClick = { consoleInput = json },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(label, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = consoleInput,
                                    onValueChange = { consoleInput = it },
                                    label = { Text("JSON-RPC Request") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    minLines = 3,
                                    maxLines = 6
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isExecuting = true
                                            consoleOutput = viewModel.executeMcpRequest(consoleInput)
                                            isExecuting = false
                                        }
                                    },
                                    enabled = !isExecuting,
                                    modifier = Modifier.fillMaxWidth().testTag("mcp_execute_button")
                                ) {
                                    if (isExecuting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Dispatch JSON-RPC Message")
                                    }
                                }

                                if (consoleOutput.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("JSON-RPC Response:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        IconButton(onClick = { copyToClipboard("MCP Response", consoleOutput) }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Output", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = consoleOutput,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                1 -> {
                    // TAB 1: External Clients (Outbound)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("LifeOS MCP Client Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Connect to external or local AI MCP servers, discover declared tools, and invoke capabilities with mandatory explicit user approval.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Registered MCP Servers (${mcpServers.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Button(
                                onClick = { showAddServerDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Server", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (mcpServers.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No External MCP Servers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Register an in-process local agent (ipc://assistant) or external JSON-RPC server to discover tools and resources.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            mcpServers.forEach { server ->
                                McpServerCard(
                                    server = server,
                                    onConnect = { viewModel.connectExternalMcpServer(server) },
                                    onDisconnect = { viewModel.disconnectExternalMcpServer(server) },
                                    onDiscoverTools = {
                                        inspectingServer = server
                                        isLoadingTools = true
                                        viewModel.discoverExternalMcpTools(server) { res ->
                                            res.onSuccess { discoveredToolsList = it }
                                            isLoadingTools = false
                                        }
                                    },
                                    onDelete = { viewModel.removeExternalMcpServer(server.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                2 -> {
                    // TAB 2: Security & Audit Log
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("MCP Security & Audit Trail", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Cryptographically logged inbound and outbound protocol activity. All write operations and external tool approvals are recorded.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Audit History (${mcpAuditLogs.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (mcpAuditLogs.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { showClearAuditDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear History", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (mcpAuditLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No MCP security events recorded yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(mcpAuditLogs, key = { it.id }) { log ->
                                    AuditLogRow(log = log)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: Add External Server
    if (showAddServerDialog) {
        var serverName by remember { mutableStateOf("") }
        var serverEndpoint by remember { mutableStateOf("ipc://assistant") }
        var serverTransport by remember { mutableStateOf("LOCAL_IPC") }
        var serverAuthToken by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddServerDialog = false },
            title = { Text("Add External MCP Server") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g. Sovereign Assistant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serverEndpoint,
                        onValueChange = { serverEndpoint = it },
                        label = { Text("Endpoint URI / URL") },
                        placeholder = { Text("ipc://assistant or http://10.0.2.2:8000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Transport Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = serverTransport == "LOCAL_IPC",
                            onClick = {
                                serverTransport = "LOCAL_IPC"
                                if (serverEndpoint.startsWith("http")) serverEndpoint = "ipc://assistant"
                            },
                            label = { Text("Local IPC") }
                        )
                        FilterChip(
                            selected = serverTransport == "HTTP_SSE",
                            onClick = {
                                serverTransport = "HTTP_SSE"
                                if (serverEndpoint.startsWith("ipc")) serverEndpoint = "http://localhost:8000"
                            },
                            label = { Text("HTTP / SSE") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverAuthToken,
                        onValueChange = { serverAuthToken = it },
                        label = { Text("Auth Bearer Token (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverName.isNotBlank() && serverEndpoint.isNotBlank()) {
                            viewModel.addExternalMcpServer(
                                name = serverName.trim(),
                                endpoint = serverEndpoint.trim(),
                                transport = serverTransport,
                                authToken = serverAuthToken.trim().ifBlank { null }
                            )
                            showAddServerDialog = false
                            Toast.makeText(context, "Server added", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = serverName.isNotBlank() && serverEndpoint.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: Discovered Tools & Call Invocation
    if (inspectingServer != null) {
        AlertDialog(
            onDismissRequest = { inspectingServer = null },
            title = { Text("Discovered Tools: ${inspectingServer?.name}") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isLoadingTools) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (discoveredToolsList.isEmpty()) {
                        Text("No tools discovered on this server or server is disconnected.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        discoveredToolsList.forEach { tool ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tool.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        FilledTonalButton(
                                            onClick = {
                                                selectedToolForExecution = tool
                                                toolParamInput = "{}"
                                                executionResultText = ""
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Invoke", fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tool.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingServer = null }) {
                    Text("Close")
                }
            }
        )
    }

    // DIALOG: Tool Execution Form
    if (selectedToolForExecution != null) {
        val tool = selectedToolForExecution!!
        AlertDialog(
            onDismissRequest = { selectedToolForExecution = null },
            title = { Text("Execute Tool: ${tool.name}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(tool.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Arguments (JSON):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = toolParamInput,
                        onValueChange = { toolParamInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        minLines = 3,
                        maxLines = 5
                    )

                    if (executionResultText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Execution Result:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = executionResultText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showApprovalDialog = true },
                    enabled = !isExecutingExternalTool
                ) {
                    if (isExecutingExternalTool) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Run Tool...")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedToolForExecution = null }) {
                    Text("Close")
                }
            }
        )
    }

    // DIALOG: Mandatory Security Approval Gate
    if (showApprovalDialog && selectedToolForExecution != null && inspectingServer != null) {
        val tool = selectedToolForExecution!!
        val server = inspectingServer!!
        AlertDialog(
            onDismissRequest = { showApprovalDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Approve External Tool Execution?") },
            text = {
                Column {
                    Text(
                        "External server '${server.name}' will execute tool '${tool.name}' with the following payload:",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = toolParamInput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "LifeOS enforces explicit human approval for all external MCP actions to prevent unauthorized prompt injection or data exfiltration.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApprovalDialog = false
                        isExecutingExternalTool = true
                        val argsJson = try { JSONObject(toolParamInput) } catch (_: Exception) { JSONObject() }
                        viewModel.executeExternalMcpTool(
                            server = server,
                            tool = tool,
                            arguments = argsJson,
                            userApproved = true
                        ) { res ->
                            isExecutingExternalTool = false
                            res.onSuccess { result ->
                                executionResultText = if (result.isError) {
                                    "Error: ${result.textOutput}"
                                } else {
                                    result.textOutput
                                }
                            }.onFailure { err ->
                                executionResultText = "Execution failed: ${err.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Approve & Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApprovalDialog = false }) {
                    Text("Cancel (Deny)")
                }
            }
        )
    }

    // DIALOG: Regenerate Token Confirmation
    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text("Regenerate Bearer Token?") },
            text = { Text("Existing AI assistant connections using the old token will be immediately disconnected. A new 32-character hex key will be created.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.regenerateMcpToken()
                        showRegenerateDialog = false
                        Toast.makeText(context, "New MCP token generated", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: Clear Audit Log Confirmation
    if (showClearAuditDialog) {
        AlertDialog(
            onDismissRequest = { showClearAuditDialog = false },
            title = { Text("Clear Audit History?") },
            text = { Text("This will permanently remove all inbound and outbound MCP security records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMcpAuditLogs()
                        showClearAuditDialog = false
                        Toast.makeText(context, "Audit logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAuditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AuditLogRow(log: McpAuditLogEntity) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    val statusColor = when (log.status.uppercase()) {
        "SUCCESS" -> Color(0xFF2E7D32)
        "DENIED" -> MaterialTheme.colorScheme.error
        "FAILED" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (log.direction == "INBOUND") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = log.direction,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.direction == "INBOUND") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(log.eventType, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = log.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.toolOrResource,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (log.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.details,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
