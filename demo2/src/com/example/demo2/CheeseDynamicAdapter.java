package com.example.demo2;

/**
 * Author: alex askerov
 * Date: 9/9/13
 * Time: 10:52 PM
 */
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: alex askerov Date: 9/7/13 Time: 10:56 PM
 */
public class CheeseDynamicAdapter extends BaseDynamicGridAdapter {

	List<ImageView> images;
	private int clickTemp = -1;
private String tag="CheeseDynamicAdapter";
	// 标识选择的Item

	public void setSeclection(int position) {
		clickTemp = position;
	}

	public List<ImageView> getImages() {
//		notifyDataSetChanged();
		return images;
	}

	public void setImages(List<ImageView> images) {
		this.images = images;
	}

	public CheeseDynamicAdapter(Context context, List<?> items, int columnCount) {
		super(context, items, columnCount);
		images = new ArrayList<ImageView>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CheeseViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(
					R.layout.item_grid, null);
			holder = new CheeseViewHolder(convertView);
			convertView.setTag(holder);
			Log.i(tag, "getview convertView == null");
		} else {
			holder = (CheeseViewHolder) convertView.getTag();
			Log.i(tag, "getview convertView != null");
		}
		holder.build(getItem(position).toString(),position);
		return convertView;
	}

	private class CheeseViewHolder {
		private TextView titleText;
		private ImageView image;

		private CheeseViewHolder(View view) {
			titleText = (TextView) view.findViewById(R.id.item_title);
			image = (ImageView) view.findViewById(R.id.item_img);
		}

		void build(String title,int index) {
			Log.i(tag, "getview convertView index:"+index);
			titleText.setText(title);
			if(index==0||index==1){
				Log.i(tag, "");
//				titleText.setBackgroundColor(Color.argb(255, 211, 211, 211));
			}else{
				image.setImageResource(R.drawable.night_biz_news_column_edit_item_del);
			}
			image.setTag(title);
			images.add(image);
		}
	}
}