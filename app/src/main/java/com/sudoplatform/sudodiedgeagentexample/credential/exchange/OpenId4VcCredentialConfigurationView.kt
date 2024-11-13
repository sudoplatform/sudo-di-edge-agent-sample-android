/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.Logo
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcAllowedHolderBindingMethods
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcCredentialConfiguration
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcCredentialConfigurationDisplay
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcCredentialIssuerDisplay
import com.sudoplatform.sudodiedgeagentexample.R
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow

private data class DisplayDetails(
    val name: String = "",
    val description: String = "",
    val bgColor: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified,
    val logoImageUri: String? = null,
)

/**
 * Create a card preview for the [OpenId4VcCredentialConfiguration], using the
 * openid4vc display metadata inside it, and/or using the openid4vc issuer display metadata.
 */
@Composable
fun OpenId4VcCredentialConfigurationView(
    configuration: OpenId4VcCredentialConfiguration,
    issuerDisplay: List<OpenId4VcCredentialIssuerDisplay>,
) {
    /**
     * Given optional display details for the configuration and issuer, try to compute
     * a [DisplayDetails] which acts as the blueprints for how the [Card] should be rendered.
     *
     * If parsing the display details fails (e.g. invalid color encoding), then return default.
     */
    fun tryParseDisplay(
        display: OpenId4VcCredentialConfigurationDisplay?,
        issuerDisplay: OpenId4VcCredentialIssuerDisplay?,
    ): DisplayDetails {
        display ?: return DisplayDetails()
        try {
            val backgroundColor = display.backgroundColor?.let {
                Color(android.graphics.Color.parseColor(it))
            } ?: Color.Unspecified
            val textColor = display.textColor?.let {
                Color(android.graphics.Color.parseColor(it))
            } ?: Color.Unspecified

            return DisplayDetails(
                name = display.name,
                description = display.description ?: "",
                bgColor = backgroundColor,
                textColor = textColor,
                logoImageUri = display.logo?.uri ?: issuerDisplay?.logo?.uri,
            )
        } catch (e: Exception) {
            return DisplayDetails()
        }
    }

    when (configuration) {
        is OpenId4VcCredentialConfiguration.SdJwtVc -> {
            val display = configuration.display?.firstOrNull()
            val displayDetails = tryParseDisplay(display, issuerDisplay.firstOrNull())

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = displayDetails.bgColor),
            ) {
                Row(
                    Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1.0f)) {
                        Text(
                            text = "SD-JWT VC: ${configuration.vct}",
                            fontWeight = FontWeight.Bold,
                            color = displayDetails.textColor,
                        )
                        display?.let {
                            NameValueTextRow(
                                name = "Name",
                                value = it.name,
                                color = displayDetails.textColor,
                            )
                            NameValueTextColumn(
                                name = "Description",
                                value = it.description ?: "",
                                color = displayDetails.textColor,
                            )
                        }

                        NameValueTextColumn(
                            "Claims",
                            configuration.claims.toString(),
                            color = displayDetails.textColor,
                        )
                    }
                    when (val logoUri = displayDetails.logoImageUri) {
                        is String -> {
                            AsyncImage(
                                model = logoUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(40.dp),
                                placeholder = painterResource(id = R.drawable.sp_logo),
                                fallback = painterResource(id = R.drawable.sp_logo),
                                error = painterResource(id = R.drawable.sp_logo),
                            )
                        }

                        null -> {
                            Image(
                                painter = painterResource(id = R.drawable.sp_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(50.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        Column(
            Modifier
                .fillMaxSize(),
        ) {
            OpenId4VcCredentialConfigurationView(
                issuerDisplay = listOf(),
                configuration = OpenId4VcCredentialConfiguration.SdJwtVc(
                    display = listOf(
                        OpenId4VcCredentialConfigurationDisplay(
                            name = "University Degree",
                            locale = null,
                            logo = Logo(
                                "https://w3c-ccg.github.io/vc-ed/plugfest-1-2022/images/JFF_LogoLockup.pn",
                                null,
                            ),
                            description = "A degree from university",
                            backgroundColor = "#12107c",
                            backgroundImage = null,
                            textColor = "#FFFFFF",
                        ),
                    ),
                    vct = "UniversityDegree",
                    claims = mapOf(),
                    allowedBindingMethods = OpenId4VcAllowedHolderBindingMethods(
                        emptyList(),
                        emptyList(),
                    ),
                ),
            )
        }
    }
}
