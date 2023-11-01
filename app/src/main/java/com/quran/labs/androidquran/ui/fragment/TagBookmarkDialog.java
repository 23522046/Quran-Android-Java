package com.quran.labs.androidquran.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;
import com.quran.data.model.bookmark.Tag;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.presenter.bookmark.TagBookmarkPresenter;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;

public class TagBookmarkDialog extends DialogFragment {

    private TagsAdapter adapter;
    
    @Inject
    TagBookmarkPresenter tagBookmarkPresenter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (shouldInject()) {
            ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
        }
    }

    public boolean shouldInject() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            long[] bookmarkIds = args.getLongArray(EXTRA_BOOKMARK_IDS);
            if (bookmarkIds != null) {
                tagBookmarkPresenter.setBookmarksMode(bookmarkIds);
            }
        }
    }

    private ListView createTagsListView() {
        Context context = requireContext();
        adapter = new TagsAdapter(context, tagBookmarkPresenter);
        ListView listView = new ListView(context);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Tag tag = adapter.getItem(position);
                boolean isChecked = tagBookmarkPresenter.toggleTag(tag.getId());
                Object viewTag = view.getTag();
                if (viewTag instanceof ViewHolder) {
                    ((ViewHolder) viewTag).checkBox.setChecked(isChecked);
                }
            }
        });
        return listView;
    }

    public void showAddTagDialog() {
        Context context = getActivity();
        if (context instanceof OnBookmarkTagsUpdateListener) {
            ((OnBookmarkTagsUpdateListener) context).onAddTagSelected();
        }
    }

    public void setData(List<Tag> tags, HashSet<Long> checkedTags) {
        adapter.setData(tags, checkedTags);
        adapter.notifyDataSetChanged();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new Builder(requireActivity());
        builder.setView(createTagsListView());
        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tagBookmarkPresenter.saveChanges();
                dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        tagBookmarkPresenter.bind(this);
        Dialog dialog = getDialog();
        if (dialog instanceof AlertDialog) {
            final AlertDialog alertDialog = (AlertDialog) dialog;
            alertDialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tagBookmarkPresenter.saveChanges();
                    alertDialog.dismiss();
                }
            });
        }
    }

    @Override
    public void onStop() {
        tagBookmarkPresenter.unbind(this);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // If in dialog mode, don't do anything (or else it will cause an exception)
        if (showsDialog) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            return createTagsListView();
        }
    }

    public interface OnBookmarkTagsUpdateListener {
        void onAddTagSelected();
    }

    private static final String TAG = "TagBookmarkDialog";
    private static final String EXTRA_BOOKMARK_IDS = "bookmark_ids";

    public static TagBookmarkDialog newInstance(long bookmarkId) {
        return newInstance(new long[]{bookmarkId});
    }

    public static TagBookmarkDialog newInstance(long[] bookmarkIds) {
        Bundle args = new Bundle();
        args.putLongArray(EXTRA_BOOKMARK_IDS, bookmarkIds);
        TagBookmarkDialog dialog = new TagBookmarkDialog();
        dialog.setArguments(args);
        return dialog;
    }

    private class TagsAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private final TagBookmarkPresenter tagBookmarkPresenter;
        private final String newTagString;
        private List<Tag> tags;
        private HashSet<Long> checkedTags;

        public TagsAdapter(Context context, TagBookmarkPresenter presenter) {
            inflater = LayoutInflater.from(context);
            tagBookmarkPresenter = presenter;
            newTagString = context.getString(R.string.new_tag);
            tags = emptyList();
            checkedTags = new HashSet<>();
        }

        public void setData(List<Tag> tags, HashSet<Long> checkedTags) {
            this.tags = (tags != null) ? tags : emptyList();
            this.checkedTags = checkedTags;
        }

        @Override
        public int getCount() {
            return tags.size();
        }

        @Override
        public Tag getItem(int position) {
            return tags.get(position);
        }

        @Override
        public long getItemId(int position) {
            return tags.get(position).getId();
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.tag_row, parent, false);
                holder = new ViewHolder();
                holder.checkBox = view.findViewById(R.id.tag_checkbox);
                holder.tagName = view.findViewById(R.id.tag_name);
                holder.addImage = view.findViewById(R.id.tag_add_image);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Tag tag = getItem(position);
            if (tag.getId() == -1L) {
                holder.addImage.setVisibility(View.VISIBLE);
                holder.checkBox.setVisibility(View.GONE);
                holder.tagName.setText(newTagString);
            } else {
                holder.addImage.setVisibility(View.GONE);
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(checkedTags.contains(tag.getId()));
                holder.tagName.setText(tag.getName());
                holder.checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tagBookmarkPresenter.toggleTag(tag.getId());
                    }
                });
            }
            return view;
        }
    }

    private static class ViewHolder {
        CheckBox checkBox;
        TextView tagName;
        ImageView addImage;
    }
}
