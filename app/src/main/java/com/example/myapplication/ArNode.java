package com.example.myapplication;

import android.content.Context;
import android.net.Uri;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;

public class ArNode extends AnchorNode {
    private AugmentedImage image;
    private static CompletableFuture<ModelRenderable> renderableCompletableFuture;
    private Node node;

    public ArNode(Context context, int modelId) {
        if (renderableCompletableFuture == null)
            renderableCompletableFuture = ModelRenderable.builder()
                    .setRegistryId("model")
                    .setSource(context, modelId)
                    .build();
    }

    public void setImage(final AugmentedImage image) {
        this.image = image;
        if (!renderableCompletableFuture.isDone()) {
            CompletableFuture.allOf(renderableCompletableFuture)
                    .thenAccept((Void v) -> setImage(image)).exceptionally(throwable -> {
                return null;
            });
        }
        setAnchor(image.createAnchor(image.getCenterPose()));

        node = new Node();
        Pose pose = Pose.makeTranslation(0, 0, 0);
        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setLocalRotation(new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
        node.setLocalScale(new Vector3(0.1f,01.f,0.1f));
        node.setRenderable(renderableCompletableFuture.getNow(null));
    }
    public void detach(){
        node.setParent(null);

    }

    public AugmentedImage getImage() {
        return image;
    }
}
