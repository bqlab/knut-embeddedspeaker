package app.bqlab.embeddedspeaker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final int SONG_BBIBBI = 1;
    final int SONG_TRAVEL = 2;
    final int SONG_PHONECERT = 3;
    final int SETTING_FND_OFF = 4;
    final int SETTING_FND_ON = 5;
    final int SETTING_MOTOR_OFF = 6;
    final int SETTING_MOTOR_ON = 7;
    final int REQUEST_ENABLE_BT = 8;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice connectedDevice;
    Set<BluetoothDevice> pairedDevices;
    OutputStream outputStream;
    InputStream inputStream;
    byte[] readBuffer;
    int readBufferPosition;
    Thread connectThread;

    LinearLayout mainMusic;
    Button mainMusicProfile;
    TextView mainMusicName, mainMusicMusician;

    Switch mainBodyFndSwitch, mainBodyMotorSwitch;

    Button mainBarPrev, mainBarPlay, mainBarNext;

    boolean isConnected;
    int playSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music1));
        mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile1));
        mainMusicName.setText("삐삐");
        mainMusicMusician.setText("아이유");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
    }

    private void init() {
        //Data setting
        playSong = SONG_BBIBBI;

        //main_music setting
        mainMusic = findViewById(R.id.main_music);
        mainMusicProfile = findViewById(R.id.main_music_profile);
        mainMusicProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected)
                    playMusic();
                else
                    setAboutBluetooth();
            }
        });
        mainMusicName = findViewById(R.id.main_music_name);
        mainMusicMusician = findViewById(R.id.main_music_musician);

        //main_body setting
        mainBodyFndSwitch = findViewById(R.id.main_body_fnd_switch);
        mainBodyFndSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true));
        mainBodyFndSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isConnected) {
                    setAboutBluetooth();
                    mainBodyFndSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true));
                }
                else {
                    getSharedPreferences("setting", MODE_PRIVATE).edit().putBoolean("fnd", isChecked).apply();
                    try {
                        if (getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true))
                            outputStream.write(SETTING_FND_ON);
                        else
                            outputStream.write(SETTING_FND_OFF);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "디바이스와의 연결이 끊겼습니다.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        mainBodyMotorSwitch = findViewById(R.id.main_body_motor_switch);
        mainBodyMotorSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("motor", true));
        mainBodyMotorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isConnected) {
                    setAboutBluetooth();
                    mainBodyMotorSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true));
                }
                else {
                    getSharedPreferences("setting", MODE_PRIVATE).edit().putBoolean("motor", isChecked).apply();
                    try {
                        if (getSharedPreferences("setting", MODE_PRIVATE).getBoolean("motor", true))
                            outputStream.write(SETTING_MOTOR_ON);
                        else
                            outputStream.write(SETTING_MOTOR_OFF);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "디바이스와의 연결이 끊겼습니다.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //main_bar setting
        mainBarPlay = findViewById(R.id.main_bar_play);
        mainBarPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected)
                    playMusic();
                else
                    setAboutBluetooth();
            }
        });
    }

    private void setAboutBluetooth() {
        //Necessary objects.
        final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = bluetoothAdapter.getBondedDevices();

        //Check the device support bluetooth.
        if (bluetoothAdapter == null)
            showUnsupportedDeviceDialog();
            //Check bluetooth activated.
        else if (!bluetoothAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, REQUEST_ENABLE_BT);
        }
        //Check this device have paired devices.
        else if (pairedDevices.size() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("페어링된 디바이스가 없습니다.")
                    .setMessage("블루투스 설정 화면으로 이동하여 디바이스를 페어링한 후 다시 시도하세요.")
                    .setPositiveButton("설정화면으로 이동", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                        }
                    }).show();
        }
        //Check this device connected to other device.
        else if (!isConnected) {
            final ArrayList<String> deviceNames = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices)
                deviceNames.add(device.getName());

            //Find the connected device object.
            final CharSequence[] items = deviceNames.toArray(new CharSequence[0]);
            new AlertDialog.Builder(this)
                    .setTitle("페어링된 디바이스를 선택하세요.")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            //Make bluetooth socket.
                            for (BluetoothDevice device : pairedDevices)
                                if (device.getName().equals(items[which].toString()))
                                    connectedDevice = device;

                            try {
                                bluetoothSocket = connectedDevice.createRfcommSocketToServiceRecord(uuid);
                                bluetoothSocket.connect();

                                outputStream = bluetoothSocket.getOutputStream();
                                inputStream = bluetoothSocket.getInputStream();

                                readBuffer = new byte[1024];
                                readBufferPosition = 0;

                                connectDevice();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, "디바이스에 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .setNeutralButton("설정화면으로 이동", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                        }
                    })
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
    }

    private void connectDevice() {
        final Handler h = new Handler();
        connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (Thread.currentThread().isInterrupted()) {
                    try {
                        isConnected = true;
                        int byteAvailable = inputStream.available();
                        if (byteAvailable > 0) {
                            byte[] bytes = new byte[byteAvailable];
                            int readBytes = inputStream.read(bytes);
                            for (int i = 0; i < byteAvailable; i++) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String receivedContent = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                h.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Here is for access main thread.
                                        String s = "수신: " + receivedContent;
                                        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        connectThread.start();
    }

    @SuppressLint("SetTextI18n")
    private void playMusic() {
        if (isConnected) {
            try {
                switch (playSong) {
                    case SONG_BBIBBI:
                        outputStream.write(SONG_BBIBBI);
                        mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music1));
                        mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile1));
                        mainMusicName.setText("삐삐");
                        mainMusicMusician.setText("아이유");
                        break;
                    case SONG_TRAVEL:
                        outputStream.write(SONG_TRAVEL);
                        mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music2));
                        mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile2));
                        mainMusicName.setText("여행");
                        mainMusicMusician.setText("볼빨간사춘기");
                        break;
                    case SONG_PHONECERT:
                        outputStream.write(SONG_PHONECERT);
                        mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music3));
                        mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile3));
                        mainMusicName.setText("폰서트");
                        mainMusicMusician.setText("10CM");
                        break;
                }
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "디바이스와의 연결이 끊겼습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showUnsupportedDeviceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("지원하지 않는 기기입니다.")
                .setMessage("현재 디바이스에서 블루투스 기능을 사용할 수 없습니다. 다른 기기로 시도하세요.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finishAffinity();
                    }
                }).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    setAboutBluetooth();
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
                                    setAboutBluetooth();
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
