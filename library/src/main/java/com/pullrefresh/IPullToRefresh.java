package com.pullrefresh;

/**
 * 下拉刷新的功能抽象成一个接口.
 */
public interface IPullToRefresh {
    /**
     * 设置下拉刷新
     * @param pullRefreshEnabled
     */
    void setPullRefreshEnabled(boolean pullRefreshEnabled);

    /**
     * 设置上拉加载
     * @param pullLoadEnabled
     */
    void setPullLoadEnabled(boolean pullLoadEnabled);

    /**
     * 设置滚动到底自动加载
     * @param scrollLoadEnabled
     */
    void setScrollLoadEnabled(boolean scrollLoadEnabled);

    boolean isPullRefreshEnabled();

    boolean isPullLoadEnabled();

    boolean isScrollLoadEnabled();

    /**
     * 刷新监听器
     * @param refreshListener
     */
    void setOnRefreshListener(OnRefreshListener refreshListener);

    void setOnLoadMoreListener(OnLoadMoreListener loadMoreListener);

    /**
     * 下拉刷新完成
     */
    void setPullDownRefreshComplete();

    /**
     * 上拉加载完成
     */
    void setPullUpRefreshComplete();

    /**
     * 设置更新标签
     */
    void setLastUpdatedLabel(CharSequence label);

    /**
     * 判断刷新的View是否滑动到顶部
     *
     * @return true表示已经滑动到顶部，否则false
     */
    boolean isReadyForPullDown();

    /**
     * 判断刷新的View是否滑动到底
     *
     * @return true表示已经滑动到底部，否则false
     */
    boolean isReadyForPullUp();

}
