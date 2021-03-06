package com.moxi.handwritinglibs.myScript;

import android.app.Application;
import android.content.Context;
import android.graphics.RectF;
import android.util.Base64;
import android.view.MotionEvent;
import android.widget.Toast;

import com.moxi.handwritinglibs.db.WritPadModel;
import com.moxi.handwritinglibs.db.WritePadUtils;
import com.moxi.handwritinglibs.listener.JiixLodingListener;
import com.moxi.handwritinglibs.listener.ScriptCallBack;
import com.moxi.handwritinglibs.model.CodeAndIndex;
import com.moxi.handwritinglibs.model.ExtendModel;
import com.moxi.handwritinglibs.model.WriteModel.WLine;
import com.moxi.handwritinglibs.model.WriteModel.WMoreLine;
import com.moxi.handwritinglibs.model.WriteModel.WPoint;
import com.moxi.handwritinglibs.model.WriteModel.WritePageData;
import com.moxi.handwritinglibs.myScript.utils.JiixForChars;
import com.moxi.handwritinglibs.myScript.utils.PathCounter;
import com.moxi.handwritinglibs.utils.JiixLoaderManager;
import com.moxi.handwritinglibs.writeUtils.PathUtils;
import com.mx.mxbase.base.MyApplication;
import com.mx.mxbase.constant.APPLog;
import com.mx.mxbase.utils.Base64Utils;
import com.mx.mxbase.utils.FileUtils;
import com.mx.mxbase.utils.StringUtils;
import com.mx.mxbase.utils.WindowsUtils;
import com.myscript.iink.ContentPart;
import com.myscript.iink.Editor;
import com.myscript.iink.IEditorListener2;
import com.myscript.iink.MimeType;
import com.myscript.iink.ParameterSet;
import com.myscript.iink.PointerEvent;
import com.myscript.iink.PointerEventType;
import com.myscript.iink.PointerType;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptManager implements IEditorListener2, JiixLodingListener {
    private Context context;
    private MyScriptService scriptService;
    //是拥有jiix文件
    public boolean existJiix = false;
    /**
     * 保存唯一标识
     */
    private CodeAndIndex codeAndIndex;
    public String savename;

    private ScriptCallBack callBack;
    private List<PointerEvent> pointerEvents;
    private PathCounter pathCounter;

    public void setCallBack(ScriptCallBack callBack) {
        this.callBack = callBack;
    }

    public ScriptCallBack getCallBack() {
        return callBack;
    }

    public ScriptManager(Context context) {
        this.context = context;
        scriptService = new MyScriptService();
        scriptService.init(WindowsUtils.WritedrawWidth, WindowsUtils.WritedrawHeight, context);
        scriptService.getEditor().addListener(this);
    }

    /**
     * 设置引擎是否可读jiix文件
     *
     * @param existJiix
     */
    public void setExistJiix(boolean existJiix) {
        if (existJiix)
            this.existJiix = existJiix;
    }

    public void setCodeAndIndex(CodeAndIndex codeAndIndex) {
        clear();
        this.codeAndIndex = codeAndIndex;
        //获取文件列外项目
        pathCounter = null;

        WritPadModel model = WritePadUtils.getInstance().getWritPadModel(codeAndIndex.saveCode, codeAndIndex.index);
        APPLog.e("setCodeAndIndex-model", model);
        if (model == null) {
            savename = System.currentTimeMillis() + "";
        } else {
            ExtendModel extendModel = model.getExtendModel();
            if (extendModel == null || extendModel.scriptSavePath.equals(""))
                savename = System.currentTimeMillis() + "";
            else
                savename = model.getExtendModel().scriptSavePath;
        }
        APPLog.e(codeAndIndex.saveCode, codeAndIndex.index);
        //初步设置jiix文件
        loadJiix();
    }

    /**
     * 注入当前删除的线并且判断可以得到的矩形框
     *
     * @param list
     */
    public void onCurRubberLine(List<WLine> list) {
        getIdle();
        if (!existJiix) return;
        if (pathCounter == null) {
            try {
                JiixForChars jiixForChars = scriptService.getJiixForChars();
                pathCounter = new PathCounter(jiixForChars.getChars(), scriptService.getTransform());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (pathCounter == null) return;
        pathCounter.judegDeleteRect(list);
    }

    public void onRubber(final WritePageData data, final List<WLine> lines) {
        if (!existJiix) return;
        if (lines == null || lines.size() == 0) return;
        if (pathCounter != null) {
            if (pathCounter.deleteCharsList != null || pathCounter.deleteCharsList.size() > 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        APPLog.e("开始时间",System.currentTimeMillis());
                        try {
                            List<WLine> draws=new ArrayList<WLine>();
                            List<RectF> rfs = pathCounter.deleteCharsList;
                            for (RectF rf : rfs) {
                                int is = data.drawMiddleLines.size();
                                for (int i = 0; i < is; i++) {
                                    WMoreLine mline = data.drawMiddleLines.get(i);
//                        PointerType type=mline.isLineStatus()?PointerType.PEN:PointerType.ERASER;
//                        APPLog.e("这里的进入",type);
                                    if (mline.status == 0) {
                                        int i1s = mline.MoreLines.size();
                                        for (int i1 = 0; i1 < i1s; i1++) {
                                            WLine line=mline.MoreLines.get(i1);
                                            if (PathUtils.getPathIntersect(rf,line)){
                                                draws.add(line);
                                            }

                                        }
                                    }
                                }
                                int size = data.mainLines.size();
                                for (int i = 0; i < size; i++) {
                                    WLine line=data.mainLines.get(i);
                                    if (PathUtils.getPathIntersect(rf,line)){
                                        draws.add(line);
                                    }
                                }
                            }
                            if (!scriptService.getEditor().isIdle()) {
                                scriptService.getEditor().waitForIdle();
                            }
                            existJiix = true;
                            DrawLine(lines,false);
                            APPLog.e("draws",draws.size());
                            DrawLine(draws,true);
                            APPLog.e("绘制时间",System.currentTimeMillis());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }).start();

            }
        }

    }

    /**
     * 获取jiix文件保存路径
     *
     * @return
     */
    public String getScriptSavePath() {
        return savename;
    }

    public void deleteiink() {
        if (!existJiix) return;
        String path = getScriptSavePath();
        scriptService.delete(context, codeAndIndex.saveCode, path);
        savename = System.currentTimeMillis() + "";
    }

    public void loadJiix() {
        String path = getScriptSavePath();
        existJiix = scriptService.setPackage(context, codeAndIndex.saveCode, path);
    }

    /**
     * 保存jiix文件到内存卡
     */
    public void saveJiix() {
        if (!existJiix) {
            callBack.saveResult();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!scriptService.getEditor().isIdle()) {
                    scriptService.getEditor().waitForIdle();
                }
                scriptService.savePacke();
                callBack.saveResult();
            }
        }).start();
    }

    /**
     * 翻译引擎是否处于空闲状态
     * @return
     */
    public void getIdle() {
        if (!scriptService.getEditor().isIdle()) {
            scriptService.getEditor().waitForIdle();
        }
    }

    /**
     * 文字转换
     *
     * @return 如果当前已有jiix文件返回true
     */
    public boolean getTransformTxt() {
        APPLog.e("existJiix", existJiix);
        if (!existJiix) {
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!scriptService.getEditor().isIdle()) {
                        scriptService.getEditor().waitForIdle();
                    }
                    if (callBack != null) callBack.transformReuslit(scriptService.change());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }

    /**
     * 原始数据全部添加入引擎进行翻译
     *
     * @param data
     */
    public void addCoordinate(final WritePageData data, boolean isTan) {
        if (data.dataNull()) {
            if (isTan)
                if (callBack != null) callBack.transformReuslit("");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int is = data.drawMiddleLines.size();
                    for (int i = 0; i < is; i++) {
                        WMoreLine mline = data.drawMiddleLines.get(i);
//                        PointerType type=mline.isLineStatus()?PointerType.PEN:PointerType.ERASER;
//                        APPLog.e("这里的进入",type);
                        if (mline.status == 0) {
                            int i1s = mline.MoreLines.size();
                            for (int i1 = 0; i1 < i1s; i1++) {
                                addCoordinate(mline.MoreLines.get(i1));
                            }
                        }
                    }
                    int size = data.mainLines.size();
                    for (int i = 0; i < size; i++) {
                        addCoordinate(data.mainLines.get(i));
                    }
                    if (!scriptService.getEditor().isIdle()) {
                        scriptService.getEditor().waitForIdle();
                    }
                    existJiix = true;
                    if (callBack != null) callBack.transformReuslit(scriptService.change());
                    APPLog.e("scriptManager-inertend", System.currentTimeMillis());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    public void DrawLine(final List<WLine> lines, final boolean is) {
        if (!existJiix) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (WLine line : lines) {
                    try {
                        addCoordinate(line, is?PointerType.PEN:PointerType.ERASER);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //    PointerType.ERASER
    private void addCoordinate(WLine line) throws Exception {
        addCoordinate(line, PointerType.PEN);
    }

    private void addCoordinate(WLine line, PointerType type) throws Exception {
        APPLog.e("PointerType", type);
        if (line == null || line.getPoints().size() < 3) return;
        List<WPoint> lines = line.getPoints();
        int size = lines.size();
        for (int i = 0; i < size; i++) {
            WPoint p = lines.get(i);
            if (i == 0) {
                addCoordinate(MotionEvent.ACTION_DOWN, p, type);
            } else if (i == (size - 1)) {
                addCoordinate(MotionEvent.ACTION_UP, p, type);
            } else {
                addCoordinate(MotionEvent.ACTION_MOVE, p, type);
            }
        }
    }

    private void addCoordinate(int action, WPoint point, PointerType type) throws Exception {
        if (pointerEvents == null) {
            pointerEvents = new ArrayList<PointerEvent>();
        }
        float x = point.x;
        float y = point.y;
        final long NO_TIMESTAMP = -1;
        final float NO_PRESSURE = 0.0f;
        final int NO_POINTER_ID = -1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                pointerEvents.clear();
                pointerEvents.add(new PointerEvent(PointerEventType.DOWN, x, y, NO_TIMESTAMP, NO_PRESSURE, type, NO_POINTER_ID));
                break;
            case MotionEvent.ACTION_MOVE:
                pointerEvents.add(new PointerEvent(PointerEventType.MOVE, x, y, NO_TIMESTAMP, NO_PRESSURE, type, NO_POINTER_ID));
                break;
            case MotionEvent.ACTION_UP:
                pointerEvents.add(new PointerEvent(PointerEventType.UP, x, y, NO_TIMESTAMP, NO_PRESSURE, type, NO_POINTER_ID));
                if (pointerEvents.size() > 0) {
                    PointerEvent[] pes = new PointerEvent[pointerEvents.size()];
                    pointerEvents.toArray(pes);
                    scriptService.getEditor().pointerEvents(pes, false);
                }
                break;
        }

    }

    /**
     * 添加点
     *
     * @param eventList
     */
    public void pointerEvents(List<PointerEvent> eventList) {
        pathCounter = null;
        APPLog.e("pointerEvents=" + eventList.size(), existJiix);
        if (existJiix) {
            if (eventList != null && eventList.size() > 0) {
                PointerEvent[] eventArr = new PointerEvent[eventList.size()];
                eventList.toArray(eventArr);
                scriptService.getEditor().pointerEvents(eventArr, false);
            }
        }
    }


    public void close() {
        scriptService.close();
    }

    public void clear() {
        scriptService.clear();
    }

    @Override
    public void selectionChanged(Editor editor, String[] strings) {

    }

    @Override
    public void activeBlockChanged(Editor editor, String s) {

    }

    @Override
    public void partChanging(Editor editor, ContentPart contentPart, ContentPart contentPart1) {

    }

    @Override
    public void partChanged(Editor editor) {

    }

    @Override
    public void contentChanged(Editor editor, String[] strings) {
        try {
            APPLog.e("contentChanged", scriptService.change());
        } catch (Exception e) {
            APPLog.e("contentChanged", e.getMessage());
        }
    }

    @Override
    public void onError(Editor editor, String s, String s1) {
        if (callBack != null) callBack.scriptFail("onError:" + s + "\n:" + s1);
    }

    private boolean savePath(String path, String value) {
        boolean is = false;
        //文件输出流
        FileOutputStream out = null;
        //设置文件路径
        File file = new File(path);
        try {
            out = new FileOutputStream(file);
            out.write(value.getBytes());
            is = true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return is;
    }

    /**
     * 加载jiix文件完成
     *
     * @param path
     * @param data
     */
    @Override
    public void onLoaderSucess(String path, String data) {
//        if (StringUtils.isNull(data)) return;
//        String cp = getScriptSavePath();
//        if (cp.equals(path)) {
//            APPLog.e("onLoaderSucess",data);
//            scriptService.loadJiix(data);
//        }
    }

    public static void deleteBySaveCode(String saveCode) {
        com.mx.mxbase.utils.StringUtils.deleteFile(new File(MyApplication.getInstance().getFilesDir(), saveCode));
    }
}
