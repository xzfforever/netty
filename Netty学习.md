##Netty

<li>I/O模型
<pre>
1、阻塞I/O
   没有数据时，阻塞，直到数据准备好或发生错误
2、非阻塞I/O
   没有数据时，直接返回，需要不断去轮询检查是否有数据
3、I/O复用模型
   Linux提供select/poll/epoll(信号中的“时分复用”)，单线程
4、信号驱动I/O模型
5、异步I/O模型
   告知内核启动某个操作，并让内核在整个操作完成后通知我们。该模型与信号驱动模型的主要区别是：信号I/O由内核通知我们何时可以开始一个I/O操作；异步I/O模型是由内核通知我们I/O操作何时已经完成
</pre>
<hr/>
<pre>
NIO库在JDK1.4中引入
与Socket类和ServerSocket类相对应，NIO也提供了SocketChannel和ServerSocketChannel两种不同的套接字通道实现，两种新增的通道都支持阻塞和非阻塞两种模式。
面向流的I/O中，将数据直接写入或将数据直接读取到Stream对象中。
NIO库中，所有数据都是用缓冲区处理的。

缓冲区实质上是一个数组，缓冲区还提供了对数据的结构化访问以及维护读写位置等信息。

Channel是一个通道，Channel和Stream的不同之处在于Channel是双向的，Stream只是一个方向上移动（一个Stream必须是InputStream或者OutputStream的子类），而Channel可以用于读、写或两者同时进行。
Channel主要可以分为两大类：用于网络读写的SelectableChannel和用于文件操作的FileChannel。
NI/O中的多路复用器Selector，它是Java NIO编程的基础，熟练地掌握Selector对于NIO编程至关重要。Selector会不断地轮询注册在其上的Channel，如果某个Channel上发生读或写事件，这个Channel就处于就绪状态，会被Selector轮询出来，通过SelectionKey可以获取就绪的Channel集合。
JDK1.7升级了NIO类库，NIO2.0,该版本正式提供了异步文件的I/O操作，以及与UNIX网络编程事件驱动I/O对应的AIO;
NIO2.0引入了新的异步通道的概念，提供了异步文件通道 和 异步套接字通道的实现。
异步通道提供以下两种方式获取操作结果：
1、通过java.util.concurrent.Future类来表示异步操作的结果
2、在执行异步操作的时候传入一个java.nio.channels









</pre>






<li>参考资料
>https://www.zhihu.com/question/32163005【I/O复用讲的很好的文章】
