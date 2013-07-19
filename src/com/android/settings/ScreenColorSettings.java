/*
   Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.PopupWindow;


public class ScreenColorSettings extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private final static String TAG = "ScreenColorSettings";

    private static final int RESTORE_DEFAULT_PREVIEW = 0;
    private static final int SELECT_NEW_FILE_ITEM = 1;
    private static final int SELECT_FILE_ITEM = 2;
    private static final int RESET = 3;
    private static final int SELECT_FILE_GROUP1 = 0;
    private static final int SELECT_FILE_GROUP2 = 1;
    private static final int SELECT_FILE_ORDER = 0;
    private static final int REQUEST_SELECT_FILE = 100;
    private static final String IMAGE_UNSPECIFIED = "image/*";
    private static final String GALLERY_CLASSNAME = "com.android.gallery3d.app.Wallpaper";
    private static final String GALLERY_PACKAGENAME = "com.android.gallery3d";
    private static final String STRING_KEY ="screencolor_preview_key";
    private static final String STRING_NAME ="screencolor_preview_name";

    private static final String KEY_ASPECT_X = "aspectX";
    private static final String KEY_ASPECT_Y = "aspectY";
    private static final String KEY_SPOTLIGHT_X = "spotlightX";
    private static final String KEY_SPOTLIGHT_Y = "spotlightY";
    private static final String KEY_FROME_SCREENCOLOR = "fromScreenColor";
    private static final int ASPECT_X = 480;
    private static final int ASPECT_Y = 800;
    private static final float SPOTLIGHT_X = 0;
    private static final float SPOTLIGHT_Y = 0;

    private ImageView mImageView;
    private RelativeLayout mRLayout;
    private LinearLayout mScreenColorLayout;
    private SeekBar mHBar, mSBar, mCBar, mIBar;
    private TextView mHTv, mSTv, mCTv, mITv;
    private ImageView mReduceH, mIncreaseH, mReduceS, mIncreaseS, mReduceC,
            mIncreaseC, mReduceI, mIncreaseI, mUpdown, mMore;
    private Button mCancelBtn, mSaveBtn, mPreviousBtn, mNewBtn;
    private boolean canRestorePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.screencolor_settings);
        initView();
    }

    private void initView(){
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mRLayout = (RelativeLayout)findViewById(R.id.background_preview);
        String previewContent = getSharePreferences();
        if("".equals(previewContent)){
            resotreBackgroundByDefault();
        }else{
            Uri previewUri = Uri.parse(previewContent);
            setBackgroundByUri(previewUri);
        }

        mScreenColorLayout = (LinearLayout)findViewById(R.id.screencolor_control);
        mScreenColorLayout.setBackgroundColor(R.color.screencolor_background);
        mScreenColorLayout.setOnClickListener(this);

        mHBar = (SeekBar) findViewById(R.id.hcontrol);
        mHBar.setOnSeekBarChangeListener(this);
        mSBar = (SeekBar) findViewById(R.id.scontrol);
        mSBar.setOnSeekBarChangeListener(this);
        mCBar = (SeekBar) findViewById(R.id.ccontrol);
        mCBar.setOnSeekBarChangeListener(this);
        mIBar = (SeekBar) findViewById(R.id.icontrol);
        mIBar.setOnSeekBarChangeListener(this);

        mHTv = (TextView) findViewById(R.id.hue);
        mHTv.setText(getString(R.string.hue_str, mHBar.getProgress()));
        mSTv = (TextView) findViewById(R.id.saturation);
        mSTv.setText(getString(R.string.saturation_str, mSBar.getProgress()));
        mCTv = (TextView) findViewById(R.id.contrast);
        mCTv.setText(getString(R.string.contrast_str, mCBar.getProgress()));
        mITv = (TextView) findViewById(R.id.intensity);
        mITv.setText(getString(R.string.intensity_str, mIBar.getProgress()));

        mReduceH = (ImageView)findViewById(R.id.reduce_hue);
        mReduceH.setOnClickListener(this);
        mIncreaseH = (ImageView)findViewById(R.id.increase_hue);
        mIncreaseH.setOnClickListener(this);
        mReduceS = (ImageView)findViewById(R.id.reduce_saturation);
        mReduceS.setOnClickListener(this);
        mIncreaseS = (ImageView)findViewById(R.id.increase_saturation);
        mIncreaseS.setOnClickListener(this);
        mReduceC = (ImageView)findViewById(R.id.reduce_contrast);
        mReduceC.setOnClickListener(this);
        mIncreaseC = (ImageView)findViewById(R.id.increase_contrast);
        mIncreaseC.setOnClickListener(this);
        mReduceI = (ImageView)findViewById(R.id.reduce_intensity);
        mReduceI.setOnClickListener(this);
        mIncreaseI = (ImageView)findViewById(R.id.increase_intensity);
        mIncreaseI.setOnClickListener(this);
        mUpdown = (ImageView)findViewById(R.id.up_down);
        mUpdown.setOnClickListener(this);
        mMore = (ImageView)findViewById(R.id.more);
        mMore.setOnClickListener(this);

        mPreviousBtn = (Button) findViewById(R.id.previous_btn);
        mPreviousBtn.setOnClickListener(this);
        mNewBtn = (Button) findViewById(R.id.new_btn);
        mNewBtn.setOnClickListener(this);
        mCancelBtn = (Button) findViewById(R.id.cancel);
        mCancelBtn.setOnClickListener(this);
        mSaveBtn = (Button) findViewById(R.id.save);
        mSaveBtn.setOnClickListener(this);

        initBtnsStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK){
            Log.i(TAG, "bail due to resultCode=" + resultCode);
            return;
        }
        switch (requestCode) {
            case REQUEST_SELECT_FILE:
                changePreviewByData(data);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(SELECT_FILE_GROUP1, SELECT_FILE_ITEM, SELECT_FILE_ORDER, R.string.selectfile_menu);
        menu.add(SELECT_FILE_GROUP1, RESET, SELECT_FILE_ORDER, R.string.restore_default_str);
        menu.add(SELECT_FILE_GROUP2, RESTORE_DEFAULT_PREVIEW, SELECT_FILE_ORDER,
                R.string.restore_preview);
        menu.add(SELECT_FILE_GROUP2, SELECT_NEW_FILE_ITEM, SELECT_FILE_ORDER,
                R.string.selectnewfile_menu);
        menu.add(SELECT_FILE_GROUP2, RESET, SELECT_FILE_ORDER, R.string.restore_default_str);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (canRestorePreview) {
            menu.setGroupVisible(SELECT_FILE_GROUP1, false);
            menu.setGroupVisible(SELECT_FILE_GROUP2, true);
        } else {
            menu.setGroupVisible(SELECT_FILE_GROUP1, true);
            menu.setGroupVisible(SELECT_FILE_GROUP2, false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case SELECT_NEW_FILE_ITEM:
                selectPicFromGallery2();
                break;
            case SELECT_FILE_ITEM:
                selectPicFromGallery2();
                break;
            case RESTORE_DEFAULT_PREVIEW:
                resotreBackgroundByDefault();
                break;
            case RESET:
                restoreDefaultHSCI();
                break;
            default:
                return true;
        }
        return false;
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setNewBtnHighlight();
        int id = seekBar.getId();
        switch (id) {
            case R.id.hcontrol:
                mHTv.setText(getString(R.string.hue_str, progress));
                break;
            case R.id.scontrol:
                mSTv.setText(getString(R.string.saturation_str, mSBar.getProgress()));
                break;
            case R.id.ccontrol:
                mCTv.setText(getString(R.string.contrast_str, mCBar.getProgress()));
                break;
            case R.id.icontrol:
                mITv.setText(getString(R.string.intensity_str, mIBar.getProgress()));
                break;
            default:
                break;
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        setActivated(seekBar,true);
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        setActivated(seekBar,false);
    }

    private void setActivated(SeekBar seekBar, boolean isActivated){
        int id = seekBar.getId();
        switch (id) {
            case R.id.hcontrol:
                mReduceH.setActivated(isActivated);
                mIncreaseH.setActivated(isActivated);
                break;
            case R.id.scontrol:
                mReduceS.setActivated(isActivated);
                mIncreaseS.setActivated(isActivated);
                break;
            case R.id.ccontrol:
                mReduceC.setActivated(isActivated);
                mIncreaseC.setActivated(isActivated);
                break;
            case R.id.icontrol:
                mReduceI.setActivated(isActivated);
                mIncreaseI.setActivated(isActivated);
                break;
            default:
                break;
        }

    }

    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.cancel:
                finish();
                break;
            case R.id.save:
                saveHSCI();
                break;
            case R.id.previous_btn:
                previousOrNewHSCI(true);
                break;
            case R.id.new_btn:
                previousOrNewHSCI(false);
                break;
            case R.id.up_down:
                upDownHSCISettingLayout();
                break;
            case R.id.more:
                showMoreMenus();
                break;
            case R.id.reduce_hue:
                mHBar.setProgress(( mHBar.getProgress() - 1 > 0 ) ? ( mHBar.getProgress() - 1 ) : 0 );
                break;
            case R.id.increase_hue:
                mHBar.setProgress(( mHBar.getProgress() + 1 < 100 ) ? ( mHBar.getProgress() + 1 ) : 100 );
                break;
            case R.id.reduce_saturation:
                mSBar.setProgress(( mSBar.getProgress() - 1 > 0 ) ? ( mSBar.getProgress() - 1 ) : 0 );
                break;
            case R.id.increase_saturation:
                mSBar.setProgress(( mSBar.getProgress() + 1 < 100 ) ? ( mSBar.getProgress() + 1 ) : 100 );
                break;
            case R.id.reduce_contrast:
                mCBar.setProgress(( mCBar.getProgress() - 1 > 0 ) ? ( mCBar.getProgress() - 1 ) : 0 );
                break;
            case R.id.increase_contrast:
                mCBar.setProgress(( mCBar.getProgress() + 1 < 100 ) ? ( mCBar.getProgress() + 1 ) : 100 );
                break;
            case R.id.reduce_intensity:
                mIBar.setProgress(( mIBar.getProgress() - 1 > 0 ) ? ( mIBar.getProgress() - 1 ) : 0 );
                break;
            case R.id.increase_intensity:
                mIBar.setProgress(( mIBar.getProgress() + 1 < 100 ) ? ( mIBar.getProgress() + 1 ) : 100 );
                break;
            default:
                break;
        }
    }

    private void selectPicFromGallery2(){
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        intent.setComponent(new ComponentName(GALLERY_PACKAGENAME, GALLERY_CLASSNAME));
        Bundle bundle = new Bundle();
        // define the width and heigh to crop the image.
        bundle.putInt(KEY_ASPECT_X, ASPECT_X);
        bundle.putInt(KEY_ASPECT_Y, ASPECT_Y);
        bundle.putFloat(KEY_SPOTLIGHT_X, SPOTLIGHT_X);
        bundle.putFloat(KEY_SPOTLIGHT_Y, SPOTLIGHT_Y);
        // send true to set CropImage view's title is not set wallpaper.
        bundle.putBoolean(KEY_FROME_SCREENCOLOR, true);
        intent.putExtras( bundle );
        startActivityForResult(intent,REQUEST_SELECT_FILE);
    }

    private void restoreDefaultHSCI(){
        // TODO restore default HSCI values.
        mHBar.setProgress(0);
        mSBar.setProgress(0);
        mCBar.setProgress(0);
        mIBar.setProgress(0);
        setNewBtnHighlight();
    }

    private void setNewBtnHighlight(){
        mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_default);
        mNewBtn.setBackgroundResource(R.drawable.ic_new_glow);
        mPreviousBtn.setEnabled(true);
        mNewBtn.setEnabled(true);
        mSaveBtn.setEnabled(true);

    }
    private void initBtnsStatus(){
        mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_dis);
        mNewBtn.setBackgroundResource(R.drawable.ic_new_dis);
        mPreviousBtn.setEnabled(false);
        mNewBtn.setEnabled(false);
        mSaveBtn.setEnabled(false);
    }

    private void previousOrNewHSCI(boolean isPrevious){
        if(View.GONE != mScreenColorLayout.getVisibility()){
            mScreenColorLayout.setVisibility(View.GONE);
            mUpdown.setBackgroundResource(R.drawable.up_button);
        }
        if(isPrevious){
            mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_glow);
            mNewBtn.setBackgroundResource(R.drawable.ic_new_default);
            mUpdown.setVisibility(View.GONE);
            mMore.setVisibility(View.GONE);
            // TODO show previous HSCI values.
        }else{
            mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_default);
            mNewBtn.setBackgroundResource(R.drawable.ic_new_glow);
            mUpdown.setVisibility(View.VISIBLE);
            mMore.setVisibility(View.VISIBLE);
            // TODO show new HSCI values.
        }
    }

    private void saveHSCI(){
        // TODO save HSCI values.
        finish();
    }

    private void changePreviewByData(Intent data){
        if (data != null) {
            Uri uri = data.getData();
            setBackgroundByUri(uri);
            saveSharePreferences(uri.toString());
        }
    }

    private void setBackgroundByUri(Uri uri){
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bm = BitmapFactory.decodeStream(is);
            BitmapDrawable bd=new BitmapDrawable(bm);
            mRLayout.setBackgroundDrawable(bd);
        } catch (FileNotFoundException e) {
        }
        canRestorePreview = true;
    }

    private void resotreBackgroundByDefault(){
        mRLayout.setBackgroundResource(R.drawable.default_screencolor_setting);
        saveSharePreferences("");
        canRestorePreview = false;
    }

    //use sharepreference to save preview image.
    private void saveSharePreferences(String value){
        SharedPreferences share = getSharedPreferences(STRING_NAME, Context.MODE_PRIVATE);
        Editor editor = share.edit();
        editor.putString(STRING_KEY, value);
        editor.commit();
    }

    private String getSharePreferences(){
        SharedPreferences share = getSharedPreferences(STRING_NAME, Context.MODE_WORLD_WRITEABLE|Context.MODE_WORLD_READABLE);
        return share.getString(STRING_KEY, "");
    }

    private void upDownHSCISettingLayout(){
        if(View.GONE != mScreenColorLayout.getVisibility()){
            mScreenColorLayout.setVisibility(View.GONE);
            mUpdown.setBackgroundResource(R.drawable.up_button);
        }else{
            mScreenColorLayout.setVisibility(View.VISIBLE);
            mUpdown.setBackgroundResource(R.drawable.down_button);
        }
    }

    private void showMoreMenus(){
        PopupMenu popup = new PopupMenu(ScreenColorSettings.this, mMore);
        Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.screencolor_more, menu);

        if (canRestorePreview) {
            menu.removeItem(R.id.selectfile_menu);
        } else {
            menu.removeItem(R.id.restore_preview);
            menu.removeItem(R.id.selectnewfile_menu);
        }

        popup.setOnMenuItemClickListener(new MyMenuItemclick());
        popup.show();
    }

    private class MyMenuItemclick implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.restore_preview:
                    resotreBackgroundByDefault();
                    return true;
                case R.id.selectnewfile_menu:
                    selectPicFromGallery2();
                    return true;
                case R.id.selectfile_menu:
                    selectPicFromGallery2();
                    return true;
                case R.id.restore_default_str:
                    restoreDefaultHSCI();
                    return true;
            }
            return true;
        }
    }
}
