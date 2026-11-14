package io.github.raghavsatyadev.openplayer.presentation.navidrome.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.raghavsatyadev.openplayer.data.navidrome.model.NavidromeCredentials
import io.github.raghavsatyadev.openplayer.presentation.components.CloudLibraryPickerItem
import io.github.raghavsatyadev.openplayer.presentation.components.CloudLibraryPickerSheet
import io.github.raghavsatyadev.openplayer.ui.theme.RoundedSans
import io.github.raghavsatyadev.openplayer.ui.theme.OpenPlayerTheme
import kotlinx.collections.immutable.toImmutableList
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
class NavidromeLoginActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(io.github.raghavsatyadev.openplayer.utils.AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenPlayerTheme {
                NavidromeLoginScreen(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavidromeLoginScreen(
    viewModel: NavidromeLoginViewModel = koinViewModel(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val loginState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var pendingLocalNetworkLogin by remember {
        mutableStateOf<Triple<String, String, String>?>(null)
    }
    var localNetworkPermissionDenied by remember { mutableStateOf(false) }

    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendingLogin = pendingLocalNetworkLogin
        pendingLocalNetworkLogin = null
        if (granted && pendingLogin != null) {
            viewModel.login(pendingLogin.first, pendingLogin.second, pendingLogin.third)
        } else if (!granted) {
            localNetworkPermissionDenied = true
        }
    }

    LaunchedEffect(localNetworkPermissionDenied) {
        if (localNetworkPermissionDenied) {
            snackbarHostState.showSnackbar(
                getString(Res.string.auth_local_network_permission_denied)
            )
            localNetworkPermissionDenied = false
        }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is NavidromeLoginState.Success -> {
                Toast.makeText(
                    context,
                    getString(Res.string.toast_welcome_user, state.username),
                    Toast.LENGTH_SHORT
                ).show()
                onClose()
            }
            is NavidromeLoginState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    (loginState as? NavidromeLoginState.SelectLibraries)?.let { selectState ->
        CloudLibraryPickerSheet(
            items = remember(selectState.musicFolders) {
                selectState.musicFolders.map { folder ->
                    CloudLibraryPickerItem(
                        id = folder.id,
                        name = folder.name,
                        selectable = true
                    )
                }.toImmutableList()
            },
            onConfirm = { viewModel.confirmLibrarySelection(it) },
            onDismiss = { viewModel.skipLibrarySelection() }
        )
    }

    val isLoading = loginState is NavidromeLoginState.Loading
    val inputShape = AbsoluteSmoothCornerShape(18.dp, 60)
    val connect = {
        focusManager.clearFocus()
        val credentials = NavidromeCredentials(serverUrl, username, password)
        val permissionMissing = Build.VERSION.SDK_INT >= 37 &&
            credentials.requiresLocalNetworkAccess &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED

        if (permissionMissing) {
            pendingLocalNetworkLogin = Triple(serverUrl, username, password)
            localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            viewModel.login(serverUrl, username, password)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.auth_navidrome_title),
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 6.dp),
                        onClick = onClose,
                        enabled = !isLoading,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.rounded_arrow_back_24),
                            contentDescription = stringResource(Res.string.auth_cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(64.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_navidrome),
                            contentDescription = stringResource(Res.string.cd_navidrome_logo),
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                androidx.compose.material3.Surface(
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(64.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_subsonic),
                            contentDescription = stringResource(Res.string.cd_subsonic_logo),
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.auth_navidrome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(Res.string.auth_navidrome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = AbsoluteSmoothCornerShape(20.dp, 60),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.auth_navidrome_info_card),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = RoundedSans
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AbsoluteSmoothCornerShape(28.dp, 60),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.auth_connection_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(Res.string.auth_connection_details_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = RoundedSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    ExpressiveLoginField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = stringResource(Res.string.auth_server_url),
                        placeholder = stringResource(Res.string.auth_navidrome_server_placeholder),
                        supportingText = stringResource(Res.string.auth_navidrome_server_url_hint),
                        leadingIcon = vectorResource(Res.drawable.rounded_cloud_queue_filled_24),
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = inputShape
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ExpressiveLoginField(
                        value = username,
                        onValueChange = { username = it },
                        label = stringResource(Res.string.auth_username),
                        placeholder = stringResource(Res.string.auth_username_placeholder_admin),
                        supportingText = stringResource(Res.string.auth_navidrome_username_hint),
                        leadingIcon = vectorResource(Res.drawable.rounded_person_filled_24),
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = inputShape
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ExpressiveLoginField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(Res.string.auth_password),
                        placeholder = stringResource(Res.string.auth_password_placeholder),
                        supportingText = stringResource(Res.string.auth_navidrome_password_hint),
                        leadingIcon = vectorResource(Res.drawable.rounded_lock_filled_24),
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                    connect()
                                }
                            }
                        ),
                        trailingContent = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) vectorResource(Res.drawable.rounded_visibility_off_filled_24) else vectorResource(Res.drawable.rounded_visibility_filled_24),
                                    contentDescription = stringResource(
                                        if (passwordVisible) Res.string.auth_hide_password else Res.string.auth_show_password
                                    )
                                )
                            }
                        },
                        shape = inputShape
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FilledTonalButton(
                        onClick = {
                            if (serverUrl.isBlank()) {
                                serverUrl = "http://"
                            }
                        },
                        enabled = !isLoading && serverUrl.isBlank(),
                        shape = inputShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.auth_prefill_http),
                            fontFamily = RoundedSans,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = connect,
                enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.auth_connecting), fontFamily = RoundedSans)
                } else {
                    Text(stringResource(Res.string.auth_connect), fontFamily = RoundedSans, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.auth_navidrome_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = RoundedSans
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExpressiveLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingContent: @Composable (() -> Unit)? = null,
    shape: AbsoluteSmoothCornerShape
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = shape,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = RoundedSans
        ),
        label = {
            Text(
                text = label,
                fontFamily = RoundedSans
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = RoundedSans
            )
        },
        supportingText = {
            Text(
                text = supportingText,
                fontFamily = RoundedSans
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = trailingContent,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun Surface(
    shape: androidx.compose.ui.graphics.Shape,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        shape = shape,
        color = color,
        modifier = modifier,
        content = content
    )
}
