package com.duan.musicoco.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.duan.musicoco.R;
import com.duan.musicoco.aidl.IPlayControl;
import com.duan.musicoco.aidl.Song;
import com.duan.musicoco.app.ExceptionHandler;
import com.duan.musicoco.app.MediaManager;
import com.duan.musicoco.app.interfaces.OnThemeChange;
import com.duan.musicoco.app.interfaces.OnPlayListVisibilityChange;
import com.duan.musicoco.app.SongInfo;
import com.duan.musicoco.app.interfaces.UserInterfaceUpdate;
import com.duan.musicoco.image.BitmapBuilder;
import com.duan.musicoco.play.PlayActivity;
import com.duan.musicoco.play.PlayListAdapter;
import com.duan.musicoco.preference.AppPreference;
import com.duan.musicoco.preference.Theme;
import com.duan.musicoco.service.PlayController;
import com.duan.musicoco.service.PlayServiceCallback;
import com.duan.musicoco.util.BitmapUtils;
import com.duan.musicoco.util.ColorUtils;
import com.duan.musicoco.util.PeriodicTask;
import com.duan.musicoco.util.ToastUtils;
import com.duan.musicoco.util.Utils;
import com.duan.musicoco.view.media.PlayView;

/**
 * Created by DuanJiaNing on 2017/6/27.
 */

