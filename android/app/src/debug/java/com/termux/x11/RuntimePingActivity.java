package com.termux.x11;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

public final class RuntimePingActivity extends Activity {
    private static final String TAG = "CodexRuntimePing";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean bound;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            new Thread(() -> readIdentity(binder), "codex-runtime-ping").start();
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            report(false, "Runtime disconnected");
        }

        @Override public void onBindingDied(ComponentName name) {
            report(false, "Runtime binding died");
        }

        @Override public void onNullBinding(ComponentName name) {
            report(false, "Runtime returned a null binding");
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, CodexRuntimeService.class).setAction(RuntimePingBinding.ACTION);
        bound = bindService(intent, connection, BIND_AUTO_CREATE);
        if (bound) {
            handler.postDelayed(() -> report(false, "Timed out waiting for runtime ping"), 5000);
        } else {
            report(false, "Could not bind to runtime");
        }
    }

    private void readIdentity(IBinder binder) {
        try {
            Bundle identity = IRuntimePing.Stub.asInterface(binder).ping();
            String mainName = Application.getProcessName();
            int mainPid = Process.myPid();
            String runtimeName = identity.getString(RuntimePingBinding.PROCESS_NAME);
            int runtimePid = identity.getInt(RuntimePingBinding.PID);
            boolean valid = getPackageName().equals(mainName)
                    && (getPackageName() + ":runtime").equals(runtimeName)
                    && runtimePid > 0 && runtimePid != mainPid;
            String evidence = "main=" + mainName + " pid=" + mainPid
                    + " runtime=" + runtimeName + " pid=" + runtimePid;
            handler.post(() -> report(valid, evidence));
        } catch (RemoteException | RuntimeException error) {
            handler.post(() -> report(false, "Ping failed: " + error));
        }
    }

    private void report(boolean passed, String evidence) {
        if (isFinishing() || isDestroyed()) return;
        Log.println(passed ? Log.INFO : Log.ERROR, TAG, (passed ? "PASS " : "FAIL ") + evidence);
        finish();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (bound) unbindService(connection);
        super.onDestroy();
    }
}
