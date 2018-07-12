# O版本各操作日志

event log 主要输出系统的一些服务的日志，比较容易追踪问题。
tag一般可以直接openGrok，或直接是openGrok查对应的id。
还有可能是在 out/target/common/obj/JAVA_LIBRARIES/services.core_intermediates/src/java/com/android/server/am/EventLogTags.java 中，需要在生成文件里搜索。

操作：

1. 基础启动与销毁
2. 分屏
3. 转屏
4. 亮/灭屏

## 基础启动与销毁

正常activity创建与销毁过程分析以下过程：

1. 桌面单击启动应用
2. 应用内启动另一个activity
3. back键退出activity（task不为空）
4. back键退出activity（task将变成空）

### 桌面单击启动应用

06-27 11:24:32.701  1144  1941 I wm_stack_created: 1//创建stack 1
06-27 11:24:32.702  1144  1941 I wm_task_created: [59,1]//在stack 1中创建 59这个task
06-27 11:24:32.703  1144  1941 I wm_task_moved: [59,1,0]//在WindowContainer的child中添加task [taskId, onTop, position]
06-27 11:24:32.703  1144  1941 I am_focused_stack: [0,1,0,reuseOrNewTask]//setFocusStack，[user,new focus,last focus,reason]
06-27 11:24:32.703  1144  1941 I wm_task_moved: [59,1,0]
06-27 11:24:32.706  1144  1941 I am_create_task: [0,59]
06-27 11:24:32.706  1144  1941 I am_create_activity: [0,236342753,59,com.android.settings/.MainSettings,android.intent.action.MAIN,NULL,NULL,270532608]
06-27 11:24:32.707  1144  1941 I wm_task_moved: [59,1,0]
06-27 11:24:32.714  1144  1941 I am_pause_activity: [2526,14510954,com.miui.home/.launcher.Launcher]
06-27 11:24:32.727  2526  2526 I am_on_paused_called: [0,com.miui.home.launcher.Launcher,handlePauseActivity]
06-27 11:24:32.755 17883 17883 I auditd  : type=1400 audit(0.0:10573): avc: denied { create } for comm="main" name="cgroup.procs" scontext=u:r:zygote:s0 tcontext=u:object_r:cgroup:s0 tclass=file permissive=0
06-27 11:24:32.765  1144  1732 I am_proc_start:  [0,17883,1000,com.android.settings,activity,com.android.settings/.MainSettings]//启动进程 [uid,pid,father uid,processName]
06-27 11:24:32.791  1144  1941 I am_proc_bound: [0,17883,com.android.settings]//进程绑定到Application
06-27 11:24:32.818  1144  1941 I am_restart_activity: [0,236342753,59,com.android.settings/.MainSettings,17883]//realStartActivity [userId,hashCode,taskID,componentName]
06-27 11:24:32.818  1144  1941 I am_restart_activity_ai: [17883,com.android.settings/.MainSettings,false]
06-27 11:24:32.822  1144  1941 I am_set_resumed_activity: [0,com.android.settings/.MainSettings,minimalResumeActivityLocked]//resume [userid,componentName,reason]
06-27 11:24:32.863  1144  1469 I am_pss  : [17813,10021,com.android.mms,20518912,18341888,62464]//调用时机是进程优先级修改
06-27 11:24:33.042 17883 17883 I sysui_multi_action: [757,1,758,1,833,0]
06-27 11:24:33.062 17883 17883 I am_on_resume_called: [0,com.android.settings.MainSettings,LAUNCH_ACTIVITY]//日志调用实在onResume完成之后
06-27 11:24:33.193  1144  1477 I sysui_multi_action: [319,139,321,89,322,495,325,112839,757,761,758,7,759,1,806,com.android.settings,871,com.android.settings.MainSettings,904,com.miui.home,905,0,945,106]
06-27 11:24:33.194  1144  1477 I am_activity_launch_time: [0,236342753,com.android.settings/.MainSettings,439,439]//reportLaunchTime，[userId,hash,componentName]
06-27 11:24:33.205  1144  1732 I am_stop_activity: [0,14510954,com.miui.home/.launcher.Launcher]
06-27 11:24:33.217  2526  2526 I am_on_stop_called: [0,com.miui.home.launcher.Launcher,handleStopActivity]

1. stack里面没有最后一个task被移除的时候，会销毁task。当需要创建一个activity到还没的stack中的时候，会重新创建它。
2. 创建wm端的task结构，并将task加入到WindowContainer中。并将焦点从home stack转到fullscreen stack。
3. am端创建task和接收activity启动请求。
4. 之前的activity端进入pause状态。
5. 启动新的activity进程并绑定application。
6. 调用realStartActivityLocked真正启动应用，日志上是am_restart_activity。并进入resume流程。
7. 之前的activity进入stop。

### 应用内启动另一个Activity

