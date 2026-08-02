sed -i '' '/import android.content.pm.ServiceInfo/i\
import android.content.Context\
import android.os.PowerManager\
' app/src/main/kotlin/com/callrecorder/app/service/CallRecorderService.kt
