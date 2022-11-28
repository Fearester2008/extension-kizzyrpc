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
	private String session_id;
	private String application_id;
	private String name;
	private String details;
	private String state;
	private String large_image;
	private String small_image;
	private String status;

	private Long start;
	private Long stop;

	private int type = 0;
	private int seq = 0;

	private boolean reconnectSession = false;

	private ArrayList<String> buttons = new ArrayList<String>();
	private ArrayList<String> button_urls = new ArrayList<String>();

	private ArrayMap<String, Object> rpc = new ArrayMap<String, Object>();
	private Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

	private WebSocketClient webSocketClient;

	private int heartbeatInterval = 0;
	private Runnable heartbeatRunnable;
	private Thread heartbeatThread;

	public KizzyClient(String token) {

		this.token = token;

		heartbeatRunnable = new Runnable() {
			public void run() {
				try {
					if (heartbeatInterval < 10000) {
						throw new RuntimeException("Invalid");
					}

					Thread.sleep(heartbeatInterval);

					ArrayMap<String, Object> obj = new ArrayMap<String, Object>();
					obj.put("op", 1);
					obj.put("d", seq == 0 ? "null" : Integer.toString(seq));
					sendToClient(obj);

				} catch (Exception e) {
					Log.e(LOG_TAG, e.toString());
				}
			}
		};
	}

	//////////////////////////////////////////////////////

	public KizzyClient setApplicationID(String id) {
		this.application_id = id;
		return this;
	}

	public KizzyClient setName(String name) {
		this.name = name;
		return this;
	}

	public KizzyClient setDetails(String details) {
		this.details = details;
		return this;
	}

	public KizzyClient setState(String state) {
		this.state = state;
		return this;
	}

	public KizzyClient setLargeImage(String large_image) {
		this.large_image = "mp:" + large_image;
		return this;
	}

	public KizzyClient setSmallImage(String link) {
		this.small_image = "mp:" + link;
		return this;
	}

	public KizzyClient setStartTimeStamps(Long timestamps) {
		this.start = timestamps;
		return this;
	}

	public KizzyClient setStopTimeStamps(Long timestamps) {
		this.stop = timestamps;
		return this;
	}

	public KizzyClient setType(int type) {
		this.type = type;
		return this;
	}

	public KizzyClient setStatus(String status) {
		this.status = status;
		return this;
	}

	public KizzyClient setButton1(String label, String link) {
		buttons.add(label);
		button_urls.add(link);
		return this;
	}

	public KizzyClient setButton2(String label, String link) {
		buttons.add(label);
		button_urls.add(link);
		return this;
	}

	//////////////////////////////////////////////////////

	public void rebuildClient() {
		ArrayMap<String, Object> activity = new ArrayMap<String, Object>();
		activity.put("application_id", application_id);
		activity.put("name", name);
		activity.put("details", details);
		activity.put("state", state);
		activity.put("type", type);

		ArrayMap<String, Object> timestamps = new ArrayMap<String, Object>();
		timestamps.put("start", start);
		timestamps.put("stop", stop);
		activity.put("timestamps", timestamps);

		ArrayMap<String, Object> assets = new ArrayMap<String, Object>();
		assets.put("large_image", large_image);
		assets.put("small_image", small_image);
		activity.put("assets", assets);

		if (buttons.size() > 0) {
			activity.put("buttons", buttons);

			ArrayMap<String, Object> metadata = new ArrayMap<String, Object>();
			metadata.put("button_urls", button_urls);
			activity.put("metadata", metadata);
		}

		ArrayMap<String, Object> d = new ArrayMap<String, Object>();
		d.put("activities", new Object[] {
			activity
		});
		d.put("afk", true);
		d.put("since", start);
		d.put("status", status);

		rpc.put("op", 3);
		rpc.put("d", d);

		if (isClientRunning()) {
			sendToClient(rpc);
		} else {
			createClient();
		}
	}

	private void sendIdentify() {
		ArrayMap<String, Object> properties = new ArrayMap<String, Object>();
		properties.put("os", "Linux");
		properties.put("browser", "Unknown");
		properties.put("device", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");

		ArrayMap<String, Object> d = new ArrayMap<String, Object>();
		d.put("capabilities", 65);
		d.put("compress", false);
		d.put("largeThreshold", 100);
		d.put("properties", properties);
		d.put("token", token);

		ArrayMap<String, Object> identify = new ArrayMap<String, Object>();
		identify.put("op", 2);
		identify.put("d", d);

		sendToClient(identify);
	}

	//////////////////////////////////////////////////////

	private void createClient() {
		URI uri;

		try {
			uri = new URI("wss://gateway.discord.gg/?encoding=json&v=10");
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			return;
		}

		Log.i(LOG_TAG, "Connecting...");

		webSocketClient = new WebSocketClient(uri) {
			@Override
			public void onOpen(ServerHandshake s) {
				Log.i(LOG_TAG, s.getHttpStatusMessage());
			}

			@Override
			public void onMessage(String message) {
				ArrayMap<String, Object> map = gson.fromJson(
					message, new TypeToken < ArrayMap<String, Object>>() {}.getType()
				);

				if (map.get("s") != null) {
					seq = ((Double) map.get("s")).intValue();
				}

				int op = ((Double) map.get("op")).intValue();
				switch (op) {
					case 0:
						if (map.get("t").toString().equals("READY")) {
							Map d = (Map) map.get("d");
							session_id = d.get("session_id").toString();

							Log.i(LOG_TAG, "Connected!");
							sendToClient(rpc);
							return;
						}
					case 1:
						if (heartbeatThread != null && !heartbeatThread.interrupted()) {
							heartbeatThread.interrupt();
						}

						ArrayMap<String, Object> obj = new ArrayMap<String, Object>();
						obj.put("op", 1);
						obj.put("d", seq == 0 ? "null" : Integer.toString(seq));
						sendToClient(obj);
						break;
					case 7:
						reconnectSession = true;
						webSocketClient.close(4000);
						break;
					case 9:
						if (heartbeatThread != null && !heartbeatThread.interrupted()) {
							heartbeatThread.interrupt();
						}

						heartbeatThread = new Thread(heartbeatRunnable);
						heartbeatThread.start();
						sendIdentify();
						break;
					case 10:
						if (heartbeatThread != null && !heartbeatThread.interrupted()) {
							heartbeatThread.interrupt();
						}

						Map mapd = (Map) map.get("d");
						heartbeatInterval = ((Double) mapd.get("heartbeat_interval")).intValue();
						heartbeatThread = new Thread(heartbeatRunnable);
						heartbeatThread.start();

						if (reconnectSession) {
							reconnectSession = false;

							ArrayMap<String, Object> d = new ArrayMap<String, Object>();
							d.put("token", token);
							d.put("session_id", session_id);
							d.put("seq", seq);

							ArrayMap<String, Object> obj = new ArrayMap<String, Object>();
							obj.put("op", 6);
							obj.put("d", d);
							sendToClient(obj);
						} else {
							sendIdentify();
						}

						break;
					case 11:
						if (heartbeatThread != null && !heartbeatThread.interrupted()) {
							heartbeatThread.interrupt();
						}

						heartbeatThread = new Thread(heartbeatRunnable);
						heartbeatThread.start();
						break;
				}
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				if (code == 4000) {
					reconnectSession = true;

					if (heartbeatThread != null && !heartbeatThread.isInterrupted())
						heartbeatThread.interrupt();

					Log.i(LOG_TAG, "Closed Socket");
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
					Log.e(LOG_TAG, e.toString());
				}
			}
		};
		webSocketClient.connect();
	}

	//////////////////////////////////////////////////////

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

	//////////////////////////////////////////////////////
}