06-27 11:25:56.293  1144  2034 I wm_task_moved: [59,1,0]
06-27 11:25:56.297  1144  2034 I am_create_activity: [0,115983833,59,com.android.settings/.SubSettings,android.intent.action.MAIN,NULL,NULL,0]
06-27 11:25:56.303  1144  2034 I am_pause_activity: [17883,236342753,com.android.settings/.MainSettings]
06-27 11:25:56.347 17883 17883 I sysui_view_visibility: [1,0]
06-27 11:25:56.347 17883 17883 I sysui_multi_action: [757,1,758,2]
06-27 11:25:56.349 17883 17883 I am_on_paused_called: [0,com.android.settings.MainSettings,handlePauseActivity]
06-27 11:25:56.368  1144  2034 I am_restart_activity: [0,115983833,59,com.android.settings/.SubSettings,17883]
06-27 11:25:56.368  1144  2034 I am_restart_activity_ai: [17883,com.android.settings/.SubSettings,false]
06-27 11:25:56.372  1144  2034 I am_set_resumed_activity: [0,com.android.settings/.SubSettings,minimalResumeActivityLocked]
06-27 11:25:56.384  1144  1470 I sysui_count: [window_time_0,268]
06-27 11:25:56.384  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,268]
06-27 11:25:56.405  1144  1477 I dvm_lock_sample: [system_server,0,android.display,23,WindowManagerService.java,5440,void com.android.server.wm.WindowManagerService$H.handleMessage(android.os.Message),WindowSurfacePlacer.java,113,void com.android.server.wm.WindowSurfacePlacer.lambda$-com_android_server_wm_WindowSurfacePlacer_5523(),4]
06-27 11:25:56.427  1144  1469 I am_pss  : [2526,10031,com.miui.home,91344896,76333056,9026560]
06-27 11:25:56.480 17883 17883 I sysui_multi_action: [757,90,758,1,833,0]
06-27 11:25:56.481 17883 17883 I am_on_resume_called: [0,com.android.settings.SubSettings,LAUNCH_ACTIVITY]
06-27 11:25:56.614  1144  1477 I dvm_lock_sample: [system_server,0,android.display,19,ActivityRecord.java,2078,void com.android.server.am.ActivityRecord.onWindowsDrawn(long),ActivityManagerService.java,7527,void com.android.server.am.ActivityManagerService.activityIdle(android.os.IBinder, android.content.res.Configuration, boolean),3]
06-27 11:25:56.614  1144  1477 I am_activity_launch_time: [0,115983833,com.android.settings/.SubSettings,228,228]
06-27 11:25:56.686  1144  1470 I am_stop_activity: [0,236342753,com.android.settings/.MainSettings]
06-27 11:25:56.690 17883 17883 I am_on_stop_called: [0,com.android.settings.MainSettings,handleStopActivity]

与桌面单击启动应用的差异就在于：不需要创建新的stack和task。


### back键退出Activity（退出后task不为空）

06-27 11:27:51.180  1144  1732 I sysui_count: [key_back_down,1]
06-27 11:27:51.181  1144  1732 I sysui_multi_action: [757,803,799,key_back_down,802,1]
06-27 11:27:51.230  1144  1732 I am_finish_activity: [17883,115983833,59,com.android.settings/.SubSettings,app-request]
06-27 11:27:51.235  1144  1732 I wm_task_moved: [59,1,0]
06-27 11:27:51.249  1144  1732 I am_pause_activity: [17883,115983833,com.android.settings/.SubSettings]
06-27 11:27:51.304  1144  1470 I sysui_count: [window_time_0,115]
06-27 11:27:51.304  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,115]
06-27 11:27:51.306 17883 17883 I sysui_view_visibility: [90,0]
06-27 11:27:51.306 17883 17883 I sysui_multi_action: [757,90,758,2]
06-27 11:27:51.311 17883 17883 I am_on_paused_called: [0,com.android.settings.SubSettings,handlePauseActivity]
06-27 11:27:51.315  1144  2034 I am_set_resumed_activity: [0,com.android.settings/.MainSettings,resumeTopActivityInnerLocked]
06-27 11:27:51.325  1144  2034 I am_resume_activity: [0,236342753,59,com.android.settings/.MainSettings,17883]
06-27 11:27:51.338  1144  1470 I sysui_count: [window_time_0,0]
06-27 11:27:51.338  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,0]
06-27 11:27:51.342 17883 17883 I sysui_multi_action: [757,1,758,1,833,0]
06-27 11:27:51.349 17883 17883 I am_on_resume_called: [0,com.android.settings.MainSettings,RESUME_ACTIVITY]
06-27 11:27:51.716  1144  1470 I am_destroy_activity: [17883,115983833,59,com.android.settings/.SubSettings,finish-imm]
06-27 11:27:51.730 17883 17883 I am_on_stop_called: [0,com.android.settings.SubSettings,destroy]

1. systemui会通知按键状态。
2. am先进行finish activity的请求，再将activity进入pause状态。
3. resume一个task中上一个activity。
4. am对前面的activity进行destroy的请求，并回调到onStop。


### back键退出Activity（退出后task不为空）

