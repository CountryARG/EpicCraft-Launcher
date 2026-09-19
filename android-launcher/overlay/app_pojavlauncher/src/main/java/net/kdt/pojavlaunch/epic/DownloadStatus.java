package net.kdt.pojavlaunch.epic;

/** Immutable snapshots shared by downloader threads and the Android UI. */
public final class DownloadStatus {
    public static final class Snapshot {
        public final String path;
        public final long done, total;
        public final boolean downloading;
        Snapshot(String path, long done, long total, boolean downloading) {
            this.path = path;
            this.done = Math.max(0, done);
            this.total = total;
            this.downloading = downloading;
        }
        public int percent() {
            if (total <= 0) return -1;
            return (int) Math.min(100, (double) done / total * 100);
        }
    }
    public static volatile Snapshot current = new Snapshot("", 0, -1, false);
    private static String path = "Preparando archivos";
    private DownloadStatus() {}
    public static synchronized void reset() {
        path = "Preparando archivos";
        current = new Snapshot(path, 0, -1, false);
    }
    public static synchronized void file(String value) { path = value; }
    public static synchronized void update(long done, long total) {
        current = new Snapshot(path, done, total, true);
    }
    public static synchronized void finish() {
        Snapshot s = current;
        current = new Snapshot(s.path, s.done, s.total, false);
    }
}
