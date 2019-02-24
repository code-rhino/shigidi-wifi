package com.shigidi.network.wifimgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WifiMgr {

    private final FileInputStream serviceAccount;
    private final DatabaseReference database;
    private  String mac;

    public WifiMgr(){
        this.serviceAccount = initializeServiceAccount();
        this.database = initializeFirebase();
        this.mac = getMAC();

    }

    private FileInputStream initializeServiceAccount() {

        URL path = null;
        try {
            String homedir = System.getProperty("user.home");
            System.out.println(homedir);
            path = new File(homedir + "/pi-car-ae948-firebase-adminsdk-2mzpv-373a4b9faf.json").toURI().toURL();

        } catch (MalformedURLException ex){
            System.out.println("Bad url" + path.toString());
        }
        try {
            return new FileInputStream(path.getPath());
        } catch (FileNotFoundException e) {

            throw new Error(e);
        }

    }
    private DatabaseReference initializeFirebase() {
        FirebaseOptions options;
        try {
            options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://pi-car-ae948.firebaseio.com")
                    .build();
        } catch (IOException e) {
            String errorMessage = "Failed to fetch google stream from `%s`";
            //logger.error(String.format(errorMessage, serviceAccount.getChannel().toString()));
            throw new Error(e);
        }
        FirebaseApp.initializeApp(options);
        return FirebaseDatabase.getInstance().getReference("wifi");
    }

    public String write(FirebaseDataEntity dataToBeWritten) {
        DatabaseReference ref = database.push(); // push node to onto list
        ref.setValueAsync(dataToBeWritten.toString());
        String key = ref.getKey();
        System.out.println(key);
        //logger.info(String.format("Value written to database `%s`", key));
        return key;
    }

    public String update(String hostname , FirebaseDataEntity dataToBeUpdated){
        DatabaseReference ref = database.child(hostname);
        ref.setValueAsync(dataToBeUpdated.toString());
        return "";
    }

    private static String getMAC(){
        String mac_address = "Unknown";
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            mac_address = sb.toString();
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        } catch (SocketException ex){
            ex.printStackTrace();
        }
        return mac_address;
    }

    public void begin(){
        while (true) {
            if (WifiMgr.netIsAvailable()) {
                try {
                    InetAddress inetAddress = InetAddress.getLocalHost();
                    MachineInfo machineInfo = new MachineInfo();
                    machineInfo.ipAddress = inetAddress.getHostAddress();
                    update(mac, new FirebaseDataEntity(machineInfo));
                    System.out.println("IP Address:- " + inetAddress.getHostAddress());
                    System.out.println("Host Name:- " + inetAddress.getHostName());
                } catch (UnknownHostException ex) {
                    System.out.println("Error");
                }
                try {
                    Thread.sleep(10000l);
                } catch (InterruptedException ex){

                }
            }
        }
    }

    private static boolean netIsAvailable() {
        try {
            final URL url = new URL("http://www.google.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String args[]){
        WifiMgr wifiMgr = new WifiMgr();
        System.out.println( System.getProperty("user.home"));
        wifiMgr.begin();
    }
}
