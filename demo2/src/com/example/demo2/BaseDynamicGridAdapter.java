package com.example.demo2;

import android.content.Context;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Author: alex askerov
 * Date: 9/7/13
 * Time: 10:49 PM
 */
public abstract class BaseDynamicGridAdapter extends BaseAdapter {
    private Context mContext;
    public static final int INVALID_ID = -1;
    private HashMap<Object, Integer> mIdMap = new HashMap<Object, Integer>();
    private ArrayList<Object> mItems = new ArrayList<Object>();
    private int mColumnCount;
	protected BaseDynamicGridAdapter(Context context, int columnCount) {
        this.mContext = context;
        this.mColumnCount = columnCount;
    }

    public BaseDynamicGridAdapter(Context context, List<?> items, int columnCount) {
        mContext = context;
        mColumnCount = columnCount;
        init(items);
    }

    private void init(List<?> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
    }


    public void set(List<?> items) {
        clear();
        init(items);
        notifyDataSetChanged();
    }

    public void clear() {
        clearStableIdMap();
        mItems.clear();
        notifyDataSetChanged();
    }

    public void add(Object item) {
        addStableId(item);
        mItems.add(item);
        notifyDataSetChanged();
    }


    public void add(List<?> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
        notifyDataSetChanged();
    }


    public void remove(Object item) {
        mItems.remove(item);
        removeStableID(item);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    /**
     * @return return columns number for GridView. Need for compatibility
     *         (@link android.widget.GridView#getNumColumns() requires api 11)
     */
    public int getColumnCount() {
        return mColumnCount;
    }

    public void setColumnCount(int columnCount) {
        this.mColumnCount = columnCount;
        notifyDataSetChanged();
    }

    /**
     * Determines how to reorder items dragged from <code>originalPosition</code> to <code>newPosition</code>
     *
     * @param originalPosition
     * @param newPosition
     */
    public void reorderItems(int originalPosition, int newPosition) {
        DynamicGridUtils.reorder(mItems, originalPosition, newPosition);
        notifyDataSetChanged();
    }

    public List getItems() {
        return mItems;
    }

    protected Context getContext() {
        return mContext;
    }
    
    //******add
    /**
     * Adapter must have stable id
     *
     * @return
     */
    @Override
    public final boolean hasStableIds() {
        return true;
    }

    /**
     * creates stable id for object
     *
     * @param item
     */
    protected void addStableId(Object item) {
        int newId = (int) getItemId(getCount() - 1);
        newId++;
        mIdMap.put(item, newId);
    }

    /**
     * create stable ids for list
     *
     * @param items
     */
    protected void addAllStableId(List<?> items) {
        int startId = (int) getItemId(getCount() - 1);
        startId++;
        for (int i = startId; i < items.size(); i++) {
            mIdMap.put(items.get(i), i);
        }
    }

    /**
     * get id for position
     *
     * @param position
     * @return
     */
    @Override
    public final long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        Object item = getItem(position);
        return mIdMap.get(item);
    }

    /**
     * clear stable id map
     * should called when clear adapter data;
     */
    protected void clearStableIdMap() {
        mIdMap.clear();
    }

    /**
     * remove stable id for <code>item</code>. Should called on remove data item from adapter
     *
     * @param item
     */
    protected void removeStableID(Object item) {
        mIdMap.remove(item);
    }
    //**********add over
}
