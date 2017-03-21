package com.pullrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 它默认是显示箭头式样的布局.
 */
public class HeaderLoadingLayout extends RelativeLayout implements LoadingLayout{

    private static final String TAG = HeaderLoadingLayout.class.getSimpleName();
    private TextView tv_state;
    private ProgressBar pb_loading;
    private ImageView iv_arrow;
    private DisplayMetrics displayMetrics;
    private boolean arrowDown = true;

    public HeaderLoadingLayout(Context context) {
        this(context, null);
    }

    public HeaderLoadingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderLoadingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        displayMetrics = context.getResources().getDisplayMetrics();
        LayoutInflater.from(context).inflate(R.layout.layout_header_loading, this, true);
        tv_state = (TextView) findViewById(R.id.tv_state);
        pb_loading = (ProgressBar) findViewById(R.id.pb_loading);
        iv_arrow = (ImageView) findViewById(R.id.iv_arrow);
        onReset();
    }

    @Override
    public void onPullToRefresh(float fraction) {
        Log.d(TAG, "onPullToRefresh");
        tv_state.setText(R.string.pull_down_refresh);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(VISIBLE);
//        iv_arrow.animate().scaleX(fraction).scaleY(fraction).setDuration(100).start();
        rotationArrowDown();
    }

    @Override
    public void onReleaseToRefresh() {
        Log.d(TAG, "onReleaseToRefresh");
        tv_state.setText(R.string.release_refresh);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(VISIBLE);
        rotationArrowUp();
    }

    @Override
    public void onRefreshing() {
        Log.d(TAG, "onRefreshing");
        tv_state.setText(R.string.refreshing);
        pb_loading.setVisibility(VISIBLE);
        iv_arrow.setVisibility(GONE);
    }

    @Override
    public void onRefreshSuccess() {
        tv_state.setText(R.string.refresh_success);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(GONE);
    }

    @Override
    public void onReset() {
        Log.d(TAG, "onReset");
        tv_state.setText(null);
        pb_loading.setVisibility(GONE);
        iv_arrow.setVisibility(GONE);
        rotationArrowDown();
    }

    private void rotationArrowDown() {
        if(!arrowDown) {
            arrowDown = true;
            ImageViewRotationHelper.rotateReverseObject(iv_arrow);
        }
    }

    private void rotationArrowUp() {
        if(arrowDown) {
            arrowDown = false;
            ImageViewRotationHelper.rotateObject(iv_arrow);
        }
    }
}
