import net.kdt.pojavlaunch.epic.DownloadStatus;
public class DownloadStatusTest {
    public static void main(String[] args) {
        DownloadStatus.reset();
        require(DownloadStatus.current.percent() == -1, "unknown size");
        DownloadStatus.file("assets/objects/ab/file");
        DownloadStatus.update(125, 300);
        require(DownloadStatus.current.percent() == 41, "actual byte ratio");
        require(DownloadStatus.current.path.equals("assets/objects/ab/file"), "file path");
        DownloadStatus.update(200, -1);
        require(DownloadStatus.current.percent() == -1, "unknown length must stay indeterminate");
        DownloadStatus.update(500, 300);
        require(DownloadStatus.current.percent() == 100, "clamp");
        DownloadStatus.finish();
        require(!DownloadStatus.current.downloading, "no stale download during extraction");
        DownloadStatus.reset();
        require(DownloadStatus.current.done == 0, "no stale bytes for next version");
        System.out.println("7 comprobaciones de progreso: OK");
    }
    static void require(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
}
