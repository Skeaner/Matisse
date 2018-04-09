package com.zhihu.matisse.internal.utils;

import android.net.Uri;

import com.zhihu.matisse.internal.entity.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by NeXT on 2018/4/9.
 */

public class ItemUtils {

    public static List<Item> toItemList(List<Uri> uriList) {
        List<Item> items = new ArrayList<>();
        if (uriList == null) {
            return items;
        }
        for (Uri uri : uriList) {
            items.add(Item.valueOf(uri));
        }
        return items;
    }
}
