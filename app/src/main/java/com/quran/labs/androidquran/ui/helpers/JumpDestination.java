/**
 * Activity or fragment that implements this interface is meant to be a jump destination/target.
 */
public interface JumpDestination {
    void jumpTo(int page);
    void jumpToAndHighlight(int page, int sura, int ayah);
}
