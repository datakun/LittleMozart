package com.kimjunu.littlemozart;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RecordingActivity extends AppCompatActivity {

    @BindView(R.id.tvTime)
    TextView tvTime;

    @BindView(R.id.btnRecord)
    Button btnRecord;

    private MediaRecorder recorder = null;

    private String filepath = "";

    private Timer timer = null;
    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        ButterKnife.bind(this);

        timer = new Timer();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRecording();
    }

    @OnClick(R.id.btnRecord)
    public void onRecordClicked() {
        if (getString(R.string.record).equals(btnRecord.getText().toString())) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        // 파일 이름 생성, 년월일_시분초.mp4
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String datetimeString = sdf.format(date);
        filepath = App.MediaPath + File.separator + datetimeString + ".mp4";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(filepath);

        // 녹음 시작
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();

        // 시간 표시용 타이머
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long currentTime = System.currentTimeMillis();

                        Date startDate = new Date(startTime);
                        Date currentDate = new Date(currentTime);

                        long diffTime = currentDate.getTime() - startDate.getTime();

                        if (App.RECORD_LIMIT_MILISECOND < diffTime)
                            stopRecording();

                        String time = "00:00:00";
                        if (diffTime != 0) {
                            long min = diffTime / (60 * 1000);
                            long sec = (diffTime % (60 * 1000)) / 1000;
                            long mil = ((diffTime % (60 * 1000)) % 1000) / 10;

                            String minutes = String.format(Locale.ENGLISH, "%02d", min);
                            String seconds = String.format(Locale.ENGLISH, "%02d", sec);
                            String milSeconds = String.format(Locale.ENGLISH, "%02d", mil);

                            time = minutes + ":" + seconds + ":" + milSeconds;
                        }

                        tvTime.setText(time);
                    }
                });
            }
        };

        startTime = System.currentTimeMillis();

        timer.schedule(timerTask, 0, 50);

        btnRecord.setText(R.string.stop);
        btnRecord.setBackgroundResource(android.R.color.darker_gray);
    }

    private boolean stopRecording() {
        btnRecord.setText(R.string.record);
        btnRecord.setBackgroundResource(android.R.color.holo_red_light);

        if (null == recorder)
            return false;

        File file = new File(filepath);

        String msg = getString(R.string.msg_complete_record) + "\n" + file.getName() + " " + getString(R.string.msg_saved);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        timer.cancel();

        // 녹음 종료
        recorder.stop();

        recorder.release();

        recorder = null;

        return true;
    }
}
