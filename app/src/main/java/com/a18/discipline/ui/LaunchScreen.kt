package com.a18.discipline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.a18.discipline.R

@Composable fun LaunchScreen(error: String?,retry: () -> Unit) {
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Surface(shape=MaterialTheme.shapes.extraLarge,color=colorResource(R.color.launcher_background)) {
                Image(painterResource(R.drawable.daymark_logo),"Daymark logo",Modifier.size(160.dp).padding(16.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Daymark",style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Plan with purpose. End the day with progress.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
            Spacer(Modifier.height(36.dp))
            if(error==null) {
                CircularProgressIndicator(Modifier.size(28.dp),strokeWidth=3.dp)
                Spacer(Modifier.height(16.dp))
                Text("Opening your to-do list…",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Your records could not be opened.",style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(error,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error,textAlign=TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick=retry){Text("Try again")}
            }
        }
    }
}
