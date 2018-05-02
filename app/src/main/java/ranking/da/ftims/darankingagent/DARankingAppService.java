package ranking.da.ftims.darankingagent;

import retrofit2.http.Body;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DARankingAppService {

    @Headers({"Accept: application/json",
            "Content-Type: application/json"})
    @POST("/api/authenticate")
    Call<ResponseAuthentication> authenticate(@Body DARankingAppCredentials credentials);

    @Headers({"Accept: application/json",
            "Content-Type: application/json"})
    @GET("/api/drivers/{user}")
    Call<DARankingAppDriver> getDriver(@Header("Authorization") String authorization, @Path("user") String user);

    @Headers({"Accept: application/json",
            "Content-Type: application/json"})
    @POST("/api/tripAgent")
    Call<TripSyncResponse> createTripFromAgent(@Header("Authorization") String authorization, @Body TripVM tripVM);

}
