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
public class KizzyRPC extends Extension {

	public static final String LOG_TAG = "KizzyRPC";

	private String token;
	private String session_id;

	private ArrayMap<String, Object> rpc = new ArrayMap<String, Object>();

	private WebSocketClient webSocketClient;
	private Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

	private Runnable heartbeatRunnable;

	private Thread heartbeatThr;

	private int heartbeat_interval, seq;

	private boolean reconnect_session = false;

	public KizzyRPC(String token) {

		this.token = token;

		heartbeatRunnable = new Runnable() {
			public void run() {
				try {
					if (heartbeat_interval < 10000) {
						throw new RuntimeException("Invalid");
					}

					Thread.sleep(heartbeat_interval);
					ArrayMap<String, Object> message = new ArrayMap<>();
					message.put("op", 1);
					message.put("d", seq == 0 ? "null" : Integer.toString(seq));
					webSocketClient.send(gson.toJson(message));

				} catch (Exception e) {
					Log.e(LOG_TAG, e.toString());
				}
			}
		};
	}

	public void buildClient(String json) {
		ArrayMap<String, Object> activity = gson.fromJson(json, new TypeToken<ArrayMap<String, Object>>() {}.getType());

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

	public void sendIdentify() {
		ArrayMap<String, Object> prop = new ArrayMap<>();
		prop.put("os", "Linux");
		prop.put("browser", "Chrome");
		prop.put("device", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");

		ArrayMap<String, Object> data = new ArrayMap<>();
		data.put("token", token);
		data.put("properties", prop);
		data.put("compress", false);
		data.put("intents", 0);

		ArrayMap<String, Object> identify = new ArrayMap<>();
		identify.put("op", 2);
		identify.put("d", data);

		sendToClient(identify);
	}

	private void createClient() {
		Log.i(LOG_TAG, "Connecting...");

		ArrayMap<String, String> headerMap = new ArrayMap<>();

		webSocketClient = new WebSocketClient(new URI("wss://gateway.discord.gg/?encoding=json&v=10"), headerMap) {
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
							session_id = ((Map) map.get("d")).get("session_id").toString();
							Log.i(LOG_TAG, "Connected!");

							sendToClient(rcp);
							return;
						}
						break;
					case 10: // Hello
						if (!reconnect_session) {
							Map data = (Map) map.get("d");
							heartbeat_interval = ((Double) data.get("heartbeat_interval")).intValue();
							heartbeatThr = new Thread(heartbeatRunnable);
							heartbeatThr.start();
							sendIdentify();
						} else {
							Log.i(LOG_TAG, "Sending Reconnect...");
							Map data = (Map) map.get("d");
							heartbeat_interval = ((Double) data.get("heartbeat_interval")).intValue();
							heartbeatThr = new Thread(heartbeatRunnable);
							heartbeatThr.start();
							reconnect_session = false;

							ArrayMap<String, Object> data = new ArrayMap<>();
							data.put("token", token);
							data.put("session_id", session_id);
							data.put("seq", seq);

							ArrayMap<String, Object> message = new ArrayMap<>();
							message.put("op", 6);
							message.put("d", data);

							sendToClient(message);
						}
						break;
					case 1:
						if (!heartbeatThr.interrupted()) {
							heartbeatThr.interrupt();
						}

						ArrayMap<String, Object> message = new ArrayMap<>();
						message.put("op", 1);
						message.put("d", seq == 0 ? "null" : Integer.toString(seq));

						sendToClient(message);
						break;
					case 11:
						if (!heartbeatThr.interrupted()) {
							heartbeatThr.interrupt();
						}

						heartbeatThr = new Thread(heartbeatRunnable);
						heartbeatThr.start();
						break;
					case 7:
						reconnect_session = true;
						webSocketClient.close(4000);
						break;
					case 9:
						if (!heartbeatThr.isInterrupted()) {
							heartbeatThr.interrupt();
							heartbeatThr = new Thread(heartbeatRunnable);
							heartbeatThr.start();
							sendIdentify();
						}
						break;
				}
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				if (code == 4000) {
					reconnect_session = true;
					heartbeatThr.interrupt();
					Log.e(LOG_TAG, "Closed Socket");
					Thread newTh = new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(200);
								reconnect();
							} catch (Exception e) {
								Log.e(LOG_TAG, e.toString());
							}
						}
					});
					newTh.start();
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

	public void sendToClient(Object obj) {
		if (webSocketClient != null)
			webSocketClient.send(gson.toJson(obj));
	}

	public void closeClient() {
		if (heartbeatThr != null && !heartbeatThr.isInterrupted())
			heartbeatThr.interrupt();

		if (webSocketClient != null)
			webSocketClient.close(1000);
	}

	public boolean isClientRunning() {
		if (webSocketClient != null)
			return webSocketClient.isOpen;

		return false;
	}
}
