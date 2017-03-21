package com.pullrefresh;

/**
 * Created by Administrator on 2016/4/23.
 * 拉动过程的状态
 */
public interface PullState {
    /**复位重置*/
    int RESET = 0;
    /**下拉刷新*/
    int PULL_TO_REFRESH = RESET + 1;
    /**释放刷新*/
    int RELEASE_TO_REFRESH = RESET + 2;
    /**刷新中*/
    int REFRESHING = RESET + 3;
    /**刷新成功*/
    int REFRESH_SUCCESS = RESET + 4;
    /**刷新失败*/
    int REFRESH_FAIL = RESET + 5;
    /**没有更多*/
    int NO_MORE_DATA = RESET + 6;
}
