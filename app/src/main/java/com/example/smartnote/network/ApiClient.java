package com.example.smartnote.network;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://192.168.55.152:8080/";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private static final CookieManager cookieManager = new CookieManager();
    static {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    // Custom Gson instance with Date adapter
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateAdapter())
            .create();

    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(new JavaNetCookieJar(cookieManager))
            .addInterceptor(new HttpLoggingInterceptor().setLevel(
                    HttpLoggingInterceptor.Level.BODY))
            .addInterceptor(new AuthInterceptor())
            .build();

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    private static final ApiService apiService = retrofit.create(ApiService.class);

    public static ApiService getApiService() {
        return apiService;
    }

    private static class AuthInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();

            // Add Android identification headers
            builder.header("User-Agent", "smartnote-android")
                    .header("X-Requested-With", "XMLHttpRequest");

            // Add CSRF token for state-changing operations
            if (requiresCsrfToken(original)) {
                String csrfToken = getCsrfTokenFromCookies();
                if (csrfToken != null) {
                    builder.header(CSRF_HEADER_NAME, csrfToken);
                } else {
                    Log.w(TAG, "CSRF token missing for " + original.method() + " request");
                }
            }

            return chain.proceed(builder.build());
        }

        private boolean requiresCsrfToken(Request request) {
            String method = request.method();
            return !method.equalsIgnoreCase("GET") &&
                    !request.url().encodedPath().startsWith("/api/public/");
        }
    }

    private static class DateAdapter extends TypeAdapter<Date> {
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

        @Override
        public void write(JsonWriter out, Date value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(dateFormat.format(value));
        }

        @Override
        public Date read(JsonReader in) throws IOException {
            try {
                if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                String dateString = in.nextString();
                return dateFormat.parse(dateString);
            } catch (ParseException e) {
                throw new IOException("Failed to parse date", e);
            }
        }
    }

    public static void initializeCsrfToken() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Fetch CSRF token via GET request
                Response response = okHttpClient.newCall(
                        new Request.Builder()
                                .url(BASE_URL)
                                .build()
                ).execute();
                response.close();
                Log.d(TAG, "CSRF token initialized");
            } catch (IOException e) {
                Log.e(TAG, "CSRF initialization error: " + e.getMessage());
            }
        });
    }

    private static String getCsrfTokenFromCookies() {
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        for (HttpCookie cookie : cookies) {
            if (CSRF_COOKIE_NAME.equalsIgnoreCase(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void clearSession() {
        cookieManager.getCookieStore().removeAll();
    }

    public static boolean hasCsrfToken() {
        return getCsrfTokenFromCookies() != null;
    }
}