06-27 11:28:43.904  1144  2034 I sysui_count: [key_back_down,1]
06-27 11:28:43.904  1144  2034 I sysui_multi_action: [757,803,799,key_back_down,802,1]
06-27 11:28:43.967  1144  2034 I am_finish_activity: [17883,236342753,59,com.android.settings/.MainSettings,app-request]
06-27 11:28:43.971  1144  2034 I am_focused_stack: [0,0,1,finishActivity adjustFocus]
06-27 11:28:43.971  1144  2034 I wm_task_moved: [44,1,0]
06-27 11:28:43.984  1144  2034 I am_pause_activity: [17883,236342753,com.android.settings/.MainSettings]
06-27 11:28:44.034  1144  1470 I sysui_count: [window_time_0,53]
06-27 11:28:44.034  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,53]
06-27 11:28:44.037 17883 17883 I sysui_view_visibility: [1,0]
06-27 11:28:44.038 17883 17883 I sysui_multi_action: [757,1,758,2]
06-27 11:28:44.040 17883 17883 I am_on_paused_called: [0,com.android.settings.MainSettings,handlePauseActivity]
06-27 11:28:44.050  1144  1941 I am_set_resumed_activity: [0,com.miui.home/.launcher.Launcher,resumeTopActivityInnerLocked]
06-27 11:28:44.062  1144  1941 I am_uid_active: 10031
06-27 11:28:44.091  1144  1941 I am_resume_activity: [0,14510954,44,com.miui.home/.launcher.Launcher,2526]
06-27 11:28:44.120  1144  1470 I sysui_count: [window_time_0,0]
06-27 11:28:44.120  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,0]
06-27 11:28:44.130  2526  2526 I am_on_resume_called: [0,com.miui.home.launcher.Launcher,RESUME_ACTIVITY]
06-27 11:28:44.563  1144  1470 I am_destroy_activity: [17883,236342753,59,com.android.settings/.MainSettings,finish-imm]
06-27 11:28:44.577 17883 17883 I am_on_stop_called: [0,com.android.settings.MainSettings,destroy]
06-27 11:28:44.624  1144  1992 I wm_task_removed: [59,removeAppToken: last token]
06-27 11:28:44.625  1144  1992 I wm_task_removed: [59,removeTask]
06-27 11:28:44.632  1144  1992 I wm_stack_removed: 1
06-27 11:28:44.647  1144  1992 I dvm_lock_sample: [system_server,0,Binder:1144_B,14,WindowManagerService.java,2934,void com.android.server.wm.WindowManagerService.executeAppTransition(),WindowSurfacePlacer.java,113,void com.android.server.wm.WindowSurfacePlacer.lambda$-com_android_server_wm_WindowSurfacePlacer_5523(),2]

与退出后task不为空的情况差异是最后的task需要被remove，stack也要被remove。

## 分屏

分屏主要分析：
1. 拖动进入分屏
2. 退出分屏

### 拖动进入分屏

06-27 16:09:41.922  1646  1646 I sysui_count: [window_enter_incompatible,1]
06-27 16:09:41.923  1646  1646 I sysui_multi_action: [757,803,799,window_enter_incompatible,802,1]
06-27 16:09:44.138  1144  1671 I wm_stack_created: 3
06-27 16:09:44.135  1144  1144 I auditd  : type=1400 audit(0.0:11981): avc: denied { write } for comm="Binder:1144_4" name="logd" dev="tmpfs" ino=20617 scontext=u:r:system_server:s0 tcontext=u:object_r:logd_socket:s0 tclass=sock_file permissive=0
06-27 16:09:44.142  1144  1671 I [70001] : [com.android.mms,0,0]//MIUI在updateMultiWindowMode [pkgName,userId,mFullScreen]
06-27 16:09:44.146  1144  1671 I wm_task_removed: [63,reParentTask]
06-27 16:09:44.147  1144  1671 I wm_task_moved: [63,1,0]
06-27 16:09:44.154  1144  1671 I am_focused_stack: [0,3,5,startActivityFromRecents]
06-27 16:09:44.154  1144  1671 I wm_task_moved: [63,1,0]
06-27 16:09:44.155  1144  1144 I auditd  : type=1400 audit(0.0:11982): avc: denied { write } for comm="Binder:1144_4" name="logd" dev="tmpfs" ino=20617 scontext=u:r:system_server:s0 tcontext=u:object_r:logd_socket:s0 tclass=sock_file permissive=0
06-27 16:09:44.166  1144  1671 I [70001] : [com.android.settings,0,0]
06-27 16:09:44.173  1144  1671 I wm_task_moved: [63,1,0]
06-27 16:09:44.177  1144  1671 I wm_task_moved: [63,1,0]
06-27 16:09:44.182  1144  1671 I am_pause_activity: [1646,137585269,com.android.systemui/.recents.RecentsActivity]
06-27 16:09:44.186  1144  1671 I am_task_to_front: [0,63]
06-27 16:09:44.204  1646  1646 I sysui_action: [270,com.android.settings/.SubSettings]
06-27 16:09:44.204  1646  1646 I sysui_multi_action: [757,270,758,4,806,com.android.settings/.SubSettings]
06-27 16:09:44.226  1646  1646 I am_on_paused_called: [0,com.android.systemui.recents.RecentsActivity,handlePauseActivity]
06-27 16:09:44.230  1144  2459 I am_set_resumed_activity: [0,com.android.settings/.SubSettings,resumeTopActivityInnerLocked]
06-27 16:09:44.242  1144  2459 I am_relaunch_resume_activity: [19595,56402416,63,com.android.settings/.SubSettings]
06-27 16:09:44.260  1144  1470 I sysui_count: [window_time_0,25]
06-27 16:09:44.260  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,25]
06-27 16:09:44.260  1144  1471 I dvm_lock_sample: [system_server,1,android.ui,26,ActivityManagerService.java,4483,void com.android.server.am.ActivityManagerService.dispatchProcessesChanged(),-,7792,void com.android.server.am.ActivityManagerService.activityPaused(android.os.IBinder),5]
06-27 16:09:44.312  1144  1469 I am_pss  : [19780,10021,com.android.mms,45883392,41836544,52224]
06-27 16:09:44.343 19595 19595 I sysui_multi_action: [757,1,758,1,833,0]
06-27 16:09:44.351 19595 19595 I am_on_resume_called: [0,com.android.settings.SubSettings,handleRelaunchActivity]
06-27 16:09:44.492  1144  1477 I sysui_multi_action: [319,305,322,302,325,129950,757,761,758,9,759,2,806,com.android.settings,871,com.android.settings.SubSettings,904,com.android.settings,905,0]

