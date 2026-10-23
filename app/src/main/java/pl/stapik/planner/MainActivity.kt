package pl.stapik.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.planner.data.config.ConnectionApiConfigStorage
import pl.stapik.planner.data.theme.DataStoreThemeStorage
import pl.stapik.planner.ui.root.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val apiConfigStorage = remember {
                ConnectionApiConfigStorage(
                    applicationContext
                )
            }
            val transport = remember { OkHttpTransport() }
            val themeStorage = remember {
                DataStoreThemeStorage(
                    applicationContext
                )
            }

            AppRoot(apiConfigStorage = apiConfigStorage, themeStorage = themeStorage, transport = transport)
        }
    }
}
