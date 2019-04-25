Binary Refactor
---
![screenshot](screenshot.png)

#### Description

Helper to manual de-obfuscate obfuscated jars,some features:
* rename class/packages in a jar
* match a jarjar-ed & obfuscated jar with a known jar,to find the 'same' classes
* bytecode dump(asm)
* class dependency graph
* all by binary!

#### Have a try
* git clone git://github.com/argan/binary-refactor.git
* cd binary-refactor
* mvn jetty:run
* open your browser http://localhost:9090/jarviewer/list.htm
* Have fun.

#### 简单描述
有时候，要研究一些java相关的东西，但是没有源代码，只有二进制的jar，同时这个jar呢，又被混淆过了，反编译很困难，为了研究需要，因此写了这么一个小东西，没几行代码，实现的功能有：

* 提供一个简单的web操作界面，当然也可以用命令行和参数来做
* 浏览jar结构，查看类的结构，修改class/method/field的名字
* 查看类的bytecode dump，反编译结果，依赖和被依赖的情况
* 查看类的依赖关系
* 扫描多个jar，匹配相同的类，用来寻找repackage过的内容

#### 目标
* 居家旅行、杀人越货的必备武器

