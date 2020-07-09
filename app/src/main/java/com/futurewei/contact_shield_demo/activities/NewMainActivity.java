package com.futurewei.contact_shield_demo.activities;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.futurewei.contact_shield_demo.BackgroundContactCheckingIntentService;
import com.futurewei.contact_shield_demo.R;
import com.futurewei.contact_shield_demo.fragments.fragment_faq;
import com.futurewei.contact_shield_demo.fragments.fragment_home;
import com.futurewei.contact_shield_demo.fragments.fragment_setting;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.contactshield.ContactShield;
import com.huawei.hms.contactshield.ContactShieldSetting;

import java.util.ArrayList;
import java.util.List;

public class NewMainActivity extends AppCompatActivity {

    private Fragment fragment_home;
    private Fragment fragment_faq;
    private Fragment fragment_settings;
    private FragmentManager fm;
    private FragmentTransaction ft;
    private int page = 0;
    private final int REQUEST_LOCATION = 1;
    private static final int PERMISSION_REQUESTS = 1;

    SharedPreferences sharedPreferences;
    private static final String TAG = "NewMainActivity";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    ft = fm.beginTransaction();
                    ft.replace(R.id.frame, fragment_home);
                    ft.commit();
                    page = 0;
                    return true;
                case R.id.navigation_faq:
                    ft = fm.beginTransaction();
                    ft.replace(R.id.frame, fragment_faq);
                    ft.commit();
                    page = 1;
                    return true;
                case R.id.navigation_settings:
                    ft = fm.beginTransaction();
                    ft.replace(R.id.frame, fragment_settings);
                    ft.commit();
                    page = 2;
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
//        boolean is_app_disabled = sharedPreferences.getBoolean("is_app_disabled", false);
        boolean is_app_disabled = false;

        if(!is_app_disabled)
            engine_start_pre_check();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.new_main_activity);

        fm = getFragmentManager();
        ft = fm.beginTransaction();

        fragment_home = new fragment_home();
        fragment_faq = new fragment_faq();
        fragment_settings = new fragment_setting();
        ft.replace(R.id.frame, fragment_home);
        ft.commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean is_app_disabled = sharedPreferences.getBoolean("is_app_disabled", false);

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
        //If not disabled, start the engine
//        if(!is_app_disabled){
//            Log.d(TAG, "onCreate >> !is_app_disabled");
//            engine_start_pre_check();
//        }
//        engine_start();



    }

    void engine_start_pre_check(){
        Log.d(TAG, "engine_start_pre_check");
        Task<Boolean> isRunningTask = ContactShield.getContactShieldEngine(this).isContactShieldRunning();
        isRunningTask.addOnSuccessListener(aBoolean -> {
            if(!aBoolean){
                engine_start();
                Log.e(TAG, "isContactShieldRunning >> NO");
            }else{
                Log.e(TAG, "isContactShieldRunning >> YES");
            }
        });
    }

    void engine_start(){
        Log.d(TAG, "engine_start");
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, new Intent(getApplicationContext(), BackgroundContactCheckingIntentService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);


        ContactShield.getContactShieldEngine(this).startContactShield(pendingIntent, ContactShieldSetting.DEFAULT)
                .addOnSuccessListener(aVoid -> Log.e(TAG, "startContactShield >> Success"))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Log.e(TAG, "startContactShield >> Failure");
                });


    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }
        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return new String[0];
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_REQUESTS) {
            return;
        }
        boolean isNeedShowDiag = false;
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                // If the storage permission is not authorized, the authorization dialog box is displayed.
                isNeedShowDiag = true;
            }
        }
//        if (isNeedShowDiag && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
//            AlertDialog dialog = new AlertDialog.Builder(this)
//                    .setMessage(getString(R.string.camera_permission_rationale))
//                    .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                            intent.setData(Uri.parse("package:" + getPackageName())); // Open the configuration page based on the package name.
//                            startActivityForResult(intent, 200);
//                            startActivity(intent);
//                        }
//                    })
//                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            finish();
//                        }
//                    }).create();
//            dialog.show();
//        }
    }

}