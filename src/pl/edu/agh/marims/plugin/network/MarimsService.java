package pl.edu.agh.marims.plugin.network;

import com.squareup.okhttp.RequestBody;
import pl.edu.agh.marims.plugin.network.models.LoggedUser;
import pl.edu.agh.marims.plugin.network.models.Session;
import pl.edu.agh.marims.plugin.network.models.UserRequest;
import retrofit.Call;
import retrofit.http.*;

import java.util.List;

public interface MarimsService {

    @POST("/register")
    Call<LoggedUser> register(@Body UserRequest user);

    @POST("/login")
    Call<LoggedUser> logIn(@Body UserRequest user);

    @POST("/logout")
    Call<Void> logOut();

    @GET("/files")
    Call<List<String>> getFiles();

    @Multipart
    @POST("/files")
    Call<Void> postFile(@Part("applicationPackage") RequestBody applicationPackage, @Part("applicationName") RequestBody applicationName, @Part("applicationVersion") RequestBody applicationVersion, @Part("applicationVersionCode") RequestBody applicationVersionCode, @Part("file\"; filename=\"app.apk\"") RequestBody file);

    @DELETE("/files/{filename}")
    Call<Void> deleteFile(@Path("filename") String filename);

    @GET("/sessions")
    Call<List<Session>> getSessions();

}
