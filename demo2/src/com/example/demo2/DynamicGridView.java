package com.example.demo2;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;

public class DynamicGridView extends BaseGridview {
	private static final int INVALID_ID = AbstractDynamicGridAdapter.INVALID_ID;

	private static final int MOVE_DURATION = 300;
	private static final int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 8;

	private BitmapDrawable mHoverCell;
	private Rect mHoverCellCurrentBounds;
	private Rect mHoverCellOriginalBounds;

	private int mTotalOffsetY = 0;
	private int mTotalOffsetX = 0;

	private int mDownX = -1;
	private int mDownY = -1;
	private int mLastEventY = -1;
	private int mLastEventX = -1;

	private long mLeftId;
	private long mRightId;
	private long mAboveId;
	private long mBelowId;
	private long mAboveLeftId;
	private long mAboveRightId;
	private long mBelowLeftId;
	private long mBelowRightId;

	//add
	private boolean isAdded = false;
	private float xInScreen;
	private float yInScreen;
	private float initialX;
	private float initialY;
	private float initialTouchX;
	private float initialTouchY;
	private float touchX;
	private float touchY;
	private Vibrator mVibrator;
	private WindowManager.LayoutParams mParams;
	private static int statusBarHeight;
	private boolean isFirstDown = true;
	private String tag = "ontouch";
	private long mMobileItemId = INVALID_ID;

	private boolean mCellIsMobile = false;

	private int mActivePointerId = INVALID_ID;

	private boolean mIsMobileScrolling;
	private int mSmoothScrollAmountAtEdge = 0;
	private boolean mIsWaitingForScrollFinish = false;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
	private boolean mIsEditMode = false;

	private List<ObjectAnimator> mWobbleAnimators = new LinkedList<ObjectAnimator>();

	private OnDropListener mDropListener;

	private boolean mHoverAnimation;
	private boolean mReorderAnimation;

