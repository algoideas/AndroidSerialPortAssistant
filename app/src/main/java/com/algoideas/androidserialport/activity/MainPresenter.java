package com.algoideas.androidserialport.activity;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.algoideas.androidserialport.R;
import com.algoideas.androidserialport.bean.LeftDetailBean;
import com.algoideas.androidserialport.bean.LeftHeadBean;
import com.algoideas.androidserialport.bean.MessageBean;
import com.algoideas.androidserialport.util.ByteUtils;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;

/**
 * author： algoideas
 * date:    2019/03/30
 * desc:
 */
public class MainPresenter implements MainContract.IPresenter {

    private final String mDateFormat = "HH:mm:ss:SSS";
    MainContract.IView mView;

    private LinkedHashSet<String> mSendHistory;

    private String mDeviceID;
    private int    mBaudRate;
    private int    mCheckDigit;
    private int    mDataBits;
    private int    mStopBit;

    private boolean isHexReceive;
    private boolean isHexSend;
    private boolean isShowSend;
    private boolean isShowTime;
    private int     mRepeatDuring;

    private boolean                   isInterrupted;
    private Disposable                mReceiveDisposable;
    private ObservableEmitter<String> mEmitter;
    private Disposable                mSendDisposable;
    private Disposable                mSendRepeatDisposable;
    
    private UsbSerialPort             mUsbSerialPort;
    private static final int          READ_WAIT_MILLIS = 200;
    private static final int          BUFSIZ = 4096;
    private final ByteBuffer          mReadBuffer = ByteBuffer.allocate(BUFSIZ);
    // Synchronized by 'mWriteBuffer'
    private final ByteBuffer          mWriteBuffer = ByteBuffer.allocate(BUFSIZ);


    public MainPresenter(MainContract.IView view, UsbSerialPort port) {
        mView = view;
        mUsbSerialPort = port;

        isHexReceive = SPUtils.getInstance().getBoolean(SPKey.SETTING_RECEIVE_TYPE, true);
        isHexSend = SPUtils.getInstance().getBoolean(SPKey.SETTING_SEND_TYPE, true);
        isShowSend = SPUtils.getInstance().getBoolean(SPKey.SETTING_RECEIVE_SHOW_SEND, true);
        isShowTime = SPUtils.getInstance().getBoolean(SPKey.SETTING_RECEIVE_SHOW_TIME, true);
        mRepeatDuring = SPUtils.getInstance().getInt(SPKey.SETTING_RECEIVE_TYPE, 1000);

        try {
            mSendHistory = (LinkedHashSet<String>) SPUtils.getInstance()
                .getStringSet(SPKey.SEND_HISTORY, new LinkedHashSet<String>(30));
        } catch (ClassCastException e) {
            e.printStackTrace();
            mSendHistory = new LinkedHashSet<String>(30);
        }
    }

    private void refreshValueFormSp() {
        if (mUsbSerialPort != null) {
            mDeviceID = SPUtils.getInstance().getString(SPKey.DEVICE_ID,
                    mUsbSerialPort.getDriver().getClass().getSimpleName());
        }
        else {
            mDeviceID = SPUtils.getInstance().getString(SPKey.DEVICE_ID, "");
        }

        mBaudRate = Integer.parseInt(SPUtils.getInstance().getString(SPKey.BAUD_RATE, "115200"));
        mCheckDigit = Integer.parseInt(SPUtils.getInstance().getString(SPKey.CHECK_DIGIT, "0"));
        mDataBits = Integer.parseInt(SPUtils.getInstance().getString(SPKey.DATA_BITS, "8"));
        mStopBit = Integer.parseInt(SPUtils.getInstance().getString(SPKey.STOP_BIT, "1"));
    }

    public void getLeftData() {
        refreshValueFormSp();

        ArrayList<MultiItemEntity> list = new ArrayList<>();

        list.add(getLeftDeviceIDBean());
        list.add(getLeftBaudRateBean());
        list.add(getLeftCheckDigitBean());
        list.add(getLeftDataBitsBean());
        list.add(getLeftStopBitsBean());

        mView.setLeftData(list);
    }

    @NonNull
    private LeftHeadBean getLeftDeviceIDBean() {
        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_serial_port;
        bean.title = "串口";
        bean.spKey = SPKey.DEVICE_ID;
        bean.value = mDeviceID;

        return bean;
    }

