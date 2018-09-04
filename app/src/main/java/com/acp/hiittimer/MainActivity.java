package com.acp.hiittimer;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int EXERCISE_STEP_WARMUP = 0;
    private static final int EXERCISE_STEP_HIGH = 1;
    private static final int EXERCISE_STEP_LOW = 2;
    private static final int EXERCISE_STEP_DONE = 3;
    private static final String TAG = "HIITTimer";
    private static final long UPDATE_INTERVAL_MS = 200;

    // State variables
    private int mSetCounter;
    private int mExerciseCounter;
    private int mStep;
    private boolean mIsRunning;
    private long mTime;
    private long mTimeLeft;
    private boolean mIsAlarmActive;

    // UI variables
    private TextView mSetText;
    private TextView mExerciseText;
    private TextView mStepText;
    private TextView mTimerText;
    private ProgressBar mProgressBar;
    Button mStartButton;
    Button mStopButton;
    Button mResetButton;

    // Preferences
    private final long numSets = 4;
    private final long numExercises = 7;
    private static final long warmupTime = 10000;
    private static final long highTime = 45000;
    private static final long lowTime = 15000;

    private ExerciseTimer mTimer;
    private MediaPlayer mMediaPlayer;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            createTimer();
        }
    };

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
            mSetCounter = savedInstanceState.getInt("mSetCounter");
            mExerciseCounter = savedInstanceState.getInt("mExerciseCounter");
            mStep = savedInstanceState.getInt("mStep");
            mIsRunning = savedInstanceState.getBoolean("mIsRunning");
            mTime = savedInstanceState.getLong("mTime");
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
        outState.putInt("mSetCounter", mSetCounter);
        outState.putInt("mExerciseCounter", mExerciseCounter);
        outState.putInt("mStep", mStep);
        outState.putBoolean("mIsRunning", mIsRunning);
        outState.putLong("mTime", mTime);
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
        sb.append("mSetCounter=");
        sb.append(mSetCounter);
        sb.append("; mExerciseCounter=");
        sb.append(mExerciseCounter);
        sb.append("; mStep=");
        sb.append(mStep);
        sb.append("; mIsRunning=");
        sb.append(mIsRunning);
        sb.append("; mTime=");
        sb.append(mTime);
        sb.append("; mTimeLeft=");
        sb.append(mTimeLeft);
        return sb.toString();
    }

    private void initSession() {
        mSetCounter = 0;
        initExercise();
        mIsRunning = false;
        updateUI(warmupTime);
        updateButtons(true, false, false);
    }

    private void initExercise() {
        mExerciseCounter = 0;
        mStep = EXERCISE_STEP_WARMUP;
        mTime = warmupTime;
        mTimeLeft = warmupTime;
    }

    private void createTimer() {
        Log.d(TAG, toString());
        if (mStep == EXERCISE_STEP_DONE) {
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
        switch (mStep) {
            case EXERCISE_STEP_WARMUP:
                mStep = EXERCISE_STEP_HIGH;
                mTime = highTime;
                mTimeLeft = mTime;
                break;
            case EXERCISE_STEP_HIGH:
                mStep = EXERCISE_STEP_LOW;
                mTime = lowTime;
                mTimeLeft = mTime;
                break;
            case EXERCISE_STEP_LOW:
                mStep = EXERCISE_STEP_HIGH;
                mTime = highTime;
                mTimeLeft = mTime;
                mExerciseCounter++;
                if (mExerciseCounter == numExercises) {
                    mSetCounter++;
                    if (mSetCounter == numSets) {
                        // decrement them to print them correctly in the UI.
                        mExerciseCounter--;
                        mSetCounter--;
                        mStep = EXERCISE_STEP_DONE;
                    } else {
                        initExercise();
                    }
                }
                break;
        }
    }

    private void updateUI(long timeLeft) {
        mTimeLeft = timeLeft;
        mSetText.setText(
                String.format(getString(R.string.set_format),
                        String.valueOf(mSetCounter + 1),
                        String.valueOf(numSets)));
        mExerciseText.setText(
                String.format(getString(R.string.exercise_format),
                        String.valueOf(mExerciseCounter + 1),
                        String.valueOf(numExercises)));
        switch (mStep) {
            case EXERCISE_STEP_WARMUP:
                mStepText.setText("WARMUP");
                break;
            case EXERCISE_STEP_HIGH:
                mStepText.setText("HIGH");
                break;
            case EXERCISE_STEP_LOW:
                mStepText.setText("LOW");
                break;
            case EXERCISE_STEP_DONE:
                mStepText.setText("DONE");
                break;
        }
        mTimerText.setText(String.valueOf(Math.round((float) timeLeft / 1000)));
        mProgressBar.setProgress((int) (timeLeft * 100 / mTime));
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
