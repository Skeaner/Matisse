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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zhihu.matisse.R;
import com.zhihu.matisse.edit.IMGEditActivity;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter;
import com.zhihu.matisse.internal.ui.widget.CheckRadioView;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.ui.widget.IncapableDialog;
import com.zhihu.matisse.internal.utils.PathUtils;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;
import com.zhihu.matisse.internal.utils.Platform;
import com.zhihu.matisse.listener.OnFragmentInteractionListener;

import java.io.File;

public abstract class BasePreviewActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener, OnFragmentInteractionListener {

    public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
    public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
    public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    public static final String CHECK_STATE = "checkState";

    private static final int REQUEST_CODE_EDIT = 99;

    protected final SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
    protected SelectionSpec mSpec;
    protected ViewPager mPager;

    protected PreviewPagerAdapter mAdapter;

    protected CheckView mCheckView;
    protected TextView mButtonBack;
    protected TextView mButtonApply;
    protected TextView mButtonEdit;
    protected TextView mSize;

    protected int mPreviousPos = -1;

    private LinearLayout mOriginalLayout;
    private CheckRadioView mOriginal;
    protected boolean mOriginalEnable;

    private FrameLayout mBottomToolbar;
    private FrameLayout mTopToolbar;
    private boolean mIsToolbarHide = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(SelectionSpec.getInstance().themeId);
        super.onCreate(savedInstanceState);
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_media_preview);
        if (Platform.hasKitKat()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mSpec = SelectionSpec.getInstance();
        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
            mOriginalEnable = getIntent().getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false);
        } else {
            mSelectedCollection.onCreate(savedInstanceState);
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        mButtonBack = (TextView) findViewById(R.id.button_back);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mButtonEdit = findViewById(R.id.button_edit);
        mSize = (TextView) findViewById(R.id.size);
        mButtonBack.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);
        mButtonEdit.setOnClickListener(this);
        if (mSpec.editable) {
            mButtonEdit.setVisibility(View.VISIBLE);
        }
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);
        mAdapter = new PreviewPagerAdapter(getSupportFragmentManager(), null);
        mPager.setAdapter(mAdapter);
        mCheckView = (CheckView) findViewById(R.id.check_view);
        mCheckView.setCountable(mSpec.countable);
        mBottomToolbar = findViewById(R.id.bottom_toolbar);
        mTopToolbar = findViewById(R.id.top_toolbar);

        mCheckView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Item item = mAdapter.getMediaItem(mPager.getCurrentItem());
                if (mSelectedCollection.isSelected(item)) {
                    mSelectedCollection.remove(item);
                    if (mSpec.countable) {
                        mCheckView.setCheckedNum(CheckView.UNCHECKED);
                    } else {
                        mCheckView.setChecked(false);
                    }
                } else {
                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item);
                        if (mSpec.countable) {
                            mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                        } else {
                            mCheckView.setChecked(true);
                        }
                    }
                }
                updateApplyButton();

                if (mSpec.onSelectedListener != null) {
                    mSpec.onSelectedListener.onSelected(mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
                }
            }
        });

        mOriginalLayout = findViewById(R.id.originalLayout);
        mOriginal = findViewById(R.id.original);
        mOriginalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int count = countOverMaxSize();
                if (count > 0) {
                    IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                                                                                  getString(R.string.error_over_original_count,
                                                                                            count,
                                                                                            mSpec.originalMaxSize));
                    incapableDialog.show(getSupportFragmentManager(), IncapableDialog.class.getName());
                    return;
                }

                mOriginalEnable = !mOriginalEnable;
                mOriginal.setChecked(mOriginalEnable);
                if (!mOriginalEnable) {
                    mOriginal.setColor(Color.WHITE);
                }

                if (mSpec.onCheckedListener != null) {
                    mSpec.onCheckedListener.onCheck(mOriginalEnable);
                }
            }
        });

        updateApplyButton();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT && resultCode == RESULT_OK) {
            //取消选中旧的
            Item oldItem = mAdapter.getMediaItem(mPager.getCurrentItem());
            if (mSelectedCollection.isSelected(oldItem)) {
                mSelectedCollection.remove(oldItem);
                if (mSpec.countable) {
                    mCheckView.setCheckedNum(CheckView.UNCHECKED);
                } else {
                    mCheckView.setChecked(false);
                }
            }
//            if (mSpec.onSelectedListener != null) {
//                mSpec.onSelectedListener.onSelected(mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
//            }
            sendBackResult(false);
            finish();
//            //选中新的
//            String path = data.getStringExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH);
//            Uri uri = FileProvider.getUriForFile(this, mSpec.captureStrategy.authority, new File(path));
//            Item newItem = Item.valueOf(uri);
//            mSelectedCollection.add(newItem);
//            mAdapter.add(0, newItem);
//            mAdapter.notifyDataSetChanged();
//            mPager.setCurrentItem(0);
//            if (mSpec.countable) {
//                mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(newItem));
//            } else {
//                mCheckView.setChecked(true);
//            }
//
//            updateApplyButton();

//            if (mSpec.onSelectedListener != null) {
//                mSpec.onSelectedListener.onSelected(mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
//            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mSelectedCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        sendBackResult(false);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_back) {
            onBackPressed();
        } else if (v.getId() == R.id.button_apply) {
            sendBackResult(true);
            finish();
        } else if (v.getId() == R.id.button_edit) {
            Item item = mAdapter.getMediaItem(mPager.getCurrentItem());
            if (!item.isImage()) {
                Toast.makeText(this, "只支持编辑图片", Toast.LENGTH_SHORT).show();
            } else {
                IMGEditActivity.startForResult(this, item.uri, REQUEST_CODE_EDIT);
            }
        }
    }

    @Override
    public void onClick() {
        if (!mSpec.autoHideToobar) {
            return;
        }

        if (mIsToolbarHide) {
            mTopToolbar.animate().setInterpolator(new FastOutSlowInInterpolator()).translationYBy(mTopToolbar.getMeasuredHeight()).start();
            mBottomToolbar.animate()
                          .translationYBy(-mBottomToolbar.getMeasuredHeight())
                          .setInterpolator(new FastOutSlowInInterpolator())
                          .start();
        } else {
            mTopToolbar.animate().setInterpolator(new FastOutSlowInInterpolator()).translationYBy(-mTopToolbar.getMeasuredHeight()).start();
            mBottomToolbar.animate()
                          .setInterpolator(new FastOutSlowInInterpolator())
                          .translationYBy(mBottomToolbar.getMeasuredHeight())
                          .start();
        }

        mIsToolbarHide = !mIsToolbarHide;

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((PreviewItemFragment) adapter.instantiateItem(mPager, mPreviousPos)).resetView();

            Item item = adapter.getMediaItem(position);
            if (mSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                mCheckView.setCheckedNum(checkedNum);
                if (checkedNum > 0) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            } else {
                boolean checked = mSelectedCollection.isSelected(item);
                mCheckView.setChecked(checked);
                if (checked) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            }
            updateSize(item);
        }
        mPreviousPos = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void updateApplyButton() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonApply.setText(R.string.button_sure_default);
            mButtonApply.setEnabled(false);
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonApply.setText(R.string.button_sure_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.button_sure, selectedCount));
        }

        if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mOriginalLayout.setVisibility(View.GONE);
        }
    }

    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (!mOriginalEnable) {
            mOriginal.setColor(Color.WHITE);
        }

        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                                                                              getString(R.string.error_over_original_size,
                                                                                        mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(), IncapableDialog.class.getName());

                mOriginal.setChecked(false);
                mOriginal.setColor(Color.WHITE);
                mOriginalEnable = false;
            }
        }
    }

    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);
            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void updateSize(Item item) {
        if (item.isGif()) {
            mSize.setVisibility(View.VISIBLE);
            mSize.setText(PhotoMetadataUtils.getSizeInMB(item.size) + "M");
        } else {
            mSize.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            mOriginalLayout.setVisibility(View.GONE);
        } else if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        setResult(Activity.RESULT_OK, intent);
    }

    private boolean assertAddSelection(Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(this, cause);
        return cause == null;
    }
}
