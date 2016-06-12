
package com.example.t_saito.orientationtest;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public abstract class PollingTask {

    private Timer mTimer;

    protected int mInterval;
    private boolean mFirstTimes;
    private boolean mCancel;;

    public PollingTask(int interval) {
        mInterval = interval;
        init();
    }

    protected abstract boolean onPolling();

    protected void onStart() {
    }

    protected void onStop() {
        init();
    }

    private void init() {
        mCancel = false;
        mFirstTimes = true;
    }

    public void start() {
        start(false);
    }

    public void start(boolean force) {
        mFirstTimes = true;
        mCancel = false;

        if (force && isRunning()) {
            stop();
        }

        if (!isRunning()) {
            onStart();
            polling();
        }
    }

    public void stop() {
        mCancel = true;
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        onStop();
    }

    private boolean isRunning() {
        return mTimer != null;
    }

    protected boolean isFirstTimes() {
        return mFirstTimes;
    }

    private void polling() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mCancel || !onPolling() || mCancel) {
                    stop();
                    return;
                }
                mFirstTimes = false;
            }
        }, 0, mInterval);
    }

    public void pollingOnce() {
        new Thread() {
            @Override
            public void run() {
                onPolling();
            }
        }.start();
        // onPolling呼び出し後、スリープしてresume情報を送る時間を作る
        try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			Log.w("TAG", e.getMessage());
		}
    }

}
