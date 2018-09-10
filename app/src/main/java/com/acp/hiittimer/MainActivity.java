package com.acp.hiittimer;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "HIITTimer";
    private static final long UPDATE_INTERVAL_MS = 200;
    private static final int ID_FIRST_ROW = 12345;

    // State variables
    private int mStep;
    private boolean mIsRunning;
    private long mTimeLeft;
    private boolean mIsAlarmActive;
    private final List<Step> mSteps = new ArrayList<>();

    // UI variables
    Button mStartButton;
    Button mStopButton;
    Button mResetButton;
    private ProgressBar mProgressBar;
    private TableLayout mTableSteps;

    private ExerciseTimer mTimer;
    private MediaPlayer mMediaPlayer;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            createTimer();
        }
    };

    private static class Step {
        long duration;
        String label;

        Step(long duration, String label) {
            this.duration = duration;
            this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mResetButton = findViewById(R.id.reset_button);
        mProgressBar = findViewById(R.id.progressBar);
        mTableSteps = findViewById(R.id.table_steps);

        mStartButton.setOnClickListener(view -> {
            mIsRunning = true;
            Message m = mHandler.obtainMessage();
            mHandler.sendMessage(m);
            updateButtons(false, true, false);
        });
        mStopButton.setOnClickListener(view -> {
            mIsRunning = false;
            mTimer.cancel();
            mTimer = null;
            updateButtons(true, false, true);
        });
        mResetButton.setOnClickListener(view -> {
            initSession();
            updateButtons(true, false, false);
        });

        mMediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        mMediaPlayer.setOnCompletionListener(mediaPlayer ->
                mIsAlarmActive = false
        );

        if (savedInstanceState != null) {
            mStep = savedInstanceState.getInt("mStep");
            mIsRunning = savedInstanceState.getBoolean("mIsRunning");
            mTimeLeft = savedInstanceState.getLong("mTimeLeft");
            mStartButton.setEnabled(savedInstanceState.getBoolean
                    ("startButtonEnabled"));
            mStopButton.setEnabled(savedInstanceState.getBoolean
                    ("stopButtonEnabled"));
            mResetButton.setEnabled(savedInstanceState.getBoolean
                    ("resetButtonEnabled"));
            initSteps();
            if (mIsRunning) {
                Log.d(TAG, "OnCreate IsRunning");
                createTimer();
            } else {
                updateUI(mTimeLeft);
            }
        } else {
            initSession();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "saving state");
        super.onSaveInstanceState(outState);
        outState.putInt("mStep", mStep);
        outState.putBoolean("mIsRunning", mIsRunning);
        outState.putLong("mTimeLeft", mTimeLeft);
        outState.putBoolean("startButtonEnabled", mStartButton.isEnabled());
        outState.putBoolean("stopButtonEnabled", mStopButton.isEnabled());
        outState.putBoolean("resetButtonEnabled", mResetButton.isEnabled());
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "OnDestroy");
        super.onDestroy();
        if (mTimer != null) {
            mTimer.cancel();
        }
        mMediaPlayer.release();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("; mStep=");
        sb.append(mStep);
        sb.append("; mIsRunning=");
        sb.append(mIsRunning);
        sb.append("; mTimeLeft=");
        sb.append(mTimeLeft);
        return sb.toString();
    }

    private void initSession() {
        mStep = 0;
        mIsRunning = false;
        initSteps();
        updateUI(mSteps.get(mStep).duration);
        updateButtons(true, false, false);
    }

    private void initSteps() {
        mSteps.clear();
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mSteps.add(new Step(15000, "Low"));
        mSteps.add(new Step(45000, "High"));
        mTableSteps.removeAllViews();
        int i = 0;
        for (Step step : mSteps) {
            // Compose objects.
            TableRow tr = new TableRow(this);
            TextView time = new TextView(this);
            TextView label = new TextView(this);
            tr.addView(time);
            tr.addView(label);
            mTableSteps.addView(tr);
            // Setup properties.
            tr.setPadding(0, 5, 0, 5);
            tr.setBackgroundColor(i % 2 == 0 ? Color.WHITE : Color.LTGRAY);
            tr.setId(ID_FIRST_ROW + i++);
            time.setText(
                    String.valueOf(Math.round((float) step.duration / 1000)));
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
            time.setWidth(Math.round(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics())));
            time.setBackgroundColor(Color.TRANSPARENT);
            label.setText(step.label);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
            label.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void createTimer() {
        Log.d(TAG, toString());
        if (mStep >= mSteps.size()) {
            mTimer = null;
            updateUI(0);
            updateButtons(false, false, true);
        } else {
            mTimer = new ExerciseTimer(mTimeLeft, UPDATE_INTERVAL_MS);
            updateUI(mTimeLeft);
            mTimer.start();
        }
    }

    private void setNextStep() {
        mStep++;
        if (mStep < mSteps.size()) {
            mTimeLeft = mSteps.get(mStep).duration;
        }
    }

    private void updateUI(long timeLeft) {
        mTimeLeft = timeLeft;
        if (mStep > 0) {
            int prevRowIndex = mStep - 1;
            findViewById(ID_FIRST_ROW + prevRowIndex).setBackgroundColor(getRowColor(prevRowIndex));
        }
        if (mStep < mSteps.size()) {
            mProgressBar.setProgress((int) (timeLeft * 100 / mSteps.get(mStep).duration));
            findViewById(ID_FIRST_ROW + mStep).setBackgroundColor(Color.GREEN);
        } else {
            mProgressBar.setProgress(0);
        }
    }

    private void updateButtons(boolean startButtonEnabled, boolean
            stopButtonEnabled, boolean resetButtonEnabled) {
        mStartButton.setEnabled(startButtonEnabled);
        mStopButton.setEnabled(stopButtonEnabled);
        mResetButton.setEnabled(resetButtonEnabled);
    }

    private int getRowColor(int row) {
        return row % 2 == 0 ? Color.WHITE : Color.LTGRAY;
    }

    private class ExerciseTimer extends CountDownTimer {

        public ExerciseTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            updateUI(l);
            if (l > 4000 && l < 4500 && !mIsAlarmActive) {
                mIsAlarmActive = true;
                mMediaPlayer.start();
            }
        }

        @Override
        public void onFinish() {
            setNextStep();
            Message m = mHandler.obtainMessage();
            mHandler.sendMessage(m);
        }
    }
}
