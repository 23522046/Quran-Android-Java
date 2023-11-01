package com.quran.labs.androidquran.presenter.bookmark;

import com.quran.data.model.bookmark.Tag;
import com.quran.labs.androidquran.model.bookmark.BookmarkModel;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.ui.fragment.AddTagDialog;

import java.util.List;

import javax.inject.Inject;

public class AddTagDialogPresenter implements Presenter<AddTagDialog> {
    private AddTagDialog dialog;
    private List<Tag> tags;
    private final BookmarkModel bookmarkModel;

    @Inject
    public AddTagDialogPresenter(BookmarkModel bookmarkModel) {
        this.bookmarkModel = bookmarkModel;
        bookmarkModel.getTagsObservable()
                .subscribe(tags -> this.tags = tags);
    }

    public boolean validate(String tagName, long tagId) {
        if (tagName.trim().isEmpty()) {
            dialog.onBlankTagName();
            return false;
        } else {
            if (tags.stream().anyMatch(tag -> tag.getName().equals(tagName) && tag.getId() != tagId)) {
                dialog.onDuplicateTagName();
                return false;
            }
        }
        return true;
    }

    public void addTag(String tagName) {
        bookmarkModel.addTagObservable(tagName)
                .subscribe();
    }

    public void updateTag(Tag tag) {
        bookmarkModel.updateTag(tag)
                .subscribe();
    }

    @Override
    public void bind(AddTagDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void unbind(AddTagDialog dialog) {
        if (this.dialog == dialog) {
            this.dialog = null;
        }
    }
}
