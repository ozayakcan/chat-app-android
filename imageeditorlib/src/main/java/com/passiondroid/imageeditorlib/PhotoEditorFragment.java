package com.passiondroid.imageeditorlib;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.passiondroid.imageeditorlib.filter.ProcessingImage;
import com.passiondroid.imageeditorlib.utils.Matrix3;
import com.passiondroid.imageeditorlib.utils.Utility;
import com.passiondroid.imageeditorlib.views.PhotoEditorView;
import com.passiondroid.imageeditorlib.views.VerticalSlideColorPicker;
import com.passiondroid.imageeditorlib.views.ViewTouchListener;
import com.passiondroid.imageeditorlib.views.imagezoom.ImageViewTouch;

public class PhotoEditorFragment extends BaseFragment
    implements View.OnClickListener, ViewTouchListener {

  ImageViewTouch mainImageView;
  TextView resetButton;
  ImageView stickerButton;
  ImageView addTextButton;
  PhotoEditorView photoEditorView;
  ImageView paintButton;
  ImageView deleteButton;
  VerticalSlideColorPicker colorPickerView;
  //CustomPaintView paintEditView;
  View toolbarLayout;
  FloatingActionButton doneBtn;
  private Bitmap mainBitmap;
  private LruCache<Integer, Bitmap> cacheStack;
  private OnFragmentInteractionListener mListener;
  public static final int MODE_NONE = 0;
  public static final int MODE_PAINT = 1;
  public static final int MODE_ADD_TEXT = 2;
  public static final int MODE_STICKER = 3;

  protected int currentMode;
  private Bitmap originalBitmap;

  private boolean imageLoaded = false;

  public static PhotoEditorFragment newInstance(String imagePath) {
    Bundle bundle = new Bundle();
    bundle.putString(ImageEditor.EXTRA_IMAGE_PATH, imagePath);
    PhotoEditorFragment photoEditorFragment = new PhotoEditorFragment();
    photoEditorFragment.setArguments(bundle);
    return photoEditorFragment;
  }

  public PhotoEditorFragment() {

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_photo_editor, container, false);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(
          context.toString() + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }



  public interface OnFragmentInteractionListener {

    void onDoneClicked(String imagePath);
  }

  public void setImageBitmap(Bitmap bitmap) {
    mainImageView.setImageBitmap(bitmap);
    mainImageView.post(new Runnable() {
      @Override
      public void run() {
        photoEditorView.setBounds(mainImageView.getBitmapRect());
        imageLoaded = true;
      }
    });
  }

  public void reset(){
    photoEditorView.reset();
  }

  protected void initView(View view) {
    mainImageView = view.findViewById(R.id.image_iv);
    resetButton = view.findViewById(R.id.reset_btn);
    stickerButton = view.findViewById(R.id.stickers_btn);
    addTextButton = view.findViewById(R.id.add_text_btn);
    deleteButton = view.findViewById(R.id.delete_view);
    photoEditorView = view.findViewById(R.id.photo_editor_view);
    paintButton = view.findViewById(R.id.paint_btn);
    colorPickerView = view.findViewById(R.id.color_picker_view);
    //paintEditView = findViewById(R.id.paint_edit_view);
    toolbarLayout = view.findViewById(R.id.toolbar_layout);
    doneBtn = view.findViewById(R.id.done_btn);

    if (getArguments() != null && getActivity()!=null && getActivity().getIntent()!=null) {
      final String imagePath = getArguments().getString(ImageEditor.EXTRA_IMAGE_PATH);
      //mainImageView.post(new Runnable() {
      //  @Override public void run() {
      //    mainBitmap = Utility.decodeBitmap(imagePath,mainImageView.getWidth(),mainImageView.getHeight());
      //
      //  }
      //});
      /*Picasso.get().load(imagePath).placeholder(R.drawable.circle).into(new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
          int currentBitmapWidth = bitmap.getWidth();
          int currentBitmapHeight = bitmap.getHeight();
          int ivWidth = mainImageView.getWidth();
          int newHeight = (int) Math.floor(
                  (double) currentBitmapHeight * ((double) ivWidth / (double) currentBitmapWidth));
          originalBitmap = Bitmap.createScaledBitmap(bitmap, ivWidth, newHeight, true);
          mainBitmap = originalBitmap;
          setImageBitmap(mainBitmap);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
      });*/
      Glide.with(this).asBitmap().load(imagePath).into(new SimpleTarget<Bitmap>() {
        @Override
        public void onResourceReady(@NonNull Bitmap resource,
                                    @Nullable Transition<? super Bitmap> transition) {
          int currentBitmapWidth = resource.getWidth();
          int currentBitmapHeight = resource.getHeight();
          int ivWidth = mainImageView.getWidth();
          int newHeight = (int) Math.floor(
              (double) currentBitmapHeight * ((double) ivWidth / (double) currentBitmapWidth));
          originalBitmap = Bitmap.createScaledBitmap(resource, ivWidth, newHeight, true);
          mainBitmap = originalBitmap;
          setImageBitmap(mainBitmap);

        }
      });


      Intent intent = getActivity().getIntent();
      setVisibility(addTextButton,intent.getBooleanExtra(ImageEditor.EXTRA_IS_TEXT_MODE, false));
      setVisibility(stickerButton,intent.getBooleanExtra(ImageEditor.EXTRA_IS_STICKER_MODE, false));
      setVisibility(paintButton,intent.getBooleanExtra(ImageEditor.EXTRA_IS_PAINT_MODE, false));


      photoEditorView.setImageView(mainImageView, deleteButton, this);
      resetButton.setOnClickListener(this);
      stickerButton.setOnClickListener(this);
      addTextButton.setOnClickListener(this);
      paintButton.setOnClickListener(this);
      doneBtn.setOnClickListener(this);
      view.findViewById(R.id.back_iv).setOnClickListener(this);

      colorPickerView.setOnColorChangeListener(
          new VerticalSlideColorPicker.OnColorChangeListener() {
            @Override
            public void onColorChange(int selectedColor) {
              if (currentMode == MODE_PAINT) {
                paintButton.setBackground(
                    Utility.tintDrawable(getContext(), R.drawable.circle, selectedColor));
                photoEditorView.setColor(selectedColor);
              } else if (currentMode == MODE_ADD_TEXT) {
                addTextButton.setBackground(
                    Utility.tintDrawable(getContext(), R.drawable.circle, selectedColor));
                photoEditorView.setTextColor(selectedColor);
              }
            }
          });
      photoEditorView.setColor(colorPickerView.getDefaultColor());
      photoEditorView.setTextColor(colorPickerView.getDefaultColor());
    }
  }

  protected void onModeChanged(int currentMode) {
    Log.i(ImageEditActivity.class.getSimpleName(), "CM: " + currentMode);
    onStickerMode(currentMode == MODE_STICKER);
    onAddTextMode(currentMode == MODE_ADD_TEXT);
    onPaintMode(currentMode == MODE_PAINT);

    if (currentMode == MODE_PAINT || currentMode == MODE_ADD_TEXT) {
      AnimationHelper.animate(getContext(), colorPickerView, R.anim.slide_in_right, View.VISIBLE,
          null);
    } else {
      AnimationHelper.animate(getContext(), colorPickerView, R.anim.slide_out_right, View.INVISIBLE,
          null);
    }
  }

  @Override
  public void onClick(final View view) {
    int id = view.getId();

    if (id == R.id.reset_btn) {
      if (imageLoaded) {
        ResetAlertDialog();
      }else{
        ErrorMsg(view.getContext());
      }
    } else if (id == R.id.stickers_btn) {
      if (imageLoaded) {
        setMode(MODE_STICKER);
      }else{
        ErrorMsg(view.getContext());
      }
    } else if (id == R.id.add_text_btn) {
      if (imageLoaded) {
        setMode(MODE_ADD_TEXT);
      }else{
        ErrorMsg(view.getContext());
      }
    } else if (id == R.id.paint_btn) {
      if (imageLoaded) {
        setMode(MODE_PAINT);
      }else{
        ErrorMsg(view.getContext());
      }
    } else if (id == R.id.back_iv) {
      getActivity().onBackPressed();
    }else if (id == R.id.done_btn) {
      if (imageLoaded) {
        new ProcessingImage(getBitmapCache(mainBitmap), Utility.getCacheFilePath(view.getContext()),
                data -> mListener.onDoneClicked(data)).executeAsync();
      }else{
        ErrorMsg(view.getContext());
      }
    }

    if (currentMode != MODE_NONE) {
      mainImageView.animate().scaleX(1f);
      photoEditorView.animate().scaleX(1f);
      mainImageView.animate().scaleY(1f);
      photoEditorView.animate().scaleY(1f);
      //touchView.setVisibility(View.GONE);
    }
  }

  private void ResetAlertDialog() {
    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
    builder.setCancelable(true);
    builder.setTitle(R.string.reset);
    builder.setMessage(R.string.are_you_sure_you_want_to_reset_changes);
    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
      reset();
      dialog.dismiss();
    });
    builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  private void ErrorMsg(Context context) {
    Toast.makeText(context, context.getString(R.string.please_wait_for_the_image_to_load), Toast.LENGTH_SHORT).show();
  }

  private void onAddTextMode(boolean status) {
    if (status) {
      addTextButton.setBackground(
          Utility.tintDrawable(getContext(), R.drawable.circle, photoEditorView.getColor()));
      //photoEditorView.setTextColor(photoEditorView.getColor());
      photoEditorView.addText();
    } else {
      addTextButton.setBackground(null);
      photoEditorView.hideTextMode();
    }
  }

  private void onPaintMode(boolean status) {
    if (status) {
      paintButton.setBackground(
          Utility.tintDrawable(getContext(), R.drawable.circle, photoEditorView.getColor()));
      photoEditorView.showPaintView();
      //paintEditView.setVisibility(View.VISIBLE);
    } else {
      paintButton.setBackground(null);
      photoEditorView.hidePaintView();
      //photoEditorView.enableTouch(true);
      //paintEditView.setVisibility(View.GONE);
    }
  }

  private void onStickerMode(boolean status) {
    if (status) {
      stickerButton.setBackground(
          Utility.tintDrawable(getContext(), R.drawable.circle, photoEditorView.getColor()));
      if(getActivity()!=null && getActivity().getIntent()!=null) {
        String folderName = getActivity().getIntent().getStringExtra(ImageEditor.EXTRA_STICKER_FOLDER_NAME);
        photoEditorView.showStickers(folderName);
      }
    } else {
      stickerButton.setBackground(null);
      photoEditorView.hideStickers();
    }
  }

  @Override
  public void onStartViewChangeListener(final View view) {
    Log.i(ImageEditActivity.class.getSimpleName(), "onStartViewChangeListener" + "" + view.getId());
    toolbarLayout.setVisibility(View.GONE);
    AnimationHelper.animate(getContext(), deleteButton, R.anim.fade_in_medium, View.VISIBLE, null);
  }

  @Override
  public void onStopViewChangeListener(View view) {
    Log.i(ImageEditActivity.class.getSimpleName(), "onStopViewChangeListener" + "" + view.getId());
    deleteButton.setVisibility(View.GONE);
    AnimationHelper.animate(getContext(), toolbarLayout, R.anim.fade_in_medium, View.VISIBLE, null);
  }

  private Bitmap getBitmapCache(Bitmap bitmap) {
    Matrix touchMatrix = mainImageView.getImageViewMatrix();

    Bitmap resultBit = Bitmap.createBitmap(bitmap).copy(Bitmap.Config.ARGB_8888, true);
    Canvas canvas = new Canvas(resultBit);

    float[] data = new float[9];
    touchMatrix.getValues(data);
    Matrix3 cal = new Matrix3(data);
    Matrix3 inverseMatrix = cal.inverseMatrix();
    Matrix m = new Matrix();
    m.setValues(inverseMatrix.getValues());

    float[] f = new float[9];
    m.getValues(f);
    int dx = (int) f[Matrix.MTRANS_X];
    int dy = (int) f[Matrix.MTRANS_Y];
    float scale_x = f[Matrix.MSCALE_X];
    float scale_y = f[Matrix.MSCALE_Y];
    canvas.save();
    canvas.translate(dx, dy);
    canvas.scale(scale_x, scale_y);

    photoEditorView.setDrawingCacheEnabled(true);
    if (photoEditorView.getDrawingCache() != null) {
      canvas.drawBitmap(photoEditorView.getDrawingCache(), 0, 0, null);
    }

    if (photoEditorView.getPaintBit() != null) {
      canvas.drawBitmap(photoEditorView.getPaintBit(), 0, 0, null);
    }

    canvas.restore();
    return resultBit;
  }

  protected void setMode(int mode) {
    if (currentMode != mode) {
      onModeChanged(mode);
    } else {
      mode = MODE_NONE;
      onModeChanged(mode);
    }
    this.currentMode = mode;
  }
}
