package app.mindpause.snapshot;

/** Test-only hierarchy reader. Unlike shell uiautomator dump, does not suppress accessibility services. */
public final class SnapshotInstrumentation extends android.app.Instrumentation {
    private boolean windows;
    @Override public void onCreate(android.os.Bundle args){super.onCreate(args);windows="true".equals(args.getString("windows"));start();}
    @Override public void onStart(){
        android.os.Bundle result=new android.os.Bundle();
        try{
            android.app.UiAutomation ui=getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            if(windows){
                android.accessibilityservice.AccessibilityServiceInfo info=ui.getServiceInfo();
                info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;ui.setServiceInfo(info);
                for(int i=0;ui.getWindows().isEmpty()&&i<10;i++)android.os.SystemClock.sleep(200);
                org.json.JSONArray entries=new org.json.JSONArray();
                for(android.view.accessibility.AccessibilityWindowInfo w:ui.getWindows()){
                    android.graphics.Rect b=new android.graphics.Rect();w.getBoundsInScreen(b);
                    android.view.accessibility.AccessibilityNodeInfo r=w.getRoot();
                    entries.put(new org.json.JSONObject().put("type",w.getType()).put("active",w.isActive()).put("focused",w.isFocused()).put("bounds",b.toShortString()).put("package",r==null?"":String.valueOf(r.getPackageName())));
                    if(r!=null)r.recycle();
                }
                result.putString("windows",entries.toString());finish(-1,result);return;
            }
            android.view.accessibility.AccessibilityNodeInfo root=ui.getRootInActiveWindow();
            for(int i=0;root==null&&i<10;i++){android.os.SystemClock.sleep(200);root=ui.getRootInActiveWindow();}
            if(root==null)throw new IllegalStateException("No active window");
            java.io.StringWriter writer=new java.io.StringWriter();org.xmlpull.v1.XmlSerializer xml=android.util.Xml.newSerializer();xml.setOutput(writer);xml.startTag(null,"hierarchy");node(xml,root);xml.endTag(null,"hierarchy");xml.flush();
            result.putString("xml",writer.toString());finish(-1,result);
        }catch(Exception e){result.putString("error",e.toString());finish(0,result);}
    }
    private void node(org.xmlpull.v1.XmlSerializer xml,android.view.accessibility.AccessibilityNodeInfo n)throws java.io.IOException{
        android.graphics.Rect b=new android.graphics.Rect();n.getBoundsInScreen(b);
        xml.startTag(null,"node");
        xml.attribute(null,"text",n.getText()==null?"":n.getText().toString());xml.attribute(null,"class",n.getClassName()==null?"":n.getClassName().toString());
        xml.attribute(null,"checked",Boolean.toString(n.isChecked()));xml.attribute(null,"enabled",Boolean.toString(n.isEnabled()));
        xml.attribute(null,"bounds","["+b.left+","+b.top+"]["+b.right+","+b.bottom+"]");
        for(int i=0;i<n.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo child=n.getChild(i);if(child!=null){if(child.isVisibleToUser())node(xml,child);child.recycle();}}
        xml.endTag(null,"node");
    }
}
