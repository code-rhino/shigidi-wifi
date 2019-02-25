package com.shigidi.network.wifimgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.stream.Stream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WifiMgr {

    private final FileInputStream serviceAccount;
    private final DatabaseReference database;
    private String machineName;

    public WifiMgr(){
        this.serviceAccount = initializeServiceAccount();
        this.database = initializeFirebase();
        String filePath = System.getProperty("user.home")+ "/identity.txt";
        machineName = readLineByLineJava8( filePath );
        machineName = machineName.replace("\n","");
        System.out.println("the machine name " + machineName);
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
        System.out.println("Writing");
        return "";
    }


    public String wirelessIp(){
        try {
            StringBuilder sb = new StringBuilder();
            String address = "";
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // drop inactive
                if (!networkInterface.isUp())
                    continue;

                // smth we can explore
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if(networkInterface.getDisplayName().equals("en0") || networkInterface.getDisplayName().equals("wlan0")) {
                    System.out.println(String.format("NetInterface: name [%s], ip [%s]",
                            networkInterface.getDisplayName(), addr.getHostAddress()));
                    address = String.format("%s:%s ",networkInterface.getDisplayName(), addr.getHostAddress());
                    }
                }

            }
            return address;
        }catch (SocketException ex){

        }
        return "no connection";
    }

    public void begin(){
        while (true) {
            if (WifiMgr.netIsAvailable()) {
                try {
                    System.out.println("Test "+wirelessIp());
                    InetAddress inetAddress = InetAddress.getLocalHost();
                    MachineInfo machineInfo = new MachineInfo();
                    machineInfo.ipAddress = wirelessIp();
                    String stuff = machineName;
                    update(stuff, new FirebaseDataEntity(machineInfo));
                    System.out.println(machineName);
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

    private static String readLineByLineJava8(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    public static void main(String args[]){
        WifiMgr wifiMgr = new WifiMgr();
        System.out.println( System.getProperty("user.home"));
        wifiMgr.begin();
    }
}
