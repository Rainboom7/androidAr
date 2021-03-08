package com.example.myapplication;

import android.content.Context;
import android.net.Uri;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ArNode extends AnchorNode {
    private AugmentedImage image;
    private CompletableFuture<ModelRenderable> renderableCompletableFuture;
    private Node node;
    private Anchor anchor;

    public ArNode(Context context, int modelId) {
        renderableCompletableFuture = ModelRenderable.builder()
                .setRegistryId("model")
                .setSource(context, modelId)
                .build();
    }

    @Override
    public String toString() {
        return "ArNode{" +
                "image=" + image.getName() +
                ", node=" + node +
                "image_method=" + image.getTrackingMethod() +

                '}';
    }

    public Node getNode() {
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArNode other = (ArNode) o;
        return image.getName().equals(((ArNode) o).getImage().getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(image.getName());
    }

    public void setImage(final AugmentedImage image) {
        this.image = image;
        if (!renderableCompletableFuture.isDone()) {
            CompletableFuture.allOf(renderableCompletableFuture)
                    .thenAccept((Void v) -> setImage(image)).exceptionally(throwable -> {
                return null;
            });
        }
        anchor = image.createAnchor(image.getCenterPose());
        setAnchor(anchor);

        node = new Node();
        Pose pose = Pose.makeTranslation(0, 0, 0);
        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setLocalRotation(new Quaternion((float) (pose.qx() - 3 * Math.PI / 2), pose.qy(), pose.qz(), pose.qw()));
        node.setLocalScale(new Vector3(0.4f, 0.2f, 0.1f));
        node.setRenderable(renderableCompletableFuture.getNow(null));
    }

    public void rotate(float dx, float dy) {
        if (node != null) {
            Quaternion rotationX= new Quaternion(Vector3.right(),dx);
            Quaternion rotationY = new Quaternion(Vector3.up(),dy);
            Quaternion rotated = Quaternion.multiply(node.getLocalRotation(),rotationX);
            rotated=Quaternion.multiply(rotated,rotationY);
            Quaternion localRotation = node.getLocalRotation();
            node.setLocalRotation(rotated);

        }
    }

    public void detach() {
        node.setParent(null);
        node.setRenderable(null);
    }

    public AugmentedImage getImage() {
        return image;
    }
}
