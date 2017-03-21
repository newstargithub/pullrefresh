package com.pullrefresh;


/**
 * 刷新动作监听者.
 */
public interface OnRefreshListener {
    void onPullDownToRefresh(PullToRefreshLayout refreshView);
}
