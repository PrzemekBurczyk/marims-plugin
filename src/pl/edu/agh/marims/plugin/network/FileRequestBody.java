package pl.edu.agh.marims.plugin.network;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import okio.BufferedSink;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileRequestBody extends RequestBody {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private ProgressListener progressListener;
    private File file;

    public FileRequestBody(final File file, final ProgressListener progressListener) {
        this.file = file;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("application/octet-stream");
    }

    @Override
    public void writeTo(BufferedSink bufferedSink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long total = 0;
        try (FileInputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                progressListener.onProgress(total, fileLength);
                total += read;
                bufferedSink.write(buffer, 0, read);
            }
            progressListener.onProgress(fileLength, fileLength);
        }
    }

    public interface ProgressListener {
        void onProgress(final long current, final long max);
    }
}
