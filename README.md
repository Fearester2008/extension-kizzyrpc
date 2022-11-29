extension-kizzyrpc
=======

A extension that uses Lime JNI to work that add's support for discord rpc to android.

Installation instructions
=======

Step 1. Install the Haxelib.

You can install it through haxelib:

```
haxelib install extension-kizzyrpc
```

You can also install it through Git for the latest updates:

```
haxelib git extension-kizzyrpc https://github.com/MAJigsaw77/extension-kizzyrpc.git
```

Step 2. Add this in `Project.xml`.

```xml
<haxelib name="extension-kizzyrpc" if="android" />
```

Step 3. Done, this is a little example for how to use it.

```haxe
import android.kizzy.KizzyClient;

var kizzyClient:KizzyClient = new KizzyClient(token); // your account token
kizzyClient.setApplicationID('378534231036395521');
kizzyClient.setName('Kizzy RPC Client Android');
kizzyClient.setDetails('When RPC is sus');
kizzyClient.setLargeImage('attachments/973256105515974676/983674644823412798/unknown.png');
kizzyClient.setSmallImage('attachments/948828217312178227/948840504542498826/Kizzy.png');
kizzyClient.setStartTimeStamps(0, true);
kizzyClient.setButton1('YouTube', 'https://youtube.com/@m.a.jigsaw7297');
kizzyClient.setType(0);
kizzyClient.setState('State');
kizzyClient.setStatus('idle');
kizzyClient.closeOnDestroy(true);
kizzyClient.rebuildClient();
```
If you used this code, on `Discord` it will look like this:

![](https://media.discordapp.net/attachments/1043613190535258242/1047082598498828299/IMG_20221129_113154.jpg)

Credits
=======

- [M.A. Jigsaw](https://github.com/MAJigsaw77) - Creator of this extension.
- The contributors.
