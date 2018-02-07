# 概述管理范围 #

AMS起作用的范围可以从AMS的dump日志中找到痕迹。


dumpsys activity -h的结果:  
```
Activity manager dump options:  
  [-a] [-c] [-p PACKAGE] [-h] [WHAT] ...  
  WHAT may be one of:  
    a[ctivities]: activity stack state  
    r[recents]: recent activities state  
    b[roadcasts] [PACKAGE_NAME] [history [-s]]: broadcast state  
    broadcast-stats [PACKAGE_NAME]: aggregated broadcast statistics  
    i[ntents] [PACKAGE_NAME]: pending intent state  
    p[rocesses] [PACKAGE_NAME]: process state  
    o[om]: out of memory management  
    perm[issions]: URI permission grant state  
    prov[iders] [COMP_SPEC ...]: content provider state  
    provider [COMP_SPEC]: provider client-side state  
    s[ervices] [COMP_SPEC ...]: service state  
    as[sociations]: tracked app associations  
    service [COMP_SPEC]: service client-side state  
    package [PACKAGE_NAME]: all state related to given package  
    all: dump all activities  
    top: dump the top activity  
  WHAT may also be a COMP_SPEC to dump activities.  
  COMP_SPEC may be a component name (com.foo/.myApp),  
    a partial substring in a component name, a  
    hex object identifier.  
  -a: include all available server state.  
  -c: include client state.  
  -p: limit output to given package.  
  --checkin: output checkin format, resetting data.  
  --C: output checkin format, not resetting data.  
```


可以看到，比较关键的有**Activity**,**Service**,**record**,**intent**等。   
从日志中可以发现比较重要的任务有：  
- 查询Activity Stack  
- 查询Service状态  
- 查询provider的状态  
- 查询broadcast的状态  
- 查询pending intent

可以看到四大组件都被AMS管理。同时对Activity Stack进行管控。

在SystemServer.java的run()函数中有  
<code>
startBootstrapServices();  
startCoreServices();  
startOtherServices();  
</code>
  
而函数中有：
```
private void startBootstrapServices() {
        // Activity manager runs the show.
        mActivityManagerService = mSystemServiceManager.startService(
                ActivityManagerService.Lifecycle.class).getService();
        mActivityManagerService.setSystemServiceManager(mSystemServiceManager);
        mActivityManagerService.setInstaller(installer);
}
```
可以看到AMS是系统启动过程中非常关键的一个服务。