1. systemui收到window_enter_incompatible的信号，某个应用需要进入分屏。
2. 创建dock stack，reparent 原有的task到dock stack上，表现为wm_task_remove,wm_task_moved两个步骤。在task_remove的时候会列出原因。
3. am把focused的stack从recents stack变成了dock stack。recent stack上的应用进入onPause，并将该应用移动到分屏的前台task。
4. am设置setting为需要resume的应用，并relaunch该activity，显示为am_relaunch_resume_activity信息，最后setting会进入resume。

需要特别说明的是，因为开启关闭分屏的方法是在recents中点击相应按钮，这种情况下的记录的日志。

### 退出分屏

06-27 17:20:37.112  1144  2648 I am_relaunch_resume_activity: [19595,56402416,63,com.android.settings/.SubSettings]
06-27 17:20:37.159  1144  2824 I dvm_lock_sample: [system_server,1,Binder:1144_1A,35,ActivityManagerService.java,10971,int com.android.server.am.ActivityManagerService.getActivityStackId(android.os.IBinder),-,11200,void com.android.server.am.ActivityManagerService.resizeDockedStack(android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect),7]
06-27 17:20:37.159 19595 19595 I sysui_view_visibility: [1,0]
06-27 17:20:37.159 19595 19595 I sysui_multi_action: [757,1,758,2]
06-27 17:20:37.161 19595 19595 I am_on_paused_called: [0,com.android.settings.SubSettings,handleRelaunchActivity]
06-27 17:20:37.161 19595 19595 I am_on_stop_called: [0,com.android.settings.SubSettings,destroy]
06-27 17:20:37.374 19595 19595 I sysui_multi_action: [757,1,758,1,833,0]
06-27 17:20:37.400 19595 19595 I am_on_resume_called: [0,com.android.settings.SubSettings,handleRelaunchActivity]
06-27 17:20:37.505  1144  1144 I auditd  : type=1400 audit(0.0:12006): avc: denied { write } for comm="Binder:1144_1D" name="logd" dev="tmpfs" ino=20617 scontext=u:r:system_server:s0 tcontext=u:object_r:logd_socket:s0 tclass=sock_file permissive=0
06-27 17:20:37.514  1144  2848 I [70001] : [com.android.settings,0,1]
06-27 17:20:37.535  1144  1144 I auditd  : type=1400 audit(0.0:12007): avc: denied { write } for comm="Binder:1144_1D" name="logd" dev="tmpfs" ino=20617 scontext=u:r:system_server:s0 tcontext=u:object_r:logd_socket:s0 tclass=sock_file permissive=0
06-27 17:20:37.540  1144  2848 I [70001] : [com.android.mms,0,1]
06-27 17:20:37.541  1144  2848 I [70001] : [com.android.systemui,0,1]
06-27 17:20:37.543  1144  2848 I wm_task_removed: [63,reParentTask]
06-27 17:20:37.543  1144  2848 I wm_task_moved: [63,1,1]
06-27 17:20:37.548  1144  2848 I wm_stack_removed: 3
06-27 17:20:37.550  1144  2848 I am_focused_stack: [0,1,3,moveTasksToFullscreenStack - onTop]
06-27 17:20:37.551  1144  2848 I wm_task_moved: [63,1,1]
06-27 17:20:37.636  1144  1470 I am_stop_activity: [0,137585269,com.android.systemui/.recents.RecentsActivity]
06-27 17:20:37.825  1144  1477 I dvm_lock_sample: [system_server,0,android.display,39,ActivityManagerService.java,3329,void com.android.server.am.ActivityManagerService.notifyActivityDrawn(android.os.IBinder),-,7527,void com.android.server.am.ActivityManagerService.activityIdle(android.os.IBinder, android.content.res.Configuration, boolean),7]
06-27 17:20:37.829  1646  1646 I sysui_view_visibility: [224,0]
06-27 17:20:37.829  1646  1646 I sysui_multi_action: [757,224,758,2]
06-27 17:20:37.831  1646  1646 I am_on_stop_called: [0,com.android.systemui.recents.RecentsActivity,handleStopActivity]

