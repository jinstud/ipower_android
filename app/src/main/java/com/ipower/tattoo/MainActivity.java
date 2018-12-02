package com.ipower.tattoo;

import java.io.File;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ChosenImages;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.ortiz.touch.TouchImageView;

public class MainActivity extends Activity implements ImageChooserListener {

    public static final int CONNECTION_RETRIES = 4;

    private AlertDialog dialog;
    private ProgressDialog loadingIndicator;
    private BluetoothAdapter bluetoothAdapter;
    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private Timer passwordTimer;
    private Timer pingTimer;

    private boolean passwordTimeout, outLine;

    private int voltsShaderValue = 0, voltsLinerValue = 0;

    private int connectionRetries = CONNECTION_RETRIES;
    private boolean notUserDisconnect = true;

	private boolean mAutoIncrement = false;	//	flag per l'autoincremento quando il bottone + rimane premuto
	private boolean mAutoDecrement = false;	//	flag per l'autodecremento quando il bottone - rimane premuto
	private Handler repeatUpdateHandler = new Handler();	//	handler per l'autoincremento e l'autodecremento
	
	private final static String TAG = MainActivity.class.getSimpleName();

	private String language;
	
	private boolean scanFlag = false;	//	flag che � true se � stata effettuata la scansione dei dispositivi

	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 800;	//	attesa in millisecondi prima di connetersi al dispositivo

    private Context context = this;
    private iPowerApplication app;

    private ImageChooserManager imageChooserManager;
    private ViewGroup previewPanel;
    private TouchImageView imageView;
    private boolean hasImage;
    private String previousImage;

