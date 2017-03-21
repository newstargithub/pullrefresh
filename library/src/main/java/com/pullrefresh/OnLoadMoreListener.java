package com.pullrefresh;


/**
 * 刷新动作监听者.
 */
public interface OnLoadMoreListener {
    void onPullUpToLoadMore(PullToRefreshLayout refreshView);
}
