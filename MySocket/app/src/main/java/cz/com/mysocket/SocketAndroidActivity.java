package cz.com.mysocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;


import android.view.View.OnClickListener;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class SocketAndroidActivity extends Activity {
    public static final int UPDATA = 1;
    public static Handler handler;
    public TextView mTextView = null;
    public TextView Texttips = null;
    public EditText editIP;
    public EditText editport;
    private Button btnip,btnsendmsg;
    private String ipaddre = null;
    private int portlang = 0;
    private ConnectThread conthread = null;
    private Timer mainTimer;
    private Message MainMsg;
    final int TX_DATA_UPDATE_UI = 3;
    final int READ_ALL_INFO =4;
    byte SendQueBuf[] = { 0x3A, 0x00, 0x01, 0x0A, 0x00, 0x00, 0x23, 0x00 };
    public static byte Node[]= new byte[32];
    private String strTemp;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    static final int CONNCTION_OK = 8;
    static final int CONNCTION_ERROR = 9;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mian);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        initcontrol();

        inihandle();
    }
    class ButtonClick implements OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_ip:
                    showipdialog(SocketAndroidActivity.this);
                    break;
            }
            }
        }

    private void showipdialog(Context context){
        View view= getLayoutInflater().inflate(R.layout.dialog,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("输入IP地址和端口号");
        builder.setView(view);
        editIP = (EditText)view.findViewById(R.id.ip1);
        editport = (EditText)view.findViewById(R.id.port1);
        boolean isRemember = pref.getBoolean("remember_passip", false);
        if(isRemember){
            String accountip = pref.getString("accountip", "");
            String portnum = pref.getString("portnum", "");
            editIP.setText(accountip);
            editport.setText(portnum);
        }
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor =pref.edit();
                editor.putBoolean("remember_passip", true);
                editor.putString("accountip", editIP.getText().toString());
                editor.putString("portnum", editport.getText().toString());
                editor.commit();
                ipaddre = editIP.getText().toString();
                portlang = Integer.parseInt(editport.getText().toString());
                conthread = new ConnectThread(ipaddre, portlang);
                conthread.start();
                mainTimer = new Timer();
                Log.d("AAAA", "到这里");// 定时查询所有终端信息
                //setTimerTask();
            }

        });
        builder.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                    }
                });
        builder.show();
    }
    private void setTimerTask() {
        mainTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (conthread != null) {
                    MainMsg = handler.obtainMessage(TX_DATA_UPDATE_UI,
                            READ_ALL_INFO, 0xFF);
                    handler.sendMessage(MainMsg);
                }
            }
        }, 500, 1000);// 表示500毫秒之后，每隔1000毫秒执行一次
    }

    private void initcontrol(){
        btnip = (Button) findViewById(R.id.btn_ip);
        btnip.setOnClickListener(new ButtonClick());
        btnsendmsg = (Button) findViewById(R.id.btn_sendmsg);
        btnsendmsg.setOnClickListener(new ButtonClick());
        mTextView = (TextView) findViewById(R.id.TextView01);
        Texttips = (TextView) findViewById(R.id.tips);
        Texttips.setText("请先连接服务器");

    }
    public void SendData(byte buffer[], int len) {
        MainMsg = ConnectThread.childHandler.obtainMessage(ConnectThread.TX_DATA,
                len, 0, (Object) buffer);
        ConnectThread.childHandler.sendMessage(MainMsg);

    }
    public void inihandle(){
        handler = new Handler(){
            public void handleMessage(Message msg){
                switch (msg.what){
                    case UPDATA:
                        strTemp = "wendu" + Node[0]+"ye"+Node[1]+"ewe"+Node[2];
                        //String str = (String) msg.obj;
                        mTextView.setText(strTemp);
                        break;
                    case TX_DATA_UPDATE_UI: // msg.arg1保存功能码 arg2保存终端地址
                        switch (msg.arg1) {
                            case READ_ALL_INFO:
                                SendQueBuf[2] = (byte) msg.arg2;// 0xFF;
                                SendQueBuf[3] = 0x01; // FC
                                SendQueBuf[4] = (byte) 0xC4;
                                SendQueBuf[5] = (byte) 0x23;

                                SendData(SendQueBuf, 6); // 查询所有终端报文3A 00 FF 01 C4 23
                                break;
                        }
                        break;
                    case CONNCTION_OK:
                        String str = (String) msg.obj;
                        Texttips.setText(str);
                        mainTimer = new Timer();
                        setTimerTask();
                        break;
                    case CONNCTION_ERROR:
                        String strd = (String) msg.obj;
                        Texttips.setText(strd);
                        break;
                }
                super.handleMessage(msg);
            }
        };

    }
}