package com.posepilot.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.posepilot.app.ui.camera.CameraScreen
import com.posepilot.app.ui.camera.CameraViewModel
import com.posepilot.app.ui.gallery.GalleryScreen
import com.posepilot.app.ui.gallery.GalleryViewModel
import com.posepilot.app.ui.onboarding.PermissionScreen
import com.posepilot.app.ui.theme.PosePilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()
    private val galleryViewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PosePilotTheme {
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var isPermissionDeniedPermanently by remember {
                    mutableStateOf(false)
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                    if (!isGranted) {
                        isPermissionDeniedPermanently = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    }
                }

                DisposableEffect(Unit) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            hasCameraPermission = granted
                            if (granted) {
                                isPermissionDeniedPermanently = false
                            }
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (hasCameraPermission) {
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = "camera",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("camera") {
                                CameraScreen(
                                    viewModel = cameraViewModel,
                                    onNavigateToGallery = {
                                        navController.navigate("gallery")
                                    }
                                )
                            }
                            composable("gallery") {
                                GalleryScreen(
                                    viewModel = galleryViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    } else {
                        PermissionScreen(
                            isPermissionDeniedPermanently = isPermissionDeniedPermanently,
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                )
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}
