package app.mindpause.fixture;

/** Emulator-only notification sender. It never opens an Activity. */
public final class NotifyReceiver extends android.content.BroadcastReceiver {
    @Override public void onReceive(android.content.Context context,android.content.Intent intent){
        android.app.NotificationManager manager=context.getSystemService(android.app.NotificationManager.class);
        if(intent.getBooleanExtra("clear",false)){manager.cancelAll();return;}
        manager.createNotificationChannel(new android.app.NotificationChannel("timer-heads-up","Timer test",android.app.NotificationManager.IMPORTANCE_HIGH));
        android.app.PendingIntent open=android.app.PendingIntent.getActivity(context,0,new android.content.Intent(context,FeedActivity.class),android.app.PendingIntent.FLAG_IMMUTABLE);
        manager.notify(intent.getIntExtra("id",1),new android.app.Notification.Builder(context,"timer-heads-up")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Timer notification")
            .setContentText("Keep watching the feed. This banner must not restart the timer.")
            .setContentIntent(open).setTimeoutAfter(10000).setAutoCancel(true).build());
    }
}
