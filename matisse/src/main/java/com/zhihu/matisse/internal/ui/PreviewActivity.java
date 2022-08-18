/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui;

import android.os.Bundle;
import android.view.View;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.SelectedItemCollection;

import java.util.EnumSet;
import java.util.List;

import androidx.annotation.Nullable;

public class PreviewActivity extends BasePreviewActivity {

    public static final String EXTRA_IMAGES = "images";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!SelectionSpec.getInstance().hasInited) {
            Matisse.from(this).choose(EnumSet.of(MimeType.JPEG)).countable(true);
        }
        super.onCreate(savedInstanceState);
        List<Item> selected = getIntent().getParcelableArrayListExtra(EXTRA_IMAGES);
        mAdapter.addAll(selected);
        mAdapter.notifyDataSetChanged();
        mCheckView.setCheckedNum(1);
        mCheckView.setClickable(false);
        mSelectedCollection.setDefaultSelection(selected);
        mButtonApply.setVisibility(View.GONE);
        mPreviousPos = 0;
        updateSize(selected.get(0));
    }

}