1. 进入分屏，首先会调用到dock stack中应用的relaunch工作，会进一步调用到onPause和onStop，然后进入onResume。
2. reparent task到fullscreen stack上，并移除dock stack。
3. 将focus的stack移动到fullscreen stack上。移动了之后am调用recent stack会进入stop。


## 转屏

转屏的情况还可以代表其他的特殊情况，如网络状态切换等。可以在app中设置是否对相应的configuration要进行relaunch。

06-27 17:07:41.671  1144  1471 I configuration_changed: 1152
06-27 17:07:41.691  1144  1471 I am_relaunch_resume_activity: [19595,56402416,63,com.android.settings/.SubSettings]
06-27 17:07:41.691 19595 19595 I sysui_view_visibility: [1,0]
06-27 17:07:41.692 19595 19595 I sysui_multi_action: [757,1,758,2]
06-27 17:07:41.729  1144  2824 I dvm_lock_sample: [system_server,1,Binder:1144_1A,46,ActivityManagerService.java,20392,int com.android.server.am.ActivityManagerService.broadcastIntent(android.app.IApplicationThread, android.content.Intent, java.lang.String, android.content.IIntentReceiver, int, java.lang.String, android.os.Bundle, java.lang.String[], int, android.os.Bundle, boolean, boolean, int),-,21062,boolean com.android.server.am.ActivityManagerService.updateDisplayOverrideConfiguration(android.content.res.Configuration, int),9]
06-27 17:07:41.731 19595 19595 I am_on_paused_called: [0,com.android.settings.SubSettings,handleRelaunchActivity]
06-27 17:07:41.733 19595 19595 I am_on_stop_called: [0,com.android.settings.SubSettings,destroy]
06-27 17:07:41.876  1144  1477 I dvm_lock_sample: [system_server,0,android.display,10,WindowManagerService.java,5440,void com.android.server.wm.WindowManagerService$H.handleMessage(android.os.Message),-,1960,int com.android.server.wm.WindowManagerService.relayoutWindow(com.android.server.wm.Session, android.view.IWindow, int, android.view.WindowManager$LayoutParams, int, int, int, int, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.util.MergedConfiguration, android.view.Surface),2]
06-27 17:07:41.919 19595 19595 I sysui_multi_action: [757,1,758,1,833,0]
06-27 17:07:41.925 19595 19595 I am_on_resume_called: [0,com.android.settings.SubSettings,handleRelaunchActivity]

1. orientation方向改变了，configuration_changed之后进行relaunch_resume的工作。
2. setting直接进入on_pause和on_stop。最后再进入on_resume。其实相当于重置了一遍可见性相关的内容。

## 亮/灭屏

灭屏的操作也会对应用的生命周末有改变。

以下分析：
1. 应用灭屏
2. 应用亮屏
3. 锁屏期间启动了应用

### 应用灭屏

