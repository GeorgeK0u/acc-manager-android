package com.example.accmanager.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.accmanager.MainActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
// Web scraping
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
// Socket
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client
{
    private static InputStream in;
    private static OutputStream out;
    private static Socket conn;
    private static final String CLOSE_CONN = "close_conn", MANUAL_SYNC = "manual_sync", MANUAL_SYNC_END = "manual_sync_end";
    public static final String SYNC_BC = "sync_broadcast";
    private static Handler handler;
    private static boolean manualSyncInProgress;
    private static Gson gson;

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void SyncAcc(String op, ArrayList<Object> Msg)
    {
        handler.post(() ->
        {
            switch (op) {
                case "C":
                    // convert object arr to string arr
                    ArrayList<String> EncAccDetails = new ArrayList<>();
                    Msg.forEach((MsgPart) -> EncAccDetails.add(String.valueOf(MsgPart)));
                    XmlHandler.SaveAcc(EncAccDetails);
                    break;

                case "U":
                    XmlHandler.UpdateAcc(Msg);
                    break;

                case "D":
                    String encAccName = String.valueOf(Msg.remove(0));
                    XmlHandler.DelAcc(encAccName);
                    break;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void StartConn()
    {
        // rotating the device calls MainActivity onDestroy
        if (conn != null)
        {
            return;
        }
        // main loop handler
        handler = new Handler(Looper.getMainLooper());
        gson = new GsonBuilder().setLenient().create();
        try
        {
			String publicIp = "";
			try
			{
				Document doc = Jsoup.connect("https://api.ipify.org").get();
				publicIp = doc.body().text();
			}
			catch (Exception e)
			{
				System.out.println("Failed to get public IP address. Most likely no internet connection");
				return;
			}
			String serverPublicIP = "";
			try
			{
				serverPublicIP = InetAddress.getByName("my-ddns.ddns.net").getHostAddress();
			}
			catch (Exception e)
			{
				System.out.println("Failed to find No-IP DNS");
				return;
			}
            // due to NAT loopback I cannot connect if server is on same LAN as device with public ip
            // determine if connection is local or remote
			String host = "";
			if (publicIp.equals(serverPublicIP))
			{
				// server private ip
				host = "192.168.2.105";
			}
			else
			{
				host = serverPublicIP;
			}
            int port = 56789;
            conn = new Socket();
            // wait a max of 5 secs to connect
            conn.connect(new InetSocketAddress(host, port), 5*1000);
            // update conn status from main thread
            if (ViewHandler.MainActivityRef != null)
            {
                handler.post(() -> {
                    ViewHandler.MainActivityRef.SetConnStatus(MainActivity.CLIENT_CONNECTED);
                });
                System.out.println("Connected!");
            }
            // get input and output streams
            in = conn.getInputStream();
            out = conn.getOutputStream();
            // Receive messages loop
            boolean open = true;
            while (open)
            {
                try
                {
                    byte[] inputStream = new byte[1024];
                    final int msgByteSize = in.read(inputStream);
                    String recvMsg = new String(inputStream, 0, msgByteSize, StandardCharsets.UTF_8);
                    if (recvMsg.equals(CLOSE_CONN))
                    {
                        open = false;
                    }
                    else if (recvMsg.contains(SYNC_BC) || recvMsg.contains(MANUAL_SYNC))
                    {
                        if (!recvMsg.contains(MANUAL_SYNC_END))
                        {
                            if (recvMsg.contains(SYNC_BC))
                            {
                                System.out.println("Broadcast sync msg received from server: " + recvMsg);
                            }
                            else
                            {
                                System.out.println("Manual sync msg received from server: " + recvMsg);
                            }
                            ArrayList<Object> Msg = gson.fromJson(recvMsg, ArrayList.class);
                            // Remove sync call from msg
                            Msg.remove(0);
                            // Get operator
                            String op = String.valueOf(Msg.remove(0));
                            // sync
                            SyncAcc(op, Msg);
                        }
                        // manual sync completed
                        else
                        {
                            System.out.println("Manual sync completed");
                            if (ViewHandler.MainActivityRef != null)
                            {
                                handler.post(() -> {
                                        Toast.makeText(ViewHandler.MainActivityRef.getApplicationContext(), "Manual sync completed", Toast.LENGTH_LONG).show();
                                });
                            }
                            manualSyncInProgress = false;
                        }
                    }
                    else
                    {
                        System.out.println("Server says " + recvMsg);
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Exception: " + e);
                    // TODO Close server side conn when turning off wifi
                    open = false;
                }
            }
            // close socket
            conn.close();
            conn = null;
            System.out.println("Socket closed from client side");
        }
        catch (Exception e)
        {
            System.out.println("An exception occurred: " + e + ". Non-updated ddns hostname ip");
        }
        finally
        {
            // update conn status from main thread
            if (ViewHandler.MainActivityRef != null)
            {
                handler.post(() -> {
                    ViewHandler.MainActivityRef.SetConnStatus(MainActivity.CLIENT_NOT_CONNECTED);
                });
                System.out.println("Not connected");
            }
        }
    }

    public static boolean IsConnected()
    {
        return conn != null;
    }

    @SuppressLint("NewApi")
    public static int SendMsg(String msgStr, boolean manualSync)
    {
        if (!manualSync)
        {
            // send msg on different thread
            new Thread(() ->
            {
                try
                {
                    byte[] msg = msgStr.getBytes(StandardCharsets.UTF_8);
                    out.write(msg);
                    out.flush();
                    // close conn msg
                    if (msgStr.equals(CLOSE_CONN))
                    {
                        System.out.println("Sent close signal to server");
                    }
                    // sync broadcast msg
                    else if (msgStr.contains(SYNC_BC))
                    {
                        // convert msg from string to object
                        ArrayList<Object> Msg = gson.fromJson(msgStr, ArrayList.class);
                        // remove sync part
                        Msg.remove(0);
                        // remove operation part
                        String op = (String)Msg.remove(0);
                        // handle this device broadcast msg locally
                        SyncAcc(op, Msg);
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Client failed to send msg: " + msgStr + " to server. Exception: " + e);
                }
            }).start();
            return 0;
        }
        else
        {
            try
            {
                byte[] msg = msgStr.getBytes(StandardCharsets.UTF_8);
                out.write(msg);
                out.flush();
                return 1;
            }
            catch (Exception e)
            {
                System.out.println("Manual sync msg failed to be sent to server. Exception: " + e);
                return -1;
            }
        }
    }

    public static void SendManualSyncMsg()
    {
        new Thread(() ->
        {
            if (conn == null || manualSyncInProgress)
            {
                if (ViewHandler.MainActivityRef != null)
                {
                    handler.post(() -> {
                        if (conn == null)
                        {
                            Toast.makeText(ViewHandler.MainActivityRef.getApplicationContext(), "Failed to start manual sync progress. Not connected", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(ViewHandler.MainActivityRef.getApplicationContext(), "A manual sync is already in progress", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                return;
            }
            // update manual sync in progress status
            manualSyncInProgress = true;
            // send all accs of this device to server
            ArrayList<ArrayList<String>> EncAccs = XmlHandler.GetAccs(false);
            for (ArrayList<String> EncAcc : EncAccs)
            {
                EncAcc.add(0, MANUAL_SYNC);
                String encAccJsonString = gson.toJson(EncAcc);
                boolean ok = SendMsg(encAccJsonString, true) == 1;
                // failed to send manual sync msg
                if (!ok)
                {
                    System.out.println("Manual sync stopped. Connection with server got lost");
                    if (ViewHandler.MainActivityRef != null)
                    {
                        handler.post(() -> {
                            Toast.makeText(ViewHandler.MainActivityRef.getApplicationContext(), "Manual sync stopped. Connection with server got lost", Toast.LENGTH_LONG).show();
                        });
                    }
                    manualSyncInProgress = false;
                    return;
                }
                // Sleep for 100 ms
                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                    System.out.println("Failed to sleep manual sync thread");
                }
            }
            // Signal end of client part
            System.out.println("Manual sync client part completed");
            boolean ok = SendMsg(MANUAL_SYNC_END, true) == 1;
            if (!ok)
            {
                System.out.println("Failed to signal manual sync end of client part");
                if (ViewHandler.MainActivityRef != null)
                {
                    handler.post(() -> {
                        Toast.makeText(ViewHandler.MainActivityRef.getApplicationContext(), "Failed to signal manual sync end of client part", Toast.LENGTH_LONG).show();
                    });
                }
                manualSyncInProgress = false;
            }
        }).start();
    }
    public static boolean GetManualSyncInProgress()
    {
        return manualSyncInProgress;
    }
    public static void SendBroadcastMsg(String msg)
    {
        SendMsg(msg, false);
    }
    public static void SendCloseSocketMsg()
    {
        SendMsg(CLOSE_CONN, false);
    }
}
