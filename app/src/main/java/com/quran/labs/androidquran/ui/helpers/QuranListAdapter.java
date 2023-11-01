import android.content.Context;
import android.graphics.PorterDuff;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.util.SparseBooleanArray;
import androidx.recyclerview.widget.RecyclerView;
import com.quran.data.model.bookmark.Tag;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.ui.QuranActivity;
import com.quran.labs.androidquran.util.LocaleUtil;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.view.JuzView;
import com.quran.labs.androidquran.view.TagsViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class QuranListAdapter extends RecyclerView.Adapter<QuranListAdapter.HeaderHolder>
        implements View.OnClickListener, View.OnLongClickListener {

    private Context context;
    private RecyclerView recyclerView;
    private QuranRow[] elements;
    private boolean isEditable;
    private LayoutInflater inflater;
    private SparseBooleanArray checkedState = new SparseBooleanArray();
    private Locale locale;
    private Map<Long, Tag> tagMap;
    private boolean showTags = false;
    private boolean showDate = false;
    private QuranTouchListener touchListener;

    public QuranListAdapter(Context context, RecyclerView recyclerView, QuranRow[] elements, boolean isEditable) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.elements = elements;
        this.isEditable = isEditable;
        this.inflater = LayoutInflater.from(context);
        this.locale = LocaleUtil.getLocale(context);
    }

    @Override
    public HeaderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = inflater.inflate(R.layout.index_header_row, parent, false);
        } else {
            view = inflater.inflate(R.layout.index_sura_row, parent, false);
        }
        return viewType == 0 ? new HeaderHolder(view) : new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HeaderHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            bindHeader(holder, position);
        } else {
            bindRow(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return elements.length;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return elements[position].isHeader() ? 0 : 1;
    }

    @Override
    public void onClick(View v) {
        int position = recyclerView.getChildAdapterPosition(v);
        if (position != RecyclerView.NO_POSITION) {
            QuranRow element = elements[position];
            if (touchListener == null) {
                ((QuranActivity) context).jumpTo(element.getPage());
            } else {
                touchListener.onClick(element, position);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (touchListener != null) {
            int position = recyclerView.getChildAdapterPosition(v);
            if (position != RecyclerView.NO_POSITION) {
                return touchListener.onLongClick(elements[position], position);
            }
        }
        return false;
    }

    public void setElements(QuranRow[] elements) {
        this.elements = elements;
        notifyDataSetChanged();
    }

    public boolean isItemChecked(int position) {
        return checkedState.get(position);
    }

    public void setItemChecked(int position, boolean checked) {
        checkedState.put(position, checked);
        notifyItemChanged(position);
    }

    public void uncheckAll() {
        checkedState.clear();
        notifyDataSetChanged();
    }

    public ArrayList<QuranRow> getCheckedItems() {
        ArrayList<QuranRow> result = new ArrayList<>();
        int count = checkedState.size();
        int elementsCount = getItemCount();
        for (int i = 0; i < count; i++) {
            int key = checkedState.keyAt(i);
            if (checkedState.get(key) && key < elementsCount) {
                result.add(getQuranRow(key));
            }
        }
        return result;
    }

    public void setQuranTouchListener(QuranTouchListener listener) {
        touchListener = listener;
    }

    public void setElements(QuranRow[] elements, Map<Long, Tag> tagMap) {
        this.elements = elements;
        this.tagMap = tagMap;
    }

    public void setShowTags(boolean showTags) {
        this.showTags = showTags;
    }

    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
    }

    private QuranRow getQuranRow(int position) {
        return elements[position];
    }

    private void bindRow(HeaderHolder vh, int position) {
        ViewHolder holder = (ViewHolder) vh;
        bindHeader(vh, position);
        QuranRow item = elements[position];

        holder.number.setText(QuranUtils.getLocalizedNumber(context, item.getSura()));
        holder.metadata.setVisibility(View.VISIBLE);
        holder.metadata.setText(item.getMetadata());
        holder.tags.setVisibility(View.GONE);

        if (item.getJuzType() != null) {
            holder.image.setImageDrawable(new JuzView(context, item.getJuzType(), item.getJuzOverlayText()));
            holder.image.setVisibility(View.VISIBLE);
            holder.number.setVisibility(View.GONE);
        } else if (item.getImageResource() == null) {
            holder.number.setVisibility(View.VISIBLE);
            holder.image.setVisibility(View.GONE);
        } else {
            holder.image.setImageResource(item.getImageResource());

            if (item.getImageFilterColor() == null) {
                holder.image.setColorFilter(null);
            } else {
                holder.image.setColorFilter(item.getImageFilterColor(), PorterDuff.Mode.SRC_ATOP);
            }

            if (showDate) {
                String date = new SimpleDateFormat("MMM dd, HH:mm", locale).format(new Date(item.getDateAddedInMillis()));
                holder.metadata.setText(item.getMetadata() + " - " + date);
            }

            holder.image.setVisibility(View.VISIBLE);
            holder.number.setVisibility(View.GONE);

            ArrayList<Tag> tagList = new ArrayList<>();
            Bookmark bookmark = item.getBookmark();
            if (bookmark != null && !bookmark.getTags().isEmpty() && showTags) {
                for (Long tagId : bookmark.getTags()) {
                    Tag tag = tagMap.get(tagId);
                    if (tag != null) {
                        tagList.add(tag);
                    }
                }
            }

            if (tagList.isEmpty()) {
                holder.tags.setVisibility(View.GONE);
            } else {
                holder.tags.setTags(tagList);
                holder.tags.setVisibility(View.VISIBLE);
            }
        }
    }

    private void bindHeader(HeaderHolder vh, int pos) {
        QuranRow item = elements[pos];
        vh.title.setText(item.getText());
        if (item.getPage() == 0) {
            vh.pageNumber.setVisibility(View.GONE);
        } else {
            vh.pageNumber.setVisibility(View.VISIBLE);
            vh.pageNumber.setText(QuranUtils.getLocalizedNumber(context, item.getPage()));
        }
        vh.setChecked(isItemChecked(pos));
        vh.setEnabled(isEnabled(pos));
    }

    private boolean isEnabled(int position) {
        QuranRow selected = elements[position];
        return !isEditable ||                    // anything in surahs or juzs
                selected.isBookmark() ||          // actual bookmarks
                selected.getRowType() == QuranRow.RowType.NONE ||  // the actual "current page"
                selected.isBookmarkHeader();      // tags
    }

    public class HeaderHolder extends RecyclerView.ViewHolder {
        View view;
        TextView title;
        TextView pageNumber;

        public HeaderHolder(View itemView) {
            super(itemView);
            view = itemView;
            title = itemView.findViewById(R.id.title);
            pageNumber = itemView.findViewById(R.id.pageNumber);
            setEnabled(true);
        }

        public void setEnabled(boolean enabled) {
            view.setEnabled(true);
            itemView.setOnClickListener(enabled ? QuranListAdapter.this : null);
            itemView.setOnLongClickListener(isEditable && enabled ? QuranListAdapter.this : null);
        }

        public void setChecked(boolean checked) {
            view.setActivated(checked);
        }
    }

    private class ViewHolder extends HeaderHolder {
        TextView metadata;
        TextView number;
        ImageView image;
        TagsViewGroup tags;
        TextView date;

        public ViewHolder(View itemView) {
            super(itemView);
            metadata = itemView.findViewById(R.id.metadata);
            number = itemView.findViewById(R.id.suraNumber);
            image = itemView.findViewById(R.id.rowIcon);
            tags = itemView.findViewById(R.id.tags);
            date = itemView.findViewById(R.id.show_date);
        }
    }

    public interface QuranTouchListener {
        void onClick(QuranRow row, int position);

        boolean onLongClick(QuranRow row, int position);
    }
}
