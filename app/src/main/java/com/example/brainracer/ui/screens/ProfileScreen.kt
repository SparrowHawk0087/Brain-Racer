package com.example.brainracer.ui.screens

import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.Image
import com.example.brainracer.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.viewmodels.AuthViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.ui.text.style.TextAlign


@Composable
fun GreetingImage(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .offset(x = -10.dp, y = 80.dp)
    ) {
            Image(
                painter = painterResource(R.drawable.startprofile),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )

    }
}

@Composable
fun MyProgressBarScreen() {
    var progress by remember { mutableFloatStateOf(0.3f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500)
    )

    Column(
        modifier = Modifier
            .offset(x = 0.dp, y = 280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Прогресс: ${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )


        Slider(
            value = progress,
            onValueChange = { progress = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )


        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                progress = (progress - 0.1f).coerceAtLeast(0f)
            }) {
                Text("-10%")
            }

            Button(onClick = {
                progress = (progress + 0.1f).coerceAtMost(1f)
            }) {
                Text("+10%")
            }
        }
    }
}






//@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    userId: String
) {
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current

        //Если пользователь удален или вышел
    LaunchedEffect(user) {
        if (user == null) {
            Toast.makeText(context, "Session expired or account deleted", Toast.LENGTH_SHORT).show()
            onNavigateToAuth()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile: $userId")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            authViewModel.deleteAccount()
        }) {
            Text("Delete Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            authViewModel.signOut()
        }) {
            Text("Sign Out")
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview( ) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.5f))


        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Edit Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "My stats",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Friend list",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Game history",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }


        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                //authViewModel.signOut()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                "Sign Out",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                //authViewModel.deleteAccount()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Red
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Delete Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(0.02f))

    }

    Text(
        text = "Rank:",
        modifier = Modifier.offset(x = 30.dp, y = 230.dp),
        fontSize = 24.sp,
    )


    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Nick Name", fontSize = 20.sp) },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .offset(x = 160.dp, y = 100.dp)
            .fillMaxWidth(0.55f)
            .height(56.dp)
    )

    Text(
        text = "Your email",
        fontSize = 20.sp,
        modifier = Modifier.offset(x = 180.dp, y = 180.dp)
    )


    MyProgressBarScreen()

    GreetingImage()

    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { },
        modifier = Modifier
            .offset(x = 90.dp, y = 180.dp)
            .fillMaxWidth(0.12f)
    ) {
        Text(
            text = "✏\uFE0F",
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}