package com.pullrefresh;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Scroller;

/**
 * 参考
 * http://my.oschina.net/oppo4545/blog/283197
 * 这个类实现了IPullToRefresh接口，作为下拉刷新的布局，
 * <p>处理onInterceptTouchEvent()和onTouchEvent()中的事件：
 * 当内容的View（比如ListView）正如处于最顶部，此时再向下拉，我们必须截断事件，
 * 然后move事件就会把后续的事件传递到onTouchEvent()方法中，然后再在这个方法中，我们根据move的距离再进行scroll整个View。<p/>
 *
 * <p>设置各种状态：这里面有很多状态，如下拉、上拉、刷新、加载中、释放等，
 * 它会根据用户拉动的距离来更改状态，状态的改变，它也会把Header和Footer的状态改变，
 * 然后Header和Footer会根据状态去显示相应的界面式样。</p>
 *
 * 待处理
 * 1.列表滚动到顶部继续滑动，没有处理
 * 2.上拉加载失败，没有更多的提示
 * 3.上拉成功下面的数据没自动出来
 * 4.子view是否可以上拉判断准确
 * 5.是否找到子view
 */
public class PullToRefreshLayout extends ViewGroup implements IPullToRefresh, NestedScrollingParent {
    private static final String TAG = PullToRefreshLayout.class.getSimpleName();
    private static final String LOG_TAG = TAG;
    private static final float DRAG_RATE = 0.5f;
    //阻尼，使有下拉感
    private int damp = 2;
    private static final long RESET_DELAY_MILLIS = 500;
    /**
     * 一个触摸动作被认为是用户滚动的像素距离
     */
    private int mTouchSlop;
    /**
     * 头部加载
     */
    private View mHeaderView;
    /**
     * 底部加载
     */
    private View mFooterView;
    /**
     * 开启下拉刷新
     */
    private boolean isPullRefreshEnabled = true;
    /**
     * 开启上拉加载
     */
    private boolean isPullLoadEnabled = true;
    /**
     * 滚动到最后就加载
     */
    private boolean isScrollLoadEnabled;
    //分别记录上次滑动的坐标
    private float mLastX;
    private float mLastY;
    private Scroller mScroller;


    private static final int INVALID_POINTER = -1;
    private OnRefreshListener mOnRefreshListener;
    private OnLoadMoreListener mLoadMoreListener;
    private int mState;
    private View mTarget;
    private int mActivePointerId;
    private float mInitialDownX;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mRefreshTriggerOffset;
    private int mLoadMoreTriggerOffset;
    private boolean mRefreshing = false;
    private boolean mLoading = false;

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private float mTotalUnconsumed;
    private float mTotalLoadUnconsumed;
    private boolean mNestedScrollInProgress;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private int mHeaderHeight;
    private int mFooterHeight;

    public PullToRefreshLayout(Context context) {
        this(context, null);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    private void init(Context context) {
        mScroller = new Scroller(getContext());
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int childNum = getChildCount();
        if (childNum == 0) {
            // no child return
            return;
        } else if (0 < childNum && childNum < 4) {
            mHeaderView = findViewById(R.id.swipe_refresh_header);
            mTarget = findViewById(R.id.swipe_target);
            mFooterView = findViewById(R.id.swipe_load_more_footer);
        } else {
            // more than three children: unsupported!
            throw new IllegalStateException("Children num must equal or less than 3");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            return;
        }
        int targetHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(targetHeightSize, MeasureSpec.EXACTLY));
        if(mHeaderView != null) {
            mHeaderView.measure(MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.AT_MOST));
            mHeaderHeight = mHeaderView.getMeasuredHeight();
            if (mRefreshTriggerOffset < mHeaderHeight) {
                mRefreshTriggerOffset = mHeaderHeight;
            }
        }
        if(mFooterView != null) {
            mFooterView.measure(MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.AT_MOST));
            mFooterHeight = mFooterView.getMeasuredHeight();
            if (mLoadMoreTriggerOffset < mFooterHeight) {
                mLoadMoreTriggerOffset = mFooterHeight;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        if(mHeaderView != null) {
            int headWidth = mHeaderView.getMeasuredWidth();
            int headHeight = mHeaderView.getMeasuredHeight();
            mHeaderView.layout(childLeft, -headHeight,
                    childLeft + headWidth, 0);
        }

        if(mFooterView != null) {
            int footerWidth = mFooterView.getMeasuredWidth();
            int footerHeight = mFooterView.getMeasuredHeight();
            mFooterView.layout(childLeft, height,
                    childLeft + footerWidth, height + footerHeight);
        }
    }

