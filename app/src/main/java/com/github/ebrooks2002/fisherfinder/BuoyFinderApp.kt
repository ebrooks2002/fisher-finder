/**
 *
 * Instantiates the view model and local context.
 * Calls the HomeScreen composable function.
 *
 * @Author E. Brooks
 */

package com.github.ebrooks2002.fisherfinder

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.ebrooks2002.fisherfinder.viewModel.FisherFinderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel



import com.github.ebrooks2002.fisherfinder.ui.HomeScreen
import androidx.core.graphics.toColorInt

@Composable
fun BuoyFinderApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color("#EFEDE8".toColorInt())
    ) {
        val fisherFinderViewModel: FisherFinderViewModel = viewModel(factory = FisherFinderViewModel.Factory)
        HomeScreen(
            fisherFinderUiState = fisherFinderViewModel.fisherFinderUiState,
            onGetDataClicked = { fisherFinderViewModel.getAssetData() },
            userLocation = fisherFinderViewModel.userLocation,
            onStartLocationUpdates = { fisherFinderViewModel.startLocationTracking() },
            userRotation = fisherFinderViewModel.userRotation,
            userDirection = fisherFinderViewModel.headingDirection,
            onStartRotationUpdates = { fisherFinderViewModel.startRotationTracking() }
        )
    }
}

