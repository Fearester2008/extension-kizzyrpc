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

	String token;
	String applicationId, activity_name, details, state, large_image, small_image, status;
	Long start_timestamps, stop_timestamps;
	int type;
	ArrayMap<String, Object> rpc = new ArrayMap<>();
	WebSocketClient webSocketClient;
	Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

	public Runnable heartbeatRunnable;

	public Thread heartbeatThr;

	public int heartbeat_interval, seq;

	private String session_id;

	private Boolean reconnect_session = false;
	ArrayList<String> buttons = new ArrayList<>();
	ArrayList<String> button_url = new ArrayList<>();

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

	/**
	 * Application Id for Rpc
	 * An application id is required for functioning of urls in buttons
	 * @param activity_name
	 * @return
	 */

	public KizzyRPCservice setApplicationId(String applicationId) {
		this.applicationId = applicationId;
		return this;
	}

	/**
	 * Activity Name of Rpc
	 *
	 * @param activity_name
	 * @return
	 */

	public KizzyRPCservice setName(String activity_name) {
		this.activity_name = activity_name;
		return this;
	}

	/**
	 * Details of Rpc
	 *
	 * @param details
	 * @return
	 */

	public KizzyRPCservice setDetails(String details) {
		this.details = details;
		return this;
	}

	/**
	 * Rpc State
	 *
	 * @param state
	 * @return
	 */

	public KizzyRPCservice setState(String state) {
		this.state = state;
		return this;
	}

	/**
	 * Large image on rpc
	 * How to get Image ?
	 * Upload image to any discord chat and copy its media link it should look like "https://media.discordapp.net/attachments/90202992002/xyz.png" now just use the image link from attachments part
	 * so it would look like: .setLargeImage("attachments/90202992002/xyz.png")
	 * @param large_image
	 * @return
	 */

	public KizzyRPCservice setLargeImage(String large_image) {
		this.large_image = "mp:" + large_image;
		return this;
	}

	/**
	 * Small image on Rpc
	 *
	 * @param small_image
	 * @return
	 */

	public KizzyRPCservice setSmallImage(String small_image) {
		this.small_image = "mp:" + small_image;
		return this;
	}

	/**
	 * start timestamps
	 *
	 * @param start_timestamps
	 * @return
	 */

	public KizzyRPCservice setStartTimestamps(Long start_timestamps) {
		this.start_timestamps = start_timestamps;
		return this;
	}

	/**
	 * stop timestamps
	 *
	 * @param stop_timestamps
	 * @return
	 */

	public KizzyRPCservice setStopTimestamps(Long stop_timestamps) {
		this.stop_timestamps = stop_timestamps;
		return this;
	}

	/**
	 * Activity Types
	 * 0: Playing
	 * 1: Streaming
	 * 2: Listening
	 * 3: Watching
	 * 5: Competing
	 *
	 * @param type
	 * @return
	 */

	public KizzyRPCservice setType(int type) {
		this.type = type;
		return this;
	}

	/**
	 * Status type for profile online,idle,dnd
	 *
	 * @param status
	 * @return
	 */

	public KizzyRPCservice setStatus(String status) {
		this.status = status;
		return this;
	}

	/**
	 * Button1 text
	 * @param status
	 * @return
	 */

	public KizzyRPCservice setButton1(String button_label, String link) {
		buttons.add(button_label);
		button_url.add(link);
		return this;
	}

	/**
	 * Button2 text
	 * @param button1_Text
	 * @return
	 */

	public KizzyRPCservice setButton2(String button_label, String link) {
		buttons.add(button_label);
		button_url.add(link);
		return this;
	}

	public void build() {
		ArrayMap<String, Object> presence = new ArrayMap<>();
		ArrayMap<String, Object> activity = new ArrayMap<>();
		activity.put("application_id", applicationId);
		activity.put("name", activity_name);
		activity.put("details", details);
		activity.put("state", state);
		activity.put("type", type);

		ArrayMap<String, Object> timestamps = new ArrayMap<>();
		timestamps.put("start", start_timestamps);
		timestamps.put("stop", stop_timestamps);
		activity.put("timestamps", timestamps);

		ArrayMap<String, Object> assets = new ArrayMap<>();
		assets.put("large_image", large_image);
		assets.put("small_image", small_image);
		activity.put("assets", assets);

		if (buttons.size() > 0) {
			ArrayMap<String, Object> metadata = new ArrayMap<>();
			activity.put("buttons", buttons);
			metadata.put("button_urls", button_url);
			activity.put("metadata", metadata);
		}

		presence.put("activities", new Object[] {
			activity
		});
		presence.put("afk", true);
		presence.put("since", start_timestamps);
		presence.put("status", status);

		rpc.put("op", 3);
		rpc.put("d", presence);

		createWebsocketClient();
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

		webSocketClient.send(gson.toJson(identify));
	}

	public void createWebsocketClient() {
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

							webSocketClient.send(gson.toJson(rpc));
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

							webSocketClient.send(gson.toJson(message));
						}
						break;
					case 1:
						if (!heartbeatThr.interrupted()) {
							heartbeatThr.interrupt();
						}

						ArrayMap<String, Object> message = new ArrayMap<>();
						message.put("op", 1);
						message.put("d", seq == 0 ? "null" : Integer.toString(seq));

						webSocketClient.send(gson.toJson(message));
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