public class BottomNavigation implements
        OnPlayListVisibilityChange,
        View.OnClickListener,
        PlayServiceCallback,
        UserInterfaceUpdate,
        OnThemeChange {

    private final Activity activity;

    private View mContainer;
    private View mListContainer;
    private View mProgress;
    private ImageView mAlbum;
    private TextView mName;
    private TextView mArts;
    private PlayView mPlay;
    private ImageButton mShowList;

    private IPlayControl controller;
    private final PeriodicTask task;

    private long mDuration;

    private final MediaManager mediaManager;
    private final AppPreference appPreference;
    private BitmapBuilder builder;

    private final Dialog mDialog;
    private ListView mList;
    private TextView mPlayMode;
    private TextView mLocation;
    private PlayListAdapter adapter;

    BottomNavigation(Activity activity, MediaManager mediaManager, AppPreference appPreference) {
        this.activity = activity;
        this.mediaManager = mediaManager;
        this.appPreference = appPreference;
        this.builder = new BitmapBuilder(activity);
        this.mDialog = new Dialog(activity, R.style.BottomDialog);

        task = new PeriodicTask(new PeriodicTask.Task() {
            @Override
            public void execute() {
                mContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        updateProgress();
                    }
                });
            }
        }, 800);
    }

    public void initView() {
        mAlbum = (ImageView) activity.findViewById(R.id.list_album);
        mName = (TextView) activity.findViewById(R.id.list_name);
        mArts = (TextView) activity.findViewById(R.id.list_arts);
        mPlay = (PlayView) activity.findViewById(R.id.list_play);
        mShowList = (ImageButton) activity.findViewById(R.id.list_list);
        mContainer = activity.findViewById(R.id.list_bottom_nav_container);
        mProgress = activity.findViewById(R.id.list_progress);

        mContainer.setOnClickListener(this);
        mShowList.setOnClickListener(this);
        mPlay.setOnClickListener(this);

        View contentView = activity.getLayoutInflater().inflate(R.layout.main_play_list, null);
        mListContainer = contentView.findViewById(R.id.main_play_list_container);
        mList = (ListView) contentView.findViewById(R.id.main_play_list);
        mLocation = (TextView) contentView.findViewById(R.id.main_play_location);
        mPlayMode = (TextView) contentView.findViewById(R.id.main_play_mode);

        mLocation.setText("当前播放");
        Drawable drawable = activity.getResources().getDrawable(R.drawable.ic_location_searching_black_24dp);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        mLocation.setCompoundDrawablePadding(8);
        mLocation.setCompoundDrawables(null, null, drawable, null);

        mPlayMode.setCompoundDrawablePadding(8);
        mLocation.setOnClickListener(this);
        mPlayMode.setOnClickListener(this);

        mDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        DisplayMetrics metrics = Utils.getMetrics(activity);
        layoutParams.width = metrics.widthPixels;
        layoutParams.height = metrics.heightPixels * 5 / 9;
        contentView.setLayoutParams(layoutParams);
        mDialog.getWindow().setGravity(Gravity.BOTTOM);
        mDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mDialog.setCanceledOnTouchOutside(true);

    }

    public void initData(IPlayControl controller) {
        this.controller = controller;

        adapter = new PlayListAdapter(activity, controller);
        mList.setAdapter(adapter);

        update(null);
    }

    @Override
    public void songChanged(Song song, int index, boolean isNext) {
        SongInfo info = mediaManager.getSongInfo(song);
        mDuration = (int) info.getDuration();

        update(null);
    }

    @Override
    public void startPlay(Song song, int index, int status) {
        SongInfo info = mediaManager.getSongInfo(song);
        mDuration = (int) info.getDuration();

        task.start();

    }

    private void updateProgress() {
        int progress;
        final float phoneWidth = Utils.getMetrics(activity).widthPixels;

        try {
            progress = controller.getProgress();
            int width = (int) (phoneWidth * (progress / (float) mDuration));
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mProgress.getLayoutParams();
            params.width = width;
            mProgress.setLayoutParams(params);
            mProgress.invalidate();

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateSongInfo(SongInfo info) {
        String name = info.getTitle();
        String arts = info.getArtist();
        builder.reset();
        Bitmap b = builder.setPath(info.getAlbum_path())
                .resize(mAlbum.getHeight())
                .build()
                .getBitmap();

        if (b == null) {
            b = BitmapUtils.bitmapResizeFromResource(
                    activity.getResources(),
                    R.mipmap.default_album,
                    mAlbum.getWidth(),
                    mAlbum.getHeight());
        }

        mName.setText(name);
        mArts.setText(arts);
        mAlbum.setImageBitmap(b);

    }

    @Override
    public void update(@Nullable Object obj) {
        if (checkNull()) {
            return;
        }

        Theme theme = appPreference.getTheme();
        themeChange(theme);

        updatePlayMode();

        try {

            if (controller.status() == PlayController.STATUS_PLAYING) {
                mPlay.setPlayStatus(true);
            } else {
                mPlay.setPlayStatus(false);
            }

            Song song = controller.currentSong();
            SongInfo info = mediaManager.getSongInfo(song);
            mDuration = (int) info.getDuration();
            updateProgress();
            updateSongInfo(info);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean checkNull() {
        if (controller == null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stopPlay(Song song, int index, int status) {
        task.stop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_bottom_nav_container:
                activity.startActivity(new Intent(activity, PlayActivity.class));
                break;
            case R.id.list_play: {
                boolean play = mPlay.isChecked();
                try {
                    if (play) {
                        controller.resume();
                    } else controller.pause();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }
            case R.id.list_list:
                if (mDialog.isShowing()) {
                    hide();
                } else {
                    show();
                }
                break;
            case R.id.main_play_mode:
                try {
                    int mode = controller.getPlayMode();
                    mode = ((mode - 21) + 1) % 3 + 21;
                    controller.setPlayMode(mode);
                    updatePlayMode();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    new ExceptionHandler().handleRemoteException(activity,
                            activity.getString(R.string.exception_remote), null
                    );
                }
                break;
            case R.id.main_play_location:
                try {
                    int index = controller.currentSongIndex();
                    mList.smoothScrollToPosition(index);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    new ExceptionHandler().handleRemoteException(activity,
                            activity.getString(R.string.exception_remote), null
                    );
                }
                break;
        }
    }

    @Override
    public void themeChange(Theme theme) {
        switch (theme) {
            case DARK: {
                setTheme(theme);
                adapter.themeChange(theme);
                break;
            }
            case WHITE:
            default: {
                setTheme(theme);
                adapter.themeChange(theme);
                break;
            }
        }
    }

    private void setTheme(Theme theme) {
        int[] colors = new int[4];
        if (theme == Theme.DARK) {
            colors = ColorUtils.getDarkThemeColors(activity);
        } else if (theme == Theme.WHITE) {
            colors = ColorUtils.getWhiteThemeColors(activity);
        } else return;

        int mainBC = colors[0];
        int mainTC = colors[1];
        int vicTC = colors[3];

        mContainer.setBackgroundColor(mainBC);
        mName.setTextColor(mainTC);
        mArts.setTextColor(vicTC);
        mPlay.setPauseLineColor(mainTC);
        mPlay.setSolidColor(mainTC);
        mPlay.setTriangleColor(mainTC);
        mProgress.setBackgroundColor(mainTC);
        mList.setBackgroundColor(mainBC);

        mPlayMode.setTextColor(mainTC);
        mListContainer.setBackgroundColor(mainBC);
        mLocation.setTextColor(mainTC);
        for (Drawable d : mPlayMode.getCompoundDrawables()) {
            if (d != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    d.setTint(vicTC);
                }
            }
        }
        for (Drawable d : mLocation.getCompoundDrawables()) {
            if (d != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    d.setTint(vicTC);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mShowList.getDrawable().setTint(mainTC);
        }
    }

    void updatePlayMode() {

        Drawable drawable = null;
        StringBuilder builder = new StringBuilder();
        int mode = PlayController.MODE_LIST_LOOP;

        try {
            mode = controller.getPlayMode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        switch (mode) {
            case PlayController.MODE_LIST_LOOP:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable = activity.getDrawable(R.drawable.list_loop);
                } else drawable = activity.getResources().getDrawable(R.drawable.list_loop);
                builder.append(activity.getString(R.string.play_mode_list_loop));
                break;

            case PlayController.MODE_SINGLE_LOOP:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable = activity.getDrawable(R.drawable.single_loop);
                } else drawable = activity.getResources().getDrawable(R.drawable.single_loop);
                builder.append(activity.getString(R.string.play_mode_single_loop));
                break;

            case PlayController.MODE_RANDOM:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable = activity.getDrawable(R.drawable.random);
                } else drawable = activity.getResources().getDrawable(R.drawable.random);
                builder.append(activity.getString(R.string.play_mode_random));
                break;
        }

        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        mPlayMode.setCompoundDrawables(drawable, null, null, null);
        mPlayMode.setText(builder.toString());

        ToastUtils.showToast(activity, builder.toString());
    }

    @Override
    public void show() {

        if (mDialog.isShowing()) {
            return;
        } else {
            mDialog.show();
        }
    }

    @Override
    public void hide() {
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }

    }
}
