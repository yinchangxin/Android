package connect.ui.activity.chat.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import connect.im.bean.MsgType;
import connect.ui.activity.R;
import connect.ui.activity.chat.bean.MsgSend;
import connect.utils.AudioUtil;
import connect.utils.DialogUtil;
import connect.utils.FileUtil;
import connect.utils.system.SystemDataUtil;
import connect.utils.system.SystemUtil;
import connect.utils.TimeUtil;
import connect.utils.log.LogManager;

/**
 * Chat recording component
 * Created by gtq on 2016/11/27.
 */
public class RecordView extends LinearLayout {

    private String Tag = "RecordView";

    /** Constantly timing little red dot */
    private ImageView redDotView;
    /** Timing text */
    private TextView timerTxt;
    /** The tape on the left shows */
    private LinearLayout recordLayout;
    /** Cancel the left shows */
    private LinearLayout releaseLayout;
    /** The tape movement view */
    private DiffuseView recordImg;
    /** The recording utility class */
    private AudioUtil audioUtil;

    public RecordView(Context context) {
        super(context);
        initView();
    }

    public RecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    protected void initView() {
        audioUtil = AudioUtil.getInstance();
        audioUtil.setOnAudioRecordListener(new AudioUtil.AudioRecordListener() {

            private long duration;

            @Override
            public void startError() {
                permissionDialog();
            }

            @Override
            public void wellPrepared() {
                duration = TimeUtil.getCurrentTimeInLong();
            }

            @Override
            public void recording(long recordtime, int decibel) {
                if (decibel > 5) {
                    decibel = 5;
                } else if (decibel <= 0) {
                    decibel = 1;
                }
                recordImg.startDiffuse(decibel);

                timerTxt.setText(TimeUtil.getTime(recordtime,TimeUtil.DATE_FORMAT_SECOND));
                if (redDotView.getVisibility() == VISIBLE) {
                    redDotView.setVisibility(INVISIBLE);
                } else {
                    redDotView.setVisibility(VISIBLE);
                }
            }

            @Override
            public void recordFinish(String path) {
                stopRecord();

                int dur = (int) (TimeUtil.getCurrentTimeInLong() - duration) / 1000;
                if ((Math.abs(recordX) < SystemDataUtil.getScreenWidth() / 2) || dur < 2) {
                    FileUtil.deleteFile(path);
                } else {
                    MsgSend.sendOuterMsg(MsgType.Voice, path, dur);
                }
            }
        });

        View view = View.inflate(getContext(), R.layout.view_record, this);
        redDotView = (ImageView) view.findViewById(R.id.img1);
        timerTxt = (TextView) view.findViewById(R.id.txt1);
        recordLayout = (LinearLayout) view.findViewById(R.id.relativelayout_1);
        releaseLayout = (LinearLayout) view.findViewById(R.id.relativelayout_2);
        recordImg = (DiffuseView) view.findViewById(R.id.record);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        recordImg.setLayoutParams(params);
    }

    protected void permissionDialog() {
        DialogUtil.showAlertTextView(getContext(), getContext().getString(R.string.Set_tip_title),
                getContext().getString(R.string.Link_Unable_to_get_the_voice_data),
                "", getContext().getString(R.string.Set_Setting), false, new DialogUtil.OnItemClickListener() {
                    @Override
                    public void confirm(String value) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                        getContext().startActivity(intent);
                    }

                    @Override
                    public void cancel() {

                    }
                }, false);
    }

    protected void startRecord() {
        recordLayout.setVisibility(VISIBLE);
        releaseLayout.setVisibility(GONE);
        audioUtil.prepareAudio();

        recordImg.setTranslationX(0);//This must be added, or in the recording for the first time Huawei mobile phones will not show the recording button.
    }

    protected void stopRecord() {
        setVisibility(GONE);
        recordImg.setTranslationX(SystemDataUtil.getScreenWidth() - recordImg.getWidth());
        recordImg.stopDiffuse();

        recordLayout.setVisibility(VISIBLE);
        timerTxt.setText(TimeUtil.getTime(0,TimeUtil.DATE_FORMAT_SECOND));
        releaseLayout.setVisibility(GONE);
    }

    private float recordX;

    /**
     * Moving components
     */
    public void slideVRecord(MotionEvent event, int[] location) {
        float transX = event.getX() + location[0];
        int transY = location[1] - SystemUtil.dipToPx(60)/2;
        recordX = transX;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                LogManager.getLogger().d(Tag, "ACTION_DOWN");

                startRecord();
                recordImg.setLocationY(location[1] + SystemUtil.dipToPx(40) / 2);
                recordImg.startDiffuse(1);
                moveLcoation((int) transX);
                leftLocationY(transY);
                break;
            case MotionEvent.ACTION_MOVE:
                LogManager.getLogger().d(Tag, "ACTION_MOVE" + event.getX() + "location:" + transX);
                moveLcoation((int) transX);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                LogManager.getLogger().d(Tag, "ACTION_UP");
                audioUtil.finishRecorder();
                leftLocationY(SystemDataUtil.getScreenHeight() - releaseLayout.getHeight());
                break;
        }
    }

    protected void moveLcoation(int transX) {
        if (Math.abs(transX) < SystemDataUtil.getScreenWidth() / 2) {
            releaseLayout.setVisibility(VISIBLE);
            recordLayout.setVisibility(GONE);
            recordImg.setDiffuseState(transX, getContext().getResources().getColor(R.color.color_red));
        } else {
            releaseLayout.setVisibility(GONE);
            recordLayout.setVisibility(VISIBLE);
            recordImg.setDiffuseState(transX, getContext().getResources().getColor(R.color.color_green));
        }
    }

    public void leftLocationY(int transY) {
        recordLayout.setTranslationY(transY);
        releaseLayout.setTranslationY(transY);
    }
}