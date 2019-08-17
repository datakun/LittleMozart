package com.kimjunu.littlemozart;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @BindView(R.id.fabRecord)
    FloatingActionButton fabRecord;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.rvFileList)
    RecyclerView rvFileList;

    private FileAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        // 스토리지 접근 권한 체크
        if (checkPermission()) {
            initEnvironment();
        }
    }

    @OnClick(R.id.fabRecord)
    public void startRecordingActivity(View view) {
        Intent intent = new Intent(MainActivity.this, RecordingActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initEnvironment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkPermission()) {
            updateFileList();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (adapter != null)
            adapter.stopMedia();
    }

    private void initEnvironment() {
        // 녹음 파일 경로 설정
        //App.MediaPath = Environment.getExternalStorageDirectory() + File.separator + "Records";
        App.MediaPath = Environment.getExternalStorageDirectory() + File.separator + "Download";

        File directory = new File(App.MediaPath);
        if (!directory.exists())
            directory.mkdirs();

        // 녹음 파일 목록 생성
        rvFileList.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        rvFileList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvFileList.setLayoutManager(layoutManager);

        ArrayList<File> fileList = new ArrayList<>();

        adapter = new FileAdapter(fileList);
        adapter.setOnActionDoneListener(new FileAdapter.OnActionDoneListener() {
            @Override
            public void onRenameDone() {
                updateFileList();
            }

            @Override
            public void onDeleted() {
                updateFileList();
            }
        });
        adapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(View view, final int position, boolean isExpanded) {
                int[] outLocation = new int[2];
                view.getLocationOnScreen(outLocation);
                int viewBottom = outLocation[1] + view.getHeight() * 3;

                fabRecord.getLocationOnScreen(outLocation);
                int fabTop = outLocation[1];

                if (viewBottom >= fabTop && isExpanded) {
                    fabRecord.hide();
                } else {
                    fabRecord.show();
                }

                // 아이템이 화면을 벗어나면 해당 아이템으로 스크롤
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
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
        rvFileList.setAdapter(adapter);

        updateFileList();
    }

    private void updateFileList() {
        File directory = new File(App.MediaPath);

        // 파일 리스트 구성
        ArrayList<File> fileList = new ArrayList<>();
        for (File item : Objects.requireNonNull(directory.listFiles())) {
            if (item.getName().endsWith(".mp4"))
                fileList.add(item);
        }

        // 내림차순 정렬
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });

        adapter.setFileList(fileList);
        adapter.notifyDataSetChanged();
    }

    private boolean checkPermission() {
        ArrayList<String> permissionList = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.RECORD_AUDIO);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!permissionList.isEmpty()) {
            String[] permissionArray = new String[permissionList.size()];
            permissionArray = permissionList.toArray(permissionArray);

            requestPermissions(permissionArray, 1000);

            return false;
        }

        return true;
    }
}
