package me.maplef.mapbotv4.utils;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;

public class HttpUtils {
    public static String doGet(String URL){
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
             HttpGet httpGet = new HttpGet(URL);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
                return content;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}