package com.pullrefresh;

import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;

/**
 * 主要的作用就是实现了ImageView的旋转功能，内部作了版本的区分
 * 最核心的就是，如果在2.x的版本上，旋转ImageView使用Matrix。
 */
public class ImageViewRotationHelper {
    private static final String TAG = ImageViewRotationHelper.class.getSimpleName();
    private static final long ROTATION_DURATION = 500;

    public static void rotate(final View target){
        final Integer start = 0;
        final Integer end = 180;
        ValueAnimator.ofInt(0, 100).setDuration(ROTATION_DURATION).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            //估值时使用
            private IntEvaluator mEvaluator = new IntEvaluator();
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //获取当前动画的进度值，整型， 1-100之间
                int value = (int) animation.getAnimatedValue();
                Log.d(TAG, "current value:" + value);
                //获取当前进度占整个动画过程的比例，浮点型，0-1之间
                float fraction = animation.getAnimatedFraction();
                Integer evaluate = mEvaluator.evaluate(fraction, start, end);
                target.setRotation(evaluate);
            }
        });
    }

    /**
     * 顺时针旋转180度
     * @param target
     */
    public static void rotateObject(View target){
        target.clearAnimation();
        target.animate().rotation(-180).setDuration(ROTATION_DURATION).start();
        /*ObjectAnimator rotated = ObjectAnimator.ofFloat(target, "rotated", 0, -180);
        rotated.setDuration(1000);
        rotated.start();*/
    }

    /**
     * 逆时针旋转180度
     * @param target
     */
    public static void rotateReverseObject(View target) {
        target.clearAnimation();
        target.animate().rotation(0).setDuration(ROTATION_DURATION).start();
        /*ObjectAnimator rotated = ObjectAnimator.ofFloat(target, "rotated", -180, 0);
        rotated.setDuration(1000);
        rotated.start();*/
    }
}
