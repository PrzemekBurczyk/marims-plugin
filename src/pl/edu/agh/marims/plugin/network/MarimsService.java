package pl.edu.agh.marims.plugin.network;

import okhttp3.RequestBody;
import retrofit.http.*;

import java.util.List;

public interface MarimsService {

    @GET("/files")
    List<String> getFiles();

    @Multipart
    @POST("/files")
    void postFile(@Part("file") RequestBody file);

    @DELETE("/files/{filename}")
    void deleteFile(@Path("filename") String filename);
}
