package ranking.da.ftims.darankingagent.rest;

public class TokenCredentials {
    private static String tokenId;

    public static String getTokenId() {
        return tokenId;
    }

    public static void setTokenId(String tokenId) {
        TokenCredentials.tokenId = tokenId;
    }
}
