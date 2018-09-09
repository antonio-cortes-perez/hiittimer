package com.acp.hiittimer;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "HIITTimer";
    private static final long UPDATE_INTERVAL_MS = 200;

    // State variables
    private int mStep;
    private boolean mIsRunning;
    private long mTimeLeft;
    private boolean mIsAlarmActive;
    private final List<Step> mSteps = new ArrayList<>();

    // UI variables
    private TextView mSetText;
    private TextView mExerciseText;
    private TextView mStepText;
    private TextView mTimerText;
    private ProgressBar mProgressBar;
    Button mStartButton;
    Button mStopButton;
    Button mResetButton;

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
        mSetText = findViewById(R.id.set_text);
        mExerciseText = findViewById(R.id.exercise_text);
        mStepText = findViewById(R.id.step_text);
        mTimerText = findViewById(R.id.timer_text);
        mProgressBar = findViewById(R.id.progressBar);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mResetButton = findViewById(R.id.reset_button);

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
        initSteps();
        mStep = 0;
        mIsRunning = false;
        updateUI(mSteps.get(mStep).duration);
        updateButtons(true, false, false);
    }

    private void initSteps() {
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
        mStepText.setText(mSteps.get(mStep).label);
        mTimerText.setText(String.valueOf(Math.round((float) timeLeft / 1000)));
        mProgressBar.setProgress((int) (timeLeft * 100 / mSteps.get(mStep).duration));
    }

    private void updateButtons(boolean startButtonEnabled, boolean
            stopButtonEnabled, boolean resetButtonEnabled) {
        mStartButton.setEnabled(startButtonEnabled);
        mStopButton.setEnabled(stopButtonEnabled);
        mResetButton.setEnabled(resetButtonEnabled);
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
