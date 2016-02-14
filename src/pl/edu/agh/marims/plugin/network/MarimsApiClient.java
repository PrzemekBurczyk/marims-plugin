package pl.edu.agh.marims.plugin.network;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import pl.edu.agh.marims.plugin.Config;
import pl.edu.agh.marims.plugin.network.models.LoggedUser;
import pl.edu.agh.marims.plugin.util.GsonUtil;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class MarimsApiClient {

    private static MarimsApiClient instance = null;
    private MarimsService marimsService;
    private LoggedUser loggedUser;

    private MarimsApiClient() {
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(chain -> {
            Request request = chain.request();
            if (loggedUser != null && loggedUser.getToken() != null) {
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + loggedUser.getToken())
                        .build();
            }
            return chain.proceed(request);
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Config.SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getGson()))
                .client(client)
                .build();

        marimsService = retrofit.create(MarimsService.class);
    }

    public static synchronized MarimsApiClient getInstance() {
        if (instance == null) {
            instance = new MarimsApiClient();
        }
        return instance;
    }

    public MarimsService getMarimsService() {
        return marimsService;
    }

    public LoggedUser getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(LoggedUser loggedUser) {
        this.loggedUser = loggedUser;
    }
}
