package com.halo.pullrefresh;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.pullrefresh.OnLoadMoreListener;
import com.pullrefresh.OnRefreshListener;
import com.pullrefresh.PullToRefreshLayout;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PullListFragment extends Fragment {

    PullToRefreshLayout mPullListView;
    private ListView mListView;
    private LinkedList<String> mListItems;
    private ListAdapter mAdapter;
    private int mCurIndex;

    public PullListFragment() {
        // Required empty public constructor
    }

    public static PullListFragment newInstance() {
        PullListFragment fragment = new PullListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pull_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
    }

    private <T> T findViewById(int id) {
        return (T) getView().findViewById(id);
    }

    private void initView() {
        mPullListView = findViewById(R.id.swipeToLoadLayout);
        // 上拉加载不可用
        mPullListView.setPullLoadEnabled(true);

        mCurIndex = 1;
        mListItems = new LinkedList<>();
        mListItems.addAll(createData(mCurIndex));
        mAdapter = new ListAdapter(getActivity(), mListItems);

        // 得到实际的View
        mListView = findViewById(R.id.swipe_target);
        // 绑定数据
        mListView.setAdapter(mAdapter);
        // 设置下拉刷新的listener
        mPullListView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onPullDownToRefresh(PullToRefreshLayout refreshView) {
                mPullListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCurIndex = 1;
                        mListItems.clear();
                        mListItems.addAll(createData(mCurIndex));
                        mAdapter.notifyDataSetChanged();
                        mPullListView.setPullDownRefreshComplete();
                    }
                }, 2000);
            }
        });
        mPullListView.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onPullUpToLoadMore(PullToRefreshLayout refreshView) {
                mPullListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCurIndex = mCurIndex + 1;
                        mListItems.addAll(createData(mCurIndex));
                        mAdapter.notifyDataSetChanged();
                        mPullListView.setPullUpRefreshComplete();
                    }
                }, 2000);
            }
        });
        // 自动刷新
//        mPullListView.doPullRefreshing(true, 500);
    }

    private List<String> createData(int current) {
        List<String> list = new ArrayList<>();
        int pageSize = 10;
        for(int i = 0; i < pageSize; i++) {
            list.add(String.valueOf(pageSize * (current - 1) + i));
        }
        return list;
    }

    private class ListAdapter extends BaseAdapter {
        private List<String> list;
        private Context context;
        private LayoutInflater mInflater;

        public ListAdapter(Context context, List<String> list) {
            this.context = context;
            this.list = list;
            mInflater = LayoutInflater.from(context);
        }


        @Override
        public int getCount() {
            if(list == null) {
                return 0;
            }
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.item_refresh, parent, false);
            }
            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(list.get(position));
            return convertView;
        }
    }

}
