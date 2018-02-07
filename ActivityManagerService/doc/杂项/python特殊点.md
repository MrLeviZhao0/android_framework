## 数据结构 ##
1. Number(数字)：可以通过int()等函数强转。
2. String(字符串)：相当于字符元组。
3. List(列表)：相当于list数据结构，可写。
4. Tuple(元组)：与list类似，只读。
5. Dictionary(字典)：与map类型，只读。

## 运算符 ##
1. 算数运算符：多了 ** 幂运算，以及//取整运算。
2. 关系运算符：与JAVA一致
3. 赋值运算符：与JAVA一致
4. 位运算符:与JAVA一致
5. 逻辑运算符:分别为and/or/not
6. 成员运算符：in 在序列中查找是否存在
7. 身份运算符：is 

特别提出一个概念，python可以获得的数据结构都是类。  
对象有三个基本要素，id,type,value。  

### "==" 是比较操作符，用于判断value是否相同 ###
比如：
```
a = 'test code'
a = 'test code'
a==b

True
```

### "is" 是同一性运算符，用于判断id是否相同 ###
```
x=y=[4,5,6]
z=[4,5,6]
x is y

True

x is z
False
```
### "type" ，用于判断type是否相同 ###

### "isinstance"，用于判断type是否是子类 ###

```
class A:
    pass
class B(A):
    pass
isinstance(A(), A)  # returns True
type(A()) == A      # returns True
isinstance(B(), A)    # returns True
type(B()) == A        # returns False

```

## 流程控制 ##
1. 判断语句

```
if True :
   do something
elif False
   do something
else
   do something
```

2. 循环语句

```
while True:
   do something
````

```
for i in [1,2,3,4,5]:
   do something
```

控制指令还有break(直接退出循环),continue(直接进入下一个循环),pass（什么事都不做）

range(0,50,2)可以用于生成列表。以上语句则生成间隔为2，由0起到50的列表。


## Lambda表达式 ##

`lambda x,y:x+y` 可以等价于`def f(x,y): return x+y`

当Lambda表达式结合map(),filter(),reduce()之后，功能会更加强大

* map()：遍历序列中元素操作。
* filter()：从老的序列中选择出新的序列操作。
* reduce()：循环遍历序列中元素操作。

[不同之处可以参考](http://www.cnblogs.com/guigujun/p/6134828.html)

## Linux命令操作相关 ##

1. sys.argv[0]是文件本身函数名，sys.argv[1]及之后的都是输入参数。len(sys.argv[0])可以判断输入参数个数。

2. `os.path.abspath('..')` 相当于访问当前路径 "../"的路径
3. `os.popen` 执行命令，并返回结果。`output.read()`可以读取文本内容。
4. `os.system` 执行命令，返回命令码(exit(0)则返回值是0)。
5. 更复杂的可以使用子进程操作subprocess相关，来进行处理。

