package com.zhihu.matisse.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import com.zhihu.matisse.edit.core.IMGMode;
import com.zhihu.matisse.edit.core.IMGPath;
import com.zhihu.matisse.edit.core.IMGText;
import com.zhihu.matisse.edit.core.file.IMGAssetFileDecoder;
import com.zhihu.matisse.edit.core.file.IMGDecoder;
import com.zhihu.matisse.edit.core.file.IMGContentDecoder;
import com.zhihu.matisse.edit.core.file.IMGFileDecoder;
import com.zhihu.matisse.edit.core.util.IMGUtils;
import com.zhihu.matisse.internal.utils.PathUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created by felix on 2017/11/14 下午2:26.
 */

public class IMGEditActivity extends IMGEditBaseActivity {

    private static final int MAX_WIDTH = 1024;

    private static final int MAX_HEIGHT = 1024;

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";

    public static final String EXTRA_IMAGE_SAVE_PATH = "IMAGE_SAVE_PATH";

    public static final String ACTION_RELOAD_FRAGMENT = "ACTION_RELOAD_FRAGMENT";

    private Uri imageUri;

    public static void startForResult(Activity activity, Uri imageUri, int requestCode) {
        String savePath = createSavePath(activity, imageUri);
        Intent intent = new Intent(activity, IMGEditActivity.class).putExtra(IMGEditActivity.EXTRA_IMAGE_URI, imageUri)
                                                                   .putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, savePath);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startForResult(Fragment fragment, Uri imageUri, int requestCode) {
        String savePath = createSavePath(fragment.getContext(), imageUri);
        Intent intent = new Intent(fragment.getContext(), IMGEditActivity.class).putExtra(IMGEditActivity.EXTRA_IMAGE_URI, imageUri)
                                                                                .putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, savePath);
        fragment.startActivityForResult(intent, requestCode);
    }

    private static String createSavePath(Context context, Uri uri) {
        String path = PathUtils.getPath(context, uri);
        File originalFile = new File(path);
        String name = originalFile.getName();
        int indexDot = name.lastIndexOf(".");
        if (indexDot > 0) {
            name = name.substring(0, indexDot);
        }
        int additionTagIndex = 1;
        File newImageFile = new File(originalFile.getParent(), name + "(" + additionTagIndex + ").jpg");
        while (newImageFile.exists()) {
            additionTagIndex++;
            newImageFile = new File(originalFile.getParent(), name + "(" + additionTagIndex + ").jpg");
        }
        return newImageFile.getAbsolutePath();
    }

    @Override
    public void onCreated() {

    }

    @Override
    public Bitmap getBitmap() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
        if (imageUri == null) {
            return null;
        }

        IMGDecoder decoder = null;

        String path = imageUri.getPath();
        if (!TextUtils.isEmpty(path)) {
            switch (imageUri.getScheme()) {
                case "asset":
                    decoder = new IMGAssetFileDecoder(this, imageUri);
                    break;
                case "file":
                    decoder = new IMGFileDecoder(imageUri);
                    break;
                case "content":
                    decoder = new IMGContentDecoder(this, imageUri);
                    break;
            }
        }

        if (decoder == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;

        decoder.decode(options);

        if (options.outWidth > MAX_WIDTH) {
            options.inSampleSize = IMGUtils.inSampleSize(Math.round(1f * options.outWidth / MAX_WIDTH));
        }

        if (options.outHeight > MAX_HEIGHT) {
            options.inSampleSize = Math.max(options.inSampleSize, IMGUtils.inSampleSize(Math.round(1f * options.outHeight / MAX_HEIGHT)));
        }

        options.inJustDecodeBounds = false;

        Bitmap bitmap = decoder.decode(options);
        if (bitmap == null) {
            return null;
        }

        IMGPath.baseDoodleWidth = bitmap.getWidth() / 80f;

        return bitmap;
    }

    @Override
    public void onText(IMGText text) {
        mImgView.addStickerText(text);
    }

    @Override
    public void onModeClick(IMGMode mode) {
        IMGMode cm = mImgView.getMode();
        if (cm == mode) {
            mode = IMGMode.NONE;
        }
        mImgView.setMode(mode);
        updateModeUI();

        if (mode == IMGMode.CLIP) {
            setOpDisplay(OP_CLIP);
        }
    }

    @Override
    public void onUndoClick() {
        IMGMode mode = mImgView.getMode();
        if (mode == IMGMode.DOODLE) {
            mImgView.undoDoodle();
        } else if (mode == IMGMode.MOSAIC) {
            mImgView.undoMosaic();
        }
    }

    @Override
    public void onCancelClick() {
        finish();
    }

    @Override
    public void onDoneClick() {
        String path = getIntent().getStringExtra(EXTRA_IMAGE_SAVE_PATH);
        if (!TextUtils.isEmpty(path)) {
            Bitmap bitmap = mImgView.saveBitmap();
            if (bitmap != null) {
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fout);
                    Intent broadcast = new Intent(ACTION_RELOAD_FRAGMENT).putExtra(EXTRA_IMAGE_SAVE_PATH, path).putExtra(EXTRA_IMAGE_URI, imageUri);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
                    setResult(RESULT_OK, broadcast);
                    finish();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    setResult(RESULT_CANCELED);
                    finish();
                } finally {
                    if (fout != null) {
                        try {
                            fout.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return;
            }
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onCancelClipClick() {
        mImgView.cancelClip();
        setOpDisplay(mImgView.getMode() == IMGMode.CLIP ? OP_CLIP : OP_NORMAL);
    }

    @Override
    public void onDoneClipClick() {
        mImgView.doClip();
        setOpDisplay(mImgView.getMode() == IMGMode.CLIP ? OP_CLIP : OP_NORMAL);
    }

    @Override
    public void onResetClipClick() {
        mImgView.resetClip();
    }

    @Override
    public void onRotateClipClick() {
        mImgView.doRotate();
    }

    @Override
    public void onColorChanged(int checkedColor) {
        mImgView.setPenColor(checkedColor);
    }
}
