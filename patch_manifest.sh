sed -i '' '/<\/application>/i\
        <!-- ── Accessibility Service (For Call Recording on Android 10+) ── -->\
        <service\
            android:name=".service.CallRecorderAccessibilityService"\
            android:exported="true"\
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">\
            <intent-filter>\
                <action android:name="android.accessibilityservice.AccessibilityService" />\
            </intent-filter>\
            <meta-data\
                android:name="android.accessibilityservice"\
                android:resource="@xml/accessibility_service_config" />\
        </service>\
' app/src/main/AndroidManifest.xml
