package pl.edu.agh.marims.plugin.network;

import pl.edu.agh.marims.plugin.Config;
import pl.edu.agh.marims.plugin.util.GsonUtil;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class MarimsApiClient {

    private static MarimsApiClient instance = null;
    private MarimsService marimsService;

    private MarimsApiClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Config.SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getGson()))
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
}
