package com.kimjunu.littlemozart.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Util {

    public static boolean checkPermission(Context context) {
        ArrayList<String> permissionList = new ArrayList<>();

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.RECORD_AUDIO);

        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!permissionList.isEmpty()) {
            String[] permissionArray = new String[permissionList.size()];
            permissionArray = permissionList.toArray(permissionArray);

            Activity activity = (Activity) context;
            activity.requestPermissions(permissionArray, 1000);

            return false;
        }

        return true;
    }

    public static String encodeFileToBase64(String filepath) {
        File file = new File(filepath);
        String fileString = "";
        FileInputStream inputStream;
        ByteArrayOutputStream byteOutStream;

        try {
            inputStream = new FileInputStream(file);
            byteOutStream = new ByteArrayOutputStream();

            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = inputStream.read(buf)) != -1) {
                byteOutStream.write(buf, 0, len);
            }

            byte[] fileArray = byteOutStream.toByteArray();

            fileString = Base64.encodeToString(fileArray, Base64.DEFAULT);

            inputStream.close();
            byteOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileString;
    }

    public static boolean decodeBase64ToFile(String base64, String filePath) {
        boolean isSucceeded = false;

        byte[] data = Base64.decode(base64, Base64.DEFAULT);

        try {
            File file = new File(filePath);

            if (file.exists())
                file.delete();

            FileOutputStream output = new FileOutputStream(file);

            output.write(data);
            output.close();

            isSucceeded = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isSucceeded;
    }
}
