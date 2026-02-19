package com.vaultstadio.app.feature.licenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.LicensesScreen

/**
 * Content wrapper for LicensesScreen.
 */
@Composable
fun LicensesContent(
    component: LicensesComponent,
    modifier: Modifier = Modifier,
) {
    LicensesScreen(
        onNavigateBack = component::onBack,
        modifier = modifier,
    )
}