    private Handler mTimerHandler;
    private boolean mTimerStarted;
    private long mTimerSeconds;
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTimerStarted) {
                mTimerSeconds += 1;

                long hours = mTimerSeconds / 3600;
                long minutes = (mTimerSeconds % 3600) / 60;
                long seconds = mTimerSeconds % 60;

                ((BigTextButton)findViewById(R.id.timer)).setText(String.format("%01d:%02d:%02d", hours, minutes, seconds));

                mTimerHandler.postDelayed(mTimerRunnable, 1000L);
            }
        }

    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        final boolean finish = getIntent().getBooleanExtra("finish", false);
        if (finish) {
            startActivity(new Intent(context, WelcomeActivity.class));
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        app = (iPowerApplication)getApplicationContext();
        app.setFootToggleOn(true);

        loadingIndicator = new ProgressDialog(context);
        loadingIndicator.setCancelable(false);

        mTimerHandler = new Handler();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName,	IBinder service) {
                app.bluetoothLeService = ((RBLService.LocalBinder) service).getService();
                if (!app.bluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                app.bluetoothLeService = null;
            }
        };

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                Log.i("DEBUG","BroadCast Receiver " + action);

                if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    if (notUserDisconnect && connectionRetries > 0) {
                        reconnectToDevice();
                        connectionRetries--;
                    } else if (notUserDisconnect) {
                        connectionRetries = CONNECTION_RETRIES;
                        iPowerApplication.makeToast(R.string.device_connection_error);
                    }
                    setButtonDisable();
                } else if (RBLService.ACTION_GATT_CONNECTED.equals(action)) {

                } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    getGattService(app.bluetoothLeService.getSupportedGattService());
                } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] data = new byte[3];
                    data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);
                    if (data[0] == 0x0B) { //output voltage from device
                        int vv = 256 * iPowerApplication.unsignedByte(data[1]);
                        vv += iPowerApplication.unsignedByte(data[2]);
                        displayData(String.format("%04.1f", (double)vv * 6.6 * 3.3 / 1024.0));
                    } else if (data[0] == 0x0E) { //speed and duty cycle
                        ToggleImageButton btnOn = (ToggleImageButton)findViewById(R.id.btn_on);
                        ToggleImageButton previewPedal = (ToggleImageButton)findViewById(R.id.preview_pedal);

                        int tt1 = iPowerApplication.unsignedByte(data[1]);
                        int tt2 = iPowerApplication.unsignedByte(data[2]);
                        BigTextButton lh = (BigTextButton)findViewById(R.id.vl_hz);
                        BigTextButton ld = (BigTextButton)findViewById(R.id.vl_duty);
                        if (tt1 == 255 && tt2 == 255) {
                            lh.setText("c.c.");
                            ld.setText("c.c.");
                        } else {
                            String ss;
                            ss = String.format("%d Hz", tt1);
                            if (ss != null) {
                                lh.setText(ss);
                            }
                            ss = String.format("%d %%",tt2);
                            if (ss != null) {
                                ld.setText(ss);
                            }
                        }
                    } else if (data[0] == 0x06 && data[2] == 0x55) { //signal from the pedal
                        ToggleImageButton btnOn = (ToggleImageButton)findViewById(R.id.btn_on);
                        ToggleImageButton previewPedal = (ToggleImageButton)findViewById(R.id.preview_pedal);

                        if (btnOn.isChecked() && data[1] == app.getRIFIUTO()) {
                            btnOn.setChecked(false);
                            previewPedal.setChecked(false);
                        } else if (!btnOn.isChecked() && data[1] == app.getCONFERMA()) {
                            btnOn.setChecked(true);
                            previewPedal.setChecked(true);
                        }
                    } else if (passwordTimeout) { //password validation transmitted read the response
                        passwordTimeout = false;
                        readRequestIdentificationResponse(data);
                    }
                }
            }
        };

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        app.setModFoot(sharedPreferences.getString("setting_footswitch_mode", "1"));
        app.setLanguage(sharedPreferences.getString("setting_language", "en"));
        app.setPassword(sharedPreferences.getString("setting_device_password", "tattoo"));

        voltsLinerValue = sharedPreferences.getInt("setting_liner", voltsLinerValue);
        voltsShaderValue = sharedPreferences.getInt("setting_shader", voltsShaderValue);

        previousImage = sharedPreferences.getString("preview_previous_image", "");

        language = app.setLanguageCreateActivity();
        
	    setContentView(R.layout.activity_main);

        resizeLayout();

        ((ToggleImageButton)findViewById(R.id.buttonConnect)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((ToggleImageButton)v).isChecked()) {
                    connectionRetries = CONNECTION_RETRIES;

                    if (scanFlag == false) {
                        notUserDisconnect = true;
                        scanForDevices();
                    }
                } else if (app.isConnState()) {
                    notUserDisconnect = false;

                    app.bluetoothLeService.disconnect();
                    app.bluetoothLeService.close();

                    setButtonDisable();
                }
            }
        });
		
		((ImageButton)findViewById(R.id.bt_menu)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
		
		final SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
		sb_volt.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onSeekBarChanged(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});

        ((BigTextButton)findViewById(R.id.timer)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mTimerSeconds = 0;
                ((BigTextButton)v).setText(String.format("%01d:%02d:%02d", 0, 0, 0));
                return false;
            }
        });
		
		class RptUpdater implements Runnable {
			public void run() {
		        if(mAutoIncrement) {
		            increment();
		            repeatUpdateHandler.postDelayed(new RptUpdater(), 100);
		        } else if(mAutoDecrement) {
		            decrement();
		            repeatUpdateHandler.postDelayed(new RptUpdater(), 100);
		        }
		    }
		}
		
		Button bt_piu = (Button)findViewById(R.id.bt_piu);	
		bt_piu.setOnLongClickListener( 
            new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    mAutoIncrement = true;
                    repeatUpdateHandler.post(new RptUpdater());
                    return false;
                }
            }
        );
			
		bt_piu.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    mAutoIncrement = false;
                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_off_intero);
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    increment();
                    deselectPresets();

                    LinearLayout ll = (LinearLayout) findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_up_on);
                }
                return false;
            }
        });

        ImageButton previewPlus = (ImageButton)findViewById(R.id.preview_plus);
        previewPlus.setOnLongClickListener(
                new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        mAutoIncrement = true;
                        repeatUpdateHandler.post(new RptUpdater());
                        return false;
                    }
                }
        );

        previewPlus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    mAutoIncrement = false;
                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_off_intero);
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    increment();
                    deselectPresets();

                    LinearLayout ll = (LinearLayout) findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_up_on);
                }
                return false;
            }
        });
		
		Button bt_meno = (Button)findViewById(R.id.bt_meno);		
		bt_meno.setOnLongClickListener( 
            new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    mAutoDecrement = true;
                    repeatUpdateHandler.post(new RptUpdater());
                    return false;
                }
            }
	    );  
		
		bt_meno.setOnTouchListener(new View.OnTouchListener() {
	        public boolean onTouch(View v, MotionEvent event) {
	            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    mAutoDecrement = false;
                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_off_intero);
	            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
	                decrement();
                    deselectPresets();

                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_down_on);
	            }
	            return false;
	        }
	    });

        ImageButton previewMinus = (ImageButton)findViewById(R.id.preview_minus);
        previewMinus.setOnLongClickListener(
                new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        mAutoDecrement = true;
                        repeatUpdateHandler.post(new RptUpdater());
                        return false;
                    }
                }
        );

        previewMinus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    mAutoDecrement = false;
                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_off_intero);
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    decrement();
                    deselectPresets();

                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_down_on);
                }
                return false;
            }
        });

        imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE);
        imageChooserManager.setImageChooserListener(this);

        previewPanel = (ViewGroup)findViewById(R.id.preview_panel);
        previewPanel.setVisibility(View.GONE);

        imageView = (TouchImageView)findViewById(R.id.preview_img);

        final ImageButton previewChoose = (ImageButton)findViewById(R.id.preview_choose);
        previewChoose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    imageChooserManager.choose();
                } catch (Exception e) {}
            }
        });

        ImageButton previewClose = (ImageButton)findViewById(R.id.preview_close);
        previewClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Animation previewDown = AnimationUtils.loadAnimation(context,
                        R.anim.preview_down);

                previewPanel.startAnimation(previewDown);
                previewPanel.setVisibility(View.INVISIBLE);
            }
        });

        ImageButton photo = (ImageButton)findViewById(R.id.photo);
        photo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation previewUp = AnimationUtils.loadAnimation(context,
                        R.anim.preview_up);

                previewUp.setAnimationListener(new Animation.AnimationListener() {
                   public void onAnimationStart(Animation animation) {

                       if (!hasImage && previousImage.length() > 0) {
                           File imageFile = new File(previousImage);
                           if (imageFile.exists()) {
                               imageView.setImageURI(Uri.fromFile(imageFile));
                           } else {
                               previousImage = "";
                           }
                       }
                   }

                   public void onAnimationRepeat(Animation animation) {
                   }

                   public void onAnimationEnd(Animation animation) {
                       if (!hasImage && previousImage.length() == 0) {
                           previewChoose.performClick();
                       }
                   }
                });

                previewPanel.startAnimation(previewUp);
                previewPanel.setVisibility(View.VISIBLE);
            }
        });
		
		ToggleButton selL = (ToggleButton)findViewById(R.id.sel1);
		selL.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ToggleButton selS = (ToggleButton)findViewById(R.id.sel2);
                    selS.setChecked(false);

                    SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
                    sb_volt.setProgress(voltsLinerValue);

                    deselectPresets();

                    BigTextButton tv = (BigTextButton)findViewById(R.id.lb_liner);
                    tv.setText(getResources().getString(R.string.lb_liner));

                    LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_left_on);
                } else {
                    BigTextButton tv = (BigTextButton)findViewById(R.id.lb_liner);
                    tv.setText(getResources().getString(R.string.vuoto));
                }
			}
		});
		
		selL.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
                voltsLinerValue = sb_volt.getProgress();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("setting_liner", voltsLinerValue);
                editor.putBoolean("outL", true);
                editor.commit();

                return false;
            }
        });
		
		ToggleButton selS = (ToggleButton)findViewById(R.id.sel2);
		selS.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ToggleButton selL = (ToggleButton)findViewById(R.id.sel1);
                    selL.setChecked(false);

                    SeekBar sb_volt=(SeekBar)findViewById(R.id.seekbar_volt);
                    sb_volt.setProgress(voltsShaderValue);

                    deselectPresets();

                    BigTextButton tv = (BigTextButton)findViewById(R.id.lb_shader);
                    tv.setText(getResources().getString(R.string.lb_shader));

                    LinearLayout ll = (LinearLayout) findViewById(R.id.bot_centrale);
                    ll.setBackgroundResource(R.drawable.central_right_on);
                } else {
                    BigTextButton tv = (BigTextButton)findViewById(R.id.lb_shader);
                    tv.setText(getResources().getString(R.string.vuoto));
                }
			}			
		});	
		
		selS.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
                voltsShaderValue = sb_volt.getProgress();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("setting_shader", voltsShaderValue);
                editor.putBoolean("outS", true);
                editor.commit();

                return false;
            }
	    });

        final ToggleImageButton btnOn = (ToggleImageButton)findViewById(R.id.btn_on);
        final ToggleImageButton previewPedal = (ToggleImageButton)findViewById(R.id.preview_pedal);

        btnOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (btnOn.isChecked()) {
                    previewPedal.setChecked(true);
                    if (app.isConnState()) {
                        byte buf[] = new byte[] {(byte)0x73, app.getCONFERMA(), (byte)0x55};
                        app.sendBluetoothData(buf);
                    } else {
                        (new Handler()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                btnOn.setChecked(false);
                                previewPedal.setChecked(false);
                            }
                        }, 100);
                    }
                } else {
                    if (app.isConnState()) {
                        byte buf[] = new byte[] {(byte)0x73, app.getRIFIUTO(), (byte)0x55};
                        app.sendBluetoothData(buf);
                    }
                    previewPedal.setChecked(false);
                }

                if (app.isConnState()) {
                    byte buf[] = new byte[] {(byte)0x73, app.getRIFIUTO(), (byte)0x55};

                    if (app.getModFoot().equals("3")) { //In-app toggle
                        buf[0] = 0x73;
                    } else if (app.getModFoot().equals("2")) { //Toggle
                        buf[0] = 0x53;
                    } else { //Continuous
                        buf[0] = 0x13;
                    }

                    app.sendBluetoothData(buf);
                }
            }
        });

        btnOn.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final ToggleImageButton buttonView, boolean isChecked) {
                //timer
                mTimerStarted = isChecked;
                if (mTimerStarted) {
                    mTimerHandler.postDelayed(mTimerRunnable, 1000L);
                }
            }
        });

        previewPedal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previewPedal.isChecked()) {
                    btnOn.setChecked(true);
                    if (app.isConnState()) {
                        byte buf[] = new byte[] {(byte)0x73, app.getCONFERMA(), (byte)0x55};
                        app.sendBluetoothData(buf);
                    } else {
                        (new Handler()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                btnOn.setChecked(false);
                                previewPedal.setChecked(false);
                            }
                        }, 100);
                    }
                } else {
                    if (app.isConnState()) {
                        byte buf[] = new byte[] {(byte)0x73, app.getRIFIUTO(), (byte)0x55};
                        app.sendBluetoothData(buf);
                    }
                    btnOn.setChecked(false);
                }

                if (app.isConnState()) {
                    byte buf[] = new byte[] {(byte)0x73, app.getRIFIUTO(), (byte)0x55};

                    if (app.getModFoot().equals("3")) { //In-app toggle
                        buf[0] = 0x73;
                    } else if (app.getModFoot().equals("2")) { //Toggle
                        buf[0] = 0x53;
                    } else { //Continuous
                        buf[0] = 0x13;
                    }

                    app.sendBluetoothData(buf);
                }
            }
        });

        final ToggleImageButton selOutL1 = (ToggleImageButton)findViewById(R.id.L1);
        final ToggleImageButton selOutL2 = (ToggleImageButton)findViewById(R.id.L2);
        final ToggleImageButton previewL1 = (ToggleImageButton)findViewById(R.id.preview_l1);
        final ToggleImageButton previewL2 = (ToggleImageButton)findViewById(R.id.preview_l2);

		selOutL1.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked) {
                if (isChecked) {
                    outLine = true;

                    byte buf[] = new byte[] {(byte)0x04, (byte)0x44, (byte)0x55};
                    app.sendBluetoothData(buf);

                    previewL1.setChecked(true);

                    selOutL2.setChecked(false);
                    previewL2.setChecked(false);
                }
			}			
		});

        previewL1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selOutL1.setChecked(((ToggleImageButton)v).isChecked());
            }
        });

        selOutL2.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked) {
				if (isChecked) {
                    outLine = false;

                    byte buf[] = new byte[] {(byte)0x04, (byte)0x66, (byte)0x55};
                    app.sendBluetoothData(buf);

                    previewL2.setChecked(true);

                    selOutL1.setChecked(false);
                    previewL1.setChecked(false);
                }
			}			
		});

        previewL2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selOutL2.setChecked(((ToggleImageButton) v).isChecked());
            }
        });

        ToggleImageButton preset1 = (ToggleImageButton)findViewById(R.id.preset1);
        preset1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVoltagePreset(1);
                deselectPresets(1);
            }
        });

        preset1.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
                savePreset(1);
                setVoltagePreset(1);
                deselectPresets(1);
				return false;
			}	            	
        });

        ToggleImageButton preset2 = (ToggleImageButton)findViewById(R.id.preset2);
        preset2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVoltagePreset(2);
                deselectPresets(2);
            }
        });

        preset2.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
                savePreset(2);
                setVoltagePreset(2);
                deselectPresets(2);
				return false;
			}	            	
        });

        ToggleImageButton preset3 = (ToggleImageButton)findViewById(R.id.preset3);
        preset3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVoltagePreset(3);
                deselectPresets(3);
            }
        });

        preset3.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
                savePreset(3);
                setVoltagePreset(3);
                deselectPresets(3);
				return false;
			}	            	
        });

        ToggleImageButton preset4 = (ToggleImageButton)findViewById(R.id.preset4);
        preset4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVoltagePreset(4);
                deselectPresets(4);
            }
        });

        preset4.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
                savePreset(4);
                setVoltagePreset(4);
                deselectPresets(4);
				return false;
			}	            	
        });

        ToggleImageButton preset5 = (ToggleImageButton)findViewById(R.id.preset5);
        preset5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVoltagePreset(5);
                deselectPresets(5);
            }
        });

        preset5.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
                savePreset(5);
                setVoltagePreset(5);
                deselectPresets(5);
				return false;
			}	            	
        });

        sb_volt.setProgress(0);

        selOutL1.setChecked(false);
        selOutL2.setChecked(false);

        selL.setChecked(false);
        selS.setChecked(false);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            iPowerApplication.makeToast("Ble not supported"); //TODO: straing to resources
			finish();
		}

		BluetoothManager mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = mBluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
            iPowerApplication.makeToast("Ble not supported"); //TODO: straing to resources
			finish();
			return;
		}

		Intent gattServiceIntent = new Intent(MainActivity.this, RBLService.class);
		bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        displayData(String.format("%04.1f", 0.0f));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);

        app.setModFoot(sharedPreferences.getString("setting_footswitch_mode", "1"));
        app.setLanguage(sharedPreferences.getString("setting_language", "en"));
        app.setPassword(sharedPreferences.getString("setting_device_password", "tattoo"));

        voltsLinerValue = sharedPreferences.getInt("setting_liner", voltsLinerValue);
        voltsShaderValue = sharedPreferences.getInt("setting_shader", voltsShaderValue);

        ((TextView)findViewById(R.id.lbl_preset1)).setText(sharedPreferences.getString("setting_machine_1_name", "MACHINE1"));
        ((TextView)findViewById(R.id.lbl_preset2)).setText(sharedPreferences.getString("setting_machine_2_name", "MACHINE2"));
        ((TextView)findViewById(R.id.lbl_preset3)).setText(sharedPreferences.getString("setting_machine_3_name", "MACHINE3"));
        ((TextView)findViewById(R.id.lbl_preset4)).setText(sharedPreferences.getString("setting_machine_4_name", "MACHINE4"));
        ((TextView)findViewById(R.id.lbl_preset5)).setText(sharedPreferences.getString("setting_machine_5_name", "MACHINE5"));

		if (!language.equals(app.getLanguage())) {
			language = app.getLanguage();
			Intent refresh = new Intent(this, MainActivity.class); 
			finish();
			startActivity(refresh);
		}

		String mf = app.getModFoot();

        if (mf.equals("1")) { // Continuous

		} else if (mf.equals("2")) { // Toggle

		} else if (mf.equals("3"))  { // In-app toggle
            ToggleImageButton btnOn = (ToggleImageButton)findViewById(R.id.btn_on);
            ToggleImageButton previewPedal = (ToggleImageButton)findViewById(R.id.preview_pedal);

			btnOn.setChecked(app.isFootToggleOn());
            previewPedal.setChecked(app.isFootToggleOn());
		}

        if (!bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		registerReceiver(broadcastReceiver, iPowerApplication.makeGattUpdateIntentFilter());
	}
	
	@Override
	protected void onStop() {
		super.onStop();

        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) { }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
        editor.putInt("setting_volts_value", sb_volt.getProgress());

		editor.putBoolean("L1", outLine);
		editor.putBoolean("L2", !outLine);

		ToggleButton selL = (ToggleButton)findViewById(R.id.sel1);
		editor.putBoolean("setting_liner_selected", selL.isChecked());

		ToggleButton selS = (ToggleButton)findViewById(R.id.sel2);
		editor.putBoolean("setting_shader_selected", selS.isChecked());

		editor.commit();

        mTimerStarted = false;
        mTimerHandler.removeCallbacks(mTimerRunnable);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (serviceConnection != null) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) { }
        }
	}

    private void onSeekBarChanged(int progress) {
        if (progress < 0) {
            progress = 0;
        }

        byte buf[] = new byte[] {(byte)0x02, (byte)progress, (byte)0x55};
        app.sendBluetoothData(buf);
    }
	
	private void decrement() {
        SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
        int pg = sb_volt.getProgress();
        if (pg > 0) {
            pg--;
            sb_volt.setProgress(pg);
        }
	}
	
	private void increment() {
        SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
        int pg = sb_volt.getProgress();
        if (pg < sb_volt.getMax()) {
            pg++;
            sb_volt.setProgress(pg);
        }
	}
	
	private void savePreset(int index) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);
        editor.putInt("setting_machine_".concat(String.valueOf(index)), sb_volt.getProgress());
        editor.commit();
	}
	
	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null) {
            return;
        }

        app.setConnState(true);

        app.characteristicTx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
		BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		app.bluetoothLeService.setCharacteristicNotification(characteristicRx, true);
		app.bluetoothLeService.readCharacteristic(characteristicRx);

		sendPasswordToDevice();
	}
	
	private void sendPasswordToDevice() {
		String password = app.getPassword();

		byte buf[] = new byte[] {(byte)0x07, (byte)0x00, (byte)0x00};

		Random random = new Random();
        app.setNumIdTrasm((byte)random.nextInt(256));

        buf[0] = (byte)7;
		buf[1] = (byte)password.length(); // max password length 15
		buf[2] = app.getNumIdTrasm();

		app.sendBluetoothData(buf);

		//Log.i("DEBUG", "byte Tx: "+ iPowerApplication.byteToHex(buf[0])+String.valueOf((int)buf[1])+ iPowerApplication.byteToHex(buf[2]));

        int b0;
		for (int i = 0; i < password.length(); i++) {
            b0 = ((i+1)<<4)+7;
            buf[0] = (byte)b0;
			buf[1] = (byte)password.charAt(i);

            app.sendBluetoothData(buf);
            //Log.i("DEBUG", "byte Tx: "+ iPowerApplication.byteToHex(buf[0])+String.valueOf((char)buf[1])+ iPowerApplication.byteToHex(buf[2]));
		}

		//Log.i("DEBUG", "Password: " + password);

        passwordTimeout = true;

        if (passwordTimer != null) {
            passwordTimer.cancel();
            passwordTimer.purge();
        }

		passwordTimer = new Timer();
        passwordTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingIndicator.isShowing()) {
                            loadingIndicator.dismiss();
                        }

                        if (passwordTimeout) {
                            passwordTimeout = false;
                            app.bluetoothLeService.disconnect();
                            app.bluetoothLeService.close();
                            setButtonDisable();
                            iPowerApplication.makeToast(R.string.device_timeout);
                        }
                    }
                });
            }
        }, 10000);
	}
	
	private void displayData(String data) {
		if (data != null) {
            ((BigTextButton)findViewById(R.id.lb_tensione)).setText(data);
            ((BigTextButton)findViewById(R.id.lb_tensione_preview)).setText(data);
		}
	}
	
	private void readRequestIdentificationResponse(byte[] data) {
		//Log.i("DEBUG", "Response for password "+ iPowerApplication.byteArrayToHex(data));
		for (int i = 0; i < data.length; i += 3) {
			if (data[i] == 0x08) {
				if (data[i + 2] == app.getNumIdTrasm()) {
					if (data[i + 1] == app.getCONFERMA()) {
						//Log.i("DEBUG", "Password accepted");

						SeekBar sb_volt = (SeekBar)findViewById(R.id.seekbar_volt);

                        byte[] buf = new byte[] {(byte)0x02, (byte)sb_volt.getProgress(), (byte)0x55};
                        app.sendBluetoothData(buf);

                        buf = new byte[] {(byte)0x04, (byte)0x00, (byte)0x55};

                        ToggleImageButton L1 = (ToggleImageButton)findViewById(R.id.L1);
                        ToggleImageButton L2 = (ToggleImageButton)findViewById(R.id.L2);

                        if (L1.isChecked()) {
                            buf[1] = 0x44;
                        } else if (L2.isChecked()) {
                            buf[1] = 0x66;
                        } else {
                            buf[1] = 0x44;
                            L1.setChecked(true);
                        }

						app.sendBluetoothData(buf);

						String foot = app.getModFoot();
						buf[1] = 0x00;
                        buf[2] = 0x55;

						if (foot.equals("3")) { //In-app toggle
							buf[0] = 0x73;
						} else if (foot.equals("2")) { //Toggle
							buf[0] = 0x53;
						} else { //Continuous
                            buf[0] = 0x13;
                        }

						app.sendBluetoothData(buf);

                        setButtonEnable();

                        connectionRetries = CONNECTION_RETRIES;
					} else {
						app.bluetoothLeService.disconnect();
						app.bluetoothLeService.close();

                        iPowerApplication.makeToast(R.string.device_incorrect_password);

                        setButtonDisable();
					}

                    if (loadingIndicator.isShowing()) {
                        loadingIndicator.dismiss();
                    }

                    passwordTimer.cancel();
                    passwordTimer.purge();
				}
			} 
		}
	}
	
	private void setButtonEnable() {
        ToggleImageButton connectBtn = (ToggleImageButton)findViewById(R.id.buttonConnect);
		connectBtn.setChecked(true);

        pingTimer = new Timer();
        pingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToggleImageButton btnOn = (ToggleImageButton)findViewById(R.id.btn_on);

                        if (!btnOn.isChecked()) {
                            ToggleImageButton L1 = (ToggleImageButton)findViewById(R.id.L1);
                            ToggleImageButton L2 = (ToggleImageButton)findViewById(R.id.L2);

                            byte[] buf = new byte[] {(byte)0x04, (byte)0x00, (byte)0x55};

                            if (L1.isChecked()) {
                                buf[1] = (byte)0x44;
                            } else if (L2.isChecked()) {
                                buf[1] = (byte)0x66;
                            }

                            app.sendBluetoothData(buf);
                        }
                    }
                });
            }
        }, 30000);
	}

	private void setButtonDisable() {	
		scanFlag = false;

		app.setConnState(false);

        ToggleImageButton connectBtn = (ToggleImageButton)findViewById(R.id.buttonConnect);
        connectBtn.setChecked(false);

        ToggleImageButton btnOn = (ToggleImageButton)findViewById(R.id.btn_on);
        ToggleImageButton previewPedal = (ToggleImageButton)findViewById(R.id.preview_pedal);

		btnOn.setChecked(false);
        previewPedal.setChecked(false);

        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer.purge();
        }

        displayData(String.format("%04.1f", 0.0f));
	}
	
	private void scanForDevices() {
        app.setDeviceAdapter(new DeviceAdapter(context, R.layout.list_item_found_device, new LinkedList<Device>()));
        app.getDeviceAdapter().clear();

        loadingIndicator.setMessage(context.getResources().getString(R.string.searching_dots));
        loadingIndicator.show();

        if (android.os.Build.VERSION.SDK_INT < 21) {
            leScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread (new Runnable() {
                        @Override
                        public void run() {
                            if (device != null) {
                                String deviceName = device.getName();
                                if (deviceName != null) {
                                    Log.i("DEBUG", deviceName);
                                    if (deviceName.contains("Shield") || deviceName.contains("Biscuit")) {
                                        saveDevice(device);
                                    } else {
                                        Log.i("DEBUG", "Device name is not valid");
                                    }
                                } else {
                                    String deviceAddress = device.getAddress();
                                    if (deviceAddress != null) {
                                        Log.i("DEBUG", deviceAddress);
                                        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                                        deviceName = bluetoothDevice.getName();
                                        if (deviceName != null) {
                                            Log.i("deviceName", deviceName);
                                            if (deviceName.contains("Shield") || deviceName.contains("Biscuit")) {
                                                saveDevice(bluetoothDevice);
                                            } else {
                                                Log.i("DEBUG", "Device name is not valid");
                                            }
                                        } else {
                                            Log.i("DEBUG", "Device name is not valid. Name is NULL.");
                                        }
                                    } else {
                                        Log.i("DEBUG", "Device UUID is not found.");
                                    }
                                }
                            } else {
                                Log.i("DEBUG", "Device is NULL.");
                            }
                        }
                    });
                }
            };

            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(final int callbackType, final ScanResult result) {
                    runOnUiThread (new Runnable() {
                        @Override
                        public void run() {
                            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                                if (result.getDevice() != null) {
                                    String deviceName = result.getDevice().getName();
                                    if (deviceName != null) {
                                        Log.i("DEBUG", deviceName);
                                        if (deviceName.contains("Shield") || deviceName.contains("Biscuit")) {
                                            saveDevice(result.getDevice());
                                        } else {
                                            Log.i("DEBUG", "Device name is not valid");
                                        }
                                    } else {
                                        String deviceAddress = result.getDevice().getAddress();
                                        if (deviceAddress != null) {
                                            Log.i("DEBUG", deviceAddress);
                                            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                                            deviceName = bluetoothDevice.getName();
                                            if (deviceName != null) {
                                                Log.i("deviceName", deviceName);
                                                if (deviceName.contains("Shield") || deviceName.contains("Biscuit")) {
                                                    saveDevice(bluetoothDevice);
                                                } else {
                                                    Log.i("DEBUG", "Device name is not valid");
                                                }
                                            } else {
                                                Log.i("DEBUG", "Device name is not valid. Name is NULL.");
                                            }
                                        } else {
                                            Log.i("DEBUG", "Device UUID is not found.");
                                        }
                                    }
                                } else {
                                    Log.i("DEBUG", "Device is NULL.");
                                }
                            } else {
                                Log.i("DEBUG", "Incorrect Callback type.");
                            }
                        }
                    });
                }
            };

            bluetoothAdapter.getBluetoothLeScanner().startScan(null, new ScanSettings.Builder() //
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback);
        }

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (android.os.Build.VERSION.SDK_INT < 21) {
                            bluetoothAdapter.stopLeScan(leScanCallback);
                        } else {
                            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                        }

                        if (app.getDeviceAdapter().isEmpty()) {
                            if (connectionRetries > 0) {
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        scanForDevices();
                                        connectionRetries--;
                                    }
                                }, 1000);
                            } else {
                                if (loadingIndicator.isShowing()) {
                                    loadingIndicator.dismiss();
                                }
                                iPowerApplication.makeToast(R.string.device_not_found);
                                setButtonDisable();
                            }
                        } else if (app.getDeviceAdapter().getCount() == 1) {
                            loadingIndicator.setMessage(context.getResources().getString(R.string.connecting_dots));
                            connectToDevice(0);
                        } else {
                            LayoutInflater layoutInflater = LayoutInflater.from(context);
                            View selectDeviceView = layoutInflater.inflate(R.layout.dialog_select_device, null);
                            ListView devicesListView = (ListView) selectDeviceView.findViewById(R.id.devicesList);
                            devicesListView.setAdapter(app.getDeviceAdapter());

                            AlertDialog.Builder builder = new AlertDialog.Builder(context).setInverseBackgroundForced(true);
                            builder.setView(selectDeviceView);
                            dialog = builder.create();

                            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    if (loadingIndicator.isShowing()) {
                                        loadingIndicator.dismiss();
                                    }
                                }
                            });
                            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {

                                }
                            });
                            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    loadingIndicator.setMessage(context.getResources().getString(R.string.connecting_dots));
                                }
                            });

                            dialog.show();

                            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);

                            dialog.getWindow().setLayout(size.x, LayoutParams.WRAP_CONTENT);
                        }
                    }
                });
            }
        }, SCAN_PERIOD);
	}

    public void connectToDevice(int index) {
        if (passwordTimer != null) {
            passwordTimer.cancel();
            passwordTimer.purge();
        }

        app.setmDevice(app.getDeviceAdapter().getItem(index).getDevice());

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        if (!app.isConnState()) {
            app.bluetoothLeService.connect(app.getmDevice().getAddress());
            scanFlag = true;
        } else {
            app.bluetoothLeService.disconnect();
            app.bluetoothLeService.close();
            setButtonDisable();
        }
    }

    public void reconnectToDevice() {
        if (app.getmDevice() != null) {
            app.bluetoothLeService.connect(app.getmDevice().getAddress());
            scanFlag = true;
        }
    }

	private void saveDevice(BluetoothDevice device) {
		if (!app.deviceExists(device)) {
            app.getDeviceAdapter().add(new Device(device));
		}
	}

    private ScanCallback scanCallback;
    private BluetoothAdapter.LeScanCallback leScanCallback;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
        if (resultCode == RESULT_OK && (requestCode == ChooserType.REQUEST_PICK_PICTURE|| requestCode == ChooserType.REQUEST_CAPTURE_PICTURE)) {
            imageChooserManager.submit(requestCode, data);
        }
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setVoltagePreset(int index) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        ((SeekBar)findViewById(R.id.seekbar_volt)).setProgress(sharedPreferences.getInt("setting_machine_".concat(String.valueOf(index)), 0));
	}

	private void deselectPresets() {
        ((ToggleImageButton)findViewById(R.id.preset1)).setChecked(false);
        ((ToggleImageButton)findViewById(R.id.preset2)).setChecked(false);
        ((ToggleImageButton)findViewById(R.id.preset3)).setChecked(false);
        ((ToggleImageButton)findViewById(R.id.preset4)).setChecked(false);
        ((ToggleImageButton)findViewById(R.id.preset5)).setChecked(false);

        LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
        ll.setBackgroundResource(R.drawable.central_off_intero);

        BigTextButton tv = (BigTextButton)findViewById(R.id.lb_shader);
        tv.setText(getResources().getString(R.string.vuoto));
        tv = (BigTextButton)findViewById(R.id.lb_liner);
        tv.setText(getResources().getString(R.string.vuoto));
	}

	private void deselectPresets(int index) {
        ((ToggleImageButton)findViewById(R.id.preset1)).setChecked(index == 1);
        ((ToggleImageButton)findViewById(R.id.preset2)).setChecked(index == 2);
        ((ToggleImageButton)findViewById(R.id.preset3)).setChecked(index == 3);
        ((ToggleImageButton)findViewById(R.id.preset4)).setChecked(index == 4);
        ((ToggleImageButton)findViewById(R.id.preset5)).setChecked(index == 5);

        LinearLayout ll = (LinearLayout)findViewById(R.id.bot_centrale);
        ll.setBackgroundResource(R.drawable.central_off_intero);

        BigTextButton tv = (BigTextButton)findViewById(R.id.lb_shader);
        tv.setText(getResources().getString(R.string.vuoto));
        tv = (BigTextButton)findViewById(R.id.lb_liner);
        tv.setText(getResources().getString(R.string.vuoto));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}

	private float calcolaSommaWeight(View v) {
		ViewGroup vg = (ViewGroup)v;
		LayoutParams pm;
		int nf = vg.getChildCount();
		float wt=0;
		for(int i = 0; i < nf; ++i) {
		    View ll = vg.getChildAt(i);
		    pm = ll.getLayoutParams();
		    if (pm.height == 0 || pm.width == 0) {
		    	wt += ((LinearLayout.LayoutParams)pm).weight;
		    }
		}
		return wt;
	}
	
	private int calcolaWidthLibera(View v, int w) {
		ViewGroup vg = (ViewGroup)v;
		if (vg instanceof LinearLayout && ((LinearLayout)vg).getOrientation()==LinearLayout.HORIZONTAL) {
			LayoutParams pm;
			int nf = vg.getChildCount();
			for(int i = 0; i < nf; ++i) {
			    View ll = vg.getChildAt(i);
			    pm = ll.getLayoutParams();
			    if (pm.width > 0) {
			    	w -= pm.width;
			    }
			    w -= ((LinearLayout.LayoutParams)pm).leftMargin;
			    w -= ((LinearLayout.LayoutParams)pm).rightMargin;
			}
		}
		return w;
	}
	
	private int calcolaHeightLibera(View v, int h) {
		ViewGroup vg = (ViewGroup)v;
		if (vg instanceof LinearLayout && ((LinearLayout)vg).getOrientation()==LinearLayout.VERTICAL) {
			LayoutParams pm;
			int nf = vg.getChildCount();
			for(int i = 0; i < nf; ++i) {
			    View ll = vg.getChildAt(i);
			    pm = ll.getLayoutParams();
			    if (pm.height > 0) {
			    	h -= pm.height;
			    }
			    h -= ((LinearLayout.LayoutParams)pm).topMargin;
			    h -= ((LinearLayout.LayoutParams)pm).bottomMargin;
			}
		}
		return h;
	}
	
	private int getHeightScreen() {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.y;
	}
	
	private int getWidthScreen() {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.x;
	}
	
	private int getHeightView(View view) {
		if (view.getId() == R.id.ads){
			int pp = getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin);
			return (getHeightScreen() - pp);
		} else {
			int hh = view.getLayoutParams().height;
			if (hh > 0) {
				return hh;
			} else {
				View parent = (View)(view.getParent());
				int parentHeight = getHeightView(parent);
				int h = calcolaHeightLibera(parent, parentHeight);
				float wt = ((LinearLayout.LayoutParams)view.getLayoutParams()).weight;
				if (wt > 0 && hh == 0) {
					float sw = calcolaSommaWeight(parent);
					h = (int)((wt / sw) * (float)h);
				}
				return h;
			}
		}		
	}
	
	private void setWidthView(View v, int w) {
		LayoutParams pm = v.getLayoutParams();
		pm.width = w;
		v.setLayoutParams(pm);
	}
	
	private void resizeLayout() {
        View ll = findViewById(R.id.bot_centrale);
        setWidthView(ll, getHeightView(ll));
	}

    @Override
    public void onImagesChosen(ChosenImages images) {

    }

    @Override
    public void onImageChosen(final ChosenImage image) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (image != null) {
                    File imageFile;

                    if (previousImage.length() > 0) {
                        imageFile = new File(previousImage);

                        if (imageFile.exists()) {
                            try {
                                imageFile.delete();
                            } catch (Exception e) { }
                        }
                    }

                    previousImage = image.getFileThumbnail();

                    imageView.setImageURI(Uri.fromFile(new File(previousImage)));
                    imageView.resetZoom();

                    hasImage = true;

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    editor.putString("preview_previous_image", previousImage);
                    editor.commit();

                    imageFile = new File(image.getFilePathOriginal());
                    if (imageFile.exists()) {
                        try {
                            imageFile.delete();
                        } catch (Exception e) { }
                    }

                    imageFile = new File(image.getFileThumbnailSmall());
                    if (imageFile.exists()) {
                        try {
                            imageFile.delete();
                        } catch (Exception e) { }
                    }

                    // Use the image
                    // image.getFilePathOriginal();
                    // image.getFileThumbnail();
                    // image.getFileThumbnailSmall();
                }
            }
        });
    }

    @Override
    public void onError(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Show error message
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (previewPanel.isShown()) {
            ((ImageButton)findViewById(R.id.preview_close)).performClick();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (previewPanel.isShown()) {
                ((ImageButton)findViewById(R.id.preview_close)).performClick();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

}
