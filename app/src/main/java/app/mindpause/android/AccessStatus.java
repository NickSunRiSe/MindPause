package app.mindpause.android;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.*;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import app.mindpause.android.core.AccessState;

final class AccessStatus {
    static AccessState read(Context context) {
        ComponentName expected=new ComponentName(context,FocusService.class);
        boolean managerEnabled=false, settingsEnabled=false;
        try {
            AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            for(AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                android.content.pm.ServiceInfo service=info.getResolveInfo().serviceInfo;
                if(expected.equals(new ComponentName(service.packageName,service.name)))managerEnabled=true;
            }
        } catch(RuntimeException ignored) { /* OEM API failure is not proof that access is off. */ }
        try {
            String value=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if(value!=null)for(String entry:value.split(":"))if(expected.equals(ComponentName.unflattenFromString(entry)))settingsEnabled=true;
        } catch(RuntimeException ignored) {}
        return AccessState.resolve(FocusService.connected,managerEnabled,settingsEnabled);
    }
}
