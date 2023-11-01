package com.quran.labs.androidquran.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.dao.translation.TranslationItem;
import com.quran.labs.androidquran.dao.translation.TranslationRowData;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.UnicastSubject;
import java.util.ArrayList;
import java.util.List;

public class TranslationsAdapter extends RecyclerView.Adapter<TranslationsAdapter.TranslationViewHolder> {

    private DownloadedItemActionListener downloadedItemActionListener = new DownloadedItemActionListenerImpl();
    private UnicastSubject<TranslationRowData> onClickDownloadSubject = UnicastSubject.create();
    private UnicastSubject<TranslationRowData> onClickRemoveSubject = UnicastSubject.create();
    private UnicastSubject<TranslationRowData> onClickRankUpSubject = UnicastSubject.create();
    private UnicastSubject<TranslationRowData> onClickRankDownSubject = UnicastSubject.create();
    private List<TranslationRowData> translations = new ArrayList<>();
    private TranslationItem selectedItem = null;

    @Override
    public TranslationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new TranslationViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(TranslationViewHolder holder, int position) {
        TranslationRowData rowItem = translations.get(position);

        switch (holder.getItemViewType()) {
            case R.layout.translation_row:
                TranslationItem item = (TranslationItem) rowItem;
                holder.translationItem = item;
                holder.itemView.setActivated(selectedItem != null && item.getTranslation().getId() == selectedItem.getTranslation().getId());
                holder.translationTitle.setText(item.name());

                if (TextUtils.isEmpty(item.getTranslation().getTranslatorNameLocalized())) {
                    holder.translationInfo.setText(item.getTranslation().getTranslator());
                } else {
                    holder.translationInfo.setText(item.getTranslation().getTranslatorNameLocalized());
                }

                ImageView leftImage = holder.leftImage;
                ImageView rightImage = holder.rightImage;

                if (item.exists()) {
                    rightImage.setVisibility(View.GONE);
                    holder.itemView.setOnLongClickListener(holder.actionMenuListener);

                    if (item.needsUpgrade()) {
                        leftImage.setImageResource(com.quran.mobile.feature.downloadmanager.R.drawable.ic_download);
                        leftImage.setVisibility(View.VISIBLE);
                        holder.translationInfo.setText(R.string.update_available);
                    } else {
                        leftImage.setVisibility(View.GONE);
                    }
                } else {
                    leftImage.setVisibility(View.GONE);
                    rightImage.setImageResource(com.quran.mobile.feature.downloadmanager.R.drawable.ic_download);
                    rightImage.setOnClickListener(null);
                    rightImage.setVisibility(View.VISIBLE);
                    rightImage.setClickable(false);
                    rightImage.setContentDescription(null);
                }
                break;

            case R.layout.translation_sep:
                holder.itemView.setActivated(false);
                holder.separatorText.setText(rowItem.name());
                break;
        }
    }

    @Override
    public int getItemCount() {
        return translations.size();
    }

    @Override
    public int getItemViewType(int position) {
        return translations.get(position).isSeparator() ? R.layout.translation_sep : R.layout.translation_row;
    }

    public Observable<TranslationRowData> getOnClickDownloadSubject() {
        return onClickDownloadSubject.hide();
    }

    public Observable<TranslationRowData> getOnClickRemoveSubject() {
        return onClickRemoveSubject.hide();
    }

    public Observable<TranslationRowData> getOnClickRankUpSubject() {
        return onClickRankUpSubject.hide();
    }

    public Observable<TranslationRowData> getOnClickRankDownSubject() {
        return onClickRankDownSubject.hide();
    }

    public void setTranslations(List<TranslationRowData> data) {
        translations = data;
    }

    public List<TranslationRowData> getTranslations() {
        return translations;
    }

    public void setSelectedItem(TranslationItem selectedItem) {
        this.selectedItem = selectedItem;
        notifyDataSetChanged();
    }

    private class DownloadedItemActionListenerImpl implements DownloadedItemActionListener {
        @Override
        public void handleDeleteItemAction() {
            if (selectedItem != null) {
                onClickRemoveSubject.onNext(selectedItem);
            }
        }

        @Override
        public void handleRankUpItemAction() {
            if (selectedItem != null) {
                onClickRankUpSubject.onNext(selectedItem);
            }
        }

        @Override
        public void handleRankDownItemAction() {
            if (selectedItem != null) {
                onClickRankDownSubject.onNext(selectedItem);
            }
        }
    }

    class TranslationViewHolder extends RecyclerView.ViewHolder {
        TextView translationTitle;
        TextView translationInfo;
        ImageView leftImage;
        ImageView rightImage;
        TextView separatorText;
        TranslationItem translationItem;

        View.OnLongClickListener actionMenuListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (translationItem != null) {
                    downloadedItemActionListener.startMenuAction(translationItem, downloadedItemActionListener);
                }
                return true;
            }
        };

        TranslationViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == R.layout.translation_row) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadedItemActionListener.finishMenuAction();
                        if (translationItem != null && (!translationItem.exists() || translationItem.needsUpgrade())) {
                            onClickDownloadSubject.onNext(translationItem);
                        }
                    }
                });
            }
        }
    }
}
