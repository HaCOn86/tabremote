package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.BackgroundPitch
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueBluetoothOnly: () -> Unit = {},
    onContinueGuest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isAutoChecking by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                isAutoChecking = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isAutoChecking = false
            }
        )
    }

    val logoScale by animateFloatAsState(
        targetValue = if (isLoading) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "auth_logo_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0308),
                        BackgroundPitch,
                        BackgroundPitch
                    )
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(76.dp)
                    .background(RubyPrimary.copy(alpha = 0.16f), CircleShape)
                    .border(1.5.dp, RubyPrimary.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "TabRemote",
                    tint = RubyPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TabRemote",
                    color = TextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Bluetooth Touchpad HID veya Google hesabıyla Wi-Fi Alıcı tablet kontrolü. Kullanım şeklinizi seçin:",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }

            Surface(
                color = SurfaceCharcoal,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.DevicesOther,
                        title = "Google ile Otomatik Cihaz Bulma",
                        subtitle = "Aynı hesaptaki telefon ve tabletler otomatik eşleşir, Wi-Fi Alıcı IP'si senkronlanır."
                    )
                    FeatureRow(
                        icon = Icons.Default.Bluetooth,
                        title = "Sadece Bluetooth Touchpad (Hesapsız)",
                        subtitle = "Google hesabı veya Wi-Fi açmadan doğrudan Bluetooth HID Touchpad & Klavye bağlayın."
                    )
                    FeatureRow(
                        icon = Icons.Default.Wifi,
                        title = "İstediğiniz Zaman Hesap Bağlayın",
                        subtitle = "Hesapsız veya Bluetooth modunda başlasanız bile Ayarlar'dan Google hesabınızı bağlayabilirsiniz."
                    )
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = RedDanger.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RedDanger.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = RedDanger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading && !isAutoChecking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RubyPrimary,
                    disabledContainerColor = SurfaceContainerCharcoal
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading || isAutoChecking) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.4.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isAutoChecking) "Oturum Kontrol Ediliyor..." else "Google ile Bağlanılıyor...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Google ile Giriş Yap",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Secondary Entry Options: Bluetooth-Only & Guest Mode
            Surface(
                color = SurfaceCharcoal,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HESAP VEYA WI-FI KULLANMAK İSTEMİYORSANIZ",
                        color = RubyPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Google oturumu açmadan doğrudan Bluetooth HID kumanda olarak kullanabilir veya çevrimdışı devam edebilirsiniz.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    OutlinedButton(
                        onClick = onContinueBluetoothOnly,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, RubyPrimary.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("bluetooth_only_continue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Sadece Bluetooth Kullan",
                            tint = RubyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sadece Bluetooth Kullan",
                            color = RubyPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onContinueGuest,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderRuby),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guest_continue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOff,
                            contentDescription = "Giriş Yapmadan Devam Et",
                            tint = TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Giriş Yapmadan Devam Et",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = "Bulut eşlemesi için yalnızca aynı Google hesabındaki cihazlarınız listelenir.",
                color = TextMuted,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(RubyAccent.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = RubyPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextSecondary, fontSize = 10.5.sp, lineHeight = 14.sp)
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: NoCredentialException) {
            onUnauthenticated()
        } catch (_: GetCredentialException) {
            onUnauthenticated()
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Giriş yapılandırması bulunamadı (default_web_client_id).")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
            if (activity == null) {
                onAuthError("Oturum penceresi açılamadı.")
                return@launch
            }
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Beklenmeyen kimlik bilgisi türü alındı.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google credentials available on device: ${e.message}")
            onAuthError("Cihazda kayıtlı uygun Google hesabı bulunamadı veya geçici olarak ulaşılamıyor.")
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Google Credential error", e)
            onAuthError(e.localizedMessage ?: "Google girişi tamamlanamadı.")
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google girişi başarısız oldu.")
        }
    }
}

fun signOutFromGoogle(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