06-27 17:50:04.356  1144  1538 I sysui_view_visibility: [127,100]
06-27 17:50:04.357  1144  1538 I sysui_multi_action: [757,127,758,1]
06-27 17:50:04.357  1144  1538 I sysui_histogram: [note_load,0]
06-27 17:50:04.357  1144  1538 I sysui_multi_action: [757,804,799,note_load,801,0,802,1]
06-27 17:50:04.357  1144  1538 I notification_panel_revealed: 0
06-27 17:50:04.516  1144  1538 I power_sleep_requested: 0
06-27 17:50:04.515   857   857 I auditd  : type=1400 audit(0.0:13358): avc: denied { read } for comm="Binder:857_1" name="cmdline" dev="proc" ino=28175 scontext=u:r:audioserver:s0 tcontext=u:r:system_app:s0 tclass=file permissive=0
06-27 17:50:04.695   857   857 I auditd  : type=1400 audit(0.0:13359): avc: denied { read } for comm="Binder:857_1" name="cmdline" dev="proc" ino=28175 scontext=u:r:audioserver:s0 tcontext=u:r:system_app:s0 tclass=file permissive=0
06-27 17:50:04.812  1144  1480 I am_pause_activity: [21272,118020323,com.android.mms/.ui.MmsTabActivity]
06-27 17:50:04.813 21272 21272 I am_on_paused_called: [0,com.android.mms.ui.MmsTabActivity,handlePauseActivity]
06-27 17:50:05.129  1144  1470 I dvm_lock_sample: [system_server,0,ActivityManager,317,ActivityStackSupervisor.java,4288,void com.android.server.am.ActivityStackSupervisor.handleDisplayChanged(int),ActivityManagerService.java,15484,android.app.ActivityManagerInternal$SleepToken com.android.server.am.ActivityManagerService.acquireSleepToken(java.lang.String, int),63]
06-27 17:50:05.130  1144  1987 I dvm_lock_sample: [system_server,1,Binder:1144_A,317,ActivityManagerService.java,7792,void com.android.server.am.ActivityManagerService.activityPaused(android.os.IBinder),-,15484,android.app.ActivityManagerInternal$SleepToken com.android.server.am.ActivityManagerService.acquireSleepToken(java.lang.String, int),63]
06-27 17:50:05.154 21272 21272 I binder_sample: [android.app.IActivityManager,13,341,com.android.mms,68]
06-27 17:50:05.157  1144  1471 I dvm_lock_sample: [system_server,0,android.ui,30,ActivityManagerService.java,4541,void com.android.server.am.ActivityManagerService.dispatchUidsChanged(),-,15484,android.app.ActivityManagerInternal$SleepToken com.android.server.am.ActivityManagerService.acquireSleepToken(java.lang.String, int),6]
06-27 17:50:05.165  1144  1470 I wm_task_moved: [66,1,0]
06-27 17:50:05.187  1144  1470 I am_stop_activity: [0,118020323,com.android.mms/.ui.MmsTabActivity]
06-27 17:50:05.193 21272 21272 I am_on_stop_called: [0,com.android.mms.ui.MmsTabActivity,sleeping]
06-27 17:50:05.224  1144  1475 I sysui_view_visibility: [223,100]
06-27 17:50:05.224  1144  1475 I sysui_multi_action: [757,223,758,1]
06-27 17:50:05.237  1144  1475 I sysui_view_visibility: [223,0]
06-27 17:50:05.237  1144  1475 I sysui_multi_action: [757,223,758,2]
06-27 17:50:05.237  1144  1475 I sysui_histogram: [dozing_minutes,0]
06-27 17:50:05.237  1144  1475 I sysui_multi_action: [757,804,799,dozing_minutes,801,0,802,1]
06-27 17:50:05.247  1144  1144 I sysui_multi_action: [757,198,758,2,759,2]
06-27 17:50:05.247  1144  1144 I power_screen_state: [0,2,0,0,0]
06-27 17:50:05.247  1144  1144 I screen_toggled: 0
06-27 17:50:05.247  1144  1144 I sysui_histogram: [screen_timeout,30]
06-27 17:50:05.247  1144  1144 I sysui_multi_action: [757,804,799,screen_timeout,801,30,802,1]
06-27 17:50:05.248  1144  1144 I power_screen_broadcast_send: 1
06-27 17:50:05.301  1646  1646 I sysui_multi_action: [757,196,758,1,759,0]
06-27 17:50:05.301  1646  1646 I sysui_status_bar_state: [1,1,0,0,0,1]

1. power_sleep_requested 标记为0。
2. activity进入pause，再进入stop。

此时的最顶端的isVisible还是true的，但是会被一直可见的statusbar给遮挡住，在am中的属性isSleeping也是true的。

### 应用亮屏

分为两个部分，解锁锁屏之前，解锁锁屏之后。

#### 解锁锁屏前

06-27 17:59:03.165  1144  1538 I sysui_view_visibility: [127,100]
06-27 17:59:03.165  1144  1538 I sysui_multi_action: [757,127,758,1]
06-27 17:59:03.165  1144  1538 I sysui_histogram: [note_load,0]
06-27 17:59:03.165  1144  1538 I sysui_multi_action: [757,804,799,note_load,801,0,802,1]
06-27 17:59:03.165  1144  1538 I notification_panel_revealed: 0
06-27 17:59:03.172  1144  1144 I screen_toggled: 1
06-27 17:59:03.172  1144  1144 I power_screen_broadcast_send: 1
06-27 17:59:03.173  1144  2730 I sysui_view_visibility: [127,100]
06-27 17:59:03.174  1144  2730 I sysui_multi_action: [757,127,758,1]
06-27 17:59:03.174  1144  2730 I sysui_histogram: [note_load,18]
06-27 17:59:03.174  1144  2730 I sysui_multi_action: [757,804,799,note_load,801,18,802,1]
06-27 17:59:03.174  1144  2730 I notification_panel_revealed: 18
06-27 17:59:03.343  1144  1480 I sysui_multi_action: [757,198,758,1,759,0,793,182]
06-27 17:59:03.343  1144  1480 I power_screen_state: [1,0,0,0,182]
06-27 17:59:03.393  1144  1144 I power_screen_broadcast_done: [1,221,1]

主要涉及systemui的可见性等变化，不是本文关注的重点。但需要注意的是 power_screen_broadcast_send 消息会发出1表示亮屏。

#### 解锁锁屏后

