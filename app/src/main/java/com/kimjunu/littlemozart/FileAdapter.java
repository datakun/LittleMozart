package com.kimjunu.littlemozart;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.kimjunu.littlemozart.common.RestAPIClient;
import com.kimjunu.littlemozart.common.RestAPIInterface;
import com.kimjunu.littlemozart.common.Util;
import com.kimjunu.littlemozart.model.LittleMozartData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileItemViewHolder> {
    private final String TAG = "FileAdapter";

    private ArrayList<File> fileList;

    private MediaPlayer mediaPlayer = null;
    private Timer durationTextChanger = null;

    private RestAPIInterface apiInterface = null;

    private Context mContext = null;

    private FileItemViewHolder currentViewHolder = null;
    private ConstraintLayout selectedLayoutMedia = null;

    private OnActionDoneListener actionDoneListener = null;
    private OnItemClickListener itemClickListener = null;

    interface OnActionDoneListener {
        void onRenameDone();

        void onDeleted();
    }

    interface OnItemClickListener {
        void onItemClicked(View view, int position, boolean isExpanded);
    }

    public void setOnActionDoneListener(OnActionDoneListener listener) {
        actionDoneListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        itemClickListener = listener;
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

                                    Util.decodeBase64ToFile(responseBody.getBinaryData(),
                                            App.MediaPath + File.separator + responseBody.getFilename());

                                    Toast.makeText(mContext, responseBody.getFilename() + mContext.getString(R.string.msg_saved), Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<LittleMozartData> call, Throwable t) {

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

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
            alertDialog.setTitle(R.string.actions);
            alertDialog.setItems(R.array.menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            startRenameDialog();

                            break;
                        case 1:
                            startDeleteDialog();

                            break;
                    }
                }
            });
            alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            alertDialog.show();
        }

        private void startRenameDialog() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (mContext == null)
                    return;

                File file = fileList.get(position);

                final String basename = file.getName().substring(0, file.getName().lastIndexOf("."));
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(basename);
                View viewInflated = LayoutInflater.from(mContext).inflate(R.layout.layout_input_dialog, null);

                final EditText editRename = viewInflated.findViewById(R.id.editRename);
                editRename.setText(basename);
                builder.setView(viewInflated);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 이름 변경
                        File srcFile = new File(App.MediaPath, basename + ".mp4");
                        File destFile = new File(App.MediaPath, editRename.getText().toString() + ".mp4");

                        if (srcFile.renameTo(destFile)) {
                            Toast.makeText(mContext, R.string.msg_success_rename, Toast.LENGTH_SHORT).show();

                            if (actionDoneListener != null)
                                actionDoneListener.onRenameDone();
                        } else {
                            Toast.makeText(mContext, R.string.msg_failed_rename, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.show();
            }
        }

        private void startDeleteDialog() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (mContext == null)
                    return;

                File file = fileList.get(position);
                final String filepath = file.getAbsolutePath();

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(file.getName());
                builder.setMessage(R.string.msg_delete_file);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 파일 삭제
                        File file = new File(filepath);

                        if (file.delete()) {
                            Toast.makeText(mContext, R.string.msg_success_delete, Toast.LENGTH_SHORT).show();

                            if (actionDoneListener != null)
                                actionDoneListener.onDeleted();
                        } else {
                            Toast.makeText(mContext, R.string.msg_failed_delete, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.show();
            }
        }

        private void initMediaView() {
            sbDuration.setProgress(0);
            tvStartTime.setText("00:00");
            ivPlayStop.setTag(R.drawable.ic_play_arrow_black_24dp);
            ivPlayStop.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }
    }

    public FileAdapter(ArrayList<File> fileList) {
        this.fileList = fileList;
    }

    public void setFileList(ArrayList<File> fileList) {
        this.fileList = fileList;
    }

    public void playMedia(FileItemViewHolder viewHolder, String filepath) {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();

        currentViewHolder = viewHolder;

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

                // 시작 전에 SeekBar를 움직였다면 해당 위치부터 시작
                if (currentViewHolder.sbDuration.getProgress() != 0)
                    mediaPlayer.seekTo(currentViewHolder.sbDuration.getProgress());

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
                            long currentTime = mediaPlayer.getCurrentPosition();
                            currentViewHolder.sbDuration.setProgress((int) currentTime);

                            String duration = "00:00";
                            if (currentTime != 0) {
                                long min = currentTime / (60 * 1000);
                                long sec = (currentTime % (60 * 1000)) / 1000;

                                String minutes = String.format(Locale.ENGLISH, "%02d", min);
                                String seconds = String.format(Locale.ENGLISH, "%02d", sec);

                                duration = minutes + ":" + seconds;
                            }
                            currentViewHolder.tvStartTime.setText(duration);
                        }
                    }
                });
            }
        };
        durationTextChanger.schedule(timerTask, 0, 500);
    }

    public void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (currentViewHolder != null)
            currentViewHolder.initMediaView();

        if (durationTextChanger != null)
            durationTextChanger.cancel();
    }

    public void pauseMedia() {
        if (mediaPlayer != null)
            mediaPlayer.pause();

        if (durationTextChanger != null)
            durationTextChanger.cancel();
    }

    @Override
    public FileAdapter.FileItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();

        apiInterface = RestAPIClient.getClient(mContext, App.SERVER_ADDRESS).create(RestAPIInterface.class);

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_file_item, parent, false);
        FileItemViewHolder viewHolder = new FileItemViewHolder(mContext, view);

        return viewHolder;
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

    @Override
    public int getItemCount() {
        return fileList.size();
    }
}
