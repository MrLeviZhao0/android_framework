# android mk系统 #

项目过程中遇到了一些问题：

新增了一个APK到目录 device/rockchip/RoyoleX2/royole/apks/PrivacyLoading/下，直接在目录下mm编译，会将目标生成到 out/out/target/product/Royole_X002_box/system/priv-app/PrivacyLoading。

直接将apk push到系统中，是没有问题的。但当系统被编译的时候，发现apk根本就没有被编译，导致出错。所以接下来分析一下Android的mk系统。

## 概述 ##

Build 系统中的 Make 文件可以分为三类：
* 第一类是 Build 系统核心文件  
  此类文件定义了整个 Build 系统的框架，而其他所有 Make 文件都是在这个框架的基础上编写出来的。
* 第二类是针对某个产品（一个产品可能是某个型号的手机或者平板电脑）的 Make 文件  
  这些文件通常位于 device 目录下，该目录下又以公司名以及产品名分为两级目录，图 2 是 device 目录下子目录的结构。对于一个产品的定义通常需要一组文件，这些文件共同构成了对于这个产品的定义。例如，/device/target/it26 目录下的文件共同构成了对于 Sony LT26 型号手机的定义
* 第三类是针对某个模块（关于模块后文会详细讨论）的 Make 文件  
  整个系统中，包含了大量的模块，每个模块都有一个专门的 Make 文件，这类文件的名称统一为“Android.mk”，该文件中定义了如何编译当前模块。Build 系统会在整个源码树中扫描名称为“Android.mk”的文件并根据其中的内容执行模块的编译。

执行源码编译的过程，一般就是三步：
1. source build/envsetup.sh 
2. lunch 
3. make 

第一步完成部分变量设置，增加一些如cgrep、make之类的函数。  
第二步完成需要的模式的环境变量设置。
第三步开始执行编译。

而整个编译系统的起点是make指令。会相应的找到根目录下的Makefile，而Makefile里面其实也只写了引用build/core/main.mk的脚本。通过这种不断的引用可以使得整个系统得到编译。

[详细的过程可以参考大佬的博客](http://blog.csdn.net/huangyabin001/article/details/36383031)

我们主要关注的目标是在新增apk的时候需要修改的模块。

首先是，有三种方式可以增加自定义的apk。
1. 在vendor/target/app/ 下添加APK
2. 在device/corp/product/apk/ 下添加APK
3. 在packages/apps/ 下添加APK

第三种方式非常不建议采用，一般来说只有原生的应用才会放在这个目录下。
第一种是使用的覆盖机制，把新增的apk覆盖到原有的apk。但现在不建议这么做。
第二种是推荐的做法，可以列出公司名，产品名，然后修改相应产品的配置。包括要添加的应用。

因为项目中选择的是第二种做法，所以我们重点关注一下，product是怎么编译的。


通常，对于一个产品的定义通常至少会包括四个文件：AndroidProducts.mk，产品版本定义文件，BoardConfig.mk 以及 verndorsetup.sh。下面我们来详细说明这几个文件。
* AndroidProducts.mk：
  该文文件中的内容很简单，其中只需要定义一个变量，名称为“PRODUCT_MAKEFILES”，该变量的值为产品版本定义文件名的列表。
* 产品版本定义文件：
  顾名思义，该文件中包含了对于特定产品版本的定义。该文件可能不只一个，因为同一个产品可能会有多种版本（例如，面向中国地区一个版本，面向美国地区一个版本）。
* BoardConfig.mk：
  该文件用来配置硬件主板，它其中定义的都是设备底层的硬件特性。例如：该设备的主板相关信息，Wifi 相关信息，还有 bootloader，内核，radioimage 等信息。
* vendorsetup.sh：
  该文件中作用是通过 add_lunch_combo 函数在 lunch 函数中添加一个菜单选项。该函数的参数是产品名称加上编译类型，中间以“-”连接。

可以看到实际项目中的

```
AndroidProducts.mk
PRODUCT_MAKEFILES := \
        $(LOCAL_DIR)/product.mk \
        $(LOCAL_DIR)/product_us.mk
```

接着找到
```
product.mk

ifeq ($(BOARD_WIFI_VENDOR), realtek)
PRODUCT_PACKAGES += rtw_fwloader
PRODUCT_COPY_FILES += \
    hardware/realtek/wlan/config/wpa_supplicant.conf:system/etc/wifi/wpa_supplicant.conf \
    hardware/realtek/wlan/config/p2p_supplicant.conf:system/etc/wifi/p2p_supplicant.conf
endif

```

可以发现 `PRODUCT_PACKAGES +=` 是当前版本输出的情况下需要打包的app名。  
而`PRODUCT_COPY_FILES +=` 是需要拷贝到目标版本需要的一些文件及库。

所以最终我们发现了，需要使用第二种方式增加APK的时候，还需要向产品的product.mk添加相应的APK。需要注意的是，AndroidProducts.mk的名字一般不会变，而product.mk的名字可以有变式，需要从脚本中寻找。
