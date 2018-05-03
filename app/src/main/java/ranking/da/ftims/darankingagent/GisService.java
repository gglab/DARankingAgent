package ranking.da.ftims.darankingagent;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface GisService {

    @Headers({"Accept: application/json",
            "Content-Type: application/json"})
    @GET("/api/gisSpeedingLimit/{latitude}/{longitude}")
    Call<ResponseGisSpeedLimit> getGisSpeedingLimit(@Header("Authorization") String authorization, @Path("latitude") double latitude, @Path("longitude") double longitude);
}