    @Override
    public boolean isReadyForPullDown() {
        return !canChildScrollUp();
    }

    @Override
    public boolean isReadyForPullUp() {
        return !canChildScrollDown();
    }

    @Override
    public void setPullRefreshEnabled(boolean pullRefreshEnabled) {
        isPullRefreshEnabled = pullRefreshEnabled;
    }

    @Override
    public void setPullLoadEnabled(boolean pullLoadEnabled) {
        isPullLoadEnabled = pullLoadEnabled;
    }

    @Override
    public void setScrollLoadEnabled(boolean scrollLoadEnabled) {
        isScrollLoadEnabled = scrollLoadEnabled;
    }

    @Override
    public boolean isPullRefreshEnabled() {
        return isPullRefreshEnabled;
    }

    @Override
    public boolean isPullLoadEnabled() {
        return isPullLoadEnabled;
    }

    @Override
    public boolean isScrollLoadEnabled() {
        return isScrollLoadEnabled;
    }

    @Override
    public void setOnRefreshListener(OnRefreshListener refreshListener) {
        mOnRefreshListener = refreshListener;
    }

    @Override
    public void setOnLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        mLoadMoreListener = loadMoreListener;
    }

    @Override
    public void setPullDownRefreshComplete() {
        setPullDown(PullState.REFRESH_SUCCESS);
        setPullDownReset();
    }

    private void setPullDownReset() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                smoothScrollBy(0, -getScrollY());
                mRefreshing = false;
                setPullDown(PullState.RESET);
            }
        }, RESET_DELAY_MILLIS);
    }

    @Override
    public void setPullUpRefreshComplete() {
        smoothScrollBy(0, -getScrollY());
        mLoading = false;
        setPullUp(PullState.RESET);
    }

    @Override
    public void setLastUpdatedLabel(CharSequence label) {

    }

    /*
    事件处理
    我们必须重写两个事件相关的方法onInterceptTouchEvent()和onTouchEvent()方法。
    由于ListView，ScrollView，WebView它们是放到内部的，所在事件先是传递到
    PullToRefreshLayout#onInterceptTouchEvent()方法中，所以我们应该在这个方法中去处理ACTION_MOVE事件，
    判断如果当前ListView，ScrollView，WebView是否在最顶部或最底部，如果是，则开始截断事件，
    一旦事件被截断，后续的事件就会传递到PullToRefreshLayout#onInterceptTouchEvent()方法中，
    我们再在ACTION_MOVE事件中去移动整个布局，从而实现下拉或上拉动作。

    滚动布局(scrollTo)
    如布局结构可知，默认情况下Header和Footer是放置在Content View的最上面和最下面，
    通过设置padding来让他跑到屏幕外面去了，如果我们将整个布局向下滚动(scrollTo)一定距离，
    那么Header就会被显示出来，基于这种情况，所以在我的实现中，最终我是调用scrollTo来实现下拉动作的。
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //外部拦截法
        final int action = MotionEventCompat.getActionMasked(ev);
        if (!isEnabled() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialDownX = getMotionEventX(ev, mActivePointerId);
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownX = initialDownX;
                mInitialDownY = initialDownY;
                mLastY = initialDownY;
                if (!mScroller.isFinished()) {
                    //当滑动未完成，下一个序列的点击事件任然交给父容器处理
                    Log.e(LOG_TAG, "!mScroller.isFinished().");
//                    mScroller.abortAnimation();
//                    mIsBeingDragged = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float x = getMotionEventX(ev, mActivePointerId);
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float xDiff = x - mInitialDownX;
                final float yDiff = y - mInitialDownY;
                if (Math.abs(xDiff) < Math.abs(yDiff)
                        && Math.abs(yDiff) > mTouchSlop && !mIsBeingDragged) {
                    if (isPullRefreshEnabled && yDiff > 0 && isReadyForPullDown()) {
                        //向下滑，且ListView已经滑动到最顶部
                        //父容器需要当前点击事件
                        mIsBeingDragged = true;
                    } else if (isPullLoadEnabled && yDiff < 0 && isReadyForPullUp()) {
                        //向上滑，且ListView已经滑动到最底部
                        mIsBeingDragged = true;
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex = -1;
        if (!isEnabled() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN");
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                if (!mScroller.isFinished()) {
//                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                //deltaY > 0 向下滑动
                final float deltaY = y - mLastY;
                if((deltaY > 0 && !isPullRefreshEnabled)
                        || (deltaY < 0 && !isPullLoadEnabled)) {
                    return false;
                }
                mLastY = y;
//                Log.d(TAG, "ACTION_MOVE, deltaX:" + " deltaY:" + deltaY);
                float overScroll = -(deltaY * DRAG_RATE);
                moveDrag(overScroll);
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                mLastY = MotionEventCompat.getY(ev, mActivePointerId);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }
                finishDrag();
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;

            case MotionEvent.ACTION_CANCEL:
                return false;
        }
        return true;
    }

    private void moveDrag(float overScroll) {
        scrollBy(0, (int) overScroll);
        //scrollY < 0 处于下拉状态
        int scrollY = getScrollY();
        if(scrollY < 0) {
            //下拉状态
            if(mRefreshing) {
                return;
            }
            if (Math.abs(scrollY + overScroll) < mRefreshTriggerOffset) {
                setPullDown(PullState.PULL_TO_REFRESH);
            } else {
                setPullDown(PullState.RELEASE_TO_REFRESH);
            }
        } else if(scrollY > 0){
            //上拉状态
            if(mLoading) {
                return;
            }
            if (Math.abs(scrollY + overScroll) < mLoadMoreTriggerOffset) {
                setPullUp(PullState.PULL_TO_REFRESH);
            } else {
                setPullUp(PullState.RELEASE_TO_REFRESH);
            }
        }
    }

    private void finishDrag() {
        int scrollY = getScrollY();
        Log.d(TAG, "ACTION_UP scrollY=" + scrollY);
        if (scrollY < 0) {
            int headerSize = mHeaderHeight;
            if (Math.abs(scrollY) >= mRefreshTriggerOffset) {
                //放开且高于头部，刷新
                smoothScrollBy(0, -scrollY - headerSize);
                setPullDown(PullState.REFRESHING);
                mRefreshing = true;
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onPullDownToRefresh(this);
                }
            } else {
                //放开且拉动距离小于触发值，还原
                smoothScrollBy(0, -scrollY);
                setPullDown(PullState.RESET);
            }
        } else if (scrollY > 0) {
            int footerSize = mFooterHeight;
            if (Math.abs(scrollY) >= mLoadMoreTriggerOffset) {
                smoothScrollBy(0, -scrollY + footerSize);
                setPullUp(PullState.REFRESHING);
                mLoading = true;
                if (mLoadMoreListener != null) {
                    mLoadMoreListener.onPullUpToLoadMore(this);
                }
            } else {
                smoothScrollBy(0, -scrollY);
                setPullUp(PullState.RESET);
            }
        }
    }

    private void setPullUp(int state) {
        if (mFooterView != null && mFooterView instanceof LoadingLayout) {
            LoadingLayout loadingLayout = (LoadingLayout) mFooterView;
            mState = state;
            switch (state) {
                case PullState.PULL_TO_REFRESH:
                    int scrollY = getScrollY();
                    int distance = mLoadMoreTriggerOffset;
                    float originalDragPercent = Math.abs(scrollY) * 1.0f / distance;
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    loadingLayout.onPullToRefresh(dragPercent);
                    break;
                case PullState.RELEASE_TO_REFRESH:
                    loadingLayout.onReleaseToRefresh();
                    break;
                case PullState.REFRESHING:
                    loadingLayout.onRefreshing();
                    break;
                default:
                    loadingLayout.onReset();
                    break;
            }
        }
    }

    private void setPullDown(int state) {
        if (mHeaderView != null && mHeaderView instanceof LoadingLayout) {
            LoadingLayout loadingLayout = (LoadingLayout) mHeaderView;
            mState = state;
            switch (state) {
                case PullState.PULL_TO_REFRESH:
                    int scrollY = getScrollY();
                    int distance = mRefreshTriggerOffset;
                    float originalDragPercent = Math.abs(scrollY) * 1.0f / distance;
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    loadingLayout.onPullToRefresh(dragPercent);
                    break;
                case PullState.RELEASE_TO_REFRESH:
                    loadingLayout.onReleaseToRefresh();
                    break;
                case PullState.REFRESHING:
                    loadingLayout.onRefreshing();
                    break;
                case PullState.REFRESH_SUCCESS:
                    loadingLayout.onRefreshSuccess();
                    break;
                default:
                    loadingLayout.onReset();
                    break;
            }
        }
    }

    private void smoothScrollBy(int dx, int dy) {
        mScroller.startScroll(getScrollX(), getScrollY(), dx, dy, 500);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    /**
     * Whether it is possible for the child view of this layout to
     * scroll down. Override this if the child view is a custom view.
     *
     * @return
     */
    protected boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getLastVisiblePosition() < absListView.getChildCount() - 1
                        || absListView.getChildAt(absListView.getChildCount() - 1).getBottom() > absListView.getPaddingBottom());
            } else {
                return ViewCompat.canScrollVertically(mTarget, 1) || mTarget.getScrollY() < 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    private float getMotionEventX(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getX(ev, index);
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    // NestedScrollingParent    嵌套滑动机制
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        Log.d(TAG, "onStartNestedScroll");
        return isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mTotalLoadUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    /**
     * 每次滑动前，Child 先询问 Parent 是否需要滑动，即dispatchNestedPreScroll()，
     * 这就回调到 Parent 的onNestedPreScroll()，Parent 可以在这个回调中“劫持”掉
     * Child 的滑动，也就是先于 Child 滑动。
     * @param target
     * @param dx
     * @param dy
     * @param consumed
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (isPullRefreshEnabled && dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            float overScroll = (consumed[1] * DRAG_RATE);
            moveDrag(overScroll);
        } else if (isPullLoadEnabled && dy < 0 && mTotalLoadUnconsumed < 0) {
            if (dy < mTotalLoadUnconsumed) {
                consumed[1] = (int) mTotalLoadUnconsumed;
                mTotalLoadUnconsumed = 0;
            } else {
                mTotalLoadUnconsumed -= dy;
                consumed[1] = dy;
            }
            float overScroll = (consumed[1] * DRAG_RATE);
            moveDrag(overScroll);
        }
        Log.d(TAG, "onNestedPreScroll");
        if(mRefreshing && dy > 0 && getScrollY() < 0) {
            Log.d(TAG, "向上滑，且头部显示");
            if (dy > Math.abs(getScrollY())) {
                consumed[1] = Math.abs(getScrollY());
            } else {
                consumed[1] = dy;
            }
            moveDrag(consumed[1]);
        } else if(mLoading && dy < 0 && getScrollY() > 0) {
            Log.d(TAG, "向下滑，且底部显示");
            if (Math.abs(dy) > getScrollY()) {
                consumed[1] = -getScrollY();
            } else {
                consumed[1] = dy;
            }
            moveDrag(consumed[1]);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishDrag();
            mTotalUnconsumed = 0;
        } else if(mTotalLoadUnconsumed < 0) {
            finishDrag();
            mTotalLoadUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    /**
     *  Child 滑动以后，会调用onNestedScroll()，回调到 Parent 的onNestedScroll()，
     *  这里就是 Child 滑动后，剩下的给 Parent 处理，也就是 后于 Child 滑动。
     * @param target
     * @param dxConsumed
     * @param dyConsumed
     * @param dxUnconsumed
     * @param dyUnconsumed
     */
    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        Log.d(TAG, "onNestedScroll dyConsumed=" + dyConsumed +" dyUnconsumed=" + dyUnconsumed);
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (isPullRefreshEnabled && dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            float overScroll = (dy * DRAG_RATE);
            moveDrag(overScroll);
        } else if(isPullLoadEnabled && dy > 0 && !canChildScrollDown()) {
            mTotalLoadUnconsumed += -dy;
            float overScroll = (dy * DRAG_RATE);
            moveDrag(overScroll);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}
