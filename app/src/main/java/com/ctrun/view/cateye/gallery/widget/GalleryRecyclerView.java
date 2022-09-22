package com.ctrun.view.cateye.gallery.widget;

import static android.view.View.MeasureSpec.EXACTLY;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ctrun.view.cateye.gallery.R;

/**
 * @author ctrun on 2018/1/19.
 */

@SuppressWarnings({"FieldCanBeLocal"})
public class GalleryRecyclerView extends RecyclerView {
    private static final String TAG = GalleryRecyclerView.class.getSimpleName();

    private final LinearSnapHelper mSnapHelper = new LinearSnapHelper();

    private final OnScrollListener mScrollListener =
            new OnScrollListener() {
                boolean mScrolled = false;
                boolean mDragScrolled = false;

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        mDragScrolled = true;
                        return;
                    }

                    if (newState == RecyclerView.SCROLL_STATE_IDLE && mScrolled) {
                        final boolean dragScrolled = mDragScrolled;
                        mScrolled = false;
                        mDragScrolled = false;

                        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                        if (layoutManager == null) {
                            return;
                        }

                        View snapView = mSnapHelper.findSnapView(layoutManager);
                        if (snapView == null) {
                            return;
                        }

                        if (dragScrolled) {
                            int[] snapDistance = mSnapHelper.calculateDistanceToFinalSnap(layoutManager, snapView);
                            //noinspection ConstantConditions
                            if (snapDistance[0] != 0 || snapDistance[1] != 0) {
                                smoothScrollBy(snapDistance[0], snapDistance[1]);
                            }
                        } else {
                            ViewHolder viewHolder = findContainingViewHolder(snapView);
                            if (viewHolder != null) {
                                int position = viewHolder.getAdapterPosition();
                                layoutManager.scrollToPositionWithOffset(position, 0);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (dx != 0 || dy != 0) {
                        mScrolled = true;
                    }
                }
            };


    private View mSelectedView;
    private int mSelectedPosition = RecyclerView.NO_POSITION;

    private final int mItemWidth;
    private final int mCenterItemWidth;
    private final int mDecoratedCenterItemWidth;
    private final int mCenterItemHeight;
    private final int mItemHeight;
    private final int mItemSpaceH;
    private final int mItemSpaceV;

    public GalleryRecyclerView(Context context) {
        this(context, null);
    }

    public GalleryRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setClipToPadding(false);

        mItemWidth = getResources().getDimensionPixelSize(R.dimen.gallery_item_width);
        mItemHeight = getResources().getDimensionPixelSize(R.dimen.gallery_item_height);
        mItemSpaceV = getResources().getDimensionPixelSize(R.dimen.gallery_item_space_v);
        mItemSpaceH = getResources().getDimensionPixelSize(R.dimen.gallery_item_space_h);
        mCenterItemWidth = mItemWidth + mItemSpaceH * 2;
        mDecoratedCenterItemWidth = mCenterItemWidth + mItemSpaceH * 2;
        mCenterItemHeight = mItemHeight + mItemSpaceV;

        addItemDecoration(new SpaceItemDecoration());

        mSnapHelper.attachToRecyclerView(this);
        mSnapHelper.attachToRecyclerView(null);
        addOnScrollListener(mScrollListener);
    }

    public void setCurrentItem(int position) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        //noinspection ConstantConditions
        layoutManager.scrollToPositionWithOffset(position, 0);
        mSelectedPosition = position;

