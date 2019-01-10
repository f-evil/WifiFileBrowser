package com.juntuo.wififilebrowser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (AndPermission.hasPermissions(
                SplashActivity.this,
                Permission.Group.STORAGE)) {
            checkPermission2();
        } else {
            new MaterialDialog.Builder(SplashActivity.this)
                    .title("必要权限申请")
                    .content("没有权限,应用将无法运行")
                    .positiveText("去申请")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            checkPermission2();
                        }
                    }).show();
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            checkPermission2();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> per) {
        Log.e("onPermissionsGranted", requestCode + "");
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d("onPermissionsDenied", requestCode + ":" + perms.size());
    }

    private void checkPermission2() {
        AndPermission.with(SplashActivity.this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        new MaterialDialog.Builder(SplashActivity.this)
                                .title("警告")
                                .content("必要权限申请被拒绝,您将无法使用该应用!")
                                .positiveText("去申请")
                                .negativeText("离开")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        new AppSettingsDialog.Builder(SplashActivity.this).build().show();
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        android.os.Process.killProcess(Process.myPid());
                                    }
                                }).show();

                    }
                })
                .start();
    }
}
