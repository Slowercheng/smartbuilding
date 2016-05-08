package cz.com.mysocket;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


/**
 * Created by Administrator on 2016/5/4.
 */
public class ConnectThread extends Thread{
    private String ipaddr = null;
    private Socket socket;
    private int portadd = 0;
    private SocketAddress socketAddress;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private RxThread rxthread;
    Message messagev;
    private boolean RXflag = true;
    public static Handler childHandler;
    static final int TX_DATA = 5;
    static final int RX_EXIT = 6;
    int ucRecvLen = 7;

    public  ConnectThread(String ipaddress , int portlang ){
        ipaddr = ipaddress;
        portadd = portlang;
    }
    public void connect(){
        socketAddress = new InetSocketAddress(ipaddr, portadd);
        socket = new Socket();
        //String message = "here is a message";
        try {
            socket.connect(socketAddress, portadd);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            messagev = SocketAndroidActivity.handler.obtainMessage(SocketAndroidActivity.CONNCTION_OK, "已连接服务器");
            SocketAndroidActivity.handler.sendMessage(messagev);
            rxthread = new RxThread();
            rxthread.start();

            /*PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
            out.println(message);

           接收来自服务器的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msgtext = br.readLine();

            if ( msgtext != null )
            {
                messagev = SocketAndroidActivity.handler.obtainMessage(SocketAndroidActivity.UPDATA , msgtext );
                SocketAndroidActivity.handler.sendMessage(messagev);
            }
            else
            {
                messagev = SocketAndroidActivity.handler.obtainMessage(SocketAndroidActivity.UPDATA , "数据错误" );
                SocketAndroidActivity.handler.sendMessage(messagev);;
            }
            //关闭流
            out.close();
            br.close();
            //关闭Socket
            socket.close();*/
        }catch (IOException e) {
            try {
                sleep(10);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            messagev = SocketAndroidActivity.handler.obtainMessage(SocketAndroidActivity.CONNCTION_ERROR, "无法连接服务器");
            SocketAndroidActivity.handler.sendMessage(messagev);
            try{
                Log.d("asdasd","dasda");
                socket.close();
            }catch (Exception e2){
                e2.printStackTrace();
            }
            e.printStackTrace();
        }catch (NumberFormatException e) {
        }
    }
    public void run(){
        connect();
        initChildHandler();
    }
    void initChildHandler() {

        Looper.prepare(); // 在子线程中创建Handler必须初始化Looper

        childHandler = new Handler() {public void handleMessage(Message msg) {

            // 接收主线程及其他线程的消息并处理...
            switch (msg.what) {
                case TX_DATA:
                    int len = msg.arg1;

                    try {
                        if (len != 7) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        outputStream.write((byte[]) msg.obj, 0, len);
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case RX_EXIT:
                    RXflag = false;
                    try {
                        if (socket.isConnected()) {
                            inputStream.close();
                            outputStream.close();
                            socket.close();
                        }

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    childHandler.getLooper().quit();// 结束消息队列

                    break;

                default:
                    break;
            }

        }
        };

        // 启动该线程的消息队列
        Looper.loop();

        }

    public class RxThread extends Thread{
       public void run(){
           try {
               while (socket.isConnected() && RXflag) {
                   byte strRxbuf[] = new byte[32];
                   int len, readBytes = 0;
                   while (readBytes < ucRecvLen) {
                       len = inputStream.read(strRxbuf, readBytes, ucRecvLen
                               - readBytes);

                       if (strRxbuf[0] == 0x3A && len >= 4) {
                           ucRecvLen = 22;
                           readBytes += len;
                       } else {
                           strRxbuf[0] = 0;
                           readBytes = 0;
                       }
                       if (len == -1)
                           break;
                   }

                   if (strRxbuf[ucRecvLen - 1] == 0x23) {
                       if (ucRecvLen == 22) {
                           System.arraycopy(strRxbuf, 0, SocketAndroidActivity.Node, 0, 4);

                           messagev = SocketAndroidActivity.handler.obtainMessage(SocketAndroidActivity.UPDATA, strRxbuf);
                           SocketAndroidActivity.handler.sendMessage(messagev);
                           // RefreshData();
                           // OnReceive(ClientSock[0]); //andy
                           len = readBytes = 0;
                           strRxbuf[0] = 0;
                           ucRecvLen = 7;
                       } else {
                           len = readBytes = 0;
                           strRxbuf[0] = 0;
                           ucRecvLen = 7;
                       }
                   }else {
                       len =readBytes= 0;
                       strRxbuf[0] = 0;
                       ucRecvLen = 7;
                   }

                   /*if (index > 100) {
                       index = 0;
                       len =readBytes= 0;
                       strRxbuf[0] = 0;
                       ucRecvLen = 7;
                   }*/
                   //SocketAndroidActivity.Node[0] = strRxbuf[0];
                   //System.arraycopy(strRxbuf,0, SocketAndroidActivity.Node, 0, 4);
                   //String strRx = new String(strRxbuf,"UTF-8");
                   //messagev = SocketAndroidActivity.handler.obtainMessage(SocketAndroidActivity.UPDATA , strRxbuf);
                   //SocketAndroidActivity.handler.sendMessage(messagev);
               }
               if (socket.isConnected())
                   socket.close();
           }catch (IOException e) {
               e.printStackTrace();
           }
       }
    }

}
