package com.kimjunu.littlemozart.ui.main;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.kimjunu.littlemozart.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MidiFileAdapter extends FileAdapter<MidiFileAdapter.FileItemViewHolder> {
    private final String TAG = "MidiFileAdapter";

    private MediaPlayer mediaPlayer = null;

    public class FileItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tvName)
        TextView tvName;

        @BindView(R.id.tvDate)
        TextView tvDate;

        @BindView(R.id.layoutMedia)
        ConstraintLayout layoutMedia;

        @BindView(R.id.ivPlayStop)
        ImageView ivPlayStop;

        public FileItemViewHolder(Context context, View view) {
            super(view);

            mContext = context;

            ButterKnife.bind(this, view);
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
                    ivPlayStop.setTag(R.drawable.ic_stop_black_24dp);
                    ivPlayStop.setImageResource(R.drawable.ic_stop_black_24dp);

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

        @OnClick(R.id.ivMore)
        public void onMoreClicked(View view) {
            stopMedia();

            initMediaView();

            startActionDialog();
        }
    }

    public MidiFileAdapter() {
        this(new ArrayList<File>());
    }

    public MidiFileAdapter(ArrayList<File> fileList) {
        this.fileExtension = ".mid";
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

                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        initMediaView();
    }

    @Override
    public void pauseMedia() {
        if (mediaPlayer != null)
            mediaPlayer.pause();
    }

    @Override
    void initMediaView() {
        FileItemViewHolder itemViewHolder = (FileItemViewHolder) currentViewHolder;
        if (itemViewHolder == null)
            return;

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

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_midi_item, parent, false);

        return new FileItemViewHolder(mContext, view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileItemViewHolder holder, int position) {
        File file = fileList.get(position);

        holder.tvName.setText(file.getName());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String date = simpleDateFormat.format(file.lastModified());
        holder.tvDate.setText(date);

        holder.ivPlayStop.setTag(R.drawable.ic_play_arrow_black_24dp);
    }
}
