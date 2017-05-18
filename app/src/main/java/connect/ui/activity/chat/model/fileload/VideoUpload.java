package connect.ui.activity.chat.model.fileload;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import connect.db.MemoryDataManager;
import connect.db.SharedPreferenceUtil;
import connect.db.green.DaoHelper.MessageHelper;
import connect.im.model.ChatSendManager;
import connect.ui.activity.chat.bean.MsgDefinBean;
import connect.ui.activity.chat.inter.FileUpLoad;
import connect.ui.activity.chat.model.content.BaseChat;
import connect.utils.BitmapUtil;
import connect.utils.cryption.EncryptionUtil;
import connect.utils.cryption.SupportKeyUril;
import protos.Connect;

/**
 * Created by gtq on 2016/12/5.
 */
public class VideoUpload extends FileUpLoad {

    public VideoUpload(Context context, BaseChat baseChat, MsgDefinBean bean, FileUpListener listener) {
        this.context = context;
        this.context = context;
        this.baseChat = baseChat;
        this.bean = bean;
        this.fileUpListener = listener;
    }

    @Override
    public void fileHandle() {
        super.fileHandle();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    String filePath = bean.getContent();
                    Bitmap thumbBitmap = BitmapUtil.thumbVideo(filePath);
                    String comFist = BitmapUtil.bitmapSavePath(thumbBitmap);

                    String priKey = MemoryDataManager.getInstance().getPriKey();
                    String pubkey = MemoryDataManager.getInstance().getPubKey();

                    Connect.GcmData firstGcmData = encodeAESGCMStructData(comFist);
                    Connect.GcmData secondGcmData = encodeAESGCMStructData(filePath);

                    Connect.RichMedia richMedia = Connect.RichMedia.newBuilder().
                            setThumbnail(firstGcmData.toByteString()).
                            setEntity(secondGcmData.toByteString()).build();
                    firstGcmData = EncryptionUtil.encodeAESGCMStructData(SupportKeyUril.EcdhExts.SALT,priKey, richMedia.toByteString());
                    mediaFile = Connect.MediaFile.newBuilder().setPubKey(pubkey).setCipherData(firstGcmData).build();

                    bean.setImageOriginWidth(thumbBitmap.getWidth());
                    bean.setImageOriginHeight(thumbBitmap.getHeight());
                    MessageHelper.getInstance().insertToMsg(bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                fileUp();
            }
        }.execute();
    }

    @Override
    public void fileUp() {
        if (mediaFile == null) {
            return;
        }
        resultUpFile(mediaFile, new FileResult() {
            @Override
            public void resultUpUrl(Connect.FileData mediaFile) {
                String content = getThumbUrl(mediaFile.getUrl(), mediaFile.getToken());
                String url = getUrl(mediaFile.getUrl(), mediaFile.getToken());

                fileUpListener.upSuccess(bean.getMessage_id(), content, url, bean.getSize(), bean.getExt1(), bean.getImageOriginWidth(), bean.getImageOriginHeight());
            }
        });
    }
}
