package org.haxe.extension;

import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.haxe.extension.Extension;

/* 
	You can use the Android Extension class in order to hook
	into the Android activity lifecycle. This is not required
	for standard Java code, this is designed for when you need
	deeper integration.

	You can access additional references from the Extension class,
	depending on your needs:

	- Extension.assetManager (android.content.res.AssetManager)
	- Extension.callbackHandler (android.os.Handler)
	- Extension.mainActivity (android.app.Activity)
	- Extension.mainContext (android.content.Context)
	- Extension.mainView (android.view.View)

	You can also make references to static or instance methods
	and properties on Java classes. These classes can be included 
	as single files using < java path="to/File.java" /> within your
	project, or use the full Android Library Project format (such
	as this example) in order to include your own AndroidManifest
	data, additional dependencies, etc.

	These are also optional, though this example shows a static
	function for performing a single task, like returning a value
	back to Haxe from Java.
*/
public class KizzyClient extends Extension {

	public static final String LOG_TAG = "KizzyClient";

	private String token;
	private String applicationId;
	private String activityName;
	private String details;
	private String state;
	private String largeImage;
	private String smallImage;
	private String status;

	private int type;
	private int seq;

	private Long startTimeStamps;
	private Long stopTimeStamps;

	private ArrayMap<String, Object> rpc = new ArrayMap<String, Object>();

	private WebSocketClient webSocketClient;
	private Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

	private int heartbeatInterval;
	private Runnable heartbeatRunnable;
	private Thread heartbeatThread;

	private String sessionId;
	private boolean reconnectSession = false;

	public KizzyClient(String token) {

		this.token = token;

		heartbeatRunnable = new Runnable() {
			public void run() {
				try {
					if (heartbeatInterval < 10000) {
						throw new RuntimeException("Invalid");
					}

					Thread.sleep(heartbeatInterval);
					ArrayMap<String, Object> message = new ArrayMap<>();
					message.put("op", 1);
					message.put("d", seq == 0 ? "null" : Integer.toString(seq));
					sendToClient(message);

				} catch (Exception e) {
					Log.e(LOG_TAG, e.toString());
				}
			}
		};
	}

	public void buildClient(String json) {
		ArrayMap<String, Object> activity = gson.fromJson(json, new TypeToken<ArrayMap<String, Object>>() {}.getType());

		ArrayMap<String, Object> presence = new ArrayMap<String, Object>();
		presence.put("activities", new Object[] {
			activity
		});
		presence.put("afk", true);
		presence.put("since", start_timestamps);
		presence.put("status", status);

		rpc.put("op", 3);
		rpc.put("d", presence);

		createClient();
	}

	public void updateClient(String json) {
		ArrayMap<String, Object> activity = gson.fromJson(json, new TypeToken<ArrayMap<String, Object>>() {}.getType());

		ArrayMap<String, Object> presence = new ArrayMap<String, Object>();
		presence.put("activities", new Object[] {
			activity
		});
		presence.put("afk", true);
		presence.put("since", start_timestamps);
		presence.put("status", status);

		rpc.put("op", 3);
		rpc.put("d", presence);

		sendToClient(rcp);
	}

	public void sendIdentify() {
		ArrayMap<String, Object> prop = new ArrayMap<String, Object>();
		prop.put("os", "Linux");
		prop.put("browser", "Unknown");
		prop.put("device", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");

		ArrayMap<String, Object> data = new ArrayMap<String, Object>();
		data.put("token", token);
		data.put("properties", prop);
		data.put("compress", false);
		data.put("intents", 0);

		ArrayMap<String, Object> identify = new ArrayMap<String, Object>();
		identify.put("op", 2);
		identify.put("d", data);

		sendToClient(identify);
	}

	public void createClient() {
		Log.i(LOG_TAG, "Connecting...");

		webSocketClient = new WebSocketClient(new URI("wss://gateway.discord.gg/?encoding=json&v=10")) {
			@Override
			public void onOpen(ServerHandshake s) {
				Log.i(LOG_TAG, s.getHttpStatusMessage());
			}

			@Override
			public void onMessage(String message) {
				ArrayMap<String, Object> map = gson.fromJson(
					message, new TypeToken < ArrayMap<String, Object>>() {}.getType()
				);
				Object o = map.get("s");

				if (o != null) {
					seq = ((Double) o).intValue();
				}

				switch (((Double) map.get("op")).intValue()) {
					case 0:
						if (((String) map.get("t")).equals("READY")) {
							sessionId = ((Map) map.get("d")).get("session_id").toString();
							Log.i(LOG_TAG, "Connected!");

							sendToClient(rcp);
							return;
						}
						break;
					case 10: // Hello
						if (!reconnectSession) {
							Map data = (Map) map.get("d");
							heartbeatInterval = ((Double) data.get("heartbeat_interval")).intValue();
							heartbeatThread = new Thread(heartbeatRunnable);
							heartbeatThread.start();
							sendIdentify();
						} else {
							Log.i(LOG_TAG, "Sending Reconnect...");
							Map data = (Map) map.get("d");
							heartbeatInterval = ((Double) data.get("heartbeat_interval")).intValue();
							heartbeatThreadead = new Thread(heartbeatRunnable);
							heartbeatThreadead.start();
							reconnectSession = false;

							ArrayMap<String, Object> data = new ArrayMap<String, Object>();
							data.put("token", token);
							data.put("session_id", sessionId);
							data.put("seq", seq);

							ArrayMap<String, Object> message = new ArrayMap<String, Object>();
							message.put("op", 6);
							message.put("d", data);

							sendToClient(message);
						}
						break;
					case 1:
						if (!heartbeatThreadead.interrupted()) {
							heartbeatThreadead.interrupt();
						}

						ArrayMap<String, Object> message = new ArrayMap<String, Object>();
						message.put("op", 1);
						message.put("d", seq == 0 ? "null" : Integer.toString(seq));
						sendToClient(message);
						break;
					case 11:
						if (!heartbeatThreadead.interrupted()) {
							heartbeatThreadead.interrupt();
						}

						heartbeatThread = new Thread(heartbeatRunnable);
						heartbeatThread.start();
						break;
					case 7:
						reconnectSession = true;
						webSocketClient.close(4000);
						break;
					case 9:
						if (!heartbeatThreadead.isInterrupted()) {
							heartbeatThreadead.interrupt();
							heartbeatThreadead = new Thread(heartbeatRunnable);
							heartbeatThreadead.start();
							sendIdentify();
						}
						break;
				}
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				if (code == 4000) {
					reconnectSession = true;
					heartbeatThread.interrupt();
					Log.e(LOG_TAG, "Closed Socket");
					Thread heartbeatNewThread = new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(200);
								reconnect();
							} catch (Exception e) {
								Log.e(LOG_TAG, e.toString());
							}
						}
					});
					heartbeatNewThread.start();
				} else {
					throw new RuntimeException("Invalid");
				}
			}

			@Override
			public void onError(Exception e) {
				if (!e.getMessage().equals("Interrupt")) {
					closeClient();
				} else {
					Log.e(LOG_TAG, e.getMessage());
				}
			}
		};
		webSocketClient.connect();
	}

	private void sendToClient(Object obj) {
		if (webSocketClient != null)
			webSocketClient.send(gson.toJson(obj));
	}

	public void closeClient() {
		if (heartbeatThread != null && !heartbeatThread.isInterrupted())
			heartbeatThread.interrupt();

		if (webSocketClient != null)
			webSocketClient.close(1000);
	}

	public boolean isClientRunning() {
		if (webSocketClient != null)
			return webSocketClient.isOpen();

		return false;
	}
}
