package com.landoulsi.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.landoulsi.demo.ui.DesignComponentsDemoScreen
import com.landoulsi.demo.ui.DiagnosticDemoScreen
import com.landoulsi.demo.ui.IntegrityDemoScreen
import com.landoulsi.demo.ui.PaymentDemoScreen
import com.landoulsi.demo.ui.SurveyDemoScreen
import com.landoulsi.demo.ui.UpdateDemoScreen
import com.landoulsi.demo.ui.ViewModelDemoScreen
import com.landoulsi.design.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DemoAppNavHost()
                }
            }
        }
    }
}

@Composable
fun DemoAppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("payment") {
            PaymentDemoScreen(onBack = { navController.popBackStack() })
        }
        composable("update") {
            UpdateDemoScreen(onBack = { navController.popBackStack() })
        }
        composable("design") {
            DesignComponentsDemoScreen(onBack = { navController.popBackStack() })
        }
        composable("survey") {
            SurveyDemoScreen(onBack = { navController.popBackStack() })
        }
        composable("diagnostic") {
            DiagnosticDemoScreen(onBack = { navController.popBackStack() })
        }
        composable("viewmodel") {
            ViewModelDemoScreen(onBack = { navController.popBackStack() })
        }
        composable("integrity") {
            IntegrityDemoScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Global SDK Demo",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("payment") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Payment SDK Demo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("update") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Update SDK Demo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("design") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Design Components")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("survey") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Survey SDK Demo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("diagnostic") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Diagnostic SDK Demo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("viewmodel") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("ViewModel SDK Demo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("integrity") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Integrity SDK Demo")
        }
    }
}
