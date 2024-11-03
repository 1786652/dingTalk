package com.example.dingtalk.config;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Data
@Component
public class RequestConfig {
    private HttpRequest.Builder builder;
    private HttpClient httpClient = HttpClient.newBuilder().build();
    public RequestConfig(){
        builder = HttpRequest.newBuilder();
    }
    public String doPost(String uri , String body) throws IOException, InterruptedException {
        builder.POST(HttpRequest.BodyPublishers.ofString(body));
        builder.uri(URI.create(uri));
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String res = response.body();
        return res;
    }

    public String doGet(String uri) throws IOException, InterruptedException, URISyntaxException {
        builder.uri(URI.create(uri));
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String res = response.body();
        return res;
    }

}
