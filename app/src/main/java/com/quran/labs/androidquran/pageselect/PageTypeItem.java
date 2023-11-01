import androidx.annotation.StringRes;
import java.io.File;

public class PageTypeItem {
    private final String pageType;
    private final File previewImage;
    @StringRes
    private final int title;
    @StringRes
    private final int description;
    private final boolean isSelected;

    public PageTypeItem(String pageType, File previewImage, int title, int description, boolean isSelected) {
        this.pageType = pageType;
        this.previewImage = previewImage;
        this.title = title;
        this.description = description;
        this.isSelected = isSelected;
    }

    public String getPageType() {
        return pageType;
    }

    public File getPreviewImage() {
        return previewImage;
    }

    public int getTitle() {
        return title;
    }

    public int getDescription() {
        return description;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
