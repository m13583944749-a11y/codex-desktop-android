package com.termux.x11;

import android.app.Application;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;

final class RuntimePingBinding {
    static final String ACTION = "com.ai.codex.debug.RUNTIME_PING";
    static final String PROCESS_NAME = "processName";
    static final String PID = "pid";

    private RuntimePingBinding() {}

    static IBinder bind(Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return null;
        return new IRuntimePing.Stub() {
            @Override public Bundle ping() {
                if (Binder.getCallingUid() != Process.myUid()) {
                    throw new SecurityException("Runtime ping requires the app UID");
                }
                Bundle identity = new Bundle();
                identity.putString(PROCESS_NAME, Application.getProcessName());
                identity.putInt(PID, Process.myPid());
                return identity;
            }
        };
    }
}
