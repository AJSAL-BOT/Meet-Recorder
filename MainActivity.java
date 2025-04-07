package com.example.screenrecorder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1000;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private String videoPath;
    private long startTime;
    private Handler handler = new Handler();
    private boolean isRecording = false;

    private EditText meetCode;
    private Button startButton, stopButton;
    private TextView timerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        meetCode = findViewById(R.id.meetCode);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        timerText = findViewById(R.id.timerText);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        startButton.setOnClickListener(v -> startRecording());
        stopButton.setOnClickListener(v -> stopRecording());
    }

    private void startRecording() {
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            setupMediaRecorder();
            mediaRecorder.start();
            isRecording = true;
            startTime = SystemClock.elapsedRealtime();
            handler.post(updateTimer);
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        }
    }

    private void setupMediaRecorder() {
        videoPath = getExternalFilesDir(null).getAbsolutePath() + "/meet_record.mp4";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoPath);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaProjection.stop();
            handler.removeCallbacks(updateTimer);
            isRecording = false;
            stopButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
            addWatermark(videoPath);
        }
    }

    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            long elapsedMillis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (elapsedMillis / 1000) % 60;
            int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
            int hours = (int) ((elapsedMillis / (1000 * 60 * 60)) % 24);
            timerText.setText(String.format("Recording Time: %02d:%02d:%02d", hours, minutes, seconds));
            handler.postDelayed(this, 1000);
        }
    };

    private void addWatermark(String videoPath) {
        String outputPath = getExternalFilesDir(null).getAbsolutePath() + "/meet_record_watermarked.mp4";
        String watermarkText = "CYBER AI";

        String command = "-i " + videoPath + " -vf \"drawtext=text='" + watermarkText + "':fontsize=30:x=10:y=10:fontcolor=white\" " + outputPath;

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
