Author:lezh 赵越

解压 GetLogTool 文件夹到指定目录，点击 getLogs.bat，会生成out_logs文件夹，其中有各种log信息。
期间不需要进行任何操作。

特别说明：
.\out_logs\bugreport\index.html 中有对bugreport分解后的信息。

如果执行失败，有如下错误可能性：
		1. get root permission error!  错误信息：没有连接ADB或者ADB获取root权限失败。
		2. remount error!  错误信息：重新挂载失败。
		3. push error!  错误信息：传输sh脚本到芯片失败。
		4. sh file excute error!  错误信息：sh脚本执行过程中出错。
		5. pull error!  错误信息：拉取日志失败。
		6. prase bugreport error!  错误信息：解析bugreport失败。	

如果发现工具有什么问题或有什么改进意见，可以联系赵越。