06-27 17:59:15.614  1646  1646 I sysui_multi_action: [757,186,758,4,826,308,827,3292]
06-27 17:59:15.615  1646  1646 I sysui_lockscreen_gesture: [1,308,3292]
06-27 17:59:15.726  1144  2470 I sysui_view_visibility: [127,0]
06-27 17:59:15.726  1144  2470 I sysui_multi_action: [757,127,758,2]
06-27 17:59:15.726  1144  2470 I notification_panel_hidden: 
06-27 17:59:15.738  1144  2730 I am_set_resumed_activity: [0,com.android.mms/.ui.MmsTabActivity,resumeTopActivityInnerLocked]
06-27 17:59:15.751  1144  2730 I am_resume_activity: [0,118020323,66,com.android.mms/.ui.MmsTabActivity,21272]
06-27 17:59:15.781 21272 21272 I am_on_resume_called: [0,com.android.mms.ui.MmsTabActivity,RESUME_ACTIVITY]
06-27 17:59:15.798  1646  1646 I sysui_multi_action: [757,196,758,2,759,0]
06-27 17:59:15.798  1646  1646 I sysui_status_bar_state: [0,0,0,0,0,1]
06-27 17:59:15.812  1144  1477 I dvm_lock_sample: [system_server,0,android.display,23,WindowManagerService.java,5440,void com.android.server.wm.WindowManagerService$H.handleMessage(android.os.Message),-,1960,int com.android.server.wm.WindowManagerService.relayoutWindow(com.android.server.wm.Session, android.view.IWindow, int, android.view.WindowManager$LayoutParams, int, int, int, int, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.util.MergedConfiguration, android.view.Surface),4]
06-27 17:59:15.825   857   857 I auditd  : type=1400 audit(0.0:13366): avc: denied { read } for comm="Binder:857_1" name="cmdline" dev="proc" ino=28175 scontext=u:r:audioserver:s0 tcontext=u:r:system_app:s0 tclass=file permissive=0
06-27 17:59:15.844  1144  1144 I notification_enqueue: [10021,21272,com.android.mms,114,NULL,0,Notification(channel=Mms pri=0 contentView=null vibrate=null sound=null defaults=0x0 flags=0x40 color=0xff607d8b vis=PRIVATE),0]
06-27 17:59:15.853  1144  1732 I am_wtf  : [0,2587,com.miui.powerkeeper,952647237,NotificationColorUtil,background can not be translucent: #0]
06-27 17:59:15.855  1144  1144 I notification_cancel_all: [10021,21272,com.android.mms,0,0,64,9,NULL]
06-27 17:59:15.862  1144  2647 I am_wtf  : [0,2366,com.xiaomi.xmsf,952647181,NotificationColorUtil,background can not be translucent: #0]
06-27 17:59:15.884  1144  2470 I am_uid_active: 10001
06-27 17:59:15.902  1144  2470 I dvm_lock_sample: [system_server,1,Binder:1144_12,6,WindowManagerService.java,1960,int com.android.server.wm.WindowManagerService.relayoutWindow(com.android.server.wm.Session, android.view.IWindow, int, android.view.WindowManager$LayoutParams, int, int, int, int, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.util.MergedConfiguration, android.view.Surface),WindowSurfacePlacer.java,113,void com.android.server.wm.WindowSurfacePlacer.lambda$-com_android_server_wm_WindowSurfacePlacer_5523(),1]
06-27 17:59:15.951  1144  1732 I dvm_lock_sample: [system_server,1,Binder:1144_5,5,ActivityManagerService.java,25073,int com.android.server.am.ActivityManagerService.getLastResumedActivityUserId(),BroadcastQueue.java,855,void com.android.server.am.BroadcastQueue.processNextBroadcast(boolean, boolean),1]
06-27 17:59:15.995  1144  1144 I sysui_multi_action: [757,128,758,5,759,8,793,153,794,0,795,153,796,114,806,com.android.mms,857,Mms,858,4,947,0]

1. am进行set_resume表明mms这个应用需要进入resume状态。

这个阶段，其实主要就是保证顶端的应用会进入resume状态成为可见的。

#### 锁屏时，应用启动

这种情况可能在某些场景会出现，当前是用的am指令模拟的状态。

06-27 18:08:58.075 21756 21756 I auditd  : type=1400 audit(0.0:13387): avc: denied { create } for comm="main" name="cgroup.procs" scontext=u:r:zygote:s0 tcontext=u:object_r:cgroup:s0 tclaass=file permissive=0
06-27 18:08:58.085  1144  1470 I am_proc_start: [0,21756,1000,com.qualcomm.qti.callenhancement,service,com.qualcomm.qti.callenhancement/.CallEnhancementService]
06-27 18:08:58.117  1144  2824 I am_proc_bound: [0,21756,com.qualcomm.qti.callenhancement]
06-27 18:08:58.175  1144  1469 I am_pss  : [21625,1000,com.qti.csm,4843520,3465216,101376]
06-27 18:08:58.536  1144  2730 I wm_stack_created: 1
06-27 18:08:58.538  1144  2730 I wm_task_created: [67,1]
06-27 18:08:58.540  1144  2730 I wm_task_moved: [67,1,0]
06-27 18:08:58.540  1144  2730 I am_focused_stack: [0,1,0,reuseOrNewTask]
06-27 18:08:58.540  1144  2730 I wm_task_moved: [67,1,0]
06-27 18:08:58.544  1144  2730 I am_create_task: [0,67]
06-27 18:08:58.544  1144  2730 I am_create_activity: [0,113459077,67,com.android.mms/.ui.MmsTabActivity,NULL,NULL,NULL,268435456]
06-27 18:08:58.545  1144  2730 I wm_task_moved: [67,1,0]
06-27 18:08:58.552  1144  2730 I am_uid_running: 10021
06-27 18:08:58.561  1144  2730 I am_proc_start: [0,21780,10021,com.android.mms,activity,com.android.mms/.ui.MmsTabActivity]
06-27 18:08:58.555 21780 21780 I auditd  : type=1400 audit(0.0:13389): avc: denied { create } for comm="main" name="cgroup.procs" scontext=u:r:zygote:s0 tcontext=u:object_r:cgroup:s0 tclass=file permissive=0
06-27 18:08:58.574  1144  2730 I am_proc_bound: [0,21780,com.android.mms]
06-27 18:08:58.597  1144  2730 I am_uid_active: 10021
06-27 18:08:58.598  1144  2730 I am_restart_activity: [0,113459077,67,com.android.mms/.ui.MmsTabActivity,21780]
06-27 18:08:58.598  1144  2730 I am_restart_activity_ai: [21780,com.android.mms/.ui.MmsTabActivity,false]
06-27 18:08:58.599  1144  2730 I am_set_resumed_activity: [0,com.android.mms/.ui.MmsTabActivity,minimalResumeActivityLocked]
06-27 18:08:58.604  1144  2730 I am_pause_activity: [21780,113459077,com.android.mms/.ui.MmsTabActivity]
06-27 18:08:58.782  1144  2730 I am_uid_running: 10093
06-27 18:08:58.785 21811 21811 I auditd  : type=1400 audit(0.0:13391): avc: denied { create } for comm="main" name="cgroup.procs" scontext=u:r:zygote:s0 tcontext=u:object_r:cgroup:s0 tclass=file permissive=0
06-27 18:08:58.795 21780 21780 I auditd  : type=1400 audit(0.0:13393): avc: denied { getattr } for comm="Binder:intercep" path="/data/data/com.miui.contentcatcher" dev="mmcblk0p82" ino=1589286 scontext=u:r:priv_app:s0:c512,c768 tcontext=u:object_r:system_app_data_file:s0 tclass=dir permissive=0
06-27 18:08:58.815  1144  1941 I am_proc_bound: [0,21811,com.xiaomi.simactivate.service]
06-27 18:08:58.820  1144  1941 I am_uid_active: 10093
06-27 18:08:58.831 21780 21780 I am_on_resume_called: [0,com.android.mms.ui.MmsTabActivity,LAUNCH_ACTIVITY]
06-27 18:08:58.871 21780 21780 I am_on_paused_called: [0,com.android.mms.ui.MmsTabActivity,handlePauseActivity]
06-27 18:08:58.877  1144  1470 I wm_task_moved: [67,1,0]
06-27 18:08:58.890  1144  1470 I am_stop_activity: [0,113459077,com.android.mms/.ui.MmsTabActivity]
06-27 18:08:58.896  1144  1470 I sysui_count: [window_time_0,22]
06-27 18:08:58.896  1144  1470 I sysui_multi_action: [757,803,799,window_time_0,802,22]
06-27 18:08:58.914  1144  1144 I notification_enqueue: [10021,21780,com.android.mms,114,NULL,0,Notification(channel=Mms pri=0 contentView=null vibrate=null sound=null defaults=0x0 flags=0x40 color=0xff607d8b vis=PRIVATE),0]
06-27 18:08:58.929  1144  2730 I am_wtf  : [0,2587,com.miui.powerkeeper,952647237,NotificationColorUtil,background can not be translucent: #0]
06-27 18:08:58.929  1144  2648 I am_wtf  : [0,2366,com.xiaomi.xmsf,952647181,NotificationColorUtil,background can not be translucent: #0]
06-27 18:08:58.945  1144  1144 I sysui_multi_action: [757,128,758,5,759,8,793,33,794,0,795,33,796,114,806,com.android.mms,857,Mms,858,4,947,0]
06-27 18:08:58.945  1144  1144 I notification_canceled: [0|com.android.mms|114|null|10021,8,33,33,0,NULL]
06-27 18:08:58.961 21780 21780 I am_on_stop_called: [0,com.android.mms.ui.MmsTabActivity,handleStopActivity]
06-27 18:08:58.973  1144  1144 I notification_cancel_all: [10021,21780,com.android.mms,0,0,64,9,NULL]
06-27 18:08:59.185  1144  2848 I am_uid_active: 10001
06-27 18:09:01.385  1144  1144 I notification_enqueue: [10021,21780,com.android.mms,114,NULL,0,Notification(channel=Mms pri=0 contentView=null vibrate=null sound=null defaults=0x0 flags=0x40 color=0xff607d8b vis=PRIVATE),0]
06-27 18:09:01.408  1144  2730 I am_wtf  : [0,2587,com.miui.powerkeeper,952647237,NotificationColorUtil,background can not be translucent: #0]
06-27 18:09:01.410  1144  2460 I am_wtf  : [0,2366,com.xiaomi.xmsf,952647181,NotificationColorUtil,background can not be translucent: #0]
06-27 18:09:01.440  1144  1144 I sysui_multi_action: [757,128,758,5,759,8,793,58,794,0,795,58,796,114,806,com.android.mms,857,Mms,858,4,947,0]


1. 首先也是创建Stack task，创建进程等工作，和直接启动应用差异不大。
2. 在应用进程绑定后，也会restart应用。但是此处会紧接着调用resume和pause，然后最后在调用stop。