    private LeftHeadBean getLeftBaudRateBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.baud_rate);
        for (int i : array) {
            if (i == mBaudRate) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_baud;
        bean.title = "波特率";
        bean.spKey = SPKey.BAUD_RATE;
        bean.value = String.valueOf(mBaudRate);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftCheckDigitBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.check_digit);
        for (int i : array) {
            if (i == mCheckDigit) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_check;
        bean.title = "校验位";
        bean.spKey = SPKey.CHECK_DIGIT;
        bean.value = String.valueOf(mCheckDigit);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftDataBitsBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.data_bits);
        for (int i : array) {
            if (i == mDataBits) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_data;
        bean.title = "数据位";
        bean.spKey = SPKey.DATA_BITS;
        bean.value = String.valueOf(mDataBits);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftStopBitsBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.stop_bits);
        for (int i : array) {
            if (i == mStopBit) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_stop;
        bean.title = "停止位";
        bean.spKey = SPKey.STOP_BIT;
        bean.value = String.valueOf(mStopBit);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    public void open(UsbManager mng) {
        refreshValueFormSp();

        if (mUsbSerialPort == null) {
            ToastUtils.showLong("没有串口设备!");
        } else {
            UsbDeviceConnection connection = mng.openDevice(mUsbSerialPort.getDriver().getDevice());
            if (connection == null) {
                ToastUtils.showLong("打开串口失败!");
                mView.setOpen(false);
                return;
            }

            try {
                mUsbSerialPort.open(connection);
                mUsbSerialPort.setParameters(mBaudRate, mDataBits, mStopBit, mCheckDigit);
            } catch (IOException e) {
                ToastUtils.showLong("打开串口失败! " + e.getMessage());
                try {
                    mUsbSerialPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mUsbSerialPort = null;
            }

            if (mUsbSerialPort != null) {
                isInterrupted = false;
                onReceiveSubscribe();
                onSendSubscribe();
                ToastUtils.showLong("打开串口成功！");
            }

            mView.setOpen(mUsbSerialPort != null);
        }
    }

    private void onSendSubscribe() {
        mSendDisposable =
            Observable.create((ObservableOnSubscribe<String>) emitter -> mEmitter = emitter)
                .filter(s -> !TextUtils.isEmpty(s))
                .doOnNext(s -> mSendHistory.add(s))
                .doOnNext(s -> mUsbSerialPort.write(isHexSend ? ByteUtils.hexStringToBytes(s)
                        : ByteUtils.stringToAsciiBytes(s), READ_WAIT_MILLIS))
                .observeOn(AndroidSchedulers.mainThread())
                .filter(s -> isShowSend)
                .subscribe(s -> {
                    mView.addData(new MessageBean(MessageBean.TYPE_SEND,
                        isShowTime ? DateTime.now().toString(mDateFormat) : "", s));
                }, Throwable::printStackTrace);
    }

    public void close() {
        isInterrupted = true;
        disposable(mReceiveDisposable);

        disposable(mSendDisposable);
        mEmitter = null;

        disposable(mSendRepeatDisposable);

        if (mUsbSerialPort != null) {
            mUsbSerialPort = null;
        }
        
        mView.setOpen(false);
    }

    private void onReceiveSubscribe() {
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
            .retry()
            .map(bytes -> isHexReceive ? addSpace(ByteUtils.bytesToHexString(bytes))
                : ByteUtils.bytesToAscii(bytes))
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(it -> mView.addData(new MessageBean(MessageBean.TYPE_RECEIVE,
                    isShowTime ? DateTime.now().toString(mDateFormat) : "", it)),
                Throwable::printStackTrace);
    }

    public void writeAsync(byte[] data) {
        synchronized (mWriteBuffer) {
            mWriteBuffer.put(data);
        }
    }

    private String addSpace(String s) {
        if (s.length() % 2 == 0) {
            StringBuilder builder = new StringBuilder();
            char[] array = s.toCharArray();
            int length = array.length;
            for (int i = 0; i < length; i += 2) {
                if (i != 0 && i <= length - 2) {
                    builder.append(" ");
                }

                builder.append(array[i]);
                builder.append(array[i + 1]);
            }

            return builder.toString();
        }
        return s;
    }

    public void sendMsg(String contain) {
        if (mEmitter != null) {
            mEmitter.onNext(contain.replace(" ", ""));
        } else {
            ToastUtils.showLong("请先打开串口！");
        }
    }

    public void refreshSendDuring(int result) {
        SPUtils.getInstance().put(SPKey.SETTING_SEND_DURING, result);
        mRepeatDuring = result;

        //正在运行
        if (mSendRepeatDisposable != null && !mSendRepeatDisposable.isDisposed()) {
            registerSendRepeat(true);
        }
    }

    public void refreshSendRepeat(boolean isChecked) {
        SPUtils.getInstance().put(SPKey.SETTING_SEND_REPEAT, isChecked);

        registerSendRepeat(isChecked);
    }

    public void refreshShowTime(boolean isChecked) {
        SPUtils.getInstance().put(SPKey.SETTING_RECEIVE_SHOW_TIME, isChecked);
        isShowTime = isChecked;
    }

    public void refreshShowSend(boolean isChecked) {
        SPUtils.getInstance().put(SPKey.SETTING_RECEIVE_SHOW_SEND, isChecked);
        isShowSend = isChecked;
    }

    public void refreshSendType(boolean isHex) {
        SPUtils.getInstance().put(SPKey.SETTING_SEND_TYPE, isHex);
        isHexSend = isHex;
    }

    public void refreshReceiveType(boolean isHex) {
        SPUtils.getInstance().put(SPKey.SETTING_RECEIVE_TYPE, isHex);
        isHexReceive = isHex;
    }

    private void registerSendRepeat(boolean checked) {
    
        if (mUsbSerialPort == null) {
            ToastUtils.showLong("请先打开串口！");
            return;
        }

        disposable(mSendRepeatDisposable);

        if (checked) {
            mSendRepeatDisposable = Observable.interval(mRepeatDuring, TimeUnit.MILLISECONDS)
                .subscribe(aLong -> sendMsg(mView.getEditText()), Throwable::printStackTrace);
        }
    }

    public void onDestroy() {
        close();
        SPUtils.getInstance().put(SPKey.SEND_HISTORY, mSendHistory);
    }

    private void disposable(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public ArrayList<String> getHistory() {
        return new ArrayList<>(mSendHistory);
    }
}
