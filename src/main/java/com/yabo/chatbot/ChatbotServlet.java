package com.yabo.chatbot;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class ChatbotServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatbotServlet.class.getName());

    private String access_token;
    private final String client_id = "ac936cf8-4063-4e5d-aab4-7dec0fcb535f";
    private final String client_secret = "RbiGsxgzszhx9o1itvYHB8V";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    public void destroy() {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        LOGGER.info("Received a POST request:\n" + request.toString());

        StringBuffer requestBody = new StringBuffer();
        String line;

        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warning(e.toString());
            response.getWriter().print(e.toString());
            return;
        }

        try {

            JSONObject requestJson = new JSONObject(requestBody.toString());
            String replyBody = "Created";
            response.getWriter().print(replyBody);

            getAccessToken();
            processRequest(requestJson);

        } catch (JSONException e) {
            e.printStackTrace();
            LOGGER.warning(e.toString());
            response.getWriter().print(e.toString());
        }

    }

    private void getAccessToken() throws IOException, JSONException {

        CloseableHttpClient httpClient = HttpClients.createDefault();

        String grant_type = "client_credentials";
        String scope = "https://api.botframework.com/.default";
        String content = "grant_type=" + grant_type +
                "&client_id=" + client_id +
                "&client_secret=" + client_secret +
                "&scope=" + URLEncoder.encode(scope, "UTF-8");

        String accessTokenUrl = "https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token";
        HttpPost request = new HttpPost(accessTokenUrl);
        HttpEntity entity = new StringEntity(content, ContentType.APPLICATION_FORM_URLENCODED);
        request.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity responseEntity = response.getEntity();

        if (responseEntity != null) {
            String entityString = EntityUtils.toString(responseEntity);
            JSONObject jsonObject = new JSONObject(entityString);
            access_token = jsonObject.getString("access_token");
        }

    }

    private void processRequest(JSONObject requestJson) throws JSONException, IOException {

        String conversationId = requestJson.getJSONObject("conversation").getString("id");
        String serviceUrl = requestJson.getString("serviceUrl");

        if (requestJson.getString("type").equals("message")) {
            String text = requestJson.getString("text");
            String echo_msg = "Hey! Did you say: " + text + "?";
            reply(serviceUrl, conversationId, echo_msg);
        } else {
            String echo_msg = "Sorry I can only respond to text messages at this moment.";
            reply(serviceUrl, conversationId, echo_msg);
        }

    }

    private void reply(String serviceUrl, String conversationId, String echo_msg) throws IOException, JSONException {

        JSONObject replyMessage = buildReplyMessage(echo_msg);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        String postbackUrl = serviceUrl + "/v3/conversations/" + conversationId + "/activities";
        HttpPost request = new HttpPost(postbackUrl);
        request.addHeader("Authorization", "Bearer " + access_token);

        HttpEntity entity = new StringEntity(replyMessage.toString(), ContentType.APPLICATION_JSON);
        request.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(request);
        LOGGER.info(response.toString());

    }

    private JSONObject buildReplyMessage(String echo_msg) throws JSONException {

        JSONObject replyMessage = new JSONObject();
        replyMessage.put("type", "message");
        replyMessage.put("text", echo_msg);

        return replyMessage;
    }

}
