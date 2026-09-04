package pl.stapik.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import pl.stapik.planner.data.config.DataStoreApiConfigStorage
import pl.stapik.planner.data.theme.DataStoreThemeStorage
import pl.stapik.planner.ui.root.AppRoot
import pl.stapik.planner.ui.theme.RetroColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val apiConfigStorage = remember {
                DataStoreApiConfigStorage(
                    applicationContext
                )
            }
            val themeStorage = remember {
                DataStoreThemeStorage(
                    applicationContext
                )
            }

            Surface(modifier = Modifier.fillMaxSize(), color = RetroColors.WindowBackground) {
                AppRoot(apiConfigStorage = apiConfigStorage, themeStorage = themeStorage)
            }
        }
    }
}
