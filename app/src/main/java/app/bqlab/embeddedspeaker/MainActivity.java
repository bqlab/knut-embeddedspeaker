package app.bqlab.embeddedspeaker;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    LinearLayout mainMusic;
    Button mainMusicProfile;
    TextView mainMusicName, mainMusicMusician;

    Button mainBarPrev, mainBarPlay, mainBarNext;

    boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        //Function initializing
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //main_music setting
        mainMusic = findViewById(R.id.main_music);
        mainMusicProfile = findViewById(R.id.main_music_profile);
        mainMusicProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic();
            }
        });
        mainMusicName = findViewById(R.id.main_music_name);
        mainMusicMusician = findViewById(R.id.main_music_musician);

        //main_bar setting
        mainBarPlay = findViewById(R.id.main_bar_play);
        mainBarPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic();
            }
        });
    }

    private void playMusic() {
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "지원하지 않는 디바이스입니다.", Toast.LENGTH_LONG).show();
            finishAffinity();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, REQUEST_ENABLE_BT);
            isConnected = true;
        }
    }

    private void showDeviceList() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        
        new AlertDialog.Builder(this)
                .setTitle("연결할 디바이스를 선택하세요.")
                .setView(t)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    showDeviceList();
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    new AlertDialog.Builder(this)
                            .setTitle("블루투스 기능이 꺼져 있습니다.")
                            .setMessage("블루투스 기능이 계속해서 꺼져있을 경우 디바이스와 연결할 수 없습니다.")
                            .setPositiveButton("연결", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    playMusic();
                                }
                            })
                            .setNegativeButton("연결하지 않음", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(MainActivity.this, "블루투스 연결을 하지 않습니다.", Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            }).show();
            }
        }
    }
}
