package com.kimjunu.littlemozart.ui.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.kimjunu.littlemozart.App;
import com.kimjunu.littlemozart.BuildConfig;
import com.kimjunu.littlemozart.R;

import java.io.File;
import java.util.ArrayList;

public abstract class FileAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    @NonNull
    @Override
    public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(@NonNull VH holder, int position);

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public abstract void playMedia(RecyclerView.ViewHolder viewHolder, String filepath);

    public abstract void stopMedia();

    public abstract void pauseMedia();

    abstract void initMediaView();

    abstract void collapseMediaView();

    String fileExtension = ".mp4";

    ArrayList<File> fileList = null;

    Context mContext = null;

    RecyclerView.ViewHolder currentViewHolder = null;
    ConstraintLayout selectedLayoutMedia = null;

    FileAdapter.OnActionDoneListener actionDoneListener = null;
    FileAdapter.OnItemClickListener itemClickListener = null;

    public interface OnActionDoneListener {
        void onRenameDone();

        void onDeleted();
    }

    public interface OnItemClickListener {
        void onItemClicked(View view, int position, boolean isExpanded);
    }

    public void setOnActionDoneListener(MidiFileAdapter.OnActionDoneListener listener) {
        actionDoneListener = listener;
    }

    public void setOnItemClickListener(MidiFileAdapter.OnItemClickListener listener) {
        itemClickListener = listener;
    }

    public void setFileList(ArrayList<File> fileList) {
        this.fileList = fileList;
    }

    void startActionDialog() {
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
                    case 2:
                        startShareActivity();

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

    void startRenameDialog() {
        if (currentViewHolder == null)
            return;

        int position = currentViewHolder.getAdapterPosition();
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
                    File srcFile = new File(App.MediaPath, basename + fileExtension);
                    File destFile = new File(App.MediaPath, editRename.getText().toString() + fileExtension);

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

    void startDeleteDialog() {
        if (currentViewHolder == null)
            return;

        int position = currentViewHolder.getAdapterPosition();
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

    void startShareActivity() {
        int position = currentViewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION)
            return;

        if (fileList.size() <= position)
            return;

        if (mContext == null)
            return;

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        String mimeType = "audio/*";
        if (".mp4".equals(fileExtension))
            mimeType = "audio/mp4";
        else if (".mid".equals(fileExtension))
            mimeType = "audio/midi";
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID, fileList.get(position)));
        Intent chooser = Intent.createChooser(intent, mContext.getString(R.string.msg_ask_share));

        mContext.startActivity(chooser);
    }
}
