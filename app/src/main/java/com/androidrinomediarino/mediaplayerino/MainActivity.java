package com.androidrinomediarino.mediaplayerino;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SongMetadataFragment.OnFragmentInteractionListener,
                                                                CurrentPlaylistFragment.OnFragmentInteractionListener,
                                                                ActivityCompat.OnRequestPermissionsResultCallback {

    // Storage Permissions variables
    private static final int    REQUEST_CODE_EXTERNAL_STORAGE = 1;
    private int[]               PERMISSION_GRANT_RESULT;
    private static String[]     PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //MusicScanner
    //private ArrayList<Song>     musicFiles;

    //MusicPlayer Service
    private MusicPlayer         musicPlayer;
    private MusicScanner        musicScanner;
    private Intent              musicIntent;
    protected boolean           musicBound = false;
    private SeekBar             seekBar;
    private boolean             _permissionsReady = false;
    private ImageButton         previousButton;
    private ImageButton         playPauseButton;
    private ImageButton         nextButton;

    //Connect to MusicPlayer Service
    private ServiceConnection musicPlayerConnection = new ServiceConnection() {
        @Override
        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayer.MusicBinder binder = (MusicPlayer.MusicBinder)service;
            //get service
            musicPlayer = binder.getService();
            musicBound = true;
            // TODO: ASYNC
            run();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        if(musicIntent==null){
            musicIntent = new Intent(this, MusicPlayer.class);
            startService(musicIntent);
            bindService(musicIntent, musicPlayerConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //stopService(musicIntent);
        //musicPlayer = null;

        if (musicPlayerConnection != null) {
            unbindService(musicPlayerConnection);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
        }

        //Permissions: MainActivity, AndroidManifest.xml
        requestStoragePermissions(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // Grant Permission
    private void requestStoragePermissions(final Activity activity) {

        PERMISSION_GRANT_RESULT = new int[] {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE),
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        };

        if (PERMISSION_GRANT_RESULT[0] != PackageManager.PERMISSION_GRANTED ||  // Read
            PERMISSION_GRANT_RESULT[1] != PackageManager.PERMISSION_GRANTED) {  // Write

            final boolean requestPermissionRational = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if(requestPermissionRational) {

                showMessageOKCancel("You need to allow access to Music Storage",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // We don't have permission so prompt the user
                                ActivityCompat.requestPermissions(
                                        activity,
                                        PERMISSIONS_STORAGE,
                                        REQUEST_CODE_EXTERNAL_STORAGE
                                );
                            }
                        });
                return;

            } else {

                showMessageOKCancel("You need to allow access to Music Storage",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // We don't have permission so prompt the user
                                ActivityCompat.requestPermissions(
                                        activity,
                                        PERMISSIONS_STORAGE,
                                        REQUEST_CODE_EXTERNAL_STORAGE
                                );
                            }
                        });
            }
        } else {
            _permissionsReady = true;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // Verify Permission Granted
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String permissions[], final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted: dependent code is ready to run

                    //Get list of music files
//                    musicScanner = MusicScanner.getInstance();
//                    musicFiles = musicScanner.getMusicFiles();

                    // TODO: ASYNC
                    _permissionsReady = true;
                    run();

                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "READ_WRITE Denied", Toast.LENGTH_SHORT).show();
                }

            } break;
            default: super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // TODO: ASYNC tasks are funneled here
    private final void run() {
        Log.i("@MainActivity", "run() is called!");
        if(musicPlayer != null && _permissionsReady) {
            if(seekBar == null) {
                seekBar = (SeekBar)findViewById(R.id.seekBar);
                musicPlayer.setSeekBar(seekBar);
            }
            previousButton      = (ImageButton) findViewById(R.id.btn_previous);
            playPauseButton     = (ImageButton) findViewById(R.id.btn_play);
            nextButton          = (ImageButton) findViewById(R.id.btn_next);

            previousButton.setOnClickListener(previousButtonListener);
            playPauseButton.setOnClickListener(playPauseButtonListener);
            nextButton.setOnClickListener(nextButtonListener);

            musicScanner = MusicScanner.getInstance();
            if(musicScanner.isInitialized()) {
                // MusicPlayer & musicFiles ready
                Log.i("X", "Service is bonded successfully!");

                seekBar = (SeekBar)findViewById(R.id.seekBar);
                //seekBar.setClickable(false);
                musicPlayer.setSeekBar(seekBar);

                // Using the music files, create song objects and add to list.
//            SongList.addSongsToList(this, musicFiles);

                // Pass list of music files to MusicPlayer
                musicPlayer.setList(SongList.SongList);

                // Initialize the starting fragment
                CurrentPlaylistFragment initialFragment = new CurrentPlaylistFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragment_container, initialFragment, "CURRENT_PLAYLIST")
                        //.commit(); // TODO: Resolve error from commit
                        .commitAllowingStateLoss();

                // TODO: Placeholder auto play all music
                musicPlayer.startOrContinue();
                Log.i("@MainActivity", "musicPlayer.startOrContinue() is called!");
            }
        }
        
    }

    private final View.OnClickListener previousButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            Log.i("@MainActivity", "musicPlayer.previousSong() is called!");
            musicPlayer.previousSong();

            //Update SONG_METADATA fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment songMetaDataFragment = fragmentManager.findFragmentByTag("SONG_METADATA");

            if (songMetaDataFragment != null && songMetaDataFragment.isVisible()) {
                songMetaDataFragment = new SongMetadataFragment();
                Bundle bundle = new Bundle();
                bundle.putString("filePath", musicPlayer.getFilePath());
                songMetaDataFragment.setArguments(bundle);
                fragmentManager.beginTransaction().replace(R.id.fragment_container, songMetaDataFragment, "SONG_METADATA").commit();
            }
        }
    };

    private final View.OnClickListener playPauseButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            Log.i("@MainActivity", "musicPlayer.playPause() is called!");
            musicPlayer.playPause(playPauseButton);
        }
    };

    private final View.OnClickListener nextButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            Log.i("@MainActivity", "musicPlayer.nextSong() is called!");
            musicPlayer.nextSong();

            //Update SONG_METADATA fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment songMetaDataFragment = fragmentManager.findFragmentByTag("SONG_METADATA");

            if (songMetaDataFragment != null && songMetaDataFragment.isVisible()) {
                songMetaDataFragment = new SongMetadataFragment();
                Bundle bundle = new Bundle();
                bundle.putString("filePath", musicPlayer.getFilePath());
                songMetaDataFragment.setArguments(bundle);
                fragmentManager.beginTransaction().replace(R.id.fragment_container, songMetaDataFragment, "SONG_METADATA").commit();
            }
        }
    };

    /**
     * Implementation of OnFragmentInteraction interface method.
     */
    public void btn_fragmentSwitch_onClick() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment playlistFragment = fragmentManager.findFragmentByTag("CURRENT_PLAYLIST");

        if (playlistFragment != null && playlistFragment.isVisible()) {
            Fragment songMetaDataFragment = new SongMetadataFragment();
            Bundle bundle = new Bundle();
            bundle.putString("filePath", musicPlayer.getFilePath());
            songMetaDataFragment.setArguments(bundle);
            fragmentManager.beginTransaction().replace(R.id.fragment_container, songMetaDataFragment, "SONG_METADATA").commit();
        } else {
            fragmentManager.beginTransaction().replace(R.id.fragment_container, new CurrentPlaylistFragment(), "CURRENT_PLAYLIST").commit();
        }
    }

    public void btn_playlistSongSelect_onClick(Song song) {
        musicPlayer.playMusic(song);
    }

    public void menu_addSongs_onClick(MenuItem menuItem) {
        Intent intent = new Intent(getApplicationContext(), AddSongActivity.class);
        startActivity(intent);
    }
}
