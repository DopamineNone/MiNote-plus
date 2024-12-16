package net.micode.notes.tool;

import static android.provider.Settings.System.getString;

import android.app.ProgressDialog;
import android.content.Context;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;

import net.micode.notes.R;

import org.json.JSONObject;

public class TextPrettifierTask extends AsyncTask<Void, Void, Void> {


    private static String API_KEY;
    private static String ENDPOINT;

    private Context context;

    private ProgressDialog dialog;

    private static final String METHOD = "POST";

    private static final String TAG = "TextPrettifierTask";

    private static final String SYSTEM_ROLE = "system";

    private static final String USER_ROLE = "user";

    private static String DASHSCOPE_PROMPT;

    private String errorMessage;

    private String inputText; // input text to be prettified

    private boolean result_flag = true; // true if success, false if error

    private String result_text = ""; // prettified text or error message

    public interface OnAsyncTaskResultListener {
        void onResultReceived(String result);
    }

    private OnAsyncTaskResultListener listener;

    private static class Message {
        String role;
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    private static class RequestBody {
        String model;
        Message[] messages;

        public RequestBody(Message[] messages) {
            this.messages = messages;
            this.model = "qwen-1.8b-chat";
        }
    }

    public TextPrettifierTask(Context context, String apiKey, String endpoint, String promptMessage, String inputText, OnAsyncTaskResultListener listener) {
        this.context = context;
        this.API_KEY = apiKey;
        this.ENDPOINT = endpoint;
        this.DASHSCOPE_PROMPT = promptMessage;
        this.inputText = inputText;
        this.errorMessage = context.getResources().getString(R.string.error_note_prettify_fail);
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if ( this.context != null) {
            ProgressDialog progressDialog = new ProgressDialog(this.context);
            progressDialog.setMessage(this.context.getResources().getString(R.string.alert_loading));
            progressDialog.setCancelable(false);
            this.dialog = progressDialog;
            this.dialog.show();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Check if API_KEY and ENDPOINT are not null
        if (API_KEY == null || ENDPOINT == null || DASHSCOPE_PROMPT == null) {
            this.result_flag = false;
            return null;
        }

        doPrettifyText();

        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
        if (!this.result_flag) {
            Toast.makeText(this.dialog.getContext(), this.errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            listener.onResultReceived(result_text);
            Log.d(TAG, "Prettified text: " + result_text);
        }
    }

    private static String newRequestJson(String text) {
        return new Gson().toJson(
                new RequestBody(new Message[] { new Message(SYSTEM_ROLE, DASHSCOPE_PROMPT), new Message(USER_ROLE, text) })
        );
    }

    private String makePrompt(String text) {
        return DASHSCOPE_PROMPT + "\n---\n" + text;
    }

    public void doPrettifyText() {
        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            // 设置请求体
            httpURLConnection.setRequestMethod(METHOD);
            httpURLConnection.setRequestProperty("Content-Type", "application/json; utf-8");
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            httpURLConnection.setDoOutput(true);

            // 发送请求体
            try (OutputStream os = httpURLConnection.getOutputStream()) {
                byte[] input = newRequestJson(makePrompt(this.inputText)).getBytes();
                Log.d(TAG, "Request body: " + input.toString());
                os.write(input, 0, input.length);
            }

            // 处理响应
            int responseCode = httpURLConnection.getResponseCode();
            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    this.result_flag = false;
                    Log.e(TAG, "Unauthorized access");
                    return;
                default:
                    this.result_flag = false;
                    Log.e(TAG, "Error response code: " + responseCode);
                    return;
            }

            // 发送请求并获取响应
            try (BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
                result_text = response.toString();
                Log.d(TAG, "Response body: " + response.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error reading response body",e);
                result_flag = false;
                return;
            }
            // 解析响应
            result_text = new JSONObject(result_text).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            result_text = e.toString();
            result_flag = false;
        }
    }
}
