package android.kizzy;

#if (!android && !native && macro)
#error 'extension-kizzyrpc is not supported on your current platform'
#end
import lime.system.JNI;

using StringTools;

/**
 * @author Mihai Alexandru (M.A. Jigsaw)
 */
class KizzyClient
{
	//////////////////////////////////////////////////////

	private var constructor:Dynamic;

	//////////////////////////////////////////////////////

	public function new(token:String):Void
	{
		var constructor_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/KizzyClient', '<init>', '(Ljava/lang/String;)V');
                constructor = constructor_jni(token);
	}

	//////////////////////////////////////////////////////

	public function setApplicationID(id:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setApplicationID', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [id]);
		return this;
	}

	public function setName(name:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setName', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [name]);
		return this;
	}

	public function setDetails(details:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setDetails', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [details]);
		return this;
	}

	public function setState(state:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setState', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [state]);
		return this;
	}

	public function setLargeImage(link:String):KizzyClient
	{
		if (link.startsWith('https://media.discordapp.net/'))
			link = link.replace('https://media.discordapp.net/', '');

		if (!link.startsWith('attachments/'))
			link = link + 'attachments/';

		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setLargeImage', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [link]);
		return this;
	}

	public function setSmallImage(link:String):KizzyClient
	{
		if (link.startsWith('https://media.discordapp.net/'))
			link = link.replace('https://media.discordapp.net/', '');

		if (!link.startsWith('attachments/'))
			link = link + 'attachments/';

		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setSmallImage', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [link]);
		return this;
	}

	public function setStartTimeStamps(time:Int, useMilliseconds:Bool = true):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setStartTimeStamps', '(IZ)Lorg/haxe/extension/KizzyClient;'), constructor, [time, useMilliseconds]);
		return this;
	}

	public function setStopTimeStamps(time:Int, useMilliseconds:Bool = true):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setStopTimeStamps', '(IZ)Lorg/haxe/extension/KizzyClient;'), constructor, [time, useMilliseconds]);
		return this;
	}

	public function setType(type:Int):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setType', '(I)Lorg/haxe/extension/KizzyClient;'), constructor, [type]);
		return this;
	}

	public function setStatus(status:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setStatus', '(Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [status]);
		return this;
	}

	public function setButton1(label:String, url:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setButton1', '(Ljava/lang/String;Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [label, url]);
		return this;
	}

	public function setButton2(label:String, url:String):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'setButton2', '(Ljava/lang/String;Ljava/lang/String;)Lorg/haxe/extension/KizzyClient;'), constructor, [label, url]);
		return this;
	}

	public function closeOnDestroy(close:Bool = false):KizzyClient
	{
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'closeOnDestroy', '(Z)Lorg/haxe/extension/KizzyClient;'), constructor, [close]);
		return this;
	}

	//////////////////////////////////////////////////////

	public function rebuildClient():Void
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'rebuildClient', '()V'), constructor, []);

	public function closeClient():Void
		JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'closeClient', '()V'), constructor, []);

	public function isClientRunning():Bool
		return JNI.callMember(JNI.createMemberMethod('org/haxe/extension/KizzyClient', 'isClientRunning', '()Z'), constructor, []);

	//////////////////////////////////////////////////////
}
