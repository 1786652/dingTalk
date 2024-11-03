package com.example.dingtalk.config;

import com.aliyun.dingtalkesign_2_0.models.GetUserInfoHeaders;
import com.aliyun.dingtalkoauth2_1_0.Client;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkoauth2_1_0.models.GetAuthInfoRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetSsoUserInfoRequest;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DingTalkeConfig {

    private String accessToken;
    private Client client;
    {
        try {
            client = new Client(new Config().setProtocol("https").setRegionId("central"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public Client dingClient() throws Exception{
        return client;
    }

    @Bean
    public String accessToken() throws Exception {
        GetAccessTokenResponse accessTokenResponse = client.getAccessToken(new GetAccessTokenRequest()
                .setAppKey("dingb1cmlolkaqutjzk1")
                .setAppSecret("hmQuT_VRkQwsw7fyMfrF4gAVIArkL8NhjGoXCc42wdfqN8TmyIrqK8mt5nyQxYFn"));
        accessToken = accessTokenResponse.getBody().getAccessToken();
        return accessToken;
    }
    @Bean
    public com.aliyun.dingtalkworkflow_1_0.Client workClient() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkworkflow_1_0.Client(config);
    }

}
