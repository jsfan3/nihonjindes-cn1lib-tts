package net.informaticalibera.japanesetts;

import com.codename1.ui.*;
import android.os.Bundle;
import android.content.Intent;
import android.view.KeyEvent;
import com.codename1.system.*;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.system.NativeLookup;
import com.codename1.push.*;
import com.codename1.ui.*;
import android.content.IntentFilter;


public class MyAPPStub extends CodenameOneActivity{
    public String d(String s) {
        return com.codename1.io.Util.xorDecode(s);
    }

    public static final String BUILD_KEY = "null";
    public static final String PACKAGE_NAME = "net.informaticalibera.japanesetts";
    public static final String BUILT_BY_USER = "null";
    public static final String LICENSE_KEY = "null";
    String [] consumable = new String[]{};
    private static MyAPPStub stubInstance;
    private static MyAPP i;
    private boolean running;
    private static boolean firstTime = true;
    private Form currentForm;
    private static final Object LOCK = new Object();
    public static MyAPP getAppInstance() {
        return i;
    }

    protected Object getApp() {
        return i;
    }

    public static MyAPPStub getInstance() {
        return stubInstance;
    }

    public MyAPPStub() {
        stubInstance = this;
    }

    public static boolean isRunning() {
        return stubInstance != null && stubInstance.running;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeLookup.register(net.informaticalibera.japanesetts.JapaneseTTSNative.class, net.informaticalibera.japanesetts.JapaneseTTSNativeStub.class);

Display.getInstance().setProperty("IncludeGPlayServices", "true");
    }
    protected void onResume() {
        running = true;
        super.onResume();
        if (isWaitingForPermissionResult()) {
            setWaitingForPermissionResult(false);
            return;
        }
        if(!Display.isInitialized()) {
AndroidImplementation.startContext(this);
        Display.getInstance().setProperty("build_key", d(BUILD_KEY));
        Display.getInstance().setProperty("package_name", PACKAGE_NAME);
        Display.getInstance().setProperty("built_by_user", d(BUILT_BY_USER));
        Display.getInstance().setProperty("android.NotificationChannel.id", "cn1-channel");
        Display.getInstance().setProperty("android.NotificationChannel.name", "Notifications");
        Display.getInstance().setProperty("android.NotificationChannel.description", "Remote notifications");
        Display.getInstance().setProperty("android.NotificationChannel.importance", "2");
        Display.getInstance().setProperty("android.NotificationChannel.enableLights", "true");
        Display.getInstance().setProperty("android.NotificationChannel.lightColor", "-65536");
        Display.getInstance().setProperty("android.NotificationChannel.enableVibration", "false");
        Display.getInstance().setProperty("android.NotificationChannel.vibrationPattern", null);
        try {
            Display.getInstance().setProperty("android.NotificationChannel.soundUri", android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION).toString());
        } catch (Exception ex){}
                }else{
AndroidImplementation.startContext(this);
        }
        if (i == null) {
          i = new MyAPP();
          if(i instanceof PushCallback) {
                com.codename1.impl.CodenameOneImplementation.setPushCallback((PushCallback)i);
          }
           if (i instanceof com.codename1.push.PushActionsProvider) {
                try{AndroidImplementation.installNotificationActionCategories((com.codename1.push.PushActionsProvider)i);}catch(java.io.IOException ex){ex.printStackTrace();}
           }
        }
        if(i instanceof PushCallback) {
            AndroidImplementation.firePendingPushes((PushCallback)i, this);
        }
        if(i instanceof com.codename1.notifications.LocalNotificationCallback){
            Intent intent = getIntent();
            if(intent != null && intent.getExtras() != null && intent.getExtras().containsKey("LocalNotificationID")){
                String id = intent.getExtras().getString("LocalNotificationID");
                intent.removeExtra("LocalNotificationID");
                ((com.codename1.notifications.LocalNotificationCallback)i).localNotificationReceived(id);
            }
        }
        Display.getInstance().callSerially(new Runnable(){
            boolean wasStopped = (currentForm == null);
            Form currForm = currentForm;
            public void run() {
                Form displayForm = Display.getInstance().getCurrent();
                MyAPPStub.this.run(displayForm == null ? currForm : displayForm, wasStopped);
            }
        });
        synchronized(LOCK) {
            currentForm = null;
        }
    }

    protected void onPause() {
        super.onPause();
        synchronized(LOCK) {
            currentForm = Display.getInstance().getCurrent();
        }
        running = false;
    }

    public void run(Form currentForm, boolean wasStopped) {
        if(firstTime) {
            firstTime = false;
            i.init(this);
         } else {
             synchronized(LOCK) {
                 if(!wasStopped) {
                     if(currentForm instanceof Dialog) {
                         ((Dialog)currentForm).showModeless();
                     }else{
                         currentForm.show();
                     }
                     fireIntentResult();
                     setWaitingForResult(false);
                     return;
                 }
             }
         }
i.start();
    }
protected void onStop() {
        com.codename1.impl.android.AndroidImplementation.writeServiceProperties(this);
        super.onStop();
        if(isWaitingForResult()){
             return;
        }
        synchronized(LOCK) {
             currentForm = null;
        }
        final boolean[] complete = new boolean[1];

        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                i.stop();
                synchronized(complete) {
                    try {
                        complete[0] = true;
                        complete.notify();
                    } catch (Exception ex) {
                    }
                }
            }
        });
        while (!complete[0]) {
            synchronized(complete) {
                try {
                    complete.wait(500);
                } catch (Exception ex){}
            }
        }
        running = false;
    }

protected void onDestroy() {
        super.onDestroy();

        Display.getInstance().callSerially(new Runnable() { public void run() {i.destroy();} });
        AndroidImplementation.stopContext(this);
        running = false;
    } public boolean onKeyDown(int keyCode, KeyEvent event){
 return super.onKeyDown(keyCode, event);
 }
 public String getBase64EncodedPublicKey() {
     return d(LICENSE_KEY);
 }
public boolean isConsumable(String sku) {
  boolean retVal = super.isConsumable(sku);
  java.util.List l = new java.util.ArrayList();
  java.util.Collections.addAll(l, consumable);
  return retVal || l.contains(sku);
}
}
