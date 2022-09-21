package com.ctrun.view.cateye.gallery.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.ctrun.view.cateye.gallery.bean.CinemaMovieBean;
import com.ctrun.view.cateye.gallery.databinding.ActivityGalleryBinding;
import com.ctrun.view.cateye.gallery.databinding.RecycleItemGalleryBinding;
import com.ctrun.view.cateye.gallery.util.ImageSizeUtils;
import com.ctrun.view.cateye.gallery.util.JsonFileUtils;
import com.ctrun.view.cateye.gallery.widget.GalleryRecyclerView;
import com.google.gson.Gson;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jp.wasabeef.glide.transformations.internal.FastBlur;

/**
 * @author ctrun on 2022/8/5.
 */
public class GalleryActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, GalleryActivity.class);
        context.startActivity(intent);
    }

    private ActivityGalleryBinding mBinding;

    private final ArrayList<CinemaMovieBean.DataBean.MoviesBean> mData = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbarActionbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }


        String jsonData = JsonFileUtils.readJsonFromAsset(this, "cateye_cinema_of_movies.json");
        CinemaMovieBean bean = new Gson().fromJson(jsonData, CinemaMovieBean.class);
        mData.addAll(bean.data.movies);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mBinding.rvGallery.setLayoutManager(layoutManager);
        mBinding.rvGallery.setOnItemChangeListener(mOnItemChangeListener);
        mBinding.rvGallery.setAdapter(mAdapter);

        if (mAdapter.getItemCount() > 1) {
            mBinding.rvGallery.setCurrentItem(1);
        }
    }

    private final GalleryRecyclerView.OnItemChangeListener mOnItemChangeListener = new GalleryRecyclerView.OnItemChangeListener() {
        @Override
        public void onItemChanged(RecyclerView recyclerView, View item, int position) {
            if(isFinishing()) {
                return;
            }

            CinemaMovieBean.DataBean.MoviesBean movie = mData.get(position);

            final String imgUrl = ImageSizeUtils.makeSmallUrlSquare(movie.img, 244);
            Glide.with(GalleryActivity.this).load(imgUrl).listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (resource instanceof BitmapDrawable) {
                                Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                                Disposable disposable =
                                        Observable.just(bitmap)
                                                .map(originalBitmap -> {
                                                    Bitmap resultBitmap = Bitmap.createBitmap(originalBitmap, 0, originalBitmap.getHeight()/4, originalBitmap.getWidth(), originalBitmap.getHeight()/2);

                                                    if (resultBitmap != null) {
                                                        resultBitmap = FastBlur.blur(resultBitmap, 30, false);
                                                    }

                                                    return resultBitmap;
                                                })
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(resultBitmap -> {
                                                    if(resultBitmap != null) {
                                                        mBinding.clMovieLayout.setBackground(new BitmapDrawable(getResources(), resultBitmap));
                                                    }
                                                }, throwable -> {
                                                    //ignore
                                                });
                            }
                            return true;
                        }
                    }).submit();
            }
    };

    RecyclerView.Adapter<ItemViewHolder> mAdapter = new RecyclerView.Adapter<ItemViewHolder>() {
        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecycleItemGalleryBinding binding = RecycleItemGalleryBinding.inflate(getLayoutInflater(), parent, false);
            ItemViewHolder viewHolder = new ItemViewHolder(binding);
            viewHolder.itemView.setOnClickListener(mItemClickListener);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            CinemaMovieBean.DataBean.MoviesBean bean = mData.get(position);
            holder.mBinding.ivDiscount.setVisibility(bean.preferential==1 ? View.VISIBLE : View.INVISIBLE);

            if (bean.labelResource == null || bean.labelResource.isEmpty()) {
                holder.mBinding.ivType.setVisibility(View.GONE);
            } else {
                holder.mBinding.ivType.setVisibility(View.VISIBLE);

                CinemaMovieBean.DataBean.MoviesBean.LabelResource labelResource = bean.labelResource.get(0);
                String imgUrl = "";
                if (labelResource != null && labelResource.picImg != null) {
                    imgUrl = labelResource.picImg.url;
                }

                Glide.with(GalleryActivity.this)
                        .load(imgUrl)
                        .apply(RequestOptions.centerCropTransform().dontAnimate())
                        .into(holder.mBinding.ivType);
            }

            //图片地址不能直接使用，需要进行转换
            final String imgUrl = ImageSizeUtils.makeSmallUrlSquare(bean.img, 244);
            Glide.with(GalleryActivity.this)
                    .load(imgUrl)
                    .apply(RequestOptions.centerCropTransform().dontAnimate())
                    .into(holder.mBinding.ivImage);

            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        final View.OnClickListener mItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) v.getTag();
                mBinding.rvGallery.smoothScrollToPosition(position);
            }
        };
    };

    static class ItemViewHolder extends RecyclerView.ViewHolder {

        RecycleItemGalleryBinding mBinding;
        public ItemViewHolder(RecycleItemGalleryBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }
}
