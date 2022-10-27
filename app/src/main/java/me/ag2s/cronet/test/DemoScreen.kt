package me.ag2s.cronet.test

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.ag2s.cronet.test.ui.theme.CronetTheme


sealed class Screen(val route: String, val title: String) {
    object Editor : Screen("editor", "编辑页面")
    object Request : Screen("request", "请求页")
    object Response : Screen("response", "响应页")
}

val items = listOf(
    Screen.Editor,
    Screen.Request,
    Screen.Response
)


@Composable
fun DemoScreen() {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { DemoTopAppBar(navController) },
        bottomBar = {
            BottomBar(navController = navController)
        }
    ) { innerPadding ->


        val viewModel: TestViewModel = viewModel()


        NavHost(navController, Screen.Editor.route, Modifier.padding(innerPadding)) {
            composable(Screen.Editor.route) {
                Editor(
                    navController = navController,
                    screen = Screen.Editor,
                    viewModel = viewModel
                )
            }
            composable(Screen.Request.route) {
                BenchmarkScreen()
//                Editor(
//                    navController = navController,
//                    screen = Screen.Request,
//                    viewModel = viewModel
//                )
            }
            composable(Screen.Response.route) {
                Editor(
                    navController = navController,
                    screen = Screen.Response,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    navController: NavController,
) {
    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}


@Composable
fun DemoTopAppBar(navController: NavController) {
    val activity = LocalContext.current as Activity
    TopAppBar(
        title = { Text(text = "Demo") },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        activity.finish()
                    }

                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CronetTheme {
        Greeting("Android")
    }
}