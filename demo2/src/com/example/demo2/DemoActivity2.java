package com.example.demo2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.astuetz.PagerSlidingTabStrip;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DemoActivity2 extends FragmentActivity {
	private PagerSlidingTabStrip tabs;
	private ViewPager pager;
	private MyPagerAdapter adapter;
	private ImageButton ib;
	private ImageButton ib_cllicked;
	private RelativeLayout rl_content;
	private RelativeLayout rl_title_clicked;
	private RelativeLayout ll_clicled;
	private RelativeLayout ll_add;// add
	private DynamicGridView gv_content;
	private BaseGridview gv_add;
	private TextView clicked_title_tv;
	private List<String> contents;
	private List<String> adds;
	private CheeseDynamicAdapter contentAdapter;
	private ArrayAdapter<String> addAdapter;
	private Button bt_com;
	private String[] contentArrays = { "toutiao", "sports", "movie", "picture",
			"politics", "Paid", "Free", "Trending" };
	private String[] addArrays = { "微博", "社会", "NBA", "国际足球", "CBA", "手机",
			"数码", "移动互联", "真话", "游戏", "旅游", "情感", "养生", "教育" };
	private boolean isEdit = false;
	private final static String SPLIT = ",";
	private boolean isFirstLogin = true;
	private boolean isMainTabPage = true;
	private long exitTime = 0;
	private String tag = "DemoActivity2";
	private int currentSelectedItem=-1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
	}

	private void init() {
		isFirstLogin = getLoginState();
		if (isFirstLogin) {
			isFirstLogin = false;
			adds = new ArrayList<String>(Arrays.asList(addArrays));
			contents = new ArrayList<String>(Arrays.asList(contentArrays));
		} else {
			List<String> list1 = getTabTileData();
			List<String> list2 = getAddData();
			contents = new ArrayList<String>(list1);
			if (list2 != null && list2.size() > 0) {
				adds = new ArrayList<String>(list2);
			} else {
				adds = new ArrayList<String>();
			}
		}
		tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		int height = tabs.getLayoutParams().height;
		pager = (ViewPager) findViewById(R.id.pager);
		ib = (ImageButton) findViewById(R.id.ib);
		ib_cllicked = (ImageButton) findViewById(R.id.title_clicked_ib);
		bt_com = (Button) findViewById(R.id.gridview_content_bt);
		ll_add = (RelativeLayout) findViewById(R.id.ll_add);
		rl_content = (RelativeLayout) findViewById(R.id.content);
		ll_clicled = (RelativeLayout) findViewById(R.id.clicked_fl);
		rl_title_clicked = (RelativeLayout) findViewById(R.id.title_clicked);
		clicked_title_tv = (TextView) findViewById(R.id.title_clicked_tv);
		LayoutParams params = rl_title_clicked.getLayoutParams();
		params.height = height;
		rl_title_clicked.setLayoutParams(params);
		gv_add = (BaseGridview) findViewById(R.id.gridview_add);
		adapter = new MyPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(adapter);
		final int pageMargin = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
						.getDisplayMetrics());
		pager.setPageMargin(pageMargin);
		pager.setCurrentItem(0);
		tabs.setViewPager(pager);
		tabs.setIndicatorHeight(20);
		tabs.setIndicatorColor(Color.RED);
		setListener();
	}

	private boolean getLoginState() {
		SharedPreferences sharedPreferences = getSharedPreferences(
				"tabsTileAndAdd", Context.MODE_PRIVATE);
		isFirstLogin = sharedPreferences.getBoolean("loginState", true);
		return isFirstLogin;
	}

	private void setListener() {
		gv_content = (DynamicGridView) findViewById(R.id.dynamic_grid);
//		gv_content.setSelector(R.drawable.content_color);
		contentAdapter = new CheeseDynamicAdapter(this, contents, 4);
		gv_content.setAdapter(contentAdapter);
		gv_content
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						if (!isEdit) {
							isEdit = true;
							bt_com.setVisibility(View.VISIBLE);
							Log.i(tag, "show bt_com");
							gv_content.startEditMode();
							showDelete(position);
							ll_add.setVisibility(View.INVISIBLE);
						}
						return true;
					}
				});
		gv_content.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (!gv_content.isEditMode()) {
					showNotClicked();
					pager.setCurrentItem(position);
					contentAdapter.setSeclection(position);
					contentAdapter.set(contents);
				}
			}
		});
		addAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.grid_item, R.id.griditem_tv, adds);
		gv_add.setAdapter(addAdapter);
		MyOnclickListener onclickListener = new MyOnclickListener();
		ib.setOnClickListener(onclickListener);
		ib_cllicked.setOnClickListener(onclickListener);
		bt_com.setOnClickListener(onclickListener);
		gv_add.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String add = adds.get(position);
				Toast.makeText(getApplicationContext(), "选择添加" + add,
						Toast.LENGTH_SHORT).show();
				adds.remove(add);
				if (adds.size() == 0) {
					ll_add.setVisibility(View.INVISIBLE);
				}
				contents.add(add);
				addAdapter.notifyDataSetChanged();
				contentAdapter.set(contents);
				adapter.notifyDataSetChanged();
				tabs.notifyDataSetChanged();
				int size = contents.size();
				clicked_title_tv.setText(size + getResources().getString(R.string.main_click_tv_title));
			}
		});
	}

	protected void showDelete(final int position) {
		List<ImageView> list = contentAdapter.getImages();
		for (int i = 2; i < list.size(); i++) {
			final ImageView iv = list.get(i);
			iv.setVisibility(View.VISIBLE);
			Log.i(tag, "showDelete");
			if (position != 0 || position != 1) {
				iv.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						contents.clear();
						List<String> items = contentAdapter.getItems();
						contents.addAll(items);
						String s = (String) v.getTag();
						if (!s.equals("toutiao") && !s.equals("sports")) {
							contents.remove(contents.indexOf(s));
							clicked_title_tv.setText(contents.size()
									+ getResources().getString(R.string.main_click_tv_title));
							adds.add(s);
							addAdapter.notifyDataSetChanged();
							contentAdapter.set(contents);
//							contentAdapter.notifyDataSetChanged();
							adapter.notifyDataSetChanged();
							tabs.notifyDataSetChanged();
						}
					}
				});
			}
		}
	}

	protected void hideDelete() {
		List<ImageView> list = contentAdapter.getImages();
		for (ImageView iv : list) {
			iv.setVisibility(View.INVISIBLE);
		}
		Log.i(tag, "hideDelete");
	}

	class MyOnclickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.ib:
				contentAdapter.setSeclection(pager.getCurrentItem());
				contentAdapter.set(contents);
				showClicked();
				break;
			case R.id.title_clicked_ib:
				if (gv_content.isEditMode()) {
					gv_content.stopEditMode();
					hideDelete();
					isEdit = false;
					bt_com.setVisibility(View.INVISIBLE);
					sotrTabs();
				}
				showNotClicked();
				pager.setCurrentItem(pager.getCurrentItem());
				break;
			// click the complete button
			case R.id.gridview_content_bt:
				if (gv_content.isEditMode()) {
					isEdit = false;
					gv_content.stopEditMode();
					bt_com.setVisibility(View.INVISIBLE);
					if (adds.size() > 0) {
						ll_add.setVisibility(View.VISIBLE);
					} else {
						ll_add.setVisibility(View.INVISIBLE);
					}
					gv_content.clearFloatingView();
					hideDelete();
					sotrTabs();
				}
				break;
			}
		}
	}

	private void sotrTabs() {
		contents.clear();
		List<String> items = contentAdapter.getItems();
		contents.addAll(items);
		adapter.notifyDataSetChanged();
		tabs.notifyDataSetChanged();
	}

	private void showNotClicked() {
		rl_content.setVisibility(View.VISIBLE);
		ll_clicled.setVisibility(View.INVISIBLE);
		rl_title_clicked.setVisibility(View.INVISIBLE);
		ll_add.setVisibility(View.INVISIBLE);
		if (gv_content.isEditMode()) {
			gv_content.stopEditMode();
		}
		contentAdapter.set(contents);
		isMainTabPage = true;
	}

	

	private void showClicked() {
//		setNotDeleteItemsBG();
//		setSelectItemsBG();
		rl_title_clicked.setVisibility(View.VISIBLE);
		int size = contents.size();
		clicked_title_tv.setText(size + getResources().getString(R.string.main_click_tv_title));
		rl_content.setVisibility(View.INVISIBLE);
		// contentAdapter.set(contents);
		ll_clicled.setVisibility(View.VISIBLE);
		if (adds.size() > 0) {
			ll_add.setVisibility(View.VISIBLE);
		} else {
			ll_add.setVisibility(View.INVISIBLE);
		}
		isMainTabPage = false;
	}

	/**
	 * set the background of the items which can not be deleted in the gridview.
	 */
	private void setNotDeleteItemsBG(){
		gv_content.getChildAt(0).setBackgroundColor(Color.LTGRAY);
		gv_content.getChildAt(1).setBackgroundColor(Color.LTGRAY);
	}
	/**
	 * set the background of the items which is selected in the gridview when use switch the UI from main page to the tabs sort page.
	 */
	private void setSelectItemsBG(){
		int item=pager.getCurrentItem();
		if(currentSelectedItem!=-1){
			gv_content.getChildAt(currentSelectedItem).setBackgroundColor(Color.LTGRAY);
			gv_content.getChildAt(item).setBackgroundColor(Color.RED);
			currentSelectedItem=item;
		}else{
			gv_content.getChildAt(item).setBackgroundColor(Color.RED);
			currentSelectedItem=item;
		}
	}
	

	public class MyPagerAdapter extends FragmentPagerAdapter {
		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return contents.get(position);
		}

		@Override
		public int getCount() {
			return contents.size();
		}

		@Override
		public Fragment getItem(int position) {
			return SuperAwesomeCardFragment.newInstance(position);
		}

	}

	@Override
	protected void onResume() {
		if (!isMainTabPage) {
			bt_com.setVisibility(View.INVISIBLE);
			Log.i(tag, "onResume  hide bt_com");
			hideDelete();
			showClicked();
		} else {
			showNotClicked();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (gv_content.isEditMode()) {
			gv_content.stopEditMode();
			isEdit = false;
		}
		saveTabTileAndAddData();
		Log.i(tag, "onPause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		saveTabTileAndAddData();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		saveTabTileAndAddData();
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		if (!isMainTabPage) {
			if (gv_content.isEditMode()) {
				gv_content.stopEditMode();
				hideDelete();
				isEdit = false;
				bt_com.setVisibility(View.INVISIBLE);
				if (adds.size() > 0) {
					ll_add.setVisibility(View.VISIBLE);
				} else {
					ll_add.setVisibility(View.INVISIBLE);
				}
			} else {
				showNotClicked();
				saveTabTileAndAddData();
			}
			gv_content.clearFloatingView();
		} else {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_toast_exit),
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				super.onBackPressed();
			}
		}

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * 将tab栏的标题和add的内容在程序退出前以字符串的形式保存在SharedPreferences里。
	 */
	private void saveTabTileAndAddData() {
		SharedPreferences sharedPreferences = getSharedPreferences(
				"tabsTileAndAdd", Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.clear();
		editor.putBoolean("loginState", isFirstLogin);
		editor.putString("tabs", getTabsTitleString(contents));
		editor.putString("adds", getTabsTitleString(adds));
		editor.commit();
	}

	/**
	 * 从SharedPreferences里取出tab栏的标题内容
	 * 
	 * @return
	 */
	private List<String> getAddData() {
		SharedPreferences sharedPreferences = getSharedPreferences(
				"tabsTileAndAdd", Context.MODE_PRIVATE);
		String add = sharedPreferences.getString("adds", "");
		isFirstLogin = sharedPreferences.getBoolean("loginState", false);
		if (!TextUtils.isEmpty(add)) {
			return getTabsTitleList(add);
		}
		return null;
	}

	/**
	 * 从SharedPreferences里取出tab栏的标题内容
	 * 
	 * @return
	 */
	private List<String> getTabTileData() {
		SharedPreferences sharedPreferences = getSharedPreferences(
				"tabsTileAndAdd", Context.MODE_PRIVATE);
		String tabs = sharedPreferences.getString("tabs", "");
		if (!TextUtils.isEmpty(tabs)) {
			return getTabsTitleList(tabs);
		}
		return null;
	}

	/**
	 * 将list形式转换成String形似。
	 * 
	 * @return
	 */
	private String getTabsTitleString(List<String> list) {
		StringBuffer sb = new StringBuffer();
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == null || list.get(i) == "") {
					continue;
				}
				sb.append(list.get(i));
				sb.append(SPLIT);
			}
		}
		return sb.toString();
	}

	/**
	 * 将内容从字符串的形式转换成list
	 * 
	 * @return
	 */
	private List<String> getTabsTitleList(String str) {
		String ss[] = str.split(",");
		List<String> list = Arrays.asList(ss);
		return list;
	}
}
