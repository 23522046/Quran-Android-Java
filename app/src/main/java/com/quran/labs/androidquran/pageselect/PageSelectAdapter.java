package com.quran.labs.androidquran.pageselect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.quran.labs.androidquran.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PageSelectAdapter extends PagerAdapter {
    private final LayoutInflater inflater;
    private final int width;
    private final SelectionHandler selectionHandler;
    private final List<PageTypeItem> items = new ArrayList<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Object tag = ((View) v.getParent()).getTag();
            if (tag != null) {
                selectionHandler.onPageTypeSelected(tag.toString());
            }
        }
    };

    public PageSelectAdapter(LayoutInflater inflater, int width, SelectionHandler selectionHandler) {
        this.inflater = inflater;
        this.width = width;
        this.selectionHandler = selectionHandler;
    }

    public void replaceItems(List<PageTypeItem> updates, ViewPager pager) {
        items.clear();
        items.addAll(updates);
        for (PageTypeItem item : items) {
            View view = pager.findViewWithTag(item.getPageType());
            if (view != null) {
                updateView(view, item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
        return obj == view;
    }

    private void updateView(View view, PageTypeItem data) {
        TextView title = view.findViewById(R.id.title);
        TextView description = view.findViewById(R.id.description);
        FloatingActionButton fab = view.findViewById(R.id.fab);
        ImageView image = view.findViewById(R.id.preview);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        title.setText(data.getTitle());
        description.setText(data.getDescription());
        fab.setOnClickListener(listener);

        if (data.getPreviewImage() != null) {
            progressBar.setVisibility(View.GONE);
            readImage(data.getPreviewImage().getPath(), new WeakReference<>(image));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            image.setImageBitmap(null);
        }
    }

    private void readImage(String path, WeakReference<ImageView> imageRef) {
        compositeDisposable.add(
                Maybe.fromCallable(() -> {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ALPHA_8;
                    return BitmapFactory.decodeFile(path, options);
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(bitmap -> {
                            ImageView imageView = imageRef.get();
                            if (imageView != null) {
                                imageView.setImageBitmap(bitmap);
                            }
                        })
        );
    }

    public void cleanUp() {
        compositeDisposable.clear();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = inflater.inflate(R.layout.page_select_page, container, false);
        PageTypeItem item = items.get(position);
        updateView(view, item);
        view.setTag(item.getPageType());
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public interface SelectionHandler {
        void onPageTypeSelected(String type);
    }
}
