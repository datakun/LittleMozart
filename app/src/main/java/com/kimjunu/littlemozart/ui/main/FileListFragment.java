package com.kimjunu.littlemozart.ui.main;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kimjunu.littlemozart.App;
import com.kimjunu.littlemozart.R;
import com.kimjunu.littlemozart.common.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FileListFragment extends Fragment {

    private static final String ARG_FILE_EXTENSION = "file-extension";

    private String fileExtension = ".mp4";

    @BindView(R.id.rvFileList)
    RecyclerView rvFileList;

    private FileAdapter fileAdapter = null;

    private RecyclerView.LayoutManager layoutManager = null;

    private FileListFragment.OnRequestFabVisibleListener requestFabVisibleListener = null;

    public interface OnRequestFabVisibleListener {
        void onRequestFabVisible(boolean visible);
    }

    public void setOnRequestFabVisibleListener(FileListFragment.OnRequestFabVisibleListener listener) {
        requestFabVisibleListener = listener;
    }

    public static FileListFragment newInstance(String fileExtension) {
        FileListFragment fragment = new FileListFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_FILE_EXTENSION, fileExtension);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_file_list, container, false);

        ButterKnife.bind(this, root);

        Bundle bundle = getArguments();
        fileExtension = bundle.getString(ARG_FILE_EXTENSION, ".mp4");

        // 스토리지 접근 권한 체크
        if (Util.checkPermission(Objects.requireNonNull(getContext()))) {
            initEnvironment();
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Util.checkPermission(Objects.requireNonNull(getContext()))) {
            updateFileList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (fileAdapter != null) {
            fileAdapter.stopMedia();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean isGranted = true;
        for (int grant : grantResults) {
            if (grant != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;

                break;
            }
        }

        if (isGranted) {
            initEnvironment();
        }
    }

    private void initEnvironment() {
        // 파일 경로 설정
        Context context = getContext();
        if (context == null)
            return;

        ApplicationInfo appInfo = getContext().getApplicationInfo();
        if (appInfo == null)
            return;

        String appName = appInfo.loadLabel(context.getPackageManager()).toString();
        App.MediaPath = Environment.getExternalStorageDirectory() + File.separator + appName;

        File directory = new File(App.MediaPath);
        if (!directory.exists())
            directory.mkdirs();

        initListView();
    }

    public void updateFileList() {
        if (fileAdapter != null) {
            File directory = new File(App.MediaPath);

            // 파일 리스트 구성
            ArrayList<File> fileList = new ArrayList<>();
            for (File item : Objects.requireNonNull(directory.listFiles())) {
                if (item.getName().endsWith(fileExtension))
                    fileList.add(item);
            }

            // 내림차순 정렬
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return Long.compare(b.lastModified(), a.lastModified());
                }
            });

            fileAdapter.setFileList(fileList);
            fileAdapter.notifyDataSetChanged();
            fileAdapter.initMediaView();
        }
    }

    private void initListView() {
        if (getContext() == null)
            return;

        // 파일 목록 생성
        rvFileList.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        rvFileList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        rvFileList.setLayoutManager(layoutManager);

        // 파일 확장자에 따른 커스텀 RecyclerView.Adapter 생성
        if (".mp4".equals(fileExtension)) {
            fileAdapter = new RecordingFileAdapter();
        } else if (".mid".equals(fileExtension)) {
            fileAdapter = new MidiFileAdapter();
        } else {
            return;
        }

        fileAdapter.setOnActionDoneListener(new RecordingFileAdapter.OnActionDoneListener() {
            @Override
            public void onRenameDone() {
                updateFileList();
            }

            @Override
            public void onDeleted() {
                updateFileList();
            }
        });
        fileAdapter.setOnItemClickListener(new RecordingFileAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(View view, final int position, boolean isExpanded) {
                int[] outLocation = new int[2];
                view.getLocationOnScreen(outLocation);
                int viewBottom = outLocation[1] + view.getHeight() * 3;

                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                if (requestFabVisibleListener != null) {
//                    fabRecord.getLocationOnScreen(outLocation);
//                    int fabTop = outLocation[1];

                    int fabTop = (size.y * 3) / 4;

                    if (viewBottom >= fabTop && isExpanded) {
                        requestFabVisibleListener.onRequestFabVisible(false);
                    } else {
                        requestFabVisibleListener.onRequestFabVisible(true);
                    }
                }

                // 아이템이 화면을 벗어나면 해당 아이템으로 스크롤
                if (viewBottom >= size.y) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            rvFileList.scrollToPosition(position);
                        }
                    }, 200);
                }
            }
        });
        rvFileList.setAdapter(fileAdapter);

        updateFileList();
    }

    public void stopMedia() {
        if (fileAdapter != null) {
            fileAdapter.stopMedia();

            fileAdapter.collapseMediaView();
        }
    }
}
