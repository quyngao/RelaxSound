package com.example.mypc.relaxsound;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mypc.relaxsound.controls.Controls;
import com.example.mypc.relaxsound.service.SongService;
import com.example.mypc.relaxsound.util.PlayerConstants;
import com.example.mypc.relaxsound.util.UtilFunctions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class RelaxActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
    static Button btnPause, btnPlay, btnStop;
    static TextView tvTime;
    static Context context;
    static SeekBar bar;
    static ProgressBar sbar;
    static GridLayout gridLayout;
    static int w, h;
    static List<ImageView> listBtn;
    ScrollView scroll;
    static int location = -1;
    InterstitialAd mInterstitialAd;

    ImageView img_settime, img_share, img_s2b;
    LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.relax_activity);
        context = RelaxActivity.this;
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_full_screen));


        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);

        mInterstitialAd.setAdListener(new AdListener() {
            public void onAdLoaded() {
                showInterstitial();
            }
        });
        init();
    }

    public static void setFirstState() {
        btnPause.setEnabled(false);
        btnPlay.setEnabled(false);
        btnStop.setEnabled(false);
    }

    public static void unSetFirstState() {
        btnPause.setEnabled(true);
        btnPlay.setEnabled(true);
        btnStop.setEnabled(true);
    }

    private void init() {

        getViews();
        if (PlayerConstants.TIME == 0) {
            SharedPreferences pre = context.getSharedPreferences("myData", context.MODE_PRIVATE);
            PlayerConstants.TIME = pre.getInt("time", 0);
            tvTime.setText(settime(PlayerConstants.TIME));
            setProgessBar(PlayerConstants.TIME);

        }
        PlayerConstants.PROGRESSBAR_HANDLER = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int time = (int) msg.obj;
                Log.e("time", "" + time);
                sbar.setProgress(time);
                tvTime.setText(settime(time + 59));
                if (time == 0 && PlayerConstants.SONG_PAUSED == false) {
                    PlayerConstants.TIME = 0;
                    tvTime.setText(settime(0));
                    bar.setVisibility(View.INVISIBLE);
                    Intent i = new Intent(context, SongService.class);
                    context.stopService(i);
                }
            }
        };
        if (PlayerConstants.SONGS_LIST.size() <= 0) {
            PlayerConstants.SONGS_LIST = UtilFunctions.listOfSongs(getApplicationContext());
        } else {
            if (SongService.checkplay() > 0) unSetFirstState();
        }
        setListItems();
        setListeners();

    }

    private static void setListItems() {
        listBtn = UtilFunctions.getListBtn(context);
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(2);
        int size = h / 9;
        for (ImageView i : listBtn) {
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
            layoutParams.setMargins(70, 15, 70, 15);
            layoutParams.width = size;
            layoutParams.height = size;
            i.setLayoutParams(layoutParams);
            gridLayout.addView(i);
            i.setOnClickListener(Click);
        }

    }

    static View.OnClickListener Click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            PlayerConstants.SONG_NUMBER = position;
            if (PlayerConstants.SONGS_LIST.get(position).isplay()) {
                location = -1;

                PlayerConstants.SONGS_LIST.get(position).setIsplay(false);
                bar.setVisibility(View.INVISIBLE);
                PlayerConstants.SONGS_LIST.get(position).pause();
            } else {
                location = position;
                PlayerConstants.SONGS_LIST.get(position).setIsplay(true);
                unSetFirstState();
                bar.setProgress(PlayerConstants.SONGS_LIST.get(location).getVolume());
                bar.setVisibility(View.VISIBLE);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bar.setVisibility(View.INVISIBLE);
                    }
                }, 5000);
            }
            boolean isServiceRunning = UtilFunctions.isServiceRunning(SongService.class.getName(), context);
            if (!isServiceRunning) {
                Intent i = new Intent(context, SongService.class);
                context.startService(i);
            } else {
                PlayerConstants.SONG_CHANGE_HANDLER.sendMessage(PlayerConstants.SONG_CHANGE_HANDLER.obtainMessage());
            }
            if (SongService.checkplay() == -1) {
                setFirstState();
                SharedPreferences pre = context.getSharedPreferences("myData", context.MODE_PRIVATE);
                int time = pre.getInt("time", 0);
                PlayerConstants.TIME = time;
                tvTime.setText(settime(time));
                bar.setVisibility(View.INVISIBLE);
                Intent i = new Intent(context, SongService.class);
                context.stopService(i);
            }


        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        try {
            boolean isServiceRunning = UtilFunctions.isServiceRunning(SongService.class.getName(), getApplicationContext());
            if (isServiceRunning == false) {
                bar.setVisibility(View.INVISIBLE);
            } else {
                if (PlayerConstants.TIME > 0) {
                    tvTime.setText(settime(PlayerConstants.TIME));
                    setProgessBar(PlayerConstants.TIME);
                }
            }
            changeUI();
        } catch (Exception e) {
        }
    }

    private void showInterstitial() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    public static void setProgessBar(int tmp) {
        sbar.setProgress(0);
        sbar.setMax(tmp);
    }

    private void getViews() {
        btnPause = (Button) findViewById(R.id.button_pause);
        btnPlay = (Button) findViewById(R.id.button_start);
        btnStop = (Button) findViewById(R.id.button_stop);
        scroll = (ScrollView) findViewById(R.id.scroll);
        bar = (SeekBar) findViewById(R.id.sbar);
        tvTime = (TextView) findViewById(R.id.tv_time);
        gridLayout = (GridLayout) findViewById(R.id.grid);
        sbar = (ProgressBar) findViewById(R.id.sbars);
        sbar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        bar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Display display = getWindowManager().getDefaultDisplay();
        Point s = new Point();
        display.getSize(s);
        w = display.getWidth();
        h = display.getHeight();
        bar.setVisibility(View.INVISIBLE);
        bar.setOnSeekBarChangeListener(this);
        Typeface font_text = Typeface.createFromAsset(this.getAssets(), "fontchu.otf");
        tvTime.setTypeface(font_text);
        tvTime.setText(settime(PlayerConstants.TIME));
        img_settime = (ImageView) findViewById(R.id.img_settime);
        img_share = (ImageView) findViewById(R.id.img_share);
        img_s2b = (ImageView) findViewById(R.id.img_s2b);


        root = (LinearLayout) findViewById(R.id.root);
        img_settime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settime_Dialog endDialog = new Settime_Dialog(context, PlayerConstants.TIME, handler);
                endDialog.show();
            }
        });

        img_s2b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://play.google.com/store/apps/developer?id=S2B+Game+Studio");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        img_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap b = getBitmap(root);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), b, "Title", null);
                Uri imageUri = Uri.parse(path);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, imageUri);
                context.startActivity(Intent.createChooser(share, "Select"));
            }
        });
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int time = msg.arg1;
            int ischange = msg.arg2;
            Toast.makeText(RelaxActivity.this, "" + ischange, Toast.LENGTH_SHORT).show();
            tvTime.setText(settime(time));
            PlayerConstants.TIME = time;
            setProgessBar(time);
            boolean isServiceRunning = UtilFunctions.isServiceRunning(SongService.class.getName(), context);
            if (!isServiceRunning) {
                Intent i = new Intent(context, SongService.class);
                context.startService(i);
            } else {
                if(ischange==1)PlayerConstants.SONG_CHANGE_HANDLER.sendMessage(PlayerConstants.SONG_CHANGE_HANDLER.obtainMessage());
            }

        }
    };

    public static String settime(int x) {
        if (x == 0) return ".. : ..";
        x = x / 60;

        int h = x / 60;
        int m = x % 60;
        String s = "";
        if (h < 10) s = s + "0" + h + ":";
        else s = s + "" + h + ":";
        if (m < 10) s = s + "0" + m;
        else s = s + "" + m;
        return s;
    }

    private Bitmap getBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private static void setListeners() {

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Controls.playControl(context);
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bar.setVisibility(View.INVISIBLE);

                Controls.pauseControl(context);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFirstState();
                PlayerConstants.TIME = 0;
                tvTime.setText(settime(0));
                setProgessBar(PlayerConstants.TIME);
                sbar.setProgress(PlayerConstants.TIME);
                Intent i = new Intent(context, SongService.class);
                context.stopService(i);
            }
        });
    }

    public static void changeButton() {
        if (PlayerConstants.SONG_PAUSED) {
            btnPause.setVisibility(View.GONE);
            btnPlay.setVisibility(View.VISIBLE);
        } else {
            btnPause.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.GONE);
        }
    }

    public static void changeUI() {
        if (location >= 0 && PlayerConstants.SONG_PAUSED == false) {

        }
        setListItems();
        changeButton();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (location >= 0) {
            PlayerConstants.SONGS_LIST.get(location).setVolume(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}