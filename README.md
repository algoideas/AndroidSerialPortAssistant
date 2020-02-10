## Android Serial Port Assistant


Android Serial Port Assistant (Android 串口助手)用于Android串口通信，工具不需要root，同时不需要NDK或者其他内核驱动，仅需采用 Android USB Host API, 工具现支持API Level 14+.

项目基于github上以下项目，并在AndroidSerialPort基础上修改串口驱动部分，在此感谢以下项目作者。

<br>

1.[Android Serial Port](https://github.com/Deemonser/AndroidSerialPort) 
    https://github.com/Deemonser/AndroidSerialPort

2.[Usb Serial for Android](https://github.com/mik3y/usb-serial-for-android)
    https://github.com/mik3y/usb-serial-for-android




Android Serial Port Assistant 支持设置波特率、校验位、数据位、停止位.

串口通信简介

串行接口是一种可以将接受来自CPU的并行数据字符转换为连续的串行数据流发送出去，同时可将接受的串行数据流转换为并行的数据字符供给CPU的器件。一般完成这种功能的电路，我们称为串行接口电路。
串口通信（Serial Communications）是指外设和计算机间，通过数据信号线 、地线、控制线等，按位进行传输数据的一种通讯方式。
串口通信是计算机中非常常见的通信方式，比如一些有线鼠标、键盘、打印机等都是通过串口进行通信的。
串口的通信一般使用3根线完成，分别是地线、发送线（tx）、接收线（rx）。

串口的参数

串口中有五个重要的参数：串口设备名、波特率、奇偶校验位、数据位、停止位。
设备名称：串口的名称。
波特率：传输速率的参数，波特率和传输距离成反比。
校验位：在串口通信中一种简单的检错方式，有四种检错方式：偶、奇、高和低，允许无校验位。
数据位：通信中实际数据位的参数
停止位：用于表示单个包的最后一位。
其中检验位一般默认位NONE，数据位一般默认为8，停止位默认为1，校验位是为了减少误差的会根据奇、偶进行补位操作。
对于两个进行通信的端口，这些参数必须匹配，否则两端不能正常收发。

<br>

### APK下载

[下载apk]
项目apk目录下下载最新编译的APK.

<br>


### API

#### 打开串口

```java

mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
UsbDeviceConnection connection = mUsbManager.openDevice(mUsbSerialPort.getDriver().getDevice());
mUsbSerialPort.open(connection);

```
<br>

如果你需要设置更多参数，请使用以下构造setParameters函数

```java
/**
     * Sets various serial port parameters.
     *
     * @param baudRate baud rate as an integer, for example {@code 115200}.
     * @param dataBits one of {@link #DATABITS_5}, {@link #DATABITS_6},
     *            {@link #DATABITS_7}, or {@link #DATABITS_8}.
     * @param stopBits one of {@link #STOPBITS_1}, {@link #STOPBITS_1_5}, or
     *            {@link #STOPBITS_2}.
     * @param parity one of {@link #PARITY_NONE}, {@link #PARITY_ODD},
     *            {@link #PARITY_EVEN}, {@link #PARITY_MARK}, or
     *            {@link #PARITY_SPACE}.
     * @throws IOException on error setting the port parameters
     */
    public void setParameters(
            int baudRate, int dataBits, int stopBits, int parity) throws IOException;
```

检验位一般默认是0（NONE），数据位一般默认为8，停止位默认为1。

<br>

#### 读写串口

##### 读数据

```java
// 配合 Rxjava2 ，方便处理异常
mReceiveDisposable = Flowable.create((FlowableOnSubscribe<byte[]>) emitter -> {
    if (mUsbSerialPort != null) {
        int len = 0;
        while (!isInterrupted
            && mUsbSerialPort != null) {
            len = mUsbSerialPort.read(mReadBuffer.array(), READ_WAIT_MILLIS);
            if (len > 0) {
                byte[] bytes = new byte[len];
                mReadBuffer.get(bytes, 0, len);
                emitter.onNext(bytes);
                mReadBuffer.clear();
            }
        }
    }
}, BackpressureStrategy.MISSING)
```


##### 写数据

```java
mUsbSerialPort.write(isHexSend ? ByteUtils.hexStringToBytes(s)
            : ByteUtils.stringToAsciiBytes(s), READ_WAIT_MILLIS));
```

<br>

#### 关闭串口

```java
 mUsbSerialPort.close();
```

