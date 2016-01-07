package pl.edu.agh.marims.plugin.network;

import com.squareup.okhttp.RequestBody;
import retrofit.Call;
import retrofit.http.*;

import java.util.List;

public interface MarimsService {

    @GET("/files")
    Call<List<String>> getFiles();

    @Multipart
    @POST("/files")
    Call<Void> postFile(@Part("applicationName") RequestBody applicationName, @Part("applicationVersion") RequestBody applicationVersion, @Part("file\"; filename=\"app.apk\"") RequestBody file);

    @DELETE("/files/{filename}")
    Call<Void> deleteFile(@Path("filename") String filename);
}