        post(() -> {
            ViewHolder viewHolder = findViewHolderForAdapterPosition(position);
            if(mOnItemChangeListener != null) {
                mOnItemChangeListener.onItemChanged(GalleryRecyclerView.this, viewHolder.itemView, mSelectedPosition);
            }
        });
    }

    @SuppressWarnings("unused")
    private void reset() {
        if (mSelectedView != null) {
            mSelectedView.setSelected(false);
            mSelectedView = null;
        }

        mSelectedPosition = RecyclerView.NO_POSITION;
    }

    private boolean mNeedMeasure = true;
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int tempHeightSpec = MeasureSpec.makeMeasureSpec(mCenterItemHeight, EXACTLY) + getPaddingTop() + getPaddingBottom();
        super.onMeasure(widthSpec, tempHeightSpec);

        if(mNeedMeasure) {
            mNeedMeasure = false;

            final int measuredWidth = getMeasuredWidth();
            int paddingLeft = (measuredWidth - mDecoratedCenterItemWidth) >> 1;
            int paddingRight = paddingLeft;

            final int decoratedWidth = measuredWidth - paddingLeft - paddingRight;
            final int widthOffset = decoratedWidth - mDecoratedCenterItemWidth;
            paddingLeft += widthOffset;
            setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());
        }

    }

    private void scaleItems() {
        ViewGroup itemView;
        View child;
        for (int i = 0; i < getChildCount(); i++) {
            itemView = (ViewGroup) getChildAt(i);
            child = itemView.getChildAt(0);
            float fraction = calculateFractionToCenter(itemView);

            ViewGroup.LayoutParams lp = child.getLayoutParams();
            lp.width = (int) (mItemWidth + (mCenterItemWidth - mItemWidth) * (1 - fraction));
            lp.height = (int) (mItemHeight + (mCenterItemHeight - mItemHeight) * (1 - fraction));
            child.setLayoutParams(lp);
            Log.d(TAG, "scaleItems: fraction:" + fraction);
        }

    }

    private float calculateFractionToCenter(View child) {
        int distance = distanceToCenter(child);
        int totalDistance = child.getWidth();

        float fraction = Math.min(1, distance*1.f/totalDistance);
        Log.d(TAG, "calculateFractionToCenter: distance:" + distance + ",totalDistance:" + totalDistance + ",fraction:" + fraction);
        return fraction;
    }

    private int distanceToCenter(View child) {
        int center = getWidth() >> 1;
        return Math.abs((child.getWidth() >> 1) + child.getLeft() - center);
    }

    private void updateSelectedItemView(View selectedView) {
        if (mSelectedView == selectedView) {
            return; // nothing to do
        }
        if (mSelectedView != null) {
            mSelectedView.setSelected(false);
        }
        mSelectedView = selectedView;
        if(mSelectedView != null) {
            mSelectedView.setSelected(true);
        }
    }

    @Override
    public void onScrolled(int dx, int dy) {
        LayoutManager layoutManager = getLayoutManager();
        if(layoutManager != null) {
            View snapView = mSnapHelper.findSnapView(layoutManager);
            updateSelectedItemView(snapView);
            scaleItems();
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            LayoutManager layoutManager = getLayoutManager();
            if(layoutManager != null) {
                View snapView = mSnapHelper.findSnapView(layoutManager);
                if(snapView == null) {
                    return;
                }

                updateSelectedItemView(snapView);

                int selectedPosition = layoutManager.getPosition(snapView);
                if(selectedPosition != mSelectedPosition) {
                    mSelectedPosition = selectedPosition;
                    if(mOnItemChangeListener != null) {
                        mOnItemChangeListener.onItemChanged(GalleryRecyclerView.this, snapView, mSelectedPosition);
                    }
                }
            }
        }
    }

    final class SpaceItemDecoration extends ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
            outRect.left = mItemSpaceH;
            outRect.right = mItemSpaceH;
        }
    }

    public interface OnItemChangeListener {
        /**
         * @param recyclerView The RecyclerView which item view belong to.
         * @param item         The current selected view
         * @param position     The current selected view's position
         */
        void onItemChanged(RecyclerView recyclerView, View item, int position);
    }

    private OnItemChangeListener mOnItemChangeListener;
    public void setOnItemChangeListener(OnItemChangeListener onItemChangeListener) {
        mOnItemChangeListener = onItemChangeListener;
    }

}
