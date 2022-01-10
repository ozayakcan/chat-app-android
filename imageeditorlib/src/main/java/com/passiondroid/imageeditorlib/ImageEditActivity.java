package com.passiondroid.imageeditorlib;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.passiondroid.imageeditorlib.utils.FragmentUtil;

import static com.passiondroid.imageeditorlib.ImageEditor.EXTRA_IMAGE_PATH;
import static com.passiondroid.imageeditorlib.ImageEditor.EXTRA_SAVE_PATH;

public class ImageEditActivity extends BaseImageEditActivity
    implements PhotoEditorFragment.OnFragmentInteractionListener,
    CropFragment.OnFragmentInteractionListener {
  private Rect cropRect;
  private String oldImagePath;
  private String savePath;
  //private View touchView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_image_edit);

    oldImagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
    savePath = getIntent().getStringExtra(EXTRA_SAVE_PATH);
    if (oldImagePath != null) {
      FragmentUtil.addFragment(this, R.id.fragment_container,
          PhotoEditorFragment.newInstance(oldImagePath, savePath));
    }
  }

  @Override
  public void onCropClicked(Bitmap bitmap) {
    FragmentUtil.replaceFragment(this, R.id.fragment_container,
        CropFragment.newInstance(bitmap, cropRect));
  }

  @Override
  public void onDoneClicked(String imagePath) {

    Intent intent = new Intent();
    intent.putExtra(ImageEditor.EXTRA_EDITED_PATH, imagePath);
    if (oldImagePath != null){
      intent.putExtra(ImageEditor.EXTRA_IMAGE_PATH, oldImagePath);
    }
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  @Override
  public void onImageCropped(Bitmap bitmap, Rect cropRect) {
    this.cropRect = cropRect;
    PhotoEditorFragment photoEditorFragment =
        (PhotoEditorFragment) FragmentUtil.getFragmentByTag(this,
            PhotoEditorFragment.class.getSimpleName());
    if (photoEditorFragment != null) {
      photoEditorFragment.setImageWithRect(cropRect);
      photoEditorFragment.reset();
      FragmentUtil.removeFragment(this,
          (BaseFragment) FragmentUtil.getFragmentByTag(this, CropFragment.class.getSimpleName()));
    }
  }

  @Override
  public void onCancelCrop() {
    FragmentUtil.removeFragment(this,
        (BaseFragment) FragmentUtil.getFragmentByTag(this, CropFragment.class.getSimpleName()));
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }
}