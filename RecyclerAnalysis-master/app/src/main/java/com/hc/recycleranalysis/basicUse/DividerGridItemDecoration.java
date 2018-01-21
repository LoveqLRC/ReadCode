package com.hc.recycleranalysis.basicUse;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.State;
import android.view.View;

public class DividerGridItemDecoration extends ItemDecoration {

    private Drawable mDivider;
    private int[] attrs = new int[]{
            android.R.attr.listDivider
    };

    public DividerGridItemDecoration(Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        mDivider = typedArray.getDrawable(0);
        typedArray.recycle();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, State state) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            drawVertical(c, child);
            drawHorizontal(c, child);
        }
    }

    private void drawVertical(Canvas c, View child) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        int left = child.getLeft() - params.leftMargin;
        int right = child.getRight() + params.rightMargin + mDivider.getIntrinsicWidth();
        int top = child.getBottom() + params.bottomMargin;
        int bottom = top + mDivider.getIntrinsicHeight();
        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(c);
    }

    private void drawHorizontal(Canvas c, View child) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        int left = child.getRight() + params.rightMargin;
        int right = left + mDivider.getIntrinsicWidth();
        int top = child.getTop() - params.topMargin;
        int bottom = child.getBottom() + params.bottomMargin;
        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(c);

    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
        // 四个方向的偏移值
        int right = mDivider.getIntrinsicWidth();
        int bottom = mDivider.getIntrinsicHeight();

        int position = ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition();

        // 是否是最后一列
        if (isLastColumn(position, parent)) {
            right = 0;
        }
        // 是否是最后一行
        if (isLastRow(position, parent)) {
            bottom = 0;
        }
        
        outRect.set(0, 0, right, bottom);
    }

    /**
     * 是否是最后一列
     */
    public boolean isLastColumn(int itemPosition, RecyclerView parent) {
        int spanCount = getSpanCount(parent);
        if ((itemPosition + 1) % spanCount == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否是最后一行
     */
    public boolean isLastRow(int itemPosition, RecyclerView parent) {

        int spanCount = getSpanCount(parent);

        int childCount = parent.getAdapter().getItemCount();

        int rowNumber = childCount % spanCount == 0 ? childCount / spanCount : (childCount / spanCount) + 1;

        int dividerItemPosition = (rowNumber - 1) * spanCount - 1;

        if (itemPosition > dividerItemPosition) {
            return true;
        }

        return false;
    }

    /**
     * 获取一行有多少列
     */
    public int getSpanCount(RecyclerView parent) {
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

        if (layoutManager instanceof GridLayoutManager) {
            // 获取一行的spanCount
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            int spanCount = gridLayoutManager.getSpanCount();
            return spanCount;
        }
        return 1;
    }
}
