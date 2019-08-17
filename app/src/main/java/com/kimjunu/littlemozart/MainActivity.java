package com.kimjunu.littlemozart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.kimjunu.littlemozart.ui.main.FileListFragment;
import com.kimjunu.littlemozart.ui.main.SectionsPagerAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @BindView(R.id.fabRecord)
    FloatingActionButton fabRecord;

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.tabLayout)
    TabLayout tabLayout;

    SectionsPagerAdapter sectionsPagerAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(sectionsPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

//        FileListFragment fileListFragment = (FileListFragment) sectionsPagerAdapter.getItem(viewPager.getCurrentItem());
//
//        if (fileListFragment != null) {
//            fileListFragment.setOnRequestFabVisibleListener(new FileListFragment.OnRequestFabVisibleListener() {
//                @Override
//                public void onRequestFabVisible(boolean visible) {
//                    if (visible)
//                        fabRecord.show();
//                    else
//                        fabRecord.hide();
//                }
//            });
//        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                stopAllMediaPlayer();

                FileListFragment fileListFragment = (FileListFragment) getSupportFragmentManager().
                        findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + viewPager.getCurrentItem());
                if (fileListFragment == null)
                    return;

                fileListFragment.setOnRequestFabVisibleListener(null);
                fileListFragment.setOnRequestFabVisibleListener(new FileListFragment.OnRequestFabVisibleListener() {
                    @Override
                    public void onRequestFabVisible(boolean visible) {
                        if (visible)
                            fabRecord.show();
                        else
                            fabRecord.hide();
                    }
                });

                fileListFragment.updateFileList();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @OnClick(R.id.fabRecord)
    public void startRecordingActivity(View view) {
        Intent intent = new Intent(MainActivity.this, RecordingActivity.class);
        startActivity(intent);
    }

    private void stopAllMediaPlayer() {
        FileListFragment midiFragment = (FileListFragment) getSupportFragmentManager().
                findFragmentByTag("android:switcher:" + R.id.viewPager + ":0");
        if (midiFragment != null)
            midiFragment.stopMedia();

        FileListFragment recordFragment = (FileListFragment) getSupportFragmentManager().
                findFragmentByTag("android:switcher:" + R.id.viewPager + ":1");
        if (recordFragment != null)
            recordFragment.stopMedia();
    }
}
