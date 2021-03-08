package com.example.myapplication;


import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@RequiresApi(api = 30)
public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private ArSceneView arSceneView;
    private Session session;
    private boolean shouldConfigureSession = false;
    private ConcurrentHashMap<String, Integer> imagesModel = new ConcurrentHashMap<>();
    private ImageSetterUtil imageSetterUtil = new ImageSetterUtil();
    private static String DEFAULT_SUFFIX = "_qrcode.png";
    private static String DEFAULT_PEFIX = "qr/";
    private static Float ACCEPTABLE_DISTANCE = 0.1f;
    private volatile ArNode trackingNode = null;
    private float prev_x = 0;
    private float prev_y = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        imagesModel = new ConcurrentHashMap<>(imageSetterUtil.getModelsMap());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //View
        arSceneView = (ArSceneView) findViewById(R.id.arView);
        //Request permission
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this,
                                "Permission required to display camera",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
        initSceneView();
    }

    private void initSceneView() {
        arSceneView.getScene().addOnUpdateListener(this);
    }

    private void setupSession() {
        if (session == null) {
            try {
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                e.printStackTrace();
            } catch (UnavailableApkTooOldException e) {
                e.printStackTrace();
            } catch (UnavailableSdkTooOldException e) {
                e.printStackTrace();
            } catch (UnavailableDeviceNotCompatibleException e) {
                e.printStackTrace();
            }
            shouldConfigureSession = true;

        }
        if (shouldConfigureSession) {
            configureSession();
            shouldConfigureSession = false;
            arSceneView.setupSession(session);
        }
        try {
            session.resume();
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
            session = null;
            return;
        }

    }

    private void configureSession() {
        Config config = new Config(session);
        if (!buildDatabase(config)) {
            Toast.makeText(this, "Error in db", Toast.LENGTH_SHORT).show();
            ;

        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
    }

    private boolean buildDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        for (String imageName : imagesModel.keySet()) {
            Bitmap bitmap = loadImage(imageName);
            if (bitmap == null)
                return false;
            augmentedImageDatabase.addImage(imageName, bitmap);
        }
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Bitmap loadImage(String imageName) {
        try {
            String name = DEFAULT_PEFIX + imageName + DEFAULT_SUFFIX;
            InputStream is = getAssets().open(name);
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            applyRotation(x - prev_x, y - prev_y);
            prev_x=x;
            prev_y=y;
        }
        return true;
    }

    private void applyRotation(float x, float y) {
        if (trackingNode != null)
            trackingNode.rotate(x, y);
    }


    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();
        Collection<AugmentedImage> updateAugmeneteImg =
                frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage img : updateAugmeneteImg
        ) {
            if (img.getTrackingState() == TrackingState.TRACKING && trackingNode == null) {
                if (img.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING)
                    setModel(img);
            } else if (img.getTrackingMethod() == AugmentedImage.TrackingMethod.LAST_KNOWN_POSE ||
                    img.getTrackingMethod() == AugmentedImage.TrackingMethod.NOT_TRACKING) {
                if (trackingNode != null) {
                    removeModel(img);
                }
            }
        }

    }


    private void removeModel(AugmentedImage augmentedImage) {
        if (trackingNode.getImage().getName().equals(augmentedImage.getName())) {
            arSceneView.getScene().removeChild(trackingNode);
            trackingNode.detach();
            trackingNode = null;
        }
    }


    private void setModel(AugmentedImage image) {
        System.out.println("caught:" + image.getName());
        String imgName = image.getName();
        if (imagesModel.containsKey(imgName)) {
            System.out.println("modelId:" + imagesModel.get(imgName));
            ArNode arNode = new ArNode(this, imagesModel.get(imgName));

            arNode.setImage(image);
            if (!arSceneView.getScene().getChildren().contains(arNode)) {
                System.out.println("adding:" + arNode.getImage().getName());
                arSceneView.getScene().addChild(arNode);

            }
            trackingNode = arNode;
            System.out.println(arNode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this,
                                "Permission required to display camera",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (session != null) {
            arSceneView.pause();
            session.pause();
        }

    }


}
