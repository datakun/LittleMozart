package com.kimjunu.littlemozart.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.kimjunu.littlemozart.App;
import com.kimjunu.littlemozart.R;
import com.kimjunu.littlemozart.common.RestAPIClient;
import com.kimjunu.littlemozart.common.RestAPIInterface;
import com.kimjunu.littlemozart.common.Util;
import com.kimjunu.littlemozart.model.LittleMozartData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordingFileAdapter extends FileAdapter<RecordingFileAdapter.FileItemViewHolder> {
    private final String TAG = "RecordingFileAdapter";

    private MediaPlayer mediaPlayer = null;
    private Timer durationTextChanger = null;

    private RestAPIInterface apiInterface = null;

    OnRestAPIResponseListener restAPIResponseListener = null;

    public interface OnRestAPIResponseListener {
        void onResponse(LittleMozartData data, boolean isSuccess);

        void onFailure(String errorMessage);
    }

    public void setOnRestAPIResponseListener(OnRestAPIResponseListener listener) {
        restAPIResponseListener = listener;
    }

    public class FileItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tvName)
        TextView tvName;

        @BindView(R.id.tvDate)
        TextView tvDate;

        @BindView(R.id.tvDuration)
        TextView tvDuration;

        @BindView(R.id.layoutMedia)
        ConstraintLayout layoutMedia;

        @BindView(R.id.tvStartTime)
        TextView tvStartTime;

        @BindView(R.id.tvEndTime)
        TextView tvEndTime;

        @BindView(R.id.ivPlayStop)
        ImageView ivPlayStop;

        @BindView(R.id.sbDuration)
        SeekBar sbDuration;

        public FileItemViewHolder(Context context, View view) {
            super(view);

            mContext = context;

            ButterKnife.bind(this, view);

            sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        final long currentTime = progress;
                        Activity activity = (Activity) mContext;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String duration = "00:00";
                                if (currentTime != 0) {
                                    long min = currentTime / (60 * 1000);
                                    long sec = (currentTime % (60 * 1000)) / 1000;

                                    String minutes = String.format(Locale.ENGLISH, "%02d", min);
                                    String seconds = String.format(Locale.ENGLISH, "%02d", sec);

                                    duration = minutes + ":" + seconds;
                                }
                                tvStartTime.setText(duration);
                            }
                        });

                        if (mediaPlayer != null) {
                            if (progress < sbDuration.getMax())
                                mediaPlayer.seekTo(progress);
                            else
                                stopMedia();
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        @OnClick(R.id.layoutItem)
        public void onItemClicked(View view) {
            currentViewHolder = this;

            stopMedia();

            initMediaView();

            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                boolean isExpanded = false;
                if (selectedLayoutMedia != layoutMedia) {
                    layoutMedia.setVisibility(View.VISIBLE);
                    isExpanded = true;

                    if (selectedLayoutMedia != null)
                        selectedLayoutMedia.setVisibility(View.GONE);
                } else if (layoutMedia.getVisibility() == View.GONE) {
                    layoutMedia.setVisibility(View.VISIBLE);
                    isExpanded = true;
                } else {
                    layoutMedia.setVisibility(View.GONE);
                }

                selectedLayoutMedia = layoutMedia;

                if (itemClickListener != null)
                    itemClickListener.onItemClicked(view, position, isExpanded);
            }
        }

        @OnClick(R.id.ivPlayStop)
        public void onPlayStopClicked(View view) {
            if (ivPlayStop.getTag().equals(R.drawable.ic_play_arrow_black_24dp)) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // 재생
                    ivPlayStop.setTag(R.drawable.ic_pause_black_24dp);
                    ivPlayStop.setImageResource(R.drawable.ic_pause_black_24dp);

                    String filepath = fileList.get(position).getAbsolutePath();

                    playMedia(this, filepath);
                }
            } else {
                // 정지
                ivPlayStop.setTag(R.drawable.ic_play_arrow_black_24dp);
                ivPlayStop.setImageResource(R.drawable.ic_play_arrow_black_24dp);

                pauseMedia();
            }
        }

        @OnClick(R.id.ivSend)
        public void onSendClicked(View view) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (mContext == null)
                    return;

                stopMedia();

                initMediaView();

                File file = fileList.get(position);
                final String filepath = file.getAbsolutePath();
                final String filename = file.getName();

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(filename);
                builder.setMessage(R.string.msg_ask_file);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LittleMozartData requestBody = new LittleMozartData(filename, Util.encodeFileToBase64(filepath), 0, "");
                        Call<LittleMozartData> responseCall = apiInterface.callLittleMozart(requestBody);
                        responseCall.enqueue(new Callback<LittleMozartData>() {
                            @Override
                            public void onResponse(Call<LittleMozartData> call, Response<LittleMozartData> response) {
                                try {
                                    if (response == null)
                                        throw new Exception("request failed: no response.");

                                    LittleMozartData responseBody = response.body();

                                    if (responseBody == null) {
                                        ResponseBody errorBody = response.errorBody();
                                        if (errorBody == null)
                                            throw new Exception("request failed: No response body and error, " + response.raw());
                                        else
                                            throw new Exception("request failed: No response body, " + errorBody.string());
                                    }

                                    if (responseBody.getError() != App.ERROR_NONE)
                                        throw new Exception("request failed: " + responseBody.getErrorMessage());

                                    boolean isSuccess = Util.decodeBase64ToFile(responseBody.getBinaryData(),
                                            App.MediaPath + File.separator + responseBody.getFilename());

                                    Toast.makeText(mContext, responseBody.getFilename() + mContext.getString(R.string.msg_saved), Toast.LENGTH_LONG).show();

                                    if (restAPIResponseListener != null)
                                        restAPIResponseListener.onResponse(responseBody, isSuccess);
                                } catch (Exception e) {
                                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();

                                    LittleMozartData data = new LittleMozartData();
                                    data.setErrorMessage(e.getMessage() + "");

                                    if (restAPIResponseListener != null)
                                        restAPIResponseListener.onResponse(data, false);
                                }
                            }

                            @Override
                            public void onFailure(Call<LittleMozartData> call, Throwable t) {
                                Log.e(TAG, Objects.requireNonNull(t.getMessage()));

                                if (restAPIResponseListener != null)
                                    restAPIResponseListener.onFailure(t.getMessage());
                            }
                        });

                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.show();
            }
        }

        @OnClick(R.id.ivMore)
        public void onMoreClicked(View view) {
            stopMedia();

            initMediaView();

            startActionDialog();
        }
    }

    public RecordingFileAdapter() {
        this.fileExtension = ".mp4";
        this.fileList = new ArrayList<>();
    }

    public RecordingFileAdapter(ArrayList<File> fileList) {
        this.fileExtension = ".mp4";
        this.fileList = fileList;
    }

    @Override
    public void playMedia(RecyclerView.ViewHolder viewHolder, String filepath) {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();

        if (!"".equals(filepath)) {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(filepath);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        stopMedia();
                    }
                });

                mediaPlayer.prepare();

                FileItemViewHolder itemViewHolder = (FileItemViewHolder) currentViewHolder;
                if (itemViewHolder != null) {
                    // 시작 전에 SeekBar를 움직였다면 해당 위치부터 시작
                    if (itemViewHolder.sbDuration.getProgress() != 0)
                        mediaPlayer.seekTo(itemViewHolder.sbDuration.getProgress());
                }

                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        durationTextChanger = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Activity activity = (Activity) mContext;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            FileItemViewHolder itemViewHolder = (FileItemViewHolder) currentViewHolder;
                            if (itemViewHolder != null) {
                                long currentTime = mediaPlayer.getCurrentPosition();
                                itemViewHolder.sbDuration.setProgress((int) currentTime);

                                String duration = "00:00";
                                if (currentTime != 0) {
                                    long min = currentTime / (60 * 1000);
                                    long sec = (currentTime % (60 * 1000)) / 1000;

                                    String minutes = String.format(Locale.ENGLISH, "%02d", min);
                                    String seconds = String.format(Locale.ENGLISH, "%02d", sec);

                                    duration = minutes + ":" + seconds;
                                }
                                itemViewHolder.tvStartTime.setText(duration);
                            }
                        }
                    }
                });
            }
        };
        durationTextChanger.schedule(timerTask, 0, 500);
    }

    @Override
    public void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        initMediaView();

        if (durationTextChanger != null)
            durationTextChanger.cancel();
    }

    @Override
    public void pauseMedia() {
        if (mediaPlayer != null)
            mediaPlayer.pause();

        if (durationTextChanger != null)
            durationTextChanger.cancel();
    }

    @Override
    void initMediaView() {
        FileItemViewHolder itemViewHolder = (FileItemViewHolder) currentViewHolder;
        if (itemViewHolder == null)
            return;

        itemViewHolder.sbDuration.setProgress(0);
        itemViewHolder.tvStartTime.setText("00:00");
        itemViewHolder.ivPlayStop.setTag(R.drawable.ic_play_arrow_black_24dp);
        itemViewHolder.ivPlayStop.setImageResource(R.drawable.ic_play_arrow_black_24dp);
    }

    @Override
    void collapseMediaView() {
        initMediaView();

        if (selectedLayoutMedia != null)
            selectedLayoutMedia.setVisibility(View.GONE);
    }

    @NonNull
    @Override
    public FileItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();

        apiInterface = RestAPIClient.getClient(mContext, App.SERVER_ADDRESS).create(RestAPIInterface.class);

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_record_item, parent, false);

        return new FileItemViewHolder(mContext, view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileItemViewHolder holder, int position) {
        File file = fileList.get(position);

        holder.tvName.setText(file.getName());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String date = simpleDateFormat.format(file.lastModified());
        holder.tvDate.setText(date);

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(file.getAbsolutePath());

        String metaDuration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        long durationTime = Long.parseLong(metaDuration);

        String duration = "00:00";
        if (durationTime != 0) {
            long min = durationTime / (60 * 1000);
            long sec = (durationTime % (60 * 1000)) / 1000;

            String minutes = String.format(Locale.ENGLISH, "%02d", min);
            String seconds = String.format(Locale.ENGLISH, "%02d", sec);

            duration = minutes + ":" + seconds;
        }
        holder.tvDuration.setText(duration);

        holder.tvEndTime.setText(duration);

        holder.sbDuration.setMax((int) durationTime);

        holder.ivPlayStop.setTag(R.drawable.ic_play_arrow_black_24dp);
    }
}
