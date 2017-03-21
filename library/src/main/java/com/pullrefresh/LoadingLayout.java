package com.pullrefresh;

/**
 * Created by Administrator on 2016/4/18.
 */
public interface LoadingLayout {

    void onPullToRefresh(float fraction);

    void onReleaseToRefresh();

    void onRefreshing();

    void onRefreshSuccess();

    void onReset();
}
