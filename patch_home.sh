sed -i '' '/import android.Manifest/i\
import android.content.Intent\
import android.provider.Settings\
import androidx.compose.runtime.LaunchedEffect\
import androidx.lifecycle.Lifecycle\
import androidx.lifecycle.compose.LifecycleEventEffect\
import com.callrecorder.app.util.AccessibilityUtil\
' app/src/main/kotlin/com/callrecorder/app/ui/home/HomeScreen.kt
