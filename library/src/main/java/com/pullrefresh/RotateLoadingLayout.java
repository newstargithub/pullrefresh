package com.pullrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 显示一个旋转图标的式样.
 */
public class RotateLoadingLayout extends RelativeLayout implements LoadingLayout {
    private DisplayMetrics displayMetrics;
    private TextView tv_state;
    private ProgressBar pb_loading;
    private ImageView iv_arrow;
    private boolean arrowRotate;

    public RotateLoadingLayout(Context context) {
        this(context, null);
    }

    public RotateLoadingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotateLoadingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        displayMetrics = context.getResources().getDisplayMetrics();
        LayoutInflater.from(context).inflate(R.layout.layout_footer_loading, this, true);
        tv_state = (TextView) findViewById(R.id.tv_state);
        pb_loading = (ProgressBar) findViewById(R.id.pb_loading);
        iv_arrow = (ImageView) findViewById(R.id.iv_arrow);
        onReset();
    }

    @Override
    public void onPullToRefresh(float fraction) {
        tv_state.setText(R.string.pull_up_load);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(VISIBLE);
        if(arrowRotate) {
            arrowRotate = false;
            ImageViewRotationHelper.rotateReverseObject(iv_arrow);
        }
    }

    @Override
    public void onReleaseToRefresh() {
        tv_state.setText(R.string.release_load);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(VISIBLE);
        if(!arrowRotate) {
            arrowRotate = true;
            ImageViewRotationHelper.rotateObject(iv_arrow);
        }
    }

    @Override
    public void onRefreshing() {
        tv_state.setText(R.string.loading);
        pb_loading.setVisibility(VISIBLE);
        iv_arrow.setVisibility(GONE);
    }

    @Override
    public void onRefreshSuccess() {

    }

    @Override
    public void onReset() {
        tv_state.setText(null);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(VISIBLE);
    }
}
