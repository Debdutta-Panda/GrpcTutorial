package com.debduttapanda.grpctutorial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debduttapanda.grpctutorial.ui.theme.GrpcTutorialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val vm: MainViewModel by viewModels()
        super.onCreate(savedInstanceState)
        setContent {
            GrpcTutorialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Android Kotlin Jetpack Compose GRPC ")
                                }
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ){
                                OutlinedTextField(
                                    enabled = vm.hostEnabled.value,
                                    value = vm.ip.value,
                                    onValueChange = {
                                        vm.onIpChange(it)
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    placeholder = {
                                        Text("IP address")
                                    },
                                    label = {
                                        Text("Server")
                                    }
                                )
                                Spacer(modifier = Modifier.size(16.dp))
                                OutlinedTextField(
                                    enabled = vm.portEnabled.value,
                                    value = vm.port.value,
                                    onValueChange = {
                                        vm.onPortChange(it)
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    placeholder = {
                                        Text("Port")
                                    },
                                    label = {
                                        Text("Port")
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Button(
                                    enabled = vm.startRouteEnabled.value,
                                    onClick = {
                                        vm.startRouteGuide()
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    Text("Start")
                                }
                                Spacer(modifier = Modifier.size(16.dp))
                                Button(
                                    enabled = vm.endRouteEnabled.value,
                                    onClick = {
                                        vm.exitRouteGuide()
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    Text("End")
                                }
                            }
                            Button(
                                enabled = vm.buttonsEnabled.value,
                                onClick = {
                                    vm.getFeature()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Simple RPC: Get Feature")
                            }
                            Button(
                                enabled = vm.buttonsEnabled.value,
                                onClick = {
                                    vm.listFeatures()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Server Streaming: List Features")
                            }
                            Button(
                                enabled = vm.buttonsEnabled.value,
                                onClick = {
                                    vm.recordRoute()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Client Streaming: Record Route")
                            }
                            Button(
                                enabled = vm.buttonsEnabled.value,
                                onClick = {
                                    vm.routeChat()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Bi-directional Streaming: Route Chat")
                            }
                            Text("Result: ${vm.result.value}")
                        }
                    }
                }
            }
        }
    }
}