package com.pandaq.pandaeye.ui.douban;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pandaq.pandaeye.R;
import com.pandaq.pandaeye.adapters.MovieListAdapter;
import com.pandaq.pandaeye.presenter.douban.DouBanMoviePresenter;
import com.pandaq.pandaeye.rxbus.RxBus;
import com.pandaq.pandaeye.rxbus.RxConstants;
import com.pandaq.pandaeye.ui.ImplView.IDoubanFrag;
import com.pandaq.pandaeye.ui.base.BaseFragment;
import com.pandaq.pandaqlib.magicrecyclerView.BaseItem;
import com.pandaq.pandaqlib.magicrecyclerView.BaseRecyclerAdapter;
import com.pandaq.pandaqlib.magicrecyclerView.MagicRecyclerView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by PandaQ on 2016/9/8.
 * email : 767807368@qq.com
 * 豆瓣top250电影列表Fragment
 */
public class MovieListFragment extends BaseFragment implements IDoubanFrag, SwipeRefreshLayout.OnRefreshListener {
    @BindView(R.id.movie_list)
    MagicRecyclerView mMovieList;
    @BindView(R.id.srl_refresh)
    SwipeRefreshLayout mSrlRefresh;
    private MovieListAdapter mMovieListAdapter;
    private DouBanMoviePresenter mPresenter = new DouBanMoviePresenter(this);
    private boolean loading = false;
    private Disposable mDisposable;
    private StaggeredGridLayoutManager mStaggeredGridLayoutManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.movielist_fragment, container, false);
        ButterKnife.bind(this, view);
        initView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        onHiddenChanged(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSrlRefresh.setRefreshing(false);
        mPresenter.dispose();
        onHiddenChanged(true);
    }

    private void initView() {
        mStaggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mMovieList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (mMovieList.loadAble()) {
                    loadMoreData();
                }
            }
        });
        mMovieList.setItemAnimator(new DefaultItemAnimator());
        //屏蔽掉默认的动画，房子刷新时图片闪烁
        mMovieList.getItemAnimator().setChangeDuration(0);
        mMovieList.setLayoutManager(mStaggeredGridLayoutManager);
        mSrlRefresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.white_FFFFFF));
        mSrlRefresh.setOnRefreshListener(this);
        mSrlRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        refreshData();
        mPresenter.loadCache();
        mMovieList.addOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, BaseItem baseItem, View view) {
            }
        });
        View footerView = mMovieList.getFooterView();
        if (footerView != null) {
            loadMoreData();
        }
    }

    @Override
    public void showProgressBar() {
        //显示加载进度条
        if (!mSrlRefresh.isRefreshing()) {
            mSrlRefresh.setRefreshing(true);
        }
    }

    @Override
    public void hideProgressBar() {
        //隐藏加载进度条
        mSrlRefresh.setRefreshing(false);
    }

    @Override
    public void showEmptyMessage() {
        //显示无信息时的界面View
    }

    @Override
    public void hideEmptyMessage() {
        //显示信息时隐藏掉无信息时的界面View
    }

    @Override
    public void loadMoreData() {
        if (!loading) {
            //加载要显示的数据
            mPresenter.loadMoreData();
            loading = true;
        }
    }

    @Override
    public void refreshData() {
        //刷新数据
        mPresenter.refreshData();
    }

    @Override
    public void loadSuccessed(ArrayList<BaseItem> movieSubjects) {
        loading = false;
        mMovieListAdapter.addBaseDatas(movieSubjects);
    }

    @Override
    public void loadFail(String errMsg) {
        //SnackBar提示错误信息
        loading = false;
    }

    @Override
    public void refreshSucceed(ArrayList<BaseItem> movieSubjects) {
        //如果是刚进入时刷新则新建一个Adapter，否则只是更新数据源
        if (mMovieListAdapter == null) {
            mMovieListAdapter = new MovieListAdapter(this);
            mMovieListAdapter.setBaseDatas(movieSubjects);
            mMovieList.setAdapter(mMovieListAdapter);
        } else {
            mMovieListAdapter.setBaseDatas(movieSubjects);
        }
    }

    @Override
    public void refreshFail(String errMsg) {
        //SnackBar提示错误信息

    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden && mSrlRefresh.isRefreshing()) { // 隐藏的时候停止 SwipeRefreshLayout 转动
            mSrlRefresh.setRefreshing(false);
        }
        if (!hidden) {
            RxBus.getDefault()
                    .toObservableWithCode(RxConstants.BACK_PRESSED_CODE, String.class)
                    .subscribeWith(new Observer<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            mDisposable = d;
                        }

                        @Override
                        public void onNext(String value) {
                            if (value.equals(RxConstants.BACK_PRESSED_DATA) && mMovieList != null) {
                                //滚动到顶部
                                mStaggeredGridLayoutManager.smoothScrollToPosition(mMovieList, null, 0);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            if (mDisposable != null && !mDisposable.isDisposed()) {
                mDisposable.dispose();
            }
        }
    }
}
