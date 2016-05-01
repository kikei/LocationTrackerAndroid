package net.xaxxi.locationtracker;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.location.Location;
import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationTrackerClient {

    private static LocationTrackerClient instance;
    public static synchronized LocationTrackerClient getInstance(Model model) {
        if (instance == null)
            instance = new LocationTrackerClient(model);
        return instance;
    }
    
    private static final String TAG = "LocationTracker.LocationTrackerClient";

    private static final int SALT_LENGTH = 32;
    private static final int MAX_LOCATIONS_SEND = 1500;
    private static final long INTERVAL_SEND_LOCATIONS = 1000L; // [millisecond]

    private static final MediaType MEDIA_TYPE_JSON =
        MediaType.parse("application/json");
    private static final String URL_BASE = "http://192.168.0.11:9000";
    private static final String PATH_LOGIN = "/login/mobile";
    private static final String PATH_LOGOUT = "/logout/mobile";
    private static final String PATH_LOCATIONS = "/locations";
    private static final String PATH_LATEST = "/locations/latest";

    Model mModel;
    AtomicBoolean mSynchronizing;

    final OkHttpClient mClient = new OkHttpClient();
    
    private LocationTrackerClient(Model model) {
        mModel = model;
        mSynchronizing = new AtomicBoolean(false);
    }

    public void synchronize() {
        final String userName = mModel.getUserName();
        final String sessionId = mModel.getSessionId();
        
        if (userName == null) {
            android.util.Log.e(TAG, "username is null");
            throw new IllegalStateException("username is null");
        }

        // Ensure running task is only one in Activity.
        SyncronizedTask task = new SyncronizedTask(mSynchronizing) {
                @Override
                public boolean run(AtomicBoolean lock) {
                    if (sessionId == null) 
                        return doCreateSessionAndSynchronize(userName, lock);
                    else
                        return doSynchronize(sessionId, lock);
                }
            };
        task.start();
    }
    
    private boolean doCreateSessionAndSynchronize(String userName,
                                                  final AtomicBoolean lock) {
        return asyncPostJson
            (PATH_LOGIN, new RequestLogin(userName, makeSalt()),
             new JsonCallback<ResponseLogin>(ResponseLogin.class, lock) {
                @Override
                public boolean onOK(Response response, ResponseLogin data) {
                    String sessionId = data.data.sessionId;
                    android.util.Log.d(TAG, "login ok, sessionId=" + sessionId);
                    mModel.putSessionId(sessionId);
                    return doSynchronize(sessionId, lock);
                }
            });
    }

    private boolean doSynchronize(String sessionId, AtomicBoolean lock) {
        android.util.Log.d(TAG, "doSynchroze sessionId=" + sessionId);
        return doGetLatest(sessionId, lock);
    }

    private boolean doGetLatest(final String sessionId,
                                final AtomicBoolean lock) {
        return asyncPostJson
            (PATH_LATEST, new RequestLocationLatest(sessionId),
             new JsonCallback<ResponseLocationLatest>
             (ResponseLocationLatest.class, lock) {
                @Override
                public boolean onNG(Response response, ResponseError data) {
                    switch (response.code()) {
                    case 403:
                        String userName = mModel.getUserName();
                        // Retry from creating session;
                        // beware not to make inifinite loop!!
                        if (userName != null)
                            return doCreateSessionAndSynchronize(userName, lock);
                    }
                    return true;
                }
                @Override
                public boolean onOK(Response response,
                                    ResponseLocationLatest r) {
                    long lastTime = r.data.time;
                    android.util.Log.d(TAG, "latest ok, time=" + lastTime);
                    return doSendLocations(sessionId, lastTime, lock);
                }
            });
    }

    private boolean doSendLocations(final String sessionId, long lastTime,
                                    final AtomicBoolean lock) {
        
        final TreeMap<Long, Location> locations =
            mModel.getLocationsLater(lastTime);
        
        final TimeLocationData[] tl =
            timeLocationDataFromMap(locations, MAX_LOCATIONS_SEND);

        android.util.Log.d(TAG,
                           "doSendLocations, lastTime=" + lastTime +
                           ", length=" + tl.length);

        RequestLocationAppend r = new RequestLocationAppend(sessionId, tl);
        
        return asyncPutJson(PATH_LOCATIONS, r,
                            new JsonCallback<ResponseLocationAppend>
                            (ResponseLocationAppend.class, lock) {
                @Override
                public boolean onOK(Response response,
                                    ResponseLocationAppend data) {
                    android.util.Log.d(TAG, "send location ok");
                    if (tl.length < locations.size()) {
                        try {
                            Thread.sleep(INTERVAL_SEND_LOCATIONS);
                        } catch (InterruptedException e) {
                            android.util.Log.w(TAG, "sleep interrupted");
                            return true;
                        }
                        return
                            doSendLocations(sessionId, tl[tl.length-1].time + 1,
                                            lock);
                    }
                    return true;
                }
            });
    }

    private <T> boolean asyncPostJson(String path, T data, Callback callback) {
        Gson gson = new Gson();
        Request request =
            new Request.Builder()
            .url(URL_BASE + path)
            .post(RequestBody.create(MEDIA_TYPE_JSON, gson.toJson(data)))
            .build();
        mClient.newCall(request).enqueue(callback);
        return false;
    }

    private <T> boolean asyncPutJson(String path, T data, Callback callback) {
        Gson gson = new Gson();
        Request request =
            new Request.Builder()
            .url(URL_BASE + path)
            .put(RequestBody.create(MEDIA_TYPE_JSON, gson.toJson(data)))
            .build();
        mClient.newCall(request).enqueue(callback);
        return false;
    }

    /**
     * Gimic for synchronized chain over asynchronous request.
     */
    private abstract class SyncronizedTask {
        final AtomicBoolean mLock;
        public SyncronizedTask(AtomicBoolean lock) { mLock = lock; }
        public synchronized void start() {
            if (!mLock.get()) {
                mLock.set(!run(mLock));
            }
        }
        public abstract boolean run(AtomicBoolean lock);
    }
    
    private abstract class JsonCallback<Success> implements Callback {
        abstract public boolean onOK(Response response, Success data);
        public boolean onNG(Response response, ResponseError data) {
            return true;
        }

        Class<Success> mSuccessClass;
        AtomicBoolean mLock;
        public JsonCallback(Class<Success> cl, AtomicBoolean lock) {
            mSuccessClass = cl;
            mLock = lock;
        }
        
        @Override
        public final void onFailure(Call call, IOException e) {
            android.util.Log.e(TAG, "erorr: " + e);
            e.printStackTrace();
            mLock.set(false);
        }
        
        @Override
        public final void onResponse(Call call, Response response) {
            Gson gson = new Gson();
            String body;
            boolean ended = true;
            try {
                try {
                    body = response.body().string();
                } catch (IOException e) {
                    android.util.Log.e(TAG, "failed to read body; skipped");
                    return;
                }
                android.util.Log.d(TAG,
                                   "onResponse" +
                                   " code=" + response.code() +
                                   ", body=" + body);
                if (!response.isSuccessful()) {
                    ResponseError r;
                    try {
                        r = gson.fromJson(body, ResponseError.class);
                    } catch (JsonSyntaxException e) {
                        android.util.Log.e(TAG, "response is bad json");
                        return;
                    }
                    android.util.Log.i(TAG,
                                       "failed to get latest" +
                                       ", status=" + r.status +
                                       ", message=" + r.message);
                    ended = onNG(response, r);
                } else {
                    Success r = gson.fromJson(body, mSuccessClass);
                    ended = onOK(response, r);
                }
            } finally {
                mLock.set(!ended);
            }
        }
    }

    private static String makeSalt() {
        StringBuilder builder = new StringBuilder();
        
        byte bytes[] = new byte[SALT_LENGTH];
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(bytes);
        } catch (NoSuchAlgorithmException e) {
            android.util.Log.e(TAG, "no such algorithm exception");
        }

        for (int i = 0; i < bytes.length; i++) 
            builder.append(String.format("%02x", bytes[i]));
        
        return builder.toString();
    }

    class ResponseError {
        public String status;
        public String message;
    }

    class TimeLocationData {
        @SerializedName("t")
        public long time;
        
        @SerializedName("loc")
        public LocationData location;

        public TimeLocationData(long time, LocationData location) {
            this.time = time;
            this.location = location;
        }
    }

    public TimeLocationData[]
        timeLocationDataFromMap(TreeMap<Long, Location> locations, int max) {

        int size = Math.min(max, locations.size());
        
        TimeLocationData[] a = new TimeLocationData[size];
        int i = 0;
        for (Entry<Long, Location> e : locations.entrySet()) {
            if (i < max) {
                LocationData location = new LocationData(e.getValue());
                TimeLocationData d =
                    new TimeLocationData(e.getKey(), location);
                a[i++] = d;
            } else break;
        }
        return a;
    }

    class LocationData {
        @SerializedName("lat")
        public double latitude;
        
        @SerializedName("lng")
        public double longitude;

        public LocationData(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public LocationData(Location location) {
            this(location.getLatitude(),
                 location.getLongitude());
        }
    }
    
    class RequestLogin {
        private String user;
        private String salt;
        private String check;

        public RequestLogin(String user, String salt) {
            this.user = user;
            this.salt = salt;
            this.check = makeCheck(user, salt);
        }
    }

    private static String makeCheck(String user, String salt) {
        return user + salt;
    }

    class ResponseLogin {
        class ResponseLoginData {
            public String sessionId;
        }
        public String status;
        public ResponseLoginData data;
    }

    class RequestLogout {
        private String sessionId;
    }

    class ResponseLogout {
        public String status;
    }

    class RequestLocationLatest {
        private String sessionId;
        public RequestLocationLatest(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    class ResponseLocationLatest {
        public String status;
        public TimeLocationData data;
    }
    
    class RequestLocationAppend {
        private String sessionId;
        private TimeLocationData[] locations;
        public RequestLocationAppend(String sessionId,
                                     TimeLocationData[] locations) {
            this.sessionId = sessionId;
            this.locations = locations;
        }
    }

    class ResponseLocationAppend {
        private String status;
    }
    
}
