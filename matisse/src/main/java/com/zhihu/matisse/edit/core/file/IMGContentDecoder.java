package com.zhihu.matisse.edit.core.file;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import com.zhihu.matisse.internal.utils.PathUtils;

import java.io.File;

/**
 * Created by felix on 2017/12/26 下午3:07.
 */

public class IMGContentDecoder extends IMGDecoder {
    private Context context;

    public IMGContentDecoder(Context context, Uri uri) {
        super(uri);
        this.context = context;
    }

    @Override
    public Bitmap decode(BitmapFactory.Options options) {
        Uri uri = getUri();
        if (uri == null) {
            return null;
        }

        String path =  PathUtils.getPath(context, uri);
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        File file = new File(path);
        if (file.exists()) {
            return BitmapFactory.decodeFile(path, options);
        }

        return null;
    }
}