	private boolean mWobbleInEditMode = true;
	private ImageView currentMobileImageview;
	private WindowManager windowManager;
	private OnItemLongClickListener mUserLongClickListener;
	private OnItemLongClickListener mLocalLongClickListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos,
				long id) {
			if (!isEnabled() || isEditMode())
				return false;
			mVibrator.vibrate(500);
			mTotalOffsetY = 0;
			mTotalOffsetX = 0;
			int position = pointToPosition(mDownX, mDownY);
			int itemNum = position - getFirstVisiblePosition();
			if (itemNum > 1) {
				View selectedView = getChildAt(itemNum);
				mMobileItemId = getAdapter().getItemId(position);
				mHoverCell = getAndAddHoverView(selectedView);
				if (!isAdded) {
					mParams = initFloatingViewParams(
							(int) (xInScreen - mDownX + selectedView.getLeft() +selectedView
									.getWidth() ), (int) (yInScreen - mDownY
									+ selectedView.getTop() - selectedView
									.getHeight()), selectedView.getWidth(),
							selectedView.getHeight());
					Bitmap b = getBitmapFromView(selectedView);
					ImageView mobileImageview = new ImageView(getContext()
							.getApplicationContext());
					currentMobileImageview = mobileImageview;
					mobileImageview.setImageDrawable(new BitmapDrawable(
							getResources(), b));
					windowManager.addView(mobileImageview, mParams);
					isAdded = true;
				}
				if (isPostHoneycomb())
					selectedView.setVisibility(View.INVISIBLE);
				mCellIsMobile = true;
				updateNeighborViewsForId(mMobileItemId);
				if (isPostHoneycomb() && mWobbleInEditMode)
					startWobbleAnimation();
				if (mUserLongClickListener != null)
					mUserLongClickListener.onItemLongClick(arg0, arg1, pos, id);
				mIsEditMode = true;
			}
			return true;
		}
	};

	private OnItemClickListener mUserItemClickListener;
	private OnItemClickListener mLocalItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mUserItemClickListener.onItemClick(parent, view, position, id);
		}
	};

	public DynamicGridView(Context context) {
		super(context);
		init(context);

	}

	public DynamicGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public DynamicGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void setOnDropListener(OnDropListener dropListener) {
		this.mDropListener = dropListener;
	}

	public boolean isFirstDown() {
		return isFirstDown;
	}

	public void setFirstDown(boolean isFirstDown) {
		this.isFirstDown = isFirstDown;
	}

	public void startEditMode() {
		mIsEditMode = true;
		if (isPostHoneycomb() && mWobbleInEditMode)
			startWobbleAnimation();
	}

	public void stopEditMode() {
		mIsEditMode = false;
		if (isPostHoneycomb() && mWobbleInEditMode)
			stopWobble(true);
	}

	public boolean isEditMode() {
		return mIsEditMode;
	}

	public boolean isWobbleInEditMode() {
		return mWobbleInEditMode;
	}

	public void setWobbleInEditMode(boolean wobbleInEditMode) {
		this.mWobbleInEditMode = wobbleInEditMode;
	}

	@Override
	public void setOnItemLongClickListener(
			final OnItemLongClickListener listener) {
		mUserLongClickListener = listener;
		super.setOnItemLongClickListener(mLocalLongClickListener);
	}

	@Override
	public void setOnItemClickListener(OnItemClickListener listener) {
		this.mUserItemClickListener = listener;
		super.setOnItemClickListener(mLocalItemClickListener);
	}

	// update i from 0 to 2
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void startWobbleAnimation() {
		for (int i = 2; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v != null
					&& Boolean.TRUE != v.getTag(R.id.dynamic_grid_wobble_tag)) {
				if (i % 2 == 0)
					animateWobble(v);
				else
					animateWobbleInverse(v);
				v.setTag(R.id.dynamic_grid_wobble_tag, true);
			}
		}
	}

	// update i from 0 to 2
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void stopWobble(boolean resetRotation) {
		for (Animator wobbleAnimator : mWobbleAnimators) {
			wobbleAnimator.cancel();
		}
		mWobbleAnimators.clear();
		for (int i = 2; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v != null) {
				if (resetRotation)
					v.setRotation(0);
				v.setTag(R.id.dynamic_grid_wobble_tag, false);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void restartWobble() {
		stopWobble(false);
		startWobbleAnimation();
	}

	public void init(Context context) {
		setOnScrollListener(mScrollListener);
		windowManager = (WindowManager) context.getApplicationContext()
				.getSystemService(Context.WINDOW_SERVICE);
		mVibrator = (Vibrator) context
				.getSystemService(Service.VIBRATOR_SERVICE);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		mSmoothScrollAmountAtEdge = (int) (SMOOTH_SCROLL_AMOUNT_AT_EDGE
				* metrics.density + 0.5f);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void animateWobble(View v) {
		ObjectAnimator animator = createBaseWobble(v);
		animator.setFloatValues(-2, 2);
		animator.start();
		mWobbleAnimators.add(animator);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void animateWobbleInverse(View v) {
		ObjectAnimator animator = createBaseWobble(v);
		animator.setFloatValues(2, -2);
		animator.start();
		mWobbleAnimators.add(animator);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private ObjectAnimator createBaseWobble(View v) {
		ObjectAnimator animator = new ObjectAnimator();
		animator.setDuration(180);
		animator.setRepeatMode(ValueAnimator.REVERSE);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setPropertyName("rotation");
		animator.setTarget(v);
		return animator;
	}

	private void reorderElements(int originalPosition, int targetPosition) {
		getAdapterInterface().reorderItems(originalPosition, targetPosition);
	}

	private int getColumnCount() {
		return getAdapterInterface().getColumnCount();
	}

	private AbstractDynamicGridAdapter getAdapterInterface() {
		return ((AbstractDynamicGridAdapter) getAdapter());
	}

	/**
	 * Creates the hover cell with the appropriate bitmap and of appropriate
	 * size. The hover cell's BitmapDrawable is drawn on top of the bitmap every
	 * single time an invalidate call is made.
	 */
	private BitmapDrawable getAndAddHoverView(View v) {

		int w = v.getWidth();
		int h = v.getHeight();
		int top = v.getTop();
		int left = v.getLeft();

		Bitmap b = getBitmapFromView(v);

		BitmapDrawable drawable = new BitmapDrawable(getResources(), b);

		mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
		mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

		// drawable.setBounds(mHoverCellCurrentBounds);

		return drawable;
	}

	/**
	 * Returns a bitmap showing a screenshot of the view passed in.
	 */
	private Bitmap getBitmapFromView(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);
		return bitmap;
	}

	private void updateNeighborViewsForId(long itemId) {
		int position = getPositionForID(itemId);
		AbstractDynamicGridAdapter adapter = (AbstractDynamicGridAdapter) getAdapter();
		int nextPos = position + 1;
		int prevPos = position - 1;
		int prevAbovePos = position - getColumnCount() - 1;
		int nextAbovePos = position - getColumnCount() + 1;
		int prevBelowPos = position + getColumnCount() - 1;
		int nextBelowPos = position + getColumnCount() + 1;
		mRightId = nextPos % getColumnCount() != 0 ? adapter.getItemId(nextPos)
				: INVALID_ID;
		mLeftId = prevPos % getColumnCount() != getColumnCount() - 1 ? adapter
				.getItemId(prevPos) : INVALID_ID;
		mAboveId = adapter.getItemId(position - getColumnCount());
		mBelowId = adapter.getItemId(position + getColumnCount());
		mAboveLeftId = prevAbovePos % getColumnCount() != getColumnCount() - 1 ? adapter
				.getItemId(prevAbovePos) : INVALID_ID;
		mAboveRightId = nextAbovePos % getColumnCount() != 0 ? adapter
				.getItemId(nextAbovePos) : INVALID_ID;
		mBelowLeftId = prevBelowPos % getColumnCount() != getColumnCount() - 1 ? adapter
				.getItemId(prevBelowPos) : INVALID_ID;
		mBelowRightId = nextBelowPos % getColumnCount() != 0 ? adapter
				.getItemId(nextBelowPos) : INVALID_ID;
	}

	/**
	 * Retrieves the position in the grid corresponding to <code>itemId</code>
	 */
	public int getPositionForID(long itemId) {
		View v = getViewForId(itemId);
		if (v == null) {
			return -1;
		} else {
			return getPositionForView(v);
		}
	}

	// update i from 0 to 2
	public View getViewForId(long itemId) {
		int firstVisiblePosition = getFirstVisiblePosition();
		AbstractDynamicGridAdapter adapter = ((AbstractDynamicGridAdapter) getAdapter());
		for (int i = 2; i < getChildCount(); i++) {
			View v = getChildAt(i);
			int position = firstVisiblePosition + i;
			long id = adapter.getItemId(position);
			if (id == itemId) {
				return v;
			}
		}
		return null;
	}

	
	//update the most:down,move,up.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (isAdded) {
				initialX = mParams.x;
				initialY = mParams.y;
				initialTouchX = event.getRawX();
				initialTouchY = event.getRawY();
			}
			mDownX = (int) event.getX();
			mDownY = (int) event.getY();
			xInScreen = event.getRawX();
			yInScreen = event.getRawY() - getStatusBarHeight();
			mActivePointerId = event.getPointerId(0);
			if (mIsEditMode && isEnabled()) {
				layoutChildren();
				mTotalOffsetY = 0;
				mTotalOffsetX = 0;
				int position = pointToPosition(mDownX, mDownY);
				int itemNum = position - getFirstVisiblePosition();
				View selectedView = getChildAt(itemNum);
				if (selectedView == null || itemNum == 0 || itemNum == 1) {
					return false;
				} else {
					mMobileItemId = getAdapter().getItemId(position);
					mHoverCell = getAndAddHoverView(selectedView);
					if (!isAdded) {
//						mParams = initFloatingViewParams((int) (xInScreen
//								- mDownX + selectedView.getLeft() / 2),
//								(int) (yInScreen - mDownY + selectedView
//										.getTop()), selectedView.getWidth(),
//								selectedView.getHeight());
						// mParams = initFloatingViewParams((int) (mDownX),
						// (int)(mDownY), selectedView.getWidth(),
						// selectedView.getHeight());
						 mParams = initFloatingViewParams((int) (xInScreen),
						 (int)(yInScreen), selectedView.getWidth(),
						 selectedView.getHeight());
						Bitmap b = getBitmapFromView(selectedView);
						ImageView mobileImageview = new ImageView(getContext()
								.getApplicationContext());
						currentMobileImageview = mobileImageview;
						mobileImageview.setImageDrawable(new BitmapDrawable(
								getResources(), b));
						windowManager.addView(mobileImageview, mParams);
						isAdded = true;
					}
					if (isPostHoneycomb())
						selectedView.setVisibility(View.INVISIBLE);
					mCellIsMobile = true;
					updateNeighborViewsForId(mMobileItemId);
				}
			} else if (!isEnabled()) {
				return false;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mActivePointerId == INVALID_ID) {
				break;
			}
			int pointerIndex = event.findPointerIndex(mActivePointerId);
			try {
				mLastEventY = (int) event.getY(pointerIndex);
				mLastEventX = (int) event.getX(pointerIndex);
			} catch (IllegalArgumentException e) {
				 e.printStackTrace(); 
			}
			touchY = event.getRawY();
			touchX = xInScreen = event.getRawX();
			yInScreen = event.getRawY() - getStatusBarHeight();
			if (mCellIsMobile) {
				updateViewPosition();
				handleCellSwitch();
				mIsMobileScrolling = false;
				handleMobileCellScroll();
				return false;
			}

			break;
		case MotionEvent.ACTION_UP:
			int pointerIndex1 = event.findPointerIndex(mActivePointerId);
			Log.i(tag, "ACTION_UP mActivePointerId:"+mActivePointerId+",pointerIndex1:"+pointerIndex1);
			try {
				mLastEventY = (int) event.getY(pointerIndex1);//error  pointer out of index
				mLastEventX = (int) event.getX(pointerIndex1);
			} catch (IllegalArgumentException e) {
				 e.printStackTrace(); 
			}
			int deltaY = mLastEventY - mDownY;
			int deltaX = mLastEventX - mDownX;
			if (mDropListener != null) {
				mDropListener.onActionDrop();
			}
			//****add
			if (isAdded) {
				windowManager.removeView(currentMobileImageview);
				isAdded = false;
				mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left
						+ deltaX + mTotalOffsetX, mHoverCellOriginalBounds.top
						+ deltaY + mTotalOffsetY);
				mHoverCell.setBounds(mHoverCellCurrentBounds);
				invalidate();
			}//add****
			touchEventsEnded();
			break;
		case MotionEvent.ACTION_CANCEL:
			touchEventsCancelled();
			if (mDropListener != null) {
				mDropListener.onActionDrop();
			}
			break;
//		case MotionEvent.ACTION_POINTER_UP:
//			pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
//			final int pointerId = event.getPointerId(pointerIndex);
//			if (pointerId == mActivePointerId) {
//				int pointerIndex2 = event.findPointerIndex(mActivePointerId);
//				mLastEventY = (int) event.getY(pointerIndex2);//error  pointer out of index
//				mLastEventX = (int) event.getX(pointerIndex2);
//				if (isAdded) {
//					windowManager.removeView(currentMobileImageview);
//					isAdded = false;
//					mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left
//							+ mLastEventX - mDownX + mTotalOffsetX, mHoverCellOriginalBounds.top
//							+ mLastEventY - mDownY + mTotalOffsetY);
//					mHoverCell.setBounds(mHoverCellCurrentBounds);
//					invalidate();
//				}
//				touchEventsEnded();
//			}
//			break;
		default:
			break;
		}
		return super.onTouchEvent(event);
	}

	public void clearFloatingView() {
		if (windowManager != null && isAdded && currentMobileImageview != null) {
			windowManager.removeView(currentMobileImageview);
		}
	}

	private android.view.WindowManager.LayoutParams initFloatingViewParams(
			int l, int t, int width, int height) {
		android.view.WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
				PixelFormat.TRANSPARENT);
		params.gravity = Gravity.LEFT | Gravity.TOP;
		params.windowAnimations = 0;
		params.x = l;
		params.y = t;
		return params;
	}

	private void handleMobileCellScroll() {
		mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
	}

	public boolean handleMobileCellScroll(Rect r) {
		int offset = computeVerticalScrollOffset();
		int height = getHeight();
		int extent = computeVerticalScrollExtent();
		int range = computeVerticalScrollRange();
		int hoverViewTop = r.top;
		int hoverHeight = r.height();
		if (hoverViewTop <= 0 && offset > 0) {
			smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
			return true;
		}
		if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
			smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
			return true;
		}
		return false;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
	}

	private void touchEventsEnded() {
		final View mobileView = getViewForId(mMobileItemId);
		if (mCellIsMobile || mIsWaitingForScrollFinish) {
			mCellIsMobile = false;
			mIsWaitingForScrollFinish = false;
			mIsMobileScrolling = false;
			mActivePointerId = INVALID_ID;
			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mIsWaitingForScrollFinish = true;
				return;
			}
			mHoverCellCurrentBounds.offsetTo(mobileView.getLeft(),
					mobileView.getTop());
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
				animateBounds(mobileView);
			} else {
				mHoverCell.setBounds(mHoverCellCurrentBounds);
				invalidate();
				reset(mobileView);
			}
		} else {
			touchEventsCancelled();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void animateBounds(final View mobileView) {
		TypeEvaluator<Rect> sBoundEvaluator = new TypeEvaluator<Rect>() {
			public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
				return new Rect(interpolate(startValue.left, endValue.left,
						fraction), interpolate(startValue.top, endValue.top,
						fraction), interpolate(startValue.right,
						endValue.right, fraction), interpolate(
						startValue.bottom, endValue.bottom, fraction));
			}

			public int interpolate(int start, int end, float fraction) {
				return (int) (start + fraction * (end - start));
			}
		};

		ObjectAnimator hoverViewAnimator = ObjectAnimator.ofObject(mHoverCell,
				"bounds", sBoundEvaluator, mHoverCellCurrentBounds);
		hoverViewAnimator
				.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator valueAnimator) {
						invalidate();
					}
				});
		hoverViewAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				mHoverAnimation = true;
				updateEnableState();
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				mHoverAnimation = false;
				updateEnableState();
				reset(mobileView);
			}
		});
		hoverViewAnimator.start();
	}

	private void reset(View mobileView) {
		mLeftId = INVALID_ID;
		mRightId = INVALID_ID;
		mAboveId = INVALID_ID;
		mBelowId = INVALID_ID;
		mAboveRightId = INVALID_ID;
		mAboveLeftId = INVALID_ID;
		mBelowLeftId = INVALID_ID;
		mBelowRightId = INVALID_ID;
		mMobileItemId = INVALID_ID;
		mobileView.setVisibility(View.VISIBLE);
		mHoverCell = null;
		if (!mIsEditMode && isPostHoneycomb() && mWobbleInEditMode)
			stopWobble(true);
		if (mIsEditMode && isPostHoneycomb() && mWobbleInEditMode)
			restartWobble();
		invalidate();

	}

	private void updateEnableState() {
		setEnabled(!mHoverAnimation && !mReorderAnimation);
	}

	/**
	 * Seems that GridView before HONEYCOMB not support stable id in proper way.
	 * That cause bugs on view recycle if we will animate or change visibility
	 * state for items.
	 * 
	 * @return
	 */
	private boolean isPostHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	private void touchEventsCancelled() {
		View mobileView = getViewForId(mMobileItemId);
		if (mCellIsMobile) {
			reset(mobileView);
		}
		mCellIsMobile = false;
		mIsMobileScrolling = false;
		mActivePointerId = INVALID_ID;

	}

	private void handleCellSwitch() {
		final int deltaY = mLastEventY - mDownY;
		final int deltaX = mLastEventX - mDownX;
		final int deltaYTotal = mHoverCellOriginalBounds.centerY()
				+ mTotalOffsetY + deltaY;
		final int deltaXTotal = mHoverCellOriginalBounds.centerX()
				+ mTotalOffsetX + deltaX;
		View belowView = getViewForId(mBelowId);
		View aboveView = getViewForId(mAboveId);
		View leftView = getViewForId(mLeftId);
		View rightView = getViewForId(mRightId);
		View mobileView = getViewForId(mMobileItemId);
		View aboveLeftView = getViewForId(mAboveLeftId);
		View aboveRightView = getViewForId(mAboveRightId);
		View belowLeftView = getViewForId(mBelowLeftId);
		View belowRightView = getViewForId(mBelowRightId);
		boolean isBelowRight = belowRightView != null
				&& deltaYTotal > belowRightView.getTop()
				&& deltaXTotal > belowRightView.getLeft();
		boolean isBelowLeft = belowLeftView != null
				&& deltaYTotal > belowLeftView.getTop()
				&& deltaXTotal < belowLeftView.getRight();
		boolean isAboveRight = aboveRightView != null
				&& deltaYTotal < aboveRightView.getBottom()
				&& deltaXTotal > aboveRightView.getLeft();
		boolean isAboveLeft = aboveLeftView != null
				&& deltaYTotal < aboveLeftView.getBottom()
				&& deltaXTotal < aboveLeftView.getRight();
		boolean isBelow = belowView != null && deltaYTotal > belowView.getTop();
		boolean isAbove = aboveView != null
				&& deltaYTotal < aboveView.getBottom();
		boolean isRight = rightView != null
				&& deltaXTotal > rightView.getLeft();
		boolean isLeft = leftView != null && deltaXTotal < leftView.getRight();

		if (isAbove || isBelow || isRight || isLeft || isAboveLeft
				|| isAboveRight || isBelowLeft || isBelowRight) {
			final int originalPosition = getPositionForView(mobileView);
			int targetPosition = INVALID_POSITION;
			int numColumns = getColumnCount();

			if (isAboveLeft)
				targetPosition = originalPosition - numColumns - 1;
			else if (isAboveRight)
				targetPosition = originalPosition - numColumns + 1;
			else if (isBelowLeft)
				targetPosition = originalPosition + numColumns - 1;
			else if (isBelowRight)
				targetPosition = originalPosition + numColumns + 1;
			else if (isAbove)
				targetPosition = originalPosition - numColumns;
			else if (isBelow)
				targetPosition = originalPosition + numColumns;
			else if (isLeft)
				targetPosition = originalPosition - 1;
			else if (isRight)
				targetPosition = originalPosition + 1;

			if (targetPosition == INVALID_POSITION) {
				updateNeighborViewsForId(mMobileItemId);
				return;
			}
			final View targetView = getViewForId(getId(targetPosition));
			reorderElements(originalPosition, targetPosition);

			mDownY = mLastEventY;
			mDownX = mLastEventX;

			mobileView.setVisibility(View.VISIBLE);

			if (isPostHoneycomb()) {
				targetView.setVisibility(View.INVISIBLE);
			}

			updateNeighborViewsForId(mMobileItemId);

			final ViewTreeObserver observer = getViewTreeObserver();
			final int finalTargetPosition = targetPosition;

			if (isPostHoneycomb()) {
				observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						observer.removeOnPreDrawListener(this);
						mTotalOffsetY += deltaY;
						mTotalOffsetX += deltaX;
						animateReorder(originalPosition, finalTargetPosition);
						return true;
					}
				});
			} else {
				mTotalOffsetY += deltaY;
				mTotalOffsetX += deltaX;
			}

		}
	}

	private long getId(int position) {
		return getAdapter().getItemId(position);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void animateReorder(final int oldPosition, final int newPosition) {
		boolean isForward = newPosition > oldPosition;
		List<Animator> resultList = new LinkedList<Animator>();
		if (isForward) {
			for (int pos = Math.min(oldPosition, newPosition); pos < Math.max(
					oldPosition, newPosition); pos++) {
				View view = getViewForId(getId(pos));
				if ((pos + 1) % getColumnCount() == 0) {
					resultList.add(createTranslationAnimations(view,
							-view.getWidth() * (getColumnCount() - 1), 0,
							view.getHeight(), 0));
				} else {
					resultList.add(createTranslationAnimations(view,
							view.getWidth(), 0, 0, 0));
				}
			}
		} else {
			for (int pos = Math.max(oldPosition, newPosition); pos > Math.min(
					oldPosition, newPosition); pos--) {
				View view = getViewForId(getId(pos));
				if ((pos + getColumnCount()) % getColumnCount() == 0) {
					resultList.add(createTranslationAnimations(view,
							view.getWidth() * (getColumnCount() - 1), 0,
							-view.getHeight(), 0));
				} else {
					resultList.add(createTranslationAnimations(view,
							-view.getWidth(), 0, 0, 0));
				}
			}
		}

		AnimatorSet resultSet = new AnimatorSet();
		resultSet.playTogether(resultList);
		resultSet.setDuration(MOVE_DURATION);
		resultSet.setInterpolator(new AccelerateDecelerateInterpolator());
		resultSet.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				mReorderAnimation = true;
				updateEnableState();
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				mReorderAnimation = false;
				updateEnableState();
			}
		});
		resultSet.start();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private AnimatorSet createTranslationAnimations(View view, float startX,
			float endX, float startY, float endY) {
		ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX",
				startX, endX);
		ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY",
				startY, endY);
		AnimatorSet animSetXY = new AnimatorSet();
		animSetXY.playTogether(animX, animY);
		return animSetXY;
	}

	private int getStatusBarHeight() {
		if (statusBarHeight == 0) {
			try {
				Class<?> c = Class.forName("com.android.internal.R$dimen");
				Object o = c.newInstance();
				Field field = c.getField("status_bar_height");
				int x = (Integer) field.get(o);
				statusBarHeight = getResources().getDimensionPixelSize(x);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return statusBarHeight;
	}

	public void updateViewPosition() {
		mParams.x = (int) (initialX + (int) (touchX - initialTouchX));
		mParams.y = (int) (initialY + (int) (touchY - initialTouchY));
		windowManager.updateViewLayout(currentMobileImageview, mParams);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHoverCell != null) {
			mHoverCell.draw(canvas);
		}
	}

	/**
	 * Interface provide callback for end of drag'n'drop event
	 */
	public interface OnDropListener {
		/**
		 * called when view been dropped
		 */
		void onActionDrop();
	}

	/**
	 * This scroll listener is added to the gridview in order to handle cell
	 * swapping when the cell is either at the top or bottom edge of the
	 * gridview. If the hover cell is at either edge of the gridview, the
	 * gridview will begin scrolling. As scrolling takes place, the gridview
	 * continuously checks if new cells became visible and determines whether
	 * they are potential candidates for a cell swap.
	 */
	private OnScrollListener mScrollListener = new OnScrollListener() {
		private int mPreviousFirstVisibleItem = -1;
		private int mPreviousVisibleItemCount = -1;
		private int mCurrentFirstVisibleItem;
		private int mCurrentVisibleItemCount;
		private int mCurrentScrollState;

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			mCurrentFirstVisibleItem = firstVisibleItem;
			mCurrentVisibleItemCount = visibleItemCount;

			mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
					: mPreviousFirstVisibleItem;
			mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
					: mPreviousVisibleItemCount;

			checkAndHandleFirstVisibleCellChange();
			checkAndHandleLastVisibleCellChange();

			mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
			mPreviousVisibleItemCount = mCurrentVisibleItemCount;
			if (isPostHoneycomb() && mWobbleInEditMode) {
				updateWobbleState(visibleItemCount);
			}
		}
		
		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		private void updateWobbleState(int visibleItemCount) {
			for (int i = 2; i < visibleItemCount; i++) {
				View child = getChildAt(i);

				if (child != null) {
					if (mMobileItemId != INVALID_ID
							&& Boolean.TRUE != child
									.getTag(R.id.dynamic_grid_wobble_tag)) {
						if (i % 2 == 0)
							animateWobble(child);
						else
							animateWobbleInverse(child);
						child.setTag(R.id.dynamic_grid_wobble_tag, true);
					} else if (mMobileItemId == INVALID_ID
							&& child.getRotation() != 0) {
						child.setRotation(0);
						child.setTag(R.id.dynamic_grid_wobble_tag, false);
					}
				}

			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mCurrentScrollState = scrollState;
			mScrollState = scrollState;
			isScrollCompleted();
		}

		/**
		 * This method is in charge of invoking 1 of 2 actions. Firstly, if the
		 * gridview is in a state of scrolling invoked by the hover cell being
		 * outside the bounds of the gridview, then this scrolling event is
		 * continued. Secondly, if the hover cell has already been released,
		 * this invokes the animation for the hover cell to return to its
		 * correct position after the gridview has entered an idle scroll state.
		 */
		private void isScrollCompleted() {
			if (mCurrentVisibleItemCount > 0
					&& mCurrentScrollState == SCROLL_STATE_IDLE) {
				if (mCellIsMobile && mIsMobileScrolling) {
					handleMobileCellScroll();
				} else if (mIsWaitingForScrollFinish) {
					touchEventsEnded();
				}
			}
		}

		/**
		 * Determines if the gridview scrolled up enough to reveal a new cell at
		 * the top of the list. If so, then the appropriate parameters are
		 * updated.
		 */
		public void checkAndHandleFirstVisibleCellChange() {
			if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForId(mMobileItemId);
					handleCellSwitch();
				}
			}
		}

		/**
		 * Determines if the gridview scrolled down enough to reveal a new cell
		 * at the bottom of the list. If so, then the appropriate parameters are
		 * updated.
		 */
		public void checkAndHandleLastVisibleCellChange() {
			int currentLastVisibleItem = mCurrentFirstVisibleItem
					+ mCurrentVisibleItemCount;
			int previousLastVisibleItem = mPreviousFirstVisibleItem
					+ mPreviousVisibleItemCount;
			if (currentLastVisibleItem != previousLastVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForId(mMobileItemId);
					handleCellSwitch();
				}
			}
		}
	};
}
