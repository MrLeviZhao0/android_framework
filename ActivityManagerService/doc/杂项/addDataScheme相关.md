# addDataScheme相关问题 #

## 发现的问题 ##

最近开发的时候发现了一个与BroadcastReceiver收不到广播的问题。

希望收到MediaScanner扫描开始与完毕的广播。所以会设置IntentFilter.  

```
		filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
```

最开始是将apk使用signApk工具进行系统签名。
可以正常接收广播。

但在应用层直接运行时无法收到的。这个时候在网上查询，发现问题出在少了一个addDataScheme。

正常代码如下：
```
		filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addDataScheme("file");
```

在各种博客中会着重描述BroadcastReceiver的使用方式。而本文主要是研究DataScheme是怎么作用于BroadcastReceiver的。

首先来看看源码是怎么描述的：


```
IntentFilter.java

	/**
     * Add a new Intent data scheme to match against.  If any schemes are
     * included in the filter, then an Intent's data must be <em>either</em>
     * one of these schemes <em>or</em> a matching data type.  If no schemes
     * are included, then an Intent will match only if it includes no data.
     *
     * <p><em>Note: scheme matching in the Android framework is
     * case-sensitive, unlike formal RFC schemes.  As a result,
     * you should always write your schemes with lower case letters,
     * and any schemes you receive from outside of Android should be
     * converted to lower case before supplying them here.</em></p>
     *
     * @param scheme Name of the scheme to match, i.e. "http".
     *
     * @see #matchData
     */
	/**
	* 增加一个scheme到Intent的匹配中。如果filter中有任何scheme，
	* Intent的data要么是scheme中的一个或者是匹配的数据类型。
	* 如果没有scheme的话，只会匹配没有data的Intent
	* 
	* 注意：scheme匹配在framework里面是大小写敏感的，不像一般的Request For Comments（RFC）匹配规则。
	* 所以需要注意写小写的scheme，且在安卓系统外部获取的sheme需要转化为小写才能提供到这个接口中。
	* 
	*/
    public final void addDataScheme(String scheme) {
        if (mDataSchemes == null) mDataSchemes = new ArrayList<String>();
        if (!mDataSchemes.contains(scheme)) {
			//不存在则增加scheme，intern是返回变量池中同样内容的一个副本
            mDataSchemes.add(scheme.intern());
        }
    }
```

注释中并没有明确的指出不同权限对于scheme匹配规则的差异。且匹配规则还是不是非常明确，似乎意思是scheme是data的类型。所以接下来去源码中验证一下这个想法：

```
MediaScannerService.java

	private void scan(String[] directories, String volumeName) {
		//组合请求刷新的uri
        Uri uri = Uri.parse("file://" + directories[0]);
        
        // don't sleep while scanning
        mWakeLock.acquire();

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MEDIA_SCANNER_VOLUME, volumeName);
            Uri scanUri = getContentResolver().insert(MediaStore.getMediaScannerUri(), values);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED, uri));

            try {
                if (volumeName.equals(MediaProvider.EXTERNAL_VOLUME)) {
                    openDatabase(volumeName);
                }

            MediaScanner scanner = createMediaScanner();
	    if(directories!=null){
              scanner.scanDirectories(directories, volumeName);
	    }
        } catch (Exception e) {
            Log.e(TAG, "exception in MediaScanner.scan()", e);
        }

            getContentResolver().delete(scanUri, null, null);

        } finally {
			//重点关注这个广播，是调用的Intent的Intent(String,Uri)的构造方法
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED, uri));
            mWakeLock.release();
        }
    }
```

```
Intent.java

	public Intent(String action, Uri uri) {
		//设置action 记录mData
        setAction(action);
        mData = uri;
    }

	//同时我们注意一下获取的scheme是什么值
	public String getScheme() {
		//从mData中调用getScheme
        return mData != null ? mData.getScheme() : null;
    }


```
接着我们继续跟踪getScheme得到的线索。
```
Uri.java

//class uri的一个子类，实现了如getScheme的具体逻辑
private static class StringUri extends AbstractHierarchicalUri {

		public String getScheme() {
            @SuppressWarnings("StringEquality")
			//查询当前scheme是否缓存，缓存了直接调用scheme
            boolean cached = (scheme != NOT_CACHED);
            return cached ? scheme : (scheme = parseScheme());
        }

        private String parseScheme() {
			//获取scheme分割符的标签坐标，并通过substring获取
            int ssi = findSchemeSeparator();
            return ssi == NOT_FOUND ? null : uriString.substring(0, ssi);
        }
		
		/** Finds the first ':'. Returns -1 if none found. */
		//查找第一个":"符号在uriString的位置
        private int findSchemeSeparator() {
            return cachedSsi == NOT_CALCULATED
                    ? cachedSsi = uriString.indexOf(':')
                    : cachedSsi;
        }

}

```
所以如上所示，一个uri，比如  "file://data/data/"，它的scheme是 "file"。而scheme是从Intent的mData中可以提取的。

## 扩展的问题 ##

到了这一步，其实线索已经非常明显了，scheme是data的种类，可以通过scheme来过滤Intent。那么问题也来了：

1. IntentFilter中加data来过滤请求可不可以？用scheme的目的是什么？
2. IntentFilter中还有什么过滤机制？如何影响IntentFilter?

[大佬关于IntentFilter的详解](http://blog.csdn.net/mynameishuangshuai/article/details/51673273)

静态注册IntentFilter的时候有

```
	<intent-filter>
        <action/>
        <data/>
        <category/>
    </intent-filter>
```

动态注册时，如下图所示

![附图](addDtaScheme相关0.png)

action和category都可以正常添加。注册data的时候是将其拆分为很多个部分。

data的结构：  
```
<scheme>://<host>:<port>/<path>
```

而各部分可以由以下几个方法来设置：
public final void addDataScheme(String scheme) 
public final void addDataAuthority(String host, String port)
public final void addDataPath(String path, int type)

所以以上两个问题可以得到回答。

1. IntentFilter可以加过滤请求。加scheme是匹配前一段，比如现在uri有很多种，而我只需要管是file类型的就行了。这时候需要设置scheme。
2. IntentFilter中有action、data、category。具体的关系与匹配规则可以看相关的一篇博客。

