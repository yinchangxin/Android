package connect.ui.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.google.protobuf.ByteString;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.ByteBuffer;

import connect.db.SharedPreferenceUtil;
import connect.im.IMessage;
import connect.im.bean.ConnectState;
import connect.im.bean.Session;
import connect.im.bean.SocketACK;
import connect.im.bean.UserCookie;
import connect.im.model.ChatSendManager;
import connect.im.model.MsgByteManager;
import connect.im.parser.CommandBean;
import connect.im.parser.ShakeHandBean;
import connect.ui.service.bean.PushMessage;
import connect.ui.service.bean.ServiceAck;
import connect.utils.TimeUtil;
import connect.utils.log.LogManager;

/**
 * Socket long service
 * Created by gtq on 2016/11/21.
 */
public class SocketService extends Service {

    private String Tag="SocketService";

    private SocketService service;
    private IMessage pushBinder;
    private LocalBinder localBinder;
    private LocalConnect localConnect;

    @Override
    public void onCreate() {
        super.onCreate();
        service=this;
        if (localBinder == null) {
            localBinder = new LocalBinder();
        }
        if(localConnect==null){
            localConnect=new LocalConnect();
        }
        EventBus.getDefault().register(this);
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, SocketService.class);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intent=new Intent(this,PushService.class);
        bindService(intent,localConnect,Service.BIND_IMPORTANT);

        return super.onStartCommand(intent, flags, startId);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public synchronized void onEventMainThread(PushMessage pushMessage) {
        LogManager.getLogger().d(Tag, "send ack:" + pushMessage.getServiceAck().getAck());
        ByteBuffer byteBuffer = pushMessage.getByteBuffer();
        try {
            pushBinder.connectMessage(pushMessage.getServiceAck().getAck(), byteBuffer.array());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void stopServer(Context context) {
        Intent intent = new Intent(context, SocketService.class);
        context.stopService(intent);
    }

    class LocalConnect implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pushBinder = IMessage.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Intent intent = new Intent(service, PushService.class);
            service.startService(intent);
            service.bindService(intent, localConnect, Service.BIND_IMPORTANT);
        }
    }

    class LocalBinder extends IMessage.Stub {

        @Override
        public void connectMessage(int type, byte[] message) throws RemoteException {
            LogManager.getLogger().d(Tag, type + "");
            ServiceAck serviceAck = ServiceAck.valueOf(type);
            switch (serviceAck) {
                case HAND_SHAKE:
                    ShakeHandBean shakeHandBean = new ShakeHandBean((byte) 0x00, null);
                    shakeHandBean.firstLoginShake();
                    break;
                case MESSAGE:
                    ByteBuffer byteBuffer = ByteBuffer.wrap(message);
                    MsgByteManager.getInstance().putByteMsg(byteBuffer);
                    break;
                case HEART_BEAT:
                    ChatSendManager.getInstance().sendToMsg(SocketACK.HEART_BREAK, ByteString.copyFrom(new byte[]{}));
                    checkUserCookie();
                    break;
                case CONNCET_REFRESH:
                    ConnectState.getInstance().sendEvent(ConnectState.ConnectType.REFRESH_ING);
                    break;
            }
        }
    }

    /**
     * check cookie expire time
     */
    public synchronized void checkUserCookie() {
        boolean checkExpire = false;
        UserCookie userCookie = Session.getInstance().getUserCookie(SharedPreferenceUtil.getInstance().getPubKey());
        if (userCookie != null) {
            long curTime = TimeUtil.getCurrentTimeSecond();
            checkExpire = curTime >= userCookie.getExpiredTime();
        }
        if (checkExpire) {
            try {
                CommandBean commandBean = new CommandBean((byte) 0x00, null);
                commandBean.chatCookieInfo(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
