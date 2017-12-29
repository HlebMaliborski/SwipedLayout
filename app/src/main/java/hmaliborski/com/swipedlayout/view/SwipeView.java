package hmaliborski.com.swipedlayout.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import hmaliborski.com.swipedlayout.R;

public class SwipeView extends FrameLayout {
    private static final String TAG = SwipeView.class.getSimpleName();

    private static final int SWIPE_LEFT = 1;
    private static final int SWIPE_RIGHT = 2;

    private static final double AUTO_OPEN_SPEED_LIMIT = 800.0;

    private static final boolean STATE_CLOSE = false;
    private static final boolean STATE_OPEN = true;

    private int mMaxWidth;
    private int mSwipeDirection = SWIPE_LEFT;
    private int mHorizontalDragRange;
    private int mViewPosition;
    private boolean mIsOpen;
    private boolean mIsBottomBorder;

    private ViewDragHelper mViewDragHelper;
    private View mMainView;
    private View mSecondView;
    private Rect mRect = new Rect();
    private Paint mPaint;

    private final ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mMainView;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int finalLeft = 0;

            switch (mSwipeDirection) {
                case SWIPE_RIGHT:
                    if (xvel > AUTO_OPEN_SPEED_LIMIT || mViewPosition > mSecondView.getWidth() / 2) {
                        finalLeft += mSecondView.getWidth();
                    }
                    break;

                case SWIPE_LEFT:
                    if (xvel < -AUTO_OPEN_SPEED_LIMIT || mViewPosition < -mSecondView.getWidth() / 2) {
                        finalLeft -= mSecondView.getWidth();
                    }
                    break;
            }

            mViewDragHelper.settleCapturedViewAt(finalLeft, releasedChild.getTop());

            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mHorizontalDragRange;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mViewPosition = left;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);

            if (mSwipeDirection == SWIPE_LEFT || mSwipeDirection == SWIPE_RIGHT) {
                if (mMainView.getLeft() == mRect.left) {
                    mIsOpen = STATE_CLOSE;
                } else {
                    mIsOpen = STATE_OPEN;
                }
            }
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int horizontalRange = 0;

            switch (mSwipeDirection) {
                case SWIPE_RIGHT:
                    horizontalRange = Math.max(
                            Math.min(left, mRect.left + mSecondView.getWidth()),
                            mRect.left
                    );
                    break;

                case SWIPE_LEFT:
                    horizontalRange = Math.max(
                            Math.min(left, mRect.left),
                            mRect.left - mSecondView.getWidth());
                    break;
            }

            return horizontalRange;
        }
    };

    public SwipeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setWillNotDraw(false);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwipeView, 0, 0);

        int borderColor;
        try {
            mSwipeDirection = typedArray.getInt(R.styleable.SwipeView_swipeDirection, 1);
            mIsBottomBorder = typedArray.getBoolean(R.styleable.SwipeView_bottomBorder, false);
            borderColor = typedArray.getColor(R.styleable.SwipeView_borderColor, Color.BLACK);
        } finally {
            typedArray.recycle();
        }

        mPaint = new Paint();
        mPaint.setColor(borderColor);
        mPaint.setStrokeWidth(1f);

        mViewDragHelper = ViewDragHelper.create(this, 1.0f, mCallback);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() == 2) {
            mSecondView = getChildAt(0);
            mMainView = getChildAt(1);
        } else {
            throw new RuntimeException("This layout doesn't support one view");
        }

        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            if (mMaxWidth < childView.getLeft() + childView.getPaddingLeft()) {
                mMaxWidth = childView.getLeft() + childView.getPaddingLeft();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mIsBottomBorder) {
            for (int i = 0; i < getChildCount(); i++) {
                int heightSpec = MeasureSpec.getSize(heightMeasureSpec);
                int parentMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
                int measureSpec = MeasureSpec.makeMeasureSpec(heightSpec - 1, parentMeasureMode);
                View childView = getChildAt(i);
                measureChild(childView, widthMeasureSpec, measureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mHorizontalDragRange = getWidth() - mSecondView.getWidth();
        mRect.set(mMainView.getLeft(), mMainView.getTop(), mMainView.getRight(), mMainView.getBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsBottomBorder) {
            canvas.drawLine(mMaxWidth, getBottom() - 1, getWidth(), getBottom() - 1, mPaint);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    static class SavedState extends BaseSavedState {
        boolean isOpen;
        int viewPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isOpen = in.readInt() != 0;
            viewPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isOpen ? 1 : 0);
            out.writeInt(viewPosition);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isOpen = mIsOpen;
        ss.viewPosition = mViewPosition;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mIsOpen = ss.isOpen;
        if (ss.isOpen) {
            openPane(ss.viewPosition);
        } else {
            closePane();
        }
    }

    private void openPane(int viewPosition) {
        if (mMainView != null) {
            mViewDragHelper.smoothSlideViewTo(mMainView, viewPosition, mRect.top);
        }
    }

    private void closePane() {
        if (mMainView != null) {
            mViewDragHelper.smoothSlideViewTo(mMainView, mRect.left, mRect.top);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
}