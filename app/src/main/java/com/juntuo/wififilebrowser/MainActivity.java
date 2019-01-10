package com.juntuo.wififilebrowser;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class MainActivity extends AppCompatActivity {

    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String PATH_ZIP = Environment.getExternalStorageDirectory().getPath() + "/DCIM/projectZip/";

    private AsyncHttpServer server = null;
    private AsyncServer mAsyncServer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdirs();
        }

        final File file1 = new File(PATH_ZIP);
        if (!file1.exists()) {
            file1.mkdirs();
        }

        startWebService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
        if (mAsyncServer != null) {
            mAsyncServer.stop();
        }
    }

    private void startWebService() {

        server = new AsyncHttpServer();
        mAsyncServer = new AsyncServer();

        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    response.send(getIndexContent());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(500).end();
                }
            }
        });

        server.get("/jquery-1.7.2.min.js", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    String fullPath = request.getPath();
                    fullPath = fullPath.replace("%20", " ");
                    String resourceName = fullPath;
                    if (resourceName.startsWith("/")) {
                        resourceName = resourceName.substring(1);
                    }
                    if (resourceName.indexOf("?") > 0) {
                        resourceName = resourceName.substring(0, resourceName.indexOf("?"));
                    }
                    response.setContentType("application/javascript");
                    BufferedInputStream bInputStream = new BufferedInputStream(getAssets().open(resourceName));
                    response.sendStream(bInputStream, bInputStream.available());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(404).end();
                    return;
                }
            }
        });

        server.get("/files/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath().replace("/files/", "");

                try {
                    path = URLDecoder.decode(path, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        response.sendStream(fis, fis.available());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                response.code(404).send("Not found!");
            }
        });

        server.get("/download/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath().replace("/download/", "");

                try {
                    path = URLDecoder.decode(path, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String[] split = path.split(",");

                if (split.length == 0) {
                    response.send("请选择下载照片");
                    return;
                }

                File[] files = new File[split.length];

                for (int i = 0; i < files.length; i++) {
                    files[i] = new File(split[i]);
                }

                String zipFileName = System.currentTimeMillis() + ".zip";
                String url = PATH_ZIP + zipFileName;

                boolean b = ZipHelper.zipFiles(files, url);
                if (b) {

                    JSONArray array = new JSONArray();
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("name", url);
                        array.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    response.send(array.toString());
                } else {
                    response.code(404).send("Not found!");
                }

            }
        });

        server.get("/files", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONArray array = new JSONArray();

                File dir = new File(PATH);

                String[] fileNames = dir.list();
                if (fileNames != null) {
                    for (String fileName : fileNames) {
                        File file = new File(dir, fileName);
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("name", fileName);
                            jsonObject.put("dir", file.isDirectory() ? "1" : "0");
                            jsonObject.put("path", file.getAbsolutePath());
                            array.put(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                response.send(array.toString());
            }
        });

        server.get("/nextDoc/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                String path = request.getPath().replace("/nextDoc/", "");

                try {
                    path = URLDecoder.decode(path, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                File dir = new File(path);

                if (dir.isDirectory()) {
                    JSONArray array = new JSONArray();
                    try {
                        JSONObject hOject = new JSONObject();
                        hOject.put("name", "->返回上级");
                        hOject.put("dir", "1");
                        hOject.put("path", new File(path).getParentFile().getAbsolutePath());
                        array.put(hOject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String[] fileNames = dir.list();
                    if (fileNames != null) {
                        for (String fileName : fileNames) {
                            File file = new File(dir, fileName);
                            try {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("name", fileName);
                                jsonObject.put("dir", file.isDirectory() ? "1" : "0");
                                jsonObject.put("path", file.getAbsolutePath());
                                array.put(jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    response.send(array.toString());
                }
            }
        });

        server.listen(mAsyncServer, 54321);

        ((TextView) findViewById(R.id.tv_addr)).setText("访问地址: http://" + getLocalIpAddress() + ":54321");

        new MaterialDialog.Builder(this)
                .title("手机服务已创建成功")
                .content("请使用电脑浏览器访问以下地址:\nhttp://" + getLocalIpAddress() + ":54321")
                .positiveText("好的")
                .show();

    }

    private String getIndexContent() throws IOException {
        BufferedInputStream bInputStream = null;
        try {
            bInputStream = new BufferedInputStream(getAssets().open("index1.html"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] tmp = new byte[10240];
            while ((len = bInputStream.read(tmp)) > 0) {
                baos.write(tmp, 0, len);
            }
            return new String(baos.toByteArray(), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (bInputStream != null) {
                try {
                    bInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getLocalIpAddress() {
        int paramInt = ((WifiManager) getApplicationContext().getSystemService(Activity.WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
