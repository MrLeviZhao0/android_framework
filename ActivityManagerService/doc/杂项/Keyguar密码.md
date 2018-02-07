# Keyguard密码校验过程 #

```
 * Author: 赵越                                                                                                                         
 * Date: 2017/12/6
```

## 密码校验起点 ##

密码一般只有在锁屏的时候才会被使用，所以关注KeyguardXXXView的父类KeyguardAbsKeyInputView。这个类会负责取子类实现的getPasswordText的值，用这个值去校验密码，得知密码是否正确。

```
KeyguardAbsKeyInputView.java

	protected void verifyPasswordAndUnlock() {
		//获取子类实现的getPasswordText
        String entry = getPasswordText();
		//验证password，使用的是LockPatternUtils这个工具类
        if (mLockPatternUtils.checkPassword(entry)) {
            ............................................
        } else {
            ............................................
        }
    }
```

接下来关注的是LockPatternUtils。
```
LockPatternUtils.java

	//当没设置密码的时候，返回值一定是true
	public boolean checkPassword(String password) {
		//获取当前的User的ID，不同用户有不同的密码。默认用户的ID是0.
        final int userId = getCurrentOrCallingUserId();
        try {
			//验证密码是否正确
            return getLockSettings().checkPassword(password, userId);
        } catch (RemoteException re) {
            return true;
        }
    }

	//从ServiceManager中获取LockSettingService的proxy端，进行binder请求
	private ILockSettings getLockSettings() {
        if (mLockSettingsService == null) {
			//lock_settings是注册到ServiceManager中的LockSettingService服务
            ILockSettings service = ILockSettings.Stub.asInterface(
                    ServiceManager.getService("lock_settings"));
            mLockSettingsService = service;
        }
        return mLockSettingsService;
    }

```

如上代码，我们可以看到实际调用的是，LockSettingsService中的函数。

```
LockSettingsService.java

	@Override
    public boolean checkPassword(String password, int userId) throws RemoteException {
		//验证是否有"LockSettingsRead"的权限
        checkPasswordReadPermission(userId);

		//对password进行加盐哈希散列，盐值是userId
        byte[] hash = mLockPatternUtils.passwordToHash(password, userId);
		//获取存储的哈希散列值
        byte[] storedHash = mStorage.readPasswordHash(userId);

		//密码未初始化，则只返回true，即验证通过
        if (storedHash == null) {
            return true;
        }
		//校验生成的与存储的哈希散列值
        boolean matched = Arrays.equals(hash, storedHash);
        if (matched && !TextUtils.isEmpty(password)) {
            maybeUpdateKeystore(password, userId);
        }
        return matched;
    }
```

这里要特别说明的有几点:

* 加盐哈希可以保证不能逆向破解密码。即你知道加盐哈希值，但是你不能反推出我的密码原文。[这个原理可以参考](https://www.zhihu.com/question/20299384)
* 在系统中只会存储哈希后的byte数组，可以保证即使root权限被破解，也不能从系统中获取用户的密码。
* 密码的存储读取实际是使用的LockSettingsStorage。实际存储路径是/system/password.key。
* 删除掉相应路径的文件，可以使密码失效。

